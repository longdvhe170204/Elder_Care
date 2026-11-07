import { useState } from 'react'
import {
  postAlertsByIdExecuteChoice,
  postAlertsByIdWishReconfirmation,
  postCriticalFlagsByIdConfirm,
  postCriticalFlagsByIdRemove,
  postResidentsByResidentIdCriticalFlags,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// Khối "Nguy kịch" (007 T046; US11; FR-050a → FR-050g; BR-M05-15, 16). Nhận alert (id, criticalFlagId) từ màn cảnh
// báo (Alert.criticalFlagId); chưa có cảnh báo thì chỉ có form ghi dấu. Nút lệnh gửi thẳng, backend kiểm quyền và điều
// kiện (409 hiện danh sách điều kiện chưa thỏa, 422 báo tại ô). Kết quả "nguyện vọng mới" (CHANGED) cần form nguyện vọng
// của 001 nên chưa có ở đây: ghi ở tab Nguyện vọng của hồ sơ.

const FLAG_TYPES = [
  ['OFFICIAL', 'Dấu chính thức'],
  ['PROVISIONAL', 'Dấu tạm (không có Bác sĩ trực)'],
]
const CHANNELS = [
  ['PHONE_CALL', 'Gọi điện'],
  ['IN_PERSON', 'Trực tiếp'],
]
const RESULTS = [
  ['KEPT', 'Giữ nguyện vọng'],
  ['UNREACHABLE', 'Không liên lạc được'],
]
const CHOICES = [
  ['RETURN_HOME', 'Về nhà'],
  ['STAY_PALLIATIVE', 'Ở lại, chăm sóc giảm nhẹ'],
  ['TRANSFER_TO_HOSPITAL', 'Chuyển bệnh viện'],
]

function Choice({ label, items, value, onChange, invalid, error }) {
  return (
    <Field data-invalid={invalid || undefined}>
      <FieldLabel>{label}</FieldLabel>
      <ToggleGroup
        variant="outline"
        value={value ? [value] : []}
        onValueChange={(n) => onChange(n[0] ?? '')}
        className="flex flex-wrap"
      >
        {items.map(([v, text]) => (
          <ToggleGroupItem key={v} value={v} className="h-11 px-4">
            {text}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      {invalid && <FieldError>{error}</FieldError>}
    </Field>
  )
}

function Text({ id, label, value, onChange, invalid, error, area }) {
  const Control = area ? Textarea : Input
  return (
    <Field data-invalid={invalid || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Control
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={invalid || undefined}
        className={area ? undefined : 'h-11'}
      />
      {invalid && <FieldError>{error}</FieldError>}
    </Field>
  )
}

// Một lệnh: gửi, hiện lỗi 409/422 trong khung; Idempotency-Key giữ nguyên khi thử lại
function useCommand(send, onDone) {
  const [key] = useState(() => crypto.randomUUID())
  const [busy, setBusy] = useState(false)
  const [problem, setProblem] = useState(null)
  const [done, setDone] = useState(false)
  async function run() {
    setBusy(true)
    setProblem(null)
    try {
      await send(key)
      setDone(true)
      onDone?.()
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return { busy, problem, done, run }
}

function RecordFlag({ residentId, onChanged }) {
  const [type, setType] = useState('')
  const [assessment, setAssessment] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const command = useCommand(
    (key) =>
      postResidentsByResidentIdCriticalFlags({
        path: { residentId },
        headers: { 'Idempotency-Key': key },
        body: { type, assessment: assessment.trim() },
      }),
    onChanged,
  )
  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (type && assessment.trim()) command.run()
  }
  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4">
      <Choice
        label="Loại dấu"
        items={FLAG_TYPES}
        value={type}
        onChange={setType}
        invalid={submitted && !type}
        error="Chọn loại dấu"
      />
      <Text
        id="flag-assessment"
        label="Nhận định"
        area
        value={assessment}
        onChange={setAssessment}
        invalid={submitted && !assessment.trim()}
        error="Nhập nhận định"
      />
      <ProblemAlert problem={command.problem} />
      {command.done && <p role="status">Đã ghi dấu nguy kịch và tạo cảnh báo Khẩn cấp.</p>}
      <Button type="submit" className="h-11" disabled={command.busy || command.done}>
        Ghi dấu nguy kịch
      </Button>
    </form>
  )
}

function FlagActions({ flagId, onChanged }) {
  const [removing, setRemoving] = useState(false)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const confirm = useCommand(
    (key) => postCriticalFlagsByIdConfirm({ path: { id: flagId }, headers: { 'Idempotency-Key': key } }),
    onChanged,
  )
  const remove = useCommand(
    (key) =>
      postCriticalFlagsByIdRemove({
        path: { id: flagId },
        headers: { 'Idempotency-Key': key },
        body: { reason: reason.trim() },
      }),
    onChanged,
  )
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-2">
        <Button variant="tonal" className="h-11" disabled={confirm.busy || confirm.done} onClick={confirm.run}>
          Xác nhận dấu tạm
        </Button>
        {!removing && (
          <Button variant="outline" className="h-11" onClick={() => setRemoving(true)}>
            Gỡ dấu
          </Button>
        )}
      </div>
      <ProblemAlert problem={confirm.problem} />
      {confirm.done && <p role="status">Đã xác nhận dấu.</p>}
      {removing && (
        <form
          noValidate
          className="flex flex-col gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            setSubmitted(true)
            if (reason.trim()) remove.run()
          }}
        >
          <Text
            id="flag-remove-reason"
            label="Lý do gỡ dấu"
            area
            value={reason}
            onChange={setReason}
            invalid={submitted && !reason.trim()}
            error="Nhập lý do gỡ dấu"
          />
          <ProblemAlert problem={remove.problem} />
          {remove.done && <p role="status">Đã gỡ dấu. Cảnh báo nguy kịch vẫn phải đóng.</p>}
          <Button type="submit" variant="destructive" className="h-11" disabled={remove.busy || remove.done}>
            Xác nhận gỡ dấu
          </Button>
        </form>
      )}
    </div>
  )
}

function Reconfirmation({ alertId, onChanged }) {
  const [name, setName] = useState('')
  const [channel, setChannel] = useState('')
  const [result, setResult] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const command = useCommand(
    (key) =>
      postAlertsByIdWishReconfirmation({
        path: { id: alertId },
        headers: { 'Idempotency-Key': key },
        body: { contactedPerson: { fullName: name.trim() }, channel, contactedAt: new Date().toISOString(), result },
      }),
    onChanged,
  )
  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(e) => {
        e.preventDefault()
        setSubmitted(true)
        if (name.trim() && channel && result) command.run()
      }}
    >
      <Text
        id="reconfirm-name"
        label="Người đã liên hệ"
        value={name}
        onChange={setName}
        invalid={submitted && !name.trim()}
        error="Nhập họ tên người đã liên hệ"
      />
      <Choice
        label="Cách liên hệ"
        items={CHANNELS}
        value={channel}
        onChange={setChannel}
        invalid={submitted && !channel}
        error="Chọn cách liên hệ"
      />
      <Choice
        label="Kết quả"
        items={RESULTS}
        value={result}
        onChange={setResult}
        invalid={submitted && !result}
        error="Chọn kết quả"
      />
      <ProblemAlert problem={command.problem} />
      {command.done && <p role="status">Đã ghi kết quả xác nhận lại.</p>}
      <Button type="submit" className="h-11" disabled={command.busy || command.done}>
        Ghi kết quả xác nhận lại
      </Button>
    </form>
  )
}

