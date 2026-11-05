import { useEffect, useState } from 'react'
import { getActivitySessionsById, postActivitySessionsByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ActivityField } from './ActivityField'
import { REGISTRATION_SOURCE, REGISTRATION_STATUS, SESSION_STATUS, toOffsetIso } from './sessionView'

// 014 T009: khung chi tiết buổi (FR-012, FR-013, FR-019). Đăng ký chỉ có mã người cao tuổi vì api-014 không có họ tên
// (DESIGN.md mục 13 điều 6). Nút lệnh theo quyền ACTIVITY_OUTING UPDATE; backend còn kiểm vai trò Trưởng tầng.
export function ActivitySessionDetail({ sessionId, activityName, canUpdate, onChanged, Title, Description }) {
  const [result, setResult] = useState(null)
  const [mode, setMode] = useState(null)
  const [form, setForm] = useState({ reason: '', startsAt: '', endsAt: '' })
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key, setKey] = useState(() => crypto.randomUUID())
  const [revision, setRevision] = useState(0)
  const current = result?.key === `${sessionId}:${revision}` ? result : null

  useEffect(() => {
    let active = true
    const requestKey = `${sessionId}:${revision}`
    getActivitySessionsById({ path: { id: sessionId } })
      .then(({ data }) => active && setResult({ key: requestKey, data }))
      .catch((error) => active && setResult({ key: requestKey, failure: problemOf(error) }))
    return () => {
      active = false
    }
  }, [sessionId, revision])

  async function send(command, body, done) {
    setBusy(true)
    setProblem(null)
    try {
      await postActivitySessionsByIdByCommand({
        path: { id: sessionId, command },
        body,
        headers: { 'Idempotency-Key': key },
      })
      setKey(crypto.randomUUID())
      setMode(null)
      setForm({ reason: '', startsAt: '', endsAt: '' })
      setRevision((v) => v + 1)
      onChanged(done)
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (!current) return <Skeleton className="h-40" />
  if (current.failure) return <ProblemAlert problem={current.failure} />
  const s = current.data
  const open = s.status === 'SCHEDULED'
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={SESSION_STATUS[s.status] ?? s.status} />
        <Title className="type-title">{activityName}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {formatDateTime(s.startsAt)} đến {formatDateTime(s.endsAt)}
        </Description>
      </div>
      {s.incompleteAttendance && <p className="type-body-sm">Điểm danh không đủ</p>}
      {s.cancelReason && <p className="type-body-sm">Lý do hủy: {s.cancelReason}</p>}

      <section className="flex flex-col gap-2" aria-labelledby="session-registrations">
        <h3 id="session-registrations" className="type-card-title">
          Đăng ký ({s.registrations.length})
        </h3>
        {s.registrations.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa có đăng ký còn hiệu lực.</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Mã người cao tuổi</TableHead>
                <TableHead>Nguồn</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {s.registrations.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="tabular-nums">{r.residentId}</TableCell>
                  <TableCell>{REGISTRATION_SOURCE[r.source] ?? r.source}</TableCell>
                  <TableCell>
                    <StatusBadge status={REGISTRATION_STATUS[r.status] ?? r.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </section>

      <ProblemAlert problem={problem} />

      {canUpdate && open && mode === 'reschedule' && (
        <form
          className="flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            send(
              'reschedule',
              { reason: form.reason, startsAt: toOffsetIso(form.startsAt), endsAt: toOffsetIso(form.endsAt) },
              'Đã dời buổi',
            )
          }}
        >
          <ActivityField
            label="Bắt đầu mới"
            type="datetime-local"
            value={form.startsAt}
            onChange={(v) => setForm((f) => ({ ...f, startsAt: v }))}
            required
          />
          <ActivityField
            label="Kết thúc mới"
            type="datetime-local"
            value={form.endsAt}
            onChange={(v) => setForm((f) => ({ ...f, endsAt: v }))}
            required
          />
          <ActivityField
            label="Lý do dời"
            type="textarea"
            maxLength={2000}
            value={form.reason}
            onChange={(v) => setForm((f) => ({ ...f, reason: v }))}
            required
          />
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="lg" disabled={busy || !form.reason.trim() || !form.startsAt || !form.endsAt}>
              Lưu giờ mới
            </Button>
            <Button type="button" variant="ghost" size="lg" onClick={() => setMode(null)}>
              Thôi
            </Button>
          </div>
        </form>
      )}

      {canUpdate && open && mode === 'cancel' && (
        <form
          className="flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            send('cancel', { reason: form.reason }, 'Đã hủy buổi')
          }}
        >
          <p className="type-body-sm">Hủy buổi sẽ hủy mọi đăng ký của buổi. Buổi đã hủy không được tạo lại từ mẫu.</p>
          <ActivityField
            label="Lý do hủy"
            type="textarea"
            maxLength={2000}
            value={form.reason}
            onChange={(v) => setForm((f) => ({ ...f, reason: v }))}
            required
          />
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="destructive" size="lg" disabled={busy || !form.reason.trim()}>
              Xác nhận hủy buổi
            </Button>
            <Button type="button" variant="ghost" size="lg" onClick={() => setMode(null)}>
              Thôi
            </Button>
          </div>
        </form>
      )}

      {canUpdate && open && mode === null && (
        <div className="flex flex-wrap gap-2 border-t pt-4">
          <Button variant="outline" size="lg" onClick={() => setMode('reschedule')}>
            Dời buổi
          </Button>
          <Button variant="destructive" size="lg" onClick={() => setMode('cancel')}>
            Hủy buổi
          </Button>
        </div>
      )}
    </div>
  )
}
