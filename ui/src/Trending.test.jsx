import { describe, it, expect, vi, beforeAll, afterAll, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

// Mock PriceChart to avoid recharts/fetch complexity in these tests
vi.mock('./PriceChart', () => ({
  default: ({ market, code }) => (
    <div data-testid="price-chart">{market}:{code}</div>
  ),
}))

// eslint-disable-next-line import/first
import Trending from './Trending'

// Two rows: CBA has higher distanceFromEma than BHP
const MOCK_ROWS = [
  {
    market: 'ASX', code: 'BHP', price: 45.0,
    trendState: { trendState: 'BULL', price: 45.0, fast: [], slow: [], lng: [], diverging: false },
    distanceFromEma: 0.015,
  },
  {
    market: 'ASX', code: 'CBA', price: 95.0,
    trendState: { trendState: 'BULL', price: 95.0, fast: [], slow: [], lng: [], diverging: false },
    distanceFromEma: 0.025,
  },
]

const server = setupServer(
  http.get('/api/prices/trending', () => HttpResponse.json(MOCK_ROWS)),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterAll(() => server.close())
afterEach(() => server.resetHandlers())

describe('Trending', () => {
  it('renders loading state initially', () => {
    render(<Trending />)
    expect(screen.getByText('Loading…')).toBeInTheDocument()
  })

  it('renders table rows after data loads', async () => {
    render(<Trending />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())
    expect(screen.getByText('CBA')).toBeInTheDocument()
    expect(screen.getByText('45.00')).toBeInTheDocument()
    expect(screen.getByText('95.00')).toBeInTheDocument()
  })

  it('sorts by distanceFromEma descending by default — highest first', async () => {
    render(<Trending />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    const cells = screen.getAllByRole('cell', { name: /^(BHP|CBA)$/ })
    expect(cells[0]).toHaveTextContent('CBA')  // 0.025 > 0.015
    expect(cells[1]).toHaveTextContent('BHP')
  })

  it('clicking distanceFromEma header once keeps desc (toggles to asc)', async () => {
    render(<Trending />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText(/Distance from EMA/i))

    const cells = screen.getAllByRole('cell', { name: /^(BHP|CBA)$/ })
    expect(cells[0]).toHaveTextContent('BHP')  // ascending: 0.015 first
    expect(cells[1]).toHaveTextContent('CBA')
  })

  it('clicking code header sorts alphabetically ascending', async () => {
    render(<Trending />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText(/^Code$/i))

    const cells = screen.getAllByRole('cell', { name: /^(BHP|CBA)$/ })
    expect(cells[0]).toHaveTextContent('BHP')
    expect(cells[1]).toHaveTextContent('CBA')
  })

  it('clicking code header twice sorts descending', async () => {
    render(<Trending />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText(/^Code$/i))
    fireEvent.click(screen.getByText(/^Code$/i))

    const cells = screen.getAllByRole('cell', { name: /^(BHP|CBA)$/ })
    expect(cells[0]).toHaveTextContent('CBA')
    expect(cells[1]).toHaveTextContent('BHP')
  })

  it('clicking a code row renders the price chart', async () => {
    render(<Trending />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText('BHP'))

    expect(await screen.findByTestId('price-chart')).toHaveTextContent('ASX:BHP')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('back button returns to the table', async () => {
    render(<Trending />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText('BHP'))
    await screen.findByTestId('price-chart')

    fireEvent.click(screen.getByText(/← Back/i))
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })

  it('shows empty message when no results', async () => {
    server.use(http.get('/api/prices/trending', () => HttpResponse.json([])))

    render(<Trending />)
    await waitFor(() =>
      expect(screen.getByText(/No stocks match/i)).toBeInTheDocument()
    )
  })

  it('shows error message when fetch fails', async () => {
    server.use(http.get('/api/prices/trending', () => HttpResponse.error()))

    render(<Trending />)
    await waitFor(() =>
      expect(screen.getByText(/Error:/i)).toBeInTheDocument()
    )
  })
})
