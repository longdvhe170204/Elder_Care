import { cn } from '@/lib/utils'

// Ảnh đại diện chữ cái (DESIGN.md, resident-avatar): chữ cái đầu của tên gọi (từ cuối của họ tên),
// màu cố định theo mã người cao tuổi trong 4 cặp --avatar-1 … --avatar-4.
const TONES = [
  'bg-(--avatar-1) text-(--avatar-1-foreground)',
  'bg-(--avatar-2) text-(--avatar-2-foreground)',
  'bg-(--avatar-3) text-(--avatar-3-foreground)',
  'bg-(--avatar-4) text-(--avatar-4-foreground)',
]

function initialOf(name) {
  const lastWord = name.trim().split(/\s+/).pop() ?? ''
  return lastWord.charAt(0).toUpperCase()
}

function toneOf(code) {
  let sum = 0
  for (const ch of code) sum += ch.charCodeAt(0)
  return TONES[sum % TONES.length]
}

export function ResidentAvatar({ name, code, initials, className }) {
  return (
    <span
      aria-hidden="true"
      className={cn('flex size-8 shrink-0 items-center justify-center rounded-full type-caption', toneOf(code), className)}
    >
      {initials ?? initialOf(name)}
    </span>
  )
}
