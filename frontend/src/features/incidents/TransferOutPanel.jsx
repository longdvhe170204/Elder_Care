import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { Skeleton } from '@/components/ui/skeleton'
import { problemOf } from '@/common/problem'
import { HandoverStaffField } from './HandoverStaffField'
import { vietnamIso } from './incidentView'
import { downloadTransferDocument, loadTransferInfo, submitTransferOut } from './transferOut'

// Chuyển viện từ sự cố (007 T105; FR-049, FR-050; BR-M05-14, Q-383, Q-384): đơn vị nhận, thời điểm, người đi cùng; hỏi
// người liên hệ chính trước khi phiếu điều trị có yêu cầu hỏi trước, chưa hỏi được thì ghi lý do (trừ sự cố Khẩn cấp).
// Xong thì tải, in bản tóm tắt chuyển viện (PDF); in lại ghi nhật ký.

export function TransferOutPanel({ incidentId, onDone, onCancel }) {
  const [info, setInfo] = useState(null)
  const [problem, setProblem] = useState(null)
  const [unit, setUnit] = useState('')
  const [at, setAt] = useState('')
  const [returnAt, setReturnAt] = useState('')
  const [companion, setCompanion] = useState('')
  const [staffId, setStaffId] = useState('')
  const [reached, setReached] = useState('')
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [summaryId, setSummaryId] = useState(null)

  useEffect(() => {
    let cancelled = false
    loadTransferInfo(incidentId).then(
      (data) => !cancelled && setInfo(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [incidentId])

  async function download(reprint) {
    setBusy(true)
    setProblem(null)
    try {
      await downloadTransferDocument(summaryId, reprint)
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (summaryId) {
    return (
      <div className="flex flex-col gap-4">
        <h3 className="type-card-title">Đã chuyển viện</h3>
        <p className="type-body-sm">Đã tạo bản tóm tắt chuyển viện. Tải hoặc in để giao cho đơn vị nhận.</p>
        {problem && <ProblemAlert problem={problem} />}
        <div className="flex flex-wrap gap-2">
          <Button className="h-11" disabled={busy} onClick={() => download(false)}>
            Tải bản tóm tắt
          </Button>
          <Button variant="outline" className="h-11" disabled={busy} onClick={() => download(true)}>
            In lại
          </Button>
          <Button variant="ghost" className="h-11" onClick={onDone}>
            Xong
          </Button>
        </div>
      </div>
    )
  }

  if (!info) return problem ? <ProblemAlert problem={problem} /> : <Skeleton className="h-40 w-full" />

  const askFirst = info.mustAskFirst
  const missing = {
    unit: !unit.trim(),
    at: !at,
    companion: !companion.trim(),
    staff: !staffId,
    reached: askFirst && !reached,
    reason: askFirst && reached === 'no' && !info.isEmergency && !reason.trim(),
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(missing).some(Boolean)) return
    setBusy(true)
    setProblem(null)
    try {
      const body = {
        transferUnit: unit.trim(),
        transferAt: vietnamIso(at),
        companion: companion.trim(),
        handoverStaffId: Number(staffId),
        ...(returnAt ? { expectedReturnAt: vietnamIso(returnAt) } : {}),
        ...(askFirst ? { requiredContactReached: reached === 'yes' } : {}),
        ...(askFirst && reached === 'no' && reason.trim() ? { notAskedFirstReason: reason.trim() } : {}),
      }
      const summary = await submitTransferOut(incidentId, body)
      setSummaryId(summary.id)
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const contact = info.primaryContact

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4">
      <h3 className="type-card-title">Chuyển viện</h3>
      {contact && (
        <p className="type-body-sm text-muted-foreground">
          Người liên hệ chính: {contact.fullName} {contact.phoneNumber ?? ''}
        </p>
      )}
      <Field data-invalid={(submitted && missing.unit) || undefined}>
        <FieldLabel htmlFor="transfer-unit">Đơn vị nhận</FieldLabel>
        <Input id="transfer-unit" value={unit} onChange={(e) => setUnit(e.target.value)} />
        {submitted && missing.unit && <FieldError>Nhập đơn vị nhận</FieldError>}
      </Field>
      <Field data-invalid={(submitted && missing.at) || undefined}>
        <FieldLabel htmlFor="transfer-at">Thời điểm chuyển</FieldLabel>
        <Input id="transfer-at" type="datetime-local" value={at} onChange={(e) => setAt(e.target.value)} />
        {submitted && missing.at && <FieldError>Chọn thời điểm chuyển</FieldError>}
      </Field>
      <Field>
        <FieldLabel htmlFor="transfer-return">Dự kiến về (nếu có)</FieldLabel>
        <Input
          id="transfer-return"
          type="datetime-local"
          value={returnAt}
          onChange={(e) => setReturnAt(e.target.value)}
        />
      </Field>
      <Field data-invalid={(submitted && missing.companion) || undefined}>
        <FieldLabel htmlFor="transfer-companion">Người đi cùng (họ tên, quan hệ hoặc chức danh)</FieldLabel>
        <Input id="transfer-companion" value={companion} onChange={(e) => setCompanion(e.target.value)} />
        {submitted && missing.companion && <FieldError>Nhập người đi cùng</FieldError>}
      </Field>
      <HandoverStaffField
        id="transfer-handover-staff"
        value={staffId}
        onChange={setStaffId}
        invalid={submitted && missing.staff}
      />
      {askFirst && (
        <>
          <Field data-invalid={(submitted && missing.reached) || undefined}>
            <FieldLabel>Đã hỏi người liên hệ chính trước khi chuyển</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={reached ? [reached] : []}
              onValueChange={(n) => setReached(n[0] ?? '')}
              className="flex flex-wrap"
            >
              <ToggleGroupItem value="yes" className="h-11 px-4">
                Đã hỏi
              </ToggleGroupItem>
              <ToggleGroupItem value="no" className="h-11 px-4">
                Chưa hỏi được
              </ToggleGroupItem>
            </ToggleGroup>
            {submitted && missing.reached && <FieldError>Chọn một đáp án</FieldError>}
          </Field>
          {reached === 'no' && !info.isEmergency && (
            <Field data-invalid={(submitted && missing.reason) || undefined}>
              <FieldLabel htmlFor="transfer-reason">Lý do chưa hỏi được</FieldLabel>
              <Textarea
                id="transfer-reason"
                rows={3}
                maxLength={2000}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
              {submitted && missing.reason && <FieldError>Nhập lý do</FieldError>}
            </Field>
          )}
        </>
      )}
      {problem && <ProblemAlert problem={problem} />}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" className="h-11" disabled={busy}>
          Xác nhận chuyển viện
        </Button>
        <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
          Quay lại
        </Button>
      </div>
    </form>
  )
}
