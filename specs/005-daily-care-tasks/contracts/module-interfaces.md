# Giao diện giữa module: Feature 005

### Bổ sung chủ dự án duyệt 06/11/2026 — T024/T019

- `ShiftStaffPort.ongoingShifts(Instant at): List<ShiftView>` trả mọi ca đã công bố chứa `at` theo `[startsAt, endsAt)`, không giới hạn bởi tầng có task/giường. E/008 cài nguồn thật; `FakeShiftStaff` hiện có cài cùng hợp đồng, không tạo fake thứ hai. Thay quyết định hoãn ngày01/11 ở riêng điểm này; không đổi `CareShiftPort` hoặc tự sửa staffing.
- `AlertRequested.residentId` chuyển `long` → `Long`, vắng khi task gắn vị trí; thêm `Optional<CareAlertSource> sourceDetails`. `CareAlertSource(locationType, locationId, floorAreaId, taskTypeId, taskTypeName, plannedAt, windowFrom, windowTo, importance)` mô tả đích và công việc nguồn; `source`, `basisType`, `basisId`, `context` giữ đủ tham chiếu task/căn cứ. Giữ constructor bảy tham số cũ với residentId kiểu long; payload cũ thiếu sourceDetails đọc thành rỗng. D/007 review và nối consumer, không gán id0 hoặc người cao tuổi giả.
- Chủ dự án chọn theo BR-M04-06: công việc Quan trọng quá hạn chỉ thông báo Trưởng tầng. Đã cho phép đồng bộ đúng FR-044(b); không đổi quy tắc Thường/Bắt buộc.

### Payload và bàn giao đã cài trong lượt tiếp tục 06–07/11/2026

- Theo quyết định03/11: `BatchedOverdueTaskReminder(floorAreaId,taskCount,from,to,catchUpAt,context)` thay danh sách `taskIds`; from/to là min/max thời điểm dự kiến của các task mới nhắc trong nhóm. Thông báo có `displayParams.path=/care-tasks/overdue?floorAreaId=…`. Giữ constructor cũ và `JsonCreator` đọc payload outbox cũ; không serialize lại danh sách id vào payload mới.
- `ProlongedPoorEating(residentId,firstMealAt,lastMealAt,mealCount,at,context)` theo quyết định03/11: khoảng từ bữa đầu tới bữa cuối của chuỗi hiện hành (thứ tự plannedAt), gồm ghi nhận EATING phát sinh; không giả mealId. 011 đọc chi tiết qua API kết quả hiện hành theo khoảng, cần D nối consumer. Giữ constructor cũ và đọc outbox mealIds cũ; khoảng không có trên payload cũ dùng at của chính sự kiện cũ khi replay.
- Chủ dự án duyệt06/11: `TaskHandoverView` giữ id/roomId/description/plannedAt/status/level, `residentId` thành `Long`, thêm `locationType`, `locationId`, `floorAreaId` nullable để payload cũ vẫn đọc được. Constructor bảy tham số cũ giữ nguyên. Producer care trả task vị trí không dùng người cao tuổi giả. E/008 phải xử lý residentId vắng trước nghiệm thu nguồn thật.
- Chủ dự án duyệt06/11: `SleepResultVoided(resultId,residentId,date,context)` phát cùng transaction của VOID kết quả ngủ, milestone `CORRECTION:<id>`; date theo ngày dự kiến của task (như SleepResultRecorded). Giữ bản gốc và trạng thái task, vẫn phát CorrectionMade cho quy tắc F. D/007 nối consumer để bỏ giá trị bị hủy khi tính quy tắc ngủ; không dùng valueGroup rỗng/null của SleepResultRecorded làm tín hiệu hủy.

### Ghi chú triển khai Phase 9 (02/11/2026)

- Chủ dự án duyệt bản dịch HTTP `POST /care-results/{id}/correct`, `type/correctedContent/reason`, `AMEND_CONTENT/VOID`, giữ Idempotency-Key và201khôngbody. Không thêm signature API/SPI/event Java. `CareResultCorrectionProvider` cài SPI hiện có cho `M04.CARE_RESULT`; bản gốc vẫn ở care_task, không thay trạng thái hoặc giá trị gốc.
- Nội dung đúng là object giá trị thay thế đầy đủ (`numericValue`, `codeValue`, `jsonValue`, `upperLimitConfirmed`, `note` tùy loại); không cho đổi metadata/resultType/performedAt. Provider kiểm cấu trúc và kiểm theo snapshot loại trong callback có id. Cách A/B dùng recorder/tầng task gốc và kiểm quyền hiện hành; MEASURED_VALUE chuyển007, cách C không được hỗ trợ tại005.
- `CorrectionMade` có sẵn tiếp tục kích hoạt quy tắc F Phase8 sau apply; không phát thêm một sự kiện thay thế hoặc gọi hai lần. AMEND_CONTENT ngủ phát `SleepResultRecorded` với group đã kiểm và milestone CORRECTION:<id>, ngày plannedAt. Chủ dự án chọn giữ gate VOID ngủ chờ D/007 thống nhất biểu diễn, không tự dùng group rỗng/null.
- `BillableCareResultCorrected.newQuantity` vẫn chờ E/010 cùng gate quantity Phase6; chưa phát số lượng suy từ giá trị kết quả. Endpoint care chạy READ_COMMITTED. Đường generic000 mặc định isolation cũ giữ CARE_SOURCE_UNAVAILABLE trước ghi, chờ A thống nhất; đọc chain không bị hạn chế như lệnh ghi. Không coi đường generic/billing/VOID ngủ đã nghiệm thu.

### Ghi chú triển khai Phase 6 (01/11/2026)

