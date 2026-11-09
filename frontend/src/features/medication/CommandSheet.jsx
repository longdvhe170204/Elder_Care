import { useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { idempotencyKeys, runPrescriptionCommand } from './prescriptions'

// Lệnh tạm dừng, tiếp tục, ngừng, gia hạn đơn (FR-011, FR-015, FR-017; BR-M07-07, BR-M07-08): bắt buộc lý do, gửi Idempotency-Key; lỗi 409 hiện trong khung.
const COPY = {
  SUSPEND: { title: 'Tạm dừng đơn', action: 'Xác nhận tạm dừng đơn' },
  RESUME: { title: 'Tiếp tục đơn', action: 'Xác nhận tiếp tục đơn' },
  DISCONTINUE: { title: 'Ngừng đơn', action: 'Xác nhận ngừng đơn' },
  EXTEND: { title: 'Gia hạn đơn', action: 'Xác nhận gia hạn đơn' },
}

export function CommandSheet(props) {
  if (!props.target) return null
  return <CommandForm key={`${props.target.command}-${props.target.prescription.id}`} {...props} />
}

function CommandForm({ target, onClose, onDone }) {
  const { command, prescription } = target
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  // Gia hạn: ngày kết thúc mới muộn hơn hiện tại, hoặc bỏ ngày kết thúc (newEndedAt = null)
  const [endedAt, setEndedAt] = useState('')
  const [noEnd, setNoEnd] = useState(false)
  const extend = command === 'EXTEND'
  const extendInvalid = extend && !noEnd && !endedAt
  const [problem, setProblem] = useState(null)
  const [saving, setSaving] = useState(false)
  const keyFor = useRef(idempotencyKeys()).current

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!reason.trim() || extendInvalid) return
    setSaving(true)
    setProblem(null)
    try {
      const extra = extend ? { newEndedAt: noEnd ? null : endedAt } : {}
      await runPrescriptionCommand(command, prescription.id, reason, keyFor({ command, reason: reason.trim(), extra }), extra)
      await onDone(command, prescription)
    } catch (error) {
      setProblem(problemOf(error))
      setSaving(false)
    }
  }

  return (
    <Sheet open onOpenChange={(open) => !open && onClose()}>
      <SheetContent>
        <form onSubmit={submit} noValidate className="flex min-h-0 flex-1 flex-col">
          <SheetHeader>
            <SheetTitle className="type-title">
              {COPY[command].title} #{prescription.id}
            </SheetTitle>
            <SheetDescription>
              {prescription.drug}, {prescription.dose}
            </SheetDescription>
          </SheetHeader>
          <div className="flex flex-col gap-4 px-4">
            <ProblemAlert problem={problem} />
            {extend && (
              <Field data-invalid={(submitted && extendInvalid) || undefined}>
                <FieldLabel htmlFor="extend-ended-at">Ngày kết thúc mới</FieldLabel>
                <Input id="extend-ended-at" type="date" value={endedAt} disabled={noEnd} onChange={(e) => setEndedAt(e.target.value)} className="h-11 w-fit" />
                <label className="flex cursor-pointer items-center gap-2 type-body-sm">
                  <input type="checkbox" checked={noEnd} onChange={(e) => setNoEnd(e.target.checked)} />
                  Bỏ ngày kết thúc
                </label>
                {submitted && extendInvalid && <FieldError>Chọn ngày kết thúc mới hoặc bỏ ngày kết thúc</FieldError>}
              </Field>
            )}
            <Field data-invalid={(submitted && !reason.trim()) || undefined}>
              <FieldLabel htmlFor="command-reason">Lý do</FieldLabel>
              <Textarea id="command-reason" rows={3} value={reason} onChange={(e) => setReason(e.target.value)} aria-invalid={(submitted && !reason.trim()) || undefined} />
              {submitted && !reason.trim() && <FieldError>Nhập lý do</FieldError>}
            </Field>
          </div>
          <SheetFooter>
            <Button type="submit" className="h-11" disabled={saving}>
              {COPY[command].action}
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  )
}
