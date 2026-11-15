import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Plus, Trash2 } from 'lucide-react'
import { medicationsApi } from '../../api/medications'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'

const TYPES = ['TABLET','CAPSULE','SYRUP','INJECTION','DROPS','INHALER','PATCH','CREAM','OTHER']
const FREQS = [
  { value: 'ONCE_DAILY',        label: 'Once daily' },
  { value: 'TWICE_DAILY',       label: 'Twice daily' },
  { value: 'THREE_TIMES_DAILY', label: 'Three times daily' },
  { value: 'FOUR_TIMES_DAILY',  label: 'Four times daily' },
  { value: 'EVERY_8_HOURS',     label: 'Every 8 hours' },
  { value: 'EVERY_12_HOURS',    label: 'Every 12 hours' },
  { value: 'WEEKLY',            label: 'Weekly' },
  { value: 'AS_NEEDED',         label: 'As needed (PRN)' },
  { value: 'CUSTOM',            label: 'Custom' },
]
const COLORS = ['#3B82F6','#10B981','#F59E0B','#EF4444','#8B5CF6','#EC4899','#06B6D4']

export default function MedicationForm({ initial, onSaved, onCancel }) {
  const [times, setTimes] = useState(initial?.scheduledTimes || ['08:00'])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: {
      name:            initial?.name || '',
      genericName:     initial?.genericName || '',
      dosageText:      initial?.dosageText || '',
      medicineType:    initial?.medicineType || 'TABLET',
      frequency:       initial?.frequency || 'ONCE_DAILY',
      startDate:       initial?.startDate || new Date().toISOString().slice(0, 10),
      endDate:         initial?.endDate || '',
      instructions:    initial?.instructions || '',
      prescribingDoctor: initial?.prescribingDoctor || '',
      notes:           initial?.notes || '',
      color:           initial?.color || '#3B82F6',
    }
  })

  const onSubmit = async (data) => {
    setError('')
    setLoading(true)
    const payload = { ...data, scheduledTimes: times.filter(t => t), endDate: data.endDate || null }
    try {
      if (initial) {
        await medicationsApi.update(initial.id, payload)
      } else {
        await medicationsApi.create(payload)
      }
      onSaved()
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to save medication')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
      {error && <Alert variant="error" message={error} />}

      <Alert variant="warning"
        message="Do not enter medication information from memory. Only enter data exactly as written on your prescription." />

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Medicine name *</label>
          <input className={`input-field ${errors.name ? 'border-red-400' : ''}`}
            {...register('name', { required: 'Name is required' })} placeholder="e.g. Metformin" />
          {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name.message}</p>}
        </div>
        <div>
          <label className="label">Generic name</label>
          <input className="input-field" {...register('genericName')} placeholder="e.g. metformin hydrochloride" />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Dosage *</label>
          <input className={`input-field ${errors.dosageText ? 'border-red-400' : ''}`}
            {...register('dosageText', { required: 'Dosage is required' })} placeholder="e.g. 500mg" />
          {errors.dosageText && <p className="text-red-500 text-xs mt-1">{errors.dosageText.message}</p>}
        </div>
        <div>
          <label className="label">Type *</label>
          <select className="input-field" {...register('medicineType', { required: true })}>
            {TYPES.map(t => <option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</option>)}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Frequency *</label>
          <select className="input-field" {...register('frequency', { required: true })}>
            {FREQS.map(f => <option key={f.value} value={f.value}>{f.label}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Color tag</label>
          <div className="flex gap-2 mt-1">
            {COLORS.map(c => (
              <label key={c} className="cursor-pointer">
                <input type="radio" value={c} className="sr-only" {...register('color')} />
                <div className="h-7 w-7 rounded-full border-2 border-white shadow-sm transition-transform hover:scale-110"
                  style={{ backgroundColor: c }} />
              </label>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Start date *</label>
          <input type="date" className={`input-field ${errors.startDate ? 'border-red-400' : ''}`}
            {...register('startDate', { required: 'Start date required' })} />
          {errors.startDate && <p className="text-red-500 text-xs mt-1">{errors.startDate.message}</p>}
        </div>
        <div>
          <label className="label">End date (leave blank if ongoing)</label>
          <input type="date" className="input-field" {...register('endDate')} />
        </div>
      </div>

      <div>
        <label className="label">Scheduled times</label>
        <div className="space-y-2">
          {times.map((t, i) => (
            <div key={i} className="flex gap-2">
              <input type="time" className="input-field flex-1" value={t}
                onChange={e => setTimes(ts => ts.map((v, j) => j === i ? e.target.value : v))} />
              {times.length > 1 && (
                <button type="button" onClick={() => setTimes(ts => ts.filter((_, j) => j !== i))}
                  className="p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg">
                  <Trash2 className="h-4 w-4" />
                </button>
              )}
            </div>
          ))}
          <button type="button" onClick={() => setTimes(ts => [...ts, '12:00'])}
            className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1">
            <Plus className="h-3 w-3" /> Add another time
          </button>
        </div>
      </div>

      <div>
        <label className="label">Instructions</label>
        <input className="input-field" {...register('instructions')} placeholder="e.g. Take after food" />
      </div>

      <div>
        <label className="label">Prescribing doctor</label>
        <input className="input-field" {...register('prescribingDoctor')} placeholder="Dr. Name" />
      </div>

      <div>
        <label className="label">Notes</label>
        <textarea rows={2} className="input-field resize-none" {...register('notes')} placeholder="Any additional notes…" />
      </div>

      <div className="flex gap-3 justify-end pt-2">
        <button type="button" onClick={onCancel} className="btn-secondary">Cancel</button>
        <button type="submit" disabled={loading} className="btn-primary flex items-center gap-2">
          {loading && <Spinner size="sm" />}
          {loading ? 'Saving…' : (initial ? 'Save changes' : 'Add medication')}
        </button>
      </div>
    </form>
  )
}
