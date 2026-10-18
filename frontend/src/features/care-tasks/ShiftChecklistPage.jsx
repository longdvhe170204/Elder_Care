import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DEMO_CARE_TASKS, DEMO_RESIDENTS, DEMO_SHIFT } from '@/common/demoData'
import { ClockAside, PageHeader } from '@/common/PageHeader'
import { cn } from '@/lib/utils'
import { DoneTaskCard, TaskCard } from './TaskCard'
import { RecordResultSheet } from './RecordResultSheet'

// Bản mẫu màn Checklist ca (005 User Story 4) dạng lưới thẻ, theo DESIGN.md 2.0.0.
// Dữ liệu minh họa; trạng thái sau khi ghi nhận ở đây chỉ mô phỏng, trạng thái thật do backend trả.

const DONE = ['Hoàn thành', 'Hoàn thành trễ', 'Không thực hiện', 'Hủy']
const GROUPS = [
  { status: 'Quá hạn', open: true },
  { status: 'Đến hạn', open: true },
  { status: 'Chưa đến hạn', open: false },
]

// Dị ứng hiện trên việc ăn, uống (DESIGN.md mục 7.2); thỏa thuận lựa chọn có rủi ro hiện trên mọi việc của
// người đó (005 FR-030, Q-360). Dữ liệu thật: backend trả kèm công việc.
const FOOD_KINDS = ['meal', 'water']
const withAllergies = (t) => {
  const resident = DEMO_RESIDENTS.find((r) => r.code === t.residentCode)
  return {
    ...t,
    allergies: FOOD_KINDS.includes(t.kind) ? (resident?.allergies ?? []) : [],
    riskChoices: resident?.riskChoices ?? [],
  }
}

function minutes(hhmm) {
  const [h, m] = hhmm.split(':').map(Number)
  return h * 60 + m
}

// shift-bar: phần ca đã qua và vạch giờ hiện tại
function ShiftBar({ shift }) {
  const start = minutes(shift.start)
  const passed = Math.min(100, Math.max(0, ((minutes(shift.now) - start) / (minutes(shift.end) - start)) * 100))

  return (
    <div className="flex flex-col gap-1.5" aria-hidden="true">
      <div className="relative h-2 rounded-full bg-muted">
        <div className="absolute inset-y-0 left-0 rounded-full bg-chart-2" style={{ width: `${passed}%` }} />
        <span
          className="absolute -top-1 -bottom-1 w-1 -translate-x-1/2 rounded-full bg-primary ring-2 ring-background"
          style={{ left: `${passed}%` }}
        />
      </div>
      <div className="flex justify-between type-caption text-muted-foreground tabular-nums">
        <span>{shift.start}</span>
        <span>{shift.end}</span>
      </div>
    </div>
  )
}

function TaskGroup({ label, count, defaultOpen, urgent, children }) {
  return (
    <Collapsible defaultOpen={defaultOpen} className="flex flex-col gap-3">
      <CollapsibleTrigger className="group flex min-h-11 w-fit items-center gap-2 rounded-full pr-2">
        <ChevronDown
          className="size-5 -rotate-90 text-foreground transition-transform group-data-[panel-open]:rotate-0"
          aria-hidden="true"
        />
        <span className="type-title">{label}</span>
        <span
          className={cn(
            'rounded-full px-2 type-caption tabular-nums',
            urgent ? 'bg-destructive-subtle text-destructive' : 'bg-muted text-muted-foreground',
          )}
        >
          {count}
        </span>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="grid grid-cols-[repeat(auto-fit,minmax(280px,1fr))] gap-3 *:max-w-[480px]">{children}</div>
      </CollapsibleContent>
    </Collapsible>
  )
}

// Vai trò có quyền T ở dòng "Checklist, ghi nhận công việc" của Phụ lục 27 (QL, BS chỉ xem).
// Bản mẫu thay cho danh sách lệnh được phép do backend trả; backend vẫn kiểm quyền.
const RECORD_ROLES = ['FLOOR_MANAGER', 'NURSING_STAFF', 'CAREGIVER']

