import { useEffect, useState } from 'react'
import { getBedAllocations } from '@/api'
import { Button } from '@/components/ui/button'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { formatDateTime } from '@/common/dateTime'
import { BedTransferForm } from './BedTransferForm'

export function BedTransfersPanel({ canTransfer }) {
  const [state, setState] = useState({ loading: true, allocations: [] })
  const [revision, setRevision] = useState(0)
  const [selected, setSelected] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [notice, setNotice] = useState(null)
  useEffect(() => {
    let current = true
    getBedAllocations({ query: { status: 'ACTIVE' } })
      .then(({ data }) => current && setState({ loading: false, allocations: data }))
      .catch((cause) => current && setState({ loading: false, allocations: [], error: problemOf(cause) }))
    return () => {
      current = false
    }
  }, [revision])
  function saved(result) {
    setSelected(null)
    setNotice({
      message:
        result.status === 202
          ? `Đã lập yêu cầu nhập bù #${result.data.approvalRequestId}, chờ Quản lý viện duyệt.`
          : 'Đã chuyển giường.',
      warnings: result.data.warnings ?? [],
    })
    setRevision((value) => value + 1)
  }
  return (
    <section className="flex flex-col gap-4" aria-label="Phân bổ đang hiệu lực">
      {notice && (
        <Alert role="status">
          <AlertTitle>{notice.message}</AlertTitle>
          <AlertDescription>
            {notice.warnings.map((warning) => (
              <p key={warning.code}>{warning.description}</p>
            ))}
          </AlertDescription>
        </Alert>
      )}
      {state.loading && <p aria-busy="true">Đang tải phân bổ giường…</p>}
      {state.error && (
        <ProblemAlert problem={state.error}>
          <Button variant="outline" onClick={() => setRevision((value) => value + 1)}>
            Thử lại
          </Button>
        </ProblemAlert>
      )}
      {!state.loading && !state.error && !state.allocations.length && (
        <p className="rounded-xl border bg-card p-6 text-muted-foreground">
          Chưa có phân bổ đang hiệu lực trong phạm vi được xem.
        </p>
      )}
      {state.allocations.map((allocation) => (
        <article
          key={allocation.id}
          className="flex flex-wrap items-center justify-between gap-4 rounded-xl border bg-card p-4"
        >
          <div className="min-w-0">
            <h2 className="type-title-sm">Hồ sơ #{allocation.residentId}</h2>
            <p className="break-words">{allocation.location.label}</p>
            <p className="type-caption text-muted-foreground">Bắt đầu: {formatDateTime(allocation.startAt)}</p>
          </div>
          {canTransfer && allocation.status === 'ACTIVE' && (
            <Button
              variant="outline"
              aria-label={`Chuyển giường hồ sơ ${allocation.residentId}`}
              onClick={() => setSelected(allocation)}
            >
              Chuyển giường
            </Button>
          )}
        </article>
      ))}
      <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && !submitting && setSelected(null)}>
        <SheetContent className="overflow-y-auto" showCloseButton={!submitting}>
          <div className="px-4 pb-6">
            {selected && (
              <BedTransferForm
                key={selected.id}
                allocation={selected}
                onSaved={saved}
                onBusyChange={setSubmitting}
                onClose={() => setSelected(null)}
              />
            )}
          </div>
        </SheetContent>
      </Sheet>
    </section>
  )
}
