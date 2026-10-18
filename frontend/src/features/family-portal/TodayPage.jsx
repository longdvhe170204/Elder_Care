import { Button } from '@/components/ui/button'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { SourceTag } from '@/common/SourceTag'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_INCIDENT, DEMO_PORTAL_BULLETINS, DEMO_PORTAL_HEALTH, DEMO_PORTAL_TODAY, DEMO_RESIDENTS } from '@/common/demoData'
import { HealthLock, InfoList, PortalCard } from './PortalParts'

// P2 Hôm nay (trang chủ cổng, 012 FR-046): thông tin chung cho mọi người thân có quan hệ Hiệu lực;
// lịch có giờ thuốc, kết quả chăm sóc và khối sức khỏe chỉ khi có quyền xem sức khỏe.

function ResidentCard() {
  const r = DEMO_RESIDENTS[0]
  return (
    <PortalCard>
      <div className="flex items-center gap-3">
        <div className="flex shrink-0 flex-col items-center gap-1">
          <ResidentAvatar name={r.name} code={r.code} initials={r.initials} className="size-14 type-title" />
          {!r.photo && <span className="type-caption text-muted-foreground">Chưa có ảnh</span>}
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="type-card-title">{r.name}</span>
          <span className="text-muted-foreground">Phòng {r.room}, {r.stayType.toLowerCase()}</span>
        </div>
        <StatusBadge status={r.status} />
      </div>
    </PortalCard>
  )
}

function TodaySchedule({ health }) {
  const items = DEMO_PORTAL_TODAY.filter((i) => health || !i.health)
  return (
    <PortalCard title="Lịch hôm nay">
      <ul className="flex flex-col">
        {items.map((i) => (
          <li key={i.time + i.text} className="flex items-center gap-3 border-t py-2 first:border-t-0 first:pt-0">
            <span className="w-12 shrink-0 type-label tabular-nums">{i.time}</span>
            <span className="min-w-0 flex-1">{i.text}</span>
            <span className="shrink-0 rounded-full bg-muted px-2.5 py-0.5 type-caption text-muted-foreground">{i.tag}</span>
          </li>
        ))}
      </ul>
      {!health && (
        <p className="type-caption text-muted-foreground">Lịch gồm hoạt động, lượt thăm, giờ đến, về; không gồm giờ thuốc, công việc chăm sóc.</p>
      )}
    </PortalCard>
  )
}

function HealthSummary({ onNavigate }) {
  const h = DEMO_PORTAL_HEALTH
  return (
    <PortalCard
      title="Sức khỏe"
      action={
        <Button variant="outline" size="sm" onClick={() => onNavigate('health')}>
          Xem sức khỏe
        </Button>
      }
    >
      <InfoList
        rows={[
          ...h.indicators.slice(0, 2).map((i) => [i.name, i.value]),
          ['Ăn trong 7 ngày', h.eating.meals],
          ['Thuốc đang dùng', `${h.medicines.length} thuốc`],
        ]}
      />
      <span className="type-caption text-muted-foreground">Cập nhật {h.updatedAt}</span>
    </PortalCard>
  )
}

// Sự cố: phần chung cho mọi người thân; phần sức khỏe theo bản đồng ý (Q-352, Q-358)
function RecentIncident({ health }) {
  const i = DEMO_INCIDENT
  return (
    <PortalCard title="Sự cố gần đây">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-label">
          {i.general.type}, mức {i.general.level.toLowerCase()}
        </span>
        <SourceTag source="done">Viện đã xử lý</SourceTag>
      </div>
      <InfoList
        rows={[
          ['Thời điểm', <span className="tabular-nums">{i.general.at}</span>],
          ['Việc đã làm', i.general.handled],
        ]}
      />
      {health ? (
        <InfoList
          rows={[
            ['Mô tả', i.health.description],
            ['Xử lý ban đầu', i.health.firstAid],
            ['Diễn biến', i.health.progress],
          ]}
        />
      ) : (
        <HealthLock />
      )}
    </PortalCard>
  )
}

export function TodayPage({ viewer, onNavigate }) {
  const health = viewer.perms.health
  const latest = DEMO_PORTAL_BULLETINS[0]
  // Máy tính: hai cột, thẻ người cao tuổi chiếm cả hàng; điện thoại: một cột
  return (
    <div className="grid items-start gap-4 lg:grid-cols-2 [&>*:first-child]:lg:col-span-2">
      <ResidentCard />
      <TodaySchedule health={health} />
      {/* Không có quyền xem sức khỏe thì không có mục Sức khỏe (012 User Story 5 kịch bản 2) */}
      {health && <HealthSummary onNavigate={onNavigate} />}
      <RecentIncident health={health} />
      <PortalCard
        title="Bản tin mới nhất"
        action={
          <Button variant="outline" size="sm" onClick={() => onNavigate('bulletins')}>
            Đọc bản tin
          </Button>
        }
      >
        <span>Tuần {latest.period}</span>
      </PortalCard>
    </div>
  )
}
