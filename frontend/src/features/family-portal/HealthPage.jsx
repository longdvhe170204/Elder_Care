import { AllergyNote } from '@/common/CareNotes'
import { PageHeader } from '@/common/PageHeader'
import { DEMO_PORTAL_HEALTH } from '@/common/demoData'
import { InfoList, PortalCard } from './PortalParts'

// P3 Sức khỏe (012 FR-046 loại sức khỏe): chỉ mở được khi người thân có quyền xem sức khỏe có tác dụng.
// Mỗi lần mở được ghi nhật ký (FR-048); người thân không xem nhật ký. Chỉ giá trị hiện hành (FR-047).

export function HealthPage() {
  const h = DEMO_PORTAL_HEALTH
  return (
    <>
      <PageHeader title="Sức khỏe" subtitle={`Cập nhật ${h.updatedAt}. Mỗi lần xem mục này được viện ghi lại.`} />
      <PortalCard title="Dị ứng, bệnh nền">
        <AllergyNote allergies={h.allergies} />
        <InfoList rows={[['Bệnh nền', h.conditions.join(', ')]]} />
      </PortalCard>
      <PortalCard title="Chỉ số gần nhất">
        <InfoList
          rows={h.indicators.map((i) => [
            i.name,
            <span className="flex flex-col items-end">
              <span className="type-label tabular-nums">{i.value}</span>
              <span className="type-caption text-muted-foreground">{i.at}</span>
            </span>,
          ])}
        />
      </PortalCard>
      <PortalCard title="Cân nặng theo tuần">
        <InfoList rows={h.weight.map((w) => [`Tuần ${w.week}`, <span className="tabular-nums">{w.value}</span>])} />
      </PortalCard>
      <PortalCard title="Thuốc đang dùng">
        <ul className="flex flex-col">
          {h.medicines.map((m) => (
            <li key={m.name} className="flex flex-col gap-0.5 border-t py-2 first:border-t-0 first:pt-0 last:pb-0">
              <span className="type-label">{m.name}</span>
              <span className="text-muted-foreground">
                {m.dose}, {m.times.toLowerCase()}
              </span>
            </li>
          ))}
        </ul>
      </PortalCard>
      <PortalCard title="Ăn uống">
        <InfoList
          rows={[
            ['Bữa ăn 7 ngày', h.eating.meals],
            ['Lượng nước', h.eating.water],
            ['Chế độ ăn riêng', h.diet],
          ]}
        />
      </PortalCard>
      <PortalCard title="Lịch khám bên ngoài">
        <InfoList rows={h.externalVisits.map((v) => [<span className="tabular-nums">{v.date}</span>, v.place])} />
      </PortalCard>
    </>
  )
}
