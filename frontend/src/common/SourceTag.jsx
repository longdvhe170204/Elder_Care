import { cva } from 'class-variance-authority'

// Nhãn người nói (DESIGN.md mục 2.2, source-tag; Q-347, BR-M01-14): mỗi mong muốn, sở thích, lựa chọn
// cho biết ai nói. Xanh trời là tiếng của người cao tuổi, xanh chàm là người thân, nền phụ là nhân viên
// quan sát; xanh lá chỉ cho việc viện đã làm xong (trang giới thiệu cổng người thân).
const sourceTagVariants = cva('inline-flex h-6 shrink-0 items-center self-start rounded-full px-2.5 type-caption', {
  variants: {
    source: {
      resident: 'bg-accent text-accent-foreground',
      family: 'bg-info text-info-foreground',
      staff: 'bg-muted text-muted-foreground',
      done: 'bg-success text-success-foreground',
    },
  },
})

// Không truyền chữ thì dùng chữ mặc định theo nguồn; người thân luôn ghi rõ là ai (FR-023g của 001).
export function SourceTag({ source, relative, children }) {
  const label =
    children ??
    { resident: 'Theo lời người cao tuổi', family: `Theo ${relative}`, staff: 'Nhân viên quan sát' }[source]
  return <span className={sourceTagVariants({ source })}>{label}</span>
}
