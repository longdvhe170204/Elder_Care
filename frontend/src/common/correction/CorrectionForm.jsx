import { useState } from 'react'
import { ApiError, postCorrectionsByRecordTypeByOriginalRecordId } from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'

// Form tạo bản đính chính (000 FR-023 → FR-026, FR-029; hợp đồng POST /corrections/{recordType}/{originalRecordId}).
// Kiểm tra trên màn chỉ để báo lỗi tại ô; quyền, cách tạo A/B/C và nội dung do backend kiểm.

const TYPES = [
  { value: 'AMEND_CONTENT', label: 'Sửa nội dung' },
  { value: 'VOID', label: 'Hủy ghi nhận' },
]

const REASON_MAX = 2000

function emptyContent(fields) {
  return Object.fromEntries(fields.map((f) => [f.name, '']))
}

// Lỗi 422 báo tại ô (DESIGN.md 7.4): lý do ở ô Lý do, còn lại ở ô nội dung khi đang sửa nội dung.
function fieldErrorOf(error, type) {
  if (error.status !== 422) return null
  const message = error.problem?.detail || error.problem?.title || 'Giá trị không hợp lệ'
  if (error.problem?.errorCode === 'REASON_REQUIRED') return { reason: message }
  if (type === 'AMEND_CONTENT') return { content: message }
  return null
}

export function CorrectionForm({ recordType, recordId, fields, onSaved }) {
  const [type, setType] = useState('AMEND_CONTENT')
  const [content, setContent] = useState(() => emptyContent(fields))
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [notice, setNotice] = useState('')

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    setNotice('')
    if (!reason.trim()) {
      setFieldErrors({ reason: 'Nhập lý do đính chính' })
      return
    }
    setFieldErrors({})
    setSubmitting(true)
    const body = type === 'AMEND_CONTENT' ? { type, correctedContent: content, reason } : { type, reason }
    try {
      const { data, status } = await postCorrectionsByRecordTypeByOriginalRecordId({
        path: { recordType, originalRecordId: recordId },
        body,
      })
      setContent(emptyContent(fields))
      setReason('')
      // 202: cách C, đã lập yêu cầu phê duyệt; bản gốc chưa được đánh dấu đính chính (US2 #4)
      if (status === 202) {
        setNotice(`Đã gửi duyệt, mã yêu cầu ${data.displayCode}. Bản đính chính chỉ được lưu khi Quản lý viện duyệt.`)
      } else {
        setNotice('Đã lưu bản đính chính.')
        onSaved()
      }
    } catch (error) {
      const atField = error instanceof ApiError ? fieldErrorOf(error, type) : null
      if (atField) setFieldErrors(atField)
      // Lỗi mạng hay lỗi không có problem thì vẫn báo bằng tiêu đề chung
      else setFailure((error instanceof ApiError && error.problem) || {})
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4">
      <FieldGroup>
        <Field>
          <FieldLabel>Loại đính chính</FieldLabel>
          <ToggleGroup
            variant="outline"
            value={[type]}
            onValueChange={(next) => next[0] && setType(next[0])}
            className="grid w-full grid-cols-2"
          >
            {TYPES.map((t) => (
              <ToggleGroupItem key={t.value} value={t.value} className="h-11">
                {t.label}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          {type === 'VOID' && (
            <FieldDescription>
              Bản ghi vẫn được giữ và xem lại được, nhưng không còn tính vào thống kê về sau.
            </FieldDescription>
          )}
        </Field>

        {type === 'AMEND_CONTENT' &&
          fields.map((f, index) => {
            const invalid = index === 0 && !!fieldErrors.content
            return (
              <Field key={f.name} data-invalid={invalid || undefined}>
                <FieldLabel htmlFor={`correction-${f.name}`}>{f.label}</FieldLabel>
                <Textarea
                  id={`correction-${f.name}`}
                  rows={3}
                  value={content[f.name]}
                  onChange={(e) => setContent({ ...content, [f.name]: e.target.value })}
                  aria-invalid={invalid || undefined}
                />
                {invalid && <FieldError>{fieldErrors.content}</FieldError>}
              </Field>
            )
          })}

        <Field data-invalid={!!fieldErrors.reason || undefined}>
          <FieldLabel htmlFor="correction-reason">Lý do</FieldLabel>
          <Textarea
            id="correction-reason"
            rows={3}
            maxLength={REASON_MAX}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!fieldErrors.reason || undefined}
          />
          {fieldErrors.reason ? (
            <FieldError>{fieldErrors.reason}</FieldError>
          ) : (
            <FieldDescription>Bắt buộc. Ghi sai ở đâu và vì sao.</FieldDescription>
          )}
        </Field>
      </FieldGroup>

      {failure && (
        <Alert variant="attention">
          <AlertTitle>{failure.title || 'Không lưu được bản đính chính'}</AlertTitle>
          {(failure.detail || failure.unmetConditions?.length > 0) && (
            <AlertDescription>
              {failure.detail && <p>{failure.detail}</p>}
              {failure.unmetConditions?.length > 0 && (
                <ul className="ml-4 list-disc">
                  {failure.unmetConditions.map((c) => (
                    <li key={c.code}>{c.description}</li>
                  ))}
                </ul>
              )}
            </AlertDescription>
          )}
        </Alert>
      )}

      <p role="status" className="type-body-sm">
        {notice}
      </p>

      <Button type="submit" className="h-11" disabled={submitting}>
        Lưu đính chính
      </Button>
    </form>
  )
}
