import { useEffect, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { getChecklist, getCareTasksById, getMeShifts, getShiftsById } from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { ClockAside, PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { CareField } from './CareField'
import { CareChecklistDetail } from './CareChecklistDetail'
import { CareChecklistSources } from './CareChecklistSources'
import { TaskBlock } from './TaskCard'
import { careChecklistView } from './careChecklistView'
import { DONE } from './taskText'

const FILTERS = [
  ['all', 'Tất cả', () => true],
  ['OVERDUE', 'Quá hạn', (t) => t.raw.status === 'OVERDUE'],
  ['DUE', 'Đến hạn', (t) => t.raw.status === 'DUE'],
  ['NOT_YET_DUE', 'Chưa đến hạn', (t) => t.raw.status === 'NOT_YET_DUE'],
  ['done', 'Đã xong', (t) => DONE.includes(t.status)],
]
function Blocks({ children }) {
  return (
    <div className="grid min-w-0 flex-1 grid-cols-[repeat(auto-fill,minmax(260px,1fr))] items-start gap-2 *:max-w-[360px]">
      {children}
    </div>
  )
}

// Cột giờ rộng 52px (DESIGN.md 7.2 shift-timeline); nhóm Quá hạn và giờ đã xong thu gọn thụt cùng cột
function HourColumn({ children }) {
  return (
    <span aria-hidden={children ? undefined : true} className="w-13 shrink-0 pt-3 type-label text-muted-foreground tabular-nums">
      {children}
    </span>
  )
}

export function CareChecklistPage({ shiftId: initialShiftId, role }) {
  const { status, can } = usePermissions()
  const permitted = can('TASK_CHECKLIST', 'VIEW') && role !== 'DOCTOR'
  const [shiftId, setShiftId] = useState(initialShiftId ? String(initialShiftId) : '')
  const [shifts, setShifts] = useState([])
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [failure, setFailure] = useState(null)
  const [stale, setStale] = useState(false)
  const [filter, setFilter] = useState('all')
  const [selectedId, setSelectedId] = useState(null)
  const [busy, setBusy] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const [openHours, setOpenHours] = useState([])
  const selection = useRef(0)
  const generation = useRef(0)
  useEffect(() => {
    if (status !== 'ready' || !permitted || initialShiftId) return
    let current = true
    getMeShifts()
      .then(({ data: list }) => {
        if (current) setShifts(list.filter((s) => !['DRAFT', 'CANCELLED'].includes(s.status)))
      })
      .catch((e) => {
        if (current) setFailure(problemOf(e))
      })
    return () => {
      current = false
    }
  }, [status, permitted, initialShiftId])
  async function refresh(id) {
    const token = ++generation.current
    setLoading(true)
    try {
      const [{ data: checklist }] = await Promise.all([
        getChecklist({ query: { shiftId: id } }),
        getShiftsById({ path: { id } }),
      ])
      if (generation.current !== token) return
      setData(checklist)
      setFailure(null)
      setStale(false)
    } catch (error) {
      if (generation.current === token) throw error
    } finally {
      if (generation.current === token) setLoading(false)
    }
  }
  useEffect(() => {
    if (status !== 'ready' || !permitted || !shiftId) return
    const requests = generation
    let current = true
    const token = ++requests.current
    Promise.resolve().then(() => {
      if (current) setLoading(true)
    })
    Promise.all([getChecklist({ query: { shiftId } }), getShiftsById({ path: { id: shiftId } })])
      .then(([checklist]) => {
        if (current && requests.current === token) {
          setData(checklist.data)
          setFailure(null)
          setStale(false)
        }
      })
      .catch((e) => {
        if (current && requests.current === token) {
          setFailure(problemOf(e))
          setData(null)
        }
      })
      .finally(() => {
        if (current && requests.current === token) setLoading(false)
      })
    return () => {
      current = false
      if (requests.current === token) ++requests.current
    }
  }, [status, permitted, shiftId])
  const view = data ? careChecklistView(data) : null
  const visible = view?.tasks.filter(FILTERS.find((f) => f[0] === filter)[2]) ?? []
  const overdue = visible.filter((t) => t.raw.status === 'OVERDUE')
  const timeline = visible.filter((t) => t.raw.status !== 'OVERDUE')
  const groups = [...new Set(timeline.map((t) => t.plannedAt.slice(0, 2)))]
  const selected = view?.tasks.find((t) => t.id === selectedId)
  async function select(task) {
    if (busy) return
    const token = ++selection.current
    setSelectedId(task.id)
    try {
      const { data: latest } = await getCareTasksById({ path: { id: task.id } })
      if (selection.current === token)
        setData((old) => ({
          ...old,
          tasks: old.tasks.map((t) =>
            t.id === task.id
              ? {
                  ...latest,
                  residentCard: latest.residentCard && {
                    ...latest.residentCard,
                    roomBed: latest.residentCard.roomBed ?? t.residentCard?.roomBed,
                    careNotes: latest.hiddenFields?.includes('content')
                      ? null
                      : (latest.residentCard.careNotes ?? t.residentCard?.careNotes),
                  },
                }
              : t,
          ),
        }))
    } catch (error) {
      if (selection.current === token) setFailure(problemOf(error))
    }
  }
  async function saved(message) {
    setAnnouncement(message)
    setSelectedId(null)
    ++selection.current
    try {
      await refresh(shiftId)
    } catch (error) {
      setStale(true)
      setFailure(problemOf(error))
    }
  }
  if (status === 'loading') return <Skeleton className="h-24" />
  if (!permitted) return <PermissionDeniedState label="Checklist ca" />
  return (
    <DetailLayout
      open={Boolean(selected)}
      detailKey={selectedId}
      onClose={() => {
        if (!busy) {
          setSelectedId(null)
          ++selection.current
        }
      }}
      label="Chi tiết công việc"
      renderDetail={({ Title, Description }) =>
        selected && (
          <CareChecklistDetail
            key={selected.id}
            task={selected}
            reasons={view.reasons}
            Title={Title}
            Description={Description}
            readOnly={stale}
            onSaved={saved}
            onBusy={setBusy}
          />
        )
      }
    >
      <PageHeader
        title="Checklist ca"
        subtitle={
          data
            ? `Cập nhật ${formatDateTime(data.loadedAt)}, ${data.tasks.length} công việc`
            : 'Chọn ca để xem công việc trong phạm vi được giao.'
        }
        aside={view && <ClockAside time={view.now} caption="Giờ cập nhật checklist" />}
      >
        {!initialShiftId && (
          <CareField
            label="Ca làm việc"
            options={Object.fromEntries(
              shifts.map((s) => [s.id, `${formatDateTime(s.startsAt)}–${formatDateTime(s.endsAt)}`]),
            )}
            value={shiftId}
            disabled={busy}
            onChange={(id) => {
              ++generation.current
              setShiftId(id)
              setSelectedId(null)
              setData(null)
              ++selection.current
            }}
          />
        )}
        {view && (
          <ToggleGroup
            variant="filter"
            value={[filter]}
            onValueChange={(next) => next[0] && setFilter(next[0])}
            aria-label="Lọc công việc"
          >
            {FILTERS.map(([value, label, match]) => (
              <ToggleGroupItem key={value} value={value} className="h-11">
                {label} {view.tasks.filter(match).length}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
        )}
      </PageHeader>
      <p role="status" className="sr-only">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {failure && shiftId && (
        <Button
          variant="outline"
          disabled={busy || loading}
          onClick={() => refresh(shiftId).catch((e) => setFailure(problemOf(e)))}
        >
          Tải lại checklist
        </Button>
      )}
      {stale && <p className="type-body-sm">Chưa cập nhật được checklist. Tải lại trước khi ghi tiếp.</p>}
      {loading ? (
        <Skeleton className="h-32" />
      ) : (
        view && (
          <>
            {visible.length === 0 && (
              <Empty className="rounded-lg border bg-card">
                <EmptyHeader>
                  <EmptyTitle>Chưa có công việc khớp bộ lọc</EmptyTitle>
                  <EmptyDescription>Chọn Tất cả hoặc tải lại checklist của ca.</EmptyDescription>
                </EmptyHeader>
              </Empty>
            )}
            {overdue.length > 0 && (
              <section aria-label="Công việc Quá hạn" className="flex flex-col gap-3">
                <h2 className="type-label text-destructive">Quá hạn ({overdue.length})</h2>
                <div className="flex gap-4">
                  <HourColumn />
                  <Blocks>
                    {overdue.map((t) => (
                      <TaskBlock key={t.id} task={t} selected={t.id === selectedId} onSelect={select} hideStatus />
                    ))}
                  </Blocks>
                </div>
              </section>
            )}
            {timeline.length > 0 && (
              <section aria-label="Dòng thời gian ca">
                {groups.map((hour) => {
                  const inHour = timeline.filter((t) => t.plannedAt.startsWith(hour))
                  const blocks = (
                    <Blocks>
                      {inHour.map((t) => (
                        <TaskBlock key={t.id} task={t} selected={t.id === selectedId} onSelect={select} />
                      ))}
                    </Blocks>
                  )
                  // DESIGN.md 11: thu gọn những gì đã xong; giờ chỉ còn việc đã đóng mở lại được
                  if (!inHour.every((t) => DONE.includes(t.status)))
                    return (
                      <div key={hour} className="flex gap-4 border-t py-3">
                        <HourColumn>{hour}:00</HourColumn>
                        {blocks}
                      </div>
                    )
                  const open = openHours.includes(hour)
                  return (
                    <div key={hour} className="flex flex-col gap-2 border-t py-1">
                      <button
                        type="button"
                        aria-expanded={open}
                        onClick={() => setOpenHours((all) => (open ? all.filter((h) => h !== hour) : [...all, hour]))}
                        className="flex min-h-11 items-center gap-4 rounded-md text-left type-body-sm text-muted-foreground"
                      >
                        <span className="w-13 shrink-0 type-label tabular-nums">{hour}:00</span>
                        <span>{inHour.length} việc đã xong</span>
                        <ChevronDown aria-hidden="true" className={open ? 'size-4 rotate-180' : 'size-4'} />
                      </button>
                      {open && (
                        <div className="flex gap-4 pb-2">
                          <span className="w-13 shrink-0" aria-hidden="true" />
                          {blocks}
                        </div>
                      )}
                    </div>
                  )
                })}
              </section>
            )}
            <CareChecklistSources data={data} />
          </>
        )
      )}
    </DetailLayout>
  )
}
