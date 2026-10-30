import { useEffect, useState } from 'react'
import { getDutyBasisAuditLogs } from '@/api'
import { Button } from '@/components/ui/button'
import { FieldGroup, Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { localInput } from './shiftLabels'

const BASES = {
  SHIFT_LEAD: 'Nhiệm vụ Người phụ trách ca',
  FLOOR_MANAGER_SUBSTITUTE: 'Thay Trưởng tầng khi chưa có Trưởng tầng',
  TEMPORARY_RECEIVER: 'Tạm nhận khi bàn giao chưa xác nhận',
  ROLE_INVITATION_SENT: 'Vai trò với lời mời đã gửi',
}
const TARGETS = {
  'M09.STAFF_ABSENCE': 'Ghi nhận vắng ca',
  'M09.ASSIGNMENT': 'Phân công',
  'M09.SHIFT': 'Ca',
  'M09.HANDOVER': 'Bàn giao',
}

export function DutyBasisPanel({ shift, scopeId, staff = [] }) {
  const [from, setFrom] = useState(() => localInput(shift.startsAt))
  const [to, setTo] = useState(() => localInput(shift.endsAt))
  const [query, setQuery] = useState(() => ({
    from: localInput(shift.startsAt),
    to: localInput(shift.endsAt),
    attempt: 0,
  }))
  const [result, setResult] = useState(null)
  const key = JSON.stringify([query, scopeId])
  useEffect(() => {
    let active = true
    getDutyBasisAuditLogs({
      query: {
        from: `${query.from}:00+07:00`,
        to: `${query.to}:00+07:00`,
        ...(scopeId ? { floorAreaId: scopeId } : {}),
      },
    })
      .then(({ data }) => {
        if (active) setResult({ key, data })
      })
      .catch((error) => {
        if (active) setResult({ key, failure: problemOf(error) })
      })
    return () => {
      active = false
    }
  }, [query, scopeId, key])
  const current = result?.key === key ? result : null
  return (
    <section className="flex flex-col gap-4" aria-label="Lịch sử căn cứ nhiệm vụ">
      <p className="type-body-sm text-muted-foreground">
        Các lần dùng nhiệm vụ hoặc quyền thay thế trong phạm vi đang xem.
      </p>
      <form
        className="flex flex-col gap-3"
        onSubmit={(e) => {
          e.preventDefault()
          if (from < to) setQuery({ from, to, attempt: query.attempt + 1 })
        }}
      >
        <FieldGroup className="grid gap-3 sm:grid-cols-2">
          <Field>
            <FieldLabel htmlFor="basis-from">Từ thời điểm</FieldLabel>
            <Input
              id="basis-from"
              type="datetime-local"
              required
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </Field>
          <Field>
            <FieldLabel htmlFor="basis-to">Đến thời điểm</FieldLabel>
            <Input
              id="basis-to"
              type="datetime-local"
              required
              value={to}
              min={from}
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </Field>
        </FieldGroup>
        {from >= to && <p role="alert">Thời điểm kết thúc phải sau thời điểm bắt đầu.</p>}
        <Button variant="outline" type="submit" disabled={!from || !to || from >= to}>
          Tra cứu căn cứ
        </Button>
      </form>
      {!current && <Skeleton className="h-24 w-full" aria-label="Đang tải căn cứ nhiệm vụ" />}
      <ProblemAlert problem={current?.failure}>
        <Button variant="outline" type="button" onClick={() => setQuery({ ...query, attempt: query.attempt + 1 })}>
          Thử lại
        </Button>
      </ProblemAlert>
      {current?.data?.length === 0 && (
        <p className="type-body-sm">Chưa có lần dùng căn cứ nhiệm vụ trong khoảng đã chọn.</p>
      )}
      {current?.data?.length > 0 && (
        <div className="min-w-0 overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Thời điểm</TableHead>
                <TableHead>Nhân viên</TableHead>
                <TableHead>Căn cứ</TableHead>
                <TableHead>Bản ghi</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {current.data.map((row) => (
                <TableRow key={row.auditId}>
                  <TableCell>{formatDateTime(row.at)}</TableCell>
                  <TableCell>
                    {staff.find((p) => p.id === row.staffId)?.fullName ?? `Nhân viên #${row.staffId}`}
                  </TableCell>
                  <TableCell>{BASES[row.basis] ?? 'Căn cứ nhiệm vụ khác'}</TableCell>
                  <TableCell>
                    {TARGETS[row.targetType] ?? 'Bản ghi'} #{row.targetId}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  )
}
