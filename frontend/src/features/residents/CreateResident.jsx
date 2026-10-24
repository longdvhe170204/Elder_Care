import { useState } from 'react'
import { ApiError, postResidents } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { GENDER_LABELS, ID_DOCUMENT_LABELS, STATUS_LABELS } from './residentLabels'

// Tạo hồ sơ người cao tuổi (001 T022; US1 #1 → #4, #10; FR-001 → FR-005; hợp đồng POST /residents).
// Idempotency-Key giữ suốt lần nhập để bấm lại không tạo hai hồ sơ; khi xác nhận "không trùng" nội dung đổi
// (thêm confirmNotDuplicate) nên lấy khóa mới.

const SELECT_CLASS =
  'h-10 w-full rounded-lg border border-input bg-transparent px-3 text-base outline-none focus-visible:ring-3 focus-visible:ring-ring/50'

function blank(value) {
  const text = value.trim()
  return text === '' ? undefined : text
}

export function CreateResident({ onCreated, onClose }) {
  const [form, setForm] = useState({
    fullName: '',
    dateOfBirth: '',
    gender: '',
    idDocumentType: '',
    idDocumentNumber: '',
    permanentAddress: '',
    contactAddress: '',
    phoneNumber: '',
    email: '',
    specialNotes: '',
  })
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [candidates, setCandidates] = useState([])
  const [saving, setSaving] = useState(false)
  const [created, setCreated] = useState(null)
  const [key, setKey] = useState(() => crypto.randomUUID())

  const set = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))

  function validate() {
    const next = {}
    if (!form.fullName.trim()) next.fullName = 'Nhập họ tên'
    if (!form.dateOfBirth) next.dateOfBirth = 'Chọn ngày sinh'
    else if (form.dateOfBirth > new Date().toISOString().slice(0, 10)) next.dateOfBirth = 'Ngày sinh không ở tương lai'
    if (!form.gender) next.gender = 'Chọn giới tính'
    if (form.idDocumentType && !form.idDocumentNumber.trim()) next.idDocumentNumber = 'Nhập số giấy tờ'
    if (!form.idDocumentType && form.idDocumentNumber.trim()) next.idDocumentType = 'Chọn loại giấy tờ'
    return next
  }

  async function save(confirmNotDuplicate, idempotencyKey) {
    setFailure(null)
    const next = validate()
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setSaving(true)
    try {
      const phoneNumber = blank(form.phoneNumber)
      const email = blank(form.email)
      const { data } = await postResidents({
        headers: { 'Idempotency-Key': idempotencyKey },
        body: {
          fullName: form.fullName.trim(),
          dateOfBirth: form.dateOfBirth,
          gender: form.gender,
          idDocumentType: form.idDocumentType || undefined,
          idDocumentNumber: blank(form.idDocumentNumber),
          permanentAddress: blank(form.permanentAddress),
          contactAddress: blank(form.contactAddress),
          contact: phoneNumber || email ? { phoneNumber, email } : undefined,
          specialNotes: blank(form.specialNotes),
          ...(confirmNotDuplicate && { confirmNotDuplicate: true }),
        },
      })
      setCandidates([])
      setCreated(data)
      onCreated?.(data)
    } catch (error) {
      const p = problemOf(error)
      const body = error instanceof ApiError ? error.problem : null
      if (body?.errorCode === 'POSSIBLE_DUPLICATE_PROFILE') {
        setCandidates(body.candidates ?? [])
      } else if (body?.errorCode === 'CITIZEN_ID_IN_USE') {
        setErrors({ idDocumentNumber: `CCCD đang dùng ở hồ sơ ${body.profileCode ?? 'khác'}` })
      } else if (error instanceof ApiError && error.status === 422) {
        setFailure({ ...p, title: p.title })
      } else {
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  function submit(event) {
    event.preventDefault()
    setCandidates([])
    save(false, key)
  }

  function confirmNotDuplicate() {
    const nextKey = crypto.randomUUID()
    setKey(nextKey)
    save(true, nextKey)
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">Tạo hồ sơ người cao tuổi</SheetTitle>
        <SheetDescription>
          Hồ sơ mới ở trạng thái Đang tiếp nhận. Họ tên, ngày sinh, giới tính là bắt buộc.
        </SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6">
        {created ? (
          <div className="flex flex-col gap-3" role="status">
            <p className="type-body-sm">
              Đã tạo hồ sơ <strong>{created.profile.profileCode}</strong> cho {created.profile.personalInfo.fullName}.
            </p>
            {created.profile.previousProfileId && (
              <p className="type-body-sm text-muted-foreground">
                Hồ sơ này được liên kết với hồ sơ trước của cùng người.
              </p>
            )}
            {created.warnings.map((w) => (
              <p key={w.code} className="type-body-sm text-destructive">
                {w.description}
              </p>
            ))}
            <Button variant="outline" className="self-start" onClick={onClose}>
              Xong
            </Button>
          </div>
        ) : (
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            <FieldGroup>
              <Field data-invalid={!!errors.fullName || undefined}>
                <FieldLabel htmlFor="resident-name">Họ tên</FieldLabel>
                <Input
                  id="resident-name"
                  value={form.fullName}
                  onChange={set('fullName')}
                  className="h-10"
                  aria-invalid={!!errors.fullName || undefined}
                />
                {errors.fullName && <FieldError>{errors.fullName}</FieldError>}
              </Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field data-invalid={!!errors.dateOfBirth || undefined}>
                  <FieldLabel htmlFor="resident-dob">Ngày sinh</FieldLabel>
                  <Input
                    id="resident-dob"
                    type="date"
                    value={form.dateOfBirth}
                    onChange={set('dateOfBirth')}
                    className="h-10"
                    aria-invalid={!!errors.dateOfBirth || undefined}
                  />
                  {errors.dateOfBirth && <FieldError>{errors.dateOfBirth}</FieldError>}
                </Field>
                <Field data-invalid={!!errors.gender || undefined}>
                  <FieldLabel htmlFor="resident-gender">Giới tính</FieldLabel>
                  <select
                    id="resident-gender"
                    value={form.gender}
                    onChange={set('gender')}
                    className={SELECT_CLASS}
                    aria-invalid={!!errors.gender || undefined}
                  >
                    <option value="">Chọn</option>
                    {Object.entries(GENDER_LABELS).map(([code, label]) => (
                      <option key={code} value={code}>
                        {label}
                      </option>
                    ))}
                  </select>
                  {errors.gender && <FieldError>{errors.gender}</FieldError>}
                </Field>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field data-invalid={!!errors.idDocumentType || undefined}>
                  <FieldLabel htmlFor="resident-doc-type">Loại giấy tờ</FieldLabel>
                  <select
                    id="resident-doc-type"
                    value={form.idDocumentType}
                    onChange={set('idDocumentType')}
                    className={SELECT_CLASS}
                  >
                    <option value="">Không có</option>
                    {Object.entries(ID_DOCUMENT_LABELS).map(([code, label]) => (
                      <option key={code} value={code}>
                        {label}
                      </option>
                    ))}
                  </select>
                  {errors.idDocumentType && <FieldError>{errors.idDocumentType}</FieldError>}
                </Field>
                <Field data-invalid={!!errors.idDocumentNumber || undefined}>
                  <FieldLabel htmlFor="resident-doc-number">Số giấy tờ</FieldLabel>
                  <Input
                    id="resident-doc-number"
                    value={form.idDocumentNumber}
                    onChange={set('idDocumentNumber')}
                    className="h-10 tabular-nums"
                    aria-invalid={!!errors.idDocumentNumber || undefined}
                  />
                  {errors.idDocumentNumber ? (
                    <FieldError>{errors.idDocumentNumber}</FieldError>
                  ) : (
                    <FieldDescription>CCCD không được trùng hồ sơ đang lưu.</FieldDescription>
                  )}
                </Field>
              </div>
              <Field>
                <FieldLabel htmlFor="resident-permanent">Địa chỉ thường trú</FieldLabel>
                <Input
                  id="resident-permanent"
                  value={form.permanentAddress}
                  onChange={set('permanentAddress')}
                  className="h-10"
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="resident-contact-address">Địa chỉ liên hệ</FieldLabel>
                <Input
                  id="resident-contact-address"
                  value={form.contactAddress}
                  onChange={set('contactAddress')}
                  className="h-10"
                />
              </Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor="resident-phone">Số điện thoại</FieldLabel>
                  <Input
                    id="resident-phone"
                    inputMode="tel"
                    value={form.phoneNumber}
                    onChange={set('phoneNumber')}
                    className="h-10"
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor="resident-email">Thư điện tử</FieldLabel>
                  <Input id="resident-email" type="email" value={form.email} onChange={set('email')} className="h-10" />
                </Field>
              </div>
              <Field>
                <FieldLabel htmlFor="resident-notes">Thông tin đặc biệt</FieldLabel>
                <Textarea id="resident-notes" value={form.specialNotes} onChange={set('specialNotes')} />
              </Field>
            </FieldGroup>

            {candidates.length > 0 && (
              <div className="flex flex-col gap-3 rounded-lg border border-destructive/40 p-3" role="alert">
                <p className="type-body-sm font-medium">
                  Đã có hồ sơ cùng họ tên và ngày sinh. Kiểm tra trước khi tạo thêm:
                </p>
                <ul className="flex flex-col gap-1 type-body-sm">
                  {candidates.map((c) => (
                    <li key={c.id}>
                      {c.profileCode} · {c.fullName} · {STATUS_LABELS[c.status] ?? c.status}
                    </li>
                  ))}
                </ul>
                <Button
                  type="button"
                  variant="outline"
                  className="h-11 self-start"
                  disabled={saving}
                  onClick={confirmNotDuplicate}
                >
                  Xác nhận không trùng, vẫn tạo hồ sơ
                </Button>
              </div>
            )}

            <ProblemAlert problem={failure} />

            <div className="flex flex-wrap gap-3">
              <Button type="submit" className="h-11" disabled={saving}>
                Tạo hồ sơ
              </Button>
              <Button type="button" variant="outline" className="h-11" onClick={onClose}>
                Hủy
              </Button>
            </div>
          </form>
        )}
      </div>
    </>
  )
}
