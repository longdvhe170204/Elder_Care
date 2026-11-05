import { useState } from 'react'
import { postResidentsByResidentIdVisitPickupWishes } from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { SourceTag } from '@/common/SourceTag'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { RIGHT_LABELS } from '../relationshipLabels'
import { describeWish, wishSource } from './requestLabels'

// Mong muốn của người cao tuổi về khách thăm, người đón (012 T016a, T017; FR-020a, FR-020b; DESIGN.md 2.2, 7.2
// visit-wishes). Mỗi mong muốn có nhãn người nói và "Ghi bởi …"; không có nút Sửa, Xóa, thay đổi bằng "Ghi mong muốn
// mới" (mong muốn cũ chuyển Được thay thế). Quyền trái mong muốn chỉ cảnh báo, không tự đổi (Q-307).

const OUTSIDER = 'outsider'
const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

function Choice({ name, value, current, onChange, children }) {
  const id = `${name}-${value}`
  return (
    <label htmlFor={id} className="flex min-h-11 items-center gap-2">
      <input
        id={id}
        type="radio"
        name={name}
        value={value}
        checked={current === value}
        onChange={() => onChange(value)}
        className="size-4"
      />
      {children}
    </label>
  )
}

function WishLine({ wish, relationships }) {
  const tag = wishSource(wish, relationships)
  return (
    <li className="flex flex-col gap-1 border-t py-3 first:border-t-0">
      <span className="type-label">{describeWish(wish, relationships)}</span>
      <SourceTag source={tag.source} relative={tag.relative} />
      <span className="type-caption text-muted-foreground">{`Ghi bởi ${wish.recordedBy}, ${formatDate(wish.at)}`}</span>
    </li>
  )
}

// Dùng lại ở màn Phản hồi khi gắn mong muốn vào phản hồi Mong muốn (012 T072): nguồn đặt sẵn "Người cao tuổi nói"
export function RecordWishSheet({ residentId, residentName, relationships, defaultSource = '', onClose, onDone }) {
  const active = relationships.filter((r) => r.status === 'ACTIVE')
  const [key] = useState(() => crypto.randomUUID())
  const [person, setPerson] = useState('')
  const [fullName, setFullName] = useState('')
  const [idDocument, setIdDocument] = useState('')
  const [type, setType] = useState('')
  const [wanted, setWanted] = useState('')
  const [source, setSource] = useState(defaultSource)
  const [familySource, setFamilySource] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const outsider = person === OUTSIDER
  const errors = {
    person: !person,
    outsider: outsider && (!fullName.trim() || !idDocument.trim()),
    type: !type,
    wanted: !wanted,
    source: !source || (source === 'FAMILY_MEMBER' && !familySource),
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    try {
      const body = outsider
        ? { fullName: fullName.trim(), idDocumentNumber: idDocument.trim() }
        : { relationshipId: Number(person) }
      body.type = type
      body.wanted = wanted === 'yes'
      body.source =
        source === 'FAMILY_MEMBER' ? { type: source, familyMemberId: Number(familySource) } : { type: source }
      await postResidentsByResidentIdVisitPickupWishes({
        path: { residentId },
        headers: { 'Idempotency-Key': key },
        body,
      })
      onDone('Đã ghi mong muốn')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const show = (name) => submitted && errors[name]

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Ghi mong muốn mới</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Mong muốn của {residentName} về người đến thăm, người đón. Mong muốn trước đó về cùng người, cùng việc chuyển
          sang lịch sử. Quyền của người thân không tự đổi theo mong muốn.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <Field data-invalid={show('person') || undefined}>
            <FieldLabel htmlFor="wish-person">Người được nói tới</FieldLabel>
            <select
              id="wish-person"
              value={person}
              onChange={(e) => setPerson(e.target.value)}
              aria-invalid={show('person') || undefined}
              className={SELECT}
            >
              <option value="">Chọn người</option>
              {active.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.familyMember.fullName}
                </option>
              ))}
              <option value={OUTSIDER}>Người chưa có hồ sơ người thân</option>
            </select>
            {show('person') && <FieldError>Chọn người được nói tới</FieldError>}
          </Field>
          {outsider && (
            <>
              <Field data-invalid={show('outsider') || undefined}>
                <FieldLabel htmlFor="wish-name">Họ tên</FieldLabel>
                <Input
                  id="wish-name"
                  className="h-11"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  aria-invalid={show('outsider') || undefined}
                />
              </Field>
              <Field data-invalid={show('outsider') || undefined}>
                <FieldLabel htmlFor="wish-id-document">Số giấy tờ</FieldLabel>
                <Input
                  id="wish-id-document"
                  className="h-11"
                  value={idDocument}
                  onChange={(e) => setIdDocument(e.target.value)}
                  aria-invalid={show('outsider') || undefined}
                />
                {show('outsider') && <FieldError>Nhập họ tên và số giấy tờ</FieldError>}
              </Field>
            </>
          )}
          <fieldset className="flex flex-col gap-1">
            <legend className="type-label">Việc</legend>
            <div className="flex gap-6">
              <Choice name="wish-type" value="VISIT" current={type} onChange={setType}>
                Đến thăm
              </Choice>
              <Choice name="wish-type" value="PICKUP" current={type} onChange={setType}>
                Đón
              </Choice>
            </div>
            {show('type') && <p className="type-body-sm text-destructive">Chọn đến thăm hoặc đón</p>}
          </fieldset>
          <fieldset className="flex flex-col gap-1">
            <legend className="type-label">Mong muốn</legend>
            <div className="flex gap-6">
              <Choice name="wish-wanted" value="yes" current={wanted} onChange={setWanted}>
                Muốn
              </Choice>
              <Choice name="wish-wanted" value="no" current={wanted} onChange={setWanted}>
                Không muốn
              </Choice>
            </div>
            {show('wanted') && <p className="type-body-sm text-destructive">Chọn muốn hoặc không muốn</p>}
          </fieldset>
          <fieldset className="flex flex-col gap-1">
            <legend className="type-label">Ai nói</legend>
            <div className="flex flex-wrap gap-x-6">
              <Choice name="wish-source" value="RESIDENT" current={source} onChange={setSource}>
                Người cao tuổi nói
              </Choice>
              <Choice name="wish-source" value="FAMILY_MEMBER" current={source} onChange={setSource}>
                Người thân nói
              </Choice>
              <Choice name="wish-source" value="OBSERVING_STAFF" current={source} onChange={setSource}>
                Nhân viên quan sát
              </Choice>
            </div>
            {source === 'FAMILY_MEMBER' && (
              <select
                aria-label="Người thân nói"
                value={familySource}
                onChange={(e) => setFamilySource(e.target.value)}
                className={SELECT}
              >
                <option value="">Chọn người thân</option>
                {active.map((r) => (
                  <option key={r.id} value={r.familyMember.id}>
                    {r.familyMember.fullName}
                  </option>
                ))}
              </select>
            )}
            {show('source') && <p className="type-body-sm text-destructive">Chọn ai nói</p>}
          </fieldset>
          <ProblemAlert problem={failure} />
          <Button type="submit" className="h-11 self-start" disabled={busy}>
            Lưu mong muốn
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

