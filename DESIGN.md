---
name: Elder_Care
version: 5.19.1
status: Đã duyệt (chủ dự án, 2026-10-16; mục 2.2 duyệt 2026-10-18; mục 15 duyệt 2026-10-18; mục 15.6 duyệt 2026-10-19)
platforms: { web: frontend/, mobile: mobile/ }
updated: 2026-10-23
direction: Trời xanh theo bảng màu letters.app
colors:
  light:
    canvas-gradient: ["#B4C8DD 0%", "#C7DCEB 58%", "#E2EDF5 100%"]  # nền trang: dải trời gốc pha 45% trắng, cố định theo khung nhìn
    menu-gradient: ["#8BAACA 0%", "#A9C9E0 58%", "#D3E4EF 100%"]    # dải trời gốc pha 15% trắng, đậm hơn nền trang một bậc
    menu-ink: "#0F2235"
    menu-ink-muted: "#14304F"   # nhãn nhóm, chữ phụ trong menu
    menu-selected: "#24496F/#FFFFFF"   # viên thuốc xanh đêm, chữ trắng
    menu-hover: "#FFFFFF73"
    menu-line: "#0F22351F"
    card: "#FFFFFF"
    surface: "#FFFFFF"       # ô nhập, nút lọc, lớp nổi
    ink: "#0F2235"           # chữ chính, xanh đêm
    ink-muted: "#50627A"     # chữ phụ, chỉ trên nền trắng hoặc nền phụ
    muted: "#F5F7FA"         # nền phụ, rãnh thanh tiến độ
    line: "#EFEFF1"
    control-line: "#868C98"  # viền ô nhập
    primary: "#24496F"       # nút chính, mục lọc đang chọn, tiêu đề trang, focus
    on-primary: "#FFFFFF"
    accent-ink: "#0B5F8A"    # chữ nhấn: giờ hiện tại, nút tonal, giữ chỗ
    accent-wash: "#C0EAFF"
    brand-graphic: "#2597D0" # chỉ đồ họa
    green-wash: "#E2F4EA"
    green-ink: "#1F6B45"
    amber-wash: "#FFECC0"
    amber-ink: "#8A5300"
    red: "#C9302C"           # chỉ việc gấp
    red-wash: "#FFECEC"
    info-wash: "#C0D5FF"
    info-ink: "#2A4A8F"
    avatar: ["#C0EAFF/#0B5F8A", "#C0D5FF/#2A4A8F", "#E2F4EA/#1F6B45", "#EFEFF1/#4A4A55"]
    bed: { occupied: "#4A6FD6", held: "#1A8CC4", pending: "#C77A12", maintenance: "#7A8699" }  # chỉ sơ đồ giường
  dark:
    canvas-gradient: ["#0F1A2A 0%", "#15233A 58%", "#1B2B42 100%"]
    menu-gradient: ["#0B1320 0%", "#0E1829 58%", "#121D2D 100%"]    # dải nền trang tối trộn 45% #060B14
    menu-ink: "#FFFFFF"
    menu-ink-muted: "#B8C6DA"
    menu-selected: "#E6EDF6/#10203A"
    menu-hover: "#FFFFFF1F"
    menu-line: "#FFFFFF26"
    card: "#18243A"
    surface: "#1E2B44"
    ink: "#E6EDF6"
    ink-muted: "#9AABC2"
    muted: "#1E2B44"
    line: "#2A3A52"
    control-line: "#6E80A0"
    primary: "#E6EDF6"
    on-primary: "#0F1A2A"
    accent-ink: "#8DB4E2"
    accent-wash: "#22344F"
    brand-graphic: "#6F9FD6"
    green-wash: "#173A2A"
    green-ink: "#7FD3A3"
    amber-wash: "#3A2C10"
    amber-ink: "#FFCB6B"
    red: "#FF7A7E"
    red-wash: "#3A1A1C"
    info-wash: "#16273D"
    info-ink: "#9CC3FF"
    avatar: ["#16273D/#9CC3FF", "#2A2340/#C9B8FF", "#173A2A/#7FD3A3", "#2E2A20/#E2D3AE"]
    bed: { occupied: "#7B9BF0", held: "#4FB6E6", pending: "#F2A646", maintenance: "#8A97AD" }
typography:
  family: "Be Vietnam Pro"   # @fontsource/be-vietnam-pro, nạp 400, 500, 600
  weights: [400, 500, 600]
  numerals: tabular-nums
  scale:
    caption: { size: 12px, line: 16px, weight: 500 }
    body-sm: { size: 14px, line: 20px, weight: 400 }
    label:   { size: 14px, line: 20px, weight: 500 }
    body:    { size: 15px, line: 22px, weight: 400 }
    card-title: { size: 16px, line: 22px, weight: 600 }
    title:   { size: 18px, line: 24px, weight: 600 }
    metric:  { size: 24px, line: 28px, weight: 600, tracking: -0.01em, numerals: tabular-nums }
    page:    { size: 26px, line: 32px, weight: 600, tracking: -0.01em }
    clock:   { size: 28px, line: 32px, weight: 600 }
    # chỉ trang giới thiệu cổng người thân (mục 2.1)
    display: { size: 38px→56px, line: 46px→64px, weight: 600, tracking: -0.02em }
    section: { size: 24px→28px, line: 32px→36px, weight: 600, tracking: -0.01em }
    lead:    { size: 18px, line: 28px, weight: 400 }
    reading: { size: 16px, line: 26px, weight: 400 }
rounded:
  sm: 6px      # điểm nhấn nhỏ
  md: 10px     # ô giường
  lg: 12px     # ô nhập, khối nhỏ
  xl: 16px     # thẻ, page-header
  2xl: 20px    # khung chi tiết, hộp thoại
  pill: 9999px # nút, mục menu, badge, nút lọc, ảnh đại diện
spacing:
  base: 4px
  steps: [4, 8, 12, 16, 20, 24, 32]
  control-height: 40px
  touch-target: 44px
  menu-width: 240px
  content-max: 1600px  # màn nhân viên; cổng người thân theo mục 2.3
components:
  page-header: khối thẻ trắng đầu màn, tiêu đề, dòng phụ, ô phải, thanh lọc
  status-pill: Badge, biến thể urgent | attention | settled | held | occupied | neutral | neutral-outline
  task-card: Card, một công việc chăm sóc
  room-card: Card, một phòng, chứa các ô giường
  resident-avatar: vòng tròn chữ cái của tên gọi
  filter-bar: ToggleGroup dạng nút bo tròn, số đếm trên nút
  shift-bar: thanh tiến độ ca kèm giờ hiện tại
  detail-panel: Sheet bên phải
  care-notes: AllergyNote, ChoiceNote, CareNote
  source-tag: SourceTag, nhãn người nói (resident | family | staff | done)
  page-state: NotDesignedState, PermissionDeniedState
---

# DESIGN.md: quy tắc giao diện Elder_Care (web và mobile)

Đây là **nguồn quy tắc duy nhất** cho giao diện web (`frontend/`) và ứng dụng di động (`mobile/`), áp dụng cho cả nhóm và cho agent. Đọc hết file này trước khi thiết kế, thêm hay sửa màn hình trên nền tảng nào.

- File này thắng skill `frontend-design` và mặc định của shadcn khi khác nhau.
- File này không đổi nghiệp vụ. Tên, trạng thái, quyền, quy tắc lấy từ `docs/business/nghiep-vu.md` và spec của feature; thấy thiếu hay mâu thuẫn thì dừng và báo lại, không đoán.
- Mọi giá trị màu, chữ, bo góc khai báo một lần ở mỗi nền tảng: `frontend/src/index.css` (web) và theme của `mobile/` (mục 15.2). File này và hai nơi đó phải luôn khớp nhau.
- Hai nền tảng dùng chung một bộ màu, chữ, trạng thái, công thức và câu chữ; chỉ cách cài khác nhau. Bảng dưới cho biết mục nào chung, mục nào riêng.

| Mục | Áp cho |
| --- | --- |
| 1. Quy tắc cốt lõi | Chung; điều 9 là cách làm ở web, mobile theo mục 15.3 |
| 2. Hướng thiết kế, 2.2 Nhân văn | Chung |
| 2.1 Trang chủ cổng người thân (giới thiệu viện) | Web |
| 2.3 Cổng người thân | Chung (cổng web và app người thân cùng chức năng, Q-305); khung điện thoại ở 2.3 là bản web, app theo 15.4 |
| 3.1 Bảng màu, 3.4 Độ tương phản, 3.5 Trạng thái, 3.6 Ảnh đại diện | Chung |
| 3.2 Menu trái | Web |
| 3.3 Sáng, tối | Chung; cách lưu ở web ghi tại 3.3, ở mobile tại 15.2 |
| 4. Chữ | Chung; cách nạp font và utility `type-*` là của web, mobile theo 15.2 |
| 5. Bố cục, 5.1 Lưới thẻ hay bảng | Web; mobile theo 15.4 |
| 6. Hình khối, độ nổi | Chung |
| 7. Component | Công thức 7.2, trạng thái màn 7.3, lệnh và lỗi 7.4 là chung; tên component shadcn, `cva`, `Sheet` là của web, mobile theo 15.3 |
| 8. Câu chữ, 11. Nên và không nên | Chung |
| 9. Trợ năng | Chung; thuộc tính `aria-*`, focus bàn phím là của web, mobile theo 15.5 |
| 10. Co giãn theo màn hình, 12. Công cụ và skill, 14. Hiện trạng | Web; mobile theo 15.1, 15.4, 15.7 |
| 13. Quy trình | Chung |
| 15. Ứng dụng di động | Mobile |

## 1. Quy tắc cốt lõi

Mười điều phải giữ ở mọi màn hình:

1. Chỉ dùng biến (`bg-primary`, `text-muted-foreground`, `bg-(--avatar-1)`), không viết mã màu thô trong JSX.
2. Thẻ, khối, nút luôn màu trơn. Gradient chỉ có ở nền trang và menu trái.
3. Chữ trên nền trời chỉ dùng màu chữ chính; chữ phụ và chữ trắng chỉ đặt trên thẻ trắng hoặc nền phụ.
4. Đỏ chỉ cho việc gấp: Quá hạn, Khẩn cấp, "đã leo thang tối đa".
5. Xanh thương hiệu `#2597D0` chỉ dùng cho đồ họa, không dùng cho chữ hay làm nền nút chữ trắng.
6. Trạng thái luôn có chữ đi kèm; màu không bao giờ là tín hiệu duy nhất.
7. Mỗi màn có một việc chính và một nút chính (xanh đêm `#24496F`).
8. Nút lệnh và mục menu lấy theo quyền: menu theo ma trận quyền (Phụ lục 27, mục 19.3 `nghiep-vu.md`), nút lệnh theo danh sách lệnh được phép do backend trả.
9. Dùng component có sẵn trước, đổi giao diện ở một chỗ: web dùng shadcn và biến thể `cva` trong file component, không ghi đè màu bằng `className`; mobile dùng widget Material 3 và theme (mục 15.3).
10. Màu, cặp chữ/nền mới phải kiểm độ tương phản trước khi dùng (mục 3.4).

## 2. Hướng thiết kế

- **Chủ đề:** một tầng của viện dưỡng lão trong một ca trực.
- **Người dùng chính trên web:** điều dưỡng, nhân viên chăm sóc, trưởng tầng ở quầy trực (máy tính, máy tính bảng). Quản lý viện, bác sĩ, hành chính, kế toán, nhân viên bếp dùng cùng bộ khung. Người thân dùng cổng người thân trên điện thoại (NFR-10).
- **Việc chính:** biết ngay ai cần gì lúc này, ghi nhận nhanh và đúng loại kết quả.
- **Tính cách:** hiện đại, trẻ trung, sáng, thanh lịch, gọn gàng, phục vụ vận hành; không theo khuôn dashboard SaaS chung chung, không ồn ào.
- **Hình ảnh chung:** nền trời xanh chuyển màu, menu cùng dải nhưng đậm hơn, thẻ trắng, chữ và nút chính xanh đêm, nhãn màu phấn (xanh trời, xanh chàm, hổ phách).
- **Điểm nhấn:** thanh tiến độ ca và giờ hiện tại; ảnh đại diện chữ cái giúp nhận ra người cao tuổi. Không thêm điểm nhấn trang trí khác.
- **Ưu tiên nội dung:** danh sách người cao tuổi, lịch chăm sóc, công việc thuốc, sự cố, bàn giao, tình trạng phòng/giường. Dùng bảng, dòng thời gian, danh sách việc, khung chi tiết bên cạnh khi hợp luồng làm việc hơn thẻ.
- **Định hướng:** Clinical & Calm, Enterprise Admin, Human-centered Care. Chính xác, an toàn đứng trước trang trí, nhất là phát thuốc, xử lý cảnh báo, bàn giao ca.
- **Nhân văn thể hiện qua nghiệp vụ, không qua trang trí:** người cao tuổi là một con người, không chỉ là hồ sơ. Hồ sơ cho thấy nhu cầu chăm sóc, người phụ trách chính, sở thích đã ghi nhận kèm người nói; danh sách việc luôn cho biết việc cho ai, ai phụ trách và lựa chọn của người đó. Chỉ hiện dữ liệu đã được ghi; không tự tính điểm, không tự suy ra hay gợi ý (Q-306, Q-307). Quy tắc chi tiết ở mục 2.2.
- **Hình ảnh:** chỉ dùng ảnh nhận diện người cao tuổi (5.1) ở chỗ cần xác định đúng người (hồ sơ; sau này là phát thuốc). Không dùng ảnh trang trí, ảnh kho, ảnh hoạt động làm như chức năng đã chạy. Chưa có ảnh thì hiện chữ cái và ghi "Chưa có ảnh". Người thân có quan hệ Hiệu lực thấy ảnh ở loại thông tin chung (spec 012 FR-046); các vai trò nhân viên theo quyền backend trả.
- **Không giả lập:** không làm như tích hợp IoT, bệnh viện, API đã hoạt động khi chưa triển khai; màn chưa thiết kế ghi rõ là chưa có.

