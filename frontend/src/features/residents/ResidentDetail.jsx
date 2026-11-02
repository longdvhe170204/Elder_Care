import { useCallback, useEffect, useState } from 'react'
import { ApiError, getResidentsById, getResidentsByIdLinkedProfiles, postResidentsByIdUpdatePersonalInfo } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { AllergyNote, CareNote } from '@/common/CareNotes'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { loadFileUrl } from '@/common/files'
import { problemOf } from '@/common/problem'
import { AssessmentTab } from '../assessments/AssessmentTab'
import { CARE_LEVEL_LABELS, RESIDENCE_TYPE_LABELS } from '../assessments/assessmentLabels'
import { ConsentTab } from '../consents/ConsentTab'
import { StatusTab } from '../status/StatusTab'
import { WishTab } from '../wishes/WishTab'
import { HealthTab } from './HealthTab'
import { GENDER_LABELS, ID_DOCUMENT_LABELS, STATUS_LABELS } from './residentLabels'

// Hồ sơ trong khung chi tiết (001 T022; US1 #6 → #9; FR-006 → FR-009; DESIGN.md 5, 6.7.0; hợp đồng GET /residents/{id},
// POST /residents/{id}/update-personal-info, GET /residents/{id}/linked-profiles). Khung chỉ có trường của Profile:
// ảnh, họ tên, mã, trạng thái; an toàn (dị ứng trong activeHealthItems, careNote); thông tin cá nhân; nhu cầu chăm sóc.
// Phần dài (Sức khỏe, Đánh giá, Trạng thái, Đồng ý, Nguyện vọng) mở bằng Sheet 768px chồng lên khung. Trường nào người
// xem không được thấy thì backend bỏ khỏi phản hồi và liệt kê ở hiddenFields; nút sửa chỉ hiện khi allowedCommands có
// UPDATE_PERSONAL_INFO (002 FR-042). If-Match lấy từ ETag của lần xem gần nhất.

function blank(value) {
  const text = value.trim()
  return text === '' ? undefined : text
}

function formOf(info) {
  return {
    fullName: info.fullName ?? '',
    dateOfBirth: info.dateOfBirth ?? '',
    gender: info.gender ?? '',
    idDocumentType: info.idDocumentType ?? '',
    idDocumentNumber: info.idDocumentNumber ?? '',
    permanentAddress: info.permanentAddress ?? '',
    contactAddress: info.contactAddress ?? '',
    phoneNumber: info.contact?.phoneNumber ?? '',
    email: info.contact?.email ?? '',
    specialNotes: info.specialNotes ?? '',
    reason: '',
  }
}

function Row({ label, children }) {
  return (
    <div className="flex flex-col">
      <dt className="type-caption text-muted-foreground">{label}</dt>
      <dd className="type-body-sm">{children || 'Chưa có'}</dd>
    </div>
  )
}

function Section({ title, children }) {
  return (
    <section className="flex flex-col gap-2">
      <h3 className="type-caption text-muted-foreground">{title}</h3>
      {children}
    </section>
  )
}

// Ảnh nhận diện (DESIGN.md 7.2 resident-photo): tải qua backend kèm cookie phiên; chưa có ảnh thì chữ cái và ghi rõ
function ResidentPhoto({ id, name, photoFileId }) {
  const [url, setUrl] = useState(null)
  useEffect(() => {
    if (!photoFileId) return undefined
    let created = null
    let cancelled = false
    loadFileUrl(photoFileId)
      .then((u) => {
        created = u
        if (!cancelled) setUrl(u)
      })
      .catch(() => {})
    return () => {
      cancelled = true
      if (created) URL.revokeObjectURL(created)
    }
  }, [photoFileId])
  if (photoFileId && url) {
    return <img src={url} alt={`Ảnh ${name}`} className="size-16 shrink-0 rounded-full object-cover" />
  }
  return (
    <div className="flex shrink-0 flex-col items-center gap-1">
      <ResidentAvatar name={name} code={String(id)} className="size-16 type-title" />
      <span className="type-caption text-muted-foreground">Chưa có ảnh</span>
    </div>
  )
}

