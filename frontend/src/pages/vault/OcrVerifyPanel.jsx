import { useState, useRef } from 'react'
import { ocrApi } from '../../api/vault'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import { AlertCircle, CheckCircle, Upload } from 'lucide-react'

export default function OcrVerifyPanel({ doc, onClose }) {
  const [result, setResult]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const [file, setFile]       = useState(null)
  const [verified, setVerified] = useState({})
  const inputRef = useRef()

  const handleProcess = async () => {
    if (!file) { setError('Please select the document file to process'); return }
    setError(''); setLoading(true)
    try {
      const data = await ocrApi.process(file)
      setResult(data)
      // Pre-populate verified fields with found (non-uncertain) values
      const v = {}
      Object.entries(data).forEach(([k, f]) => {
        if (f && typeof f === 'object' && !f.uncertain && f.value) v[k] = f.value
      })
      setVerified(v)
    } catch (e) {
      setError(e?.detail || e?.message || 'OCR processing failed. Check that the OCR service is running.')
    } finally {
      setLoading(false)
    }
  }

  const FIELDS = [
    { key: 'medicine_name',    label: 'Medicine name' },
    { key: 'dosage',           label: 'Dosage' },
    { key: 'frequency',        label: 'Frequency' },
    { key: 'timing',           label: 'Timing' },
    { key: 'duration',         label: 'Duration' },
    { key: 'doctor_name',      label: 'Doctor name' },
    { key: 'prescription_date', label: 'Prescription date' },
    { key: 'instructions',     label: 'Instructions' },
  ]

  return (
    <div className="space-y-5">
      <Alert variant="warning"
        message="OCR extracts CANDIDATE text only. All fields must be manually verified before saving. Never save medication info without checking against the original document." />

      {!result && (
        <div>
          <p className="text-sm text-gray-600 mb-3">
            Upload the prescription file for local OCR processing. Only PDF, JPG, JPEG, PNG files are supported.
            OCR runs locally using Tesseract — no data leaves your server.
          </p>
          <div className="flex gap-3">
            <input ref={inputRef} type="file" accept=".pdf,.jpg,.jpeg,.png" className="hidden"
              onChange={e => setFile(e.target.files[0])} />
            <button type="button" onClick={() => inputRef.current.click()}
              className="btn-secondary flex items-center gap-2">
              <Upload className="h-4 w-4" /> {file ? file.name : 'Select file'}
            </button>
            <button onClick={handleProcess} disabled={loading || !file} className="btn-primary flex items-center gap-2">
              {loading ? <Spinner size="sm" /> : null}
              {loading ? 'Processing…' : 'Run OCR'}
            </button>
          </div>
          {error && <div className="mt-3"><Alert variant="error" message={error} /></div>}
        </div>
      )}

      {result && (
        <div className="space-y-5">
          <div className="bg-blue-50 rounded-xl p-3 text-xs text-blue-800">
            <strong>OCR Note:</strong> {result.ocr_note}
          </div>

          <div>
            <h3 className="font-semibold text-gray-900 mb-3">Extracted Fields — Review & Correct</h3>
            <div className="space-y-3">
              {FIELDS.map(({ key, label }) => {
                const field = result[key]
                if (!field) return null
                const isUncertain = field.uncertain
                return (
                  <div key={key} className={`rounded-xl p-3 border ${isUncertain ? 'bg-amber-50 border-amber-200' : 'bg-green-50 border-green-200'}`}>
                    <div className="flex items-center gap-2 mb-2">
                      {isUncertain
                        ? <AlertCircle className="h-4 w-4 text-amber-500 shrink-0" />
                        : <CheckCircle className="h-4 w-4 text-green-500 shrink-0" />}
                      <span className="text-sm font-medium text-gray-700">{label}</span>
                      {isUncertain && <span className="text-xs text-amber-600 ml-auto">Uncertain — verify manually</span>}
                    </div>
                    {isUncertain && (
                      <p className="text-xs text-amber-600 mb-2">{field.uncertainty_reason}</p>
                    )}
                    {field.source_text && !isUncertain && (
                      <p className="text-xs text-gray-400 mb-1">Source: "{field.source_text}"</p>
                    )}
                    <input
                      className="input-field text-sm"
                      value={verified[key] || ''}
                      onChange={e => setVerified(v => ({ ...v, [key]: e.target.value }))}
                      placeholder={isUncertain ? 'Enter value after checking original document…' : ''}
                    />
                  </div>
                )
              })}
            </div>
          </div>

          <div>
            <h3 className="font-semibold text-gray-900 mb-2">Raw Extracted Text</h3>
            <pre className="bg-gray-50 rounded-xl p-3 text-xs text-gray-600 whitespace-pre-wrap max-h-48 overflow-y-auto border">
              {result.raw_text || '(no text extracted)'}
            </pre>
          </div>

          <Alert variant="warning"
            message="Saving verified data to a medication record is not done automatically. After verifying these fields, return to the Medications page and add a new medication using the verified information." />

          <div className="flex gap-3 justify-end">
            <button onClick={onClose} className="btn-secondary">Close</button>
            <button onClick={() => setResult(null)} className="btn-secondary">Process another file</button>
          </div>
        </div>
      )}
    </div>
  )
}
