import { AlertCircle, CheckCircle, Info, XCircle, X } from 'lucide-react'
import { useState } from 'react'

const VARIANTS = {
  success: { bg: 'bg-green-50 border-green-200',  text: 'text-green-800',  icon: CheckCircle,   iconColor: 'text-green-500' },
  error:   { bg: 'bg-red-50 border-red-200',      text: 'text-red-800',    icon: XCircle,       iconColor: 'text-red-500' },
  warning: { bg: 'bg-amber-50 border-amber-200',  text: 'text-amber-800',  icon: AlertCircle,   iconColor: 'text-amber-500' },
  info:    { bg: 'bg-blue-50 border-blue-200',    text: 'text-blue-800',   icon: Info,          iconColor: 'text-blue-500' },
}

export default function Alert({ variant = 'info', message, dismissible = false }) {
  const [dismissed, setDismissed] = useState(false)
  if (!message || dismissed) return null
  const { bg, text, icon: Icon, iconColor } = VARIANTS[variant]
  return (
    <div className={`flex items-start gap-3 rounded-xl border p-4 ${bg}`}>
      <Icon className={`mt-0.5 h-5 w-5 shrink-0 ${iconColor}`} />
      <p className={`flex-1 text-sm font-medium ${text}`}>{message}</p>
      {dismissible && (
        <button onClick={() => setDismissed(true)} className={`ml-auto ${text} hover:opacity-70`}>
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  )
}
