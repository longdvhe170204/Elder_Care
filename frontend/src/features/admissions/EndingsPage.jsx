import { useCallback, useEffect, useRef, useState } from 'react'
import { getResidencyEndings, getResidencyEndingsById, getResidentsById } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { END_CASES, ENDING_STATUS, labelOf } from './admissionLabels'
import { CreateEndingSheet, EndingView } from './EndingParts'

// Kết thúc lưu trú (004 T061, T107; US7, FR-062 → FR-067): bảng hồ sơ kết thúc lọc theo trạng thái (GET /residency-endings,
// "Đổi api" 004 A duyệt 2026-11-07), chọn dòng để xem điều kiện (a) → (h), bàn giao, ngoại lệ; lập mới thì mở ngay hồ sơ
// vừa lập.

const FILTERS = [
  ['PREPARING', 'Đang chuẩn bị'],
  ['COMPLETED', 'Đã kết thúc'],
  ['CANCELLED', 'Đã hủy'],
]

export function EndingsPage({ role }) {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('PREPARING')
  const [endings, setEndings] = useState(null)
  const [ending, setEnding] = useState(null)
  const [failure, setFailure] = useState(null)
  const [creating, setCreating] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const [names, setNames] = useState({})
  const requested = useRef(new Set())

  const loadNames = useCallback((rows) => {
    for (const id of new Set(rows.map((e) => e.residentId))) {
      if (requested.current.has(id)) continue
      requested.current.add(id)
      getResidentsById({ path: { id } })
        .then(({ data }) => setNames((n) => ({ ...n, [id]: data.personalInfo?.fullName })))
        .catch(() => {})
    }
  }, [])

  const load = useCallback(
    () =>
      getResidencyEndings({ query: {} })
        .then(({ data }) => {
          setEndings(data)
          setFailure(null)
          loadNames(data)
        })
        .catch((e) => {
          setEndings([])
          setFailure(problemOf(e))
        }),
    [loadNames],
  )

  useEffect(() => {
    load()
  }, [load])

  function open(id) {
    setFailure(null)
    return getResidencyEndingsById({ path: { id } })
      .then(({ data }) => setEnding(data))
      .catch((e) => {
        setEnding(null)
        setFailure(problemOf(e))
      })
  }

  if (failure?.status === 403) return <PermissionDeniedState label="Kết thúc lưu trú" />

  const nameOf = (e) => names[e.residentId] ?? `Người cao tuổi mã ${e.residentId}`
  const rows = (endings ?? []).filter((e) => e.status === filter)

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-5">
      <PageHeader
        title="Kết thúc lưu trú"
        subtitle="Hồ sơ kết thúc lưu trú của người cao tuổi: điều kiện, bàn giao, ngoại lệ."
        aside={
          can('RESIDENCY_END_DEATH', 'CREATE') && (
            <Button className="h-11" onClick={() => setCreating(true)}>
              Lập hồ sơ kết thúc
            </Button>
          )
        }
      />
      {creating && (
        <CreateEndingSheet
          role={role}
          onClose={() => setCreating(false)}
          onDone={(created) => {
            setCreating(false)
            setAnnouncement('Đã lập hồ sơ kết thúc lưu trú')
            setEnding(created)
            load()
          }}
        />
      )}
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[filter]}
        onValueChange={(n) => n[0] && setFilter(n[0])}
        aria-label="Lọc hồ sơ theo trạng thái"
      >
        {FILTERS.map(([key, label]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(endings ?? []).filter((e) => e.status === key).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {endings === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && <p className="type-body-sm text-muted-foreground">Không có hồ sơ nào trong nhóm này.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Hồ sơ kết thúc lưu trú">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Người cao tuổi</TableHead>
                <TableHead>Trường hợp</TableHead>
                <TableHead>Ngày kết thúc dự kiến</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((e) => (
                <TableRow key={e.id} className={e.id === ending?.id ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={e.id === ending?.id}
                      onClick={() => open(e.id)}
                    >
                      {nameOf(e)}
                    </button>
                    <span className="block type-caption text-muted-foreground">{`Hồ sơ mã ${e.id}`}</span>
                  </TableCell>
                  <TableCell className="whitespace-normal">{labelOf(END_CASES, e.endCase)}</TableCell>
                  <TableCell className="tabular-nums">{formatDate(e.expectedEndDate)}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={ENDING_STATUS[e.status] ?? e.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
      {ending && (
        <EndingView
          key={ending.id}
          ending={ending}
          role={role}
          canUpdate={can('RESIDENCY_END_DEATH', 'UPDATE')}
          onChanged={(message) => {
            setAnnouncement(message)
            open(ending.id)
            load()
          }}
        />
      )}
    </div>
  )
}
