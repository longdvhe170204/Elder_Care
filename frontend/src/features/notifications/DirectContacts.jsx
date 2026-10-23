import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getDirectContacts, postDirectContactsByIdResults } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { CONTACT_STATUS_LABELS, label } from './notificationLabels'

// Liên hệ trực tiếp (009 T028; FR-021, FR-031): Hành chính ghi kết quả, Quản lý viện chỉ xem (bảng FR-039). Kết quả là
// trạng thái cuối; sửa qua đính chính của 000 (`M13.DIRECT_CONTACT_RESULT`).

const STATUSES = ['PENDING', 'CONTACTED', 'UNREACHABLE']

// Giờ hiện tại theo dạng datetime-local của trình duyệt
function nowLocal() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

export function DirectContacts({ canRecord }) {
  const [status, setStatus] = useState('PENDING')
  const [state, setState] = useState({ status: 'loading', rows: [] })
  const [recording, setRecording] = useState(null)
  const [announcement, setAnnouncement] = useState('')

  const load = useCallback(async (s) => {
    setState({ status: 'loading', rows: [] })
    try {
      const { data } = await getDirectContacts({ query: { status: s } })
      setState({ status: 'ready', rows: data })
    } catch {
      setState({ status: 'error', rows: [] })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở tab hoặc đổi trạng thái
    load(status)
  }, [load, status])

  function saved() {
    setRecording(null)
    setAnnouncement('Đã lưu kết quả liên hệ')
    load(status)
  }

  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
      <ToggleGroup
        variant="outline"
        value={[status]}
        onValueChange={(next) => next[0] && setStatus(next[0])}
        className="flex-wrap self-start"
      >
        {STATUSES.map((s) => (
          <ToggleGroupItem key={s} value={s} className="h-10 px-4">
            {CONTACT_STATUS_LABELS[s]}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <p aria-live="polite" className="sr-only">
        {announcement}
      </p>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải danh sách liên hệ</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && (
        <Alert role="alert">
          <AlertTitle>Không tải được danh sách liên hệ trực tiếp</AlertTitle>
          <AlertAction>
            <Button variant="outline" size="sm" onClick={() => load(status)}>
              Thử lại
            </Button>
          </AlertAction>
        </Alert>
      )}
      {state.status === 'ready' && state.rows.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không có người thân nào ở trạng thái này</EmptyTitle>
            <EmptyDescription>
              Người thân không nhận được thông báo Trung bình sẽ hiện ở đây để gọi trực tiếp.
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {state.status === 'ready' && state.rows.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tạo lúc</TableHead>
              <TableHead>Người thân</TableHead>
              <TableHead>Số điện thoại</TableHead>
              <TableHead className="min-w-56">Nội dung rút gọn</TableHead>
              <TableHead>Trạng thái</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.rows.map((r) => (
              <TableRow key={r.id} className="align-top">
                <TableCell className="tabular-nums">{formatDateTime(r.createdAt)}</TableCell>
                <TableCell className="whitespace-normal">
                  {r.fullName ?? 'Chưa có họ tên'}
                  {r.relationship && <span className="block type-caption text-muted-foreground">{r.relationship}</span>}
                </TableCell>
                <TableCell className="tabular-nums">{r.phoneNumber ?? 'Không có số'}</TableCell>
                <TableCell className="whitespace-normal">{r.shortContent}</TableCell>
                <TableCell>
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={label(CONTACT_STATUS_LABELS, r.status)} />
                    {canRecord && r.status === 'PENDING' && (
                      <Button variant="tonal" size="sm" onClick={() => setRecording(r)}>
                        Ghi kết quả
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Sheet open={recording !== null} onOpenChange={(open) => !open && setRecording(null)}>
        <SheetContent className="sm:max-w-lg">
          {recording && <ContactResultForm contact={recording} onSaved={saved} onFinal={() => load(status)} />}
        </SheetContent>
      </Sheet>
    </section>
  )
}

function ContactResultForm({ contact, onSaved, onFinal }) {
  const [result, setResult] = useState('CONTACTED')
  const [occurredAt, setOccurredAt] = useState(nowLocal)
  const [reason, setReason] = useState('')
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  // Một khóa cho cả form: bấm lại khi mạng chập chờn không ghi hai kết quả
  const key = useRef(crypto.randomUUID())

  async function save() {
    setFailure(null)
    if (result === 'UNREACHABLE' && !reason.trim()) {
      setErrors({ reason: 'Nhập lý do khi không liên hệ được' })
      return
    }
    setErrors({})
    setBusy(true)
    try {
      await postDirectContactsByIdResults({
        path: { id: contact.id },
        headers: { 'Idempotency-Key': key.current },
        body: {
          result,
          occurredAt: new Date(occurredAt).toISOString(),
          ...(reason.trim() && { reason: reason.trim() }),
          ...(note.trim() && { note: note.trim() }),
        },
      })
      onSaved()
    } catch (error) {
      const p = problemOf(error)
      if (error instanceof ApiError && error.status === 422) {
        setErrors({ reason: p.detail || p.title })
      } else {
        setFailure(p)
        if (error instanceof ApiError && error.status === 409) onFinal()
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{contact.fullName ?? 'Người thân'}</SheetTitle>
        <SheetDescription>{contact.shortContent}</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 overflow-y-auto px-4 pb-6">
        <FieldGroup>
          <Field>
            <FieldLabel>Kết quả</FieldLabel>
            <ToggleGroup
              variant="outline"
              value={[result]}
              onValueChange={(next) => next[0] && setResult(next[0])}
              className="flex-wrap"
            >
              <ToggleGroupItem value="CONTACTED" className="h-11 px-4">
                Đã liên hệ
              </ToggleGroupItem>
              <ToggleGroupItem value="UNREACHABLE" className="h-11 px-4">
                Không liên hệ được
              </ToggleGroupItem>
            </ToggleGroup>
          </Field>
          <Field>
            <FieldLabel htmlFor="contact-occurred-at">Thời điểm liên hệ</FieldLabel>
            <Input
              id="contact-occurred-at"
              type="datetime-local"
              className="h-11"
              value={occurredAt}
              onChange={(e) => setOccurredAt(e.target.value)}
            />
          </Field>
          <Field data-invalid={!!errors.reason || undefined}>
            <FieldLabel htmlFor="contact-reason">Lý do</FieldLabel>
            <Textarea
              id="contact-reason"
              aria-invalid={!!errors.reason || undefined}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            {errors.reason && <FieldError>{errors.reason}</FieldError>}
          </Field>
          <Field>
            <FieldLabel htmlFor="contact-note">Ghi chú</FieldLabel>
            <Textarea id="contact-note" value={note} onChange={(e) => setNote(e.target.value)} />
          </Field>
        </FieldGroup>
        <ProblemAlert problem={failure} />
        <Button className="h-11 self-start" disabled={busy} onClick={save}>
          Lưu kết quả
        </Button>
      </div>
    </>
  )
}
