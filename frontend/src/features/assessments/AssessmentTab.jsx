import { useCallback, useEffect, useState } from 'react'
import {
  ApiError,
  getCatalogsByType,
  getResidentsByIdAssessments,
  getResidentsByIdReassessment,
  getResidentsByIdCareLevel,
  getResidentsByIdCareLevelHistory,
  postResidentsByIdAssessments,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { CorrectionBadge } from '@/common/correction/CorrectionBadge'
import { CorrectionHistorySheet } from '@/common/correction/CorrectionHistorySheet'
import { AssessmentForm } from './AssessmentForm'
import { ReassessmentNotice } from './ReassessmentNotice'
import {
  ASSESSMENT_CORRECTION_FIELDS,
  ASSESSMENT_RECORD_TYPE,
  ASSESSMENT_STATUS_LABELS,
  ASSESSMENT_TYPE_LABELS,
  BASIS_TYPE_LABELS,
  CARE_LEVEL_LABELS,
  REASSESSABLE_STATUSES,
  RESIDENCE_TYPE_LABELS,
} from './assessmentLabels'

// Tab "Đánh giá" trong khung chi tiết hồ sơ (001 T042; US3; FR-024 → FR-038; bố cục duyệt 2026-10-23): mức chăm sóc hiện
// hành, phiếu đang soạn (hai cột: phiếu và thông tin tham khảo), lịch sử đánh giá. Nút theo allowedCommands của hồ sơ:
// CREATE_ASSESSMENT (Bác sĩ, Điều dưỡng), CONFIRM_ASSESSMENT (chỉ Bác sĩ, cũng là người đính chính).

// Loại đánh giá lập được theo trạng thái người cao tuổi (FR-034, FR-035); backend kiểm lại và trả 409 khi sai.
function creatableType(status, hasConfirmedInitial) {
  if (status === 'ADMISSION_IN_PROGRESS') return hasConfirmedInitial ? 'REASSESSMENT' : 'INITIAL'
  return REASSESSABLE_STATUSES.includes(status) ? 'REASSESSMENT' : null
}

function CareLevelCard({ level, careNote, residentId }) {
  const [history, setHistory] = useState(null)
  const [open, setOpen] = useState(false)

  async function toggle() {
    if (!open && history === null) {
      try {
        const { data } = await getResidentsByIdCareLevelHistory({ path: { id: residentId } })
        setHistory(data)
      } catch {
        setHistory([])
      }
    }
    setOpen(!open)
  }

  return (
    <section aria-labelledby="care-level-title" className="flex flex-col gap-2 rounded-lg border p-4">
      <h3 id="care-level-title" className="type-card-title">
        Mức chăm sóc hiện hành
      </h3>
      {level ? (
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <StatusBadge status={CARE_LEVEL_LABELS[level.careLevel]} />
          <span className="type-caption text-muted-foreground tabular-nums">
            từ {formatDate(level.effectiveFrom)}, {BASIS_TYPE_LABELS[level.basisType]}
          </span>
        </p>
      ) : (
        <p className="type-body-sm text-muted-foreground">
          Chưa có mức chăm sóc hiện hành: mức đầu tiên được đặt khi Bác sĩ xác nhận đánh giá đầu vào.
        </p>
      )}
      {careNote && (
        <p className="type-body-sm">
          <span className="type-caption text-muted-foreground">Lưu ý chăm sóc: </span>
          {careNote}
        </p>
      )}
      {level && (
        <Button type="button" variant="ghost" className="h-10 self-start" aria-expanded={open} onClick={toggle}>
          {open ? 'Ẩn lịch sử mức' : 'Lịch sử mức'}
        </Button>
      )}
      {open && history && (
        <ol className="flex flex-col gap-1 type-body-sm">
          {history.map((h) => (
            <li key={`${h.effectiveFrom}-${h.basisId}`} className="tabular-nums">
              {formatDateTime(h.effectiveFrom)}: {CARE_LEVEL_LABELS[h.careLevel]}, {BASIS_TYPE_LABELS[h.basisType]}
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}

function HistoryItem({ item, canCorrect, onCorrect }) {
  return (
    <li className="flex flex-col gap-2 rounded-lg border p-3">
      <div className="flex flex-wrap items-center gap-2">
        <span className="type-label">{ASSESSMENT_TYPE_LABELS[item.type]}</span>
        <StatusBadge status={ASSESSMENT_STATUS_LABELS[item.status]} />
        <CorrectionBadge corrected={item.corrected} />
        {item.careLevel && <span className="type-caption">{CARE_LEVEL_LABELS[item.careLevel]}</span>}
        {item.confirmedAt && (
          <span className="type-caption text-muted-foreground tabular-nums">
            {item.confirmedBy}, {formatDateTime(item.confirmedAt)}
          </span>
        )}
      </div>
      <details>
        <summary className="type-body-sm cursor-pointer">Xem nội dung</summary>
        <dl className="mt-2 flex flex-col gap-2">
          <div className="flex flex-col">
            <dt className="type-caption text-muted-foreground">Nhận xét đánh giá</dt>
            <dd className="type-body-sm whitespace-pre-wrap">{item.comment || 'Chưa ghi'}</dd>
          </div>
          <div className="flex flex-col">
            <dt className="type-caption text-muted-foreground">Lưu ý chăm sóc</dt>
            <dd className="type-body-sm whitespace-pre-wrap">{item.careNote || 'Không có'}</dd>
          </div>
          <div className="flex flex-col">
            <dt className="type-caption text-muted-foreground">Nhu cầu chăm sóc</dt>
            <dd className="type-body-sm">
              {item.careNeeds.length > 0 ? item.careNeeds.map((n) => n.name).join(', ') : 'Không có'}
            </dd>
          </div>
          {item.proposedResidenceType && (
            <div className="flex flex-col">
              <dt className="type-caption text-muted-foreground">Loại hình lưu trú đề xuất (tham khảo)</dt>
              <dd className="type-body-sm">{RESIDENCE_TYPE_LABELS[item.proposedResidenceType]}</dd>
            </div>
          )}
          <div className="flex flex-col">
            <dt className="type-caption text-muted-foreground">Người lập</dt>
            <dd className="type-body-sm">{item.createdBy}</dd>
          </div>
        </dl>
      </details>
      {item.status === 'CONFIRMED' && canCorrect && (
        <Button type="button" variant="outline" className="h-10 self-start" onClick={() => onCorrect(item.id)}>
          Đính chính
        </Button>
      )}
    </li>
  )
}

export function AssessmentTab({ residentId, residentStatus, allowedCommands = [], onChanged }) {
  const [state, setState] = useState('loading')
  const [level, setLevel] = useState(null)
  const [reassessment, setReassessment] = useState(null)
  const [list, setList] = useState([])
  const [careNeeds, setCareNeeds] = useState([])
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)
  const [correctingId, setCorrectingId] = useState(null)

  const canCreate = allowedCommands.includes('CREATE_ASSESSMENT')
  const canConfirm = allowedCommands.includes('CONFIRM_ASSESSMENT')

  const load = useCallback(async () => {
    const [levelRes, listRes, needsRes, reassessmentRes] = await Promise.allSettled([
      getResidentsByIdCareLevel({ path: { id: residentId } }),
      getResidentsByIdAssessments({ path: { id: residentId } }),
      getCatalogsByType({ path: { type: 'care-needs' } }),
      getResidentsByIdReassessment({ path: { id: residentId } }),
    ])
    if (listRes.status === 'rejected') {
      if (listRes.reason instanceof ApiError && listRes.reason.status === 403) {
        setState('forbidden')
      } else {
        setFailure(problemOf(listRes.reason))
        setState('error')
      }
      return
    }
    if (levelRes.status === 'fulfilled') setLevel(levelRes.value.data)
    else if (levelRes.reason instanceof ApiError && levelRes.reason.status === 404) setLevel(null)
    else {
      setFailure(problemOf(levelRes.reason))
      setState('error')
      return
    }
    setList(listRes.value.data)
    // Báo đánh giá lại là phần phụ của tab: lỗi tải thì không hiện dải, không chặn tab
    setReassessment(reassessmentRes.status === 'fulfilled' ? reassessmentRes.value.data : null)
    setCareNeeds(needsRes.status === 'fulfilled' ? needsRes.value.data.filter((n) => n.status !== 'INACTIVE') : [])
    setFailure(null)
    setState('ready')
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp đánh giá khi mở tab
    load()
  }, [load])

  const draft = list.find((a) => a.status === 'DRAFT')
  const latestConfirmed = list.find((a) => a.status === 'CONFIRMED')
  const hasConfirmedInitial = list.some((a) => a.status === 'CONFIRMED' && a.type === 'INITIAL')
  const createType = creatableType(residentStatus, hasConfirmedInitial)
  const history = list.filter((a) => a.status !== 'DRAFT')

  async function create() {
    setFailure(null)
    setResult(null)
    setBusy(true)
    try {
      await postResidentsByIdAssessments({
        path: { id: residentId },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { type: createType },
      })
      await load()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  async function confirmed(data) {
    setResult(data.effects)
    await load()
    onChanged?.()
  }

  if (state === 'loading') {
    return (
      <div aria-busy="true" className="flex flex-col gap-2">
        <span className="sr-only">Đang tải đánh giá</span>
        <Skeleton className="h-16" />
        <Skeleton className="h-32" />
      </div>
    )
  }
  if (state === 'forbidden') {
    return (
      <Empty>
        <EmptyHeader>
          <EmptyTitle>Không có quyền xem đánh giá</EmptyTitle>
          <EmptyDescription>Vai trò của bạn không được xem đánh giá và mức chăm sóc của người này.</EmptyDescription>
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

  return (
    <div className="flex flex-col gap-5">
      <ReassessmentNotice reassessment={reassessment} />
      <CareLevelCard level={level} careNote={latestConfirmed?.careNote} residentId={residentId} />

      {result && (
        <p role="status" className="type-body-sm rounded-lg border bg-muted/30 p-3">
          Đã xác nhận đánh giá. {result.currentCareLevelChanged ? 'Mức chăm sóc hiện hành đã được cập nhật. ' : ''}
          {result.administratorNotified ? 'Đã báo Hành chính lập yêu cầu đổi mức. ' : ''}
          {result.supersededRequestIds?.length > 0
            ? `${result.supersededRequestIds.length} yêu cầu đổi mức chưa kết thúc đã chuyển sang Được thay thế. `
            : ''}
          {result.carePlanReviewNotified ? 'Điều dưỡng phụ trách được báo xem xét kế hoạch chăm sóc.' : ''}
        </p>
      )}

      {draft ? (
        <AssessmentForm
          key={draft.id}
          assessmentId={draft.id}
          residentStatus={residentStatus}
          currentLevel={level?.careLevel ?? null}
          careNeedOptions={careNeeds}
          canConfirm={canConfirm}
          onChanged={load}
          onConfirmed={confirmed}
        />
      ) : (
        <section aria-labelledby="assessment-new-title" className="flex flex-col gap-2">
          <h3 id="assessment-new-title" className="type-card-title">
            Lập đánh giá
          </h3>
          <ProblemAlert problem={failure} />
          {createType === null ? (
            <p className="type-body-sm text-muted-foreground">
              Trạng thái hiện tại của người cao tuổi không cho phép lập đánh giá.
            </p>
          ) : canCreate ? (
            <Button type="button" className="h-11 self-start" disabled={busy} onClick={create}>
              {createType === 'INITIAL' ? 'Lập đánh giá đầu vào' : 'Lập đánh giá lại'}
            </Button>
          ) : (
            <p className="type-body-sm text-muted-foreground">Bạn không có quyền lập đánh giá.</p>
          )}
        </section>
      )}

      <section aria-labelledby="assessment-history-title" className="flex flex-col gap-2">
        <h3 id="assessment-history-title" className="type-card-title">
          Lịch sử đánh giá
        </h3>
        {history.length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa có lần đánh giá nào đã xác nhận hoặc đã hủy.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {history.map((a) => (
              <HistoryItem key={a.id} item={a} canCorrect={canConfirm} onCorrect={setCorrectingId} />
            ))}
          </ul>
        )}
      </section>

      {correctingId !== null && (
        <CorrectionHistorySheet
          recordType={ASSESSMENT_RECORD_TYPE}
          recordId={correctingId}
          fields={ASSESSMENT_CORRECTION_FIELDS}
          open
          onOpenChange={(open) => !open && setCorrectingId(null)}
          canCorrect={canConfirm}
          onChanged={() => {
            load()
            onChanged?.()
          }}
        />
      )}
    </div>
  )
}
