import { useState } from 'react'
import { ApiError, postAccounts } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { ROLE_LABELS, STAFF_ROLES } from './accountLabels'
import { TemporaryPasswordNotice } from './TemporaryPasswordNotice'

// Tạo tài khoản nhân viên (002 T035; US2 #1 → #3; FR-002 → FR-004a, FR-006; hợp đồng POST /accounts).
// Mã hồ sơ gõ tay tới khi 008 có danh sách hồ sơ nhân viên (chủ dự án duyệt 2026-10-20). Ít nhất một vai trò.
// Idempotency-Key giữ suốt form để bấm lại không tạo hai tài khoản.

export function CreateStaffAccount({ onCreated, onClose }) {
  const [staffId, setStaffId] = useState('')
  const [username, setUsername] = useState('')
  const [roles, setRoles] = useState([])
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)
  const [created, setCreated] = useState(null)
  const [key] = useState(() => crypto.randomUUID())

  function toggle(code, checked) {
    setRoles((current) => (checked ? [...current, code] : current.filter((r) => r !== code)))
  }

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const next = {}
    if (!/^\d+$/.test(staffId.trim())) next.staffId = 'Nhập mã hồ sơ nhân viên (số)'
    if (!username.trim()) next.username = 'Nhập tên đăng nhập'
    if (roles.length === 0) next.roles = 'Chọn ít nhất một vai trò'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSaving(true)
    try {
      const { data } = await postAccounts({
        headers: { 'Idempotency-Key': key },
        body: {
          staffId: Number(staffId.trim()),
          username: username.trim(),
          roles: STAFF_ROLES.filter((r) => roles.includes(r)),
        },
      })
      setCreated(data)
      onCreated(data)
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422) {
        setErrors({ username: p.detail || p.title })
      } else {
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">Tạo tài khoản nhân viên</SheetTitle>
        <SheetDescription>Hồ sơ phải đang làm việc và có chức danh dùng hệ thống.</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6">
        {created ? (
          <div className="flex flex-col gap-3">
            <p className="type-body-sm">Đã tạo tài khoản {created.username}.</p>
            <TemporaryPasswordNotice password={created.temporaryPassword} />
            <Button variant="outline" className="self-start" onClick={onClose}>
              Xong
            </Button>
          </div>
        ) : (
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            <FieldGroup>
              <Field data-invalid={!!errors.staffId || undefined}>
                <FieldLabel htmlFor="create-account-staff">Mã hồ sơ nhân viên</FieldLabel>
                <Input
                  id="create-account-staff"
                  inputMode="numeric"
                  value={staffId}
                  onChange={(e) => setStaffId(e.target.value)}
                  aria-invalid={!!errors.staffId || undefined}
                  className="h-10 max-w-48 tabular-nums"
                />
                {errors.staffId ? (
                  <FieldError>{errors.staffId}</FieldError>
                ) : (
                  <FieldDescription>Mã trên hồ sơ nhân viên.</FieldDescription>
                )}
              </Field>
              <Field data-invalid={!!errors.username || undefined}>
                <FieldLabel htmlFor="create-account-username">Tên đăng nhập</FieldLabel>
                <Input
                  id="create-account-username"
                  autoComplete="off"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  aria-invalid={!!errors.username || undefined}
                  className="h-10"
                />
                {errors.username ? (
                  <FieldError>{errors.username}</FieldError>
                ) : (
                  <FieldDescription>Không đổi được sau khi tạo.</FieldDescription>
                )}
              </Field>
              <FieldSet data-invalid={!!errors.roles || undefined}>
                <FieldLegend variant="label">Vai trò</FieldLegend>
                <div className="grid gap-2 sm:grid-cols-2">
                  {STAFF_ROLES.map((code) => (
                    <Field key={code} orientation="horizontal">
                      <Checkbox
                        id={`create-account-role-${code}`}
                        checked={roles.includes(code)}
                        onCheckedChange={(checked) => toggle(code, checked === true)}
                      />
                      <FieldLabel htmlFor={`create-account-role-${code}`}>{ROLE_LABELS[code]}</FieldLabel>
                    </Field>
                  ))}
                </div>
                {errors.roles && <FieldError>{errors.roles}</FieldError>}
              </FieldSet>
            </FieldGroup>

            <ProblemAlert problem={failure} />

            <div className="flex flex-wrap gap-3">
              <Button type="submit" className="h-11" disabled={saving}>
                Tạo tài khoản
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
