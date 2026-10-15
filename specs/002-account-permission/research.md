# Research: Tài khoản và phân quyền (002)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Các quyết định nền R-01 → R-05 của feature 000 (phiên bản, cấu trúc kho mã, quy ước API, thời gian, bảo đảm nhóm dữ liệu) được dùng lại nguyên vẹn, xem [../000-shared-foundation/research.md](../000-shared-foundation/research.md). Tài liệu này chỉ ghi phần riêng của 002, đánh số A-01 → A-14. Không quyết định nào đổi nghiệp vụ (constitution IX).

---

## A-01. Vị trí trong backend

**Quyết định**: Feature 002 nằm ở module M15, gói `vn.eldercare.m15.taikhoan`, chia bốn gói con:

| Gói con | Nội dung |
| --- | --- |
| `taikhoan.xacthuc` | Đăng nhập, phiên, mật khẩu, thiết bị (mục A, B) |
| `taikhoan.quyen` | Vai trò, ma trận quyền, thu hẹp, quyền theo nhiệm vụ, giới hạn trường (mục C) |
| `taikhoan.phamvi` | Phạm vi dữ liệu, ngoại lệ phạm vi (mục D, FR-044a, FR-035a) |
| `taikhoan.phaply` | Danh mục nghiệp vụ chuyên môn, kiểm tra giấy phép, cảnh báo hết hạn (mục E) |

Mỗi gói có `api`, `internal`, `web`, và `lich` khi có job. Gói `taikhoan.api` công khai các giao diện mà 000 đã khai báo trước (`NguoiDungHienTai`, `QuyenApi`, `PhamViApi`, `QuyenXemTruongApi`, `NguoiDuyetApi`), giữ đúng chữ ký ở [../000-shared-foundation/contracts/module-interfaces.md](../000-shared-foundation/contracts/module-interfaces.md) mục 2.

**Lý do**: Phụ lục 26.2 đặt UC-68, UC-70 ở feature 002 thuộc M15; 000 đã dành chỗ `m15/taikhoan` (plan 000, Project Structure).

**Phương án đã cân nhắc**: Tách xác thực thành module riêng bị loại vì sinh module ngoài M01–M15 (constitution X).

---

## A-02. Phiên đăng nhập và token

**Quyết định**:
- Token phiên là chuỗi ngẫu nhiên 256 bit (opaque), máy chủ chỉ lưu giá trị băm SHA-256 trong bảng `phien_dang_nhap`. Không dùng JWT.
- Mỗi yêu cầu tra phiên theo khóa chính (giá trị băm), đọc kèm trạng thái tài khoản và `phien_ban_quyen`. Không đệm kết quả tra phiên giữa các yêu cầu.
- Kênh gửi token:
  - web: cookie `HttpOnly`, `Secure`, `SameSite=Strict`, cộng header `X-Yeu-Cau-Tu: web` bắt buộc với mọi lệnh ghi, để chặn CSRF;
  - hai app Flutter: header `Authorization: Bearer <token>`, token lưu bằng `flutter_secure_storage`.
- Tự đăng xuất (FR-013a, BR-M15-21): mỗi lần tra phiên so `hoat_dong_cuoi_luc` với CFG-M15-10 của kênh của phiên; quá hạn thì đặt `ket_thuc_luc`, `ly_do_ket_thuc = HET_THOI_GIAN` và trả 401 `PHIEN_HET_HAN`; còn hạn thì cập nhật `hoat_dong_cuoi_luc`. Không cần job. Trạng thái tài khoản không đổi.
- Kết thúc phiên ngay (FR-013): lệnh làm tài khoản rời Hoạt động (khóa, khóa tạm, Không hoạt động, Nghỉ việc) đặt `ket_thuc_luc` cho mọi phiên của tài khoản **trong cùng transaction**. Yêu cầu tới sau commit đọc thấy phiên đã kết thúc và bị từ chối 401.
- Một thiết bị một phiên (FR-014a, BR-M15-15):
  - client gửi `X-Thiet-Bi` là mã thiết bị (UUID sinh lần đầu cài app, hoặc lần đầu mở web trên trình duyệt);
  - đăng nhập trên một thiết bị kết thúc mọi phiên đang mở của thiết bị đó, của bất kỳ tài khoản nào, trong cùng transaction;
  - một tài khoản vẫn có thể có nhiều phiên trên nhiều thiết bị khác nhau.

**Lý do**: FR-013 và FR-027 đòi thu hồi phiên và áp quyền mới ngay ở yêu cầu kế tiếp. JWT tự chứa không thu hồi được nếu không thêm danh sách thu hồi, tức là vẫn phải tra máy chủ. Với một instance và 60 người dùng đồng thời (NFR-01), một lần tra khóa chính mỗi yêu cầu không đáng kể so với ngân sách 2 giây (NFR-02).

**Phương án đã cân nhắc**:
- JWT ngắn hạn kèm refresh token (dự kiến ở 000 R-12): vẫn có khoảng trễ bằng thời gian sống của access token, trái FR-013. 000 R-12 ghi là "dự kiến"; plan này chốt lại, không ảnh hưởng 000.
- Spring Session lưu trong MySQL: được, nhưng khó gắn mã thiết bị và kết thúc phiên trong cùng transaction nghiệp vụ.

---

## A-03. Mật khẩu và đăng nhập

