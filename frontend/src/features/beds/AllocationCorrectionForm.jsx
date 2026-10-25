import { useEffect, useRef, useState } from 'react'
import { getBedAllocationsById, postBedAllocationsByIdCorrections } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// T048: chỉ ba trường FR-026, phần bỏ trống giữ nguyên; không sửa bản ghi gốc.
export function AllocationCorrectionForm({ allocationId, onSaved }) {
  const [type, setType] = useState('AMEND_CONTENT')
  const [bed, setBed] = useState('')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [reason, setReason] = useState('')
  const [state, setState] = useState({})
  const command = useRef(null)
  const pending = useRef(false)
  const [record, setRecord] = useState({ loading: true })
  useEffect(() => {
    let current = true
    getBedAllocationsById({ path: { id: allocationId } })
      .then(({ data }) => current && setRecord({ status: data.status }))
      .catch((cause) => current && setRecord({ error: problemOf(cause) }))
    return () => {
      current = false
    }
  }, [allocationId])
  const stamp = (value) => `${value.length === 16 ? `${value}:00` : value}+07:00`
  async function save(event) {
    event.preventDefault()
    if (pending.current) return
    const correctedContent = {
      ...(bed && { bedId: Number(bed) }),
      ...(start && { startAt: stamp(start) }),
      ...(end && { endAt: stamp(end) }),
    }
    const body = { type, reason: reason.trim(), ...(type === 'AMEND_CONTENT' && { correctedContent }) }
    const signature = JSON.stringify(body)
    if (command.current?.signature !== signature) command.current = { signature, key: crypto.randomUUID() }
    pending.current = true
    setState({ busy: true })
    try {
      await postBedAllocationsByIdCorrections({
        path: { id: allocationId },
        body,
        headers: { 'Idempotency-Key': command.current.key },
      })
      setState({ saved: true })
      command.current = null
      onSaved?.()
    } catch (cause) {
      setState({ error: problemOf(cause) })
      if (cause.status && cause.status < 500) command.current = null
    } finally {
      pending.current = false
    }
  }
  if (record.loading)
    return (
      <div aria-busy="true">
        <span className="sr-only">Đang kiểm tra phân bổ</span>
        <Skeleton className="h-20" />
      </div>
    )
  if (record.error) return <ProblemAlert problem={record.error} />
  if (!['CLOSED', 'CANCELLED'].includes(record.status)) return <p>Chỉ đính chính phân bổ đã đóng hoặc đã hủy.</p>
  return (
    <form onSubmit={save} className="flex flex-col gap-3 rounded-xl border p-4" aria-label="Đính chính phân bổ">
      <h3 className="type-title-sm">Đính chính phân bổ</h3>
      <p className="type-body-sm">
        Bản gốc giữ nguyên. Tùy quyền của người ghi, đính chính được lưu hoặc gửi Quản lý viện duyệt.
      </p>
      <label className="flex flex-col gap-2">
        Loại đính chính
        <select
          className="h-11 rounded-md border bg-background px-3"
          value={type}
          onChange={(event) => setType(event.target.value)}
          disabled={state.busy}
        >
          <option value="AMEND_CONTENT">Sửa nội dung</option>
          <option value="VOID">Hủy ghi nhận</option>
        </select>
      </label>
      {type === 'AMEND_CONTENT' && (
        <div className="grid gap-3 sm:grid-cols-3">
          <label className="flex flex-col gap-2">
            Giường đúng
            <Input
              type="number"
              min="1"
              step="1"
              value={bed}
              onChange={(event) => setBed(event.target.value)}
              disabled={state.busy}
            />
          </label>
          <label className="flex flex-col gap-2">
            Bắt đầu đúng
            <Input
              type="datetime-local"
              value={start}
              onChange={(event) => setStart(event.target.value)}
              disabled={state.busy}
            />
          </label>
          <label className="flex flex-col gap-2">
            Kết thúc đúng
            <Input
              type="datetime-local"
              value={end}
              onChange={(event) => setEnd(event.target.value)}
              disabled={state.busy}
            />
          </label>
        </div>
      )}
      <label className="flex flex-col gap-2">
        Lý do đính chính
        <Input
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          maxLength={2000}
          required
          disabled={state.busy}
        />
      </label>
      {state.error && <ProblemAlert problem={state.error} />}
      {state.saved && (
        <p role="status">Đã tiếp nhận đính chính. Xem các lần đính chính hoặc yêu cầu phê duyệt để kiểm tra kết quả.</p>
      )}
      <Button
        className="self-start"
        type="submit"
        disabled={state.busy || !reason.trim() || (type === 'AMEND_CONTENT' && !bed && !start && !end)}
      >
        Lưu đính chính
      </Button>
    </form>
  )
}
