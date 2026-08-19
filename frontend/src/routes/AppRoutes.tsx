import { lazy, Suspense } from 'react'
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
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { ResetPasswordPage } from '../pages/ResetPasswordPage'
import { VerifyEmailPage } from '../pages/VerifyEmailPage'
import { AccountPage } from '../pages/AccountPage'
import { AchievementsPage } from '../pages/AchievementsPage'
import { PrivateRoute } from './PrivateRoute'
import { PublicRoute } from './PublicRoute'

const SocialJoinPage = lazy(() => import('../pages/SocialJoinPage').then((module) => ({
  default: module.SocialJoinPage,
})))
const SocialLobbyPage = lazy(() => import('../pages/SocialLobbyPage').then((module) => ({
  default: module.SocialLobbyPage,
})))
const SocialRoomPage = lazy(() => import('../pages/SocialRoomPage').then((module) => ({
  default: module.SocialRoomPage,
})))

export function AppRoutes() {
  return (
    <Suspense fallback={<div className="grid min-h-svh place-items-center bg-canvas text-sm font-bold text-white/55">Abrindo sessão…</div>}>
      <Routes>
      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
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
        <Route path="/achievements" element={<AchievementsPage />} />
        <Route path="/account" element={<AccountPage />} />
        <Route path="/social" element={<SocialLobbyPage />} />
        <Route path="/social/join/:inviteCode" element={<SocialJoinPage />} />
        <Route path="/social/rooms/:roomId" element={<SocialRoomPage />} />
      </Route>

      <Route element={<PrivateRoute requiredRole="ADMIN" />}>
        <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  )
}
