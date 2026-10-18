import { Building2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AllergyNote, CareNote, ChoiceNote } from '@/common/CareNotes'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { cn } from '@/lib/utils'

// Thẻ công việc chăm sóc (DESIGN.md 2.0.0, công thức task-card). Chỉ công việc Đến hạn hoặc
// Quá hạn mới có nút Ghi nhận (bảng trạng thái công việc, mục 8.x nghiep-vu.md).
const RECORDABLE = ['Đến hạn', 'Quá hạn']

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
        <span className="type-caption text-muted-foreground tabular-nums">{task.bed}</span>
      </div>
    </div>
  )
}

// Người được phân công (8.4). Việc chung của tầng chưa ai nhận thì ghi rõ để người phụ trách ca phân.
export function AssigneeLine({ task }) {
  return (
    <span className="type-caption text-muted-foreground">
      {task.assignee ? `Phụ trách: ${task.assignee}` : 'Chưa ai nhận'}
    </span>
  )
}

export function TaskCard({ task, canRecord, onRecord }) {
  const overdue = task.status === 'Quá hạn'
  const mandatory = task.importance === 'Bắt buộc'
  const showRecord = canRecord && RECORDABLE.includes(task.status)

  return (
    <Card className={cn('gap-3', overdue && 'ring-[1.5px] ring-destructive')}>
      <CardHeader className="gap-2">
        <div className="flex items-center justify-between gap-2">
          <span className={cn('type-card-title tabular-nums', overdue && 'text-destructive')}>
            {task.from}
            <span className="ml-1 type-caption text-muted-foreground">đến {task.to}</span>
          </span>
          <StatusBadge status={task.status} />
        </div>
        <CardTitle className="type-card-title font-semibold">{task.title}</CardTitle>
      </CardHeader>

      <CardContent className="flex flex-1 flex-col gap-3">
        <WhoLine task={task} />

        <AssigneeLine task={task} />
        <AllergyNote allergies={task.allergies} />
        <ChoiceNote choices={task.riskChoices} lines={2} />
        <CareNote note={task.careNote} lines={2} />

        {(showRecord || mandatory || task.carriedOver) && (
          // Hàng cuối thẻ (DESIGN.md 2.1.0): ghi chú ngắn bên trái, nút Ghi nhận gọn bên phải, luôn ở đáy
          <div className="mt-auto flex min-h-10 items-center justify-between gap-3 border-t pt-3">
            <div className="flex flex-wrap gap-x-3 gap-y-1 type-caption">
              {mandatory && <span className="text-foreground">Bắt buộc</span>}
              {task.carriedOver && <span className="text-muted-foreground">Tồn từ ca trước</span>}
            </div>
            {showRecord && (
              <Button variant={overdue ? 'default' : 'tonal'} onClick={() => onRecord(task)}>
                Ghi nhận
              </Button>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export function DoneTaskCard({ task }) {
  return (
    <Card className="gap-3">
      <CardHeader className="gap-2">
        <div className="flex items-center justify-between gap-2">
          <span className="type-body-sm text-muted-foreground tabular-nums">
            {task.from} đến {task.to}
          </span>
          <StatusBadge status={task.status} />
        </div>
        <CardTitle className="type-body font-medium text-muted-foreground">{task.title}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-2 type-body-sm">
        <WhoLine task={task} muted />
        {task.result && <span className="text-muted-foreground">{task.result}</span>}
        <AssigneeLine task={task} />
      </CardContent>
    </Card>
  )
}
