import { useEffect, useState } from 'react'
import {
  getResidents,
  getResidentsByResidentIdRelationships,
  postFamilyStays,
  postFamilyStaysByIdByCommand,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ROOMMATE_RESULTS, ROOM_OF_RESIDENT, STAY_STATUS_LABELS, locationLabel, vietnamIso } from './stayLabels'

// Khung chi tiết và khung đăng ký của lượt ở lại (012 T035; FR-040 → FR-045). Lệnh theo trạng thái (FR-045) và vai trò
// (Phụ lục 27 chú thích 20): Trưởng tầng, Quản lý viện xác nhận, từ chối; Hành chính, Trưởng tầng bắt đầu, kết thúc;
// Hành chính hủy, gia hạn.

const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'
const CONFIRMERS = ['FLOOR_MANAGER', 'NURSING_HOME_MANAGER']
const STARTERS = ['OFFICE_STAFF', 'FLOOR_MANAGER']

function Choices({ label, options, value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel>{label}</FieldLabel>
      <ToggleGroup
        variant="outline"
        value={value ? [value] : []}
        onValueChange={(next) => next[0] && onChange(next[0])}
        aria-label={label}
        className="flex-wrap"
      >
        {options.map(([code, text]) => (
          <ToggleGroupItem key={code} value={code} className="h-11 px-4">
            {text}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

function Info({ label, children }) {
  return (
    <div className="flex flex-col">
      <span className="type-caption text-muted-foreground">{label}</span>
      <span className="type-body-sm tabular-nums">{children}</span>
    </div>
  )
}

function ReasonField({ id, value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>Lý do</FieldLabel>
      <Textarea
        id={id}
        rows={2}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={!!error || undefined}
      />
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

export function StayDetail({ stay, role, memberName, residentName, onChanged, Title, Description }) {
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [newEnd, setNewEnd] = useState('')
  const [askedPerson, setAskedPerson] = useState('')
  const [result, setResult] = useState('')
  const [note, setNote] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  const status = stay.status
  const confirmer = CONFIRMERS.includes(role)
  const office = role === 'OFFICE_STAFF'

  function open(next) {
    setMode(next)
    setSubmitted(false)
    setFailure(null)
  }

  async function run(command, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await postFamilyStaysByIdByCommand({
        path: { id: stay.id, command },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body,
      })
      setMode(null)
      setReason('')
      setSubmitted(false)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  function confirm() {
    // Ý kiến người cùng phòng chỉ gửi khi đã ghi người được hỏi; phòng từ hai giường đang dùng thì backend bắt buộc
    const body = {}
    if (askedPerson.trim()) {
      body.roommateOpinion = { askedPerson: askedPerson.trim(), result }
      if (note.trim()) body.roommateOpinion.note = note.trim()
    }
    run('confirm', body, 'Đã xác nhận lượt ở lại')
  }

  function withReason(command, body, message) {
    setSubmitted(true)
    if (!reason.trim()) return
    run(command, { ...body, reason: reason.trim() }, message)
  }

  function extend() {
    setSubmitted(true)
    if (!newEnd || !reason.trim()) return
    run('extend', { newExpectedEndAt: vietnamIso(newEnd), reason: reason.trim() }, 'Đã gia hạn lượt ở lại')
  }

  const reasonError = submitted && !reason.trim() ? 'Nhập lý do' : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={STAY_STATUS_LABELS[status] ?? status} />
        <Title className="type-title">{`${memberName} ở lại với ${residentName}`}</Title>
        <Description className="type-body-sm text-muted-foreground">{locationLabel(stay.location)}</Description>
      </div>
      <div className="grid gap-3">
        <Info label="Thời gian dự kiến">
          {formatDateTime(stay.expectedStartAt)} → {formatDateTime(stay.expectedEndAt)}
        </Info>
        {stay.actualStartAt && (
          <Info label="Thực tế">
            Từ {formatDateTime(stay.actualStartAt)}
            {stay.actualEndAt ? ` tới ${formatDateTime(stay.actualEndAt)}` : ''}, {stay.nightCount} đêm
          </Info>
        )}
        <Info label="Lý do">{stay.reason}</Info>
        <Info label="Đăng ký ăn">{stay.mealRegistered ? 'Có' : 'Không'}</Info>
      </div>
      <ProblemAlert problem={failure} />

      {status === 'PENDING_CONFIRMATION' && confirmer && mode === null && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <h3 className="type-card-title">Ý kiến người cùng phòng</h3>
          <p className="type-body-sm text-muted-foreground">
            Bắt buộc khi phòng có từ hai giường đang dùng: ghi người cùng phòng (hoặc người đại diện của họ) đã được hỏi
            và kết quả.
          </p>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="roommate-person">Người được hỏi</FieldLabel>
              <Input
                id="roommate-person"
                className="h-11"
                value={askedPerson}
                onChange={(e) => setAskedPerson(e.target.value)}
              />
            </Field>
            <Choices label="Kết quả" options={ROOMMATE_RESULTS} value={result} onChange={setResult} />
            <Field>
              <FieldLabel htmlFor="roommate-note">Ghi chú</FieldLabel>
              <Input id="roommate-note" className="h-11" value={note} onChange={(e) => setNote(e.target.value)} />
            </Field>
          </FieldGroup>
          <div className="flex flex-wrap justify-end gap-2">
            <Button variant="outline" className="h-11" onClick={() => open('reject')}>
              Từ chối…
            </Button>
            <Button className="h-11" disabled={busy} onClick={confirm}>
              Xác nhận lượt ở lại
            </Button>
          </div>
        </div>
      )}

      {mode === 'reject' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <ReasonField id="stay-reason" value={reason} onChange={setReason} error={reasonError} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => open(null)}>
              Quay lại
            </Button>
            <Button className="h-11" disabled={busy} onClick={() => withReason('reject', {}, 'Đã từ chối lượt ở lại')}>
              Xác nhận từ chối
            </Button>
          </div>
        </div>
      )}

      {mode === 'cancel' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <ReasonField id="stay-reason" value={reason} onChange={setReason} error={reasonError} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => open(null)}>
              Quay lại
            </Button>
            <Button
              variant="destructive"
              className="h-11"
              disabled={busy}
              onClick={() => withReason('cancel', {}, 'Đã hủy lượt ở lại')}
            >
              Xác nhận hủy
            </Button>
          </div>
        </div>
      )}

      {mode === 'extend' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <Field data-invalid={(submitted && !newEnd) || undefined}>
            <FieldLabel htmlFor="stay-new-end">Kết thúc dự kiến mới</FieldLabel>
            <Input
              id="stay-new-end"
              type="datetime-local"
              className="h-11"
              value={newEnd}
              onChange={(e) => setNewEnd(e.target.value)}
            />
            {submitted && !newEnd && <FieldError>Chọn thời gian kết thúc dự kiến mới</FieldError>}
          </Field>
          <ReasonField id="stay-reason" value={reason} onChange={setReason} error={reasonError} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => open(null)}>
              Quay lại
            </Button>
            <Button className="h-11" disabled={busy} onClick={extend}>
              Lưu gia hạn
            </Button>
          </div>
        </div>
      )}

      {mode === null && (
        <div className="flex flex-wrap justify-end gap-2">
          {office && ['PENDING_CONFIRMATION', 'CONFIRMED'].includes(status) && (
            <Button variant="ghost" className="h-11" onClick={() => open('cancel')}>
              Hủy lượt…
            </Button>
          )}
          {office && status === 'STAYING' && (
            <Button variant="outline" className="h-11" onClick={() => open('extend')}>
              Gia hạn…
            </Button>
          )}
          {STARTERS.includes(role) && status === 'CONFIRMED' && (
            <Button className="h-11" disabled={busy} onClick={() => run('start', {}, 'Đã ghi bắt đầu ở lại')}>
              Bắt đầu ở lại
            </Button>
          )}
          {STARTERS.includes(role) && status === 'STAYING' && (
            <Button className="h-11" disabled={busy} onClick={() => run('end', {}, 'Đã ghi kết thúc ở lại')}>
              Kết thúc ở lại
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

/** Đăng ký ở lại (FR-040): Hành chính; người thân có quan hệ Hiệu lực với người cao tuổi Đang lưu trú. */
export function RegisterStaySheet({ onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [residents, setResidents] = useState(null)
  const [residentId, setResidentId] = useState('')
  const [relationships, setRelationships] = useState([])
  const [familyMemberId, setFamilyMemberId] = useState('')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [reason, setReason] = useState('')
  const [meal, setMeal] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    let cancelled = false
    getResidents({ query: { status: ['IN_RESIDENCE'] } })
      .then(({ data }) => !cancelled && setResidents(data.items))
      .catch((e) => {
        if (!cancelled) {
          setResidents([])
          setFailure(problemOf(e))
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  function pickResident(id) {
    setResidentId(id)
    setFamilyMemberId('')
    setRelationships([])
    if (!id) return
    getResidentsByResidentIdRelationships({ path: { residentId: Number(id) } })
      .then(({ data }) => setRelationships(data.filter((r) => r.status === 'ACTIVE')))
      .catch((e) => setFailure(problemOf(e)))
  }

  const errors = {
    resident: !residentId && 'Chọn người cao tuổi',
    member: !familyMemberId && 'Chọn người thân ở lại',
    start: !start && 'Chọn thời gian bắt đầu dự kiến',
    end: (!end && 'Chọn thời gian kết thúc dự kiến') || (start && end <= start && 'Kết thúc phải sau bắt đầu'),
    reason: !reason.trim() && 'Nhập lý do ở lại',
    meal: !meal && 'Chọn có đăng ký ăn hay không',
  }
  const show = (name) => (submitted && errors[name]) || null

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      await postFamilyStays({
        headers: { 'Idempotency-Key': key },
        body: {
          familyMemberId: Number(familyMemberId),
          residentId: Number(residentId),
          expectedStartAt: vietnamIso(start),
          expectedEndAt: vietnamIso(end),
          location: ROOM_OF_RESIDENT,
          reason: reason.trim(),
          mealRegistered: meal === 'YES',
        },
      })
      onDone('Đã đăng ký lượt ở lại, chờ Trưởng tầng xác nhận')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Đăng ký người thân ở lại</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Lượt mới chờ Trưởng tầng được giao của tầng xác nhận; chỉ lượt đã xác nhận mới bắt đầu được.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <FieldGroup>
            <Field data-invalid={!!show('resident') || undefined}>
              <FieldLabel htmlFor="stay-resident">Người cao tuổi</FieldLabel>
              <select
                id="stay-resident"
                value={residentId}
                onChange={(e) => pickResident(e.target.value)}
                aria-invalid={!!show('resident') || undefined}
                className={SELECT}
              >
                <option value="">{residents === null ? 'Đang tải…' : 'Chọn người'}</option>
                {(residents ?? []).map((r) => (
                  <option key={r.id} value={r.id}>
                    {`${r.personalInfo.fullName}, ${r.profileCode}`}
                  </option>
                ))}
              </select>
              {show('resident') && <FieldError>{errors.resident}</FieldError>}
            </Field>
            <Field data-invalid={!!show('member') || undefined}>
              <FieldLabel htmlFor="stay-member">Người thân ở lại</FieldLabel>
              <select
                id="stay-member"
                value={familyMemberId}
                onChange={(e) => setFamilyMemberId(e.target.value)}
                aria-invalid={!!show('member') || undefined}
                className={SELECT}
              >
                <option value="">Chọn người thân có quan hệ Hiệu lực</option>
                {relationships.map((r) => (
                  <option key={r.familyMember.id} value={r.familyMember.id}>
                    {r.familyMember.fullName}
                  </option>
                ))}
              </select>
              {show('member') && <FieldError>{errors.member}</FieldError>}
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field data-invalid={!!show('start') || undefined}>
                <FieldLabel htmlFor="stay-start">Bắt đầu dự kiến</FieldLabel>
                <Input
                  id="stay-start"
                  type="datetime-local"
                  className="h-11"
                  value={start}
                  onChange={(e) => setStart(e.target.value)}
                />
                {show('start') && <FieldError>{errors.start}</FieldError>}
              </Field>
              <Field data-invalid={!!show('end') || undefined}>
                <FieldLabel htmlFor="stay-end">Kết thúc dự kiến</FieldLabel>
                <Input
                  id="stay-end"
                  type="datetime-local"
                  className="h-11"
                  value={end}
                  onChange={(e) => setEnd(e.target.value)}
                />
                {show('end') && <FieldError>{errors.end}</FieldError>}
              </Field>
            </div>
            <Info label="Vị trí">{locationLabel(ROOM_OF_RESIDENT)}</Info>
            <Field data-invalid={!!show('reason') || undefined}>
              <FieldLabel htmlFor="stay-register-reason">Lý do</FieldLabel>
              <Textarea
                id="stay-register-reason"
                rows={2}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                aria-invalid={!!show('reason') || undefined}
              />
              {show('reason') && <FieldError>{errors.reason}</FieldError>}
            </Field>
            <Choices
              label="Đăng ký ăn"
              options={[
                ['YES', 'Có'],
                ['NO', 'Không'],
              ]}
              value={meal}
              onChange={setMeal}
              error={show('meal')}
            />
          </FieldGroup>
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" className="h-11" onClick={onClose}>
              Quay lại
            </Button>
            <Button type="submit" className="h-11" disabled={busy}>
              Lưu đăng ký
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
