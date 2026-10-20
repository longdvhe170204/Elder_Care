import { useState } from 'react'
import { postAuthLogin } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { LeafMark } from '@/common/layout/AppShell'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { deviceId, homeOf } from '@/common/session'
import { PortalCard } from '@/features/family-portal/PortalParts'

// Đăng nhập web cho nhân viên và người thân (002 T045; UC-70; FR-006, FR-008, FR-014, FR-014a; chủ dự án chọn một màn
// chung, 2026-10-20). Bố cục theo màn P1 đã duyệt. Thông báo lỗi lấy từ máy chủ, không nói tên có tồn tại hay không.
// Tài khoản do viện cấp, không tự đăng ký (Q-341).

const goTo = (url) => window.location.assign(url)

export function LoginPage({ onNavigate = goTo }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const next = {}
    if (!username.trim()) next.username = 'Nhập tên đăng nhập'
    if (!password) next.password = 'Nhập mật khẩu'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSaving(true)
    try {
      const { data } = await postAuthLogin({
        headers: { 'X-Device-Id': deviceId() },
        body: { username: username.trim(), password, channel: 'WEB' },
      })
      onNavigate(homeOf({ ...data.user, mustChangePassword: data.mustChangePassword }))
    } catch (error) {
      setPassword('')
      setFailure({ ...problemOf(error), unlocksAt: error?.problem?.unlocksAt })
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
        <PortalCard title="Đăng nhập">
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            <FieldGroup>
              <Field data-invalid={!!errors.username || undefined}>
                <FieldLabel htmlFor="login-user">Tên đăng nhập</FieldLabel>
                <Input
                  id="login-user"
                  autoComplete="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  aria-invalid={!!errors.username || undefined}
                  className="h-11"
                />
                {errors.username && <FieldError>{errors.username}</FieldError>}
              </Field>
              <Field data-invalid={!!errors.password || undefined}>
                <FieldLabel htmlFor="login-password">Mật khẩu</FieldLabel>
                <Input
                  id="login-password"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  aria-invalid={!!errors.password || undefined}
                  className="h-11"
                />
                {errors.password && <FieldError>{errors.password}</FieldError>}
              </Field>
            </FieldGroup>
            <ProblemAlert problem={failure} title={failure?.status === 401 ? 'Chưa đăng nhập được' : undefined}>
              {failure?.unlocksAt && <p className="tabular-nums">Mở lại lúc {formatDateTime(failure.unlocksAt)}.</p>}
            </ProblemAlert>
            <Button type="submit" className="h-11" disabled={saving}>
              Đăng nhập
            </Button>
          </form>
        </PortalCard>
        <PortalCard>
          <p>
            <span className="type-label">Quên mật khẩu? </span>
            Nhân viên liên hệ Quản lý viện. Người thân liên hệ Hành chính của viện; viện cấp lại sau khi xác minh danh
            tính trực tiếp hoặc gọi lại số điện thoại đã đăng ký.
          </p>
          <p>
            <span className="type-label">Chưa có tài khoản? </span>
            Viện cấp tài khoản cho nhân viên và cho người thân sau khi gia đình làm thủ tục với viện.
          </p>
        </PortalCard>
      </div>
    </div>
  )
}
