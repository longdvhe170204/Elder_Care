import { useEffect, useState } from 'react'
import { getStaff } from '@/api'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'

// Nhân viên bàn giao khi Chuyển viện (từ sự cố hoặc lựa chọn nguy kịch): lệnh Chuyển viện của 001/004 bắt buộc người bàn
// giao (quickstart 007 Q55, 2026-11-12). Danh sách nhân viên đang làm việc mà người ghi xem được (api-008), như màn ghi đón.
export function HandoverStaffField({ id, value, onChange, invalid }) {
  const [staff, setStaff] = useState(null)
  useEffect(() => {
    let live = true
    getStaff({ query: { status: 'WORKING' } })
      .then(({ data }) => live && setStaff(data ?? []))
      .catch(() => live && setStaff([]))
    return () => {
      live = false
    }
  }, [])
  return (
    <Field data-invalid={invalid || undefined}>
      <FieldLabel htmlFor={id}>Nhân viên bàn giao</FieldLabel>
      <select
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={invalid || undefined}
        className="h-11 w-full rounded-md border border-input bg-transparent px-3"
      >
        <option value="">{staff === null ? 'Đang tải danh sách nhân viên' : 'Chưa chọn'}</option>
        {(staff ?? []).map((s) => (
          <option key={s.id} value={String(s.id)}>
            {s.fullName}
          </option>
        ))}
      </select>
      {invalid && <FieldError>Chọn nhân viên bàn giao</FieldError>}
    </Field>
  )
}
