import { useCallback, useState } from 'react'
import { getResidentsByResidentIdRelationships, getVisitSlots, getVisits, postVisitsByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ResidentPicker } from '../FamilyMembersPage'
import { CancelVisitSheet, CheckInSheet, RegisterVisitSheet } from './VisitSheets'
import { VISIT_STATUS_LABELS, slotLabel, todayIso } from './visitLabels'

// Đăng ký thăm (012 T025; US4, FR-029 → FR-039; Phụ lục 27 dòng "Đăng ký thăm": Hành chính đăng ký tại quầy, ghi vào,
// ra; Quản lý viện, Trưởng tầng xem). Chọn người cao tuổi và ngày; khung trong ngày kèm số chỗ còn để tự chọn khung khác
// (Q-307); bảng lượt thăm. Nút theo /me/permissions; backend kiểm lại ở lệnh.

const FUNCTION = 'VISIT_REGISTRATION'

export function VisitsPage() {
  const { can } = usePermissions()
  const [resident, setResident] = useState(null)
  const [date, setDate] = useState(todayIso)
  const [slots, setSlots] = useState(null)
  const [visits, setVisits] = useState(null)
  const [relationships, setRelationships] = useState([])
  const [failure, setFailure] = useState(null)
  const [actionFailure, setActionFailure] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(async (residentId, day) => {
    getVisitSlots({ query: { date: day } })
      .then(({ data }) => setSlots(data))
      .catch(() => setSlots([]))
    // Tên người thân lấy từ quan hệ khi người xem đọc được (Hành chính, Quản lý viện)
    getResidentsByResidentIdRelationships({ path: { residentId } })
      .then(({ data }) => setRelationships(data))
      .catch(() => setRelationships([]))
    try {
      const { data } = await getVisits({ query: { residentId, date: day } })
      setVisits(data)
      setFailure(null)
    } catch (error) {
      setVisits([])
      setFailure(problemOf(error))
    }
  }, [])

  function pick(next) {
    setResident(next)
    setSlots(null)
    setVisits(null)
    load(next.id, date)
  }

  function changeDate(next) {
    setDate(next)
    if (resident && next) load(resident.id, next)
  }

  function changed(message) {
    setSheet(null)
    setActionFailure(null)
    setAnnouncement(message)
    load(resident.id, date)
  }

  async function checkOut(visit) {
    try {
      await postVisitsByIdByCommand({
        path: { id: visit.id, command: 'check-out' },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: {},
      })
      changed('Đã ghi ra')
    } catch (error) {
      setActionFailure(problemOf(error))
    }
  }

  if (failure?.status === 403) {
    return <PermissionDeniedState label="Đăng ký thăm" />
  }

  const nameOf = (familyMemberId) =>
    relationships.find((r) => r.familyMember.id === familyMemberId)?.familyMember.fullName ??
    `Người thân mã ${familyMemberId}`
  const canUpdate = can(FUNCTION, 'UPDATE')

  return (
    <div className="flex flex-col gap-5">
      <PageHeader
        title={resident ? `Đăng ký thăm ${resident.name}` : 'Đăng ký thăm'}
        subtitle={
          resident
            ? `${resident.code}. Lượt đạt điều kiện được duyệt ngay và giữ chỗ trong khung.`
            : 'Chọn người cao tuổi để xem và ghi lượt thăm.'
        }
      />
      {!resident ? (
        <ResidentPicker onPick={pick} />
      ) : (
        <>
          <div className="flex flex-wrap items-end gap-3">
            <Field className="w-auto">
              <FieldLabel htmlFor="visit-date">Ngày thăm</FieldLabel>
              <Input
                id="visit-date"
                type="date"
                className="h-11"
                value={date}
                onChange={(e) => changeDate(e.target.value)}
              />
            </Field>
            {can(FUNCTION, 'CREATE') && (
              <Button className="h-11" onClick={() => setSheet({ kind: 'register' })}>
                Đăng ký tại quầy
              </Button>
            )}
            <Button variant="ghost" className="h-11" onClick={() => setResident(null)}>
              Chọn người cao tuổi khác
            </Button>
          </div>
          <p role="status" className="type-body-sm empty:hidden">
            {announcement}
          </p>

          <Card>
            <CardContent className="flex flex-col gap-2">
              <h2 className="type-card-title">Khung thăm trong ngày</h2>
              {slots === null ? (
                <Skeleton className="h-11 w-full" />
              ) : (
                <ul
                  aria-label="Khung thăm trong ngày"
                  className="flex flex-wrap gap-x-6 gap-y-1 type-body-sm tabular-nums"
                >
                  {slots.map((s) => (
                    <li key={slotLabel(s)}>
                      {slotLabel(s)}, còn {s.seatsLeft} chỗ
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          <section aria-labelledby="visits-title" className="flex flex-col gap-3">
            <h2 id="visits-title" className="type-card-title">
              Lượt thăm
            </h2>
            <ProblemAlert problem={failure ?? actionFailure} />
            {visits === null ? (
              <Skeleton className="h-24 w-full" />
            ) : visits.length === 0 ? (
              !failure && <p className="type-body-sm text-muted-foreground">Chưa có lượt thăm nào trong ngày.</p>
            ) : (
              <Card className="py-0">
                <Table aria-label="Lượt thăm">
                  <TableHeader>
                    <TableRow className="hover:bg-transparent">
                      <TableHead className="pl-4">Khung giờ</TableHead>
                      <TableHead>Người đăng ký</TableHead>
                      <TableHead>Số người</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead className="pr-4">Lệnh</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {visits.map((v) => {
                      const who = `lượt ${slotLabel(v)} của ${nameOf(v.familyMemberId)}`
                      return (
                        <TableRow key={v.id}>
                          <TableCell className="pl-4 tabular-nums">{slotLabel(v)}</TableCell>
                          <TableCell>{nameOf(v.familyMemberId)}</TableCell>
                          <TableCell className="tabular-nums">{v.visitorCount}</TableCell>
                          <TableCell className="whitespace-normal">
                            <span className="flex flex-col">
                              <StatusBadge status={VISIT_STATUS_LABELS[v.status] ?? v.status} />
                              {v.cancellationReason && (
                                <span className="type-caption text-muted-foreground">{v.cancellationReason}</span>
                              )}
                            </span>
                          </TableCell>
                          <TableCell className="pr-4">
                            {canUpdate && v.status === 'APPROVED' && (
                              <span className="flex flex-wrap gap-2">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  aria-label={`Ghi vào ${who}`}
                                  onClick={() => setSheet({ kind: 'check-in', visit: v })}
                                >
                                  Ghi vào
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  aria-label={`Hủy ${who}`}
                                  onClick={() => setSheet({ kind: 'cancel', visit: v })}
                                >
                                  Hủy
                                </Button>
                              </span>
                            )}
                            {canUpdate && v.status === 'CHECKED_IN' && (
                              <Button
                                variant="outline"
                                size="sm"
                                aria-label={`Ghi ra ${who}`}
                                onClick={() => checkOut(v)}
                              >
                                Ghi ra
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </Card>
            )}
          </section>

          {sheet?.kind === 'register' && (
            <RegisterVisitSheet
              resident={resident}
              date={date}
              slots={slots ?? []}
              relationships={relationships}
              onClose={() => setSheet(null)}
              onDone={changed}
            />
          )}
          {sheet?.kind === 'check-in' && (
            <CheckInSheet visit={sheet.visit} onClose={() => setSheet(null)} onDone={changed} />
          )}
          {sheet?.kind === 'cancel' && (
            <CancelVisitSheet visit={sheet.visit} onClose={() => setSheet(null)} onDone={changed} />
          )}
        </>
      )}
    </div>
  )
}