### 2.1 Trang chủ cổng người thân (giới thiệu viện)

Trang chủ của cổng người thân (`src/features/family-portal/HomePage.jsx`), mở ra ngay khi người thân đăng nhập (mục 2.3). Trang giới thiệu viện trước, giá trị chăm sóc tiếp theo, rồi mới tới cách Elder_Care giúp gia đình đồng hành. Không còn trang giới thiệu công khai riêng. Phong cách Modern Healthcare Editorial: nhiều khoảng trắng, căn trái, ít thẻ; không gradient, không bóng đổ, không hiệu ứng tự chạy. Ý tưởng "Một nơi để sống, một nơi để an tâm"; ba giá trị Tôn trọng, An tâm, Đồng hành.

- **Thứ tự section:** đầu trang của cổng (mục 2.3); mở đầu (thông điệp, nút "Xem thông tin của <họ tên>" và "Tìm hiểu về viện", ảnh toàn cảnh); 01 Không gian sống và môi trường chăm sóc (phòng ở, sinh hoạt chung, sân vườn, khu vận động, thiết bị hỗ trợ, giường sẵn sàng, khi có dịch); 02 Con người và đội ngũ chăm sóc (vai trò từng vị trí, bảng "Gia đình liên hệ với ai" theo spec 012 FR-059); 03 Đời sống hằng ngày; 04 Quy trình chăm sóc và bảo đảm an toàn (đánh giá, kế hoạch, theo dõi sức khỏe, thuốc, bàn giao ca, sự cố); 05 Viện và gia đình cùng chăm sóc; Elder_Care giúp gia đình đồng hành (lối vào Hôm nay, Bản tin, Thăm, Phản hồi; quyền riêng tư); Cần trao đổi với viện. Tiêu đề 01 → 05 có số thứ tự màu xanh đêm.
- **Nền section** (token `--landing-*` trong `index.css`): trời `#9ABFDA` cho đầu trang, mở đầu và Elder_Care; trắng ngà `#FBF9F4` cho 01; trắng cho 02 và 04; be ấm `#F2EADF` cho 03 và 05; xanh đêm `#24496F` cho Cần trao đổi với viện. Ô ảnh giữ chỗ `#E4D8C6`.
- **Độ tương phản đã kiểm:** chữ chính trên trời 8,33:1, trên be 13,54:1, trên trắng ngà 15,35:1; chữ phụ trên be 5,23:1, trên trắng ngà 5,92:1; chữ trắng trên xanh đêm 9,30:1. Trên nền trời chỉ dùng chữ chính (chữ phụ chỉ 3,22:1).
- **Giá trị nói bằng việc làm:** mô tả công việc bằng hành động, quy trình, người chịu trách nhiệm; không hứa an toàn tuyệt đối; không số liệu, chứng nhận, thông tin đội ngũ chưa xác minh; không lời làm gia đình áy náy.
- **Tiếng nói có chủ là dấu nhận diện:** mỗi điều ghi về người cao tuổi mang nhãn người nói (`SourceTag`, mục 2.2): "Theo lời bà" (xanh trời), "Theo con gái" (xanh chàm), "Nhân viên ghi nhận" (nền phụ); việc đã xong mang nhãn xanh lá ("Viện đã xử lý", "Đã đặt").
- **Không có bản tin minh họa:** người thân đã đăng nhập xem bản tin thật ở màn Bản tin, nên trang chủ bỏ phần "Một tuần mẫu".
- **Ảnh:** chỉ ảnh thực tế được phép sử dụng. Khi chưa có, dùng ô giữ chỗ ghi "Chờ ảnh thực tế được phép sử dụng" kèm chú thích; không ảnh kho, không ảnh thương hại hay vui vẻ giả tạo.
- **Không giả lập:** điều chưa có trong nghiệp vụ (sân vườn, khu vận động) ghi "chờ viện xác nhận"; kênh liên hệ ghi "chờ viện xác nhận"; quy trình, vai trò nêu trên trang phải có trong nghiệp vụ (2.3, 2.4, 5.3, 7.1, 7.5, 8.1 → 8.10, 9.x, 10.x, 11.x, 12.x, 13.1a, 13.5, 14.1 → 14.7).
- Chữ lớn dùng thang riêng của trang (mục 4); một họ font Be Vietnam Pro.

### 2.2 Nhân văn trong màn nghiệp vụ

Ba nguyên tắc đã chốt ở `nghiep-vu.md` (1.3; Q-347 → Q-364) hiện ra trên màn hình bằng các quy tắc dưới đây. Câu hỏi kiểm cho mọi màn có mong muốn hay quyết định của người cao tuổi: **ai nói, ai quyết, có hiệu lực tới khi nào**. Màn chưa trả lời được ba câu đó thì chưa xong.

**Tiếng nói có chủ (Q-347, BR-M01-14, 001 FR-023g)**

- Mỗi mong muốn, sở thích, thói quen, lựa chọn, ý kiến hiện kèm `source-tag` (nhãn người nói) và một dòng `caption` "Ghi bởi <người ghi>, <dd/MM/yyyy>". Nhãn: "Theo lời người cao tuổi" (xanh trời), "Theo <quan hệ>, <họ tên người thân>" (xanh chàm, luôn ghi rõ người nào), "Nhân viên quan sát" (nền phụ). Trang giới thiệu (2.1) dùng cùng component, chữ theo câu chuyện ("Theo lời bà").
- Thứ tự: tiếng người cao tuổi trước, rồi người thân, rồi nhân viên.
- Người cao tuổi và người thân nói khác nhau thì hiện hai dòng riêng cạnh nhau; không gộp, không chọn hộ, không gạch, không làm mờ ý kiến không được chọn.
- Mục đã ghi không có nút Sửa, Xóa. Thay đổi bằng nút "Ghi ý kiến mới"; mục cũ vẫn xem được ở "Lịch sử" của chính nhóm đó.
- Sở thích: mỗi nhóm một khối, trong khối mỗi nguồn tối đa một dòng hiện hành (Q-363).
- Quyết định có người quyết khác người nói (phiếu nguyện vọng, thỏa thuận lựa chọn có rủi ro, ý kiến vào ở) ghi riêng dòng "Người quyết định: …" theo BR-M01-15, tách khỏi danh sách ý kiến.

**Năng lực xét theo từng quyết định (Q-348, 001 FR-023h)**

- Không có nhãn, cột, bộ lọc, biểu tượng hay màu nào nói về năng lực của người cao tuổi ở danh sách, hồ sơ, thẻ việc, sơ đồ giường, menu.
- Nhận định "tự quyết được việc này hay không" chỉ hiện bên trong chính quyết định đó (phiếu nguyện vọng cuối đời, thỏa thuận lựa chọn có rủi ro), kèm Bác sĩ ghi, thời điểm, cách đã hỗ trợ người cao tuổi hiểu thông tin và ngày xem lại.
- Màn đời sống hằng ngày (sở thích, hoạt động, khách thăm, người đón) không có ô nhập hay dòng hiển thị về năng lực.
- Tình trạng do Tòa án tuyên chỉ hiện ở bản đồng ý và hồ sơ người đại diện theo pháp luật (người thân quan hệ "Khác", Q-359), kèm quyết định của Tòa án làm bằng chứng.

**Lựa chọn của người cao tuổi (Q-349, Q-350, Q-356, Q-360, Q-361)**

- Thỏa thuận lựa chọn có rủi ro hiện bằng `choice-note` trên mọi thẻ việc và khung ghi nhận của người đó (005 FR-030): "Lựa chọn của người cao tuổi: … Đã trao đổi rủi ro, xem lại dd/MM/yyyy." Nền xanh trời như nhãn tiếng người cao tuổi; không dùng hổ phách, đỏ (đây không là cảnh báo); không chặn việc, không có nút. Thứ tự trên thẻ: dị ứng, lựa chọn, lưu ý chăm sóc.
- "Không thực hiện" có loại lý do bắt buộc, chọn bằng danh sách nút; "Người cao tuổi từ chối" luôn đứng đầu; ghi chú tùy chọn, gợi ý ghi điều người cao tuổi nói. Kết quả hiện tông `neutral` như mọi việc Không thực hiện, không tô màu riêng cho từ chối.
- Ý kiến vào ở: lệnh Hoàn tất tiếp nhận hiện ý kiến hiện hành của người cao tuổi kèm nhãn người nói trước khi bấm. Chưa ghi ý kiến thì `Alert` tông `attention` cảnh báo, không chặn. Ý kiến "Không đồng ý" thì ô lý do của lệnh bắt buộc và đặt ngay dưới dòng ý kiến.
- Nhắc ngày xem lại là thông báo mức Nhẹ, không đổi màu thẻ, không thêm dấu trên hồ sơ.

**Riêng tư và phạm vi thông báo (Q-351 → Q-354, Q-357, Q-358, Q-362, Q-364)**

- Thông báo sự cố, bản tin chia hai khối có tiêu đề: "Phần chung" (có sự cố, loại, mức, thời điểm, viện đã xử lý) và "Phần sức khỏe" (biểu tượng khóa). Thông báo sự cố cho người thân do hệ thống tự gửi theo phạm vi đồng ý (009, 007 FR-082), nhân viên không soạn: chi tiết sự cố hiện người thân nhận từng phần và bản ghi gửi. Bản tin do Điều dưỡng soạn (14.5): mục sự cố cũng chia hai khối và ghi người thân nào thấy phần sức khỏe. Người thân chưa được đồng ý chỉ thấy dòng khóa "Phần sức khỏe chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ", không lộ tiêu đề hay độ dài của nội dung bị ẩn.
- Gửi ngoài phạm vi đồng ý (Q-354): bản ghi gửi hiện tên trường hợp áp dụng (ví dụ "Khẩn cấp, người liên hệ chính, nội dung tối thiểu").
- Mong muốn về khách thăm, người đón: màn lượt thăm, lượt đón và danh sách người được phép đón hiện mong muốn đang hiệu lực của người cao tuổi cạnh tên người thân, kèm nhãn người nói. Yêu cầu trái mong muốn hiện `Alert` tông `attention` "Trái mong muốn của người cao tuổi" và trạng thái chờ Quản lý viện duyệt; khung duyệt bắt buộc lý do và ô xác nhận "Đã trao đổi với người cao tuổi". Yêu cầu bỏ người được phép đón không có cảnh báo này.
- Giao diện không tự đổi quyền hay tự ẩn người theo mong muốn (Q-307); chỉ hiện và để người có quyền ra lệnh.

**Lời văn**

- Gọi người cao tuổi bằng họ tên (hoặc "người cao tuổi" khi không có tên); không gọi bằng bệnh hay số giường ("bệnh nhân sa sút trí tuệ", "giường 202-A").
- Mô tả việc đã xảy ra, không gắn nhãn con người: "từ chối", không "không hợp tác", "chống đối".
- Không có chữ, biểu tượng hay màu nào ngụ ý người cao tuổi là gánh nặng hay đối tượng bị quản lý.

### 2.3 Cổng người thân

Cổng người thân là bản web (`src/features/family-portal/`, đường dẫn `/family`; đăng nhập ở `/login`, màn chung với nhân viên từ 002 T045), khung riêng, không dùng `AppShell` và menu trái của nhân viên. Đăng nhập xong vào Trang chủ (mục 2.1). Trong bản mẫu nhân viên, chọn vai trò Người thân cũng mở cổng này. App người thân trên mobile làm theo mục 15. Các màn P1 → P11 ở `docs/thiet-ke/man-hinh-theo-actor.md` (M37).

- **Đầu trang:** cùng kiểu đầu trang của trang chủ: dải nền trời dính đầu, logo (về Trang chủ), các mục, tên người đăng nhập, nút Đăng xuất.
- **Máy tính (từ 1024px):** 6 mục trên đầu trang dạng viên thuốc (Trang chủ, Hôm nay, Bản tin, Thăm, Thông báo, Thêm). Trang chủ rộng toàn màn như mục 2.1; các màn khác rộng tối đa 960px; Hôm nay chia hai cột, thẻ người cao tuổi chiếm cả hàng.
- **Điện thoại:** thanh dưới 5 mục (Trang chủ, Hôm nay, Bản tin, Thăm, Thêm), cao 56px, mục đang chọn có nền xanh trời quanh biểu tượng; Thông báo là nút chuông ở đầu trang. Một cột.
- Số việc chờ người xem (thông báo cần xác nhận đã nhận, việc chờ người đại diện quyết định) hiện trên Thông báo bằng viên xanh đêm, không dùng đỏ.
- **Hiện theo quyền:** mục người thân không có quyền thì không hiện (spec 012 User Story 5): không có quyền xem sức khỏe thì không có mục Sức khỏe, lịch không có giờ thuốc, công việc chăm sóc; trong bản tin và sự cố, phần sức khỏe thay bằng dòng khóa của mục 2.2. Chi phí cho người có quyền xem chi phí; hợp đồng, việc chờ quyết định cho người đại diện; đồ gửi cho người đại diện và người được phép đón. Khoản thuốc chỉ hiện mã vật phẩm khi người xem không có quyền xem sức khỏe.
- **Trang Tài khoản** liệt kê quyền của người xem (Có, Không) để người thân biết mình được xem gì; không sửa quyền ở đây.
- **Lệnh của người thân** theo cùng quy tắc form (mục 7.2): từ chối, mở lại bắt buộc lý do; đăng ký thăm từ chối ngay kèm lý do cụ thể, không tạo lượt.
- **Bản mẫu:** hiện theo một người thân có đủ quyền (người đại diện, người liên hệ chính, có tên trong bản đồng ý) để thấy mọi phần; quy tắc ẩn theo quyền vẫn có trong mã.

