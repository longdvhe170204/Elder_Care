import { useCallback, useEffect, useState } from 'react'
import {
  ApiError,
  getResidentsByIdInitialHealthRecord,
  postResidentsByIdInitialHealthRecordConfirm,
  putResidentsByIdInitialHealthRecord,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { CorrectionBadge } from '@/common/correction/CorrectionBadge'
import { CorrectionHistorySheet } from '@/common/correction/CorrectionHistorySheet'
import { HEALTH_RECORD_FIELDS, HEALTH_RECORD_TYPE } from './healthLabels'

// Hồ sơ sức khỏe ban đầu (001 T033; US2; FR-017; hợp đồng GET, PUT /residents/{id}/initial-health-record, POST …/confirm).
// Sửa được khi còn Bản nháp (If-Match lấy từ ETag lần đọc gần nhất); chỉ Bác sĩ xác nhận; sau đó chỉ đính chính qua
// khung dùng chung của 000. Nút hiện theo allowedCommands của hồ sơ; quyền thật do backend kiểm.

function blank(value) {
  const text = value.trim()
  return text === '' ? undefined : text
}

function formOf(record) {
  return Object.fromEntries(HEALTH_RECORD_FIELDS.map((f) => [f.name, record?.[f.name] ?? '']))
}

export function InitialHealthRecord({ residentId, allowedCommands }) {
  const [state, setState] = useState('loading')
  const [record, setRecord] = useState(null)
  const [etag, setEtag] = useState(null)
  const [failure, setFailure] = useState(null)
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState(null)
  const [busy, setBusy] = useState(false)
  const [correcting, setCorrecting] = useState(false)

  const canWrite = allowedCommands.includes('WRITE_INITIAL_HEALTH_RECORD')
  const canConfirm = allowedCommands.includes('CONFIRM_INITIAL_HEALTH_RECORD')

  const load = useCallback(async () => {
    try {
      const { data, etag: tag } = await getResidentsByIdInitialHealthRecord({ path: { id: residentId } })
      setRecord(data)
      setEtag(tag)
      setFailure(null)
      setState('ready')
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setRecord(null)
        setEtag(null)
        setState('none')
      } else {
        setFailure(problemOf(error))
        setState('error')
      }
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp hồ sơ khi mở tab
    load()
  }, [load])

  function startEditing() {
    setForm(formOf(record))
    setFailure(null)
    setEditing(true)
  }

  async function save(event) {
    event.preventDefault()
    setFailure(null)
    setBusy(true)
    try {
      const { data, etag: tag } = await putResidentsByIdInitialHealthRecord({
        path: { id: residentId },
        headers: record ? { 'If-Match': etag } : {},
        body: Object.fromEntries(HEALTH_RECORD_FIELDS.map((f) => [f.name, blank(form[f.name])])),
      })
      setRecord(data)
      setEtag(tag)
      setState('ready')
      setEditing(false)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  async function confirm() {
    setFailure(null)
    setBusy(true)
    try {
      await postResidentsByIdInitialHealthRecordConfirm({ path: { id: residentId }, headers: { 'If-Match': etag } })
      await load()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const set = (name) => (event) => setForm((current) => ({ ...current, [name]: event.target.value }))

  const problem = (
    <ProblemAlert problem={failure}>
      {failure?.status === 412 && (
        <Button
          type="button"
          variant="outline"
          className="h-11 self-start"
          onClick={() => {
            setEditing(false)
            load()
          }}
        >
          Xem giá trị mới
        </Button>
      )}
    </ProblemAlert>
  )

  return (
    <section aria-labelledby="initial-health-record-title" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <h3 id="initial-health-record-title" className="type-card-title">
          Hồ sơ sức khỏe ban đầu
        </h3>
        {record && <StatusBadge status={record.confirmed ? 'Đã xác nhận' : 'Bản nháp'} />}
        {record && <CorrectionBadge corrected={record.corrected} />}
      </div>

      {state === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải hồ sơ sức khỏe ban đầu</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-16" />
        </div>
      )}

      {state === 'error' && problem}

      {state === 'none' && !editing && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Chưa lập hồ sơ sức khỏe ban đầu</EmptyTitle>
            <EmptyDescription>
              Hồ sơ lập được khi người cao tuổi đang tiếp nhận hoặc đã vào ở; chưa lập cũng không chặn hoàn tất tiếp
              nhận.
            </EmptyDescription>
          </EmptyHeader>
          {canWrite && (
            <Button type="button" className="h-11" onClick={startEditing}>
              Lập hồ sơ
            </Button>
          )}
        </Empty>
      )}

      {editing && (
        <form onSubmit={save} noValidate className="flex flex-col gap-4">
          <FieldGroup>
            {HEALTH_RECORD_FIELDS.map((f) => (
              <Field key={f.name}>
                <FieldLabel htmlFor={`health-record-${f.name}`}>{f.label}</FieldLabel>
                {f.short ? (
                  <Input
                    id={`health-record-${f.name}`}
                    maxLength={10}
                    value={form[f.name]}
                    onChange={set(f.name)}
                    className="h-10 sm:max-w-40"
                  />
                ) : (
                  <Textarea id={`health-record-${f.name}`} rows={2} value={form[f.name]} onChange={set(f.name)} />
                )}
              </Field>
            ))}
          </FieldGroup>
          {problem}
          <div className="flex flex-wrap gap-3">
            <Button type="submit" className="h-11" disabled={busy}>
              Lưu bản nháp
            </Button>
            <Button
              type="button"
              variant="outline"
              className="h-11"
              onClick={() => {
                setEditing(false)
                setFailure(null)
              }}
            >
              Hủy
            </Button>
          </div>
        </form>
      )}

      {record && !editing && (
        <>
          <dl className="grid grid-cols-1 gap-x-4 gap-y-3 sm:grid-cols-2">
            {HEALTH_RECORD_FIELDS.map((f) => (
              <div key={f.name} className="flex flex-col">
                <dt className="type-caption text-muted-foreground">{f.label}</dt>
                <dd className="type-body-sm whitespace-pre-wrap font-medium">{record[f.name] || 'Chưa ghi nhận'}</dd>
              </div>
            ))}
          </dl>
          {record.confirmed && record.confirmedBy && (
            <p className="type-caption text-muted-foreground">Bác sĩ xác nhận: {record.confirmedBy}</p>
          )}
          {problem}
          <div className="flex flex-wrap gap-3">
            {!record.confirmed && canWrite && (
              <Button type="button" variant="outline" className="h-11" onClick={startEditing}>
                Sửa bản nháp
              </Button>
            )}
            {!record.confirmed && canConfirm && (
              <Button type="button" className="h-11" disabled={busy} onClick={confirm}>
                Xác nhận hồ sơ
              </Button>
            )}
            {record.confirmed && (
              <Button type="button" variant="outline" className="h-11" onClick={() => setCorrecting(true)}>
                Đính chính
              </Button>
            )}
          </div>
          {record.confirmed && (
            <CorrectionHistorySheet
              recordType={HEALTH_RECORD_TYPE}
              recordId={record.id}
              fields={HEALTH_RECORD_FIELDS}
              open={correcting}
              onOpenChange={setCorrecting}
              canCorrect={canWrite}
              onChanged={load}
            />
          )}
        </>
      )}
    </section>
  )
}
