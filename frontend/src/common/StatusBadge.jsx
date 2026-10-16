import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

// Màu theo mức chú ý, không theo từng trạng thái: đỏ chỉ dành cho việc cần xử lý ngay.
const TONES = {
  urgent: 'bg-destructive/10 text-destructive',
  attention: 'bg-warning text-warning-foreground',
  settled: 'bg-success text-success-foreground',
  neutral: 'bg-muted text-muted-foreground',
}

const STATUS_TONE = {
  'Quá hạn': 'urgent',
  'Khẩn cấp': 'urgent',
  'Đến hạn': 'attention',
  'Tạm vắng': 'attention',
  'Điều trị tại bệnh viện': 'attention',
  'Đang tiếp nhận': 'attention',
  'Hoàn thành': 'settled',
  'Đang lưu trú': 'settled',
}

export function StatusBadge({ status, className }) {
  const tone = STATUS_TONE[status] ?? 'neutral'
  return <Badge className={cn('font-normal', TONES[tone], className)}>{status}</Badge>
}
