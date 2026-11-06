import { useRef, useState } from 'react'
import { postResidentsByResidentIdBelongings } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { FilesField, SelectField } from './BelongingFields'
import { GIVER_LABELS, activeOptions, nowLocal, toOffsetIso } from './belongingLabels'

// Lệnh Tiếp nhận đồ gửi (013 T011, US1; FR-003 → FR-008; api-013 BelongingIntake). Loại tiền mặt nhập số tiền, loại
// khác nhập số lượng (FR-005); đồ có giá trị (dấu `valuable` của loại theo CFG-M12-01 hiện hành) bắt buộc ảnh (BR-M12-05);
// giao ngay đồ thuộc loại cần đồng ý phải kèm bản ký của người đại diện (BR-M12-06). Ảnh, bản ký tải lên qua POST /files
// rồi gửi mã tệp. Idempotency-Key giữ suốt form để bấm lại sau lỗi mạng không tạo hai đồ. Backend kiểm lại mọi điều kiện.

// Người giao khi tiếp nhận (FR-003): người thân có quan hệ Hiệu lực, người khác (họ tên và giấy tờ), chính người cao tuổi
const INTAKE_GIVERS = ['FAMILY_MEMBER', 'OTHER_PERSON', 'RESIDENT']

const EMPTY = {
  typeId: '',
  description: '',
  amount: '',
  unit: '',
  levelId: '',
  conditionDescription: '',
  giverType: '',
  giverId: '',
  giverName: '',
  giverIdDocument: '',
  at: '',
  storageLocationId: '',
  note: '',
}

function validate(values, type, photos, giveNow, signedForm) {
  const errors = {}
  if (!values.typeId) errors.typeId = 'Chọn loại đồ.'
  if (!values.description.trim()) errors.description = 'Nhập mô tả đồ.'
  const amount = Number(values.amount)
  if (!values.amount || !(amount > 0))
    errors.amount = type?.hasCashAmount ? 'Nhập số tiền lớn hơn 0.' : 'Nhập số lượng lớn hơn 0.'
  if (!values.levelId) errors.levelId = 'Chọn mức tình trạng.'
  if (!values.giverType) errors.giverType = 'Chọn người giao.'
  if (values.giverType === 'FAMILY_MEMBER' && !/^[1-9]\d*$/.test(values.giverId)) errors.giverId = 'Nhập mã người thân.'
  if (values.giverType === 'OTHER_PERSON' && !values.giverName.trim()) errors.giverName = 'Nhập họ tên người giao.'
  if (values.giverType === 'OTHER_PERSON' && !values.giverIdDocument.trim())
    errors.giverIdDocument = 'Nhập giấy tờ của người giao.'
  if (!giveNow && !values.storageLocationId) errors.storageLocationId = 'Chọn vị trí lưu giữ.'
  if (!values.at) errors.at = 'Nhập thời điểm tiếp nhận.'
  if (type?.valuable && photos.length === 0) errors.photos = 'Đồ có giá trị bắt buộc có ít nhất một ảnh.'
  if (giveNow && type?.requiresConsentForUse && signedForm.length === 0)
    errors.signedForm = 'Loại đồ này cần bản ký đồng ý của người đại diện khi giao ngay.'
  return errors
}

