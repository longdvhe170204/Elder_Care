import { useRef, useState } from 'react'
import { getCostsPostFinalizationChanges } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card } from '@/components/ui/card'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { CostAdjustmentForm } from './CostAdjustmentForm'

function SourceSnapshot({ value = {} }) {
  const day = value.day
  const session = value.session
  const quantity = value.quantity ?? value.source?.quantity
  return (
    <dl className="space-y-1">
      {quantity != null && (
        <div>
          <dt>Số lượng</dt>
          <dd>{quantity}</dd>
        </div>
      )}
      {value.cancelled != null && (
        <div>
          <dt>Ghi nhận nguồn</dt>
          <dd>{value.cancelled ? 'Đã hủy' : 'Còn hiệu lực'}</dd>
        </div>
      )}
      {day?.date && (
        <div>
          <dt>Ngày lưu trú</dt>
          <dd>{formatDate(day.date)}</dd>
        </div>
      )}
      {day?.presence && (
        <div>
          <dt>Có mặt</dt>
          <dd>{day.presence === 'PRESENT' ? 'Có mặt' : day.presence === 'ABSENT' ? 'Vắng' : day.presence}</dd>
        </div>
      )}
      {day?.bedReleased != null && (
        <div>
          <dt>Giường</dt>
          <dd>{day.bedReleased ? 'Đã giải phóng' : 'Chưa giải phóng'}</dd>
        </div>
      )}
      {day?.outsideBillingPeriod != null && (
        <div>
          <dt>Khoảng tính phí</dt>
          <dd>{day.outsideBillingPeriod ? 'Ngoài khoảng tính phí' : 'Trong khoảng tính phí'}</dd>
        </div>
      )}
      {session?.date && (
        <div>
          <dt>Ngày bán trú</dt>
          <dd>{formatDate(session.date)}</dd>
        </div>
      )}
      {session?.coefficientPercent != null && (
        <div>
          <dt>Hệ số buổi</dt>
          <dd>{session.coefficientPercent}%</dd>
        </div>
      )}
      {session?.closureDay != null && (
        <div>
          <dt>Lịch khu bán trú</dt>
          <dd>{session.closureDay ? 'Ngày nghỉ' : 'Ngày hoạt động'}</dd>
        </div>
      )}
      {!day && !session && quantity == null && value.cancelled == null && <dd>Xem căn cứ nguồn của khoản gốc.</dd>}
    </dl>
  )
}

export function PostFinalizationChanges() {
  const [resident, setResident] = useState('')
  const [rows, setRows] = useState(null)
  const [failure, setFailure] = useState(null)
  const [pending, setPending] = useState(false)
  const [selected, setSelected] = useState(null)
  const busy = useRef(false)
  async function search(event) {
    event.preventDefault()
    if (busy.current) return
    busy.current = true
    setPending(true)
    setSelected(null)
    setRows(null)
    setFailure(null)
    try {
      const { data } = await getCostsPostFinalizationChanges({
        query: resident ? { residentId: Number(resident) } : {},
      })
      setRows(data)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }
  return (
    <Card className="space-y-4 p-4">
      <form onSubmit={search} className="space-y-3">
        <label className="type-body-sm">
          Mã người cao tuổi (không bắt buộc)
          <Input
            type="number"
            min="1"
            value={resident}
            disabled={pending}
            onChange={(e) => {
              setResident(e.target.value)
              setRows(null)
              setSelected(null)
            }}
          />
        </label>
        <Button type="submit" disabled={pending}>
          Tra cứu biến động
        </Button>
      </form>
      <ProblemAlert problem={failure} />
      {rows?.length === 0 && <p className="type-body-sm">Không có biến động sau chốt.</p>}
      {rows?.map((row) => (
        <div key={row.id} className="space-y-2 border-b border-line py-3 type-body-sm">
          <p>
            Khoản #{row.itemId} · {formatDateTime(row.occurredAt)}
          </p>
          <p>Căn cứ: {row.reference}</p>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <section>
              <h3 className="type-card-title">Trước thay đổi</h3>
              <SourceSnapshot value={row.before} />
            </section>
            <section>
              <h3 className="type-card-title">Sau thay đổi</h3>
              <SourceSnapshot value={row.after} />
            </section>
          </div>
          <Button variant="outline" onClick={() => setSelected(row)}>
            Điều chỉnh khoản #{row.itemId}
          </Button>
        </div>
      ))}
      {selected && <CostAdjustmentForm key={selected.id} itemId={selected.itemId} sourceChangeId={selected.id} />}
    </Card>
  )
}
