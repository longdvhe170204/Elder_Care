import { useCallback, useEffect, useState } from 'react'
import {
  ApiError,
  getResidentsByIdConditionsByCommand,
  getResidentsByIdStatusHistory,
  postResidentsByIdCancelAdmission,
  postResidentsByIdCompleteAdmission,
  postResidentsByIdEndResidency,
  postResidentsByIdGrantTemporaryAbsence,
  postResidentsByIdRecordDeath,
  postResidentsByIdRecordReturn,
  postResidentsByIdTransferOut,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { STATUS_LABELS } from '../residents/residentLabels'
import {
  BASIS_KINDS,
  COMMAND_EFFECTS,
  COMMAND_LABELS,
  COMMAND_ORDER,
  FINAL_STATUSES,
  HISTORY_COMMAND_LABELS,
} from './statusLabels'

// Tab "Trạng thái" trong khung chi tiết hồ sơ (001 T052; US4; FR-044 → FR-047b; bố cục do B giao cho Claude quyết định
// 2026-10-23): thẻ trạng thái hiện tại kèm nút lệnh theo allowedCommands, form lệnh mở ngay dưới thẻ (xem trước điều kiện
// chưa thỏa, lý do bắt buộc, ô riêng của Chuyển viện và Ghi nhận qua đời), lịch sử trạng thái. Giao diện không tự suy ra
// quyền hay điều kiện; mọi kiểm tra thật do backend (DESIGN.md 7.4).

const SENDERS = {
  COMPLETE_ADMISSION: postResidentsByIdCompleteAdmission,
  CANCEL_ADMISSION: postResidentsByIdCancelAdmission,
  GRANT_TEMPORARY_ABSENCE: postResidentsByIdGrantTemporaryAbsence,
  RECORD_RETURN: postResidentsByIdRecordReturn,
  TRANSFER_OUT: postResidentsByIdTransferOut,
  END_RESIDENCY: postResidentsByIdEndResidency,
  RECORD_DEATH: postResidentsByIdRecordDeath,
}

// Chữ thường của tên lệnh, dùng trong nhãn nút xác nhận: "Xác nhận cho tạm vắng"
const lower = (label) => label.charAt(0).toLowerCase() + label.slice(1)

function ConditionList({ conditions }) {
  return (
    <ul className="flex flex-col gap-1">
      {conditions.map((c) => (
        <li key={c.code} className="flex flex-wrap items-center gap-x-2">
          <span>{c.description}</span>
          {c.basis && <span className="type-caption text-muted-foreground">({c.basis})</span>}
          {c.waivable && <StatusBadge status="Miễn được" />}
        </li>
      ))}
    </ul>
  )
}

function CommandForm({ residentId, command, onCancel, onDone }) {
  const label = COMMAND_LABELS[command]
  const [conditions, setConditions] = useState(null)
  const [reason, setReason] = useState('')
  const [basisKind, setBasisKind] = useState('')
  const [basisText, setBasisText] = useState('')
  const [evidence, setEvidence] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [key] = useState(() => crypto.randomUUID())

  const loadConditions = useCallback(async () => {
    try {
      const { data } = await getResidentsByIdConditionsByCommand({ path: { id: residentId, command } })
      setConditions(data)
    } catch (error) {
      setConditions([])
      setFailure(problemOf(error))
    }
  }, [residentId, command])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- xem trước điều kiện khi mở lệnh
    loadConditions()
  }, [loadConditions])

  function detailsOf() {
    if (command === 'TRANSFER_OUT') {
      const kind = BASIS_KINDS.find((k) => k.value === basisKind)
      const value = basisText.trim()
      return { basis: { [basisKind]: kind.numeric ? Number(value) : value } }
    }
    if (command === 'RECORD_DEATH' && evidence.trim()) return { evidenceFileId: Number(evidence) }
    return {}
  }

  async function submit(event) {
    event.preventDefault()
    const next = {}
    if (!reason.trim()) next.reason = 'Nhập lý do của lệnh'
    if (command === 'TRANSFER_OUT') {
      const kind = BASIS_KINDS.find((k) => k.value === basisKind)
      const value = basisText.trim()
      if (!kind || !value || (kind.numeric && !Number.isInteger(Number(value)))) {
        next.basis = 'Chọn và nhập căn cứ chuyển viện'
      }
    }
    if (command === 'RECORD_DEATH' && evidence.trim() && !Number.isInteger(Number(evidence))) {
      next.evidence = 'Mã tệp là số'
    }
    setErrors(next)
    if (Object.keys(next).length > 0) return
    setFailure(null)
    setBusy(true)
    try {
      const { data } = await SENDERS[command]({
        path: { id: residentId },
        headers: { 'Idempotency-Key': key },
        body: { reason: reason.trim(), details: detailsOf() },
      })
      onDone(data)
    } catch (error) {
      const problem = problemOf(error)
      if (error instanceof ApiError && error.status === 422) {
        if (problem.errorCode === 'REASON_REQUIRED') setErrors({ reason: problem.detail || 'Nhập lý do của lệnh' })
        else if (problem.errorCode === 'BASIS_REQUIRED') setErrors({ basis: 'Chọn và nhập căn cứ chuyển viện' })
        else if (problem.errorCode === 'EVIDENCE_REQUIRED') {
          setErrors({ evidence: 'Phải kèm giấy tờ bằng chứng khi Hành chính ghi nhận qua đời ngoài viện' })
        } else setFailure(problem)
      } else {
        setFailure(problem)
        if (problem.status === 409) loadConditions()
      }
    } finally {
      setBusy(false)
    }
  }

  const unmet = conditions ?? []
  const blocked = conditions === null || unmet.length > 0

  return (
    <form
      onSubmit={submit}
      noValidate
      aria-label={label}
      className="flex flex-col gap-4 rounded-lg border bg-muted/30 p-4"
    >
      <h3 className="type-card-title">{label}</h3>
      <p className="type-body-sm text-muted-foreground">{COMMAND_EFFECTS[command]}</p>

      {conditions === null ? (
        <Skeleton className="h-10" />
      ) : unmet.length > 0 ? (
        <div role="alert" className="flex flex-col gap-2 rounded-lg border border-attention-foreground/30 p-3">
          <p className="type-label">Chưa đủ điều kiện để thực hiện</p>
          <ConditionList conditions={unmet} />
        </div>
      ) : (
        <p className="type-body-sm text-muted-foreground">Đủ điều kiện để thực hiện.</p>
      )}

      <Field data-invalid={!!errors.reason || undefined}>
        <FieldLabel htmlFor={`status-reason-${command}`}>Lý do của lệnh (bắt buộc)</FieldLabel>
        <Textarea
          id={`status-reason-${command}`}
          rows={3}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={!!errors.reason || undefined}
        />
        {errors.reason && <FieldError>{errors.reason}</FieldError>}
      </Field>

      {command === 'TRANSFER_OUT' && (
        <Field data-invalid={!!errors.basis || undefined}>
          <span className="type-label" id="basis-kind-label">
            Căn cứ chuyển viện (bắt buộc)
          </span>
          <div role="radiogroup" aria-labelledby="basis-kind-label" className="flex flex-wrap gap-2">
            {BASIS_KINDS.map((k) => (
              <Button
                key={k.value}
                type="button"
                role="radio"
                aria-checked={basisKind === k.value}
                variant={basisKind === k.value ? 'default' : 'outline'}
                className="h-10"
                onClick={() => setBasisKind(k.value)}
              >
                {k.label}
              </Button>
            ))}
          </div>
          <FieldLabel htmlFor="status-basis-text">Nội dung căn cứ</FieldLabel>
          <Input
            id="status-basis-text"
            value={basisText}
            onChange={(e) => setBasisText(e.target.value)}
            placeholder={BASIS_KINDS.find((k) => k.value === basisKind)?.hint}
            aria-invalid={!!errors.basis || undefined}
          />
          {errors.basis && <FieldError>{errors.basis}</FieldError>}
        </Field>
      )}

      {command === 'RECORD_DEATH' && (
        <Field data-invalid={!!errors.evidence || undefined}>
          <FieldLabel htmlFor="status-evidence">Mã tệp giấy tờ bằng chứng</FieldLabel>
          <Input
            id="status-evidence"
            inputMode="numeric"
            value={evidence}
            onChange={(e) => setEvidence(e.target.value)}
            aria-invalid={!!errors.evidence || undefined}
          />
          {errors.evidence ? (
            <FieldError>{errors.evidence}</FieldError>
          ) : (
            <FieldDescription>
              Bác sĩ không cần khi xác nhận tại viện. Hành chính ghi nhận ngoài viện phải kèm giấy báo tử hoặc giấy chứng
              tử. Tải tệp lên sẽ có khi tính năng tệp đính kèm kết nối.
            </FieldDescription>
          )}
        </Field>
      )}

      <ProblemAlert problem={failure} />

      <div className="flex flex-wrap gap-3">
        <Button type="submit" className="h-11" disabled={busy || blocked}>
          {`Xác nhận ${lower(label)}`}
        </Button>
        <Button type="button" variant="outline" className="h-11" onClick={onCancel}>
          Thôi
        </Button>
      </div>
    </form>
  )
}

