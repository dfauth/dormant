import { useEffect, useMemo, useState } from 'react'

const DETAIL_COLUMNS = [
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

const CONSENSUS_LABEL = {
  STRONG_BUY: 'Strong Buy',
  BUY: 'Buy',
  HOLD: 'Hold',
  SELL: 'Sell',
  STRONG_SELL: 'Strong Sell',
}

const COLUMNS = [
  { key: 'date',           label: 'Date' },
  { key: 'code',           label: 'Code' },
  { key: 'consensus',      label: 'Consensus' },
  { key: 'prevTarget',     label: 'Prev Target' },
  { key: 'target',         label: 'Target' },
  { key: 'targetChange',   label: 'Change' },
  { key: 'targetChangePct',label: 'Change %' },
  { key: 'price',          label: 'Price' },
  { key: 'potential',      label: 'Potential' },
]

function sortValue(row, key) {
  const v = row[key]
  if (v == null) return null
  return (key === 'date' || key === 'prevDate' || key === 'code' || key === 'consensus') ? v : Number(v)
}

function compareValues(av, bv, dir) {
  if (av == null && bv == null) return 0
  if (av == null) return 1
  if (bv == null) return -1
  const cmp = av < bv ? -1 : av > bv ? 1 : 0
  return dir === 'asc' ? cmp : -cmp
}

function sortData(data, col, dir, col2 = 'targetChangePct', dir2 = 'desc') {
  return [...data].sort((a, b) => {
    const primary = compareValues(sortValue(a, col), sortValue(b, col), dir)
    if (primary !== 0) return primary
    return compareValues(sortValue(a, col2), sortValue(b, col2), dir2)
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

function fmtPct(val) {
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

export default function ValuationChanges() {
  const [direction, setDirection] = useState('RISING')
  const [state, setState] = useState({ data: [], loading: true, error: null })
  const [sortCol, setSortCol] = useState('targetChangePct')
  const [sortDir, setSortDir] = useState('desc')
  const [selectedItem, setSelectedItem] = useState(null)
  const [detail, setDetail] = useState({ data: [], loading: false, error: null })
  const [detailSortCol, setDetailSortCol] = useState('date')
  const [detailSortDir, setDetailSortDir] = useState('desc')

  useEffect(() => {
    setState({ data: [], loading: true, error: null })
    fetch(`/api/valuations/changes?direction=${direction}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setState({ data, loading: false, error: null }))
      .catch(err => setState({ data: [], loading: false, error: err.message }))
  }, [direction])

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
  const detailSorted = useMemo(
    () => sortData(detail.data, detailSortCol, detailSortDir, 'potential', 'desc'),
    [detail.data, detailSortCol, detailSortDir]
  )

  function handleSort(key) {
    if (key === sortCol) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(key)
      setSortDir('desc')
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
            <SortableHeader columns={DETAIL_COLUMNS} sortCol={detailSortCol} sortDir={detailSortDir} onSort={handleDetailSort} />
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
                    {fmtPct(v.potential)}
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

  const changeClass = direction === 'RISING' ? 'pnl-positive' : 'pnl-negative'
  const emptyMsg = direction === 'RISING'
    ? 'No stocks with an increased target price.'
    : 'No stocks with a decreased target price.'

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '0.5rem' }}>
        <h1 style={{ margin: 0 }}>Target Price Changes</h1>
        <div className="toggle-group">
          <button
            className={`toggle-btn${direction === 'RISING' ? ' toggle-btn--active' : ''}`}
            onClick={() => setDirection('RISING')}
          >Rising</button>
          <button
            className={`toggle-btn${direction === 'FALLING' ? ' toggle-btn--active' : ''}`}
            onClick={() => setDirection('FALLING')}
          >Falling</button>
        </div>
      </div>
      {state.loading ? (
        <p>Loading…</p>
      ) : state.error ? (
        <p className="error">Error: {state.error}</p>
      ) : state.data.length === 0 ? (
        <p className="empty">{emptyMsg}</p>
      ) : (
        <table className="trades-table">
          <SortableHeader columns={COLUMNS} sortCol={sortCol} sortDir={sortDir} onSort={handleSort} />
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
                <td>{fmtPrice(v.prevTarget)}</td>
                <td>{fmtPrice(v.target)}</td>
                <td className={changeClass}>{fmtPrice(v.targetChange)}</td>
                <td className={changeClass}>{fmtPct(v.targetChangePct)}</td>
                <td>{fmtPrice(v.price)}</td>
                <td className={potentialClass(v.potential)}>
                  {fmtPct(v.potential)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
