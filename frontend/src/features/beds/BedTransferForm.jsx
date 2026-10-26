import { useEffect, useRef, useState } from 'react'
import { getBedAllocationsSuitableBeds, getBedTransferReasons, postBedAllocationsByIdTransfer } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

const selectClass = 'h-11 w-full rounded-md border bg-background px-3'

export function BedTransferForm({ allocation, onSaved, onClose, onBusyChange }) {
  const [bedId, setBedId] = useState('')
  const [reasonId, setReasonId] = useState('')
  const [reason, setReason] = useState('')
  const [localTime, setLocalTime] = useState('')
  const [revision, setRevision] = useState(0)
  const [options, setOptions] = useState({ loading: true, beds: [], reasons: [] })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const pending = useRef(false)
  const command = useRef(null)
  const transferAt = localTime ? `${localTime.length === 16 ? `${localTime}:00` : localTime}+07:00` : undefined
  useEffect(() => {
    let current = true
    Promise.all([
      getBedAllocationsSuitableBeds({
        query: { residentId: allocation.residentId, fromAllocationId: allocation.id, startAt: transferAt },
      }),
      getBedTransferReasons(),
    ])
      .then(([beds, reasons]) => {
        if (current)
          setOptions({
            loading: false,
            beds: beds.data,
            reasons: reasons.data.filter((item) => item.status === 'ACTIVE'),
          })
      })
      .catch((cause) => {
        if (current) {
          setOptions({ loading: false, beds: [], reasons: [] })
          setError(problemOf(cause))
        }
      })
    return () => {
      current = false
    }
  }, [allocation.id, allocation.residentId, transferAt, revision])
  function reload() {
    setBedId('')
    setOptions({ loading: true, beds: [], reasons: [] })
    setError(null)
    setRevision((value) => value + 1)
  }
  async function submit(event) {
    event.preventDefault()
    if (pending.current || !bedId || !reasonId || !reason.trim() || options.loading) return
    const body = {
      newBedId: Number(bedId),
      transferReasonId: Number(reasonId),
      reason: reason.trim(),
      ...(transferAt && { transferAt }),
    }
    const signature = JSON.stringify(body)
    if (command.current?.signature !== signature) command.current = { signature, key: crypto.randomUUID() }
    pending.current = true
    onBusyChange(true)
    setBusy(true)
    setError(null)
    try {
      const result = await postBedAllocationsByIdTransfer({
        path: { id: allocation.id },
        body,
        headers: { 'Idempotency-Key': command.current.key },
      })
      onSaved(result)
    } catch (cause) {
      setError(problemOf(cause))
      if (cause.status && cause.status < 500) command.current = null
    } finally {
      pending.current = false
      onBusyChange(false)
      setBusy(false)
    }
  }
  return (
    <>
      <SheetHeader className="px-0">
        <SheetTitle>Chuyển giường</SheetTitle>
        <SheetDescription>
          Hồ sơ #{allocation.residentId} · {allocation.location.label}
        </SheetDescription>
      </SheetHeader>
      <form onSubmit={submit} className="flex flex-col gap-4">
        <ProblemAlert problem={error} />
        <label className="flex flex-col gap-2" htmlFor="transfer-time">
          Thời điểm chuyển (giờ Việt Nam)
          <Input
            id="transfer-time"
            type="datetime-local"
            value={localTime}
            disabled={busy}
            onChange={(event) => {
              setBedId('')
              setOptions({ loading: true, beds: [], reasons: [] })
              setError(null)
              setLocalTime(event.target.value)
            }}
          />
        </label>
        <p className="type-caption text-muted-foreground">
          Để trống để chuyển tại thời điểm hiện tại. Nhập thời điểm thực tế khi cần nhập bù.
        </p>
        {options.loading && <p aria-busy="true">Đang tải giường phù hợp…</p>}
        {!options.loading && !options.beds.length && !error && <p>Không có giường phù hợp tại thời điểm đã chọn.</p>}
        <label className="flex flex-col gap-2" htmlFor="transfer-bed">
          Giường mới
          <select
            id="transfer-bed"
            className={selectClass}
            value={bedId}
            onChange={(event) => setBedId(event.target.value)}
            required
            disabled={busy || options.loading}
          >
            <option value="">Chọn giường phù hợp</option>
            {options.beds.map((bed) => (
              <option key={bed.location.bedId} value={bed.location.bedId}>
                {bed.location.label}
              </option>
            ))}
          </select>
        </label>
        <Button type="button" variant="outline" disabled={busy || options.loading} onClick={reload}>
          Tải lại giường phù hợp
        </Button>
        <label className="flex flex-col gap-2" htmlFor="transfer-reason">
          Lý do chuyển
          <select
            id="transfer-reason"
            className={selectClass}
            value={reasonId}
            onChange={(event) => setReasonId(event.target.value)}
            required
            disabled={busy || options.loading}
          >
            <option value="">Chọn lý do chuyển</option>
            {options.reasons.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-2" htmlFor="transfer-command-reason">
          Lý do của lệnh
          <textarea
            id="transfer-command-reason"
            className="min-h-24 rounded-md border bg-background p-3"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            required
            maxLength={2000}
            disabled={busy}
          />
        </label>
        <div className="flex flex-wrap justify-end gap-2">
          <Button type="button" variant="outline" disabled={busy} onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" disabled={busy || options.loading || !bedId || !reasonId || !reason.trim()}>
            {busy ? 'Đang gửi…' : 'Xác nhận chuyển'}
          </Button>
        </div>
      </form>
    </>
  )
}
