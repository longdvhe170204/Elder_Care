import { useCallback, useEffect, useState } from 'react'
import { getResidentsByResidentIdResidenceRegistrations, postResidentsByResidentIdResidenceRegistrations } from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { CorrectionHistorySheet } from '@/common/correction/CorrectionHistorySheet'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { RegistrationForm } from './RegistrationForms'
import {
  CHANNEL_LABELS,
  COMMANDS,
  CORRECTION_FIELDS,
  CURRENT_STATUSES,
  FUNCTION_CODE,
  IN_PROGRESS_STATUSES,
  RECORD_TYPE,
  RESULT_LABELS,
  RESULT_TONES,
  STATUS_LABELS,
  STATUS_TONES,
  TASK_LABELS,
  TASK_TONES,
  TYPE_LABELS,
} from './registrationLabels'

// Khung chi tiết khai báo theo người cao tuổi (018 T008, T010, T012, T014; DESIGN.md bố cục K3, K4, K7). Nút lệnh chỉ hiện
// theo allowedCommands của khai báo (backend) và quyền sửa; khai báo cũ thu gọn ở cuối.

const COMMAND_BUTTONS = [
  { command: COMMANDS.CONFIRM_LOCAL, form: 'confirm-local', label: 'Xác nhận thường trú cùng xã/phường' },
  { command: COMMANDS.SUBMITTED, form: 'submitted', label: 'Ghi đã nộp' },
  { command: COMMANDS.ACCEPTED, form: 'result', label: 'Ghi kết quả' },
  { command: COMMANDS.REJECTED, form: 'result', label: 'Ghi kết quả' },
  { command: COMMANDS.DEREGISTERED, form: 'deregistered', label: 'Ghi đã khai báo xóa' },
]

function Submissions({ submissions }) {
  if (!submissions?.length) return null
  return (
    <section aria-label="Lịch sử lần nộp" className="flex flex-col gap-2">
      <h4 className="type-caption text-muted-foreground">Lịch sử lần nộp</h4>
      <ol className="flex flex-col gap-3">
        {submissions.map((s) => (
          <li key={s.id} className="flex flex-col gap-1 border-l-2 pl-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="type-label tabular-nums">{formatDate(s.submittedDate)}</span>
              <Badge variant={RESULT_TONES[s.result] ?? 'neutral'}>{RESULT_LABELS[s.result] ?? s.result}</Badge>
            </div>
            <span className="type-body-sm">{CHANNEL_LABELS[s.channel] ?? s.channel}</span>
            <span className="type-body-sm">{s.receivingAuthority}</span>
            {s.dossierCode && <span className="type-body-sm">Mã hồ sơ {s.dossierCode}</span>}
            {s.rejectionReason && <span className="type-body-sm">Lý do: {s.rejectionReason}</span>}
            {s.recordedAt && (
              <span className="type-caption text-muted-foreground tabular-nums">
                Ghi lúc {formatDateTime(s.recordedAt)}
              </span>
            )}
          </li>
        ))}
      </ol>
    </section>
  )
}

function RegistrationCard({ registration, canUpdate, openForm, onOpenForm, onCloseForm, onDone, onCorrect }) {
  const allowed = canUpdate ? (registration.allowedCommands ?? []) : []
  // Ghi chấp nhận và từ chối dùng chung một form "Ghi kết quả"
  const buttons = []
  for (const b of COMMAND_BUTTONS) {
    if (allowed.includes(b.command) && !buttons.some((x) => x.form === b.form)) buttons.push(b)
  }
  const formOpen = openForm?.id === registration.id ? openForm.form : null
  return (
    <article aria-label={`Khai báo ${registration.id}`} className="flex flex-col gap-3 rounded-lg border bg-card p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="type-label">{TYPE_LABELS[registration.type] ?? registration.type}</h3>
        <Badge variant={STATUS_TONES[registration.status] ?? 'neutral'}>
          {STATUS_LABELS[registration.status] ?? registration.status}
        </Badge>
      </div>
      {registration.renewal && <p className="type-body-sm">Khai báo gia hạn</p>}
      <dl className="type-body-sm flex flex-col gap-1">
        {registration.dueAt && (
          <div className="flex gap-2">
            <dt className="text-muted-foreground">Hạn khai báo</dt>
            <dd className="tabular-nums">{formatDate(registration.dueAt)}</dd>
          </div>
        )}
        {registration.expiryDate && (
          <div className="flex gap-2">
            <dt className="text-muted-foreground">Hết hạn ngày</dt>
            <dd className="tabular-nums">{formatDate(registration.expiryDate)}</dd>
          </div>
        )}
        <div className="flex gap-2">
          <dt className="text-muted-foreground">Thường trú cùng xã/phường</dt>
          <dd>{registration.localPermanentResident ? 'Có' : 'Không'}</dd>
        </div>
        {registration.basisNote && (
          <div className="flex gap-2">
            <dt className="text-muted-foreground">Căn cứ</dt>
            <dd>{registration.basisNote}</dd>
          </div>
        )}
      </dl>
      <Submissions submissions={registration.submissions} />
      {!formOpen && (buttons.length > 0 || canUpdate) && (
        <div className="flex flex-wrap gap-2">
          {buttons.map((b) => (
            <Button
              key={b.form}
              variant="outline"
              className="h-11"
              onClick={() => onOpenForm({ id: registration.id, form: b.form })}
            >
              {b.label}
            </Button>
          ))}
          {canUpdate && (
            <Button variant="ghost" className="h-11" onClick={() => onCorrect(registration.id)}>
              Đính chính
            </Button>
          )}
        </div>
      )}
      {formOpen && (
        <RegistrationForm form={formOpen} registration={registration} onDone={onDone} onCancel={onCloseForm} />
      )}
    </article>
  )
}

