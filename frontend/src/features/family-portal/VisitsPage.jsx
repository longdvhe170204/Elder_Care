import { useState } from 'react'
import { Plus, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_PORTAL_VISIT_MAX_PEOPLE, DEMO_PORTAL_VISIT_SLOTS, DEMO_PORTAL_VISITS, DEMO_RESIDENTS } from '@/common/demoData'
import { PortalCard } from './PortalParts'

// P5 Thăm (14.2; 012 FR-029 → FR-039): đạt mọi kiểm tra thì lượt tạo thẳng ở Đã duyệt; không đạt thì từ chối
// ngay kèm lý do, không tạo lượt. Đổi khung bằng hủy rồi đăng ký lại. Bản mẫu chỉ kiểm khung và số chỗ;
// kiểm tra khoanh vùng, người cao tuổi vắng do backend.

const DAYS = ['19/10', '20/10', '21/10', '22/10', '23/10', '24/10', '25/10']
const WEEKDAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

function RegisterForm({ onRegistered }) {
  const [day, setDay] = useState('')
  const [slot, setSlot] = useState('')
  const [companions, setCompanions] = useState([])
  const [submitted, setSubmitted] = useState(false)
  const [rejection, setRejection] = useState('')

  const people = 1 + companions.length
  const slotInfo = DEMO_PORTAL_VISIT_SLOTS.find((s) => s.label === slot)
  const companionMissing = companions.some((c) => !c.name.trim() || !c.relation.trim())

  function updateCompanion(i, key, value) {
    setCompanions(companions.map((c, j) => (j === i ? { ...c, [key]: value } : c)))
  }

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    setRejection('')
    if (!day || !slot || companionMissing) return
    // Từ chối ngay kèm lý do cụ thể (FR-030 b, FR-031)
    if (slotInfo.left < people) {
      setRejection(`Không đăng ký được: khung ${slot} ngày ${day} chỉ còn ${slotInfo.left} chỗ, lượt của bạn có ${people} người.`)
      return
    }
    onRegistered({ id: `new-${Date.now()}`, when: `${slot}, ${day}/2026`, people, status: 'Đã duyệt' })
    setDay('')
    setSlot('')
    setCompanions([])
    setSubmitted(false)
  }

  return (
    <PortalCard title={`Đăng ký thăm ${DEMO_RESIDENTS[0].name}`}>
      <form onSubmit={submit} noValidate className="flex flex-col gap-4">
        <Field data-invalid={(submitted && !day) || undefined}>
          <FieldLabel>Ngày</FieldLabel>
          <ToggleGroup variant="outline" value={day ? [day] : []} onValueChange={(n) => setDay(n[0] ?? '')} className="flex flex-wrap">
            {DAYS.map((d, i) => (
              <ToggleGroupItem key={d} value={d} className="h-14 min-w-14 flex-col gap-0 px-2">
                <span className="type-caption">{WEEKDAYS[(i + 1) % 7]}</span>
                <span className="type-label tabular-nums">{d}</span>
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          {submitted && !day && <FieldError>Chọn ngày thăm</FieldError>}
        </Field>

        <Field data-invalid={(submitted && !slot) || undefined}>
          <FieldLabel>Khung giờ</FieldLabel>
          <ToggleGroup variant="outline" value={slot ? [slot] : []} onValueChange={(n) => setSlot(n[0] ?? '')} className="grid w-full grid-cols-1 sm:grid-cols-2">
            {DEMO_PORTAL_VISIT_SLOTS.map((s) => (
              <ToggleGroupItem key={s.label} value={s.label} className="h-14 justify-between px-4">
                <span className="type-label tabular-nums">{s.label}</span>
                <span className="type-caption text-muted-foreground">còn {s.left} chỗ</span>
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          {submitted && !slot && <FieldError>Chọn khung giờ</FieldError>}
        </Field>

        <Field data-invalid={(submitted && companionMissing) || undefined}>
          <FieldLabel>Người đi cùng</FieldLabel>
          <FieldDescription>Tối đa {DEMO_PORTAL_VISIT_MAX_PEOPLE} người mỗi lượt, kể cả bạn.</FieldDescription>
          {companions.map((c, i) => (
            <div key={i} className="flex items-start gap-2">
              <Input
                aria-label={`Họ tên người đi cùng ${i + 1}`}
                placeholder="Họ tên"
                value={c.name}
                onChange={(e) => updateCompanion(i, 'name', e.target.value)}
                className="h-11"
              />
              <Input
                aria-label={`Quan hệ của người đi cùng ${i + 1}`}
                placeholder="Quan hệ"
                value={c.relation}
                onChange={(e) => updateCompanion(i, 'relation', e.target.value)}
                className="h-11 w-32 shrink-0"
              />
              <Button
                type="button"
                variant="ghost"
                size="icon-lg"
                aria-label={`Bỏ người đi cùng ${i + 1}`}
                onClick={() => setCompanions(companions.filter((_, j) => j !== i))}
              >
                <X aria-hidden="true" />
              </Button>
            </div>
          ))}
          {submitted && companionMissing && <FieldError>Nhập họ tên và quan hệ của người đi cùng</FieldError>}
          {people < DEMO_PORTAL_VISIT_MAX_PEOPLE && (
            <Button type="button" variant="outline" className="self-start" onClick={() => setCompanions([...companions, { name: '', relation: '' }])}>
              <Plus aria-hidden="true" />
              Thêm người đi cùng
            </Button>
          )}
        </Field>

        {rejection && (
          <p role="alert" className="rounded-lg bg-warning px-3 py-2 text-warning-foreground">
            {rejection}
          </p>
        )}
        <Button type="submit" className="h-11">
          Đăng ký thăm
        </Button>
      </form>
    </PortalCard>
  )
}

export function VisitsPage({ viewer }) {
  const [visits, setVisits] = useState(DEMO_PORTAL_VISITS)
  const [notice, setNotice] = useState('')

  function cancel(id) {
    setVisits(visits.map((v) => (v.id === id ? { ...v, status: 'Hủy', reason: 'Bạn đã hủy' } : v)))
    setNotice('Đã hủy lượt thăm, chỗ được trả lại cho khung giờ.')
  }

  return (
    <>
      <PageHeader title="Thăm" subtitle="Khung thăm 09:00 → 11:00 và 15:00 → 17:00 hằng ngày." />
      {viewer.perms.visits ? (
        <RegisterForm
          onRegistered={(v) => {
            setVisits([v, ...visits])
            setNotice(`Đã đăng ký: lượt ${v.when} được duyệt ngay.`)
          }}
        />
      ) : (
        <PortalCard>
          <p>Bạn chưa có quyền đăng ký thăm. Liên hệ người đại diện của gia đình hoặc Hành chính của viện để được cấp quyền.</p>
        </PortalCard>
      )}
      <p role="status" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {notice}
      </p>
      <PortalCard title="Lượt thăm của bạn">
        {visits.length === 0 ? (
          <p className="text-muted-foreground">Bạn chưa có lượt thăm nào.</p>
        ) : (
          <ul className="flex flex-col">
            {visits.map((v) => (
              <li key={v.id} className="flex flex-wrap items-center justify-between gap-2 border-t py-2.5 first:border-t-0 first:pt-0">
                <span className="flex flex-col gap-0.5">
                  <span className="type-label tabular-nums">{v.when}</span>
                  <span className="type-caption text-muted-foreground">
                    {v.people} người{v.reason && `, ${v.reason.toLowerCase()}`}
                  </span>
                </span>
                <span className="flex items-center gap-2">
                  <StatusBadge status={v.status} />
                  {v.status === 'Đã duyệt' && (
                    <Button variant="outline" size="sm" onClick={() => cancel(v.id)}>
                      Hủy lượt
                    </Button>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </PortalCard>
    </>
  )
}
