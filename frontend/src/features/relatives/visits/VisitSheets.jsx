import { useState } from 'react'
import { postVisits, postVisitsByIdByCommand } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { slotLabel } from './visitLabels'

// Khung lệnh của màn Đăng ký thăm (012 T025): đăng ký tại quầy (FR-029, FR-030; không đạt thì hiện mọi lý do, không
// tạo lượt), ghi vào với người thực tế (FR-033), hủy kèm lý do (FR-035). Không chọn sẵn giá trị nào.

const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

// Danh sách người nhập tay (họ tên, quan hệ); `prefix` đặt nhãn ô
function PeopleFields({ people, onChange, prefix, addLabel, submitted }) {
  const set = (i, key, value) => onChange(people.map((p, j) => (j === i ? { ...p, [key]: value } : p)))
  return (
    <div className="flex flex-col gap-3">
      {people.map((p, i) => (
        <fieldset key={i} className="grid gap-2 rounded-lg border p-3 sm:grid-cols-2">
          <legend className="type-label px-1">
            {prefix} {i + 1}
          </legend>
          <Field data-invalid={(submitted && !p.fullName.trim()) || undefined}>
            <FieldLabel htmlFor={`${prefix}-name-${i}`}>
              Họ tên {prefix.toLowerCase()} {i + 1}
            </FieldLabel>
            <Input
              id={`${prefix}-name-${i}`}
              className="h-11"
              value={p.fullName}
              onChange={(e) => set(i, 'fullName', e.target.value)}
            />
            {submitted && !p.fullName.trim() && <FieldError>Nhập họ tên</FieldError>}
          </Field>
          <Field data-invalid={(submitted && !p.relationship.trim()) || undefined}>
            <FieldLabel htmlFor={`${prefix}-relationship-${i}`}>
              Quan hệ {prefix.toLowerCase()} {i + 1}
            </FieldLabel>
            <Input
              id={`${prefix}-relationship-${i}`}
              className="h-11"
              value={p.relationship}
              onChange={(e) => set(i, 'relationship', e.target.value)}
            />
            {submitted && !p.relationship.trim() && <FieldError>Nhập quan hệ</FieldError>}
          </Field>
          <Button
            type="button"
            variant="ghost"
            className="h-11 self-end sm:col-span-2 sm:justify-self-start"
            onClick={() => onChange(people.filter((_, j) => j !== i))}
          >
            Bỏ {prefix.toLowerCase()} {i + 1}
          </Button>
        </fieldset>
      ))}
      <Button
        type="button"
        variant="outline"
        className="h-11 self-start"
        onClick={() => onChange([...people, { fullName: '', relationship: '' }])}
      >
        {addLabel}
      </Button>
    </div>
  )
}

const trimmed = (people) => people.map((p) => ({ fullName: p.fullName.trim(), relationship: p.relationship.trim() }))
const incomplete = (people) => people.some((p) => !p.fullName.trim() || !p.relationship.trim())

