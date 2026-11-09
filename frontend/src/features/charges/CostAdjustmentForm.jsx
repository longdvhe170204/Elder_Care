import { useRef, useState } from 'react'
import { postCostsItemsByIdAdjust } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { controlClass } from './chargeLabels'

export function CostAdjustmentForm({ itemId, sourceChangeId, onCreated = () => {} }) {
  const [amount, setAmount] = useState('')
  const [type, setType] = useState(sourceChangeId ? 'SOURCE_CHANGE' : 'POST_FINALIZATION_CORRECTION')
  const [reason, setReason] = useState('')
  const [failure, setFailure] = useState(null)
  const [pending, setPending] = useState(false)
  const [done, setDone] = useState(false)
  const busy = useRef(false)
  const nonce = useRef(null)
  async function submit(event) {
    event.preventDefault()
    if (busy.current || !reason.trim() || !Number.isSafeInteger(Number(amount))) return
    busy.current = true
    setPending(true)
    setFailure(null)
    nonce.current ??= crypto.randomUUID()
    try {
      await postCostsItemsByIdAdjust({
        path: { id: itemId },
        headers: { 'Idempotency-Key': nonce.current },
        body: {
          amount: Number(amount),
          adjustmentType: type,
          reason: reason.trim(),
          ...(sourceChangeId ? { sourceChangeId } : {}),
        },
      })
      setDone(true)
      onCreated()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      busy.current = false
      setPending(false)
    }
  }
  if (done)
    return (
      <p role="status" className="type-body-sm">
        Đã lập khoản điều chỉnh nháp. Khoản cần được kiểm tra, duyệt và chốt.
      </p>
    )
  return (
    <form onSubmit={submit} className="space-y-3">
      <p className="type-body-sm">Điều chỉnh cho khoản #{itemId}. Nhập số tiền âm để giảm, dương để tăng.</p>
      <label className="block type-body-sm">
        Loại điều chỉnh
        <select
          className={controlClass}
          value={type}
          disabled={pending}
          onChange={(e) => {
            setType(e.target.value)
            nonce.current = null
          }}
        >
          <option value="POST_FINALIZATION_CORRECTION">Sửa sai sau chốt</option>
          <option value="SOURCE_CHANGE">Biến động nguồn</option>
          <option value="WAIVER">Miễn giảm</option>
          <option value="LATE_ADDITION">Bổ sung muộn</option>
        </select>
      </label>
      <label className="block type-body-sm">
        Số tiền điều chỉnh (đồng)
        <Input
          type="number"
          step="1"
          min="-999999999999999"
          max="999999999999999"
          required
          value={amount}
          disabled={pending}
          onChange={(e) => {
            setAmount(e.target.value)
            nonce.current = null
          }}
        />
      </label>
      <label className="block type-body-sm">
        Lý do
        <Textarea
          required
          maxLength={2000}
          value={reason}
          disabled={pending}
          onChange={(e) => {
            setReason(e.target.value)
            nonce.current = null
          }}
        />
      </label>
      <ProblemAlert problem={failure} />
      <Button type="submit" disabled={pending || !reason.trim() || amount === ''}>
        Lập khoản điều chỉnh
      </Button>
    </form>
  )
}
