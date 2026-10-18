import { cva } from 'class-variance-authority'
import { ImageIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { DEMO_RESIDENTS } from '@/common/demoData'

// Trang chủ cổng người thân sau khi đăng nhập (DESIGN.md 2.1, 2.3): giới thiệu viện trước (01 → 05), rồi mới
// tới cách Elder_Care giúp gia đình đồng hành. Mọi điều nêu ở đây lấy từ nghiệp vụ; điều chưa có trong nghiệp
// vụ (sân vườn, khu vận động) ghi "chờ viện xác nhận". Không số liệu, chứng nhận, tên người chưa xác minh.

// Nền từng section (token --landing-*). Chữ trên nền trời chỉ dùng màu chữ chính.
const band = cva('scroll-mt-20 px-4 py-16 sm:px-8 md:py-24', {
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

// Số thứ tự, tiêu đề section và đoạn mở, căn trái, dòng ngắn
function Intro({ id, number, title, children }) {
  return (
    <div className="flex max-w-[62ch] flex-col gap-4">
      {number && <span className="type-title text-primary tabular-nums">{number}</span>}
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

const SPACE = [
  { title: 'Phòng ở theo tầng', text: 'Viện chia khu, tầng, phòng, giường. Mỗi phòng có loại phòng, chính sách giới tính và mức chăm sóc được phép.' },
  { title: 'Ba hình thức ở', text: 'Nội trú dài hạn, nội trú ngắn ngày và bán trú ban ngày; người ở bán trú về nhà mỗi tối.' },
  { title: 'Thiết bị hỗ trợ', text: 'Giường, xe lăn, nệm chống loét, đồ dùng tại tầng và thiết bị y tế như máy đo do bộ phận phụ trách quản lý, bảo trì.' },
  { title: 'Giường sẵn sàng trước khi đón người mới', text: 'Giường chỉ trở lại Trống khi được xác nhận sẵn sàng; sau người nghi nhiễm thì phải ghi đã khử khuẩn.' },
  { title: 'Khi có dịch', text: 'Phòng có thể được cách ly, khu có thể được khoanh vùng; lượt thăm trong vùng bị hủy và gia đình được báo.' },
]

const TEAM = [
  { title: 'Bác sĩ', text: 'Đánh giá và chọn mức chăm sóc, duyệt kế hoạch chăm sóc, kê đơn trong phạm vi được phép, đặt chế độ ăn.' },
  { title: 'Điều dưỡng', text: 'Lập kế hoạch chăm sóc, phát thuốc và xác nhận từng liều, xử lý cảnh báo, bàn giao ca, soạn bản tin cho gia đình.' },
  { title: 'Nhân viên chăm sóc', text: 'Làm và ghi lại từng việc chăm sóc trong ca, đo chỉ số, đưa người cao tuổi tham gia hoạt động.' },
  { title: 'Trưởng tầng', text: 'Điều phối tầng, phân công người chăm sóc, theo dõi việc quá giờ, tiếp nhận góp ý của gia đình về tầng mình.' },
  { title: 'Nhân viên bếp', text: 'Lập thực đơn để bác sĩ xác nhận, chuẩn bị suất ăn theo chế độ riêng của từng người.' },
  { title: 'Hành chính, Kế toán, Quản lý viện', text: 'Tiếp nhận, hợp đồng, lịch thăm, đồ gửi, tài khoản người thân; thu tiền, số dư; duyệt và giám sát chung.' },
]

const CONTACTS = [
  ['Chăm sóc, sinh hoạt, ăn uống', 'Trưởng tầng của tầng người cao tuổi ở'],
  ['Sức khỏe, thuốc', 'Trưởng tầng; có thể giao điều dưỡng trả lời'],
  ['Chi phí, hợp đồng và việc khác', 'Hành chính của viện'],
  ['Việc khẩn cấp', 'Viện báo ngay người liên hệ chính của gia đình'],
]

const DAILY = [
  { title: 'Ăn uống', text: 'Thực đơn theo tuần; chế độ ăn riêng do bác sĩ đặt; đồ ăn gia đình gửi được đối chiếu với dị ứng trước khi dùng.' },
  { title: 'Vận động và hoạt động', text: 'Buổi sinh hoạt nhóm, vận động, đi dạo, hoạt động ngoài viện. Khi đăng ký, nhân viên thấy điều người cao tuổi không thích để không xếp nhầm.' },
  { title: 'Giao tiếp và tinh thần', text: 'Tâm trạng, giao tiếp, mức tham gia được nhân viên để ý qua từng ca; ít tham gia kéo dài thì người phụ trách được báo.' },
  { title: 'Sở thích và lựa chọn', text: 'Điều thích, điều không thích được ghi kèm người đã nói ra. Lựa chọn có rủi ro được trao đổi, không bị cấm mặc định.' },
  { title: 'Nghỉ ngơi và lịch riêng', text: 'Mỗi người có thời khóa biểu riêng, gồm lịch sinh hoạt, bữa ăn, hoạt động, lịch thăm và các việc chăm sóc trong ngày.' },
]

const CARE = [
  { title: 'Đánh giá khi vào ở', text: 'Bác sĩ đánh giá bằng nhận xét rồi chọn mức chăm sóc; không chấm điểm tự động. Đánh giá lại định kỳ và sau sự cố.' },
  { title: 'Kế hoạch chăm sóc', text: 'Điều dưỡng lập kế hoạch riêng cho từng người, bác sĩ duyệt; công việc mỗi ca lập theo kế hoạch.' },
  { title: 'Theo dõi sức khỏe', text: 'Chỉ số đo theo lịch bác sĩ đặt; chỉ số vượt ngưỡng tạo cảnh báo, việc chậm được leo thang tới người có trách nhiệm.' },
  { title: 'Thuốc theo chỉ định', text: 'Bác sĩ kê đơn; điều dưỡng phát và xác nhận từng liều; thuốc gia đình gửi được tiếp nhận và đối chiếu.' },
  { title: 'Bàn giao ca', text: 'Ca trước bàn giao việc còn dở và cảnh báo chưa xử lý; ca sau xác nhận đã nhận rồi mới nhận ca.' },
  { title: 'Xử lý sự cố', text: 'Sự cố được ghi nhận và giao người xử lý; việc khẩn cấp kích hoạt quy trình khẩn cấp và báo người liên hệ chính.' },
]

const FAMILY = [
  { title: 'Tham gia kế hoạch chăm sóc', text: 'Ý kiến của gia đình được ghi riêng, có tên người góp ý, cạnh ý kiến của người cao tuổi; không ai viết đè lên ai.' },
  { title: 'Thăm người thân', text: 'Đặt lịch thăm theo khung giờ của viện; lượt thăm phải hủy, như khi người cao tuổi đi khám, viện báo lại.' },
  { title: 'Nhận cập nhật', text: 'Bản tin mỗi tuần do điều dưỡng phụ trách soạn và thông báo khi có việc; không phải gọi điện hỏi mới biết.' },
  { title: 'Trao đổi khi có vấn đề', text: 'Gửi góp ý, hỏi đáp hoặc khiếu nại; mỗi phản hồi có người phụ trách và thời hạn trả lời.' },
]

export function HomePage({ onNavigate }) {
  const resident = DEMO_RESIDENTS[0]
  return (
    <div className="bg-landing-ivory text-foreground">
      <section aria-labelledby="hero-title" className="bg-landing-sky px-4 pt-14 pb-16 sm:px-8 md:pt-20 md:pb-24">
        <div className="mx-auto grid max-w-[1120px] items-center gap-10 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]">
          <div className="flex flex-col gap-6">
            <h1 id="hero-title" className="max-w-[14ch] type-display">
              Một nơi để sống, một nơi để an tâm.
            </h1>
            <p className="max-w-[52ch] type-lead">
              Viện là nơi người cao tuổi sống mỗi ngày: có phòng ở, bữa ăn, bạn bè và người chăm sóc. Trang này giới thiệu viện
              sống và chăm sóc ra sao, và gia đình vẫn đồng hành cùng người thân như thế nào.
            </p>
            <div className="flex flex-wrap gap-3">
              <Button size="lg" onClick={() => onNavigate('today')}>
                Xem thông tin của {resident.name}
              </Button>
              <Button size="lg" variant="outline" className="border-primary" nativeButton={false} render={<a href="#khong-gian" />}>
                Tìm hiểu về viện
              </Button>
            </div>
          </div>
          <PhotoSlot caption="Toàn cảnh viện" />
        </div>
      </section>

      <Band id="khong-gian" tone="ivory">
        <Intro id="khong-gian" number="01" title="Không gian sống và môi trường chăm sóc">
          Gia đình cần hình dung được người thân sẽ sống ở đâu, sinh hoạt ra sao, được hỗ trợ an toàn thế nào.
        </Intro>
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <PhotoSlot caption="Phòng ở của người cao tuổi" />
          <PhotoSlot caption="Phòng sinh hoạt chung" />
          <PhotoSlot caption="Sân vườn (chờ viện xác nhận)" />
          <PhotoSlot caption="Khu vận động (chờ viện xác nhận)" />
        </div>
        <Points wide items={SPACE} />
      </Band>

      <Band id="doi-ngu" tone="white">
        <Intro id="doi-ngu" number="02" title="Con người và đội ngũ chăm sóc">
          Mỗi tầng có Trưởng tầng; mỗi ca có người phụ trách ca và nhân viên được phân công cho từng người cao tuổi.
        </Intro>
        <Points wide items={TEAM} />
        <div className="grid gap-6 border-t border-border pt-10 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-10">
          <h3 className="type-title">Gia đình liên hệ với ai</h3>
          <dl className="flex flex-col">
            {CONTACTS.map(([topic, who]) => (
              <div key={topic} className="flex flex-col gap-1 border-t border-border py-3 first:border-t-0 first:pt-0 sm:flex-row sm:justify-between sm:gap-6">
                <dt className="type-card-title">{topic}</dt>
                <dd className="type-reading sm:text-right">{who}</dd>
              </div>
            ))}
          </dl>
        </div>
      </Band>

      <Band id="doi-song" tone="beige">
        <div className="grid items-start gap-12 lg:grid-cols-[minmax(0,6fr)_minmax(0,5fr)]">
          <div className="flex flex-col gap-12">
            <Intro id="doi-song" number="03" title="Đời sống hằng ngày">
              Viện là một môi trường sống, không chỉ là nơi làm các việc chăm sóc. Chăm sóc giúp những việc người cao tuổi
              không còn tự làm được; phần còn lại của ngày vẫn theo thói quen và lựa chọn của họ.
            </Intro>
            <Points items={DAILY} />
          </div>
          <PhotoSlot caption="Một buổi sinh hoạt nhóm" className="lg:sticky lg:top-28" />
        </div>
      </Band>

      <Band id="quy-trinh" tone="white">
        <Intro id="quy-trinh" number="04" title="Quy trình chăm sóc và bảo đảm an toàn">
          Không nơi chăm sóc nào loại bỏ được mọi rủi ro. Điều viện làm được là ai cũng biết ai phụ trách việc gì, việc gì
          đã làm, và khi có chuyện thì ai báo cho ai.
        </Intro>
        <ol className="grid gap-x-10 gap-y-8 md:grid-cols-2 lg:grid-cols-3">
          {CARE.map((step, i) => (
            <li key={step.title} className="flex flex-col gap-2 border-t border-foreground/15 pt-4">
              <span className="type-title text-primary tabular-nums">{i + 1}</span>
              <span className="type-card-title">{step.title}</span>
              <span className="type-reading">{step.text}</span>
            </li>
          ))}
        </ol>
      </Band>

      <Band id="gia-dinh" tone="beige">
        <Intro id="gia-dinh" number="05" title="Viện và gia đình cùng chăm sóc">
          Đưa người thân vào viện không có nghĩa là gia đình thôi chăm sóc. Viện lo phần chăm sóc hằng ngày; gia đình giữ phần
          gắn bó: góp ý, đến thăm, được biết và trao đổi khi có vấn đề.
        </Intro>
        <Points items={FAMILY} />
      </Band>

      <Band id="elder-care" tone="sky">
        <Intro id="elder-care" title="Elder_Care giúp gia đình đồng hành">
          Những gì viện ghi lại mỗi ngày đến với gia đình qua cổng người thân, đúng phần người cao tuổi đồng ý chia sẻ.
        </Intro>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            ['Hôm nay', 'Lịch sinh hoạt, sự cố gần đây và thông tin của người cao tuổi.', 'today'],
            ['Bản tin', 'Bản tin tuần do điều dưỡng phụ trách soạn.', 'bulletins'],
            ['Thăm', 'Đặt và hủy lịch thăm theo khung giờ của viện.', 'visits'],
            ['Phản hồi', 'Góp ý, hỏi đáp, khiếu nại có người phụ trách và hạn trả lời.', 'feedback'],
          ].map(([title, text, page]) => (
            <div key={page} className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5 text-card-foreground">
              <span className="type-card-title">{title}</span>
              <span className="flex-1 type-body-sm">{text}</span>
              <Button variant="outline" className="self-start" onClick={() => onNavigate(page)}>
                Mở {title.toLowerCase()}
              </Button>
            </div>
          ))}
        </div>
        <div className="grid gap-6 border-t border-foreground/15 pt-10 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-10">
          <h3 className="type-title">Quyền riêng tư của người cao tuổi</h3>
          <ul className="flex list-disc flex-col gap-3 pl-5 type-reading">
            <li>Thông tin sức khỏe là của người cao tuổi; họ quyết định chia sẻ cho người thân nào.</li>
            <li>Khi người cao tuổi rút lại đồng ý, quyền xem sức khỏe của người thân tắt ngay.</li>
            <li>Mong muốn của người cao tuổi về người được đến thăm, người được đón được ghi lại và được tôn trọng.</li>
          </ul>
        </div>
      </Band>

      <Band id="lien-he" tone="navy">
        <div className="grid gap-10 md:grid-cols-[minmax(0,7fr)_minmax(0,5fr)] md:items-end">
          <div className="flex flex-col gap-4">
            <h2 id="lien-he-title" className="max-w-[22ch] type-section">
              Cần trao đổi với viện?
            </h2>
            <p className="max-w-[48ch] type-lead">Gửi phản hồi qua cổng, hoặc liên hệ Hành chính của viện.</p>
          </div>
          <div className="flex flex-col gap-4">
            <Button size="lg" variant="secondary" className="self-start" onClick={() => onNavigate('feedback')}>
              Gửi phản hồi
            </Button>
            <p className="type-body-sm">Kênh liên hệ chính thức của viện: chờ viện xác nhận.</p>
          </div>
        </div>
      </Band>
    </div>
  )
}
