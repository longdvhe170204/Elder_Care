import { useEffect, useRef, useState } from 'react'
import {
  getTripsById,
  postTripsByIdAssignment,
  postTripsByIdByCommand,
  postActivitySessionsByIdRegistrations,
  postActivityRegistrationsByIdCancel,
  postActivitySessionsByIdByCommand,
} from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ActivityField } from './ActivityField'
import { TRIP_STATUS, toOffsetIso } from './sessionView'

const STATUSES = {
  REGISTERED: 'Đã đăng ký',
  ELIGIBLE: 'Được đi',
  NOT_GOING: 'Không đi',
  DEPARTED: 'Đã rời viện',
  RETURNED: 'Đã về',
  LEFT_GROUP: 'Rời đoàn',
  MISSING_ON_RETURN: 'Thiếu khi về',
}

// T018: khung chuyến được duyệt; chưa hiển thị dữ liệu lâm sàng khi chưa có hợp đồng nguồn.
export function TripDetail({ sessionId, residents, canCreate, canUpdate, onChanged, Title, Description }) {
  const [trip, setTrip] = useState(null)
  const [problem, setProblem] = useState(null)
  const [leader, setLeader] = useState('')
  const [companions, setCompanions] = useState('')
  const [selected, setSelected] = useState([])
  const [reason, setReason] = useState('')
  const [confirm, setConfirm] = useState(false)
  const [busy, setBusy] = useState(false)
  const [revision, setRevision] = useState(0)
  const [residentId, setResidentId] = useState('')
  const [warnings, setWarnings] = useState([])
  const [cancelling, setCancelling] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [rescheduling, setRescheduling] = useState(false)
  const [cancellingTrip, setCancellingTrip] = useState(false)
  const [tripCancelReason, setTripCancelReason] = useState('')
  const [move, setMove] = useState({ startsAt: '', endsAt: '', reason: '' })
  const keys = useRef(new Map())
  useEffect(() => {
    let active = true
    getTripsById({ path: { id: sessionId } })
      .then(({ data }) => {
        if (!active) return
        setTrip(data)
        setLeader(data.leaderId == null ? '' : String(data.leaderId))
        setCompanions(data.companionIds.join(', '))
      })
      .catch((error) => active && setProblem(problemOf(error)))
    return () => {
      active = false
    }
  }, [sessionId, revision])
  async function command(run, message, bodyKey, onProblem) {
    setBusy(true)
    setProblem(null)
    const signature = JSON.stringify({
      message,
      bodyKey,
      leader,
      companions,
      selected: [...selected].sort((a, b) => a - b),
      reason,
    })
    if (!keys.current.has(signature)) keys.current.set(signature, crypto.randomUUID())
    try {
      await run({ 'Idempotency-Key': keys.current.get(signature) })
      keys.current.delete(signature)
      setConfirm(false)
      setSelected([])
      setRevision((v) => v + 1)
      onChanged(message)
      return true
    } catch (error) {
      const failure = problemOf(error)
      if (!onProblem?.(failure)) setProblem(failure)
      setConfirm(false)
      return false
    } finally {
      setBusy(false)
    }
  }
  async function register(acknowledgeWarnings) {
    const body = { residentId: Number(residentId), ...(acknowledgeWarnings ? { acknowledgeWarnings: true } : {}) }
    const saved = await command(
      (headers) => postActivitySessionsByIdRegistrations({ path: { id: sessionId }, body, headers }),
      'Đã đăng ký người cao tuổi',
      JSON.stringify(body),
      (failure) => {
        if (failure.errorCode !== 'WARNING_ACKNOWLEDGEMENT_REQUIRED') return false
        setWarnings(failure.unmetConditions ?? [])
        return true
      },
    )
    if (saved) {
      setResidentId('')
      setWarnings([])
    }
  }
  const nameOf = (id) => residents.find((r) => r.id === id)?.fullName ?? `Người cao tuổi ${id}`
  return (
    <div className="flex flex-col gap-5">
      <div>
        <Title className="type-title">Chuyến đi ngoài viện</Title>
        <Description className="type-body-sm">Phân công và điểm danh rời viện</Description>
      </div>
      <ProblemAlert problem={problem} />
      {!trip ? (
        <Skeleton className="h-40" />
      ) : (
        <>
          <section className="flex flex-col gap-2">
            <StatusBadge status={TRIP_STATUS[trip.status] ?? trip.status} />
            <p className="type-body">Điểm đến: {trip.destination}</p>
            <p className="type-body-sm tabular-nums">Rời dự kiến: {formatDateTime(trip.plannedDepartureAt)}</p>
            <p className="type-body-sm tabular-nums">Về dự kiến: {formatDateTime(trip.plannedReturnAt)}</p>
            <p className="type-body-sm">
              Phương tiện: {trip.transport}
              {trip.facilityVehicle ? ' · Xe của viện' : ''}
            </p>
            {canUpdate && trip.status === 'SCHEDULED' && (
              <Button variant="outline" size="lg" onClick={() => setRescheduling((value) => !value)}>
                Dời chuyến
              </Button>
            )}
            {rescheduling && (
              <form
                className="flex flex-col gap-3"
                onSubmit={async (e) => {
                  e.preventDefault()
                  const body = {
                    startsAt: toOffsetIso(move.startsAt),
                    endsAt: toOffsetIso(move.endsAt),
                    reason: move.reason,
                  }
                  if (
                    await command(
                      (headers) =>
                        postActivitySessionsByIdByCommand({
                          path: { id: trip.id, command: 'reschedule' },
                          body,
                          headers,
                        }),
                      'Đã dời chuyến',
                      JSON.stringify(body),
                    )
                  )
                    setRescheduling(false)
                }}
              >
                <ActivityField
                  label="Bắt đầu mới"
                  type="datetime-local"
                  value={move.startsAt}
                  onChange={(value) => setMove((form) => ({ ...form, startsAt: value }))}
                  required
                />
                <ActivityField
                  label="Kết thúc mới"
                  type="datetime-local"
                  value={move.endsAt}
                  onChange={(value) => setMove((form) => ({ ...form, endsAt: value }))}
                  required
                />
                <ActivityField
                  label="Lý do dời chuyến"
                  type="textarea"
                  value={move.reason}
                  onChange={(value) => setMove((form) => ({ ...form, reason: value }))}
                  required
                />
                <p className="type-body-sm">
                  Giờ rời và giờ về dự kiến được dời tương ứng với giờ bắt đầu và kết thúc mới. Đổi ngày sẽ cần đánh giá
                  lại người Được đi.
                </p>
                <Button type="submit" variant="outline" disabled={busy || !move.reason.trim()}>
                  Xác nhận dời chuyến
                </Button>
              </form>
            )}
          </section>
          <section className="flex flex-col gap-3 border-t pt-4">
            <h3 className="type-card-title">Phân công</h3>
            {canUpdate && trip.status === 'SCHEDULED' ? (
              <form
                className="flex flex-col gap-3"
                onSubmit={(e) => {
                  e.preventDefault()
                  const ids = companions.trim() ? companions.split(',').map((value) => Number(value.trim())) : []
                  if (!ids.every((value) => Number.isSafeInteger(value) && value > 0)) {
                    setProblem({ detail: 'Nhập mã người đi cùng bằng số nguyên dương, cách nhau bằng dấu phẩy.' })
                    return
                  }
                  command(
                    (headers) =>
                      postTripsByIdAssignment({
                        path: { id: trip.id },
                        body: { leaderId: Number(leader), companionIds: ids },
                        headers,
                      }),
                    'Đã lưu phân công chuyến',
                  )
                }}
              >
                <ActivityField label="Mã trưởng đoàn" type="number" value={leader} onChange={setLeader} required />
                <ActivityField
                  label="Mã người đi cùng (cách nhau bằng dấu phẩy)"
                  value={companions}
                  onChange={setCompanions}
                />
                <Button type="submit" variant="outline" size="lg" disabled={busy}>
                  Lưu phân công
                </Button>
              </form>
            ) : (
              <p className="type-body-sm">
                Trưởng đoàn: {trip.leaderId ?? 'Chưa phân công'} · Người đi cùng:{' '}
                {trip.companionIds.join(', ') || 'Chưa phân công'}
              </p>
            )}
          </section>
          {canUpdate && trip.status === 'SCHEDULED' && (
            <Button variant="destructive" size="lg" disabled={busy} onClick={() => setCancellingTrip(true)}>
              Hủy chuyến
            </Button>
          )}
          <Dialog open={cancellingTrip} onOpenChange={setCancellingTrip}>
            <DialogContent>
              <DialogTitle>Hủy chuyến đi</DialogTitle>
              <DialogDescription>
                Hủy chuyến đến {trip.destination} và các đăng ký còn hiệu lực. Việc hủy được ghi vào nhật ký.
              </DialogDescription>
              <form
                className="flex flex-col gap-3"
                onSubmit={async (e) => {
                  e.preventDefault()
                  const body = { reason: tripCancelReason }
                  if (
                    await command(
                      (headers) =>
                        postActivitySessionsByIdByCommand({ path: { id: trip.id, command: 'cancel' }, body, headers }),
                      'Đã hủy chuyến',
                      JSON.stringify(body),
                    )
                  )
                    setCancellingTrip(false)
                }}
              >
                <ActivityField
                  label="Lý do hủy chuyến"
                  type="textarea"
                  value={tripCancelReason}
                  onChange={setTripCancelReason}
                  required
                  maxLength={2000}
                />
                <ProblemAlert problem={problem} />
                <div className="flex flex-wrap justify-end gap-2">
                  <Button type="button" variant="outline" disabled={busy} onClick={() => setCancellingTrip(false)}>
                    Quay lại
                  </Button>
                  <Button type="submit" variant="destructive" disabled={busy || !tripCancelReason.trim()}>
                    Xác nhận hủy chuyến
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
          <section className="flex flex-col gap-3 border-t pt-4">
            <h3 className="type-card-title">Người tham gia</h3>
            <p className="type-body-sm text-muted-foreground">
              Chưa có dữ liệu thuốc, sức khỏe và liên hệ trên màn này. Việc đánh giá đầy đủ chờ nguồn chuyên môn.
            </p>
            {trip.participants.length === 0 && <p className="type-body-sm">Chưa có người tham gia.</p>}
            {trip.participants.map((p) => (
              <div key={p.id} className="rounded-lg border p-3">
                <label className="flex min-h-11 items-center gap-3">
                  {canCreate && p.status === 'ELIGIBLE' && ['SCHEDULED', 'IN_PROGRESS'].includes(trip.status) && (
                    <input
                      type="checkbox"
                      disabled={busy}
                      aria-label={`Chọn ${nameOf(p.residentId)}`}
                      checked={selected.includes(p.residentId)}
                      onChange={(e) =>
                        setSelected((ids) =>
                          e.target.checked ? [...ids, p.residentId] : ids.filter((id) => id !== p.residentId),
                        )
                      }
                    />
                  )}
                  <span className="type-label">{nameOf(p.residentId)}</span>
                </label>
                <StatusBadge status={STATUSES[p.status] ?? p.status} />
                {p.reason && <p className="mt-2 type-body-sm">Lý do: {p.reason}</p>}
                {canUpdate && trip.status === 'SCHEDULED' && ['REGISTERED', 'ELIGIBLE'].includes(p.status) && (
                  <Button
                    variant="ghost"
                    size="lg"
                    aria-label={`Hủy đăng ký của ${nameOf(p.residentId)}`}
                    disabled={busy}
                    onClick={() => {
                      setCancelling(p.id)
                      setCancelReason('')
                    }}
                  >
                    Hủy đăng ký
                  </Button>
                )}
              </div>
            ))}
            {cancelling != null && (
              <form
                className="flex flex-col gap-3"
                onSubmit={async (e) => {
                  e.preventDefault()
                  const body = { reason: cancelReason }
                  if (
                    await command(
                      (headers) => postActivityRegistrationsByIdCancel({ path: { id: cancelling }, body, headers }),
                      'Đã hủy đăng ký',
                      `${cancelling}:${JSON.stringify(body)}`,
                    )
                  )
                    setCancelling(null)
                }}
              >
                <ActivityField
                  label="Lý do hủy đăng ký"
                  type="textarea"
                  value={cancelReason}
                  onChange={setCancelReason}
                  required
                  maxLength={2000}
                />
                <div className="flex flex-wrap gap-2">
                  <Button type="submit" variant="destructive" disabled={busy || !cancelReason.trim()}>
                    Xác nhận hủy đăng ký
                  </Button>
                  <Button type="button" variant="outline" onClick={() => setCancelling(null)}>
                    Thôi
                  </Button>
                </div>
              </form>
            )}
            {canCreate && trip.status === 'SCHEDULED' && (
              <div className="flex flex-col gap-3">
                <ActivityField
                  label="Người cao tuổi"
                  options={residents
                    .filter((r) => !trip.participants.some((p) => p.residentId === r.id && p.status !== 'NOT_GOING'))
                    .map((r) => [String(r.id), r.fullName])}
                  value={residentId}
                  onChange={(value) => {
                    setResidentId(value)
                    setWarnings([])
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
                <Button
                  size="lg"
                  variant="outline"
                  disabled={busy || !residentId}
                  onClick={() => register(warnings.length > 0)}
                >
                  {warnings.length > 0 ? 'Xác nhận cảnh báo và đăng ký' : 'Đăng ký'}
                </Button>
              </div>
            )}
          </section>
          {canCreate && ['SCHEDULED', 'IN_PROGRESS'].includes(trip.status) && (
            <div className="flex flex-col gap-3 border-t pt-4">
              <ActivityField
                label="Lý do điểm danh thay (Trưởng tầng)"
                type="textarea"
                value={reason}
                onChange={setReason}
              />
              <p className="type-body-sm text-muted-foreground">Cần kết nối mạng để điểm danh rời viện.</p>
              <Button size="lg" disabled={busy || selected.length === 0} onClick={() => setConfirm(true)}>
                Điểm danh rời viện
              </Button>
            </div>
          )}
          <Dialog open={confirm} onOpenChange={setConfirm}>
            <DialogContent>
              <DialogTitle>Xác nhận rời viện</DialogTitle>
              <DialogDescription>
                Ghi nhận {selected.length} người rời viện: {selected.map(nameOf).join(', ')}. Hệ thống sẽ kiểm tra điều
                kiện trước khi ghi nhận.
              </DialogDescription>
              <div className="flex flex-wrap justify-end gap-2">
                <Button variant="outline" onClick={() => setConfirm(false)} disabled={busy}>
                  Quay lại
                </Button>
                <Button
                  disabled={busy}
                  onClick={() =>
                    command(
                      (headers) =>
                        postTripsByIdByCommand({
                          path: { id: trip.id, command: 'depart' },
                          body: { residentIds: selected, ...(reason.trim() ? { reason: reason.trim() } : {}) },
                          headers,
                        }),
                      'Đã điểm danh rời viện',
                    )
                  }
                >
                  Xác nhận rời viện
                </Button>
              </div>
            </DialogContent>
          </Dialog>
        </>
      )}
    </div>
  )
}
