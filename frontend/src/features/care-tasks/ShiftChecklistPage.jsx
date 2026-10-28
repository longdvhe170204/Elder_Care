import { useEffect, useRef, useState } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DEMO_CHECKLIST, DEMO_SHIFT, DEMO_TASK_DETAILS, DEMO_TASK_TYPES } from '@/common/demoData'
import { ClockAside, PageHeader } from '@/common/PageHeader'
import { cn } from '@/lib/utils'
import { TaskBlock } from './TaskCard'
import { DONE } from './taskText'
import { TaskDetail, TaskDetailSheet } from './RecordResultSheet'
import { checklistView, resultText } from './checklistView'

// Bản mẫu màn Checklist ca (005 User Story 4) theo DESIGN.md: nhóm Quá hạn trên cùng, rồi dòng thời gian ca theo
// giờ (shift-timeline); khung chi tiết cố định bên phải từ 1280px khi đã chọn việc. Dữ liệu minh họa theo đúng hình
// dạng hợp đồng api-005 (GET /checklist), đọc qua checklistView; trạng thái sau khi ghi nhận ở đây chỉ mô phỏng.

function minutes(hhmm) {
  const [h, m] = hhmm.split(':').map(Number)
  return h * 60 + m
}

function remainingText(shift) {
  const left = minutes(shift.end) - minutes(shift.now)
  if (left <= 0) return 'Ca đã hết'
  const h = Math.floor(left / 60)
  const m = left % 60
  return `còn ${[h && `${h} giờ`, m && `${m} phút`].filter(Boolean).join(' ')}`
}

const WIDE = '(min-width: 1280px)'

// Khung chi tiết cố định khi màn rộng từ 1280px (DESIGN.md mục 5, 10)
function useWide() {
  const [wide, setWide] = useState(() => typeof window !== 'undefined' && !!window.matchMedia?.(WIDE).matches)
  useEffect(() => {
    const query = window.matchMedia?.(WIDE)
    if (!query) return undefined
    const update = () => setWide(query.matches)
    query.addEventListener('change', update)
    return () => query.removeEventListener('change', update)
  }, [])
  return wide
}

const FILTERS = [
  { value: 'all', label: 'Tất cả', match: () => true },
  { value: 'Quá hạn', label: 'Quá hạn', match: (t) => t.status === 'Quá hạn' },
  { value: 'Đến hạn', label: 'Đến hạn', match: (t) => t.status === 'Đến hạn' },
  { value: 'Chưa đến hạn', label: 'Chưa đến hạn', match: (t) => t.status === 'Chưa đến hạn' },
  { value: 'done', label: 'Đã xong', match: (t) => DONE.includes(t.status) },
  { value: 'floor', label: 'Việc chung của tầng', match: (t) => t.floorTask },
]

function BlockGrid({ children }) {
  return <div className="grid min-w-0 flex-1 grid-cols-[repeat(auto-fill,minmax(260px,1fr))] gap-2 *:max-w-[360px]">{children}</div>
}

// Vai trò có quyền T ở dòng "Checklist, ghi nhận công việc" của Phụ lục 27 (QL, BS chỉ xem). Bản mẫu: vai trò khác
// thì bỏ lenhDuocPhep như backend sẽ trả; nút lệnh vẫn đọc từ lenhDuocPhep của công việc.
const RECORD_ROLES = ['FLOOR_MANAGER', 'NURSING_STAFF', 'CAREGIVER']

function demoChecklist(role) {
  const view = checklistView(DEMO_CHECKLIST, DEMO_TASK_TYPES)
  if (RECORD_ROLES.includes(role)) return view
  return { ...view, tasks: view.tasks.map((t) => ({ ...t, commands: [] })) }
}

const DEMO_RESULTS = Object.fromEntries(Object.entries(DEMO_TASK_DETAILS).map(([id, d]) => [id, resultText(d)]))

