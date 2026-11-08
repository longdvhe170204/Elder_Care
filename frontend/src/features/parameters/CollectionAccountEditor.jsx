import { useEffect, useState } from 'react'
import { getBanks } from '@/api'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

// Giá trị CFG-M11-10, tài khoản thu của viện (017 T045; research E-04): ngân hàng chọn từ danh mục mã BIN NAPAS
// (GET /banks của 017), số tài khoản, chủ tài khoản. Backend kiểm khi lưu (BIN còn hiệu lực, 6 → 19 chữ số, tên không
// rỗng); 422 hiện ở ô giá trị của form.

const SELECT =
  'h-10 w-full rounded-[10px] border border-input bg-background px-3 text-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:opacity-50'

export function CollectionAccountEditor({ value, onChange, disabled }) {
  const [banks, setBanks] = useState([])
  const [failed, setFailed] = useState(false)
  const v = value ?? { bin: null, accountNumber: null, accountHolder: null }
  const set = (key) => (e) => onChange({ ...v, [key]: e.target.value === '' ? null : e.target.value })

  useEffect(() => {
    let cancelled = false
    getBanks()
      .then(({ data }) => !cancelled && setBanks(data))
      .catch(() => !cancelled && setFailed(true))
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="flex flex-col gap-3">
      <label className="flex flex-col gap-1">
        <span className="type-label">Ngân hàng</span>
        <select className={cn(SELECT)} value={v.bin ?? ''} onChange={set('bin')} disabled={disabled}>
          <option value="">Chọn ngân hàng</option>
          {banks.map((b) => (
            <option key={b.bin} value={b.bin}>
              {`${b.shortName} (${b.bin})`}
            </option>
          ))}
        </select>
        {failed && <span className="type-caption text-muted-foreground">Không tải được danh mục ngân hàng.</span>}
      </label>
      <label className="flex flex-col gap-1">
        <span className="type-label">Số tài khoản</span>
        <Input value={v.accountNumber ?? ''} onChange={set('accountNumber')} inputMode="numeric" disabled={disabled} />
      </label>
      <label className="flex flex-col gap-1">
        <span className="type-label">Chủ tài khoản</span>
        <Input value={v.accountHolder ?? ''} onChange={set('accountHolder')} disabled={disabled} />
      </label>
    </div>
  )
}
