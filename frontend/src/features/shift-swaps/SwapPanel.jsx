import { useEffect, useState } from 'react'
import {
  getMeShifts,
  getShiftSwaps,
  getShiftSwapsByIdChecks,
  getShiftSwapsRecipientShifts,
  getStaff,
  postShiftSwaps,
  postShiftSwapsByIdByCommand,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel, FieldGroup } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { SWAP_COMMANDS, SWAP_KIND, SWAP_STATUS, WARNING_TEXT } from './swapLabels'

const selectClass =
  'h-10 w-full rounded-lg border border-input bg-background px-3 text-base outline-none focus-visible:ring-2 focus-visible:ring-ring'

// UC-54: đổi ca. Người nhường lập, người nhận đồng ý, Trưởng tầng duyệt (FR-019 → FR-025).
export function SwapPanel() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [failure, setFailure] = useState(null)
  const [creating, setCreating] = useState(false)
  const [active, setActive] = useState(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let cancelled = false
    getShiftSwaps()
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
    <section className="flex flex-col gap-4" aria-label="Yêu cầu đổi ca">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="type-card-title">Yêu cầu đổi ca</h2>
        <Button type="button" onClick={() => setCreating(true)}>
          Lập yêu cầu đổi ca
        </Button>
      </div>
      <ProblemAlert problem={failure} />
      {creating && <SwapForm onDone={reload} onCancel={() => setCreating(false)} />}
      {loading ? (
        <p role="status">Đang tải…</p>
      ) : rows.length === 0 ? (
        <p>Chưa có yêu cầu đổi ca.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Mã</TableHead>
              <TableHead>Dạng</TableHead>
              <TableHead>Ca của người nhường</TableHead>
              <TableHead>Ca của người nhận</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.id}>
                <TableCell>#{row.id}</TableCell>
                <TableCell>{SWAP_KIND[row.kind]}</TableCell>
                <TableCell>Ca #{row.shiftXId}</TableCell>
                <TableCell>{row.shiftYId ? `Ca #${row.shiftYId}` : '—'}</TableCell>
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
                        variant={command === 'approve' || command === 'accept' ? 'default' : 'outline'}
                        onClick={() => setActive({ row, command })}
                      >
                        {SWAP_COMMANDS[command]?.label ?? command}
                      </Button>
                    ))}
                  </div>
                  {row.reason && <p className="type-body-sm text-muted-foreground">Lý do: {row.reason}</p>}
                  {row.recipientDeclineReason && (
                    <p className="type-body-sm text-muted-foreground">
                      Lý do người nhận từ chối: {row.recipientDeclineReason}
                    </p>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {active && <SwapCommand {...active} onDone={reload} onCancel={() => setActive(null)} />}
    </section>
  )
}

function SwapForm({ onDone, onCancel }) {
  const [kind, setKind] = useState('MUTUAL')
  const [mine, setMine] = useState([])
  const [staff, setStaff] = useState(null)
  const [shiftX, setShiftX] = useState('')
  const [recipient, setRecipient] = useState('')
  const [options, setOptions] = useState([])
  const [shiftY, setShiftY] = useState('')
  const [reason, setReason] = useState('')
  const [tried, setTried] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const from = new Date().toISOString().slice(0, 10)
    const to = new Date(Date.now() + 60 * 86400000).toISOString().slice(0, 10)
    getMeShifts({ query: { from, to } })
      .then(({ data }) => setMine(data.filter((s) => s.status === 'PUBLISHED')))
      .catch((error) => setFailure(problemOf(error)))
    // Không có quyền xem danh sách nhân viên thì nhập mã nhân viên người nhận
    getStaff()
      .then(({ data }) => setStaff(data.filter((p) => p.status === 'WORKING')))
      .catch(() => setStaff(null))
  }, [])

  useEffect(() => {
    if (kind !== 'MUTUAL' || !shiftX || !recipient) return
    getShiftSwapsRecipientShifts({ query: { shiftXId: shiftX, recipientId: recipient } })
      .then(({ data }) => setOptions(data))
      .catch((error) => setFailure(problemOf(error)))
  }, [kind, shiftX, recipient])

  async function submit(event) {
    event.preventDefault()
    setTried(true)
    if (!reason.trim() || !shiftX || !recipient || (kind === 'MUTUAL' && !shiftY)) return
    setBusy(true)
    const body = { kind, shiftXId: Number(shiftX), recipientId: Number(recipient), reason: reason.trim() }
    if (kind === 'MUTUAL') body.shiftYId = Number(shiftY)
    try {
      await postShiftSwaps({ body })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4 rounded-xl border p-4" aria-label="Lập yêu cầu đổi ca">
      <ProblemAlert problem={failure} />
      <FieldGroup>
        <Field>
          <FieldLabel htmlFor="swap-kind">Dạng</FieldLabel>
          <select id="swap-kind" className={selectClass} value={kind} onChange={(e) => setKind(e.target.value)}>
            <option value="MUTUAL">Đổi hai chiều</option>
            <option value="TAKE_OVER">Nhận thay</option>
          </select>
        </Field>
        <Field>
          <FieldLabel htmlFor="swap-x">Ca của tôi</FieldLabel>
          <select id="swap-x" className={selectClass} value={shiftX} onChange={(e) => setShiftX(e.target.value)}>
            <option value="">Chọn ca</option>
            {mine.map((s) => (
              <option key={s.id} value={s.id}>
                {formatDateTime(s.startsAt)} – {formatDateTime(s.endsAt)}
              </option>
            ))}
          </select>
        </Field>
        <Field>
          <FieldLabel htmlFor="swap-recipient">Người nhận</FieldLabel>
          {staff ? (
            <select
              id="swap-recipient"
              className={selectClass}
              value={recipient}
              onChange={(e) => setRecipient(e.target.value)}
            >
              <option value="">Chọn đồng nghiệp</option>
              {staff.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.fullName}
                </option>
              ))}
            </select>
          ) : (
            <Input
              id="swap-recipient"
              inputMode="numeric"
              value={recipient}
              onChange={(e) => setRecipient(e.target.value)}
              placeholder="Mã nhân viên người nhận"
            />
          )}
        </Field>
        {kind === 'MUTUAL' && (
          <Field>
            <FieldLabel htmlFor="swap-y">Ca của người nhận</FieldLabel>
            <select id="swap-y" className={selectClass} value={shiftY} onChange={(e) => setShiftY(e.target.value)}>
              <option value="">Chọn ca</option>
              {(kind === 'MUTUAL' && shiftX && recipient ? options : []).map((s) => (
                <option key={s.shiftId} value={s.shiftId}>
                  {formatDateTime(s.startsAt)} – {formatDateTime(s.endsAt)}
                </option>
              ))}
            </select>
          </Field>
        )}
        <Field>
          <FieldLabel htmlFor="swap-reason">Lý do</FieldLabel>
          <Textarea
            id="swap-reason"
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