**Quyết định**:
- Băm mật khẩu bằng Argon2id (`Argon2PasswordEncoder` của Spring Security, tham số theo khuyến nghị OWASP hiện hành).
- Mật khẩu tạm khi tạo tài khoản hoặc cấp lại: 12 ký tự ngẫu nhiên, chỉ trả về một lần trong phản hồi của lệnh, để người thực hiện giao trực tiếp cho người đã xác minh (FR-052). Tài khoản mang cờ `buoc_doi_mat_khau`. Phiên của tài khoản mang cờ này chỉ gọi được `doi-mat-khau` và `dang-xuat` (FR-006).
- Độ mạnh mật khẩu (FR-006a, BR-M15-21): kiểm theo CFG-M15-09 (dạng `BANG` `{doDaiToiThieu, batBuocChu, batBuocSo}`, mặc định 8, có, có), thêm điều kiện kỹ thuật "khác mật khẩu tạm"; không đạt trả 422 `MAT_KHAU_KHONG_DAT` kèm yêu cầu. Mật khẩu tạm sinh ra cũng đạt mức này.
- Khởi tạo Quản lý viện đầu tiên (FR-004b, BR-M15-22): lệnh dòng lệnh `khoi-tao-quan-ly-vien` của ứng dụng backend, chỉ chạy khi chưa có dòng `gan_vai_tro` còn hiệu lực với AC-01 (kiểm dưới khóa bảng). Lệnh gọi `HoSoNhanVienApi` của 008 tạo hồ sơ tối thiểu, tạo tài khoản AC-01 buộc đổi mật khẩu, in mật khẩu tạm ra màn hình một lần, ghi nhật ký với `nguoi_thuc_hien_loai = KHOI_TAO`. Không có endpoint REST cho việc này.
- Đếm sai (FR-008, BR-M15-05):
  - cột `so_lan_sai_lien_tiep` trên `tai_khoan`, tăng bằng `UPDATE … SET so_lan_sai = so_lan_sai + 1` để không mất lần đếm khi đăng nhập đồng thời;
  - đủ ngưỡng của CFG-M15-01 thì đặt `khoa_tam_den = bây giờ + thời gian khóa`, chuyển Khóa tạm, kết thúc phiên;
  - hết thời gian khóa thì tài khoản trở về Hoạt động **khi có yêu cầu kế tiếp** (đăng nhập hoặc tra cứu trạng thái): chuyển trạng thái, ghi nhật ký người thực hiện là Bộ lập lịch, căn cứ CFG-M15-01. Không cần job, vì trạng thái chỉ có ý nghĩa khi có người đăng nhập. Màn quản trị hiển thị Hoạt động khi `khoa_tam_den` đã qua.
  - CFG-M15-01 là tham số dạng `BANG` hai thành phần `{soLanSai, thoiGianKhoa}` (000 R-07, FR-008).
- Thông báo từ chối (FR-014): sai tên đăng nhập và sai mật khẩu trả cùng một mã `DANG_NHAP_KHONG_THANH`, cùng thời gian xử lý (luôn chạy băm với một giá trị giả khi không tìm thấy tên). Chỉ khi mật khẩu đúng mà tài khoản không ở Hoạt động mới trả mã riêng (`TAI_KHOAN_KHOA_TAM` kèm thời điểm mở, `TAI_KHOAN_CAN_LIEN_HE_VIEN`).
- Nhật ký đăng nhập (FR-015, 19.4): mọi lần đăng nhập thành công, thất bại ghi `nhat_ky` với `hanh_dong` `DANG_NHAP` / `DANG_NHAP_THAT_BAI`, `truoc`/`sau` rỗng (000 FR-043a). Lần thất bại với tên không tồn tại ghi `doi_tuong_id` rỗng, chi tiết là tên đăng nhập đã nhập.

**Lý do**: Argon2id là lựa chọn mặc định hiện nay cho lưu mật khẩu. Đếm sai bằng câu lệnh nguyên tử tránh vượt ngưỡng do đăng nhập đồng thời.

**Phương án đã cân nhắc**: Job mở khóa tạm định kỳ bị loại vì không cần thiết; tính trễ khi có yêu cầu vẫn đúng FR-008 và giữ số job tối thiểu (FR-010a của 000).

---

## A-04. Ma trận quyền tối đa (FR-016 → FR-020)

**Quyết định**:
- **Khóa quyền** `KhoaQuyen = (ChucNang, LoaiThaoTac)`:
  - `ChucNang`: enum Java, mỗi giá trị là một dòng của Phụ lục 27 (ví dụ `HO_SO_NGUOI_CAO_TUOI`, `MON_AN_THUC_DON`, `SU_CO_KHAN_CAP`), kèm danh sách UC;
  - `LoaiThaoTac`: `XEM`, `TAO`, `SUA`, `XAC_NHAN`, `DUYET`, `CHOT`, `CHUYEN_MON` (19.2, bảng loại thao tác của spec).
- **Ma trận**: bảng `ma_tran_quyen` (nhóm 1, chỉ đọc qua API) nạp từ migration. Mỗi ô Phụ lục 27 (vai trò × chức năng) thành 0..n dòng `(vai_tro, chuc_nang, loai_thao_tac, pham_vi, ma_chu_thich[])`, quy đổi ký hiệu theo bảng loại thao tác của spec:
  - X → `XEM` toàn viện; P → `XEM` theo phân công;
  - T → `TAO`, `SUA` và `XAC_NHAN` khi dòng có ghi nhận; dòng chuyên môn (đơn thuốc, phát thuốc…) thêm `CHUYEN_MON`;
  - D → `DUYET`; dòng "Chốt kỳ, xuất kế toán" thêm `CHOT`; C → `TAO`, `SUA` với nhóm 1;
  - phạm vi của T, D theo vai trò (FR-034): TT, ĐD, CS theo phân công; QL, BS, HC, KT, Bếp toàn viện.
