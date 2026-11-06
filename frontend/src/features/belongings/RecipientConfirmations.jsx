import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getRecipientConfirmations,
  postRecipientConfirmations,
  postRecipientConfirmationsByIdByCommand,
  postRecipientConfirmationsByIdContactEvidence,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { uploadFile } from '@/common/files'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { FilesField, SelectField } from './BelongingFields'
import { CHANNEL_OPTIONS, CONFIRMATION_LABELS, nowLocal, pickLabel, toOffsetIso } from './belongingLabels'

// Xác nhận người nhận khác của một người cao tuổi (013 T019; FR-017 → FR-017b, BR-M12-04; api-013 /recipient-
// confirmations). Hành chính lập (kèm bản ký của người đại diện thì Hiệu lực ngay), ghi các lần liên hệ người đại diện,
// hủy; Quản lý viện duyệt thay khi không liên hệ được hoặc không có người đại diện (không phải người lập; backend kiểm).
// Người đại diện xác nhận, từ chối trên cổng và app người thân. Hợp đồng không trả lệnh được phép: nút Duyệt thay hiện
// theo vai trò đang xem, nút khác theo khóa quyền; backend kiểm lại.

const BASES = [
  { value: 'REPRESENTATIVE_UNREACHABLE', label: 'Không liên hệ được người đại diện' },
  { value: 'NO_REPRESENTATIVE', label: 'Không có người đại diện' },
]
const OPEN = ['PENDING_CONFIRMATION', 'EFFECTIVE']

