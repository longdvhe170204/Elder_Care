import { useEffect, useState } from 'react'
import { getBedAllocations, getBedsSummary, getResidents } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { BedAllocationForm } from './BedAllocationForm'

const statuses = { FUTURE: 'Tương lai', ACTIVE: 'Đang hiệu lực', CLOSED: 'Đã đóng', CANCELLED: 'Đã hủy' }

export function BedAllocationsPanel({ canCreate }) {
  const [board, setBoard] = useState({ loading: true, allocations: [] })
  const [revision, setRevision] = useState(0)
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState({ items: [], loading: false })
  const [selected, setSelected] = useState(null)
  const [notice, setNotice] = useState(null)
  useEffect(() => {
    let current = true
    Promise.all([getBedsSummary(), getBedAllocations()])
      .then(
        ([summary, allocations]) =>
          current && setBoard({ loading: false, summary: summary.data, allocations: allocations.data }),
      )
      .catch((cause) => current && setBoard({ loading: false, allocations: [], error: problemOf(cause) }))
    return () => {
      current = false
    }
  }, [revision])
  async function find(event) {
    event.preventDefault()
    setSelected(null)
    setSearch({ items: [], loading: true })
    try {
      const { data } = await getResidents({
        query: { q: query.trim(), status: ['ADMISSION_IN_PROGRESS', 'IN_RESIDENCE'] },
      })
      setSearch({ items: data.items, loading: false })
    } catch (cause) {
      setSearch({ items: [], loading: false, error: problemOf(cause) })
    }
  }
  return (
    <section className="flex flex-col gap-4" aria-label="Bảng phân bổ giường">
      {notice && <p role="status">{notice}</p>}
      {board.loading && <p aria-busy="true">Đang tải bảng giường…</p>}
      {board.error && (
        <ProblemAlert problem={board.error}>
          <Button variant="outline" onClick={() => setRevision((value) => value + 1)}>
            Thử lại
          </Button>
        </ProblemAlert>
      )}
      {board.summary && (
        <div className="flex flex-wrap gap-4 rounded-xl border bg-card p-4">
          <p>Sức chứa sử dụng: {board.summary.usable}</p>
          <p>Giường trống phù hợp: {board.summary.emptyMatching}</p>
        </div>
      )}
      {canCreate && (
        <>
          <form onSubmit={find} className="flex flex-wrap items-end gap-3">
            <label className="flex flex-col gap-2">
              Tìm người cao tuổi
              <Input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Họ tên hoặc mã hồ sơ"
              />
            </label>
            <Button type="submit" disabled={search.loading || !query.trim()}>
              Tìm hồ sơ
            </Button>
          </form>
          {search.error && <ProblemAlert problem={search.error} />}
          {search.items.map((resident) => (
            <div className="flex items-center justify-between gap-3 rounded-xl border bg-card p-4" key={resident.id}>
              <span>
                {resident.fullName} ({resident.profileCode})
              </span>
              <Button
                variant="outline"
                aria-label={`Chọn ${resident.profileCode}`}
                onClick={() => setSelected(resident)}
              >
                Chọn
              </Button>
            </div>
          ))}
          {selected && (
            <BedAllocationForm
              key={selected.id}
              resident={selected}
              onSaved={(result) => {
                setSelected(null)
                setNotice(result.status === 'FUTURE' ? 'Đã đặt phân bổ tương lai.' : 'Đã phân bổ giường.')
                setRevision((value) => value + 1)
              }}
            />
          )}
        </>
      )}
      <div className="overflow-x-auto rounded-xl border bg-card">
        <table className="w-full text-left">
          <caption className="p-4 text-left type-title-sm">Phân bổ trong phạm vi được xem</caption>
          <thead>
            <tr>
              <th className="p-3">Hồ sơ</th>
              <th className="p-3">Vị trí</th>
              <th className="p-3">Trạng thái</th>
              <th className="p-3">Bắt đầu</th>
            </tr>
          </thead>
          <tbody>
            {board.allocations.map((row) => (
              <tr key={row.id} className="border-t">
                <td className="p-3">#{row.residentId}</td>
                <td className="p-3">{row.location.label}</td>
                <td className="p-3">{statuses[row.status]}</td>
                <td className="p-3">{formatDateTime(row.startAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
