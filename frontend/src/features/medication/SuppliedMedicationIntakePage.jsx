import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getProcurementMedicationLines, getResidents, postSuppliedMedicationIntakes } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// 006 T065 (FR-054, FR-055, BR-M07-24; Q-410): Điều dưỡng ghi tiếp nhận thuốc mua theo yêu cầu mua sắm loại thuốc. Danh
// sách là dòng thuốc đã ghi đã mua, chưa có lần Đạt, của người cao tuổi trong phạm vi (GET /procurement-medication-lines,
// không có giá). Đạt không tạo tồn kho, không tính phí, không đổi đơn; Không đạt bắt buộc sai lệch và lý do, viện báo
// người lập và Phụ trách bộ phận; giao bù là một lần tiếp nhận mới trên cùng dòng.

export function SuppliedMedicationIntakePage() {
  const [state, setState] = useState({ status: 'loading' })
  const [names, setNames] = useState({})
  const [target, setTarget] = useState(null)
  const [notice, setNotice] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getProcurementMedicationLines()
      setState({ status: 'ready', lines: data })
    } catch (error) {
      setState({ status: 'error', error })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải danh sách khi mở màn
    load()
    getResidents({ query: {} })
      .then(({ data }) =>
        setNames(Object.fromEntries(data.items.map((r) => [r.id, `${r.personalInfo.fullName}, ${r.profileCode}`]))),
      )
      .catch(() => setNames({}))
  }, [load])

  const residentOf = (line) => names[line.residentId]?.split(', ')[0] ?? `Người cao tuổi #${line.residentId}`

  let body
  if (state.status === 'loading') body = <Skeleton className="h-40 w-full" />
  else if (state.status === 'error')
    body =
      state.error instanceof ApiError && state.error.status === 403 ? (
        <p>Bạn không có quyền tiếp nhận thuốc cung ứng.</p>
      ) : (
        <ProblemAlert problem={problemOf(state.error)} />
      )
  else if (state.lines.length === 0)
    body = <p className="text-muted-foreground">Không có thuốc mua về đang chờ tiếp nhận.</p>
  else
    body = (
      <ul className="flex flex-col">
        {state.lines.map((line) => (
          <li
            key={line.id}
            className="flex flex-wrap items-center justify-between gap-3 border-t py-3 first:border-t-0 first:pt-0"
          >
            <span className="flex flex-col gap-0.5">
              <span className="type-label">
                {line.medicationName} {line.strength}, {line.dosageForm}
              </span>
              <span className="type-caption text-muted-foreground">
                {names[line.residentId] ?? `Người cao tuổi #${line.residentId}`} · Số lượng mua{' '}
                <span className="tabular-nums">{line.quantity}</span> {line.unit} · Yêu cầu #{line.procurementRequestId}
              </span>
            </span>
            <Button
              className="h-11"
              aria-label={`Tiếp nhận ${line.medicationName} của ${residentOf(line)}`}
              onClick={() => {
                setNotice(null)
                setTarget(line)
              }}
            >
              Tiếp nhận
            </Button>
          </li>
        ))}
      </ul>
    )

  return (
    <>
      <PageHeader
        title="Tiếp nhận thuốc cung ứng"
        subtitle="Thuốc viện mua theo yêu cầu mua sắm cho người cao tuổi bạn phụ trách. Đạt không tạo tồn kho, không tính phí, không đổi đơn."
      />
      <Card>
        <CardContent className="flex flex-col gap-4">
          {notice && <p role="status">{notice}</p>}
          {body}
        </CardContent>
      </Card>
      {target && (
        <IntakeSheet
          key={target.id}
          line={target}
          resident={residentOf(target)}
          onClose={() => setTarget(null)}
          onDone={async (passed) => {
            setTarget(null)
            setNotice(
              passed
                ? 'Đã ghi tiếp nhận Đạt.'
                : 'Đã ghi Không đạt; viện đã báo người lập yêu cầu và Phụ trách bộ phận. Giao bù thì ghi lần tiếp nhận mới.',
            )
            await load()
          }}
        />
      )}
    </>
  )
}