- **19.3 là căn cứ khi khác Phụ lục 27** (FR-018): các điều chỉnh của 19.3 được nạp cùng migration như dòng có `nguon = '19.3'`. Ví dụ quyền Quản lý viện duyệt, lập thay yêu cầu đổi ca, nghỉ đột xuất của ca toàn viện (Q-177), dù ô Phụ lục 27 là X. Danh sách điều chỉnh nằm ở [data-model.md](data-model.md) mục 3.
- **Chú thích có điều kiện** (¹ bản đồng ý, ² người đại diện, ¹⁰ ngoài phạm vi…): ma trận chỉ ghi mã chú thích. Mỗi chú thích được kiểm bởi một `DieuKienChuThich` do module sở hữu điều kiện cài đặt (ví dụ ¹ do 001 cài qua bản đồng ý, ² do 012 cài qua người đại diện). `QuyenApi` gọi các điều kiện này như một phần của lớp vai trò. Chú thích chỉ mô tả, không thêm điều kiện, thì không cần cài.
- **Chú thích ⁴², ⁴³, ⁴⁴ và dòng "Tiếp nhận thuốc cung ứng"** *(bổ sung 2026-10-11, Q-337 → Q-339)*: ba chú thích này là loại chỉ mô tả, không có `DieuKienChuThich`; ô được quy đổi theo ký hiệu đứng trước (T, D) như mọi ô khác. Giới hạn được kiểm ở module sở hữu lệnh (U2): ⁴² (Hành chính chỉ duyệt thay đổi lưu trú nhỏ) do 000 `gioiHanVaiTroDuyet` và handler `M02.THAY_DOI_LUU_TRU` của 004; ⁴³ (giao người xử lý sự cố) do 007 bằng thao tác riêng `GIAO`, không dùng khóa `(SU_CO_KHAN_CAP, TAO)` vì khóa này được miễn lớp phạm vi (FR-044a); ⁴⁴ (Điều dưỡng với yêu cầu mua sắm loại "thuốc" và lần tiếp nhận) do 019 và 006. Phạm vi T của Điều dưỡng vẫn theo phân công như quy tắc trên. Ô T³⁷ ⁴⁴ của Điều dưỡng ở dòng "Yêu cầu mua sắm" quy đổi thành `TAO` như mọi ô T³⁷; điều kiện "đang là Phụ trách bộ phận" (³⁷) và ngoại lệ loại "thuốc" (⁴⁴) cùng do 019 kiểm (019 research W-05), không cài `DieuKienChuThich` cho ³⁷, để hai điều kiện không chặn nhau ở lớp vai trò (bổ sung 2026-10-11 sau audit). Dòng mới của Phụ lục 27 thêm một giá trị `ChucNang`; ma trận và `MaTranQuyenKhopNguonTest` đọc từ tài liệu nguồn nên không cần đổi cách nạp. Tên chức năng, thao tác ở research của từng feature (ví dụ `SU_CO`/`GIAO` ở 007, `MUA_SAM`/`LAP` ở 019) khác tên ở đây (`SU_CO_KHAN_CAP`, `TAO`…); đợt này giữ nguyên, đối chiếu khi chạy /speckit-analyze.
- **Lệnh của module**: mỗi lệnh khai báo khóa quyền bằng chú thích `@CanQuyen(chucNang, thaoTac)` trên phương thức service. Một interceptor gọi `QuyenApi.coQuyen` trước khi lệnh chạy. ArchUnit bắt mọi `@LenhNghiepVu` phải có `@CanQuyen` (FR-041).
- **Kiểm thử khớp ma trận (SC-001)**: một test tham số hóa đọc bảng Phụ lục 27 trong `docs/business/nghiep-vu.md` (phân tích bảng Markdown) cộng danh sách điều chỉnh 19.3, rồi so với `ma_tran_quyen` đã nạp. Lệch thì test lỗi. Nhờ vậy khi tài liệu nguồn đổi mà migration chưa đổi, bộ test báo ngay.

**Lý do**: Constitution VI, FR-018. Một nguồn quy đổi duy nhất cho 9 vai trò × khoảng 70 chức năng, kiểm được tự động.

**Phương án đã cân nhắc**:
- Viết quyền thẳng trong mã bằng `@PreAuthorize("hasRole…")`: không thu hẹp được lúc chạy (FR-025), không tra được quá khứ (FR-026a).
- Cho Quản lý viện sửa ma trận qua API: trái FR-025 (chỉ được thu hẹp).

---

## A-05. Thu hẹp, quyền gán thêm và thứ tự tính (FR-024 → FR-027)

