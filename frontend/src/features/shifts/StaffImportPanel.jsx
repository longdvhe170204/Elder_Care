import { useState } from 'react'
import { ApiError, getStaffImportTemplate, postStaffImportPreview, postStaffImportConfirm } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Field, FieldLabel } from '@/components/ui/field'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDate } from '@/common/dateTime'
const COLUMNS = [
  ['fullName', 'Họ tên'],
  ['position', 'Chức danh'],
  ['phoneNumber', 'Số điện thoại'],
  ['startDate', 'Ngày bắt đầu làm việc'],
  ['specialty', 'Chuyên môn'],
  ['email', 'Email'],
]
export function StaffImportPanel({ onDone }) {
  const [file, setFile] = useState(null),
    [preview, setPreview] = useState(null),
    [failure, setFailure] = useState(null),
    [notice, setNotice] = useState(''),
    [busy, setBusy] = useState(false)
  async function download() {
    setBusy(true)
    try {
      const { data } = await getStaffImportTemplate({ responseType: 'blob' })
      const url = URL.createObjectURL(data)
      const link = document.createElement('a')
      link.href = url
      link.download = 'staff-template.xlsx'
      link.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  async function send(confirm) {
    if (!file) return
    setBusy(true)
    setFailure(null)
    setNotice('')
    const form = new FormData()
    form.append('file', file)
    try {
      const { data } = await (confirm ? postStaffImportConfirm : postStaffImportPreview)({
        body: form,
        ...(confirm && { headers: { 'Idempotency-Key': crypto.randomUUID() } }),
      })
      if (confirm) {
        setPreview(null)
        setNotice(`Đã tạo ${data.createdProfileCount} hồ sơ nhân viên.`)
        onDone?.()
      } else setPreview(data)
    } catch (error) {
      if (error instanceof ApiError && error.status === 409 && Array.isArray(error.problem?.errors)) {
        setPreview(error.problem)
      } else setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <section className="flex min-w-0 flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5 [&_button]:min-h-11 [&_input]:min-h-11">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="type-section">Import hồ sơ nhân viên</h2>
        <Button variant="outline" disabled={busy} onClick={download}>
          Tải file mẫu
        </Button>
      </div>
      <p className="text-muted-foreground">
        Dùng file .xlsx đúng mẫu, tối đa 5 MB. Bốn cột đầu bắt buộc. Xem trước để kiểm tra; hệ thống kiểm lại toàn bộ
        file khi xác nhận.
      </p>
      <Field>
        <FieldLabel htmlFor="staff-import-file">File Excel</FieldLabel>
        <Input
          id="staff-import-file"
          type="file"
          accept=".xlsx"
          disabled={busy}
          onChange={(e) => {
            setFile(e.target.files?.[0] ?? null)
            setPreview(null)
            setNotice('')
            setFailure(null)
          }}
        />
      </Field>
      <ProblemAlert problem={failure} />
      {notice && <p role="status">{notice}</p>}
      <div className="flex flex-wrap gap-2">
        <Button variant="outline" disabled={!file || busy} onClick={() => send(false)}>
          {busy ? 'Đang xử lý…' : 'Xem trước'}
        </Button>
        <Button disabled={!preview?.canConfirm || busy} onClick={() => send(true)}>
          Xác nhận nhập
        </Button>
      </div>
      {preview && (
        <>
          <p role="status">
            {preview.rowCount} dòng · {preview.errors.length ? `${preview.errors.length} lỗi cần sửa` : 'Không có lỗi'}
          </p>
          {preview.errors.length > 0 && (
            <ul aria-label="Các lỗi cần sửa" className="ml-5 list-disc text-attention">
              {preview.errors.map((error, index) => (
                <li key={index}>
                  Dòng {error.rowNumber} · {COLUMNS.find(([key]) => key === error.column)?.[1] ?? error.column}:{' '}
                  {error.reason}
                </li>
              ))}
            </ul>
          )}
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Dòng</TableHead>
                {COLUMNS.map(([key, label]) => (
                  <TableHead key={key}>{label}</TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {preview.rows.map((row) => (
                <TableRow key={row.rowNumber}>
                  <TableCell>{row.rowNumber}</TableCell>
                  {COLUMNS.map(([key, label]) => {
                    const errors = preview.errors.filter((e) => e.rowNumber === row.rowNumber && e.column === key)
                    return (
                      <TableCell
                        key={key}
                        aria-invalid={errors.length ? true : undefined}
                        className={errors.length ? 'bg-attention/10' : ''}
                      >
                        <span>{key === 'startDate' ? formatDate(row[key]) : row[key] || '—'}</span>
                        {errors.map((e, n) => (
                          <p
                            key={n}
                            className="mt-1 max-w-56 whitespace-normal text-attention"
                            aria-label={`Lỗi dòng ${row.rowNumber}, ${label}`}
                          >
                            {e.reason}
                            {e.duplicateRowNumber ? ` (dòng ${e.duplicateRowNumber})` : ''}
                          </p>
                        ))}
                      </TableCell>
                    )
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </>
      )}
    </section>
  )
}
