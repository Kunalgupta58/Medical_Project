"""
OCR service tests — uses synthetic prescription text, no real patient data.
"""
import pytest
from app.main import extract_fields, _uncertain, _found


SAMPLE_PRESCRIPTION_TEXT = """
Dr. Rajesh Kumar, MD
Apollo Hospitals, Bangalore
Date: 15/11/2026

Patient: Ramesh Sharma, 72 years

Rx:
Tab. Metformin 500mg
  Twice daily (BD)
  After food
  For 30 days
  
Tab. Amlodipine 5mg
  Once daily (OD)
  At bedtime
  
Instructions: Monitor blood pressure weekly. Avoid salty food.
"""

SAMPLE_PARTIAL_TEXT = """
500mg
BD
after meals
"""

BLANK_TEXT = ""

NOISY_TEXT = "@@## !!! ~~~ gibberish xyzabc"


def test_extract_dosage_from_full_prescription():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    assert not fields["dosage"].uncertain
    assert "500" in fields["dosage"].value


def test_extract_frequency_bd():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    assert not fields["frequency"].uncertain
    assert "twice" in fields["frequency"].value.lower() or "bd" in fields["frequency"].value.lower()


def test_extract_timing_after_food():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    assert not fields["timing"].uncertain
    assert "food" in fields["timing"].value.lower() or "meal" in fields["timing"].value.lower()


def test_extract_duration():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    assert not fields["duration"].uncertain
    assert "30" in fields["duration"].value


def test_extract_doctor_name():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    assert not fields["doctor_name"].uncertain
    assert "Kumar" in fields["doctor_name"].value or "Rajesh" in fields["doctor_name"].value


def test_extract_date():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    assert not fields["prescription_date"].uncertain
    assert "2026" in fields["prescription_date"].value or "15" in fields["prescription_date"].value


def test_partial_text_returns_uncertain_for_missing():
    fields = extract_fields(SAMPLE_PARTIAL_TEXT)
    # Dosage may be found
    # Medicine name should be uncertain
    assert fields["medicine_name"].uncertain
    assert fields["medicine_name"].uncertainty_reason is not None


def test_blank_text_all_uncertain():
    fields = extract_fields(BLANK_TEXT)
    for key, field in fields.items():
        assert field.uncertain, f"Field '{key}' should be uncertain for blank text"


def test_noisy_text_returns_uncertain():
    fields = extract_fields(NOISY_TEXT)
    assert fields["medicine_name"].uncertain
    assert fields["dosage"].uncertain


def test_uncertain_field_has_reason():
    field = _uncertain("Test reason")
    assert field.uncertain
    assert field.value is None
    assert "Test reason" in field.uncertainty_reason


def test_found_field_values():
    field = _found("Metformin 500mg", "Tab. Metformin 500mg BD")
    assert not field.uncertain
    assert field.value == "Metformin 500mg"
    assert "Tab." in field.source_text


def test_once_daily_frequency():
    text = "Tab. Aspirin 75mg once daily OD after food"
    fields = extract_fields(text)
    assert not fields["frequency"].uncertain


def test_tds_frequency():
    text = "Syrup Amoxicillin 250mg TDS for 7 days"
    fields = extract_fields(text)
    assert not fields["frequency"].uncertain


def test_weekly_frequency():
    text = "Tab. Folic Acid 5mg once weekly"
    fields = extract_fields(text)
    assert not fields["frequency"].uncertain
    assert "week" in fields["frequency"].value.lower()


def test_as_needed_frequency():
    text = "Tab. Paracetamol 500mg as needed (PRN) for fever"
    fields = extract_fields(text)
    assert not fields["frequency"].uncertain


def test_mcg_dosage():
    text = "Tab. Thyroxine 50mcg once daily"
    fields = extract_fields(text)
    assert not fields["dosage"].uncertain
    assert "mcg" in fields["dosage"].value.lower()


def test_ml_dosage():
    text = "Syrup Lactulose 15ml twice daily"
    fields = extract_fields(text)
    assert not fields["dosage"].uncertain


def test_instructions_extracted():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    assert not fields["instructions"].uncertain


def test_before_food_timing():
    text = "Tab. Glipizide 5mg before food twice daily"
    fields = extract_fields(text)
    assert not fields["timing"].uncertain
    assert "food" in fields["timing"].value.lower()


def test_bedtime_timing():
    text = "Tab. Atorvastatin 20mg at bedtime OD"
    fields = extract_fields(text)
    assert not fields["timing"].uncertain


def test_duration_weeks():
    text = "Take for 2 weeks then review"
    fields = extract_fields(text)
    assert not fields["duration"].uncertain


def test_duration_months():
    text = "Continue for 3 months"
    fields = extract_fields(text)
    assert not fields["duration"].uncertain


def test_date_slash_format():
    text = "Prescription date: 10/05/2025"
    fields = extract_fields(text)
    assert not fields["prescription_date"].uncertain


def test_date_month_name_format():
    text = "Date: 15 November 2026"
    fields = extract_fields(text)
    assert not fields["prescription_date"].uncertain


def test_no_hallucination_on_blank():
    """OCR must never invent values when text is empty."""
    fields = extract_fields("")
    for key, field in fields.items():
        assert field.value is None or field.uncertain is False, \
            f"Field '{key}' should not have a value and be certain when input is blank"


def test_source_text_present_when_found():
    fields = extract_fields(SAMPLE_PRESCRIPTION_TEXT)
    if not fields["dosage"].uncertain:
        assert fields["dosage"].source_text is not None
        assert len(fields["dosage"].source_text) > 0
