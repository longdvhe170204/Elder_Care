import { useCallback, useEffect, useState } from 'react'
import {
  ApiError,
  getCatalogsByType,
  getResidentsByIdHealthItems,
  postHealthItemsByIdExclude,
  postResidentsByIdHealthItems,
} from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { AllergyNote } from '@/common/CareNotes'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { CATALOG_TYPES, HEALTH_TYPE_LABELS, HEALTH_TYPE_ORDER } from './healthLabels'

// Dị ứng, bệnh nền, tiền sử bệnh (001 T033; US2; FR-018 → FR-022; DBR-04; hợp đồng GET, POST
// /residents/{id}/health-items, POST /health-items/{id}/exclude). Mỗi mục chỉ ghi nhận và loại trừ có lý do: không có
// nút Sửa, Xóa. Dị ứng luôn hiện thành khối riêng tông attention (DESIGN.md 7.2). Nút theo allowedCommands.

const STATUS_FILTERS = [
  { value: 'ACTIVE', label: 'Hiệu lực' },
  { value: 'EXCLUDED', label: 'Đã loại trừ' },
  { value: 'ALL', label: 'Tất cả' },
]

const KINDS = [
  { value: 'ALLERGY_CATALOG', label: 'Dị ứng (danh mục)' },
  { value: 'ALLERGY_OTHER', label: 'Dị ứng khác, nhập tay' },
  { value: 'CHRONIC_CONDITION', label: 'Bệnh nền' },
  { value: 'MEDICAL_HISTORY', label: 'Tiền sử bệnh' },
]

function nameOf(item) {
  return item.allergen?.name ?? item.freeText
}

