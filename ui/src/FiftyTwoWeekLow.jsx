import { useState, useEffect, useMemo, useRef } from 'react'
import PriceChart from './PriceChart'

const MARKETS = ['ASX']
const TENORS = ['1M', '3M', '6M', '1Y', '2Y', '3Y', '5Y']
const DIRECTIONS = ['RISING', 'FALLING']

function SortTh({ label, col, sortCol, sortDir, onSort }) {
  const icon = col !== sortCol ? '↕' : sortDir === 'asc' ? '↑' : '↓'
  return (
    <th className="th-sortable" onClick={() => onSort(col)}>
      {label}<span className="th-sort-icon">{icon}</span>
    </th>
  )
}

function fmt2(n) {
  return n != null ? Number(n).toFixed(2) : '—'
}

export default function FiftyTwoWeekLow() {
  const [market, setMarket] = useState('ASX')
  const [tenor, setTenor] = useState('1Y')
  const [direction, setDirection] = useState('RISING')
  const [threshold, setThreshold] = useState(5)
  const [fetchThreshold, setFetchThreshold] = useState(5)
  const [data, setData] = useState({ rows: [], loading: false, error: null })
  const [selectedStock, setSelectedStock] = useState(null)
  const [sortCol, setSortCol] = useState('distancePct')
  const [sortDir, setSortDir] = useState('asc')
  const debounceRef = useRef(null)

  function handleThresholdChange(value) {
    setThreshold(value)
    clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => setFetchThreshold(value), 500)
  }

  useEffect(() => {
    setData(d => ({ ...d, loading: true, error: null }))
    const params = new URLSearchParams({ market, tenor, direction, threshold: fetchThreshold / 100 })
    fetch(`/api/prices/watermark?${params}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(rows => setData({
        rows: rows.map(r => ({
          code: r.current?.code ?? r.waterMark?.code,
          market: r.current?.market ?? r.waterMark?.market,
          watermarkPrice: r.waterMark?.close,
          watermarkDate: r.waterMark?.date,
          currentPrice: r.current?.close,
          distancePct: r.distance != null ? r.distance * 100 : null,
          intervalsSince: r.intervalsSince,
          touches: r.touches,
        })),
        loading: false,
        error: null,
      }))
      .catch(err => setData({ rows: [], loading: false, error: err.message }))
  }, [market, tenor, direction, fetchThreshold])

  useEffect(() => {
    setSelectedStock(null)
  }, [market, tenor, direction, fetchThreshold])

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
      const av = a[sortCol]
      const bv = b[sortCol]
      if (av == null && bv == null) return 0
      if (av == null) return 1
      if (bv == null) return -1
      const cmp = typeof av === 'string' ? av.localeCompare(bv) : av < bv ? -1 : av > bv ? 1 : 0
      return sortDir === 'asc' ? cmp : -cmp
    })
  }, [data.rows, sortCol, sortDir])

  const sortProps = { sortCol, sortDir, onSort }
  const title = `${tenor} ${direction === 'RISING' ? 'High' : 'Low'}`
  const watermarkLabel = direction === 'RISING' ? `${tenor} High` : `${tenor} Low`

  if (selectedStock) {
    return (
      <>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
          <button className="back-btn" style={{ marginBottom: 0 }} onClick={() => setSelectedStock(null)}>← Back</button>
          <h1 style={{ margin: 0 }}>{selectedStock.code} — {title}</h1>
        </div>
        <PriceChart market={selectedStock.market} code={selectedStock.code} />
      </>
    )
  }

  return (
    <>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <h1 style={{ margin: 0 }}>{title}</h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <label style={{ color: '#6b7280', fontSize: '0.875rem' }}>Market</label>
            <select value={market} onChange={e => setMarket(e.target.value)} className="tenor-select">
              {MARKETS.map(m => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <label style={{ color: '#6b7280', fontSize: '0.875rem' }}>Tenor</label>
            <select value={tenor} onChange={e => setTenor(e.target.value)} className="tenor-select">
              {TENORS.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <label style={{ color: '#6b7280', fontSize: '0.875rem' }}>Direction</label>
            <select value={direction} onChange={e => setDirection(e.target.value)} className="tenor-select">
              {DIRECTIONS.map(d => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <label style={{ color: '#6b7280', fontSize: '0.875rem' }}>Threshold</label>
          <span style={{ color: '#6b7280', fontSize: '0.875rem' }}>0%</span>
          <input
            type="range"
            min={0}
            max={20}
            step={1}
            value={threshold}
            onChange={e => handleThresholdChange(Number(e.target.value))}
            style={{ width: '16rem', accentColor: '#6366f1' }}
          />
          <span style={{ color: '#6b7280', fontSize: '0.875rem' }}>20%</span>
          <span style={{ color: '#e0e0f0', fontSize: '0.875rem', minWidth: '2.5rem' }}>{threshold}%</span>
        </div>
      </div>

      {data.loading ? (
        <p>Loading…</p>
      ) : data.error ? (
        <p className="error">Error: {data.error}</p>
      ) : sortedRows.length === 0 ? (
        <p className="empty">No securities are currently within {threshold}% of their {title.toLowerCase()}.</p>
      ) : (
        <table className="trades-table">
          <thead>
            <tr>
              <SortTh label="Code"           col="code"           {...sortProps} />
              <SortTh label={watermarkLabel} col="watermarkPrice" {...sortProps} />
              <SortTh label="Watermark Date" col="watermarkDate"  {...sortProps} />
              <SortTh label="Current Price"  col="currentPrice"   {...sortProps} />
              <SortTh label="Distance %"     col="distancePct"    {...sortProps} />
              <SortTh label="Days Since"     col="intervalsSince" {...sortProps} />
              <SortTh label="Touches"        col="touches"        {...sortProps} />
            </tr>
          </thead>
          <tbody>
            {sortedRows.map(r => (
              <tr key={r.code}>
                <td className="code-link" onClick={() => setSelectedStock({ market: r.market, code: r.code })}>
                  {r.code}
                </td>
                <td>{fmt2(r.watermarkPrice)}</td>
                <td>{r.watermarkDate ?? '—'}</td>
                <td>{fmt2(r.currentPrice)}</td>
                <td>{r.distancePct != null ? fmt2(r.distancePct) + '%' : '—'}</td>
                <td>{r.intervalsSince ?? '—'}</td>
                <td>{r.touches ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
