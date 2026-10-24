import { useCallback, useEffect, useRef, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import { ApiError, getDashboard, getMetrics } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { MetricDetailSheet } from '@/features/reports/MetricDetailSheet'
import { METRIC_GROUP_LABELS, formatValue, labelNotes } from '@/features/reports/reportLabels'

// Tổng quan là dashboard của 016 (T010; FR-020, FR-021; bố cục duyệt 2026-10-24, DESIGN.md 5.21.0 `dashboard-block`).
// Khối theo nhóm chỉ tiêu backend trả cho vai trò và phạm vi hiện hành; làm mới mỗi 30 giây với ETag (research O-06).
// Bấm một số mở danh sách bản ghi tạo nên số đó.

export const REFRESH_MS = 30_000

const FACILITY_WIDE = ['NURSING_HOME_MANAGER', 'DOCTOR', 'OFFICE_STAFF', 'ACCOUNTANT']

const TIME = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Ho_Chi_Minh',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
})

function MetricButton({ value, metric, onOpen }) {
  const overdue = value.metricCode.endsWith('.OVERDUE') && value.value > 0
  const breakdown = value.breakdown ?? []
  // "không có dữ liệu" đã ghi riêng dưới số
  const notes = labelNotes({ ...value, labels: (value.labels ?? []).filter((l) => l !== 'NO_DATA') })
  return (
    <button
      type="button"
      onClick={() => onOpen({ metric, valueText: formatValue(value.value) })}
      className="flex flex-col items-start gap-1 rounded-xl p-2 text-left hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
    >
      <span className={`type-metric tabular-nums ${overdue ? 'text-destructive' : ''}`}>{formatValue(value.value)}</span>
      <span className="type-caption text-muted-foreground text-balance">{metric.name}</span>
      {value.value === null && <span className="type-caption text-muted-foreground">không có dữ liệu</span>}
      {breakdown.length > 0 && (
        <span className="type-body-sm">{breakdown.map((b) => `${b.key} ${formatValue(b.value)}`).join(', ')}</span>
      )}
      {notes && <span className="type-caption text-muted-foreground">{notes}</span>}
    </button>
  )
}

function DashboardBlock({ block, metrics, onOpen }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="type-card-title">{METRIC_GROUP_LABELS[block.group] ?? block.group}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        {block.values.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">
            Chưa có dữ liệu: module nguồn chưa cung cấp số liệu cho nhóm này.
          </p>
        ) : (
          block.values.map((v) => (
            <MetricButton
              key={v.metricCode}
              value={v}
              metric={metrics[v.metricCode] ?? { code: v.metricCode, name: v.metricCode }}
              onOpen={onOpen}
            />
          ))
        )}
        {block.status === 'UPDATING' && <p className="type-caption text-muted-foreground">Đang cập nhật</p>}
      </CardContent>
    </Card>
  )
}

export function DashboardPage({ role }) {
  const [state, setState] = useState({ status: 'loading' })
  const [metrics, setMetrics] = useState({})
  const [opened, setOpened] = useState(null)
  const etag = useRef(null)

  const load = useCallback(async () => {
    try {
      const { data, etag: next } = await getDashboard({
        headers: etag.current ? { 'If-None-Match': etag.current } : {},
      })
      etag.current = next
      setState({ status: 'ready', dashboard: data })
    } catch (error) {
      // 304: số liệu không đổi, giữ khối đang hiện
      if (error instanceof ApiError && error.status === 304) return
      setState((s) => (s.status === 'ready' ? s : { status: 'error', problem: problemOf(error) }))
    }
  }, [])

  useEffect(() => {
    // tải khi mở màn, rồi làm mới định kỳ (FR-021)
    load()
    getMetrics()
      .then(({ data }) => setMetrics(Object.fromEntries(data.map((m) => [m.code, m]))))
      .catch(() => {})
    const timer = setInterval(load, REFRESH_MS)
    return () => clearInterval(timer)
  }, [load])

  const facilityWide = FACILITY_WIDE.includes(role)
  const dashboard = state.dashboard
  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader
        title={facilityWide ? 'Tổng quan toàn viện hôm nay' : 'Tổng quan trong phạm vi của bạn hôm nay'}
        subtitle={
          dashboard
            ? `Cập nhật lúc ${TIME.format(new Date(dashboard.asOf))}, tự làm mới mỗi 30 giây.`
            : 'Tự làm mới mỗi 30 giây.'
        }
        aside={
          <Button variant="outline" onClick={() => load()}>
            <RefreshCw aria-hidden="true" />
            Làm mới
          </Button>
        }
      />
      {state.status === 'loading' && (
        <div aria-busy="true" className="grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(240px,1fr))]">
          <span className="sr-only">Đang tải tổng quan</span>
          <Skeleton className="h-36 rounded-2xl" />
          <Skeleton className="h-36 rounded-2xl" />
          <Skeleton className="h-36 rounded-2xl" />
        </div>
      )}
      {state.status === 'error' && (
        <section className="rounded-2xl border bg-card p-4 md:p-5">
          <ProblemAlert problem={state.problem} title="Không tải được tổng quan">
            <Button variant="outline" size="sm" onClick={() => load()}>
              Thử lại
            </Button>
          </ProblemAlert>
        </section>
      )}
      {state.status === 'ready' && dashboard.blocks.length === 0 && (
        <section className="rounded-2xl border bg-card p-4 md:p-5">
          <Empty>
            <EmptyHeader>
              <EmptyTitle>{dashboard.message ?? 'Hiện không có phạm vi phân công'}</EmptyTitle>
              <EmptyDescription>Số liệu hiện khi bạn có ca, tầng hoặc người cao tuổi được phân công.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        </section>
      )}
      {state.status === 'ready' && dashboard.blocks.length > 0 && (
        <div className="grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(240px,1fr))]">
          {dashboard.blocks.map((b) => (
            <DashboardBlock key={b.group} block={b} metrics={metrics} onOpen={setOpened} />
          ))}
        </div>
      )}
      <MetricDetailSheet opened={opened} onClose={() => setOpened(null)} />
    </div>
  )
}
