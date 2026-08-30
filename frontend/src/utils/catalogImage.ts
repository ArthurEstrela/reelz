const TMDB_IMAGE_BASE_URL = 'https://image.tmdb.org/t/p'

export function resolveCatalogImageUrl(
  image: string | null | undefined,
  tmdbSize: 'w500' | 'original' = 'w500',
): string | null {
  if (!image) return null
  if (/^https?:\/\//i.test(image) || image.startsWith('//')) return image
  return `${TMDB_IMAGE_BASE_URL}/${tmdbSize}${image.startsWith('/') ? image : `/${image}`}`
}
