import { useEffect, useState } from 'react'
import { Users, Mail, CheckCircle, Trash2, Plus } from 'lucide-react'
import { relationshipsApi } from '../../api/caregiver'
import { useAuth } from '../../context/AuthContext'
import Modal from '../../components/ui/Modal'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import EmptyState from '../../components/ui/EmptyState'
import { fmt } from '../../utils/format'

export default function CareTeamPage() {
  const { isElderly, user } = useAuth()
  const [rels, setRels]       = useState([])
  const [invites, setInvites] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [inviteOpen, setInviteOpen] = useState(false)
  const [acceptOpen, setAcceptOpen] = useState(false)
  const [toast, setToast]     = useState('')

  const showToast = (m) => { setToast(m); setTimeout(() => setToast(''), 3000) }

  const load = () => {
    setLoading(true)
    Promise.all([
      relationshipsApi.getAll(),
      isElderly ? relationshipsApi.getInvites() : relationshipsApi.getPending(),
    ])
      .then(([r, inv]) => { setRels(r.data); setInvites(inv.data); setLoadError('') })
      .catch(() => setLoadError('Failed to load care team. Please refresh.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleRevoke = async (id) => {
    try { await relationshipsApi.revoke(id); load(); showToast('Access revoked') }
    catch { showToast('Failed to revoke access') }
  }

  const handleAccept = async (token) => {
    try { await relationshipsApi.accept(token); load(); showToast('Connected successfully') }
    catch (e) { showToast(e?.response?.data?.message || 'Failed to accept invitation') }
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Spinner size="lg" /></div>

  return (
    <div className="space-y-6">
      {toast && <div className="fixed top-4 right-4 z-50 bg-gray-900 text-white px-5 py-3 rounded-xl shadow-lg text-sm">{toast}</div>}
      {loadError && <Alert variant="error" message={loadError} />}

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Care Team</h1>
          <p className="text-gray-500 text-sm mt-0.5">Manage family members and caregivers connected to your profile</p>
        </div>
        <div className="flex gap-2">
          {!isElderly && (
            <button onClick={() => setAcceptOpen(true)} className="btn-secondary flex items-center gap-2">
              <CheckCircle className="h-4 w-4" /> Accept Invitation
            </button>
          )}
          {isElderly && (
            <button onClick={() => setInviteOpen(true)} className="btn-primary flex items-center gap-2">
              <Plus className="h-4 w-4" /> Invite Family / Caregiver
            </button>
          )}
        </div>
      </div>

      {/* Connected members */}
      <div className="card">
        <h2 className="font-semibold text-gray-900 mb-4">Connected Members ({rels.length})</h2>
        {rels.length === 0 ? (
          <EmptyState icon={Users} title="No connections yet"
            description={isElderly
              ? "Invite a family member or caregiver to get started."
              : "Accept an invitation from an elderly user to connect."} />
        ) : (
          <div className="space-y-3">
            {rels.map(r => (
              <div key={r.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50">
                <div className="h-10 w-10 rounded-full bg-primary-100 flex items-center justify-center font-semibold text-primary-700">
                  {isElderly ? r.relatedUser?.firstName?.[0] : r.elderlyUser?.firstName?.[0]}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900">
                    {isElderly ? `${r.relatedUser?.firstName} ${r.relatedUser?.lastName}` : `${r.elderlyUser?.firstName} ${r.elderlyUser?.lastName}`}
                  </p>
                  <p className="text-sm text-gray-500 capitalize">{r.relationshipType?.replace('_',' ').toLowerCase()}</p>
                  <p className="text-xs text-gray-400">Connected {fmt.relative(r.createdAt)}</p>
                </div>
                {isElderly && (
                  <button onClick={() => handleRevoke(r.id)}
                    className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors">
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Invitations */}
      {isElderly && invites.length > 0 && (
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-4">Sent Invitations</h2>
          <div className="space-y-2">
            {invites.map(inv => (
              <div key={inv.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50 text-sm">
                <Mail className="h-4 w-4 text-gray-400 shrink-0" />
                <div className="flex-1">
                  <p className="font-medium text-gray-900">{inv.inviteeEmail}</p>
                  <p className="text-xs text-gray-500 capitalize">{inv.inviteeRole?.replace('_',' ').toLowerCase()} · Expires {fmt.date(inv.expiresAt)}</p>
                </div>
                <span className={`text-xs font-medium px-2 py-1 rounded-full
                  ${inv.status === 'ACCEPTED' ? 'bg-green-100 text-green-700' :
                    inv.status === 'PENDING' ? 'bg-amber-100 text-amber-700' : 'bg-gray-100 text-gray-500'}`}>
                  {inv.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {!isElderly && invites.length > 0 && (
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-2">Invitations to you</h2>
          <p className="text-sm text-gray-500 mb-4">These invitations were sent to your signed-in email address.</p>
          <div className="space-y-3">
            {invites.map(inv => (
              <div key={inv.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50">
                <Mail className="h-4 w-4 text-gray-400 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900">{inv.elderlyUserName} invited you</p>
                  <p className="text-xs text-gray-500 capitalize">{inv.inviteeRole?.replace('_', ' ').toLowerCase()} · Expires {fmt.date(inv.expiresAt)}</p>
                  {inv.message && <p className="text-sm text-gray-600 mt-1">{inv.message}</p>}
                </div>
                <button onClick={() => handleAccept(inv.invitationToken)} className="btn-primary text-sm">
                  Accept
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
      {!isElderly && invites.length === 0 && (
        <p className="text-sm text-gray-500">
          No pending invitations for {user?.email}. Check that the elder invited this exact email address.
        </p>
      )}

      {/* Invite modal */}
      <Modal open={inviteOpen} onClose={() => setInviteOpen(false)} title="Invite Family Member or Caregiver" size="sm">
        <InviteForm onSent={() => { setInviteOpen(false); load(); showToast('Invitation sent') }}
          onCancel={() => setInviteOpen(false)} />
      </Modal>

      {/* Accept invitation modal */}
      <Modal open={acceptOpen} onClose={() => setAcceptOpen(false)} title="Accept Invitation" size="sm">
        <AcceptForm onAccepted={() => { setAcceptOpen(false); load(); showToast('Connected successfully') }}
          onCancel={() => setAcceptOpen(false)} />
      </Modal>
    </div>
  )
}

function InviteForm({ onSent, onCancel }) {
  const [form, setForm]     = useState({ email: '', role: 'FAMILY_MEMBER', message: '' })
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    if (!form.email) { setError('Email is required'); return }
    setError(''); setLoading(true)
    try {
      await relationshipsApi.invite(form)
      onSent()
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to send invitation')
    } finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      {error && <Alert variant="error" message={error} />}
      <p className="text-sm text-gray-600">
        The invitee must sign in or register with this email, then accept the invitation from their Care Team page.
        Email delivery is not configured in this local setup.
      </p>
      <div>
        <label className="label">Their email address</label>
        <input type="email" className="input-field" value={form.email}
          onChange={e => setForm(p => ({ ...p, email: e.target.value }))} placeholder="family@example.com" />
      </div>
      <div>
        <label className="label">Role</label>
        <select className="input-field" value={form.role} onChange={e => setForm(p => ({ ...p, role: e.target.value }))}>
          <option value="FAMILY_MEMBER">Family Member</option>
          <option value="CAREGIVER">Caregiver</option>
        </select>
      </div>
      <div>
        <label className="label">Message (optional)</label>
        <textarea rows={2} className="input-field resize-none" value={form.message}
          onChange={e => setForm(p => ({ ...p, message: e.target.value }))} placeholder="Personal message…" />
      </div>
      <div className="flex gap-3">
        <button onClick={onCancel} className="btn-secondary flex-1">Cancel</button>
        <button onClick={submit} disabled={loading} className="btn-primary flex-1 flex items-center justify-center gap-2">
          {loading && <Spinner size="sm" />} Send invitation
        </button>
      </div>
    </div>
  )
}

function AcceptForm({ onAccepted, onCancel }) {
  const [token, setToken]   = useState('')
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    if (!token) { setError('Token is required'); return }
    setError(''); setLoading(true)
    try {
      await relationshipsApi.accept(token.trim())
      onAccepted()
    } catch (e) {
      setError(e?.response?.data?.message || 'Invalid or expired invitation token')
    } finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      {error && <Alert variant="error" message={error} />}
      <p className="text-sm text-gray-600">Enter the invitation token you received from the elderly user.</p>
      <div>
        <label className="label">Invitation token</label>
        <input className="input-field font-mono text-sm" value={token}
          onChange={e => setToken(e.target.value)} placeholder="Paste invitation token here…" />
      </div>
      <div className="flex gap-3">
        <button onClick={onCancel} className="btn-secondary flex-1">Cancel</button>
        <button onClick={submit} disabled={loading} className="btn-primary flex-1 flex items-center justify-center gap-2">
          {loading && <Spinner size="sm" />} Accept invitation
        </button>
      </div>
    </div>
  )
}
