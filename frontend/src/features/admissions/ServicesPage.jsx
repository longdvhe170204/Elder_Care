import { useCallback, useEffect, useState } from 'react'
import { getServices } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { SERVICE_BILLING_PERIODS, SERVICE_TYPES, labelOf } from './admissionLabels'
import { ServiceDetail, ServiceSheet } from './ServiceParts'

// Dịch vụ, đơn giá (004 T068; US9, FR-034 → FR-036): Quản lý viện cấu hình, Hành chính xem (Phụ lục 27 dòng "Dịch vụ,
// đơn giá"). Bảng danh mục, khung chi tiết có phiên bản đơn giá và hợp đồng lệch giá.

export function ServicesPage() {
  const { can } = usePermissions()
  const [services, setServices] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(
    () =>
      getServices({ query: {} })
        .then(({ data }) => {
          setServices(data)
          setFailure(null)
        })
        .catch((e) => {
          setServices([])
          setFailure(problemOf(e))
        }),
    [],
  )

  useEffect(() => {
    load()
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Dịch vụ, đơn giá" />

  const current = (services ?? []).find((s) => s.id === opened)

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết dịch vụ"
      renderDetail={(titles) => (
        <ServiceDetail
          key={opened}
          service={current}
          canUpdate={can('SERVICE_PRICE', 'UPDATE')}
          onChanged={changed}
          onEdit={(service) => setSheet({ service })}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Dịch vụ, đơn giá"
        subtitle="Danh mục dịch vụ và các phiên bản đơn giá; hợp đồng ghi đơn giá tại lúc ký."
        aside={
          can('SERVICE_PRICE', 'CREATE') && (
            <Button className="h-11" onClick={() => setSheet({})}>
              Tạo dịch vụ
            </Button>
          )
        }
      />
      {sheet && (
        <ServiceSheet
          service={sheet.service}
          onClose={() => setSheet(null)}
          onDone={(message) => {
            setSheet(null)
            changed(message)
          }}
        />
      )}
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {services === null ? (
        <Skeleton className="h-24 w-full" />
      ) : services.length === 0 ? (
        !failure && <p className="type-body-sm text-muted-foreground">Chưa có dịch vụ nào trong danh mục.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Danh mục dịch vụ">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Dịch vụ</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Đơn vị, kỳ tính</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {services.map((s) => (
                <TableRow key={s.id} className={s.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4">
                    <button
                      type="button"
                      className="text-left underline-offset-4 hover:underline"
                      aria-pressed={s.id === opened}
                      onClick={() => setOpened(s.id)}
                    >
                      {s.name}
                    </button>
                  </TableCell>
                  <TableCell className="whitespace-normal">{labelOf(SERVICE_TYPES, s.type)}</TableCell>
                  <TableCell className="whitespace-normal">
                    {s.unit}
                    <span className="block type-caption text-muted-foreground">
                      {labelOf(SERVICE_BILLING_PERIODS, s.billingPeriod)}
                    </span>
                  </TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={s.status === 'ACTIVE' ? 'Hiệu lực' : 'Ngừng hiệu lực'} />
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
