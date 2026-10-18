import { Badge } from '@/components/ui/badge'

// Ánh xạ trạng thái → tông theo DESIGN.md (mục Trạng thái → tông màu).
// Trạng thái chưa có trong bảng hiển thị tông neutral.
const STATUS_TONE = {
  'Quá hạn': 'urgent',
  'Khẩn cấp': 'urgent',
  'Đến hạn': 'attention',
  'Tạm vắng': 'attention',
  'Điều trị tại bệnh viện': 'attention',
  'Đang tiếp nhận': 'attention',
  'Chờ sẵn sàng': 'attention',
  'Có ý kiến': 'attention',
  'Chờ xác nhận': 'attention',
  'Hoàn thành': 'settled',
  'Đang lưu trú': 'settled',
  'Đang giữ chỗ': 'held',
  'Đang sử dụng': 'occupied',
}

function statusTone(status) {
  return STATUS_TONE[status] ?? 'neutral'
}

export function StatusBadge({ status, className }) {
  return (
    <Badge variant={statusTone(status)} className={className}>
      {status}
    </Badge>
  )
}