- Sáu operation HTTP đã dịch tại plan.md#quyet-dinh-hop-dong-phase-6 mục1–3; mục5 CareTask.resultType chỉ đọc từ snapshot được chủ dự án duyệt bổ sung. CareTaskResponse/Checklist/CareTaskDetail cùng dùng trường này; không thêm chữ ký Java API/SPI.
- CareVitalSignTasks cài VitalSignTaskApi hiện có, callback MANDATORY. Pending giữ mở; callback cùng người/giờ gốc đóng bằng vitalSignRecordId, không chép giá trị đo đầu thành giá trị đã xác nhận. Callback khác người/giờ và phát sinh đo chỉ số chờ D/007 theo lựa chọn chủ dự án; không tự thêm metadata/hợp đồng.
- PublicCareResults cài PublicCareResultApi.results: đọc theo lô, overlay CorrectionApi.current, bỏ VOID, chỉ chia sẻ numericValue/codeValue/jsonValue. 012 chịu trách nhiệm quyền/đồng ý/nhật ký xem. summary trả CARE_SOURCE_UNAVAILABLE khi chưa có công thức đã thống nhất E/012.
- M04.KET_QUA_CONG_VIEC/M04.GHI_NHAN_PHAT_SINH chưa có handler vì payload mục4 chưa duyệt, chờ A. BillableCareResultRecorded.quantity chờ E/010; lệnh billable tạm chặn trước lưu, không phát số lượng tự suy đoán. Không coi các provider/consumer này đã tích hợp nguồn thật.

**Ngày**: 2026-10-09 | **Plan**: [../plan.md](../plan.md)

Hợp đồng trong backend, ở gói `com.eldercare.backend.care.api`. Module khác chỉ dùng các giao diện này, không đọc bảng của 005 (constitution X). Chữ ký viết dạng Java rút gọn để mô tả hợp đồng; mã thật viết ở giai đoạn triển khai.

Chiều phụ thuộc theo research C-02: 005 phụ thuộc 000, 001, 002, 003, 004. Các module 006, 007, 008, 010, 011, 012, 014, 015, 016 phụ thuộc 005. Chỗ 005 cần dữ liệu của module phụ thuộc nó thì 005 khai báo SPI (mục 2.1), module đó cài. Plan của các module này phải giữ đúng chữ ký hoặc ghi rõ thay đổi.

---

## 1. 005 cung cấp

**Khai báo Phase2 ngày 2026-10-27**: chữ ký logic mục1.2–1.10a đã có interface tiếng Anh ở `m04.api`, DTO/enum ở `api.dto`; ánh xạ glossary11.2. Đây là khai báo, chưa có provider nghiệp vụ story. Test consumer import `FakeM04ApiConfig` ở test/demo, tiêm `FakeM04ApiFixture` để đặt Supplier theo interface/method/đủ đối số; chưa đặt thì báo lỗi, không mặc định nhận lệnh. Không thay fake8SPI T002 bằng fake thứ hai.

Mục1.11 đã có28event ở `api.events`; `CareEventContext(sourceType, sourceId, milestone, occurredAt, floorAreaId, recipients)` là metadata kỹ thuật. `milestone` phải ổn định theo sự kiện nghiệp vụ, không đổi thành giờ phát lại. Khóa tính từ lớp+id+hash(namespace+mốc); TaskSourceChanged phân biệt SourceType, nhắc thỏa thuận phân biệt reviewDateType. `CareNotice` cài HasNotificationRequests; nhóm nhận do producer trong story cung cấp đúng C-13. Chưa có producer/listener/outbox story nên khai báo không là bằng chứng đã giao tới007/009/010/011. `RiskAgreementReviewReminder` tương ứng NhacXemLaiThoaThuan của C-18/Q-360, bổ sung vào nhóm sự kiện review vốn chưa có dòng riêng trong bảng.

**Đồng bộ tiếng Anh khi merge 2026-10-28, chủ dự án duyệt**: giữ chữ ký đã có consumer M09: `TaskTypeApi.requirements(long): TaskRequirements`; `ShiftTaskApi.onAbsence(long,long)`, `onResignation(long,Instant)`, `onApprovedLeave(long,Set<Long>)`, `onAssignmentChanged`, `onAssignmentEnded`; `onHandoverConfirmed(long,long,Long,Set<Long>,Instant): HandoverTransferResult`. `nextShiftId` nullable khi chưa có ca sau; không tự mở ca hoặc chuyển null sang id giả. Giữ đủ bốn hàm CarePlanApi. Kiểu NamedInterface api là `checklist`. Đây là đồng bộ khai báo, chưa có bản cài story.

### 1.1. SPI khai báo ở module thấp hơn, M04 cài

| SPI (gói khai báo) | Bản cài của 005 | Dùng bởi |
| --- | --- | --- |
| `ThamGiaChuyenTrangThai` (`resident.api`) | Bean `@Order` sau 004, 003. Không có điều kiện chặn. Tác động theo bảng dưới | 001 H-02 |
| `TrangThaiCoMatBanTruPort` (`admission.api`) | `trangThai(nctId, ngay, buoiId)` đọc `co_mat_ban_tru`: `CO_MAT` (gồm `DA_VE`), `VANG_CO_BAO`, `VANG_KHONG_BAO`; rỗng khi `CHUA_DEN` hoặc `DA_HUY_NGAY_NGHI` | 004 FR-060 *(R-15: `co_mat_ban_tru` → `ho_so_chung`)* |
| `NgoaiTuyenHandler` (`administration.taikhoan.api`) | `M04.KET_QUA_CONG_VIEC`, `M04.GHI_NHAN_PHAT_SINH` (research C-14) | 002 A-11 |
| `DinhChinhDuocProvider` (000) | `M04.KET_QUA_GHI_NHAN` (`tangKhuVuc` = tầng/khu của công việc; `choPhep` sai với kết quả đo chỉ số; `sauKhiDinhChinh` phát sự kiện cho 010 khi có tính phí); `M04.DIEM_DANH_BAN_TRU` (`sauKhiDinhChinh` cập nhật giờ trên dòng có mặt) | 000 R-08 |
| `PhanLoaiTruongProvider`, `LuuTruProvider` (000) | data-model mục 10, 11 | |
| `GanPhamViProvider` (002) | `M04.KE_HOACH`, `M04.CONG_VIEC`, `M04.KET_QUA_GHI_NHAN`, `M04.CO_MAT_BAN_TRU`: trả `DoiTuong` với người cao tuổi và tầng/khu | 002 FR-034 |

**Tác động của bean `ThamGiaChuyenTrangThai`** (trong transaction của 001, `Propagation.MANDATORY`):

