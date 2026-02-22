import { useState, useEffect, useRef } from 'react'
import './App.css'
import PriceSheet from './PriceSheet'
import Valuations from './Valuations'
import MarketDepth from './MarketDepth'

const NAV_ITEMS = [
  { key: 'positions',  label: 'Positions',    subItems: ['open positions', 'closed positions'] },
  { key: 'trades',     label: 'Trades',       subItems: ['default'] },
  { key: 'prices',     label: 'Prices',       subItems: ['default'] },
  { key: 'valuations', label: 'Valuations',   subItems: ['default'] },
  { key: 'depth',      label: 'Market Depth', subItems: ['default'] },
]

function useSort(initialCol, initialDir = 'asc') {
  const [sortCol, setSortCol] = useState(initialCol)
  const [sortDir, setSortDir] = useState(initialDir)

  function onSort(col) {
    if (col === sortCol) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc')
    } else {
      setSortCol(col)
      setSortDir('asc')
    }
  }

  function icon(col) {
    if (col !== sortCol) return '↕'
    return sortDir === 'asc' ? '↑' : '↓'
  }

  function sort(data, getVal) {
    return [...data].sort((a, b) => {
      const av = getVal(a, sortCol)
      const bv = getVal(b, sortCol)
      if (av == null && bv == null) return 0
      if (av == null) return 1
      if (bv == null) return -1
      const cmp = typeof av === 'string' ? av.localeCompare(bv) : av < bv ? -1 : av > bv ? 1 : 0
      return sortDir === 'asc' ? cmp : -cmp
    })
  }

  return { sortCol, onSort, icon, sort }
}

function positionVal(p, col) {
  if (col === 'trades')       return p.trades?.length ?? 0
  if (col === 'size')         return parseFloat(p.size ?? 0)
  if (col === 'averagePrice') return parseFloat(p.averagePrice ?? 0)
  if (col === 'realisedPnl')  return parseFloat(p.realisedPnl ?? 0)
  return p[col] ?? ''
}

function SortTh({ label, col, sort }) {
  return (
    <th className="th-sortable" onClick={() => sort.onSort(col)}>
      {label}<span className="th-sort-icon">{sort.icon(col)}</span>
    </th>
  )
}

