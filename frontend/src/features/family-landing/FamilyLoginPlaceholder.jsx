import { NotDesignedState } from '@/common/PageState'
import { LeafMark } from '@/common/layout/AppShell'

// Chỗ của màn đăng nhập cổng người thân (feature 002). Chưa thiết kế nên chỉ báo rõ là chưa có.
export function FamilyLoginPlaceholder() {
  return (
    <div className="flex min-h-screen flex-col gap-10 bg-landing-sky px-4 py-6 text-foreground sm:px-8">
      <a href="/nguoi-than" className="mx-auto flex w-full max-w-[1120px] items-center gap-2.5 rounded-lg">
        <LeafMark />
        <span className="type-title">Elder_Care</span>
      </a>
      <div className="mx-auto w-full max-w-xl">
        <NotDesignedState label="Đăng nhập cổng người thân" />
      </div>
      <a href="/nguoi-than" className="mx-auto rounded type-body-sm underline underline-offset-4">
        Quay lại trang giới thiệu
      </a>
    </div>
  )
}
