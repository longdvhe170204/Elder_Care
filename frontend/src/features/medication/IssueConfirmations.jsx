import { AlertTriangle, CheckCircle2 } from 'lucide-react'
import { FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { issueText } from './prescriptionView'

// Kết quả kiểm tra trùng hoạt chất, dị ứng (FR-007, FR-008) từ backend: vấn đề phải có lý do xác nhận mới lưu được
// (BR-M07-06); mục dị ứng loại khác chỉ để xem xét, không chặn. Thẻ không viền đỏ (DESIGN.md 6.7).
export function IssueConfirmations({ check, reasons, onReason }) {
  if (!check) return null
  const { issues, reviewItems } = check
  if (issues.length === 0 && reviewItems.length === 0)
    return (
      <p className="flex items-center gap-2 rounded-md bg-muted px-3 py-2 type-body-sm">
        <CheckCircle2 className="size-4 shrink-0" aria-hidden="true" />
        Không phát hiện trùng hoạt chất hoặc xung đột dị ứng.
      </p>
    )
  return (
    <div className="flex flex-col gap-3">
      {issues.map((issue) => (
        <div key={issue.issueCode} className="flex flex-col gap-2 rounded-lg border bg-card p-3">
          <span className="flex items-start gap-2 type-label">
            <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            {issueText(issue)}
          </span>
          <FieldLabel htmlFor={`reason_${issue.issueCode}`} className="type-caption">
            Lý do xác nhận (bắt buộc, FR-007)
          </FieldLabel>
          <Input
            id={`reason_${issue.issueCode}`}
            value={reasons[issue.issueCode] ?? ''}
            onChange={(e) => onReason(issue.issueCode, e.target.value)}
            placeholder="Lý do chuyên môn"
          />
        </div>
      ))}
      {reviewItems.map((item, i) => (
        <p key={item.issueCode ?? i} className="flex items-start gap-2 rounded-md bg-warning px-3 py-2 type-body-sm text-warning-foreground">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          {issueText(item)}
        </p>
      ))}
    </div>
  )
}