## 3. Màu

### 3.1 Bảng màu

| Tên | Sáng | Tối | Dùng cho | Biến |
| --- | --- | --- | --- | --- |
| Nền trang | dải `#B4C8DD` 0% → `#C7DCEB` 58% → `#E2EDF5` 100% (dải trời gốc `#779BC1` → `#9ABFDA` → `#CBDFEC` pha 45% trắng) | `#0F1A2A` → `#15233A` → `#1B2B42` | Nền toàn trang, cố định theo khung nhìn | `--canvas-from`, `--canvas-mid`, `--canvas-to`; `--background` là màu trơn dự phòng (điểm giữa) |
| Thẻ, khối | `#FFFFFF` | `#18243A` | Page-header, thẻ, bảng, khung chi tiết | `--card` |
| Mặt nổi | `#FFFFFF` | `#1E2B44` | Ô nhập, nút lọc, menu nổi, khung ghi nhận | `--popover` |
| Chữ | `#0F2235` | `#E6EDF6` | Chữ chính | `--foreground` |
| Chữ phụ | `#50627A` | `#9AABC2` | Chữ phụ, nhãn; chỉ trên thẻ hoặc nền phụ | `--muted-foreground` |
| Nền phụ | `#F5F7FA` | `#1E2B44` | Nhãn trung tính, ô số nhỏ, rãnh thanh tiến độ | `--muted`, `--secondary` |
| Đường kẻ | `#EFEFF1` | `#2A3A52` | Viền, đường chia | `--border` |
| Viền điều khiển | `#868C98` | `#6E80A0` | Viền ô nhập | `--input` |
| Xanh đêm | `#24496F`, chữ trắng | `#E6EDF6`, chữ `#0F1A2A` | Nút chính, mục lọc đang chọn, tiêu đề trang, vạch giờ hiện tại | `--primary`, `--primary-foreground` |
| Xanh trời đậm | chữ `#0B5F8A` / nền `#C0EAFF` | `#8DB4E2` / `#22344F` | Giờ hiện tại, nút tonal, giường giữ chỗ | `--accent-foreground`, `--accent` |
| Xanh thương hiệu | `#2597D0` | `#6F9FD6` | **Chỉ** đồ họa: thanh tiến độ | `--chart-2` |
| Xanh lá | nền `#E2F4EA` / chữ `#1F6B45` | `#173A2A` / `#7FD3A3` | Đã xong, ổn định | `--success`, `--success-foreground` |
| Hổ phách | nền `#FFECC0` / chữ `#8A5300` | `#3A2C10` / `#FFCB6B` | Cần chú ý | `--warning`, `--warning-foreground` |
| Đỏ | `#C9302C`, nền `#FFECEC` | `#FF7A7E`, nền `#3A1A1C` | **Chỉ** việc gấp | `--destructive`, `--destructive-subtle` |
| Sơ đồ giường | Đang sử dụng `#4A6FD6`, giữ chỗ (viền) `#1A8CC4`, Chờ sẵn sàng `#C77A12`, Đang bảo trì `#7A8699` | `#7B9BF0`, `#4FB6E6`, `#F2A646`, `#8A97AD` | **Chỉ** ô giường trong sơ đồ (`bed-cell`), không làm chữ hay nền chữ | `--bed-occupied`, `--bed-held`, `--bed-pending`, `--bed-maintenance` |
| Xanh chàm | nền `#C0D5FF` / chữ `#2A4A8F` | `#16273D` / `#9CC3FF` | Giường đang sử dụng, ảnh đại diện | `--info`, `--info-foreground` |
| Focus | `#24496F`; trong menu `#0F2235` | `#8DB4E2`; trong menu trắng | Viền focus bàn phím | `--ring`, `--sidebar-ring` |

Chưa có chỗ dùng cho `#0099FF` của letters.app (chỉ đạt 3,00:1 trên nền trắng); không thêm vào.

### 3.2 Menu trái

| Phần | Sáng | Tối | Biến |
| --- | --- | --- | --- |
| Nền | dải `#8BAACA` 0% → `#A9C9E0` 58% → `#D3E4EF` 100% (dải trời gốc pha 15% trắng: cùng sắc, cùng điểm dừng với nền trang, đậm hơn nền trang một bậc) | `#0B1320` → `#0E1829` → `#121D2D` | `--sidebar-from`, `--sidebar-mid`, `--sidebar-to`; `--sidebar` là màu trơn dự phòng |
| Chữ, biểu tượng mục menu, tên vai trò | `#0F2235` (màu chữ chính), mục menu `type-label` 14/500 | `#FFFFFF` | `--sidebar-foreground` |
| Nhãn nhóm, chữ phụ trong menu | `#14304F` | `#B8C6DA` | `--sidebar-muted-foreground` |
| Mục đang chọn | viên thuốc `#24496F`, chữ trắng, đậm 500 (cùng màu nút chính) | viên thuốc `#E6EDF6`, chữ `#10203A` | `--sidebar-accent`, `--sidebar-accent-foreground` |
| Rê chuột | `#FFFFFF73` | `#FFFFFF1F` | `--sidebar-hover` |
| Logo | ô `#24496F`, lá trắng | ô sáng, lá xanh đêm | `--sidebar-primary`, `--sidebar-primary-foreground` |
| Đường ngăn | `#0F22351F`, bên phải (máy tính) hoặc bên dưới (màn hẹp); đường trên khối người dùng | `#FFFFFF26` | `--sidebar-border` |

- Menu dính, cao bằng màn hình trên máy tính; dải màu vẽ trên toàn chiều cao menu, nội suy sRGB (`bg-linear-to-b/srgb`) để khớp nền trang.
- Muốn đổi độ sáng menu thì đổi tỷ lệ pha trắng, giữ cùng ba điểm dừng, rồi kiểm lại chữ menu và viên thuốc xanh đêm trên điểm đậm nhất. Menu luôn đậm hơn nền trang (hai dải cùng gốc, menu pha ít trắng hơn), không được sáng hơn nội dung; đổi một dải thì đổi cả hai.

### 3.3 Sáng, tối

- Chế độ sáng là mặc định. Chế độ tối chỉ bật khi người dùng bấm nút sáng/tối ở khối người dùng cuối menu; ở web lựa chọn lưu trong `localStorage` (khóa `elder-care-theme`, `src/common/theme.js`), ở mobile lưu trên máy (mục 15.2); không theo cài đặt của máy.
- Token tối khai báo dưới lớp `.dark` trong `index.css`. Mọi màn phải xem được ở cả hai chế độ.

### 3.4 Độ tương phản (WCAG 2.1)

Ngưỡng bắt buộc: chữ ≥ 4,5:1; viền điều khiển, đồ họa cần nhận biết, mục chọn so với nền ≥ 3:1. Đã kiểm ở chế độ sáng:

| Cặp | Tỷ lệ |
| --- | --- |
| Chữ chính `#0F2235` trên điểm đậm nhất nền trang `#B4C8DD` | 9,42:1 |
| Chữ menu `#0F2235` / nhãn nhóm `#14304F` trên điểm đậm nhất menu `#8BAACA` | 6,69:1 / 5,56:1 |
| Chữ trắng trên nút chính `#24496F` (rê chuột 90%: ≥ 7:1) | 9,30:1 |
| Viên thuốc xanh đêm `#24496F` so với nền menu (điểm đậm nhất / nhạt nhất) | 3,85:1 / 7,14:1 |
| Chữ trắng trên viên thuốc xanh đêm | 9,30:1 |
| Ô giường so với nền thẻ trắng: Đang sử dụng, giữ chỗ, Chờ sẵn sàng, Đang bảo trì | 4,63 / 3,76 / 3,38 / 3,69:1 |
| Chữ phụ trên thẻ trắng / trên nền phụ | 6,23:1 / 5,81:1 |
| Chữ nhấn `#0B5F8A` trên `#C0EAFF` | 5,45:1 |
| Chữ đỏ trên nền đỏ nhạt (thấp nhất) | 4,68:1 |
| Viền ô nhập `#868C98` trên trắng | 3,38:1 |
| Xanh thương hiệu trên rãnh thanh tiến độ (nền phụ) | 3,05:1 |

Không đạt nên cấm: xanh thương hiệu làm chữ (3,27:1 trên trắng); chữ trắng hay chữ phụ trực tiếp trên nền trời.

### 3.5 Trạng thái → tông màu

Tên trạng thái lấy đúng theo `nghiep-vu.md`. `StatusBadge` ánh xạ theo bảng này; trạng thái mới chưa có thì xếp `neutral` và bổ sung bảng trong cùng thay đổi.

| Tông | Hiển thị | Trạng thái |
| --- | --- | --- |
| `urgent` | Chữ đỏ trên nền đỏ nhạt | Thông báo mức Khẩn cấp; Công việc Quá hạn; người cao tuổi Quá hạn đánh giá lại (cột Đánh giá lại, dải đầu tab Đánh giá); sự cố, cảnh báo mức Khẩn cấp; cảnh báo "đã leo thang tối đa"; liều thuốc Trễ, Bỏ lỡ; viền đỏ của thẻ cảnh báo Khẩn cấp hoặc đã leo thang tối đa |
| `attention` | Chữ hổ phách trên nền hổ phách | Thông báo mức Trung bình, Gửi thất bại, Không liên lạc được; liên hệ trực tiếp Chờ liên hệ; Công việc Đến hạn; liều thuốc Đến giờ; cảnh báo, sự cố mức Trung bình; cảnh báo Mới, Leo thang (chưa tối đa); chỉ số Vượt ngưỡng; người cao tuổi Tạm vắng, Điều trị tại bệnh viện, Đang tiếp nhận; giường Chờ sẵn sàng; phòng đang cách ly; bàn giao Có ý kiến; yêu cầu thay đổi quyền người thân Chờ xác nhận; đề nghị mua hộ Chờ đồng ý; phản hồi Đang xử lý, Mở lại; đồ gửi Thất lạc, Hư hỏng; yêu cầu phê duyệt Chờ duyệt, Áp dụng không thành; tài khoản Khóa tạm; quyền Đã thu hẹp (lệch ma trận); dị ứng (`AllergyNote`, có viền và biểu tượng cảnh báo) |
| `settled` | Chữ xanh lá trên nền xanh lá nhạt | Thông báo Đã xem, Đã xác nhận; liên hệ trực tiếp Đã liên hệ; kênh thông báo Đang bật; Công việc Hoàn thành; liều thuốc Đã dùng; cảnh báo Đã đóng; bàn giao Đã xác nhận; người cao tuổi Đang lưu trú; lượt thăm Đã duyệt; phản hồi Đã phản hồi; hợp đồng Hiệu lực; quyết định của người thân Đã đồng ý, Đã xác nhận; yêu cầu phê duyệt Đã duyệt, Đã áp dụng; tài khoản Hoạt động |
| `held` | Chữ xanh trời đậm, viền cùng màu, nền trong | Giường Đang giữ chỗ (ghi kèm lý do) |
| `occupied` | Chữ xanh chàm trên nền xanh chàm | Giường Đang sử dụng; lượt thăm Đã vào |
| `neutral` | Chữ phụ trên nền phụ | Thông báo mức Nhẹ, Chờ hết giờ yên tĩnh, Đã gửi, Không gửi; liên hệ trực tiếp Không liên hệ được; kênh thông báo Đã tắt; lỗi gửi (trừ Gửi thất bại); Lưu ý chăm sóc (`CareNote`, chữ chính trên nền phụ, biểu tượng thông tin); công việc Chưa đến hạn, Hoàn thành trễ, Không thực hiện, Hủy; giường Trống, Đang bảo trì, Tạm ngừng sử dụng, Không sử dụng; người cao tuổi ở trạng thái cuối; lượt thăm Đã ra, Không đến, Hủy; phản hồi Mới, Đóng; đồ gửi Đang giữ, Đang được người cao tuổi sử dụng; kỳ chi phí Tạm tính, Đã chốt; quyết định Đã từ chối; liều thuốc Chưa đến giờ, Từ chối, Không thực hiện, Tạm dừng, Mang theo, Đã hủy; cảnh báo, sự cố mức Nhẹ; cảnh báo Đã tiếp nhận, Chuyển sự cố; bàn giao Bản nháp, Đã lập; bản ghi Đã đính chính, Đã hủy ghi nhận (`CorrectionBadge`); yêu cầu phê duyệt Nháp, Từ chối, Đã hủy; tài khoản Không hoạt động; quyền Theo ma trận |
| `neutral-outline` | Chữ phụ, viền màu viền điều khiển (`--input`, 3,38:1 trên trắng), nền trong | Tài khoản Đã khóa (tách khỏi Không hoạt động cùng tông trung tính, chủ dự án duyệt 2026-10-20) |

### 3.6 Ảnh đại diện

Bốn cặp màu: xanh trời nhạt, xanh chàm nhạt, xanh lá nhạt, xám (`--avatar-1` … `--avatar-4`); chọn cố định theo mã người cao tuổi. Không dùng hổ phách, đỏ cho ảnh đại diện.

## 4. Chữ

