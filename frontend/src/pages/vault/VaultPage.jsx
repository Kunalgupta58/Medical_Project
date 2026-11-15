import { useEffect, useState, useRef } from 'react'
import { Upload, FileText, Search, Download, Trash2, Eye, Filter } from 'lucide-react'
import { vaultApi, ocrApi } from '../../api/vault'
import Modal from '../../components/ui/Modal'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import EmptyState from '../../components/ui/EmptyState'
import { fmt, DOC_CATEGORY_LABELS } from '../../utils/format'
import UploadForm from './UploadForm'
import OcrVerifyPanel from './OcrVerifyPanel'

export default function VaultPage() {
  const [docs, setDocs]           = useState([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState('')
  const [toast, setToast]         = useState('')
  const [search, setSearch]       = useState('')
  const [catFilter, setCatFilter] = useState('')
  const [showUpload, setShowUpload] = useState(false)
  const [ocrDoc, setOcrDoc]       = useState(null)
  const [deleting, setDeleting]   = useState(null)
  const [previewing, setPreviewing] = useState(null)
  const searchTimeout = useRef(null)

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000) }

  const load = () => {
    setLoading(true)
    vaultApi.getAll()
      .then(r => setDocs(r.data))
      .catch(() => setError('Failed to load documents'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleSearch = (q) => {
    clearTimeout(searchTimeout.current)
    setSearch(q)
    if (!q) { load(); return }
    searchTimeout.current = setTimeout(() => {
      vaultApi.search(q)
        .then(r => setDocs(r.data))
        .catch(() => {})
    }, 400)
  }

  const handleDelete = async () => {
    try {
      await vaultApi.delete(deleting)
      load()
      showToast('Document deleted')
    } catch { showToast('Failed to delete document') }
    finally { setDeleting(null) }
  }

  const handleDownload = async (doc) => {
    try {
      const { data } = await vaultApi.download(doc.id)
      const url = URL.createObjectURL(new Blob([data]))
      const a = document.createElement('a')
      a.href = url; a.download = doc.fileName; a.click()
      URL.revokeObjectURL(url)
    } catch { showToast('Download failed') }
  }

  const filtered = docs.filter(d => !catFilter || d.category === catFilter)
  const categories = [...new Set(docs.map(d => d.category))]

  return (
    <div className="space-y-6">
      {toast && <div className="fixed top-4 right-4 z-50 bg-gray-900 text-white px-5 py-3 rounded-xl shadow-lg text-sm">{toast}</div>}

      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Medical Vault</h1>
          <p className="text-gray-500 text-sm mt-0.5">{docs.length} document{docs.length !== 1 ? 's' : ''} stored securely</p>
        </div>
        <button onClick={() => setShowUpload(true)} className="btn-primary flex items-center gap-2">
          <Upload className="h-4 w-4" /> Upload Document
        </button>
      </div>

      {error && <Alert variant="error" message={error} />}

      {/* Search + filter */}
      <div className="flex gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input className="input-field pl-9" placeholder="Search documents…"
            value={search} onChange={e => handleSearch(e.target.value)} />
        </div>
        <select className="input-field w-auto" value={catFilter} onChange={e => setCatFilter(e.target.value)}>
          <option value="">All categories</option>
          {categories.map(c => <option key={c} value={c}>{DOC_CATEGORY_LABELS[c] || c}</option>)}
        </select>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20"><Spinner size="lg" /></div>
      ) : filtered.length === 0 ? (
        <EmptyState icon={FileText} title="No documents found"
          description="Upload your first medical document to build your secure digital health vault."
          action={<button onClick={() => setShowUpload(true)} className="btn-primary">Upload Document</button>} />
      ) : (
        <div className="space-y-3">
          {filtered.map(doc => (
            <DocCard key={doc.id} doc={doc}
              onPreview={() => setPreviewing(doc)}
              onDownload={() => handleDownload(doc)}
              onOcr={() => setOcrDoc(doc)}
              onDelete={() => setDeleting(doc.id)} />
          ))}
        </div>
      )}

      {/* Upload modal */}
      <Modal open={showUpload} onClose={() => setShowUpload(false)} title="Upload Medical Document" size="lg">
        <UploadForm onUploaded={() => { setShowUpload(false); load(); showToast('Document uploaded successfully') }}
          onCancel={() => setShowUpload(false)} />
      </Modal>

      {/* OCR verify modal */}
      <Modal open={!!ocrDoc} onClose={() => setOcrDoc(null)} title="OCR — Extract Prescription Text" size="xl">
        {ocrDoc && <OcrVerifyPanel doc={ocrDoc} onClose={() => setOcrDoc(null)} />}
      </Modal>

      {/* Preview modal */}
      <Modal open={!!previewing} onClose={() => setPreviewing(null)} title={previewing?.title} size="xl">
        {previewing && <DocPreview doc={previewing} onDownload={() => handleDownload(previewing)} />}
      </Modal>

      <ConfirmDialog open={!!deleting} onClose={() => setDeleting(null)} onConfirm={handleDelete}
        danger title="Delete Document"
        message="This permanently removes the document from your Medical Vault. The original file will be deleted. This cannot be undone."
        confirmLabel="Delete permanently" />
    </div>
  )
}

function DocCard({ doc, onPreview, onDownload, onOcr, onDelete }) {
  const ocr = doc.ocrStatus
  return (
    <div className="card flex gap-4">
      <div className="rounded-xl bg-primary-50 p-3 shrink-0">
        <FileText className="h-6 w-6 text-primary-600" />
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <p className="font-semibold text-gray-900 truncate">{doc.title}</p>
            <p className="text-sm text-gray-500">
              {DOC_CATEGORY_LABELS[doc.category] || doc.category}
              {doc.doctorName && ` · Dr. ${doc.doctorName}`}
              {doc.hospitalName && ` · ${doc.hospitalName}`}
            </p>
            <p className="text-xs text-gray-400 mt-0.5">
              {fmt.date(doc.documentDate)} · {doc.fileFormat} · {fmt.fileSize(doc.fileSizeBytes)}
            </p>
          </div>
          <div className="flex items-center gap-1 shrink-0">
            {doc.category === 'PRESCRIPTION' && (
              <button onClick={onOcr} title="Run OCR"
                className="p-2 text-purple-500 hover:bg-purple-50 rounded-lg transition-colors text-xs flex items-center gap-1">
                OCR
              </button>
            )}
            <button onClick={onPreview} title="Preview" className="p-2 text-gray-400 hover:text-primary-600 hover:bg-gray-50 rounded-lg">
              <Eye className="h-4 w-4" />
            </button>
            <button onClick={onDownload} title="Download" className="p-2 text-gray-400 hover:text-primary-600 hover:bg-gray-50 rounded-lg">
              <Download className="h-4 w-4" />
            </button>
            <button onClick={onDelete} title="Delete" className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg">
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function DocPreview({ doc, onDownload }) {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3 text-sm">
        <Info label="Category" value={DOC_CATEGORY_LABELS[doc.category] || doc.category} />
        <Info label="Date" value={fmt.date(doc.documentDate)} />
        {doc.doctorName && <Info label="Doctor" value={doc.doctorName} />}
        {doc.hospitalName && <Info label="Hospital / Lab" value={doc.hospitalName} />}
        <Info label="Format" value={doc.fileFormat} />
        <Info label="Size" value={fmt.fileSize(doc.fileSizeBytes)} />
        <Info label="Uploaded" value={fmt.datetime(doc.createdAt)} />
        <Info label="OCR Status" value={doc.ocrStatus?.replace(/_/g,' ').toLowerCase()} />
      </div>
      {doc.notes && (
        <div className="bg-gray-50 rounded-xl p-3">
          <p className="text-xs text-gray-400 mb-1">Notes</p>
          <p className="text-sm text-gray-700">{doc.notes}</p>
        </div>
      )}
      <div className="bg-amber-50 rounded-xl p-3 text-xs text-amber-700">
        For security reasons, document preview is not available in the web browser for this file type.
        Use the download button to view the original file.
      </div>
      <button onClick={onDownload} className="btn-primary flex items-center gap-2 w-full justify-center">
        <Download className="h-4 w-4" /> Download original file
      </button>
    </div>
  )
}

function Info({ label, value }) {
  return (
    <div>
      <p className="text-xs text-gray-400">{label}</p>
      <p className="font-medium text-gray-900">{value || '—'}</p>
    </div>
  )
}
