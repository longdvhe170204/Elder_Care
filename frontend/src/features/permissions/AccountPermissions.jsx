import { useCallback, useEffect, useId, useState } from 'react'
import {
  getAccountsByIdPermissions,
  postAccountsByIdGrantCarePlanApproval,
  postAccountsByIdRestorePermission,
  postAccountsByIdRestrict,
  postAccountsByIdRevokeCarePlanApproval,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Field, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { PermissionCommandForm, RestrictionKindField } from './PermissionCommandForm'
import { PermissionsAsOf, PermissionSummary } from './PermissionsAsOf'
import { RestrictionList } from './RolePermissions'
import { KIND_LABELS, SCOPE_LABELS, keyLabel } from './permissionLabels'

// Mục Quyền trong khung chi tiết tài khoản (002 T077; US7 #2, #4, #5; FR-024 → FR-026a; bố cục duyệt 2026-10-21):
// vai trò, thu hẹp theo vai trò (chỉ xem), thu hẹp theo tài khoản (khôi phục được), quyền gán thêm, quyền hiện hành;
// lệnh theo allowedCommands của tài khoản. If-Match là ETag của tập quyền (permission_version, BR-M15-17).

const keyOf = (p) => `${p.function}/${p.operation}`

export function AccountPermissions({ accountId, allowedCommands, onChanged }) {
  const [state, setState] = useState({ status: 'loading' })
  const [form, setForm] = useState(null)
  const allowed = new Set(allowedCommands)

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data, etag } = await getAccountsByIdPermissions({ path: { id: accountId } })
      setState({ status: 'ready', data, etag })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [accountId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải tập quyền (kèm ETag) khi mở khung
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2">
        <span className="sr-only">Đang tải quyền</span>
        <Skeleton className="h-16" />
      </div>
    )
  }
  if (state.status === 'error') return <ProblemAlert problem={state.problem} />

  const { data, etag } = state
  const path = { id: accountId }

  function applied({ data: next, etag: nextEtag }) {
    setForm(null)
    setState({ status: 'ready', data: next, etag: nextEtag })
    onChanged()
  }

  const stale = (p) =>
    p.currentValue?.accountRestrictions && (
      <p className="mt-1">
        Thu hẹp theo tài khoản hiện có: {p.currentValue.accountRestrictions.map(keyLabel).join('; ') || 'không có'}.
      </p>
    )
  const reload = () => {
    setForm(null)
    load()
  }

  return (
    <section aria-label="Quyền" className="flex flex-col gap-4">
      <h3 className="type-card-title">Quyền</h3>
      <PermissionSummary permissions={data} />

      <RestrictionList
        title="Thu hẹp theo tài khoản"
        restrictions={data.accountRestrictions}
        emptyText="Chưa thu hẹp quyền nào của riêng tài khoản này."
        onRestore={
          allowed.has('restore-permission')
            ? async (r, reason) =>
                applied(
                  await postAccountsByIdRestorePermission({
                    path,
                    headers: { 'If-Match': etag },
                    body: { restrictionId: r.id, reason },
                  }),
                )
            : undefined
        }
        onStale={load}
        stale={stale}
      />

      {form?.command === 'restrict' && (
        <RestrictForm
          permissions={data.current}
          form={form}
          setForm={setForm}
          onCancel={() => setForm(null)}
          onStale={reload}
          stale={stale}
          onSubmit={async (key, reason) =>
            applied(
              await postAccountsByIdRestrict({
                path,
                headers: { 'If-Match': etag },
                body: { function: key.function, operation: key.operation, kind: form.kind, reason },
              }),
            )
          }
        />
      )}
      {form?.command === 'grant' && (
        <PermissionCommandForm
          title="Gán quyền Duyệt kế hoạch chăm sóc"
          submitLabel="Gán quyền"
          onCancel={() => setForm(null)}
          onStale={reload}
          stale={stale}
          onSubmit={async (reason) =>
            applied(
              await postAccountsByIdGrantCarePlanApproval({ path, headers: { 'If-Match': etag }, body: { reason } }),
            )
          }
        />
      )}
      {form?.command === 'revoke' && (
        <PermissionCommandForm
          title="Gỡ quyền Duyệt kế hoạch chăm sóc"
          submitLabel="Gỡ quyền"
          onCancel={() => setForm(null)}
          onStale={reload}
          stale={stale}
          onSubmit={async (reason) =>
            applied(
              await postAccountsByIdRevokeCarePlanApproval({ path, headers: { 'If-Match': etag }, body: { reason } }),
            )
          }
        />
      )}
      {!form && (
        <div className="flex flex-wrap gap-2">
          {allowed.has('restrict') && data.current.length > 0 && (
            <Button
              variant="outline"
              onClick={() => setForm({ command: 'restrict', key: keyOf(data.current[0]), kind: 'REMOVE_OPERATION' })}
            >
              Thu hẹp quyền
            </Button>
          )}
          {allowed.has('grant-care-plan-approval') && (
            <Button variant="outline" onClick={() => setForm({ command: 'grant' })}>
              Gán quyền Duyệt kế hoạch chăm sóc
            </Button>
          )}
          {allowed.has('revoke-care-plan-approval') && (
            <Button variant="outline" onClick={() => setForm({ command: 'revoke' })}>
              Gỡ quyền Duyệt kế hoạch chăm sóc
            </Button>
          )}
        </div>
      )}

      <Collapsible>
        <CollapsibleTrigger render={<Button variant="ghost" className="h-11 justify-start px-2" />}>
          Quyền hiện hành ({data.current.length})
        </CollapsibleTrigger>
        <CollapsibleContent>
          <ul className="flex flex-col gap-1 pt-2 type-body-sm">
            {data.current.map((p, i) => (
              <li key={`${keyOf(p)}-${i}`}>
                {keyLabel(p)} <span className="text-muted-foreground">({SCOPE_LABELS[p.scope] ?? p.scope})</span>
              </li>
            ))}
          </ul>
        </CollapsibleContent>
      </Collapsible>

      <PermissionsAsOf accountId={accountId} />
    </section>
  )
}

