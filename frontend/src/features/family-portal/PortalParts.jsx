import { LockKeyhole } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'

// Phần dùng chung của cổng người thân (DESIGN.md 2.2, 15.4): thẻ có tiêu đề, dòng nhãn và giá trị, dòng khóa.

export function PortalCard({ title, action, className, children }) {
  return (
    <Card className={className}>
      {(title || action) && (
        <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-2">
          {title && <CardTitle className="type-card-title">{title}</CardTitle>}
          {action}
        </CardHeader>
      )}
      <CardContent className="flex flex-col gap-3 type-body-sm">{children}</CardContent>
    </Card>
  )
}

// Danh sách dòng nhãn – giá trị, có đường kẻ giữa các dòng
export function InfoList({ rows }) {
  return (
    <dl className="flex flex-col">
      {rows.map(([label, value], i) => (
        <div key={i} className="flex items-start justify-between gap-4 border-t py-2 first:border-t-0 first:pt-0 last:pb-0">
          <dt className="text-muted-foreground">{label}</dt>
          <dd className="text-right">{value}</dd>
        </div>
      ))}
    </dl>
  )
}

// Không lộ tiêu đề hay độ dài của nội dung bị ẩn (DESIGN.md 2.2).
export function HealthLock({ className }) {
  return (
    <p className={cn('flex items-start gap-2 rounded-lg bg-muted px-3 py-2 text-muted-foreground', className)}>
      <LockKeyhole className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      Thông tin sức khỏe chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ.
    </p>
  )
}

