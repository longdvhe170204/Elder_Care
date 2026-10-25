import { Separator } from '@/components/ui/separator'
import { HealthItems } from './HealthItems'
import { InitialHealthRecord } from './InitialHealthRecord'

// Tab "Sức khỏe" của khung chi tiết hồ sơ (001 T033; US2; bố cục duyệt 2026-10-22): hồ sơ sức khỏe ban đầu rồi mục dị
// ứng, bệnh nền, tiền sử bệnh. Chỉ hiện khi vai trò được xem phần sức khỏe (ResidentDetail ẩn tab khi backend bỏ
// activeHealthItems theo hiddenFields). Nút lệnh theo allowedCommands của hồ sơ.
export function HealthTab({ residentId, allowedCommands = [] }) {
  return (
    <div className="flex flex-col gap-5">
      <InitialHealthRecord residentId={residentId} allowedCommands={allowedCommands} />
      <Separator />
      <HealthItems residentId={residentId} allowedCommands={allowedCommands} />
    </div>
  )
}
