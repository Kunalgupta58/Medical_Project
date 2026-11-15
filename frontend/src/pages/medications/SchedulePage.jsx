import { useEffect, useState } from 'react'
import { CheckCircle, Clock, XCircle, SkipForward, Info } from 'lucide-react'
import { medicationsApi } from '../../api/medications'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'
import Modal from '../../components/ui/Modal'
import { fmt, DOSE_STATUS_CONFIG } from '../../utils/format'

export default function SchedulePage() {
  const [doses, setDoses]     = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')
  const [toast, setToast]     = useState('')
  const [skipModal, setSkipModal] = useState(null)
  const [skipReason, setSkipReason] = useState('')
  const [acting, setActing]   = useState(null)

  const load = () => {
    setLoading(true)
    medicationsApi.getTodaySchedule()
      .then(r => setDoses(r.data))
      .catch(() => setError('Failed to load today\'s schedule'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const showToast = (msg) => { setToast(msg); setTimeout(() => setToast(''), 3000) }

  const handleTaken = async (id) => {
    setActing(id)
    try {
      const { data } = await medicationsApi.markTaken(id)
      setDoses(d => d.map(dose => dose.id === id ? { ...dose, ...data } : dose))
      showToast('✅ Dose marked as taken')
    } catch (e) {
      showToast('❌ ' + (e?.response?.data?.message || 'Failed to update dose'))
    } finally {
      setActing(null)
    }
  }

  const handleSnooze = async (id) => {
    setActing(id)
    try {
      const { data } = await medicationsApi.snoozeDose(id)
      setDoses(d => d.map(dose => dose.id === id ? { ...dose, ...data } : dose))
      showToast('⏰ Reminder snoozed for 15 minutes')
    } catch (e) {
      showToast('❌ ' + (e?.response?.data?.message || 'Could not snooze'))
    } finally {
      setActing(null)
    }
  }

  const handleSkip = async () => {
    setActing(skipModal)
    try {
      const { data } = await medicationsApi.skipDose(skipModal, skipReason)
      setDoses(d => d.map(dose => dose.id === skipModal ? { ...dose, ...data } : dose))
      showToast('⏭ Dose skipped')
    } catch (e) {
      showToast('❌ ' + (e?.response?.data?.message || 'Could not skip'))
    } finally {
      setActing(null)
      setSkipModal(null)
      setSkipReason('')
    }
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Spinner size="lg" /></div>

  const groups = {
    upcoming: doses.filter(d => d.status === 'PENDING' || d.status === 'SNOOZED'),
    taken:    doses.filter(d => d.status === 'TAKEN'),
    missed:   doses.filter(d => d.status === 'SKIPPED' || d.status === 'UNCONFIRMED'),
  }

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-gray-900 text-white px-5 py-3 rounded-xl shadow-lg text-sm animate-pulse">
          {toast}
        </div>
      )}

      <div>
        <h1 className="text-2xl font-bold text-gray-900">Today's Medication Schedule</h1>
        <p className="text-gray-500 text-sm mt-0.5">
          {new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          {' — '}{doses.length} dose{doses.length !== 1 ? 's' : ''} scheduled
        </p>
      </div>

      {error && <Alert variant="error" message={error} />}

      <Alert variant="info"
        message="Important: Mark a dose as taken only after you have actually taken the medication. Never double a missed dose without consulting your doctor." />

      {doses.length === 0 && (
        <div className="card text-center py-12">
          <Clock className="h-10 w-10 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">No doses scheduled for today. Add medications to see your schedule.</p>
        </div>
      )}

      {Object.entries({ upcoming: 'Upcoming / Active', taken: 'Taken', missed: 'Missed / Skipped' }).map(([key, label]) => (
        groups[key].length > 0 && (
          <div key={key}>
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{label}</h2>
            <div className="space-y-3">
              {groups[key].map(dose => (
                <DoseCard key={dose.id} dose={dose} acting={acting}
                  onTaken={handleTaken} onSnooze={handleSnooze} onSkip={id => setSkipModal(id)} />
              ))}
            </div>
          </div>
        )
      ))}

      {/* Skip reason modal */}
      <Modal open={!!skipModal} onClose={() => { setSkipModal(null); setSkipReason('') }} title="Skip this dose">
        <p className="text-sm text-gray-600 mb-4">
          You can optionally record why you are skipping this dose. For missed-dose decisions, please consult your prescribing doctor.
        </p>
        <label className="label">Reason (optional)</label>
        <input className="input-field mb-4" value={skipReason} onChange={e => setSkipReason(e.target.value)}
          placeholder="e.g. Side effects, already took alternate…" />
        <div className="flex gap-3 justify-end">
          <button className="btn-secondary" onClick={() => { setSkipModal(null); setSkipReason('') }}>Cancel</button>
          <button className="btn-danger" onClick={handleSkip} disabled={!!acting}>
            {acting ? <Spinner size="sm" /> : 'Skip dose'}
          </button>
        </div>
      </Modal>
    </div>
  )
}

function DoseCard({ dose, acting, onTaken, onSnooze, onSkip }) {
  const cfg = DOSE_STATUS_CONFIG[dose.status] || DOSE_STATUS_CONFIG.PENDING
  const isBusy = acting === dose.id
  const canAct = dose.status === 'PENDING' || dose.status === 'SNOOZED'

  return (
    <div className={`card flex gap-4 ${dose.status === 'UNCONFIRMED' ? 'border-red-200 bg-red-50' : ''}`}>
      <div className="w-1 rounded-full shrink-0" style={{ backgroundColor: dose.color || '#3B82F6' }} />
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="font-semibold text-gray-900 text-base">{dose.medicationName}</p>
            <p className="text-sm text-gray-600">{dose.dosageText} · {dose.medicineType}</p>
            {dose.instructions && (
              <p className="text-xs text-gray-400 mt-0.5 flex items-center gap-1">
                <Info className="h-3 w-3" />{dose.instructions}
              </p>
            )}
          </div>
          <div className="shrink-0 text-right">
            <span className={cfg.color}>{cfg.label}</span>
            <p className="text-xs text-gray-500 mt-1">{fmt.time(dose.scheduledDatetime)}</p>
          </div>
        </div>

        {dose.status === 'TAKEN' && dose.takenAt && (
          <p className="text-xs text-green-600 mt-2">✓ Confirmed at {fmt.time(dose.takenAt)}
            {dose.confirmedByName ? ` by ${dose.confirmedByName}` : ''}
          </p>
        )}
        {dose.status === 'SNOOZED' && (
          <p className="text-xs text-blue-600 mt-2">Snoozed ({dose.snoozeCount}/3) — reminder at {fmt.time(dose.snoozedUntil)}</p>
        )}
        {dose.status === 'UNCONFIRMED' && (
          <p className="text-xs text-red-600 mt-2">⚠ Dose not confirmed — caregiver has been notified</p>
        )}

        {canAct && (
          <div className="flex gap-2 mt-3">
            <button onClick={() => onTaken(dose.id)} disabled={isBusy}
              className="flex items-center gap-1.5 bg-green-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors">
              {isBusy ? <Spinner size="sm" /> : <CheckCircle className="h-4 w-4" />}
              Mark taken
            </button>
            {dose.snoozeCount < 3 && (
              <button onClick={() => onSnooze(dose.id)} disabled={isBusy}
                className="flex items-center gap-1.5 bg-amber-100 text-amber-800 text-sm px-4 py-2 rounded-lg hover:bg-amber-200 disabled:opacity-50 transition-colors">
                <Clock className="h-4 w-4" /> Snooze 15m
              </button>
            )}
            <button onClick={() => onSkip(dose.id)} disabled={isBusy}
              className="flex items-center gap-1.5 bg-gray-100 text-gray-700 text-sm px-4 py-2 rounded-lg hover:bg-gray-200 disabled:opacity-50 transition-colors">
              <SkipForward className="h-4 w-4" /> Skip
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
