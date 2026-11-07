import { useState } from 'react'
import { postRightChangeRequestsByIdByCommand } from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import {
  CONFIRMATION_LABELS,
  REQUEST_HINTS,
  REQUEST_SOURCE_LABELS,
  REQUEST_STATUS_LABELS,
  describeRequest,
} from './requestLabels'

// Khung chi tiết một yêu cầu thay đổi quyền (012 T017; FR-015 → FR-020b). Lệnh của nhân viên: Hành chính ghi nhận bản
// ký của người đại diện (người ký, bản scan), Quản lý viện duyệt hoặc từ chối kèm lý do, người lập hủy kèm lý do. Yêu
// cầu trái mong muốn của người cao tuổi chỉ Quản lý viện duyệt, kèm nội dung đã trao đổi (DESIGN.md 2.2, visit-wishes).

const COMMANDS = {
  confirm: { label: 'Ghi nhận bản ký', submit: 'Xác nhận theo bản ký', done: 'Đã ghi nhận bản ký' },
  approve: { label: 'Duyệt', submit: 'Xác nhận duyệt', done: 'Đã duyệt yêu cầu', reason: true },
  reject: { label: 'Từ chối', submit: 'Xác nhận từ chối', done: 'Đã từ chối yêu cầu', reason: true },
  cancel: { label: 'Hủy yêu cầu', submit: 'Xác nhận hủy yêu cầu', done: 'Đã hủy yêu cầu', reason: true },
}

const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

function CommandForm({ request, command, signers, onDone, onCancel }) {
  const spec = COMMANDS[command]
  const conflict = request.conflictingWishId != null
  const [key] = useState(() => crypto.randomUUID())
  const [reason, setReason] = useState('')
  const [discussed, setDiscussed] = useState('')
  const [signer, setSigner] = useState('')
  const [scan, setScan] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  const reasonMissing = spec.reason && !reason.trim()
  const discussedMissing = command === 'approve' && conflict && !discussed.trim()
  const signerMissing = command === 'confirm' && !signer
  const scanMissing = command === 'confirm' && !scan

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (reasonMissing || discussedMissing || signerMissing || scanMissing) return
    setBusy(true)
    try {
      const body = {}
      if (spec.reason) body.reason = reason.trim()
      if (command === 'approve' && conflict) body.discussedWithResident = discussed.trim()
      if (command === 'confirm') {
        body.signedByFamilyMemberId = Number(signer)
        body.evidenceFileId = await uploadFile(scan)
      }
      await postRightChangeRequestsByIdByCommand({
        path: { id: request.id, command },
        headers: { 'Idempotency-Key': key },
        body,
      })
      onDone(spec.done)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-lg border p-4">
      <h3 className="type-card-title">{spec.label}</h3>
      {command === 'confirm' && (
        <>
          <Field data-invalid={(submitted && signerMissing) || undefined}>
            <FieldLabel htmlFor="request-signer">Người đại diện ký</FieldLabel>
            <select
              id="request-signer"
              value={signer}
              onChange={(e) => setSigner(e.target.value)}
              aria-invalid={(submitted && signerMissing) || undefined}
              className={SELECT}
            >
              <option value="">Chọn người đại diện</option>
              {signers.map((r) => (
                <option key={r.id} value={r.familyMember.id}>
                  {r.familyMember.fullName}
                </option>
              ))}
            </select>
            {submitted && signerMissing && <FieldError>Chọn người đại diện đã ký</FieldError>}
          </Field>
          <Field data-invalid={(submitted && scanMissing) || undefined}>
            <FieldLabel htmlFor="request-scan">Bản scan bản ký</FieldLabel>
            <input
              id="request-scan"
              type="file"
              accept="application/pdf,image/jpeg,image/png"
              className="type-body-sm h-11 rounded-lg border px-3 py-2"
              aria-invalid={(submitted && scanMissing) || undefined}
              onChange={(e) => setScan(e.target.files?.[0] ?? null)}
            />
            {submitted && scanMissing && <FieldError>Chọn bản scan bản ký</FieldError>}
          </Field>
        </>
      )}
      {spec.reason && (
        <Field data-invalid={(submitted && reasonMissing) || undefined}>
          <FieldLabel htmlFor="request-reason">Lý do</FieldLabel>
          <Textarea
            id="request-reason"
            rows={3}
            maxLength={2000}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={(submitted && reasonMissing) || undefined}
          />
          {submitted && reasonMissing && <FieldError>Nhập lý do</FieldError>}
        </Field>
      )}
      {command === 'approve' && conflict && (
        <Field data-invalid={(submitted && discussedMissing) || undefined}>
          <FieldLabel htmlFor="request-discussed">Nội dung đã trao đổi với người cao tuổi</FieldLabel>
          <Textarea
            id="request-discussed"
            rows={3}
            maxLength={2000}
            value={discussed}
            onChange={(e) => setDiscussed(e.target.value)}
            aria-invalid={(submitted && discussedMissing) || undefined}
          />
          {submitted && discussedMissing && <FieldError>Ghi nội dung đã trao đổi với người cao tuổi</FieldError>}
        </Field>
      )}
      <ProblemAlert problem={failure}>
        {failure && REQUEST_HINTS[failure.errorCode] && <p>{REQUEST_HINTS[failure.errorCode]}</p>}
      </ProblemAlert>
      <div className="flex flex-wrap gap-3">
        <Button
          type="submit"
          className="h-11"
          variant={command === 'reject' || command === 'cancel' ? 'destructive' : 'default'}
          disabled={busy}
        >
          {spec.submit}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Đóng form
        </Button>
      </div>
    </form>
  )
}

