import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getCallRemindersById, postCallRemindersByIdResults } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import {
  CALL_REASON_CHOICES,
  CALL_REASON_LABELS,
  CALL_RESULT_LABELS,
  HANDOFF_LABELS,
  REMINDER_STATUS_LABELS,
  label,
} from './notificationLabels'

// Khung lượt nhắc gọi (009; FR-027, FR-027a, FR-029a, FR-030a, FR-031): chi tiết, form ghi kết quả thay nội dung ngay trong
// khung. Số điện thoại là số hiện hành lúc mở. Hợp đồng chỉ có `residentId` nên khung không hiện tên người cao tuổi.

const AWAITING = 'AWAITING_RESULT'

// Giờ hiện tại theo dạng datetime-local của trình duyệt
function nowLocal() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

const timeOf = (iso) => formatDateTime(iso).slice(-5)

export function CallReminderSheet({ id, startRecording, onChanged }) {
  const [state, setState] = useState({ status: 'loading' })
  const [recording, setRecording] = useState(startRecording)
  const [failure, setFailure] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getCallRemindersById({ path: { id } })
      setState({ status: 'ready', reminder: data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [id])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở khung
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-3 p-6">
        <span className="sr-only">Đang tải lượt nhắc gọi</span>
        <Skeleton className="h-6 w-2/3" />
        <Skeleton className="h-20" />
      </div>
    )
  }
  if (state.status === 'error') {
    return (
      <div className="p-6">
        <ProblemAlert problem={state.problem} title="Không mở được lượt nhắc gọi" />
      </div>
    )
  }

  const backToDetail = async () => {
    setRecording(false)
    await load()
    onChanged()
  }

  if (recording && state.reminder.canRecordResult) {
    return (
      <CallResultForm
        reminder={state.reminder}
        onSaved={() => {
          setFailure(null)
          return backToDetail()
        }}
        onEnded={(problem) => {
          setFailure(problem)
          return backToDetail()
        }}
        onBack={() => setRecording(false)}
      />
    )
  }
  return <ReminderDetail reminder={state.reminder} failure={failure} onRecord={() => setRecording(true)} />
}

function ReminderDetail({ reminder: r, failure, onRecord }) {
  const person = r.personToCall
  const kind = person.type === 'STAFF' ? 'Nhân viên' : 'Người thân'
  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">Lượt nhắc gọi</SheetTitle>
        <SheetDescription>Gọi {person.fullName}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6 type-body-sm">
        <div className="flex flex-col gap-1">
          <StatusBadge status={label(REMINDER_STATUS_LABELS, r.status)} className="self-start" />
          <p className="type-card-title">{person.fullName}</p>
          <p className="text-muted-foreground">{person.relationship ? `${kind}, ${person.relationship}` : kind}</p>
          {person.phoneNumber ? (
            <a href={`tel:${person.phoneNumber}`} className="type-title tabular-nums underline-offset-4 hover:underline">
              {person.phoneNumber}
            </a>
          ) : (
            <p>Chưa có số điện thoại</p>
          )}
        </div>

        <section className="flex flex-col gap-2 rounded-xl border p-3">
          <h3 className="type-card-title">Nội dung cần báo</h3>
          {r.contentToReport.map((c, i) => (
            <p key={i} className="whitespace-pre-line">
              <span className="block type-caption text-muted-foreground tabular-nums">{timeOf(c.occurredAt)}</span>
              {c.content}
            </p>
          ))}
        </section>

        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
          <dt className="text-muted-foreground">Đang nhắc</dt>
          <dd>{r.currentlyReminded}</dd>
          <dt className="text-muted-foreground">Hạn</dt>
          <dd className="tabular-nums">{formatDateTime(r.dueAt)}</dd>
          {r.stopReason && (
            <>
              <dt className="text-muted-foreground">Lý do dừng</dt>
              <dd>{r.stopReason}</dd>
            </>
          )}
        </dl>

        {r.results.length > 0 && (
          <section className="flex flex-col gap-2 rounded-xl border p-3">
            <h3 className="type-card-title">Kết quả gọi</h3>
            {r.results.map((result, i) => (
              <ResultLines key={i} result={result} />
            ))}
          </section>
        )}

        {r.handoffs.length > 0 && (
          <section className="flex flex-col gap-1">
            <h3 className="type-card-title">Lịch sử</h3>
            {r.handoffs.map((h, i) => (
              <p key={i} className="text-muted-foreground">
                {`${timeOf(h.occurredAt)} ${HANDOFF_LABELS[h.reason] ?? 'Chuyển nhắc'}`}
              </p>
            ))}
          </section>
        )}

        <ProblemAlert problem={failure} />
        {r.canRecordResult && (
          <Button className="h-11 self-start" onClick={onRecord}>
            {r.status === AWAITING ? 'Ghi kết quả gọi' : 'Ghi lần gọi bổ sung'}
          </Button>
        )}
        {!r.canRecordResult && r.status === AWAITING && (
          <p className="text-muted-foreground">Bạn chỉ xem lượt nhắc này, không ghi được kết quả.</p>
        )}
      </div>
    </>
  )
}

