import { useState } from 'react'
import { postShiftSchedules, postShiftSchedulesByIdPublish, postShiftTemplates, putShiftTemplatesById } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { Choice, WarningConfirmation } from './ShiftForm'
import { SCOPES, versionHeader } from './shiftLabels'

export function ScheduleForm({ month, onDone }) {
  const [scope, setScope] = useState('FLOOR')
  const [id, setId] = useState('')
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      await postShiftSchedules({
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { month, scopeType: scope, ...(scope !== 'FACILITY' && { scopeId: Number(id) }) },
      })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form className="flex flex-col gap-4" onSubmit={submit}>
      <ProblemAlert problem={failure} />
      <Choice id="schedule-scope" label="Phạm vi lịch" value={scope} onChange={(e) => setScope(e.target.value)}>
        {Object.entries(SCOPES).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </Choice>
      {scope !== 'FACILITY' && (
        <Field>
          <FieldLabel htmlFor="schedule-scope-id">Mã tầng / khu vực</FieldLabel>
          <Input
            id="schedule-scope-id"
            type="number"
            min="1"
            required
            value={id}
            onChange={(e) => setId(e.target.value)}
          />
        </Field>
      )}
      <Button type="submit" disabled={busy}>
        {busy ? 'Đang lập…' : 'Lập lịch nháp'}
      </Button>
    </form>
  )
}

export function TemplateForm({ template, onDone, onStale }) {
  const [name, setName] = useState(template?.name ?? '')
  const [start, setStart] = useState(template?.startTime ?? '')
  const [end, setEnd] = useState(template?.endTime ?? '')
  const [scope, setScope] = useState(template?.scopeType ?? 'FLOOR')
  const [handover, setHandover] = useState(template?.handoverRequired ?? true)
  const [active, setActive] = useState(template?.active ?? true)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      const body = { name, startTime: start, endTime: end, scopeType: scope, handoverRequired: handover, active }
      const headers = {
        'Idempotency-Key': crypto.randomUUID(),
        ...(template && { 'If-Match': versionHeader(template.version) }),
      }
      if (template) await putShiftTemplatesById({ path: { id: template.id }, headers, body })
      else await postShiftTemplates({ headers, body })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form className="flex flex-col gap-4" onSubmit={submit}>
      <ProblemAlert problem={failure}>
        {failure?.status === 412 && (
          <Button type="button" onClick={onStale} variant="outline">
            Tải lại danh sách mẫu
          </Button>
        )}
      </ProblemAlert>
      <Field>
        <FieldLabel htmlFor="template-name">Tên mẫu ca</FieldLabel>
        <Input id="template-name" value={name} required onChange={(e) => setName(e.target.value)} />
      </Field>
      <div className="grid grid-cols-2 gap-3">
        <Field>
          <FieldLabel htmlFor="template-start">Giờ bắt đầu</FieldLabel>
          <Input id="template-start" type="time" required value={start} onChange={(e) => setStart(e.target.value)} />
        </Field>
        <Field>
          <FieldLabel htmlFor="template-end">Giờ kết thúc</FieldLabel>
          <Input id="template-end" type="time" required value={end} onChange={(e) => setEnd(e.target.value)} />
        </Field>
      </div>
      <Choice id="template-scope" label="Loại phạm vi" value={scope} onChange={(e) => setScope(e.target.value)}>
        {Object.entries(SCOPES).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </Choice>
      <label className="flex items-center gap-2">
        <input type="checkbox" checked={handover} onChange={(e) => setHandover(e.target.checked)} />
        Yêu cầu bàn giao
      </label>
      <label className="flex items-center gap-2">
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Đang sử dụng
      </label>
      <Button type="submit" disabled={busy || failure?.status === 412}>
        Lưu mẫu ca
      </Button>
    </form>
  )
}

