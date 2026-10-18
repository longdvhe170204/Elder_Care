import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import {
  DEMO_HANDOVER_AFTER_LOCK,
  DEMO_HANDOVER_DRAFT_ITEMS,
  DEMO_HANDOVER_HISTORY,
  DEMO_HANDOVER_SCOPE,
  DEMO_HANDOVER_SOURCE_GROUPS,
  DEMO_HANDOVER_SOURCES,
} from '@/common/demoData'
import { cn } from '@/lib/utils'

// Bản mẫu màn Bàn giao ca (M17; spec 008 User Story 1, FR-038 → FR-047). Người bàn giao là Người phụ trách ca
// (bản mẫu: vai trò Điều dưỡng); người xác nhận là người phụ trách ca sau hoặc Trưởng tầng của phạm vi (bản mẫu:
// vai trò Trưởng tầng). Hệ thống không lập sẵn mục; người bàn giao tự chọn từ bản ghi nguồn (Q-306, Q-307).
// Trạng thái thật và các tác động khi xác nhận do backend thực hiện.

const GIVER_ROLE = 'NURSING_STAFF'
const CONFIRM_ROLE = 'FLOOR_MANAGER'
const sourceOf = (id) => DEMO_HANDOVER_SOURCES.find((s) => s.id === id)
const groupLabel = (key) => DEMO_HANDOVER_SOURCE_GROUPS.find((g) => g.key === key).label

function SourceLine({ source }) {
  return (
    <span className="flex min-w-0 flex-col">
      <span className="type-label">{source.resident ?? 'Chung của tầng'}</span>
      <span className="text-muted-foreground">{source.text}</span>
    </span>
  )
}

// Bên trái: bản ghi nguồn đang mở của phạm vi, theo nhóm FR-039 (a) → (g)
function SourcesPanel({ items, onAdd }) {
  return (
    <Card className="h-fit">
      <CardHeader>
        <CardTitle className="type-card-title">Bản ghi nguồn đang mở</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4 type-body-sm">
        {DEMO_HANDOVER_SOURCE_GROUPS.map((g) => {
          const sources = DEMO_HANDOVER_SOURCES.filter((s) => s.group === g.key)
          return (
            <section key={g.key} className="flex flex-col gap-2">
              <h3 className="type-caption text-muted-foreground">
                {g.label} <span className="tabular-nums">({sources.length})</span>
              </h3>
              {sources.length === 0 ? (
                <p className="text-muted-foreground">Không có.</p>
              ) : (
                <ul className="flex flex-col">
                  {sources.map((s) => {
                    const added = items.some((i) => i.sourceId === s.id)
                    return (
                      <li key={s.id} className="flex items-center gap-3 border-t py-2 first:border-t-0 first:pt-0">
                        <SourceLine source={s} />
                        <span className="ml-auto flex shrink-0 items-center gap-2">
                          <StatusBadge status={s.currentStatus ?? s.status} />
                          {added ? (
                            <span className="w-14 type-caption text-muted-foreground">Đã thêm</span>
                          ) : (
                            <Button variant="outline" size="sm" className="w-14" onClick={() => onAdd(s.id)}>
                              Thêm
                            </Button>
                          )}
                        </span>
                      </li>
                    )
                  })}
                </ul>
              )}
            </section>
          )
        })}
      </CardContent>
    </Card>
  )
}