| Lệnh | Tác động | FR |
| --- | --- | --- |
| `CHO_TAM_VANG`, `CHUYEN_VIEN`, `DIEM_DANH_ROI_VIEN` | Hủy công việc Chưa đến hạn có thời điểm dự kiến từ thời điểm rời, lý do `VANG_MAT`; công việc Đến hạn, Quá hạn giữ nguyên | FR-020, FR-027, BR-M04-04 |
| `GHI_NHAN_TRO_VE`, `DIEM_DANH_VE` | Phát `CanLapCongViec(nctId, TRO_VE, thoiDiem)` | FR-020 |
| `HUY_GHI_NHAN_ROI_VIEN` | Áp `CongViecVangMatApi.tinhLai` với khoảng đi rỗng | FR-020a |
| `HOAN_TAT_TIEP_NHAN` | Phát `CanLapCongViec(nctId, HOAN_TAT_TIEP_NHAN, thoiDiem)` | FR-017 (CHK008) |
| `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI`, `HUY_TIEP_NHAN` | Bảng FR-003 hai dòng cuối; Chưa đến hạn → Hủy; Đến hạn, Quá hạn → Không thực hiện (Hệ thống, `FR_010_TRANG_THAI_CUOI`); phát `YeuCauDongCanhBao(TRANG_THAI_CUOI)`; không nhắc, không leo thang tiếp | FR-010 |

### 1.2. `KeHoachChamSocApi` (đọc; 007, 011, 012, 016)

```java
Optional<PhienBanKeHoachView> hieuLucTaiNgay(long nctId, LocalDate ngay);   // kèm danh sách mục
Optional<Integer> mucTieuNuocMl(long nctId, LocalDate ngay);               // FR-039; 016 hiển thị
boolean coKeHoachHieuLuc(long nctId);                                      // FR-009
Set<Long> loaiCongViecHienHanh(long nctId);
    // bổ sung theo plan 008: loại công việc trong phiên bản kế hoạch Hiệu lực (008 FR-031)
```

`LoaiCongViecApi` (đọc; bổ sung theo plan 008):

```java
YeuCauLoaiCongViec yeuCau(long loaiCongViecId);   // vai trò yêu cầu, loại giấy phép/đào tạo yêu cầu (FR-038; 008 FR-028a, FR-030)
```

### 1.3. `CoMatBanTruApi` (003, 007, 010, 011, 012, 014, 016)

```java
Optional<CoMatView> theoNgay(long nctId, LocalDate ngay);           // trạng thái, giờ đến/về dự kiến, giờ về hiện hành, giờ thực tế
Optional<LocalTime> gioVeTheoNgay(long nctId, LocalDate ngay);      // FR-018a: nguồn cho 003, 010, 011
List<CoMatView> theoKhoang(long nctId, LocalDate tu, LocalDate den); // 010 tính phí buổi, 016
void doiGioVe(long nctId, LocalDate ngay, LocalTime gioMoi, long dangKyId);  // MANDATORY; 014 đồng ý về muộn (FR-018a)
void traGioVe(long nctId, LocalDate ngay, long dangKyId);                    // MANDATORY; 014 hủy đăng ký trước giờ bắt đầu
void diemDanhVe(long nctId, LocalDate ngay, long banGhiDonId, LoaiVe loai,
                Instant thoiDiem, NguoiDungHienTai nd);                      // MANDATORY; 012 khi ghi bàn giao "Viện đưa về" (Q-295)
```

### 1.4. `CongViecCaApi`: tác động kèm lệnh của 008, 015 (mọi hàm MANDATORY)

```java
void khiVangCa(long nhanVienId, long caId);                       // FR-032, BR-M04-13, Q-91
void khiNghiCoDuyet(long nhanVienId, Set<Long> caIds);            // FR-032 (015 FR-029)
void khiNghiViec(long nhanVienId, Instant tuThoiDiem);            // FR-032, BR-M09-04
void khiDoiPhanCong(long nctId, long caId);                       // FR-024, BR-M04-28
void khiKetThucPhanCong(long phanCongId, long caId);              // FR-032 (008 FR-027; 015 FR-024 b)
KetQuaXacNhan khiXacNhanBanGiao(long banGiaoId, long caTruocId, long caSauId,
                                Set<Long> congViecIdsTrongBanGiao, Instant thoiDiem);
    // FR-047, FR-047a: công việc Thường đang Quá hạn, có trong bàn giao, vẫn là công việc chung chưa ai nhận
    // (hoặc người thực hiện thuộc ca trước) → Không thực hiện "hệ thống đóng sau bàn giao";
    // công việc chưa đóng khác → giao theo phân công ca sau hoặc chung, dấu TON_TU_CA_TRUOC; trả danh sách đã đóng, đã chuyển
```

Mỗi hàm phát `CongViecThanhChung` (Trung bình, Trưởng tầng) khi có công việc thành chung, và `CongViecChungKhongAiDuDieuKien` khi ca không có ai đủ điều kiện.

### 1.5. `CongViecBanGiaoApi` (008; FR-046, BR-M04-07, BR-M09-06)

```java
List<CongViecChoBanGiaoView> chuaDong(long tangKhuVucId, long caId);
    // Đến hạn, Quá hạn và Chưa đến hạn có thời điểm dự kiến trong ca; người cao tuổi, loại, thời điểm, trạng thái,
    // mức, người thực hiện, số lần đã nhắc, cảnh báo liên quan. Không có công việc của người đã ở trạng thái cuối (FR-010)
Map<Long, TrangThaiCongViecView> trangThaiHienHanh(Collection<Long> congViecIds);   // mục bàn giao hiển thị trạng thái hiện hành
void daGhiVaoBanGiao(long banGiaoId, Collection<Long> congViecIds);                  // MANDATORY; ghi lịch sử GHI_VAO_BAN_GIAO
```

Không có hàm lập sẵn mục bàn giao (Q-307).

### 1.6. `NguonCongViecApi` (007, 011, 014; MANDATORY)

