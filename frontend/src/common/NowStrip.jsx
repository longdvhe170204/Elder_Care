// Dải "Cần làm ngay" (DESIGN.md mục 5, 7.2 now-strip): một dòng trên bề mặt trắng ngay dưới đầu màn; nhãn đỏ kèm số,
// rồi các mục nối bằng dấu phẩy, bấm một mục mở khung chi tiết của nó; bên phải chữ phụ tóm tắt việc sắp tới.
// Chỉ hiện số liệu, cảnh báo đã có, không tự gợi ý (Q-306, Q-307). Không có mục nào thì không có dải.
// items: [{ key, label, onSelect }]
export function NowStrip({ label = 'Cần làm ngay', items, summary }) {
  if (!items?.length) return null
  return (
    <section
      aria-label={label}
      className="flex min-h-12 flex-wrap items-center gap-x-3 gap-y-1 rounded-lg border bg-card px-4 py-2"
    >
      <span className="type-label text-destructive">
        {label} ({items.length})
      </span>
      <span className="flex flex-1 flex-wrap items-center type-body-sm">
        {items.map((item, i) => (
          <span key={item.key}>
            <button type="button" onClick={item.onSelect} className="rounded-sm underline-offset-4 hover:underline">
              {item.label}
            </button>
            {i < items.length - 1 && ', '}
          </span>
        ))}
      </span>
      {summary && <span className="type-body-sm text-muted-foreground">{summary}</span>}
    </section>
  )
}
