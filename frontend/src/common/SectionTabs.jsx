import { cn } from 'cn'
import { toggleVariants } from '@/components/ui/toggle'

// Điều hướng phụ trong một màn (DESIGN.md 7.2 filter-bar): cùng kiểu với thanh lọc ToggleGroup variant "filter"
// (tab gạch chân từ 768px, chip cuộn ngang dưới 768px), cả hàng có đường kẻ dưới. `semantics`: "tab" cho
// role=tab/aria-selected, "pressed" cho nút aria-pressed.
export function SectionTabs({ label, tabs, value, onChange, semantics = 'tab', className }) {
  const asTabs = semantics === 'tab'
  return (
    <div
      role={asTabs ? 'tablist' : 'group'}
      aria-label={label}
      className={cn('flex gap-2 overflow-x-auto md:gap-5 md:border-b', className)}
    >
      {tabs.map(([key, text]) => {
        const active = value === key
        return (
          <button
            key={key}
            type="button"
            role={asTabs ? 'tab' : undefined}
            aria-selected={asTabs ? active : undefined}
            aria-pressed={asTabs ? undefined : active}
            data-state={active ? 'on' : 'off'}
            className={cn(toggleVariants({ variant: 'filter', size: 'lg' }), 'h-11 px-4')}
            onClick={() => onChange(key)}
          >
            {text}
          </button>
        )
      })}
    </div>
  )
}
