import { useState } from 'react'
import { postRightChangeRequests } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { RELATIONSHIP_LABELS, RIGHT_LABELS, RIGHT_ORDER } from '../relationshipLabels'
import { REQUEST_HINTS } from './requestLabels'

// Lập yêu cầu thay đổi quyền tại quầy (012 T017; FR-013 → FR-018, FR-005): Hành chính lập theo đề nghị của gia đình.
// Có bản ký của người đại diện thì yêu cầu có hiệu lực ngay (FR-015 b); không thì Chờ xác nhận. Người đón mới chưa có
// hồ sơ thì nhập kèm, backend lập hồ sơ và quan hệ. Không chọn sẵn giá trị nào.

const TYPES = {
  RIGHT: 'Đổi một quyền',
  SELF_RETURN: 'Dấu được tự về',
  SET_REPRESENTATIVE: 'Đặt thêm người đại diện',
  REMOVE_REPRESENTATIVE: 'Thôi người đại diện',
}

const NEW_PICKER = 'new'
const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

function Choice({ name, value, current, onChange, children }) {
  const id = `${name}-${value}`
  return (
    <label htmlFor={id} className="flex min-h-11 items-center gap-2">
      <input
        id={id}
        type="radio"
        name={name}
        value={value}
        checked={current === value}
        onChange={() => onChange(value)}
        className="size-4"
      />
      {children}
    </label>
  )
}