export function RegistrationDetail({ residentId, fullName, task, Title, Description, onChanged }) {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [openForm, setOpenForm] = useState(null)
  const [creating, setCreating] = useState(false)
  const [createFailure, setCreateFailure] = useState(null)
  const [notice, setNotice] = useState('')
  const [correctingId, setCorrectingId] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByResidentIdResidenceRegistrations({ path: { residentId } })
      setState({ status: 'ready', items: data.toSorted((a, b) => b.id - a.id) })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khai báo khi mở khung
    load()
  }, [load])

  const canUpdate = can(FUNCTION_CODE, 'UPDATE')
  const current = state.items.filter((r) => CURRENT_STATUSES.includes(r.status))
  const old = state.items.filter((r) => !CURRENT_STATUSES.includes(r.status))
  const hasInProgress = state.items.some((r) => IN_PROGRESS_STATUSES.includes(r.status))
  // Khai báo gia hạn chỉ lập được khi còn Đăng ký tạm trú Đã xác nhận (FR-005); Hết hạn thì lập khai báo thường
  const hasTemporaryConfirmed = state.items.some(
    (r) => r.type === 'TEMPORARY_RESIDENCE_REGISTRATION' && r.status === 'CONFIRMED',
  )

  async function create(renewal) {
    setCreating(true)
    setCreateFailure(null)
    try {
      await postResidentsByResidentIdResidenceRegistrations({
        path: { residentId },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { renewal },
      })
      setNotice(renewal ? 'Đã lập khai báo gia hạn.' : 'Đã lập khai báo.')
      await load()
      onChanged?.()
    } catch (error) {
      setCreateFailure(problemOf(error))
    } finally {
      setCreating(false)
    }
  }

  function done(message) {
    setOpenForm(null)
    setNotice(message)
    load()
    onChanged?.()
  }

  const card = (registration) => (
    <RegistrationCard
      key={registration.id}
      registration={registration}
      canUpdate={canUpdate}
      openForm={openForm}
      onOpenForm={setOpenForm}
      onCloseForm={() => setOpenForm(null)}
      onDone={done}
      onCorrect={setCorrectingId}
    />
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <Title className="type-title">{fullName}</Title>
        <Description className="text-muted-foreground">Khai báo tạm trú, lưu trú</Description>
      </div>
      {task && (
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant={TASK_TONES[task.taskType] ?? 'neutral'}>{TASK_LABELS[task.taskType]}</Badge>
          {task.dueAt && <span className="type-body-sm tabular-nums">Hạn {formatDate(task.dueAt)}</span>}
        </div>
      )}
      <div role="status" className="type-body-sm text-success-foreground">
        {notice}
      </div>
      {state.status === 'loading' && <Skeleton className="h-32 w-full" />}
      {state.status === 'error' && (
        <div className="flex flex-col gap-2">
          <ProblemAlert problem={state.problem} />
          <Button variant="outline" className="h-11 self-start" onClick={load}>
            Thử lại
          </Button>
        </div>
      )}
      {state.status === 'ready' && (
        <>
          {current.length === 0 && <p className="text-muted-foreground">Người này chưa có khai báo đang xử lý.</p>}
          {current.map(card)}
          {can(FUNCTION_CODE, 'CREATE') && !hasInProgress && (
            <div className="flex flex-wrap gap-2">
              <Button className="h-11" disabled={creating} onClick={() => create(false)}>
                Lập khai báo
              </Button>
              {hasTemporaryConfirmed && (
                <Button variant="outline" className="h-11" disabled={creating} onClick={() => create(true)}>
                  Lập khai báo gia hạn
                </Button>
              )}
            </div>
          )}
          <ProblemAlert problem={createFailure} />
          {old.length > 0 && (
            <Collapsible>
              <CollapsibleTrigger className="type-label min-h-11 rounded-full px-3 hover:bg-accent">
                Khai báo cũ ({old.length})
              </CollapsibleTrigger>
              <CollapsibleContent className="flex flex-col gap-3 pt-2">{old.map(card)}</CollapsibleContent>
            </Collapsible>
          )}
        </>
      )}
      {correctingId !== null && (
        <CorrectionHistorySheet
          recordType={RECORD_TYPE}
          recordId={correctingId}
          fields={CORRECTION_FIELDS}
          open
          onOpenChange={(open) => !open && setCorrectingId(null)}
          canCorrect={canUpdate}
          prefillCurrent
          onChanged={() => {
            load()
            onChanged?.()
          }}
        />
      )}
    </div>
  )
}
