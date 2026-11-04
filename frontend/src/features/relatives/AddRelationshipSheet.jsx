import { useState } from 'react'
import { getFamilyMembers, postFamilyMembers, postResidentsByResidentIdRelationships } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { CONFLICT_HINTS, RELATIONSHIP_LABELS } from './relationshipLabels'

// Thêm người thân cho một người cao tuổi (012 T013; FR-001, FR-002, FR-005, FR-008): tìm hồ sơ đã có theo họ tên, số
// điện thoại, số giấy tờ (một người thân nhiều quan hệ, FR-002) hoặc lập hồ sơ mới, rồi lập quan hệ. Mọi quyền tắt khi
// lập; liên hệ chính tự nhận thông báo khẩn, người đại diện tự được yêu cầu thay đổi dịch vụ (FR-009, FR-010).

const EMPTY_MEMBER = { fullName: '', phoneNumber: '', idDocumentType: '', idDocumentNumber: '', address: '' }

function MemberFields({ member, onChange, submitted }) {
  const set = (name) => (e) => onChange({ ...member, [name]: e.target.value })
  return (
    <div className="flex flex-col gap-3">
      <Field data-invalid={(submitted && !member.fullName.trim()) || undefined}>
        <FieldLabel htmlFor="member-name">Họ tên</FieldLabel>
        <Input
          id="member-name"
          autoComplete="off"
          className="h-11"
          value={member.fullName}
          onChange={set('fullName')}
        />
        {submitted && !member.fullName.trim() && <FieldError>Nhập họ tên</FieldError>}
      </Field>
      <Field data-invalid={(submitted && !member.phoneNumber.trim()) || undefined}>
        <FieldLabel htmlFor="member-phone">Số điện thoại</FieldLabel>
        <Input
          id="member-phone"
          inputMode="tel"
          autoComplete="off"
          className="h-11"
          value={member.phoneNumber}
          onChange={set('phoneNumber')}
        />
        {submitted && !member.phoneNumber.trim() && <FieldError>Nhập số điện thoại</FieldError>}
      </Field>
      <Field>
        <FieldLabel htmlFor="member-document-type">Loại giấy tờ tùy thân (không bắt buộc)</FieldLabel>
        <Input
          id="member-document-type"
          className="h-11"
          value={member.idDocumentType}
          onChange={set('idDocumentType')}
        />
      </Field>
      <Field>
        <FieldLabel htmlFor="member-document-number">
          Số giấy tờ tùy thân (không bắt buộc; cần khi được phép đón)
        </FieldLabel>
        <Input
          id="member-document-number"
          className="h-11"
          value={member.idDocumentNumber}
          onChange={set('idDocumentNumber')}
        />
      </Field>
      <Field>
        <FieldLabel htmlFor="member-address">Địa chỉ (không bắt buộc)</FieldLabel>
        <Input id="member-address" className="h-11" value={member.address} onChange={set('address')} />
      </Field>
    </div>
  )
}