function CreateForm({ residentId, belongings, locationName, onDone }) {
  const [v, setV] = useState({ name: '', document: '', representativeId: '' })
  const [isResident, setIsResident] = useState(false)
  const [ids, setIds] = useState([])
  const [form, setForm] = useState([])
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const key = useRef(crypto.randomUUID())
  const set = (name) => (e) => setV((s) => ({ ...s, [name]: e.target.value }))

  async function submit(event) {
    event.preventDefault()
    const e = {}
    if (!v.name.trim()) e.name = 'Nhập họ tên người nhận.'
    if (!v.document.trim()) e.document = 'Nhập giấy tờ của người nhận.'
    if (ids.length === 0) e.ids = 'Chọn ít nhất một đồ.'
    if (form.length > 0 && !/^[1-9]\d*$/.test(v.representativeId)) e.representativeId = 'Nhập mã người đại diện đã ký.'
    setErrors(e)
    setFailure(null)
    if (Object.keys(e).length > 0) return
    try {
      const fileId = form.length > 0 ? await uploadFile(form[0]) : null
      await postRecipientConfirmations({
        headers: { 'Idempotency-Key': key.current },
        body: {
          residentId,
          recipientName: v.name.trim(),
          recipientIdDocument: v.document.trim(),
          recipientIsResident: isResident,
          belongingIds: ids,
          ...(fileId && { signedForm: { representativeId: Number(v.representativeId), fileId } }),
        },
      })
      onDone('Đã lập yêu cầu xác nhận người nhận khác.')
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  return (
    <form className="flex flex-col gap-4 rounded-xl border p-3" onSubmit={submit} noValidate>
      <FieldGroup>
        <Field data-invalid={!!errors.name || undefined}>
          <FieldLabel htmlFor="rc-name">Họ tên người nhận khác</FieldLabel>
          <Input id="rc-name" autoComplete="off" className="h-11" value={v.name} onChange={set('name')} />
          {errors.name && <FieldError>{errors.name}</FieldError>}
        </Field>
        <Field data-invalid={!!errors.document || undefined}>
          <FieldLabel htmlFor="rc-document">Giấy tờ của người nhận</FieldLabel>
          <Input id="rc-document" className="h-11" value={v.document} onChange={set('document')} />
          {errors.document && <FieldError>{errors.document}</FieldError>}
        </Field>
        <label className="flex min-h-10 cursor-pointer items-center gap-2">
          <Checkbox
            checked={isResident}
            onCheckedChange={setIsResident}
            aria-label="Người nhận là chính người cao tuổi"
          />
          <span className="type-body-sm">Người nhận là chính người cao tuổi</span>
        </label>
        <fieldset className="flex flex-col gap-2">
          <legend className="type-label">Đồ được trả cho người này</legend>
          {belongings.map((b) => (
            <label key={b.id} className="flex min-h-10 cursor-pointer items-center gap-2">
              <Checkbox
                checked={ids.includes(b.id)}
                onCheckedChange={(on) => setIds((c) => (on ? [...c, b.id] : c.filter((x) => x !== b.id)))}
                aria-label={`Xác nhận cho ${b.description}, mã ${b.id}`}
              />
              <span className="type-body-sm">{pickLabel(b, locationName)}</span>
            </label>
          ))}
          {errors.ids && <FieldError>{errors.ids}</FieldError>}
        </fieldset>
        <FilesField
          id="rc-form"
          label="Bản ký của người đại diện (không bắt buộc)"
          files={form}
          onChange={setForm}
          hint="Có bản ký thì yêu cầu Hiệu lực ngay."
        />
        {form.length > 0 && (
          <Field data-invalid={!!errors.representativeId || undefined}>
            <FieldLabel htmlFor="rc-representative">Mã người đại diện đã ký</FieldLabel>
            <Input
              id="rc-representative"
              inputMode="numeric"
              className="h-11"
              value={v.representativeId}
              onChange={set('representativeId')}
            />
            {errors.representativeId && <FieldError>{errors.representativeId}</FieldError>}
          </Field>
        )}
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11 self-start">
        Lưu yêu cầu xác nhận
      </Button>
    </form>
  )
}

function ContactForm({ confirmation, onDone }) {
  const [v, setV] = useState({ familyMemberId: '', channel: '', at: nowLocal(), result: '' })
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const key = useRef(crypto.randomUUID())

  async function submit(event) {
    event.preventDefault()
    const e = {}
    if (!/^[1-9]\d*$/.test(v.familyMemberId)) e.familyMemberId = 'Nhập mã người đại diện.'
    if (!v.channel) e.channel = 'Chọn kênh liên hệ.'
    if (!v.result.trim()) e.result = 'Nhập kết quả liên hệ.'
    setErrors(e)
    setFailure(null)
    if (Object.keys(e).length > 0) return
    try {
      await postRecipientConfirmationsByIdContactEvidence({
        path: { id: confirmation.id },
        headers: { 'Idempotency-Key': key.current },
        body: {
          familyMemberId: Number(v.familyMemberId),
          channel: v.channel,
          at: toOffsetIso(v.at),
          result: v.result.trim(),
        },
      })
      onDone('Đã ghi lần liên hệ.')
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  return (
    <form className="flex flex-col gap-3" onSubmit={submit} noValidate>
      <Field data-invalid={!!errors.familyMemberId || undefined}>
        <FieldLabel htmlFor={`ce-member-${confirmation.id}`}>Mã người đại diện đã liên hệ</FieldLabel>
        <Input
          id={`ce-member-${confirmation.id}`}
          inputMode="numeric"
          className="h-11"
          value={v.familyMemberId}
          onChange={(e) => setV((s) => ({ ...s, familyMemberId: e.target.value }))}
        />
        {errors.familyMemberId && <FieldError>{errors.familyMemberId}</FieldError>}
      </Field>
      <SelectField
        id={`ce-channel-${confirmation.id}`}
        label="Kênh liên hệ"
        value={v.channel}
        onChange={(channel) => setV((s) => ({ ...s, channel }))}
        options={CHANNEL_OPTIONS}
        error={errors.channel}
      />
      <Field>
        <FieldLabel htmlFor={`ce-at-${confirmation.id}`}>Thời điểm liên hệ</FieldLabel>
        <Input
          id={`ce-at-${confirmation.id}`}
          type="datetime-local"
          className="h-11 tabular-nums"
          value={v.at}
          onChange={(e) => setV((s) => ({ ...s, at: e.target.value }))}
        />
      </Field>
      <Field data-invalid={!!errors.result || undefined}>
        <FieldLabel htmlFor={`ce-result-${confirmation.id}`}>Kết quả liên hệ</FieldLabel>
        <Input
          id={`ce-result-${confirmation.id}`}
          className="h-11"
          value={v.result}
          onChange={(e) => setV((s) => ({ ...s, result: e.target.value }))}
        />
        {errors.result && <FieldError>{errors.result}</FieldError>}
      </Field>
      <ProblemAlert problem={failure} />
      <Button type="submit" variant="outline" className="h-11 self-start">
        Lưu lần liên hệ
      </Button>
    </form>
  )
}

function CommandForm({ confirmation, command, onDone }) {
  const [reason, setReason] = useState('')
  const [basis, setBasis] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const key = useRef(crypto.randomUUID())
  const approve = command === 'approve-on-behalf'

  async function submit(event) {
    event.preventDefault()
    const e = {}
    if (!reason.trim()) e.reason = 'Nhập lý do.'
    if (approve && !basis) e.basis = 'Chọn căn cứ.'
    setErrors(e)
    setFailure(null)
    if (Object.keys(e).length > 0) return
    try {
      await postRecipientConfirmationsByIdByCommand({
        path: { id: confirmation.id, command },
        headers: { 'Idempotency-Key': key.current },
        body: { reason: reason.trim(), ...(approve && { basis }) },
      })
      onDone(approve ? 'Đã duyệt thay yêu cầu xác nhận.' : 'Đã hủy yêu cầu xác nhận.')
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  return (
    <form className="flex flex-col gap-3" onSubmit={submit} noValidate>
      {approve && (
        <SelectField
          id={`rc-basis-${confirmation.id}`}
          label="Căn cứ duyệt thay"
          value={basis}
          onChange={setBasis}
          options={BASES}
          error={errors.basis}
        />
      )}
      <Field data-invalid={!!errors.reason || undefined}>
        <FieldLabel htmlFor={`rc-reason-${confirmation.id}`}>Lý do</FieldLabel>
        <Textarea
          id={`rc-reason-${confirmation.id}`}
          rows={2}
          maxLength={2000}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={!!errors.reason || undefined}
        />
        {errors.reason && <FieldError>{errors.reason}</FieldError>}
      </Field>
      <ProblemAlert problem={failure} />
      <Button type="submit" variant={approve ? 'default' : 'destructive'} className="h-11 self-start">
        {approve ? 'Xác nhận duyệt thay' : 'Xác nhận hủy'}
      </Button>
    </form>
  )
}

// refreshKey: màn cha tăng sau khi lệnh trả thành công (xác nhận đã dùng chuyển Đã dùng), để tải lại danh sách
export function RecipientConfirmations({ residentId, belongings, locationName, role, refreshKey = 0 }) {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [creating, setCreating] = useState(false)
  const [open, setOpen] = useState(null)
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    try {
      const { data } = await getRecipientConfirmations({ query: { residentId } })
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi mở và sau khi trả đồ thành công
    load()
  }, [load, refreshKey])

  function done(message) {
    setCreating(false)
    setOpen(null)
    setNotice(message)
    load()
  }

  const canUpdate = can('BELONGINGS', 'UPDATE')
  const manager = role === 'NURSING_HOME_MANAGER'
  const toggle = (id, action) => setOpen(open?.id === id && open.action === action ? null : { id, action })

  return (
    <section className="flex flex-col gap-3" aria-label="Xác nhận người nhận khác">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="type-card-title">Xác nhận người nhận khác</h3>
        {canUpdate && (
          <Button variant="outline" className="h-11" aria-pressed={creating} onClick={() => setCreating(!creating)}>
            Lập xác nhận người nhận khác
          </Button>
        )}
      </div>
      <div role="status" className="type-body-sm text-success-foreground">
        {notice}
      </div>
      {creating && <CreateForm residentId={residentId} belongings={belongings} locationName={locationName} onDone={done} />}
      {state.status === 'error' && <ProblemAlert problem={state.problem} />}
      {state.status === 'ready' && state.items.length === 0 && (
        <p className="type-body-sm text-muted-foreground">Chưa có yêu cầu xác nhận người nhận khác.</p>
      )}
      <ul className="flex flex-col">
        {state.items.map((c) => (
          <li key={c.id} className="flex flex-col gap-2 border-t py-3 first:border-t-0">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="type-label">{c.recipientName}</span>
              <StatusBadge status={CONFIRMATION_LABELS[c.status] ?? c.status} />
            </div>
            <span className="type-body-sm text-muted-foreground">
              Mã {c.id}, giấy tờ {c.recipientIdDocument}, {c.belongingIds.length} đồ
              {c.expiresAt && `, hết hạn ${formatDateTime(c.expiresAt)}`}
            </span>
            {OPEN.includes(c.status) && (
              <div className="flex flex-wrap gap-2">
                {canUpdate && c.status === 'PENDING_CONFIRMATION' && (
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-10"
                    aria-label={`Ghi lần liên hệ cho ${c.recipientName}`}
                    onClick={() => toggle(c.id, 'contact')}
                  >
                    Ghi lần liên hệ
                  </Button>
                )}
                {manager && c.status === 'PENDING_CONFIRMATION' && (
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-10"
                    aria-label={`Duyệt thay cho ${c.recipientName}`}
                    onClick={() => toggle(c.id, 'approve-on-behalf')}
                  >
                    Duyệt thay
                  </Button>
                )}
                {canUpdate && (
                  <Button
                    variant="outline"
                    size="sm"
                    className="h-10"
                    aria-label={`Hủy yêu cầu của ${c.recipientName}`}
                    onClick={() => toggle(c.id, 'cancel')}
                  >
                    Hủy yêu cầu
                  </Button>
                )}
              </div>
            )}
            {open?.id === c.id && open.action === 'contact' && <ContactForm confirmation={c} onDone={done} />}
            {open?.id === c.id && open.action !== 'contact' && (
              <CommandForm confirmation={c} command={open.action} onDone={done} />
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
