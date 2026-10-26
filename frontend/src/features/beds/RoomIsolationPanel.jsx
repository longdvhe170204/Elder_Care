import { useEffect, useRef, useState } from 'react'
import { getRoomsByIdIsolationHistory, postRoomsByIdIsolate, postRoomsByIdLiftIsolation } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'

// T056: quyền truyền từ /me/permissions; chỉ hai lệnh đã duyệt, không sửa danh mục phòng.
export function RoomIsolationPanel({ rooms = [], canIsolate = false, canLift = false, onChanged = () => {} }) {
  const [id, setId] = useState('')
  const [reason, setReason] = useState('')
  const [incident, setIncident] = useState('')
  const [history, setHistory] = useState({ rows: [] })
  const [revision, setRevision] = useState(0)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [busy, setBusy] = useState(false)
  const pending = useRef(false)
  const command = useRef(null)
  const room = rooms.find((row) => row.id === Number(id))
  useEffect(() => {
    if (!id) return
    let current = true
    getRoomsByIdIsolationHistory({ path: { id } })
      .then((response) => {
        if (current) setHistory({ rows: response.data })
      })
      .catch((cause) => {
        if (current) setHistory({ rows: [], error: problemOf(cause) })
      })
    return () => {
      current = false
    }
  }, [id, revision])
  function choose(value) {
    setId(value)
    setHistory({ rows: [], loading: Boolean(value) })
    setReason('')
    setIncident('')
    setError(null)
    setNotice(null)
    command.current = null
  }
  async function submit(event) {
    event.preventDefault()
    if (pending.current || !room || !reason.trim()) return
    const body = { reason: reason.trim(), ...(!room.isolated && incident && { incidentId: Number(incident) }) }
    const signature = JSON.stringify({ id, isolated: room.isolated, body })
    if (command.current?.signature !== signature) command.current = { signature, key: crypto.randomUUID() }
    pending.current = true
    setBusy(true)
    setError(null)
    try {
      const action = room.isolated ? postRoomsByIdLiftIsolation : postRoomsByIdIsolate
      await action({ path: { id }, body, headers: { 'Idempotency-Key': command.current.key } })
      setNotice(room.isolated ? 'Đã gỡ cách ly phòng.' : 'Đã đặt cách ly phòng.')
      setReason('')
      setIncident('')
      command.current = null
      await onChanged()
      setRevision((value) => value + 1)
    } catch (cause) {
      setError(problemOf(cause))
    } finally {
      pending.current = false
      setBusy(false)
    }
  }
  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4" aria-label="Cách ly phòng">
      <h2 className="type-title">Cách ly phòng</h2>
      <label className="flex flex-col gap-2">
        Phòng
        <select
          className="h-11 rounded-md border border-input bg-background px-3"
          disabled={busy}
          value={id}
          onChange={(event) => choose(event.target.value)}
        >
          <option value="">Chọn phòng</option>
          {rooms.map((row) => (
            <option key={row.id} value={row.id}>
              {row.name} ({row.code})
            </option>
          ))}
        </select>
      </label>
      {!rooms.length && <p>Chưa có phòng trong danh sách được xem.</p>}
      {notice && (
        <Alert role="status">
          <AlertTitle>Kết quả thao tác</AlertTitle>
          <AlertDescription>{notice}</AlertDescription>
        </Alert>
      )}
      {error && <ProblemAlert problem={error} />}
      {room && (
        <>
          <div className="flex flex-wrap gap-3">
            <Badge variant="secondary">{room.isolated ? 'Đang cách ly' : 'Không đặt cách ly phòng'}</Badge>
            {room.isolationBlocked && !room.isolated && <p>Phòng vẫn chịu chặn do thuộc vùng khoanh.</p>}
          </div>
          {((room.isolated && canLift) || (!room.isolated && canIsolate)) && (
            <form onSubmit={submit} className="flex flex-col gap-4">
              <label className="flex flex-col gap-2">
                Lý do
                <textarea
                  required
                  maxLength={2000}
                  disabled={busy}
                  className="min-h-24 rounded-md border border-input bg-background p-3"
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                />
              </label>
              {!room.isolated && (
                <label className="flex flex-col gap-2">
                  Mã sự cố liên quan (nếu có)
                  <Input
                    type="number"
                    min="1"
                    step="1"
                    disabled={busy}
                    value={incident}
                    onChange={(event) => setIncident(event.target.value)}
                  />
                </label>
              )}
              <Button type="submit" className="self-start" disabled={busy || !reason.trim()}>
                {busy ? 'Đang lưu…' : room.isolated ? 'Gỡ cách ly' : 'Đặt cách ly'}
              </Button>
            </form>
          )}
          <h3 className="type-label">Lịch sử đặt, gỡ cách ly</h3>
          {history.loading && <Skeleton className="h-24 w-full" />}
          {history.error && (
            <ProblemAlert problem={history.error}>
              <Button
                variant="outline"
                onClick={() => {
                  setHistory({ rows: [], loading: true })
                  setRevision((value) => value + 1)
                }}
              >
                Thử lại
              </Button>
            </ProblemAlert>
          )}
          {!history.loading && !history.error && !history.rows.length && <p>Chưa có lần đặt/gỡ cách ly.</p>}
          {history.rows.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left type-body-sm">
                <thead>
                  <tr>
                    <th className="p-2">Thời điểm</th>
                    <th className="p-2">Thao tác</th>
                    <th className="p-2">Người thực hiện</th>
                    <th className="p-2">Lý do</th>
                    <th className="p-2">Sự cố</th>
                  </tr>
                </thead>
                <tbody>
                  {history.rows.map((row, index) => (
                    <tr className="border-t" key={`${row.occurredAt}-${index}`}>
                      <td className="p-2">{formatDateTime(row.occurredAt)}</td>
                      <td className="p-2">{row.action === 'ISOLATE' ? 'Đặt cách ly' : 'Gỡ cách ly'}</td>
                      <td className="p-2">{row.actor ?? 'Chưa có tên hiển thị'}</td>
                      <td className="break-words p-2">{row.reason}</td>
                      <td className="p-2">{row.incidentId ?? 'Không ghi'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </section>
  )
}
