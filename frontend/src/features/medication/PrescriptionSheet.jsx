import { useState } from 'react'
import { AlertCircle, AlertTriangle, CheckCircle2, FileText, ShieldAlert } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { DEMO_RESIDENTS } from '@/common/demoData'
import { DEMO_MEDICATIONS } from './prescriptionData'

export function PrescriptionSheet({ open, onOpenChange, onSave, existingPrescriptions = [] }) {
  const [residentCode, setResidentCode] = useState(DEMO_RESIDENTS[0]?.code || 'NCT-0001')
  const [medicationId, setMedicationId] = useState(1)
  const [dosage, setDosage] = useState('1')
  const [dosageUnit, setDosageUnit] = useState('Viên')
  const [route, setRoute] = useState('Uống')
  const [prescriptionType, setPrescriptionType] = useState('PERIODIC') // PERIODIC | PRN
  const [frequencyType, setFrequencyType] = useState('TIMED_SLOTS') // TIMED_SLOTS | EVERY_N_HOURS | DAYS_OF_WEEK
  const [timedSlots, setTimedSlots] = useState('08:00, 20:00')
  const [minIntervalMinutes, setMinIntervalMinutes] = useState('240')
  const [maxDosesPer24h, setMaxDosesPer24h] = useState('3')
  const [medicationSource, setMedicationSource] = useState('FACILITY') // FACILITY | FAMILY_SENT
  const [prescriberType, setPrescriberType] = useState('INTERNAL') // INTERNAL | EXTERNAL
  const [facilityName, setFacilityName] = useState('')
  const [prescriberName, setPrescriberName] = useState('')
  const [instructionNote, setInstructionNote] = useState('')

  // Kết quả kiểm tra xung đột & dị ứng
  const [checkResult, setCheckResult] = useState(null)
  const [confirmedReasons, setConfirmedReasons] = useState({})
  const [errorMsg, setErrorMsg] = useState('')

  const resident = DEMO_RESIDENTS.find((r) => r.code === residentCode) || DEMO_RESIDENTS[0]
  const selectedMed = DEMO_MEDICATIONS.find((m) => m.id === Number(medicationId))

  // Kiểm tra trùng hoạt chất & dị ứng
  function runCheck() {
    setErrorMsg('')
    const med = DEMO_MEDICATIONS.find((m) => m.id === Number(medicationId))
    if (!med) return { issues: [], reviewItems: [] }

    const issues = []
    const reviewItems = []

    // 1. Kiểm tra trùng hoạt chất với các đơn đang dùng của người cao tuổi này
    const residentPrescriptions = existingPrescriptions.filter(
      (p) => p.residentCode === residentCode && (p.status === 'Hiệu lực' || p.status === 'Tạm dừng')
    )
    for (const p of residentPrescriptions) {
      const pMed = DEMO_MEDICATIONS.find((m) => m.id === p.medicationId) || { activeIngredients: [p.drug], allergenIds: p.allergenIds || [] }
      const overlap = (med.allergenIds || []).filter((id) => (pMed.allergenIds || []).includes(id))
      if (overlap.length > 0) {
        issues.push({
          issueCode: `TRUNG_HC_${p.id}_${overlap[0]}`,
          issueType: 'DUPLICATE_ACTIVE_INGREDIENT',
          activeIngredient: med.activeIngredients?.[0] || 'Hoạt chất trùng',
          duplicatePrescriptionId: p.id,
          message: `Trùng hoạt chất [${med.activeIngredients?.join(', ')}] với đơn hiện có #${p.id} (${p.drug})`,
        })
      }
    }

    // 2. Kiểm tra xung đột dị ứng với hồ sơ người cao tuổi
    const residentAllergies = resident.allergies || []
    for (const alg of residentAllergies) {
      const isMatch = (med.activeIngredients || []).some((ing) =>
        alg.name?.toLowerCase().includes(ing.toLowerCase()) || ing.toLowerCase().includes(alg.name?.toLowerCase())
      )
      if (isMatch) {
        issues.push({
          issueCode: `DI_UNG_${alg.name}`,
          issueType: 'ALLERGY_CONFLICT',
          activeIngredient: alg.name,
          severity: alg.level || 'NANG',
          message: `Xung đột dị ứng: Người cao tuổi có tiền sử dị ứng [${alg.name}] (mức độ: ${alg.level || 'Nặng'})`,
        })
      }
    }

    // 3. Dị ứng khác cần xem xét thủ công
    if (resident.allergies?.some((a) => a.kind === 'Khác' || a.freeText)) {
      reviewItems.push({
        issueCode: 'DI_UNG_KHAC',
        issueType: 'OTHER_ALLERGY_REVIEW',
        message: 'Hồ sơ có ghi nhận dị ứng tự do / dị ứng khác. Cần Bác sĩ xem xét cẩn trọng trước khi kê đơn.',
      })
    }

    setCheckResult({ issues, reviewItems })
    return { issues, reviewItems }
  }

  function handleSave(e) {
    e.preventDefault()
    setErrorMsg('')

    // Kiểm tra tính hợp lệ cơ bản
    if (!dosage || Number(dosage) <= 0) {
      setErrorMsg('Vui lòng nhập liều dùng lớn hơn 0')
      return
    }
    if (prescriberType === 'EXTERNAL' && (!facilityName.trim() || !prescriberName.trim())) {
      setErrorMsg('Đơn bên ngoài bắt buộc nhập Tên cơ sở khám và Bác sĩ kê đơn')
      return
    }

    // Kiểm tra vấn đề xung đột
    let check = checkResult
    if (!check) {
      check = runCheck()
    }

    // Kiểm tra xem đã có lý do xác nhận cho mọi vấn đề bắt buộc chưa
    const unconfirmed = (check?.issues || []).filter((issue) => !confirmedReasons[issue.issueCode]?.trim())
    if (unconfirmed.length > 0) {
      setErrorMsg(`Phát hiện ${unconfirmed.length} xung đột hoạt chất hoặc dị ứng chưa có lý do xác nhận vượt chặn!`)
      return
    }

    const nextId = existingPrescriptions.length > 0 ? Math.max(...existingPrescriptions.map((p) => p.id)) + 1 : 1
    const newPrescription = {
      id: nextId,
      residentCode,
      residentName: resident.name,
      medicationId: Number(medicationId),
      drug: selectedMed?.name || 'Thuốc',
      dosage: Number(dosage),
      dosageUnit,
      route,
      prescriptionType,
      frequencyType: prescriptionType === 'PERIODIC' ? frequencyType : null,
      timedSlots: prescriptionType === 'PERIODIC' ? timedSlots : null,
      minIntervalMinutes: prescriptionType === 'PRN' ? Number(minIntervalMinutes) : null,
      maxDosesPer24h: prescriptionType === 'PRN' ? Number(maxDosesPer24h) : null,
      medicationSource,
      prescriberType,
      facilityName: prescriberType === 'EXTERNAL' ? facilityName : null,
      prescriberName: prescriberType === 'EXTERNAL' ? prescriberName : 'Bác sĩ viện',
      prescribedAt: '2026-10-23',
      instructionNote,
      status: 'Hiệu lực',
      confirmedIssues: Object.entries(confirmedReasons).map(([issueCode, reason]) => ({ issueCode, reason })),
      createdAt: '2026-10-23T10:00:00Z',
    }

    onSave(newPrescription)
    onOpenChange(false)
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="flex w-full flex-col sm:max-w-xl md:max-w-2xl overflow-y-auto">
        <SheetHeader className="border-b pb-4">
          <SheetTitle className="flex items-center gap-2 type-title">
            <FileText className="size-5 text-primary" />
            Nhập đơn thuốc mới
          </SheetTitle>
          <SheetDescription>
            Tạo đơn định kỳ hoặc khi cần (PRN), kiểm tra xung đột hoạt chất và dị ứng theo BR-M07-05, BR-M07-06.
          </SheetDescription>
        </SheetHeader>

        <form onSubmit={handleSave} className="flex flex-col gap-5 py-4">
          {errorMsg && (
            <Alert variant="destructive" className="animate-in fade-in">
              <AlertCircle className="size-4" />
              <AlertTitle>Không thể lưu đơn</AlertTitle>
              <AlertDescription>{errorMsg}</AlertDescription>
            </Alert>
          )}

          {/* 1. Chọn Người cao tuổi */}
          <div className="flex flex-col gap-2">
            <FieldLabel htmlFor="residentSelect">Người cao tuổi *</FieldLabel>
            <div className="flex items-center gap-3 rounded-lg border bg-card p-3">
              <ResidentAvatar name={resident.name} code={resident.code} initials={resident.initials} className="size-10" />
              <div className="flex flex-1 flex-col">
                <span className="font-medium type-body">{resident.name}</span>
                <span className="type-caption text-muted-foreground tabular-nums">Mã: {resident.code} • Phòng: {resident.room}</span>
              </div>
              <select
                id="residentSelect"
                value={residentCode}
                onChange={(e) => {
                  setResidentCode(e.target.value)
                  setCheckResult(null)
                }}
                className="rounded-md border bg-background px-3 py-1.5 text-sm"
              >
                {DEMO_RESIDENTS.map((r) => (
                  <option key={r.code} value={r.code}>
                    {r.name} ({r.code})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 2. Chọn thuốc từ danh mục */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-1.5">
              <FieldLabel htmlFor="medSelect">Thuốc từ danh mục *</FieldLabel>
              <select
                id="medSelect"
                value={medicationId}
                onChange={(e) => {
                  const mId = Number(e.target.value)
                  setMedicationId(mId)
                  const m = DEMO_MEDICATIONS.find((x) => x.id === mId)
                  if (m) {
                    setDosageUnit(m.usageUnit || 'Viên')
                    setRoute(m.defaultRoute || 'Uống')
                  }
                  setCheckResult(null)
                }}
                className="rounded-md border bg-background px-3 py-2 text-sm"
              >
                {DEMO_MEDICATIONS.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name} ({m.concentration}) - {m.dosageForm}
                  </option>
                ))}
              </select>
              {selectedMed && (
                <span className="type-caption text-muted-foreground">
                  Hoạt chất: {selectedMed.activeIngredients?.join(', ')}
                </span>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <FieldLabel htmlFor="medSource">Nguồn thuốc *</FieldLabel>
              <select
                id="medSource"
                value={medicationSource}
                onChange={(e) => setMedicationSource(e.target.value)}
                className="rounded-md border bg-background px-3 py-2 text-sm"
              >
                <option value="FACILITY">Thuốc nguồn viện (FACILITY)</option>
                <option value="FAMILY_SENT">Thuốc gia đình gửi (FAMILY_SENT)</option>
              </select>
            </div>
          </div>

          {/* 3. Liều dùng, đơn vị, đường dùng */}
          <div className="grid grid-cols-3 gap-3">
            <div className="flex flex-col gap-1.5">
              <FieldLabel htmlFor="dosageInput">Liều mỗi lần *</FieldLabel>
              <Input
                id="dosageInput"
                type="number"
                step="0.1"
                min="0.1"
                value={dosage}
                onChange={(e) => setDosage(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <FieldLabel htmlFor="unitInput">Đơn vị *</FieldLabel>
              <Input
                id="unitInput"
                value={dosageUnit}
                onChange={(e) => setDosageUnit(e.target.value)}
                placeholder="Viên, gói, ml..."
                required
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <FieldLabel htmlFor="routeInput">Đường dùng *</FieldLabel>
              <Input
                id="routeInput"
                value={route}
                onChange={(e) => setRoute(e.target.value)}
                placeholder="Uống, tiêm..."
                required
              />
            </div>
          </div>

          {/* 4. Loại đơn & Tần suất */}
          <div className="flex flex-col gap-3 rounded-lg border bg-muted/30 p-3">
            <div className="flex items-center gap-4">
              <span className="text-sm font-medium">Loại đơn:</span>
              <label className="flex items-center gap-1.5 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="prescType"
                  value="PERIODIC"
                  checked={prescriptionType === 'PERIODIC'}
                  onChange={() => setPrescriptionType('PERIODIC')}
                />
                Định kỳ (Periodic)
              </label>
              <label className="flex items-center gap-1.5 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="prescType"
                  value="PRN"
                  checked={prescriptionType === 'PRN'}
                  onChange={() => setPrescriptionType('PRN')}
                />
                Khi cần (PRN)
              </label>
            </div>

            {prescriptionType === 'PERIODIC' ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 pt-2">
                <div className="flex flex-col gap-1.5">
                  <FieldLabel htmlFor="freqSelect">Kiểu tần suất</FieldLabel>
                  <select
                    id="freqSelect"
                    value={frequencyType}
                    onChange={(e) => setFrequencyType(e.target.value)}
                    className="rounded-md border bg-background px-3 py-1.5 text-sm"
                  >
                    <option value="TIMED_SLOTS">Các mốc giờ cố định</option>
                    <option value="EVERY_N_HOURS">Mỗi N giờ</option>
                    <option value="DAYS_OF_WEEK">Theo các ngày trong tuần</option>
                  </select>
                </div>
                <div className="flex flex-col gap-1.5">
                  <FieldLabel htmlFor="timedSlotsInput">
                    {frequencyType === 'TIMED_SLOTS' ? 'Các mốc giờ (phân cách bằng dấu phẩy)' : 'Chi tiết tần suất'}
                  </FieldLabel>
                  <Input
                    id="timedSlotsInput"
                    value={timedSlots}
                    onChange={(e) => setTimedSlots(e.target.value)}
                    placeholder="08:00, 12:00, 20:00"
                  />
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-3 pt-2">
                <div className="flex flex-col gap-1.5">
                  <FieldLabel htmlFor="intervalInput">Khoảng cách tối thiểu (phút) *</FieldLabel>
                  <Input
                    id="intervalInput"
                    type="number"
                    value={minIntervalMinutes}
                    onChange={(e) => setMinIntervalMinutes(e.target.value)}
                    placeholder="240 = 4 giờ"
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <FieldLabel htmlFor="max24hInput">Số lần tối đa trong 24h *</FieldLabel>
                  <Input
                    id="max24hInput"
                    type="number"
                    value={maxDosesPer24h}
                    onChange={(e) => setMaxDosesPer24h(e.target.value)}
                    placeholder="3"
                  />
                </div>
              </div>
            )}
          </div>

          {/* 5. Cơ sở kê đơn */}
          <div className="flex flex-col gap-3 rounded-lg border bg-muted/30 p-3">
            <div className="flex items-center gap-4">
              <span className="text-sm font-medium">Cơ sở kê đơn:</span>
              <label className="flex items-center gap-1.5 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="prescriberType"
                  value="INTERNAL"
                  checked={prescriberType === 'INTERNAL'}
                  onChange={() => setPrescriberType('INTERNAL')}
                />
                Nội bộ viện (INTERNAL)
              </label>
              <label className="flex items-center gap-1.5 text-sm cursor-pointer">
                <input
                  type="radio"
                  name="prescriberType"
                  value="EXTERNAL"
                  checked={prescriberType === 'EXTERNAL'}
                  onChange={() => setPrescriberType('EXTERNAL')}
                />
                Bên ngoài (EXTERNAL)
              </label>
            </div>

            {prescriberType === 'EXTERNAL' && (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 pt-2">
                <div className="flex flex-col gap-1.5">
                  <FieldLabel htmlFor="facName">Tên cơ sở khám/bệnh viện *</FieldLabel>
                  <Input
                    id="facName"
                    value={facilityName}
                    onChange={(e) => setFacilityName(e.target.value)}
                    placeholder="Bệnh viện Lão khoa TW"
                    required
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <FieldLabel htmlFor="docName">Bác sĩ kê đơn *</FieldLabel>
                  <Input
                    id="docName"
                    value={prescriberName}
                    onChange={(e) => setPrescriberName(e.target.value)}
                    placeholder="BS. Nguyễn Văn A"
                    required
                  />
                </div>
              </div>
            )}
          </div>

          {/* 6. Ghi chú hướng dẫn */}
          <div className="flex flex-col gap-1.5">
            <FieldLabel htmlFor="instNote">Ghi chú hướng dẫn dùng thuốc</FieldLabel>
            <Textarea
              id="instNote"
              rows={2}
              value={instructionNote}
              onChange={(e) => setInstructionNote(e.target.value)}
              placeholder="Uống sau bữa ăn sáng 15 phút, uống với nhiều nước..."
            />
          </div>

          {/* 7. Khu vực kiểm tra xung đột hoạt chất & dị ứng */}
          <div className="flex flex-col gap-3 rounded-lg border border-primary/20 bg-primary/5 p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldAlert className="size-5 text-primary" />
                <span className="font-semibold text-sm">Kiểm tra an toàn đơn thuốc</span>
              </div>
              <Button type="button" variant="outline" size="sm" onClick={runCheck}>
                Kiểm tra ngay
              </Button>
            </div>

            {checkResult && (
              <div className="flex flex-col gap-3 pt-2">
                {checkResult.issues.length === 0 && checkResult.reviewItems.length === 0 ? (
                  <div className="flex items-center gap-2 text-sm text-green-600 dark:text-green-400 font-medium bg-green-500/10 p-2.5 rounded-md">
                    <CheckCircle2 className="size-4 shrink-0" />
                    Không phát hiện xung đột hoạt chất hoặc dị ứng. Đơn hợp lệ để lưu!
                  </div>
                ) : (
                  <>
                    {checkResult.issues.map((issue) => (
                      <Card key={issue.issueCode} className="border-destructive/40 bg-destructive/5">
                        <CardContent className="flex flex-col gap-2 p-3">
                          <div className="flex items-start gap-2">
                            <AlertCircle className="size-4 shrink-0 text-destructive mt-0.5" />
                            <div className="flex flex-col">
                              <span className="text-sm font-semibold text-destructive">{issue.message}</span>
                              <span className="type-caption text-muted-foreground">Mã vấn đề: {issue.issueCode}</span>
                            </div>
                          </div>
                          <div className="flex flex-col gap-1 pt-1">
                            <FieldLabel htmlFor={`reason_${issue.issueCode}`} className="text-xs font-medium text-destructive">
                              Lý do xác nhận vượt chặn (bắt buộc theo FR-007, BR-M07-06):
                            </FieldLabel>
                            <Input
                              id={`reason_${issue.issueCode}`}
                              size="sm"
                              placeholder="Nhập lý do chuyên môn (ví dụ: Đã cân nhắc lợi ích vượt trội nguy cơ...)"
                              value={confirmedReasons[issue.issueCode] || ''}
                              onChange={(e) =>
                                setConfirmedReasons((prev) => ({
                                  ...prev,
                                  [issue.issueCode]: e.target.value,
                                }))
                              }
                              required
                            />
                          </div>
                        </CardContent>
                      </Card>
                    ))}

                    {checkResult.reviewItems.map((review) => (
                      <div key={review.issueCode} className="flex items-start gap-2 rounded-md bg-amber-500/10 p-2.5 text-xs text-amber-700 dark:text-amber-400">
                        <AlertTriangle className="size-4 shrink-0 mt-0.5" />
                        <span>{review.message}</span>
                      </div>
                    ))}
                  </>
                )}
              </div>
            )}
          </div>

          <SheetFooter className="mt-2 flex gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="default">
              Lưu đơn thuốc
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  )
}
