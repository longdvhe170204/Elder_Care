import { useEffect, useState } from 'react'
import { CalendarDays, Plus } from 'lucide-react'
import {
  deleteShiftsById,
  getMeShifts,
  getShiftSchedules,
  getShiftSchedulesByIdPublicationPreview,
  getShiftTemplates,
  getShiftsById,
  getStaff,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { PermissionsProvider, usePermissions } from '@/common/permissions/permissions'
import { ShiftForm, Choice } from './ShiftForm'
import { PublicationForm, ScheduleForm, TemplateForm } from './ScheduleForms'
import { AssignmentPanel } from './AssignmentPanel'
import { FloorManagerPanel } from './FloorManagerPanel'
import { StaffingRatioPanel } from './StaffingRatioPanel'
import { PostPublicationPanel } from './PostPublicationPanel'
import { currentMonth, nextMonth, scopeLabel, STATUSES } from './shiftLabels'

export function ShiftsPage(props) {
  return (
    <PermissionsProvider>
      <ShiftWorkspace {...props} />
    </PermissionsProvider>
  )
}

function ShiftWorkspace({ initialMonth }) {
  const { status, can } = usePermissions()
  const manage = can('SHIFT_SCHEDULE', 'CREATE') || can('SHIFT_SCHEDULE', 'UPDATE') || can('SHIFT_SCHEDULE', 'APPROVE')
  const [month, setMonth] = useState(initialMonth ?? currentMonth())
  const [schedules, setSchedules] = useState([])
  const [mine, setMine] = useState([])
  const [templates, setTemplates] = useState([])
  const [staff, setStaff] = useState([])
  const [selected, setSelected] = useState(null)
  const [panel, setPanel] = useState(null)
  const [failure, setFailure] = useState(null)
  const [loading, setLoading] = useState(true)
  const [revision, setRevision] = useState(0)
  const [actionBusy, setActionBusy] = useState(false)
  const view = can('SHIFT_SCHEDULE', 'VIEW')
  useEffect(() => {
    if (status !== 'ready' || !view) return
    let cancelled = false
    const load = async () => {
      try {
        const requests = manage
          ? [getShiftSchedules({ query: { month } }), getShiftTemplates(), getStaff()]
          : [getMeShifts({ query: { from: `${month}-01`, to: nextMonth(month) } })]
        const result = await Promise.all(requests)
        if (cancelled) return
        if (manage) {
          setSchedules(result[0].data)
          setTemplates(result[1].data)
          setStaff(result[2].data)
        } else setMine(result[0].data)
        setFailure(null)
      } catch (error) {
        if (!cancelled) setFailure(problemOf(error))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [status, view, manage, month, revision])
  const schedule = schedules.find((s) => String(s.id) === String(selected)) ?? schedules[0]
  const shifts = manage ? (schedule?.shifts ?? []) : mine
  const done = () => {
    setPanel(null)
    setLoading(true)
    setRevision((n) => n + 1)
  }
  async function openShift(shift, kind = 'detail') {
    setActionBusy(true)
    try {
      const result = await getShiftsById({ path: { id: shift.id } })
      setPanel({ kind, shift: result.data, etag: result.etag })
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setActionBusy(false)
    }
  }
  async function preview() {
    setActionBusy(true)
    try {
      const { data } = await getShiftSchedulesByIdPublicationPreview({ path: { id: schedule.id } })
      setPanel({ kind: 'publish', preview: data })
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setActionBusy(false)
    }
  }
  async function remove() {
    setActionBusy(true)
    try {
      await deleteShiftsById({ path: { id: panel.shift.id }, headers: { 'If-Match': panel.etag } })
      done()
    } catch (error) {
      setPanel({ ...panel, failure: problemOf(error) })
    } finally {
      setActionBusy(false)
    }
  }
  if (status === 'error' || (status === 'ready' && !view) || failure?.status === 403)
    return <PermissionDeniedState label="lịch ca" />
  const titles = {
    add: 'Thêm ca',
    edit: 'Sửa ca',
    detail: 'Chi tiết ca',
    delete: 'Xóa ca nháp',
    schedule: 'Lập lịch tháng',
    template: panel?.template ? 'Sửa mẫu ca' : 'Thêm mẫu ca',
    templates: 'Danh mục mẫu ca',
    publish: 'Xem trước công bố',
    assignments: 'Phân công trong ca',
    floorManagers: 'Giao Trưởng tầng',
  }
  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-4">
      <PageHeader
        title={manage ? 'Lịch ca' : 'Ca của tôi'}
        subtitle="Lịch làm việc và nhân viên trong ca · Giờ Việt Nam"
        aside={<CalendarDays aria-hidden="true" className="size-7 text-primary" />}
      >
        <div className="flex flex-wrap items-end gap-3">
          <Field className="w-44">
            <FieldLabel htmlFor="calendar-month">Tháng</FieldLabel>
            <Input
              id="calendar-month"
              type="month"
              required
              value={month}
              onChange={(e) => {
                if (e.target.value) {
                  setMonth(e.target.value)
                  setLoading(true)
                  setPanel(null)
                }
              }}
            />
          </Field>
          {manage && (
            <>
              <Button variant="outline" onClick={() => setPanel({ kind: 'templates' })}>
                Mẫu ca
              </Button>
              <Button variant="outline" onClick={() => setPanel({ kind: 'floorManagers' })}>
                Giao Trưởng tầng
              </Button>
              {can('SHIFT_SCHEDULE', 'CREATE') && (
                <Button onClick={() => setPanel({ kind: 'schedule' })}>
                  <Plus aria-hidden="true" />
                  Lập lịch tháng
                </Button>
              )}
            </>
          )}
        </div>
      </PageHeader>
      <ProblemAlert problem={failure} />
      <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5" aria-busy={loading}>
        {manage && (
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div className="min-w-48 flex-1">
              <Choice
                id="selected-schedule"
                label="Lịch tháng theo phạm vi"
                value={schedule?.id ?? ''}
                onChange={(e) => setSelected(e.target.value)}
              >
                {schedules.length === 0 && <option value="">Chưa có lịch</option>}
                {schedules.map((s) => (
                  <option key={s.id} value={s.id}>
                    {scopeLabel(s)} · {STATUSES[s.status]}
                  </option>
                ))}
              </Choice>
            </div>
            {schedule && <StatusBadge status={STATUSES[schedule.status]} />}
            {schedule?.status === 'DRAFT' && can('SHIFT_SCHEDULE', 'UPDATE') && (
              <Button variant="outline" disabled={actionBusy} onClick={() => setPanel({ kind: 'add' })}>
                Thêm ca
              </Button>
            )}
            {schedule?.status === 'DRAFT' && can('SHIFT_SCHEDULE', 'APPROVE') && (
              <Button disabled={actionBusy} onClick={preview}>
                Xem trước công bố
              </Button>
            )}
          </div>
        )}
        {loading || status === 'loading' ? (
          <p role="status">Đang tải lịch ca…</p>
        ) : shifts.length === 0 ? (
          <div className="py-12 text-center">
            <CalendarDays className="mx-auto mb-3 size-8 text-muted-foreground" aria-hidden="true" />
            <h2 className="type-section">Chưa có ca trong tháng này</h2>
            <p className="mt-1 text-muted-foreground">
              {manage
                ? 'Chọn lịch và thêm ca để bắt đầu sắp xếp nhân viên.'
                : 'Ca đã công bố có tên bạn sẽ xuất hiện ở đây.'}
            </p>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Bắt đầu</TableHead>
                <TableHead>Kết thúc</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Nhân viên / Phụ trách</TableHead>
                <TableHead>
                  <span className="sr-only">Thao tác</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {shifts.map((shift) => (
                <TableRow key={shift.id}>
                  <TableCell className="whitespace-nowrap font-medium">{formatDateTime(shift.startsAt)}</TableCell>
                  <TableCell className="whitespace-nowrap">{formatDateTime(shift.endsAt)}</TableCell>
                  <TableCell>
                    <StatusBadge status={STATUSES[shift.status]} />
                  </TableCell>
                  <TableCell>
                    {shift.staff?.length ?? 0} người
                    {shift.leadStaffId ? ` · #${shift.leadStaffId}` : shift.missingLead ? ' · Chưa có phụ trách' : ''}
                    {shift.staffingRatio && !shift.staffingRatio.meetsThreshold && (
                      <p className="type-caption font-medium text-warning-foreground">Dưới ngưỡng phục vụ</p>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Button size="sm" variant="ghost" onClick={() => openShift(shift)} disabled={actionBusy}>
                        Chi tiết
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openShift(shift, 'assignments')}
                        disabled={actionBusy}
                      >
                        Phân công
                      </Button>
                      {shift.allowedCommands?.includes('edit') && (
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={actionBusy}
                          onClick={() => openShift(shift, 'edit')}
                        >
                          Sửa ca
                        </Button>
                      )}
                      {shift.allowedCommands?.includes('delete') && (
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={actionBusy}
                          onClick={() => openShift(shift, 'delete')}
                        >
                          Xóa ca
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </section>
      <Sheet open={!!panel} onOpenChange={(open) => !open && setPanel(null)}>
        <SheetContent className="overflow-x-hidden overflow-y-auto data-[side=right]:w-full data-[side=right]:sm:max-w-2xl">
          <SheetHeader>
            <SheetTitle>{titles[panel?.kind]}</SheetTitle>
            <SheetDescription>
              {schedule ? `${month} · ${scopeLabel(schedule)}` : 'Mẫu ca và lịch làm việc'}
            </SheetDescription>
          </SheetHeader>
          <div className="px-4 pb-6">
            {(panel?.kind === 'add' || panel?.kind === 'edit') && (
              <ShiftForm
                key={`${panel.kind}-${panel.shift?.id ?? ''}`}
                schedule={schedule}
                shift={panel.shift}
                etag={panel.etag}
                templates={templates}
                staff={staff}
                onDone={done}
              />
            )}
            {panel?.kind === 'schedule' && <ScheduleForm month={month} onDone={done} />}
            {panel?.kind === 'assignments' && (
              <AssignmentPanel key={panel.shift.id} shift={panel.shift} staff={staff} />
            )}
            {panel?.kind === 'floorManagers' && (
              <FloorManagerPanel
                staff={staff}
                canCreate={can('SHIFT_SCHEDULE', 'CREATE')}
                canUpdate={can('SHIFT_SCHEDULE', 'UPDATE')}
              />
            )}
            {panel?.kind === 'template' && (
              <TemplateForm key={panel.template?.id ?? 'new'} template={panel.template} onDone={done} onStale={done} />
            )}
            {panel?.kind === 'templates' && (
              <div className="flex flex-col gap-3">
                {templates.map((t) => (
                  <div key={t.id} className="flex items-center justify-between gap-3 rounded-xl border p-3">
                    <div>
                      <p className="font-medium">{t.name}</p>
                      <p className="text-muted-foreground">
                        {t.startTime}–{t.endTime} · {t.active ? 'Đang sử dụng' : 'Ngừng dùng'}
                      </p>
                    </div>
                    {can('SHIFT_SCHEDULE', 'UPDATE') && (
                      <Button variant="outline" onClick={() => setPanel({ kind: 'template', template: t })}>
                        Sửa mẫu {t.name}
                      </Button>
                    )}
                  </div>
                ))}
                {can('SHIFT_SCHEDULE', 'CREATE') && (
                  <Button onClick={() => setPanel({ kind: 'template' })}>Thêm mẫu ca</Button>
                )}
              </div>
            )}
            {panel?.kind === 'publish' && <PublicationForm schedule={schedule} preview={panel.preview} onDone={done} />}
            {panel?.kind === 'delete' && (
              <div className="flex flex-col gap-4">
                <p>Xóa ca bắt đầu {formatDateTime(panel.shift.startsAt)} khỏi lịch nháp?</p>
                <ProblemAlert problem={panel.failure} />
                {panel.failure?.status === 412 ? (
                  <Button onClick={() => openShift(panel.shift, 'delete')}>Tải lại ca</Button>
                ) : (
                  <Button variant="outline" disabled={actionBusy} onClick={remove}>
                    Xác nhận xóa ca
                  </Button>
                )}
              </div>
            )}
            {panel?.kind === 'detail' && (
              <div className="flex flex-col gap-3">
                <p>
                  {formatDateTime(panel.shift.startsAt)} → {formatDateTime(panel.shift.endsAt)}
                </p>
                <StatusBadge status={STATUSES[panel.shift.status]} />
                <h3 className="type-section">Nhân viên cùng ca</h3>
                {panel.shift.staff?.map((m) => (
                  <p key={m.staffId}>
                    {staff.find((p) => p.id === m.staffId)?.fullName ?? `Nhân viên #${m.staffId}`}
                    {m.onDutyDoctor && ' · Bác sĩ trực'}
                    {panel.shift.leadStaffId === m.staffId && ' · Phụ trách ca'}
                  </p>
                ))}
                <PostPublicationPanel shift={panel.shift} staff={staff} scopeId={schedule?.scopeId} onDone={done} />
                {schedule?.scopeType !== 'FACILITY' && <StaffingRatioPanel shiftId={panel.shift.id} />}
              </div>
            )}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
