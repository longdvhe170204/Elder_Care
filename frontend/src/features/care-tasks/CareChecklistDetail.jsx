import { useRef, useState } from 'react'
import { postCareTasksByIdClaim, postCareTasksByIdNotPerform, postCareTasksByIdRecord } from '@/api'
import { Button } from '@/components/ui/button'
import { FieldError } from '@/components/ui/field'
import { CareNote, ChoiceNote } from '@/common/CareNotes'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { CareField } from './CareField'
import { WhoLine } from './TaskCard'
import { sourceText, timeText } from './taskText'
import { CareResultForm } from './CareResultForm'

export function CareChecklistDetail({ task, reasons, Title, Description, onSaved, onBusy, readOnly }) {
  const [notPerformed, setNotPerformed] = useState(false)
  const [reasonId, setReasonId] = useState('')
  const [note, setNote] = useState('')
  const [missing, setMissing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const retry = useRef(null)
  async function command(operation, body) {
    if (busy) return
    const signature = JSON.stringify([operation, body])
    if (retry.current?.signature !== signature) retry.current = { signature, key: crypto.randomUUID() }
    setBusy(true)
    onBusy(true)
    setFailure(null)
    try {
      const params = { path: { id: task.id }, headers: { 'Idempotency-Key': retry.current.key } }
      let message
      if (operation === 'claim') {
        await postCareTasksByIdClaim(params)
        message = 'Đã nhận công việc.'
      } else if (operation === 'record') {
        const response = await postCareTasksByIdRecord({ ...params, body })
        message = response.status === 202 ? 'Đã gửi lần đo. Chờ xác nhận đo lại.' : 'Đã lưu kết quả.'
      } else {
        await postCareTasksByIdNotPerform({ ...params, body })
        message = 'Đã lưu Không thực hiện.'
      }
      await onSaved(message)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
      onBusy(false)
    }
  }
  const can = (command) => !readOnly && task.commands.includes(command)
  return (
    <>
      <div className="flex flex-col gap-3">
        <StatusBadge status={task.status} />
        <Title className="type-title">{task.title}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {timeText(task)}, {sourceText(task)}
        </Description>
        <WhoLine task={task} />
        {task.content && <p>{task.content}</p>}
        {task.awaitingRemeasure && <p className="text-warning-foreground">Chờ xác nhận đo lại</p>}
        <ChoiceNote choices={task.riskChoices} />
        <CareNote note={task.careNote} />
        {task.assignee && <p className="type-body-sm text-muted-foreground">Phụ trách: {task.assignee}</p>}
      </div>
      <ProblemAlert problem={failure} />
      {can('record') && !notPerformed && (
        <CareResultForm
          type={task.raw.resultType}
          importance={task.raw.importance}
          busy={busy}
          failure={failure}
          onSubmit={(body) => command('record', body)}
        />
      )}
      {can('claim') && (
        <Button disabled={busy} onClick={() => command('claim')}>
          Nhận công việc
        </Button>
      )}
      {can('notPerform') && !notPerformed && (
        <Button variant="outline" disabled={busy} onClick={() => setNotPerformed(true)}>
          Không thực hiện được…
        </Button>
      )}
      {can('notPerform') && notPerformed && (
        <div className="flex flex-col gap-4">
          <fieldset
            disabled={busy}
            aria-describedby={missing ? 'reason-required' : undefined}
            className="flex flex-col gap-2"
          >
            <legend className="type-label">Loại lý do (bắt buộc)</legend>
            {reasons.map((r) => (
              <label key={r.id} className="flex min-h-11 items-center gap-3">
                <input
                  type="radio"
                  className="size-5 accent-primary"
                  name={`reason-${task.id}`}
                  value={r.id}
                  checked={reasonId === String(r.id)}
                  onChange={() => {
                    setReasonId(String(r.id))
                    setMissing(false)
                  }}
                />
                {r.name}
              </label>
            ))}
            {missing && <FieldError id="reason-required">Chọn loại lý do Không thực hiện.</FieldError>}
          </fieldset>
          <CareField label="Ghi chú" value={note} type="textarea" maxLength={2000} disabled={busy} onChange={setNote} />
          <p className="type-body-sm text-muted-foreground">Có thể ghi điều người cao tuổi đã nói.</p>
          <Button
            disabled={busy}
            onClick={() => {
              if (!reasonId) {
                setMissing(true)
                return
              }
              command('notPerform', { reasonTypeId: Number(reasonId), ...(note ? { note } : {}) })
            }}
          >
            Lưu Không thực hiện
          </Button>
        </div>
      )}
      {busy && <p role="status">Đang lưu…</p>}
    </>
  )
}
