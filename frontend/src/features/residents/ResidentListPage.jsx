import { useState } from 'react'
import { Search, TriangleAlert } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { StatusBadge } from '@/common/StatusBadge'
import { DEMO_RESIDENTS } from '@/common/demoData'
import { cn } from '@/lib/utils'

// Bản mẫu bố cục cho danh sách hồ sơ người cao tuổi của feature 001. Trường hiển thị, phạm vi
// xem và các lệnh đổi trạng thái do 001, 002 quy định; nút lệnh sẽ lấy theo danh sách lệnh
// được phép do backend trả, màn mẫu không tự suy ra.

const STATUS_FILTERS = ['Tất cả', 'Đang lưu trú', 'Tạm vắng', 'Điều trị tại bệnh viện', 'Đang tiếp nhận']

export function ResidentListPage() {
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('Tất cả')
  const [selectedId, setSelectedId] = useState(DEMO_RESIDENTS[0].id)

  const q = query.trim().toLowerCase()
  const rows = DEMO_RESIDENTS.filter(
    (r) =>
      (statusFilter === 'Tất cả' || r.status === statusFilter) &&
      (!q || [r.name, r.code, r.room].some((v) => v.toLowerCase().includes(q))),
  )
  const selected = DEMO_RESIDENTS.find((r) => r.id === selectedId)

  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
      <div className="flex min-w-0 flex-col gap-4">
        <h1 className="text-lg font-medium">Người cao tuổi</h1>
        <div className="flex flex-wrap items-center gap-2">
          <div className="relative min-w-56 flex-1">
            <Search className="absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Họ tên, mã hoặc phòng"
              className="pl-8"
              aria-label="Tìm người cao tuổi"
            />
          </div>
          <div className="flex flex-wrap gap-1">
            {STATUS_FILTERS.map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => setStatusFilter(s)}
                aria-pressed={statusFilter === s}
                className={cn(
                  'rounded-full border px-3 py-1 text-xs text-muted-foreground hover:bg-muted',
                  statusFilter === s && 'border-primary bg-accent text-accent-foreground',
                )}
              >
                {s}
              </button>
            ))}
          </div>
        </div>

        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-20">Phòng</TableHead>
                <TableHead>Họ tên</TableHead>
                <TableHead>Mức chăm sóc</TableHead>
                <TableHead>Loại hình</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow
                  key={r.id}
                  onClick={() => setSelectedId(r.id)}
                  data-state={r.id === selectedId ? 'selected' : undefined}
                  className="cursor-pointer data-[state=selected]:bg-accent"
                >
                  <TableCell className="tabular-nums">{r.room}</TableCell>
                  <TableCell>
                    <span className="font-medium">{r.name}</span>
                    {r.openAlert && (
                      <TriangleAlert className="ml-2 inline size-3.5 text-destructive" aria-label="Có cảnh báo đang mở" />
                    )}
                    <div className="text-xs text-muted-foreground">{r.code}</div>
                  </TableCell>
                  <TableCell>{r.careLevel}</TableCell>
                  <TableCell className="text-muted-foreground">{r.stayType}</TableCell>
                  <TableCell>
                    <StatusBadge status={r.status} />
                  </TableCell>
                </TableRow>
              ))}
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="py-8 text-center text-muted-foreground">
                    Không có người cao tuổi khớp điều kiện lọc
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      {selected && (
        <aside className="h-fit rounded-lg border bg-card p-4 text-sm xl:sticky xl:top-5">
          <div className="text-base font-medium">{selected.name}</div>
          <div className="mb-3 text-muted-foreground">
            {selected.code} · {selected.room} · {selected.stayType}
          </div>
          <StatusBadge status={selected.status} />

          {selected.allergies.length > 0 && (
            <div className="mt-4 rounded-md bg-warning px-3 py-2 text-warning-foreground">
              Dị ứng: {selected.allergies.join(', ')}
            </div>
          )}

          <dl className="mt-4 flex flex-col gap-3">
            <div>
              <dt className="text-xs text-muted-foreground">Mức chăm sóc</dt>
              <dd>{selected.careLevel}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Lưu ý chăm sóc</dt>
              <dd>{selected.careNote || '—'}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Hôm nay</dt>
              <dd className="tabular-nums">
                {selected.today.tasks} công việc · {selected.today.overdue} quá hạn · {selected.today.doses} liều thuốc
              </dd>
            </div>
          </dl>

          <div className="mt-4 border-t pt-3 text-xs text-muted-foreground">
            Các lệnh (Cho tạm vắng, Ghi nhận trở về…) hiện theo danh sách lệnh được phép do backend trả.
          </div>
        </aside>
      )}
    </div>
  )
}
