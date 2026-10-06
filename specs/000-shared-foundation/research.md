# Research: Nền tảng quy tắc nghiệp vụ dùng chung (000)

**Ngày**: 2026-10-06 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Đây là các quyết định kỹ thuật. Chúng không đổi nghiệp vụ (constitution IX). Các quyết định nền (R-01 → R-05) áp dụng cho mọi feature sau. Plan của các feature đó dẫn lại, không quyết lại.

---

## R-01. Phiên bản công nghệ

**Quyết định**:

| Thành phần | Phiên bản | Ghi chú |
| --- | --- | --- |
| Java | 21 LTS | Theo constitution X |
| Spring Boot | 3.5.x (bản vá mới nhất của nhánh 3.5) | Spring Framework 6.2, Hibernate 6.6 |
| Spring Modulith | 1.4.x | Kiểm tra ranh giới module, sự kiện có lưu trữ (R-09) |
| MySQL | 8.4 LTS | InnoDB, `utf8mb4_0900_ai_ci` |
| Flyway | 11.x | Migration cơ sở dữ liệu (sinh ở giai đoạn triển khai) |
| springdoc-openapi | 2.8.x | Sinh OpenAPI 3.1 từ mã để đối chiếu với `contracts/` |
| JUnit 5, AssertJ, Testcontainers (MySQL 8.4), ArchUnit | Bản hiện hành | Kiểm thử backend |
| React | 19.x, TypeScript 5.x, Vite | Web |
| TanStack Query, React Router | Bản hiện hành | Gọi API, điều hướng |
| Flutter | Kênh stable 3.x, Dart 3.x | Hai app |
| drift (SQLite) | Bản hiện hành | Hàng đợi ngoại tuyến của app nhân viên (R-13) |

**Lý do**: Constitution X đặt mặc định. Spring Boot 3.5 là nhánh 3.x mới nhất còn được hỗ trợ ở thời điểm thiết kế. MySQL 8.4 là bản LTS của dòng 8.x.

**Phương án đã cân nhắc**: Spring Boot 4.x bị loại vì constitution chốt 3.x. MySQL 8.0 bị loại vì đã hết hỗ trợ.

---

## R-02. Cấu trúc kho mã và gói backend

**Quyết định**: Một kho mã có bốn thư mục gốc: `backend/`, `web/`, `app-nhan-vien/`, `app-nguoi-than/`. Backend là một ứng dụng Spring Boot. Mỗi module M01–M15 là một gói con trực tiếp của `vn.eldercare` (`m01` … `m15`) và được Spring Modulith coi là một application module. Mỗi module chỉ công khai gói `api` (giao diện, DTO, sự kiện), phần còn lại là nội bộ.

Feature 000 nằm trong **M15 – Quản trị hệ thống**, vì tham số (BR-M15-04), nhật ký (19.4, BR-M15-11 → 13) và quy tắc nền (BR-M15-06 → 10) đều thuộc M15. Năm gói con: `m15.thamso`, `m15.nhatky`, `m15.pheduyet`, `m15.dinhchinh`, `m15.luutru`. Thêm một gói kỹ thuật `vn.eldercare.common`: đồng hồ, mô hình lỗi, kiểu cơ sở, chú thích phân nhóm dữ liệu. Gói này không chứa dữ liệu nghiệp vụ và không phải module nghiệp vụ.

**Lý do**: Constitution X yêu cầu một khối chia gói theo M01–M15 và chặn truy cập chéo dữ liệu. `ApplicationModules.verify()` của Spring Modulith và ArchUnit kiểm tra điều này tự động trong bộ test.

**Phương án đã cân nhắc**:
- Tách gói `foundation` thành module thứ 16: bị loại vì sinh thêm một module ngoài M01–M15.
- Mỗi module một Maven module: bị loại vì quá nặng cho quy mô đồ án. Modulith đủ để kiểm tra ranh giới.

---

## R-03. Quy ước API

**Quyết định**:
- Tiền tố `/api/v1`. Đường dẫn là tiếng Việt không dấu, nối bằng gạch ngang, ví dụ `/api/v1/yeu-cau-phe-duyet/{id}/duyet`. Tên trường JSON là camelCase không dấu (`lyDo`, `ngayHieuLucMongMuon`).
- Nhóm 2: chỉ có `POST .../{id}/<lenh>` cho từng lệnh nghiệp vụ, không có `PUT`/`PATCH` lên trạng thái (constitution XI). Nhóm 3: chỉ có `POST` tạo và `POST .../{id}/dinh-chinh`. Nhóm 1: `POST`, `PUT`, `POST .../ngung-hieu-luc`, `POST .../kich-hoat-lai`, và `DELETE` khi chưa được tham chiếu.
- Chống ghi đè: mọi tài nguyên sửa được đều trả `ETag` (số phiên bản). Lệnh sửa bắt buộc `If-Match`. Sai phiên bản thì trả `412 Precondition Failed` kèm giá trị hiện hành (FR-021, FR-040).
- Lỗi theo RFC 9457 (`application/problem+json`) và thêm các trường:
  - `maLoi` (ổn định, ví dụ `DIEU_KIEN_CHUA_THOA`, `THIEU_LY_DO`, `KHONG_CO_QUYEN`, `DA_DUOC_XU_LY`);
  - `dieuKienChuaThoa[]` (mã BR, mô tả; FR-006);
  - `giaTriHienHanh` khi bị xung đột.