**Quyết định**:
- Ba bảng có hiệu lực theo thời điểm, mỗi dòng có `hieu_luc_tu` và `hieu_luc_den` (`DATETIME(6)`, `NULL` là còn hiệu lực):
  - `thu_hep_quyen` với `doi_tuong = VAI_TRO | TAI_KHOAN`: bỏ một loại thao tác, hoặc hạ phạm vi từ toàn viện xuống theo phân công. Với chức năng `DASHBOARD_BAO_CAO`, thêm cột `nhom_chi_tieu` và `kenh` (dashboard / báo cáo) để thu hẹp chi tiết hơn (feature 016 FR-010a);
  - `quyen_gan_them`: chỉ cho khóa `(KE_HOACH_CHAM_SOC, DUYET)` gán cho tài khoản có vai trò Điều dưỡng (FR-024, Q-07);
  - `gan_vai_tro`: tài khoản, vai trò, người gán, lý do.
- Thay đổi = đóng dòng cũ (`hieu_luc_den = bây giờ`) và thêm dòng mới trong một transaction. Không xóa dòng. Nhờ đó tra cứu quyền tại thời điểm quá khứ (FR-026a, BR-M15-16) chỉ cần lọc `hieu_luc_tu <= t < coalesce(hieu_luc_den, ∞)`. Đây là dữ liệu nhóm 1 có lịch sử, đúng mẫu 1.2 của data-model 000; cột `hieu_luc_den` là cột duy nhất được `UPDATE`, một lần.
- **Kiểm tra không vượt ma trận (FR-025)**: lệnh thu hẹp kiểm khóa và phạm vi mới phải nằm trong ô ma trận của vai trò (thu hẹp theo vai trò) hoặc trong hợp quyền các vai trò của tài khoản (thu hẹp theo tài khoản). Vi phạm trả 409 `VUOT_QUYEN_TOI_DA`. Không có API thêm quyền ngoài `quyen_gan_them`.
- **Thứ tự tính quyền vai trò** (FR-025, BR-M15-18, CHK017):
  1. với từng vai trò của tài khoản: quyền ma trận trừ thu hẹp theo vai trò;
  2. hợp các vai trò; phạm vi của cùng một khóa là hợp phạm vi (toàn viện thắng);
  3. trừ thu hẹp theo tài khoản;
  4. cộng `quyen_gan_them`.
- **Luôn còn Quản lý viện (FR-012)**: lệnh khóa tài khoản, gỡ vai trò AC-01, thu hẹp khóa `(TAI_KHOAN_PHAN_QUYEN, *)` đếm số tài khoản Hoạt động còn quyền đó **sau** thay đổi, dưới khóa `SELECT … FOR UPDATE` trên các dòng `gan_vai_tro` của AC-01. Bằng 0 thì từ chối 409 `KHONG_CON_QUAN_LY_VIEN`. Khóa tự động do Nghỉ việc không bị chặn (spec chỉ chặn "thao tác người dùng").
- **Chống ghi đè (FR-026, BR-M15-17)**: `tai_khoan.phien_ban_quyen` cho thay đổi theo tài khoản, `vai_tro.phien_ban_quyen` cho thay đổi theo vai trò; lệnh bắt buộc `If-Match`; sai thì 412 kèm tập quyền hiện hành.
- **Hiệu lực ngay (FR-027)**: quyền được tính lại ở mỗi yêu cầu. Có bộ đệm trong tiến trình cho "quyền vai trò đã hợp" của một tài khoản, khóa đệm gồm `(tai_khoan_id, tai_khoan.phien_ban_quyen, tổng phien_ban_quyen của các vai trò)`; các số phiên bản đọc cùng lần tra phiên (A-02), nên đổi quyền là khóa đệm đổi ngay ở yêu cầu kế tiếp.

**Lý do**: Bảng có hiệu lực theo thời điểm vừa phục vụ tính hiện hành vừa phục vụ tra quá khứ, không cần dựng lại từ nhật ký.

**Phương án đã cân nhắc**: Dựng quyền quá khứ từ `nhat_ky`: được nhưng chậm và phụ thuộc định dạng JSON của nhật ký.

---

## A-06. Quyền theo nhiệm vụ (FR-019 → FR-019f)

**Quyết định**:
- Nhiệm vụ không phải vai trò; 002 không lưu nhiệm vụ. 002 hỏi module sở hữu tại thời điểm kiểm tra:
  - Người phụ trách ca, Trưởng tầng được giao (kể cả giao tạm), Trưởng đoàn, Người đi cùng, Người nhận tại tầng, Phụ trách bộ phận: `NhiemVuApi` của feature 008 (Phụ trách bộ phận theo 008 FR-001a; Trưởng đoàn, Người đi cùng 008 lấy từ 014);
  - "ca không có Trưởng tầng được giao" (FR-019e): `NhiemVuApi.caKhongCoTruongTang(ca)` của 008 theo 014 FR-066, Q-262.
- Bảng cố định trong mã `QuyenTheoNhiemVu` liệt kê đúng bảy ngoại lệ FR-019a → FR-019g (FR-019g: Người phụ trách ca Xác nhận giường sẵn sàng, Q-313): nhiệm vụ, khóa quyền được thêm, giới hạn (tầng/khu của ca, khoảng FR-032, loại bản ghi gắn tầng, bộ phận). Không có ngoại lệ nào khác (FR-019). ArchUnit cấm nơi khác thêm quyền.
- Nhật ký của lệnh dùng quyền theo nhiệm vụ ghi `can_cu.nhiemVu` (FR-019a → FR-019f "nhật ký ghi căn cứ nhiệm vụ"). `QuyenApi.coQuyen` trả `KetQuaQuyen` gồm `duocPhep`, `lopKhongThoa`, `canCu` (vai trò / nhiệm vụ / ngoại lệ); module chép `canCu` vào nhật ký.
- FR-019f ràng buộc: tài khoản có vai trò AC-01 hoặc AC-14 không giữ được nhiệm vụ Phụ trách bộ phận; kiểm tra nằm ở lệnh chỉ định của 008, 002 cung cấp `TaiKhoanApi.coVaiTro(tk, AC_01|AC_14)`.

