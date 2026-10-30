import { useState } from 'react'
import {
  postStaffByIdCredentials,
  postCredentialsByIdRenew,
  postCredentialsByIdRevoke,
  postCredentialsByIdCorrect,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDate } from '@/common/dateTime'
import { openFile, uploadFile } from '@/common/files'
import { Choice } from './ShiftForm'
const VALIDITY = {
  VALID: 'Còn hiệu lực',
  EXPIRED: 'Hết hạn',
  REVOKED: 'Đã thu hồi',
  REPLACED: 'Đã thay thế',
  NOT_YET_ISSUED: 'Chưa tới ngày cấp',
}
export function CredentialPanel({ person, rows, types, scopes, create, update, onDone }) {
  const [form, setForm] = useState(null),
    [failure, setFailure] = useState(null)
  function choose(kind, credential) {
    setForm({ kind, credential })
    setFailure(null)
  }
  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="type-section">Giấy phép và đào tạo</h3>
        {create && (
          <Button variant="outline" onClick={() => choose('add')}>
            Thêm chứng chỉ
          </Button>
        )}
      </div>
      <ProblemAlert problem={failure} />
      {!rows.length && <p>Chưa có giấy phép hoặc đào tạo.</p>}
      {rows.map((c) => (
        <article key={c.id} className="flex flex-col gap-2 rounded-xl border p-3">
          <p className="font-medium">
            {types.find((t) => t.id === c.credentialTypeId)?.name ?? 'Chứng chỉ'} · {VALIDITY[c.validityStatus]}
          </p>
          <p className="text-muted-foreground">
            Cấp {formatDate(c.issueDate)} · {c.expiryDate ? `Hết hạn ${formatDate(c.expiryDate)}` : 'Không thời hạn'}
          </p>
          {c.credentialNumber && <p>Số: {c.credentialNumber}</p>}
          {c.issuingAuthority && <p>Đơn vị cấp: {c.issuingAuthority}</p>}
          {c.practiceScopes?.length > 0 && (
            <p>
              Phạm vi:{' '}
              {c.practiceScopes
                .map((code) => scopes.find((s) => String(s.id) === code || s.code === code)?.name ?? code)
                .join(', ')}
            </p>
          )}
          {c.scanFileId && (
            <Button
              variant="outline"
              onClick={async () => {
                try {
                  await openFile(c.scanFileId)
                } catch (e) {
                  setFailure(problemOf(e))
                }
              }}
            >
              Xem bản scan
            </Button>
          )}
          {update && (
            <div className="flex flex-wrap gap-2">
              {c.validityStatus !== 'REPLACED' && (
                <>
                  <Button variant="outline" onClick={() => choose('renew', c)}>
                    Gia hạn
                  </Button>
                  <Button variant="outline" onClick={() => choose('revoke', c)}>
                    Thu hồi
                  </Button>
                </>
              )}
              <Button variant="outline" onClick={() => choose('correct', c)}>
                Sửa sai
              </Button>
            </div>
          )}
        </article>
      ))}
      {form && (
        <CredentialForm
          key={`${form.kind}-${form.credential?.id ?? 'new'}`}
          {...form}
          person={person}
          types={types}
          scopes={scopes}
          onCancel={() => setForm(null)}
          onDone={() => {
            setForm(null)
            onDone()
          }}
        />
      )}
    </section>
  )
}
function CredentialForm({ kind, credential, person, types, scopes, onDone, onCancel }) {
  const [value, setValue] = useState(credential ?? {}),
    [reason, setReason] = useState(''),
    [date, setDate] = useState(''),
    [file, setFile] = useState(null),
    [scan, setScan] = useState(credential?.scanFileId),
    [failure, setFailure] = useState(null),
    [busy, setBusy] = useState(false)
  const set = (key, v) => setValue((old) => ({ ...old, [key]: v })),
    type = types.find((t) => t.id === Number(value.credentialTypeId)),
    needReason = kind === 'revoke' || kind === 'correct'
  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    try {
      let scanId = scan
      if (file && !scan) {
        scanId = await uploadFile(file)
        setScan(scanId)
      }
      const body =
        kind === 'revoke'
          ? { effectiveFrom: date, reason }
          : {
              credentialTypeId: Number(value.credentialTypeId),
              credentialNumber: value.credentialNumber || undefined,
              issuingAuthority: value.issuingAuthority || undefined,
              issueDate: value.issueDate,
              expiryDate: value.expiryDate || undefined,
              practiceScopes: value.practiceScopes ?? [],
              scanFileId: scanId || undefined,
              ...(needReason && { reason }),
            }
      const action = {
        add: postStaffByIdCredentials,
        renew: postCredentialsByIdRenew,
        revoke: postCredentialsByIdRevoke,
        correct: postCredentialsByIdCorrect,
      }[kind]
      await action({
        path: { id: kind === 'add' ? person.id : credential.id },
        body,
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form onSubmit={submit} className="flex flex-col gap-4 rounded-xl border p-4">
      <h4 className="font-semibold">
        {
          {
            add: 'Thêm chứng chỉ',
            renew: 'Gia hạn chứng chỉ',
            revoke: 'Ghi nhận thu hồi',
            correct: 'Sửa sai chứng chỉ',
          }[kind]
        }
      </h4>
      <ProblemAlert problem={failure} />
      {kind === 'revoke' ? (
        <Field>
          <FieldLabel htmlFor="credential-revoked">Ngày hiệu lực thu hồi</FieldLabel>
          <Input id="credential-revoked" type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
        </Field>
      ) : (
        <>
          <Choice
            id="credential-type"
            label="Loại chứng chỉ"
            required
            disabled={kind === 'renew'}
            value={value.credentialTypeId ?? ''}
            onChange={(e) => set('credentialTypeId', Number(e.target.value))}
          >
            <option value="">Chọn loại chứng chỉ</option>
            {types
              .filter((t) => t.active !== false || t.id === value.credentialTypeId)
              .map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
          </Choice>
          {[
            ['credentialNumber', 'Số chứng chỉ', 'text'],
            ['issuingAuthority', 'Đơn vị cấp', 'text'],
            ['issueDate', 'Ngày cấp', 'date'],
            ['expiryDate', 'Ngày hết hạn', 'date'],
          ].map(([key, label, typeInput]) => (
            <Field key={key}>
              <FieldLabel htmlFor={`credential-${key}`}>{label}</FieldLabel>
              <Input
                id={`credential-${key}`}
                type={typeInput}
                required={key === 'issueDate' || (key === 'expiryDate' && type?.hasExpiry)}
                value={value[key] ?? ''}
                onChange={(e) => set(key, e.target.value)}
              />
            </Field>
          ))}
          {type?.hasPracticeScopes && (
            <fieldset className="flex flex-col gap-2">
              <legend>Phạm vi hành nghề</legend>
              {scopes
                .filter((s) => s.active !== false)
                .map((s) => (
                  <label key={s.id} className="flex min-h-11 items-center gap-2">
                    <input
                      type="checkbox"
                      checked={(value.practiceScopes ?? []).includes(s.code)}
                      onChange={(e) => {
                        const code = s.code
                        set(
                          'practiceScopes',
                          e.target.checked
                            ? [...(value.practiceScopes ?? []), code]
                            : (value.practiceScopes ?? []).filter((x) => x !== code),
                        )
                      }}
                    />
                    {s.name}
                  </label>
                ))}
            </fieldset>
          )}
          <Field>
            <FieldLabel htmlFor="credential-scan">Bản scan (PDF, JPEG, PNG)</FieldLabel>
            <Input
              id="credential-scan"
              type="file"
              accept="application/pdf,image/jpeg,image/png"
              onChange={(e) => {
                setFile(e.target.files?.[0] ?? null)
                setScan(null)
              }}
            />
          </Field>
        </>
      )}
      {needReason && (
        <Field>
          <FieldLabel htmlFor="credential-reason">Lý do</FieldLabel>
          <Textarea
            id="credential-reason"
            required
            maxLength={2000}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
        </Field>
      )}
      <div className="flex gap-2">
        <Button type="submit" disabled={busy}>
          {busy ? 'Đang lưu…' : 'Lưu chứng chỉ'}
        </Button>
        <Button variant="outline" type="button" disabled={busy} onClick={onCancel}>
          Hủy
        </Button>
      </div>
    </form>
  )
}
