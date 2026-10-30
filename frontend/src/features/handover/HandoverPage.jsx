import { useEffect, useState } from 'react'
import {
 getMeShifts, getShiftSchedules, getShiftsById, getShiftsByIdHandovers, getHandoversById,
 getHandoversByIdSourceRecords, postShiftsByIdHandovers, postHandoversByIdItems,
 putHandoversByIdItemsByItemId, deleteHandoversByIdItemsByItemId, putHandoversByIdAssessments,
 postHandoversByIdComplete, postHandoversByIdConfirm, postHandoversByIdComments,
 postHandoversByIdSupplementAndResubmit, getResidentsById, getResidents,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { PermissionsProvider, usePermissions } from '@/common/permissions/permissions'
import { HandoverCorrections } from './HandoverCorrections'

const STATUS = { DRAFT: 'Bản nháp', PREPARED: 'Đã lập', HAS_COMMENTS: 'Có ý kiến', CONFIRMED: 'Đã xác nhận' }
const GROUP = { TASK: 'Công việc', DOSE: 'Liều thuốc', VITAL_SIGN: 'Chỉ số vượt ngưỡng', ALERT: 'Cảnh báo', INCIDENT: 'Sự cố', RESIDENT_CHANGE: 'Biến động người cao tuổi', BED_AWAITING_READINESS: 'Giường chờ sẵn sàng', STORED_BELONGING: 'Đồ gửi đang giữ', FOLLOW_UP_ISSUE: 'Vấn đề cần theo dõi' }
const SOURCE_STATUS = { OVERDUE: 'Quá hạn', DUE: 'Đến hạn', NOT_DUE: 'Chưa đến hạn', OPEN: 'Đang mở', NEW: 'Mới', ACKNOWLEDGED: 'Đã tiếp nhận', IN_PROGRESS: 'Đang xử lý', COMPLETED: 'Hoàn thành', COMPLETED_LATE: 'Hoàn thành trễ', NOT_PERFORMED: 'Không thực hiện', CANCELLED: 'Đã hủy', CLOSED: 'Đã đóng', LATE: 'Trễ', MISSED: 'Bỏ lỡ', REFUSED: 'Từ chối', TAKEN: 'Đã dùng', CARRIED: 'Mang theo', ABNORMAL: 'Vượt ngưỡng', IN_RESIDENCE: 'Đang lưu trú', TEMPORARILY_ABSENT: 'Tạm vắng', HOSPITALIZED: 'Điều trị tại bệnh viện', OFF_SITE_ACTIVITY: 'Hoạt động bên ngoài', OVERDUE_RETURN: 'Quá giờ dự kiến trở về', AWAITING_READINESS: 'Chờ sẵn sàng', AWAITING_CONFIRMATION: 'Chờ xác nhận', HELD: 'Đang giữ', NOT_HELD: 'Đã chuyển giữ hoặc trả' }
const labelStatus = (value) => SOURCE_STATUS[value] ?? 'Chưa đọc được trạng thái'
const inputOf = (item) => Object.fromEntries(['group', 'sourceType', 'sourceId', 'residentId', 'roomId', 'description', 'note', 'resolved'].map((key) => [key, item[key]]))
function monthNow() { const [, m, y] = formatDate(new Date().toISOString()).split('/'); return `${y}-${m}` }
function monthAfter(value) { const [y, m] = value.split('-').map(Number); return m === 12 ? `${y + 1}-01` : `${y}-${String(m + 1).padStart(2, '0')}` }
function ResidentName({ id }) {
 const [name, setName] = useState('')
 useEffect(() => { let current = true; if (id) getResidentsById({ path: { id } }).then(({ data }) => { if (current) setName(data.fullName ?? '') }).catch(() => {}); return () => { current = false } }, [id])
 return id ? <span className="type-label">{name || 'Người cao tuổi trong bản ghi'}</span> : null
}
function SelectField({ label, value, onChange, children, id }) {
 return <Field><FieldLabel htmlFor={id}>{label}</FieldLabel><select id={id} value={value} onChange={(e) => onChange(e.target.value)} className="h-11 w-full min-w-0 rounded-xl border border-input bg-background px-3 type-body focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">{children}</select></Field>
}
export function HandoverPage(props) { return <PermissionsProvider><Workspace {...props} /></PermissionsProvider> }
function Workspace({ initialShiftId }) {
 const { status, can } = usePermissions()
 const [month, setMonth] = useState(monthNow)
 const [shiftId, setShiftId] = useState(initialShiftId ? String(initialShiftId) : '')
 const [choices, setChoices] = useState([])
 const [shift, setShift] = useState(null)
 const [handover, setHandover] = useState(null)
 const [etag, setEtag] = useState(null)
 const [sources, setSources] = useState([])
 const [failure, setFailure] = useState(null)
 const [loading, setLoading] = useState(false)
 const [busy, setBusy] = useState(false)
 const [revision, setRevision] = useState(0)
 const [announcement, setAnnouncement] = useState('')
 const view = can('SHIFT_HANDOVER', 'VIEW')
 const manage = can('SHIFT_SCHEDULE', 'CREATE') || can('SHIFT_SCHEDULE', 'UPDATE') || can('SHIFT_SCHEDULE', 'APPROVE')
 useEffect(() => {
  if (status !== 'ready' || !view || initialShiftId) return
  let current = true
  const request = manage ? getShiftSchedules({ query: { month } }) : getMeShifts({ query: { from: `${month}-01`, to: `${monthAfter(month)}-01` } })
  request.then(({ data }) => { if (current) { const all = manage ? data.flatMap((s) => s.shifts ?? []) : data; setChoices(all); setShiftId((old) => all.some((s) => String(s.id) === old) ? old : String(all[0]?.id ?? '')) } }).catch((e) => { if (current) setFailure(problemOf(e)) })
  return () => { current = false }
 }, [status, view, manage, month, initialShiftId])
 useEffect(() => {
  if (status !== 'ready' || !view || !shiftId) return
  let current = true
  async function load() {
   setLoading(true)
   try {
    const detail = await getShiftsById({ path: { id: shiftId } })
    let result
    try { result = await getShiftsByIdHandovers({ path: { id: shiftId } }) } catch (e) { if (e.status !== 404) throw e }
    if (!current) return
    setShift(detail.data); setHandover(result?.data ?? null); setEtag(result?.etag ?? null); setSources([])
    if (result) {
     const rows = await getHandoversByIdSourceRecords({ path: { id: result.data.id }, query: { afterPreparation: result.data.status !== 'DRAFT' } })
     if (current) setSources(rows.data)
    }
    if (current) setFailure(null)
   } catch (e) { if (current) setFailure(problemOf(e)) } finally { if (current) setLoading(false) }
  }
  load(); return () => { current = false }
 }, [status, view, shiftId, revision])
 const allowed = (command) => (handover?.allowedCommands ?? []).includes(command)
 async function refresh(id) {
  const result = await getHandoversById({ path: { id } })
  setHandover(result.data); setEtag(result.etag)
  const rows = await getHandoversByIdSourceRecords({ path: { id }, query: { afterPreparation: result.data.status !== 'DRAFT' } })
  setSources(rows.data)
 }
 async function act(command, message) {
  if (busy) return
  if (!navigator.onLine) { setFailure({ status: 0, title: 'Cần kết nối mạng', detail: 'Kết nối mạng rồi thực hiện lại lệnh bàn giao.', unmetConditions: [] }); return }
  setBusy(true); setFailure(null)
  try { const created = await command(); await refresh(created?.data?.id ?? handover.id); setAnnouncement(message) } catch (e) { setFailure(problemOf(e)) } finally { setBusy(false) }
 }
 const params = (body) => ({ path: { id: handover.id }, headers: { 'Idempotency-Key': crypto.randomUUID(), 'If-Match': etag }, ...(body !== undefined && { body }) })
 if (status === 'ready' && !view || status === 'error' || failure?.status === 403) return <PermissionDeniedState label="bàn giao ca" />
 return <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
  <PageHeader title="Bàn giao ca" subtitle={shift ? `Ca ${formatDateTime(shift.startsAt)} đến ${formatDateTime(shift.endsAt)}` : 'Chọn ca để xem hoặc lập bàn giao.'} aside={handover && <StatusBadge status={STATUS[handover.status]} />}>
   {!initialShiftId && <div className="grid gap-3 md:grid-cols-[12rem_minmax(0,1fr)]"><Field><FieldLabel htmlFor="handover-month">Tháng có ca</FieldLabel><Input id="handover-month" type="month" value={month} onChange={(e) => setMonth(e.target.value)} /></Field><SelectField id="handover-shift" label="Ca cần bàn giao" value={shiftId} onChange={setShiftId}><option value="">Chọn ca</option>{choices.map((s) => <option key={s.id} value={s.id}>{formatDateTime(s.startsAt)} đến {formatDateTime(s.endsAt)}</option>)}</SelectField></div>}
  </PageHeader>
  {announcement && <p role="status" aria-live="polite" className="rounded-xl border bg-card px-4 py-3 type-body-sm">{announcement}</p>}
  <ProblemAlert problem={failure}>
   {failure?.status === 412 && <p>Bản hiện tại: {STATUS[failure.currentValue?.status] ?? 'Nội dung đã thay đổi'}.</p>}
   <Button variant="outline" className="h-11" onClick={() => setRevision((n) => n + 1)}>Tải lại bàn giao</Button>
  </ProblemAlert>
  {loading || status === 'loading' ? <Card><CardContent><Skeleton className="h-28 w-full" /></CardContent></Card> : !handover ? <Card><CardContent className="flex flex-col items-start gap-3"><p>{shiftId ? 'Ca chưa có bàn giao. Chỉ người có quyền mới lập được từ mốc trước khi kết ca.' : 'Chưa có ca trong tháng hoặc chưa chọn ca.'}</p>{shift?.allowedCommands?.includes('createHandover') && <Button disabled={busy} className="h-11" onClick={() => act(() => postShiftsByIdHandovers({ path: { id: shiftId }, headers: { 'Idempotency-Key': crypto.randomUUID() } }), 'Đã lập bản nháp bàn giao.')}>Lập bàn giao</Button>}</CardContent></Card> :
   <div className={`grid items-start gap-4 ${handover.status === 'DRAFT' ? 'lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]' : 'lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]'}`}>
    <Card className={handover.status !== 'DRAFT' ? 'lg:order-2' : ''}><CardHeader><CardTitle className="type-card-title">{handover.status === 'DRAFT' ? 'Bản ghi nguồn để tham khảo' : 'Bản ghi nguồn mới sau khi lập'}</CardTitle></CardHeader><CardContent className="flex flex-col gap-4">
     <p className="type-body-sm text-muted-foreground">Chọn từng bản ghi cần bàn giao. Hệ thống không lập sẵn mục.</p>
     {sources.length === 0 && <p>Chưa có bản ghi nguồn trong danh sách này.</p>}
     {sources.map((s, index) => <div key={`${s.sourceType}:${s.sourceId ?? index}`} className="flex min-w-0 flex-col gap-2 border-t pt-3 first:border-t-0">
      <p className="type-caption text-muted-foreground">{GROUP[s.group]}</p><ResidentName id={s.residentId} /><p className="break-words type-body-sm">{s.dataUnavailable ? 'Nguồn chưa sẵn sàng. Tải lại để kiểm tra dữ liệu.' : s.description ?? 'Nội dung được giới hạn theo quyền xem.'}</p>
      {!s.dataUnavailable && <div className="flex flex-wrap items-center justify-between gap-2"><StatusBadge status={labelStatus(s.currentStatus)} />{allowed('addItem') && <Button variant="outline" className="h-11" disabled={busy || s.hasItem} aria-label={`Thêm ${s.description}`} onClick={() => act(() => postHandoversByIdItems(params(inputOf({ ...s, resolved: false }))), 'Đã thêm mục bàn giao.')}>{s.hasItem ? 'Đã có mục' : 'Thêm'}</Button>}</div>}
     </div>)}
    </CardContent></Card>
    <Card><CardHeader><CardTitle className="type-card-title">{handover.status === 'DRAFT' ? 'Nội dung bàn giao' : 'Nội dung đã chốt'}</CardTitle></CardHeader><CardContent className="flex flex-col gap-5">
     {handover.finalizedAt && <p className="type-caption text-muted-foreground">Chốt lúc {formatDateTime(handover.finalizedAt)}; phần gốc được giữ nguyên.</p>}
     {handover.items.length === 0 && <p className="type-body-sm">Chưa thêm mục bàn giao.</p>}
     {handover.items.map((item) => <ItemBlock key={item.id} item={item} critical={sources.some((s) => s.sourceType === item.sourceType && s.sourceId === item.sourceId && s.critical)} busy={busy} editable={allowed('updateItem')} removable={allowed('deleteItem')} onSave={(note, resolved) => act(() => putHandoversByIdItemsByItemId({ ...params({ ...inputOf(item), note, resolved }), path: { id: handover.id, itemId: item.id } }), 'Đã lưu ghi chú.')} onDelete={() => act(() => deleteHandoversByIdItemsByItemId({ path: { id: handover.id, itemId: item.id }, headers: { 'If-Match': etag } }), 'Đã bỏ mục bàn giao.')} />)}
     {allowed('addItem') && <FollowUpForm busy={busy} residents={[...new Set([...sources, ...handover.items].map((s) => s.residentId).filter(Boolean))]} rooms={[...new Set([...sources, ...handover.items].map((s) => s.roomId).filter(Boolean))]} onAdd={(body) => act(() => postHandoversByIdItems(params(body)), 'Đã ghi vấn đề cần theo dõi.')} />}
     {allowed('assess') ? <AssessmentForm handover={handover} busy={busy} onSave={(body) => act(() => putHandoversByIdAssessments(params(body)), 'Đã lưu nhận định.')} /> : <div className="flex flex-col gap-3"><h3 className="type-label">Nhận định chung</h3><p className="whitespace-pre-wrap type-body-sm">{handover.generalAssessment || 'Chưa ghi nhận.'}</p>{handover.residentAssessments.map((a) => <div key={a.residentId}><ResidentName id={a.residentId} /><p className="whitespace-pre-wrap type-body-sm">{a.content ?? 'Nội dung được giới hạn theo quyền xem.'}</p></div>)}</div>}
     {handover.hiddenFields.length > 0 && <p className="type-caption text-muted-foreground">Một số thông tin sức khỏe được giới hạn theo quyền xem.</p>}
     {handover.comments.map((c) => <div key={c.id} className="rounded-xl border bg-muted p-3 type-body-sm"><p className="type-label">{c.sender}, {formatDateTime(c.at)}</p><p className="whitespace-pre-wrap">{c.content}</p>{c.replies?.map((r, index) => <p key={index} className="mt-2 whitespace-pre-wrap">Phản hồi: {r.content}</p>)}</div>)}
     {allowed('complete') && <Button className="h-11 self-end" disabled={busy} onClick={() => act(() => postHandoversByIdComplete(params()), 'Đã hoàn tất bàn giao và chốt nội dung.')}>Hoàn tất bàn giao</Button>}
     {allowed('comment') && <CommentForm busy={busy} onSend={(content) => act(() => postHandoversByIdComments(params({ content })), 'Đã gửi ý kiến, chờ bổ sung.')} />}
     {allowed('confirm') && <Button className="h-11 self-end" disabled={busy} onClick={() => act(() => postHandoversByIdConfirm(params()), 'Đã xác nhận bàn giao. Công việc và trách nhiệm được chuyển cùng lần xác nhận.')}>Xác nhận bàn giao</Button>}
     {allowed('supplementAndResubmit') && <ReplyForm comments={handover.comments} busy={busy} onSend={(replies) => act(() => postHandoversByIdSupplementAndResubmit(params({ replies })), 'Đã bổ sung và gửi lại.')} />}
     {handover.status === 'CONFIRMED' && <p className="type-body-sm">Bàn giao đã xác nhận. Sai sót được xử lý bằng đính chính; nội dung gốc được giữ nguyên.</p>}
     {handover.status === 'CONFIRMED' && <HandoverCorrections key={handover.id} handover={handover} canCorrect={can('SHIFT_HANDOVER', 'UPDATE') && !handover.hiddenFields.length} onChanged={() => setRevision((n) => n + 1)} />}
    </CardContent></Card>
   </div>}
 </div>
}
function ItemBlock({ item, critical, editable, removable, busy, onSave, onDelete }) {
 const [note, setNote] = useState(item.note ?? '')
 const [resolved, setResolved] = useState(Boolean(item.resolved))
 const protectedItem = ['VITAL_SIGN', 'RESIDENT_CHANGE'].includes(item.group)
 return <section className="flex min-w-0 flex-col gap-3 border-t pt-4"><div className="flex flex-wrap items-center justify-between gap-2"><h3 className="type-label">{GROUP[item.group]}</h3>{item.section === 'AFTER_PREPARATION' && <span className="type-caption text-muted-foreground">Phát sinh sau khi lập</span>}{item.section === 'RESOLVED_DURING_SHIFT' && <StatusBadge status="Đã xử lý trong ca" />}</div><ResidentName id={item.residentId} /><p className="break-words type-body-sm">{item.description ?? 'Nội dung được giới hạn theo quyền xem.'}</p><div className="flex flex-wrap gap-2 type-caption">{item.statusAtFinalization && <span>Lúc chốt: {labelStatus(item.statusAtFinalization)}</span>}<span>Nay: {labelStatus(item.currentStatus)}</span></div>
  {editable && critical && !note.trim() && <p className="type-caption text-destructive" role="status">Cần ghi chú trước khi hoàn tất</p>}
  {editable ? <><Field><FieldLabel htmlFor={`note-${item.id}`}>Ghi chú mục {item.id}</FieldLabel><Textarea id={`note-${item.id}`} rows={3} value={note} onChange={(e) => setNote(e.target.value)} /></Field><div className="flex flex-wrap gap-2"><Button variant="outline" className="h-11" disabled={busy} aria-label={`Lưu ghi chú mục ${item.id}`} onClick={() => onSave(note, resolved)}>Lưu ghi chú</Button>{!protectedItem && <Button variant="outline" className="h-11" disabled={busy} onClick={() => { setResolved(true); onSave(note, true) }}>Ghi đã xử lý</Button>}{removable && !protectedItem ? <Button variant="ghost" className="h-11" disabled={busy} aria-label={`Bỏ mục ${item.id}`} onClick={onDelete}>Bỏ mục</Button> : protectedItem && <span className="self-center type-caption text-muted-foreground">Không bỏ được</span>}</div></> : <p className="whitespace-pre-wrap type-body-sm">{item.note || 'Chưa ghi chú.'}</p>}
  {item.noteAuthor && <p className="type-caption text-muted-foreground">Ghi chú bởi {item.noteAuthor}</p>}
 </section>
}
function AssessmentForm({ handover, busy, onSave }) {
 const [general, setGeneral] = useState(handover.generalAssessment ?? '')
 const [byResident, setByResident] = useState(Object.fromEntries(handover.residentAssessments.map((r) => [r.residentId, r.content ?? ''])))
 const residents = [...new Set(handover.items.map((i) => i.residentId).filter(Boolean))]
 return <form className="flex flex-col gap-3 border-t pt-4" onSubmit={(e) => { e.preventDefault(); onSave({ general, byResident: Object.entries(byResident).map(([residentId, content]) => ({ residentId: Number(residentId), content })) }) }}><Field><FieldLabel htmlFor="handover-general">Nhận định chung</FieldLabel><Textarea id="handover-general" value={general} onChange={(e) => setGeneral(e.target.value)} rows={3} /><p className="type-caption text-muted-foreground">Không ghi thông tin sức khỏe riêng của từng người ở nhận định chung.</p></Field>{residents.map((id) => <Field key={id}><FieldLabel htmlFor={`assessment-${id}`}>Nhận định: <ResidentName id={id} /></FieldLabel><Textarea id={`assessment-${id}`} value={byResident[id] ?? ''} onChange={(e) => setByResident({ ...byResident, [id]: e.target.value })} /></Field>)}<Button type="submit" variant="outline" className="h-11 self-end" disabled={busy}>Lưu nhận định</Button></form>
}
function FollowUpForm({ residents, rooms, busy, onAdd }) {
 const [target, setTarget] = useState('')
 const [description, setDescription] = useState('')
 const [query, setQuery] = useState('')
 const [people, setPeople] = useState([])
 const [error, setError] = useState(null)
 const residentIds = residents.join(',')
 useEffect(() => {
  let current = true
  const timer = setTimeout(async () => {
   try {
    const values = query.trim() ? (await getResidents({ query: { q: query.trim() } })).data.items : await Promise.all(residentIds.split(',').filter(Boolean).map(async (id) => (await getResidentsById({ path: { id } })).data))
    if (current) { setPeople(values); setError(null) }
   } catch (e) { if (current) setError(problemOf(e)) }
  }, 250)
  return () => { current = false; clearTimeout(timer) }
 }, [query, residentIds])
 return <form className="flex flex-col gap-3 border-t pt-4" onSubmit={(e) => { e.preventDefault(); if (!target || !description.trim()) return; const [kind, id] = target.split(':'); onAdd({ group: 'FOLLOW_UP_ISSUE', description: description.trim(), [kind === 'resident' ? 'residentId' : 'roomId']: Number(id), resolved: false }) }}>
  <h3 className="type-label">Ghi vấn đề cần theo dõi</h3>
  <Field><FieldLabel htmlFor="follow-up-search">Tìm người cao tuổi</FieldLabel><Input id="follow-up-search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Nhập tên hoặc mã hồ sơ" /></Field>
  <ProblemAlert problem={error} />
  <SelectField id="follow-up-target" label="Người cao tuổi hoặc phòng liên quan" value={target} onChange={setTarget}><option value="">Chọn đối tượng trong ca</option>{people.map((person) => <option key={person.id} value={`resident:${person.id}`}>{person.fullName}</option>)}{rooms.map((id) => <option key={id} value={`room:${id}`}>Phòng có mã {id}</option>)}</SelectField>
  <Field><FieldLabel htmlFor="follow-up-content">Nội dung cần theo dõi</FieldLabel><Textarea id="follow-up-content" value={description} onChange={(e) => setDescription(e.target.value)} /></Field>
  <Button variant="outline" className="h-11 self-end" type="submit" disabled={busy || !target || !description.trim()}>Thêm vấn đề cần theo dõi</Button>
 </form>
}
function CommentForm({ busy, onSend }) {
 const [content, setContent] = useState('')
 return <form className="flex flex-col gap-3 border-t pt-4" onSubmit={(e) => { e.preventDefault(); if (content.trim()) onSend(content.trim()) }}><Field><FieldLabel htmlFor="handover-comment">Ý kiến khi thấy thiếu sót</FieldLabel><Textarea id="handover-comment" value={content} onChange={(e) => setContent(e.target.value)} /></Field><Button type="submit" variant="outline" className="h-11 self-end" disabled={busy || !content.trim()}>Gửi ý kiến</Button></form>
}
function ReplyForm({ comments, busy, onSend }) {
 const unanswered = comments.filter((c) => !c.replies?.length)
 const [replies, setReplies] = useState({})
 return <form className="flex flex-col gap-3 border-t pt-4" onSubmit={(e) => { e.preventDefault(); onSend(unanswered.map((c) => ({ commentId: c.id, content: replies[c.id]?.trim() ?? '' }))) }}>{unanswered.map((c) => <Field key={c.id}><FieldLabel htmlFor={`reply-${c.id}`}>Phản hồi ý kiến: {c.content}</FieldLabel><Textarea id={`reply-${c.id}`} value={replies[c.id] ?? ''} onChange={(e) => setReplies({ ...replies, [c.id]: e.target.value })} /></Field>)}<Button type="submit" className="h-11 self-end" disabled={busy || unanswered.some((c) => !replies[c.id]?.trim())}>Bổ sung và gửi lại</Button></form>
}
