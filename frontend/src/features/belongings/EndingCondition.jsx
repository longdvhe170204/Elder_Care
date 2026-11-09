import { useCallback, useEffect, useRef, useState } from 'react'
import { getResidentsByResidentIdBelongingsEndingCondition, postApprovalRequests } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { uploadFile } from '@/common/files'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { FilesField, SelectField } from './BelongingFields'
import { CHANNEL_OPTIONS, amountText, nowLocal, statusLabel, toOffsetIso } from './belongingLabels'

// Điều kiện (b) kết thúc lưu trú phần đồ gửi và mục "xử lý đồ gửi" sau qua đời (013 T026, US5; FR-025, FR-026,
// BR-M12-03; api-013 GET /residents/{residentId}/belongings/ending-condition), kèm đề nghị xử lý đồ không người nhận qua
// yêu cầu phê duyệt của 000 loại M12.UNCLAIMED_BELONGING_DISPOSAL (BR-M12-08, FR-027a; Hành chính lập, Quản lý viện
// duyệt ở màn Yêu cầu phê duyệt, không tự duyệt). Backend kiểm thời hạn, số lần liên hệ, biên bản, ảnh, tiền mặt chỉ
// chuyển cơ quan có thẩm quyền.

const METHODS = [
  { value: 'SELL', label: 'Bán' },
  { value: 'DESTROY', label: 'Tiêu hủy' },
  { value: 'TRANSFER_TO_AUTHORITY', label: 'Chuyển cơ quan có thẩm quyền' },
]
const POST_DEATH = { NOT_STARTED: 'Chưa làm', IN_PROGRESS: 'Đang làm', COMPLETED: 'Hoàn thành' }
const DISPOSABLE = ['HELD', 'DAMAGED']
const emptyAttempt = () => ({ familyMemberId: '', channel: '', at: nowLocal(), result: '' })

