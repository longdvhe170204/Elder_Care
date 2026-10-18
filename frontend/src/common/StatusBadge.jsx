import { Badge } from '@/components/ui/badge'

// Ánh xạ trạng thái → tông theo DESIGN.md (mục Trạng thái → tông màu).
// Trạng thái chưa có trong bảng hiển thị tông neutral.
const STATUS_TONE = {
  'Quá hạn': 'urgent',
  'Trễ': 'urgent',
  'Bỏ lỡ': 'urgent',
  'Khẩn cấp': 'urgent',
  'Đến hạn': 'attention',
  'Đến giờ': 'attention',
  'Tạm vắng': 'attention',
  'Điều trị tại bệnh viện': 'attention',
  'Đang tiếp nhận': 'attention',
  'Chờ sẵn sàng': 'attention',
  'Có ý kiến': 'attention',
  'Chờ xác nhận': 'attention',
  'Chờ đồng ý': 'attention',
  'Mở lại': 'attention',
  'Đang xử lý': 'attention',
  'Thất lạc': 'attention',
  'Hư hỏng': 'attention',
  'Hoàn thành': 'settled',
  'Đã dùng': 'settled',
  'Đang lưu trú': 'settled',
  'Đã duyệt': 'settled',
  'Đã phản hồi': 'settled',
  'Đã đồng ý': 'settled',
  'Đã xác nhận': 'settled',
  'Hiệu lực': 'settled',
  'Đã vào': 'occupied',
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