export function ShiftChecklistPage({ role, initialFilter = 'all' }) {
  const [{ now, reasons }] = useState(() => demoChecklist(role))
  const [tasks, setTasks] = useState(() => demoChecklist(role).tasks)
  const [results, setResults] = useState(DEMO_RESULTS)
  const [filter, setFilter] = useState(initialFilter)
  const [selectedId, setSelectedId] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const wide = useWide()
  // Ca lấy từ 008 (GET /shifts/{id} theo caId); giờ hiện tại theo taiLuc của checklist (NFR-09)
  const shift = { ...DEMO_SHIFT, now }
  const residentCount = new Set(tasks.filter((t) => t.nctId).map((t) => t.nctId)).size

  const matches = FILTERS.find((f) => f.value === filter)?.match ?? (() => true)
  const visible = tasks.filter(matches)
  const overdue = visible.filter((t) => t.status === 'Quá hạn').sort((a, b) => minutes(a.from) - minutes(b.from))
  const timeline = visible.filter((t) => t.status !== 'Quá hạn').sort((a, b) => minutes(a.from) - minutes(b.from))
  const hours = [...new Set(timeline.map((t) => t.from.slice(0, 2)))]
  const nowHour = shift.now.slice(0, 2)
  const nowIndex = hours.findIndex((h) => h > nowHour)
  const selected = tasks.find((t) => t.id === selectedId) ?? null
  // Khung chi tiết cố định chỉ hiện khi đã chọn một việc; chưa chọn thì danh sách rộng hết màn (DESIGN.md mục 5)
  const panelOpen = wide && !!selected
  const panelRef = useRef(null)
  const triggerRef = useRef(null)

  function select(task) {
    triggerRef.current = document.activeElement
    setSelectedId(task.id)
  }

  // Đóng khung thì focus trở về khối đã mở (DESIGN.md 9.2, WCAG 2.1.2)
  function closePanel() {
    setSelectedId(null)
    requestAnimationFrame(() => triggerRef.current?.focus?.())
  }

  // Mở khung thì đưa focus vào khung để người dùng bàn phím, trình đọc màn hình đọc tiếp
  useEffect(() => {
    if (panelOpen) panelRef.current?.focus()
  }, [panelOpen, selectedId])

  // Mô phỏng phản hồi của backend: việc đóng thì không còn lệnh nào; kết quả lưu như CongViecChiTiet
  function close(task, status, result, message) {
    setTasks((all) => all.map((t) => (t.id === task.id ? { ...t, status, commands: [] } : t)))
    setResults((all) => ({ ...all, [task.id]: result }))
    closePanel()
    setAnnouncement(message)
  }

  const detailProps = {
    now: shift.now,
    reasons,
    result: selected ? results[selected.id] : null,
    onSave: ({ value, doneAt, lateReason }) =>
      close(
        selected,
        // Mô phỏng mục 8.x: ghi nhận trong khung thì Hoàn thành, sau khung thì Hoàn thành trễ.
        minutes(doneAt) > minutes(selected.to) ? 'Hoàn thành trễ' : 'Hoàn thành',
        [value, `lúc ${doneAt}`, lateReason].filter(Boolean).join(', '),
        `Đã lưu kết quả: ${selected.title}`,
      ),
    onNotDone: ({ reasonName, note }) =>
      close(selected, 'Không thực hiện', [reasonName, note].filter(Boolean).join(', '), `Đã ghi nhận không thực hiện: ${selected.title}`),
  }

  const nowLine = (
    <div key="now" className="flex items-center gap-4 py-1" role="presentation">
      <span className="w-13 shrink-0 type-caption text-accent-foreground">Bây giờ</span>
      <span className="h-0.5 flex-1 bg-chart-2" />
    </div>
  )

  return (
    <div
      className={cn(
        'mx-auto flex max-w-[1600px] flex-col gap-5',
        panelOpen && 'grid grid-cols-[minmax(0,1fr)_420px] items-start gap-0',
      )}
    >
      <div className={cn('flex min-w-0 flex-col gap-5', panelOpen && 'pr-8')}>
        <PageHeader
          title={`Checklist ${shift.name.toLowerCase()}`}
          subtitle={`${shift.floor}, ${tasks.length} việc, ${residentCount} người cao tuổi`}
          aside={<ClockAside time={shift.now} caption={remainingText(shift)} />}
        >
          <ToggleGroup
            variant="filter"
            value={[filter]}
            onValueChange={(next) => next[0] && setFilter(next[0])}
            className="flex-wrap"
            aria-label="Lọc công việc"
          >
            {FILTERS.map((f) => {
              const n = tasks.filter(f.match).length
              return (
                <ToggleGroupItem
                  key={f.value}
                  value={f.value}
                  className={cn('h-11', f.value === 'Quá hạn' && n > 0 && 'text-destructive aria-pressed:text-destructive')}
                >
                  {f.label} <span className="tabular-nums">{n}</span>
                </ToggleGroupItem>
              )
            })}
          </ToggleGroup>
        </PageHeader>

        <p className="sr-only" aria-live="polite">
          {announcement}
        </p>

        {visible.length === 0 ? (
          <Empty className="rounded-lg border bg-card">
            <EmptyHeader>
              <EmptyTitle>Không có công việc nào khớp bộ lọc</EmptyTitle>
              <EmptyDescription>Chọn "Tất cả" để xem toàn bộ công việc của ca.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <>
            {overdue.length > 0 && (
              <section aria-labelledby="overdue-title" className="flex flex-col gap-2.5">
                <h2 id="overdue-title" className="type-label text-destructive">
                  Quá hạn ({overdue.length})
                </h2>
                <BlockGrid>
                  {overdue.map((t) => (
                    <TaskBlock key={t.id} task={t} selected={t.id === selectedId} onSelect={select} />
                  ))}
                </BlockGrid>
              </section>
            )}

            {timeline.length > 0 && (
              <section aria-label="Dòng thời gian ca" className="flex flex-col">
                {hours.flatMap((h, i) => [
                  i === nowIndex && nowLine,
                  <div key={h} className="flex gap-4 border-t py-3">
                    <span className="w-13 shrink-0 pt-3 type-label text-muted-foreground tabular-nums">{h}:00</span>
                    <BlockGrid>
                      {timeline
                        .filter((t) => t.from.startsWith(h))
                        .map((t) => (
                          <TaskBlock key={t.id} task={t} selected={t.id === selectedId} onSelect={select} />
                        ))}
                    </BlockGrid>
                  </div>,
                ])}
                {nowIndex === -1 && nowLine}
              </section>
            )}
          </>
        )}
      </div>

      {panelOpen && (
        <aside
          ref={panelRef}
          tabIndex={-1}
          aria-label="Chi tiết công việc"
          onKeyDown={(event) => event.key === 'Escape' && closePanel()}
          className="sticky top-0 -my-6 -mr-8 flex h-screen flex-col gap-2 overflow-y-auto border-l bg-card p-6 outline-none"
        >
          <Button variant="ghost" size="icon" className="self-end" aria-label="Đóng chi tiết" onClick={closePanel}>
            <X aria-hidden="true" />
          </Button>
          <TaskDetail key={selected.id} task={selected} {...detailProps} />
        </aside>
      )}

      <TaskDetailSheet
        task={wide ? null : selected}
        onOpenChange={(open) => !open && closePanel()}
        {...detailProps}
      />
    </div>
  )
}
