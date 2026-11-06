import { useEffect, useState } from 'react'
import {
  getAbsencesById,
  postAbsencesByIdCorrect,
  postAbsencesByIdExtendExpectedReturn,
  postAbsencesByIdReleaseBed,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ABSENCE_STATUS, ABSENCE_TYPES, newKey, vietnamIso } from './admissionLabels'
import { Info, ReasonField } from './FormParts'

// Khung chi tiết lượt vắng (004 T056; FR-048 → FR-061). Gia hạn: Hành chính, Trưởng tầng (FR-049); Giải phóng giường: chỉ
// Quản lý viện (Q-395, BR-M02-07); đính chính lượt đã đóng theo 000 FR-026 (BR-M02-18). Cho tạm vắng và Ghi nhận trở về là
// lệnh của 001 ở tab Trạng thái của hồ sơ người cao tuổi.

export function absenceLabel(absence) {
  const type = ABSENCE_TYPES[absence.absenceType] ?? absence.absenceType
  return absence.status === 'OPEN'
    ? `${type}, dự kiến trở lại ${absence.expectedReturnAt ? formatDateTime(absence.expectedReturnAt) : 'chưa có'}`
    : `${type}, Đã đóng`
}

export function AbsenceDetail({ absence, role, canUpdate, onChanged, Title, Description }) {
  const [detail, setDetail] = useState(absence)
  const [mode, setMode] = useState(null)
  const [expected, setExpected] = useState('')
  const [departed, setDeparted] = useState('')
  const [returned, setReturned] = useState('')
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const open = absence.status === 'OPEN'

  useEffect(() => {
    let cancelled = false
    getAbsencesById({ path: { id: absence.id } })
      .then(({ data }) => !cancelled && setDetail(data))
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [absence.id])

  function openMode(next) {
    setMode(next)
    setReason('')
    setExpected('')
    setDeparted('')
    setReturned('')
    setSubmitted(false)
    setFailure(null)
  }

  async function run(call, body, message) {
    setBusy(true)
    setFailure(null)
    try {
      await call({ path: { id: absence.id }, headers: newKey(), body })
      openMode(null)
      onChanged(message)
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  function submit() {
    setSubmitted(true)
    if (!reason.trim()) return
    if (mode === 'extend') {
      if (!expected) return
      run(postAbsencesByIdExtendExpectedReturn, { expectedReturnAt: vietnamIso(expected), reason: reason.trim() }, 'Đã gia hạn dự kiến trở lại')
    } else if (mode === 'release') {
      run(postAbsencesByIdReleaseBed, { reason: reason.trim() }, 'Đã giải phóng giường')
    } else {
      const corrected = { ...(departed && { departedAt: vietnamIso(departed) }), ...(returned && { returnedAt: vietnamIso(returned) }) }
      if (Object.keys(corrected).length === 0) return
      run(postAbsencesByIdCorrect, { type: 'EDIT_CONTENT', correctedContent: corrected, reason: reason.trim() }, 'Đã đính chính lượt vắng')
    }
  }

  const reasonError = submitted && !reason.trim() ? 'Nhập lý do' : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={ABSENCE_STATUS[detail.status] ?? detail.status} />
        <Title className="type-title">{ABSENCE_TYPES[detail.absenceType] ?? detail.absenceType}</Title>
        <Description className="type-body-sm text-muted-foreground">{`Rời viện ${formatDateTime(detail.departedAt)}`}</Description>
      </div>
      <div className="grid gap-3">
        {detail.expectedReturnAt && <Info label="Dự kiến trở lại">{formatDateTime(detail.expectedReturnAt)}</Info>}
        {detail.returnedAt && <Info label="Đã trở lại">{formatDateTime(detail.returnedAt)}</Info>}
        {detail.pickedUpBy && <Info label="Người đón">{detail.pickedUpBy}</Info>}
        {detail.handoverStaff && <Info label="Nhân viên bàn giao">{detail.handoverStaff}</Info>}
        <Info label="Giữ giường">{detail.holdBed ? 'Có' : 'Không'}</Info>
      </div>
      {detail.expectedReturnHistory?.length > 0 && (
        <section className="flex flex-col gap-2">
          <h3 className="type-card-title">Lịch sử đổi dự kiến trở lại</h3>
          <ul className="flex flex-col gap-1">
            {detail.expectedReturnHistory.map((h, i) => (
              <li key={i} className="type-body-sm">
                {`${formatDateTime(h.oldValue)} → ${formatDateTime(h.newValue)}`}
                <span className="block type-caption text-muted-foreground">{`${h.reason}, ${formatDateTime(h.at)}`}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
      <ProblemAlert problem={failure} />

      {mode === null && canUpdate && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          {open && ['OFFICE_STAFF', 'FLOOR_MANAGER'].includes(role) && (
            <Button variant="outline" className="h-11" onClick={() => openMode('extend')}>
              Gia hạn dự kiến trở lại…
            </Button>
          )}
          {open && detail.holdBed && role === 'NURSING_HOME_MANAGER' && (
            <Button variant="outline" className="h-11" onClick={() => openMode('release')}>
              Giải phóng giường…
            </Button>
          )}
          {!open && (
            <Button variant="outline" className="h-11" onClick={() => openMode('correct')}>
              Đính chính lượt vắng…
            </Button>
          )}
        </div>
      )}

      {mode !== null && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <FieldGroup>
            {mode === 'extend' && (
              <Field data-invalid={(submitted && !expected) || undefined}>
                <FieldLabel htmlFor="absence-expected">Dự kiến trở lại</FieldLabel>
                <Input id="absence-expected" type="datetime-local" className="h-11" value={expected} onChange={(e) => setExpected(e.target.value)} />
                {submitted && !expected && <FieldError>Nhập thời điểm dự kiến trở lại</FieldError>}
              </Field>
            )}
            {mode === 'release' && (
              <p className="type-body-sm text-muted-foreground">
                Giường được giải phóng ngay; từ ngày giải phóng, ngày vắng không tính phí lưu trú.
              </p>
            )}
            {mode === 'correct' && (
              <>
                <Field>
                  <FieldLabel htmlFor="absence-departed">Thời điểm rời đúng</FieldLabel>
                  <Input id="absence-departed" type="datetime-local" className="h-11" value={departed} onChange={(e) => setDeparted(e.target.value)} />
                </Field>
                <Field>
                  <FieldLabel htmlFor="absence-returned">Thời điểm về đúng</FieldLabel>
                  <Input id="absence-returned" type="datetime-local" className="h-11" value={returned} onChange={(e) => setReturned(e.target.value)} />
                </Field>
              </>
            )}
            <ReasonField id="absence-reason" value={reason} onChange={setReason} error={reasonError} />
          </FieldGroup>
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => openMode(null)}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={submit}>
              {{ extend: 'Lưu gia hạn', release: 'Xác nhận giải phóng giường', correct: 'Lưu đính chính' }[mode]}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
