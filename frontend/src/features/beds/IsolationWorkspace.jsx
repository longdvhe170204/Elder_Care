import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getMe,
  getRoomIsolationRooms,
  getRoomIsolationPendingStays,
  getBedAllocations,
  getBedAllocationsPrescribingDoctors,
  getBedAllocationsSuitableBeds,
  postBedAllocationsByIdInfectionControlTransfer,
  postBedAllocationsByIdConfirmIsolatedStay,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { RoomIsolationPanel } from './RoomIsolationPanel'

// T056: danh sách dùng API theo quyền cách ly, không yêu cầu quyền quản trị phòng/tài khoản.
export function IsolationWorkspace({ canIsolate = false, canLift = false, canTransfer = false }) {
  const [state, setState] = useState({ loading: true })
  const [selected, setSelected] = useState(null)
  const [reason, setReason] = useState('')
  const [doctor, setDoctor] = useState('')
  const [bed, setBed] = useState('')
  const [options, setOptions] = useState({ beds: [], doctors: [] })
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [busy, setBusy] = useState(false)
  const pending = useRef(false)
  const command = useRef(null)
  const sequence = useRef(0)
  const fetchData = useCallback(async () => {
    const [rooms, allocations, me] = await Promise.all([
      getRoomIsolationRooms(),
      canTransfer ? getBedAllocations({ query: { status: 'ACTIVE' } }) : Promise.resolve({ data: [] }),
      canLift ? getMe() : Promise.resolve({ data: { roles: [] } }),
    ])
    const doctor = me.data.roles.includes('AC-03')
    const stays = doctor && canLift ? (await getRoomIsolationPendingStays()).data : []
    return { rooms: rooms.data, allocations: allocations.data, stays }
  }, [canTransfer, canLift])
  const reload = useCallback(async () => {
    setState(await fetchData())
  }, [fetchData])
  useEffect(() => {
    let current = true
    fetchData()
      .then((data) => {
        if (current) setState(data)
      })
      .catch((cause) => {
        if (current) setState({ error: problemOf(cause) })
      })
    return () => {
      current = false
      sequence.current += 1
    }
  }, [fetchData])
  async function choose(allocation, kind) {
    const current = ++sequence.current
    setSelected({ allocation, kind })
    setReason('')
    setDoctor('')
    setBed('')
    setError(null)
    setNotice(null)
    command.current = null
    setOptions({ beds: [], doctors: [], loading: kind === 'transfer' })
    if (kind !== 'transfer') return
    try {
      const [beds, doctors] = await Promise.all([
        getBedAllocationsSuitableBeds({
          query: { residentId: allocation.residentId, fromAllocationId: allocation.id, infectionControl: true },
        }),
        getBedAllocationsPrescribingDoctors(),
      ])
      if (current === sequence.current) setOptions({ beds: beds.data, doctors: doctors.data })
    } catch (cause) {
      if (current === sequence.current) setOptions({ beds: [], doctors: [], error: problemOf(cause) })
    }
  }
  async function save(event) {
    event.preventDefault()
    if (pending.current || !selected || !reason.trim()) return
    const transfer = selected.kind === 'transfer'
    const body = {
      reason: reason.trim(),
      ...(transfer && { newBedId: Number(bed), prescribingDoctorId: Number(doctor) }),
    }
    const signature = JSON.stringify({ id: selected.allocation.id, kind: selected.kind, body })
    if (command.current?.signature !== signature) command.current = { signature, key: crypto.randomUUID() }
    pending.current = true
    setBusy(true)
    setError(null)
    try {
      const action = transfer
        ? postBedAllocationsByIdInfectionControlTransfer
        : postBedAllocationsByIdConfirmIsolatedStay
      await action({ path: { id: selected.allocation.id }, body, headers: { 'Idempotency-Key': command.current.key } })
      setNotice(transfer ? 'Đã chuyển giường theo chỉ định kiểm soát lây nhiễm.' : 'Đã xác nhận ở lại giường.')
      setSelected(null)
      command.current = null
      await reload()
    } catch (cause) {
      setError(problemOf(cause))
    } finally {
      pending.current = false
      setBusy(false)
    }
  }
  if (state.loading) return <Skeleton className="h-64 w-full" />
  if (state.error)
    return (
      <ProblemAlert problem={state.error}>
        <Button variant="outline" onClick={() => reload().catch((cause) => setState({ error: problemOf(cause) }))}>
          Thử lại
        </Button>
      </ProblemAlert>
    )
  return (
    <section className="flex flex-col gap-6">
      <RoomIsolationPanel rooms={state.rooms} canIsolate={canIsolate} canLift={canLift} onChanged={reload} />
      {notice && (
        <Alert role="status">
          <AlertTitle>Kết quả thao tác</AlertTitle>
          <AlertDescription>{notice}</AlertDescription>
        </Alert>
      )}
      {error && <ProblemAlert problem={error} />}
      {state.stays.length > 0 && (
        <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4">
          <h2 className="type-title">Chờ xác nhận ở lại vùng cách ly</h2>
          {state.stays.map((row) => (
            <article key={row.id} className="flex flex-wrap items-center justify-between gap-3 border-t pt-3">
              <div>
                <p className="type-label">Hồ sơ #{row.residentId}</p>
                <p>{row.location.label}</p>
              </div>
              <Button variant="outline" disabled={busy} onClick={() => choose(row, 'confirm')}>
                Xác nhận ở lại hồ sơ {row.residentId}
              </Button>
            </article>
          ))}
        </section>
      )}
      {canTransfer && (
        <section className="flex flex-col gap-3 rounded-2xl border bg-card p-4">
          <h2 className="type-title">Chuyển theo chỉ định kiểm soát lây nhiễm</h2>
          {!state.allocations.length && <p>Chưa có phân bổ Hiệu lực trong phạm vi được chuyển.</p>}
          {state.allocations.map((row) => (
            <article key={row.id} className="flex flex-wrap items-center justify-between gap-3 border-t pt-3">
              <div>
                <p className="type-label">Hồ sơ #{row.residentId}</p>
                <p>{row.location.label}</p>
              </div>
              <Button variant="outline" disabled={busy} onClick={() => choose(row, 'transfer')}>
                Chuyển kiểm soát lây nhiễm hồ sơ {row.residentId}
              </Button>
            </article>
          ))}
        </section>
      )}
      {selected && (
        <form onSubmit={save} className="flex flex-col gap-4 rounded-2xl border bg-card p-4">
          <h2 className="type-title">
            {selected.kind === 'transfer' ? 'Chuyển theo chỉ định' : 'Xác nhận ở lại'} hồ sơ #
            {selected.allocation.residentId}
          </h2>
          <p>Giường hiện tại: {selected.allocation.location.label}</p>
          {options.loading && <Skeleton className="h-24 w-full" />}
          {options.error && (
            <ProblemAlert problem={options.error}>
              <Button variant="outline" onClick={() => choose(selected.allocation, selected.kind)}>
                Tải lại lựa chọn
              </Button>
            </ProblemAlert>
          )}
          {selected.kind === 'transfer' && (
            <>
              <label className="flex flex-col gap-2">
                Giường đích
                <select
                  required
                  disabled={busy || options.loading}
                  className="h-11 rounded-md border border-input bg-background px-3"
                  value={bed}
                  onChange={(event) => setBed(event.target.value)}
                >
                  <option value="">Chọn giường phù hợp</option>
                  {options.beds.map((row) => (
                    <option key={row.location.bedId} value={row.location.bedId}>
                      {row.location.label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="flex flex-col gap-2">
                Bác sĩ chỉ định
                <select
                  required
                  disabled={busy || options.loading}
                  className="h-11 rounded-md border border-input bg-background px-3"
                  value={doctor}
                  onChange={(event) => setDoctor(event.target.value)}
                >
                  <option value="">Chọn Bác sĩ Hoạt động</option>
                  {options.doctors.map((row) => (
                    <option key={row.id} value={row.id}>
                      {row.fullName ?? `Tài khoản #${row.id}`}
                    </option>
                  ))}
                </select>
              </label>
              {!options.loading && !options.error && (!options.beds.length || !options.doctors.length) && (
                <p>Chưa đủ giường phù hợp hoặc Bác sĩ để chọn.</p>
              )}
            </>
          )}
          <label className="flex flex-col gap-2">
            {selected.kind === 'transfer' ? 'Lý do chuyển' : 'Lý do xác nhận'}
            <textarea
              required
              maxLength={2000}
              disabled={busy}
              className="min-h-24 rounded-md border border-input bg-background p-3"
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
          <div className="flex flex-wrap gap-2">
            <Button
              type="submit"
              disabled={
                busy ||
                !reason.trim() ||
                (selected.kind === 'transfer' && (!bed || !doctor || options.loading || Boolean(options.error)))
              }
            >
              {busy
                ? 'Đang lưu…'
                : selected.kind === 'transfer'
                  ? 'Lưu chuyển kiểm soát lây nhiễm'
                  : 'Lưu xác nhận ở lại'}
            </Button>
            <Button
              variant="outline"
              disabled={busy}
              onClick={() => {
                sequence.current += 1
                setSelected(null)
                setError(null)
              }}
            >
              Hủy
            </Button>
          </div>
        </form>
      )}
    </section>
  )
}
