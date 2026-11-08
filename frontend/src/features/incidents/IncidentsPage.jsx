import { useCallback, useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { DetailLayout } from '@/common/DetailLayout'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { TransferOutPanel } from './TransferOutPanel'
import { loadIncident, loadIncidentBoard, loadResidentChoices, recordIncident, runIncidentCommand } from './incidents'
import {
  LEVEL_LABEL,
  LEVEL_ORDER,
  STATUS_LABEL,
  isOpen,
  needsLowerReason,
  sortIncidents,
  statusTone,
  vietnamIso,
  vietnamNowLocal,
} from './incidentView'

// Màn Sự cố (spec 007 User Story 4, FR-040 → FR-046; T037 phần web): danh sách theo phạm vi từ api-007
// `GET /incidents`, khung chi tiết có nội dung, diễn biến và nút lệnh theo `allowedCommands` (DESIGN.md 7.4). Ghi nhận
// sự cố bắt buộc trực tuyến; mức thấp hơn mặc định của loại cần lý do (FR-042). Giao người xử lý cần danh sách người
// đủ điều kiện mà api-007 chưa có nên chưa có nút ở đây.

const GROUPS = [
  ['open', 'Đang mở'],
  ['ended', 'Đã kết thúc'],
]
const INFECTION_OUTCOME = [
  ['CONFIRMED_INFECTION', 'Xác định lây nhiễm'],
  ['RULED_OUT', 'Loại trừ'],
  ['UNDETERMINED', 'Chưa xác định'],
]
const FORM_TITLE = {
  progressions: 'Ghi diễn biến',
  'change-level': 'Đổi mức',
  reopen: 'Mở lại xử lý',
  close: 'Đóng sự cố',
}
const SUBMIT_LABEL = {
  progressions: 'Lưu diễn biến',
  'change-level': 'Lưu mức mới',
  reopen: 'Lưu và mở lại',
  close: 'Lưu và đóng sự cố',
}
const TEXT_LABEL = {
  progressions: 'Nội dung',
  'change-level': 'Lý do đổi mức',
  reopen: 'Lý do mở lại',
  close: 'Kết quả xử lý',
}
const TEXT_ERROR = {
  progressions: 'Nhập nội dung diễn biến',
  'change-level': 'Nhập lý do đổi mức',
  reopen: 'Nhập lý do mở lại',
  close: 'Nhập kết quả xử lý',
}

function IncidentStatus({ incident }) {
  return <Badge variant={statusTone(incident)}>{STATUS_LABEL[incident.status] ?? incident.status}</Badge>
}

function Facts({ rows }) {
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 type-body-sm">
      {rows
        .filter(([, value]) => value)
        .map(([label, value]) => (
          <div key={label} className="contents">
            <dt className="text-muted-foreground">{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
    </dl>
  )
}

function CommandForm({ mode, incident, progressionTypes, typeCode, busy, onSubmit, onCancel }) {
  const [text, setText] = useState('')
  const [choice, setChoice] = useState('')
  const [outcome, setOutcome] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const needsChoice = mode === 'progressions' || mode === 'change-level'
  const textMissing = !text.trim()
  const choiceMissing = needsChoice && !choice

  function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (textMissing || choiceMissing) return
    const value = text.trim()
    const body = {
      progressions: { progressionTypeId: Number(choice), content: value },
      'change-level': { newLevel: choice, reason: value },
      reopen: { reason: value },
      close: { resolution: value, ...(outcome ? { infectionOutcome: outcome } : {}) },
    }[mode]
    onSubmit(mode, body)
  }

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4">
      <h3 className="type-card-title">{FORM_TITLE[mode]}</h3>
      {mode === 'progressions' && (
        <Field data-invalid={(submitted && choiceMissing) || undefined}>
          <FieldLabel>Loại diễn biến</FieldLabel>
          <ToggleGroup
            variant="outline"
            value={choice ? [choice] : []}
            onValueChange={(n) => setChoice(n[0] ?? '')}
            className="flex flex-wrap"
          >
            {progressionTypes
              .filter((t) => t.status !== 'INACTIVE')
              .map((t) => (
                <ToggleGroupItem key={t.id} value={String(t.id)} className="h-11 px-4">
                  {t.name}
                </ToggleGroupItem>
              ))}
          </ToggleGroup>
          {submitted && choiceMissing && <FieldError>Chọn loại diễn biến</FieldError>}
        </Field>
      )}
      {mode === 'change-level' && (
        <Field data-invalid={(submitted && choiceMissing) || undefined}>
          <FieldLabel>Mức mới</FieldLabel>
          <ToggleGroup
            variant="outline"
            value={choice ? [choice] : []}
            onValueChange={(n) => setChoice(n[0] ?? '')}
            className="flex flex-wrap"
          >
            {LEVEL_ORDER.filter((l) => l !== incident.level).map((l) => (
              <ToggleGroupItem key={l} value={l} className="h-11 px-4">
                {LEVEL_LABEL[l]}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          {submitted && choiceMissing && <FieldError>Chọn mức mới</FieldError>}
        </Field>
      )}
      <Field data-invalid={(submitted && textMissing) || undefined}>
        <FieldLabel htmlFor="incident-command-text">{TEXT_LABEL[mode]}</FieldLabel>
        <Textarea
          id="incident-command-text"
          rows={3}
          maxLength={2000}
          value={text}
          onChange={(e) => setText(e.target.value)}
          aria-invalid={(submitted && textMissing) || undefined}
        />
        {submitted && textMissing && <FieldError>{TEXT_ERROR[mode]}</FieldError>}
      </Field>
      {mode === 'close' && typeCode === 'SUSPECTED_INFECTION' && (
        <Field>
          <FieldLabel>Kết luận lây nhiễm</FieldLabel>
          <ToggleGroup
            variant="outline"
            value={outcome ? [outcome] : []}
            onValueChange={(n) => setOutcome(n[0] ?? '')}
            className="flex flex-wrap"
          >
            {INFECTION_OUTCOME.map(([value, label]) => (
              <ToggleGroupItem key={value} value={value} className="h-11 px-4">
                {label}
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
        </Field>
      )}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" className="h-11" disabled={busy}>
          {SUBMIT_LABEL[mode]}
        </Button>
        <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
          Quay lại
        </Button>
      </div>
    </form>
  )
}

function IncidentDetail({ id, name, types, Title, Description, onChanged }) {
  const [state, setState] = useState(null)
  const [problem, setProblem] = useState(null)
  const [mode, setMode] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    loadIncident(id).then(
      (data) => !cancelled && setState(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [id])

  if (!state) return problem ? <ProblemAlert problem={problem} /> : <Skeleton className="h-40 w-full" />

  const { incident, progressionTypes } = state
  const commands = incident.allowedCommands ?? []
  const type = types.find((t) => t.id === incident.currentTypeId)
  const progressionName = (typeId) => progressionTypes.find((t) => t.id === typeId)?.name ?? 'Diễn biến'

  async function run(command, body, message) {
    setBusy(true)
    setProblem(null)
    try {
      await runIncidentCommand(incident.id, command, body)
      setMode(null)
      setState(await loadIncident(incident.id))
      onChanged(message)
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const canTransfer =
    ['NEW', 'IN_PROGRESS'].includes(incident.status) && incident.content?.residentId != null && commands.includes('change-level')

  const messages = {
    progressions: 'Đã ghi diễn biến',
    'change-level': 'Đã đổi mức sự cố',
    reopen: 'Đã mở lại xử lý',
    close: 'Đã đóng sự cố',
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-2">
        <Title className="type-title">{type?.name ?? 'Sự cố'}</Title>
        <Description className="type-body-sm text-muted-foreground">{name}</Description>
        <div className="flex flex-wrap gap-2">
          <StatusBadge status={LEVEL_LABEL[incident.level]} />
          <IncidentStatus incident={incident} />
        </div>
      </div>
      <Facts
        rows={[
          ['Phát hiện lúc', formatDateTime(incident.content?.discoveredAt)],
          ['Xảy ra lúc', incident.content?.occurredAt && formatDateTime(incident.content.occurredAt)],
          ['Nơi xảy ra', incident.content?.location],
          ['Mô tả', incident.content?.description],
          ['Xử lý ban đầu', incident.content?.initialResponse],
          ['Người xử lý', incident.assignee ?? 'Chưa ai nhận'],
          ['Tiếp nhận lúc', incident.receivedAt && formatDateTime(incident.receivedAt)],
          ['Kết quả', incident.resolution],
        ]}
      />
      {incident.noOnDutyDoctor && <p className="type-body-sm text-destructive">Không có bác sĩ trực lúc ghi nhận</p>}
      <section className="flex flex-col gap-2">
        <h3 className="type-caption text-muted-foreground">Diễn biến</h3>
        {(incident.progressions ?? []).length === 0 ? (
          <p className="type-body-sm text-muted-foreground">Chưa ghi diễn biến.</p>
        ) : (
          <ol className="flex flex-col gap-1.5 border-l-2 pl-3 type-body-sm">
            {incident.progressions.map((p) => (
              <li key={p.id} className="flex flex-col">
                <span className="tabular-nums text-muted-foreground">
                  {formatDateTime(p.occurredAt)}, {progressionName(p.progressionTypeId)}
                </span>
                <span>{p.content}</span>
              </li>
            ))}
          </ol>
        )}
      </section>
      {problem && <ProblemAlert problem={problem} />}
      {mode === 'transfer-out' ? (
        <TransferOutPanel
          incidentId={incident.id}
          onCancel={() => setMode(null)}
          onDone={async () => {
            setMode(null)
            setState(await loadIncident(incident.id))
            onChanged('Đã chuyển viện')
          }}
        />
      ) : mode ? (
        <CommandForm
          key={mode}
          mode={mode}
          incident={incident}
          progressionTypes={progressionTypes}
          typeCode={type?.code}
          busy={busy}
          onSubmit={(command, body) => run(command, body, messages[command])}
          onCancel={() => setMode(null)}
        />
      ) : (
        <div className="flex flex-col gap-2">
          {commands.includes('receive') && (
            <Button className="h-11" disabled={busy} onClick={() => run('receive', null, 'Đã tiếp nhận sự cố')}>
              Tiếp nhận
            </Button>
          )}
          {commands.includes('close') && (
            <Button className="h-11" onClick={() => setMode('close')}>
              Đóng sự cố
            </Button>
          )}
          {commands.includes('progressions') && (
            <Button variant="outline" className="h-11" onClick={() => setMode('progressions')}>
              Ghi diễn biến
            </Button>
          )}
          {commands.includes('move-to-monitoring') && (
            <Button
              variant="outline"
              className="h-11"
              disabled={busy}
              onClick={() => run('move-to-monitoring', null, 'Đã chuyển theo dõi')}
            >
              Chuyển theo dõi
            </Button>
          )}
          {commands.includes('reopen') && (
            <Button variant="outline" className="h-11" onClick={() => setMode('reopen')}>
              Mở lại xử lý
            </Button>
          )}
          {commands.includes('change-level') && (
            <Button variant="outline" className="h-11" onClick={() => setMode('change-level')}>
              Đổi mức
            </Button>
          )}
          {canTransfer && (
            <Button variant="outline" className="h-11" onClick={() => setMode('transfer-out')}>
              Chuyển viện
            </Button>
          )}
          {commands.includes('join-handling') && (
            <Button
              variant="outline"
              className="h-11"
              disabled={busy}
              onClick={() => run('join-handling', null, 'Đã tham gia xử lý')}
            >
              Tham gia xử lý
            </Button>
          )}
        </div>
      )}
    </div>
  )
}

// Ghi nhận sự cố (FR-040, FR-042): người cao tuổi (loại cơ sở vật chất được bỏ trống), loại, mức, thời điểm phát hiện,
// mô tả; mức thấp hơn mặc định cần lý do. Khẩn cấp kích hoạt quy trình ở backend trong cùng lệnh.
function RecordIncidentSheet({ open, types, onClose, onRecorded }) {
  const [residents, setResidents] = useState(null)
  const [residentId, setResidentId] = useState(null)
  const [typeId, setTypeId] = useState('')
  const [level, setLevel] = useState('')
  const [discoveredAt, setDiscoveredAt] = useState(() => vietnamNowLocal())
  const [description, setDescription] = useState('')
  const [lowerReason, setLowerReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!open) return undefined
    let cancelled = false
    loadResidentChoices().then(
      (data) => !cancelled && setResidents(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [open])

  const activeTypes = types.filter((t) => t.status !== 'INACTIVE')
  const type = activeTypes.find((t) => String(t.id) === typeId)
  const residentRequired = !type?.allowsNoResident
  const lowerNeeded = needsLowerReason(level, type)
  const errors = {
    resident: residentRequired && residentId == null,
    type: !type,
    level: !level,
    description: !description.trim(),
    lowerReason: lowerNeeded && !lowerReason.trim(),
  }

  function pickType(value) {
    setTypeId(value)
    const picked = activeTypes.find((t) => String(t.id) === value)
    if (picked && !level) setLevel(picked.defaultLevel)
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setProblem(null)
    try {
      await recordIncident({
        ...(residentId != null ? { residentId } : {}),
        incidentTypeId: type.id,
        level,
        discoveredAt: vietnamIso(discoveredAt),
        description: description.trim(),
        ...(lowerNeeded ? { lowerLevelReason: lowerReason.trim() } : {}),
      })
      onRecorded('Đã ghi nhận sự cố')
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open={open} onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetHeader className="px-0">
          <SheetTitle className="type-title">Ghi nhận sự cố</SheetTitle>
          <SheetDescription>Mức Khẩn cấp mở quy trình khẩn cấp ngay khi lưu.</SheetDescription>
        </SheetHeader>
        <form onSubmit={submit} noValidate className="flex flex-col gap-4">
          <Field data-invalid={(submitted && errors.resident) || undefined}>
            <FieldLabel>Người cao tuổi</FieldLabel>
            {!residents ? (
              <Skeleton className="h-11 w-full" />
            ) : (
              <div className="flex flex-col gap-1.5">
                {residents.map((r) => (
                  <Button
                    key={r.id}
                    type="button"
                    variant={residentId === r.id ? 'default' : 'outline'}
                    aria-pressed={residentId === r.id}
                    aria-label={`${r.name}, ${r.code}`}
                    className="h-11 justify-start gap-2"
                    onClick={() => setResidentId(r.id)}
                  >
                    <ResidentAvatar name={r.name} code={String(r.id)} className="size-6" />
                    <span>{r.name}</span>
                    <span className="type-caption">{r.code}</span>
                  </Button>
                ))}
              </div>
            )}
            {submitted && errors.resident && <FieldError>Chọn người cao tuổi</FieldError>}
          </Field>
          <Field data-invalid={(submitted && errors.type) || undefined}>
            <FieldLabel>Loại sự cố</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={typeId ? [typeId] : []}
              onValueChange={(n) => pickType(n[0] ?? '')}
              className="flex flex-wrap"
            >
              {activeTypes.map((t) => (
                <ToggleGroupItem key={t.id} value={String(t.id)} className="h-11 px-4">
                  {t.name}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
            {submitted && errors.type && <FieldError>Chọn loại sự cố</FieldError>}
          </Field>
          <Field data-invalid={(submitted && errors.level) || undefined}>
            <FieldLabel>Mức</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={level ? [level] : []}
              onValueChange={(n) => setLevel(n[0] ?? '')}
              className="flex flex-wrap"
            >
              {LEVEL_ORDER.map((l) => (
                <ToggleGroupItem key={l} value={l} className="h-11 px-4">
                  {LEVEL_LABEL[l]}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
            {type && (
              <span className="type-caption text-muted-foreground">
                Mặc định của loại: {LEVEL_LABEL[type.defaultLevel]}
              </span>
            )}
            {submitted && errors.level && <FieldError>Chọn mức</FieldError>}
          </Field>
          {lowerNeeded && (
            <Field data-invalid={(submitted && errors.lowerReason) || undefined}>
              <FieldLabel htmlFor="incident-lower-reason">Lý do chọn mức thấp hơn</FieldLabel>
              <Textarea
                id="incident-lower-reason"
                rows={2}
                maxLength={2000}
                value={lowerReason}
                onChange={(e) => setLowerReason(e.target.value)}
                aria-invalid={(submitted && errors.lowerReason) || undefined}
              />
              {submitted && errors.lowerReason && <FieldError>Nhập lý do chọn mức thấp hơn mặc định</FieldError>}
            </Field>
          )}
          <Field>
            <FieldLabel htmlFor="incident-discovered-at">Phát hiện lúc</FieldLabel>
            <Input
              id="incident-discovered-at"
              type="datetime-local"
              className="h-11"
              value={discoveredAt}
              onChange={(e) => setDiscoveredAt(e.target.value)}
            />
          </Field>
          <Field data-invalid={(submitted && errors.description) || undefined}>
            <FieldLabel htmlFor="incident-description">Mô tả</FieldLabel>
            <Textarea
              id="incident-description"
              rows={3}
              maxLength={2000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              aria-invalid={(submitted && errors.description) || undefined}
            />
            {submitted && errors.description && <FieldError>Nhập mô tả sự cố</FieldError>}
          </Field>
          {problem && <ProblemAlert problem={problem} />}
          <Button type="submit" className="h-11" disabled={busy}>
            Lưu sự cố
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

export function IncidentsPage() {
  const [board, setBoard] = useState(null)
  const [problem, setProblem] = useState(null)
  const [group, setGroup] = useState('open')
  const [openId, setOpenId] = useState(null)
  const [recording, setRecording] = useState(false)
  const [announcement, setAnnouncement] = useState('')

  const reload = useCallback(async () => {
    try {
      setBoard(await loadIncidentBoard())
      setProblem(null)
    } catch (error) {
      setProblem(problemOf(error))
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    loadIncidentBoard().then(
      (data) => !cancelled && setBoard(data),
      (error) => {
        if (cancelled) return
        setProblem(problemOf(error))
        setBoard({ incidents: [], types: [], names: {} })
      },
    )
    return () => {
      cancelled = true
    }
  }, [])

  if (problem?.status === 403) return <PermissionDeniedState label="Sự cố" />

  const incidents = board?.incidents ?? []
  const types = board?.types ?? []
  const nameOf = (i) =>
    i.content?.residentId == null
      ? 'Không gắn người cao tuổi'
      : (board?.names?.[i.content.residentId] ?? 'Người cao tuổi')
  const typeName = (i) => types.find((t) => t.id === i.currentTypeId)?.name ?? 'Sự cố'
  const inGroup = (value) => incidents.filter((i) => (value === 'open' ? isOpen(i) : !isOpen(i)))
  const visible = sortIncidents(inGroup(group))
  const opened = incidents.find((i) => i.id === openId)

  async function changed(message) {
    setAnnouncement(`${message}.`)
    await reload()
  }

  return (
    <DetailLayout
      open={!!opened}
      detailKey={openId}
      onClose={() => setOpenId(null)}
      label="Chi tiết sự cố"
      renderDetail={({ Title, Description }) =>
        opened && (
          <IncidentDetail
            key={opened.id}
            id={opened.id}
            name={nameOf(opened)}
            types={types}
            Title={Title}
            Description={Description}
            onChanged={changed}
          />
        )
      }
    >
      <PageHeader title="Sự cố" subtitle="Sự cố trong phạm vi của bạn, khẩn cấp lên đầu.">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          <ToggleGroup
            variant="filter"
            size="lg"
            value={[group]}
            onValueChange={(n) => n[0] && setGroup(n[0])}
            aria-label="Nhóm sự cố"
          >
            {GROUPS.map(([value, label]) => (
              <ToggleGroupItem key={value} value={value} className="h-11 px-4">
                {label} <span className="tabular-nums">{inGroup(value).length}</span>
              </ToggleGroupItem>
            ))}
          </ToggleGroup>
          <Button className="h-11" onClick={() => setRecording(true)}>
            <Plus aria-hidden="true" />
            Ghi nhận sự cố
          </Button>
        </div>
      </PageHeader>
      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {announcement}
      </p>
      {problem && <ProblemAlert problem={problem} />}
      <Card>
        <CardContent>
          {!board ? (
            <Skeleton className="h-40 w-full" />
          ) : visible.length === 0 ? (
            <p className="type-body-sm">
              {group === 'open' ? 'Không có sự cố đang mở trong phạm vi của bạn.' : 'Chưa có sự cố đã kết thúc.'}
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Người cao tuổi</TableHead>
                  <TableHead>Loại</TableHead>
                  <TableHead>Mức</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Phát hiện lúc</TableHead>
                  <TableHead>Người xử lý</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visible.map((i) => (
                  <TableRow key={i.id} data-state={i.id === openId ? 'selected' : undefined}>
                    <TableCell>
                      <Button
                        variant="ghost"
                        className="h-auto justify-start gap-2 px-1 py-1"
                        onClick={() => setOpenId(i.id)}
                        aria-label={`${nameOf(i)}, ${typeName(i)}`}
                      >
                        <ResidentAvatar name={nameOf(i)} code={String(i.content?.residentId ?? i.id)} />
                        <span className="type-label">{nameOf(i)}</span>
                      </Button>
                    </TableCell>
                    <TableCell className="type-body-sm">{typeName(i)}</TableCell>
                    <TableCell>
                      <StatusBadge status={LEVEL_LABEL[i.level]} />
                    </TableCell>
                    <TableCell>
                      <IncidentStatus incident={i} />
                    </TableCell>
                    <TableCell className="type-body-sm tabular-nums">
                      {formatDateTime(i.content?.discoveredAt)}
                    </TableCell>
                    <TableCell className="type-body-sm">{i.assignee ?? 'Chưa ai nhận'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      {recording && (
        <RecordIncidentSheet
          open={recording}
          types={types}
          onClose={() => setRecording(false)}
          onRecorded={async (message) => {
            setRecording(false)
            await changed(message)
          }}
        />
      )}
    </DetailLayout>
  )
}
