import { useEffect, useState } from 'react'
import {
  getContracts,
  getResidentsById,
  getServices,
  postResidencyChangeRequests,
  putResidencyChangeRequestsById,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { CARE_LEVELS, CHANGE_TYPES, RESIDENCE_TYPES, SELECT, labelOf, money, newKey } from './admissionLabels'
import { Choices, Info, ReasonField } from './FormParts'

// Yêu cầu thay đổi lưu trú (004 T047; US5, FR-037 → FR-047). Lập, sửa ở Nháp; gửi duyệt, duyệt, từ chối ở màn Yêu cầu phê
// duyệt (api-000). Giá trị trước và ảnh hưởng chi phí do hệ thống điền (FR-038).

const REQUEST_STATUS = {
  DRAFT: 'Nháp',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  APPLIED: 'Đã áp dụng',
  APPLY_FAILED: 'Áp dụng không thành',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
}

function valueText(type, value) {
  if (!value) return '—'
  switch (type) {
    case 'RESIDENCE_TYPE':
      return labelOf(RESIDENCE_TYPES, value.residenceType)
    case 'CARE_LEVEL':
      return labelOf(CARE_LEVELS, value.careLevel)
    case 'DURATION':
      return value.endDate ? formatDate(value.endDate) : '—'
    case 'BED':
      return value.newBedId ? `Giường mã ${value.newBedId}` : '—'
    case 'SERVICE':
      return `${(value.services ?? []).length} dịch vụ`
    default:
      return `${(value.arrivalSchedule ?? []).length} dòng lịch`
  }
}

export function ChangeRequestView({ request, canUpdate, onEdit, onReload }) {
  const [contractName, setContractName] = useState('')

  useEffect(() => {
    let cancelled = false
    getContracts({ query: {} })
      .then(({ data }) => {
        const contract = data.find((c) => c.id === request.contractId)
        if (!contract) return
        return getResidentsById({ path: { id: contract.residentId } }).then(({ data: r }) => {
          if (!cancelled) setContractName(`${r.personalInfo?.fullName ?? ''}, ${contract.contractNumber}`)
        })
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [request.contractId])

  return (
    <div className="flex flex-col gap-4 rounded-2xl border bg-card p-5">
      <div className="flex flex-col gap-1">
        <StatusBadge status={REQUEST_STATUS[request.status] ?? request.status} />
        <h2 className="type-title">{contractName || `Hợp đồng mã ${request.contractId}`}</h2>
        <p className="type-body-sm text-muted-foreground">
          {`Ngày hiệu lực mong muốn ${formatDate(request.requestedEffectiveDate)}${request.requestedBy ? `, lập bởi ${request.requestedBy}` : ''}`}
        </p>
      </div>
      <Info label="Lý do">{request.reason}</Info>
      <section className="flex flex-col gap-2">
        <h3 className="type-card-title">Nội dung thay đổi</h3>
        <ul className="flex flex-col gap-2">
          {request.changeTypes.map((type) => (
            <li key={type} className="flex flex-col">
              <span className="type-body-sm">{labelOf(CHANGE_TYPES, type)}</span>
              <span className="type-caption text-muted-foreground">
                {`Trước: ${valueText(type, request.previousValue)}; sau: ${valueText(type, request.newValue)}`}
              </span>
            </li>
          ))}
        </ul>
      </section>
      {request.costImpact?.length > 0 && (
        <section className="flex flex-col gap-2">
          <h3 className="type-card-title">Ảnh hưởng chi phí</h3>
          <ul className="flex flex-col gap-1">
            {request.costImpact.map((c) => (
              <li key={c.item} className="type-body-sm tabular-nums">
                {`${c.item}: ${money(c.previousUnitPrice)} → ${money(c.newUnitPrice)}`}
              </li>
            ))}
          </ul>
        </section>
      )}
      {request.minorChange && <p className="type-body-sm">Thay đổi nhỏ: Hành chính duyệt được.</p>}
      {request.addendumId && <p className="type-body-sm">{`Đã tạo phụ lục mã ${request.addendumId}.`}</p>}
      <p className="type-caption text-muted-foreground">
        {request.status === 'DRAFT'
          ? `Gửi duyệt ở màn Yêu cầu phê duyệt (mã yêu cầu ${request.approvalRequestId}).`
          : `Yêu cầu phê duyệt mã ${request.approvalRequestId}.`}
      </p>
      <div className="flex flex-wrap justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onReload}>
          Tải lại
        </Button>
        {request.status === 'DRAFT' && canUpdate && (
          <Button className="h-11" onClick={() => onEdit(request)}>
            Sửa yêu cầu nháp
          </Button>
        )}
      </div>
    </div>
  )
}

export function ChangeRequestSheet({ request, etag, onClose, onDone }) {
  const editing = !!request
  const [contracts, setContracts] = useState([])
  const [names, setNames] = useState({})
  const [catalog, setCatalog] = useState([])
  const [contractId, setContractId] = useState(request ? String(request.contractId) : '')
  const [types, setTypes] = useState(request?.changeTypes ?? [])
  const [value, setValue] = useState(request?.newValue ?? {})
  const [date, setDate] = useState(request?.requestedEffectiveDate ?? '')
  const [reason, setReason] = useState(request?.reason ?? '')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    getContracts({ query: { status: ['ACTIVE'] } })
      .then(({ data }) => {
        const active = data.filter((c) => c.status === 'ACTIVE')
        setContracts(active)
        for (const c of active)
          getResidentsById({ path: { id: c.residentId } })
            .then(({ data: r }) => setNames((n) => ({ ...n, [c.residentId]: r.personalInfo?.fullName })))
            .catch(() => {})
      })
      .catch((e) => setFailure(problemOf(e)))
    getServices({ query: {} })
      .then(({ data }) => setCatalog(data.filter((s) => s.status === 'ACTIVE')))
      .catch(() => {})
  }, [])

  const set = (patch) => setValue((v) => ({ ...v, ...patch }))
  const toggleType = (type) => setTypes((t) => (t.includes(type) ? t.filter((x) => x !== type) : [...t, type]))
  const pickedServices = value.services ?? []
  const toggleService = (id) =>
    set({
      services: pickedServices.some((s) => s.serviceId === id)
        ? pickedServices.filter((s) => s.serviceId !== id)
        : [...pickedServices, { serviceId: id, inPackage: false }],
    })

  const errors = {
    contract: !contractId && 'Chọn hợp đồng',
    types: types.length === 0 && 'Chọn ít nhất một loại thay đổi',
    date: !date && 'Nhập ngày hiệu lực mong muốn',
    reason: !reason.trim() && 'Nhập lý do',
  }
  const shown = (key) => (submitted ? errors[key] : null)

  async function save() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    // Chỉ gửi phần giá trị mới của các loại đã chọn
    const keep = {
      RESIDENCE_TYPE: ['residenceType'],
      CARE_LEVEL: ['careLevel'],
      DURATION: ['endDate'],
      BED: ['newBedId'],
      SERVICE: ['services'],
      ARRIVAL_SCHEDULE: ['arrivalSchedule'],
    }
    const newValue = {}
    for (const type of types) for (const key of keep[type]) if (value[key] !== undefined) newValue[key] = value[key]
    const body = {
      contractId: Number(contractId),
      changeTypes: types,
      newValue,
      requestedEffectiveDate: date,
      reason: reason.trim(),
    }
    try {
      if (editing) {
        await putResidencyChangeRequestsById({
          path: { id: request.id },
          headers: { ...newKey(), 'If-Match': etag },
          body,
        })
        onDone('Đã sửa yêu cầu nháp', request.id)
      } else {
        const { data } = await postResidencyChangeRequests({ headers: newKey(), body })
        onDone('Đã lập yêu cầu thay đổi lưu trú ở Nháp', data.id)
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
        <SheetTitle>{editing ? 'Sửa yêu cầu thay đổi lưu trú' : 'Lập yêu cầu thay đổi lưu trú'}</SheetTitle>
        <SheetDescription>
          Yêu cầu ở Nháp; giá trị trước và ảnh hưởng chi phí do hệ thống điền khi lưu.
        </SheetDescription>
        <FieldGroup className="mt-4">
          <Field data-invalid={!!shown('contract') || undefined}>
            <FieldLabel htmlFor="change-contract">Hợp đồng</FieldLabel>
            <select
              id="change-contract"
              className={SELECT}
              value={contractId}
              disabled={editing}
              onChange={(e) => setContractId(e.target.value)}
            >
              <option value="">Hợp đồng đang hiệu lực</option>
              {contracts.map((c) => (
                <option key={c.id} value={c.id}>
                  {`${names[c.residentId] ?? `Mã ${c.residentId}`}, ${c.contractNumber}`}
                </option>
              ))}
            </select>
            {shown('contract') && <FieldError>{shown('contract')}</FieldError>}
          </Field>

          <Field data-invalid={!!shown('types') || undefined}>
            <FieldLabel>Loại thay đổi</FieldLabel>
            <div className="flex flex-col gap-2">
              {CHANGE_TYPES.map(([code, text]) => (
                <label key={code} className="flex items-center gap-2 type-body-sm">
                  <input type="checkbox" checked={types.includes(code)} onChange={() => toggleType(code)} />
                  {text}
                </label>
              ))}
            </div>
            {shown('types') && <FieldError>{shown('types')}</FieldError>}
          </Field>

          {types.includes('RESIDENCE_TYPE') && (
            <Choices
              label="Loại lưu trú mới"
              options={RESIDENCE_TYPES}
              value={value.residenceType ?? ''}
              onChange={(v) => set({ residenceType: v })}
            />
          )}
          {types.includes('CARE_LEVEL') && (
            <Choices
              label="Mức chăm sóc mới"
              options={CARE_LEVELS}
              value={value.careLevel ?? ''}
              onChange={(v) => set({ careLevel: v })}
            />
          )}
          {types.includes('DURATION') && (
            <Field>
              <FieldLabel htmlFor="change-end">Ngày kết thúc mới</FieldLabel>
              <Input
                id="change-end"
                type="date"
                className="h-11"
                value={value.endDate ?? ''}
                onChange={(e) => set({ endDate: e.target.value })}
              />
            </Field>
          )}
          {types.includes('BED') && (
            <Field>
              <FieldLabel htmlFor="change-bed">Mã giường mới</FieldLabel>
              <Input
                id="change-bed"
                type="number"
                min="1"
                className="h-11"
                value={value.newBedId ?? ''}
                onChange={(e) => set({ newBedId: Number(e.target.value) })}
              />
            </Field>
          )}
          {types.includes('SERVICE') && (
            <Field>
              <FieldLabel>Dịch vụ mới của hợp đồng</FieldLabel>
              {catalog.map((s) => (
                <label key={s.id} className="flex items-center gap-2 type-body-sm">
                  <input
                    type="checkbox"
                    checked={pickedServices.some((x) => x.serviceId === s.id)}
                    onChange={() => toggleService(s.id)}
                  />
                  {s.name}
                </label>
              ))}
            </Field>
          )}
          {types.includes('ARRIVAL_SCHEDULE') && (
            <Field>
              <FieldLabel>Lịch đến theo tuần</FieldLabel>
              {(value.arrivalSchedule ?? []).map((row, i) => (
                <div key={i} className="flex flex-wrap items-center gap-2">
                  <Input
                    aria-label={`Thứ dòng ${i + 1} (1 đến 7)`}
                    type="number"
                    min="1"
                    max="7"
                    className="h-11 w-20"
                    value={row.dayOfWeek ?? ''}
                    onChange={(e) =>
                      set({
                        arrivalSchedule: value.arrivalSchedule.map((r, j) =>
                          j === i ? { ...r, dayOfWeek: Number(e.target.value) } : r,
                        ),
                      })
                    }
                  />
                  <Input
                    aria-label={`Giờ đến dòng ${i + 1}`}
                    type="time"
                    className="h-11 w-32"
                    value={row.arrivalTime ?? ''}
                    onChange={(e) =>
                      set({
                        arrivalSchedule: value.arrivalSchedule.map((r, j) =>
                          j === i ? { ...r, arrivalTime: e.target.value } : r,
                        ),
                      })
                    }
                  />
                  <Input
                    aria-label={`Giờ về dòng ${i + 1}`}
                    type="time"
                    className="h-11 w-32"
                    value={row.departureTime ?? ''}
                    onChange={(e) =>
                      set({
                        arrivalSchedule: value.arrivalSchedule.map((r, j) =>
                          j === i ? { ...r, departureTime: e.target.value } : r,
                        ),
                      })
                    }
                  />
                </div>
              ))}
              <Button
                variant="outline"
                className="h-11 self-start"
                onClick={() => set({ arrivalSchedule: [...(value.arrivalSchedule ?? []), {}] })}
              >
                Thêm dòng lịch
              </Button>
            </Field>
          )}

          <Field data-invalid={!!shown('date') || undefined}>
            <FieldLabel htmlFor="change-date">Ngày hiệu lực mong muốn</FieldLabel>
            <Input
              id="change-date"
              type="date"
              className="h-11"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
            {shown('date') && <FieldError>{shown('date')}</FieldError>}
          </Field>
          <ReasonField id="change-reason" value={reason} onChange={setReason} error={shown('reason')} />
        </FieldGroup>
        <div className="mt-4">
          <ProblemAlert problem={failure} />
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" className="h-11" onClick={onClose}>
            Đóng
          </Button>
          <Button className="h-11" disabled={busy || (editing && !etag)} onClick={save}>
            Lưu yêu cầu
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
