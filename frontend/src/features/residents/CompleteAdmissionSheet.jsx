import { useState } from 'react'
import { CircleCheck, TriangleAlert } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { SourceTag } from '@/common/SourceTag'

// Khung lệnh Hoàn tất tiếp nhận (DESIGN.md 2.2, màn D; 6.1, Q-356, Q-361; 004 FR-004a).
// Ý kiến của người cao tuổi về việc vào ở đứng đầu, trước điều kiện. Ý kiến không chặn lệnh;
// "Không đồng ý" thì lý do của lệnh bắt buộc. Kiểm tra thật và danh sách điều kiện do backend trả.

function Opinion({ opinion }) {
  if (!opinion) {
    return (
      <Alert variant="attention">
        <TriangleAlert aria-hidden="true" />
        <AlertTitle>Chưa ghi ý kiến của người cao tuổi về việc vào ở</AlertTitle>
        <AlertDescription>Lệnh vẫn thực hiện được. Ý kiến ghi ở lượt đăng ký tiếp nhận.</AlertDescription>
      </Alert>
    )
  }
  return (
    <div className="flex flex-col gap-1 rounded-lg bg-muted px-3 py-2">
      <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span className="font-medium">{opinion.value}</span>
        <SourceTag source={opinion.source} relative={opinion.relative} />
      </span>
      {opinion.note && <span>{opinion.note}</span>}
      <span className="type-caption text-muted-foreground tabular-nums">
        Ghi bởi {opinion.recordedBy}, {opinion.recordedAt}
      </span>
    </div>
  )
}

function AdmissionForm({ resident, admission, onDone }) {
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const opinion = admission?.opinion
  const reasonRequired = opinion?.value === 'Không đồng ý'
  const reasonMissing = reasonRequired && !reason.trim()

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!reasonMissing) onDone()
  }

  return (
    <form onSubmit={submit} className="flex flex-1 flex-col" noValidate>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">Hoàn tất tiếp nhận</SheetTitle>
        <SheetDescription>
          {resident.name}, {resident.code}
        </SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 px-4 type-body-sm">
        <section className="flex flex-col gap-2">
          <h3 className="type-caption text-muted-foreground">Ý kiến của người cao tuổi về việc vào ở</h3>
          <Opinion opinion={opinion} />
        </section>
        <Field data-invalid={(submitted && reasonMissing) || undefined}>
          <FieldLabel htmlFor="admission-reason">{reasonRequired ? 'Lý do của lệnh (bắt buộc)' : 'Lý do của lệnh'}</FieldLabel>
          <Textarea
            id="admission-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
            aria-invalid={(submitted && reasonMissing) || undefined}
          />
          {submitted && reasonMissing ? (
            <FieldError>Nhập lý do vì người cao tuổi không đồng ý vào ở</FieldError>
          ) : (
            reasonRequired && (
              <FieldDescription>Người cao tuổi không đồng ý vào ở. Ghi lý do hoàn tất tiếp nhận.</FieldDescription>
            )
          )}
        </Field>
        <section className="flex flex-col gap-2">
          <h3 className="type-caption text-muted-foreground">Điều kiện tiếp nhận</h3>
          <ul className="flex flex-col gap-1.5">
            {(admission?.conditions ?? []).map((c) => (
              <li key={c} className="flex items-center gap-2">
                <CircleCheck className="size-4 shrink-0 text-success-foreground" aria-hidden="true" />
                {c}, đã đáp ứng
              </li>
            ))}
          </ul>
        </section>
      </div>
      <SheetFooter>
        <Button type="submit" className="h-11">
          Hoàn tất tiếp nhận
        </Button>
      </SheetFooter>
    </form>
  )
}

export function CompleteAdmissionSheet({ resident, admission, open, onOpenChange, onDone }) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent>
        {open && <AdmissionForm resident={resident} admission={admission} onDone={onDone} />}
      </SheetContent>
    </Sheet>
  )
}
