import { useState, useRef } from 'react'
import { Upload, X, FileText } from 'lucide-react'
import { vaultApi } from '../../api/vault'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'

const CATEGORIES = [
  'PRESCRIPTION','CBC_REPORT','BLOOD_GLUCOSE','HBA1C','KIDNEY_FUNCTION',
  'LIVER_FUNCTION','LIPID_PROFILE','XRAY','MRI','CT_SCAN','ULTRASOUND',
  'DISCHARGE_SUMMARY','CONSULTATION_NOTES','VACCINATION_RECORD','OTHER',
]

const CAT_LABELS = {
  PRESCRIPTION:'Prescription',CBC_REPORT:'CBC Report',BLOOD_GLUCOSE:'Blood Glucose',
  HBA1C:'HbA1c',KIDNEY_FUNCTION:'Kidney Function',LIVER_FUNCTION:'Liver Function',
  LIPID_PROFILE:'Lipid Profile',XRAY:'X-Ray',MRI:'MRI',CT_SCAN:'CT Scan',
  ULTRASOUND:'Ultrasound',DISCHARGE_SUMMARY:'Discharge Summary',
  CONSULTATION_NOTES:'Consultation Notes',VACCINATION_RECORD:'Vaccination Record',OTHER:'Other',
}

export default function UploadForm({ onUploaded, onCancel }) {
  const [file, setFile]       = useState(null)
  const [form, setForm]       = useState({ title: '', category: 'PRESCRIPTION', documentDate: '', doctorName: '', hospitalName: '', notes: '' })
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)
  const inputRef = useRef()

  const onFileChange = (e) => {
    const f = e.target.files[0]
    if (!f) return
    if (f.size > 20 * 1024 * 1024) { setError('File must be under 20 MB'); return }
    const ext = f.name.split('.').pop().toLowerCase()
    if (!['pdf','jpg','jpeg','png'].includes(ext)) { setError('Only PDF, JPG, JPEG, PNG files allowed'); return }
    setError('')
    setFile(f)
    if (!form.title) setForm(prev => ({ ...prev, title: f.name.replace(/\.[^.]+$/, '').replace(/[-_]/g, ' ') }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!file) { setError('Please select a file'); return }
    if (!form.title.trim()) { setError('Please enter a document title'); return }
    setError(''); setLoading(true)

    const fd = new FormData()
    fd.append('file', file)
    fd.append('title', form.title)
    fd.append('category', form.category)
    if (form.documentDate) fd.append('documentDate', form.documentDate)
    if (form.doctorName)   fd.append('doctorName', form.doctorName)
    if (form.hospitalName) fd.append('hospitalName', form.hospitalName)
    if (form.notes)        fd.append('notes', form.notes)

    try {
      await vaultApi.upload(fd)
      onUploaded()
    } catch (err) {
      setError(err?.response?.data?.message || 'Upload failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      {error && <Alert variant="error" message={error} dismissible />}

      {/* Drop zone */}
      <div onClick={() => inputRef.current.click()}
        className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors
          ${file ? 'border-primary-400 bg-primary-50' : 'border-gray-200 hover:border-primary-300 hover:bg-gray-50'}`}>
        <input ref={inputRef} type="file" accept=".pdf,.jpg,.jpeg,.png" className="hidden" onChange={onFileChange} />
        {file ? (
          <div className="flex items-center justify-center gap-3">
            <FileText className="h-8 w-8 text-primary-600" />
            <div className="text-left">
              <p className="font-medium text-gray-900">{file.name}</p>
              <p className="text-sm text-gray-500">{(file.size / 1024).toFixed(0)} KB</p>
            </div>
            <button type="button" onClick={e => { e.stopPropagation(); setFile(null) }}
              className="ml-auto text-gray-400 hover:text-red-500">
              <X className="h-5 w-5" />
            </button>
          </div>
        ) : (
          <div>
            <Upload className="h-10 w-10 text-gray-300 mx-auto mb-3" />
            <p className="font-medium text-gray-700">Click to select a file</p>
            <p className="text-sm text-gray-400 mt-1">PDF, JPG, JPEG, PNG · Max 20 MB</p>
          </div>
        )}
      </div>

      <div>
        <label className="label">Document title *</label>
        <input className="input-field" value={form.title}
          onChange={e => setForm(p => ({ ...p, title: e.target.value }))} placeholder="e.g. Blood test report Nov 2026" />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Category *</label>
          <select className="input-field" value={form.category} onChange={e => setForm(p => ({ ...p, category: e.target.value }))}>
            {CATEGORIES.map(c => <option key={c} value={c}>{CAT_LABELS[c]}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Document date</label>
          <input type="date" className="input-field" value={form.documentDate}
            onChange={e => setForm(p => ({ ...p, documentDate: e.target.value }))} />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Doctor name</label>
          <input className="input-field" value={form.doctorName}
            onChange={e => setForm(p => ({ ...p, doctorName: e.target.value }))} placeholder="Dr. Kumar" />
        </div>
        <div>
          <label className="label">Hospital / Lab</label>
          <input className="input-field" value={form.hospitalName}
            onChange={e => setForm(p => ({ ...p, hospitalName: e.target.value }))} placeholder="Apollo Hospital" />
        </div>
      </div>

      <div>
        <label className="label">Notes</label>
        <textarea rows={2} className="input-field resize-none" value={form.notes}
          onChange={e => setForm(p => ({ ...p, notes: e.target.value }))} placeholder="Optional notes about this document…" />
      </div>

      <div className="flex gap-3 justify-end pt-2">
        <button type="button" onClick={onCancel} className="btn-secondary">Cancel</button>
        <button type="submit" disabled={loading || !file} className="btn-primary flex items-center gap-2">
          {loading && <Spinner size="sm" />}
          {loading ? 'Uploading…' : 'Upload document'}
        </button>
      </div>
    </form>
  )
}
