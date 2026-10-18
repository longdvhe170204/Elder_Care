import { OctagonAlert } from 'lucide-react'
import { Alert, AlertAction, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { PageHeader } from '@/common/PageHeader'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import {
  DEMO_BEDS,
  DEMO_ESCALATED_ALERT,
  DEMO_HANDOVER,
  DEMO_INDICATORS,
  DEMO_RESIDENTS,
  DEMO_SCOPE,
  DEMO_TASKS,
} from '@/common/demoData'
import { cn } from '@/lib/utils'

// Bản mẫu bố cục dashboard của feature 016 (mục 18.5), theo DESIGN.md 2.1.0.
// Chỉ tiêu, phạm vi, quyền xem do 016 quy định; màn này chỉ thử cách trình bày.

// Ô giường (DESIGN.md 7.2, bed-cell): màu đặc token --bed-*, mỗi màu ≥ 3:1 so với nền thẻ. Giữ chỗ là viền
// đậm trên nền xanh trời nhạt để khác hẳn Đang sử dụng; Trống, Tạm ngừng chỉ có viền để không tranh chú ý.
const BED_STYLE = {
  'Đang sử dụng': 'bg-bed-occupied',
  'Đang giữ chỗ': 'bg-accent ring-2 ring-bed-held ring-inset',
  Trống: 'bg-card ring-1 ring-input ring-inset',
  'Chờ sẵn sàng': 'bg-bed-pending',
  'Đang bảo trì': 'bg-bed-maintenance',
  'Tạm ngừng sử dụng': 'bg-card border-2 border-dashed border-bed-maintenance',
  'Không sử dụng': 'bg-card border-2 border-dashed border-bed-maintenance',
}

// Số đếm mở được danh sách bản ghi (016 US1). Bản mẫu chỉ có màn Checklist ca để mở;
// các số khác chưa có màn đích nên chưa bấm được.
const INDICATOR_TARGETS = {
  'Quá hạn': ['care-tasks', { initialFilter: 'Quá hạn' }],
}

// Phòng minh họa: phòng 2xx, 4 giường A–D mỗi phòng
const BEDS_PER_ROOM = 4
const DEMO_ROOMS = Array.from({ length: Math.ceil(DEMO_BEDS.length / BEDS_PER_ROOM) }, (_, r) => ({
  room: `2${String(r + 1).padStart(2, '0')}`,
  beds: DEMO_BEDS.slice(r * BEDS_PER_ROOM, (r + 1) * BEDS_PER_ROOM),
}))

// Khung giờ của một công việc: giờ tròn chứa giờ bắt đầu (07:45 thuộc 07:00)
const slotOf = (time) => `${time.slice(0, 2)}:00`
const residentOf = (name) => DEMO_RESIDENTS.find((r) => r.name === name)

function IndicatorStrip({ onNavigate }) {
  return (
    <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-xl border bg-border sm:grid-cols-3 xl:grid-cols-6">
      {DEMO_INDICATORS.map((ind) => {
        const target = INDICATOR_TARGETS[ind.label]
        const value = (
          <>
            {ind.value}
            {ind.detail && <span className="ml-2 type-caption">{ind.detail}</span>}
          </>
        )
        return (
          <div key={ind.label} className="flex flex-col gap-1 bg-card px-4 py-3">
            <dt className="type-caption text-balance text-muted-foreground">{ind.label}</dt>
            <dd className={cn('mt-auto type-metric', ind.urgent && 'text-destructive')}>
              {target ? (
                <button
                  type="button"
                  onClick={() => onNavigate(...target)}
                  className="rounded-md underline decoration-2 underline-offset-4 hover:decoration-current"
                  aria-label={`${ind.label}: ${ind.value}. Mở danh sách`}
                >
                  {value}
                </button>
              ) : (
                value
              )}
            </dd>
          </div>
        )
      })}
    </dl>
  )
}

// Một công việc trong lưới khung giờ: giờ, trạng thái; tên việc; người cao tuổi, phòng; người phụ trách (8.4).
function TaskTile({ task }) {
  const done = task.status === 'Hoàn thành'
  const overdue = task.status === 'Quá hạn'
  const resident = residentOf(task.resident)
  return (
    <li
      className={cn(
        'flex flex-col gap-2 rounded-xl border bg-card p-3',
        overdue && 'border-transparent ring-[1.5px] ring-destructive',
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <span className={cn('type-label tabular-nums', overdue && 'text-destructive', done && 'text-muted-foreground')}>
          {task.time}
        </span>
        <StatusBadge status={task.status} />
      </div>
      <span className={cn('type-card-title', done && 'text-muted-foreground')}>
        {task.title}
        {task.mandatory && <span className="ml-2 type-caption text-foreground">Bắt buộc</span>}
      </span>
      <span className="flex items-center gap-2">
        <ResidentAvatar name={task.resident} code={resident?.code ?? task.resident} initials={resident?.initials} className="size-7" />
        <span className="flex min-w-0 flex-col">
          <span className={cn('truncate type-label', done && 'text-muted-foreground')}>{task.resident}</span>
          <span className="type-caption text-muted-foreground tabular-nums">{task.room}</span>
        </span>
      </span>
      {/* Người được phân công (8.4); việc chưa ai nhận ghi rõ để người phụ trách ca phân */}
      <span className="mt-auto border-t pt-2 type-caption text-muted-foreground">
        {task.assignee ? `Phụ trách: ${task.assignee}` : 'Chưa phân công'}
      </span>
    </li>
  )
}

// Công việc trong ca theo khung giờ (DESIGN.md 5.1): mỗi khung một hàng, trong hàng là lưới thẻ việc vì một
// khung giờ có nhiều việc của nhiều người cao tuổi, nhiều người phụ trách. Khung đang diễn ra ghi "Bây giờ".
function TaskSlots({ now }) {
  const slots = Object.entries(Object.groupBy(DEMO_TASKS, (t) => slotOf(t.time)))
  const current = slotOf(now)
  return (
    <Card>
      <CardHeader>
        <CardTitle className="type-title">Công việc trong ca theo giờ</CardTitle>
      </CardHeader>
      <CardContent>
        <ol className="flex flex-col">
          {slots.map(([slot, tasks]) => (
            <li
              key={slot}
              className="grid gap-3 border-t py-4 first:border-t-0 first:pt-0 last:pb-0 md:grid-cols-[5.5rem_minmax(0,1fr)]"
            >
              <div className="flex items-baseline gap-2 md:flex-col md:gap-0.5">
                <span className={cn('type-title tabular-nums', slot === current && 'text-accent-foreground')}>{slot}</span>
                <span className="type-caption text-muted-foreground">
                  {slot === current ? 'Bây giờ, ' : ''}
                  {tasks.length} việc
                </span>
              </div>
              <ul className="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-3 *:max-w-[360px]">
                {tasks.map((task) => (
                  <TaskTile key={task.time + task.title + task.resident} task={task} />
                ))}
              </ul>
            </li>
          ))}
        </ol>
      </CardContent>
    </Card>
  )
}

// Giường của tầng theo phòng; màu theo trạng thái, chú thích có chữ và số đếm (màu không là tín hiệu duy nhất)
function BedMap() {
  const bedCounts = DEMO_BEDS.reduce((acc, s) => ({ ...acc, [s]: (acc[s] ?? 0) + 1 }), {})
  return (
    <Card>
      <CardHeader>
        <CardTitle className="type-title">Giường của tầng</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <ul className="grid grid-cols-[repeat(auto-fill,minmax(88px,1fr))] gap-2" aria-hidden="true">
          {DEMO_ROOMS.map(({ room, beds }) => (
            <li key={room} className="flex flex-col gap-1.5 rounded-lg bg-muted p-2">
              <span className="type-caption text-muted-foreground tabular-nums">{room}</span>
              <span className="grid grid-cols-4 gap-1">
                {beds.map((status, i) => (
                  <span key={i} title={`${room}-${'ABCD'[i]}, ${status}`} className={cn('h-7 rounded-md', BED_STYLE[status])} />
                ))}
              </span>
            </li>
          ))}
        </ul>
        <ul className="grid grid-cols-2 gap-x-4 gap-y-2 type-body-sm">
          {Object.entries(bedCounts).map(([status, count]) => (
            <li key={status} className="flex items-center gap-2">
              <span className={cn('size-4 shrink-0 rounded', BED_STYLE[status])} aria-hidden="true" />
              <span className="text-muted-foreground">{status}</span>
              <span className="ml-auto type-label tabular-nums">{count}</span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

export function DashboardPage({ onNavigate }) {
  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
      <PageHeader
        title={`Tổng quan ${DEMO_SCOPE.floor.toLowerCase()}`}
        subtitle={`${DEMO_SCOPE.shift}, số liệu trong phạm vi bạn được giao. Cập nhật lúc ${DEMO_SCOPE.updatedAt}.`}
      >
        <IndicatorStrip onNavigate={onNavigate} />
      </PageHeader>

      <Alert
        variant="urgent"
        className="rounded-2xl px-4 py-3 has-data-[slot=alert-action]:pr-4 sm:has-data-[slot=alert-action]:pr-40"
      >
        <OctagonAlert aria-hidden="true" />
        <AlertTitle>
          Cảnh báo mức {DEMO_ESCALATED_ALERT.level.toLowerCase()}, phòng {DEMO_ESCALATED_ALERT.room}
        </AlertTitle>
        <AlertDescription>{DEMO_ESCALATED_ALERT.note}</AlertDescription>
        <AlertAction className="static col-start-2 mt-2 sm:absolute sm:top-1/2 sm:right-3 sm:mt-0 sm:-translate-y-1/2">
          <Button size="sm">Mở cảnh báo</Button>
        </AlertAction>
      </Alert>

      <TaskSlots now={DEMO_SCOPE.updatedAt} />

      <div className="grid gap-4 lg:grid-cols-2">
        <BedMap />

        <Card>
          <CardHeader>
            <CardTitle className="type-title">Bàn giao ca</CardTitle>
          </CardHeader>
          {/* Bàn giao ca (13.5): ai bàn giao, ai xác nhận, còn gì tồn, ai phải làm tiếp */}
          <CardContent className="flex flex-col gap-3">
            <div className="flex items-center justify-between gap-3">
              <span className="type-label">
                {DEMO_HANDOVER.from} → {DEMO_HANDOVER.to}
              </span>
              <StatusBadge status={DEMO_HANDOVER.status} />
            </div>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 type-body-sm">
              <div className="flex flex-col">
                <dt className="text-muted-foreground">Người bàn giao</dt>
                <dd>{DEMO_HANDOVER.giver}</dd>
              </div>
              <div className="flex flex-col">
                <dt className="text-muted-foreground">Người xác nhận</dt>
                <dd>{DEMO_HANDOVER.receiver}</dd>
              </div>
              <div className="flex flex-col">
                <dt className="text-muted-foreground">Công việc tồn</dt>
                <dd className="tabular-nums">{DEMO_HANDOVER.openTasks}</dd>
              </div>
              <div className="flex flex-col">
                <dt className="text-muted-foreground">Cảnh báo chưa xử lý</dt>
                <dd className="tabular-nums">{DEMO_HANDOVER.openAlerts}</dd>
              </div>
            </dl>
            {DEMO_HANDOVER.status === 'Có ý kiến' && (
              // Q-87: Có ý kiến quay lại người bàn giao; ca chưa đóng tới khi gửi lại
              <p className="rounded-lg bg-muted px-3 py-2 type-body-sm">
                <span className="font-medium">Chờ {DEMO_HANDOVER.giver} bổ sung và gửi lại. </span>
                {DEMO_HANDOVER.comment}
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
