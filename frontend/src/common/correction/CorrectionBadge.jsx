import { StatusBadge } from '@/common/StatusBadge'

// Nhãn đặt cạnh bản ghi trên màn của module sở hữu, để người xem nhận ra bản ghi đã được đính chính
// mà không cần tra nhật ký (FR-028, SC-007). Bản ghi đã Hủy ghi nhận vẫn xem lại được (FR-029).
export function CorrectionBadge({ corrected, voided, className }) {
  if (voided) return <StatusBadge status="Đã hủy ghi nhận" className={className} />
  if (corrected) return <StatusBadge status="Đã đính chính" className={className} />
  return null
}
