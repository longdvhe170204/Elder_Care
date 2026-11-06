import { useCallback, useState } from 'react'
import {
  getResidentsByResidentIdPickupList,
  getResidentsByResidentIdPickupRecords,
  getResidentsByResidentIdVisitPickupWishes,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { SourceTag } from '@/common/SourceTag'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ResidentPicker } from '../FamilyMembersPage'
import { RELATIONSHIP_LABELS } from '../relationshipLabels'
import { describeWish, wishSource } from '../right-change-requests/requestLabels'
import { ExceptionCommandForm, PickupExceptionSheet } from './PickupExceptions'
import { RecordPickupSheet } from './RecordPickupSheet'
import { PICKUP_BASIS_LABELS, PICKUP_STATUS_LABELS, PICKUP_TYPE_LABELS } from './pickupLabels'

// Quy trình đón (012 T021; US2, FR-021 → FR-028, BR-M10-03; Phụ lục 27 dòng "Quy trình đón, ngoại lệ đón": Trưởng tầng,
// Điều dưỡng, Nhân viên chăm sóc, Hành chính theo quyền lệnh nguồn; Quản lý viện duyệt ngoại lệ). Chọn người cao tuổi,
// xem người được phép đón kèm mong muốn của người cao tuổi về người đón (DESIGN.md 2.2), ghi đón, bản ghi đón. Nút theo
// /me/permissions; quyền lệnh nguồn theo loại đón do backend kiểm.

const FUNCTION = 'PICKUP'

