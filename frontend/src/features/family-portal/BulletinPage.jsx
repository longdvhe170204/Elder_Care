import { useState } from 'react'
import { ArrowLeft, ChevronRight, Info, LockKeyhole } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/common/PageHeader'
import { SourceTag } from '@/common/SourceTag'
import { DEMO_PORTAL_BULLETINS } from '@/common/demoData'
import { HealthLock, InfoList, PortalCard } from './PortalParts'

// P4 Bản tin (012 FR-051 → FR-057): Điều dưỡng lập, duyệt, gửi; người thân chỉ thấy phần mình có quyền.
// Bản đã gửi không sửa; sai sót có bản đính chính, bản gốc mang dấu "đã được đính chính" (FR-056).

function BulletinDetail({ bulletin, viewer, onBack }) {
  const { general, health, costs } = bulletin
  return (
    <>
      <Button variant="ghost" className="self-start" onClick={onBack}>
        <ArrowLeft aria-hidden="true" />
        Tất cả bản tin
      </Button>
      <PageHeader title={`Bản tin tuần ${bulletin.period}`} subtitle={`Duyệt bởi ${bulletin.approvedBy}, gửi ${bulletin.sentAt}`} />
      {bulletin.corrected && (
        <p className="flex items-start gap-2 rounded-xl border bg-card px-4 py-3 type-body-sm">
          <Info className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          Bản tin này đã được đính chính. Nội dung đúng ở bản đính chính viện đã gửi.
        </p>
      )}
      <PortalCard title="Phần chung">
        <InfoList
          rows={[
            ['Hoạt động đã tham gia', <span className="tabular-nums">{general.activities}</span>],
            ['Số ngày vắng', <span className="tabular-nums">{general.absentDays}</span>],
          ]}
        />
        {general.incidents.length > 0 &&
          general.incidents.map((i) => (
            <div key={i} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted px-3 py-2">
              <span>Sự cố: {i}</span>
              <SourceTag source="done">Viện đã xử lý</SourceTag>
            </div>
          ))}
        <p>
          <span className="type-label">Nhận xét: </span>
          {general.comment}
        </p>
      </PortalCard>
      <PortalCard
        title={
          <span className="flex items-center gap-2">
            <LockKeyhole className="size-4 text-muted-foreground" aria-hidden="true" />
            Phần sức khỏe
          </span>
        }
      >
        {viewer.perms.health ? (
          <>
            <InfoList
              rows={[
                ['Bữa ăn', health.meals],
                ['Lượng nước', health.water],
                ['Cân nặng', health.weight],
                ['Chỉ số chính', health.indicators],
              ]}
            />
            {health.explanation && (
              <p>
                <span className="type-label">Giải thích về sự cố: </span>
                {health.explanation}
              </p>
            )}
          </>
        ) : (
          <HealthLock />
        )}
      </PortalCard>
      {viewer.perms.costs && (
        <PortalCard title="Chi phí tạm tính của tuần">
          <InfoList rows={costs.map((c) => [c.type, <span className="tabular-nums">{c.amount}</span>])} />
          <span className="type-caption text-muted-foreground">Tạm tính theo loại, chưa chốt, không có chi tiết từng khoản.</span>
        </PortalCard>
      )}
    </>
  )
}

export function BulletinPage({ viewer }) {
  const [openId, setOpenId] = useState(null)
  const open = DEMO_PORTAL_BULLETINS.find((b) => b.id === openId)
  if (open) return <BulletinDetail bulletin={open} viewer={viewer} onBack={() => setOpenId(null)} />

  return (
    <>
      <PageHeader title="Bản tin" subtitle="Bản tin hằng tuần do điều dưỡng phụ trách soạn." />
      <PortalCard>
        <ul className="flex flex-col">
          {DEMO_PORTAL_BULLETINS.map((b) => (
            <li key={b.id} className="border-t first:border-t-0">
              <button
                type="button"
                onClick={() => setOpenId(b.id)}
                className="flex min-h-14 w-full items-center gap-3 rounded-lg py-2 text-left"
              >
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="type-label">Tuần {b.period}</span>
                  <span className="type-caption text-muted-foreground">
                    Gửi {b.sentAt}
                    {b.corrected && ', đã được đính chính'}
                  </span>
                </span>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
              </button>
            </li>
          ))}
        </ul>
      </PortalCard>
    </>
  )
}