```java
void nguonBiHuy(LoaiCanCu loai, long canCuId, LyDo lyDo);   // FR-022: hủy công việc Chưa đến hạn có căn cứ đó (bữa hủy, bữa bỏ vì vắng, đăng ký hủy, lịch đo ngừng)
void nguonBiHuy(LoaiCanCu loai, long canCuId, long nctId, LyDo lyDo);   // như trên, chỉ công việc của một người (bữa của người đó bỏ vì vắng, 011 FR-042); Đổi api 2026-11-12
void nguonDaDoi(LoaiCanCu loai, long canCuId);              // FR-022: chỉ báo Người phụ trách ca, Trưởng tầng; không hủy
```

### 1.7. `CongViecVangMatApi` (014; MANDATORY)

```java
void tinhLai(long nctId, Optional<KhoangThoiGian> khoangCu, Optional<KhoangThoiGian> khoangMoi);  // FR-020a
```

### 1.8. `CongViecDoChiSoApi` (007; MANDATORY)

```java
void daXacNhan(long congViecId, long banGhiChiSoId, Instant thoiDiemDo, long nguoiDoId);  // FR-034 "đo lại để xác nhận"
```

### 1.9. `CongViecThongKeApi` (016; đọc, FR-050 dòng "Gửi 016")

```java
Stream<CongViecBaoCaoView> theoKhoang(PhamViLoc phamVi, Instant tu, Instant den);
    // trạng thái và lịch sử trạng thái, mức, loại, tầng/khu, ca, người thực hiện, người đóng và quy tắc đóng (FR-028),
    // thời điểm dự kiến, thời điểm thực hiện (gồm thời điểm thiết bị), nhãn ghi muộn, nhãn đồng bộ, từng Quá hạn
Stream<CoMatView> coMatTheoKhoang(PhamViLoc phamVi, LocalDate tu, LocalDate den);
Stream<KetQuaView> ketQuaTheoKhoang(long nctId, Set<LoaiKetQua> loai, Instant tu, Instant den);  // 007 FR-039c (ngủ), 014 hồ sơ tinh thần
Set<Long> nguoiDaGhiKetQua(long nctId, Instant tu, Instant den);
    // bổ sung theo plan 007: nhân viên đã ghi kết quả công việc cho người đó trong khoảng, kể cả công việc chung (007 FR-060 c, CHK018)
```

### 1.10. `ThoiKhoaBieuApi` (012 cổng người thân nếu spec 012 cho xem; 016)

```java
List<MucThoiKhoaBieuView> theoNgay(long nctId, LocalDate ngay);   // FR-048
```

### 1.10a. `KetQuaChamSocCongApi` (012; bổ sung theo plan 012, research F-11, F-12)

```java
List<KetQuaNguoiThanView> ketQua(long nctId, Instant tu, Instant den);   // kết quả công việc chăm sóc đã ghi, không có ghi chú nội bộ (012 FR-046 (2))
TongHopAnUongView tongHop(long nctId, LocalDate tu, LocalDate den);      // lượng nước TB (bỏ tỷ lệ ăn 2026-11-09, 012 FR-046); rỗng khi không có dữ liệu
```

012 kiểm quyền xem sức khỏe và ghi nhật ký lượt xem (012 FR-048) trước khi gọi.

### 1.10b. `BaoXemXetKeHoachApi` (007; MANDATORY; chủ dự án duyệt 08/11, xác nhận 09/11/2026)

Tiếng Anh: `care.api.CarePlanReviewApi`. Thay cho việc 005 nghe `SuCoNgaDaGhiNhan`, `LuaChonChamSocGiamNheDaGhi` của 007: chiều mã là 007 → 005 (research C-02; nghe sự kiện 007 tạo vòng care → incident → health → care, thử ngày 05/11).

```java
void suCoNgaDaGhiNhan(long nctId, long suCoId);                                      // fallIncidentRecorded
void luaChonChamSocGiamNheDaGhi(long nctId, String khoaSuKien, Instant thoiDiem);    // palliativeCareChoiceRecorded
```

- 007 gọi trong transaction của lệnh ghi sự cố ngã, lệnh ghi lựa chọn ở lại chăm sóc giảm nhẹ; lỗi ném ra làm lệnh 007 rollback.
- Mỗi lần gọi phát `CanXemXetKeHoach` (mục 1.11) cho điều dưỡng phụ trách (`AssignedNurse`, 009 phân giải) và từng người có quyền duyệt kế hoạch (`ApproverApi`, mỗi sự kiện một tài khoản); không tạo bản ghi nghiệp vụ (Q35). Nguồn `M05.INCIDENT` (id sự cố) và `M05.PALLIATIVE_CARE_CHOICE` (`sourceId` là người cao tuổi, `khoaSuKien` vào mốc và tham chiếu).
- Gọi lặp cùng sự cố, cùng `khoaSuKien` không báo lần hai (`reminder_marker` loại `M04.CARE_PLAN_REVIEW`); hai khóa khác nhau cho hai bộ thông báo khác khóa.
- Không có `LichDoDaDoi`: đổi lịch đo đã đi qua `NguonCongViecApi.nguonDaDoi` (mục 1.6, `TaskSourceApi.sourceChanged`).
- Bản giả chung: bean `CarePlanReviewApi` trong `support/fake/care/FakeM04ApiConfig` (đặt `FakeM04ApiFixture.reply`, chưa đặt thì ném lỗi); test 007 không import cấu hình này thì chạy với bản thật.

### 1.11. Sự kiện 005 phát (sau commit, qua outbox của 000)

