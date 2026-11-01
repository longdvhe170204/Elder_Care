import { useEffect, useRef, useState } from 'react'
import {
  getDayCareAttendances,
  getResidents,
  postDayCareAttendancesCheckIn,
  postDayCareAttendancesByIdCheckOut,
  postDayCareAttendanceRecordsByIdCorrect,
} from '@/api'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { usePermissions } from '@/common/permissions/permissions'
import { CareField } from './CareField'
const STATUS = {
  NOT_ARRIVED: 'Chưa đến',
  PRESENT: 'Có mặt',
  LEFT: 'Đã về',
  REPORTED_ABSENT: 'Vắng có báo',
  UNREPORTED_ABSENT: 'Vắng không báo',
  HOLIDAY_CANCELLED: 'Đã hủy do ngày nghỉ',
}
const iso = (v) => (v ? `${v}:00+07:00` : undefined)
export function DayCareAttendancePage({ initialDate = '' }) {
  const { status, can } = usePermissions(),
    view = can('DAY_CARE_ATTENDANCE', 'VIEW'),
    write = can('DAY_CARE_ATTENDANCE', 'CREATE')
  const [date, setDate] = useState(initialDate),
    [rows, setRows] = useState([]),
    [people, setPeople] = useState([]),
    [cursor, setCursor] = useState(null)
  const [resident, setResident] = useState(''),
    [arrivalAt, setArrivalAt] = useState(''),
    [pickups, setPickups] = useState({}),
    [departureTimes, setDepartureTimes] = useState({})
  const [problem, setProblem] = useState(null),
    [message, setMessage] = useState(''),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(false)
  const [recordId, setRecordId] = useState(''),
    [correctAt, setCorrectAt] = useState(''),
    [reason, setReason] = useState('')
  const attempts = useRef(new Map())
  useEffect(() => {
    if (status !== 'ready' || !view) return
    let current = true
    getResidents()
      .then(({ data }) => {
        if (current) {
          setPeople(data.items)
          setCursor(data.nextCursor)
        }
      })
      .catch((e) => {
        if (current) setProblem(problemOf(e))
      })
    return () => {
      current = false
    }
  }, [status, view])
  useEffect(() => {
    if (status !== 'ready' || !view || !date) return
    let current = true
    Promise.resolve().then(() => {
      if (current) setLoading(true)
    })
    getDayCareAttendances({ query: { date } })
      .then(({ data }) => {
        if (current) {
          setRows(data)
          setProblem(null)
        }
      })
      .catch((e) => {
        if (current) {
          setRows([])
          setProblem(problemOf(e))
        }
      })
      .finally(() => {
        if (current) setLoading(false)
      })
    return () => {
      current = false
    }
  }, [status, view, date])
  async function refresh() {
    if (date) {
      const { data } = await getDayCareAttendances({ query: { date } })
      setRows(data)
    }
  }
  async function command(signature, send, success) {
    if (busy) return
    setBusy(true)
    setProblem(null)
    setMessage('')
    if (!attempts.current.has(signature)) attempts.current.set(signature, crypto.randomUUID())
    try {
      await send({ 'Idempotency-Key': attempts.current.get(signature) })
      attempts.current.delete(signature)
      setMessage(success)
      await refresh()
    } catch (e) {
      setProblem(problemOf(e))
    } finally {
      setBusy(false)
    }
  }
  async function more() {
    try {
      const { data } = await getResidents({ query: { cursor } })
      setPeople((old) => [...old, ...data.items])
      setCursor(data.nextCursor)
    } catch (e) {
      setProblem(problemOf(e))
    }
  }
  if (status === 'ready' && !view) return <PermissionDeniedState label="Điểm danh bán trú" />
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Điểm danh bán trú" subtitle="Đối chiếu hiện diện, ghi đến và về bằng lệnh trực tuyến." />
      <div className="max-w-sm">
        <CareField label="Ngày xem trạng thái" type="date" value={date} onChange={setDate} disabled={busy} />
      </div>
      <ProblemAlert problem={problem} />
      {message && (
        <p role="status" className="rounded-xl border bg-card p-4">
          {message}
        </p>
      )}
      {loading && <p role="status">Đang tải trạng thái có mặt…</p>}
      {write && (
        <form
          className="grid gap-4 rounded-2xl border bg-card p-5 md:grid-cols-2"
          onSubmit={(e) => {
            e.preventDefault()
            const body = { residentId: Number(resident), at: iso(arrivalAt) }
            command(
              JSON.stringify(['arrive', body]),
              (headers) => postDayCareAttendancesCheckIn({ body, headers }),
              'Đã điểm danh đến.',
            )
          }}
        >
          <h2 className="type-section md:col-span-2">Điểm danh đến</h2>
          <CareField
            label="Người cao tuổi đến"
            options={Object.fromEntries(people.map((p) => [p.id, p.fullName]))}
            value={resident}
            onChange={setResident}
            required
            disabled={busy}
          />
          <CareField
            label="Giờ đến thực tế (để trống dùng lúc lệnh)"
            type="datetime-local"
            value={arrivalAt}
            onChange={setArrivalAt}
            disabled={busy}
          />
          {cursor && (
            <Button type="button" variant="outline" onClick={more}>
              Tải thêm người cao tuổi
            </Button>
          )}
          <div className="md:col-span-2">
            <Button type="submit" disabled={busy || !resident}>
              Điểm danh đến
            </Button>
          </div>
        </form>
      )}
      {!date && <p>Chọn ngày để xem trạng thái có mặt.</p>}
      {date && !loading && !rows.length && <p>Chưa có trạng thái có mặt trong ngày đã chọn.</p>}
      <div className="grid gap-4 xl:grid-cols-2">
        {rows.map((row) => (
          <article key={row.id} className="flex min-w-0 flex-col gap-3 rounded-2xl border bg-card p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <h2 className="type-section">
                {people.find((p) => p.id === row.residentId)?.fullName || `Người cao tuổi · mã ${row.residentId}`}
              </h2>
              <span className="rounded-lg bg-muted px-3 py-1 font-medium">{STATUS[row.status]}</span>
            </div>
            {row.adHocSession && <p>Buổi phát sinh</p>}
            {row.lateAbsenceReported && <p>Báo vắng muộn đã được lưu kèm.</p>}
            <dl className="grid grid-cols-2 gap-2">
              <dt>Dự kiến đến</dt>
              <dd>{row.expectedArrival || 'Chưa có'}</dd>
              <dt>Dự kiến về hiện hành</dt>
              <dd>{row.currentExpectedReturn || 'Chưa có'}</dd>
              <dt>Đã đến</dt>
              <dd>{row.arrivedAt ? formatDateTime(row.arrivedAt) : 'Chưa điểm danh'}</dd>
              <dt>Đã về</dt>
              <dd>{row.leftAt ? formatDateTime(row.leftAt) : 'Chưa điểm danh'}</dd>
            </dl>
            {write && row.status === 'PRESENT' && (
              <form
                className="flex flex-col gap-3 border-t pt-4"
                onSubmit={(e) => {
                  e.preventDefault()
                  const body = { pickupRecordId: Number(pickups[row.id]), at: iso(departureTimes[row.id]) }
                  command(
                    JSON.stringify(['leave', row.id, body]),
                    (headers) => postDayCareAttendancesByIdCheckOut({ path: { id: row.id }, body, headers }),
                    'Đã điểm danh về; các công việc chưa đến hạn liên quan đã được xử lý.',
                  )
                }}
              >
                <CareField
                  label={`Bản ghi đón của dòng ${row.id}`}
                  type="number"
                  min="1"
                  value={pickups[row.id]}
                  onChange={(v) => setPickups((old) => ({ ...old, [row.id]: v }))}
                  required
                  disabled={busy}
                />
                <CareField
                  label={`Giờ về thực tế dòng ${row.id} (để trống dùng lúc lệnh)`}
                  type="datetime-local"
                  value={departureTimes[row.id]}
                  onChange={(v) => setDepartureTimes((old) => ({ ...old, [row.id]: v }))}
                  disabled={busy}
                />
                <Button disabled={busy || !pickups[row.id]} type="submit">
                  Điểm danh về dòng {row.id}
                </Button>
              </form>
            )}
          </article>
        ))}
      </div>
      {write && (
        <details className="rounded-2xl border bg-card p-5">
          <summary className="cursor-pointer font-medium">Đính chính giờ điểm danh đã ghi sai</summary>
          <form
            className="mt-4 grid gap-4 md:grid-cols-2"
            onSubmit={(e) => {
              e.preventDefault()
              const body = { type: 'AMEND_CONTENT', correctedContent: { at: iso(correctAt) }, reason }
              command(
                JSON.stringify(['correct', recordId, body]),
                (headers) => postDayCareAttendanceRecordsByIdCorrect({ path: { id: Number(recordId) }, body, headers }),
                'Đã gửi đính chính; kết quả áp dụng theo quyền và phê duyệt.',
              )
            }}
          >
            <CareField
              label="Mã bản ghi điểm danh cần đính chính"
              type="number"
              min="1"
              value={recordId}
              onChange={setRecordId}
              required
              disabled={busy}
            />
            <CareField
              label="Thời điểm đúng"
              type="datetime-local"
              value={correctAt}
              onChange={setCorrectAt}
              required
              disabled={busy}
            />
            <CareField
              label="Lý do đính chính"
              type="textarea"
              value={reason}
              onChange={setReason}
              maxLength={2000}
              required
              disabled={busy}
            />
            <div className="md:col-span-2">
              <Button type="submit" disabled={busy}>
                Gửi đính chính giờ
              </Button>
            </div>
          </form>
        </details>
      )}
    </div>
  )
}
