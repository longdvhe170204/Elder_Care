import { useCallback, useEffect, useRef, useState } from 'react'
import { getAdmissionApplications, getResidentsById } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { APPLICATION_STATUS, CARE_LEVELS, RESIDENCE_TYPES, labelOf } from './admissionLabels'
import { ApplicationDetail, ApplicationSheet } from './ApplicationParts'

// Đăng ký tiếp nhận (004 T019; US1, FR-001, FR-003 → FR-006): bảng lượt đăng ký lọc theo trạng thái kèm số đếm, khung chi
// tiết có tiến trình, điều kiện tiếp nhận và ý kiến về việc vào ở (DESIGN.md mục 5, 7.2 detail-panel; G1 duyệt 2026-11-05).

const FILTERS = [
  ['IN_PROGRESS', 'Đang tiếp nhận'],
  ['WAITING', 'Chờ giường'],
  ['ADMITTED', 'Đã vào'],
  ['CANCELLED', 'Đã hủy'],
]

export function ApplicationsPage() {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('IN_PROGRESS')
  const [applications, setApplications] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const [names, setNames] = useState({})
  const requested = useRef(new Set())

  const loadNames = useCallback((rows) => {
    for (const id of new Set(rows.map((a) => a.residentId))) {
      if (requested.current.has(id)) continue
      requested.current.add(id)
      getResidentsById({ path: { id } })
        .then(({ data }) => setNames((n) => ({ ...n, [id]: data.personalInfo?.fullName })))
        .catch(() => {})
    }
  }, [])

  const load = useCallback(
    () =>
      getAdmissionApplications({ query: {} })
        .then(({ data }) => {
          setApplications(data)
          setFailure(null)
          loadNames(data)
        })
        .catch((e) => {
          setApplications([])
          setFailure(problemOf(e))
        }),
    [loadNames],
  )

  useEffect(() => {
    load()
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Đăng ký tiếp nhận" />

  const nameOf = (a) => names[a.residentId] ?? `Người cao tuổi mã ${a.residentId}`
  const rows = (applications ?? []).filter((a) => a.status === filter)
  const current = (applications ?? []).find((a) => a.id === opened)

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết đăng ký tiếp nhận"
      renderDetail={(titles) => (
        <ApplicationDetail
          key={opened}
          application={current}
          residentName={nameOf(current)}
          canUpdate={can('ADMISSION_CONTRACT_DEPOSIT', 'UPDATE')}
          onChanged={changed}
          onEdit={(application) => setSheet({ application })}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Đăng ký tiếp nhận"
        subtitle="Lượt đăng ký của người cao tuổi đang được tiếp nhận; điều kiện tiếp nhận kiểm theo từng lượt."
        aside={
          can('ADMISSION_CONTRACT_DEPOSIT', 'CREATE') && (
            <Button className="h-11" onClick={() => setSheet({})}>
              Lập đăng ký
            </Button>
          )
        }
      />
      {sheet && (
        <ApplicationSheet
          application={sheet.application}
          onClose={() => setSheet(null)}
          onDone={(message) => {
            setSheet(null)
            changed(message)
          }}
        />
      )}
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[filter]}
        onValueChange={(n) => n[0] && setFilter(n[0])}
        aria-label="Lọc đăng ký theo trạng thái"
      >
        {FILTERS.map(([key, label]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(applications ?? []).filter((a) => a.status === key).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {applications === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && <p className="type-body-sm text-muted-foreground">Không có đăng ký nào trong nhóm này.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Lượt đăng ký tiếp nhận">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Người cao tuổi</TableHead>
                <TableHead>Ngày đăng ký</TableHead>
                <TableHead>Loại lưu trú, mức chăm sóc</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((a) => (
                <TableRow key={a.id} className={a.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={a.id === opened}
                      onClick={() => setOpened(a.id)}
                    >
                      {nameOf(a)}
                    </button>
                  </TableCell>
                  <TableCell className="tabular-nums">{formatDate(a.applicationDate)}</TableCell>
                  <TableCell className="whitespace-normal">
                    {labelOf(RESIDENCE_TYPES, a.desiredResidenceType)}
                    <span className="block type-caption text-muted-foreground">
                      {labelOf(CARE_LEVELS, a.expectedCareLevel)}
                    </span>
                  </TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={APPLICATION_STATUS[a.status] ?? a.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </DetailLayout>
  )
}
