import { Button } from '@/components/ui/button'
import { FieldError } from '@/components/ui/field'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDateTime } from '@/common/dateTime'
import { CreateForm } from './RecipientConfirmations'

// Chọn hoặc lập xác nhận người nhận khác ngay trong màn Trả đồ (013 FR-017, FR-018, Q-253; BF-14 nhánh ngoại lệ). Chỉ
// Hành chính (FR-016a). Danh sách lọc bằng confirmationFit (chép luật backend) để nhân viên chọn, không gõ mã; backend
// vẫn kiểm khi lưu trả. Lập ngay dùng lại CreateForm với người nhận, giấy tờ, đồ đã nhập ở form trả.

export function ReturnConfirmation({
  list,
  matches,
  pending,
  chosenId,
  onChoose,
  itemLabel,
  createInitial,
  createHint,
  creating,
  onCreating,
  onCreated,
  residentId,
  belongings,
  locationName,
  error,
}) {
  const chosen = matches.find((m) => m.confirmation.id === chosenId)
  return (
    <section className="flex min-w-0 flex-col gap-3 rounded-lg border p-3" aria-label="Chọn xác nhận người nhận khác">
      <h3 className="type-label">Xác nhận người nhận khác</h3>
      {list.status === 'idle' && <p className="type-body-sm text-muted-foreground">Đang tải xác nhận…</p>}
      {list.status === 'error' && <ProblemAlert problem={list.problem} />}
      {matches.length > 0 && (
        <ul className="flex flex-col gap-2">
          {matches.map(({ confirmation: c }) => (
            <li key={c.id}>
              <Button
                type="button"
                variant="outline"
                className="h-auto min-h-11 w-full flex-col items-start gap-1 py-2 text-left whitespace-normal"
                aria-pressed={c.id === chosenId}
                aria-label={`Chọn xác nhận mã ${c.id}`}
                onClick={() => onChoose(c.id === chosenId ? null : c.id)}
              >
                <span className="type-label">
                  Mã {c.id}: {c.recipientName}, giấy tờ {c.recipientIdDocument}
                </span>
                <span className="type-caption text-muted-foreground">
                  {c.belongingIds.map(itemLabel).join('; ')}
                  {c.expiresAt && `. Hết hạn ${formatDateTime(c.expiresAt)}`}
                </span>
              </Button>
            </li>
          ))}
        </ul>
      )}
      {list.status === 'ready' && matches.length === 0 && (
        <p className="type-body-sm text-muted-foreground">
          Chưa có xác nhận phù hợp với người nhận và các đồ đang chọn.
        </p>
      )}
      {pending.map(({ confirmation: c }) => (
        <p key={c.id} className="type-body-sm text-warning-foreground">
          Đang chờ xác nhận, chưa thể trả đồ: mã {c.id} ({c.recipientName}). Trả khi người đại diện đã xác nhận.
        </p>
      ))}
      {chosen?.extraIds.length > 0 && (
        <p className="type-body-sm text-warning-foreground">
          Xác nhận này còn gồm {chosen.extraIds.map(itemLabel).join('; ')}. Sau lần trả này xác nhận chuyển Đã dùng; các
          đồ đó cần xác nhận mới, hoặc tick thêm để trả cùng lần.
        </p>
      )}
      {list.status === 'ready' && !chosen && (
        <p className="type-body-sm text-warning-foreground">
          Chưa chọn xác nhận người nhận khác phù hợp: lệnh trả sẽ bị chặn và ghi lần chặn.
        </p>
      )}
      {list.status === 'ready' && matches.length === 0 && !creating && (
        <div className="flex flex-col gap-1">
          {createHint ? (
            <p className="type-body-sm text-muted-foreground">{createHint}</p>
          ) : (
            <Button type="button" variant="outline" className="h-11 self-start" onClick={() => onCreating(true)}>
              Lập xác nhận ngay
            </Button>
          )}
        </div>
      )}
      {creating && (
        <>
          <CreateForm
            embedded
            residentId={residentId}
            belongings={belongings}
            locationName={locationName}
            initial={createInitial}
            onCreated={onCreated}
          />
          <Button type="button" variant="ghost" className="h-11 self-start" onClick={() => onCreating(false)}>
            Đóng phần lập
          </Button>
        </>
      )}
      {error && <FieldError>{error}</FieldError>}
    </section>
  )
}
