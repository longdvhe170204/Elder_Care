import { useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_ALERTS, DEMO_RESIDENTS } from '@/common/demoData'
import { cn } from '@/lib/utils'

// Bản mẫu màn Cảnh báo (M15; spec 007 User Story 3, FR-029 → FR-036). Điều dưỡng, Trưởng tầng, Bác sĩ thao tác;
// Quản lý viện chỉ tiếp nhận được khi cảnh báo ở Leo thang cấp 2 (Phụ lục 27 chú thích 23); Nhân viên chăm sóc xem.
// Nút theo trạng thái đúng bảng FR-029. Leo thang, gộp, hạn tiếp nhận do hệ thống tính; ở đây là dữ liệu minh họa.

const ACT_ROLES = ['NURSING_STAFF', 'FLOOR_MANAGER', 'DOCTOR']
const MANAGER = 'NURSING_HOME_MANAGER'
const LEVELS = ['Khẩn cấp', 'Trung bình', 'Nhẹ']
const OPEN = ['Mới', 'Leo thang', 'Đã tiếp nhận', 'Đang xử lý']
const residentOf = (code) => DEMO_RESIDENTS.find((r) => r.code === code)

// Nhãn trạng thái: leo thang tối đa là việc gấp (DESIGN.md 3.5)
function statusLabel(a) {
  if (a.status === 'Leo thang') return a.maxEscalation ? 'Đã leo thang tối đa' : `Leo thang cấp ${a.escalation}`
  return a.status
}

// Tông trạng thái (DESIGN.md 3.5): leo thang tối đa đỏ; Mới, leo thang hổ phách; Đã đóng xanh lá; còn lại trung tính
function AlertStatus({ alert }) {
  const tone = alert.maxEscalation && alert.status === 'Leo thang' ? 'urgent' : ['Mới', 'Leo thang'].includes(alert.status) ? 'attention' : alert.status === 'Đã đóng' ? 'settled' : 'neutral'
  return <Badge variant={tone}>{statusLabel(alert)}</Badge>
}

function canAccept(a, role) {
  if (!['Mới', 'Leo thang'].includes(a.status)) return false
  return ACT_ROLES.includes(role) || (role === MANAGER && a.status === 'Leo thang' && a.escalation === 2)
}

