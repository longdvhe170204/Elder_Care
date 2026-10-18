// Đầu trang dùng chung (DESIGN.md 2.1.0, công thức page-header): tiêu đề, dòng phụ,
// ô bên phải (giờ, thao tác), và phần dưới cho thanh lọc hoặc ô tìm.
export function PageHeader({ title, subtitle, aside, children }) {
  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-4 md:p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
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
      <span className="type-caption text-muted-foreground">{caption}</span>
    </div>
  )
}
