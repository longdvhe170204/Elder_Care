import { useCallback, useEffect, useState } from 'react'
import { getMetricDetailsByMetricCode } from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'

// Danh sách bản ghi tạo nên một con số (016 FR-009; DESIGN.md `metric-detail-sheet`). Có khoảng thì như báo cáo, không
// có thì như dashboard. Dòng người cao tuổi ngoài phạm vi hiện hành ở dạng rút gọn (Q-196), không mở được bản ghi.

const PAGE_SIZE = 50

function resultText(result) {
  return Object.entries(result ?? {})
    .map(([k, v]) => `${k}: ${v}`)
    .join(', ')
}

function DetailBody({ metric, from, to, breakdownKey }) {
  const [page, setPage] = useState(1)
  const [state, setState] = useState({ status: 'loading' })

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data } = await getMetricDetailsByMetricCode({
        path: { metricCode: metric.code },
        query: { from, to, breakdownKey, page },
      })
      setState({ status: 'ready', detail: data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [metric.code, from, to, breakdownKey, page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở khung hoặc đổi trang
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2 p-6">
        <span className="sr-only">Đang tải danh sách bản ghi</span>
        <Skeleton className="h-10" />
        <Skeleton className="h-10" />
      </div>
    )
  }
  if (state.status === 'error') {
    return (
      <div className="p-6">
        <ProblemAlert problem={state.problem} title="Không tải được danh sách bản ghi" />
      </div>
    )
  }
  const { total, rows } = state.detail
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE))
  return (
    <div className="flex flex-col gap-4 px-6 pb-6">
      <p className="type-body-sm text-muted-foreground">{total} bản ghi</p>
      {rows.length === 0 ? (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không có bản ghi</EmptyTitle>
            <EmptyDescription>Con số này bằng 0 trong phạm vi và khoảng đã chọn.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Thời điểm</TableHead>
              <TableHead>Tầng/khu</TableHead>
              <TableHead>Người cao tuổi</TableHead>
              <TableHead>Người thực hiện</TableHead>
              <TableHead>Kết quả</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.sourceKey}>
                <TableCell className="tabular-nums">{r.referenceTime ? formatDateTime(r.referenceTime) : '—'}</TableCell>
                <TableCell>{r.floorArea ?? '—'}</TableCell>
                <TableCell>
                  <div className="flex flex-col">
                    <span>{r.resident ?? '—'}</span>
                    {r.reduced && <span className="type-caption text-muted-foreground">Ngoài phạm vi hiện hành</span>}
                  </div>
                </TableCell>
                <TableCell>{r.performedBy ?? '—'}</TableCell>
                <TableCell>
                  <div className="flex flex-wrap items-center gap-2">
                    {r.status && <span>{r.status}</span>}
                    {resultText(r.result) && <span>{resultText(r.result)}</span>}
                    {r.pendingReview && <StatusBadge status="Chờ xem lại" />}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {pages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => setPage(page - 1)}>
            Trang trước
          </Button>
          <span className="type-body-sm tabular-nums">
            Trang {page}/{pages}
          </span>
          <Button variant="outline" size="sm" disabled={page >= pages} onClick={() => setPage(page + 1)}>
            Trang sau
          </Button>
        </div>
      )}
    </div>
  )
}

// opened: { metric: {code, name}, from?, to?, breakdownKey?, valueText } hoặc null
export function MetricDetailSheet({ opened, onClose }) {
  // giữ nội dung lần mở gần nhất để khung không trống trong lúc trượt ra khi đóng
  const [shown, setShown] = useState(opened)
  if (opened && opened !== shown) setShown(opened)
  return (
    <Sheet open={opened !== null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="sm:max-w-3xl">
        {shown && (
          <>
            <SheetHeader>
              <SheetTitle>{shown.metric.name}</SheetTitle>
              <SheetDescription>
                {shown.breakdownKey ? `${shown.breakdownKey}: ` : ''}
                {shown.valueText}
              </SheetDescription>
            </SheetHeader>
            <DetailBody
              key={`${shown.metric.code}|${shown.breakdownKey ?? ''}|${shown.from ?? ''}|${shown.to ?? ''}`}
              metric={shown.metric}
              from={shown.from}
              to={shown.to}
              breakdownKey={shown.breakdownKey}
            />
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}
