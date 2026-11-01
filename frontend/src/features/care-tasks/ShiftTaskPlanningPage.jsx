import { useEffect, useRef, useState } from 'react'
import {
  getMeShifts,
  getShiftsById,
  getShiftSchedules,
  getResidents,
  getTaskTypes,
  getShiftsByShiftIdSourceData,
  getShiftsByShiftIdCareTasks,
  postShiftsByShiftIdCareTasksPlan,
} from '@/api'
import { Button } from '@/components/ui/button'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { usePermissions } from '@/common/permissions/permissions'
import { CareField, CareCheck } from './CareField'
import { IMPORTANCE, CARE_ROLES } from './carePlanLabels'

const BASIS = {
  CARE_PLAN_ITEM: 'Mục kế hoạch',
  MEASUREMENT: 'Lần đo',
  MEAL: 'Bữa ăn',
  ACTIVITY_SESSION: 'Buổi hoạt động',
  POST_FALL_MONITORING: 'Theo dõi sau ngã',
  CONTACT_MONITORING: 'Theo dõi tiếp xúc',
  WATER_INTAKE_ALERT: 'Cảnh báo uống nước',
  SHIFT_START_CHECK: 'Kiểm tra đầu ca',
  SUPERVISION: 'Giám sát',
  AD_HOC: 'Phát sinh',
}
const STATUSES = {
  NOT_YET_DUE: 'Chưa đến hạn',
  DUE: 'Đến hạn',
  OVERDUE: 'Quá hạn',
  COMPLETED: 'Hoàn thành',
  COMPLETED_LATE: 'Hoàn thành trễ',
  NOT_PERFORMED: 'Không thực hiện',
  CANCELLED: 'Đã hủy',
}
const ERRORS = {
  RESIDENT_NOT_STAYING: 'Người cao tuổi không đang lưu trú.',
  RESIDENT_ABSENT: 'Người cao tuổi đang vắng mặt.',
  DAY_CARE_NOT_PRESENT: 'Người bán trú chưa có mặt trong khoảng này.',
  OUTSIDE_SHIFT: 'Thời điểm dự kiến nằm ngoài ca.',
  PLAN_ITEM_MISMATCH: 'Thời điểm, khung hoặc mức chưa khớp mục kế hoạch.',
  INVALID_BASIS: 'Căn cứ chưa hợp lệ hoặc nguồn chưa xác nhận.',
  MEDICATION_IS_NOT_A_TASK: 'Liều thuốc không được lập thành công việc.',
  INVALID_PERFORMER: 'Người thực hiện hoặc lựa chọn công việc chung chưa hợp lệ.',
  MISSING_CERTIFICATE: 'Người thực hiện chưa đủ chứng chỉ.',
  DUPLICATE_BASIS: 'Trùng căn cứ và thời điểm dự kiến.',
  OLD_REVISION_WINDOW_OVERLAP: 'Trùng khung công việc của phiên bản trước.',
  SHIFT_CHECK_ALREADY_EXISTS: 'Điều dưỡng đã có công việc này trong ca.',
}
const optionalNumber = (text) => (text === '' || text == null ? undefined : Number(text))

