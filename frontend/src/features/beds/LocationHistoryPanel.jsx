import { useRef, useState } from 'react'
import {
  getResidents,
  getLocationHistoryResidentsByResidentId,
  getLocationHistoryBeds,
  getLocationHistoryRoommates,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { PermissionDeniedState } from '@/common/PageState'
import { CorrectionBadge } from '@/common/correction/CorrectionBadge'
import { CorrectionHistorySheet } from '@/common/correction/CorrectionHistorySheet'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { AllocationCorrectionForm } from './AllocationCorrectionForm'

const absenceLabels = {
  TEMPORARILY_ABSENT: 'Tạm vắng',
  HOSPITALIZED: 'Điều trị tại bệnh viện',
  OFF_SITE_ACTIVITY: 'Hoạt động bên ngoài',
}
const allocationFields = [
  { name: 'bedId', label: 'Giường' },
  { name: 'startAt', label: 'Bắt đầu' },
  { name: 'endAt', label: 'Kết thúc' },
]
const iso = (value) => `${value.length === 16 ? `${value}:00` : value}+07:00`
const interval = (from, to) => `${formatDateTime(from)} – ${to ? formatDateTime(to) : 'Chưa kết thúc'}`

// T048: ba cách tra cứu dùng chung bộ lọc; kết quả cũ bỏ ngay khi đổi điều kiện hoặc truy vấn lại.
export function LocationHistoryPanel({ catalogs = {}, canCorrectAllocation = false }) {
  const [mode, setMode] = useState('resident')
  const [timeMode, setTimeMode] = useState('interval')
  const [query, setQuery] = useState('')
  const [search, setSearch] = useState({ items: [] })
  const [selected, setSelected] = useState(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [at, setAt] = useState('')
  const [date, setDate] = useState('')
  const [locationType, setLocationType] = useState('roomId')
  const [locationId, setLocationId] = useState('')
  const [state, setState] = useState({ status: 'idle', rows: [] })
  const [chain, setChain] = useState(null)
  const [correcting, setCorrecting] = useState(null)
  const version = useRef(0)
  function clear() {
    version.current += 1
    setState({ status: 'idle', rows: [] })
  }
  function change(setter, value) {
    setter(value)
    clear()
  }
  async function find(event) {
    event.preventDefault()
    change(setSelected, null)
    setSearch({ items: [], loading: true })
    const current = version.current
    try {
      const { data } = await getResidents({ query: { q: query.trim() } })
      if (current === version.current) setSearch({ items: data.items, loading: false })
    } catch (cause) {
      if (current === version.current) setSearch({ items: [], error: problemOf(cause) })
    }
  }
  async function lookup(event) {
    event.preventDefault()
    const current = ++version.current
    setState({ status: 'loading', rows: [] })
    try {
      let result
      if (mode === 'beds')
        result = await getLocationHistoryBeds({
          query: { [locationType]: Number(locationId), ...(timeMode === 'point' ? { at: iso(at) } : { date }) },
        })
      else if (mode === 'roommates')
        result = await getLocationHistoryRoommates({ query: { residentId: selected.id, from: iso(from), to: iso(to) } })
      else
        result = await getLocationHistoryResidentsByResidentId({
          path: { residentId: selected.id },
          query: timeMode === 'point' ? { at: iso(at) } : { from: iso(from), to: iso(to) },
        })
      if (current === version.current) setState({ status: 'ready', rows: result.data })
    } catch (cause) {
      if (current === version.current) setState({ status: 'error', rows: [], problem: problemOf(cause) })
    }
  }
  const canLookup =
    mode === 'beds'
      ? locationId && (timeMode === 'point' ? at : date)
      : selected && (mode !== 'roommates' && timeMode === 'point' ? at : from && to && from < to)
  const options = catalogs[{ roomId: 'rooms', floorId: 'floors', areaId: 'areas' }[locationType]] ?? []
  return (
    <section aria-label="Lịch sử vị trí" className="flex flex-col gap-4 rounded-xl border bg-card p-4">
      <h2 className="type-title">Lịch sử vị trí</h2>
      <label className="flex flex-col gap-2">
        Tra cứu theo
        <select
          className="h-11 rounded-md border bg-background px-3"
          value={mode}
          onChange={(event) => {
            change(setMode, event.target.value)
            setTimeMode('interval')
          }}
        >
          <option value="resident">Người cao tuổi</option>
          <option value="beds">Giường trong phòng, tầng, khu vực</option>
          <option value="roommates">Người cùng phòng</option>
        </select>
      </label>
      {mode !== 'beds' && (
        <>
          <form onSubmit={find} className="flex flex-wrap items-end gap-3">
            <label className="flex grow flex-col gap-2">
              Tìm người cao tuổi
              <Input
                value={query}
                onChange={(event) => {
                  change(setQuery, event.target.value)
                  setSelected(null)
                  setSearch({ items: [] })
                }}
                required
              />
            </label>
            <Button type="submit" variant="outline" disabled={search.loading || !query.trim()}>
              Tìm hồ sơ
            </Button>
          </form>
          {search.error && <ProblemAlert problem={search.error} />}
          <div className="flex flex-wrap gap-2">
            {search.items.map((person) => (
              <Button
                key={person.id}
                variant={selected?.id === person.id ? 'secondary' : 'outline'}
                aria-label={`Chọn ${person.profileCode}`}
                onClick={() => change(setSelected, person)}
              >
                {person.fullName} ({person.profileCode})
              </Button>
            ))}
          </div>
          {selected && <p>Đang tra cứu: {selected.fullName}</p>}
          {search.items.length === 0 && search.loading === false && <p>Không có hồ sơ phù hợp trong phạm vi.</p>}
        </>
      )}
      <form onSubmit={lookup} className="flex flex-wrap items-end gap-3">
        {mode === 'beds' && (
          <>
            <label className="flex flex-col gap-2">
              Loại vị trí
              <select
                className="h-11 rounded-md border bg-background px-3"
                value={locationType}
                onChange={(event) => {
                  change(setLocationType, event.target.value)
                  setLocationId('')
                }}
              >
                <option value="roomId">Phòng</option>
                <option value="floorId">Tầng</option>
                <option value="areaId">Khu vực</option>
              </select>
            </label>
            <label className="flex flex-col gap-2">
              Vị trí
              {options.length ? (
                <select
                  className="h-11 rounded-md border bg-background px-3"
                  value={locationId}
                  onChange={(event) => change(setLocationId, event.target.value)}
                  required
                >
                  <option value="">Chọn vị trí</option>
                  {options.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name} ({item.code})
                    </option>
                  ))}
                </select>
              ) : (
                <Input
                  type="number"
                  min="1"
                  placeholder="Mã số vị trí trong hồ sơ giường"
                  value={locationId}
                  onChange={(event) => change(setLocationId, event.target.value)}
                  required
                />
              )}
            </label>
          </>
        )}
        {mode !== 'roommates' && (
          <label className="flex flex-col gap-2">
            Mốc tra cứu
            <select
              className="h-11 rounded-md border bg-background px-3"
              value={timeMode}
              onChange={(event) => change(setTimeMode, event.target.value)}
            >
              <option value="interval">{mode === 'beds' ? 'Trong ngày' : 'Trong khoảng'}</option>
              <option value="point">Tại thời điểm</option>
            </select>
          </label>
        )}
        {mode !== 'roommates' && timeMode === 'point' ? (
          <label className="flex flex-col gap-2">
            Thời điểm
            <Input type="datetime-local" value={at} onChange={(event) => change(setAt, event.target.value)} required />
          </label>
        ) : mode === 'beds' ? (
          <label className="flex flex-col gap-2">
            Ngày
            <Input type="date" value={date} onChange={(event) => change(setDate, event.target.value)} required />
          </label>
        ) : (
          <>
            <label className="flex flex-col gap-2">
              Từ
              <Input
                type="datetime-local"
                value={from}
                onChange={(event) => change(setFrom, event.target.value)}
                required
              />
            </label>
            <label className="flex flex-col gap-2">
              Đến
              <Input
                type="datetime-local"
                value={to}
                min={from}
                onChange={(event) => change(setTo, event.target.value)}
                required
              />
            </label>
          </>
        )}
        <Button type="submit" disabled={!canLookup || state.status === 'loading'}>
          Tra cứu
        </Button>
      </form>
      {state.status === 'loading' && (
        <div aria-busy="true">
          <span className="sr-only">Đang tra cứu</span>
          <Skeleton className="h-24" />
        </div>
      )}
      {state.problem?.status === 403 ? (
        <PermissionDeniedState label="lịch sử vị trí" />
      ) : (
        state.problem && <ProblemAlert problem={state.problem} />
      )}
      {state.status === 'ready' && !state.rows.length && (
        <p role="status">Không có lịch sử vị trí phù hợp trong phạm vi và khoảng đã chọn.</p>
      )}
      <div className="flex flex-col gap-3">
        {state.rows.map((row, index) => (
          <article
            className="flex flex-col gap-2 border-t pt-3"
            key={`${row.allocationId ?? row.residentId ?? row.location?.bedId}-${index}`}
          >
            {mode === 'resident' ? (
              <>
                <p className="type-label">{row.location.label}</p>
                <p>
                  <span>{formatDateTime(row.from)}</span> – {row.to ? formatDateTime(row.to) : 'Chưa kết thúc'}
                </p>
                <CorrectionBadge corrected={row.corrected} />
                <div className="flex flex-wrap gap-2">
                  <Button variant="outline" onClick={() => setChain(row.allocationId)}>
                    Các lần đính chính
                  </Button>
                  {canCorrectAllocation && row.to && (
                    <Button variant="outline" onClick={() => setCorrecting(row.allocationId)}>
                      Đính chính
                    </Button>
                  )}
                </div>
                {correcting === row.allocationId && (
                  <AllocationCorrectionForm
                    allocationId={row.allocationId}
                    onSaved={() => setChain(row.allocationId)}
                  />
                )}
              </>
            ) : mode === 'beds' ? (
              <>
                <p className="type-label">{row.location.label}</p>
                {row.residents.length ? (
                  row.residents.map((person, n) => (
                    <div key={`${person.residentId}-${n}`}>
                      <p>
                        {person.fullName}, {interval(person.from, person.to)}
                      </p>
                      <CorrectionBadge corrected={person.corrected} />
                    </div>
                  ))
                ) : (
                  <p>Không có người sử dụng trong khoảng tra cứu.</p>
                )}
              </>
            ) : (
              <>
                <p className="type-label">{row.fullName}</p>
                <p>{row.room}</p>
                {row.overlaps.map((period, n) => (
                  <p key={n}>{interval(period.from, period.to)}</p>
                ))}
                <CorrectionBadge corrected={row.corrected} />
                {row.absences.map((period, n) => (
                  <p className="type-body-sm" key={n}>
                    {period.residentId === selected.id ? selected.fullName : row.fullName}:{' '}
                    {absenceLabels[period.type] ?? 'Vắng mặt'}, {interval(period.from, period.to)}
                  </p>
                ))}
              </>
            )}
            {row.pendingBackfill && <Badge variant="attention">Có nhập bù đang chờ duyệt</Badge>}
          </article>
        ))}
      </div>
      <CorrectionHistorySheet
        open={chain !== null}
        onOpenChange={(open) => !open && setChain(null)}
        recordType="M03.BED_ALLOCATION"
        recordId={chain}
        fields={allocationFields}
      />
    </section>
  )
}
