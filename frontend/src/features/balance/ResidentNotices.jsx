import { useCallback, useEffect, useState } from 'react'
import { getPaymentNoticesByIdFile, getResidentsByResidentIdPaymentNotices } from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { STATEMENT_TYPE_LABELS, formatMoney, labelOf } from './balanceLabels'
import { saveBlob } from './download'

// Phiếu báo phí đã phát hành của một người cao tuổi (017 T036; FR-031, FR-037, BR-M11-23): tải lại đúng file PDF hoặc
// Excel đã lưu; mỗi lần tải được ghi nhật ký ở máy chủ.

export function ResidentNotices({ residentId }) {
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [failure, setFailure] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByResidentIdPaymentNotices({ path: { residentId } })
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải phiếu khi mở người cao tuổi
    load()
  }, [load])

  async function download(notice, format) {
    setFailure(null)
    try {
      const { data } = await getPaymentNoticesByIdFile({
        path: { id: notice.id },
        query: { format },
        responseType: 'blob',
      })
      saveBlob(data, `${notice.noticeCode}.${format === 'PDF' ? 'pdf' : 'xlsx'}`)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  if (state.status === 'loading') return <Skeleton className="h-24 w-full" />
  if (state.status === 'error') return <ProblemAlert problem={state.problem} />
  if (state.items.length === 0)
    return (
      <Empty>
        <EmptyHeader>
          <EmptyTitle>Chưa có phiếu báo phí</EmptyTitle>
          <EmptyDescription>Phiếu hiện ở đây sau khi Kế toán phát hành cho một bảng chi phí đã chốt.</EmptyDescription>
        </EmptyHeader>
      </Empty>
    )
  return (
    <div className="flex flex-col gap-3">
      <ProblemAlert problem={failure} />
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Mã phiếu</TableHead>
            <TableHead>Kỳ</TableHead>
            <TableHead>Loại bảng</TableHead>
            <TableHead className="text-right">Tổng</TableHead>
            <TableHead className="text-right">Số dư sau</TableHead>
            <TableHead>Phát hành</TableHead>
            <TableHead>
              <span className="sr-only">Tải</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {state.items.map((notice) => (
            <TableRow key={notice.id}>
              <TableCell className="tabular-nums">{notice.noticeCode}</TableCell>
              <TableCell className="tabular-nums">{notice.period}</TableCell>
              <TableCell>{labelOf(STATEMENT_TYPE_LABELS, notice.statementType)}</TableCell>
              <TableCell className="text-right tabular-nums">{formatMoney(notice.total)}</TableCell>
              <TableCell className="text-right tabular-nums">{formatMoney(notice.balanceAfter)}</TableCell>
              <TableCell className="tabular-nums">
                {formatDate(notice.issuedAt)}
                <span className="type-caption block text-muted-foreground">{notice.issuedBy}</span>
              </TableCell>
              <TableCell className="flex gap-2">
                <Button
                  variant="outline"
                  className="h-11"
                  aria-label={`Tải PDF phiếu ${notice.noticeCode}`}
                  onClick={() => download(notice, 'PDF')}
                >
                  PDF
                </Button>
                <Button
                  variant="outline"
                  className="h-11"
                  aria-label={`Tải Excel phiếu ${notice.noticeCode}`}
                  onClick={() => download(notice, 'XLSX')}
                >
                  Excel
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
