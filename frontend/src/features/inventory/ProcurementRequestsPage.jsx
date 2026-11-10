import { useCallback, useEffect, useRef, useState } from 'react'
import { getDepartments, getProcurementRequests, getResidentsById } from '@/api'
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
import { formatDate, formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { CreateProcurementSheet, ProcurementDetail } from './ProcurementParts'
import {
  PROCUREMENT_FILTERS,
  PROCUREMENT_STATUS_LABELS,
  money,
  priorityLabel,
  receivedLabel,
  typeLabel,
} from './procurementLabels'

// Yêu cầu mua sắm (019 T008 màn web; US7, FR-008 → FR-008d; bố cục duyệt 2026-11-09 mục (8): danh sách, DetailLayout,
// lệnh trong khung). Backend trả yêu cầu người xem được thấy (Quản lý viện, Kế toán mọi yêu cầu; Phụ trách bộ phận của
// bộ phận mình; người khác yêu cầu mình lập). api-019 chưa có allowedCommands nên nút theo trạng thái và quyền ở
// /me/permissions; backend kiểm lại người lập, Phụ trách bộ phận, Kế toán khác người ghi đã mua. Tên bộ phận đọc từ
// GET /departments (008), họ tên người cao tuổi từ hồ sơ (001); không đọc được thì hiện mã (người dùng chọn 2026-11-10).

export function ProcurementRequestsPage({ role }) {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('ALL')
  const [requests, setRequests] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [creating, setCreating] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const [departments, setDepartments] = useState(null)
  const [residents, setResidents] = useState({})
  const requested = useRef(new Set())

  const loadResidents = useCallback((rows) => {
    const ids = rows.flatMap((r) => r.lines.map((l) => l.residentId)).filter((id) => id != null)
    for (const id of new Set(ids)) {
      if (requested.current.has(id)) continue
      requested.current.add(id)
      getResidentsById({ path: { id } })
        .then(({ data }) => setResidents((n) => ({ ...n, [id]: data.personalInfo?.fullName })))
        .catch(() => {})
    }
  }, [])

  const load = useCallback(
    () =>
      getProcurementRequests({ query: {} })
        .then(({ data }) => {
          setRequests(data)
          setFailure(null)
          loadResidents(data)
        })
        .catch((e) => {
          setRequests([])
          setFailure(problemOf(e))
        }),
    [loadResidents],
  )

  useEffect(() => {
    load()
    getDepartments()
      .then(({ data }) => setDepartments(data))
      .catch(() => setDepartments([]))
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Yêu cầu mua sắm" />

  const departmentName = (id) => departments?.find((d) => d.id === id)?.name ?? `Bộ phận mã ${id}`
  const residentName = (id) => residents[id] ?? `Người cao tuổi mã ${id}`
  const statusesOf = (key) => PROCUREMENT_FILTERS.find(([k]) => k === key)[2]
  const inFilter = (statuses) => (r) => !statuses || statuses.includes(r.status)
  const rows = (requests ?? []).filter(inFilter(statusesOf(filter)))
  const current = (requests ?? []).find((r) => r.id === opened)
  // Kế toán không lập yêu cầu (Q-293); quyền lập theo vai trò và nhiệm vụ Phụ trách bộ phận, backend kiểm lại
  const canCreate = can('PURCHASE_REQUEST', 'CREATE') && role !== 'ACCOUNTANT'

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết yêu cầu mua sắm"
      renderDetail={(titles) => (
        <ProcurementDetail
          key={opened}
          request={current}
          role={role}
          departmentName={departmentName(current.departmentId)}
          residentName={residentName}
          onChanged={changed}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Yêu cầu mua sắm"
        subtitle="Bộ phận lập và tự mua; Quản lý viện duyệt chi phí, Kế toán đối chiếu chứng từ."
        aside={
          canCreate && (
            <Button className="h-11" onClick={() => setCreating(true)}>
              Lập yêu cầu
            </Button>
          )
        }
      />
      {creating && (
        <CreateProcurementSheet
          departments={departments}
          onClose={() => setCreating(false)}
          onDone={(message) => {
            setCreating(false)
            changed(message)
          }}
        />
      )}
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[filter]}
        onValueChange={(n) => n[0] && setFilter(n[0])}
        aria-label="Lọc yêu cầu mua sắm"
      >
        {PROCUREMENT_FILTERS.map(([key, label, statuses]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(requests ?? []).filter(inFilter(statuses)).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {requests === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && (
          <Card className="p-6">
            <p className="type-body-sm text-muted-foreground">Chưa có yêu cầu nào trong nhóm này.</p>
          </Card>
        )
      ) : (
        <Card className="py-0">
          <Table aria-label="Yêu cầu mua sắm">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Yêu cầu</TableHead>
                <TableHead>Bộ phận</TableHead>
                <TableHead className="text-right">Số tiền</TableHead>
                <TableHead>Cần lúc</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.id} className={r.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left type-label underline-offset-4 hover:underline"
                      aria-pressed={r.id === opened}
                      onClick={() => setOpened(r.id)}
                    >
                      {`Yêu cầu #${r.id}`}
                    </button>
                    <span className="block type-body-sm text-muted-foreground">
                      {`${typeLabel(r.type)}, ${r.lines.map((l) => l.name).join(', ')}`}
                    </span>
                  </TableCell>
                  <TableCell className="whitespace-normal">{departmentName(r.departmentId)}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    {money(r.approvedAmount ?? r.estimatedTotal)}
                    <span className="block type-caption text-muted-foreground">
                      {r.approvedAmount != null ? 'được duyệt' : 'dự kiến'}
                    </span>
                  </TableCell>
                  <TableCell className="whitespace-normal tabular-nums">
                    {r.neededBy ? formatDateTime(r.neededBy) : r.neededOn ? formatDate(r.neededOn) : '—'}
                    {r.priority && (
                      <span className="block type-caption text-muted-foreground">{priorityLabel(r.priority)}</span>
                    )}
                  </TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={PROCUREMENT_STATUS_LABELS[r.status] ?? r.status} />
                    {receivedLabel(r) && (
                      <span className="block type-caption text-muted-foreground">{receivedLabel(r)}</span>
                    )}
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
