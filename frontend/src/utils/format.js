import { format, formatDistanceToNow, parseISO, isValid } from 'date-fns'

export const fmt = {
  date: (d) => {
    if (!d) return '—'
    try { return format(typeof d === 'string' ? parseISO(d) : new Date(d), 'dd MMM yyyy') }
    catch { return d }
  },
  datetime: (d) => {
    if (!d) return '—'
    try { return format(typeof d === 'string' ? parseISO(d) : new Date(d), 'dd MMM yyyy, hh:mm a') }
    catch { return d }
  },
  time: (d) => {
    if (!d) return '—'
    try { return format(typeof d === 'string' ? parseISO(d) : new Date(d), 'hh:mm a') }
    catch { return d }
  },
  relative: (d) => {
    if (!d) return '—'
    try { return formatDistanceToNow(typeof d === 'string' ? parseISO(d) : new Date(d), { addSuffix: true }) }
    catch { return d }
  },
  fileSize: (bytes) => {
    if (!bytes) return '—'
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / 1024 ** 2).toFixed(1)} MB`
  },
}

export const DOSE_STATUS_CONFIG = {
  PENDING:     { label: 'Pending',      color: 'badge-amber', dotColor: 'bg-amber-400' },
  TAKEN:       { label: 'Taken',        color: 'badge-green', dotColor: 'bg-green-400' },
  SNOOZED:     { label: 'Snoozed',      color: 'badge-blue',  dotColor: 'bg-blue-400' },
  SKIPPED:     { label: 'Skipped',      color: 'badge-gray',  dotColor: 'bg-gray-400' },
  UNCONFIRMED: { label: 'Unconfirmed',  color: 'badge-red',   dotColor: 'bg-red-400' },
}

export const APPOINTMENT_STATUS_CONFIG = {
  SCHEDULED:   { label: 'Scheduled',   color: 'badge-blue' },
  COMPLETED:   { label: 'Completed',   color: 'badge-green' },
  CANCELLED:   { label: 'Cancelled',   color: 'badge-red' },
  RESCHEDULED: { label: 'Rescheduled', color: 'badge-amber' },
}

export const DOC_CATEGORY_LABELS = {
  PRESCRIPTION:       'Prescription',
  CBC_REPORT:         'CBC Report',
  BLOOD_GLUCOSE:      'Blood Glucose',
  HBA1C:              'HbA1c',
  KIDNEY_FUNCTION:    'Kidney Function',
  LIVER_FUNCTION:     'Liver Function',
  LIPID_PROFILE:      'Lipid Profile',
  XRAY:               'X-Ray',
  MRI:                'MRI',
  CT_SCAN:            'CT Scan',
  ULTRASOUND:         'Ultrasound',
  DISCHARGE_SUMMARY:  'Discharge Summary',
  CONSULTATION_NOTES: 'Consultation Notes',
  VACCINATION_RECORD: 'Vaccination Record',
  OTHER:              'Other',
}
