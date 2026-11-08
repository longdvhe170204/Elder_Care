import { useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { idempotencyKeys, runPrescriptionCommand } from './prescriptions'

// Lệnh tạm dừng, tiếp tục, ngừng đơn (FR-011; BR-M07-07): bắt buộc lý do, gửi Idempotency-Key; lỗi 409 hiện trong khung.
const COPY = {
  SUSPEND: { title: 'Tạm dừng đơn', action: 'Xác nhận tạm dừng đơn' },
  RESUME: { title: 'Tiếp tục đơn', action: 'Xác nhận tiếp tục đơn' },
  DISCONTINUE: { title: 'Ngừng đơn', action: 'Xác nhận ngừng đơn' },
}

export function CommandSheet(props) {
  if (!props.target) return null
  return <CommandForm key={`${props.target.command}-${props.target.prescription.id}`} {...props} />
}

function CommandForm({ target, onClose, onDone }) {
  const { command, prescription } = target
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [saving, setSaving] = useState(false)
  const keyFor = useRef(idempotencyKeys()).current

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!reason.trim()) return
    setSaving(true)
    setProblem(null)
    try {
      await runPrescriptionCommand(command, prescription.id, reason, keyFor({ command, reason: reason.trim() }))
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
