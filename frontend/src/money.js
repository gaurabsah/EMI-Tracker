const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })

export function formatPaise(paise) {
  return inr.format(paise / 100)
}

export function formatBps(bps) {
  return `${bps / 100}%`
}

/** "70000" -> 7000000, "4442.44" -> 444244. Returns null when the text is not a plain number with up to 2 decimals. */
export function toMinorUnits(text) {
  const match = /^(\d+)(?:\.(\d{1,2}))?$/.exec(String(text).trim().replace(/,/g, ''))
  if (!match) return null
  return Number(match[1]) * 100 + Number((match[2] ?? '').padEnd(2, '0'))
}

export function formatDate(iso) {
  if (!iso) return ''
  return new Date(`${iso}T00:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}
