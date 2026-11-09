import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'

// Ô dùng chung trong các form đồ gửi (013): ô chọn native và ô tệp (ảnh, bản ký). Giữ cùng kiểu với ô của 001.

const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

export function SelectField({ id, label, value, onChange, options, error, placeholder = 'Chọn…', hint }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <select
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={!!error || undefined}
        className={SELECT}
      >
        <option value="">{placeholder}</option>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error ? <FieldError>{error}</FieldError> : hint && <FieldDescription>{hint}</FieldDescription>}
    </Field>
  )
}

// multiple: ảnh đồ gửi (nhiều tệp); một tệp: bản ký
export function FilesField({ id, label, files, onChange, error, hint, multiple = false, accept }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <input
        id={id}
        type="file"
        multiple={multiple}
        accept={accept ?? (multiple ? 'image/jpeg,image/png' : 'application/pdf,image/jpeg,image/png')}
        aria-invalid={!!error || undefined}
        className="type-body-sm h-11 rounded-lg border px-3 py-2"
        onChange={(e) => {
          onChange(Array.from(e.target.files ?? []))
        }}
      />
      {files.length > 0 && (
        <span className="type-body-sm text-muted-foreground">Đã chọn: {files.map((f) => f.name).join(', ')}</span>
      )}
      {error ? <FieldError>{error}</FieldError> : hint && <FieldDescription>{hint}</FieldDescription>}
    </Field>
  )
}
