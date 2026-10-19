import { useState } from 'react'
import { ApiError, postParametersByCodeChangeValue } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { TableValueEditor } from './TableValueEditor'
import { UNIT_LABELS, formatValue, isDuration } from './parameterValue'

// Đổi giá trị một tham số (000 T081; US4 #2, #5, #7, #9; FR-014 → FR-016, FR-021; hợp đồng
// POST /parameters/{code}/change-value). Lý do bắt buộc; có hiệu lực ngay khi lưu; 412 hiện giá trị hiện hành mới.

const REASON_MAX = 2000

function scalarOf(parameter) {
  const value = parameter.currentValue
  if (isDuration(value)) return String(value.value)
  return value === null || value === undefined ? '' : String(value)
}

function valueLabel(parameter) {
  const unit = parameter.type === 'DURATION' ? (parameter.currentValue?.unit ?? parameter.unit) : parameter.unit
  const label = parameter.type === 'PERCENTAGE' ? '%' : UNIT_LABELS[unit]
  return label ? `Giá trị mới (${label})` : 'Giá trị mới'
}

// Giá trị gửi lên theo kiểu tham số; null khi ô trống
function toValue(parameter, text) {
  if (text.trim() === '') return null
  if (parameter.type === 'TIME_OF_DAY') return text.trim()
  const number = Number(text.trim())
  if (parameter.type === 'DURATION') return { value: number, unit: parameter.currentValue?.unit ?? parameter.unit }
  return number
}

export function ParameterForm({ parameter, etag, onSaved, onStale }) {
  const isTable = parameter.type === 'TABLE'
  const [text, setText] = useState(() => scalarOf(parameter))
  const [table, setTable] = useState(() => parameter.currentValue)
  const [restore, setRestore] = useState(false)
  const [reason, setReason] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState('')

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    setNotice('')
    const next = {}
    const value = isTable ? table : toValue(parameter, text)
    if (!restore && !isTable && value === null) next.value = 'Nhập giá trị mới'
    if (!restore && !isTable && typeof value === 'number' && Number.isNaN(value)) next.value = 'Nhập một số'
    if (!reason.trim()) next.reason = 'Nhập lý do đổi giá trị'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSaving(true)
    try {
      const { data, etag: nextEtag } = await postParametersByCodeChangeValue({
        path: { code: parameter.code },
        headers: { 'If-Match': etag },
        body: restore ? { restoreDefault: true, reason } : { value, reason },
      })
      setReason('')
      setRestore(false)
      setNotice('Đã lưu giá trị mới.')
      onSaved(data, nextEtag)
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422) {
        setErrors(p.errorCode === 'REASON_REQUIRED' ? { reason: p.detail || p.title } : { value: p.detail || p.title })
      } else {
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4">
      <FieldGroup>
        {isTable ? (
          <Field data-invalid={!!errors.value || undefined}>
            <FieldLabel>Giá trị mới</FieldLabel>
            <TableValueEditor value={table} onChange={setTable} disabled={restore} />
            {errors.value && <FieldError>{errors.value}</FieldError>}
          </Field>
        ) : (
          <Field data-invalid={!!errors.value || undefined}>
            <FieldLabel htmlFor="parameter-value">{valueLabel(parameter)}</FieldLabel>
            <Input
              id="parameter-value"
              value={text}
              type={parameter.type === 'TIME_OF_DAY' ? 'text' : 'number'}
              inputMode={parameter.type === 'TIME_OF_DAY' ? undefined : 'decimal'}
              placeholder={parameter.type === 'TIME_OF_DAY' ? 'HH:mm' : undefined}
              onChange={(e) => setText(e.target.value)}
              disabled={restore}
              aria-invalid={!!errors.value || undefined}
              className="h-10 max-w-48"
            />
            {errors.value && <FieldError>{errors.value}</FieldError>}
          </Field>
        )}

        <Field orientation="horizontal">
          <Checkbox
            id="parameter-restore"
            checked={restore}
            onCheckedChange={(checked) => setRestore(checked === true)}
          />
          <FieldLabel htmlFor="parameter-restore">
            Khôi phục mặc định ({formatValue(parameter.type, parameter.defaultValue, parameter.unit)})
          </FieldLabel>
        </Field>

        <Field data-invalid={!!errors.reason || undefined}>
          <FieldLabel htmlFor="parameter-reason">Lý do</FieldLabel>
          <Textarea
            id="parameter-reason"
            rows={3}
            maxLength={REASON_MAX}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!errors.reason || undefined}
          />
          {errors.reason ? (
            <FieldError>{errors.reason}</FieldError>
          ) : (
            <FieldDescription>Bắt buộc. Giá trị mới có hiệu lực ngay khi lưu.</FieldDescription>
          )}
        </Field>
      </FieldGroup>

      <ProblemAlert problem={failure}>
        {failure?.status === 412 && failure.currentValue && (
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <p>
              Giá trị hiện hành mới:{' '}
              {formatValue(failure.currentValue.type, failure.currentValue.currentValue, failure.currentValue.unit)}
            </p>
            <Button type="button" variant="outline" size="sm" onClick={onStale}>
              Tải lại
            </Button>
          </div>
        )}
      </ProblemAlert>

      <p role="status" className="type-body-sm">
        {notice}
      </p>

      <Button type="submit" className="h-11 self-start" disabled={saving}>
        Lưu giá trị
      </Button>
    </form>
  )
}
