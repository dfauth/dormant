import { useEffect, useState } from 'react'

const CONSENSUS_LABEL = {
  STRONG_BUY: 'Strong Buy',
  BUY: 'Buy',
  HOLD: 'Hold',
  SELL: 'Sell',
  STRONG_SELL: 'Strong Sell',
}

function consensusClass(consensus) {
  if (consensus === 'STRONG_BUY' || consensus === 'BUY') return 'consensus-buy'
  if (consensus === 'STRONG_SELL' || consensus === 'SELL') return 'consensus-sell'
  return 'consensus-hold'
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

export default function Valuations() {
  const [state, setState] = useState({ data: [], loading: true, error: null })

  useEffect(() => {
    fetch('/api/valuations/recent', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setState({ data, loading: false, error: null }))
      .catch(err => setState({ data: [], loading: false, error: err.message }))
  }, [])

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
          <thead>
            <tr>
              <th>Date</th>
              <th>Code</th>
              <th>Consensus</th>
              <th>Buy</th>
              <th>Hold</th>
              <th>Sell</th>
              <th>Price</th>
              <th>Potential</th>
            </tr>
          </thead>
          <tbody>
            {state.data.map(v => (
              <tr key={`${v.market}-${v.code}`}>
                <td>{v.date}</td>
                <td>{v.code}</td>
                <td className={consensusClass(v.consensus)}>
                  {CONSENSUS_LABEL[v.consensus] ?? v.consensus}
                </td>
                <td>{v.buy}</td>
                <td>{v.hold}</td>
                <td>{v.sell}</td>
                <td>{fmtPrice(v.price)}</td>
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
