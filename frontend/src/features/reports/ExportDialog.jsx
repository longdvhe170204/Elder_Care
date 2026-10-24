import { useRef, useState } from 'react'
import { postReportsByGroupExport } from '@/api'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { openFile } from '@/common/files'
import { problemOf } from '@/common/problem'

// Hộp xuất báo cáo (016 FR-060 → FR-062; DESIGN.md `export-dialog`). Chỉ Quản lý viện có ô định danh người cao tuổi và
// mục đích (bắt buộc khi chọn, Q-198); vai trò khác chỉ xuất tổng hợp. Backend ghi nhật ký trước khi trả mã tệp.

export function ExportDialog({ open, onOpenChange, group, groupLabel, from, to, canIdentify, onExported }) {
  const [includeIdentity, setIncludeIdentity] = useState(false)
  const [purpose, setPurpose] = useState('')
  const [purposeError, setPurposeError] = useState(null)
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)
  const key = useRef(crypto.randomUUID())

  function reset(next) {
    if (!next) {
      setIncludeIdentity(false)
      setPurpose('')
      setPurposeError(null)
      setFailure(null)
      setResult(null)
      key.current = crypto.randomUUID()
    }
    onOpenChange(next)
  }

  async function submit() {
    setPurposeError(null)
    setFailure(null)
    if (includeIdentity && !purpose.trim()) {
      setPurposeError('Nhập mục đích xuất danh sách có định danh.')
      return
    }
    setBusy(true)
    try {
      const { data } = await postReportsByGroupExport({
        path: { group },
        headers: { 'Idempotency-Key': key.current },
        body: {
          from,
          to,
          includeIdentity,
          purpose: purpose.trim() || undefined,
          format: 'XLSX',
        },
      })
      setResult(data)
      onExported()
    } catch (error) {
      const problem = problemOf(error)
      if (problem.errorCode === 'PURPOSE_REQUIRED') setPurposeError(problem.detail || problem.title)
      else setFailure(problem)
      key.current = crypto.randomUUID()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={reset}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Xuất báo cáo {groupLabel.toLowerCase()}</DialogTitle>
          <DialogDescription>
            {formatDate(from)} – {formatDate(to)}. File XLSX cùng số với màn hình, lần xuất được ghi vào nhật ký.
          </DialogDescription>
        </DialogHeader>
        {result ? (
          <p className="type-body">Đã xuất file. Lần xuất đã ghi vào nhật ký.</p>
        ) : (
          <div className="flex flex-col gap-4">
            {canIdentify ? (
              <>
                <Field orientation="horizontal" className="min-h-10 items-center">
                  <Checkbox
                    id="export-identity"
                    checked={includeIdentity}
                    onCheckedChange={(c) => setIncludeIdentity(!!c)}
                  />
                  <FieldLabel htmlFor="export-identity" className="type-body-sm">
                    Có danh sách định danh người cao tuổi
                  </FieldLabel>
                </Field>
                {includeIdentity && (
                  <Field data-invalid={!!purposeError || undefined}>
                    <FieldLabel htmlFor="export-purpose">Mục đích xuất (bắt buộc)</FieldLabel>
                    <Textarea
                      id="export-purpose"
                      value={purpose}
                      aria-invalid={!!purposeError || undefined}
                      onChange={(e) => setPurpose(e.target.value)}
                    />
                    {purposeError && <FieldError>{purposeError}</FieldError>}
                  </Field>
                )}
              </>
            ) : (
              <p className="type-body-sm text-muted-foreground">File chỉ có số liệu tổng hợp.</p>
            )}
            <ProblemAlert problem={failure} title="Không xuất được báo cáo" />
          </div>
        )}
        <DialogFooter>
          {result ? (
            <Button className="h-11" onClick={() => openFile(result.fileId)}>
              Tải file
            </Button>
          ) : (
            <>
              <Button variant="outline" className="h-11" onClick={() => reset(false)}>
                Hủy
              </Button>
              <Button className="h-11" disabled={busy} onClick={submit}>
                Xuất file
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