function PickupListCard({ pickupList, wishes }) {
  const pickupWishes = (wishes?.effective ?? []).filter((w) => w.type === 'PICKUP')
  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <section aria-labelledby="pickup-list-title" className="flex flex-col gap-2">
          <h2 id="pickup-list-title" className="type-card-title">
            Người được phép đón
          </h2>
          {pickupList.length === 0 ? (
            <p className="type-body-sm text-muted-foreground">
              Chưa có người thân nào được phép đón. Bật quyền Được phép đón ở màn Người thân và quyền.
            </p>
          ) : (
            <ul aria-label="Người được phép đón" className="flex flex-col">
              {pickupList.map((p) => (
                <li key={p.familyMemberId} className="flex flex-wrap gap-x-2 border-t py-2 first:border-t-0 first:pt-0">
                  <span className="type-label">{p.fullName}</span>
                  <span className="type-body-sm text-muted-foreground">
                    {RELATIONSHIP_LABELS[p.relationship] ?? p.relationship}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
        {wishes && (
          <section aria-labelledby="pickup-wishes-title" className="flex flex-col gap-2">
            <h2 id="pickup-wishes-title" className="type-card-title">
              Mong muốn của người cao tuổi về người đón
            </h2>
            {pickupWishes.length === 0 ? (
              <p className="type-body-sm text-muted-foreground">Chưa ghi nhận mong muốn nào về người đón.</p>
            ) : (
              <ul className="flex flex-col">
                {pickupWishes.map((w) => (
                  <li key={w.id} className="flex flex-col gap-1 border-t py-2 first:border-t-0 first:pt-0">
                    <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
                      <span className="type-label">{describeWish(w, [])}</span>
                      <SourceTag {...wishSource(w, [])} />
                    </span>
                    <span className="type-caption text-muted-foreground tabular-nums">
                      Ghi bởi {w.recordedBy}, {formatDateTime(w.at)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}
      </CardContent>
    </Card>
  )
}

function PickupRecordTable({ records, pickupList }) {
  const pickerOf = (r) => {
    if (r.basis === 'SELF_RETURN') return 'Không có người đón'
    if (r.familyMemberId == null) return 'Người đón theo ngoại lệ'
    return (
      pickupList.find((p) => p.familyMemberId === r.familyMemberId)?.fullName ?? `Người thân mã ${r.familyMemberId}`
    )
  }
  return (
    <Card className="py-0">
      <Table aria-label="Bản ghi đón">
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="pl-4">Thời điểm</TableHead>
            <TableHead>Loại đón</TableHead>
            <TableHead>Người đón</TableHead>
            <TableHead>Căn cứ</TableHead>
            <TableHead>Hết hiệu lực</TableHead>
            <TableHead className="pr-4">Trạng thái</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {records.map((r) => (
            <TableRow key={r.id}>
              <TableCell className="pl-4 tabular-nums">{formatDateTime(r.at)}</TableCell>
              <TableCell>{PICKUP_TYPE_LABELS[r.type] ?? r.type}</TableCell>
              <TableCell>{pickerOf(r)}</TableCell>
              <TableCell className="whitespace-normal">{PICKUP_BASIS_LABELS[r.basis] ?? r.basis}</TableCell>
              <TableCell className="tabular-nums">
                {r.expiresAt ? formatDateTime(r.expiresAt) : 'Không hết hạn'}
              </TableCell>
              <TableCell className="pr-4">
                <StatusBadge status={PICKUP_STATUS_LABELS[r.status] ?? r.status} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  )
}

export function PickupPage() {
  const { can } = usePermissions()
  const [resident, setResident] = useState(null)
  const [pickupList, setPickupList] = useState(null)
  const [records, setRecords] = useState(null)
  const [failure, setFailure] = useState(null)
  const [wishes, setWishes] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  // Mong muốn chỉ hiện khi người xem đọc được (dòng Phụ lục 27 "Mong muốn về khách thăm, người đón"); backend trả 403
  // thì bỏ khối
  const load = useCallback(async (residentId) => {
    getResidentsByResidentIdPickupList({ path: { residentId } })
      .then(({ data }) => setPickupList(data))
      .catch(() => setPickupList([]))
    getResidentsByResidentIdVisitPickupWishes({ path: { residentId } })
      .then(({ data }) => setWishes(data))
      .catch(() => setWishes(null))
    try {
      const { data } = await getResidentsByResidentIdPickupRecords({ path: { residentId } })
      setRecords(data)
      setFailure(null)
    } catch (error) {
      setRecords([])
      setFailure(problemOf(error))
    }
  }, [])

  function pick(next) {
    setResident(next)
    setPickupList(null)
    setRecords(null)
    setWishes(null)
    load(next.id)
  }

  function changed(message) {
    setSheet(null)
    setAnnouncement(message)
    load(resident.id)
  }

  if (failure?.status === 403) {
    return <PermissionDeniedState label="Quy trình đón" />
  }

  const exceptionCommands = [
    ...(can(FUNCTION, 'APPROVE') ? ['approve', 'reject'] : []),
    ...(can(FUNCTION, 'CREATE') ? ['cancel'] : []),
  ]

  return (
    <div className="flex flex-col gap-5">
      <PageHeader
        title={resident ? `Quy trình đón của ${resident.name}` : 'Quy trình đón'}
        subtitle={
          resident
            ? `${resident.code}. Người đón phải thuộc danh sách được phép đón hoặc có ngoại lệ Hiệu lực.`
            : 'Chọn người cao tuổi để ghi đón và xem bản ghi đón.'
        }
      />
      {!resident ? (
        <ResidentPicker onPick={pick} />
      ) : (
        <>
          <div className="flex flex-wrap gap-3">
            {can(FUNCTION, 'CREATE') && (
              <Button className="h-11" onClick={() => setSheet('record')}>
                Ghi đón
              </Button>
            )}
            {can(FUNCTION, 'CREATE') && (
              <Button variant="outline" className="h-11" onClick={() => setSheet('exception')}>
                Lập yêu cầu ngoại lệ
              </Button>
            )}
            <Button variant="ghost" className="h-11" onClick={() => setResident(null)}>
              Chọn người cao tuổi khác
            </Button>
          </div>
          <p role="status" className="type-body-sm empty:hidden">
            {announcement}
          </p>

          {pickupList === null ? (
            <Skeleton className="h-24 w-full" />
          ) : (
            <PickupListCard pickupList={pickupList} wishes={wishes} />
          )}

          <section aria-labelledby="pickup-records-title" className="flex flex-col gap-3">
            <h2 id="pickup-records-title" className="type-card-title">
              Bản ghi đón
            </h2>
            <ProblemAlert problem={failure} />
            {records === null ? (
              <Skeleton className="h-24 w-full" />
            ) : records.length === 0 ? (
              !failure && <p className="type-body-sm text-muted-foreground">Chưa có bản ghi đón nào.</p>
            ) : (
              <PickupRecordTable records={records} pickupList={pickupList ?? []} />
            )}
          </section>

          {exceptionCommands.length > 0 && (
            <ExceptionCommandForm commands={exceptionCommands} onDone={(message) => changed(message)} />
          )}

          {sheet === 'record' && (
            <RecordPickupSheet
              residentId={resident.id}
              residentName={resident.name}
              pickupList={pickupList ?? []}
              onClose={() => setSheet(null)}
              onDone={changed}
            />
          )}
          {sheet === 'exception' && (
            <PickupExceptionSheet
              residentId={resident.id}
              residentName={resident.name}
              onClose={() => setSheet(null)}
              onDone={changed}
            />
          )}
        </>
      )}
    </div>
  )
}
