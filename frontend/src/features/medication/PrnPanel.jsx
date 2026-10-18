import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { AllergyNote } from '@/common/CareNotes'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { DEMO_PRN_ORDERS, DEMO_RESIDENTS } from '@/common/demoData'
import { DoseTags } from './DoseTags'
import { minutes, toHHMM } from './medicationTime'

// Thuốc khi cần (spec 006 User Story 6; FR-024, FR-025): không có lịch, ghi từng lần kèm lý do. Hệ thống chặn khi
// chưa đủ khoảng cách tối thiểu hoặc đã đủ số lần trong 24 giờ trượt, hiện thời điểm sớm nhất được dùng;
// không có thao tác vượt chặn. Bản mẫu tính trong ngày; tính thật do backend.

const HHMM = /^([01]\d|2[0-3]):[0-5]\d$/
const residentOf = (code) => DEMO_RESIDENTS.find((r) => r.code === code)

// Thời điểm sớm nhất được dùng: muộn hơn trong hai mốc (lần gần nhất + khoảng cách; lần cũ nhất trong 24 giờ + 24 giờ)
function earliestOf(order) {
  if (order.uses.length === 0) return null
  const last = Math.max(...order.uses.map(minutes))
  const byGap = last + order.minHours * 60
  const byCount = order.uses.length >= order.maxPer24h ? Math.min(...order.uses.map(minutes)) + 24 * 60 : 0
  return Math.max(byGap, byCount)
}

function blockReason(order, at) {
  const earliest = earliestOf(order)
  if (earliest === null || minutes(at) >= earliest) return null
  return order.uses.length >= order.maxPer24h ? 'Đã đủ số lần trong 24 giờ' : 'Chưa đủ khoảng cách'
}

function PrnForm({ order, now, onSave }) {
  const [time, setTime] = useState(now)
  const [reason, setReason] = useState('')
  const [reaction, setReaction] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const timeInvalid = !HHMM.test(time) || time > now
  const blocked = !timeInvalid && blockReason(order, time)
  const r = residentOf(order.residentCode)

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (timeInvalid || blocked || !reason.trim()) return
    onSave(order, time)
  }

  return (
    <form onSubmit={submit} noValidate className="flex min-h-0 flex-1 flex-col">
      <SheetHeader className="gap-3 pt-6">
        <div className="flex items-center gap-3">
          <div className="flex shrink-0 flex-col items-center gap-1">
            <ResidentAvatar name={r.name} code={r.code} initials={r.initials} className="size-16 type-title" />
            {!r.photo && <span className="type-caption text-muted-foreground">Chưa có ảnh</span>}
          </div>
          <div className="flex flex-col gap-0.5">
            <SheetTitle className="type-title">{r.name}</SheetTitle>
            <SheetDescription>
              {order.drug}, {order.dose}, {order.route}
            </SheetDescription>
          </div>
        </div>
        <AllergyNote allergies={r.allergies} />
      </SheetHeader>
      <div className="flex flex-col gap-4 px-4">
        <Field data-invalid={(submitted && (timeInvalid || blocked)) || undefined}>
          <FieldLabel htmlFor="prn-time">Thời điểm dùng</FieldLabel>
          <Input id="prn-time" inputMode="numeric" value={time} onChange={(e) => setTime(e.target.value)} className="h-11 w-32 tabular-nums" />
          {submitted && timeInvalid && <FieldError>Nhập giờ dạng HH:mm, không sau {now}</FieldError>}
          {submitted && blocked && (
            <FieldError>
              {blocked}: sớm nhất được dùng lúc {toHHMM(earliestOf(order))}. Cần thì báo bác sĩ.
            </FieldError>
          )}
        </Field>
        <Field data-invalid={(submitted && !reason.trim()) || undefined}>
          <FieldLabel htmlFor="prn-reason">Lý do dùng</FieldLabel>
          <Textarea id="prn-reason" rows={2} value={reason} onChange={(e) => setReason(e.target.value)} aria-invalid={(submitted && !reason.trim()) || undefined} />
          {submitted && !reason.trim() && <FieldError>Nhập lý do dùng thuốc khi cần</FieldError>}
        </Field>
        <Field>
          <FieldLabel htmlFor="prn-reaction">Phản ứng sau dùng</FieldLabel>
          <Textarea id="prn-reaction" rows={2} value={reaction} onChange={(e) => setReaction(e.target.value)} />
          <FieldDescription>Không bắt buộc. Có ghi phản ứng thì hệ thống tạo cảnh báo cho người phụ trách.</FieldDescription>
        </Field>
      </div>
      <SheetFooter>
        <Button type="submit" className="h-11">
          Lưu lần dùng
        </Button>
      </SheetFooter>
    </form>
  )
}

export function PrnPanel({ canRecord, now, onSaved }) {
  const [orders, setOrders] = useState(DEMO_PRN_ORDERS)
  const [recording, setRecording] = useState(null)

  function save(order, time) {
    setOrders((all) => all.map((o) => (o.id === order.id ? { ...o, uses: [...o.uses, time] } : o)))
    setRecording(null)
    onSaved(`Đã lưu lần dùng ${order.drug} lúc ${time}. Bản mẫu: trạng thái thật do backend trả.`)
  }

  return (
    <>
      <div className="grid grid-cols-[repeat(auto-fit,minmax(280px,1fr))] gap-3 *:max-w-[480px]">
        {orders.map((o) => {
          const r = residentOf(o.residentCode)
          const blocked = blockReason(o, now)
          const earliest = earliestOf(o)
          return (
            <Card key={o.id} className="gap-3">
              <CardHeader className="flex flex-row items-center gap-2.5">
                <ResidentAvatar name={r.name} code={r.code} initials={r.initials} />
                <div className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate type-label">{r.name}</span>
                  <span className="type-caption text-muted-foreground tabular-nums">{r.room}</span>
                </div>
              </CardHeader>
              <CardContent className="flex flex-1 flex-col gap-3 type-body-sm">
                <AllergyNote allergies={r.allergies} />
                <div className="flex flex-col gap-1.5">
                  <span className="type-card-title">
                    {o.drug}
                    <span className="block type-body-sm font-normal text-muted-foreground">
                      {o.dose}, {o.route}; cách ít nhất {o.minHours} giờ, tối đa {o.maxPer24h} lần trong 24 giờ
                    </span>
                  </span>
                  <DoseTags dose={o} />
                </div>
                <span className="text-muted-foreground tabular-nums">
                  Lần gần nhất {o.uses.at(-1) ?? 'chưa có'}; {o.uses.length} lần trong 24 giờ
                </span>
                {blocked && (
                  <p className="rounded-lg bg-warning px-3 py-2 text-warning-foreground">
                    {blocked}: sớm nhất được dùng lúc <span className="tabular-nums">{toHHMM(earliest)}</span>. Không có thao tác vượt chặn; cần thì báo bác sĩ.
                  </p>
                )}
                {canRecord && !blocked && (
                  <div className="mt-auto flex justify-end border-t pt-3">
                    <Button variant="tonal" onClick={() => setRecording(o)}>
                      Ghi lần dùng
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          )
        })}
      </div>
      <Sheet open={!!recording} onOpenChange={(open) => !open && setRecording(null)}>
        <SheetContent>{recording && <PrnForm key={recording.id} order={recording} now={now} onSave={save} />}</SheetContent>
      </Sheet>
    </>
  )
}
