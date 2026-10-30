import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { CareNote, ChoiceNote } from '@/common/CareNotes'
import { DEMO_MEAL_LEVELS } from '@/common/demoData'
import { StatusBadge } from '@/common/StatusBadge'
import { WhoLine } from './TaskCard'
import { DONE, sourceText, timeText } from './taskText'

// Khung chi tiết công việc (DESIGN.md 7.2 task-detail): thông tin việc, care-notes, form ghi nhận; nút chính ở
// chân khung (005 US4 kịch bản 3–6, 10). Đặt trong DetailLayout (khung cố định từ 1280px, hẹp hơn là Sheet).
// Bản mẫu: kiểm tra trên màn chỉ để hiện lỗi tại ô; kiểm tra thật và trạng thái mới do backend trả.

// Chỉ công việc Đến hạn hoặc Quá hạn mới ghi nhận được (bảng trạng thái công việc, mục 8.x nghiep-vu.md).
const RECORDABLE = ['Đến hạn', 'Quá hạn']

function ResultField({ task, value, onChange, invalid }) {
  if (task.kind === 'meal') {
    return (
      <Field data-invalid={invalid || undefined}>
        <FieldLabel>Mức ăn</FieldLabel>
        <ToggleGroup
          variant="outline"
          value={value ? [value] : []}
          onValueChange={(next) => onChange(next[0] ?? '')}
          className="grid w-full grid-cols-2"
          aria-invalid={invalid || undefined}
        >
          {DEMO_MEAL_LEVELS.map((level) => (
            <ToggleGroupItem key={level} value={level} className="h-11">
              {level}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        {invalid && <FieldError>Chọn mức ăn</FieldError>}
      </Field>
    )
  }

  if (task.kind === 'simple') return null

  const label = { water: 'Lượng nước (ml)', measure: `Giá trị đo (${task.unit})`, quantity: `Số lượng (${task.unit})` }[task.kind]
  const numeric = NUMERIC_KINDS.includes(task.kind)
  return (
    <Field data-invalid={invalid || undefined}>
      <FieldLabel htmlFor="result-value">{label}</FieldLabel>
      <Input
        id="result-value"
        inputMode={numeric ? 'numeric' : 'text'}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={invalid || undefined}
        className="h-11"
      />
      {invalid && (
        <FieldError>{numeric ? `Nhập số lớn hơn 0 cho ${label.toLowerCase()}` : `Nhập ${label.toLowerCase()}`}</FieldError>
      )}
    </Field>
  )
}

const NUMERIC_KINDS = ['water', 'quantity']

// Hoàn thành trễ việc Quan trọng, Bắt buộc phải có lý do (hợp đồng api-005 POST /cong-viec/{id}/ghi-nhan, GhiNhan.lyDo)
const LATE_REASON_IMPORTANCE = ['Quan trọng', 'Bắt buộc']

function RecordForm({ task, now, reasons, onSave, onNotDone }) {
  const [mode, setMode] = useState('result')
  const [value, setValue] = useState('')
  const [doneAt, setDoneAt] = useState(now)
  const [note, setNote] = useState('')
  const [lateReason, setLateReason] = useState('')
  const [reasonId, setReasonId] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const needsValue = task.kind !== 'simple'
  // ml, số lượng phải là số dương; giá trị đo để dạng chữ (ví dụ 120/80). Backend vẫn kiểm lại (422).
  const valueMissing =
    needsValue && (!value.trim() || (NUMERIC_KINDS.includes(task.kind) && !(Number(value.replace(',', '.')) > 0)))
  const reasonMissing = !reasonId
  // Giờ 24h HH:mm (NFR-11); không sau thời điểm ghi (mục 8.6). Backend vẫn kiểm lại.
  const timeInvalid = !/^([01]\d|2[0-3]):[0-5]\d$/.test(doneAt) || doneAt > now
  const needsLateReason = !timeInvalid && doneAt > task.to && LATE_REASON_IMPORTANCE.includes(task.importance)
  const lateReasonMissing = needsLateReason && !lateReason.trim()
  const canNotDo = task.commands.includes('KHONG_THUC_HIEN')

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (mode === 'result' && !valueMissing && !timeInvalid && !lateReasonMissing) {
      onSave({ value, doneAt, note, lateReason: needsLateReason ? lateReason.trim() : undefined })
    }
    if (mode === 'not-done' && !reasonMissing) {
      onNotDone({ reasonId: Number(reasonId), reasonName: reasons.find((r) => String(r.id) === reasonId)?.ten, note: note.trim() })
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-1 flex-col gap-5" noValidate>
      <FieldGroup>
        {mode === 'result' ? (
          <>
            <ResultField task={task} value={value} onChange={setValue} invalid={submitted && valueMissing} />
            <Field data-invalid={(submitted && timeInvalid) || undefined}>
              <FieldLabel htmlFor="done-at">Thời điểm thực hiện</FieldLabel>
              <Input
                id="done-at"
                inputMode="numeric"
                placeholder="HH:mm"
                aria-invalid={(submitted && timeInvalid) || undefined}
                value={doneAt}
                onChange={(e) => setDoneAt(e.target.value)}
                className="h-11 tabular-nums"
              />
              {submitted && timeInvalid ? (
                <FieldError>Nhập giờ dạng HH:mm, không sau {now}</FieldError>
              ) : (
                <FieldDescription>Giờ 24h, không sau thời điểm ghi.</FieldDescription>
              )}
            </Field>
            {needsLateReason && (
              <Field data-invalid={(submitted && lateReasonMissing) || undefined}>
                <FieldLabel htmlFor="late-reason">Lý do hoàn thành trễ</FieldLabel>
                <Textarea
                  id="late-reason"
                  value={lateReason}
                  onChange={(e) => setLateReason(e.target.value)}
                  rows={2}
                  aria-invalid={(submitted && lateReasonMissing) || undefined}
                />
                {submitted && lateReasonMissing ? (
                  <FieldError>Nhập lý do: việc {task.importance} thực hiện sau {task.to}</FieldError>
                ) : (
                  <FieldDescription>Bắt buộc vì việc {task.importance} thực hiện sau cuối khung {task.to}.</FieldDescription>
                )}
              </Field>
            )}
            <Field>
              <FieldLabel htmlFor="note">Ghi chú</FieldLabel>
              <Textarea id="note" value={note} onChange={(e) => setNote(e.target.value)} rows={3} />
            </Field>
          </>
        ) : (
          // Lý do = loại lý do (bắt buộc) + ghi chú (tùy chọn) (005 FR-028a, Q-350). "Người cao tuổi từ chối"
          // luôn đứng đầu; tên loại mô tả việc đã xảy ra, không gắn nhãn người cao tuổi.
          <>
            <Field data-invalid={(submitted && reasonMissing) || undefined}>
              <FieldLabel>Loại lý do</FieldLabel>
              <ToggleGroup
                variant="outline"
                value={reasonId ? [reasonId] : []}
                onValueChange={(next) => setReasonId(next[0] ?? '')}
                className="flex w-full flex-col items-stretch"
                aria-invalid={(submitted && reasonMissing) || undefined}
              >
                {reasons.map((r) => (
                  <ToggleGroupItem key={r.id} value={String(r.id)} className="h-11 justify-start px-4">
                    {r.ten}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
              {submitted && reasonMissing && <FieldError>Chọn loại lý do</FieldError>}
            </Field>
            <Field>
              <FieldLabel htmlFor="reason-note">Ghi chú</FieldLabel>
              <Textarea id="reason-note" value={note} onChange={(e) => setNote(e.target.value)} rows={3} />
              <FieldDescription>Không bắt buộc. Ghi điều người cao tuổi nói hoặc việc đã xảy ra.</FieldDescription>
            </Field>
          </>
        )}
      </FieldGroup>

      <div className="mt-auto flex flex-col gap-2">
        <Button type="submit" className="h-12">
          {mode === 'result' ? 'Lưu kết quả' : 'Ghi nhận không thực hiện'}
        </Button>
        {canNotDo && (
          <Button
            type="button"
            variant="ghost"
            className="h-11"
            onClick={() => {
              setMode(mode === 'result' ? 'not-done' : 'result')
              setSubmitted(false)
            }}
          >
            {mode === 'result' ? 'Không thực hiện được…' : 'Quay lại ghi kết quả'}
          </Button>
        )}
      </div>
    </form>
  )
}

// Thời gian trễ so với cuối khung, hiện ở dòng trạng thái của việc Quá hạn
function lateText(task, now) {
  const toMin = (hhmm) => hhmm.split(':').map(Number).reduce((h, m) => h * 60 + m)
  const late = toMin(now) - toMin(task.to)
  if (late <= 0) return null
  const h = Math.floor(late / 60)
  const m = late % 60
  return `trễ ${[h && `${h} giờ`, m && `${m} phút`].filter(Boolean).join(' ')}`
}

// Nội dung khung chi tiết; dùng chung cho khung cố định và Sheet. Title là tên việc (tiêu đề hộp thoại khi ở Sheet).
// Lệnh ghi nhận theo lenhDuocPhep của công việc (DESIGN.md mục 1 điều 8); kết quả việc đã đóng từ CongViecChiTiet.
export function TaskDetail({ task, now, reasons, result, onSave, onNotDone, Title = 'h2', Description = 'p' }) {
  const recordable = task.commands.includes('GHI_NHAN') && RECORDABLE.includes(task.status)
  const late = task.status === 'Quá hạn' ? lateText(task, now) : null

  return (
    <div className="flex min-h-full flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="flex items-center gap-1.5">
          <StatusBadge status={task.status} />
          {late && <span className="type-caption text-destructive">{late}</span>}
        </span>
        {task.importance && task.importance !== 'Thường' && (
          <span className="type-caption text-muted-foreground">{task.importance}</span>
        )}
      </div>
      <div className="flex flex-col gap-1">
        <Title className="type-title">{task.title}</Title>
        <Description className="type-body-sm text-muted-foreground">
          <span className="tabular-nums">{timeText(task)}</span>, {sourceText(task)},{' '}
          {task.assignee ? `phụ trách ${task.assignee}` : 'chưa ai nhận'}
        </Description>
        {task.content && <p className="type-body-sm">{task.content}</p>}
      </div>
      <WhoLine task={task} />
      {task.awaitingRemeasure && (
        <p className="type-body-sm text-warning-foreground">Chờ xác nhận đo lại</p>
      )}
      <ChoiceNote choices={task.riskChoices} />
      <CareNote note={task.careNote} />
      {DONE.includes(task.status) && result && (
        <p className="type-body-sm">
          <span className="text-muted-foreground">Kết quả: </span>
          {result}
        </p>
      )}
      {recordable && (
        <>
          <div className="h-px bg-border" />
          <RecordForm key={task.id} task={task} now={now} reasons={reasons} onSave={onSave} onNotDone={onNotDone} />
        </>
      )}
    </div>
  )
}
