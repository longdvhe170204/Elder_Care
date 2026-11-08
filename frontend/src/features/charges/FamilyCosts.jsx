import { useEffect, useRef, useState } from 'react'
import { getResidentsByResidentIdCostsStatements, getResidentsByResidentIdCostsProvisional } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { COST_TYPE, money, periodLabel } from './chargeLabels'

// A nối vào cổng 012; component nhận người đang chọn, không nhập chéo feature.
export function FamilyCosts({ residentId }) {
  return <FamilyCostContent key={residentId} residentId={residentId} />
}
function FamilyCostContent({ residentId }) {
  const [rows, setRows] = useState(null)
  const [summary, setSummary] = useState(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [failure, setFailure] = useState(null)
  const [denied, setDenied] = useState(false)
  const [busy, setBusy] = useState(false)
  const [revision, setRevision] = useState(0)
  const active = useRef(true)
  useEffect(() => {
    active.current = true
    getResidentsByResidentIdCostsStatements({ path: { residentId } })
      .then(({ data }) => {
        if (active.current) setRows(data)
      })
      .catch((error) => {
        if (active.current) {
          setFailure(problemOf(error))
          setDenied(error.status === 403)
        }
      })
    return () => {
      active.current = false
    }
  }, [residentId, revision])
  async function provisional(event) {
    event.preventDefault()
    setBusy(true)
    setSummary(null)
    setFailure(null)
    try {
      const { data } = await getResidentsByResidentIdCostsProvisional({
        path: { residentId },
        query: { from: from || undefined, to: to || undefined },
      })
      if (active.current) setSummary(data)
    } catch (error) {
      if (active.current) {
        setFailure(problemOf(error))
        if (error.status === 403) {
          setDenied(true)
          setRows(null)
        }
      }
    } finally {
      if (active.current) setBusy(false)
    }
  }
  if (denied) return <PermissionDeniedState label="Chi phí" />
  return (
    <section className="space-y-4" aria-label="Chi phí người thân">
      <h2 className="type-title">Chi phí</h2>
      <ProblemAlert problem={failure} />
      {failure && (
        <Button variant="outline" onClick={() => setRevision((n) => n + 1)}>
          Tải lại
        </Button>
      )}
      <form onSubmit={provisional} className="space-y-3 rounded-lg border bg-card p-4">
        <h3 className="type-card-title">Tạm tính, chưa chốt</h3>
        <p className="type-body-sm">Chọn khoảng ngày trong kỳ đang mở; để trống để xem từ đầu tháng tới hôm nay.</p>
        <label className="flex flex-col gap-2">
          Từ ngày
          <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label className="flex flex-col gap-2">
          Đến ngày
          <Input type="date" min={from} value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
        <Button disabled={busy} type="submit">
          {busy ? 'Đang tải…' : 'Xem tạm tính'}
        </Button>
        {summary && (
          <div role="status">
            <Totals values={summary.totalsByType} />
            {summary.hasUnpricedItems && <p>Còn khoản chưa có đơn giá</p>}
          </div>
        )}
      </form>
      <h3 className="type-card-title">Bảng đã chốt</h3>
      {rows === null && !failure && <p role="status">Đang tải bảng chi phí…</p>}
      {rows?.length === 0 && <p>Chưa có bảng chi phí đã chốt.</p>}
      {rows?.map((row) => (
        <details className="rounded-lg border bg-card p-4" key={row.id}>
          <summary className="min-h-11 cursor-pointer type-label">
            Kỳ {periodLabel(row.period)} ·{' '}
            {row.statementType === 'SUPPLEMENTARY' ? 'Bổ sung' : row.finalPeriod ? 'Kỳ cuối' : 'Bảng thường'} ·{' '}
            {money(row.total)}
          </summary>
          <Totals values={row.totalsByType} />
          <ul className="divide-y">
            {row.items?.map((item) => (
              <li key={item.id} className="py-3">
                <p>
                  {item.medicationName || item.description} · {money(item.amount)}
                </p>
                <p className="type-body-sm text-muted-foreground">
                  {COST_TYPE[item.costType]} · {formatDate(item.incurredOn)} · {item.quantity} {item.unit} · Đơn giá{' '}
                  {money(item.unitPrice)}
                </p>
                {item.originalPeriod && <p>Điều chỉnh kỳ {periodLabel(item.originalPeriod)}</p>}
              </li>
            ))}
          </ul>
        </details>
      ))}
    </section>
  )
}
function Totals({ values = [] }) {
  return (
    <dl>
      {values.map((row) => (
        <div key={row.type} className="flex justify-between gap-3 py-2">
          <dt>{COST_TYPE[row.type]}</dt>
          <dd>{money(row.total)}</dd>
        </div>
      ))}
    </dl>
  )
}
