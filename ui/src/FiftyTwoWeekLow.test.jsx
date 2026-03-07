import { describe, it, expect, vi, beforeAll, afterAll, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

vi.mock('./PriceChart', () => ({
  default: ({ market, code }) => (
    <div data-testid="price-chart">{market}:{code}</div>
  ),
}))

// eslint-disable-next-line import/first
import FiftyTwoWeekLow from './FiftyTwoWeekLow'

const MOCK_ROWS = [
  {
    waterMark: { market: 'ASX', code: 'BHP', date: '2025-06-01', close: 100.0 },
    current:   { market: 'ASX', code: 'BHP', date: '2026-02-01', close: 96.0 },
    intervalsSince: 1,
    distance: -0.04,
  },
  {
    waterMark: { market: 'ASX', code: 'CBA', date: '2025-08-01', close: 200.0 },
    current:   { market: 'ASX', code: 'CBA', date: '2026-02-15', close: 194.0 },
    intervalsSince: 3,
    distance: -0.03,
  },
]

const server = setupServer(
  http.get('/api/prices/watermark', () => HttpResponse.json(MOCK_ROWS)),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterAll(() => server.close())
afterEach(() => server.resetHandlers())

describe('FiftyTwoWeekLow', () => {
  it('renders loading state initially', () => {
    render(<FiftyTwoWeekLow />)
    expect(screen.getByText('Loading…')).toBeInTheDocument()
  })

  it('renders table rows after data loads', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    expect(screen.getByText('CBA')).toBeInTheDocument()
    // watermark and current prices
    expect(screen.getByText('100.00')).toBeInTheDocument()
    expect(screen.getByText('96.00')).toBeInTheDocument()
    // distance %
    expect(screen.getByText('-4.00%')).toBeInTheDocument()
    // days since
    expect(screen.getByText('1')).toBeInTheDocument()
  })

  it('defaults to direction=RISING, tenor=1Y, threshold=5', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    expect(screen.getByDisplayValue('RISING')).toBeInTheDocument()
    expect(screen.getByDisplayValue('1Y')).toBeInTheDocument()
    // slider value
    expect(screen.getByRole('slider')).toHaveValue('5')
    // live label beside slider
    expect(screen.getByText('5%')).toBeInTheDocument()
  })

  it('title reflects default tenor and direction', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())
    expect(screen.getByRole('heading', { name: '1Y High' })).toBeInTheDocument()
  })

  it('sorts by distancePct ascending by default — most negative first', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    const cells = screen.getAllByRole('cell', { name: /^(BHP|CBA)$/ })
    expect(cells[0]).toHaveTextContent('BHP') // -4% < -3%
    expect(cells[1]).toHaveTextContent('CBA')
  })

  it('clicking the Distance % header reverses the sort', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText(/Distance %/i))

    const cells = screen.getAllByRole('cell', { name: /^(BHP|CBA)$/ })
    expect(cells[0]).toHaveTextContent('CBA') // descending: -3% before -4%
    expect(cells[1]).toHaveTextContent('BHP')
  })

  it('threshold label updates immediately when the slider moves', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    const slider = screen.getByRole('slider')
    fireEvent.change(slider, { target: { value: '15' } })

    expect(screen.getByText('15%')).toBeInTheDocument()
  })

  it('changing direction triggers a re-fetch with the new param', async () => {
    let capturedUrl = null
    server.use(
      http.get('/api/prices/watermark', ({ request }) => {
        capturedUrl = request.url
        return HttpResponse.json(MOCK_ROWS)
      }),
    )

    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.change(screen.getByDisplayValue('RISING'), { target: { value: 'FALLING' } })

    await waitFor(() => expect(capturedUrl).toContain('direction=FALLING'))
  })

  it('changing tenor triggers a re-fetch with the new param', async () => {
    let capturedUrl = null
    server.use(
      http.get('/api/prices/watermark', ({ request }) => {
        capturedUrl = request.url
        return HttpResponse.json(MOCK_ROWS)
      }),
    )

    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.change(screen.getByDisplayValue('1Y'), { target: { value: '6M' } })

    await waitFor(() => expect(capturedUrl).toContain('tenor=6M'))
  })

  it('clicking a code row renders the price chart', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText('BHP'))

    expect(await screen.findByTestId('price-chart')).toHaveTextContent('ASX:BHP')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('back button returns to the table', async () => {
    render(<FiftyTwoWeekLow />)
    await waitFor(() => expect(screen.getByText('BHP')).toBeInTheDocument())

    fireEvent.click(screen.getByText('BHP'))
    await screen.findByTestId('price-chart')

    fireEvent.click(screen.getByText(/← Back/i))
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })

  it('shows empty message including current threshold when no results', async () => {
    server.use(http.get('/api/prices/watermark', () => HttpResponse.json([])))

    render(<FiftyTwoWeekLow />)
    await waitFor(() =>
      expect(screen.getByText(/within 5% of their 1y high/i)).toBeInTheDocument(),
    )
  })

  it('shows error message when fetch fails', async () => {
    server.use(http.get('/api/prices/watermark', () => HttpResponse.error()))

    render(<FiftyTwoWeekLow />)
    await waitFor(() =>
      expect(screen.getByText(/Error:/i)).toBeInTheDocument(),
    )
  })
})
