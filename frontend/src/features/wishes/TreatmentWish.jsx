import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getResidentsByIdTreatmentWishes, postResidentsByIdTreatmentWishes } from '@/api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
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
import { POSITIVE_INTEGER, uploadProblem, toRequestOpinion } from './wishUtils'
import { OpinionEditor, OpinionList } from './Opinions'
import { ESCORT_LABELS, FINAL_STATUSES, OPINION_SOURCE_LABELS, TREATMENT_STATUS_LABELS, WISH_ERRORS } from './wishLabels'

// Khối "Nguyện vọng khi điều trị" của tab Nguyện vọng (001 T080; US7 #9 → #11; FR-023j → FR-023m, BR-M01-16; bố cục đề
// xuất 2026-10-23). Một hoặc hai bệnh viện ưu tiên, bảo hiểm y tế không bắt buộc, hỏi gia đình trước, người cần báo,
// người đi kèm, ý kiến khác có nguồn. Nội dung thuộc nhóm sức khỏe: Hành chính chỉ thấy "Đã có phiếu", Kế toán không thấy
// khối này (hasWish rỗng). Tạm thời mã người thân nhập tay vì chưa có danh sách (feature 012).

const WARNINGS = {
  NO_FAMILY_MEMBER_TO_NOTIFY: 'Phiếu yêu cầu hỏi gia đình trước nhưng chưa chọn người thân cần báo.',
}

function TreatmentDetails({ wish }) {
  const insurance = wish.healthInsurance
  return (
    <>
      <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
        {wish.preferredHospitals.map((hospital, index) => (
          <div key={index} className="col-span-2">
            <Row label={`Bệnh viện ưu tiên ${index + 1}`}>{`${hospital.name} · ${hospital.address}`}</Row>
          </div>
        ))}
        <Row label="Hỏi gia đình trước khi chuyển viện không khẩn cấp">{wish.askFamilyFirst ? 'Có' : 'Không'}</Row>
        <Row label="Người đi kèm">{ESCORT_LABELS[wish.accompaniedBy]}</Row>
        <div className="col-span-2">
          <Row label="Người thân cần báo">
            {wish.notifyFamilyMembers.length === 0
              ? 'Chưa chọn'
              : wish.notifyFamilyMembers
                  .map((m) => [m.fullName ?? `Mã ${m.familyMemberId}`, m.relationship, m.phoneNumber].filter(Boolean).join(', '))
                  .join('; ')}
          </Row>
        </div>
        {insurance && (
          <div className="col-span-2">
            <Row label="Bảo hiểm y tế">
              {[
                insurance.initialRegistrationPlace && `Nơi đăng ký ${insurance.initialRegistrationPlace}`,
                insurance.cardNumber && `Số thẻ ${insurance.cardNumber}`,
                insurance.cardExpiry && `Hạn ${insurance.cardExpiry}`,
              ]
                .filter(Boolean)
                .join(' · ')}
            </Row>
          </div>
        )}
        {wish.note && (
          <div className="col-span-2">
            <Row label="Ghi chú">{wish.note}</Row>
          </div>
        )}
        <div className="col-span-2">
          <Row label="Người trả lời">
            {OPINION_SOURCE_LABELS[wish.respondent.type]}
            {wish.respondent.familyMemberId != null && ` (mã ${wish.respondent.familyMemberId})`}
          </Row>
        </div>
        <Row label="Ghi bởi">{wish.recordedBy}</Row>
        <Row label="Ghi lúc">{formatDateTime(wish.at)}</Row>
      </dl>
      <OpinionList opinions={wish.otherOpinions} />
    </>
  )
}

