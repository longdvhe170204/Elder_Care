import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { AllergyNote } from '@/common/CareNotes'
import { ClockAside, PageHeader } from '@/common/PageHeader'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_DOSES, DEMO_RESIDENTS, DEMO_SHIFT } from '@/common/demoData'
import { cn } from '@/lib/utils'
import { DoseSheet } from './DoseSheet'
import { DoseTags } from './DoseTags'
import { windowOf } from './medicationTime'
import { PrescriptionPanel } from './PrescriptionPanel'
import { PrnPanel } from './PrnPanel'

// Bản mẫu màn Phát thuốc (M14; spec 006 User Story 2, 6), theo DESIGN.md 5.1: lưới thẻ theo giờ dự kiến, mỗi thẻ
// là một người cao tuổi trong giờ đó. Chỉ Điều dưỡng xác nhận liều (FR-026); Trưởng tầng, Bác sĩ, Quản lý viện
// xem. Trạng thái liều do hệ thống tính theo giờ (FR-021); ở đây cố định theo giờ hiện tại của bản mẫu.

const RECORD_ROLE = 'NURSING_STAFF'
const OPEN = ['Đến giờ', 'Trễ']
const FILTERS = ['Tất cả', 'Trễ', 'Đến giờ', 'Chưa đến giờ', 'Đã xong']

const residentOf = (code) => DEMO_RESIDENTS.find((r) => r.code === code)

// Trạng thái của thẻ theo liều gấp nhất trong thẻ
function groupStatus(doses) {
  for (const s of ['Trễ', 'Đến giờ', 'Chưa đến giờ']) if (doses.some((d) => d.status === s)) return s
  // Tạm dừng (người cao tuổi vắng, đơn tạm dừng) không phải đã xong: giữ ở giờ của nó, không có nút (BR-M07-03)
  if (doses.every((d) => d.status === 'Tạm dừng')) return 'Tạm dừng'
  return 'Đã xong'
}

function groupsOf(doses) {
  const map = new Map()
  for (const d of doses) {
    const key = `${d.time}:${d.residentCode}`
    if (!map.has(key)) map.set(key, { key, time: d.time, resident: residentOf(d.residentCode), doses: [] })
    map.get(key).doses.push(d)
  }
  return [...map.values()].map((g) => ({ ...g, status: groupStatus(g.doses) }))
}

