import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_PORTAL_DEPOSITS } from '@/common/demoData'
import { PortalCard } from './PortalParts'

// P9 Đồ gửi (16, 013; 012 FR-046b): chỉ người đại diện và người được phép đón đang hiệu lực thấy.
// Xác nhận người nhận khác của người đại diện nằm ở mục Thông báo, phần Chờ bạn quyết định.

export function DepositsPage({ viewer }) {
  return (
    <>
      <PageHeader title="Đồ gửi" subtitle="Đồ dùng gia đình gửi viện giữ hộ và lần bàn giao gần nhất." />
      <PortalCard>
        <ul className="flex flex-col">
          {DEMO_PORTAL_DEPOSITS.map((d) => (
            <li key={d.name} className="flex flex-col gap-1.5 border-t py-3 first:border-t-0 first:pt-0 last:pb-0">
              <span className="type-label">
                {d.name}, số lượng <span className="tabular-nums">{d.qty}</span>
              </span>
              <StatusBadge status={d.status} />
              <span className="type-caption text-muted-foreground">Lần gần nhất: {d.lastHandover}</span>
            </li>
          ))}
        </ul>
        <span className="type-caption text-muted-foreground">Ảnh đồ gửi: chưa có ảnh trong bản mẫu.</span>
      </PortalCard>
      {viewer.representative && (
        <PortalCard>
          <p>Khi viện cần trả đồ cho người không thuộc danh sách được phép đón, yêu cầu xác nhận sẽ ở mục Thông báo.</p>
        </PortalCard>
      )}
    </>
  )
}
