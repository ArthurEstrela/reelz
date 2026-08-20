import { api } from './api'
import type {
  BillingPlan,
  BillingPlanCode,
  BillingSubscription,
  CheckoutResponse,
} from '../types/billing'

export async function getBillingPlans(): Promise<BillingPlan[]> {
  const { data } = await api.get<BillingPlan[]>('/api/v1/billing/plans')
  return data
}

export async function getCurrentSubscription(): Promise<BillingSubscription> {
  const { data } = await api.get<BillingSubscription>('/api/v1/billing/subscription')
  return data
}

export async function createSubscriptionCheckout(planCode: BillingPlanCode): Promise<CheckoutResponse> {
  const { data } = await api.post<CheckoutResponse>('/api/v1/billing/checkout', { planCode })
  return data
}

export async function cancelCurrentSubscription(): Promise<BillingSubscription> {
  const { data } = await api.post<BillingSubscription>('/api/v1/billing/subscription/cancel')
  return data
}
