import { useEffect, useState } from 'react'
import { getMetrics } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { ExportDialog } from './ExportDialog'
import { ExportList } from './ExportList'
import { MetricDetailSheet } from './MetricDetailSheet'
import { ReportTable } from './ReportTable'
import { REPORT_GROUPS, isoDate } from './reportLabels'

// Mục menu "Báo cáo" (016; bố cục duyệt 2026-10-24, DESIGN.md 5.21.0): tab theo nhóm báo cáo vai trò thấy (bảng FR-010)
// và tab "Lần xuất"; chọn khoảng (FR-060, tối đa CFG-M14-01, backend từ chối khi dài hơn); nút chính "Xuất file".
// Lọc theo tầng chờ danh mục tầng của 003.

const EXPORTS = 'EXPORTS'

function rangeOf(preset, custom) {
  const today = new Date()
  switch (preset) {
    case 'TODAY':
      return { from: isoDate(today), to: isoDate(today) }
    case 'WEEK':
      return { from: isoDate(new Date(today.getTime() - 6 * 86_400_000)), to: isoDate(today) }
    case 'MONTH':
      return { from: `${isoDate(today).slice(0, 8)}01`, to: isoDate(today) }
    default:
      return custom
  }
}

export function ReportsPage({ role }) {
  const groups = REPORT_GROUPS.filter((g) => g.roles.includes(role))
  const [tab, setTab] = useState(groups[0]?.value ?? EXPORTS)
  const [preset, setPreset] = useState('MONTH')
  const [custom, setCustom] = useState(() => rangeOf('MONTH'))
  const [metrics, setMetrics] = useState({})
  const [opened, setOpened] = useState(null)
  const [exporting, setExporting] = useState(false)
  const [exports, setExports] = useState(0)

  useEffect(() => {
    getMetrics()
      .then(({ data }) => setMetrics(Object.fromEntries(data.map((m) => [m.code, m]))))
      .catch(() => {})
  }, [])

  const current = groups.find((g) => g.value === tab)
  const range = rangeOf(preset, custom)
  const validRange = range.from && range.to && range.from <= range.to

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-6">
      <PageHeader
        title={current ? `Báo cáo ${current.label.toLowerCase()}` : 'Lần xuất báo cáo'}
        subtitle={
          current
            ? 'Số liệu trong phạm vi của bạn theo khoảng đã chọn.'
            : 'Các lần xuất file; Quản lý viện thấy mọi lần, người khác thấy lần của mình.'
        }
        aside={
          current && (
            <Button disabled={!validRange} onClick={() => setExporting(true)}>
              Xuất file
            </Button>
          )
        }
      >
        <ToggleGroup
          variant="outline"
          value={[tab]}
          onValueChange={(next) => next[0] && setTab(next[0])}
          className="flex-wrap self-start"
        >
          {groups.map((g) => (
            <ToggleGroupItem key={g.value} value={g.value} className="h-10 px-4">
              {g.label}
            </ToggleGroupItem>
          ))}
          <ToggleGroupItem value={EXPORTS} className="h-10 px-4">
            Lần xuất
          </ToggleGroupItem>
        </ToggleGroup>
        {current && (
          <div className="flex flex-wrap items-end gap-3">
            <ToggleGroup
              variant="outline"
              aria-label="Khoảng thời gian"
              value={[preset]}
              onValueChange={(next) => next[0] && setPreset(next[0])}
              className="flex-wrap"
            >
              <ToggleGroupItem value="TODAY" className="h-10 px-4">
                Hôm nay
              </ToggleGroupItem>
              <ToggleGroupItem value="WEEK" className="h-10 px-4">
                7 ngày qua
              </ToggleGroupItem>
              <ToggleGroupItem value="MONTH" className="h-10 px-4">
                Tháng này
              </ToggleGroupItem>
              <ToggleGroupItem value="CUSTOM" className="h-10 px-4">
                Tùy chọn
              </ToggleGroupItem>
            </ToggleGroup>
            {preset === 'CUSTOM' && (
              <>
                <Field className="w-auto">
                  <FieldLabel htmlFor="report-from">Từ ngày</FieldLabel>
                  <Input
                    id="report-from"
                    type="date"
                    value={custom.from}
                    onChange={(e) => setCustom({ ...custom, from: e.target.value })}
                  />
                </Field>
                <Field className="w-auto">
                  <FieldLabel htmlFor="report-to">Đến ngày</FieldLabel>
                  <Input
                    id="report-to"
                    type="date"
                    value={custom.to}
                    onChange={(e) => setCustom({ ...custom, to: e.target.value })}
                  />
                </Field>
              </>
            )}
          </div>
        )}
      </PageHeader>
      {current && validRange && (
        <ReportTable
          key={`${current.value}|${range.from}|${range.to}`}
          group={current.value}
          from={range.from}
          to={range.to}
          metrics={metrics}
          onOpen={setOpened}
        />
      )}
      {current && !validRange && (
        <section className="rounded-2xl border bg-card p-4 md:p-5">
          <p className="type-body">Chọn ngày bắt đầu không sau ngày kết thúc.</p>
        </section>
      )}
      {!current && <ExportList version={exports} />}
      {current && (
        <ExportDialog
          open={exporting}
          onOpenChange={setExporting}
          group={current.value}
          groupLabel={current.label}
          from={range.from}
          to={range.to}
          canIdentify={role === 'NURSING_HOME_MANAGER'}
          onExported={() => setExports((n) => n + 1)}
        />
      )}
      <MetricDetailSheet opened={opened} onClose={() => setOpened(null)} />
    </div>
  )
}
