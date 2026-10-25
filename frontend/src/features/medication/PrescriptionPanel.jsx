import { useState } from 'react'
import { Calendar, Clock, FileText, Plus, ShieldAlert, User } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_RESIDENTS } from '@/common/demoData'
import { PrescriptionSheet } from './PrescriptionSheet'
import { INITIAL_PRESCRIPTIONS } from './prescriptionData'

const STATUS_FILTERS = ['Tất cả', 'Hiệu lực', 'Chờ hiệu lực', 'Tạm dừng', 'Đã ngừng', 'Hết hạn']

export function PrescriptionPanel({ canRecord, onSaved }) {
  const [prescriptions, setPrescriptions] = useState(INITIAL_PRESCRIPTIONS)
  const [selectedResidentCode, setSelectedResidentCode] = useState('ALL')
  const [statusFilter, setStatusFilter] = useState('Tất cả')
  const [isSheetOpen, setIsSheetOpen] = useState(false)

  // Lọc theo người cao tuổi và trạng thái
  const filteredPrescriptions = prescriptions.filter((p) => {
    const matchResident = selectedResidentCode === 'ALL' || p.residentCode === selectedResidentCode
    const matchStatus = statusFilter === 'Tất cả' || p.status === statusFilter
    return matchResident && matchStatus
  })

  function handleSavePrescription(newPresc) {
    setPrescriptions((prev) => [newPresc, ...prev])
    if (onSaved) {
      onSaved(`Đã thêm thành công đơn thuốc mới [${newPresc.drug}] cho ${newPresc.residentName}.`)
    }
  }

  return (
    <div className="flex flex-col gap-5">
      {/* Thanh công cụ: Chọn người cao tuổi & Lọc trạng thái & Nút tạo đơn */}
      <div className="flex flex-col gap-4 rounded-xl border bg-card p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <User className="size-4 text-muted-foreground" />
            <span className="text-sm font-medium">Người cao tuổi:</span>
            <select
              value={selectedResidentCode}
              onChange={(e) => setSelectedResidentCode(e.target.value)}
              className="rounded-md border bg-background px-3 py-1.5 text-sm font-medium"
            >
              <option value="ALL">Tất cả người cao tuổi ({DEMO_RESIDENTS.length})</option>
              {DEMO_RESIDENTS.map((r) => (
                <option key={r.code} value={r.code}>
                  {r.name} ({r.code})
                </option>
              ))}
            </select>
          </div>

          <ToggleGroup
            variant="filter"
            size="sm"
            value={[statusFilter]}
            onValueChange={(val) => val[0] && setStatusFilter(val[0])}
            className="flex-wrap"
          >
            {STATUS_FILTERS.map((s) => (
              <ToggleGroupItem key={s} value={s} className="px-3">
                {s}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
        </div>

        {canRecord && (
          <Button onClick={() => setIsSheetOpen(true)} className="gap-2 shrink-0">
            <Plus className="size-4" />
            Nhập đơn thuốc mới
          </Button>
        )}
      </div>

      {/* Danh sách đơn thuốc */}
      <div className="flex flex-col gap-3">
        {filteredPrescriptions.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-xl border border-dashed p-10 text-center">
            <FileText className="size-10 text-muted-foreground/60 mb-2" />
            <p className="type-body font-medium">Không tìm thấy đơn thuốc nào</p>
            <p className="type-caption text-muted-foreground">
              Thử thay đổi bộ lọc trạng thái hoặc bấm "Nhập đơn thuốc mới".
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {filteredPrescriptions.map((p) => {
              const res = DEMO_RESIDENTS.find((r) => r.code === p.residentCode)
              return (
                <Card key={p.id} className="flex flex-col justify-between overflow-hidden transition-shadow hover:shadow-md">
                  <CardHeader className="flex flex-row items-center gap-3 border-b bg-muted/20 pb-3">
                    {res && <ResidentAvatar name={res.name} code={res.code} initials={res.initials} className="size-10" />}
                    <div className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate font-semibold type-body">{p.residentName}</span>
                      <span className="type-caption text-muted-foreground tabular-nums">Mã: {p.residentCode} • Đơn #{p.id}</span>
                    </div>
                    <StatusBadge status={p.status} />
                  </CardHeader>

                  <CardContent className="flex flex-1 flex-col gap-3 pt-3">
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-base font-bold text-foreground">{p.drug}</span>
                        <Badge variant="outline" className="text-xs">
                          {p.prescriptionType === 'PERIODIC' ? 'Định kỳ' : 'Khi cần (PRN)'}
                        </Badge>
                      </div>
                      <p className="type-body-sm text-muted-foreground mt-0.5">
                        Liều: <strong className="text-foreground">{p.dosage} {p.dosageUnit}</strong>, đường dùng: {p.route}
                      </p>
                    </div>

                    <div className="flex flex-col gap-1.5 rounded-lg border bg-muted/10 p-2.5 text-xs">
                      <div className="flex items-center gap-1.5 text-muted-foreground">
                        <Clock className="size-3.5 shrink-0" />
                        <span>
                          {p.prescriptionType === 'PERIODIC'
                            ? `Tần suất: ${p.timedSlots || 'Theo mốc giờ'}`
                            : `Cách tối thiểu ${p.minIntervalMinutes / 60}h, tối đa ${p.maxDosesPer24h} lần/24h`}
                        </span>
                      </div>
                      <div className="flex items-center gap-1.5 text-muted-foreground">
                        <Calendar className="size-3.5 shrink-0" />
                        <span>Kê ngày: {p.prescribedAt} bởi {p.prescriberName}</span>
                      </div>
                    </div>

                    {p.instructionNote && (
                      <div className="rounded-md bg-secondary/20 p-2 text-xs italic text-secondary-foreground">
                        "{p.instructionNote}"
                      </div>
                    )}

                    {p.confirmedIssues && p.confirmedIssues.length > 0 && (
                      <div className="flex flex-col gap-1 rounded-md border border-amber-500/30 bg-amber-500/10 p-2">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-700 dark:text-amber-400">
                          <ShieldAlert className="size-3.5 shrink-0" />
                          Đã xác nhận vấn đề chuyên môn:
                        </div>
                        {p.confirmedIssues.map((ci) => (
                          <div key={ci.issueCode} className="text-xs text-muted-foreground pl-4">
                            • <em>{ci.issueCode}</em>: {ci.reason}
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              )
            })}
          </div>
        )}
      </div>

      <PrescriptionSheet
        open={isSheetOpen}
        onOpenChange={setIsSheetOpen}
        onSave={handleSavePrescription}
        existingPrescriptions={prescriptions}
      />
    </div>
  )
}
