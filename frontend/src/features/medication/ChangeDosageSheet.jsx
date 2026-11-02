import { useState } from 'react'
import { AlertCircle, ArrowRight, RefreshCw, ShieldAlert } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { DEMO_RESIDENTS } from '@/common/demoData'

/**
 * Sheet Đổi liều đơn thuốc theo nghiệp vụ BR-M07-07 & DBR-14:
 * Không sửa đè đơn cũ; tạo đơn thay thế mới trỏ về đơn cũ,
 * đơn cũ chuyển sang ĐÃ NGỪNG, kèm lý do đổi liều bắt buộc.
 */
export function ChangeDosageSheet(props) {
  if (!props.prescription) return null
  return <ChangeDosageForm key={props.prescription.id} {...props} />
}

function ChangeDosageForm({ open, onOpenChange, prescription, onSave }) {
  const [newDosage, setNewDosage] = useState(String(prescription.dosage || '1'))
  const [newDosageUnit, setNewDosageUnit] = useState(prescription.dosageUnit || 'Viên')
  const [newRoute, setNewRoute] = useState(prescription.route || 'Uống')
  const [newTimedSlots, setNewTimedSlots] = useState(prescription.timedSlots || '08:00, 20:00')
  const [effectiveFrom, setEffectiveFrom] = useState('IMMEDIATE') // IMMEDIATE | NEXT_SHIFT | TOMORROW
  const [reason, setReason] = useState('')
  const [instructionNote, setInstructionNote] = useState(prescription.instructionNote || '')
  const [errorMsg, setErrorMsg] = useState('')

  const res = DEMO_RESIDENTS.find((r) => r.code === prescription.residentCode)

  function handleSubmit(e) {
    e.preventDefault()
    setErrorMsg('')

    if (!newDosage || Number(newDosage) <= 0) {
      setErrorMsg('Vui lòng nhập liều lượng mới lớn hơn 0.')
      return
    }

    if (!reason.trim()) {
      setErrorMsg('Lý do đổi liều là bắt buộc theo quy định BR-M07-07 & DBR-14.')
      return
    }

    const nextId = Date.now() % 10000 + 200
    const newPrescription = {
      ...prescription,
      id: nextId,
      dosage: Number(newDosage),
      dosageUnit: newDosageUnit,
      route: newRoute,
      timedSlots: newTimedSlots,
      instructionNote: instructionNote.trim(),
      status: 'Hiệu lực',
      replacesId: prescription.id,
      prescribedAt: new Date().toISOString().split('T')[0],
      createdAt: new Date().toISOString(),
      changeReason: reason.trim(),
    }

    onSave({
      oldPrescriptionId: prescription.id,
      newPrescription,
      reason: reason.trim(),
      effectiveFrom,
    })
    onOpenChange(false)
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col sm:max-w-xl md:max-w-2xl overflow-y-auto">
        <SheetHeader className="border-b pb-4">
          <SheetTitle className="flex items-center gap-2 type-title">
            <RefreshCw className="size-5 text-primary" />
            Đổi liều đơn thuốc #{prescription.id}
          </SheetTitle>
          <SheetDescription>
            Tạo đơn thuốc thay thế mới theo <strong>BR-M07-07, DBR-14</strong> (đơn cũ sẽ được ngừng, bảo lưu lịch sử liều cũ).
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-col gap-5 py-4">
          {errorMsg && (
            <Alert variant="destructive" className="animate-in fade-in">
              <AlertCircle className="size-4" />
              <AlertTitle>Không thể đổi liều</AlertTitle>
              <AlertDescription>{errorMsg}</AlertDescription>
            </Alert>
          )}

          {/* Thông tin người cao tuổi & thuốc hiện hành */}
          <div className="flex flex-col gap-3 rounded-lg border bg-muted/20 p-3.5">
            <div className="flex items-center gap-3">
              {res && <ResidentAvatar name={res.name} code={res.code} initials={res.initials} className="size-10" />}
              <div className="flex flex-1 flex-col">
                <span className="font-semibold type-body">{prescription.residentName}</span>
                <span className="type-caption text-muted-foreground tabular-nums">
                  Mã: {prescription.residentCode} • Đơn hiện tại: #{prescription.id}
                </span>
              </div>
              <Badge variant="outline" className="font-semibold">
                {prescription.drug}
              </Badge>
            </div>

            <div className="flex items-center gap-3 rounded-md bg-background/80 p-2.5 text-xs text-muted-foreground border">
              <span>Liều hiện hành:</span>
              <strong className="text-foreground text-sm">
                {prescription.dosage} {prescription.dosageUnit}
              </strong>
              <span>• Đường dùng: <strong>{prescription.route}</strong></span>
              <span>• Tần suất: <strong>{prescription.timedSlots || 'Khi cần (PRN)'}</strong></span>
            </div>
          </div>

          {/* Nhập liều mới */}
          <div className="flex flex-col gap-3 rounded-lg border border-primary/30 bg-primary/5 p-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-primary">
              <ArrowRight className="size-4" />
              Thông số liều thay thế mới
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="newDosageInput">Liều mới *</FieldLabel>
                <Input
                  id="newDosageInput"
                  type="number"
                  step="0.1"
                  min="0.1"
                  value={newDosage}
                  onChange={(e) => setNewDosage(e.target.value)}
                  required
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="newUnitInput">Đơn vị *</FieldLabel>
                <Input
                  id="newUnitInput"
                  value={newDosageUnit}
                  onChange={(e) => setNewDosageUnit(e.target.value)}
                  required
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="newRouteInput">Đường dùng *</FieldLabel>
                <Input
                  id="newRouteInput"
                  value={newRoute}
                  onChange={(e) => setNewRoute(e.target.value)}
                  required
                />
              </div>
            </div>

            {prescription.prescriptionType === 'PERIODIC' && (
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="newSlotsInput">Mốc giờ dùng thuốc mới</FieldLabel>
                <Input
                  id="newSlotsInput"
                  value={newTimedSlots}
                  onChange={(e) => setNewTimedSlots(e.target.value)}
                  placeholder="08:00, 20:00"
                />
              </div>
            )}
          </div>

          {/* Thời điểm áp dụng */}
          <div className="flex flex-col gap-2 rounded-lg border bg-muted/20 p-3">
            <FieldLabel>Thời điểm áp dụng đơn thay thế</FieldLabel>
            <div className="flex flex-wrap gap-4 text-sm pt-1">
              <label className="flex items-center gap-1.5 cursor-pointer">
                <input
                  type="radio"
                  name="effectiveFromRadio"
                  value="IMMEDIATE"
                  checked={effectiveFrom === 'IMMEDIATE'}
                  onChange={() => setEffectiveFrom('IMMEDIATE')}
                />
                Áp dụng ngay lập tức
              </label>
              <label className="flex items-center gap-1.5 cursor-pointer">
                <input
                  type="radio"
                  name="effectiveFromRadio"
                  value="NEXT_SHIFT"
                  checked={effectiveFrom === 'NEXT_SHIFT'}
                  onChange={() => setEffectiveFrom('NEXT_SHIFT')}
                />
                Áp dụng từ ca kế tiếp
              </label>
              <label className="flex items-center gap-1.5 cursor-pointer">
                <input
                  type="radio"
                  name="effectiveFromRadio"
                  value="TOMORROW"
                  checked={effectiveFrom === 'TOMORROW'}
                  onChange={() => setEffectiveFrom('TOMORROW')}
                />
                Áp dụng từ ngày mai
              </label>
            </div>
          </div>

          {/* Lý do đổi liều (Bắt buộc theo BR-M07-07) */}
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <FieldLabel htmlFor="changeReasonInput" className="font-semibold text-destructive">
                Lý do đổi liều * (Bắt buộc theo BR-M07-07 & DBR-14)
              </FieldLabel>
              <span className="type-caption text-muted-foreground">Tối đa 2000 ký tự</span>
            </div>
            <Textarea
              id="changeReasonInput"
              rows={3}
              placeholder="Ví dụ: Tăng liều Amlodipin từ 5mg lên 10mg do huyết áp chưa đạt mục tiêu (150/95 mmHg)..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required
            />
          </div>

          {/* Ghi chú hướng dẫn mới */}
          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="newInstructionNote">Ghi chú hướng dẫn dùng thuốc mới</FieldLabel>
            <Textarea
              id="newInstructionNote"
              rows={2}
              placeholder="Uống sau bữa ăn sáng, theo dõi huyết áp hàng ngày..."
              value={instructionNote}
              onChange={(e) => setInstructionNote(e.target.value)}
            />
          </div>

          <div className="flex items-start gap-2 rounded-md bg-amber-500/10 p-3 text-xs text-amber-800 dark:text-amber-300">
            <ShieldAlert className="size-4 shrink-0 mt-0.5" />
            <span>
              <strong>Lưu ý bất biến:</strong> Sau khi xác nhận, đơn #{prescription.id} sẽ chuyển sang trạng thái <em>Đã ngừng</em>.
              Các liều đã phát trước đây vẫn giữ nguyên kết quả và không bị ảnh hưởng.
            </span>
          </div>

          <SheetFooter className="mt-2 flex gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="default" className="gap-1.5">
              <RefreshCw className="size-4" />
              Xác nhận đổi liều
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  )
}
