import { Component, type ErrorInfo, type ReactNode } from 'react'

interface AppErrorBoundaryProps {
  children: ReactNode
}
interface AppErrorBoundaryState {
  failed: boolean
}

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  state: AppErrorBoundaryState = { failed: false }

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { failed: true }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Reelz UI error', error, errorInfo)
  }

  render() {
    if (!this.state.failed) return this.props.children

    return (
      <main className="flex min-h-svh items-center justify-center bg-canvas p-6 text-center text-white">
        <div className="max-w-sm rounded-[2rem] border border-white/10 bg-white/[0.035] p-7">
          <span className="text-5xl" aria-hidden="true">🎞️</span>
          <h1 className="mt-4 text-2xl font-black">A sessão engasgou</h1>
          <p className="mt-2 text-sm leading-6 text-white/50">
            Seus dados continuam salvos. Recarregue o Reelz para retomar de onde parou.
          </p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-6 w-full rounded-2xl bg-reel px-5 py-3 font-black text-black"
          >
            Recarregar aplicativo
          </button>
        </div>
      </main>
    )
  }
}
