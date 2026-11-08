import { useCallback, useEffect, useRef, useState } from 'react'
import {
  getResidentsByResidentIdSettlement,
  getSettlementStatementsByIdFile,
  postResidentsByResidentIdSettlementConfirm,
  postSettlementStatementsByIdSignedCopy,
} from '@/api'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { uploadFile } from '@/common/files'
import { usePermissions } from '@/common/permissions/permissions'
import { problemOf } from '@/common/problem'
import { formatMoney } from './balanceLabels'
import { saveBlob } from './download'

// Quyết toán của một người cao tuổi (017 T033; FR-022, FR-034, FR-037, BR-M11-22, DBR-38): hai điều kiện kết thúc lưu
// trú kèm lý do, lệnh Xác nhận quyết toán (mỗi lần tạo một phiên bản bảng PDF, phiên bản mới nhất là hiện hành), tải
// PDF đúng phiên bản, đính kèm bản ký của người đại diện (một lần cho phiên bản hiện hành). Ngoại lệ đã duyệt của 004
// chỉ miễn ba ý về tiền, không miễn ý xác nhận quyết toán.

const CONDITION_LABELS = {
  END_H_SETTLEMENT: 'Bảng kỳ cuối đã chốt và trừ, số dư và số dư cọc bằng 0',
  END_H_SETTLEMENT_CONFIRMATION: 'Không còn giao dịch Chờ duyệt và Kế toán đã xác nhận quyết toán',
}

export function SettlementPanel({ residentId }) {
  const { can } = usePermissions()
  const [state, setState] = useState({ status: 'loading' })
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const [files, setFiles] = useState({})
  const confirmKey = useRef(crypto.randomUUID())
  const signKeys = useRef(new Map())

  const load = useCallback(async () => {
    try {
      const { data } = await getResidentsByResidentIdSettlement({ path: { residentId } })
      setState({ status: 'ready', data })
    } catch (error) {
      setState({ status: 'error', problem: problemOf(error) })
    }
  }, [residentId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải quyết toán khi mở người cao tuổi
    load()
  }, [load])

  async function confirm() {
    setBusy(true)
    setFailure(null)
    try {
      await postResidentsByResidentIdSettlementConfirm({
        path: { residentId },
        headers: { 'Idempotency-Key': confirmKey.current },
      })
      confirmKey.current = crypto.randomUUID()
      setConfirming(false)
      await load()
    } catch (error) {
      setConfirming(false)
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  async function download(version) {
    setFailure(null)
    try {
      const { data } = await getSettlementStatementsByIdFile({ path: { id: version.id }, responseType: 'blob' })
      saveBlob(data, `bang-quyet-toan-phien-ban-${version.version}.pdf`)
    } catch (error) {
      setFailure(problemOf(error))
    }
  }

  async function attach(version) {
    const file = files[version.id]
    if (!file) return
    setBusy(true)
    setFailure(null)
    if (!signKeys.current.has(version.id)) signKeys.current.set(version.id, crypto.randomUUID())
    try {
      const fileId = await uploadFile(file)
      await postSettlementStatementsByIdSignedCopy({
        path: { id: version.id },
        headers: { 'Idempotency-Key': signKeys.current.get(version.id) },
        body: { fileId },
      })
      signKeys.current.delete(version.id)
      await load()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }

  if (state.status === 'loading') return <Skeleton className="h-24 w-full" />
  if (state.status === 'error') return <ProblemAlert problem={state.problem} />
  const { conditions, versions, hasException } = state.data
  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-3">
        {conditions.map((condition) => (
          <li key={condition.code} className="flex flex-col gap-1 rounded-lg border p-3">
            <div className="flex flex-wrap items-center gap-2">
              <Badge variant={condition.status === 'MET' ? 'settled' : 'attention'}>
                {condition.status === 'MET' ? 'Đạt' : 'Chưa đạt'}
              </Badge>
              <span className="type-label">{CONDITION_LABELS[condition.code] ?? condition.code}</span>
              <span className="type-caption text-muted-foreground">
                {condition.waivable ? 'Miễn được bằng ngoại lệ' : 'Không miễn được'}
              </span>
            </div>
            {condition.reasons.length > 0 && (
              <ul className="ml-4 list-disc type-body-sm">
                {condition.reasons.map((reason) => (
                  <li key={reason}>{reason}</li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
      {hasException && <p className="type-body-sm text-muted-foreground">Hồ sơ có ngoại lệ quyết toán đã duyệt.</p>}
      <ProblemAlert problem={failure} />
      {can('SETTLEMENT_RECORD', 'CREATE') && (
        <Button className="h-11 self-start" onClick={() => setConfirming(true)}>
          Xác nhận quyết toán
        </Button>
      )}
      {versions.length > 0 && (
        <ul className="flex flex-col gap-3" aria-label="Phiên bản bảng quyết toán">
          {versions.map((version) => (
            <li
              key={version.id}
              className={`flex flex-col gap-2 rounded-lg border p-3 ${version.current ? 'border-primary' : ''}`}
            >
              <div className="flex flex-wrap items-center gap-2">
                <span className="type-label">Phiên bản {version.version}</span>
                {version.current && <Badge variant="held">Hiện hành</Badge>}
                {version.hasSignedCopy && <Badge variant="settled">Đã có bản ký</Badge>}
                {version.hasException && <Badge variant="neutral">Theo ngoại lệ</Badge>}
              </div>
              <span className="type-body-sm text-muted-foreground tabular-nums">
                Xác nhận bởi {version.confirmedBy}, {formatDateTime(version.confirmedAt)}
                {version.debtOrRefundAmount !== 0 && <> · Còn lại {formatMoney(version.debtOrRefundAmount)}</>}
              </span>
              <div className="flex flex-wrap items-center gap-2">
                <Button
                  variant="outline"
                  className="h-11"
                  aria-label={`Tải PDF phiên bản ${version.version}`}
                  onClick={() => download(version)}
                >
                  Tải PDF
                </Button>
                {version.current && !version.hasSignedCopy && can('SETTLEMENT_RECORD', 'UPDATE') && (
                  <>
                    <input
                      type="file"
                      aria-label={`Bản ký phiên bản ${version.version}`}
                      accept="application/pdf,image/jpeg,image/png"
                      className="type-body-sm h-11 rounded-lg border px-3 py-2"
                      onChange={(e) => setFiles((s) => ({ ...s, [version.id]: e.target.files?.[0] }))}
                    />
                    <Button className="h-11" disabled={busy || !files[version.id]} onClick={() => attach(version)}>
                      Đính kèm bản ký
                    </Button>
                  </>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
      <Dialog open={confirming} onOpenChange={setConfirming}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Xác nhận quyết toán</DialogTitle>
            <DialogDescription>
              Hệ thống dựng bảng quyết toán PDF phiên bản mới từ số liệu hiện tại. Phiên bản cũ được giữ để tra cứu.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" className="h-11" onClick={() => setConfirming(false)}>
              Đóng
            </Button>
            <Button className="h-11" disabled={busy} onClick={confirm}>
              Xác nhận
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