**Lý do**: Nhiệm vụ đổi theo ca và theo lệnh của 008; lưu bản sao ở 002 sẽ lệch.

---

## A-07. Phạm vi dữ liệu (FR-028 → FR-035a)

**Quyết định**:
- **Tính trong từng yêu cầu, không đệm giữa các yêu cầu** (FR-033, BR-M15-02). Trong một yêu cầu, kết quả được nhớ lại cho các lần hỏi sau (bộ nhớ theo yêu cầu).
- **Nguồn**:
  - phân công đang hiệu lực của nhân viên (008): đối tượng (khu vực, tầng, phòng, người cao tuổi), ca, khoảng hiệu lực;
  - phân bổ giường đang mở (003): người cao tuổi → giường → phòng → tầng → khu vực;
  - nhiệm vụ (A-06): tầng/khu của ca với Người phụ trách ca; người tham gia chuyến với Trưởng đoàn, Người đi cùng (FR-031).
- **Khoảng theo ca** (FR-032): phân công gắn ca góp vào phạm vi trong `[bắt đầu ca − CFG-M15-07, kết thúc ca + CFG-M15-07]`. Phân công không gắn ca (giao Trưởng tầng) góp trong suốt thời gian hiệu lực. CFG-M15-07 đọc một lần mỗi yêu cầu (000 FR-018).
- **Hai dạng trả về** của `PhamViApi`:
  - `trongPhamVi(nd, doiTuong, thoiDiem)` → boolean, cho lệnh trên một đối tượng;
  - `boLoc(nd, khoaQuyen)` → `PhamViLoc` là `TOAN_VIEN` hoặc tập `(tangKhuVucIds, phongIds, nguoiCaoTuoiIds)`. Module chuyển thành điều kiện SQL (`IN (...)`) cho danh sách, tìm kiếm, số đếm (FR-035). Với 300 người cao tuổi (NFR-01), tập id tối đa vài trăm phần tử, an toàn cho `IN`.
- **Đối tượng không gắn người cao tuổi** (FR-034, CHK049): module sở hữu cài `GanPhamViProvider` trả tầng/khu hoặc nhân viên của đối tượng (lịch ca, yêu cầu đổi ca, phản hồi). 002 không biết cấu trúc đối tượng.
- **Phạm vi báo cáo khoảng đã qua** (FR-035a, Q-192, Q-195): `PhamViApi.boLocLichSu(nd, khoaQuyen, tu, den)` dựng từ lịch sử phân công của 008 (008 FR-037a). Chỉ dùng cho đọc; mọi lệnh ghi dùng `boLoc` hiện hành. ArchUnit cấm gọi `boLocLichSu` từ phương thức `@LenhNghiepVu`.
- **Ngoại lệ sự cố, khẩn cấp** (FR-044a, BR-M15-01): khi khóa là `(SU_CO_KHAN_CAP, TAO)`, `QuyenApi` bỏ lớp phạm vi với người cao tuổi chưa ở trạng thái cuối và trả `canCu = NGOAI_PHAM_VI` nếu người đó nằm ngoài phạm vi thường. Thông tin nhận dạng (họ tên, ảnh, phòng/giường) do 001 cung cấp qua `NguoiCaoTuoiApi.nhanDang`; 007 chỉ hiển thị ba trường này cho người ngoài phạm vi. Nhật ký mang `chiTiet.ngoaiPhamVi = true`.
- **Người thân** (FR-045, FR-049): phạm vi là tập người cao tuổi có quan hệ Hiệu lực (012), trừ người đã ở trạng thái cuối quá CFG-M01-04 (001, 004). Tính trong từng yêu cầu như nhân viên.

**Lý do**: Phân công đổi nhiều lần trong ngày (đổi ca, chuyển giường). Đệm giữa các yêu cầu tạo khoảng trễ trái FR-033; tính lại mỗi yêu cầu chỉ là vài truy vấn khóa chính và chỉ mục trên tập nhỏ.

**Phương án đã cân nhắc**:
- Bảng phạm vi dựng sẵn cập nhật theo sự kiện: nhanh hơn khi đọc nhưng có khoảng trễ giữa commit và sự kiện, và là bản sao dữ liệu của 008, 003.
- Row-level security ở MySQL: MySQL không hỗ trợ.

---

## A-08. Giới hạn trường (FR-022, FR-034, BR-M15-11)

