import { useRef, useState } from 'react'
import { postCostsPeriodsByIdExport } from '@/api'
import { Button } from '@/components/ui/button'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { controlClass } from './chargeLabels'

function downloadFile(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function AccountingExport({ periodId, download = downloadFile }) {
  const [scope, setScope] = useState('ALL')
  const [format, setFormat] = useState('XLSX')
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const [message, setMessage] = useState('')
  const retry = useRef(null)
  async function submit(event) {
    event.preventDefault()
    if (!periodId || busy) return
    const signature = `${periodId}:${scope}:${format}`
    if (retry.current?.signature !== signature) retry.current = { signature, key: crypto.randomUUID() }
    setBusy(true)
    setFailure(null)
    try {
      const { data } = await postCostsPeriodsByIdExport({
        path: { id: Number(periodId) },
        body: { scope, format },
        headers: { 'Idempotency-Key': retry.current.key },
        responseType: 'blob',
      })
      download(data, `chi-phi-${periodId}.${format.toLowerCase()}`)
      retry.current = null
      setMessage('Đã tải file kế toán. Đối chiếu dòng tổng kiểm soát trước khi nhập dữ liệu.')
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <form onSubmit={submit} className="space-y-3 rounded-lg border bg-card p-4">
      <h2 className="type-title">Xuất kế toán</h2>
      <p className="type-body-sm">
        Chỉ xuất các bảng đã chốt của kỳ đang chọn. File kèm danh sách người có bảng chưa chốt.
      </p>
      <label className="flex flex-col gap-2">
        Phạm vi xuất
        <select className={controlClass} value={scope} disabled={busy} onChange={(e) => setScope(e.target.value)}>
          <option value="ALL">Toàn bộ bảng đã chốt</option>
          <option value="NEVER_EXPORTED">Chỉ bảng chưa từng xuất</option>
        </select>
      </label>
      <label className="flex flex-col gap-2">
        Định dạng
        <select className={controlClass} value={format} disabled={busy} onChange={(e) => setFormat(e.target.value)}>
          <option value="XLSX">Excel (.xlsx)</option>
          <option value="CSV">CSV (.csv)</option>
        </select>
      </label>
      <ProblemAlert problem={failure} />
      <Button disabled={!periodId || busy} type="submit">
        {busy ? 'Đang xuất…' : 'Tải file kế toán'}
      </Button>
      <p role="status">{message}</p>
    </form>
  )
}
