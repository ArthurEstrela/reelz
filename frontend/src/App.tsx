import { BrowserRouter } from 'react-router'
import { PwaStatusPrompt } from './components/pwa/PwaStatusPrompt'
import { AuthProvider } from './context/AuthContext'
import { AppRoutes } from './routes/AppRoutes'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
        <PwaStatusPrompt />
      </AuthProvider>
    </BrowserRouter>
  )
}
