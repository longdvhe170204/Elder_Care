import { useState } from 'react'
import {
  getStaffById,
  postStaff,
  putStaffById,
  postStaffByIdTerminateEmployment,
  postStaffByIdRehire,
  postStaffByIdChangeDepartment,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { Choice } from './ShiftForm'
export function StaffProfileForm({ person, etag, departments, onDone }) {
  const [value, setValue] = useState(person ?? { fullName: '', position: '' }),
    [match, setMatch] = useState(etag),
    [failure, setFailure] = useState(null),
    [busy, setBusy] = useState(false)
  const change = (key, next) => setValue((v) => ({ ...v, [key]: next }))
  async function reload() {
    setBusy(true)
    try {
      const r = await getStaffById({ path: { id: person.id } })
      setValue(r.data)
      setMatch(r.etag)
      setFailure(null)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }
  async function save(e) {
    e.preventDefault()
    setBusy(true)
    try {
      const body = Object.fromEntries(
        ['fullName', 'position', 'specialty', 'startDate', 'phoneNumber', 'email', 'departmentId'].map((k) => [
          k,
          value[k] || undefined,
        ]),
      )
      const headers = { 'Idempotency-Key': crypto.randomUUID(), ...(person && { 'If-Match': match }) }
      await (person ? putStaffById : postStaff)({ path: { id: person?.id }, headers, body })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form onSubmit={save} className="flex flex-col gap-4">
      <h3 className="type-section">{person ? 'Sửa hồ sơ' : 'Thêm nhân viên'}</h3>
      <ProblemAlert problem={failure} />
      {failure?.status === 412 && (
        <Button type="button" onClick={reload} disabled={busy}>
          Tải lại hồ sơ
        </Button>
      )}
      {[
        ['fullName', 'Họ tên', 'text', true],
        ['position', 'Chức danh', 'text', true],
        ['specialty', 'Chuyên môn', 'text'],
        ['startDate', 'Ngày bắt đầu làm việc', 'date'],
        ['phoneNumber', 'Số điện thoại', 'tel'],
        ['email', 'Email', 'email'],
      ].map(([key, label, type, required]) => (
        <Field key={key}>
          <FieldLabel htmlFor={`staff-${key}`}>{label}</FieldLabel>
          <Input
            id={`staff-${key}`}
            type={type}
            required={required}
            maxLength={key === 'fullName' || key === 'position' ? 200 : 255}
            value={value[key] ?? ''}
            onChange={(e) => change(key, e.target.value)}
          />
        </Field>
      ))}
      {!person && (
        <Choice
          label="Bộ phận"
          id="staff-department"
          value={value.departmentId ?? ''}
          onChange={(e) => change('departmentId', e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Chưa có bộ phận</option>
          {departments
            .filter((d) => d.active)
            .map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
        </Choice>
      )}
      <Button type="submit" disabled={busy || failure?.status === 412}>
        {busy ? 'Đang lưu…' : 'Lưu hồ sơ'}
      </Button>
    </form>
  )
}
export function StaffCommandForm({ kind, person, staff, departments, onDone }) {
  const [reason, setReason] = useState(''),
    [at, setAt] = useState(''),
    [department, setDepartment] = useState(''),
    [replacement, setReplacement] = useState(''),
    [failure, setFailure] = useState(null),
    [busy, setBusy] = useState(false)
  const titles = { terminate: 'Cho nghỉ việc', rehire: 'Nhận lại làm việc', department: 'Đổi bộ phận' }
  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    try {
      const body = {
        reason,
        ...(kind === 'terminate' && { effectiveAt: `${at}:00+07:00` }),
        ...(kind === 'department' && { departmentId: Number(department) }),
        ...(replacement && { replacementStaffId: Number(replacement) }),
      }
      const action =
        kind === 'terminate'
          ? postStaffByIdTerminateEmployment
          : kind === 'rehire'
            ? postStaffByIdRehire
            : postStaffByIdChangeDepartment
      await action({ path: { id: person.id }, body, headers: { 'Idempotency-Key': crypto.randomUUID() } })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <h3 className="type-section">{titles[kind]}</h3>
      <ProblemAlert problem={failure} />
      {kind === 'terminate' && (
        <>
          <p className="text-muted-foreground">
            Tại mốc hiệu lực: khóa tài khoản, rút khỏi ca và kết thúc phân công. Chọn thời điểm từ hiện tại trở đi theo
            giờ Việt Nam.
          </p>
          <Field>
            <FieldLabel htmlFor="employment-at">Hiệu lực từ</FieldLabel>
            <Input
              id="employment-at"
              required
              type="datetime-local"
              value={at}
              onChange={(e) => setAt(e.target.value)}
            />
          </Field>
        </>
      )}
      {kind === 'rehire' && <p>Tài khoản và phân công cũ không tự khôi phục.</p>}
      {kind === 'department' && (
        <Choice
          label="Bộ phận mới"
          id="next-department"
          required
          value={department}
          onChange={(e) => setDepartment(e.target.value)}
        >
          <option value="">Chọn bộ phận</option>
          {departments
            .filter((d) => d.active && d.id !== person.departmentId)
            .map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
        </Choice>
      )}
      {kind !== 'rehire' && (
        <Choice
          label="Người thay Phụ trách bộ phận"
          id="replacement-staff"
          value={replacement}
          onChange={(e) => setReplacement(e.target.value)}
        >
          <option value="">Chọn nếu đang phụ trách bộ phận</option>
          {staff
            .filter(
              (s) =>
                s.id !== person.id && s.status === 'WORKING' && s.departmentId === person.departmentId && s.accountId,
            )
            .map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName}
              </option>
            ))}
        </Choice>
      )}
      <Field>
        <FieldLabel htmlFor="staff-command-reason">Lý do</FieldLabel>
        <Textarea
          id="staff-command-reason"
          required
          maxLength={2000}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </Field>
      <Button disabled={busy} type="submit">
        {busy
          ? 'Đang lưu…'
          : kind === 'terminate'
            ? 'Xác nhận cho nghỉ việc'
            : kind === 'rehire'
              ? 'Xác nhận nhận lại'
              : 'Xác nhận đổi bộ phận'}
      </Button>
    </form>
  )
}
