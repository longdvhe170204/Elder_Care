import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getNotificationChannels, putNotificationChannelsByCode } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { CHANNEL_LABELS, LEVEL_LABELS, label } from './notificationLabels'

// Kênh thông báo (009 T028; FR-016, FR-040): chỉ Quản lý viện. Trong ứng dụng và cổng người thân là hộp thông báo, dùng
// cho mọi mức, không có nút đổi; tin nhắn dùng cho Trung bình, Khẩn cấp (BR-M13-01); chỉ kênh khác gắn được mức.
// If-Match là ETag của cả danh sách kênh (hợp đồng không có phiên bản từng kênh).

const LEVELS = ['LOW', 'MEDIUM', 'URGENT']
const IN_APP = ['IN_APP', 'FAMILY_PORTAL']
const isOther = (code) => code.startsWith('OTHER_')

function usage(c) {
  if (IN_APP.includes(c.code)) return 'Mọi mức'
  if (c.code === 'SMS') return 'Trung bình, Khẩn cấp'
  const levels = LEVELS.filter((l) => c.levels.includes(l)).map((l) => LEVEL_LABELS[l])
  return levels.length ? levels.join(', ') : 'Chưa gắn mức'
}

export function NotificationChannels() {
  const [state, setState] = useState({ status: 'loading', rows: [], etag: null })
  const [editing, setEditing] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(async () => {
    try {
      const { data, etag } = await getNotificationChannels()
      setState({ status: 'ready', rows: data, etag })
    } catch {
      setState({ status: 'error', rows: [], etag: null })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở tab
    load()
  }, [load])

  function saved() {
    setEditing(null)
    setAnnouncement('Đã lưu thay đổi kênh')
    load()
  }

  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
      <p aria-live="polite" className="sr-only">
        {announcement}
      </p>
      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải kênh</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && (
        <Alert role="alert">
          <AlertTitle>Không tải được kênh thông báo</AlertTitle>
          <AlertAction>
            <Button variant="outline" size="sm" onClick={load}>
              Thử lại
            </Button>
          </AlertAction>
        </Alert>
      )}
      {state.status === 'ready' && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Kênh</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Dùng cho mức</TableHead>
              <TableHead>
                <span className="sr-only">Thao tác</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.rows.map((c) => (
              <TableRow key={c.code}>
                <TableCell>{c.name || label(CHANNEL_LABELS, c.code)}</TableCell>
                <TableCell>
                  <StatusBadge status={c.enabled ? 'Đang bật' : 'Đã tắt'} />
                </TableCell>
                <TableCell>{usage(c)}</TableCell>
                <TableCell className="text-right">
                  {!IN_APP.includes(c.code) && (
                    <Button variant="outline" size="sm" onClick={() => setEditing(c)}>
                      Đổi
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Sheet open={editing !== null} onOpenChange={(open) => !open && setEditing(null)}>
        <SheetContent className="sm:max-w-lg">
          {editing && <ChannelForm channel={editing} etag={state.etag} onSaved={saved} onStale={load} />}
        </SheetContent>
      </Sheet>
    </section>
  )
}

function ChannelForm({ channel, etag, onSaved, onStale }) {
  const [enabled, setEnabled] = useState(channel.enabled)
  const [levels, setLevels] = useState(channel.levels)
  const [reason, setReason] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const key = useRef(crypto.randomUUID())

  function toggleLevel(level, checked) {
    setLevels((ls) => (checked ? [...new Set([...ls, level])] : ls.filter((l) => l !== level)))
  }

  async function save() {
    setFailure(null)
    if (!reason.trim()) {
      setErrors({ reason: 'Nhập lý do thay đổi' })
      return
    }
    setErrors({})
    setBusy(true)
    try {
      await putNotificationChannelsByCode({
        path: { code: channel.code },
        headers: { 'If-Match': etag, 'Idempotency-Key': key.current },
        body: { code: channel.code, name: channel.name, enabled, levels, reason: reason.trim() },
      })
      onSaved()
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422) {
        setErrors({ reason: p.detail || p.title })
      } else {
        setFailure(p)
        if (error instanceof ApiError && error.status === 412) onStale()
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{channel.name || label(CHANNEL_LABELS, channel.code)}</SheetTitle>
        <SheetDescription>{`Dùng cho mức: ${usage(channel)}`}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6">
        <FieldGroup>
          <Field orientation="horizontal">
            <Checkbox id="channel-enabled" checked={enabled} onCheckedChange={(c) => setEnabled(c === true)} />
            <FieldLabel htmlFor="channel-enabled">Bật kênh</FieldLabel>
          </Field>
          {isOther(channel.code) && (
            <FieldSet>
              <FieldLegend variant="label">Mức được gắn</FieldLegend>
              {LEVELS.map((l) => (
                <Field key={l} orientation="horizontal">
                  <Checkbox
                    id={`channel-level-${l}`}
                    checked={levels.includes(l)}
                    onCheckedChange={(c) => toggleLevel(l, c === true)}
                  />
                  <FieldLabel htmlFor={`channel-level-${l}`}>{LEVEL_LABELS[l]}</FieldLabel>
                </Field>
              ))}
            </FieldSet>
          )}
          <Field data-invalid={!!errors.reason || undefined}>
            <FieldLabel htmlFor="channel-reason">Lý do</FieldLabel>
            <Textarea
              id="channel-reason"
              aria-invalid={!!errors.reason || undefined}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            {errors.reason && <FieldError>{errors.reason}</FieldError>}
          </Field>
        </FieldGroup>
        <ProblemAlert problem={failure} />
        <Button className="h-11 self-start" disabled={busy} onClick={save}>
          Lưu thay đổi
        </Button>
      </div>
    </>
  )
}
