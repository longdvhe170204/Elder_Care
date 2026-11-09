import { useCallback, useEffect, useState } from 'react'
import { getResidenceRegistrationsToDo, getResidents } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { RegistrationDetail } from './RegistrationDetail'
import { TASK_LABELS, TASK_TONES, compareTasks } from './registrationLabels'

// Màn Khai báo cư trú (018 T008, T012, T014; Phụ lục 27 dòng "Khai báo tạm trú, lưu trú": Hành chính thao tác, Quản lý
// viện xem; DESIGN.md bố cục K1 → K7). Bảng người cần khai báo (GET /residence-registrations/to-do), ô tìm người cao tuổi
// để mở khai báo của bất kỳ ai, bấm một dòng mở khung chi tiết theo người.

const DAY_MS = 24 * 60 * 60 * 1000
const FILTER_ORDER = ['OVERDUE', 'TO_CREATE', 'EXPIRING_SOON', 'EXPIRED', 'TO_DEREGISTER']

function overdueText(dueAt) {
  const days = Math.max(1, Math.floor((Date.now() - new Date(dueAt).getTime()) / DAY_MS))
  return `quá hạn ${days} ngày`
}

function ResidentSearch({ onPick }) {
  const [q, setQ] = useState('')
  const [state, setState] = useState({ status: 'idle', items: [] })

  async function search(event) {
    event.preventDefault()
    setState({ status: 'loading', items: [] })
    try {
      const { data } = await getResidents({ query: { q: q.trim() || undefined } })
      setState({ status: 'ready', items: data.items })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <form role="search" className="flex flex-wrap items-center gap-2" onSubmit={search}>
        <Input
          aria-label="Tìm người cao tuổi"
          placeholder="Họ tên hoặc mã hồ sơ"
          className="h-11 w-72"
          value={q}
          onChange={(event) => setQ(event.target.value)}
        />
        <Button type="submit" variant="outline" className="h-11">
          Tìm
        </Button>
      </form>
      {state.status === 'error' && <ProblemAlert problem={state.problem} />}
      {state.status === 'ready' && state.items.length === 0 && (
        <p className="type-body-sm text-muted-foreground">Không có người cao tuổi khớp với từ tìm trong phạm vi của bạn.</p>
      )}
      {state.items.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {state.items.map((r) => (
            <li key={r.id}>
              <Button
                variant="outline"
                className="h-11 gap-2"
                aria-label={`${r.personalInfo.fullName}, ${r.profileCode}`}
                onClick={() => {
                  onPick({ residentId: r.id, fullName: r.personalInfo.fullName })
                  setState({ status: 'idle', items: [] })
                }}
              >
                <ResidentAvatar name={r.personalInfo.fullName} code={String(r.id)} />
                <span className="type-label">{r.personalInfo.fullName}</span>
                <span className="type-caption text-muted-foreground">{r.profileCode}</span>
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function TaskTable({ tasks, opened, onOpen }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Họ tên</TableHead>
          <TableHead>Việc</TableHead>
          <TableHead>Hạn</TableHead>
          <TableHead className="w-24">
            <span className="sr-only">Thao tác</span>
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {tasks.map((task) => {
          const rowKey = `${task.residentId}:${task.taskType}:${task.registrationId ?? ''}`
          return (
            <TableRow
              key={rowKey}
              data-state={opened === rowKey ? 'selected' : undefined}
              className="data-[state=selected]:bg-accent"
            >
              <TableCell>{task.fullName}</TableCell>
              <TableCell>
                <Badge variant={TASK_TONES[task.taskType] ?? 'neutral'}>{TASK_LABELS[task.taskType]}</Badge>
              </TableCell>
              <TableCell className="tabular-nums">
                {task.dueAt ? (
                  <>
                    {formatDate(task.dueAt)}
                    {task.taskType === 'OVERDUE' && (
                      <span className="type-caption block text-muted-foreground">{overdueText(task.dueAt)}</span>
                    )}
                  </>
                ) : (
                  '—'
                )}
              </TableCell>
              <TableCell>
                <Button
                  variant="outline"
                  className="h-11"
                  aria-label={`Mở khai báo của ${task.fullName}`}
                  aria-pressed={opened === rowKey}
                  onClick={() => onOpen({ residentId: task.residentId, fullName: task.fullName, task, key: rowKey })}
                >
                  Mở
                </Button>
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}

export function ResidenceRegistrationPage() {
  const [state, setState] = useState({ status: 'loading', tasks: [] })
  const [filter, setFilter] = useState('ALL')
  const [opened, setOpened] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getResidenceRegistrationsToDo()
      setState({ status: 'ready', tasks: data.toSorted(compareTasks) })
    } catch (error) {
      setState({ status: 'error', tasks: [], problem: problemOf(error) })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách việc khi mở màn
    load()
  }, [load])

  const counts = Object.fromEntries(
    FILTER_ORDER.map((type) => [type, state.tasks.filter((t) => t.taskType === type).length]),
  )
  const shown = filter === 'ALL' ? state.tasks : state.tasks.filter((t) => t.taskType === filter)

  return (
    <DetailLayout
      open={opened != null}
      detailKey={opened?.key ?? opened?.residentId}
      onClose={() => setOpened(null)}
      label="Chi tiết khai báo"
      renderDetail={({ Title, Description }) => (
        <RegistrationDetail
          key={opened.residentId}
          residentId={opened.residentId}
          fullName={opened.fullName}
          task={opened.task}
          Title={Title}
          Description={Description}
          onChanged={load}
        />
      )}
    >
      <PageHeader title="Khai báo cư trú" subtitle="Khai báo tạm trú, lưu trú của người cao tuổi nội trú với cơ quan công an.">
        <ToggleGroup variant="filter" value={[filter]} onValueChange={(next) => next[0] && setFilter(next[0])}>
          <ToggleGroupItem value="ALL">Tất cả {state.tasks.length}</ToggleGroupItem>
          {FILTER_ORDER.filter((type) => counts[type] > 0).map((type) => (
            <ToggleGroupItem key={type} value={type}>
              {TASK_LABELS[type]} {counts[type]}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        <ResidentSearch onPick={setOpened} />
      </PageHeader>
      <Card>
        <CardContent className="flex flex-col gap-4">
          {state.status === 'loading' && <Skeleton className="h-32 w-full" />}
          {state.status === 'error' && (
            <div className="flex flex-col gap-2">
              <ProblemAlert problem={state.problem} />
              <Button variant="outline" className="h-11 self-start" onClick={load}>
                Thử lại
              </Button>
            </div>
          )}
          {state.status === 'ready' && state.tasks.length === 0 && (
            <Empty>
              <EmptyHeader>
                <EmptyTitle>Không có người cần khai báo</EmptyTitle>
                <EmptyDescription>
                  Người nội trú mới vào ở hoặc khai báo sắp hết hạn sẽ hiện ở đây. Dùng ô tìm để mở khai báo của một người bất kỳ.
                </EmptyDescription>
              </EmptyHeader>
            </Empty>
          )}
          {state.status === 'ready' && state.tasks.length > 0 && (
            <TaskTable tasks={shown} opened={opened?.key} onOpen={setOpened} />
          )}
        </CardContent>
      </Card>
    </DetailLayout>
  )
}
