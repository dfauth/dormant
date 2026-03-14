import { useEffect, useMemo, useState } from 'react'
import PriceChart from './PriceChart'

const OVERSOLD_THRESHOLD  = 30
const OVERBOUGHT_THRESHOLD = 70

function fmtRsi(val) {
  return typeof val === 'number' ? val.toFixed(2) : '—'
}

function rsiClass(rsi) {
  if (rsi <= OVERSOLD_THRESHOLD)  return 'pnl-positive'
  if (rsi >= OVERBOUGHT_THRESHOLD) return 'pnl-negative'
  return ''
}

export default function RsiScanner() {
  const [mode, setMode] = useState('oversold')
  const [state, setState] = useState({ data: [], loading: true, error: null })
  const [sortCol, setSortCol] = useState('rsi')
  const [sortDir, setSortDir] = useState('asc')
  const [selectedStock, setSelectedStock] = useState(null)

  useEffect(() => {
    setState({ data: [], loading: true, error: null })
    fetch('/api/ta/rsi', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setState({ data, loading: false, error: null }))
      .catch(err => setState({ data: [], loading: false, error: err.message }))
  }, [])

  const filtered = useMemo(() => {
    const threshold = mode === 'oversold' ? OVERSOLD_THRESHOLD : OVERBOUGHT_THRESHOLD
    return state.data.filter(item =>
      mode === 'oversold' ? item.payload <= threshold : item.payload >= threshold
    )
  }, [state.data, mode])

  const sorted = useMemo(() => {
    return [...filtered].sort((a, b) => {
      const av = sortCol === 'code' ? a.code : a.payload
      const bv = sortCol === 'code' ? b.code : b.payload
      const cmp = av < bv ? -1 : av > bv ? 1 : 0
      return sortDir === 'asc' ? cmp : -cmp
    })
  }, [filtered, sortCol, sortDir])

  function handleSort(col) {
    if (col === sortCol) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(col)
      setSortDir(col === 'rsi' ? 'asc' : 'asc')
    }
  }

  function sortIcon(col) {
    if (col !== sortCol) return ''
    return sortDir === 'asc' ? ' ↑' : ' ↓'
  }

  if (selectedStock) {
    return (
      <>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
          <button className="back-btn" style={{ marginBottom: 0 }} onClick={() => setSelectedStock(null)}>← Back</button>
          <h1 style={{ margin: 0 }}>{selectedStock.code} — RSI {fmtRsi(selectedStock.rsi)}</h1>
        </div>
        <PriceChart market={selectedStock.market} code={selectedStock.code} />
      </>
    )
  }

  const emptyMsg = mode === 'oversold'
    ? `No stocks with RSI ≤ ${OVERSOLD_THRESHOLD}.`
    : `No stocks with RSI ≥ ${OVERBOUGHT_THRESHOLD}.`

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '0.5rem' }}>
        <h1 style={{ margin: 0 }}>RSI Scanner</h1>
        <div className="toggle-group">
          <button
            className={`toggle-btn${mode === 'oversold' ? ' toggle-btn--active' : ''}`}
            onClick={() => setMode('oversold')}
          >Oversold</button>
          <button
            className={`toggle-btn${mode === 'overbought' ? ' toggle-btn--active' : ''}`}
            onClick={() => setMode('overbought')}
          >Overbought</button>
        </div>
        <span style={{ color: '#9ca3b0', fontSize: '0.8rem' }}>
          {mode === 'oversold' ? `RSI ≤ ${OVERSOLD_THRESHOLD}` : `RSI ≥ ${OVERBOUGHT_THRESHOLD}`}
        </span>
      </div>

      {state.loading ? (
        <p>Loading…</p>
      ) : state.error ? (
        <p className="error">Error: {state.error}</p>
      ) : sorted.length === 0 ? (
        <p className="empty">{emptyMsg}</p>
      ) : (
        <table className="trades-table">
          <thead>
            <tr>
              <th className="th-sortable" onClick={() => handleSort('code')}>
                Code<span className="th-sort-icon">{sortIcon('code')}</span>
              </th>
              <th className="th-sortable" onClick={() => handleSort('rsi')}>
                RSI<span className="th-sort-icon">{sortIcon('rsi')}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {sorted.map(item => (
              <tr key={item.code}>
                <td
                  className="code-link"
                  onClick={() => {
                    const [market, code] = item.code.split(':')
                    setSelectedStock({ market, code, rsi: item.payload })
                  }}
                >{item.code}</td>
                <td className={rsiClass(item.payload)}>{fmtRsi(item.payload)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
