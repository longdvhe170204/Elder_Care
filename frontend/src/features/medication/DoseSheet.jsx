import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { AllergyNote } from '@/common/CareNotes'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { cn } from '@/lib/utils'
import { DoseTags } from './DoseTags'
import { minutes, windowOf } from './medicationTime'

// Khung xác nhận liều (DESIGN.md 7.2 detail-panel, form ghi nhận; spec 006 FR-021, FR-026, FR-029 → FR-032, FR-044).
// Mỗi liều chọn kết quả riêng và lưu thành bản ghi riêng; mỗi liều chỉ xác nhận một lần (BR-M07-12).
// Bản mẫu: kiểm tra trên màn chỉ để hiện lỗi tại ô; kiểm tra thật (giấy phép, phân công, trực tuyến) do backend.

const RESULTS = ['Đã dùng', 'Từ chối', 'Không thực hiện']
const OPEN = ['Đến giờ', 'Trễ']
const HHMM = /^([01]\d|2[0-3]):[0-5]\d$/

function errorsOf(dose, entry, now) {
  if (!entry.result) return {}
  const e = {}
  if (entry.result === 'Đã dùng') {
    if (!HHMM.test(entry.time) || entry.time > now) e.time = `Nhập giờ dạng HH:mm, không sau ${now}`
    if (dose.source === 'Gia đình gửi') {
      const lot = dose.lots.find((l) => l.code === entry.lot)
      if (!lot) e.lot = 'Chọn lô thuốc gia đình gửi'
      else if (lot.expired) e.lot = `${lot.code} đã hết hạn dùng, chọn lô khác`
    }
  } else if (!entry.reason.trim()) {
    e.reason = entry.result === 'Từ chối' ? 'Nhập lý do người cao tuổi từ chối' : 'Nhập lý do không thực hiện'
  }
  return e
}

