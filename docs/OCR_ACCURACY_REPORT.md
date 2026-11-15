# ElderCare 360 — OCR Accuracy Report

## Overview

This report documents the OCR evaluation methodology, test dataset description, results and limitations for the local Tesseract-based prescription OCR service.

**Important:** This service uses local Tesseract OCR. It does not call any external AI or LLM APIs. All text extraction is deterministic and runs entirely on the server.

---

## Dataset Description

### Test samples used

The OCR service was evaluated against **synthetic and publicly available prescription template text** only.

| Sample set | Count | Description |
|---|---|---|
| Development | 10 | Synthetic typed prescriptions for feature development |
| Validation | 10 | Additional synthetic samples with varying formats |
| Unit tests | 30 | Programmatic text strings (see `ocr-service/tests/test_ocr.py`) |

**No real patient prescriptions were used at any point.**

All synthetic samples were created for testing purposes only and do not represent any real individual's medical information.

---

## Methodology

The OCR pipeline applies the following preprocessing steps before Tesseract:

1. Convert to grayscale
2. Scale up images narrower than 1000px
3. Fast non-local means denoising
4. Adaptive Gaussian thresholding
5. Deskew (rotation correction)
6. Tesseract with `--oem 3 --psm 6`

Field extraction uses deterministic regex patterns. Each field is classified as either:
- **Found** — pattern matched, value extracted, source text recorded
- **Uncertain** — no pattern matched or value ambiguous

---

## Test Results (Unit Tests)

The 30 unit tests in `tests/test_ocr.py` cover:

| Test category | Tests | Expected behavior |
|---|---|---|
| Full prescription parsing | 8 | Dosage, frequency, timing, duration, doctor, date extracted |
| Partial text | 3 | Missing fields return uncertain with explanation |
| Blank / noisy text | 4 | All fields uncertain, no invented values |
| Specific frequency patterns | 5 | OD, BD, TDS, Weekly, PRN each recognized |
| Specific dosage units | 3 | mg, mcg, ml each recognized |
| Timing patterns | 3 | Before/after food, bedtime recognized |
| Duration patterns | 2 | Days and months extracted |
| Non-hallucination check | 2 | Empty input produces no invented values |

**To run the tests:**
```bash
cd ocr-service
pip install -r requirements.txt
pytest tests/ -v
```

---

## Field Extraction Results (on synthetic typed text)

Results are from the unit test suite on synthetic text inputs only. These do not represent real-world scan performance.

| Field | Detection rate on clear typed text | Notes |
|---|---|---|
| Dosage (mg/mcg/ml) | High | Standard patterns reliably matched |
| Frequency (OD/BD/TDS) | High | Common abbreviations recognized |
| Timing (before/after food) | High | Standard phrases matched |
| Duration (days/weeks) | High | Numeric + unit patterns matched |
| Doctor name | Medium | Depends on "Dr." prefix being present |
| Prescription date | Medium | Multiple formats supported |
| Medicine name | Low-Medium | Requires "Tab." / "Cap." / "Rx:" prefix |
| Instructions | Medium | Variable formatting reduces reliability |

**On real handwritten or low-quality scans:** accuracy will be significantly lower. This is a known limitation of Tesseract and is explicitly communicated to users via uncertainty flags.

---

## Limitations

1. **Handwritten prescriptions** are poorly supported by Tesseract. OCR results will typically be low quality and uncertain.

2. **Low-resolution scans** (below ~200 DPI) produce unreliable output.

3. **Multi-language prescriptions** — only English is configured. Non-English text will produce uncertain or garbled output.

4. **Confidence scores** — Tesseract character-level confidence is not exposed as a calibrated probability. The service uses pattern-matching success/failure, not confidence thresholds.

5. **No medical validation** — the OCR service never validates whether extracted text is a real medicine name. It only reports what it found in the text.

6. **No automatic medication creation** — the OCR service returns candidate fields only. A human must verify and manually create the medication record.

---

## Safety Guarantees

The OCR service is designed with these hard safety constraints:

- Never invents medicine names, dosages, or frequencies
- Never automatically creates medication schedules from OCR output
- Every uncertain field shows an explicit uncertainty reason
- Raw extracted text is always shown alongside candidate fields
- Users must explicitly confirm verified data before saving

---

## Accuracy Report Status

| Metric | Status |
|---|---|
| Character Error Rate | Not measured — requires character-level ground truth |
| Word Error Rate | Not measured — requires word-level ground truth |
| Medicine name exact match | Not measured on real scans — only synthetic text tested |
| Uncertain field rejection | Working correctly (all ambiguous fields flagged) |
| No-hallucination on empty input | Verified by unit test |

A full CER/WER evaluation requires a licensed dataset of real prescription scans with verified ground truth transcriptions. This was not available for the hackathon timeframe. The evaluation was conducted on synthetic text only.

---

## Future Improvements

1. Add a labeled dataset of authorized synthetic prescription scans for formal CER/WER measurement
2. Experiment with Tesseract language models fine-tuned for medical text
3. Add preprocessing for handwriting (would require a specialized model)
4. Add document layout analysis to handle multi-column prescriptions
5. Consider confidence-based filtering using Tesseract's per-word confidence values
