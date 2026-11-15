import { NavLink, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Pill, FileText, Calendar, Users,
  Bell, AlertTriangle, User, LogOut, Heart, ChevronLeft, Menu
} from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'

const NAV = [
  { to: '/dashboard',     label: 'Dashboard',       icon: LayoutDashboard },
  { to: '/medications',   label: 'Medications',      icon: Pill },
  { to: '/schedule',      label: 'Today\'s Schedule',icon: Pill },
  { to: '/vault',         label: 'Medical Vault',    icon: FileText },
  { to: '/appointments',  label: 'Appointments',     icon: Calendar },
  { to: '/caregiver',     label: 'Care Team',        icon: Users },
  { to: '/notifications', label: 'Notifications',    icon: Bell },
  { to: '/emergency',     label: 'Emergency SOS',    icon: AlertTriangle },
  { to: '/profile',       label: 'Profile',          icon: User },
]

export default function Sidebar({ unreadCount = 0 }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)

  const handleLogout = () => { logout(); navigate('/') }

  return (
    <aside className={`${collapsed ? 'w-16' : 'w-64'} flex flex-col bg-white border-r border-gray-100 shadow-sm transition-all duration-200 shrink-0`}>
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-gray-100">
        {!collapsed && (
          <div className="flex items-center gap-2">
            <div className="rounded-xl bg-primary-600 p-1.5">
              <Heart className="h-5 w-5 text-white" />
            </div>
            <span className="font-bold text-gray-900 text-sm">ElderCare 360</span>
          </div>
        )}
        <button onClick={() => setCollapsed(!collapsed)} className="text-gray-400 hover:text-gray-700 ml-auto">
          {collapsed ? <Menu className="h-5 w-5" /> : <ChevronLeft className="h-5 w-5" />}
        </button>
      </div>

      {/* User chip */}
      {!collapsed && user && (
        <div className="px-4 py-3 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-full bg-primary-100 flex items-center justify-center">
              <span className="text-primary-700 font-semibold text-sm">
                {user.firstName?.[0]}{user.lastName?.[0]}
              </span>
            </div>
            <div className="min-w-0">
              <p className="text-sm font-semibold text-gray-900 truncate">{user.firstName} {user.lastName}</p>
              <p className="text-xs text-gray-400 capitalize">{user.role?.replace('_', ' ').toLowerCase()}</p>
            </div>
          </div>
        </div>
      )}

      {/* Nav links */}
      <nav className="flex-1 overflow-y-auto py-3 px-2">
        {NAV.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-xl mb-0.5 text-sm font-medium transition-colors
               ${isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'}`
            }
            title={collapsed ? label : undefined}
          >
            <div className="relative shrink-0">
              <Icon className="h-5 w-5" />
              {label === 'Notifications' && unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 h-4 w-4 bg-red-500 rounded-full text-white text-[10px] flex items-center justify-center font-bold">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </div>
            {!collapsed && <span>{label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* Logout */}
      <div className="p-2 border-t border-gray-100">
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium text-red-600 hover:bg-red-50 transition-colors"
          title={collapsed ? 'Logout' : undefined}
        >
          <LogOut className="h-5 w-5 shrink-0" />
          {!collapsed && <span>Logout</span>}
        </button>
      </div>
    </aside>
  )
}
