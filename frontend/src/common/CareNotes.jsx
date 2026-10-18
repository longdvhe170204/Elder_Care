import { Handshake, Info, TriangleAlert } from 'lucide-react'
import { cn } from '@/lib/utils'

// Ghi chú an toàn (DESIGN.md mục 7.2, care-notes): dị ứng và lưu ý chăm sóc tách hai kiểu để
// dị ứng không lẫn vào lưu ý thường. Dị ứng: tông attention có biểu tượng cảnh báo và viền;
// lưu ý chăm sóc: nền phụ, chữ chính. Không dùng đỏ (đỏ chỉ cho việc gấp).
export function AllergyNote({ allergies, className }) {
  if (!allergies?.length) return null
  return (
    <p
      className={cn(
        'flex items-start gap-2 rounded-lg border border-warning-foreground/40 bg-warning px-3 py-2 type-body-sm text-warning-foreground',
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

export function CareNote({ note, lines, className }) {
  if (!note) return null
  return (
    <p className={cn('flex items-start gap-2 rounded-lg bg-muted px-3 py-2 type-body-sm', className)}>
      <Info className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
      <span className={cn(lines === 2 && 'line-clamp-2')}>
        <span className="font-medium">Lưu ý: </span>
        {note}
      </span>
    </p>
  )
}

// Thỏa thuận lựa chọn có rủi ro (DESIGN.md mục 2.2, choice-note; Q-349, Q-360): lựa chọn của người cao tuổi
// đã được trao đổi, không phải cảnh báo. Nền xanh trời như nhãn "Theo lời người cao tuổi"; không chặn việc,
// không có nút. Chỉ hiện lựa chọn và ngày xem lại; rủi ro, người tham gia, nhận định ở kế hoạch chăm sóc.
export function ChoiceNote({ choices, lines, className }) {
  if (!choices?.length) return null
  return choices.map((c) => (
    <p
      key={c.choice}
      className={cn('flex items-start gap-2 rounded-lg bg-accent px-3 py-2 type-body-sm text-accent-foreground', className)}
    >
      <Handshake className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <span className={cn(lines === 2 && 'line-clamp-2')}>
        <span className="font-medium">Lựa chọn của người cao tuổi: </span>
        {c.choice}. Đã trao đổi rủi ro, xem lại <span className="tabular-nums">{c.reviewOn}</span>.
      </span>
    </p>
  ))
}
