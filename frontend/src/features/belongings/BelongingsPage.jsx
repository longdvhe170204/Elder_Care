import { useCallback, useEffect, useState } from 'react'
import { getResidents, getResidentsByResidentIdBelongings } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { BelongingDetail } from './BelongingDetail'
import { HeldBelongings } from './HeldBelongings'
import { IntakeForm } from './IntakeForm'
import { amountText, holderLabel, statusLabel } from './belongingLabels'
import { useBelongingCatalogs } from './useBelongingCatalogs'

// Màn Đồ gửi (013; Phụ lục 27 dòng "Đồ gửi": Hành chính, Điều dưỡng thực hiện, Quản lý viện, Trưởng tầng xem). Chọn
// người cao tuổi (tìm qua GET /residents của 001), bảng đồ gửi theo api-013 GET /residents/{residentId}/belongings, bấm
// đồ mở khung chi tiết (DESIGN.md mục 5). Nút "Tiếp nhận đồ" mở form trong Sheet (T011).

function ResidentPicker({ onPick }) {
  const [q, setQ] = useState('')
  const [state, setState] = useState({ status: 'idle', items: [] })

  async function search(event) {
    event.preventDefault()
    setState({ status: 'loading', items: [] })
    try {
      const { data } = await getResidents({ query: { q: q.trim() || undefined } })
      setState({ status: 'ready', items: data.items })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <form role="search" className="flex flex-wrap items-center gap-2" onSubmit={search}>
        <Input
          aria-label="Tìm người cao tuổi"
          placeholder="Họ tên hoặc mã hồ sơ"
          className="h-11 w-72"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <Button type="submit" variant="outline" className="h-11">
          Tìm
        </Button>
      </form>
      {state.status === 'error' && <ProblemAlert problem={state.problem} />}
      {state.status === 'ready' && state.items.length === 0 && (
        <p className="type-body-sm text-muted-foreground">
          Không có người cao tuổi khớp với từ tìm trong phạm vi của bạn.
        </p>
      )}
      {state.items.length > 0 && (
        <ul className="flex flex-wrap gap-2">
          {state.items.map((r) => (
            <li key={r.id}>
              <Button
                variant="outline"
                className="h-11 gap-2"
                aria-label={`${r.personalInfo.fullName}, ${r.profileCode}`}
                onClick={() => {
                  onPick(r)
                  setState({ status: 'idle', items: [] })
                }}
              >
                <ResidentAvatar name={r.personalInfo.fullName} code={String(r.id)} />
                <span className="type-label">{r.personalInfo.fullName}</span>
                <span className="type-caption text-muted-foreground">{r.profileCode}</span>
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function BelongingTable({ items, catalogs, opened, onOpen }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Đồ gửi</TableHead>
          <TableHead>Loại</TableHead>
          <TableHead>Số lượng, số tiền</TableHead>
          <TableHead>Trạng thái</TableHead>
          <TableHead>Người giữ</TableHead>
          <TableHead>Vị trí</TableHead>
          <TableHead>Bàn giao gần nhất</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {items.map((b) => (
          <TableRow
            key={b.id}
            data-state={b.id === opened ? 'selected' : undefined}
            className="align-top data-[state=selected]:bg-accent"
          >
            <TableCell className="whitespace-normal">
              <Button
                variant="link"
                className="h-auto p-0 text-left"
                aria-label={`Chi tiết ${b.description}`}
                aria-pressed={b.id === opened}
                onClick={() => onOpen(b.id)}
              >
                {b.description}
              </Button>
            </TableCell>
            <TableCell>{catalogs.typeName(b.typeId)}</TableCell>
            <TableCell className="tabular-nums">{amountText(b)}</TableCell>
            <TableCell>
              <StatusBadge status={statusLabel(b.status)} />
            </TableCell>
            <TableCell>{holderLabel(b.holder)}</TableCell>
            <TableCell>{catalogs.locationName(b.storageLocationId)}</TableCell>
            <TableCell className="tabular-nums">{formatDateTime(b.lastHandoverAt)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

function ResidentBelongings({ resident, catalogs }) {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [opened, setOpened] = useState(null)
  const [intake, setIntake] = useState(false)
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByResidentIdBelongings({ path: { residentId: resident.id } })
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [resident.id])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải đồ gửi khi chọn người cao tuổi
    load()
  }, [load])

  const name = resident.personalInfo.fullName
  return (
    <DetailLayout
      open={opened != null}
      detailKey={opened}
      onClose={() => setOpened(null)}
      label="Chi tiết đồ gửi"
      renderDetail={({ Title, Description }) => (
        <BelongingDetail
          key={opened}
          belongingId={opened}
          catalogs={catalogs}
          onChanged={load}
          Title={Title}
          Description={Description}
        />
      )}
    >
      <Card>
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="type-title">Đồ gửi của {name}</h2>
            {can('BELONGINGS', 'CREATE') && (
              <Button className="h-11" onClick={() => setIntake(true)}>
                Tiếp nhận đồ
              </Button>
            )}
          </div>
          <div role="status" className="type-body-sm text-success-foreground">
            {notice}
          </div>
          {state.status === 'loading' && <Skeleton className="h-24 w-full" />}
          {state.status === 'error' && <ProblemAlert problem={state.problem} />}
          {state.status === 'ready' && state.items.length === 0 && (
            <Empty>
              <EmptyHeader>
                <EmptyTitle>Chưa có đồ gửi</EmptyTitle>
                <EmptyDescription>Đồ gia đình gửi viện giữ hộ sẽ hiện ở đây sau khi được tiếp nhận.</EmptyDescription>
              </EmptyHeader>
            </Empty>
          )}
          {state.status === 'ready' && state.items.length > 0 && (
            <BelongingTable items={state.items} catalogs={catalogs} opened={opened} onOpen={setOpened} />
          )}
        </CardContent>
      </Card>
      <Sheet open={intake} onOpenChange={setIntake}>
        <SheetContent className="overflow-y-auto p-6 sm:max-w-lg [&_[data-slot=sheet-title]]:pr-8">
          <SheetHeader className="p-0">
            <SheetTitle className="type-title">Tiếp nhận đồ gửi</SheetTitle>
            <SheetDescription>Của {name}</SheetDescription>
          </SheetHeader>
          <IntakeForm
            residentId={resident.id}
            types={catalogs.types}
            levels={catalogs.levels}
            locations={catalogs.locations}
            onDone={() => {
              setIntake(false)
              setNotice('Đã tiếp nhận đồ gửi.')
              load()
            }}
          />
        </SheetContent>
      </Sheet>
    </DetailLayout>
  )
}

// Tab của màn (thanh lọc DESIGN.md 7.2 filter-bar); tab sau thêm theo task 013
const TABS = [
  { value: 'resident', label: 'Theo người cao tuổi' },
  { value: 'held', label: 'Nhân viên đang giữ' },
]

export function BelongingsPage() {
  const catalogs = useBelongingCatalogs()
  const [resident, setResident] = useState(null)
  const [tab, setTab] = useState('resident')

  return (
    <div className="flex flex-col gap-5">
      <PageHeader title="Đồ gửi" subtitle="Tiếp nhận, bàn giao và trả đồ gia đình gửi viện giữ hộ.">
        <ToggleGroup variant="filter" value={[tab]} onValueChange={(next) => next[0] && setTab(next[0])}>
          {TABS.map((t) => (
            <ToggleGroupItem key={t.value} value={t.value}>
              {t.label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        {tab === 'resident' && <ResidentPicker onPick={setResident} />}
      </PageHeader>
      {catalogs.status === 'error' && (
        <ProblemAlert
          problem={{
            title: 'Không tải được danh mục đồ gửi',
            detail: 'Tải lại trang để thử lại.',
            unmetConditions: [],
          }}
        />
      )}
      {tab === 'resident' && !resident && (
        <Card>
          <CardContent>
            <p className="text-muted-foreground">Tìm và chọn một người cao tuổi để xem đồ gửi.</p>
          </CardContent>
        </Card>
      )}
      {tab === 'resident' && resident && (
        <ResidentBelongings key={resident.id} resident={resident} catalogs={catalogs} />
      )}
      {tab === 'held' && <HeldBelongings catalogs={catalogs} />}
    </div>
  )
}
