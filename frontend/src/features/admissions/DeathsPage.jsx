import { useCallback, useEffect, useState } from 'react'
import {
  getResidentsByResidentIdDeathRecord,
  getResidentsByResidentIdPostDeathTasks,
  postDeathRecordsByIdCorrect,
  postResidentsByResidentIdPostDeathTasksRequestSettlementException,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { POST_DEATH_ITEMS, POST_DEATH_STATUS, newKey } from './admissionLabels'
import { Info, ReasonField, ResidentSelect } from './FormParts'

// Qua đời (004 T065, T109; US8, FR-068 → FR-072, FR-071a, BR-M02-21, Q-387): bản ghi qua đời (nguyên nhân bị ẩn với Hành chính,
// Kế toán), đính chính, danh sách việc bắt buộc sau qua đời và ngoại lệ cho mục quyết toán số dư, tiền cọc. Lệnh Ghi nhận
// qua đời là lệnh của 001 ở tab Trạng thái của hồ sơ.

function CorrectForm({ record, onDone, onCancel }) {
  const causeHidden = record.hiddenFields?.includes('cause')
  const [cause, setCause] = useState('')
  const [place, setPlace] = useState('')
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  async function save() {
    setSubmitted(true)
    const corrected = { ...(cause.trim() && { cause: cause.trim() }), ...(place.trim() && { place: place.trim() }) }
    if (!reason.trim() || Object.keys(corrected).length === 0) return
    setBusy(true)
    setFailure(null)
    try {
      await postDeathRecordsByIdCorrect({
        path: { id: record.id },
        headers: newKey(),
        body: { type: 'EDIT_CONTENT', correctedContent: corrected, reason: reason.trim() },
      })
      onDone('Đã đính chính bản ghi qua đời')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <h3 className="type-card-title">Đính chính bản ghi qua đời</h3>
      <FieldGroup>
        {!causeHidden && (
          <Field>
            <FieldLabel htmlFor="death-cause">Nguyên nhân đúng</FieldLabel>
            <Input id="death-cause" className="h-11" value={cause} onChange={(e) => setCause(e.target.value)} />
          </Field>
        )}
        <Field>
          <FieldLabel htmlFor="death-place">Nơi xảy ra đúng</FieldLabel>
          <Input id="death-place" className="h-11" value={place} onChange={(e) => setPlace(e.target.value)} />
        </Field>
        <ReasonField
          id="death-reason"
          value={reason}
          onChange={setReason}
          error={submitted && !reason.trim() ? 'Nhập lý do' : null}
        />
      </FieldGroup>
      {submitted && !cause.trim() && !place.trim() && (
        <p className="type-body-sm text-destructive">Nhập nội dung cần sửa</p>
      )}
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onCancel}>
          Đóng
        </Button>
        <Button className="h-11" disabled={busy} onClick={save}>
          Lưu đính chính
        </Button>
      </div>
    </div>
  )
}

function SettlementException({ residentId, onDone, onCancel }) {
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  async function save() {
    setSubmitted(true)
    if (!reason.trim()) return
    setBusy(true)
    setFailure(null)
    try {
      await postResidentsByResidentIdPostDeathTasksRequestSettlementException({
        path: { residentId },
        headers: newKey(),
        body: { reason: reason.trim() },
      })
      onDone('Đã gửi yêu cầu ngoại lệ mục quyết toán, chờ Quản lý viện duyệt')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-3 border-t pt-4">
      <p className="type-body-sm text-muted-foreground">
        Chỉ thay ba ý về tiền; Kế toán vẫn phải xác nhận quyết toán. Mục khác không có ngoại lệ.
      </p>
      <ReasonField
        id="settlement-reason"
        value={reason}
        onChange={setReason}
        error={submitted && !reason.trim() ? 'Nhập lý do' : null}
      />
      <ProblemAlert problem={failure} />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" className="h-11" onClick={onCancel}>
          Đóng
        </Button>
        <Button className="h-11" disabled={busy} onClick={save}>
          Gửi yêu cầu ngoại lệ
        </Button>
      </div>
    </div>
  )
}

export function DeathsPage() {
  const { can } = usePermissions()
  const [residentId, setResidentId] = useState('')
  const [record, setRecord] = useState(null)
  const [tasks, setTasks] = useState(null)
  const [missing, setMissing] = useState(false)
  const [failure, setFailure] = useState(null)
  const [mode, setMode] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(() => {
    if (!residentId) return
    const path = { residentId: Number(residentId) }
    getResidentsByResidentIdDeathRecord({ path })
      .then(({ data }) => setRecord(data))
      .catch((e) => (e.status === 404 ? setMissing(true) : setFailure(problemOf(e))))
    getResidentsByResidentIdPostDeathTasks({ path })
      .then(({ data }) => setTasks(data))
      .catch(() => {})
  }, [residentId])

  useEffect(() => {
    load()
  }, [load])

  if (failure?.status === 403) return <PermissionDeniedState label="Qua đời" />

  const settlement = tasks?.items.find((i) => i.code === 'SETTLEMENT')
  const causeHidden = record?.hiddenFields?.includes('cause')

  function changed(message) {
    setMode(null)
    setAnnouncement(message)
    load()
  }

  return (
    <div className="mx-auto flex max-w-[1600px] flex-col gap-5">
      <PageHeader title="Qua đời" subtitle="Bản ghi qua đời và các việc bắt buộc sau qua đời của người cao tuổi." />
      <Card className="gap-3 p-4">
        <ResidentSelect
          id="death-resident"
          statuses={['DECEASED']}
          value={residentId}
          onChange={(value) => {
            setResidentId(value)
            setRecord(null)
            setTasks(null)
            setMissing(false)
            setFailure(null)
          }}
          onFailure={(e) => setFailure(problemOf(e))}
        />
      </Card>
      <p role="status" className="empty:hidden type-body-sm">
        {announcement}
      </p>
      <ProblemAlert problem={failure} />
      {!residentId && <p className="type-body-sm text-muted-foreground">Chọn người cao tuổi để xem bản ghi qua đời.</p>}
      {missing && <p className="type-body-sm text-muted-foreground">Người cao tuổi này chưa có bản ghi qua đời.</p>}

      {record && (
        <Card className="gap-4 p-5">
          <h2 className="type-card-title">Bản ghi qua đời</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            <Info label="Thời điểm">{formatDateTime(record.deathAt)}</Info>
            <Info label="Nơi xảy ra">{record.place}</Info>
            <Info label="Người phát hiện">{record.discoveredBy}</Info>
            <Info label="Người xác nhận">{record.confirmedBy}</Info>
            {causeHidden ? (
              <Info label="Nguyên nhân">Nguyên nhân được ẩn theo quyền của bạn</Info>
            ) : (
              <Info label="Nguyên nhân">{record.cause ?? 'Chưa ghi nhận'}</Info>
            )}
            {record.corrected && <Info label="Đính chính">Đã đính chính</Info>}
          </div>
          {mode === null && can('RESIDENCY_END_DEATH', 'UPDATE') && (
            <div className="flex justify-end">
              <Button variant="outline" className="h-11" onClick={() => setMode('correct')}>
                Đính chính bản ghi…
              </Button>
            </div>
          )}
          {mode === 'correct' && <CorrectForm record={record} onCancel={() => setMode(null)} onDone={changed} />}
        </Card>
      )}

      {tasks && (
        <Card className="gap-4 p-5">
          <h2 className="type-card-title">Việc bắt buộc sau qua đời</h2>
          <ul className="flex flex-col gap-3">
            {tasks.items.map((i) => (
              <li key={i.code} className="flex flex-wrap items-start justify-between gap-2">
                <span className="flex flex-col">
                  <span className="type-body-sm">{POST_DEATH_ITEMS[i.code] ?? i.code}</span>
                  {i.basis && <span className="type-caption text-muted-foreground">{i.basis}</span>}
                </span>
                <StatusBadge status={POST_DEATH_STATUS[i.status] ?? i.status} />
              </li>
            ))}
          </ul>
          {tasks.closedAt && (
            <p className="type-body-sm">{`Hồ sơ lưu trú đã đóng lúc ${formatDateTime(tasks.closedAt)}.`}</p>
          )}
          {settlement?.status === 'NOT_COMPLETED' && can('RESIDENCY_END_DEATH', 'CREATE') && mode === null && (
            <div className="flex justify-end">
              <Button variant="outline" className="h-11" onClick={() => setMode('exception')}>
                Xin ngoại lệ mục quyết toán…
              </Button>
            </div>
          )}
          {mode === 'exception' && (
            <SettlementException residentId={Number(residentId)} onCancel={() => setMode(null)} onDone={changed} />
          )}
        </Card>
      )}
    </div>
  )
}
