import { useEffect, useState } from 'react'
import {
  getServicesByIdMismatchedPriceContracts,
  getServicesByIdPriceVersions,
  postServices,
  postServicesByIdDeactivate,
  postServicesByIdPriceVersions,
  putServicesById,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { CARE_LEVELS, SELECT, SERVICE_BILLING_PERIODS, SERVICE_TYPES, labelOf, money, newKey } from './admissionLabels'
import { Choices, Info, ReasonField } from './FormParts'

// Khung chi tiết và form của dịch vụ, đơn giá (004 T068; US9, FR-034 → FR-036, DBR-08, Q-297). Phiên bản đơn giá không có
// lệnh sửa; ngừng hiệu lực và sửa dịch vụ gửi If-Match theo version.

const etagOf = (service) => `"${service.version}"`

function PriceVersionForm({ serviceId, onDone, onCancel }) {
  const [price, setPrice] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const errors = { price: price === '' && 'Nhập đơn giá', from: !from && 'Nhập ngày hiệu lực' }

  async function save() {
    setSubmitted(true)
    if (errors.price || errors.from) return
    setBusy(true)
    try {
      await postServicesByIdPriceVersions({
        path: { id: serviceId },
        headers: newKey(),
        body: { unitPrice: Number(price), effectiveFrom: from, ...(to && { effectiveTo: to }) },
      })
      onDone('Đã tạo phiên bản đơn giá')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <FieldGroup>
        <Field data-invalid={(submitted && !!errors.price) || undefined}>
          <FieldLabel htmlFor="price-amount">Đơn giá (đồng)</FieldLabel>
          <Input id="price-amount" type="number" min="0" className="h-11" value={price} onChange={(e) => setPrice(e.target.value)} />
          {submitted && errors.price && <FieldError>{errors.price}</FieldError>}
        </Field>
        <Field data-invalid={(submitted && !!errors.from) || undefined}>
          <FieldLabel htmlFor="price-from">Hiệu lực từ</FieldLabel>
          <Input id="price-from" type="date" className="h-11" value={from} onChange={(e) => setFrom(e.target.value)} />
          {submitted && errors.from && <FieldError>{errors.from}</FieldError>}
        </Field>
        <Field>
          <FieldLabel htmlFor="price-to">Hiệu lực đến (không bắt buộc)</FieldLabel>
          <Input id="price-to" type="date" className="h-11" value={to} onChange={(e) => setTo(e.target.value)} />
        </Field>
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onCancel}>
          Đóng
        </Button>
        <Button className="h-11" disabled={busy} onClick={save}>
          Lưu phiên bản đơn giá
        </Button>
      </div>
    </div>
  )
}

export function ServiceDetail({ service, canUpdate, onChanged, onEdit, Title, Description }) {
  const [versions, setVersions] = useState(null)
  const [mismatched, setMismatched] = useState([])
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)
  const active = service.status === 'ACTIVE'

  useEffect(() => {
    let cancelled = false
    getServicesByIdPriceVersions({ path: { id: service.id } })
      .then(({ data }) => !cancelled && setVersions(data))
      .catch((e) => !cancelled && setFailure(problemOf(e)))
    // Chỉ Quản lý viện xem được; vai trò khác nhận 403 thì bỏ qua khối này
    getServicesByIdMismatchedPriceContracts({ path: { id: service.id } })
      .then(({ data }) => !cancelled && setMismatched(data))
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [service.id, reload])

  async function deactivate() {
    setSubmitted(true)
    if (!reason.trim()) return
    setBusy(true)
    try {
      await postServicesByIdDeactivate({
        path: { id: service.id },
        headers: { ...newKey(), 'If-Match': etagOf(service) },
        body: { reason: reason.trim() },
      })
      setMode(null)
      onChanged('Đã ngừng hiệu lực dịch vụ')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={active ? 'Hiệu lực' : 'Ngừng hiệu lực'} />
        <Title className="type-title">{service.name}</Title>
        <Description className="type-body-sm text-muted-foreground">{labelOf(SERVICE_TYPES, service.type)}</Description>
      </div>
      <div className="grid gap-3">
        <Info label="Đơn vị">{service.unit}</Info>
        <Info label="Kỳ tính">{labelOf(SERVICE_BILLING_PERIODS, service.billingPeriod)}</Info>
        {service.careLevelCode && <Info label="Mức chăm sóc">{labelOf(CARE_LEVELS, service.careLevelCode)}</Info>}
      </div>
      <ProblemAlert problem={failure} />

      <section className="flex flex-col gap-2">
        <h3 className="type-card-title">Phiên bản đơn giá</h3>
        {versions === null ? (
          <p className="type-body-sm text-muted-foreground">Đang tải…</p>
        ) : versions.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa có phiên bản đơn giá.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {versions.map((v) => (
              <li key={v.id} className="flex flex-col">
                <span className="type-body-sm tabular-nums">{money(v.unitPrice)}</span>
                <span className="type-caption text-muted-foreground">
                  {`Từ ${formatDate(v.effectiveFrom)}${v.effectiveTo ? ` đến ${formatDate(v.effectiveTo)}` : ', đang mở'}`}
                  {v.referenced ? '. Đã được hợp đồng tham chiếu, không sửa được' : ''}
                </span>
              </li>
            ))}
          </ul>
        )}
        {canUpdate && mode === null && (
          <div className="flex justify-end">
            <Button variant="outline" className="h-11" onClick={() => setMode('price')}>
              Thêm phiên bản đơn giá
            </Button>
          </div>
        )}
        {mode === 'price' && (
          <PriceVersionForm
            serviceId={service.id}
            onCancel={() => setMode(null)}
            onDone={(message) => {
              setMode(null)
              setReload((n) => n + 1)
              onChanged(message)
            }}
          />
        )}
      </section>

      {mismatched.length > 0 && (
        <section className="flex flex-col gap-2">
          <h3 className="type-card-title">Hợp đồng có đơn giá ghi khác phiên bản mới nhất</h3>
          <ul className="flex flex-col gap-1">
            {mismatched.map((m) => (
              <li key={m.contractId} className="type-body-sm tabular-nums">
                {`${m.contractNumber}: ghi ${money(m.recordedUnitPrice)}, mới nhất ${money(m.newUnitPrice)}`}
              </li>
            ))}
          </ul>
          <p className="type-caption text-muted-foreground">Hệ thống không tự đổi hợp đồng.</p>
        </section>
      )}

      {canUpdate && active && mode !== 'deactivate' && mode !== 'price' && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          <Button variant="outline" className="h-11" onClick={() => onEdit(service)}>
            Sửa dịch vụ
          </Button>
          <Button variant="outline" className="h-11" onClick={() => setMode('deactivate')}>
            Ngừng hiệu lực dịch vụ
          </Button>
        </div>
      )}
      {mode === 'deactivate' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <ReasonField id="service-reason" value={reason} onChange={setReason} error={submitted && !reason.trim() ? 'Nhập lý do' : null} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
              Đóng
            </Button>
            <Button variant="destructive" className="h-11" disabled={busy} onClick={deactivate}>
              Xác nhận ngừng hiệu lực
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

export function ServiceSheet({ service, onClose, onDone }) {
  const editing = !!service
  const [type, setType] = useState(service?.type ?? '')
  const [name, setName] = useState(service?.name ?? '')
  const [unit, setUnit] = useState(service?.unit ?? '')
  const [period, setPeriod] = useState(service?.billingPeriod ?? '')
  const [level, setLevel] = useState(service?.careLevelCode ?? '')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const errors = {
    type: !type && 'Chọn loại dịch vụ',
    name: !name.trim() && 'Nhập tên dịch vụ',
    unit: !unit.trim() && 'Nhập đơn vị',
    period: !period && 'Chọn kỳ tính',
    level: type === 'CARE' && !level && 'Chọn mức chăm sóc của dịch vụ chăm sóc',
  }
  const shown = (key) => (submitted ? errors[key] : null)

  async function save() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    const body = {
      type,
      name: name.trim(),
      unit: unit.trim(),
      billingPeriod: period,
      ...(type === 'CARE' && { careLevelCode: level }),
    }
    try {
      if (editing) {
        await putServicesById({ path: { id: service.id }, headers: { ...newKey(), 'If-Match': etagOf(service) }, body })
        onDone('Đã sửa dịch vụ')
      } else {
        await postServices({ headers: newKey(), body })
        onDone('Đã tạo dịch vụ')
      }
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle>{editing ? 'Sửa dịch vụ' : 'Tạo dịch vụ'}</SheetTitle>
        <SheetDescription>Dịch vụ chăm sóc gắn đúng một mức; mỗi mức tối đa một dịch vụ chăm sóc hiệu lực.</SheetDescription>
        <FieldGroup className="mt-4">
          <Field data-invalid={!!shown('type') || undefined}>
            <FieldLabel htmlFor="service-type">Loại dịch vụ</FieldLabel>
            <select id="service-type" className={SELECT} value={type} onChange={(e) => setType(e.target.value)}>
              <option value="">Chọn loại dịch vụ</option>
              {SERVICE_TYPES.map(([code, text]) => (
                <option key={code} value={code}>
                  {text}
                </option>
              ))}
            </select>
            {shown('type') && <FieldError>{shown('type')}</FieldError>}
          </Field>
          <Field data-invalid={!!shown('name') || undefined}>
            <FieldLabel htmlFor="service-name">Tên dịch vụ</FieldLabel>
            <Input id="service-name" className="h-11" value={name} onChange={(e) => setName(e.target.value)} />
            {shown('name') && <FieldError>{shown('name')}</FieldError>}
          </Field>
          <Field data-invalid={!!shown('unit') || undefined}>
            <FieldLabel htmlFor="service-unit">Đơn vị</FieldLabel>
            <Input id="service-unit" className="h-11" value={unit} onChange={(e) => setUnit(e.target.value)} />
            {shown('unit') && <FieldError>{shown('unit')}</FieldError>}
          </Field>
          <Choices label="Kỳ tính" options={SERVICE_BILLING_PERIODS} value={period} onChange={setPeriod} error={shown('period')} />
          {type === 'CARE' && (
            <Choices label="Mức chăm sóc" options={CARE_LEVELS} value={level} onChange={setLevel} error={shown('level')} />
          )}
        </FieldGroup>
        <div className="mt-4">
          <ProblemAlert problem={failure} />
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" className="h-11" onClick={onClose}>
            Đóng
          </Button>
          <Button className="h-11" disabled={busy} onClick={save}>
            Lưu dịch vụ
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
