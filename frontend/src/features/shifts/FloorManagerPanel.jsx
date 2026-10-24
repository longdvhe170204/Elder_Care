import { useEffect, useRef, useState } from 'react'
import { getFloorManagerAssignments, postFloorManagerAssignments, postFloorManagerAssignmentsByIdEnd } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDate } from '@/common/dateTime'
import { Choice } from './ShiftForm'

export function FloorManagerPanel({ staff = [], canCreate = false, canUpdate = false }) {
  const [rows, setRows] = useState([]),
    [revision, setRevision] = useState(0)
  const [person, setPerson] = useState(String(staff[0]?.id ?? '')),
    [floor, setFloor] = useState('')
  const [from, setFrom] = useState(''),
    [to, setTo] = useState(''),
    [reason, setReason] = useState('')
  const [selected, setSelected] = useState(null),
    [end, setEnd] = useState(''),
    [endReason, setEndReason] = useState('')
  const [failure, setFailure] = useState(null),
    [busy, setBusy] = useState(false),
    [notice, setNotice] = useState('')
  const keys = useRef(new Map())
  function keyFor(body) {
    const text = JSON.stringify(body)
    if (!keys.current.has(text)) keys.current.set(text, crypto.randomUUID())
    return keys.current.get(text)
  }
  useEffect(() => {
    let active = true
    getFloorManagerAssignments()
      .then(({ data }) => {
        if (active) setRows(data)
      })
      .catch((e) => {
        if (active) setFailure(problemOf(e))
      })
    return () => {
      active = false
    }
  }, [revision])
  async function create(e) {
    e.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      const body = {
        staffId: Number(person),
        floorAreaId: Number(floor),
        from,
        ...(to && { to }),
        ...(reason.trim() && { reason: reason.trim() }),
      }
      await postFloorManagerAssignments({ headers: { 'Idempotency-Key': keyFor(body) }, body })
      keys.current.clear()
      setNotice('Đã giao Trưởng tầng.')
      setRevision((n) => n + 1)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  async function shorten(e) {
    e.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      const body = { to: end, reason: endReason.trim() }
      await postFloorManagerAssignmentsByIdEnd({
        path: { id: selected.id },
        body,
        headers: { 'Idempotency-Key': keyFor({ id: selected.id, ...body }) },
      })
      setSelected(null)
      setNotice('Đã cập nhật thời hạn giao.')
      setRevision((n) => n + 1)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="flex min-w-0 flex-col gap-4 [&_button]:min-h-11 [&_input]:min-h-11 [&_select]:min-h-11">
      <p className="text-muted-foreground">
        Quản lý viện giao Trưởng tầng và thời hạn; mỗi tầng có tối đa một người được giao tại một thời điểm.
      </p>
      <ProblemAlert problem={failure} />
      {notice && <p role="status">{notice}</p>}
      {!rows.length && <p>Chưa có giao Trưởng tầng trong phạm vi được xem.</p>}
      {rows.map((row) => (
        <div key={row.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border p-3">
          <div>
            <p className="font-medium">
              {staff.find((s) => s.id === row.staffId)?.fullName ?? `Nhân viên #${row.staffId}`} · Tầng/khu vực #
              {row.floorAreaId}
            </p>
            <p>
              {formatDate(row.from)} → {row.to ? formatDate(row.to) : 'Chưa có ngày kết thúc'}
            </p>
          </div>
          {canUpdate && (
            <Button
              variant="outline"
              disabled={busy}
              onClick={() => {
                setSelected(row)
                setEnd(row.to ?? '')
                setEndReason('')
              }}
            >
              Kết thúc giao #{row.id}
            </Button>
          )}
        </div>
      ))}
      {selected && (
        <form className="flex flex-col gap-3 rounded-xl border p-4" onSubmit={shorten}>
          <h3 className="type-section">Rút ngắn hoặc kết thúc giao #{selected.id}</h3>
          <Field>
            <FieldLabel htmlFor="floor-end-date">Ngày kết thúc giao #{selected.id}</FieldLabel>
            <Input id="floor-end-date" type="date" required value={end} onChange={(e) => setEnd(e.target.value)} />
          </Field>
          <p className="text-muted-foreground">
            Người hiện tại phụ trách đến hết ngày đã chọn. Giao người thay bắt đầu từ ngày kế tiếp.
          </p>
          <Field>
            <FieldLabel htmlFor="floor-end-reason">Lý do rút ngắn</FieldLabel>
            <Textarea
              id="floor-end-reason"
              required
              maxLength={2000}
              value={endReason}
              onChange={(e) => setEndReason(e.target.value)}
            />
          </Field>
          <Button type="submit" disabled={busy || !end || !endReason.trim()}>
            Xác nhận rút ngắn
          </Button>
          <Button type="button" variant="outline" onClick={() => setSelected(null)}>
            Quay lại
          </Button>
        </form>
      )}
      {canCreate && (
        <form className="flex flex-col gap-3 rounded-xl border p-4" onSubmit={create}>
          <h3 className="type-section">Giao Trưởng tầng</h3>
          <Choice
            id="floor-manager"
            label="Nhân viên được giao"
            required
            value={person}
            onChange={(e) => setPerson(e.target.value)}
          >
            {!staff.length && <option value="">Chưa có nhân viên</option>}
            {staff.map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName}
              </option>
            ))}
          </Choice>
          <Field>
            <FieldLabel htmlFor="floor-manager-area">Mã tầng / khu vực được giao</FieldLabel>
            <Input
              id="floor-manager-area"
              type="number"
              min="1"
              required
              value={floor}
              onChange={(e) => setFloor(e.target.value)}
            />
          </Field>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field>
              <FieldLabel htmlFor="floor-from">Ngày bắt đầu giao</FieldLabel>
              <Input id="floor-from" type="date" required value={from} onChange={(e) => setFrom(e.target.value)} />
            </Field>
            <Field>
              <FieldLabel htmlFor="floor-to">Ngày kết thúc (tùy chọn)</FieldLabel>
              <Input id="floor-to" type="date" min={from} value={to} onChange={(e) => setTo(e.target.value)} />
            </Field>
          </div>
          <Field>
            <FieldLabel htmlFor="floor-reason">Lý do giao (tùy chọn)</FieldLabel>
            <Textarea id="floor-reason" maxLength={2000} value={reason} onChange={(e) => setReason(e.target.value)} />
          </Field>
          <Button type="submit" disabled={busy || !person || !floor || !from}>
            Giao Trưởng tầng
          </Button>
        </form>
      )}
    </div>
  )
}
