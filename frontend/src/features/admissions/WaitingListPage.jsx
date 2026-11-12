import { useCallback, useEffect, useState } from 'react'
import { getWaitingListEntries } from '@/api'
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
import { CARE_LEVELS, RESIDENCE_TYPES, WAITING_STATUS, labelOf } from './admissionLabels'
import { AddEntrySheet, EntryBadges, EntryDetail, MatchingSheet } from './WaitingListParts'

// Danh sách chờ (004 T028; US2, FR-007 → FR-018): Ưu tiên trước rồi theo ngày đăng ký (backend sắp), "từng lưu trú" chỉ
// hiển thị để Hành chính cân nhắc (Q-307); hồ sơ phù hợp với giường Trống không chọn sẵn (FR-013). Chỉ Hành chính, Quản
// lý viện xem (FR-018, Phụ lục 27 dòng "Danh sách chờ, dấu ưu tiên").

const FILTERS = [
  ['WAITING', 'Đang chờ', ['WAITING']],
  ['CONTACTED', 'Đã liên hệ', ['CONTACTED']],
  ['DONE', 'Đã xong', ['ADMITTED', 'DECLINED', 'WAITING_CANCELLED']],
]

// entryId: mở thẳng một hồ sơ chờ (016 Q12, từ dòng chi tiết báo cáo)
export function WaitingListPage({ entryId = null } = {}) {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('WAITING')
  const [entries, setEntries] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(entryId)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(
    () =>
      getWaitingListEntries({ query: {} })
        .then(({ data }) => {
          setEntries(data)
          setFailure(null)
        })
        .catch((e) => {
          setEntries([])
          setFailure(problemOf(e))
        }),
    [],
  )

  useEffect(() => {
    load()
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Danh sách chờ" />

  const statusesOf = (key) => FILTERS.find(([k]) => k === key)[2]
  const rows = (entries ?? []).filter((e) => statusesOf(filter).includes(e.status))
  const current = (entries ?? []).find((e) => e.id === opened)

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết hồ sơ chờ"
      renderDetail={(titles) => (
        <EntryDetail
          key={opened}
          entry={current}
          canUpdate={can('WAITING_LIST', 'UPDATE')}
          onChanged={changed}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Danh sách chờ"
        subtitle="Hồ sơ chờ giường theo Ưu tiên rồi ngày đăng ký. Hệ thống không tự chọn người cho giường."
        aside={
          <div className="flex flex-wrap gap-2">
            {can('WAITING_LIST', 'UPDATE') && (
              <Button variant="outline" className="h-11" onClick={() => setSheet('matching')}>
                Hồ sơ phù hợp với giường trống
              </Button>
            )}
            {can('WAITING_LIST', 'CREATE') && (
              <Button className="h-11" onClick={() => setSheet('add')}>
                Đưa vào danh sách chờ
              </Button>
            )}
          </div>
        }
      />
      {sheet === 'matching' && (
        <MatchingSheet
          onClose={() => setSheet(null)}
          onDone={(message) => {
            setSheet(null)
            changed(message)
          }}
        />
      )}
      {sheet === 'add' && (
        <AddEntrySheet
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
        aria-label="Lọc hồ sơ chờ theo trạng thái"
      >
        {FILTERS.map(([key, label, statuses]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(entries ?? []).filter((e) => statuses.includes(e.status)).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {entries === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && <p className="type-body-sm text-muted-foreground">Không có hồ sơ chờ nào trong nhóm này.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Hồ sơ chờ">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Người cao tuổi</TableHead>
                <TableHead>Ngày đăng ký</TableHead>
                <TableHead>Loại lưu trú, mức chăm sóc</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((e) => (
                <TableRow key={e.id} className={e.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={e.id === opened}
                      onClick={() => setOpened(e.id)}
                    >
                      {e.fullName}
                    </button>
                    <EntryBadges entry={e} />
                  </TableCell>
                  <TableCell className="tabular-nums">{formatDate(e.applicationDate)}</TableCell>
                  <TableCell className="whitespace-normal">
                    {labelOf(RESIDENCE_TYPES, e.residenceType)}
                    <span className="block type-caption text-muted-foreground">
                      {labelOf(CARE_LEVELS, e.careLevel)}
                    </span>
                  </TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={WAITING_STATUS[e.status] ?? e.status} />
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
