import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getResidentsByIdConsents, postConsentsByIdWithdraw, postResidentsByIdConsents } from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { openFile, uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { CONSENTER_LABELS, FINAL_STATUSES, STATUS_LABELS } from './consentLabels'

// Tab "Đồng ý" trong khung chi tiết hồ sơ (001 T059; US5; FR-010 → FR-016; bố cục duyệt 2026-10-23): thẻ bản đồng ý hiện
// hành kèm nút theo allowedCommands, form ghi nhận hoặc rút lại mở ngay dưới thẻ, lịch sử các bản đã rút lại hoặc được
// thay thế. Bản ký và bằng chứng tải lên bằng POST /files, tải xuống qua GET /files/{id} (backend kiểm quyền mỗi lần).
// Tạm thời: feature 012 chưa có danh sách người thân nên mã người thân nhập tay; backend kiểm quan hệ của người ký
// (409 INVALID_SIGNER), còn mã trong danh sách được xem sức khỏe thì không kiểm tồn tại (phạm vi theo quan hệ của 002 đã
// lọc khi xem).

const FILE_HINT = 'PDF, JPEG hoặc PNG, tối đa 10 MB.'
const POSITIVE_INTEGER = /^[1-9]\d{0,17}$/

function today() {
  return new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date())
}

function uploadProblem(problem) {
  if (problem.errorCode === 'FILE_TYPE_NOT_ALLOWED') return `Tệp phải là ${FILE_HINT.replace('.', '')}`
  if (problem.errorCode === 'FILE_TOO_LARGE') return 'Tệp quá 10 MB'
  return null
}

// multiple: chọn nhiều tệp một lần; onPick nhận mảng File và ô được xóa để chọn lại cùng tệp được (001 T082)
export function FileField({ id, label, error, onPick, hint = FILE_HINT, multiple = false }) {
  function picked(event) {
    if (!multiple) return onPick(event.target.files?.[0] ?? null)
    onPick(Array.from(event.target.files ?? []))
    event.target.value = ''
  }
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <input
        id={id}
        type="file"
        accept="application/pdf,image/jpeg,image/png"
        multiple={multiple}
        aria-invalid={!!error || undefined}
        className="type-body-sm h-11 rounded-lg border px-3 py-2"
        onChange={picked}
      />
      {error ? <FieldError>{error}</FieldError> : <FieldDescription>{hint}</FieldDescription>}
    </Field>
  )
}

function DownloadButton({ fileId, label, onFailure }) {
  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      className="h-10"
      onClick={async () => {
        try {
          await openFile(fileId)
          onFailure(null)
        } catch (error) {
          onFailure(problemOf(error))
        }
      }}
    >
      {label}
    </Button>
  )
}

export function Row({ label, children }) {
  return (
    <div className="flex flex-col">
      <dt className="type-caption text-muted-foreground">{label}</dt>
      <dd className="type-body-sm font-medium">{children}</dd>
    </div>
  )
}

function yesNo(value) {
  return value ? 'Có' : 'Không'
}

function ConsentDetails({ consent, onDownloadFailure }) {
  const court = consent.courtDecision
  return (
    <>
      <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
        <Row label="Người đồng ý">
          {CONSENTER_LABELS[consent.consenterType]}
          {consent.consenterFamilyMemberId != null && ` (mã người thân ${consent.consenterFamilyMemberId})`}
        </Row>
        <Row label="Ngày đồng ý">{formatDateTime(consent.consentedAt)}</Row>
        <Row label="Người thân được xem sức khỏe">
          {consent.familyMemberIdsAllowedHealthView.length > 0
            ? `Mã ${consent.familyMemberIdsAllowedHealthView.join(', ')}`
            : 'Không ai'}
        </Row>
        <Row label="Đồng ý dùng hình ảnh">{yesNo(consent.photoConsent)}</Row>
        <Row label="Nhận thông báo không khẩn về người cao tuổi">{yesNo(consent.notificationConsent)}</Row>
        {court && (
          <div className="col-span-2">
            <Row label="Quyết định của Tòa án">{`${court.number}, ngày ${court.date}, ${court.court}`}</Row>
          </div>
        )}
      </dl>
      <div className="flex flex-wrap gap-2">
        <DownloadButton fileId={consent.evidenceFileId} label="Tải bản ký" onFailure={onDownloadFailure} />
        {court && (
          <DownloadButton
            fileId={court.fileId}
            label="Tải quyết định của Tòa án"
            onFailure={onDownloadFailure}
          />
        )}
        {consent.withdrawal?.evidenceFileId != null && (
          <DownloadButton
            fileId={consent.withdrawal.evidenceFileId}
            label="Tải bản yêu cầu rút lại"
            onFailure={onDownloadFailure}
          />
        )}
      </div>
    </>
  )
}

