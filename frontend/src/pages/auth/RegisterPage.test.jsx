import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RegisterPage from './RegisterPage'

const { mockRegister } = vi.hoisted(() => ({ mockRegister: vi.fn() }))

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({ register: mockRegister }),
}))

function renderRegisterPage() {
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>
  )
}

describe('RegisterPage', () => {
  beforeEach(() => {
    mockRegister.mockReset()
  })

  it('explains why an invalid form was not submitted', async () => {
    renderRegisterPage()

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Please correct the highlighted fields before continuing.')).toBeInTheDocument()
    expect(mockRegister).not.toHaveBeenCalled()
  })

  it('shows a useful message when the backend cannot be reached', async () => {
    mockRegister.mockRejectedValueOnce({ request: {} })
    renderRegisterPage()

    fireEvent.change(screen.getByPlaceholderText('Ramesh'), { target: { value: 'Asha' } })
    fireEvent.change(screen.getByPlaceholderText('Sharma'), { target: { value: 'Patel' } })
    fireEvent.change(screen.getByPlaceholderText('ramesh@example.com'), { target: { value: 'asha@example.com' } })
    fireEvent.change(screen.getByPlaceholderText('At least 8 characters'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => expect(mockRegister).toHaveBeenCalledOnce())
    expect(await screen.findByText('Could not reach the backend. Check your connection and try again.')).toBeInTheDocument()
  })
})
