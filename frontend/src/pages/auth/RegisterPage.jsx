import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { Heart, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'

const ROLES = [
  { value: 'ELDERLY',      label: 'Elderly User',    desc: 'Manage my own medications and records' },
  { value: 'FAMILY_MEMBER', label: 'Family Member',  desc: 'Monitor a family member\'s health' },
  { value: 'CAREGIVER',    label: 'Caregiver',        desc: 'Professional care provider' },
]

export default function RegisterPage() {
  const { register: authRegister } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPwd, setShowPwd] = useState(false)

  const { register, handleSubmit, setFocus, watch, formState: { errors } } = useForm({ defaultValues: { role: 'ELDERLY' } })

  const onInvalid = (formErrors) => {
    setError('Please correct the highlighted fields before continuing.')
    const firstInvalidField = Object.keys(formErrors)[0]
    if (firstInvalidField) setFocus(firstInvalidField)
  }

  const onSubmit = async (data) => {
    setError('')
    setLoading(true)
    try {
      await authRegister({
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        password: data.password,
        role: data.role,
        phone: data.phone,
      })
      navigate('/dashboard')
    } catch (err) {
      const fieldErrors = Object.values(err?.response?.data?.errors ?? {}).filter(Boolean)
      const message = err?.response?.data?.message
      if (fieldErrors.length) {
        setError(fieldErrors.join(' '))
      } else if (message) {
        setError(message)
      } else if (!err?.response) {
        setError('Could not reach the backend. Check your connection and try again.')
      } else {
        setError(`Registration failed (HTTP ${err.response.status}). Please try again.`)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-teal-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg p-8">
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center rounded-2xl bg-primary-600 p-3 mb-3">
            <Heart className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Create your account</h1>
          <p className="text-gray-500 text-sm mt-1">Join ElderCare 360</p>
        </div>

        <Alert variant="error" message={error} dismissible />

        <form onSubmit={handleSubmit(onSubmit, onInvalid)} className="space-y-4 mt-4" noValidate>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label">First name</label>
              <input className={`input-field ${errors.firstName ? 'border-red-400' : ''}`}
                placeholder="Ramesh"
                {...register('firstName', { required: 'Required', maxLength: { value: 100, message: 'Too long' } })} />
              {errors.firstName && <p className="text-red-500 text-xs mt-1">{errors.firstName.message}</p>}
            </div>
            <div>
              <label className="label">Last name</label>
              <input className={`input-field ${errors.lastName ? 'border-red-400' : ''}`}
                placeholder="Sharma"
                {...register('lastName', { required: 'Required', maxLength: { value: 100, message: 'Too long' } })} />
              {errors.lastName && <p className="text-red-500 text-xs mt-1">{errors.lastName.message}</p>}
            </div>
          </div>

          <div>
            <label className="label">Email address</label>
            <input type="email" className={`input-field ${errors.email ? 'border-red-400' : ''}`}
              placeholder="ramesh@example.com"
              {...register('email', { required: 'Email required', pattern: { value: /^\S+@\S+\.\S+$/, message: 'Invalid email' } })} />
            {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email.message}</p>}
          </div>

          <div>
            <label className="label">Phone (optional)</label>
            <input type="tel" className="input-field" placeholder="+91 98765 43210"
              {...register('phone')} />
          </div>

          <div>
            <label className="label">Password</label>
            <div className="relative">
              <input type={showPwd ? 'text' : 'password'}
                className={`input-field pr-10 ${errors.password ? 'border-red-400' : ''}`}
                placeholder="At least 8 characters"
                {...register('password', { required: 'Password required', minLength: { value: 8, message: 'Minimum 8 characters' } })} />
              <button type="button" onClick={() => setShowPwd(!showPwd)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                {showPwd ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
          </div>

          <div>
            <label className="label">I am a…</label>
            <div className="grid gap-2">
              {ROLES.map(({ value, label, desc }) => (
                <label key={value} className={`flex items-start gap-3 p-3 rounded-xl border cursor-pointer transition-colors
                  ${watch('role') === value ? 'border-primary-400 bg-primary-50' : 'border-gray-200 hover:border-gray-300'}`}>
                  <input type="radio" value={value} className="mt-1 accent-primary-600" {...register('role', { required: true })} />
                  <div>
                    <p className="font-medium text-sm text-gray-900">{label}</p>
                    <p className="text-xs text-gray-500">{desc}</p>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
            {loading && <Spinner size="sm" />}
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          Have an account?{' '}
          <Link to="/login" className="text-primary-600 font-medium hover:text-primary-700">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