- Một họ font **Be Vietnam Pro** (dấu tiếng Việt rõ ở cỡ nhỏ), nạp 400, 500, 600 từ `@fontsource/be-vietnam-pro`; không thêm độ đậm khác, không dùng font monospace.
- Thang cỡ dùng utility `type-*` trong `index.css`: `caption` 12/500, `body-sm` 14, `label` 14/500, `body` 15, `card-title` 16/600, `title` 18/600, `metric` 24/600 (số, giãn −0,01em), `page` 26/600 (giãn −0,01em), `clock` 28/600. Không khai báo khóa `--text-*` (vì `cn()` hiểu nhầm thành màu).
- Trong màn nghiệp vụ không dùng `text-sm`, `text-base`, `font-medium`, `font-semibold` rời để đặt cỡ hay độ đậm; chọn một `type-*`. `cn()` không biết `type-*`, nên không đặt `text-*` mặc định trong component nhận `type-*` từ nơi gọi (ví dụ `CardTitle` chỉ giữ `font-heading`, nơi gọi luôn truyền `type-*`). Component shadcn giữ `text-sm`, `text-xs` sẵn có khi trùng thang (14/20, 12/16); không dùng cỡ lẻ như `text-[0.8rem]`.
- Trạng thái (đã xong, quá hạn) chỉ đổi màu chữ, không đổi cỡ hay độ đậm, để thẻ không nhảy kích thước.

| Vai trò chữ | Utility | Ví dụ |
| --- | --- | --- |
| Tiêu đề trang | `type-page`, `text-primary` | "Tổng quan tầng 2" |
| Dòng phụ dưới tiêu đề trang, thân bài | `type-body` | Phạm vi, giờ cập nhật |
| Tiêu đề khu vực (thẻ lớn), giờ của khung | `type-title` | "Công việc trong ca theo giờ", "07:00" |
| Số liệu dashboard | `type-metric` | Số trong dải số đếm |
| Tên công việc, tiêu đề thẻ con | `type-card-title` | "Đo chỉ số sức khỏe" |
| Tên người cao tuổi trong thẻ gọn, giờ trong thẻ việc, số đếm chú thích, mục menu | `type-label` | "[Họ tên NCT 1]", "08:30" |
| Nội dung thẻ, chú thích | `type-body-sm` | Giá trị trong thẻ bàn giao |
| Nhãn phụ, nhãn nhóm menu, badge trạng thái, phòng/giường, "Phụ trách: …" | `type-caption` (badge: `text-xs font-medium` của component, cùng giá trị) | "Quá hạn", "201-A" |
| Giờ hiện tại | `type-clock`, `text-accent-foreground` | Ô phải của page-header |

- Nhãn số liệu dài cho phép xuống hai dòng (`text-balance`); số liệu canh đáy ô để các số trong một hàng thẳng nhau.
- 600 cho tiêu đề trang, tiêu đề thẻ, giờ hiện tại; 500 cho nhãn, nút; 400 cho thân bài.
- Tiêu đề trang màu xanh đêm (`text-primary`); giờ hiện tại màu xanh trời đậm (`text-accent-foreground`).
- Giờ, ngày, số đếm, số tiền dùng `tabular-nums`.
- Trang giới thiệu cổng người thân (mục 2.1) có thêm `display` 38/46 (từ 768px 56/64, 600, giãn −0,02em), `section` 24/32 (từ 768px 28/36, 600), `lead` 18/28, `reading` 16/26; không dùng các cỡ này trong màn nghiệp vụ.
- Viết hoa như câu thường; không viết hoa toàn bộ, không tô nổi một từ trong tiêu đề. Dòng dài tối đa khoảng 80 ký tự.

## 5. Bố cục

- **Khung chung** (`AppShell`): lưới hai cột, menu trái rộng 240px, nội dung bên phải (lề 16px, từ 768px là 32px; trên dưới 24px). Không có thanh trên. Căn trái toàn bộ.
- **Menu:** logo, các nhóm mục theo vai trò (nhãn nhóm `caption`), cuối là khối người dùng: ảnh đại diện vai trò, tên vai trò, tầng và ca, nút sáng/tối. Mục menu bo tròn, cao tối thiểu 40px, biểu tượng 16px.
- **Đầu mỗi màn:** `page-header`, khối thẻ trắng bo 16px gồm tiêu đề nói phạm vi bằng câu bình thường, dòng phụ, ô bên phải (giờ, thao tác) và thanh lọc hoặc ô tìm ở dưới. Không đặt dãy thẻ số liệu lớn ở đầu trang; số đếm nằm trên nút lọc hoặc trong dải số đếm gọn.
- Màn nhân viên rộng tối đa 1600px (cổng người thân theo mục 2.3); khoảng cách giữa các khối 24px, giữa các thẻ 12px. Dùng `gap-*`, không dùng `space-x-*`/`space-y-*`.
- **Khung chi tiết:** `Sheet` mở bên phải, bo 20px phía trái.

### 5.1 Lưới thẻ hay bảng

Một thẻ là **một đối tượng nghiệp vụ người dùng thao tác riêng lẻ**. Cần tìm, so sánh cột hay sắp xếp nhiều dòng thì dùng bảng.

| Màn | Dạng | Một thẻ là |
| --- | --- | --- |
| Phòng, giường | Lưới thẻ, chia theo khu và tầng | Một phòng; giường là ô bên trong |
| Checklist chăm sóc | Lưới thẻ, nhóm Quá hạn → Đến hạn → Chưa đến hạn → Đã xong | Một công việc chăm sóc |
| Phát thuốc | Lưới thẻ, nhóm theo giờ dự kiến (giờ có liều Trễ, Đến giờ mở sẵn; giờ sau thu gọn; Đã xong cuối) | Một người cao tuổi trong một giờ dự kiến |
| Cổng người thân | Thẻ | Một người cao tuổi |
| Danh sách người cao tuổi, chi phí, số dư, nhật ký, lịch ca, tham số | Bảng | — |
| Tổng quan | Dải số đếm, cảnh báo, lưới việc theo khung giờ, sơ đồ giường theo phòng | Một công việc (trong hàng của khung giờ); không dùng lưới thẻ số liệu |

Lưới dùng `repeat(auto-fit, minmax(<min>, 1fr))` để thẻ giãn lấp đầy hàng, kèm giới hạn rộng mỗi thẻ (`*:max-w-[480px]`) để thẻ lẻ không kéo dài cả màn: thẻ công việc, thẻ thuốc tối thiểu 280px, tối đa 480px; thẻ phòng tối thiểu 220px; thẻ việc ở Tổng quan tối thiểu 220px, tối đa 360px.

## 6. Hình khối, độ nổi

- Thẻ nền trắng, viền 1px màu Đường kẻ, không đổ bóng. Chỉ lớp nổi (menu nổi, khung chi tiết, hộp thoại) có một tầng bóng.
- Bo góc theo cấp: 6px điểm nhấn, 10px ô giường, 12px ô nhập và khối nhỏ, 16px thẻ, 20px khung chi tiết và hộp thoại; nút, mục menu, badge, nút lọc, ảnh đại diện bo tròn hẳn.
- Không làm mờ nền, không kính mờ, không hiệu ứng trang trí. Gradient chỉ ở nền trang và menu (mục 3).

## 7. Component

Dùng component shadcn (kiểu `base-nova`, nền Base UI) trước khi tự viết; thêm bằng `npx shadcn@latest add` vào `src/components/ui/`. Dự án dùng JavaScript: ví dụ `.tsx` viết lại thành `.jsx`. Thành phần dùng chung của dự án để ở `src/common/`, màn hình ở `src/features/<tên>/`.

### 7.1 Nút

| Biến thể | Giao diện | Dùng cho |
| --- | --- | --- |
| `default` | Nền xanh đêm, chữ trắng, rê chuột 90% | Nút chính, một nút mỗi màn; nút Ghi nhận khi Quá hạn |
| `tonal` | Nền `#C0EAFF`, chữ `#0B5F8A` | Thao tác lặp lại nhiều lần trên một màn (Ghi nhận khi Đến hạn) |
| `outline`, `secondary`, `ghost` | Theo token | Thao tác phụ |
| `destructive` | Chữ đỏ trên nền đỏ nhạt | Chỉ thao tác xóa, hủy có hậu quả |

Nút bo tròn, cao 40px (nhỏ 32px, lớn 44px); nút trong form ghi nhận cao 44px.

### 7.2 Công thức

