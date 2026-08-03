import { Navigate, Route, Routes } from 'react-router'
import { HomePage } from '../pages/HomePage'
import { AdminAnalyticsPage } from '../pages/AdminAnalyticsPage'
import { LibraryPage } from '../pages/LibraryPage'
import { LoginPage } from '../pages/LoginPage'
import { OnboardingPage } from '../pages/OnboardingPage'
import { AboutPage } from '../pages/AboutPage'
import { PrivacyPage } from '../pages/PrivacyPage'
import { RegisterPage } from '../pages/RegisterPage'
import { TermsPage } from '../pages/TermsPage'
import { PrivateRoute } from './PrivateRoute'
import { PublicRoute } from './PublicRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route path="/about" element={<AboutPage />} />
      <Route path="/terms" element={<TermsPage />} />
      <Route path="/privacy" element={<PrivacyPage />} />

      <Route element={<PrivateRoute mode="onboarding" />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
      </Route>

      <Route element={<PrivateRoute />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/library" element={<LibraryPage />} />
      </Route>

      <Route element={<PrivateRoute requiredRole="ADMIN" />}>
        <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
