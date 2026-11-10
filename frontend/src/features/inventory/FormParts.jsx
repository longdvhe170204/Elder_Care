import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

// Phần form dùng chung của các màn 019 (yêu cầu mua sắm, tài sản, lịch xe): ô nhập cao 44px, nhóm nút chọn, lý do.

export const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

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

export function TextField({ id, label, value, onChange, error, type = 'text', ...rest }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        type={type}
        className="h-11"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={!!error || undefined}
        {...rest}
      />
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

export function ReasonField({ id, value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>Lý do</FieldLabel>
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
