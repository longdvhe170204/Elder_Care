import { useCallback, useEffect, useState } from 'react'
import {
  ApiError,
  getApprovalRequestsById,
  getAuditLogsEntitiesByEntityTypeByEntityId,
  postApprovalRequestsByIdApprove,
  postApprovalRequestsByIdCancel,
  postApprovalRequestsByIdReject,
  postApprovalRequestsByIdSubmit,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { ACTION_LABELS, actorLabel, statusLabel, typeLabel } from './approvalStatus'

// Chi tiết một yêu cầu phê duyệt (000 T097; US5; FR-031 → FR-040). Nút theo `allowedCommands` backend trả;
// backend vẫn kiểm mọi lệnh. Ý kiến bắt buộc khi từ chối; tự duyệt thiếu ý kiến thì backend trả 422, báo tại ô.
// Sửa nội dung nháp (edit-content) chưa có trên màn: nội dung theo schema của module sở hữu loại yêu cầu.

const TEXT_MAX = 2000

function valueText(value) {
  if (value === null || value === undefined) return 'trống'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function EffectiveLine({ request }) {
  if (request.status === 'APPLIED' && request.actualEffectiveDate) {
    return (
      <p className="font-medium">
        {`Áp dụng ngày ${formatDate(request.actualEffectiveDate)}${request.catchUpApplied ? ' (áp dụng bù)' : ''}`}
      </p>
    )
  }
  if (request.status === 'APPROVED') {
    return <p className="font-medium">{`Chờ hiệu lực ngày ${formatDate(request.requestedEffectiveDate)}`}</p>
  }
  return null
}

function Timeline({ request }) {
  const [items, setItems] = useState(null)

  useEffect(() => {
    let active = true
    getAuditLogsEntitiesByEntityTypeByEntityId({
      path: { entityType: 'M15.APPROVAL_REQUEST', entityId: String(request.id) },
    })
      .then(({ data }) => active && setItems(data.items))
      // Không có quyền xem nhật ký của yêu cầu (403) hoặc không tìm thấy: ẩn mục
      .catch(() => active && setItems(null))
    return () => {
      active = false
    }
  }, [request.id, request.version])

  if (!items || items.length === 0) return null
  return (
    <section className="flex flex-col gap-2">
      <h3 className="type-caption text-muted-foreground">Diễn biến</h3>
      <ul aria-label="Diễn biến" className="flex flex-col gap-2 border-l-2 pl-3">
        {items.map((entry) => (
          <li key={entry.id} className="flex flex-col">
            <span>{ACTION_LABELS[entry.action] ?? entry.action}</span>
            <span className="type-caption text-muted-foreground tabular-nums">
              {formatDateTime(entry.occurredAt)}, {actorLabel(entry.actor)}
            </span>
            {entry.reason && <span className="type-caption">{entry.reason}</span>}
          </li>
        ))}
      </ul>
    </section>
  )
}

export function ApprovalRequestDetail({ id, onChanged }) {
  const [state, setState] = useState({ status: 'loading' })
  const [comment, setComment] = useState('')
  const [cancelling, setCancelling] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  // Tải lại sau 409/412 giữ nguyên khung, không hiện trạng thái đang tải
  const load = useCallback(
    async (initial) => {
      if (initial) setState({ status: 'loading' })
      try {
        const { data, etag } = await getApprovalRequestsById({ path: { id } })
        setState({ status: 'ready', request: data, etag })
        if (!initial) onChanged(data)
      } catch (error) {
        setState({ status: 'error', problem: problemOf(error) })
      }
    },
    [id, onChanged],
  )

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải chi tiết (kèm ETag) khi mở khung
    load(true)
  }, [load])

  if (state.status === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2 px-4 pt-14">
        <span className="sr-only">Đang tải yêu cầu</span>
        <Skeleton className="h-8" />
        <Skeleton className="h-24" />
      </div>
    )
  }
  if (state.status === 'error') {
    return (
      <div className="px-4 pt-14">
        <ProblemAlert problem={state.problem} />
      </div>
    )
  }

  const { request, etag } = state
  const allowed = request.allowedCommands ?? []
  const canDecide = allowed.includes('approve') || allowed.includes('reject')

  async function run(command, body, field) {
    setFailure(null)
    setErrors({})
    setBusy(true)
    try {
      const { data, etag: nextEtag } = await command({ path: { id }, headers: { 'If-Match': etag }, body })
      setState({ status: 'ready', request: data, etag: nextEtag })
      setComment('')
      setCancelReason('')
      setCancelling(false)
      onChanged(data)
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422 && field) {
        setErrors({ [field]: p.detail || p.title })
      } else {
        setFailure(p)
        // Người khác đã xử lý (409 ALREADY_PROCESSED) hoặc đã đổi (412): tải lại để thấy trạng thái mới
        if (error instanceof ApiError && (error.status === 409 || error.status === 412)) load(false)
      }
    } finally {
      setBusy(false)
    }
  }

  function approve() {
    run(postApprovalRequestsByIdApprove, comment.trim() ? { comment } : {}, 'comment')
  }

  function reject() {
    if (!comment.trim()) {
      setErrors({ comment: 'Nhập ý kiến khi từ chối' })
      return
    }
    run(postApprovalRequestsByIdReject, { comment }, 'comment')
  }

  function cancel() {
    if (!cancelReason.trim()) {
      setErrors({ cancelReason: 'Nhập lý do hủy' })
      return
    }
    run(postApprovalRequestsByIdCancel, { reason: cancelReason }, 'cancelReason')
  }

  const content = Object.entries(request.content ?? {})

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title tabular-nums">{request.displayCode}</SheetTitle>
        <SheetDescription>{typeLabel(request.type)}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6 type-body-sm">
        <div className="flex flex-col gap-2">
          <StatusBadge status={statusLabel(request.status)} className="self-start" />
          <EffectiveLine request={request} />
          {request.closingReason && <p>{`Lý do đóng: ${request.closingReason}`}</p>}
        </div>

        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
          <dt className="text-muted-foreground">Người yêu cầu</dt>
          <dd>{actorLabel(request.requester)}</dd>
          <dt className="text-muted-foreground">Lập lúc</dt>
          <dd className="tabular-nums">{formatDateTime(request.createdAt)}</dd>
          <dt className="text-muted-foreground">Hiệu lực đề nghị</dt>
          <dd className="tabular-nums">{formatDate(request.requestedEffectiveDate)}</dd>
          <dt className="text-muted-foreground">Lý do</dt>
          <dd>{request.reason}</dd>
          {request.decidedAt && (
            <>
              <dt className="text-muted-foreground">Quyết định</dt>
              <dd>
                {actorLabel(request.decidedBy)}, {formatDateTime(request.decidedAt)}
                {request.selfApproved && ' (tự duyệt)'}
              </dd>
            </>
          )}
          {request.decisionComment && (
            <>
              <dt className="text-muted-foreground">Ý kiến</dt>
              <dd>{request.decisionComment}</dd>
            </>
          )}
        </dl>

        {content.length > 0 && (
          <section className="flex flex-col gap-2">
            <h3 className="type-caption text-muted-foreground">Nội dung</h3>
            <ul className="flex flex-col gap-1">
              {content.map(([field, value]) => (
                <li key={field} className="break-words">
                  <span className="text-muted-foreground">{field}:</span> <span>{valueText(value)}</span>
                </li>
              ))}
            </ul>
          </section>
        )}

        <ProblemAlert problem={failure} />

        {canDecide && (
          <section className="flex flex-col gap-3">
            <Field data-invalid={!!errors.comment || undefined}>
              <FieldLabel htmlFor="approval-comment">Ý kiến</FieldLabel>
              <Textarea
                id="approval-comment"
                rows={3}
                maxLength={TEXT_MAX}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                aria-invalid={!!errors.comment || undefined}
              />
              {errors.comment ? (
                <FieldError>{errors.comment}</FieldError>
              ) : (
                <FieldDescription>Bắt buộc khi từ chối, hoặc khi duyệt yêu cầu do chính mình lập.</FieldDescription>
              )}
            </Field>
            <div className="flex flex-wrap gap-2">
              {allowed.includes('approve') && (
                <Button className="h-11" disabled={busy} onClick={approve}>
                  Duyệt
                </Button>
              )}
              {allowed.includes('reject') && (
                <Button variant="outline" className="h-11" disabled={busy} onClick={reject}>
                  Từ chối
                </Button>
              )}
            </div>
          </section>
        )}

        {(allowed.includes('submit') || allowed.includes('cancel')) && (
          <div className="flex flex-wrap gap-2">
            {allowed.includes('submit') && (
              <Button className="h-11" disabled={busy} onClick={() => run(postApprovalRequestsByIdSubmit)}>
                Gửi duyệt
              </Button>
            )}
            {allowed.includes('cancel') && !cancelling && (
              <Button variant="outline" className="h-11" disabled={busy} onClick={() => setCancelling(true)}>
                Hủy yêu cầu
              </Button>
            )}
          </div>
        )}

        {cancelling && (
          <section className="flex flex-col gap-3 rounded-xl border p-3">
            <Field data-invalid={!!errors.cancelReason || undefined}>
              <FieldLabel htmlFor="approval-cancel-reason">Lý do hủy</FieldLabel>
              <Textarea
                id="approval-cancel-reason"
                rows={3}
                maxLength={TEXT_MAX}
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                aria-invalid={!!errors.cancelReason || undefined}
              />
              {errors.cancelReason && <FieldError>{errors.cancelReason}</FieldError>}
            </Field>
            <div className="flex flex-wrap gap-2">
              <Button variant="destructive" className="h-11" disabled={busy} onClick={cancel}>
                Xác nhận hủy
              </Button>
              <Button variant="ghost" className="h-11" onClick={() => setCancelling(false)}>
                Không hủy
              </Button>
            </div>
          </section>
        )}

        <Timeline request={request} />
      </div>
    </>
  )
}
