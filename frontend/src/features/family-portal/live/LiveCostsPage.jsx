import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { ApiError, getFamilyPortalResidentsByResidentIdBalance } from '@/api'
import { Skeleton } from '@/components/ui/skeleton'
import { PageHeader } from '@/common/PageHeader'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { PortalCard } from '../PortalParts'
import { LiveCostStatements } from './LiveCostStatements'
import { LiveDocuments } from './LiveDocuments'
import { LivePurchaseConsent } from './LivePurchaseConsent'

// P7 Chi phí, số dư, phần số dư (017 T011; FR-003; Phụ lục 27 chú thích 16): số dư, cọc, giao dịch Đã xác nhận, tài
// khoản thu của viện (CFG-M11-10), mã nộp tiền và mã QR VietQR không kèm số tiền (người thân tự nhập số tiền). Thanh toán
// bảng chỉ hiện tổng (Q-133). Không có thanh toán trực tuyến (Q-285). Bảng chi phí từng tháng, tạm tính (010 FR-037,
// FR-038) và đề nghị mua hộ chờ người đại diện (BR-M10-10) nối 2026-11-10.

const MONEY = new Intl.NumberFormat('vi-VN')
const money = (n) => `${MONEY.format(n ?? 0)} đồng`

const TYPES = {
  CASH_PAYMENT: 'Nộp tiền mặt',
  BANK_TRANSFER_PAYMENT: 'Nộp chuyển khoản',
  STATEMENT_PAYMENT: 'Thanh toán bảng chi phí',
  REFUND: 'Hoàn tiền',
  DEPOSIT_OFFSET: 'Cấn trừ tiền cọc',
  ADJUSTMENT: 'Điều chỉnh',
  DEPOSIT_RECEIPT: 'Thu cọc',
  DEPOSIT_REFUND: 'Hoàn cọc',
}

function Instructions({ p }) {
  if (!p) return <p className="text-muted-foreground">Liên hệ Kế toán để biết tài khoản nộp tiền.</p>
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
      <div className="self-start rounded-[10px] border bg-card p-3">
        <QRCodeSVG value={p.vietQr} size={168} role="img" aria-label="Mã QR chuyển khoản cho viện, không kèm số tiền" />
      </div>
      <dl className="grid gap-2">
        <div className="flex flex-col gap-0.5">
          <dt className="type-caption text-muted-foreground">Ngân hàng</dt>
          <dd>{p.bank}</dd>
        </div>
        <div className="flex flex-col gap-0.5">
          <dt className="type-caption text-muted-foreground">Số tài khoản</dt>
          <dd className="tabular-nums">{p.accountNumber}</dd>
        </div>
        <div className="flex flex-col gap-0.5">
          <dt className="type-caption text-muted-foreground">Chủ tài khoản</dt>
          <dd>{p.accountHolder}</dd>
        </div>
        <div className="flex flex-col gap-0.5">
          <dt className="type-caption text-muted-foreground">Nội dung chuyển khoản</dt>
          <dd className="type-label tabular-nums">{p.paymentCode}</dd>
        </div>
      </dl>
    </div>
  )
}

export function LiveCostsPage({ viewer }) {
  const [state, setState] = useState({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    getFamilyPortalResidentsByResidentIdBalance({ path: { residentId: viewer.residentId } })
      .then(({ data }) => !cancelled && setState({ status: 'ready', data }))
      .catch((error) => !cancelled && setState({ status: 'error', error }))
    return () => {
      cancelled = true
    }
  }, [viewer.residentId])

  const header = (
    <PageHeader title="Chi phí, số dư" subtitle={`Số dư, tiền cọc và các khoản nộp, trừ của ${viewer.residentName}.`} />
  )
  if (state.status === 'loading') {
    return (
      <>
        {header}
        <Skeleton className="h-40 w-full" />
      </>
    )
  }
  if (state.status === 'error') {
    const denied = state.error instanceof ApiError && state.error.status === 403
    return (
      <>
        {header}
        <PortalCard>
          {denied ? (
            <p>
              Bạn chưa có quyền xem chi phí của {viewer.residentName}. Người đại diện có thể đề nghị viện cấp quyền.
            </p>
          ) : (
            <ProblemAlert problem={problemOf(state.error)} />
          )}
        </PortalCard>
      </>
    )
  }
  const b = state.data
  return (
    <>
      {header}
      <PortalCard title="Số dư hiện tại">
        <span className="type-metric tabular-nums">
          {b.balance < 0 ? `Còn nợ ${money(-b.balance)}` : money(b.balance)}
        </span>
        <span className="text-muted-foreground">
          Tiền cọc: <span className="tabular-nums">{money(b.depositBalance)}</span>
        </span>
      </PortalCard>
      <PortalCard title="Nộp tiền">
        <p className="text-muted-foreground">
          Nộp tiền mặt tại viện, hoặc chuyển khoản với nội dung là mã nộp tiền; số tiền do gia đình tự nhập.
        </p>
        <Instructions p={b.paymentInstructions} />
      </PortalCard>
      <PortalCard title="Các khoản đã ghi nhận">
        {b.transactions.length === 0 ? (
          <p className="text-muted-foreground">Chưa có khoản nộp, trừ nào.</p>
        ) : (
          <ul className="flex flex-col">
            {b.transactions.map((t) => (
              <li
                key={t.id}
                className="flex items-start justify-between gap-4 border-t py-2 first:border-t-0 first:pt-0"
              >
                <span className="flex flex-col gap-0.5">
                  <span>{TYPES[t.type] ?? t.type}</span>
                  <span className="type-caption text-muted-foreground">
                    {formatDate(t.at)}
                    {t.ledger === 'DEPOSIT' && ', sổ tiền cọc'}
                  </span>
                </span>
                <span className="type-label tabular-nums">{money(t.amount)}</span>
              </li>
            ))}
          </ul>
        )}
      </PortalCard>
      <LiveCostStatements residentId={viewer.residentId} />
      <LiveDocuments residentId={viewer.residentId} />
      {viewer.representative && <LivePurchaseConsent residentId={viewer.residentId} />}
    </>
  )
}
