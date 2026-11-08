import { useCallback, useEffect, useState } from 'react'
import { getBalanceTransactionsPendingApproval } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { LEDGER_LABELS, TYPE_LABELS, formatMoney, labelOf } from './balanceLabels'

// Giao dịch Chờ duyệt (017 T027; FR-013): hoàn tiền, hoàn cọc, điều chỉnh do Kế toán lập. Quản lý viện duyệt hoặc từ chối
// ở màn Yêu cầu phê duyệt của 000 (mã yêu cầu ghi ở cột cuối); số dư chưa đổi cho tới khi duyệt.

export function PendingTransactions() {
  const [state, setState] = useState({ status: 'loading', items: [] })

  const load = useCallback(async () => {
    try {
      const { data } = await getBalanceTransactionsPendingApproval()
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi mở màn
    load()
  }, [load])

  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        {state.status === 'loading' && <Skeleton className="h-32 w-full" />}
        {state.status === 'error' && (
          <div className="flex flex-col gap-2">
            <ProblemAlert problem={state.problem} />
            <Button variant="outline" className="h-11 self-start" onClick={load}>
              Thử lại
            </Button>
          </div>
        )}
        {state.status === 'ready' && state.items.length === 0 && (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Không có giao dịch chờ duyệt</EmptyTitle>
              <EmptyDescription>
                Hoàn tiền, hoàn cọc và điều chỉnh đang chờ Quản lý viện duyệt sẽ hiện ở đây.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
        {state.status === 'ready' && state.items.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Loại</TableHead>
                <TableHead>Sổ</TableHead>
                <TableHead className="text-right">Số tiền</TableHead>
                <TableHead>Lý do</TableHead>
                <TableHead>Lập bởi</TableHead>
                <TableHead>Yêu cầu phê duyệt</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {state.items.map((t) => (
                <TableRow key={t.id}>
                  <TableCell>
                    {labelOf(TYPE_LABELS, t.type)} <Badge variant="attention">Chờ duyệt</Badge>
                  </TableCell>
                  <TableCell>{labelOf(LEDGER_LABELS, t.ledger)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatMoney(t.amount)}</TableCell>
                  <TableCell>{t.reason}</TableCell>
                  <TableCell className="tabular-nums">
                    {t.createdBy}
                    <span className="type-caption block text-muted-foreground">{formatDateTime(t.at)}</span>
                  </TableCell>
                  <TableCell className="tabular-nums">
                    {t.approvalRequestId != null ? `#${t.approvalRequestId}` : '—'}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}
