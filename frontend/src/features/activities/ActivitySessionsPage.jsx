import { useEffect, useState } from 'react'
import { getActivities, getActivitySessions, getResidents } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { TripDetail } from './TripDetail'
import { ActivitySessionDetail } from './ActivitySessionDetail'
import { CreateFromTemplateSheet, CreateSingleSheet } from './ActivitySessionForms'
import { SESSION_STATUS, TRIP_STATUS, addDays, todayInVietnam, weekOf } from './sessionView'

const ALL_STATUS = { ...SESSION_STATUS, ...TRIP_STATUS }
const FILTERS = [['ALL', 'Tất cả'], ...Object.entries(ALL_STATUS)]

// 014 T009 (FR-006 → FR-014a; bố cục duyệt 2026-11-05): buổi trong tuần của tầng, khung chi tiết có Dời buổi, Hủy
// buổi; nút chính Tạo buổi từ mẫu, nút phụ Tạo buổi lẻ. Không tự tạo buổi (Q-306).
export function ActivitySessionsPage({ floorAreaId, initialDate }) {
  const { status, can } = usePermissions()
  const permitted = can('ACTIVITY_OUTING', 'VIEW')
  const canCreate = can('ACTIVITY_OUTING', 'CREATE')
  const canUpdate = can('ACTIVITY_OUTING', 'UPDATE')
  const [week, setWeek] = useState(() => weekOf(initialDate ?? todayInVietnam()))
  const [filter, setFilter] = useState('ALL')
  const [selectedId, setSelectedId] = useState(null)
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState(null)
  const [activities, setActivities] = useState([])
  const [residents, setResidents] = useState([])
  const [sheet, setSheet] = useState(null)
  const [message, setMessage] = useState('')
  const requestKey = `${week.from}:${floorAreaId ?? 'all'}:${revision}:${status}:${permitted}`
  const current = result?.key === requestKey ? result : null

  useEffect(() => {
    if (status !== 'ready' || !permitted) return
    let active = true
    getActivitySessions({ query: { from: week.from, to: week.to, floorAreaId } })
      .then(({ data }) => active && setResult({ key: requestKey, rows: data }))
      .catch((error) => active && setResult({ key: requestKey, rows: [], failure: problemOf(error) }))
    return () => {
      active = false
    }
  }, [status, permitted, week, floorAreaId, requestKey])

  useEffect(() => {
    if (status !== 'ready' || !permitted) return
    let active = true
    getActivities({ query: { floorAreaId } })
      .then(({ data }) => active && setActivities(data))
      .catch(() => active && setActivities([]))
    return () => {
      active = false
    }
  }, [status, permitted, floorAreaId])

  // Họ tên người cao tuổi cho bảng đăng ký và ô chọn (001, như màn Điểm danh bán trú của 005); lỗi thì chỉ hiện mã.
  useEffect(() => {
    if (status !== 'ready' || !permitted) return
    let active = true
    getResidents()
      .then(({ data }) => active && setResidents(data.items ?? []))
      .catch(() => active && setResidents([]))
    return () => {
      active = false
    }
  }, [status, permitted])

  if (status === 'loading') return <Skeleton className="h-24" />
  if (!permitted || current?.failure?.status === 403) return <PermissionDeniedState label="Buổi hoạt động" />

  const names = new Map(activities.map((a) => [a.id, a.name]))
  const nameOf = (id) => names.get(id) ?? `Hoạt động ${id}`
  const rows = current?.rows ?? []
  const counts = Object.fromEntries(
    FILTERS.map(([code]) => [code, code === 'ALL' ? rows.length : rows.filter((row) => row.status === code).length]),
  )
  const visible = filter === 'ALL' ? rows : rows.filter((s) => s.status === filter)
  const selected = rows.find((s) => s.id === selectedId)
  const activeOptions = activities.filter((a) => a.status === 'ACTIVE').map((a) => [String(a.id), a.name])
  const done = (text) => {
    setSheet(null)
    setMessage(text)
    setRevision((v) => v + 1)
  }

  return (
    <DetailLayout
      open={Boolean(selected)}
      detailKey={selectedId}
      onClose={() => setSelectedId(null)}
      label="Chi tiết buổi hoạt động"
      renderDetail={({ Title, Description }) =>
        selected &&
        (selected.isTrip ? (
          <TripDetail
            key={selected.id}
            sessionId={selected.id}
            residents={residents}
            canCreate={canCreate}
            canUpdate={canUpdate}
            onChanged={done}
            Title={Title}
            Description={Description}
          />
        ) : (
          <ActivitySessionDetail
            key={selected.id}
            sessionId={selected.id}
            activityName={nameOf(selected.activityId)}
            residents={residents}
            canCreate={canCreate}
            canUpdate={canUpdate}
            onChanged={done}
            Title={Title}
            Description={Description}
          />
        ))
      }
    >
      <PageHeader
        title="Buổi hoạt động"
        subtitle={`Tuần ${formatDate(week.from)} → ${formatDate(week.to)}`}
        aside={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" size="lg" onClick={() => setWeek(weekOf(addDays(week.from, -7)))}>
              Tuần trước
            </Button>
            <Button variant="outline" size="lg" onClick={() => setWeek(weekOf(addDays(week.from, 7)))}>
              Tuần sau
            </Button>
            {canCreate && (
              <>
                <Button variant="outline" size="lg" onClick={() => setSheet('single')}>
                  Tạo buổi lẻ
                </Button>
                <Button size="lg" onClick={() => setSheet('template')}>
                  Tạo buổi từ mẫu
                </Button>
              </>
            )}
          </div>
        }
      >
        <ToggleGroup
          variant="filter"
          size="lg"
          value={[filter]}
          onValueChange={(next) => next[0] && setFilter(next[0])}
          aria-label="Lọc theo trạng thái buổi"
        >
          {FILTERS.map(([code, label]) => (
            <ToggleGroupItem key={code} value={code} className="h-11 px-4">
              {label} <span className="tabular-nums">{counts[code]}</span>
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </PageHeader>

      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {message}
      </p>
      <ProblemAlert problem={current?.failure} />

      {!current ? (
        <Skeleton className="h-40" />
      ) : visible.length === 0 ? (
        <Card>
          <CardContent className="type-body-sm">
            {rows.length === 0
              ? 'Tuần này chưa có buổi. Trưởng tầng tạo buổi bằng "Tạo buổi từ mẫu" hoặc "Tạo buổi lẻ".'
              : 'Không có buổi ở trạng thái này.'}
          </CardContent>
        </Card>
      ) : (
        <Card className="overflow-x-auto py-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Bắt đầu</TableHead>
                <TableHead>Hoạt động</TableHead>
                <TableHead className="text-right">Đăng ký</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {visible.map((s) => {
                const start = formatDateTime(s.startsAt)
                const name = nameOf(s.activityId)
                return (
                  <TableRow key={s.id} className={selectedId === s.id ? 'bg-accent' : undefined}>
                    <TableCell className="tabular-nums">
                      <button
                        type="button"
                        className="min-h-11 text-left underline-offset-4 hover:underline focus-visible:outline-ring"
                        aria-pressed={selectedId === s.id}
                        aria-label={`Chi tiết buổi ${name} ${start}`}
                        onClick={() => setSelectedId(s.id)}
                      >
                        {start}
                      </button>
                    </TableCell>
                    <TableCell>{name}</TableCell>
                    <TableCell className="text-right tabular-nums">{s.registrationCount}</TableCell>
                    <TableCell>
                      <StatusBadge status={ALL_STATUS[s.status] ?? s.status} />
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </Card>
      )}

      <CreateFromTemplateSheet
        open={sheet === 'template'}
        onOpenChange={(next) => setSheet(next ? 'template' : null)}
        activities={activeOptions}
        onDone={done}
      />
      <CreateSingleSheet
        open={sheet === 'single'}
        onOpenChange={(next) => setSheet(next ? 'single' : null)}
        activities={activeOptions}
        onDone={done}
      />
    </DetailLayout>
  )
}
