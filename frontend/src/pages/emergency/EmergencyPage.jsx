import { useState, useEffect } from 'react'
import { AlertTriangle, Phone, Plus, Shield } from 'lucide-react'
import { emergencyApi } from '../../api/caregiver'
import Modal from '../../components/ui/Modal'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'

export default function EmergencyPage() {
  const [contacts, setContacts] = useState([])
  const [sosResult, setSosResult] = useState(null)
  const [loading, setLoading]   = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [notes, setNotes]       = useState('')
  const [isDemo, setIsDemo]     = useState(true)
  const [addOpen, setAddOpen]   = useState(false)
  const [newContact, setNewContact] = useState({ name: '', phone: '', email: '', relationship: '', isPrimary: false })

  useEffect(() => {
    emergencyApi.getContacts()
      .then(r => setContacts(r.data))
      .catch(() => {})
  }, [])

  const handleSOS = async () => {
    setLoading(true)
    try {
      const { data } = await emergencyApi.triggerSOS({ notes, isDemo })
      setSosResult(data)
      setConfirmOpen(false)
    } catch (e) {
      setSosResult({ error: e?.response?.data?.message || 'SOS failed' })
    } finally {
      setLoading(false)
    }
  }

  const handleAddContact = async () => {
    try {
      const { data } = await emergencyApi.addContact(newContact)
      setContacts(c => [...c, data])
      setAddOpen(false)
      setNewContact({ name: '', phone: '', email: '', relationship: '', isPrimary: false })
    } catch {}
  }

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Emergency SOS</h1>
        <p className="text-gray-500 text-sm mt-0.5">Instantly alert your caregivers and family</p>
      </div>

      <Alert variant="info"
        message="The SOS button notifies all connected caregivers and emergency contacts via in-app notifications. External SMS/WhatsApp/email delivery requires additional configuration." />

      {/* Demo mode toggle */}
      <div className="card flex items-center justify-between">
        <div>
          <p className="font-medium text-gray-900">Demo mode</p>
          <p className="text-sm text-gray-500">Test SOS without alerting real contacts</p>
        </div>
        <button onClick={() => setIsDemo(!isDemo)}
          className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${isDemo ? 'bg-amber-400' : 'bg-primary-600'}`}>
          <span className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${isDemo ? 'translate-x-1' : 'translate-x-6'}`} />
        </button>
      </div>

      {/* Big SOS button */}
      <div className="card text-center py-10">
        {isDemo && (
          <div className="inline-flex items-center gap-2 bg-amber-100 text-amber-700 rounded-full px-4 py-1 text-sm font-medium mb-6">
            DEMO MODE — contacts will not be contacted
          </div>
        )}
        <button
          onClick={() => setConfirmOpen(true)}
          className={`mx-auto flex flex-col items-center justify-center h-40 w-40 rounded-full text-white font-bold text-xl shadow-2xl transition-transform active:scale-95
            ${isDemo ? 'bg-amber-500 hover:bg-amber-600' : 'bg-red-600 hover:bg-red-700'}`}
          aria-label="Emergency SOS button">
          <AlertTriangle className="h-10 w-10 mb-2" />
          SOS
        </button>
        <p className="text-gray-400 text-sm mt-6">Press and confirm to send emergency alert</p>
      </div>

      {/* SOS result */}
      {sosResult && (
        <div className={`card ${sosResult.error ? 'border-red-200 bg-red-50' : 'border-green-200 bg-green-50'}`}>
          <h3 className="font-semibold mb-2">{sosResult.error ? '❌ SOS Failed' : '✅ SOS Triggered'}</h3>
          {sosResult.error
            ? <p className="text-sm text-red-700">{sosResult.error}</p>
            : (
              <>
                <p className="text-sm text-green-800 mb-2">{sosResult.message}</p>
                {sosResult.isDemo && <p className="text-xs text-amber-600 font-medium mb-2">⚠ DEMO MODE — this was a test</p>}
                <p className="text-xs text-gray-500">{sosResult.deliveryNote}</p>
                <p className="text-xs text-gray-400 mt-1">{sosResult.timestamp}</p>
                {sosResult.contactsNotified?.length > 0 && (
                  <div className="mt-3">
                    <p className="text-xs font-medium text-gray-600 mb-1">Contacts in notification list:</p>
                    {sosResult.contactsNotified.map((c, i) => (
                      <p key={i} className="text-xs text-gray-500">· {c.name} ({c.relationship}) — in-app: {c.inAppNotified ? '✓' : '✗'}</p>
                    ))}
                  </div>
                )}
              </>
            )}
        </div>
      )}

      {/* Emergency contacts */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-900 flex items-center gap-2">
            <Phone className="h-4 w-4 text-primary-600" /> Emergency Contacts
          </h2>
          <button onClick={() => setAddOpen(true)} className="btn-secondary text-sm py-2 flex items-center gap-1">
            <Plus className="h-4 w-4" /> Add
          </button>
        </div>
        {contacts.length === 0 ? (
          <p className="text-gray-400 text-sm">No emergency contacts added yet.</p>
        ) : (
          <div className="space-y-3">
            {contacts.map((c, i) => (
              <div key={c.id || i} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50">
                <div className="rounded-full bg-primary-100 h-10 w-10 flex items-center justify-center font-semibold text-primary-700">
                  {c.name?.[0]}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900">{c.name} {c.primary && <span className="badge-blue ml-1">Primary</span>}</p>
                  <p className="text-sm text-gray-500">{c.phone}{c.relationship && ` · ${c.relationship}`}</p>
                </div>
                <a href={`tel:${c.phone}`} className="p-2 text-green-600 hover:bg-green-50 rounded-lg">
                  <Phone className="h-4 w-4" />
                </a>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Confirm SOS modal */}
      <Modal open={confirmOpen} onClose={() => setConfirmOpen(false)} title="Confirm Emergency SOS" size="sm">
        <div className="space-y-4">
          {isDemo && (
            <Alert variant="warning" message="DEMO MODE is on. This will NOT contact real emergency services or send real SMS." />
          )}
          {!isDemo && (
            <Alert variant="error" message="This will send real emergency alerts to all connected caregivers. Only trigger if this is a genuine emergency." />
          )}
          <div>
            <label className="label">Optional message</label>
            <textarea rows={2} className="input-field resize-none" value={notes}
              onChange={e => setNotes(e.target.value)} placeholder="e.g. Chest pain, fell in bathroom…" />
          </div>
          <div className="flex gap-3">
            <button onClick={() => setConfirmOpen(false)} className="btn-secondary flex-1">Cancel</button>
            <button onClick={handleSOS} disabled={loading}
              className={`flex-1 flex items-center justify-center gap-2 font-semibold py-3 rounded-xl text-white transition-colors
                ${isDemo ? 'bg-amber-500 hover:bg-amber-600' : 'bg-red-600 hover:bg-red-700'}`}>
              {loading ? <Spinner size="sm" /> : <AlertTriangle className="h-4 w-4" />}
              {loading ? 'Sending…' : (isDemo ? 'Send Demo SOS' : 'SEND EMERGENCY SOS')}
            </button>
          </div>
        </div>
      </Modal>

      {/* Add contact modal */}
      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="Add Emergency Contact" size="sm">
        <div className="space-y-3">
          {['name','phone','email','relationship'].map(f => (
            <div key={f}>
              <label className="label capitalize">{f}</label>
              <input className="input-field" value={newContact[f]}
                onChange={e => setNewContact(p => ({ ...p, [f]: e.target.value }))} />
            </div>
          ))}
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" className="accent-primary-600"
              checked={newContact.isPrimary} onChange={e => setNewContact(p => ({ ...p, isPrimary: e.target.checked }))} />
            <span className="text-sm">Primary contact</span>
          </label>
          <div className="flex gap-3 pt-2">
            <button onClick={() => setAddOpen(false)} className="btn-secondary flex-1">Cancel</button>
            <button onClick={handleAddContact} className="btn-primary flex-1">Add contact</button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
