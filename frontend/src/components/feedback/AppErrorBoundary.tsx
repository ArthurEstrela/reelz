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
      <main className="flex min-h-svh items-center justify-center bg-canvas p-6 text-center text-paper">
        <div className="max-w-sm border-y border-white/10 py-9">
          <svg className="mx-auto size-12 text-reel" viewBox="0 0 48 48" fill="none" aria-hidden="true">
            <rect x="7" y="10" width="34" height="28" rx="3" stroke="currentColor" strokeWidth="2" />
            <path d="M7 17h34M7 31h34M15 10v7m9-7v7m9-7v7M15 31v7m9-7v7m9-7v7" stroke="currentColor" strokeWidth="2" />
          </svg>
          <h1 className="mt-5 text-2xl font-bold">A sessão engasgou</h1>
          <p className="mt-2 text-sm leading-6 text-white/65">
            Seus dados continuam salvos. Recarregue o Reelz para retomar de onde parou.
          </p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="mt-6 w-full rounded-xl bg-reel px-5 py-3 font-bold text-white transition hover:bg-reel-bright"
          >
            Recarregar aplicativo
          </button>
        </div>
      </main>
    )
  }
}
