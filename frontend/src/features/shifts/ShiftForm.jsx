import { useState } from 'react'
import { getShiftsById, postShiftSchedulesByIdShifts, putShiftsById } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel, FieldGroup } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { localInput, versionHeader } from './shiftLabels'

export const selectClass =
  'h-9 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-2 focus-visible:outline-ring'
export function Choice({ label, children, ...props }) {
  return (
    <Field>
      <FieldLabel htmlFor={props.id}>{label}</FieldLabel>
      <select className={selectClass} {...props}>
        {children}
      </select>
    </Field>
  )
}
export function WarningConfirmation({ warnings, confirmed, onConfirm, reason, onReason }) {
  if (!warnings.length) return null
  return (
    <div className="flex flex-col gap-3 rounded-xl border border-attention bg-attention/10 p-4">
      <p className="font-medium">Cần xem xét trước khi tiếp tục</p>
      <ul className="ml-4 list-disc">
        {warnings.map((w, i) => (
          <li key={`${w.code}-${i}`}>{w.description}</li>
        ))}
      </ul>
      <label className="flex items-center gap-2">
        <input type="checkbox" checked={confirmed} onChange={(e) => onConfirm(e.target.checked)} />
        Tôi đã xem các cảnh báo
      </label>
      <Field>
        <FieldLabel htmlFor="warning-reason">Lý do</FieldLabel>
        <Textarea
          id="warning-reason"
          required
          maxLength={2000}
          value={reason}
          onChange={(e) => onReason(e.target.value)}
        />
      </Field>
    </div>
  )
}

