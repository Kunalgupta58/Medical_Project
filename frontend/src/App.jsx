import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/layout/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'

const LandingPage = lazy(() => import('./pages/landing/LandingPage'))
const LoginPage = lazy(() => import('./pages/auth/LoginPage'))
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage'))
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'))
const SchedulePage = lazy(() => import('./pages/medications/SchedulePage'))
const MedicationsPage = lazy(() => import('./pages/medications/MedicationsPage'))
const VaultPage = lazy(() => import('./pages/vault/VaultPage'))
const AppointmentsPage = lazy(() => import('./pages/appointments/AppointmentsPage'))
const NotificationsPage = lazy(() => import('./pages/notifications/NotificationsPage'))
const EmergencyPage = lazy(() => import('./pages/emergency/EmergencyPage'))
const CareTeamPage = lazy(() => import('./pages/caregiver/CareTeamPage'))
const ProfilePage = lazy(() => import('./pages/profile/ProfilePage'))

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Suspense fallback={<div className="p-6 text-center" role="status">Loading page...</div>}>
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
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  )
}
