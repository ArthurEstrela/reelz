export type BillingPlanCode = 'PREMIUM_MONTHLY' | 'PREMIUM_ANNUAL'
export type BillingSubscriptionStatus =
  | 'CHECKOUT_PENDING'
  | 'ACTIVE'
  | 'PAST_DUE'
  | 'CANCELED'
  | 'EXPIRED'

export interface BillingPlan {
  code: BillingPlanCode
  name: string
  priceCents: number
  currency: string
  interval: 'MONTHLY' | 'ANNUAL'
  available: boolean
  recommended: boolean
  features: string[]
}

export interface BillingSubscription {
  accountPlan: 'FREE' | 'PREMIUM'
  premium: boolean
  planCode: BillingPlanCode | null
  status: BillingSubscriptionStatus | null
  amountCents: number
  currency: string
  currentPeriodEnd: string | null
  canceledAt: string | null
  checkoutUrl: string | null
  cancelable: boolean
}

export interface CheckoutResponse {
  planCode: BillingPlanCode
  checkoutUrl: string
  reused: boolean
}
