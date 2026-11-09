import { useEffect, useState } from 'react'
import {
  getAreas,
  getFloors,
  getLeaveRequests,
  getLeaveRequestsByIdPreApproval,
  getMeShifts,
  getStaff,
  postLeaveRequests,
  postLeaveRequestsByIdByCommand,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel, FieldGroup } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { ITEM_RESULT, LEAVE_COMMANDS, LEAVE_KIND, SCOPE_TYPES, SWAP_STATUS } from './swapLabels'

const selectClass =
  'h-10 w-full rounded-lg border border-input bg-background px-3 text-base outline-none focus-visible:ring-2 focus-visible:ring-ring'

// Nghỉ đột xuất: theo ca hoặc theo ngày, người duyệt quyết định từng mục (FR-026 → FR-031).
export function LeavePanel() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [failure, setFailure] = useState(null)
  const [creating, setCreating] = useState(false)
  const [active, setActive] = useState(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let cancelled = false
    getLeaveRequests()
      .then(({ data }) => {
        if (!cancelled) setRows(data)
      })
      .catch((error) => {
        if (!cancelled) setFailure(problemOf(error))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [revision])

  const reload = () => {
    setActive(null)
    setCreating(false)
    setLoading(true)
    setRevision((n) => n + 1)
  }

  return (
    <section className="flex flex-col gap-4" aria-label="Nghỉ đột xuất">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="type-card-title">Nghỉ đột xuất</h2>
        <Button type="button" onClick={() => setCreating(true)}>
          Lập yêu cầu nghỉ
        </Button>
      </div>
      <ProblemAlert problem={failure} />
      {creating && <LeaveForm onDone={reload} onCancel={() => setCreating(false)} />}
      {loading ? (
        <p role="status">Đang tải…</p>
      ) : rows.length === 0 ? (
        <p>Chưa có yêu cầu nghỉ.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã</TableHead>
              <TableHead>Dạng</TableHead>
              <TableHead>Nhân viên</TableHead>
              <TableHead>Ca hoặc ngày</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.id}>
                <TableCell>#{row.id}</TableCell>
                <TableCell>
                  {LEAVE_KIND[row.kind]}
                  {row.onBehalf && <span className="type-body-sm text-muted-foreground"> (lập thay)</span>}
                </TableCell>
                <TableCell>#{row.staffId}</TableCell>
                <TableCell>
                  <ul>
                    {row.items.map((item) => (
                      <li key={item.shiftId ?? item.date}>
                        {item.shiftId ? `Ca #${item.shiftId}` : formatDate(item.date)}
                        {' — '}
                        {ITEM_RESULT[item.result] ?? item.result}
                      </li>
                    ))}
                  </ul>
                </TableCell>
                <TableCell>
                  <StatusBadge status={SWAP_STATUS[row.status] ?? row.status} />
                </TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-2">
                    {(row.allowedCommands ?? []).map((command) => (
                      <Button
                        key={command}
                        type="button"
                        size="sm"
                        variant={command === 'approve' ? 'default' : 'outline'}
                        onClick={() => setActive({ row, command })}
                      >
                        {LEAVE_COMMANDS[command]?.label ?? command}
                      </Button>
                    ))}
                  </div>
                  {row.reason && <p className="type-body-sm text-muted-foreground">Lý do: {row.reason}</p>}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {active && <LeaveCommand {...active} onDone={reload} onCancel={() => setActive(null)} />}
    </section>
  )
}

function LeaveForm({ onDone, onCancel }) {
  const [kind, setKind] = useState('BY_SHIFT')
  const [mine, setMine] = useState([])
  const [chosen, setChosen] = useState([])
  const [dates, setDates] = useState('')
  const [scopeType, setScopeType] = useState('FLOOR')
  const [scopeId, setScopeId] = useState('')
  const [staffId, setStaffId] = useState('')
  const [staff, setStaff] = useState([])
  const [places, setPlaces] = useState({ FLOOR: [], AREA: [] })
  const [reason, setReason] = useState('')
  const [tried, setTried] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const from = new Date().toISOString().slice(0, 10)
    const to = new Date(Date.now() + 60 * 86400000).toISOString().slice(0, 10)
    getMeShifts({ query: { from, to } })
      .then(({ data }) => setMine(data.filter((s) => s.status === 'PUBLISHED')))
      .catch(() => setMine([]))
    // Không đọc được danh sách nhân viên, tầng, khu thì nhập mã (null)
    getStaff()
      .then(({ data }) => setStaff(data.filter((p) => p.status === 'WORKING')))
      .catch(() => setStaff(null))
    Promise.all([getFloors(), getAreas()])
      .then(([floors, areas]) => setPlaces({ FLOOR: floors.data, AREA: areas.data }))
      .catch(() => setPlaces(null))
  }, [])

  const toggle = (id) => setChosen((list) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]))

  async function submit(event) {
    event.preventDefault()
    setTried(true)
    const days = dates
      .split(/[\s,;]+/)
      .map((d) => d.trim())
      .filter(Boolean)
    if (
      !reason.trim() ||
      (kind === 'BY_SHIFT' && !chosen.length && !staffId) ||
      (kind === 'BY_DAY' && (!days.length || (scopeType !== 'FACILITY' && !scopeId)))
    )
      return
    setBusy(true)
    const body = { kind, reason: reason.trim() }
    if (staffId) body.staffId = Number(staffId)
    if (kind === 'BY_SHIFT') body.shiftIds = chosen
    else {
      body.dates = days
      body.scopeType = scopeType
      if (scopeType !== 'FACILITY') body.scopeId = Number(scopeId)
    }
    try {
      await postLeaveRequests({ body })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4 rounded-xl border p-4" aria-label="Lập yêu cầu nghỉ">
      <ProblemAlert problem={failure} />
      <FieldGroup>
        <Field>
          <FieldLabel htmlFor="leave-kind">Dạng</FieldLabel>
          <select id="leave-kind" className={selectClass} value={kind} onChange={(e) => setKind(e.target.value)}>
            <option value="BY_SHIFT">Nghỉ theo ca</option>
            <option value="BY_DAY">Nghỉ theo ngày (tháng chưa công bố lịch)</option>
          </select>
        </Field>
        <Field>
          <FieldLabel htmlFor="leave-staff">Người nghỉ</FieldLabel>
          {staff ? (
            <select
              id="leave-staff"
              className={selectClass}
              value={staffId}
              onChange={(e) => setStaffId(e.target.value)}
            >
              <option value="">Tôi xin nghỉ</option>
              {staff.map((p) => (
                <option key={p.id} value={p.id}>
                  Lập thay cho {p.fullName}
                </option>
              ))}
            </select>
          ) : (
            <Input
              id="leave-staff"
              inputMode="numeric"
              value={staffId}
              onChange={(e) => setStaffId(e.target.value)}
              placeholder="Mã nhân viên, chỉ khi lập thay"
            />
          )}
        </Field>
        {kind === 'BY_SHIFT' ? (
          <fieldset className="flex flex-col gap-2">
            <legend className="font-medium">Ca xin nghỉ</legend>
            {mine.map((s) => (
              <label key={s.id} className="flex items-center gap-2">
                <input type="checkbox" checked={chosen.includes(s.id)} onChange={() => toggle(s.id)} />
                {formatDateTime(s.startsAt)} – {formatDateTime(s.endsAt)}
              </label>
            ))}
          </fieldset>
        ) : (
          <>
            <Field>
              <FieldLabel htmlFor="leave-dates">Ngày nghỉ (yyyy-MM-dd, cách nhau bằng dấu phẩy)</FieldLabel>
              <Input id="leave-dates" value={dates} onChange={(e) => setDates(e.target.value)} />
            </Field>
            <Field>
              <FieldLabel htmlFor="leave-scope">Phạm vi</FieldLabel>
              <select
                id="leave-scope"
                className={selectClass}
                value={scopeType}
                onChange={(e) => {
                  setScopeType(e.target.value)
                  setScopeId('')
                }}
              >
                {Object.entries(SCOPE_TYPES).map(([code, label]) => (
                  <option key={code} value={code}>
                    {label}
                  </option>
                ))}
              </select>
            </Field>
            {scopeType !== 'FACILITY' && (
              <Field data-invalid={tried && !scopeId}>
                <FieldLabel htmlFor="leave-scope-id">{scopeType === 'FLOOR' ? 'Tầng' : 'Khu vực'}</FieldLabel>
                {places ? (
                  <select
                    id="leave-scope-id"
                    className={selectClass}
                    value={scopeId}
                    aria-invalid={tried && !scopeId}
                    onChange={(e) => setScopeId(e.target.value)}
                  >
                    <option value="">{scopeType === 'FLOOR' ? 'Chọn tầng' : 'Chọn khu vực'}</option>
                    {places[scopeType].map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                ) : (
                  <Input
                    id="leave-scope-id"
                    inputMode="numeric"
                    value={scopeId}
                    aria-invalid={tried && !scopeId}
                    onChange={(e) => setScopeId(e.target.value)}
                    placeholder="Mã tầng hoặc khu vực"
                  />
                )}
              </Field>
            )}
          </>
        )}
        <Field>
          <FieldLabel htmlFor="leave-reason">Lý do</FieldLabel>
          <Textarea
            id="leave-reason"
            value={reason}
            aria-invalid={tried && !reason.trim()}
            onChange={(e) => setReason(e.target.value)}
          />
        </Field>
      </FieldGroup>
      <div className="flex gap-2">
        <Button type="submit" disabled={busy}>
          Gửi yêu cầu
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Đóng
        </Button>
      </div>
    </form>
  )
}

function LeaveCommand({ row, command, onDone, onCancel }) {
  const meta = LEAVE_COMMANDS[command] ?? { label: command, reason: false }
  const [info, setInfo] = useState([])
  const [results, setResults] = useState({})
  const [leads, setLeads] = useState({})
  const [reason, setReason] = useState('')
  const [tried, setTried] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const pending = row.items.filter((i) => i.result === 'PENDING')
  const key = (item) => item.shiftId ?? item.date

  useEffect(() => {
    if (command !== 'approve') return
    getLeaveRequestsByIdPreApproval({ path: { id: row.id } })
      .then(({ data }) => setInfo(data))
      .catch((error) => setFailure(problemOf(error)))
  }, [command, row.id])

  const anyRejected = pending.some((i) => results[key(i)] === 'REJECTED')
  const reasonRequired = meta.reason || anyRejected

  async function submit(event) {
    event.preventDefault()
    setTried(true)
    if (reasonRequired && !reason.trim()) return
    setBusy(true)
    const body = {}
    if (reason.trim()) body.reason = reason.trim()
    if (command === 'approve') {
      body.items = pending.map((item) => ({
        ...(item.shiftId ? { shiftId: item.shiftId } : { date: item.date }),
        result: results[key(item)] ?? 'APPROVED',
        ...(leads[key(item)] ? { newLeadId: Number(leads[key(item)]) } : {}),
      }))
    }
    try {
      await postLeaveRequestsByIdByCommand({ path: { id: row.id, command }, body })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form
      onSubmit={submit}
      className="flex flex-col gap-4 rounded-xl border p-4"
      aria-label={`${meta.label} yêu cầu nghỉ #${row.id}`}
    >
      <h3 className="type-card-title">
        {meta.label} yêu cầu nghỉ #{row.id}
      </h3>
      <ProblemAlert problem={failure} />
      {command === 'approve' && (
        <ul className="flex flex-col gap-3">
          {pending.map((item) => {
            const detail = info.find((i) => i.shiftId === item.shiftId)
            return (
              <li key={key(item)} className="flex flex-col gap-2 rounded-lg border p-3">
                <p className="font-medium">{item.shiftId ? `Ca #${item.shiftId}` : formatDate(item.date)}</p>
                {detail?.isLead && (
                  <Field>
                    <FieldLabel htmlFor={`lead-${item.shiftId}`}>
                      Mã người phụ trách mới (ứng viên: {detail.eligibleLeadCandidates.join(', ') || 'không có'})
                    </FieldLabel>
                    <Input
                      id={`lead-${item.shiftId}`}
                      inputMode="numeric"
                      value={leads[key(item)] ?? ''}
                      onChange={(e) => setLeads({ ...leads, [key(item)]: e.target.value })}
                    />
                  </Field>
                )}
                {detail?.overlappingTrips?.length > 0 && <p>Có chuyến đi chồng giờ, nhiệm vụ sẽ được gỡ.</p>}
                <select
                  aria-label={`Quyết định ${item.shiftId ? `ca ${item.shiftId}` : item.date}`}
                  className={selectClass}
                  value={results[key(item)] ?? 'APPROVED'}
                  onChange={(e) => setResults({ ...results, [key(item)]: e.target.value })}
                >
                  <option value="APPROVED">Duyệt</option>
                  <option value="REJECTED">Từ chối</option>
                </select>
              </li>
            )
          })}
        </ul>
      )}
      {(meta.reason || command === 'approve') && (
        <Field>
          <FieldLabel htmlFor="leave-command-reason">Lý do</FieldLabel>
          <Textarea
            id="leave-command-reason"
            value={reason}
            aria-invalid={tried && reasonRequired && !reason.trim()}
            onChange={(e) => setReason(e.target.value)}
          />
        </Field>
      )}
      <div className="flex gap-2">
        <Button type="submit" disabled={busy}>
          {meta.label}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Đóng
        </Button>
      </div>
    </form>
  )
}