export function ShiftTaskPlanningPage({ shiftId: initialShiftId }) {
  const { status, can } = usePermissions()
  const [shiftId, setShiftId] = useState(initialShiftId || '')
  const [shifts, setShifts] = useState([])
  const [month, setMonth] = useState('')
  const [source, setSource] = useState(null)
  const [shiftMembers, setShiftMembers] = useState([])
  const [residents, setResidents] = useState([])
  const [cursor, setCursor] = useState(null)
  const [types, setTypes] = useState([])
  const [tasks, setTasks] = useState([])
  const [rows, setRows] = useState([])
  const [failure, setFailure] = useState(null)
  const [rowErrors, setRowErrors] = useState([])
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState('')
  const summary = useRef(null)
  const attempt = useRef(null)
  const view = can('SHIFT_TASK_PLANNING', 'VIEW')
  const create = can('SHIFT_TASK_PLANNING', 'CREATE')
  useEffect(() => {
    if (status !== 'ready' || !view) return
    let current = true
    Promise.all([getTaskTypes(), getResidents()])
      .then(([catalog, people]) => {
        if (!current) return
        setTypes(catalog.data.filter((t) => t.status === 'ACTIVE'))
        setResidents(people.data.items)
        setCursor(people.data.nextCursor)
      })
      .catch((error) => {
        if (current) setFailure(problemOf(error))
      })
    return () => {
      current = false
    }
  }, [status, view])
  useEffect(() => {
    if (status !== 'ready' || !view || !shiftId) return
    let current = true
    Promise.resolve().then(() => {
      if (current) setLoading(true)
    })
    Promise.all([
      getShiftsByShiftIdSourceData({ path: { shiftId } }),
      getShiftsByShiftIdCareTasks({ path: { shiftId } }),
      getShiftsById({ path: { id: shiftId } }),
    ])
      .then(([data, work, roster]) => {
        if (current) {
          setSource(data.data)
          setTasks(work.data)
          setShiftMembers(roster.data.staff.filter((m) => !m.absent && !m.approvedLeave))
          setFailure(null)
        }
      })
      .catch((error) => {
        if (current) {
          setSource(null)
          setFailure(problemOf(error))
        }
      })
      .finally(() => {
        if (current) setLoading(false)
      })
    return () => {
      current = false
    }
  }, [status, view, shiftId])
  useEffect(() => {
    if (rowErrors.length) summary.current?.focus()
  }, [rowErrors])
  async function findShifts(event) {
    event.preventDefault()
    setFailure(null)
    try {
      if (can('SHIFT_SCHEDULE', 'CREATE')) {
        const { data } = await getShiftSchedules({ query: { month } })
        setShifts(
          data
            .filter((s) => s.status === 'PUBLISHED')
            .flatMap((s) => s.shifts)
            .filter((s) => s.status !== 'CANCELLED'),
        )
      } else {
        const start = `${month}-01`
        const [year, value] = month.split('-').map(Number)
        const end = value === 12 ? `${year + 1}-01-01` : `${year}-${String(value + 1).padStart(2, '0')}-01`
        const { data } = await getMeShifts({ query: { from: start, to: end } })
        setShifts(data.filter((s) => !['DRAFT', 'CANCELLED'].includes(s.status)))
      }
    } catch (error) {
      setFailure(problemOf(error))
    }
  }
  async function moreResidents() {
    try {
      const { data } = await getResidents({ query: { cursor } })
      setResidents((old) => [...old, ...data.items])
      setCursor(data.nextCursor)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }
  function update(key, name, value) {
    setRows((old) => old.map((row) => (row.key === key ? { ...row, [name]: value } : row)))
    setSuccess('')
    setRowErrors([])
  }
  async function submit(event) {
    event.preventDefault()
    if (!rows.length || busy) return
    const body = rows.map((r) => ({
      residentId: r.locationType ? undefined : optionalNumber(r.residentId),
      location: r.locationType ? { type: r.locationType, id: source.shift.floorAreaId } : undefined,
      taskTypeId: Number(r.taskTypeId),
      basis: { type: r.basisType, id: optionalNumber(r.basisId) },
      content: r.content || undefined,
      plannedAt: `${r.plannedAt}:00+07:00`,
      windowBeforeMinutes: optionalNumber(r.before),
      windowAfterMinutes: optionalNumber(r.after),
      importance: r.importance,
      performerId: r.shared ? undefined : optionalNumber(r.performerId),
      shared: !!r.shared,
    }))
    const signature = JSON.stringify(body)
    if (attempt.current?.signature !== signature) attempt.current = { signature, key: crypto.randomUUID() }
    setBusy(true)
    setFailure(null)
    setRowErrors([])
    setSuccess('')
    try {
      await postShiftsByShiftIdCareTasksPlan({
        path: { shiftId },
        body,
        headers: { 'Idempotency-Key': attempt.current.key },
      })
      setRows([])
      attempt.current = null
      setSuccess('Đã lập công việc của ca.')
      const { data } = await getShiftsByShiftIdCareTasks({ path: { shiftId } })
      setTasks(data)
    } catch (error) {
      setFailure(problemOf(error))
      setRowErrors(error.problem?.rowErrors || [])
    } finally {
      setBusy(false)
    }
  }
  if (status === 'ready' && !view) return <PermissionDeniedState label="Lập công việc của ca" />
  const people = Object.fromEntries(residents.map((r) => [r.id, r.fullName]))
  const typeOptions = Object.fromEntries(types.map((t) => [t.id, t.name]))
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Lập công việc của ca" subtitle="Đối chiếu dữ liệu nguồn, nhập từng dòng rồi gửi toàn bộ lô." />
      {!initialShiftId && (
        <form onSubmit={findShifts} className="flex flex-wrap items-end gap-3">
          <CareField
            label="Tháng của ca"
            type="month"
            required
            value={month}
            onChange={setMonth}
            disabled={busy || rows.length > 0}
          />
          <Button type="submit" variant="outline" disabled={!month || busy || rows.length > 0}>
            Tìm ca
          </Button>
          <CareField
            label="Ca đã công bố"
            value={shiftId}
            onChange={setShiftId}
            disabled={busy || rows.length > 0}
            options={Object.fromEntries(
              shifts.map((s) => [s.id, `${formatDateTime(s.startsAt)} – ${formatDateTime(s.endsAt)}`]),
            )}
          />
        </form>
      )}
      {rows.length > 0 && !initialShiftId && (
        <p className="type-body-sm text-muted-foreground">Xóa các dòng đang nhập trước khi đổi ca.</p>
      )}
      <ProblemAlert problem={failure} />
      {loading && <p role="status">Đang tải dữ liệu ca…</p>}
      {source && !loading && (
        <>
          <section aria-label="Dữ liệu nguồn" className="flex flex-col gap-3 border-b pb-5">
            <h2 className="type-section">Dữ liệu nguồn để đối chiếu</h2>
            <p>
              {formatDateTime(source.shift.startsAt)} – {formatDateTime(source.shift.endsAt)}
            </p>
            <p className="type-body-sm text-muted-foreground">Dữ liệu này không được tự chuyển thành công việc.</p>
            {source.residents.map((r) => (
              <details key={r.residentId}>
                <summary className="min-h-11 cursor-pointer py-3">
                  {people[r.residentId] || 'Hồ sơ chưa tải tên'}
                </summary>
                <ul className="flex flex-col gap-2 pl-4">
                  {r.planItems.map((i) => (
                    <li key={`plan-${i.id}`}>Mục kế hoạch: {i.content}</li>
                  ))}
                  {r.otherSources.map((i) => (
                    <li key={`${i.type}-${i.id}`}>
                      {BASIS[i.type] || 'Nguồn khác'}: {i.description} · {formatDateTime(i.at)}
                    </li>
                  ))}
                </ul>
              </details>
            ))}
          </section>
          <section aria-label="Công việc đã lập" className="flex flex-col gap-3">
            <h2 className="type-section">Công việc đã lập ({tasks.length})</h2>
            {!tasks.length && <p className="text-muted-foreground">Ca chưa có công việc.</p>}
            {tasks.map((t) => (
              <div className="flex flex-wrap items-center justify-between gap-3 border-b py-3" key={t.id}>
                <div>
                  <p>
                    {t.residentCard?.fullName || t.location} · {t.content || t.taskType}
                  </p>
                  <p className="type-body-sm text-muted-foreground">
                    {formatDateTime(t.plannedAt)} · {t.performer || 'Công việc chung'}
                  </p>
                </div>
                <StatusBadge status={STATUSES[t.status]} />
              </div>
            ))}
          </section>
          {create && (
            <form onSubmit={submit} className="flex flex-col gap-5">
              <h2 className="type-section">Các dòng đang nhập</h2>
              {!!rowErrors.length && (
                <div
                  ref={summary}
                  tabIndex={-1}
                  role="alert"
                  className="rounded-lg border p-4 focus-visible:outline-ring"
                >
                  <h3 className="type-label">Chưa lưu lô công việc</h3>
                  <ul>
                    {rowErrors.map((e) => (
                      <li key={e.row}>
                        <a className="underline" href={`#planning-row-${e.row}`}>
                          Kiểm tra dòng {e.row}
                        </a>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {!rows.length && <p>Chưa có dòng công việc.</p>}
              {rows.map((r, index) => {
                const number = index + 1
                const current = source.residents.find((v) => v.residentId === Number(r.residentId))
                const assignments = r.locationType ? shiftMembers : current?.assignments || []
                const staff = Object.fromEntries(
                  assignments.map((a) => [
                    a.staffId,
                    a.fullName || `${CARE_ROLES[a.role] || 'Nhân viên'} · mã ${a.staffId}`,
                  ]),
                )
                const basis =
                  r.basisType === 'CARE_PLAN_ITEM'
                    ? current?.planItems.map((v) => [v.id, v.content]) || []
                    : current?.otherSources
                        .filter((v) => v.type === r.basisType)
                        .map((v) => [v.id, `${v.description} · ${formatDateTime(v.at)}`]) || []
                const errors = rowErrors.find((e) => e.row === number)?.codes || []
                const set = (name) => (value) => update(r.key, name, value)
                return (
                  <fieldset
                    id={`planning-row-${number}`}
                    key={r.key}
                    tabIndex={-1}
                    disabled={busy}
                    aria-describedby={errors.length ? `row-errors-${number}` : undefined}
                    className="min-w-0 rounded-xl border bg-card p-5"
                  >
                    <legend className="type-label px-2">Dòng {number}</legend>
                    {errors.length > 0 && (
                      <ul id={`row-errors-${number}`} className="mb-4 text-destructive">
                        {errors.map((code) => (
                          <li key={code}>{ERRORS[code] || 'Dòng chưa đủ điều kiện, kiểm tra lại dữ liệu.'}</li>
                        ))}
                      </ul>
                    )}
                    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                      <CareField
                        label={`Người cao tuổi dòng ${number}`}
                        value={r.residentId}
                        onChange={set('residentId')}
                        options={people}
                        required={!r.locationType}
                        disabled={!!r.locationType}
                      />
                      <CareField
                        label={`Vị trí chung dòng ${number}`}
                        value={r.locationType}
                        onChange={set('locationType')}
                        options={{ FLOOR: 'Tầng của ca', AREA: 'Khu vực của ca' }}
                      />
                      <CareField
                        label={`Loại công việc dòng ${number}`}
                        value={r.taskTypeId}
                        onChange={set('taskTypeId')}
                        options={typeOptions}
                        required
                      />
                      <CareField
                        label={`Loại căn cứ dòng ${number}`}
                        value={r.basisType}
                        onChange={set('basisType')}
                        options={BASIS}
                        required
                      />
                      <CareField
                        label={`Căn cứ dòng ${number}`}
                        value={r.basisId}
                        onChange={set('basisId')}
                        options={Object.fromEntries(basis)}
                        required={!['SHIFT_START_CHECK', 'SUPERVISION', 'AD_HOC'].includes(r.basisType)}
                        disabled={['SHIFT_START_CHECK', 'SUPERVISION', 'AD_HOC'].includes(r.basisType)}
                      />
                      <CareField
                        label={`Thời điểm dự kiến dòng ${number}`}
                        type="datetime-local"
                        value={r.plannedAt}
                        onChange={set('plannedAt')}
                        required
                      />
                      <CareField
                        label={`Khung trước (phút) dòng ${number}`}
                        type="number"
                        min={0}
                        value={r.before}
                        onChange={set('before')}
                      />
                      <CareField
                        label={`Khung sau (phút) dòng ${number}`}
                        type="number"
                        min={0}
                        value={r.after}
                        onChange={set('after')}
                      />
                      <CareField
                        label={`Mức quan trọng dòng ${number}`}
                        value={r.importance}
                        onChange={set('importance')}
                        options={IMPORTANCE}
                        required
                      />
                      <CareField
                        label={`Người thực hiện dòng ${number}`}
                        value={r.performerId}
                        onChange={set('performerId')}
                        options={staff}
                        required={!r.shared}
                        disabled={r.shared}
                      />
                      <CareCheck
                        label={`Công việc chung dòng ${number}`}
                        checked={!!r.shared}
                        onChange={set('shared')}
                      />
                      <CareField
                        label={`Nội dung dòng ${number}`}
                        value={r.content}
                        onChange={set('content')}
                        type="textarea"
                        maxLength={1000}
                      />
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      className="mt-4"
                      onClick={() => {
                        setRows((old) => old.filter((v) => v.key !== r.key))
                        setRowErrors([])
                      }}
                    >
                      Bỏ dòng {number}
                    </Button>
                  </fieldset>
                )
              })}
              {cursor && (
                <Button type="button" variant="outline" onClick={moreResidents}>
                  Tải thêm hồ sơ để chọn
                </Button>
              )}
              <div className="flex flex-wrap gap-3">
                <Button
                  type="button"
                  variant="outline"
                  disabled={busy}
                  onClick={() => setRows((old) => [...old, { key: crypto.randomUUID(), content: '' }])}
                >
                  Thêm dòng công việc
                </Button>
                <Button type="submit" disabled={busy || !rows.length}>
                  {busy ? 'Đang kiểm tra và lưu…' : 'Lập công việc của ca'}
                </Button>
              </div>
              <p className="type-body-sm text-muted-foreground">
                Một dòng lỗi thì cả lô không được lưu. Khung để trống sẽ lấy theo căn cứ và cấu hình hiện hành.
              </p>
            </form>
          )}
        </>
      )}
      {success && <p role="status">{success}</p>}
    </div>
  )
}