function RecordForm({ residentId, onCancel, onDone }) {
  const [signer, setSigner] = useState('RESIDENT')
  const [signerId, setSignerId] = useState('')
  const [court, setCourt] = useState({ number: '', date: '', court: '' })
  const [courtFile, setCourtFile] = useState(null)
  const [evidence, setEvidence] = useState(null)
  const [idInput, setIdInput] = useState('')
  const [ids, setIds] = useState([])
  const [photo, setPhoto] = useState(false)
  const [notification, setNotification] = useState(false)
  const [date, setDate] = useState(today)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key] = useState(() => crypto.randomUUID())
  // tệp đã tải lên theo đối tượng File, để gửi lại sau lỗi không tải lên lần nữa
  const uploaded = useRef(new Map())

  function addId() {
    const text = idInput.trim()
    if (!POSITIVE_INTEGER.test(text)) {
      setErrors((current) => ({ ...current, ids: 'Mã người thân là số nguyên dương' }))
      return
    }
    const value = Number(text)
    setIds((current) => (current.includes(value) ? current : [...current, value]))
    setIdInput('')
    setErrors((current) => ({ ...current, ids: undefined }))
  }

  async function upload(file) {
    if (!uploaded.current.has(file)) uploaded.current.set(file, await uploadFile(file))
    return uploaded.current.get(file)
  }

  function validate() {
    const next = {}
    if (!evidence) next.evidence = 'Chọn tệp bản ký đã scan'
    if (!date) next.date = 'Chọn ngày đồng ý'
    if (signer === 'LEGAL_REPRESENTATIVE') {
      if (!POSITIVE_INTEGER.test(signerId.trim())) next.signerId = 'Nhập mã người thân là người ký'
      if (!court.number.trim()) next.number = 'Nhập số quyết định'
      if (!court.date) next.courtDate = 'Chọn ngày quyết định'
      if (!court.court.trim()) next.courtName = 'Nhập tên Tòa án'
      if (!courtFile) next.courtFile = 'Chọn tệp quyết định của Tòa án'
    }
    return next
  }

  async function submit(event) {
    event.preventDefault()
    const next = validate()
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setFailure(null)
    setBusy(true)
    let step = 'evidence'
    try {
      const evidenceFileId = await upload(evidence)
      let courtDecision
      if (signer === 'LEGAL_REPRESENTATIVE') {
        step = 'courtFile'
        courtDecision = {
          number: court.number.trim(),
          date: court.date,
          court: court.court.trim(),
          fileId: await upload(courtFile),
        }
      }
      step = 'save'
      const { data } = await postResidentsByIdConsents({
        path: { id: residentId },
        headers: { 'Idempotency-Key': key },
        body: {
          consenterType: signer,
          consenterFamilyMemberId: signer === 'LEGAL_REPRESENTATIVE' ? Number(signerId.trim()) : undefined,
          courtDecision,
          familyMemberIdsAllowedHealthView: ids,
          photoConsent: photo,
          notificationConsent: notification,
          consentedAt: new Date(`${date}T00:00:00+07:00`).toISOString(),
          evidenceFileId,
        },
      })
      onDone(data)
    } catch (error) {
      const problem = problemOf(error)
      const fileMessage = step === 'save' ? null : uploadProblem(problem)
      if (fileMessage) setErrors({ [step]: fileMessage })
      else if (error instanceof ApiError && problem.errorCode === 'INVALID_SIGNER') {
        setErrors({ signerId: 'Người ký thay phải là người thân có quan hệ Hiệu lực với người cao tuổi' })
      } else if (error instanceof ApiError && problem.errorCode === 'COURT_DECISION_REQUIRED') {
        setErrors({ number: 'Cần đủ số, ngày, Tòa án và tệp quyết định của Tòa án' })
      } else setFailure(problem)
    } finally {
      setBusy(false)
    }
  }

  const rep = signer === 'LEGAL_REPRESENTATIVE'
  return (
    <form
      onSubmit={submit}
      noValidate
      aria-label="Ghi nhận bản đồng ý"
      className="flex flex-col gap-4 rounded-lg border bg-muted/30 p-4"
    >
      <h3 className="type-card-title">Ghi nhận bản đồng ý</h3>
      <p className="type-body-sm text-muted-foreground">
        Bản mới thay bản Hiệu lực hiện hành. Người thân không có trong danh sách dưới đây sẽ không xem được thông tin sức
        khỏe.
      </p>

      <Field>
        <span className="type-label" id="consent-signer-label">
          Người đồng ý
        </span>
        <div role="radiogroup" aria-labelledby="consent-signer-label" className="flex flex-wrap gap-2">
          {Object.entries(CONSENTER_LABELS).map(([value, label]) => (
            <Button
              key={value}
              type="button"
              role="radio"
              aria-checked={signer === value}
              variant={signer === value ? 'default' : 'outline'}
              className="h-10"
              onClick={() => setSigner(value)}
            >
              {label}
            </Button>
          ))}
        </div>
        {rep && (
          <FieldDescription>
            Chỉ người đại diện theo pháp luật mới ký thay, kèm quyết định của Tòa án. Người chỉ có vai trò “người đại
            diện” ở hồ sơ người thân thì chưa được ký thay.
          </FieldDescription>
        )}
      </Field>

      {rep && (
        <>
          <Field data-invalid={!!errors.signerId || undefined}>
            <FieldLabel htmlFor="consent-signer-id">Mã người thân là người ký</FieldLabel>
            <Input
              id="consent-signer-id"
              inputMode="numeric"
              value={signerId}
              onChange={(e) => setSignerId(e.target.value)}
              aria-invalid={!!errors.signerId || undefined}
            />
            {errors.signerId ? (
              <FieldError>{errors.signerId}</FieldError>
            ) : (
              <FieldDescription>
                Tạm thời nhập mã tới khi hồ sơ người thân (feature 012) có danh sách. Hệ thống kiểm người này có quan hệ
                Hiệu lực với người cao tuổi.
              </FieldDescription>
            )}
          </Field>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field data-invalid={!!errors.number || undefined}>
              <FieldLabel htmlFor="consent-court-number">Số quyết định</FieldLabel>
              <Input
                id="consent-court-number"
                value={court.number}
                onChange={(e) => setCourt({ ...court, number: e.target.value })}
                aria-invalid={!!errors.number || undefined}
              />
              {errors.number && <FieldError>{errors.number}</FieldError>}
            </Field>
            <Field data-invalid={!!errors.courtDate || undefined}>
              <FieldLabel htmlFor="consent-court-date">Ngày quyết định</FieldLabel>
              <Input
                id="consent-court-date"
                type="date"
                max={today()}
                value={court.date}
                onChange={(e) => setCourt({ ...court, date: e.target.value })}
                aria-invalid={!!errors.courtDate || undefined}
              />
              {errors.courtDate && <FieldError>{errors.courtDate}</FieldError>}
            </Field>
            <Field data-invalid={!!errors.courtName || undefined}>
              <FieldLabel htmlFor="consent-court-name">Tòa án</FieldLabel>
              <Input
                id="consent-court-name"
                value={court.court}
                onChange={(e) => setCourt({ ...court, court: e.target.value })}
                aria-invalid={!!errors.courtName || undefined}
              />
              {errors.courtName && <FieldError>{errors.courtName}</FieldError>}
            </Field>
          </div>
          <FileField
            id="consent-court-file"
            label="Bản scan quyết định của Tòa án"
            error={errors.courtFile}
            onPick={setCourtFile}
          />
        </>
      )}

      <FileField id="consent-evidence" label="Bản ký đã scan" error={errors.evidence} onPick={setEvidence} />

      <Field data-invalid={!!errors.date || undefined}>
        <FieldLabel htmlFor="consent-date">Ngày đồng ý</FieldLabel>
        <Input
          id="consent-date"
          type="date"
          max={today()}
          value={date}
          onChange={(e) => setDate(e.target.value)}
          aria-invalid={!!errors.date || undefined}
        />
        {errors.date && <FieldError>{errors.date}</FieldError>}
      </Field>

      <Field data-invalid={!!errors.ids || undefined}>
        <FieldLabel htmlFor="consent-family-id">Mã người thân được xem sức khỏe</FieldLabel>
        <div className="flex gap-2">
          <Input
            id="consent-family-id"
            inputMode="numeric"
            value={idInput}
            onChange={(e) => setIdInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                addId()
              }
            }}
            aria-invalid={!!errors.ids || undefined}
          />
          <Button type="button" variant="outline" className="h-11" onClick={addId}>
            Thêm mã
          </Button>
        </div>
        {errors.ids ? (
          <FieldError>{errors.ids}</FieldError>
        ) : (
          <FieldDescription>
            Tạm thời nhập mã tới khi hồ sơ người thân (feature 012) có danh sách. Để trống nếu không ai được xem.
          </FieldDescription>
        )}
        {ids.length > 0 && (
          <ul aria-label="Người thân được xem sức khỏe" className="flex flex-wrap gap-2">
            {ids.map((id) => (
              <li key={id} className="flex items-center gap-1 rounded-full border px-3 py-1 type-body-sm">
                <span>Mã {id}</span>
                <button
                  type="button"
                  className="min-h-8 min-w-8 rounded-full text-muted-foreground"
                  aria-label={`Bỏ mã ${id}`}
                  onClick={() => setIds((current) => current.filter((x) => x !== id))}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        )}
      </Field>

      <div className="flex flex-col gap-2">
        <label className="flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border px-3">
          <Checkbox checked={photo} onCheckedChange={setPhoto} aria-label="Đồng ý dùng hình ảnh" />
          <span className="type-body-sm">Đồng ý dùng hình ảnh</span>
        </label>
        <label className="flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border px-3">
          <Checkbox
            checked={notification}
            onCheckedChange={setNotification}
            aria-label="Đồng ý nhận thông báo không khẩn về người cao tuổi"
          />
          <span className="type-body-sm">Đồng ý nhận thông báo không khẩn về người cao tuổi</span>
        </label>
      </div>

      <ProblemAlert problem={failure} />

      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={busy}>
          Lưu bản đồng ý
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Thôi
        </Button>
      </div>
    </form>
  )
}

function WithdrawForm({ consent, onCancel, onDone }) {
  const [reason, setReason] = useState('')
  const [file, setFile] = useState(null)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key] = useState(() => crypto.randomUUID())

  async function submit(event) {
    event.preventDefault()
    const next = {}
    if (!reason.trim()) next.reason = 'Nhập lý do rút lại'
    if (!file) next.file = 'Chọn tệp bản yêu cầu rút lại'
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setFailure(null)
    setBusy(true)
    let uploading = true
    try {
      const evidenceFileId = await uploadFile(file)
      uploading = false
      const { data } = await postConsentsByIdWithdraw({
        path: { id: consent.id },
        headers: { 'Idempotency-Key': key },
        body: { reason: reason.trim(), evidenceFileId },
      })
      onDone(data)
    } catch (error) {
      const problem = problemOf(error)
      const fileMessage = uploading ? uploadProblem(problem) : null
      if (fileMessage) setErrors({ file: fileMessage })
      else if (problem.errorCode === 'REASON_REQUIRED') setErrors({ reason: problem.detail || 'Nhập lý do rút lại' })
      else setFailure(problem)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form
      onSubmit={submit}
      noValidate
      aria-label="Rút lại đồng ý"
      className="flex flex-col gap-4 rounded-lg border bg-muted/30 p-4"
    >
      <h3 className="type-card-title">Rút lại đồng ý</h3>
      <p className="type-body-sm text-muted-foreground">
        Người thân trong phạm vi mất quyền xem thông tin sức khỏe từ lần truy cập kế tiếp. Bản đã rút lại không kích hoạt
        lại được; muốn đồng ý lại thì ghi nhận bản mới.
      </p>
      <Field data-invalid={!!errors.reason || undefined}>
        <FieldLabel htmlFor="consent-withdraw-reason">Lý do rút lại (bắt buộc)</FieldLabel>
        <Textarea
          id="consent-withdraw-reason"
          rows={3}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={!!errors.reason || undefined}
        />
        {errors.reason && <FieldError>{errors.reason}</FieldError>}
      </Field>
      <FileField id="consent-withdraw-file" label="Bản yêu cầu rút lại đã scan" error={errors.file} onPick={setFile} />
      <ProblemAlert problem={failure} />
      <div className="flex flex-wrap gap-3">
        <Button type="submit" variant="destructive" className="h-11" disabled={busy}>
          Xác nhận rút lại
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Thôi
        </Button>
      </div>
    </form>
  )
}

export function ConsentTab({ residentId, status, allowedCommands = [], onChanged }) {
  const [state, setState] = useState('loading')
  const [consents, setConsents] = useState([])
  const [failure, setFailure] = useState(null)
  const [downloadFailure, setDownloadFailure] = useState(null)
  const [form, setForm] = useState(null)
  const [done, setDone] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByIdConsents({ path: { id: residentId } })
      setConsents(data)
      setFailure(null)
      setState('ready')
    } catch (error) {
      if (error instanceof ApiError && error.status === 403) {
        setState('forbidden')
      } else {
        setFailure(problemOf(error))
        setState('error')
      }
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp bản đồng ý khi mở tab
    load()
  }, [load])

  async function finished(message) {
    setForm(null)
    setDone(message)
    await load()
    onChanged?.()
  }

  if (state === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2">
        <span className="sr-only">Đang tải bản đồng ý</span>
        <Skeleton className="h-16" />
        <Skeleton className="h-24" />
      </div>
    )
  }
  if (state === 'forbidden') {
    return (
      <Empty>
        <EmptyHeader>
          <EmptyTitle>Không có quyền xem bản đồng ý</EmptyTitle>
          <EmptyDescription>Vai trò của bạn không được xem bản đồng ý chia sẻ dữ liệu của người này.</EmptyDescription>
        </EmptyHeader>
      </Empty>
    )
  }
  if (state === 'error') {
    return (
      <div className="flex flex-col gap-3">
        <ProblemAlert problem={failure} />
        <Button type="button" variant="outline" className="h-11 self-start" onClick={load}>
          Thử lại
        </Button>
      </div>
    )
  }

  const current = consents.find((c) => c.status === 'ACTIVE')
  const past = consents.filter((c) => c.status !== 'ACTIVE')
  const isFinal = FINAL_STATUSES.includes(status)
  const canRecord = allowedCommands.includes('RECORD_CONSENT') && !isFinal
  const canWithdraw = allowedCommands.includes('WITHDRAW_CONSENT') && !!current

  return (
    <div className="flex flex-col gap-5">
      <section aria-labelledby="consent-now-title" className="flex flex-col gap-3 rounded-lg border p-4">
        <div className="flex flex-wrap items-center gap-2">
          <h3 id="consent-now-title" className="type-card-title">
            Bản đồng ý hiện hành
          </h3>
          {current && <StatusBadge status={STATUS_LABELS.ACTIVE} />}
        </div>

        {current ? (
          <ConsentDetails consent={current} onDownloadFailure={setDownloadFailure} />
        ) : (
          <Alert variant="attention">
            <AlertTitle>Chưa có bản đồng ý Hiệu lực</AlertTitle>
            <AlertDescription>
              Người thân chưa xem được thông tin sức khỏe của người này cho tới khi có bản đồng ý.
              {isFinal && ' Hồ sơ ở trạng thái cuối nên không ghi nhận bản mới được.'}
            </AlertDescription>
          </Alert>
        )}

        <ProblemAlert problem={downloadFailure} />

        {(canRecord || canWithdraw) && (
          <div className="flex flex-wrap gap-2">
            {canRecord && (
              <Button
                type="button"
                variant={current ? 'outline' : 'default'}
                className="h-11"
                aria-pressed={form === 'record'}
                onClick={() => {
                  setDone(null)
                  setForm('record')
                }}
              >
                {current ? 'Ghi nhận bản mới' : 'Ghi nhận bản đồng ý'}
              </Button>
            )}
            {canWithdraw && (
              <Button
                type="button"
                variant="outline"
                className="h-11"
                aria-pressed={form === 'withdraw'}
                onClick={() => {
                  setDone(null)
                  setForm('withdraw')
                }}
              >
                Rút lại đồng ý
              </Button>
            )}
          </div>
        )}
      </section>

      {done && (
        <p role="status" className="type-body-sm rounded-lg border bg-muted/30 p-3">
          {done}
        </p>
      )}

      {form === 'record' && canRecord && (
        <RecordForm
          residentId={residentId}
          onCancel={() => setForm(null)}
          onDone={() => finished('Đã ghi nhận bản đồng ý.')}
        />
      )}
      {form === 'withdraw' && canWithdraw && (
        <WithdrawForm
          key={current.id}
          consent={current}
          onCancel={() => setForm(null)}
          onDone={() => finished('Đã rút lại đồng ý. Người thân mất quyền xem sức khỏe từ lần truy cập kế tiếp.')}
        />
      )}

      <section aria-labelledby="consent-history-title" className="flex flex-col gap-2">
        <h3 id="consent-history-title" className="type-card-title">
          Lịch sử bản đồng ý
        </h3>
        {past.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa có bản nào đã rút lại hoặc được thay thế.</p>
        ) : (
          <ul aria-label="Lịch sử bản đồng ý" className="flex flex-col gap-2">
            {past.map((consent) => (
              <li key={consent.id} className="flex flex-col gap-3 rounded-lg border p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={STATUS_LABELS[consent.status]} />
                  {consent.withdrawal && (
                    <span className="type-caption text-muted-foreground tabular-nums">
                      {formatDateTime(consent.withdrawal.at)}
                    </span>
                  )}
                </div>
                {consent.withdrawal?.reason && (
                  <p className="type-body-sm whitespace-pre-wrap">{consent.withdrawal.reason}</p>
                )}
                <ConsentDetails consent={consent} onDownloadFailure={setDownloadFailure} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