function DoseCard({ group, canRecord, onOpen }) {
  const late = group.status === 'Trễ'
  const done = group.status === 'Đã xong'
  const r = group.resident
  const showRecord = canRecord && group.doses.some((d) => OPEN.includes(d.status))
  return (
    <Card className={cn('gap-3', late && 'ring-[1.5px] ring-destructive')}>
      <CardHeader className="flex flex-row items-center gap-2.5">
        <ResidentAvatar name={r.name} code={r.code} initials={r.initials} />
        <div className="flex min-w-0 flex-1 flex-col">
          <span className={cn('truncate type-label', done && 'text-muted-foreground')}>{r.name}</span>
          <span className="type-caption text-muted-foreground tabular-nums">{r.room === '—' ? 'Bán trú' : r.room}</span>
        </div>
        <StatusBadge status={group.status} />
      </CardHeader>
      <CardContent className="flex flex-1 flex-col gap-3">
        {/* Dị ứng hiện trên mọi thẻ thuốc (chủ dự án duyệt 2026-10-18) */}
        {!done && <AllergyNote allergies={r.allergies} />}
        <ul className="flex flex-col">
          {group.doses.map((d) => (
            <li key={d.id} className="flex flex-col gap-1.5 border-t py-2 first:border-t-0 first:pt-0">
              <div className="flex items-start justify-between gap-2">
                <span className={cn('type-card-title', done && 'text-muted-foreground')}>
                  {d.drug}
                  <span className="block type-body-sm font-normal text-muted-foreground">
                    {d.dose}, {d.route}
                  </span>
                </span>
                {d.status !== group.status && <StatusBadge status={d.status} />}
              </div>
              {!done && <DoseTags dose={d} />}
              {(d.result || d.note) && <span className="type-caption text-muted-foreground">{d.result ?? d.note}</span>}
            </li>
          ))}
        </ul>
        {showRecord && (
          <div className="mt-auto flex justify-end border-t pt-3">
            <Button variant={late ? 'default' : 'tonal'} onClick={() => onOpen(group)}>
              Phát thuốc
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function Section({ label, sub, count, defaultOpen, urgent, current, children }) {
  return (
    <Collapsible defaultOpen={defaultOpen} className="flex flex-col gap-3">
      <CollapsibleTrigger className="group flex min-h-11 w-fit items-center gap-2 rounded-full pr-2">
        <ChevronDown className="size-5 -rotate-90 text-foreground transition-transform group-data-[panel-open]:rotate-0" aria-hidden="true" />
        <span className={cn('type-title tabular-nums', current && 'text-accent-foreground')}>{label}</span>
        {sub && <span className="type-caption text-foreground tabular-nums">{sub}</span>}
        <span className={cn('rounded-full px-2 type-caption tabular-nums', urgent ? 'bg-destructive-subtle text-destructive' : 'bg-card text-muted-foreground')}>
          {count}
        </span>
      </CollapsibleTrigger>
      <CollapsibleContent>
        <div className="grid grid-cols-[repeat(auto-fit,minmax(280px,1fr))] gap-3 *:max-w-[480px]">{children}</div>
      </CollapsibleContent>
    </Collapsible>
  )
}

export function MedicationPage({ role }) {
  const canRecord = role === RECORD_ROLE
  const shift = DEMO_SHIFT
  const [mode, setMode] = useState('scheduled')
  const [filter, setFilter] = useState('Tất cả')
  const [doses, setDoses] = useState(DEMO_DOSES)
  const [opening, setOpening] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const groups = groupsOf(doses)
  const count = (s) => (s === 'Tất cả' ? groups.length : groups.filter((g) => g.status === s).length)
  const visible = groups.filter((g) => filter === 'Tất cả' || g.status === filter)
  const active = visible.filter((g) => g.status !== 'Đã xong')
  const done = visible.filter((g) => g.status === 'Đã xong')
  const times = [...new Set(active.map((g) => g.time))].sort()
  const current = times.find((t) => windowOf(t).from <= shift.now && shift.now <= windowOf(t).to)

  function save(results) {
    setDoses((all) =>
      all.map((d) => {
        const r = results.find((x) => x.dose.id === d.id)
        if (!r) return d
        if (r.result === 'Đã dùng') {
          const late = r.time > windowOf(d.time).to
          return { ...d, status: 'Đã dùng', result: `Đã dùng lúc ${r.time}${late ? ', dùng trễ' : ''}${r.lot ? `, ${r.lot}` : ''}` }
        }
        return { ...d, status: r.result, result: `${r.result}: ${r.reason.trim()}` }
      }),
    )
    setOpening(null)
    setAnnouncement(`Đã lưu ${results.length} liều của ${opening.resident.name}. Bản mẫu: trạng thái thật do backend trả.`)
  }

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
      <PageHeader
        title={`Phát thuốc ${shift.floor.toLowerCase()}, ${shift.name.toLowerCase()}`}
        subtitle={canRecord ? 'Liều của người cao tuổi bạn phụ trách và liều chung của tầng.' : 'Bạn chỉ xem; xác nhận liều do Điều dưỡng thực hiện.'}
        aside={<ClockAside time={shift.now} caption={`Ca ${shift.start} đến ${shift.end}`} />}
      >
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <ToggleGroup variant="filter" size="lg" value={[mode]} onValueChange={(n) => n[0] && setMode(n[0])} aria-label="Loại thuốc">
            <ToggleGroupItem value="scheduled" className="h-11 px-4">
              Liều theo lịch
            </ToggleGroupItem>
            <ToggleGroupItem value="prn" className="h-11 px-4">
              Thuốc khi cần
            </ToggleGroupItem>
            <ToggleGroupItem value="prescriptions" className="h-11 px-4">
              Đơn thuốc
            </ToggleGroupItem>
          </ToggleGroup>
          {mode === 'scheduled' && (
            <ToggleGroup variant="filter" size="lg" value={[filter]} onValueChange={(n) => n[0] && setFilter(n[0])} aria-label="Lọc theo trạng thái">
              {FILTERS.map((f) => (
                <ToggleGroupItem
                  key={f}
                  value={f}
                  className={cn('h-11 px-4', f === 'Trễ' && count('Trễ') > 0 && 'bg-destructive-subtle text-destructive')}
                >
                  {f} <span className="tabular-nums">{count(f)}</span>
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          )}
        </div>
      </PageHeader>

      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {announcement}
      </p>

      {mode === 'prn' ? (
        <PrnPanel canRecord={canRecord} now={shift.now} onSaved={setAnnouncement} />
      ) : mode === 'prescriptions' ? (
        <PrescriptionPanel canRecord={canRecord} onSaved={setAnnouncement} />
      ) : (
        <div className="flex flex-col gap-6">
          {times.map((t) => {
            const inSlot = active.filter((g) => g.time === t)
            const urgent = inSlot.some((g) => g.status === 'Trễ')
            const w = windowOf(t)
            return (
              <Section
                key={t}
                label={t}
                sub={`${w.from} → ${w.to}${t === current ? ', bây giờ' : ''}`}
                count={inSlot.length}
                urgent={urgent}
                current={t === current}
                defaultOpen={inSlot.some((g) => OPEN.includes(g.status))}
              >
                {inSlot.map((g) => (
                  <DoseCard key={g.key} group={g} canRecord={canRecord} onOpen={setOpening} />
                ))}
              </Section>
            )
          })}
          {done.length > 0 && (
            <Section label="Đã xong" count={done.length} defaultOpen={false}>
              {done.map((g) => (
                <DoseCard key={g.key} group={g} canRecord={false} onOpen={setOpening} />
              ))}
            </Section>
          )}
        </div>
      )}

      <DoseSheet group={opening} now={shift.now} onOpenChange={(open) => !open && setOpening(null)} onSave={save} />
    </div>
  )
}