| Công thức | Component | Quy định |
| --- | --- | --- |
| `page-header` | `PageHeader` | Mục 5 |
| `status-pill` | `Badge` + biến thể tông | Bo tròn, chữ 12/500, luôn có chữ trạng thái (mục 3.5) |
| `task-card` | `Card` | Hàng 1: giờ bắt đầu 16/600 (trái), nhãn trạng thái (phải). Hàng 2: tên công việc `card-title`. Hàng 3: ảnh đại diện, tên người cao tuổi, phòng/giường. Hàng 4: người được phân công ("Phụ trách: …", chưa có thì "Chưa ai nhận"). Hàng 5 (nếu có): dị ứng (chỉ với việc ăn, uống), lựa chọn của người cao tuổi (`ChoiceNote`), rồi lưu ý chăm sóc; lựa chọn và lưu ý tối đa 2 dòng. Hàng cuối, cách bằng đường kẻ: trái là ghi chú ngắn (Bắt buộc, Tồn từ ca trước), phải là nút **Ghi nhận** gọn: `tonal` khi Đến hạn, `default` khi Quá hạn |
| Thẻ quá hạn | `task-card` | Viền đỏ 1,5px, giờ màu đỏ, nút Ghi nhận tô đầy |
| Thẻ đã xong | `task-card` | Nền trắng, chữ phụ, không có nút; ghi kết quả ngắn |
| Việc chung của tầng | `task-card` | Thay ảnh đại diện bằng biểu tượng tầng; ghi "Việc chung của tầng" |
| `care-notes` | `AllergyNote`, `ChoiceNote`, `CareNote` | Dị ứng và lưu ý chăm sóc luôn là hai khối khác kiểu (mục 3.5) để dị ứng không chìm giữa các lưu ý; `ChoiceNote` nền xanh trời, biểu tượng bắt tay (mục 2.2). Thứ tự: dị ứng, lựa chọn, lưu ý. Dùng chung ở thẻ việc, khung ghi nhận, hồ sơ. Không có gì thì ghi "Chưa ghi nhận", không ghi "Không có" |
| `resident-photo` | `ResidentPhoto` trong hồ sơ | Ảnh tròn 64px, `alt` là "Ảnh <họ tên>"; chưa có ảnh thì chữ cái kèm "Chưa có ảnh" |
| `about-person` | Khối "Về người này" trong hồ sơ | Nhu cầu chăm sóc (nhãn nền phụ), nhân viên phụ trách chính, sở thích hiện hành theo nhóm; trong nhóm mỗi nguồn một dòng: mức, `source-tag`, "Ghi bởi …, dd/MM/yyyy" (mục 2.2). Sau khối An toàn |
| `source-tag` | `SourceTag` | Viên thuốc cao 24px, chữ `caption`: `resident` xanh trời, `family` xanh chàm, `staff` nền phụ, `done` xanh lá (chỉ trang giới thiệu). Không truyền chữ thì dùng chữ mặc định theo nguồn |
| `handover-card` | `Card` | Ca → ca, trạng thái; người bàn giao, người xác nhận, số công việc tồn, số cảnh báo chưa xử lý; ở Có ý kiến ghi rõ ai phải bổ sung (Q-87) |
| Lưới việc theo khung giờ (tổng quan) | `ol` trong `Card` | Mỗi khung giờ tròn một hàng: bên trái giờ `title` và số việc (khung đang diễn ra màu xanh trời đậm, ghi "Bây giờ"), bên phải lưới thẻ việc `minmax(220px, 1fr)`, vì một khung có nhiều việc của nhiều người cao tuổi, nhiều người phụ trách (Bác sĩ, Điều dưỡng, Nhân viên chăm sóc). Thẻ việc gọn: giờ và nhãn trạng thái; tên việc kèm "Bắt buộc"; ảnh đại diện, người cao tuổi, phòng; cuối thẻ "Phụ trách: …" hoặc "Chưa phân công". Quá hạn viền đỏ 1,5px; đã xong chữ phụ. Dưới 768px giờ nằm trên lưới |
| `resident-avatar` | `ResidentAvatar` | Tròn 32px, chữ cái đầu của tên gọi (từ cuối họ tên), màu theo mã người cao tuổi, `aria-hidden` |
| `room-card` | `Card` | Tiêu đề số phòng, bên phải chính sách giới tính; dòng phụ loại phòng, mức chăm sóc được phép; ô giường lưới 2 cột; phòng cách ly ghi "Đang cách ly" (tông `attention`) |
| `bed-cell` | `button` trong thẻ phòng | Mã giường, rồi người đang ở hoặc trạng thái; tông theo mục 3.5. Sơ đồ nhỏ ở Tổng quan chia theo phòng (khối nền phụ, số phòng, 4 ô cao 28px), ô dùng màu đặc `--bed-*`: Đang sử dụng xanh chàm, Chờ sẵn sàng hổ phách, Đang bảo trì xám đậm; Đang giữ chỗ viền 2px `--bed-held` trên nền xanh trời nhạt; Trống viền mảnh trên nền trắng; Tạm ngừng sử dụng, Không sử dụng viền nét đứt. Chú thích luôn có chữ và số đếm |
| `filter-bar` | `ToggleGroup` | Nút bo tròn nền trắng; mục đang chọn nền xanh đêm chữ trắng; nút Quá hạn nền đỏ nhạt khi số > 0 |
| `shift-bar` | `div` | Thanh 8px bo tròn trên rãnh nền phụ, phần đã qua màu xanh thương hiệu, vạch dọc xanh đêm ở giờ hiện tại; giờ hiện tại `clock` kèm khung giờ ca ở ô phải của page-header |
| `detail-panel` | `Sheet` có `SheetTitle` | Thông tin đối tượng, rồi các nút lệnh lấy từ danh sách lệnh được phép |
| `dose-card` | `Card` | Hàng 1: ảnh đại diện, tên, phòng, nhãn trạng thái của thẻ (theo liều gấp nhất). Dị ứng (mọi thẻ thuốc). Mỗi liều: tên thuốc `card-title`, liều và đường dùng, nhãn nguồn ("Thuốc viện", "Gia đình gửi"), "Kiểm soát đặc biệt, phải trực tuyến", "Liều chung của tầng". Hàng cuối: nút **Phát thuốc** `tonal` khi Đến giờ, `default` khi Trễ; chỉ Điều dưỡng. Thẻ Trễ viền đỏ 1,5px. Liều Tạm dừng, Mang theo chỉ có chữ, không nút |
| `dose-sheet` | `Sheet` | Ảnh nhận diện 64px (chưa có ảnh thì chữ cái và "Chưa có ảnh"), tên, phòng, giờ và cửa sổ, dị ứng; mỗi liều một khối: Đã dùng / Từ chối / Không thực hiện (nút chia ba); Đã dùng: chọn lô khi gia đình gửi (không chọn sẵn, lô hết hạn ghi rõ và bị chặn), thời điểm dùng (điền sẵn giờ hiện tại, sau cửa sổ ghi "dùng trễ"), phản ứng; Từ chối, Không thực hiện: lý do bắt buộc. Một nút lưu cho các liều đã chọn, mỗi liều lưu riêng |
| `handover-draft` | Hai `Card` (5:7) | Màn Bàn giao ca, người bàn giao ở Bản nháp: bên trái bản ghi nguồn đang mở theo nhóm spec 008 FR-039 (a) → (g), mỗi dòng có nút "Thêm" (không lập sẵn mục); bên phải nội dung bàn giao: mỗi mục một khối có nhóm, người cao tuổi, trạng thái nguồn, "Cần ghi chú" khi nghiêm trọng, ghi chú, "Ghi đã xử lý" khi nguồn đã xong, "Bỏ mục" (mục chỉ số vượt ngưỡng, biến động người cao tuổi ghi "không bỏ được"); vấn đề cần theo dõi, nhận định chung; "Hoàn tất bàn giao" liệt kê điều còn thiếu |
| `handover-confirm` | Hai `Card` (7:5) | Đã lập: nội dung đã chốt, mỗi mục "Lúc chốt" và "Nay" khi khác; bên phải bản ghi nguồn mới sau khi lập (chỉ xem), ô ý kiến, "Gửi ý kiến", "Xác nhận bàn giao". Có ý kiến: người bàn giao thấy ý kiến và ô phản hồi bắt buộc. Cuối màn bảng các bàn giao trước |
| `alert-card` | `Card` | Ảnh đại diện, tên, phòng, nhãn mức; nội dung cảnh báo `card-title`; nguồn và số lần gộp; nhãn trạng thái, người phụ trách; hạn tiếp nhận; nút theo trạng thái (Tiếp nhận → Bắt đầu xử lý → Đóng cảnh báo) và "Chi tiết". Xếp Khẩn cấp trước, rồi leo thang tối đa. Khung chi tiết: lịch sử, Đóng (kết quả bắt buộc), Đổi mức (lý do bắt buộc; hạ từ Khẩn cấp chỉ Bác sĩ), Chuyển sự cố, Kích hoạt khẩn cấp |
| `prn-card` | `Card` | Thuốc khi cần: tên thuốc, khoảng cách tối thiểu, số lần tối đa trong 24 giờ, lần gần nhất; bị chặn thì khối hổ phách "sớm nhất được dùng lúc …", không có nút vượt chặn |
| `alert-banner` | `Alert` | `urgent` chỉ cho cảnh báo cần xử lý ngay; `attention` (hổ phách, có viền) cho điều cần chú ý mà không gấp: trái mong muốn của người cao tuổi, chưa ghi ý kiến vào ở (mục 2.2) |
| `opinion-history` | `Sheet` (`OpinionHistorySheet`) | Mở từ nút "Lịch sử" của từng nhóm sở thích: "Đang hiệu lực" (mỗi nguồn một dòng), "Lịch sử" (chữ mức màu phụ, "thay bằng mục mới ngày …"); nút "Ghi ý kiến mới" ở chân, chỉ cho vai trò được ghi |
| `admission-command` | `Sheet` (`CompleteAdmissionSheet`) | Thứ tự: ý kiến vào ở (khối nền phụ có `source-tag`, hoặc `Alert` `attention` khi chưa ghi), ô "Lý do của lệnh" (bắt buộc khi "Không đồng ý"), điều kiện tiếp nhận, nút chính |
| `incident-parts` | Hai `Card` + bảng | "Phần chung", "Phần sức khỏe" (biểu tượng khóa), mỗi phần ghi "Người thân nhận: …"; bảng "Bản ghi gửi" (giờ, người thân, phần đã gửi, căn cứ). Cổng người thân chỉ có Phần chung và dòng khóa |
| `visit-wishes` | `Card`, bảng, `Card` yêu cầu | Khối "Mong muốn của người cao tuổi"; bảng người thân có cột mong muốn kèm `source-tag`; thẻ yêu cầu trái mong muốn: nhãn trạng thái, `Alert` `attention`, khung duyệt (lý do, ô "Đã trao đổi với người cao tuổi") chỉ cho Quản lý viện |
| `notification-inbox` | Danh sách dòng trong `Card`, `Sheet` | Hộp thông báo (009 T035): khối "Khẩn cấp chưa xác nhận" trên cùng, mỗi dòng viền đỏ 1,5px; dưới là danh sách dòng (mức, tiêu đề, thời điểm gửi, "Chưa xem" hoặc "Đã xem"), không dùng lưới thẻ. Thanh lọc mức và "Chưa xem", chọn loại sự kiện. Thông báo mới đến qua SSE thì danh sách tải lại, đọc qua vùng `aria-live`. Khung chi tiết: mức, loại sự kiện, thời điểm gửi và xem; mỗi phần nội dung một khối có tiêu đề theo loại thông tin ("Phần chung", "Phần sức khỏe" có khóa, "Chi phí", "Thăm", "Hợp đồng"); phần đã hết quyền xem thay bằng dòng khóa (mục 2.2); bản ghi nguồn chỉ là chữ khi chưa mở được; nút phụ "Ẩn khỏi danh sách" chỉ khi đã xem |
| `correction-sheet` | `CorrectionBadge`, `CorrectionHistorySheet` (`src/common/correction/`) | Đính chính dùng chung (000 FR-028, FR-029): nhãn "Đã đính chính" hoặc "Đã hủy ghi nhận" đặt cạnh bản ghi trên màn của module. Khung chi tiết: giá trị hiện hành; các lần đính chính theo thứ tự tăng dần (lần, loại "Sửa nội dung"/"Hủy ghi nhận", nội dung đúng, lý do, "Ghi bởi …, dd/MM/yyyy HH:mm, cách A/B/C"); nội dung gốc nguyên văn, không gạch, không làm mờ. Form ở chân khung chỉ khi màn truyền `canCorrect` theo lệnh được phép của bản ghi (mặc định ẩn; quyền thật ở backend): loại đính chính, ô nội dung do module khai (`fields`), lý do bắt buộc, nút "Lưu đính chính"; cách C báo "Đã gửi duyệt, mã yêu cầu …". Bản ghi đã Hủy ghi nhận không có form |
| Form ghi nhận | `FieldGroup`, `Field`, `ToggleGroup` | Kiểu nhập theo loại công việc (ml, mức ăn, giá trị đo, số lượng); "Không thực hiện" bắt buộc loại lý do (danh sách nút, "Người cao tuổi từ chối" đứng đầu), ghi chú tùy chọn (Q-350); ô và nút cao 44px |
| Trống | `Empty` | Nói rõ vì sao trống và làm gì tiếp |
| Đang tải | `Skeleton` | Không tự viết hiệu ứng nhấp nháy |

### 7.3 Trạng thái màn

| Trạng thái | Cách hiện |
| --- | --- |
| Chưa có bản mẫu | `NotDesignedState`: thẻ trắng, biểu tượng, "<tên màn>: chưa có bản mẫu", nói rõ chức năng chưa hoạt động |
| Không có quyền | `PermissionDeniedState`: nói rõ không có quyền và cần liên hệ ai; không hiện dữ liệu, không hiện nút lệnh |
| Đang tải | `Skeleton` theo đúng hình khối sẽ hiện (thẻ, dòng bảng); chưa có màn mẫu |
| Lỗi tải | `Alert` trong thẻ, nói chuyện gì xảy ra và nút Thử lại; chưa có màn mẫu |
| Trống | `Empty` trong thẻ, nói vì sao trống và làm gì tiếp |

Mọi trạng thái đặt trên thẻ trắng, không đặt chữ trực tiếp trên nền trời.

### 7.4 Lệnh và lỗi (research R-03 của 000)

- Nút lệnh chỉ hiện theo danh sách lệnh được phép do backend trả; giao diện không tự suy ra quyền.
- 409: hiện danh sách điều kiện chưa thỏa (`unmetConditions[]`) bằng `Alert` trong khung đang thao tác.
- 412: báo dữ liệu đã đổi, hiện giá trị mới (`currentValue`).
- 422: báo tại ô nhập bằng `data-invalid` trên `Field`, `aria-invalid` trên ô.

## 8. Câu chữ

- Tiếng Việt, thuật ngữ theo mục 2.4 `nghiep-vu.md`; ngày dd/MM/yyyy, giờ 24h (HH:mm), tiền VND (NFR-11).
- Nút nói đúng việc sẽ xảy ra: "Lưu kết quả", "Cho tạm vắng"; không "Gửi", "OK".
- Thông tin ghép viết thành câu ngắn có dấu phẩy. Không nối bằng dấu chấm giữa (·), không dùng kiểu "TỪ — mảnh câu", không thêm mũi tên vào link hay nút; mũi tên chỉ khi có nghĩa chiều ("Ca đêm → Ca ngày").
- Lỗi nói chuyện gì xảy ra và cần làm gì; không xin lỗi, không mơ hồ.
- Lời văn về người cao tuổi theo mục 2.2.
- Đường dẫn URL (`/login`, `/family`), neo trong trang (`#living-space`) và `id` trong mã dùng tiếng Anh, chữ thường nối bằng gạch ngang; chữ hiển thị cho người dùng vẫn bằng tiếng Việt (constitution X, research R-03).
- Không bịa quy tắc nghiệp vụ, quyền hay dữ liệu người cao tuổi. Dữ liệu minh họa để trong một file riêng (`src/common/demoData.js`), ghi rõ là minh họa, tên dạng `[Họ tên NCT 1]`.

## 9. Trợ năng

- Focus bàn phím: viền 2px màu `--ring` (trong menu `--sidebar-ring`), cách 2px, cho mọi nút, link, ô nhập.
- Mọi thao tác dùng được bằng bàn phím; phần bấm được là `button` hay `a` thật, không gắn `onClick` vào `div`.
- Biểu tượng chỉ có hình thì có `aria-label`; biểu tượng trang trí và ảnh đại diện chữ cái có `aria-hidden`.
- Mục menu đang chọn có `aria-current="page"`; nút sáng/tối có `aria-pressed`.
- Vùng bấm tối thiểu 44px trên máy tính bảng, kể cả tiêu đề nhóm thu gọn được; trên app di động 48 logical px (mục 15.5).
- Chuyển động chỉ khi người dùng thao tác, 150–200ms; tắt khi máy bật giảm chuyển động. Không có hiệu ứng tự chạy khi tải trang.

## 10. Co giãn theo màn hình

| Bề rộng | Bố cục |
| --- | --- |
| ≥ 1280px | Menu trái, nội dung tối đa 1600px |
| 768–1279px | Menu trái; lưới thẻ 2–3 cột; khung chi tiết mở đè |
| < 768px | Menu thành thanh ngang cuộn được, đường ngăn bên dưới; lưới thẻ 1 cột; không cuộn ngang trang |

Cổng người thân thiết kế cho điện thoại trước.

## 11. Nên và không nên

**Nên**
- Để việc gấp nổi lên bằng thứ tự (nhóm Quá hạn đứng đầu) trước khi dùng màu.
- Thu gọn những gì đã xong.
- Giữ cảnh báo nổi rõ nhưng không làm cả màn hình căng thẳng.

**Không nên**
- Dùng đỏ cho thứ không gấp (kể cả phòng cách ly, dị ứng, mức Bắt buộc).
- Gradient, kính mờ, bóng đổ ở thẻ, khối, nút; dãy thẻ số liệu to; biểu đồ trang trí.
- Dùng lưới thẻ cho danh sách cần so sánh hay sắp xếp.
- Gắn nhãn năng lực lên người cao tuổi; gộp ý kiến của người cao tuổi và gia đình thành một dòng; đặt nút Sửa, Xóa trên ý kiến đã ghi (mục 2.2).
- Viết hoa toàn bộ nhãn; font monospace cho số liệu.
- Ghi đè màu component bằng `className`; tự đặt `z-index` cho lớp nổi.

## 12. Công cụ và skill

Skill ở `.claude/skills/`, tải nguyên bản, không sửa:

- `frontend-design` (anthropics/skills, commit `dbd4588`): định hướng thẩm mỹ, tránh mặc định "kiểu AI". File này là đề bài đã chốt và thắng khi khác skill.
- `shadcn` (shadcn-ui/ui, commit `c2477f3`): dùng component shadcn đúng cách. Khi nạp, skill tự chạy `npx shadcn@latest info --json` (tải CLI mới nhất qua mạng) và cho phép chạy `npx shadcn@latest …` không cần hỏi.

