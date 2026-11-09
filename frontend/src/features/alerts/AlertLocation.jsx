import { useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { Badge } from '@/components/ui/badge'

// Vị trí GPS của cảnh báo từ nút SOS, nút hỗ trợ (007 T102; FR-047h, BR-M05-28; research S-24, S-26). Nhận
// `LocationView` của api-007 (latitude, longitude, accuracyMeters, measuredAt, possiblyStale, isPrimary). Vị trí chính
// (người cao tuổi đang ở Hoạt động bên ngoài) thì có bản đồ Leaflet; trong viện chỉ là vị trí tham khảo. Luôn có liên kết
// chỉ đường. Backend chưa trả vị trí (T096) thì hiện "Chưa có vị trí GPS".

const TILE = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
const timeFormat = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })

function LocationMap({ latitude, longitude, accuracyMeters }) {
  const box = useRef(null)
  useEffect(() => {
    if (!box.current) return undefined
    const map = L.map(box.current).setView([latitude, longitude], 17)
    L.tileLayer(TILE, { attribution: '© OpenStreetMap' }).addTo(map)
    L.circle([latitude, longitude], { radius: accuracyMeters ?? 0 }).addTo(map)
    L.circleMarker([latitude, longitude], { radius: 6 }).addTo(map)
    return () => map.remove()
  }, [latitude, longitude, accuracyMeters])
  return (
    <div
      ref={box}
      role="img"
      aria-label={`Bản đồ vị trí, sai số khoảng ${Math.round(accuracyMeters ?? 0)} mét`}
      className="h-56 w-full overflow-hidden rounded-lg border"
    />
  )
}

export function AlertLocation({ location }) {
  if (!location || location.latitude == null || location.longitude == null) {
    return <p className="type-body-sm text-muted-foreground">Chưa có vị trí GPS.</p>
  }
  const { latitude, longitude, accuracyMeters, measuredAt, possiblyStale, isPrimary } = location
  const directions = `https://www.google.com/maps/dir/?api=1&destination=${latitude},${longitude}`
  return (
    <section aria-label="Vị trí" className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <span className="type-label">{isPrimary ? 'Vị trí chính' : 'Vị trí tham khảo'}</span>
        {possiblyStale && <Badge variant="attention">Có thể đã cũ</Badge>}
      </div>
      <p className="type-body-sm text-muted-foreground tabular-nums">
        Sai số khoảng {Math.round(accuracyMeters ?? 0)} mét
        {measuredAt && `, đo lúc ${timeFormat.format(new Date(measuredAt))}`}
        {!isPrimary && ', người cao tuổi đang trong viện nên chỉ để tham khảo'}
      </p>
      {isPrimary && <LocationMap latitude={latitude} longitude={longitude} accuracyMeters={accuracyMeters} />}
      <a href={directions} target="_blank" rel="noreferrer" className="type-body-sm underline">
        Mở chỉ đường
      </a>
    </section>
  )
}
