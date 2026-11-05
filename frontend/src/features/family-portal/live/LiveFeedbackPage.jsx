import { useCallback, useEffect, useState } from 'react'
import { getFeedback, getFeedbackById, postFeedback, postFeedbackByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import {
  FEEDBACK_GROUPS,
  FEEDBACK_STATUS,
  FEEDBACK_TYPES,
  ITEM_LABELS,
  groupLabel,
  typeLabel,
} from '@/features/relatives/feedback/feedbackLabels'
import { HealthLock, PortalCard } from '../PortalParts'

// P8 Phản hồi với phiên thật (012 T030; US6, FR-058, FR-061, FR-064 → FR-066). Gửi cho người cao tuổi đang xem; nội
// dung đã gửi không sửa; Đã phản hồi thì Đồng ý đóng hoặc Mở lại có lý do. Trả lời nhóm "Sức khỏe, thuốc" là thông tin
// sức khỏe: người xem không có quyền có tác dụng thì backend bỏ phần đó, màn hiện dòng khóa (DESIGN.md 2.2, 2.3).

function ChoiceField({ label, options, value, onChange, error, children }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel>{label}</FieldLabel>
      <ToggleGroup
        variant="outline"
        value={value ? [value] : []}
        onValueChange={(n) => onChange(n[0] ?? '')}
        aria-label={label}
        className="flex flex-wrap"
      >
        {options.map(([v, text]) => (
          <ToggleGroupItem key={v} value={v} className="h-11 px-4">
            {text}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      {children}
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

function SubmitForm({ viewer, onSent }) {
  const [type, setType] = useState('')
  const [group, setGroup] = useState('')
  const [content, setContent] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!type || !group || !content.trim()) return
    setFailure(null)
    try {
      await postFeedback({
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { residentId: viewer.residentId, type, group, content: content.trim() },
      })
      setType('')
      setGroup('')
      setContent('')
      setSubmitted(false)
      onSent()
    } catch (e) {
      setFailure(problemOf(e))
    }
  }

  return (
    <PortalCard title="Gửi phản hồi">
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <FieldGroup>
          <ChoiceField
            label="Loại"
            options={FEEDBACK_TYPES}
            value={type}
            onChange={setType}
            error={submitted && !type ? 'Chọn loại phản hồi' : ''}
          >
            {type && <FieldDescription>Viện trả lời trong {type === 'COMPLAINT' ? '24' : '72'} giờ.</FieldDescription>}
          </ChoiceField>
          <ChoiceField
            label="Nội dung về"
            options={FEEDBACK_GROUPS}
            value={group}
            onChange={setGroup}
            error={submitted && !group ? 'Chọn nội dung phản hồi về việc gì' : ''}
          >
            {group === 'HEALTH_MEDICATION' && !viewer.perms.health && (
              <FieldDescription>
                Bạn chưa có quyền xem sức khỏe nên phần trả lời chỉ gồm thông tin chung.
              </FieldDescription>
            )}
          </ChoiceField>
          <Field data-invalid={(submitted && !content.trim()) || undefined}>
            <FieldLabel htmlFor="feedback-text">Nội dung</FieldLabel>
            <Textarea
              id="feedback-text"
              rows={4}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              aria-invalid={(submitted && !content.trim()) || undefined}
            />
            {submitted && !content.trim() && <FieldError>Nhập nội dung phản hồi</FieldError>}
          </Field>
        </FieldGroup>
        <ProblemAlert problem={failure} />
        <Button type="submit" className="h-11">
          Gửi phản hồi
        </Button>
      </form>
    </PortalCard>
  )
}

function Replies({ detail }) {
  if (!detail) return <Skeleton className="h-12 w-full" />
  const replies = detail.items.filter((i) => i.type !== 'CONTENT')
  return (
    <div className="flex flex-col gap-2">
      {replies.map((i) => (
        <p key={i.id} className="rounded-lg bg-muted px-3 py-2">
          <span className="type-label">{ITEM_LABELS[i.type] ?? i.type}: </span>
          {i.content}
          <span className="block type-caption text-muted-foreground">{formatDateTime(i.at)}</span>
        </p>
      ))}
      {detail.healthReplyHidden && <HealthLock />}
      {replies.length === 0 && !detail.healthReplyHidden && <p className="text-muted-foreground">Chưa có trả lời.</p>}
    </div>
  )
}

function FeedbackRow({ item, onChanged }) {
  const [showReplies, setShowReplies] = useState(false)
  const [reopening, setReopening] = useState(false)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [detail, setDetail] = useState(null)
  const [detailFailure, setDetailFailure] = useState(null)

  // Chi tiết có các mục trả lời và lệnh được phép (allowedCommands, đổi api 2026-11-05); đọc khi Đã phản hồi hoặc mở xem
  const needDetail = item.status === 'REPLIED' || showReplies
  useEffect(() => {
    if (!needDetail) return
    let cancelled = false
    getFeedbackById({ path: { id: item.id } })
      .then(({ data }) => !cancelled && setDetail(data))
      .catch((e) => !cancelled && setDetailFailure(problemOf(e)))
    return () => {
      cancelled = true
    }
  }, [item.id, item.status, needDetail])
  const can = (name) => detail?.allowedCommands?.includes(name)

  async function run(command, body, message) {
    setFailure(null)
    try {
      await postFeedbackByIdByCommand({
        path: { id: item.id, command },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body,
      })
      setReopening(false)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    }
  }

  function reopen(event) {
    event.preventDefault()
    setSubmitted(true)
    if (reason.trim()) run('reopen', { reason: reason.trim() }, 'Đã mở lại phản hồi. Viện sẽ trả lời theo hạn mới.')
  }

  return (
    <li className="flex flex-col gap-2 border-t py-3 first:border-t-0 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-caption text-muted-foreground">
          {typeLabel(item.type)}, {groupLabel(item.group).toLowerCase()}, hạn trả lời {formatDateTime(item.dueAt)}
        </span>
        <StatusBadge status={FEEDBACK_STATUS[item.status] ?? item.status} />
      </div>
      <span className="whitespace-pre-line">{item.content}</span>
      {(item.status === 'REPLIED' || item.status === 'CLOSED') && (
        <Button
          variant="ghost"
          size="sm"
          className="self-start"
          aria-expanded={showReplies}
          onClick={() => setShowReplies(!showReplies)}
        >
          {showReplies ? 'Ẩn trả lời' : 'Xem trả lời'}
        </Button>
      )}
      {showReplies && !detailFailure && <Replies detail={detail} />}
      <ProblemAlert problem={detailFailure} />
      <ProblemAlert problem={failure} />
      {!reopening && (can('agree-to-close') || can('reopen')) && (
        <div className="flex flex-wrap gap-2">
          {can('agree-to-close') && (
            <Button variant="outline" size="sm" onClick={() => run('agree-to-close', {}, 'Đã đóng phản hồi.')}>
              Đồng ý đóng
            </Button>
          )}
          {can('reopen') && (
            <Button variant="ghost" size="sm" onClick={() => setReopening(true)}>
              Mở lại
            </Button>
          )}
        </div>
      )}
      {reopening && (
        <form onSubmit={reopen} noValidate className="flex flex-col gap-2">
          <Field data-invalid={(submitted && !reason.trim()) || undefined}>
            <FieldLabel htmlFor={`reopen-${item.id}`}>Lý do mở lại</FieldLabel>
            <Textarea
              id={`reopen-${item.id}`}
              rows={2}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              aria-invalid={(submitted && !reason.trim()) || undefined}
            />
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

export function LiveFeedbackPage({ viewer }) {
  const [items, setItems] = useState(null)
  const [failure, setFailure] = useState(null)
  const [notice, setNotice] = useState('')

  const load = useCallback(() => {
    getFeedback({ query: {} })
      .then(({ data }) => {
        setItems(data.filter((f) => f.residentId === viewer.residentId))
        setFailure(null)
      })
      .catch((e) => {
        setItems([])
        setFailure(problemOf(e))
      })
  }, [viewer.residentId])

  useEffect(() => {
    load()
  }, [load])

  function changed(message) {
    setNotice(message)
    load()
  }

  return (
    <>
      <PageHeader
        title="Phản hồi"
        subtitle={`Góp ý, khiếu nại hoặc hỏi viện về việc chăm sóc ${viewer.residentName ?? 'người cao tuổi'}.`}
      />
      <SubmitForm viewer={viewer} onSent={() => changed('Đã gửi phản hồi. Viện sẽ báo bạn khi có trả lời.')} />
      <p role="status" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {notice}
      </p>
      <PortalCard title="Phản hồi của bạn">
        <ProblemAlert problem={failure} />
        {items === null ? (
          <Skeleton className="h-16 w-full" />
        ) : items.length === 0 ? (
          !failure && <p className="text-muted-foreground">Bạn chưa gửi phản hồi nào.</p>
        ) : (
          <ul className="flex flex-col">
            {items.map((f) => (
              <FeedbackRow key={f.id} item={f} onChanged={changed} />
            ))}
          </ul>
        )}
      </PortalCard>
    </>
  )
}
