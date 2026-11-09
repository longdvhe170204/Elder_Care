import { useRef, useState } from 'react'
import { FileText, ShieldAlert } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { AllergyNote } from '@/common/CareNotes'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { cn } from '@/lib/utils'
import { todayVN } from './doseRound'
import { IssueConfirmations } from './IssueConfirmations'
import { checkPrescription, createPrescription, idempotencyKeys } from './prescriptions'
import { DAY_LABELS, newPrescriptionBody, parseSlots } from './prescriptionView'

// Khung Nhập đơn thuốc (spec 006 User Story 1; FR-004 → FR-008): chọn thuốc từ danh mục, kiểm tra trùng hoạt chất và
// dị ứng do backend tính (POST .../prescriptions/check hoặc 409 ISSUES_REQUIRE_CONFIRMATION), vấn đề phải có lý do
// xác nhận mới lưu (BR-M07-06). Bác sĩ kê đơn nội bộ hoặc bên ngoài; Điều dưỡng chỉ nhập đơn bên ngoài (BR-M07-05).

const select = 'h-11 rounded-md border bg-background px-3 text-sm'

export function PrescriptionSheet({ open, onOpenChange, ...props }) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col overflow-y-auto sm:max-w-xl md:max-w-2xl">
        {open && <PrescriptionForm onOpenChange={onOpenChange} {...props} />}
      </SheetContent>
    </Sheet>
  )
}

