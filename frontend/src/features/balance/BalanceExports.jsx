import { useRef, useState } from 'react'
import { X } from 'lucide-react'
import { getBalanceExportsByIdFile, postBalanceExportsIncomeExpenseReport, postBalanceExportsStatement } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate, formatDateTime } from '@/common/dateTime'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { SelectField, TextField } from './BalanceFields'
import { ResidentPicker } from './ResidentPicker'
import { saveBlob } from './download'

// Xuất sao kê và báo cáo thu chi (017 T039; FR-025, FR-026, FR-027, Q-381, Q-386): Kế toán xuất Excel hoặc PDF; file
// dựng một lần, tải lại đúng file đã dựng. Quản lý viện xem được các nội dung này nhưng không xuất (Phụ lục 27).

const TYPE_LABELS = { BALANCE_STATEMENT: 'Sao kê số dư', INCOME_EXPENSE: 'Báo cáo thu chi' }
const FORMAT_OPTIONS = [
  { value: 'XLSX', label: 'Excel (.xlsx)' },
  { value: 'PDF', label: 'PDF' },
]

export function BalanceExports() {
  const { can, status } = usePermissions()
  const [residents, setResidents] = useState([])
  const [v, setV] = useState({ from: '', to: '', format: 'XLSX' })
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [exports, setExports] = useState([])
  const key = useRef({ statement: crypto.randomUUID(), report: crypto.randomUUID() })
  const set = (name) => (value) => setV((s) => ({ ...s, [name]: value }))

  if (status === 'loading') return <Skeleton className="h-32 w-full" />
  if (!can('STATEMENT_REPORT', 'CREATE'))
    return (
      <Card>
        <CardContent>
          <p className="text-muted-foreground">Chỉ Kế toán xuất được sao kê và báo cáo thu chi.</p>
        </CardContent>
      </Card>
    )

  function check(needResidents) {
    const found = {}
    if (needResidents && residents.length === 0) found.residents = 'Chọn ít nhất một người cao tuổi.'
    if (!v.from) found.from = 'Chọn ngày bắt đầu.'
    if (!v.to) found.to = 'Chọn ngày kết thúc.'
    if (v.from && v.to && v.from > v.to) found.to = 'Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.'
    return found
  }

  async function run(kind) {
    const found = check(kind === 'statement')
    setErrors(found)
    setFailure(null)
    if (Object.keys(found).length > 0) return
    setBusy(true)
    try {
      const range = { from: v.from, to: v.to, format: v.format }
      const { data } =
        kind === 'statement'
          ? await postBalanceExportsStatement({
              headers: { 'Idempotency-Key': key.current.statement },
              body: { residentIds: residents.map((r) => r.residentId), ...range },
            })
          : await postBalanceExportsIncomeExpenseReport({
              headers: { 'Idempotency-Key': key.current.report },
              body: range,
            })
      key.current[kind] = crypto.randomUUID()
      setExports((list) => [data, ...list])
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  async function download(item) {
    setFailure(null)
    try {
      const { data } = await getBalanceExportsByIdFile({ path: { id: item.id }, responseType: 'blob' })
      saveBlob(
        data,
        `${item.type === 'BALANCE_STATEMENT' ? 'sao-ke' : 'thu-chi'}-${item.from}-${item.to}.${item.format === 'PDF' ? 'pdf' : 'xlsx'}`,
      )
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-6">
        <div className="flex flex-col gap-3">
          <h2 className="type-section">Người cao tuổi cho sao kê</h2>
          <ResidentPicker
            onPick={(picked) =>
              setResidents((list) => (list.some((r) => r.residentId === picked.residentId) ? list : [...list, picked]))
            }
          />
          {residents.length > 0 && (
            <ul className="flex flex-wrap gap-2" aria-label="Người cao tuổi đã chọn">
              {residents.map((r) => (
                <li key={r.residentId} className="flex items-center gap-1 rounded-full border px-3 py-1">
                  <span className="type-label">{r.fullName}</span>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Bỏ ${r.fullName}`}
                    onClick={() => setResidents((list) => list.filter((x) => x.residentId !== r.residentId))}
                  >
                    <X aria-hidden="true" />
                  </Button>
                </li>
              ))}
            </ul>
          )}
          {errors.residents && <p className="text-destructive">{errors.residents}</p>}
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <TextField
            id="export-from"
            label="Từ ngày"
            type="date"
            value={v.from}
            onChange={set('from')}
            error={errors.from}
          />
          <TextField id="export-to" label="Đến ngày" type="date" value={v.to} onChange={set('to')} error={errors.to} />
          <SelectField
            id="export-format"
            label="Định dạng"
            value={v.format}
            onChange={set('format')}
            options={FORMAT_OPTIONS}
          />
        </div>
        <ProblemAlert problem={failure} />
        <div className="flex flex-wrap gap-2">
          <Button className="h-11" disabled={busy} onClick={() => run('statement')}>
            Xuất sao kê
          </Button>
          <Button variant="outline" className="h-11" disabled={busy} onClick={() => run('report')}>
            Xuất báo cáo thu chi
          </Button>
        </div>
        {exports.length > 0 && (
          <div className="flex flex-col gap-2">
            <h2 className="type-section">Lần xuất trong phiên này</h2>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Loại</TableHead>
                  <TableHead>Khoảng</TableHead>
                  <TableHead>Định dạng</TableHead>
                  <TableHead>Số dòng</TableHead>
                  <TableHead>Lúc</TableHead>
                  <TableHead>
                    <span className="sr-only">Tải</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {exports.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>{TYPE_LABELS[item.type] ?? item.type}</TableCell>
                    <TableCell className="tabular-nums">
                      {formatDate(item.from)} – {formatDate(item.to)}
                    </TableCell>
                    <TableCell>{item.format === 'PDF' ? 'PDF' : 'Excel'}</TableCell>
                    <TableCell className="tabular-nums">{item.rowCount} dòng</TableCell>
                    <TableCell className="tabular-nums">{formatDateTime(item.at)}</TableCell>
                    <TableCell>
                      <Button
                        variant="outline"
                        className="h-11"
                        aria-label={`Tải lần xuất ${TYPE_LABELS[item.type] ?? item.type} ${formatDate(item.from)}`}
                        onClick={() => download(item)}
                      >
                        Tải
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