**Quyết định**:
- Enum `NhomThongTin`: `NHAN_DANG`, `LIEN_HE`, `MUC_CHAM_SOC`, `SUC_KHOE` (dị ứng, bệnh nền, tiền sử, chỉ số, đánh giá), `THUOC`, `NGUYEN_VONG`, `CHI_PHI`, `TAI_CHINH`, `CONG_KHAI_NOI_BO`. Danh sách cố định theo 19.3; module không thêm nhóm.
- Bảng `quy_tac_xem_truong` (nạp từ migration, nguồn 19.3): vai trò × nhóm → được xem / không. Ví dụ Hành chính, Kế toán không xem `SUC_KHOE`, `THUOC`; `NGUYEN_VONG` chỉ thấy "đã có / chưa có"; Bếp không xem `SUC_KHOE`.
- **Theo cặp (nhóm, phạm vi) của từng vai trò rồi mới hợp** (FR-034): `QuyenXemTruongApi.duocXem(nd, nhom, doiTuong)` xét từng vai trò của tài khoản: vai trò cho xem nhóm đó **và** đối tượng nằm trong phạm vi của vai trò đó. Ví dụ Hành chính kiêm Điều dưỡng xem `SUC_KHOE` chỉ khi người cao tuổi trong phạm vi phân công điều dưỡng.
- **Áp ở backend khi tuần tự hóa**: trường DTO mang chú thích `@NhomTruong(NhomThongTin.SUC_KHOE)`. Một `BeanSerializerModifier` của Jackson bỏ trường người xem không được xem và thêm mảng `truongBiAn` vào đối tượng. Giá trị thật không rời máy chủ. Nhật ký dùng cùng API (000 R-06).
- Khoản chi phí loại Thuốc (Q-133): trường `tenThuoc`, `hamLuong` mang nhóm `THUOC`; `maVatPham` mang `CHI_PHI`.
- Bộ lọc và tìm kiếm: module không cho lọc theo trường người xem không được xem (ví dụ Hành chính không lọc theo tên thuốc). Kiểm tra bằng cùng API khi nhận tham số lọc.

**Lý do**: Một điểm áp dụng cho mọi DTO, khó quên hơn kiểm từng màn hình.

**Phương án đã cân nhắc**: Mỗi module tự tạo DTO riêng theo vai trò: lặp và dễ sót khi thêm trường.

---

## A-09. Điều kiện pháp lý (FR-036 → FR-040, BR-M06-05, BR-M15-19)

**Quyết định**:
- Bảng `nghiep_vu_chuyen_mon` (nhóm 1): mã, tên, khóa quyền áp dụng (`ChucNang` + `CHUYEN_MON`), loại giấy phép/đào tạo yêu cầu, có yêu cầu phạm vi hoạt động của cơ sở không. Bảng nối `nghiep_vu_chuyen_mon_pham_vi` tới danh mục phạm vi hành nghề **của feature 008** (008 FR-006, BR-M15-19); 002 không giữ danh mục riêng.
- Hai mục mặc định nạp từ migration, cờ `mac_dinh = true`, không ngừng hiệu lực được: chẩn đoán và kê đơn nội bộ; phát thuốc và xác nhận liều.
- Kiểm tra tại thời điểm thao tác (hoặc thời điểm thiết bị với bản ghi ngoại tuyến):
  - (a), (b) qua `GiayPhepApi.hieuLuc(nhanVienId, loai, ngay)` của 008: trả giấy phép còn hiệu lực đến hết ngày hết hạn theo Asia/Ho_Chi_Minh (FR-038) cùng các mục phạm vi hành nghề;
  - (c) qua `GiayPhepCoSoApi.coPhamVi(phamVi, ngay)` của feature 007 (007 FR-070, mục 10.4).
- Không lưu trạng thái "quyền chuyên môn đang bật". Bật, tắt theo ngày được suy ra ở mỗi lần kiểm tra, nên gia hạn có hiệu lực ngay (FR-038, SC-006).
- Bản ghi đã xác nhận không bị kiểm lại khi giấy phép hết hạn (FR-040): kiểm tra chỉ chạy trong lệnh.
- **Cảnh báo sắp hết hạn** (FR-039): job `CanhBaoGiayPhepSapHetHan` chạy hằng ngày lúc 06:00 (chu kỳ kỹ thuật). Job lấy giấy phép cơ sở và giấy phép người hành nghề có ngày hết hạn trong CFG-M06-02, phát sự kiện `GiayPhepSapHetHan` tới 009 kèm nghiệp vụ chuyên môn bị ảnh hưởng và người liên quan. Bảng kỹ thuật `da_canh_bao_giay_phep (loai, giay_phep_id, ngay_het_han)` chống gửi lặp cho cùng một ngày hết hạn; gia hạn đổi ngày hết hạn nên lần sắp hết hạn sau lại được cảnh báo. Cảnh báo đào tạo bắt buộc (CFG-M09-03) thuộc 008.

**Lý do**: Suy ra theo ngày ở mỗi lần kiểm tra loại bỏ mọi job bật, tắt quyền và đúng nguyên tắc không tự sinh dữ liệu (FR-010a của 000).

---

## A-10. Quyền hiệu lực: một điểm kiểm tra (FR-041 → FR-044a)

**Quyết định**:
- `QuyenApi.coQuyen(nd, khoaQuyen, doiTuong)` là điểm duy nhất, chạy theo thứ tự:
  1. vai trò (A-04, A-05) cùng điều kiện chú thích;
  2. quyền theo nhiệm vụ (A-06) nếu bước 1 không thỏa;
  3. phạm vi (A-07), trừ ngoại lệ FR-044a;
  4. điều kiện pháp lý (A-09) nếu khóa là `CHUYEN_MON`.
