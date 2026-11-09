import { useCallback, useEffect, useState } from 'react'
import { getBalanceStatuses, getResidentsById, postResidentsByResidentIdBalanceStopReminders } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { BALANCE_STATUS, balanceText } from './amounts'

// Tình trạng số dư (017 T022; US5; FR-016 → FR-020; BR-M11-13): danh sách Sắp hết tiền, Còn nợ do job hằng ngày và
// lần ghi tiền nộp tính (không tự tính ở web, Q-306). Quản lý viện ghi "dừng nhắc" kèm lý do cho hồ sơ trạng thái cuối
// còn nợ (chú thích ³²); backend chặn hồ sơ chưa ở trạng thái cuối. Màn gắn vào trang Số dư, thu chi (B) như một phần.

const FILTERS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'RUNNING_LOW', label: BALANCE_STATUS.RUNNING_LOW },
  { value: 'IN_DEBT', label: BALANCE_STATUS.IN_DEBT },
]

function StopReminders({ row, name, onClose, onStopped }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [failure, setFailure] = useState(null)
  const [saving, setSaving] = useState(false)
  const [key] = useState(() => crypto.randomUUID())

  async function submit() {
    if (!reason.trim()) {
      setError('Nhập lý do dừng nhắc')
      return
    }
    setError('')
    setSaving(true)
    try {
      await postResidentsByResidentIdBalanceStopReminders({
        path: { residentId: row.residentId },
        headers: { 'Idempotency-Key': key },
        body: { reason: reason.trim() },
      })
      onStopped()
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Dừng nhắc còn nợ của {name}</DialogTitle>
          <DialogDescription>
            Chỉ cho hồ sơ đã kết thúc lưu trú, qua đời hoặc hủy tiếp nhận mà còn nợ. Người đại diện, Kế toán không còn
            được nhắc; thông báo khi có khoản nộp vẫn gửi.
          </DialogDescription>
        </DialogHeader>
        <Field data-invalid={!!error || undefined}>
          <FieldLabel htmlFor="stop-reason">Lý do</FieldLabel>
          <Textarea
            id="stop-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            aria-invalid={!!error || undefined}
            rows={3}
          />
          {error && <FieldError>{error}</FieldError>}
        </Field>
        <ProblemAlert problem={failure} />
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            Quay lại
          </Button>
          <Button onClick={submit} disabled={saving}>
            Dừng nhắc
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function BalanceStatuses() {
  const { can } = usePermissions()
  const [filter, setFilter] = useState('ALL')
  const [state, setState] = useState({ status: 'loading' })
  const [names, setNames] = useState({})
  const [stopping, setStopping] = useState(null)

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const { data } = await getBalanceStatuses({ query: { status: filter === 'ALL' ? undefined : filter } })
      setState({ status: 'ready', rows: data })
      const entries = await Promise.all(
        data.map((r) =>
          getResidentsById({ path: { id: r.residentId } })
            .then(({ data: p }) => [r.residentId, p?.fullName])
            .catch(() => [r.residentId, null]),
        ),
      )
      setNames(Object.fromEntries(entries))
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [filter])

  useEffect(() => {
    // Tải lại khi đổi bộ lọc; trạng thái đặt trong hàm tải
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  const nameOf = (r) => names[r.residentId] ?? `Mã hồ sơ ${r.residentId}`

  return (
    <div className="flex flex-col gap-5">
      <ToggleGroup variant="filter" value={[filter]} onValueChange={(next) => next[0] && setFilter(next[0])}>
        {FILTERS.map((f) => (
          <ToggleGroupItem key={f.value} value={f.value}>
            {f.label}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <Card>
        <CardContent>
          {state.status === 'loading' && <Skeleton className="h-32 w-full" />}
          {state.status === 'error' && <ProblemAlert problem={state.problem} />}
          {state.status === 'ready' && state.rows.length === 0 && (
            <Empty>
              <EmptyHeader>
                <EmptyTitle>Không có hồ sơ Sắp hết tiền, Còn nợ</EmptyTitle>
                <EmptyDescription>
                  Tình trạng được tính hằng ngày lúc 02:00 và ngay khi ghi nhận tiền nộp.
                </EmptyDescription>
              </EmptyHeader>
            </Empty>
          )}
          {state.status === 'ready' && state.rows.length > 0 && (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Người cao tuổi</TableHead>
                  <TableHead>Tình trạng</TableHead>
                  <TableHead>Số dư</TableHead>
                  <TableHead>Số ngày còn đủ tiền</TableHead>
                  <TableHead>Mã nộp tiền</TableHead>
                  <TableHead>
                    <span className="sr-only">Lệnh</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {state.rows.map((r) => (
                  <TableRow key={r.residentId}>
                    <TableCell className="type-label">{nameOf(r)}</TableCell>
                    <TableCell>
                      {BALANCE_STATUS[r.status]}
                      {r.remindersStopped && <span className="type-caption text-muted-foreground">, đã dừng nhắc</span>}
                    </TableCell>
                    <TableCell className="tabular-nums">{balanceText(r.balance)}</TableCell>
                    <TableCell className="tabular-nums">
                      {r.daysCovered === null || r.daysCovered === undefined ? '—' : `${r.daysCovered} ngày`}
                    </TableCell>
                    <TableCell className="tabular-nums">{r.paymentCode}</TableCell>
                    <TableCell>
                      {can('BALANCE_LEDGER', 'APPROVE') && r.status === 'IN_DEBT' && !r.remindersStopped && (
                        <Button
                          variant="outline"
                          size="sm"
                          aria-label={`Dừng nhắc ${nameOf(r)}`}
                          onClick={() => setStopping(r)}
                        >
                          Dừng nhắc
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      {stopping && (
        <StopReminders
          row={stopping}
          name={nameOf(stopping)}
          onClose={() => setStopping(null)}
          onStopped={() => {
            setStopping(null)
            load()
          }}
        />
      )}
    </div>
  )
}
