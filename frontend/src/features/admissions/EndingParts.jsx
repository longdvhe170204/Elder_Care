import { useEffect, useState } from 'react'
import {
  getResidentsById,
  getResidentsByResidentIdRelationships,
  getResidencyEndingsByIdConditions,
  getStaff,
  postResidencyEndings,
  postResidencyEndingsByIdCancel,
  postResidencyEndingsByIdChangeDate,
  postResidencyEndingsByIdHandover,
  postResidencyEndingsByIdRequestException,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import {
  CONDITION_STATUS,
  END_CASES,
  ENDING_STATUS,
  EXCEPTION_CONDITIONS,
  RECIPIENT_TYPES,
  SELECT,
  labelOf,
  newKey,
  vietnamIso,
} from './admissionLabels'
import { Choices, Info, ReasonField, ResidentSelect } from './FormParts'

// Hồ sơ kết thúc lưu trú (004 T061, T107; US7, FR-062 → FR-067, Q-379). Điều kiện (a) → (h); (h) có hai ý (quyết toán số dư,
// tiền cọc và Kế toán đã xác nhận quyết toán). (e) bàn giao và ý xác nhận quyết toán không có ngoại lệ (FR-064). Lệnh Kết thúc
// lưu trú là lệnh của 001 ở tab Trạng thái của hồ sơ người cao tuổi.

const SETTLEMENT = ['END_H_SETTLEMENT', 'END_H_SETTLEMENT_CONFIRMATION']
const HOUSE_LABEL = 'Điều kiện (h): số dư và tiền cọc đã quyết toán'

function ConditionLine({ line }) {
  return (
    <li className="flex flex-col">
      <span className="type-body-sm">{line.name ?? line.code}</span>
      <span className="type-caption text-muted-foreground">
        {CONDITION_STATUS[line.status] ?? line.status}
        {line.basis ? `, ${line.basis}` : ''}
      </span>
    </li>
  )
}

function Conditions({ conditions }) {
  const settlement = conditions.filter((c) => SETTLEMENT.includes(c.code))
  const rest = conditions.filter((c) => !SETTLEMENT.includes(c.code))
  return (
    <section className="flex flex-col gap-2">
      <h3 className="type-card-title">Điều kiện kết thúc</h3>
      <ul className="flex flex-col gap-2">
        {rest.map((c) => (
          <ConditionLine key={c.code} line={c} />
        ))}
      </ul>
      {settlement.length > 0 && (
        <ul role="group" aria-label={HOUSE_LABEL} className="flex flex-col gap-2 border-t pt-2">
          {settlement.map((c) => (
            <ConditionLine key={c.code} line={c} />
          ))}
        </ul>
      )}
    </section>
  )
}

function HandoverForm({ ending, onDone, onCancel }) {
  const [type, setType] = useState('')
  const [members, setMembers] = useState([])
  const [staff, setStaff] = useState([])
  const [memberId, setMemberId] = useState('')
  const [facility, setFacility] = useState('')
  const [at, setAt] = useState('')
  const [staffId, setStaffId] = useState('')
  const [pickup, setPickup] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    getResidentsByResidentIdRelationships({ path: { residentId: ending.residentId } })
      .then(({ data }) => setMembers(data.filter((r) => r.status === 'ACTIVE')))
      .catch(() => {})
    getStaff({ query: { status: 'WORKING' } })
      .then(({ data }) => setStaff(data))
      .catch(() => {})
  }, [ending.residentId])

  const family = type === 'FAMILY_MEMBER'
  const errors = {
    type: !type && 'Chọn người nhận',
    member: family && !memberId && 'Chọn người thân nhận',
    facility: type === 'RECEIVING_FACILITY' && !facility.trim() && 'Nhập tên cơ sở tiếp nhận',
    at: !at && 'Nhập thời điểm bàn giao',
    staff: !staffId && 'Chọn nhân viên bàn giao',
  }

  async function save() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      await postResidencyEndingsByIdHandover({
        path: { id: ending.id },
        headers: newKey(),
        body: {
          recipientType: type,
          ...(family ? { familyMemberId: Number(memberId) } : { facilityName: facility.trim() }),
          handoverAt: vietnamIso(at),
          handoverStaffId: Number(staffId),
          ...(family && pickup && { pickupRecordId: Number(pickup) }),
        },
      })
      onDone('Đã ghi nhận bàn giao')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  const shown = (key) => (submitted ? errors[key] : null)
  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <h3 className="type-card-title">Ghi nhận bàn giao người cao tuổi</h3>
      <FieldGroup>
        <Choices label="Người nhận" options={RECIPIENT_TYPES} value={type} onChange={setType} error={shown('type')} />
        {family && (
          <>
            <Field data-invalid={!!shown('member') || undefined}>
              <FieldLabel htmlFor="handover-member">Người thân nhận</FieldLabel>
              <select id="handover-member" className={SELECT} value={memberId} onChange={(e) => setMemberId(e.target.value)}>
                <option value="">Chọn người thân</option>
                {members.map((r) => (
                  <option key={r.id} value={r.familyMember.id}>
                    {r.familyMember.fullName}
                  </option>
                ))}
              </select>
              {shown('member') && <FieldError>{shown('member')}</FieldError>}
            </Field>
            <Field>
              <FieldLabel htmlFor="handover-pickup">Mã bản ghi đón</FieldLabel>
              <Input id="handover-pickup" type="number" min="1" className="h-11" value={pickup} onChange={(e) => setPickup(e.target.value)} />
              <p className="type-caption text-muted-foreground">Bản ghi đón hợp lệ của 012; dùng một lần, không bắt buộc.</p>
            </Field>
          </>
        )}
        {type === 'RECEIVING_FACILITY' && (
          <Field data-invalid={!!shown('facility') || undefined}>
            <FieldLabel htmlFor="handover-facility">Tên cơ sở tiếp nhận</FieldLabel>
            <Input id="handover-facility" className="h-11" value={facility} onChange={(e) => setFacility(e.target.value)} />
            {shown('facility') && <FieldError>{shown('facility')}</FieldError>}
          </Field>
        )}
        <Field data-invalid={!!shown('at') || undefined}>
          <FieldLabel htmlFor="handover-at">Thời điểm bàn giao</FieldLabel>
          <Input id="handover-at" type="datetime-local" className="h-11" value={at} onChange={(e) => setAt(e.target.value)} />
          {shown('at') && <FieldError>{shown('at')}</FieldError>}
        </Field>
        <Field data-invalid={!!shown('staff') || undefined}>
          <FieldLabel htmlFor="handover-staff">Nhân viên bàn giao</FieldLabel>
          <select id="handover-staff" className={SELECT} value={staffId} onChange={(e) => setStaffId(e.target.value)}>
            <option value="">Chọn nhân viên</option>
            {staff.map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName ?? s.name ?? `Mã ${s.id}`}
              </option>
            ))}
          </select>
          {shown('staff') && <FieldError>{shown('staff')}</FieldError>}
        </Field>
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onCancel}>
          Đóng
        </Button>
        <Button className="h-11" disabled={busy} onClick={save}>
          Lưu bàn giao
        </Button>
      </div>
    </div>
  )
}

