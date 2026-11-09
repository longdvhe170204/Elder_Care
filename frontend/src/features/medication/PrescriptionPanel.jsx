import { useCallback, useState } from 'react'
import { Calendar, CalendarPlus, Clock, FileText, Pause, Play, Plus, RefreshCw, ShieldAlert, XCircle } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { hhmm } from './doseRoundView'
import { ChangeDosageSheet } from './ChangeDosageSheet'
import { CommandSheet } from './CommandSheet'
import { PrescriptionSheet } from './PrescriptionSheet'
import { loadPrescriptionBoard } from './prescriptions'
import { STATUS_FILTERS } from './prescriptionView'
import { ResidentPicker } from './ResidentPicker'

// Màn Đơn thuốc (spec 006 User Story 1; FR-004 → FR-017): chọn người cao tuổi, danh sách và chuỗi thay thế đọc từ
// api-006, nút lệnh theo allowedCommands của từng đơn. Bác sĩ có thẩm quyền kê đơn nội bộ, đổi liều, tạm dừng, ngừng;
// Điều dưỡng chỉ nhập đơn bên ngoài (BR-M07-05, BR-M07-07). Thẻ không viền đỏ (DESIGN.md 6.7).

const COMMAND_BUTTONS = [
  { command: 'SUSPEND', label: 'Tạm dừng', Icon: Pause },
  { command: 'RESUME', label: 'Tiếp tục', Icon: Play },
  { command: 'DISCONTINUE', label: 'Ngừng', Icon: XCircle },
  { command: 'EXTEND', label: 'Gia hạn', Icon: CalendarPlus },
]