const PARTS = [
  { value: 'health', label: 'Sức khỏe' },
  { value: 'assessment', label: 'Đánh giá' },
  { value: 'status', label: 'Trạng thái' },
  { value: 'consent', label: 'Đồng ý' },
  { value: 'wishes', label: 'Nguyện vọng' },
]

export function ResidentDetail({ residentId, onChanged, Title = 'h2', Description = 'p' }) {
  const [profile, setProfile] = useState(null)
  const [etag, setEtag] = useState(null)
  const [linked, setLinked] = useState([])
  const [failure, setFailure] = useState(null)
  // Phần đang xem giữ lại sau khi đóng để tiêu đề không mất trong lúc Sheet đang đóng
  const [part, setPart] = useState(null)
  const [partOpen, setPartOpen] = useState(false)
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [key, setKey] = useState(() => crypto.randomUUID())

  const load = useCallback(async () => {
    try {
      const { data, etag: tag } = await getResidentsById({ path: { id: residentId } })
      setFailure(null)
      setProfile(data)
      setEtag(tag)
    } catch (error) {
      setFailure(problemOf(error))
      return
    }
    try {
      const { data } = await getResidentsByIdLinkedProfiles({ path: { id: residentId } })
      setLinked(data)
    } catch {
      setLinked([])
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp hồ sơ khi mở khung
    load()
  }, [load])

  const set = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))

  function startEditing() {
    setForm(formOf(profile.personalInfo))
    setErrors({})
    setFailure(null)
    setKey(crypto.randomUUID())
    setEditing(true)
  }

  function validate() {
    const next = {}
    if (!form.fullName.trim()) next.fullName = 'Nhập họ tên'
    if (!form.dateOfBirth) next.dateOfBirth = 'Chọn ngày sinh'
    else if (form.dateOfBirth > new Date().toISOString().slice(0, 10)) next.dateOfBirth = 'Ngày sinh không ở tương lai'
    if (!form.gender) next.gender = 'Chọn giới tính'
    if (form.idDocumentType && !form.idDocumentNumber.trim()) next.idDocumentNumber = 'Nhập số giấy tờ'
    if (!form.idDocumentType && form.idDocumentNumber.trim()) next.idDocumentType = 'Chọn loại giấy tờ'
    if (!form.reason.trim()) next.reason = 'Nhập lý do cập nhật'
    return next
  }

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const next = validate()
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setSaving(true)
    try {
      const phoneNumber = blank(form.phoneNumber)
      const email = blank(form.email)
      const { data, etag: tag } = await postResidentsByIdUpdatePersonalInfo({
        path: { id: residentId },
        headers: { 'If-Match': etag, 'Idempotency-Key': key },
        body: {
          fullName: form.fullName.trim(),
          dateOfBirth: form.dateOfBirth,
          gender: form.gender,
          idDocumentType: form.idDocumentType || undefined,
          idDocumentNumber: blank(form.idDocumentNumber),
          permanentAddress: blank(form.permanentAddress),
          contactAddress: blank(form.contactAddress),
          contact: phoneNumber || email ? { phoneNumber, email } : undefined,
          specialNotes: blank(form.specialNotes),
          reason: form.reason.trim(),
        },
      })
      setProfile(data)
      setEtag(tag)
      setEditing(false)
      onChanged?.(data)
    } catch (error) {
      const body = error instanceof ApiError ? error.problem : null
      if (body?.errorCode === 'CITIZEN_ID_IN_USE') {
        setErrors({ idDocumentNumber: `CCCD đang dùng ở hồ sơ ${body.profileCode ?? 'khác'}` })
      } else {
        setFailure(problemOf(error))
      }
    } finally {
      setSaving(false)
    }
  }

  const changed = () => {
    load()
    onChanged?.()
  }

  const info = profile?.personalInfo
  const canUpdate = profile?.allowedCommands?.includes('UPDATE_PERSONAL_INFO')
  const hidden = profile?.hiddenFields ?? []
  const allowedCommands = profile?.allowedCommands ?? []
  // Vai trò không được xem phần sức khỏe (backend bỏ activeHealthItems) thì không có phần Sức khỏe (US2; FR-008)
  const parts = PARTS.filter((p) => p.value !== 'health' || !hidden.includes('activeHealthItems'))
  const allergies = (profile?.activeHealthItems ?? [])
    .filter((item) => item.type === 'ALLERGY' && item.status !== 'EXCLUDED')
    .map((item) => item.allergen?.name ?? item.freeText)
  const partLabel = PARTS.find((p) => p.value === part)?.label

  const problem = (
    <ProblemAlert problem={failure}>
      {failure?.status === 412 && (
        <Button type="button" variant="outline" className="h-11 self-start" onClick={load}>
          Xem giá trị mới
        </Button>
      )}
    </ProblemAlert>
  )

  return (
    <div className="flex flex-col gap-5">
      {!profile && (
        <>
          <Title className="type-title">Hồ sơ người cao tuổi</Title>
          <Description className="type-body-sm text-muted-foreground">
            {failure ? 'Không mở được hồ sơ' : 'Đang tải hồ sơ'}
          </Description>
        </>
      )}
      {!editing && problem}

      {info && (
        <>
          <header className="flex items-center gap-3">
            <ResidentPhoto id={profile.id} name={info.fullName} photoFileId={info.photoFileId} />
            <div className="flex min-w-0 flex-col gap-1">
              <Title className="type-title">{info.fullName}</Title>
              <Description className="type-caption text-muted-foreground tabular-nums">{profile.profileCode}</Description>
              <span className="flex flex-wrap items-baseline gap-x-2">
                <StatusBadge status={STATUS_LABELS[profile.status] ?? profile.status} />
                {profile.statusSince && (
                  <span className="type-caption text-muted-foreground tabular-nums">
                    từ {formatDate(profile.statusSince)}
                  </span>
                )}
              </span>
            </div>
          </header>

          {!hidden.includes('activeHealthItems') && (
            <Section title="An toàn">
              {allergies.length === 0 && !profile.careNote ? (
                <p className="type-body-sm">Chưa ghi nhận</p>
              ) : (
                <>
                  <AllergyNote allergies={allergies} />
                  <CareNote note={profile.careNote} />
                </>
              )}
            </Section>
          )}

          <Section title="Thông tin cá nhân">
            <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
              <Row label="Ngày sinh">{formatDate(info.dateOfBirth)}</Row>
              <Row label="Giới tính">{GENDER_LABELS[info.gender]}</Row>
              <Row label="Giấy tờ">
                {info.idDocumentType && `${ID_DOCUMENT_LABELS[info.idDocumentType]} ${info.idDocumentNumber ?? ''}`}
              </Row>
              <Row label="Điện thoại">{info.contact?.phoneNumber}</Row>
              <Row label="Mức chăm sóc hiện hành">{CARE_LEVEL_LABELS[profile.currentCareLevel]}</Row>
              <Row label="Loại hình lưu trú">{RESIDENCE_TYPE_LABELS[profile.currentResidenceType]}</Row>
              <div className="col-span-2">
                <Row label="Địa chỉ thường trú">{info.permanentAddress}</Row>
              </div>
              <div className="col-span-2">
                <Row label="Địa chỉ liên hệ">{info.contactAddress}</Row>
              </div>
              <div className="col-span-2">
                <Row label="Thư điện tử">{info.contact?.email}</Row>
              </div>
              <div className="col-span-2">
                <Row label="Thông tin đặc biệt">{info.specialNotes}</Row>
              </div>
            </dl>
            {hidden.length > 0 && (
              <p className="type-caption text-muted-foreground">Một số thông tin không hiển thị theo quyền của bạn.</p>
            )}
          </Section>

          {profile.careNeeds && (
            <Section title="Nhu cầu chăm sóc">
              {profile.careNeeds.length > 0 ? (
                <ul className="flex flex-wrap gap-1.5">
                  {profile.careNeeds.map((need) => (
                    <li key={need} className="rounded-full bg-muted px-2.5 py-0.5 type-body-sm">
                      {need}
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="type-body-sm">Chưa ghi nhận</p>
              )}
            </Section>
          )}

          {linked.length > 0 && (
            <Section title="Hồ sơ trước của cùng người">
              <ul className="flex flex-col gap-1 type-body-sm">
                {linked.map((l) => (
                  <li key={l.id}>
                    {l.profileCode}
                    {l.viewable && l.status ? ` · ${STATUS_LABELS[l.status] ?? l.status}` : ' · không có quyền xem'}
                  </li>
                ))}
              </ul>
            </Section>
          )}

          <Section title="Các phần của hồ sơ">
            <div className="flex flex-wrap gap-2">
              {parts.map((p) => (
                <Button
                  key={p.value}
                  type="button"
                  variant="outline"
                  className="h-11"
                  aria-haspopup="dialog"
                  onClick={() => {
                    setPart(p.value)
                    setPartOpen(true)
                  }}
                >
                  {p.label}
                </Button>
              ))}
            </div>
          </Section>

          {canUpdate && (
            <Button type="button" className="h-11 self-start" aria-haspopup="dialog" onClick={startEditing}>
              Cập nhật thông tin
            </Button>
          )}

          <Sheet open={partOpen} onOpenChange={setPartOpen}>
            <SheetContent className="sm:max-w-3xl">
              <SheetHeader className="pt-6">
                <SheetTitle className="type-title">{`${partLabel}: ${info.fullName}`}</SheetTitle>
                <SheetDescription>{profile.profileCode}</SheetDescription>
              </SheetHeader>
              <div className="flex flex-col gap-4 overflow-y-auto px-4 pb-6">
                {part === 'health' && <HealthTab residentId={residentId} allowedCommands={allowedCommands} />}
                {part === 'assessment' && (
                  <AssessmentTab
                    residentId={residentId}
                    residentStatus={profile.status}
                    allowedCommands={allowedCommands}
                    onChanged={load}
                  />
                )}
                {part === 'status' && (
                  <StatusTab
                    residentId={residentId}
                    status={profile.status}
                    allowedCommands={allowedCommands}
                    onChanged={changed}
                  />
                )}
                {part === 'consent' && (
                  <ConsentTab
                    residentId={residentId}
                    status={profile.status}
                    allowedCommands={allowedCommands}
                    onChanged={changed}
                  />
                )}
                {part === 'wishes' && (
                  <WishTab
                    residentId={residentId}
                    status={profile.status}
                    allowedCommands={allowedCommands}
                    profile={profile}
                    onChanged={changed}
                  />
                )}
              </div>
            </SheetContent>
          </Sheet>

          <Sheet open={editing} onOpenChange={(open) => !open && setEditing(false)}>
            <SheetContent className="sm:max-w-lg">
              <SheetHeader className="pt-6">
                <SheetTitle className="type-title">{`Cập nhật thông tin: ${info.fullName}`}</SheetTitle>
                <SheetDescription>{profile.profileCode}</SheetDescription>
              </SheetHeader>
              {editing && (
                <form onSubmit={submit} noValidate className="flex flex-col gap-4 overflow-y-auto px-4 pb-6">
                  {problem}
                  <FieldGroup>
                    <Field data-invalid={!!errors.fullName || undefined}>
                      <FieldLabel htmlFor="edit-name">Họ tên</FieldLabel>
                      <Input id="edit-name" value={form.fullName} onChange={set('fullName')} className="h-10" />
                      {errors.fullName && <FieldError>{errors.fullName}</FieldError>}
                    </Field>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field data-invalid={!!errors.dateOfBirth || undefined}>
                        <FieldLabel htmlFor="edit-dob">Ngày sinh</FieldLabel>
                        <Input
                          id="edit-dob"
                          type="date"
                          value={form.dateOfBirth}
                          onChange={set('dateOfBirth')}
                          className="h-10"
                        />
                        {errors.dateOfBirth && <FieldError>{errors.dateOfBirth}</FieldError>}
                      </Field>
                      <Field data-invalid={!!errors.gender || undefined}>
                        <FieldLabel htmlFor="edit-gender">Giới tính</FieldLabel>
                        <select
                          id="edit-gender"
                          value={form.gender}
                          onChange={set('gender')}
                          className="h-10 w-full rounded-lg border border-input bg-transparent px-3 text-base"
                        >
                          <option value="">Chọn</option>
                          {Object.entries(GENDER_LABELS).map(([code, label]) => (
                            <option key={code} value={code}>
                              {label}
                            </option>
                          ))}
                        </select>
                        {errors.gender && <FieldError>{errors.gender}</FieldError>}
                      </Field>
                    </div>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field data-invalid={!!errors.idDocumentType || undefined}>
                        <FieldLabel htmlFor="edit-doc-type">Loại giấy tờ</FieldLabel>
                        <select
                          id="edit-doc-type"
                          value={form.idDocumentType}
                          onChange={set('idDocumentType')}
                          className="h-10 w-full rounded-lg border border-input bg-transparent px-3 text-base"
                        >
                          <option value="">Không có</option>
                          {Object.entries(ID_DOCUMENT_LABELS).map(([code, label]) => (
                            <option key={code} value={code}>
                              {label}
                            </option>
                          ))}
                        </select>
                        {errors.idDocumentType && <FieldError>{errors.idDocumentType}</FieldError>}
                      </Field>
                      <Field data-invalid={!!errors.idDocumentNumber || undefined}>
                        <FieldLabel htmlFor="edit-doc-number">Số giấy tờ</FieldLabel>
                        <Input
                          id="edit-doc-number"
                          value={form.idDocumentNumber}
                          onChange={set('idDocumentNumber')}
                          className="h-10 tabular-nums"
                        />
                        {errors.idDocumentNumber && <FieldError>{errors.idDocumentNumber}</FieldError>}
                      </Field>
                    </div>
                    <Field>
                      <FieldLabel htmlFor="edit-permanent">Địa chỉ thường trú</FieldLabel>
                      <Input
                        id="edit-permanent"
                        value={form.permanentAddress}
                        onChange={set('permanentAddress')}
                        className="h-10"
                      />
                    </Field>
                    <Field>
                      <FieldLabel htmlFor="edit-contact-address">Địa chỉ liên hệ</FieldLabel>
                      <Input
                        id="edit-contact-address"
                        value={form.contactAddress}
                        onChange={set('contactAddress')}
                        className="h-10"
                      />
                    </Field>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field>
                        <FieldLabel htmlFor="edit-phone">Số điện thoại</FieldLabel>
                        <Input
                          id="edit-phone"
                          inputMode="tel"
                          value={form.phoneNumber}
                          onChange={set('phoneNumber')}
                          className="h-10"
                        />
                      </Field>
                      <Field>
                        <FieldLabel htmlFor="edit-email">Thư điện tử</FieldLabel>
                        <Input id="edit-email" type="email" value={form.email} onChange={set('email')} className="h-10" />
                      </Field>
                    </div>
                    <Field>
                      <FieldLabel htmlFor="edit-notes">Thông tin đặc biệt</FieldLabel>
                      <Textarea id="edit-notes" value={form.specialNotes} onChange={set('specialNotes')} />
                    </Field>
                    <Field data-invalid={!!errors.reason || undefined}>
                      <FieldLabel htmlFor="edit-reason">Lý do cập nhật</FieldLabel>
                      <Textarea id="edit-reason" value={form.reason} onChange={set('reason')} />
                      {errors.reason && <FieldError>{errors.reason}</FieldError>}
                    </Field>
                  </FieldGroup>
                  <div className="flex flex-wrap gap-3">
                    <Button type="submit" className="h-11" disabled={saving}>
                      Lưu thay đổi
                    </Button>
                    <Button type="button" variant="outline" className="h-11" onClick={() => setEditing(false)}>
                      Hủy
                    </Button>
                  </div>
                </form>
              )}
            </SheetContent>
          </Sheet>
        </>
      )}
    </div>
  )
}
