import { useCallback, useEffect, useState } from 'react'
import { getReportsByGroup } from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { formatValue, labelNotes } from './reportLabels'

// Bảng một báo cáo (016 FR-022 → FR-073; DESIGN.md `report-table`): dòng tổng rồi dòng chia nhóm; bấm giá trị mở danh
// sách bản ghi của con số hoặc của ô đó (FR-009).

function ratio(v) {
  return v.denominator === null || v.denominator === undefined
    ? ''
    : `${formatValue(v.numerator)} / ${formatValue(v.denominator)}`
}

export function ReportTable({ group, from, to, metrics, onOpen }) {
  const [state, setState] = useState({ status: 'loading' })

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data } = await getReportsByGroup({ path: { group }, query: { from, to } })
      setState({ status: 'ready', report: data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [group, from, to])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi đổi báo cáo hoặc khoảng
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <section aria-busy="true" className="flex flex-col gap-2 rounded-2xl border bg-card p-4 md:p-5">
        <span className="sr-only">Đang tải báo cáo</span>
        <Skeleton className="h-10" />
        <Skeleton className="h-10" />
      </section>
    )
  }
  if (state.status === 'error') {
    return (
      <section className="rounded-2xl border bg-card p-4 md:p-5">
        <ProblemAlert problem={state.problem} title="Không tải được báo cáo">
          <Button variant="outline" size="sm" onClick={() => load()}>
            Thử lại
          </Button>
        </ProblemAlert>
      </section>
    )
  }

  const { report } = state
  const metricOf = (code) => metrics[code] ?? { code, name: code }
  return (
    <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4 md:p-5">
      <p className="type-body-sm text-muted-foreground">
        Tính đến {formatDateTime(report.asOf)}. Bấm giá trị để xem danh sách bản ghi.
      </p>
      {report.notes.map((n) => (
        <p key={n} className="type-body-sm">
          {n}
        </p>
      ))}
      {report.values.length === 0 ? (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Chưa có dữ liệu</EmptyTitle>
            <EmptyDescription>Các module nguồn chưa cung cấp số liệu cho báo cáo này.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Chỉ tiêu</TableHead>
              <TableHead>Nhóm</TableHead>
              <TableHead>Giá trị</TableHead>
              <TableHead>Tử số / mẫu số</TableHead>
              <TableHead>Ghi chú</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {report.values.flatMap((v) => {
              const metric = metricOf(v.metricCode)
              const open = (breakdownKey, value) =>
                onOpen({ metric, from, to, breakdownKey, valueText: formatValue(value) })
              return [
                <TableRow key={v.metricCode}>
                  <TableCell className="type-label">{metric.name}</TableCell>
                  <TableCell>Tổng</TableCell>
                  <TableCell>
                    <Button variant="link" className="h-auto p-0 tabular-nums" onClick={() => open(undefined, v.value)}>
                      {formatValue(v.value)}
                    </Button>
                  </TableCell>
                  <TableCell className="tabular-nums">{ratio(v)}</TableCell>
                  <TableCell className="text-muted-foreground">{labelNotes(v)}</TableCell>
                </TableRow>,
                ...v.breakdown.map((b) => (
                  <TableRow key={`${v.metricCode}|${b.key}`}>
                    <TableCell />
                    <TableCell className="pl-6 text-muted-foreground">{b.key}</TableCell>
                    <TableCell>
                      <Button variant="link" className="h-auto p-0 tabular-nums" onClick={() => open(b.key, b.value)}>
                        {formatValue(b.value)}
                      </Button>
                    </TableCell>
                    <TableCell className="tabular-nums">{ratio(b)}</TableCell>
                    <TableCell />
                  </TableRow>
                )),
              ]
            })}
          </TableBody>
        </Table>
      )}
    </section>
  )
}