function AlertCard({ alert, role, onAction, onOpen }) {
  const r = residentOf(alert.residentCode)
  const urgent = alert.level === 'Khẩn cấp' || alert.maxEscalation
  const act = ACT_ROLES.includes(role)
  return (
    <Card className={cn('gap-3', urgent && alert.status !== 'Đã đóng' && 'ring-[1.5px] ring-destructive')}>
      <CardHeader className="flex flex-row items-center gap-2.5">
        <ResidentAvatar name={r.name} code={r.code} initials={r.initials} />
        <div className="flex min-w-0 flex-1 flex-col">
          <span className="truncate type-label">{r.name}</span>
          <span className="type-caption text-muted-foreground tabular-nums">{r.room === '—' ? 'Bán trú' : r.room}</span>
        </div>
        <StatusBadge status={alert.level} />
      </CardHeader>
      <CardContent className="flex flex-1 flex-col gap-2 type-body-sm">
        <span className="type-card-title">{alert.text}</span>
        <span className="text-muted-foreground">
          Nguồn: {alert.source}
          {alert.merged > 1 && `, ${alert.merged} lần cùng loại đã gộp`}
        </span>
        <div className="flex flex-wrap items-center gap-2">
          <AlertStatus alert={alert} />
          <span className="type-caption text-muted-foreground">Phụ trách: {alert.mine ? 'bạn' : alert.owner}</span>
        </div>
        {alert.due && <span className={cn('type-caption', urgent ? 'text-destructive' : 'text-muted-foreground')}>{alert.due}</span>}
        {alert.result && <span className="text-muted-foreground">Kết quả: {alert.result}</span>}
        <div className="mt-auto flex flex-wrap justify-end gap-2 border-t pt-3">
          <Button variant="ghost" size="sm" onClick={() => onOpen(alert)}>
            Chi tiết
          </Button>
          {canAccept(alert, role) && (
            <Button variant={urgent ? 'default' : 'tonal'} onClick={() => onAction(alert, { status: 'Đã tiếp nhận', mine: true, due: null }, 'Đã tiếp nhận cảnh báo')}>
              Tiếp nhận
            </Button>
          )}
          {act && alert.status === 'Đã tiếp nhận' && (
            <Button variant="tonal" onClick={() => onAction(alert, { status: 'Đang xử lý' }, 'Đã bắt đầu xử lý')}>
              Bắt đầu xử lý
            </Button>
          )}
          {act && alert.status === 'Đang xử lý' && (
            <Button variant="tonal" onClick={() => onOpen(alert)}>
              Đóng cảnh báo
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

// Khung chi tiết: lịch sử, các lệnh cần nhập lý do hay kết quả (đóng, đổi mức), chuyển sự cố, khẩn cấp
function AlertSheet({ alert, role, onAction }) {
  const [mode, setMode] = useState(null)
  const [text, setText] = useState('')
  const [level, setLevel] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const act = ACT_ROLES.includes(role)
  const open = OPEN.includes(alert.status)
  const r = residentOf(alert.residentCode)
  // Hạ từ Khẩn cấp chỉ Bác sĩ (FR-029)
  const levelChoices = LEVELS.filter((l) => l !== alert.level && (alert.level !== 'Khẩn cấp' || role === 'DOCTOR'))

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (!text.trim() || (mode === 'level' && !level)) return
    if (mode === 'close') onAction(alert, { status: 'Đã đóng', result: text.trim(), due: null }, 'Đã đóng cảnh báo')
    if (mode === 'level') onAction(alert, { level }, `Đã đổi mức sang ${level.toLowerCase()}`)
  }

  return (
    <>
      <SheetHeader className="gap-2 pt-6">
        <SheetTitle className="type-title">{alert.text}</SheetTitle>
        <SheetDescription>
          {r.name}, {r.room === '—' ? 'bán trú' : `phòng ${r.room}`}; nguồn: {alert.source}
        </SheetDescription>
        <div className="flex flex-wrap gap-2">
          <StatusBadge status={alert.level} />
          <AlertStatus alert={alert} />
        </div>
      </SheetHeader>
      <div className="flex flex-col gap-4 px-4 type-body-sm">
        <section className="flex flex-col gap-2">
          <h3 className="type-caption text-muted-foreground">Lịch sử</h3>
          <ol className="flex flex-col gap-1.5 border-l-2 pl-3">
            {alert.history.map((h) => (
              <li key={h} className="tabular-nums">
                {h}
              </li>
            ))}
          </ol>
        </section>
        {act && open && !mode && (
          <div className="flex flex-col gap-2">
            {alert.status === 'Đang xử lý' && (
              <Button className="h-11" onClick={() => setMode('close')}>
                Đóng cảnh báo
              </Button>
            )}
            <Button variant="outline" className="h-11" onClick={() => setMode('level')}>
              Đổi mức
            </Button>
            {alert.status === 'Đang xử lý' && (
              <Button variant="outline" className="h-11" onClick={() => onAction(alert, { status: 'Chuyển sự cố', due: null }, 'Đã chuyển thành sự cố. Bản mẫu: màn sự cố chưa có lệnh xử lý')}>
                Chuyển sự cố
              </Button>
            )}
            {['Mới', 'Leo thang', 'Đã tiếp nhận'].includes(alert.status) && (
              <Button variant="destructive" className="h-11" onClick={() => onAction(alert, { status: 'Chuyển sự cố', level: 'Khẩn cấp', due: null }, 'Đã kích hoạt khẩn cấp. Bản mẫu: quy trình khẩn cấp chưa có bản mẫu')}>
                Kích hoạt khẩn cấp
              </Button>
            )}
          </div>
        )}
        {mode && (
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            {mode === 'level' && (
              <Field data-invalid={(submitted && !level) || undefined}>
                <FieldLabel>Mức mới</FieldLabel>
                <ToggleGroup variant="outline" value={level ? [level] : []} onValueChange={(n) => setLevel(n[0] ?? '')} className="flex flex-wrap">
                  {levelChoices.map((l) => (
                    <ToggleGroupItem key={l} value={l} className="h-11 px-4">
                      {l}
                    </ToggleGroupItem>
                  ))}
                </ToggleGroup>
                {submitted && !level && <FieldError>Chọn mức mới</FieldError>}
              </Field>
            )}
            <Field data-invalid={(submitted && !text.trim()) || undefined}>
              <FieldLabel htmlFor="alert-text">
                {mode === 'close' ? (alert.fromMandatoryTask ? 'Kết quả xử lý, lý do' : 'Kết quả xử lý') : 'Lý do đổi mức'}
              </FieldLabel>
              <Textarea id="alert-text" rows={3} value={text} onChange={(e) => setText(e.target.value)} aria-invalid={(submitted && !text.trim()) || undefined} />
              {submitted && !text.trim() && <FieldError>{mode === 'close' ? 'Nhập kết quả xử lý' : 'Nhập lý do đổi mức'}</FieldError>}
            </Field>
            <SheetFooter className="px-0">
              <Button type="submit" className="h-11">
                {mode === 'close' ? 'Đóng cảnh báo' : 'Lưu mức mới'}
              </Button>
              <Button type="button" variant="ghost" className="h-11" onClick={() => setMode(null)}>
                Quay lại
              </Button>
            </SheetFooter>
          </form>
        )}
      </div>
    </>
  )
}

export function AlertsPage({ role }) {
  const [alerts, setAlerts] = useState(DEMO_ALERTS)
  const [view, setView] = useState('open')
  const [levelFilter, setLevelFilter] = useState('Tất cả')
  const [openId, setOpenId] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const byView = alerts.filter((a) =>
    view === 'open' ? OPEN.includes(a.status) : view === 'mine' ? a.mine && OPEN.includes(a.status) : !OPEN.includes(a.status),
  )
  const visible = byView
    .filter((a) => levelFilter === 'Tất cả' || a.level === levelFilter)
    .toSorted((a, b) => LEVELS.indexOf(a.level) - LEVELS.indexOf(b.level) || Number(!!b.maxEscalation) - Number(!!a.maxEscalation))
  const opened = alerts.find((a) => a.id === openId)
  const viewCount = (v) => alerts.filter((a) => (v === 'open' ? OPEN.includes(a.status) : v === 'mine' ? a.mine && OPEN.includes(a.status) : !OPEN.includes(a.status))).length

  function action(alert, patch, message) {
    setAlerts((all) => all.map((a) => (a.id === alert.id ? { ...a, ...patch } : a)))
    setOpenId(null)
    setAnnouncement(`${message}: ${alert.text}. Bản mẫu: trạng thái thật do backend trả.`)
  }

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
      <PageHeader
        title="Cảnh báo tầng 2"
        subtitle={ACT_ROLES.includes(role) ? 'Cảnh báo cần tiếp nhận và xử lý trong phạm vi của bạn.' : 'Bạn chỉ xem cảnh báo trong phạm vi.'}
      >
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <ToggleGroup variant="filter" size="lg" value={[view]} onValueChange={(n) => n[0] && setView(n[0])} aria-label="Nhóm cảnh báo">
            {[
              ['open', 'Đang mở'],
              ['mine', 'Của tôi'],
              ['closed', 'Đã đóng'],
            ].map(([v, label]) => (
              <ToggleGroupItem key={v} value={v} className="h-11 px-4">
                {label} <span className="tabular-nums">{viewCount(v)}</span>
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          <ToggleGroup variant="filter" size="lg" value={[levelFilter]} onValueChange={(n) => n[0] && setLevelFilter(n[0])} className="flex-wrap" aria-label="Lọc theo mức">
            {['Tất cả', ...LEVELS].map((l) => (
              <ToggleGroupItem key={l} value={l} className="h-11 px-4">
                {l} <span className="tabular-nums">{l === 'Tất cả' ? byView.length : byView.filter((a) => a.level === l).length}</span>
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
        </div>
      </PageHeader>
      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {announcement}
      </p>
      {visible.length === 0 ? (
        <Card>
          <CardContent className="type-body-sm">Không có cảnh báo trong nhóm này.</CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-[repeat(auto-fit,minmax(300px,1fr))] gap-3 *:max-w-[480px]">
          {visible.map((a) => (
            <AlertCard key={a.id} alert={a} role={role} onAction={action} onOpen={(x) => setOpenId(x.id)} />
          ))}
        </div>
      )}
      <Sheet open={!!opened} onOpenChange={(o) => !o && setOpenId(null)}>
        <SheetContent>{opened && <AlertSheet key={opened.id} alert={opened} role={role} onAction={action} />}</SheetContent>
      </Sheet>
    </div>
  )
}
