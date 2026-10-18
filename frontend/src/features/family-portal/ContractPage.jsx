import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_PORTAL_CONTRACT } from '@/common/demoData'
import { InfoList, PortalCard } from './PortalParts'

// P10 Hợp đồng, dịch vụ (6.3, 6.6; 012 FR-046 loại hợp đồng, FR-049): chỉ người đại diện xem, không sửa.
// Mọi thay đổi đi qua yêu cầu thay đổi, khi được duyệt thành phụ lục. Nút gửi yêu cầu chỉ hiện khi có quyền.

export function ContractPage({ viewer }) {
  const c = DEMO_PORTAL_CONTRACT
  const [asked, setAsked] = useState(false)
  return (
    <>
      <PageHeader title="Hợp đồng, dịch vụ" subtitle="Hợp đồng chỉ xem. Thay đổi đi qua yêu cầu và thành phụ lục." />
      <PortalCard title={`Hợp đồng ${c.code}`} action={<StatusBadge status={c.status} />}>
        <InfoList
          rows={[
            ['Loại hình lưu trú', c.stayType],
            ['Thời hạn', <span className="tabular-nums">{`${c.from} → ${c.to}`}</span>],
            ['Mức chăm sóc', c.careLevel],
            ['Đặt cọc', c.deposit],
          ]}
        />
      </PortalCard>
      <PortalCard title="Dịch vụ đang dùng">
        <ul className="flex flex-col gap-1.5">
          {c.services.map((s) => (
            <li key={s}>{s}</li>
          ))}
        </ul>
        {viewer.perms.serviceChange && (
          <div className="flex flex-col gap-2 border-t pt-3">
            <Button variant="outline" className="self-start" onClick={() => setAsked(true)}>
              Gửi yêu cầu thay đổi dịch vụ
            </Button>
            <p role="status" className="type-caption text-muted-foreground">
              {asked && 'Form yêu cầu thay đổi dịch vụ: chưa có bản mẫu.'}
            </p>
          </div>
        )}
      </PortalCard>
      <PortalCard title="Phụ lục">
        <InfoList rows={c.annexes.map((a) => [`${a.code}, ${a.date}`, a.text])} />
      </PortalCard>
    </>
  )
}
