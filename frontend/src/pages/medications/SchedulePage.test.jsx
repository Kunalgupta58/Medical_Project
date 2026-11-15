import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SchedulePage from './SchedulePage'
import { medicationsApi } from '../../api/medications'

vi.mock('../../api/medications', () => ({
  medicationsApi: {
    getTodaySchedule: vi.fn(),
    markTaken: vi.fn(),
    snoozeDose: vi.fn(),
    skipDose: vi.fn(),
  },
}))

const dose = {
  id: 'dose-1',
  medicationName: 'Metformin',
  dosageText: '500mg',
  medicineType: 'TABLET',
  scheduledDatetime: '2026-01-01T08:00:00Z',
  status: 'PENDING',
  snoozeCount: 0,
}

describe('SchedulePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    medicationsApi.getTodaySchedule.mockResolvedValue({ data: [dose] })
  })

  it('updates the displayed dose after the API confirms it was taken', async () => {
    medicationsApi.markTaken.mockResolvedValue({
      data: { ...dose, status: 'TAKEN', takenAt: '2026-01-01T08:01:00Z' },
    })
    render(<SchedulePage />)

    fireEvent.click(await screen.findByRole('button', { name: /Mark taken/i }))

    await waitFor(() => expect(medicationsApi.markTaken).toHaveBeenCalledWith('dose-1'))
    expect(await screen.findByText(/Confirmed at\s*08:01 AM/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Mark taken/i })).not.toBeInTheDocument()
  })

  it('shows a useful error when the schedule cannot be loaded', async () => {
    medicationsApi.getTodaySchedule.mockRejectedValueOnce(new Error('offline'))
    render(<SchedulePage />)

    expect(await screen.findByText("Failed to load today's schedule")).toBeInTheDocument()
  })

  it('does not offer another snooze after the maximum count', async () => {
    medicationsApi.getTodaySchedule.mockResolvedValueOnce({
      data: [{ ...dose, status: 'SNOOZED', snoozeCount: 3 }],
    })
    render(<SchedulePage />)

    expect(await screen.findByText(/Snoozed \(3\/3\)/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Snooze 15m/i })).not.toBeInTheDocument()
  })
})
