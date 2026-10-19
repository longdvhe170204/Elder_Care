import { LockKeyhole, PencilRuler } from 'lucide-react'
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty'

// Trạng thái cả màn (DESIGN.md mục 7.3): luôn đặt trên thẻ trắng, không đặt chữ phụ trên nền trời.
function StateCard({ icon: Icon, title, description }) {
  return (
    <Empty className="mx-auto max-w-[1600px] rounded-2xl border border-solid bg-card py-12">
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <Icon aria-hidden="true" />
        </EmptyMedia>
        <EmptyTitle>{title}</EmptyTitle>
        <EmptyDescription>{description}</EmptyDescription>
      </EmptyHeader>
    </Empty>
  )
}

// Màn chưa có bản mẫu trong giai đoạn thiết kế; không giả là chức năng đã chạy.
export function NotDesignedState({ label }) {
  return (
    <StateCard
      icon={PencilRuler}
      title={`${label}: chưa có bản mẫu`}
      description="Màn này chưa được thiết kế. Chức năng chưa hoạt động trong bản mẫu."
    />
  )
}

// Không có quyền xem (theo quyền backend trả, feature 002). Nói rõ cần làm gì tiếp.
export function PermissionDeniedState({ label, description }) {
  return (
    <StateCard
      icon={LockKeyhole}
      title={`Bạn không có quyền xem ${label.toLowerCase()}`}
      description={description ?? 'Quyền theo vai trò của bạn. Cần xem thì liên hệ Quản lý viện để được cấp quyền.'}
    />
  )
}
