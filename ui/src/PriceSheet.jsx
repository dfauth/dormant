import { useState, useCallback } from 'react'
import Spreadsheet from 'react-spreadsheet'

const INITIAL_ROWS = 30
const INITIAL_COLS = 10

function emptyGrid(rows, cols) {
  return Array.from({ length: rows }, () =>
    Array.from({ length: cols }, () => ({ value: '' }))
  )
}

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

export default function PriceSheet() {
  const [data, setData] = useState(() => emptyGrid(INITIAL_ROWS, INITIAL_COLS))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleChange = useCallback((newData) => {
    setData(newData)
  }, [])

  const handleCommit = useCallback(async () => {
    // Scan the grid for formula cells
    for (let r = 0; r < data.length; r++) {
      for (let c = 0; c < data[r].length; c++) {
        const cell = data[r][c]
        const parsed = parseFormula(cell?.value)
        if (!parsed) continue

        setLoading(true)
        setError(null)
        try {
          const res = await fetch(buildUrl(parsed), { credentials: 'include' })
          if (!res.ok) throw new Error(`HTTP ${res.status}`)
          const prices = await res.json()

          setData(prev => {
            const grid = prev.map(row => row.map(cell => ({ ...cell })))

            // Ensure enough rows for header + data
            const needed = r + 1 + prices.length
            while (grid.length < needed) {
              grid.push(Array.from({ length: grid[0].length }, () => ({ value: '' })))
            }

            // Ensure enough columns for the spill
            const colsNeeded = c + HEADERS.length
            if (colsNeeded > grid[0].length) {
              for (const row of grid) {
                while (row.length < colsNeeded) row.push({ value: '' })
              }
            }

            // Write the formula cell as a label
            grid[r][c] = { value: `${parsed.market}:${parsed.code}`, readOnly: true }

            // Write headers in the same row, offset by 1
            HEADERS.forEach((h, i) => {
              if (c + i < grid[r].length) {
                grid[r][c + i] = { value: h, readOnly: true }
              }
            })

            // Write price rows below
            prices.forEach((price, ri) => {
              FIELDS.forEach((f, ci) => {
                grid[r + 1 + ri][c + ci] = { value: String(price[f] ?? ''), readOnly: true }
              })
            })

            return grid
          })
        } catch (err) {
          setError(`Failed to fetch prices: ${err.message}`)
        } finally {
          setLoading(false)
        }
        return // process one formula at a time
      }
    }
  }, [data])

  return (
    <>
      <h1>Prices</h1>
      <p className="sheet-hint">
        Type <code>=PRICES(market,code,tenor)</code> in any cell then press Fetch.
        Example: <code>=PRICES(ASX,BHP,1Y)</code>
      </p>
      <div className="sheet-toolbar">
        <button className="sheet-btn" onClick={handleCommit} disabled={loading}>
          {loading ? 'Fetching…' : 'Fetch'}
        </button>
        <button className="sheet-btn sheet-btn-secondary" onClick={() => setData(emptyGrid(INITIAL_ROWS, INITIAL_COLS))}>
          Clear
        </button>
        {error && <span className="error">{error}</span>}
      </div>
      <div className="sheet-container">
        <Spreadsheet data={data} onChange={handleChange} />
      </div>
    </>
  )
}
