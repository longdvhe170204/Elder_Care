import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { ProblemAlert } from '@/common/ProblemAlert'
import { ResidentAvatar } from '@/common/ResidentAvatar'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import {
  cancelThreshold,
  createSchedule,
  createThreshold,
  loadOverview,
  loadResidentSettings,
  stopSchedule,
  stopThreshold,
} from './monitoring'
import {
  BOUNDS,
  FREQUENCY_LABEL,
  PRIORITY_LABEL,
  SCHEDULE_STATUS,
  THRESHOLD_STATUS,
  WEEKDAYS,
  frequencyText,
  indicatorNames,
  parseTimes,
  thresholdText,
  vietnamIso,
} from './monitoringView'

// Màn Lịch đo và ngưỡng (spec 007 User Story 2, FR-003 → FR-006, FR-020 → FR-025; T024 phần web): chọn người cao
// tuổi, xem lịch đo và ngưỡng cá nhân (cả lịch sử), danh sách "chưa có ngưỡng cá nhân". Hợp đồng không trả lệnh được
// phép nên nút tạo, ngừng, hủy chỉ cho Bác sĩ (Phụ lục 27), backend kiểm lại. Lịch đo là căn cứ để lập công việc đo,
// màn không tạo công việc (Q-306).

const tone = (status) => (status === 'EFFECTIVE' ? 'settled' : status === 'PENDING' ? 'attention' : 'neutral')