export function RegisterVisitSheet({ resident, date, slots, relationships, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [member, setMember] = useState('')
  const [slot, setSlot] = useState('')
  const [companions, setCompanions] = useState([])
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const active = relationships.filter((r) => r.status === 'ACTIVE')

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!member || !slot || incomplete(companions)) return
    const chosen = slots.find((s) => slotLabel(s) === slot)
    setBusy(true)
    setFailure(null)
    try {
      await postVisits({
        headers: { 'Idempotency-Key': key },
        body: {
          residentId: resident.id,
          familyMemberId: Number(member),
          date,
          startTime: chosen.startTime,
          endTime: chosen.endTime,
          companions: trimmed(companions),
        },
      })
      onDone('Đã đăng ký thăm')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Đăng ký thăm tại quầy cho {resident.name}</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Đạt mọi điều kiện thì lượt được duyệt ngay và giữ chỗ; không đạt thì không tạo lượt.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <Field data-invalid={(submitted && !member) || undefined}>
            <FieldLabel htmlFor="visit-member">Người thân</FieldLabel>
            <select id="visit-member" value={member} onChange={(e) => setMember(e.target.value)} className={SELECT}>
              <option value="">Người thân có quan hệ Hiệu lực</option>
              {active.map((r) => (
                <option key={r.id} value={r.familyMember.id}>
                  {r.familyMember.fullName}
                </option>
              ))}
            </select>
            {submitted && !member && <FieldError>Chọn người thân</FieldError>}
          </Field>
          <fieldset className="flex flex-col gap-1">
            <legend className="type-label">Khung giờ ngày {date.split('-').reverse().join('/')}</legend>
            {slots.map((s) => (
              <label
                key={slotLabel(s)}
                htmlFor={`visit-slot-${slotLabel(s)}`}
                className="flex min-h-11 items-center gap-2"
              >
                <input
                  id={`visit-slot-${slotLabel(s)}`}
                  type="radio"
                  name="visit-slot"
                  checked={slot === slotLabel(s)}
                  onChange={() => setSlot(slotLabel(s))}
                  className="size-4"
                />
                {slotLabel(s)}
                <span className="type-body-sm text-muted-foreground tabular-nums">còn {s.seatsLeft} chỗ</span>
              </label>
            ))}
            {submitted && !slot && <p className="type-body-sm text-destructive">Chọn khung giờ</p>}
          </fieldset>
          <PeopleFields
            people={companions}
            onChange={setCompanions}
            prefix="Người đi cùng"
            addLabel="Thêm người đi cùng"
            submitted={submitted}
          />
          <ProblemAlert problem={failure} />
          <Button type="submit" className="h-11 self-start" disabled={busy}>
            Đăng ký thăm
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

export function CheckInSheet({ visit, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [entered, setEntered] = useState([])
  const [added, setAdded] = useState([])
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (incomplete(added)) return
    setBusy(true)
    setFailure(null)
    const registered = visit.companions.filter((c) => entered.includes(c.fullName))
    try {
      await postVisitsByIdByCommand({
        path: { id: visit.id, command: 'check-in' },
        headers: { 'Idempotency-Key': key },
        body: {
          actualVisitors: [
            ...registered.map((c) => ({ fullName: c.fullName, relationship: c.relationship })),
            ...trimmed(added),
          ],
        },
      })
      onDone('Đã ghi vào')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Ghi vào</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Lượt {slotLabel(visit)}, đăng ký {visit.visitorCount} người. Đánh dấu người đi cùng thực tế vào; người chưa có
          trong danh sách phải thêm tên trước khi vào.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          {visit.companions.length > 0 && (
            <fieldset className="flex flex-col gap-1">
              <legend className="type-label">Người đi cùng đã đăng ký</legend>
              {visit.companions.map((c) => (
                <label key={c.fullName} htmlFor={`entered-${c.fullName}`} className="flex min-h-11 items-center gap-2">
                  <input
                    id={`entered-${c.fullName}`}
                    type="checkbox"
                    checked={entered.includes(c.fullName)}
                    onChange={(e) =>
                      setEntered(e.target.checked ? [...entered, c.fullName] : entered.filter((n) => n !== c.fullName))
                    }
                    className="size-4"
                  />
                  {c.fullName}
                </label>
              ))}
            </fieldset>
          )}
          <PeopleFields
            people={added}
            onChange={setAdded}
            prefix="Người thêm"
            addLabel="Thêm người vào"
            submitted={submitted}
          />
          <ProblemAlert problem={failure} />
          <Button type="submit" className="h-11 self-start" disabled={busy}>
            Xác nhận ghi vào
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

export function CancelVisitSheet({ visit, onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!reason.trim()) return
    setBusy(true)
    setFailure(null)
    try {
      await postVisitsByIdByCommand({
        path: { id: visit.id, command: 'cancel' },
        headers: { 'Idempotency-Key': key },
        body: { reason: reason.trim() },
      })
      onDone('Đã hủy lượt thăm')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-xl">
        <SheetTitle className="type-title">Hủy lượt thăm</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Lượt {slotLabel(visit)}. Hủy trả chỗ cho khung; chỉ hủy được trước giờ bắt đầu.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <Field data-invalid={(submitted && !reason.trim()) || undefined}>
            <FieldLabel htmlFor="visit-cancel-reason">Lý do</FieldLabel>
            <Textarea
              id="visit-cancel-reason"
              rows={3}
              maxLength={2000}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            {submitted && !reason.trim() && <FieldError>Nhập lý do</FieldError>}
          </Field>
          <ProblemAlert problem={failure} />
          <Button type="submit" variant="destructive" className="h-11 self-start" disabled={busy}>
            Xác nhận hủy
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}
