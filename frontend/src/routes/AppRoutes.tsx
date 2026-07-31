import { Navigate, Route, Routes } from 'react-router'
import { HomePage } from '../pages/HomePage'
import { LibraryPage } from '../pages/LibraryPage'
import { LoginPage } from '../pages/LoginPage'
import { OnboardingPage } from '../pages/OnboardingPage'
import { RegisterPage } from '../pages/RegisterPage'
import { PrivateRoute } from './PrivateRoute'
import { PublicRoute } from './PublicRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route element={<PrivateRoute mode="onboarding" />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
      </Route>

      <Route element={<PrivateRoute />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/library" element={<LibraryPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