function TextField({ id, label, value, onChange, invalid, error }) {
  return (
    <Field data-invalid={invalid || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        className="h-11"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={invalid || undefined}
      />
      {invalid && <FieldError>{error}</FieldError>}
    </Field>
  )
}

export function CreateRightChangeRequestSheet({ open, residentId, residentName, relationships, onClose, onDone }) {
  const active = relationships.filter((r) => r.status === 'ACTIVE')
  const representatives = active.filter((r) => r.representative)
  const [key] = useState(() => crypto.randomUUID())
  const [type, setType] = useState('RIGHT')
  const [person, setPerson] = useState('')
  const [right, setRight] = useState('')
  const [value, setValue] = useState('')
  const [picker, setPicker] = useState({
    fullName: '',
    phoneNumber: '',
    idDocumentType: '',
    idDocumentNumber: '',
    relationship: '',
  })
  const [signed, setSigned] = useState(false)
  const [signer, setSigner] = useState('')
  const [file, setFile] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const newPicker = type === 'RIGHT' && person === NEW_PICKER
  const representativeChange = type === 'SET_REPRESENTATIVE' || type === 'REMOVE_REPRESENTATIVE'
  const people =
    type === 'SET_REPRESENTATIVE'
      ? active.filter((r) => !r.representative)
      : type === 'REMOVE_REPRESENTATIVE'
        ? representatives
        : active
  const pickerMissing = newPicker && Object.values(picker).some((v) => !v.trim())
  const errors = {
    person: type !== 'SELF_RETURN' && !person,
    right: type === 'RIGHT' && !newPicker && !right,
    value: (type === 'RIGHT' && !newPicker && !value) || (type === 'SELF_RETURN' && !value),
    picker: pickerMissing,
    signer: signed && !signer,
    file: (signed || representativeChange) && !file,
    reason: !reason.trim(),
  }

  function changeType(next) {
    setType(next)
    setPerson('')
    setRight('')
    setValue('')
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    try {
      const body = { residentId, type }
      if (newPicker) {
        body.right = 'PICKUP_ALLOWED'
        body.newValue = true
        body.newPicker = Object.fromEntries(Object.entries(picker).map(([k, v]) => [k, v.trim()]))
      } else if (type !== 'SELF_RETURN') {
        body.relationshipId = Number(person)
      }
      if (type === 'RIGHT' && !newPicker) {
        body.right = right
        body.newValue = value === 'on'
      }
      if (type === 'SELF_RETURN') body.newValue = value === 'on'
      body.withSignedForm = signed
      if (signed) body.signedByFamilyMemberId = Number(signer)
      if (signed || representativeChange) body.evidenceFileId = await uploadFile(file)
      body.reason = reason.trim()
      await postRightChangeRequests({ headers: { 'Idempotency-Key': key }, body })
      onDone('Đã lập yêu cầu')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const show = (name) => submitted && errors[name]

  return (
    <Sheet open={open} onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Lập yêu cầu thay đổi quyền</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Yêu cầu cho {residentName} theo đề nghị của gia đình tại quầy. Có bản ký của người đại diện thì có hiệu lực
          ngay; không thì chờ người đại diện xác nhận hoặc Quản lý viện duyệt.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <Field>
            <FieldLabel htmlFor="request-type">Loại yêu cầu</FieldLabel>
            <select id="request-type" value={type} onChange={(e) => changeType(e.target.value)} className={SELECT}>
              {Object.entries(TYPES)
                .filter(([t]) => t !== 'SET_REPRESENTATIVE' || representatives.length > 0)
                .map(([t, label]) => (
                  <option key={t} value={t}>
                    {label}
                  </option>
                ))}
            </select>
          </Field>

          {type !== 'SELF_RETURN' && (
            <Field data-invalid={show('person') || undefined}>
              <FieldLabel htmlFor="request-person">Người thân</FieldLabel>
              <select
                id="request-person"
                value={person}
                onChange={(e) => setPerson(e.target.value)}
                aria-invalid={show('person') || undefined}
                className={SELECT}
              >
                <option value="">Chọn người thân</option>
                {people.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.familyMember.fullName}
                  </option>
                ))}
                {type === 'RIGHT' && <option value={NEW_PICKER}>Người đón mới, chưa có hồ sơ</option>}
              </select>
              {show('person') && <FieldError>Chọn người thân</FieldError>}
            </Field>
          )}

          {newPicker && (
            <fieldset className="flex flex-col gap-3 rounded-lg border p-4">
              <legend className="type-label px-1">Người đón mới, được bật quyền Được phép đón</legend>
              <TextField
                id="picker-name"
                label="Họ tên người đón"
                value={picker.fullName}
                onChange={(v) => setPicker({ ...picker, fullName: v })}
                invalid={submitted && !picker.fullName.trim()}
                error="Nhập họ tên"
              />
              <TextField
                id="picker-phone"
                label="Số điện thoại"
                value={picker.phoneNumber}
                onChange={(v) => setPicker({ ...picker, phoneNumber: v })}
                invalid={submitted && !picker.phoneNumber.trim()}
                error="Nhập số điện thoại"
              />
              <TextField
                id="picker-doc-type"
                label="Loại giấy tờ"
                value={picker.idDocumentType}
                onChange={(v) => setPicker({ ...picker, idDocumentType: v })}
                invalid={submitted && !picker.idDocumentType.trim()}
                error="Nhập loại giấy tờ"
              />
              <TextField
                id="picker-doc-number"
                label="Số giấy tờ"
                value={picker.idDocumentNumber}
                onChange={(v) => setPicker({ ...picker, idDocumentNumber: v })}
                invalid={submitted && !picker.idDocumentNumber.trim()}
                error="Nhập số giấy tờ"
              />
              <Field data-invalid={(submitted && !picker.relationship) || undefined}>
                <FieldLabel htmlFor="picker-relationship">Quan hệ với người cao tuổi</FieldLabel>
                <select
                  id="picker-relationship"
                  value={picker.relationship}
                  onChange={(e) => setPicker({ ...picker, relationship: e.target.value })}
                  aria-invalid={(submitted && !picker.relationship) || undefined}
                  className={SELECT}
                >
                  <option value="">Chọn quan hệ</option>
                  {Object.entries(RELATIONSHIP_LABELS).map(([k, label]) => (
                    <option key={k} value={k}>
                      {label}
                    </option>
                  ))}
                </select>
                {submitted && !picker.relationship && <FieldError>Chọn quan hệ</FieldError>}
              </Field>
            </fieldset>
          )}

          {type === 'RIGHT' && !newPicker && (
            <Field data-invalid={show('right') || undefined}>
              <FieldLabel htmlFor="request-right">Quyền</FieldLabel>
              <select
                id="request-right"
                value={right}
                onChange={(e) => setRight(e.target.value)}
                aria-invalid={show('right') || undefined}
                className={SELECT}
              >
                <option value="">Chọn quyền</option>
                {RIGHT_ORDER.map((r) => (
                  <option key={r} value={r}>
                    {RIGHT_LABELS[r]}
                  </option>
                ))}
              </select>
              {show('right') && <FieldError>Chọn quyền</FieldError>}
            </Field>
          )}

          {((type === 'RIGHT' && !newPicker) || type === 'SELF_RETURN') && (
            <fieldset className="flex flex-col gap-1" aria-invalid={show('value') || undefined}>
              <legend className="type-label">Giá trị mới</legend>
              <div className="flex gap-6">
                <Choice name="request-value" value="on" current={value} onChange={setValue}>
                  Bật
                </Choice>
                <Choice name="request-value" value="off" current={value} onChange={setValue}>
                  Tắt
                </Choice>
              </div>
              {show('value') && <p className="type-body-sm text-destructive">Chọn bật hoặc tắt</p>}
            </fieldset>
          )}

          {representatives.length > 0 && (
            <Field orientation="horizontal">
              <Checkbox id="request-signed" checked={signed} onCheckedChange={(v) => setSigned(!!v)} />
              <FieldLabel htmlFor="request-signed">Có bản ký của người đại diện</FieldLabel>
            </Field>
          )}
          {signed && (
            <Field data-invalid={show('signer') || undefined}>
              <FieldLabel htmlFor="request-form-signer">Người đại diện ký</FieldLabel>
              <select
                id="request-form-signer"
                value={signer}
                onChange={(e) => setSigner(e.target.value)}
                aria-invalid={show('signer') || undefined}
                className={SELECT}
              >
                <option value="">Chọn người đại diện</option>
                {representatives.map((r) => (
                  <option key={r.id} value={r.familyMember.id}>
                    {r.familyMember.fullName}
                  </option>
                ))}
              </select>
              {show('signer') && <FieldError>Chọn người đại diện đã ký</FieldError>}
            </Field>
          )}
          {(signed || representativeChange) && (
            <Field data-invalid={show('file') || undefined}>
              <FieldLabel htmlFor="request-file">
                {signed ? 'Bản scan bản ký' : 'Bằng chứng (văn bản ủy quyền, giấy tờ quan hệ)'}
              </FieldLabel>
              <input
                id="request-file"
                type="file"
                accept="application/pdf,image/jpeg,image/png"
                className="type-body-sm h-11 rounded-lg border px-3 py-2"
                aria-invalid={show('file') || undefined}
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
              {show('file') && <FieldError>Chọn tệp</FieldError>}
            </Field>
          )}

          <Field data-invalid={show('reason') || undefined}>
            <FieldLabel htmlFor="create-request-reason">Lý do</FieldLabel>
            <Textarea
              id="create-request-reason"
              rows={3}
              maxLength={2000}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              aria-invalid={show('reason') || undefined}
            />
            {show('reason') && <FieldError>Nhập lý do</FieldError>}
          </Field>

          <ProblemAlert problem={failure}>
            {failure && REQUEST_HINTS[failure.errorCode] && <p>{REQUEST_HINTS[failure.errorCode]}</p>}
          </ProblemAlert>
          <Button type="submit" className="h-11 self-start" disabled={busy}>
            Lập yêu cầu thay đổi quyền
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}
