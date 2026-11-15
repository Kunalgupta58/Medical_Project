import { useEffect, useState } from 'react'
import { Plus, Pill, Edit, Trash2, ChevronDown, ChevronUp } from 'lucide-react'
import { medicationsApi } from '../../api/medications'
import Modal from '../../components/ui/Modal'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import EmptyState from '../../components/ui/EmptyState'
import { fmt } from '../../utils/format'
import MedicationForm from './MedicationForm'

export default function MedicationsPage() {
  const [meds, setMeds]       = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')
  const [toast, setToast]     = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing]   = useState(null)
  const [deleting, setDeleting] = useState(null)

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000) }

  const load = () => {
    setLoading(true)
    medicationsApi.getAll()
      .then(r => setMeds(r.data))
      .catch(() => setError('Failed to load medications'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleSaved = () => { setShowForm(false); setEditing(null); load(); showToast('Medication saved') }

  const handleDelete = async () => {
    try {
      await medicationsApi.deactivate(deleting)
      load()
      showToast('Medication deactivated')
    } catch {
      showToast('Failed to deactivate medication')
    } finally {
      setDeleting(null)
    }
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Spinner size="lg" /></div>

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-gray-900 text-white px-5 py-3 rounded-xl shadow-lg text-sm">
          {toast}
        </div>
      )}

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Medications</h1>
          <p className="text-gray-500 text-sm mt-0.5">{meds.filter(m => m.active).length} active medications</p>
        </div>
        <button onClick={() => { setEditing(null); setShowForm(true) }} className="btn-primary flex items-center gap-2">
          <Plus className="h-4 w-4" /> Add Medication
        </button>
      </div>

      {error && <Alert variant="error" message={error} />}

      {meds.length === 0 ? (
        <EmptyState icon={Pill} title="No medications yet"
          description="Add your first medication to start tracking your schedule."
          action={<button onClick={() => setShowForm(true)} className="btn-primary">Add Medication</button>} />
      ) : (
        <div className="space-y-3">
          {meds.map(med => (
            <MedCard key={med.id} med={med}
              onEdit={() => { setEditing(med); setShowForm(true) }}
              onDelete={() => setDeleting(med.id)} />
          ))}
        </div>
      )}

      <Modal open={showForm} onClose={() => { setShowForm(false); setEditing(null) }}
        title={editing ? 'Edit Medication' : 'Add Medication'} size="lg">
        <MedicationForm initial={editing} onSaved={handleSaved} onCancel={() => { setShowForm(false); setEditing(null) }} />
      </Modal>

      <ConfirmDialog open={!!deleting} onClose={() => setDeleting(null)} onConfirm={handleDelete}
        title="Deactivate Medication" danger
        message="This will deactivate the medication and stop generating new dose reminders. Existing dose history is preserved."
        confirmLabel="Deactivate" />
    </div>
  )
}

function MedCard({ med, onEdit, onDelete }) {
  const [expanded, setExpanded] = useState(false)
  return (
    <div className={`card ${!med.active ? 'opacity-60' : ''}`}>
      <div className="flex items-start gap-3">
        <div className="rounded-xl p-2.5 mt-0.5" style={{ backgroundColor: (med.color || '#3B82F6') + '20' }}>
          <Pill className="h-5 w-5" style={{ color: med.color || '#3B82F6' }} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div>
              <p className="font-semibold text-gray-900">{med.name}
                {med.genericName && <span className="text-gray-400 font-normal text-sm ml-1">({med.genericName})</span>}
              </p>
              <p className="text-sm text-gray-600">{med.dosageText} · {med.medicineType} · {med.frequency?.replace(/_/g, ' ').toLowerCase()}</p>
              <p className="text-xs text-gray-400 mt-0.5">
                {fmt.date(med.startDate)} {med.endDate ? `→ ${fmt.date(med.endDate)}` : '(ongoing)'}
              </p>
            </div>
            <div className="flex items-center gap-2 shrink-0">
              <span className={med.active ? 'badge-green' : 'badge-gray'}>{med.active ? 'Active' : 'Inactive'}</span>
              <button onClick={onEdit} className="p-2 text-gray-400 hover:text-primary-600 hover:bg-gray-50 rounded-lg transition-colors">
                <Edit className="h-4 w-4" />
              </button>
              {med.active && (
                <button onClick={onDelete} className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors">
                  <Trash2 className="h-4 w-4" />
                </button>
              )}
              <button onClick={() => setExpanded(!expanded)} className="p-2 text-gray-400 hover:text-gray-600 rounded-lg">
                {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
              </button>
            </div>
          </div>

          {expanded && (
            <div className="mt-3 pt-3 border-t border-gray-100 grid grid-cols-2 gap-2 text-xs">
              {med.scheduledTimes?.length > 0 && (
                <div><span className="text-gray-400">Times: </span><span className="text-gray-700">{med.scheduledTimes.join(', ')}</span></div>
              )}
              {med.prescribingDoctor && (
                <div><span className="text-gray-400">Doctor: </span><span className="text-gray-700">{med.prescribingDoctor}</span></div>
              )}
              {med.instructions && (
                <div className="col-span-2"><span className="text-gray-400">Instructions: </span><span className="text-gray-700">{med.instructions}</span></div>
              )}
              {med.notes && (
                <div className="col-span-2"><span className="text-gray-400">Notes: </span><span className="text-gray-700">{med.notes}</span></div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
