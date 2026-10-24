import { useEffect, useRef, useState } from 'react'
import {
  deleteAssignmentsById,
  getShiftsByIdAssignments,
  postAssignmentsByIdCancel,
  postAssignmentsByIdEnd,
  postShiftsByIdAssignments,
  postShiftsByIdAssignmentsPreview,
  postShiftsByIdCopyAssignments,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { Choice } from './ShiftForm'
import { localInput } from './shiftLabels'

const TARGETS = { FLOOR: 'Tầng', AREA: 'Khu vực', ROOM: 'Phòng', RESIDENT: 'Người cao tuổi' }
const ROLES = {
  CAREGIVER: 'Nhân viên chăm sóc',
  NURSING_STAFF: 'Điều dưỡng',
  FLOOR_MANAGER: 'Trưởng tầng',
  DOCTOR: 'Bác sĩ',
  OFFICE_STAFF: 'Hành chính',
  KITCHEN_STAFF: 'Nhân viên bếp',
}
const STATES = { DRAFT: 'Nháp', SCHEDULED: 'Tương lai', ACTIVE: 'Hiệu lực', ENDED: 'Đã kết thúc', CANCELLED: 'Đã hủy' }

export function AssignmentPanel({ shift, staff = [] }) {
  const commands = shift.allowedCommands ?? []
  const canCreate = commands.includes('createAssignment')
  const [rows, setRows] = useState([])
  const [revision, setRevision] = useState(0)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')
  const [input, setInput] = useState({
    staffId: String(shift.staff?.[0]?.staffId ?? ''),
    targetType: 'FLOOR',
    targetId: '',
    taskTypeId: '',
    role: 'CAREGIVER',
    assignmentKind: 'PRIMARY',
    assignedNurse: false,
    startsAt: localInput(shift.startsAt),
    endsAt: localInput(shift.endsAt),
  })
  const [preview, setPreview] = useState(null)
  const [confirmed, setConfirmed] = useState(false)
  const [action, setAction] = useState(null)
  const [reason, setReason] = useState('')
  const [source, setSource] = useState('')
  const [copyResult, setCopyResult] = useState(null)
  const keys = useRef(new Map())
  const keyFor = (body) => {
    const text = JSON.stringify(body)
    if (!keys.current.has(text)) keys.current.set(text, crypto.randomUUID())
    return keys.current.get(text)
  }
  useEffect(() => {
    let active = true
    getShiftsByIdAssignments({ path: { id: shift.id } })
      .then(({ data }) => {
        if (active) setRows(data)
      })
      .catch((e) => {
        if (active) setFailure(problemOf(e))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [shift.id, revision])
  function change(name, value) {
    setInput((current) => ({ ...current, [name]: value }))
    setPreview(null)
    setConfirmed(false)
    setNotice('')
  }
  function body() {
    return {
      staffId: Number(input.staffId),
      targetType: input.targetType,
      targetId: Number(input.targetId),
      role: input.role,
      assignmentKind: input.assignmentKind,
      assignedNurse: input.assignedNurse,
      ...(input.taskTypeId && { taskTypeId: Number(input.taskTypeId) }),
      ...(input.startsAt && { startsAt: `${input.startsAt}:00+07:00` }),
      ...(input.endsAt && { endsAt: `${input.endsAt}:00+07:00` }),
    }
  }
  async function check(e) {
    e.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      const { data } = await postShiftsByIdAssignmentsPreview({ path: { id: shift.id }, body: body() })
      setPreview(data)
      setConfirmed(false)
    } catch (error) {
      setFailure(problemOf(error))
      setPreview(null)
    } finally {
      setBusy(false)
    }
  }
  async function save() {
    setBusy(true)
    setFailure(null)
    try {
      const value = body()
      await postShiftsByIdAssignments({
        path: { id: shift.id },
        headers: { 'Idempotency-Key': keyFor(value) },
        body: value,
      })
      setPreview(null)
      keys.current.clear()
      setNotice('Đã lưu phân công.')
      setRevision((n) => n + 1)
    } catch (error) {
      setFailure(problemOf(error))
      setPreview(null)
    } finally {
      setBusy(false)
    }
  }
  async function finish(e) {
    e.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      if (action.kind === 'delete') await deleteAssignmentsById({ path: { id: action.row.id } })
      else {
        const command = action.kind === 'end' ? postAssignmentsByIdEnd : postAssignmentsByIdCancel
        const value = { reason: reason.trim() }
        await command({
          path: { id: action.row.id },
          body: value,
          headers: { 'Idempotency-Key': keyFor({ id: action.row.id, kind: action.kind, ...value }) },
        })
      }
      setAction(null)
      setReason('')
      setRevision((n) => n + 1)
      setNotice('Đã cập nhật phân công.')
    } catch (error) {
      setFailure(problemOf(error))
      if (error.status === 409) setRevision((n) => n + 1)
    } finally {
      setBusy(false)
    }
  }
  async function copy(e) {
    e.preventDefault()
    setBusy(true)
    setFailure(null)
    try {
      const value = { sourceShiftId: Number(source) }
      const { data } = await postShiftsByIdCopyAssignments({
        path: { id: shift.id },
        body: value,
        headers: { 'Idempotency-Key': keyFor({ target: shift.id, ...value }) },
      })
      setCopyResult(data)
      setRevision((n) => n + 1)
      setPreview(null)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="flex min-w-0 flex-col gap-5 [&_button]:min-h-11 [&_input:not([type=checkbox])]:min-h-11 [&_select]:min-h-11">
      <ProblemAlert problem={failure} />
      {notice && <p role="status">{notice}</p>}
      {loading ? (
        <p role="status">Đang tải phân công…</p>
      ) : !rows.length ? (
        <p>Chưa có phân công trong ca.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nhân viên / Đối tượng</TableHead>
              <TableHead>Vai trò / Trạng thái</TableHead>
              <TableHead>Thao tác</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.id}>
                <TableCell>
                  <p className="font-medium">
                    {staff.find((s) => s.id === row.staffId)?.fullName ?? `Nhân viên #${row.staffId}`}
                  </p>
                  <p>
                    {TARGETS[row.targetType]} #{row.targetId}
                    {row.taskTypeId ? ` · Công việc #${row.taskTypeId}` : ''}
                  </p>
                  {row.startsAt && (
                    <p className="text-muted-foreground">
                      {formatDateTime(row.startsAt)} → {formatDateTime(row.endsAt)}
                    </p>
                  )}
                </TableCell>
                <TableCell>
                  <p>
                    {ROLES[row.role]} · {row.assignmentKind === 'PRIMARY' ? 'Chính' : 'Hỗ trợ'}
                  </p>
                  <p>
                    {STATES[row.status]}
                    {row.assignedNurse ? ' · Điều dưỡng phụ trách' : ''}
                  </p>
                </TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-2">
                    {[
                      ['DRAFT', 'deleteAssignment', 'delete', 'Xóa'],
                      ['SCHEDULED', 'cancelAssignment', 'cancel', 'Hủy'],
                      ['ACTIVE', 'endAssignment', 'end', 'Kết thúc'],
                    ].map(
                      ([status, permission, kind, label]) =>
                        row.status === status &&
                        commands.includes(permission) && (
                          <Button
                            key={kind}
                            size="sm"
                            variant="outline"
                            disabled={busy}
                            onClick={() => {
                              setAction({ row, kind })
                              setReason('')
                            }}
                          >
                            {label} #{row.id}
                          </Button>
                        ),
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {action && (
        <form className="flex flex-col gap-3 rounded-xl border p-3" onSubmit={finish}>
          <h3 className="type-section">
            {action.kind === 'delete'
              ? 'Xóa phân công nháp'
              : action.kind === 'end'
                ? 'Kết thúc phân công'
                : 'Hủy phân công'}{' '}
            #{action.row.id}
          </h3>
          {action.kind !== 'delete' && (
            <Field>
              <FieldLabel htmlFor="assignment-reason">
                {action.kind === 'end' ? 'Lý do kết thúc' : 'Lý do hủy'}
              </FieldLabel>
              <Textarea
                id="assignment-reason"
                required
                maxLength={2000}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </Field>
          )}
          <div className="flex gap-2">
            <Button type="submit" disabled={busy || (action.kind !== 'delete' && !reason.trim())}>
              {action.kind === 'delete' ? 'Xác nhận xóa' : action.kind === 'end' ? 'Xác nhận kết thúc' : 'Xác nhận hủy'}
            </Button>
            <Button type="button" variant="outline" onClick={() => setAction(null)}>
              Quay lại
            </Button>
          </div>
        </form>
      )}
      {canCreate && (
        <form className="flex flex-col gap-4 rounded-xl border p-4" onSubmit={check}>
          <h3 className="type-section">Lập phân công</h3>
          <fieldset disabled={busy} className="grid min-w-0 gap-3 sm:grid-cols-2">
            <Choice
              id="assignment-staff"
              label="Nhân viên trong ca"
              value={input.staffId}
              onChange={(e) => change('staffId', e.target.value)}
              required
            >
              {!shift.staff?.length && <option value="">Chưa có nhân viên</option>}
              {shift.staff?.map((m) => (
                <option key={m.staffId} value={m.staffId}>
                  {staff.find((s) => s.id === m.staffId)?.fullName ?? `Nhân viên #${m.staffId}`}
                </option>
              ))}
            </Choice>
            <Choice
              id="assignment-target"
              label="Đối tượng phân công"
              value={input.targetType}
              onChange={(e) => change('targetType', e.target.value)}
            >
              {Object.entries(TARGETS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Choice>
            <Field>
              <FieldLabel htmlFor="assignment-target-id">Mã đối tượng</FieldLabel>
              <Input
                id="assignment-target-id"
                type="number"
                min="1"
                required
                value={input.targetId}
                onChange={(e) => change('targetId', e.target.value)}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="assignment-task">Mã loại công việc (tùy chọn)</FieldLabel>
              <Input
                id="assignment-task"
                type="number"
                min="1"
                value={input.taskTypeId}
                onChange={(e) => change('taskTypeId', e.target.value)}
              />
            </Field>
            <Choice
              id="assignment-role"
              label="Vai trò thực hiện"
              value={input.role}
              onChange={(e) => change('role', e.target.value)}
            >
              {Object.entries(ROLES).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Choice>
            <Choice
              id="assignment-kind"
              label="Chính / Hỗ trợ"
              value={input.assignmentKind}
              onChange={(e) => change('assignmentKind', e.target.value)}
            >
              <option value="PRIMARY">Chính</option>
              <option value="SUPPORT">Hỗ trợ</option>
            </Choice>
            <Field>
              <FieldLabel htmlFor="assignment-start">Bắt đầu phân công</FieldLabel>
              <Input
                id="assignment-start"
                type="datetime-local"
                value={input.startsAt}
                onChange={(e) => change('startsAt', e.target.value)}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="assignment-end">Kết thúc phân công</FieldLabel>
              <Input
                id="assignment-end"
                type="datetime-local"
                value={input.endsAt}
                onChange={(e) => change('endsAt', e.target.value)}
              />
            </Field>
            <label className="flex min-h-11 items-center gap-2 sm:col-span-2">
              <input
                type="checkbox"
                checked={input.assignedNurse}
                onChange={(e) => change('assignedNurse', e.target.checked)}
              />
              Điều dưỡng phụ trách
            </label>
          </fieldset>
          {preview && (
            <div className="flex flex-col gap-2" role="status">
              {preview.warnings?.length ? (
                <>
                  {preview.warnings.map((w) => (
                    <p key={w.code}>
                      <span>{w.description}</span>
                      {w.details?.taskTypeIds?.length ? ` · Loại công việc: ${w.details.taskTypeIds.join(', ')}` : ''}
                    </p>
                  ))}
                  <label className="flex min-h-11 items-center gap-2">
                    <input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} />
                    Đã xem các công việc không đủ điều kiện
                  </label>
                </>
              ) : (
                <p>Phân công đủ điều kiện tại thời điểm kiểm tra.</p>
              )}
            </div>
          )}
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="outline" disabled={busy}>
              Kiểm tra phân công
            </Button>
            <Button
              type="button"
              onClick={save}
              disabled={busy || !preview || (!!preview.warnings?.length && !confirmed)}
            >
              Lưu phân công
            </Button>
          </div>
        </form>
      )}
      {commands.includes('copyAssignments') && (
        <form className="flex flex-col gap-3 rounded-xl border p-4" onSubmit={copy}>
          <h3 className="type-section">Sao chép từ ca cùng phạm vi</h3>
          <Field>
            <FieldLabel htmlFor="source-shift">Mã ca nguồn</FieldLabel>
            <Input
              id="source-shift"
              type="number"
              required
              min="1"
              value={source}
              onChange={(e) => setSource(e.target.value)}
            />
          </Field>
          <Button type="submit" variant="outline" disabled={busy || !source}>
            Sao chép phân công
          </Button>
          {copyResult && (
            <div role="status">
              <p>Đã sao chép {copyResult.copiedIds?.length ?? 0} phân công.</p>
              {copyResult.notCopied?.map((row) => (
                <p key={row.sourceAssignmentId}>
                  Không chép #{row.sourceAssignmentId}: {row.reason}
                </p>
              ))}
            </div>
          )}
        </form>
      )}
    </div>
  )
}
