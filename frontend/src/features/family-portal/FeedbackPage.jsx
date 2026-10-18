import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_PORTAL_FEEDBACK, DEMO_PORTAL_FEEDBACK_GROUPS, DEMO_PORTAL_FEEDBACK_TYPES } from '@/common/demoData'
import { PortalCard } from './PortalParts'

// P8 Phản hồi (14.7; 012 FR-058 → FR-066): Góp ý, Khiếu nại, Hỏi đáp; nội dung đã gửi không sửa.
// Trả lời nhóm "Sức khỏe, thuốc" chỉ hiện ở mức chung với người chưa có quyền xem sức khỏe (FR-058).
// Hạn trả lời mặc định theo CFG-M10-01: Khiếu nại 24 giờ, Góp ý và Hỏi đáp 72 giờ.

function FeedbackForm({ viewer, onSent }) {
  const [type, setType] = useState('')
  const [group, setGroup] = useState('')
  const [text, setText] = useState('')
  const [submitted, setSubmitted] = useState(false)

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!type || !group || !text.trim()) return
    onSent({ id: `new-${Date.now()}`, type, group, text: text.trim(), sentAt: 'Vừa gửi', status: 'Mới' })
    setType('')
    setGroup('')
    setText('')
    setSubmitted(false)
  }

  return (
    <PortalCard title="Gửi phản hồi">
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <FieldGroup>
          <Field data-invalid={(submitted && !type) || undefined}>
            <FieldLabel>Loại</FieldLabel>
            <ToggleGroup variant="outline" value={type ? [type] : []} onValueChange={(n) => setType(n[0] ?? '')} className="flex flex-wrap">
              {DEMO_PORTAL_FEEDBACK_TYPES.map((t) => (
                <ToggleGroupItem key={t} value={t} className="h-11 px-4">
                  {t}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
            {type && <FieldDescription>Viện trả lời trong {type === 'Khiếu nại' ? '24' : '72'} giờ.</FieldDescription>}
            {submitted && !type && <FieldError>Chọn loại phản hồi</FieldError>}
          </Field>
          <Field data-invalid={(submitted && !group) || undefined}>
            <FieldLabel>Nội dung về</FieldLabel>
            <ToggleGroup variant="outline" value={group ? [group] : []} onValueChange={(n) => setGroup(n[0] ?? '')} className="flex flex-wrap">
              {DEMO_PORTAL_FEEDBACK_GROUPS.map((g) => (
                <ToggleGroupItem key={g} value={g} className="h-11 px-4">
                  {g}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
            {group === 'Sức khỏe, thuốc' && !viewer.perms.health && (
              <FieldDescription>Bạn chưa có quyền xem sức khỏe nên phần trả lời chỉ gồm thông tin chung.</FieldDescription>
            )}
            {submitted && !group && <FieldError>Chọn nội dung phản hồi về việc gì</FieldError>}
          </Field>
          <Field data-invalid={(submitted && !text.trim()) || undefined}>
            <FieldLabel htmlFor="feedback-text">Nội dung</FieldLabel>
            <Textarea id="feedback-text" rows={4} value={text} onChange={(e) => setText(e.target.value)} aria-invalid={(submitted && !text.trim()) || undefined} />
            {submitted && !text.trim() && <FieldError>Nhập nội dung phản hồi</FieldError>}
          </Field>
          <Field>
            <FieldLabel htmlFor="feedback-file">Tệp đính kèm</FieldLabel>
            <Input id="feedback-file" type="file" className="h-11" />
            <FieldDescription>Không bắt buộc.</FieldDescription>
          </Field>
        </FieldGroup>
        <Button type="submit" className="h-11">
          Gửi phản hồi
        </Button>
      </form>
    </PortalCard>
  )
}

function FeedbackItem({ item }) {
  const [status, setStatus] = useState(item.status)
  const [reopening, setReopening] = useState(false)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)

  function reopen(event) {
    event.preventDefault()
    setSubmitted(true)
    if (reason.trim()) {
      setStatus('Mở lại')
      setReopening(false)
    }
  }

  return (
    <li className="flex flex-col gap-2 border-t py-3 first:border-t-0 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-caption text-muted-foreground">
          {item.type}, {item.group.toLowerCase()}, {item.sentAt}
        </span>
        <StatusBadge status={status} />
      </div>
      <span>{item.text}</span>
      {item.reply && (
        <p className="rounded-lg bg-muted px-3 py-2">
          <span className="type-label">Viện trả lời: </span>
          {item.reply}
        </p>
      )}
      {status === 'Đã phản hồi' && !reopening && (
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={() => setStatus('Đóng')}>
            Đồng ý đóng
          </Button>
          <Button variant="ghost" size="sm" onClick={() => setReopening(true)}>
            Mở lại
          </Button>
        </div>
      )}
      {reopening && (
        <form onSubmit={reopen} noValidate className="flex flex-col gap-2">
          <Field data-invalid={(submitted && !reason.trim()) || undefined}>
            <FieldLabel htmlFor={`reopen-${item.id}`}>Lý do mở lại</FieldLabel>
            <Textarea id={`reopen-${item.id}`} rows={2} value={reason} onChange={(e) => setReason(e.target.value)} aria-invalid={(submitted && !reason.trim()) || undefined} />
            {submitted && !reason.trim() && <FieldError>Nhập lý do mở lại</FieldError>}
          </Field>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="sm">
              Gửi mở lại
            </Button>
            <Button type="button" variant="ghost" size="sm" onClick={() => setReopening(false)}>
              Quay lại
            </Button>
          </div>
        </form>
      )}
    </li>
  )
}

export function FeedbackPage({ viewer }) {
  const [items, setItems] = useState(DEMO_PORTAL_FEEDBACK)
  const [notice, setNotice] = useState('')
  return (
    <>
      <PageHeader title="Phản hồi" subtitle="Góp ý, khiếu nại hoặc hỏi viện về việc chăm sóc." />
      <FeedbackForm
        viewer={viewer}
        onSent={(f) => {
          setItems([f, ...items])
          setNotice('Đã gửi phản hồi. Viện sẽ báo bạn khi có trả lời.')
        }}
      />
      <p role="status" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {notice}
      </p>
      <PortalCard title="Phản hồi của bạn">
        <ul className="flex flex-col">
          {items.map((f) => (
            <FeedbackItem key={f.id} item={f} />
          ))}
        </ul>
      </PortalCard>
    </>
  )
}
