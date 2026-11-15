/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
        },
        eldercare: {
          teal:   '#0d9488',
          blue:   '#1d4ed8',
          purple: '#7c3aed',
          green:  '#16a34a',
          amber:  '#d97706',
          red:    '#dc2626',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        'elder-sm':   ['1rem',    { lineHeight: '1.5rem' }],
        'elder-base': ['1.125rem', { lineHeight: '1.75rem' }],
        'elder-lg':   ['1.25rem', { lineHeight: '1.75rem' }],
        'elder-xl':   ['1.5rem',  { lineHeight: '2rem' }],
      },
    },
  },
  plugins: [],
}
