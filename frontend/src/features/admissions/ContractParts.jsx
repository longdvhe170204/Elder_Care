import { useEffect, useState } from 'react'
import {
  getContractsById,
  getContractsByIdAddenda,
  getContractsByIdDeposit,
  getContractsByIdNonStandardTerms,
  getResidentsByResidentIdRelationships,
  getServices,
  postContracts,
  postContractsByIdCancel,
  postContractsByIdRecordSigned,
  postContractsByIdReturnToDraft,
  postContractsByIdSendForSignature,
  postContractsByIdSubmitTermsForApproval,
  putContractsById,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { uploadFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import {
  ADDENDUM_STATUS,
  BILLING_PERIODS,
  CARE_LEVELS,
  CONTRACT_STATUS,
  RESIDENCE_TYPES,
  SELECT,
  labelOf,
  money,
  newKey,
} from './admissionLabels'
import { Choices, Info, ReasonField, ResidentSelect } from './FormParts'

// Khung chi tiết và form của hợp đồng (004 T038; US3, US4, FR-019 → FR-025a, FR-028 → FR-031). Nút lệnh theo
// allowedCommands của hợp đồng; trường tài chính ẩn theo quyền (hiddenFields); đặt cọc chỉ đọc vì 017 tính (FR-030).

const DEPOSIT_STATUS = { NOT_SATISFIED: 'Chưa đặt cọc', SATISFIED: 'Đã đặt cọc', NOT_REQUIRED: 'Không yêu cầu đặt cọc' }
const REASON_COMMANDS = {
  'submit-terms-for-approval': ['Gửi duyệt điều khoản…', 'Gửi Quản lý viện duyệt điều khoản khác chuẩn', postContractsByIdSubmitTermsForApproval, 'Đã gửi duyệt điều khoản'],
  'return-to-draft': ['Trả về nháp…', 'Trả hợp đồng về nháp', postContractsByIdReturnToDraft, 'Đã trả hợp đồng về nháp'],
  cancel: ['Hủy hợp đồng…', 'Hủy hợp đồng', postContractsByIdCancel, 'Đã hủy hợp đồng'],
}

function RecordSignedForm({ contract, onDone, onCancel }) {
  const [signedDate, setSignedDate] = useState('')
  const [representatives, setRepresentatives] = useState([])
  const [representativeId, setRepresentativeId] = useState('')
  const [file, setFile] = useState(null)
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    getResidentsByResidentIdRelationships({ path: { residentId: contract.residentId } })
      .then(({ data }) => setRepresentatives(data.filter((r) => r.status === 'ACTIVE')))
      .catch((e) => setFailure(problemOf(e)))
  }, [contract.residentId])

  const errors = {
    date: !signedDate && 'Nhập ngày ký',
    representative: !representativeId && 'Chọn người đại diện ký',
    file: !file && 'Chọn bản scan hợp đồng',
  }

  async function save() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      const scanFileId = await uploadFile(file)
      await postContractsByIdRecordSigned({
        path: { id: contract.id },
        headers: newKey(),
        body: { signedDate, signingRepresentativeId: Number(representativeId), scanFileId },
      })
      onDone('Đã ghi nhận hợp đồng đã ký, hợp đồng Hiệu lực')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <h3 className="type-card-title">Ghi nhận hợp đồng đã ký</h3>
      <FieldGroup>
        <Field data-invalid={(submitted && !!errors.date) || undefined}>
          <FieldLabel htmlFor="signed-date">Ngày ký</FieldLabel>
          <Input id="signed-date" type="date" className="h-11" value={signedDate} onChange={(e) => setSignedDate(e.target.value)} />
          {submitted && errors.date && <FieldError>{errors.date}</FieldError>}
        </Field>
        <Field data-invalid={(submitted && !!errors.representative) || undefined}>
          <FieldLabel htmlFor="signed-representative">Người đại diện ký</FieldLabel>
          <select id="signed-representative" className={SELECT} value={representativeId} onChange={(e) => setRepresentativeId(e.target.value)}>
            <option value="">Chọn người đại diện</option>
            {representatives.map((r) => (
              <option key={r.id} value={r.familyMember.id}>
                {r.familyMember.fullName}
              </option>
            ))}
          </select>
          {submitted && errors.representative && <FieldError>{errors.representative}</FieldError>}
        </Field>
        <Field data-invalid={(submitted && !!errors.file) || undefined}>
          <FieldLabel htmlFor="signed-scan">Bản scan hợp đồng</FieldLabel>
          <Input id="signed-scan" type="file" accept="application/pdf,image/*" className="h-11" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          {submitted && errors.file && <FieldError>{errors.file}</FieldError>}
        </Field>
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onCancel}>
          Đóng
        </Button>
        <Button className="h-11" disabled={busy} onClick={save}>
          Lưu đã ký
        </Button>
      </div>
    </div>
  )
}

