import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

// Label bao điều khiển để tên truy cập luôn có; ô bắt buộc ghi rõ ở nhãn (DESIGN.md 9.2 điểm 3).
export function ActivityField({ label, value, onChange, type = 'text', options, required, maxLength }) {
  const props = { value: value ?? '', onChange: (e) => onChange(e.target.value), required }
  return (
    <label className="flex min-w-0 flex-col gap-2">
      <span className="type-label">
        {label}
        {required && <span className="text-muted-foreground"> (bắt buộc)</span>}
      </span>
      {options ? (
        <select
          {...props}
          className="h-11 w-full rounded-lg border border-input bg-background px-3 focus-visible:outline-ring"
        >
          <option value="">Chọn…</option>
          {options.map(([code, text]) => (
            <option key={code} value={code}>
              {text}
            </option>
          ))}
        </select>
      ) : type === 'textarea' ? (
        <Textarea {...props} maxLength={maxLength} />
      ) : (
        <Input {...props} className="h-11" type={type} maxLength={maxLength} />
      )}
    </label>
  )
}
