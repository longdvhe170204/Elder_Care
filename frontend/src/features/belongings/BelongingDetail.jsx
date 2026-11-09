import { useCallback, useEffect, useState } from 'react'
import { getBelongingsById, postBelongingsHandoversByIdLinkIncident } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { openFile } from '@/common/files'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { ConsentForm } from './ConsentForm'
import { HandoverForm } from './HandoverForm'
import { amountText, commandsFor, GIVER_LABELS, holderLabel, kindLabel, statusLabel } from './belongingLabels'

// Khung chi tiết một đồ gửi (013 T011; FR-011, FR-012; api-013 GET /belongings/{id}): thông tin đồ, lịch sử bàn giao
// (cả lần chặn trả) và các lệnh. Hợp đồng không trả danh sách lệnh được phép nên nút hiện theo khóa quyền "Đồ gửi" của
// /me/permissions và trạng thái đồ; backend kiểm lại mọi lệnh.

// Bản ghi liên kết được sự cố ghi sau, một lần (FR-021, FR-024)
const LINKABLE = ['REPORT_LOST', 'RECORD_DAMAGE']

function IncidentLink({ entry, onLinked }) {
  const [incidentId, setIncidentId] = useState('')
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  async function link() {
    setFailure(null)
    if (!/^[1-9]\d*$/.test(incidentId)) {
      setFailure({ title: 'Nhập mã sự cố', detail: 'Mã sự cố là số đã ghi ở màn Sự cố.', unmetConditions: [] })
      return
    }
    setBusy(true)
    try {
      await postBelongingsHandoversByIdLinkIncident({
        path: { id: entry.id },
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: { incidentId: Number(incidentId) },
      })
      onLinked('Đã liên kết sự cố.')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <Input
          aria-label={'Mã sự cố cho ' + kindLabel(entry.kind).toLowerCase()}
          inputMode="numeric"
          className="h-10 w-32"
          value={incidentId}
          onChange={(e) => setIncidentId(e.target.value)}
        />
        <Button type="button" variant="outline" size="sm" className="h-10" disabled={busy} onClick={link}>
          Liên kết sự cố
        </Button>
      </div>
      <ProblemAlert problem={failure} />
    </div>
  )
}

function HistoryEntry({ entry, onFailure, canLink, onLinked }) {
  // Bên nhận là nhân viên thì backend không có tên (chỉ mã nhân viên): hiện vai trò thay cho vế bị trống
  const recipient = entry.recipientName ?? GIVER_LABELS[entry.recipientType]
  const parties = [entry.giverName, recipient].filter(Boolean).join(' → ')
  return (
    <li className="flex flex-col gap-1 border-t py-3 first:border-t-0 first:pt-0">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <span className="type-label">{kindLabel(entry.kind)}</span>
        <span className="type-body-sm tabular-nums text-muted-foreground">{formatDateTime(entry.occurredAt)}</span>
      </div>
      {parties && <span className="type-body-sm">{parties}</span>}
      <span className="type-body-sm text-muted-foreground">
        {[amountText(entry), entry.conditionDescription, entry.targetStatus && statusLabel(entry.targetStatus)]
          .filter(Boolean)
          .join(', ')}
      </span>
      {entry.reason && <span className="type-body-sm">Lý do: {entry.reason}</span>}
      {entry.incidentId != null && <span className="type-body-sm">Sự cố #{entry.incidentId}</span>}
      {entry.incidentId == null && canLink && LINKABLE.includes(entry.kind) && (
        <IncidentLink entry={entry} onLinked={onLinked} />
      )}
      {entry.photoFileIds?.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {entry.photoFileIds.map((id, index) => (
            <Button
              key={id}
              type="button"
              variant="outline"
              size="sm"
              onClick={() =>
                openFile(id).then(
                  () => onFailure(null),
                  (e) => onFailure(problemOf(e)),
                )
              }
            >
              Xem ảnh {index + 1}
            </Button>
          ))}
        </div>
      )}
    </li>
  )
}

export function BelongingDetail({ belongingId, catalogs, onChanged, Title, Description }) {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading' })
  const [action, setAction] = useState(null)
  const [notice, setNotice] = useState('')
  const [failure, setFailure] = useState(null)

  const load = useCallback(async () => {
    try {
      const { data } = await getBelongingsById({ path: { id: belongingId } })
      setState({ status: 'ready', ...data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [belongingId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải chi tiết khi mở khung
    load()
  }, [load])

  function done(message) {
    setAction(null)
    setNotice(message)
    load()
    onChanged?.()
  }

  if (state.status === 'loading') return <Skeleton className="h-40 w-full" />
  if (state.status === 'error') return <ProblemAlert problem={state.problem} />

  const b = state.belonging
  const type = catalogs.typeOf(b.typeId)
  const canUpdate = can('BELONGINGS', 'UPDATE')
  const actions = [
    canUpdate && commandsFor(b.status).length > 0 && ['handover', 'Bàn giao'],
    canUpdate &&
      ['HELD', 'IN_USE'].includes(b.status) &&
      type?.requiresConsentForUse && ['consent', 'Ghi đồng ý cho tự giữ'],
  ].filter(Boolean)

  return (
    <div className="flex flex-col gap-5 type-body-sm">
      <div className="flex flex-col gap-1">
        <Title className="type-title">{b.description}</Title>
        <Description className="type-body-sm text-muted-foreground">
          {catalogs.typeName(b.typeId)}
          {type?.valuable ? ', đồ có giá trị' : ''}
        </Description>
      </div>
      <StatusBadge status={statusLabel(b.status)} className="self-start" />
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
        <dt className="text-muted-foreground">{b.cashAmount != null ? 'Số tiền' : 'Số lượng'}</dt>
        <dd className="tabular-nums">{amountText(b)}</dd>
        <dt className="text-muted-foreground">Người giữ</dt>
        <dd>{holderLabel(b.holder)}</dd>
        <dt className="text-muted-foreground">Vị trí</dt>
        <dd>{catalogs.locationName(b.storageLocationId)}</dd>
        {b.originalBelongingId != null && (
          <>
            <dt className="text-muted-foreground">Tách từ đồ</dt>
            <dd className="tabular-nums">#{b.originalBelongingId}</dd>
          </>
        )}
      </dl>

      <div role="status" className="type-body-sm text-success-foreground">
        {notice}
      </div>
      <ProblemAlert problem={failure} />

      {actions.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {actions.map(([name, label]) => (
            <Button
              key={name}
              variant="outline"
              className="h-11"
              aria-pressed={action === name}
              onClick={() => setAction(action === name ? null : name)}
            >
              {label}
            </Button>
          ))}
        </div>
      )}
      {action === 'handover' && (
        <section className="flex flex-col gap-3 rounded-xl border p-3" aria-label="Bàn giao">
          <HandoverForm
            belonging={b}
            levels={catalogs.levels}
            locations={catalogs.locations}
            valuable={!!type?.valuable}
            onDone={done}
          />
        </section>
      )}
      {action === 'consent' && (
        <section className="flex flex-col gap-3 rounded-xl border p-3" aria-label="Ghi đồng ý cho tự giữ">
          <ConsentForm belonging={b} onDone={done} />
        </section>
      )}

      <section className="flex flex-col gap-2">
        <h3 className="type-card-title">Lịch sử bàn giao</h3>
        <ul className="flex flex-col">
          {state.handovers.map((h) => (
            <HistoryEntry key={h.id} entry={h} onFailure={setFailure} canLink={canUpdate} onLinked={done} />
          ))}
        </ul>
      </section>
    </div>
  )
}
