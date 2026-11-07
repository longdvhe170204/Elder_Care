import { useEffect, useState } from 'react'
import {
  getAdmissionApplications,
  getBeds,
  getBedsByBedIdMatchingEntries,
  getResidentsById,
  postWaitingListEntries,
  postWaitingListEntriesByIdCancelWaiting,
  postWaitingListEntriesByIdConfirmStillNeeded,
  postWaitingListEntriesByIdDecline,
  postWaitingListEntriesByIdDeclineBed,
  postWaitingListEntriesByIdMarkPriority,
  postWaitingListEntriesByIdRecordContacted,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { CARE_LEVELS, RESIDENCE_TYPES, SELECT, WAITING_STATUS, labelOf, newKey } from './admissionLabels'
import { Info, ReasonField } from './FormParts'

// Khung chi tiết hồ sơ chờ và các khung lệnh (004 T028; FR-007, FR-009, FR-012 → FR-014, FR-017, BR-M02-01, BR-M02-10).
// Hợp đồng không trả lệnh được phép: nút theo trạng thái hồ sơ, backend kiểm lại.

const REASON_COMMANDS = {
  decline: [
    'Ghi nhận từ chối…',
    'Ghi nhận gia đình không tiếp tục chờ',
    postWaitingListEntriesByIdDecline,
    'Đã ghi nhận từ chối',
  ],
  cancel: ['Hủy chờ…', 'Hủy chờ', postWaitingListEntriesByIdCancelWaiting, 'Đã hủy chờ'],
  declineBed: [
    'Gia đình từ chối giường…',
    'Gia đình từ chối giường này',
    postWaitingListEntriesByIdDeclineBed,
    'Đã ghi nhận từ chối giường',
  ],
}

export function EntryBadges({ entry }) {
  return (
    <span className="flex flex-wrap items-center gap-x-3 type-caption text-muted-foreground">
      {entry.priority && <span className="text-foreground">Ưu tiên</span>}
      {entry.previouslyResided && <span>Từng lưu trú tại viện</span>}
      {entry.notAssessed && <span>Chưa đánh giá</span>}
    </span>
  )
}

export function EntryDetail({ entry, canUpdate, onChanged, Title, Description }) {
  const [mode, setMode] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const waiting = entry.status === 'WAITING'
  const contacted = entry.status === 'CONTACTED'

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
      await call({ path: { id: entry.id }, headers: newKey(), ...(body && { body }) })
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
    if (mode === 'priority') {
      run(
        postWaitingListEntriesByIdMarkPriority,
        { priority: !entry.priority, reason: reason.trim() },
        'Đã lưu dấu Ưu tiên',
      )
      return
    }
    const [, , call, message] = REASON_COMMANDS[mode]
    run(call, { reason: reason.trim() }, message)
  }

  const reasonError = submitted && !reason.trim() ? 'Nhập lý do' : null
  const title =
    mode === 'priority' ? (entry.priority ? 'Gỡ dấu Ưu tiên' : 'Đánh dấu Ưu tiên') : REASON_COMMANDS[mode]?.[1]

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <StatusBadge status={WAITING_STATUS[entry.status] ?? entry.status} />
        <Title className="type-title">{entry.fullName}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {`Đăng ký ngày ${formatDate(entry.applicationDate)}`}
        </Description>
      </div>
      <div className="grid gap-3">
        <Info label="Loại lưu trú">{labelOf(RESIDENCE_TYPES, entry.residenceType)}</Info>
        <Info label="Mức chăm sóc">
          {labelOf(CARE_LEVELS, entry.careLevel)}
          {entry.notAssessed ? ', lấy từ đăng ký, chưa có đánh giá' : ''}
        </Info>
        {entry.priority && <Info label="Lý do Ưu tiên">{entry.priorityReason}</Info>}
        {entry.previouslyResided && (
          <Info label="Lưu ý">Từng lưu trú tại viện; Hành chính cân nhắc, hệ thống không tự đánh dấu.</Info>
        )}
        {entry.heldBedId && <Info label="Giường đang giữ chỗ">{`Mã giường ${entry.heldBedId}`}</Info>}
      </div>
      <ProblemAlert problem={failure} />

      {mode === null && canUpdate && (waiting || contacted) && (
        <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
          {waiting && (
            <>
              <Button
                variant="outline"
                className="h-11"
                disabled={busy}
                onClick={() => run(postWaitingListEntriesByIdConfirmStillNeeded, null, 'Đã xác nhận còn nhu cầu')}
              >
                Xác nhận còn nhu cầu
              </Button>
              <Button variant="outline" className="h-11" onClick={() => open('priority')}>
                {entry.priority ? 'Gỡ dấu Ưu tiên' : 'Đánh dấu Ưu tiên'}
              </Button>
            </>
          )}
          {contacted && (
            <Button variant="outline" className="h-11" onClick={() => open('declineBed')}>
              {REASON_COMMANDS.declineBed[0]}
            </Button>
          )}
          <Button variant="outline" className="h-11" onClick={() => open('cancel')}>
            {REASON_COMMANDS.cancel[0]}
          </Button>
          <Button className="h-11" onClick={() => open('decline')}>
            {REASON_COMMANDS.decline[0]}
          </Button>
        </div>
      )}

      {mode !== null && (
        <div className="flex flex-col gap-3 border-t pt-4">
          <h3 className="type-card-title">{title}</h3>
          {(mode === 'decline' || mode === 'cancel') && (
            <p className="type-body-sm text-muted-foreground">
              Lệnh này kéo theo Hủy tiếp nhận của người cao tuổi trong cùng một lần, cùng lý do.
            </p>
          )}
          <ReasonField id="entry-reason" value={reason} onChange={setReason} error={reasonError} />
          <div className="flex justify-end gap-2">
            <Button variant="ghost" className="h-11" onClick={() => open(null)}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={submitReason}>
              {mode === 'priority' ? 'Lưu dấu Ưu tiên' : title}
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

// Hồ sơ phù hợp với một giường Trống (FR-012, FR-013): không chọn sẵn, bỏ qua thứ tự phải có lý do (FR-014)
export function MatchingSheet({ onClose, onDone }) {
  const [beds, setBeds] = useState(null)
  const [bedId, setBedId] = useState('')
  const [matching, setMatching] = useState(null)
  const [picked, setPicked] = useState(null)
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    getBeds({ query: { status: ['EMPTY'] } })
      .then(({ data }) => setBeds(data.beds))
      .catch((e) => {
        setBeds([])
        setFailure(problemOf(e))
      })
  }, [])

  function pickBed(id) {
    setBedId(id)
    setMatching(null)
    setPicked(null)
    setSubmitted(false)
    if (!id) return
    getBedsByBedIdMatchingEntries({ path: { bedId: Number(id) } })
      .then(({ data }) => setMatching(data))
      .catch((e) => setFailure(problemOf(e)))
  }

  const outOfOrder = matching && picked && matching.entries[0]?.id !== picked
  const reasonError = submitted && outOfOrder && !reason.trim() ? 'Nhập lý do bỏ qua thứ tự' : null

  async function save() {
    setSubmitted(true)
    if (!picked || (outOfOrder && !reason.trim())) return
    setBusy(true)
    setFailure(null)
    try {
      await postWaitingListEntriesByIdRecordContacted({
        path: { id: picked },
        headers: newKey(),
        body: { bedId: Number(bedId), ...(outOfOrder && { outOfOrderReason: reason.trim() }) },
      })
      onDone('Đã ghi nhận đã liên hệ, giường được giữ chỗ')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle>Hồ sơ phù hợp với giường trống</SheetTitle>
        <SheetDescription>
          Danh sách theo Ưu tiên rồi ngày đăng ký; hệ thống không chọn sẵn. Chọn hồ sơ không đứng đầu thì ghi lý do.
        </SheetDescription>
        <FieldGroup className="mt-4">
          <Field>
            <FieldLabel htmlFor="matching-bed">Giường trống</FieldLabel>
            <select id="matching-bed" className={SELECT} value={bedId} onChange={(e) => pickBed(e.target.value)}>
              <option value="">{beds === null ? 'Đang tải…' : 'Chọn giường trống'}</option>
              {(beds ?? []).map((b) => (
                <option key={b.id} value={b.id}>
                  {b.code}
                </option>
              ))}
            </select>
          </Field>
        </FieldGroup>
        {matching && matching.entries.length === 0 && (
          <p className="mt-4 type-body-sm text-muted-foreground">Không có người phù hợp với giường này.</p>
        )}
        {matching && matching.entries.length > 0 && (
          <ul className="mt-4 flex flex-col gap-2" aria-label="Hồ sơ phù hợp">
            {matching.entries.map((e) => (
              <li key={e.id} className="flex items-center justify-between gap-3 rounded-lg border p-3">
                <span className="flex flex-col">
                  <span className="type-body-sm">{e.fullName}</span>
                  <span className="type-caption text-muted-foreground">
                    {`Đăng ký ${formatDate(e.applicationDate)}, ${labelOf(CARE_LEVELS, e.careLevel)}`}
                  </span>
                  <EntryBadges entry={e} />
                </span>
                <Button
                  variant={picked === e.id ? 'default' : 'outline'}
                  className="h-11"
                  aria-pressed={picked === e.id}
                  aria-label={`Chọn ${e.fullName}`}
                  onClick={() => setPicked(e.id)}
                >
                  {picked === e.id ? 'Đã chọn' : 'Chọn'}
                </Button>
              </li>
            ))}
          </ul>
        )}
        {picked && (
          <div className="mt-4 flex flex-col gap-3">
            {outOfOrder && (
              <Field data-invalid={!!reasonError || undefined}>
                <ReasonField
                  id="matching-reason"
                  label="Lý do bỏ qua thứ tự"
                  value={reason}
                  onChange={setReason}
                  error={reasonError}
                />
              </Field>
            )}
            <div className="flex justify-end">
              <Button className="h-11" disabled={busy} onClick={save}>
                Ghi nhận đã liên hệ
              </Button>
            </div>
          </div>
        )}
        <div className="mt-4">
          <ProblemAlert problem={failure} />
        </div>
      </SheetContent>
    </Sheet>
  )
}

// Đưa một lượt đăng ký Đang xử lý vào danh sách chờ (FR-007)
export function AddEntrySheet({ onClose, onDone }) {
  const [applications, setApplications] = useState(null)
  const [names, setNames] = useState({})
  const [applicationId, setApplicationId] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    getAdmissionApplications({ query: { status: ['IN_PROGRESS'] } })
      .then(({ data }) => {
        const open = data.filter((a) => a.status === 'IN_PROGRESS')
        setApplications(open)
        for (const a of open)
          getResidentsById({ path: { id: a.residentId } })
            .then(({ data: r }) => setNames((n) => ({ ...n, [a.residentId]: r.personalInfo?.fullName })))
            .catch(() => {})
      })
      .catch((e) => {
        setApplications([])
        setFailure(problemOf(e))
      })
  }, [])

  async function save() {
    setSubmitted(true)
    if (!applicationId) return
    setBusy(true)
    setFailure(null)
    try {
      await postWaitingListEntries({ headers: newKey(), body: { applicationId: Number(applicationId) } })
      onDone('Đã đưa vào danh sách chờ')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  const error = submitted && !applicationId ? 'Chọn lượt đăng ký' : null
  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetTitle>Đưa vào danh sách chờ</SheetTitle>
        <SheetDescription>
          Chọn lượt đăng ký đang tiếp nhận; mức chăm sóc lấy từ đánh giá được chấp nhận (FR-008).
        </SheetDescription>
        <FieldGroup className="mt-4">
          <Field data-invalid={!!error || undefined}>
            <FieldLabel htmlFor="entry-application">Lượt đăng ký</FieldLabel>
            <select
              id="entry-application"
              className={SELECT}
              value={applicationId}
              aria-invalid={!!error || undefined}
              onChange={(e) => setApplicationId(e.target.value)}
            >
              <option value="">{applications === null ? 'Đang tải…' : 'Chọn lượt đăng ký'}</option>
              {(applications ?? []).map((a) => (
                <option key={a.id} value={a.id}>
                  {`${names[a.residentId] ?? `Người cao tuổi mã ${a.residentId}`}, đăng ký ${formatDate(a.applicationDate)}`}
                </option>
              ))}
            </select>
            {error && <FieldError>{error}</FieldError>}
          </Field>
        </FieldGroup>
        <div className="mt-4">
          <ProblemAlert problem={failure} />
        </div>
        <div className="mt-4 flex justify-end gap-2">
          <Button variant="ghost" className="h-11" onClick={onClose}>
            Đóng
          </Button>
          <Button className="h-11" disabled={busy} onClick={save}>
            Đưa vào danh sách chờ
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
