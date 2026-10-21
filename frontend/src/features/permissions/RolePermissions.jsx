import { Fragment, useCallback, useEffect, useId, useState } from 'react'
import {
  ApiError,
  getRoles,
  getRolesByCodePermissions,
  postRolesByCodeRestorePermission,
  postRolesByCodeRestrict,
} from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PermissionDeniedState } from '@/common/PageState'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { PermissionCommandForm, RestrictionKindField } from './PermissionCommandForm'
import {
  FUNCTION_ORDER,
  KIND_LABELS,
  OPERATION_LABELS,
  OPERATION_ORDER,
  SCOPE_LABELS,
  functionLabel,
  keyLabel,
} from './permissionLabels'

// Tab Phân quyền (002 T077; US7 #1, #3 → #5; FR-018, FR-020, FR-025, FR-026; bố cục duyệt 2026-10-21): bên trái 9 vai
// trò, bên phải quyền của vai trò theo dòng ma trận (tối đa, hiện hành, trạng thái) và các thu hẹp đang hiệu lực.
// Chỉ ô có trong ma trận mới có nút Thu hẹp; không có lệnh thêm quyền (vượt ma trận bị chặn ở backend, 409).

const keyOf = (p) => `${p.function}/${p.operation}`

const byMatrixOrder = (a, b) =>
  FUNCTION_ORDER.indexOf(a.function) - FUNCTION_ORDER.indexOf(b.function) ||
  OPERATION_ORDER.indexOf(a.operation) - OPERATION_ORDER.indexOf(b.operation)

function restrictionText(r) {
  return `${keyLabel(r)}: ${KIND_LABELS[r.kind] ?? r.kind}`
}

export function RolePermissions() {
  const [roles, setRoles] = useState({ status: 'loading', items: [] })
  const [selected, setSelected] = useState(null)

  useEffect(() => {
    let cancelled = false
    getRoles()
      .then(({ data }) => {
        if (cancelled) return
        setRoles({ status: 'ready', items: data })
        setSelected((s) => s ?? data[0]?.code ?? null)
      })
      .catch((error) => {
        if (!cancelled)
          setRoles({ status: error instanceof ApiError && error.status === 403 ? 'denied' : 'error', items: [] })
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (roles.status === 'denied') return <PermissionDeniedState label="phân quyền" />

  return (
    <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-6 lg:grid-cols-[200px_minmax(0,1fr)]">
      <nav aria-label="Vai trò" className="flex flex-col gap-1 rounded-2xl border bg-card p-2">
        {roles.status === 'loading' && (
          <div aria-busy="true" className="flex flex-col gap-2 p-2">
            <span className="sr-only">Đang tải vai trò</span>
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
          </div>
        )}
        {roles.status === 'error' && <p className="p-3 type-body-sm">Không tải được danh sách vai trò.</p>}
        {roles.items.map((r) => (
          <Button
            key={r.code}
            variant={selected === r.code ? 'default' : 'ghost'}
            aria-pressed={selected === r.code}
            className="h-11 justify-start"
            onClick={() => setSelected(r.code)}
          >
            {r.name}
          </Button>
        ))}
      </nav>
      {selected && <RolePanel key={selected} role={roles.items.find((r) => r.code === selected)} />}
    </div>
  )
}

function RolePanel({ role }) {
  const [state, setState] = useState({ status: 'loading' })
  const [query, setQuery] = useState('')
  const [form, setForm] = useState(null)
  const headingId = useId()

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data, etag } = await getRolesByCodePermissions({ path: { code: role.code } })
      setState({ status: 'ready', data, etag })
    } catch {
      setState({ status: 'error' })
    }
  }, [role.code])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải quyền (kèm ETag) khi chọn vai trò
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <section aria-busy="true" className="flex flex-col gap-2 rounded-2xl border bg-card p-5">
        <span className="sr-only">Đang tải quyền của vai trò</span>
        <Skeleton className="h-8" />
        <Skeleton className="h-10" />
        <Skeleton className="h-10" />
      </section>
    )
  }
  if (state.status === 'error') {
    return (
      <Alert>
        <AlertTitle>Không tải được quyền của vai trò</AlertTitle>
        <AlertAction>
          <Button variant="outline" size="sm" onClick={load}>
            Thử lại
          </Button>
        </AlertAction>
      </Alert>
    )
  }

  const { data, etag } = state
  const current = new Map(data.current.map((p) => [keyOf(p), p]))
  const restricted = new Set(data.restrictions.map(keyOf))
  const q = query.trim().toLowerCase()
  const rows = [...data.maximum]
    .sort(byMatrixOrder)
    .filter((p) => !q || functionLabel(p.function).toLowerCase().includes(q))

  function applied({ data: next, etag: nextEtag }) {
    setForm(null)
    setState({ status: 'ready', data: next, etag: nextEtag })
  }

  const stale = (p) =>
    p.currentValue?.restrictions && (
      <ul className="mt-1 ml-4 list-disc">
        {p.currentValue.restrictions.length === 0 && <li>Chưa có thu hẹp nào.</li>}
        {p.currentValue.restrictions.map((r) => (
          <li key={r.id}>{restrictionText(r)}</li>
        ))}
      </ul>
    )

  return (
    <div className="flex flex-col gap-6">
      <section aria-labelledby={headingId} className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div className="flex flex-col gap-1">
            <h2 id={headingId} className="type-title">
              Quyền của {role.name}
            </h2>
            <p className="type-body-sm text-muted-foreground">
              Quyền tối đa theo ma trận; chỉ thu hẹp được (bỏ thao tác, hạ toàn viện xuống theo phân công).
            </p>
          </div>
          <Field className="w-full max-w-72">
            <FieldLabel htmlFor={`${headingId}-search`}>Tìm chức năng</FieldLabel>
            <Input
              id={`${headingId}-search`}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="h-10"
            />
          </Field>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Chức năng</TableHead>
              <TableHead>Thao tác</TableHead>
              <TableHead>Tối đa</TableHead>
              <TableHead>Hiện hành</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>
                <span className="sr-only">Lệnh</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((p) => {
              const key = keyOf(p)
              const now = current.get(key)
              return (
                <Fragment key={key}>
                  <TableRow>
                    <TableCell className="whitespace-normal">
                      {functionLabel(p.function)}
                      {p.footnoteCodes.length > 0 && (
                        <span className="block type-caption text-muted-foreground">
                          Chú thích {p.footnoteCodes.join(', ')}
                        </span>
                      )}
                    </TableCell>
                    <TableCell>{OPERATION_LABELS[p.operation] ?? p.operation}</TableCell>
                    <TableCell>{SCOPE_LABELS[p.scope] ?? p.scope}</TableCell>
                    <TableCell className={now ? undefined : 'text-muted-foreground'}>
                      {now ? (SCOPE_LABELS[now.scope] ?? now.scope) : 'Đã bỏ'}
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={restricted.has(key) ? 'Đã thu hẹp' : 'Theo ma trận'} />
                    </TableCell>
                    <TableCell className="text-right">
                      {now && form?.key !== key && (
                        <Button variant="outline" size="sm" onClick={() => setForm({ key, kind: 'REMOVE_OPERATION' })}>
                          Thu hẹp
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                  {form?.key === key && (
                    <TableRow>
                      <TableCell colSpan={6} className="whitespace-normal">
                        <PermissionCommandForm
                          title={`Thu hẹp: ${keyLabel(p)}`}
                          submitLabel="Lưu thu hẹp"
                          onCancel={() => setForm(null)}
                          onStale={() => {
                            setForm(null)
                            load()
                          }}
                          stale={stale}
                          onSubmit={async (reason) =>
                            applied(
                              await postRolesByCodeRestrict({
                                path: { code: role.code },
                                headers: { 'If-Match': etag },
                                body: { function: p.function, operation: p.operation, kind: form.kind, reason },
                              }),
                            )
                          }
                        >
                          <RestrictionKindField
                            kind={form.kind}
                            onChange={(kind) => setForm({ ...form, kind })}
                            canLower={now.scope === 'FACILITY'}
                            labels={KIND_LABELS}
                          />
                        </PermissionCommandForm>
                      </TableCell>
                    </TableRow>
                  )}
                </Fragment>
              )
            })}
          </TableBody>
        </Table>
        {rows.length === 0 && <p className="type-body-sm text-muted-foreground">Không có chức năng khớp.</p>}
      </section>

      <RestrictionList
        restrictions={data.restrictions}
        emptyText="Chưa có thu hẹp nào; vai trò dùng đúng ma trận."
        onRestore={async (r, reason) =>
          applied(
            await postRolesByCodeRestorePermission({
              path: { code: role.code },
              headers: { 'If-Match': etag },
              body: { restrictionId: r.id, reason },
            }),
          )
        }
        onStale={load}
        stale={stale}
      />
    </div>
  )
}

