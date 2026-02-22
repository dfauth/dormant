import { useState, useEffect, useRef } from 'react'
import './App.css'
import PriceSheet from './PriceSheet'
import Valuations from './Valuations'
import MarketDepth from './MarketDepth'

const NAV_ITEMS = [
  { key: 'positions',  label: 'Positions',    subItems: ['open positions'] },
  { key: 'trades',     label: 'Trades',       subItems: ['default'] },
  { key: 'prices',     label: 'Prices',       subItems: ['default'] },
  { key: 'valuations', label: 'Valuations',   subItems: ['default'] },
  { key: 'depth',      label: 'Market Depth', subItems: ['default'] },
]

export default function App() {
  const [state, setState] = useState({ status: 'loading', trades: [], error: null })
  const [page, setPage] = useState('positions')
  const [subPage, setSubPage] = useState('default')
  const [openMenu, setOpenMenu] = useState(null)
  const [positions, setPositions] = useState({ data: [], loading: false, error: null })
  const [selectedPosition, setSelectedPosition] = useState(null)
  const [positionTrades, setPositionTrades] = useState({ data: [], loading: false, error: null })
  const navRef = useRef(null)

  function handleSetPage(p) {
    setPage(p)
    setSubPage('default')
    setSelectedPosition(null)
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
    fetch('/api/positions', { credentials: 'include' })
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

        {page === 'positions' && !selectedPosition && (
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
                    <th>Market</th>
                    <th>Code</th>
                    <th>Side</th>
                    <th>Size</th>
                    <th>Avg Price</th>
                    <th>Realised P&amp;L</th>
                    <th>Opened</th>
                  </tr>
                </thead>
                <tbody>
                  {positions.data.map((p, i) => (
                    <tr key={`${p.market}-${p.code}-${i}`}>
                      <td>{p.market}</td>
                      <td
                        className="code-link"
                        onClick={() => setSelectedPosition({ market: p.market, code: p.code })}
                      >
                        {p.code}
                      </td>
                      <td>{p.side}</td>
                      <td>{p.size}</td>
                      <td>{p.averagePrice}</td>
                      <td className={Number(p.realisedPnl) >= 0 ? 'pnl-positive' : 'pnl-negative'}>{p.realisedPnl}</td>
                      <td>{p.openDate ?? '—'}</td>
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
