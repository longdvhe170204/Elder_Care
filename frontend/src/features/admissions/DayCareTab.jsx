import { useCallback, useEffect, useState } from 'react'
import {
  getDayCareClosureDays,
  postDayCareAbsenceNotices,
  postDayCareClosureDays,
  postDayCareClosureDaysByIdCancel,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { newKey, vietnamIso } from './admissionLabels'
import { ReasonField, ResidentSelect } from './FormParts'

// Bán trú (004 T056; FR-060, FR-060a): ghi nhận gia đình báo vắng (hệ thống tự xác định đủ hạn hay muộn theo CFG-M02-06)
// và khai báo, hủy ngày khu bán trú nghỉ. Chỉ Hành chính lập (Phụ lục 27).

function NoticeForm({ canWrite }) {
  const [residentId, setResidentId] = useState('')
  const [date, setDate] = useState('')
  const [notified, setNotified] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [result, setResult] = useState('')
  const [busy, setBusy] = useState(false)
  const errors = {
    resident: !residentId && 'Chọn người cao tuổi',
    date: !date && 'Nhập ngày vắng',
    notified: !notified && 'Nhập thời điểm gia đình báo',
  }

  async function save() {
    setSubmitted(true)
    setResult('')
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      const { data } = await postDayCareAbsenceNotices({
        headers: newKey(),
        body: { residentId: Number(residentId), date, familyNotifiedAt: vietnamIso(notified) },
      })
      setResult(data?.withinNoticePeriod ? 'Báo đủ hạn' : 'Báo muộn, không đủ hạn báo trước')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card className="gap-4 p-4">
      <h2 className="type-card-title">Báo vắng bán trú</h2>
      <FieldGroup>
        <ResidentSelect
          id="notice-resident"
          statuses={['IN_RESIDENCE']}
          value={residentId}
          onChange={setResidentId}
          error={submitted ? errors.resident : null}
          onFailure={(e) => setFailure(problemOf(e))}
        />
        <Field data-invalid={(submitted && !!errors.date) || undefined}>
          <FieldLabel htmlFor="notice-date">Ngày vắng</FieldLabel>
          <Input id="notice-date" type="date" className="h-11" value={date} onChange={(e) => setDate(e.target.value)} />
          {submitted && errors.date && <FieldError>{errors.date}</FieldError>}
        </Field>
        <Field data-invalid={(submitted && !!errors.notified) || undefined}>
          <FieldLabel htmlFor="notice-at">Gia đình báo lúc</FieldLabel>
          <Input
            id="notice-at"
            type="datetime-local"
            className="h-11"
            value={notified}
            onChange={(e) => setNotified(e.target.value)}
          />
          {submitted && errors.notified && <FieldError>{errors.notified}</FieldError>}
        </Field>
      </FieldGroup>
      <ProblemAlert problem={failure} />
      <p role="status" className="empty:hidden type-body-sm">
        {result}
      </p>
      {canWrite && (
        <div className="flex justify-end">
          <Button className="h-11" disabled={busy} onClick={save}>
            Ghi nhận báo vắng
          </Button>
        </div>
      )}
    </Card>
  )
}

function ClosureDays({ canWrite }) {
  const [days, setDays] = useState(null)
  const [date, setDate] = useState('')
  const [reason, setReason] = useState('')
  const [cancelId, setCancelId] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(
    () =>
      getDayCareClosureDays({ query: {} })
        .then(({ data }) => setDays(data))
        .catch((e) => {
          setDays([])
          setFailure(problemOf(e))
        }),
    [],
  )
  useEffect(() => {
    load()
  }, [load])

  async function declare() {
    setSubmitted(true)
    if (!date || !reason.trim()) return
    setBusy(true)
    setFailure(null)
    try {
      await postDayCareClosureDays({ headers: newKey(), body: { date, reason: reason.trim() } })
      setDate('')
      setReason('')
      setSubmitted(false)
      load()
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  async function cancel() {
    if (!cancelReason.trim()) return
    setBusy(true)
    try {
      await postDayCareClosureDaysByIdCancel({
        path: { id: cancelId },
        headers: newKey(),
        body: { reason: cancelReason.trim() },
      })
      setCancelId(null)
      setCancelReason('')
      load()
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card className="gap-4 p-4">
      <h2 className="type-card-title">Ngày khu bán trú nghỉ</h2>
      {days === null ? (
        <p className="type-body-sm text-muted-foreground">Đang tải…</p>
      ) : days.length === 0 ? (
        <p className="type-body-sm text-muted-foreground">Chưa khai báo ngày nghỉ nào.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {days.map((d) => (
            <li key={d.id} className="flex flex-wrap items-center justify-between gap-2">
              <span className="type-body-sm tabular-nums">
                {formatDate(d.date)}
                <span className="block type-caption text-muted-foreground">{d.reason}</span>
              </span>
              <span className="flex items-center gap-2">
                <StatusBadge status={d.status === 'ACTIVE' ? 'Hiệu lực' : 'Đã hủy'} />
                {canWrite && d.status === 'ACTIVE' && (
                  <Button variant="outline" className="h-11" onClick={() => setCancelId(d.id)}>
                    {`Hủy ngày nghỉ ${formatDate(d.date)}`}
                  </Button>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}
      {cancelId && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <ReasonField id="closure-cancel-reason" value={cancelReason} onChange={setCancelReason} error={null} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => setCancelId(null)}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={cancel}>
              Xác nhận hủy ngày nghỉ
            </Button>
          </div>
        </div>
      )}
      {canWrite && (
        <FieldGroup className="border-t pt-4">
          <Field data-invalid={(submitted && !date) || undefined}>
            <FieldLabel htmlFor="closure-date">Ngày nghỉ</FieldLabel>
            <Input
              id="closure-date"
              type="date"
              className="h-11"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
            {submitted && !date && <FieldError>Nhập ngày nghỉ</FieldError>}
          </Field>
          <ReasonField
            id="closure-reason"
            label="Lý do nghỉ"
            value={reason}
            onChange={setReason}
            error={submitted && !reason.trim() ? 'Nhập lý do' : null}
          />
          <div className="flex justify-end">
            <Button className="h-11" disabled={busy} onClick={declare}>
              Khai báo ngày nghỉ
            </Button>
          </div>
        </FieldGroup>
      )}
      <ProblemAlert problem={failure} />
    </Card>
  )
}

export function DayCareTab({ canWrite }) {
  return (
    <div className="grid gap-5 lg:grid-cols-2">
      <NoticeForm canWrite={canWrite} />
      <ClosureDays canWrite={canWrite} />
    </div>
  )
}
