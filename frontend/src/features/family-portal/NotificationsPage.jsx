import { useState } from 'react'
import { OctagonAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_PORTAL_CONFIRMATIONS, DEMO_PORTAL_NOTIFICATIONS } from '@/common/demoData'
import { cn } from '@/lib/utils'
import { PortalCard } from './PortalParts'

// P6 Thông báo (009, UC-66) và việc chờ người đại diện quyết định (012 FR-046a, FR-046b, FR-050).
// Thông báo Khẩn cấp có "Xác nhận đã nhận"; đây không phải tiếp nhận xử lý (Q-102).

function Confirmation({ item }) {
  const [status, setStatus] = useState(item.status)
  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const decided = status !== item.status

  function reject(event) {
    event.preventDefault()
    setSubmitted(true)
    if (reason.trim()) setStatus('Đã từ chối')
  }

  return (
    <li className="flex flex-col gap-2 border-t py-3 first:border-t-0 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-caption text-muted-foreground">{item.kind}</span>
        <StatusBadge status={status} />
      </div>
      <span className="type-label">{item.text}</span>
      <span className="text-muted-foreground">{item.detail}</span>
      {!decided && !rejecting && (
        <div className="flex flex-wrap gap-2">
          <Button onClick={() => setStatus(item.accept === 'Đồng ý' ? 'Đã đồng ý' : 'Đã xác nhận')}>{item.accept}</Button>
          <Button variant="outline" onClick={() => setRejecting(true)}>
            Từ chối
          </Button>
        </div>
      )}
      {!decided && rejecting && (
        <form onSubmit={reject} noValidate className="flex flex-col gap-2">
          <Field data-invalid={(submitted && !reason.trim()) || undefined}>
            <FieldLabel htmlFor={`reason-${item.id}`}>Lý do từ chối</FieldLabel>
            <Textarea
              id={`reason-${item.id}`}
              rows={2}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              aria-invalid={(submitted && !reason.trim()) || undefined}
            />
            {submitted && !reason.trim() && <FieldError>Nhập lý do từ chối</FieldError>}
          </Field>
          <div className="flex flex-wrap gap-2">
            <Button type="submit">Gửi từ chối</Button>
            <Button type="button" variant="ghost" onClick={() => setRejecting(false)}>
              Quay lại
            </Button>
          </div>
        </form>
      )}
      {decided && <p className="type-caption text-muted-foreground">Bản mẫu: quyết định của bạn đã được ghi.</p>}
    </li>
  )
}

function Notification({ n }) {
  const [acked, setAcked] = useState(false)
  const urgent = n.level === 'Khẩn cấp'
  return (
    <li
      className={cn(
        'flex flex-col gap-2 rounded-xl border bg-card px-4 py-3',
        urgent && 'border-transparent ring-[1.5px] ring-destructive',
      )}
    >
      <div className="flex items-start gap-2">
        {urgent && <OctagonAlert className="mt-0.5 size-4 shrink-0 text-destructive" aria-hidden="true" />}
        <span className="min-w-0 flex-1">{n.text}</span>
        {urgent && <StatusBadge status="Khẩn cấp" />}
      </div>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-caption text-muted-foreground tabular-nums">{n.at}</span>
        {n.needsAck &&
          (acked ? (
            <span className="type-caption text-muted-foreground">Bạn đã xác nhận đã nhận.</span>
          ) : (
            <Button size="sm" onClick={() => setAcked(true)}>
              Xác nhận đã nhận
            </Button>
          ))}
      </div>
    </li>
  )
}

export function NotificationsPage({ viewer }) {
  const notifications = DEMO_PORTAL_NOTIFICATIONS
  return (
    <>
      <PageHeader title="Thông báo" subtitle="Thông báo viện gửi bạn và việc chờ bạn xác nhận." />
      {viewer.representative && (
        <PortalCard title="Chờ bạn quyết định">
          <ul className="flex flex-col">
            {DEMO_PORTAL_CONFIRMATIONS.map((c) => (
              <Confirmation key={c.id} item={c} />
            ))}
          </ul>
        </PortalCard>
      )}
      <ul className="flex flex-col gap-3" aria-label="Thông báo">
        {notifications.map((n) => (
          <Notification key={n.id} n={n} />
        ))}
      </ul>
    </>
  )
}
