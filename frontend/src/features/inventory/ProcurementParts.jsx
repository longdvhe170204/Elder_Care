import { useEffect, useRef, useState } from 'react'
import {
  getResidents,
  getResidentsByResidentIdPrescriptions,
  postProcurementRequests,
  postProcurementRequestsByIdByCommand,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { uploadFile } from '@/common/files'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import {
  PRIORITIES,
  PROCUREMENT_STATUS_LABELS,
  PROCUREMENT_TYPES,
  RESOLUTIONS,
  facilityUnended,
  lineAmount,
  money,
  priorityLabel,
  receivedLabel,
  typeLabel,
  vietnamIso,
} from './procurementLabels'
import { Choices, Info, ReasonField, SELECT, TextField } from './FormParts'

// Khung chi tiết và khung lập yêu cầu mua sắm (019 T008; bảng FR-008, FR-008a, FR-008b, FR-008d). Lệnh theo trạng
// thái và quyền: người lập (quyền Lập) gửi, rút lại, hủy Nháp; Quản lý viện (quyền Duyệt) duyệt, từ chối, xử lý lệch;
// Phụ trách bộ phận (quyền Sửa, không là Kế toán) ghi đã mua, hủy Đã duyệt; Kế toán đối chiếu. Backend kiểm lại người
// lập và Phụ trách bộ phận (api-019 không trả người lập, phiên bản nên web chưa sửa được Nháp).

const wholeNumber = (text) => (/^\d+$/.test(text.trim()) ? Number(text.trim()) : null)

export function ProcurementDetail({ request, role, departmentName, residentName, onChanged, Title, Description }) {
  const { can } = usePermissions()
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [approved, setApproved] = useState('')
  const [actual, setActual] = useState('')
  const [purchasedOn, setPurchasedOn] = useState('')
  const [supplier, setSupplier] = useState('')
  const [receipts, setReceipts] = useState([])
  const [discrepancy, setDiscrepancy] = useState('')
  const [resolution, setResolution] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const uploaded = useRef({ files: null, ids: [] })

  const status = request.status
  const accountant = role === 'ACCOUNTANT'
  const author = can('PURCHASE_REQUEST', 'CREATE') && !accountant
  const approver = can('PURCHASE_REQUEST', 'APPROVE')
  const lead = can('PURCHASE_REQUEST', 'UPDATE') && !accountant
  const medication = request.type === 'MEDICATION'
  const label = `yêu cầu #${request.id}`

  function open(next) {
    setMode(next)
    setSubmitted(false)
    setFailure(null)
  }

  async function run(command, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await postProcurementRequestsByIdByCommand({
        path: { id: request.id, command },
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

  const reasonError = submitted && !reason.trim() ? 'Nhập lý do' : null

  function withReason(command, message) {
    setSubmitted(true)
    if (!reason.trim()) return
    run(command, { reason: reason.trim() }, message)
  }

  // Để trống là duyệt đúng tổng dự kiến; giảm thì bắt buộc lý do; không duyệt tăng (FR-008)
  const approvedValue = approved.trim() === '' ? null : wholeNumber(approved)
  const approveErrors = {
    amount:
      approved.trim() !== '' &&
      (approvedValue === null || approvedValue > request.estimatedTotal) &&
      `Nhập số từ 0 tới ${money(request.estimatedTotal)}`,
    reason:
      approvedValue !== null &&
      approvedValue < request.estimatedTotal &&
      !reason.trim() &&
      'Giảm số được duyệt thì nhập lý do',
  }

  function approve() {
    setSubmitted(true)
    if (approveErrors.amount || approveErrors.reason) return
    const body = {}
    if (approvedValue !== null) body.approvedAmount = approvedValue
    if (reason.trim()) body.reason = reason.trim()
    run('approve', body, `Đã duyệt ${label}`)
  }

  const purchaseErrors = {
    actual: wholeNumber(actual) === null && 'Nhập số tiền thực tế',
    date: !purchasedOn && 'Chọn ngày mua',
    supplier: !supplier.trim() && 'Nhập nhà cung cấp',
    receipts: receipts.length === 0 && 'Chọn ảnh hóa đơn hoặc phiếu bán hàng',
  }

  async function recordPurchased() {
    setSubmitted(true)
    if (Object.values(purchaseErrors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      // Tệp đã tải ở lần gửi trước (bị chặn 409) thì dùng lại, không tải lần nữa
      if (uploaded.current.files !== receipts) {
        uploaded.current = { files: receipts, ids: await Promise.all(receipts.map((f) => uploadFile(f))) }
      }
    } catch (e) {
      setFailure(problemOf(e))
      setBusy(false)
      return
    }
    setBusy(false)
    run(
      'record-purchased',
      {
        actualAmount: wholeNumber(actual),
        purchasedOn,
        supplier: supplier.trim(),
        receiptFileIds: uploaded.current.ids,
      },
      `Đã ghi đã mua ${label}, chờ Kế toán đối chiếu`,
    )
  }

  const discrepancyError = submitted && !wholeNumber(discrepancy) ? 'Nhập số tiền lệch' : null

  function recordDiscrepancy() {
    setSubmitted(true)
    if (!wholeNumber(discrepancy) || !reason.trim()) return
    run(
      'record-discrepancy',
      { discrepancyAmount: wholeNumber(discrepancy), reason: reason.trim() },
      `Đã ghi lệch ${label}, chờ Quản lý viện xử lý`,
    )
  }

  function resolve() {
    setSubmitted(true)
    if (!resolution || !reason.trim()) return
    run('resolve-discrepancy', { resolution, reason: reason.trim() }, `Đã xử lý lệch ${label}`)
  }

  const show = (errors, name) => (submitted && errors[name]) || null
  const back = (
    <Button variant="ghost" className="h-11" onClick={() => open(null)}>
      Quay lại
    </Button>
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={PROCUREMENT_STATUS_LABELS[status] ?? status} />
        <Title className="type-title">{`Yêu cầu #${request.id}: ${typeLabel(request.type)}`}</Title>
        <Description className="type-body-sm text-muted-foreground">{departmentName}</Description>
      </div>
      <div className="grid gap-3">
        <Info label="Lý do">{request.reason}</Info>
        {medication ? (
          <Info label="Thời điểm cần thuốc">
            {request.neededBy ? formatDateTime(request.neededBy) : '—'}
            {request.priority ? `, ${priorityLabel(request.priority)}` : ''}
          </Info>
        ) : (
          request.neededOn && <Info label="Ngày cần">{formatDate(request.neededOn)}</Info>
        )}
        <div className="grid grid-cols-2 gap-3">
          <Info label="Tổng dự kiến">{money(request.estimatedTotal)}</Info>
          {request.approvedAmount != null && <Info label="Số được duyệt">{money(request.approvedAmount)}</Info>}
          {request.actualAmount != null && <Info label="Số tiền thực tế">{money(request.actualAmount)}</Info>}
          {['ASSET', 'SERVICE'].includes(request.type) && request.linkedSourceTotal > 0 && (
            <Info label="Đã gắn tài sản, bảo trì">{money(request.linkedSourceTotal)}</Info>
          )}
        </div>
        {receivedLabel(request) && <Info label="Tiếp nhận thuốc">{receivedLabel(request)}</Info>}
        {request.originalRequestId && <Info label="Bổ sung cho">{`Yêu cầu #${request.originalRequestId}`}</Info>}
      </div>

      <section aria-labelledby={`lines-${request.id}`} className="flex flex-col gap-2">
        <h3 id={`lines-${request.id}`} className="type-card-title">
          Các dòng
        </h3>
        <ul className="flex flex-col divide-y rounded-lg border">
          {request.lines.map((line, index) => (
            <li key={line.id ?? index} className="flex flex-col gap-0.5 p-3">
              <div className="flex justify-between gap-2">
                <span className="type-label">{line.name}</span>
                <span className="type-body-sm tabular-nums">{money(lineAmount(line))}</span>
              </div>
              <span className="type-body-sm text-muted-foreground tabular-nums">
                {`${line.quantity}${line.unit ? ` ${line.unit}` : ''} × ${money(line.estimatedUnitPrice)}`}
              </span>
              {line.residentId != null && (
                <span className="type-body-sm text-muted-foreground">
                  {`${residentName(line.residentId)}, đơn #${line.prescriptionId}; ${line.activeIngredient} ${line.strength}, ${line.dosageForm}`}
                </span>
              )}
            </li>
          ))}
        </ul>
      </section>

      <ProblemAlert problem={failure} />

      {mode === 'approve' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <TextField
            id="procurement-approved"
            label="Số được duyệt (đồng)"
            inputMode="numeric"
            value={approved}
            onChange={setApproved}
            error={show(approveErrors, 'amount')}
            placeholder={`Để trống là duyệt ${money(request.estimatedTotal)}`}
          />
          <ReasonField
            id="procurement-reason"
            value={reason}
            onChange={setReason}
            error={show(approveErrors, 'reason')}
          />
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={approve}>
              Xác nhận duyệt
            </Button>
          </div>
        </div>
      )}

      {['reject', 'cancel', 'return'].includes(mode) && (
        <div className="flex flex-col gap-3 border-t pt-4">
          {mode === 'cancel' && status === 'DRAFT' ? (
            <p className="type-body-sm">Hủy yêu cầu Nháp này? Yêu cầu đã hủy không mở lại được.</p>
          ) : (
            <ReasonField id="procurement-reason" value={reason} onChange={setReason} error={reasonError} />
          )}
          <div className="flex justify-end gap-2">
            {back}
            {mode === 'reject' && (
              <Button className="h-11" disabled={busy} onClick={() => withReason('reject', `Đã từ chối ${label}`)}>
                Xác nhận từ chối
              </Button>
            )}
            {mode === 'return' && (
              <Button
                className="h-11"
                disabled={busy}
                onClick={() => withReason('return-for-reconciliation', `Đã trả lại ${label} để ghi đã mua lại`)}
              >
                Xác nhận trả lại
              </Button>
            )}
            {mode === 'cancel' && (
              <Button
                variant="destructive"
                className="h-11"
                disabled={busy}
                onClick={() =>
                  status === 'DRAFT' ? run('cancel', {}, `Đã hủy ${label}`) : withReason('cancel', `Đã hủy ${label}`)
                }
              >
                Xác nhận hủy
              </Button>
            )}
          </div>
        </div>
      )}

      {mode === 'purchase' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <FieldGroup>
            <TextField
              id="procurement-actual"
              label="Số tiền thực tế (đồng)"
              inputMode="numeric"
              value={actual}
              onChange={setActual}
              error={show(purchaseErrors, 'actual')}
            />
            <TextField
              id="procurement-date"
              label="Ngày mua"
              type="date"
              value={purchasedOn}
              onChange={setPurchasedOn}
              error={show(purchaseErrors, 'date')}
            />
            <TextField
              id="procurement-supplier"
              label="Nhà cung cấp"
              value={supplier}
              onChange={setSupplier}
              error={show(purchaseErrors, 'supplier')}
            />
            <Field data-invalid={!!show(purchaseErrors, 'receipts') || undefined}>
              <FieldLabel htmlFor="procurement-receipts">Chứng từ</FieldLabel>
              <Input
                id="procurement-receipts"
                type="file"
                multiple
                accept="image/*,application/pdf"
                className="h-11"
                onChange={(e) => setReceipts([...(e.target.files ?? [])])}
                aria-invalid={!!show(purchaseErrors, 'receipts') || undefined}
              />
              {show(purchaseErrors, 'receipts') && <FieldError>{purchaseErrors.receipts}</FieldError>}
            </Field>
          </FieldGroup>
          <p className="type-body-sm text-muted-foreground">
            Vượt số được duyệt quá mức cho phép thì không ghi được; lập yêu cầu bổ sung cho phần vượt.
          </p>
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={recordPurchased}>
              Lưu đã mua
            </Button>
          </div>
        </div>
      )}

      {mode === 'discrepancy' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <TextField
            id="procurement-discrepancy"
            label="Số tiền lệch (đồng)"
            inputMode="numeric"
            value={discrepancy}
            onChange={setDiscrepancy}
            error={discrepancyError}
          />
          <ReasonField id="procurement-reason" value={reason} onChange={setReason} error={reasonError} />
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={recordDiscrepancy}>
              Lưu lệch
            </Button>
          </div>
        </div>
      )}

      {mode === 'resolve' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <Choices
            label="Cách xử lý"
            options={RESOLUTIONS}
            value={resolution}
            onChange={setResolution}
            error={submitted && !resolution ? 'Chọn cách xử lý' : null}
          />
          <ReasonField id="procurement-reason" value={reason} onChange={setReason} error={reasonError} />
          <div className="flex justify-end gap-2">
            {back}
            <Button className="h-11" disabled={busy} onClick={resolve}>
              Lưu xử lý
            </Button>
          </div>
        </div>
      )}

      {mode === null && (
        <div className="flex flex-wrap justify-end gap-2">
          {author && status === 'DRAFT' && (
            <>
              <Button variant="ghost" className="h-11" onClick={() => open('cancel')}>
                Hủy yêu cầu…
              </Button>
              <Button
                className="h-11"
                disabled={busy}
                onClick={() => run('submit', undefined, `Đã gửi duyệt ${label}`)}
              >
                Gửi duyệt
              </Button>
            </>
          )}
          {author && status === 'PENDING_APPROVAL' && (
            <Button
              variant="outline"
              className="h-11"
              disabled={busy}
              onClick={() => run('withdraw', undefined, `Đã rút ${label} về Nháp`)}
            >
              Rút lại về Nháp
            </Button>
          )}
          {approver && status === 'PENDING_APPROVAL' && (
            <>
              <Button variant="outline" className="h-11" onClick={() => open('reject')}>
                Từ chối…
              </Button>
              <Button className="h-11" onClick={() => open('approve')}>
                Duyệt…
              </Button>
            </>
          )}
          {lead && status === 'APPROVED' && (
            <>
              <Button variant="ghost" className="h-11" onClick={() => open('cancel')}>
                Hủy yêu cầu…
              </Button>
              <Button className="h-11" onClick={() => open('purchase')}>
                Ghi đã mua…
              </Button>
            </>
          )}
          {accountant && status === 'PENDING_RECONCILIATION' && (
            <>
              <Button variant="ghost" className="h-11" onClick={() => open('return')}>
                Trả lại…
              </Button>
              <Button variant="outline" className="h-11" onClick={() => open('discrepancy')}>
                Ghi lệch…
              </Button>
              <Button
                className="h-11"
                disabled={busy}
                onClick={() => run('confirm-match', undefined, `Đã xác nhận khớp ${label}`)}
              >
                Xác nhận khớp
              </Button>
            </>
          )}
          {approver && status === 'DISCREPANCY' && (
            <Button className="h-11" onClick={() => open('resolve')}>
              Xử lý lệch…
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

const EMPTY_LINE = {
  name: '',
  quantity: '',
  estimatedUnitPrice: '',
  residentId: '',
  prescriptionId: '',
  activeIngredient: '',
  strength: '',
  dosageForm: '',
  unit: '',
}

function lineErrors(line, medication) {
  const errors = {
    name: !line.name.trim() && (medication ? 'Nhập tên thuốc' : 'Nhập tên hàng hoặc dịch vụ'),
    quantity: !(Number(line.quantity) > 0) && 'Nhập số lượng lớn hơn 0',
    estimatedUnitPrice: wholeNumber(String(line.estimatedUnitPrice)) === null && 'Nhập đơn giá dự kiến',
  }
  if (medication) {
    errors.residentId = !line.residentId && 'Chọn người cao tuổi'
    errors.prescriptionId = !line.prescriptionId && 'Chọn đơn thuốc'
    errors.activeIngredient = !line.activeIngredient.trim() && 'Nhập hoạt chất'
    errors.strength = !line.strength.trim() && 'Nhập hàm lượng'
    errors.dosageForm = !line.dosageForm.trim() && 'Nhập dạng thuốc'
    errors.unit = !line.unit.trim() && 'Nhập đơn vị'
  }
  return errors
}

function LineFields({ index, line, medication, residents, prescriptions, onChange, onPickResident, onRemove, errors }) {
  const id = (name) => `procurement-line-${index}-${name}`
  const set = (name) => (value) => onChange({ ...line, [name]: value })
  const err = (name) => errors?.[name] || null
  return (
    <fieldset className="flex flex-col gap-3 rounded-lg border p-4">
      <legend className="px-1 type-label">{`Dòng ${index + 1}`}</legend>
      {medication && (
        <div className="grid gap-3 sm:grid-cols-2">
          <Field data-invalid={!!err('residentId') || undefined}>
            <FieldLabel htmlFor={id('resident')}>Người cao tuổi</FieldLabel>
            <select
              id={id('resident')}
              value={line.residentId}
              onChange={(e) => onPickResident(e.target.value)}
              aria-invalid={!!err('residentId') || undefined}
              className={SELECT}
            >
              <option value="">{residents === null ? 'Đang tải…' : 'Chọn người'}</option>
              {(residents ?? []).map((r) => (
                <option key={r.id} value={r.id}>
                  {`${r.personalInfo.fullName}, ${r.profileCode}`}
                </option>
              ))}
            </select>
            {err('residentId') && <FieldError>{err('residentId')}</FieldError>}
          </Field>
          <Field data-invalid={!!err('prescriptionId') || undefined}>
            <FieldLabel htmlFor={id('prescription')}>Đơn thuốc nguồn viện</FieldLabel>
            <select
              id={id('prescription')}
              value={line.prescriptionId}
              onChange={(e) => set('prescriptionId')(e.target.value)}
              aria-invalid={!!err('prescriptionId') || undefined}
              className={SELECT}
            >
              <option value="">Chọn đơn Hiệu lực, Tạm dừng, Chờ hiệu lực</option>
              {(prescriptions ?? []).map((p) => (
                <option key={p.id} value={p.id}>
                  {`Đơn #${p.id}, từ ${formatDate(p.startedAt)}`}
                </option>
              ))}
            </select>
            {err('prescriptionId') && <FieldError>{err('prescriptionId')}</FieldError>}
          </Field>
        </div>
      )}
      <TextField
        id={id('name')}
        label={medication ? 'Tên thuốc' : 'Tên hàng hoặc dịch vụ'}
        value={line.name}
        onChange={set('name')}
        error={err('name')}
      />
      {medication && (
        <div className="grid gap-3 sm:grid-cols-3">
          <TextField
            id={id('ingredient')}
            label="Hoạt chất"
            value={line.activeIngredient}
            onChange={set('activeIngredient')}
            error={err('activeIngredient')}
          />
          <TextField
            id={id('strength')}
            label="Hàm lượng"
            value={line.strength}
            onChange={set('strength')}
            error={err('strength')}
          />
          <TextField
            id={id('form')}
            label="Dạng"
            value={line.dosageForm}
            onChange={set('dosageForm')}
            error={err('dosageForm')}
          />
        </div>
      )}
      <div className="grid gap-3 sm:grid-cols-3">
        <TextField
          id={id('quantity')}
          label="Số lượng"
          inputMode="decimal"
          value={line.quantity}
          onChange={set('quantity')}
          error={err('quantity')}
        />
        {medication && (
          <TextField id={id('unit')} label="Đơn vị" value={line.unit} onChange={set('unit')} error={err('unit')} />
        )}
        <TextField
          id={id('price')}
          label="Đơn giá dự kiến (đồng)"
          inputMode="numeric"
          value={line.estimatedUnitPrice}
          onChange={set('estimatedUnitPrice')}
          error={err('estimatedUnitPrice')}
        />
      </div>
      <div className="flex items-center justify-between gap-2">
        <span className="type-body-sm text-muted-foreground">
          Thành tiền <span className="tabular-nums text-foreground">{money(lineAmount(line))}</span>
        </span>
        {onRemove && (
          <Button type="button" variant="ghost" className="h-11" onClick={onRemove}>
            {`Bỏ dòng ${index + 1}`}
          </Button>
        )}
      </div>
    </fieldset>
  )
}

/** Lập yêu cầu Nháp (FR-008; FR-008d với loại thuốc). Backend tính lại thành tiền, tổng và kiểm quyền người lập. */
export function CreateProcurementSheet({ departments, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [departmentId, setDepartmentId] = useState('')
  const [type, setType] = useState('')
  const [priority, setPriority] = useState('')
  const [neededBy, setNeededBy] = useState('')
  const [neededOn, setNeededOn] = useState('')
  const [reason, setReason] = useState('')
  const [originalRequestId, setOriginalRequestId] = useState('')
  const [lines, setLines] = useState([EMPTY_LINE])
  const [residents, setResidents] = useState(null)
  const [prescriptions, setPrescriptions] = useState({})
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const medication = type === 'MEDICATION'

  useEffect(() => {
    if (!medication || residents !== null) return undefined
    let cancelled = false
    getResidents({ query: { status: ['IN_RESIDENCE'] } })
      .then(({ data }) => !cancelled && setResidents(data.items))
      .catch((e) => {
        if (!cancelled) {
          setResidents([])
          setFailure(problemOf(e))
        }
      })
    return () => {
      cancelled = true
    }
  }, [medication, residents])

  function pickResident(index, residentId) {
    setLines((all) => all.map((l, i) => (i === index ? { ...l, residentId, prescriptionId: '' } : l)))
    if (!residentId || prescriptions[residentId]) return
    getResidentsByResidentIdPrescriptions({ path: { residentId: Number(residentId) } })
      .then(({ data }) => setPrescriptions((p) => ({ ...p, [residentId]: data.filter(facilityUnended) })))
      .catch((e) => setFailure(problemOf(e)))
  }

  const total = lines.reduce((sum, l) => sum + lineAmount(l), 0)
  const errors = {
    department: !departmentId && 'Chọn bộ phận',
    type: !type && 'Chọn loại yêu cầu',
    priority: medication && !priority && 'Chọn mức ưu tiên',
    neededBy: medication && !neededBy && 'Chọn thời điểm cần thuốc',
    reason: !reason.trim() && 'Nhập lý do',
    original: originalRequestId.trim() !== '' && wholeNumber(originalRequestId) === null && 'Nhập số yêu cầu gốc',
  }
  const linesErrors = lines.map((l) => lineErrors(l, medication))
  const show = (name) => (submitted && errors[name]) || null

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean) || linesErrors.some((e) => Object.values(e).some(Boolean))) return
    setBusy(true)
    setFailure(null)
    const body = { departmentId: Number(departmentId), type }
    if (medication) {
      body.priority = priority
      body.neededBy = vietnamIso(neededBy)
    } else {
      if (priority) body.priority = priority
      if (neededOn) body.neededOn = neededOn
    }
    body.reason = reason.trim()
    body.lines = lines.map((l) => {
      const line = {
        name: l.name.trim(),
        quantity: Number(l.quantity),
        estimatedUnitPrice: Number(l.estimatedUnitPrice),
      }
      if (!medication) return line
      return {
        ...line,
        residentId: Number(l.residentId),
        prescriptionId: Number(l.prescriptionId),
        activeIngredient: l.activeIngredient.trim(),
        strength: l.strength.trim(),
        dosageForm: l.dosageForm.trim(),
        unit: l.unit.trim(),
      }
    })
    if (originalRequestId.trim()) body.originalRequestId = wholeNumber(originalRequestId)
    try {
      await postProcurementRequests({ headers: { 'Idempotency-Key': key }, body })
      onDone('Đã lập yêu cầu ở Nháp; gửi duyệt khi đủ thông tin')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Lập yêu cầu mua sắm</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Yêu cầu lưu ở Nháp; gửi duyệt rồi thì không sửa được nội dung, muốn đổi thì rút lại về Nháp.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <FieldGroup>
            <Field data-invalid={!!show('department') || undefined}>
              <FieldLabel htmlFor="procurement-department">Bộ phận</FieldLabel>
              {departments === null || departments.length > 0 ? (
                <select
                  id="procurement-department"
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
              ) : (
                <Input
                  id="procurement-department"
                  inputMode="numeric"
                  className="h-11"
                  placeholder="Mã bộ phận"
                  value={departmentId}
                  onChange={(e) => setDepartmentId(e.target.value.replace(/\D/g, ''))}
                  aria-invalid={!!show('department') || undefined}
                />
              )}
              {show('department') && <FieldError>{errors.department}</FieldError>}
            </Field>
            <Choices
              label="Loại yêu cầu"
              options={PROCUREMENT_TYPES}
              value={type}
              onChange={setType}
              error={show('type')}
            />
            <Choices
              label={medication ? 'Mức ưu tiên' : 'Mức ưu tiên (không bắt buộc)'}
              options={PRIORITIES}
              value={priority}
              onChange={setPriority}
              error={show('priority')}
            />
            {medication ? (
              <TextField
                id="procurement-needed-by"
                label="Thời điểm cần thuốc"
                type="datetime-local"
                value={neededBy}
                onChange={setNeededBy}
                error={show('neededBy')}
              />
            ) : (
              <TextField
                id="procurement-needed-on"
                label="Ngày cần (không bắt buộc)"
                type="date"
                value={neededOn}
                onChange={setNeededOn}
              />
            )}
            <ReasonField id="procurement-create-reason" value={reason} onChange={setReason} error={show('reason')} />
            <TextField
              id="procurement-original"
              label="Bổ sung cho yêu cầu số (không bắt buộc)"
              inputMode="numeric"
              value={originalRequestId}
              onChange={setOriginalRequestId}
              error={show('original')}
            />
          </FieldGroup>

          {lines.map((line, index) => (
            <LineFields
              key={index}
              index={index}
              line={line}
              medication={medication}
              residents={residents}
              prescriptions={prescriptions[line.residentId]}
              errors={submitted ? linesErrors[index] : null}
              onChange={(next) => setLines((all) => all.map((l, i) => (i === index ? next : l)))}
              onPickResident={(id) => pickResident(index, id)}
              onRemove={lines.length > 1 ? () => setLines((all) => all.filter((_, i) => i !== index)) : null}
            />
          ))}
          <div className="flex items-center justify-between gap-2">
            <Button
              type="button"
              variant="outline"
              className="h-11"
              onClick={() => setLines((all) => [...all, EMPTY_LINE])}
            >
              Thêm dòng
            </Button>
            <span className="type-label tabular-nums">{`Tổng dự kiến: ${money(total)}`}</span>
          </div>

          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" className="h-11" onClick={onClose}>
              Quay lại
            </Button>
            <Button type="submit" className="h-11" disabled={busy}>
              Lưu nháp
            </Button>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
}