export function EndingView({ ending, role, canUpdate, onChanged }) {
  const [name, setName] = useState('')
  const [conditions, setConditions] = useState(null)
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [date, setDate] = useState('')
  const [code, setCode] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)
  const preparing = ending.status === 'PREPARING'

  useEffect(() => {
    let cancelled = false
    getResidentsById({ path: { id: ending.residentId } })
      .then(({ data }) => !cancelled && setName(data.personalInfo?.fullName ?? ''))
      .catch(() => {})
    getResidencyEndingsByIdConditions({ path: { id: ending.id } })
      .then(({ data }) => !cancelled && setConditions(data.conditions))
      .catch((e) => !cancelled && setFailure(problemOf(e)))
    return () => {
      cancelled = true
    }
  }, [ending.id, ending.residentId, ending.status, reload])

  function open(next) {
    setMode(next)
    setReason('')
    setDate('')
    setCode('')
    setSubmitted(false)
    setFailure(null)
  }

  async function run(call, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await call({ path: { id: ending.id }, headers: newKey(), body })
      open(null)
      setReload((n) => n + 1)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  function submit() {
    setSubmitted(true)
    if (!reason.trim()) return
    if (mode === 'change-date') {
      if (!date) return
      run(postResidencyEndingsByIdChangeDate, { expectedEndDate: date, reason: reason.trim() }, 'Đã đổi ngày kết thúc dự kiến')
    } else if (mode === 'exception') {
      if (!code) return
      run(postResidencyEndingsByIdRequestException, { conditionCode: code, reason: reason.trim() }, 'Đã gửi yêu cầu ngoại lệ, chờ Quản lý viện duyệt')
    } else {
      run(postResidencyEndingsByIdCancel, { reason: reason.trim() }, 'Đã hủy hồ sơ kết thúc')
    }
  }

  const exceptionLines = (conditions ?? []).filter(
    (c) =>
      EXCEPTION_CONDITIONS.includes(c.code) &&
      c.status === 'NOT_MET' &&
      (role !== 'ACCOUNTANT' || c.code === 'END_H_SETTLEMENT'),
  )
  const reasonError = submitted && !reason.trim() ? 'Nhập lý do' : null

  return (
    <div className="flex flex-col gap-4 rounded-2xl border bg-card p-5">
      <div className="flex flex-col gap-1">
        <StatusBadge status={ENDING_STATUS[ending.status] ?? ending.status} />
        <h2 className="type-title">{name || `Người cao tuổi mã ${ending.residentId}`}</h2>
        <p className="type-body-sm text-muted-foreground">{labelOf(END_CASES, ending.endCase)}</p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <Info label="Ngày kết thúc dự kiến">{formatDate(ending.expectedEndDate)}</Info>
        {ending.destination && <Info label="Nơi chuyển đến">{ending.destination}</Info>}
        {ending.actualEndedAt && <Info label="Đã kết thúc lúc">{formatDateTime(ending.actualEndedAt)}</Info>}
        {ending.overdueSince && <Info label="Quá hạn từ">{formatDate(ending.overdueSince)}</Info>}
        {ending.handover && (
          <Info label="Bàn giao">
            {`${ending.handover.recipient ?? ''}, ${ending.handover.at ? formatDateTime(ending.handover.at) : ''}`}
          </Info>
        )}
      </div>
      <ProblemAlert problem={failure} />
      {conditions && <Conditions conditions={conditions} />}

      {preparing && canUpdate && mode === null && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          <Button variant="outline" className="h-11" onClick={() => open('handover')}>
            Ghi nhận bàn giao…
          </Button>
          <Button variant="outline" className="h-11" onClick={() => open('exception')}>
            Xin ngoại lệ…
          </Button>
          <Button variant="outline" className="h-11" onClick={() => open('change-date')}>
            Đổi ngày kết thúc…
          </Button>
          <Button variant="outline" className="h-11" onClick={() => open('cancel')}>
            Hủy hồ sơ…
          </Button>
        </div>
      )}

      {mode === 'handover' && (
        <HandoverForm
          ending={ending}
          onCancel={() => open(null)}
          onDone={(message) => {
            open(null)
            setReload((n) => n + 1)
            onChanged(message)
          }}
        />
      )}
      {['exception', 'change-date', 'cancel'].includes(mode) && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <FieldGroup>
            {mode === 'exception' && (
              <Field data-invalid={(submitted && !code) || undefined}>
                <FieldLabel htmlFor="ending-exception">Điều kiện xin ngoại lệ</FieldLabel>
                <select id="ending-exception" className={SELECT} value={code} onChange={(e) => setCode(e.target.value)}>
                  <option value="">Chọn điều kiện</option>
                  {exceptionLines.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.name ?? c.code}
                    </option>
                  ))}
                </select>
                {submitted && !code && <FieldError>Chọn điều kiện</FieldError>}
              </Field>
            )}
            {mode === 'change-date' && (
              <Field data-invalid={(submitted && !date) || undefined}>
                <FieldLabel htmlFor="ending-new-date">Ngày kết thúc dự kiến mới</FieldLabel>
                <Input id="ending-new-date" type="date" className="h-11" value={date} onChange={(e) => setDate(e.target.value)} />
                {submitted && !date && <FieldError>Nhập ngày mới</FieldError>}
              </Field>
            )}
            <ReasonField id="ending-reason" value={reason} onChange={setReason} error={reasonError} />
          </FieldGroup>
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => open(null)}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={submit}>
              {{ exception: 'Gửi yêu cầu ngoại lệ', 'change-date': 'Lưu ngày mới', cancel: 'Xác nhận hủy hồ sơ' }[mode]}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

