import { useState } from 'react'
import { postTripsByIdByCommand, postTripsByIdEligibilityAssessments } from '@/api'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { ActivityField } from './ActivityField'
import { CommandSheet } from './ActivitySessionForms'
import { toOffsetIso } from './sessionView'

// T018 (bố cục duyệt 2026-11-05): đánh giá Đạt/Không đạt; chưa có nguồn thuốc, sức khỏe nên ghi rõ, không suy ra an toàn.
export function TripAssessmentSheet({ tripId, participant, name, onOpenChange, onDone }) {
  const [result, setResult] = useState('')
  const [reason, setReason] = useState('')
  return (
    <CommandSheet
      open={Boolean(participant)}
      onOpenChange={onOpenChange}
      title="Đánh giá khả năng tham gia"
      description={`${name}. Chưa có dữ liệu thuốc, sức khỏe và liên hệ trên màn này; người đánh giá tự kiểm tra ở nguồn chuyên môn.`}
      submitLabel="Lưu đánh giá"
      ready={result === 'PASS' || (result === 'FAIL' && reason.trim())}
      onSubmit={async (headers) => {
        const body = { residentId: participant.residentId, result, ...(reason.trim() ? { reason: reason.trim() } : {}) }
        await postTripsByIdEligibilityAssessments({ path: { id: tripId }, body, headers })
        onDone('Đã lưu đánh giá khả năng tham gia')
      }}
    >
      <fieldset className="flex flex-col gap-2">
        <legend className="type-label">Kết quả (bắt buộc)</legend>
        <div className="flex flex-wrap gap-2">
          {[
            ['PASS', 'Đạt'],
            ['FAIL', 'Không đạt'],
          ].map(([value, text]) => (
            <label key={value} className="flex min-h-11 items-center gap-2 rounded-full border px-4">
              <input type="radio" name="assessment" checked={result === value} onChange={() => setResult(value)} />
              {text}
            </label>
          ))}
        </div>
      </fieldset>
      {result === 'FAIL' && (
        <ActivityField
          label="Lý do không đạt"
          type="textarea"
          value={reason}
          onChange={setReason}
          required
          maxLength={2000}
        />
      )}
    </CommandSheet>
  )
}