| Sự kiện | Người nghe | FR |
| --- | --- | --- |
| `YeuCauCanhBao(loai, muc, nctId, nguon, khoa, loaiCanCu, canCuId)` (thêm `loaiCanCu`, `canCuId` của công việc nguồn theo plan 007; công việc căn cứ `LAN_DO` được 007 ghi với khóa "bỏ lỡ lần đo theo lịch", 007 FR-039b) | 007 (tạo hoặc gộp theo BR-M05-02) | FR-039, FR-040, FR-042, FR-044 c |
| `YeuCauDongCanhBao(congViecId, lyDo)` | 007 | FR-010, FR-035 |
| `NhacQuaHan(congViecId, nguoiNhan)`; `NhacQuaHanGop(nguoiNhan, congViecIds)` | 009 (Nhẹ) | FR-043, FR-052 b, BR-M04-29 |
| `CongViecQuaHanCanXuLy(congViecId, muc, nguoiNhan)` | 009 (Trung bình) | FR-044 a, b |
| `CaChuaCoCongViec(caId, tangKhuVucId)` | 009 (Người phụ trách ca, Trưởng tầng) | FR-017 |
| `CanLapCongViec(nctId, lyDo, tuThoiDiem)` | 009 (Người phụ trách ca của tầng hoặc khu bán trú) | FR-017, FR-018, FR-020, FR-020a |
| `KeHoachDaDoi(nctId, phienBanMoi, phienBanCu)`; `NguonCongViecDaDoi(loai, canCuId, nctId)` | 009 (Người phụ trách ca, Trưởng tầng) | FR-006, FR-022 |
| `PhienBanKeHoachDaGuiDuyet`, `…DaTraLai`, `…DaThuHoi`, `…QuaHanDuyet`, `…BaoQuanLyVien` | 009 | FR-003, FR-005 |
| `CanXemXetKeHoach(nctId, nguon, thamChieu)` | 009 (điều dưỡng phụ trách, người có quyền duyệt) | FR-011 |
| `CongViecThanhChung`, `CongViecChungKhongAiDuDieuKien` | 009 (Trưởng tầng) | FR-032, CHK037 |
| `DaDiemDanhDen(nctId, ngay, gioDen)` | 009 (Người phụ trách ca khu bán trú) | FR-018, SC-009 |
| `VangKhongBao(nctId, ngay)` | 009 (Trưởng tầng) | FR-013 |
| `BanTruConCoMatHetNgay(nctId, ngay)` | 009 (Trung bình Người phụ trách ca đang diễn ra; Nhẹ Trưởng tầng) | FR-019 |
| `TrangThaiCoMatDaXacDinh`, `TrangThaiCoMatDaDoi` (`nctId`, `ngay`, trạng thái, buổi phát sinh) | 010, 011, 004 | FR-016 |
| `KetQuaCoTinhPhi(ketQuaId, nctId, loaiCongViecId, dichVuId, soLuong, thoiDiemThucHien)` | 010 | FR-051, DBR-15 |
| `KetQuaTinhPhiDaDinhChinh(ketQuaId, kieu, soLuongMoi)` | 010 | FR-037, DBR-17 |
| `AnKemKeoDai(nctId, buaIds, thoiDiem)` | 011 | FR-040 |
| `CongViecDoBiBo(congViecId, nctId, lichDoId, chiSoIds, lyDo, mucQuanTrong)` (bổ sung theo plan 007): công việc căn cứ `LAN_DO` đóng Không thực hiện (không tính đóng do trạng thái cuối) hoặc còn Quá hạn khi hết ca; `lyDo` là `KHONG_THUC_HIEN` hoặc `QUA_HAN_HET_CA` | 007 (quy tắc bỏ lỡ lần đo, 007 FR-039b, Q-75) | FR-026, FR-044 |
| `KetQuaNguDaGhi(ketQuaId, nctId, ngay, nhomGiaTri)` (bổ sung theo plan 007): khi ghi hoặc đính chính kết quả ngủ; `ngay` là ngày dương lịch của thời điểm dự kiến | 007 (mất ngủ kéo dài, 007 FR-039c) | FR-034, FR-037 |

Mỗi sự kiện mang khóa `loại + id + mốc` để bên nghe bỏ trùng (NFR-04).

---

## 2. 005 cần từ module khác

### 2.1. SPI 005 khai báo ở `care.api`, module khác cài

**Triển khai T002, 2026-10-26**: chữ ký logic bên dưới được giữ nguyên số phương thức/đối số và ánh xạ sang tiếng Anh trong `m04.api.spi` theo glossary mục 11. Đưa phần khai báo tám SPI lên T002 để fake có interface thật; API khác của T003 vẫn chưa cài. Bản giả duy nhất do 005 sở hữu là `support/fake/m04/FakeM04Config`, import tường minh ở test/demo.

Các view chỉ phục vụ SPI được đặt lồng trong interface. Những view thiết kế chưa liệt kê trường được cụ thể hóa thành projection tối thiểu sau (không thay API dữ liệu của module nguồn):

| SPI | View/kiểu trao đổi khi triển khai |
| --- | --- |
| `ShiftStaffPort` | `ShiftView(id, floorAreaId, startsAt, endsAt, published)`; `CareAssignmentView(staffId, role, primary)`; `HandoverTasksView(receiveAtStart, prepareAtEnd)` là hai việc checklist điều dưỡng theo FR-029 |
| `TaskSourcePort` | `SourceType` tương ứng đủ 12 giá trị LoaiCanCu; `SourceValidation(valid, residentMatches, timeMatches, prescribed)` theo ghi chú kiểm căn cứ và FR-023 |
| `VitalSignPort` | `RecordingResult(status, vitalSignRecordId, reason, remeasurementSessionId)` với status `SAVED`, `BLOCKED`, `AWAITING_REMEASUREMENT`; các id/lý do dùng Optional |
| `MedicationChecklistPort` | `DoseView(doseKey, residentId, prescriptionId, plannedAt, windowFrom, windowTo, status, sharedOnFloor, awaitingHandoverConfirmation, awaitingRecordingAfterReturn, onlineRequired, medicationSource, deliveryId, medication)` giữ trường của 006 mục 1.3; `MedicationSummary(description)` là projection mô tả, không tự khai danh mục thuốc; status/medicationSource là mã do 006 cung cấp |
| `AlertReadPort` | `AlertView(id, residentId)` là định danh/tham chiếu cảnh báo đang mở; không khẳng định đã cung cấp toàn bộ nội dung cảnh báo của 007 |
| `DayCarePickupPort` | `PickupView(id, pickupType)`; nguồn 012 chịu trách nhiệm xác minh điều kiện hợp lệ, `pickupType` là mã nguồn; lỗi dùng lại `BAN_GHI_DON_DA_DUNG` tương ứng `PICKUP_RECORD_ALREADY_USED` |
| `QualityCheckPort` | `FailedCheckView(id, shiftId)`; cặp kết quả/công việc gốc nằm trong đối số truy vấn |
| `TimetableSourcePort` | `TimetableItemView(at, source, description, status)` theo schema MucThoiKhoaBieu; source/status giữ mã nguồn |

