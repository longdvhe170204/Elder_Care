import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getFeedback,
  getFeedbackById,
  getFeedbackByIdAssignableStaff,
  getResidentsById,
  postFeedbackByIdByCommand,
} from '@/api'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ASSIGNEE_ROLES, FEEDBACK_STATUS, ITEM_LABELS, PRIORITY, groupLabel, typeLabel } from './feedbackLabels'

// Phản hồi của người thân (012 T030; US6, FR-058 → FR-066, BR-M10-05; Phụ lục 27 dòng "Phản hồi": Trưởng tầng, Điều
// dưỡng, Hành chính trong phạm vi, Quản lý viện xem mọi phản hồi và danh sách quá hạn). Bảng, bấm dòng mở khung chi tiết
// (DESIGN.md 5, 7.2). Nút lệnh theo allowedCommands của chi tiết (đổi api 2026-11-05); backend vẫn kiểm lại ở lệnh (403
// hiện trong khung).

const FILTERS = [
  ['all', 'Tất cả'],
  ['NEW', 'Mới'],
  ['IN_PROGRESS', 'Đang xử lý'],
  ['REOPENED', 'Mở lại'],
  ['REPLIED', 'Đã phản hồi'],
  ['CLOSED', 'Đóng'],
  ['overdue', 'Quá hạn'],
]

// Quá hạn vẫn giữ trạng thái để biết đã có người nhận xử lý chưa (bố cục duyệt 2026-11-05)
function FeedbackStatus({ f }) {
  const status = FEEDBACK_STATUS[f.status] ?? f.status
  return f.overdue ? <Badge variant="urgent">{status}, quá hạn</Badge> : <StatusBadge status={status} />
}

// Quá hạn lên đầu, rồi theo hạn xử lý gần nhất (DESIGN.md 11: việc gấp nổi lên bằng thứ tự)
function byUrgency(a, b) {
  if (a.overdue !== b.overdue) return a.overdue ? -1 : 1
  return new Date(a.dueAt) - new Date(b.dueAt)
}

async function command(id, name, body) {
  const { data } = await postFeedbackByIdByCommand({
    path: { id, command: name },
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    body,
  })
  return data
}

function ReasonForm({ label, submitLabel, children, onSubmit, onCancel }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  return (
    <form
      noValidate
      className="flex flex-col gap-3 rounded-lg border p-3"
      onSubmit={(e) => {
        e.preventDefault()
        if (!reason.trim()) {
          setError('Nhập lý do')
          return
        }
        onSubmit(reason.trim())
      }}
    >
      <FieldGroup>
        {children}
        <Field data-invalid={!!error || undefined}>
          <FieldLabel htmlFor="feedback-reason">Lý do</FieldLabel>
          <Textarea
            id="feedback-reason"
            rows={2}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!error || undefined}
          />
          {error && <FieldError>{error}</FieldError>}
        </Field>
      </FieldGroup>
      <span className="flex flex-wrap gap-2">
        <Button type="submit" className="h-11">
          {submitLabel}
        </Button>
        <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
          Quay lại
        </Button>
      </span>
      <span className="sr-only">{label}</span>
    </form>
  )
}

function ReplyForm({ onSubmit }) {
  const [direction, setDirection] = useState('')
  const [content, setContent] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const missingDirection = submitted && !direction.trim()
  const missingContent = submitted && !content.trim()
  return (
    <form
      noValidate
      className="flex flex-col gap-3"
      onSubmit={(e) => {
        e.preventDefault()
        setSubmitted(true)
        if (direction.trim() && content.trim())
          onSubmit({ handlingDirection: direction.trim(), content: content.trim() })
      }}
    >
      <FieldGroup>
        <Field data-invalid={missingDirection || undefined}>
          <FieldLabel htmlFor="feedback-direction">Hướng xử lý</FieldLabel>
          <Textarea
            id="feedback-direction"
            rows={2}
            value={direction}
            onChange={(e) => setDirection(e.target.value)}
            aria-invalid={missingDirection || undefined}
          />
          {missingDirection && <FieldError>Nhập hướng xử lý</FieldError>}
        </Field>
        <Field data-invalid={missingContent || undefined}>
          <FieldLabel htmlFor="feedback-reply">Nội dung trả lời</FieldLabel>
          <Textarea
            id="feedback-reply"
            rows={3}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            aria-invalid={missingContent || undefined}
          />
          {missingContent && <FieldError>Nhập nội dung trả lời gửi người thân</FieldError>}
        </Field>
      </FieldGroup>
      <Button type="submit" className="h-11">
        Gửi trả lời
      </Button>
    </form>
  )
}

