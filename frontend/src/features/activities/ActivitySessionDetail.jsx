import { useEffect, useState } from 'react'
import {
  getActivitySessionsById,
  postActivityRegistrationsByIdCancel,
  postActivitySessionsByIdByCommand,
  postActivitySessionsByIdRegistrations,
} from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ActivityField } from './ActivityField'
import { SessionAttendanceForm } from './SessionAttendanceForm'
import { WellbeingSheet } from './WellbeingSheet'
import { REGISTRATION_SOURCE, REGISTRATION_STATUS, SESSION_STATUS, toOffsetIso } from './sessionView'

const EMPTY = { reason: '', startsAt: '', endsAt: '', residentId: '', registrationReason: '' }

// 014 T009, T012, T014: khung chi tiết buổi (FR-012, FR-013, FR-015 → FR-019, FR-027 → FR-029; bố cục duyệt 2026-11-05). Họ tên người cao
// tuổi lấy từ danh sách GET /residents (001) như màn Điểm danh bán trú của 005; không có thì hiện mã. Nút lệnh theo quyền
// ACTIVITY_OUTING (CREATE đăng ký, UPDATE dời, hủy); backend còn kiểm vai trò và điều kiện.
export function ActivitySessionDetail({
  sessionId,
  activityName,
  residents = [],
  canCreate,
  canUpdate,
  onChanged,
  Title,
  Description,
}) {
  const [result, setResult] = useState(null)
  const [mode, setMode] = useState(null)
  const [cancelling, setCancelling] = useState(null)
  const [warnings, setWarnings] = useState([])
  const [form, setForm] = useState(EMPTY)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key, setKey] = useState(() => crypto.randomUUID())
  const [revision, setRevision] = useState(0)
  const [wellbeingResident, setWellbeingResident] = useState(null)
  const current = result?.key === `${sessionId}:${revision}` ? result : null
  const names = new Map(residents.map((r) => [r.id, r.fullName]))
  const nameOf = (id) => names.get(id) ?? `Mã ${id}`
  const set = (field) => (value) => setForm((f) => ({ ...f, [field]: value }))

  useEffect(() => {
    let active = true
    const requestKey = `${sessionId}:${revision}`
    getActivitySessionsById({ path: { id: sessionId } })
      .then(({ data }) => active && setResult({ key: requestKey, data, loadedAt: Date.now() }))
      .catch((error) => active && setResult({ key: requestKey, failure: problemOf(error) }))
    return () => {
      active = false
    }
  }, [sessionId, revision])

  // Mỗi lệnh một Idempotency-Key; giữ khóa khi lỗi để gửi lại an toàn, đổi khóa sau khi thành công.
  async function run(call, done, onProblem) {
    setBusy(true)
    setProblem(null)
    try {
      await call({ 'Idempotency-Key': key })
      setKey(crypto.randomUUID())
      setMode(null)
      setCancelling(null)
      setWarnings([])
      setForm(EMPTY)
      setRevision((v) => v + 1)
      onChanged(done)
    } catch (error) {
      const p = problemOf(error)
      if (!onProblem?.(p)) setProblem(p)
    } finally {
      setBusy(false)
    }
  }

  function register(acknowledgeWarnings) {
    const body = { residentId: Number(form.residentId) }
    if (acknowledgeWarnings) body.acknowledgeWarnings = true
    // Lần gửi lại sau cảnh báo có thân khác lần đầu nên dùng khóa riêng cho mỗi thân.
    const attempt = { 'Idempotency-Key': `${key}:${body.residentId}:${acknowledgeWarnings ? 'ack' : 'new'}` }
    run(
      () => postActivitySessionsByIdRegistrations({ path: { id: sessionId }, body, headers: attempt }),
      'Đã đăng ký người cao tuổi',
      (p) => {
        if (p.errorCode !== 'WARNING_ACKNOWLEDGEMENT_REQUIRED') return false
        setWarnings(p.unmetConditions)
        return true
      },
    )
  }

  if (!current) return <Skeleton className="h-40" />
  if (current.failure) return <ProblemAlert problem={current.failure} />
  const s = current.data
  const open = s.status === 'SCHEDULED'
  // Đăng ký trước giờ bắt đầu; từ giờ bắt đầu là điểm danh (FR-015 (e), FR-028).
  const started = Date.parse(s.startsAt) <= current.loadedAt
  const allRecorded = s.registrations.length > 0 && s.registrations.every((r) => r.status === 'ATTENDED')
  const registeredIds = new Set(s.registrations.map((r) => r.residentId))
  const candidates = residents.filter((r) => !registeredIds.has(r.id)).map((r) => [String(r.id), r.fullName])
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
                <TableHead>Người cao tuổi</TableHead>
                <TableHead>Nguồn</TableHead>
                <TableHead>Trạng thái</TableHead>
                {canUpdate && open && <TableHead>Thao tác</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {s.registrations.map((r) => (
                <TableRow key={r.id}>
                  <TableCell>
                    <span>{nameOf(r.residentId)}</span>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setWellbeingResident(r.residentId)}
                      aria-label={`Hồ sơ tinh thần của ${nameOf(r.residentId)}`}
                    >
                      Hồ sơ tinh thần
                    </Button>
                  </TableCell>
                  <TableCell>{REGISTRATION_SOURCE[r.source] ?? r.source}</TableCell>
                  <TableCell>
                    <StatusBadge status={REGISTRATION_STATUS[r.status] ?? r.status} />
                  </TableCell>
                  {canUpdate && open && (
                    <TableCell>
                      {r.status === 'REGISTERED' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={`Hủy đăng ký của ${nameOf(r.residentId)}`}
                          onClick={() => setCancelling(r.id)}
                        >
                          Hủy đăng ký
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </section>

      {cancelling !== null && (
        <form
          className="flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              (headers) =>
                postActivityRegistrationsByIdCancel({
                  path: { id: cancelling },
                  body: { reason: form.registrationReason },
                  headers,
                }),
              'Đã hủy đăng ký',
            )
          }}
        >
          <ActivityField
            label="Lý do hủy đăng ký"
            type="textarea"
            maxLength={2000}
            value={form.registrationReason}
            onChange={set('registrationReason')}
            required
          />
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="destructive" size="lg" disabled={busy || !form.registrationReason.trim()}>
              Xác nhận hủy đăng ký
            </Button>
            <Button type="button" variant="ghost" size="lg" onClick={() => setCancelling(null)}>
              Thôi
            </Button>
          </div>
        </form>
      )}

      {canCreate && open && !started && (
        <section className="flex flex-col gap-3 border-t pt-4" aria-labelledby="session-register">
          <h3 id="session-register" className="type-card-title">
            Đăng ký người cao tuổi
          </h3>
          <ActivityField
            label="Người cao tuổi"
            options={candidates}
            value={form.residentId}
            onChange={(v) => {
              setWarnings([])
              set('residentId')(v)
            }}
            required
          />
          {warnings.length > 0 && (
            <Alert variant="attention">
              <AlertTitle>Cần xác nhận cảnh báo</AlertTitle>
              <AlertDescription>
                <ul className="list-disc pl-5">
                  {warnings.map((w) => (
                    <li key={w.code}>{w.description}</li>
                  ))}
                </ul>
              </AlertDescription>
            </Alert>
          )}
          <div className="flex flex-wrap gap-2">
            {warnings.length > 0 ? (
              <Button size="lg" disabled={busy} onClick={() => register(true)}>
                Xác nhận cảnh báo và đăng ký
              </Button>
            ) : (
              <Button size="lg" disabled={busy || !form.residentId} onClick={() => register(false)}>
                Đăng ký
              </Button>
            )}
          </div>
        </section>
      )}

      {canCreate && open && started && (
        <SessionAttendanceForm
          key={revision}
          sessionId={sessionId}
          registrations={s.registrations}
          residents={residents}
          nameOf={nameOf}
          onDone={(message) => {
            setRevision((v) => v + 1)
            onChanged(message)
          }}
        />
      )}

      {canUpdate && open && started && allRecorded && (
        <Button
          size="lg"
          disabled={busy}
          onClick={() =>
            run(
              (headers) =>
                postActivitySessionsByIdByCommand({
                  path: { id: sessionId, command: 'complete-attendance' },
                  body: {},
                  headers,
                }),
              'Đã hoàn tất điểm danh',
            )
          }
        >
          Hoàn tất điểm danh
        </Button>
      )}

      <ProblemAlert problem={problem} />

      {canUpdate && open && mode === 'reschedule' && (
        <form
          className="flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              (headers) =>
                postActivitySessionsByIdByCommand({
                  path: { id: sessionId, command: 'reschedule' },
                  body: { reason: form.reason, startsAt: toOffsetIso(form.startsAt), endsAt: toOffsetIso(form.endsAt) },
                  headers,
                }),
              'Đã dời buổi',
            )
          }}
        >
          <ActivityField
            label="Bắt đầu mới"
            type="datetime-local"
            value={form.startsAt}
            onChange={set('startsAt')}
            required
          />
          <ActivityField
            label="Kết thúc mới"
            type="datetime-local"
            value={form.endsAt}
            onChange={set('endsAt')}
            required
          />
          <ActivityField
            label="Lý do dời"
            type="textarea"
            maxLength={2000}
            value={form.reason}
            onChange={set('reason')}
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
            run(
              (headers) =>
                postActivitySessionsByIdByCommand({
                  path: { id: sessionId, command: 'cancel' },
                  body: { reason: form.reason },
                  headers,
                }),
              'Đã hủy buổi',
            )
          }}
        >
          <p className="type-body-sm">Hủy buổi sẽ hủy mọi đăng ký của buổi. Buổi đã hủy không được tạo lại từ mẫu.</p>
          <ActivityField
            label="Lý do hủy buổi"
            type="textarea"
            maxLength={2000}
            value={form.reason}
            onChange={set('reason')}
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
      {wellbeingResident && (
        <WellbeingSheet
          residentId={wellbeingResident}
          residentName={nameOf(wellbeingResident)}
          onClose={() => setWellbeingResident(null)}
        />
      )}
    </div>
  )
}
