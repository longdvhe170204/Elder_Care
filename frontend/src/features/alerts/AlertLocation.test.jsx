import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AlertLocation } from './AlertLocation'

// 007 T102 (tasks ghi ViTriCanhBao.test.jsx; FR-047h): vị trí chính có bản đồ, tham khảo không có; "có thể đã cũ";
// luôn có liên kết chỉ đường; chưa có vị trí thì nói rõ. Leaflet giả để kiểm bản đồ được dựng và gỡ.

const leaflet = vi.hoisted(() => {
  const map = { setView: vi.fn(), remove: vi.fn() }
  map.setView.mockReturnValue(map)
  const layer = () => ({ addTo: vi.fn() })
  return { map, L: { map: vi.fn(() => map), tileLayer: vi.fn(layer), circle: vi.fn(layer), circleMarker: vi.fn(layer) } }
})
vi.mock('leaflet', () => ({ default: leaflet.L }))
vi.mock('leaflet/dist/leaflet.css', () => ({}))

beforeEach(() => {
  vi.clearAllMocks()
  leaflet.map.setView.mockReturnValue(leaflet.map)
})

const POINT = {
  latitude: 10.7769,
  longitude: 106.7009,
  accuracyMeters: 12.4,
  measuredAt: '2026-11-06T03:05:00Z',
  possiblyStale: false,
  isPrimary: true,
}

describe('AlertLocation', () => {
  it('vị trí chính: bản đồ ở tọa độ và liên kết chỉ đường', () => {
    const { unmount } = render(<AlertLocation location={POINT} />)

    expect(screen.getByText('Vị trí chính')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Bản đồ vị trí, sai số khoảng 12 mét' })).toBeInTheDocument()
    expect(leaflet.map.setView).toHaveBeenCalledWith([10.7769, 106.7009], 17)
    expect(screen.getByRole('link', { name: 'Mở chỉ đường' })).toHaveAttribute(
      'href',
      'https://www.google.com/maps/dir/?api=1&destination=10.7769,106.7009',
    )
    unmount()
    expect(leaflet.map.remove).toHaveBeenCalled()
  })

  it('vị trí tham khảo trong viện: không có bản đồ, ghi rõ tham khảo, có thể đã cũ', () => {
    render(<AlertLocation location={{ ...POINT, isPrimary: false, possiblyStale: true }} />)

    expect(screen.getByText('Vị trí tham khảo')).toBeInTheDocument()
    expect(screen.getByText('Có thể đã cũ')).toBeInTheDocument()
    expect(screen.getByText(/chỉ để tham khảo/)).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(leaflet.L.map).not.toHaveBeenCalled()
  })

  it('chưa có vị trí', () => {
    render(<AlertLocation location={null} />)

    expect(screen.getByText('Chưa có vị trí GPS.')).toBeInTheDocument()
  })
})