// Thu hẹp theo tài khoản: chọn trong quyền hiện hành của tài khoản (không chọn được quyền ngoài ma trận, FR-025)
function RestrictForm({ permissions, form, setForm, onSubmit, onCancel, onStale, stale }) {
  const id = useId()
  const keys = [...new Map(permissions.map((p) => [keyOf(p), p])).values()]
  const selected = keys.find((p) => keyOf(p) === form.key) ?? keys[0]
  const canLower = permissions.some((p) => keyOf(p) === keyOf(selected) && p.scope === 'FACILITY')
  return (
    <PermissionCommandForm
      title="Thu hẹp quyền của tài khoản"
      submitLabel="Lưu thu hẹp"
      onCancel={onCancel}
      onStale={onStale}
      stale={stale}
      onSubmit={(reason) => onSubmit(selected, reason)}
    >
      <Field>
        <FieldLabel htmlFor={`${id}-key`}>Quyền cần thu hẹp</FieldLabel>
        <select
          id={`${id}-key`}
          value={keyOf(selected)}
          onChange={(e) => setForm({ ...form, key: e.target.value, kind: 'REMOVE_OPERATION' })}
          className="h-10 w-full rounded-md border border-input bg-transparent px-3"
        >
          {keys.map((p) => (
            <option key={keyOf(p)} value={keyOf(p)}>
              {keyLabel(p)}
            </option>
          ))}
        </select>
      </Field>
      <RestrictionKindField
        kind={form.kind}
        onChange={(kind) => setForm({ ...form, kind })}
        canLower={canLower}
        labels={KIND_LABELS}
      />
    </PermissionCommandForm>
  )
}
