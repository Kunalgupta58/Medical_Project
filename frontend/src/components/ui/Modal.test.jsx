import { fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import Modal from './Modal'

function ControlledModal({ onClose }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <button onClick={() => setOpen(true)}>Open modal</button>
      <Modal open={open} onClose={() => { onClose(); setOpen(false) }} title="Confirm action">
        <button>Confirm</button>
      </Modal>
    </>
  )
}

describe('Modal', () => {
  it('labels the dialog and keeps keyboard focus inside it', () => {
    render(
      <>
        <button>Open modal</button>
        <Modal open onClose={vi.fn()} title="Confirm action">
          <button>Confirm</button>
        </Modal>
      </>
    )

    const dialog = screen.getByRole('dialog', { name: 'Confirm action' })
    const closeButton = screen.getByRole('button', { name: 'Close' })
    const confirmButton = screen.getByRole('button', { name: 'Confirm' })

    expect(closeButton).toHaveFocus()
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true })
    expect(confirmButton).toHaveFocus()

    fireEvent.keyDown(document, { key: 'Tab' })
    expect(closeButton).toHaveFocus()
    expect(dialog).toHaveAttribute('aria-modal', 'true')
  })

  it('closes on Escape and restores focus to the opener', () => {
    const onClose = vi.fn()
    render(<ControlledModal onClose={onClose} />)

    const opener = screen.getByRole('button', { name: 'Open modal' })
    opener.focus()
    fireEvent.click(opener)
    fireEvent.keyDown(document, { key: 'Escape' })

    expect(onClose).toHaveBeenCalledOnce()
    expect(opener).toHaveFocus()
  })
})