Công nghệ: React 19, Vite 8, JavaScript, Tailwind CSS v4, shadcn/ui (Base UI), lucide-react; `cn` lấy từ gói npm `cn`; alias `@/` trỏ `src/`; `src/api/` sinh từ contracts, không sửa tay.

## 13. Quy trình

1. Đọc file này, spec của feature và mục nghiệp vụ liên quan.
2. Đề xuất bố cục (phác thảo hoặc mockup) → chủ dự án duyệt → mới sửa mã.
3. Làm từng bước; sau mỗi bước chạy `npm run lint` và `npm run build` (web) hoặc `flutter analyze` và `flutter test` (mobile).
4. Kiểm tra: web trên trình duyệt (`npm run dev`) ở ba bề rộng, chế độ tối, bàn phím, menu từng vai trò (đổi ở khối người dùng); mobile trên máy ảo hoặc máy thật ở điện thoại và máy tính bảng, chế độ tối, cỡ chữ máy 200%, khi mất mạng, từng loại tài khoản. Chỉnh xong một màn rồi mới áp sang màn khác.
5. Đổi quy định hay thêm màu: sửa file này, `index.css` và theme của mobile (khi đã có) trong cùng thay đổi, kiểm độ tương phản (mục 3.4), tăng `version` (đổi lớn: số đầu; thêm quy định: số giữa; sửa câu chữ: số cuối) và ghi vào mục 16.
6. Một màn có trên cả hai nền tảng (cổng người thân, màn nhân viên có cả web và app) thì giữ cùng công thức, cùng thứ tự nội dung và cùng chữ.

## 14. Hiện trạng (web)

- Đã có: khung chung (`AppShell`, menu theo vai trò, khối người dùng, nút sáng/tối), trạng thái màn chưa có và không có quyền, và ba màn mẫu: Tổng quan (dải số đếm, cảnh báo, lưới việc theo khung giờ có người phụ trách, sơ đồ giường theo phòng, thẻ bàn giao ca), Checklist ca (lưới thẻ có người phụ trách, dị ứng tách riêng, lựa chọn của người cao tuổi, loại lý do "Người cao tuổi từ chối"), Danh sách người cao tuổi (bảng kèm khung chi tiết có ảnh nhận diện, khối "Về người này" có sở thích theo nguồn và lịch sử ý kiến, lệnh Hoàn tất tiếp nhận có ý kiến vào ở khi xem bằng vai trò Hành chính). Theo mục 2.2 còn có: chi tiết sự cố hai phần (`incidents`, vai trò Người thân thấy bản cổng) và Người thân (`relatives`, mục menu mới theo dòng "Mong muốn về khách thăm, người đón" của Phụ lục 27).
- Dị ứng hiện trên thẻ việc ăn, uống và trên mọi thẻ thuốc (chủ dự án duyệt 2026-10-18 cho màn Phát thuốc); loại công việc chăm sóc khác chưa chốt.
- Đợt 3 (thẩm mỹ, đang trao đổi): giảm màu cho trạng thái bình thường (Đang lưu trú), bo góc thẻ, dải nền trang.
- Đã có trang chủ giới thiệu viện trong cổng người thân (mục 2.1), màn đăng nhập chung `/login` (002 T045: đăng nhập thật cho nhân viên và người thân, bố cục P1; `/change-password` khi buộc đổi mật khẩu; khung nhân viên dùng vai trò thật và nút Đăng xuất khi có phiên, không có phiên thì giữ ô đổi vai trò của bản mẫu) và cổng người thân `/family` với 11 màn P1 → P11 (mục 2.3). Chờ: ảnh thực tế được phép sử dụng, kênh liên hệ chính thức của viện.
- Đã có màn mẫu Phát thuốc (M14): liều theo lịch nhóm theo giờ, khung xác nhận liều, thuốc khi cần.
- Đã có màn mẫu Bàn giao ca (M17) và Cảnh báo (M15). Bản mẫu: Điều dưỡng là người bàn giao, Trưởng tầng là người xác nhận; trạng thái bàn giao giữ khi đổi vai trò để thử cả luồng.
- Chưa thiết kế: Phòng, giường dạng thẻ phòng; danh sách sự cố và lệnh xử lý sự cố; cổng người thân trên app mobile (mục 15); khi người thân có quan hệ với nhiều người cao tuổi thì chọn người ở đâu.
- Mục 2.2 (4.4.0) đã duyệt 2026-10-18; bố cục màn mẫu A–D duyệt cùng ngày. Còn chưa có màn mẫu: phiếu nguyện vọng cuối đời, mục sự cố trong bản tin Điều dưỡng soạn, form ghi ý kiến và ghi mong muốn, danh sách người thân và quyền (012).
- Chưa có `toast`: màn Checklist ca báo đã lưu bằng vùng `aria-live` và thẻ chuyển sang nhóm Đã xong.
- Chưa có trang trình bày design system (bảng màu, mẫu chữ, ma trận trạng thái).
- Đã có component đính chính dùng chung `correction-sheet` (000 T106), chưa gắn vào màn nào: chờ màn của module sở hữu bản ghi và profile `demo` có người dùng (002).
- Đã có màn Tham số vận hành (000 T081, tab Tham số của mục menu "Tài khoản, tham số") và tab Tài khoản (002 T035: bảng lọc theo loại, trạng thái, tên đăng nhập; khung chi tiết có nút lệnh theo `allowedCommands`, form lệnh có lý do bắt buộc; tạo tài khoản nhân viên gõ mã hồ sơ tới khi 008 có danh sách hồ sơ; mật khẩu tạm hiện một lần trong khung) và Yêu cầu phê duyệt (000 T097, mục menu ngay dưới Tổng quan cho mọi vai trò nhân viên). Hai màn gọi API thật, chưa có dữ liệu khi chạy `npm run dev` không có backend.
- Đã có mục menu "Thông báo" (009 T035, T028; bố cục duyệt 2026-10-23), nhóm đầu ngay dưới "Yêu cầu phê duyệt", cho mọi vai trò nhân viên; tab theo bảng quyền FR-039 của spec 009: Hộp thông báo (mọi vai trò), Lịch sử gửi (Quản lý viện, Trưởng tầng), Lỗi gửi (Quản lý viện), Liên hệ trực tiếp (Hành chính ghi kết quả, Quản lý viện xem), Kênh (Quản lý viện). Bốn tab sau dùng bảng, bấm dòng mở khung chi tiết. Nút "Xác nhận đã nhận" thông báo Khẩn cấp ở khối Khẩn cấp (009 US1); ô lọc "Người cao tuổi" cạnh "Loại sự kiện" lấy hồ sơ trong phạm vi (001), không xem được hồ sơ thì không có ô. Gọi API thật.
- Đã có tab Phân quyền (002 T077, bố cục duyệt 2026-10-21): bên trái 9 vai trò, bên phải bảng quyền của vai trò theo dòng ma trận (chức năng, thao tác, tối đa, hiện hành, trạng thái Theo ma trận / Đã thu hẹp, nút Thu hẹp mở form ngay dưới dòng), dưới là danh sách thu hẹp đang hiệu lực có Khôi phục; không có lệnh thêm quyền. Khung chi tiết tài khoản thêm mục Quyền (vai trò, thu hẹp theo vai trò chỉ xem, thu hẹp theo tài khoản, quyền gán thêm, nút theo `allowedCommands`, quyền hiện hành thu gọn, "Xem tại thời điểm" chỉ xem). Gọi API thật.
- Đã có tab Sức khỏe trong khung chi tiết hồ sơ người cao tuổi (001 T033, bố cục duyệt 2026-10-22; `src/features/residents/`): nút chuyển Thông tin | Sức khỏe, ẩn khi backend bỏ phần sức khỏe theo quyền (`hiddenFields`). Tab gồm hai khối. Hồ sơ sức khỏe ban đầu: nhãn Bản nháp hoặc Đã xác nhận (kèm Đã đính chính), tám ô theo FR-017, nút Lập hồ sơ, Sửa bản nháp, Xác nhận hồ sơ (chỉ Bác sĩ), Đính chính mở khung đính chính dùng chung. Dị ứng, bệnh nền, tiền sử bệnh: bộ lọc Hiệu lực | Đã loại trừ | Tất cả, khối dị ứng `AllergyNote` đứng trước, mục chia theo loại kèm nguồn, người ghi, ngày; "Không kiểm tra tự động" tông `attention`, "Chờ kiểm tra thuốc và chế độ ăn" tông `neutral`; nút Ghi nhận mục mở form ngay dưới (Dị ứng từ danh mục, Dị ứng khác nhập tay, Bệnh nền, Tiền sử bệnh; trùng thì hỏi "Vẫn ghi nhận mục này"); nút Loại trừ mở ô lý do bắt buộc ngay dưới mục. Mục không có nút Sửa, Xóa (DBR-04). Nút hiện theo `allowedCommands`. Gọi API thật.
- Đã có tab Đánh giá trong khung chi tiết hồ sơ người cao tuổi (001 T042, bố cục duyệt 2026-10-23; `src/features/assessments/`): nút chuyển Thông tin | Sức khỏe | Đánh giá; khung rộng `sm:max-w-5xl` khi mở tab này. Từ trên xuống: thẻ "Mức chăm sóc hiện hành" (mức, từ ngày, căn cứ, lưu ý chăm sóc, Lịch sử mức), phiếu đang soạn hai cột (trái phiếu: căn cứ, nhận xét, lưu ý ≤1000 ký tự có bộ đếm, mức chọn bằng ba nút không chọn sẵn, nhu cầu chăm sóc dạng ô chọn, loại hình lưu trú đề xuất chỉ với đầu vào; phải khối tham khảo: hồ sơ sức khỏe ban đầu, mục Hiệu lực, chỉ số đo gần nhất ghi "Chưa có dữ liệu chỉ số đo" tới khi tính năng đo kết nối), lịch sử đánh giá (mở xem nội dung, nút Đính chính dùng `correction-sheet`). Mức trong phiếu tách khỏi mức hiện hành bằng nhãn "chưa áp dụng cho tới khi Bác sĩ xác nhận"; hộp Xác nhận nêu tác động theo loại; thiếu nhận xét hoặc mức báo tại ô. Không có ô điểm, không có mức đề xuất, không nhãn năng lực (mục 2.2).
- Đã có báo đánh giá lại (001 T066, bố cục duyệt 2026-10-23; `src/features/assessments/ReassessmentNotice.jsx`, `src/features/residents/ResidentSearch.jsx`): đầu tab Đánh giá có `Alert` khi người cao tuổi cần hoặc quá hạn đánh giá lại (`attention` kèm hạn, hoặc `urgent` "Quá hạn đánh giá lại từ …"; danh sách căn cứ đã báo kèm thời điểm báo; đang Điều trị tại bệnh viện ghi hạn tính từ lúc Ghi nhận trở về). Không có nút: hệ thống chỉ báo, Bác sĩ hoặc Điều dưỡng tự lập đánh giá lại (Q-307). Bảng hồ sơ thêm cột Đánh giá lại và bộ lọc "Tất cả hồ sơ | Cần đánh giá lại (n)", ẩn khi vai trò không xem được đánh giá (403); bật bộ lọc thì bảng chỉ có những người đó, quá hạn trước. Gọi API thật.

## 15. Ứng dụng di động (Flutter)

Mục này nói cách áp các mục chung vào `mobile/`. Màu, chữ, trạng thái, nhân văn, câu chữ không có bản riêng cho mobile: lấy đúng các mục chung. Mục 15 đã duyệt 2026-10-18, gồm bố cục 15.4 và bộ biểu tượng.

### 15.1 Phạm vi và công nghệ

- Một app Flutter chung cho nhân viên, người thân và người cao tuổi, chức năng theo loại tài khoản (Q-346, Q-365); "app nhân viên", "app người thân", "app người cao tuổi" trong spec là app này. Mã ở `lib/common`, `lib/staff`, `lib/family`, `lib/resident` (research R-02 của 000; tasks ghi `nguoi-cao-tuoi`). Phần người cao tuổi theo 15.6.
- Material 3 (`uses-material-design: true`). Phần nhân viên ghi được khi mất mạng (Q-01, 8.6).
- Cổng người thân trên app có cùng chức năng, tài khoản và quyền với web (Q-305): màn người thân trên app và trên web theo cùng công thức.
- Skill `frontend-design` áp cho phần thẩm mỹ; skill `shadcn` không áp cho mobile.

### 15.2 Token và theme

- Mọi giá trị trong frontmatter `colors`, `typography`, `rounded` khai báo một lần trong theme của app ở `lib/common/` (tên file chốt ở plan khi triển khai). Widget chỉ dùng theme, không viết `Color(0x…)`, cỡ chữ hay bo góc thô.
- `ColorScheme` sáng và tối lấy từ token chung: `primary`/`onPrimary` là xanh đêm/trắng, `surface`/`onSurface` là thẻ/chữ chính, `onSurfaceVariant` là chữ phụ, `outline` là viền điều khiển, `outlineVariant` là đường kẻ, `error` là đỏ (chỉ việc gấp, mục 1 điều 4).
- Tông trạng thái (mục 3.5), nhãn người nói (`source-tag`), ảnh đại diện (mục 3.6), nền trời và các màu khác không có chỗ trong `ColorScheme` khai báo bằng một `ThemeExtension` riêng của dự án.
- Font Be Vietnam Pro 400, 500, 600 đóng gói trong app (assets), không tải lúc chạy, vì app phải dùng được khi mất mạng.
- Thang chữ mục 4 dùng làm logical px trong `TextTheme`. Chữ co giãn theo cỡ chữ của máy; không khóa `textScaler`. Màn phải dùng được khi cỡ chữ máy ở 200%.
- Sáng, tối: mặc định sáng, đổi bằng nút trong app như mục 3.3, lựa chọn lưu trên máy; cách lưu ghi ở plan.