function PrescriptionForm({ onOpenChange, residentId, resident, medications, isDoctor, onSaved }) {
  const usable = medications.filter((m) => m.active !== false)
  const [form, setForm] = useState({
    medicationId: usable[0]?.id ?? '',
    dosage: '1',
    route: usable[0]?.defaultRoute ?? '',
    prescriptionType: 'PERIODIC',
    frequencyType: 'TIMED_SLOTS',
    timedSlots: '08:00',
    daysOfWeek: [],
    everyNHours: '8',
    firstSlot: '08:00',
    minIntervalMinutes: '240',
    maxDosesPer24h: '3',
    medicationSource: 'FACILITY',
    prescriberType: isDoctor ? 'INTERNAL' : 'EXTERNAL',
    facilityName: '',
    prescriberName: '',
    prescribedAt: todayVN(),
    instructionNote: '',
    confirmations: {},
  })
  const [check, setCheck] = useState(null)
  const [errorMsg, setErrorMsg] = useState('')
  const [problem, setProblem] = useState(null)
  const [saving, setSaving] = useState(false)
  const keyFor = useRef(idempotencyKeys()).current
  const medication = usable.find((m) => m.id === Number(form.medicationId))
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))

  function invalid() {
    if (!medication) return 'Chọn thuốc từ danh mục'
    if (!(Number(form.dosage) > 0)) return 'Nhập liều mỗi lần lớn hơn 0'
    if (!form.route.trim()) return 'Nhập đường dùng'
    if (form.prescriptionType === 'PRN') {
      if (!(Number(form.minIntervalMinutes) >= 1) || !(Number(form.maxDosesPer24h) >= 1))
        return 'Nhập khoảng cách tối thiểu (phút) và số lần tối đa trong 24 giờ, mỗi giá trị từ 1'
    } else if (form.frequencyType === 'TIMED_SLOTS') {
      if (!parseSlots(form.timedSlots)) return 'Nhập các mốc giờ dạng HH:mm, cách nhau bằng dấu phẩy'
    } else if (form.frequencyType === 'DAYS_OF_WEEK') {
      if (form.daysOfWeek.length === 0 || !parseSlots(form.timedSlots)) return 'Chọn ít nhất một ngày trong tuần và nhập các mốc giờ dạng HH:mm'
    } else if (!(Number(form.everyNHours) >= 1) || !parseSlots(form.firstSlot)) {
      return 'Nhập số giờ giữa các lần (từ 1) và giờ lần đầu dạng HH:mm'
    }
    if (form.prescriberType === 'EXTERNAL' && (!form.facilityName.trim() || !form.prescriberName.trim() || !form.prescribedAt))
      return 'Đơn bên ngoài bắt buộc nhập cơ sở khám, người kê và ngày kê (BR-M07-05)'
    return ''
  }

  async function runCheck() {
    const message = invalid()
    setErrorMsg(message)
    if (message) return
    setProblem(null)
    try {
      setCheck(await checkPrescription(residentId, newPrescriptionBody(form)))
    } catch (error) {
      setProblem(problemOf(error))
    }
  }

  async function submit(event) {
    event.preventDefault()
    const message = invalid()
    setErrorMsg(message)
    if (message) return
    const unconfirmed = (check?.issues ?? []).filter((i) => !form.confirmations[i.issueCode]?.trim())
    if (unconfirmed.length > 0) {
      setErrorMsg(`Còn ${unconfirmed.length} vấn đề chưa có lý do xác nhận (FR-007)`)
      return
    }
    setSaving(true)
    setProblem(null)
    const body = newPrescriptionBody(form)
    try {
      await createPrescription(residentId, body, keyFor(body))
      await onSaved(`Đã lưu đơn ${medication.name} cho ${resident.name}.`)
      onOpenChange(false)
    } catch (error) {
      if (error?.problem?.errorCode === 'ISSUES_REQUIRE_CONFIRMATION') {
        setCheck({ issues: error.problem.issues ?? [], reviewItems: check?.reviewItems ?? [] })
        setErrorMsg('Đơn có vấn đề cần lý do xác nhận trước khi lưu (FR-007)')
      } else {
        setProblem(problemOf(error))
      }
      setSaving(false)
    }
  }

  return (
    <>
      <SheetHeader className="border-b pb-4">
        <SheetTitle className="flex items-center gap-2 type-title">
          <FileText className="size-5" aria-hidden="true" />
          Nhập đơn thuốc mới
        </SheetTitle>
        <SheetDescription>
          Cho {resident.name}. Hệ thống kiểm tra trùng hoạt chất và dị ứng theo BR-M07-05, BR-M07-06.
        </SheetDescription>
        <AllergyNote allergies={resident.allergies} />
      </SheetHeader>

      <form onSubmit={submit} noValidate className="flex flex-col gap-5 px-4 py-4">
        {errorMsg && (
          <Alert variant="attention">
            <AlertTitle>Chưa lưu được đơn</AlertTitle>
            <AlertDescription>{errorMsg}</AlertDescription>
          </Alert>
        )}
        <ProblemAlert problem={problem} />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="medSelect">Thuốc từ danh mục</FieldLabel>
            <select
              id="medSelect"
              value={form.medicationId}
              onChange={(e) => {
                const m = usable.find((x) => x.id === Number(e.target.value))
                set({ medicationId: e.target.value, route: m?.defaultRoute ?? form.route })
                setCheck(null)
              }}
              className={select}
            >
              {usable.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} ({m.concentration}), {m.dosageForm}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="medSource">Nguồn thuốc</FieldLabel>
            <select id="medSource" value={form.medicationSource} onChange={(e) => set({ medicationSource: e.target.value })} className={select}>
              <option value="FACILITY">Thuốc của viện</option>
              <option value="FAMILY_SENT">Gia đình gửi</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="dosageInput">Liều mỗi lần{medication?.usageUnit ? ` (${medication.usageUnit})` : ''}</FieldLabel>
            <Input id="dosageInput" type="number" step="0.1" min="0.1" value={form.dosage} onChange={(e) => set({ dosage: e.target.value })} className="h-11" />
          </div>
          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="routeInput">Đường dùng</FieldLabel>
            <Input id="routeInput" value={form.route} onChange={(e) => set({ route: e.target.value })} className="h-11" />
          </div>
        </div>

        <div className="flex flex-col gap-3 rounded-lg border bg-card p-3">
          <div className="flex flex-wrap items-center gap-4">
            <span className="type-label">Loại đơn</span>
            {[
              ['PERIODIC', 'Định kỳ'],
              ['PRN', 'Khi cần (PRN)'],
            ].map(([value, label]) => (
              <label key={value} className="flex cursor-pointer items-center gap-1.5 type-body-sm">
                <input type="radio" name="prescType" value={value} checked={form.prescriptionType === value} onChange={() => set({ prescriptionType: value })} />
                {label}
              </label>
            ))}
          </div>
          {form.prescriptionType === 'PERIODIC' ? (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="freqSelect">Kiểu tần suất</FieldLabel>
                <select id="freqSelect" value={form.frequencyType} onChange={(e) => set({ frequencyType: e.target.value })} className={select}>
                  <option value="TIMED_SLOTS">Các mốc giờ cố định</option>
                  <option value="EVERY_N_HOURS">Mỗi N giờ</option>
                  <option value="DAYS_OF_WEEK">Theo ngày trong tuần</option>
                </select>
                {form.frequencyType === 'DAYS_OF_WEEK' && (
                  <fieldset className="flex flex-wrap gap-x-3 gap-y-1 pt-1">
                    <legend className="sr-only">Ngày trong tuần</legend>
                    {Object.entries(DAY_LABELS).map(([day, label]) => (
                      <label key={day} className="flex cursor-pointer items-center gap-1 type-body-sm">
                        <input
                          type="checkbox"
                          checked={form.daysOfWeek.includes(Number(day))}
                          onChange={(e) =>
                            set({ daysOfWeek: e.target.checked ? [...form.daysOfWeek, Number(day)] : form.daysOfWeek.filter((d) => d !== Number(day)) })
                          }
                        />
                        {label}
                      </label>
                    ))}
                  </fieldset>
                )}
              </div>
              {form.frequencyType !== 'EVERY_N_HOURS' ? (
                <div className="flex flex-col gap-1.5">
                  <FieldLabel htmlFor="timedSlotsInput">Các mốc giờ (cách nhau bằng dấu phẩy)</FieldLabel>
                  <Input id="timedSlotsInput" value={form.timedSlots} onChange={(e) => set({ timedSlots: e.target.value })} placeholder="08:00, 20:00" className="h-11" />
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-3">
                  <div className="flex flex-col gap-1.5">
                    <FieldLabel htmlFor="everyNInput">Mỗi bao nhiêu giờ</FieldLabel>
                    <Input id="everyNInput" type="number" min="1" value={form.everyNHours} onChange={(e) => set({ everyNHours: e.target.value })} className="h-11" />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <FieldLabel htmlFor="firstSlotInput">Giờ lần đầu</FieldLabel>
                    <Input id="firstSlotInput" value={form.firstSlot} onChange={(e) => set({ firstSlot: e.target.value })} placeholder="08:00" className="h-11" />
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="intervalInput">Khoảng cách tối thiểu (phút)</FieldLabel>
                <Input id="intervalInput" type="number" min="1" value={form.minIntervalMinutes} onChange={(e) => set({ minIntervalMinutes: e.target.value })} className="h-11" />
              </div>
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="max24hInput">Số lần tối đa trong 24 giờ</FieldLabel>
                <Input id="max24hInput" type="number" min="1" value={form.maxDosesPer24h} onChange={(e) => set({ maxDosesPer24h: e.target.value })} className="h-11" />
              </div>
            </div>
          )}
        </div>

        <div className="flex flex-col gap-3 rounded-lg border bg-card p-3">
          <div className="flex flex-wrap items-center gap-4">
            <span className="type-label">Nơi kê đơn</span>
            <label className={cn('flex items-center gap-1.5 type-body-sm', isDoctor ? 'cursor-pointer' : 'cursor-not-allowed opacity-50')}>
              <input type="radio" name="prescriberType" value="INTERNAL" disabled={!isDoctor} checked={form.prescriberType === 'INTERNAL'} onChange={() => set({ prescriberType: 'INTERNAL' })} />
              Nội bộ viện {!isDoctor && '(chỉ Bác sĩ)'}
            </label>
            <label className="flex cursor-pointer items-center gap-1.5 type-body-sm">
              <input type="radio" name="prescriberType" value="EXTERNAL" checked={form.prescriberType === 'EXTERNAL'} onChange={() => set({ prescriberType: 'EXTERNAL' })} />
              Bên ngoài (theo toa)
            </label>
          </div>
          {!isDoctor && (
            <p className="rounded-md bg-warning px-3 py-2 type-body-sm text-warning-foreground">
              Điều dưỡng chỉ nhập đơn từ cơ sở khám bên ngoài theo toa, không tự kê đơn nội bộ (BR-M07-05, FR-006).
            </p>
          )}
          {form.prescriberType === 'EXTERNAL' && (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="facName">Cơ sở khám</FieldLabel>
                <Input id="facName" value={form.facilityName} onChange={(e) => set({ facilityName: e.target.value })} className="h-11" />
              </div>
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="docName">Người kê đơn</FieldLabel>
                <Input id="docName" value={form.prescriberName} onChange={(e) => set({ prescriberName: e.target.value })} className="h-11" />
              </div>
              <div className="flex flex-col gap-1.5">
                <FieldLabel htmlFor="prescribedAtInput">Ngày kê</FieldLabel>
                <Input id="prescribedAtInput" type="date" max={todayVN()} value={form.prescribedAt} onChange={(e) => set({ prescribedAt: e.target.value })} className="h-11" />
              </div>
            </div>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <FieldLabel htmlFor="instNote">Ghi chú hướng dẫn dùng thuốc</FieldLabel>
          <Textarea id="instNote" rows={2} value={form.instructionNote} onChange={(e) => set({ instructionNote: e.target.value })} />
        </div>

        <div className="flex flex-col gap-3 rounded-lg border bg-card p-4">
          <div className="flex items-center justify-between gap-2">
            <span className="flex items-center gap-2 type-label">
              <ShieldAlert className="size-5" aria-hidden="true" />
              Kiểm tra trùng hoạt chất, dị ứng
            </span>
            <Button type="button" variant="outline" size="sm" onClick={runCheck}>
              Kiểm tra ngay
            </Button>
          </div>
          <IssueConfirmations check={check} reasons={form.confirmations} onReason={(code, text) => set({ confirmations: { ...form.confirmations, [code]: text } })} />
        </div>

        <SheetFooter className="mt-2 flex gap-2">
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Hủy
          </Button>
          <Button type="submit" disabled={saving}>
            Lưu đơn thuốc
          </Button>
        </SheetFooter>
      </form>
    </>
  )
}
