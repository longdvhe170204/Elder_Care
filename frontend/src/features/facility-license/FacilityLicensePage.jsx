import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { changeContact, loadExaminations, loadFacility, recordExamination, saveLicense } from './facility'

// Màn Giấy phép và khám (spec 007 User Story 9, FR-070 → FR-075; T057 phần web): giấy phép cơ sở và lịch sử (Quản
// lý viện, Bác sĩ xem; Quản lý viện lưu bản mới thay bản cũ), số liên hệ của viện (đổi gửi If-Match), bản ghi khám và
// điều trị của một người cao tuổi (FR-074). Hợp đồng không trả lệnh được phép nên nút theo vai trò, backend kiểm lại.
// Tải bản scan giấy phép chưa có ở màn này (cần lệnh tải tệp của 000).

const EXAM_TYPE = {
  EXAMINATION: 'Khám',
  ASSESSMENT: 'Đánh giá',
  DIAGNOSIS: 'Chẩn đoán',
  ORDER: 'Chỉ định',
  TREATMENT: 'Điều trị',
  RESULT: 'Kết quả',
}
const EXAM_ROLES = ['DOCTOR', 'NURSING_STAFF']

function LicenseSheet({ current, onClose, onSaved }) {
  const [form, setForm] = useState({ number: '', issuer: '', issuedOn: '', expiresOn: '', scopes: '', reason: '' })
  const [covers, setCovers] = useState(false)
  const [dueToError, setDueToError] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))
  const errors = {
    number: !form.number.trim(),
    issuer: !form.issuer.trim(),
    issuedOn: !form.issuedOn,
    expiresOn: !form.expiresOn,
    reason: dueToError && !form.reason.trim(),
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setProblem(null)
    try {
      await saveLicense({
        number: form.number.trim(),
        issuer: form.issuer.trim(),
        issuedOn: form.issuedOn,
        expiresOn: form.expiresOn,
        coversMedicalPractice: covers,
        scopes: form.scopes
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
        ...(current ? { replaces: current.id } : {}),
        ...(dueToError ? { replacesDueToError: true, reason: form.reason.trim() } : {}),
      })
      onSaved('Đã lưu giấy phép mới')
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const field = (id, label, key, type = 'text', error) => (
    <Field data-invalid={(submitted && errors[key]) || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        type={type}
        className="h-11"
        value={form[key]}
        onChange={set(key)}
        aria-invalid={(submitted && errors[key]) || undefined}
      />
      {submitted && errors[key] && <FieldError>{error}</FieldError>}
    </Field>
  )

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetHeader className="px-0">
          <SheetTitle className="type-title">Lưu giấy phép mới</SheetTitle>
          <SheetDescription>
            Gia hạn, đổi phạm vi hoặc sửa sai: bản mới thay bản đang hiệu lực, bản cũ giữ trong lịch sử.
          </SheetDescription>
        </SheetHeader>
        <form onSubmit={submit} noValidate className="flex flex-col gap-4">
          {field('license-number', 'Số giấy phép', 'number', 'text', 'Nhập số giấy phép')}
          {field('license-issuer', 'Nơi cấp', 'issuer', 'text', 'Nhập nơi cấp')}
          {field('license-issued', 'Ngày cấp', 'issuedOn', 'date', 'Chọn ngày cấp')}
          {field('license-expires', 'Ngày hết hạn', 'expiresOn', 'date', 'Chọn ngày hết hạn')}
          <Button
            type="button"
            variant={covers ? 'default' : 'outline'}
            aria-pressed={covers}
            className="h-11 self-start"
            onClick={() => setCovers((v) => !v)}
          >
            Có phạm vi khám bệnh, chữa bệnh
          </Button>
          <Field>
            <FieldLabel htmlFor="license-scopes">Phạm vi (cách nhau bằng dấu phẩy)</FieldLabel>
            <Input id="license-scopes" className="h-11" value={form.scopes} onChange={set('scopes')} />
          </Field>
          {current && (
            <Button
              type="button"
              variant={dueToError ? 'default' : 'outline'}
              aria-pressed={dueToError}
              className="h-11 self-start"
              onClick={() => setDueToError((v) => !v)}
            >
              Sửa sai bản {current.number}
            </Button>
          )}
          {dueToError && (
            <Field data-invalid={(submitted && errors.reason) || undefined}>
              <FieldLabel htmlFor="license-reason">Lý do sửa sai</FieldLabel>
              <Textarea
                id="license-reason"
                rows={2}
                maxLength={2000}
                value={form.reason}
                onChange={set('reason')}
                aria-invalid={(submitted && errors.reason) || undefined}
              />
              {submitted && errors.reason && <FieldError>Nhập lý do sửa sai</FieldError>}
            </Field>
          )}
          {problem && <ProblemAlert problem={problem} />}
          <Button type="submit" className="h-11" disabled={busy}>
            Lưu giấy phép
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

function ContactCard({ contact, canEdit, onChanged }) {
  const [editing, setEditing] = useState(false)
  const [phone, setPhone] = useState(contact?.phoneNumber ?? '')
  const [note, setNote] = useState(contact?.note ?? '')
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!phone.trim()) return
    setBusy(true)
    setProblem(null)
    try {
      await changeContact(contact?.version ?? 0, {
        phoneNumber: phone.trim(),
        ...(note.trim() ? { note: note.trim() } : {}),
      })
      setEditing(false)
      onChanged('Đã đổi số liên hệ của viện')
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card role="region" aria-labelledby="contact-title">
      <CardHeader className="flex flex-row items-center justify-between gap-3">
        <h2 id="contact-title" className="type-title">
          Số liên hệ của viện
        </h2>
        {canEdit && !editing && (
          <Button variant="outline" onClick={() => setEditing(true)}>
            Đổi số liên hệ
          </Button>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {!editing ? (
          <>
            <span className="type-card-title tabular-nums">{contact?.phoneNumber ?? 'Chưa ghi số liên hệ'}</span>
            {contact?.note && <span className="type-body-sm text-muted-foreground">{contact.note}</span>}
          </>
        ) : (
          <form onSubmit={submit} noValidate className="flex flex-col gap-3">
            <Field data-invalid={(submitted && !phone.trim()) || undefined}>
              <FieldLabel htmlFor="contact-phone">Số điện thoại</FieldLabel>
              <Input
                id="contact-phone"
                type="tel"
                className="h-11"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
              {submitted && !phone.trim() && <FieldError>Nhập số điện thoại</FieldError>}
            </Field>
            <Field>
              <FieldLabel htmlFor="contact-note">Ghi chú</FieldLabel>
              <Input id="contact-note" className="h-11" value={note} onChange={(e) => setNote(e.target.value)} />
            </Field>
            {problem && <ProblemAlert problem={problem} />}
            <div className="flex flex-wrap gap-2">
              <Button type="submit" className="h-11" disabled={busy}>
                Lưu số liên hệ
              </Button>
              <Button type="button" variant="ghost" className="h-11" onClick={() => setEditing(false)}>
                Quay lại
              </Button>
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  )
}

function ExaminationForm({ resident, onSaved, onCancel }) {
  const [type, setType] = useState('')
  const [content, setContent] = useState('')
  const [atFacility, setAtFacility] = useState(true)
  const [facilityName, setFacilityName] = useState('')
  const [performer, setPerformer] = useState('')
  const [performedAt, setPerformedAt] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const errors = { type: !type, content: !content.trim(), performedAt: !performedAt }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setProblem(null)
    try {
      await recordExamination(resident.id, {
        type,
        content: content.trim(),
        atFacility,
        ...(!atFacility && facilityName.trim() ? { externalFacilityName: facilityName.trim() } : {}),
        ...(!atFacility && performer.trim() ? { externalPerformerName: performer.trim() } : {}),
        performedAt: `${performedAt}:00+07:00`,
      })
      onSaved('Đã ghi bản ghi khám, điều trị')
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-3 rounded-xl border p-3">
      <Field data-invalid={(submitted && errors.type) || undefined}>
        <FieldLabel>Loại</FieldLabel>
        <ToggleGroup
          variant="outline"
          value={type ? [type] : []}
          onValueChange={(n) => setType(n[0] ?? '')}
          className="flex flex-wrap"
        >
          {Object.entries(EXAM_TYPE).map(([value, label]) => (
            <ToggleGroupItem key={value} value={value} className="h-11 px-4">
              {label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        {submitted && errors.type && <FieldError>Chọn loại</FieldError>}
      </Field>
      <Field data-invalid={(submitted && errors.content) || undefined}>
        <FieldLabel htmlFor="exam-content">Nội dung</FieldLabel>
        <Textarea
          id="exam-content"
          rows={3}
          maxLength={2000}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          aria-invalid={(submitted && errors.content) || undefined}
        />
        {submitted && errors.content && <FieldError>Nhập nội dung</FieldError>}
      </Field>
      <ToggleGroup
        variant="outline"
        value={[atFacility ? 'in' : 'out']}
        onValueChange={(n) => n[0] && setAtFacility(n[0] === 'in')}
        className="flex flex-wrap"
        aria-label="Nơi thực hiện"
      >
        <ToggleGroupItem value="in" className="h-11 px-4">
          Tại viện
        </ToggleGroupItem>
        <ToggleGroupItem value="out" className="h-11 px-4">
          Bên ngoài
        </ToggleGroupItem>
      </ToggleGroup>
      {!atFacility && (
        <>
          <Field>
            <FieldLabel htmlFor="exam-facility">Cơ sở bên ngoài</FieldLabel>
            <Input
              id="exam-facility"
              className="h-11"
              value={facilityName}
              onChange={(e) => setFacilityName(e.target.value)}
            />
          </Field>
          <Field>
            <FieldLabel htmlFor="exam-performer">Người thực hiện</FieldLabel>
            <Input
              id="exam-performer"
              className="h-11"
              value={performer}
              onChange={(e) => setPerformer(e.target.value)}
            />
          </Field>
        </>
      )}
      <Field data-invalid={(submitted && errors.performedAt) || undefined}>
        <FieldLabel htmlFor="exam-at">Thời điểm</FieldLabel>
        <Input
          id="exam-at"
          type="datetime-local"
          className="h-11"
          value={performedAt}
          onChange={(e) => setPerformedAt(e.target.value)}
        />
        {submitted && errors.performedAt && <FieldError>Chọn thời điểm</FieldError>}
      </Field>
      {problem && <ProblemAlert problem={problem} />}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" className="h-11" disabled={busy}>
          Lưu bản ghi
        </Button>
        <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
          Quay lại
        </Button>
      </div>
    </form>
  )
}

function ExaminationsCard({ resident, canRecord, onMessage }) {
  const [records, setRecords] = useState(null)
  const [problem, setProblem] = useState(null)
  const [adding, setAdding] = useState(false)

  useEffect(() => {
    let cancelled = false
    loadExaminations(resident.id).then(
      (data) => !cancelled && setRecords(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [resident.id])

  async function saved(message) {
    setAdding(false)
    onMessage(message)
    try {
      setRecords(await loadExaminations(resident.id))
    } catch (error) {
      setProblem(problemOf(error))
    }
  }

  return (
    <Card role="region" aria-labelledby="exam-title">
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-3">
        <div className="flex flex-col">
          <h2 id="exam-title" className="type-title">
            Khám và điều trị
          </h2>
          <span className="type-body-sm text-muted-foreground">{resident.name}</span>
        </div>
        {canRecord && !adding && (
          <Button variant="outline" onClick={() => setAdding(true)}>
            <Plus aria-hidden="true" />
            Ghi khám, điều trị
          </Button>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {problem && <ProblemAlert problem={problem} />}
        {adding && <ExaminationForm resident={resident} onSaved={saved} onCancel={() => setAdding(false)} />}
        {!records ? (
          !problem && <Skeleton className="h-20 w-full" />
        ) : records.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa có bản ghi khám, điều trị.</p>
        ) : (
          <ol className="flex flex-col gap-2 border-l-2 pl-3 type-body-sm">
            {records.map((r) => (
              <li key={r.id} className="flex flex-col">
                <span className="tabular-nums text-muted-foreground">
                  {formatDateTime(r.performedAt)}, {EXAM_TYPE[r.type] ?? r.type},{' '}
                  {r.atFacility
                    ? 'tại viện'
                    : `bên ngoài${r.externalFacilityName ? `: ${r.externalFacilityName}` : ''}`}
                </span>
                <span>{r.content}</span>
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  )
}

export function FacilityLicensePage({ role }) {
  const isManager = role === 'NURSING_HOME_MANAGER'
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [adding, setAdding] = useState(false)
  const [resident, setResident] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  useEffect(() => {
    let cancelled = false
    loadFacility().then(
      (result) => !cancelled && setData(result),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [])

  async function reload(message) {
    setAnnouncement(`${message}.`)
    try {
      setData(await loadFacility())
    } catch (error) {
      setProblem(problemOf(error))
    }
  }

  const licenses = data?.licenses ?? []
  const current = licenses.find((l) => l.active)

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-5">
      <PageHeader
        title="Giấy phép và khám"
        subtitle="Giấy phép cơ sở, số liên hệ của viện, bản ghi khám và điều trị."
      />
      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {announcement}
      </p>
      {problem && <ProblemAlert problem={problem} />}
      {!data ? (
        <Skeleton className="h-40 w-full" />
      ) : (
        <div className="grid items-start gap-5 xl:grid-cols-2">
          <div className="flex flex-col gap-5">
            <Card role="region" aria-labelledby="license-title">
              <CardHeader className="flex flex-row items-center justify-between gap-3">
                <h2 id="license-title" className="type-title">
                  Giấy phép cơ sở
                </h2>
                {isManager && (
                  <Button variant="outline" onClick={() => setAdding(true)}>
                    <Plus aria-hidden="true" />
                    Lưu giấy phép mới
                  </Button>
                )}
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                {data.licensesError && <ProblemAlert problem={problemOf(data.licensesError)} />}
                {data.licenses && licenses.length === 0 && (
                  <p className="type-body-sm text-muted-foreground">Chưa lưu giấy phép cơ sở.</p>
                )}
                {licenses.map((l) => (
                  <div key={l.id} className="flex flex-col gap-1 border-b pb-3 last:border-b-0 last:pb-0">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="type-card-title">{l.number}</span>
                      <Badge variant={l.active ? 'settled' : 'neutral'}>
                        {l.active ? 'Đang hiệu lực' : 'Đã thay thế'}
                      </Badge>
                    </div>
                    <span className="type-body-sm">
                      {l.issuer}, cấp {formatDate(l.issuedOn)}, hết hạn {formatDate(l.expiresOn)}
                    </span>
                    <span className="type-body-sm text-muted-foreground">
                      {l.coversMedicalPractice
                        ? 'Có phạm vi khám bệnh, chữa bệnh'
                        : 'Không có phạm vi khám bệnh, chữa bệnh'}
                      {(l.scopes ?? []).length > 0 && `: ${l.scopes.join(', ')}`}
                    </span>
                    {l.replacesDueToError && (
                      <span className="type-body-sm text-muted-foreground">
                        Sửa sai bản trước{l.reason && `: ${l.reason}`}
                      </span>
                    )}
                  </div>
                ))}
              </CardContent>
            </Card>
            <ContactCard
              key={data.contact?.version ?? 'none'}
              contact={data.contact}
              canEdit={isManager}
              onChanged={reload}
            />
          </div>
          <div className="flex flex-col gap-5">
            <Card>
              <CardHeader>
                <h2 className="type-title">Người cao tuổi</h2>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-2">
                {data.residents.length === 0 && (
                  <p className="type-body-sm text-muted-foreground">Chưa đọc được danh sách người cao tuổi.</p>
                )}
                {data.residents.map((r) => (
                  <Button
                    key={r.id}
                    variant={resident?.id === r.id ? 'default' : 'outline'}
                    aria-pressed={resident?.id === r.id}
                    aria-label={`${r.name}, ${r.code}`}
                    className="h-11 gap-2"
                    onClick={() => setResident(r)}
                  >
                    <ResidentAvatar name={r.name} code={String(r.id)} className="size-6" />
                    <span>{r.name}</span>
                    <span className="type-caption">{r.code}</span>
                  </Button>
                ))}
              </CardContent>
            </Card>
            {resident && (
              <ExaminationsCard
                key={resident.id}
                resident={resident}
                canRecord={EXAM_ROLES.includes(role)}
                onMessage={(m) => setAnnouncement(`${m}: ${resident.name}.`)}
              />
            )}
          </div>
        </div>
      )}
      {adding && (
        <LicenseSheet
          current={current}
          onClose={() => setAdding(false)}
          onSaved={async (message) => {
            setAdding(false)
            await reload(message)
          }}
        />
      )}
    </div>
  )
}
