import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { FilterPills } from './FilterPills'

describe('FilterPills', () => {
  it('shows navigation arrows and supports the regular mouse wheel when options overflow', async () => {
    render(
      <FilterPills
        legend="Gênero"
        options={[
          { value: 1, label: 'Comédia' },
          { value: 2, label: 'Ação' },
          { value: 3, label: 'Terror' },
          { value: 4, label: 'Drama' },
        ]}
        selectedValues={[]}
        onToggle={vi.fn()}
      />,
    )

    const scroller = screen.getByLabelText('Opções de Gênero')
    Object.defineProperties(scroller, {
      clientWidth: { configurable: true, value: 200 },
      scrollWidth: { configurable: true, value: 600 },
      scrollLeft: { configurable: true, value: 0, writable: true },
    })
    Object.defineProperty(scroller, 'scrollTo', {
      configurable: true,
      value: vi.fn(({ left }: ScrollToOptions) => {
        scroller.scrollLeft = Number(left)
        fireEvent.scroll(scroller)
      }),
    })

    fireEvent(window, new Event('resize'))
    const nextButton = await screen.findByRole('button', { name: 'Ver próximas opções de Gênero' })
    expect(screen.queryByRole('button', { name: 'Ver opções anteriores de Gênero' })).not.toBeInTheDocument()

    fireEvent.click(nextButton)
    expect(scroller.scrollLeft).toBe(180)
    expect(await screen.findByRole('button', { name: 'Ver opções anteriores de Gênero' })).toBeInTheDocument()

    fireEvent.wheel(scroller, { deltaX: 0, deltaY: 80 })
    expect(scroller.scrollLeft).toBe(260)

    scroller.scrollLeft = 400
    fireEvent.scroll(scroller)
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Ver próximas opções de Gênero' })).not.toBeInTheDocument()
    })
  })
})
