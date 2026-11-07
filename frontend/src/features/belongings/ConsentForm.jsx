import { useRef, useState } from 'react'
import { postSelfCustodyConsents } from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { FilesField } from './BelongingFields'

// Hành chính ghi đồng ý cho tự giữ theo bản ký scan của người đại diện (013 T011; FR-015a, FR-017b, BR-M12-06; api-013
// POST /self-custody-consents). Backend kiểm người ký là người đại diện hiện tại (409 GIVER_NOT_REPRESENTATIVE). Mã người
// đại diện nhập tay vì api-013 không có danh sách người thân (như tab Đồng ý của 001).
export function ConsentForm({ belonging, onDone }) {
  const [representativeId, setRepresentativeId] = useState('')
  const [files, setFiles] = useState([])
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const key = useRef(crypto.randomUUID())

  async function submit(event) {
    event.preventDefault()
    const found = {}
    if (!/^[1-9]\d*$/.test(representativeId)) found.representativeId = 'Nhập mã người đại diện.'
    if (files.length === 0) found.files = 'Chọn tệp bản ký.'
    setErrors(found)
    setFailure(null)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    try {
      const signedFormFileId = await uploadFile(files[0])
      await postSelfCustodyConsents({
        headers: { 'Idempotency-Key': key.current },
        body: { belongingId: belonging.id, representativeId: Number(representativeId), signedFormFileId },
      })
      onDone('Đã ghi đồng ý cho tự giữ.')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <FieldGroup>
        <Field data-invalid={!!errors.representativeId || undefined}>
          <FieldLabel htmlFor="consent-representative">Mã người đại diện</FieldLabel>
          <Input
            id="consent-representative"
            inputMode="numeric"
            className="h-11"
            value={representativeId}
            onChange={(e) => setRepresentativeId(e.target.value)}
            aria-invalid={!!errors.representativeId || undefined}
          />
          {errors.representativeId && <FieldError>{errors.representativeId}</FieldError>}
        </Field>
        <FilesField
          id="consent-file"
          label="Bản ký của người đại diện"
          files={files}
          onChange={setFiles}
          error={errors.files}
          hint="PDF, JPEG hoặc PNG, tối đa 10 MB."
        />
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <Button type="submit" className="h-11 self-start" disabled={busy}>
        Lưu đồng ý
      </Button>
    </form>
  )
}
