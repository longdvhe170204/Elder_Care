import { useCallback, useEffect, useState } from 'react'
import {
  getAssessmentsById,
  postAssessmentsByIdConfirm,
  postAssessmentsByIdDiscardDraft,
  putAssessmentsById,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { HEALTH_RECORD_FIELDS, HEALTH_TYPE_LABELS, HEALTH_TYPE_ORDER } from '../residents/healthLabels'
import {
  ASSESSMENT_STATUS_LABELS,
  ASSESSMENT_TYPE_LABELS,
  CARE_LEVEL_LABELS,
  CARE_LEVEL_ORDER,
  RESIDENCE_TYPE_LABELS,
} from './assessmentLabels'

// Phiếu đánh giá đang soạn (001 T042; US3; FR-024 → FR-029, FR-031; hợp đồng GET, PUT /assessments/{id},
// POST …/confirm, …/discard-draft). Bên trái phiếu, bên phải thông tin tham khảo (FR-024). Không có ô điểm và không có
// mức đề xuất: Bác sĩ tự chọn mức. Sửa nháp dùng If-Match lấy từ ETag lần đọc gần nhất; chỉ Bác sĩ xác nhận; quyền thật
// do backend kiểm, nút hiện theo allowedCommands của hồ sơ.

const NOTE_MAX = 1000

function blank(value) {
  const text = value.trim()
  return text === '' ? undefined : text
}

function formOf(a) {
  return {
    basis: a.basis ?? '',
    comment: a.comment ?? '',
    careNote: a.careNote ?? '',
    careLevel: a.careLevel ?? '',
    careNeedIds: new Set((a.careNeeds ?? []).map((n) => n.id)),
    proposedResidenceType: a.proposedResidenceType ?? '',
  }
}

function bodyOf(form) {
  return {
    basis: blank(form.basis),
    comment: blank(form.comment),
    careNote: blank(form.careNote),
    careLevel: form.careLevel || undefined,
    careNeedIds: [...form.careNeedIds],
    proposedResidenceType: form.proposedResidenceType || undefined,
  }
}

// Tác động của lệnh Xác nhận, nêu trước khi bấm (research H-10; FR-033, FR-035, FR-036).
function effectOf({ type, residentStatus, currentLevel, level }) {
  const chosen = CARE_LEVEL_LABELS[level] ?? 'mức đã chọn'
  if (type === 'INITIAL' || residentStatus === 'ADMISSION_IN_PROGRESS') {
    return `${chosen} sẽ trở thành mức chăm sóc hiện hành ngay khi xác nhận.`
  }
  if (currentLevel !== level) {
    return `Mức hiện hành (${CARE_LEVEL_LABELS[currentLevel] ?? 'chưa có'}) giữ nguyên. Hành chính được báo lập yêu cầu thay đổi lưu trú loại đổi mức sang ${chosen}; yêu cầu đổi mức chưa kết thúc của người này chuyển sang Được thay thế.`
  }
  return 'Mức chăm sóc không đổi. Yêu cầu đổi mức chưa kết thúc của người này (nếu có) chuyển sang Được thay thế.'
}

function Reference({ reference }) {
  const initial = reference?.initialHealthRecord
  const items = reference?.activeHealthItems ?? []
  const vitals = reference?.latestVitalSigns ?? []
  return (
    <aside aria-label="Thông tin tham khảo" className="flex flex-col gap-4 rounded-lg border bg-muted/30 p-4">
      <p className="type-caption text-muted-foreground">
        Thông tin tham khảo khi đánh giá. Hệ thống không tính điểm và không đề xuất mức chăm sóc.
      </p>

      <section aria-labelledby="ref-initial" className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <h4 id="ref-initial" className="type-label">
            Hồ sơ sức khỏe ban đầu
          </h4>
          {initial && <StatusBadge status={initial.confirmed ? 'Đã xác nhận' : 'Bản nháp'} />}
        </div>
        {initial ? (
          <dl className="flex flex-col gap-2">
            {HEALTH_RECORD_FIELDS.filter((f) => initial[f.name]).map((f) => (
              <div key={f.name} className="flex flex-col">
                <dt className="type-caption text-muted-foreground">{f.label}</dt>
                <dd className="type-body-sm whitespace-pre-wrap">{initial[f.name]}</dd>
              </div>
            ))}
          </dl>
        ) : (
          <p className="type-body-sm text-muted-foreground">Chưa lập hồ sơ sức khỏe ban đầu.</p>
        )}
      </section>

      <section aria-labelledby="ref-items" className="flex flex-col gap-2">
        <h4 id="ref-items" className="type-label">
          Dị ứng, bệnh nền, tiền sử đang hiệu lực
        </h4>
        {items.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Không có mục nào đang hiệu lực.</p>
        ) : (
          HEALTH_TYPE_ORDER.filter((t) => items.some((i) => i.type === t)).map((t) => (
            <div key={t} className="flex flex-col">
              <span className="type-caption text-muted-foreground">{HEALTH_TYPE_LABELS[t]}</span>
              <ul className="type-body-sm">
                {items
                  .filter((i) => i.type === t)
                  .map((i) => (
                    <li key={i.id}>
                      {i.allergen?.name ?? i.freeText}
                      {i.severity ? `, mức ${i.severity}` : ''}
                    </li>
                  ))}
              </ul>
            </div>
          ))
        )}
      </section>

      <section aria-labelledby="ref-vitals" className="flex flex-col gap-2">
        <h4 id="ref-vitals" className="type-label">
          Chỉ số đo gần nhất
        </h4>
        {vitals.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">
            Chưa có dữ liệu chỉ số đo: phần này chờ kết nối với tính năng đo chỉ số sức khỏe.
          </p>
        ) : (
          <ul className="type-body-sm">
            {vitals.map((v, index) => (
              <li key={index}>{Object.values(v).join(' · ')}</li>
            ))}
          </ul>
        )}
      </section>
    </aside>
  )
}

