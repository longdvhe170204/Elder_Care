// Đầu trang dùng chung (DESIGN.md mục 5, công thức page-header): đặt thẳng trên nền trang, không bọc thẻ;
// tiêu đề, dòng phụ bên trái, ô bên phải (giờ hoặc ô tìm), phần dưới cho thanh lọc.
export function PageHeader({ title, subtitle, aside, children }) {
  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="type-page text-primary">{title}</h1>
          {subtitle && <p className="text-muted-foreground">{subtitle}</p>}
        </div>
        {aside}
      </div>
      {children}
    </section>
  )
}

export function ClockAside({ time, caption }) {
  return (
    <div className="flex flex-col items-end">
      <span className="type-clock text-accent-foreground">{time}</span>
      <span className="type-body-sm text-muted-foreground">{caption}</span>
    </div>
  )
}
