import { CalendarClock } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { formatDateTime } from '@/common/dateTime'
import { REASSESSMENT_BASIS_LABELS } from './assessmentLabels'

// Dải cảnh báo đầu tab Đánh giá (001 T066; US6 #1 → #4; FR-041, FR-042; bố cục duyệt 2026-10-23): chỉ hiện khi cần hoặc
// quá hạn đánh giá lại. Hệ thống chỉ báo, không tự lập đánh giá hay yêu cầu (Q-307), nên không có nút. Quá hạn dùng tông
// urgent (đỏ chỉ cho việc gấp, DESIGN.md 1); chỉ cần đánh giá lại dùng attention.

export function ReassessmentNotice({ reassessment }) {
  if (!reassessment || (reassessment.status !== 'DUE' && reassessment.status !== 'OVERDUE')) return null
  const overdue = reassessment.status === 'OVERDUE'

  return (
    <Alert variant={overdue ? 'urgent' : 'attention'}>
      <CalendarClock aria-hidden="true" />
      <AlertTitle>
        {overdue ? `Quá hạn đánh giá lại từ ${formatDateTime(reassessment.overdueSince)}` : 'Cần đánh giá lại'}
      </AlertTitle>
      <AlertDescription className="flex flex-col gap-1">
        {!overdue && (
          <p>
            {reassessment.dueAt
              ? `Hạn đánh giá lại: ${formatDateTime(reassessment.dueAt)}.`
              : 'Đang điều trị tại bệnh viện: hạn tính từ lúc Ghi nhận trở về.'}
          </p>
        )}
        <ul className="flex flex-col">
          {reassessment.basis.map((b) => (
            <li key={`${b.basis}-${b.source}`} className="tabular-nums">
              {REASSESSMENT_BASIS_LABELS[b.basis] ?? b.basis}, báo lúc {formatDateTime(b.notifiedAt)}
            </li>
          ))}
        </ul>
        <p>Hệ thống chỉ báo. Bác sĩ hoặc Điều dưỡng tự lập đánh giá lại khi thấy cần.</p>
      </AlertDescription>
    </Alert>
  )
}
