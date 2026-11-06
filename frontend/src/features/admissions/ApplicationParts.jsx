import { useEffect, useState } from 'react'
import {
  getAdmissionApplicationsById,
  getAdmissionApplicationsByIdAdmissionConditions,
  getAdmissionApplicationsByIdProgress,
  postAdmissionApplications,
  postAdmissionApplicationsByIdAdmissionOpinion,
  postAdmissionApplicationsByIdUpdate,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import {
  APPLICATION_STATUS,
  CARE_LEVELS,
  CONDITION_STATUS,
  OPINIONS,
  OPINION_SOURCES,
  PROGRESS_STEPS,
  RESIDENCE_TYPES,
  labelOf,
  newKey,
} from './admissionLabels'
import { Choices, Info, ReasonField, ResidentSelect } from './FormParts'

// Khung chi tiết và form của lượt đăng ký tiếp nhận (004 T019; FR-001, FR-003, FR-004, FR-004a, FR-005). Danh sách điều
// kiện tiếp nhận (a) → (f) tách khỏi ý kiến của người cao tuổi về việc vào ở: ý kiến không là điều kiện (Q-356).

const OPEN_STATUSES = ['IN_PROGRESS', 'WAITING']

function OpinionBlock({ opinions }) {
  if (!opinions.length)
    return <p className="type-body-sm text-muted-foreground">Chưa ghi ý kiến về việc vào ở.</p>
  return (
    <ul className="flex flex-col gap-2">
      {opinions.map((o) => (
        <li key={o.id} className="type-body-sm">
          <span className={o.status === 'SUPERSEDED' ? 'text-muted-foreground' : undefined}>
            {labelOf(OPINIONS, o.opinion)}, nguồn {labelOf(OPINION_SOURCES, o.source?.type).toLowerCase()}
          </span>
          <span className="block type-caption text-muted-foreground">
            {o.status === 'SUPERSEDED' ? 'Đã được thay thế. ' : 'Hiện hành. '}
            {o.recordedBy ? `Ghi bởi ${o.recordedBy}, ` : ''}
            {o.at ? formatDateTime(o.at) : ''}
            {o.note ? `. ${o.note}` : ''}
          </span>
        </li>
      ))}
    </ul>
  )
}

function OpinionForm({ applicationId, onDone, onCancel }) {
  const [source, setSource] = useState('')
  const [opinion, setOpinion] = useState('')
  const [note, setNote] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  async function save() {
    setSubmitted(true)
    if (!source || !opinion) return
    setBusy(true)
    try {
      await postAdmissionApplicationsByIdAdmissionOpinion({
        path: { id: applicationId },
        headers: newKey(),
        body: { opinion, source: { type: source }, ...(note.trim() && { note: note.trim() }) },
      })
      onDone('Đã ghi ý kiến về việc vào ở')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <FieldGroup>
        <Choices
          label="Nguồn ý kiến"
          options={OPINION_SOURCES.filter(([code]) => code !== 'FAMILY_MEMBER')}
          value={source}
          onChange={setSource}
          error={submitted && !source ? 'Chọn nguồn ý kiến' : null}
        />
        <Choices
          label="Ý kiến"
          options={OPINIONS}
          value={opinion}
          onChange={setOpinion}
          error={submitted && !opinion ? 'Chọn ý kiến' : null}
        />
        <ReasonField id="opinion-note" label="Ghi chú" value={note} onChange={setNote} />
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
        <Button className="h-11" disabled={busy} onClick={save}>
          Lưu ý kiến
        </Button>
      </div>
    </div>
  )
}

export function ApplicationDetail({ application, residentName, canUpdate, onChanged, onEdit, Title, Description }) {
  const [conditions, setConditions] = useState(null)
  const [progress, setProgress] = useState(null)
  const [failure, setFailure] = useState(null)
  const [writing, setWriting] = useState(false)
  const [reload, setReload] = useState(0)
  const id = application.id
  const open = OPEN_STATUSES.includes(application.status)

  useEffect(() => {
    let cancelled = false
    Promise.all([
      getAdmissionApplicationsByIdAdmissionConditions({ path: { id } }),
      getAdmissionApplicationsByIdProgress({ path: { id } }),
    ])
      .then(([c, p]) => {
        if (cancelled) return
        setConditions(c.data)
        setProgress(p.data)
        setFailure(null)
      })
      .catch((e) => !cancelled && setFailure(problemOf(e)))
    return () => {
      cancelled = true
    }
  }, [id, reload])

  const opinions = conditions?.admissionOpinion
    ? [conditions.admissionOpinion]
    : (application.admissionOpinion ?? [])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={APPLICATION_STATUS[application.status] ?? application.status} />
        <Title className="type-title">{residentName}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {`Đăng ký ngày ${formatDate(application.applicationDate)}`}
        </Description>
      </div>
      <div className="grid gap-3">
        <Info label="Loại lưu trú mong muốn">{labelOf(RESIDENCE_TYPES, application.desiredResidenceType)}</Info>
        <Info label="Mức chăm sóc dự kiến">{labelOf(CARE_LEVELS, application.expectedCareLevel)}</Info>
        {application.desiredAdmissionDate && (
          <Info label="Ngày vào dự kiến">{formatDate(application.desiredAdmissionDate)}</Info>
        )}
        {application.contactPerson && <Info label="Người liên hệ">{application.contactPerson}</Info>}
        {application.needs && <Info label="Nhu cầu">{application.needs}</Info>}
        {application.note && <Info label="Ghi chú">{application.note}</Info>}
      </div>
      <ProblemAlert problem={failure} />

      {progress && (
        <section className="flex flex-col gap-2">
          <h3 className="type-card-title">Tiến trình</h3>
          <ol className="flex flex-col gap-1">
            {progress.steps.map((s) => (
              <li key={s.name} className="type-body-sm">
                {PROGRESS_STEPS[s.name] ?? s.name}
                <span className="text-muted-foreground">{s.done ? ', đã xong' : ', chưa xong'}</span>
              </li>
            ))}
          </ol>
        </section>
      )}

      {conditions && (
        <section className="flex flex-col gap-2">
          <h3 className="type-card-title">Điều kiện tiếp nhận</h3>
          <ul className="flex flex-col gap-2">
            {conditions.conditions.map((c) => (
              <li key={c.code} className="flex flex-col">
                <span className="type-body-sm">{c.name ?? c.code}</span>
                <span className="type-caption text-muted-foreground">
                  {CONDITION_STATUS[c.status] ?? c.status}
                  {c.basis ? `, ${c.basis}` : ''}
                </span>
              </li>
            ))}
          </ul>
          {conditions.warnings?.length > 0 && (
            <ul className="flex flex-col gap-1" aria-label="Cảnh báo">
              {conditions.warnings.map((w) => (
                <li key={w.code} className="type-body-sm text-warning-foreground">
                  {w.description}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <section className="flex flex-col gap-2">
        <h3 className="type-card-title">Ý kiến về việc vào ở</h3>
        <p className="type-caption text-muted-foreground">Ghi nhận ý kiến của người cao tuổi, không phải điều kiện tiếp nhận.</p>
        <OpinionBlock opinions={opinions} />
        {open && canUpdate && !writing && (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" className="h-11" onClick={() => setWriting(true)}>
              Ghi ý kiến mới
            </Button>
          </div>
        )}
        {writing && (
          <OpinionForm
            applicationId={id}
            onCancel={() => setWriting(false)}
            onDone={(message) => {
              setWriting(false)
              setReload((n) => n + 1)
              onChanged(message)
            }}
          />
        )}
      </section>

      {open && canUpdate && (
        <div className="flex justify-end border-t pt-4">
          <Button className="h-11" onClick={() => onEdit(application)}>
            Cập nhật đăng ký
          </Button>
        </div>
      )}
    </div>
  )
}

// Lập mới (chưa có application) hoặc cập nhật (có application: tải lại để lấy ETag cho If-Match, lý do bắt buộc)
export function ApplicationSheet({ application, onClose, onDone }) {
  const editing = !!application
  const [etag, setEtag] = useState(null)
  const [residentId, setResidentId] = useState(application ? String(application.residentId) : '')
  const [applicationDate, setApplicationDate] = useState(application?.applicationDate ?? '')
  const [type, setType] = useState(application?.desiredResidenceType ?? '')
  const [level, setLevel] = useState(application?.expectedCareLevel ?? '')
  const [desiredDate, setDesiredDate] = useState(application?.desiredAdmissionDate ?? '')
  const [contact, setContact] = useState(application?.contactPerson ?? '')
  const [needs, setNeeds] = useState(application?.needs ?? '')
  const [note, setNote] = useState(application?.note ?? '')
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    if (!editing) return
    getAdmissionApplicationsById({ path: { id: application.id } })
      .then(({ etag: tag }) => setEtag(tag))
      .catch((e) => setFailure(problemOf(e)))
  }, [editing, application?.id])

  const errors = {
    resident: !residentId && 'Chọn người cao tuổi',
    date: !applicationDate && 'Nhập ngày đăng ký',
    type: !type && 'Chọn loại lưu trú',
    level: !level && 'Chọn mức chăm sóc',
    reason: editing && !reason.trim() && 'Nhập lý do',
  }

  async function save() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    const body = {
      residentId: Number(residentId),
      applicationDate,
      desiredResidenceType: type,
      expectedCareLevel: level,
      ...(desiredDate && { desiredAdmissionDate: desiredDate }),
      ...(contact.trim() && { contactPerson: contact.trim() }),
      ...(needs.trim() && { needs: needs.trim() }),
      ...(note.trim() && { note: note.trim() }),
    }
    try {
      if (editing) {
        await postAdmissionApplicationsByIdUpdate({
          path: { id: application.id },
          headers: { ...newKey(), 'If-Match': etag },
          body: { ...body, reason: reason.trim() },
        })
        onDone('Đã cập nhật đăng ký')
      } else {
        await postAdmissionApplications({ headers: newKey(), body })
        onDone('Đã lập đăng ký tiếp nhận')
      }
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  const shown = (key) => (submitted ? errors[key] : null)

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle>{editing ? 'Cập nhật đăng ký tiếp nhận' : 'Lập đăng ký tiếp nhận'}</SheetTitle>
        <SheetDescription>Người cao tuổi phải đang ở trạng thái Đang tiếp nhận; mỗi người một lượt đang mở.</SheetDescription>
        <FieldGroup className="mt-4">
          {editing ? (
            <Info label="Người cao tuổi">{`Mã ${application.residentId}`}</Info>
          ) : (
            <ResidentSelect
              id="application-resident"
              statuses={['ADMISSION_IN_PROGRESS']}
              value={residentId}
              onChange={setResidentId}
              error={shown('resident')}
              onFailure={(e) => setFailure(problemOf(e))}
            />
          )}
          <Field data-invalid={!!shown('date') || undefined}>
            <FieldLabel htmlFor="application-date">Ngày đăng ký</FieldLabel>
            <Input
              id="application-date"
              type="date"
              className="h-11"
              value={applicationDate}
              aria-invalid={!!shown('date') || undefined}
              onChange={(e) => setApplicationDate(e.target.value)}
            />
            {shown('date') && <FieldError>{shown('date')}</FieldError>}
          </Field>
          <Choices label="Loại lưu trú mong muốn" options={RESIDENCE_TYPES} value={type} onChange={setType} error={shown('type')} />
          <Choices label="Mức chăm sóc dự kiến" options={CARE_LEVELS} value={level} onChange={setLevel} error={shown('level')} />
          <Field>
            <FieldLabel htmlFor="application-desired">Ngày vào dự kiến</FieldLabel>
            <Input id="application-desired" type="date" className="h-11" value={desiredDate} onChange={(e) => setDesiredDate(e.target.value)} />
          </Field>
          <Field>
            <FieldLabel htmlFor="application-contact">Người liên hệ</FieldLabel>
            <Input id="application-contact" className="h-11" value={contact} onChange={(e) => setContact(e.target.value)} />
          </Field>
          <Field>
            <FieldLabel htmlFor="application-needs">Nhu cầu</FieldLabel>
            <Textarea id="application-needs" rows={2} value={needs} onChange={(e) => setNeeds(e.target.value)} />
          </Field>
          <Field>
            <FieldLabel htmlFor="application-note">Ghi chú</FieldLabel>
            <Textarea id="application-note" rows={2} value={note} onChange={(e) => setNote(e.target.value)} />
          </Field>
          {editing && <ReasonField id="application-reason" value={reason} onChange={setReason} error={shown('reason')} />}
        </FieldGroup>
        <div className="mt-4">
          <ProblemAlert problem={failure} />
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" className="h-11" onClick={onClose}>
            Đóng
          </Button>
          <Button className="h-11" disabled={busy || (editing && !etag)} onClick={save}>
            Lưu đăng ký
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
