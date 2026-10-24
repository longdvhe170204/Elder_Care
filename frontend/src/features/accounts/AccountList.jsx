import { useCallback, useEffect, useState } from 'react'
import { ApiError, getAccounts } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PermissionDeniedState } from '@/common/PageState'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { AccountDetail } from './AccountDetail'
import { CreateResidentAccount } from './CreateResidentAccount'
import { CreateStaffAccount } from './CreateStaffAccount'
import { STATUS_LABELS, TYPE_LABELS, profileLabel, roleNames } from './accountLabels'

// Danh sách tài khoản (002 T035; US2; FR-001, FR-051; hợp đồng GET /accounts, phân trang theo con trỏ).
// Quản lý viện thấy mọi tài khoản; Hành chính thấy tài khoản người thân, người cao tuổi (backend lọc). canCreate,
// canCreateResident chỉ để hiện nút tạo tài khoản nhân viên, người cao tuổi (002 T095); backend vẫn kiểm (constitution XI).

const EMPTY_FILTER = { type: '', status: '', q: '' }

function toQuery(filter, cursor) {
  return { type: filter.type || undefined, status: filter.status || undefined, q: filter.q.trim() || undefined, cursor }
}

export function AccountList({ canCreate, canCreateResident = false }) {
  const [draft, setDraft] = useState(EMPTY_FILTER)
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [state, setState] = useState({ status: 'loading', items: [], nextCursor: null })
  const [opened, setOpened] = useState(null)

  const load = useCallback(async (nextFilter, cursor) => {
    setState((s) => ({ ...s, status: cursor ? 'more' : 'loading', ...(cursor ? {} : { items: [] }) }))
    try {
      const { data } = await getAccounts({ query: toQuery(nextFilter, cursor) })
      setState((s) => ({
        status: 'ready',
        items: cursor ? [...s.items, ...data.items] : data.items,
        nextCursor: data.nextCursor ?? null,
      }))
    } catch (error) {
      setState((s) => ({ ...s, status: error instanceof ApiError && error.status === 403 ? 'denied' : 'error' }))
    }
  }, [])

  useEffect(() => {
    load(EMPTY_FILTER)
  }, [load])

  const replaceRow = useCallback((data) => {
    setState((s) => ({ ...s, items: s.items.map((a) => (a.id === data.id ? data : a)) }))
  }, [])

  if (state.status === 'denied') return <PermissionDeniedState label="tài khoản" />

  function search(event) {
    event.preventDefault()
    setFilter(draft)
    load(draft)
  }

  return (
    <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
      <form
        onSubmit={search}
        className="grid items-end gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)_auto]"
      >
        <Field>
          <FieldLabel htmlFor="account-search">Tìm tên đăng nhập</FieldLabel>
          <Input
            id="account-search"
            value={draft.q}
            onChange={(e) => setDraft({ ...draft, q: e.target.value })}
            className="h-10"
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="account-type">Loại</FieldLabel>
          <select
            id="account-type"
            value={draft.type}
            onChange={(e) => setDraft({ ...draft, type: e.target.value })}
            className="h-10 w-full rounded-md border border-input bg-transparent px-3"
          >
            <option value="">Tất cả</option>
            <option value="STAFF">{TYPE_LABELS.STAFF}</option>
            <option value="FAMILY_MEMBER">{TYPE_LABELS.FAMILY_MEMBER}</option>
            <option value="RESIDENT">{TYPE_LABELS.RESIDENT}</option>
          </select>
        </Field>
        <Field>
          <FieldLabel htmlFor="account-status">Trạng thái</FieldLabel>
          <select
            id="account-status"
            value={draft.status}
            onChange={(e) => setDraft({ ...draft, status: e.target.value })}
            className="h-10 w-full rounded-md border border-input bg-transparent px-3"
          >
            <option value="">Tất cả</option>
            {Object.entries(STATUS_LABELS).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
        </Field>
        <div className="flex flex-wrap gap-2">
          <Button type="submit" variant="outline" className="h-10">
            Lọc
          </Button>
          {canCreate && (
            <Button type="button" className="h-10" onClick={() => setOpened('new')}>
              Tạo tài khoản nhân viên
            </Button>
          )}
          {canCreateResident && (
            <Button type="button" className="h-10" onClick={() => setOpened('new-resident')}>
              Tạo tài khoản người cao tuổi
            </Button>
          )}
        </div>
      </form>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải tài khoản</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && (
        <Alert role="alert">
          <AlertTitle>Không tải được danh sách tài khoản</AlertTitle>
          <AlertAction>
            <Button variant="outline" size="sm" onClick={() => load(filter)}>
              Thử lại
            </Button>
          </AlertAction>
        </Alert>
      )}
      {state.status !== 'loading' && state.status !== 'error' && state.items.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không có tài khoản khớp</EmptyTitle>
            <EmptyDescription>Đổi từ khóa hoặc bộ lọc rồi bấm Lọc.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {state.items.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tên đăng nhập</TableHead>
              <TableHead>Hồ sơ</TableHead>
              <TableHead>Loại</TableHead>
              <TableHead className="min-w-40">Vai trò</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Đăng nhập gần nhất</TableHead>
              {filter.type === 'RESIDENT' && <TableHead>Thiết bị</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.items.map((a) => (
              <TableRow key={a.id}>
                <TableCell>
                  <Button variant="link" className="h-auto p-0" onClick={() => setOpened(a.id)}>
                    {a.username}
                  </Button>
                </TableCell>
                <TableCell className={a.fullName ? undefined : 'text-muted-foreground'}>{profileLabel(a)}</TableCell>
                <TableCell>{TYPE_LABELS[a.type] ?? a.type}</TableCell>
                <TableCell className="whitespace-normal">{roleNames(a.roles)}</TableCell>
                <TableCell>
                  <StatusBadge status={STATUS_LABELS[a.status] ?? a.status} />
                </TableCell>
                <TableCell className="tabular-nums">
                  {a.lastLoginAt ? (
                    formatDateTime(a.lastLoginAt)
                  ) : (
                    <span className="text-muted-foreground">Chưa đăng nhập</span>
                  )}
                </TableCell>
                {filter.type === 'RESIDENT' && (
                  <TableCell className="tabular-nums">
                    {a.boundDevice ? (
                      `Đã gắn ${formatDateTime(a.boundDevice.boundAt)}`
                    ) : (
                      <span className="text-muted-foreground">Chưa gắn</span>
                    )}
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {state.nextCursor && (
        <Button
          variant="outline"
          className="self-start"
          disabled={state.status === 'more'}
          onClick={() => load(filter, state.nextCursor)}
        >
          Xem thêm
        </Button>
      )}

      <Sheet open={opened !== null} onOpenChange={(open) => !open && setOpened(null)}>
        <SheetContent className="sm:max-w-lg">
          {opened === 'new' && <CreateStaffAccount onCreated={() => load(filter)} onClose={() => setOpened(null)} />}
          {opened === 'new-resident' && (
            <CreateResidentAccount onCreated={() => load(filter)} onClose={() => setOpened(null)} />
          )}
          {typeof opened === 'number' && <AccountDetail key={opened} id={opened} onChanged={replaceRow} />}
        </SheetContent>
      </Sheet>
    </section>
  )
}
