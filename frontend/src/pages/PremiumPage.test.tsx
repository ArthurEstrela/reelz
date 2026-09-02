import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createSubscriptionCheckout,
  getBillingPlans,
  getCurrentSubscription,
} from '../services/billingService'
import { PremiumPage } from './PremiumPage'

vi.mock('../services/billingService', () => ({
  getBillingPlans: vi.fn(),
  getCurrentSubscription: vi.fn(),
  createSubscriptionCheckout: vi.fn(),
  cancelCurrentSubscription: vi.fn(),
}))

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ user: { email: 'premium@cinegiro.app', role: 'USER' }, logout: vi.fn() }),
}))

describe('PremiumPage', () => {
  beforeEach(() => {
    vi.mocked(getBillingPlans).mockResolvedValue([
      {
        code: 'PREMIUM_MONTHLY',
        name: 'Premium mensal',
        priceCents: 1290,
        currency: 'BRL',
        interval: 'MONTHLY',
        available: true,
        recommended: false,
        features: ['Giros ilimitados'],
      },
      {
        code: 'PREMIUM_ANNUAL',
        name: 'Premium anual',
        priceCents: 9990,
        currency: 'BRL',
        interval: 'ANNUAL',
        available: true,
        recommended: true,
        features: ['Giros ilimitados'],
      },
    ])
    vi.mocked(getCurrentSubscription).mockResolvedValue({
      accountPlan: 'FREE',
      premium: false,
      planCode: null,
      status: null,
      amountCents: 0,
      currency: 'BRL',
      currentPeriodEnd: null,
      canceledAt: null,
      checkoutUrl: null,
      cancelable: false,
    })
  })

  it('loads plans and starts checkout through the backend', async () => {
    const assign = vi.fn()
    vi.stubGlobal('location', { ...window.location, assign })
    vi.mocked(createSubscriptionCheckout).mockResolvedValue({
      planCode: 'PREMIUM_ANNUAL',
      checkoutUrl: 'https://pay.example/checkout',
      reused: false,
    })
    render(<MemoryRouter><PremiumPage /></MemoryRouter>)

    expect(await screen.findByText('Premium anual')).toBeInTheDocument()
    const buttons = screen.getAllByRole('button', { name: 'Assinar Premium' })
    await userEvent.click(buttons[1])

    await waitFor(() => expect(createSubscriptionCheckout).toHaveBeenCalledWith('PREMIUM_ANNUAL'))
    expect(assign).toHaveBeenCalledWith('https://pay.example/checkout')
    vi.unstubAllGlobals()
  })
})