- Kết quả `KetQuaQuyen(duocPhep, lopKhongThoa: VAI_TRO | PHAM_VI | PHAP_LY | DU_LIEU_NGUON, giayPhep?, canCu)`.
- **Ánh xạ lỗi theo FR-043**:
  - người dùng không xem được chính đối tượng (ngoài phạm vi xem, hoặc không tồn tại): 404 với thông báo chung "không tìm thấy hoặc không có quyền", không nêu lớp (000 R-03);
  - xem được nhưng thiếu quyền thao tác: 403 `KHONG_CO_QUYEN` kèm `lopKhongThoa`;
  - thiếu điều kiện pháp lý: 403 `DIEU_KIEN_PHAP_LY` kèm tên giấy phép, ngày hết hạn.
- **Danh sách thao tác được phép (FR-042)**: mọi `GET` chi tiết của đối tượng nhóm 2 trả `lenhDuocPhep[]`, tính bằng cùng `QuyenApi` (000 đã dùng cách này). `GET /toi/quyen` trả tập khóa quyền của người dùng để client ẩn menu. Client chỉ dùng để hiển thị (constitution XI).
- **Dữ liệu nguồn thiếu, mâu thuẫn** (FR-041b): API của module nguồn ném `DuLieuQuyenKhongHopLe(nguon, loaiLoi, banGhiId)`. `QuyenApi` từ chối với `lopKhongThoa = DU_LIEU_NGUON`, trừ khóa `(SU_CO_KHAN_CAP, TAO)` (đi theo FR-044a với phần nhận dạng có được). Bảng `loi_du_lieu_quyen` gộp theo `(nguon, ban_ghi_id, loai_loi)`: lần đầu thêm dòng và phát sự kiện báo Quản lý viện; các lần sau chỉ tăng bộ đếm. Lần kiểm tra kế tiếp đọc được dữ liệu hợp lệ thì đặt `da_khac_phuc_luc` (CHK059). Đây là bản ghi kỹ thuật kèm theo lần kiểm tra quyền, không phải dữ liệu nghiệp vụ tự sinh.
- **Bộ lập lịch** (FR-044): `NguoiDungHienTai.laBoLapLich()` thì `QuyenApi` bỏ lớp phạm vi và pháp lý. Job chỉ gọi các tác động mà quy tắc nguồn quy định (FR-010a của 000).

---

## A-11. Ngoại tuyến (FR-041a, FR-041c, BR-M15-20)

**Quyết định**:
- **Kiểm tra theo thời điểm thiết bị**: `QuyenApi.coQuyenTaiThoiDiem(nd, khoaQuyen, doiTuong, t)` dùng lịch sử:
  - trạng thái tài khoản tại `t`: bảng `lich_su_trang_thai_tai_khoan` (mỗi lần chuyển một dòng, chỉ thêm);
  - vai trò, thu hẹp, quyền gán thêm tại `t`: các bảng có hiệu lực (A-05);
  - phân công tại `t`: lịch sử phân công của 008;
  - giấy phép tại `t`: `GiayPhepApi.hieuLuc(..., ngay của t)`.
- **Tính hợp lý của `t`** (FR-041c): bảng `thiet_bi (ma_thiet_bi, tai_khoan_id, lan_ket_noi_cuoi)`; `lan_ket_noi_cuoi` được cập nhật ở mọi yêu cầu trực tuyến của thiết bị. `t` hợp lý khi `lan_ket_noi_cuoi_truoc_khi_ngoai_tuyen <= t <= thời điểm đồng bộ` và `thời điểm đồng bộ − lan_ket_noi_cuoi <= CFG-M15-08`.
- **Ba kết quả** (dùng `KetQuaDongBo` của 000):
  - hợp lý và hợp lệ tại `t`, còn quyền lúc đồng bộ: `CHAP_NHAN`;
  - hợp lý, hợp lệ tại `t`, mất một lớp lúc đồng bộ: `CHAP_NHAN_KEM_NHAN`; module lưu bản ghi với cột `nhan_dong_bo_sau_mat_quyen = true`; 002 phát sự kiện báo Trưởng tầng của tầng liên quan (Người phụ trách ca nếu tầng chưa có Trưởng tầng, Quản lý viện nếu không gắn tầng; CHK056);
  - không hợp lý: `CHO_XEM_LAI`;
  - hợp lý nhưng không hợp lệ tại `t`: `TU_CHOI` kèm lý do.
- **Bản ghi chờ xem lại**: bảng chung `ban_ghi_cho_xem_lai` ở 002 (nhóm 2): loại bản ghi (mã module), nội dung JSON, thời điểm thiết bị, thời điểm đồng bộ, người ghi, tầng/khu, trạng thái `CHO_XEM_LAI` → `DA_CHAP_NHAN` / `DA_TU_CHOI`. Module đăng ký `NgoaiTuyenHandler` để kiểm nội dung và ghi bản ghi thật khi được chấp nhận (thời điểm hiệu lực là lúc chấp nhận, giữ `thoi_diem_thiet_bi`). Bản ghi bị từ chối vẫn lưu ở bảng này, không có hiệu lực (FR-041c).
- **Báo kết quả cho người dùng** (BR-M15-20): phản hồi đồng bộ trả kết quả từng bản ghi; app nhân viên hiển thị danh sách. Nếu tài khoản không còn Hoạt động thì phản hồi kèm kết thúc phiên; nếu phạm vi đã hết, app nhận danh sách người cao tuổi còn trong phạm vi để chặn ghi mới (CHK057).
- Thao tác bắt buộc trực tuyến (kích hoạt khẩn cấp, xác nhận liều có kiểm soát đặc biệt, theo Q-01) không đi qua luồng này; module từ chối bản ghi ngoại tuyến của các thao tác đó.

