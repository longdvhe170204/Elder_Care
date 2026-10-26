import { useState } from 'react'
import {
  postShiftsByIdAddStaff,
  postShiftsByIdChangeLead,
  postShiftsByIdRecordAbsence,
  postShiftsByIdCancel,
} from '@/api'
import { Button } from '@/components/ui/button'
import { FieldGroup, Field, FieldLabel, FieldError } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { Choice } from './ShiftForm'
import { DutyBasisPanel } from './DutyBasisPanel'

const ACTIONS = {
  addStaff: { label: 'Bổ sung nhân viên', confirm: 'Xác nhận bổ sung', api: postShiftsByIdAddStaff },
  changeLead: {
    label: 'Chuyển Người phụ trách',
    confirm: 'Xác nhận chuyển người phụ trách',
    api: postShiftsByIdChangeLead,
  },
  recordAbsence: { label: 'Ghi nhận vắng ca', confirm: 'Xác nhận ghi vắng', api: postShiftsByIdRecordAbsence },
  cancelShift: { label: 'Hủy ca', confirm: 'Xác nhận hủy ca', api: postShiftsByIdCancel },
}

export function PostPublicationPanel({ shift, staff = [], scopeId, onDone }) {
  const [action, setAction] = useState(null)
  const [person, setPerson] = useState('')
  const [replacement, setReplacement] = useState('')
  const [reason, setReason] = useState('')
  const [tried, setTried] = useState(false)
  const [failure, setFailure] = useState(null)
  const [warnings, setWarnings] = useState([])
  const [confirmed, setConfirmed] = useState(false)
  const [busy, setBusy] = useState(false)
  const commands = shift.allowedCommands ?? []
  const choices = Object.keys(ACTIONS).filter((key) => commands.includes(key))
  const history = commands.includes('viewDutyBasisAudit')
  if (!choices.length && !history) return null
  const members = (shift.staff ?? []).filter((member) => !member.absent && !member.approvedLeave)
  const name = (id) => staff.find((p) => p.id === id)?.fullName ?? `Nhân viên #${id}`
  const invalid = (tried && !reason.trim()) || failure?.errorCode === 'REASON_REQUIRED'
  const options =
    action === 'addStaff'
      ? staff.filter((p) => p.status === 'WORKING' && !shift.staff?.some((m) => m.staffId === p.id)).map((p) => p.id)
      : members.map((m) => m.staffId).filter((id) => action !== 'changeLead' || id !== shift.leadStaffId)
  function select(next) {
    setAction(next)
    setPerson('')
    setReplacement('')
    setReason('')
    setTried(false)
    setFailure(null)
    setWarnings([])
    setConfirmed(false)
  }
  async function submit(event) {
    event.preventDefault()
    setTried(true)
    if (!reason.trim() || (action !== 'cancelShift' && !person) || (warnings.length && !confirmed)) return
    setBusy(true)
    const body = { reason: reason.trim() }
    if (action !== 'cancelShift') body.staffId = Number(person)
    if (action === 'recordAbsence' && Number(person) === shift.leadStaffId && replacement)
      body.replacementLeadStaffId = Number(replacement)
    if (action === 'addStaff') body.confirmWarnings = confirmed
    try {
      await ACTIONS[action].api({ path: { id: shift.id }, body })
      setReason('')
      onDone?.()
    } catch (error) {
      setFailure(problemOf(error))
      setWarnings(error.problem?.warnings ?? [])
      setConfirmed(false)
    } finally {
      setBusy(false)
    }
  }
  return (
    <section className="flex flex-col gap-4 border-t pt-4" aria-label="Điều chỉnh ca">
      <h3 className="type-card-title">Điều chỉnh ca</h3>
      <div className="flex flex-wrap gap-2">
        {choices.map((key) => (
          <Button
            key={key}
            type="button"
            variant={key === 'cancelShift' ? 'destructive' : 'outline'}
            disabled={busy}
            onClick={() => select(key)}
          >
            {ACTIONS[key].label}
          </Button>
        ))}
        {history && (
          <Button type="button" variant="outline" disabled={busy} onClick={() => select('history')}>
            Lịch sử căn cứ nhiệm vụ
          </Button>
        )}
      </div>
      {action === 'history' && <DutyBasisPanel shift={shift} scopeId={scopeId} staff={staff} />}
      {ACTIONS[action] && (
        <form onSubmit={submit} className="flex flex-col gap-4" aria-label={ACTIONS[action].label}>
          <ProblemAlert problem={failure}>
            {failure?.errorCode === 'OUTSIDE_ABSENCE_REPORTING_WINDOW' && (
              <p>Hãy lập yêu cầu nghỉ đột xuất tại mục Đổi ca, nghỉ phép.</p>
            )}
            {['STATE_CHANGED', 'STAFF_NOT_IN_SHIFT', 'OUTSIDE_ABSENCE_REPORTING_WINDOW'].includes(
              failure?.errorCode,
            ) && (
              <Button type="button" variant="outline" onClick={onDone}>
                Tải lại ca
              </Button>
            )}
          </ProblemAlert>
          {action === 'cancelShift' && (
            <p className="type-body-sm">Ca và các phân công trong ca sẽ bị hủy. Nhân viên sẽ nhận thông báo.</p>
          )}
          <FieldGroup>
            {action !== 'cancelShift' && (
              <Choice
                id="command-staff"
                label={
                  action === 'recordAbsence'
                    ? 'Nhân viên vắng'
                    : action === 'changeLead'
                      ? 'Người phụ trách mới'
                      : 'Nhân viên bổ sung'
                }
                required
                value={person}
                onChange={(e) => {
                  setPerson(e.target.value)
                  setReplacement('')
                  setWarnings([])
                  setConfirmed(false)
                  setFailure(null)
                }}
              >
                <option value="">Chọn nhân viên</option>
                {options.map((id) => (
                  <option key={id} value={id}>
                    {name(id)}
                  </option>
                ))}
              </Choice>
            )}
            {action === 'recordAbsence' && Number(person) === shift.leadStaffId && (
              <>
                <Choice
                  id="replacement-lead"
                  label="Người phụ trách thay thế"
                  value={replacement}
                  onChange={(e) => setReplacement(e.target.value)}
                >
                  <option value="">Chưa chỉ định</option>
                  {members
                    .filter((m) => m.staffId !== Number(person))
                    .map((m) => (
                      <option key={m.staffId} value={m.staffId}>
                        {name(m.staffId)}
                      </option>
                    ))}
                </Choice>
                <p className="type-body-sm text-muted-foreground">
                  Nếu còn người đủ điều kiện, phải chọn người thay trong cùng lần ghi nhận.
                </p>
              </>
            )}
            <Field data-invalid={invalid || undefined}>
              <FieldLabel htmlFor="command-reason">Lý do</FieldLabel>
              <Textarea
                id="command-reason"
                required
                maxLength={2000}
                rows={3}
                value={reason}
                aria-invalid={invalid || undefined}
                onChange={(e) => {
                  setReason(e.target.value)
                  if (failure?.errorCode === 'REASON_REQUIRED') setFailure(null)
                }}
              />
              {invalid && <FieldError>Nhập lý do trước khi xác nhận.</FieldError>}
            </Field>
            {warnings.length > 0 && (
              <Alert variant="attention">
                <AlertDescription>
                  <ul className="ml-4 list-disc">
                    {warnings.map((warning, index) => (
                      <li key={`${warning.code}-${index}`}>{warning.description}</li>
                    ))}
                  </ul>
                  <Field orientation="horizontal">
                    <Checkbox id="confirm-shift-warning" checked={confirmed} onCheckedChange={setConfirmed} />
                    <FieldLabel htmlFor="confirm-shift-warning">Đã xem cảnh báo</FieldLabel>
                  </Field>
                </AlertDescription>
              </Alert>
            )}
          </FieldGroup>
          <Button
            type="submit"
            variant={action === 'cancelShift' ? 'destructive' : 'default'}
            disabled={busy || (warnings.length > 0 && !confirmed)}
          >
            {busy ? 'Đang thực hiện…' : ACTIONS[action].confirm}
          </Button>
        </form>
      )}
    </section>
  )
}