export function AddRelationshipSheet({
  open,
  residentId,
  residentName,
  hasPrimaryContact,
  hasRepresentative,
  onClose,
  onDone,
}) {
  const [keys] = useState(() => ({ member: crypto.randomUUID(), relationship: crypto.randomUUID() }))
  const [q, setQ] = useState('')
  const [found, setFound] = useState(null)
  const [chosen, setChosen] = useState(null)
  const [creating, setCreating] = useState(false)
  const [member, setMember] = useState(EMPTY_MEMBER)
  const [relationship, setRelationship] = useState('')
  const [primaryContact, setPrimaryContact] = useState(false)
  const [firstRepresentative, setFirstRepresentative] = useState(false)
  const [evidence, setEvidence] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  async function search(event) {
    event.preventDefault()
    try {
      const { data } = await getFamilyMembers({ query: { q: q.trim() } })
      setFound(data)
      setFailure(null)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  const memberMissing = creating ? !member.fullName.trim() || !member.phoneNumber.trim() : !chosen
  const representativeMissing = firstRepresentative && (!evidence || !reason.trim())

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (memberMissing || !relationship || representativeMissing) return
    setBusy(true)
    try {
      let familyMemberId = chosen?.id
      if (creating) {
        const { data } = await postFamilyMembers({ headers: { 'Idempotency-Key': keys.member }, body: member })
        familyMemberId = data.id
      }
      const body = { familyMemberId, relationship, primaryContact, setFirstRepresentative: firstRepresentative }
      if (firstRepresentative) {
        body.evidenceFileId = await uploadFile(evidence)
        body.reason = reason.trim()
      }
      await postResidentsByResidentIdRelationships({
        path: { residentId },
        headers: { 'Idempotency-Key': keys.relationship },
        body,
      })
      onDone('Đã thêm người thân')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open={open} onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle className="type-title">Thêm người thân</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Người thân của {residentName}. Mọi quyền tắt khi lập; bật quyền qua phiếu đăng ký hoặc yêu cầu thay đổi quyền.
        </SheetDescription>

        <form onSubmit={search} className="mt-4 flex flex-col gap-2" role="search">
          <Field>
            <FieldLabel htmlFor="member-search">
              Tìm hồ sơ người thân đã có (họ tên, số điện thoại, số giấy tờ)
            </FieldLabel>
            <div className="flex gap-2">
              <Input id="member-search" className="h-11" value={q} onChange={(e) => setQ(e.target.value)} />
              <Button type="submit" variant="outline" className="h-11">
                Tìm
              </Button>
            </div>
          </Field>
          {found && (
            <ul className="flex flex-col gap-1" aria-label="Hồ sơ người thân tìm thấy">
              {found.length === 0 && <li className="type-body-sm text-muted-foreground">Không có hồ sơ khớp.</li>}
              {found.map((m) => (
                <li key={m.id}>
                  <Button
                    type="button"
                    variant={chosen?.id === m.id ? 'default' : 'ghost'}
                    className="h-11 w-full justify-start"
                    aria-pressed={chosen?.id === m.id}
                    onClick={() => {
                      setChosen(m)
                      setCreating(false)
                    }}
                  >
                    {m.fullName}, {m.phoneNumber}
                  </Button>
                </li>
              ))}
            </ul>
          )}
          <Button
            type="button"
            variant="ghost"
            className="h-11 self-start"
            aria-pressed={creating}
            onClick={() => {
              setCreating(true)
              setChosen(null)
            }}
          >
            Lập hồ sơ người thân mới
          </Button>
        </form>

        <form onSubmit={submit} noValidate className="mt-2 flex flex-col gap-4">
          {creating && <MemberFields member={member} onChange={setMember} submitted={submitted} />}
          {!creating && submitted && !chosen && (
            <p className="type-body-sm text-destructive">Chọn một hồ sơ người thân hoặc lập hồ sơ mới</p>
          )}
          <Field data-invalid={(submitted && !relationship) || undefined}>
            <FieldLabel htmlFor="relationship-type">Quan hệ với người cao tuổi</FieldLabel>
            <select
              id="relationship-type"
              className="h-11 w-full rounded-md border border-input bg-transparent px-3"
              value={relationship}
              onChange={(e) => setRelationship(e.target.value)}
              aria-invalid={(submitted && !relationship) || undefined}
            >
              <option value="">Chọn quan hệ</option>
              {Object.entries(RELATIONSHIP_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
            {submitted && !relationship && <FieldError>Chọn quan hệ</FieldError>}
          </Field>
          {!hasPrimaryContact && (
            <Field orientation="horizontal">
              <Checkbox id="primary-contact" checked={primaryContact} onCheckedChange={setPrimaryContact} />
              <FieldLabel htmlFor="primary-contact">Là người liên hệ chính</FieldLabel>
            </Field>
          )}
          {!hasRepresentative && (
            <Field orientation="horizontal">
              <Checkbox
                id="first-representative"
                checked={firstRepresentative}
                onCheckedChange={setFirstRepresentative}
              />
              <FieldLabel htmlFor="first-representative">Đặt làm người đại diện đầu tiên</FieldLabel>
            </Field>
          )}
          {firstRepresentative && (
            <>
              <Field data-invalid={(submitted && !evidence) || undefined}>
                <FieldLabel htmlFor="new-evidence">Bằng chứng (văn bản ủy quyền, giấy tờ quan hệ, hợp đồng)</FieldLabel>
                <input
                  id="new-evidence"
                  type="file"
                  accept="application/pdf,image/jpeg,image/png"
                  className="type-body-sm h-11 rounded-lg border px-3 py-2"
                  onChange={(e) => setEvidence(e.target.files?.[0] ?? null)}
                />
                {submitted && !evidence && <FieldError>Chọn tệp bằng chứng</FieldError>}
              </Field>
              <Field data-invalid={(submitted && !reason.trim()) || undefined}>
                <FieldLabel htmlFor="new-reason">Lý do đặt người đại diện</FieldLabel>
                <Textarea
                  id="new-reason"
                  rows={3}
                  maxLength={2000}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
                {submitted && !reason.trim() && <FieldError>Nhập lý do</FieldError>}
              </Field>
            </>
          )}
          <ProblemAlert problem={failure}>
            {failure && CONFLICT_HINTS[failure.errorCode] && <p>{CONFLICT_HINTS[failure.errorCode]}</p>}
          </ProblemAlert>
          <Button type="submit" className="h-11 self-start" disabled={busy}>
            Lập quan hệ
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}