function DoseForm({ dose, entry, onChange, errors }) {
  const late = entry.result === 'Đã dùng' && HHMM.test(entry.time) && minutes(entry.time) > minutes(windowOf(dose.time).to)
  return (
    <fieldset className="flex flex-col gap-3 rounded-xl border p-4">
      <legend className="sr-only">
        {dose.drug}, {dose.dose}
      </legend>
      <div className="flex flex-col gap-1.5">
        <span className="type-card-title">
          {dose.drug}, {dose.dose}, {dose.route}
        </span>
        <DoseTags dose={dose} />
      </div>
      <ToggleGroup
        variant="outline"
        value={entry.result ? [entry.result] : []}
        onValueChange={(n) => onChange({ result: n[0] ?? '' })}
        className="grid w-full grid-cols-3"
        aria-label={`Kết quả liều ${dose.drug}`}
      >
        {RESULTS.map((r) => (
          <ToggleGroupItem key={r} value={r} className="h-11 px-2">
            {r}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>

      {entry.result === 'Đã dùng' && (
        <>
          {/* FR-044: hiện các lô kèm hạn và số còn để điều dưỡng chọn, không chọn sẵn hay đề xuất lô (Q-307) */}
          {dose.source === 'Gia đình gửi' && (
            <Field data-invalid={(errors.lot && true) || undefined}>
              <FieldLabel>Lô thuốc gia đình gửi</FieldLabel>
              <ToggleGroup
                variant="outline"
                value={entry.lot ? [entry.lot] : []}
                onValueChange={(n) => onChange({ lot: n[0] ?? '' })}
                className="flex w-full flex-col items-stretch"
              >
                {dose.lots.map((l) => (
                  <ToggleGroupItem key={l.code} value={l.code} className="h-11 justify-between px-4">
                    <span className="type-label">{l.code}</span>
                    <span className={cn('type-caption', l.expired ? 'text-warning-foreground' : 'text-muted-foreground')}>
                      Hạn {l.expiry}, còn {l.left}
                      {l.expired && ', hết hạn dùng'}
                    </span>
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
              {errors.lot && <FieldError>{errors.lot}</FieldError>}
            </Field>
          )}
          <Field data-invalid={(errors.time && true) || undefined}>
            <FieldLabel htmlFor={`time-${dose.id}`}>Thời điểm dùng</FieldLabel>
            <Input
              id={`time-${dose.id}`}
              inputMode="numeric"
              value={entry.time}
              onChange={(e) => onChange({ time: e.target.value })}
              aria-invalid={(errors.time && true) || undefined}
              className="h-11 w-32 tabular-nums"
            />
            {errors.time ? (
              <FieldError>{errors.time}</FieldError>
            ) : (
              <FieldDescription>{late ? 'Sau cửa sổ: liều được ghi "dùng trễ".' : 'Giờ 24h, không sau thời điểm ghi.'}</FieldDescription>
            )}
          </Field>
          <Field>
            <FieldLabel htmlFor={`reaction-${dose.id}`}>Phản ứng sau dùng</FieldLabel>
            <Textarea id={`reaction-${dose.id}`} rows={2} value={entry.reaction} onChange={(e) => onChange({ reaction: e.target.value })} />
            <FieldDescription>Không bắt buộc. Có ghi phản ứng thì hệ thống tạo cảnh báo cho người phụ trách.</FieldDescription>
          </Field>
        </>
      )}

      {(entry.result === 'Từ chối' || entry.result === 'Không thực hiện') && (
        <Field data-invalid={(errors.reason && true) || undefined}>
          <FieldLabel htmlFor={`reason-${dose.id}`}>Lý do</FieldLabel>
          <Textarea
            id={`reason-${dose.id}`}
            rows={2}
            value={entry.reason}
            onChange={(e) => onChange({ reason: e.target.value })}
            aria-invalid={(errors.reason && true) || undefined}
          />
          {errors.reason && <FieldError>{errors.reason}</FieldError>}
          {entry.result === 'Từ chối' && dose.refusedBefore && (
            <FieldDescription>Lần trước người cao tuổi cũng từ chối thuốc này; ghi lần này thì hệ thống tạo cảnh báo.</FieldDescription>
          )}
        </Field>
      )}
    </fieldset>
  )
}

function SheetBody({ group, now, onSave }) {
  const open = group.doses.filter((d) => OPEN.includes(d.status))
  const [entries, setEntries] = useState(() =>
    Object.fromEntries(open.map((d) => [d.id, { result: '', lot: '', time: now, reaction: '', reason: '' }])),
  )
  const [submitted, setSubmitted] = useState(false)
  const errors = Object.fromEntries(open.map((d) => [d.id, errorsOf(d, entries[d.id], now)]))
  const decided = open.filter((d) => entries[d.id].result)
  const noneDecided = decided.length === 0
  const r = group.resident

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (noneDecided || decided.some((d) => Object.keys(errors[d.id]).length)) return
    onSave(decided.map((d) => ({ dose: d, ...entries[d.id] })))
  }

  return (
    <form onSubmit={submit} noValidate className="flex min-h-0 flex-1 flex-col">
      <SheetHeader className="gap-3 pt-6">
        {/* Ảnh nhận diện để xác định đúng người trước khi cho dùng (DESIGN.md mục 2) */}
        <div className="flex items-center gap-3">
          <div className="flex shrink-0 flex-col items-center gap-1">
            <ResidentAvatar name={r.name} code={r.code} initials={r.initials} className="size-16 type-title" />
            {!r.photo && <span className="type-caption text-muted-foreground">Chưa có ảnh</span>}
          </div>
          <div className="flex flex-col gap-0.5">
            <SheetTitle className="type-title">{r.name}</SheetTitle>
            <SheetDescription className="tabular-nums">
              Phòng {r.room}, giờ {group.time}, cửa sổ {windowOf(group.time).from} → {windowOf(group.time).to}
            </SheetDescription>
          </div>
        </div>
        <AllergyNote allergies={r.allergies} />
      </SheetHeader>
      <div className="flex flex-col gap-3 overflow-y-auto px-4">
        {open.map((d) => (
          <DoseForm
            key={d.id}
            dose={d}
            entry={entries[d.id]}
            errors={submitted ? errors[d.id] : {}}
            onChange={(patch) => setEntries({ ...entries, [d.id]: { ...entries[d.id], ...patch } })}
          />
        ))}
        {submitted && noneDecided && <FieldError>Chọn kết quả cho ít nhất một liều</FieldError>}
      </div>
      <SheetFooter>
        <Button type="submit" className="h-11">
          {decided.length > 1 ? `Lưu xác nhận ${decided.length} liều` : 'Lưu xác nhận'}
        </Button>
      </SheetFooter>
    </form>
  )
}

export function DoseSheet({ group, now, onOpenChange, onSave }) {
  return (
    <Sheet open={!!group} onOpenChange={onOpenChange}>
      <SheetContent>{group && <SheetBody key={group.key} group={group} now={now} onSave={onSave} />}</SheetContent>
    </Sheet>
  )
}
