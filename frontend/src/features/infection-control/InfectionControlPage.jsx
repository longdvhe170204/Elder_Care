import { useEffect, useState } from 'react'
import { Plus, Search } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import {
  addContact,
  confirmContactList,
  createContactList,
  createIsolation,
  excludeContact,
  liftIsolation,
  loadContactList,
  loadInfectionBoard,
  lookupContacts,
  removeContact,
} from './infection'

// Màn Kiểm soát lây nhiễm (spec 007 User Story 8, FR-060 → FR-066; T054 phần web): vùng đang khoanh, sự cố lây
// nhiễm nghi ngờ đang mở và danh sách tiếp xúc của từng sự cố. Tra cứu chỉ hiện dữ liệu sẵn có theo nhóm, người lập
// tự chọn từng người (Q-306). Hợp đồng không trả lệnh được phép nên nút theo vai trò (Phụ lục 27): khoanh, gỡ vùng
// cho Bác sĩ, Quản lý viện; lập, sửa, xác nhận danh sách cho Bác sĩ, Điều dưỡng, Quản lý viện; backend kiểm lại.

const ZONE_ROLES = ['DOCTOR', 'NURSING_HOME_MANAGER']
const LIST_ROLES = ['DOCTOR', 'NURSING_STAFF', 'NURSING_HOME_MANAGER']
const GROUP_LABEL = {
  SAME_ROOM: 'Cùng phòng',
  ACTIVITY: 'Cùng hoạt động',
  CARE_STAFF: 'Nhân viên chăm sóc',
  FAMILY_VISIT: 'Người thân đến thăm',
  OTHER: 'Khác',
}
const TYPE_LABEL = {
  RESIDENT: 'Người cao tuổi',
  STAFF: 'Nhân viên',
  FAMILY_MEMBER: 'Người thân',
  COMPANION: 'Người đi cùng',
}
const LIST_STATUS = { DRAFT: 'Nháp', CONFIRMED: 'Đã xác nhận', ENDED: 'Đã kết thúc' }

function ReasonForm({ submitLabel, busy, onSubmit, onCancel }) {
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const missing = !reason.trim()
  return (
    <form
      noValidate
      className="flex flex-col gap-3"
      onSubmit={(e) => {
        e.preventDefault()
        setSubmitted(true)
        if (!missing) onSubmit(reason.trim())
      }}
    >
      <Field data-invalid={(submitted && missing) || undefined}>
        <FieldLabel htmlFor="infection-reason">Lý do</FieldLabel>
        <Textarea
          id="infection-reason"
          rows={2}
          maxLength={2000}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          aria-invalid={(submitted && missing) || undefined}
        />
        {submitted && missing && <FieldError>Nhập lý do</FieldError>}
      </Field>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" className="h-11" disabled={busy}>
          {submitLabel}
        </Button>
        <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
          Quay lại
        </Button>
      </div>
    </form>
  )
}

function scopeText(scope, floors, areas) {
  if (scope?.areaId != null) return areas.find((a) => a.id === scope.areaId)?.name ?? 'Khu vực'
  return (scope?.floorIds ?? []).map((id) => floors.find((f) => f.id === id)?.name ?? 'Tầng').join(', ')
}

