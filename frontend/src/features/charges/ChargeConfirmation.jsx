import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'
import { ProblemAlert } from '@/common/ProblemAlert'
import { money } from './chargeLabels'

const ACTIONS = {
  CHECK: ['Xác nhận kiểm tra chi phí', 'Xác nhận kiểm tra'],
  APPROVE: ['Xác nhận duyệt chi phí', 'Xác nhận duyệt'],
  SUBMIT: ['Xác nhận gửi chốt bảng', 'Xác nhận gửi chốt'],
  FINALIZE: ['Xác nhận chốt bảng', 'Xác nhận chốt bảng'],
  RETURN_STATEMENT: ['Xác nhận trả lại bảng', 'Xác nhận trả lại bảng'],
  RETURN: ['Xác nhận trả lại khoản', 'Xác nhận trả lại khoản'],
  UNCHECK: ['Xác nhận bỏ kiểm tra', 'Xác nhận bỏ kiểm tra'],
  CANCEL: ['Xác nhận hủy khoản', 'Xác nhận hủy khoản'],
}

export function ChargeConfirmation({
  intent,
  statement,
  pending,
  stale,
  invalidReason,
  problem,
  onReason,
  onConfirm,
  onReload,
  onClose,
}) {
  const required = ['RETURN_STATEMENT', 'RETURN', 'UNCHECK', 'CANCEL'].includes(intent.operation) || intent.excess
  const reasonLabel = intent.excess ? 'Lý do chấp nhận phần vượt (bắt buộc)' : 'Lý do (bắt buộc)'
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open && !pending) onClose()
      }}
    >
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>{ACTIONS[intent.operation][0]}</DialogTitle>
          <DialogDescription>
            Bảng #{statement.id}, người cao tuổi mã {statement.residentId}. Hãy xem lại trước khi thực hiện.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={(event) => {
            event.preventDefault()
            onConfirm(required)
          }}
          className="flex flex-col gap-4"
        >
          <p className="type-body-sm">
            Tổng bảng: <span className="tabular-nums">{money(statement.total)}</span>.
          </p>
          {intent.ids?.length > 0 && (
            <p className="type-body-sm">Khoản được chọn: {intent.ids.map((id) => `#${id}`).join(', ')}.</p>
          )}
          {intent.filter && (
            <p className="type-body-sm">
              Áp dụng theo bộ lọc hiện tại của bảng. Khoản không đủ điều kiện sẽ bị bỏ qua kèm lý do.
            </p>
          )}
          {intent.operation === 'FINALIZE' && (
            <p className="type-body-sm">
              Sau khi chốt, bảng và khoản đã chốt không sửa trực tiếp. Sai sót xử lý bằng khoản điều chỉnh.
            </p>
          )}
          {required && (
            <FieldGroup>
              <Field data-invalid={invalidReason || undefined}>
                <FieldLabel htmlFor="charge-command-reason">{reasonLabel}</FieldLabel>
                <Textarea
                  id="charge-command-reason"
                  maxLength={2000}
                  value={intent.reason}
                  disabled={pending}
                  aria-required="true"
                  aria-invalid={invalidReason || undefined}
                  onChange={(event) => onReason(event.target.value)}
                />
                {invalidReason && <FieldError>Cần nhập lý do trước khi thực hiện.</FieldError>}
              </Field>
            </FieldGroup>
          )}
          <ProblemAlert problem={problem}>
            {stale && (
              <>
                <p>Dữ liệu bảng đã thay đổi. Tải lại và xem thông tin mới trước khi xác nhận.</p>
                <Button type="button" variant="outline" disabled={pending} onClick={onReload}>
                  Tải lại bảng
                </Button>
              </>
            )}
          </ProblemAlert>
          <DialogFooter>
            <Button type="button" variant="outline" disabled={pending} onClick={onClose}>
              Quay lại
            </Button>
            <Button type="submit" disabled={pending || stale}>
              {pending ? 'Đang thực hiện…' : ACTIONS[intent.operation][1]}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