`MANDATORY` của `VitalSignPort.record` và `DayCarePickupPort.markUsed` là trách nhiệm provider thật. Fixture bộ nhớ không thay thế kiểm rollback/MySQL hoặc tính điều kiện nghiệp vụ nguồn.

```java
interface NhanSuCaPort {                    // 008 cài
  Optional<CaView> ca(long caId);                                         // tầng/khu, giờ bắt đầu, kết thúc, đã công bố
  List<CaView> caDaCongBo(long tangKhuVucId, Instant tu, Instant den);    // FR-017: Trưởng tầng lập trước
  Optional<CaView> caChua(long tangKhuVucId, Instant thoiDiem);           // ca chứa thời điểm dự kiến
  Optional<Long> nguoiPhuTrachCa(long caId);
  List<PhanCongChamSocView> phanCong(long nctId, long caId);              // nhân viên, vai trò, chính/hỗ trợ (FR-024)
  List<Long> dieuDuongPhuTrach(long nctId, Instant thoiDiem);             // FR-011, FR-040
  boolean coTenTrongCa(long nhanVienId, Instant thoiDiem);                // nhận việc (FR-031), FR-047a
  boolean duDieuKien(long nhanVienId, Set<String> chungChi, LocalDate ngay);  // BR-M09-01
  boolean coNguoiDuDieuKien(long caId, VaiTro vaiTro, Set<String> chungChi);   // CHK037
  boolean banGiaoDaXacNhan(long caTruocId);                               // FR-047a
  ViecBanGiaoView viecBanGiao(long nhanVienId, long caId);                // checklist điều dưỡng (FR-029)
}

interface NguonCongViecPort {               // 007 (LAN_DO, THEO_DOI_SAU_NGA, THEO_DOI_TIEP_XUC, CANH_BAO_UONG_NUOC),
                                            // 011 (BUA), 014 (BUOI_HOAT_DONG, LAM_LAI) mỗi bên một bean
  Set<LoaiCanCu> loaiQuanLy();
  KetQuaKiemTraCanCu kiemTra(LoaiCanCu loai, long canCuId, long nctId, Instant thoiDiemDuKien);
      // hợp lệ, người cao tuổi khớp, thời điểm khớp; với LAN_DO trả "theo chỉ định" để buộc mức Bắt buộc (FR-023)
  default boolean xacNhanPhucVu(long buaId, long nctId) { return true; }   // 011: FR-050 (suất đặc biệt, cần đối chiếu)
}

interface ChiSoPort {                       // 007 cài; MANDATORY
  KetQuaGhiChiSo ghi(long nctId, String loaiChiSo, BigDecimal giaTri, Instant thoiDiemDo,
                     long nguoiDoId, long congViecId, Optional<Instant> thoiDiemThietBi);
      // DA_LUU(banGhiChiSoId) | BI_CHAN(lyDo, CFG-M06-03) | CHO_DO_LAI(phienId) (FR-034, FR-041, BR-M04-10)
}

interface LieuThuocChecklistPort {          // 006 cài; chỉ đọc
  List<LieuHienThiView> theoCa(Collection<Long> nctIds, Instant tu, Instant den);   // FR-029
}

interface CanhBaoDocPort {                  // 007 cài; chỉ đọc
  List<CanhBaoView> dangMo(Collection<Long> nctIds);             // checklist điều dưỡng (FR-029); bàn giao (FR-046)
  Map<Long, List<Long>> theoCongViec(Collection<Long> congViecIds);
}

interface BanGhiDonBanTruPort {             // 012 cài
  Optional<BanGhiDonView> hopLe(long banGhiDonId, long nctId, Instant thoiDiem);
      // người đón hợp lệ hoặc ngoại lệ đón Hiệu lực, hoặc "Tự về" khi dấu "được tự về" có tác dụng (Q-125); loại đón
  void daDung(long banGhiDonId, long diemDanhId);
      // bổ sung theo plan 012: MANDATORY, trong lệnh Điểm danh về; lần hai → BAN_GHI_DON_DA_DUNG
}

interface KetQuaKiemTraPort {               // 014 cài
  Optional<KetQuaKiemTraView> khongDat(long ketQuaKiemTraId, long congViecGocId);   // FR-026a: kèm ca của danh sách kiểm tra
}

interface ThoiKhoaBieuNguonPort {           // 006, 012, 014 mỗi bên một bean; chỉ đọc
  List<MucThoiKhoaBieuView> theoNgay(long nctId, LocalDate ngay);   // FR-048: thời điểm, nguồn, trạng thái
}
```

Bean `NguonCongViecPort` thiếu cho một loại căn cứ thì lệnh lập công việc với căn cứ đó bị chặn `CAN_CU_KHONG_HOP_LE` (mặc định an toàn).

### 2.2. Sự kiện 005 nghe

| Sự kiện | Module | Xử lý | FR |
| --- | --- | --- | --- |
| `DanhGiaDaDoiMucHoacNhuCau` | 001 | `CanXemXetKeHoach` | FR-011, Q-296 |
| `DaChuyenGiuong` | 003 | Gán lại công việc chưa đóng theo phân công khu mới (idempotent theo `phanBoMoiId`) | FR-032 |
| `PhuLucDaApDung` | 004 | Có `MUC_CHAM_SOC`: `CanXemXetKeHoach`; có `LICH_DEN`, `LOAI_LUU_TRU`: cập nhật dòng có mặt từ ngày hiệu lực | FR-011, FR-012 |
| `BaoVangBanTruDaGhi`, `NgayNghiBanTruDaDoi` | 004 | Bảng FR-013 | FR-013, FR-014 |
| `ThamSoDaDoi(CFG-M04-*)` | 000 | Xóa bộ đệm tham số; không tính lại kết quả cũ | Edge Case |

