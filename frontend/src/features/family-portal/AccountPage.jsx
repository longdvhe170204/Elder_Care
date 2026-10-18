import { useState } from 'react'
import { Check, Minus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/common/PageHeader'
import { DEMO_PORTAL_PERMISSION_LABELS, DEMO_RESIDENTS } from '@/common/demoData'
import { InfoList, PortalCard } from './PortalParts'

// P11 Tài khoản, quyền của tôi (14.1, 19.1, BR-M15-21): người thân thấy mình được xem, làm gì; đổi mật khẩu.
// Quyền không sửa ở đây: thay đổi qua người đại diện hoặc Hành chính (BR-M10-07). Mật khẩu theo CFG-M15-09.

const STRONG = /^(?=.*[A-Za-zÀ-ỹ])(?=.*\d).{8,}$/

function PasswordForm() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [again, setAgain] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [done, setDone] = useState(false)
  const errors = {
    current: !current && 'Nhập mật khẩu hiện tại',
    next: !STRONG.test(next) && 'Mật khẩu mới cần ít nhất 8 ký tự, có cả chữ và số',
    again: next !== again && 'Mật khẩu nhập lại chưa khớp',
  }

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    setDone(!errors.current && !errors.next && !errors.again)
  }

  const fields = [
    ['current', 'Mật khẩu hiện tại', current, setCurrent, 'current-password'],
    ['next', 'Mật khẩu mới', next, setNext, 'new-password'],
    ['again', 'Nhập lại mật khẩu mới', again, setAgain, 'new-password'],
  ]
  return (
    <PortalCard title="Đổi mật khẩu">
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <FieldGroup>
          {fields.map(([key, label, value, setValue, autoComplete]) => (
            <Field key={key} data-invalid={(submitted && errors[key]) || undefined}>
              <FieldLabel htmlFor={`pw-${key}`}>{label}</FieldLabel>
              <Input
                id={`pw-${key}`}
                type="password"
                autoComplete={autoComplete}
                value={value}
                onChange={(e) => setValue(e.target.value)}
                aria-invalid={(submitted && errors[key]) || undefined}
                className="h-11"
              />
              {submitted && errors[key] ? (
                <FieldError>{errors[key]}</FieldError>
              ) : (
                key === 'next' && <FieldDescription>Ít nhất 8 ký tự, có cả chữ và số.</FieldDescription>
              )}
            </Field>
          ))}
        </FieldGroup>
        <Button type="submit" className="h-11">
          Đổi mật khẩu
        </Button>
        <p role="status" className="type-caption text-muted-foreground">
          {done && 'Bản mẫu: mật khẩu mới hợp lệ, chưa lưu.'}
        </p>
      </form>
    </PortalCard>
  )
}

export function AccountPage({ viewer }) {
  const roles = [viewer.representative && 'Người đại diện', viewer.primaryContact && 'Người liên hệ chính'].filter(Boolean)
  return (
    <>
      <PageHeader title="Tài khoản, quyền của tôi" />
      <PortalCard title={viewer.name}>
        <InfoList
          rows={[
            ['Người cao tuổi', DEMO_RESIDENTS[0].name],
            ['Quan hệ', viewer.relation],
            ['Vai trò trong gia đình', roles.length ? roles.join(', ') : 'Không có'],
          ]}
        />
      </PortalCard>
      <PortalCard title="Quyền của bạn">
        <ul className="flex flex-col">
          {DEMO_PORTAL_PERMISSION_LABELS.map(([key, label]) => (
            <li key={key} className="flex items-center gap-3 border-t py-2 first:border-t-0 first:pt-0">
              {viewer.perms[key] ? (
                <Check className="size-4 text-success-foreground" aria-hidden="true" />
              ) : (
                <Minus className="size-4 text-muted-foreground" aria-hidden="true" />
              )}
              <span className="flex-1">{label}</span>
              <span className={viewer.perms[key] ? 'type-label' : 'text-muted-foreground'}>{viewer.perms[key] ? 'Có' : 'Không'}</span>
            </li>
          ))}
        </ul>
        <span className="type-caption text-muted-foreground">
          Muốn thay đổi quyền, liên hệ người đại diện của gia đình hoặc Hành chính của viện.
        </span>
      </PortalCard>
      <PasswordForm />
    </>
  )
}
