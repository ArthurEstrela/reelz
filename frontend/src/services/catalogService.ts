import { api } from './api'
import type { CatalogItem } from '../types/catalog'

export async function getProviders(): Promise<CatalogItem[]> {
  const { data } = await api.get<CatalogItem[]>('/api/v1/catalog/providers')
  return data
}

export async function getVibes(): Promise<CatalogItem[]> {
  const { data } = await api.get<CatalogItem[]>('/api/v1/catalog/vibes')
  return data
}
