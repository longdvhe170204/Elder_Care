import { useCallback, useEffect, useState } from 'react'
import { getCorrectionsByRecordTypeByOriginalRecordId } from '@/api'
import { Alert, AlertAction, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDateTime } from '@/common/dateTime'
import { CorrectionBadge } from './CorrectionBadge'
import { CorrectionForm } from './CorrectionForm'

// Khung xem và tạo đính chính dùng chung (000 T106; FR-028, FR-029, SC-007; hợp đồng GET/POST /corrections).
// - fields: các ô nội dung do module sở hữu bản ghi khai báo ([{ name, label }]); component không tự suy schema.
// - canCorrect: màn của module truyền theo lệnh được phép của bản ghi; chỉ để ẩn hoặc hiện form,
//   quyền thật do backend kiểm (403 CORRECTION_NOT_ALLOWED). Mặc định không hiện.

const TYPE_LABEL = { AMEND_CONTENT: 'Sửa nội dung', VOID: 'Hủy ghi nhận' }

function ContentList({ content, fields }) {
  return (
    <dl className="flex flex-col gap-1">
      {fields.map((f) => (
        <div key={f.name} className="flex flex-col">
          <dt className="type-caption text-muted-foreground">{f.label}</dt>
          <dd className="whitespace-pre-wrap">{content?.[f.name] ?? 'Chưa ghi nhận'}</dd>
        </div>
      ))}
    </dl>
  )
}

function Section({ id, title, children }) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-2">
      <h3 id={id} className="type-caption text-muted-foreground">
        {title}
      </h3>
      {children}
    </section>
  )
}

function CorrectionItem({ item, fields }) {
  return (
    <li className="flex flex-col gap-1 border-l-2 pl-3">
      <span className="type-label">
        Lần {item.sequenceNo}, {TYPE_LABEL[item.type]}
      </span>
      {item.type === 'AMEND_CONTENT' && <ContentList content={item.correctedContent} fields={fields} />}
      <p>Lý do: {item.reason}</p>
      <span className="type-caption text-muted-foreground tabular-nums">
        Ghi bởi {item.actor?.fullName ?? 'Chưa có tên'}, {formatDateTime(item.recordedAt)}, cách {item.creationMethod}
      </span>
    </li>
  )
}

function ChainBody({ chain, fields }) {
  const corrections = chain.corrections.toSorted((a, b) => a.sequenceNo - b.sequenceNo)
  return (
    <>
      <Section id="correction-current" title="Giá trị hiện hành">
        <ContentList content={chain.currentContent} fields={fields} />
      </Section>
      <Section id="correction-list" title="Các lần đính chính">
        {corrections.length > 0 ? (
          <ol className="flex flex-col gap-3">
            {corrections.map((item) => (
              <CorrectionItem key={item.id} item={item} fields={fields} />
            ))}
          </ol>
        ) : (
          <p>Chưa có lần đính chính nào.</p>
        )}
      </Section>
      {/* Bản gốc giữ nguyên, hiện nguyên văn (FR-025) */}
      <Section id="correction-original" title="Nội dung gốc">
        <ContentList content={chain.originalContent} fields={fields} />
      </Section>
    </>
  )
}

function SheetBody({ recordType, recordId, fields, canCorrect, onChanged }) {
  const [state, setState] = useState({ status: 'loading' })

  // refresh: tải lại sau khi lưu, giữ nội dung đang hiện cho tới khi có chuỗi mới.
  // Không phụ thuộc onChanged để màn gọi truyền hàm viết tại chỗ không làm tải lại chuỗi.
  const load = useCallback(
    async (refresh) => {
      if (!refresh) setState({ status: 'loading' })
      try {
        const { data } = await getCorrectionsByRecordTypeByOriginalRecordId({
          path: { recordType, originalRecordId: recordId },
        })
        setState({ status: 'ready', chain: data })
        return data
      } catch {
        setState({ status: 'error' })
        return null
      }
    },
    [recordType, recordId],
  )

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- tải chuỗi khi mở khung
    load(false)
  }, [load])

  async function refreshAfterSave() {
    const data = await load(true)
    if (data) onChanged?.(data)
  }

  const chain = state.chain
  return (
    <>
      <SheetHeader className="gap-2 pt-6">
        <SheetTitle className="type-title">Các lần đính chính</SheetTitle>
        <SheetDescription>Bản gốc giữ nguyên. Mỗi lần đính chính có lý do, người ghi và thời điểm.</SheetDescription>
        {chain && <CorrectionBadge corrected={chain.corrected} voided={chain.voided} />}
      </SheetHeader>
      <div className="flex flex-1 flex-col gap-5 overflow-y-auto px-4 pb-6 type-body-sm">
        {state.status === 'loading' && (
          <div aria-busy="true" className="flex flex-col gap-3">
            <span className="sr-only">Đang tải các lần đính chính</span>
            <Skeleton className="h-12" />
            <Skeleton className="h-20" />
            <Skeleton className="h-12" />
          </div>
        )}
        {state.status === 'error' && (
          <Alert>
            <AlertTitle>Không tải được các lần đính chính</AlertTitle>
            <AlertAction>
              <Button variant="outline" size="sm" onClick={() => load(false)}>
                Thử lại
              </Button>
            </AlertAction>
          </Alert>
        )}
        {chain && (
          <>
            <ChainBody chain={chain} fields={fields} />
            {chain.voided ? (
              <p>Bản ghi đã hủy ghi nhận, không đính chính thêm được.</p>
            ) : (
              canCorrect && (
                <section aria-labelledby="correction-new" className="flex flex-col gap-3 border-t pt-4">
                  <h3 id="correction-new" className="type-card-title">
                    Đính chính bản ghi
                  </h3>
                  <CorrectionForm
                    recordType={recordType}
                    recordId={recordId}
                    fields={fields}
                    onSaved={refreshAfterSave}
                  />
                </section>
              )
            )}
          </>
        )}
      </div>
    </>
  )
}

export function CorrectionHistorySheet({
  recordType,
  recordId,
  fields,
  open,
  onOpenChange,
  canCorrect = false,
  onChanged,
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent>
        {open && (
          <SheetBody
            key={`${recordType}/${recordId}`}
            recordType={recordType}
            recordId={recordId}
            fields={fields}
            canCorrect={canCorrect}
            onChanged={onChanged}
          />
        )}
      </SheetContent>
    </Sheet>
  )
}
