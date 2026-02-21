import { useEffect, useMemo, useState } from 'react'

const CONSENSUS_LABEL = {
  STRONG_BUY: 'Strong Buy',
  BUY: 'Buy',
  HOLD: 'Hold',
  SELL: 'Sell',
  STRONG_SELL: 'Strong Sell',
}

const SUMMARY_COLUMNS = [
  { key: 'date',      label: 'Date' },
  { key: 'code',      label: 'Code' },
  { key: 'consensus', label: 'Consensus' },
  { key: 'buy',       label: 'Buy' },
  { key: 'hold',      label: 'Hold' },
  { key: 'sell',      label: 'Sell' },
  { key: 'price',     label: 'Price' },
  { key: 'target',    label: 'Target' },
  { key: 'potential', label: 'Potential' },
]


function sortValue(row, key) {
  const v = row[key]
  if (v == null) return null
  return (key === 'date' || key === 'code' || key === 'consensus') ? v : Number(v)
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

function consensusClass(consensus) {
  switch (consensus) {
    case 'STRONG_BUY':  return 'consensus-strong-buy'
    case 'BUY':         return 'consensus-buy'
    case 'SELL':        return 'consensus-sell'
    case 'STRONG_SELL': return 'consensus-strong-sell'
    default:            return ''
  }
}

function potentialClass(potential) {
  if (potential == null) return ''
  const n = Number(potential)
  if (n > 0) return 'pnl-positive'
  if (n < 0) return 'pnl-negative'
  return ''
}

function fmtPrice(val) {
  return val == null ? '—' : Number(val).toFixed(2)
}

function fmtPotential(val) {
  if (val == null) return '—'
  const n = Number(val)
  return `${n > 0 ? '+' : ''}${n.toFixed(2)}%`
}

function SortableHeader({ columns, sortCol, sortDir, onSort }) {
  return (
    <thead>
      <tr>
        {columns.map(col => (
          <th key={col.key} className="th-sortable" onClick={() => onSort(col.key)}>
            {col.label}
            <span className="th-sort-icon">
              {sortCol === col.key ? (sortDir === 'asc' ? ' ↑' : ' ↓') : ''}
            </span>
          </th>
        ))}
      </tr>
    </thead>
  )
}

export default function Valuations() {
  const [state, setState] = useState({ data: [], loading: true, error: null })
  const [sortCol, setSortCol] = useState('date')
  const [sortDir, setSortDir] = useState('desc')

  const [selectedItem, setSelectedItem] = useState(null) // { code, market }
  const [detail, setDetail] = useState({ data: [], loading: false, error: null })
  const [detailSortCol, setDetailSortCol] = useState('date')
  const [detailSortDir, setDetailSortDir] = useState('desc')

  useEffect(() => {
    fetch('/api/valuations/recent', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setState({ data, loading: false, error: null }))
      .catch(err => setState({ data: [], loading: false, error: err.message }))
  }, [])

  useEffect(() => {
    if (!selectedItem) return
    setDetail({ data: [], loading: true, error: null })
    fetch(`/api/valuations/${selectedItem.code}?market=${selectedItem.market}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setDetail({ data, loading: false, error: null }))
      .catch(err => setDetail({ data: [], loading: false, error: err.message }))
  }, [selectedItem])

  const sorted = useMemo(() => sortData(state.data, sortCol, sortDir), [state.data, sortCol, sortDir])
  const detailSorted = useMemo(() => sortData(detail.data, detailSortCol, detailSortDir), [detail.data, detailSortCol, detailSortDir])

  function handleSort(key) {
    if (key === sortCol) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(key)
      setSortDir('asc')
    }
  }

  function handleDetailSort(key) {
    if (key === detailSortCol) {
      setDetailSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setDetailSortCol(key)
      setDetailSortDir('asc')
    }
  }

  if (selectedItem) {
    return (
      <>
        <h1>Valuations — {selectedItem.market}:{selectedItem.code}</h1>
        <button className="back-btn" onClick={() => setSelectedItem(null)}>← Back</button>
        {detail.loading && <p>Loading…</p>}
        {detail.error && <p className="error">Error: {detail.error}</p>}
        {!detail.loading && !detail.error && detail.data.length === 0 && (
          <p className="empty">No valuation history for {selectedItem.code}.</p>
        )}
        {!detail.loading && !detail.error && detail.data.length > 0 && (
          <table className="trades-table">
            <SortableHeader columns={SUMMARY_COLUMNS} sortCol={detailSortCol} sortDir={detailSortDir} onSort={handleDetailSort} />
            <tbody>
              {detailSorted.map(v => (
                <tr key={v.id}>
                  <td>{v.date}</td>
                  <td>{v.code}</td>
                  <td className={consensusClass(v.consensus)}>
                    {CONSENSUS_LABEL[v.consensus] ?? v.consensus}
                  </td>
                  <td>{v.buy}</td>
                  <td>{v.hold}</td>
                  <td>{v.sell}</td>
                  <td>{fmtPrice(v.price)}</td>
                  <td>{fmtPrice(v.target)}</td>
                  <td className={potentialClass(v.potential)}>
                    {fmtPotential(v.potential)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </>
    )
  }

  if (state.loading) return <p>Loading…</p>
  if (state.error) return <p className="error">Error: {state.error}</p>

  const markets = [...new Set(state.data.map(v => v.market))].join(', ')

  return (
    <>
      <h1>Valuations{markets ? ` — ${markets}` : ''}</h1>
      {state.data.length === 0 ? (
        <p className="empty">No valuations updated in the last 3 months.</p>
      ) : (
        <table className="trades-table">
          <SortableHeader columns={SUMMARY_COLUMNS} sortCol={sortCol} sortDir={sortDir} onSort={handleSort} />
          <tbody>
            {sorted.map(v => (
              <tr key={`${v.market}-${v.code}`}>
                <td>{v.date}</td>
                <td>
                  <span className="code-link" onClick={() => setSelectedItem({ code: v.code, market: v.market })}>
                    {v.code}
                  </span>
                </td>
                <td className={consensusClass(v.consensus)}>
                  {CONSENSUS_LABEL[v.consensus] ?? v.consensus}
                </td>
                <td>{v.buy}</td>
                <td>{v.hold}</td>
                <td>{v.sell}</td>
                <td>{fmtPrice(v.price)}</td>
                <td>{fmtPrice(v.target)}</td>
                <td className={potentialClass(v.potential)}>
                  {fmtPotential(v.potential)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