*(2026-11-09, C, chủ dự án duyệt 08/11, xác nhận 09/11)* Bỏ dòng 007 (`SuCoNgaDaGhiNhan`, `LuaChonChamSocGiamNheDaGhi`, `LichDoDaDoi`): hai nguồn đầu thành lệnh 007 gọi (mục 1.10b), đổi lịch đo qua `NguonCongViecApi.nguonDaDoi` (mục 1.6).

### 2.3. Giao diện có sẵn

| Giao diện | Module (feature) | Dùng cho | Khi chưa có |
| --- | --- | --- | --- |
| `NguoiCaoTuoiApi.trangThai`, `theoTrangThai`; `DanhGiaApi.luuYHienHanh`; `TrangThaiNguoiCaoTuoiApi.khoangTrangThai` | M01 (001) | FR-017, FR-025, FR-030, FR-039 | Có sẵn |
| `NguonYKienApi.kiemTra` (001 mục 1.8b); `QuanHeNguoiThanApi.thuocNguoiCaoTuoi` (012) | M01 (001), M10 (012) | FR-002a: nguồn ý kiến, người đại diện của người bệnh (bổ sung 2026-10-18) | Có sẵn sau 001; bản giả cho 012 |
| `ViTriApi.hienTai`, `CoCauViTriApi` | M03 (003) | Thẻ người cao tuổi, tầng của người (FR-030) | Có sẵn |
| `HopDongDocApi.lichDenTaiNgay`, `noiDungTaiNgay` (loại lưu trú) | M02 (004) | FR-012 | Có sẵn |
| `QuyenApi`, `PhamViApi` (`boLoc`, `nguoiPhuTrachCa`, `truongTangDuocGiao`), `NguoiDuyetApi`, `NguoiDungHienTai` | M15 (002) | Research C-16 | Có sẵn |
| `NhatKyApi`, `ThamSoApi`, `DinhChinhApi`, `NhacQuaHan`, `common` | M15 (000) | Mọi lệnh | Có sẵn |
| Bản cài các SPI mục 2.1 | 006, 007, 008, 011, 012, 014 | | Bản giả có công tắc `/api/v1/kiem-thu/gia-lap/{module}` |

## Bản cài nền danh mục ngày2026-10-31

TaskTypeApi.requirements cóproviderthật CareTaskTypeLookup,đọc CareCatalogStore/CatalogApi mỗi lần;chứngchỉ thayđổi áp ngay theoFR-038,khôngcache. CarePlanApi và các API story khác vẫn mới khai báo/fake,chưa cóprovidernghiệpvụ. Không đổi chữkýDTO/APIcôngkhai. ID danh mục đãchuyển giữgiátrịcũ qua attributes.legacy;IDvậtlýcatalog_item làchi tiếtlưutrữ,không rò vàoresponse hoặc thayIDnhậtký.

## Bản cài kế hoạch Phase 3 tại checkout — 2026-10-31

CarePlanApi có nguồn thật CarePlanQueries: effectiveOn đọc các khoảng EFFECTIVE/EXPIRED tại ngày, waterTargetMl lấy mục tiêu duy nhất theo quyết định chủ dự án31/10, hasEffectivePlan và currentTaskTypes đọc ngày nghiệp vụ hiện tại. @Fallback chỉ để fake do005 sở hữu được ưu tiên khi test/demo chủ động import. Không đổi chữ ký DTO/API này. Các API công việc/attendance story sau vẫn còn chờ provider, không coi Phase3 là hoàn tất mọi API005.

HTTP ba danh mục TaskType/ResultValue/NonPerformanceReasonType thêm version chỉ đọc, chủ dự án chọn phương án2 ngày31/10; dùng version từng dòng GET để gửi If-Match khi sửa. 11 operation kế hoạch và12 operation danh mục có x-implementation-phase cho client web; phần chưa dịch chưa được sinh.

**Đổi api được chủ dự án duyệt31/10:** `CarePlanReturned(long planId, long residentId, CareEventContext context, String reason)`. Giữ constructor ba tham số cũ, reason=null cho caller/sự kiện cũ. Producer Trả lại truyền nguyên Reason.value; reason chỉ vào ContentPart HEALTH, title/shortContent giữ nội dung chung. Không thay eventKey, nhóm nhận hoặc mức LOW. Không sửa chữ ký API khác hay hợp đồng HTTP.

**Điều kiện chưa đạt:** lý do hợp lệ2000dấu ngoặc kép tạo JSON4233ký tự với context fixture, vượt EVENT_PUBLICATION.SERIALIZED_EVENT VARCHAR(4000). Chủ dự án chọn chờ A mở rộng outbox trước, không duyệt C đổi schema nền. Bản cài reason trong checkout chưa được nghiệm thu/merge trước khi giải quyết ca lưu payload tối đa; không cắt lý do và không giảm giới hạn hợp đồng. Bàn giao A tại team-plan mục8.

## Bổ sung đã duyệt Phase 4 — 2026-11-01

Chủ dự án trả lời “1” duyệt `plan.md#quyet-dinh-hop-dong-phase-4` mục 1–3. Bảy operation HTTP T025 và các schema đã dịch sang tiếng Anh trong api-005.yaml, giữ method/status/required/header; lỗi theo dòng từ 1.

Bổ sung không đổi chữ ký cũ:

```java
// TaskSourcePort: mỗi nguồn chỉ đọc dữ liệu thuộc managedTypes() trong [from,to).
List<SourceView> sources(long residentId, Instant from, Instant to);
record SourceView(SourceType type, long id, Instant at, String description) {}
// ShiftStaffPort: người còn có mặt trong lịch ca đã công bố.
List<ShiftMemberView> members(long shiftId);
record ShiftMemberView(long staffId, Role role) {}
```

005 sửa đúng FakeTaskSource/FakeShiftStaff hiện có, kể cả reset fixture; không tạo fake thứ hai hoặc production fake. E008 cần cài ShiftStaffPort thật; D007/D011/C014 cài sources trong provider nguồn sở hữu. Kết quả test fixture không phải bằng chứng tích hợp nguồn thật.

