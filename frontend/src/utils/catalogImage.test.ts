import { describe, expect, it } from 'vitest'
import { resolveCatalogImageUrl } from './catalogImage'

describe('resolveCatalogImageUrl', () => {
  it('keeps absolute Movie of the Night image URLs untouched', () => {
    expect(resolveCatalogImageUrl('https://cdn.example/poster.jpg'))
      .toBe('https://cdn.example/poster.jpg')
  })

  it('resolves legacy TMDB image paths with the requested size', () => {
    expect(resolveCatalogImageUrl('/poster.jpg'))
      .toBe('https://image.tmdb.org/t/p/w500/poster.jpg')
    expect(resolveCatalogImageUrl('/logo.jpg', 'original'))
      .toBe('https://image.tmdb.org/t/p/original/logo.jpg')
  })
})
