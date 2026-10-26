import { useState } from 'react'
import { getCorrectionsByRecordTypeByOriginalRecordId, postCorrectionsByRecordTypeByOriginalRecordId } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'

function contentOf(h) {
 return { generalAssessment: h.generalAssessment, items: h.items.map(({ id, description, note }) => ({ id, description, note })), residentAssessments: h.residentAssessments.map(({ residentId, content }) => ({ residentId, content })), comments: h.comments.map(({ id, content, replies }) => ({ id, content, replies: (replies ?? []).map(({ content }) => ({ content })) })) }
}
function Content({ value }) {
 return <div className="flex flex-col gap-2 whitespace-pre-wrap type-body-sm"><p>Nhận định chung: {value?.generalAssessment}</p>{value?.items?.map((i) => <p key={i.id}>Mục {i.id}: {i.description}{'\n'}Ghi chú: {i.note}</p>)}{value?.residentAssessments?.map((a) => <p key={a.residentId}>Nhận định riêng: {a.content}</p>)}{value?.comments?.map((c) => <div key={c.id}><p>Ý kiến: {c.content}</p>{c.replies?.map((r, index) => <p key={index}>Phản hồi: {r.content}</p>)}</div>)}</div>
}
export function HandoverCorrections({ handover, canCorrect, onChanged }) {
 const [open, setOpen] = useState(false)
 const [chain, setChain] = useState(null)
 const [content, setContent] = useState(() => contentOf(handover))
 const [reason, setReason] = useState('')
 const [failure, setFailure] = useState(null)
 const [busy, setBusy] = useState(false)
 const [notice, setNotice] = useState('')
 const path = { recordType: 'M09.HANDOVER', originalRecordId: handover.id }
 async function show() {
  setOpen(true); setFailure(null); setBusy(true)
  try { setChain((await getCorrectionsByRecordTypeByOriginalRecordId({ path })).data) } catch (e) { setFailure(problemOf(e)) } finally { setBusy(false) }
 }
 async function save(e) {
  e.preventDefault()
  if (!navigator.onLine) { setFailure({ title: 'Cần kết nối mạng', detail: 'Kết nối mạng rồi lưu đính chính.' }); return }
  setBusy(true); setFailure(null)
  try {
   const result = await postCorrectionsByRecordTypeByOriginalRecordId({ path, headers: { 'Idempotency-Key': crypto.randomUUID() }, body: { type: 'AMEND_CONTENT', correctedContent: content, reason: reason.trim() } })
   if (result.status === 202) setNotice(`Đã gửi yêu cầu ${result.data.displayCode} chờ Quản lý viện duyệt.`)
   else { setNotice('Đã lưu đính chính; bản gốc được giữ nguyên.'); setChain((await getCorrectionsByRecordTypeByOriginalRecordId({ path })).data); onChanged() }
  } catch (e) { setFailure(problemOf(e)) } finally { setBusy(false) }
 }
 const field = (label, value, change, id) => <Field key={id}><FieldLabel htmlFor={id}>{label}</FieldLabel><Textarea id={id} value={value ?? ''} onChange={(e) => change(e.target.value)} /></Field>
 const updateRow = (key, index, patch) => setContent((old) => ({ ...old, [key]: old[key].map((r, i) => i === index ? { ...r, ...patch } : r) }))
 return <section className="flex flex-col gap-3 border-t pt-4"><Button variant="outline" className="h-11 self-start" onClick={() => open ? setOpen(false) : show()}>{open ? 'Đóng đính chính' : 'Xem và đính chính'}</Button>{open && <>
  <ProblemAlert problem={failure} />{notice && <p role="status">{notice}</p>}
  {chain && <><details><summary className="cursor-pointer type-label">Xem nội dung gốc</summary><Content value={chain.originalContent} /></details>{chain.corrections.map((c) => <details key={c.id}><summary className="cursor-pointer type-label">Đính chính lần {c.sequenceNo}, {formatDateTime(c.recordedAt)}</summary><Content value={c.correctedContent} /><p>Lý do: {c.reason}</p><p>Người ghi: {c.actor?.fullName ?? 'Chưa có tên'}</p></details>)}</>}
  {chain && canCorrect && !chain.voided && <form className="flex flex-col gap-3" onSubmit={save}>
   {field('Nhận định chung đúng', content.generalAssessment, (value) => setContent({ ...content, generalAssessment: value }), 'correct-general')}
   {content.items.map((item, index) => <div key={item.id} className="flex flex-col gap-3 rounded-xl border p-3">{field(`Nội dung mục ${item.id}`, item.description, (description) => updateRow('items', index, { description }), `correct-description-${item.id}`)}{field(`Ghi chú mục ${item.id}`, item.note, (note) => updateRow('items', index, { note }), `correct-note-${item.id}`)}</div>)}
   {content.residentAssessments.map((a, index) => field(`Nhận định riêng ${index + 1}`, a.content, (value) => updateRow('residentAssessments', index, { content: value }), `correct-resident-${a.residentId}`))}
   {content.comments.map((c, index) => <div key={c.id} className="flex flex-col gap-3">{field(`Ý kiến ${index + 1}`, c.content, (value) => updateRow('comments', index, { content: value }), `correct-comment-${c.id}`)}{c.replies.map((r, ri) => field(`Phản hồi ${ri + 1} của ý kiến ${index + 1}`, r.content, (value) => updateRow('comments', index, { replies: c.replies.map((row, n) => n === ri ? { content: value } : row) }), `correct-reply-${c.id}-${ri}`))}</div>)}
   {field('Lý do đính chính', reason, setReason, 'correct-reason')}
   <Button type="submit" className="h-11 self-end" disabled={busy || !reason.trim()}>Lưu đính chính</Button>
  </form>}
 </>}</section>
}
