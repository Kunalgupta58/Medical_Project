import { useEffect, useState } from 'react'
import { Bell, CheckCheck } from 'lucide-react'
import { notificationsApi } from '../../api/notifications'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'
import EmptyState from '../../components/ui/EmptyState'
import { fmt } from '../../utils/format'

const TYPE_COLORS = {
  MEDICATION_REMINDER: 'bg-blue-50 border-blue-200',
  CAREGIVER_ESCALATION: 'bg-red-50 border-red-200',
  EMERGENCY_ALERT: 'bg-red-50 border-red-300',
  APPOINTMENT_REMINDER: 'bg-purple-50 border-purple-200',
  INVITATION: 'bg-green-50 border-green-200',
  default: 'bg-gray-50 border-gray-200',
}

export default function NotificationsPage() {
  const [notifs, setNotifs]   = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')
  const [marking, setMarking] = useState(false)

  const load = () => {
    setLoading(true)
    notificationsApi.getAll()
      .then(r => setNotifs(r.data))
      .catch(() => setError('Failed to load notifications'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const markRead = async (id) => {
    try {
      const { data } = await notificationsApi.markRead(id)
      setNotifs(ns => ns.map(n => n.id === id ? { ...n, ...data } : n))
    } catch {
      setError('Failed to mark notification as read')
    }
  }

  const markAllRead = async () => {
    setMarking(true)
    try {
      await notificationsApi.markAllRead()
      setNotifs(ns => ns.map(n => ({ ...n, read: true })))
    } catch { setError('Failed to mark all as read') }
    finally { setMarking(false) }
  }

  const unread = notifs.filter(n => !n.read).length

  if (loading) return <div className="flex items-center justify-center py-20"><Spinner size="lg" /></div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>
          <p className="text-gray-500 text-sm mt-0.5">{unread} unread · {notifs.length} total</p>
        </div>
        {unread > 0 && (
          <button onClick={markAllRead} disabled={marking}
            className="btn-secondary flex items-center gap-2 text-sm">
            {marking ? <Spinner size="sm" /> : <CheckCheck className="h-4 w-4" />}
            Mark all read
          </button>
        )}
      </div>

      {error && <Alert variant="error" message={error} />}

      {notifs.length === 0 ? (
        <EmptyState icon={Bell} title="No notifications" description="You're all caught up!" />
      ) : (
        <div className="space-y-2">
          {notifs.map(n => {
            const bg = TYPE_COLORS[n.notificationType] || TYPE_COLORS.default
            return (
              <div key={n.id}
                className={`flex gap-3 p-4 rounded-xl border cursor-pointer transition-opacity ${bg} ${n.read ? 'opacity-70' : ''}`}
                onClick={() => !n.read && markRead(n.id)}>
                <div className={`mt-1 h-2 w-2 rounded-full shrink-0 ${n.read ? 'bg-gray-300' : 'bg-primary-500'}`} />
                <div className="flex-1 min-w-0">
                  <p className={`text-sm font-semibold ${n.read ? 'text-gray-600' : 'text-gray-900'}`}>{n.title}</p>
                  <p className="text-sm text-gray-500 mt-0.5 line-clamp-2">{n.message}</p>
                  <div className="flex items-center gap-3 mt-1">
                    <span className="text-xs text-gray-400">{fmt.relative(n.createdAt)}</span>
                    <span className="text-xs text-gray-300">·</span>
                    <span className="text-xs text-gray-400">{n.notificationType?.replace(/_/g,' ').toLowerCase()}</span>
                    {!n.emailSent && (
                      <span className="text-xs text-amber-500">In-app only</span>
                    )}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
