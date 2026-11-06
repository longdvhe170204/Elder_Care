import { useId, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatDate } from '@/common/dateTime'
import { sourceCreationBody, sourceSelectionKey, sourceSkipMessage } from './sourceSelection'

const quantities = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 3 })
const flagLabels = {
  INCLUDED_IN_PACKAGE: 'Thuộc gói', MISSING_UNIT_PRICE: 'Thiếu đơn giá',
  NOT_YET_MOVED_IN: 'Chưa vào ở', CREATED_IN_ADVANCE: 'Lập trước',
  AFTER_FINALIZED_PERIOD: 'Phát sinh thuộc kỳ đã chốt', EXCEEDS_CONSENT: 'Vượt số tiền đã đồng ý',
  URGENT_EXCESS: 'Mua gấp vượt số tiền đã đồng ý', AFFECTS_TERMINATION: 'Ảnh hưởng kết thúc lưu trú',
}

// Màn gọi API giữ lựa chọn/trạng thái gửi; xác nhận chỉ còn hiệu lực với dữ liệu đã xem.
export function BillingSourceResults({
  residentId, sourceType, sources = [], selection = [], onSelectionChange,
  canCreate = false, creating = false,
  onCreate, onOpenItem, skipped = [], dateRange,
}) {
  const prefix = useId()
  const [reviewed, setReviewed] = useState(null)
  const selected = new Set(selection)
  const rangeMode = sourceType === 'RESIDENCY_DAY'
  const body = sourceCreationBody({ residentId, sourceType, sources, selection, dateRange })
  const inRange = (source) => body && source.sourceType === sourceType
    && source.incurredOn >= body.dateRange.from && source.incurredOn <= body.dateRange.to
  const count = rangeMode ? sources.filter(inRange).length : body?.sourceUnits.length || 0
  const reviewedSources = sources.filter((source) => rangeMode ? inRange(source)
    : selected.has(sourceSelectionKey(residentId, source)))
  const fingerprint = body && JSON.stringify({ body, sources: reviewedSources.map((source) => ({
    sourceId: source.sourceId, billingUnit: source.billingUnit, incurredOn: source.incurredOn,
    description: source.description, quantity: source.quantity, unit: source.unit, factor: source.factor,
    flags: source.flags, existingItemId: source.existingItemId,
  })) })
  const confirmed = fingerprint !== null && reviewed === fingerprint

  function toggle(source, checked) {
    const visibleKeys = new Set(sources.map((row) => sourceSelectionKey(residentId, row)).filter((key) => key !== null))
    const next = new Set(selection.filter((key) => visibleKeys.has(key)))
    const key = sourceSelectionKey(residentId, source)
    if (checked) next.add(key)
    else next.delete(key)
    onSelectionChange?.([...next])
    setReviewed(null)
  }

  return (
    <section className="flex flex-col gap-4" aria-label="Kết quả tra cứu nguồn tính phí" aria-busy={creating}>
      <Table className="type-body-sm">
        <TableHeader>
          <TableRow>
            <TableHead scope="col">{rangeMode ? 'Phạm vi' : 'Chọn'}</TableHead>
            <TableHead scope="col">Ngày phát sinh</TableHead>
            <TableHead scope="col">Nguồn</TableHead>
            <TableHead scope="col">Mô tả</TableHead>
            <TableHead scope="col" className="text-right">Số lượng</TableHead>
            <TableHead scope="col">Đơn vị</TableHead>
            <TableHead scope="col" className="text-right">Hệ số</TableHead>
            <TableHead scope="col">Khoản hiện có</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {!sources.length && <TableRow><TableCell colSpan={8}>Không có nguồn trong kết quả tra cứu.</TableCell></TableRow>}
          {sources.map((source, index) => {
            const key = sourceSelectionKey(residentId, source)
            const checkboxId = `${prefix}-source-${index}`
            const flags = (source.flags || []).map((flag) => flagLabels[flag]).filter(Boolean)
            return (
              <TableRow key={key || `${source.sourceId}-${index}`}>
                <TableCell>
                  {rangeMode ? (inRange(source) ? 'Trong khoảng ngày' : '—') : <label htmlFor={checkboxId} className="inline-flex min-h-11 min-w-11 items-center justify-center p-3">
                    <Checkbox id={checkboxId} aria-label={`Chọn ${source.description || source.sourceId} ngày ${formatDate(source.incurredOn)}`}
                      checked={key !== null && selected.has(key)}
                      disabled={!canCreate || creating || key === null || source.sourceType !== sourceType || !onSelectionChange}
                      onCheckedChange={(checked) => toggle(source, checked)} />
                  </label>}
                </TableCell>
                <TableCell className="tabular-nums">{formatDate(source.incurredOn) || '—'}</TableCell>
                <TableCell>{source.sourceId}</TableCell>
                <TableCell className="whitespace-normal">
                  <p>{source.description || '—'}</p>
                  {!!flags.length && <p className="type-caption text-muted-foreground">{flags.join(' · ')}</p>}
                </TableCell>
                <TableCell className="text-right tabular-nums">{source.quantity == null ? '—' : quantities.format(source.quantity)}</TableCell>
                <TableCell>{source.unit || '—'}</TableCell>
                <TableCell className="text-right tabular-nums">{source.factor == null ? '—' : quantities.format(source.factor)}</TableCell>
                <TableCell>
                  {source.existingItemId == null ? '—' : onOpenItem
                    ? <Button type="button" variant="link" size="lg" onClick={() => onOpenItem(source.existingItemId)}>Xem khoản #{source.existingItemId}</Button>
                    : `Khoản #${source.existingItemId}`}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>

      <div className="flex flex-col gap-3">
        <p className="type-body-sm" role="status">Đã chọn {count} nguồn. Kiểm tra ngày, số lượng và đơn vị trước khi lập.</p>
        {rangeMode && body && <p className="type-body-sm">Khoảng ngày: {formatDate(body.dateRange.from)} – {formatDate(body.dateRange.to)}.</p>}
        <label htmlFor={`${prefix}-confirmed`} className="type-body-sm flex min-h-11 items-center gap-3">
          <Checkbox id={`${prefix}-confirmed`} checked={confirmed}
            disabled={!canCreate || creating || !body || !count}
            onCheckedChange={(checked) => setReviewed(checked ? fingerprint : null)} />
          Tôi đã kiểm tra các nguồn đã chọn
        </label>
        <Button type="button" size="lg" className="self-start"
          disabled={!canCreate || creating || !body || !count || !confirmed || !onCreate}
          onClick={() => { setReviewed(null); onCreate?.(body) }}>
          {creating ? 'Đang lập khoản…' : `Lập ${count} khoản chi phí`}
        </Button>
      </div>

      {!!skipped.length && <div role="status" className="type-body-sm flex flex-col gap-2">
        <h3 className="type-card-title">Nguồn bị bỏ qua ({skipped.length})</h3>
        <ul className="list-disc space-y-1 pl-5">
          {skipped.map((item, index) => <li key={`${item.source || item.itemId}-${index}`}>
            {item.source || (item.itemId == null ? 'Nguồn đã chọn' : `Khoản #${item.itemId}`)}: {sourceSkipMessage(item)}
          </li>)}
        </ul>
      </div>}
    </section>
  )
}
