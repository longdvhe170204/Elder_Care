import { useState } from 'react'
import { putMeContact } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// Số điện thoại, email của chính mình (api-002 PUT /me/contact; A duyệt 2026-11-09). Số điện thoại dùng nhận mã tự đặt
// lại mật khẩu (BR-M15-25) nên lệnh bắt buộc mật khẩu hiện tại; đổi số thì backend gửi tin báo về số cũ. Lỗi báo tại ô
// (CONTACT_INVALID theo unmetConditions[].code, INVALID_VALUE ở ô mật khẩu); giá trị đã gõ giữ lại (DESIGN.md 9.2 điểm 4).

const FIELDS = [
  ['phoneNumber', 'Số điện thoại', 'tel', 'tel'],
  ['email', 'Email', 'email', 'email'],
]

function ContactForm({ initial, onSaved, onCancel }) {
  const [values, setValues] = useState({
    phoneNumber: initial.phoneNumber ?? '',
    email: initial.email ?? '',
    password: '',
  })
  const [errors, setErrors] = useState({})
  const [problem, setProblem] = useState(null)
  const [saving, setSaving] = useState(false)
  const set = (key) => (event) => setValues((v) => ({ ...v, [key]: event.target.value }))

  async function submit(event) {
    event.preventDefault()
    setProblem(null)
    if (!values.password) {
      setErrors({ password: 'Nhập mật khẩu hiện tại' })
      return
    }
    setErrors({})
    setSaving(true)
    try {
      const { data } = await putMeContact({
        body: { currentPassword: values.password, phoneNumber: values.phoneNumber, email: values.email },
      })
      onSaved(data)
    } catch (error) {
      const p = problemOf(error)
      if (p.errorCode === 'CONTACT_INVALID' && p.unmetConditions.length > 0) {
        setErrors(Object.fromEntries(p.unmetConditions.map((c) => [c.code, c.description])))
      } else if (p.errorCode === 'INVALID_VALUE') {
        setErrors({ password: p.detail || 'Mật khẩu hiện tại không đúng' })
      } else {
        setProblem(p)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4">
      <FieldGroup>
        {FIELDS.map(([key, label, type, autoComplete]) => (
          <Field key={key} data-invalid={errors[key] ? true : undefined}>
            <FieldLabel htmlFor={`my-${key}`}>{label}</FieldLabel>
            <Input
              id={`my-${key}`}
              type={type}
              autoComplete={autoComplete}
              value={values[key]}
              onChange={set(key)}
              aria-invalid={errors[key] ? true : undefined}
              className="h-11"
            />
            {errors[key] && <FieldError>{errors[key]}</FieldError>}
          </Field>
        ))}
        <Field data-invalid={errors.password ? true : undefined}>
          <FieldLabel htmlFor="my-current-password">Mật khẩu hiện tại (bắt buộc)</FieldLabel>
          <Input
            id="my-current-password"
            type="password"
            autoComplete="current-password"
            value={values.password}
            onChange={set('password')}
            aria-invalid={errors.password ? true : undefined}
            className="h-11"
          />
          {errors.password ? (
            <FieldError>{errors.password}</FieldError>
          ) : (
            <FieldDescription>
              Số điện thoại này dùng nhận mã đặt lại mật khẩu; đổi số sẽ gửi tin báo về số cũ.
            </FieldDescription>
          )}
        </Field>
      </FieldGroup>
      <ProblemAlert problem={problem} />
      <div className="flex flex-wrap gap-2">
        <Button type="submit" size="lg" disabled={saving}>
          Lưu liên hệ
        </Button>
        <Button type="button" size="lg" variant="outline" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}

// Hai dòng liên hệ trong khối Thông tin tài khoản, nút "Sửa liên hệ" mở form ngay dưới
export function MyContact({ initial, InfoRow }) {
  const [contact, setContact] = useState({ phoneNumber: initial.phoneNumber, email: initial.email })
  const [editing, setEditing] = useState(false)
  const [message, setMessage] = useState('')

  return (
    <>
      <dl className="grid gap-4 sm:grid-cols-2">
        <InfoRow label="Số điện thoại">{contact.phoneNumber || 'Chưa có'}</InfoRow>
        <InfoRow label="Email">{contact.email || 'Chưa có'}</InfoRow>
      </dl>
      <p role="status" className="type-body-sm text-muted-foreground empty:hidden">
        {message}
      </p>
      {editing ? (
        <ContactForm
          initial={contact}
          onCancel={() => setEditing(false)}
          onSaved={(user) => {
            setContact({ phoneNumber: user.phoneNumber, email: user.email })
            setEditing(false)
            setMessage('Đã lưu liên hệ.')
          }}
        />
      ) : (
        <div>
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              setMessage('')
              setEditing(true)
            }}
          >
            Sửa liên hệ
          </Button>
        </div>
      )}
    </>
  )
}
