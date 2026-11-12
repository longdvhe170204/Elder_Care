import { useCallback, useEffect, useState } from 'react'
import { getAssetTypes, getAssets, getDepartments } from '@/api'
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
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { AssetDetail, CreateAssetSheet } from './AssetParts'
import { ASSET_FILTERS, flagLabels, statusLabel } from './assetLabels'

// Tài sản (019 T018 màn web; US4, FR-011 → FR-016; bố cục duyệt 2026-11-09 mục (8): danh sách, DetailLayout, lệnh trong
// khung). Backend trả tài sản người xem được thấy (Quản lý viện, Hành chính mọi tài sản; Phụ trách bộ phận của bộ phận
// mình; Trưởng tầng tài sản có vị trí thuộc tầng). Tên loại từ GET /asset-types (bổ sung 2026-11-10), tên bộ phận từ
// GET /departments (008). api-019 chưa có GET /assets/{id} (lịch sử vị trí, bảo trì, hư hỏng) nên khung chi tiết chỉ có
// trường của danh sách; lịch sử thêm khi hợp đồng có schema.

// assetId: mở thẳng một tài sản (016 Q12, từ dòng chi tiết báo cáo)
export function AssetsPage({ assetId = null } = {}) {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('ALL')
  const [assets, setAssets] = useState(null)
  const [types, setTypes] = useState(null)
  const [departments, setDepartments] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(assetId)
  const [creating, setCreating] = useState(false)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(
    () =>
      getAssets({ query: {} })
        .then(({ data }) => {
          setAssets(data)
          setFailure(null)
        })
        .catch((e) => {
          setAssets([])
          setFailure(problemOf(e))
        }),
    [],
  )

  useEffect(() => {
    load()
    getAssetTypes()
      .then(({ data }) => setTypes(data))
      .catch(() => setTypes([]))
    getDepartments()
      .then(({ data }) => setDepartments(data))
      .catch(() => setDepartments([]))
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Tài sản" />

  const typeOf = (id) => types?.find((t) => t.id === id)
  const typeName = (id) => typeOf(id)?.name ?? `Loại mã ${id}`
  const departmentName = (id) => departments?.find((d) => d.id === id)?.name ?? `Bộ phận mã ${id}`
  const statusesOf = (key) => ASSET_FILTERS.find(([k]) => k === key)[2]
  const inFilter = (statuses) => (a) => !statuses || statuses.includes(a.status)
  const rows = (assets ?? []).filter(inFilter(statusesOf(filter)))
  const current = (assets ?? []).find((a) => a.id === opened)

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết tài sản"
      renderDetail={(titles) => (
        <AssetDetail
          key={opened}
          asset={current}
          typeName={typeName(current.typeId)}
          typeCode={typeOf(current.typeId)?.code}
          departmentName={departmentName(current.departmentId)}
          departments={departments}
          types={types}
          onChanged={changed}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Tài sản"
        subtitle="Tài sản của từng bộ phận; Phụ trách bộ phận thực hiện các lệnh, thanh lý do Quản lý viện duyệt."
        aside={
          can('ASSET_VEHICLE', 'CREATE') && (
            <Button className="h-11" onClick={() => setCreating(true)}>
              Ghi tăng tài sản
            </Button>
          )
        }
      />
      {creating && (
        <CreateAssetSheet
          types={types}
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
        aria-label="Lọc tài sản"
      >
        {ASSET_FILTERS.map(([key, label, statuses]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(assets ?? []).filter(inFilter(statuses)).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {assets === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && (
          <Card className="p-6">
            <p className="type-body-sm text-muted-foreground">Chưa có tài sản nào trong nhóm này.</p>
          </Card>
        )
      ) : (
        <Card className="py-0">
          <Table aria-label="Tài sản">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Tài sản</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Bộ phận</TableHead>
                <TableHead>Vị trí</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((a) => (
                <TableRow key={a.id} className={a.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left type-label underline-offset-4 hover:underline"
                      aria-pressed={a.id === opened}
                      onClick={() => setOpened(a.id)}
                    >
                      {a.code}
                    </button>
                    <span className="block type-body-sm text-muted-foreground">{a.name}</span>
                  </TableCell>
                  <TableCell className="whitespace-normal">{typeName(a.typeId)}</TableCell>
                  <TableCell className="whitespace-normal">{departmentName(a.departmentId)}</TableCell>
                  <TableCell className="whitespace-normal">{a.location ?? '—'}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={statusLabel(a.status)} />
                    {(a.flags ?? []).length > 0 && (
                      <span
                        className={
                          a.flags.includes('AWAITING_RESIDENT_TRANSFER')
                            ? 'block type-caption text-warning-foreground'
                            : 'block type-caption text-destructive'
                        }
                      >
                        {flagLabels(a.flags).join(', ')}
                      </span>
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
