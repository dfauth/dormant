import { useState, useEffect, useMemo } from 'react'
import PriceChart from './PriceChart'

const MARKETS = ['ASX']

const SENTIMENTS = [
  { value: 'BULL',       label: 'Bull' },
  { value: 'LATE_BULL',  label: 'Late Bull' },
  { value: 'EARLY_BULL', label: 'Early Bull' },
  { value: 'BEAR',       label: 'Bear' },
  { value: 'LATE_BEAR',  label: 'Late Bear' },
  { value: 'EARLY_BEAR', label: 'Early Bear' },
]

function sentimentLabel(value) {
  return SENTIMENTS.find(s => s.value === value)?.label ?? value
}

function SortTh({ label, col, sortCol, sortDir, onSort }) {
  const icon = col !== sortCol ? '↕' : sortDir === 'asc' ? '↑' : '↓'
  return (
    <th className="th-sortable" onClick={() => onSort(col)}>
      {label}<span className="th-sort-icon">{icon}</span>
    </th>
  )
}

export default function Trending() {
  const [market, setMarket] = useState('ASX')
  const [sentiment, setSentiment] = useState('BULL')
  const [data, setData] = useState({ rows: [], loading: false, error: null })
  const [selectedStock, setSelectedStock] = useState(null)
  const [sortCol, setSortCol] = useState('distanceFromEma')
  const [sortDir, setSortDir] = useState('desc')

  useEffect(() => {
    setData(d => ({ ...d, loading: true, error: null }))
    const params = new URLSearchParams({ market, sentiment })
    fetch(`/api/prices/trending?${params}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(rows => setData({ rows, loading: false, error: null }))
      .catch(err => setData({ rows: [], loading: false, error: err.message }))
  }, [market, sentiment])

  useEffect(() => {
    setSelectedStock(null)
  }, [market, sentiment])

  function onSort(col) {
    if (col === sortCol) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(col)
      setSortDir('asc')
    }
  }

  const sortedRows = useMemo(() => {
    return [...data.rows].sort((a, b) => {
      let av, bv
      if (sortCol === 'distanceFromEma') {
        av = a.distanceFromEma
        bv = b.distanceFromEma
      } else if (sortCol === 'price') {
        av = a.price
        bv = b.price
      } else {
        av = a[sortCol] ?? ''
        bv = b[sortCol] ?? ''
      }
      if (av == null && bv == null) return 0
      if (av == null) return 1
      if (bv == null) return -1
      const cmp = typeof av === 'string' ? av.localeCompare(bv) : av < bv ? -1 : av > bv ? 1 : 0
      return sortDir === 'asc' ? cmp : -cmp
    })
  }, [data.rows, sortCol, sortDir])

  const sortProps = { sortCol, sortDir, onSort }

  if (selectedStock) {
    return (
      <>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
          <button className="back-btn" style={{ marginBottom: 0 }} onClick={() => setSelectedStock(null)}>← Back</button>
          <h1 style={{ margin: 0 }}>{selectedStock.code} — {sentimentLabel(sentiment)}</h1>
          <span style={{ color: '#6b7280', fontSize: '0.875rem' }}>
            EMA <span style={{ color: '#f59e0b' }}>8</span>
            {' · '}
            <span style={{ color: '#60a5fa' }}>21</span>
            {' · '}
            <span style={{ color: '#a78bfa' }}>200</span>
          </span>
        </div>
        <PriceChart market={selectedStock.market} code={selectedStock.code} />
      </>
    )
  }

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem', marginBottom: '1.25rem' }}>
        <h1 style={{ margin: 0 }}>Trending</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label style={{ color: '#6b7280', fontSize: '0.875rem' }}>Market</label>
          <select value={market} onChange={e => setMarket(e.target.value)} className="tenor-select">
            {MARKETS.map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label style={{ color: '#6b7280', fontSize: '0.875rem' }}>Sentiment</label>
          <select value={sentiment} onChange={e => setSentiment(e.target.value)} className="tenor-select">
            {SENTIMENTS.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </div>
      </div>

      {data.loading ? (
        <p>Loading…</p>
      ) : data.error ? (
        <p className="error">Error: {data.error}</p>
      ) : sortedRows.length === 0 ? (
        <p className="empty">No stocks match the selected filters.</p>
      ) : (
        <table className="trades-table">
          <thead>
            <tr>
              <SortTh label="Code"             col="code"             {...sortProps} />
              <SortTh label="Price"            col="price"            {...sortProps} />
              <SortTh label="Trend"            col="trendState"       {...sortProps} />
              <SortTh label="Distance from EMA" col="distanceFromEma" {...sortProps} />
            </tr>
          </thead>
          <tbody>
            {sortedRows.map(r => (
              <tr key={r.code}>
                <td className="code-link" onClick={() => setSelectedStock({ market: r.market, code: r.code })}>
                  {r.code}
                </td>
                <td>{r.price != null ? Number(r.price).toFixed(2) : '—'}</td>
                <td>{sentimentLabel(r.trendState?.trendState)}</td>
                <td>{r.distanceFromEma != null ? (r.distanceFromEma * 100).toFixed(2) + '%' : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
