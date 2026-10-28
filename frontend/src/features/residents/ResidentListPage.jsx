import { useEffect, useState } from 'react'
import { Search, TriangleAlert } from 'lucide-react'
import { getMe } from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { InputGroup, InputGroupAddon, InputGroupInput } from '@/components/ui/input-group'
import { Separator } from '@/components/ui/separator'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { AllergyNote, CareNote } from '@/common/CareNotes'
import { PageHeader } from '@/common/PageHeader'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_ADMISSION, DEMO_PREFERENCE_HISTORY, DEMO_RESIDENTS } from '@/common/demoData'
import { CompleteAdmissionSheet } from './CompleteAdmissionSheet'
import { OpinionHistorySheet, PreferenceList } from './OpinionHistorySheet'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { CreateResident } from './CreateResident'
import { ResidentSearch } from './ResidentSearch'
import { PROFILE_CREATORS } from './residentLabels'

// Bản mẫu bố cục danh sách hồ sơ người cao tuổi của feature 001, theo DESIGN.md 2.1.0
// (màn cần tìm, so sánh nên dùng bảng). Trường hiển thị, phạm vi xem và các lệnh đổi trạng thái
// do 001, 002 quy định; nút lệnh sẽ lấy theo danh sách lệnh được phép do backend trả.

const STATUS_FILTERS = ['Tất cả', 'Đang lưu trú', 'Tạm vắng', 'Điều trị tại bệnh viện', 'Đang tiếp nhận']

function Section({ title, children }) {
  return (
    <section className="flex flex-col gap-2">
      <h3 className="type-caption text-muted-foreground">{title}</h3>
      {children}
    </section>
  )
}

function TodayFigure({ value, label, urgent }) {
  return (
    <div className="flex flex-col rounded-lg bg-muted px-3 py-2">
      <span className={urgent ? 'type-title text-destructive tabular-nums' : 'type-title tabular-nums'}>{value}</span>
      <span className="type-caption text-muted-foreground">{label}</span>
    </div>
  )
}

// Ảnh nhận diện (5.1): dùng để xác định đúng người, không để trang trí. Chưa có ảnh thì hiện chữ cái
// và ghi rõ. Vai trò nào được xem ảnh (nhất là Người thân) chưa chốt; quyền thật do backend trả.
function ResidentPhoto({ resident }) {
  if (resident.photo) {
    return <img src={resident.photo} alt={`Ảnh ${resident.name}`} className="size-16 shrink-0 rounded-full object-cover" />
  }
  return (
    <div className="flex shrink-0 flex-col items-center gap-1">
      <ResidentAvatar
        name={resident.name}
        code={resident.code}
        initials={resident.initials}
        className="size-16 type-title"
      />
      <span className="type-caption text-muted-foreground">Chưa có ảnh</span>
    </div>
  )
}

// Về người này: nhu cầu chăm sóc (mục 4), phụ trách chính (8.4), sở thích hiện hành theo nhóm và nguồn
// (8.10, Q-363). Chỉ hiện dữ liệu đã ghi; không tính điểm hay tự suy ra (Q-306, Q-307).
// Mỗi nhóm có nút Lịch sử mở khung xem mục cũ (DESIGN.md 2.2, màn A).
function AboutPerson({ resident, onOpenHistory }) {
  const groups = Object.groupBy(resident.preferences, (p) => p.group)
  return (
    <dl className="flex flex-col gap-3 type-body-sm">
      <div className="flex flex-col gap-1">
        <dt className="text-muted-foreground">Nhu cầu chăm sóc</dt>
        <dd className="flex flex-wrap gap-1.5">
          {resident.careNeeds.length > 0
            ? resident.careNeeds.map((need) => (
                <span key={need} className="rounded-full bg-muted px-2.5 py-0.5 font-medium">
                  {need}
                </span>
              ))
            : 'Chưa có'}
        </dd>
      </div>
      <div className="flex flex-col gap-1">
        <dt className="text-muted-foreground">Nhân viên phụ trách chính</dt>
        <dd className="font-medium">{resident.primaryCaregiver ?? 'Chưa phân công'}</dd>
      </div>
      <div className="flex flex-col gap-1">
        <dt className="text-muted-foreground">Sở thích</dt>
        <dd>
          {resident.preferences.length === 0 ? (
            'Chưa ghi nhận'
          ) : (
            <ul className="flex flex-col gap-3">
              {Object.entries(groups).map(([group, items]) => (
                <li key={group} className="flex flex-col gap-1.5">
                  <span className="flex items-center justify-between gap-2">
                    <span className="text-muted-foreground">{group}</span>
                    <Button variant="ghost" size="sm" onClick={() => onOpenHistory(group)}>
                      Lịch sử
                    </Button>
                  </span>
                  <PreferenceList items={items} />
                </li>
              ))}
            </ul>
          )}
        </dd>
      </div>
    </dl>
  )
}

