import { Toggle as TogglePrimitive } from "@base-ui/react/toggle"
import { cva } from "class-variance-authority";
import { cn } from "cn"

const toggleVariants = cva(
  "group/toggle inline-flex items-center justify-center gap-1 rounded-lg text-sm font-medium whitespace-nowrap transition-all outline-none hover:bg-muted hover:text-foreground focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none disabled:opacity-50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 aria-pressed:bg-accent aria-pressed:text-accent-foreground data-[state=on]:bg-accent data-[state=on]:text-accent-foreground dark:aria-invalid:ring-destructive/40 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        default: "bg-transparent",
        outline: "border border-input bg-transparent hover:bg-muted",
        // filter-bar (DESIGN.md 7.2): dưới 768px chip bo tròn, chip chọn nền xanh đêm chữ trắng; từ 768px tab gạch
        // chân, mục chọn chữ chính và gạch 2px xanh đêm
        filter:
          "shrink-0 rounded-full border border-border bg-card px-3.5 text-foreground aria-pressed:border-primary aria-pressed:bg-primary aria-pressed:text-primary-foreground data-[state=on]:bg-primary data-[state=on]:text-primary-foreground md:rounded-none md:border-0 md:border-b-2 md:border-transparent md:bg-transparent md:px-1 md:text-muted-foreground md:hover:bg-transparent md:hover:text-foreground md:aria-pressed:border-primary md:aria-pressed:bg-transparent md:aria-pressed:text-foreground md:data-[state=on]:bg-transparent md:data-[state=on]:text-foreground",
      },
      size: {
        default:
          "h-8 min-w-8 px-2.5 has-data-[icon=inline-end]:pr-2 has-data-[icon=inline-start]:pl-2",
        sm: "h-7 min-w-7 rounded-[min(var(--radius-md),12px)] px-2.5 text-[0.8rem] has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 [&_svg:not([class*='size-'])]:size-3.5",
        lg: "h-9 min-w-9 px-2.5 has-data-[icon=inline-end]:pr-2 has-data-[icon=inline-start]:pl-2",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

function Toggle({
  className,
  variant = "default",
  size = "default",
  ...props
}) {
  return (
    <TogglePrimitive
      data-slot="toggle"
      className={cn(toggleVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Toggle, toggleVariants }
