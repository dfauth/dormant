import { useState, useEffect } from 'react'
import './App.css'
import PriceSheet from './PriceSheet'
import Valuations from './Valuations'

export default function App() {
  const [state, setState] = useState({ status: 'loading', trades: [], error: null })
  const [page, setPage] = useState('trades')
  const [positions, setPositions] = useState({ data: [], loading: false, error: null })

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
      <nav className="nav">
        <span className={`nav-item ${page === 'trades' ? 'active' : ''}`} onClick={() => setPage('trades')}>Trades</span>
        <span className={`nav-item ${page === 'positions' ? 'active' : ''}`} onClick={() => setPage('positions')}>Positions</span>
        <span className={`nav-item ${page === 'prices' ? 'active' : ''}`} onClick={() => setPage('prices')}>Prices</span>
        <span className={`nav-item ${page === 'valuations' ? 'active' : ''}`} onClick={() => setPage('valuations')}>Valuations</span>
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
                  </tr>
                </thead>
                <tbody>
                  {state.trades.map((t, i) => (
                    <tr key={t.id ?? i}>
                      <td>{t.tradeDate ?? t.date ?? '—'}</td>
                      <td>{t.code}</td>
                      <td>{t.side}</td>
                      <td>{t.size}</td>
                      <td>{t.price}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}

        {page === 'positions' && (
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
                      <td>{p.code}</td>
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

        {page === 'prices' && <PriceSheet />}
        {page === 'valuations' && <Valuations />}
      </div>
    </>
  )
}