// Chi tiết mở bằng hộp ở giữa màn khi bấm một dòng (chủ dự án chọn 2026-10-26, DESIGN.md mục 5, ngoại lệ của
// khung chi tiết Sheet bên phải): bên trái hồ sơ, an toàn, hôm nay; bên phải về người này.
function ResidentDetailDialog({ resident, role, notice, onClose, onOpenHistory, onCompleteAdmission }) {
  return (
    <Dialog open={!!resident} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[85vh] gap-5 overflow-y-auto rounded-2xl p-6 [scrollbar-width:none] sm:max-w-3xl [&::-webkit-scrollbar]:hidden">
        {resident && (
          <DetailBody
            resident={resident}
            role={role}
            notice={notice}
            onOpenHistory={onOpenHistory}
            onCompleteAdmission={onCompleteAdmission}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function DetailBody({ resident, role, notice, onOpenHistory, onCompleteAdmission }) {
  return (
    <>
      <DialogHeader className="gap-3 pr-8">
        <div className="flex items-center gap-3">
          <ResidentPhoto resident={resident} />
          <div className="flex min-w-0 flex-col gap-0.5">
            <DialogTitle className="type-title font-semibold">{resident.name}</DialogTitle>
            <DialogDescription className="type-body-sm">
              {resident.code}, phòng {resident.room}
            </DialogDescription>
          </div>
        </div>
        <StatusBadge status={resident.status} />
      </DialogHeader>
      <Separator />
      <div className="grid gap-6 md:grid-cols-2">
        <div className="flex flex-col gap-4">
          <Section title="Hồ sơ">
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 type-body-sm">
              <div className="flex flex-col">
                <dt className="text-muted-foreground">Mức chăm sóc</dt>
                <dd className="font-medium">{resident.careLevel}</dd>
              </div>
              <div className="flex flex-col">
                <dt className="text-muted-foreground">Loại hình lưu trú</dt>
                <dd className="font-medium">{resident.stayType}</dd>
              </div>
            </dl>
          </Section>
          <Section title="An toàn">
            {resident.allergies.length === 0 && !resident.careNote ? (
              <p className="type-body-sm">Chưa ghi nhận dị ứng hay lưu ý chăm sóc.</p>
            ) : (
              <>
                <AllergyNote allergies={resident.allergies} />
                <CareNote note={resident.careNote} />
              </>
            )}
          </Section>
          <Section title="Hôm nay">
            <div className="grid grid-cols-3 gap-2">
              <TodayFigure value={resident.today.tasks} label="công việc" />
              <TodayFigure value={resident.today.overdue} label="quá hạn" urgent={resident.today.overdue > 0} />
              <TodayFigure value={resident.today.doses} label="liều thuốc" />
            </div>
          </Section>
        </div>
        <Section title="Về người này">
          <AboutPerson resident={resident} onOpenHistory={onOpenHistory} />
        </Section>
      </div>
      <Separator />
      <div className="flex flex-col gap-2">
        {/* Bản mẫu: Hoàn tất tiếp nhận do Hành chính thực hiện (Phụ lục 27); lệnh thật theo danh sách lệnh được phép */}
        {resident.status === 'Đang tiếp nhận' && role === 'OFFICE_STAFF' && (
          <Button className="h-11 self-start" onClick={onCompleteAdmission}>
            Hoàn tất tiếp nhận
          </Button>
        )}
        <p role="status" className="type-body-sm">
          {notice}
        </p>
        <p className="type-caption text-muted-foreground">
          Các lệnh (Cho tạm vắng, Ghi nhận trở về…) hiện theo danh sách lệnh được phép do backend trả.
        </p>
      </div>
    </>
  )
}

// Ghi sở thích: Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc trong phạm vi (8.10)
const PREFERENCE_RECORDERS = ['FLOOR_MANAGER', 'NURSING_STAFF', 'CAREGIVER']

export function ResidentListPage({ role }) {
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('Tất cả')
  const [selectedId, setSelectedId] = useState(null)
  const [historyGroup, setHistoryGroup] = useState(null)
  const [admitting, setAdmitting] = useState(false)
  const [notice, setNotice] = useState('')
  const [creating, setCreating] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  // Có phiên thật (GET /me như App) thì chỉ dữ liệu thật của 001; không có backend thì bản mẫu (team-plan L106).
  // null: chưa biết, chưa vẽ phần nào để không nháy dữ liệu mẫu
  const [live, setLive] = useState(null)

  useEffect(() => {
    let cancelled = false
    getMe()
      .then(() => !cancelled && setLive(true))
      .catch(() => !cancelled && setLive(false))
    return () => {
      cancelled = true
    }
  }, [])

  const q = query.trim().toLowerCase()
  const rows = DEMO_RESIDENTS.filter(
    (r) =>
      (statusFilter === 'Tất cả' || r.status === statusFilter) &&
      (!q || [r.name, r.code, r.room].some((v) => v.toLowerCase().includes(q))),
  )
  const selected = DEMO_RESIDENTS.find((r) => r.id === selectedId)
  const open = (id) => {
    setSelectedId(id)
    setNotice('')
  }
  const countOf = (s) => (s === 'Tất cả' ? DEMO_RESIDENTS.length : DEMO_RESIDENTS.filter((r) => r.status === s).length)

  const createButton = PROFILE_CREATORS.includes(role) && (
    <Button className="h-11" onClick={() => setCreating(true)}>
      Tạo hồ sơ
    </Button>
  )
  const createSheet = (
    <Sheet open={creating} onOpenChange={setCreating}>
      <SheetContent className="sm:max-w-lg">
        {creating && <CreateResident onCreated={() => setReloadKey((k) => k + 1)} onClose={() => setCreating(false)} />}
      </SheetContent>
    </Sheet>
  )

  if (live !== false) {
    return (
      <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
        <PageHeader title="Người cao tuổi" aside={createButton} />
        {live && (
          <>
            <ResidentSearch reloadKey={reloadKey} />
            {createSheet}
          </>
        )}
      </div>
    )
  }

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
      <PageHeader
        title="Người cao tuổi"
        subtitle={`${DEMO_RESIDENTS.length} hồ sơ trong phạm vi bạn được xem`}
        aside={createButton}
      >
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <InputGroup className="h-11 bg-popover lg:w-72 lg:shrink-0">
            <InputGroupAddon>
              <Search aria-hidden="true" />
            </InputGroupAddon>
            <InputGroupInput
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Họ tên, mã hoặc phòng"
              aria-label="Tìm người cao tuổi"
            />
          </InputGroup>
          <ToggleGroup
            variant="filter"
            size="lg"
            value={[statusFilter]}
            onValueChange={(next) => next[0] && setStatusFilter(next[0])}
            className="flex-wrap"
            aria-label="Lọc theo trạng thái"
          >
            {STATUS_FILTERS.map((s) => (
              <ToggleGroupItem key={s} value={s} className="h-11 px-4">
                {s} <span className="tabular-nums">{countOf(s)}</span>
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
        </div>
      </PageHeader>

      <ResidentSearch reloadKey={reloadKey} />

      {createSheet}

      <Card className="py-0">
        {rows.length === 0 ? (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Không có người cao tuổi khớp điều kiện</EmptyTitle>
              <EmptyDescription>Đổi từ khóa tìm hoặc chọn "Tất cả".</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="h-11 pl-4">Họ tên</TableHead>
                <TableHead>Phòng</TableHead>
                <TableHead>Mức chăm sóc</TableHead>
                <TableHead>Loại hình</TableHead>
                <TableHead className="pr-4">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow
                  key={r.id}
                  onClick={() => open(r.id)}
                  data-state={r.id === selectedId ? 'selected' : undefined}
                  className="cursor-pointer data-[state=selected]:bg-muted data-[state=selected]:shadow-[inset_3px_0_0_var(--primary)]"
                >
                  <TableCell className="py-3 pl-4">
                    {/* Nút tên để chọn dòng bằng bàn phím; bấm chuột vào cả dòng cũng chọn */}
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation()
                        open(r.id)
                      }}
                      aria-haspopup="dialog"
                      className="flex items-center gap-3 rounded-lg text-left"
                    >
                      <ResidentAvatar name={r.name} code={r.code} initials={r.initials} />
                      <span className="flex flex-col">
                        <span className="flex items-center gap-1.5 font-medium">
                          {r.name}
                          {r.openAlert && (
                            <TriangleAlert className="size-3.5 text-destructive" aria-label="Có cảnh báo đang mở" />
                          )}
                        </span>
                        <span className="type-caption text-muted-foreground">{r.code}</span>
                      </span>
                    </button>
                  </TableCell>
                  <TableCell className="tabular-nums">{r.room}</TableCell>
                  <TableCell>{r.careLevel}</TableCell>
                  <TableCell className="whitespace-normal text-muted-foreground">{r.stayType}</TableCell>
                  <TableCell className="pr-4">
                    <StatusBadge status={r.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Card>

      <ResidentDetailDialog
        resident={selected}
        role={role}
        notice={notice}
        onClose={() => setSelectedId(null)}
        onOpenHistory={setHistoryGroup}
        onCompleteAdmission={() => setAdmitting(true)}
      />

      {selected && (
        <>
          <OpinionHistorySheet
            group={historyGroup}
            current={selected.preferences.filter((p) => p.group === historyGroup)}
            history={(DEMO_PREFERENCE_HISTORY[selected.code] ?? []).filter((p) => p.group === historyGroup)}
            canRecord={PREFERENCE_RECORDERS.includes(role)}
            onOpenChange={(open) => !open && setHistoryGroup(null)}
          />
          <CompleteAdmissionSheet
            resident={selected}
            admission={DEMO_ADMISSION[selected.code]}
            open={admitting}
            onOpenChange={setAdmitting}
            onDone={() => {
              setAdmitting(false)
              setNotice('Bản mẫu: lệnh Hoàn tất tiếp nhận hợp lệ. Trạng thái thật do backend trả.')
            }}
          />
        </>
      )}
    </div>
  )
}
