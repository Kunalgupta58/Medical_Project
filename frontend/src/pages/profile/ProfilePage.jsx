import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { User, Shield, LogOut } from 'lucide-react'
import Alert from '../../components/ui/Alert'
import { useNavigate } from 'react-router-dom'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [toast, setToast] = useState('')

  const showToast = (m) => { setToast(m); setTimeout(() => setToast(''), 3000) }

  const handleLogout = () => { logout(); navigate('/') }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {toast && <div className="fixed top-4 right-4 z-50 bg-gray-900 text-white px-5 py-3 rounded-xl shadow-lg text-sm">{toast}</div>}

      <h1 className="text-2xl font-bold text-gray-900">Profile & Settings</h1>

      {/* Profile card */}
      <div className="card">
        <div className="flex items-center gap-4 mb-6">
          <div className="h-16 w-16 rounded-2xl bg-primary-100 flex items-center justify-center">
            <span className="text-2xl font-bold text-primary-700">{user?.firstName?.[0]}{user?.lastName?.[0]}</span>
          </div>
          <div>
            <h2 className="text-xl font-bold text-gray-900">{user?.firstName} {user?.lastName}</h2>
            <p className="text-gray-500 capitalize">{user?.role?.replace('_', ' ').toLowerCase()}</p>
            <p className="text-sm text-gray-400">{user?.email}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 text-sm">
          <div className="bg-gray-50 rounded-xl p-3">
            <p className="text-gray-400 text-xs mb-1">User ID</p>
            <p className="font-mono text-xs text-gray-600 break-all">{user?.userId}</p>
          </div>
          <div className="bg-gray-50 rounded-xl p-3">
            <p className="text-gray-400 text-xs mb-1">Role</p>
            <p className="font-medium text-gray-900 capitalize">{user?.role?.replace('_', ' ').toLowerCase()}</p>
          </div>
        </div>
      </div>

      {/* Security */}
      <div className="card">
        <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <Shield className="h-4 w-4 text-primary-600" /> Security
        </h2>
        <div className="space-y-3 text-sm">
          <div className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
            <span className="text-gray-700">Authentication</span>
            <span className="badge-green">JWT Active</span>
          </div>
          <div className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
            <span className="text-gray-700">Password</span>
            <span className="badge-blue">BCrypt encrypted</span>
          </div>
          <div className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
            <span className="text-gray-700">Medical documents</span>
            <span className="badge-green">Private storage</span>
          </div>
        </div>
      </div>

      {/* Disclaimer */}
      <Alert variant="info"
        message="ElderCare 360 uses synthetic demo data for this hackathon demonstration. Do not store real patient information in a demo deployment." />

      {/* Logout */}
      <button onClick={handleLogout}
        className="w-full btn-danger flex items-center justify-center gap-2">
        <LogOut className="h-4 w-4" /> Sign out
      </button>
    </div>
  )
}
