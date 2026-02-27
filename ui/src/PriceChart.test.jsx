import { describe, it, expect, vi, beforeAll, afterAll, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

// Mock recharts — jsdom has no layout engine so ResponsiveContainer can't measure
// width. We expose the data length so tests can verify the 3-month display window.
vi.mock('recharts', () => ({
  ComposedChart: ({ children, data }) => (
    <div data-testid="chart" data-points={data?.length ?? 0}>{children}</div>
  ),
  Bar:               () => null,
  Line:              ({ dataKey }) => <span data-testid={`line-${dataKey}`} />,
  XAxis:             () => null,
  YAxis:             () => null,
  CartesianGrid:     () => null,
  Tooltip:           () => null,
  ResponsiveContainer: ({ children }) => <div data-testid="chart-container">{children}</div>,
}))

// eslint-disable-next-line import/first
import PriceChart from './PriceChart'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makePrice(daysAgo, close = 10) {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  const date = d.toISOString().slice(0, 10)
  return { date, open: close, high: close, low: close, close, volume: 100_000 }
}

// 250 prices: the last 250 calendar days (today = index 0)
const PRICES_250 = Array.from({ length: 250 }, (_, i) => makePrice(249 - i, 10 + i * 0.1))

// ---------------------------------------------------------------------------
// MSW server
// ---------------------------------------------------------------------------

const server = setupServer(
  http.get('/api/prices/:code', () => HttpResponse.json(PRICES_250)),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterAll(() => server.close())
afterEach(() => server.resetHandlers())

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('PriceChart', () => {
  it('renders loading state while fetching', () => {
    render(<PriceChart market="ASX" code="BHP" />)
    expect(screen.getByText('Loading…')).toBeInTheDocument()
  })

  it('renders error message when fetch fails', async () => {
    server.use(http.get('/api/prices/:code', () => HttpResponse.error()))

    render(<PriceChart market="ASX" code="BHP" />)
    await waitFor(() =>
      expect(screen.getByText(/Error:/i)).toBeInTheDocument()
    )
  })

  it('renders empty message when price list is empty', async () => {
    server.use(http.get('/api/prices/:code', () => HttpResponse.json([])))

    render(<PriceChart market="ASX" code="BHP" />)
    await waitFor(() =>
      expect(screen.getByText(/No price data available/i)).toBeInTheDocument()
    )
  })

  it('fetches all prices without a tenor param', async () => {
    let capturedUrl
    server.use(
      http.get('/api/prices/:code', ({ request }) => {
        capturedUrl = new URL(request.url)
        return HttpResponse.json(PRICES_250)
      }),
    )

    render(<PriceChart market="ASX" code="BHP" />)
    await waitFor(() => expect(screen.getByTestId('chart')).toBeInTheDocument())

    expect(capturedUrl.searchParams.get('market')).toBe('ASX')
    expect(capturedUrl.searchParams.has('tenor')).toBe(false)
  })

  it('renders the chart and all three EMA lines when data loads', async () => {
    render(<PriceChart market="ASX" code="BHP" />)

    await waitFor(() =>
      expect(screen.getByTestId('chart')).toBeInTheDocument()
    )
    expect(screen.getByTestId('line-ema8')).toBeInTheDocument()
    expect(screen.getByTestId('line-ema21')).toBeInTheDocument()
    expect(screen.getByTestId('line-ema200')).toBeInTheDocument()
  })

  it('only displays the last 3 months of data', async () => {
    render(<PriceChart market="ASX" code="BHP" />)

    await waitFor(() =>
      expect(screen.getByTestId('chart')).toBeInTheDocument()
    )

    // 3 months ≈ 90 calendar days; the 250-price dataset spans ~250 days,
    // so the displayed slice should be well under 100 points.
    const points = Number(screen.getByTestId('chart').getAttribute('data-points'))
    expect(points).toBeGreaterThan(0)
    expect(points).toBeLessThan(100)
  })
})
