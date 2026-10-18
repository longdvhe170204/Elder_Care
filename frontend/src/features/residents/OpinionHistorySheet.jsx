import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { SourceTag } from '@/common/SourceTag'
import { cn } from '@/lib/utils'

// Lịch sử ý kiến của một nhóm sở thích (DESIGN.md 2.2, màn A; Q-347, Q-363, BR-M01-14).
// Mục đã ghi không sửa, không xóa; thay đổi bằng mục mới, mục cũ chuyển xuống Lịch sử.

// Thứ tự hiện: tiếng người cao tuổi trước, rồi người thân, rồi nhân viên.
const SOURCE_ORDER = { resident: 0, family: 1, staff: 2 }

function PreferenceItem({ item, past }) {
  return (
    <li className="flex flex-col gap-1">
      <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <span className={cn('font-medium', past && 'text-muted-foreground')}>{item.level}</span>
        <SourceTag source={item.source} relative={item.relative} />
      </span>
      <span className="type-caption text-muted-foreground tabular-nums">
        Ghi bởi {item.recordedBy}, {item.recordedAt}
        {item.replacedAt && `; thay bằng mục mới ngày ${item.replacedAt}`}
      </span>
    </li>
  )
}

// Người cao tuổi và người thân nói khác nhau thì là hai dòng riêng, không gộp, không chọn hộ.
export function PreferenceList({ items, past }) {
  return (
    <ul className="flex flex-col gap-2 border-l-2 pl-3">
      {items
        .toSorted((a, b) => SOURCE_ORDER[a.source] - SOURCE_ORDER[b.source])
        .map((item) => (
          <PreferenceItem key={item.source + (item.relative ?? '') + item.recordedAt} item={item} past={past} />
        ))}
    </ul>
  )
}

function HistoryBody({ group, current, history, canRecord }) {
  const [askedNew, setAskedNew] = useState(false)
  return (
    <>
      <SheetHeader className="gap-1 pt-6">
        <SheetTitle className="type-title">{group}</SheetTitle>
        <SheetDescription>Sở thích, mỗi nguồn một ý kiến đang hiệu lực. Mục đã ghi không sửa, không xóa.</SheetDescription>
      </SheetHeader>
      <div className="flex flex-col gap-5 px-4 type-body-sm">
        <section className="flex flex-col gap-2">
          <h3 className="type-caption text-muted-foreground">Đang hiệu lực</h3>
          <PreferenceList items={current} />
        </section>
        <section className="flex flex-col gap-2">
          <h3 className="type-caption text-muted-foreground">Lịch sử</h3>
          {history.length > 0 ? <PreferenceList items={history} past /> : <p>Chưa có mục cũ.</p>}
        </section>
      </div>
      {/* Trưởng tầng, Điều dưỡng, Nhân viên chăm sóc ghi sở thích trong phạm vi (8.10); quyền thật do backend trả */}
      {canRecord && (
        <SheetFooter>
          <Button variant="outline" className="h-11" onClick={() => setAskedNew(true)}>
            Ghi ý kiến mới
          </Button>
          <p role="status" className="type-caption text-muted-foreground">
            {askedNew && 'Form ghi ý kiến: chưa có bản mẫu.'}
          </p>
        </SheetFooter>
      )}
    </>
  )
}

export function OpinionHistorySheet({ group, current, history, canRecord, onOpenChange }) {
  return (
    <Sheet open={!!group} onOpenChange={onOpenChange}>
      <SheetContent>
        {group && <HistoryBody key={group} group={group} current={current} history={history} canRecord={canRecord} />}
      </SheetContent>
    </Sheet>
  )
}