### 15.3 Component

- Dùng widget Material 3 trước khi tự viết; đổi giao diện bằng theme, không đặt màu tại chỗ. Nút: `FilledButton` là nút chính (`default`), `FilledButton.tonal` là `tonal`, `OutlinedButton`, `TextButton` là thao tác phụ (mục 7.1).
- Các công thức ở mục 7.2 (`task-card`, `care-notes`, `choice-note`, `source-tag`, `status-pill`, `resident-avatar`, `resident-photo`, form ghi nhận, `incident-parts`…) làm thành widget dùng chung trong `lib/common/`, giữ cùng thứ tự nội dung và cùng chữ như web.
- Khung chi tiết và khung ghi nhận mở bằng bottom sheet (`showModalBottomSheet`) thay cho `Sheet` bên phải; hộp xác nhận dùng dialog.
- Trạng thái màn (mục 7.3) và cách báo lỗi 409, 412, 422 (mục 7.4) giống web; 422 báo tại ô bằng `errorText` của ô nhập.
- Bộ biểu tượng: lucide, cùng bộ với web (`lucide-react`), cùng tên biểu tượng cho cùng một việc, để hai nền tảng nhìn giống nhau. Gói Flutter cụ thể chốt ở plan khi triển khai; không trộn với Material Icons.

### 15.4 Bố cục

- Điều hướng: thanh dưới (`NavigationBar`), tối đa 5 mục, lấy theo loại tài khoản và quyền backend trả; mục thứ 6 trở đi gom vào mục "Thêm".
- Mỗi màn có thanh tiêu đề nói phạm vi bằng câu bình thường (như page-header, mục 5), một việc chính và một nút chính.
- Nền màn giữ dải trời của mục 3.1, nội dung đặt trên thẻ trắng; chữ trên nền trời theo mục 1 điều 3.
- Danh sách cần tìm, so sánh dùng danh sách dọc có ô tìm và nút lọc thay cho bảng; lưới thẻ (mục 5.1) một cột trên điện thoại, hai cột trên máy tính bảng.
- Lề 16 logical px; khoảng cách giữa các khối và giữa các thẻ như mục 5.

### 15.5 Ngoại tuyến và trợ năng

- Khi mất mạng, đầu màn có một dải báo "Đang ngoại tuyến. Bản ghi sẽ đồng bộ khi có mạng." (tông `neutral`, có chữ, không đỏ).
- Bản ghi chưa đồng bộ mang nhãn "Chưa đồng bộ"; chi tiết bản ghi hiện hai mốc: thời điểm ghi trên thiết bị và thời điểm đồng bộ (8.6).
- Hai thao tác bắt buộc trực tuyến (kích hoạt sự cố khẩn cấp, xác nhận liều thuốc có kiểm soát đặc biệt; 8.6): khi mất mạng, nút vẫn hiện; bấm thì báo rõ thao tác này cần kết nối mạng.
- Vùng bấm tối thiểu 48 logical px. Biểu tượng chỉ có hình có nhãn `Semantics`; biểu tượng trang trí và ảnh đại diện chữ cái loại khỏi cây trợ năng. Trạng thái luôn có chữ (mục 1 điều 6).
- Chuyển động chỉ khi người dùng thao tác, tắt khi máy bật giảm chuyển động (mục 9).

### 15.6 Phần người cao tuổi

Cho tài khoản người cao tuổi (AC-16; nghiep-vu.md 14.6a, spec 012 mục G2, spec 007 US12, spec 002 US8). Người dùng có thể nhìn kém, tay run, ít quen điện thoại, có lúc nhầm lẫn. Các quy tắc chung của mục 15 vẫn áp; dưới đây là phần khác.

**Nguyên tắc**

- Chỉ thông tin của chính người cao tuổi; không có màn hay thẻ nào về sức khỏe, thuốc, chi phí, số dư, hợp đồng (Q-367, BR-M10-11).
- Mỗi màn một việc và tối đa một nút chính. Không có thao tác vuốt, kéo hay giữ lâu.
- Chữ: thang mục 4 nhân 1,25 qua một `TextTheme` riêng của phần này (thân bài 15 → 19 logical px, tiêu đề màn tối thiểu 26); vẫn co giãn theo cỡ chữ của máy, dùng được ở 200%.
- Vùng bấm tối thiểu 56 logical px; nút chính rộng hết màn, cao 56; khoảng cách giữa hai vùng bấm tối thiểu 12.
- Tương phản chữ thân bài tối thiểu 7:1 (cao hơn mức 4,5:1 ở mục 3.4). Nhãn trạng thái luôn có chữ (mục 1 điều 6).
- Điều hướng: không dùng `NavigationBar`. Màn đầu "Hôm nay" là nơi về; mọi màn khác có nút "Quay lại" có chữ ở góc trái thanh tiêu đề.
- Câu ngắn, không thuật ngữ, không viết tắt. Câu không cần đại từ ("Chạm nút để gọi người giúp"); khi cần gọi thì dùng họ tên như mục 2.2, không đoán giới tính để chọn "ông", "bà".
- Mất mạng: màn xem hiện dữ liệu đã tải gần nhất kèm dải báo của 15.5; gửi góp ý cần mạng và nói rõ khi chưa gửi được.

**Màn đầu "Hôm nay"**

- Thanh tiêu đề: "Hôm nay, thứ Hai, 19/10/2026".
- Các thẻ lớn một cột, mỗi thẻ là một nút mở màn tương ứng: "Lịch hôm nay", "Bữa ăn", "Hoạt động", "Người đến thăm", "Đồ của tôi", "Góp ý với viện". Thẻ hiện ngay việc gần nhất ("10:00 Tập dưỡng sinh ở sân", "15:00 [Họ tên người thân] đến thăm").
- Nút SOS cố định ở đáy màn đầu, rộng hết màn, cao 72, nền `error` chữ trắng, biểu tượng lucide `siren` kèm chữ "Gọi người giúp". Đây là chỗ duy nhất màu đỏ là màu nền (mục 1 điều 4: việc gấp).

**Nút SOS** (BR-M05-27, 012 FR-085)

- Chạm một lần (Q-372): màn đổi thành "Đang gọi người giúp sau 3… 2… 1" với số đếm lớn, và nút "Hủy" cao 72 rộng hết màn (tông `neutral`, viền rõ). Hết 3 giây thì rung nhẹ (nếu máy có) và gửi. Chạm nút gọi lần nữa trong lúc đếm thì gửi ngay. Bấm "Hủy" thì không gửi gì, chữ "Đã hủy, chưa gửi." trong 3 giây rồi về màn Hôm nay.
- Nút hiện cả khi chưa đăng nhập hay khi màn xem đang khóa bằng sinh trắc học, trên máy đã gắn (002 FR-071, FR-074).
- Nhãn trợ năng: "Gọi người giúp. Chạm để gửi, có ba giây để hủy." Trình đọc màn hình đọc số đếm; số đếm vẫn chạy khi máy bật giảm chuyển động, chỉ bỏ hiệu ứng chuyển cảnh.
- Lần mở app đầu tiên: một màn giới thiệu "Nút Gọi người giúp báo nhân viên đang trực. Nút không thay chuông gọi ở đầu giường." và nút "Đã hiểu".

**Sau khi gửi** (007 FR-047f)

- Màn trạng thái toàn màn: "Đã gửi. Nhân viên đang được báo." (tông `neutral`); khi có người tiếp nhận: "[Họ tên nhân viên] đang đến." (tông `settled`). Màn tự cập nhật, người dùng không phải bấm gì.
- Dưới đó, không bắt buộc: "Đang ở đâu?" với bốn nút lớn "Ở phòng", "Nhà vệ sinh", "Ngoài sân", "Chỗ khác"; chọn một lần rồi ẩn.
- Không có nút hủy. Nút "Về màn Hôm nay" ở cuối.

**Không gửi được** (007 FR-047d, 012 FR-085)

- Màn toàn màn tông `urgent`: "Chưa gửi được." kèm lý do bằng câu thường ("Đang ghi là tạm vắng", "Máy chưa kết nối mạng", "Máy này chưa được gắn với tài khoản").
- Hai nút gọi điện cao 56, rộng hết màn: "Gọi viện: [số liên hệ của viện]" (số nhận được gần nhất, lưu trên máy) và "Gọi cấp cứu 115". Bấm là mở trình gọi của máy.

**Đăng nhập** (002 FR-069, FR-074)

- Màn quét mã QR toàn màn: "Đặt mã QR nhân viên đưa vào trong khung." Quét được thì vào thẳng "Hôm nay". Nút phụ "Dùng mật khẩu" ở cuối.
- Sau lần đầu, gợi ý một lần "Mở bằng vân tay hoặc khuôn mặt?" với "Có" và "Để sau". Không có mã PIN.

**Các màn xem và gửi** (012 FR-083, FR-058a, FR-058b)

- "Bữa ăn": theo ngày, mỗi bữa một thẻ, tên món; món thay thế ghi "Món thay thế". Không ghi tên chế độ ăn.
- "Hoạt động": mỗi buổi một thẻ, giờ, nơi; nhãn "Đã có tên" (`settled`) hoặc "Chưa có tên" (`neutral`). Không có nút đăng ký (012 FR-084).
- "Người đến thăm": ngày, giờ, họ tên người thăm.
- "Đồ của tôi": ảnh lớn, tên đồ, trạng thái.
- "Góp ý với viện": bước 1 chọn một trong bốn nút, tên theo loại của 14.7, dưới mỗi tên một dòng giải thích ("Góp ý": muốn viện làm tốt hơn; "Hỏi đáp": muốn hỏi một việc; "Khiếu nại": có việc không đúng; "Mong muốn": điều mình muốn); "Mong muốn" hỏi thêm "Về người đến thăm, người đón tôi" hay "Việc khác" (chủ đề, 012 FR-058b); bước 2 ô viết hoặc nói (nhập giọng nói của máy); nút "Gửi tới viện". Danh sách góp ý đã gửi hiện trạng thái và trả lời; trả lời về sức khỏe chỉ phần chung (012 FR-058a).

**Thông báo**: chỉ nhắc mức Nhẹ (lượt thăm, hoạt động trong ngày, góp ý đã được trả lời; 009 FR-022a); chữ thông báo nói rõ việc ("15:00 hôm nay [Họ tên người thân] đến thăm"), bấm vào mở đúng màn.

### 15.7 Hiện trạng

- (2026-10-24) Đã có theme `lib/common/theme/app_theme.dart` theo token và phần người cao tuổi: đăng nhập QR, mật khẩu dự phòng, đổi mật khẩu lần đầu, hỏi mở bằng sinh trắc học, màn khóa, "Hôm nay" mới có tiêu đề (thẻ và nút SOS thêm khi 012, 007 có phần app). Font mới có 400, 700. Chưa nghiệm thu trên máy thật.
- Đã duyệt 2026-10-18: bố cục 15.4, bộ biểu tượng lucide. Cách lưu chế độ sáng tối và gói lucide cho Flutter chốt ở plan khi triển khai.

## 16. Lịch sử

