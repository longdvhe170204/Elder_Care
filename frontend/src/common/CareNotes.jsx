import { TriangleAlert } from 'lucide-react'
import { cn } from '@/lib/utils'

// Ghi chú an toàn (DESIGN.md mục 7.2, care-notes, 6.0.0): chỉ dị ứng là khối (tông attention, viền, biểu tượng
// cảnh báo) để không lẫn vào lưu ý thường; lựa chọn và lưu ý chăm sóc là chữ có nhãn nhỏ phía trên, không nền.
// Không dùng đỏ (đỏ chỉ cho việc gấp). Thứ tự dùng: dị ứng, lựa chọn, lưu ý.
export function AllergyNote({ allergies, className }) {
  if (!allergies?.length) return null
  return (
    <p
      className={cn(
        'flex items-start gap-2 rounded-md border border-warning-foreground/40 bg-warning px-3 py-2 type-body-sm text-warning-foreground',
        className,
      )}
    >
      <TriangleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <span>
        <span className="font-semibold">Dị ứng: </span>
        {allergies.join(', ')}
      </span>
    </p>
  )
}

function LabeledNote({ label, lines, className, children }) {
  return (
    <p className={cn('flex flex-col type-body-sm', className)}>
      <span className="type-caption text-muted-foreground">{label}</span>
      <span className={cn(lines === 2 && 'line-clamp-2')}>{children}</span>
    </p>
  )
}

export function CareNote({ note, lines, className }) {
  if (!note) return null
  return (
    <LabeledNote label="Lưu ý chăm sóc" lines={lines} className={className}>
      {note}
    </LabeledNote>
  )
}

// Thỏa thuận lựa chọn có rủi ro (DESIGN.md mục 2.2, choice-note; Q-349, Q-360): lựa chọn của người cao tuổi
// đã được trao đổi, không phải cảnh báo; không chặn việc, không có nút. Chỉ hiện lựa chọn và ngày xem lại;
// rủi ro, người tham gia, nhận định ở kế hoạch chăm sóc.
export function ChoiceNote({ choices, lines, className }) {
  if (!choices?.length) return null
  return choices.map((c) => (
    <LabeledNote key={c.choice} label="Lựa chọn của người cao tuổi" lines={lines} className={className}>
      {c.choice}. Đã trao đổi rủi ro, xem lại <span className="tabular-nums">{c.reviewOn}</span>.
    </LabeledNote>
  ))
}
