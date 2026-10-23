import { useCallback, useEffect, useState } from 'react'
import { getDeliveryErrors } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { ERROR_TYPE_LABELS, label, reasonLabel } from './notificationLabels'

// Lỗi gửi (009 T035; FR-038): chỉ Quản lý viện (backend trả 403 cho vai trò khác).

export function DeliveryErrors() {
  const [type, setType] = useState('ALL')
  const [state, setState] = useState({ status: 'loading', rows: [] })

  const load = useCallback(async (t) => {
    setState({ status: 'loading', rows: [] })
    try {
      const { data } = await getDeliveryErrors({ query: { type: t === 'ALL' ? undefined : t } })
      setState({ status: 'ready', rows: data })
    } catch {
      setState({ status: 'error', rows: [] })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở tab hoặc đổi loại lỗi
    load(type)
  }, [load, type])

  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
      <ToggleGroup
        variant="outline"
        value={[type]}
        onValueChange={(next) => next[0] && setType(next[0])}
        className="flex-wrap self-start"
      >
        <ToggleGroupItem value="ALL" className="h-10 px-4">
          Tất cả
        </ToggleGroupItem>
        {Object.entries(ERROR_TYPE_LABELS).map(([value, text]) => (
          <ToggleGroupItem key={value} value={value} className="h-10 px-4">
            {text}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải lỗi gửi</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && (
        <Alert role="alert">
          <AlertTitle>Không tải được lỗi gửi</AlertTitle>
          <AlertAction>
            <Button variant="outline" size="sm" onClick={() => load(type)}>
              Thử lại
            </Button>
          </AlertAction>
        </Alert>
      )}
      {state.status === 'ready' && state.rows.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không có lỗi gửi</EmptyTitle>
            <EmptyDescription>Mọi yêu cầu loại này đều đã gửi được tới người nhận.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {state.status === 'ready' && state.rows.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tiếp nhận</TableHead>
              <TableHead>Loại lỗi</TableHead>
              <TableHead>Sự kiện</TableHead>
              <TableHead>Bản ghi nguồn</TableHead>
              <TableHead className="min-w-48">Lý do</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.rows.map((r, i) => (
              <TableRow key={`${r.type}-${r.requestId}-${r.notificationId ?? ''}-${i}`} className="align-top">
                <TableCell className="tabular-nums">{formatDateTime(r.receivedAt)}</TableCell>
                <TableCell>
                  <StatusBadge status={label(ERROR_TYPE_LABELS, r.type)} />
                </TableCell>
                <TableCell>{r.eventType}</TableCell>
                <TableCell className="whitespace-normal">
                  {r.sourceRecordType ? `${r.sourceRecordType} ${r.sourceRecordId}` : 'Không có'}
                </TableCell>
                <TableCell className="whitespace-normal">{reasonLabel(r.reason)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </section>
  )
}