export function AssessmentForm({
  assessmentId,
  residentStatus,
  currentLevel,
  careNeedOptions,
  canConfirm,
  onChanged,
  onConfirmed,
}) {
  const [state, setState] = useState('loading')
  const [detail, setDetail] = useState(null)
  const [etag, setEtag] = useState(null)
  const [form, setForm] = useState(null)
  const [failure, setFailure] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [discarding, setDiscarding] = useState(false)
  const [discardReason, setDiscardReason] = useState('')
  const [discardError, setDiscardError] = useState('')

  const load = useCallback(async () => {
    try {
      const { data, etag: tag } = await getAssessmentsById({ path: { id: assessmentId } })
      setDetail(data)
      setEtag(tag)
      setForm(formOf(data))
      setFailure(null)
      setState('ready')
    } catch (error) {
      setFailure(problemOf(error))
      setState('error')
    }
  }, [assessmentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp phiếu khi mở tab
    load()
  }, [load])

  const set = (name) => (event) => {
    setSaved(false)
    setForm((current) => ({ ...current, [name]: event.target.value }))
  }

  function toggleNeed(id, checked) {
    setSaved(false)
    setForm((current) => {
      const ids = new Set(current.careNeedIds)
      if (checked) ids.add(id)
      else ids.delete(id)
      return { ...current, careNeedIds: ids }
    })
  }

  // Lưu nháp; trả ETag mới (hoặc null khi lỗi) để lệnh Xác nhận dùng ngay.
  async function persist() {
    const { data, etag: tag } = await putAssessmentsById({
      path: { id: assessmentId },
      headers: { 'If-Match': etag },
      body: bodyOf(form),
    })
    setDetail(data)
    setEtag(tag)
    return tag
  }

  async function save(event) {
    event.preventDefault()
    setFailure(null)
    setFieldErrors({})
    setBusy(true)
    try {
      await persist()
      setSaved(true)
      onChanged?.()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  async function confirm() {
    setFailure(null)
    setFieldErrors({})
    setBusy(true)
    try {
      const tag = await persist()
      const { data } = await postAssessmentsByIdConfirm({
        path: { id: assessmentId },
        headers: { 'If-Match': tag },
      })
      setConfirmOpen(false)
      onConfirmed?.(data)
    } catch (error) {
      setConfirmOpen(false)
      const problem = problemOf(error)
      const codes = problem.unmetConditions.map((c) => c.code)
      if (problem.errorCode === 'CONDITION_NOT_MET' && codes.length > 0) {
        // Thiếu nhận xét hoặc mức (FR-028): báo tại ô, không chặn lưu nháp
        setFieldErrors({
          comment: codes.includes('comment') ? 'Nhập nhận xét đánh giá trước khi xác nhận' : undefined,
          careLevel: codes.includes('careLevel') ? 'Chọn một mức chăm sóc trước khi xác nhận' : undefined,
        })
      } else {
        setFailure(problem)
      }
    } finally {
      setBusy(false)
    }
  }

  async function discard(event) {
    event.preventDefault()
    if (!discardReason.trim()) {
      setDiscardError('Nhập lý do hủy nháp')
      return
    }
    setDiscardError('')
    setBusy(true)
    try {
      await postAssessmentsByIdDiscardDraft({ path: { id: assessmentId }, body: { reason: discardReason.trim() } })
      onChanged?.()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (state === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2">
        <span className="sr-only">Đang tải phiếu đánh giá</span>
        <Skeleton className="h-10" />
        <Skeleton className="h-32" />
      </div>
    )
  }
  if (state === 'error') return <ProblemAlert problem={failure} />

  const problem = (
    <ProblemAlert problem={failure}>
      {failure?.status === 412 && (
        <Button type="button" variant="outline" className="h-11 self-start" onClick={load}>
          Xem giá trị mới
        </Button>
      )}
    </ProblemAlert>
  )
  const isInitial = detail.type === 'INITIAL'

  return (
    <section aria-labelledby="assessment-draft-title" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <h3 id="assessment-draft-title" className="type-card-title">
          {ASSESSMENT_TYPE_LABELS[detail.type]} đang soạn
        </h3>
        <StatusBadge status={ASSESSMENT_STATUS_LABELS[detail.status]} />
        <span className="type-caption text-muted-foreground">Người lập: {detail.createdBy}</span>
      </div>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <form onSubmit={save} noValidate className="flex flex-col gap-4">
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="assessment-basis">Căn cứ, lý do</FieldLabel>
              <Input
                id="assessment-basis"
                value={form.basis}
                onChange={set('basis')}
                maxLength={1000}
                className="h-10"
              />
            </Field>

            <Field data-invalid={!!fieldErrors.comment || undefined}>
              <FieldLabel htmlFor="assessment-comment">Nhận xét đánh giá (bắt buộc khi xác nhận)</FieldLabel>
              <Textarea
                id="assessment-comment"
                rows={6}
                value={form.comment}
                onChange={set('comment')}
                aria-invalid={!!fieldErrors.comment || undefined}
              />
              <FieldDescription>
                Tình trạng chung, vận động, tự chăm sóc, nhận thức và các nguy cơ cần lưu ý.
              </FieldDescription>
              {fieldErrors.comment && <FieldError>{fieldErrors.comment}</FieldError>}
            </Field>

            <Field>
              <FieldLabel htmlFor="assessment-care-note">Lưu ý chăm sóc (hiện trên thẻ người cao tuổi)</FieldLabel>
              <Textarea
                id="assessment-care-note"
                rows={2}
                maxLength={NOTE_MAX}
                value={form.careNote}
                onChange={set('careNote')}
              />
              <FieldDescription className="tabular-nums">
                {form.careNote.length}/{NOTE_MAX} ký tự
              </FieldDescription>
            </Field>

            <Field data-invalid={!!fieldErrors.careLevel || undefined}>
              <FieldLabel id="assessment-level-label">Mức chăm sóc trong phiếu (bắt buộc khi xác nhận)</FieldLabel>
              <ToggleGroup
                variant="outline"
                aria-labelledby="assessment-level-label"
                value={form.careLevel ? [form.careLevel] : []}
                onValueChange={(next) => {
                  setSaved(false)
                  setForm((current) => ({ ...current, careLevel: next[0] ?? '' }))
                }}
                className="grid w-full grid-cols-1 sm:grid-cols-3"
              >
                {CARE_LEVEL_ORDER.map((code) => (
                  <ToggleGroupItem key={code} value={code} className="h-11">
                    {CARE_LEVEL_LABELS[code]}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
              <FieldDescription>
                Mức này chưa áp dụng cho tới khi Bác sĩ xác nhận.
                {!isInitial && residentStatus !== 'ADMISSION_IN_PROGRESS'
                  ? ' Người cao tuổi đã lưu trú: mức hiện hành chỉ đổi theo yêu cầu thay đổi lưu trú.'
                  : ''}
              </FieldDescription>
              {fieldErrors.careLevel && <FieldError>{fieldErrors.careLevel}</FieldError>}
            </Field>

            <Field>
              <FieldLabel id="assessment-needs-label">Nhu cầu chăm sóc</FieldLabel>
              {careNeedOptions.length === 0 ? (
                <p className="type-body-sm text-muted-foreground">Danh mục nhu cầu chăm sóc đang trống.</p>
              ) : (
                <ul aria-labelledby="assessment-needs-label" className="flex flex-wrap gap-2">
                  {careNeedOptions.map((n) => (
                    <li key={n.id}>
                      <label className="flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border px-3">
                        <Checkbox
                          checked={form.careNeedIds.has(n.id)}
                          onCheckedChange={(checked) => toggleNeed(n.id, checked)}
                        />
                        <span className="type-body-sm">{n.name}</span>
                      </label>
                    </li>
                  ))}
                </ul>
              )}
            </Field>

            {isInitial && (
              <Field>
                <FieldLabel id="assessment-residence-label">Loại hình lưu trú đề xuất (không bắt buộc)</FieldLabel>
                <ToggleGroup
                  variant="outline"
                  aria-labelledby="assessment-residence-label"
                  value={form.proposedResidenceType ? [form.proposedResidenceType] : []}
                  onValueChange={(next) => {
                    setSaved(false)
                    setForm((current) => ({ ...current, proposedResidenceType: next[0] ?? '' }))
                  }}
                  className="grid w-full grid-cols-1 sm:grid-cols-3"
                >
                  {Object.entries(RESIDENCE_TYPE_LABELS).map(([code, label]) => (
                    <ToggleGroupItem key={code} value={code} className="h-11">
                      {label}
                    </ToggleGroupItem>
                  ))}
                </ToggleGroup>
                <FieldDescription>Chỉ để tham khảo; loại hình chính thức do hợp đồng quyết định.</FieldDescription>
              </Field>
            )}
          </FieldGroup>

          {problem}
          <p role="status" aria-live="polite" className="type-caption text-muted-foreground">
            {saved ? 'Đã lưu bản nháp.' : ''}
          </p>

          <div className="flex flex-wrap gap-3">
            <Button type="submit" variant="outline" className="h-11" disabled={busy}>
              Lưu nháp
            </Button>
            {canConfirm && (
              <Button type="button" className="h-11" disabled={busy} onClick={() => setConfirmOpen(true)}>
                Xác nhận đánh giá
              </Button>
            )}
            <Button
              type="button"
              variant="ghost"
              className="h-11"
              disabled={busy}
              onClick={() => setDiscarding((open) => !open)}
            >
              Hủy nháp…
            </Button>
          </div>

          {discarding && (
            <div className="flex flex-col gap-2 rounded-lg border p-3">
              <Field data-invalid={!!discardError || undefined}>
                <FieldLabel htmlFor="assessment-discard-reason">Lý do hủy nháp</FieldLabel>
                <Textarea
                  id="assessment-discard-reason"
                  rows={2}
                  value={discardReason}
                  onChange={(event) => setDiscardReason(event.target.value)}
                  aria-invalid={!!discardError || undefined}
                />
                {discardError && <FieldError>{discardError}</FieldError>}
              </Field>
              <Button type="button" variant="outline" className="h-11 self-start" disabled={busy} onClick={discard}>
                Hủy bản nháp này
              </Button>
              <p className="type-caption text-muted-foreground">
                Bản đã hủy vẫn được lưu trong lịch sử và không sửa được.
              </p>
            </div>
          )}
        </form>

        <Reference reference={detail.reference} />
      </div>

      <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác nhận đánh giá</DialogTitle>
            <DialogDescription>
              {effectOf({ type: detail.type, residentStatus, currentLevel, level: form.careLevel })} Sau khi xác nhận
              không sửa được nữa, chỉ đính chính.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button type="button" variant="outline" className="h-11" onClick={() => setConfirmOpen(false)}>
              Quay lại soạn
            </Button>
            <Button type="button" className="h-11" disabled={busy} onClick={confirm}>
              Xác nhận
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  )
}
