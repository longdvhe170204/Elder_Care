import { useCallback, useEffect, useRef, useState } from 'react'
import { getContracts, getResidentsById } from '@/api'
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
import { CONTRACT_STATUS, RESIDENCE_TYPES, labelOf } from './admissionLabels'
import { ContractDetail, ContractSheet } from './ContractParts'

// Hợp đồng (004 T038; US3, US4, FR-019 → FR-031): bảng hợp đồng lọc theo trạng thái kèm số đếm, khung chi tiết có lệnh theo
// allowedCommands của hợp đồng (DESIGN.md mục 5, 7.2 detail-panel). Hành chính lập, Quản lý viện xem và duyệt, Kế toán xem
// theo Phụ lục 27 dòng "Tiếp nhận, hợp đồng, đặt cọc"; trường tài chính bị bỏ theo hiddenFields.

const FILTERS = [
  ['DRAFT', 'Nháp', ['DRAFT']],
  ['PENDING_SIGNATURE', 'Chờ ký', ['PENDING_SIGNATURE']],
  ['ACTIVE', 'Hiệu lực', ['ACTIVE']],
  ['DONE', 'Đã xong', ['ENDED', 'TERMINATED', 'CANCELLED']],
]

export function ContractsPage() {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('DRAFT')
  const [contracts, setContracts] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const [names, setNames] = useState({})
  const requested = useRef(new Set())

  const loadNames = useCallback((rows) => {
    for (const id of new Set(rows.map((c) => c.residentId))) {
      if (requested.current.has(id)) continue
      requested.current.add(id)
      getResidentsById({ path: { id } })
        .then(({ data }) => setNames((n) => ({ ...n, [id]: data.personalInfo?.fullName })))
        .catch(() => {})
    }
  }, [])

  const load = useCallback(
    () =>
      getContracts({ query: {} })
        .then(({ data }) => {
          setContracts(data)
          setFailure(null)
          loadNames(data)
        })
        .catch((e) => {
          setContracts([])
          setFailure(problemOf(e))
        }),
    [loadNames],
  )

  useEffect(() => {
    load()
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Hợp đồng" />

  const nameOf = (c) => names[c.residentId] ?? `Người cao tuổi mã ${c.residentId}`
  const statusesOf = (key) => FILTERS.find(([k]) => k === key)[2]
  const rows = (contracts ?? []).filter((c) => statusesOf(filter).includes(c.status))
  const current = (contracts ?? []).find((c) => c.id === opened)

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết hợp đồng"
      renderDetail={(titles) => (
        <ContractDetail
          key={opened}
          contract={current}
          residentName={nameOf(current)}
          canUpdate={can('ADMISSION_CONTRACT_DEPOSIT', 'UPDATE')}
          onChanged={changed}
          onEdit={(contract) => setSheet({ contract })}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Hợp đồng"
        subtitle="Hợp đồng lưu trú, điều khoản, đặt cọc và phụ lục của từng người cao tuổi."
        aside={
          can('ADMISSION_CONTRACT_DEPOSIT', 'CREATE') && (
            <Button className="h-11" onClick={() => setSheet({})}>
              Lập hợp đồng
            </Button>
          )
        }
      />
      {sheet && (
        <ContractSheet
          contract={sheet.contract}
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
        aria-label="Lọc hợp đồng theo trạng thái"
      >
        {FILTERS.map(([key, label, statuses]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(contracts ?? []).filter((c) => statuses.includes(c.status)).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {contracts === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && <p className="type-body-sm text-muted-foreground">Không có hợp đồng nào trong nhóm này.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Hợp đồng">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Người cao tuổi</TableHead>
                <TableHead>Loại lưu trú</TableHead>
                <TableHead>Thời gian</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((c) => (
                <TableRow key={c.id} className={c.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={c.id === opened}
                      onClick={() => setOpened(c.id)}
                    >
                      {nameOf(c)}
                    </button>
                    <span className="block type-caption text-muted-foreground">{c.contractNumber}</span>
                  </TableCell>
                  <TableCell className="whitespace-normal">{labelOf(RESIDENCE_TYPES, c.residenceType)}</TableCell>
                  <TableCell className="whitespace-normal tabular-nums">
                    {`Từ ${formatDate(c.startDate)}${c.endDate ? ` đến ${formatDate(c.endDate)}` : ''}`}
                    {c.overdueSince && (
                      <span className="block type-caption text-destructive">{`Quá hạn từ ${formatDate(c.overdueSince)}`}</span>
                    )}
                  </TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={CONTRACT_STATUS[c.status] ?? c.status} />
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