function SwapCommand({ row, command, onDone, onCancel }) {
  const meta = SWAP_COMMANDS[command] ?? { label: command, reason: false }
  const [checks, setChecks] = useState(null)
  const [reason, setReason] = useState('')
  const [leadX, setLeadX] = useState('')
  const [leadY, setLeadY] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [tried, setTried] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const showChecks = command === 'approve' || command === 'accept'

  useEffect(() => {
    if (!showChecks) return
    getShiftSwapsByIdChecks({ path: { id: row.id } })
      .then(({ data }) => setChecks(data))
      .catch((error) => setFailure(problemOf(error)))
  }, [showChecks, row.id])

  const warnings = checks?.continuousHours ?? []
  const needLead = (ref) => ref?.requiresChoice === true
  const reasonRequired = meta.reason || (command === 'approve' && (row.selfApproved || warnings.length > 0))

  async function submit(event) {
    event.preventDefault()
    setTried(true)
    if (reasonRequired && !reason.trim()) return
    setBusy(true)
    const body = {}
    if (reason.trim()) body.reason = reason.trim()
    if (command === 'approve') {
      if (leadX) body.newLeadXId = Number(leadX)
      if (leadY) body.newLeadYId = Number(leadY)
      body.confirmWarning = confirmed
    }
    try {
      await postShiftSwapsByIdByCommand({ path: { id: row.id, command }, body })
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
      aria-label={`${meta.label} yêu cầu #${row.id}`}
    >
      <h3 className="type-card-title">
        {meta.label} yêu cầu #{row.id}
      </h3>
      <ProblemAlert problem={failure}>
        {failure?.errorCode === 'STATE_CHANGED' && (
          <Button type="button" variant="outline" onClick={onDone}>
            Tải lại
          </Button>
        )}
      </ProblemAlert>
      {checks && (
        <div className="flex flex-col gap-2" aria-label="Kết quả kiểm tra">
          {checks.overlaps.length > 0 && <p>Có ca chồng giờ, không thực hiện được.</p>}
          {warnings.length > 0 && (
            <div className="rounded-xl border border-attention bg-attention/10 p-3">
              <p>{WARNING_TEXT.CONTINUOUS_WORK_HOURS}</p>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} />
                Tôi đã xem cảnh báo
              </label>
            </div>
          )}
          {checks.nonTransferableAssignments.length > 0 && (
            <p>
              {checks.nonTransferableAssignments.length} phân công không chuyển được, công việc thành công việc chung.
            </p>
          )}
          {command === 'approve' && needLead(checks.leadAfterSwap?.shiftX) && (
            <Field>
              <FieldLabel htmlFor="lead-x">Mã người phụ trách mới của ca X</FieldLabel>
              <Input id="lead-x" inputMode="numeric" value={leadX} onChange={(e) => setLeadX(e.target.value)} />
            </Field>
          )}
          {command === 'approve' && needLead(checks.leadAfterSwap?.shiftY) && (
            <Field>
              <FieldLabel htmlFor="lead-y">Mã người phụ trách mới của ca Y</FieldLabel>
              <Input id="lead-y" inputMode="numeric" value={leadY} onChange={(e) => setLeadY(e.target.value)} />
            </Field>
          )}
          {checks.monthlyHours && <p className="type-body-sm">Giờ làm trong tháng đã có trong kết quả kiểm tra.</p>}
        </div>
      )}
      {(meta.reason || command === 'approve') && (
        <Field>
          <FieldLabel htmlFor="swap-command-reason">Lý do</FieldLabel>
          <Textarea
            id="swap-command-reason"
            value={reason}
            aria-invalid={tried && reasonRequired && !reason.trim()}
            onChange={(e) => setReason(e.target.value)}
          />
        </Field>
      )}
      <div className="flex gap-2">
        <Button type="submit" disabled={busy || (warnings.length > 0 && !confirmed && command === 'approve')}>
          {meta.label}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Đóng
        </Button>
      </div>
    </form>
  )
}
