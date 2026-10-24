import { useCallback, useEffect, useState } from 'react'
import { getNotificationHistory } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { CHANNEL_LABELS, LEVEL_LABELS, RESULT_LABELS, basisLabel, label, recipientLabel } from './notificationLabels'

// Lịch sử gửi (009 T035; FR-037): Quản lý viện toàn viện, Trưởng tầng các tầng được giao (backend lọc). Lượt nhắc gọi
// hiện khi có 009 US1.

const EMPTY_FILTER = { from: '', to: '', sourceRecordType: '', sourceRecordId: '' }

// datetime-local (giờ máy) → ISO có múi giờ cho tham số OffsetDateTime
const toIso = (local) => (local ? new Date(local).toISOString() : undefined)

function channelText(c) {
  return `${label(CHANNEL_LABELS, c.channel)}: ${c.result ? label(RESULT_LABELS, c.result) : 'Đang gửi'}`
}

export function NotificationHistory() {
  const [filter, setFilter] = useState(EMPTY_FILTER)
  const [state, setState] = useState({ status: 'loading', rows: [] })
  const [opened, setOpened] = useState(null)

  const load = useCallback(async (f) => {
    setState({ status: 'loading', rows: [] })
    try {
      const { data } = await getNotificationHistory({
        query: {
          from: toIso(f.from),
          to: toIso(f.to),
          sourceRecordType: f.sourceRecordType || undefined,
          sourceRecordId: f.sourceRecordId || undefined,
        },
      })
      setState({ status: 'ready', rows: data })
    } catch {
      setState({ status: 'error', rows: [] })
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải khi mở tab
    load(EMPTY_FILTER)
  }, [load])

  const set = (key) => (e) => setFilter((f) => ({ ...f, [key]: e.target.value }))

  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
      <form
        className="grid gap-3 md:grid-cols-[repeat(4,minmax(0,14rem))_auto] md:items-end"
        onSubmit={(e) => {
          e.preventDefault()
          load(filter)
        }}
      >
        <Field>
          <FieldLabel htmlFor="history-from">Từ</FieldLabel>
          <Input id="history-from" type="datetime-local" value={filter.from} onChange={set('from')} />
        </Field>
        <Field>
          <FieldLabel htmlFor="history-to">Đến</FieldLabel>
          <Input id="history-to" type="datetime-local" value={filter.to} onChange={set('to')} />
        </Field>
        <Field>
          <FieldLabel htmlFor="history-source-type">Loại bản ghi nguồn</FieldLabel>
          <Input id="history-source-type" value={filter.sourceRecordType} onChange={set('sourceRecordType')} />
        </Field>
        <Field>
          <FieldLabel htmlFor="history-source-id">Mã bản ghi nguồn</FieldLabel>
          <Input id="history-source-id" value={filter.sourceRecordId} onChange={set('sourceRecordId')} />
        </Field>
        <Button type="submit" variant="outline" className="h-10">
          Lọc
        </Button>
      </form>

      {state.status === 'loading' && (
        <div aria-busy="true" className="flex flex-col gap-2">
          <span className="sr-only">Đang tải lịch sử</span>
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </div>
      )}
      {state.status === 'error' && (
        <Alert role="alert">
          <AlertTitle>Không tải được lịch sử gửi</AlertTitle>
          <AlertAction>
            <Button variant="outline" size="sm" onClick={() => load(filter)}>
              Thử lại
            </Button>
          </AlertAction>
        </Alert>
      )}
      {state.status === 'ready' && state.rows.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>Không có thông báo nào trong khoảng này</EmptyTitle>
            <EmptyDescription>Đổi khoảng thời gian hoặc bản ghi nguồn rồi lọc lại.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {state.status === 'ready' && state.rows.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tiếp nhận</TableHead>
              <TableHead>Nguồn</TableHead>
              <TableHead>Người nhận, căn cứ</TableHead>
              <TableHead>Mức</TableHead>
              <TableHead className="min-w-56">Kênh, kết quả</TableHead>
              <TableHead>Gửi, xem</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {state.rows.map((r, i) => (
              <TableRow key={`${r.requestId}-${r.recipient}-${i}`} className="align-top">
                <TableCell className="tabular-nums">
                  <Button variant="link" className="h-auto p-0 tabular-nums" onClick={() => setOpened(r)}>
                    {formatDateTime(r.receivedAt)}
                  </Button>
                </TableCell>
                <TableCell className="whitespace-normal">{`${r.sourceRecord.type} ${r.sourceRecord.id}`}</TableCell>
                <TableCell className="whitespace-normal">
                  {recipientLabel(r.recipient)}
                  <span className="block type-caption text-muted-foreground">{r.basis.map(basisLabel).join(', ')}</span>
                </TableCell>
                <TableCell>
                  <StatusBadge status={label(LEVEL_LABELS, r.level)} />
                </TableCell>
                <TableCell className="whitespace-normal">
                  {r.channels.length === 0 ? (
                    <span className="text-muted-foreground">Chưa có lần gửi</span>
                  ) : (
                    r.channels.map((c, j) => (
                      <span key={j} className="block">
                        {channelText(c)}
                      </span>
                    ))
                  )}
                </TableCell>
                <TableCell className="whitespace-normal tabular-nums">
                  {r.sentAt ? formatDateTime(r.sentAt) : 'Chưa gửi'}
                  <span className="block type-caption text-muted-foreground">
                    {r.viewedAt ? `Xem ${formatDateTime(r.viewedAt)}` : 'Chưa xem'}
                  </span>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Sheet open={opened !== null} onOpenChange={(open) => !open && setOpened(null)}>
        <SheetContent className="sm:max-w-lg">{opened && <HistoryDetail row={opened} />}</SheetContent>
      </Sheet>
    </section>
  )
}

function HistoryDetail({ row: r }) {
  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{recipientLabel(r.recipient)}</SheetTitle>
        <SheetDescription>{`${r.sourceRecord.type} ${r.sourceRecord.id}, yêu cầu ${r.requestId}`}</SheetDescription>
      </SheetHeader>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 px-4 pb-6 type-body-sm">
        <dt className="text-muted-foreground">Mức</dt>
        <dd>{label(LEVEL_LABELS, r.level)}</dd>
        <dt className="text-muted-foreground">Căn cứ</dt>
        <dd>{r.basis.map(basisLabel).join(', ')}</dd>
        <dt className="text-muted-foreground">Tiếp nhận</dt>
        <dd className="tabular-nums">{formatDateTime(r.receivedAt)}</dd>
        <dt className="text-muted-foreground">Gửi</dt>
        <dd className="tabular-nums">{r.sentAt ? formatDateTime(r.sentAt) : 'Chưa gửi'}</dd>
        <dt className="text-muted-foreground">Xem</dt>
        <dd className="tabular-nums">{r.viewedAt ? formatDateTime(r.viewedAt) : 'Chưa xem'}</dd>
        <dt className="text-muted-foreground">Xác nhận</dt>
        <dd className="tabular-nums">{r.acknowledgedAt ? formatDateTime(r.acknowledgedAt) : 'Chưa xác nhận'}</dd>
        <dt className="text-muted-foreground">Kênh</dt>
        <dd>
          {r.channels.length === 0
            ? 'Chưa có lần gửi'
            : r.channels.map((c, j) => (
                <span key={j} className="block">
                  {`${channelText(c)}${c.occurredAt ? `, ${formatDateTime(c.occurredAt)}` : ''}`}
                </span>
              ))}
        </dd>
        {r.q354Case && (
          <>
            <dt className="text-muted-foreground">Trường hợp ngoài phạm vi đồng ý</dt>
            <dd>{r.q354Case}</dd>
          </>
        )}
        {r.flags.length > 0 && (
          <>
            <dt className="text-muted-foreground">Dấu</dt>
            <dd>{r.flags.join(', ')}</dd>
          </>
        )}
      </dl>
    </>
  )
}
