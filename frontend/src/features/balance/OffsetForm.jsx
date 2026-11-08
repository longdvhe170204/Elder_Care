import { useRef, useState } from 'react'
import { postResidentsByResidentIdDepositOffsets } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldGroup } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { TextField } from './BalanceFields'

// Cấn trừ tiền cọc vào số dư âm (017 T027; FR-015): Kế toán thực hiện, không cần duyệt; máy chủ tạo cặp hai giao dịch
// liên kết cùng số tiền và kiểm điều kiện (hồ sơ kết thúc lưu trú hoặc việc sau qua đời, bảng kỳ cuối đã chốt, không vượt
// số dư cọc và phần còn nợ).

export function OffsetForm({ residentId, onDone }) {
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState('')
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const key = useRef(crypto.randomUUID())

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    if (!/^[1-9]\d*$/.test(amount.trim())) {
      setError('Nhập số tiền là số nguyên lớn hơn 0.')
      return
    }
    setError('')
    setBusy(true)
    try {
      await postResidentsByResidentIdDepositOffsets({
        path: { residentId },
        headers: { 'Idempotency-Key': key.current },
        body: { amount: Number(amount), ...(note.trim() && { note: note.trim() }) },
      })
      onDone('Đã cấn trừ tiền cọc vào số dư.')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <FieldGroup>
        <TextField
          id="offset-amount"
          label="Số tiền cấn trừ"
          inputMode="numeric"
          value={amount}
          onChange={setAmount}
          error={error}
          hint="Không vượt số dư cọc và phần còn nợ. Đơn vị đồng."
        />
        <Field>
          <label htmlFor="offset-note" className="type-label">
            Ghi chú (không bắt buộc)
          </label>
          <Textarea id="offset-note" rows={2} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11" disabled={busy}>
        Cấn trừ
      </Button>
    </form>
  )
}
