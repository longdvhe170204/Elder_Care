import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

// Label bao điều khiển để tên truy cập được giữ cả khi form có nhiều mục lặp lại.
export function CareField({ label, value, onChange, type = 'text', options, required, maxLength, min, disabled }) {
  const props = { value: value ?? '', onChange: (e) => onChange(e.target.value), required, disabled }
  return (
    <label className="flex min-w-0 flex-col gap-2">
      <span className="type-label">{label}</span>
      {options ? (
        <select
          {...props}
          className="h-11 w-full rounded-lg border border-input bg-background px-3 focus-visible:outline-ring"
        >
          <option value="">Chọn…</option>
          {Object.entries(options).map(([code, text]) => (
            <option key={code} value={code}>
              {text}
            </option>
          ))}
        </select>
      ) : type === 'textarea' ? (
        <Textarea {...props} maxLength={maxLength} />
      ) : (
        <Input {...props} className="h-11" type={type} maxLength={maxLength} min={min} />
      )}
    </label>
  )
}

export function CareCheck({ label, checked, onChange, disabled }) {
  return (
    <label className="flex min-h-11 items-center gap-3">
      <input
        className="size-5 accent-primary"
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        disabled={disabled}
      />
      <span>{label}</span>
    </label>
  )
}
