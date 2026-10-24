import { useCallback, useEffect, useState } from 'react'
import { getReportsExports } from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { openFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { ROLES } from '@/common/layout/navigation'
import { REPORT_GROUP_LABELS } from './reportLabels'

// Lần xuất (016 FR-062): Quản lý viện thấy mọi lần, người khác lần của mình (backend lọc). Tải lại file đã xuất bằng
// mã tệp; file không đổi khi dữ liệu nguồn đổi.

const ROLE_LABELS = Object.fromEntries(ROLES.map((r) => [r.code, r.label]))
const ROLE_BY_CODE = {
  'AC-01': 'NURSING_HOME_MANAGER',
  'AC-02': 'FLOOR_MANAGER',
  'AC-03': 'DOCTOR',
  'AC-04': 'NURSING_STAFF',
  'AC-09': 'OFFICE_STAFF',
  'AC-14': 'ACCOUNTANT',
}

export function ExportList({ version }) {
  const [state, setState] = useState({ status: 'loading' })
  const [failure, setFailure] = useState(null)

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data } = await getReportsExports()
      setState({ status: 'ready', rows: data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở tab hoặc sau một lần xuất
    load()
  }, [load, version])

  async function download(fileId) {
    setFailure(null)
    try {
      await openFile(fileId)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  return (
    <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải lần xuất</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && <ProblemAlert problem={state.problem} title="Không tải được lần xuất" />}
      <ProblemAlert problem={failure} title="Không tải được file" />
      {state.status === 'ready' && state.rows.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Chưa có lần xuất</EmptyTitle>
            <EmptyDescription>Xuất một báo cáo bằng nút Xuất file ở tab báo cáo.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {state.status === 'ready' && state.rows.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Thời điểm</TableHead>
              <TableHead>Báo cáo</TableHead>
              <TableHead>Khoảng</TableHead>
              <TableHead>Vai trò người xuất</TableHead>
              <TableHead>Định danh</TableHead>
              <TableHead>Mục đích</TableHead>
              <TableHead>Số dòng</TableHead>
              <TableHead>
                <span className="sr-only">Tải</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.rows.map((r) => (
              <TableRow key={r.auditLogId}>
                <TableCell className="tabular-nums">{formatDateTime(r.exportedAt)}</TableCell>
                <TableCell>{REPORT_GROUP_LABELS[r.group] ?? r.group}</TableCell>
                <TableCell className="tabular-nums">
                  {formatDate(r.from)} – {formatDate(r.to)}
                </TableCell>
                <TableCell>{ROLE_LABELS[ROLE_BY_CODE[r.role]] ?? r.role}</TableCell>
                <TableCell>{r.includeIdentity ? 'Có' : 'Không'}</TableCell>
                <TableCell>{r.purpose ?? '—'}</TableCell>
                <TableCell className="tabular-nums">{r.rowCount ?? '—'}</TableCell>
                <TableCell>
                  <Button variant="outline" size="sm" onClick={() => download(r.fileId)}>
                    Tải file
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </section>
  )
}
