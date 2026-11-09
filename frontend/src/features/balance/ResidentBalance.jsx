import { useCallback, useEffect, useState } from 'react'
import { getResidentsByResidentIdBalance, getResidentsByResidentIdBalanceTransactions } from '@/api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { OffsetForm } from './OffsetForm'
import { ResidentNotices } from './ResidentNotices'
import { SettlementPanel } from './SettlementPanel'
import { TransactionForm } from './TransactionForm'
import {
  BALANCE_STATUS_LABELS,
  DEPOSIT_STATUS_LABELS,
  LEDGER_LABELS,
  TRANSACTION_STATUS_LABELS,
  TYPE_LABELS,
  formatMoney,
  labelOf,
} from './balanceLabels'

// Số dư, giao dịch, quyết toán và phiếu báo phí của một người cao tuổi (017 T027, T033, T036; FR-002 → FR-004, FR-010 →
// FR-015, BR-M11-10, 14). Hành chính chỉ nhận số dư, số dư cọc, trạng thái đặt cọc (hiddenFields liệt kê phần ẩn).

const STATUS_TONES = { NORMAL: 'settled', RUNNING_LOW: 'attention', IN_DEBT: 'urgent' }

function Figure({ label, children }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="type-caption text-muted-foreground">{label}</span>
      <span className="type-metric tabular-nums">{children}</span>
    </div>
  )
}

export function ResidentBalance({ residentId, fullName }) {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading' })
  const [sheet, setSheet] = useState(null)
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    try {
      const [balance, transactions] = await Promise.all([
        getResidentsByResidentIdBalance({ path: { residentId } }),
        getResidentsByResidentIdBalanceTransactions({ path: { residentId } }),
      ])
      setState({ status: 'ready', balance: balance.data, transactions: transactions.data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải số dư khi mở người cao tuổi
    load()
  }, [load])

  function done(message) {
    setSheet(null)
    setNotice(message)
    load()
  }

  const canCreate = can('BALANCE_LEDGER', 'CREATE')
  const balance = state.balance
  return (
    <div className="flex flex-col gap-5">
      <h2 className="type-section">Số dư của {fullName}</h2>
      {notice && (
        <Alert role="status">
          <AlertDescription>{notice}</AlertDescription>
        </Alert>
      )}
      <Card>
        <CardContent className="flex flex-col gap-4">
          {state.status === 'loading' && <Skeleton className="h-24 w-full" />}
          {state.status === 'error' && (
            <div className="flex flex-col gap-2">
              <ProblemAlert problem={state.problem} />
              <Button variant="outline" className="h-11 self-start" onClick={load}>
                Thử lại
              </Button>
            </div>
          )}
          {state.status === 'ready' && (
            <>
              <div className="grid gap-4 sm:grid-cols-3">
                <Figure label="Số dư">{formatMoney(balance.balance)}</Figure>
                <Figure label="Số dư cọc">{formatMoney(balance.depositBalance)}</Figure>
                {balance.depositStatus && (
                  <Figure label="Đặt cọc">{labelOf(DEPOSIT_STATUS_LABELS, balance.depositStatus)}</Figure>
                )}
              </div>
              <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
                {balance.status && (
                  <Badge variant={STATUS_TONES[balance.status] ?? 'neutral'}>
                    {labelOf(BALANCE_STATUS_LABELS, balance.status)}
                  </Badge>
                )}
                {balance.debt != null && <span className="type-body">Còn nợ {formatMoney(balance.debt)}</span>}
                {balance.daysCovered != null && (
                  <span className="type-body">Đủ tiền cho {balance.daysCovered} ngày</span>
                )}
                {balance.unfinalizedTotal != null && (
                  <span className="type-body-sm text-muted-foreground">
                    Chi phí chưa chốt {formatMoney(balance.unfinalizedTotal)}
                  </span>
                )}
                {balance.paymentCode && (
                  <span className="type-body-sm text-muted-foreground">
                    Mã nộp tiền <span className="tabular-nums">{balance.paymentCode}</span>
                  </span>
                )}
              </div>
              {canCreate && (
                <div className="flex flex-wrap gap-2">
                  <Button className="h-11" onClick={() => setSheet('transaction')}>
                    Lập hoàn tiền, điều chỉnh
                  </Button>
                  <Button variant="outline" className="h-11" onClick={() => setSheet('offset')}>
                    Cấn trừ tiền cọc
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {state.status === 'ready' && (
        <Card>
          <CardContent className="flex flex-col gap-3">
            <h3 className="type-section">Giao dịch</h3>
            {state.transactions.length === 0 ? (
              <Empty>
                <EmptyHeader>
                  <EmptyTitle>Chưa có giao dịch</EmptyTitle>
                  <EmptyDescription>Nộp tiền, thanh toán bảng chi phí, hoàn tiền sẽ hiện ở đây.</EmptyDescription>
                </EmptyHeader>
              </Empty>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Thời điểm</TableHead>
                    <TableHead>Sổ</TableHead>
                    <TableHead>Loại</TableHead>
                    <TableHead className="text-right">Số tiền</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Người lập</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {state.transactions.map((t) => (
                    <TableRow key={t.id}>
                      <TableCell className="tabular-nums">{formatDateTime(t.at)}</TableCell>
                      <TableCell>{labelOf(LEDGER_LABELS, t.ledger)}</TableCell>
                      <TableCell>{labelOf(TYPE_LABELS, t.type)}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatMoney(t.amount)}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            t.status === 'PENDING_APPROVAL'
                              ? 'attention'
                              : t.status === 'CONFIRMED'
                                ? 'settled'
                                : 'neutral'
                          }
                        >
                          {labelOf(TRANSACTION_STATUS_LABELS, t.status)}
                        </Badge>
                      </TableCell>
                      <TableCell>{t.createdBy}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      )}

      {can('SETTLEMENT_RECORD', 'VIEW') && (
        <Card>
          <CardContent className="flex flex-col gap-3">
            <h3 className="type-section">Quyết toán</h3>
            <SettlementPanel residentId={residentId} />
          </CardContent>
        </Card>
      )}
      {can('PAYMENT_NOTICE', 'VIEW') && (
        <Card>
          <CardContent className="flex flex-col gap-3">
            <h3 className="type-section">Phiếu báo phí đã phát hành</h3>
            <ResidentNotices residentId={residentId} />
          </CardContent>
        </Card>
      )}

      <Sheet open={sheet != null} onOpenChange={(next) => !next && setSheet(null)}>
        <SheetContent className="overflow-y-auto p-6 sm:max-w-lg [&_[data-slot=sheet-title]]:pr-8">
          <SheetHeader className="p-0">
            <SheetTitle className="type-title">
              {sheet === 'offset' ? 'Cấn trừ tiền cọc' : 'Lập hoàn tiền, điều chỉnh'}
            </SheetTitle>
            <SheetDescription>{fullName}</SheetDescription>
          </SheetHeader>
          {sheet === 'transaction' && <TransactionForm residentId={residentId} onDone={done} />}
          {sheet === 'offset' && <OffsetForm residentId={residentId} onDone={done} />}
        </SheetContent>
      </Sheet>
    </div>
  )
}
