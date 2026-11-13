import { cn } from 'cn'

// Hàng ô lọc của một màn (DESIGN.md mục 5): ô lọc không kéo dài hết màn; mỗi ô rộng 224–320px, dưới 640px
// mỗi ô một hàng. Dùng cho ô tìm, ô chọn kỳ, ô chọn ca đặt trên danh sách.
export function FilterBar({ className, ...props }) {
  return (
    <div
      className={cn('flex flex-wrap items-end gap-3 *:w-full sm:*:w-auto sm:*:min-w-56 sm:*:max-w-80 sm:*:flex-1', className)}
      {...props}
    />
  )
}
