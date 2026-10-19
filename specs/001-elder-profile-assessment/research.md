# Research: Hồ sơ và đánh giá người cao tuổi (001)

**Ngày**: 2026-10-07 | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Mỗi mục gồm Quyết định, Lý do, Phương án đã cân nhắc. Đây là quyết định kỹ thuật, không đổi nghiệp vụ (constitution IX). Các quyết định nền R-01 → R-05 của [000](../000-shared-foundation/research.md) (phiên bản, cấu trúc kho mã, quy ước API, thời gian, bảo đảm nhóm dữ liệu) và A-02, A-07, A-08, A-10 của [002](../002-account-permission/research.md) (phiên, phạm vi, giới hạn trường, một điểm kiểm tra quyền) được dùng lại, không quyết lại.

---

## H-01. Vị trí trong backend

**Quyết định**: Feature 001 là module **M01** (`com.cambae.backend.m01`). Gói con:

| Gói | Nội dung | FR |
| --- | --- | --- |
| `m01.api` | Giao diện công khai, DTO, sự kiện, SPI `ThamGiaChuyenTrangThai` | Mọi giao diện ở [contracts/module-interfaces.md](contracts/module-interfaces.md) |
| `m01.hoso` | Hồ sơ cá nhân, tra cứu, liên kết hồ sơ trước | FR-001 → FR-009 |
| `m01.trangthai` | Máy trạng thái người cao tuổi, lịch sử trạng thái, mức chăm sóc hiện hành | FR-044 → FR-053 |
| `m01.dongy` | Bản đồng ý chia sẻ dữ liệu | FR-010 → FR-016 |
| `m01.suckhoe` | Hồ sơ sức khỏe ban đầu, mục sức khỏe, danh mục dị nguyên, mức độ, nguồn thông tin | FR-017 → FR-023 |
| `m01.nguyenvong` | Phiếu nguyện vọng cuối đời | FR-023a → FR-023f |
| `m01.danhgia` | Lần đánh giá, nhu cầu chăm sóc, báo đánh giá lại | FR-024 → FR-043 |
| `m01.lich` | Job của Bộ lập lịch (H-12) | FR-012, FR-023d, FR-036, FR-039 → FR-041, FR-050 |

Mỗi gói con chia `internal` (service, repository, entity) và `web` (controller). Chỉ `m01.api` được module khác dùng (Spring Modulith, 000 R-02).

**Lý do**: Constitution X; spec 001 là Module 01 (mục 5) của tài liệu nguồn.

**Phương án đã cân nhắc**: Đặt máy trạng thái ở M02 (feature 004) vì 004 giữ nội dung chi tiết của tạm vắng, kết thúc lưu trú, qua đời. Bị loại vì spec 001 sở hữu tập trạng thái, chuyển hợp lệ, điều kiện chặn và lịch sử (FR-044 → FR-049; spec 004 bảng trạng thái ghi các lệnh này là "(001)").

---

## H-02. Máy trạng thái người cao tuổi và các module tham gia

**Quyết định**:

1. **Một điểm thực hiện**. Mọi chuyển trạng thái đi qua `ChuyenTrangThaiService` của M01. Bảng chuyển hợp lệ (bảng 5.5 và bảng trạng thái của spec) cài thành hằng bảng trong mã; cặp không có trong bảng trả 409 `CHUYEN_KHONG_HOP_LE` (FR-044, BR-M01-04).

