import { useRef, useState } from 'react'
import { postBelongingsHandovers } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { FilesField, SelectField } from './BelongingFields'
import { activeOptions, commandsFor, kindLabel, nowLocal, toOffsetIso } from './belongingLabels'

// Lệnh bàn giao trên một đồ (013 T015, US2; FR-011 → FR-015, FR-021, FR-024; api-013 HandoverCommand). Lệnh hiện theo
// trạng thái đồ như bảng chuyển của data-model mục 3; số lượng nhỏ hơn hiện có thì backend tách đồ (FR-012); số đếm
// được ít hơn thì phần thiếu được Báo thất lạc cùng lệnh (FR-013); chọn mức tình trạng xấu hơn thì chọn cách xử lý
// (FR-015); Báo thất lạc, Ghi hư hỏng chọn ghi sự cố mới, liên kết sự cố có sẵn hoặc không (FR-021, Q-307). Gửi kèm
// phiên bản của đồ (FR-011b).

const REASON_REQUIRED = ['TRANSFER_CUSTODY', 'REPORT_LOST', 'RECORD_DAMAGE']
const INCIDENT_COMMANDS = ['REPORT_LOST', 'RECORD_DAMAGE']
const HOLDER_ABSENT_COMMANDS = ['TAKE_BACK', 'TRANSFER_CUSTODY', 'REPORT_LOST', 'RECORD_DAMAGE']
const INCIDENT_OPTIONS = [
  { value: 'NONE', label: 'Không ghi sự cố' },
  { value: 'NEW', label: 'Ghi sự cố mới' },
  { value: 'LINK', label: 'Liên kết sự cố đã có' },
]
const LEVEL_OPTIONS = [
  { value: 'LOW', label: 'Nhẹ' },
  { value: 'MEDIUM', label: 'Trung bình' },
  { value: 'CRITICAL', label: 'Khẩn cấp' },
]
const HANDLING_OPTIONS = [
  { value: 'ACKNOWLEDGE', label: 'Ghi nhận chênh lệch' },
  { value: 'RECORD_DAMAGE', label: 'Ghi hư hỏng' },
]

// Lệnh đưa đồ về Đang giữ hoặc Hư hỏng bắt buộc có vị trí lưu giữ (HandoverCommandCheck); Ghi hư hỏng bắt buộc ảnh với mọi
// đồ, Tìm thấy bắt buộc ảnh với đồ có giá trị (FR-008). Chọn xử lý chênh lệch "Ghi hư hỏng" thì lệnh là Ghi hư hỏng (FR-015).
function effectiveType(type, handling) {
  return handling === 'RECORD_DAMAGE' ? 'RECORD_DAMAGE' : type
}

function needsLocation(type, handling, status) {
  const t = effectiveType(type, handling)
  return (
    ['TAKE_BACK', 'FOUND', 'RECORD_DAMAGE'].includes(t) ||
    (t === 'TRANSFER_CUSTODY' && ['HELD', 'DAMAGED'].includes(status))
  )
}

function needsPhoto(type, handling, valuable) {
  const t = effectiveType(type, handling)
  return t === 'RECORD_DAMAGE' || (valuable && t === 'FOUND')
}