function DraftPanel({ items, setItems, general, setGeneral, followUp, setFollowUp, onComplete }) {
  const [submitted, setSubmitted] = useState(false)
  const missing = [
    ...items
      .filter((i) => sourceOf(i.sourceId).serious && !i.handled && !i.note.trim())
      .map((i) => `ghi chú cho ${sourceOf(i.sourceId).text} của ${sourceOf(i.sourceId).resident}`),
    ...(general.trim() ? [] : ['nhận định chung']),
  ]
  const update = (id, patch) => setItems(items.map((i) => (i.sourceId === id ? { ...i, ...patch } : i)))

  function complete(event) {
    event.preventDefault()
    setSubmitted(true)
    if (missing.length === 0) onComplete()
  }

  return (
    <Card className="h-fit">
      <CardHeader>
        <CardTitle className="type-card-title">Nội dung bàn giao</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={complete} noValidate className="flex flex-col gap-4 type-body-sm">
          {items.length === 0 && <p className="text-muted-foreground">Chưa có mục. Chọn "Thêm" ở danh sách bản ghi nguồn.</p>}
          <ul className="flex flex-col gap-3">
            {items.map((i) => {
              const s = sourceOf(i.sourceId)
              const closed = s.currentStatus && s.currentStatus !== s.status
              const noteMissing = submitted && s.serious && !i.handled && !i.note.trim()
              return (
                <li key={i.sourceId} className={cn('flex flex-col gap-2 rounded-xl border p-3', i.handled && 'bg-muted')}>
                  <div className="flex items-start gap-3">
                    <span className="flex min-w-0 flex-col gap-0.5">
                      <span className="type-caption text-muted-foreground">{groupLabel(s.group)}</span>
                      <SourceLine source={s} />
                    </span>
                    <span className="ml-auto flex shrink-0 flex-col items-end gap-1">
                      <StatusBadge status={s.currentStatus ?? s.status} />
                      {s.serious && !i.handled && <span className="type-caption text-warning-foreground">Cần ghi chú</span>}
                    </span>
                  </div>
                  {/* Nguồn đã xử lý xong: người bàn giao ghi "đã xử lý" hoặc bỏ mục (FR-040) */}
                  {closed && (
                    <p className="text-muted-foreground">
                      Nguồn đã chuyển {s.currentStatus.toLowerCase()} sau khi thêm vào bàn giao.
                    </p>
                  )}
                  <Field data-invalid={noteMissing || undefined}>
                    <FieldLabel htmlFor={`note-${s.id}`} className="sr-only">
                      Ghi chú cho mục {s.text}
                    </FieldLabel>
                    <Textarea
                      id={`note-${s.id}`}
                      rows={2}
                      placeholder={s.serious ? 'Ghi chú (bắt buộc với mục nghiêm trọng)' : 'Ghi chú'}
                      value={i.note}
                      onChange={(e) => update(s.id, { note: e.target.value })}
                      aria-invalid={noteMissing || undefined}
                    />
                    {noteMissing && <FieldError>Mục nghiêm trọng cần ghi chú</FieldError>}
                  </Field>
                  <div className="flex flex-wrap items-center justify-end gap-2">
                    {closed && !i.handled && (
                      <Button type="button" variant="outline" size="sm" onClick={() => update(s.id, { handled: true })}>
                        Ghi đã xử lý
                      </Button>
                    )}
                    {i.handled && <span className="type-caption text-muted-foreground">Đã ghi đã xử lý</span>}
                    {s.keep ? (
                      <span className="type-caption text-muted-foreground">Mục này không bỏ được</span>
                    ) : (
                      <Button type="button" variant="ghost" size="sm" onClick={() => setItems(items.filter((x) => x.sourceId !== s.id))}>
                        Bỏ mục
                      </Button>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>
          <Field>
            <FieldLabel htmlFor="follow-up">Vấn đề cần theo dõi, nhận định theo người cao tuổi</FieldLabel>
            <Textarea id="follow-up" rows={3} value={followUp} onChange={(e) => setFollowUp(e.target.value)} />
          </Field>
          <Field data-invalid={(submitted && !general.trim()) || undefined}>
            <FieldLabel htmlFor="general">Nhận định chung</FieldLabel>
            <Textarea id="general" rows={3} value={general} onChange={(e) => setGeneral(e.target.value)} aria-invalid={(submitted && !general.trim()) || undefined} />
            {submitted && !general.trim() ? <FieldError>Nhập nhận định chung</FieldError> : <FieldDescription>Bắt buộc trước khi hoàn tất.</FieldDescription>}
          </Field>
          {submitted && missing.length > 0 && (
            <div role="alert" className="rounded-lg bg-warning px-3 py-2 text-warning-foreground">
              <span className="type-label">Chưa hoàn tất được, còn thiếu:</span>
              <ul className="list-disc pl-5">
                {missing.map((m) => (
                  <li key={m}>{m}</li>
                ))}
              </ul>
            </div>
          )}
          <Button type="submit" className="h-11 self-end">
            Hoàn tất bàn giao
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

// Nội dung đã chốt: mỗi mục có trạng thái lúc chốt và trạng thái hiện tại nếu khác (FR-045)
function LockedContent({ items, general, followUp, status }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="type-card-title">Nội dung đã chốt</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4 type-body-sm">
        <ul className="flex flex-col">
          {items.map((i) => {
            const s = sourceOf(i.sourceId)
            const atLock = i.handled ? 'Đã xử lý' : (s.currentStatus ?? s.status)
            const now = status === 'Đã lập' && s.laterStatus
            return (
              <li key={i.sourceId} className="flex flex-col gap-1.5 border-t py-3 first:border-t-0 first:pt-0">
                <div className="flex items-start gap-3">
                  <span className="flex min-w-0 flex-col gap-0.5">
                    <span className="type-caption text-muted-foreground">{groupLabel(s.group)}</span>
                    <SourceLine source={s} />
                  </span>
                  <span className="ml-auto flex shrink-0 flex-col items-end gap-1 type-caption text-muted-foreground">
                    <span>
                      Lúc chốt: <StatusBadge status={atLock} />
                    </span>
                    {now && (
                      <span>
                        Nay: <StatusBadge status={now} />
                      </span>
                    )}
                  </span>
                </div>
                {i.note && <p>{i.note}</p>}
              </li>
            )
          })}
        </ul>
        {followUp && (
          <p>
            <span className="type-label">Vấn đề cần theo dõi: </span>
            {followUp}
          </p>
        )}
        <p>
          <span className="type-label">Nhận định chung: </span>
          {general}
        </p>
      </CardContent>
    </Card>
  )
}

function ConfirmPanel({ onConfirm, onComment }) {
  const [comment, setComment] = useState('')
  const [submitted, setSubmitted] = useState(false)
  return (
    <Card className="h-fit">
      <CardHeader>
        <CardTitle className="type-card-title">Xác nhận bàn giao</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4 type-body-sm">
        <section className="flex flex-col gap-2">
          <h3 className="type-caption text-muted-foreground">Bản ghi nguồn mới sau khi lập (chỉ để xem)</h3>
          <ul className="flex flex-col">
            {DEMO_HANDOVER_AFTER_LOCK.map((n) => (
              <li key={n.id} className="flex items-center gap-3 border-t py-2 first:border-t-0 first:pt-0">
                <SourceLine source={n} />
                <StatusBadge status={n.status} className="ml-auto" />
              </li>
            ))}
          </ul>
        </section>
        <Field data-invalid={(submitted && !comment.trim()) || undefined}>
          <FieldLabel htmlFor="comment">Ý kiến khi thấy thiếu sót</FieldLabel>
          <Textarea id="comment" rows={3} value={comment} onChange={(e) => setComment(e.target.value)} aria-invalid={(submitted && !comment.trim()) || undefined} />
          {submitted && !comment.trim() && <FieldError>Nhập nội dung ý kiến</FieldError>}
        </Field>
        <div className="flex flex-wrap justify-end gap-2">
          <Button
            variant="outline"
            className="h-11"
            onClick={() => {
              setSubmitted(true)
              if (comment.trim()) onComment(comment.trim())
            }}
          >
            Gửi ý kiến
          </Button>
          <Button className="h-11" onClick={onConfirm}>
            Xác nhận bàn giao
          </Button>
        </div>
        <p className="type-caption text-muted-foreground">
          Xác nhận thì công việc tồn sang checklist ca sau, cảnh báo và sự cố đang mở đổi người phụ trách, bàn giao bị khóa.
        </p>
      </CardContent>
    </Card>
  )
}

function ReplyPanel({ comments, onResend }) {
  const [reply, setReply] = useState('')
  const [submitted, setSubmitted] = useState(false)
  return (
    <Card className="h-fit">
      <CardHeader>
        <CardTitle className="type-card-title">Ý kiến của ca sau</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4 type-body-sm">
        <p className="rounded-lg bg-warning px-3 py-2 text-warning-foreground">{comments.at(-1)}</p>
        <Field data-invalid={(submitted && !reply.trim()) || undefined}>
          <FieldLabel htmlFor="reply">Phản hồi, phần bổ sung</FieldLabel>
          <Textarea id="reply" rows={3} value={reply} onChange={(e) => setReply(e.target.value)} aria-invalid={(submitted && !reply.trim()) || undefined} />
          {submitted && !reply.trim() && <FieldError>Nhập phản hồi cho ý kiến</FieldError>}
        </Field>
        <Button
          className="h-11 self-end"
          onClick={() => {
            setSubmitted(true)
            if (reply.trim()) onResend(reply.trim())
          }}
        >
          Bổ sung và gửi lại
        </Button>
      </CardContent>
    </Card>
  )
}

function HistoryCard() {
  return (
    <Card className="py-0">
      <CardHeader className="pt-4">
        <CardTitle className="type-card-title">Bàn giao trước</CardTitle>
      </CardHeader>
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="pl-4">Ca</TableHead>
            <TableHead>Ngày</TableHead>
            <TableHead>Người bàn giao</TableHead>
            <TableHead>Người xác nhận</TableHead>
            <TableHead className="pr-4">Trạng thái</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {DEMO_HANDOVER_HISTORY.map((h) => (
            <TableRow key={h.id}>
              <TableCell className="pl-4">
                {h.from} → {h.to}
              </TableCell>
              <TableCell className="tabular-nums">{h.date}</TableCell>
              <TableCell>{h.giver}</TableCell>
              <TableCell>{h.receiver}</TableCell>
              <TableCell className="pr-4">
                <StatusBadge status={h.status} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  )
}

export function HandoverPage({ role }) {
  const scope = DEMO_HANDOVER_SCOPE
  const canGive = role === GIVER_ROLE
  const canConfirm = role === CONFIRM_ROLE
  const [status, setStatus] = useState('Bản nháp')
  const [items, setItems] = useState(DEMO_HANDOVER_DRAFT_ITEMS.map((i) => ({ ...i, handled: false })))
  const [general, setGeneral] = useState('')
  const [followUp, setFollowUp] = useState('')
  const [comments, setComments] = useState([])
  const [announcement, setAnnouncement] = useState('')

  const waiting = (text) => (
    <Card>
      <CardContent className="type-body-sm">
        <p>{text}</p>
      </CardContent>
    </Card>
  )

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
      <PageHeader
        title={`Bàn giao ${scope.from.toLowerCase()} → ${scope.to.toLowerCase()}, ${scope.floor.toLowerCase()}`}
        subtitle={`Người bàn giao ${scope.giver}; người xác nhận: ${scope.receiver.toLowerCase()} hoặc Trưởng tầng. Bản mẫu hiện như lúc ${scope.now}, ca kết thúc ${scope.end}.`}
        aside={<StatusBadge status={status} />}
      />
      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {announcement}
      </p>

      {status === 'Bản nháp' &&
        (canGive ? (
          <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
            <SourcesPanel items={items} onAdd={(id) => setItems([...items, { sourceId: id, note: '', handled: false }])} />
            <DraftPanel
              items={items}
              setItems={setItems}
              general={general}
              setGeneral={setGeneral}
              followUp={followUp}
              setFollowUp={setFollowUp}
              onComplete={() => {
                setStatus('Đã lập')
                setAnnouncement('Đã hoàn tất bàn giao, nội dung được chốt. Bản mẫu: đổi vai trò sang Trưởng tầng để xác nhận.')
              }}
            />
          </div>
        ) : (
          waiting('Người phụ trách ca đang lập bàn giao (Bản nháp). Bản mẫu: đổi vai trò sang Điều dưỡng để lập.')
        ))}

      {status !== 'Bản nháp' && (
        <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]">
          <LockedContent items={items} general={general} followUp={followUp} status={status} />
          {status === 'Đã lập' &&
            (canConfirm ? (
              <ConfirmPanel
                onConfirm={() => {
                  setStatus('Đã xác nhận')
                  setAnnouncement('Đã xác nhận bàn giao. Bản mẫu: chuyển công việc tồn, đổi người phụ trách cảnh báo do backend thực hiện.')
                }}
                onComment={(c) => {
                  setComments([...comments, c])
                  setStatus('Có ý kiến')
                  setAnnouncement('Đã gửi ý kiến, bàn giao quay lại người bàn giao. Bản mẫu: đổi vai trò sang Điều dưỡng để bổ sung.')
                }}
              />
            ) : (
              waiting('Chờ người phụ trách ca sau xác nhận. Bản mẫu: đổi vai trò sang Trưởng tầng để xác nhận.')
            ))}
          {status === 'Có ý kiến' &&
            (canGive ? (
              <ReplyPanel
                comments={comments}
                onResend={(r) => {
                  setFollowUp(followUp ? `${followUp}\nBổ sung: ${r}` : `Bổ sung: ${r}`)
                  setStatus('Đã lập')
                  setAnnouncement('Đã bổ sung và gửi lại, bàn giao trở lại Đã lập.')
                }}
              />
            ) : (
              waiting('Bàn giao có ý kiến, chờ người bàn giao bổ sung và gửi lại.')
            ))}
          {status === 'Đã xác nhận' && waiting('Bàn giao đã xác nhận và bị khóa; sai sót xử lý bằng bản đính chính.')}
        </div>
      )}

      <HistoryCard />
    </div>
  )
}