function SelectField({ id, label, value, onChange, options, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <select
        id={id}
        value={value}
        onChange={onChange}
        aria-invalid={!!error || undefined}
        className="h-10 w-full rounded-lg border border-input bg-transparent px-3 text-base"
      >
        <option value="">Chọn</option>
        {options.map((o) => (
          <option key={o.id} value={o.id}>
            {o.name}
          </option>
        ))}
      </select>
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

function ExcludeForm({ item, onDone, onCancel }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    if (!reason.trim()) {
      setError('Nhập lý do loại trừ')
      return
    }
    setError('')
    setFailure(null)
    setBusy(true)
    try {
      await postHealthItemsByIdExclude({
        path: { id: item.id },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { reason: reason.trim() },
      })
      onDone()
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-2 flex flex-col gap-3 rounded-lg bg-muted p-3">
      <Field data-invalid={!!error || undefined}>
        <FieldLabel htmlFor={`exclude-reason-${item.id}`}>Lý do loại trừ</FieldLabel>
        <Textarea
          id={`exclude-reason-${item.id}`}
          rows={2}
          maxLength={2000}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={!!error || undefined}
        />
        {error && <FieldError>{error}</FieldError>}
      </Field>
      <ProblemAlert problem={failure} />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={busy}>
          Xác nhận loại trừ
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}

function ItemRow({ item, canExclude, excluding, onExclude, onCancel, onExcluded }) {
  const excluded = item.status === 'EXCLUDED'
  return (
    <li className="flex flex-col gap-1 rounded-lg border p-3">
      <div className="flex flex-wrap items-center gap-2">
        <span className="font-medium">{nameOf(item)}</span>
        {item.type === 'ALLERGY' && item.severity && (
          <span className="type-body-sm text-muted-foreground">Mức độ: {item.severity}</span>
        )}
        {excluded && <StatusBadge status="Đã loại trừ" />}
        {item.notAutoChecked && <Badge variant="attention">Không kiểm tra tự động</Badge>}
        {item.pendingCheck && !excluded && <Badge variant="neutral">Chờ kiểm tra thuốc và chế độ ăn</Badge>}
      </div>
      <p className="type-caption text-muted-foreground">
        Nguồn: {item.source}. Ghi bởi {item.recordedBy}, {formatDate(item.at)}
      </p>
      {excluded && item.exclusion && (
        <p className="type-caption text-muted-foreground">
          Loại trừ bởi {item.exclusion.by}, {formatDate(item.exclusion.at)}. Lý do: {item.exclusion.reason}
        </p>
      )}
      {!excluded && canExclude && !excluding && (
        <Button type="button" variant="outline" size="sm" className="self-start" onClick={onExclude}>
          Loại trừ
        </Button>
      )}
      {excluding && <ExcludeForm item={item} onDone={onExcluded} onCancel={onCancel} />}
    </li>
  )
}

function bodyOf(kind, form) {
  const body = { type: kind.startsWith('ALLERGY') ? 'ALLERGY' : kind, sourceId: Number(form.sourceId) }
  if (kind === 'ALLERGY_CATALOG') body.allergenId = Number(form.allergenId)
  else body.freeText = form.freeText.trim()
  if (kind.startsWith('ALLERGY')) body.severityId = Number(form.severityId)
  return body
}

function RecordItemForm({ residentId, onRecorded, onCancel }) {
  const [catalogs, setCatalogs] = useState(null)
  const [kind, setKind] = useState('ALLERGY_CATALOG')
  const [form, setForm] = useState({ allergenId: '', freeText: '', severityId: '', sourceId: '' })
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [duplicate, setDuplicate] = useState(false)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function loadCatalogs() {
      try {
        const query = { status: 'ACTIVE' }
        const [allergens, severities, sources] = await Promise.all([
          getCatalogsByType({ path: { type: CATALOG_TYPES.allergens }, query }),
          getCatalogsByType({ path: { type: CATALOG_TYPES.severities }, query }),
          getCatalogsByType({ path: { type: CATALOG_TYPES.sources }, query }),
        ])
        if (!cancelled) setCatalogs({ allergens: allergens.data, severities: severities.data, sources: sources.data })
      } catch (error) {
        if (!cancelled) setFailure(problemOf(error))
      }
    }
    loadCatalogs()
    return () => {
      cancelled = true
    }
  }, [])

  const set = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))

  function validate() {
    const next = {}
    if (kind === 'ALLERGY_CATALOG' && !form.allergenId) next.allergenId = 'Chọn dị nguyên'
    if (kind !== 'ALLERGY_CATALOG' && !form.freeText.trim()) next.freeText = 'Nhập nội dung'
    if (kind.startsWith('ALLERGY') && !form.severityId) next.severityId = 'Chọn mức độ'
    if (!form.sourceId) next.sourceId = 'Chọn nguồn thông tin'
    return next
  }

  async function submit(confirmDuplicate) {
    const next = validate()
    setErrors(next)
    setFailure(null)
    if (Object.keys(next).length > 0) return
    setBusy(true)
    try {
      const { data } = await postResidentsByIdHealthItems({
        path: { id: residentId },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { ...bodyOf(kind, form), ...(confirmDuplicate && { confirmDuplicate: true }) },
      })
      onRecorded(data.warnings ?? [])
    } catch (error) {
      if (error instanceof ApiError && error.problem?.errorCode === 'DUPLICATE_HEALTH_ITEM') setDuplicate(true)
      else setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (!catalogs) {
    return failure ? (
      <ProblemAlert problem={failure} />
    ) : (
      <div aria-busy="true">
        <span className="sr-only">Đang tải danh mục</span>
        <Skeleton className="h-24" />
      </div>
    )
  }

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        setDuplicate(false)
        submit(false)
      }}
      noValidate
      className="flex flex-col gap-4 rounded-lg border p-3"
    >
      <Field>
        <FieldLabel>Loại mục</FieldLabel>
        <ToggleGroup
          variant="outline"
          value={[kind]}
          onValueChange={(next) => {
            if (next[0]) {
              setKind(next[0])
              setErrors({})
              setDuplicate(false)
            }
          }}
          className="flex-wrap"
        >
          {KINDS.map((k) => (
            <ToggleGroupItem key={k.value} value={k.value} className="h-11 px-4">
              {k.label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </Field>
      <FieldGroup>
        {kind === 'ALLERGY_CATALOG' ? (
          <SelectField
            id="health-allergen"
            label="Dị nguyên"
            value={form.allergenId}
            onChange={set('allergenId')}
            options={catalogs.allergens}
            error={errors.allergenId}
          />
        ) : (
          <Field data-invalid={!!errors.freeText || undefined}>
            <FieldLabel htmlFor="health-free-text">Nội dung</FieldLabel>
            <Textarea
              id="health-free-text"
              rows={2}
              maxLength={1000}
              value={form.freeText}
              onChange={set('freeText')}
              aria-invalid={!!errors.freeText || undefined}
            />
            {errors.freeText && <FieldError>{errors.freeText}</FieldError>}
          </Field>
        )}
        {kind.startsWith('ALLERGY') && (
          <SelectField
            id="health-severity"
            label="Mức độ"
            value={form.severityId}
            onChange={set('severityId')}
            options={catalogs.severities}
            error={errors.severityId}
          />
        )}
        <SelectField
          id="health-source"
          label="Nguồn thông tin"
          value={form.sourceId}
          onChange={set('sourceId')}
          options={catalogs.sources}
          error={errors.sourceId}
        />
      </FieldGroup>
      {kind === 'ALLERGY_OTHER' && (
        <Alert variant="attention">
          <AlertTitle>Dị ứng nhập tay không kiểm tra tự động</AlertTitle>
          <AlertDescription>
            Xung đột với thuốc và chế độ ăn sẽ không được phát hiện tự động. Nên chọn từ danh mục khi dị nguyên đã có.
          </AlertDescription>
        </Alert>
      )}
      <ProblemAlert problem={failure} />
      {duplicate && (
        <Alert variant="attention">
          <AlertTitle>Đã có mục đang hiệu lực trùng nội dung</AlertTitle>
          <AlertDescription>
            <p>Kiểm tra danh sách rồi quyết định có ghi thêm không.</p>
            <Button type="button" variant="outline" className="h-11 self-start" onClick={() => submit(true)}>
              Vẫn ghi nhận mục này
            </Button>
          </AlertDescription>
        </Alert>
      )}
      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={busy}>
          Ghi nhận
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}

export function HealthItems({ residentId, allowedCommands }) {
  const [filter, setFilter] = useState('ACTIVE')
  const [items, setItems] = useState(null)
  const [failure, setFailure] = useState(null)
  const [recording, setRecording] = useState(false)
  const [excludingId, setExcludingId] = useState(null)
  const [warnings, setWarnings] = useState([])

  const canRecord = allowedCommands.includes('RECORD_HEALTH_ITEM')
  const canExclude = allowedCommands.includes('EXCLUDE_HEALTH_ITEM')

  const load = useCallback(
    async (status) => {
      try {
        const { data } = await getResidentsByIdHealthItems({ path: { id: residentId }, query: { status } })
        setItems(data)
        setFailure(null)
      } catch (error) {
        setItems([])
        setFailure(problemOf(error))
      }
    },
    [residentId],
  )

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp danh sách khi mở tab hoặc đổi bộ lọc
    load(filter)
  }, [load, filter])

  const groups = HEALTH_TYPE_ORDER.map((type) => ({ type, rows: (items ?? []).filter((i) => i.type === type) })).filter(
    (g) => g.rows.length > 0,
  )
  const activeAllergies = (items ?? [])
    .filter((i) => i.type === 'ALLERGY' && i.status === 'ACTIVE')
    .map((i) => (i.severity ? `${nameOf(i)} (${i.severity})` : nameOf(i)))

  return (
    <section aria-labelledby="health-items-title" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 id="health-items-title" className="type-card-title">
          Dị ứng, bệnh nền, tiền sử bệnh
        </h3>
        {canRecord && !recording && (
          <Button type="button" className="h-11" onClick={() => setRecording(true)}>
            Ghi nhận mục
          </Button>
        )}
      </div>

      {recording && (
        <RecordItemForm
          residentId={residentId}
          onCancel={() => setRecording(false)}
          onRecorded={(next) => {
            setRecording(false)
            setWarnings(next)
            load(filter)
          }}
        />
      )}

      {warnings.length > 0 && (
        <Alert variant="attention">
          <AlertTitle>Đã ghi nhận, cần chú ý</AlertTitle>
          <AlertDescription>
            {warnings.map((w) => (
              <p key={w.code}>{w.description}</p>
            ))}
          </AlertDescription>
        </Alert>
      )}

      <ToggleGroup
        variant="filter"
        value={[filter]}
        onValueChange={(next) => {
          if (next[0]) {
            setFilter(next[0])
            setExcludingId(null)
          }
        }}
        aria-label="Lọc mục sức khỏe theo trạng thái"
        className="flex-wrap"
      >
        {STATUS_FILTERS.map((f) => (
          <ToggleGroupItem key={f.value} value={f.value} className="h-11 px-4">
            {f.label}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>

      <ProblemAlert problem={failure} />

      {items === null && (
        <div aria-busy="true">
          <span className="sr-only">Đang tải mục sức khỏe</span>
          <Skeleton className="h-16" />
        </div>
      )}

      {items && items.length === 0 && !failure && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Chưa ghi nhận dị ứng, bệnh nền hay tiền sử bệnh</EmptyTitle>
            <EmptyDescription>
              {filter === 'EXCLUDED' ? 'Chưa có mục nào bị loại trừ.' : 'Mục ghi nhận xong sẽ hiện ở đây.'}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}

      {activeAllergies.length > 0 && <AllergyNote allergies={activeAllergies} />}

      {groups.length > 0 && (
        <ul aria-label="Mục sức khỏe" className="flex flex-col gap-4">
          {groups.map((g) => (
            <li key={g.type} className="flex flex-col gap-2">
              <h4 className="type-caption text-muted-foreground">{HEALTH_TYPE_LABELS[g.type]}</h4>
              <ul className="flex flex-col gap-2">
                {g.rows.map((item) => (
                  <ItemRow
                    key={item.id}
                    item={item}
                    canExclude={canExclude}
                    excluding={excludingId === item.id}
                    onExclude={() => setExcludingId(item.id)}
                    onCancel={() => setExcludingId(null)}
                    onExcluded={() => {
                      setExcludingId(null)
                      load(filter)
                    }}
                  />
                ))}
              </ul>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
