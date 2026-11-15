import { Link } from 'react-router-dom'
import {
  Heart, Pill, FileText, Users, Bell, AlertTriangle,
  Calendar, Shield, ChevronRight, CheckCircle, Star
} from 'lucide-react'

const PROBLEMS = [
  { problem: 'Forgetting scheduled medicines',          solution: 'Scheduled reminders with snooze and caregiver escalation' },
  { problem: 'Difficulty maintaining medication routine', solution: 'Visual daily schedule with one-tap dose confirmation' },
  { problem: 'Family not knowing if dose was taken',    solution: 'Real-time caregiver dashboard and push alerts' },
  { problem: 'Prescriptions scattered in physical files', solution: 'Secure Medical Vault with categorized digital storage' },
  { problem: 'Difficulty finding old records',          solution: 'Searchable medical history timeline' },
  { problem: 'Lack of family coordination',             solution: 'Invitation-based family and caregiver connections' },
  { problem: 'Missing doctor appointments',             solution: 'Appointment management with configurable reminders' },
  { problem: 'Delayed emergency assistance',            solution: 'One-tap SOS with instant caregiver notification' },
]

const FEATURES = [
  { icon: Pill,          title: 'Medication Schedules',   desc: 'Create and track medication schedules. Confirm doses, snooze reminders, review history.' },
  { icon: Bell,          title: 'Smart Reminders',        desc: 'Configurable reminders with automatic caregiver escalation for unconfirmed doses.' },
  { icon: FileText,      title: 'Medical Vault',          desc: 'Secure digital storage for prescriptions, blood reports, MRI, discharge summaries and more.' },
  { icon: Shield,        title: 'Local OCR',              desc: 'Extract prescription text using local Tesseract OCR. No external AI APIs. Manual verification required.' },
  { icon: Users,         title: 'Family Coordination',    desc: 'Connect family members and caregivers via invitation. Control access per document.' },
  { icon: Calendar,      title: 'Appointments',           desc: 'Schedule and track doctor appointments with status management and reminders.' },
  { icon: AlertTriangle, title: 'Emergency SOS',          desc: 'One-tap SOS triggers instant notifications to all connected caregivers.' },
  { icon: Heart,         title: 'Health Dashboard',       desc: 'Real medication adherence charts backed by actual dose records — no fabricated numbers.' },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Nav */}
      <nav className="border-b border-gray-100 sticky top-0 bg-white/95 backdrop-blur z-40">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="rounded-xl bg-primary-600 p-1.5"><Heart className="h-5 w-5 text-white" /></div>
            <span className="font-bold text-gray-900">ElderCare 360</span>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/login" className="text-gray-600 hover:text-gray-900 font-medium text-sm px-4 py-2">Login</Link>
            <Link to="/register" className="btn-primary text-sm py-2 px-5">Get Started</Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="bg-gradient-to-br from-primary-50 via-white to-teal-50 py-20 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 bg-primary-100 text-primary-700 rounded-full px-4 py-1.5 text-sm font-medium mb-6">
            <Star className="h-4 w-4" /> PromptWars × Catalyst Hackathon
          </div>
          <h1 className="text-4xl md:text-5xl font-bold text-gray-900 leading-tight mb-4">
            Intelligent Elderly Care &<br />
            <span className="text-primary-600">Medical Record Management</span>
          </h1>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto mb-8">
            One platform connecting elderly users, family members and caregivers — with medication scheduling,
            secure medical records, local OCR and emergency SOS.
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link to="/register" className="btn-primary flex items-center justify-center gap-2">
              Start Free <ChevronRight className="h-4 w-4" />
            </Link>
            <Link to="/login" className="btn-secondary">Already have an account?</Link>
          </div>
        </div>
      </section>

      {/* Problems We Solve */}
      <section className="py-20 px-4 bg-gray-50">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-3xl font-bold text-gray-900 text-center mb-3">Problems We Solve</h2>
          <p className="text-gray-500 text-center mb-12">Every feature maps directly to a real elderly-care challenge.</p>
          <div className="grid md:grid-cols-2 gap-4">
            {PROBLEMS.map(({ problem, solution }, i) => (
              <div key={i} className="card flex gap-4">
                <CheckCircle className="h-5 w-5 text-primary-600 mt-0.5 shrink-0" />
                <div>
                  <p className="text-sm font-semibold text-gray-700 mb-0.5">❌ {problem}</p>
                  <p className="text-sm text-gray-500">✅ {solution}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-20 px-4">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-3xl font-bold text-gray-900 text-center mb-3">Platform Features</h2>
          <p className="text-gray-500 text-center mb-12">Built for real use, not screenshots.</p>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {FEATURES.map(({ icon: Icon, title, desc }) => (
              <div key={title} className="card text-center hover:shadow-md transition-shadow">
                <div className="inline-flex items-center justify-center rounded-2xl bg-primary-50 p-3 mb-4">
                  <Icon className="h-6 w-6 text-primary-600" />
                </div>
                <h3 className="font-semibold text-gray-900 mb-2">{title}</h3>
                <p className="text-sm text-gray-500 leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="bg-primary-600 py-16 px-4 text-center">
        <h2 className="text-3xl font-bold text-white mb-4">Ready to get started?</h2>
        <p className="text-primary-100 mb-8 max-w-xl mx-auto">
          Register as an elderly user, family member or caregiver and experience the platform today.
        </p>
        <Link to="/register" className="bg-white text-primary-700 font-semibold px-8 py-3 rounded-xl hover:bg-primary-50 transition-colors inline-block">
          Create your account
        </Link>
      </section>

      <footer className="bg-gray-900 text-gray-400 text-center py-8 text-sm">
        <p>© 2026 ElderCare 360 — PromptWars × Catalyst Hackathon Submission</p>
        <p className="mt-1 text-xs">Uses synthetic demo data. No real patient information is stored in this demo.</p>
      </footer>
    </div>
  )
}