export function ShiftChecklistPage({ role, initialFilter = 'all' }) {
  const canRecord = RECORD_ROLES.includes(role)
  const [tasks, setTasks] = useState(() => DEMO_CARE_TASKS.map(withAllergies))
  const [filter, setFilter] = useState(initialFilter)
  const [recording, setRecording] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const shift = DEMO_SHIFT

  const visible = tasks.filter((t) => filter === 'all' || (filter === 'floor' ? t.floorTask : t.status === filter))
  const count = (status) => tasks.filter((t) => t.status === status).length
  const done = visible.filter((t) => DONE.includes(t.status))
  const overdueCount = count('Quá hạn')

  function close(task, patch, message) {
    setTasks((all) => all.map((t) => (t.id === task.id ? { ...t, ...patch } : t)))
    setRecording(null)
    setAnnouncement(message)
  }

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
      <PageHeader
        title={`Checklist ${shift.name.toLowerCase()}`}
        subtitle={`${shift.floor}, bạn phụ trách ${shift.residentsAssigned} người cao tuổi`}
        aside={<ClockAside time={shift.now} caption={`Ca ${shift.start} đến ${shift.end}`} />}
      >
        <ShiftBar shift={shift} />
        <ToggleGroup
          variant="filter"
          size="lg"
          value={[filter]}
          onValueChange={(next) => next[0] && setFilter(next[0])}
          className="flex-wrap"
          aria-label="Lọc công việc"
        >
          <ToggleGroupItem value="all" className="h-11 px-4">
            Tất cả <span className="tabular-nums">{tasks.length}</span>
          </ToggleGroupItem>
          <ToggleGroupItem
            value="Quá hạn"
            className={cn(
              'h-11 px-4',
              overdueCount > 0 && filter !== 'Quá hạn' && 'border-transparent bg-destructive-subtle text-destructive',
            )}
          >
            Quá hạn <span className="tabular-nums">{overdueCount}</span>
          </ToggleGroupItem>
          <ToggleGroupItem value="Đến hạn" className="h-11 px-4">
            Đến hạn <span className="tabular-nums">{count('Đến hạn')}</span>
          </ToggleGroupItem>
          <ToggleGroupItem value="floor" className="h-11 px-4">
            Việc chung của tầng <span className="tabular-nums">{tasks.filter((t) => t.floorTask).length}</span>
          </ToggleGroupItem>
        </ToggleGroup>
      </PageHeader>

      <p className="sr-only" aria-live="polite">
        {announcement}
      </p>

      {visible.length === 0 ? (
        <Empty className="rounded-2xl border bg-card">
          <EmptyHeader>
            <EmptyTitle>Không có công việc nào khớp bộ lọc</EmptyTitle>
            <EmptyDescription>Chọn "Tất cả" để xem toàn bộ công việc của ca.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="flex flex-col gap-8">
          {GROUPS.map((g) => {
            const items = visible.filter((t) => t.status === g.status)
            if (items.length === 0) return null
            return (
              <TaskGroup key={g.status} label={g.status} count={items.length} defaultOpen={g.open} urgent={g.status === 'Quá hạn'}>
                {items.map((t) => (
                  <TaskCard key={t.id} task={t} canRecord={canRecord} onRecord={setRecording} />
                ))}
              </TaskGroup>
            )
          })}
          {done.length > 0 && (
            <TaskGroup label="Đã xong" count={done.length} defaultOpen={false}>
              {done.map((t) => (
                <DoneTaskCard key={t.id} task={t} />
              ))}
            </TaskGroup>
          )}
        </div>
      )}

      <RecordResultSheet
        task={recording}
        now={shift.now}
        onOpenChange={(open) => !open && setRecording(null)}
        onSave={({ value, doneAt }) =>
          close(
            recording,
            {
              // Mô phỏng mục 8.x: ghi nhận trong khung thì Hoàn thành, sau khung thì Hoàn thành trễ.
              status: minutes(doneAt) > minutes(recording.to) ? 'Hoàn thành trễ' : 'Hoàn thành',
              result: [value, `lúc ${doneAt}`].filter(Boolean).join(', '),
            },
            `Đã lưu kết quả: ${recording.title}`,
          )
        }
        onNotDone={({ reasonType, note }) =>
          close(
            recording,
            { status: 'Không thực hiện', result: [reasonType, note].filter(Boolean).join(', ') },
            `Đã ghi nhận không thực hiện: ${recording.title}`,
          )
        }
      />
    </div>
  )
}
