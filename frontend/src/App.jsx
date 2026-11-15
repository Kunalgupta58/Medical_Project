import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/layout/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'
import LandingPage from './pages/landing/LandingPage'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import SchedulePage from './pages/medications/SchedulePage'
import MedicationsPage from './pages/medications/MedicationsPage'
import VaultPage from './pages/vault/VaultPage'
import AppointmentsPage from './pages/appointments/AppointmentsPage'
import NotificationsPage from './pages/notifications/NotificationsPage'
import EmergencyPage from './pages/emergency/EmergencyPage'
import CareTeamPage from './pages/caregiver/CareTeamPage'
import ProfilePage from './pages/profile/ProfilePage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/"         element={<LandingPage />} />
          <Route path="/login"    element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Protected app */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard"      element={<DashboardPage />} />
              <Route path="/medications"    element={<MedicationsPage />} />
              <Route path="/schedule"       element={<SchedulePage />} />
              <Route path="/vault"          element={<VaultPage />} />
              <Route path="/appointments"   element={<AppointmentsPage />} />
              <Route path="/caregiver"      element={<CareTeamPage />} />
              <Route path="/notifications"  element={<NotificationsPage />} />
              <Route path="/emergency"      element={<EmergencyPage />} />
              <Route path="/profile"        element={<ProfilePage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