function ReasonForm({ title, submitLabel, busy, onSubmit, onCancel }) {
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const missing = !reason.trim()
  return (
    <form
      noValidate
      className="flex flex-col gap-3 rounded-xl border p-3"
      onSubmit={(e) => {
        e.preventDefault()
        setSubmitted(true)
        if (!missing) onSubmit(reason.trim())
      }}
    >
      <span className="type-label">{title}</span>
      <Field data-invalid={(submitted && missing) || undefined}>
        <FieldLabel htmlFor="monitoring-reason">Lý do</FieldLabel>
        <Textarea
          id="monitoring-reason"
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

function IndicatorChoice({ catalog, value, onChange, multiple }) {
  return (
    <ToggleGroup
      variant="outline"
      multiple={multiple}
      value={value}
      onValueChange={(next) => onChange(multiple ? next : next.slice(-1))}
      className="flex flex-wrap"
    >
      {catalog
        .filter((c) => c.active !== false)
        .map((c) => (
          <ToggleGroupItem key={c.id} value={String(c.id)} className="h-11 px-4">
            {c.name}
          </ToggleGroupItem>
        ))}
    </ToggleGroup>
  )
}

// Tạo lịch đo (FR-003, FR-004): chỉ số, tần suất (theo giờ, mỗi n giờ, theo thứ), bắt đầu, kết thúc, mức quan trọng
function ScheduleSheet({ resident, catalog, onClose, onSaved }) {
  const [indicators, setIndicators] = useState([])
  const [type, setType] = useState('CLOCK_TIMES')
  const [times, setTimes] = useState('')
  const [everyHours, setEveryHours] = useState('')
  const [weekdays, setWeekdays] = useState([])
  const [startAt, setStartAt] = useState('')
  const [endAt, setEndAt] = useState('')
  const [priority, setPriority] = useState('MANDATORY')
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)

  const parsedTimes = parseTimes(times)
  const errors = {
    indicators: indicators.length === 0,
    times: type !== 'EVERY_N_HOURS' && !parsedTimes,
    everyHours: type === 'EVERY_N_HOURS' && !(Number(everyHours) >= 1),
    weekdays: type === 'WEEKDAYS' && weekdays.length === 0,
    startAt: !startAt,
  }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    const frequency =
      type === 'EVERY_N_HOURS'
        ? { type, everyHours: Number(everyHours) }
        : type === 'WEEKDAYS'
          ? { type, weekdays: weekdays.map(Number), times: parsedTimes }
          : { type, times: parsedTimes }
    setBusy(true)
    setProblem(null)
    try {
      await createSchedule(resident.id, {
        indicatorIds: indicators.map(Number),
        frequency,
        startAt: vietnamIso(startAt),
        ...(endAt ? { endAt: vietnamIso(endAt) } : {}),
        priority,
      })
      onSaved('Đã tạo lịch đo')
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
          <SheetTitle className="type-title">Tạo lịch đo</SheetTitle>
          <SheetDescription>
            {resident.name}. Lịch đo là căn cứ để lập công việc đo, không tự tạo công việc.
          </SheetDescription>
        </SheetHeader>
        <form onSubmit={submit} noValidate className="flex flex-col gap-4">
          <Field data-invalid={(submitted && errors.indicators) || undefined}>
            <FieldLabel>Chỉ số</FieldLabel>
            <IndicatorChoice catalog={catalog} value={indicators} onChange={setIndicators} multiple />
            {submitted && errors.indicators && <FieldError>Chọn ít nhất một chỉ số</FieldError>}
          </Field>
          <Field>
            <FieldLabel>Tần suất</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={[type]}
              onValueChange={(n) => n[0] && setType(n[0])}
              className="flex flex-wrap"
            >
              {Object.entries(FREQUENCY_LABEL).map(([value, label]) => (
                <ToggleGroupItem key={value} value={value} className="h-11 px-4">
                  {label}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          </Field>
          {type === 'WEEKDAYS' && (
            <Field data-invalid={(submitted && errors.weekdays) || undefined}>
              <FieldLabel>Các thứ trong tuần</FieldLabel>
              <ToggleGroup
                variant="outline"
                multiple
                value={weekdays}
                onValueChange={setWeekdays}
                className="flex flex-wrap"
              >
                {WEEKDAYS.map(([value, label]) => (
                  <ToggleGroupItem key={value} value={String(value)} className="h-11 px-3">
                    {label}
                  </ToggleGroupItem>
                ))}
              </ToggleGroup>
              {submitted && errors.weekdays && <FieldError>Chọn ít nhất một thứ</FieldError>}
            </Field>
          )}
          {type === 'EVERY_N_HOURS' ? (
            <Field data-invalid={(submitted && errors.everyHours) || undefined}>
              <FieldLabel htmlFor="schedule-every">Số giờ giữa hai lần đo</FieldLabel>
              <Input
                id="schedule-every"
                type="number"
                min={1}
                className="h-11"
                value={everyHours}
                onChange={(e) => setEveryHours(e.target.value)}
              />
              {submitted && errors.everyHours && <FieldError>Nhập số giờ từ 1 trở lên</FieldError>}
            </Field>
          ) : (
            <Field data-invalid={(submitted && errors.times) || undefined}>
              <FieldLabel htmlFor="schedule-times">Giờ đo</FieldLabel>
              <Input
                id="schedule-times"
                className="h-11"
                placeholder="07:00, 19:00"
                value={times}
                onChange={(e) => setTimes(e.target.value)}
              />
              {submitted && errors.times && <FieldError>Nhập giờ dạng HH:mm, cách nhau bằng dấu phẩy</FieldError>}
            </Field>
          )}
          <Field data-invalid={(submitted && errors.startAt) || undefined}>
            <FieldLabel htmlFor="schedule-start">Bắt đầu</FieldLabel>
            <Input
              id="schedule-start"
              type="datetime-local"
              className="h-11"
              value={startAt}
              onChange={(e) => setStartAt(e.target.value)}
            />
            {submitted && errors.startAt && <FieldError>Chọn thời điểm bắt đầu</FieldError>}
          </Field>
          <Field>
            <FieldLabel htmlFor="schedule-end">Kết thúc (không bắt buộc)</FieldLabel>
            <Input
              id="schedule-end"
              type="datetime-local"
              className="h-11"
              value={endAt}
              onChange={(e) => setEndAt(e.target.value)}
            />
          </Field>
          <Field>
            <FieldLabel>Mức quan trọng</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={[priority]}
              onValueChange={(n) => n[0] && setPriority(n[0])}
              className="flex flex-wrap"
            >
              {Object.entries(PRIORITY_LABEL).map(([value, label]) => (
                <ToggleGroupItem key={value} value={value} className="h-11 px-4">
                  {label}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          </Field>
          {problem && <ProblemAlert problem={problem} />}
          <Button type="submit" className="h-11" disabled={busy}>
            Lưu lịch đo
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

// Thiết lập ngưỡng cá nhân (FR-020 → FR-022): một chỉ số, bốn cận mỗi thành phần (để trống là không đặt), hiệu lực từ,
// lý do (bắt buộc khi đã có phiên bản trước; backend kiểm khoảng vật lý và thứ tự cận, 422 INVALID_THRESHOLD)
function ThresholdSheet({ resident, catalog, onClose, onSaved }) {
  const [indicator, setIndicator] = useState([])
  const [bounds, setBounds] = useState({})
  const [effectiveFrom, setEffectiveFrom] = useState('')
  const [reason, setReason] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const picked = catalog.find((c) => String(c.id) === indicator[0])

  const components = (picked?.components ?? [])
    .map((c) => {
      const values = Object.fromEntries(
        BOUNDS.map(([key]) => [key, bounds[`${c.code}.${key}`]])
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, Number(v)]),
      )
      return { code: c.code, ...values }
    })
    .filter((c) => Object.keys(c).length > 1)
  const errors = { indicator: !picked, components: components.length === 0, effectiveFrom: !effectiveFrom }

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setProblem(null)
    try {
      await createThreshold({
        scope: 'PERSONAL',
        residentId: resident.id,
        indicatorId: picked.id,
        effectiveFrom: vietnamIso(effectiveFrom),
        ...(reason.trim() ? { reason: reason.trim() } : {}),
        components,
      })
      onSaved('Đã thiết lập ngưỡng cá nhân')
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
          <SheetTitle className="type-title">Thiết lập ngưỡng</SheetTitle>
          <SheetDescription>
            {resident.name}. Phiên bản Hiệu lực cũ hết hiệu lực từ thời điểm hiệu lực mới.
          </SheetDescription>
        </SheetHeader>
        <form onSubmit={submit} noValidate className="flex flex-col gap-4">
          <Field data-invalid={(submitted && errors.indicator) || undefined}>
            <FieldLabel>Chỉ số</FieldLabel>
            <IndicatorChoice catalog={catalog} value={indicator} onChange={setIndicator} />
            {submitted && errors.indicator && <FieldError>Chọn chỉ số</FieldError>}
          </Field>
          {(picked?.components ?? []).map((c) => (
            <fieldset key={c.code} className="flex flex-col gap-2">
              <legend className="type-label">
                {c.name}
                {picked.unit && <span className="type-caption text-muted-foreground"> ({picked.unit})</span>}
              </legend>
              <div className="grid grid-cols-2 gap-3">
                {BOUNDS.map(([key, label]) => (
                  <Field key={key}>
                    <FieldLabel htmlFor={`bound-${c.code}-${key}`}>{`${c.name}, ${label}`}</FieldLabel>
                    <Input
                      id={`bound-${c.code}-${key}`}
                      type="number"
                      step="any"
                      className="h-11"
                      value={bounds[`${c.code}.${key}`] ?? ''}
                      onChange={(e) => setBounds((b) => ({ ...b, [`${c.code}.${key}`]: e.target.value }))}
                    />
                  </Field>
                ))}
              </div>
            </fieldset>
          ))}
          {submitted && picked && errors.components && (
            <p className="type-body-sm text-destructive">Nhập ít nhất một cận</p>
          )}
          <Field data-invalid={(submitted && errors.effectiveFrom) || undefined}>
            <FieldLabel htmlFor="threshold-from">Hiệu lực từ</FieldLabel>
            <Input
              id="threshold-from"
              type="datetime-local"
              className="h-11"
              value={effectiveFrom}
              onChange={(e) => setEffectiveFrom(e.target.value)}
            />
            {submitted && errors.effectiveFrom && <FieldError>Chọn thời điểm hiệu lực</FieldError>}
          </Field>
          <Field>
            <FieldLabel htmlFor="threshold-reason">Lý do</FieldLabel>
            <Textarea
              id="threshold-reason"
              rows={2}
              maxLength={2000}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          </Field>
          {problem && <ProblemAlert problem={problem} />}
          <Button type="submit" className="h-11" disabled={busy}>
            Lưu ngưỡng
          </Button>
        </form>
      </SheetContent>
    </Sheet>
  )
}

function ResidentSettings({ resident, catalog, isDoctor, onMessage }) {
  const [settings, setSettings] = useState(null)
  const [problem, setProblem] = useState(null)
  const [reasonFor, setReasonFor] = useState(null)
  const [sheet, setSheet] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    loadResidentSettings(resident.id).then(
      (data) => !cancelled && setSettings(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [resident.id])

  async function refresh(message) {
    setReasonFor(null)
    setSheet(null)
    onMessage(message)
    try {
      setSettings(await loadResidentSettings(resident.id))
    } catch (error) {
      setProblem(problemOf(error))
    }
  }

  async function withReason(reason) {
    const { kind, id } = reasonFor
    setBusy(true)
    setProblem(null)
    try {
      if (kind === 'stop-schedule') await stopSchedule(id, reason)
      if (kind === 'cancel-threshold') await cancelThreshold(id, reason)
      if (kind === 'stop-threshold') await stopThreshold(id, reason)
      await refresh(
        {
          'stop-schedule': 'Đã ngừng lịch đo',
          'cancel-threshold': 'Đã hủy phiên bản ngưỡng',
          'stop-threshold': 'Đã ngừng ngưỡng cá nhân',
        }[kind],
      )
    } catch (error) {
      setProblem(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (problem?.status === 403) return <PermissionDeniedState label="Lịch đo và ngưỡng" />
  if (!settings) return problem ? <ProblemAlert problem={problem} /> : <Skeleton className="h-40 w-full" />

  const indicatorOf = (id) => catalog.find((c) => c.id === id)
  const reasonForm = (kind, id, title, submitLabel) =>
    reasonFor?.kind === kind && reasonFor.id === id ? (
      <ReasonForm
        title={title}
        submitLabel={submitLabel}
        busy={busy}
        onSubmit={withReason}
        onCancel={() => setReasonFor(null)}
      />
    ) : null

  return (
    <div className="grid gap-5 xl:grid-cols-2">
      {problem && <ProblemAlert problem={problem} />}
      <Card role="region" aria-labelledby="schedules-title">
        <CardHeader className="flex flex-row items-center justify-between gap-3">
          <h2 id="schedules-title" className="type-title">
            Lịch đo
          </h2>
          {isDoctor && (
            <Button variant="outline" onClick={() => setSheet('schedule')}>
              <Plus aria-hidden="true" />
              Tạo lịch đo
            </Button>
          )}
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {settings.schedules.length === 0 && <p className="type-body-sm text-muted-foreground">Chưa có lịch đo.</p>}
          {settings.schedules.map((s) => (
            <div key={s.id} className="flex flex-col gap-1.5 border-b pb-3 last:border-b-0 last:pb-0">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="type-card-title">{indicatorNames(s.indicatorIds ?? [], catalog)}</span>
                <Badge variant={tone(s.status)}>{SCHEDULE_STATUS[s.status] ?? s.status}</Badge>
              </div>
              <span className="type-body-sm">
                {frequencyText(s.frequency)}, {PRIORITY_LABEL[s.priority ?? 'MANDATORY'].toLowerCase()}
              </span>
              <span className="type-body-sm text-muted-foreground tabular-nums">
                Từ {formatDateTime(s.startAt)}
                {s.endAt && ` đến ${formatDateTime(s.endAt)}`}
                {s.stoppedAt && `, ngừng ${formatDateTime(s.stoppedAt)}`}
                {s.stopReason && `: ${s.stopReason}`}
              </span>
              {isDoctor && ['PENDING', 'EFFECTIVE'].includes(s.status) && !reasonFor && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="self-start"
                  onClick={() => setReasonFor({ kind: 'stop-schedule', id: s.id })}
                >
                  Ngừng lịch đo
                </Button>
              )}
              {reasonForm('stop-schedule', s.id, 'Ngừng lịch đo', 'Lưu và ngừng')}
            </div>
          ))}
        </CardContent>
      </Card>
      <Card role="region" aria-labelledby="thresholds-title">
        <CardHeader className="flex flex-row items-center justify-between gap-3">
          <h2 id="thresholds-title" className="type-title">
            Ngưỡng cá nhân
          </h2>
          {isDoctor && (
            <Button variant="outline" onClick={() => setSheet('threshold')}>
              <Plus aria-hidden="true" />
              Thiết lập ngưỡng
            </Button>
          )}
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {settings.thresholds.length === 0 && (
            <p className="type-body-sm text-muted-foreground">
              Chưa có ngưỡng cá nhân, đang so theo ngưỡng mặc định của viện.
            </p>
          )}
          {settings.thresholds.map((t) => (
            <div key={t.id} className="flex flex-col gap-1.5 border-b pb-3 last:border-b-0 last:pb-0">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="type-card-title">{indicatorOf(t.indicatorId)?.name ?? 'Chỉ số'}</span>
                <Badge variant={tone(t.status)}>{THRESHOLD_STATUS[t.status] ?? t.status}</Badge>
              </div>
              {(t.components ?? []).map((c) => (
                <span key={c.code} className="type-body-sm">
                  {thresholdText(c, indicatorOf(t.indicatorId))}
                </span>
              ))}
              <span className="type-body-sm text-muted-foreground tabular-nums">
                Hiệu lực từ {formatDateTime(t.effectiveFrom)}
                {t.effectiveUntil && ` đến ${formatDateTime(t.effectiveUntil)}`}
                {t.setBy && `, ${t.setBy}`}
              </span>
              {isDoctor && !reasonFor && t.status === 'PENDING' && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="self-start"
                  onClick={() => setReasonFor({ kind: 'cancel-threshold', id: t.id })}
                >
                  Hủy phiên bản
                </Button>
              )}
              {isDoctor && !reasonFor && t.status === 'EFFECTIVE' && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="self-start"
                  onClick={() => setReasonFor({ kind: 'stop-threshold', id: t.id })}
                >
                  Ngừng ngưỡng cá nhân
                </Button>
              )}
              {reasonForm('cancel-threshold', t.id, 'Hủy phiên bản ngưỡng', 'Lưu và hủy')}
              {reasonForm('stop-threshold', t.id, 'Ngừng ngưỡng cá nhân', 'Lưu và ngừng')}
            </div>
          ))}
        </CardContent>
      </Card>
      {sheet === 'schedule' && (
        <ScheduleSheet resident={resident} catalog={catalog} onClose={() => setSheet(null)} onSaved={refresh} />
      )}
      {sheet === 'threshold' && (
        <ThresholdSheet resident={resident} catalog={catalog} onClose={() => setSheet(null)} onSaved={refresh} />
      )}
    </div>
  )
}

export function MonitoringSettingsPage({ role }) {
  const isDoctor = role === 'DOCTOR'
  const [overview, setOverview] = useState(null)
  const [problem, setProblem] = useState(null)
  const [query, setQuery] = useState('')
  const [resident, setResident] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  useEffect(() => {
    let cancelled = false
    loadOverview().then(
      (data) => !cancelled && setOverview(data),
      (error) => !cancelled && setProblem(problemOf(error)),
    )
    return () => {
      cancelled = true
    }
  }, [])

  if (problem?.status === 403) return <PermissionDeniedState label="Lịch đo và ngưỡng" />

  const residents = overview?.residents ?? []
  const shown = residents.filter((r) => `${r.name} ${r.code}`.toLowerCase().includes(query.trim().toLowerCase()))
  const missing = residents.filter((r) => overview?.missing?.includes(r.id))

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-5">
      <PageHeader title="Lịch đo và ngưỡng" subtitle="Chọn người cao tuổi để xem lịch đo và ngưỡng cá nhân.">
        <Input
          aria-label="Tìm người cao tuổi"
          placeholder="Tìm theo họ tên, mã hồ sơ"
          className="h-11 w-72"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </PageHeader>
      <p role="status" aria-live="polite" className="empty:hidden rounded-xl border bg-card px-4 py-3 type-body-sm">
        {announcement}
      </p>
      {problem && <ProblemAlert problem={problem} />}
      {!overview ? (
        <Skeleton className="h-24 w-full" />
      ) : (
        <>
          {missing.length > 0 && (
            <section
              aria-label="Chưa có ngưỡng cá nhân"
              className="flex flex-wrap items-center gap-2 rounded-xl border bg-card px-4 py-3 type-body-sm"
            >
              <span className="type-label text-warning-foreground">Chưa có ngưỡng cá nhân ({missing.length})</span>
              {missing.map((r, i) => (
                <button
                  key={r.id}
                  type="button"
                  className="underline-offset-4 hover:underline"
                  onClick={() => setResident(r)}
                >
                  {r.name}
                  {i < missing.length - 1 && ','}
                </button>
              ))}
            </section>
          )}
          <Card>
            <CardContent className="flex flex-wrap gap-2">
              {shown.length === 0 && <p className="type-body-sm">Không có người cao tuổi khớp với tìm kiếm.</p>}
              {shown.map((r) => (
                <Button
                  key={r.id}
                  variant={resident?.id === r.id ? 'default' : 'outline'}
                  aria-pressed={resident?.id === r.id}
                  aria-label={`${r.name}, ${r.code}`}
                  className="h-11 gap-2"
                  onClick={() => setResident(r)}
                >
                  <ResidentAvatar name={r.name} code={String(r.id)} className="size-6" />
                  <span>{r.name}</span>
                  <span className="type-caption">{r.code}</span>
                </Button>
              ))}
            </CardContent>
          </Card>
          {resident && (
            <ResidentSettings
              key={resident.id}
              resident={resident}
              catalog={overview.catalog}
              isDoctor={isDoctor}
              onMessage={(m) => setAnnouncement(`${m}: ${resident.name}.`)}
            />
          )}
        </>
      )}
    </div>
  )
}
