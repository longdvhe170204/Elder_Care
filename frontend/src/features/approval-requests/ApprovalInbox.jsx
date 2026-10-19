import { useCallback, useEffect, useState } from 'react'
import { getApprovalRequests } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { ApprovalRequestDetail } from './ApprovalRequestDetail'
import { STATUS_LABELS, TYPE_LABELS, actorLabel, statusLabel, typeLabel } from './approvalStatus'

// Hộp duyệt (000 T097; US5; FR-031 → FR-040; hợp đồng GET /approval-requests). Ba hộp theo `inbox`; danh sách
// chỉ gồm yêu cầu trong phạm vi người xem (backend lọc). Mục menu "Yêu cầu phê duyệt" cho mọi vai trò nhân viên.

const INBOXES = [
  { value: 'AWAITING_MY_APPROVAL', label: 'Chờ tôi duyệt' },
  { value: 'MINE', label: 'Của tôi' },
  { value: 'ALL_IN_SCOPE', label: 'Trong phạm vi' },
]

const SELECT_CLASS = 'h-10 w-full rounded-md border border-input bg-transparent px-3'

function toQuery({ inbox, status, type }, cursor) {
  return { inbox, status: status || undefined, type: type || undefined, cursor }
}

export function ApprovalInbox() {
  const [filter, setFilter] = useState({ inbox: 'AWAITING_MY_APPROVAL', status: '', type: '' })
  const [state, setState] = useState({ status: 'loading', items: [], nextCursor: null })
  const [awaitingCount, setAwaitingCount] = useState(null)
  const [opened, setOpened] = useState(null)

  const load = useCallback(async (nextFilter, cursor) => {
    setState((s) => (cursor ? { ...s, loadingMore: true } : { status: 'loading', items: [], nextCursor: null }))
    try {
      const { data } = await getApprovalRequests({ query: toQuery(nextFilter, cursor) })
      setState((s) => ({
        status: 'ready',
        items: cursor ? [...s.items, ...data.items] : data.items,
        nextCursor: data.nextCursor,
      }))
      if (!cursor && nextFilter.inbox === 'AWAITING_MY_APPROVAL' && !nextFilter.status && !nextFilter.type) {
        setAwaitingCount(`${data.items.length}${data.nextCursor ? '+' : ''}`)
      }
    } catch {
      setState({ status: 'error', items: [], nextCursor: null })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải hộp đầu khi mở màn
    load({ inbox: 'AWAITING_MY_APPROVAL', status: '', type: '' })
  }, [load])

  function change(next) {
    const merged = { ...filter, ...next }
    setFilter(merged)
    load(merged)
  }

  const replaceRow = useCallback((data) => {
    setState((s) => ({ ...s, items: s.items.map((r) => (r.id === data.id ? data : r)) }))
  }, [])

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader title="Yêu cầu phê duyệt" subtitle="Yêu cầu chờ bạn duyệt, yêu cầu bạn lập và yêu cầu trong phạm vi.">
        <ToggleGroup
          variant="outline"
          value={[filter.inbox]}
          onValueChange={(next) => next[0] && change({ inbox: next[0] })}
          className="flex-wrap self-start"
        >
          {INBOXES.map((box) => (
            <ToggleGroupItem key={box.value} value={box.value} className="h-10 px-4">
              {box.label}
              {box.value === 'AWAITING_MY_APPROVAL' && awaitingCount !== null && (
                <span className="tabular-nums">({awaitingCount})</span>
              )}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        <div className="grid gap-3 md:grid-cols-[repeat(2,minmax(0,16rem))]">
          <Field>
            <FieldLabel htmlFor="approval-status">Trạng thái</FieldLabel>
            <select
              id="approval-status"
              value={filter.status}
              onChange={(e) => change({ status: e.target.value })}
              className={SELECT_CLASS}
            >
              <option value="">Tất cả</option>
              {Object.entries(STATUS_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
          <Field>
            <FieldLabel htmlFor="approval-type">Loại yêu cầu</FieldLabel>
            <select
              id="approval-type"
              value={filter.type}
              onChange={(e) => change({ type: e.target.value })}
              className={SELECT_CLASS}
            >
              <option value="">Tất cả</option>
              {Object.entries(TYPE_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
        </div>
      </PageHeader>

      <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
        {state.status === 'loading' && (
          <div aria-busy="true" className="flex flex-col gap-2">
            <span className="sr-only">Đang tải yêu cầu</span>
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
          </div>
        )}
        {state.status === 'error' && (
          <Alert role="alert">
            <AlertTitle>Không tải được yêu cầu</AlertTitle>
            <AlertAction>
              <Button variant="outline" size="sm" onClick={() => load(filter)}>
                Thử lại
              </Button>
            </AlertAction>
          </Alert>
        )}
        {state.status === 'ready' && state.items.length === 0 && (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Không có yêu cầu nào</EmptyTitle>
              <EmptyDescription>Đổi hộp hoặc bộ lọc để xem yêu cầu khác.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
        {state.status === 'ready' && state.items.length > 0 && (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã</TableHead>
                  <TableHead>Loại</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Người yêu cầu</TableHead>
                  <TableHead>Hiệu lực đề nghị</TableHead>
                  <TableHead className="min-w-44">Lý do</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {state.items.map((r) => (
                  <TableRow key={r.id} className="align-top">
                    <TableCell>
                      <Button variant="link" className="h-auto p-0 tabular-nums" onClick={() => setOpened(r.id)}>
                        {r.displayCode}
                      </Button>
                    </TableCell>
                    <TableCell>{typeLabel(r.type)}</TableCell>
                    <TableCell>
                      <StatusBadge status={statusLabel(r.status)} />
                    </TableCell>
                    <TableCell>{actorLabel(r.requester)}</TableCell>
                    <TableCell className="tabular-nums">{formatDate(r.requestedEffectiveDate)}</TableCell>
                    <TableCell className="whitespace-normal">{r.reason}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {state.nextCursor && (
              <Button
                variant="outline"
                className="self-start"
                disabled={state.loadingMore}
                onClick={() => load(filter, state.nextCursor)}
              >
                Xem thêm
              </Button>
            )}
          </>
        )}
      </section>

      <Sheet open={opened !== null} onOpenChange={(open) => !open && setOpened(null)}>
        <SheetContent className="sm:max-w-lg">
          {opened !== null && <ApprovalRequestDetail key={opened} id={opened} onChanged={replaceRow} />}
        </SheetContent>
      </Sheet>
    </div>
  )
}
