import { useCallback, useEffect, useRef, useState } from 'react'
import { getPaymentNoticesPendingIssue, postCostsStatementsByStatementIdPaymentNotice } from '@/api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { STATEMENT_TYPE_LABELS, formatMoney, labelOf } from './balanceLabels'

// Phiếu báo phí chờ phát hành (017 T036; FR-031, BR-M11-21, DBR-37): bảng chi phí Đã chốt đã có giao dịch thanh toán mà
// chưa có phiếu. Kế toán phát hành (PDF và Excel dựng một lần ở máy chủ, mỗi bảng tối đa một phiếu); Quản lý viện xem.
// Phiếu đã phát hành xem theo người cao tuổi (ResidentNotices).

export function PaymentNotices() {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [busyId, setBusyId] = useState(null)
  const [failure, setFailure] = useState(null)
  const [notice, setNotice] = useState('')
  // Một Idempotency-Key cho mỗi bảng: bấm lại sau lỗi mạng không phát hành hai lần
  const keys = useRef(new Map())

  const load = useCallback(async () => {
    try {
      const { data } = await getPaymentNoticesPendingIssue()
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi mở màn
    load()
  }, [load])

  async function issue(item) {
    setBusyId(item.costStatementId)
    setFailure(null)
    setNotice('')
    if (!keys.current.has(item.costStatementId)) keys.current.set(item.costStatementId, crypto.randomUUID())
    try {
      const { data } = await postCostsStatementsByStatementIdPaymentNotice({
        path: { statementId: item.costStatementId },
        headers: { 'Idempotency-Key': keys.current.get(item.costStatementId) },
      })
      keys.current.delete(item.costStatementId)
      setNotice(`Đã phát hành phiếu ${data.noticeCode} cho ${item.fullName}, kỳ ${item.period}.`)
      await load()
    } catch (error) {
      setFailure(problemOf(error))
      if (error?.status === 409) keys.current.delete(item.costStatementId)
    } finally {
      setBusyId(null)
    }
  }

  const canIssue = can('PAYMENT_NOTICE', 'CREATE')
  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        {notice && (
          <Alert role="status">
            <AlertDescription>{notice}</AlertDescription>
          </Alert>
        )}
        <ProblemAlert problem={failure} />
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
              <EmptyTitle>Không có bảng chờ phát hành phiếu</EmptyTitle>
              <EmptyDescription>
                Bảng chi phí đã chốt và đã trừ vào số dư sẽ hiện ở đây cho tới khi phát hành phiếu báo phí.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
        {state.status === 'ready' && state.items.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Người cao tuổi</TableHead>
                <TableHead>Kỳ</TableHead>
                <TableHead>Loại bảng</TableHead>
                <TableHead className="text-right">Tổng</TableHead>
                <TableHead>Chốt lúc</TableHead>
                <TableHead className="w-28">
                  <span className="sr-only">Thao tác</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {state.items.map((item) => (
                <TableRow key={item.costStatementId}>
                  <TableCell>{item.fullName}</TableCell>
                  <TableCell className="tabular-nums">{item.period}</TableCell>
                  <TableCell>{labelOf(STATEMENT_TYPE_LABELS, item.statementType)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatMoney(item.total)}</TableCell>
                  <TableCell className="tabular-nums">{formatDate(item.finalizedAt)}</TableCell>
                  <TableCell>
                    {canIssue && (
                      <Button
                        variant="outline"
                        className="h-11"
                        aria-label={`Phát hành phiếu của ${item.fullName}`}
                        disabled={busyId === item.costStatementId}
                        onClick={() => issue(item)}
                      >
                        Phát hành
                      </Button>
                    )}
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
