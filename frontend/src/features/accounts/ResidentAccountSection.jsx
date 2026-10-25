import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import {
  ApiError,
  postAccountsByIdLockResident,
  postAccountsByIdLoginQrCode,
  postAccountsByIdUnbindDevice,
  postAccountsByIdUnlockResident,
  postApprovalRequests,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { RESIDENT_COMMANDS, RESIDENT_LOCK_BASIS_LABELS } from './accountLabels'
import { ResidentConsentFields } from './ResidentConsentFields'
import { EMPTY_CONSENT, consentBody, consentErrors } from './residentConsent'

// Phần tài khoản người cao tuổi trong khung chi tiết (002 T095; US8; FR-069 → FR-073, A-16; bố cục duyệt 2026-10-24):
// thiết bị đang gắn, mã QR đăng nhập dùng một lần kèm thời gian còn lại, lệnh theo allowedCommands mở form ngay dưới,
// đề nghị khóa theo lời người thân là yêu cầu phê duyệt M15.RESIDENT_ACCOUNT_LOCK do Quản lý viện duyệt.

const REASON_MAX = 2000
const FAMILY_REQUEST = 'family-request'

function remaining(expiresAt, now) {
  const seconds = Math.max(0, Math.ceil((new Date(expiresAt).getTime() - now) / 1000))
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

function vietnamToday() {
  return new Date(Date.now() + 7 * 3600 * 1000).toISOString().slice(0, 10)
}

export function ResidentAccountSection({ account, etag, onChanged }) {
  const [command, setCommand] = useState(null)
  const [qr, setQr] = useState(null)
  const [qrFailure, setQrFailure] = useState(null)
  const [now, setNow] = useState(() => Date.now())
  const allowed = (account.allowedCommands ?? []).filter((c) => RESIDENT_COMMANDS[c])

  useEffect(() => {
    if (!qr) return undefined
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [qr])

  async function createQr() {
    setQrFailure(null)
    try {
      const { data } = await postAccountsByIdLoginQrCode({
        path: { id: account.id },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      })
      setNow(Date.now())
      setQr(data)
    } catch (error) {
      setQrFailure(problemOf(error))
    }
  }

  const expired = qr && new Date(qr.expiresAt).getTime() <= now

  return (
    <section aria-label="Đăng nhập trên app" className="flex flex-col gap-3 border-t pt-4">
      <h3 className="type-card-title">Đăng nhập trên app</h3>
      <p className="type-body-sm">
        {account.boundDevice ? `Thiết bị: Đã gắn ${formatDateTime(account.boundDevice.boundAt)}` : 'Chưa gắn thiết bị'}
      </p>

      {qr && (
        <div className="flex flex-wrap items-center gap-4 rounded-xl border p-3">
          {expired ? (
            <p className="type-body-sm">Mã đã hết hạn. Tạo mã mới.</p>
          ) : (
            <>
              <QRCodeSVG value={qr.code} size={168} role="img" aria-label="Mã QR đăng nhập" />
              <div className="flex flex-col gap-1 type-body-sm">
                <span>Mã dùng một lần</span>
                <span className="tabular-nums text-warning-foreground">{`Còn ${remaining(qr.expiresAt, now)}`}</span>
                <span className="text-muted-foreground">Tạo mã mới làm mã cũ hết hiệu lực.</span>
              </div>
            </>
          )}
        </div>
      )}
      <ProblemAlert problem={qrFailure} />

      {command ? (
        <ResidentCommandForm
          key={command}
          command={command}
          account={account}
          etag={etag}
          onDone={() => {
            if (command !== FAMILY_REQUEST) {
              setCommand(null)
              onChanged()
            }
          }}
          onCancel={() => setCommand(null)}
          onStale={() => {
            setCommand(null)
            onChanged()
          }}
        />
      ) : (
        <div className="flex flex-wrap gap-2">
          {allowed.map((c) =>
            c === 'login-qr-code' ? (
              <Button key={c} onClick={createQr}>
                {RESIDENT_COMMANDS[c].label}
              </Button>
            ) : (
              <Button key={c} variant="outline" onClick={() => setCommand(c)}>
                {RESIDENT_COMMANDS[c].label}
              </Button>
            ),
          )}
          {allowed.includes('lock-resident') && (
            <Button
              variant="outline"
              className="h-auto min-h-10 max-w-full shrink py-2 whitespace-normal text-left"
              onClick={() => setCommand(FAMILY_REQUEST)}
            >
              Lập đề nghị khóa theo lời người thân
            </Button>
          )}
        </div>
      )}
    </section>
  )
}

function ResidentCommandForm({ command, account, etag, onDone, onCancel, onStale }) {
  const [reason, setReason] = useState('')
  const [basis, setBasis] = useState('')
  const [consent, setConsent] = useState(EMPTY_CONSENT)
  const [familyMemberId, setFamilyMemberId] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)
  const [requestId, setRequestId] = useState(null)
  const [key] = useState(() => crypto.randomUUID())

  const needsReason = command !== 'unlock-resident'
  const title = command === FAMILY_REQUEST ? 'Đề nghị khóa theo lời người thân' : RESIDENT_COMMANDS[command].label
  const submitLabel =
    command === FAMILY_REQUEST ? 'Gửi Quản lý viện duyệt' : RESIDENT_COMMANDS[command].submit

  function send() {
    const path = { id: account.id }
    switch (command) {
      case 'unbind-device':
        return postAccountsByIdUnbindDevice({ path, headers: { 'If-Match': etag }, body: { reason } })
      case 'lock-resident':
        return postAccountsByIdLockResident({ path, headers: { 'If-Match': etag }, body: { basis, reason } })
      case 'unlock-resident':
        return postAccountsByIdUnlockResident({
          path,
          headers: { 'If-Match': etag },
          body: { consent: consentBody(consent) },
        })
      default:
        return postApprovalRequests({
          headers: { 'Idempotency-Key': key },
          body: {
            type: 'M15.RESIDENT_ACCOUNT_LOCK',
            content: { accountId: account.id, requestingFamilyMemberId: Number(familyMemberId.trim()), reason },
            requestedEffectiveDate: vietnamToday(),
            reason,
            submitImmediately: true,
          },
        })
    }
  }

  async function submit(event) {
    event.preventDefault()
    setFailure(null)
    const next = command === 'unlock-resident' ? consentErrors(consent) : {}
    if (command === 'lock-resident' && !basis) next.basis = 'Chọn căn cứ khóa'
    if (command === FAMILY_REQUEST && !/^\d+$/.test(familyMemberId.trim())) {
      next.familyMemberId = 'Nhập mã hồ sơ người thân'
    }
    if (needsReason && !reason.trim()) next.reason = 'Nhập lý do'
    setErrors(next)
    if (Object.keys(next).length > 0) return

    setSaving(true)
    try {
      const { data } = await send()
      if (command === FAMILY_REQUEST) setRequestId(data.id)
      onDone()
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422 && needsReason) {
        setErrors({ reason: p.detail || p.title })
      } else {
        setFailure(p)
      }
    } finally {
      setSaving(false)
    }
  }

  if (requestId !== null) {
    return (
      <div className="flex flex-col gap-3 rounded-xl border p-4">
        <p className="type-body-sm">{`Đã gửi Quản lý viện duyệt, mã yêu cầu ${requestId}. Tài khoản chỉ khóa khi được duyệt.`}</p>
        <Button variant="outline" className="self-start" onClick={onCancel}>
          Xong
        </Button>
      </div>
    )
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4 rounded-xl border p-4">
      <h3 className="type-card-title">{title}</h3>
      <FieldGroup>
        {command === 'lock-resident' && (
          <FieldSet data-invalid={!!errors.basis || undefined}>
            <FieldLegend variant="label">Căn cứ</FieldLegend>
            <div className="flex flex-wrap gap-4">
              {Object.entries(RESIDENT_LOCK_BASIS_LABELS).map(([code, label]) => (
                <label key={code} className="flex min-h-11 items-center gap-2 type-body-sm">
                  <input
                    type="radio"
                    name="resident-lock-basis"
                    value={code}
                    checked={basis === code}
                    onChange={() => setBasis(code)}
                    className="size-4 accent-primary"
                  />
                  {label}
                </label>
              ))}
            </div>
            {errors.basis && <FieldError>{errors.basis}</FieldError>}
          </FieldSet>
        )}

        {command === 'unlock-resident' && (
          <ResidentConsentFields idPrefix="resident-unlock" consent={consent} onChange={setConsent} errors={errors} />
        )}

        {command === FAMILY_REQUEST && (
          <Field data-invalid={!!errors.familyMemberId || undefined}>
            <FieldLabel htmlFor="resident-family-request-member">Mã hồ sơ người thân đề nghị</FieldLabel>
            <Input
              id="resident-family-request-member"
              inputMode="numeric"
              value={familyMemberId}
              onChange={(e) => setFamilyMemberId(e.target.value)}
              aria-invalid={!!errors.familyMemberId || undefined}
              className="h-10 max-w-48 tabular-nums"
            />
            {errors.familyMemberId ? (
              <FieldError>{errors.familyMemberId}</FieldError>
            ) : (
              <FieldDescription>Người thân không tự khóa được; Quản lý viện duyệt.</FieldDescription>
            )}
          </Field>
        )}

        {needsReason && (
          <Field data-invalid={!!errors.reason || undefined}>
            <FieldLabel htmlFor="resident-command-reason">Lý do</FieldLabel>
            <Textarea
              id="resident-command-reason"
              rows={3}
              maxLength={REASON_MAX}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              aria-invalid={!!errors.reason || undefined}
            />
            {errors.reason ? <FieldError>{errors.reason}</FieldError> : <FieldDescription>Bắt buộc.</FieldDescription>}
          </Field>
        )}
      </FieldGroup>

      <ProblemAlert problem={failure}>
        {failure?.status === 412 && (
          <Button type="button" variant="outline" size="sm" className="mt-2" onClick={onStale}>
            Tải lại
          </Button>
        )}
      </ProblemAlert>

      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={saving}>
          {submitLabel}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}
