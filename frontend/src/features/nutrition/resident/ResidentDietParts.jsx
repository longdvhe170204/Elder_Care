import { useState } from 'react'
import {
  getResidentsByResidentIdDietAssignments,
  getResidentsByResidentIdRelationships,
  postDietAssignmentsByIdApply,
  postDietAssignmentsByIdCancel,
  postFamilyFoodsByIdByCommand,
  postResidentsByResidentIdDietAssignments,
  postResidentsByResidentIdFamilyFoods,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { CheckList, Info, ReasonField, SELECT } from '../NutritionParts'
import { useCommand, useLoad } from '../useNutrition'
import {
  ASSIGNMENT_BASIS,
  ASSIGNMENT_STATUS,
  DISPOSAL,
  FAMILY_FOOD_COMMAND,
  FAMILY_FOOD_REASON,
  FAMILY_FOOD_STATUS,
  REVIEW_SOURCE,
  TEXTURE,
  familyFoodCommands,
  label,
} from '../nutritionView'

// Bản gán chế độ ăn (011 FR-010 → FR-017, BR-M08-03: Bác sĩ lập, áp dụng; lý do chuyên môn khi FR-013; cảnh báo xung đột
// phải xác nhận đã xem) và đồ ăn gia đình (FR-052 → FR-057, BR-M08-04: Bác sĩ, Điều dưỡng). Lệnh theo trạng thái; backend
// kiểm lại quyền, phạm vi.

const vietnamIso = (local) => (local ? `${local}:00+07:00` : '')

function Choices({ label: text, options, value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel>{text}</FieldLabel>
      <ToggleGroup
        variant="outline"
        value={value ? [value] : []}
        onValueChange={(n) => n[0] && onChange(n[0])}
        aria-label={text}
        className="flex-wrap"
      >
        {options.map(([code, name]) => (
          <ToggleGroupItem key={code} value={code} className="h-11 px-4">
            {name}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

// Ô lý do chuyên môn và ô "đã xem cảnh báo" mở theo lỗi 409 của lệnh áp dụng (FR-013, FR-016)
function ApplyExtras({ failure, reason, setReason, seen, setSeen, submitted }) {
  const needsReason = failure?.errorCode === 'PROFESSIONAL_REASON_REQUIRED'
  const needsSeen = failure?.errorCode === 'WARNINGS_NOT_CONFIRMED'
  return (
    <>
      {needsReason && (
        <ReasonField
          id="assignment-professional"
          label="Lý do chuyên môn"
          value={reason}
          onChange={setReason}
          error={submitted && !reason.trim() ? 'Nhập lý do chuyên môn' : null}
        />
      )}
      {needsSeen && (
        <Field orientation="horizontal" data-invalid={(submitted && !seen) || undefined}>
          <Checkbox id="assignment-seen" checked={seen} onCheckedChange={(c) => setSeen(c === true)} />
          <FieldLabel htmlFor="assignment-seen">Đã xem các cảnh báo xung đột trên</FieldLabel>
        </Field>
      )}
    </>
  )
}

export function ProposeSheet({ resident, diets, allergens, notices, onClose, onDone }) {
  const [form, setForm] = useState({
    dietId: '',
    texture: '',
    personalRestrictions: [],
    personalInstruction: '',
    basis: '',
    reviewNoticeId: '',
    effectiveFrom: '',
  })
  const [submitted, setSubmitted] = useState(false)
  const [result, setResult] = useState(null)
  const [professional, setProfessional] = useState('')
  const [seen, setSeen] = useState(false)
  const { busy, failure, run } = useCommand()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const errors = {
    dietId: !form.dietId ? 'Chọn chế độ ăn' : null,
    texture: !form.texture ? 'Chọn kết cấu' : null,
    basis: !form.basis ? 'Chọn căn cứ' : null,
    effectiveFrom: !form.effectiveFrom ? 'Chọn thời điểm áp dụng' : null,
  }
  const show = (k) => (submitted ? errors[k] : null)
  async function submit() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    const body = {
      dietId: Number(form.dietId),
      texture: form.texture,
      personalRestrictions: form.personalRestrictions,
      basis: form.basis,
      effectiveFrom: vietnamIso(form.effectiveFrom),
      ...(form.personalInstruction.trim() && { personalInstruction: form.personalInstruction.trim() }),
      ...(form.reviewNoticeId && { reviewNoticeId: Number(form.reviewNoticeId) }),
    }
    const response = await run(postResidentsByResidentIdDietAssignments, { path: { residentId: resident.id }, body })
    if (!response) return
    // Phản hồi lập đề xuất không có id bản gán: đọc lại danh sách, mỗi người chỉ một bản Đề xuất (BR-M08-03)
    const list = await getResidentsByResidentIdDietAssignments({ path: { residentId: resident.id } })
      .then((r) => r.data)
      .catch(() => [])
    const proposed = list.filter((a) => a.status === 'PROPOSED').sort((a, b) => b.id - a.id)[0]
    setResult({ check: response.data ?? {}, id: proposed?.id })
  }

  async function apply() {
    setSubmitted(true)
    if (result.check.professionalReasonRequired && !professional.trim()) return
    const codes = result.check.warningCodes ?? []
    if (codes.length > 0 && !seen) return
    const body = { confirmedWarnings: codes, ...(professional.trim() && { professionalReason: professional.trim() }) }
    if (await run(postDietAssignmentsByIdApply, { path: { id: result.id }, body })) onDone('Đã áp dụng bản gán')
  }
  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">{`Lập bản gán chế độ ăn cho ${resident.name}`}</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Bản gán ở Đề xuất tới khi áp dụng. Đổi liên quan điều trị cần lý do chuyên môn khi áp dụng.
        </SheetDescription>
        {result ? (
          <div className="mt-4 flex flex-col gap-3">
            <p className="type-body-sm">Đã lập đề xuất. Xem kết quả đối chiếu rồi áp dụng, hoặc đóng để áp dụng sau.</p>
            {(result.check.allergies?.length ?? 0) > 0 && (
              <p className="type-body-sm">{`Có ${result.check.allergies.length} món trong thực đơn chưa hết tuần xung đột với người này và chưa có món thay thế phù hợp.`}</p>
            )}
            {result.check.professionalReasonRequired && (
              <ReasonField
                id="propose-professional"
                label="Lý do chuyên môn"
                value={professional}
                onChange={setProfessional}
                error={submitted && !professional.trim() ? 'Nhập lý do chuyên môn' : null}
              />
            )}
            {(result.check.warningCodes?.length ?? 0) > 0 && (
              <Field orientation="horizontal" data-invalid={(submitted && !seen) || undefined}>
                <Checkbox id="propose-seen" checked={seen} onCheckedChange={(c) => setSeen(c === true)} />
                <FieldLabel htmlFor="propose-seen">{`Đã xem ${result.check.warningCodes.length} cảnh báo xung đột`}</FieldLabel>
              </Field>
            )}
            <ProblemAlert problem={failure} />
            <div className="flex justify-end gap-2">
              <Button variant="ghost" className="h-11" onClick={() => onDone('Đã lập đề xuất bản gán')}>
                Áp dụng sau
              </Button>
              {result.id && (
                <Button className="h-11" disabled={busy} onClick={apply}>
                  Áp dụng bản gán
                </Button>
              )}
            </div>
          </div>
        ) : (
          <>
            <FieldGroup className="mt-4">
              <Field data-invalid={!!show('dietId') || undefined}>
                <FieldLabel htmlFor="assignment-diet">Chế độ ăn</FieldLabel>
                <select
                  id="assignment-diet"
                  className={SELECT}
                  value={form.dietId}
                  onChange={(e) => set({ dietId: e.target.value })}
                  aria-invalid={!!show('dietId') || undefined}
                >
                  <option value="">Chọn chế độ ăn</option>
                  {diets.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
                {show('dietId') && <FieldError>{show('dietId')}</FieldError>}
              </Field>
              <Choices
                label="Kết cấu"
                options={Object.entries(TEXTURE)}
                value={form.texture}
                onChange={(texture) => set({ texture })}
                error={show('texture')}
              />
              <CheckList
                idPrefix="assignment-restriction"
                legend="Hạn chế riêng"
                options={allergens}
                value={form.personalRestrictions}
                onChange={(personalRestrictions) => set({ personalRestrictions })}
              />
              <Field>
                <FieldLabel htmlFor="assignment-instruction">Chỉ dẫn riêng</FieldLabel>
                <Textarea
                  id="assignment-instruction"
                  rows={2}
                  value={form.personalInstruction}
                  onChange={(e) => set({ personalInstruction: e.target.value })}
                />
              </Field>
              <Choices
                label="Căn cứ"
                options={Object.entries(ASSIGNMENT_BASIS)}
                value={form.basis}
                onChange={(basis) => set({ basis })}
                error={show('basis')}
              />
              {notices.length > 0 && (
                <Field>
                  <FieldLabel htmlFor="assignment-notice">Thông báo xem lại liên quan</FieldLabel>
                  <select
                    id="assignment-notice"
                    className={SELECT}
                    value={form.reviewNoticeId}
                    onChange={(e) => set({ reviewNoticeId: e.target.value })}
                  >
                    <option value="">Không gắn</option>
                    {notices.map((n) => (
                      <option key={n.id} value={n.id}>
                        {`${label(REVIEW_SOURCE, n.source)}, ${formatDateTime(n.noticedAt)}`}
                      </option>
                    ))}
                  </select>
                </Field>
              )}
              <Field data-invalid={!!show('effectiveFrom') || undefined}>
                <FieldLabel htmlFor="assignment-from">Áp dụng từ</FieldLabel>
                <Input
                  id="assignment-from"
                  type="datetime-local"
                  className="h-11"
                  value={form.effectiveFrom}
                  onChange={(e) => set({ effectiveFrom: e.target.value })}
                  aria-invalid={!!show('effectiveFrom') || undefined}
                />
                {show('effectiveFrom') && <FieldError>{show('effectiveFrom')}</FieldError>}
              </Field>
            </FieldGroup>
            <div className="mt-4 flex flex-col gap-3">
              <ProblemAlert problem={failure} />
              <div className="flex justify-end gap-2">
                <Button variant="ghost" className="h-11" onClick={onClose}>
                  Đóng
                </Button>
                <Button className="h-11" disabled={busy} onClick={submit}>
                  Lập đề xuất
                </Button>
              </div>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}

export function PendingAssignment({ assignment, dietName, doctor, onChanged }) {
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [professional, setProfessional] = useState('')
  const [seen, setSeen] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, setFailure, run } = useCommand()
  // Mã cảnh báo FR-016 lấy từ 409 của lần áp dụng trước (unmetConditions; chủ dự án chọn 2026-11-08)
  const warnings = failure?.errorCode === 'WARNINGS_NOT_CONFIRMED' ? failure.unmetConditions.map((u) => u.code) : []

  async function apply() {
    setSubmitted(true)
    if (failure?.errorCode === 'PROFESSIONAL_REASON_REQUIRED' && !professional.trim()) return
    if (warnings.length > 0 && !seen) return
    const body = {
      confirmedWarnings: seen ? warnings : [],
      ...(professional.trim() && { professionalReason: professional.trim() }),
    }
    if (await run(postDietAssignmentsByIdApply, { path: { id: assignment.id }, body })) onChanged('Đã áp dụng bản gán')
  }

  return (
    <li className="flex flex-col gap-2 rounded-md border p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-label">{`${dietName(assignment.dietId)}, kết cấu ${label(TEXTURE, assignment.texture).toLowerCase()}`}</span>
        <StatusBadge status={label(ASSIGNMENT_STATUS, assignment.status)} />
      </div>
      <span className="type-body-sm text-muted-foreground">{`Áp dụng từ ${formatDateTime(assignment.effectiveFrom)}, căn cứ ${label(ASSIGNMENT_BASIS, assignment.basis).toLowerCase()}`}</span>
      <ProblemAlert problem={failure} />
      {mode !== 'cancel' && (
        <ApplyExtras
          failure={failure}
          reason={professional}
          setReason={setProfessional}
          seen={seen}
          setSeen={setSeen}
          submitted={submitted}
        />
      )}
      {mode === 'cancel' && (
        <ReasonField
          id={`assignment-cancel-${assignment.id}`}
          value={reason}
          onChange={setReason}
          error={submitted && !reason.trim() ? 'Nhập lý do' : null}
        />
      )}
      {doctor && (
        <div className="flex flex-wrap justify-end gap-2">
          {mode === 'cancel' ? (
            <>
              <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
                Thôi
              </Button>
              <Button
                variant="destructive"
                className="h-11"
                disabled={busy}
                onClick={async () => {
                  setSubmitted(true)
                  if (
                    reason.trim() &&
                    (await run(postDietAssignmentsByIdCancel, {
                      path: { id: assignment.id },
                      body: { reason: reason.trim() },
                    }))
                  )
                    onChanged('Đã hủy bản gán')
                }}
              >
                Hủy bản gán
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outline"
                className="h-11"
                onClick={() => {
                  setFailure(null)
                  setSubmitted(false)
                  setMode('cancel')
                }}
              >
                Hủy bản gán…
              </Button>
              {assignment.status === 'PROPOSED' && (
                <Button className="h-11" disabled={busy} onClick={apply}>
                  Áp dụng
                </Button>
              )}
            </>
          )}
        </div>
      )}
    </li>
  )
}

export function FamilyFoodSheet({ resident, allergens, onClose, onDone }) {
  const relations = useLoad(
    () => getResidentsByResidentIdRelationships({ path: { residentId: resident.id } }).then((r) => r.data),
    [resident.id],
  )
  const [form, setForm] = useState({
    senderName: '',
    familyMemberId: '',
    type: '',
    description: '',
    ingredients: [],
    ingredientsUnknown: false,
    expiresAt: '',
    storageMethod: '',
  })
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const errors = {
    senderName: !form.senderName.trim() ? 'Nhập người gửi' : null,
    type: !form.type.trim() ? 'Nhập loại đồ ăn' : null,
    description: !form.description.trim() ? 'Mô tả đồ ăn' : null,
    expiresAt: !form.expiresAt ? 'Nhập hạn dùng' : null,
    storageMethod: !form.storageMethod.trim() ? 'Nhập cách bảo quản' : null,
  }
  const show = (k) => (submitted ? errors[k] : null)
  function pickMember(id) {
    const r = (relations.data ?? []).find((x) => String(x.familyMember.id) === id)
    set({ familyMemberId: id, ...(r && !form.senderName.trim() && { senderName: r.familyMember.fullName }) })
  }
  async function submit() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    const body = {
      senderName: form.senderName.trim(),
      type: form.type.trim(),
      description: form.description.trim(),
      ingredients: form.ingredients,
      ingredientsUnknown: form.ingredientsUnknown,
      expiresAt: vietnamIso(form.expiresAt),
      storageMethod: form.storageMethod.trim(),
      ...(form.familyMemberId && { familyMemberId: Number(form.familyMemberId) }),
    }
    const response = await run(postResidentsByResidentIdFamilyFoods, { path: { residentId: resident.id }, body })
    if (response) onDone(`Đã ghi nhận đồ ăn gia đình: ${label(FAMILY_FOOD_STATUS, response.data?.status)}`)
  }
  const field = (key, text, type = 'text') => (
    <Field data-invalid={!!show(key) || undefined}>
      <FieldLabel htmlFor={`family-food-${key}`}>{text}</FieldLabel>
      <Input
        id={`family-food-${key}`}
        type={type}
        className="h-11"
        value={form[key]}
        onChange={(e) => set({ [key]: e.target.value })}
        aria-invalid={!!show(key) || undefined}
      />
      {show(key) && <FieldError>{show(key)}</FieldError>}
    </Field>
  )
  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">{`Ghi nhận đồ ăn gia đình mang cho ${resident.name}`}</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Hệ thống đối chiếu ngay với danh mục cấm, hạn dùng, dị ứng và hạn chế rồi gán trạng thái (FR-053).
        </SheetDescription>
        <FieldGroup className="mt-4">
          {(relations.data ?? []).length > 0 && (
            <Field>
              <FieldLabel htmlFor="family-food-member">Người thân gửi</FieldLabel>
              <select
                id="family-food-member"
                className={SELECT}
                value={form.familyMemberId}
                onChange={(e) => pickMember(e.target.value)}
              >
                <option value="">Không có trong danh sách người thân</option>
                {relations.data.map((r) => (
                  <option key={r.familyMember.id} value={r.familyMember.id}>
                    {r.familyMember.fullName}
                  </option>
                ))}
              </select>
            </Field>
          )}
          {field('senderName', 'Họ tên người gửi')}
          {field('type', 'Loại đồ ăn')}
          {field('description', 'Mô tả')}
          <CheckList
            idPrefix="family-food-ingredient"
            legend="Thành phần"
            options={allergens}
            value={form.ingredients}
            onChange={(ingredients) => set({ ingredients })}
          />
          <Field orientation="horizontal">
            <Checkbox
              id="family-food-unknown"
              checked={form.ingredientsUnknown}
              onCheckedChange={(c) => set({ ingredientsUnknown: c === true })}
            />
            <FieldLabel htmlFor="family-food-unknown">Không rõ thành phần</FieldLabel>
          </Field>
          {field('expiresAt', 'Hạn dùng', 'datetime-local')}
          {field('storageMethod', 'Cách bảo quản')}
        </FieldGroup>
        <div className="mt-4 flex flex-col gap-3">
          <ProblemAlert problem={failure} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={onClose}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={submit}>
              Ghi nhận
            </Button>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}

export function FamilyFoodItem({ food, canConfirm, canUpdate, onChanged }) {
  const [mode, setMode] = useState(null)
  const [text, setText] = useState('')
  const [method, setMethod] = useState('')
  const [returnedTo, setReturnedTo] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { busy, failure, run } = useCommand()
  const commands = familyFoodCommands(food).filter((c) =>
    c === 'approve-use' || c === 'reject-use' ? canConfirm : canUpdate,
  )
  const methods = Object.entries(DISPOSAL).filter(([code]) => mode === 'end' || code !== 'CONSUMED')

  async function send(cmd) {
    setSubmitted(true)
    let body = {}
    if (cmd === 'approve-use') body = text.trim() ? { note: text.trim() } : {}
    if (cmd === 'reject-use') {
      if (!text.trim()) return
      body = { reason: text.trim() }
    }
    if (cmd === 'record-disposal' || cmd === 'end') {
      if (!method || (method === 'RETURNED' && !returnedTo.trim())) return
      body = { method, ...(method === 'RETURNED' && { returnedTo: returnedTo.trim() }) }
    }
    if (await run(postFamilyFoodsByIdByCommand, { path: { id: food.id, command: cmd }, body }))
      onChanged(`${FAMILY_FOOD_COMMAND[cmd].replace('…', '')}: xong`)
  }

  return (
    <li className="flex flex-col gap-2 rounded-md border p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-label">{`${food.type}: ${food.description}`}</span>
        <StatusBadge status={label(FAMILY_FOOD_STATUS, food.status)} />
      </div>
      <span className="type-body-sm text-muted-foreground">{`${food.senderName} gửi, hạn dùng ${formatDateTime(food.expiresAt)}, bảo quản ${food.storageMethod}`}</span>
      {food.systemReasons?.length > 0 && (
        <ul className="ml-4 list-disc type-body-sm">
          {food.systemReasons.map((r) => (
            <li key={r}>{label(FAMILY_FOOD_REASON, r)}</li>
          ))}
        </ul>
      )}
      <ProblemAlert problem={failure} />
      {mode && (
        <div className="flex flex-col gap-2">
          {(mode === 'approve-use' || mode === 'reject-use') && (
            <ReasonField
              id={`family-food-text-${food.id}`}
              label={mode === 'approve-use' ? 'Ghi chú' : 'Lý do'}
              value={text}
              onChange={setText}
              error={mode === 'reject-use' && submitted && !text.trim() ? 'Nhập lý do' : null}
            />
          )}
          {(mode === 'record-disposal' || mode === 'end') && (
            <>
              <Choices
                label="Cách xử lý"
                options={methods}
                value={method}
                onChange={setMethod}
                error={submitted && !method ? 'Chọn cách xử lý' : null}
              />
              {method === 'RETURNED' && (
                <Field data-invalid={(submitted && !returnedTo.trim()) || undefined}>
                  <FieldLabel htmlFor={`family-food-returned-${food.id}`}>Người nhận lại</FieldLabel>
                  <Input
                    id={`family-food-returned-${food.id}`}
                    className="h-11"
                    value={returnedTo}
                    onChange={(e) => setReturnedTo(e.target.value)}
                  />
                  {submitted && !returnedTo.trim() && <FieldError>Ghi người nhận lại</FieldError>}
                </Field>
              )}
            </>
          )}
        </div>
      )}
      {commands.length > 0 && (
        <div className="flex flex-wrap justify-end gap-2">
          {mode ? (
            <>
              <Button variant="ghost" className="h-11" onClick={() => setMode(null)}>
                Thôi
              </Button>
              <Button className="h-11" disabled={busy} onClick={() => send(mode)}>
                {FAMILY_FOOD_COMMAND[mode].replace('…', '')}
              </Button>
            </>
          ) : (
            commands.map((cmd) => (
              <Button
                key={cmd}
                variant="outline"
                className="h-11"
                onClick={() => {
                  setSubmitted(false)
                  setText('')
                  setMethod('')
                  setMode(cmd)
                }}
              >
                {FAMILY_FOOD_COMMAND[cmd]}
              </Button>
            ))
          )}
        </div>
      )}
    </li>
  )
}

export function AssignmentSummary({ assignment, dietName }) {
  if (!assignment)
    return (
      <p className="type-body-sm text-muted-foreground">
        Chưa có bản gán Hiệu lực; người cao tuổi dùng chế độ ăn mặc định của viện.
      </p>
    )
  return (
    <div className="grid gap-3">
      <Info label="Chế độ ăn">{dietName(assignment.dietId)}</Info>
      <Info label="Kết cấu">{label(TEXTURE, assignment.texture)}</Info>
      {assignment.personalInstruction && <Info label="Chỉ dẫn riêng">{assignment.personalInstruction}</Info>}
      <Info label="Hiệu lực từ">{formatDateTime(assignment.actualEffectiveAt ?? assignment.effectiveFrom)}</Info>
      {assignment.professionalReason && <Info label="Lý do chuyên môn">{assignment.professionalReason}</Info>}
    </div>
  )
}
