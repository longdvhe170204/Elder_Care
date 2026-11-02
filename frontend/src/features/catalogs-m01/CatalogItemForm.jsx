import { useState } from 'react'
import { postCatalogsByType, putCatalogsByTypeById } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { ALLERGEN_GROUPS } from './catalogLabels'

// Tạo hoặc sửa một mục danh mục (001 T072; FR-018a, BR-M15-06). Sửa gửi If-Match là version của dòng (phản hồi danh
// sách có `version`); 412 thì mời tải lại. Dị nguyên cần nhóm; mức độ dị ứng có thứ tự.

const SHORT_SELECT = 'h-10 w-full rounded-md border border-input bg-transparent px-3'

export function CatalogItemForm({ type, item, onSaved, onCancel, onStale }) {
  const editing = !!item
  const [name, setName] = useState(item?.name ?? '')
  const [group, setGroup] = useState(item?.group ?? 'ACTIVE_INGREDIENT')
  const [order, setOrder] = useState(item?.order == null ? '' : String(item.order))
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const next = {}
    if (!name.trim()) next.name = 'Nhập tên'
    if (type === 'allergy-severities' && order.trim() && !/^\d+$/.test(order.trim())) next.order = 'Nhập số nguyên'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    const body = {
      name: name.trim(),
      ...(type === 'allergens' && { group }),
      ...(type === 'allergy-severities' && order.trim() && { order: Number(order) }),
    }
    setSaving(true)
    try {
      if (editing) {
        await putCatalogsByTypeById({
          path: { type, id: item.id },
          headers: { 'If-Match': `"${item.version}"` },
          body,
        })
      } else {
        await postCatalogsByType({ path: { type }, body })
      }
      onSaved()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-xl border p-4">
      <h3 className="type-card-title">{editing ? 'Sửa mục' : 'Thêm mục'}</h3>
      <FieldGroup>
        <Field data-invalid={!!errors.name || undefined}>
          <FieldLabel htmlFor="catalog-name">Tên</FieldLabel>
          <Input
            id="catalog-name"
            value={name}
            maxLength={200}
            onChange={(e) => setName(e.target.value)}
            aria-invalid={!!errors.name || undefined}
            className="h-10"
          />
          {errors.name && <FieldError>{errors.name}</FieldError>}
        </Field>
        {type === 'allergens' && (
          <Field>
            <FieldLabel htmlFor="catalog-group">Nhóm</FieldLabel>
            <select id="catalog-group" value={group} onChange={(e) => setGroup(e.target.value)} className={SHORT_SELECT}>
              {Object.entries(ALLERGEN_GROUPS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
        )}
        {type === 'allergy-severities' && (
          <Field data-invalid={!!errors.order || undefined}>
            <FieldLabel htmlFor="catalog-order">Thứ tự</FieldLabel>
            <Input
              id="catalog-order"
              inputMode="numeric"
              value={order}
              onChange={(e) => setOrder(e.target.value)}
              aria-invalid={!!errors.order || undefined}
              className="h-10 max-w-32 tabular-nums"
            />
            {errors.order && <FieldError>{errors.order}</FieldError>}
          </Field>
        )}
      </FieldGroup>

      <ProblemAlert problem={failure}>
        {failure?.status === 412 && (
          <Button type="button" variant="outline" size="sm" className="mt-2" onClick={onStale}>
            Tải lại
          </Button>
        )}
      </ProblemAlert>

      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={saving}>
          Lưu
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}
