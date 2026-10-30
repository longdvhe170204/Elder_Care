import { useState } from 'react'
import { postDepartments, postDepartmentsByIdAssignLead } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { Choice } from './ShiftForm'
export function DepartmentPanel({ departments, staff, create, update, onDone }) {
  const [selected, setSelected] = useState(null),
    [name, setName] = useState(''),
    [lead, setLead] = useState(''),
    [reason, setReason] = useState(''),
    [failure, setFailure] = useState(null),
    [busy, setBusy] = useState(false)
  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    try {
      if (selected === 'new')
        await postDepartments({ body: { name, active: true }, headers: { 'Idempotency-Key': crypto.randomUUID() } })
      else
        await postDepartmentsByIdAssignLead({
          path: { id: selected.id },
          body: { staffId: Number(lead), reason },
          headers: { 'Idempotency-Key': crypto.randomUUID() },
        })
      setSelected(null)
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
      <div className="flex flex-wrap justify-between gap-3">
        <h2 className="type-section">Bộ phận</h2>
        {create && (
          <Button
            variant="outline"
            onClick={() => {
              setSelected('new')
              setName('')
              setFailure(null)
            }}
          >
            Thêm bộ phận
          </Button>
        )}
      </div>
      <ProblemAlert problem={failure} />
      {departments.map((d) => (
        <div key={d.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border p-3">
          <div>
            <p className="font-medium">{d.name}</p>
            <p className="text-muted-foreground">
              {d.leadStaffId
                ? `Phụ trách: ${staff.find((s) => s.id === d.leadStaffId)?.fullName ?? `Nhân viên #${d.leadStaffId}`}`
                : 'Chưa có Phụ trách bộ phận'}
              {!d.active ? ' · Ngừng sử dụng' : ''}
            </p>
          </div>
          {update && d.active && (
            <Button
              variant="outline"
              onClick={() => {
                setSelected(d)
                setLead('')
                setReason('')
                setFailure(null)
              }}
            >
              Chỉ định phụ trách {d.name}
            </Button>
          )}
        </div>
      ))}
      {selected && (
        <form onSubmit={submit} className="flex flex-col gap-4 rounded-xl border p-4">
          {selected === 'new' ? (
            <Field>
              <FieldLabel htmlFor="department-name">Tên bộ phận</FieldLabel>
              <Input
                id="department-name"
                required
                maxLength={200}
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </Field>
          ) : (
            <>
              <Choice
                id="department-lead"
                label="Người phụ trách"
                required
                value={lead}
                onChange={(e) => setLead(e.target.value)}
              >
                <option value="">Chọn nhân viên cùng bộ phận</option>
                {staff
                  .filter((s) => s.departmentId === selected.id && s.status === 'WORKING' && s.accountId)
                  .map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.fullName}
                    </option>
                  ))}
              </Choice>
              <Field>
                <FieldLabel htmlFor="department-reason">Lý do</FieldLabel>
                <Textarea
                  id="department-reason"
                  required
                  maxLength={2000}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                />
              </Field>
            </>
          )}
          <div className="flex gap-2">
            <Button type="submit" disabled={busy}>
              Lưu bộ phận
            </Button>
            <Button type="button" variant="outline" disabled={busy} onClick={() => setSelected(null)}>
              Hủy
            </Button>
          </div>
        </form>
      )}
    </section>
  )
}