function HistoryItem({ item }) {
  const actor = item.bySystem
    ? `Hệ thống (điểm danh), người điểm danh ${item.performedBy}`
    : `${item.performedBy}, ${HISTORY_COMMAND_LABELS[item.command] ?? item.command}`
  return (
    <li className="flex flex-col gap-1 rounded-lg border p-3">
      <div className="flex flex-wrap items-center gap-2">
        {item.from && (
          <>
            <StatusBadge status={STATUS_LABELS[item.from]} />
            <span aria-hidden="true">→</span>
          </>
        )}
        <StatusBadge status={STATUS_LABELS[item.to]} />
        {item.conditionWaiver && <StatusBadge status="Miễn điều kiện" />}
        <span className="type-caption text-muted-foreground tabular-nums">{formatDateTime(item.at)}</span>
      </div>
      <p className="type-body-sm">{actor}</p>
      {item.reason && <p className="type-body-sm whitespace-pre-wrap">{item.reason}</p>}
      {item.conditionWaiver && (
        <p className="type-caption text-muted-foreground">
          Theo yêu cầu ngoại lệ đã duyệt số {item.conditionWaiver.approvalRequestId}
        </p>
      )}
    </li>
  )
}

export function StatusTab({ residentId, status, allowedCommands = [], onChanged }) {
  const [state, setState] = useState('loading')
  const [history, setHistory] = useState([])
  const [failure, setFailure] = useState(null)
  const [command, setCommand] = useState(null)
  const [done, setDone] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByIdStatusHistory({ path: { id: residentId } })
      setHistory(data)
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
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp lịch sử khi mở tab
    load()
  }, [load])

  async function finished(data) {
    setCommand(null)
    setDone(data)
    await load()
    onChanged?.()
  }

  if (state === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2">
        <span className="sr-only">Đang tải trạng thái</span>
        <Skeleton className="h-16" />
        <Skeleton className="h-24" />
      </div>
    )
  }
  if (state === 'forbidden') {
    return (
      <Empty>
        <EmptyHeader>
          <EmptyTitle>Không có quyền xem lịch sử trạng thái</EmptyTitle>
          <EmptyDescription>Vai trò của bạn không được xem lịch sử trạng thái của người này.</EmptyDescription>
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

  const isFinal = FINAL_STATUSES.includes(status)
  const commands = COMMAND_ORDER.filter((c) => allowedCommands.includes(c))
  const newest = [...history].reverse()

  return (
    <div className="flex flex-col gap-5">
      <section aria-labelledby="status-now-title" className="flex flex-col gap-3 rounded-lg border p-4">
        <h3 id="status-now-title" className="type-card-title">
          Trạng thái hiện tại
        </h3>
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <StatusBadge status={STATUS_LABELS[status] ?? status} />
          {history.length > 0 && (
            <span className="type-caption text-muted-foreground tabular-nums">
              từ {formatDateTime(history[history.length - 1].at)}
            </span>
          )}
        </p>
        {isFinal ? (
          <p className="type-body-sm text-muted-foreground">
            Hồ sơ ở trạng thái cuối, chỉ xem. Không còn lệnh nào thực hiện được.
          </p>
        ) : commands.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {commands.map((c, index) => (
              <Button
                key={c}
                type="button"
                variant={index === 0 ? 'default' : 'outline'}
                className="h-11"
                aria-pressed={command === c}
                onClick={() => {
                  setDone(null)
                  setCommand(c)
                }}
              >
                {COMMAND_LABELS[c]}
              </Button>
            ))}
          </div>
        ) : (
          <p className="type-body-sm text-muted-foreground">Bạn không có lệnh nào dùng được ở trạng thái này.</p>
        )}
      </section>

      {done && (
        <p role="status" className="type-body-sm rounded-lg border bg-muted/30 p-3">
          Đã chuyển sang {STATUS_LABELS[done.status] ?? done.status}.
          {done.warnings?.length > 0 && ` Lưu ý: ${done.warnings.map((w) => w.description).join('; ')}`}
        </p>
      )}

      {command && !isFinal && (
        <CommandForm
          key={command}
          residentId={residentId}
          command={command}
          onCancel={() => setCommand(null)}
          onDone={finished}
        />
      )}

      <section aria-labelledby="status-history-title" className="flex flex-col gap-2">
        <h3 id="status-history-title" className="type-card-title">
          Lịch sử trạng thái
        </h3>
        <ul aria-label="Lịch sử trạng thái" className="flex flex-col gap-2">
          {newest.map((item) => (
            <HistoryItem key={`${item.at}-${item.command}`} item={item} />
          ))}
        </ul>
      </section>
    </div>
  )
}
