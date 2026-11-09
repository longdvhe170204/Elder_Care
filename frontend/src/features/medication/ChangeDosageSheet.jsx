import { useRef, useState } from 'react'
import { ArrowRight, RefreshCw, ShieldAlert } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { todayVN } from './doseRound'
import { hhmm } from './doseRoundView'
import { IssueConfirmations } from './IssueConfirmations'
import { changeDosage, idempotencyKeys } from './prescriptions'
import { changeDosageBody, parseSlots } from './prescriptionView'

// Khung Đổi liều (BR-M07-07, DBR-14): không sửa đè đơn cũ; một lệnh ngừng đơn cũ và tạo đơn thay thế, bắt buộc lý do.
// Thuốc, loại đơn, nguồn, người kê giữ nguyên theo đơn cũ. Đơn thay thế qua kiểm tra trùng hoạt chất, dị ứng (FR-007).
export function ChangeDosageSheet(props) {
  if (!props.prescription) return null
  return <ChangeDosageForm key={props.prescription.id} {...props} />
}

function ChangeDosageForm({ prescription, onClose, onDone }) {
  const raw = prescription.raw
  const hasSlots = raw.prescriptionType === 'PERIODIC' && raw.frequency?.type === 'TIMED_SLOTS'
  const [form, setForm] = useState({
    dosage: String(raw.dosage),
    route: raw.route,
    timedSlots: (raw.frequency?.timedSlots ?? []).join(', '),
    instructionNote: raw.instructionNote ?? '',
    reason: '',
    effectiveFrom: `${todayVN()}T${hhmm(new Date())}`,
    confirmations: {},
  })
  const [check, setCheck] = useState(null)
  const [errorMsg, setErrorMsg] = useState('')
  const [problem, setProblem] = useState(null)
  const [saving, setSaving] = useState(false)
  const keyFor = useRef(idempotencyKeys()).current
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))

  async function submit(event) {
    event.preventDefault()
    if (!(Number(form.dosage) > 0)) return setErrorMsg('Nhập liều mới lớn hơn 0')
    if (!form.route.trim()) return setErrorMsg('Nhập đường dùng')
    if (hasSlots && !parseSlots(form.timedSlots)) return setErrorMsg('Nhập các mốc giờ dạng HH:mm, cách nhau bằng dấu phẩy')
    if (!form.effectiveFrom) return setErrorMsg('Chọn thời điểm áp dụng')
    if (!form.reason.trim()) return setErrorMsg('Lý do đổi liều là bắt buộc (BR-M07-07, DBR-14)')
    setErrorMsg('')
    setSaving(true)
    setProblem(null)
    const body = changeDosageBody(raw, form)
    try {
      await changeDosage(prescription.id, body, keyFor(body))
      await onDone(`Đã đổi liều đơn #${prescription.id} (${prescription.drug}): đơn cũ ngừng, đơn thay thế có hiệu lực từ thời điểm đã chọn.`)
    } catch (error) {
      if (error?.problem?.errorCode === 'ISSUES_REQUIRE_CONFIRMATION') {
        setCheck({ issues: error.problem.issues ?? [], reviewItems: [] })
        setErrorMsg('Đơn thay thế có vấn đề cần lý do xác nhận trước khi lưu (FR-007)')
      } else {
        setProblem(problemOf(error))
      }
      setSaving(false)
    }
  }

  return (
    <Sheet open onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="flex w-full flex-col overflow-y-auto sm:max-w-xl md:max-w-2xl">
        <SheetHeader className="border-b pb-4">
          <SheetTitle className="flex items-center gap-2 type-title">
            <RefreshCw className="size-5" aria-hidden="true" />
            Đổi liều đơn #{prescription.id}
          </SheetTitle>
          <SheetDescription>
            {prescription.drug}: liều hiện hành {prescription.dose}, đường dùng {prescription.route}; {prescription.frequencyText}.
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={submit} noValidate className="flex flex-col gap-5 px-4 py-4">
          {errorMsg && (
            <Alert variant="attention">
              <AlertTitle>Chưa đổi liều được</AlertTitle>
              <AlertDescription>{errorMsg}</AlertDescription>
            </Alert>
          )}
          <ProblemAlert problem={problem} />

          <div className="flex flex-col gap-3 rounded-lg border bg-card p-4">
            <span className="flex items-center gap-2 type-label">
              <ArrowRight className="size-4" aria-hidden="true" />
              Thông số của đơn thay thế
            </span>
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="newDosageInput">Liều mới</FieldLabel>
                <Input id="newDosageInput" type="number" step="0.1" min="0.1" value={form.dosage} onChange={(e) => set({ dosage: e.target.value })} className="h-11" />
              </div>
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="newRouteInput">Đường dùng</FieldLabel>
                <Input id="newRouteInput" value={form.route} onChange={(e) => set({ route: e.target.value })} className="h-11" />
              </div>
            </div>
            {hasSlots && (
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="newSlotsInput">Mốc giờ dùng thuốc</FieldLabel>
                <Input id="newSlotsInput" value={form.timedSlots} onChange={(e) => set({ timedSlots: e.target.value })} placeholder="08:00, 20:00" className="h-11" />
              </div>
            )}
            <div className="flex flex-col gap-1.5">
              <FieldLabel htmlFor="effectiveFromInput">Áp dụng từ</FieldLabel>
              <Input id="effectiveFromInput" type="datetime-local" value={form.effectiveFrom} onChange={(e) => set({ effectiveFrom: e.target.value })} className="h-11 w-fit" />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="changeReasonInput">Lý do đổi liều (bắt buộc, tối đa 2000 ký tự)</FieldLabel>
            <Textarea id="changeReasonInput" rows={3} maxLength={2000} value={form.reason} onChange={(e) => set({ reason: e.target.value })} />
          </div>

          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="newInstructionNote">Ghi chú hướng dẫn dùng thuốc mới</FieldLabel>
            <Textarea id="newInstructionNote" rows={2} value={form.instructionNote} onChange={(e) => set({ instructionNote: e.target.value })} />
          </div>

          <IssueConfirmations check={check} reasons={form.confirmations} onReason={(code, text) => set({ confirmations: { ...form.confirmations, [code]: text } })} />

          <p className="flex items-start gap-2 rounded-md bg-warning px-3 py-2 type-body-sm text-warning-foreground">
            <ShieldAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span>
              Sau khi xác nhận, đơn #{prescription.id} chuyển sang Đã ngừng; liều đã phát giữ nguyên kết quả.
            </span>
          </p>

          <SheetFooter className="mt-2 flex gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" className="gap-1.5" disabled={saving}>
              <RefreshCw className="size-4" aria-hidden="true" />
              Xác nhận đổi liều
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  )
}
