import { Field, FieldDescription, FieldError, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { CONSENT_METHOD_LABELS } from './accountLabels'

// Xác nhận của chính người cao tuổi (ResidentConsent; FR-067 tạo, FR-073 mở khóa): yêu cầu hay đồng ý, nói trực tiếp
// với nhân viên nào. Mã hồ sơ nhân viên gõ tay vì /me chưa có mã hồ sơ nhân viên của người đang thao tác.

export function ResidentConsentFields({ idPrefix, consent, onChange, errors }) {
  return (
    <>
      <FieldSet data-invalid={!!errors.method || undefined}>
        <FieldLegend variant="label">Người cao tuổi đã</FieldLegend>
        <div className="flex flex-wrap gap-4">
          {Object.entries(CONSENT_METHOD_LABELS).map(([code, label]) => (
            <label key={code} className="flex min-h-11 items-center gap-2 type-body-sm">
              <input
                type="radio"
                name={`${idPrefix}-method`}
                value={code}
                checked={consent.method === code}
                onChange={() => onChange({ ...consent, method: code })}
                className="size-4 accent-primary"
              />
              {label}
            </label>
          ))}
        </div>
        {errors.method && <FieldError>{errors.method}</FieldError>}
      </FieldSet>
      <Field data-invalid={!!errors.staffId || undefined}>
        <FieldLabel htmlFor={`${idPrefix}-staff`}>Mã hồ sơ nhân viên đã nghe</FieldLabel>
        <Input
          id={`${idPrefix}-staff`}
          inputMode="numeric"
          value={consent.staffId}
          onChange={(e) => onChange({ ...consent, staffId: e.target.value })}
          aria-invalid={!!errors.staffId || undefined}
          className="h-10 max-w-48 tabular-nums"
        />
        {errors.staffId ? (
          <FieldError>{errors.staffId}</FieldError>
        ) : (
          <FieldDescription>Người cao tuổi nói trực tiếp với nhân viên này.</FieldDescription>
        )}
      </Field>
    </>
  )
}