export function IntakeForm({ residentId, types, levels, locations, onDone }) {
  const [values, setValues] = useState(() => ({ ...EMPTY, at: nowLocal() }))
  const [photos, setPhotos] = useState([])
  const [signedForm, setSignedForm] = useState([])
  const [giveNow, setGiveNow] = useState(false)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const key = useRef(crypto.randomUUID())

  const type = types.find((t) => String(t.id) === values.typeId)
  const set = (name) => (value) => setValues((v) => ({ ...v, [name]: value }))

  async function submit(event) {
    event.preventDefault()
    const found = validate(values, type, photos, giveNow, signedForm)
    setErrors(found)
    setFailure(null)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    try {
      const photoFileIds = []
      for (const file of photos) photoFileIds.push(await uploadFile(file))
      const consentSignedFormFileId =
        giveNow && type.requiresConsentForUse ? await uploadFile(signedForm[0]) : undefined
      const amount = Number(values.amount)
      const giver = { type: values.giverType }
      if (values.giverType === 'FAMILY_MEMBER') giver.id = Number(values.giverId)
      if (values.giverName.trim()) giver.fullName = values.giverName.trim()
      if (values.giverIdDocument.trim()) giver.idDocument = values.giverIdDocument.trim()
      const { data } = await postResidentsByResidentIdBelongings({
        path: { residentId },
        headers: { 'Idempotency-Key': key.current },
        body: {
          typeId: Number(values.typeId),
          description: values.description.trim(),
          ...(type.hasCashAmount ? { cashAmount: amount } : { quantity: amount }),
          ...(values.unit.trim() && !type.hasCashAmount && { unit: values.unit.trim() }),
          condition: {
            levelId: Number(values.levelId),
            ...(values.conditionDescription.trim() && { description: values.conditionDescription.trim() }),
          },
          giver,
          at: toOffsetIso(values.at),
          ...(values.storageLocationId && { storageLocationId: Number(values.storageLocationId) }),
          ...(photoFileIds.length > 0 && { photoFileIds }),
          ...(values.note.trim() && { note: values.note.trim() }),
          ...(giveNow && { giveForUseNow: true }),
          ...(consentSignedFormFileId && { consentSignedFormFileId }),
        },
      })
      onDone(data)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  // Mức "Không dùng được" chỉ ghi qua Ghi hư hỏng hoặc Tiếp nhận (FR-001a), nên Tiếp nhận vẫn chọn được
  const levelOptions = activeOptions([...levels].sort((a, b) => a.order - b.order))

  return (
    <form className="flex flex-col gap-5" onSubmit={submit} noValidate>
      <FieldGroup>
        <SelectField
          id="intake-type"
          label="Loại đồ"
          value={values.typeId}
          onChange={set('typeId')}
          options={activeOptions(types)}
          error={errors.typeId}
          hint={type?.valuable ? 'Đồ có giá trị: bắt buộc ảnh, được kiểm kê định kỳ.' : undefined}
        />
        <Field data-invalid={!!errors.description || undefined}>
          <FieldLabel htmlFor="intake-description">Mô tả</FieldLabel>
          <Textarea
            id="intake-description"
            rows={2}
            maxLength={2000}
            value={values.description}
            onChange={(e) => set('description')(e.target.value)}
            aria-invalid={!!errors.description || undefined}
          />
          {errors.description && <FieldError>{errors.description}</FieldError>}
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field data-invalid={!!errors.amount || undefined}>
            <FieldLabel htmlFor="intake-amount">{type?.hasCashAmount ? 'Số tiền (đồng)' : 'Số lượng'}</FieldLabel>
            <Input
              id="intake-amount"
              type="number"
              min="0"
              inputMode="decimal"
              className="h-11 tabular-nums"
              value={values.amount}
              onChange={(e) => set('amount')(e.target.value)}
              aria-invalid={!!errors.amount || undefined}
            />
            {errors.amount && <FieldError>{errors.amount}</FieldError>}
          </Field>
          {!type?.hasCashAmount && (
            <Field>
              <FieldLabel htmlFor="intake-unit">Đơn vị (không bắt buộc)</FieldLabel>
              <Input
                id="intake-unit"
                className="h-11"
                value={values.unit}
                onChange={(e) => set('unit')(e.target.value)}
              />
            </Field>
          )}
        </div>
        <SelectField
          id="intake-level"
          label="Mức tình trạng"
          value={values.levelId}
          onChange={set('levelId')}
          options={levelOptions}
          error={errors.levelId}
        />
        <Field>
          <FieldLabel htmlFor="intake-condition">Mô tả tình trạng (không bắt buộc)</FieldLabel>
          <Input
            id="intake-condition"
            className="h-11"
            value={values.conditionDescription}
            onChange={(e) => set('conditionDescription')(e.target.value)}
          />
        </Field>
        <SelectField
          id="intake-giver"
          label="Người giao"
          value={values.giverType}
          onChange={set('giverType')}
          options={INTAKE_GIVERS.map((value) => ({ value, label: GIVER_LABELS[value] }))}
          error={errors.giverType}
        />
        {values.giverType === 'FAMILY_MEMBER' && (
          <Field data-invalid={!!errors.giverId || undefined}>
            <FieldLabel htmlFor="intake-giver-id">Mã người thân</FieldLabel>
            <Input
              id="intake-giver-id"
              inputMode="numeric"
              className="h-11"
              value={values.giverId}
              onChange={(e) => set('giverId')(e.target.value)}
              aria-invalid={!!errors.giverId || undefined}
            />
            {errors.giverId && <FieldError>{errors.giverId}</FieldError>}
          </Field>
        )}
        {['OTHER_PERSON', 'FAMILY_MEMBER'].includes(values.giverType) && (
          <div className="grid gap-4 sm:grid-cols-2">
            <Field data-invalid={!!errors.giverName || undefined}>
              <FieldLabel htmlFor="intake-giver-name">Họ tên người giao</FieldLabel>
              <Input
                id="intake-giver-name"
                autoComplete="off"
                className="h-11"
                value={values.giverName}
                onChange={(e) => set('giverName')(e.target.value)}
                aria-invalid={!!errors.giverName || undefined}
              />
              {errors.giverName && <FieldError>{errors.giverName}</FieldError>}
            </Field>
            <Field data-invalid={!!errors.giverIdDocument || undefined}>
              <FieldLabel htmlFor="intake-giver-doc">Giấy tờ người giao</FieldLabel>
              <Input
                id="intake-giver-doc"
                className="h-11"
                value={values.giverIdDocument}
                onChange={(e) => set('giverIdDocument')(e.target.value)}
                aria-invalid={!!errors.giverIdDocument || undefined}
              />
              {errors.giverIdDocument && <FieldError>{errors.giverIdDocument}</FieldError>}
            </Field>
          </div>
        )}
        <Field data-invalid={!!errors.at || undefined}>
          <FieldLabel htmlFor="intake-at">Thời điểm tiếp nhận</FieldLabel>
          <Input
            id="intake-at"
            type="datetime-local"
            className="h-11 tabular-nums"
            value={values.at}
            onChange={(e) => set('at')(e.target.value)}
            aria-invalid={!!errors.at || undefined}
          />
          {errors.at && <FieldError>{errors.at}</FieldError>}
        </Field>
        <SelectField
          id="intake-location"
          label="Vị trí lưu giữ"
          value={values.storageLocationId}
          onChange={set('storageLocationId')}
          options={activeOptions(locations)}
          error={errors.storageLocationId}
          placeholder={giveNow ? 'Không ghi vị trí' : 'Chọn…'}
        />
        <FilesField
          id="intake-photos"
          label="Ảnh đồ gửi"
          files={photos}
          onChange={setPhotos}
          multiple
          error={errors.photos}
          hint={
            type?.valuable
              ? 'Bắt buộc với đồ có giá trị. JPEG hoặc PNG, tối đa 10 MB mỗi ảnh.'
              : 'JPEG hoặc PNG, tối đa 10 MB mỗi ảnh.'
          }
        />
        <Field>
          <FieldLabel htmlFor="intake-note">Ghi chú (không bắt buộc)</FieldLabel>
          <Textarea
            id="intake-note"
            rows={2}
            maxLength={2000}
            value={values.note}
            onChange={(e) => set('note')(e.target.value)}
          />
        </Field>
        <label className="flex min-h-11 cursor-pointer items-center gap-2 rounded-lg border px-3">
          <Checkbox checked={giveNow} onCheckedChange={setGiveNow} aria-label="Giao ngay cho người cao tuổi sử dụng" />
          <span className="type-body-sm">Giao ngay cho người cao tuổi sử dụng</span>
        </label>
        {giveNow && type?.requiresConsentForUse && (
          <FilesField
            id="intake-signed-form"
            label="Bản ký đồng ý cho tự giữ"
            files={signedForm}
            onChange={setSignedForm}
            error={errors.signedForm}
            hint="Bản scan đồng ý của người đại diện. PDF, JPEG hoặc PNG, tối đa 10 MB."
          />
        )}
      </FieldGroup>
      {giveNow && type && !type.requiresConsentForUse && (
        <p className="type-body-sm text-muted-foreground">Loại đồ này không cần đồng ý khi giao sử dụng.</p>
      )}
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11 self-start" disabled={busy}>
        Lưu tiếp nhận
      </Button>
    </form>
  )
}