function DisposalForm({ residentId, belongings, onDone }) {
  const [ids, setIds] = useState([])
  const [v, setV] = useState({ method: '', unit: '', date: '', reason: '' })
  const [minutes, setMinutes] = useState([])
  const [photos, setPhotos] = useState([])
  const [attempts, setAttempts] = useState([emptyAttempt()])
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const key = useRef(crypto.randomUUID())
  const set = (name) => (value) => setV((s) => ({ ...s, [name]: value }))
  const setAttempt = (i, patch) => setAttempts((list) => list.map((a, j) => (j === i ? { ...a, ...patch } : a)))

  async function submit(event) {
    event.preventDefault()
    const e = {}
    if (ids.length === 0) e.ids = 'Chọn ít nhất một đồ.'
    if (!v.method) e.method = 'Chọn cách xử lý.'
    if (!v.date) e.date = 'Chọn ngày áp dụng.'
    if (!v.reason.trim()) e.reason = 'Nhập lý do.'
    setErrors(e)
    setFailure(null)
    if (Object.keys(e).length > 0) return
    try {
      const minutesFileId = minutes.length > 0 ? await uploadFile(minutes[0]) : undefined
      const photoFileIds = []
      for (const file of photos) photoFileIds.push(await uploadFile(file))
      const contactAttempts = attempts
        .filter((a) => a.familyMemberId && a.channel)
        .map((a) => ({
          familyMemberId: Number(a.familyMemberId),
          channel: a.channel,
          at: toOffsetIso(a.at),
          result: a.result.trim(),
        }))
      const { data } = await postApprovalRequests({
        headers: { 'Idempotency-Key': key.current },
        body: {
          type: 'M12.UNCLAIMED_BELONGING_DISPOSAL',
          requestedEffectiveDate: v.date,
          reason: v.reason.trim(),
          submitImmediately: true,
          content: {
            residentId,
            belongingIds: ids,
            method: v.method,
            ...(v.unit.trim() && { receivingUnit: v.unit.trim() }),
            ...(minutesFileId && { minutesFileId }),
            ...(photoFileIds.length > 0 && { photoFileIds }),
            contactAttempts,
          },
        },
      })
      onDone(`Đã gửi đề nghị duyệt, mã yêu cầu ${data.displayCode ?? data.id}.`)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  return (
    <form className="flex flex-col gap-4 rounded-xl border p-3" onSubmit={submit} noValidate>
      <FieldGroup>
        <fieldset className="flex flex-col gap-2">
          <legend className="type-label">Đồ đề nghị xử lý</legend>
          {belongings.map((b) => (
            <label key={b.id} className="flex min-h-10 cursor-pointer items-center gap-2">
              <Checkbox
                checked={ids.includes(b.id)}
                onCheckedChange={(on) => setIds((c) => (on ? [...c, b.id] : c.filter((x) => x !== b.id)))}
                aria-label={`Xử lý ${b.description}`}
              />
              <span className="type-body-sm">
                {b.description}, {amountText(b)}
              </span>
            </label>
          ))}
          {errors.ids && <FieldError>{errors.ids}</FieldError>}
        </fieldset>
        <SelectField
          id="disposal-method"
          label="Cách xử lý"
          value={v.method}
          onChange={set('method')}
          options={METHODS}
          error={errors.method}
          hint="Tiền mặt chỉ chuyển cơ quan có thẩm quyền."
        />
        <Field>
          <FieldLabel htmlFor="disposal-unit">Đơn vị nhận</FieldLabel>
          <Input id="disposal-unit" className="h-11" value={v.unit} onChange={(e) => set('unit')(e.target.value)} />
        </Field>
        <FilesField id="disposal-minutes" label="Biên bản" files={minutes} onChange={setMinutes} />
        <FilesField id="disposal-photos" label="Ảnh đồ" files={photos} onChange={setPhotos} multiple />
        <fieldset className="flex flex-col gap-3">
          <legend className="type-label">Các lần liên hệ người thân</legend>
          {attempts.map((a, i) => (
            <div key={i} className="grid gap-3 rounded-lg border p-3 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor={`attempt-member-${i}`}>Mã người thân đã liên hệ (lần {i + 1})</FieldLabel>
                <Input
                  id={`attempt-member-${i}`}
                  inputMode="numeric"
                  className="h-11"
                  value={a.familyMemberId}
                  onChange={(e) => setAttempt(i, { familyMemberId: e.target.value })}
                />
              </Field>
              <SelectField
                id={`attempt-channel-${i}`}
                label={`Kênh (lần ${i + 1})`}
                value={a.channel}
                onChange={(channel) => setAttempt(i, { channel })}
                options={CHANNEL_OPTIONS}
              />
              <Field>
                <FieldLabel htmlFor={`attempt-at-${i}`}>Thời điểm (lần {i + 1})</FieldLabel>
                <Input
                  id={`attempt-at-${i}`}
                  type="datetime-local"
                  className="h-11 tabular-nums"
                  value={a.at}
                  onChange={(e) => setAttempt(i, { at: e.target.value })}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor={`attempt-result-${i}`}>Kết quả (lần {i + 1})</FieldLabel>
                <Input
                  id={`attempt-result-${i}`}
                  className="h-11"
                  value={a.result}
                  onChange={(e) => setAttempt(i, { result: e.target.value })}
                />
              </Field>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            className="h-11 self-start"
            onClick={() => setAttempts((l) => [...l, emptyAttempt()])}
          >
            Thêm lần liên hệ
          </Button>
        </fieldset>
        <Field data-invalid={!!errors.date || undefined}>
          <FieldLabel htmlFor="disposal-date">Ngày áp dụng đề nghị</FieldLabel>
          <Input
            id="disposal-date"
            type="date"
            className="h-11 tabular-nums"
            value={v.date}
            onChange={(e) => set('date')(e.target.value)}
          />
          {errors.date && <FieldError>{errors.date}</FieldError>}
        </Field>
        <Field data-invalid={!!errors.reason || undefined}>
          <FieldLabel htmlFor="disposal-reason">Lý do đề nghị</FieldLabel>
          <Textarea
            id="disposal-reason"
            rows={2}
            maxLength={2000}
            value={v.reason}
            onChange={(e) => set('reason')(e.target.value)}
          />
          {errors.reason && <FieldError>{errors.reason}</FieldError>}
        </Field>
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11 self-start">
        Gửi đề nghị duyệt
      </Button>
    </form>
  )
}

function BelongingLines({ items, catalogs }) {
  return (
    <ul className="flex flex-col gap-1">
      {items.map((b) => (
        <li key={b.id} className="type-body-sm">
          {b.description}, {catalogs.typeName(b.typeId)}, {amountText(b)}, {statusLabel(b.status)}
        </li>
      ))}
    </ul>
  )
}

export function EndingCondition({ residentId, catalogs }) {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading' })
  const [disposing, setDisposing] = useState(false)
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByResidentIdBelongingsEndingCondition({ path: { residentId } })
      setState({ status: 'ready', ...data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải điều kiện khi mở
    load()
  }, [load])

  if (state.status === 'loading') return <Skeleton className="h-20 w-full" />
  if (state.status === 'error') return <ProblemAlert problem={state.problem} />

  const disposable = state.blocking.filter((b) => DISPOSABLE.includes(b.status))
  return (
    <section className="flex flex-col gap-3" aria-label="Điều kiện kết thúc lưu trú phần đồ gửi">
      <div className="flex flex-wrap items-baseline gap-3">
        <h3 className="type-card-title">Điều kiện kết thúc lưu trú phần đồ gửi</h3>
        <StatusBadge status={state.met ? 'Đạt' : 'Chưa đạt'} />
      </div>
      {state.blocking.length > 0 && (
        <>
          <p className="type-body-sm text-muted-foreground">Đồ chưa trả hoặc chưa xử lý:</p>
          <BelongingLines items={state.blocking} catalogs={catalogs} />
        </>
      )}
      {state.lostWithOpenIncident.length > 0 && (
        <>
          <p className="type-body-sm text-muted-foreground">
            Đồ thất lạc còn sự cố đang mở (chặn qua điều kiện sự cố):
          </p>
          <BelongingLines items={state.lostWithOpenIncident} catalogs={catalogs} />
        </>
      )}
      {state.postDeathItem?.status && (
        <p className="type-body-sm">
          Mục "xử lý đồ gửi" sau qua đời: {POST_DEATH[state.postDeathItem.status] ?? state.postDeathItem.status}
          {state.postDeathItem.basis && `, ${state.postDeathItem.basis}`}
        </p>
      )}
      <div role="status" className="type-body-sm text-success-foreground">
        {notice}
      </div>
      {can('BELONGINGS', 'CREATE') && disposable.length > 0 && (
        <Button
          variant="outline"
          className="h-11 self-start"
          aria-pressed={disposing}
          onClick={() => setDisposing(!disposing)}
        >
          Lập đề nghị xử lý đồ không người nhận
        </Button>
      )}
      {disposing && (
        <DisposalForm
          residentId={residentId}
          belongings={disposable}
          onDone={(message) => {
            setDisposing(false)
            setNotice(message)
          }}
        />
      )}
    </section>
  )
}
