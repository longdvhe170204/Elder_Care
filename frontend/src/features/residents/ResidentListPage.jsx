import { useState } from 'react'
import { Search, TriangleAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
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

function DetailPanel({ resident, role, notice, onOpenHistory, onCompleteAdmission }) {
  return (
    <Card className="h-fit xl:sticky xl:top-6">
      <CardHeader className="gap-3">
        <div className="flex items-center gap-3">
          <ResidentPhoto resident={resident} />
          <div className="flex min-w-0 flex-col gap-0.5">
            <CardTitle className="type-title font-semibold">{resident.name}</CardTitle>
            <span className="type-body-sm text-muted-foreground">
              {resident.code}, phòng {resident.room}
            </span>
          </div>
        </div>
        <StatusBadge status={resident.status} />
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <Separator />
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
        <Section title="Về người này">
          <AboutPerson resident={resident} onOpenHistory={onOpenHistory} />
        </Section>
        <Section title="Hôm nay">
          <div className="grid grid-cols-3 gap-2">
            <TodayFigure value={resident.today.tasks} label="công việc" />
            <TodayFigure value={resident.today.overdue} label="quá hạn" urgent={resident.today.overdue > 0} />
            <TodayFigure value={resident.today.doses} label="liều thuốc" />
          </div>
        </Section>
        <Separator />
        {/* Bản mẫu: Hoàn tất tiếp nhận do Hành chính thực hiện (Phụ lục 27); lệnh thật theo danh sách lệnh được phép */}
        {resident.status === 'Đang tiếp nhận' && role === 'OFFICE_STAFF' && (
          <Button className="h-11" onClick={onCompleteAdmission}>
            Hoàn tất tiếp nhận
          </Button>
        )}
        <p role="status" className="type-body-sm">
          {notice}
        </p>
        <p className="type-caption text-muted-foreground">
          Các lệnh (Cho tạm vắng, Ghi nhận trở về…) hiện theo danh sách lệnh được phép do backend trả.
        </p>
      </CardContent>
    </Card>
  )
}

// Ghi sở thích: Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc trong phạm vi (8.10)
const PREFERENCE_RECORDERS = ['FLOOR_MANAGER', 'NURSING_STAFF', 'CAREGIVER']

export function ResidentListPage({ role }) {
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('Tất cả')
  const [selectedId, setSelectedId] = useState(DEMO_RESIDENTS[0].id)
  const [historyGroup, setHistoryGroup] = useState(null)
  const [admitting, setAdmitting] = useState(false)
  const [notice, setNotice] = useState('')

  const q = query.trim().toLowerCase()
  const rows = DEMO_RESIDENTS.filter(
    (r) =>
      (statusFilter === 'Tất cả' || r.status === statusFilter) &&
      (!q || [r.name, r.code, r.room].some((v) => v.toLowerCase().includes(q))),
  )
  const selected = DEMO_RESIDENTS.find((r) => r.id === selectedId)
  const countOf = (s) => (s === 'Tất cả' ? DEMO_RESIDENTS.length : DEMO_RESIDENTS.filter((r) => r.status === s).length)

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
      <PageHeader title="Người cao tuổi" subtitle={`${DEMO_RESIDENTS.length} hồ sơ trong phạm vi bạn được xem`}>
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

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
        <Card className="h-fit py-0">
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
                    onClick={() => setSelectedId(r.id)}
                    data-state={r.id === selectedId ? 'selected' : undefined}
                    className="cursor-pointer data-[state=selected]:bg-muted data-[state=selected]:shadow-[inset_3px_0_0_var(--primary)]"
                  >
                    <TableCell className="py-3 pl-4">
                      {/* Nút tên để chọn dòng bằng bàn phím; bấm chuột vào cả dòng cũng chọn */}
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedId(r.id)
                          setNotice('')
                        }}
                        aria-pressed={r.id === selectedId}
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

        {selected && (
          <DetailPanel
            resident={selected}
            role={role}
            notice={notice}
            onOpenHistory={setHistoryGroup}
            onCompleteAdmission={() => setAdmitting(true)}
          />
        )}
      </div>

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
