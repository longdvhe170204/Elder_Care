import { useEffect, useState } from 'react'
import { getCareTasksOverdue } from '@/api'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { CareChecklistDetail } from './CareChecklistDetail'
import { careChecklistView } from './careChecklistView'
import { TaskBlock } from './TaskCard'

// T046: danh sách theo phạm vi server, không dùng vai trò để suy quyền xử lý.
export function OverdueTasksPage({ floorAreaId, onNavigate }) {
  const { status, can } = usePermissions()
  const permitted = can('OVERDUE_TASK_HANDLING', 'VIEW')
  const [result, setResult] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [revision, setRevision] = useState(0)
  const requestKey = `${floorAreaId ?? 'all'}:${revision}:${status}:${permitted}`
  const current = result?.key === requestKey ? result : null
  const rows = current?.rows ?? []
  const loading = current === null
  const failure = current?.failure
  useEffect(() => {
    let active = true
    if (status !== 'ready' || !permitted) return
    getCareTasksOverdue({ query: { floorAreaId } })
      .then(({ data }) => {
        if (active) setResult({ key: requestKey, rows: data })
      })
      .catch((error) => {
        if (active) setResult({ key: requestKey, rows: [], failure: problemOf(error) })
      })
    return () => {
      active = false
    }
  }, [status, permitted, floorAreaId, requestKey])
  const view = careChecklistView({ tasks: rows })
  const selected = view.tasks.find((t) => t.id === selectedId)
  if (status === 'loading') return <Skeleton className="h-24" />
  if (!permitted || failure?.status === 403) return <PermissionDeniedState label="Công việc quá hạn" />
  return (
    <DetailLayout
      open={Boolean(selected)}
      detailKey={selectedId}
      onClose={() => setSelectedId(null)}
      label="Chi tiết công việc quá hạn"
      renderDetail={({ Title, Description }) =>
        selected && (
          <CareChecklistDetail task={selected} reasons={[]} Title={Title} Description={Description} readOnly />
        )
      }
    >
      <PageHeader
        title="Công việc quá hạn"
        subtitle="Theo dõi các công việc chưa được đóng trong phạm vi của bạn."
        aside={
          <Button variant="outline" disabled={loading} onClick={() => setRevision((v) => v + 1)}>
            Tải lại
          </Button>
        }
      />
      <ProblemAlert problem={failure} />
      {loading ? (
        <Skeleton className="h-24" />
      ) : !failure && rows.length === 0 ? (
        <p role="status" className="type-body text-muted-foreground">
          Không có công việc quá hạn
        </p>
      ) : (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(260px,1fr))] gap-3 *:max-w-[360px]">
          {view.tasks.map((task) => (
            <div key={task.id} className="flex flex-col gap-2">
              <TaskBlock task={task} selected={selectedId === task.id} onSelect={() => setSelectedId(task.id)} />
              <p className="type-body-sm text-muted-foreground">Dự kiến: {formatDateTime(task.raw.plannedAt)}</p>
              <p className="type-body-sm text-muted-foreground">Đã nhắc và báo: {task.raw.reminderCount ?? 0}</p>
            </div>
          ))}
        </div>
      )}
      {onNavigate && can('TASK_CHECKLIST', 'VIEW') && (
        <Button variant="outline" onClick={() => onNavigate('care-tasks')}>
          Mở checklist ca để xử lý
        </Button>
      )}
    </DetailLayout>
  )
}
