import { useState } from 'react'
import { postPickupExceptionsByIdByCommand, postResidentsByResidentIdPickupExceptions } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { EXCEPTION_COMMAND_DONE, EXCEPTION_COMMAND_LABELS } from './pickupLabels'

// Ngoại lệ đón (012 T021; FR-024, FR-025, BR-M10-03): người có quyền lệnh nguồn lập cho người ngoài danh sách; người
// đại diện xác nhận qua cổng hoặc Quản lý viện duyệt kèm lý do. api-012 không có lệnh đọc ngoại lệ, nên lệnh trên
// ngoại lệ nhập theo mã (mã trả về khi lập, có trong thông báo gửi Quản lý viện).

function TextField({ id, label, value, onChange, error, show }) {
  return (
    <Field data-invalid={show || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        className="h-11"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={show || undefined}
      />
      {show && <FieldError>{error}</FieldError>}
    </Field>
  )
}

export function PickupExceptionSheet({ residentId, residentName, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [form, setForm] = useState({ fullName: '', idDocumentNumber: '', relationship: '', reason: '' })
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const missing = (name) => submitted && !form[name].trim()
  const set = (name) => (value) => setForm({ ...form, [name]: value })

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(form).some((v) => !v.trim())) return
    setBusy(true)
    try {
      const { data } = await postResidentsByResidentIdPickupExceptions({
        path: { residentId },
        headers: { 'Idempotency-Key': key },
        body: Object.fromEntries(Object.entries(form).map(([k, v]) => [k, v.trim()])),
      })
      onDone(`Đã lập yêu cầu ngoại lệ, mã ${data.id}. Chờ người đại diện xác nhận hoặc Quản lý viện duyệt.`)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Lập yêu cầu ngoại lệ đón</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Người đón không thuộc danh sách được phép đón của {residentName}. Yêu cầu có hiệu lực khi người đại diện xác
          nhận qua cổng hoặc Quản lý viện duyệt.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <TextField
            id="exception-name"
            label="Họ tên người đón"
            value={form.fullName}
            onChange={set('fullName')}
            show={missing('fullName')}
            error="Nhập họ tên người đón"
          />
          <TextField
            id="exception-document"
            label="Số giấy tờ"
            value={form.idDocumentNumber}
            onChange={set('idDocumentNumber')}
            show={missing('idDocumentNumber')}
            error="Nhập số giấy tờ"
          />
          <TextField
            id="exception-relationship"
            label="Quan hệ với người cao tuổi"
            value={form.relationship}
            onChange={set('relationship')}
            show={missing('relationship')}
            error="Nhập quan hệ với người cao tuổi"
          />
          <Field data-invalid={missing('reason') || undefined}>
            <FieldLabel htmlFor="exception-reason">Lý do</FieldLabel>
            <Textarea
              id="exception-reason"
              rows={3}
              maxLength={2000}
              value={form.reason}
              onChange={(e) => set('reason')(e.target.value)}
              aria-invalid={missing('reason') || undefined}
            />
            {missing('reason') && <FieldError>Nhập lý do</FieldError>}
          </Field>
          <ProblemAlert problem={failure} />
          <Button type="submit" className="h-11 self-start" disabled={busy}>
            Gửi yêu cầu ngoại lệ
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

// Lệnh trên ngoại lệ theo mã: Duyệt, Từ chối (Quản lý viện, lý do bắt buộc), Hủy (lý do khi ngoại lệ đã Hiệu lực;
// backend kiểm)
export function ExceptionCommandForm({ commands, onDone }) {
  const [id, setId] = useState('')
  const [command, setCommand] = useState('')
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const errors = {
    id: !/^\d+$/.test(id.trim()),
    command: !command,
    reason: command !== 'cancel' && !reason.trim(),
  }
  const show = (name) => submitted && errors[name]

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      await postPickupExceptionsByIdByCommand({
        path: { id: Number(id.trim()), command },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: reason.trim() ? { reason: reason.trim() } : {},
      })
      onDone(`${EXCEPTION_COMMAND_DONE[command]} yêu cầu ngoại lệ ${id.trim()}`)
      setId('')
      setCommand('')
      setReason('')
      setSubmitted(false)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form aria-labelledby="exception-command-title" onSubmit={submit} noValidate className="flex flex-col gap-4">
          <h2 id="exception-command-title" className="type-card-title">
            Xử lý yêu cầu ngoại lệ
          </h2>
          <Field data-invalid={show('id') || undefined}>
            <FieldLabel htmlFor="exception-command-id">Mã yêu cầu ngoại lệ</FieldLabel>
            <Input
              id="exception-command-id"
              inputMode="numeric"
              className="h-11 max-w-60"
              value={id}
              onChange={(e) => setId(e.target.value)}
              aria-invalid={show('id') || undefined}
            />
            {show('id') && <FieldError>Nhập mã yêu cầu ngoại lệ</FieldError>}
          </Field>
          <fieldset className="flex flex-col gap-1">
            <legend className="type-label">Lệnh</legend>
            <div className="flex flex-wrap gap-x-6">
              {commands.map((c) => (
                <label key={c} htmlFor={`exception-command-${c}`} className="flex min-h-11 items-center gap-2">
                  <input
                    id={`exception-command-${c}`}
                    type="radio"
                    name="exception-command"
                    value={c}
                    checked={command === c}
                    onChange={() => setCommand(c)}
                    className="size-4"
                  />
                  {EXCEPTION_COMMAND_LABELS[c]}
                </label>
              ))}
            </div>
            {show('command') && <p className="type-body-sm text-destructive">Chọn lệnh</p>}
          </fieldset>
          <Field data-invalid={show('reason') || undefined}>
            <FieldLabel htmlFor="exception-command-reason">Lý do</FieldLabel>
            <Textarea
              id="exception-command-reason"
              rows={2}
              maxLength={2000}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              aria-invalid={show('reason') || undefined}
            />
            {show('reason') && <FieldError>Nhập lý do</FieldError>}
          </Field>
          <ProblemAlert problem={failure} />
          <Button type="submit" variant="outline" className="h-11 self-start" disabled={busy}>
            Thực hiện lệnh
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
