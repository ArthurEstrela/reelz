import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AppErrorBoundary } from './AppErrorBoundary'

function BrokenComponent(): never {
  throw new Error('render failed')
}
describe('AppErrorBoundary', () => {
  afterEach(() => vi.restoreAllMocks())

  it('shows a recoverable screen instead of a blank application', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined)

    render(
      <AppErrorBoundary>
        <BrokenComponent />
      </AppErrorBoundary>,
    )

    expect(screen.getByRole('heading', { name: 'A sessão engasgou' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Recarregar aplicativo' })).toBeInTheDocument()
  })
})
