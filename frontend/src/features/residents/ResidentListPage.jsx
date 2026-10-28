import { useCallback, useEffect, useState } from 'react'
import { Search } from 'lucide-react'
import { getReassessmentsDue, getResidents, getResidentsById } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { InputGroup, InputGroupAddon, InputGroupInput } from '@/components/ui/input-group'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { cn } from '@/lib/utils'
import { CARE_LEVEL_LABELS, RESIDENCE_TYPE_LABELS } from '../assessments/assessmentLabels'
import { CreateResident } from './CreateResident'
import { ResidentDetail } from './ResidentDetail'
import { PROFILE_CREATORS, STATUS_LABELS } from './residentLabels'

// Màn Người cao tuổi (001 T022, T066; DESIGN.md 5, 5.1, 6.7.0): một ô tìm (tham số q của GET /residents), thanh lọc
// trạng thái (tham số status), bảng chỉ có trường của Profile trong api-001; bấm tên mở hồ sơ trong khung chi tiết.
// Kết quả đã lọc theo phạm vi ở backend; vai trò không được xem nhận 403 và màn hiện lỗi đó (FR-007).
// Đánh giá lại (T066; US6; FR-041): cột Đánh giá lại và bộ lọc "Cần đánh giá lại n" lấy từ GET /reassessments/due;
// vai trò không xem được đánh giá (403) thì không có cột và bộ lọc. Hợp đồng không trả số đếm theo trạng thái nên
// các mục lọc trạng thái không có số.

const STATUS_FILTERS = ['ADMISSION_IN_PROGRESS', 'IN_RESIDENCE', 'TEMPORARILY_ABSENT', 'HOSPITALIZED']
const DUE_FILTER = 'DUE'

function ReassessmentCell({ item }) {
  if (!item) return null
  return (
    <div className="flex flex-col items-start gap-1">
      {item.status === 'OVERDUE' ? (
        <StatusBadge status="Quá hạn" />
      ) : (
        <Badge variant="attention">Cần đánh giá lại</Badge>
      )}
      <span className="type-caption text-muted-foreground tabular-nums">
        {item.status === 'OVERDUE'
          ? `từ ${formatDateTime(item.overdueSince)}`
          : item.dueAt
            ? `Hạn ${formatDateTime(item.dueAt)}`
            : 'Hạn tính từ lúc Ghi nhận trở về'}
      </span>
    </div>
  )
}

