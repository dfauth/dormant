import { useState, useEffect, useMemo } from 'react'
import {
  ComposedChart, Bar, Line, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'

function computeEMA(values, period) {
  const k = 2 / (period + 1)
  const result = new Array(values.length).fill(null)
  if (values.length < period) return result
  let ema = values.slice(0, period).reduce((a, b) => a + b, 0) / period
  result[period - 1] = ema
  for (let i = period; i < values.length; i++) {
    ema = (values[i] - ema) * k + ema
    result[i] = ema
  }
  return result
}

function CandlestickBar({ x, y, width, height, payload }) {
  if (!payload) return null
  const { open, high, low, close } = payload
  if (high === low) return null

  const bullish = close >= open
  const color = bullish ? '#22c55e' : '#ef4444'

  // recharts gives y = top of bar (pixel of high), height = total pixels for high→low range
  const topY = y
  const totalH = Math.abs(height)
  const scale = totalH / (high - low)

  const wickX = x + width / 2
  const bodyTop = topY + (high - Math.max(open, close)) * scale
  const bodyH = Math.max(Math.abs(open - close) * scale, 1)

  return (
    <g>
      <line x1={wickX} y1={topY} x2={wickX} y2={topY + totalH} stroke={color} strokeWidth={1} />
      <rect x={x} y={bodyTop} width={width} height={bodyH} fill={color} />
    </g>
  )
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null
  const d = payload[0]?.payload
  if (!d) return null
  const rows = [
    ['Date',   d.date],
    ['Open',   d.open.toFixed(2)],
    ['High',   d.high.toFixed(2)],
    ['Low',    d.low.toFixed(2)],
    ['Close',  d.close.toFixed(2)],
    ...(d.ema8   != null ? [['EMA 8',   d.ema8.toFixed(2)]]   : []),
    ...(d.ema21  != null ? [['EMA 21',  d.ema21.toFixed(2)]]  : []),
    ...(d.ema200 != null ? [['EMA 200', d.ema200.toFixed(2)]] : []),
  ]
  return (
    <div style={{
      background: '#1e1e38', border: '1px solid #2e2e50', borderRadius: 6,
      padding: '0.6rem 0.9rem', fontSize: 12, color: '#e0e0f0', lineHeight: 1.8,
    }}>
      {rows.map(([label, val]) => (
        <div key={label} style={{ display: 'flex', justifyContent: 'space-between', gap: '1.5rem' }}>
          <span style={{ color: '#aaa' }}>{label}</span>
          <span style={{ fontWeight: 500 }}>{val}</span>
        </div>
      ))}
    </div>
  )
}

export default function PriceChart({ market, code }) {
  const [raw, setRaw] = useState({ prices: [], loading: true, error: null })

  useEffect(() => {
    setRaw({ prices: [], loading: true, error: null })
    fetch(`/api/prices/${code}?market=${market}`, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(prices => setRaw({ prices, loading: false, error: null }))
      .catch(err => setRaw({ prices: [], loading: false, error: err.message }))
  }, [market, code])

  const chartData = useMemo(() => {
    if (!raw.prices.length) return []
    const closes = raw.prices.map(p => parseFloat(p.close))
    const ema8   = computeEMA(closes, 8)
    const ema21  = computeEMA(closes, 21)
    const ema200 = computeEMA(closes, 200)

    const threeMonthsAgo = new Date()
    threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3)

    return raw.prices
      .map((p, i) => {
        const open  = parseFloat(p.open)
        const high  = parseFloat(p.high)
        const low   = parseFloat(p.low)
        const close = parseFloat(p.close)
        return {
          date: p.date, open, high, low, close,
          highLow: [low, high],
          ema8:   ema8[i],
          ema21:  ema21[i],
          ema200: ema200[i],
        }
      })
      .filter(p => new Date(p.date) >= threeMonthsAgo)
  }, [raw.prices])

  const yDomain = useMemo(() => {
    if (!chartData.length) return ['auto', 'auto']
    const vals = chartData.flatMap(d =>
      [d.low, d.high, d.ema8, d.ema21, d.ema200].filter(v => v != null)
    )
    const pad = (Math.max(...vals) - Math.min(...vals)) * 0.04
    return [Math.min(...vals) - pad, Math.max(...vals) + pad]
  }, [chartData])

  const tickInterval = Math.max(1, Math.floor(chartData.length / 10))

  if (raw.loading) return <p>Loading…</p>
  if (raw.error)   return <p className="error">Error: {raw.error}</p>
  if (!chartData.length) return <p className="empty">No price data available.</p>

  return (
    <ResponsiveContainer width="100%" height={480}>
      <ComposedChart data={chartData} margin={{ top: 16, right: 24, bottom: 48, left: 56 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#2e2e50" />
        <XAxis
          dataKey="date"
          stroke="#aaa"
          tick={{ fontSize: 11, fill: '#aaa' }}
          angle={-45}
          textAnchor="end"
          interval={tickInterval}
        />
        <YAxis
          stroke="#aaa"
          tick={{ fontSize: 11, fill: '#aaa' }}
          domain={yDomain}
          tickFormatter={v => v.toFixed(2)}
          width={52}
        />
        <Tooltip content={<ChartTooltip />} />
        <Bar dataKey="highLow" shape={<CandlestickBar />} isAnimationActive={false} />
        <Line dataKey="ema8"   stroke="#f59e0b" dot={false} strokeWidth={1.5} isAnimationActive={false} connectNulls={false} />
        <Line dataKey="ema21"  stroke="#60a5fa" dot={false} strokeWidth={1.5} isAnimationActive={false} connectNulls={false} />
        <Line dataKey="ema200" stroke="#a78bfa" dot={false} strokeWidth={1.5} isAnimationActive={false} connectNulls={false} />
      </ComposedChart>
    </ResponsiveContainer>
  )
}