// T020 (bố cục duyệt 2026-11-07): chuyến Đang đi; điểm danh về, gia hạn, chuyển trưởng đoàn, kết thúc điểm danh về.
// Báo thiếu người chưa hiện: chờ port ghi sự cố của 007.
export function TripReturnPanel({ trip, nameOf, onDone }) {
  const [selected, setSelected] = useState([])
  const [sheet, setSheet] = useState(null)
  const [extend, setExtend] = useState({ at: '', reason: '' })
  const [transfer, setTransfer] = useState({ to: '', reason: '' })
  const [finishing, setFinishing] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(false)
  const [keys] = useState(() => new Map())
  const out = trip.participants.filter((p) => p.status === 'DEPARTED')
  const overdue = new Date(trip.plannedReturnAt) < new Date()
  const send = async (command, body, message) => {
    const signature = `${command}:${JSON.stringify(body)}`
    if (!keys.has(signature)) keys.set(signature, crypto.randomUUID())
    await postTripsByIdByCommand({
      path: { id: trip.id, command },
      body,
      headers: { 'Idempotency-Key': keys.get(signature) },
    })
    keys.delete(signature)
    setSheet(null)
    setSelected([])
    onDone(message)
  }
  const direct = async (command, body, message) => {
    setBusy(true)
    setProblem(null)
    try {
      await send(command, body, message)
      return true
    } catch (error) {
      setProblem(problemOf(error))
      return false
    } finally {
      setBusy(false)
    }
  }
  return (
    <section className="flex flex-col gap-3 border-t pt-4">
      <h3 className="type-card-title">Đang đi</h3>
      <p className="type-body-sm tabular-nums">
        Về dự kiến {formatDateTime(trip.plannedReturnAt)}
        {overdue && (
          <>
            , <span className="text-destructive">Quá giờ về dự kiến</span>
          </>
        )}
      </p>
      <div className="flex flex-wrap gap-2">
        <Button variant="outline" size="lg" onClick={() => setSheet('extend')}>
          Gia hạn giờ về
        </Button>
        <Button variant="outline" size="lg" onClick={() => setSheet('transfer')}>
          Chuyển trưởng đoàn
        </Button>
      </div>
      {out.length === 0 ? (
        <p className="type-body-sm">Không còn người đang ở ngoài.</p>
      ) : (
        <ul className="flex flex-col gap-2" aria-label="Người đang ở ngoài">
          {out.map((p) => (
            <li key={p.id}>
              <label className="flex min-h-11 items-center gap-3">
                <input
                  type="checkbox"
                  className="size-5"
                  disabled={busy}
                  aria-label={`Chọn về ${nameOf(p.residentId)}`}
                  checked={selected.includes(p.residentId)}
                  onChange={(e) =>
                    setSelected((ids) =>
                      e.target.checked ? [...ids, p.residentId] : ids.filter((id) => id !== p.residentId),
                    )
                  }
                />
                <span className="type-label">{nameOf(p.residentId)}</span>
              </label>
            </li>
          ))}
        </ul>
      )}
      <ProblemAlert problem={problem} />
      <div className="flex flex-wrap justify-end gap-2 border-t pt-4">
        <Button variant="outline" size="lg" disabled={busy} onClick={() => setFinishing(true)}>
          Kết thúc điểm danh về
        </Button>
        <Button
          size="lg"
          disabled={busy || selected.length === 0}
          onClick={() => direct('return', { residentIds: [...selected].sort((a, b) => a - b) }, 'Đã điểm danh về')}
        >
          Điểm danh về ({selected.length})
        </Button>
      </div>
      <CommandSheet
        open={sheet === 'extend'}
        onOpenChange={(open) => setSheet(open ? 'extend' : null)}
        title="Gia hạn giờ về"
        description={`Giờ về dự kiến hiện tại ${formatDateTime(trip.plannedReturnAt)}. Giờ mới phải sau giờ hiện tại.`}
        submitLabel="Lưu gia hạn"
        ready={extend.at && extend.reason.trim()}
        onSubmit={() =>
          send(
            'extend',
            { newPlannedReturnAt: toOffsetIso(extend.at), reason: extend.reason.trim() },
            'Đã gia hạn giờ về',
          )
        }
      >
        <ActivityField
          label="Giờ về mới"
          type="datetime-local"
          value={extend.at}
          onChange={(at) => setExtend((f) => ({ ...f, at }))}
          required
        />
        <ActivityField
          label="Lý do gia hạn"
          type="textarea"
          value={extend.reason}
          onChange={(reason) => setExtend((f) => ({ ...f, reason }))}
          required
          maxLength={2000}
        />
      </CommandSheet>
      <CommandSheet
        open={sheet === 'transfer'}
        onOpenChange={(open) => setSheet(open ? 'transfer' : null)}
        title="Chuyển trưởng đoàn"
        description="Người nhận là người đi cùng có vai trò Trưởng tầng, Điều dưỡng hoặc Nhân viên chăm sóc; trưởng đoàn cũ thành người đi cùng."
        submitLabel="Xác nhận chuyển trưởng đoàn"
        ready={transfer.to && transfer.reason.trim()}
        onSubmit={() =>
          send(
            'transfer-leader',
            { newLeaderId: Number(transfer.to), reason: transfer.reason.trim() },
            'Đã chuyển trưởng đoàn',
          )
        }
      >
        <ActivityField
          label="Người nhận"
          options={trip.companionIds.map((id) => [String(id), `Nhân viên ${id}`])}
          value={transfer.to}
          onChange={(to) => setTransfer((f) => ({ ...f, to }))}
          required
        />
        <ActivityField
          label="Lý do chuyển"
          type="textarea"
          value={transfer.reason}
          onChange={(reason) => setTransfer((f) => ({ ...f, reason }))}
          required
          maxLength={2000}
        />
      </CommandSheet>
      <Dialog open={finishing} onOpenChange={setFinishing}>
        <DialogContent>
          <DialogTitle>Kết thúc điểm danh về</DialogTitle>
          <DialogDescription>
            {out.length === 0
              ? 'Mọi người đã về. Chuyến chuyển sang Đã về.'
              : `${out.map((p) => nameOf(p.residentId)).join(', ')} sẽ chuyển sang Thiếu khi về và hệ thống gửi cảnh báo Khẩn cấp.`}
          </DialogDescription>
          <ProblemAlert problem={problem} />
          <div className="flex flex-wrap justify-end gap-2">
            <Button variant="outline" disabled={busy} onClick={() => setFinishing(false)}>
              Quay lại
            </Button>
            <Button
              disabled={busy}
              onClick={async () => {
                if (await direct('finish-return', {}, 'Đã kết thúc điểm danh về')) setFinishing(false)
              }}
            >
              Xác nhận kết thúc
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </section>
  )
}