// Khoanh vùng (FR-064): một hoặc nhiều tầng, hoặc đúng một khu vực (Q-69); gắn ít nhất một sự cố lây nhiễm chưa đóng
function IsolationSheet({ board, onClose, onSaved }) {
  const [kind, setKind] = useState('floors')
  const [floorIds, setFloorIds] = useState([])
  const [areaId, setAreaId] = useState([])
  const [incidentIds, setIncidentIds] = useState([])
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const errors = {
    scope: kind === 'floors' ? floorIds.length === 0 : areaId.length === 0,
    incidents: incidentIds.length === 0,
    reason: !reason.trim(),
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setProblem(null)
    try {
      await createIsolation({
        scope: kind === 'floors' ? { floorIds: floorIds.map(Number) } : { areaId: Number(areaId[0]) },
        incidentIds: incidentIds.map(Number),
        reason: reason.trim(),
      })
      onSaved('Đã khoanh vùng')
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-lg">
        <SheetHeader className="px-0">
          <SheetTitle className="type-title">Khoanh vùng</SheetTitle>
          <SheetDescription>
            Một hoặc nhiều tầng, hoặc đúng một khu vực. Người có trách nhiệm được thông báo khi lưu.
          </SheetDescription>
        </SheetHeader>
        <form onSubmit={submit} noValidate className="flex flex-col gap-4">
          <Field>
            <FieldLabel>Phạm vi</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={[kind]}
              onValueChange={(n) => n[0] && setKind(n[0])}
              className="flex flex-wrap"
            >
              <ToggleGroupItem value="floors" className="h-11 px-4">
                Theo tầng
              </ToggleGroupItem>
              <ToggleGroupItem value="area" className="h-11 px-4">
                Một khu vực
              </ToggleGroupItem>
            </ToggleGroup>
          </Field>
          <Field data-invalid={(submitted && errors.scope) || undefined}>
            <FieldLabel>{kind === 'floors' ? 'Tầng' : 'Khu vực'}</FieldLabel>
            {kind === 'floors' ? (
              <ToggleGroup
                variant="outline"
                multiple
                value={floorIds}
                onValueChange={setFloorIds}
                className="flex flex-wrap"
              >
                {board.floors.map((f) => (
                  <ToggleGroupItem key={f.id} value={String(f.id)} className="h-11 px-4">
                    {f.name}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
            ) : (
              <ToggleGroup
                variant="outline"
                value={areaId}
                onValueChange={(n) => setAreaId(n.slice(-1))}
                className="flex flex-wrap"
              >
                {board.areas.map((a) => (
                  <ToggleGroupItem key={a.id} value={String(a.id)} className="h-11 px-4">
                    {a.name}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
            )}
            {submitted && errors.scope && <FieldError>Chọn phạm vi khoanh vùng</FieldError>}
          </Field>
          <Field data-invalid={(submitted && errors.incidents) || undefined}>
            <FieldLabel>Sự cố lây nhiễm</FieldLabel>
            {board.infections.length === 0 ? (
              <p className="type-body-sm text-muted-foreground">Chưa có sự cố lây nhiễm nghi ngờ đang mở.</p>
            ) : (
              <ToggleGroup
                variant="outline"
                multiple
                value={incidentIds}
                onValueChange={setIncidentIds}
                className="flex flex-wrap"
              >
                {board.infections.map((i) => (
                  <ToggleGroupItem key={i.id} value={String(i.id)} className="h-11 px-4">
                    {board.names[i.content?.residentId] ?? 'Người cao tuổi'}, {formatDateTime(i.content?.discoveredAt)}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
            )}
            {submitted && errors.incidents && <FieldError>Chọn ít nhất một sự cố</FieldError>}
          </Field>
          <Field data-invalid={(submitted && errors.reason) || undefined}>
            <FieldLabel htmlFor="isolation-reason">Lý do</FieldLabel>
            <Textarea
              id="isolation-reason"
              rows={2}
              maxLength={2000}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              aria-invalid={(submitted && errors.reason) || undefined}
            />
            {submitted && errors.reason && <FieldError>Nhập lý do</FieldError>}
          </Field>
          {problem && <ProblemAlert problem={problem} />}
          <Button type="submit" className="h-11" disabled={busy}>
            Lưu khoanh vùng
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

function ContactRow({ contact, list, canEdit, busy, onRemove, onExclude }) {
  const [excluding, setExcluding] = useState(false)
  return (
    <li className="flex flex-col gap-1 border-b pb-2 last:border-b-0">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="type-label">{contact.fullName ?? TYPE_LABEL[contact.type]}</span>
        {contact.excludedAt && <Badge variant="neutral">Đã loại</Badge>}
      </div>
      <span className="type-body-sm text-muted-foreground tabular-nums">
        {TYPE_LABEL[contact.type]}, {GROUP_LABEL[contact.group] ?? contact.group}, {formatDateTime(contact.contactFrom)}{' '}
        đến {formatDateTime(contact.contactTo)}
      </span>
      {canEdit && list.status === 'DRAFT' && (
        <Button variant="ghost" size="sm" className="self-start" disabled={busy} onClick={() => onRemove(contact)}>
          Bỏ khỏi danh sách
        </Button>
      )}
      {canEdit && list.status === 'CONFIRMED' && !contact.excludedAt && !excluding && (
        <Button variant="ghost" size="sm" className="self-start" onClick={() => setExcluding(true)}>
          Loại khỏi danh sách
        </Button>
      )}
      {excluding && (
        <ReasonForm
          submitLabel="Lưu và loại"
          busy={busy}
          onSubmit={(reason) => onExclude(contact, reason)}
          onCancel={() => setExcluding(false)}
        />
      )}
    </li>
  )
}

function ContactListPanel({ incident, name, canEdit, onMessage }) {
  const [list, setList] = useState(undefined)
  const [lookup, setLookup] = useState(null)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    loadContactList(incident.id).then(
      (data) => !cancelled && setList(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [incident.id])

  async function act(fn, message) {
    setBusy(true)
    setProblem(null)
    try {
      await fn()
      setList(await loadContactList(incident.id))
      onMessage(message)
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  async function search() {
    setBusy(true)
    setProblem(null)
    try {
      setLookup(await lookupContacts(incident.id))
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (list === undefined) return problem ? <ProblemAlert problem={problem} /> : <Skeleton className="h-24 w-full" />

  const listed = new Set((list?.contacts ?? []).map((c) => `${c.type}:${c.subjectId}`))
  const editable = canEdit && list && list.status !== 'ENDED'

  return (
    <Card role="region" aria-labelledby="contact-list-title">
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-3">
        <div className="flex flex-col">
          <h2 id="contact-list-title" className="type-title">
            Danh sách tiếp xúc
          </h2>
          <span className="type-body-sm text-muted-foreground">{name}</span>
        </div>
        {list && (
          <Badge variant={list.status === 'CONFIRMED' ? 'settled' : 'neutral'}>{LIST_STATUS[list.status]}</Badge>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {problem && <ProblemAlert problem={problem} />}
        {!list ? (
          canEdit ? (
            <Button
              className="h-11 self-start"
              disabled={busy}
              onClick={() => act(() => createContactList(incident.id), 'Đã lập danh sách tiếp xúc')}
            >
              Lập danh sách tiếp xúc
            </Button>
          ) : (
            <p className="type-body-sm text-muted-foreground">Chưa lập danh sách tiếp xúc.</p>
          )
        ) : (
          <>
            {(list.contacts ?? []).length === 0 ? (
              <p className="type-body-sm text-muted-foreground">
                {list.noContacts ? 'Đã xác nhận không có người tiếp xúc.' : 'Chưa có người trong danh sách.'}
              </p>
            ) : (
              <ul className="flex flex-col gap-2">
                {list.contacts.map((c) => (
                  <ContactRow
                    key={c.id ?? `${c.type}:${c.subjectId}`}
                    contact={c}
                    list={list}
                    canEdit={editable}
                    busy={busy}
                    onRemove={(x) => act(() => removeContact(list.id, x.id), 'Đã bỏ người khỏi danh sách')}
                    onExclude={(x, reason) =>
                      act(() => excludeContact(list.id, x.id, reason), 'Đã loại người khỏi danh sách')
                    }
                  />
                ))}
              </ul>
            )}
            {editable && list.status === 'DRAFT' && (
              <div className="flex flex-wrap gap-2">
                <Button
                  className="h-11"
                  disabled={busy || (list.contacts ?? []).length === 0}
                  onClick={() => act(() => confirmContactList(list.id), 'Đã xác nhận danh sách tiếp xúc')}
                >
                  Xác nhận danh sách
                </Button>
                <Button variant="outline" className="h-11" disabled={busy} onClick={search}>
                  <Search aria-hidden="true" />
                  Tra cứu tiếp xúc
                </Button>
              </div>
            )}
            {lookup && (
              <section aria-label="Kết quả tra cứu" className="flex flex-col gap-3 rounded-xl border p-3">
                <span className="type-caption text-muted-foreground">
                  Dữ liệu sẵn có theo nhóm, hệ thống không đề xuất; chọn từng người cần đưa vào danh sách.
                </span>
                {(lookup.group ?? []).map((g) => (
                  <div key={g.group} className="flex flex-col gap-1.5">
                    <span className="type-label">{GROUP_LABEL[g.group] ?? g.group}</span>
                    {g.noData && (
                      <span className="type-body-sm text-muted-foreground">Chưa có dữ liệu của nhóm này.</span>
                    )}
                    {(g.people ?? []).map((p) => (
                      <div
                        key={`${p.type}:${p.subjectId}`}
                        className="flex flex-wrap items-center justify-between gap-2 type-body-sm"
                      >
                        <span>
                          {p.fullName ?? TYPE_LABEL[p.type]}, {TYPE_LABEL[p.type]?.toLowerCase()}
                        </span>
                        {listed.has(`${p.type}:${p.subjectId}`) ? (
                          <span className="type-caption text-muted-foreground">Đã trong danh sách</span>
                        ) : (
                          <Button
                            variant="tonal"
                            size="sm"
                            disabled={busy}
                            aria-label={`Thêm ${p.fullName ?? TYPE_LABEL[p.type]}`}
                            onClick={() => act(() => addContact(list.id, p), 'Đã thêm người vào danh sách')}
                          >
                            Thêm
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>
                ))}
              </section>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}

export function InfectionControlPage({ role }) {
  const canZone = ZONE_ROLES.includes(role)
  const canList = LIST_ROLES.includes(role)
  const [board, setBoard] = useState(null)
  const [problem, setProblem] = useState(null)
  const [lifting, setLifting] = useState(null)
  const [creating, setCreating] = useState(false)
  const [selected, setSelected] = useState(null)
  const [busy, setBusy] = useState(false)
  const [announcement, setAnnouncement] = useState('')

  useEffect(() => {
    let cancelled = false
    loadInfectionBoard().then(
      (data) => !cancelled && setBoard(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [])

  async function reload(message) {
    setAnnouncement(`${message}.`)
    try {
      setBoard(await loadInfectionBoard())
    } catch (error) {
      setProblem(problemOf(error))
    }
  }

  async function lift(zone, reason) {
    setBusy(true)
    setProblem(null)
    try {
      await liftIsolation(zone.id, reason)
      setLifting(null)
      await reload('Đã gỡ khoanh vùng')
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (problem?.status === 403) return <PermissionDeniedState label="Kiểm soát lây nhiễm" />

  const active = (board?.zones ?? []).filter((z) => z.status === 'ACTIVE')
  const nameOf = (incident) => board?.names?.[incident.content?.residentId] ?? 'Người cao tuổi'

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-5">
      <PageHeader
        title="Kiểm soát lây nhiễm"
        subtitle="Vùng đang khoanh, sự cố lây nhiễm nghi ngờ và danh sách tiếp xúc."
      >
        {canZone && (
          <Button className="h-11" onClick={() => setCreating(true)} disabled={!board}>
            <Plus aria-hidden="true" />
            Khoanh vùng
          </Button>
        )}
      </PageHeader>
      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {announcement}
      </p>
      {problem && <ProblemAlert problem={problem} />}
      {!board ? (
        <Skeleton className="h-40 w-full" />
      ) : (
        <div className="grid items-start gap-5 xl:grid-cols-2">
          <div className="flex flex-col gap-5">
            <Card role="region" aria-labelledby="zones-title">
              <CardHeader>
                <h2 id="zones-title" className="type-title">
                  Vùng đang khoanh
                </h2>
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                {active.length === 0 && (
                  <p className="type-body-sm text-muted-foreground">Không có vùng đang khoanh.</p>
                )}
                {active.map((z) => (
                  <div key={z.id} className="flex flex-col gap-1.5 border-b pb-3 last:border-b-0 last:pb-0">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="type-card-title">{scopeText(z.scope, board.floors, board.areas)}</span>
                      <Badge variant="attention">Đang khoanh vùng</Badge>
                    </div>
                    <span className="type-body-sm text-muted-foreground tabular-nums">
                      Từ {formatDateTime(z.startedAt)}, {z.incidentIds?.length ?? 0} sự cố, lý do: {z.reason}
                    </span>
                    {canZone && lifting !== z.id && (
                      <Button variant="ghost" size="sm" className="self-start" onClick={() => setLifting(z.id)}>
                        Gỡ khoanh vùng
                      </Button>
                    )}
                    {lifting === z.id && (
                      <ReasonForm
                        submitLabel="Lưu và gỡ"
                        busy={busy}
                        onSubmit={(reason) => lift(z, reason)}
                        onCancel={() => setLifting(null)}
                      />
                    )}
                  </div>
                ))}
              </CardContent>
            </Card>
            <Card role="region" aria-labelledby="infections-title">
              <CardHeader>
                <h2 id="infections-title" className="type-title">
                  Sự cố lây nhiễm nghi ngờ
                </h2>
              </CardHeader>
              <CardContent className="flex flex-col gap-2">
                {board.infections.length === 0 && (
                  <p className="type-body-sm text-muted-foreground">Không có sự cố lây nhiễm nghi ngờ đang mở.</p>
                )}
                {board.infections.map((i) => (
                  <Button
                    key={i.id}
                    variant={selected?.id === i.id ? 'default' : 'outline'}
                    aria-pressed={selected?.id === i.id}
                    className="h-auto min-h-11 flex-col items-start gap-0.5 py-2"
                    onClick={() => setSelected(i)}
                  >
                    <span className="type-label">{nameOf(i)}</span>
                    <span className="type-caption tabular-nums">
                      Phát hiện {formatDateTime(i.content?.discoveredAt)}
                    </span>
                  </Button>
                ))}
              </CardContent>
            </Card>
          </div>
          {selected && (
            <ContactListPanel
              key={selected.id}
              incident={selected}
              name={nameOf(selected)}
              canEdit={canList}
              onMessage={(m) => setAnnouncement(`${m}: ${nameOf(selected)}.`)}
            />
          )}
        </div>
      )}
      {creating && board && (
        <IsolationSheet
          board={board}
          onClose={() => setCreating(false)}
          onSaved={async (message) => {
            setCreating(false)
            await reload(message)
          }}
        />
      )}
    </div>
  )
}
