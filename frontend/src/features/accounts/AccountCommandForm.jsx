import { useEffect, useState } from 'react'
import {
  ApiError,
  getAccountsByIdPermissions,
  postAccountsByIdAssignRole,
  postAccountsByIdLock,
  postAccountsByIdRemoveRole,
  postAccountsByIdResetPassword,
  postAccountsByIdUnlock,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { COMMANDS, ROLE_LABELS, STAFF_ROLES } from './accountLabels'
import { TemporaryPasswordNotice } from './TemporaryPasswordNotice'

// Một lệnh trên tài khoản nhân viên (002 T035; FR-003 → FR-006, FR-011, FR-012; hợp đồng /accounts/{id}/…).
// Lý do bắt buộc. Khóa, mở khóa gửi If-Match là ETag tài khoản; gán, gỡ vai trò gửi ETag tập quyền
// (GET /accounts/{id}/permissions, phiên bản quyền BR-M15-17). Khóa người đang là Phụ trách bộ phận cần người thay (Q-292).

const REASON_MAX = 2000

const isRoleCommand = (command) => command === 'assign-role' || command === 'remove-role'

function roleChoices(command, account) {
  const held = account.roles ?? []
  return command === 'assign-role' ? STAFF_ROLES.filter((r) => !held.includes(r)) : held
}

export function AccountCommandForm({ command, account, etag, onDone, onCancel, onStale }) {
  const roles = roleChoices(command, account)
  const [role, setRole] = useState(roles[0] ?? '')
  const [reason, setReason] = useState('')
  const [replacement, setReplacement] = useState('')
  const [needsReplacement, setNeedsReplacement] = useState(false)
  const [permissionEtag, setPermissionEtag] = useState(null)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)
  const [password, setPassword] = useState(null)

  useEffect(() => {
    if (!isRoleCommand(command)) return undefined
    let cancelled = false
    getAccountsByIdPermissions({ path: { id: account.id } })
      .then(({ etag: next }) => !cancelled && setPermissionEtag(next))
      .catch((error) => !cancelled && setFailure(problemOf(error)))
    return () => {
      cancelled = true
    }
  }, [command, account.id])

  async function send() {
    const path = { id: account.id }
    switch (command) {
      case 'lock':
        return postAccountsByIdLock({
          path,
          headers: { 'If-Match': etag },
          body: { reason, ...(needsReplacement && { replacementDepartmentLead: Number(replacement) }) },
        })
      case 'unlock':
        return postAccountsByIdUnlock({ path, headers: { 'If-Match': etag }, body: { reason } })
      case 'reset-password':
        return postAccountsByIdResetPassword({ path, body: { reason } })
      case 'assign-role':
        return postAccountsByIdAssignRole({ path, headers: { 'If-Match': permissionEtag }, body: { role, reason } })
      default:
        return postAccountsByIdRemoveRole({ path, headers: { 'If-Match': permissionEtag }, body: { role, reason } })
    }
  }

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const next = {}
    if (isRoleCommand(command) && !role) next.role = 'Chọn vai trò'
    if (needsReplacement && !/^\d+$/.test(replacement.trim())) next.replacement = 'Nhập mã hồ sơ người thay'
    if (!reason.trim()) next.reason = 'Nhập lý do'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSaving(true)
    try {
      const { data } = await send()
      if (command === 'reset-password') {
        setPassword(data.temporaryPassword)
        onDone(null)
      } else {
        onDone(data)
      }
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422) {
        setErrors({ reason: p.detail || p.title })
      } else if (p.errorCode === 'REPLACEMENT_LEAD_REQUIRED') {
        setNeedsReplacement(true)
        setFailure(p)
      } else {
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  if (password) {
    return (
      <div className="flex flex-col gap-3">
        <TemporaryPasswordNotice password={password} />
        <Button variant="outline" className="self-start" onClick={onCancel}>
          Xong
        </Button>
      </div>
    )
  }

  const meta = COMMANDS[command]
  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-xl border p-4">
      <h3 className="type-card-title">{meta.label}</h3>
      <FieldGroup>
        {isRoleCommand(command) && (
          <Field data-invalid={!!errors.role || undefined}>
            <FieldLabel htmlFor="account-command-role">Vai trò</FieldLabel>
            <select
              id="account-command-role"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              aria-invalid={!!errors.role || undefined}
              className="h-10 w-full rounded-md border border-input bg-transparent px-3"
            >
              {roles.map((code) => (
                <option key={code} value={code}>
                  {ROLE_LABELS[code] ?? code}
                </option>
              ))}
            </select>
            {errors.role && <FieldError>{errors.role}</FieldError>}
          </Field>
        )}

        {needsReplacement && (
          <Field data-invalid={!!errors.replacement || undefined}>
            <FieldLabel htmlFor="account-command-replacement">Mã hồ sơ người thay Phụ trách bộ phận</FieldLabel>
            <Input
              id="account-command-replacement"
              inputMode="numeric"
              value={replacement}
              onChange={(e) => setReplacement(e.target.value)}
              aria-invalid={!!errors.replacement || undefined}
              className="h-10 max-w-48 tabular-nums"
            />
            {errors.replacement ? (
              <FieldError>{errors.replacement}</FieldError>
            ) : (
              <FieldDescription>
                Người này đang là Phụ trách bộ phận; người thay nhận nhiệm vụ cùng lúc khóa.
              </FieldDescription>
            )}
          </Field>
        )}

        <Field data-invalid={!!errors.reason || undefined}>
          <FieldLabel htmlFor="account-command-reason">Lý do</FieldLabel>
          <Textarea
            id="account-command-reason"
            rows={3}
            maxLength={REASON_MAX}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!errors.reason || undefined}
          />
          {errors.reason ? <FieldError>{errors.reason}</FieldError> : <FieldDescription>Bắt buộc.</FieldDescription>}
        </Field>
      </FieldGroup>

      <ProblemAlert problem={failure}>
        {failure?.status === 412 && (
          <Button type="button" variant="outline" size="sm" className="mt-2" onClick={onStale}>
            Tải lại
          </Button>
        )}
      </ProblemAlert>

      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={saving || (isRoleCommand(command) && !permissionEtag)}>
          {meta.submit}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}
