import { useEffect, useState } from 'react'
import { getResidents } from '@/api'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { SELECT } from './admissionLabels'

// Thành phần form dùng chung trong features/admissions (004): chọn một trong vài giá trị, dòng thông tin, ô lý do,
// chọn người cao tuổi. Không dùng chung với feature khác (frontend/README mục 4 điều 1).

export function Choices({ label, options, value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel>{label}</FieldLabel>
      <ToggleGroup
        variant="outline"
        value={value ? [value] : []}
        onValueChange={(next) => next[0] && onChange(next[0])}
        aria-label={label}
        className="flex-wrap"
      >
        {options.map(([code, text]) => (
          <ToggleGroupItem key={code} value={code} className="h-11 px-4">
            {text}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

export function Info({ label, children }) {
  return (
    <div className="flex flex-col">
      <span className="type-caption text-muted-foreground">{label}</span>
      <span className="type-body-sm tabular-nums">{children}</span>
    </div>
  )
}

export function ReasonField({ id, value, onChange, error, label = 'Lý do' }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Textarea
        id={id}
        rows={2}
        maxLength={2000}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={!!error || undefined}
      />
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

// Chọn người cao tuổi theo trạng thái (api-001 GET /residents); không đọc được thì chỉ có ô trống
export function ResidentSelect({ id, label = 'Người cao tuổi', statuses, value, onChange, error, onFailure }) {
  const [residents, setResidents] = useState(null)

  useEffect(() => {
    let cancelled = false
    getResidents({ query: statuses ? { status: statuses } : {} })
      .then(({ data }) => !cancelled && setResidents(data.items))
      .catch((e) => {
        if (!cancelled) {
          setResidents([])
          onFailure?.(e)
        }
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <select
        id={id}
        className={SELECT}
        value={value}
        aria-invalid={!!error || undefined}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">{residents === null ? 'Đang tải…' : 'Chọn người cao tuổi'}</option>
        {(residents ?? []).map((r) => (
          <option key={r.id} value={r.id}>
            {`${r.personalInfo?.fullName ?? r.fullName ?? `Mã ${r.id}`}${r.profileCode ? ` (${r.profileCode})` : ''}`}
          </option>
        ))}
      </select>
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}
