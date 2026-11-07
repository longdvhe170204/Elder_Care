import { useState } from 'react'
import { postAlertsByIdMarkArrived } from '@/api'
import { Button } from '@/components/ui/button'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { AlertLocation } from './AlertLocation'
import { Call115Panel } from './Call115Panel'

// Khối chi tiết của cảnh báo Khẩn cấp (007 T102; FR-026 (d), (e), FR-047f → FR-047k): nguồn (nút SOS, nút hỗ trợ),
// người bấm, loại sự việc, nơi đã chọn, vị trí; nút "Đã tới nơi" và "Gọi 115" theo `allowedCommands` của cảnh báo
// (api-007 `Alert`). Màn cảnh báo thật (T031 phần web) truyền `alert` vào; màn hiện còn là bản mẫu nên chưa nối.

const ORIGIN = { SOS: 'Nút SOS của người cao tuổi', ASSIST_BUTTON: 'Nút hỗ trợ khẩn cấp của nhân viên' }
const EVENT = {
  CARDIAC_ARREST_OR_FAINTING: 'Ngừng tim, ngất',
  FALL: 'Ngã',
  AGITATION_OR_VIOLENCE: 'Kích động, bạo lực',
  OTHER: 'Khác',
}
const PLACE = {
  ROOM: 'Phòng',
  RESTROOM: 'Nhà vệ sinh',
  CORRIDOR: 'Hành lang',
  COMMON_ROOM: 'Phòng sinh hoạt chung',
  YARD: 'Sân',
  OTHER: 'Nơi khác',
}
const timeFormat = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })

export function PressedAlertPanel({ alert, onChanged, dial }) {
  const commands = alert.allowedCommands ?? []
  const [arriveKey] = useState(() => crypto.randomUUID())
  const [arrivedAt, setArrivedAt] = useState(alert.arrivedAt ?? null)
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState(null)

  async function markArrived() {
    setBusy(true)
    setProblem(null)
    try {
      await postAlertsByIdMarkArrived({ path: { id: alert.id }, headers: { 'Idempotency-Key': arriveKey } })
      setArrivedAt(new Date().toISOString())
      onChanged?.()
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const facts = [
    ['Nguồn', ORIGIN[alert.origin]],
    ['Người bấm', alert.pressedBy],
    ['Loại sự việc', EVENT[alert.eventType]],
    ['Nơi đã chọn', PLACE[alert.chosenPlace]],
  ].filter(([, value]) => value)

  return (
    <section aria-label="Cảnh báo khẩn cấp" className="flex flex-col gap-4">
      {facts.length > 0 && (
        <dl className="grid gap-3 sm:grid-cols-2">
          {facts.map(([label, value]) => (
            <div key={label} className="flex flex-col">
              <dt className="type-caption text-muted-foreground">{label}</dt>
              <dd className="type-body-sm">{value}</dd>
            </div>
          ))}
        </dl>
      )}
      {(alert.origin === 'SOS' || alert.origin === 'ASSIST_BUTTON') && (
        <AlertLocation location={alert.latestLocation} />
      )}
      {arrivedAt ? (
        <p className="type-body-sm tabular-nums">Đã tới nơi lúc {timeFormat.format(new Date(arrivedAt))}.</p>
      ) : (
        commands.includes('mark-arrived') && (
          <Button className="h-11 self-start" onClick={markArrived} disabled={busy}>
            Đã tới nơi
          </Button>
        )
      )}
      <ProblemAlert problem={problem} />
      {commands.includes('call-115') && <Call115Panel subject="alerts" id={alert.id} dial={dial} />}
    </section>
  )
}
