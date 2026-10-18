import { useState } from 'react'
import { cva } from 'class-variance-authority'
import { ImageIcon, LockKeyhole, Menu, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { LeafMark } from '@/common/layout/AppShell'
import { SourceTag as Tag } from '@/common/SourceTag'

// Trang giới thiệu cổng người thân (DESIGN.md mục 2.1). Mọi chức năng nêu ở đây lấy từ nghiệp vụ
// (5.6, 8.1 → 8.10, 9.3 → 9.5, 14.1 → 14.7, 19); bản tin là dữ liệu minh họa, không có dữ liệu thật.

const LOGIN_URL = '/dang-nhap'

const NAV = [
  { href: '#vien', label: 'Viện' },
  { href: '#cham-soc', label: 'Chăm sóc' },
  { href: '#doi-song', label: 'Đời sống' },
  { href: '#mot-tuan', label: 'Một tuần mẫu' },
  { href: '#gia-dinh', label: 'Gia đình' },
  { href: '#cong-nguoi-than', label: 'Cổng người thân' },
]

// Nền từng section (token --landing-*). Chữ trên nền trời chỉ dùng màu chữ chính.
const band = cva('scroll-mt-16 px-4 py-20 sm:px-8 md:py-28', {
  variants: {
    tone: {
      sky: 'bg-landing-sky',
      ivory: 'bg-landing-ivory',
      beige: 'bg-landing-beige',
      white: 'bg-card',
      navy: 'bg-primary text-primary-foreground',
    },
  },
})

function Band({ id, tone, children }) {
  return (
    <section id={id} aria-labelledby={`${id}-title`} className={band({ tone })}>
      <div className="mx-auto flex max-w-[1120px] flex-col gap-12">{children}</div>
    </section>
  )
}

// Tiêu đề section và đoạn mở, căn trái, dòng ngắn.
function Intro({ id, title, children }) {
  return (
    <div className="flex max-w-[60ch] flex-col gap-4">
      <h2 id={`${id}-title`} className="type-section">
        {title}
      </h2>
      {children && <p className="type-lead">{children}</p>}
    </div>
  )
}

// Chỗ cho ảnh thực tế của viện. Chưa có ảnh được phép sử dụng nên không dùng ảnh kho (DESIGN.md mục 2).
function PhotoSlot({ caption, className }) {
  return (
    <figure className={className}>
      <div
        role="img"
        aria-label={`Chỗ đặt ảnh: ${caption}`}
        className="flex aspect-[3/2] flex-col items-center justify-center gap-2 rounded-2xl bg-landing-photo p-6 text-center"
      >
        <ImageIcon className="size-6 text-muted-foreground" aria-hidden="true" />
        <span className="max-w-[28ch] type-body-sm text-muted-foreground">Chờ ảnh thực tế được phép sử dụng</span>
      </div>
      <figcaption className="pt-2 type-body-sm">{caption}</figcaption>
    </figure>
  )
}

// Danh sách điểm có tiêu đề ngắn, dùng cho các section giới thiệu.
function Points({ items, wide = false }) {
  return (
    <dl className={wide ? 'grid gap-x-10 gap-y-8 sm:grid-cols-2 lg:grid-cols-3' : 'grid gap-x-10 gap-y-8 sm:grid-cols-2'}>
      {items.map((item) => (
        <div key={item.title} className="flex flex-col gap-2 border-t border-foreground/15 pt-4">
          <dt className="type-card-title">{item.title}</dt>
          <dd className="type-reading">{item.text}</dd>
        </div>
      ))}
    </dl>
  )
}

function Entry({ voice, label, children }) {
  return (
    <li className="flex flex-col gap-1.5 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
      <span className="type-reading">{children}</span>
      {voice && <Tag source={voice}>{label}</Tag>}
    </li>
  )
}

function HealthLock() {
  return (
    <p className="flex items-start gap-2 rounded-lg bg-muted px-3 py-2 type-body-sm text-muted-foreground">
      <LockKeyhole className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      Phần sức khỏe chỉ hiện trong cổng, với người thân được bà đồng ý chia sẻ.
    </p>
  )
}

// Một hàng của bản tin minh họa: lời giải thích bên trái, phần bản tin bên phải.
// Trên máy tính các phần nối thành một tờ; trên điện thoại mỗi phần là một thẻ riêng.
const sheetPart = cva('flex flex-col gap-4 border-border bg-card p-5 text-card-foreground sm:p-6', {
  variants: {
    position: {
      first: 'rounded-2xl border md:rounded-b-none',
      middle: 'rounded-2xl border md:rounded-none md:border-t-0',
      last: 'rounded-2xl border md:rounded-t-none md:border-t-0',
    },
  },
})

function BulletinRow({ title, note, label, position, children }) {
  return (
    <div className="grid gap-4 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-10">
      <div className="flex flex-col gap-3 md:py-6">
        <h3 className="type-title">{title}</h3>
        <p className="max-w-[44ch] type-reading">{note}</p>
      </div>
      <section aria-label={label} className={sheetPart({ position })}>
        <h4 className="type-card-title">{label}</h4>
        {children}
      </section>
    </div>
  )
}

function Header() {
  const [open, setOpen] = useState(false)
  return (
    <header className="sticky top-0 z-10 border-b border-foreground/10 bg-landing-sky px-4 sm:px-8">
      <div className="mx-auto flex h-16 max-w-[1120px] items-center gap-6">
        <a href="#dau-trang" className="flex items-center gap-2.5 rounded-lg">
          <LeafMark />
          <span className="type-title">Elder_Care</span>
        </a>
        <nav aria-label="Các phần của trang" className="ml-auto hidden lg:block">
          <ul className="flex gap-1">
            {NAV.map((item) => (
              <li key={item.href}>
                <a href={item.href} className="inline-flex h-10 items-center rounded-full px-3 type-body-sm hover:bg-card/40">
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
        <Button className="ml-auto lg:ml-0" nativeButton={false} render={<a href={LOGIN_URL} />}>
          Đăng nhập
        </Button>
        <Button
          variant="secondary"
          size="icon"
          className="lg:hidden"
          aria-expanded={open}
          aria-controls="menu-trang"
          aria-label={open ? 'Đóng danh sách các phần' : 'Mở danh sách các phần'}
          onClick={() => setOpen(!open)}
        >
          {open ? <X aria-hidden="true" /> : <Menu aria-hidden="true" />}
        </Button>
      </div>
      {open && (
        <nav id="menu-trang" aria-label="Các phần của trang" className="mx-auto max-w-[1120px] pb-4 lg:hidden">
          <ul className="flex flex-col rounded-2xl border border-border bg-card p-2 text-card-foreground">
            {NAV.map((item) => (
              <li key={item.href}>
                <a
                  href={item.href}
                  onClick={() => setOpen(false)}
                  className="flex h-11 items-center rounded-xl px-3 type-body hover:bg-muted"
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </header>
  )
}

const CARE_STEPS = [
  { title: 'Đầu ca nhận bàn giao', text: 'Ca trước bàn giao việc còn dở và cảnh báo chưa xử lý; ca sau xác nhận đã nhận.' },
  {
    title: 'Trong ca làm theo checklist',
    text: 'Nhân viên được phân công làm và ghi lại từng việc chăm sóc; việc quá giờ được nhắc và báo lên.',
  },
  {
    title: 'Theo dõi sức khỏe theo lịch',
    text: 'Điều dưỡng đo chỉ số theo lịch bác sĩ đặt; chỉ số vượt ngưỡng tạo cảnh báo cho người phụ trách.',
  },
  {
    title: 'Có sự cố thì xử lý và báo',
    text: 'Sự cố được ghi nhận, giao người xử lý; việc khẩn cấp báo ngay người liên hệ chính của gia đình.',
  },
]

const ACCOUNT_STEPS = [
  'Hành chính của viện tạo tài khoản cho bạn sau khi bạn được ghi là người thân của người cao tuổi.',
  'Đăng nhập bằng tài khoản và mật khẩu viện cấp.',
  'Quên mật khẩu thì liên hệ Hành chính để được cấp lại.',
]

export function FamilyLandingPage() {
  return (
    <div id="dau-trang" className="min-h-screen bg-landing-ivory text-foreground">
      <Header />

      <main>
        {/* Hero: lời hứa của trang và một trích đoạn bản tin có nguồn, điểm khác biệt của Elder_Care */}
        <section aria-labelledby="hero-title" className="bg-landing-sky px-4 pt-16 pb-20 sm:px-8 md:pt-24 md:pb-28">
          <div className="mx-auto grid max-w-[1120px] items-center gap-12 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]">
            <div className="flex flex-col gap-6">
              <h1 id="hero-title" className="max-w-[14ch] type-display">
                Một nơi để sống, một nơi để an tâm.
              </h1>
              <p className="max-w-[52ch] type-lead">
                Ở viện, người cao tuổi được chăm sóc bằng chuyên môn và vẫn sống theo cách của mình. Elder_Care cho
                gia đình biết điều đó mỗi tuần, từ người trực tiếp chăm sóc.
              </p>
              <div className="flex flex-wrap gap-3">
                <Button size="lg" nativeButton={false} render={<a href={LOGIN_URL} />}>
                  Đăng nhập cổng người thân
                </Button>
                <Button
                  size="lg"
                  variant="outline"
                  className="border-primary"
                  nativeButton={false}
                  render={<a href="#mot-tuan" />}
                >
                  Xem một tuần mẫu
                </Button>
              </div>
            </div>
            <figure className="flex flex-col gap-3">
              <div className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-6 text-card-foreground">
                <p className="type-card-title">Bà, phòng 204</p>
                <ul className="flex flex-col gap-3">
                  <Entry voice="resident" label="Theo lời bà">
                    Thích nghe nhạc trữ tình sau bữa trưa.
                  </Entry>
                  <Entry voice="family" label="Theo con gái">
                    Thích có người đọc báo cho nghe buổi sáng.
                  </Entry>
                  <Entry voice="staff" label="Nhân viên ghi nhận">
                    Chiều thứ năm, bà hát cùng nhóm ở phòng sinh hoạt.
                  </Entry>
                </ul>
              </div>
              <figcaption className="type-body-sm">Trích bản tin minh họa. Mỗi dòng ghi rõ ai đã nói ra.</figcaption>
            </figure>
          </div>
        </section>

        <Band id="vien" tone="ivory">
          <Intro id="vien" title="Một viện chia theo tầng, mỗi tầng có người phụ trách">
            Người cao tuổi ở theo phòng trong từng tầng. Mỗi tầng có Trưởng tầng; mỗi ca có người phụ trách ca và nhân
            viên được phân công cho từng người.
          </Intro>
          <div className="grid gap-6 sm:grid-cols-2">
            <PhotoSlot caption="Phòng ở của người cao tuổi" />
            <PhotoSlot caption="Phòng sinh hoạt chung" />
          </div>
          <Points
            items={[
              {
                title: 'Ba hình thức ở',
                text: 'Nội trú dài hạn, nội trú ngắn ngày và bán trú ban ngày; người ở bán trú về nhà mỗi tối.',
              },
              {
                title: 'Ba mức chăm sóc',
                text: 'Cơ bản, thường xuyên, đặc biệt. Bác sĩ chọn mức sau khi đánh giá bằng nhận xét, không chấm điểm tự động.',
              },
              {
                title: 'Đánh giá lại khi cần',
                text: 'Bác sĩ đánh giá lại theo định kỳ, sau sự cố hoặc sau khi người cao tuổi trở về từ bệnh viện.',
              },
              {
                title: 'Kế hoạch chăm sóc riêng',
                text: 'Mỗi người có một kế hoạch chăm sóc do điều dưỡng lập và bác sĩ duyệt.',
              },
            ]}
          />
        </Band>

        <Band id="cham-soc" tone="white">
          <Intro id="cham-soc" title="Chăm sóc là việc có người làm, có ghi chép, có bàn giao">
            Không nơi chăm sóc nào loại bỏ được mọi rủi ro. Điều viện làm được là ai cũng biết ai phụ trách việc gì,
            việc gì đã làm, và khi có chuyện thì ai báo cho ai.
          </Intro>
          <ol className="grid gap-x-10 gap-y-8 md:grid-cols-2 lg:grid-cols-4">
            {CARE_STEPS.map((step, i) => (
              <li key={step.title} className="flex flex-col gap-2 border-t border-foreground/15 pt-4">
                <span className="type-title text-primary tabular-nums">{i + 1}</span>
                <span className="type-card-title">{step.title}</span>
                <span className="type-reading">{step.text}</span>
              </li>
            ))}
          </ol>
        </Band>

        <Band id="doi-song" tone="beige">
          <div className="grid items-start gap-12 lg:grid-cols-[minmax(0,6fr)_minmax(0,5fr)]">
            <div className="flex flex-col gap-12">
              <Intro id="doi-song" title="Mỗi ngày vẫn là ngày của người cao tuổi">
                Chăm sóc giúp những việc người cao tuổi không còn tự làm được. Phần còn lại của một ngày vẫn theo thói
                quen và lựa chọn của họ.
              </Intro>
              <Points
                items={[
                  {
                    title: 'Bữa ăn',
                    text: 'Thực đơn theo tuần; chế độ ăn riêng do bác sĩ đặt; đồ ăn gia đình gửi được đối chiếu với dị ứng trước khi dùng.',
                  },
                  {
                    title: 'Hoạt động',
                    text: 'Buổi sinh hoạt nhóm, vận động, đi dạo. Khi đăng ký, nhân viên thấy điều người cao tuổi không thích để không xếp nhầm.',
                  },
                  {
                    title: 'Sở thích và lựa chọn',
                    text: 'Điều thích, điều không thích được ghi kèm người đã nói ra. Lựa chọn có rủi ro được trao đổi, không bị cấm mặc định.',
                  },
                  {
                    title: 'Nghỉ ngơi và tinh thần',
                    text: 'Giấc ngủ, tâm trạng, giao tiếp được nhân viên để ý qua từng ca; ít tham gia kéo dài thì người phụ trách được báo.',
                  },
                ]}
              />
            </div>
            <PhotoSlot caption="Một buổi sinh hoạt nhóm" className="lg:sticky lg:top-24" />
          </div>
        </Band>

        <Band id="mot-tuan" tone="sky">
          <div className="flex flex-col gap-4">
            <h2 id="mot-tuan-title" className="max-w-[24ch] type-section">
              Một tuần của bà, như gia đình nhìn thấy
            </h2>
            <p className="max-w-[60ch] type-lead">
              Bản tin minh họa, tuần 13/10 đến 19/10. Dữ liệu không có thật; bản tin thật do điều dưỡng phụ trách viết
              mỗi tuần.
            </p>
          </div>

          <div className="flex flex-col gap-8 md:gap-0">
            <BulletinRow
              title="Đời sống"
              note="Bà hiện ra trước hết là chính bà: điều bà thích, điều bà chọn, theo lời của ai."
              label="Đời sống của bà"
              position="first"
            >
              <ul className="flex flex-col gap-3">
                <Entry voice="resident" label="Theo lời bà">
                  Thích nghe nhạc trữ tình sau bữa trưa.
                </Entry>
                <Entry voice="resident" label="Theo lời bà">
                  Uống trà nóng mỗi chiều. Bà đã nghe bác sĩ nói về nguy cơ sặc và vẫn chọn uống; hai bên hẹn xem lại
                  sau một tháng.
                </Entry>
                <Entry voice="staff" label="Nhân viên ghi nhận">
                  Tham gia 4 buổi sinh hoạt: hát cùng nhóm và tập dưỡng sinh.
                </Entry>
              </ul>
            </BulletinRow>

            <BulletinRow
              title="Chăm sóc"
              note="Sự cố được kể đúng như đã xảy ra: chuyện gì, viện đã làm gì, gia đình được báo lúc nào."
              label="Chăm sóc trong tuần"
              position="middle"
            >
              <p className="type-reading">Điều dưỡng phụ trách tầng 2 chăm sóc bà trong ca ngày.</p>
              <div className="flex flex-col gap-3 rounded-xl border border-border p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="type-card-title">Thứ tư, 14:10, bà trượt chân ở hành lang</span>
                  <Tag source="done">Viện đã xử lý</Tag>
                </div>
                <ul className="flex flex-col gap-1.5 type-reading">
                  <li>14:12, nhân viên đỡ bà ngồi dậy và báo điều dưỡng.</li>
                  <li>14:20, điều dưỡng kiểm tra và đưa bà về phòng nghỉ.</li>
                  <li>14:40, viện báo cho con gái qua cổng người thân.</li>
                </ul>
              </div>
              <HealthLock />
            </BulletinRow>

            <BulletinRow
              title="Gia đình"
              note="Ý kiến của gia đình được ghi riêng, cạnh ý kiến của bà; không ai viết đè lên ai."
              label="Gia đình"
              position="last"
            >
              <ul className="flex flex-col gap-3">
                <Entry voice="family" label="Theo con gái">
                  Mong bà được ra vườn vào buổi sáng.
                </Entry>
                <Entry voice="resident" label="Theo lời bà">
                  Thích ngồi ở hiên hơn, ngoài vườn nắng.
                </Entry>
                <li className="flex flex-col gap-1.5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                  <span className="type-reading">Lịch thăm thứ bảy, 9:00 đến 10:00</span>
                  <Tag source="done">Đã đặt</Tag>
                </li>
              </ul>
            </BulletinRow>
          </div>
        </Band>

        <Band id="gia-dinh" tone="beige">
          <Intro id="gia-dinh" title="Gia đình vẫn ở trong cuộc sống của người thân">
            Viện lo phần chăm sóc hằng ngày. Gia đình giữ phần gắn bó: đến thăm, góp ý và được biết, theo cách hợp với
            mình.
          </Intro>
          <Points
            items={[
              {
                title: 'Lịch thăm',
                text: 'Đặt lịch thăm theo khung giờ của viện. Lượt thăm phải hủy, như khi người cao tuổi đi khám, viện báo lại.',
              },
              {
                title: 'Phối hợp chăm sóc',
                text: 'Hiểu biết của gia đình về người thân được ghi lại kèm tên người góp ý, để mọi ca đều biết.',
              },
              {
                title: 'Nhận cập nhật',
                text: 'Bản tin mỗi tuần và thông báo khi có việc; không phải gọi điện hỏi mới biết.',
              },
              {
                title: 'Trao đổi với viện',
                text: 'Gửi góp ý hoặc khiếu nại qua cổng; mỗi phản hồi có người phụ trách và thời hạn trả lời.',
              },
            ]}
          />
        </Band>

        <Band id="cong-nguoi-than" tone="white">
          <Intro id="cong-nguoi-than" title="Cổng người thân: xem đúng phần mình được chia sẻ">
            Mỗi người thân thấy một phần khác nhau, theo quyền viện cấp và theo bản đồng ý của người cao tuổi.
          </Intro>
          <Points
            wide
            items={[
              {
                title: 'Thông báo',
                text: 'Việc khẩn cấp luôn được báo tới người liên hệ chính. Thông báo thường ngày theo phạm vi người cao tuổi đã đồng ý.',
              },
              {
                title: 'Thông tin sức khỏe',
                text: 'Chỉ hiện với người thân có tên trong bản đồng ý chia sẻ dữ liệu của người cao tuổi.',
              },
              {
                title: 'Lịch thăm và chi phí',
                text: 'Đặt lịch thăm, xem chi phí đã chốt nếu được cấp quyền; mọi việc tiếp tục làm trong cổng sau khi đăng nhập.',
              },
            ]}
          />
          <div
            id="quyen-rieng-tu"
            className="grid scroll-mt-20 gap-6 border-t border-border pt-10 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-10"
          >
            <h3 className="type-title">Quyền riêng tư của người cao tuổi</h3>
            <ul className="flex list-disc flex-col gap-3 pl-5 type-reading">
              <li>Thông tin sức khỏe là của người cao tuổi; họ quyết định chia sẻ cho người thân nào.</li>
              <li>Khi người cao tuổi rút lại đồng ý, quyền xem sức khỏe của người thân tắt ngay.</li>
              <li>Mong muốn của người cao tuổi về người được đến thăm, người được đón được ghi lại và được tôn trọng.</li>
            </ul>
          </div>
          <div
            id="ho-tro-tai-khoan"
            className="grid scroll-mt-20 gap-6 border-t border-border pt-10 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-10"
          >
            <h3 className="type-title">Cách có tài khoản</h3>
            <ol className="flex flex-col gap-4">
              {ACCOUNT_STEPS.map((step, i) => (
                <li key={step} className="flex gap-4">
                  <span className="type-title text-primary tabular-nums">{i + 1}</span>
                  <span className="type-reading">{step}</span>
                </li>
              ))}
            </ol>
          </div>
        </Band>

        <Band id="lien-he" tone="navy">
          <div className="grid gap-10 md:grid-cols-[minmax(0,7fr)_minmax(0,5fr)] md:items-end">
            <div className="flex flex-col gap-4">
              <h2 id="lien-he-title" className="max-w-[20ch] type-section">
                Đã có tài khoản? Vào cổng người thân.
              </h2>
              <p className="max-w-[48ch] type-lead">
                Chưa có tài khoản hoặc cần hỏi về việc chăm sóc, hãy liên hệ Hành chính của viện.
              </p>
            </div>
            <div className="flex flex-col gap-4">
              <Button size="lg" variant="secondary" className="self-start" nativeButton={false} render={<a href={LOGIN_URL} />}>
                Đăng nhập cổng người thân
              </Button>
              <p className="type-body-sm">Kênh liên hệ chính thức của viện: chờ viện xác nhận.</p>
            </div>
          </div>
        </Band>
      </main>

      <footer className="bg-primary px-4 pb-10 text-primary-foreground sm:px-8">
        <div className="mx-auto flex max-w-[1120px] flex-col gap-4 border-t border-primary-foreground/20 pt-8 md:flex-row md:items-center md:justify-between">
          <p className="type-body-sm">Elder_Care, cổng người thân. Trang mẫu giai đoạn thiết kế.</p>
          <nav aria-label="Liên kết cuối trang">
            <ul className="flex flex-wrap gap-x-6 gap-y-2 type-body-sm">
              <li>
                <a href="#quyen-rieng-tu" className="rounded underline-offset-4 hover:underline">
                  Quyền riêng tư
                </a>
              </li>
              <li>
                <a href="#ho-tro-tai-khoan" className="rounded underline-offset-4 hover:underline">
                  Hỗ trợ tài khoản
                </a>
              </li>
              <li>
                <a href="#lien-he" className="rounded underline-offset-4 hover:underline">
                  Liên hệ
                </a>
              </li>
            </ul>
          </nav>
        </div>
      </footer>
    </div>
  )
}