export function RightChangeRequestDetail({ request, relationships, can, onChanged, Title, Description }) {
  const [command, setCommand] = useState(null)
  const pending = request.status === 'PENDING_CONFIRMATION'
  const conflict = request.conflictingWishId != null
  const nominee =
    request.type === 'SET_REPRESENTATIVE' || request.type === 'REMOVE_REPRESENTATIVE' ? request.relationshipId : null
  const signers = relationships.filter((r) => r.status === 'ACTIVE' && r.representative && r.id !== nominee)

  const commands = !pending
    ? []
    : [
        !conflict && can('UPDATE') && 'confirm',
        can('APPROVE') && 'approve',
        can('APPROVE') && 'reject',
        can('CREATE') && 'cancel',
      ].filter(Boolean)

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        <Title className="type-title">{describeRequest(request, relationships)}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {REQUEST_SOURCE_LABELS[request.source]}
        </Description>
        <StatusBadge status={REQUEST_STATUS_LABELS[request.status] ?? request.status} />
      </div>

      {pending && conflict && (
        <Alert variant="attention">
          <AlertTitle>Trái mong muốn của người cao tuổi</AlertTitle>
          <AlertDescription>
            Yêu cầu chờ Quản lý viện duyệt; khi duyệt cần ghi nội dung đã trao đổi với người cao tuổi.
          </AlertDescription>
        </Alert>
      )}

      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 type-body-sm">
        <dt className="text-muted-foreground">Lý do</dt>
        <dd>{request.reason}</dd>
        {request.newPicker?.fullName && (
          <>
            <dt className="text-muted-foreground">Người đón mới</dt>
            <dd>
              {[request.newPicker.fullName, request.newPicker.idDocumentType, request.newPicker.idDocumentNumber]
                .filter(Boolean)
                .join(', ')}
            </dd>
          </>
        )}
        {request.confirmationMethod && (
          <>
            <dt className="text-muted-foreground">Cách có hiệu lực</dt>
            <dd>{CONFIRMATION_LABELS[request.confirmationMethod]}</dd>
          </>
        )}
        {pending && request.appliedOnCreation && (
          <>
            <dt className="text-muted-foreground">Áp dụng</dt>
            <dd>Đã áp dụng khi lập; nếu bị từ chối hoặc hủy, quyền được khôi phục từ lúc có kết quả.</dd>
          </>
        )}
      </dl>

      {command ? (
        <CommandForm
          key={command}
          request={request}
          command={command}
          signers={signers}
          onCancel={() => setCommand(null)}
          onDone={(message) => {
            setCommand(null)
            onChanged(message)
          }}
        />
      ) : (
        commands.length > 0 && (
          <div className="flex flex-wrap gap-3">
            {commands.map((c) => (
              <Button
                key={c}
                variant={c === 'reject' || c === 'cancel' ? 'destructive' : 'outline'}
                className="h-11"
                onClick={() => setCommand(c)}
              >
                {COMMANDS[c].label}
              </Button>
            ))}
          </div>
        )
      )}
    </div>
  )
}