function TreatmentForm({ residentId, onCancel, onDone }) {
  const [hospitals, setHospitals] = useState([{ name: '', address: '' }])
  const [insurance, setInsurance] = useState({ place: '', number: '', expiry: '' })
  const [ask, setAsk] = useState(false)
  const [idInput, setIdInput] = useState('')
  const [ids, setIds] = useState([])
  const [escort, setEscort] = useState(null)
  const [note, setNote] = useState('')
  const [respondentType, setRespondentType] = useState('RESIDENT')
  const [respondentId, setRespondentId] = useState('')
  const [opinions, setOpinions] = useState([])
  const [signedCopy, setSignedCopy] = useState(null)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key] = useState(() => crypto.randomUUID())
  const uploaded = useRef(new Map())

  function addId() {
    const text = idInput.trim()
    if (!POSITIVE_INTEGER.test(text)) {
      setErrors((current) => ({ ...current, notifyIds: 'Mã người thân là số nguyên dương' }))
      return
    }
    const value = Number(text)
    setIds((current) => (current.includes(value) ? current : [...current, value]))
    setIdInput('')
    setErrors((current) => ({ ...current, notifyIds: undefined }))
  }

  function setHospital(index, field, value) {
    setHospitals((current) => current.map((h, i) => (i === index ? { ...h, [field]: value } : h)))
  }

  function validate() {
    const next = {}
    if (!hospitals[0].name.trim() || !hospitals[0].address.trim()) next.hospital1 = WISH_ERRORS.PREFERRED_HOSPITAL_REQUIRED[1]
    if (hospitals[1] && (!hospitals[1].name.trim() || !hospitals[1].address.trim())) {
      next.hospital2 = 'Nhập đủ tên và địa chỉ bệnh viện thứ hai hoặc bỏ bệnh viện này'
    }
    if (!escort) next.escort = 'Chọn người đi kèm'
    if (respondentType === 'FAMILY_MEMBER' && !POSITIVE_INTEGER.test(respondentId.trim())) {
      next.respondentId = 'Nhập mã người thân trả lời'
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
    let uploading = true
    try {
      let signedCopyFileId
      if (signedCopy) {
        if (!uploaded.current.has(signedCopy)) uploaded.current.set(signedCopy, await uploadFile(signedCopy))
        signedCopyFileId = uploaded.current.get(signedCopy)
      }
      uploading = false
      const hasInsurance = insurance.place.trim() || insurance.number.trim() || insurance.expiry
      const { data } = await postResidentsByIdTreatmentWishes({
        path: { id: residentId },
        headers: { 'Idempotency-Key': key },
        body: {
          preferredHospitals: hospitals.map((h) => ({ name: h.name.trim(), address: h.address.trim() })),
          healthInsurance: hasInsurance
            ? {
                initialRegistrationPlace: insurance.place.trim() || undefined,
                cardNumber: insurance.number.trim() || undefined,
                cardExpiry: insurance.expiry || undefined,
              }
            : undefined,
          askFamilyFirst: ask,
          notifyFamilyMemberIds: ids,
          accompaniedBy: escort,
          note: note.trim() || undefined,
          respondent: {
            type: respondentType,
            familyMemberId: respondentType === 'FAMILY_MEMBER' ? Number(respondentId.trim()) : undefined,
          },
          otherOpinions: opinions.map(toRequestOpinion),
          signedCopyFileId,
        },
      })
      onDone(data)
    } catch (error) {
      const problem = problemOf(error)
      const fileMessage = uploading ? uploadProblem(problem) : null
      const mapped = error instanceof ApiError ? WISH_ERRORS[problem.errorCode] : null
      if (fileMessage) setErrors({ signedCopy: fileMessage })
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
      aria-label="Ghi phiếu nguyện vọng khi điều trị"
      className="flex flex-col gap-4 rounded-lg border bg-muted/30 p-4"
    >
      <h3 className="type-card-title">Ghi phiếu nguyện vọng khi điều trị</h3>
      <p className="type-body-sm text-muted-foreground">
        Phiếu mới thay phiếu Hiệu lực hiện hành; phiếu cũ chuyển Hết hiệu lực và vẫn xem được. Chưa có phiếu không chặn
        việc nào và không có nhắc định kỳ.
      </p>

      {hospitals.map((hospital, index) => (
        <fieldset key={index} className="flex flex-col gap-3 rounded-lg border p-3">
          <legend className="type-label px-1">{`Bệnh viện ưu tiên ${index + 1}`}</legend>
          <Field data-invalid={!!errors[`hospital${index + 1}`] || undefined}>
            <FieldLabel htmlFor={`tw-hospital-${index}-name`}>{`Tên bệnh viện ${index + 1}`}</FieldLabel>
            <Input
              id={`tw-hospital-${index}-name`}
              value={hospital.name}
              onChange={(e) => setHospital(index, 'name', e.target.value)}
              aria-invalid={!!errors[`hospital${index + 1}`] || undefined}
            />
          </Field>
          <Field data-invalid={!!errors[`hospital${index + 1}`] || undefined}>
            <FieldLabel htmlFor={`tw-hospital-${index}-address`}>{`Địa chỉ bệnh viện ${index + 1}`}</FieldLabel>
            <Input
              id={`tw-hospital-${index}-address`}
              value={hospital.address}
              onChange={(e) => setHospital(index, 'address', e.target.value)}
              aria-invalid={!!errors[`hospital${index + 1}`] || undefined}
            />
            {errors[`hospital${index + 1}`] && <FieldError>{errors[`hospital${index + 1}`]}</FieldError>}
          </Field>
          {index === 1 && (
            <Button type="button" variant="outline" className="h-10 self-start" onClick={() => setHospitals([hospitals[0]])}>
              Bỏ bệnh viện thứ hai
            </Button>
          )}
        </fieldset>
      ))}
      {hospitals.length === 1 && (
        <Button
          type="button"
          variant="outline"
          className="h-10 self-start"
          onClick={() => setHospitals([...hospitals, { name: '', address: '' }])}
        >
          Thêm bệnh viện thứ hai
        </Button>
      )}

      <fieldset className="flex flex-col gap-3 rounded-lg border p-3">
        <legend className="type-label px-1">Bảo hiểm y tế (không bắt buộc)</legend>
        <Field>
          <FieldLabel htmlFor="tw-ins-place">Nơi đăng ký khám chữa bệnh ban đầu</FieldLabel>
          <Input
            id="tw-ins-place"
            value={insurance.place}
            onChange={(e) => setInsurance({ ...insurance, place: e.target.value })}
          />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field>
            <FieldLabel htmlFor="tw-ins-number">Số thẻ</FieldLabel>
            <Input
              id="tw-ins-number"
              value={insurance.number}
              onChange={(e) => setInsurance({ ...insurance, number: e.target.value })}
            />
          </Field>
          <Field>
            <FieldLabel htmlFor="tw-ins-expiry">Hạn thẻ</FieldLabel>
            <Input
              id="tw-ins-expiry"
              type="date"
              value={insurance.expiry}
              onChange={(e) => setInsurance({ ...insurance, expiry: e.target.value })}
            />
          </Field>
        </div>
      </fieldset>

      <label className="flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border px-3">
        <Checkbox checked={ask} onCheckedChange={setAsk} aria-label="Hỏi gia đình trước khi chuyển viện không khẩn cấp" />
        <span className="type-body-sm">Hỏi gia đình trước khi chuyển viện không khẩn cấp</span>
      </label>

      <Field data-invalid={!!errors.notifyIds || undefined}>
        <FieldLabel htmlFor="tw-notify-id">Mã người thân cần báo</FieldLabel>
        <div className="flex gap-2">
          <Input
            id="tw-notify-id"
            inputMode="numeric"
            value={idInput}
            onChange={(e) => setIdInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                addId()
              }
            }}
            aria-invalid={!!errors.notifyIds || undefined}
          />
          <Button type="button" variant="outline" className="h-11" onClick={addId}>
            Thêm mã
          </Button>
        </div>
        {errors.notifyIds ? (
          <FieldError>{errors.notifyIds}</FieldError>
        ) : (
          <FieldDescription>
            Tạm thời nhập mã tới khi hồ sơ người thân (feature 012) có danh sách; để trống nếu chưa chọn. Hệ thống kiểm
            người này có quan hệ Hiệu lực.
          </FieldDescription>
        )}
        {ids.length > 0 && (
          <ul aria-label="Người thân cần báo" className="flex flex-wrap gap-2">
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

      <Field data-invalid={!!errors.escort || undefined}>
        <span className="type-label" id="tw-escort-label">
          Người đi kèm
        </span>
        <div role="radiogroup" aria-labelledby="tw-escort-label" className="flex flex-wrap gap-2">
          {Object.entries(ESCORT_LABELS).map(([value, label]) => (
            <Button
              key={value}
              type="button"
              role="radio"
              aria-checked={escort === value}
              variant={escort === value ? 'default' : 'outline'}
              className="h-10"
              onClick={() => setEscort(value)}
            >
              {label}
            </Button>
          ))}
        </div>
        {errors.escort && <FieldError>{errors.escort}</FieldError>}
      </Field>

      <Field>
        <FieldLabel htmlFor="tw-note">Ghi chú</FieldLabel>
        <Textarea id="tw-note" rows={2} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>

      <Field data-invalid={!!errors.respondentId || undefined}>
        <span className="type-label" id="tw-respondent-label">
          Người trả lời
        </span>
        <div role="radiogroup" aria-labelledby="tw-respondent-label" className="flex flex-wrap gap-2">
          {Object.entries(OPINION_SOURCE_LABELS).map(([value, label]) => (
            <Button
              key={value}
              type="button"
              role="radio"
              aria-checked={respondentType === value}
              variant={respondentType === value ? 'default' : 'outline'}
              className="h-10"
              onClick={() => setRespondentType(value)}
            >
              {label}
            </Button>
          ))}
        </div>
        {respondentType === 'FAMILY_MEMBER' && (
          <>
            <FieldLabel htmlFor="tw-respondent-id">Mã người thân trả lời</FieldLabel>
            <Input
              id="tw-respondent-id"
              inputMode="numeric"
              value={respondentId}
              onChange={(e) => setRespondentId(e.target.value)}
              aria-invalid={!!errors.respondentId || undefined}
            />
          </>
        )}
        {errors.respondentId && <FieldError>{errors.respondentId}</FieldError>}
      </Field>

      <OpinionEditor idPrefix="tw" items={opinions} onChange={setOpinions} error={errors.opinions} />

      <FileField
        id="tw-signed-copy"
        label="Bản ký đã scan (không bắt buộc)"
        error={errors.signedCopy}
        onPick={setSignedCopy}
      />

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

export function TreatmentWish({ residentId, status, allowedCommands = [], hasWish, onChanged }) {
  const [state, setState] = useState('loading')
  const [summary, setSummary] = useState(null)
  const [failure, setFailure] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [done, setDone] = useState(null)
  const [warnings, setWarnings] = useState([])

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByIdTreatmentWishes({ path: { id: residentId } })
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

  async function finished(result) {
    setFormOpen(false)
    setDone('Đã ghi phiếu nguyện vọng khi điều trị.')
    setWarnings(result?.warnings ?? [])
    await load()
    onChanged?.()
  }

  const heading = (
    <h3 id="tw-title" className="type-card-title">
      Nguyện vọng khi điều trị
    </h3>
  )

  if (state === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2">
        <span className="sr-only">Đang tải phiếu nguyện vọng khi điều trị</span>
        <Skeleton className="h-16" />
        <Skeleton className="h-24" />
      </div>
    )
  }
  if (state === 'forbidden') {
    // Kế toán không có cờ (hasWish rỗng): không hiện khối này (FR-023m)
    if (hasWish == null) return null
    return (
      <section aria-labelledby="tw-title" className="flex flex-col gap-2 rounded-lg border p-4">
        {heading}
        <p className="type-body-sm">{hasWish ? 'Đã có phiếu nguyện vọng khi điều trị.' : 'Chưa có phiếu nguyện vọng khi điều trị.'}</p>
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

  const current = summary.active
  const past = summary.history.filter((w) => w.status !== 'ACTIVE')
  const canRecord = allowedCommands.includes('RECORD_TREATMENT_WISH') && !FINAL_STATUSES.includes(status)

  return (
    <div className="flex flex-col gap-4">
      <section aria-labelledby="tw-title" className="flex flex-col gap-3 rounded-lg border p-4">
        <div className="flex flex-wrap items-center gap-2">
          {heading}
          {current && <StatusBadge status={TREATMENT_STATUS_LABELS.ACTIVE} />}
        </div>
        {current ? (
          <TreatmentDetails wish={current} />
        ) : (
          <Alert variant="attention">
            <AlertTitle>Chưa có phiếu nguyện vọng khi điều trị Hiệu lực</AlertTitle>
            <AlertDescription>
              Chưa có phiếu không chặn việc nào; lệnh Chuyển viện sẽ không có bệnh viện ưu tiên để tham khảo.
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
              setWarnings([])
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
      {warnings.map((w) => (
        <Alert key={w.code} variant="attention">
          <AlertTitle>Lưu ý</AlertTitle>
          <AlertDescription>{WARNINGS[w.code] ?? w.description}</AlertDescription>
        </Alert>
      ))}
      {formOpen && canRecord && (
        <TreatmentForm residentId={residentId} onCancel={() => setFormOpen(false)} onDone={finished} />
      )}

      <section aria-labelledby="tw-history-title" className="flex flex-col gap-2">
        <h4 id="tw-history-title" className="type-label">
          Lịch sử phiếu nguyện vọng khi điều trị
        </h4>
        {past.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa có phiếu nào hết hiệu lực.</p>
        ) : (
          <ul aria-label="Lịch sử phiếu nguyện vọng khi điều trị" className="flex flex-col gap-2">
            {past.map((wish) => (
              <li key={wish.id} className="flex flex-col gap-3 rounded-lg border p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={TREATMENT_STATUS_LABELS[wish.status]} />
                  <span className="type-caption text-muted-foreground tabular-nums">{formatDateTime(wish.at)}</span>
                </div>
                <TreatmentDetails wish={wish} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
