import { useEffect, useState } from 'react'
import { Pill, FileText, Calendar, Users, Bell, TrendingUp, CheckCircle, XCircle, Clock } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'
import { dashboardApi } from '../../api/dashboard'
import { useAuth } from '../../context/AuthContext'
import StatCard from '../../components/ui/StatCard'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'
import { fmt } from '../../utils/format'

const DOSE_COLORS = { taken: '#16a34a', pending: '#d97706', skipped: '#6b7280', unconfirmed: '#dc2626' }

export default function DashboardPage() {
  const { user, isElderly } = useAuth()
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState('')

  useEffect(() => {
    dashboardApi.overview()
      .then(r => setData(r.data))
      .catch(() => setError('Failed to load dashboard. Please refresh.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-20"><Spinner size="lg" /></div>
  if (error)   return <Alert variant="error" message={error} />

  const pieData = [
    { name: 'Taken',       value: data.takenToday },
    { name: 'Pending',     value: data.pendingToday },
    { name: 'Skipped',     value: data.skippedToday },
    { name: 'Unconfirmed', value: data.unconfirmedToday },
  ].filter(d => d.value > 0)

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Good {getGreeting()}, {user?.firstName}
        </h1>
        <p className="text-gray-500 text-sm mt-0.5">
          {new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        </p>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Pill}         label="Today's doses"      value={data.totalScheduledToday} color="blue" />
        <StatCard icon={CheckCircle}  label="Taken today"        value={data.takenToday}          color="green" />
        <StatCard icon={XCircle}      label="Skipped / missed"   value={data.skippedToday + data.unconfirmedToday} color="red" />
        <StatCard icon={TrendingUp}   label="7-day adherence"    value={`${data.adherencePercentage}%`} color="teal"
          sub={`Based on confirmed doses`} />
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Pill}         label="Active medications"  value={data.activeMedications}   color="purple" />
        <StatCard icon={Calendar}     label="Upcoming appts."    value={data.upcomingAppointments} color="amber" />
        <StatCard icon={Users}        label="Pending tasks"      value={data.pendingCareTasks}     color="blue" />
        <StatCard icon={FileText}     label="Medical documents"  value={data.totalDocuments}       color="teal" />
      </div>

      {/* Charts */}
      <div className="grid lg:grid-cols-2 gap-6">
        {/* Weekly adherence */}
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-4">7-Day Medication Adherence</h2>
          {data.weeklyAdherence?.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={data.weeklyAdherence} margin={{ top: 4, right: 8, bottom: 0, left: -20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} tickFormatter={d => d.slice(5)} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip formatter={(v, n) => [v, n.charAt(0).toUpperCase() + n.slice(1)]} />
                <Bar dataKey="taken"   name="Taken"    fill="#16a34a" radius={[4,4,0,0]} />
                <Bar dataKey="missed"  name="Missed"   fill="#dc2626" radius={[4,4,0,0]} />
                <Legend />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-gray-400 text-sm text-center py-8">No schedule data for the past 7 days yet.</p>
          )}
        </div>

        {/* Today's dose pie */}
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-4">Today's Dose Status</h2>
          {pieData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={pieData} cx="50%" cy="50%" innerRadius={55} outerRadius={85}
                  dataKey="value" nameKey="name" paddingAngle={3}>
                  {pieData.map((entry) => (
                    <Cell key={entry.name} fill={DOSE_COLORS[entry.name.toLowerCase()] || '#94a3b8'} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-gray-400 text-sm text-center py-8">No doses scheduled today.</p>
          )}
        </div>
      </div>

      {/* Upcoming appointments + recent notifications */}
      <div className="grid lg:grid-cols-2 gap-6">
        {/* Appointments */}
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Calendar className="h-4 w-4 text-primary-600" /> Upcoming Appointments
          </h2>
          {data.nextAppointments?.length > 0 ? (
            <div className="space-y-3">
              {data.nextAppointments.map(a => (
                <div key={a.id} className="flex items-start gap-3 p-3 rounded-xl bg-gray-50">
                  <div className="rounded-lg bg-primary-100 p-2">
                    <Calendar className="h-4 w-4 text-primary-700" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-medium text-sm text-gray-900">{a.doctorName}</p>
                    <p className="text-xs text-gray-500">{a.purpose}</p>
                    <p className="text-xs text-primary-600 font-medium mt-0.5">{fmt.datetime(a.appointmentDatetime)}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-400 text-sm py-4">No upcoming appointments.</p>
          )}
        </div>

        {/* Notifications */}
        <div className="card">
          <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Bell className="h-4 w-4 text-primary-600" /> Recent Notifications
            {data.unreadNotifications > 0 && (
              <span className="ml-auto badge-red">{data.unreadNotifications} unread</span>
            )}
          </h2>
          {data.recentNotifications?.length > 0 ? (
            <div className="space-y-2">
              {data.recentNotifications.map(n => (
                <div key={n.id} className={`p-3 rounded-xl text-sm ${n.read ? 'bg-gray-50' : 'bg-primary-50 border border-primary-100'}`}>
                  <p className={`font-medium ${n.read ? 'text-gray-700' : 'text-gray-900'}`}>{n.title}</p>
                  <p className="text-xs text-gray-500 mt-0.5 line-clamp-1">{n.message}</p>
                  <p className="text-xs text-gray-400 mt-1">{fmt.relative(n.createdAt)}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-400 text-sm py-4">No notifications yet.</p>
          )}
        </div>
      </div>
    </div>
  )
}

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'morning'
  if (h < 17) return 'afternoon'
  return 'evening'
}
