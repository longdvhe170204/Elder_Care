import { useEffect, useState } from 'react'
import { getResidents, postVehicleSchedules, postVehicleSchedulesByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { Choices, Info, ReasonField, SELECT, TextField } from './FormParts'
import { wholeNumber } from './assetLabels'
import { BOOKER_ROLES, PURPOSES, SCHEDULE_STATUS_LABELS, purposeLabel, toLocalInput, vietnamIso } from './vehicleLabels'

// Khung chi tiết và khung đặt lịch xe (019 T022; FR-017, FR-018, BR-M03-16). api-019 chưa có allowedCommands và người
// dùng hiện tại không có mã nhân viên ở /me, nên nút lệnh theo trạng thái và vai trò (Trưởng tầng, Hành chính, Quản lý
// viện); backend chỉ cho người đặt hoặc Quản lý viện. Lịch Đang dùng chồng giờ, xe không dùng được trả 409.

export function ScheduleDetail({
  schedule,
  vehicleName,
  driverName,
  residentName,
  role,
  onChanged,
  Title,
  Description,
}) {
  const { can } = usePermissions()
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [departure, setDeparture] = useState(() => toLocalInput(schedule.plannedDepartureAt))
  const [returnAt, setReturnAt] = useState(() => toLocalInput(schedule.plannedReturnAt))
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  const status = schedule.status
  const actor = BOOKER_ROLES.includes(role) && can('ASSET_VEHICLE', 'UPDATE')

  function open(next) {
    setMode(next)
    setSubmitted(false)
    setFailure(null)
  }

  async function run(command, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await postVehicleSchedulesByIdByCommand({
        path: { id: schedule.id, command },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body,
      })
      setMode(null)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  const timeErrors = {
    departure: !departure && 'Chọn giờ đi mới',
    returnAt: (!returnAt && 'Chọn giờ về mới') || (departure && returnAt <= departure && 'Giờ về phải sau giờ đi'),
  }
  const show = (errors, name) => (submitted && errors[name]) || null

  function reschedule() {
    setSubmitted(true)
    if (timeErrors.departure || timeErrors.returnAt) return
    run(
      'reschedule',
      { plannedDepartureAt: vietnamIso(departure), plannedReturnAt: vietnamIso(returnAt) },
      'Đã dời giờ lịch xe',
    )
  }

  function cancel() {
    setSubmitted(true)
    if (!reason.trim()) return
    run('cancel', { reason: reason.trim() }, 'Đã hủy lịch xe')
  }

  const back = (
    <Button variant="ghost" className="h-11" onClick={() => open(null)}>
      Quay lại
    </Button>
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={SCHEDULE_STATUS_LABELS[status] ?? status} />
        <Title className="type-title">{vehicleName}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {`${purposeLabel(schedule.purpose)}${schedule.tripId ? `, chuyến đi #${schedule.tripId}` : ''}`}
        </Description>
      </div>
      <div className="grid gap-3">
        <Info label="Dự kiến">
          {`${formatDateTime(schedule.plannedDepartureAt)} → ${formatDateTime(schedule.plannedReturnAt)}`}
        </Info>
        {schedule.departedAt && (
          <Info label="Thực tế">
            {`Xuất phát ${formatDateTime(schedule.departedAt)}${schedule.returnedAt ? `, trả xe ${formatDateTime(schedule.returnedAt)}` : ''}`}
          </Info>
        )}
        <Info label="Người lái">{driverName}</Info>
        {schedule.residentId != null && <Info label="Người cao tuổi">{residentName}</Info>}
        {schedule.cancellationReason && <Info label="Lý do hủy">{schedule.cancellationReason}</Info>}
      </div>
      <ProblemAlert problem={failure} />

      {mode === 'reschedule' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <TextField
              id="schedule-departure"
              label="Giờ đi mới"
              type="datetime-local"
              value={departure}
              onChange={setDeparture}
              error={show(timeErrors, 'departure')}
            />
            <TextField
              id="schedule-return"
              label="Giờ về mới"
              type="datetime-local"
              value={returnAt}
              onChange={setReturnAt}
              error={show(timeErrors, 'returnAt')}
            />
          </div>
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={reschedule}>
              Lưu giờ mới
            </Button>
          </div>
        </div>
      )}

      {mode === 'cancel' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <ReasonField
            id="schedule-reason"
            value={reason}
            onChange={setReason}
            error={submitted && !reason.trim() ? 'Nhập lý do' : null}
          />
          <p className="type-body-sm text-muted-foreground">
            Người đặt, Phụ trách bộ phận quản lý xe và người lái (nếu có tài khoản) được báo.
          </p>
          <div className="flex justify-end gap-2">
            {back}
            <Button variant="destructive" className="h-11" disabled={busy} onClick={cancel}>
              Xác nhận hủy
            </Button>
          </div>
        </div>
      )}

      {mode === null && actor && (
        <div className="flex flex-wrap justify-end gap-2">
          {status === 'BOOKED' && (
            <>
              <Button variant="ghost" className="h-11" onClick={() => open('cancel')}>
                Hủy lịch…
              </Button>
              <Button variant="outline" className="h-11" onClick={() => open('reschedule')}>
                Dời giờ…
              </Button>
              <Button className="h-11" disabled={busy} onClick={() => run('record-departure', {}, 'Đã ghi xuất phát')}>
                Ghi xuất phát
              </Button>
            </>
          )}
          {status === 'IN_USE' && (
            <Button className="h-11" disabled={busy} onClick={() => run('record-return', {}, 'Đã ghi trả xe')}>
              Ghi trả xe
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

/** Đặt lịch xe (FR-017): xe, giờ đi, giờ về, mục đích, tham chiếu (chuyến đi hoặc người cao tuổi), người lái. */
export function BookScheduleSheet({ vehicles, staff, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [vehicleId, setVehicleId] = useState('')
  const [departure, setDeparture] = useState('')
  const [returnAt, setReturnAt] = useState('')
  const [purpose, setPurpose] = useState('')
  const [tripId, setTripId] = useState('')
  const [residentId, setResidentId] = useState('')
  const [driverStaffId, setDriverStaffId] = useState('')
  const [note, setNote] = useState('')
  const [residents, setResidents] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const trip = purpose === 'TRIP'

  useEffect(() => {
    if (!purpose || trip || residents !== null) return undefined
    let cancelled = false
    getResidents({ query: { status: ['IN_RESIDENCE'] } })
      .then(({ data }) => !cancelled && setResidents(data.items))
      .catch(() => !cancelled && setResidents([]))
    return () => {
      cancelled = true
    }
  }, [purpose, trip, residents])

  const errors = {
    vehicle: !vehicleId && 'Chọn xe',
    departure: !departure && 'Chọn giờ đi dự kiến',
    returnAt: (!returnAt && 'Chọn giờ về dự kiến') || (departure && returnAt <= departure && 'Giờ về phải sau giờ đi'),
    purpose: !purpose && 'Chọn mục đích',
    trip: trip && !wholeNumber(tripId) && 'Nhập mã chuyến đi',
    driver: !driverStaffId && 'Chọn người lái',
  }
  const show = (name) => (submitted && errors[name]) || null

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    const body = {
      vehicleId: Number(vehicleId),
      plannedDepartureAt: vietnamIso(departure),
      plannedReturnAt: vietnamIso(returnAt),
      purpose,
    }
    if (trip) body.tripId = wholeNumber(tripId)
    else if (residentId) body.residentId = Number(residentId)
    body.driverStaffId = Number(driverStaffId)
    if (note.trim()) body.note = note.trim()
    setBusy(true)
    setFailure(null)
    try {
      await postVehicleSchedules({ headers: { 'Idempotency-Key': key }, body })
      onDone('Đã đặt lịch xe')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Đặt lịch xe</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Lịch bị chặn khi chồng giờ với lịch chưa hủy của cùng xe, hoặc xe đang Hỏng, Đang bảo trì, quá hạn đăng kiểm,
          bảo hiểm vào ngày đi.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <FieldGroup>
            <Field data-invalid={!!show('vehicle') || undefined}>
              <FieldLabel htmlFor="schedule-vehicle">Xe</FieldLabel>
              <select
                id="schedule-vehicle"
                value={vehicleId}
                onChange={(e) => setVehicleId(e.target.value)}
                aria-invalid={!!show('vehicle') || undefined}
                className={SELECT}
              >
                <option value="">{vehicles === null ? 'Đang tải…' : 'Chọn một xe'}</option>
                {(vehicles ?? [])
                  .filter((v) => v.status !== 'DISPOSED')
                  .map((v) => (
                    <option key={v.id} value={v.id}>
                      {`${v.code}, ${v.name}`}
                    </option>
                  ))}
              </select>
              {show('vehicle') && <FieldError>{errors.vehicle}</FieldError>}
            </Field>
            <div className="grid gap-3 sm:grid-cols-2">
              <TextField
                id="schedule-new-departure"
                label="Giờ đi dự kiến"
                type="datetime-local"
                value={departure}
                onChange={setDeparture}
                error={show('departure')}
              />
              <TextField
                id="schedule-new-return"
                label="Giờ về dự kiến"
                type="datetime-local"
                value={returnAt}
                onChange={setReturnAt}
                error={show('returnAt')}
              />
            </div>
            <Choices
              label="Mục đích"
              options={PURPOSES}
              value={purpose}
              onChange={setPurpose}
              error={show('purpose')}
            />
            {trip ? (
              <TextField
                id="schedule-trip"
                label="Mã chuyến đi"
                inputMode="numeric"
                value={tripId}
                onChange={setTripId}
                error={show('trip')}
              />
            ) : (
              purpose && (
                <Field>
                  <FieldLabel htmlFor="schedule-resident">Người cao tuổi (không bắt buộc)</FieldLabel>
                  <select
                    id="schedule-resident"
                    value={residentId}
                    onChange={(e) => setResidentId(e.target.value)}
                    className={SELECT}
                  >
                    <option value="">{residents === null ? 'Đang tải…' : 'Không gắn người cao tuổi'}</option>
                    {(residents ?? []).map((r) => (
                      <option key={r.id} value={r.id}>
                        {`${r.personalInfo.fullName}, ${r.profileCode}`}
                      </option>
                    ))}
                  </select>
                </Field>
              )
            )}
            <Field data-invalid={!!show('driver') || undefined}>
              <FieldLabel htmlFor="schedule-driver">Người lái</FieldLabel>
              <select
                id="schedule-driver"
                value={driverStaffId}
                onChange={(e) => setDriverStaffId(e.target.value)}
                aria-invalid={!!show('driver') || undefined}
                className={SELECT}
              >
                <option value="">{staff === null ? 'Đang tải…' : 'Chọn một nhân viên'}</option>
                {(staff ?? []).map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.fullName}
                  </option>
                ))}
              </select>
              {show('driver') && <FieldError>{errors.driver}</FieldError>}
            </Field>
            <TextField id="schedule-note" label="Ghi chú (không bắt buộc)" value={note} onChange={setNote} />
          </FieldGroup>
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" className="h-11" onClick={onClose}>
              Quay lại
            </Button>
            <Button type="submit" className="h-11" disabled={busy}>
              Lưu lịch xe
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
