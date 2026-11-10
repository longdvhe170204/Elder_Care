import { useCallback, useEffect, useRef, useState } from 'react'
import { getAssetTypes, getAssets, getResidentsById, getStaff, getVehicleSchedules } from '@/api'
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
import { BookScheduleSheet, ScheduleDetail } from './VehicleScheduleParts'
import { VEHICLE_CODE } from './assetLabels'
import {
  BOOKER_ROLES,
  SCHEDULE_FILTERS,
  SCHEDULE_STATUS_LABELS,
  addDays,
  purposeLabel,
  todayInVietnam,
  weekOf,
} from './vehicleLabels'

// Lịch xe (019 T022 màn web; US6, FR-017, FR-018; bố cục duyệt 2026-11-09 mục (8): danh sách, DetailLayout, lệnh trong
// khung). Lịch theo tuần (GET /vehicle-schedules from, to), lọc trạng thái kèm số đếm. Tên xe từ GET /assets lọc loại Xe
// (loại lấy từ GET /asset-types), người lái từ GET /staff (008), người cao tuổi từ hồ sơ (001); không đọc được thì hiện
// mã (người dùng chọn 2026-11-10).

export function VehicleSchedulesPage({ role, initialDate }) {
  const { can } = usePermissions()
  const [week, setWeek] = useState(() => weekOf(initialDate ?? todayInVietnam()))
  const [filter, setFilter] = useState('ALL')
  const [schedules, setSchedules] = useState(null)
  const [vehicles, setVehicles] = useState(null)
  const [staff, setStaff] = useState(null)
  const [residents, setResidents] = useState({})
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [booking, setBooking] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const requested = useRef(new Set())

  const loadResidents = useCallback((rows) => {
    for (const id of new Set(rows.map((r) => r.residentId).filter((id) => id != null))) {
      if (requested.current.has(id)) continue
      requested.current.add(id)
      getResidentsById({ path: { id } })
        .then(({ data }) => setResidents((n) => ({ ...n, [id]: data.personalInfo?.fullName })))
        .catch(() => {})
    }
  }, [])

  const load = useCallback(
    () =>
      getVehicleSchedules({ query: { from: week.from, to: week.to } })
        .then(({ data }) => {
          setSchedules(data)
          setFailure(null)
          loadResidents(data)
        })
        .catch((e) => {
          setSchedules([])
          setFailure(problemOf(e))
        }),
    [week, loadResidents],
  )

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    getAssetTypes()
      .then(({ data }) => {
        const type = data.find((t) => t.code === VEHICLE_CODE)
        if (!type) return setVehicles([])
        return getAssets({ query: { typeId: type.id } }).then((r) => setVehicles(r.data))
      })
      .catch(() => setVehicles([]))
    getStaff({ query: { status: 'WORKING' } })
      .then(({ data }) => setStaff(data))
      .catch(() => setStaff([]))
  }, [])

  if (failure?.status === 403) return <PermissionDeniedState label="Lịch xe" />

  const vehicleName = (id) => {
    const v = vehicles?.find((x) => x.id === id)
    return v ? `${v.code}, ${v.name}` : `Xe mã ${id}`
  }
  const driverName = (id) => staff?.find((s) => s.id === id)?.fullName ?? `Nhân viên mã ${id}`
  const residentName = (id) => residents[id] ?? `Người cao tuổi mã ${id}`
  const statusesOf = (key) => SCHEDULE_FILTERS.find(([k]) => k === key)[2]
  const inFilter = (statuses) => (s) => !statuses || statuses.includes(s.status)
  const rows = (schedules ?? []).filter(inFilter(statusesOf(filter)))
  const current = (schedules ?? []).find((s) => s.id === opened)
  const canBook = BOOKER_ROLES.includes(role) && can('ASSET_VEHICLE', 'CREATE')

  function changed(message) {
    setAnnouncement(message)
    load()
  }

  function moveWeek(days) {
    setOpened(null)
    setWeek(weekOf(addDays(week.monday, days)))
  }

  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết lịch xe"
      renderDetail={(titles) => (
        <ScheduleDetail
          key={opened}
          schedule={current}
          role={role}
          vehicleName={vehicleName(current.vehicleId)}
          driverName={driverName(current.driverStaffId)}
          residentName={residentName(current.residentId)}
          onChanged={changed}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Lịch xe"
        subtitle={`Tuần ${formatDate(week.monday)} → ${formatDate(week.sunday)}`}
        aside={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" className="h-11" onClick={() => moveWeek(-7)}>
              Tuần trước
            </Button>
            <Button variant="outline" className="h-11" onClick={() => moveWeek(7)}>
              Tuần sau
            </Button>
            {canBook && (
              <Button className="h-11" onClick={() => setBooking(true)}>
                Đặt lịch xe
              </Button>
            )}
          </div>
        }
      />
      {booking && (
        <BookScheduleSheet
          vehicles={vehicles}
          staff={staff}
          onClose={() => setBooking(false)}
          onDone={(message) => {
            setBooking(false)
            changed(message)
          }}
        />
      )}
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[filter]}
        onValueChange={(n) => n[0] && setFilter(n[0])}
        aria-label="Lọc lịch xe"
      >
        {SCHEDULE_FILTERS.map(([key, label, statuses]) => (
          <ToggleGroupItem key={key} value={key} className="h-11 px-4">
            {`${label} ${(schedules ?? []).filter(inFilter(statuses)).length}`}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {schedules === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && (
          <Card className="p-6">
            <p className="type-body-sm text-muted-foreground">Không có lịch xe nào trong tuần này ở nhóm đã chọn.</p>
          </Card>
        )
      ) : (
        <Card className="py-0">
          <Table aria-label="Lịch xe">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Thời gian dự kiến</TableHead>
                <TableHead>Xe</TableHead>
                <TableHead>Mục đích</TableHead>
                <TableHead>Người lái</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((s) => (
                <TableRow key={s.id} className={s.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="whitespace-normal pl-4 tabular-nums">
                    <button
                      type="button"
                      className="text-left type-label underline-offset-4 hover:underline"
                      aria-label={`Lịch xe ${formatDateTime(s.plannedDepartureAt)}`}
                      aria-pressed={s.id === opened}
                      onClick={() => setOpened(s.id)}
                    >
                      {formatDateTime(s.plannedDepartureAt)}
                    </button>
                    <span className="block type-body-sm text-muted-foreground">
                      {`về ${formatDateTime(s.plannedReturnAt)}`}
                    </span>
                  </TableCell>
                  <TableCell className="whitespace-normal">{vehicleName(s.vehicleId)}</TableCell>
                  <TableCell className="whitespace-normal">
                    {purposeLabel(s.purpose)}
                    {s.tripId != null && (
                      <span className="block type-body-sm text-muted-foreground">{`Chuyến đi #${s.tripId}`}</span>
                    )}
                    {s.residentId != null && (
                      <span className="block type-body-sm text-muted-foreground">{residentName(s.residentId)}</span>
                    )}
                  </TableCell>
                  <TableCell className="whitespace-normal">{driverName(s.driverStaffId)}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={SCHEDULE_STATUS_LABELS[s.status] ?? s.status} />
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
