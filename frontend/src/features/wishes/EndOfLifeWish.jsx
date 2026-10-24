import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getResidentsByIdEndOfLifeWishes, postResidentsByIdEndOfLifeWishes } from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { FileField, Row } from '../consents/ConsentTab'
import { POSITIVE_INTEGER, today, uploadProblem, toRequestOpinion } from './wishUtils'
import { OpinionEditor, OpinionList } from './Opinions'
import {
  CHOICE_LABELS,
  END_OF_LIFE_STATUS_LABELS,
  FINAL_STATUSES,
  SELECTION_LABELS,
  SIGNER_LABELS,
  SOURCE_LABELS,
  WISH_ERRORS,
} from './wishLabels'

// Khối "Nguyện vọng cuối đời" của tab Nguyện vọng (001 T070; US7; FR-023a → FR-023i; bố cục đề xuất 2026-10-23). Thẻ phiếu
// hiện hành kèm nút theo allowedCommands, form ghi phiên bản mới mở ngay dưới thẻ, lịch sử các phiên bản. Nhận định "tự
// quyết được" chỉ hiện bên trong chính phiếu (FR-023h), kèm dòng "Người quyết định". Hành chính, Kế toán không xem được
// nội dung: chỉ thấy "Đã có phiếu" hoặc "Chưa có phiếu" (FR-023e). Tạm thời: mã Bác sĩ nhận định và mã người thân nhập
// tay vì chưa có danh sách (feature 008, 012); backend kiểm vai trò Bác sĩ và quan hệ Hiệu lực.

function yesNo(value) {
  return value ? 'Có' : 'Không'
}

function WishDetails({ wish }) {
  const assessment = wish.selfDeterminationAssessment
  return (
    <>
      <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
        <div className="col-span-2">
          <Row label="Lựa chọn khi nguy kịch">{CHOICE_LABELS[wish.choice]}</Row>
        </div>
        <Row label="Người cần liên hệ trước khi thực hiện">{wish.firstContact}</Row>
        <Row label="Nguồn phiếu">{SOURCE_LABELS[wish.source]}</Row>
        <div className="col-span-2">
          <Row label="Người quyết định">
            {SIGNER_LABELS[wish.signerType]}
            {wish.signer && ` · ${wish.signer}`}
            {wish.representativeSelection && ` · ${SELECTION_LABELS[wish.representativeSelection]}`}
          </Row>
        </div>
        {wish.note && (
          <div className="col-span-2">
            <Row label="Ghi chú">{wish.note}</Row>
          </div>
        )}
        <Row label="Ký lúc">{formatDateTime(wish.signedAt)}</Row>
      </dl>
      <section aria-label="Nhận định của Bác sĩ cho phiếu này" className="flex flex-col gap-2 rounded-lg border p-3">
        <h4 className="type-label">Nhận định của Bác sĩ cho phiếu này</h4>
        <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
          <Row label="Tự quyết được việc chọn này">{yesNo(assessment.canSelfDetermine)}</Row>
          <Row label="Ghi lúc">{formatDateTime(assessment.at)}</Row>
          <div className="col-span-2">
            <Row label="Cách đã hỗ trợ hiểu thông tin">{assessment.supportMethod}</Row>
          </div>
          <Row label="Ngày xem lại nhận định">{assessment.reviewDate}</Row>
        </dl>
      </section>
      <OpinionList opinions={wish.otherOpinions} />
    </>
  )
}