2. **Khóa dòng, không dùng `If-Match`**. Mỗi lệnh mở transaction, `SELECT … FOR UPDATE` trên dòng `nguoi_cao_tuoi`, rồi mới kiểm tra điều kiện. Lệnh thứ hai gửi gần như đồng thời chờ khóa, rồi được kiểm tra lại trên trạng thái mới; không hợp lệ thì bị từ chối với điều kiện chưa thỏa (FR-048, US4 #13). Lệnh trạng thái không đòi `If-Match`, vì spec yêu cầu kiểm tra lại chứ không yêu cầu báo xung đột phiên bản.

3. **SPI `ThamGiaChuyenTrangThai`** cho module khác. Mỗi module có điều kiện chặn hoặc tác động ở bảng trạng thái cài một bean:

   ```java
   Set<LenhTrangThai> lenhQuanTam();
   List<DieuKien> kiemTraDieuKien(NguCanhLenh nc);          // rỗng = thỏa; đọc, không ghi
   Optional<MienDieuKien> mienDieuKien(NguCanhLenh nc);     // chỉ 004: yêu cầu ngoại lệ đã duyệt
   void kiemTraChiTiet(LenhTrangThai lenh, JsonNode chiTiet); // module sở hữu nội dung chi tiết của lệnh
   void thucHienTacDong(KetQuaChuyen kq);                    // Propagation.MANDATORY
   ```

   - **Liệt kê đủ điều kiện**: service gọi `kiemTraDieuKien` của mọi bean quan tâm lệnh đó, gộp kết quả, rồi bỏ những điều kiện được miễn. Còn điều kiện thì trả 409 `DIEU_KIEN_CHUA_THOA` với toàn bộ danh sách trong một lần (FR-045, SC-002).
   - **Hoặc toàn bộ, hoặc không** (FR-047): đủ điều kiện thì đổi trạng thái, thêm dòng lịch sử, ghi nhật ký, rồi gọi `thucHienTacDong` của mọi bean, tất cả trong cùng transaction. Một bean ném lỗi thì rollback toàn bộ. Thứ tự gọi cố định theo `@Order` để test lặp lại được.
   - **Tác động có độ trễ** (khóa tài khoản người thân sau CFG-M01-04): không lưu lịch riêng. Job `KhoaSauKetThucLuuTru` của 002 tính mốc từ thời điểm trạng thái cuối, nên việc "lên lịch" chính là dòng lịch sử trạng thái đã commit (FR-047 CHK022).
   - **Thông báo**: phát sự kiện `NguoiCaoTuoiDaChuyenTrangThai` sau commit, qua outbox của 000 (R-09). Thông báo Khẩn cấp cho người liên hệ chính khi Chuyển viện và Ghi nhận qua đời do 009 gửi từ sự kiện này (Q-97).

4. **Bảng bean tham gia** (theo bảng trạng thái của spec; module chưa có thì dùng bản giả trong test):

   | Lệnh | Điều kiện chặn (bean) | Tác động trong transaction (bean) |
   | --- | --- | --- |
   | Hoàn tất tiếp nhận | M01: đánh giá đầu vào Đã xác nhận, lần gần nhất chưa quá CFG-M01-02 (FR-034a). 004: hợp đồng Hiệu lực, không sớm hơn ngày bắt đầu (004 FR-075), mức hợp đồng trùng mức hiện hành, đặt cọc đạt. 003: phân bổ giường, giường đã Trống hoặc đổi giường trong lệnh. 012: DBR-02 | 004: lượt đăng ký Đã tiếp nhận, sự kiện cho 018. 005, 006, 011: tính lịch từ thời điểm lệnh. 005: báo Người phụ trách ca. M01: bắt đầu nhắc bản đồng ý, nguyện vọng nếu thiếu (H-12) |
   | Hủy tiếp nhận | Có lý do | 003: hủy giữ chỗ. 004: đóng hồ sơ chờ |
   | Cho tạm vắng | 012: bản ghi đón hợp lệ | 004: mở lượt vắng (chi tiết lệnh). 005, 011: hủy việc, suất. 006: liều Mang theo / Tạm dừng. 003: giường theo chính sách |
   | Chuyển viện | Căn cứ (sự cố hoặc chỉ định) nằm trong chi tiết lệnh | Như Cho tạm vắng; 004 mở hoặc đổi lượt vắng loại Bệnh viện |
   | Ghi nhận trở về | — | 004: đóng lượt vắng. 006, 011: tính lại. 005: báo Người phụ trách ca. Từ bệnh viện: 006 tạm dừng lịch cũ, M01 báo đánh giá lại (H-11) |
   | Kết thúc lưu trú | 013: đồ gửi. 006: thuốc gửi. 010: chi phí đã chốt. 007: cảnh báo, sự cố mở. Miễn: 004 yêu cầu ngoại lệ đã duyệt | 005, 006, 011: hủy lịch tương lai. 003: giải phóng giường. 004: hợp đồng, lượt vắng |
   | Ghi nhận qua đời | Người xác nhận (FR-047b) | Như Kết thúc lưu trú; 004: bản ghi qua đời (004 FR-069, FR-070) |

5. **Ai sở hữu endpoint**. M01 có một endpoint `POST /nguoi-cao-tuoi/{id}/<lenh>` cho mỗi lệnh (constitution XI). Phần thân có trường `chiTiet` (JSON). Bean của module sở hữu nội dung chi tiết kiểm tra trường này bằng `kiemTraChiTiet` rồi ghi bản ghi của mình trong `thucHienTacDong`. Ví dụ: 004 ghi lượt vắng (thời điểm dự kiến trở lại, người đón, có mang thuốc), bản ghi qua đời (004 FR-069); 003 nhận giường đổi trong lệnh Hoàn tất tiếp nhận (003 FR-023). Kiểu của `chiTiet` theo từng lệnh được khai báo ở [contracts/api-001.yaml](contracts/api-001.yaml) và do plan của 003, 004 chốt.

6. **Xem trước điều kiện**. `GET /nguoi-cao-tuoi/{id}/dieu-kien/{lenh}` chạy cùng bộ `kiemTraDieuKien` mà không đổi gì. Danh sách điều kiện tiếp nhận của 004 (004 FR-004) dùng API này qua `TrangThaiNguoiCaoTuoiApi.xemDieuKien`.

**Lý do**:
- Spec đòi một lần thực hiện vừa đổi trạng thái vừa kích hoạt tác động, hoặc không gì cả (FR-047). Chỉ lời gọi đồng bộ trong cùng transaction làm được việc này. Sự kiện sau commit không đủ.
- Điều kiện nằm ở nhiều module. SPI giữ được ranh giới module (constitution X): M01 không đọc bảng của module khác, module khác không đổi trạng thái của M01.

**Phương án đã cân nhắc**:
- Saga hoặc sự kiện bất đồng bộ cho tác động: bị loại vì vi phạm FR-047.
- M01 gọi trực tiếp từng API của 003, 004, 005…: bị loại vì M01 sẽ phụ thuộc vào mọi module và tạo vòng phụ thuộc (các module đó cũng gọi M01).
- Mỗi module tự có endpoint cho lệnh của mình (004 có `/cho-tam-vang`): bị loại vì hai nơi cùng đổi trạng thái người cao tuổi thì khó giữ FR-044, FR-048.

---

## H-03. Chuyển trạng thái do module khác kích hoạt

**Quyết định**: Ba trường hợp không có endpoint M01 riêng. Module nguồn gọi `TrangThaiNguoiCaoTuoiApi.thucHien(...)` với `Propagation.MANDATORY`, trong transaction của lệnh nguồn:

| Trường hợp | Module gọi | Người thực hiện ghi ở lịch sử | Căn cứ |
| --- | --- | --- | --- |
| Điểm danh rời viện, điểm danh về của chuyến đi | 014 | Người điểm danh; lệnh mang dấu `doHeThong = true`, không bắt buộc lý do (FR-046, CHK046) | Chuyến đi (BR-M01-06, 014 FR-040) |
| Hủy ghi nhận điểm danh rời viện | 014 (FR-031a) | Trưởng tầng đính chính | Bản đính chính điểm danh |
| Ghi chuyển viện trên một sự cố Khẩn cấp | 007 | Điều dưỡng hoặc Bác sĩ ghi | Sự cố (BR-M01-06, BR-M05-14) |

Mọi kiểm tra của H-02 (bảng chuyển, khóa dòng, điều kiện, tác động) vẫn chạy. Lệnh nguồn thất bại thì chuyển trạng thái cũng rollback.

**Lý do**: Spec quy định các chuyển này xảy ra "khi" một lệnh của module khác thành công (bảng trạng thái của spec, BR-M01-06).

**Phương án đã cân nhắc**: Lắng nghe sự kiện sau commit của 014, 007. Bị loại vì có thể có lúc chuyến đi đã ghi rời viện nhưng trạng thái chưa đổi.

---

## H-04. Hồ sơ cá nhân

**Quyết định**:

- **Mã hồ sơ** (FR-003): `NCT-<năm tạo>-<id 6 chữ số>`, tạo từ `id` sau `INSERT` trong cùng transaction. Cột có `UNIQUE`. Không có bảng đếm riêng.
- **CCCD duy nhất trong hồ sơ chưa ở trạng thái cuối** (DBR-01): cột sinh `cccd_dang_mo = IF(loai_giay_to = 'CCCD' AND trang_thai NOT IN ('KET_THUC_LUU_TRU','QUA_DOI','HUY_TIEP_NHAN'), so_giay_to, NULL)` có `UNIQUE`. MySQL cho phép nhiều `NULL`, nên hồ sơ cũ không chặn hồ sơ mới. Backend kiểm trước để trả 409 `CCCD_DANG_DUNG` kèm mã hồ sơ đang mang CCCD đó (US1 #2). Ràng buộc ở cơ sở dữ liệu chặn trường hợp hai lệnh chạy đồng thời.
- **Liên kết hồ sơ trước** (FR-004):
  - Khi tạo hồ sơ có CCCD, backend tìm hồ sơ gần nhất cùng CCCD ở Kết thúc lưu trú hoặc Hủy tiếp nhận, rồi đặt `ho_so_truoc_id`.
  - Hồ sơ trùng ở Qua đời thì không liên kết. Phản hồi 201 kèm cảnh báo `CCCD_TRUNG_HO_SO_QUA_DOI` (US1 #10).
  - Chuỗi liên kết đọc bằng truy vấn đệ quy (`WITH RECURSIVE`).
  - Quyền xem từng hồ sơ trong chuỗi kiểm riêng bằng `QuyenApi` trên chính hồ sơ đó.
- **Cảnh báo trùng khi không có CCCD** (FR-005):
  - Cột sinh `ho_ten_so_khop` (bỏ khoảng trắng thừa) dùng collation `utf8mb4_0900_as_ci`: không phân biệt hoa thường nhưng **có** phân biệt dấu. Collation mặc định của R-01 (`ai_ci`) không phân biệt dấu nên không dùng được ở đây.
  - Chỉ mục `(ho_ten_so_khop, ngay_sinh)`.
  - Có trùng thì trả 409 `CO_THE_TRUNG_HO_SO` kèm danh sách. Client gửi lại với `xacNhanKhongTrung = true`; nhật ký ghi lần xác nhận này.
- **Cập nhật thông tin cá nhân** (FR-006): `POST …/{id}/cap-nhat-thong-tin`, bắt buộc `If-Match` và `lyDo`. Nhật ký ghi các trường đổi, trước và sau.
- **Chặn trên hồ sơ trạng thái cuối** (BR-M01-05, FR-049): mọi lệnh ghi của M01 gọi `HoSoMoGuard.kiemTra(nctId, ngoaiLe)` đầu tiên. Guard từ chối với 409 `HO_SO_DA_DONG`, trừ hai ngoại lệ: "Rút lại đồng ý" và đính chính đi đường (c) của 000 (H-10). Một test ArchUnit buộc mọi phương thức `@LenhNghiepVu` trong `m01` phải gọi guard này.

**Lý do**: Cột sinh cùng `UNIQUE` đưa DBR-01 xuống cơ sở dữ liệu, đúng constitution XII, mà không phải tự viết khóa.

**Phương án đã cân nhắc**:
- Kiểm CCCD chỉ ở backend: hai lệnh đồng thời có thể cùng qua.
- Chuẩn hóa tên bằng thư viện Java rồi lưu cột thường: được, nhưng cột sinh giữ được một nguồn duy nhất cho quy tắc so khớp.

---

## H-05. Quyền và giới hạn trường

**Quyết định**:

- **Khóa quyền** theo dòng của Phụ lục 27, đối chiếu tự động bằng `MaTranQuyenKhopNguonTest` của 002:

  | Chức năng (khóa) | Dòng Phụ lục 27 | Thao tác dùng ở 001 |
  | --- | --- | --- |
  | `HO_SO_NGUOI_CAO_TUOI` | Hồ sơ người cao tuổi (UC-01, 02) | XEM, TAO, SUA |
  | `BAN_DONG_Y` | Bản đồng ý chia sẻ dữ liệu (UC-03) | XEM, TAO (ghi nhận, rút lại) |
  | `DI_UNG_BENH_NEN` | Dị ứng, bệnh nền (UC-04) | XEM, TAO (ghi nhận, loại trừ, hồ sơ sức khỏe ban đầu) |
  | `DANH_GIA_MUC_CHAM_SOC` | Đánh giá, mức chăm sóc (UC-05, 06) | XEM, TAO (lập nháp), DUYET (xác nhận) |
  | `TIEP_NHAN_HOP_DONG` | Tiếp nhận, hợp đồng, đặt cọc (UC-08) | TAO (Hoàn tất, Hủy tiếp nhận) |
  | `TAM_VANG_TRO_VE` | Tạm vắng, trở về (UC-15) | TAO |
  | `KET_THUC_QUA_DOI` | Kết thúc lưu trú, qua đời (UC-17, 18) | TAO; FR-047b kiểm thêm theo trạng thái |
  | `NGUYEN_VONG_CUOI_DOI` | Nguyện vọng cuối đời (UC-82) | XEM, TAO |
  | `CHUYEN_VIEN` | Không có dòng riêng ở Phụ lục 27 (xem plan, điểm báo lại 1) | TAO cho AC-03, AC-04 theo UC-16 và BR-M01-06 |

- **Giới hạn trường** dùng `@NhomTruong` của 002 (A-08), không thêm nhóm mới:
  - giấy tờ định danh, ảnh: `NHAN_DANG`;
  - địa chỉ, liên hệ: `LIEN_HE`;
  - mức chăm sóc hiện hành: `MUC_CHAM_SOC`;
  - mục sức khỏe, hồ sơ sức khỏe ban đầu: `SUC_KHOE`;
  - nội dung nguyện vọng: `NGUYEN_VONG`. Với Hành chính và Kế toán là `CHI_CO_HAY_KHONG`, nên DTO chỉ còn `coNguyenVong` (FR-023e).
- **Nhận xét đánh giá** không nằm trong DTO hồ sơ. Nó chỉ trả qua endpoint đánh giá, có khóa `(DANH_GIA_MUC_CHAM_SOC, XEM)`. Nhân viên chăm sóc có P ở dòng "Dị ứng, bệnh nền" nhưng "—" ở dòng "Đánh giá", nên thấy dị ứng mà không thấy nhận xét (FR-008, SC-012).
- **Lưu ý chăm sóc** (FR-009, FR-038) mang nhóm `CONG_KHAI_NOI_BO`, nên mọi nhân viên được xem hồ sơ đều thấy. Với tài khoản người thân, controller trả DTO `HoSoChoNguoiThan` không có trường này.
- **Chú thích ¹** (bản đồng ý, BR-M01-08, DBR-03): M01 cài `DieuKienChuThich("1")`. Bean kiểm có bản đồng ý `HIEU_LUC` nào của người cao tuổi chứa `nguoiThanId` của người xem không. Kiểm ở mỗi yêu cầu, không đệm (002 A-07), nên rút lại đồng ý có hiệu lực từ lần truy cập kế tiếp (FR-015, SC-008).
- **Phạm vi P**: tra cứu danh sách lọc bằng `PhamViApi.boLoc` (002 FR-035). Xem một hồ sơ ngoài phạm vi trả 404 (000 R-03).

**Lý do**: Constitution VI, XI. Dùng nguyên cơ chế của 002 để chỉ còn một nơi quyết định quyền.

**Phương án đã cân nhắc**: Thêm nhóm `KET_QUA_DANH_GIA` vào `NhomThongTin`. Bị loại vì danh sách nhóm của 002 cố định theo 19.3. Tách endpoint đạt cùng kết quả mà không đổi 002.

---

## H-06. Bản đồng ý chia sẻ dữ liệu

**Quyết định**:
- **Tối đa một bản Hiệu lực** (FR-013): cột sinh `dang_hieu_luc = IF(trang_thai = 'HIEU_LUC', nguoi_cao_tuoi_id, NULL)` có `UNIQUE`. Ghi nhận bản mới khi đã có bản Hiệu lực thì làm trong một transaction, theo thứ tự: khóa dòng người cao tuổi, chuyển bản cũ `DA_RUT_LAI` với lý do "được thay thế" và `thay_the_boi_id`, rồi thêm bản mới.
- **Phạm vi người thân**: bảng con `ban_dong_y_nguoi_than` chỉ thêm, ghi cùng lúc với bản đồng ý. Đổi phạm vi là ghi bản mới (US5 #6).
- **Người ký thay** (FR-011, sửa 2026-10-18 theo Q-348): `nguoiDongYLoai` là `NGUOI_CAO_TUOI` hoặc `DAI_DIEN_PHAP_LUAT`. Với `DAI_DIEN_PHAP_LUAT`: bắt buộc số, ngày, Tòa án và tệp scan quyết định của Tòa án (`CHECK` ở cơ sở dữ liệu, 422 `THIEU_QUYET_DINH_TOA_AN` ở API); người ký là người thân có quan hệ Hiệu lực với người cao tuổi, kiểm bằng `QuanHeNguoiThanApi.thuocNguoiCaoTuoi` (409 `NGUOI_KY_KHONG_HOP_LE`). Không gọi `laNguoiDaiDienHieuLuc`: vai trò "người đại diện" của 012 không cho quyền ký thay. Bỏ `lyDoDaiDien`. Người cao tuổi không tự ký được mà chưa có quyết định của Tòa án: không có đường ghi (Q-355, chốt 2026-10-19); hồ sơ tiếp tục thiếu bản đồng ý và chịu nhắc FR-012.
- **Mục "nhận thông báo"** (Q-353): `dongYThongBao` giữ là một giá trị cho cả bản; 009 dùng nó để lọc người nhận thông báo không khẩn có nội dung về người cao tuổi (plan 009). M01 không đổi cách lưu.
- **Rút lại** (FR-014, FR-014a): `POST /ban-dong-y/{id}/rut-lai`, bắt buộc lý do và tệp bằng chứng. Guard H-04 cho phép lệnh này trên hồ sơ trạng thái cuối. Ghi nhận bản mới trên hồ sơ trạng thái cuối bị chặn.
- **Bằng chứng**: tham chiếu `tep_id` bắt buộc (H-13).
- **Sự kiện** `BanDongYDaDoi(nctId, banHieuLucId hoặc rỗng)` phát sau commit. 012 dùng để tính lại bản tin, 009 dùng để lọc người nhận. Quyền xem sức khỏe không phụ thuộc sự kiện này, vì chú thích ¹ được kiểm trực tiếp ở mỗi yêu cầu (H-05).
- **Giá trị cho 009, 012** (FR-010): `BanDongYApi.hienHanh(nctId)` trả phạm vi người thân, `dongYHinhAnh`, `dongYThongBao`.

**Lý do**: DBR-03, FR-013 → FR-016.

**Phương án đã cân nhắc**: Một bản đồng ý sửa phạm vi tại chỗ. Bị loại vì FR-016 cấm sửa nội dung.

---

## H-07. Mục sức khỏe và kiểm tra dị ứng

**Quyết định**:

- **Bất biến nội dung** (DBR-04, FR-019): `muc_suc_khoe` là nhóm 2 nhưng nội dung không đổi được. Có hai trigger:
  - `BEFORE UPDATE` chặn khi bất kỳ cột nội dung nào đổi (`loai`, `di_nguyen_id`, `noi_dung_van_ban`, `muc_do_id`, `nguon_id`, `nguoi_ghi_id`, `thoi_diem`). Chỉ cho đổi các cột của lệnh Loại trừ và hai cờ kiểm tra;
  - `BEFORE DELETE` theo mẫu `trg_<bang>_khong_xoa` của 000 (chỉ tài khoản `eldercare_luu_tru`).
- **Loại trừ**: `POST /muc-suc-khoe/{id}/loai-tru` (lý do bắt buộc). Chuyển `HIEU_LUC → DA_LOAI_TRU` một chiều; trigger chặn chuyển ngược.
- **Danh mục** (nhóm 1, mẫu 000 data-model 1.1):
  - `di_nguyen`: nhóm `THUOC_HOAT_CHAT`, `THUC_PHAM`, `KHAC`. Quyền ghi theo nhóm: `THUOC_HOAT_CHAT` cho người có C ở "Danh mục thuốc" (Quản lý viện, 006 FR-002); `THUC_PHAM` cho Bác sĩ (011 FR-003). Nhóm `KHAC` chưa có nguồn quy định người ghi (plan, điểm báo lại 3); tạm cho Quản lý viện.
  - `muc_do_di_ung`, `nguon_thong_tin` (BR-M01-11): Quản lý viện cấu hình; giá trị khởi tạo nạp bằng migration.
- **Dị ứng loại "khác"** (FR-018a): `di_nguyen_id IS NULL` và có `noi_dung_van_ban`. Cột sinh `khong_kiem_tra_tu_dong` luôn đi kèm khi trả mục ra ngoài, kể cả qua `MucSucKhoeApi` cho thẻ thông tin khẩn cấp (007). Phản hồi tạo mục có cảnh báo `DI_UNG_KHONG_KIEM_TRA_TU_DONG`.
- **Cảnh báo trùng** (FR-021): trước khi thêm, tìm mục `HIEU_LUC` cùng `di_nguyen_id`, hoặc cùng loại và cùng nội dung (so khớp như H-04). Có trùng thì trả 409 `MUC_SUC_KHOE_TRUNG`; client gửi lại với `xacNhanTrung = true`.
- **Kiểm tra lại đơn thuốc, chế độ ăn** (FR-020, BR-M01-07, BR-M01-12):
  - Lệnh ghi dị ứng từ danh mục đặt `cho_kiem_tra_thuoc = cho_kiem_tra_an = true` và phát `DiUngDaGhiNhan(mucId, nctId, diNguyenId)` qua outbox. Mục được lưu dù 006, 011 có chạy hay không.
  - Listener của 006 và 011 chạy kiểm tra, tạo cảnh báo xung đột qua 007, rồi gọi `MucSucKhoeApi.daKiemTra(mucId, NguonKiemTra.THUOC | AN)` để gỡ cờ của mình.
  - Listener lỗi thì outbox giao lại. Job `ThuLaiKiemTraDiUng` (H-12) phát lại sự kiện cho mục còn cờ quá 15 phút, tới khi cả hai cờ được gỡ.
  - Trong lúc còn cờ, API trả `choKiemTra = true` để hồ sơ và thẻ thông tin khẩn cấp hiện dấu.
- **Sự kiện khác**:
  - `DiUngDaLoaiTru(mucId, nctId)`: 011 tính lại suất đặc biệt, đối chiếu lại đồ ăn gia đình; 007 đóng nguồn cảnh báo xung đột (FR-020).
  - `DiUngKhacDaGhiNhan(mucId, nctId)`: 011 đưa người đó vào danh sách cần đối chiếu (011 FR-036a).

**Lý do**: BR-M01-12 cho phép mục được lưu khi kiểm tra chưa chạy được. Vì vậy kiểm tra phải tách khỏi transaction ghi mục; outbox cùng job thử lại bảo đảm kiểm tra cuối cùng sẽ chạy.

**Phương án đã cân nhắc**:
- Gọi đồng bộ 006, 011 trong transaction: lỗi ở đó làm mất mục dị ứng, ngược BR-M01-12.
- Lưu cờ "chờ kiểm tra" ở bảng riêng: thêm một bảng mà không thêm thông tin; hai cột cờ là dữ liệu kỹ thuật (xem plan, Complexity Tracking).

---

## H-08. Hồ sơ sức khỏe ban đầu

**Quyết định**:
- Bảng `ho_so_suc_khoe_ban_dau`, mỗi người cao tuổi tối đa một dòng (`UNIQUE nguoi_cao_tuoi_id`). Đây là nhóm 3 có bản nháp: `PUT` sửa được khi `thoi_diem_xac_nhan IS NULL`; Bác sĩ `POST …/xac-nhan`; trigger `trg_ho_so_suc_khoe_ban_dau_khong_sua` chặn sửa sau xác nhận (000 data-model 1.4).
- Đính chính sau xác nhận: M01 cài `DinhChinhDuocProvider` cho loại `M01.HO_SO_SUC_KHOE_BAN_DAU`:
  - `tangKhuVuc` trả rỗng, vì bản ghi không gắn tầng; đi cách (a) người ghi gốc, hoặc (c) yêu cầu phê duyệt;
  - trên hồ sơ trạng thái cuối, `choPhep` chỉ cho cách (c) (FR-049a).
- Không là điều kiện chặn Hoàn tất tiếp nhận (FR-017), nên không có bean điều kiện.
- "Thuốc đang sử dụng khi tiếp nhận" là văn bản. 006 đọc qua `HoSoSucKhoeApi.banDau(nctId)` khi lập phiếu đối chiếu thuốc.

**Lý do**: FR-017, 000 FR-026.

---

## H-09. Phiếu nguyện vọng cuối đời

**Quyết định**:
- **Phiên bản** (DBR-34, FR-023b): bảng `nguyen_vong_cuoi_doi` chỉ thêm phiên bản. Cột sinh `dang_hieu_luc` có `UNIQUE` (tối đa một phiên bản Hiệu lực). Ghi phiên bản mới thì làm trong một transaction: chuyển phiên bản cũ sang `DUOC_THAY_THE`, đặt `thay_the_boi_id`, rồi thêm phiên bản mới. Trigger chỉ cho `UPDATE` hai cột `trang_thai`, `thay_the_boi_id`, và chỉ theo chiều `HIEU_LUC → DUOC_THAY_THE`.
- **Người ký** (FR-023c, Q-216):
  - phiếu ghi `bac_si_nhan_dinh_id`, phải có vai trò AC-03 lúc ghi;
  - (sửa 2026-10-18, Q-348) Bác sĩ ghi nhận định cho chính phiếu: `tuQuyetDuoc`, thời điểm, `cachHoTroHieu`, `ngayXemLaiNhanDinh` (bắt buộc cả ba);
  - `tuQuyetDuoc = true` thì người ký phải là người cao tuổi;
  - `false` thì người ký là người đại diện của người bệnh (`DAI_DIEN_NGUOI_BENH`, kèm `cachChonDaiDien` = `NCT_DA_CHON` hoặc `GIA_DINH_CHON`) hoặc người đại diện theo pháp luật (`DAI_DIEN_PHAP_LUAT`, kèm số và tệp quyết định của Tòa án). Cả hai là người thân có quan hệ Hiệu lực, kiểm bằng `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`; không dùng `laNguoiDaiDienHieuLuc`;
  - sai thì trả 409 `NGUOI_KY_KHONG_HOP_LE`;
  - tới ngày xem lại, job `NhacXemLaiNhanDinh` nhắc một lần (FR-023i, Q-360; H-12); phiếu bị thay trước ngày đó thì không nhắc.
- **Ý kiến khác** (FR-023c, FR-023g): mỗi ý kiến khác lựa chọn là một dòng `nguyen_vong_y_kien` có nguồn (H-16), ghi cùng phiên bản; không gộp vào `ghiChu`.
- **Nguồn phiên bản**: `TIEP_NHAN`, `YEU_CAU_GIA_DINH`, `XAC_NHAN_LAI_NGUY_KICH`. Nguồn cuối do 007 gọi `NguyenVongApi.ghiPhienBan(...)` khi ghi kết quả xác nhận lại (BR-M05-16).
- **Hiển thị**: theo H-05, nhóm `NGUYEN_VONG`.
- **Cho 007**: `NguyenVongApi.hieuLuc(nctId)` để dùng trên thẻ thông tin khẩn cấp và cảnh báo nguy kịch (FR-023).
- **Nhắc khi thiếu** (FR-023d): theo H-12.

**Lý do**: FR-023a → FR-023f.

---

## H-10. Lần đánh giá và mức chăm sóc

**Quyết định**:

- **Bảng `danh_gia`** là nhóm 3 có bản nháp. Các ràng buộc:
  - cột sinh `nhap_dang_mo = IF(trang_thai = 'NHAP', nguoi_cao_tuoi_id, NULL)` có `UNIQUE`: tối đa một bản Nháp (BR-M01-13);
  - cột sinh `dau_vao_da_xac_nhan = IF(loai = 'DAU_VAO' AND trang_thai = 'DA_XAC_NHAN', nguoi_cao_tuoi_id, NULL)` có `UNIQUE`: tối đa một đánh giá đầu vào Đã xác nhận (FR-034);
  - `CHECK (trang_thai <> 'DA_XAC_NHAN' OR (nhan_xet IS NOT NULL AND muc_cham_soc IS NOT NULL))` (DBR-05, FR-028);
  - trigger chặn mọi `UPDATE` khi trạng thái cũ là `DA_XAC_NHAN` hoặc `DA_HUY`.
- **Nhu cầu chăm sóc**: bảng con `danh_gia_nhu_cau`. Trigger chặn thêm hay xóa khi lần đánh giá cha không còn Nháp. Không đòi lý do khi chọn (FR-029).
- **Lệnh**:

  | Lệnh | Người thực hiện | Kiểm tra chính |
  | --- | --- | --- |
  | Tạo nháp | AC-03, AC-04 | Trạng thái người cao tuổi theo FR-035; đánh giá lại ở Đang tiếp nhận cần đầu vào đã xác nhận |
  | Sửa nháp | Người tạo hoặc AC-03; `If-Match` | Chưa xác nhận |
  | Xác nhận | Chỉ AC-03 (`DANH_GIA_MUC_CHAM_SOC, DUYET`) | FR-028 |
  | Hủy nháp | Người tạo hoặc AC-03; lý do | — |

- **Tham khảo khi mở lần đánh giá** (FR-024): `GET /danh-gia/{id}` trả kèm hồ sơ sức khỏe ban đầu, các mục Hiệu lực và chỉ số gần nhất. Chỉ số lấy qua `ChiSoApi.ganNhat(nctId)` của 007. Không có trường điểm hay mức đề xuất.
- **Tác động khi xác nhận**, trong cùng transaction:
  1. **Đầu vào, hoặc đánh giá lại khi Đang tiếp nhận** (FR-033, FR-035): đặt `nguoi_cao_tuoi.muc_cham_soc_hien_hanh`; thêm dòng `lich_su_muc_cham_soc` (căn cứ: lần đánh giá, hiệu lực từ thời điểm xác nhận); phát `DanhGiaDauVaoDaXacNhan` cho 004 (004 FR-025b).
  2. **Đánh giá lại khi đã lưu trú, khác mức hiện hành** (FR-036): không đổi mức. Gọi `YeuCauDoiMucApi.thayTheBoiDanhGia(nctId, danhGiaId)` của 004, chạy MANDATORY trong cùng transaction. Hàm này chuyển yêu cầu đổi mức chưa kết thúc (Nháp, Chờ duyệt, Đã duyệt) sang `DUOC_THAY_THE` (FR-036a). Sau đó bắt đầu nhắc Hành chính (H-12) và phát `CanLapYeuCauDoiMuc` cho 009.
  3. **Đánh giá lại trùng mức hiện hành**: vẫn gọi `thayTheBoiDanhGia` (FR-036a). Không báo Hành chính.
  4. **Mức hoặc tập nhu cầu khác lần Đã xác nhận trước** (FR-043, Q-296): phát `DanhGiaDaDoiMucHoacNhuCau` cho 005.
  5. **Mọi lần đánh giá lại**: các căn cứ đã báo trước thời điểm xác nhận được coi là đã đáp ứng (H-11).
- **Mức chăm sóc hiện hành đổi theo yêu cầu thay đổi lưu trú** (FR-052): handler áp dụng của 004 gọi `MucChamSocApi.apDungThayDoi(nctId, muc, ngayHieuLuc, yeuCauId)`, chạy MANDATORY. Hàm này đặt mức hiện hành và thêm dòng lịch sử (căn cứ: yêu cầu). Không có endpoint sửa mức trực tiếp.
- **Tra mức tại một ngày** (FR-053): `GET /nguoi-cao-tuoi/{id}/muc-cham-soc?ngay=` lấy dòng lịch sử có `hieu_luc_tu` lớn nhất mà `<=` cuối ngày đó. Loại hình lưu trú hiện hành và lịch sử của nó lấy từ hợp đồng qua `HopDongApi` của 004; M01 không lưu bản sao (FR-052).
- **Đính chính** (FR-032, FR-049a): provider `M01.DANH_GIA`.
  - `nguoiGhiGoc` là bác sĩ xác nhận; `tangKhuVuc` rỗng.
  - `sauKhiDinhChinh`: chỉ khi bản bị đính chính là lần Đã xác nhận gần nhất, hồ sơ chưa ở trạng thái cuối, và mức trong bản đính chính khác mức gốc, thì báo đánh giá lại với căn cứ `DINH_CHINH_DANH_GIA`.
  - Lưu ý chăm sóc hiển thị lấy qua `DinhChinhApi.hienHanh` (FR-038).
- **Lưu ý chăm sóc cho thẻ** (FR-038): `DanhGiaApi.luuYHienHanh(nctIds)` trả theo lô cho 005, 007. Giá trị lấy từ lần Đã xác nhận gần nhất, sau đính chính nếu có. Lần đó để trống thì trả rỗng, không lùi về lần cũ hơn.

**Lý do**: DBR-05, BR-M01-09, BR-M01-13. Dùng cột sinh và `CHECK` để quy tắc đếm và quy tắc bắt buộc có thêm một lớp bảo vệ ở cơ sở dữ liệu.

**Phương án đã cân nhắc**:
- Tính mức hiện hành mỗi lần đọc từ lịch sử, không lưu cột: được, nhưng mức được đọc rất nhiều (003, 008, 010). Cột `muc_cham_soc_hien_hanh` cùng lịch sử, cập nhật trong cùng transaction, đơn giản hơn. Test tích hợp kiểm cột luôn bằng dòng lịch sử mới nhất.
- M01 tự chuyển yêu cầu của 004 sang Được thay thế qua `YeuCauPheDuyetApi` của 000: bị loại vì loại yêu cầu `M02.THAY_DOI_LUU_TRU` thuộc 004. 004 mới biết yêu cầu nào là "đổi mức chăm sóc".

---

## H-11. Báo đánh giá lại

**Quyết định**:

- **Không có bản ghi yêu cầu** (Q-307). M01 giữ bảng kỹ thuật `lan_bao_danh_gia_lai`, ghi mỗi lần hệ thống báo bác sĩ. Đây là nhật ký thông báo của chính module, giống `lan_chay_lich` của 000. Không có lệnh, không có trạng thái nghiệp vụ, người dùng không tạo được.
- **Căn cứ và nguồn**:

  | Căn cứ | Nguồn | Khóa chống trùng |
  | --- | --- | --- |
  | `DINH_KY` | Job, khi lần Đã xác nhận gần nhất đã qua CFG-M01-02 (FR-039) | `(nct, DINH_KY, danh_gia_id)` |
  | `SAU_SU_CO` | Sự kiện `SuCoDaGhiNhan` của 007, khi sự cố là ngã hoặc mức ≥ Trung bình (FR-040) | `(nct, SAU_SU_CO, su_co_id)` |
  | `TRO_VE_BENH_VIEN` | Tác động của lệnh Ghi nhận trở về từ Điều trị tại bệnh viện | `(nct, TRO_VE_BENH_VIEN, lich_su_trang_thai_id)` |
  | `DINH_CHINH_DANH_GIA` | `sauKhiDinhChinh` (H-10) | `(nct, DINH_CHINH_DANH_GIA, dinh_chinh_id)` |

- **Trạng thái tính được** (FR-041 → FR-043):
  - "Cần đánh giá lại": có lần báo có `thoi_diem_bao` sau thời điểm xác nhận của lần đánh giá lại Đã xác nhận gần nhất.
  - Hạn = `min(han_tinh_tu)` của các lần báo chưa đáp ứng, cộng CFG-M01-03. Khi người cao tuổi đang Điều trị tại bệnh viện, `han_tinh_tu = NULL`. Lệnh Ghi nhận trở về đặt `han_tinh_tu` bằng thời điểm trở về cho mọi lần báo chưa đáp ứng (Edge Case nằm viện).
  - "Quá hạn đánh giá lại" có từ thời điểm hạn, cung cấp cho 016 qua `DanhGiaLaiApi.trangThai(nctIds)` (FR-041).
- **Căn cứ mới khi đang cần đánh giá lại** (FR-042): thêm một dòng mới và báo thêm, không đổi hạn, vì hạn lấy `min`.
- **Trạng thái cuối** (FR-043): job và listener bỏ qua người cao tuổi ở trạng thái cuối; truy vấn trạng thái tính được trả "không áp dụng".

**Lý do**: Spec cần thời điểm báo để tính hạn (FR-041). Q-307 cấm bản ghi yêu cầu, không cấm lưu lần thông báo; 000 cũng cho job ghi "nhật ký, lần chạy, thông báo".

**Phương án đã cân nhắc**: Đọc thời điểm báo từ bảng thông báo của 009. Bị loại vì M01 sẽ phải đọc dữ liệu của module khác (constitution X), và hạn sẽ phụ thuộc vào việc gửi thành công.

---

## H-12. Bộ lập lịch của 001

**Quyết định**: Bảy job dùng `@Scheduled` cùng ShedLock, chu kỳ kỹ thuật 15 phút như 000 R-10. Mốc nghiệp vụ đọc từ `ThamSoApi` mỗi lần chạy. Mỗi lần chạy có một dòng `lan_chay_lich`.

| Job | Quy tắc | Việc làm | Chống lặp |
| --- | --- | --- | --- |
| `BaoDanhGiaLaiDinhKy` | FR-039, BR-M01-02 | Thêm `lan_bao_danh_gia_lai` (DINH_KY), phát sự kiện báo bác sĩ | `UNIQUE` ở H-11 |
| `CanhBaoQuaHanDanhGiaLai` | FR-041 | Cảnh báo bác sĩ một lần mỗi người mỗi lần quá hạn | Cột `da_canh_bao_qua_han_luc` |
| `NhacThieuDongYNguyenVong` | FR-012, FR-023d, SC-013 | Người ở Đang lưu trú, Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện mà thiếu bản đồng ý (hoặc phiếu) Hiệu lực: nhắc Hành chính (hoặc Bác sĩ, Điều dưỡng phụ trách) mỗi CFG-M01-06 | `nhac_chu_ky.lan_nhac_cuoi` |
| `NhacLapYeuCauDoiMuc` | FR-036 | Lần đánh giá lại gần nhất khác mức hiện hành và 004 báo không có yêu cầu đổi mức chưa kết thúc: nhắc Hành chính mỗi CFG-M03-02 | `nhac_chu_ky` |
| `CanhBaoQuaHanTroLai` | FR-050, BR-M01-01 | Lượt vắng đang mở (004 `LuotVangApi.dangMo()`) đã quá mốc dự kiến cộng CFG-M01-01: cảnh báo Trưởng tầng và Hành chính | `canh_bao_tro_lai (luot_vang_id, moc_du_kien)` |
| `NhacXemLaiNhanDinh` (bổ sung 2026-10-18) | FR-023i, Q-360 | Phiếu nguyện vọng Hiệu lực có `ngay_xem_lai_nhan_dinh` ≤ hôm nay, hồ sơ chưa ở trạng thái cuối: nhắc một lần Bác sĩ đã nhận định (`NhanVien(bacSiNhanDinhId)`, không thay) và Điều dưỡng phụ trách, mức Nhẹ | `nhac_chu_ky` loại `XEM_LAI_NHAN_DINH`, `moc_id` = id phiếu |
| `ThuLaiKiemTraDiUng` | BR-M01-12 | Phát lại `DiUngDaGhiNhan` cho mục còn cờ quá 15 phút | Listener của 006, 011 idempotent theo `mucId` |

- Các job chỉ chuyển trạng thái theo thời gian, nhắc, cảnh báo (000 FR-010a). Bảng job được ghi là bảng kỹ thuật: `lan_bao_danh_gia_lai`, `nhac_chu_ky`, `canh_bao_tro_lai`. ArchUnit của 000 kiểm gói `m01.lich` không gọi repository tạo bản ghi nghiệp vụ.
- Gia hạn dự kiến trở lại (004 FR-049) tạo mốc mới, nên `canh_bao_tro_lai` có khóa mới và cảnh báo được tính lại (Edge Case).
- NFR-04: với 300 người cao tuổi, mỗi job chỉ là vài truy vấn có chỉ mục, xong trong vài giây.

**Lý do**: Constitution XIII; NFR-04 (chạy lại không gửi trùng).

**Phương án đã cân nhắc**: Một job tổng cho M01. Bị loại vì khó đọc nhật ký lần chạy và khó kiểm thử riêng từng quy tắc.

---

## H-13. Tệp đính kèm (dùng chung)

**Quyết định**: Plan 000 chưa có cơ chế tệp. 001 là feature đầu tiên cần tệp: ảnh người cao tuổi, bản scan bản đồng ý, bằng chứng rút lại, phiếu nguyện vọng, giấy tờ qua đời. Vì vậy 001 chốt một thành phần kỹ thuật dùng chung `com.cambae.backend.common.tep`; đã ghi bổ sung vào hợp đồng của 000.

- **Lưu nội dung** *(Sửa 2026-10-19, chủ dự án chọn AWS S3)*: trong bucket AWS S3 riêng tư (chặn mọi truy cập công khai, mã hóa phía máy chủ SSE-S3, bật versioning), khóa đối tượng là UUID; MySQL chỉ giữ siêu dữ liệu ở bảng `tep`. Dịch vụ tệp gọi S3 qua AWS SDK for Java v2 (`software.amazon.awssdk:s3`, bean `S3Client`) để tải lên, tải xuống, xóa và xử lý kết quả hoặc lỗi trả về. Tên bucket, region đọc từ cấu hình (`AWS_REGION`, `ELDERCARE_S3_BUCKET`); thông tin đăng nhập theo chuỗi mặc định của SDK (biến môi trường, hồ sơ AWS hoặc IAM role), không ghi vào kho mã.
  - **Region và pháp lý: chờ chủ dự án chốt.** AWS chưa có region ở Việt Nam; lưu bản scan, ảnh, giấy tờ sức khỏe ở region nước ngoài là chuyển dữ liệu cá nhân nhạy cảm ra nước ngoài theo Luật Bảo vệ dữ liệu cá nhân 91/2025/QH15 và Nghị định 356/2025/NĐ-CP (cùng nhóm Q-03, 24.4). Xem plan, "Điểm cần báo lại".
  - Tải xuống vẫn đi qua backend (`GET /tep/{id}`): kiểm quyền bằng `QuyenTepProvider` mỗi lần rồi chuyển nội dung từ S3; không phát URL ký sẵn (presigned) vì URL dùng lại được sau khi người xem mất quyền.
  - Test không cần tài khoản AWS: test dùng bản giả của interface `S3Client` (lưu trong bộ nhớ) đặt ở `BT/support/`; chưa dùng Docker nên không chạy LocalStack, MinIO (000 R-14).
- **Bảng `tep`** (kỹ thuật, ghi một lần): `id`, `uuid`, `ten_goc`, `kieu_mime`, `kich_thuoc`, `sha256`, `tai_len_boi`, `tai_len_luc`, `chu_so_huu_loai`, `chu_so_huu_id`, `gan_luc`.
  - Tệp vừa tải lên chưa có chủ sở hữu.
  - Lệnh nghiệp vụ gắn tệp vào bản ghi trong transaction của lệnh (`TepApi.gan(tepId, loai, id)`). Tệp đã gắn không gắn lại được.
  - Tệp không được gắn sau 24 giờ bị job dọn xóa (xóa đối tượng S3 rồi dòng `tep`). Đây là dữ liệu tạm, không phải bản ghi nghiệp vụ.
- **Giới hạn**: PDF, JPEG, PNG; tối đa 10 MB. Đây là thông số kỹ thuật, không phải mã CFG (cùng lý do với chu kỳ job ở plan 000).
- **Tải xuống**: `GET /tep/{id}` chỉ trả nội dung (đọc từ S3) khi người xem có quyền xem bản ghi chủ sở hữu. Quyền kiểm qua SPI `QuyenTepProvider` do module sở hữu cài. Ví dụ M01 cài cho `M01.BAN_DONG_Y`, `M01.NGUYEN_VONG`, `M01.ANH_NGUOI_CAO_TUOI`.
- **Lưu giữ**: tệp theo hạn của bản ghi chủ sở hữu, qua `LuuTruProvider` của module (000 R-11).

**Lý do**: Spec bắt buộc bằng chứng scan ở nhiều lệnh (FR-010, FR-014, FR-023a, FR-047b). Dữ liệu sức khỏe trong tệp phải chịu cùng quyền như bản ghi (NFR-08).

**Phương án đã cân nhắc**:
- `BLOB` trong MySQL: làm phình bản sao lưu và nhật ký nhị phân.
- Lưu trên ổ đĩa máy chủ (volume của container): phương án của bản trước, thay bằng S3 *(Sửa 2026-10-19, chủ dự án chọn AWS S3)*; `TepApi` của 000 không đổi.
- MinIO tự dựng: phải tự vận hành, sao lưu; chủ dự án chọn dịch vụ S3 của AWS.
- URL ký sẵn để client tải thẳng từ S3: giảm tải backend nhưng bỏ qua kiểm quyền tại lúc tải (NFR-08).

---

## H-14. Thông báo của 001

**Quyết định**: M01 phát sự kiện sau commit; 009 gửi (constitution XIV). Mức theo bảng 009 FR-043b khi có. Với thông báo chưa có trong bảng, plan áp nguyên tắc 3 bậc của 009 FR-043b; spec 001 cần ghi lại các mức này (plan, điểm báo lại 4).

| Sự kiện | Người nhận | Mức | Căn cứ mức | Khóa sự kiện (009 FR-005) |
| --- | --- | --- | --- | --- |
| `NguoiCaoTuoiDaChuyenTrangThai` với Chuyển viện, Ghi nhận qua đời | Người liên hệ chính | Khẩn cấp | Bảng 009 (Q-97) | `(nct, lichSuTrangThaiId)` |
| `CanDanhGiaLai` | Bác sĩ (bác sĩ trực của tầng; toàn viện nếu không có) | Nhẹ | Nhắc chưa quá hạn | `(nct, lanBaoId)` |
| `QuaHanDanhGiaLai` | Bác sĩ | Trung bình | Nhắc leo thang vì quá hạn | `(nct, hanBatDau)` |
| `CanLapYeuCauDoiMuc` (lần đầu) | Hành chính | Trung bình | Cần hành động trong ngày; tương tự Q-302 | `(nct, danhGiaId)` |
| Nhắc lập yêu cầu đổi mức | Hành chính | Nhẹ | Nhắc theo chu kỳ | `(nct, danhGiaId, lanNhac)` |
| Nhắc thiếu bản đồng ý; thiếu nguyện vọng | Hành chính; Bác sĩ, Điều dưỡng phụ trách | Nhẹ | Nhắc theo chu kỳ | `(nct, loai, lanNhac)` |
| `NhacXemLaiNhanDinh` (bổ sung 2026-10-18) | Bác sĩ đã nhận định; Điều dưỡng phụ trách | Nhẹ | FR-023i, Q-360 | `(nguyenVongId)` |
| `TamVangQuaHan` | Trưởng tầng, Hành chính | Trung bình | Cần hành động trong ca | `(luotVangId, mocDuKien)` |
| Yêu cầu đổi mức "Được thay thế" | Người yêu cầu, người duyệt | Nhẹ | Bảng 009 | Do 004 phát |

Nội dung cho kênh ngoài ứng dụng không chứa họ tên hay thông tin sức khỏe (009 FR-019). Thông báo cho bác sĩ chỉ mang loại thông tin "sức khỏe" ở phần nội dung đầy đủ.

**Bổ sung 2026-10-18 (009 FR-001a, Q-353, Q-354)**: thông báo Chuyển viện, Ghi nhận qua đời tới người liên hệ chính khai `doiTuongThongTin = VE_NGUOI_CAO_TUOI` và mã trường hợp Q-354 `CHUYEN_VIEN` hoặc `QUA_DOI` (009 dùng mã khi người nhận nằm ngoài phạm vi bản đồng ý). Các thông báo khác của 001 chỉ gửi nhân viên, không khai đối tượng.

**Lý do**: 009 FR-043a buộc module nguồn nêu mức, nhóm người nhận và khóa sự kiện.

---

## H-15. Kiểm thử

**Quyết định**: Theo 000 R-14 và 002 A-14.
- **Tên test** theo BR hoặc FR, ví dụ `BR_M01_04_chuyenNgoaiBang_tuChoi`, `FR_045_thieuNhieuDieuKien_lietKeDu`.
- **Bean giả** cho mọi `ThamGiaChuyenTrangThai` của 003, 004, 005, 006, 007, 010, 011, 012, 013, 014. Mỗi bean giả trả Đạt hoặc Chưa đạt theo cấu hình test (spec CHK035, CHK045). Có thêm một bean giả ném lỗi ở `thucHienTacDong` để chứng minh rollback (FR-047).
- **Test tích hợp MySQL (000 R-14)**: chạy cột sinh, `UNIQUE`, `CHECK` và trigger của data-model mục 9. Gồm test chứng minh DBR-01, 03, 04, 05, 34 ở tầng cơ sở dữ liệu.
- **Đồng thời**: hai luồng gửi Cho tạm vắng và Chuyển viện trên cùng hồ sơ (FR-048); hai luồng ghi bản đồng ý (FR-013).
- **Đồng hồ giả lập** cho CFG-M01-01, 02, 03, 06, CFG-M03-02 (SC-007, SC-013).
- **Client**:
  - Web (Vitest): màn hồ sơ (ẩn trường theo `truongBiAn`), danh sách điều kiện chưa thỏa, cảnh báo trùng có nút xác nhận.
  - App nhân viên (`flutter_test`): thẻ người cao tuổi hiện lưu ý chăm sóc, dấu "không kiểm tra tự động" và dấu "chờ kiểm tra".
  - Không có BR nào của 001 thực hiện ở client (XV).

---

## H-16. Nguồn của ý kiến và không có dấu năng lực *(bổ sung 2026-10-18; FR-023g, FR-023h; Q-347, Q-348)*

**Quyết định**:
- **Kiểu dùng chung**: `m01.api` công khai record `NguonYKien(LoaiNguonYKien loai, Long nguoiThanId)` với `LoaiNguonYKien = NGUOI_CAO_TUOI | NGUOI_THAN | NHAN_VIEN_QUAN_SAT`, và `NguonYKienApi.kiemTra(long nctId, NguonYKien nguon)`: `NGUOI_THAN` thì người thân phải có quan hệ Hiệu lực với người cao tuổi lúc ghi (qua `QuanHeNguoiThanApi.thuocNguoiCaoTuoi`), ngược lại `nguoiThanId` phải rỗng; sai thì ném lỗi 422 `NGUON_Y_KIEN_KHONG_HOP_LE`.
- **Mẫu lưu**: mỗi bảng ghi ý kiến tự có cột `nguon_loai`, `nguon_nguoi_than_id`, `nguoi_ghi_id`, `thoi_diem` theo data-model mục 6a; nội dung và nguồn không sửa; thay đổi là dòng mới. Hợp đồng REST của mỗi feature chép đúng schema `NguonYKien` (và `NhanDinhTuQuyet` khi cần) của api-001.yaml, vì hợp đồng mỗi feature tự đứng, không `$ref` sang file khác. Nơi dùng: 001 (`nguyen_vong_y_kien`), 004 (ý kiến về việc vào ở, Q-356), 005 (thỏa thuận lựa chọn có rủi ro), 012 (mong muốn về khách thăm, người đón), 014 (sở thích).
- **Không có dấu năng lực** (FR-023h): không cột, không trường, không trạng thái năng lực chung. Nhận định gắn với quyết định tầng 2 (phiếu nguyện vọng ở 001; thỏa thuận ở 005). Test kiến trúc `FR_023h_KhongDauNangLucArchTest` quét `information_schema.COLUMNS` và OpenAPI (data-model mục 6a).

**Lý do**: BR-M01-14 cần một định nghĩa nguồn dùng chung cho năm feature; mỗi module vẫn giữ bảng của mình (constitution X), nên chia sẻ kiểu và lệnh kiểm, không chia sẻ bảng.

**Phương án đã cân nhắc**:
- Một bảng `y_kien` chung ở M01 cho mọi module: module khác phải ghi vào bảng của M01 và M01 phải biết loại đối tượng của từng module; bị loại.
- Chỉ ghi chữ tự do "theo lời ai" trong ghi chú: không kiểm được người thân có quan hệ Hiệu lực và không lọc được theo nguồn; bị loại.

---

## H-17. Liên kết mục ý kiến với phản hồi Mong muốn *(bổ sung 2026-10-18; FR-023g, Q-366)*

**Quyết định**: Mẫu cột nguồn ý kiến có thêm cột tùy chọn `phan_hoi_id BIGINT NULL` (tham chiếu logic, không khóa ngoại chéo module); chỉ bảng có lệnh ghi mục từ phản hồi Mong muốn mới thêm cột (hiện là `so_thich` của 014); 012 gắn `mong_muon_nct` bằng `phan_hoi_y_kien` trong cùng module. Lệnh ghi mục của module sở hữu nhận `phanHoiId` tùy chọn; khi có, cùng transaction ghi sự kiện `YKienDaGhiTuPhanHoi(phanHoiId, moduleNguon, loaiMuc, mucId, nctId, nguon)` qua outbox. 012 nghe và thêm dòng `phan_hoi_y_kien` nếu phản hồi thuộc đúng người cao tuổi và nguồn là `NGUOI_CAO_TUOI`; sai thì bỏ qua và ghi lỗi kỹ thuật.

**Lý do**: Mục nằm ở bảng của module sở hữu (001 mục 6a); 012 không đọc được bảng đó, còn module sở hữu gọi 012 sẽ tạo vòng M04 ↔ M10. Sự kiện giữ một chiều.

**Phương án đã cân nhắc**: 012 gọi API đọc của từng module để kiểm mục (thêm hàm đọc ở mọi module có ý kiến). Lưu liên kết ở 001 (001 không sở hữu các mục này).

