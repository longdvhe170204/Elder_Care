import { useEffect, useState } from 'react'
import { getAreas, getFloors, getRooms, postAssets, postAssetsByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { Choices, Info, ReasonField, SELECT, TextField } from './FormParts'
import {
  BED_CODE,
  IMPACT_LEVELS,
  LOCATION_TYPES,
  MAINTENANCE_KINDS,
  MAINTENANCE_RESULTS,
  SOURCES,
  VEHICLE_CODE,
  flagLabels,
  statusLabel,
  wholeNumber,
} from './assetLabels'

// Khung chi tiết và khung ghi tăng tài sản (019 T018; bảng FR-013). Lệnh theo trạng thái và quyền (api-019 chưa có
// allowedCommands, chưa trả phiên bản nên chưa sửa mô tả): Phụ trách bộ phận (quyền Sửa) đưa vào sử dụng, chuyển vị trí,
// báo hỏng, bảo trì, ghi kết quả, đề nghị thanh lý; Trưởng tầng, Quản lý viện gỡ dấu chờ chuyển người; Quản lý viện
// chuyển bộ phận. Tài sản giường không có Đưa vào sử dụng, Chuyển vị trí (Q-242, Q-243). Backend kiểm lại người thực hiện.

const MANAGER = 'NURSING_HOME_MANAGER'

function asList(data) {
  return Array.isArray(data) ? data : (data?.items ?? [])
}

// Vị trí chuyển tới: loại, rồi chọn phòng, tầng, khu vực theo danh mục của 003; bãi xe, kho tài sản không cần mã
function LocationFields({ location, onChange, error }) {
  const [options, setOptions] = useState(null)
  useEffect(() => {
    let cancelled = false
    Promise.all([getAreas(), getFloors(), getRooms()])
      .then(([areas, floors, rooms]) => {
        if (!cancelled) setOptions({ AREA: asList(areas.data), FLOOR: asList(floors.data), ROOM: asList(rooms.data) })
      })
      .catch(() => !cancelled && setOptions({ AREA: [], FLOOR: [], ROOM: [] }))
    return () => {
      cancelled = true
    }
  }, [])
  const list = options?.[location.type] ?? null
  return (
    <>
      <Choices
        label="Vị trí mới"
        options={LOCATION_TYPES}
        value={location.type}
        onChange={(type) => onChange({ type, id: '' })}
        error={error?.type}
      />
      {list && (
        <Field data-invalid={!!error?.id || undefined}>
          <FieldLabel htmlFor="asset-location">Chọn nơi</FieldLabel>
          <select
            id="asset-location"
            value={location.id}
            onChange={(e) => onChange({ ...location, id: e.target.value })}
            aria-invalid={!!error?.id || undefined}
            className={SELECT}
          >
            <option value="">Chọn một nơi</option>
            {list.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
          </select>
          {error?.id && <FieldError>{error.id}</FieldError>}
        </Field>
      )}
    </>
  )
}

function locationBody(location) {
  const body = { type: location.type }
  if (['AREA', 'FLOOR', 'ROOM'].includes(location.type)) body.id = Number(location.id)
  return body
}

function locationError(location, submitted) {
  if (!submitted) return null
  if (!location.type) return { type: 'Chọn vị trí mới' }
  if (['AREA', 'FLOOR', 'ROOM'].includes(location.type) && !location.id) return { id: 'Chọn một nơi' }
  return null
}

export function AssetDetail({
  asset,
  typeName,
  departmentName,
  departments,
  typeCode,
  role,
  onChanged,
  Title,
  Description,
}) {
  const { can } = usePermissions()
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [description, setDescription] = useState('')
  const [impact, setImpact] = useState('')
  const [location, setLocation] = useState({ type: '', id: '' })
  const [kind, setKind] = useState('MAINTENANCE')
  const [result, setResult] = useState('')
  const [date, setDate] = useState('')
  const [content, setContent] = useState('')
  const [performedBy, setPerformedBy] = useState('')
  const [nextDueOn, setNextDueOn] = useState('')
  const [cost, setCost] = useState('')
  const [procurementRequestId, setProcurementRequestId] = useState('')
  const [repairDescription, setRepairDescription] = useState('')
  const [repairedBy, setRepairedBy] = useState('')
  const [newDepartmentId, setNewDepartmentId] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  const status = asset.status
  const bed = asset.bedId != null
  const vehicle = typeCode === VEHICLE_CODE
  const live = status !== 'DISPOSED'
  const lead = can('ASSET_VEHICLE', 'UPDATE')
  const manager = role === MANAGER
  const floorOrManager = role === 'FLOOR_MANAGER' || manager
  const flagged = (asset.flags ?? []).includes('AWAITING_RESIDENT_TRANSFER')
  const label = asset.code

  function open(next) {
    setMode(next)
    setSubmitted(false)
    setFailure(null)
  }

  async function run(command, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await postAssetsByIdByCommand({
        path: { id: asset.id, command },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body,
      })
      setMode(null)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  const show = (cond, text) => (submitted && cond ? text : null)
  const reasonError = show(!reason.trim(), 'Nhập lý do')
  const locError = locationError(location, submitted)

  function withReason(command, extra, message) {
    setSubmitted(true)
    if (!reason.trim()) return
    run(command, { ...extra, reason: reason.trim() }, message)
  }

  function putIntoUse() {
    setSubmitted(true)
    if (locError || !location.type) return
    run('put-into-use', { location: locationBody(location) }, `Đã đưa ${label} vào sử dụng`)
  }

  function relocate() {
    setSubmitted(true)
    if (!reason.trim() || locError || !location.type) return
    run('relocate', { location: locationBody(location), reason: reason.trim() }, `Đã chuyển vị trí ${label}`)
  }

  function reportDamage() {
    setSubmitted(true)
    if (!description.trim()) return
    const body = { description: description.trim() }
    if (impact) body.impactLevel = impact
    run('report-damage', body, `Đã báo hỏng ${label}`)
  }

  function startMaintenance() {
    run('start-maintenance', { maintenanceKind: kind }, `Đã đưa ${label} vào bảo trì`)
  }

  const resultErrors = {
    result: !result && 'Chọn Đạt hoặc Không đạt',
    date: !date && 'Chọn ngày thực hiện',
    content: !content.trim() && 'Nhập nội dung',
    performedBy: !performedBy.trim() && 'Nhập đơn vị thực hiện',
    cost: cost.trim() !== '' && wholeNumber(cost) === null && 'Nhập chi phí là số đồng',
    procurement: cost.trim() !== '' && !wholeNumber(procurementRequestId) && 'Nhập số yêu cầu mua sắm Đã duyệt',
  }

  function recordResult() {
    setSubmitted(true)
    if (Object.values(resultErrors).some(Boolean)) return
    const body = { result, date, content: content.trim(), performedBy: performedBy.trim() }
    if (nextDueOn) body.nextDueOn = nextDueOn
    if (cost.trim()) {
      body.outsourced = true
      body.cost = wholeNumber(cost)
      body.procurementRequestId = wholeNumber(procurementRequestId)
    }
    run('record-maintenance-result', body, `Đã ghi kết quả bảo trì ${label}`)
  }

  function removeFlag() {
    setSubmitted(true)
    if (!reason.trim()) return
    const body = { reason: reason.trim() }
    if (repairDescription.trim()) body.repairDescription = repairDescription.trim()
    if (repairedBy.trim()) body.repairedBy = repairedBy.trim()
    run('remove-transfer-flag', body, `Đã gỡ dấu chờ chuyển người của ${label}`)
  }

  function transferDepartment() {
    setSubmitted(true)
    if (!reason.trim() || !newDepartmentId) return
    run(
      'transfer-department',
      { newDepartmentId: Number(newDepartmentId), reason: reason.trim() },
      `Đã chuyển bộ phận quản lý ${label}`,
    )
  }

  const back = (
    <Button variant="ghost" className="h-11" onClick={() => open(null)}>
      Quay lại
    </Button>
  )
  const reasonField = <ReasonField id="asset-reason" value={reason} onChange={setReason} error={reasonError} />
  const kinds = vehicle ? MAINTENANCE_KINDS : MAINTENANCE_KINDS.slice(0, 2)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={statusLabel(status)} />
        <Title className="type-title">{`${asset.code}: ${asset.name}`}</Title>
        <Description className="type-body-sm text-muted-foreground">{typeName}</Description>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Info label="Bộ phận quản lý">{departmentName}</Info>
        <Info label="Vị trí">{asset.location ?? '—'}</Info>
        {bed && <Info label="Giường">{`Mã giường ${asset.bedId}`}</Info>}
        {(asset.flags ?? []).length > 0 && <Info label="Dấu">{flagLabels(asset.flags).join(', ')}</Info>}
      </div>
      <ProblemAlert problem={failure} />

      {mode === 'use' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <LocationFields location={location} onChange={setLocation} error={locError} />
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={putIntoUse}>
              Lưu đưa vào sử dụng
            </Button>
          </div>
        </div>
      )}

      {mode === 'relocate' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <LocationFields location={location} onChange={setLocation} error={locError} />
          {reasonField}
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={relocate}>
              Lưu chuyển vị trí
            </Button>
          </div>
        </div>
      )}

      {mode === 'damage' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <Field data-invalid={!!show(!description.trim(), 'x') || undefined}>
            <FieldLabel htmlFor="asset-damage">Mô tả hư hỏng</FieldLabel>
            <textarea
              id="asset-damage"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              aria-invalid={!!show(!description.trim(), 'x') || undefined}
              className="w-full rounded-md border border-input bg-transparent px-3 py-2"
            />
            {show(!description.trim(), 'x') && <FieldError>Nhập mô tả hư hỏng</FieldError>}
          </Field>
          {bed && (
            <>
              <Choices label="Mức ảnh hưởng" options={IMPACT_LEVELS} value={impact} onChange={setImpact} />
              <p className="type-body-sm text-muted-foreground">
                Bắt buộc khi giường đang có người ở hoặc đang giữ chỗ; backend sẽ báo nếu thiếu.
              </p>
            </>
          )}
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={reportDamage}>
              Lưu báo hỏng
            </Button>
          </div>
        </div>
      )}

      {mode === 'maintenance' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <Choices label="Loại lần" options={kinds} value={kind} onChange={setKind} />
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={startMaintenance}>
              Lưu đưa vào bảo trì
            </Button>
          </div>
        </div>
      )}

      {mode === 'result' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <FieldGroup>
            <Choices
              label="Kết quả"
              options={MAINTENANCE_RESULTS}
              value={result}
              onChange={setResult}
              error={show(true, resultErrors.result)}
            />
            <TextField
              id="asset-date"
              label="Ngày thực hiện"
              type="date"
              value={date}
              onChange={setDate}
              error={show(true, resultErrors.date)}
            />
            <TextField
              id="asset-content"
              label="Nội dung"
              value={content}
              onChange={setContent}
              error={show(true, resultErrors.content)}
            />
            <TextField
              id="asset-by"
              label="Đơn vị thực hiện"
              value={performedBy}
              onChange={setPerformedBy}
              error={show(true, resultErrors.performedBy)}
            />
            <TextField
              id="asset-next-due"
              label="Hạn kế tiếp (đăng kiểm, bảo hiểm)"
              type="date"
              value={nextDueOn}
              onChange={setNextDueOn}
            />
            <TextField
              id="asset-cost"
              label="Chi phí thuê ngoài (đồng, nếu có)"
              inputMode="numeric"
              value={cost}
              onChange={setCost}
              error={show(true, resultErrors.cost)}
            />
            {cost.trim() !== '' && (
              <TextField
                id="asset-procurement"
                label="Yêu cầu mua sắm đã duyệt (số)"
                inputMode="numeric"
                value={procurementRequestId}
                onChange={setProcurementRequestId}
                error={show(true, resultErrors.procurement)}
              />
            )}
          </FieldGroup>
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={recordResult}>
              Lưu kết quả
            </Button>
          </div>
        </div>
      )}

      {mode === 'flag' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          {reasonField}
          <TextField
            id="asset-repair"
            label="Nội dung đã sửa"
            value={repairDescription}
            onChange={setRepairDescription}
          />
          <TextField id="asset-repaired-by" label="Người, đơn vị sửa" value={repairedBy} onChange={setRepairedBy} />
          <p className="type-body-sm text-muted-foreground">
            Nội dung đã sửa và người, đơn vị sửa bắt buộc khi hư hỏng ở mức mất an toàn.
          </p>
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={removeFlag}>
              Lưu gỡ dấu
            </Button>
          </div>
        </div>
      )}

      {mode === 'disposal' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          {reasonField}
          <p className="type-body-sm text-muted-foreground">
            Tạo yêu cầu thanh lý chờ Quản lý viện duyệt ở màn Yêu cầu phê duyệt; tài sản chuyển Đã thanh lý khi được
            duyệt.
          </p>
          <div className="flex justify-end gap-2">
            {back}
            <Button
              variant="destructive"
              className="h-11"
              disabled={busy}
              onClick={() => withReason('request-disposal', {}, `Đã gửi đề nghị thanh lý ${label}`)}
            >
              Gửi đề nghị thanh lý
            </Button>
          </div>
        </div>
      )}

      {mode === 'department' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <Field data-invalid={!!show(!newDepartmentId, 'x') || undefined}>
            <FieldLabel htmlFor="asset-new-department">Bộ phận mới</FieldLabel>
            <select
              id="asset-new-department"
              value={newDepartmentId}
              onChange={(e) => setNewDepartmentId(e.target.value)}
              className={SELECT}
            >
              <option value="">Chọn một bộ phận</option>
              {(departments ?? [])
                .filter((d) => d.active !== false && d.id !== asset.departmentId)
                .map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
            </select>
            {show(!newDepartmentId, 'x') && <FieldError>Chọn bộ phận mới</FieldError>}
          </Field>
          {reasonField}
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={transferDepartment}>
              Lưu chuyển bộ phận
            </Button>
          </div>
        </div>
      )}

      {mode === null && live && (
        <div className="flex flex-wrap justify-end gap-2">
          {manager && (
            <Button variant="ghost" className="h-11" onClick={() => open('department')}>
              Chuyển bộ phận…
            </Button>
          )}
          {lead && (
            <Button variant="ghost" className="h-11" onClick={() => open('disposal')}>
              Đề nghị thanh lý…
            </Button>
          )}
          {lead && !bed && (
            <Button variant="outline" className="h-11" onClick={() => open('relocate')}>
              Chuyển vị trí…
            </Button>
          )}
          {lead && ['READY', 'IN_USE'].includes(status) && (
            <Button variant="outline" className="h-11" onClick={() => open('damage')}>
              Báo hỏng…
            </Button>
          )}
          {flagged && floorOrManager && (
            <Button variant="outline" className="h-11" onClick={() => open('flag')}>
              Gỡ dấu chờ chuyển người…
            </Button>
          )}
          {lead && ['READY', 'IN_USE', 'DAMAGED'].includes(status) && (
            <Button variant="outline" className="h-11" onClick={() => open('maintenance')}>
              Đưa vào bảo trì…
            </Button>
          )}
          {lead && status === 'UNDER_MAINTENANCE' && (
            <Button className="h-11" onClick={() => open('result')}>
              Ghi kết quả bảo trì…
            </Button>
          )}
          {lead && status === 'READY' && !bed && (
            <Button className="h-11" onClick={() => open('use')}>
              Đưa vào sử dụng…
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

/** Ghi tăng tài sản (FR-011, FR-008c): không phải loại giường; mua mới gắn yêu cầu mua sắm Đã duyệt và nguyên giá. */
export function CreateAssetSheet({ types, departments, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [typeId, setTypeId] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [source, setSource] = useState('')
  const [sourceDescription, setSourceDescription] = useState('')
  const [procurementRequestId, setProcurementRequestId] = useState('')
  const [acquisitionCost, setAcquisitionCost] = useState('')
  const [inServiceOn, setInServiceOn] = useState('')
  const [interval, setInterval] = useState('')
  const [dueOn, setDueOn] = useState('')
  const [plate, setPlate] = useState('')
  const [seats, setSeats] = useState('')
  const [inspectionDueOn, setInspectionDueOn] = useState('')
  const [insuranceDueOn, setInsuranceDueOn] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const selectable = (types ?? []).filter((t) => t.status === 'ACTIVE' && t.code !== BED_CODE)
  const vehicle = (types ?? []).find((t) => String(t.id) === typeId)?.code === VEHICLE_CODE
  const purchased = source === 'PURCHASED'
  const errors = {
    code: !code.trim() && 'Nhập mã tài sản',
    name: !name.trim() && 'Nhập tên tài sản',
    type: !typeId && 'Chọn loại tài sản',
    department: !departmentId && 'Chọn bộ phận quản lý',
    source: !source && 'Chọn nguồn',
    description: source && !purchased && !sourceDescription.trim() && 'Ghi nguồn của tài sản được tặng hoặc chuyển tới',
    request: purchased && !wholeNumber(procurementRequestId) && 'Nhập số yêu cầu mua sắm Đã duyệt',
    cost: purchased && wholeNumber(acquisitionCost) === null && 'Nhập nguyên giá',
    interval: interval.trim() !== '' && !wholeNumber(interval) && 'Nhập số ngày',
    seats: seats.trim() !== '' && !wholeNumber(seats) && 'Nhập số chỗ',
  }
  const show = (name) => (submitted && errors[name]) || null

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    const body = {
      code: code.trim(),
      typeId: Number(typeId),
      name: name.trim(),
      departmentId: Number(departmentId),
      source,
    }
    if (purchased) {
      body.procurementRequestId = wholeNumber(procurementRequestId)
      body.acquisitionCost = wholeNumber(acquisitionCost)
    } else {
      body.sourceDescription = sourceDescription.trim()
      if (acquisitionCost.trim() && wholeNumber(acquisitionCost) !== null)
        body.acquisitionCost = wholeNumber(acquisitionCost)
    }
    if (inServiceOn) body.inServiceOn = inServiceOn
    if (interval.trim()) body.maintenanceIntervalDays = wholeNumber(interval)
    if (dueOn) body.maintenanceDueOn = dueOn
    if (vehicle) {
      if (plate.trim()) body.licensePlate = plate.trim()
      if (seats.trim()) body.seatCount = wholeNumber(seats)
      if (inspectionDueOn) body.vehicleInspectionDueOn = inspectionDueOn
      if (insuranceDueOn) body.insuranceDueOn = insuranceDueOn
    }
    setBusy(true)
    setFailure(null)
    try {
      await postAssets({ headers: { 'Idempotency-Key': key }, body })
      onDone('Đã ghi tăng tài sản; vị trí mặc định là kho tài sản')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Ghi tăng tài sản</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Tài sản giường chỉ tạo kèm lệnh Tạo giường. Mua mới phải gắn yêu cầu mua sắm Đã duyệt của cùng bộ phận và có
          nguyên giá; hàng được tặng hoặc chuyển tới phải ghi nguồn.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <FieldGroup>
            <TextField id="asset-code" label="Mã tài sản" value={code} onChange={setCode} error={show('code')} />
            <TextField id="asset-name" label="Tên tài sản" value={name} onChange={setName} error={show('name')} />
            <Field data-invalid={!!show('type') || undefined}>
              <FieldLabel htmlFor="asset-type">Loại tài sản</FieldLabel>
              <select
                id="asset-type"
                value={typeId}
                onChange={(e) => setTypeId(e.target.value)}
                aria-invalid={!!show('type') || undefined}
                className={SELECT}
              >
                <option value="">{types === null ? 'Đang tải…' : 'Chọn một loại'}</option>
                {selectable.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
              {show('type') && <FieldError>{errors.type}</FieldError>}
            </Field>
            <Field data-invalid={!!show('department') || undefined}>
              <FieldLabel htmlFor="asset-department">Bộ phận quản lý</FieldLabel>
              <select
                id="asset-department"
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value)}
                aria-invalid={!!show('department') || undefined}
                className={SELECT}
              >
                <option value="">{departments === null ? 'Đang tải…' : 'Chọn một bộ phận'}</option>
                {(departments ?? [])
                  .filter((d) => d.active !== false)
                  .map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
              </select>
              {show('department') && <FieldError>{errors.department}</FieldError>}
            </Field>
            <Choices label="Nguồn" options={SOURCES} value={source} onChange={setSource} error={show('source')} />
            {purchased ? (
              <>
                <TextField
                  id="asset-request"
                  label="Yêu cầu mua sắm đã duyệt (số)"
                  inputMode="numeric"
                  value={procurementRequestId}
                  onChange={setProcurementRequestId}
                  error={show('request')}
                />
                <TextField
                  id="asset-cost"
                  label="Nguyên giá (đồng)"
                  inputMode="numeric"
                  value={acquisitionCost}
                  onChange={setAcquisitionCost}
                  error={show('cost')}
                />
              </>
            ) : (
              source && (
                <>
                  <TextField
                    id="asset-source"
                    label="Mô tả nguồn"
                    value={sourceDescription}
                    onChange={setSourceDescription}
                    error={show('description')}
                  />
                  <TextField
                    id="asset-cost"
                    label="Nguyên giá (đồng, không bắt buộc)"
                    inputMode="numeric"
                    value={acquisitionCost}
                    onChange={setAcquisitionCost}
                  />
                </>
              )
            )}
            <TextField
              id="asset-in-service"
              label="Ngày đưa vào sử dụng (không bắt buộc)"
              type="date"
              value={inServiceOn}
              onChange={setInServiceOn}
            />
            <div className="grid gap-3 sm:grid-cols-2">
              <TextField
                id="asset-interval"
                label="Chu kỳ bảo trì (ngày)"
                inputMode="numeric"
                value={interval}
                onChange={setInterval}
                error={show('interval')}
              />
              <TextField id="asset-due" label="Hạn bảo trì kế tiếp" type="date" value={dueOn} onChange={setDueOn} />
            </div>
            {vehicle && (
              <>
                <div className="grid gap-3 sm:grid-cols-2">
                  <TextField id="asset-plate" label="Biển số" value={plate} onChange={setPlate} />
                  <TextField
                    id="asset-seats"
                    label="Số chỗ"
                    inputMode="numeric"
                    value={seats}
                    onChange={setSeats}
                    error={show('seats')}
                  />
                </div>
                <div className="grid gap-3 sm:grid-cols-2">
                  <TextField
                    id="asset-inspection"
                    label="Hạn đăng kiểm"
                    type="date"
                    value={inspectionDueOn}
                    onChange={setInspectionDueOn}
                  />
                  <TextField
                    id="asset-insurance"
                    label="Hạn bảo hiểm"
                    type="date"
                    value={insuranceDueOn}
                    onChange={setInsuranceDueOn}
                  />
                </div>
              </>
            )}
          </FieldGroup>
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" className="h-11" onClick={onClose}>
              Quay lại
            </Button>
            <Button type="submit" className="h-11" disabled={busy}>
              Lưu tài sản
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