function IntakeSheet({ line, resident, onClose, onDone }) {
  const [form, setForm] = useState({
    concentration: line.strength ?? '',
    dosageForm: line.dosageForm ?? '',
    receivedQuantity: String(line.quantity ?? ''),
    unit: line.unit ?? '',
    expiryDate: '',
    condition: '',
    result: 'PASSED',
    discrepancy: '',
    reason: '',
    delivererName: '',
  })
  const [submitted, setSubmitted] = useState(false)
  const [saving, setSaving] = useState(false)
  const [problem, setProblem] = useState(null)
  const key = useRef(crypto.randomUUID()).current
  const set = (name) => (e) => setForm({ ...form, [name]: e.target.value })
  const failed = form.result === 'NOT_PASSED'
  const missing = {
    concentration: !form.concentration.trim(),
    dosageForm: !form.dosageForm.trim(),
    receivedQuantity: !(Number(form.receivedQuantity) > 0),
    unit: !form.unit.trim(),
    expiryDate: !form.expiryDate,
    condition: !form.condition.trim(),
    delivererName: !form.delivererName.trim(),
    discrepancy: failed && !form.discrepancy.trim(),
    reason: failed && !form.reason.trim(),
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(missing).some(Boolean)) return
    setSaving(true)
    setProblem(null)
    try {
      await postSuppliedMedicationIntakes({
        body: {
          requestLineId: line.id,
          concentration: form.concentration.trim(),
          dosageForm: form.dosageForm.trim(),
          receivedQuantity: Number(form.receivedQuantity),
          unit: form.unit.trim(),
          expiryDate: form.expiryDate,
          condition: form.condition.trim(),
          result: form.result,
          ...(failed && { discrepancy: form.discrepancy.trim(), reason: form.reason.trim() }),
          delivererName: form.delivererName.trim(),
        },
        headers: { 'Idempotency-Key': key },
      })
      await onDone(!failed)
    } catch (error) {
      setProblem(problemOf(error))
      setSaving(false)
    }
  }

  const field = (name, label, props = {}, error = `Nhập ${label.toLowerCase()}`) => (
    <Field data-invalid={(submitted && missing[name]) || undefined}>
      <FieldLabel htmlFor={`intake-${name}`}>{label}</FieldLabel>
      <Input
        id={`intake-${name}`}
        className="h-11"
        value={form[name]}
        onChange={set(name)}
        aria-invalid={(submitted && missing[name]) || undefined}
        {...props}
      />
      {submitted && missing[name] && <FieldError>{error}</FieldError>}
    </Field>
  )

  return (
    <Sheet open onOpenChange={(open) => !open && onClose()}>
      <SheetContent>
        <form onSubmit={submit} noValidate className="flex min-h-0 flex-1 flex-col">
          <SheetHeader>
            <SheetTitle className="type-title">Tiếp nhận {line.medicationName}</SheetTitle>
            <SheetDescription>
              {resident}; mua {line.quantity} {line.unit}, yêu cầu #{line.procurementRequestId}
            </SheetDescription>
          </SheetHeader>
          <div className="flex flex-col gap-4 overflow-y-auto px-4">
            <ProblemAlert problem={problem} />
            {field('concentration', 'Hàm lượng')}
            {field('dosageForm', 'Dạng bào chế')}
            <div className="grid grid-cols-2 gap-3">
              {field(
                'receivedQuantity',
                'Số lượng nhận',
                { type: 'number', min: 0, inputMode: 'decimal' },
                'Nhập số lượng lớn hơn 0',
              )}
              {field('unit', 'Đơn vị')}
            </div>
            {field('expiryDate', 'Hạn dùng', { type: 'date' }, 'Chọn hạn dùng')}
            {field('condition', 'Tình trạng bao bì')}
            {field('delivererName', 'Người giao')}
            <fieldset className="flex flex-col gap-2">
              <legend className="type-label">Kết quả</legend>
              {[
                ['PASSED', 'Đạt'],
                ['NOT_PASSED', 'Không đạt'],
              ].map(([value, label]) => (
                <label key={value} className="flex min-h-11 cursor-pointer items-center gap-2">
                  <input
                    type="radio"
                    name="intake-result"
                    value={value}
                    checked={form.result === value}
                    onChange={set('result')}
                  />
                  {label}
                </label>
              ))}
            </fieldset>
            {failed && (
              <>
                <Field data-invalid={(submitted && missing.discrepancy) || undefined}>
                  <FieldLabel htmlFor="intake-discrepancy">Sai lệch</FieldLabel>
                  <Textarea id="intake-discrepancy" rows={2} value={form.discrepancy} onChange={set('discrepancy')} />
                  {submitted && missing.discrepancy && <FieldError>Nhập sai lệch</FieldError>}
                </Field>
                <Field data-invalid={(submitted && missing.reason) || undefined}>
                  <FieldLabel htmlFor="intake-reason">Lý do</FieldLabel>
                  <Textarea id="intake-reason" rows={2} value={form.reason} onChange={set('reason')} />
                  {submitted && missing.reason && <FieldError>Nhập lý do</FieldError>}
                </Field>
              </>
            )}
          </div>
          <SheetFooter>
            <Button type="submit" className="h-11" disabled={saving}>
              Ghi tiếp nhận
            </Button>
          </SheetFooter>
        </form>
      </SheetContent>
    </Sheet>
  )
}
