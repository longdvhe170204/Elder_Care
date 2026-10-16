import { OctagonAlert } from 'lucide-react'
import { StatusBadge } from '@/common/StatusBadge'
import {
  DEMO_BEDS,
  DEMO_ESCALATED_ALERT,
  DEMO_HANDOVER,
  DEMO_INDICATORS,
  DEMO_SCOPE,
  DEMO_TASKS,
} from '@/common/demoData'
import { cn } from '@/lib/utils'

// Bản mẫu bố cục cho dashboard của feature 016 (mục 18.5). Chỉ tiêu, phạm vi, quyền xem
// do 016 quy định; màn này chỉ thử cách trình bày.

const BED_STYLE = {
  'Đang sử dụng': 'bg-primary',
  'Đang giữ chỗ': 'bg-accent ring-1 ring-primary ring-inset',
  Trống: 'bg-card ring-1 ring-input ring-inset',
  'Chờ sẵn sàng': 'bg-warning ring-1 ring-warning-foreground/40 ring-inset',
  'Đang bảo trì': 'bg-muted-foreground/40',
  'Tạm ngừng sử dụng': 'bg-muted ring-1 ring-input ring-inset',
}

function Panel({ title, children, className }) {
  return (
    <section className={cn('rounded-lg border bg-card', className)}>
      <h2 className="border-b px-4 py-2.5 text-sm font-medium">{title}</h2>
      {children}
    </section>
  )
}

export function DashboardPage() {
  const bedCounts = DEMO_BEDS.reduce((acc, s) => ({ ...acc, [s]: (acc[s] ?? 0) + 1 }), {})

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-lg font-medium">
          {DEMO_SCOPE.floor} · {DEMO_SCOPE.shift}
        </h1>
        <span className="text-sm text-muted-foreground tabular-nums">Cập nhật lúc {DEMO_SCOPE.updatedAt}</span>
      </div>

      <div
        role="alert"
        className="flex flex-wrap items-center justify-between gap-2 border-l-4 border-destructive bg-destructive/10 px-4 py-2.5 text-sm text-destructive"
      >
        <span className="flex items-center gap-2">
          <OctagonAlert className="size-4 shrink-0" aria-hidden="true" />
          Cảnh báo {DEMO_ESCALATED_ALERT.level.toLowerCase()} · Phòng {DEMO_ESCALATED_ALERT.room} ·{' '}
          {DEMO_ESCALATED_ALERT.note}
        </span>
        <button type="button" className="font-medium underline-offset-4 hover:underline">
          Mở cảnh báo
        </button>
      </div>

      <dl className="flex flex-wrap gap-x-8 gap-y-3 border-b pb-4">
        {DEMO_INDICATORS.map((ind) => (
          <div key={ind.label} className="flex flex-col">
            <dt className="text-xs text-muted-foreground">{ind.label}</dt>
            <dd className={cn('text-xl font-medium tabular-nums', ind.urgent && 'text-destructive')}>
              {ind.value}
              {ind.detail && <span className="ml-1.5 text-xs font-normal">{ind.detail}</span>}
            </dd>
          </div>
        ))}
      </dl>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <Panel title="Công việc trong ca theo giờ">
          <ol>
            {DEMO_TASKS.map((task, i) => (
              <li
                key={i}
                className={cn(
                  'grid grid-cols-[3.5rem_minmax(0,1fr)_auto] items-center gap-3 border-b px-4 py-2.5 text-sm last:border-b-0',
                  task.status === 'Hoàn thành' && 'text-muted-foreground',
                )}
              >
                <span className={cn('tabular-nums', task.status === 'Quá hạn' && 'text-destructive')}>{task.time}</span>
                <span className="truncate">
                  {task.title} · {task.room}
                  {task.mandatory && <span className="ml-2 text-xs text-destructive">Bắt buộc</span>}
                </span>
                <StatusBadge status={task.status} />
              </li>
            ))}
          </ol>
        </Panel>

        <div className="flex flex-col gap-5">
          <Panel title="Giường của tầng">
            <div className="grid grid-cols-10 gap-1 p-4">
              {DEMO_BEDS.map((status, i) => (
                <span key={i} title={status} className={cn('h-4 rounded-sm', BED_STYLE[status])} />
              ))}
            </div>
            <ul className="grid grid-cols-2 gap-x-4 gap-y-1 px-4 pb-4 text-xs text-muted-foreground">
              {Object.entries(bedCounts).map(([status, count]) => (
                <li key={status} className="flex items-center gap-2">
                  <span className={cn('size-2.5 rounded-sm', BED_STYLE[status])} />
                  {status} <span className="ml-auto tabular-nums text-foreground">{count}</span>
                </li>
              ))}
            </ul>
          </Panel>

          <Panel title="Bàn giao ca">
            <div className="flex items-center justify-between px-4 py-3 text-sm">
              <span>
                {DEMO_HANDOVER.from} → {DEMO_HANDOVER.to}
              </span>
              <StatusBadge status={DEMO_HANDOVER.status} />
            </div>
          </Panel>
        </div>
      </div>
    </div>
  )
}
