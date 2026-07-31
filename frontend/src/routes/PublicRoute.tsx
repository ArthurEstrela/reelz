import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../hooks/useAuth'

export function PublicRoute() {
  const { isAuthenticated, user } = useAuth()
  if (!isAuthenticated) return <Outlet />
  return <Navigate to={user?.onboardingCompleted ? '/' : '/onboarding'} replace />
}