// Thu hẹp đang hiệu lực, mỗi dòng có Khôi phục về mức ma trận (FR-025); dùng chung cho vai trò và tài khoản
export function RestrictionList({
  title = 'Thu hẹp đang hiệu lực',
  restrictions,
  emptyText,
  onRestore,
  onStale,
  stale,
}) {
  const [restoring, setRestoring] = useState(null)
  const headingId = useId()
  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
      <h2 id={headingId} className="type-card-title">
        {title}
      </h2>
      {restrictions.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">{emptyText}</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {[...restrictions].sort(byMatrixOrder).map((r) => (
            <li key={r.id} className="flex flex-col gap-2 border-b pb-3 last:border-b-0 last:pb-0">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="flex flex-col gap-0.5 type-body-sm">
                  <span className="type-label">{restrictionText(r)}</span>
                  <span className="text-muted-foreground">
                    Từ <span className="tabular-nums">{formatDateTime(r.effectiveFrom)}</span>, lý do: {r.reason}
                  </span>
                </div>
                {onRestore && restoring !== r.id && (
                  <Button variant="outline" size="sm" onClick={() => setRestoring(r.id)}>
                    Khôi phục
                  </Button>
                )}
              </div>
              {restoring === r.id && (
                <PermissionCommandForm
                  title={`Khôi phục: ${keyLabel(r)}`}
                  submitLabel="Khôi phục về mức ma trận"
                  onCancel={() => setRestoring(null)}
                  onStale={() => {
                    setRestoring(null)
                    onStale()
                  }}
                  stale={stale}
                  onSubmit={async (reason) => {
                    await onRestore(r, reason)
                    setRestoring(null)
                  }}
                />
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