export default function App() {
  const [state, setState] = useState({ status: 'loading', trades: [], error: null })
  const [page, setPage] = useState('positions')
  const [subPage, setSubPage] = useState('default')
  const [openMenu, setOpenMenu] = useState(null)
  const [positions, setPositions] = useState({ data: [], loading: false, error: null })
  const [selectedPosition, setSelectedPosition] = useState(null)
  const [positionTrades, setPositionTrades] = useState({ data: [], loading: false, error: null })
  const [selectedCode, setSelectedCode] = useState(null)
  const [codePositions, setCodePositions] = useState({ data: [], loading: false, error: null })
  const [closedPositions, setClosedPositions] = useState({ data: [], loading: false, error: null })
  const navRef = useRef(null)

  const openSort   = useSort('openDate')
  const closedSort = useSort('openDate')
  const historySort = useSort('openDate')

  function handleSetPage(p) {
    setPage(p)
    setSubPage('default')
    setSelectedPosition(null)
    setSelectedCode(null)
  }

  useEffect(() => {
    function handleClickOutside(e) {
      if (navRef.current && !navRef.current.contains(e.target)) {
        setOpenMenu(null)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  useEffect(() => {
    fetch('/api/trades', { credentials: 'include' })
      .then(res => {
        if (res.status === 401 || res.redirected) {
          setState({ status: 'unauthenticated', trades: [], error: null })
          return null
        }
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => {
        if (data !== null) setState({ status: 'authenticated', trades: data, error: null })
      })
      .catch(err => setState({ status: 'error', trades: [], error: err.message }))
  }, [])

  useEffect(() => {
    if (page !== 'positions' || state.status !== 'authenticated') return

    setPositions(p => ({ ...p, loading: true, error: null }))
    fetch('/api/positions/open', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setPositions({ data, loading: false, error: null }))
      .catch(err => setPositions({ data: [], loading: false, error: err.message }))
  }, [page, state.status])

  useEffect(() => {
    if (!selectedPosition) return

    setPositionTrades({ data: [], loading: true, error: null })
    fetch(`/api/positions/market/${selectedPosition.market}/code/${selectedPosition.code}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(positions => {
        const trades = positions.flatMap(pos => pos.trades ?? [])
        setPositionTrades({ data: trades, loading: false, error: null })
      })
      .catch(err => setPositionTrades({ data: [], loading: false, error: err.message }))
  }, [selectedPosition])

  useEffect(() => {
    if (!selectedCode) return

    setCodePositions({ data: [], loading: true, error: null })
    fetch(`/api/positions/market/${selectedCode.market}/code/${selectedCode.code}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setCodePositions({ data, loading: false, error: null }))
      .catch(err => setCodePositions({ data: [], loading: false, error: err.message }))
  }, [selectedCode])

  useEffect(() => {
    if (page !== 'positions' || subPage !== 'closed positions' || state.status !== 'authenticated') return

    setClosedPositions(p => ({ ...p, loading: true, error: null }))
    fetch('/api/positions/closed', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setClosedPositions({ data, loading: false, error: null }))
      .catch(err => setClosedPositions({ data: [], loading: false, error: err.message }))
  }, [page, subPage, state.status])

  if (state.status === 'loading') return <p className="page">Loading…</p>

  if (state.status === 'error') return <p className="page error">Error: {state.error}</p>

  if (state.status === 'unauthenticated') {
    return (
      <div className="login">
        <h1>Trade UI</h1>
        <a href="/oauth2/authorization/google">
          <button className="login-btn">Login with Google</button>
        </a>
      </div>
    )
  }

  // Pre-compute position history rows so all derived fields are sortable
  const historyRows = historySort.sort(
    codePositions.data.map(p => {
      const trades = p.trades ?? []
      const exitSide = p.side === 'BUY' ? 'SELL' : 'BUY'
      const purchaseValue = trades
        .filter(t => t.side === p.side)
        .reduce((sum, t) => sum + parseFloat(t.cost), 0)
      const saleValue = trades
        .filter(t => t.side === exitSide)
        .reduce((sum, t) => sum + parseFloat(t.cost), 0)
      const commission = trades.reduce((sum, t) =>
        sum + (parseFloat(t.cost) - parseFloat(t.price) * parseFloat(t.size)), 0)
      const pnl = parseFloat(p.realisedPnl ?? 0)
      const returnPct = purchaseValue > 0 ? (pnl / purchaseValue * 100) : null
      let cagr = null
      if (!p.open && p.closeDate && p.openDate && returnPct !== null) {
        const days = (new Date(p.closeDate) - new Date(p.openDate)) / 86400000
        if (days > 0) cagr = (Math.pow(1 + returnPct / 100, 365 / days) - 1) * 100
      }
      return {
        openDate: p.openDate, closeDate: p.closeDate,
        purchaseValue, saleValue, commission, pnl,
        returnPct, tradeCount: trades.length, cagr,
        realisedPnl: p.realisedPnl,
      }
    }),
    (r, col) => r[col] ?? (typeof r[col] === 'number' ? 0 : '')
  )

  return (
    <>
      <nav className="nav" ref={navRef}>
        {NAV_ITEMS.map(item => (
          <div key={item.key} className="nav-dropdown-wrapper">
            <span
              className={`nav-item ${page === item.key ? 'active' : ''}`}
              onClick={() => {
                handleSetPage(item.key)
                setOpenMenu(openMenu === item.key ? null : item.key)
              }}
            >
              {item.label}
            </span>
            {openMenu === item.key && (
              <div className="nav-dropdown">
                {item.subItems.map(sub => (
                  <span
                    key={sub}
                    className={`nav-dropdown-item ${subPage === sub ? 'active' : ''}`}
                    onClick={() => { setSubPage(sub); setOpenMenu(null) }}
                  >
                    {sub}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
        <a href="/logout" className="nav-item logout">Logout</a>
      </nav>

      <div className={page === 'prices' ? 'page-prices' : 'page'}>
        {page === 'trades' && (
          <>
            <h1>Trades — ASX</h1>
            {state.trades.length === 0 ? (
              <p className="empty">No trades found.</p>
            ) : (
              <table className="trades-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Code</th>
                    <th>Side</th>
                    <th>Size</th>
                    <th>Price</th>
                    <th>Value</th>
                    <th>Commission</th>
                  </tr>
                </thead>
                <tbody>
                  {state.trades.map((t, i) => {
                    const commission = t.cost != null && t.price != null && t.size != null
                      ? (parseFloat(t.cost) - parseFloat(t.price) * parseFloat(t.size)).toFixed(2)
                      : '—'
                    return (
                      <tr key={t.id ?? i}>
                        <td>{t.tradeDate ?? t.date ?? '—'}</td>
                        <td>{t.code}</td>
                        <td>{t.side}</td>
                        <td>{t.size}</td>
                        <td>{t.price}</td>
                        <td>{t.cost ?? '—'}</td>
                        <td>{commission}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </>
        )}

        {page === 'positions' && subPage !== 'closed positions' && !selectedPosition && !selectedCode && (
          <>
            <h1>Open Positions</h1>
            {positions.loading ? (
              <p>Loading…</p>
            ) : positions.error ? (
              <p className="error">Error: {positions.error}</p>
            ) : positions.data.length === 0 ? (
              <p className="empty">No open positions.</p>
            ) : (
              <table className="trades-table">
                <thead>
                  <tr>
                    <SortTh label="Market"       col="market"       sort={openSort} />
                    <SortTh label="Code"         col="code"         sort={openSort} />
                    <SortTh label="Side"         col="side"         sort={openSort} />
                    <SortTh label="Size"         col="size"         sort={openSort} />
                    <SortTh label="Avg Price"    col="averagePrice" sort={openSort} />
                    <SortTh label="Realised P&L" col="realisedPnl"  sort={openSort} />
                    <SortTh label="Opened"       col="openDate"     sort={openSort} />
                    <SortTh label="Trades"       col="trades"       sort={openSort} />
                  </tr>
                </thead>
                <tbody>
                  {openSort.sort(positions.data, positionVal).map((p, i) => (
                    <tr key={`${p.market}-${p.code}-${i}`}>
                      <td>{p.market}</td>
                      <td className="code-link" onClick={() => setSelectedCode({ market: p.market, code: p.code })}>
                        {p.code}
                      </td>
                      <td>{p.side}</td>
                      <td>{p.size}</td>
                      <td>{p.averagePrice}</td>
                      <td className={Number(p.realisedPnl) >= 0 ? 'pnl-positive' : 'pnl-negative'}>{p.realisedPnl}</td>
                      <td>{p.openDate ?? '—'}</td>
                      <td className="code-link" onClick={() => setSelectedPosition({ market: p.market, code: p.code })}>
                        {p.trades?.length ?? 0}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}

        {page === 'positions' && subPage === 'closed positions' && !selectedPosition && !selectedCode && (
          <>
            <h1>Closed Positions</h1>
            {closedPositions.loading ? (
              <p>Loading…</p>
            ) : closedPositions.error ? (
              <p className="error">Error: {closedPositions.error}</p>
            ) : closedPositions.data.length === 0 ? (
              <p className="empty">No closed positions.</p>
            ) : (
              <table className="trades-table">
                <thead>
                  <tr>
                    <SortTh label="Market"       col="market"       sort={closedSort} />
                    <SortTh label="Code"         col="code"         sort={closedSort} />
                    <SortTh label="Side"         col="side"         sort={closedSort} />
                    <SortTh label="Size"         col="size"         sort={closedSort} />
                    <SortTh label="Avg Price"    col="averagePrice" sort={closedSort} />
                    <SortTh label="Realised P&L" col="realisedPnl"  sort={closedSort} />
                    <SortTh label="Opened"       col="openDate"     sort={closedSort} />
                    <SortTh label="Closed"       col="closeDate"    sort={closedSort} />
                    <SortTh label="Trades"       col="trades"       sort={closedSort} />
                  </tr>
                </thead>
                <tbody>
                  {closedSort.sort(closedPositions.data, positionVal).map((p, i) => (
                    <tr key={`${p.market}-${p.code}-${i}`}>
                      <td>{p.market}</td>
                      <td className="code-link" onClick={() => setSelectedCode({ market: p.market, code: p.code })}>
                        {p.code}
                      </td>
                      <td>{p.side}</td>
                      <td>{p.size}</td>
                      <td>{p.averagePrice}</td>
                      <td className={Number(p.realisedPnl) >= 0 ? 'pnl-positive' : 'pnl-negative'}>{p.realisedPnl}</td>
                      <td>{p.openDate ?? '—'}</td>
                      <td>{p.closeDate ?? '—'}</td>
                      <td className="code-link" onClick={() => setSelectedPosition({ market: p.market, code: p.code })}>
                        {p.trades?.length ?? 0}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}

        {page === 'positions' && selectedCode && (
          <>
            <button className="back-btn" onClick={() => setSelectedCode(null)}>← Back</button>
            <h1>Position History — {selectedCode.code}</h1>
            {codePositions.loading ? (
              <p>Loading…</p>
            ) : codePositions.error ? (
              <p className="error">Error: {codePositions.error}</p>
            ) : historyRows.length === 0 ? (
              <p className="empty">No position history found.</p>
            ) : (
              <table className="trades-table">
                <thead>
                  <tr>
                    <SortTh label="Start"          col="openDate"      sort={historySort} />
                    <SortTh label="End"            col="closeDate"     sort={historySort} />
                    <SortTh label="Purchase Value" col="purchaseValue" sort={historySort} />
                    <SortTh label="Sale Value"     col="saleValue"     sort={historySort} />
                    <SortTh label="Realised P&L"   col="pnl"           sort={historySort} />
                    <SortTh label="Commission"     col="commission"    sort={historySort} />
                    <SortTh label="Return %"       col="returnPct"     sort={historySort} />
                    <SortTh label="Trades"         col="tradeCount"    sort={historySort} />
                    <SortTh label="CAGR"           col="cagr"          sort={historySort} />
                  </tr>
                </thead>
                <tbody>
                  {historyRows.map((r, i) => (
                    <tr key={i}>
                      <td>{r.openDate ?? '—'}</td>
                      <td>{r.closeDate ?? '—'}</td>
                      <td>{r.purchaseValue.toFixed(2)}</td>
                      <td>{r.saleValue > 0 ? r.saleValue.toFixed(2) : '—'}</td>
                      <td className={r.pnl >= 0 ? 'pnl-positive' : 'pnl-negative'}>{r.realisedPnl ?? '—'}</td>
                      <td>{r.commission.toFixed(2)}</td>
                      <td>{r.returnPct !== null ? r.returnPct.toFixed(2) + '%' : '—'}</td>
                      <td>{r.tradeCount}</td>
                      <td>{r.cagr !== null ? r.cagr.toFixed(2) + '%' : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}

        {page === 'positions' && selectedPosition && (
          <>
            <button className="back-btn" onClick={() => setSelectedPosition(null)}>← Back</button>
            <h1>Trades — {selectedPosition.code}</h1>
            {positionTrades.loading ? (
              <p>Loading…</p>
            ) : positionTrades.error ? (
              <p className="error">Error: {positionTrades.error}</p>
            ) : positionTrades.data.length === 0 ? (
              <p className="empty">No trades found.</p>
            ) : (
              <table className="trades-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Code</th>
                    <th>Side</th>
                    <th>Size</th>
                    <th>Price</th>
                    <th>Value</th>
                    <th>Commission</th>
                  </tr>
                </thead>
                <tbody>
                  {positionTrades.data.map((t, i) => {
                    const commission = t.cost != null && t.price != null && t.size != null
                      ? (parseFloat(t.cost) - parseFloat(t.price) * parseFloat(t.size)).toFixed(2)
                      : '—'
                    return (
                      <tr key={t.id ?? i}>
                        <td>{t.tradeDate ?? t.date ?? '—'}</td>
                        <td>{t.code}</td>
                        <td>{t.side}</td>
                        <td>{t.size}</td>
                        <td>{t.price}</td>
                        <td>{t.cost ?? '—'}</td>
                        <td>{commission}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </>
        )}

        {page === 'prices' && <PriceSheet />}
        {page === 'valuations' && <Valuations />}
        {page === 'depth' && <MarketDepth />}
      </div>
    </>
  )
}
