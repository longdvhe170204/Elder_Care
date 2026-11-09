import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

// Phần dùng chung của các màn 011: dòng thông tin, ô lý do, nhóm ô chọn nhiều id, tab, gửi lệnh có Idempotency-Key.

export const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

export function Info({ label, children }) {
  return (
    <div className="flex flex-col">
      <span className="type-caption text-muted-foreground">{label}</span>
      <span className="type-body-sm tabular-nums">{children ?? '—'}</span>
    </div>
  )
}

export function ReasonField({ id, label = 'Lý do', value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Textarea
        id={id}
        rows={2}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={!!error || undefined}
      />
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

// Chọn nhiều mục danh mục (dị nguyên, chế độ ăn) bằng ô chọn
export function CheckList({ idPrefix, legend, options, value, onChange, error }) {
  function toggle(id, on) {
    onChange(on ? [...value, id] : value.filter((v) => v !== id))
  }
  return (
    <FieldSet data-invalid={!!error || undefined}>
      <FieldLegend variant="label">{legend}</FieldLegend>
      {options.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">Danh mục chưa có mục nào.</p>
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {options.map((o) => (
            <Field key={o.id} orientation="horizontal">
              <Checkbox
                id={`${idPrefix}-${o.id}`}
                checked={value.includes(o.id)}
                onCheckedChange={(checked) => toggle(o.id, checked === true)}
              />
              <FieldLabel htmlFor={`${idPrefix}-${o.id}`}>{o.name}</FieldLabel>
            </Field>
          ))}
        </div>
      )}
      {error && <FieldError>{error}</FieldError>}
    </FieldSet>
  )
}

// Tab gạch chân của màn (DESIGN.md 7.2 filter-bar)
export function Tabs({ label, tabs, value, onChange }) {
  return (
    <ToggleGroup
      variant="filter"
      size="lg"
      value={[value]}
      onValueChange={(n) => n[0] && onChange(n[0])}
      aria-label={label}
    >
      {tabs.map(([key, text]) => (
        <ToggleGroupItem key={key} value={key} className="h-11 px-4">
          {text}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  )
}
