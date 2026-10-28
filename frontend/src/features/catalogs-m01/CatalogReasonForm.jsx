import { useState } from 'react'
import { postCatalogsByTypeByIdDeactivate, postCatalogsByTypeByIdReactivate } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'

// Ngừng hiệu lực hoặc kích hoạt lại một mục (001 T072; FR-018a, BR-M15-06). Lý do bắt buộc; kích hoạt lại trùng tên
// mục đang Hiệu lực thì 409.

const MAX_REASON = 2000

export function CatalogReasonForm({ type, item, onDone, onCancel }) {
  const deactivating = item.status === 'ACTIVE'
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    if (!reason.trim()) {
      setError('Nhập lý do')
      return
    }
    setError('')
    setSaving(true)
    try {
      const send = deactivating ? postCatalogsByTypeByIdDeactivate : postCatalogsByTypeByIdReactivate
      await send({ path: { type, id: item.id }, body: { reason: reason.trim() } })
      onDone()
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-xl border p-4">
      <h3 className="type-card-title">
        {deactivating ? 'Ngừng hiệu lực' : 'Kích hoạt lại'}: {item.name}
      </h3>
      <Field data-invalid={!!error || undefined}>
        <FieldLabel htmlFor="catalog-reason">Lý do</FieldLabel>
        <Textarea
          id="catalog-reason"
          rows={3}
          maxLength={MAX_REASON}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={!!error || undefined}
        />
        {error ? <FieldError>{error}</FieldError> : <FieldDescription>Bắt buộc.</FieldDescription>}
      </Field>
      <ProblemAlert problem={failure} />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={saving}>
          {deactivating ? 'Xác nhận ngừng' : 'Xác nhận kích hoạt'}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}