export function ContractDetail({ contract, residentName, canUpdate, onChanged, onEdit, Title, Description }) {
  const [deposit, setDeposit] = useState(null)
  const [addenda, setAddenda] = useState([])
  const [terms, setTerms] = useState([])
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const allowed = contract.allowedCommands ?? []
  const hidden = contract.hiddenFields ?? []
  const draft = contract.status === 'DRAFT'

  useEffect(() => {
    let cancelled = false
    getContractsByIdDeposit({ path: { id: contract.id } })
      .then(({ data }) => !cancelled && setDeposit(data))
      .catch(() => {})
    getContractsByIdAddenda({ path: { id: contract.id } })
      .then(({ data }) => !cancelled && setAddenda(data))
      .catch(() => {})
    if (draft)
      getContractsByIdNonStandardTerms({ path: { id: contract.id } })
        .then(({ data }) => !cancelled && setTerms(data))
        .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [contract.id, draft])

  function open(next) {
    setMode(next)
    setReason('')
    setSubmitted(false)
    setFailure(null)
  }

  async function run(call, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await call({ path: { id: contract.id }, headers: newKey(), ...(body && { body }) })
      open(null)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  function submitReason() {
    setSubmitted(true)
    if (!reason.trim()) return
    const [, , call, message] = REASON_COMMANDS[mode]
    run(call, { reason: reason.trim() }, message)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={CONTRACT_STATUS[contract.status] ?? contract.status} />
        <Title className="type-title">{residentName}</Title>
        <Description className="type-body-sm text-muted-foreground">{`Hợp đồng ${contract.contractNumber ?? 'chưa có số'}`}</Description>
      </div>
      {contract.overdueSince && (
        <p className="type-body-sm text-destructive">{`Quá hạn từ ${formatDate(contract.overdueSince)}`}</p>
      )}
      <div className="grid gap-3">
        <Info label="Loại lưu trú">{labelOf(RESIDENCE_TYPES, contract.residenceType)}</Info>
        <Info label="Thời gian">
          {`Từ ${formatDate(contract.startDate)}${contract.endDate ? ` đến ${formatDate(contract.endDate)}` : ''}`}
        </Info>
        <Info label="Mức chăm sóc">{labelOf(CARE_LEVELS, contract.careLevel)}</Info>
        <Info label="Kỳ tính phí lưu trú">{labelOf(BILLING_PERIODS, contract.residencyFeeBillingPeriod)}</Info>
        {contract.signedDate && <Info label="Ngày ký">{formatDate(contract.signedDate)}</Info>}
        {contract.terminationReason && <Info label="Lý do chấm dứt">{contract.terminationReason}</Info>}
      </div>
      {hidden.length > 0 && (
        <p className="type-caption text-muted-foreground">Trường tài chính được ẩn theo quyền của bạn.</p>
      )}
      <ProblemAlert problem={failure} />

      {deposit && (
        <section className="flex flex-col gap-1">
          <h3 className="type-card-title">Đặt cọc</h3>
          <p className="type-body-sm">{DEPOSIT_STATUS[deposit.status] ?? deposit.status}</p>
          {deposit.requiredAmount !== undefined && deposit.requiredAmount !== null && (
            <p className="type-body-sm tabular-nums">{`Khoản cần đặt cọc: ${money(deposit.requiredAmount)}`}</p>
          )}
          <p className="type-caption text-muted-foreground">Kế toán xác nhận đặt cọc ở màn Số dư, tiền cọc.</p>
        </section>
      )}

      {terms.length > 0 && (
        <section className="flex flex-col gap-2">
          <h3 className="type-card-title">Điều khoản khác chuẩn</h3>
          <ul className="flex flex-col gap-1">
            {terms.map((t) => (
              <li key={t.type} className="type-body-sm">
                {t.description}
                <span className="block type-caption text-muted-foreground">{`Chuẩn: ${t.standardValue}; đề xuất: ${t.proposedValue}`}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {addenda.length > 0 && (
        <section className="flex flex-col gap-2">
          <h3 className="type-card-title">Phụ lục</h3>
          <ul className="flex flex-col gap-1">
            {addenda.map((a) => (
              <li key={a.id} className="type-body-sm">
                {`Hiệu lực ${formatDate(a.effectiveDate)}`}
                <span className="block type-caption text-muted-foreground">{ADDENDUM_STATUS[a.status] ?? a.status}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {mode === null && canUpdate && allowed.length > 0 && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          {allowed.includes('update') && (
            <Button variant="outline" className="h-11" onClick={() => onEdit(contract)}>
              Sửa nội dung
            </Button>
          )}
          {['submit-terms-for-approval', 'return-to-draft', 'cancel'].map(
            (command) =>
              allowed.includes(command) && (
                <Button key={command} variant="outline" className="h-11" onClick={() => open(command)}>
                  {REASON_COMMANDS[command][0]}
                </Button>
              ),
          )}
          {allowed.includes('record-signed') && (
            <Button className="h-11" onClick={() => open('record-signed')}>
              Ghi nhận đã ký…
            </Button>
          )}
          {allowed.includes('send-for-signature') && (
            <Button className="h-11" disabled={busy} onClick={() => run(postContractsByIdSendForSignature, null, 'Đã gửi ký')}>
              Gửi ký
            </Button>
          )}
        </div>
      )}

      {mode === 'record-signed' && <RecordSignedForm contract={contract} onCancel={() => open(null)} onDone={(m) => { open(null); onChanged(m) }} />}
      {mode && mode !== 'record-signed' && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <h3 className="type-card-title">{REASON_COMMANDS[mode][1]}</h3>
          <ReasonField id="contract-reason" value={reason} onChange={setReason} error={submitted && !reason.trim() ? 'Nhập lý do' : null} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => open(null)}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={submitReason}>
              {REASON_COMMANDS[mode][1]}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

const DAYS = [
  [1, 'Thứ hai'],
  [2, 'Thứ ba'],
  [3, 'Thứ tư'],
  [4, 'Thứ năm'],
  [5, 'Thứ sáu'],
  [6, 'Thứ bảy'],
  [7, 'Chủ nhật'],
]

// Lập hợp đồng Nháp hoặc sửa Nháp (tải lại để lấy ETag cho If-Match)
export function ContractSheet({ contract, onClose, onDone }) {
  const editing = !!contract
  const [etag, setEtag] = useState(null)
  const [catalog, setCatalog] = useState([])
  const [residentId, setResidentId] = useState(contract ? String(contract.residentId) : '')
  const [type, setType] = useState(contract?.residenceType ?? '')
  const [startDate, setStartDate] = useState(contract?.startDate ?? '')
  const [endDate, setEndDate] = useState(contract?.endDate ?? '')
  const [level, setLevel] = useState(contract?.careLevel ?? '')
  const [period, setPeriod] = useState(contract?.residencyFeeBillingPeriod ?? '')
  const [picked, setPicked] = useState(() => new Map((contract?.services ?? []).map((s) => [s.serviceId, !!s.inPackage])))
  const [noDeposit, setNoDeposit] = useState(!!contract?.noDepositRequired)
  const [deposit, setDeposit] = useState(contract?.depositAmount ?? '')
  const [schedule, setSchedule] = useState(contract?.arrivalSchedule ?? [])
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    getServices({ query: {} })
      .then(({ data }) => setCatalog(data.filter((s) => s.status === 'ACTIVE')))
      .catch(() => {})
    if (editing)
      getContractsById({ path: { id: contract.id } })
        .then(({ etag: tag }) => setEtag(tag))
        .catch((e) => setFailure(problemOf(e)))
  }, [editing, contract?.id])

  const errors = {
    resident: !residentId && 'Chọn người cao tuổi',
    type: !type && 'Chọn loại lưu trú',
    start: !startDate && 'Nhập ngày bắt đầu',
    end: type === 'SHORT_TERM_RESIDENTIAL' && !endDate && 'Nội trú ngắn ngày cần ngày kết thúc',
    level: !level && 'Chọn mức chăm sóc',
    period: !period && 'Chọn kỳ tính phí lưu trú',
  }
  const shown = (key) => (submitted ? errors[key] : null)

  async function save() {
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    const body = {
      residentId: Number(residentId),
      residenceType: type,
      startDate,
      ...(endDate && { endDate }),
      careLevel: level,
      residencyFeeBillingPeriod: period,
      services: [...picked].map(([serviceId, inPackage]) => ({ serviceId, inPackage })),
      noDepositRequired: noDeposit,
      ...(!noDeposit && deposit !== '' && { depositAmount: Number(deposit) }),
      ...(type === 'DAY_CARE' && { arrivalSchedule: schedule }),
    }
    try {
      if (editing) {
        await putContractsById({ path: { id: contract.id }, headers: { ...newKey(), 'If-Match': etag }, body })
        onDone('Đã sửa hợp đồng nháp')
      } else {
        await postContracts({ headers: newKey(), body })
        onDone('Đã lập hợp đồng nháp')
      }
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  function toggleService(id) {
    setPicked((current) => {
      const next = new Map(current)
      if (next.has(id)) next.delete(id)
      else next.set(id, false)
      return next
    })
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle>{editing ? 'Sửa hợp đồng nháp' : 'Lập hợp đồng nháp'}</SheetTitle>
        <SheetDescription>Đúng một dịch vụ chăm sóc theo mức của hợp đồng; người cao tuổi phải có người đại diện.</SheetDescription>
        <FieldGroup className="mt-4">
          {editing ? (
            <Info label="Người cao tuổi">{`Mã ${contract.residentId}`}</Info>
          ) : (
            <ResidentSelect id="contract-resident" value={residentId} onChange={setResidentId} error={shown('resident')} onFailure={(e) => setFailure(problemOf(e))} />
          )}
          <Choices label="Loại lưu trú" options={RESIDENCE_TYPES} value={type} onChange={setType} error={shown('type')} />
          <Field data-invalid={!!shown('start') || undefined}>
            <FieldLabel htmlFor="contract-start">Ngày bắt đầu</FieldLabel>
            <Input id="contract-start" type="date" className="h-11" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            {shown('start') && <FieldError>{shown('start')}</FieldError>}
          </Field>
          <Field data-invalid={!!shown('end') || undefined}>
            <FieldLabel htmlFor="contract-end">Ngày kết thúc</FieldLabel>
            <Input id="contract-end" type="date" className="h-11" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
            {shown('end') && <FieldError>{shown('end')}</FieldError>}
          </Field>
          <Choices label="Mức chăm sóc" options={CARE_LEVELS} value={level} onChange={setLevel} error={shown('level')} />
          <Choices label="Kỳ tính phí lưu trú" options={BILLING_PERIODS} value={period} onChange={setPeriod} error={shown('period')} />

          <Field>
            <FieldLabel>Dịch vụ trong hợp đồng</FieldLabel>
            {catalog.length === 0 ? (
              <p className="type-body-sm text-muted-foreground">Chưa có dịch vụ hiệu lực trong danh mục.</p>
            ) : (
              <ul className="flex flex-col gap-2">
                {catalog.map((s) => (
                  <li key={s.id} className="flex flex-wrap items-center gap-3">
                    <label className="flex items-center gap-2 type-body-sm">
                      <input type="checkbox" checked={picked.has(s.id)} onChange={() => toggleService(s.id)} />
                      {s.name}
                    </label>
                    {picked.has(s.id) && (
                      <label className="flex items-center gap-2 type-caption text-muted-foreground">
                        <input
                          type="checkbox"
                          checked={picked.get(s.id)}
                          onChange={(e) => setPicked((c) => new Map(c).set(s.id, e.target.checked))}
                        />
                        {`Thuộc gói (${s.name})`}
                      </label>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Field>

          <Field>
            <label className="flex items-center gap-2 type-body-sm">
              <input type="checkbox" checked={noDeposit} onChange={(e) => setNoDeposit(e.target.checked)} />
              Không yêu cầu đặt cọc
            </label>
          </Field>
          {!noDeposit && (
            <Field>
              <FieldLabel htmlFor="contract-deposit">Khoản đặt cọc (đồng)</FieldLabel>
              <Input id="contract-deposit" type="number" min="0" className="h-11" value={deposit} onChange={(e) => setDeposit(e.target.value)} />
            </Field>
          )}

          {type === 'DAY_CARE' && (
            <Field>
              <FieldLabel>Lịch đến theo tuần</FieldLabel>
              {schedule.map((row, i) => (
                <div key={i} className="flex flex-wrap items-center gap-2">
                  <select
                    aria-label={`Thứ trong tuần dòng ${i + 1}`}
                    className={SELECT}
                    value={row.dayOfWeek ?? ''}
                    onChange={(e) => setSchedule((s) => s.map((r, j) => (j === i ? { ...r, dayOfWeek: Number(e.target.value) } : r)))}
                  >
                    <option value="">Chọn thứ</option>
                    {DAYS.map(([d, text]) => (
                      <option key={d} value={d}>
                        {text}
                      </option>
                    ))}
                  </select>
                  <Input
                    aria-label={`Giờ đến dòng ${i + 1}`}
                    type="time"
                    className="h-11 w-32"
                    value={row.arrivalTime ?? ''}
                    onChange={(e) => setSchedule((s) => s.map((r, j) => (j === i ? { ...r, arrivalTime: e.target.value } : r)))}
                  />
                  <Input
                    aria-label={`Giờ về dòng ${i + 1}`}
                    type="time"
                    className="h-11 w-32"
                    value={row.departureTime ?? ''}
                    onChange={(e) => setSchedule((s) => s.map((r, j) => (j === i ? { ...r, departureTime: e.target.value } : r)))}
                  />
                  <Button variant="ghost" className="h-11" onClick={() => setSchedule((s) => s.filter((_, j) => j !== i))}>
                    {`Bỏ dòng ${i + 1}`}
                  </Button>
                </div>
              ))}
              <Button variant="outline" className="h-11 self-start" onClick={() => setSchedule((s) => [...s, {}])}>
                Thêm dòng lịch
              </Button>
            </Field>
          )}
        </FieldGroup>
        <div className="mt-4">
          <ProblemAlert problem={failure} />
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" className="h-11" onClick={onClose}>
            Đóng
          </Button>
          <Button className="h-11" disabled={busy || (editing && !etag)} onClick={save}>
            Lưu hợp đồng nháp
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
