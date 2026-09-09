import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { CineGiroLogo } from './CineGiroLogo'

describe('CineGiroLogo', () => {
  it('renders the complete brand lockup by default', () => {
    render(<CineGiroLogo />)

    const logo = screen.getByRole('img', { name: 'CineGiro' })
    const asset = logo.querySelector('img')

    expect(asset).toHaveAttribute('src', '/brand/cinegiro-lockup-dark.svg')
  })

  it('renders only the symbol in compact contexts', () => {
    render(<CineGiroLogo compact />)

    const logo = screen.getByRole('img', { name: 'CineGiro' })
    const asset = logo.querySelector('img')

    expect(asset).toHaveAttribute('src', '/brand/cinegiro-symbol.svg')
  })
})
