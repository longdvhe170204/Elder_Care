import { useCallback, useEffect, useRef, useState } from 'react'
import { getFamilyStays, getResidentsById, getResidentsByResidentIdRelationships } from '@/api'
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
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { RegisterStaySheet, StayDetail } from './StayParts'
import { STAY_FILTERS, STAY_STATUS_LABELS, locationLabel } from './stayLabels'

// Người thân ở lại (012 T035 màn web; US8, FR-040 → FR-045; bố cục duyệt 2026-11-05, DESIGN.md 6.8.7). Bảng lượt ở lại
// trong phạm vi người xem, lọc theo trạng thái kèm số đếm; khung chi tiết có lệnh theo trạng thái và vai trò (Phụ lục 27
// chú thích 20). Hợp đồng FamilyStay không trả lệnh được phép, nên nút theo vai trò; backend kiểm lại ở lệnh.

export function FamilyStaysPage({ role }) {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('PENDING_CONFIRMATION')
  const [stays, setStays] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [registering, setRegistering] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const [residents, setResidents] = useState({})
  const [members, setMembers] = useState({})
  const requested = useRef(new Set())

  // Tên người cao tuổi từ hồ sơ (api-001), tên người thân từ quan hệ của người cao tuổi; không đọc được thì hiện mã
  const loadNames = useCallback((rows) => {
    for (const id of new Set(rows.map((s) => s.residentId))) {
      if (requested.current.has(id)) continue
      requested.current.add(id)
      getResidentsById({ path: { id } })
        .then(({ data }) => setResidents((n) => ({ ...n, [id]: data.personalInfo?.fullName })))
        .catch(() => {})
      getResidentsByResidentIdRelationships({ path: { residentId: id } })
        .then(({ data }) =>
          setMembers((m) => {
            const next = { ...m }
            for (const r of data)
              next[`${id}:${r.familyMember.id}`] = { name: r.familyMember.fullName, relationship: r.relationship }
            return next
          }),
        )
        .catch(() => {})
    }
  }, [])

  const load = useCallback(
    () =>
      getFamilyStays({ query: {} })
        .then(({ data }) => {
          setStays(data)
          setFailure(null)
          loadNames(data)
        })
        .catch((e) => {
          setStays([])
          setFailure(problemOf(e))
        }),
    [loadNames],
  )

  useEffect(() => {
    load()
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Người thân ở lại" />

  const memberName = (s) => members[`${s.residentId}:${s.familyMemberId}`]?.name ?? `Người thân mã ${s.familyMemberId}`
  const residentName = (s) => residents[s.residentId] ?? `Người cao tuổi mã ${s.residentId}`
  const statusesOf = (key) => STAY_FILTERS.find(([k]) => k === key)[2]
  const rows = (stays ?? []).filter((s) => statusesOf(filter).includes(s.status))
  const current = (stays ?? []).find((s) => s.id === opened)

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết lượt ở lại"
      renderDetail={(titles) => (
        <StayDetail
          key={opened}
          stay={current}
          role={role}
          memberName={memberName(current)}
          residentName={residentName(current)}
          onChanged={changed}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Người thân ở lại"
        subtitle="Lượt ở lại của người cao tuổi trong phạm vi của bạn; Trưởng tầng xác nhận trước khi bắt đầu."
        aside={
          role === 'OFFICE_STAFF' &&
          can('FAMILY_STAY', 'CREATE') && (
            <Button className="h-11" onClick={() => setRegistering(true)}>
              Đăng ký ở lại
            </Button>
          )
        }
      />
      {registering && (
        <RegisterStaySheet
          onClose={() => setRegistering(false)}
          onDone={(message) => {
            setRegistering(false)
            changed(message)
          }}
        />
      )}
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[filter]}
        onValueChange={(n) => n[0] && setFilter(n[0])}
        aria-label="Lọc lượt ở lại"
      >
        {STAY_FILTERS.map(([key, label, statuses]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(stays ?? []).filter((s) => statuses.includes(s.status)).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {stays === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && <p className="type-body-sm text-muted-foreground">Không có lượt ở lại nào trong nhóm này.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Lượt ở lại">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Người ở lại</TableHead>
                <TableHead>Thời gian dự kiến</TableHead>
                <TableHead>Số đêm</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((s) => (
                <TableRow key={s.id} className={s.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={s.id === opened}
                      onClick={() => setOpened(s.id)}
                    >
                      {memberName(s)}
                    </button>
                    <span className="block type-caption text-muted-foreground">với {residentName(s)}</span>
                  </TableCell>
                  <TableCell className="whitespace-normal tabular-nums">
                    {formatDateTime(s.expectedStartAt)} → {formatDateTime(s.expectedEndAt)}
                    <span className="block type-caption text-muted-foreground">
                      {locationLabel(s.location)}
                      {s.mealRegistered ? ', có đăng ký ăn' : ''}
                    </span>
                  </TableCell>
                  <TableCell className="tabular-nums">{s.actualStartAt ? s.nightCount : '—'}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={STAY_STATUS_LABELS[s.status] ?? s.status} />
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
