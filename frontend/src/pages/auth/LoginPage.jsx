import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { Heart, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPwd, setShowPwd] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm()

  const onSubmit = async (data) => {
    setError('')
    setLoading(true)
    try {
      await login(data.email, data.password)
      navigate('/dashboard')
    } catch (err) {
      setError(err?.response?.data?.message || 'Invalid email or password')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-teal-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-8">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center rounded-2xl bg-primary-600 p-3 mb-3">
            <Heart className="h-7 w-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Welcome back</h1>
          <p className="text-gray-500 text-sm mt-1">Sign in to ElderCare 360</p>
        </div>

        <Alert variant="error" message={error} dismissible />

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-4" noValidate>
          <div>
            <label className="label">Email address</label>
            <input
              type="email"
              autoComplete="email"
              className={`input-field ${errors.email ? 'border-red-400' : ''}`}
              placeholder="your@email.com"
              {...register('email', { required: 'Email is required', pattern: { value: /^\S+@\S+\.\S+$/, message: 'Invalid email' } })}
            />
            {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email.message}</p>}
          </div>

          <div>
            <label className="label">Password</label>
            <div className="relative">
              <input
                type={showPwd ? 'text' : 'password'}
                autoComplete="current-password"
                className={`input-field pr-10 ${errors.password ? 'border-red-400' : ''}`}
                placeholder="••••••••"
                {...register('password', { required: 'Password is required' })}
              />
              <button type="button" onClick={() => setShowPwd(!showPwd)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                {showPwd ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
          </div>

          <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
            {loading ? <Spinner size="sm" /> : null}
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          No account?{' '}
          <Link to="/register" className="text-primary-600 font-medium hover:text-primary-700">Create one</Link>
        </p>
        <p className="text-center mt-2">
          <Link to="/" className="text-xs text-gray-400 hover:text-gray-600">← Back to home</Link>
        </p>
      </div>
    </div>
  )
}
