import { useEffect, useState } from 'react'
import { getCostsItemsById } from '@/api'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { COMMAND, COST_TYPE, ITEM_STATUS, money, ORIGIN } from './chargeLabels'
import { usePermissions } from '@/common/permissions/permissions'
import { CostAdjustmentForm } from './CostAdjustmentForm'

export function CostItemSheet({ id, onClose, onAction, canUpdate, canApprove, statementStatus, onDenied, onAdjusted }) {
  const { can } = usePermissions()
  const [adjusting, setAdjusting] = useState(false)
  const [item, setItem] = useState(null)
  const [failure, setFailure] = useState(null)
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    getCostsItemsById({ path: { id } })
      .then(({ data }) => {
        if (active) setItem(data)
      })
      .catch((error) => {
        if (active) {
          if (error.status === 403) onDenied()
          else setFailure(problemOf(error))
        }
      })
    return () => {
      active = false
    }
  }, [id, onDenied, revision])
  return (
    <Sheet open onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle className="pr-8">Khoản #{id}</SheetTitle>
        <SheetDescription>Giá trị và lịch sử của khoản chi phí.</SheetDescription>
        <ProblemAlert problem={failure} />
        {!item ? (
          !failure && <Skeleton className="h-32 w-full" />
        ) : (
          <div className="flex flex-col gap-4 type-body-sm">
            <StatusBadge status={ITEM_STATUS[item.status]} />
            <h3 className="type-title">{item.description}</h3>
            {item.itemCode && <p>Mã vật phẩm: {item.itemCode}</p>}
            {item.medicationName && !item.hiddenFields?.includes('medicationName') && (
              <p>Tên thuốc: {item.medicationName}</p>
            )}
            <dl className="grid grid-cols-2 gap-2">
              <dt>Loại</dt>
              <dd>{COST_TYPE[item.costType]}</dd>
              <dt>Nguồn</dt>
              <dd>{ORIGIN[item.origin]}</dd>
              <dt>Ngày phát sinh</dt>
              <dd>{formatDate(item.incurredOn)}</dd>
              <dt>Số lượng</dt>
              <dd>
                {item.quantity} {item.unit}
              </dd>
              <dt>Đơn giá</dt>
              <dd>{item.unitPrice == null ? 'Chưa có đơn giá' : money(item.unitPrice)}</dd>
              <dt>Thành tiền</dt>
              <dd>{money(item.amount)}</dd>
              {item.originalItemId && (
                <>
                  <dt>Khoản gốc</dt>
                  <dd>
                    #{item.originalItemId}, kỳ {item.originalPeriod}
                  </dd>
                </>
              )}
            </dl>
            <h3 className="type-card-title">Lịch sử kiểm tra, duyệt</h3>
            {can('COST_ADJUSTMENT', 'CREATE') &&
              item.status !== 'CANCELLED' &&
              (adjusting ? (
                <CostAdjustmentForm
                  key={id}
                  itemId={id}
                  onCreated={() => {
                    setRevision((value) => value + 1)
                    onAdjusted?.()
                  }}
                />
              ) : (
                <Button variant="outline" onClick={() => setAdjusting(true)}>
                  Lập điều chỉnh
                </Button>
              ))}
            {item.commandHistory?.length ? (
              <ol className="flex flex-col gap-3">
                {item.commandHistory.map((entry, index) => (
                  <li key={index} className="flex flex-col gap-1">
                    <span>{COMMAND[entry.operation]}</span>
                    <span>Tài khoản #{entry.actorId}</span>
                    <time dateTime={entry.at}>{formatDateTime(entry.at)}</time>
                    {entry.reason && <p>Lý do: {entry.reason}</p>}
                  </li>
                ))}
              </ol>
            ) : (
              <p>Chưa có lệnh kiểm tra, duyệt được ghi.</p>
            )}
            <h3 className="type-card-title">Lịch sử tính lại</h3>
            {item.recalculationHistory?.length ? (
              item.recalculationHistory.map((entry, index) => (
                <p key={index}>
                  {entry.cause}: {money(entry.before?.amount)} → {money(entry.after?.amount)}
                </p>
              ))
            ) : (
              <p>Chưa có lần tính lại.</p>
            )}
            {statementStatus !== 'FINALIZED' && (
              <div className="flex flex-wrap gap-2">
                {canApprove && item.status === 'CHECKED' && (
                  <Button onClick={() => onAction('APPROVE', item)}>Duyệt khoản</Button>
                )}
                {canApprove && ['CHECKED', 'APPROVED'].includes(item.status) && (
                  <Button variant="outline" onClick={() => onAction('RETURN', item)}>
                    Trả lại khoản
                  </Button>
                )}
                {canUpdate && item.status === 'CHECKED' && statementStatus === 'OPEN' && (
                  <Button variant="outline" onClick={() => onAction('UNCHECK', item)}>
                    Bỏ kiểm tra
                  </Button>
                )}
                {canUpdate && item.status === 'DRAFT' && (
                  <Button variant="destructive" onClick={() => onAction('CANCEL', item)}>
                    Hủy khoản
                  </Button>
                )}
              </div>
            )}
          </div>
        )}
      </SheetContent>
    </Sheet>
  )
}
