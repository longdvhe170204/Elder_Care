import { useEffect, useRef, useState } from 'react'
import {
  getPurchaseAssistanceRequests,
  postPurchaseAssistanceRequestsByIdAgree,
  postPurchaseAssistanceRequestsByIdReject,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { PortalCard } from '../PortalParts'

// Đề nghị mua hộ chờ người đại diện quyết định (010 T031, FR-023, FR-023a; BR-M10-10; 012 FR-046a). Dấu "số dư không đủ"
// chỉ để biết, không chặn quyết định (017 FR-021). Quyết định có bước xem lại (DESIGN.md 9.2 điều 5); 409 thì tải lại.

const MONEY = new Intl.NumberFormat('vi-VN')
const money = (n) => `${MONEY.format(n ?? 0)} đồng`

function Request({ row, onDone }) {
  const [command, setCommand] = useState(null)
  const [reason, setReason] = useState('')
  const [pending, setPending] = useState(false)
  const [failure, setFailure] = useState(null)
  const nonce = useRef(null)
  async function submit(event) {
    event.preventDefault()
    if (pending || (command === 'reject' && !reason.trim())) return
    setPending(true)
    setFailure(null)
    nonce.current ??= crypto.randomUUID()
    try {
      const call =
        command === 'agree' ? postPurchaseAssistanceRequestsByIdAgree : postPurchaseAssistanceRequestsByIdReject
      await call({
        path: { id: row.id },
        headers: { 'Idempotency-Key': nonce.current },
        ...(command === 'reject' ? { body: { reason: reason.trim() } } : {}),
      })
      onDone('Đã ghi quyết định của bạn. Danh sách được tải lại.')
    } catch (error) {
      if (error.status === 409) onDone('Đề nghị đã thay đổi. Hãy xem lại danh sách trước khi quyết định.')
      else setFailure(problemOf(error))
    } finally {
      setPending(false)
    }
  }
  // Lần xin đồng ý bổ sung (Q-251): số mới nằm ở quyết định REQUEST_ADDITIONAL gần nhất.
  const amount = row.additionalConsentPending
    ? (row.decisions?.findLast((d) => d.type === 'REQUEST_ADDITIONAL')?.newAmount ?? row.estimatedAmount)
    : row.estimatedAmount
  return (
    <PortalCard title={row.description}>
      <p>
        Số lượng {row.quantity}, số tiền cần đồng ý {money(amount)}
      </p>
      {row.additionalConsentPending && row.allowedAmount != null && (
        <p>{`Đã đồng ý ${money(row.allowedAmount)}, nay cần đồng ý ${money(amount)}`}</p>
      )}
      {row.reason && <p className="text-muted-foreground">Lý do: {row.reason}</p>}
      {row.insufficientBalance && <p>Số dư hiện không đủ cho số tiền này. Bạn vẫn có thể đồng ý hoặc từ chối.</p>}
      {command === null ? (
        <div className="flex flex-wrap gap-2">
          <Button onClick={() => setCommand('agree')} aria-label={`Đồng ý đề nghị #${row.id}`}>
            Đồng ý
          </Button>
          <Button variant="outline" onClick={() => setCommand('reject')} aria-label={`Từ chối đề nghị #${row.id}`}>
            Từ chối
          </Button>
        </div>
      ) : (
        <form onSubmit={submit} className="flex flex-col gap-3">
          <p>
            Xem lại trước khi {command === 'agree' ? 'đồng ý' : 'từ chối'}: {row.description}, {money(amount)}.
          </p>
          {command === 'reject' && (
            <label className="flex flex-col gap-1">
              Lý do từ chối
              <Textarea
                required
                maxLength={2000}
                value={reason}
                disabled={pending}
                onChange={(e) => {
                  setReason(e.target.value)
                  nonce.current = null
                }}
              />
            </label>
          )}
          <ProblemAlert problem={failure} />
          <div className="flex flex-wrap gap-2">
            <Button type="submit" disabled={pending || (command === 'reject' && !reason.trim())}>
              {command === 'agree' ? 'Xác nhận đồng ý' : 'Xác nhận từ chối'}
            </Button>
            <Button type="button" variant="outline" disabled={pending} onClick={() => setCommand(null)}>
              Quay lại
            </Button>
          </div>
        </form>
      )}
    </PortalCard>
  )
}

export function LivePurchaseConsent({ residentId }) {
  const [state, setState] = useState({ status: 'loading' })
  const [revision, setRevision] = useState(0)
  const [announcement, setAnnouncement] = useState('')
  useEffect(() => {
    let cancelled = false
    getPurchaseAssistanceRequests({ query: { residentId, status: 'AWAITING_CONSENT' } })
      .then(({ data }) => !cancelled && setState({ status: 'ready', data }))
      .catch((error) => !cancelled && setState({ status: 'error', error }))
    return () => {
      cancelled = true
    }
  }, [residentId, revision])
  function done(message) {
    setAnnouncement(message)
    setRevision((n) => n + 1)
  }
  return (
    <>
      <h2 className="type-title">Đề nghị mua hộ cần bạn quyết định</h2>
      {announcement && (
        <p role="status" className="type-body-sm">
          {announcement}
        </p>
      )}
      {state.status === 'loading' && <Skeleton className="h-24 w-full" />}
      {state.status === 'error' && (
        <PortalCard>
          <ProblemAlert problem={problemOf(state.error)} />
        </PortalCard>
      )}
      {state.status === 'ready' &&
        (state.data.length === 0 ? (
          <PortalCard>
            <p className="text-muted-foreground">Không có đề nghị mua hộ nào đang chờ bạn quyết định.</p>
          </PortalCard>
        ) : (
          state.data.map((row) => <Request key={row.id} row={row} onDone={done} />)
        ))}
    </>
  )
}
