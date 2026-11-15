"""
ElderCare 360 — Local OCR Microservice
Uses Tesseract OCR + OpenCV. No external LLM or hosted OCR API.
"""

import os
import io
import re
import time
import logging
from pathlib import Path
from typing import Optional

import cv2
import numpy as np
import pytesseract
import fitz  # PyMuPDF
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Point to Tesseract binary if needed (Windows default path)
TESSERACT_CMD = os.getenv("TESSERACT_CMD", r"C:\Program Files\Tesseract-OCR\tesseract.exe")
if os.path.exists(TESSERACT_CMD):
    pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD

app = FastAPI(
    title="ElderCare 360 OCR Service",
    description="Local prescription and medical document OCR using Tesseract. No external AI APIs.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

ALLOWED_EXTENSIONS = {".pdf", ".jpg", ".jpeg", ".png"}
MAX_FILE_BYTES = 20 * 1024 * 1024  # 20 MB


# ── Response Models ──────────────────────────────────────────────────────────

class ExtractedField(BaseModel):
    value: Optional[str]
    source_text: Optional[str]
    uncertain: bool
    uncertainty_reason: Optional[str]


class OCRResult(BaseModel):
    raw_text: str
    page_count: int
    medicine_name: ExtractedField
    dosage: ExtractedField
    frequency: ExtractedField
    timing: ExtractedField
    duration: ExtractedField
    doctor_name: ExtractedField
    prescription_date: ExtractedField
    instructions: ExtractedField
    processing_duration_ms: int
    ocr_note: str


# ── Image Preprocessing ──────────────────────────────────────────────────────

def preprocess_image(image: np.ndarray) -> np.ndarray:
    """
    Apply preprocessing to improve Tesseract accuracy:
    grayscale → denoise → threshold → scale up if small.
    """
    # Convert to grayscale
    if len(image.shape) == 3:
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    else:
        gray = image.copy()

    # Scale up small images
    h, w = gray.shape
    if w < 1000:
        scale = 1000 / w
        gray = cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)

    # Denoise
    denoised = cv2.fastNlMeansDenoising(gray, h=10, templateWindowSize=7, searchWindowSize=21)

    # Adaptive threshold (handles uneven lighting)
    thresh = cv2.adaptiveThreshold(
        denoised, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY, 31, 10
    )

    # Deskew
    coords = np.column_stack(np.where(thresh < 128))
    if len(coords) > 100:
        angle = cv2.minAreaRect(coords)[-1]
        if angle < -45:
            angle = -(90 + angle)
        else:
            angle = -angle
        if abs(angle) > 0.5:
            (h2, w2) = thresh.shape
            center = (w2 // 2, h2 // 2)
            M = cv2.getRotationMatrix2D(center, angle, 1.0)
            thresh = cv2.warpAffine(thresh, M, (w2, h2),
                                    flags=cv2.INTER_CUBIC,
                                    borderMode=cv2.BORDER_REPLICATE)

    return thresh


def run_tesseract(image_array: np.ndarray) -> str:
    """Run Tesseract on a preprocessed numpy image array."""
    pil_image = Image.fromarray(image_array)
    config = "--oem 3 --psm 6 -c preserve_interword_spaces=1"
    text = pytesseract.image_to_string(pil_image, config=config, lang="eng")
    return text.strip()


# ── PDF → Images ─────────────────────────────────────────────────────────────

def pdf_to_images(pdf_bytes: bytes) -> list[np.ndarray]:
    """Convert each PDF page to a numpy image array at 300 DPI."""
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    images = []
    for page in doc:
        mat = fitz.Matrix(300 / 72, 300 / 72)  # 300 DPI
        pix = page.get_pixmap(matrix=mat, colorspace=fitz.csRGB)
        img_array = np.frombuffer(pix.samples, dtype=np.uint8).reshape(
            pix.height, pix.width, 3
        )
        images.append(img_array)
    doc.close()
    return images


# ── Candidate Field Extraction ────────────────────────────────────────────────

def _uncertain(reason: str) -> ExtractedField:
    return ExtractedField(
        value=None,
        source_text=None,
        uncertain=True,
        uncertainty_reason=reason
    )


def _found(value: str, source: str) -> ExtractedField:
    return ExtractedField(
        value=value.strip(),
        source_text=source.strip(),
        uncertain=False,
        uncertainty_reason=None
    )


def extract_fields(text: str) -> dict:
    """
    Attempt to extract candidate fields using deterministic regex patterns.
    Never invents values. Returns uncertain fields with explanation.
    """
    lines = [l.strip() for l in text.splitlines() if l.strip()]
    full = " ".join(lines)

    # ── Medicine name ─────────────────────────────────────────────────────────
    medicine_name = _uncertain("Could not confidently identify medicine name. Please verify manually.")
    rx_patterns = [
        r"(?:Tab\.?|Cap\.?|Tablet|Capsule|Syrup|Inj\.?|Injection|Drop[s]?|Gel|Cream)\s+([A-Z][A-Za-z0-9\-\s]{2,30})",
        r"(?:Rx|Prescription|Medicine|Drug)[:\s]+([A-Z][A-Za-z0-9\-\s]{2,30})",
    ]
    for pat in rx_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            candidate = m.group(1).strip()
            if 3 <= len(candidate) <= 40:
                medicine_name = _found(candidate, m.group(0))
                break

    # ── Dosage ────────────────────────────────────────────────────────────────
    dosage = _uncertain("Could not confidently extract dosage. Please verify manually.")
    dosage_patterns = [
        r"(\d+(?:\.\d+)?\s*(?:mg|mcg|ml|g|IU|units?)(?:\s*/\s*\d+\s*(?:mg|ml))?)",
        r"(\d+\s*(?:mg|mcg|ml|g))",
    ]
    for pat in dosage_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            dosage = _found(m.group(1), m.group(0))
            break

    # ── Frequency ─────────────────────────────────────────────────────────────
    frequency = _uncertain("Could not confidently extract frequency. Please verify manually.")
    freq_patterns = [
        (r"\b(once\s+(?:a\s+)?daily|OD|od)\b", "Once daily"),
        (r"\b(twice\s+(?:a\s+)?daily|BD|BID|bid|bd)\b", "Twice daily"),
        (r"\b(three\s+times\s+(?:a\s+)?day|TDS|TID|tid|tds)\b", "Three times daily"),
        (r"\b(four\s+times\s+(?:a\s+)?day|QID|qid)\b", "Four times daily"),
        (r"\b(every\s+\d+\s*hours?)\b", None),
        (r"\b(weekly|once\s+a\s+week)\b", "Weekly"),
        (r"\b(as\s+needed|PRN|prn|SOS|sos)\b", "As needed"),
    ]
    for pat, label in freq_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            value = label if label else m.group(1)
            frequency = _found(value, m.group(0))
            break

    # ── Timing ────────────────────────────────────────────────────────────────
    timing = _uncertain("Could not confidently extract timing. Please verify manually.")
    timing_patterns = [
        r"\b(before\s+(?:food|meals?|breakfast|lunch|dinner))\b",
        r"\b(after\s+(?:food|meals?|breakfast|lunch|dinner))\b",
        r"\b(with\s+(?:food|water|milk))\b",
        r"\b(at\s+(?:bedtime|night|morning))\b",
        r"\b(empty\s+stomach)\b",
    ]
    for pat in timing_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            timing = _found(m.group(1), m.group(0))
            break

    # ── Duration ─────────────────────────────────────────────────────────────
    duration = _uncertain("Could not confidently extract duration. Please verify manually.")
    dur_patterns = [
        r"\b(for\s+\d+\s*(?:days?|weeks?|months?))\b",
        r"\b(\d+\s*(?:days?|weeks?|months?)\s*course)\b",
    ]
    for pat in dur_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            duration = _found(m.group(1), m.group(0))
            break

    # ── Doctor name ───────────────────────────────────────────────────────────
    doctor_name = _uncertain("Could not confidently extract doctor name. Please verify manually.")
    doc_patterns = [
        r"(?:Dr\.?|Doctor|Physician)[:\s]+([A-Z][a-zA-Z\s\.]{2,40})",
        r"(?:Signed by|Prescribed by)[:\s]+([A-Z][a-zA-Z\s\.]{2,40})",
    ]
    for pat in doc_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            candidate = m.group(1).strip().rstrip(".,")
            if 3 <= len(candidate) <= 50:
                doctor_name = _found(candidate, m.group(0))
                break

    # ── Prescription date ─────────────────────────────────────────────────────
    presc_date = _uncertain("Could not confidently extract date. Please verify manually.")
    date_patterns = [
        r"\b(\d{1,2}[/\-\.]\d{1,2}[/\-\.]\d{2,4})\b",
        r"\b(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})\b",
        r"\b((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{4})\b",
    ]
    for pat in date_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            presc_date = _found(m.group(1), m.group(0))
            break

    # ── Instructions ──────────────────────────────────────────────────────────
    instructions = _uncertain("No specific instructions found. Please review original document.")
    instr_patterns = [
        r"(?:Instructions?|Notes?|Advice|Directions?)[:\s]+(.{10,200}?)(?:\n|$)",
        r"(?:Do\s+not|Avoid|Take\s+with)[^\n]{10,100}",
    ]
    for pat in instr_patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            candidate = m.group(0 if "Do not" in pat or "Avoid" in pat or "Take with" in pat else 1).strip()
            if len(candidate) >= 10:
                instructions = _found(candidate[:200], candidate[:200])
                break

    return {
        "medicine_name": medicine_name,
        "dosage": dosage,
        "frequency": frequency,
        "timing": timing,
        "duration": duration,
        "doctor_name": doctor_name,
        "prescription_date": presc_date,
        "instructions": instructions,
    }


# ── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    try:
        version = pytesseract.get_tesseract_version()
        return {"status": "ok", "tesseract_version": str(version)}
    except Exception as e:
        return {"status": "degraded", "error": str(e),
                "note": "Tesseract may not be installed or path may be incorrect"}


@app.post("/ocr/process", response_model=OCRResult)
async def process_document(file: UploadFile = File(...)):
    """
    Process an uploaded PDF or image with local Tesseract OCR.
    Returns raw extracted text and candidate fields.
    All fields are marked uncertain until manually verified by the user.
    This endpoint never invents medical information.
    """
    start = time.time()

    # Validate extension
    suffix = Path(file.filename or "").suffix.lower()
    if suffix not in ALLOWED_EXTENSIONS:
        raise HTTPException(400, f"Unsupported file type '{suffix}'. Allowed: {ALLOWED_EXTENSIONS}")

    content = await file.read()
    if len(content) > MAX_FILE_BYTES:
        raise HTTPException(413, "File exceeds 20 MB limit")

    if len(content) < 100:
        raise HTTPException(400, "File appears to be empty or corrupt")

    all_text_parts = []
    page_count = 1

    try:
        if suffix == ".pdf":
            images = pdf_to_images(content)
            page_count = len(images)
            for i, img in enumerate(images):
                processed = preprocess_image(img)
                page_text = run_tesseract(processed)
                if page_text.strip():
                    all_text_parts.append(f"[Page {i+1}]\n{page_text}")
        else:
            # Image file
            np_arr = np.frombuffer(content, np.uint8)
            img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            if img is None:
                raise HTTPException(400, "Could not decode image. File may be corrupt.")
            processed = preprocess_image(img)
            text = run_tesseract(processed)
            all_text_parts.append(text)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"OCR processing error: {e}")
        raise HTTPException(500, f"OCR processing failed: {str(e)}")

    raw_text = "\n\n".join(all_text_parts).strip()

    if not raw_text:
        raise HTTPException(422, "No text could be extracted from this document. "
                                  "The file may be a low-quality scan or contain no text.")

    fields = extract_fields(raw_text)
    duration_ms = int((time.time() - start) * 1000)

    return OCRResult(
        raw_text=raw_text,
        page_count=page_count,
        processing_duration_ms=duration_ms,
        ocr_note=(
            "IMPORTANT: All extracted fields are CANDIDATES only. "
            "OCR results may contain errors. "
            "You MUST manually verify every field before saving any medication information. "
            "Do not use uncertain fields without verification."
        ),
        **fields,
    )


@app.get("/")
def root():
    return {
        "service": "ElderCare 360 OCR Service",
        "version": "1.0.0",
        "engine": "Tesseract OCR (local, no external AI APIs)",
        "docs": "/docs",
    }
