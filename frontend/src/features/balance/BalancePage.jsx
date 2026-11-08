import { useState } from 'react'
import { Skeleton } from '@/components/ui/skeleton'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { PageHeader } from '@/common/PageHeader'
import { PermissionDeniedState } from '@/common/PageState'
import { usePermissions } from '@/common/permissions/permissions'
import { BalanceExports } from './BalanceExports'
import { BalanceStatuses } from './BalanceStatuses'
import { PaymentNotices } from './PaymentNotices'
import { PaymentRecording } from './PaymentRecording'
import { PendingTransactions } from './PendingTransactions'
import { ResidentBalance } from './ResidentBalance'
import { ResidentPicker } from './ResidentPicker'

// Màn Số dư, thu chi (017 T015, T022 của A; T027, T033, T036, T039; Phụ lục 27 dòng "Số dư, thu chi", "Phiếu báo phí", "Bảng quyết toán",
// "Sao kê, báo cáo thu chi": Kế toán thực hiện, Quản lý viện duyệt và xem, Hành chính xem số dư). Các phần chuyển bằng nút
// lọc: Ghi nhận tiền nộp (Kế toán lập), Tình trạng số dư, Phiếu chờ phát hành, Giao dịch chờ duyệt, Xuất sao kê và báo
// cáo, Theo người cao tuổi (số dư, giao dịch, quyết toán, phiếu đã phát hành). Người thân xem số dư, chứng từ ở cổng người thân (012), không ở màn này.

const SECTIONS = [
  { value: 'payments', label: 'Ghi nhận tiền nộp', function: 'BALANCE_LEDGER', operation: 'CREATE' },
  { value: 'statuses', label: 'Tình trạng số dư', function: 'BALANCE_LEDGER' },
  { value: 'notices', label: 'Phiếu chờ phát hành', function: 'PAYMENT_NOTICE' },
  { value: 'pending', label: 'Giao dịch chờ duyệt', function: 'BALANCE_LEDGER' },
  { value: 'exports', label: 'Xuất sao kê, báo cáo', function: 'STATEMENT_REPORT' },
  { value: 'resident', label: 'Theo người cao tuổi', function: 'BALANCE_LEDGER' },
]

export function BalancePage() {
  const { status, hasFunction, can } = usePermissions()
  const [section, setSection] = useState('resident')
  const [resident, setResident] = useState(null)

  if (status === 'loading') return <Skeleton className="mx-auto h-32 w-full max-w-[1600px]" />
  if (!['BALANCE_LEDGER', 'PAYMENT_NOTICE', 'STATEMENT_REPORT', 'SETTLEMENT_RECORD'].some(hasFunction))
    return <PermissionDeniedState label="Số dư, thu chi" />
  const sections = SECTIONS.filter((s) => (s.operation ? can(s.function, s.operation) : hasFunction(s.function)))
  const current = sections.some((s) => s.value === section) ? section : sections[0].value

  return (
    <div className="mx-auto flex w-full max-w-[1600px] flex-col gap-5">
      <PageHeader
        title="Số dư, thu chi"
        subtitle="Số dư, giao dịch, quyết toán, phiếu báo phí và sao kê của người cao tuổi nội trú."
      >
        <ToggleGroup variant="filter" value={[current]} onValueChange={(next) => next[0] && setSection(next[0])}>
          {sections.map((s) => (
            <ToggleGroupItem key={s.value} value={s.value}>
              {s.label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </PageHeader>
      {current === 'payments' && <PaymentRecording />}
      {current === 'statuses' && <BalanceStatuses />}
      {current === 'notices' && <PaymentNotices />}
      {current === 'pending' && <PendingTransactions />}
      {current === 'exports' && <BalanceExports />}
      {current === 'resident' && (
        <div className="flex flex-col gap-5">
          <ResidentPicker onPick={setResident} />
          {resident && (
            <ResidentBalance key={resident.residentId} residentId={resident.residentId} fullName={resident.fullName} />
          )}
        </div>
      )}
    </div>
  )
}
