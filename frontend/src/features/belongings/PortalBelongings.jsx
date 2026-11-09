import { useEffect, useRef, useState } from 'react'
import { getFamilyPortalResidentsByResidentIdBelongings, postFamilyPortalSelfCustodyConsentsByIdWithdraw } from '@/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { formatDateTime } from '@/common/dateTime'
import { openFile } from '@/common/files'
import { problemOf } from '@/common/problem'
import { amountText, kindLabel, statusLabel } from './belongingLabels'

// Đồ gửi trên cổng người thân (013 T028, US6; FR-030, BR-M12-09; api-013 GET /family-portal/residents/{residentId}/
// belongings): chỉ xem, không có vị trí lưu giữ, không tên nhân viên; người đại diện và người được phép đón đang hiệu lực
// (backend kiểm mỗi lần gọi, khác thì 403). Màn P9 của cổng (DESIGN.md 2.3) đặt phần này khi khung cổng có người cao
// tuổi thật (team-plan mục 8, E).
// Người đại diện rút đồng ý cho tự giữ của một đồ (FR-015a; api-013 POST /family-portal/self-custody-consents/{id}/withdraw).
// Cổng không biết người xem có phải người đại diện hiện tại không; không phải thì backend trả 403 và màn hiện lỗi.
function WithdrawConsent({ consent, onDone }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [failure, setFailure] = useState(null)
  const [busy, setBusy] = useState(false)
  const key = useRef(crypto.randomUUID())
  async function submit(event) {
    event.preventDefault()
    if (!reason.trim()) {
      setFailure({ title: 'Nhập lý do', detail: 'Nhập lý do rút đồng ý.', unmetConditions: [] })
      return
    }
    setBusy(true)
    setFailure(null)
    try {
      await postFamilyPortalSelfCustodyConsentsByIdWithdraw({
        path: { id: consent.id },
        headers: { 'Idempotency-Key': key.current },
        body: { reason: reason.trim() },
      })
      onDone()
    } catch (error) {
      setFailure(problemOf(error))
    } finally {
      setBusy(false)
    }
  }
  if (!open)
    return (
      <Button type="button" variant="outline" size="sm" className="h-10 self-start" onClick={() => setOpen(true)}>
        Rút đồng ý
      </Button>
    )
  return (
    <form className="flex flex-col gap-2" onSubmit={submit} noValidate>
      <Input
        aria-label="Lý do rút đồng ý"
        className="h-11"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
      />
      <ProblemAlert problem={failure} />
      <Button type="submit" size="sm" className="h-10 self-start" disabled={busy}>
        Xác nhận rút đồng ý
      </Button>
    </form>
  )
}

export function PortalBelongings({ residentId }) {
  const [state, setState] = useState({ status: 'loading', items: [] })
  const [failure, setFailure] = useState(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let cancelled = false
    getFamilyPortalResidentsByResidentIdBelongings({ path: { residentId } })
      .then(({ data }) => !cancelled && setState({ status: 'ready', items: data }))
      .catch((error) => !cancelled && setState({ status: 'error', items: [], problem: problemOf(error) }))
    return () => {
      cancelled = true
    }
  }, [residentId, reload])

  if (state.status === 'loading') return <Skeleton className="h-24 w-full" />
  if (state.status === 'error')
    return state.problem.status === 403 ? (
      <p>Chỉ người đại diện và người được phép đón đang hiệu lực xem được đồ gửi.</p>
    ) : (
      <ProblemAlert problem={state.problem} />
    )
  if (state.items.length === 0) return <p className="text-muted-foreground">Chưa có đồ gửi viện giữ hộ.</p>

  return (
    <div className="flex flex-col gap-2">
      <ProblemAlert problem={failure} />
      <ul className="flex flex-col">
        {state.items.map((b) => (
          <li key={b.id} className="flex flex-col gap-2 border-t py-3 first:border-t-0 first:pt-0">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="type-label">{b.description}</span>
              <StatusBadge status={statusLabel(b.status)} />
            </div>
            <span className="type-body-sm tabular-nums text-muted-foreground">{amountText(b)}</span>
            {b.selfCustodyConsent && (
              <div className="flex flex-col gap-2">
                <span className="type-body-sm">Đã đồng ý cho người cao tuổi tự giữ</span>
                <WithdrawConsent consent={b.selfCustodyConsent} onDone={() => setReload((n) => n + 1)} />
              </div>
            )}
            <ol className="flex flex-col gap-1">
              {b.history.map((h) => (
                <li key={h.id} className="type-body-sm flex flex-wrap gap-x-2">
                  <span className="tabular-nums text-muted-foreground">{formatDateTime(h.occurredAt)}</span>
                  <span>{kindLabel(h.kind)}</span>
                  {h.conditionDescription && <span className="text-muted-foreground">{h.conditionDescription}</span>}
                </li>
              ))}
            </ol>
            <div className="flex flex-wrap gap-2">
              {b.history
                .flatMap((h) => h.photoFileIds ?? [])
                .map((id, index) => (
                  <Button
                    key={id}
                    variant="outline"
                    size="sm"
                    className="h-10"
                    aria-label={`Xem ảnh ${index + 1} của ${b.description}`}
                    onClick={() =>
                      openFile(id).then(
                        () => setFailure(null),
                        (e) => setFailure(problemOf(e)),
                      )
                    }
                  >
                    Xem ảnh {index + 1}
                  </Button>
                ))}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
