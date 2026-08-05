import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { SlotMachine } from './SlotMachine'

describe('SlotMachine', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('keeps communicating progress while the spin is running', () => {
    vi.useFakeTimers()
    render(<SlotMachine />)

    expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByText('Procurando a sessão perfeita…')).toBeInTheDocument()

    act(() => vi.advanceTimersByTime(1_300))

    expect(screen.getByText('Cruzando seus streamings…')).toBeInTheDocument()
  })
})
