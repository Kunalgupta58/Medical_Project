import { useEffect, useState } from 'react'
import { Plus, Calendar, Edit, XCircle } from 'lucide-react'
import { appointmentsApi } from '../../api/appointments'
import Modal from '../../components/ui/Modal'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import EmptyState from '../../components/ui/EmptyState'
import { fmt, APPOINTMENT_STATUS_CONFIG } from '../../utils/format'
import { useForm } from 'react-hook-form'

export default function AppointmentsPage() {
  const [appts, setAppts]     = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')
  const [toast, setToast]     = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing]   = useState(null)
  const [cancelling, setCancelling] = useState(null)

  const showToast = (m) => { setToast(m); setTimeout(() => setToast(''), 3000) }
  const load = () => {
    setLoading(true)
    appointmentsApi.getAll()
      .then(r => setAppts(r.data))
      .catch(() => setError('Failed to load appointments'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleCancel = async () => {
    try { await appointmentsApi.cancel(cancelling); load(); showToast('Appointment cancelled') }
    catch { showToast('Failed to cancel') }
    finally { setCancelling(null) }
  }

  const upcoming = appts.filter(a => a.status === 'SCHEDULED' && new Date(a.appointmentDatetime) >= new Date())
  const past     = appts.filter(a => a.status !== 'SCHEDULED' || new Date(a.appointmentDatetime) < new Date())

  if (loading) return <div className="flex items-center justify-center py-20"><Spinner size="lg" /></div>

  return (
    <div className="space-y-6">
      {toast && <div className="fixed top-4 right-4 z-50 bg-gray-900 text-white px-5 py-3 rounded-xl shadow-lg text-sm">{toast}</div>}

      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Appointments</h1>
        <button onClick={() => { setEditing(null); setShowForm(true) }} className="btn-primary flex items-center gap-2">
          <Plus className="h-4 w-4" /> New Appointment
        </button>
      </div>

      {error && <Alert variant="error" message={error} />}

      {appts.length === 0 ? (
        <EmptyState icon={Calendar} title="No appointments yet"
          description="Schedule your first doctor appointment."
          action={<button onClick={() => setShowForm(true)} className="btn-primary">Schedule Appointment</button>} />
      ) : (
        <>
          {upcoming.length > 0 && (
            <Section title="Upcoming" appts={upcoming}
              onEdit={a => { setEditing(a); setShowForm(true) }} onCancel={id => setCancelling(id)} />
          )}
          {past.length > 0 && (
            <Section title="Past / Cancelled" appts={past} past
              onEdit={a => { setEditing(a); setShowForm(true) }} onCancel={id => setCancelling(id)} />
          )}
        </>
      )}

      <Modal open={showForm} onClose={() => { setShowForm(false); setEditing(null) }}
        title={editing ? 'Edit Appointment' : 'New Appointment'} size="md">
        <ApptForm initial={editing}
          onSaved={() => { setShowForm(false); setEditing(null); load(); showToast('Appointment saved') }}
          onCancel={() => { setShowForm(false); setEditing(null) }} />
      </Modal>

      <ConfirmDialog open={!!cancelling} onClose={() => setCancelling(null)} onConfirm={handleCancel}
        title="Cancel Appointment" danger message="Mark this appointment as cancelled?" confirmLabel="Cancel appointment" />
    </div>
  )
}

function Section({ title, appts, past = false, onEdit, onCancel }) {
  return (
    <div>
      <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{title}</h2>
      <div className="space-y-3">
        {appts.map(a => {
          const cfg = APPOINTMENT_STATUS_CONFIG[a.status] || {}
          return (
            <div key={a.id} className="card flex gap-4">
              <div className="rounded-xl bg-primary-50 p-3 shrink-0">
                <Calendar className="h-5 w-5 text-primary-600" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-semibold text-gray-900">{a.doctorName}</p>
                    {a.hospitalName && <p className="text-sm text-gray-500">{a.hospitalName}</p>}
                    <p className="text-sm text-primary-600 font-medium mt-0.5">{fmt.datetime(a.appointmentDatetime)}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{a.purpose}</p>
                    {a.notes && <p className="text-xs text-gray-400">{a.notes}</p>}
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <span className={cfg.color || 'badge-gray'}>{cfg.label || a.status}</span>
                    {!past && a.status === 'SCHEDULED' && (
                      <>
                        <button onClick={() => onEdit(a)} className="p-2 text-gray-400 hover:text-primary-600 rounded-lg">
                          <Edit className="h-4 w-4" />
                        </button>
                        <button onClick={() => onCancel(a.id)} className="p-2 text-gray-400 hover:text-red-600 rounded-lg">
                          <XCircle className="h-4 w-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

function ApptForm({ initial, onSaved, onCancel }) {
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)
  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: {
      doctorName: initial?.doctorName || '',
      hospitalName: initial?.hospitalName || '',
      appointmentDatetime: initial?.appointmentDatetime
        ? new Date(initial.appointmentDatetime).toISOString().slice(0, 16)
        : '',
      purpose: initial?.purpose || '',
      notes: initial?.notes || '',
    }
  })

  const onSubmit = async (data) => {
    setError(''); setLoading(true)
    const payload = { ...data, appointmentDatetime: new Date(data.appointmentDatetime).toISOString() }
    try {
      if (initial) await appointmentsApi.update(initial.id, payload)
      else await appointmentsApi.create(payload)
      onSaved()
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to save appointment')
    } finally { setLoading(false) }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      {error && <Alert variant="error" message={error} />}
      <div>
        <label className="label">Doctor name *</label>
        <input className={`input-field ${errors.doctorName ? 'border-red-400' : ''}`}
          {...register('doctorName', { required: 'Doctor name required' })} placeholder="Dr. Kumar" />
        {errors.doctorName && <p className="text-red-500 text-xs mt-1">{errors.doctorName.message}</p>}
      </div>
      <div>
        <label className="label">Hospital / Clinic</label>
        <input className="input-field" {...register('hospitalName')} placeholder="Apollo Hospital" />
      </div>
      <div>
        <label className="label">Date & time *</label>
        <input type="datetime-local" className={`input-field ${errors.appointmentDatetime ? 'border-red-400' : ''}`}
          {...register('appointmentDatetime', { required: 'Date and time required' })} />
        {errors.appointmentDatetime && <p className="text-red-500 text-xs mt-1">{errors.appointmentDatetime.message}</p>}
      </div>
      <div>
        <label className="label">Purpose *</label>
        <input className={`input-field ${errors.purpose ? 'border-red-400' : ''}`}
          {...register('purpose', { required: 'Purpose required' })} placeholder="e.g. Diabetes follow-up" />
        {errors.purpose && <p className="text-red-500 text-xs mt-1">{errors.purpose.message}</p>}
      </div>
      <div>
        <label className="label">Notes</label>
        <textarea rows={2} className="input-field resize-none" {...register('notes')} placeholder="Any additional information…" />
      </div>
      <div className="flex gap-3 justify-end pt-2">
        <button type="button" onClick={onCancel} className="btn-secondary">Cancel</button>
        <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2">
          {loading && <Spinner size="sm" />}
          {loading ? 'Saving…' : (initial ? 'Save changes' : 'Schedule appointment')}
        </button>
      </div>
    </form>
  )
}
