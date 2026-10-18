import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { LeafMark } from '@/common/layout/AppShell'
import { PortalCard } from './PortalParts'

// P1 Đăng nhập cổng người thân (UC-70, 19.1, BR-M15-21; feature 002). Bản mẫu: không kiểm tra tài khoản,
// bấm Đăng nhập với hai ô đã điền thì mở cổng mẫu. Tài khoản do viện cấp, không tự đăng ký (Q-341).

export function FamilyLoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const userMissing = !username.trim()
  const passwordMissing = !password

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!userMissing && !passwordMissing) window.location.assign('/family')
  }

  return (
    <div className="flex min-h-screen flex-col items-center gap-6 px-4 py-10">
      <div className="flex items-center gap-2.5">
        <LeafMark />
        <span className="type-title">Elder_Care</span>
      </div>
      <div className="flex w-full max-w-sm flex-col gap-4">
        <PortalCard title="Đăng nhập cổng người thân">
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            <FieldGroup>
              <Field data-invalid={(submitted && userMissing) || undefined}>
                <FieldLabel htmlFor="login-user">Tên đăng nhập</FieldLabel>
                <Input
                  id="login-user"
                  autoComplete="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  aria-invalid={(submitted && userMissing) || undefined}
                  className="h-11"
                />
                {submitted && userMissing && <FieldError>Nhập tên đăng nhập</FieldError>}
              </Field>
              <Field data-invalid={(submitted && passwordMissing) || undefined}>
                <FieldLabel htmlFor="login-password">Mật khẩu</FieldLabel>
                <Input
                  id="login-password"
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  aria-invalid={(submitted && passwordMissing) || undefined}
                  className="h-11"
                />
                {submitted && passwordMissing && <FieldError>Nhập mật khẩu</FieldError>}
              </Field>
            </FieldGroup>
            <Button type="submit" className="h-11">
              Đăng nhập
            </Button>
            <p className="type-caption text-muted-foreground">Bản mẫu: chưa kiểm tra tài khoản.</p>
          </form>
        </PortalCard>
        <PortalCard>
          <p>
            <span className="type-label">Quên mật khẩu? </span>
            Liên hệ Hành chính của viện. Viện cấp lại mật khẩu sau khi xác minh danh tính trực tiếp hoặc gọi lại số điện thoại đã đăng ký.
          </p>
          <p>
            <span className="type-label">Chưa có tài khoản? </span>
            Viện cấp tài khoản cho người thân sau khi gia đình làm thủ tục với viện.
          </p>
        </PortalCard>
      </div>
    </div>
  )
}
