import { useCallback, useEffect, useState } from 'react'
import { getResidencyChangeRequests, getResidencyChangeRequestsById } from '@/api'
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
import { CHANGE_TYPES, REQUEST_STATUS, labelOf } from './admissionLabels'
import { ChangeRequestSheet, ChangeRequestView } from './ChangeParts'

// Yêu cầu thay đổi lưu trú (004 T047; US5, FR-037 → FR-047): bảng yêu cầu lọc theo trạng thái (GET
// /residency-change-requests, "Đổi api" 004 A duyệt 2026-11-07), chọn dòng để xem, sửa ở Nháp; gửi duyệt, duyệt dùng màn Yêu
// cầu phê duyệt (api-000). Mã yêu cầu là approvalRequestId.

const FILTERS = [
  ['DRAFT', 'Nháp', ['DRAFT']],
  ['PENDING_APPROVAL', 'Chờ duyệt', ['PENDING_APPROVAL']],
  ['APPROVED', 'Đã duyệt', ['APPROVED']],
  ['DONE', 'Đã xong', ['APPLIED', 'APPLY_FAILED', 'REJECTED', 'CANCELLED']],
]

export function ChangeRequestsPage() {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('DRAFT')
  const [requests, setRequests] = useState(null)
  const [shown, setShown] = useState(null)
  const [failure, setFailure] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(
    () =>
      getResidencyChangeRequests({ query: {} })
        .then(({ data }) => {
          setRequests(data)
          setFailure(null)
        })
        .catch((e) => {
          setRequests([])
          setFailure(problemOf(e))
        }),
    [],
  )

  useEffect(() => {
    load()
  }, [load])

  function open(id) {
    setFailure(null)
    return getResidencyChangeRequestsById({ path: { id } })
      .then(({ data, etag }) => setShown({ request: data, etag }))
      .catch((e) => {
        setShown(null)
        setFailure(problemOf(e))
      })
  }

  function changed(message, id) {
    setAnnouncement(message)
    load()
    if (id) open(id)
  }

  if (failure?.status === 403) return <PermissionDeniedState label="Yêu cầu thay đổi lưu trú" />

  const statusesOf = (key) => FILTERS.find(([k]) => k === key)[2]
  const rows = (requests ?? []).filter((r) => statusesOf(filter).includes(r.status))
  const openedId = shown?.request.approvalRequestId

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-5">
      <PageHeader
        title="Yêu cầu thay đổi lưu trú"
        subtitle="Đổi loại lưu trú, mức chăm sóc, dịch vụ, thời hạn, giường hoặc lịch đến của hợp đồng đang hiệu lực."
        aside={
          can('RESIDENCY_CHANGE_REQUEST', 'CREATE') && (
            <Button className="h-11" onClick={() => setSheet({})}>
              Lập yêu cầu thay đổi
            </Button>
          )
        }
      />
      {sheet && (
        <ChangeRequestSheet
          request={sheet.request}
          etag={sheet.etag}
          onClose={() => setSheet(null)}
          onDone={(message, id) => {
            setSheet(null)
            changed(message, id)
          }}
        />
      )}
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[filter]}
        onValueChange={(n) => n[0] && setFilter(n[0])}
        aria-label="Lọc yêu cầu theo trạng thái"
      >
        {FILTERS.map(([key, label, statuses]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(requests ?? []).filter((r) => statuses.includes(r.status)).length}`}
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
        !failure && <p className="type-body-sm text-muted-foreground">Không có yêu cầu nào trong nhóm này.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Yêu cầu thay đổi lưu trú">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Yêu cầu</TableHead>
                <TableHead>Thay đổi</TableHead>
                <TableHead>Ngày hiệu lực mong muốn</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow
                  key={r.approvalRequestId}
                  className={r.approvalRequestId === openedId ? 'bg-accent/40' : undefined}
                >
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={r.approvalRequestId === openedId}
                      onClick={() => open(r.approvalRequestId)}
                    >
                      {`Yêu cầu mã ${r.approvalRequestId}`}
                    </button>
                    <span className="block type-caption text-muted-foreground">{`Hợp đồng mã ${r.contractId}`}</span>
                  </TableCell>
                  <TableCell className="whitespace-normal">
                    {r.changeTypes.map((t) => labelOf(CHANGE_TYPES, t)).join(', ')}
                  </TableCell>
                  <TableCell className="tabular-nums">{formatDate(r.requestedEffectiveDate)}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={REQUEST_STATUS[r.status] ?? r.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
      {shown && (
        <ChangeRequestView
          key={shown.request.approvalRequestId}
          request={shown.request}
          canUpdate={can('RESIDENCY_CHANGE_REQUEST', 'UPDATE')}
          onEdit={(request) => setSheet({ request, etag: shown.etag })}
          onReload={() => open(shown.request.approvalRequestId)}
        />
      )}
    </div>
  )
}
