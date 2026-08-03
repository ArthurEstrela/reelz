import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { submitBetaFeedback } from '../../services/feedbackService'
import { FeedbackDialog } from './FeedbackDialog'

vi.mock('../../services/feedbackService', () => ({
  submitBetaFeedback: vi.fn(),
}))

describe('FeedbackDialog', () => {
  beforeEach(() => vi.clearAllMocks())

  it('sends a score and optional comment without exposing identity', async () => {
    vi.mocked(submitBetaFeedback).mockResolvedValue(undefined)
    const user = userEvent.setup()

    render(<FeedbackDialog open onClose={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: '5: Demais' }))
    await user.type(screen.getByLabelText(/O que mais pesou/), 'Decidi rápido.')
    await user.click(screen.getByRole('button', { name: 'Enviar feedback' }))

    await waitFor(() => {
      expect(submitBetaFeedback).toHaveBeenCalledWith({
        score: 5,
        message: 'Decidi rápido.',
      })
    })
    expect(await screen.findByRole('heading', { name: 'Feedback recebido' })).toBeInTheDocument()
  })

  it('requires a score before submitting', async () => {
    const user = userEvent.setup()
    render(<FeedbackDialog open onClose={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Enviar feedback' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Escolha uma nota de 1 a 5.')
    expect(submitBetaFeedback).not.toHaveBeenCalled()
  })
})
