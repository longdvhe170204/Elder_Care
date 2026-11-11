import { useEffect, useState } from 'react'
import { getResidentsByResidentIdCostsProvisional, getResidentsByResidentIdCostsStatements } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { COST_TYPE } from '@/common/costLabels'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { PortalCard } from '../PortalParts'

// P7 Chi phí, phần bảng chi phí (010 T031; FR-037, FR-038, FR-019): bảng Đã chốt có từng khoản, khoản điều chỉnh kèm kỳ
// gốc; tạm tính chỉ tổng theo loại, nhãn "tạm tính, chưa chốt". Khoản thuốc hiện tên thuốc chỉ khi backend trả (người
// xem có quyền xem sức khỏe), còn lại là mã vật phẩm. Chuyển từ features/charges/FamilyCosts (2026-11-10).

const MONEY = new Intl.NumberFormat('vi-VN')
const money = (n) => (n == null ? 'Chưa có số tiền' : `${MONEY.format(n)} đồng`)
const period = (value) => (value ? `${value.slice(5)}/${value.slice(0, 4)}` : '')
const KIND = { REGULAR: 'bảng thường', SUPPLEMENTARY: 'bảng bổ sung' }

function Totals({ values = [] }) {
  return (
    <dl className="flex flex-col">
      {values.map((row) => (
        <div key={row.type} className="flex justify-between gap-4 border-t py-2 first:border-t-0 first:pt-0">
          <dt>{COST_TYPE[row.type] ?? row.type}</dt>
          <dd className="type-label tabular-nums">{money(row.total)}</dd>
        </div>
      ))}
    </dl>
  )
}

function Statement({ row }) {
  const kind = row.finalPeriod ? 'kỳ cuối' : (KIND[row.statementType] ?? 'bảng thường')
  return (
    <PortalCard title={`Kỳ ${period(row.period)}, ${kind}`}>
      <Totals values={row.totalsByType} />
      <ul className="flex flex-col">
        {(row.items ?? []).map((item) => (
          <li key={item.id} className="flex items-start justify-between gap-4 border-t py-2">
            <span className="flex flex-col gap-0.5">
              <span>{item.medicationName || item.description}</span>
              <span className="type-caption text-muted-foreground">
                {COST_TYPE[item.costType] ?? item.costType}, {formatDate(item.incurredOn)}, {item.quantity} {item.unit},
                đơn giá {money(item.unitPrice)}
                {item.originalPeriod && `, điều chỉnh kỳ ${period(item.originalPeriod)}`}
              </span>
            </span>
            <span className="type-label tabular-nums">{money(item.amount)}</span>
          </li>
        ))}
        <li className="flex items-center justify-between gap-4 border-t pt-2">
          <span className="type-label">Tổng</span>
          <span className="type-card-title tabular-nums">{money(row.total)}</span>
        </li>
      </ul>
    </PortalCard>
  )
}

function Provisional({ residentId }) {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [state, setState] = useState({ status: 'idle' })
  async function submit(event) {
    event.preventDefault()
    setState({ status: 'loading' })
    try {
      const { data } = await getResidentsByResidentIdCostsProvisional({
        path: { residentId },
        query: { from: from || undefined, to: to || undefined },
      })
      setState({ status: 'ready', data })
    } catch (error) {
      setState({ status: 'error', error })
    }
  }
  return (
    <PortalCard title="Tạm tính, chưa chốt">
      <form onSubmit={submit} className="flex flex-col gap-3">
        <p className="text-muted-foreground">
          Chọn khoảng ngày trong kỳ đang mở; để trống để xem từ đầu tháng tới hôm nay. Chỉ có tổng theo loại.
        </p>
        <label className="flex flex-col gap-1">
          Từ ngày
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label className="flex flex-col gap-1">
          Đến ngày
          <Input type="date" min={from} value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
        <Button type="submit" variant="outline" disabled={state.status === 'loading'}>
          Xem tạm tính
        </Button>
      </form>
      {state.status === 'error' && <ProblemAlert problem={problemOf(state.error)} />}
      {state.status === 'ready' && (
        <div role="status" className="flex flex-col gap-2">
          <Totals values={state.data.totalsByType} />
          {state.data.hasUnpricedItems && <p>Còn khoản chưa có đơn giá, chưa được cộng.</p>}
        </div>
      )}
    </PortalCard>
  )
}

export function LiveCostStatements({ residentId }) {
  const [state, setState] = useState({ status: 'loading' })
  useEffect(() => {
    let cancelled = false
    getResidentsByResidentIdCostsStatements({ path: { residentId } })
      .then(({ data }) => !cancelled && setState({ status: 'ready', data }))
      .catch((error) => !cancelled && setState({ status: 'error', error }))
    return () => {
      cancelled = true
    }
  }, [residentId])
  return (
    <>
      <Provisional residentId={residentId} />
      <h2 className="type-title">Bảng chi phí đã chốt</h2>
      {state.status === 'loading' && <Skeleton className="h-32 w-full" />}
      {state.status === 'error' && (
        <PortalCard>
          <ProblemAlert problem={problemOf(state.error)} />
        </PortalCard>
      )}
      {state.status === 'ready' &&
        (state.data.length === 0 ? (
          <PortalCard>
            <p className="text-muted-foreground">Chưa có bảng chi phí đã chốt.</p>
          </PortalCard>
        ) : (
          state.data.map((row) => <Statement key={row.id} row={row} />)
        ))}
    </>
  )
}
