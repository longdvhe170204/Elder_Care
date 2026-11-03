import { useEffect, useState } from 'react'
import { getResultValues } from '@/api'
import { Button } from '@/components/ui/button'
import { FieldError } from '@/components/ui/field'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { CareCheck, CareField } from './CareField'

const CODES = {
  EATING: { ATE_ALL: 'Ăn hết', MOST: 'Phần lớn', SOME: 'Một phần', NOT_EATEN: 'Không ăn', SKIPPED: 'Bỏ bữa' },
  MOVEMENT: { INDEPENDENT: 'Tự thực hiện', ASSISTED: 'Cần hỗ trợ', UNABLE: 'Không thực hiện được' },
}
const CATALOG = { SLEEP: 'SLEEP_QUALITY', MOOD: 'MOOD', BEHAVIOR: 'BEHAVIOR' }
const NUMERIC = {
  ML: 'Lượng nước (ml)',
  ITEM_QUANTITY: 'Số lượng',
  MEASURED_VALUE: 'Giá trị đo',
  SLEEP: 'Thời gian ngủ (phút)',
}

// FR-038: chọn form bằng snapshot của công việc; danh mục hiện hành chỉ cung cấp lựa chọn mã.
export function CareResultForm({ type, busy, failure, importance, onSubmit }) {
  const [numeric, setNumeric] = useState('')
  const [code, setCode] = useState('')
  const [occurred, setOccurred] = useState('')
  const [description, setDescription] = useState('')
  const [note, setNote] = useState('')
  const [performedAt, setPerformedAt] = useState('')
  const [reason, setReason] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [options, setOptions] = useState({})
  const [catalogError, setCatalogError] = useState(null)
  const [missing, setMissing] = useState(false)
  useEffect(() => {
    if (!CATALOG[type]) return
    let active = true
    getResultValues()
      .then(({ data }) => {
        if (active)
          setOptions(
            Object.fromEntries(
              data.filter((r) => r.status === 'ACTIVE' && r.type === CATALOG[type]).map((r) => [r.code, r.name]),
            ),
          )
      })
      .catch((error) => {
        if (active) setCatalogError(problemOf(error))
      })
    return () => {
      active = false
    }
  }, [type])
  if (
    !['EATING', 'ML', 'MEASURED_VALUE', 'MOVEMENT', 'SLEEP', 'MOOD', 'BEHAVIOR', 'ITEM_QUANTITY', 'PERFORMED'].includes(
      type,
    )
  )
    return <p>Loại công việc này không ghi kết quả tại checklist.</p>
  const needsCode =
    Boolean(CODES[type]) || ['SLEEP', 'MOOD'].includes(type) || (type === 'BEHAVIOR' && occurred === 'yes')
  function submit(event) {
    event.preventDefault()
    if (
      (NUMERIC[type] && (numeric === '' || !Number.isFinite(Number(numeric)))) ||
      (needsCode && !code) ||
      (type === 'BEHAVIOR' && (!occurred || (occurred === 'yes' && !description.trim())))
    ) {
      setMissing(true)
      return
    }
    onSubmit({
      ...(NUMERIC[type] && { numericValue: Number(numeric) }),
      ...(needsCode && type !== 'BEHAVIOR' && { codeValue: code }),
      ...(type === 'BEHAVIOR' && {
        jsonValue: { occurred: occurred === 'yes', ...(occurred === 'yes' && { behaviorCode: code, description }) },
      }),
      ...(performedAt && { performedAt: `${performedAt}:00+07:00` }),
      ...(note && { note }),
      ...(reason && { reason }),
      ...(confirmed && { upperLimitConfirmed: true }),
    })
  }
  return (
    <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
      <ProblemAlert problem={catalogError} />
      {NUMERIC[type] && (
        <CareField
          label={NUMERIC[type]}
          type="number"
          value={numeric}
          onChange={(v) => {
            setNumeric(v)
            setConfirmed(false)
          }}
          disabled={busy}
        />
      )}
      {type === 'BEHAVIOR' && (
        <CareField
          label="Có hành vi bất thường"
          options={{ no: 'Không', yes: 'Có' }}
          value={occurred}
          onChange={setOccurred}
          disabled={busy}
        />
      )}
      {needsCode && (
        <CareField
          label={
            {
              EATING: 'Mức ăn',
              MOVEMENT: 'Khả năng vận động',
              SLEEP: 'Chất lượng ngủ',
              MOOD: 'Tâm trạng',
              BEHAVIOR: 'Loại hành vi',
            }[type]
          }
          options={CODES[type] ?? options}
          value={code}
          onChange={setCode}
          disabled={busy}
        />
      )}
      {type === 'BEHAVIOR' && occurred === 'yes' && (
        <CareField
          label="Mô tả hành vi"
          type="textarea"
          maxLength={2000}
          value={description}
          onChange={setDescription}
          disabled={busy}
        />
      )}
      <CareField
        label="Thời điểm thực hiện"
        type="datetime-local"
        value={performedAt}
        onChange={setPerformedAt}
        disabled={busy}
      />
      <p className="type-body-sm text-muted-foreground">Giờ Việt Nam. Để trống để dùng thời điểm gửi ghi nhận.</p>
      <CareField
        label="Ghi chú kết quả"
        type="textarea"
        maxLength={2000}
        value={note}
        onChange={setNote}
        disabled={busy}
      />
      {['IMPORTANT', 'MANDATORY'].includes(importance) && (
        <CareField
          label="Lý do hoàn thành trễ (nếu có)"
          type="textarea"
          maxLength={2000}
          value={reason}
          onChange={setReason}
          disabled={busy}
        />
      )}
      {failure?.errorCode === 'CONFIRMATION_REQUIRED' && (
        <CareCheck
          label="Xác nhận giá trị đã nhập vượt giới hạn hợp lý"
          checked={confirmed}
          onChange={setConfirmed}
          disabled={busy}
        />
      )}
      {missing && <FieldError>Nhập đầy đủ kết quả trước khi lưu.</FieldError>}
      <Button type="submit" disabled={busy || Boolean(catalogError)}>
        Lưu kết quả
      </Button>
    </form>
  )
}