- Ánh xạ mã HTTP:

  | Mã | Khi nào |
  | --- | --- |
  | 400 | Sai định dạng |
  | 401 | Chưa xác thực |
  | 403 | Không có quyền (BR-M15-01) |
  | 404 | Không tồn tại hoặc ngoài phạm vi; không lộ là có tồn tại |
  | 409 | Vi phạm điều kiện nghiệp vụ hoặc trạng thái |
  | 412 | Sai phiên bản |
  | 422 | Thiếu lý do hoặc giá trị sai kiểu, ngoài khoảng |

- Thời điểm theo ISO-8601 có offset `+07:00`. Ngày theo `yyyy-MM-dd`; client hiển thị dạng dd/MM/yyyy (NFR-11).
- Mỗi lệnh ghi nhận `Idempotency-Key` (UUID do client sinh). Cùng khóa, cùng người gửi thì trả lại kết quả lần đầu, không tạo bản ghi thứ hai. Đây là nền cho đồng bộ ngoại tuyến (R-13) và cho FR-037 ("kích hoạt lặp lại không tạo tác động").

**Lý do**: Constitution X, XI. Cùng một mô hình lỗi cho ba client giúp hiển thị đúng "điều kiện chưa thỏa".

**Phương án đã cân nhắc**: Đường dẫn tiếng Anh bị loại để giữ thuật ngữ mục 2.4 xuyên suốt và khớp ví dụ ở constitution XI. GraphQL bị loại vì constitution chốt REST.

---

## R-04. Thời gian và đồng hồ giả lập

**Quyết định**:
- Backend dùng một bean `java.time.Clock` duy nhất, múi giờ `Asia/Ho_Chi_Minh`. Không mã nào gọi `Instant.now()`, `LocalDate.now()` hay `NOW()` của MySQL; ArchUnit chặn các lời gọi này. Thời điểm luôn lấy từ máy chủ (NFR-09, DBR-25, FR-046).
- Lưu trữ:
  - thời điểm (`Instant`) lưu bằng `DATETIME(6)` theo UTC; JDBC đặt `connectionTimeZone=UTC`, Hibernate đặt `hibernate.jdbc.time_zone=UTC`;
  - ngày lịch nghiệp vụ (ngày hiệu lực, khoảng hiệu lực) lưu bằng `DATE`, tính theo `Asia/Ho_Chi_Minh` (BR-M15-07);
  - quy đổi giữa thời điểm và ngày đi qua một tiện ích duy nhất `ThoiGianNghiepVu` (ví dụ `batDauNgay(LocalDate)` trả 00:00 +07:00).
- Bản ghi ngoại tuyến lưu thêm `thoiDiemThietBi` do client gửi. `thoiDiem` (thời điểm đồng bộ) vẫn do máy chủ đặt.
- Đồng hồ giả lập (NFR-13): profile `test` và `demo` thay bean bằng `DieuKhienDongHo` (đặt, tua). Chỉ hai profile này có endpoint `/api/v1/kiem-thu/dong-ho`; profile `prod` không nạp bean và không có endpoint.

**Lý do**: Một nguồn thời gian cho mọi quy tắc theo giờ, kiểm thử được mọi mốc CFG.

**Phương án đã cân nhắc**:
- Lưu giờ địa phương: Việt Nam không có giờ mùa hè nên cũng được, nhưng UTC tránh lệch khi máy chủ hoặc driver đổi múi giờ.
- `TIMESTAMP` của MySQL: bị loại vì giới hạn năm 2038 và tự đổi múi giờ theo session.

---

## R-05. Bảo đảm nhóm dữ liệu (FR-001 → FR-010, DBR-23)

**Quyết định**: Ba lớp, lớp nào cũng đủ để chặn:

1. **Mã nguồn**:
   - mỗi entity mang chú thích `@NhomDuLieu(DANH_MUC | NGHIEP_VU | GHI_NHAN)`;
   - repository của nhóm 3 kế thừa `ChiThemRepository`, chỉ có `them(...)` và các hàm đọc, không có `save` cho bản ghi đã có hay `delete`;
   - entity nhóm 3 đánh dấu `@Immutable` (Hibernate) sau khi xác nhận;
   - ArchUnit kiểm tra mọi entity có đúng một chú thích nhóm (FR-001) và nhóm 2, 3 không có setter trạng thái công khai.
2. **API**: không có endpoint sửa hay xóa cho nhóm 2, 3 (R-03).
3. **Cơ sở dữ liệu**:
   - backend chạy bằng tài khoản `eldercare_app`. Tài khoản này chỉ có `SELECT, INSERT` trên các bảng nhóm 3 không có bản nháp (`nhat_ky`, `dinh_chinh`, `lich_su_tham_so`) và không có `DELETE` trên mọi bảng nhóm 3;
   - bảng nhóm 3 có bản nháp (do module quy định) cần `UPDATE` cho tới lúc xác nhận. Trigger `BEFORE UPDATE` chặn bằng `SIGNAL SQLSTATE '45000'` khi `OLD.thoi_diem_xac_nhan IS NOT NULL`;
   - trigger `BEFORE DELETE` trên mọi bảng nhóm 3 chỉ cho xóa khi `CURRENT_USER()` là `eldercare_luu_tru`, tài khoản riêng của job loại bỏ dữ liệu (FR-009, FR-048, FR-051).

   Quy ước đặt tên và mẫu trigger nằm ở [data-model.md](data-model.md#quy-uoc-bao-dam-o-co-so-du-lieu). Migration sinh ở giai đoạn triển khai.

"Đã được đính chính" và "Hủy ghi nhận" không ghi ngược vào bản gốc. Chúng được suy ra từ bảng `dinh_chinh` (R-08), nên bản gốc không bao giờ bị `UPDATE`.

**Lý do**: Constitution XII yêu cầu không có UPDATE hay DELETE trên bản ghi đã xác nhận, kể cả với tài khoản Quản lý viện. Chặn ở cơ sở dữ liệu giữ được điều này ngay cả khi mã có lỗi (SC-002).

**Phương án đã cân nhắc**:
- Chỉ kiểm tra ở backend: bị loại vì một lỗi mã là phá được SC-002.
- Event sourcing toàn hệ thống: bị loại vì quá nặng cho đồ án. Bảng ghi thêm cùng nhật ký đã đủ cho nhu cầu truy vết.

---

## R-06. Nhật ký (FR-042 → FR-051a)

**Quyết định**:
- **Ghi**: lệnh nghiệp vụ gọi `NhatKyApi.ghi(BanGhiNhatKy)` ngay trong transaction của lệnh (`@Transactional`, `Propagation.MANDATORY`).
  - Nhật ký và thay đổi cùng commit hoặc cùng rollback, vì cùng một transaction MySQL (FR-047).
  - Không dùng Hibernate Envers hay listener tự động. Nhật ký cần lý do, căn cứ, dấu "tự duyệt", "áp dụng bù", "ngoài phạm vi"; listener không biết các thông tin này và có thể bỏ sót thay đổi ghi bằng câu lệnh hàng loạt.
- **Kiểm tra đủ nhật ký (SC-001)**: test tích hợp dùng `DataSource` proxy đếm câu `INSERT`/`UPDATE` trên bảng nhóm 2, 3. Mỗi transaction có thay đổi phải có ít nhất một `INSERT` vào `nhat_ky`. Ngoài ra, ArchUnit bắt mọi phương thức `@LenhNghiepVu` phải nhận `LyDo` khi lệnh đó bắt buộc lý do.
- **Dữ liệu trước/sau**: cột `JSON`, dạng `{ "<tenTruong>": giaTri }`, chỉ ghi các trường đã đổi. Phần chi tiết riêng (FR-043a) lưu ở cột `chi_tiet JSON`.
- **Che trường (FR-049, BR-M15-11)**:
  - mỗi loại đối tượng khai báo bảng `tenTruong → nhomThongTin` (ví dụ `diUng → SUC_KHOE`, `diaChi → LIEN_HE`) qua `PhanLoaiTruongProvider` của module sở hữu;
  - khi đọc nhật ký, `NhatKyQueryService` gọi `QuyenXemTruongApi` của feature 002 để biết người xem được xem nhóm thông tin nào. Trường không được xem trả `{"daThayDoi": true}`. Có ít nhất một trường bị che thì `lyDo` trả `null` kèm `lyDoBiAn: true`;
  - việc che làm ở backend, không gửi giá trị thật về client.
- **Phạm vi xem**:
  - Quản lý viện xem toàn viện.
  - Nhân viên chỉ xem lịch sử của một đối tượng cụ thể, sau khi `PhamViApi` (feature 002) xác nhận họ đang có quyền xem đối tượng đó. Nhân viên không có tra cứu toàn viện.
  - Người thân bị chặn ở tầng xác thực, theo vai trò AC-10.
- **Hiệu năng (SC-006, NFR-01)**:
  - Ước lượng khoảng 300 người cao tuổi, khoảng 200 bản ghi nhật ký mỗi người mỗi ngày (công việc, liều, chỉ số, cộng thêm thao tác nhân viên). Như vậy khoảng 60 nghìn bản ghi một ngày, khoảng 22 triệu một năm.
  - Chỉ mục `(loai_doi_tuong, id_doi_tuong, thoi_diem)`, `(nguoi_thuc_hien_id, thoi_diem)`, `(hanh_dong, thoi_diem)`.
  - Phân vùng `RANGE` theo năm của `thoi_diem`, nên khóa chính là `(id, thoi_diem)` và bảng không có khóa ngoại. Tham chiếu tới đối tượng là định danh logic.
  - Tra cứu dùng phân trang theo khóa (`thoi_diem`, `id`), không dùng `OFFSET`.
- **Bản tóm tắt đợt loại bỏ**: ghi vào chính bảng `nhat_ky`, `hanh_dong = LOAI_BO_DU_LIEU_HET_HAN`, `khong_loai_bo = true`. Job loại bỏ bỏ qua các dòng này (FR-050, FR-051).

**Lý do**: Ghi nhật ký tường minh trong cùng transaction là cách duy nhất đồng thời bảo đảm nguyên tử và đủ ngữ nghĩa.

**Phương án đã cân nhắc**:
- Hibernate Envers: thiếu lý do, căn cứ và ngữ cảnh lệnh.
- Trigger MySQL ghi nhật ký: không biết người thực hiện và lý do.
- Ghi nhật ký bất đồng bộ qua hàng đợi: vi phạm FR-047.

---

## R-07. Tham số cấu hình (FR-011 → FR-022)

**Quyết định**:
- Bảng `tham_so` (nhóm 1, mỗi mã một dòng) giữ giá trị hiện hành và `phien_ban`. Bảng `lich_su_tham_so` (nhóm 3, chỉ thêm) có một dòng cho mỗi giá trị từng hiện hành, gồm `hieu_luc_tu` (thời điểm lưu), giá trị, người đổi, lý do. Không có cột "hiệu lực đến": giá trị của một dòng hết hiệu lực khi dòng kế tiếp của cùng mã bắt đầu. Vì vậy bảng không bao giờ bị `UPDATE`.
  - Lần nạp ban đầu ghi một dòng lịch sử cho mỗi mã, giá trị mặc định, người thực hiện là Bộ lập lịch hệ thống, `hieu_luc_tu` là thời điểm khởi tạo.
  - DBR-24 được bảo đảm hai cách: `tham_so.ma` là khóa chính, nên mỗi mã có đúng một giá trị hiện hành; `UNIQUE (ma, hieu_luc_tu)` trên lịch sử, nên tại mỗi thời điểm có đúng một dòng lịch sử áp dụng. Lệnh đổi giá trị cập nhật `tham_so` và thêm dòng lịch sử trong cùng transaction. Test tích hợp kiểm `tham_so.gia_tri` luôn bằng giá trị của dòng lịch sử mới nhất.
- **Mã và giá trị**: mã CFG cố định trong một enum Java `MaThamSo`, sinh từ Phụ lục 25. Không có API tạo hay xóa mã (FR-011). Migration nạp dữ liệu ban đầu chứa mô tả, mặc định, cột "Dùng tại".
  - Kiểu giá trị: `THOI_LUONG`, `SO_NGUYEN`, `TY_LE`, `SO_TIEN`, `GIO_TRONG_NGAY`, `BANG`.
  - `BANG` lưu JSON theo JSON Schema riêng của từng mã. Đổi một dòng là một lần thay đổi cả tham số, với nội dung trước và sau đầy đủ (FR-022).
  - Mỗi mã có `rangBuoc`. Phần kỹ thuật hiển nhiên (thời lượng > 0, số nguyên ≥ 0, tỷ lệ 0–100) do plan đặt. Ràng buộc nghiệp vụ lấy từ nguồn, ví dụ CFG-M15-06 > CFG-M15-05.
- **Đọc lúc chạy**: `ThamSoApi.doc(MaThamSo)` trả `GiaTriThamSo` bất biến.
  - Có bộ nhớ đệm trong tiến trình. Bộ đệm bị xóa bằng sự kiện `ThamSoDaDoi` ở pha `AFTER_COMMIT`, nên lần kiểm tra quy tắc đầu tiên sau commit đã thấy giá trị mới (FR-016, SC-004).
  - Backend chạy một instance (đồ án). Nếu sau này chạy nhiều instance, cần thêm kênh xóa bộ đệm chung; ghi ở rủi ro.
  - Mỗi lần kiểm tra quy tắc đọc tham số một lần vào biến cục bộ, nên dùng trọn một giá trị (FR-018).
- **Tra cứu quá khứ (FR-019)**: `ThamSoApi.docTaiThoiDiem(ma, Instant)` lấy dòng `lich_su_tham_so` có `hieu_luc_tu` lớn nhất mà `<= t`.
- **Đổi giá trị**: `POST /tham-so/{ma}/doi-gia-tri` với `If-Match`. Phiên bản lệch thì trả 412 kèm giá trị mới (FR-021). Khôi phục mặc định là cùng lệnh, giá trị bằng mặc định (FR-020).
- **Nhắc trước giá trị theo module**: FR-031a cho phép module đặt thời hạn ngắn hơn. Module làm việc này bằng mã CFG riêng của nó (ví dụ CFG-M10-11), không ghi đè CFG-M15-05.

**Lý do**: Constitution XIII, BR-M15-04, NFR-12.

**Phương án đã cân nhắc**:
- Spring Cloud Config hoặc file `application.yml`: bị loại vì cần triển khai lại hoặc khởi động lại, và không có lịch sử nghiệp vụ.
- Một bảng duy nhất có cột `hien_hanh`: bị loại vì phải `UPDATE` cờ trên dòng cũ, khó bảo đảm duy nhất.

---

## R-08. Đính chính (FR-023 → FR-030)

**Quyết định**:
- Một bảng chung `dinh_chinh` (nhóm 3, chỉ thêm). Mỗi dòng gồm:
  - `loai_ban_ghi`: mã loại của module, ví dụ `M07.LIEU_THUOC`;
  - `id_ban_ghi_goc`;
  - `kieu`: `SUA_NOI_DUNG` hoặc `HUY_GHI_NHAN`;
  - `noi_dung_dung` (JSON);
  - `ly_do`, `nguoi_thuc_hien_id`, `thoi_diem`;
  - `cach_tao`: `A_NGUOI_GHI_GOC`, `B_PHU_TRACH_TANG` hoặc `C_YEU_CAU_PHE_DUYET`;
  - `yeu_cau_phe_duyet_id`, chỉ có với cách (c).
- Mọi bản đính chính trỏ về bản gốc ban đầu (FR-027). Giá trị hiện hành là bản `SUA_NOI_DUNG` mới nhất, xếp theo `(thoi_diem, id)`.
- **Tuần tự hóa** đính chính trên cùng một bản gốc: bảng kỹ thuật `chuoi_dinh_chinh (loai_ban_ghi, id_ban_ghi_goc, so_ban, da_huy, phien_ban)` có khóa lạc quan.
  - Thêm đính chính đồng thời tăng `so_ban`. Bên đến sau gặp xung đột và thử lại, nên thứ tự xác định được.
  - Đã `da_huy = true` thì từ chối `SUA_NOI_DUNG` (FR-029).
  - Bảng này là dữ liệu dẫn xuất kỹ thuật, không phải bản ghi nhóm 3, nên được `UPDATE`.
- **SPI cho module sở hữu** (`DinhChinhDuocProvider<T>`):
  - `loaiBanGhi()`;
  - `ganTangKhuVuc(id)` trả tầng/khu vực hoặc rỗng (FR-026 b/c, CHK051);
  - `nguoiGhiGoc(id)`, `daXacNhan(id)`;
  - `kiemTraNoiDung(json)`;
  - `coQuyenGhiNhan(nguoiDung)`;
  - `thuHep(...)` (tùy chọn, FR-026 "module MAY thu hẹp");
  - `apDung(id, banDinhChinh)` (tùy chọn, để module phát sự kiện tác động dây chuyền, ví dụ 010 lập khoản điều chỉnh).
- **Chọn cách tạo (FR-026)**: `DinhChinhService` thử lần lượt (a), (b), (c). Với (b), dùng `PhamViApi` (002) để xác định "người phụ trách ca / trưởng tầng của tầng đó, trong phạm vi hiện tại", gồm cả nhánh dự phòng CHK033. Cách (c) tạo một yêu cầu phê duyệt loại `DINH_CHINH` (R-10). Bản đính chính chỉ được thêm khi yêu cầu đó Đã áp dụng.
- **Đọc giá trị hiện hành (FR-030)**:
  - API `DinhChinhApi.hienHanh(loai, ids)` trả theo lô;
  - view MySQL `v_dinh_chinh_moi_nhat` (bản sửa mới nhất mỗi bản gốc) và `v_ban_ghi_da_huy` để truy vấn báo cáo của module tự `LEFT JOIN`. Báo cáo không tự cài lại logic "mới nhất".
- Chi phí đã chốt và giao dịch số dư không đăng ký provider, vì không có đính chính (1.5, DBR-17). Gọi đính chính cho các loại này thì trả 409 `KHONG_HO_TRO_DINH_CHINH`.

**Lý do**: Một cơ chế cho mọi loại bản ghi nhóm 3 để giữ đúng một quy tắc (FR-026) cho mọi module; module chỉ khai báo phần riêng.

**Phương án đã cân nhắc**:
- Mỗi module có bảng đính chính riêng: lặp quy tắc ở 10 module, dễ lệch.
- Ghi cờ "đã đính chính" lên bản gốc: vi phạm R-05.

---

## R-09. Sự kiện giữa module và thông báo

**Quyết định**: Dùng sự kiện ứng dụng của Spring Modulith, có Event Publication Registry lưu ở MySQL (transactional outbox). Sự kiện chỉ được giao sau commit và được giao lại nếu tiến trình chết giữa chừng.

000 phát các sự kiện:
- `YeuCauDaGuiDuyet`;
- `YeuCauCanNhacNguoiDuyet`;
- `YeuCauCanBaoQuanLyVien`;
- `YeuCauDaQuyetDinh`;
- `YeuCauApDungKhongThanh`;
- `ThamSoDaDoi`;
- `DaDinhChinh`.

Feature 009 (M13) lắng nghe các sự kiện này và gửi thông báo (constitution XIV). 000 không gọi trực tiếp kênh gửi nào. Mức thông báo do module nguồn nêu trong payload sự kiện.

**Lý do**: Constitution XIV yêu cầu thông báo qua dịch vụ chung. Gửi sau commit không thông báo một thay đổi đã rollback. Outbox không mất thông báo nhắc.

**Phương án đã cân nhắc**:
- Gọi đồng bộ `ThongBaoApi`: lỗi gửi có thể làm hỏng lệnh nghiệp vụ.
- Message broker ngoài (Kafka, RabbitMQ): thừa với một khối.

---

## R-10. Yêu cầu phê duyệt (FR-031 → FR-041)

**Quyết định**:
- **Bảng**: `yeu_cau_phe_duyet` (nhóm 2). Lịch sử trạng thái lưu ở `nhat_ky`. Tác động lưu ở dữ liệu của module, có cột `yeu_cau_phe_duyet_id` (FR-039).
- **Máy trạng thái**: viết bằng mã. Trạng thái là chuỗi mã. Bảng chuyển chung ở [data-model.md](data-model.md#yeu_cau_phe_duyet) được cài thành `BangChuyenTrangThai`. Loại yêu cầu được khai báo thêm trạng thái và chuyển (015: `CHO_NGUOI_NHAN_DONG_Y`, `NGUOI_NHAN_TU_CHOI`, `DA_AP_DUNG_MOT_PHAN`, `HET_HIEU_LUC`; 001: `DUOC_THAY_THE`). Chuyển không khai báo thì bị chặn.
- **SPI** (`LoaiYeuCauHandler`), mỗi loại yêu cầu do module sở hữu cài một handler:
  - `maLoai()`;
  - `kiemTraNoiDung(json)`;
  - `quyenTao()`, `quyenDuyet()`: khóa quyền đối chiếu cột T, D của Phụ lục 27 qua 002;
  - `phamVi(yeuCau)`: đối tượng hoặc tầng, để kiểm tra phạm vi duyệt;
  - `kiemTraDieuKienApDung(yeuCau)`: trả danh sách điều kiện chưa thỏa;
  - `apDung(yeuCau, ngayHieuLucThucTe)`: ghi tác động, trả tham chiếu kết quả;
  - `choTuDuyet()`: mặc định `true`; ví dụ 013 FR-027a trả `false`;
  - `maThamSoNhac()`: tùy chọn, mã CFG riêng của module.
- **Áp dụng đúng một lần (FR-037, SC-003)**:
  - mỗi lần áp dụng chạy trong một transaction riêng: khóa dòng yêu cầu bằng `SELECT … FOR UPDATE`, kiểm tra còn `DA_DUYET`, gọi `kiemTraDieuKienApDung`, rồi gọi `apDung`, rồi chuyển `DA_AP_DUNG`, rồi ghi nhật ký;
  - `UNIQUE (yeu_cau_phe_duyet_id)` trên bảng `ket_qua_ap_dung` chặn trùng ngay cả khi có lỗi logic;
  - lần kích hoạt sau thấy trạng thái đã kết thúc thì không làm gì.
- **Lỗi khi áp dụng (FR-037b)**: transaction rollback, yêu cầu vẫn `DA_DUYET`. Một transaction riêng (`REQUIRES_NEW`) ghi nhật ký `AP_DUNG_LOI` kèm nguyên nhân.
- **Điều kiện không thỏa (FR-038)**: chuyển `AP_DUNG_KHONG_THANH`, ghi `dieuKienChuaThoa` vào lý do, phát `YeuCauApDungKhongThanh`.
- **Bộ lập lịch**:
  - Spring `@Scheduled`; mỗi lần chạy có một dòng `lan_chay_lich` (id, job, bắt đầu, kết thúc, kết quả), dùng làm "định danh lần chạy" trong căn cứ nhật ký (FR-045);
  - job `ApDungYeuCauDenHan` chạy lúc 00:05 hằng ngày và khi ứng dụng khởi động. Nó lấy mọi yêu cầu `DA_DUYET` có `ngay_hieu_luc_thuc_te <= hôm nay`, sắp theo `(ngay_hieu_luc_thuc_te, thoi_diem_quyet_dinh)`, áp dụng lần lượt (FR-037a, BR-M15-10);
  - "Áp dụng bù" là yêu cầu có ngày hiệu lực thực tế trước ngày chạy. Nhật ký mang dấu `apDungBu = true` cùng cả hai mốc;
  - job `NhacYeuCauChoDuyet` chạy mỗi 15 phút (chu kỳ kỹ thuật, không phải tham số nghiệp vụ). Hai cột `da_nhac_luc` và `da_bao_qlv_luc` chống nhắc lặp. Cách tính theo giờ liên tục từ `thoi_diem_gui_duyet`. Khi không còn người duyệt thì báo Quản lý viện ngay ở mốc CFG-M15-05 (BR-M15-09).
  - Hai job dùng ShedLock (bảng `shedlock`) để không chạy chồng khi kích hoạt tay hoặc khi khởi động lại.
- **Ngày hiệu lực thực tế (FR-037)**: `max(ngayHieuLucMongMuon, ngàyDuyệt)`, tính khi duyệt. Nếu kết quả ≤ hôm nay thì áp dụng ngay trong transaction riêng sau khi quyết định đã commit. Quyết định duyệt không bị rollback vì áp dụng lỗi; yêu cầu nằm lại `DA_DUYET` và job xử lý ở lần sau, đúng FR-037b.
- **Đồng thời (FR-040)**: cột `phien_ban` (khóa lạc quan) cùng `If-Match`. Quyết định đến sau trả 409 `DA_DUOC_XU_LY`.
- **Quyền xét lúc quyết định (FR-034a)**: không lưu sẵn danh sách người duyệt. Mỗi lần quyết định đều gọi `QuyenApi` (002).

**Lý do**: Áp dụng ngay sau commit tách được quyết định khỏi tác động, đúng bảng trạng thái của spec: lỗi áp dụng giữ "Đã duyệt (chờ hiệu lực)".

**Phương án đã cân nhắc**:
- Spring State Machine: nặng và khó kiểm thử hơn bảng chuyển viết tay.
- Quartz: thừa với một instance; `@Scheduled` cùng ShedLock đủ dùng.

---

## R-11. Lưu giữ và loại bỏ dữ liệu (FR-050, FR-051, FR-051a)

**Quyết định**:
- Module đăng ký `LuuTruProvider` cho từng loại dữ liệu của mình:
  - gắn với hồ sơ người cao tuổi nào, nếu có;
  - mốc bắt đầu tính hạn (kết thúc lưu trú, hoặc thời điểm tạo);
  - danh sách tham chiếu ra và vào.
- Job `LoaiBoDuLieuHetHan` chạy hằng tháng, bằng một `DataSource` riêng với tài khoản `eldercare_luu_tru` (R-05):
  1. Tính tập ứng viên theo CFG-M15-03 và CFG-M15-04 hiện hành.
  2. Dựng chuỗi tham chiếu: bản gốc, các bản đính chính, nhật ký của các bản ghi trong chuỗi, và các bản ghi tham chiếu.
  3. Loại khỏi tập mọi chuỗi còn phần tử chưa hết hạn (FR-051a).
  4. Xóa từng chuỗi trong một transaction (BR-M15-13).
  5. Ghi một bản nhật ký tóm tắt bằng tài khoản ứng dụng, sau khi đợt chạy kết thúc hoặc dừng giữa chừng, với số đếm thực tế (FR-051).
- CFG-M15-03 và 04 mặc định 10 năm, nên trong thời gian đồ án job không xóa gì trên dữ liệu thật. Job vẫn được kiểm thử đầy đủ bằng đồng hồ giả lập (NFR-13).
- Phạm vi của 000 là khung job, SPI và nhật ký tóm tắt. Mỗi module khai báo provider trong plan của nó.

**Lý do**: Chỉ một tác nhân (AC-11) được xóa, có tài khoản cơ sở dữ liệu riêng để trigger kiểm được.

**Phương án đã cân nhắc**:
- Xóa mềm (cờ `da_loai_bo`): không đáp ứng mục đích "loại bỏ" sau thời hạn.
- `DROP PARTITION` theo năm: không đúng ngữ nghĩa theo chuỗi và theo hồ sơ.

---

## R-12. Xác thực và quyền: phần 000 dùng từ 002

**Quyết định**: 000 không tự cài xác thực. 000 gọi các giao diện của 002, được khai báo trước ở [contracts/module-interfaces.md](contracts/module-interfaces.md):
- `NguoiDungHienTai`: id, vai trò, có phải Bộ lập lịch không;
- `QuyenApi.coQuyen(nguoiDung, khoaQuyen, phamVi)`: ba lớp BR-M15-01;
- `PhamViApi`: đối tượng hoặc tầng trong phạm vi; người phụ trách ca, trưởng tầng của tầng;
- `QuyenXemTruongApi`.

Trong lúc 002 chưa có, test của 000 dùng bản cài giả. Plan của 002 sẽ chốt cơ chế token (dự kiến JWT access token ngắn hạn kèm refresh token, Spring Security Resource Server). Feature 000 không phụ thuộc vào lựa chọn này.

**Lý do**: Thứ tự plan 000 → 002 (CLAUDE.md). Giao diện cần có trước để 000 thiết kế được.

---

## R-13. Ngoại tuyến: phần khung dùng chung (FR-046, DBR-25)

**Quyết định**: 000 chỉ định nghĩa khung chung. Các lệnh của chính 000 (đổi tham số, phê duyệt, đính chính, tra cứu nhật ký) **chỉ chạy trực tuyến**, vì 8.6 chỉ cho ghi tạm với "ghi nhận thực hiện" và các lệnh này cần trạng thái máy chủ (khóa phiên bản, quyết định đầu tiên thắng). Khung chung gồm:
- phong bì đồng bộ: `Idempotency-Key`, `thoiDiemThietBi`, `ngoaiTuyen = true`;
- cột `thoi_diem_thiet_bi` trong `nhat_ky`;
- kết quả đồng bộ từng bản ghi (`CHAP_NHAN`, `CHAP_NHAN_KEM_NHAN`, `CHO_XEM_LAI`, `TU_CHOI`; BR-M15-20) trả cho app.

Hàng đợi trên thiết bị dùng drift (SQLite) trong app nhân viên. Quy tắc nhận, đưa vào "chờ xem lại" theo CFG-M15-08 do plan 002 và 005 chi tiết.

**Lý do**: Không mở rộng ngoại tuyến ngoài 8.6 (constitution IX). Khung thống nhất cho các module ghi nhận dùng chung.

---

## R-14. Kiểm thử (constitution XV, NFR-13)

**Quyết định**:
- **Backend**:
  - test đơn vị cho máy trạng thái, chọn cách đính chính, kiểm tra tham số;
  - test tích hợp với Testcontainers MySQL 8.4 thật, có trigger, tài khoản `eldercare_app` và `eldercare_luu_tru`, để chứng minh SC-002 ở tầng cơ sở dữ liệu;
  - test kiến trúc bằng `ApplicationModules.verify()` và ArchUnit.
- **Đặt tên test**: `BR_M15_09_khongConNguoiDuyet_baoQuanLyVienNgayMocNhac`, hoặc theo FR khi FR không có mã BR (`FR_037_apDungLap_khongTaoTacDongThuHai`). Mỗi kịch bản Given/When/Then của spec 000 có một test. Bảng truy vết nằm ở [quickstart.md](quickstart.md).
- **Đồng hồ**: `DieuKhienDongHo`. Không dùng `Thread.sleep`.
- **Hợp đồng API**: test so `springdoc` sinh ra với `contracts/*.yaml` (bằng openapi-diff); lệch thì test lỗi.
- **Client**:
  - Web dùng Vitest và React Testing Library cho màn hình tham số, nhật ký (hiện "đã thay đổi"), duyệt yêu cầu, xử lý 412;
  - Flutter dùng `flutter_test` cho màn duyệt yêu cầu trên app nhân viên.
  - Không có BR của 000 thực hiện ở client nên không bắt buộc test BR ở client (XV); các test trên kiểm tra hiển thị.
