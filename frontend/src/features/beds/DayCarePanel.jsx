import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getDayCareRestAreas,
  getDayCareSessions,
  postDayCareRestAreas,
  postDayCareSessions,
  getDayCareRestAreasRegistrationCounts,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { ProblemAlert } from '@/common/ProblemAlert'
import { PermissionDeniedState } from '@/common/PageState'
import { problemOf } from '@/common/problem'
import { formatDate } from '@/common/dateTime'

// T052: cấu hình và đếm lịch thật; quyền lệnh do BedsPage đọc từ backend.
export function DayCarePanel({ catalogs = {}, canCreate = false }) {
  const [state, setState] = useState({ loading: true })
  const [form, setForm] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [warnings, setWarnings] = useState([])
  const [dates, setDates] = useState({ from: '', to: '' })
  const [counts, setCounts] = useState({ rows: [] })
  const sequence = useRef(0)
  const load = useCallback(async () => {
    const [areas, sessions] = await Promise.all([getDayCareRestAreas(), getDayCareSessions()])
    return { areas: areas.data, sessions: sessions.data }
  }, [])
  useEffect(() => {
    let current = true
    load()
      .then((data) => {
        if (current) setState(data)
      })
      .catch((cause) => {
        if (current) setState({ problem: problemOf(cause) })
      })
    return () => {
      current = false
    }
  }, [load])
  async function save(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const response =
        form.kind === 'session'
          ? await postDayCareSessions({ body: { name: form.name, startTime: form.startTime, endTime: form.endTime } })
          : await postDayCareRestAreas({
              body: {
                code: form.code,
                name: form.name,
                [form.locationType === 'FLOOR' ? 'floorId' : 'areaId']: Number(form.locationId),
              },
            })
      setWarnings(response.data?.overCapacitySessions ?? [])
      setNotice('Đã lưu cấu hình. Lịch đến đã đăng ký được giữ nguyên.')
      setForm(null)
      setState(await load())
      sequence.current += 1
      setCounts({ rows: [] })
    } catch (cause) {
      setError(problemOf(cause))
    } finally {
      setBusy(false)
    }
  }
  async function lookup(event) {
    event.preventDefault()
    const current = ++sequence.current
    setCounts({ rows: [], loading: true })
    try {
      const response = await getDayCareRestAreasRegistrationCounts({ query: dates })
      if (current === sequence.current) setCounts({ rows: response.data })
    } catch (cause) {
      if (current === sequence.current) setCounts({ rows: [], problem: problemOf(cause) })
    }
  }
  if (state.loading) return <p aria-busy="true">Đang tải cấu hình bán trú…</p>
  if (state.problem)
    return state.problem.status === 403 ? (
      <PermissionDeniedState label="cấu hình bán trú" />
    ) : (
      <ProblemAlert problem={state.problem} />
    )
  const locationOptions = form?.locationType === 'AREA' ? (catalogs.areas ?? []) : (catalogs.floors ?? [])
  return (
    <section className="flex flex-col gap-6" aria-label="Bán trú">
      {notice && (
        <Alert>
          <AlertTitle>Kết quả thao tác</AlertTitle>
          <AlertDescription>{notice}</AlertDescription>
        </Alert>
      )}
      {error && <ProblemAlert problem={error} />}
      {warnings.length > 0 && (
        <Alert>
          <AlertTitle>Buổi vượt sức chứa</AlertTitle>
          <AlertDescription>
            {warnings.map((row) => (
              <p key={`${row.date}-${row.sessionId}`}>
                {row.sessionName} ngày {formatDate(row.date)}: {row.registrations} đăng ký, {row.capacity} chỗ. Không tự
                hủy lịch đến.
              </p>
            ))}
          </AlertDescription>
        </Alert>
      )}
      <section className="rounded-2xl border bg-card p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="type-title">Khu nghỉ bán trú</h2>
          {canCreate && !state.areas.some((row) => row.status === 'ACTIVE') && (
            <Button
              onClick={() => {
                setForm({ kind: 'area', code: '', name: '', locationType: 'FLOOR', locationId: '' })
                setError(null)
              }}
            >
              Khai báo khu nghỉ
            </Button>
          )}
        </div>
        {!state.areas.length && <p className="mt-3 text-muted-foreground">Chưa khai báo khu nghỉ.</p>}
        {state.areas.map((row) => (
          <div key={row.id} className="mt-3 flex flex-wrap items-center gap-3">
            <h3 className="type-label">{row.name}</h3>
            <span>{row.code}</span>
            <Badge variant="secondary">{row.status === 'ACTIVE' ? 'Hiệu lực' : 'Ngừng hiệu lực'}</Badge>
            <span>
              {row.capacityPerSession == null
                ? 'Sức chứa: viện chưa đặt CFG-M03-01'
                : `${row.capacityPerSession} chỗ mỗi buổi`}
            </span>
            <span className="text-muted-foreground">
              {row.floorId
                ? (catalogs.floors?.find((item) => item.id === row.floorId)?.name ?? `Tầng #${row.floorId}`)
                : (catalogs.areas?.find((item) => item.id === row.areaId)?.name ?? `Khu vực #${row.areaId}`)}
            </span>
          </div>
        ))}
      </section>
      <section className="rounded-2xl border bg-card p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="type-title">Danh mục buổi</h2>
          {canCreate && (
            <Button
              onClick={() => {
                setForm({ kind: 'session', name: '', startTime: '', endTime: '' })
                setError(null)
              }}
            >
              Thêm buổi
            </Button>
          )}
        </div>
        {!state.sessions.length && <p className="mt-3 text-muted-foreground">Chưa khai báo buổi.</p>}
        {state.sessions.map((row) => (
          <div key={row.id} className="mt-3 flex flex-wrap items-center gap-3">
            <h3 className="type-label">{row.name}</h3>
            <span>
              {row.startTime} – {row.endTime}
            </span>
            <Badge variant="secondary">{row.status === 'ACTIVE' ? 'Hiệu lực' : 'Ngừng hiệu lực'}</Badge>
          </div>
        ))}
      </section>
      {form && (
        <form
          onSubmit={save}
          className="flex flex-col gap-4 rounded-2xl border bg-card p-4"
          aria-label={form.kind === 'session' ? 'Khai báo buổi' : 'Khai báo khu nghỉ'}
        >
          <h2 className="type-title">{form.kind === 'session' ? 'Thêm buổi' : 'Khai báo khu nghỉ'}</h2>
          {form.kind === 'area' && (
            <label className="flex flex-col gap-2">
              Mã khu nghỉ
              <Input
                required
                maxLength={30}
                value={form.code}
                onChange={(event) => setForm({ ...form, code: event.target.value })}
              />
            </label>
          )}
          <label className="flex flex-col gap-2">
            {form.kind === 'session' ? 'Tên buổi' : 'Tên khu nghỉ'}
            <Input
              required
              maxLength={form.kind === 'session' ? 50 : 100}
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
            />
          </label>
          {form.kind === 'session' ? (
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex flex-col gap-2">
                Giờ bắt đầu
                <Input
                  type="time"
                  required
                  value={form.startTime}
                  onChange={(event) => setForm({ ...form, startTime: event.target.value })}
                />
              </label>
              <label className="flex flex-col gap-2">
                Giờ kết thúc
                <Input
                  type="time"
                  required
                  value={form.endTime}
                  min={form.startTime}
                  onChange={(event) => setForm({ ...form, endTime: event.target.value })}
                />
              </label>
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="flex flex-col gap-2">
                Gắn vào
                <select
                  value={form.locationType}
                  onChange={(event) => setForm({ ...form, locationType: event.target.value, locationId: '' })}
                  className="rounded-md border bg-background p-2"
                >
                  <option value="FLOOR">Tầng</option>
                  <option value="AREA">Khu vực</option>
                </select>
              </label>
              <label className="flex flex-col gap-2">
                Vị trí
                <select
                  required
                  value={form.locationId}
                  onChange={(event) => setForm({ ...form, locationId: event.target.value })}
                  className="rounded-md border bg-background p-2"
                >
                  <option value="">Chọn vị trí</option>
                  {locationOptions
                    .filter((row) => row.status === 'ACTIVE')
                    .map((row) => (
                      <option key={row.id} value={row.id}>
                        {row.name}
                      </option>
                    ))}
                </select>
              </label>
            </div>
          )}
          <div className="flex gap-2">
            <Button type="submit" disabled={busy}>
              {busy ? 'Đang lưu…' : form.kind === 'session' ? 'Lưu buổi' : 'Lưu khu nghỉ'}
            </Button>
            <Button type="button" variant="outline" disabled={busy} onClick={() => setForm(null)}>
              Hủy
            </Button>
          </div>
        </form>
      )}
      <section className="rounded-2xl border bg-card p-4">
        <h2 className="type-title">Số đăng ký theo buổi</h2>
        <form onSubmit={lookup} className="mt-4 flex flex-wrap items-end gap-4">
          <label className="flex flex-col gap-2">
            Từ ngày
            <Input
              type="date"
              required
              value={dates.from}
              onChange={(event) => {
                sequence.current += 1
                setDates({ ...dates, from: event.target.value })
                setCounts({ rows: [] })
              }}
            />
          </label>
          <label className="flex flex-col gap-2">
            Đến ngày
            <Input
              type="date"
              required
              min={dates.from}
              value={dates.to}
              onChange={(event) => {
                sequence.current += 1
                setDates({ ...dates, to: event.target.value })
                setCounts({ rows: [] })
              }}
            />
          </label>
          <Button type="submit" disabled={counts.loading}>
            Tra cứu số đăng ký
          </Button>
        </form>
        {counts.loading && <p aria-busy="true">Đang đếm lịch đăng ký…</p>}
        {counts.problem && <ProblemAlert problem={counts.problem} />}
        {counts.rows.length > 0 && (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left type-body-sm">
              <thead>
                <tr>
                  <th className="p-2">Ngày</th>
                  <th className="p-2">Buổi</th>
                  <th className="p-2">Đăng ký</th>
                  <th className="p-2">Sức chứa</th>
                  <th className="p-2">Còn lại</th>
                </tr>
              </thead>
              <tbody>
                {counts.rows.map((row) => (
                  <tr key={`${row.date}-${row.sessionId}`} className="border-t">
                    <td className="p-2">{formatDate(row.date)}</td>
                    <td className="p-2">{row.sessionName}</td>
                    <td className="p-2">{row.registrations}</td>
                    <td className="p-2">{row.capacity}</td>
                    <td className="p-2">{row.remaining < 0 ? `Vượt ${-row.remaining} chỗ` : row.remaining}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </section>
  )
}
