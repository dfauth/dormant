import { useEffect, useMemo, useState } from 'react'

const COLUMNS = [
  { key: 'date',         label: 'Date' },
  { key: 'code',         label: 'Code' },
  { key: 'buyers',       label: 'Buyers' },
  { key: 'buyerShares',  label: 'Buyer Shares' },
  { key: 'sellers',      label: 'Sellers' },
  { key: 'sellerShares', label: 'Seller Shares' },
  { key: 'ratio',        label: 'Buy Ratio %' },
]

function sortValue(row, key) {
  const v = row[key]
  if (v == null) return null
  return (key === 'date' || key === 'code') ? v : Number(v)
}

function sortData(data, col, dir) {
  return [...data].sort((a, b) => {
    const av = sortValue(a, col)
    const bv = sortValue(b, col)
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    const cmp = av < bv ? -1 : av > bv ? 1 : 0
    return dir === 'asc' ? cmp : -cmp
  })
}

function ratioClass(ratio) {
  if (ratio == null) return ''
  const n = Number(ratio)
  if (n > 100) return 'pnl-positive'
  if (n < 50) return 'pnl-negative'
  return ''
}

function fmtRatio(val) {
  return val == null ? '—' : `${Number(val).toFixed(1)}%`
}

export default function MarketDepth() {
  const [state, setState] = useState({ data: [], loading: true, error: null })
  const [sortCol, setSortCol] = useState('date')
  const [sortDir, setSortDir] = useState('desc')

  useEffect(() => {
    fetch('/api/depth/recent', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setState({ data, loading: false, error: null }))
      .catch(err => setState({ data: [], loading: false, error: err.message }))
  }, [])

  const sorted = useMemo(() => sortData(state.data, sortCol, sortDir), [state.data, sortCol, sortDir])

  function handleSort(key) {
    if (key === sortCol) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(key)
      setSortDir('asc')
    }
  }

  if (state.loading) return <p>Loading…</p>
  if (state.error) return <p className="error">Error: {state.error}</p>

  return (
    <>
      <h1>Market Depth — Last 3 Days</h1>
      {state.data.length === 0 ? (
        <p className="empty">No market depth data for the last 3 days.</p>
      ) : (
        <table className="trades-table">
          <thead>
            <tr>
              {COLUMNS.map(col => (
                <th key={col.key} className="th-sortable" onClick={() => handleSort(col.key)}>
                  {col.label}
                  <span className="th-sort-icon">
                    {sortCol === col.key ? (sortDir === 'asc' ? ' ↑' : ' ↓') : ''}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sorted.map((row, i) => (
              <tr key={`${row.code}-${row.date}-${i}`}>
                <td>{row.date}</td>
                <td>{row.code}</td>
                <td>{row.buyers.toLocaleString()}</td>
                <td>{row.buyerShares.toLocaleString()}</td>
                <td>{row.sellers.toLocaleString()}</td>
                <td>{row.sellerShares.toLocaleString()}</td>
                <td className={ratioClass(row.ratio)}>{fmtRatio(row.ratio)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