function ExecuteChoice({ alertId, onChanged }) {
  const [choice, setChoice] = useState('')
  const [reason, setReason] = useState('')
  const [unit, setUnit] = useState('')
  const [companion, setCompanion] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const transfer = choice === 'TRANSFER_TO_HOSPITAL'
  const command = useCommand(
    (key) =>
      postAlertsByIdExecuteChoice({
        path: { id: alertId },
        headers: { 'Idempotency-Key': key },
        body: {
          choice,
          reason: reason.trim() || undefined,
          transferOut: transfer
            ? { transferUnit: unit.trim(), transferAt: new Date().toISOString(), companion: companion.trim() }
            : undefined,
        },
      }),
    onChanged,
  )
  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(e) => {
        e.preventDefault()
        setSubmitted(true)
        if (choice && (!transfer || (unit.trim() && companion.trim()))) command.run()
      }}
    >
      <Choice
        label="Lựa chọn thực hiện"
        items={CHOICES}
        value={choice}
        onChange={setChoice}
        invalid={submitted && !choice}
        error="Chọn lựa chọn thực hiện"
      />
      {transfer && (
        <>
          <Text
            id="choice-unit"
            label="Bệnh viện chuyển đến"
            value={unit}
            onChange={setUnit}
            invalid={submitted && !unit.trim()}
            error="Nhập nơi chuyển đến"
          />
          <Text
            id="choice-companion"
            label="Người đi cùng (họ tên, quan hệ hoặc chức danh)"
            value={companion}
            onChange={setCompanion}
            invalid={submitted && !companion.trim()}
            error="Nhập người đi cùng"
          />
        </>
      )}
      <Text
        id="choice-reason"
        label="Lý do (bắt buộc khi không liên lạc được và chưa có nguyện vọng)"
        area
        value={reason}
        onChange={setReason}
      />
      <ProblemAlert problem={command.problem} />
      {command.done && <p role="status">Đã ghi lựa chọn thực hiện.</p>}
      <Button type="submit" className="h-11" disabled={command.busy || command.done}>
        Thực hiện lựa chọn
      </Button>
    </form>
  )
}

function Section({ title, children }) {
  return (
    <Card className="gap-3">
      <CardHeader>
        <h3 className="type-card-title">{title}</h3>
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

export function CriticalCarePanel({ residentId, alert, onChanged }) {
  if (!alert?.criticalFlagId) {
    return (
      <Section title="Ghi dấu nguy kịch">
        <RecordFlag residentId={residentId} onChanged={onChanged} />
      </Section>
    )
  }
  return (
    <div className="flex flex-col gap-3">
      <Section title="Dấu nguy kịch">
        <FlagActions flagId={alert.criticalFlagId} onChanged={onChanged} />
      </Section>
      <Section title="Xác nhận lại nguyện vọng">
        <Reconfirmation alertId={alert.id} onChanged={onChanged} />
      </Section>
      <Section title="Thực hiện lựa chọn">
        <ExecuteChoice alertId={alert.id} onChanged={onChanged} />
      </Section>
    </div>
  )
}
