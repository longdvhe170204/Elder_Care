import { useEffect, useRef, useState } from 'react'
import { getBedAllocationsSuitableBeds, postBedAllocations } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'

// T028: người dùng tự chọn giường; đổi thời điểm hoặc tải lại luôn bỏ lựa chọn cũ.
export function BedAllocationForm({ resident, onSaved }) {
  const [bedId, setBedId] = useState('')
  const [reason, setReason] = useState('')
  const [localTime, setLocalTime] = useState('')
  const [revision, setRevision] = useState(0)
  const [options, setOptions] = useState({ loading: true, beds: [] })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const pending = useRef(false)
  const command = useRef(null)
  const startAt = localTime ? `${localTime.length === 16 ? `${localTime}:00` : localTime}+07:00` : undefined
  useEffect(() => {
    let current = true
    getBedAllocationsSuitableBeds({ query: { residentId: resident.id, startAt } })
      .then(({ data }) => current && setOptions({ loading: false, beds: data }))
      .catch((cause) => {
        if (current) {
          setOptions({ loading: false, beds: [] })
          setError(problemOf(cause))
        }
      })
    return () => {
      current = false
    }
  }, [resident.id, startAt, revision])
  function reload() {
    setBedId('')
    setError(null)
    setOptions({ loading: true, beds: [] })
    setRevision((value) => value + 1)
  }
  async function submit(event) {
    event.preventDefault()
    if (pending.current || options.loading || !bedId || !reason.trim()) return
    const body = { residentId: resident.id, bedId: Number(bedId), reason: reason.trim(), ...(startAt && { startAt }) }
    const signature = JSON.stringify(body)
    if (command.current?.signature !== signature) command.current = { signature, key: crypto.randomUUID() }
    pending.current = true
    setBusy(true)
    setError(null)
    try {
      const result = await postBedAllocations({ body, headers: { 'Idempotency-Key': command.current.key } })
      onSaved(result.data)
    } catch (cause) {
      setError(problemOf(cause))
      if (cause.status && cause.status < 500) command.current = null
    } finally {
      pending.current = false
      setBusy(false)
    }
  }
  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-4 rounded-xl border bg-card p-4"
      aria-label="Chọn giường phân bổ"
    >
      <h2 className="type-title-sm">
        Phân bổ cho {resident.fullName} {resident.profileCode && `(${resident.profileCode})`}
      </h2>
      {resident.status === 'ADMISSION_IN_PROGRESS' && (
        <label className="flex flex-col gap-2">
          Bắt đầu dự kiến
          <Input
            type="datetime-local"
            value={localTime}
            disabled={busy}
            onChange={(event) => {
              setLocalTime(event.target.value)
              setBedId('')
              setOptions({ loading: true, beds: [] })
              setError(null)
            }}
          />
        </label>
      )}
      <label className="flex flex-col gap-2">
        Giường phù hợp
        <select
          className="h-11 w-full rounded-md border bg-background px-3"
          value={bedId}
          disabled={busy || options.loading}
          onChange={(event) => setBedId(event.target.value)}
        >
          <option value="">Chọn giường</option>
          {options.beds.map((bed) => (
            <option key={bed.location.bedId} value={bed.location.bedId}>
              {bed.location.label}
            </option>
          ))}
        </select>
      </label>
      {options.loading && <p aria-busy="true">Đang tải giường phù hợp…</p>}
      {!options.loading && !error && !options.beds.length && <p>Chưa có giường phù hợp tại thời điểm này.</p>}
      {options.beds
        .filter((bed) => String(bed.location.bedId) === bedId && bed.earliestStartAt)
        .map((bed) => (
          <p key={bed.location.bedId}>Hạn giường sẵn sàng: {formatDateTime(bed.earliestStartAt)}</p>
        ))}
      <label className="flex flex-col gap-2">
        Lý do phân bổ
        <Input value={reason} disabled={busy} onChange={(event) => setReason(event.target.value)} required />
      </label>
      {error && <ProblemAlert problem={error} />}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" disabled={busy || options.loading || !bedId || !reason.trim()}>
          Xác nhận phân bổ
        </Button>
        <Button type="button" variant="outline" disabled={busy} onClick={reload}>
          Tải lại giường
        </Button>
      </div>
    </form>
  )
}
