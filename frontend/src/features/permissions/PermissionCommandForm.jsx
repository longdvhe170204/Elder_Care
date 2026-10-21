import { useId, useState } from 'react'
import { ApiError } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// Form một lệnh đổi quyền (002 T077; FR-026): lý do bắt buộc, ô riêng của lệnh truyền qua children; 409 hiện lý do
// từ máy chủ, 412 hiện giá trị mới qua `stale(problem)` và nút Tải lại (DESIGN.md 7.4).

const REASON_MAX = 2000

export function PermissionCommandForm({ title, submitLabel, onSubmit, onCancel, onStale, stale, children }) {
  const id = useId()
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    if (!reason.trim()) {
      setError('Nhập lý do')
      return
    }
    setError(null)
    setSaving(true)
    try {
      await onSubmit(reason)
    } catch (e) {
      const p = problemOf(e)
      if (e instanceof ApiError && e.status === 422) setError(p.detail || p.title)
      else setFailure(p)
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-xl border bg-card p-4">
      <h3 className="type-card-title">{title}</h3>
      <FieldGroup>
        {children}
        <Field data-invalid={!!error || undefined}>
          <FieldLabel htmlFor={`${id}-reason`}>Lý do</FieldLabel>
          <Textarea
            id={`${id}-reason`}
            rows={3}
            maxLength={REASON_MAX}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!error || undefined}
          />
          {error ? <FieldError>{error}</FieldError> : <FieldDescription>Bắt buộc, ghi vào nhật ký.</FieldDescription>}
        </Field>
      </FieldGroup>

      <ProblemAlert problem={failure}>
        {failure?.status === 412 && (
          <>
            {stale?.(failure)}
            <Button type="button" variant="outline" size="sm" className="mt-2" onClick={onStale}>
              Tải lại
            </Button>
          </>
        )}
      </ProblemAlert>

      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={saving}>
          {submitLabel}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}

// Chọn kiểu thu hẹp (FR-025): hạ phạm vi chỉ khi quyền đang ở toàn viện
export function RestrictionKindField({ kind, onChange, canLower, labels }) {
  const id = useId()
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="type-label mb-1">Kiểu thu hẹp</legend>
      {Object.entries(labels)
        .filter(([value]) => value !== 'LOWER_SCOPE' || canLower)
        .map(([value, label]) => (
          <label key={value} htmlFor={`${id}-${value}`} className="flex min-h-11 items-center gap-2">
            <input
              id={`${id}-${value}`}
              type="radio"
              name={`${id}-kind`}
              value={value}
              checked={kind === value}
              onChange={() => onChange(value)}
              className="size-4 accent-primary"
            />
            {label}
          </label>
        ))}
    </fieldset>
  )
}
