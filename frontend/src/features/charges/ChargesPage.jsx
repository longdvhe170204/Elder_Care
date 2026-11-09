import { useCallback, useEffect, useState } from 'react'
import { getCostsPeriods, getCostsStatements } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { StatementDetail } from './StatementDetail'
import { BillingSources } from './BillingSources'
import { PostFinalizationChanges } from './PostFinalizationChanges'
import { PurchaseRequests } from './PurchaseRequests'
import { AccountingExport } from './AccountingExport'
import { controlClass, money, periodLabel, STATEMENT_STATUS } from './chargeLabels'

// 010 T019: chỉ đọc dữ liệu thật qua client sinh; không lập sẵn bảng, không tự chọn khoản.
export function ChargesPage({ role }) {
  const { status, can } = usePermissions()
  const canView = can('COST_ADJUSTMENT', 'VIEW')
  const [periods, setPeriods] = useState(null)
  const [periodId, setPeriodId] = useState('')
  const [rows, setRows] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [opened, setOpened] = useState(null)
  const [failure, setFailure] = useState(null)
  const [denied, setDenied] = useState(false)
  const [revision, setRevision] = useState(0)
  const [periodRevision, setPeriodRevision] = useState(0)
  const [announcement, setAnnouncement] = useState('')
  const [workspace, setWorkspace] = useState('statements')
  const deny = useCallback(() => {
    setDenied(true)
    setPeriods(null)
    setRows(null)
    setOpened(null)
  }, [])

  useEffect(() => {
    if (status !== 'ready' || !canView) return
    let active = true
    getCostsPeriods()
      .then(({ data }) => {
        if (active) {
          setPeriods(data)
          setFailure(null)
        }
      })
      .catch((error) => {
        if (active) {
          if (error.status === 403) deny()
          else setFailure(problemOf(error))
        }
      })
    return () => {
      active = false
    }
  }, [status, canView, periodRevision, deny])

  useEffect(() => {
    if (!periodId || !canView || denied) return
    let active = true
    getCostsStatements({ query: { periodId: Number(periodId) } })
      .then(({ data }) => {
        if (active) {
          setRows(data)
          setFailure(null)
        }
      })
      .catch((error) => {
        if (active) {
          if (error.status === 403) deny()
          else {
            setRows([])
            setFailure(problemOf(error))
          }
        }
      })
    return () => {
      active = false
    }
  }, [periodId, revision, canView, denied, deny])

  if (denied || (status === 'ready' && !canView) || status === 'error') return <PermissionDeniedState label="Chi phí" />
  const visible = (rows ?? []).filter(
    (row) =>
      (filter === 'ALL' || row.status === filter) && (!search.trim() || String(row.residentId).includes(search.trim())),
  )
  function changed(message) {
    setAnnouncement(message)
    setRevision((value) => value + 1)
  }

  if (workspace === 'purchases' && can('PURCHASE_ON_BEHALF', 'VIEW'))
    return (
      <div className="flex flex-col gap-5">
        <PageHeader title="Đề nghị mua hộ" />
        <Button variant="outline" onClick={() => setWorkspace('statements')}>
          Về bảng chi phí
        </Button>
        <PurchaseRequests />
      </div>
    )

  if (workspace !== 'statements' && can('COST_ADJUSTMENT', 'CREATE'))
    return (
      <div className="space-y-4">
        <PageHeader title={workspace === 'sources' ? 'Lập khoản từ nguồn' : 'Biến động sau chốt'} />
        <Button variant="outline" onClick={() => setWorkspace('statements')}>
          Về bảng chi phí
        </Button>
        {workspace === 'sources' ? <BillingSources onChanged={changed} /> : <PostFinalizationChanges />}
      </div>
    )

  return (
    <DetailLayout
      open={opened !== null}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết bảng chi phí"
      renderDetail={(titles) => (
        <StatementDetail key={opened} id={opened} onChanged={changed} onDenied={deny} {...titles} />
      )}
    >
      <PageHeader title="Chi phí" subtitle="Kiểm tra từng khoản, duyệt và chốt bảng chi phí theo kỳ." />
      {can('PURCHASE_ON_BEHALF', 'VIEW') && (
        <Button variant="outline" onClick={() => setWorkspace('purchases')}>
          Đề nghị mua hộ
        </Button>
      )}
      {can('COST_ADJUSTMENT', 'CREATE') && (
        <div className="flex gap-3">
          <Button onClick={() => setWorkspace('sources')}>Lập khoản từ nguồn</Button>
          <Button variant="outline" onClick={() => setWorkspace('changes')}>
            Biến động sau chốt
          </Button>
        </div>
      )}
      <Card className="p-4">
        <FieldGroup className="sm:flex-row">
          <Field>
            <FieldLabel htmlFor="charge-period">Kỳ chi phí</FieldLabel>
            <select
              id="charge-period"
              className={controlClass}
              value={periodId}
              disabled={!periods}
              onChange={(event) => {
                setPeriodId(event.target.value)
                setRows(null)
                setOpened(null)
              }}
            >
              <option value="">Chọn kỳ chi phí</option>
              {(periods ?? []).map((period) => (
                <option key={period.id} value={period.id}>
                  {periodLabel(period.month)}, {STATEMENT_STATUS[period.status]}
                </option>
              ))}
            </select>
          </Field>
          <Field>
            <FieldLabel htmlFor="charge-resident">Tìm theo mã người cao tuổi</FieldLabel>
            <Input
              id="charge-resident"
              inputMode="numeric"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </Field>
        </FieldGroup>
      </Card>
      {role === 'ACCOUNTANT' && can('PERIOD_CLOSING', 'CREATE') && <AccountingExport periodId={periodId} />}
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {failure && (
        <Button
          variant="outline"
          onClick={() => {
            setPeriodRevision((n) => n + 1)
            setRevision((n) => n + 1)
          }}
        >
          Thử lại
        </Button>
      )}
      {periods === null && !failure ? (
        <Skeleton className="h-24 w-full" />
      ) : !periods?.length ? (
        <Card className="p-4 type-body-sm">Chưa có kỳ chi phí để xem.</Card>
      ) : !periodId ? (
        <Card className="p-4 type-body-sm">Chọn kỳ chi phí để xem các bảng của kỳ đó.</Card>
      ) : (
        <>
          <ToggleGroup
            variant="filter"
            value={[filter]}
            onValueChange={(value) => {
              if (value[0]) setFilter(value[0])
            }}
            aria-label="Trạng thái bảng"
          >
            {Object.entries({ ALL: 'Tất cả', ...STATEMENT_STATUS }).map(([key, label]) => (
              <ToggleGroupItem key={key} value={key}>
                {label} {(rows ?? []).filter((r) => key === 'ALL' || r.status === key).length}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          {rows === null ? (
            <Skeleton className="h-32 w-full" />
          ) : (
            <Card className="py-0">
              {visible.length === 0 ? (
                <p className="p-4 type-body-sm">
                  Không có bảng chi phí khớp bộ lọc. Bảng được lập cùng lệnh tạo khoản chi phí.
                </p>
              ) : (
                <Table aria-label="Bảng chi phí">
                  <TableHeader>
                    <TableRow>
                      <TableHead>Bảng, người cao tuổi</TableHead>
                      <TableHead>Loại bảng</TableHead>
                      <TableHead>Tổng</TableHead>
                      <TableHead>Trạng thái</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {visible.map((row) => (
                      <TableRow key={row.id} data-state={opened === row.id ? 'selected' : undefined}>
                        <TableCell>
                          <Button
                            variant="ghost"
                            aria-label={`Xem bảng #${row.id}`}
                            aria-pressed={opened === row.id}
                            onClick={() => setOpened(row.id)}
                          >
                            Bảng #{row.id}
                          </Button>
                          <p className="type-body-sm text-muted-foreground">Người cao tuổi mã {row.residentId}</p>
                        </TableCell>
                        <TableCell>
                          {row.statementType === 'REGULAR' ? 'Bảng thường' : 'Bảng bổ sung'}
                          {row.finalPeriod && <p className="type-body-sm">Kỳ cuối</p>}
                        </TableCell>
                        <TableCell className="tabular-nums">{money(row.total)}</TableCell>
                        <TableCell>
                          <StatusBadge status={STATEMENT_STATUS[row.status]} />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </Card>
          )}
        </>
      )}
    </DetailLayout>
  )
}