export function HandoverForm({ belonging, levels, locations, valuable = false, onDone }) {
  const [v, setV] = useState({
    type: '',
    quantity: '',
    counted: '',
    levelId: '',
    conditionDescription: '',
    handling: '',
    recipientStaffId: '',
    storageLocationId: '',
    reason: '',
    at: nowLocal(),
    incident: 'NONE',
    incidentId: '',
    incidentLevel: '',
    incidentDescription: '',
  })
  const [holderAbsent, setHolderAbsent] = useState(false)
  const [photos, setPhotos] = useState([])
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const key = useRef(crypto.randomUUID())
  const set = (name) => (value) => setV((s) => ({ ...s, [name]: value }))

  // Vị trí gửi đi: ô đã chọn, không thì vị trí hiện tại của đồ khi lệnh đưa đồ về Đang giữ hoặc Hư hỏng
  const locationId = () =>
    v.storageLocationId ||
    (needsLocation(v.type, v.handling, belonging.status) && belonging.storageLocationId != null
      ? String(belonging.storageLocationId)
      : '')

  function validate() {
    const e = {}
    if (!v.type) e.type = 'Chọn lệnh bàn giao.'
    if (REASON_REQUIRED.includes(v.type) && !v.reason.trim()) e.reason = 'Nhập lý do.'
    if (v.quantity && !(Number(v.quantity) > 0)) e.quantity = 'Số lượng phải lớn hơn 0.'
    if (v.counted && !(Number(v.counted) >= 0)) e.counted = 'Số đếm được không âm.'
    if (v.incident === 'NEW' && !v.incidentLevel) e.incidentLevel = 'Chọn mức sự cố.'
    if (v.incident === 'LINK' && !/^[1-9]\d*$/.test(v.incidentId)) e.incidentId = 'Nhập mã sự cố.'
    if (needsLocation(v.type, v.handling, belonging.status) && !locationId())
      e.storageLocationId = 'Chọn vị trí lưu giữ.'
    if (needsPhoto(v.type, v.handling, valuable) && photos.length === 0) e.photos = 'Chọn ít nhất một ảnh.'
    return e
  }

  async function submit(event) {
    event.preventDefault()
    const found = validate()
    setErrors(found)
    setFailure(null)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    try {
      const photoFileIds = []
      for (const file of photos) photoFileIds.push(await uploadFile(file))
      const item = { belongingId: belonging.id, version: belonging.version }
      if (v.quantity) item.quantity = Number(v.quantity)
      if (v.counted !== '') item.countedQuantity = Number(v.counted)
      if (v.levelId)
        item.condition = {
          levelId: Number(v.levelId),
          ...(v.conditionDescription.trim() && { description: v.conditionDescription.trim() }),
        }
      if (photoFileIds.length > 0) item.photoFileIds = photoFileIds
      if (INCIDENT_COMMANDS.includes(v.type) && v.incident !== 'NONE')
        item.incident =
          v.incident === 'LINK'
            ? { method: 'LINK', incidentId: Number(v.incidentId) }
            : {
                method: 'NEW',
                level: v.incidentLevel,
                ...(v.incidentDescription.trim() && { description: v.incidentDescription.trim() }),
              }
      await postBelongingsHandovers({
        headers: { 'Idempotency-Key': key.current },
        body: {
          type: v.type,
          items: [item],
          at: toOffsetIso(v.at),
          ...(v.reason.trim() && { reason: v.reason.trim() }),
          ...(v.recipientStaffId && { recipientStaffId: Number(v.recipientStaffId) }),
          ...(locationId() && { storageLocationId: Number(locationId()) }),
          ...(v.handling && { conditionHandling: v.handling }),
          ...(holderAbsent && HOLDER_ABSENT_COMMANDS.includes(v.type) && { holderAbsent: true }),
        },
      })
      onDone(`Đã ghi ${kindLabel(v.type).toLowerCase()}.`)
    } catch (error) {
      const p = problemOf(error)
      if (p.errorCode === 'CONDITION_HANDLING_REQUIRED')
        setErrors({ handling: p.detail || 'Chọn cách xử lý chênh lệch.' })
      setFailure(p)
    } finally {
      setBusy(false)
    }
  }

  const amountLabel = belonging.cashAmount != null ? 'Số tiền bàn giao (trống là toàn bộ)' : 'Số lượng bàn giao'
  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <FieldGroup>
        <SelectField
          id="handover-type"
          label="Lệnh bàn giao"
          value={v.type}
          onChange={set('type')}
          options={commandsFor(belonging.status).map((c) => ({ value: c, label: kindLabel(c) }))}
          error={errors.type}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field data-invalid={!!errors.quantity || undefined}>
            <FieldLabel htmlFor="handover-quantity">{amountLabel}</FieldLabel>
            <Input
              id="handover-quantity"
              type="number"
              min="0"
              className="h-11 tabular-nums"
              value={v.quantity}
              placeholder="Toàn bộ"
              onChange={(e) => set('quantity')(e.target.value)}
              aria-invalid={!!errors.quantity || undefined}
            />
            {errors.quantity && <FieldError>{errors.quantity}</FieldError>}
          </Field>
          <Field data-invalid={!!errors.counted || undefined}>
            <FieldLabel htmlFor="handover-counted">Số đếm được</FieldLabel>
            <Input
              id="handover-counted"
              type="number"
              min="0"
              className="h-11 tabular-nums"
              value={v.counted}
              onChange={(e) => set('counted')(e.target.value)}
              aria-invalid={!!errors.counted || undefined}
            />
            {errors.counted && <FieldError>{errors.counted}</FieldError>}
          </Field>
        </div>
        <SelectField
          id="handover-level"
          label="Mức tình trạng lúc bàn giao"
          value={v.levelId}
          onChange={set('levelId')}
          options={activeOptions([...levels].sort((a, b) => a.order - b.order))}
          placeholder="Không đổi"
        />
        {v.levelId && (
          <>
            <Field>
              <FieldLabel htmlFor="handover-condition">Mô tả tình trạng (không bắt buộc)</FieldLabel>
              <Input
                id="handover-condition"
                className="h-11"
                value={v.conditionDescription}
                onChange={(e) => set('conditionDescription')(e.target.value)}
              />
            </Field>
            <SelectField
              id="handover-handling"
              label="Xử lý chênh lệch tình trạng"
              value={v.handling}
              onChange={set('handling')}
              options={HANDLING_OPTIONS}
              error={errors.handling}
              placeholder="Không có chênh lệch"
              hint="Chọn khi tình trạng xấu hơn lần bàn giao trước."
            />
          </>
        )}
        {v.type !== 'GIVE_FOR_USE' && (
          <div className="grid gap-4 sm:grid-cols-2">
            <Field>
              <FieldLabel htmlFor="handover-recipient">Mã nhân viên nhận (trống là bạn)</FieldLabel>
              <Input
                id="handover-recipient"
                inputMode="numeric"
                className="h-11"
                value={v.recipientStaffId}
                onChange={(e) => set('recipientStaffId')(e.target.value)}
              />
            </Field>
            <SelectField
              id="handover-location"
              label="Vị trí lưu giữ mới"
              value={locationId()}
              onChange={set('storageLocationId')}
              options={activeOptions(locations)}
              placeholder="Giữ vị trí hiện tại"
              error={errors.storageLocationId}
              hint={
                needsLocation(v.type, v.handling, belonging.status)
                  ? 'Bắt buộc với lệnh này; để trống thì dùng vị trí hiện tại của đồ.'
                  : undefined
              }
            />
          </div>
        )}
        <Field data-invalid={!!errors.reason || undefined}>
          <FieldLabel htmlFor="handover-reason">Lý do</FieldLabel>
          <Textarea
            id="handover-reason"
            rows={2}
            maxLength={2000}
            value={v.reason}
            onChange={(e) => set('reason')(e.target.value)}
            aria-invalid={!!errors.reason || undefined}
          />
          {errors.reason && <FieldError>{errors.reason}</FieldError>}
        </Field>
        <Field>
          <FieldLabel htmlFor="handover-at">Thời điểm bàn giao</FieldLabel>
          <Input
            id="handover-at"
            type="datetime-local"
            className="h-11 tabular-nums"
            value={v.at}
            onChange={(e) => set('at')(e.target.value)}
          />
        </Field>
        <FilesField
          id="handover-photos"
          label="Ảnh lúc bàn giao"
          files={photos}
          onChange={setPhotos}
          multiple
          error={errors.photos}
          hint={
            effectiveType(v.type, v.handling) === 'RECORD_DAMAGE'
              ? 'Bắt buộc khi ghi hư hỏng. JPEG hoặc PNG, tối đa 10 MB mỗi ảnh.'
              : 'Bắt buộc khi ghi hư hỏng hoặc tìm thấy đồ có giá trị. JPEG hoặc PNG, tối đa 10 MB mỗi ảnh.'
          }
        />
        {HOLDER_ABSENT_COMMANDS.includes(v.type) && (
          <label className="flex min-h-11 cursor-pointer items-center gap-2 rounded-lg border px-3">
            <Checkbox checked={holderAbsent} onCheckedChange={setHolderAbsent} aria-label="Người đang giữ vắng mặt" />
            <span className="type-body-sm">Người đang giữ vắng mặt</span>
          </label>
        )}
        {INCIDENT_COMMANDS.includes(v.type) && (
          <>
            <SelectField
              id="handover-incident"
              label="Sự cố"
              value={v.incident}
              onChange={set('incident')}
              options={INCIDENT_OPTIONS}
              placeholder="Chọn…"
              hint="Người ghi quyết định; hệ thống không tự tạo sự cố."
            />
            {v.incident === 'NEW' && (
              <>
                <SelectField
                  id="handover-incident-level"
                  label="Mức sự cố"
                  value={v.incidentLevel}
                  onChange={set('incidentLevel')}
                  options={LEVEL_OPTIONS}
                  error={errors.incidentLevel}
                />
                <Field>
                  <FieldLabel htmlFor="handover-incident-description">Mô tả sự cố (trống thì dùng lý do)</FieldLabel>
                  <Textarea
                    id="handover-incident-description"
                    rows={2}
                    value={v.incidentDescription}
                    onChange={(e) => set('incidentDescription')(e.target.value)}
                  />
                </Field>
              </>
            )}
            {v.incident === 'LINK' && (
              <Field data-invalid={!!errors.incidentId || undefined}>
                <FieldLabel htmlFor="handover-incident-id">Mã sự cố</FieldLabel>
                <Input
                  id="handover-incident-id"
                  inputMode="numeric"
                  className="h-11"
                  value={v.incidentId}
                  onChange={(e) => set('incidentId')(e.target.value)}
                  aria-invalid={!!errors.incidentId || undefined}
                />
                {errors.incidentId && <FieldError>{errors.incidentId}</FieldError>}
              </Field>
            )}
          </>
        )}
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11 self-start" disabled={busy}>
        Lưu bàn giao
      </Button>
    </form>
  )
}