export function PrescriptionPanel({ role, onSaved }) {
  const isDoctor = role === 'DOCTOR'
  const canPrescribe = isDoctor || role === 'NURSING_STAFF'

  const [resident, setResident] = useState(null)
  const [board, setBoard] = useState(null)
  const [problem, setProblem] = useState(null)
  const [statusFilter, setStatusFilter] = useState('Tất cả')
  const [creating, setCreating] = useState(false)
  const [changing, setChanging] = useState(null)
  const [commanding, setCommanding] = useState(null)

  const load = useCallback(async (id) => {
    try {
      setBoard(await loadPrescriptionBoard(id))
      setProblem(null)
    } catch (error) {
      setBoard({ prescriptions: [], medications: [], resident: null })
      setProblem(problemOf(error))
    }
  }, [])

  function pick(r) {
    setResident(r)
    setBoard(null)
    setProblem(null)
    load(r.id)
  }

  async function saved(message) {
    onSaved?.(message)
    await load(resident.id)
  }

  const prescriptions = (board?.prescriptions ?? []).filter((p) => statusFilter === 'Tất cả' || p.status === statusFilter)

  return (
    <div className="flex flex-col gap-5">
      <ResidentPicker onPick={pick} selectedId={resident?.id} />

      {resident && (
        <div className="flex flex-col gap-4 rounded-xl border bg-card p-4 sm:flex-row sm:items-center sm:justify-between">
          <ToggleGroup
            variant="filter"
            size="sm"
            value={[statusFilter]}
            onValueChange={(val) => val[0] && setStatusFilter(val[0])}
            className="flex-wrap"
            aria-label="Lọc theo trạng thái"
          >
            {STATUS_FILTERS.map((s) => (
              <ToggleGroupItem key={s} value={s} className="px-3">
                {s}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          {canPrescribe && board && (
            <Button onClick={() => setCreating(true)} className="shrink-0 gap-2">
              <Plus className="size-4" aria-hidden="true" />
              {isDoctor ? 'Nhập đơn thuốc mới' : 'Nhập đơn bên ngoài (theo toa)'}
            </Button>
          )}
        </div>
      )}

      <ProblemAlert problem={problem} />
      {resident && !board && !problem && <p className="rounded-xl border bg-card px-4 py-3 type-body-sm">Đang tải đơn thuốc…</p>}

      {board && !problem && prescriptions.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed p-10 text-center">
          <FileText className="mb-2 size-10 text-muted-foreground" aria-hidden="true" />
          <p className="type-body font-medium">Không có đơn thuốc nào</p>
          <p className="type-caption text-muted-foreground">Đổi bộ lọc trạng thái hoặc nhập đơn mới.</p>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        {prescriptions.map((p) => {
          const buttons = isDoctor ? COMMAND_BUTTONS.filter((b) => p.allowedCommands.includes(b.command)) : []
          const canChange = isDoctor && p.allowedCommands.includes('CHANGE_DOSE')
          return (
            <Card key={p.id} className="flex flex-col justify-between overflow-hidden">
              <CardHeader className="flex flex-row items-center gap-3 border-b pb-3">
                <div className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate type-label">{p.drug}</span>
                  <span className="type-caption text-muted-foreground tabular-nums">Đơn #{p.id}</span>
                </div>
                <StatusBadge status={p.status} />
              </CardHeader>

              <CardContent className="flex flex-1 flex-col gap-3 pt-3">
                <div className="flex items-center justify-between gap-2">
                  <p className="type-body-sm text-muted-foreground">
                    Liều: <strong className="text-foreground">{p.dose}</strong>, đường dùng: {p.route}
                  </p>
                  <Badge variant="outline">{p.typeLabel}</Badge>
                </div>

                {p.replacesId && <Badge variant="secondary">Đơn thay thế đơn #{p.replacesId}</Badge>}
                {p.replacedById && (
                  <span className="type-caption text-muted-foreground">
                    Đã được thay thế bởi đơn <strong>#{p.replacedById}</strong>
                  </span>
                )}
                {p.noMedicationAvailable && <span className="type-caption text-muted-foreground">Chưa có thuốc để phát (FR-009)</span>}

                <div className="flex flex-col gap-1.5 rounded-lg border bg-muted/30 p-2.5 type-caption">
                  <span className="flex items-center gap-1.5 text-muted-foreground">
                    <Clock className="size-3.5 shrink-0" aria-hidden="true" />
                    {p.frequencyText}
                  </span>
                  <span className="flex items-center gap-1.5 text-muted-foreground">
                    <Calendar className="size-3.5 shrink-0" aria-hidden="true" />
                    {p.prescribedAt ? `Kê ngày ${p.prescribedAt.split('-').reverse().join('/')}, ` : `Hiệu lực từ ${hhmm(p.startedAt)}, `}
                    {p.prescriberText}
                  </span>
                </div>

                {p.instructionNote && <p className="rounded-md bg-muted px-3 py-2 type-caption italic">{p.instructionNote}</p>}

                {p.confirmedIssues.length > 0 && (
                  <div className="flex flex-col gap-1 rounded-md border bg-warning p-2 text-warning-foreground">
                    <span className="flex items-center gap-1.5 type-caption font-semibold">
                      <ShieldAlert className="size-3.5 shrink-0" aria-hidden="true" />
                      Đã xác nhận khi kê đơn:
                    </span>
                    {p.confirmedIssues.map((i) => (
                      <span key={i.issueCode} className="pl-5 type-caption">
                        {i.text}
                      </span>
                    ))}
                  </div>
                )}
              </CardContent>

              {(buttons.length > 0 || canChange) && (
                <CardFooter className="flex items-center justify-between border-t px-3 py-2.5">
                  <div className="flex items-center gap-1.5">
                    {buttons.map(({ command, label, Icon }) => (
                      <Button key={command} variant="ghost" size="sm" className="h-9 px-2" onClick={() => setCommanding({ command, prescription: p })}>
                        <Icon className="mr-1 size-3.5" aria-hidden="true" />
                        {label}
                      </Button>
                    ))}
                  </div>
                  {canChange && (
                    <Button size="sm" className="h-9 gap-1.5 px-3" onClick={() => setChanging(p)}>
                      <RefreshCw className="size-3.5" aria-hidden="true" />
                      Đổi liều
                    </Button>
                  )}
                </CardFooter>
              )}
            </Card>
          )
        })}
      </div>

      {resident && board && (
        <PrescriptionSheet
          open={creating}
          onOpenChange={setCreating}
          residentId={resident.id}
          resident={board.resident ?? { name: resident.personalInfo.fullName, allergies: [] }}
          medications={board.medications}
          isDoctor={isDoctor}
          onSaved={saved}
        />
      )}
      <ChangeDosageSheet
        prescription={changing}
        onClose={() => setChanging(null)}
        onDone={async (message) => {
          setChanging(null)
          await saved(message)
        }}
      />
      <CommandSheet
        target={commanding}
        onClose={() => setCommanding(null)}
        onDone={async (command, p) => {
          setCommanding(null)
          await saved(`Đã ${{ SUSPEND: 'tạm dừng', RESUME: 'tiếp tục', DISCONTINUE: 'ngừng', EXTEND: 'gia hạn' }[command]} đơn #${p.id} (${p.drug}).`)
        }}
      />
    </div>
  )
}
