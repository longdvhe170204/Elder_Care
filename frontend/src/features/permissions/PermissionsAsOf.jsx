import { useId, useState } from 'react'
import { getAccountsByIdPermissions } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { STATUS_LABELS, roleNames } from '@/features/accounts/accountLabels'
import { KIND_LABELS, SCOPE_LABELS, keyLabel } from './permissionLabels'

// Tra tập quyền của tài khoản tại một thời điểm quá khứ (002 T077; FR-026a, BR-M15-16): chỉ xem, không có lệnh.
// Giờ nhập theo giờ Việt Nam (UTC+7, NFR-11), gửi asOf dạng ISO-8601 UTC.

function toAsOf(localValue) {
  return new Date(`${localValue}:00+07:00`).toISOString()
}

export function PermissionsAsOf({ accountId }) {
  const id = useId()
  const [value, setValue] = useState('')
  const [result, setResult] = useState(null)
  const [failure, setFailure] = useState(null)

  async function view(event) {
    event.preventDefault()
    if (!value) return
    setFailure(null)
    try {
      const { data } = await getAccountsByIdPermissions({ path: { id: accountId }, query: { asOf: toAsOf(value) } })
      setResult(data)
    } catch (error) {
      setResult(null)
      setFailure(problemOf(error))
    }
  }

  return (
    <div className="flex flex-col gap-3 rounded-xl border p-4">
      <form onSubmit={view} className="flex flex-wrap items-end gap-3">
        <Field className="w-auto">
          <FieldLabel htmlFor={`${id}-as-of`}>Xem tại thời điểm</FieldLabel>
          <Input
            id={`${id}-as-of`}
            type="datetime-local"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            className="h-10 tabular-nums"
          />
        </Field>
        <Button type="submit" variant="outline" className="h-10" disabled={!value}>
          Xem
        </Button>
      </form>
      <ProblemAlert problem={failure} />
      {result && (
        <section aria-label={`Quyền lúc ${formatDateTime(result.asOf)}`} className="flex flex-col gap-3">
          <h4 className="type-label">Quyền lúc {formatDateTime(result.asOf)}</h4>
          <PermissionSummary permissions={result} showAccountRestrictions />
          <p className="type-body-sm text-muted-foreground">{result.current.length} quyền có hiệu lực lúc đó.</p>
        </section>
      )}
    </div>
  )
}

// Vai trò, thu hẹp theo vai trò, quyền gán thêm của một tập quyền (AccountPermissions của hợp đồng)
export function PermissionSummary({ permissions, showAccountRestrictions = false }) {
  const list = (items, empty, text) =>
    items.length === 0 ? (
      <span className="text-muted-foreground">{empty}</span>
    ) : (
      <ul className="flex flex-col gap-0.5">
        {items.map((item, i) => (
          <li key={item.id ?? i}>{text(item)}</li>
        ))}
      </ul>
    )
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 type-body-sm">
      <dt className="text-muted-foreground">Trạng thái</dt>
      <dd>
        <StatusBadge status={STATUS_LABELS[permissions.status] ?? permissions.status} />
      </dd>
      <dt className="text-muted-foreground">Vai trò</dt>
      <dd>{permissions.roles.length ? roleNames(permissions.roles) : 'Chưa có vai trò'}</dd>
      <dt className="text-muted-foreground">Thu hẹp theo vai trò</dt>
      <dd>
        {list(permissions.roleRestrictions, 'Không có', (r) => `${keyLabel(r)}: ${KIND_LABELS[r.kind] ?? r.kind}`)}
      </dd>
      {showAccountRestrictions && (
        <>
          <dt className="text-muted-foreground">Thu hẹp theo tài khoản</dt>
          <dd>
            {list(
              permissions.accountRestrictions,
              'Không có',
              (r) => `${keyLabel(r)}: ${KIND_LABELS[r.kind] ?? r.kind}`,
            )}
          </dd>
        </>
      )}
      <dt className="text-muted-foreground">Quyền gán thêm</dt>
      <dd>
        {list(
          permissions.additionalGrants,
          'Chưa gán thêm quyền nào',
          (g) => `${keyLabel(g)} (${(SCOPE_LABELS[g.scope] ?? g.scope).toLowerCase()})`,
        )}
      </dd>
    </dl>
  )
}
