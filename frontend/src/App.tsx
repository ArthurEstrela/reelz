import { BrowserRouter } from 'react-router'
import { PwaStatusPrompt } from './components/pwa/PwaStatusPrompt'
import { AuthProvider } from './context/AuthContext'
import { AchievementProvider } from './context/AchievementContext'
import { AppRoutes } from './routes/AppRoutes'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AchievementProvider>
          <AppRoutes />
          <PwaStatusPrompt />
        </AchievementProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