function FeedbackDetail({ id, residentName, onChanged, Title, Description }) {
  const [f, setF] = useState(null)
  const [failure, setFailure] = useState(null)
  const [mode, setMode] = useState(null)
  const [done, setDone] = useState('')

  useEffect(() => {
    let cancelled = false
    getFeedbackById({ path: { id } })
      .then(({ data }) => !cancelled && setF(data))
      .catch((e) => !cancelled && setFailure(problemOf(e)))
    return () => {
      cancelled = true
    }
  }, [id])

  async function run(name, body, message) {
    setFailure(null)
    try {
      const data = await command(id, name, body)
      setF(data)
      setMode(null)
      setDone(message)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    }
  }

  if (!f) {
    return (
      <div className="flex flex-col gap-3 p-4">
        <Title className="type-title">Chi tiết phản hồi</Title>
        {failure ? <ProblemAlert problem={failure} /> : <Skeleton className="h-40 w-full" />}
      </div>
    )
  }

  const can = (name) => f.allowedCommands?.includes(name)
  const healthWarning = f.group === 'HEALTH_MEDICATION' && f.senderHealthRightEffective === false
  return (
    <div className="flex flex-col gap-4 p-4">
      <div className="flex flex-col gap-1">
        <span>
          <FeedbackStatus f={f} />
        </span>
        <Title className="type-title">
          {typeLabel(f.type)}, {groupLabel(f.group).toLowerCase()}
        </Title>
        <Description className="type-body-sm text-muted-foreground">
          {residentName ?? `Người cao tuổi mã ${f.residentId}`}. {f.familyMemberName ?? 'Người thân'} gửi{' '}
          {formatDateTime(f.submittedAt)}
          {f.source === 'COUNTER' ? ', Hành chính ghi tại quầy' : ', qua cổng'}.
        </Description>
      </div>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 type-body-sm">
        <dt className="text-muted-foreground">Mức</dt>
        <dd>{PRIORITY[f.priority] ?? f.priority}</dd>
        <dt className="text-muted-foreground">Hạn xử lý</dt>
        <dd className={f.overdue ? 'text-destructive tabular-nums' : 'tabular-nums'}>{formatDateTime(f.dueAt)}</dd>
        <dt className="text-muted-foreground">Người phụ trách</dt>
        <dd>{f.assignee}</dd>
      </dl>
      <ol aria-label="Nội dung và trả lời" className="flex flex-col gap-3">
        {f.items.map((item) => (
          <li key={item.id} className="flex flex-col gap-0.5 border-t pt-2 first:border-t-0 first:pt-0">
            <span className="type-caption text-muted-foreground">
              {ITEM_LABELS[item.type] ?? item.type}, {formatDateTime(item.at)}
            </span>
            <span className="type-body-sm whitespace-pre-line">{item.content}</span>
          </li>
        ))}
      </ol>
      {/* Khung Sheet chặn phần ngoài với trình đọc màn hình nên báo kết quả ngay trong khung */}
      <p role="status" className="empty:hidden type-body-sm">
        {done}
      </p>
      <ProblemAlert problem={failure} />
      {can('start-handling') && (
        <Button className="h-11" onClick={() => run('start-handling', {}, 'Đã nhận xử lý phản hồi')}>
          Nhận xử lý
        </Button>
      )}
      {can('reply') && (
        <>
          {healthWarning && (
            <Alert variant="attention">
              <AlertDescription>
                Người gửi không có quyền xem sức khỏe: trả lời chỉ ghi thông tin chung, ví dụ mời liên hệ trực tiếp hoặc
                đề nghị người đại diện cấp quyền.
              </AlertDescription>
            </Alert>
          )}
          <ReplyForm onSubmit={(body) => run('reply', body, 'Đã gửi trả lời cho người thân')} />
        </>
      )}
      {mode === null && (can('change-priority') || can('reassign') || can('add-note')) && (
        <span className="flex flex-wrap gap-2">
          {can('change-priority') && (
            <Button variant="outline" className="h-11" onClick={() => setMode('priority')}>
              Đổi mức
            </Button>
          )}
          {can('reassign') && (
            <Button variant="outline" className="h-11" onClick={() => setMode('reassign')}>
              Giao lại
            </Button>
          )}
          {can('add-note') && (
            <Button variant="ghost" className="h-11" onClick={() => setMode('note')}>
              Ghi chú
            </Button>
          )}
        </span>
      )}
      {mode === 'priority' && (
        <PriorityForm
          current={f.priority}
          onCancel={() => setMode(null)}
          onSubmit={(body) => run('change-priority', body, 'Đã đổi mức, hạn tính lại')}
        />
      )}
      {mode === 'reassign' && (
        <ReassignForm
          id={id}
          onCancel={() => setMode(null)}
          onSubmit={(body) => run('reassign', body, 'Đã giao lại phản hồi')}
        />
      )}
      {mode === 'note' && (
        <NoteForm onCancel={() => setMode(null)} onSubmit={(content) => run('add-note', { content }, 'Đã ghi chú')} />
      )}
    </div>
  )
}

