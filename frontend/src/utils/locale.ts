const COUNTRY_CODE_PATTERN = /^[A-Z]{2}$/

export function getBrowserTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
}

export function getBrowserCountryCode(): string {
  try {
    const region = new Intl.Locale(navigator.language).region?.toUpperCase()
    return region && COUNTRY_CODE_PATTERN.test(region) ? region : 'BR'
  } catch {
    return 'BR'
  }
}