export function ShiftForm({ schedule, shift, etag, templates, staff, onDone }) {
  const [current, setCurrent] = useState(shift)
  const [match, setMatch] = useState(etag)
  const [template, setTemplate] = useState(String(shift?.shiftTemplateId ?? ''))
  const [date, setDate] = useState(shift ? localInput(shift.startsAt).slice(0, 10) : `${schedule.month}-01`)
  const [start, setStart] = useState(localInput(shift?.startsAt))
  const [end, setEnd] = useState(localInput(shift?.endsAt))
  const [members, setMembers] = useState(shift?.staff ?? [])
  const [lead, setLead] = useState(String(shift?.leadStaffId ?? ''))
  const [warnings, setWarnings] = useState([])
  const [confirmed, setConfirmed] = useState(false)
  const [reason, setReason] = useState('')
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const stale = failure?.status === 412
  function changeMembers(id, checked) {
    setMembers(checked ? [...members, { staffId: id, onDutyDoctor: false }] : members.filter((m) => m.staffId !== id))
    if (!checked && lead === String(id)) setLead('')
  }
  async function reload() {
    setBusy(true)
    try {
      const { data, etag: next } = await getShiftsById({ path: { id: shift.id } })
      setCurrent(data)
      setMatch(next)
      setTemplate(String(data.shiftTemplateId))
      setDate(localInput(data.startsAt).slice(0, 10))
      setStart(localInput(data.startsAt))
      setEnd(localInput(data.endsAt))
      setMembers(data.staff ?? [])
      setLead(String(data.leadStaffId ?? ''))
      setWarnings([])
      setFailure(null)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  async function submit(e) {
    e.preventDefault()
    if (busy || stale || (warnings.length && (!confirmed || !reason.trim()))) return
    setBusy(true)
    setFailure(null)
    const body = {
      shiftTemplateId: Number(template),
      date,
      staff: members.map((m) => ({ staffId: m.staffId, onDutyDoctor: !!m.onDutyDoctor })),
      leadStaffId: lead ? Number(lead) : null,
      ...(start && { startsAt: `${start}:00+07:00` }),
      ...(end && { endsAt: `${end}:00+07:00` }),
      ...(warnings.length > 0 && { confirmWarnings: confirmed, reason }),
    }
    try {
      const headers = {
        'If-Match': current ? match : versionHeader(schedule.version),
        'Idempotency-Key': crypto.randomUUID(),
      }
      if (current) await putShiftsById({ path: { id: current.id }, headers, body })
      else await postShiftSchedulesByIdShifts({ path: { id: schedule.id }, headers, body })
      onDone()
    } catch (error) {
      setWarnings(error.problem?.warnings ?? [])
      setConfirmed(false)
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <ProblemAlert problem={failure}>
        {stale && (
          <>
            <p>Phiên bản hiện tại: {failure.currentValue?.version ?? 'đã thay đổi'}. Tải lại để xem trước khi sửa.</p>
            <Button type="button" variant="outline" onClick={reload} disabled={busy}>
              Tải lại ca
            </Button>
          </>
        )}
      </ProblemAlert>
      <FieldGroup>
        <Field>
          <FieldLabel htmlFor="shift-date">Ngày</FieldLabel>
          <Input id="shift-date" type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
        </Field>
        <Choice
          id="shift-template"
          label="Mẫu ca"
          required
          value={template}
          onChange={(e) => setTemplate(e.target.value)}
        >
          <option value="">Chọn mẫu ca</option>
          {templates
            .filter((t) => t.active || t.id === current?.shiftTemplateId)
            .map((t) => (
              <option key={t.id} value={t.id}>
                {t.name} · {t.startTime}–{t.endTime}
              </option>
            ))}
        </Choice>
        <p className="text-muted-foreground">
          Để trống giờ riêng để dùng giờ mẫu. Giờ kết thúc bằng giờ bắt đầu trong mẫu là ca 24 giờ.
        </p>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field>
            <FieldLabel htmlFor="shift-start">Bắt đầu riêng</FieldLabel>
            <Input id="shift-start" type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} />
          </Field>
          <Field>
            <FieldLabel htmlFor="shift-end">Kết thúc riêng</FieldLabel>
            <Input id="shift-end" type="datetime-local" value={end} onChange={(e) => setEnd(e.target.value)} />
          </Field>
        </div>
        <fieldset className="flex flex-col gap-3 rounded-xl border p-3">
          <legend className="px-1 font-medium">Nhân viên trong ca</legend>
          {staff.length === 0 && <p>Chưa có nhân viên để xếp ca.</p>}
          {staff.map((person) => {
            const member = members.find((m) => m.staffId === person.id)
            return (
              <div key={person.id} className="flex flex-wrap items-center justify-between gap-2">
                <label className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={!!member}
                    onChange={(e) => changeMembers(person.id, e.target.checked)}
                  />
                  {person.fullName}
                </label>
                {member && (
                  <label className="flex items-center gap-2 text-muted-foreground">
                    <input
                      type="checkbox"
                      checked={!!member.onDutyDoctor}
                      onChange={(e) =>
                        setMembers(
                          members.map((m) => (m.staffId === person.id ? { ...m, onDutyDoctor: e.target.checked } : m)),
                        )
                      }
                    />
                    Bác sĩ trực — {person.fullName}
                  </label>
                )}
              </div>
            )
          })}
        </fieldset>
        <Choice id="shift-lead" label="Người phụ trách ca" value={lead} onChange={(e) => setLead(e.target.value)}>
          <option value="">Chưa chỉ định</option>
          {members.map((m) => (
            <option key={m.staffId} value={m.staffId}>
              {staff.find((p) => p.id === m.staffId)?.fullName ?? `Nhân viên #${m.staffId}`}
            </option>
          ))}
        </Choice>
        <WarningConfirmation
          warnings={warnings}
          confirmed={confirmed}
          onConfirm={setConfirmed}
          reason={reason}
          onReason={setReason}
        />
      </FieldGroup>
      <Button
        type="submit"
        disabled={
          busy ||
          stale ||
          (current && !current.allowedCommands?.includes('edit')) ||
          (!!warnings.length && (!confirmed || !reason.trim()))
        }
      >
        {busy ? 'Đang lưu…' : warnings.length ? 'Xác nhận và lưu' : 'Lưu ca'}
      </Button>
    </form>
  )
}