export function ResidentListPage({ role }) {
  const [text, setText] = useState('')
  const [applied, setApplied] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [items, setItems] = useState(null)
  const [nextCursor, setNextCursor] = useState(null)
  const [failure, setFailure] = useState(null)
  const [due, setDue] = useState(null)
  const [dueProfiles, setDueProfiles] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [creating, setCreating] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const status = STATUS_FILTERS.includes(filter) ? filter : undefined
  const onlyDue = filter === DUE_FILTER

  const load = useCallback(
    async (cursor) => {
      try {
        const { data } = await getResidents({ query: { q: applied || undefined, status, cursor } })
        setFailure(null)
        setItems((current) => (cursor && current ? [...current, ...data.items] : data.items))
        setNextCursor(data.nextCursor)
      } catch (error) {
        setItems([])
        setFailure(problemOf(error))
      }
    },
    [applied, status],
  )

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp danh sách khi vào màn, đổi từ khóa, đổi bộ lọc hoặc vừa tạo hồ sơ
    load()
  }, [load, reloadKey])

  useEffect(() => {
    let cancelled = false
    getReassessmentsDue()
      .then(({ data }) => !cancelled && setDue(data))
      .catch(() => !cancelled && setDue(null))
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  useEffect(() => {
    if (!onlyDue || !due) return undefined
    let cancelled = false
    Promise.allSettled(due.map((d) => getResidentsById({ path: { id: d.residentId } }))).then((results) => {
      if (!cancelled) setDueProfiles(results.filter((r) => r.status === 'fulfilled').map((r) => r.value.data))
    })
    return () => {
      cancelled = true
    }
  }, [onlyDue, due])

  const dueById = new Map((due ?? []).map((d) => [d.residentId, d]))
  // Bộ lọc Cần đánh giá lại: chỉ những người đó, theo thứ tự backend trả (quá hạn trước, rồi theo hạn)
  const rows = onlyDue ? (dueProfiles ?? []) : items

  function submit(event) {
    event.preventDefault()
    setApplied(text.trim())
  }

  return (
    <DetailLayout
      open={selectedId !== null}
      detailKey={selectedId}
      onClose={() => setSelectedId(null)}
      label="Hồ sơ người cao tuổi"
      renderDetail={(titles) => (
        <ResidentDetail key={selectedId} residentId={selectedId} {...titles} onChanged={() => setReloadKey((k) => k + 1)} />
      )}
    >
      <PageHeader
        title="Người cao tuổi"
        subtitle="Hồ sơ trong phạm vi bạn được xem"
        aside={
          PROFILE_CREATORS.includes(role) && (
            <Button className="h-11" onClick={() => setCreating(true)}>
              Tạo hồ sơ
            </Button>
          )
        }
      >
        <form role="search" onSubmit={submit} className="flex flex-col gap-3 sm:flex-row">
          <InputGroup className="h-11 bg-popover sm:max-w-sm">
            <InputGroupAddon>
              <Search aria-hidden="true" />
            </InputGroupAddon>
            <InputGroupInput
              type="search"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Họ tên, mã hồ sơ hoặc số giấy tờ"
              aria-label="Tìm hồ sơ"
            />
          </InputGroup>
          <Button type="submit" variant="outline" className="h-11">
            Tìm
          </Button>
        </form>
        <ToggleGroup
          variant="filter"
          value={[filter]}
          onValueChange={(next) => next[0] && setFilter(next[0])}
          aria-label="Lọc hồ sơ"
        >
          <ToggleGroupItem value="ALL" className="h-11">
            Tất cả
          </ToggleGroupItem>
          {STATUS_FILTERS.map((s) => (
            <ToggleGroupItem key={s} value={s} className="h-11">
              {STATUS_LABELS[s]}
            </ToggleGroupItem>
          ))}
          {due && (
            <ToggleGroupItem value={DUE_FILTER} className="h-11">
              Cần đánh giá lại <span className="tabular-nums">{due.length}</span>
            </ToggleGroupItem>
          )}
        </ToggleGroup>
      </PageHeader>

      <ProblemAlert problem={failure} />

      <Card className="@container py-0">
        {onlyDue && due?.length === 0 ? (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Chưa có ai cần đánh giá lại</EmptyTitle>
              <EmptyDescription>
                Hệ thống báo khi tới hạn định kỳ, sau sự cố, sau nằm viện hoặc sau đính chính đổi mức.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : rows?.length === 0 && !failure ? (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Không có hồ sơ khớp điều kiện</EmptyTitle>
              <EmptyDescription>Đổi từ khóa tìm, chọn "Tất cả" hoặc dùng nút "Tạo hồ sơ".</EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : (
          rows?.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="h-11 pl-4">Người cao tuổi</TableHead>
                  <TableHead className="hidden @4xl:table-cell">Ngày sinh</TableHead>
                  <TableHead className="hidden @xl:table-cell">Mức chăm sóc</TableHead>
                  <TableHead className="hidden @4xl:table-cell">Loại hình</TableHead>
                  <TableHead className={due ? undefined : 'pr-4'}>Trạng thái</TableHead>
                  {due && <TableHead className="hidden pr-4 @xl:table-cell">Đánh giá lại</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((p) => (
                  <TableRow
                    key={p.id}
                    onClick={() => setSelectedId(p.id)}
                    data-state={p.id === selectedId ? 'selected' : undefined}
                    className="cursor-pointer data-[state=selected]:bg-accent"
                  >
                    <TableCell className="py-3 pl-4 whitespace-normal">
                      {/* Nút tên để mở hồ sơ bằng bàn phím; bấm chuột vào cả dòng cũng mở */}
                      <button
                        type="button"
                        aria-label={`${p.personalInfo.fullName}, ${p.profileCode}`}
                        aria-pressed={p.id === selectedId}
                        onClick={(e) => {
                          e.stopPropagation()
                          setSelectedId(p.id)
                        }}
                        className="flex items-center gap-3 rounded-lg text-left"
                      >
                        <ResidentAvatar name={p.personalInfo.fullName} code={String(p.id)} />
                        <span className="flex flex-col">
                          <span className="type-label">{p.personalInfo.fullName}</span>
                          <span className="type-caption text-muted-foreground tabular-nums">{p.profileCode}</span>
                          {p.currentCareLevel && (
                            <span className="type-body-sm text-muted-foreground @xl:hidden">
                              {CARE_LEVEL_LABELS[p.currentCareLevel]}
                            </span>
                          )}
                        </span>
                      </button>
                    </TableCell>
                    <TableCell className="hidden tabular-nums @4xl:table-cell">
                      {formatDate(p.personalInfo.dateOfBirth)}
                    </TableCell>
                    <TableCell className="hidden @xl:table-cell">
                      {CARE_LEVEL_LABELS[p.currentCareLevel] ?? 'Chưa có'}
                    </TableCell>
                    <TableCell className="hidden whitespace-normal text-muted-foreground @4xl:table-cell">
                      {RESIDENCE_TYPE_LABELS[p.currentResidenceType] ?? 'Chưa có hợp đồng'}
                    </TableCell>
                    <TableCell className={cn('whitespace-normal', !due && 'pr-4')}>
                      <StatusBadge status={STATUS_LABELS[p.status] ?? p.status} />
                    </TableCell>
                    {due && (
                      <TableCell className="hidden pr-4 @xl:table-cell">
                        <ReassessmentCell item={dueById.get(p.id)} />
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )
        )}
      </Card>
      {!onlyDue && nextCursor && (
        <Button variant="outline" className="h-11 self-start" onClick={() => load(nextCursor)}>
          Xem thêm
        </Button>
      )}

      <Sheet open={creating} onOpenChange={setCreating}>
        <SheetContent className="sm:max-w-lg">
          {creating && (
            <CreateResident onCreated={() => setReloadKey((k) => k + 1)} onClose={() => setCreating(false)} />
          )}
        </SheetContent>
      </Sheet>
    </DetailLayout>
  )
}
