import { useEffect, useState } from 'react'
import { getShiftsByIdStaffingRatios } from '@/api'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'

const EVENTS = {
  DRAFT_SAVED: 'Lưu ca nháp',
  PUBLISHED: 'Công bố lịch',
  STAFF_ABSENT: 'Ghi nhận vắng ca',
  STAFF_ADDED: 'Bổ sung nhân viên',
  EMPLOYMENT_ENDED: 'Nhân viên nghỉ việc',
  SHIFT_SWAP_015: 'Đổi ca / nghỉ có duyệt',
  CARE_LEVEL_CHANGED: 'Đổi mức chăm sóc',
  ALLOCATION_CHANGED: 'Thay đổi phân bổ giường',
  RESIDENT_ABSENCE: 'Người cao tuổi vắng / trở về',
  FINAL_STATUS: 'Kết thúc hồ sơ người cao tuổi',
}
const number = (value) => new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(value)

export function StaffingRatioPanel({ shiftId }) {
  const [result, setResult] = useState(null)
  const [attempt, setAttempt] = useState(0)
  const current = result?.shiftId === shiftId && result?.attempt === attempt ? result : null
  const rows = current?.rows ?? null
  const failure = current?.failure
  const latest = rows?.at(-1)
  useEffect(() => {
    let active = true
    getShiftsByIdStaffingRatios({ path: { id: shiftId } })
      .then(({ data }) => {
        if (active) setResult({ shiftId, attempt, rows: data })
      })
      .catch((error) => {
        if (active) setResult({ shiftId, attempt, failure: problemOf(error) })
      })
    return () => {
      active = false
    }
  }, [shiftId, attempt])
  return (
    <section className="flex min-w-0 flex-col gap-3" aria-label="Tỷ lệ phục vụ">
      <h3 className="type-section">Tỷ lệ phục vụ</h3>
      <p className="type-caption text-muted-foreground">
        Tổng trọng số chăm sóc chia cho số Điều dưỡng và Nhân viên chăm sóc được tính trong ca. Tỷ lệ lớn hơn ngưỡng là
        thiếu nhân lực.
      </p>
      {latest && (
        <div className="flex flex-col gap-1 rounded-xl border bg-muted p-3">
          <p className={latest.meetsThreshold ? 'font-medium' : 'font-medium text-warning-foreground'}>
            {latest.meetsThreshold ? 'Lần tính gần nhất: Đạt ngưỡng' : 'Lần tính gần nhất: Dưới ngưỡng'}
          </p>
          <p>
            {number(latest.totalWeight)} trọng số · {latest.staffCount} nhân viên · ngưỡng {number(latest.threshold)}
          </p>
          <p className="type-caption text-muted-foreground">
            {latest.ratio == null ? 'Không có nhân viên được tính' : `Tỷ lệ ${number(latest.ratio)}`} ·{' '}
            {formatDateTime(latest.at)}
          </p>
        </div>
      )}
      {failure ? (
        <>
          <ProblemAlert problem={failure} />
          <Button variant="outline" onClick={() => setAttempt((v) => v + 1)}>
            Thử lại
          </Button>
        </>
      ) : rows === null ? (
        <p role="status">Đang tải lịch sử tính…</p>
      ) : rows.length === 0 ? (
        <p className="text-muted-foreground">Chưa có lần tính tỷ lệ phục vụ.</p>
      ) : (
        <div className="min-w-0 overflow-x-auto rounded-xl border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Thời điểm / thay đổi</TableHead>
                <TableHead>Trọng số</TableHead>
                <TableHead>Nhân viên</TableHead>
                <TableHead>Tỷ lệ / ngưỡng</TableHead>
                <TableHead>Kết quả</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {[...rows].reverse().map((row, index) => (
                <TableRow key={`${row.at}:${index}`}>
                  <TableCell>
                    <p className="whitespace-nowrap">{formatDateTime(row.at)}</p>
                    <p className="type-caption text-muted-foreground">{EVENTS[row.event] ?? 'Tính lại tỷ lệ'}</p>
                  </TableCell>
                  <TableCell>{number(row.totalWeight)}</TableCell>
                  <TableCell>{row.staffCount}</TableCell>
                  <TableCell>
                    {row.ratio == null ? <span>Chưa có nhân viên được tính</span> : <span>{number(row.ratio)}</span>}
                    <span className="whitespace-nowrap text-muted-foreground"> / {number(row.threshold)}</span>
                  </TableCell>
                  <TableCell>
                    <span
                      className={row.meetsThreshold ? 'text-muted-foreground' : 'font-medium text-warning-foreground'}
                    >
                      {row.meetsThreshold ? 'Đạt ngưỡng' : 'Dưới ngưỡng phục vụ'}
                    </span>
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
