import { useCallback, useEffect, useState } from 'react'
import { ApiError, getResidentsById, getResidentsByIdLinkedProfiles, postResidentsByIdUpdatePersonalInfo } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { AssessmentTab } from '../assessments/AssessmentTab'
import { ConsentTab } from '../consents/ConsentTab'
import { StatusTab } from '../status/StatusTab'
import { HealthTab } from './HealthTab'
import { GENDER_LABELS, ID_DOCUMENT_LABELS, STATUS_LABELS } from './residentLabels'

// Chi tiết hồ sơ thật và cập nhật thông tin cá nhân (001 T022; US1 #6 → #9; FR-006, FR-007; hợp đồng
// GET /residents/{id}, POST /residents/{id}/update-personal-info, GET /residents/{id}/linked-profiles).
// Trường nào người xem không được thấy thì backend bỏ khỏi phản hồi và liệt kê ở hiddenFields; nút sửa chỉ hiện
// khi allowedCommands có UPDATE_PERSONAL_INFO (002 FR-042). If-Match lấy từ ETag của lần xem gần nhất.

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
      <dd className="type-body-sm font-medium">{children || 'Chưa có'}</dd>
    </div>
  )
}

export function ResidentDetail({ residentId, onChanged, onClose, onTabChange }) {
  const [profile, setProfile] = useState(null)
  const [etag, setEtag] = useState(null)
  const [linked, setLinked] = useState([])
  const [failure, setFailure] = useState(null)
  const [editing, setEditing] = useState(false)
  const [tab, setTab] = useState('info')
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

  const info = profile?.personalInfo
  const canUpdate = profile?.allowedCommands?.includes('UPDATE_PERSONAL_INFO')
  const hidden = profile?.hiddenFields ?? []
  // Vai trò không được xem phần sức khỏe (backend bỏ activeHealthItems) thì không có tab Sức khỏe (US2; FR-008)
  const showHealthTab = !!profile && !hidden.includes('activeHealthItems')

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{info ? info.fullName : 'Hồ sơ người cao tuổi'}</SheetTitle>
        <SheetDescription>
          {profile ? `${profile.profileCode} · ${STATUS_LABELS[profile.status] ?? profile.status}` : 'Đang tải hồ sơ'}
        </SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-4 overflow-y-auto px-4 pb-6">
        <ProblemAlert problem={failure}>
          {failure?.status === 412 && (
            <Button type="button" variant="outline" className="h-11 self-start" onClick={load}>
              Xem giá trị mới
            </Button>
          )}
        </ProblemAlert>

        {!!profile && !editing && (
          <ToggleGroup
            variant="outline"
            value={[tab]}
            onValueChange={(next) => {
              if (!next[0]) return
              setTab(next[0])
              onTabChange?.(next[0])
            }}
            aria-label="Phần của hồ sơ"
            className="self-start"
          >
            <ToggleGroupItem value="info" className="h-10 px-4">
              Thông tin
            </ToggleGroupItem>
            {showHealthTab && (
              <ToggleGroupItem value="health" className="h-10 px-4">
                Sức khỏe
              </ToggleGroupItem>
            )}
            <ToggleGroupItem value="assessment" className="h-10 px-4">
              Đánh giá
            </ToggleGroupItem>
            <ToggleGroupItem value="status" className="h-10 px-4">
              Trạng thái
            </ToggleGroupItem>
            <ToggleGroupItem value="consent" className="h-10 px-4">
              Đồng ý
            </ToggleGroupItem>
          </ToggleGroup>
        )}

        {info && tab === 'health' && showHealthTab && (
          <HealthTab residentId={residentId} allowedCommands={profile.allowedCommands ?? []} />
        )}

        {info && tab === 'assessment' && (
          <AssessmentTab
            residentId={residentId}
            residentStatus={profile.status}
            allowedCommands={profile.allowedCommands ?? []}
            onChanged={load}
          />
        )}

        {info && tab === 'status' && (
          <StatusTab
            residentId={residentId}
            status={profile.status}
            allowedCommands={profile.allowedCommands ?? []}
            onChanged={() => {
              load()
              onChanged?.()
            }}
          />
        )}

        {info && tab === 'consent' && (
          <ConsentTab
            residentId={residentId}
            status={profile.status}
            allowedCommands={profile.allowedCommands ?? []}
            onChanged={() => {
              load()
              onChanged?.()
            }}
          />
        )}

        {info && !editing && tab === 'info' && (
          <>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
              <Row label="Ngày sinh">{info.dateOfBirth}</Row>
              <Row label="Giới tính">{GENDER_LABELS[info.gender]}</Row>
              <Row label="Giấy tờ">
                {info.idDocumentType && `${ID_DOCUMENT_LABELS[info.idDocumentType]} ${info.idDocumentNumber ?? ''}`}
              </Row>
              <Row label="Mức chăm sóc hiện hành">{profile.currentCareLevel}</Row>
              <Row label="Địa chỉ thường trú">{info.permanentAddress}</Row>
              <Row label="Địa chỉ liên hệ">{info.contactAddress}</Row>
              <Row label="Điện thoại">{info.contact?.phoneNumber}</Row>
              <Row label="Thư điện tử">{info.contact?.email}</Row>
              <div className="col-span-2">
                <Row label="Thông tin đặc biệt">{info.specialNotes}</Row>
              </div>
            </dl>
            {hidden.length > 0 && (
              <p className="type-caption text-muted-foreground">Một số thông tin không hiển thị theo quyền của bạn.</p>
            )}
            {linked.length > 0 && (
              <section className="flex flex-col gap-1">
                <h3 className="type-caption text-muted-foreground">Hồ sơ trước của cùng người</h3>
                <ul className="flex flex-col gap-1 type-body-sm">
                  {linked.map((l) => (
                    <li key={l.id}>
                      {l.profileCode}
                      {l.viewable && l.status ? ` · ${STATUS_LABELS[l.status] ?? l.status}` : ' · không có quyền xem'}
                    </li>
                  ))}
                </ul>
              </section>
            )}
            <div className="flex flex-wrap gap-3">
              {canUpdate && (
                <Button type="button" className="h-11" onClick={startEditing}>
                  Cập nhật thông tin
                </Button>
              )}
              <Button type="button" variant="outline" className="h-11" onClick={onClose}>
                Đóng
              </Button>
            </div>
          </>
        )}

        {info && editing && (
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
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
      </div>
    </>
  )
}
