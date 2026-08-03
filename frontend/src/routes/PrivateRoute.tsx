import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../hooks/useAuth'

interface PrivateRouteProps {
  mode?: 'completed' | 'onboarding'
  requiredRole?: 'ADMIN'
}

export function PrivateRoute({ mode = 'completed', requiredRole }: PrivateRouteProps) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (mode === 'completed' && !user?.onboardingCompleted) {
    return <Navigate to="/onboarding" replace />
  }

  if (mode === 'onboarding' && user?.onboardingCompleted) {
    return <Navigate to="/" replace />
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