export function CreateEndingSheet({ role, onClose, onDone }) {
  const [residentId, setResidentId] = useState('')
  const [endCase, setEndCase] = useState('')
  const [medical, setMedical] = useState(false)
  const [date, setDate] = useState('')
  const [destination, setDestination] = useState('')
  const [requestedBy, setRequestedBy] = useState('')
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const cases = role === 'DOCTOR' ? END_CASES.filter(([c]) => ['DISCHARGE', 'TRANSFER_TO_ANOTHER_FACILITY'].includes(c)) : END_CASES

  const errors = {
    resident: !residentId && 'Chọn người cao tuổi',
    endCase: !endCase && 'Chọn trường hợp kết thúc',
    date: !date && 'Nhập ngày kết thúc dự kiến',
    destination: endCase === 'TRANSFER_TO_ANOTHER_FACILITY' && !destination.trim() && 'Nhập nơi chuyển đến',
    requestedBy: !requestedBy.trim() && 'Nhập người đề nghị',
    reason: !reason.trim() && 'Nhập lý do',
  }
  const shown = (key) => (submitted ? errors[key] : null)

  async function save() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      const { data } = await postResidencyEndings({
        headers: newKey(),
        body: {
          residentId: Number(residentId),
          endCase,
          medicalReason: medical,
          expectedEndDate: date,
          ...(destination.trim() && { destination: destination.trim() }),
          requestedBy: requestedBy.trim(),
          reason: reason.trim(),
        },
      })
      onDone(data)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle>Lập hồ sơ kết thúc lưu trú</SheetTitle>
        <SheetDescription>Mỗi người tối đa một hồ sơ chưa ở trạng thái cuối; bảng kỳ cuối nháp được tạo cùng lúc.</SheetDescription>
        <FieldGroup className="mt-4">
          <ResidentSelect
            id="ending-resident"
            statuses={['IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'HOSPITALIZED']}
            value={residentId}
            onChange={setResidentId}
            error={shown('resident')}
            onFailure={(e) => setFailure(problemOf(e))}
          />
          <Choices label="Trường hợp kết thúc" options={cases} value={endCase} onChange={setEndCase} error={shown('endCase')} />
          <Field>
            <label className="flex items-center gap-2 type-body-sm">
              <input type="checkbox" checked={medical} onChange={(e) => setMedical(e.target.checked)} />
              Vì lý do y tế
            </label>
          </Field>
          <Field data-invalid={!!shown('date') || undefined}>
            <FieldLabel htmlFor="ending-date">Ngày kết thúc dự kiến</FieldLabel>
            <Input id="ending-date" type="date" className="h-11" value={date} onChange={(e) => setDate(e.target.value)} />
            {shown('date') && <FieldError>{shown('date')}</FieldError>}
          </Field>
          <Field data-invalid={!!shown('destination') || undefined}>
            <FieldLabel htmlFor="ending-destination">Nơi chuyển đến</FieldLabel>
            <Input id="ending-destination" className="h-11" value={destination} onChange={(e) => setDestination(e.target.value)} />
            {shown('destination') && <FieldError>{shown('destination')}</FieldError>}
          </Field>
          <Field data-invalid={!!shown('requestedBy') || undefined}>
            <FieldLabel htmlFor="ending-requested-by">Người đề nghị</FieldLabel>
            <Input id="ending-requested-by" className="h-11" value={requestedBy} onChange={(e) => setRequestedBy(e.target.value)} />
            {shown('requestedBy') && <FieldError>{shown('requestedBy')}</FieldError>}
          </Field>
          <ReasonField id="ending-create-reason" value={reason} onChange={setReason} error={shown('reason')} />
        </FieldGroup>
        <div className="mt-4">
          <ProblemAlert problem={failure} />
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" className="h-11" onClick={onClose}>
            Đóng
          </Button>
          <Button className="h-11" disabled={busy} onClick={save}>
            Lập hồ sơ
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
