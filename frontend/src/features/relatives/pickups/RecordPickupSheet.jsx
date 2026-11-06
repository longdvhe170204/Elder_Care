import { useEffect, useState } from 'react'
import { ApiError, getStaff, postResidentsByResidentIdPickups } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { RELATIONSHIP_LABELS } from '../relationshipLabels'
import { BLOCK_REASON_LABELS, PICKUP_TYPE_LABELS, handoverLabel } from './pickupLabels'

// Ghi đón (012 T021; FR-021 bước 1 → 5, FR-026; chỉ trực tuyến, FR-028): chọn loại đón, người đón (trong danh sách được
// phép đón tại thời điểm hoặc theo ngoại lệ), đối chiếu giấy tờ xuất trình, nhân viên bàn giao. Thời điểm do backend
// ghi theo giờ hệ thống (NFR-09). Backend kiểm danh sách, giấy tờ; không đạt thì đã ghi lần chặn và trả 409 kèm
// blockReason. Không chọn sẵn giá trị nào.

const EXCEPTION = 'exception'
const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

function Radio({ name, value, current, onChange, children }) {
  const id = `${name}-${value}`
  return (
    <label htmlFor={id} className="flex min-h-11 items-center gap-2">
      <input
        id={id}
        type="radio"
        name={name}
        value={value}
        checked={current === value}
        onChange={() => onChange(value)}
        className="size-4"
      />
      {children}
    </label>
  )
}

function blockOf(error) {
  return error instanceof ApiError ? (error.problem?.blockReason ?? null) : null
}

export function RecordPickupSheet({ residentId, residentName, pickupList, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [type, setType] = useState('')
  const [picker, setPicker] = useState('')
  const [exceptionId, setExceptionId] = useState('')
  const [docType, setDocType] = useState('')
  const [docNumber, setDocNumber] = useState('')
  const [staffId, setStaffId] = useState('')
  const [staff, setStaff] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const [blockReason, setBlockReason] = useState(null)

  useEffect(() => {
    // Nhân viên đang làm việc mà người ghi xem được (api-008: nhân viên chỉ thấy người cùng ca)
    getStaff({ query: { status: 'WORKING' } })
      .then(({ data }) => setStaff(data))
      .catch(() => setStaff([]))
  }, [])

  const selfReturn = type === 'SELF_RETURN'
  const byException = picker === EXCEPTION
  const errors = {
    type: !type,
    picker: !selfReturn && !picker,
    exception: !selfReturn && byException && !/^\d+$/.test(exceptionId.trim()),
    document: !selfReturn && !docNumber.trim(),
    staff: !staffId,
  }
  const show = (name) => submitted && errors[name]

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    setBlockReason(null)
    const body = { type }
    if (!selfReturn) {
      if (byException) body.exceptionId = Number(exceptionId.trim())
      else body.familyMemberId = Number(picker)
      body.presentedIdDocument = docType.trim()
        ? { type: docType.trim(), number: docNumber.trim() }
        : { number: docNumber.trim() }
    }
    body.handoverStaffId = Number(staffId)
    try {
      await postResidentsByResidentIdPickups({
        path: { residentId },
        headers: { 'Idempotency-Key': key },
        body,
      })
      onDone('Đã ghi đón')
    } catch (error) {
      setFailure(problemOf(error))
      setBlockReason(blockOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Ghi đón cho {residentName}</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Đối chiếu người đón với danh sách được phép đón và giấy tờ xuất trình. Cần kết nối mạng; thời điểm ghi theo
          giờ hệ thống.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <fieldset className="flex flex-col gap-1">
            <legend className="type-label">Loại đón</legend>
            <div className="grid gap-x-6 sm:grid-cols-2">
              {Object.entries(PICKUP_TYPE_LABELS).map(([value, label]) => (
                <Radio key={value} name="pickup-type" value={value} current={type} onChange={setType}>
                  {label}
                </Radio>
              ))}
            </div>
            {show('type') && <p className="type-body-sm text-destructive">Chọn loại đón</p>}
          </fieldset>

          {!selfReturn && (
            <fieldset className="flex flex-col gap-1">
              <legend className="type-label">Người đón</legend>
              {pickupList.length === 0 && (
                <p className="type-body-sm text-muted-foreground">Danh sách được phép đón đang trống.</p>
              )}
              {pickupList.map((p) => (
                <Radio
                  key={p.familyMemberId}
                  name="pickup-picker"
                  value={String(p.familyMemberId)}
                  current={picker}
                  onChange={setPicker}
                >
                  {p.fullName}
                  <span className="type-body-sm text-muted-foreground">
                    {RELATIONSHIP_LABELS[p.relationship] ?? p.relationship}
                  </span>
                </Radio>
              ))}
              <Radio name="pickup-picker" value={EXCEPTION} current={picker} onChange={setPicker}>
                Người đón theo ngoại lệ
              </Radio>
              {show('picker') && <p className="type-body-sm text-destructive">Chọn người đón</p>}
            </fieldset>
          )}

          {!selfReturn && byException && (
            <Field data-invalid={show('exception') || undefined}>
              <FieldLabel htmlFor="pickup-exception-id">Mã yêu cầu ngoại lệ</FieldLabel>
              <Input
                id="pickup-exception-id"
                inputMode="numeric"
                className="h-11"
                value={exceptionId}
                onChange={(e) => setExceptionId(e.target.value)}
                aria-invalid={show('exception') || undefined}
              />
              {show('exception') && <FieldError>Nhập mã yêu cầu ngoại lệ</FieldError>}
            </Field>
          )}

          {!selfReturn && (
            <>
              <Field>
                <FieldLabel htmlFor="pickup-doc-type">Loại giấy tờ xuất trình</FieldLabel>
                <Input
                  id="pickup-doc-type"
                  className="h-11"
                  value={docType}
                  onChange={(e) => setDocType(e.target.value)}
                />
              </Field>
              <Field data-invalid={show('document') || undefined}>
                <FieldLabel htmlFor="pickup-doc-number">Số giấy tờ xuất trình</FieldLabel>
                <Input
                  id="pickup-doc-number"
                  className="h-11"
                  value={docNumber}
                  onChange={(e) => setDocNumber(e.target.value)}
                  aria-invalid={show('document') || undefined}
                />
                {show('document') && <FieldError>Nhập số giấy tờ xuất trình</FieldError>}
              </Field>
            </>
          )}

          <Field data-invalid={show('staff') || undefined}>
            <FieldLabel htmlFor="pickup-staff">{handoverLabel(type)}</FieldLabel>
            <select
              id="pickup-staff"
              value={staffId}
              onChange={(e) => setStaffId(e.target.value)}
              aria-invalid={show('staff') || undefined}
              className={SELECT}
            >
              <option value="">{staff === null ? 'Đang tải danh sách nhân viên' : 'Chọn nhân viên'}</option>
              {(staff ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.fullName}
                </option>
              ))}
            </select>
            {show('staff') && <FieldError>Chọn {handoverLabel(type).toLowerCase()}</FieldError>}
          </Field>

          <ProblemAlert problem={failure}>
            {blockReason && (
              <>
                <p>{BLOCK_REASON_LABELS[blockReason] ?? blockReason}</p>
                <p>Đã ghi lần chặn đón, Trưởng tầng được báo. Người ngoài danh sách cần yêu cầu ngoại lệ.</p>
              </>
            )}
          </ProblemAlert>
          <Button type="submit" className="h-11 self-start" disabled={busy}>
            Ghi bản ghi đón
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}