export function PublicationForm({ schedule, preview, onDone }) {
  const [reason, setReason] = useState('')
  const [reasonRequired, setReasonRequired] = useState(false)
  const [confirmed, setConfirmed] = useState(false)
  const [removed, setRemoved] = useState([])
  const [failure, setFailure] = useState(null)
  const [warnings, setWarnings] = useState(preview.warnings ?? [])
  const [busy, setBusy] = useState(false)
  const information = warnings.filter((w) =>
    ['BELOW_STAFFING_THRESHOLD', 'STAFFING_RATIO_UNAVAILABLE'].includes(w.code),
  )
  const confirmationWarnings = warnings.filter(
    (w) => !['BELOW_STAFFING_THRESHOLD', 'STAFFING_RATIO_UNAVAILABLE'].includes(w.code),
  )
  const blocked =
    !!preview.unmetConditions?.length ||
    (preview.invalidAssignments ?? []).some((a) => !removed.includes(a.assignmentId)) ||
    ((reasonRequired || !!confirmationWarnings.length) && !reason.trim()) ||
    (!!confirmationWarnings.length && !confirmed)
  const needsPreview = failure && !['REASON_REQUIRED', 'WARNING_CONFIRMATION_REQUIRED'].includes(failure.errorCode)
  async function submit(e) {
    e.preventDefault()
    if (busy || blocked || needsPreview) return
    setBusy(true)
    try {
      await postShiftSchedulesByIdPublish({
        path: { id: schedule.id },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { ...(reason.trim() && { reason }), confirmWarnings: confirmed, assignmentIdsToRemove: removed },
      })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
      if (error.problem?.errorCode === 'REASON_REQUIRED') setReasonRequired(true)
      setWarnings(error.problem?.warnings ?? warnings)
      setConfirmed(false)
    } finally {
      setBusy(false)
    }
  }
  return (
    <form className="flex flex-col gap-4" onSubmit={submit}>
      <p>Công bố lịch sẽ thông báo cho nhân viên trong ca và lưu bản công bố.</p>
      <ProblemAlert problem={failure} />
      {!!preview.unmetConditions?.length && (
        <div className="rounded-xl border p-4">
          <p className="font-medium">Cần hoàn tất trước khi công bố</p>
          <ul className="ml-4 list-disc">
            {preview.unmetConditions.map((c) => (
              <li key={c.code}>{c.description}</li>
            ))}
          </ul>
        </div>
      )}
      {preview.invalidAssignments?.map((a) => (
        <label key={a.assignmentId} className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={removed.includes(a.assignmentId)}
            onChange={(e) =>
              setRemoved(
                e.target.checked ? [...removed, a.assignmentId] : removed.filter((id) => id !== a.assignmentId),
              )
            }
          />
          Gỡ phân công #{a.assignmentId}: {a.reason}
        </label>
      ))}
      <WarningConfirmation
        warnings={confirmationWarnings}
        confirmed={confirmed}
        onConfirm={setConfirmed}
        reason={reason}
        onReason={setReason}
      />
      {information.length > 0 && (
        <div className="flex flex-col gap-2 rounded-xl border bg-muted p-3">
          <p className="font-medium text-warning-foreground">Cảnh báo tỷ lệ phục vụ</p>
          {information.map((w, index) => (
            <div key={`${w.code}:${index}`}>
              <p>{w.description}</p>
              {w.details?.shiftId && <p className="type-caption text-muted-foreground">Ca #{w.details.shiftId}</p>}
            </div>
          ))}
        </div>
      )}
      {!confirmationWarnings.length && (
        <Field>
          <FieldLabel htmlFor="publish-reason">Lý do công bố / tự duyệt</FieldLabel>
          <Textarea
            id="publish-reason"
            required={reasonRequired}
            maxLength={2000}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        </Field>
      )}
      <Button type="submit" disabled={busy || blocked || !!needsPreview}>
        Công bố lịch
      </Button>
      {needsPreview && <p>Đóng khung và xem trước lại để cập nhật điều kiện công bố.</p>}
    </form>
  )
}