**Lý do**: Khung chung ở một chỗ, đúng R-13 của 000 ("quy tắc nhận, đưa vào chờ xem lại theo CFG-M15-08 do plan 002 chi tiết").

---

## A-12. Tài khoản người thân và các tác động kèm lệnh

**Quyết định**:
- Tạo, kích hoạt lại, cấp lại mật khẩu tài khoản người thân (FR-050 → FR-052): lệnh bắt buộc trường `cachXacMinh` (`TRUC_TIEP_GIAY_TO` / `GOI_LAI_SO_DA_DANG_KY`), cùng số giấy tờ đối chiếu hoặc số điện thoại đã gọi. Backend đối chiếu số điện thoại với hồ sơ người thân (012): khác số đã đăng ký thì từ chối 409 `XAC_MINH_KHONG_HOP_LE` (US6 #9). Cách xác minh ghi vào `nhat_ky.chi_tiet`.
- **Tác động kèm lệnh của module khác** (không phải tự sinh, FR-010a của 000):
  - Nghỉ việc (FR-009, BR-M09-04): lệnh "Cho nghỉ việc" của 008 gọi đồng bộ `TaiKhoanApi.khoaDoNghiViec(nhanVienId, canCu)` **trong cùng transaction**, nên tài khoản Đã khóa và phiên kết thúc cùng lúc với trạng thái Nghỉ việc. Nhật ký người thực hiện là Bộ lập lịch, căn cứ BR-M09-04 và lệnh nguồn (US2 #4).
  - Quan hệ người thân chấm dứt (FR-049, BR-M15-14): lệnh của 012 gọi `TaiKhoanApi.xetLaiNguoiThan(nguoiThanId, canCu)` trong cùng transaction; hết quan hệ Hiệu lực thì Đã khóa.
- **Chuyển trạng thái theo thời gian** (job hằng ngày lúc 01:00, ShedLock):
  - `ChuyenKhongHoatDong`: tài khoản người thân Hoạt động có mốc tính (lần đăng nhập gần nhất, hoặc thời điểm tạo, kích hoạt lại, mở khóa nếu muộn hơn; CHK015) quá CFG-M15-02;
  - `KhoaSauKetThucLuuTru`: tài khoản người thân mà mọi người cao tuổi có quan hệ đã ở trạng thái cuối quá CFG-M01-04 (5.6).
  - Cả hai chỉ chuyển trạng thái và kết thúc phiên, ghi nhật ký người thực hiện Bộ lập lịch, căn cứ CFG và lần chạy (000 FR-045). Không tạo bản ghi nghiệp vụ nào.
- Người thân ngoài CFG-M01-04 của một người cao tuổi (US6 #6): xử lý ở phạm vi (A-07), không cần job.

---

## A-13. Độ mạnh mật khẩu và tự đăng xuất (Q-309)

**Quyết định**: Hai thông số đã có mã sau Q-309 (2026-10-07): CFG-M15-09 độ mạnh mật khẩu, CFG-M15-10 thời gian không thao tác thì tự đăng xuất theo kênh (dạng `BANG` `{WEB, APP_NHAN_VIEN, APP_NGUOI_THAN}`, mặc định 30 phút, 12 giờ, 30 ngày). Cả hai đọc qua `ThamSoApi` như mọi CFG (000 R-07). Cách áp ở A-02 và A-03. Web hiện cảnh báo trước khi hết giờ (khoảng 2 phút, thông số giao diện, không phải quy tắc); bấm "Tiếp tục làm việc" gửi một yêu cầu nhẹ để đếm lại.

**Lý do**: Chủ dự án chốt ở Q-309; trước đó plan tạm đặt ở cấu hình ứng dụng.

---

## A-14. Kiểm thử (constitution XV)

**Quyết định**:
- Test tên theo BR hoặc FR như 000 R-14, ví dụ `BR_M15_05_saiDuNguong_khoaTam`, `FR_044a_suCoNgoaiPhamVi_chiThayNhanDang`.
- **Bộ test ma trận** `MaTranQuyenKhopNguonTest` (A-04) cùng `MaTranQuyenIT`: 9 vai trò × mọi chức năng × (trong, ngoài phạm vi) × (chú thích thỏa, không thỏa), sinh tự động từ bảng nạp (SC-001).
- **Bản giả cho module chưa có**: 008 (phân công, nhiệm vụ, giấy phép), 003 (phân bổ giường), 012 (quan hệ), 001 (bản đồng ý, nhận dạng), 007 (giấy phép cơ sở) có bản cài giả trong test, giữ đúng chữ ký ở [contracts/module-interfaces.md](contracts/module-interfaces.md) mục 2.
- **Client**:
  - web: màn đăng nhập (thông báo không lộ tồn tại), buộc đổi mật khẩu, quản trị tài khoản và phân quyền (412), ẩn nút theo `lenhDuocPhep`;
  - app nhân viên: đăng nhập trên thiết bị dùng chung (một thiết bị một phiên), mở khóa sớm của Trưởng tầng, hiển thị kết quả đồng bộ (BR-M15-20 có phần ở client nên bắt buộc test client, constitution XV), duyệt bản ghi chờ xem lại;
  - app người thân: đăng nhập, buộc đổi mật khẩu, ẩn phần sức khỏe khi không có quyền.
