import { Building2, TriangleAlert } from 'lucide-react'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { cn } from '@/lib/utils'
import { DONE, sourceText } from './taskText'

export function WhoLine({ task, muted }) {
  if (task.floorTask) {
    return (
      <div className="flex items-center gap-2.5">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground" aria-hidden="true">
          <Building2 className="size-4" />
        </span>
        <span className={cn(muted && 'text-muted-foreground')}>Việc chung của tầng</span>
      </div>
    )
  }
  return (
    <div className="flex items-center gap-2.5">
      <ResidentAvatar name={task.resident} code={task.residentCode} initials={task.initials} />
      <div className="flex min-w-0 flex-col">
        <span className={cn('truncate', muted && 'text-muted-foreground')}>{task.resident}</span>
        <span className="type-body-sm text-muted-foreground tabular-nums">{task.bed}</span>
      </div>
    </div>
  )
}

// Người được phân công (8.4). Việc chung của tầng chưa ai nhận thì ghi rõ để người phụ trách ca phân.
export function AssigneeLine({ task }) {
  return (
    <span className="type-body-sm text-muted-foreground">
      {task.assignee ? `Phụ trách: ${task.assignee}` : 'Chưa ai nhận'}
    </span>
  )
}

// Khối việc trong dòng thời gian ca (DESIGN.md 7.2 task-block, 6.3.0): đủ trường của spec 005 FR-030; không có
// nút bên trong, bấm khối mở khung chi tiết. Nội dung là span để hợp lệ trong button.
export function TaskBlock({ task, selected, onSelect }) {
  const done = DONE.includes(task.status)
  const overdue = task.status === 'Quá hạn'

  return (
    <button
      type="button"
      onClick={() => onSelect(task)}
      aria-pressed={selected}
      className={cn(
        'flex w-full flex-col gap-1.5 rounded-md border bg-card p-3 text-left type-body-sm',
        done && 'border-dashed bg-transparent text-muted-foreground',
        selected && 'ring-2 ring-primary',
      )}
    >
      <span className="flex items-center justify-between gap-2">
        <span className={cn('type-caption tabular-nums', overdue ? 'text-destructive' : 'text-muted-foreground')}>
          {task.from}–{task.to}
        </span>
        <StatusBadge status={task.status} className="h-auto" />
      </span>
      <span className={cn('type-label', done ? 'text-muted-foreground' : 'text-foreground')}>
        {task.title}
        {task.importance && task.importance !== 'Thường' && (
          <span className="ml-1.5 type-caption text-muted-foreground">{task.importance}</span>
        )}
      </span>
      {task.floorTask ? (
        <span className="flex items-center gap-2">
          <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground" aria-hidden="true">
            <Building2 className="size-3.5" />
          </span>
          Việc chung của tầng
        </span>
      ) : (
        <span className="flex items-center gap-2">
          <ResidentAvatar name={task.resident} code={task.residentCode} initials={task.initials} className="size-6" />
          <span className="truncate">
            {task.resident}, <span className="tabular-nums">{task.bed}</span>
          </span>
        </span>
      )}
      {done ? (
        task.result && <span>{task.result}</span>
      ) : (
        <>
          <span className="text-muted-foreground">{sourceText(task)}</span>
          {task.allergies?.length > 0 && (
            <span className="flex items-start gap-1.5 text-warning-foreground">
              <TriangleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              Dị ứng: {task.allergies.join(', ')}
            </span>
          )}
          {task.riskChoices?.map((c) => (
            <span key={c.choice} className="line-clamp-2">
              Lựa chọn của người cao tuổi: {c.choice}. Đã trao đổi rủi ro, xem lại {c.reviewOn}.
            </span>
          ))}
          {task.careNote && <span className="line-clamp-2">Lưu ý: {task.careNote}</span>}
        </>
      )}
      <AssigneeLine task={task} />
    </button>
  )
}