function RadioRow({ label, labelId, options, value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <span className="type-label" id={labelId}>
        {label}
      </span>
      <div role="radiogroup" aria-labelledby={labelId} className="flex flex-wrap gap-2">
        {options.map(([optionValue, optionLabel]) => (
          <Button
            key={String(optionValue)}
            type="button"
            role="radio"
            aria-checked={value === optionValue}
            variant={value === optionValue ? 'default' : 'outline'}
            className="h-10"
            onClick={() => onChange(optionValue)}
          >
            {optionLabel}
          </Button>
        ))}
      </div>
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

function TextField({ id, label, value, onChange, error, hint, inputMode, type }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel htmlFor={id}>{label}</FieldLabel>
      <Input
        id={id}
        type={type}
        inputMode={inputMode}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={!!error || undefined}
      />
      {error ? <FieldError>{error}</FieldError> : hint && <FieldDescription>{hint}</FieldDescription>}
    </Field>
  )
}

function WishForm({ residentId, onCancel, onDone }) {
  const [v, setV] = useState({
    choice: null,
    firstContact: '',
    note: '',
    doctorId: '',
    canSelf: null,
    supportMethod: '',
    reviewDate: '',
    signerType: null,
    signerId: '',
    relationship: '',
    selection: null,
    courtNumber: '',
    courtDate: '',
    courtName: '',
    signedAt: today(),
    source: 'ADMISSION',
  })
  const [opinions, setOpinions] = useState([])
  const [signedCopy, setSignedCopy] = useState(null)
  const [courtFile, setCourtFile] = useState(null)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key] = useState(() => crypto.randomUUID())
  // tệp đã tải lên theo đối tượng File, để gửi lại sau lỗi không tải lên lần nữa
  const uploaded = useRef(new Map())
  const set = (name) => (value) => setV((current) => ({ ...current, [name]: value }))

  async function upload(file) {
    if (!uploaded.current.has(file)) uploaded.current.set(file, await uploadFile(file))
    return uploaded.current.get(file)
  }

  const canSelf = v.canSelf === true
  const legal = v.signerType === 'LEGAL_REPRESENTATIVE'
  const patientRep = v.signerType === 'PATIENT_REPRESENTATIVE'

  function validate() {
    const next = {}
    if (!v.choice) next.choice = 'Chọn một lựa chọn'
    if (!v.firstContact.trim()) next.firstContact = WISH_ERRORS.FIRST_CONTACT_REQUIRED[1]
    if (!POSITIVE_INTEGER.test(v.doctorId.trim())) next.doctorId = 'Nhập mã tài khoản Bác sĩ nhận định'
    if (v.canSelf === null) next.canSelf = 'Chọn nhận định của Bác sĩ'
    if (!v.supportMethod.trim()) next.supportMethod = 'Nhập cách đã hỗ trợ người cao tuổi hiểu thông tin'
    if (!v.reviewDate) next.reviewDate = 'Chọn ngày xem lại nhận định'
    else if (v.reviewDate < today()) next.reviewDate = WISH_ERRORS.INVALID_REVIEW_DATE[1]
    if (v.canSelf === false) {
      if (!v.signerType) next.signerType = 'Chọn người ký thay'
      if (!POSITIVE_INTEGER.test(v.signerId.trim())) next.signerId = 'Nhập mã người thân là người ký'
      if (!v.relationship.trim()) next.relationship = 'Nhập quan hệ với người cao tuổi'
      if (patientRep && !v.selection) next.selection = WISH_ERRORS.REPRESENTATIVE_SELECTION_REQUIRED[1]
      if (legal) {
        if (!v.courtNumber.trim()) next.courtNumber = 'Nhập số quyết định'
        if (!v.courtDate) next.courtDate = 'Chọn ngày quyết định'
        if (!v.courtName.trim()) next.courtName = 'Nhập tên Tòa án'
        if (!courtFile) next.courtFile = 'Chọn tệp quyết định của Tòa án'
      }
    }
    if (!signedCopy) next.signedCopy = WISH_ERRORS.EVIDENCE_REQUIRED[1]
    if (!v.signedAt) next.signedAt = 'Chọn ngày ký'
    return next
  }

  async function submit(event) {
    event.preventDefault()
    const next = validate()
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setFailure(null)
    setBusy(true)
    let step = 'signedCopy'
    try {
      const signedCopyFileId = await upload(signedCopy)
      let courtDecision
      if (canSelf === false && legal) {
        step = 'courtFile'
        courtDecision = {
          number: v.courtNumber.trim(),
          date: v.courtDate,
          court: v.courtName.trim(),
          fileId: await upload(courtFile),
        }
      }
      step = 'save'
      const signer = canSelf ? 'RESIDENT' : v.signerType
      const { data } = await postResidentsByIdEndOfLifeWishes({
        path: { id: residentId },
        headers: { 'Idempotency-Key': key },
        body: {
          choice: v.choice,
          firstContact: v.firstContact.trim(),
          note: v.note.trim() || undefined,
          respondentType: signer,
          respondentFamilyMemberId: canSelf ? undefined : Number(v.signerId.trim()),
          relationship: canSelf ? undefined : v.relationship.trim(),
          assessingDoctorId: Number(v.doctorId.trim()),
          selfDeterminationAssessment: {
            canSelfDetermine: canSelf,
            supportMethod: v.supportMethod.trim(),
            reviewDate: v.reviewDate,
          },
          signerType: signer,
          signerFamilyMemberId: canSelf ? undefined : Number(v.signerId.trim()),
          representativeSelection: patientRep && !canSelf ? v.selection : undefined,
          courtDecision,
          otherOpinions: opinions.map(toRequestOpinion),
          signedAt: new Date(`${v.signedAt}T00:00:00+07:00`).toISOString(),
          signedCopyFileId,
          source: v.source,
        },
      })
      onDone(data)
    } catch (error) {
      const problem = problemOf(error)
      const fileMessage = step === 'save' ? null : uploadProblem(problem)
      const mapped = error instanceof ApiError ? WISH_ERRORS[problem.errorCode] : null
      if (fileMessage) setErrors({ [step]: fileMessage })
      else if (mapped) setErrors({ [mapped[0]]: mapped[1] })
      else setFailure(problem)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form
      onSubmit={submit}
      noValidate
      aria-label="Ghi phiếu nguyện vọng cuối đời"
      className="flex flex-col gap-4 rounded-lg border bg-muted/30 p-4"
    >
      <h3 className="type-card-title">Ghi phiếu nguyện vọng cuối đời</h3>
      <p className="type-body-sm text-muted-foreground">
        Phiếu mới thay phiếu Hiệu lực hiện hành; phiếu cũ vẫn xem được ở lịch sử. Phiếu mang nhận định của Bác sĩ chỉ cho
        quyết định này, không phải dấu của người cao tuổi.
      </p>

      <RadioRow
        label="Lựa chọn khi nguy kịch"
        labelId="eol-choice"
        options={Object.entries(CHOICE_LABELS)}
        value={v.choice}
        onChange={set('choice')}
        error={errors.choice}
      />
      <TextField
        id="eol-first-contact"
        label="Người cần liên hệ trước khi thực hiện"
        value={v.firstContact}
        onChange={set('firstContact')}
        error={errors.firstContact}
      />
      <Field>
        <FieldLabel htmlFor="eol-note">Ghi chú</FieldLabel>
        <Textarea id="eol-note" rows={2} value={v.note} onChange={(e) => set('note')(e.target.value)} />
      </Field>

      <fieldset className="flex flex-col gap-3 rounded-lg border p-3">
        <legend className="type-label px-1">Nhận định của Bác sĩ cho phiếu này</legend>
        <TextField
          id="eol-doctor"
          label="Mã tài khoản Bác sĩ nhận định"
          inputMode="numeric"
          value={v.doctorId}
          onChange={set('doctorId')}
          error={errors.doctorId}
          hint="Tạm thời nhập mã tới khi hồ sơ nhân viên (feature 008) có danh sách. Hệ thống kiểm người này là Bác sĩ."
        />
        <RadioRow
          label="Người cao tuổi tự quyết được việc chọn này"
          labelId="eol-can-self"
          options={[
            [true, 'Có'],
            [false, 'Không'],
          ]}
          value={v.canSelf}
          onChange={set('canSelf')}
          error={errors.canSelf}
        />
        <Field data-invalid={!!errors.supportMethod || undefined}>
          <FieldLabel htmlFor="eol-support">Cách đã hỗ trợ người cao tuổi hiểu thông tin</FieldLabel>
          <Textarea
            id="eol-support"
            rows={2}
            value={v.supportMethod}
            onChange={(e) => set('supportMethod')(e.target.value)}
            aria-invalid={!!errors.supportMethod || undefined}
          />
          {errors.supportMethod && <FieldError>{errors.supportMethod}</FieldError>}
        </Field>
        <TextField
          id="eol-review-date"
          type="date"
          label="Ngày xem lại nhận định"
          value={v.reviewDate}
          onChange={set('reviewDate')}
          error={errors.reviewDate}
          hint="Tới ngày này hệ thống nhắc Bác sĩ và Điều dưỡng phụ trách một lần."
        />
      </fieldset>

      {v.canSelf === true && (
        <p className="type-body-sm rounded-lg border p-3">
          Người quyết định: <strong>Người cao tuổi</strong> (chính người cao tuổi trả lời và ký).
        </p>
      )}
      {v.canSelf === false && (
        <fieldset className="flex flex-col gap-3 rounded-lg border p-3">
          <legend className="type-label px-1">Người quyết định thay</legend>
          <RadioRow
            label="Người ký thay"
            labelId="eol-signer-type"
            options={[
              ['PATIENT_REPRESENTATIVE', SIGNER_LABELS.PATIENT_REPRESENTATIVE],
              ['LEGAL_REPRESENTATIVE', SIGNER_LABELS.LEGAL_REPRESENTATIVE],
            ]}
            value={v.signerType}
            onChange={set('signerType')}
            error={errors.signerType}
          />
          <TextField
            id="eol-signer-id"
            label="Mã người thân là người ký"
            inputMode="numeric"
            value={v.signerId}
            onChange={set('signerId')}
            error={errors.signerId}
            hint="Tạm thời nhập mã tới khi hồ sơ người thân (feature 012) có danh sách. Người này cũng là người trả lời phiếu."
          />
          <TextField
            id="eol-relationship"
            label="Quan hệ với người cao tuổi"
            value={v.relationship}
            onChange={set('relationship')}
            error={errors.relationship}
          />
          {patientRep && (
            <RadioRow
              label="Người đại diện được chọn theo cách nào"
              labelId="eol-selection"
              options={Object.entries(SELECTION_LABELS)}
              value={v.selection}
              onChange={set('selection')}
              error={errors.selection}
            />
          )}
          {legal && (
            <>
              <div className="grid gap-4 sm:grid-cols-3">
                <TextField
                  id="eol-court-number"
                  label="Số quyết định"
                  value={v.courtNumber}
                  onChange={set('courtNumber')}
                  error={errors.courtNumber}
                />
                <TextField
                  id="eol-court-date"
                  type="date"
                  label="Ngày quyết định"
                  value={v.courtDate}
                  onChange={set('courtDate')}
                  error={errors.courtDate}
                />
                <TextField
                  id="eol-court-name"
                  label="Tòa án"
                  value={v.courtName}
                  onChange={set('courtName')}
                  error={errors.courtName}
                />
              </div>
              <FileField
                id="eol-court-file"
                label="Bản scan quyết định của Tòa án"
                error={errors.courtFile}
                onPick={setCourtFile}
              />
            </>
          )}
        </fieldset>
      )}

      <FileField id="eol-signed-copy" label="Bản ký đã scan" error={errors.signedCopy} onPick={setSignedCopy} />
      <TextField
        id="eol-signed-at"
        type="date"
        label="Ngày ký"
        value={v.signedAt}
        onChange={set('signedAt')}
        error={errors.signedAt}
      />
      <RadioRow
        label="Nguồn phiếu"
        labelId="eol-source"
        options={[
          ['ADMISSION', SOURCE_LABELS.ADMISSION],
          ['FAMILY_REQUEST', SOURCE_LABELS.FAMILY_REQUEST],
        ]}
        value={v.source}
        onChange={set('source')}
      />

      <OpinionEditor idPrefix="eol" items={opinions} onChange={setOpinions} error={errors.opinions} />

      <ProblemAlert problem={failure} />

      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={busy}>
          Lưu phiếu
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Thôi
        </Button>
      </div>
    </form>
  )
}

export function EndOfLifeWish({ residentId, status, allowedCommands = [], hasWish, onChanged }) {
  const [state, setState] = useState('loading')
  const [summary, setSummary] = useState(null)
  const [failure, setFailure] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [done, setDone] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByIdEndOfLifeWishes({ path: { id: residentId } })
      setSummary(data)
      setFailure(null)
      setState('ready')
    } catch (error) {
      if (error instanceof ApiError && error.status === 403) setState('forbidden')
      else {
        setFailure(problemOf(error))
        setState('error')
      }
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp phiếu khi mở tab
    load()
  }, [load])

  async function finished() {
    setFormOpen(false)
    setDone('Đã ghi phiếu nguyện vọng cuối đời.')
    await load()
    onChanged?.()
  }

  const heading = (
    <h3 id="eol-title" className="type-card-title">
      Nguyện vọng cuối đời
    </h3>
  )

  if (state === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2">
        <span className="sr-only">Đang tải phiếu nguyện vọng cuối đời</span>
        <Skeleton className="h-16" />
        <Skeleton className="h-24" />
      </div>
    )
  }
  if (state === 'forbidden') {
    return (
      <section aria-labelledby="eol-title" className="flex flex-col gap-2 rounded-lg border p-4">
        {heading}
        <p className="type-body-sm">{hasWish ? 'Đã có phiếu nguyện vọng cuối đời.' : 'Chưa có phiếu nguyện vọng cuối đời.'}</p>
        <p className="type-body-sm text-muted-foreground">
          Vai trò của bạn chỉ thấy phiếu đã có hay chưa, không xem được nội dung.
        </p>
      </section>
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

  const current = summary.current
  const past = summary.history.filter((w) => w.status !== 'ACTIVE')
  const canRecord = allowedCommands.includes('RECORD_END_OF_LIFE_WISH') && !FINAL_STATUSES.includes(status)

  return (
    <div className="flex flex-col gap-4">
      <section aria-labelledby="eol-title" className="flex flex-col gap-3 rounded-lg border p-4">
        <div className="flex flex-wrap items-center gap-2">
          {heading}
          {current && <StatusBadge status={END_OF_LIFE_STATUS_LABELS.ACTIVE} />}
        </div>
        {current ? (
          <WishDetails wish={current} />
        ) : (
          <Alert variant="attention">
            <AlertTitle>Chưa có phiếu nguyện vọng cuối đời Hiệu lực</AlertTitle>
            <AlertDescription>
              Bác sĩ và Điều dưỡng phụ trách được nhắc ghi phiếu tới khi có.
              {FINAL_STATUSES.includes(status) && ' Hồ sơ ở trạng thái cuối nên không ghi phiếu mới được.'}
            </AlertDescription>
          </Alert>
        )}
        {canRecord && (
          <Button
            type="button"
            variant={current ? 'outline' : 'default'}
            className="h-11 self-start"
            aria-pressed={formOpen}
            onClick={() => {
              setDone(null)
              setFormOpen(true)
            }}
          >
            {current ? 'Ghi phiếu mới' : 'Ghi phiếu nguyện vọng'}
          </Button>
        )}
      </section>

      {done && (
        <p role="status" className="type-body-sm rounded-lg border bg-muted/30 p-3">
          {done}
        </p>
      )}
      {formOpen && canRecord && <WishForm residentId={residentId} onCancel={() => setFormOpen(false)} onDone={finished} />}

      <section aria-labelledby="eol-history-title" className="flex flex-col gap-2">
        <h4 id="eol-history-title" className="type-label">
          Lịch sử phiếu nguyện vọng cuối đời
        </h4>
        {past.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa có phiếu nào được thay thế.</p>
        ) : (
          <ul aria-label="Lịch sử phiếu nguyện vọng cuối đời" className="flex flex-col gap-2">
            {past.map((wish) => (
              <li key={wish.id} className="flex flex-col gap-3 rounded-lg border p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={END_OF_LIFE_STATUS_LABELS[wish.status]} />
                  <span className="type-caption text-muted-foreground tabular-nums">{formatDateTime(wish.signedAt)}</span>
                </div>
                <WishDetails wish={wish} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
