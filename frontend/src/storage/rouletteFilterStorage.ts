export const ROULETTE_FILTER_DRAFT_TTL_MS = 12 * 60 * 60 * 1_000

const ROULETTE_FILTER_DRAFT_KEY_PREFIX = 'cinegiro.roulette.filters'

export interface RouletteFilterDraft {
  genreId: number | null
  vibeId: string | null
  expiresAt: number
}

export interface RouletteFilterSelection {
  genreId: number | null
  vibeId: string | null
}

function storageKey(userId: string): string {
  return `${ROULETTE_FILTER_DRAFT_KEY_PREFIX}.${userId}`
}

function removeStoredDraft(key: string): void {
  try {
    localStorage.removeItem(key)
  } catch {
    // A seleção continua funcionando em memória quando o navegador bloqueia armazenamento local.
  }
}

function isFilterDraft(value: unknown): value is RouletteFilterDraft {
  if (typeof value !== 'object' || value === null) return false

  const draft = value as Partial<RouletteFilterDraft>
  const validGenre = draft.genreId === null
    || (typeof draft.genreId === 'number' && Number.isInteger(draft.genreId) && draft.genreId > 0)
  const validVibe = draft.vibeId === null
    || (typeof draft.vibeId === 'string' && draft.vibeId.length > 0)

  return validGenre && validVibe && typeof draft.expiresAt === 'number'
}

export function getRouletteFilterDraft(
  userId: string,
  now = Date.now(),
): RouletteFilterDraft | null {
  const key = storageKey(userId)
  let serializedDraft: string | null
  try {
    serializedDraft = localStorage.getItem(key)
  } catch {
    return null
  }
  if (!serializedDraft) return null

  try {
    const draft: unknown = JSON.parse(serializedDraft)
    if (!isFilterDraft(draft) || draft.expiresAt <= now) {
      removeStoredDraft(key)
      return null
    }
    return draft
  } catch {
    removeStoredDraft(key)
    return null
  }
}

export function saveRouletteFilterDraft(
  userId: string,
  selection: RouletteFilterSelection,
  now = Date.now(),
): void {
  if (selection.genreId === null && selection.vibeId === null) {
    clearRouletteFilterDraft(userId)
    return
  }

  const draft: RouletteFilterDraft = {
    ...selection,
    expiresAt: now + ROULETTE_FILTER_DRAFT_TTL_MS,
  }
  try {
    localStorage.setItem(storageKey(userId), JSON.stringify(draft))
  } catch {
    // Falhas de quota/privacidade não devem impedir o giro atual.
  }
}

export function clearRouletteFilterDraft(userId: string): void {
  removeStoredDraft(storageKey(userId))
}
