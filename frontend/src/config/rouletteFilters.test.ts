import { describe, expect, it } from 'vitest'

import { GENRE_OPTIONS } from './rouletteFilters'

describe('rouletteFilters', () => {
  it('inclui os gêneros de alta intenção com os IDs oficiais do TMDB', () => {
    expect(GENRE_OPTIONS).toEqual(expect.arrayContaining([
      { value: 10749, label: 'Romance' },
      { value: 16, label: 'Animação' },
      { value: 99, label: 'Documentário' },
    ]))
  })

  it('não repete IDs de gênero', () => {
    const genreIds = GENRE_OPTIONS.map(({ value }) => value)

    expect(new Set(genreIds).size).toBe(genreIds.length)
  })
})
