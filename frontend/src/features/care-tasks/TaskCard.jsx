import { Building2 } from 'lucide-react'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { cn } from '@/lib/utils'
import { DONE, sourceText, timeText } from './taskText'

// Việc gắn phòng, giường, khu vực hiện vị trí thay cho người cao tuổi (005 FR-030, CHK042)
export function WhoLine({ task, muted }) {
  if (!task.nctId) {
    return (
      <div className="flex items-center gap-2.5">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground" aria-hidden="true">
          <Building2 className="size-4" />
        </span>
        <span className={cn(muted && 'text-muted-foreground')}>{task.location ?? 'Việc chung của tầng'}</span>
      </div>
    )
  }
  return (
    <div className="flex items-center gap-2.5">
      <ResidentAvatar name={task.resident} code={String(task.nctId)} photo={task.photo} />
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

// Khối việc trong dòng thời gian ca (DESIGN.md 7.2 task-block): đúng các trường của hợp đồng CongViec theo
// 005 FR-030, không thêm trường ngoài hợp đồng; kết quả việc đã đóng chỉ ở khung chi tiết. Không có nút bên trong,
// bấm khối mở khung chi tiết. Nội dung là span để hợp lệ trong button.
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
          {timeText(task)}
        </span>
        <StatusBadge status={task.status} className="h-auto" />
      </span>
      <span className="flex flex-col">
        <span className={cn('type-label', done ? 'text-muted-foreground' : 'text-foreground')}>
          {task.title}
          {task.importance && task.importance !== 'Thường' && (
            <span className="ml-1.5 type-caption text-muted-foreground">{task.importance}</span>
          )}
        </span>
        {task.content && <span className="text-muted-foreground">{task.content}</span>}
      </span>
      {task.nctId ? (
        <span className="flex items-center gap-2">
          <ResidentAvatar name={task.resident} code={String(task.nctId)} photo={task.photo} className="size-6" />
          <span className="truncate">
            {task.resident}, <span className="tabular-nums">{task.bed}</span>
          </span>
        </span>
      ) : (
        <span className="flex items-center gap-2">
          <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground" aria-hidden="true">
            <Building2 className="size-3.5" />
          </span>
          {task.location ?? 'Việc chung của tầng'}
        </span>
      )}
      {!done && (
        <>
          <span className="text-muted-foreground">{sourceText(task)}</span>
          {task.awaitingRemeasure && <span className="text-warning-foreground">Chờ đo lại</span>}
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