function PriorityForm({ current, onSubmit, onCancel }) {
  const [priority, setPriority] = useState(current === 'HIGH' ? 'NORMAL' : 'HIGH')
  return (
    <ReasonForm
      label="Đổi mức ưu tiên"
      submitLabel="Lưu mức mới"
      onCancel={onCancel}
      onSubmit={(reason) => onSubmit({ priority, reason })}
    >
      <Field>
        <FieldLabel>Mức mới</FieldLabel>
        <ToggleGroup
          variant="outline"
          value={[priority]}
          onValueChange={(n) => n[0] && setPriority(n[0])}
          aria-label="Mức mới"
        >
          {Object.entries(PRIORITY).map(([v, label]) => (
            <ToggleGroupItem key={v} value={v} className="h-11 px-4">
              {label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </Field>
    </ReasonForm>
  )
}

// Người nhận giao lại: Trưởng tầng, Điều dưỡng, Hành chính có phạm vi của người cao tuổi (FR-060; đổi api 2026-11-05)
function ReassignForm({ id, onSubmit, onCancel }) {
  const [staff, setStaff] = useState(null)
  const [failure, setFailure] = useState(null)
  const [assignee, setAssignee] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    getFeedbackByIdAssignableStaff({ path: { id } })
      .then(({ data }) => !cancelled && setStaff(data))
      .catch((e) => !cancelled && setFailure(problemOf(e)))
    return () => {
      cancelled = true
    }
  }, [id])

  return (
    <ReasonForm
      label="Giao lại phản hồi"
      submitLabel="Giao lại"
      onCancel={onCancel}
      onSubmit={(reason) => {
        if (!assignee) setError('Chọn người được giao')
        else onSubmit({ assigneeId: Number(assignee), reason })
      }}
    >
      <ProblemAlert problem={failure} />
      <Field data-invalid={!!error || undefined}>
        <FieldLabel htmlFor="feedback-assignee">Người được giao</FieldLabel>
        <select
          id="feedback-assignee"
          value={assignee}
          onChange={(e) => {
            setAssignee(e.target.value)
            setError('')
          }}
          aria-invalid={!!error || undefined}
          className="h-11 w-full rounded-md border border-input bg-transparent px-3"
        >
          <option value="">{staff === null && !failure ? 'Đang tải…' : 'Chọn người'}</option>
          {staff?.map((s) => (
            <option key={s.accountId} value={s.accountId}>
              {s.fullName ?? `Tài khoản ${s.accountId}`}, {ASSIGNEE_ROLES[s.role] ?? s.role}
            </option>
          ))}
        </select>
        {error && <FieldError>{error}</FieldError>}
      </Field>
    </ReasonForm>
  )
}

function NoteForm({ onSubmit, onCancel }) {
  const [content, setContent] = useState('')
  const [error, setError] = useState('')
  return (
    <form
      noValidate
      className="flex flex-col gap-3 rounded-lg border p-3"
      onSubmit={(e) => {
        e.preventDefault()
        if (!content.trim()) setError('Nhập nội dung ghi chú')
        else onSubmit(content.trim())
      }}
    >
      <Field data-invalid={!!error || undefined}>
        <FieldLabel htmlFor="feedback-note">Ghi chú bổ sung</FieldLabel>
        <Textarea
          id="feedback-note"
          rows={2}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          aria-invalid={!!error || undefined}
        />
        {error && <FieldError>{error}</FieldError>}
      </Field>
      <span className="flex flex-wrap gap-2">
        <Button type="submit" className="h-11">
          Lưu ghi chú
        </Button>
        <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
          Quay lại
        </Button>
      </span>
    </form>
  )
}

export function FeedbackPage() {
  const [filter, setFilter] = useState('all')
  const [rows, setRows] = useState(null)
  const [failure, setFailure] = useState(null)
  const [opened, setOpened] = useState(null)
  const [names, setNames] = useState({})
  const [announcement, setAnnouncement] = useState('')

  const requested = useRef(new Set())

  // Tên người cao tuổi đọc từ hồ sơ (api-001); không đọc được thì hiện mã
  const loadNames = useCallback((data) => {
    for (const id of new Set(data.map((f) => f.residentId))) {
      if (requested.current.has(id)) continue
      requested.current.add(id)
      getResidentsById({ path: { id } })
        .then(({ data: r }) => setNames((n) => ({ ...n, [id]: r.personalInfo?.fullName ?? null })))
        .catch(() => setNames((n) => ({ ...n, [id]: null })))
    }
  }, [])

  const load = useCallback(
    (next) =>
      getFeedback({ query: next === 'overdue' ? { overdue: true } : next === 'all' ? {} : { status: next } })
        .then(({ data }) => {
          setRows([...data].sort(byUrgency))
          setFailure(null)
          loadNames(data)
        })
        .catch((e) => {
          setRows([])
          setFailure(problemOf(e))
        }),
    [loadNames],
  )

  useEffect(() => {
    load(filter)
  }, [filter, load])

  if (failure?.status === 403) return <PermissionDeniedState label="Phản hồi" />

  const current = rows?.find((f) => f.id === opened)
  return (
    <DetailLayout
      open={!!current}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết phản hồi"
      renderDetail={(titles) => (
        <FeedbackDetail
          key={opened}
          id={opened}
          residentName={names[current.residentId]}
          onChanged={(message) => {
            setAnnouncement(message)
            load(filter)
          }}
          {...titles}
        />
      )}
    >
      <PageHeader
        title="Phản hồi của người thân"
        subtitle="Góp ý, khiếu nại, hỏi đáp trong phạm vi của bạn; trả lời trước hạn xử lý."
      />
      <ToggleGroup
        variant="filter"
        size="lg"
        value={[filter]}
        onValueChange={(n) => n[0] && setFilter(n[0])}
        aria-label="Lọc phản hồi"
      >
        {FILTERS.map(([v, label]) => (
          <ToggleGroupItem key={v} value={v} className="h-11 px-4">
            {label}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {rows === null ? (
        <Skeleton className="h-24 w-full" />
      ) : rows.length === 0 ? (
        !failure && <p className="type-body-sm text-muted-foreground">Không có phản hồi nào trong nhóm này.</p>
      ) : (
        <Card className="py-0">
          <Table aria-label="Phản hồi">
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="pl-4">Nội dung</TableHead>
                <TableHead>Người cao tuổi</TableHead>
                <TableHead>Người gửi</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Hạn xử lý</TableHead>
                <TableHead>Người phụ trách</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((f) => (
                <TableRow key={f.id} className={f.id === opened ? 'bg-accent/40' : undefined}>
                  <TableCell className="max-w-80 whitespace-normal pl-4">
                    <button
                      type="button"
                      className="line-clamp-2 text-left underline-offset-4 hover:underline"
                      aria-pressed={f.id === opened}
                      onClick={() => setOpened(f.id)}
                    >
                      {f.content}
                    </button>
                  </TableCell>
                  <TableCell>{names[f.residentId] ?? `Mã ${f.residentId}`}</TableCell>
                  <TableCell className="whitespace-normal">
                    {f.familyMemberName}
                    <span className="block type-caption text-muted-foreground tabular-nums">
                      {formatDateTime(f.submittedAt)}
                    </span>
                  </TableCell>
                  <TableCell className="whitespace-normal">
                    {typeLabel(f.type)}, {groupLabel(f.group).toLowerCase()}
                  </TableCell>
                  <TableCell className={f.overdue ? 'text-destructive tabular-nums' : 'tabular-nums'}>
                    {formatDateTime(f.dueAt)}
                  </TableCell>
                  <TableCell>{f.assignee}</TableCell>
                  <TableCell className="pr-4">
                    <FeedbackStatus f={f} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </DetailLayout>
  )
}
