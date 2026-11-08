import { useState } from 'react'
import { postAuthPasswordResetConfirm, postAuthPasswordResetRequest } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// Tự đặt lại mật khẩu (002 T105; US3 #12 → #14; FR-052a, BR-M15-25; hợp đồng /auth/password-reset/*): nhập tên đăng
// nhập, nhận mã qua tin nhắn tới số trong hồ sơ, nhập mã và mật khẩu mới. Máy chủ trả như nhau dù tài khoản có thật hay
// không, nên màn không nói mã đã được gửi hay chưa. Chỉ hiện khi kênh tin nhắn đã cấu hình (availability).

export function ForgotPassword({ onDone, onCancel }) {
  const [step, setStep] = useState('request')
  const [username, setUsername] = useState('')
  const [code, setCode] = useState('')
  const [next, setNext] = useState('')
  const [again, setAgain] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)

  async function request(event) {
    event.preventDefault()
    setFailure(null)
    if (!username.trim()) {
      setErrors({ username: 'Nhập tên đăng nhập' })
      return
    }
    setErrors({})
    setSaving(true)
    try {
      await postAuthPasswordResetRequest({ body: { username: username.trim() } })
      setStep('confirm')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setSaving(false)
    }
  }

  async function confirm(event) {
    event.preventDefault()
    setFailure(null)
    const found = {}
    if (!code.trim()) found.code = 'Nhập mã trong tin nhắn'
    if (!next) found.next = 'Nhập mật khẩu mới'
    else if (next !== again) found.again = 'Hai lần nhập mật khẩu mới chưa giống nhau'
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setSaving(true)
    try {
      await postAuthPasswordResetConfirm({ body: { username: username.trim(), code: code.trim(), newPassword: next } })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setSaving(false)
    }
  }

  if (step === 'request') {
    return (
      <form onSubmit={request} noValidate className="flex flex-col gap-4">
        <p>Nhập tên đăng nhập. Nếu hồ sơ có số điện thoại đã đăng ký, viện gửi mã đặt lại mật khẩu tới số đó.</p>
        <Field data-invalid={!!errors.username || undefined}>
          <FieldLabel htmlFor="reset-user">Tên đăng nhập</FieldLabel>
          <Input
            id="reset-user"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            aria-invalid={!!errors.username || undefined}
            className="h-11"
          />
          {errors.username && <FieldError>{errors.username}</FieldError>}
        </Field>
        <ProblemAlert problem={failure} />
        <Button type="submit" className="h-11" disabled={saving}>
          Gửi mã
        </Button>
        <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
          Quay lại đăng nhập
        </Button>
      </form>
    )
  }

  return (
    <form onSubmit={confirm} noValidate className="flex flex-col gap-4">
      <p>
        Nếu tài khoản có số điện thoại đã đăng ký, mã đã được gửi tới số đó. Không nhận được mã thì liên hệ viện để được
        cấp lại mật khẩu.
      </p>
      <FieldGroup>
        <Field data-invalid={!!errors.code || undefined}>
          <FieldLabel htmlFor="reset-code">Mã trong tin nhắn</FieldLabel>
          <Input
            id="reset-code"
            inputMode="numeric"
            autoComplete="one-time-code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            aria-invalid={!!errors.code || undefined}
            className="h-11 tabular-nums"
          />
          {errors.code && <FieldError>{errors.code}</FieldError>}
        </Field>
        <Field data-invalid={!!errors.next || undefined}>
          <FieldLabel htmlFor="reset-new">Mật khẩu mới</FieldLabel>
          <Input
            id="reset-new"
            type="password"
            autoComplete="new-password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
            aria-invalid={!!errors.next || undefined}
            className="h-11"
          />
          {errors.next ? (
            <FieldError>{errors.next}</FieldError>
          ) : (
            <FieldDescription>Đạt độ mạnh viện quy định.</FieldDescription>
          )}
        </Field>
        <Field data-invalid={!!errors.again || undefined}>
          <FieldLabel htmlFor="reset-again">Nhập lại mật khẩu mới</FieldLabel>
          <Input
            id="reset-again"
            type="password"
            autoComplete="new-password"
            value={again}
            onChange={(e) => setAgain(e.target.value)}
            aria-invalid={!!errors.again || undefined}
            className="h-11"
          />
          {errors.again && <FieldError>{errors.again}</FieldError>}
        </Field>
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11" disabled={saving}>
        Đặt mật khẩu mới
      </Button>
      <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
        Quay lại đăng nhập
      </Button>
    </form>
  )
}