T025 đã có CareTaskSources cài TaskSourceApi: MANDATORY, sourceCancelled khóa các cư dân tăng dần rồi dòng công việc, chỉ hủy NOT_YET_DUE thực tế chưa tới đầu khung; việc đã tới hạn được cập nhật theo giờ và giữ để đóng theo FR-027. sourceChanged chỉ phát TaskSourceChanged theo người, không hủy hoặc tạo công việc; milestone chứa residentId để nguồn dùng chung không đụng khóa sự kiện.

Ngoài phần đã duyệt, ongoingShifts (mục 5) và sửa ApiClient mobile của A (mục 6) đang chờ; chưa thêm chữ ký/hay sửa file nền.

**Quyết định tiếp 2026-11-01:** chủ dự án không duyệt triển khai ongoingShifts hoặc cho C sửa ApiClient nền trong lượt này; T024 chờ E, T026 adapter production chờ A. Cảnh báo MANDATORY gắn vị trí chờ thống nhất với 007 vì AlertRequested yêu cầu residentId. Không đổi các hợp đồng này; không dùng id giả. Mục ghi chờ duyệt ở trên là trạng thái trước câu trả lời, được thay thế bằng quyết định hoãn này.

## Cập nhật đã duyệt Phase 5 — 2026-11-01

Chủ dự án chọn “1” cho toàn bộ plan.md#quyet-dinh-hop-dong-phase-5 mục1–3. Đã thêm `DayCareCalendarPort.onDate(residentId,date): CalendarView(holiday,timelyAbsenceId,lateAbsenceId)` và `DayCareAttendanceApi.refreshCalendar(residentId,date)` MANDATORY; giữ chữ ký cũ. FakeDayCareCalendar trong support/fake/care là fixture duy nhất, chưa đặt ngày thì báo lỗi; FakeM04Config đăng ký opt-in, không production fake. HTTP bốn operation/schema/request riêng đã dịch và sinh client bằng npm run gen:api (34operation005 tổng cộng).

Provider thực hiện DayCareAttendanceApi là AttendanceQueries @Fallback; dữ liệu nhóm2 qua SimpleRecordApi, nhóm3 qua RecordEventApi; callback giờ về/checkout/calendar giữ transaction của caller. `inRange` gồm hai ngày biên theo khoảng LocalDate; `returnTimeOn` đọc giờ về hiện hành sau014 thay đổi/khôi phục theo đúng registrationId. Callback checkout012 kiểm bản ghi đón và loại đối chiếu với nguồn012, không nhận loại từ HTTPclient.

**Cách ghép nguồn bắt buộc theo C-02:** care đã dùng ContractReadApi của admission, nên B không cài trực tiếp care.spi hoặc gọi care.api trong module admission. B công bố API/sự kiện004; adapter/listener thuộc care sẽ đọc API và gọi callback đồng bộ cùng transaction. Điều chỉnh câu giao việc “B cài SPI/call callback” trong đề xuất trước; không đổi các chữ ký đã duyệt. Chưa có nguồn/types004 thật để viết adapter, không ngầm ghi đã tích hợp.

**Điều kiện chưa đạt:** callback sau đổi khỏi DAY_CARE đang409, có thể rollback lệnh004/job; VOID chưa chốt, endpoint005 chặn trước tạo request còn generic000 cần A nối capability; retention alias/shared correction chain cần A xử lý; TrangThaiCoMatBanTruPort chưa có kiểu thật của B và mobile transport list còn chờ A. Giữ các gate riêng, không dùng test fake làm chứng cứ production hoặc đóng phase.


**Ghi chú khi merge — ưu tiên listener của B/E (chủ dự án đã chốt):** nếu nhánh ghép đã có listener và kiểu sự kiện nguồn tương ứng do B/E cung cấp, ưu tiên dùng bản của B/E thay cho đường tích hợp tạm đã làm ở005. Đối chiếu và hợp nhất adapter/callback hiện có để mỗi sự kiện chỉ được xử lý một lần; bỏ phần tích hợp tạm bị thay thế khi đã kiểm tương đương. Giữ chữ ký API cũ và bàn giao B/E review trước khi ghép. Chỗ đặt lớp và chiều phụ thuộc vẫn theo C-02; nếu hợp đồng khác nhau thì ghi rõ chênh lệch để review/duyệt, không tự đổi chữ ký hoặc giữ hai listener chạy trùng. Ghi chú này là quy tắc cho lần merge sau, chưa xác nhận B/E đã có listener và chưa thay mã nguồn hiện tại.

## Bản cài Phase 7 — 02/11/2026 (chưa nghiệm thu tích hợp)

Giữ toàn bộ chữ ký công khai. CareHandoverQueries cài HandoverTaskApi đầy đủ005; CareTaskHandoverProjection cài TaskHandoverApi tương thích008. Projection đầy đủ đọc reminderCount từ StatusHistoryApi và alertIds từ AlertReadPort, lọc floor/shift/trạng thái cuối. recordedInHandover dùng marker chống lặp và history có căn cứ M09.HANDOVER. CareTaskHandover cài onHandoverConfirmed MANDATORY; chỉ tự đóng NORMAL OVERDUE còn mở có trong taskIds của bàn giao; người đã nhận lúc chờ được giữ, task còn lại chuyển ca/dấu tồn, xét chứng chỉ hiện hành. nextShiftId null giữ id ca gốc, không tạo id/ca giả, marker bảo đảm replay không đóng lại việc đã nhận.

Chưa đủ để nói008 đã tích hợp: ShiftStaffPort production chờE; primitive residentId của TaskHandoverView cũ không hỗ trợ vị trí (guard503, không id0), E cần thống nhất dùng HandoverTaskApi Optional/adapter trước nghiệm thu đủ nguồn. Năm callback vắng/đổi phân công T038 vẫn503; claim tồn ca sau vẫn chờ quy tắcE đã chọn hoãn. MANDATORY vị trí chờD007; dữ liệu nguồn cho MeasurementTaskMissed quá hạn hết ca (measurementScheduleId/vitalSignIds) chưa có trong SPI đọc nguồn hiện hành, chưa phát payload suy đoán. Không sửa API/DTO nguồnD hoặc coi thiếu event này đã hoàn thành.