| Phiên bản | Ngày | Nội dung |
| --- | --- | --- |
| 5.19.1 | 2026-10-23 | Chủ dự án cho triển khai bố cục tab Nguyện vọng trong khung chi tiết hồ sơ người cao tuổi (001 T070, T080; mục 14) theo đề xuất: hai khối xếp dọc "Nguyện vọng cuối đời" và "Nguyện vọng khi điều trị", mỗi khối có thẻ phiếu hiện hành, nút theo lệnh được phép, form mở dưới thẻ, lịch sử phiên bản; nhận định "tự quyết được" chỉ nằm trong thẻ phiếu cuối đời kèm dòng "Người quyết định"; Hành chính chỉ thấy "Đã có phiếu", Kế toán không thấy khối khi điều trị. Không thêm màu, không thêm quy định; "Hiệu lực" dùng tông `settled`, "Được thay thế", "Hết hiệu lực" tông `neutral` mặc định. Chưa nghiệm thu trên trình duyệt. |
| 5.19.0 | 2026-10-23 | Chủ dự án duyệt bố cục thông báo (009 T035, T028): mục menu "Thông báo" với năm tab theo vai trò (mục 14), công thức `notification-inbox` (mục 7.2). Tông trạng thái vào mục 3.5: mức thông báo như mức cảnh báo; thông báo Chờ hết giờ yên tĩnh, Đã gửi, Không gửi `neutral`, Đã xem, Đã xác nhận `settled`, Gửi thất bại, Không liên lạc được `attention`; liên hệ trực tiếp Chờ liên hệ `attention`, Đã liên hệ `settled`, Không liên hệ được `neutral`; kênh Đang bật `settled`, Đã tắt `neutral`. Không thêm màu |
| 5.18.4 | 2026-10-23 | Chủ dự án duyệt bố cục báo đánh giá lại (001 T066; mục 14): dải `Alert` đầu tab Đánh giá, cột Đánh giá lại và bộ lọc "Cần đánh giá lại (n)" ở bảng hồ sơ. Không thêm màu; mục 3.5 thêm "Cần đánh giá lại" vào tông `attention` và "Quá hạn đánh giá lại" vào tông `urgent`. |
| 5.18.3 | 2026-10-23 | Chủ dự án duyệt bố cục tab Đồng ý trong khung chi tiết hồ sơ người cao tuổi (001 T059; mục 14): thẻ bản đồng ý hiện hành kèm nút theo lệnh được phép, form ghi nhận hoặc rút lại mở dưới thẻ, lịch sử bản đã rút lại. Không thêm màu, không thêm quy định; "Hiệu lực" dùng tông `settled`, "Đã rút lại" tông `neutral` mặc định. Tải bản ký đi qua backend, không có đường dẫn công khai. |
| 5.18.2 | 2026-10-23 | Chủ dự án duyệt bố cục tab Đánh giá trong khung chi tiết hồ sơ người cao tuổi (001 T042; mục 14): hai cột phiếu và thông tin tham khảo, khung rộng khi mở tab. Không thêm màu, không thêm quy định; nhãn Bản nháp, Đã hủy dùng tông `neutral`, Đã xác nhận `settled` như hồ sơ sức khỏe ban đầu. |
| 5.18.1 | 2026-10-22 | Chủ dự án duyệt bố cục tab Sức khỏe trong khung chi tiết hồ sơ người cao tuổi (001 T033; mục 14). Không thêm màu, không thêm quy định; "Bản nháp", "Đã loại trừ" dùng tông `neutral` mặc định của `StatusBadge` |
| 5.18.0 | 2026-10-21 | Chủ dự án duyệt bố cục phân quyền (002 T077): tab Phân quyền trong "Tài khoản, tham số", mục Quyền trong khung chi tiết tài khoản (mục 14). Trạng thái quyền vào mục 3.5: Đã thu hẹp `attention`, Theo ma trận `neutral`. Không thêm màu |
| 5.17.1 | 2026-10-20 | Chủ dự án chọn một màn đăng nhập chung (002 T045): `/login` cho nhân viên và người thân, thêm `/change-password`; khối người dùng cuối menu có họ tên, vai trò và nút Đăng xuất khi có phiên thật. Không thêm màu, không đổi công thức |
| 5.17.0 | 2026-10-20 | Chủ dự án duyệt bố cục tab Tài khoản (002 T035) và màu trạng thái tài khoản ở mục 3.5: Hoạt động `settled`, Khóa tạm `attention`, Không hoạt động `neutral`, Đã khóa tông mới `neutral-outline` (viền `--input`, nền trong) để phân biệt với Không hoạt động. Không thêm màu |
| 5.16.0 | 2026-10-19 | Chủ dự án duyệt bố cục màn Tham số (000 T081: bảng, khung chi tiết hai tab Đổi giá trị, Lịch sử; tham số dạng bảng sửa ngay trong bảng) và Yêu cầu phê duyệt (000 T097: ba hộp Chờ tôi duyệt, Của tôi, Trong phạm vi; nội dung hiện dạng "trường: giá trị"); mục menu "Yêu cầu phê duyệt" ngay dưới Tổng quan; trạng thái yêu cầu phê duyệt vào mục 3.5. Không thêm màu |
| 5.15.0 | 2026-10-19 | Chủ dự án duyệt bố cục đính chính dùng chung (000 T106): thêm công thức `correction-sheet` ở mục 7.2; trạng thái bản ghi Đã đính chính, Đã hủy ghi nhận vào tông `neutral` ở mục 3.5. Không thêm màu |
| 5.14.0 | 2026-10-19 | Chủ dự án duyệt mục 15.6 Phần người cao tuổi (gồm nút SOS theo Q-372) |
| 5.13.0 | 2026-10-19 | Thêm mục 15.6 Phần người cao tuổi theo Q-365 → Q-371 (chờ duyệt): nguyên tắc chữ, vùng bấm, tương phản, điều hướng; màn Hôm nay, nút SOS chạm một lần, đếm ngược 3 giây có nút Hủy (Q-372), màn sau khi gửi, màn không gửi được, đăng nhập bằng mã QR, các màn xem và góp ý. Hiện trạng thành 15.7. Không thêm màu |
| 5.12.0 | 2026-10-18 | Màn mẫu Bàn giao ca (spec 008) và Cảnh báo (spec 007): thêm `handover-draft`, `handover-confirm`, `alert-card`; tông mức cảnh báo, trạng thái cảnh báo, bàn giao vào mục 3.5. Nút "Mở cảnh báo" ở Tổng quan mở màn Cảnh báo. Không thêm màu |
| 5.11.0 | 2026-10-18 | Theo góp ý chủ dự án (màn Phát thuốc thừa khoảng trống): màn nhân viên rộng tối đa 1600px thay cho 1280px; lưới thẻ dùng `auto-fit` để thẻ giãn lấp đầy hàng, mỗi thẻ rộng tối đa 480px (thẻ việc ở Tổng quan 360px) để thẻ lẻ không kéo dài cả màn |
| 5.10.0 | 2026-10-18 | Màn mẫu Phát thuốc (spec 006): thêm `dose-card`, `dose-sheet`, `prn-card`; trạng thái liều vào mục 3.5 (Trễ, Bỏ lỡ đỏ; Đến giờ hổ phách; Đã dùng xanh lá); dị ứng hiện trên mọi thẻ thuốc; thời điểm dùng điền sẵn giờ hiện tại. Không thêm màu |
| 5.9.0 | 2026-10-18 | Theo yêu cầu chủ dự án: đường dẫn và neo bằng tiếng Anh (`/dang-nhap` → `/login`, `/cong-nguoi-than` → `/family`, các neo của Trang chủ); thêm quy tắc ở mục 8 |
| 5.8.0 | 2026-10-18 | Theo yêu cầu chủ dự án: trang giới thiệu chuyển vào cổng người thân làm Trang chủ sau đăng nhập, bỏ trang công khai `/nguoi-than`; sắp lại nội dung: viện trước (01 Không gian sống, 02 Đội ngũ và "Gia đình liên hệ với ai", 03 Đời sống, 04 Quy trình chăm sóc và an toàn, 05 Viện và gia đình), rồi Elder_Care; bỏ "Một tuần mẫu". Cổng: bỏ nút "Xem như" R, M, N; đầu trang chung kiểu trang chủ; bố cục máy tính rộng 960px, Hôm nay hai cột |
| 5.7.0 | 2026-10-18 | Thêm mục 2.3 Cổng người thân: khung riêng cho điện thoại, thanh điều hướng 5 mục, hiện theo quyền, nút "Xem như" của bản mẫu; màn đăng nhập thay trang giữ chỗ. Ảnh người cao tuổi với người thân theo spec 012 FR-046. Bổ sung trạng thái lượt thăm, phản hồi, đồ gửi, kỳ chi phí, quyết định của người thân vào mục 3.5 |
| 5.6.0 | 2026-10-18 | Theo góp ý chủ dự án (chữ menu bị chìm): chữ menu `#0F2235` (màu chữ chính), mục menu 14/500 (`type-label`), nhãn nhóm `#14304F`; mục đang chọn vẫn là viên thuốc xanh đêm. Đã kiểm độ tương phản |
| 5.5.0 | 2026-10-18 | Rà soát, chuẩn hóa chữ màn Tổng quan và menu: thêm `type-label` 14/500 và `type-metric` 24/600; bảng vai trò chữ ở mục 4. Sửa lỗi `CardTitle` đặt sẵn `text-base` đè `type-title` (tiêu đề khu vực hiện 16px thay vì 18px); số liệu từ 26px (bằng tiêu đề trang) xuống 24px; tên công việc trong thẻ việc từ 14/500 lên 16/600; tên người cao tuổi 14/500; nút nhỏ từ 12,8px về 12px; trạng thái đã xong chỉ đổi màu; nhãn số liệu ngắt dòng cân đối, số canh đáy. Không đổi font, màu, bố cục |
| 5.4.0 | 2026-10-18 | Theo góp ý chủ dự án: chữ menu đổi từ gần đen `#08131F` sang xanh đêm đậm `#14304F` cùng sắc nền trời; thêm `--sidebar-muted-foreground` `#1B3A5C` (tối `#B8C6DA`) cho nhãn nhóm và chữ phụ; viền focus trong menu `#14304F`. Đã kiểm độ tương phản |
| 5.3.0 | 2026-10-18 | Theo góp ý chủ dự án (nội dung đậm hơn menu): nền trang sáng lên, dải trời gốc pha 45% trắng (`#B4C8DD` → `#C7DCEB` → `#E2EDF5`); menu pha 15% trắng (`#8BAACA` → `#A9C9E0` → `#D3E4EF`), đậm hơn nội dung một bậc. Trang giới thiệu (`--landing-*`) và chế độ tối không đổi. Đã kiểm độ tương phản |
| 5.2.0 | 2026-10-18 | Theo góp ý chủ dự án: Tổng quan đổi danh sách việc thành lưới việc theo khung giờ; sơ đồ giường chia theo phòng, màu đặc mới `--bed-*` (chỉ dùng cho ô giường); menu trái sáng hơn (dải nền trang pha 25% trắng), mục đang chọn thành viên thuốc xanh đêm chữ trắng, rê chuột `#FFFFFF73`. Đã kiểm độ tương phản (mục 3.4) |
| 5.1.0 | 2026-10-18 | Chủ dự án duyệt mục 15: bố cục mobile 15.4 thành quy định; bộ biểu tượng mobile là lucide, cùng bộ và cùng tên với web |
| 5.0.0 | 2026-10-18 | Thành file chung cho web và mobile theo yêu cầu chủ dự án: chuyển từ `frontend/DESIGN.md` lên gốc kho; thêm bảng phạm vi từng mục, mục 15 Ứng dụng di động (Flutter: phạm vi, token và theme, component, bố cục đề xuất chờ duyệt, ngoại tuyến và trợ năng, hiện trạng); lịch sử chuyển sang mục 16; quy trình có lệnh kiểm của mobile. Không đổi giá trị màu, chữ nào |
| 4.5.0 | 2026-10-18 | Màn mẫu theo mục 2.2: lịch sử ý kiến, Hoàn tất tiếp nhận có ý kiến vào ở, chi tiết sự cố hai phần, Người thân và mong muốn về khách thăm, người đón; thêm `opinion-history`, `admission-command`, `incident-parts`, `visit-wishes`, biến thể `attention` của `Alert`, `Checkbox`; "Chờ xác nhận" sang tông `attention`. Sửa câu về thông báo sự cố ở mục 2.2: hệ thống tự gửi, nhân viên không soạn. Không thêm màu |
| 4.4.0 | 2026-10-18 | Thêm mục 2.2 Nhân văn trong màn nghiệp vụ theo Q-347 → Q-364: nhãn người nói `source-tag` dùng chung cho hồ sơ và trang giới thiệu, không nhãn năng lực, `choice-note` cho thỏa thuận lựa chọn có rủi ro, loại lý do "Người cao tuổi từ chối", phần chung và phần sức khỏe, mong muốn về khách thăm và người đón, lời văn. Sửa `task-card`, `care-notes`, `about-person`, form ghi nhận. Không thêm màu |
| 4.3.0 | 2026-10-18 | Viết lại mục 2.1 theo bản yêu cầu landing page của chủ dự án: 9 section, nền trời, trắng ngà, be ấm, xanh đêm (token `--landing-*`), ô ảnh giữ chỗ, menu thu gọn, trang `/dang-nhap` giữ chỗ |
| 4.2.0 | 2026-10-18 | Thêm mục 2.1 Trang giới thiệu cổng người thân (bản tin minh họa có lời giải thích, nhãn người nói, không ảnh) và thang chữ `display`, `section`, `lead`, `reading` chỉ dùng cho trang này |
| 4.1.0 | 2026-10-18 | Theo vòng review của chủ dự án (đợt 1, 2): thêm định hướng Clinical & Calm, Human-centered Care, quy tắc hình ảnh và không giả lập (mục 2); tách dị ứng khỏi lưu ý chăm sóc (`care-notes`, lưu ý chăm sóc sang `neutral`); thẻ việc và danh sách việc có người phụ trách; thêm `resident-photo`, `about-person`, `handover-card`; thêm mục 7.3 Trạng thái màn; tiêu đề nhóm thu gọn 44px; bỏ làm mờ nền hộp thoại |
| 4.0.0 | 2026-10-16 | Viết lại thành bộ quy tắc duy nhất cho frontend theo yêu cầu chủ dự án; chuyển các luật giao diện từ `CLAUDE.md` sang đây (hướng thiết kế, ưu tiên nội dung, skill). Gom quy tắc cốt lõi, tách mục Menu trái, bảng độ tương phản, bảng biến thể nút. Không đổi giá trị nào so với 3.6.0 |
| 3.6.0 | 2026-10-16 | Nút và chữ sang tông xanh đêm: chữ `#0F2235`, chữ phụ `#50627A`, nút chính `#24496F`; menu chữ `#08131F`, mục chọn viên thuốc trắng chữ xanh đêm, logo xanh đêm |
| 3.5.0 | 2026-10-16 | Menu dùng bản đậm của dải nền trang (`#5E85AE` → `#759DBF` → `#94B2CA`) để tách khỏi nội dung mà vẫn hài hòa |
| 3.4.0 | 2026-10-16 | Toàn bộ giao diện dùng dải trời xanh `#779BC1` → `#9ABFDA` → `#CBDFEC` |
| 3.0.0 – 3.3.0 | 2026-10-16 | Đổi sang bảng màu đo từ letters.app; thử menu trong suốt, màu trơn `#9ABFDA` rồi `#779BC1` |
| 1.0.0 – 2.5.0 | 2026-10-16 | Các hướng trước (Lá trà, Ngọc bích sáng, bạc hà, Trời xanh bản đầu); đổi font sang Be Vietnam Pro, bỏ thanh trên, thêm page-header, nút tonal, chế độ tối theo nút bấm |
