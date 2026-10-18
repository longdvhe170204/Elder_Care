import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_PORTAL_COSTS } from '@/common/demoData'
import { PortalCard } from './PortalParts'

// P7 Chi phí, số dư (010, 017; 012 FR-046 loại chi phí): chỉ người có quyền xem chi phí. Khoản thuốc chỉ hiện
// mã vật phẩm, tên thuốc chỉ hiện khi người xem có cả quyền xem sức khỏe (Phụ lục 27 chú thích 16).
// Nộp tiền bằng tiền mặt hoặc chuyển khoản ngoài hệ thống, không có cổng thanh toán (Q-285).

function Period({ period, health }) {
  return (
    <PortalCard title={period.period} action={<StatusBadge status={period.status} />}>
      <ul className="flex flex-col">
        {period.lines.map((l) => (
          <li key={l.type + (l.item ?? '')} className="flex items-start justify-between gap-4 border-t py-2 first:border-t-0 first:pt-0">
            <span className="flex flex-col gap-0.5">
              <span>{l.type}</span>
              {l.item && <span className="type-caption text-muted-foreground">{health && l.medicine ? l.medicine : l.item}</span>}
            </span>
            <span className="type-label tabular-nums">{l.amount}</span>
          </li>
        ))}
        <li className="flex items-center justify-between gap-4 border-t pt-2">
          <span className="type-label">Tổng</span>
          <span className="type-card-title tabular-nums">{period.total}</span>
        </li>
      </ul>
    </PortalCard>
  )
}

export function CostsPage({ viewer }) {
  return (
    <>
      <PageHeader title="Chi phí, số dư" subtitle="Bảng chi phí từng tháng và số dư của người cao tuổi." />
      <PortalCard title="Số dư hiện tại">
        <span className="type-metric">{DEMO_PORTAL_COSTS.balance}</span>
        <span className="text-muted-foreground">Nộp tiền bằng tiền mặt tại viện hoặc chuyển khoản vào tài khoản của viện.</span>
      </PortalCard>
      {DEMO_PORTAL_COSTS.periods.map((p) => (
        <Period key={p.period} period={p} health={viewer.perms.health} />
      ))}
    </>
  )
}
