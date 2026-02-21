import { useEffect, useRef, useState } from 'react'
import { createUniver, defaultTheme, LocaleType, merge } from '@univerjs/presets'
import { UniverSheetsCorePreset } from '@univerjs/preset-sheets-core'
import SheetsLocaleEnUS from '@univerjs/preset-sheets-core/locales/en-US'
import '@univerjs/presets/lib/styles/preset-sheets-core.css'
import '@univerjs/preset-sheets-core/lib/index.css'

function parseFormula(value) {
  if (typeof value !== 'string') return null
  const m = value.match(/^=PRICES\((\w+),(\w+)(?:,(\w+))?\)$/i)
  if (!m) return null
  return { market: m[1], code: m[2], tenor: m[3] || undefined }
}

function buildUrl({ market, code, tenor }) {
  const params = new URLSearchParams({ market })
  if (tenor) params.set('tenor', tenor)
  return `/api/prices/${code}?${params}`
}

const HEADERS = ['Date', 'Open', 'High', 'Low', 'Close', 'Volume']
const FIELDS = ['date', 'open', 'high', 'low', 'close', 'volume']
const SCAN_ROWS = 200
const SCAN_COLS = 50

export default function PriceSheet() {
  const univerRef = useRef(null)
  const anchorRef = useRef(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    // Create the container ourselves outside React's management so React
    // reconciliation can never touch the children UniverJS adds to it.
    const container = document.createElement('div')
    container.className = 'sheet-container'
    container.style.width = '100%'
    container.style.height = 'calc(100vh - 11rem)'
    anchorRef.current.insertAdjacentElement('afterend', container)

    const { univerAPI } = createUniver({
      locale: LocaleType.EN_US,
      locales: { [LocaleType.EN_US]: merge({}, SheetsLocaleEnUS) },
      theme: defaultTheme,
      presets: [UniverSheetsCorePreset({ container })],
    })
    univerAPI.createWorkbook({ name: 'Prices' })
    univerRef.current = univerAPI

    return () => {
      univerAPI.dispose()
      container.remove()
      univerRef.current = null
    }
  }, [])

  const handleFetch = async () => {
    const univerAPI = univerRef.current
    if (!univerAPI) return
    const sheet = univerAPI.getActiveWorkbook()?.getActiveSheet()
    if (!sheet) return

    for (let r = 0; r < SCAN_ROWS; r++) {
      for (let c = 0; c < SCAN_COLS; c++) {
        const cell = sheet.getRange(r, c).getValue()
        const parsed = parseFormula(cell)
        if (!parsed) continue

        setLoading(true)
        setError(null)
        try {
          const res = await fetch(buildUrl(parsed), { credentials: 'include' })
          if (!res.ok) throw new Error(`HTTP ${res.status}`)
          const prices = await res.json()

          sheet.getRange(r, c, 1, HEADERS.length).setValues([HEADERS])
          if (prices.length > 0) {
            const rows = prices.map(p => FIELDS.map(f => p[f] ?? ''))
            sheet.getRange(r + 1, c, prices.length, FIELDS.length).setValues(rows)
          }
        } catch (err) {
          setError(`Failed to fetch prices: ${err.message}`)
        } finally {
          setLoading(false)
        }
        return // process one formula at a time
      }
    }
  }

  const handleClear = () => {
    const univerAPI = univerRef.current
    if (!univerAPI) return
    const sheet = univerAPI.getActiveWorkbook()?.getActiveSheet()
    sheet?.clear()
  }

  return (
    <>
      <h1>Prices</h1>
      <p className="sheet-hint">
        Type <code>=PRICES(market,code,tenor)</code> in any cell then press Fetch.
        Example: <code>=PRICES(ASX,BHP,1Y)</code>
      </p>
      <div className="sheet-toolbar">
        <button className="sheet-btn" onClick={handleFetch} disabled={loading}>
          {loading ? 'Fetching…' : 'Fetch'}
        </button>
        <button className="sheet-btn sheet-btn-secondary" onClick={handleClear}>
          Clear
        </button>
        {error && <span className="error">{error}</span>}
      </div>
      {/* anchor: UniverJS container is inserted after this div by useEffect */}
      <div ref={anchorRef} />
    </>
  )
}
