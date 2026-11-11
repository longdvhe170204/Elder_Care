import { useEffect, useState } from 'react'
import {
  ApiError,
  getFamilyPortalDocumentsByTypeByIdFile,
  getFamilyPortalResidentsByResidentIdDocuments,
  postFamilyPortalResidentsByResidentIdBalanceStatement,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { PortalCard } from '../PortalParts'

// 012 T073 (FR-046 (3), Q-377 → Q-379; 017 FR-026, FR-027, FR-037, FR-038): chứng từ trên cổng — phiếu báo phí, bảng
// quyết toán tải đúng file đã lưu; sao kê số dư theo khoảng ngày (Excel, PDF). Không có quyền xem chi phí (403) thì ẩn
// cả mục; phần số dư phía trên đã báo. Cùng chữ với app người thân (FamilyDocumentsScreen).

const TYPES = { PAYMENT_NOTICE: 'Phiếu báo phí', SETTLEMENT_STATEMENT: 'Bảng quyết toán' }
const EXT = { PDF: 'pdf', XLSX: 'xlsx' }

function save(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export function LiveDocuments({ residentId }) {
  const [state, setState] = useState({ status: 'loading' })
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [range, setRange] = useState({ from: '', to: '', format: 'PDF' })

  useEffect(() => {
    let cancelled = false
    getFamilyPortalResidentsByResidentIdDocuments({ path: { residentId } })
      .then(({ data }) => !cancelled && setState({ status: 'ready', data }))
      .catch((error) => !cancelled && setState({ status: 'error', error }))
    return () => {
      cancelled = true
    }
  }, [residentId])

  async function run(action) {
    setFailure(null)
    setBusy(true)
    try {
      await action()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  const download = (d, format) =>
    run(async () => {
      const { data } = await getFamilyPortalDocumentsByTypeByIdFile({
        path: { type: d.type, id: d.id },
        query: { format },
        responseType: 'blob',
      })
      save(data, `${d.code}.${EXT[format]}`)
    })

  const statement = () =>
    run(async () => {
      const { data } = await postFamilyPortalResidentsByResidentIdBalanceStatement({
        path: { residentId },
        body: range,
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        responseType: 'blob',
      })
      save(data, `sao-ke-${range.from}-${range.to}.${EXT[range.format]}`)
    })

  if (state.status === 'error' && state.error instanceof ApiError && state.error.status === 403) return null
  if (state.status === 'loading') return <Skeleton className="h-32 w-full" />

  return (
    <>
      <PortalCard title="Phiếu báo phí và bảng quyết toán">
        {state.status === 'error' ? (
          <ProblemAlert problem={problemOf(state.error)} />
        ) : state.data.length === 0 ? (
          <p className="text-muted-foreground">Chưa có phiếu báo phí hay bảng quyết toán nào.</p>
        ) : (
          <ul className="flex flex-col">
            {state.data.map((d) => (
              <li
                key={`${d.type}-${d.id}`}
                className="flex flex-wrap items-center justify-between gap-2 border-t py-2 first:border-t-0 first:pt-0"
              >
                <span className="flex flex-col gap-0.5">
                  <span>
                    {TYPES[d.type] ?? d.type} {d.code}
                  </span>
                  <span className="type-caption text-muted-foreground">
                    {formatDate(d.date)}
                    {d.period && `, kỳ ${d.period}`}
                  </span>
                </span>
                <span className="flex gap-2">
                  {d.formats.map((format) => (
                    <Button
                      key={format}
                      variant="outline"
                      size="sm"
                      disabled={busy}
                      aria-label={`Tải ${d.code} dạng ${format}`}
                      onClick={() => download(d, format)}
                    >
                      Tải {format}
                    </Button>
                  ))}
                </span>
              </li>
            ))}
          </ul>
        )}
      </PortalCard>
      <PortalCard title="Sao kê số dư">
        <p className="type-caption text-muted-foreground">
          Khoảng ngày không dài hơn giới hạn của viện (tối đa 12 tháng).
        </p>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="statement-from">Từ ngày</Label>
            <Input
              id="statement-from"
              type="date"
              value={range.from}
              onChange={(e) => setRange({ ...range, from: e.target.value })}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="statement-to">Đến ngày</Label>
            <Input
              id="statement-to"
              type="date"
              value={range.to}
              onChange={(e) => setRange({ ...range, to: e.target.value })}
            />
          </div>
        </div>
        <div className="flex flex-wrap gap-2" role="group" aria-label="Định dạng sao kê">
          {[
            ['PDF', 'PDF'],
            ['XLSX', 'Excel'],
          ].map(([value, label]) => (
            <Button
              key={value}
              variant={range.format === value ? 'default' : 'outline'}
              size="sm"
              aria-pressed={range.format === value}
              onClick={() => setRange({ ...range, format: value })}
            >
              {label}
            </Button>
          ))}
        </div>
        {failure && <ProblemAlert problem={failure} />}
        <Button className="self-start" disabled={busy || !range.from || !range.to} onClick={statement}>
          Tải sao kê
        </Button>
      </PortalCard>
    </>
  )
}
