import { useState } from 'react'
import { ApiError, getMe, postAuthChangePassword } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { LeafMark } from '@/common/layout/AppShell'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { homeOf } from '@/common/session'
import { PortalCard } from '@/features/family-portal/PortalParts'

// Đổi mật khẩu (002 T045; US3 #9, #10; FR-006, FR-006a; hợp đồng POST /auth/change-password): bắt buộc ở lần đăng nhập
// đầu và sau khi được cấp lại; mật khẩu mới theo CFG-M15-09 do máy chủ kiểm. Đổi xong thì vào khung theo loại tài khoản.

const goTo = (url) => window.location.assign(url)

export function ChangePasswordPage({ onNavigate = goTo }) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [again, setAgain] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const found = {}
    if (!current) found.current = 'Nhập mật khẩu hiện tại'
    if (!next) found.next = 'Nhập mật khẩu mới'
    else if (next !== again) found.again = 'Hai lần nhập mật khẩu mới chưa giống nhau'
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setSaving(true)
    try {
      await postAuthChangePassword({ body: { currentPassword: current, newPassword: next } })
      const { data } = await getMe()
      onNavigate(homeOf(data))
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422 && p.errorCode !== 'PASSWORD_POLICY_NOT_MET') {
        setErrors({ current: p.detail || p.title })
      } else {
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center gap-6 px-4 py-10">
      <div className="flex items-center gap-2.5">
        <LeafMark />
        <span className="type-title">Elder_Care</span>
      </div>
      <div className="flex w-full max-w-sm flex-col gap-4">
        <PortalCard title="Đổi mật khẩu">
          <p>Cần đổi mật khẩu trước khi dùng hệ thống.</p>
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            <FieldGroup>
              <Field data-invalid={!!errors.current || undefined}>
                <FieldLabel htmlFor="password-current">Mật khẩu hiện tại</FieldLabel>
                <Input
                  id="password-current"
                  type="password"
                  autoComplete="current-password"
                  value={current}
                  onChange={(e) => setCurrent(e.target.value)}
                  aria-invalid={!!errors.current || undefined}
                  className="h-11"
                />
                {errors.current && <FieldError>{errors.current}</FieldError>}
              </Field>
              <Field data-invalid={!!errors.next || undefined}>
                <FieldLabel htmlFor="password-new">Mật khẩu mới</FieldLabel>
                <Input
                  id="password-new"
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
                  <FieldDescription>Khác mật khẩu hiện tại, đạt độ mạnh viện quy định.</FieldDescription>
                )}
              </Field>
              <Field data-invalid={!!errors.again || undefined}>
                <FieldLabel htmlFor="password-again">Nhập lại mật khẩu mới</FieldLabel>
                <Input
                  id="password-again"
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
              Đổi mật khẩu
            </Button>
          </form>
        </PortalCard>
      </div>
    </div>
  )
}
