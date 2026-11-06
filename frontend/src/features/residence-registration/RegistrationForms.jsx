import { useRef, useState } from 'react'
import { postResidenceRegistrationsByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { CHANNEL_LABELS, COMMANDS } from './registrationLabels'

// Form lệnh trên khai báo (018 T008, T010, T014; api-018 POST /residence-registrations/{id}/{command}; bố cục K4, K7).
// Mỗi form kiểm trước các trường bắt buộc rồi gửi; 409 và 422 hiện bằng Alert trong khung (DESIGN.md 7.4). Idempotency-Key
// đổi sau mỗi lần lỗi để lần gửi sau (đã sửa nội dung) là lệnh khác.

const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

const todayIso = () => new Date().toLocaleDateString('sv-SE')

function useCommand(registration, onDone) {
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const key = useRef(crypto.randomUUID())

  async function run(command, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await postResidenceRegistrationsByIdByCommand({
        path: { id: registration.id, command },
        headers: { 'Idempotency-Key': key.current },
        body,
      })
      onDone(message)
    } catch (error) {
      key.current = crypto.randomUUID()
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return { busy, failure, run }
}

function FormShell({ title, submitLabel, busy, failure, onSubmit, onCancel, children }) {
  return (
    <form
      aria-label={title}
      noValidate
      className="flex flex-col gap-4 rounded-lg border bg-card p-4"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <h4 className="type-label">{title}</h4>
      <FieldGroup>{children}</FieldGroup>
      <ProblemAlert problem={failure} />
      <div className="flex flex-wrap gap-2">
        <Button type="submit" className="h-11" disabled={busy}>
          {submitLabel}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}

function TextField({ id, label, value, onChange, error, type = 'text' }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        type={type}
        className="h-11"
        value={value}
        aria-invalid={!!error || undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

function EvidenceField({ id, label, files, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <input
        id={id}
        type="file"
        accept="application/pdf,image/jpeg,image/png"
        aria-invalid={!!error || undefined}
        className="type-body-sm h-11 rounded-lg border px-3 py-2"
        onChange={(event) => onChange(Array.from(event.target.files ?? []))}
      />
      {files.length > 0 && (
        <span className="type-body-sm text-muted-foreground">Đã chọn: {files.map((f) => f.name).join(', ')}</span>
      )}
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

function ConfirmLocalForm({ registration, onDone, onCancel }) {
  const { busy, failure, run } = useCommand(registration, onDone)
  const [basisNote, setBasisNote] = useState('')
  const [error, setError] = useState('')

  function submit() {
    if (!basisNote.trim()) {
      setError('Nhập căn cứ xác nhận thường trú cùng xã/phường.')
      return
    }
    setError('')
    run(COMMANDS.CONFIRM_LOCAL, { basisNote: basisNote.trim() }, 'Đã xác nhận thường trú cùng xã/phường.')
  }

  return (
    <FormShell
      title="Xác nhận thường trú cùng xã/phường"
      submitLabel="Xác nhận thường trú"
      busy={busy}
      failure={failure}
      onSubmit={submit}
      onCancel={onCancel}
    >
      <Field data-invalid={!!error || undefined}>
        <FieldLabel htmlFor="registration-basis">Căn cứ</FieldLabel>
        <Textarea
          id="registration-basis"
          value={basisNote}
          aria-invalid={!!error || undefined}
          onChange={(event) => setBasisNote(event.target.value)}
        />
        {error && <FieldError>{error}</FieldError>}
      </Field>
    </FormShell>
  )
}

function SubmittedForm({ registration, onDone, onCancel }) {
  const { busy, failure, run } = useCommand(registration, onDone)
  const [v, setV] = useState({ submittedDate: todayIso(), channel: '', receivingAuthority: '', dossierCode: '' })
  const [errors, setErrors] = useState({})
  const set = (name) => (value) => setV((s) => ({ ...s, [name]: value }))

  function submit() {
    const found = {}
    if (!v.submittedDate) found.submittedDate = 'Nhập ngày nộp.'
    if (!v.channel) found.channel = 'Chọn kênh nộp.'
    if (!v.receivingAuthority.trim()) found.receivingAuthority = 'Nhập cơ quan tiếp nhận.'
    setErrors(found)
    if (Object.keys(found).length > 0) return
    run(
      COMMANDS.SUBMITTED,
      {
        submittedDate: v.submittedDate,
        channel: v.channel,
        receivingAuthority: v.receivingAuthority.trim(),
        ...(v.dossierCode.trim() && { dossierCode: v.dossierCode.trim() }),
      },
      'Đã ghi đã nộp.',
    )
  }

  return (
    <FormShell
      title="Ghi đã nộp"
      submitLabel="Lưu đã nộp"
      busy={busy}
      failure={failure}
      onSubmit={submit}
      onCancel={onCancel}
    >
      <TextField
        id="registration-submitted-date"
        type="date"
        label="Ngày nộp"
        value={v.submittedDate}
        onChange={set('submittedDate')}
        error={errors.submittedDate}
      />
      <Field data-invalid={!!errors.channel || undefined}>
        <FieldLabel htmlFor="registration-channel">Kênh nộp</FieldLabel>
        <select
          id="registration-channel"
          className={SELECT}
          value={v.channel}
          aria-invalid={!!errors.channel || undefined}
          onChange={(event) => set('channel')(event.target.value)}
        >
          <option value="">Chọn…</option>
          {Object.entries(CHANNEL_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        {errors.channel && <FieldError>{errors.channel}</FieldError>}
      </Field>
      <TextField
        id="registration-authority"
        label="Cơ quan tiếp nhận"
        value={v.receivingAuthority}
        onChange={set('receivingAuthority')}
        error={errors.receivingAuthority}
      />
      <TextField id="registration-dossier" label="Mã hồ sơ (không bắt buộc)" value={v.dossierCode} onChange={set('dossierCode')} />
    </FormShell>
  )
}

function ResultForm({ registration, onDone, onCancel }) {
  const { busy, failure, run } = useCommand(registration, onDone)
  const options = [
    { value: COMMANDS.ACCEPTED, label: 'Được chấp nhận' },
    { value: COMMANDS.REJECTED, label: 'Bị từ chối' },
  ].filter((o) => (registration.allowedCommands ?? []).includes(o.value))
  const [choice, setChoice] = useState('')
  const [evidence, setEvidence] = useState([])
  const [expiryDate, setExpiryDate] = useState('')
  const [reason, setReason] = useState('')
  const [errors, setErrors] = useState({})
  const temporary = registration.type === 'TEMPORARY_RESIDENCE_REGISTRATION'

  async function submit() {
    const found = {}
    if (!choice) found.choice = 'Chọn kết quả.'
    if (choice === COMMANDS.ACCEPTED) {
      if (evidence.length === 0) found.evidence = 'Chọn bằng chứng được chấp nhận.'
      if (temporary && !expiryDate) found.expiryDate = 'Nhập ngày hết hạn.'
    }
    if (choice === COMMANDS.REJECTED && !reason.trim()) found.reason = 'Nhập lý do bị từ chối.'
    setErrors(found)
    if (Object.keys(found).length > 0) return
    if (choice === COMMANDS.REJECTED) {
      run(COMMANDS.REJECTED, { reason: reason.trim() }, 'Đã ghi bị từ chối.')
      return
    }
    try {
      const evidenceFileId = await uploadFile(evidence[0])
      run(
        COMMANDS.ACCEPTED,
        { evidenceFileId, ...(temporary && { expiryDate }) },
        'Đã ghi được chấp nhận.',
      )
    } catch (error) {
      setErrors({ evidence: problemOf(error).detail || 'Không tải được tệp bằng chứng.' })
    }
  }

  return (
    <FormShell
      title="Ghi kết quả"
      submitLabel="Lưu kết quả"
      busy={busy}
      failure={failure}
      onSubmit={submit}
      onCancel={onCancel}
    >
      <Field data-invalid={!!errors.choice || undefined}>
        <FieldLabel>Kết quả</FieldLabel>
        <ToggleGroup value={choice ? [choice] : []} onValueChange={(next) => setChoice(next[0] ?? '')}>
          {options.map((o) => (
            <ToggleGroupItem key={o.value} value={o.value} variant="outline" className="h-11">
              {o.label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        {errors.choice && <FieldError>{errors.choice}</FieldError>}
      </Field>
      {choice === COMMANDS.ACCEPTED && (
        <>
          <EvidenceField
            id="registration-result-evidence"
            label="Bằng chứng được chấp nhận"
            files={evidence}
            onChange={setEvidence}
            error={errors.evidence}
          />
          {temporary && (
            <TextField
              id="registration-expiry"
              type="date"
              label="Ngày hết hạn"
              value={expiryDate}
              onChange={setExpiryDate}
              error={errors.expiryDate}
            />
          )}
        </>
      )}
      {choice === COMMANDS.REJECTED && (
        <Field data-invalid={!!errors.reason || undefined}>
          <FieldLabel htmlFor="registration-reason">Lý do bị từ chối</FieldLabel>
          <Textarea
            id="registration-reason"
            value={reason}
            aria-invalid={!!errors.reason || undefined}
            onChange={(event) => setReason(event.target.value)}
          />
          {errors.reason && <FieldError>{errors.reason}</FieldError>}
        </Field>
      )}
    </FormShell>
  )
}

function DeregisteredForm({ registration, onDone, onCancel }) {
  const { busy, failure, run } = useCommand(registration, onDone)
  const [evidence, setEvidence] = useState([])
  const [error, setError] = useState('')

  async function submit() {
    if (evidence.length === 0) {
      setError('Chọn bằng chứng đã khai báo xóa.')
      return
    }
    setError('')
    try {
      const evidenceFileId = await uploadFile(evidence[0])
      run(COMMANDS.DEREGISTERED, { evidenceFileId }, 'Đã ghi đã khai báo xóa.')
    } catch (uploadError) {
      setError(problemOf(uploadError).detail || 'Không tải được tệp bằng chứng.')
    }
  }

  return (
    <FormShell
      title="Ghi đã khai báo xóa"
      submitLabel="Ghi đã khai báo xóa"
      busy={busy}
      failure={failure}
      onSubmit={submit}
      onCancel={onCancel}
    >
      <p className="type-body-sm text-muted-foreground">
        Một lần ghi áp cho mọi khai báo Đã xác nhận, Hết hạn của người này.
      </p>
      <EvidenceField
        id="registration-deregister-evidence"
        label="Bằng chứng đã khai báo xóa"
        files={evidence}
        onChange={setEvidence}
        error={error}
      />
    </FormShell>
  )
}

const FORMS = {
  'confirm-local': ConfirmLocalForm,
  submitted: SubmittedForm,
  result: ResultForm,
  deregistered: DeregisteredForm,
}

// form: confirm-local | submitted | result | deregistered; onDone(thông báo) khi lệnh thành công
export function RegistrationForm({ form, registration, onDone, onCancel }) {
  const Form = FORMS[form]
  return <Form registration={registration} onDone={onDone} onCancel={onCancel} />
}