export function VisitPickupWishes({ residentId, residentName, wishes, relationships, canCreate, onChanged }) {
  const [open, setOpen] = useState(false)
  const nameOf = (id) => relationships.find((r) => r.id === id)?.familyMember.fullName ?? 'Người thân'
  return (
    <section aria-labelledby="visit-pickup-wishes" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="visit-pickup-wishes" className="type-card-title">
          Mong muốn của người cao tuổi
        </h2>
        {canCreate && (
          <Button variant="outline" className="h-11" onClick={() => setOpen(true)}>
            Ghi mong muốn mới
          </Button>
        )}
      </div>
      {wishes.conflictingRights.length > 0 && (
        <Alert variant="attention">
          <AlertTitle>Quyền đang trái mong muốn của người cao tuổi</AlertTitle>
          <AlertDescription>
            <ul className="ml-4 list-disc">
              {wishes.conflictingRights.map((c) => (
                <li key={`${c.relationshipId}:${c.right}`}>
                  {`${nameOf(c.relationshipId)} đang có quyền ${RIGHT_LABELS[c.right] ?? c.right}.`}
                </li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      )}
      <Card>
        <CardContent>
          {wishes.effective.length === 0 ? (
            <p className="type-body-sm text-muted-foreground">Chưa ghi nhận mong muốn nào về khách thăm, người đón.</p>
          ) : (
            <ul className="flex flex-col">
              {wishes.effective.map((w) => (
                <WishLine key={w.id} wish={w} relationships={relationships} />
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
      {open && (
        <RecordWishSheet
          residentId={residentId}
          residentName={residentName}
          relationships={relationships}
          onClose={() => setOpen(false)}
          onDone={(message) => {
            setOpen(false)
            onChanged(message)
          }}
        />
      )}
    </section>
  )
}
