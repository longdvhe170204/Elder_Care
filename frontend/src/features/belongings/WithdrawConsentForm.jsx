import { useRef, useState } from 'react'
import { postSelfCustodyConsentsByIdWithdraw } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { FilesField } from './BelongingFields'

// Hành chính rút đồng ý cho tự giữ theo bản ký scan của người đại diện (FR-015a; api-013
// POST /self-custody-consents/{id}/withdraw). Mã bản đồng ý lấy từ selfCustodyConsent của chi tiết đồ; người đại diện
// điền sẵn theo bản đồng ý, sửa được nếu người ký rút là người đại diện hiện tại khác.
export function WithdrawConsentForm({ consent, onDone }) {
  const [reason, setReason] = useState('')
  const [representativeId, setRepresentativeId] = useState(String(consent.representativeId))
  const [files, setFiles] = useState([])
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const key = useRef(crypto.randomUUID())

  async function submit(event) {
    event.preventDefault()
    const found = {}
    if (!reason.trim()) found.reason = 'Nhập lý do rút đồng ý.'
    if (!/^[1-9]\d*$/.test(representativeId)) found.representativeId = 'Nhập mã người đại diện.'
    if (files.length === 0) found.files = 'Chọn tệp bản ký.'
    setErrors(found)
    setFailure(null)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    try {
      const signedFormFileId = await uploadFile(files[0])
      await postSelfCustodyConsentsByIdWithdraw({
        path: { id: consent.id },
        headers: { 'Idempotency-Key': key.current },
        body: { reason: reason.trim(), representativeId: Number(representativeId), signedFormFileId },
      })
      onDone('Đã rút đồng ý cho tự giữ.')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <FieldGroup>
        <Field data-invalid={!!errors.reason || undefined}>
          <FieldLabel htmlFor="withdraw-reason">Lý do rút đồng ý</FieldLabel>
          <Input
            id="withdraw-reason"
            className="h-11"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!errors.reason || undefined}
          />
          {errors.reason && <FieldError>{errors.reason}</FieldError>}
        </Field>
        <Field data-invalid={!!errors.representativeId || undefined}>
          <FieldLabel htmlFor="withdraw-representative">Mã người đại diện</FieldLabel>
          <Input
            id="withdraw-representative"
            inputMode="numeric"
            className="h-11"
            value={representativeId}
            onChange={(e) => setRepresentativeId(e.target.value)}
            aria-invalid={!!errors.representativeId || undefined}
          />
          {errors.representativeId && <FieldError>{errors.representativeId}</FieldError>}
        </Field>
        <FilesField
          id="withdraw-file"
          label="Bản ký của người đại diện"
          files={files}
          onChange={setFiles}
          error={errors.files}
          hint="PDF, JPEG hoặc PNG, tối đa 10 MB."
        />
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11 self-start" disabled={busy}>
        Rút đồng ý
      </Button>
    </form>
  )
}