function ResultLines({ result }) {
  const text = label(CALL_RESULT_LABELS, result.result)
  const reason = label(CALL_REASON_LABELS, result.reason)
  const calledAt = formatDateTime(result.calledAt)
  return (
    <div className="flex flex-col">
      <p>{reason ? `${text}, ${reason}` : text}</p>
      <p className="type-caption text-muted-foreground tabular-nums">
        {result.supplementary ? `${calledAt}, lần gọi bổ sung` : calledAt}
      </p>
      {result.answeredBy && <p className="type-caption text-muted-foreground">Người nghe máy: {result.answeredBy}</p>}
      {result.note && <p className="whitespace-pre-line">{result.note}</p>}
    </div>
  )
}

function CallResultForm({ reminder, onSaved, onEnded, onBack }) {
  const [result, setResult] = useState(null)
  const [reason, setReason] = useState(null)
  const [calledAt, setCalledAt] = useState(nowLocal)
  const [phone, setPhone] = useState(reminder.personToCall.phoneNumber ?? '')
  const [answeredBy, setAnsweredBy] = useState('')
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  // Một khóa cho cả form: bấm lại khi mạng chập chờn không ghi hai kết quả; backend từ chối thì lần sau là lệnh mới
  const key = useRef(crypto.randomUUID())

  function validate() {
    if (!result) return { choice: 'Chọn kết quả gọi.' }
    if (result === 'NOT_REACHED' && !reason) return { choice: 'Chọn lý do không liên lạc được.' }
    if (result === 'REACHED' && !note.trim()) return { note: 'Cần ghi chú khi đã liên lạc được.' }
    return {}
  }

  async function save() {
    setFailure(null)
    const invalid = validate()
    setErrors(invalid)
    if (Object.keys(invalid).length > 0) return
    setBusy(true)
    try {
      await postCallRemindersByIdResults({
        path: { id: reminder.id },
        headers: { 'Idempotency-Key': key.current },
        body: {
          result,
          calledAt: new Date(calledAt).toISOString(),
          ...(phone.trim() && { calledPhoneNumber: phone.trim() }),
          ...(result === 'REACHED' && answeredBy.trim() && { answeredBy: answeredBy.trim() }),
          ...(result === 'NOT_REACHED' && { reason }),
          ...(note.trim() && { note: note.trim() }),
        },
      })
      await onSaved()
    } catch (error) {
      const p = problemOf(error)
      if (!(error instanceof ApiError)) {
        setFailure(p)
        return
      }
      key.current = crypto.randomUUID()
      if (error.status === 422) setErrors({ note: p.detail || p.title })
      else if (error.status === 409) await onEnded(p)
      else setFailure(p)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">Ghi kết quả gọi</SheetTitle>
        <SheetDescription>Gọi {reminder.personToCall.fullName}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6">
        <FieldGroup>
          <Field data-invalid={!!errors.choice || undefined}>
            <FieldLabel>Kết quả</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={result ? [result] : []}
              onValueChange={(next) => next[0] && setResult(next[0])}
              className="flex-wrap"
            >
              {Object.entries(CALL_RESULT_LABELS).map(([value, text]) => (
                <ToggleGroupItem key={value} value={value} className="h-11 px-4">
                  {text}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
            {result === 'NOT_REACHED' && (
              <>
                <FieldLabel>Lý do</FieldLabel>
                <ToggleGroup
                  variant="outline"
                  value={reason ? [reason] : []}
                  onValueChange={(next) => next[0] && setReason(next[0])}
                  className="flex-wrap"
                >
                  {Object.entries(CALL_REASON_CHOICES).map(([value, text]) => (
                    <ToggleGroupItem key={value} value={value} className="h-11 px-4">
                      {text}
                    </ToggleGroupItem>
                  ))}
                </ToggleGroup>
              </>
            )}
            {errors.choice && <FieldError>{errors.choice}</FieldError>}
          </Field>
          <Field>
            <FieldLabel htmlFor="call-called-at">Thời điểm gọi</FieldLabel>
            <Input
              id="call-called-at"
              type="datetime-local"
              className="h-11"
              value={calledAt}
              onChange={(e) => setCalledAt(e.target.value)}
            />
          </Field>
          <Field>
            <FieldLabel htmlFor="call-phone">Số đã gọi</FieldLabel>
            <Input
              id="call-phone"
              type="tel"
              className="h-11"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </Field>
          {result === 'REACHED' && (
            <Field>
              <FieldLabel htmlFor="call-answered-by">Người nghe máy (nếu khác người cần gọi)</FieldLabel>
              <Input
                id="call-answered-by"
                className="h-11"
                value={answeredBy}
                onChange={(e) => setAnsweredBy(e.target.value)}
              />
            </Field>
          )}
          <Field data-invalid={!!errors.note || undefined}>
            <FieldLabel htmlFor="call-note">Ghi chú</FieldLabel>
            <Textarea
              id="call-note"
              aria-invalid={!!errors.note || undefined}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
            {errors.note && <FieldError>{errors.note}</FieldError>}
          </Field>
        </FieldGroup>
        <p className="type-caption text-muted-foreground">Kết quả đã lưu chỉ sửa được bằng đính chính.</p>
        <ProblemAlert problem={failure} />
        <div className="flex flex-wrap gap-2">
          <Button className="h-11" disabled={busy} onClick={save}>
            Lưu kết quả
          </Button>
          <Button variant="outline" className="h-11" disabled={busy} onClick={onBack}>
            Quay lại
          </Button>
        </div>
      </div>
    </>
  )
}
