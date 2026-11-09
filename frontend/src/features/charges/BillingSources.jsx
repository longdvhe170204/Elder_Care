import { useRef, useState } from 'react'
import { getCostsBillingSources, postCostsItemsFromEvents } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card } from '@/components/ui/card'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { controlClass } from './chargeLabels'

const types = {
  RESIDENCY_DAY: 'Ngày lưu trú',
  DAY_CARE_SESSION: 'Buổi bán trú',
  CARE_TASK_RESULT: 'Kết quả chăm sóc',
  DOSE: 'Liều thuốc',
  PRN_ADMINISTRATION: 'Lần dùng thuốc khi cần',
  DISPENSE_RECORD: 'Lần giao thuốc',
  FAMILY_MEAL: 'Suất ăn người thân',
  OVERNIGHT_STAY: 'Đêm ở lại',
  ACTIVITY_ATTENDANCE: 'Điểm danh hoạt động',
}
const keyOf = (row) => JSON.stringify([row.sourceId, row.billingUnit])

export function BillingSources({ onChanged = () => {} }) {
  const [filter, setFilter] = useState({ residentId: '', type: 'RESIDENCY_DAY', from: '', to: '' })
  const [loaded, setLoaded] = useState(null)
  const [rows, setRows] = useState(null)
  const [selected, setSelected] = useState([])
  const [result, setResult] = useState(null)
  const [failure, setFailure] = useState(null)
  const [pending, setPending] = useState(false)
  const busy = useRef(false)
  const nonce = useRef(null)
  function change(name, value) {
    setFilter((current) => ({ ...current, [name]: value }))
    setRows(null)
    setLoaded(null)
    setSelected([])
    setResult(null)
    nonce.current = null
  }
  async function search(event) {
    event.preventDefault()
    if (busy.current) return
    busy.current = true
    setPending(true)
    setFailure(null)
    setRows(null)
    setSelected([])
    setResult(null)
    nonce.current = null
    try {
      const query = { ...filter, residentId: Number(filter.residentId) }
      const { data } = await getCostsBillingSources({ query })
      setRows(data)
      setLoaded(query)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }
  async function create() {
    if (busy.current || !selected.length || !loaded) return
    busy.current = true
    setPending(true)
    setFailure(null)
    nonce.current ??= crypto.randomUUID()
    try {
      const sourceUnits = rows
        .filter((row) => selected.includes(keyOf(row)))
        .map(({ sourceId, billingUnit }) => ({ sourceId, billingUnit }))
      const { data } = await postCostsItemsFromEvents({
        headers: { 'Idempotency-Key': nonce.current },
        body: { residentId: loaded.residentId, sourceType: loaded.type, sourceUnits },
      })
      setResult(data)
      setSelected([])
      nonce.current = null
      onChanged(`Đã lập ${data.succeededIds.length} khoản.`)
      const refreshed = await getCostsBillingSources({ query: loaded })
      setRows(refreshed.data)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }
  return (
    <Card className="space-y-4 p-4">
      <form onSubmit={search} className="grid gap-3 sm:grid-cols-2">
        <label className="type-body-sm">
          Mã người cao tuổi
          <Input
            type="number"
            min="1"
            required
            value={filter.residentId}
            disabled={pending}
            onChange={(e) => change('residentId', e.target.value)}
          />
        </label>
        <label className="type-body-sm">
          Loại nguồn
          <select
            className={controlClass}
            value={filter.type}
            disabled={pending}
            onChange={(e) => change('type', e.target.value)}
          >
            {Object.entries(types).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label className="type-body-sm">
          Từ ngày
          <Input
            type="date"
            required
            value={filter.from}
            disabled={pending}
            onChange={(e) => change('from', e.target.value)}
          />
        </label>
        <label className="type-body-sm">
          Đến ngày
          <Input
            type="date"
            required
            min={filter.from}
            value={filter.to}
            disabled={pending}
            onChange={(e) => change('to', e.target.value)}
          />
        </label>
        <Button type="submit" disabled={pending}>
          Tra cứu nguồn
        </Button>
      </form>
      <ProblemAlert problem={failure} />
      {rows?.length === 0 && <p className="type-body-sm">Không có nguồn trong khoảng đã chọn.</p>}
      {rows?.map((row) => (
        <label key={keyOf(row)} className="flex items-center gap-3 border-b border-line py-3 type-body-sm">
          <input
            type="checkbox"
            disabled={pending}
            checked={selected.includes(keyOf(row))}
            onChange={(e) => {
              setSelected((current) =>
                e.target.checked ? [...current, keyOf(row)] : current.filter((key) => key !== keyOf(row)),
              )
              nonce.current = null
            }}
          />
          <span>
            {formatDate(row.incurredOn)} · {row.description} · {row.quantity} {row.unit ?? 'Chưa có đơn vị nguồn'}
          </span>
          {row.existingItemId && <span>Khoản #{row.existingItemId}</span>}
        </label>
      ))}
      {rows && (
        <Button disabled={pending || !selected.length} onClick={create}>
          Lập khoản đã chọn ({selected.length})
        </Button>
      )}
      {result && (
        <div role="status" className="space-y-2 type-body-sm">
          <p>
            Đã lập {result.succeededIds.length} khoản; bỏ qua {result.skipped.length} nguồn.
          </p>
          {result.skipped.map((s, i) => (
            <p key={`${s.source}:${i}`}>
              {s.source}: {s.reason}
            </p>
          ))}
        </div>
      )}
    </Card>
  )
}
