# Bảng thuật ngữ Việt – Anh cho mã nguồn

**Ngày**: 2026-10-16, bổ sung 2026-10-19 (mục 6, feature 000; mục 7, feature 002), 2026-10-21 (mục 15, feature 009) | **Căn cứ**: constitution X (3.1.0), research R-03

Plan, data-model, contracts, tasks dùng tên logic tiếng Việt không dấu. Mã nguồn dùng tên tiếng Anh dịch theo bảng này. Một thuật ngữ chỉ có một bản dịch, dùng giống nhau ở bảng, lớp, endpoint, JSON và màn hình. Thuật ngữ chưa có thì thêm vào bảng trong cùng thay đổi với mã; đổi bản dịch đã dùng thì phải đổi cả mã đã viết.

Mã nghiệp vụ giữ nguyên, không dịch: BR-, FR-, CFG-, DBR-, Q-, UC-, AC-, BF-, NFR-, SC-.

## 1. Cách viết tên

| Loại | Quy ước | Ví dụ (tên logic → mã) |
| --- | --- | --- |
| Gói Java | chữ thường, một từ | `administration.pheduyet` → `administration.approval` |
| Lớp, interface, enum | PascalCase | `YeuCauPheDuyetController` → `ApprovalRequestController` |
| Hàm, biến, trường JSON | camelCase | `lyDo` → `reason` |
| Hằng, giá trị enum | UPPER_SNAKE | `CHO_DUYET` → `PENDING_APPROVAL` |
| Bảng, cột | snake_case, danh từ số ít | `yeu_cau_phe_duyet` → `approval_request` |
| Đường dẫn API | kebab-case, danh từ số nhiều, lệnh là động từ | `/yeu-cau-phe-duyet/{id}/duyet` → `/approval-requests/{id}/approve` |
| Migration Flyway | `V{NNN}_{nn}__snake_case.sql` | `V000_02__nhat_ky.sql` → `V000_02__audit_log.sql` |
| Thư mục web, app | kebab-case (web), snake_case (Dart) | `tinh-nang/tham-so/` → `features/parameters/` |
| Component React | PascalCase `.jsx` | `ThamSoForm.jsx` → `ParameterForm.jsx` |
| File Dart | snake_case | `hop_duyet.dart` → `approval_inbox.dart` |
| Lớp, phương thức test | giữ mã quy tắc ở đầu | `BR_M15_09_noApproverLeft_notifiesDirectorAtReminderTime` |

## 2. Thư mục và gói cấu trúc

| Tên logic | Mã |
| --- | --- |
| `chung` | `common` |
| `tinh-nang` | `features` |
| `nhan-vien` (phần nhân viên của app) | `staff` |
| `nguoi-than` (phần người thân của app) | `family` |
| `nguoi-cao-tuoi` (phần người cao tuổi của app) | `resident` |
| `api`, `internal`, `web` | giữ nguyên |
| `lich` (job định kỳ) | `scheduling` |
| `support` (test) | giữ nguyên |
| gói module M01 … M15 (trước 2026-10-30 là `m01` … `m15`) | `resident` (M01), `admission` (M02), `roombedallocation` (M03), `care` (M04), `incident` (M05), `health` (M06), `medication` (M07), `nutrition` (M08), `staffing` (M09), `family` (M10), `billing` (M11), `belongings` (M12), `notification` (M13), `reporting` (M14), `administration` (M15); research R-02. Mã nghiệp vụ M01 … M15 (BR, loại bản ghi, nhật ký) giữ nguyên |
| `administration.thamso` | `administration.parameter` |
| `administration.nhatky` | `administration.auditlog` |
| `administration.pheduyet` | `administration.approval` |
| `administration.dinhchinh` | `administration.correction` |
| `administration.luutru` | `administration.retention` |
| `administration.taikhoan` | `administration.account` |
| `common.thoigian` | `common.time` |
| `common.loi`, `common.web`, `common.ngoaituyen`, `common.dulieu` | `common.error`, `common.web`, `common.offline`, `common.data` |
| `common.thongbao`, `common.tep`, `common.lich` | `common.notification`, `common.file`, `common.scheduling` |

## 3. Vai trò (mục 2.3)

| Tên logic | Mã (enum `common.Role`, có `code()` = AC-xx) |
| --- |------------------------|
| Quản lý viện | `NURSING_HOME_MANAGER` |
| Trưởng tầng | `FLOOR_MANAGER`        |
| Bác sĩ | `DOCTOR`               |
| Điều dưỡng | `NURSING_STAFF`        |
| Nhân viên chăm sóc | `CAREGIVER`            |
| Nhân viên bếp | `KITCHEN_STAFF`        |
| Hành chính | `OFFICE_STAFF`         |
| Kế toán | `ACCOUNTANT`           |
| Người thân | `FAMILY_MEMBER`        |
| Người cao tuổi (AC-16) | `RESIDENT` |
| Bộ lập lịch | `SCHEDULER`            |

## 4. Thuật ngữ chung

| Tên logic | Mã |
| --- | --- |
| người cao tuổi | resident |
| lý do | reason |
| bản ghi | record |
| nhóm dữ liệu | data group |
| danh mục | catalog |
| phiên bản | version |
| hiệu lực, ngày hiệu lực | effective, effectiveDate |
| ngừng hiệu lực, kích hoạt lại | deactivate, reactivate |
| tham số | parameter |
| nhật ký | audit log |
| yêu cầu phê duyệt | approval request |
| gửi duyệt, duyệt, từ chối, hủy | submit, approve, reject, cancel |
| áp dụng, áp dụng bù | apply, catch-up apply |
| người duyệt, người yêu cầu | approver, requester |
| đính chính | correction |
| lưu giữ, loại bỏ | retention, purge |
| lệnh nghiệp vụ | business command |
| thời gian nghiệp vụ | business time |
| đồng hồ (giả lập) | clock (test clock) |
| điều kiện chưa thỏa | unmet conditions |
| giá trị hiện hành | current value |
| tài khoản | account |
| vai trò, quyền, phạm vi | role, permission, scope |
| phiên đăng nhập, thiết bị | session, device |
| tài khoản người cao tuổi, thiết bị gắn | resident account, bound device |
| mã QR đăng nhập | login QR code |
| đăng nhập, đăng xuất, đổi mật khẩu | login, logout, change password |
| khóa tạm, mở khóa sớm | temporary lock, early unlock |
| hồ sơ nhân viên, giấy phép | staff profile, license |
| tầng, phòng, giường | floor, room, bed |
| ca, phân công, bàn giao | shift, assignment, handover |
| tiếp nhận, hợp đồng, lưu trú | admission, contract, residency |
| nội trú, bán trú | residential care, day care |
| mức chăm sóc, kế hoạch chăm sóc | care level, care plan |
| công việc chăm sóc | care task |
| thuốc, đơn thuốc, liều | medication, prescription, dose |
| chỉ số, cảnh báo, sự cố | vital sign, alert, incident |
| nút SOS, cảnh báo SOS | SOS button, SOS alert |
| thông báo | notification |
| chi phí phát sinh | incurred cost |
| dinh dưỡng, thực đơn, suất ăn | nutrition, menu, meal portion |
| cổng người thân | family portal |
| kênh người cao tuổi | resident channel |
| phản hồi; loại Góp ý, Hỏi đáp, Khiếu nại, Mong muốn; ghi thay | feedback; suggestion, question, complaint, wish; recorded on behalf |
| đồ dùng cá nhân | personal belongings |
| hoạt động | activity |
| đổi ca | shift swap |
| báo cáo, dashboard | report, dashboard |
| sổ số dư | balance ledger |
| khai báo tạm trú | residence registration |
| kho, tài sản | inventory, asset |
| ngoại tuyến, đồng bộ | offline, sync |

## 5. Mã lỗi (`maLoi` → `errorCode`)

| Tên logic | Mã |
| --- | --- |
| `DIEU_KIEN_CHUA_THOA` | `CONDITION_NOT_MET` |
| `THIEU_LY_DO` | `REASON_REQUIRED` |
| `KHONG_CO_QUYEN` | `PERMISSION_DENIED` |
| `DA_DUOC_XU_LY` | `ALREADY_PROCESSED` |
| `GIA_TRI_KHONG_HOP_LE` | `INVALID_VALUE` |
| `PHIEN_HET_HAN` | `SESSION_EXPIRED` |
| `DIEU_KIEN_PHAP_LY` | `LEGAL_CONDITION_NOT_MET` |
| `TRANG_THAI_KHONG_HOP_LE` | `INVALID_STATE` |
| `KHONG_DUOC_TU_DUYET` | `SELF_APPROVAL_NOT_ALLOWED` |
| `SAI_PHIEN_BAN` | `VERSION_MISMATCH` |
| `KHONG_DUOC_DINH_CHINH` | `CORRECTION_NOT_ALLOWED` |
| `KHONG_HO_TRO_DINH_CHINH` | `CORRECTION_NOT_SUPPORTED` |
| `BAN_GHI_DA_HUY_GHI_NHAN` | `RECORD_VOIDED` |
| `BAN_GHI_CHUA_XAC_NHAN` | `RECORD_NOT_CONFIRMED` |
| `DANH_MUC_DA_DUOC_THAM_CHIEU` | `CATALOG_ITEM_IN_USE` |
| `KHONG_TIM_THAY` (404, không lộ tồn tại) | `NOT_FOUND` |
| `KHOA_LAP_LAI_DA_DUNG` (cùng khóa, nội dung khác) | `IDEMPOTENCY_KEY_REUSED` |
| `KHOA_LAP_LAI_DANG_XU_LY` | `IDEMPOTENCY_IN_PROGRESS` |
| cùng khóa, lệnh đã chạy, kết quả có bí mật nên không lưu | `IDEMPOTENCY_RESULT_NOT_KEPT` |
| dùng lệnh khóa, mở khóa riêng của tài khoản người cao tuổi | `USE_RESIDENT_ACCOUNT_COMMAND` |
| người cao tuổi đã ở trạng thái cuối, không tạo mã đăng nhập | `RESIDENCY_ENDED` |
| lỗi của framework | tên trạng thái HTTP, ví dụ `BAD_REQUEST`, `NOT_FOUND`, `METHOD_NOT_ALLOWED` |

Bổ sung theo từng feature khi dịch `contracts/api-NNN.yaml` (R-03).

## 6. Feature 000 *(bổ sung 2026-10-19, khi dịch `contracts/api-000.yaml`)*

### 6.1. Đường dẫn API (tiền tố `/api/v1`)

| Tên logic | Mã |
| --- | --- |
| `/tham-so`, `/tham-so/{ma}` | `/parameters`, `/parameters/{code}` |
| `…/doi-gia-tri`, `…/lich-su?taiThoiDiem=` | `…/change-value`, `…/history?asOf=` |
| `/yeu-cau-phe-duyet`, `/yeu-cau-phe-duyet/{id}` | `/approval-requests`, `/approval-requests/{id}` |
| `…/sua-noi-dung`, `…/gui-duyet`, `…/duyet`, `…/tu-choi`, `…/huy` | `…/edit-content`, `…/submit`, `…/approve`, `…/reject`, `…/cancel` |
| `/dinh-chinh/{loaiBanGhi}/{idBanGhiGoc}` | `/corrections/{recordType}/{originalRecordId}` |
| `/nhat-ky`, `/nhat-ky/doi-tuong/{doiTuongLoai}/{doiTuongId}` | `/audit-logs`, `/audit-logs/entities/{entityType}/{entityId}` |
| `/kiem-thu/dong-ho` | `/testing/clock` |

### 6.2. Schema và trường JSON

| Tên logic | Mã |
| --- | --- |
| `Loi` (`maLoi`, `dieuKienChuaThoa[]` {`ma`, `moTa`, `canCu`, `module`, `mienDuoc`}, `giaTriHienHanh`, `ungVien[]`) | `Problem` (`errorCode`, `unmetConditions[]` {`code`, `description`, `basis`, `module`, `waivable`}, `currentValue`, `candidates[]`); `UnmetCondition` (ba trường sau không bắt buộc, 001 BL-23 2026-10-23) |
| `LyDo` | `Reason` |
| `ThamSo` (`ma`, `moTa`, `kieu`, `donVi`, `rangBuoc`, `giaTriMacDinh`, `giaTriHienHanh`, `dungTai`, `phienBan`, `doiLuc`) | `Parameter` (`code`, `description`, `type`, `unit`, `constraints`, `defaultValue`, `currentValue`, `usedIn`, `version`, `changedAt`) |
| đổi giá trị: `giaTri`, `khoiPhucMacDinh` | `value`, `restoreDefault` |
| `LichSuThamSo` (`hieuLucTu`, `hieuLucDen`, `laKhoiPhucMacDinh`) | `ParameterHistoryEntry` (`effectiveFrom`, `effectiveTo`, `isDefaultRestore`) |
| `NguoiThucHien` (`loai`, `hoTen`, `vaiTro`) | `Actor` (`type`, `fullName`, `role`) |
| `YeuCau` | `ApprovalRequest` |
| `loai`, `noiDung`, `trangThai`, `maHienThi` | `type`, `content`, `status`, `displayCode` |
| `nguoiYeuCau`, `taoLuc`, `guiDuyetNgay` | `requester`, `createdAt`, `submitImmediately` |
| `ngayHieuLucMongMuon`, `ngayHieuLucThucTe` | `requestedEffectiveDate`, `actualEffectiveDate` |
| `thoiDiemGuiDuyet`, `nguoiQuyetDinh`, `thoiDiemQuyetDinh`, `yKienQuyetDinh`, `yKien` | `submittedAt`, `decidedBy`, `decidedAt`, `decisionComment`, `comment` |
| `tuDuyet`, `lyDoKetThuc`, `thoiDiemApDung`, `apDungBu`, `ketQuaApDung` | `selfApproved`, `closingReason`, `appliedAt`, `catchUpApplied`, `applicationResult` |
| `lenhDuocPhep` (giá trị là tên lệnh trong đường dẫn: `duyet`, `tu-choi`…) | `allowedCommands` (`approve`, `reject`…) |
| `gioiHanVaiTroDuyet` (SPI `LoaiYeuCauHandler`) | `approverRoleRestriction` |
| `hop` (`CUA_TOI`, `CHO_TOI_DUYET`, `TAT_CA_TRONG_PHAM_VI`) | `inbox` (`MINE`, `AWAITING_MY_APPROVAL`, `ALL_IN_SCOPE`) |
| `TrangYeuCau`, `TrangNhatKy` (`muc`, `sau`); tham số `sau` | `ApprovalRequestPage`, `AuditLogPage` (`items`, `nextCursor`); tham số `cursor` |
| `DinhChinh` (`soThuTu`, `kieu`, `noiDungDung`, `cachTao`, `yeuCauPheDuyetId`, `thoiDiem`) | `Correction` (`sequenceNo`, `type`, `correctedContent`, `creationMethod`, `approvalRequestId`, `recordedAt`) |
| `ChuoiDinhChinh` (`loaiBanGhi`, `idBanGhiGoc`, `daDinhChinh`, `daHuyGhiNhan`, `noiDungGoc`, `noiDungHienHanh`, `cacLan`) | `CorrectionChain` (`recordType`, `originalRecordId`, `corrected`, `voided`, `originalContent`, `currentContent`, `corrections`) |
| `GiaTriTruong` (`{"daThayDoi": true}`) | `FieldValue` (`{"changed": true}`) |
| `BanGhiNhatKy` (`thoiDiem`, `thoiDiemThietBi`, `hanhDong`, `doiTuong`, `duLieuTruoc`, `duLieuSau`, `lyDoBiAn`, `canCu`, `chiTiet`) | `AuditLogEntry` (`occurredAt`, `deviceTime`, `action`, `entity`, `dataBefore`, `dataAfter`, `reasonHidden`, `basis`, `details`) |
| lọc nhật ký: `nguoiThucHienId`, `tu`, `den` | `actorId`, `from`, `to` |
| `DongHo` (`thoiDiem`, `giaLap`); `datThoiDiem`, `tua`, `chayJob` | `Clock` (`now`, `simulated`); `setTo`, `advanceBy`, `runJobs` |

### 6.3. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| kiểu tham số `THOI_LUONG`, `SO_NGUYEN`, `TY_LE`, `SO_TIEN`, `GIO_TRONG_NGAY`, `BANG` | `DURATION`, `INTEGER`, `PERCENTAGE`, `MONEY`, `TIME_OF_DAY`, `TABLE` |
| trạng thái yêu cầu `NHAP`, `CHO_DUYET`, `DA_DUYET`, `DA_AP_DUNG`, `AP_DUNG_KHONG_THANH`, `TU_CHOI`, `HUY` | `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `APPLIED`, `APPLY_FAILED`, `REJECTED`, `CANCELLED` |
| kiểu đính chính `SUA_NOI_DUNG`, `HUY_GHI_NHAN` | `AMEND_CONTENT`, `VOID` |
| cách tạo đính chính `A`, `B`, `C` | giữ nguyên |
| người thực hiện `NGUOI_DUNG`, `BO_LAP_LICH`, `KHOI_TAO` | `USER`, `SCHEDULER`, `BOOTSTRAP` |
| nhóm dữ liệu `DANH_MUC`, `NGHIEP_VU`, `GHI_NHAN` | `CATALOG`, `BUSINESS`, `RECORD` |
| kết quả đồng bộ `CHAP_NHAN`, `CHAP_NHAN_KEM_NHAN`, `CHO_XEM_LAI`, `TU_CHOI` | `ACCEPTED`, `ACCEPTED_WITH_FLAG`, `PENDING_REVIEW`, `REJECTED` |
| job `AP_DUNG_YEU_CAU_DEN_HAN`, `NHAC_YEU_CAU_CHO_DUYET`, `LOAI_BO_DU_LIEU_HET_HAN` | `APPLY_DUE_REQUESTS`, `REMIND_PENDING_REQUESTS`, `PURGE_EXPIRED_DATA` |
| loại yêu cầu, loại bản ghi của 000: `M15.DINH_CHINH`, `M15.MAU`, `M15.GHI_NHAN_MAU` | `M15.CORRECTION`, `M15.SAMPLE`, `M15.SAMPLE_RECORD` |

Loại yêu cầu, loại bản ghi của module khác (ví dụ `M02.THAY_DOI_LUU_TRU` → `M02.RESIDENCY_CHANGE`, `M07.LIEU_THUOC` → `M07.MEDICATION_DOSE` trong ví dụ của api-000) do feature sở hữu chốt khi dịch hợp đồng của mình.

### 6.4. Bảng, view, trigger

| Tên logic | Mã |
| --- | --- |
| `tham_so`, `lich_su_tham_so` | `parameter`, `parameter_history` |
| `nhat_ky` | `audit_log` |
| `yeu_cau_phe_duyet`, `ket_qua_ap_dung` | `approval_request`, `application_result` |
| `dinh_chinh`, `chuoi_dinh_chinh` | `correction`, `correction_chain` |
| `khoa_lap_lai` (`khoa`, `nguoi_dung_id`, `ma_bam_yeu_cau`, `ket_qua_json`, `het_han_luc`) | `idempotency_key` (`key_value`, `username`, `request_hash`, `result_json`, `expires_at`) |
| `event_publication`, `shedlock` | giữ nguyên (Spring Modulith, ShedLock) |
| `v_dinh_chinh_moi_nhat`, `v_ban_ghi_da_huy` | `v_latest_correction`, `v_voided_record` |
| `trg_<bang>_khong_sua`, `trg_<bang>_khong_xoa` | `trg_<table>_no_update`, `trg_<table>_no_delete` |
| tài khoản `eldercare_app`, `eldercare_luu_tru` | `eldercare_app`, `eldercare_retention` |
| cột `doi_tuong_loai`, `doi_tuong_id`, `nguoi_thuc_hien_loai`, `nguoi_thuc_hien_id`, `thoi_diem`, `thoi_diem_thiet_bi` | `entity_type`, `entity_id`, `actor_type`, `actor_id`, `occurred_at`, `device_time` |
| cột `ly_do`, `can_cu`, `chi_tiet`, `du_lieu_truoc`, `du_lieu_sau`, `khong_loai_bo` | `reason`, `basis`, `details`, `data_before`, `data_after`, `exempt_from_purge` |
| cột `gia_tri_hien_hanh`, `gia_tri_mac_dinh`, `rang_buoc`, `hieu_luc_tu`, `hieu_luc_den`, `phien_ban` | `current_value`, `default_value`, `constraints`, `effective_from`, `effective_to`, `version` |

### 6.5. Lớp chính

| Tên logic | Mã |
| --- | --- |
| `DongHoConfig`, `ThoiGianNghiepVu` (`homNay`, `bayGio`, `batDauNgay`, `trongKhoang`) | `ClockConfig`, `BusinessTime` (`today`, `now`, `startOfDay`, `isWithin`) |
| `DieuKhienDongHo`, `DongHoKiemThuController` | `TestClockControl`, `TestClockController` |
| `LoiNghiepVu` (`DieuKienChuaThoa`, `KhongCoQuyen`, `DaDuocXuLy`, `SaiPhienBan`, `ThieuLyDo`, `GiaTriKhongHopLe`) | `BusinessException` (`ConditionNotMetException`, `PermissionDeniedException`, `AlreadyProcessedException`, `VersionMismatchException`, `ReasonRequiredException`, `InvalidValueException`) |
| `ProblemDetailsHandler`, `PhienBanEtag`, `IdempotencyFilter` | `ProblemDetailsHandler`, `VersionEtag`, `IdempotencyFilter` |
| `PhongBiNgoaiTuyen` (`idempotencyKey`, `thoiDiemThietBi`, `ngoaiTuyen`), `KetQuaDongBo` kèm lý do | `OfflineEnvelope` (`idempotencyKey`, `deviceTime`, `offline`), `SyncResult`, `SyncOutcome` (`result`, `reason`) |
| `DieuKien` (mã BR, mô tả) | `UnmetCondition` (`code`, `description`) |
| `LyDo` | `Reason` |
| `@NhomDuLieu`, `@LenhNghiepVu` (`batBuocLyDo`), `ChiThemRepository` (`them`), `DanhMucCoSo`, `PhienBanHieuLuc` | `@DataGroup`, `@BusinessCommand` (`requiresReason`), `AppendOnlyRepository` (`append`), `CatalogBase`, `EffectiveVersioning` |
| lớp cơ sở repository JPA (có `append`) | `BaseJpaRepository` (khai ở `common.data.JpaConfig`) |
| trạng thái danh mục `HIEU_LUC`, `NGUNG_HIEU_LUC`; cột `ten`, `ten_hieu_luc` | `CatalogStatus` `ACTIVE`, `INACTIVE`; cột `name`, `active_name` |
| `DanhMucCoSo` (`ngungHieuLuc`, `kichHoatLai`) | `CatalogBase` (`deactivate`, `reactivate`, `isActive`, `rename`) |
| `PhienBanHieuLuc` | `EffectiveVersioning` (`addVersion`, `VersionedTable`) |
| kiểm lý do của `@LenhNghiepVu` | `BusinessCommandAspect` |
| dữ liệu mẫu `M15.GHI_NHAN_MAU`, danh mục mẫu, nghiệp vụ mẫu | `M15.SAMPLE_RECORD` (bảng `sample_floor_record`, `sample_office_record`), `sample_catalog`, `sample_case` (`sample_case_version`) |
| test `NhomDuLieuArchTest`, `DanhMucMauIT`, `PhienBanKhongChongKhoangIT`, `NhomGhiNhanCoTriggerIT`, `LenhNghiepVuLyDoTest` | `DataGroupArchTest`, `CatalogSampleIT`, `EffectiveVersionNoOverlapIT`, `RecordGroupTriggerIT`, `BusinessCommandReasonIT` |
| `backend/docs/quy-uoc-nhom-du-lieu.md` | `backend/docs/data-group-conventions.md` |
| `backend/docs/van-hanh.md`, `backend/tools/sinh-du-lieu/` | `backend/docs/operations.md`, `backend/tools/data-generator/` |
| `frontend/src/chung/loi.js` | `frontend/src/common/problem.js` (`problemOf`), `ProblemAlert.jsx`; ngày `dateTime.js` (`formatDate`) |
| `NguoiDungHienTai` (`taiKhoanId`, `vaiTro`, `laBoLapLich`), `VaiTro` | `CurrentUser` (`accountId`, `roles`, `isScheduler`), `Role` (có `code()` = AC-xx) |
| `QuyenApi.coQuyen`, `KhoaQuyen` (`chucNang`, `thaoTac`), `KetQuaQuyen`, `DoiTuong` | `PermissionApi.hasPermission`, `PermissionKey` (`function`, `operation`), `PermissionResult`, `Target` (`type`, `id`, `residentId`, `floorAreaId`; `UNSPECIFIED`) |
| `PhamViApi` (`trongPhamVi`, `nguoiPhuTrachCa`, `truongTangDuocGiao`) | `ScopeApi` (`inScope`, `shiftLead`, `assignedFloorManager`) |
| `QuyenXemTruongApi.duocXem`, `MucXem` (`DAY_DU`, `CHI_CO_HAY_KHONG`, `KHONG`) | `FieldVisibilityApi.canView`, `Visibility` (`FULL`, `PRESENCE_ONLY`, `NONE`) |
| `NhomThongTin` (`NHAN_DANG`, `LIEN_HE`, `MUC_CHAM_SOC`, `SUC_KHOE`, `THUOC`, `NGUYEN_VONG`, `CHI_PHI`, `TAI_CHINH`, `CONG_KHAI_NOI_BO`) | `InfoGroup` (`IDENTITY`, `CONTACT`, `CARE_LEVEL`, `HEALTH`, `MEDICATION`, `WISHES`, `COSTS`, `FINANCE`, `INTERNAL_PUBLIC`) |
| `NguoiDuyetApi` (`coNguoiCoQuyen`, `danhSach`), `TaiKhoanApi.coVaiTro` | `ApproverApi` (`hasAnyApprover`, `approvers`), `AccountApi.hasRole` |
| `QuyenGia` (bản giả trong test) | `FakePermissions` |
| `NhatKyApi` (`ghi`, `ghiNgoaiTransaction`), `BanGhiNhatKy`, `NhatKyService`, `NhatKyQueryService`, `PhanLoaiTruongProvider` | `AuditLogApi` (`write`, `writeOutsideTransaction`), `AuditLogEntry`, `AuditLogService`, `AuditLogQueryService`, `FieldClassificationProvider` |
| builder `BanGhiNhatKy`: `hanhDong`, `doiTuong`, `nguoiCaoTuoi`, `truoc`, `sau`, `lyDo`, `canCu`, `chiTiet`, `thoiDiemThietBi` | `action`, `entity`, `resident`, `before`, `after`, `reason`, `basis`, `details`, `deviceTime` |
| hành động nhật ký (mã, có bắt buộc lý do) | `AuditAction` (`code`, `requiresReason`) |
| `CanCu` (`maQuyTac`, `yeuCauPheDuyetId`, `banGhiNguonId`, `lanChayLichId`) | `Basis` (`ruleCode`, `approvalRequestId`, `sourceRecordId`, `schedulerRunId`) |
| `ThamSoApi` (`doc`, `docTaiThoiDiem`), `MaThamSo`, `GiaTriThamSo`, `ThamSoService`, `KiemTraThamSo` | `ParameterApi` (`read`, `readAt`), `ParameterCode` (hằng `CFG_M15_05`, `code()` = `CFG-M15-05`), `ParameterValue` (`asDuration`, `asPeriod`, `asInt`, `asPercent`, `asMoney`, `asLocalTime`, `asTable`, `isSet`), `ParameterService`, `ParameterValidator` |
| kiểu tham số `THOI_LUONG`, `SO_NGUYEN`, `TY_LE`, `SO_TIEN`, `GIO_TRONG_NGAY`, `BANG` | `ParameterKind`: `DURATION`, `INTEGER`, `PERCENTAGE`, `MONEY`, `TIME_OF_DAY`, `TABLE` |
| đơn vị `GIAY`, `PHUT`, `GIO`, `NGAY`, `THANG`, `NAM`, `LAN`, `PHAN_TRAM`, đồng, `DONG` (dòng; 2026-10-22, Q-373, Q-382) | `SECONDS`, `MINUTES`, `HOURS`, `DAYS`, `MONTHS`, `YEARS`, `TIMES`, `PERCENT`, `VND`, `ROWS` |
| bảng `tham_so` (`ma`, `mo_ta`, `kieu`, `don_vi`, `rang_buoc`, `gia_tri_mac_dinh`, `gia_tri_hien_hanh`, `dung_tai`, `phien_ban`, `doi_luc`, `doi_boi`) | `parameter` (`code`, `description`, `kind`, `unit`, `constraints`, `default_value`, `current_value`, `used_in`, `version`, `changed_at`, `changed_by`) |
| bảng `lich_su_tham_so` (`gia_tri`, `hieu_luc_tu`, `nguoi_thuc_hien_id`, `ly_do`, `la_khoi_phuc_mac_dinh`) | `parameter_history` (`value`, `effective_from`, `actor_id`, `reason`, `is_default_restore`) |
| ràng buộc `lonHon` | `greaterThan` |
| `YeuCauPheDuyetApi`, `LoaiYeuCauHandler`, `BangChuyenYeuCau`, `YeuCauPheDuyetService`, `NhacQuaHan` | `ApprovalRequestApi`, `RequestTypeHandler`, `RequestTransitionTable`, `ApprovalRequestService`, `OverdueReminder` |
| `DinhChinhApi` (`taoDinhChinh`, `hienHanh`, `chuoi`), `DinhChinhDuocProvider`, `ChonCachDinhChinh`, `DinhChinhService` | `CorrectionApi` (`create`, `current`, `chain`), `CorrectableRecordProvider`, `CorrectionMethodSelector`, `CorrectionService` |
| *(bổ sung 2026-10-27)* `DinhChinhDuocProvider.nguoiCaoTuoi(id)`; `ThoiHanLuuGiu` của bản ghi trong chuỗi khi hỏi phần đi kèm | `CorrectableRecordProvider.residentId(id)` (mặc định rỗng); `AttachedRetention.attachedTo(record, recordKind, cutoff)` |
| SPI `DinhChinhDuocProvider`: `loaiBanGhi`, `daXacNhan`, `nguoiGhiGoc`, `tangKhuVuc`, `coQuyenGhiNhan`, `kiemTraNoiDung`, `noiDungGoc`, `choPhep`, `sauKhiDinhChinh` | `recordType`, `isConfirmed`, `originalRecorder`, `floorArea`, `canRecord`, `validateContent`, `originalContent`, `allows`, `afterCorrection` |
| `KetQuaDinhChinh` (`DaLuu`, `ChoDuyet`), `GiaTriHienHanh`, `ChuoiDinhChinhView`, `DinhChinhView`, sự kiện `DaDinhChinh` | `CorrectionResult` (`Saved`, `PendingApproval`), `CurrentValue`, `CorrectionChainView`, `CorrectionView`, `CorrectionMade` |
| bảng `dinh_chinh` (`loai_ban_ghi`, `id_ban_ghi_goc`, `kieu`, `noi_dung_dung`, `ly_do`, `nguoi_thuc_hien_id`, `cach_tao`, `yeu_cau_phe_duyet_id`, `thoi_diem`, `so_thu_tu`) | `correction` (`record_type`, `original_record_id`, `type`, `corrected_content`, `reason`, `actor_id`, `creation_method`, `approval_request_id`, `recorded_at`, `sequence_no`) |
| hành động nhật ký `DINH_CHINH_THEM` | `CORRECTION_ADDED` |
| schema lỗi `Loi` trên OpenAPI sinh ra | `common.error.web.ProblemBody` (`@Schema(name = "Problem")`) |
| `LuuTruProvider`, `LoaiBoDuLieuHetHan` | `RetentionProvider`, `ExpiredDataPurgeJob` |
| SPI `PhanLoaiTruongProvider`: `loaiDoiTuong`, `phanLoaiTruong` | `FieldClassificationProvider`: `entityType`, `fieldGroups` |
| khóa quyền xem nhật ký (Phụ lục 27, dòng "Nhật ký thay đổi": X toàn viện, P trong phạm vi) | `AuditLogPermissions.VIEW` = `(Function.AUDIT_LOG, Operation.VIEW)`; tra cứu toàn viện là kiểm `VIEW` với `Target.UNSPECIFIED` (chỉ phạm vi toàn viện đạt; bỏ `VIEW_FACILITY` khi 002 đổi khóa sang enum) |
| `NhatKyQueryService`: tra toàn viện, lịch sử một đối tượng, `TrangNhatKy` | `AuditLogQueryService`: `search`, `history`, `Page`; controller `AuditLogController` |
| SPI `LuuTruProvider`: `loaiDuLieu`, `loaiHan`, `ungVienHetHan`, `thamChieuToi`, `xoa`; `UngVien`, `ThamChieuDuLieu` | `RetentionProvider`: `dataType`, `retentionKind`, `expiredCandidates`, `referencesTo`, `delete`; `Candidate`, `DataRef` |
| `LoaiHan` (`THEO_HO_SO`, `KHONG_GAN_HO_SO`, `DAI_HON(MaThamSo)`) | `RetentionKind` (`ResidentRecord`, `NotResidentLinked`, `Longer(ParameterCode)`) |
| kết nối tài khoản `eldercare_luu_tru` | `RetentionDatabase` (`jdbc`, `transaction`) |
| loại dữ liệu lưu giữ của 000: nhật ký, đính chính (đi kèm bản ghi trong chuỗi) | `M15.AUDIT_LOG`, `M15.CORRECTION` (`AuditLogRetention`, `CorrectionRetention`, `AttachedRetention`); một đợt loại bỏ: `ExpiredDataPurge` |
| hành động nhật ký `LOAI_BO_DU_LIEU_HET_HAN` (bản tóm tắt, đối tượng là lần chạy), `XUAT_BAO_CAO` | `EXPIRED_DATA_PURGED` (`AuditAction.EXPIRED_DATA_PURGED`, đối tượng `M15.PURGE_RUN`), `REPORT_EXPORTED` |
| *(Đổi api 2026-10-24, 016 US9)* `NhatKyApi.banGhiDaGhi`, bản ghi đã lưu | `AuditLogApi.recorded` (`RECORDED_LIMIT` = 200), `AuditLogRecord` (`id`, `occurredAt`, `actorAccountId`, `role`, `entityId`, `reason`, `details`) |
| job chạy ngay theo `runJobs` của đồng hồ giả lập | `common.scheduling.ScheduledJob` (`name`, `runNow`) |
| test `NhatKyDayDuIT`, `NhatKyCheTruongIT`, `NhatKyPhamViIT`, `NhatKyNgoaiTuyenIT`, `NhatKyXuatBaoCaoIT`, `LoaiBoDuLieuIT` | `AuditLogCompletenessIT`, `AuditLogFieldMaskingIT`, `AuditLogScopeIT`, `AuditLogOfflineIT`, `AuditLogReportExportIT`, `ExpiredDataPurgeIT` |
| màn `tinh-nang/nhat-ky/NhatKyBang.jsx`, test `NhatKyBang.test.jsx` | `features/audit-log/AuditLogTable.jsx`, `AuditLogTable.test.jsx` |
| màn `tinh-nang/tham-so/ThamSoDanhSach.jsx`, `ThamSoForm.jsx`, `ThamSoLichSu.jsx`, test `ThamSoForm.test.jsx` | `features/parameters/ParameterList.jsx`, `ParameterForm.jsx`, `ParameterHistory.jsx` (thêm `TableValueEditor.jsx`, `parameterValue.js`), test `ParameterForm.test.jsx`, `ParameterList.test.jsx`, `parameterValue.test.js`; màn chứa `features/admin/AdminPage.jsx` |
| màn `tinh-nang/yeu-cau-phe-duyet/HopDuyet.jsx`, `ChiTietYeuCau.jsx`, test `HopDuyet.test.jsx`; `lenhDuocPhep` | `features/approval-requests/ApprovalInbox.jsx`, `ApprovalRequestDetail.jsx` (nhãn ở `approvalStatus.js`), test `ApprovalInbox.test.jsx`; `allowedCommands` |
| tiện ích phiên bản: đổi thuộc tính từ ngày (FR-004) | `EffectiveVersioning.addVersionFrom`; test `VersionedAttributeIT`; migration mẫu `V000_91__sample_case_version_close.sql` |
| test truy vết FR-010a, FR-017 | `SchedulerScopeArchTest` (danh sách `ALLOWED`), `ParameterChangeNoRecalculationIT` |
| chạy một đợt loại bỏ dữ liệu lúc khởi động (sau khôi phục) | `eldercare.jobs.purge-expired-data.run-once`, `ExpiredDataPurgeJob.atStartup`; test `ExpiredDataPurgeJobTest` |
| bản giả fetch theo đường dẫn cho test web | `frontend/src/test/fetchRoutes.js` (`mockRoutes`, `json`, `problem`) |
| `ApDungYeuCauDenHan`, `NhacYeuCauChoDuyet` | `ApplyDueRequestsJob`, `RemindPendingRequestsJob` (gói `scheduling`); một đợt: `DueRequestApplication`, `PendingRequestReminder`; một yêu cầu: `ApplyTransaction`, `RequestApplier` (`applySafely`), `ReminderTransaction` |
| `ThamSoService` (`danhSach`, `chiTiet`, `lichSu`, `doiGiaTri`), `KiemTraThamSo`, `ThamSoController`, sự kiện `ThamSoDaDoi` | `ParameterService` (`list`, `detail`, `history`, `changeValue`), `ParameterValidator`, `ParameterController`, `ParameterChanged` |
| hành động nhật ký `THAM_SO_DOI`; khóa quyền tham số (xem, đổi) | `PARAMETER_CHANGED` (đối tượng `M15.PARAMETER`); `ParameterPermissions.VIEW`, `ParameterPermissions.CHANGE` = `(Function.ACCOUNT_PERMISSION_PARAMETER, Operation.VIEW / UPDATE)` |
| test `KiemTraThamSoTest`, `ThamSoIT`, `ThamSoQuyenIT`, `ThamSoBoDemSauCommitIT`, `ThamSoTaiThoiDiemIT`, `ThamSoBangIT` | `ParameterValidatorTest`, `ParameterChangeIT`, `ParameterPermissionIT`, `ParameterCacheAfterCommitIT`, `ParameterAsOfIT`, `ParameterTableIT` |
| bảng `yeu_cau_phe_duyet`, `ket_qua_ap_dung` | `approval_request`, `application_result` (`ApprovalRequestStore`); người duyệt, quyền xem: `ApproverDirectory`; `NhacQuaHanImpl` → `OverdueReminderImpl` |
| trạng thái yêu cầu `NHAP`, `CHO_DUYET`, `DA_DUYET`, `DA_AP_DUNG`, `AP_DUNG_KHONG_THANH`, `TU_CHOI`, `DA_HUY` | `ApprovalStatus.DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `APPLIED`, `APPLY_FAILED`, `REJECTED`, `CANCELLED` |
| hành động nhật ký `YC_LAP`, `YC_SUA_NHAP`, `YC_GUI_DUYET`, `YC_DUYET`, `YC_TU_CHOI`, `YC_HUY`, `YC_AP_DUNG`, `YC_AP_DUNG_KHONG_THANH`, `YC_AP_DUNG_LOI`, `YC_NHAC`, `YC_BAO_QLV`, `YC_CHUYEN_MO_RONG`, `YC_DONG_DO_TRANG_THAI_CUOI` | `REQUEST_CREATED`, `REQUEST_DRAFT_EDITED`, `REQUEST_SUBMITTED`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_CANCELLED`, `REQUEST_APPLIED`, `REQUEST_APPLY_FAILED`, `REQUEST_APPLY_ERROR`, `REQUEST_REMINDED`, `REQUEST_ESCALATED`, `REQUEST_EXTENDED_TRANSITION`, `REQUEST_CLOSED_FOR_FINAL_STATE` (đối tượng `M15.APPROVAL_REQUEST`) |
| sự kiện `YeuCauDaLap`, `YeuCauDaGuiDuyet`, `YeuCauCanNhacNguoiDuyet`, `YeuCauCanBaoQuanLyVien`, `YeuCauDaQuyetDinh`, `YeuCauDaApDung`, `YeuCauApDungKhongThanh`, `YeuCauDaHuy` | `RequestCreated`, `RequestSubmitted`, `ApproverReminderDue`, `ManagerEscalationDue`, `RequestDecided`, `RequestApplied`, `RequestApplyFailed`, `RequestCancelled` |
| hộp yêu cầu (`CUA_TOI`, `CHO_TOI_DUYET`, `TRONG_PHAM_VI`) | `ApprovalRequestService.Inbox` (`MINE`, `AWAITING_MY_APPROVAL`, `ALL_IN_SCOPE`); controller `ApprovalRequestController` |
| test `BangChuyenTrangThaiTest`, `YeuCauVongDoiIT`, `ApDungMotLanIT`, `ApDungBuIT`, `ApDungLoiIT`, `NhacYeuCauIT`, `QuyetDinhDongThoiIT`, `TuDuyetIT`, `GioiHanVaiTroDuyetIT`; handler mẫu `YeuCauMauHandler` | `RequestTransitionTableTest`, `ApprovalLifecycleIT`, `ApplyOnceIT`, `CatchUpApplyIT`, `ApplyFailureIT`, `PendingReminderIT`, `ConcurrentDecisionIT`, `SelfApprovalIT`, `ApproverRoleRestrictionIT`; `ApprovalFixtures.SampleRequestHandler` |
| `DinhChinhYeuCauHandler`, khóa quyền yêu cầu đính chính (lập, duyệt); test cách C | `CorrectionRequestHandler`, `CorrectionPermissions.REQUEST`, `CorrectionPermissions.APPROVE` = `(Function.UNASSIGNED_RECORD_CORRECTION, Operation.CREATE / APPROVE)`; `CorrectionApprovalIT` |
| loại dữ liệu lưu giữ yêu cầu phê duyệt | `M15.APPROVAL_REQUEST` (`ApprovalRequestRetention`) |
| `TepApi` (`taiLen`, `gan`), `QuyenTepProvider` | `FileApi` (`upload`, `attach`), `FilePermissionProvider` (`ownerType`, `canDownload`) |
| kiểu tệp nhận được (001 research H-13, 017 research E-11; bổ sung 2026-10-22) | `FileTypes` (`check`, `USER_UPLOAD` cho `POST /files`, `SYSTEM_GENERATED` thêm `XLSX` cho `FileApi.upload`, `MAX_SIZE`) |
| gói `common.bieumau`, `PdfBieuMau` (`dongDau`), `ExcelBieuMau`; thông tin viện CFG-M11-11 (tên, địa chỉ, mã số thuế, điện thoại, logo) (007 research S-25; 007 T104) | gói `common.document`, `PdfDocument` (`a4`, `paragraph`, `table`, `signatures`, `toBytes`, `stamp`), `ExcelDocument` (`sheet`, `Sheet.header`, `line`, `table`, `toBytes`, `read`), `ExcelRow` (`rowNumber`, `text`, `number`, `date`); `FacilityInfo` (`name`, `address`, `taxCode`, `phone`, `logoFileId`; cũng là khóa JSON của giá trị CFG-M11-11) |
| SPI `KiemTraGiaTriThamSo` (`ma`, `kiemTra`) (017 research E-04) | `ParameterValueCheck` (`code`, `check`) ở `administration.parameter.api.spi`; bản giả `FakeParameterValueCheck` |
| `LanChayLich` | `SchedulerRuns` (`start`, `finish`) |
| `SuKienConfig` | không có lớp riêng: cấu hình `spring.modulith.events.*` trong `application.yml` |
| `CoYeuCauThongBao` (`yeuCauThongBao`), `YeuCauThongBao` | `HasNotificationRequests` (`notificationRequests`), `NotificationRequest` |
| trường `YeuCauThongBao`: `moduleNguon`, `loaiSuKien`, `nhomNguon`, `banGhiNguon`, `khoaSuKien`, `muc`, `nctId`, `tangKhuVucId`, `nhomNguoiNhan`, `noiDung`, `noiDungRutGon`, `tieuDe`, `nguoiGayRa`, `baoNguoiGayRa`, `thoiDiemPhatSinh`, `thamSoHienThi`, `viecSauQuaDoi`, `doiTuongThongTin`, `maTruongHop` | `sourceModule`, `eventType`, `sourceGroup`, `sourceRecord`, `eventKey`, `level`, `residentId`, `floorAreaId`, `recipientGroups`, `content`, `shortContent`, `title`, `causedBy`, `notifyCauser`, `occurredAt`, `displayParams`, `afterDeath`, `informationSubject`, `q354Case` |
| `MucThongBao` (`NHE`, `TRUNG_BINH`, `KHAN_CAP`) | `NotificationLevel` (`LOW`, `MEDIUM`, `URGENT`) |
| `LoaiThongTin` (`CHUNG`, `SUC_KHOE`, `CHI_PHI`, `THAM_NOM`, `HOP_DONG`), `PhanNoiDung`, `BanGhiNguon` | `InformationType` (`GENERAL`, `HEALTH`, `COSTS`, `VISITS`, `CONTRACT`), `ContentPart`, `SourceRecord` |
| `NhomNguon` (nhóm của mục 17) | `SourceGroup` (`INCIDENT`, `HEALTH_ALERT`, `MEDICATION`, `OVERDUE_TASK`, `SCHEDULE_CHANGE`, `LICENSE_EXPIRING`, `COSTS`, `FAMILY_INFO`, `OTHER`) |
| `DoiTuongThongTin` (`VE_NGUOI_CAO_TUOI`, `VE_VIEC_NGUOI_THAN`) | `InformationSubject` (`ABOUT_RESIDENT`, `ABOUT_FAMILY_MATTER`) |
| `MaTruongHopQ354` (`KHAN_CAP_NGUOI_LIEN_HE_CHINH`, `DANH_SACH_TIEP_XUC`, `XAC_NHAN_LAI_NGUYEN_VONG`, `CHUYEN_VIEN`, `QUA_DOI`) | `Q354Case` (`PRIMARY_CONTACT_EMERGENCY`, `CONTACT_LIST`, `WISH_RECONFIRMATION`, `TRANSFER`, `DEATH`) |
| `NhomNhan` (`VaiTroToanVien`, `NguoiCoQuyenDuyet`…), `CachThay` (`MAC_DINH`, `KHONG_THAY`, `NGUOI_THAY_CHI_DINH`) | `RecipientGroup` (`FacilityRole`, `Approvers`…; 009 dịch các biến thể còn lại), `Substitution` (`Default`, `NoSubstitute`, `Designated`) |
| test `ModuleBoundaryTest`, `HopDongApiTest` | `ModuleBoundaryTest`, `ApiContractIT` |

### 6.6. Bảng dùng chung sau gộp *(bổ sung 2026-10-29, research R-15; sửa lần 2 cùng ngày)*

| Tên logic | Mã |
| --- | --- |
| phần `administration.shared` | `administration.shared` (`CatalogApi`, `StatusHistoryApi`, `RecordEventApi`, `SimpleRecordApi`, `ResidentModuleStateApi`) |
| `danh_muc_chung` (`loai`, `module`, `ma`, `ten`, `cha_id`, `thu_tu`, `trang_thai`, `ten_hieu_luc`, `thuoc_tinh`, `xac_nhan_boi`, `xac_nhan_luc`, `phien_ban`) | `catalog_item` (`catalog_type`, `module`, `code`, `name`, `parent_id`, `sort_order`, `status`, `active_name`, `attributes`, `confirmed_by`, `confirmed_at`, `version`) |
| `lich_su_chung` (`loai_doi_tuong`, `doi_tuong_id`, `thuoc_tinh`, `kieu`, `gia_tri_truoc`, `gia_tri`, `gia_tri_id`, `tu`, `den`, `lenh`, `nguoi_thuc_hien_id`, `do_he_thong`, `ly_do`, `can_cu_loai`, `can_cu_id`, `chi_tiet`, `khoa_mo`) | `status_history` (`entity_type`, `entity_id`, `attribute`, `kind`, `value_before`, `value`, `value_id`, `starts_at`, `ends_at`, `command`, `actor_id`, `by_system`, `reason`, `basis_type`, `basis_id`, `details`, `open_key`) |
| `kieu` `MOC`, `KHOANG` | `POINT`, `PERIOD` |
| `su_kien_chung` (`loai`, `noi_dung`, `tra_loi_cho_id`, `thoi_diem_xac_nhan`, `khoa_duy_nhat`) | `record_event` (`kind`, `content`, `reply_to_id`, `confirmed_at`, `unique_key`) |
| `ho_so_chung` (`loai`, `nct_id`, `trang_thai`, `noi_dung`, `hieu_luc_tu`, `hieu_luc_den`, `khoa_duy_nhat`, `khoa_luc`) | `simple_record` (`kind`, `resident_id`, `status`, `content`, `effective_from`, `effective_to`, `unique_key`, `locked_at`) |
| `trang_thai_nct` (`nct_id`, `module`, `du_lieu`) | `resident_module_state` (`resident_id`, `module`, `data`) |
| `moc_da_nhac` (`loai`, `moc`, `thoi_diem`) | `reminder_marker` (`kind`, `marker`, `marked_at`); lớp `ReminderMarkers` (`common`) |
| `khoa_ky_thuat` (`khoa`, `cap_nhat_luc`) | `technical_lock` (`lock_key`, `updated_at`); lớp `TechnicalLocks` (`common`) |
| cột `ma_khoa` của `danh_muc_chung` | `code_key` |
| bảng gộp của feature đã cài: `nguyen_vong` (001), `quyen_tai_khoan`, `thiet_bi_xac_thuc` (002), `lan_giao_nhan_thuoc` (006, mở rộng `medication_handover`) | `wish`, `permission_assignment`, `device_credential`, `medication_handover`; cột phân loại `kind` |
| cột `khoa_luc`, trigger `trg_<bang>_khoa_dong` | `locked_at`, `trg_<table>_row_lock` |
| `legacy` (`bang`, `id`) | `legacy` (`table`, `id`) |
| file log lần chạy job | `logs/jobs.log`; khóa MDC `jobRunId` |
| `@NhomDuLieu(NHIEU_NHOM)`, trường `khoaLuc` | `@DataGroup(MULTIPLE)`, trường `lockedAt` |
| bảng ánh xạ loại bản ghi cũ (SPI), tra loại cũ và mới | `LegacyRecordTypeSource` (`legacyRecordTypes`, `administration.shared.api.spi`); `RecordTypeAliases` (`withLegacy`, `current`) |
| xóa dòng bảng chung khi loại bỏ dữ liệu | `SharedRowRetention` (`deleteFor`, `deleteForResidents`, `RESIDENT`) |
| `ket_qua_ap_dung` sau gộp: cột kết quả áp dụng của `yeu_cau_phe_duyet`, `khoa_luc` | `approval_request` (`result_type`, `result_id`, `result_recorded_at`, `locked_at`) |
| thư mục file log job | thuộc tính `eldercare.logging.jobs-dir`; `logback-spring.xml`, logger `jobs` |
| khai nhóm dữ liệu của bảng không có entity; nhóm kỹ thuật | COMMENT của bảng `DataGroup=…`; giá trị `TECHNICAL` (chỉ ở COMMENT) |
| job dọn dữ liệu kỹ thuật (khóa chống lặp, sự kiện đã giao xong) | `CLEAN_TECHNICAL_DATA`; `TechnicalDataCleanup`, `TechnicalDataCleanupJob` (`common.scheduling`) |

## 7. Feature 002 *(bổ sung 2026-10-19, khi dịch `contracts/api-002.yaml`)*

Bảng, cột, lớp của 002 bổ sung vào mục này cùng thay đổi với mã (R-03). Tên đã có ở mục 6.5 (bản giả `administration.account.api` của 000) giữ nguyên khi 002 cài bản thật.

### 7.1. Đường dẫn API và header (tiền tố `/api/v1`)

| Tên logic | Mã |
| --- | --- |
| `/xac-thuc/dang-nhap`, `/xac-thuc/dang-nhap-qr`, `/xac-thuc/dang-xuat`, `/xac-thuc/doi-mat-khau` | `/auth/login`, `/auth/qr-login`, `/auth/logout`, `/auth/change-password` |
| *(bổ sung 2026-11-03, Q-399)* tự đặt lại mật khẩu: có dùng được không, xin mã, đặt mật khẩu mới | `/auth/password-reset/availability`, `/auth/password-reset/request`, `/auth/password-reset/confirm` |
| `/toi`, `/toi/quyen` | `/me`, `/me/permissions` |
| `/tai-khoan`, `/tai-khoan/{id}`, `/tai-khoan-nguoi-than`, `/tai-khoan-nguoi-cao-tuoi` | `/accounts`, `/accounts/{id}`, `/family-accounts`, `/resident-accounts` |
| `…/khoa`, `…/mo-khoa`, `…/mo-khoa-som`, `…/kich-hoat-lai`, `…/cap-lai-mat-khau` | `…/lock`, `…/unlock`, `…/early-unlock`, `…/reactivate`, `…/reset-password` |
| `…/gan-vai-tro`, `…/go-vai-tro`, `…/quyen?taiThoiDiem=` | `…/assign-role`, `…/remove-role`, `…/permissions?asOf=` |
| `…/thu-hep`, `…/khoi-phuc-quyen` | `…/restrict`, `…/restore-permission` |
| `…/gan-quyen-duyet-ke-hoach`, `…/go-quyen-duyet-ke-hoach` | `…/grant-care-plan-approval`, `…/revoke-care-plan-approval` |
| `…/ma-qr`, `…/go-thiet-bi`, `…/khoa-nguoi-cao-tuoi`, `…/mo-khoa-nguoi-cao-tuoi` | `…/login-qr-code`, `…/unbind-device`, `…/lock-resident`, `…/unlock-resident` |
| `/vai-tro`, `/vai-tro/{ma}/quyen` | `/roles`, `/roles/{code}/permissions` |
| `/nghiep-vu-chuyen-mon`, `…/ngung-hieu-luc` | `/professional-activities`, `…/deactivate` |
| `/dong-bo`, `/ban-ghi-cho-xem-lai`, `…/chap-nhan`, `…/tu-choi` | `/sync`, `/pending-review-records`, `…/accept`, `…/reject` |
| header `X-Thiet-Bi`, `X-Yeu-Cau-Tu`; cookie và security scheme `phien` | `X-Device-Id`, `X-Requested-From`; `session` |
| tag `xac-thuc`, `tai-khoan`, `phan-quyen`, `nghiep-vu-chuyen-mon`, `ngoai-tuyen` | `auth`, `accounts`, `permissions`, `professional-activities`, `offline` |

### 7.2. Schema và trường JSON

| Tên logic | Mã |
| --- | --- |
| đăng nhập: `tenDangNhap`, `matKhau`, `kenh`, `tokenThietBi`, `buocDoiMatKhau`, `nguoiDung` | `username`, `password`, `channel`, `deviceToken`, `mustChangePassword`, `user` |
| `matKhauHienTai`, `matKhauMoi`, `matKhauTam`, `moKhoaLuc` | `currentPassword`, `newPassword`, `temporaryPassword`, `unlocksAt` |
| `NguoiDung` (`taiKhoanId`, `loai`, `hoTen`, `vaiTro`) | `UserInfo` (`accountId`, `type`, `fullName`, `roles`) |
| `TaiKhoan` (`tenDangNhap`, `nhanVienId`, `nguoiThanId`, `nguoiCaoTuoiId`, `trangThai`, `nguyenNhanKhoa`, `khoaTamDen`, `lanDangNhapCuoi`, `vaiTro`, `lenhDuocPhep`) | `Account` (`username`, `staffId`, `familyMemberId`, `residentId`, `status`, `lockCause`, `lockedUntil`, `lastLoginAt`, `roles`, `allowedCommands`) |
| `thietBiGan` (`ganLuc`, `maThietBi`) | `boundDevice` (`boundAt`, `deviceId`) |
| `TaiKhoanVaMatKhauTam`, `TrangTaiKhoan` | `AccountWithTemporaryPassword`, `AccountPage` (`items`, `nextCursor`) |
| `XacMinh` (`cach`, `soGiayTo`, `soDienThoaiDaGoi`) | `Verification` (`method`, `idDocumentNumber`, `calledPhoneNumber`) |
| `XacNhanNguoiCaoTuoi` (`cach`, `voiNhanVienId`); trường `xacNhan` | `ResidentConsent` (`method`, `withStaffId`); trường `consent` |
| `nguoiThayPhuTrach`, `daXacMinhTrucTiep` | `replacementDepartmentLead`, `verifiedInPerson` |
| `VaiTro` (`ma`, `ten`, `loaiTaiKhoan`, `phamViThucHien`); `MaVaiTro`, `MaVaiTroNhanVien` | `RoleInfo` (`code`, `name`, `accountType`, `executionScope`); `RoleCode`, `StaffRoleCode` |
| `QuyenHieuLuc` (`chucNang`, `loaiThaoTac`, `phamVi`, `maChuThich`, `nguon`) | `EffectivePermission` (`function`, `operation`, `scope`, `footnoteCodes`, `source`) |
| `QuyenVaiTro` (`toiDa`, `thuHep`, `hienHanh`) | `RolePermissions` (`maximum`, `restrictions`, `current`) |
| `QuyenTaiKhoan` (`taiThoiDiem`, `thuHepTheoVaiTro`, `thuHepTheoTaiKhoan`, `ganThem`, `hienHanh`) | `AccountPermissions` (`asOf`, `roleRestrictions`, `accountRestrictions`, `additionalGrants`, `current`) |
| `ThuHep` (`kieu`, `hieuLucTu`); thân lệnh thu hẹp `nhomChiTieu`, `kenh`; khôi phục `thuHepId` | `Restriction` (`kind`, `effectiveFrom`); `metricGroup`, `channel`; `restrictionId` |
| thân lệnh chỉ có lý do (`requestBodies/LyDo`), `ThuHep`, `KhoiPhuc` | `ReasonBody`, `RestrictBody`, `RestoreBody` |
| `IfMatchQuyen` (`phien_ban_quyen`) | `IfMatchPermission` (`permission_version`) |
| `NghiepVuChuyenMon` (`loaiGiayPhepYeuCau`, `phamViHanhNgheYeuCau`, `canPhamViCoSo`, `macDinh`); `NghiepVuChuyenMonGhi` | `ProfessionalActivity` (`requiredLicenseType`, `requiredPracticeScopes`, `requiresFacilityScope`, `isDefault`); `ProfessionalActivityInput` |
| đồng bộ: `lanKetNoiCuoi`, `banGhi` (`loaiBanGhi`, `noiDung`), `phienKetThuc`, `nguoiCaoTuoiTrongPhamVi`, `ketQua` (`ketQua`, `thamChieu`) | `lastConnectedAt`, `records` (`recordType`, `content`), `sessionEnded`, `residentsInScope`, `results` (`result`, `reference`) |
| mã QR: `ma`, `hetHanLuc` | `code`, `expiresAt` |
| khóa tài khoản người cao tuổi: `canCu` | `basis` |
| `Loi`: `lopKhongThoa`, `giayPhep` (`ten`, `ngayHetHan`) | `Problem`: `failedLayer`, `license` (`name`, `expiryDate`) |

### 7.3. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| kênh `WEB`, `APP_NHAN_VIEN`, `APP_NGUOI_THAN`, `APP_NGUOI_CAO_TUOI` | `WEB`, `STAFF_APP`, `FAMILY_APP`, `RESIDENT_APP` (cùng khóa của CFG-M15-10) |
| *(bổ sung 2026-10-28, đổi api-002)* kênh đăng nhập app chung | `MOBILE_APP`: chỉ ở thân `POST /auth/login` (`LoginBody.Channel`, `Channel.session()`); kênh phiên gán theo loại tài khoản, vẫn là `STAFF_APP`, `FAMILY_APP` |
| loại tài khoản `NHAN_VIEN`, `NGUOI_THAN`, `NGUOI_CAO_TUOI` | `STAFF`, `FAMILY_MEMBER`, `RESIDENT` |
| trạng thái tài khoản `HOAT_DONG`, `KHOA_TAM`, `KHONG_HOAT_DONG`, `DA_KHOA` | `ACTIVE`, `TEMPORARILY_LOCKED`, `INACTIVE`, `LOCKED` |
| loại thao tác `XEM`, `TAO`, `SUA`, `XAC_NHAN`, `DUYET`, `CHOT`, `CHUYEN_MON` | `Operation` `VIEW`, `CREATE`, `UPDATE`, `CONFIRM`, `APPROVE`, `FINALIZE`, `PROFESSIONAL` |
| phạm vi `TOAN_VIEN`, `PHAN_CONG`, `QUAN_HE` | `FACILITY`, `ASSIGNED`, `RELATIONSHIP` |
| nguồn quyền `VAI_TRO`, `NHIEM_VU`, `GAN_THEM` | `ROLE`, `DUTY`, `ADDITIONAL_GRANT` |
| kiểu thu hẹp `BO_THAO_TAC`, `HA_PHAM_VI`; kênh thu hẹp `DASHBOARD`, `BAO_CAO` | `REMOVE_OPERATION`, `LOWER_SCOPE`; `DASHBOARD`, `REPORT` |
| cách xác minh `TRUC_TIEP_GIAY_TO`, `GOI_LAI_SO_DA_DANG_KY` | `IN_PERSON_ID_DOCUMENT`, `CALLBACK_REGISTERED_NUMBER` |
| xác nhận người cao tuổi `YEU_CAU`, `DONG_Y` | `REQUESTED`, `AGREED` |
| căn cứ khóa tài khoản người cao tuổi `NCT_TU_YEU_CAU`, `CO_LY_DO` | `RESIDENT_REQUESTED`, `WITH_REASON` |
| lớp không thỏa `VAI_TRO`, `PHAM_VI`, `PHAP_LY`, `DU_LIEU_NGUON` | `ROLE`, `SCOPE`, `LEGAL`, `SOURCE_DATA` |
| trạng thái nghiệp vụ chuyên môn `HIEU_LUC`, `NGUNG_HIEU_LUC` | `ACTIVE`, `INACTIVE` (như `CatalogStatus`) |
| `ChucNang` (76 dòng Phụ lục 27, ví dụ `CHI_PHI_DIEU_CHINH`, `DASHBOARD_BAO_CAO`, `NHAT_KY_THAY_DOI`); `LoaiThaoTac`; `KhoaQuyen` | enum `administration.account.api.dto.Function` (`COST_ADJUSTMENT`, `DASHBOARD_REPORT`, `AUDIT_LOG`…; danh sách đủ, cùng thứ tự Phụ lục 27, `label()` là tên dòng); `Operation`; `PermissionKey(Function, Operation)` |
| *(bổ sung 2026-10-27, Q-391)* `ChucNang` "Danh mục công việc chăm sóc" (loại công việc, giá trị kết quả, loại lý do Không thực hiện của 005) | `Function.CARE_TASK_CATALOG` |
| loại yêu cầu `M15.KHOA_TAI_KHOAN_NCT`; loại bản ghi ví dụ `M04.KET_QUA_CONG_VIEC` | `M15.RESIDENT_ACCOUNT_LOCK`; `M04.CARE_TASK_RESULT` (004 chốt khi dịch hợp đồng của mình) |

### 7.4. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `DANG_NHAP_KHONG_THANH` | `LOGIN_FAILED` |
| `TAI_KHOAN_KHOA_TAM`, `TAI_KHOAN_CAN_LIEN_HE_VIEN`, `TAI_KHOAN_DA_KHOA` | `ACCOUNT_TEMPORARILY_LOCKED`, `ACCOUNT_CONTACT_FACILITY`, `ACCOUNT_LOCKED` |
| `MAT_KHAU_KHONG_DAT` | `PASSWORD_POLICY_NOT_MET` |
| *(bổ sung 2026-11-03, Q-399)* `MA_DAT_LAI_KHONG_HOP_LE` | `RESET_CODE_INVALID` |
| `HO_SO_DA_CO_TAI_KHOAN`, `DA_CO_TAI_KHOAN` | `PROFILE_ALREADY_HAS_ACCOUNT`, `ALREADY_HAS_ACCOUNT` |
| `NHAN_VIEN_NGHI_VIEC`, `CHUC_DANH_KHONG_DUNG_HE_THONG` | `STAFF_RESIGNED`, `POSITION_NOT_SYSTEM_USER` |
| `KHONG_CON_QUAN_HE`, `XAC_MINH_KHONG_HOP_LE` | `RELATIONSHIP_ENDED`, `INVALID_VERIFICATION` |
| `KHONG_CON_QUAN_LY_VIEN`, `CAN_NGUOI_THAY_PHU_TRACH` | `NO_MANAGER_LEFT`, `REPLACEMENT_LEAD_REQUIRED` |
| `NGUYEN_NHAN_KHOA_CON_TON_TAI` | `LOCK_CAUSE_STILL_EXISTS` |
| `KHONG_GAN_VAI_TRO_CHO_TAI_KHOAN_NGUOI_CAO_TUOI` | `RESIDENT_ACCOUNT_HAS_NO_ROLES` |
| `VUOT_QUYEN_TOI_DA` | `EXCEEDS_MAXIMUM_PERMISSION` |
| `MUC_MAC_DINH` | `DEFAULT_ITEM` |
| `KHONG_DANG_LUU_TRU`, `KHONG_PHAI_TAI_KHOAN_NGUOI_CAO_TUOI` | `NOT_IN_RESIDENCE`, `NOT_RESIDENT_ACCOUNT` |
| `MA_QR_KHONG_HOP_LE`, `THIET_BI_DA_GAN`, `KHONG_CO_THIET_BI_GAN` | `INVALID_QR_CODE`, `DEVICE_ALREADY_BOUND`, `NO_BOUND_DEVICE` |
| *(bổ sung 2026-10-20, T041)* phiên buộc đổi mật khẩu gọi đường khác (research A-03) | `PASSWORD_CHANGE_REQUIRED` (403) |

### 7.5. Gói và kiểu Java của `administration.account` (T002, T008 → T010)

*(Sửa 2026-10-21, tổ chức lại theo research R-02 mục "Cấu trúc trong module")* Đường dẫn tính từ `administration.account`: `api/` (interface), `api/dto/`, `api/spi/`, `api/events/`; `web/`, `web/request/`, `web/response/`, `web/security/`; `internal/<auth, permission, scope, legal, offline>/{service, domain, persistence, config}`, `internal/domain/` (`Account`, trước là `AccountStore.AccountRow`), `internal/persistence/` (`AccountStore`); `scheduling/`. Đường dẫn dạng cũ `auth/internal/…` trong plan, tasks là lịch sử.

| Tên logic | Mã |
| --- | --- |
| gói `administration.taikhoan.{api, xacthuc, quyen, phamvi, phaply, ngoaituyen, lich}` | `administration.account.{api, auth, permission, scope, legal, offline, scheduling}`; sự kiện ở `administration.account.api.events` |
| `NguoiDungHienTai` thêm `loai`, `nhanVienId`, `nguoiThanId`, `nguoiCaoTuoiId`, `maThietBi`, `kenh` | `CurrentUser` thêm `type`, `staffId`, `familyMemberId`, `residentId`, `deviceId`, `channel` |
| `LoaiTaiKhoan`, `KenhTruyCap`, trạng thái tài khoản | `AccountType`, `AccessChannel`, `AccountStatus` (giá trị ở mục 7.3) |
| `KetQuaQuyen` (`duocPhep`, `lopKhongThoa`, `giayPhep`, `canCu`); `Lop`; `GiayPhepView` | `PermissionResult` (`allowed`, `failedLayer`, `license`, `basis`; `allowed(basis)`, `denied(layer)`, `deniedByLicense(license)`); `FailedLayer`; `LicenseView` (`name`, `expiryDate`) |
| căn cứ quyền `VAI_TRO`, `NHIEM_VU(loai)`, `NGOAI_PHAM_VI`, `BO_LAP_LICH` | `PermissionBasis` (`kind`: `ROLE`, `DUTY`, `OUT_OF_SCOPE`, `SCHEDULER`; `duty`) |
| nhiệm vụ: Người phụ trách ca, Trưởng tầng được giao, Phụ trách bộ phận, Trưởng đoàn, Người đi cùng, Người nhận tại tầng | `Duty`: `SHIFT_LEAD`, `ASSIGNED_FLOOR_MANAGER`, `DEPARTMENT_LEAD`, `GROUP_LEADER`, `COMPANION`, `FLOOR_RECEIVER` |
| `QuyenApi` `coQuyenTaiThoiDiem`, `tapQuyen`, `lenhDuocPhep` | `PermissionApi` `hasPermissionAt`, `permissions`, `allowedCommands` |
| `PhamViApi` `boLoc`, `boLocLichSu`, `nguoiQuyetDinhXemLai`; `PhamViLoc` (`toanVien`, `tangKhuVucIds`, `phongIds`, `nguoiCaoTuoiIds`); `NguoiNhanXemLai` | `ScopeApi` `filter`, `historyFilter`, `reviewDecider`; `ScopeFilter` (`facilityWide`, `floorAreaIds`, `roomIds`, `residentIds`; `FACILITY`); `ReviewRecipient` (`accountIds`) |
| `TaiKhoanApi` `khoaDoNghiViec`, `xetLaiNguoiThan`, `theoNhanVien`, `theoNguoiThan`, `theoNguoiCaoTuoi`; `TaiKhoanView` | `AccountApi` `lockOnResignation`, `recheckFamilyMember`, `byStaff`, `byFamilyMember`, `byResident`, `byId` (`theoId`, bổ sung 2026-10-22), `activeWithRole` (`dangHoatDongCoVaiTro`, bổ sung 2026-10-22); `AccountView` (`accountId`, `type`, `status`, `roles`, `staffId`) |
| chú thích `@CanQuyen(chucNang, thaoTac)`, `@NhomTruong` | `@RequiresPermission(function, operation)`, `@FieldGroup` |
| SPI `PhanBoGiuongApi` (`dangMo`, `nguoiCaoTuoiTai`), `PhanBoView` | `BedAllocationApi` (`openAt`, `residentsIn`), `BedAllocationView` (`allocationId`, `bedId`, `roomId`, `floorAreaId`) |
| SPI `TenTangKhuVucApi` (`ten`) *(bổ sung 2026-10-27)* | `FloorAreaNameApi` (`name`); bản cài M03 `FloorAreaNamesForAccount` |
| SPI `QuanHeNguoiThanApi` (`hieuLuc`, `soDienThoaiDaDangKy`, `laNguoiDaiDienHieuLuc`, `thuocNguoiCaoTuoi`, `loaiQuanHe`, `quyenDangBat`), `QuanHeView` | `FamilyRelationshipApi` (`active`, `registeredPhoneNumber`, `isActiveRepresentative`, `belongsToResident`, `relationship`, `rightEnabled` với `common.notification.FamilyRight`), `FamilyRelationshipView` (`relationshipId`, `residentId`) |
| `LoaiQuanHe` (`VO_CHONG`, `CON`, `CHAU`, `ANH_CHI_EM`, `KHAC`; 012 `quan_he_nguoi_than.quan_he`) *(bổ sung 2026-10-30)* | `FamilyRelationshipType` (`SPOUSE`, `CHILD`, `GRANDCHILD`, `SIBLING`, `OTHER`) |
| SPI `KetThucLuuTruApi.thoiDiemTrangThaiCuoi` | `ResidencyEndApi.finalStatusAt` |
| SPI phần `NguoiCaoTuoiApi` 002 dùng (`trangThai`, `hoTen`; 2026-10-24, M01 cài để tránh vòng M15 ↔ M01) | `ResidentProfileApi.profile` → `ResidentProfileView` (`fullName`, `inResidence`, `finalStatus`) |
| `TaiKhoanApi.khoaKhiNctTrangThaiCuoi` (M01 gọi từ listener `NguoiCaoTuoiDaChuyenTrangThai`) | `AccountApi.lockResidentOnFinalStatus` |
| SPI `NgoaiTuyenHandler` (`loaiBanGhi`, `khoaQuyen`, `doiTuong`, `kiemTraNoiDung`, `apDung`, `batBuocTrucTuyen`) | `OfflineHandler` (`recordType`, `permissionKey`, `target`, `validate`, `apply`, `requiresOnline`) |
| SPI `DieuKienChuThich` (`maChuThich`, `thoa`), `GanPhamViProvider` (`loaiDoiTuong`, `ganPhamVi`) | `FootnoteCondition` (`footnoteCode`, `isSatisfied`), `ScopeAssignmentProvider` (`entityType`, `assignScope`) |
| ngoại lệ `DuLieuQuyenKhongHopLe` (`nguon`, `loaiLoi`, `banGhiId`) | `InvalidPermissionDataException` (`source`, `errorKind`, `recordId`) |
| SPI `HoSoNhanVienApi` (`trangThai`, `chucDanhCoVaiTro`); trạng thái `DANG_LAM_VIEC`, `NGHI_VIEC` | `StaffProfileApi` (`status`, `positionRole`); `StaffStatus` `WORKING`, `RESIGNED` |
| SPI `GiayPhepApi` (`hieuLuc`, `sapHetHan`) | `LicenseApi` (`valid`, `expiringWithin`); `LicenseValidity`, `ExpiringLicense` |
| SPI `DanhMucPhamViHanhNgheApi`; mục phạm vi hành nghề | `PracticeScopeCatalogApi` (`find`); `PracticeScopeView` |
| SPI `PhanCongApi` (`dangHieuLuc`, `lichSu`, `nhanVienPhanCongChoNguoiCaoTuoi`, `nhanVienPhanCongChoPhong`); loại phân công giao trưởng tầng, thành viên ca, người phụ trách ca | `AssignmentApi` (`activeAt`, `history`, `staffAssignedToResident`, `staffAssignedToRoom`), `AssignmentView`; `AssignmentKind` `FLOOR_MANAGER`, `SHIFT_MEMBER`, `SHIFT_LEAD` |
| SPI `NhiemVuApi` (`nguoiPhuTrachCa`, `truongTangDuocGiao`, `nguoiThayTruongTang`, `caKhongCoTruongTang`, `phuTrachBoPhan`, `laPhuTrachBoPhan`, `boPhanCua`, `truongDoan`, `nguoiDiCung`, `nguoiNhanTaiTang`) | `DutyApi` (`shiftLead`, `assignedFloorManager`, `floorManagerSubstitute`, `shiftHasNoFloorManager`, `departmentLead`, `isDepartmentLead`, `departmentOf`, `groupLeader`, `companions`, `floorReceivers`) |
| sự kiện `TaiKhoanDaTao`, `TaiKhoanDoiTrangThai`, `BanGhiDongBoSauMatQuyen`, `BanGhiChoXemLai`, `LoiDuLieuQuyen`, `GiayPhepSapHetHan` | `AccountCreated`, `AccountStatusChanged`, `RecordSyncedAfterPermissionLoss`, `RecordPendingReview`, `PermissionDataError`, `LicenseExpiring` |
| bản giả `BT/support/gia002/` | `support/fake/administration/` (`FakeM15Config`, `FakeStaffProfiles`, `FakeLicenses`, `FakePracticeScopes`, `FakeAssignments`, `FakeDuties`, `FakeBedAllocations`, `FakeFamilyRelationships`, `FakeResidencyEnds`) |
| `xacthuc/internal/PhienService`, `xacthuc/web/XacThucTokenFilter` (T011); phiên đang mở; token phiên | `internal/auth/service/SessionService` (`resolve`), `web/security/SessionTokenFilter`, `web/security/SecurityConfig`; `ActiveSession` (`accountId`, `type`, `staffId`, `familyMemberId`, `roles`, `deviceId`, `channel`), `SessionAuthentication`; `SessionTokens` (`generate`, `hash`, `cookie`) |
| `xacthuc/internal/NguoiDungHienTaiImpl` (T012); bản Bộ lập lịch | `internal/auth/service/CurrentUserImpl`; `SchedulerUser`, `SchedulerUserAspect`; `ActiveSession` cài `CurrentUser` |
| `quyen/internal/TinhQuyenVaiTro` (T013); phạm vi `TOAN_VIEN`, `PHAN_CONG`, `QUAN_HE`; phương án quyền | `internal/permission/service/RolePermissionCalculator` (`current`, `at`); `PermissionScope` (`FACILITY`, `ASSIGNED`, `RELATIONSHIP`); `RoleGrant` (`scope`, `footnotes`, `source`), `RoleGrants` (`add`, `lower`), `GrantSource` (`ROLE`, `ADDITIONAL_GRANT`) |
| `xacthuc/internal/KhoiTaoQuanLyVien` (T016); lệnh dòng lệnh `khoi-tao-quan-ly-vien`; `HoSoNhanVienApi.taoHoSoToiThieu` | `internal/auth/service/FirstManagerBootstrap` (`run`, `Result`), `FirstManagerBootstrapRunner` (`eldercare.bootstrap-manager.run-once`, `.username`, `.full-name`), `PasswordConfig`; `StaffProfileApi.createMinimal(fullName, positionRole)` |
| hành động nhật ký `TK_TAO`, `VT_GAN`; đối tượng tài khoản; người thực hiện `KHOI_TAO` | `ACCOUNT_CREATED`, `ROLE_ASSIGNED`; `M15.ACCOUNT`; `actor_type = BOOTSTRAP` qua `AuditLogApi.writeAsBootstrap` |
| `quyen/internal/QuyenService` (T022); `phamvi/internal/PhamViService` (`trongPhamVi`, phần kéo lên US1); phân loại chú thích | `internal/permission/service/PermissionService`; `internal/scope/service/ScopeService` (`inScope`); `FootnoteRules` (`CONDITIONS`, `DUTIES`, `blockedAtRoleLayer`); nguồn quyền thêm `GrantSource.DUTY` |
| `quyen/internal/QuyenXemTruongService`, `GioiHanTruongSerializerModifier` (T023); `truongBiAn`; DTO có đối tượng | `internal/permission/service/FieldVisibilityService`, `FieldGroupSerializerModifier`, `FieldVisibilityJacksonConfig`; trường JSON `hiddenFields`; `api.VisibilityTarget` (`visibilityTarget()`) |
| *(bổ sung 2026-10-24, "Đổi api" cho 001 FR-023m)* DTO tự khai trường bị ẩn (`DtoTuAnTruong`, `truongTuAn`) | `administration.account.api.SelfHiddenFields` (`selfHiddenFields`); `FieldGroupSerializerModifier` bỏ các trường đó và ghi vào `hiddenFields` |
| `phamvi/internal/LoiDuLieuQuyenService` (T024); bảng `loi_du_lieu_quyen` (`nguon`, `ban_ghi_id`, `loai_loi`, `lan_dau_luc`, `lan_cuoi_luc`, `so_lan`, `da_khac_phuc_luc`); hành động `QUYEN_TU_CHOI_DU_LIEU_NGUON` | `internal/scope/service/PermissionDataErrors` (`record`, `markFixed`); `permission_data_error` (`source`, `record_id`, `error_kind`, `first_at`, `last_at`, `occurrences`, `fixed_at`, `open_key`); `PERMISSION_DENIED_SOURCE_DATA` (đối tượng `M15.PERMISSION_DATA_ERROR`) *(R-15: bảng đã gộp trong dòng này xem mục bảng sau gộp của feature)* |
| `quyen/web/ToiController` (T025) | `web/MeController` (`UserInfo`, `EffectivePermission`) |
| web `chung/quyen/useQuyen.js`, test `AnNutTheoQuyen.test.jsx` (T026) | `frontend/src/common/permissions/permissions.jsx` (`PermissionsProvider`, `usePermissions`, `IfAllowed`, `isAllowed`), `HideByPermission.test.jsx` |
| test T018 `MaTranQuyenIT`, `QuyenBaLopIT`; T019 `GioiHanTruongIT`; T020 `SuCoNgoaiPhamViIT` | `PermissionMatrixIT`, `ThreeLayerPermissionIT`; `FieldVisibilityIT`; `IncidentOutOfScopeIT`; T024 `PermissionDataErrorIT`; T025 `MeControllerIT` |
| test `CanQuyenArchTest` (T015) | `RequiresPermissionArchTest` (`SELF_CHECKED`: lệnh tự kiểm quyền) |
| `quyen/internal/CanQuyenInterceptor` (T014) | `internal/permission/service/RequiresPermissionAspect`; trường lỗi `failedLayer`, `license` qua `PermissionDeniedException.properties()` |
| *(bổ sung 2026-10-20)* `xacthuc/internal/TaiKhoanService`, `TaiKhoanApiImpl` (T031, T032, gộp một lớp); kho dữ liệu tài khoản; mật khẩu tạm | `internal/auth/service/AccountService`, `AccountStore` (`AccountRow`), `TemporaryPasswords`, `AccountAuditActions`; `web/AccountController` (T034) |
| `quyen/internal/GanVaiTroService` (T033) | `internal/permission/service/RoleAssignmentService` (`assign`, `remove`, `current`), `AccountPermissionsQuery`, `PermissionViews`; `web/response/UserInfo` |
| `TaiKhoanApi.hoTen`, `HoSoNhanVienApi.hoTen`, `QuanHeNguoiThanApi.hoTenNguoiThan`, `NhiemVuApi.thayPhuTrachBoPhan` (T032a, T031) | `AccountApi.fullName`, `StaffProfileApi.fullName`, `FamilyRelationshipApi.familyMemberName`, `DutyApi.replaceDepartmentLead` |
| `xacthuc/internal/DangNhapService`, `MatKhauService` (T041, T042); quy tắc đăng nhập | `internal/auth/service/LoginService` (`login`, `logout`, `changePassword`), `LoginRules` (`decide`, `effective`); `web/AuthController` (T044) |
| `TaiKhoanService` mở khóa sớm, kích hoạt lại (T042) | `AccountService.earlyUnlock` (căn cứ `NURSING_HOME_MANAGER`, `SHIFT_LEAD`, `FLOOR_MANAGER`), `AccountService.reactivate` |
| `lich/ChuyenKhongHoatDong` (T043) | `scheduling/MarkInactiveAccountsJob` (`MARK_INACTIVE_ACCOUNTS`, `eldercare.jobs.mark-inactive-accounts.cron`), `internal/auth/service/InactiveAccounts` |
| hành động nhật ký `TK_KHOA`, `TK_MO_KHOA`, `TK_CAP_LAI_MAT_KHAU`, `VT_GO`, `DANG_NHAP`, `DANG_NHAP_THAT_BAI`, `TK_KHOA_TAM`, `TK_HET_KHOA_TAM`, `TK_MO_KHOA_SOM`, `TK_KHONG_HOAT_DONG`, `TK_KICH_HOAT_LAI`, `TK_DOI_MAT_KHAU` | `ACCOUNT_LOCKED`, `ACCOUNT_UNLOCKED`, `PASSWORD_RESET`, `ROLE_REMOVED`, `LOGIN`, `LOGIN_FAILED`, `ACCOUNT_TEMPORARILY_LOCKED`, `TEMPORARY_LOCK_EXPIRED`, `ACCOUNT_EARLY_UNLOCKED`, `ACCOUNT_INACTIVE`, `ACCOUNT_REACTIVATED`, `PASSWORD_CHANGED` |
| `GiayPhepCoSoApi` (`coPhamVi`, `sapHetHan`), giấy phép cơ sở sắp hết hạn | `FacilityLicenseApi` (`coversScope`, `expiringWithin`), `ExpiringFacilityLicense`; bản giả `FakeFacilityLicenses` |
| người thực hiện "chưa đăng nhập" của lần đăng nhập thất bại (chủ dự án duyệt 2026-10-20) | `actor_type = ANONYMOUS` (V002_05_1), `AuditLogApi.writeAnonymous`; `Actor.type` của api-000 thêm `ANONYMOUS` |
| *(bổ sung 2026-10-20, Phase 6)* `phamvi/internal/PhamViService` (T052) `trongPhamVi(nd, doiTuong, thoiDiem)`; phân công góp vào phạm vi theo khoảng CFG-M15-07 | `internal/scope/service/ScopeService`, `ScopeApi.inScope(user, target, at)`; `internal/scope/service/ActiveAssignments` (`at`, `during`, `floorOf`, `margin`) |
| `quyen/internal/QuyenTheoNhiemVu` (T053), `NguoiDuyetService` (T054) | `internal/permission/service/DutyPermissions` (`grant`), `internal/permission/service/ApproverService` |
| `ngoaituyen/internal/DongBoService` (T056), `ChoXemLaiService` (T057); lô phong bì, kết quả từng bản ghi | `internal/offline/service/SyncService` (`SyncRecord`, `RecordResult`, `Report`), `PendingReviewService` (`page`, `accept`, `reject`), `PendingReviewRecords`, `OfflineHandlers` |
| `ngoaituyen/web/DongBoController`, `ChoXemLaiController` (T058); trường phản hồi `phienKetThuc`, `nguoiCaoTuoiTrongPhamVi` | `web/SyncController`, `PendingReviewController`; `sessionEnded`, `residentsInScope`; schema `PendingReviewRecord`, `PendingReviewRecordPage` (api-002, bổ sung 2026-10-20) |
| trường `BanGhiChoXemLai` *(bổ sung 2026-10-27)* `tenNguoiGhi`, `tenTangKhuVuc`, `phienBan` | `recorderName`, `floorAreaName`, `version` |
| `/ung-vien-mo-khoa-som` *(bổ sung 2026-10-27, FR-019c)*; `UngVienMoKhoaSom` (`taiKhoanId`, `hoTen`, `khoaTamDen`, `phienBan`) | `/early-unlock-candidates`; `EarlyUnlockCandidate` (`accountId`, `fullName`, `lockedUntil`, `version`), `EarlyUnlockCandidatePage` |
| lý do từ chối khi đồng bộ: loại không có handler, bắt buộc trực tuyến, nội dung sai, tài khoản không Hoạt động, không có quyền tại thời điểm thiết bị (kèm lớp), khóa chống lặp dùng lại; lý do kèm nhãn: mất quyền trước khi đồng bộ | `UNKNOWN_RECORD_TYPE`, `REQUIRES_ONLINE`, `INVALID_CONTENT`, `ACCOUNT_NOT_ACTIVE`, `PERMISSION_DENIED_<lớp>`, `IDEMPOTENCY_KEY_REUSED`, `IDEMPOTENCY_IN_PROGRESS`; `PERMISSION_LOST_BEFORE_SYNC` |
| hành động nhật ký `NT_CHO_XEM_LAI`, `NT_CHAP_NHAN`, `NT_TU_CHOI`; đối tượng bản ghi chờ xem lại | `OFFLINE_PENDING_REVIEW`, `OFFLINE_RECORD_ACCEPTED`, `OFFLINE_RECORD_REJECTED`; `M15.PENDING_REVIEW_RECORD` |
| *(bổ sung 2026-10-21, Phase 7)* `phaply/internal/DieuKienPhapLyService` (T064), `NghiepVuChuyenMonService` (T065), job `CanhBaoGiayPhepSapHetHan` (T066); `phaply/web/NghiepVuChuyenMonController` | `internal/legal/service/LegalConditionService` (`denial`), `ProfessionalActivityService`, `ProfessionalActivities`, `ProfessionalActivity`, `LicenseExpiryWarnings`; `scheduling/WarnExpiringLicensesJob` (`WARN_EXPIRING_LICENSES`, `eldercare.jobs.warn-expiring-licenses.cron`); `web/ProfessionalActivityController` |
| *(bổ sung 2026-10-21, Phase 8)* job `KhoaSauKetThucLuuTru` (T070); `TaiKhoanService.taoNguoiThan`, endpoint `POST /tai-khoan-nguoi-than` (T069); `xetLaiNguoiThan` | `scheduling/LockAfterResidencyEndJob` (`LOCK_AFTER_RESIDENCY_END`, `eldercare.jobs.lock-after-residency-end.cron`), `internal/auth/service/ResidencyEndLocks`, `AccountService.lockIfResidenciesExpired`; `AccountService.createFamily`, `web/FamilyAccountController` (`POST /family-accounts`); `AccountApi.recheckFamilyMember` |
| `NghiepVuChuyenMonApi.loaiYeuCau(maNghiepVu)`; mục mặc định chẩn đoán và kê đơn nội bộ, phát thuốc và xác nhận liều; `GiayPhepApi` thêm giấy phép gần nhất; giấy phép sắp hết hạn thêm loại | `ProfessionalActivityApi.requiredLicenseType(activityCode)`; `INTERNAL_DIAGNOSIS_PRESCRIBING`, `MEDICATION_ADMINISTRATION`; `LicenseApi.latest(staffId, licenseType)`; `ExpiringLicense.licenseType` |
| hành động nhật ký `NVCM_TAO`, `NVCM_SUA`, `NVCM_NGUNG`; đối tượng; mã lỗi mục mặc định, tên trùng | `PROFESSIONAL_ACTIVITY_CREATED`, `PROFESSIONAL_ACTIVITY_UPDATED`, `PROFESSIONAL_ACTIVITY_DEACTIVATED`; `M15.PROFESSIONAL_ACTIVITY`; `DEFAULT_ITEM`, `DUPLICATE_NAME` |
| *(bổ sung 2026-10-21, Phase 9)* thứ tự tính quyền vai trò (T073); dòng thu hẹp | `internal/permission/domain/RolePermissionRules` (`combine`, `ofRole`), `RestrictionRule`; `RolePermissionCalculator.ofRole` |
| `quyen/internal/ThuHepQuyenService` (T075), `quyen/web/PhanQuyenController` (T076); thân lệnh thu hẹp, khôi phục; kiểu `BO_THAO_TAC`, `HA_PHAM_VI`; kênh dashboard, báo cáo | `internal/permission/service/PermissionChangeService` (`restrictRole`, `restoreRole`, `restrictAccount`, `restoreAccount`, `grantCarePlanApproval`, `revokeCarePlanApproval`), `internal/permission/persistence/PermissionChanges`, `web/PermissionController`; `RestrictBody`, `RestoreBody`; `RestrictionKind` (`REMOVE_OPERATION`, `LOWER_SCOPE`); `RestrictBody.Channel` (`DASHBOARD`, `REPORT`) |
| quyền của vai trò, thông tin vai trò; tập quyền tại thời điểm | `PermissionViews.RolePermissions` (`maximum`, `restrictions`, `current`), `RoleInfo`; `RoleAssignmentService.at`, `AccountPermissionsQuery.at`, `AccountStore.rolesAt`, `statusAt` |
| hành động nhật ký `QUYEN_THU_HEP`, `QUYEN_KHOI_PHUC`, `QUYEN_GAN_THEM`, `QUYEN_GO_THEM`; đối tượng vai trò | `PERMISSION_RESTRICTED`, `PERMISSION_RESTORED`, `ADDITIONAL_PERMISSION_GRANTED`, `ADDITIONAL_PERMISSION_REVOKED` (`PermissionAuditActions`); `M15.ROLE` (mã đối tượng là mã vai trò) |
| test T073, T074 `ThuTuThuHepTest`, `ThuHepKhongVuotIT`, `DoiQuyenDongThoiIT`, `QuyenTaiThoiDiemIT`, `PhanQuyenChiQuanLyIT` | `RestrictionOrderTest`, `RestrictWithinMatrixIT`, `ConcurrentPermissionChangeIT`, `PermissionsAsOfIT`, `OnlyManagerChangesPermissionsIT`; thêm `CarePlanApprovalGrantIT` |
| web `tinh-nang/phan-quyen/PhanQuyenVaiTro.jsx`, `PhanQuyenTaiKhoan.jsx`, `QuyenTaiThoiDiem.jsx`, test `PhanQuyen.test.jsx` (T077); lệnh được phép `thu-hep`, `khoi-phuc-quyen`, `gan-quyen-duyet-ke-hoach`, `go-quyen-duyet-ke-hoach` | `frontend/src/features/permissions/RolePermissions.jsx` (`RestrictionList`), `AccountPermissions.jsx`, `PermissionsAsOf.jsx` (`PermissionSummary`), `PermissionCommandForm.jsx`, `permissionLabels.js`, `Permissions.test.jsx`; `allowedCommands` `restrict`, `restore-permission`, `grant-care-plan-approval`, `revoke-care-plan-approval` |
| *(bổ sung 2026-10-21, Phase 9a)* `ThietBiGanApi.xacDinh`, `NguoiCaoTuoiCuaThietBi` (`nguoiCaoTuoiId`, `taiKhoanId`, `maThietBi`) | `administration.account.api.BoundDeviceApi.identify`, `api.dto.ResidentDevice` (`residentId`, `accountId`, `deviceId`) |
| `xacthuc/internal/MaQrService`, `ThietBiGanService`, khóa/mở khóa tài khoản người cao tuổi, loại yêu cầu `M15.KHOA_TAI_KHOAN_NCT` | `internal/auth/service/LoginService.qrLogin`, `ResidentDevices` (`bind`, `unbindActive`, `boundToOther`), `ResidentAccountService` (`createLoginCode`, `unbindDevice`, `lockResident`, `unlockResident`, `lockOnFamilyRequest`), `ResidentAccountLockRequestHandler`; `internal/auth/persistence/DeviceBindings`; `web/ResidentAccountController`; `QrLoginBody`, `QrLoginResponse`, `LockResidentBody`, `UnlockResidentBody` |
| hành động nhật ký `QR_TAO`, `QR_DUNG`, `TB_GAN`, `TB_GO` | `LOGIN_QR_CREATED`, `LOGIN_QR_USED`, `DEVICE_BOUND`, `DEVICE_UNBOUND` |
| phạm vi `BAN_THAN`; nguyên nhân khóa `NCT_KET_THUC_LUU_TRU`, `NCT_TU_YEU_CAU`, `CO_LY_DO`, `THEO_DE_NGHI_NGUOI_THAN`; lý do kết thúc phiên `GO_THIET_BI`, `DOI_MAT_KHAU`; lý do gỡ `GAN_MAY_MOI`, `GO_THU_CONG`, `TAI_KHOAN_DA_KHOA` | `SELF`; `RESIDENT_RESIDENCY_ENDED`, `RESIDENT_REQUESTED`, `WITH_REASON`, `FAMILY_REQUEST`; `DEVICE_UNBOUND`, `PASSWORD_CHANGED`; `NEW_DEVICE_BOUND`, `MANUAL`, `ACCOUNT_LOCKED` |
| *(bổ sung 2026-10-21, Phase 10)* test T021 `PhamViBaoCaoLichSuIT`; bản giả `BT/support/QuyenGia` đổi thành tiện ích người dùng thật (T080); dữ liệu mẫu `V002_90__tai_khoan_mau` (T079a); test FR-023 (T082) | `HistoryReportScopeIT`; `support/TestUsers` (`staff`, `family`, `session`, `bearer`, `TestUser`), test `TestUsersIT`, `FakePermissions` giữ cho test của 000; `db/migration/demo/V002_90__sample_accounts.sql`, test `SampleAccountsIT`; `FloorManagerLimitsIT` |

### 7.6. Bảng và cột (T004 → T006)

| Tên logic | Mã |
| --- | --- |
| `tai_khoan` (`ten_dang_nhap`, `loai`, `nhan_vien_id`, `nguoi_than_id`, `trang_thai`, `nguyen_nhan_khoa`, `khoa_tam_den`, `so_lan_sai_lien_tiep`, `mat_khau_bam`, `buoc_doi_mat_khau`, `lan_dang_nhap_cuoi`, `moc_tinh_khong_hoat_dong`, `phien_ban`, `phien_ban_quyen`, `tao_luc`, `tao_boi`) | `account` (`username`, `type`, `staff_id`, `family_member_id`, `status`, `lock_cause`, `locked_until`, `failed_login_count`, `password_hash`, `must_change_password`, `last_login_at`, `inactivity_reference_at`, `version`, `permission_version`, `created_at`, `created_by`) |
| nguyên nhân khóa `THU_CONG`, `NGHI_VIEC`, `HET_HAN_SAU_KET_THUC`, `HET_QUAN_HE` | `MANUAL`, `RESIGNATION`, `EXPIRED_AFTER_RESIDENCY_END`, `RELATIONSHIP_ENDED` |
| `phien_dang_nhap` (`ma_bam_token`, `ma_thiet_bi`, `kenh`, `bat_dau_luc`, `hoat_dong_cuoi_luc`, `ket_thuc_luc`, `ly_do_ket_thuc`, `thiet_bi_dang_mo`) | `login_session` (`token_hash`, `device_id`, `channel`, `started_at`, `last_active_at`, `ended_at`, `end_reason`, `open_device_id`) |
| lý do kết thúc phiên `DANG_XUAT`, `TAI_KHOAN_ROI_HOAT_DONG`, `THIET_BI_DANG_NHAP_KHAC`, `HET_THOI_GIAN` | `LOGOUT`, `ACCOUNT_LEFT_ACTIVE`, `DEVICE_SIGNED_IN_ELSEWHERE`, `TIMED_OUT` |
| `thiet_bi` (`ma_thiet_bi`, `lan_ket_noi_cuoi`, `tai_khoan_cuoi_id`) | `device` (`device_id`, `last_connected_at`, `last_account_id`) |
| lý do chờ `MUON_HON_DONG_BO`, `SOM_HON_KET_NOI_CUOI`, `QUA_CFG_M15_08`; trạng thái `CHO_XEM_LAI`, `DA_CHAP_NHAN`, `DA_TU_CHOI` | `LATER_THAN_SYNC`, `EARLIER_THAN_LAST_CONNECTION`, `EXCEEDS_CFG_M15_08`; `PENDING_REVIEW`, `ACCEPTED`, `REJECTED` |
| `ma_tran_quyen` (`vai_tro`, `chuc_nang`, `loai_thao_tac`, `pham_vi`, `ma_chu_thich`, `nguon` `PL27`/`19.3`) | `permission_matrix` (`role`, `function_code`, `operation`, `scope`, `footnote_codes`, `source` `APPENDIX_27`/`SECTION_19_3`) |

### 7.11. Bảng sau gộp R-15 *(bổ sung 2026-10-30; thay các dòng bảng đã bỏ ở 7.6; cột chi tiết ở data-model 002 mục 0.1)*

| Tên logic | Mã |
| --- | --- |
| `quyen_tai_khoan` (`loai` gán vai trò, thu hẹp, gán thêm; khóa vai trò hiện hành) | `permission_assignment` (`kind` `ROLE_ASSIGNMENT`, `RESTRICTION`, `ADDITIONAL_GRANT`; `active_role_key`) |
| `thiet_bi_xac_thuc` (`loai` mã QR đăng nhập, thiết bị gắn; mã băm; khóa thiết bị đang dùng) | `device_credential` (`kind` `LOGIN_QR_CODE`, `BOUND_DEVICE`; `secret_hash`; `active_account_id`, `active_device_id`) |
| loại bảng chung của 002 | `M15.ROLE`, `M15.FIELD_VISIBILITY`, `M15.PROFESSIONAL_ACTIVITY` (danh mục); `M15.PENDING_REVIEW_RECORD`, `M15.PERMISSION_DATA_ERROR` (hồ sơ chung); `M15.LICENSE_EXPIRY_WARNING` (mốc đã nhắc); thuộc tính `TRANG_THAI` của `TAI_KHOAN` (lịch sử chung) |


## 8. Feature 001 (`001-elder-profile-assessment`, B)

*Bổ sung 2026-10-21, khi dịch `contracts/api-001.yaml`. Tên bảng, cột, lớp nội bộ còn lại bổ sung vào mục này cùng thay đổi với mã (R-03). Chỉ sửa mục này.*

Thuật ngữ: người cao tuổi = `resident` (mã `nct` trong tên cũ = `resident`); bản đồng ý chia sẻ dữ liệu = `consent`; hồ sơ người cao tuổi = `profile`; mục sức khỏe (dị ứng, bệnh nền, tiền sử) = `health item`; phiếu nguyện vọng cuối đời = `end-of-life wish`.

### 8.1. Đường dẫn API (tiền tố `/api/v1`) và tag

| Tên logic | Mã |
| --- | --- |
| `/nguoi-cao-tuoi`, `/nguoi-cao-tuoi/{id}` | `/residents`, `/residents/{id}` |
| `…/cap-nhat-thong-tin`, `…/ho-so-lien-ket`, `…/lich-su-trang-thai`, `…/dieu-kien/{lenh}` | `…/update-personal-info`, `…/linked-profiles`, `…/status-history`, `…/conditions/{command}` |
| `…/hoan-tat-tiep-nhan`, `…/huy-tiep-nhan`, `…/cho-tam-vang`, `…/ghi-nhan-tro-ve` | `…/complete-admission`, `…/cancel-admission`, `…/grant-temporary-absence`, `…/record-return` |
| `…/chuyen-vien`, `…/ket-thuc-luu-tru`, `…/ghi-nhan-qua-doi` | `…/transfer-out`, `…/end-residency`, `…/record-death` |
| `…/muc-cham-soc`, `…/lich-su-muc-cham-soc` | `…/care-level`, `…/care-level-history` |
| `…/ban-dong-y`, `/ban-dong-y/{id}/rut-lai` | `…/consents`, `/consents/{id}/withdraw` |
| `…/ho-so-suc-khoe-ban-dau`, `…/xac-nhan`, `/ho-so-suc-khoe-ban-dau/{id}/dinh-chinh` | `…/initial-health-record`, `…/confirm`, `/initial-health-records/{id}/correct` |
| `…/muc-suc-khoe`, `/muc-suc-khoe/{id}/loai-tru` | `…/health-items`, `/health-items/{id}/exclude` |
| `…/nguyen-vong` | `…/end-of-life-wishes` |
| `…/danh-gia`, `/danh-gia/{id}`, `…/xac-nhan`, `…/huy-nhap`, `…/dinh-chinh` | `…/assessments`, `/assessments/{id}`, `…/confirm`, `…/discard-draft`, `…/correct` |
| `…/danh-gia-lai`, `/danh-gia-lai/can-thuc-hien` | `…/reassessment`, `/reassessments/due` |
| `/danh-muc/{loai}`, `…/{id}/ngung-hieu-luc`, `…/{id}/kich-hoat-lai` | `/catalogs/{type}`, `…/{id}/deactivate`, `…/{id}/reactivate` |
| loại danh mục `di-nguyen`, `muc-do-di-ung`, `nguon-thong-tin`, `nhu-cau-cham-soc`, `muc-cham-soc` | `allergens`, `allergy-severities`, `information-sources`, `care-needs`, `care-levels` |
| `/tep`, `/tep/{id}`; trường multipart `tep` | `/files`, `/files/{id}`; `file` |
| tham số `sau`, `ngay`, `nhom`, `trangThai` | `cursor`, `date`, `group`, `status` |
| tag `ho-so`, `trang-thai`, `ban-dong-y`, `suc-khoe`, `nguyen-vong`, `danh-gia`, `danh-muc`, `tep` | `profiles`, `status`, `consents`, `health`, `end-of-life-wishes`, `assessments`, `catalogs`, `files` |

### 8.2. Schema và trường JSON

| Tên logic | Mã |
| --- | --- |
| `HoSo`, `HoSoChoNguoiThan`, `ThongTinCaNhan`, `TrangHoSo`, `HoSoLienKet`, `KetQuaTaoHoSo` | `Profile`, `FamilyProfile`, `PersonalInfo`, `ProfilePage`, `LinkedProfile`, `CreateProfileResult` |
| `HoSo`: `maHoSo`, `thongTin`, `trangThai`, `trangThaiTuLuc`, `loaiLuuTruHienHanh`, `mucChamSocHienHanh`, `nhuCauChamSoc`, `luuYChamSoc`, `mucSucKhoeHieuLuc`, `coBanDongYHieuLuc`, `coNguyenVong`, `hoSoTruocId`, `lenhDuocPhep`, `truongBiAn` | `profileCode`, `personalInfo`, `status`, `statusSince`, `currentResidenceType`, `currentCareLevel`, `careNeeds`, `careNote`, `activeHealthItems`, `hasActiveConsent`, `hasEndOfLifeWish`, `previousProfileId`, `allowedCommands`, `hiddenFields` |
| `ThongTinCaNhan`: `hoTen`, `ngaySinh`, `gioiTinh`, `loaiGiayTo`, `soGiayTo`, `anhTepId`, `diaChiThuongTru`, `diaChiLienHe`, `lienHe` (`soDienThoai`), `thongTinDacBiet`, `xacNhanKhongTrung` | `fullName`, `dateOfBirth`, `gender`, `idDocumentType`, `idDocumentNumber`, `photoFileId`, `permanentAddress`, `contactAddress`, `contact` (`phoneNumber`), `specialNotes`, `confirmNotDuplicate` |
| `TrangHoSo` (`muc`, `sau`) | `ProfilePage` (`items`, `nextCursor`) |
| `LichSuTrangThai` (`tu`, `den`, `lenh`, `thoiDiem`, `nguoiThucHien`, `doHeThong`, `lyDo`, `canCu`, `mienDieuKien`), `KetQuaLenhTrangThai` (`lichSu`, `canhBao`) | `StatusHistoryEntry` (`from`, `to`, `command`, `at`, `performedBy`, `bySystem`, `reason`, `basis`, `conditionWaiver`), `StatusCommandResult` (`history`, `warnings`) |
| `DieuKien` (`ma`, `moTa`, `canCu`, `module`, `coTheMien`), `CanhBao` (`ma`, `moTa`) | `Condition` (`code`, `description`, `basis`, `module`, `waivable`), `Warning` (`code`, `description`) |
| `Loi`: `maLoi`, `dieuKienChuaThoa`, `giaTriHienHanh`, `ungVien` | `Problem`: `errorCode`, `unmetConditions`, `currentValue`, `candidates` |
| thân lệnh trạng thái (`requestBodies/LenhTrangThai`: `lyDo`, `chiTiet`), thân chỉ có lý do (`LyDo`), thân đính chính (`DinhChinh`: `kieu`, `noiDungDung`, `lyDo`) | `StatusCommandBody` (`reason`, `details`), `ReasonBody` (`reason`), `ResidentCorrectionBody` (`type`, `correctedContent`, `reason`; tên schema khác `CorrectionBody` của 000 để không trùng trong OpenAPI) |
| `chiTiet` của lệnh trạng thái: `doiPhanBoSangGiuongId`; chuyển viện `canCu` (`suCoId`, `chiDinh`); qua đời `bangChungTepId` | `details`: `switchAllocationToBedId`; `basis` (`incidentId`, `doctorOrder`); `evidenceFileId` |
| `KetQuaDinhChinh` (`cach`, `dinhChinhId`, `yeuCauPheDuyetId`) | `CorrectionResult` (`creationMethod`, `correctionId`, `approvalRequestId`) |
| `DiNguyenRutGon` (`id`, `ten`; dị nguyên trong mục sức khỏe), `KetQuaTaiTep` (`tepId`) | `AllergenRef` (`id`, `name`), `UploadedFile` (`fileId`) |
| `MucChamSocTaiNgay` (`mucChamSoc`, `hieuLucTu`, `canCuLoai`, `canCuId`) | `CareLevelAtDate` (`careLevel`, `effectiveFrom`, `basisType`, `basisId`) |
| `BanDongY` (`nguoiDongYLoai`, `nguoiDongYNguoiThanId`, `quyetDinhToaAn`, `nguoiThanDuocXemSucKhoe`, `dongYHinhAnh`, `dongYThongBao`, `thoiDiemDongY`, `bangChungTepId`, `trangThai`, `rutLai` (`luc`, `lyDo`, `duocThayTheBoiId`)) | `Consent` (`consenterType`, `consenterFamilyMemberId`, `courtDecision`, `familyMemberIdsAllowedHealthView`, `photoConsent`, `notificationConsent`, `consentedAt`, `evidenceFileId`, `status`, `withdrawal` (`at`, `reason`, `supersededById`)) |
| `QuyetDinhToaAn` (`so`, `ngay`, `toaAn`, `tepId`) | `CourtDecision` (`number`, `date`, `court`, `fileId`) |
| `NoiDungHoSoSucKhoe`, `HoSoSucKhoeBanDau` (`nhomMau`, `khaNangVanDong`, `khaNangTuChamSoc`, `khaNangNhanThuc`, `ghiChuNhuCauBanDau`, `nhuCauDinhDuong`, `thuocDangDung`, `nhuCauPhucHoi`, `daXacNhan`, `nguoiXacNhan`, `daDinhChinh`) | `HealthRecordContent`, `InitialHealthRecord` (`bloodType`, `mobility`, `selfCareAbility`, `cognition`, `initialNeedsNote`, `nutritionNeeds`, `currentMedications`, `rehabilitationNeeds`, `confirmed`, `confirmedBy`, `corrected`) |
| `MucSucKhoe` (`loai`, `diNguyen`, `noiDungVanBan`, `khongKiemTraTuDong`, `choKiemTra`, `mucDo`, `nguon`, `nguoiGhi`, `thoiDiem`, `trangThai`, `loaiTru` (`luc`, `boi`, `lyDo`)); thân tạo: `diNguyenId`, `mucDoId`, `nguonId`, `xacNhanTrung` | `HealthItem` (`type`, `allergen`, `freeText`, `notAutoChecked`, `pendingCheck`, `severity`, `source`, `recordedBy`, `at`, `status`, `exclusion` (`at`, `by`, `reason`)); `allergenId`, `severityId`, `sourceId`, `confirmDuplicate` |
| `KetQuaMucSucKhoe` (`muc`, `canhBao`) | `HealthItemResult` (`item`, `warnings`) |
| `NguyenVong` (`luaChon`, `nguoiLienHeTruoc`, `ghiChu`, `nhanDinh`, `nguoiKy`, `nguoiKyLoai`, `cachChonDaiDien`, `yKienKhac` (`nguon`, `noiDung`, `nguoiGhi`, `thoiDiem`), `thoiDiemKy`, `nguon`, `trangThai`); thân tạo: `nguoiTraLoiLoai`, `nguoiTraLoiNguoiThanId`, `quanHe`, `bacSiNhanDinhId`, `nguoiKyNguoiThanId`, `banKyTepId` | `EndOfLifeWish` (`choice`, `firstContact`, `note`, `selfDeterminationAssessment`, `signer`, `signerType`, `representativeSelection`, `otherOpinions` (`source`, `content`, `recordedBy`, `at`), `signedAt`, `source`, `status`); `respondentType`, `respondentFamilyMemberId`, `relationship`, `assessingDoctorId`, `signerFamilyMemberId`, `signedCopyFileId` |
| `NguyenVongTongHop` (`coNguyenVong`, `hieuLuc`, `lichSu`, `truongBiAn`) | `EndOfLifeWishSummary` (`hasEndOfLifeWish`, `current`, `history`, `hiddenFields`) |
| `NhanDinhTuQuyet` (`tuQuyetDuoc`, `thoiDiem`, `cachHoTroHieu`, `ngayXemLai`) | `SelfDeterminationAssessment` (`canSelfDetermine`, `at`, `supportMethod`, `reviewDate`) |
| `NguonYKien` (`loai`, `nguoiThanId`) | `OpinionSource` (`type`, `familyMemberId`) |
| `DanhGia` (`loai`, `canCu`, `nguoiLap`, `nhanXet`, `luuYChamSoc`, `mucChamSoc`, `nhuCauChamSoc`, `loaiLuuTruDeXuat`, `trangThai`, `nguoiXacNhan`, `thoiDiemXacNhan`, `daDinhChinh`); thân sửa nháp: `nhuCauChamSocIds` | `Assessment` (`type`, `basis`, `createdBy`, `comment`, `careNote`, `careLevel`, `careNeeds`, `proposedResidenceType`, `status`, `confirmedBy`, `confirmedAt`, `corrected`); `careNeedIds` |
| `DanhGiaChiTiet` (`thamKhao`: `hoSoSucKhoeBanDau`, `mucSucKhoeHieuLuc`, `chiSoGanNhat`) | `AssessmentDetail` (`reference`: `initialHealthRecord`, `activeHealthItems`, `latestVitalSigns`) |
| `KetQuaXacNhanDanhGia` (`danhGia`, `tacDong`: `mucHienHanhDaDoi`, `yeuCauDuocThayThe`, `daBaoHanhChinhLapYeuCau`, `daBaoXemXetKeHoach`) | `ConfirmAssessmentResult` (`assessment`, `effects`: `currentCareLevelChanged`, `supersededRequestIds`, `administratorNotified`, `carePlanReviewNotified`) |
| `TrangThaiDanhGiaLai` (`nguoiCaoTuoiId`, `trangThai`, `hanLuc`, `quaHanTu`, `canCu` (`canCu`, `nguon`, `thoiDiemBao`)) | `ReassessmentStatus` (`residentId`, `status`, `dueAt`, `overdueSince`, `basis` (`basis`, `source`, `notifiedAt`)) |
| `MucDanhMuc` (`ten`, `nhom`, `thuTu`, `trangThai`) | `CatalogItem` (`name`, `group`, `order`, `status`) |
| `Id`, `IfMatch`, `IdempotencyKey`, `Sau`; security scheme `phien`; response `Loi`, `LenhOk`, `LenhLoi409`, `LenhLoi422`; `LenhNguoiDung`, `MaMucChamSoc` | `Id`, `IfMatch`, `IdempotencyKey`, `Cursor`; `session`; `Problem`, `CommandOk`, `CommandConflict`, `CommandInvalid`; `UserCommand`, `CareLevelCode` |

### 8.3. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| trạng thái `TrangThaiNct`: `DANG_TIEP_NHAN`, `DANG_LUU_TRU`, `TAM_VANG`, `HOAT_DONG_BEN_NGOAI`, `DIEU_TRI_BENH_VIEN`, `KET_THUC_LUU_TRU`, `QUA_DOI`, `HUY_TIEP_NHAN` | `ResidentStatus`: `ADMISSION_IN_PROGRESS`, `IN_RESIDENCE`, `TEMPORARILY_ABSENT`, `OFF_SITE_ACTIVITY`, `HOSPITALIZED`, `RESIDENCY_ENDED`, `DECEASED`, `ADMISSION_CANCELLED` |
| lệnh `LenhTrangThai` (người dùng): `HOAN_TAT_TIEP_NHAN`, `HUY_TIEP_NHAN`, `CHO_TAM_VANG`, `GHI_NHAN_TRO_VE`, `CHUYEN_VIEN`, `KET_THUC_LUU_TRU`, `GHI_NHAN_QUA_DOI` | `StatusCommand` (`UserCommand` trong hợp đồng): `COMPLETE_ADMISSION`, `CANCEL_ADMISSION`, `GRANT_TEMPORARY_ABSENCE`, `RECORD_RETURN`, `TRANSFER_OUT`, `END_RESIDENCY`, `RECORD_DEATH` |
| lệnh do module khác gọi / hệ thống: `TAO_HO_SO`, `DIEM_DANH_ROI_VIEN`, `DIEM_DANH_VE`, `HUY_GHI_NHAN_ROI_VIEN` | `CREATE_PROFILE`, `ROLL_CALL_DEPARTURE`, `ROLL_CALL_RETURN`, `CANCEL_DEPARTURE_RECORD` |
| giới tính `NAM`, `NU`; loại giấy tờ `CCCD`, `KHAC` | `MALE`, `FEMALE`; `CITIZEN_ID`, `OTHER` |
| mức chăm sóc `CO_BAN`, `THUONG_XUYEN`, `DAC_BIET` | `BASIC`, `REGULAR`, `SPECIAL` |
| loại lưu trú đề xuất `NOI_TRU_DAI_HAN`, `NOI_TRU_NGAN_NGAY`, `BAN_TRU` | `LONG_TERM_RESIDENTIAL`, `SHORT_TERM_RESIDENTIAL`, `DAY_CARE` |
| căn cứ mức chăm sóc `DANH_GIA`, `YEU_CAU_THAY_DOI_LUU_TRU` | `ASSESSMENT`, `RESIDENCY_CHANGE_REQUEST` |
| trạng thái bản đồng ý `HIEU_LUC`, `DA_RUT_LAI`; người đồng ý `NGUOI_CAO_TUOI`, `DAI_DIEN_PHAP_LUAT` | `ACTIVE`, `WITHDRAWN`; `RESIDENT`, `LEGAL_REPRESENTATIVE` |
| loại mục sức khỏe `DI_UNG`, `BENH_NEN`, `TIEN_SU`; trạng thái `HIEU_LUC`, `DA_LOAI_TRU`, lọc `TAT_CA` | `ALLERGY`, `CHRONIC_CONDITION`, `MEDICAL_HISTORY`; `ACTIVE`, `EXCLUDED`, `ALL` |
| nhóm dị nguyên `THUOC_HOAT_CHAT`, `THUC_PHAM`, `KHAC` | `ACTIVE_INGREDIENT`, `FOOD`, `OTHER` |
| trạng thái danh mục `HIEU_LUC`, `NGUNG_HIEU_LUC` | `ACTIVE`, `INACTIVE` (như `CatalogStatus`) |
| lựa chọn cuối đời `CHUYEN_BENH_VIEN`, `DUA_VE_NHA`, `O_LAI_GIAM_NHE` | `TRANSFER_TO_HOSPITAL`, `RETURN_HOME`, `STAY_PALLIATIVE` |
| người trả lời, người ký `NGUOI_CAO_TUOI`, `DAI_DIEN_NGUOI_BENH`, `DAI_DIEN_PHAP_LUAT` | `RESIDENT`, `PATIENT_REPRESENTATIVE`, `LEGAL_REPRESENTATIVE` |
| cách chọn đại diện `NCT_DA_CHON`, `GIA_DINH_CHON`; nguồn phiếu `TIEP_NHAN`, `YEU_CAU_GIA_DINH`; trạng thái phiếu `HIEU_LUC`, `DUOC_THAY_THE` | `CHOSEN_BY_RESIDENT`, `CHOSEN_BY_FAMILY`; `ADMISSION`, `FAMILY_REQUEST`; `ACTIVE`, `SUPERSEDED` |
| nguồn ý kiến `LoaiNguonYKien`: `NGUOI_CAO_TUOI`, `NGUOI_THAN`, `NHAN_VIEN_QUAN_SAT` | `OpinionSourceType`: `RESIDENT`, `FAMILY_MEMBER`, `OBSERVING_STAFF` |
| loại đánh giá `DAU_VAO`, `DANH_GIA_LAI`; trạng thái `NHAP`, `DA_XAC_NHAN`, `DA_HUY` | `INITIAL`, `REASSESSMENT`; `DRAFT`, `CONFIRMED`, `DISCARDED` |
| trạng thái đánh giá lại `KHONG`, `CAN`, `QUA_HAN`, `KHONG_AP_DUNG` | `NONE`, `DUE`, `OVERDUE`, `NOT_APPLICABLE` |
| kiểu đính chính `SUA_NOI_DUNG`, `HUY_GHI_NHAN`; cách `A_NGUOI_GHI_GOC`, `B_PHU_TRACH_TANG`, `C_YEU_CAU_PHE_DUYET` | `EDIT_CONTENT`, `CANCEL_RECORD`; `A_ORIGINAL_AUTHOR`, `B_FLOOR_LEAD`, `C_APPROVAL_REQUEST` |
| cảnh báo `THIEU_BAN_DONG_Y`, `THIEU_NGUYEN_VONG`, `CCCD_TRUNG_HO_SO_QUA_DOI`, `DI_UNG_KHONG_KIEM_TRA_TU_DONG` | `MISSING_CONSENT`, `MISSING_END_OF_LIFE_WISH`, `CITIZEN_ID_MATCHES_DECEASED_PROFILE`, `ALLERGY_NOT_AUTO_CHECKED` |
| nhóm trường (`@FieldGroup`) `SUC_KHOE` | `HEALTH` |

### 8.4. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `CCCD_DANG_DUNG`, `CO_THE_TRUNG_HO_SO` | `CITIZEN_ID_IN_USE`, `POSSIBLE_DUPLICATE_PROFILE` |
| `DIEU_KIEN_CHUA_THOA`, `CHUYEN_KHONG_HOP_LE`, `HO_SO_DA_DONG` | `UNMET_CONDITIONS`, `INVALID_TRANSITION`, `PROFILE_CLOSED` |
| `THIEU_LY_DO`, `CHI_TIET_KHONG_HOP_LE` | `REASON_REQUIRED`, `INVALID_DETAILS` |
| `NGUOI_KY_KHONG_HOP_LE`, `NGUON_Y_KIEN_KHONG_HOP_LE`, `THIEU_QUYET_DINH_TOA_AN` | `INVALID_SIGNER`, `INVALID_OPINION_SOURCE`, `COURT_DECISION_REQUIRED` |
| `MUC_SUC_KHOE_TRUNG`, `DA_XAC_NHAN` (hồ sơ sức khỏe) | `DUPLICATE_HEALTH_ITEM`, `ALREADY_CONFIRMED` |
| `DA_CO_BAN_NHAP`, `DA_CO_DANH_GIA_DAU_VAO`, `TRANG_THAI_KHONG_CHO_DANH_GIA` | `DRAFT_ALREADY_EXISTS`, `INITIAL_ASSESSMENT_EXISTS`, `STATUS_NOT_ASSESSABLE` |
| `TEP_SAI_KIEU`, `TEP_QUA_LON` | `FILE_TYPE_NOT_ALLOWED`, `FILE_TOO_LARGE` |

### 8.5. Gói, giao diện công khai `resident.api`, sự kiện (T001, T007 → T009)

| Tên logic | Mã |
| --- | --- |
| gói chức năng `hoso`, `trangthai`, `dongy`, `suckhoe`, `nguyenvong`, `danhgia`, `lich` | `internal.profile`, `internal.status`, `internal.consent`, `internal.health`, `internal.wish`, `internal.assessment`; `scheduling` |
| `resident.api` / `api/sukien`; `@NamedInterface` | `resident.api` (`dto`, `spi`, `events`); `resident`, `resident-spi`, `resident-events` |
| `NguoiCaoTuoiApi` (`trangThai`, `nhanDang`, `thoiDiemTrangThaiCuoi`, `laTrangThaiCuoi`, `gioiTinh`, `theoTrangThai`); `NhanDangView` | `ResidentApi` (`status`, `identity`, `finalStatusAt`, `isFinalStatus`, `gender`, `byStatus`); `IdentityView` |
| `TrangThaiNguoiCaoTuoiApi` (`thucHien`, `xemDieuKien`, `khoangTrangThai`); `KhoangTrangThai` | `ResidentStatusApi` (`execute`, `previewConditions`, `statusPeriods`); `StatusPeriod` |
| `MucChamSocApi` (`hienHanh`, `taiNgay`, `apDungThayDoi`) | `CareLevelApi` (`current`, `atDate`, `applyChange`) |
| `DanhGiaApi` (`luuYHienHanh`, `xacNhanGanNhat`), `DanhGiaLaiApi` | `AssessmentApi` (`currentCareNote`, `latestConfirmed`), `ReassessmentApi` |
| `MucSucKhoeApi` (`hieuLuc`, `daKiemTra`), `HoSoSucKhoeApi` (`banDau`), `DiNguyenApi` (`diNguyenHieuLuc`) | `HealthItemApi` (`active`, `markChecked`), `HealthRecordApi` (`initial`), `AllergenApi` (`activeAllergens`) |
| `BanDongYApi` (`baoGom`), `NguyenVongApi` (`hieuLuc`, `ghiPhienBan`), `NguonYKienApi` (`kiemTra`), `NguoiCaoTuoiThongKeApi` | `ConsentApi` (`includes`), `EndOfLifeWishApi` (`current`, `recordVersion`), `OpinionSourceApi` (`validate`), `ResidentStatisticsApi` |
| SPI `ThamGiaChuyenTrangThai` (`lenhQuanTam`, `kiemTraDieuKien`, `mienDieuKien`, `kiemTraChiTiet`, `thucHienTacDong`, `canhBao`) | `StatusTransitionParticipant` (`interestedCommands`, `checkConditions`, `waiveConditions`, `validateDetails`, `applyEffects`, `warnings`) |
| `NguCanhLenh`, `DieuKien`, `MienDieuKien`, `KetQuaChuyen`, `CanhBao`, `CanCu`, `LyDo` | `CommandContext`, `Condition`, `ConditionWaiver`, `TransitionResult`, `Warning`, `Basis`, `Reason` |
| SPI M02 cài: `YeuCauDoiMucApi` (`thayTheBoiDanhGia`, `coYeuCauDoiMucChuaKetThuc`), `HopDongApi` (`loaiLuuTruHienHanh`, `lichSuLoaiLuuTru`), `LuotVangApi` (`dangMo`) | `CareLevelChangeRequestApi` (`supersedeByAssessment`, `hasOpenRequest`), `ContractApi` (`currentResidenceType`, `residenceTypeHistory`), `AbsenceApi` (`open`) |
| `ChiSoApi` (`ganNhat`) (M06 cài) | `VitalSignApi` (`latest`) |
| `NguoiCaoTuoiDaTao(nctId, hoSoTruocId)` | `ResidentCreated(residentId, previousProfileId)` |
| `NguoiCaoTuoiDaChuyenTrangThai(nctId, tu, den, lenh, lichSuId, thoiDiem)` | `ResidentStatusChanged(residentId, from, to, command, historyId, at)` |
| `BanDongYDaDoi(nctId, banHieuLucId)` | `ConsentChanged(residentId, activeConsentId)` |
| `DiUngDaGhiNhan(mucId, nctId, diNguyenId)`, `DiUngKhacDaGhiNhan`, `DiUngDaLoaiTru` | `AllergyRecorded(itemId, residentId, allergenId)`, `OtherAllergyRecorded`, `AllergyExcluded` |
| `HoSoSucKhoeBanDauDaDoi`, `DanhGiaDauVaoDaXacNhan`, `DanhGiaDaDoiMucHoacNhuCau` | `InitialHealthRecordChanged`, `InitialAssessmentConfirmed`, `AssessmentChangedCareLevelOrNeeds` |
| `CanDanhGiaLai`, `QuaHanDanhGiaLai`, `CanLapYeuCauDoiMuc`, `NhacLapYeuCauDoiMuc` | `ReassessmentDue`, `ReassessmentOverdue`, `CareLevelChangeRequestNeeded`, `CareLevelChangeRequestReminder` |
| `NhacThieuBanDongY`, `NhacThieuNguyenVong`, `NhacXemLaiNhanDinh`, `TamVangQuaHan` | `MissingConsentReminder`, `MissingEndOfLifeWishReminder`, `SelfDeterminationReviewReminder`, `TemporaryAbsenceOverdue` |
| `MucChamSocDaDoi`, `ThongTinCaNhanDaCapNhat`; sự kiện 007 `SuCoDaGhiNhan` | `CareLevelChanged`, `PersonalInfoUpdated`; `IncidentRecorded` |
| SPI M01 cài: `DieuKienChuThich` mã "1"; `GanPhamViProvider`; `QuyenTepProvider`; `LuuTruProvider`; `PhanLoaiTruongProvider`; `DinhChinhDuocProvider` | `FootnoteCondition` mã "1"; `ScopeAssignmentProvider`; `FilePermissionProvider`; `RetentionProvider`; `FieldClassificationProvider`; `CorrectableRecordProvider` |
| `NguonKiemTra` (`THUOC`, `AN`); `NguonNguyenVong` (thêm `XAC_NHAN_LAI_NGUY_KICH`); `GhiNguyenVong`, `DiNguyenView`, `TaoDiNguyen`, `MucSucKhoeView`, `HoSoSucKhoeBanDauView` | `HealthItemCheckSource` (`MEDICATION`, `NUTRITION`); `WishRecordSource` (`ADMISSION`, `FAMILY_REQUEST`, `CRITICAL_RECONFIRMATION`); `RecordEndOfLifeWish`, `AllergenView`, `CreateAllergen`, `HealthItemView`, `InitialHealthRecordView` |
| bản giả `support/gia001/` | `support/fake/resident/` (`FakeM01Config`) |

### 8.6. Bảng và migration (T003, T005, T006)

| Tên logic | Mã |
| --- | --- |
| `V001_01__tep.sql`, `V001_02__nguoi_cao_tuoi.sql`, `V001_03__danh_muc_m01.sql` | `V001_01__file.sql`, `V001_02__resident.sql`, `V001_03__m01_catalogs.sql` |
| `tep` (`uuid`, `ten_goc`, `kieu_mime`, `kich_thuoc`, `tai_len_boi`, `tai_len_luc`, `chu_so_huu_loai`, `chu_so_huu_id`, `gan_luc`) | `file` (`uuid`, `original_name`, `mime_type`, `size_bytes`, `uploaded_by`, `uploaded_at`, `owner_type`, `owner_id`, `attached_at`) |
| `nguoi_cao_tuoi` (`ma_ho_so`, `ho_so_truoc_id`, `ho_ten`, `ho_ten_so_khop`, `ngay_sinh`, `gioi_tinh`, `loai_giay_to`, `so_giay_to`, `cccd_dang_mo`, `trang_thai`, `trang_thai_tu_luc`, `muc_cham_soc_hien_hanh`, `phien_ban`) | `resident` (`profile_code`, `previous_profile_id`, `full_name`, `full_name_match`, `date_of_birth`, `gender`, `id_document_type`, `id_document_number`, `open_citizen_id`, `status`, `status_since`, `current_care_level`, `version`) |
| `nguoi_cao_tuoi` còn lại: `anh_tep_id`, `dia_chi_thuong_tru`, `dia_chi_lien_he`, `lien_he`, `thong_tin_dac_biet`, `tao_luc`, `tao_boi` | `photo_file_id`, `permanent_address`, `contact_address`, `contact`, `special_notes`, `created_at`, `created_by` |
| thực thể danh mục, nhật ký `DM_TAO`, `DM_SUA`, `DM_NGUNG`, `DM_KICH_HOAT_LAI` | `Allergen`, `AllergySeverity`, `InformationSource`, `CareNeed` (loại đối tượng `M01.ALLERGEN`, `M01.ALLERGY_SEVERITY`, `M01.INFORMATION_SOURCE`, `M01.CARE_NEED`); `CATALOG_CREATED`, `CATALOG_UPDATED`, `CATALOG_DEACTIVATED`, `CATALOG_REACTIVATED` |
| lỗi mức chăm sóc cố định không sửa được; trùng tên mục Hiệu lực | `READ_ONLY_CATALOG` (405); `DUPLICATE_NAME` (409, như 002) |
| `V001_05__danh_gia.sql`; `danh_gia`, `danh_gia_nhu_cau` (`loai`, `can_cu`, `nguoi_thuc_hien_id`, `thoi_diem`, `nhan_xet`, `luu_y_cham_soc`, `muc_cham_soc`, `loai_luu_tru_de_xuat`, `nguoi_xac_nhan_id`, `thoi_diem_xac_nhan`, `huy_boi`, `huy_luc`, `ly_do_huy`, `nhap_dang_mo`, `dau_vao_da_xac_nhan`, `nhu_cau_id`) | `V001_05__assessment.sql`; `assessment`, `assessment_care_need` (`type`, `basis`, `performed_by`, `performed_at`, `comment`, `care_note`, `care_level`, `proposed_residence_type`, `confirmed_by`, `confirmed_at`, `discarded_by`, `discarded_at`, `discard_reason`, `open_draft`, `confirmed_initial`, `care_need_id`) |
| `DanhGiaService`, `DanhGiaApi` impl, `MucChamSocApi` impl, `DinhChinhDuocProvider` (`M01.DANH_GIA`), `DanhGiaController`; nhật ký `DG_TAO`, `DG_SUA_NHAP`, `DG_XAC_NHAN`, `DG_HUY_NHAP` | `AssessmentService`, `AssessmentApiImpl`, `CareLevelApiImpl`, `AssessmentCorrectionProvider` (`M01.ASSESSMENT`), `AssessmentController`, `CareLevelQueryService`; `ASSESSMENT_CREATED`, `ASSESSMENT_DRAFT_EDITED`, `ASSESSMENT_CONFIRMED`, `ASSESSMENT_DISCARDED` |
| sự kiện nội bộ đính chính đổi mức (chờ T062 ghi lần báo `DINH_CHINH_DANH_GIA`); lỗi sửa hay hủy lần đánh giá không còn Nháp; xác nhận thiếu nhận xét hoặc mức (hợp đồng ghi `UNMET_CONDITIONS`, mã thực của 000) | `AssessmentCorrectedLevel`; `NOT_A_DRAFT` (409); `CONDITION_NOT_MET` (409, trường thiếu `comment`, `careLevel`) |
| `HoSoMoGuard`, `HoSoMoGuardArchTest`; `NguonYKienService` | `ProfileOpenGuard`, `ProfileOpenGuardArchTest`; `OpinionSourceService` |
| `V001_07__ban_dong_y.sql`; `ban_dong_y` (`nguoi_cao_tuoi_id`, `nguoi_dong_y_loai`, `nguoi_dong_y_nguoi_than_id`, `qd_toa_so`, `qd_toa_ngay`, `qd_toa_toa_an`, `qd_toa_tep_id`, `dong_y_hinh_anh`, `dong_y_thong_bao`, `thoi_diem_dong_y`, `bang_chung_tep_id`, `trang_thai`, `dang_hieu_luc`, `rut_lai_luc`, `rut_lai_boi`, `rut_lai_ly_do`, `rut_lai_bang_chung_tep_id`, `thay_the_boi_id`); `ban_dong_y_nguoi_than`; `nhac_chu_ky` (`loai` `THIEU_BAN_DONG_Y`, `THIEU_NGUYEN_VONG`, `LAP_YEU_CAU_DOI_MUC`, `XEM_LAI_NHAN_DINH`; `moc_id`, `bat_dau_luc`, `lan_nhac_cuoi_luc`, `so_lan_nhac`) | `V001_07__consent.sql`; `consent` (`resident_id`, `consenter_type`, `consenter_family_member_id`, `court_decision_number`, `court_decision_date`, `court_decision_court`, `court_decision_file_id`, `photo_consent`, `notification_consent`, `consented_at`, `evidence_file_id`, `status` `ACTIVE`, `WITHDRAWN`, `active_resident_id`, `withdrawn_at`, `withdrawn_by`, `withdrawal_reason`, `withdrawal_evidence_file_id`, `superseded_by_id`, `created_at`, `created_by`); `consent_family_member` (`consent_id`, `family_member_id`); `periodic_reminder` (`type` `MISSING_CONSENT`, `MISSING_END_OF_LIFE_WISH`, `CREATE_CARE_LEVEL_CHANGE_REQUEST`, `REVIEW_SELF_DETERMINATION`; `resident_id`, `milestone_id`, `started_at`, `last_reminded_at`, `reminder_count`). `V001_01__file.sql` thực tế là `V001_06_1__file.sql` (V001_02 → V001_06 đã áp dụng) |
| `BanDongYService`, `DieuKienChuThich` mã "1", `QuyenTepProvider` `M01.BAN_DONG_Y`, `NhacThieuDongYNguyenVong`, `BanDongYController`; nhật ký `BDY_GHI_NHAN`, `BDY_THAY_THE`, `BDY_RUT_LAI`; loại đối tượng `M01.BAN_DONG_Y` | `ConsentService` (`record`, `withdraw`, `list`, `targetOf`), `ConsentFootnoteCondition` (`footnoteCode` `"1"`), `ConsentFilePermission` (`M01.CONSENT`), `MissingConsentReminderService` (`startReminding`, `run`) và `MissingConsentReminderJob` (`MISSING_CONSENT_REMINDER`), `ConsentController`, `ConsentStore`; `CONSENT_RECORDED`, `CONSENT_SUPERSEDED`, `CONSENT_WITHDRAWN`; `M01.CONSENT` |
| (mã lỗi tiếng Việt chưa có trong tài liệu; hợp đồng chỉ ghi `422`, `409`); lệnh trong `allowedCommands` | `EVIDENCE_REQUIRED` (422); `COURT_DECISION_REQUIRED` (422), `COURT_DECISION_NOT_ALLOWED` (422); `CONSENT_NOT_ACTIVE` (409); `RECORD_CONSENT`, `WITHDRAW_CONSENT` |
| `TepApi`, `TepService`, `TepController`, `DonTepChuaGan` (mã lỗi `TEP_SAI_KIEU`, `TEP_QUA_LON` ở dòng trên) | `FileApi`, `FileService` (`upload`, `attach`, `download`), `FileController`, `UnattachedFileCleanup` và `UnattachedFileCleanupJob` (`CLEAN_UNATTACHED_FILES`); `FILE_ALREADY_ATTACHED` (409), `FILE_STORAGE_UNAVAILABLE` (503), `FILE_STORAGE_ERROR` (503) |
| `BanDongYDaDoi(nctId, banHieuLucId hoặc rỗng)` | `ConsentChanged(residentId, activeConsentId)` với `activeConsentId` rỗng (`null`) khi không còn bản Hiệu lực |
| `V001_08__lan_bao_danh_gia_lai.sql`; `lan_bao_danh_gia_lai` (`nguoi_cao_tuoi_id`, `can_cu` `DINH_KY`, `SAU_SU_CO`, `TRO_VE_BENH_VIEN`, `DINH_CHINH_DANH_GIA`; `nguon_loai`, `nguon_id`, `thoi_diem_bao`, `han_tinh_tu`, `da_canh_bao_qua_han_luc`) | `V001_08__reassessment_notice.sql`; `reassessment_notice` (`resident_id`, `basis` `PERIODIC`, `AFTER_INCIDENT`, `RETURN_FROM_HOSPITAL`, `ASSESSMENT_CORRECTION`; `source_type` `ASSESSMENT`, `INCIDENT`, `STATUS_HISTORY`, `CORRECTION`; `source_id`, `notified_at`, `deadline_from`, `overdue_alerted_at`); `uq_reassessment_notice_source` |
| `BaoDanhGiaLaiService`, `DanhGiaLaiApi` impl, `BaoDanhGiaLaiListener`, `NhacLapYeuCauDoiMucService`, `DanhGiaLaiController`; job `BaoDanhGiaLaiDinhKy`, `CanhBaoQuaHanDanhGiaLai`, `NhacLapYeuCauDoiMuc` (`BAO_DANH_GIA_LAI_DINH_KY`, `CANH_BAO_QUA_HAN_DANH_GIA_LAI`, `NHAC_LAP_YEU_CAU_DOI_MUC`) | `ReassessmentNoticeService` (`notifyAfterIncident`, `notifyAfterCorrection`, `onReturnFromHospital`, `runPeriodic`, `runOverdue`, `detail`), `ReassessmentApiImpl`, `ReassessmentNoticeListener`, `CareLevelChangeRequestReminderService`, `ReassessmentQueryService`, `ReassessmentController`; `NotifyPeriodicReassessmentJob` (`NOTIFY_PERIODIC_REASSESSMENT`), `AlertOverdueReassessmentJob` (`ALERT_OVERDUE_REASSESSMENT`), `RemindCareLevelChangeRequestJob` (`REMIND_CARE_LEVEL_CHANGE_REQUEST`); cấu hình `eldercare.jobs.notify-periodic-reassessment`, `alert-overdue-reassessment`, `remind-care-level-change-request` |
| `source` của căn cứ trong `TrangThaiDanhGiaLai.canCu` có dạng `LOẠI:mã` | `ReassessmentStatus.basis[].source` là `ASSESSMENT:12`, `INCIDENT:77`, `STATUS_HISTORY:3`, `CORRECTION:9` |
| `V001_09__nguyen_vong.sql`; `nguyen_vong_cuoi_doi` (`nguoi_cao_tuoi_id`, `lua_chon`, `nguoi_lien_he_truoc`, `ghi_chu`, `nguoi_tra_loi_loai`, `nguoi_tra_loi_nguoi_than_id`, `quan_he`, `bac_si_nhan_dinh_id`, `tu_quyet_duoc`, `thoi_diem_nhan_dinh`, `cach_ho_tro_hieu`, `ngay_xem_lai_nhan_dinh`, `nguoi_ky_loai`, `nguoi_ky_nguoi_than_id`, `cach_chon_dai_dien`, `qd_toa_*`, `thoi_diem_ky`, `ban_ky_tep_id`, `nguon`, `trang_thai` `HIEU_LUC`, `DUOC_THAY_THE`, `thay_the_boi_id`, `dang_hieu_luc`, `nguoi_thuc_hien_id`, `thoi_diem`); `nguyen_vong_y_kien` (`nguyen_vong_id`, `nguon_loai`, `nguon_nguoi_than_id`, `noi_dung`, `nguoi_ghi_id`, `thoi_diem`) | `V001_09__end_of_life_wish.sql`; `end_of_life_wish` (`resident_id`, `choice`, `first_contact`, `note`, `respondent_type`, `respondent_family_member_id`, `relationship`, `assessing_doctor_id`, `can_self_determine`, `assessed_at`, `support_method`, `review_date`, `signer_type`, `signer_family_member_id`, `representative_selection`, `court_decision_number`, `court_decision_date`, `court_decision_court`, `court_decision_file_id`, `signed_at`, `signed_copy_file_id`, `source`, `status` `ACTIVE`, `SUPERSEDED`, `superseded_by_id`, `active_resident_id`, `recorded_by`, `recorded_at`); `end_of_life_wish_opinion` (`wish_id`, `source_type`, `source_family_member_id`, `content`, `recorded_by`, `recorded_at`) |
| `V001_10__nguyen_vong_dieu_tri.sql`; `nguyen_vong_dieu_tri` (`bv1_*`, `bv2_*`, `bhyt_*`, `hoi_gia_dinh_truoc`, `nguoi_di_kem`, `ghi_chu`, `nguon_loai`, `nguon_nguoi_than_id`, `ban_ky_tep_id`, `trang_thai` `HIEU_LUC`, `HET_HIEU_LUC`, `thay_the_boi_id`, `dang_hieu_luc`); `nguyen_vong_dieu_tri_nguoi_bao`; `nguyen_vong_dieu_tri_y_kien` | `V001_10__treatment_wish.sql`; `treatment_wish` (`resident_id`, `hospital1_name`, `hospital1_address`, `hospital2_name`, `hospital2_address`, `insurance_registration_place`, `insurance_card_number`, `insurance_card_expiry`, `ask_family_first`, `escort` `FAMILY`, `FACILITY_STAFF`, `CASE_BY_CASE`, `note`, `respondent_type`, `respondent_family_member_id`, `signed_copy_file_id`, `status` `ACTIVE`, `EXPIRED`, `superseded_by_id`, `active_resident_id`, `recorded_by`, `recorded_at`); `treatment_wish_notify` (`treatment_wish_id`, `family_member_id`); `treatment_wish_opinion` |
| `NguyenVongService`, `NguyenVongDieuTriService`, `NguyenVongApi` impl, `NhacNguyenVongService` (`batDauNhac`, `nhacThieu`, `nhacXemLai`), `NguyenVongController`, `NguyenVongDieuTriController`; nhật ký `NV_GHI`, `NVDT_GHI`; loại đối tượng `M01.NGUYEN_VONG`, `M01.NGUYEN_VONG_DIEU_TRI`; `NguyenVongApi.dieuTriHieuLuc` | `EndOfLifeWishService` (`get`, `record`, `recordVersion`, `currentView`), `TreatmentWishService` (`get`, `record`, `currentView`), `EndOfLifeWishApiImpl` (`EndOfLifeWishApi.currentTreatmentWish`), `WishReminderService` (`startReminding`, `runMissing`, `runReviews`), `EndOfLifeWishController`, `TreatmentWishController`; nhật ký `END_OF_LIFE_WISH_RECORDED`, `END_OF_LIFE_WISH_SUPERSEDED`, `TREATMENT_WISH_RECORDED`, `TREATMENT_WISH_EXPIRED`; loại đối tượng `M01.END_OF_LIFE_WISH`, `M01.TREATMENT_WISH`; lệnh trong `allowedCommands`: `RECORD_END_OF_LIFE_WISH`, `RECORD_TREATMENT_WISH` |
| mã lỗi `NGUOI_KY_KHONG_HOP_LE` (409), `NGUOI_THAN_KHONG_HOP_LE` (409), `NGUON_Y_KIEN_KHONG_HOP_LE` (422); cảnh báo `THIEU_NGUYEN_VONG`, `THIEU_NGUYEN_VONG_DIEU_TRI`, `CHUA_CO_NGUOI_CAN_BAO` | `INVALID_SIGNER` (409), `INVALID_FAMILY_MEMBER` (409), `INVALID_OPINION_SOURCE` (422); `SELF_DETERMINATION_ASSESSMENT_REQUIRED`, `INVALID_REVIEW_DATE`, `INVALID_ASSESSING_DOCTOR`, `RESPONDENT_REQUIRED`, `REPRESENTATIVE_SELECTION_REQUIRED`, `FIRST_CONTACT_REQUIRED`, `SOURCE_NOT_ALLOWED`, `PREFERRED_HOSPITAL_REQUIRED`, `TOO_MANY_PREFERRED_HOSPITALS` (422); `INVALID_RESPONDENT` (409); cảnh báo `MISSING_END_OF_LIFE_WISH`, `MISSING_TREATMENT_WISH`, `NO_FAMILY_MEMBER_TO_NOTIFY` |
| tên logic của lớp test trong tasks, quickstart, data-model: `BR_M01_01_TamVangQuaHanIT`, `BR_M01_02_BaoDanhGiaLaiIT`, `BR_M01_03_DoiMucIT`, `CapNhatThongTinIT`, `DocLapLuuTruMucChamSocIT`, `GhiNhanQuaDoiIT`, `GioiHanTruongHoSoIT`, `HoSoCccdIT`, `HoSoLienKetIT`, `HoSoTrungTenIT`, `LichSuTrangThaiIT`, `NhacLapYeuCauDoiMucIT`, `NhatKyDayDuM01IT`, `TaoHoSoIT`, `TraCuuPhamViIT`, `FR_053_MucTaiNgayIT`; phiếu nguyện vọng: `NguyenVongPhienBanIT`, `NguyenVongNguoiKyIT`, `NguyenVongGioiHanXemIT`, `NhacThieuNguyenVongIT`, `NhacXemLaiNhanDinhIT`, `NguyenVongDieuTriPhienBanIT`, `NguyenVongDieuTriGioiHanXemIT`, `HoanTatTiepNhanCanhBaoDieuTriIT` | `BR_M01_01_TemporaryAbsenceOverdueIT`, `BR_M01_02_ReassessmentNoticeIT`, `BR_M01_03_CareLevelChangeIT`, `UpdatePersonalInfoIT`, `ResidenceTypeAndCareLevelAreIndependentIT`, `RecordDeathIT`, `ProfileFieldLimitsIT`, `ProfileCitizenIdIT`, `ProfileLinkIT`, `ProfileDuplicateNameIT`, `StatusHistoryIT`, `CareLevelChangeRequestReminderIT`, `M01AuditCompletenessIT`, `CreateProfileIT`, `ScopedSearchIT`, `FR_053_CareLevelAtDateIT`; `EndOfLifeWishVersionIT`, `EndOfLifeWishSignerIT`, `EndOfLifeWishVisibilityIT`, `MissingEndOfLifeWishReminderIT`, `SelfDeterminationReviewReminderIT`, `TreatmentWishVersionIT`, `TreatmentWishVisibilityIT`, `CompleteAdmissionWishWarningsIT` (đổi tên 2026-10-23 theo constitution X) |
| `YKienDaGhiTuPhanHoi(phanHoiId, moduleNguon, loaiMuc, mucId, nctId, nguon)`; `moduleNguon` `M04`, `loaiMuc` `SO_THICH` | `OpinionRecordedFromFeedback(feedbackId, sourceModule, itemType, itemId, residentId, source)`; `sourceModule` `M04`, `itemType` `INTEREST` (hằng `SOURCE_ACTIVITY_MODULE`, `ITEM_INTEREST`) |
| `TrangThaiNguoiCaoTuoiApi.giayToDinhKem(lichSuId)`; `TepView`; `TepApi` đọc tệp gắn bản ghi; chủ sở hữu `M01.GIAY_TO_BENH_VIEN`, `M01.QUA_DOI` | `ResidentStatusApi.hospitalDocuments(historyId)`; `FileView`; `FileApi.attachedTo(ownerType, ownerId)`; `M01.HOSPITAL_DOCUMENT`, `M01.DEATH_EVIDENCE` |
| `LuuTruProvider` M01 (`M01.NGUOI_CAO_TUOI` và bản ghi con) | `ResidentRetention` (`M01.RESIDENT`), `ResidentChildRetention` (bảy loại con), `ResidentRetentionBeans` (gói `retention.config`); đọc chung theo bảng con: `ResidentRecordStore` (`resident.internal.persistence`) |
| `GanPhamViProvider` của bản ghi con | `ChildScopeAssignment`, `ChildScopeAssignments` (gói `profile.config`), đọc qua `ResidentRecordStore` |
| `DinhChinhDuocProvider` cho `M01.LICH_SU_TRANG_THAI` | `StatusHistoryCorrectionProvider` (`M01.RESIDENT_STATUS_HISTORY`); nhật ký `RECORD_CORRECTED` |
| thông báo M01 → M13 (`HasNotificationRequests`); sự kiện Chuyển viện, Ghi nhận qua đời | các sự kiện M01 cài `HasNotificationRequests` (lớp `Notices`); `ResidentStatusNotice` |
| nhật ký `CHUYEN_MUC` của lệnh áp dụng đổi mức; nhật ký đính chính gắn người cao tuổi | `CARE_LEVEL_CHANGED` (loại `M01.RESIDENT`); `RECORD_CORRECTED` |
| tên logic test: `NhatKyDayDuM01IT`, `FR_023h_KhongDauNangLucArchTest`, `GiayToBenhVienIT`, `LuuTruM01IT`, `DinhChinhLichSuTrangThaiIT`, `KhoangTrangThaiIT`, `GanPhamViBanGhiConIT` | `M01AuditCompletenessIT` và `M01CommandAuditTrailIT`, `FR_023h_NoCapacityFlagIT`, `HospitalDocumentsIT`, `ResidentRetentionIT`, `StatusHistoryCorrectionIT`, `StatusPeriodsIT`, `ChildScopeAssignmentIT` |
| `ho_so_suc_khoe_ban_dau`; `muc_suc_khoe` | `V001_04__health.sql`; `initial_health_record` (`resident_id`, `blood_type`, `mobility`, `self_care_ability`, `cognition`, `initial_needs_note`, `nutrition_needs`, `current_medications`, `rehabilitation_needs`, `performed_by`, `performed_at`, `confirmed_by`, `confirmed_at`, `version`); `health_item` (`resident_id`, `type`, `allergen_id`, `free_text`, `not_auto_checked`, `severity_id`, `source_id`, `recorded_by`, `recorded_at`, `status`, `excluded_at`, `excluded_by`, `exclusion_reason`, `pending_medication_check`, `pending_nutrition_check`, `version`) |
| `canh_bao_tro_lai` | `V001_06__return_overdue_alert.sql`; `return_overdue_alert` (`absence_id`, `expected_return_at`, `resident_id`, `alerted_at`) |
| lớp truy cập bảng nhắc, lần báo; bản ghi đọc từ các lớp đó | `PeriodicReminderStore` (`resident.internal.persistence`), `ReassessmentNoticeStore`, `ResidentStatusStore.recordOverdueAlert`, `EndOfLifeWishStore.dueReviews`; `DueReminder`, `ReassessmentNotice`, `PeriodicReassessmentDue`, `WishReviewDue`, `NewEndOfLifeWish`, `NewTreatmentWish`, `ResidentMatch` (gói `domain`) |

### 8.7. Bảng sau gộp R-15 *(bổ sung 2026-10-30; thay các dòng bảng đã bỏ ở 8.6; cột chi tiết ở data-model 001 mục 0.1)*

| Tên logic | Mã |
| --- | --- |
| `nguyen_vong` (`loai` `CUOI_DOI`, `DIEU_TRI`; người được báo; khóa hiệu lực) | `wish` (`kind` `END_OF_LIFE`, `TREATMENT`; `notify_family_member_ids`; `active_key`) |
| cột người thân của `ban_dong_y`; nhu cầu của `danh_gia` | `consent.family_member_ids`; `assessment.care_need_ids` |
| loại bảng chung của 001 | `M01.CARE_LEVEL`, `M01.ALLERGEN`, `M01.ALLERGY_SEVERITY`, `M01.INFORMATION_SOURCE`, `M01.CARE_NEED` (danh mục); `M01.WISH_OPINION` (sự kiện); `M01.INITIAL_HEALTH_RECORD`, `M01.REASSESSMENT_NOTICE` (hồ sơ chung); `M01.PERIODIC_REMINDER.<type>`, `M01.RETURN_OVERDUE_ALERT` (mốc đã nhắc); thuộc tính `TRANG_THAI`, `MUC_CHAM_SOC` của `NGUOI_CAO_TUOI` (lịch sử chung) |


## 9. Feature 003 (`003-room-bed-allocation`, C)

**Phase 1 (2026-10-22)**: bảng dưới chỉ chốt tên phần khung/SPI cần cho T001–T002. `contracts/api-003.yaml` chưa dịch; chưa chốt tên endpoint/schema HTTP trong lượt này.

| Tên logic | Tên trong mã |
| --- | --- |
| `cocau`, `giuong`, `phanbo`, `cachly`, `bantru`, `lich` | `internal.structure`, `internal.bed`, `internal.allocation`, `internal.isolation`, `internal.daycare`, `scheduling` |
| `NguonLuuTruChoPhanBo` | `api.spi.AllocationResidencySource` |
| `loaiLuuTru`, `mucTheoHopDong`, `mucTheoLuotDangKy`, `ketThucDuKien`, `giuGiuongKhiVang`, `khacDonGia`, `hoSoChoHetHanGiuCho`, `daPhanBo` | `residencyType`, `careLevelFromContract`, `careLevelFromRegistration`, `expectedEnd`, `holdBedWhenAbsent`, `differentUnitPrice`, `waitingRecordHoldExpired`, `allocated` |
| `DemLichDenBanTru.demTuNgay`, `coLichTuongLai` | `api.spi.DayCareArrivalCounter.countFrom`, `hasFutureArrivals` |
| `TinhTrangLayNhiemChoPhanBo.vungDangKhoanh`, `thuocDanhSach` | `api.spi.AllocationInfectionStatus.cordonedAt`, `listedAt` |
| `ThamChieuViTri.dangThamChieu`, `daTungThamChieu` | `api.spi.LocationReferences.currentReferences`, `everReferenced` |
| `TaiSanGiuongPort.ghiTangChoGiuong`, `mangDauChoChuyenNguoi`, `giuongMangDau` | `internal.bed.domain.BedAssetPort.recordIncreaseForBed`, `awaitingResidentTransfer`, `flaggedBeds` |
| `BuoiNgay`, `BuoiBanTru`, `ViTriRef` | `api.dto.SessionDate` (`date`, `sessionId`), `DayCareSession` (`id`, `name`, `startTime`, `endTime`, `active`), `LocationRef` (`type`, `id`) |
| `GiuGiuong`, `VungKhoanh`, `ThamChieuView` | `api.spi.HeldBed` (`held`, `until`), `CordonedArea` (`floorIds`, `areaIds`), `LocationReferenceView` (`type`, `id`, `label`) |
| Mã mức `CO_BAN`, `THUONG_XUYEN`, `DAC_BIET` trong SPI phân bổ | `api.spi.CareLevelCode`: `BASIC`, `FREQUENT`, `SPECIAL` |
| Loại lưu trú nội trú dài hạn, nội trú ngắn ngày, bán trú trong SPI phân bổ | `api.spi.ResidencyType`: `LONG_TERM`, `SHORT_TERM`, `DAY_CARE` |
| `BT/support/gia003/`, bản giả năm cổng T002 | `support/fake/roombedallocation/`: `FakeM03Config`, `FakeAllocationResidencySource`, `FakeDayCareArrivals`, `FakeAllocationInfectionStatus`, `FakeLocationReferences`, `FakeBedAssets` |
| Màn web 003 | `frontend/src/features/beds/` (đã chốt ở frontend/README.md; Phase 1 chưa sửa web) |

**Phase 2 (2026-10-23)**: khai báo hợp đồng nội bộ và nền dữ liệu; hợp đồng HTTP vẫn chưa dịch.

| Tên logic | Tên trong mã |
| --- | --- |
| `ViTriApi` | `api.ResidentLocationApi`: `current`, `current`, `at`, `floorOn`, `hasNoBed`, `currentOrFutureAllocation` |
| `LichSuViTriApi` | `api.LocationHistoryApi`: `byResident`, `byLocation`, `byLocationOn`, `roommates` |
| `PhanBoGiuongLenhApi` | `api.BedAllocationCommandApi`: `holdForWaitingRecord`, `cancelHold`, `releaseWhenAbsent`, `extendExpectedEnd`, `checkScheduledReservation`, `reserveScheduledTransfer`, `checkScheduledTransfer`, `applyScheduledTransfer`, `cancelScheduledAllocation`, `requestCreatedAfterUrgentTransfer`, `urgentTransferRequestResult` |
| `SucChuaBanTruApi` | `api.DayCareCapacityApi`: `lock`, `occupiedSessions`, `checkCapacity` |
| `CoCauViTriApi` | `api.LocationStructureApi`: `bed`, `room`, `floor`, `area`, `active`, `floorsInArea`, `dayCareRestAreaLocation`, `roomTypeOfBed` |
| `PhongChoPhepApi` | `api.RoomPolicyApi`: `currentRoomAllows`, `allocationProperties` |
| `TinhTrangGiuongApi` | `api.BedStatusApi`: `countByState`, `countAvailable`, `flaggedBeds`, `emptyPeriodStarted`, `overdueAwaitingReady` |
| `GiuongThongKeApi` | `api.BedStatisticsApi`: `stateAt`, `awaitingReady`, `isolatedRooms` |
| `CanhBaoBanGiaoApi` | `api.BedHandoverAlertApi`: `byFloor` |
| `GiuongTheoTaiSanPort`, `LenhTaiSanGiuong` | `internal.bed.domain.BedAssetCommandPort`, `BedAssetCommand` (MAINTENANCE, READY, RETIREMENT) |
| Trạng thái Trống, Đang sử dụng, Đang giữ chỗ, Chờ sẵn sàng, Đang bảo trì, Tạm ngừng, Không sử dụng | `api.dto.BedStatus`: EMPTY, OCCUPIED, HELD, AWAITING_READY, UNDER_MAINTENANCE, SUSPENDED, RETIRED |
| Chính sách phòng nam/nữ/không giới hạn; giới tính người cao tuổi nam/nữ | `api.dto.GenderPolicy`: MALE/FEMALE/UNRESTRICTED; `ResidentGender`: MALE/FEMALE |
| `GiuongTrangThaiService`, bảng chuyển, lịch sử/truy cập trạng thái | `internal.bed.service.BedStateService`, `domain.BedTransitions`, `persistence.BedStateStore` |
| `khu_vuc`, `tang`, `loai_phong`, `loai_phong_muc`, `phong`, `phong_muc_cho_phep`, `ly_do_chuyen_giuong` | `area`, `floor`, `room_type`, `room_type_care_level`, `room`, `room_allowed_care_level`, `bed_transfer_reason` *(R-15: bảng đã gộp trong dòng này xem mục bảng sau gộp của feature)* |
| `giuong`, `lich_su_trang_thai_giuong`; thuộc tính trạng thái/giữ chỗ/hạn | `bed`, `bed_state_history`; `state`, `hold_reason`, `hold_resident_id`, `hold_waiting_record_id`, `hold_until`, `ready_deadline`, `last_closed_allocation_id`, `suspension_reason` |
| `phan_bo_giuong`, `lich_su_cach_ly_phong`, `yeu_cau_nhap_bu`, `nhac_phan_bo` (bảng tạo ở các phase sau) | `bed_allocation` (`resident_id`, `bed_id`, `status`, `previous_allocation_id`), `room_isolation_history` (`room_id`, `occurred_at`), `bed_backfill_request` (`current_allocation_id`), `allocation_reminder` (`target_type`, `target_id`) |
| `thoi_diem`, `thoi_diem_du_kien`, `thuc_hien_tre`, `da_khu_khuan`, `da_dung_bao_ho`, `bat_buoc_khu_khuan` | `occurred_at`, `planned_at`, `executed_late`, `disinfected`, `ppe_used`, `disinfection_required` |
| Lý do thông thường; lý do lây nhiễm/cách ly trong dữ liệu nhật ký | `reason`; `infectionControlReason`, `isolationReason` (HEALTH); cột reason của nhật ký cũng bị che khi các trường này bị che |
| `M03.GIUONG`, `M03.PHONG`, `M03.PHAN_BO_GIUONG`, `M03.LICH_SU_TRANG_THAI_GIUONG`, `M03.LICH_SU_CACH_LY_PHONG`, `M03.XAC_NHAN_SAN_SANG` | `M03.BED`, `M03.ROOM`, `M03.BED_ALLOCATION`, `M03.BED_STATE_HISTORY`, `M03.ROOM_ISOLATION_HISTORY`, `M03.BED_READINESS_CONFIRMATION` |
| `M03_GIUONG_CHUYEN`; `CHUYEN_KHONG_HOP_LE`; `CHUA_GHI_KHU_KHUAN` | `M03_BED_STATE_CHANGED`; `INVALID_BED_TRANSITION`; `DISINFECTION_CONFIRMATION_REQUIRED` |
| Metadata thông báo sự kiện | `api.dto.BedNotificationContext` (`occurredAt`, `floorAreaId`, `recipients`); nhóm người nhận do lệnh nguồn chọn theo G-12 |

| Lịch sử vị trí Phase 7 (T046–T048) | `LocationHistoryReading`, `LocationHistoryQueries`, `LocationHistoryStore`, `HistoryStay`, `HistoryAbsence`, `LocationHistoryController`; HTTP `/location-history/residents/{residentId}`, `/location-history/beds`, `/location-history/roommates` |
| Provider đính chính và HTTP Phase 7 | `AllocationCorrectionProvider`, `ReadinessCorrectionProvider`, `HistoryCorrections`, `RecordCorrection`; `/bed-allocations/{id}/corrections`, `/beds/{id}/state-history/{historyId}/corrections` |
| Projection/store đính chính Phase 7 | `AllocationCorrectionOriginal`, `AllocationCorrectionSnapshot`, `AllocationCorrectionStore`, `ReadinessCorrectionOriginal`, `ReadinessCorrectionStore` |
| Schema web lịch sử và màn trong `features/beds` | `ResidentLocationHistoryRow`, `BedLocationHistoryRow`, `BedResidentHistoryRow`, `RoommateHistoryRow`, `HistoryInterval`, `HistoryAbsenceResponse`; `LocationHistoryPanel`, `AllocationCorrectionForm` |

| Sự kiện logic | Record tại `api.events` |
| --- | --- |
| `GiuongDaTrong` | `BedBecameEmpty` |
| `PhanBoDaBatDau` | `BedAllocationStarted` |
| `PhanBoDaDong` | `BedAllocationClosed` |
| `DaChuyenGiuong` | `BedTransferred` |
| `GiuChoNguoiVangHetHan` | `AbsentResidentHoldExpired` |
| `PhongDaDatCachLy` | `RoomIsolated` |
| `PhongDaGoCachLy` | `RoomIsolationLifted` |
| `PhanBoDaDinhChinh` | `BedAllocationCorrected` |
| `DaChuyenGiuongKhacGia` | `DifferentPriceBedTransfer` (MEDIUM) |
| `BacSiDuocGhiChiDinh` | `DoctorAssignedToAllocation` (LOW) |
| `TroVePhongCachLy` | `ReturnedToIsolatedRoom` (URGENT) |
| `PhanBoKhongConPhuHop` | `AllocationNoLongerSuitable` (MEDIUM) |
| `PhanBoTuongLaiQuaBatDau` | `FutureAllocationPastStart` (MEDIUM) |
| `PhanBoTuongLaiBiHuyDoGiuongHong` | `FutureAllocationCancelledForDamagedBed` (MEDIUM) |
| `ChuyenTheoLichChoGiuong` | `ScheduledTransferWaitingForBed` (MEDIUM) |
| `NhacPhanBo` | `AllocationReminder` (LOW) |
| `NhacGiuongChoSanSang` | `BedReadinessReminder` (LOW) |
| `NguoiRoiGiuongVaoDanhSachTiepXuc` | `FormerOccupantListed` (MEDIUM) |
| `ViecLichTre` | `DelayedScheduledWork` (MEDIUM) |
| `KhuNghiVuotSucChua` | `DayCareOverCapacity` (MEDIUM) |

**Phase 3 — phần backend độc lập (2026-10-23, chưa hoàn tất phase)**:

| Tên logic / vai trò | Tên trong mã |
| --- | --- |
| Khu vực, tầng, loại phòng, phòng, lý do chuyển | `internal.structure.domain.Area`, `Floor`, `RoomType`, `Room`, `BedTransferReason` (entity `CatalogBase`, CATALOG) |
| Giường | `internal.bed.domain.Bed` (BUSINESS; JPA không ghi `state`) |
| Loại danh mục, dữ liệu lệnh nội bộ | `StructureKind`, `StructureChange` (không phải schema HTTP) |
| Dịch vụ / kho cơ cấu | `internal.structure.service.StructureService`, `internal.structure.persistence.StructureStore` |
| Tạo giường và kết quả/cảnh báo | `internal.bed.service.BedService`, `internal.bed.persistence.BedStore`, `BedCreation`, `BedWarning` |
| `VUOT_SO_GIUONG_TIEU_CHUAN` | `STANDARD_BED_COUNT_EXCEEDED` (cảnh báo nội bộ, chưa dịch hợp đồng HTTP) |
| Mã/tên trùng, dữ liệu cơ cấu sai, thiếu nguồn kiểm | `DUPLICATE_CODE`, `INVALID_STRUCTURE`, `SOURCE_UNAVAILABLE` |
| Nhật ký cơ cấu tạo/sửa/ngừng/kích hoạt/xóa | `M03_STRUCTURE_CREATED`, `M03_STRUCTURE_UPDATED`, `M03_STRUCTURE_DEACTIVATED`, `M03_STRUCTURE_REACTIVATED`, `M03_STRUCTURE_DELETED` |
| Nhật ký tạo giường | `M03_STRUCTURE_CREATED`, entity `M03.BED`, `after.roomId/code`, `details.warnings` (ngoài nhật ký chuyển trạng thái) |

**Phase 3 — HTTP/API đọc và web được duyệt (2026-10-23)**:

| Tên logic / vai trò | Tên trong mã / hợp đồng |
| --- | --- |
| /khu-vuc, /tang, /loai-phong, /phong, /ly-do-chuyen-giuong, /giuong | /areas, /floors, /room-types, /rooms, /bed-transfer-reasons, /beds |
| /ngung-hieu-luc, /kich-hoat-lai | /deactivate, /reactivate |
| id, ma, ten, phienBan, trangThai danh mục | id, code, name, version, status (ACTIVE/INACTIVE) |
| khuVucId, tangId, phongId, loaiPhongId, tenToaNha | areaId, floorId, roomId, roomTypeId, buildingName |
| soGiuongTieuChuan, mucMacDinh, mucChoPhep, chinhSachGioiTinh | standardBeds, defaultCareLevels, allowedCareLevels, genderPolicy |
| phucVuDaiHan, phucVuNganNgay, dangCachLy, yTeAnToan, coDinh | servesLongTerm, servesShortTerm, isolated, medicalSafety, fixed |
| Trạng thái giường, vị trí, cảnh báo tạo giường | state (BedStatus), location (LocationResponse), warnings (WarningResponse), STANDARD_BED_COUNT_EXCEEDED |
| Kết quả tạo / bảng giường | BedCreationResponse (bed, warnings), BedBoardResponse (beds, residentsWithoutBed), BedResponse |
| Dấu giường | BedFlagCode: ROOM_UNSUITABLE, AWAITING_DOCTOR_CONFIRMATION, AWAITING_REQUEST, AWAITING_SAME_PRICE (story sau mới cấp dữ liệu) |
| Cổng đọc cơ cấu/chính sách, đọc bảng giường | internal.structure.service.StructureQueries, internal.bed.service.BedQueries, domain.BedDetails |
| Controller theo tài nguyên | web.AreaController, FloorController, RoomTypeController, RoomController, BedTransferReasonController, BedController |
| Dữ liệu ghi / lý do lệnh | web.request.*Write, ReasonBody (reason dùng Reason của common) |
| Problem riêng hợp đồng 003 / người cao tuổi trong schema bảng giường | StructureProblem (M03StructureProblem), ResidentSummarySchema (chỉ mô tả schema, Phase 3 không cấp dữ liệu người ở) |
| Màn cơ cấu, form, khai báo danh mục | frontend/src/features/beds/BedsPage.jsx, StructureForm.jsx, bedCatalog.js |
| Khu nghỉ bán trú tương lai được API đọc | day_care_rest_area (floor_id, area_id, status), đối chiếu migration khi triển khai story bán trú |

Tại mốc chốt Phase 3, 39 operation đã dịch có x-implementation-phase: 3; các component tiếng Việt được giữ cho các phase sau. Generator không sinh client của phần chưa dịch. Đổi phòng giường chưa có đồng bộ tài sản là thao tác tạm hoãn; không có relocateForBed mới trong BedAssetPort.

**Phase 4 — HTTP được duyệt dịch riêng (2026-10-23, chưa triển khai endpoint)**:

| Tên logic | Tên trong hợp đồng/client |
| --- | --- |
| `/giuong/tong-hop` | `GET /beds/summary`, schema `BedSummary` (`byState`, `usable`, `emptyMatching`) |
| `/phan-bo-giuong/giuong-phu-hop` | `GET /bed-allocations/suitable-beds`, schema `SuitableBed` (`location`, `state`, `futureAllocationFrom`, `earliestStartAt`, `roommates`) |
| `/phan-bo-giuong`, `/{id}`, `/{id}/huy` | `POST /bed-allocations`, `GET /bed-allocations/{id}`, `POST /bed-allocations/{id}/cancel` |
| `/nguoi-cao-tuoi/{nctId}/vi-tri` | `GET /residents/{residentId}/location`, schema `ResidentLocation` (`location`, `currentAllocation`, `futureAllocation`, `withoutBedSince`) |
| `PhanBo`, `TrangThaiPhanBo`; Tương lai, Đang hiệu lực, Đã đóng, Đã hủy | `BedAllocation`, `AllocationStatus`; `FUTURE`, `ACTIVE`, `CLOSED`, `CANCELLED` |
| `nctId`, `giuongId`, `batDau`, `tuPhanBoId`, `kiemSoatLayNhiem`, `lyDo` | `residentId`, `bedId`, `startAt`, `fromAllocationId`, `infectionControl`, `reason` |
| `batDauDuKien`, `ketThuc`, `ketThucDuKien`, `lyDoBatDau`, `lyDoKetThuc`, `lyDoHuy` | `plannedStartAt`, `endAt`, `expectedEnd`, `startReason`, `endReason`, `cancellationReason` |
| `nguoiMo`, `nguoiDong`, `canCu`, `phanBoTruocId`, `mucDaKiemTra`, `nguonMuc` | `openedBy`, `closedBy`, `basis` (`type`, `id`), `previousAllocationId`, `checkedCareLevel`, `careLevelSource` (`CURRENT`, `CONTRACT`, `REGISTRATION`) |
| `bacSiChiDinh`, `nhapBu`, `thoiDiemNhapThuc`, `thoiDiemDuKien`, `thucHienTre`, `choGiuongSanSang`, `dau`, `bacSiXacNhan`, `daDinhChinh` | `prescribingDoctor`, `backfilled`, `actualRecordedAt`, `plannedAt`, `executedLate`, `waitingForBedReady`, `flags`, `doctorConfirmation` (`fullName`, `occurredAt`, `reason`), `corrected` |
| `DA_CO_PHAN_BO`, `DIEU_KIEN_CHUA_THOA`, `PB_CHONG_THOI_GIAN` (mô tả lệnh Phase 4) | `ALREADY_ALLOCATED`, `UNMET_CONDITIONS`, `ALLOCATION_TIME_OVERLAP` (chưa có implementation) |

Bảy operation mang `x-implementation-phase: 4` ghi nhận phần đã được duyệt, chưa phải trạng thái hoàn tất. Tổng 46 operation đã dịch (39 Phase 3 + 7 Phase 4); 25 operation phase sau giữ nguyên, tổng 71 operation. Các schema tên logic cũ vẫn phục vụ phase sau. Chủ dự án đã duyệt bổ sung `GET /bed-allocations`: bộ lọc `residentId`, `bedId`, `status` tùy chọn, trả mảng `BedAllocation` trong phạm vi quyền người xem; client sinh `getBedAllocations`.

**Phase 4 — nền dữ liệu và API đọc (cập nhật 2026-10-24)**:

| Tên logic / vai trò | Tên trong mã |
| --- | --- |
| Phân bổ giường, vị trí kỹ thuật theo người, mốc nhắc | `internal.allocation.domain.BedAllocation`, `ResidentPosition`, `AllocationReminder` (khóa lồng `Key`) |
| Trạng thái và nguồn mức nội bộ | `AllocationStatus`, `CareLevelSource`; mức dùng `api.spi.CareLevelCode` hiện có |
| Projection vị trí / kho dữ liệu / bản cài API đọc | `AllocationLocation`, `internal.allocation.persistence.AllocationStore`, `internal.allocation.service.AllocationQueries` |
| Khóa tuần tự theo người rồi giường | `internal.allocation.service.AllocationLocks.lock` (MANDATORY) |
| Bảng khóa người / mốc nhắc / view đã đính chính | `resident_position` (`resident_id`, `without_bed_since`, `version`), `allocation_reminder` (`target_type`, `target_id`, `flag_type`, `started_at`, `last_reminded_at`, `reminder_count`), `v_current_allocation` *(R-15: bảng đã gộp trong dòng này xem mục bảng sau gộp của feature)* |
| Cột phân bổ thời điểm và cột sinh | `start_at`, `planned_start_at`, `end_at`, `expected_end`, `effective_end`, `active_bed_id`, `active_resident_id`, `future_resident_id` |
| Dấu mốc nhắc | `WITHOUT_BED`, `AWAITING_REQUEST`, `AWAITING_SAME_PRICE`, `ROOM_UNSUITABLE` |
| Lỗi nguồn phân bổ mâu thuẫn | `InvalidPermissionDataException` (`source=M03.BED_ALLOCATION`, `errorKind=CONFLICTING_ALLOCATION`) |

`AllocationQueries` cài `BedAllocationApi` của 002 và `ResidentLocationApi` của 003; không sửa chữ ký API/fake. T025 mới có khóa tuần tự; chưa có bộ kiểm điều kiện hoặc retry ở transaction lệnh. T026 mới có phần đọc; chưa có lệnh tạo/hủy phân bổ.

**Phase 5 — phần độc lập được duyệt (2026-10-24; chưa triển khai chuyển giường)**:

| Tên logic / vai trò | Tên trong mã / hợp đồng |
| --- | --- |
| `/phan-bo-giuong/{id}/chuyen-giuong` | `POST /bed-allocations/{id}/transfer` (x-implementation-phase: 5; pending controller) |
| Request `ChuyenGiuong`; `giuongMoiId`, `lyDoChuyenId`, `lyDo`, `thoiDiemChuyen` | requestBody `BedTransfer`; `newBedId`, `transferReasonId`, `reason`, `transferAt` |
| Kết quả chuyển, phân bổ cũ/mới, cảnh báo | `BedTransferResult` (`previousAllocation`, `newAllocation`, `warnings`), dùng `BedAllocation` và `Warning` đã dịch |
| Kết quả lập yêu cầu nhập bù chờ duyệt | `BackfillRequestCreated` (`approvalRequestId`, `status`) |
| Màn danh sách phân bổ để chuyển / form chuyển giường web | `BedTransfersPanel`, `BedTransferForm` trong `frontend/src/features/beds/` (T036) |
| Chỉ chuyển theo chỉ định / cần yêu cầu đổi lưu trú trong mô tả HTTP Phase 5 | `INFECTION_CONTROL_TRANSFER_REQUIRED`, `RESIDENCY_CHANGE_REQUEST_REQUIRED` (service T032 đã có một phần 2026-10-29; HTTP T036 còn pending) |
| Bảng kỹ thuật `yeu_cau_nhap_bu` | `bed_backfill_request` (`approval_request_id`, `resident_id`, `current_allocation_id`, `new_bed_id`, `transfer_at`), migration `V003_04__bed_backfill_request.sql` |
| Mã loại yêu cầu nhập bù trong fixture schema (handler chưa cài) | `M03.BED_ALLOCATION_BACKFILL` (tên logic `M03.NHAP_BU_PHAN_BO`) |
| Tạm ngừng, sử dụng lại, lịch sử trạng thái (T041) | `POST /beds/{id}/suspend`, `POST /beds/{id}/reuse`, `GET /beds/{id}/state-history?after=...`; `BedStateChanged`, `BedStateHistoryRow`, `After` |
| Lịch sử trạng thái / actor / căn cứ | `BedHistoryEntry`; `from`, `to`, `event`, `occurredAt`, `plannedAt`, `executedLate`, `actor`, `reason`, `basis.type/id`, `disinfected`, `ppeUsed`, `note`, `corrected` |
| Giữ chỗ và hết hạn (T040/T044) | `BedHoldService`, `BedHoldStore`, `HoldExpiryProcessor`, `HoldExpiryJob` (`EXPIRE_BED_HOLDS`) |
| Nhật ký giải phóng, gia hạn và hết hạn giữ người vắng | `M03_ALLOCATION_CLOSED`, `M03_EXPECTED_END_EXTENDED`, `M03_ABSENT_HOLD_EXPIRED` |
| Khóa trang lịch sử không hợp lệ | `INVALID_CURSOR` |
| Nhánh trạng thái tài sản giường độc lập (T038/T042) | `BedAssetStateService`, `BedAssetStateStore`; chưa nối adapter `BedAssetCommandPort` đầy đủ |
| Nhật ký hủy phân bổ tương lai do giường hỏng | `M03_FUTURE_ALLOCATION_CANCELLED` |
| Client chuyển giường | `postBedAllocationsByIdTransfer`, sinh từ contract bằng generator |

Tại mốc Phase 5: tổng 71 operation giữ nguyên, gồm 39 Phase 3 +7 Phase 4 +1 Phase 5 đã duyệt, 24 chưa dịch. Clone component mới, giữ component tên logic cho phase sau. T034 mới có migration/test schema, chưa handler phê duyệt; T036 mới có bản dịch và client, chưa controller/UI/mobile.

**T029/T032 — chuyển thường (2026-10-29, phần đang có dấu giá còn hoãn)**:

| Tên logic / vai trò | Tên trong mã |
| --- | --- |
| ChuyenGiuongService; metadata chuyển trực tiếp | BedTransferService; BedTransferStore |
| Nhật ký chuyển giường thường | M03_BED_TRANSFER; đối tượng M03.BED_ALLOCATION |
| Bộ test chuyển thường/đồng thời/sự kiện/hoàn tác | BR_M03_07_BedTransferIT (FR_028, FR_030, FR_032, FR_033) |
| Nhánh đang có dấu giá nhưng chưa có nguồn giá hợp đồng | SOURCE_UNAVAILABLE (503); giữ nguyên AWAITING_REQUEST/AWAITING_SAME_PRICE và reminder theo quyết định giữ SPI |
| Lỗi đã có bản cài ở chuyển trực tiếp chưa có dấu chờ | INFECTION_CONTROL_TRANSFER_REQUIRED, RESIDENCY_CHANGE_REQUEST_REQUIRED; không thay endpoint pending T036 |

### 9.20. Bảng sau gộp R-15 *(bổ sung 2026-10-30; cột chi tiết ở data-model 003 mục 0.1)*

| Tên logic | Mã |
| --- | --- |
| mức chăm sóc cho phép của `phong` | `room.allowed_care_levels` |
| loại bảng chung của 003 | `M03.AREA`, `M03.FLOOR`, `M03.ROOM_TYPE`, `M03.BED_TRANSFER_REASON`, `M03.DAY_CARE_REST_AREA`, `M03.DAY_CARE_SESSION` (danh mục); thuộc tính `CACH_LY` của `PHONG` (lịch sử chung); `M03.BED_BACKFILL_REQUEST` (hồ sơ chung); `M03` (trạng thái theo người); `M03.ALLOCATION_REMINDER.<flagType>` (mốc đã nhắc); `M03.PHAN_BO`, `M03.SUC_CHUA_BAN_TRU`, `M03.KHU_NGHI` (khóa) |
### 9.21. Nguồn phân bổ chờ bác sĩ cho bàn giao (2026-10-31)

| Tên logic | Tên triển khai |
| --- | --- |
| Nguồn phân bổ đang chờ bác sĩ theo tầng | `AllocationHandoverApi.atFloor`,bản cài `BedReportingService` |
| Mốc đặt dấu chờ bác sĩ trên bản phân bổ | `bed_allocation.doctor_confirmation_requested_at`,`BedReportingSnapshot.Flags.doctorConfirmationRequestedAt`; `AllocationHandoverView.createdAt`,NULL với dữ liệu cũ chưa biết mốc |
| Migration bổ sung mốc,không tạo bảng mới | `V003_07__doctor_confirmation_requested_at.sql` |
## 10. Feature 004 (`004-admission-residency`, B)

*Bổ sung 2026-10-25, khi dịch `contracts/api-004.yaml` (chờ chủ dự án duyệt). Tên bảng, cột, lớp nội bộ ghi dự kiến theo plan, data-model, tasks; chỉnh lại cùng thay đổi với mã (R-03). Chỉ sửa mục này.*

Thuật ngữ: lượt đăng ký tiếp nhận = `admission application` (không dùng `registration`, tránh trùng 018 `residence registration`); hồ sơ chờ = `waiting list entry`; điểm ưu tiên = `priority score`; tình huống (đặc biệt) = `circumstance`; lượt trống (của giường) = `vacancy`; giữ chỗ tạm (cho hồ sơ chờ) = `reservation`; giữ giường (khi vắng) = `bed hold`; hợp đồng = `contract`; phụ lục = `addendum` (số nhiều `addenda`); điều khoản khác chuẩn = `non-standard term`; dịch vụ = `service`; đơn giá, phiên bản đơn giá = `unit price`, `price version`; đặt cọc = `deposit`; lượt vắng, ngày vắng = `absence`, `absence day`; chính sách vắng = `absence policy`; hệ số = `coefficient`; lịch đến (bán trú) = `arrival schedule`; báo vắng bán trú = `day care absence notice`; ngày nghỉ bán trú = `day care closure day`; hồ sơ kết thúc lưu trú = `residency ending`; bàn giao người cao tuổi = `resident handover`; ngoại lệ (điều kiện kết thúc) = `ending exception`; kỳ cuối = `final period`; bản ghi qua đời = `death record`; việc sau qua đời = `post-death task`; ý kiến về việc vào ở = `admission opinion`. `nct` trong tên cũ = `resident`.

### 10.1. Đường dẫn API (tiền tố `/api/v1`) và tag

| Tên logic | Mã |
| --- | --- |
| `/luot-dang-ky`, `/{id}`, `…/cap-nhat`, `…/y-kien-vao-o`, `…/dieu-kien-tiep-nhan`, `…/tien-trinh` | `/admission-applications`, `/{id}`, `…/update`, `…/admission-opinion`, `…/admission-conditions`, `…/progress` |
| `/ho-so-cho`, `/{id}`, `…/lich-su-diem`, `…/cap-nhat-tinh-huong`, `…/de-nghi-dieu-chinh-diem` | `/waiting-list-entries`, `/{id}`, `…/score-history`, `…/update-circumstances`, `…/propose-score-adjustment` |
| `/giuong/{giuongId}/ho-so-phu-hop` | `/beds/{bedId}/matching-entries` |
| `…/ghi-nhan-da-lien-he`, `…/tu-choi-giuong`, `…/tu-choi`, `…/huy-cho`, `…/xac-nhan-con-nhu-cau` | `…/record-contacted`, `…/decline-bed`, `…/decline`, `…/cancel-waiting`, `…/confirm-still-needed` |
| `/hop-dong`, `/{id}`, `…/dieu-khoan-khac-chuan`, `…/gui-duyet-dieu-khoan`, `…/gui-ky`, `…/tra-ve-nhap` | `/contracts`, `/{id}`, `…/non-standard-terms`, `…/submit-terms-for-approval`, `…/send-for-signature`, `…/return-to-draft` |
| `…/ghi-nhan-da-ky`, `…/huy`, `…/noi-dung?ngay=`, `…/phu-luc`, `…/dat-coc` | `…/record-signed`, `…/cancel`, `…/content?date=`, `…/addenda`, `…/deposit` |
| `/dich-vu`, `/{id}`, `…/ngung-hieu-luc`, `…/phien-ban-don-gia`, `…/hop-dong-lech-gia` | `/services`, `/{id}`, `…/deactivate`, `…/price-versions`, `…/mismatched-price-contracts` |
| `/yeu-cau-thay-doi-luu-tru`, `/{id}` | `/residency-change-requests`, `/{id}` |
| `/luot-vang`, `/{id}`, `…/gia-han-du-kien`, `…/quyet-dinh-giu-giuong`, `…/dinh-chinh` | `/absences`, `/{id}`, `…/extend-expected-return`, `…/bed-hold-decision`, `…/correct` |
| `/bao-vang-ban-tru`, `/ngay-nghi-ban-tru`, `…/{id}/huy` | `/day-care-absence-notices`, `/day-care-closure-days`, `…/{id}/cancel` |
| `/ho-so-ket-thuc`, `/{id}`, `…/dieu-kien`, `…/doi-ngay`, `…/huy`, `…/ban-giao`, `…/yeu-cau-ngoai-le` | `/residency-endings`, `/{id}`, `…/conditions`, `…/change-date`, `…/cancel`, `…/handover`, `…/request-exception` |
| `/nguoi-cao-tuoi/{nctId}/ban-ghi-qua-doi`, `/ban-ghi-qua-doi/{id}/dinh-chinh` | `/residents/{residentId}/death-record`, `/death-records/{id}/correct` |
| `/nguoi-cao-tuoi/{nctId}/viec-sau-qua-doi`, `…/yeu-cau-ngoai-le-quyet-toan` | `/residents/{residentId}/post-death-tasks`, `…/request-settlement-exception` |
| tham số `nctId`, `giuongId`, `trangThai`, `quaHan`, `loai`, `tu`, `den`, `ngay`, `sau` | `residentId`, `bedId`, `status`, `overdue`, `type`, `from`, `to`, `date`, `cursor` |
| tag `dang-ky`, `danh-sach-cho`, `hop-dong`, `dich-vu`, `thay-doi`, `vang`, `ket-thuc`, `qua-doi` | `applications`, `waiting-list`, `contracts`, `services`, `change-requests`, `absences`, `residency-ending`, `death` |

### 10.2. Schema và trường JSON

| Tên logic | Mã |
| --- | --- |
| `LuotDangKyGhi`, `LuotDangKy` (`nctId`, `ngayDangKy`, `nguoiLienHe`, `nhuCau`, `loaiLuuTruMongMuon`, `mucDuKien`, `ngayMongMuonVao`, `ghiChu`, `trangThai`, `hoSoChoId`, `yKienVaoO`, `yKienLucTiepNhanId`) | `AdmissionApplicationInput`, `AdmissionApplication` (`residentId`, `applicationDate`, `contactPerson`, `needs`, `desiredResidenceType`, `expectedCareLevel`, `desiredAdmissionDate`, `note`, `status`, `waitingListEntryId`, `admissionOpinion`, `opinionAtAdmissionId`) |
| `YKienVaoO` (`yKien`, `nguon`, `ghiChu`, `nguoiGhi`, `thoiDiem`, `trangThai`); thân ghi (`yKien`, `nguon`, `ghiChu`); `NguonYKien` (`loai`, `nguoiThanId`) | `AdmissionOpinion` (`opinion`, `source`, `note`, `recordedBy`, `at`, `status`); `OpinionSource` (`type`, `familyMemberId`, như api-001) |
| `DanhSachDieuKien` (`dieuKien`, `canhBao`, `yKienVaoO`), `DongDieuKien` (`ma`, `ten`, `trangThai`, `canCu`, `module`, `ngoaiLe` (`yeuCauPheDuyetId`, `nguoiDuyet`)), `CanhBao` (`ma`, `moTa`) | `ConditionList` (`conditions`, `warnings`, `admissionOpinion`), `ConditionLine` (`code`, `name`, `status`, `basis`, `module`, `exception` (`approvalRequestId`, `approver`)), `Warning` (`code`, `description`) |
| `TienTrinh` (`buoc` (`ten`, `xong`, `canCu`)) | `Progress` (`steps` (`name`, `done`, `basis`)) |
| `HoSoCho` (`hoTen`, `gioiTinh`, `ngayDangKy`, `loaiLuuTru`, `mucChamSoc`, `chuaDanhGia`, `tinhHuong` (`ma`, `trangThai`), `diem`, `diemChiTiet` (`mucChamSoc`, `thoiGianCho`, `tinhHuong`, `dieuChinh`), `giuongDangGiuId`, `trangThai`) | `WaitingListEntry` (`fullName`, `gender`, `applicationDate`, `residenceType`, `careLevel`, `notAssessed`, `circumstances` (`code`, `status`), `score`, `scoreBreakdown` (`careLevel`, `waitingTime`, `circumstances`, `adjustment`), `heldBedId`, `status`) |
| `DongLichSuDiem` (`thoiDiem`, `diemCu`, `diemMoi`, `nguyenNhan`, `nguoiDeNghi`, `nguoiDuyet`, `lyDo`) | `ScoreHistoryEntry` (`at`, `oldScore`, `newScore`, `cause`, `proposedBy`, `approver`, `reason`) |
| `DanhSachPhuHop` (`giuongId`, `batDauLuotTrong`, `hoSo`) | `MatchingEntries` (`bedId`, `vacancyStart`, `entries`) |
| thân: `luotDangKyId`, `tinhHuong`, `soDiem`, `lyDoKhongTheoThuTu`, `giuongId` | `applicationId`, `circumstances`, `points`, `outOfOrderReason`, `bedId` |
| `HopDongGhi` (`loaiLuuTru`, `ngayBatDau`, `ngayKetThuc`, `mucChamSoc`, `loaiPhongId`, `giuongId`, `kyTinhPhiLuuTru`, `dichVu` (`dichVuId`, `thuocGoi`, `soLuong`, `donGiaRieng`), `khongYeuCauCoc`, `khoanDatCoc`, `chinhSachGiuGiuong`, `chinhSachVangGhiDe`, `lichDen` (`thuTrongTuan`, `ngay`, `gioDen`, `gioVe`), `dieuKienChamDut`) | `ContractInput` (`residenceType`, `startDate`, `endDate`, `careLevel`, `roomTypeId`, `bedId`, `residencyFeeBillingPeriod`, `services` (`serviceId`, `inPackage`, `quantity`, `customUnitPrice`), `noDepositRequired`, `depositAmount`, `bedHoldPolicy`, `absencePolicyOverrides`, `arrivalSchedule` (`dayOfWeek`, `date`, `arrivalTime`, `departureTime`), `terminationCondition`) |
| `DongChinhSachVang` (`loaiVang`, `tuNgayThu`, `denNgayThu`, `heSo`, `giuGiuong`) | `AbsencePolicyLine` (`absenceType`, `fromDay`, `toDay`, `coefficient`, `holdBed`) |
| `HopDong` (`soHopDong`, `ngayKetThucHienHanh`, `dangChoDuyetDieuKhoan`, `dauQuaHanTu`, `ngayKy`, `lyDoChamDut`, `truongBiAn`, `lenhDuocPhep`), `KetQuaHopDong` (`hopDong`, `canhBao`) | `Contract` (`contractNumber`, `currentEndDate`, `awaitingTermsApproval`, `overdueSince`, `signedDate`, `terminationReason`, `hiddenFields`, `allowedCommands`), `ContractResult` (`contract`, `warnings`) |
| `DieuKhoanKhacChuan` (`loai`, `moTa`, `giaTriChuan`, `giaTriDeXuat`) | `NonStandardTerm` (`type`, `description`, `standardValue`, `proposedValue`) |
| thân ghi nhận đã ký (`ngayKy`, `nguoiDaiDienKyId`, `tepBanScanId`) | `signedDate`, `signingRepresentativeId`, `scanFileId` |
| `NoiDungHopDong` (`hopDongId`, `phienBan`, `hieuLucTu`, `phuLucId`, `noiDung`, `donGiaDaGhi`) | `ContractContent` (`contractId`, `version`, `effectiveFrom`, `addendumId`, `content`, `recordedUnitPrices`) |
| `PhuLuc` (`yeuCauPheDuyetId`, `loaiThayDoi`, `noiDungTruoc`, `noiDungSau`, `ngayHieuLuc`, `trangThai`) | `Addendum` (`approvalRequestId`, `changeTypes`, `contentBefore`, `contentAfter`, `effectiveDate`, `status`) |
| `DatCoc` (`khongYeuCau`, `khoanCanDatCoc`, `trangThai`, `canCu` (`thoiDiem`, `giaoDichId`, `keToan`, `soPhieuThu`)) | `Deposit` (`notRequired`, `requiredAmount`, `status`, `basis` (`at`, `transactionId`, `accountant`, `receiptNumber`)) |
| `DichVuGhi`, `DichVu` (`ten`, `donViTinh`, `kyTinh`, `maMuc`, `loaiPhongId`, `thuocId`), `PhienBanDonGia` (`donGia`, `hieuLucTu`, `hieuLucDen`, `daThamChieu`) | `ServiceInput`, `Service` (`name`, `unit`, `billingPeriod`, `careLevelCode`, `roomTypeId`, `medicationId`), `PriceVersion` (`unitPrice`, `effectiveFrom`, `effectiveTo`, `referenced`) |
| hợp đồng lệch giá (`hopDongId`, `soHopDong`, `donGiaDaGhi`, `donGiaMoi`) | `contractId`, `contractNumber`, `recordedUnitPrice`, `newUnitPrice` |
| `YeuCauThayDoiGhi` (`hopDongId`, `loaiThayDoi`, `giaTriSau`, `ngayHieuLucMongMuon`, `thamChieu` (`loai`, `id`)), `YeuCauThayDoi` (`yeuCauPheDuyetId`, `giaTriTruoc`, `anhHuongChiPhi` (`khoan`, `donGiaTruoc`, `donGiaSau`), `thayDoiNho`, `phuLucId`, `nguoiYeuCau`) | `ResidencyChangeRequestInput` (`contractId`, `changeTypes`, `newValue`, `requestedEffectiveDate`, `reference` (`type`, `id`)), `ResidencyChangeRequest` (`approvalRequestId`, `previousValue`, `costImpact` (`item`, `previousUnitPrice`, `newUnitPrice`), `minorChange`, `addendumId`, `requestedBy`) |
| `LuotVang` (`loaiVang`, `roiLuc`, `duKienTroLai`, `veLuc`, `nguoiDon`, `nhanVienBanGiao`, `mangThuoc`, `giuGiuong`, `trangThai`) | `Absence` (`absenceType`, `departedAt`, `expectedReturnAt`, `returnedAt`, `pickedUpBy`, `handoverStaff`, `takesMedication`, `holdBed`, `status`) |
| `LuotVangChiTiet` (`lichSuDuKien` (`giaTriCu`, `giaTriMoi`, `lyDo`, `thoiDiem`), `ngayVang` (`ngay`, `soThuTu`, `heSo`, `giuGiuong`, `nguonDong`, `choQuyetDinh`, `khongCoDong`), `quyetDinh` (`luaChon`, `heSo`, `ngayXemXetLai`, `nguoiQuyetDinh`, `thoiDiem`), `truongBiAn`) | `AbsenceDetail` (`expectedReturnHistory` (`oldValue`, `newValue`, `reason`, `at`), `absenceDays` (`date`, `sequenceNo`, `coefficient`, `holdBed`, `policyLineSource`, `pendingDecision`, `noPolicyLine`), `decisions` (`choice`, `coefficient`, `reviewDate`, `decidedBy`, `at`), `hiddenFields`) |
| thân quyết định giữ giường (`luaChon`, `heSo`, `ngayXemXetLai`, `lyDo`); thân gia hạn (`duKienTroLai`, `lyDo`) | `choice`, `coefficient`, `reviewDate`, `reason`; `expectedReturnAt`, `reason` |
| thân báo vắng bán trú (`nctId`, `ngay`, `buoiId`, `thoiDiemGiaDinhBao`) → phản hồi `duHan`; `NgayNghiBanTru` (`ngay`, `lyDo`, `khaiBaoMuon`, `trangThai`) | `residentId`, `date`, `sessionId`, `familyNotifiedAt` → `withinNoticePeriod`; `DayCareClosureDay` (`date`, `reason`, `lateDeclaration`, `status`) |
| `HoSoKetThuc` (`truongHop`, `lyDoYTe`, `ngayKetThucDuKien`, `thoiDiemKetThucThucTe`, `noiChuyenDen`, `nguoiYeuCau`, `dauQuaNgayDuKienTu`, `banGiao` (`loaiNguoiNhan`, `nguoiNhan`, `thoiDiem`, `nhanVien`), `trangThai`) | `ResidencyEnding` (`endCase`, `medicalReason`, `expectedEndDate`, `actualEndedAt`, `destination`, `requestedBy`, `overdueSince`, `handover` (`recipientType`, `recipient`, `at`, `staff`), `status`) |
| thân bàn giao (`loaiNguoiNhan`, `nguoiThanId`, `tenCoSo`, `thoiDiemBanGiao`, `nhanVienBanGiaoId`); thân ngoại lệ (`maDieuKien`, `lyDo`) | `recipientType`, `familyMemberId`, `facilityName`, `handoverAt`, `handoverStaffId`; `conditionCode`, `reason` |
| `BanGhiQuaDoi` (`thoiDiemQuaDoi`, `diaDiem`, `nguoiPhatHien`, `nguoiXacNhan`, `nguyenNhan`, `tepBangChungId`, `daDinhChinh`, `truongBiAn`) | `DeathRecord` (`deathAt`, `place`, `discoveredBy`, `confirmedBy`, `cause`, `evidenceFileId`, `corrected`, `hiddenFields`) |
| `DanhSachViecSauQuaDoi` (`muc` (`ma`, `trangThai`, `canCu`), `daDongLuc`) | `PostDeathTaskList` (`items` (`code`, `status`, `basis`), `closedAt`) |
| `ThamChieuYeuCau` (`yeuCauPheDuyetId`, `loai`, `trangThai`) | `ApprovalRequestRef` (`approvalRequestId`, `type`, `status`) |
| `Loi` (`maLoi`, `dieuKienChuaThoa`, `giaTriHienHanh`), `DieuKien` (`ma`, `moTa`, `canCu`, `module`, `coTheMien`) | `Problem` (`errorCode`, `unmetConditions`, `currentValue`), `Condition` (`code`, `description`, `basis`, `module`, `waivable`); giữ nguyên cấu trúc gốc của api-004, chưa trùng `Problem` chung của api-000 (xem mục "Điểm cần A quyết định" ở PR) |
| `Id`, `IfMatch`, `IdempotencyKey`, `Sau`; scheme `phien`; response `Loi`, `LoiChan`; thân `LyDo`, `DinhChinh` (`kieu`, `noiDungDung`, `lyDo`); `LyDo`, `MaMucChamSoc`, `LoaiLuuTru` | `Id`, `IfMatch`, `IdempotencyKey`, `Cursor`; `session`; `Problem`, `Conflict`; `ReasonBody`, `CorrectionBody` (`type`, `correctedContent`, `reason`); `Reason`, `CareLevelCode`, `ResidenceType` (như api-001) |

### 10.3. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| `TrangThaiLuotDangKy`: `DANG_XU_LY`, `DANG_CHO`, `DA_TIEP_NHAN`, `DA_HUY` | `ApplicationStatus`: `IN_PROGRESS`, `WAITING`, `ADMITTED`, `CANCELLED` |
| `TrangThaiHoSoCho`: `DANG_CHO`, `DA_LIEN_HE`, `DA_TIEP_NHAN`, `TU_CHOI`, `HUY_CHO` | `WaitingListStatus`: `WAITING`, `CONTACTED`, `ADMITTED`, `DECLINED`, `WAITING_CANCELLED` |
| trạng thái tình huống `XAC_NHAN`, `CHO_XAC_NHAN` | `CONFIRMED`, `PENDING_CONFIRMATION` |
| `TrangThaiHopDong`: `NHAP`, `CHO_KY`, `HIEU_LUC`, `KET_THUC`, `CHAM_DUT`, `DA_HUY` | `ContractStatus`: `DRAFT`, `PENDING_SIGNATURE`, `ACTIVE`, `ENDED`, `TERMINATED`, `CANCELLED` |
| `LoaiDichVu`: `CHAM_SOC`, `PHI_LUU_TRU`, `BUOI_BAN_TRU`, `PHUC_HOI`, `HOAT_DONG`, `DUA_DI_KHAM`, `TIEM`, `NGUOI_THAN_O_LAI`, `SUAT_AN`, `THUOC_NGUON_VIEN`, `KHAC` | `ServiceType`: `CARE`, `RESIDENCY_FEE`, `DAY_CARE_SESSION`, `REHABILITATION`, `ACTIVITY`, `MEDICAL_ESCORT`, `INJECTION`, `FAMILY_STAY`, `MEAL`, `FACILITY_SUPPLIED_MEDICATION`, `OTHER` |
| kỳ tính `THANG`, `NGAY`, `BUOI`, `LAN`, `DON_VI` | `BillingPeriod`: `MONTH`, `DAY`, `SESSION`, `OCCURRENCE`, `UNIT` |
| ý kiến `DONG_Y`, `KHONG_DONG_Y`, `KHONG_BAY_TO_DUOC`; trạng thái ý kiến `HIEU_LUC`, `DUOC_THAY_THE` | `AGREE`, `DISAGREE`, `UNABLE_TO_EXPRESS`; `ACTIVE`, `SUPERSEDED` |
| trạng thái điều kiện `DAT`, `CHUA_DAT`, `KHONG_AP_DUNG`, `DAT_NGOAI_LE`, `TU_DONG` | `MET`, `NOT_MET`, `NOT_APPLICABLE`, `MET_BY_EXCEPTION`, `AUTOMATIC` |
| bước tiến trình `DANG_KY`, `THU_THAP_THONG_TIN`, `DANH_GIA`, `XAC_DINH_LUU_TRU`, `XAC_DINH_CHAM_SOC`, `XAC_DINH_DICH_VU`, `KIEM_TRA_DIEU_KIEN`, `TIEP_NHAN` | `APPLICATION`, `INFORMATION_COLLECTION`, `ASSESSMENT`, `RESIDENCE_TYPE_DETERMINATION`, `CARE_DETERMINATION`, `SERVICE_DETERMINATION`, `CONDITION_CHECK`, `ADMISSION` |
| loại vắng `VE_NHA`, `DI_CHOI_GIA_DINH`, `DI_KHAM_TRONG_NGAY`, `LY_DO_KHAC`, `BENH_VIEN`; lý do chuẩn `VE_NHA_NGUYEN_VONG_CUOI_DOI` | `AbsenceType`: `HOME_VISIT`, `FAMILY_OUTING`, `SAME_DAY_MEDICAL_VISIT`, `OTHER`, `HOSPITAL`; `HOME_VISIT_END_OF_LIFE_WISH` |
| trạng thái lượt vắng `DANG_MO`, `DA_DONG` | `OPEN`, `CLOSED` |
| dòng chính sách: `giuGiuong` `CO`, `KHONG`, `THEO_HOP_DONG`, `QUAN_LY_QUYET_DINH`; `heSo` `THEO_HOP_DONG`, `QUAN_LY_QUYET_DINH`; nguồn dòng `HOP_DONG`, `CO_SO`, `MAC_DINH` | `YES`, `NO`, `PER_CONTRACT`, `MANAGER_DECIDES`; `PER_CONTRACT`, `MANAGER_DECIDES`; `CONTRACT`, `FACILITY`, `DEFAULT` |
| lựa chọn giữ giường `GIU_TIEP`, `GIAI_PHONG`, `CHI_DAT_HE_SO` | `HOLD_CONTINUE`, `RELEASE`, `COEFFICIENT_ONLY` |
| loại điều khoản khác chuẩn `GIA`, `CHINH_SACH_VANG`, `CHINH_SACH_GIU_GIUONG` | `PRICE`, `ABSENCE_POLICY`, `BED_HOLD_POLICY` |
| loại thay đổi `LOAI_LUU_TRU`, `MUC_CHAM_SOC`, `DICH_VU`, `THOI_GIAN`, `GIUONG`, `LICH_DEN`; tham chiếu `DANH_GIA`, `LENH_CHUYEN_GIUONG` | `RESIDENCE_TYPE`, `CARE_LEVEL`, `SERVICE`, `DURATION`, `BED`, `ARRIVAL_SCHEDULE`; `ASSESSMENT`, `BED_TRANSFER_ORDER` |
| trạng thái phụ lục `CHO_HIEU_LUC`, `DA_AP_DUNG`, `KHONG_AP_DUNG` | `PENDING_EFFECTIVE`, `APPLIED`, `NOT_APPLIED` |
| trạng thái đặt cọc `CHUA_DAP_UNG`, `DA_DAP_UNG`, `KHONG_YEU_CAU` (`DepositStatus`) | `NOT_SATISFIED`, `SATISFIED`, `NOT_REQUIRED` |
| trạng thái ngày nghỉ bán trú `HIEU_LUC`, `DA_HUY` | `ACTIVE`, `CANCELLED` |
| trường hợp kết thúc `HET_HOP_DONG`, `XUAT_VIEN`, `CHUYEN_CO_SO`, `CHAM_DUT_HOP_DONG` (+ `QUA_DOI` ở `thongTinQuyetToan`) | `EndCase`: `CONTRACT_EXPIRED`, `DISCHARGE`, `TRANSFER_TO_ANOTHER_FACILITY`, `CONTRACT_TERMINATION` (+ `DEATH`) |
| trạng thái hồ sơ kết thúc `DANG_CHUAN_BI`, `HOAN_TAT`, `DA_HUY`; người nhận `NGUOI_THAN`, `CO_SO_TIEP_NHAN` | `PREPARING`, `COMPLETED`, `CANCELLED`; `FAMILY_MEMBER`, `RECEIVING_FACILITY` |
| mã điều kiện tiếp nhận `TN_B_HOP_DONG`, `TN_C_DAT_COC`, `TN_E_MUC_HOP_DONG` (và (a), (d), (f) theo 001, 012) | `ADM_B_CONTRACT`, `ADM_C_DEPOSIT`, `ADM_E_CONTRACT_LEVEL` |
| mã điều kiện kết thúc `KT_A_CHI_PHI`, `KT_B_DO_GUI`, `KT_C_THUOC_GUI`, `KT_D_CANH_BAO_SU_CO`, `KT_E_BAN_GIAO`, `KT_H_QUYET_TOAN`, `KT_H_XAC_NHAN_QUYET_TOAN` (dùng chung với 006, 007, 010, 013, 017) | `END_A_COSTS`, `END_B_BELONGINGS`, `END_C_STORED_MEDICATION`, `END_D_ALERTS_INCIDENTS`, `END_E_HANDOVER`, `END_H_SETTLEMENT`, `END_H_SETTLEMENT_CONFIRMATION` |
| mục việc sau qua đời `DO_GUI`, `THUOC_GUI`, `CHI_PHI_KY_CUOI`, `CANH_BAO_SU_CO`, `QUYET_TOAN`; trạng thái `CHUA_HOAN_THANH`, `HOAN_THANH`, `KHONG_AP_DUNG` | `PostDeathItemCode`: `BELONGINGS`, `STORED_MEDICATION`, `FINAL_PERIOD_COSTS`, `ALERTS_INCIDENTS`, `SETTLEMENT`; `PostDeathItemStatus`: `NOT_COMPLETED`, `COMPLETED`, `NOT_APPLICABLE` |
| trạng thái có mặt bán trú `CO_MAT`, `VANG_CO_BAO`, `VANG_KHONG_BAO` | `Attendance`: `PRESENT`, `ABSENT_NOTIFIED`, `ABSENT_UNNOTIFIED` |
| nguyên nhân điểm `TINH_HANG_NGAY`, `DOI_MUC`, `DOI_TINH_HUONG`, `DIEU_CHINH`, `DOI_BANG_DIEM`, `TAO` | `ScoreCause`: `DAILY_CALCULATION`, `CARE_LEVEL_CHANGE`, `CIRCUMSTANCE_CHANGE`, `ADJUSTMENT`, `SCORE_TABLE_CHANGE`, `CREATED` |
| kết quả liên hệ giường `LIEN_HE`, `TU_CHOI_GIUONG`, `HET_HAN` | `BedContactResult`: `CONTACTED`, `BED_DECLINED`, `EXPIRED` |
| lệnh `chiTiet` của 001 do 004 sở hữu: `CHO_TAM_VANG` (`loaiVang`, `duKienTroLai`, `nguoiDonId`, `banGhiDonId`, `nhanVienBanGiaoId`, `mangThuoc`, `lyDoChuan`), `CHUYEN_VIEN` (`donViDuaDi`, `duKienTroLai`, `nhanVienBanGiaoId`), `GHI_NHAN_TRO_VE` (`thoiDiemVe`), `KET_THUC_LUU_TRU` (`hoSoKetThucId`, `thoiDiemHieuLuc`), `GHI_NHAN_QUA_DOI` (`thoiDiemQuaDoi`, `diaDiem`, `nguoiPhatHien`, `nguoiXacNhanId`, `nguyenNhan`, `tepBangChungId`), `HUY_TIEP_NHAN` (`hoSoChoId`, `lenhHoSoCho`) | `details` của `GRANT_TEMPORARY_ABSENCE` (`absenceType`, `expectedReturnAt`, `pickedUpById`, `pickupRecordId`, `handoverStaffId`, `takesMedication`, `standardReason`), `TRANSFER_OUT` (`transferUnit`, `expectedReturnAt`, `handoverStaffId`), `RECORD_RETURN` (`returnedAt`), `END_RESIDENCY` (`residencyEndingId`, `effectiveAt`), `RECORD_DEATH` (`deathAt`, `place`, `discoveredBy`, `confirmedById`, `cause`, `evidenceFileId`), `CANCEL_ADMISSION` (`waitingListEntryId`, `waitingListCommand` `DECLINE` hoặc `CANCEL_WAITING`) |
| loại yêu cầu phê duyệt `M02.DIEU_CHINH_DIEM`, `M02.DUYET_DIEU_KHOAN_HOP_DONG`, `M02.THAY_DOI_LUU_TRU`, `M02.NGOAI_LE_KET_THUC` | `M02.SCORE_ADJUSTMENT`, `M02.CONTRACT_TERMS_APPROVAL`, `M02.RESIDENCY_CHANGE`, `M02.ENDING_EXCEPTION` |
| loại đính chính `M02.LUOT_VANG`, `M02.BAN_GHI_QUA_DOI`, `M02.QUYET_DINH_GIU_GIUONG`, `M02.BAN_GIAO_NCT`, `M02.LICH_SU_DIEM` | `M02.ABSENCE`, `M02.DEATH_RECORD`, `M02.BED_HOLD_DECISION`, `M02.RESIDENT_HANDOVER`, `M02.SCORE_HISTORY` |
| loại lưu giữ `M02.LUOT_DANG_KY`, `M02.HO_SO_CHO`, `M02.HOP_DONG`, `M02.LUOT_VANG`, `M02.BAO_VANG_BAN_TRU`, `M02.HO_SO_KET_THUC`, `M02.BAN_GHI_QUA_DOI`, `M02.NGAY_NGHI_BAN_TRU`; loại tệp `M02.BAN_SCAN_HOP_DONG`, `M02.BANG_CHUNG_QUA_DOI` | `M02.ADMISSION_APPLICATION`, `M02.WAITING_LIST_ENTRY`, `M02.CONTRACT`, `M02.ABSENCE`, `M02.DAY_CARE_ABSENCE_NOTICE`, `M02.RESIDENCY_ENDING`, `M02.DEATH_RECORD`, `M02.DAY_CARE_CLOSURE_DAY`; `M02.CONTRACT_SCAN`, `M02.DEATH_EVIDENCE` |
| nhóm trường (`@FieldGroup`) `TAI_CHINH`, `SUC_KHOE` | `FINANCE`, `HEALTH` |
| khóa quyền (`Function`) `TIEP_NHAN_HOP_DONG_DAT_COC`, `DANH_SACH_CHO_DIEM_UU_TIEN`, `YEU_CAU_THAY_DOI_LUU_TRU`, `TAM_VANG_TRO_VE`, `KET_THUC_LUU_TRU_QUA_DOI`, `DICH_VU_DON_GIA` | `ADMISSION_CONTRACT_DEPOSIT`, `WAITING_LIST`, `RESIDENCY_CHANGE_REQUEST`, `ABSENCE_RETURN`, `RESIDENCY_END_DEATH`, `SERVICE_PRICE` (đã có trong `m15.account.api.dto.Function`) |

### 10.4. Mã lỗi và cảnh báo

| Tên logic | Mã |
| --- | --- |
| `DIEU_KIEN_CHUA_THOA`, `CHUYEN_KHONG_HOP_LE` | `CONDITION_NOT_MET` (mã thực của 000), `INVALID_TRANSITION` |
| `DA_CO_LUOT_DANG_KY`, `THIEU_NGUOI_DAI_DIEN` | `APPLICATION_ALREADY_EXISTS` (409), `REPRESENTATIVE_REQUIRED` (409) |
| `HOP_DONG_DA_KHOA`, `HOP_DONG_CHONG_THOI_GIAN` | `CONTRACT_LOCKED` (409), `CONTRACT_OVERLAP` (409) |
| `NGAY_DA_KHAI_BAO`, `BAN_GHI_DON_DA_DUNG` | `DAY_ALREADY_DECLARED` (409), `PICKUP_RECORD_ALREADY_USED` (409) |
| cảnh báo `CHUA_GHI_Y_KIEN_VAO_O`, `NCT_KHONG_DONG_Y_VAO_O` | `ADMISSION_OPINION_MISSING`, `RESIDENT_DISAGREES_WITH_ADMISSION` |
| nhật ký `M02_DANG_KY_*`, `M02_HO_SO_CHO_*`, `M02_DIEM`, `M02_HOP_DONG_*`, `M02_PHU_LUC_*`, `M02_VANG_*` (gồm `M02_VANG_TINH_LAI`), `M02_KET_THUC_*`, `M02_QUA_DOI`, `M02_DICH_VU_*`, `M02_NHAC`, `M02_BAO` | `M02_APPLICATION_*`, `M02_WAITING_ENTRY_*`, `M02_SCORE`, `M02_CONTRACT_*`, `M02_ADDENDUM_*`, `M02_ABSENCE_*` (gồm `M02_ABSENCE_RECALCULATED`), `M02_ENDING_*`, `M02_DEATH`, `M02_SERVICE_*`, `M02_REMINDER`, `M02_NOTICE` |

### 10.5. Gói, giao diện công khai `admission.api`, sự kiện (T001, T003, T004)

| Tên logic | Mã |
| --- | --- |
| gói chức năng `dangky`, `danhsachcho`, `hopdong`, `dichvu`, `thaydoi`, `vang`, `ketthuc`, `quadoi`, `tinhphi`, `trangthai`; `lich` | `internal.application`, `internal.waitinglist`, `internal.contract`, `internal.servicecatalog` (tránh trùng tầng `service`), `internal.change`, `internal.absence`, `internal.ending`, `internal.death`, `internal.residencyday`, `internal.transition`; `scheduling` |
| `admission.api` (`api/sukien`); `@NamedInterface` | `admission.api` (`dto`, `spi`, `events`); `admission`, `admission-dto`, `admission-spi`, `admission-events` |
| `HopDongDocApi` (`noiDungTaiNgay`, `hopDong`, `cuaNguoiCaoTuoi`, `khoanDatCoc`, `lichDenTaiNgay`) | `ContractReadApi` (`contentAtDate`, `contract`, `ofResident`, `depositAmount`, `arrivalScheduleAtDate`); `ContractContentView`, `ContractView`, `DepositAmountView`, `ArrivalScheduleView` |
| `DonGiaApi` (`donGiaTaiNgay`, `donGiaHopDong`, `dichVuChamSoc`, `phiLuuTruTheoLoaiPhong`, `thuocNguonVien`; `danhDauThamChieu`) | `UnitPriceApi` (`unitPriceAt`, `contractUnitPrice`, `careServiceOf`, `residencyFeeServiceOf`, `facilityMedicationServiceOf`; `markReferenced`); `UnitPriceView`, `ServiceView` |
| `NgayLuuTruApi` (`theoKhoang`, `buoiBanTru`, `mocDungTinhPhi`); `NgayLuuTruView`, `BuoiBanTruView` | `ResidencyDayApi` (`daysBetween`, `dayCareSessions`, `billingStopDate`); `ResidencyDayView`, `DayCareSessionView` |
| `DieuKienTiepNhanApi` (`dieuKienTiepNhan`, `tienTrinh`, `dieuKienKetThuc`) | `AdmissionConditionApi` (`admissionConditions`, `progress`, `endingConditions`) |
| `HoSoKetThucDocApi` (`dangChuanBi`, `thongTinQuyetToan`, `coNgoaiLeQuyetToanDaDuyet`) | `ResidencyEndingReadApi` (`preparing`, `settlementInfo`, `hasApprovedSettlementException`) |
| `LuuTruThongKeApi` (`loaiLuuTruTaiNgay`, `danhSachCho`) | `ResidencyStatisticsApi` (`residenceTypeAtDate`, `waitingList`) |
| `DanhSachViecSauQuaDoiApi` (`capNhat`, `xem`) | `PostDeathTaskApi` (`refresh`, `view`) |
| SPI 004 khai báo: `NguoiDaiDienPort` (`laNguoiDaiDienHieuLuc`, `coNguoiDaiDien`), `BanGhiDonPort` (`banGhiDonHopLe`, `daDung`), `TrangThaiDatCocPort` (`trangThai`), `KyCuoiPort` (`taoKyCuoiNhap`, `tinhLai`, `huyMocDung`), `TrangThaiCoMatBanTruPort` (`trangThai`), `MucViecSauQuaDoiPort` (`ma`, `trangThai`) | `RepresentativePort` (`isActiveRepresentative`, `hasRepresentative`), `PickupRecordPort` (`validPickupRecord`, `markUsed`), `DepositStatusPort` (`status`), `FinalPeriodPort` (`createDraft`, `recalculate`, `cancelStop`), `DayCareAttendancePort` (`status`), `PostDeathItemPort` (`code`, `status`) |
| SPI M02 cài cho module thấp hơn: `YeuCauDoiMucApi`, `HopDongApi`, `LuotVangApi` (001); `KetThucLuuTruApi` (002) | `CareLevelChangeRequestApi`, `ContractApi`, `AbsenceApi` (đã đặt tên ở mục 8.5); `ResidencyEndApi` (`m15.account.api.spi`, như team-plan mục 8) |
| SPI 003 M02 cài: `NguonLuuTruChoPhanBo`, `DemLichDenBanTru`; API 003 M02 gọi: `PhanBoGiuongLenhApi`, `SucChuaBanTruApi` | tên tiếng Anh do C chốt ở mục 9 khi dịch api-003; 004 dùng đúng tên đó, chưa đặt trước |
| `GiuongTrongCanChonHoSo`, `LechMucHopDong`, `HopDongDaDoiTrangThai`, `KhoanCocDaDoi`, `TiepNhanHoanTat` | `BedAvailableForWaitingList(bedId, matchingEntryCount)`, `ContractCareLevelMismatch(residentId, assessedLevel, contractLevel, contractStatus)`, `ContractStatusChanged(contractId, residentId, from, to, reason)`, `DepositAmountChanged(contractId, newAmount)`, `AdmissionCompleted(residentId, contractId, residenceType, startDate, expectedEndDate)` |
| `PhuLucDaApDung`, `LuotVangDaMo`, `LuotVangDaGiaHan`, `LuotVangDaDong`, `NgayLuuTruDaDoi` | `AddendumApplied(addendumId, contractId, residentId, changeTypes, effectiveDate)`, `AbsenceOpened`, `AbsenceExtended`, `AbsenceClosed` (`absenceId`, `residentId`, mốc), `ResidencyDaysChanged(residentId, fromDate, toDate, cause)` |
| `LoaiLuuTruDaDoi(nctId)` | `ResidenceTypeChanged(residentId)` |
| `CanQuyetDinhGiuGiuong`, `DenNgayXemXetGiuGiuong`, `KhongCoDongChinhSach`, `HopDongQuaHanBaoQuanLy`, `NhacM02`, `DatCocChuaDapUngSauLuuTru`, `YeuCauNguoiDaiDienCanHoanThien` | `BedHoldDecisionNeeded(absenceId, days, policyLine)`, `BedHoldReviewDue(absenceId)`, `NoAbsencePolicyLine(absenceId, date)`, `ContractOverdueEscalated(contractId, days)`, `M02Reminder(type, subjectId)`, `DepositNotSatisfiedAfterAdmission(contractId)`, `RepresentativeRequestNeedsCompletion(requestId)` |
| `HoSoKetThucDaLap`, `…DaDoiNgay`, `…DaHuy`, `…HoanTat`; `QuaDoiDaGhiNhan`; `NgoaiLeKetThucDaDuyet`; `NgayNghiBanTruDaDoi`, `BaoVangBanTruDaGhi`; `PhienBanDonGiaDaTao` | `ResidencyEndingCreated`, `ResidencyEndingDateChanged`, `ResidencyEndingCancelled`, `ResidencyEndingCompleted` (`residencyEndingId`, `residentId`, ngày); `DeathRecorded(residentId, at, recordId)`; `EndingExceptionApproved(residencyEndingId, residentId, conditionCode)` (`residencyEndingId` rỗng khi qua đời, Q-387); `DayCareClosureDayChanged(date, active)`, `DayCareAbsenceNoticeRecorded(residentId, date, sessionId, withinNoticePeriod)`; `PriceVersionCreated(serviceId, effectiveFrom, effectiveTo)` |
| sự kiện 004 nghe: `NguoiCaoTuoiDaChuyenTrangThai`, `DanhGiaDauVaoDaXacNhan`, `MucChamSocDaDoi` (001); `GiuongDaTrong` (003); `YeuCauDaQuyetDinh`, `YeuCauApDungKhongThanh`, `YeuCauDaHuy`, `ThamSoDaDoi` (000) | `ResidentStatusChanged`, `InitialAssessmentConfirmed`, `CareLevelChanged`; `BedBecameVacant` (tên do C chốt, mục 9); `RequestDecided`, `RequestApplyFailed`, `RequestCancelled`, `ParameterChanged` |
| `TrangThaiDatCocDaDoi`, `DieuKienQuyetToanDaDoi` (017 phát) | `DepositStatusChanged`, `SettlementConditionChanged` (tên do E chốt ở mục 23; 004 chỉ nghe) |
| `ThamSoApi`, `NhatKyApi`, `DinhChinhApi`, `YeuCauPheDuyetApi`, `TepApi`, `TaiKhoanApi.coVaiTro` | `ParameterApi`, `AuditLogApi`, `CorrectionApi`, `ApprovalRequestApi`, `FileApi`, `AccountApi.hasRole` (như mục 6, 7, 8) |

### 10.6. Bảng, cột, lớp và migration (T005 → T009, T015, T023, T032, T044, T051, T058, T063)

| Tên logic | Mã |
| --- | --- |
| `V004_01__luu_tru_nct.sql`, `V004_02__dich_vu.sql`, `V004_03__luot_dang_ky.sql`, `V004_04__danh_sach_cho.sql`, `V004_05__hop_dong.sql`, `V004_06__phu_luc.sql`, `V004_07__vang_mat.sql`, `V004_08__ket_thuc.sql`, `V004_09__qua_doi.sql` | `V004_01__resident_residency.sql`, `V004_02__service_catalog.sql`, `V004_03__admission_application.sql`, `V004_04__waiting_list.sql`, `V004_05__contract.sql`, `V004_06__addendum.sql`, `V004_07__absence.sql`, `V004_08__residency_ending.sql`, `V004_09__death_record.sql` |
| `luu_tru_nct` (`nct_id`, `da_dong_luc`, `phien_ban`); `lan_chay_theo_ngay` (`viec`, `ngay`, `ket_qua`, `lan_chay_lich_id`); `nhac_m02` (`loai`, `doi_tuong_id`, `lan_nhac_cuoi`, `so_lan`) | `resident_residency` (`resident_id`, `closed_at`, `version`); `daily_run` (`task`, `date`, `result`, `scheduler_run_id`); `m02_reminder` (`type`, `subject_id`, `last_reminded_at`, `count`) |
| `luot_dang_ky` (`nct_id`, `ngay_dang_ky`, `nguoi_lien_he`, `nhu_cau`, `ghi_chu`, `loai_luu_tru_mong_muon`, `muc_du_kien`, `ngay_mong_muon_vao`, `trang_thai`, `nct_dang_mo`, `y_kien_luc_tiep_nhan_id`) | `admission_application` (`resident_id`, `application_date`, `contact_person`, `needs`, `note`, `desired_residence_type`, `expected_care_level`, `desired_admission_date`, `status`, `open_resident_id`, `opinion_at_admission_id`) |
| `y_kien_vao_o` (`luot_dang_ky_id`, `y_kien`, `nguon_loai`, `nguon_nguoi_than_id`, `nguoi_ghi_id`, `thoi_diem`, `ghi_chu`, `trang_thai`, `thay_the_boi_id`, `luot_hieu_luc`) | `admission_opinion` (`application_id`, `opinion`, `source_type`, `source_family_member_id`, `recorded_by`, `recorded_at`, `note`, `status`, `superseded_by_id`, `active_application_id`) |
| `ho_so_cho` (`luot_dang_ky_id`, `nct_id`, `muc_cham_soc`, `muc_chua_danh_gia`, `diem_hien_hanh`, `diem_chi_tiet`, `giuong_dang_giu_id`, `moc_cap_nhat`, `trang_thai`, `nct_dang_mo`, `giuong_giu_duy_nhat`); `ho_so_cho_tinh_huong` (`ma_tinh_huong`, `trang_thai`) | `waiting_list_entry` (`application_id`, `resident_id`, `care_level`, `care_level_not_assessed`, `current_score`, `score_breakdown`, `held_bed_id`, `last_confirmed_at`, `status`, `open_resident_id`, `held_bed_unique`); `waiting_list_circumstance` (`entry_id`, `circumstance_code`, `status`) |
| `lich_su_diem` (`ho_so_cho_id`, `diem_cu`, `diem_moi`, `thanh_phan`, `nguyen_nhan`, `yeu_cau_phe_duyet_id`, `thoi_diem`); `lien_he_giuong` (`ho_so_cho_id`, `giuong_id`, `ket_qua`, `thoi_diem`, `nguoi_thuc_hien_id`, `ly_do`, `ly_do_khong_theo_thu_tu`) | `score_history` (`entry_id`, `old_score`, `new_score`, `components`, `cause`, `approval_request_id`, `at`); `bed_contact` (`entry_id`, `bed_id`, `result`, `at`, `performed_by`, `reason`, `out_of_order_reason`) |
| `hop_dong` (`so_hop_dong`, `nct_id`, `loai_luu_tru`, `ngay_bat_dau`, `ngay_ket_thuc`, `ngay_ket_thuc_hien_hanh`, `muc_cham_soc`, `loai_phong_id`, `giuong_id`, `nguoi_dai_dien_ky_id`, `ngay_ky`, `tep_ban_scan_id`, `ky_tinh_phi_luu_tru`, `khoan_dat_coc`, `khong_yeu_cau_coc`, `chinh_sach_giu_giuong`, `dieu_kien_cham_dut`, `bam_noi_dung`, `dang_cho_duyet_dieu_khoan`, `yeu_cau_duyet_dieu_khoan_id`, `trang_thai`, `ly_do_cham_dut`, `dau_qua_han_tu`, `da_bao_qlv_qua_han_luc`) | `contract` (`contract_number`, `resident_id`, `residence_type`, `start_date`, `end_date`, `current_end_date`, `care_level`, `room_type_id`, `bed_id`, `signing_representative_id`, `signed_date`, `scan_file_id`, `residency_fee_billing_period`, `deposit_amount`, `no_deposit_required`, `bed_hold_policy`, `termination_condition`, `content_hash`, `awaiting_terms_approval`, `terms_approval_request_id`, `status`, `termination_reason`, `overdue_since`, `manager_notified_overdue_at`) |
| `hop_dong_dich_vu`, `hop_dong_chinh_sach_vang`, `hop_dong_lich_den`; `phien_ban_noi_dung_hop_dong` (`so_thu_tu`, `hieu_luc_tu`, `phu_luc_id`, `noi_dung`) | `contract_service` (`in_package`, `quantity`, `custom_unit_price`, `recorded_unit_price`), `contract_absence_policy`, `contract_arrival_schedule` (`day_of_week`, `date`, `arrival_time`, `departure_time`); `contract_content_version` (`sequence_no`, `effective_from`, `addendum_id`, `content`) |
| trigger `trg_hop_dong_khoa_noi_dung`, `trg_hop_dong_khong_chong`, `trg_phien_ban_don_gia_khong_chong`, `trg_phien_ban_don_gia_khong_sua`, `trg_luot_vang_khong_sua` | `trg_contract_lock_content`, `trg_contract_no_overlap`, `trg_price_version_no_overlap`, `trg_price_version_immutable`, `trg_absence_immutable_when_closed` |
| `dich_vu` (`loai`, `ten`, `don_vi_tinh`, `ky_tinh`, `ma_muc`, `muc_hieu_luc`, `ten_hieu_luc`, `loai_phong_id`, `thuoc_id`); `phien_ban_don_gia` (`dich_vu_id`, `don_gia`, `hieu_luc_tu`, `hieu_luc_den`, `da_tham_chieu`) | `service` (`type`, `name`, `unit`, `billing_period`, `care_level_code`, `active_care_level`, `active_name`, `room_type_id`, `medication_id`); `price_version` (`service_id`, `unit_price`, `effective_from`, `effective_to`, `referenced`) |
| `phu_luc_hop_dong` (`hop_dong_id`, `yeu_cau_phe_duyet_id`, `loai_thay_doi`, `noi_dung_truoc`, `noi_dung_sau`, `ngay_hieu_luc`, `trang_thai`, `phien_ban_noi_dung_id`); `yeu_cau_thay_doi_thuoc_tinh` (`thuoc_tinh`, `cho_hieu_luc`, `khoa`) | `contract_addendum` (`contract_id`, `approval_request_id`, `change_types`, `content_before`, `content_after`, `effective_date`, `status`, `content_version_id`); `change_request_attribute` (`attribute`, `pending_effective`, `lock_key`) |
| `luot_vang` (`nct_id`, `loai_vang`, `ly_do`, `ly_do_chuan`, `roi_luc`, `du_kien_tro_lai`, `ve_luc`, `nguoi_don_id`, `ban_ghi_don_id`, `don_vi_dua_di`, `nhan_vien_ban_giao_id`, `mang_thuoc`, `giu_giuong_ban_dau`, `da_bao_nguong_luc`, `ngay_xem_xet_lai`, `luot_truoc_id`, `trang_thai`, `nct_dang_mo`) | `absence` (`resident_id`, `absence_type`, `reason`, `standard_reason`, `departed_at`, `expected_return_at`, `returned_at`, `picked_up_by_id`, `pickup_record_id`, `transfer_unit`, `handover_staff_id`, `takes_medication`, `initial_bed_hold`, `threshold_notified_at`, `review_date`, `previous_absence_id`, `status`, `open_resident_id`) |
| `lich_su_du_kien_tro_lai`; `ngay_vang` (`luot_vang_id`, `ngay`, `so_thu_tu`, `he_so`, `giu_giuong`, `nguon_dong`, `dong_chinh_sach`, `dau_cho_quyet_dinh`, `dau_khong_co_dong`, `quyet_dinh_id`, `tinh_luc`, `thoi_diem_tham_so`); `quyet_dinh_giu_giuong` (`ngay_nguong`, `lua_chon`, `he_so`, `ngay_xem_xet_lai`, `ap_tu_ngay`, `ap_den_ngay`, `nguoi_quyet_dinh_id`) | `expected_return_history`; `absence_day` (`absence_id`, `date`, `sequence_no`, `coefficient`, `hold_bed`, `policy_line_source`, `policy_line`, `pending_decision_flag`, `no_policy_line_flag`, `decision_id`, `calculated_at`, `parameter_at`); `bed_hold_decision` (`threshold_date`, `choice`, `coefficient`, `review_date`, `applies_from`, `applies_to`, `decided_by`) |
| `bao_vang_ban_tru` (`nct_id`, `ngay`, `buoi_id`, `thoi_diem_gia_dinh_bao`, `du_han`); `ngay_nghi_ban_tru` (`ngay`, `ly_do`, `trang_thai`, `ngay_hieu_luc`, `khai_bao_muon`) | `day_care_absence_notice` (`resident_id`, `date`, `session_id`, `family_notified_at`, `within_notice_period`); `day_care_closure_day` (`date`, `reason`, `status`, `active_date`, `late_declaration`) |
| `ho_so_ket_thuc` (`nct_id`, `truong_hop`, `ly_do_y_te`, `noi_chuyen_den`, `ngay_ket_thuc_du_kien`, `thoi_diem_ket_thuc_thuc_te`, `nguoi_yeu_cau`, `dau_qua_ngay_du_kien_tu`, `ky_cuoi_tham_chieu`, `trang_thai`, `nct_dang_mo`) | `residency_ending` (`resident_id`, `end_case`, `medical_reason`, `destination`, `expected_end_date`, `actual_ended_at`, `requested_by`, `overdue_since`, `final_period_ref`, `status`, `open_resident_id`) |
| `ban_giao_nct` (`ho_so_ket_thuc_id`, `loai_nguoi_nhan`, `nguoi_than_id`, `ten_co_so`, `thoi_diem_ban_giao`, `nhan_vien_ban_giao_id`); `ngoai_le_dieu_kien` (`ho_so_ket_thuc_id`, `nct_id`, `ma_dieu_kien`, `yeu_cau_phe_duyet_id`) | `resident_handover` (`ending_id`, `recipient_type`, `family_member_id`, `facility_name`, `handover_at`, `handover_staff_id`); `ending_condition_exception` (`ending_id`, `resident_id`, `condition_code`, `approval_request_id`) |
| `ban_ghi_qua_doi` (`nct_id`, `thoi_diem_qua_doi`, `dia_diem`, `nguoi_phat_hien`, `nguoi_xac_nhan_id`, `nguyen_nhan`, `tep_bang_chung_id`); `muc_viec_sau_qua_doi` (`ban_ghi_qua_doi_id`, `ma_muc`, `trang_thai`, `can_cu`, `cap_nhat_luc`) | `death_record` (`resident_id`, `death_at`, `place`, `discovered_by`, `confirmed_by_id`, `cause_of_death`, `evidence_file_id`); `post_death_task` (`death_record_id`, `item_code`, `status`, `basis`, `updated_at`) |
| loại nhắc `HO_SO_CHO_CAP_NHAT`, `PHAN_BO_LAI`, `HOP_DONG_SAP_HET`, `HOP_DONG_NGAN_NGAY`, `QUA_HAN_HOP_DONG`, `QUA_NGAY_DU_KIEN`, `VIEC_SAU_QUA_DOI`, `CHUA_VAO_O`, `XEM_XET_GIU_GIUONG` | `WAITING_ENTRY_UPDATE`, `REALLOCATION`, `CONTRACT_EXPIRING`, `SHORT_TERM_CONTRACT`, `CONTRACT_OVERDUE`, `PAST_EXPECTED_DATE`, `POST_DEATH_TASKS`, `NOT_YET_ADMITTED`, `BED_HOLD_REVIEW` |
| `ViecHangNgayM02` (việc `TINH_DIEM`, `NGAY_VANG`, `HOP_DONG`, `KET_THUC`, `NHAC`) | `M02DailyJob` (tên job `M02_DAILY`; việc `CALCULATE_SCORES`, `ABSENCE_DAYS`, `CONTRACTS`, `ENDINGS`, `REMINDERS`); cấu hình `eldercare.jobs.m02-daily` |
| `KhoaLuuTru`, `ThamGiaM02`, `DonGiaService`, `LuotDangKyService`, `DieuKienTiepNhanService`, `YKienVaoOService`, `TinhDiemUuTien`, `HoSoChoService` | `ResidencyLock`, `M02StatusParticipant`, `UnitPriceService`, `AdmissionApplicationService`, `AdmissionConditionService`, `AdmissionOpinionService`, `PriorityScoreCalculator`, `WaitingListService` |
| `HopDongService`, `XacDinhKhacChuan`, `HopDongDocService`, `ThayDoiLuuTruHandler`, `LaThayDoiNho`, `TraChinhSachVang`, `LuotVangService`, `HoSoKetThucService`, `HoSoKetThucDocApiImpl`, `ViecSauQuaDoiService`, `DichVuService`, `NgayLuuTruService` | `ContractService`, `NonStandardTermDetector`, `ContractReadService`, `ResidencyChangeHandler` (`typeCode` `M02.RESIDENCY_CHANGE`), `IsMinorChange`, `AbsencePolicyResolver`, `AbsenceService`, `ResidencyEndingService`, `ResidencyEndingReadApiImpl`, `PostDeathTaskService`, `ServiceCatalogService`, `ResidencyDayService` |
| `LuotDangKyController`, `HoSoChoController`, `HopDongController`, `DichVuController`, `YeuCauThayDoiController`, `VangController`, `KetThucController`, `QuaDoiController` | `AdmissionApplicationController`, `WaitingListController`, `ContractController`, `ServiceCatalogController`, `ResidencyChangeRequestController`, `AbsenceController`, `ResidencyEndingController`, `DeathRecordController` |
| `PhanLoaiTruongProvider`, `LuuTruProvider`, `GanPhamViProvider`, `QuyenTepProvider`, `DinhChinhDuocProvider` | `FieldClassificationProvider`, `RetentionProvider`, `ScopeAssignmentProvider`, `FilePermissionProvider`, `CorrectableRecordProvider` |
| bản giả `support/gia004/` | `support/fake/m02/` (`FakeM02Config`; bản giả của sáu SPI 004 khai báo) |
| hàm thuần: điểm ưu tiên, tra chính sách vắng, điều khoản khác chuẩn, nội dung tại ngày | `PriorityScoreCalculator`, `AbsencePolicyResolver`, `NonStandardTermDetector`, `ContractReadService.contentAtDate` |

### 10.7. Lớp test (quickstart mục 4, tasks)

| Tên logic | Mã |
| --- | --- |
| `LuotDangKyIT`, `CapNhatLuotDangKyIT`, `TienTrinhIT`, `FR_004a_YKienVaoOIT`, `FR_004_DanhSachKhopLenhIT`, `BR_M02_22_MucHopDongIT`, `FR_076_CanhBaoChuaVaoOIT` | `AdmissionApplicationIT`, `UpdateAdmissionApplicationIT`, `ProgressIT`, `FR_004a_AdmissionOpinionIT`, `FR_004_ConditionsMatchCommandIT`, `BR_M02_22_ContractCareLevelIT`, `FR_076_NotYetAdmittedWarningIT` |
| `BR_M02_10_DiemUuTienTest`, `DiemHangNgayIT`, `DieuChinhDiemIT`, `BR_M02_01_DanhSachPhuHopIT`, `FR_012_LuotTrongIT`, `BR_M02_02_GiuChoIT`, `GhiNhanLienHeDongThoiIT`, `FR_002_TuChoiHuyTiepNhanIT`, `BR_M02_03_NhacHoSoChoIT` | `BR_M02_10_PriorityScoreTest`, `DailyScoreIT`, `ScoreAdjustmentIT`, `BR_M02_01_MatchingEntriesIT`, `FR_012_VacancyIT`, `BR_M02_02_BedReservationIT`, `ConcurrentContactIT`, `FR_002_DeclineCancelAdmissionIT`, `BR_M02_03_WaitingEntryReminderIT` |
| `HopDongVongDoiIT`, `HopDongKhoaNoiDungDbIT`, `DBR_06_HopDongKhongChongIT`, `FR_021_GhiNhanDaKyIT`, `XacDinhKhacChuanTest`, `DuyetDieuKhoanIT`, `BR_M02_09_NoiDungTaiNgayIT`, `BR_M02_05_NhacHopDongIT`, `FR_029_QuaHanHopDongIT` | `ContractLifecycleIT`, `ContractContentLockDbIT`, `DBR_06_ContractNoOverlapIT`, `FR_021_RecordSignedIT`, `NonStandardTermDetectorTest`, `TermsApprovalIT`, `BR_M02_09_ContentAtDateIT`, `BR_M02_05_ContractReminderIT`, `FR_029_ContractOverdueIT` |
| `DatCocDieuKienIT`, `FR_032_DatCocSauLuuTruIT`, `SuKien017018IT` | `DepositConditionIT`, `FR_032_DepositAfterAdmissionIT`, `Events017018IT` |
| `BR_M02_04_ApDungPhuLucIT`, `DBR_07_PhuLucIT`, `FR_041_KiemTraKhiDuyetIT`, `FR_046_TrungThuocTinhIT`, `ApDungKhongThanhIT`, `YeuCauNguoiDaiDienIT`, `DoiLichDenIT`, `LaThayDoiNhoTest`, `FR_043_ThayDoiNhoIT` | `BR_M02_04_ApplyAddendumIT`, `DBR_07_AddendumIT`, `FR_041_CheckOnApprovalIT`, `FR_046_DuplicateAttributeIT`, `ApplyFailedIT`, `RepresentativeRequestIT`, `ChangeArrivalScheduleIT`, `IsMinorChangeTest`, `FR_043_MinorChangeIT` |
| `BR_M02_06_TraChinhSachTest`, `BR_M02_17_QuanLyQuyetDinhNgay1Test`, `NgayVangIT`, `BR_M02_07_QuyetDinhGiuGiuongIT`, `BR_M02_18_DinhChinhVuotNguongIT`, `FR_057_ChuyenVienKhiTamVangIT`, `BanTruHeSoIT`, `NgayNghiBanTruIT` | `BR_M02_06_AbsencePolicyLookupTest`, `BR_M02_17_ManagerDecidesDay1Test`, `AbsenceDayIT`, `BR_M02_07_BedHoldDecisionIT`, `BR_M02_18_CorrectionOverThresholdIT`, `FR_057_TransferOutDuringAbsenceIT`, `DayCareCoefficientIT`, `DayCareClosureDayIT` |
| `HoSoKetThucIT`, `DieuKienKetThucIT`, `NgoaiLeKetThucIT`, `FR_066_KyCuoiIT`, `FR_066a_QuaNgayDuKienIT`, `HoSoKetThucDocApiIT` | `ResidencyEndingIT`, `EndingConditionsIT`, `EndingExceptionIT`, `FR_066_FinalPeriodIT`, `FR_066a_PastExpectedDateIT`, `ResidencyEndingReadApiIT` |
| `GhiNhanQuaDoiIT`, `DanhSachViecSauQuaDoiIT`, `BR_M02_21_NguyenNhanTuVongIT` | `RecordDeathIT`, `PostDeathTaskListIT`, `BR_M02_21_CauseOfDeathIT` |
| `DichVuIT`, `DBR_08_PhienBanDonGiaIT`, `HopDongLechGiaIT` | `ServiceCatalogIT`, `DBR_08_PriceVersionIT`, `MismatchedPriceContractIT` |
| `BR_M02_19_ChayBuIT`, `NgayLuuTruApiIT`, `HopDongApiSpiIT`, `NguonLuuTruChoPhanBoIT`, `BeanThamGiaChuyenTrangThaiM02IT` | `BR_M02_19_CatchUpRunIT`, `ResidencyDayApiIT`, `ContractApiSpiIT`, (tên do C chốt, mục 9), `M02StatusParticipantIT` |
| `HopDongApiTest` (so hợp đồng), `NhomGhiNhanCoTriggerIT` (mở rộng cho nhóm 3 của 004) | đăng ký api-004 vào `ApiContractIT`; mở rộng test nhóm 3 của 000 |

### 10.8. Bổ sung khi cài Phase 2 (T003 → T012, 2026-10-25)

*Tên kiểu trong chữ ký `admission.api` mà 10.5 chưa đặt, và chỗ cài khác 10.5, 10.6. Mọi chữ ký view là **provisional** tới khi module dùng xác nhận.*

| Tên logic | Mã |
| --- | --- |
| `NoiDungHopDongView`, `HopDongView`, `KhoanDatCocView`, `LichDenView` | `ContractContentView` (`contractId`, `version`, `effectiveFrom`, `addendumId`, `content`, `recordedUnitPrices`), `ContractView` (kèm `ContractStatus`), `DepositAmountView`, `ArrivalScheduleView` |
| `DonGiaView`, `DichVuView` | `UnitPriceView` (`serviceId`, `priceVersionId`, `unitPrice`, `effectiveFrom`, `effectiveTo`), `ServiceView` (`id`, `type`, `name`, `unit`, `billingPeriod`) |
| `NgayLuuTruView`, `BuoiBanTruView` | `ResidencyDayView` (`presence`, `absenceId`, `sequenceNo`, `coefficientPercent`, cờ `pendingDecision`, `noPolicyLine`, `notYetAdmitted`, `contractOverdue`, `outsideBillingPeriod`), `DayCareSessionView` |
| `DanhSachDieuKienView`, `TienTrinhView` | `ConditionListView` (`conditions`, `warnings`), `ConditionView` (`code`, `name`, `status` `ConditionStatus`, `basis`, `module`, `exceptionRequestId`), `ProgressStepView` |
| `LoaiLuuTruTaiNgayView`, `HoSoChoView`, `DanhSachViecView` | `ResidenceTypeAtDateView`, `WaitingEntryView`, `PostDeathTaskView` (`items` `Item`, `closedAt`) |
| `PhamViLoc`, `MaMucChamSoc` (trong chữ ký) | `ScopeFilter` (`m15.account.api.dto`), `CareLevel` (`m01.api.dto`) |
| Sự kiện gửi thông báo (thêm trường so với 10.5) | Mọi sự kiện gửi thông báo thêm `at` (`Instant`) và `residentId` khi có: `BedAvailableForWaitingList(bedId, matchingEntryCount, at)`, `ContractCareLevelMismatch(…, at)`, `BedHoldDecisionNeeded(absenceId, residentId, days, policyLine, at)`, `BedHoldReviewDue(absenceId, residentId, at)`, `NoAbsencePolicyLine(absenceId, residentId, date, at)`, `ContractOverdueEscalated(contractId, residentId, days, at)`, `DepositNotSatisfiedAfterAdmission(contractId, residentId, at)`, `RepresentativeRequestNeedsCompletion(requestId, residentId, at)`, `M02Reminder(type, subjectId, on, at)` |
| `trg_phien_ban_don_gia_khong_chong` | `trg_price_version_no_overlap_insert`, `trg_price_version_no_overlap_update` (MySQL cần một trigger cho mỗi sự kiện); `trg_price_version_immutable` như 10.6 |
| `lan_chay_theo_ngay.ket_qua` | `daily_run.result` chỉ `SUCCEEDED`, `SKIPPED`; việc lỗi không ghi dòng nên lần sau làm lại |
| `ViecHangNgayM02` (bộ chạy bù) | `M02DailyRunner` (`internal.transition.service`, hàm `catchUp`), `M02DailyTask` (mỗi việc: `name`, `latestDayOnly`, `run(day)`), job `M02DailyJob` (`scheduling`); `runJobs` thêm `M02_DAILY` |
| `ThamGiaM02` | `M02StatusParticipant` (`@Order(1)`) |
| SPI M02 cài cho module thấp hơn | `ResidencyEndLookup` (`ResidencyEndApi`), `AbsenceLookup` (`AbsenceApi`), `ContractLookup` (`ContractApi`), `CareLevelChangeRequestHandler` (`CareLevelChangeRequestApi`), cả bốn ở `internal.transition.service` |
| `DonGiaService` | `UnitPriceService` (`internal.servicecatalog.service`) |
| `PhanLoaiTruongProvider` | `M02FieldClassificationConfig` (`internal.transition.config`): bốn provider `M02.CONTRACT`, `M02.ADDENDUM` (tên tạm), `M02.RESIDENCY_CHANGE`, `M02.DEATH_RECORD` |

### 10.9. Bổ sung khi cài Phase 3 (US1, T013 → T018, 2026-10-25)

*Tên cài thực tế của đăng ký tiếp nhận; mã lỗi, cảnh báo, mã dòng điều kiện mà 10.3, 10.4 chưa đặt. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| lỗi `NCT_KHONG_DANG_TIEP_NHAN`, `LUOT_DA_DONG`, `NGAY_DANG_KY_BI_KHOA` | `RESIDENT_NOT_ADMISSION_IN_PROGRESS` (409), `APPLICATION_CLOSED` (409), `APPLICATION_DATE_LOCKED` (409, BR-M02-15) |
| cảnh báo lệch mức, lệch loại lưu trú, chưa vào ở (FR-005, FR-076) | `CONTRACT_CARE_LEVEL_MISMATCH`, `RESIDENCE_TYPE_MISMATCH`, `NOT_YET_ADMITTED_FEE_CHARGED` |
| mã dòng điều kiện khi Đạt: (a), (d), (f) (khi Chưa đạt dòng mang mã điều kiện của module sở hữu: `INITIAL_ASSESSMENT_NOT_CONFIRMED`, `INITIAL_ASSESSMENT_EXPIRED`, mã của 003, `DBR_02`) | `ADM_A_ASSESSMENT`, `ADM_D_BED`, `ADM_F_CONTACTS`; dòng (b), (c), (e) giữ `ADM_B_CONTRACT`, `ADM_C_DEPOSIT`, `ADM_E_CONTRACT_LEVEL` |
| nhật ký lượt đăng ký | `M02_APPLICATION_CREATED`, `M02_APPLICATION_UPDATED`, `M02_APPLICATION_OPINION`, `M02_APPLICATION_ADMITTED`, `M02_APPLICATION_CANCELLED` (loại đối tượng `M02.ADMISSION_APPLICATION`) |
| lớp trong `admission.internal.application` | `domain`: `AdmissionApplication`, `AdmissionOpinion`, `ApplicationStatus`, `OpinionChoice`, `OpinionStatus`; `persistence`: `AdmissionApplicationStore`; `service`: `AdmissionApplicationService`, `AdmissionOpinionService`, `AdmissionConditionService` (cài `AdmissionConditionApi`), `AdmissionContractConditions` ((b), (c), (e) dùng chung bean và danh sách), `AdmissionTransition` (tác động Hoàn tất, Hủy tiếp nhận), `AdmissionOpinionWarnings`, `NotYetAdmittedReminder`, `AdmissionApplicationScopeAssignment`, `AdmissionApplicationRetention`, `M02ActorNames` |
| việc nhắc `NHAC` của `ViecHangNgayM02` | `M02ReminderTask` (việc `REMINDERS`, `latestDayOnly`) chạy các bean `ReminderRule`; ghi qua `ReminderLog` (`m02_reminder`); loại nhắc `NOT_YET_ADMITTED` (`CHUA_VAO_O`) |
| bản giả hợp đồng đọc cho tới Phase 5 | `support/fake/m02/FakeContractReads` (`ContractReadApi`); đã xóa ở Phase 5, test dùng `ContractFixtures` (ghi bảng `contract` thật) |

### 10.10. Bổ sung khi cài Phase 4 (US2, T020, T022 → T024, 2026-10-25)

*Tên cài thực tế của danh sách chờ; mã lỗi, nhật ký, giá trị enum mà 10.2 → 10.6 chưa đặt. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| lượt liên hệ giường `ket_qua` `LIEN_HE`, `TU_CHOI_GIUONG`, `HET_HAN` | `bed_contact.result`: `CONTACTED`, `BED_DECLINED`, `EXPIRED` |
| nguyên nhân dòng lịch sử điểm `TAO`, `TINH_HANG_NGAY`, `DOI_MUC`, `DOI_TINH_HUONG`, `DIEU_CHINH`, `DOI_BANG_DIEM` | `score_history.cause`: `CREATED`, `DAILY`, `CARE_LEVEL_CHANGED`, `CIRCUMSTANCES_CHANGED`, `ADJUSTMENT`, `SCORE_TABLE_CHANGED` |
| thành phần điểm (`score_breakdown`, `components`) | JSON `{careLevel, waitingTime, circumstances, adjustment}`; `current_score` là tổng; `adjustment` là tổng điều chỉnh đã duyệt |
| bảng điểm CFG-M02-08 (giá trị kiểu TABLE) | `{waitingPeriod {value, unit DAYS}, pointsPerWaitingPeriod, careLevelPoints {BASIC, REGULAR, SPECIAL → điểm} hoặc null, specialSituationPoints {mã tình huống → điểm} hoặc null}`; `null` là viện chưa đặt, tính 0 điểm; mã tình huống chọn được là khóa của `specialSituationPoints` |
| loại yêu cầu `M02.DIEU_CHINH_DIEM` | `M02.SCORE_ADJUSTMENT`, nội dung `{entryId, points}`; Hành chính lập (khóa `WAITING_LIST`, `UPDATE`), Quản lý viện duyệt (`APPROVE`); gửi duyệt ngay khi lập |
| loại đối tượng nhật ký, lưu giữ, gắn phạm vi | `M02.WAITING_LIST_ENTRY` (cũng là loại kết quả áp dụng của yêu cầu) |
| nhật ký hồ sơ chờ | `M02_WAITING_ENTRY_CREATED`, `M02_WAITING_ENTRY_CIRCUMSTANCES_UPDATED` (có lý do), `M02_WAITING_ENTRY_CONFIRMED`, `M02_WAITING_ENTRY_DECLINED`, `M02_WAITING_ENTRY_CANCELLED`; điểm đổi ghi ở `score_history`, không ghi nhật ký |
| lỗi `LUOT_KHONG_DANG_XU_LY`, `HO_SO_CHO_DA_DONG`, `HO_SO_CHO_KHONG_DANG_CHO`, `HO_SO_CHO_DA_TON_TAI` | `APPLICATION_NOT_IN_PROGRESS` (409), `WAITING_ENTRY_CLOSED` (409), `WAITING_ENTRY_NOT_WAITING` (409), `WAITING_ENTRY_ALREADY_EXISTS` (409, bảo đảm thêm bởi cột sinh `UNIQUE`) |
| tình huống chờ xác nhận | `waiting_list_circumstance.status` `PENDING_CONFIRMATION`; chỉ tình huống `CONFIRMED` được tính điểm |
| tình huống "từng lưu trú tại viện" (FR-007) | mã `PREVIOUSLY_RESIDED` (`WaitingListService.PREVIOUSLY_RESIDED`), đánh dấu sẵn `PENDING_CONFIRMATION` khi `ResidentApi.previousProfileId` có và hồ sơ trước `RESIDENCY_ENDED`; điểm của mã này do viện đặt trong `specialSituationPoints` |
| hàm đọc hồ sơ trước ("Đổi api" 001) | `ResidentApi.previousProfileId(residentId): Optional<Long>` |
| mã tham số ngoài bảng điểm | CFG-M02-03 đọc bằng `asDuration()` (ngày) |
| lớp trong `admission.internal.waitinglist` | `domain`: `WaitingListEntry`, `WaitingListStatus`, `Circumstance`, `CircumstanceStatus`, `ScoreBreakdown`, `ScoreCause`, `ScoreHistoryRow`; `persistence`: `WaitingListStore`; `service`: `WaitingListService`, `PriorityScoreCalculator` (hàm thuần), `PriorityScoreTable` (và `Config` đọc CFG-M02-08), `ScoreRecalculation`, `CalculateScoresTask` (việc `CALCULATE_SCORES`, `latestDayOnly`), `WaitingListScoreListeners` (nghe `CareLevelChanged`, `ParameterChanged`), `ScoreAdjustmentRequestHandler`, `WaitingListTransition` (tác động lên hồ sơ chờ của lệnh Hủy tiếp nhận, do `M02StatusParticipant` gọi), `WaitingEntryUpdateReminder` (nhắc `WAITING_ENTRY_UPDATE`), `MatchingEntryService` (hồ sơ phù hợp, nhận `MatchCriteria` từ người gọi), `WaitingListEntryRetention`, `WaitingListScopeAssignment` |
| phản hồi `HoSoCho`, lịch sử điểm, tham chiếu yêu cầu | `WaitingListEntryBody`, `ScoreHistoryEntryBody`, `ApprovalRequestRefBody`; thân yêu cầu `AddToWaitingListBody`, `UpdateCircumstancesBody`, `ProposeScoreAdjustmentBody`, `ReasonBody` (ở `m02.web`) |
| test | `BR_M02_10_PriorityScoreTest`, `DailyScoreIT`, `ScoreAdjustmentIT`, `WaitingListEntryIT`, `BR_M02_01_MatchingEntriesIT`, `FR_002_DeclineCancelAdmissionIT`, `BR_M02_03_WaitingEntryReminderIT`, `WaitingListRetentionIT`, `FR_007_PreviousResidencyIT` |

### 10.11. Bổ sung khi cài Phase 5 (US3, T029 → T037, 2026-10-25)

*Tên cài thực tế của hợp đồng; mã lỗi, điều kiện, cảnh báo, nhật ký mà 10.2 → 10.6 chưa đặt. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| lỗi `NGUOI_CAO_TUOI_KHONG_DU_DIEU_KIEN_LAP_HOP_DONG`, `NGUOI_KY_KHONG_PHAI_NGUOI_DAI_DIEN`, `KHONG_CO_DIEU_KHOAN_KHAC_CHUAN` | `RESIDENT_NOT_ELIGIBLE_FOR_CONTRACT` (409), `INVALID_SIGNING_REPRESENTATIVE` (409, DBR-02), `NO_NON_STANDARD_TERMS` (409); `CONTRACT_LOCKED`, `CONTRACT_OVERLAP`, `REPRESENTATIVE_REQUIRED`, `INVALID_TRANSITION` đã có ở 10.4 |
| mã điều kiện chặn Gửi ký (`CONDITION_NOT_MET`, module `004`) | `CONTRACT_ROOM_REQUIRED`, `CONTRACT_ARRIVAL_SCHEDULE_REQUIRED`, `CONTRACT_DEPOSIT_REQUIRED`, `CONTRACT_CARE_SERVICE_REQUIRED` (đúng một dịch vụ chăm sóc, đúng mức hợp đồng, Q-297), `TERMS_APPROVAL_REQUIRED` (FR-025a); chặn Ghi nhận đã ký: `SERVICE_PRICE_MISSING` (FR-036) |
| cảnh báo không chặn khi lập, sửa, Gửi ký (FR-025) | `CONTRACT_CARE_LEVEL_MISMATCH` (đã ở 10.9), `SERVICE_PRICE_NOT_EFFECTIVE` |
| loại yêu cầu `M02.DUYET_DIEU_KHOAN_HOP_DONG`; nội dung | `M02.CONTRACT_TERMS_APPROVAL`; `{contractId, contentHash, terms[] (type, description, standardValue, proposedValue)}`; lập khóa `ADMISSION_CONTRACT_DEPOSIT, UPDATE` (Hành chính), duyệt `ADMISSION_CONTRACT_DEPOSIT, APPROVE` (Quản lý viện) |
| loại điều khoản khác chuẩn (đã ở 10.3) | `NonStandardTerm.Type`: `PRICE`, `ABSENCE_POLICY`, `BED_HOLD_POLICY` |
| loại tệp, chủ sở hữu tệp bản scan | `M02.CONTRACT_SCAN` (chủ sở hữu là hợp đồng; `ContractScanFilePermission`) |
| nhật ký hợp đồng (loại đối tượng `M02.CONTRACT`) | `M02_CONTRACT_CREATED`, `M02_CONTRACT_UPDATED`, `M02_CONTRACT_TERMS_SUBMITTED` (có lý do), `M02_CONTRACT_TERMS_APPROVED`, `M02_CONTRACT_SENT`, `M02_CONTRACT_RETURNED` (có lý do), `M02_CONTRACT_SIGNED`, `M02_CONTRACT_CANCELLED` (có lý do ở lệnh Hủy), `M02_CONTRACT_TERMINATED` (Hủy tiếp nhận sau khi Hiệu lực) |
| việc hằng ngày `HOP_DONG` | `CONTRACTS` (`ContractOverdueTask`, `@Order(30)`, không `latestDayOnly`); nhắc `ContractReminders` (`CONTRACT_EXPIRING`, `SHORT_TERM_CONTRACT`, `CONTRACT_OVERDUE`) |
| bảng | `contract` (cột `current_end_date`, `content_hash`, `awaiting_terms_approval`, `terms_approval_request_id`, `overdue_since`, `manager_notified_overdue_at`), `contract_service`, `contract_absence_policy`, `contract_arrival_schedule`, `contract_content_version` (mã hợp đồng trong cột `contract_id`); trigger thêm `trg_contract_no_delete`, ba trigger khóa mỗi bảng con (`…_lock_insert`, `…_lock_update`, `…_lock_delete`), `trg_contract_content_version_no_update`, `trg_contract_content_version_no_delete` |
| lớp trong `admission.internal.contract` | `domain`: `Contract`, `ContractContent` (`ServiceLine`, `AbsencePolicyLine`, `ArrivalLine`), `ContractVersion`, `NonStandardTerm`; `persistence`: `ContractStore`; `service`: `ContractService`, `ContractRules`, `ContractSnapshot` (JSON chuẩn hóa và băm SHA-256), `NonStandardTermDetector`, `ContractTermsRequestHandler`, `ContractTermsListeners`, `ContractReadService` (cài `ContractReadApi` và `ContractApi` của 001; thay `ContractLookup` khung), `ContractTransition` (Hủy tiếp nhận kéo theo hợp đồng), `ContractOverdueTask`, `ContractReminders`, `ContractScopeAssignment`, `ContractScanFilePermission` |
| phản hồi, thân yêu cầu | `ContractBody`, `ContractResultBody`, `ContractContentBody`, `NonStandardTermBody`, `AddendumBody`; `ContractInputBody` (kèm `FeeBillingPeriod`, `HoldBed`, `ServiceLineBody`, `AbsencePolicyLineBody`, `ArrivalLineBody`), `RecordSignedBody` |
| hợp đồng lệch glossary | `AddendumBody.Status` theo hợp đồng api-004 là `PENDING_EFFECTIVE`, `APPLIED`, `NOT_APPLICABLE`; 10.3 ghi `NOT_APPLIED` (lệch, chờ chủ dự án chọn khi làm Phase 7) |
| test | `ContractLifecycleIT`, `ContractContentLockDbIT`, `DBR_06_ContractNoOverlapIT`, `FR_021_RecordSignedIT`, `NonStandardTermDetectorTest`, `TermsApprovalIT`, `BR_M02_09_ContentAtDateIT`, `BR_M02_05_ContractReminderIT`, `FR_029_ContractOverdueIT`; nền `ContractTestBase`, `ContractFixtures` (thay `FakeContractReads` đã xóa) |

### 10.12. Bổ sung khi cài Phase 6 (US4, T039 → T041, 2026-10-26)

*Tên cài thực tế của phần đặt cọc. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `GET /hop-dong/{id}/dat-coc`; thân phản hồi | `GET /contracts/{id}/deposit`; `DepositBody` (`notRequired`, `requiredAmount`, `status`, `basis`), `DepositBody.Basis` (`at`, `transactionId`, `accountant` là họ tên Kế toán, `receiptNumber` luôn rỗng vì Q-385 bỏ số phiếu thu chi) |
| lớp trong `admission.internal.deposit.service` | `DepositViewService` (khoản cần đặt cọc từ hợp đồng, trạng thái từ `DepositStatusPort`; chưa có 017 thì Chưa đáp ứng), `DepositRecheckService.recheck(contractId)` (FR-032, listener của 017 sẽ gọi) |
| sự kiện khoản cọc | `DepositAmountChanged(contractId, newAmount)` phát khi hợp đồng vào Chờ ký (kể cả gửi ký lại sau Trả về nháp); `newAmount` bằng 0 khi không yêu cầu cọc; phụ lục đổi khoản cọc phát ở Phase 7 |
| test | `DepositConditionIT`, `FR_032_DepositAfterResidencyIT`, `Events017018IT` |

### 10.13. Bổ sung khi cài Phase 7 (US5, T042 → T047, T075, 2026-10-26)

*Tên cài thực tế của phần thay đổi lưu trú, phụ lục. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `V004_06__phu_luc.sql` (đổi tên theo 10.6: `V004_06__addendum.sql`); `phu_luc_hop_dong`, `yeu_cau_thay_doi_thuoc_tinh` | `V004_06__addendum.sql`; `contract_addendum` (`contract_id`, `approval_request_id`, `change_types`, `content_before`, `content_after`, `effective_date`, `status`, `content_version_id`, `created_at`), `change_request_attribute` (`approval_request_id`, `contract_id`, `resident_id`, `attribute`, `pending_effective`, `lock_key`) |
| trạng thái phụ lục (cột) | `PENDING_EFFECTIVE`, `APPLIED`, `NOT_APPLIED` (cột, theo 10.3); thân phản hồi `AddendumBody.Status` giữ `NOT_APPLICABLE` theo api-004 (chờ chủ dự án chọn, BL-66) |
| `THAY_DOI_LUU_TRU` (loại yêu cầu), `ThayDoiLuuTruHandler`, `LaThayDoiNho` | `M02.RESIDENCY_CHANGE`, `ResidencyChangeHandler`, `IsMinorChange` (`admission.internal.change.service`) |
| trạng thái mở rộng `DUOC_THAY_THE`, chuyển mở rộng | `SUPERSEDED`; mã chuyển `SUPERSEDE` (từ `DRAFT`, `PENDING_APPROVAL`, `APPROVED`) |
| loại thay đổi `LOAI_LUU_TRU`, `MUC_CHAM_SOC`, `DICH_VU`, `THOI_GIAN`, `GIUONG`, `LICH_DEN` | `RESIDENCE_TYPE`, `CARE_LEVEL`, `SERVICE`, `DURATION`, `BED`, `ARRIVAL_SCHEDULE` (`ChangeType`) |
| `newValue.services[]` | dòng thay đổi `{serviceId, action: ADD \| REMOVE, inPackage, quantity, customUnitPrice}` (không phải danh sách đích); khoản cọc đi kèm `newValue.depositAmount`, `newValue.noDepositRequired` |
| lớp trong `admission.internal.change` | `domain`: `ChangeType`, `Addendum`, `ResidencyChangeContent`; `persistence`: `AddendumStore`; `service`: `ResidencyChangeHandler`, `ResidencyChangeRules` (điều kiện FR-041, FR-045, FR-046; giá trị trước; ảnh hưởng chi phí), `AddendumContent` (áp thay đổi lên nội dung phiên bản, diff trước/sau), `IsMinorChange`, `ResidencyChangeService`, `ResidencyChangeListeners`, `CareLevelChangeRequestHandler` (cài `CareLevelChangeRequestApi`, thay khung ở `transition`) |
| web | `ResidencyChangeRequestController` (`POST /residency-change-requests`, `GET /residency-change-requests/{id}`), `ResidencyChangeRequestInputBody` (`Reference`), `ResidencyChangeRequestBody`, `AddendumBody.of`; `GET /contracts/{id}/addenda` đọc dữ liệu thật |
| mã điều kiện chưa thỏa (409 `CONDITION_NOT_MET`) | `CONTRACT_NOT_ACTIVE`, `RESIDENT_IN_FINAL_STATUS`, `EFFECTIVE_DATE_BEFORE_CONTRACT_START`, `DUPLICATE_PENDING_ATTRIBUTE`, `RESIDENCE_TYPE_UNCHANGED`, `END_DATE_REQUIRED`, `ARRIVAL_SCHEDULE_REQUIRED`, `BED_REQUIRED`, `CARE_LEVEL_UNCHANGED`, `CARE_SERVICE_MISSING`, `SERVICE_NOT_FOUND`, `SERVICE_NOT_EFFECTIVE`, `SERVICE_PRICE_MISSING`, `SERVICE_NOT_IN_CONTRACT`, `SERVICE_ALREADY_IN_CONTRACT`, `CARE_SERVICE_NOT_ALLOWED`, `END_DATE_BEFORE_EFFECTIVE`, `CONTRACT_OVERLAP`, `NOT_INPATIENT`, `NOT_DAY_CARE`, `ADDENDUM_NOT_PENDING`, `BED_ALLOCATION_CHECK_UNAVAILABLE`, `DAY_CARE_CAPACITY_CHECK_UNAVAILABLE` |
| nhật ký | `M02_ADDENDUM_CREATED` (khi duyệt), `M02_ADDENDUM_APPLIED`, `M02_ADDENDUM_NOT_APPLIED`; loại đối tượng `M02.ADDENDUM` |
| test | `IsMinorChangeTest`, `BR_M02_04_ApplyAddendumIT`, `DBR_07_AddendumIT`, `FR_041_CheckOnApprovalIT`, `FR_046_DuplicateAttributeIT`, `ApplyFailedIT`, `RepresentativeRequestIT`, `ChangeArrivalScheduleIT`, `CareLevelChangeRequestIT`, `FR_043_MinorChangeIT` |

### 10.14. Bổ sung khi cài Phase 8 (US6, T048 → T055, 2026-10-26)

*Tên cài thực tế của phần lượt vắng, ngày vắng, quyết định giữ giường, báo vắng và ngày nghỉ bán trú. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `V004_07__vang_mat.sql` (đổi tên theo 10.6: `V004_07__absence.sql`); `luot_vang`, `lich_su_du_kien_tro_lai`, `quyet_dinh_giu_giuong`, `ngay_vang`, `bao_vang_ban_tru`, `ngay_nghi_ban_tru` | `V004_07__absence.sql`; `absence` (`resident_id`, `absence_type`, `reason`, `standard_reason`, `departed_at`, `expected_return_at`, `returned_at`, `picked_up_by_id`, `pickup_record_id`, `transfer_unit`, `handover_staff_id`, `takes_medication`, `initial_bed_hold`, `threshold_notified_at`, `review_date`, `previous_absence_id`, `status`, `open_resident_id`, `created_by`, `created_at`, `closed_by`), `expected_return_history` (`absence_id`, `old_value`, `new_value`, `reason`, `actor_id`, `at`), `bed_hold_decision` (`absence_id`, `threshold_date`, `choice`, `coefficient`, `review_date`, `applies_from`, `applies_to`, `reason`, `decided_by`, `decided_at`), `absence_day`, `day_care_absence_notice` (`resident_id`, `date`, `session_id`, `family_notified_at`, `within_notice_period`, `recorded_by`, `recorded_at`), `day_care_closure_day` (`date`, `reason`, `status`, `active_date`, `late_declaration`, `declared_by`, `declared_at`, `cancelled_by`, `cancel_reason`, `cancelled_at`) |
| trigger | `trg_absence_immutable_when_closed` (đã có ở 10.6), `trg_absence_no_delete`, `trg_expected_return_history_immutable`, `trg_bed_hold_decision_immutable`, `trg_day_care_absence_notice_immutable` và các `*_no_delete` (chỉ tài khoản `eldercare_retention` xóa) |
| `TraChinhSachVang` (hàm thuần) | `AbsencePolicyResolver` (`resolve`, `dayCare`, `noLine`; `Outcome`, `Hold` `YES`/`NO`/`MANAGER_DECIDES`, `Source` `CONTRACT`/`FACILITY`/`DEFAULT`) ở `admission.internal.absence.service` |
| lớp trong `admission.internal.absence` | `domain`: `AbsenceType`, `Absence`, `AbsenceDay`, `BedHoldChoice`, `BedHoldDecision`; `persistence`: `AbsenceStore`, `DayCareStore`; `service`: `AbsenceService` (đọc, gia hạn, quyết định giữ giường, tính lại sau đính chính), `AbsenceDayCalculator` (ghi `absence_day`), `AbsenceTransition` (tác động của `GRANT_TEMPORARY_ABSENCE`, `TRANSFER_OUT`, `RECORD_RETURN`), `AbsenceDayTask` (việc `ABSENCE_DAYS` của `M02_DAILY`), `AbsenceCorrectionProvider` (`M02.ABSENCE`), `BedHoldDecisionCorrectionProvider` (`M02.BED_HOLD_DECISION`), `DayCareService`, `AbsenceNames`; `AbsenceLookup` (`AbsenceApi.open`) ở `internal.transition.service` thay khung |
| việc hằng ngày | `ABSENCE_DAYS` (`@Order(20)`, bảng `daily_run`): ghi nốt ngày vắng tới ngày chạy, báo ngưỡng một lần (`threshold_notified_at`), báo ngày xem xét lại |
| web | `AbsenceController` (`GET /absences`, `GET /absences/{id}`, `POST …/extend-expected-return`, `…/bed-hold-decision`, `…/correct`), `DayCareController` (`POST /day-care-absence-notices`, `GET`, `POST /day-care-closure-days`, `POST …/{id}/cancel`); thân `ExtendExpectedReturnBody`, `BedHoldDecisionBody`, `DayCareAbsenceNoticeBody`, `DayCareClosureDayBody`, `CorrectionRequestBody`; phản hồi `AbsenceBody`, `AbsenceDetailBody`, `DayCareClosureDayView` |
| mã điều kiện, lỗi | `ABS_PICKUP_RECORD` (điều kiện chưa thỏa của Cho tạm vắng, 409 `UNMET_CONDITIONS`), `ABSENCE_ALREADY_OPEN`, `ABSENCE_CLOSED`, `ABSENCE_STILL_OPEN`, `BED_NOT_HELD`, `THRESHOLD_NOT_REACHED`, `NO_PENDING_DAYS`, `NO_ARRIVAL_ON_DATE`, `CLOSURE_ALREADY_CANCELLED` (cộng `DAY_ALREADY_DECLARED`, `PICKUP_RECORD_ALREADY_USED` ở 10.4); cảnh báo `BED_REALLOCATION_NEEDED` |
| nhật ký | `M02_ABSENCE_OPENED`, `M02_ABSENCE_CLOSED`, `M02_ABSENCE_EXTENDED`, `M02_ABSENCE_DECISION`, `M02_ABSENCE_RECALCULATED`, `M02_ABSENCE_NOTICE_RECORDED`, `M02_ABSENCE_CLOSURE_DECLARED`, `M02_ABSENCE_CLOSURE_CANCELLED`; loại đối tượng `M02.ABSENCE`, `M02.DAY_CARE_ABSENCE_NOTICE`, `M02.DAY_CARE_CLOSURE_DAY` |
| nguyên nhân `ResidencyDaysChanged.cause` | `BED_HOLD_DECISION`, `BED_HOLD_DECISION_CORRECTION`, `ABSENCE_CORRECTION` |
| nội dung đính chính | `M02.ABSENCE`: `departedAt`, `returnedAt` (ISO-8601); `M02.BED_HOLD_DECISION`: `coefficient`, `reviewDate` |
| test | `BR_M02_06_AbsencePolicyLookupTest`, `BR_M02_17_ManagerDecidesDay1Test`, `AbsenceDayIT`, `BR_M02_07_BedHoldDecisionIT`, `BR_M02_18_CorrectionOverThresholdIT`, `FR_057_TransferOutDuringAbsenceIT`, `DayCareCoefficientIT`, `DayCareClosureDayIT`; nền `AbsenceTestBase` |

### 10.15. Bổ sung khi cài Phase 9 (US7, T057 → T061, T106 → T108, 2026-10-26)

*Tên cài thực tế của hồ sơ kết thúc lưu trú, bàn giao, ngoại lệ điều kiện và đọc cho 017. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `V004_08__ket_thuc.sql` (đổi tên theo 10.6: `V004_08__residency_ending.sql`); `ho_so_ket_thuc`, `ban_giao_nct`, `ngoai_le_dieu_kien` | `V004_08__residency_ending.sql`; `residency_ending` (`resident_id`, `end_case`, `medical_reason`, `destination`, `expected_end_date`, `actual_ended_at`, `requested_by`, `reason`, `overdue_since`, `final_period_ref`, `status`, `cancel_reason`, `open_resident_id`, `version`, `created_by`, `created_at`, `closed_by`, `closed_at`), `resident_handover` (`ending_id`, `recipient_type`, `family_member_id`, `facility_name`, `handover_at`, `handover_staff_id`, `recorded_by`, `recorded_at`), `ending_condition_exception` (`ending_id` rỗng được, `resident_id`, `condition_code`, `approval_request_id`, `created_at`) |
| trigger | `trg_residency_ending_immutable_when_closed`, `trg_residency_ending_no_delete`, `trg_resident_handover_immutable`, `trg_resident_handover_no_delete`, `trg_ending_condition_exception_immutable`, `trg_ending_condition_exception_no_delete` |
| `ThamGiaM02` phần `KET_THUC_LUU_TRU` | `EndingTransition` (`unmet`, `waiver`, `completed`) do `M02StatusParticipant` gọi cho `END_RESIDENCY`; hợp đồng: `ContractTransition.residencyEnded`; lượt vắng: `AbsenceTransition.closedByFinalState`; yêu cầu thay đổi lưu trú: `ResidencyChangeClosing.closeAll` (`closeForFinalState` của 000 rồi `AddendumClosing`) |
| lớp trong `admission.internal.ending` | `domain`: `ResidencyEnding`, `EndingStatus`, `Handover`, `RecipientType`, `EndingExceptionRecord`, `EndingConditionCode`; `persistence`: `ResidencyEndingStore`; `service`: `ResidencyEndingService` (lập, đổi ngày, hủy, bàn giao, lập yêu cầu ngoại lệ, xem), `EndingConditionService` (danh sách (a) → (h), cài `AdmissionConditionApi.endingConditions` qua `AdmissionConditionService`), `EndingTransition`, `EndingHandovers` (bàn giao hiện hành sau đính chính), `HandoverCorrectionProvider` (`M02.RESIDENT_HANDOVER`), `EndingExceptionHandler` (loại yêu cầu `M02.ENDING_EXCEPTION`), `EndingFootnoteCondition` (chú thích 39 của Phụ lục 27), `EndingOverdueTask` (việc `ENDINGS`), `EndingReminders` (`PAST_EXPECTED_DATE`), `ResidencyEndingReadApiImpl` |
| web | `ResidencyEndingController` (`POST /residency-endings`, `GET …/{id}`, `GET …/{id}/conditions`, `POST …/change-date`, `…/cancel`, `…/handover`, `…/request-exception`); thân `ResidencyEndingInputBody` (`EndCaseInput`), `ChangeExpectedEndDateBody`, `ResidentHandoverBody`, `RequestEndingExceptionBody`; phản hồi `ResidencyEndingBody` (`HandoverBody`), `ConditionListBody.ofEnding` (thêm `exception`: `ExceptionRefBody`) |
| `admission.api` mới (T108) | `ResidencyEndingReadApi` (`HoSoKetThucDocApi`: `preparing`, `settlementInfo`, `hasApprovedSettlementException`); `dto`: `EndCase` (thêm `DEATH` chỉ cho `SettlementInfoView`), `ResidencyEndingView`, `SettlementInfoView` |
| mã điều kiện kết thúc | `END_A_COSTS`, `END_B_BELONGINGS`, `END_C_STORED_MEDICATION`, `END_D_ALERTS_INCIDENTS`, `END_E_HANDOVER`, `END_H_SETTLEMENT`, `END_H_SETTLEMENT_CONFIRMATION` (10.3); dòng hiển thị cố định `END_F_CONTRACT`, `END_G_BED` (trạng thái `AUTOMATIC`, mã 004 đặt vì research T-09 không đặt mã cho (f), (g)); dòng (h) hiện mã `END_H_SETTLEMENT` |
| mã điều kiện chưa thỏa của 004 (409 `CONDITION_NOT_MET`) | `END_NO_PREPARING_ENDING`, `END_PAST_EXPECTED_DATE`, `END_EFFECTIVE_DATE_OUT_OF_RANGE`, `END_E_HANDOVER` (cùng `FINAL_PERIOD_UNAVAILABLE` khi lập hồ sơ mà chưa có bản cài `KyCuoiPort` của 010) |
| mã lỗi | `RESIDENT_NOT_IN_RESIDENCE` (409), `RESIDENCY_ENDING_ALREADY_OPEN` (409), `RESIDENCY_ENDING_NOT_PREPARING` (409) |
| nhật ký | `M02_ENDING_CREATED`, `M02_ENDING_DATE_CHANGED`, `M02_ENDING_CANCELLED`, `M02_ENDING_HANDOVER_RECORDED`, `M02_ENDING_COMPLETED`, `M02_ENDING_OVERDUE_MARKED`, `M02_ENDING_EXCEPTION_APPROVED`, `M02_CONTRACT_ENDED`; loại đối tượng `M02.RESIDENCY_ENDING` |
| đối tượng kiểm quyền | `M02.RESIDENCY_ENDING` (hồ sơ kết thúc), `M02.ENDING_EXCEPTION` (yêu cầu ngoại lệ), `M02.ENDING_EXCEPTION.SETTLEMENT` (yêu cầu ngoại lệ ý tiền (h): chú thích 39 chỉ thỏa với hai loại đối tượng của hồ sơ kết thúc và loại này) |
| loại yêu cầu, bản ghi nhóm 3 | `M02.NGOAI_LE_KET_THUC` → `M02.ENDING_EXCEPTION` (nội dung `{residencyEndingId, conditionCode}`); `M02.BAN_GIAO_NCT` → `M02.RESIDENT_HANDOVER` (đính chính `handoverAt`, `handoverStaffId`, `familyMemberId`, `facilityName`) |
| nguyên nhân `ResidencyDaysChanged.cause` | `RESIDENCY_ENDING_OVERDUE` |
| việc hằng ngày, nhắc | `ENDINGS` (`@Order(40)`, bảng `daily_run`); loại nhắc `PAST_EXPECTED_DATE` (mức Nhẹ) |
| test | `ResidencyEndingIT`, `EndingConditionsIT`, `EndingExceptionIT`, `FR_066_FinalPeriodIT`, `FR_066a_PastExpectedDateIT`, `ResidencyEndingReadApiIT`, `EndingConcurrencyIT`, `EndingClosesRequestsIT`; nền `EndingTestBase` |


### 10.16. Bổ sung khi cài Phase 10 (US8, T062 → T065, T106, T109, 2026-10-26)

*Tên cài thực tế của bản ghi qua đời và danh sách việc sau qua đời. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `V004_09__qua_doi.sql`; `ban_ghi_qua_doi`, `muc_viec_sau_qua_doi` | `V004_09__death_record.sql`; `death_record` (`resident_id` `UNIQUE`, `death_at`, `place`, `discovered_by`, `confirmed_by_id`, `cause_of_death`, `evidence_file_id`, `history_id`, `recorded_by`, `recorded_at`), `post_death_task` (`death_record_id`, `item_code`, `status`, `basis`, `updated_at`) |
| trigger | `trg_death_record_immutable`, `trg_death_record_no_delete`, `trg_post_death_task_no_delete` |
| `ThamGiaM02` phần `GHI_NHAN_QUA_DOI` | `DeathTransition.recorded` do `M02StatusParticipant` gọi cho `RECORD_DEATH`; hợp đồng `ContractTransition.residentDied` |
| lớp trong `admission.internal.death` | `domain`: `DeathRecord`, `PostDeathTask`; `persistence`: `DeathStore`; `service`: `DeathTransition`, `PostDeathTaskService` (`PostDeathTaskApi`: `refresh`, `view`; thêm `list`, `recalculate`, `requestSettlementException`, `settlementExceptionAllowed`, `recordIdOf`), `DeathRecordService`, `DeathRecordCorrectionProvider`, `DeathEvidenceFilePermission`, `PostDeathReminders` |
| web | `DeathController` (`GET /residents/{residentId}/death-record`, `POST /death-records/{id}/correct`, `GET /residents/{residentId}/post-death-tasks`, `POST …/request-settlement-exception`); `DeathRecordBody`, `PostDeathTaskListBody` |
| loại đối tượng, tệp | `M02.DEATH_RECORD` (kiểm quyền, nhật ký, đính chính, phân loại trường `cause` = `HEALTH`); chủ sở hữu tệp `M02.DEATH_EVIDENCE` |
| nội dung yêu cầu ngoại lệ sau qua đời | `M02.ENDING_EXCEPTION` với `{residentId, basis = POST_DEATH, conditionCode = END_H_SETTLEMENT}`; `ResidencyEndingService.POST_DEATH_BASIS`, `EXCEPTION_TYPE`, `settlementExceptionTarget` |
| mã lỗi | `POST_DEATH_LIST_CLOSED` (409), `POST_DEATH_SETTLEMENT_NOT_OPEN` (409), `EVIDENCE_REQUIRED` (422, của 001) |
| nhật ký | `M02_DEATH_RECORDED`, `M02_POST_DEATH_ITEM_CHANGED`, `M02_POST_DEATH_CLOSED`, `M02_ENDING_CANCELLED_BY_DEATH`, `M02_ENDING_EXCEPTION_APPROVED` (dùng chung) |
| nhắc | `POST_DEATH_TASKS` (mức Nhẹ, `subjectId` là mã bản ghi qua đời) |
| test | `RecordDeathEffectsIT` (tránh trùng `m01.RecordDeathIT`), `PostDeathTaskIT`, `BR_M02_21_CauseOfDeathIT`, `PostDeathSettlementExceptionIT`; nền `DeathTestBase` |
### 10.17. Bổ sung khi cài Phase 11 (US9, T066 → T068 phần backend, 2026-10-27)

*Tên cài thực tế của lệnh dịch vụ và phiên bản đơn giá. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `DichVuService` (T067) | `ServiceCatalogService` (`internal.servicecatalog.service`; `list`, `create`, `update`, `deactivate`, `priceVersions`, `createPriceVersion`, `mismatchedContracts`; `Input`; `target(serviceId)`) |
| kho dữ liệu, kiểu miền | `ServiceCatalogStore` (`persistence`); `ServiceRecord`, `PriceVersion`, `MismatchedContract` (`domain`) |
| web (T068) | `ServiceController` (`GET`, `POST /services`, `PUT /services/{id}`, `POST /services/{id}/deactivate`, `GET`, `POST /services/{id}/price-versions`, `GET /services/{id}/mismatched-price-contracts`); `ServiceInputBody`, `PriceVersionInputBody`; `ServiceBody` (schema `Service`), `PriceVersionBody` (schema `PriceVersion`), `MismatchedPriceContractBody` |
| loại đối tượng | `M02.SERVICE`, `M02.PRICE_VERSION` (quyền `SERVICE_PRICE`: AC-01 xem, tạo, sửa; AC-09 chỉ xem) |
| mã lỗi | `SERVICE_NAME_EXISTS`, `CARE_SERVICE_LEVEL_EXISTS` (409, Q-297), `SERVICE_INACTIVE`, `SERVICE_ALREADY_INACTIVE`, `SERVICE_FIELD_LOCKED` (409), `PRICE_VERSION_OVERLAPS_APPLIED` (409), `VERSION_MISMATCH` (412) |
| nhật ký | `M02_SERVICE_CREATED`, `M02_SERVICE_UPDATED`, `M02_SERVICE_DEACTIVATED` (bắt buộc lý do), `M02_SERVICE_PRICE_VERSION_CREATED` |
| hợp đồng | schema `Service` thêm `version` (cũng là ETag, `If-Match` của `PUT`, `deactivate`) |
| test | `ServiceCatalogIT` (DichVuIT), `DBR_08_PriceVersionIT`, `MismatchedPriceContractsIT` |


### 10.18. Bổ sung khi cài Phase 12 (T069 → T071, 2026-10-27)

*Tên cài thực tế của dữ liệu ngày lưu trú và số liệu 016. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `NgayLuuTruService` (T069) | `ResidencyDayService` (`internal.residencyday.service`; cài `ResidencyDayApi`: `daysBetween`, `dayCareSessions`, `billingStopDate`) |
| `LuuTruThongKeService` (T069) | `ResidencyStatisticsService` (cài `ResidencyStatisticsApi`: `residenceTypeAtDate`, `waitingList`) |
| buổi bán trú | `DayCareSessions` (`internal.absence.service`; `sessionId` là số thứ tự buổi trong ngày theo giờ đến) |
| cột mới (V004_10) | `admission_application.admitted_on` (ngày Hoàn tất tiếp nhận), `contract.terminated_on` (ngày hợp đồng Chấm dứt); kho `AdmissionApplicationStore.admittedOn`, `ContractStore.terminatedOn`, `ResidencyEndingStore.completedAt` |
| cột mới (V004_11) | `resident_residency.admitted_on` (ngày vào ở ghi theo người; `AdmissionApplicationStore.recordAdmittedOn`) |
| test (audit) | `AdmissionCompletedEventIT`, `DayCareSessionsIT` |
| test | `ResidencyDayApiIT` (NgayLuuTruApiIT), `M02StatusParticipantIT` (BeanThamGiaChuyenTrangThaiM02IT), `M02RecordGroupTriggerIT` (NhomGhiNhanCoTriggerIT cho 004) |

### 10.19. Sửa theo R-15, Q-394, Q-395 (2026-11-02; thay các dòng tương ứng ở trên)

| Tên logic | Tên trong mã |
| --- | --- |
| `ho_so_tiep_nhan` (gộp `luot_dang_ky`, `ho_so_cho`, `y_kien_vao_o`) | bảng `admission_record`, cột `kind` (`APPLICATION`, `WAITING_ENTRY`, `OPINION`) |
| `uu_tien`, `ly_do_uu_tien`, dấu Ưu tiên | `priority`, `priority_reason`; `POST /waiting-list-entries/{id}/mark-priority`; hành động `M02_WAITING_ENTRY_PRIORITY_MARKED` |
| `phien_ban_hop_dong` (gộp `phien_ban_noi_dung_hop_dong`, `phu_luc_hop_dong`, `hop_dong_dich_vu`, `hop_dong_lich_den`) | bảng `contract_version`, cột `kind` (`DRAFT`, `CONTENT`, `ADDENDUM`) |
| `dich_vu` | `catalog_item` loại `M02.SERVICE` (`ServiceAttributes`), cột sinh `m02_active_care_level` |
| `luot_vang` (gộp `bao_vang_ban_tru`, `ngay_nghi_ban_tru`) | bảng `absence`, cột `kind` (`ABSENCE`, `DAY_CARE_NOTICE`, `CLOSURE_DAY`) |
| `da_bao_quan_ly_luc`, `giai_phong_giuong_luc`, `giai_phong_boi_id`, `ly_do_giai_phong` | `manager_notified_at`, `bed_released_at`, `bed_released_by`, `bed_release_reason` |
| Lệnh Giải phóng giường | `POST /absences/{id}/release-bed`; `AbsenceService.releaseBed`; hành động `M02_ABSENCE_BED_RELEASED` |
| Báo Quản lý viện khi vắng dài | sự kiện `LongAbsenceManagerNotice`; việc `ABSENCE_NOTICE`; CFG-M02-05 `managerNoticeDays`, `dayCareNoticedPercent`, `dayCareUnnoticedPercent` |
| `ket_thuc_luu_tru` (gộp `ho_so_ket_thuc`, `ban_ghi_qua_doi`) | bảng `residency_ending`, cột `kind` (`ENDING`, `DEATH`) |
| `lien_he_giuong`, `ban_giao_nct`, `ngoai_le_dieu_kien` | `record_event` loại `M02.BED_CONTACT`, `M02.RESIDENT_HANDOVER`, `M02.ENDING_CONDITION_EXCEPTION` |
| `lich_su_du_kien_tro_lai` | `status_history`, track `M02.ABSENCE` / `EXPECTED_RETURN` |
| `luu_tru_nct`, `muc_viec_sau_qua_doi` | `resident_module_state` loại `M02.RESIDENCY` (`ResidencyState`) |
| `lan_chay_theo_ngay`, `nhac_m02` | `reminder_marker` loại `M02.DAILY_RUN`, `M02.REMINDER` |
| `yeu_cau_thay_doi_thuoc_tinh` | `simple_record` loại `M02.CHANGE_ATTRIBUTE` (`ChangeAttribute`; lệch data-model 0.1, lý do ở plan) |
| Đã bỏ theo Q-394, Q-395 | `score_history`, `waiting_list_circumstance`, `bed_hold_decision`, `absence_day`, `contract_absence_policy`, `absencePolicyOverrides`, `/score-history`, `/update-circumstances`, `/propose-score-adjustment`, `/bed-hold-decision` |


## 11. Feature 005 (`005-daily-care-tasks`, C)

| Tên logic | Mã |
| --- | --- |
| schema lỗi của 005 trong mã (T060, 2026-11-03) | `care.web.response.CareProblemBody` (`CareCatalogProblem`), `.TaskClosed` (`CareTaskClosedProblem`), `.RowErrors` (`CareTaskRowProblem`) |
| nhận định tự quyết trong thân lệnh thỏa thuận rủi ro; thân lệnh Duyệt kế hoạch; phản hồi việc cùng loại đang mở | `RiskAssessmentWrite` (`CareSelfDeterminationAssessment`); `CarePlanApprovalWrite` (`CarePlanApproval`); `SameTypeTasksResponse` (`CareSameTypeTasks`) |

### 11.1. Phase 1 — khung gói và SPI (2026-10-26)

Chưa dịch các endpoint/schema nghiệp vụ api-005. Riêng operation công tắc dưới đây được chủ dự án duyệt trong phiên 2026-10-26. Tên logic trong tasks/research không phải đường dẫn mã thật.

| Tên logic | Tên triển khai |
| --- | --- |
| kehoach, comat, congviec, ghinhan | `careplan`, `attendance`, `task`, `recording` |
| checklist, danhmuc, quytac, quahan | `checklist`, `catalog`, `rules`, `overdue` |
| thoikhoabieu, trangthai, lich | `timetable`, `status`, `scheduling` |
| NhanSuCaPort | `m04.api.spi.ShiftStaffPort`: `shift`, `publishedShifts`, `shiftContaining`, `shiftLead`, `assignments`, `responsibleNurses`, `onShift`, `qualified`, `hasQualifiedStaff`, `handoverConfirmed`, `handoverTasks` |
| NguonCongViecPort | `TaskSourcePort`: `managedTypes`, `validate`, `confirmServing` |
| ChiSoPort | `VitalSignPort.record` |
| LieuThuocChecklistPort | `MedicationChecklistPort.inRange` |
| CanhBaoDocPort | `AlertReadPort.open`, `byTask` |
| BanGhiDonBanTruPort | `DayCarePickupPort.valid`, `markUsed` |
| KetQuaKiemTraPort | `QualityCheckPort.failed` |
| ThoiKhoaBieuNguonPort | `TimetableSourcePort.onDate` |
| CaView, PhanCongChamSocView, ViecBanGiaoView | `ShiftStaffPort.ShiftView`, `CareAssignmentView`, `HandoverTasksView` |
| LoaiCanCu, KetQuaKiemTraCanCu | `TaskSourcePort.SourceType`, `SourceValidation` |
| KetQuaGhiChiSo: DA_LUU / BI_CHAN / CHO_DO_LAI | `VitalSignPort.RecordingResult`, `RecordingStatus`: `SAVED` / `BLOCKED` / `AWAITING_REMEASUREMENT` |
| LieuHienThiView, ThuocTomTat | `MedicationChecklistPort.DoseView`, `MedicationSummary` |
| CanhBaoView, BanGhiDonView, KetQuaKiemTraView, MucThoiKhoaBieuView | `AlertReadPort.AlertView`, `DayCarePickupPort.PickupView`, `QualityCheckPort.FailedCheckView`, `TimetableSourcePort.TimetableItemView` |
| MUC_KE_HOACH, LAN_DO, BUA, BUOI_HOAT_DONG | `CARE_PLAN_ITEM`, `MEASUREMENT`, `MEAL`, `ACTIVITY_SESSION` |
| THEO_DOI_SAU_NGA, THEO_DOI_TIEP_XUC, CANH_BAO_UONG_NUOC | `POST_FALL_MONITORING`, `CONTACT_MONITORING`, `WATER_INTAKE_ALERT` |
| KIEM_TRA_DAU_CA, GIAM_SAT, PHAT_SINH, GHI_NHAN_PHAT_SINH, LAM_LAI | `SHIFT_START_CHECK`, `SUPERVISION`, `AD_HOC`, `AD_HOC_RECORDING`, `REWORK` |
| BAN_GHI_DON_DA_DUNG | `PICKUP_RECORD_ALREADY_USED` (lỗi fake, chưa là schema lỗi HTTP nghiệp vụ) |
| Bản giả SPI 005 | `support/fake/m04/`: `FakeShiftStaff`, `FakeTaskSource`, `FakeVitalSigns`, `FakeMedicationChecklist`, `FakeAlertReader`, `FakeDayCarePickups`, `FakeQualityChecks`, `FakeTimetableSource` |
| Cấu hình/công tắc | `FakeM04Config`, `FakeM04Switches`, `FakeM04Controller`, `SwitchRequest(enabled)`; `PUT /api/v1/testing/fakes/{module}`, 204; module `006`, `007`, `008`, `011`, `012`, `014` |

Các view được mô tả projection ở module-interfaces mục 2.1; kiểu chỉ SPI dùng nằm lồng trong SPI theo backend/README.md. Kiểu fixture `FakeVitalSigns.Measurement` lưu đủ đối số lần gọi, không phải DTO production. Không thêm thuật ngữ hay enum vai trò ngoài `common.Role` đã công bố.

### 11.2. Phase 2 — API, sự kiện, nền dữ liệu (2026-10-27)

Đã duyệt riêng bản dịch HTTP danh mục và hai operation result-values (contracts/phase2-api-review.md); chưa cài service/controller do điểm quyền chưa được chốt. API Java bên dưới là khai báo, không nhận là provider nghiệp vụ đã có. Tám SPI Phase1 giữ nguyên.

| Tên logic | Tên triển khai |
| --- | --- |
| KeHoachChamSocApi | `CarePlanApi: effectiveOn, waterTargetMl, hasEffectivePlan, currentTaskTypes` |
| LoaiCongViecApi | `TaskTypeApi.requirements` |
| CoMatBanTruApi | `DayCareAttendanceApi: onDate, returnTimeOn, inRange, changeReturnTime, restoreReturnTime, checkOut` |
| CongViecCaApi | `ShiftTaskApi: onAbsence, onApprovedLeave, onResignation, onAssignmentChanged, onAssignmentEnded, onHandoverConfirmed` |
| CongViecBanGiaoApi | `HandoverTaskApi: unclosed, currentStates, recordedInHandover` |
| NguonCongViecApi | `TaskSourceApi: sourceCancelled, sourceChanged` |
| CongViecVangMatApi | `AbsenceTaskApi.recalculate` |
| CongViecDoChiSoApi | `VitalSignTaskApi.confirmed` |
| CongViecThongKeApi | `TaskStatisticsApi: inRange, attendanceInRange, resultsInRange, resultRecorders` |
| ThoiKhoaBieuApi | `TimetableApi.onDate` |
| KetQuaChamSocCongApi | `PublicCareResultApi: results, summary` |
| PhienBanKeHoachView, MucKeHoachView | `CarePlanView`, `CarePlanItemView` |
| YeuCauLoaiCongViec, CoMatView | `TaskRequirements`, `AttendanceView` |
| KetQuaXacNhan, CongViecChoBanGiaoView, TrangThaiCongViecView | `HandoverTransferResult`, `HandoverTaskView`, `TaskStateView` |
| KhoangThoiGian, CongViecBaoCaoView, KetQuaView | `CareTimeRange`, `TaskReportView`, `CareResultView` |
| KetQuaNguoiThanView, TongHopAnUongView, lịch sử trạng thái | `FamilyCareResultView`, `EatingDrinkingSummary`, `TaskStateChange` |
| Mức quan trọng | `Importance.NORMAL/IMPORTANT/MANDATORY` |
| Loại kết quả | `ResultType.EATING/ML/MEASURED_VALUE/MOVEMENT/SLEEP/MOOD/BEHAVIOR/ITEM_QUANTITY/PERFORMED/NONE` |
| Trạng thái công việc | `TaskStatus.NOT_YET_DUE/DUE/OVERDUE/COMPLETED/COMPLETED_LATE/NOT_PERFORMED/CANCELLED` |
| Trạng thái phiên bản | `CarePlanStatus.DRAFT/PENDING_APPROVAL/PENDING_EFFECT/EFFECTIVE/EXPIRED/CANCELLED` |
| Trạng thái có mặt | `AttendanceStatus.NOT_ARRIVED/PRESENT/LEFT/REPORTED_ABSENT/UNREPORTED_ABSENT/HOLIDAY_CANCELLED` |
| QUA_DON, TU_VE, VIEN_DUA_VE | `CheckoutType.PICKUP/SELF_RETURN/INSTITUTE_RETURN` |
| KhoaChamSoc, ViecTheoPhutM04, ViecHangNgayM04 | `CareResidentLock`, `CareMinuteJob` (CARE_MINUTE), `CareDailyJob` (CARE_DAILY) |

DTO ở api/dto, sự kiện ở api/events, private metadata/read/store/service/config đúng internal/<chức năng>/<tầng>. Không tạo thêm enum Role, dùng common.Role; default_role trong SQL dùng VARCHAR để giữ một nguồn enum Java, provider/lệnh sau này phải kiểm Role hợp lệ.

| Sự kiện logic | Record trong api/events |
| --- | --- |
| YeuCauCanhBao | `AlertRequested` |
| YeuCauDongCanhBao | `AlertClosureRequested` |
| NhacQuaHan | `OverdueTaskReminder` |
| NhacQuaHanGop | `BatchedOverdueTaskReminder` |
| CongViecQuaHanCanXuLy | `OverdueTaskNeedsHandling` |
| CaChuaCoCongViec | `ShiftWithoutTasks` |
| CanLapCongViec | `TaskPlanningNeeded` |
| KeHoachDaDoi | `CarePlanChanged` |
| NguonCongViecDaDoi | `TaskSourceChanged` |
| PhienBanKeHoachDaGuiDuyet | `CarePlanSubmitted` |
| PhienBanKeHoachDaTraLai | `CarePlanReturned` |
| PhienBanKeHoachDaThuHoi | `CarePlanApprovalWithdrawn` |
| PhienBanKeHoachQuaHanDuyet | `CarePlanApprovalOverdue` |
| PhienBanKeHoachBaoQuanLyVien | `CarePlanManagerEscalation` |
| NhacXemLaiThoaThuan | `RiskAgreementReviewReminder` |
| CanXemXetKeHoach | `CarePlanReviewNeeded` |
| CongViecThanhChung | `TaskBecameShared` |
| CongViecChungKhongAiDuDieuKien | `SharedTaskWithoutQualifiedStaff` |
| DaDiemDanhDen | `DayCareCheckedIn` |
| VangKhongBao | `UnreportedAbsence` |
| BanTruConCoMatHetNgay | `DayCarePresentAtDayEnd` |
| TrangThaiCoMatDaXacDinh | `AttendanceDetermined` |
| TrangThaiCoMatDaDoi | `AttendanceChanged` |
| KetQuaCoTinhPhi | `BillableCareResultRecorded` |
| KetQuaTinhPhiDaDinhChinh | `BillableCareResultCorrected` |
| AnKemKeoDai | `ProlongedPoorEating` |
| CongViecDoBiBo | `MeasurementTaskMissed` |
| KetQuaNguDaGhi | `SleepResultRecorded` |

CareEventContext là metadata sourceType/sourceId/milestone/occurredAt/floorAreaId/recipients; CareEvent tính key, CareNotice triển khai HasNotificationRequests, CareEventNotifications dựng khuôn. Khóa hash namespace+mốc có độ dài cố định, sourceId và tên event là tiền tố; nguồn/task type và loại ngày xem lại không bị gộp nhầm. Các DTO/event chưa có HTTP schema nghiệp vụ mới.

| Bảng logic / file | Tên thực tế |
| --- | --- |
| cham_soc_nct (nct_id, phien_ban) | `care_resident_lock(resident_id, version)` |
| lan_chay_m04 (viec, moc) | `m04_job_milestone(job, milestone)` |
| loai_cong_viec | `care_task_type`: task_group, professional, result_type, default_role, default_importance, window_before_minutes, window_after_minutes, billable, service_id, upper_limit, vital_sign_type; name/status/version/active_name theo nhóm1 |
| loai_cong_viec_chung_chi | `care_task_type_certificate(task_type_id, certificate_code)` |
| danh_muc_gia_tri_ket_qua | `care_result_value(value_type, code, name, rule_group, status, version, active_name)` |
| loai_ly_do_khong_thuc_hien | `care_non_performance_reason_type(name, fixed, status, version, active_name)` |
| V005_01__nen_m04.sql | `V005_01__care_foundation.sql` |
| V005_01a__loai_ly_do.sql | `V005_01_1__non_performance_reason_type.sql`; 01a không hợp lệ với version số Flyway |
| Nhóm kết quả ngủ/tâm trạng/hành vi | `SLEEP_QUALITY/MOOD/BEHAVIOR`, rule_group `INSOMNIA/NEGATIVE` hoặc NULL |
| Nhóm công việc18giá trị | `PERSONAL_HYGIENE/BATHING/DRESSING/EATING_ASSISTANCE/DRINKING_ASSISTANCE/TOILETING_ASSISTANCE/MOVEMENT/REHABILITATION/VITAL_SIGN_MEASUREMENT/ACTIVITY/SLEEP/MOOD/BEHAVIOR/DIAPER_ITEM_CHANGE/SHIFT_START_CHECK/SUPERVISION/MEDICATION/OTHER` |

Nền migration hiện chỉ có sáu bảng trên. Provider T008 dùng các **tên dự kiến của bảng story chưa tạo**, không phải bằng chứng đã cài các bảng: care_plan (resident_id, previous_plan_id), care_plan_item (care_plan_id, source_item_id), care_risk_agreement (care_plan_id, source_agreement_id), care_risk_opinion/care_risk_participant (agreement_id), care_task (resident_id nullable, floor_area_id, planned_at, basis_type, basis_id, original_task_id), care_result (task_id, resident_id), care_task_history (task_id), care_day_attendance (resident_id), care_attendance_record (attendance_id). Migration/service story phải giữ đồng bộ các tên này và bổ sung cột nghiệp vụ đúng data-model; không lấy projection tối thiểu làm toàn bộ schema.

Loại đối tượng chính: M04.CARE_PLAN, M04.TASK, M04.CARE_RESULT, M04.DAY_CARE_ATTENDANCE. Lưu giữ còn có M04.CARE_PLAN_ITEM, M04.RISK_AGREEMENT, M04.RISK_OPINION, M04.TASK_HISTORY, M04.ATTENDANCE_RECORD và ba alias LOCATION_TASK/LOCATION_CARE_RESULT/LOCATION_TASK_HISTORY cho cùng bảng nhưng thời hạn CFG-M15-04. Alias là phân tách kỹ thuật RetentionKind, chưa thêm trường hoặc endpoint HTTP. Nhật ký/căn cứ của story sau phải dùng đúng loại để đính chính/lưu giữ đi kèm được kiểm nhất quán.

Bản giả API mới: FakeM04ApiConfig/FakeM04ApiFixture, import riêng với test/demo, không thay FakeM04Config của8SPI. Công tắc các SPI Phase1 không tự bật tắt fixture API mới; hai cấu hình có mục đích khác nhau.

### 11.3. HTTP kế hoạch Phase 3 (005, duyệt 2026-10-27)

Chỉ là hợp đồng đã dịch, chưa xác nhận endpoint runtime. Bảng đầy đủ và operationId tại `005-daily-care-tasks/contracts/phase3-api-review.md`; không đổi chữ ký DTO/API nội bộ đã công bố.

| Tên logic | Tên tiếng Anh |
| --- | --- |
| `/nguoi-cao-tuoi/{nctId}/ke-hoach-cham-soc`, `/ke-hoach-cham-soc/{id}` | `/residents/{residentId}/care-plans`, `/care-plans/{id}` |
| `/muc`, `/thoa-thuan`, `/gui-duyet`, `/duyet`, `/tra-lai`, `/huy`, `/thu-hoi` | `/items`, `/risk-agreements`, `/submit`, `/approve`, `/return`, `/cancel`, `/withdraw-approval` |
| `/so-sanh?tu=&den=` | `/compare?from=&to=` |
| PhienBanKeHoach, MucKeHoach, ThoaThuanRuiRo, SoSanhPhienBan | `CarePlan`, `CarePlanItem`, `RiskAgreement`, `CarePlanComparison` |
| saoChep, nctId, soPhienBan, trangThai | `copyCurrent`, `residentId`, `revision`, `status` |
| ngayHieuLuc, hieuLucDen, nguoiLap, nguoiDuyet, duyetLuc, tuDuyet | `effectiveFrom`, `effectiveTo`, `createdBy`, `approvedBy`, `approvedAt`, `selfApproved` |
| lyDoThayDoi, lyDo, muc, thoaThuan, chuaCoKeHoachHieuLuc, lenhDuocPhep | `changeReason`, `reason`, `items`, `riskAgreements`, `noEffectivePlan`, `allowedCommands` |
| mucGocId, loaiCongViecId, noiDung | `sourceItemId`, `taskTypeId`, `content` |
| kieuTanSuat, nGio, soLan, mocBatDau, cacMoc, ngayTrongTuan | `frequencyType`, `intervalHours`, `timesPerDay`, `startsAt`, `times`, `weekdays` (HTTP mảng 1–7) |
| SO_LAN_NGAY, MOI_N_GIO, NGAY_TRONG_TUAN, MOT_LAN_MOI_CA | `TIMES_PER_DAY`, `EVERY_N_HOURS`, `DAYS_OF_WEEK`, `ONCE_PER_SHIFT` |
| khungTruocPhut, khungSauPhut, vaiTroThucHien, loaiKetQua, mucQuanTrong, coTinhPhi, mucTieuMl | `windowBeforeMinutes`, `windowAfterMinutes`, `performerRole`, `resultType`, `importance`, `billable`, `waterTargetMl` |
| thoaThuanGocId, luaChon, ruiRoDaTraoDoi, nctThamGia, nguoiThamGia | `sourceAgreementId`, `choice`, `discussedRisks`, `residentParticipates`, `participants` |
| bacSiNhanDinhId, nhanDinh, nguoiQuyetDinh, nguoiThanId, bacSiId | `assessingDoctorId`, `selfDeterminationAssessment`, `decisionMaker`, `familyMemberId`, `doctorId` |
| loai, id; NGUOI_THAN/NHAN_VIEN của người tham gia | `type`, `id`; `FAMILY_MEMBER/STAFF` |
| NGUOI_CAO_TUOI/DAI_DIEN_NGUOI_BENH/BAC_SI của người quyết định | `RESIDENT/PATIENT_REPRESENTATIVE/DOCTOR` |
| yKien, nguon, nguoiGhi, thoiDiem, ngayXemLai | `opinions`, `source`, `recordedBy`, `at`, `reviewDate` |
| them, bot, doi, truoc, sau, truongDoi | `added`, `removed`, `changed`, `before`, `after`, `changedFields` |
| DA_CO_PHIEN_BAN_MO, PHIEN_BAN_DA_KHOA, NGAY_HIEU_LUC_KHONG_HOP_LE | `OPEN_REVISION_EXISTS`, `REVISION_LOCKED`, `INVALID_EFFECTIVE_DATE` |
| NhanDinhTuQuyet, NguonYKien | `SelfDeterminationAssessment`, `OpinionSource`: chép đúng schema công khai 001, không thêm trường |
| Trạng thái kế hoạch, mức quan trọng | `CarePlanStatus` (giá trị như 11.2), `Importance.NORMAL/IMPORTANT/MANDATORY` |

Các operation chưa dịch ở story khác vẫn giữ tên; tham chiếu tới schema mục/thỏa thuận dùng tên schema mới để không có `$ref` treo. Lỗi kế hoạch dùng `CarePlanProblem`/`CarePlanConditionProblem`, cùng hình dạng tiếng Anh đã có của `CareCatalogProblem`; `CarePlanReason` dùng `Reason`, không đổi thân lý do của các story chưa dịch.

### 11.4. T007a — danh mục loại lý do Không thực hiện (2026-10-28)

| Tên logic | Tên thực tế |
| --- | --- |
| Loại lý do; kho; service; controller danh mục lý do | NonPerformanceReasonType; NonPerformanceReasonStore; NonPerformanceReasonService; NonPerformanceReasonController |
| Yêu cầu ghi tên; lý do ngừng; phản hồi danh mục | NonPerformanceReasonWrite; CareCatalogReason (Reason); NonPerformanceReasonTypeResponse (schema NonPerformanceReasonType) |
| Loại đối tượng nhật ký danh mục lý do | M04.NON_PERFORMANCE_REASON_TYPE |
| Tạo/sửa/ngừng loại lý do | NON_PERFORMANCE_REASON_CREATED / NON_PERFORMANCE_REASON_UPDATED / NON_PERFORMANCE_REASON_DEACTIVATED |
| LOAI_LY_DO_CO_DINH | REASON_TYPE_FIXED (409); dùng lại DUPLICATE_NAME (409), INVALID_VALUE (422), VERSION_MISMATCH (412), REASON_REQUIRED (422) |
| Quyền danh mục (Q-391, thay ghi chú C-16 cũ) | Function.CARE_TASK_CATALOG: VIEW/CREATE/UPDATE |
### 11.5. Nền R-15 và danh mục T007 (2026-10-31)

| Thuật ngữ | Mã triển khai |
| --- | --- |
| Loại công việc, giá trị kết quả, loại lý do Không thực hiện | `CareCatalogKinds.TASK_TYPES/RESULT_VALUES/REASONS`; `M04.TASK_TYPE`, `M04.RESULT_VALUE`, `M04.NON_PERFORMANCE_REASON_TYPE` |
| Nội dung riêng của danh mục | `TaskTypeAttributes`, `ResultValueAttributes`, `ReasonAttributes` |
| ID danh mục trước gộp | `LegacyCatalogId(table,id)`; `attributes.legacy`,adapter giữIDcôngkhai cũ |
| Lệnh danh mục và nguồn yêu cầu loại công việc | `CareCatalogService`, `CareCatalogStore`, `CareTaskTypeLookup` cài `TaskTypeApi` |
| HTTP danh mục đã duyệt | `CareCatalogController`; `TaskTypeWrite/Response`, `ResultValueWrite/Response` |
| Khóa theo người, dấu xử lý theo ngày | `CareResidentState`, `CareLockStore` qua `ResidentModuleStateApi`; `CareMilestoneStore` qua `ReminderMarkers` |
| Migration nền sau gộp | `V005_95__care_foundation_consolidation.sql` |

### 11.6. Kế hoạch theo phiên bản Phase 3 (2026-10-31)

| Thuật ngữ | Mã triển khai |
| --- | --- |
| Phiên bản, mục, nội dung mục | `CarePlan`, `CarePlanItem`, `CarePlanItemData`; `care_plan`, `care_plan_item` |
| Bảng chuyển, chuỗi thời điểm, so sánh | `CarePlanTransitions`, `CarePlanItemSequence`, `CarePlanComparison`; `CarePlanDifference` giữ được khung null của Nháp |
| Kho/lệnh/nguồn đọc kế hoạch | `CarePlanStore`, `CarePlanService`, `CarePlanQueries` (cài `CarePlanApi`) |
| Thỏa thuận, người quyết định, người tham gia, ý kiến | `CareRiskAgreement`, `RiskDecisionMaker`, `RiskParticipant`, `RiskOpinion`, `RiskOpinionEntry` |
| Loại mục trong bảng gộp | `TASK`, `RISK_AGREEMENT`; mọi căn cứ công việc chỉ lấy `TASK` |
| Loại con bảng chung | `CareRiskKinds.PARTICIPANTS/OPINIONS`; `M04.RISK_PARTICIPANT`, `M04.RISK_OPINION`, parent `M04.RISK_AGREEMENT` |
| Kho/lệnh thỏa thuận | `CareRiskStore`, `CareRiskService` |
| Bước hiệu lực, nhắc duyệt, nhắc xem lại | `CarePlanActivation`, `CarePlanApprovalReminders`, `CareRiskReview`; tên bước `CARE_PLAN_EFFECTIVE`, `CARE_PLAN_APPROVAL_REMINDER`, `CARE_RISK_REVIEW` |
| Thông báo theo người có quyền duyệt | `CarePlanNotices`; từng `RecipientGroup.Approvers` chỉ mang một account để giới hạn payload |
| Lệnh được phép của phiên bản | `replaceItems`, `replaceRiskAgreements`, `submit`, `approve`, `return`, `cancel`, `withdrawApproval` |
| Lỗi kế hoạch thiếu mục, hồ sơ cuối, nguồn người lập chưa sẵn sàng | `EMPTY_PLAN` (409), `RESIDENT_FINAL_STATUS` (409), `CARE_SOURCE_UNAVAILABLE` (503) |
| Nhật ký lệnh kế hoạch | `CARE_PLAN_CREATED`, `CARE_PLAN_ITEMS_REPLACED`, `CARE_PLAN_RISKS_REPLACED`, `CARE_PLAN_SUBMITTED`, `CARE_PLAN_APPROVED`, `CARE_PLAN_SELF_APPROVED`, `CARE_PLAN_RETURN`, `CARE_PLAN_CANCEL`, `CARE_PLAN_WITHDRAW` |
| Nhật ký job kế hoạch | `CARE_PLAN_EFFECTIVE`, `CARE_PLAN_EXPIRED`, `CARE_PLAN_APPROVAL_REMINDER` |
| Migration kế hoạch và trigger con theo phiên bản | `V005_96__care_plan_revisions.sql` |
| Màn web T017 | `features/care-tasks/CarePlanPage`, `CarePlanDetail`, `CarePlanItemsForm`, `CareRiskAgreementsForm`, `CareCatalogPage`; menu `care-plans`, `care-catalogs` |
| Phiên bản danh mục chỉ đọc được chủ dự án duyệt31/10 | `version` trên `TaskType`, `ResultValue`, `NonPerformanceReasonType`; gửi lại bằng `If-Match` khi sửa |
| Lý do thông báo Trả lại, được chủ dự án duyệt31/10 | `CarePlanReturned.reason`; `String`, null cho constructor/sự kiện cũ; chỉ trong `ContentPart(HEALTH, reason)`, không có trong `title`/`shortContent` |

### 11.7. Lập công việc Phase 4 (2026-11-01)

| Thuật ngữ | Mã triển khai |
| --- | --- |
| Lệnh nhập, snapshot, công việc và kết quả kiểm | `CareTaskDraft`, `CareTaskData`, `CareTask`, `CareTaskValidation` |
| Bảng chuyển và trạng thái lúc lập | `CareTaskTransitions.next/initialStatus` |
| Kiểm từng dòng, lệnh lô, nguồn đọc | `CareTaskValidator`, `CareTaskService`, `CareTaskQueries` |
| Kho công việc và thông tin đóng | `CareTaskStore`, `CareTaskClosure`; `care_task` |
| Lịch sử xử lý dùng chung | `CareTaskHandling`; `HistoryTrack(M04.TASK,HANDLING)` qua `StatusHistoryApi` |
| Callback hủy/đổi nguồn | `CareTaskSources` cài `TaskSourceApi`, MANDATORY |
| Bước tới hạn, nhắc khi lập muộn | `CareTaskDueStep`, `CareTaskNotices`; marker `M04.TASK_OVERDUE` |
| Đọc nguồn/nhân sự đã duyệt | `TaskSourcePort.sources`/`SourceView`, `ShiftStaffPort.members`/`ShiftMemberView` |
| Lỗi theo dòng từ 1 | `rowErrors[{row,codes}]`, HTTP 409 `UNMET_CONDITIONS` |
| Mã kiểm từng dòng C-07 | `RESIDENT_NOT_STAYING`, `RESIDENT_ABSENT`, `DAY_CARE_NOT_PRESENT`, `OUTSIDE_SHIFT`, `PLAN_ITEM_MISMATCH`, `INVALID_BASIS`, `MEDICATION_IS_NOT_A_TASK`, `INVALID_PERFORMER`, `MISSING_CERTIFICATE`, `DUPLICATE_BASIS`, `OLD_REVISION_WINDOW_OVERLAP`, `SHIFT_CHECK_ALREADY_EXISTS` |
| Khóa công việc đã tới hạn/đóng | HTTP 409 `TASK_CLOSED`; không hủy việc đã thực tế tới đầu khung khi job chậm |
| Migration mới T020 | `V005_97__shift_care_tasks.sql`, BUSINESS; `duplicate_key`, `shift_check_key` |
| Màn web/mobile T026 | `ShiftTaskPlanningPage`, menu `care-task-planning`; `ShiftTaskPlanningScreen`, `CareTaskPlanningGateway` (chưa có adapter production) |

Chủ dự án chọn hoãn mục 5–6: không công bố `ongoingShifts`, không sửa client mobile của A; cảnh báo MANDATORY gắn vị trí cũng chờ thống nhất với 007 (đề xuất mục 7).

### 11.8. Có mặt bán trú Phase 5 — bản cài 2026-11-01

| Thuật ngữ | Mã triển khai |
| --- | --- |
| Bảng chuyển FR-013 | `AttendanceTransitions` |
| Snapshot hồ sơ và bản ghi điểm danh | `AttendanceData`, `AttendanceRecordData`, `AttendanceKinds`, `AttendanceDaySource` |
| Lưu hồ sơ nhóm2/bản ghi nhóm3 | `AttendanceStore` qua `SimpleRecordApi`/`RecordEventApi`; kind `M04.DAY_CARE_ATTENDANCE`, `M04.ATTENDANCE_RECORD` |
| Nguồn, lệnh và projection API | `AttendanceSources`, `AttendanceService`, `AttendanceQueries` (@Fallback) |
| Kết quả về cùng danh sách việc hủy | `AttendanceCheckoutResult`, `DayCareCheckoutResponse` |
| Các bước theo lịch | `AttendanceDayStartWork` (`DAY_CARE_DAY_START`), `AttendanceClockWork`, `AttendanceDayEndWork` (`DAY_CARE_DAY_END`) |
| Mốc báo hết ngày | `M04.ATTENDANCE_DAY_END`, entity `M04.DAY_CARE_ATTENDANCE`, marker ngày |
| Đính chính giờ | `AttendanceCorrectionProvider`, `AttendanceCorrectionWrite`, `DayCareAttendanceCorrectionController`; AMEND_CONTENT, VOID chưa hỗ trợ |
| Nguồn ngày nghỉ/báo vắng đã duyệt | `DayCareCalendarPort.CalendarView(holiday,timelyAbsenceId,lateAbsenceId)`; fixture `FakeDayCareCalendar` |
| Callback đã duyệt | `DayCareAttendanceApi.refreshCalendar(residentId,date)` MANDATORY, giữ chữ ký cũ |
| Bốn HTTP | `/day-care-attendances`, `/day-care-attendances/check-in`, `/day-care-attendances/{id}/check-out`, `/day-care-attendance-records/{id}/correct` |
| Request/response | `DayCareCheckInWrite`, `DayCareCheckOutWrite`, `DayCareAttendanceResponse`; requestBody `DayCareAttendanceCorrection` |
| Màn web/mobile | `DayCareAttendancePage`, menu `day-care-attendance`; `DayCareAttendanceScreen`, `DayCareAttendanceGateway`, `DayCareAttendanceData` |
| Lỗi điều kiện | `DAY_CARE_NOT_STAYING`, `ATTENDANCE_STATE_CONFLICT`, `INVALID_PICKUP_RECORD`; thiếu nguồn `CARE_SOURCE_UNAVAILABLE`; VOID dùng `CORRECTION_NOT_SUPPORTED` của khung chung |
| Migration và cột/index riêng M04 | `V005_94__shared_day_care_attendance.sql`; `m04_attendance_date`, `m04_expected_arrival`, `uq_m04_attendance_day`, `ck_m04_attendance_state`, `ix_m04_attendance_due`, `ix_m04_attendance_day` |
| Test | `FR_013_AttendanceTransitionsTest`, `FR_012_AttendanceSchemaIT`, `FR_012_AttendanceContractTest`, `BR_M04_02_DayCareAttendanceIT`, `FR_015_AttendanceCorrectionControllerTest` |

API mục1–3 của đề xuất đã được duyệt. Chiều mã giữ care→admission: adapter/listener thuộc care, nguồn API/sự kiện004 do B công bố. Chưa nghiệm thu nguồn thật, nhánh đổi khỏiDAY_CARE, VOID, retention correction chain hoặc adapter mobile. Không coi raw parent M01.RESIDENT đã bảo đảm lưu giữ toàn chuỗi.

### 11.9. Checklist và kết quả Phase 6 (005)

| Tên logic | Mã triển khai |
| --- | --- |
| Trạng thái đóng theo khung | `CareResultTiming.completedStatus`, `COMPLETED`, `COMPLETED_LATE` |
| Lệnh/kiểm kết quả | `CareResultService`, `CareResultValidation`, `CareResultInput`, `CareResultStore`, `CareResultQueries` |
| Checklist/nhận việc | `CareChecklistService`, `CareChecklistStore`, `CareChecklistController`, `CareChecklistResponse`, `ChecklistRiskAgreement` |
| Phát sinh/kết quả công khai/xác nhận chỉ số | `AdHocCareRecordingService`, `PublicCareResults`, `CareVitalSignTasks` |
| Request/response | `CareResultWrite`, `CareNotPerformedWrite`, `AdHocCareRecordingWrite`, `CareResultResponse`; `CareTask.resultType` readOnly đã duyệt |
| Snapshot cấu hình kết quả nội bộ | `CareTaskData.upperLimit`, `vitalSignType`, `resultConfigurationCaptured` |
| Căn cứ quyền | `ASSIGNED`, `FLOOR_MANAGER_ROLE`, `SHIFT_LEAD_DUTY` |
| Mức ăn/vận động | `ATE_ALL`, `MOST`, `SOME`, `NOT_EATEN`, `SKIPPED`; `INDEPENDENT`, `ASSISTED`, `UNABLE` |
| Việc tồn | `FROM_PREVIOUS_SHIFT`, `AWAITING_HANDOVER_CONFIRMATION` |
| Lỗi lệnh | `CARE_TASK_CLOSED`, `CONFIRMATION_REQUIRED`, `AWAITING_REMEASUREMENT`, `SERVICE_NOT_CONFIRMED`, `REASON_REQUIRED`; thiếu nguồn/quy tắc dùng `CARE_SOURCE_UNAVAILABLE` |
| Web/mobile | `CareChecklistPage`, `CareChecklistDetail`, `CareResultForm`; mobile `CareChecklistScreen`, `CareChecklistRepository`, `CareChecklistCache`, `CachedCareChecklist`, `ApiCareChecklistGateway` |
| Migration R-15 | `V005_98__care_task_results.sql`, `V005_99__care_checklist_carryover.sql`, `V005_100__care_result_reason_reference.sql`; kết quả gộp `care_task`, không tạo `care_result` |

### 11.10. Quá hạn và bàn giao Phase 7 (005, 02/11/2026)

| Tên nghiệp vụ / đường dẫn logic | Tên mã / đường dẫn thực |
| --- | --- |
| Danh sách công việc quá hạn | GET /care-tasks/overdue; query floorAreaId (chủ dự án duyệt bản dịch) |
| Chuyển trạng thái/leo thang theo phút | CareTaskDueStep, CareTaskClockProcessor, CareTaskNotices; internal/task/service |
| Nguồn bàn giao đầy đủ005 | CareHandoverQueries implements HandoverTaskApi |
| Projection tương thích008 | CareTaskHandoverProjection implements TaskHandoverApi; chưa hỗ trợ task không residentId |
| Xác nhận bàn giao | CareTaskHandover.onHandoverConfirmed; MANDATORY |
| Tồn từ ca trước / tồn chờ xác nhận | FROM_PREVIOUS_SHIFT / AWAITING_HANDOVER_CONFIRMATION (carryover hiện có) |
| Lịch sử người trước/sau và căn cứ bàn giao | CareTaskHandling.previousPerformerId / performerId / handoverId; StatusHistoryApi |
| Màn quá hạn web / app nhân viên | features/care-tasks/OverdueTasksPage.jsx / staff/care_tasks/overdue_tasks_screen.dart |

Không thêm enum/trường API công khai mới; callback ngoài bàn giao và các dependency chưa đạt giữ gate trong plan/team-plan.

### 11.11. Quy tắc từ kết quả Phase 8 (005, 02/11/2026)

| Tên nghiệp vụ | Tên mã |
| --- | --- |
| Hàm thuần nước, chuỗi bữa, chuỗi ngày | `WaterIntakeRule`, `PoorMealStreak`, `NegativeMoodStreak`; `internal/rules/domain` |
| Projection hiện hành từ kết quả gốc và đính chính | `CurrentCareResults`; `internal/rules/service`, tái sử dụng `CareResultStore`/`CareTaskStore` |
| Điều phối quy tắc và bước kiểm tra nước theo phút | `CareResultRules`, `CareWaterCheckStep`; `internal/rules/service` |
| Loại yêu cầu cảnh báo nước / ăn / tâm trạng | `LOW_WATER_INTAKE`, `PROLONGED_POOR_EATING`, `PROLONGED_NEGATIVE_MOOD` (trường String hiện có) |
| Dấu xử lý/ngăn phát lặp | `M04.WATER_CHECK`, `M04.POOR_EATING`, `M04.NEGATIVE_MOOD`; `reminder_marker` |

Giữ nguyên chữ ký `AlertRequested`, `ProlongedPoorEating`, `CorrectionMade`. `mealIds` phải là id bữa của 011; nhánh EATING chưa liên kết giữ gate theo quyết định chủ dự án. Không tạo bảng kết quả hoặc bộ đếm mới.

### 11.12. Đính chính kết quả Phase 9 (005, 02/11/2026)

| Khái niệm | Tên mã |
| --- | --- |
| Endpoint đính chính đã duyệt | `POST /care-results/{id}/correct`; `CareResultCorrection`, `type`, `correctedContent`, `reason`; `AMEND_CONTENT`, `VOID` |
| Provider kết quả nhóm 3 | `CareResultCorrectionProvider`, loại `M04.CARE_RESULT`; `internal/result/service` |
| Transaction của endpoint care | `CareResultCorrectionService`, `READ_COMMITTED` |
| HTTP controller/request | `CareResultCorrectionController`, `CareResultCorrectionWrite`; `web`, `web/request` |
| Client sinh từ YAML | `postCareResultsByIdCorrect` trong `frontend/src/api/api005.js` |
| Mốc đính chính ngủ | `CORRECTION:<correctionId>` trên `SleepResultRecorded.context` |

Không thêm chữ ký SPI/event Java; quantity tính phí và tín hiệu VOID ngủ còn gate đã ghi trong plan/team-plan.

## 12. Feature 006 (`006-medication-management`, D)

### 12.1. Bảng và cột nền tảng (T005, `V006_01__medication_foundation.sql`)

| Tên logic | Mã |
| --- | --- |
| `ly_do_doi_chieu` `TIEP_NHAN`, `TRO_VE_BENH_VIEN` | `ADMISSION`, `HOSPITAL_RETURN` |

*Mục 12.2 → 12.5 bổ sung 2026-10-26 khi dịch `contracts/api-006.yaml` (sửa sau PR #34). Tên mã theo tên D đã dùng trong medication khi cùng nghĩa.*

### 12.2. Đường dẫn API (tiền tố `/api/v1`) và tag

| Tên logic | Mã |
| --- | --- |
| `/thuoc`, `/thuoc/{id}`, `…/ngung-hieu-luc` | `/medications`, `/medications/{id}`, `…/deactivate` |
| `/nguoi-cao-tuoi/{nctId}/don-thuoc`, `…/kiem-tra` | `/residents/{residentId}/prescriptions`, `…/check` |
| `/don-thuoc/{id}`, `…/ghi-chu`, `…/tam-dung`, `…/tiep-tuc`, `…/ngung`, `…/doi-lieu`, `…/gia-han`, `…/lan-dung-prn` | `/prescriptions/{id}`, `…/instruction-note`, `…/suspend`, `…/resume`, `…/discontinue`, `…/change-dosage`, `…/extend`, `…/prn-administrations` |
| `/nguoi-cao-tuoi/{nctId}/lich-thuoc` (tham số `tu`, `den`) | `/residents/{residentId}/medication-schedule` (`from`, `to`) |
| `/lieu-thuoc/vong-phat-thuoc`, `/lieu-thuoc/{lieuKey}`, `…/da-dung`, `…/tu-choi`, `…/khong-thuc-hien` | `/medication-doses/rounds`, `/medication-doses/{doseKey}`, `…/administer`, `…/refuse`, `…/skip` |
| `/xem-lai-lieu`, `…/{id}/ket-qua` | `/dose-reviews`, `…/{id}/result` |
| `/nguoi-cao-tuoi/{nctId}/phieu-doi-chieu`, `/phieu-doi-chieu/{id}`, `…/dong`, `…/gui-xac-nhan`, `…/tra-lai`, `…/xac-nhan`, `…/de-nghi-dinh-chinh` | `/residents/{residentId}/medication-reconciliations`, `/medication-reconciliations/{id}`, `…/items`, `…/submit`, `…/return`, `…/confirm`, `…/propose-correction` |
| `/de-nghi-dinh-chinh-phieu/{id}/xac-nhan`, `…/tra-lai` | `/reconciliation-corrections/{id}/confirm`, `…/return` |
| `/tiep-nhan-thuoc-cung-ung` (tham số `yeuCauMuaSamId`) | `/supplied-medication-intakes` (`purchaseRequestId`) |
| `/nguoi-cao-tuoi/{nctId}/thuoc-gia-dinh-gui`, `/thuoc-gia-dinh-gui/{id}`, `…/xac-nhan-su-dung`, `…/chi-giu-ho`, `…/ngung-su-dung`, `…/kiem-ke-dieu-chinh`, `…/hoan-tra`, `…/huy` | `/residents/{residentId}/family-medications`, `/family-medications/{id}`, `…/confirm-use`, `…/hold-only`, `…/stop-use`, `…/adjust-inventory`, `…/return`, `…/dispose` |
| `/nguoi-cao-tuoi/{nctId}/thuoc-can-mang` (tham số `chuyenId`), `…/lan-giao-thuoc`, `/lan-giao-thuoc/{id}/nhan-lai`, `/nguoi-cao-tuoi/{nctId}/khong-mang-thuoc` | `/residents/{residentId}/medications-to-bring` (`tripId`), `…/medication-handovers`, `/medication-handovers/{id}/return`, `/residents/{residentId}/no-medication-orders` |
| tham số `hieuLuc` (danh mục), `trangThai` (đơn, xem lại liều) | `active`, `status` |
| tag `danh-muc-thuoc`, `don-thuoc`, `lieu`, `prn`, `doi-chieu`, `thuoc-gui`, `mang-theo`, `tiep-nhan-thuoc` | `medication-catalog`, `prescription`, `dose`, `prn`, `reconciliation`, `family-medication`, `leave-medication`, `supplied-medication` |

### 12.3. Schema và trường JSON

| Tên logic | Mã |
| --- | --- |
| `Thuoc` (`ten`, `hamLuong`, `dangBaoChe`, `duongDungMacDinh`, `donViDung`, `hoatChat`, `cuaSoTruocPhut`, `cuaSoSauPhut`, `kiemSoatDacBiet`, `hieuLuc`, `chuaCoMucVatPham`) | `Medication` (`name`, `concentration`, `dosageForm`, `defaultRoute`, `usageUnit`, `allergenIds`, `beforeWindowMinutes`, `afterWindowMinutes`, `specialControl`, `active`, `missingPricingItem`) |
| `TanSuat` (`kieu`, `cacMoc`, `nGio`, `mocDau`, `ngayTrongTuan`) | `Frequency` (`type`, `timedSlots`, `everyNHours`, `firstSlot`, `daysOfWeek`) |
| `CoSoKe` (`loai`, `tenCoSo`, `nguoiKe`, `ngayKe`, `tepDonId`) | `Prescriber` (`type`, `facilityName`, `prescriberName`, `prescribedAt`, `prescriptionFileId`) |
| `CanCuCoSoKe` (`tenCoSo`, `nguoiChiDinh`, `ngay`, `hinhThuc`, `nguoiGoi`, `nguoiTraLoi`, `thoiDiemGoi`, `tepId`) | `ExternalBasis` (`facilityName`, `prescriberName`, `prescribedDate`, `basisType`, `callerName`, `receiverName`, `calledAt`, `documentFileId`) |
| `LenhDonThan` (`lyDo`, `canCuCoSoKe`); request body `LyDo`, `LenhDon` | `PrescriptionCommandBody` (`reason`, `externalBasis`); `Reason`, `PrescriptionCommand` |
| `VanDe` (`maVanDe`, `loai`, `hoatChat`, `donTrungId`, `mucDiUngId`, `mucDo`, `khongKiemTraTuDong`, `dong`) | `Issue` (`issueCode`, `issueType`, `activeIngredient`, `duplicatePrescriptionId`, `allergyItemId`, `severity`, `notAutoChecked`, `lineNumber`) |
| `DonThuocMoi` (`thuocId`, `lieu`, `duongDung`, `loaiDon`, `tanSuat`, `khoangCachToiThieuPhut`, `soLanToiDa24h`, `batDauLuc`, `ngayKetThuc`, `nguonThuoc`, `coSoKe`, `ghiChuHuongDan`, `xacNhanVanDe` {`maVanDe`, `lyDo`}) | `NewPrescription` (`medicationId`, `dosage`, `route`, `prescriptionType`, `frequency`, `minIntervalMinutes`, `maxDosesPer24h`, `startedAt`, `endedAt`, `medicationSource`, `prescriber`, `instructionNote`, `issueConfirmations` {`issueCode`, `reason`}) |
| `DonThuoc` (`nctId`, `trangThai`, `lyDoTamDung`, `dauChoTiepNhan`, `ngungTu`, `thayTheChoId`, `biThayTheBoiId`, `chuaCoThuoc`, `vanDeDaXacNhan`, `loGan`, `lenhDuocPhep`, `truongBiAn`) | `Prescription` (`residentId`, `status`, `suspensionReason`, `pendingAdmission`, `suspendedFrom`, `replacesId`, `replacedById`, `noMedicationAvailable`, `confirmedIssues`, `boundBatchIds`, `allowedCommands`, `hiddenFields`) |
| thân ngừng `ngungTu`, phản hồi `canhBao`; thân gia hạn `ngayKetThucMoi`; thân đổi liều `donThayThe`, `hieuLucTu` | `discontinueAt`, `warnings`; `newEndedAt`; `newPrescription`, `effectiveFrom` |
| `Lieu` (`lieuKey`, `donThuocId`, `thoiDiemDuKien`, `cuaSoTu`, `cuaSoDen`, `trangThai`, `trangThaiHienHanh`, `daVatHoa`, `lieuChungTang`, `tonChoXacNhanBanGiao`, `choGhiNhanSauTroVe`, `batBuocTrucTuyen`, `nguonThuoc`, `loDuocDung` {`loId`, `hanDung`, `soLuongCon`}, `lanGiaoId`, `ketQua`, `dongBoi`, `canRaSoat`) | `Dose` (`doseKey`, `prescriptionId`, `scheduledTime`, `windowFrom`, `windowTo`, `status`, `currentStatus`, `materialized`, `floorShared`, `pendingHandoverConfirmation`, `awaitingRecordAfterReturn`, `requiresOnline`, `medicationSource`, `usableBatches` {`batchId`, `expiryDate`, `remainingQuantity`}, `handoverId`, `result`, `closedBy`, `needsReview`) |
| `XacNhanLieu` (`thoiDiemDung`, `loThuocGuiId`, `canCu`, `nguoiBaoLai`, `phanUng`, `lyDo`, `ghiChu`, `ghiSauMatKetNoi`) | `DoseResult` (`administeredAt`, `familyMedicationBatchId`, `basis`, `reportedBy`, `adverseReaction`, `reason`, `note`, `recordedAfterDisconnect`) |
| `LanDungPrnMoi` (`thoiDiemDung`, `lieuDung`, …) | `NewPrnAdministration` (`administeredAt`, `dosage`, …, như `DoseResult`) |
| `NguoiNgoai` (`loai`, `id`, `hoTen`, `quanHe`) | `ExternalPerson` (`type`, `id`, `fullName`, `relationship`) |
| thân xem lại liều `ketQua`, `ghiChu` | `result`, `note` |
| `PhieuDoiChieu` (`nctId`, `lyDo`, `lyDoLap`, `trangThai`, `hanLuc`, `quaHan`, `nguoiLap`, `nguoiGui`, `nguoiXacNhan`, `dong`, `lenhDuocPhep`) | `Reconciliation` (`residentId`, `reason`, `reasonDetail`, `status`, `deadlineAt`, `overdue`, `createdBy`, `submittedBy`, `confirmedBy`, `items`, `allowedCommands`) |
| `DongDoiChieu` (`nguon`, `donThuocId`, `mucThuocDangDungId`, `moTaThuoc`, `quyetDinh`, `lyDo`, `donMoi`, `daNgungNgoaiPhieu`, `thuocDaBoKhoiHoSo`, `donTaoId`) | `ReconciliationItem` (`source`, `prescriptionId`, `admissionMedicationItemId`, `medicationDescription`, `decision`, `reason`, `newPrescription`, `discontinuedOutsideTicket`, `medicationRemovedFromProfile`, `createdPrescriptionId`) |
| thân đề nghị đính chính `noiDungDung`, `lyDo` | `correctedContent`, `reason` |
| `LanTiepNhanThuocGhi` (`dongYeuCauId`, `hamLuong`, `dangThuoc`, `soLuongNhan`, `donVi`, `hanDung`, `tinhTrang`, `ketQua`, `saiLech`, `lyDo`, `nguoiGiao`) | `NewSuppliedMedicationIntake` (`requestLineId`, `concentration`, `dosageForm`, `receivedQuantity`, `unit`, `expiryDate`, `condition`, `result`, `discrepancy`, `reason`, `delivererName`) |
| `TiepNhanThuocGui` (`thuocId`, `tenTuDo`, `hoatChatTuDo`, `hamLuong`, `soLuong`, `donVi`, `hanDung`, `tinhTrangBaoBi`, `coToa`, `tepIds`, `nguoiGiao`, `deNghiMuaHoId`, `thoiDiem`) | `ReceiveFamilyMedication` (`medicationId`, `customName`, `customActiveIngredient`, `concentration`, `quantity`, `unit`, `expiryDate`, `packagingCondition`, `hasPrescription`, `fileIds`, `deliverer`, `purchaseAssistanceId`, `receivedAt`) |
| `ThuocGui` (`trangThai`, `soLuongCon`, `donGan`, `soNgayConLai`, `bienDong` {`loai`, `soLuong`, `thoiDiem`, `nguon`}) | `FamilyMedicationBatch` (`status`, `remainingQuantity`, `boundPrescriptionIds`, `daysRemaining`, `movements` {`movementType`, `quantity`, `movedAt`, `source`}) |
| thân xác nhận sử dụng `donThuocIds`, `thuocId`, `baoBiNhanDienDuoc`, `canCu`; kiểm kê `soLuongThucTe`; hoàn trả `soLuong`, `nguoiNhan`, `thoiDiem`; hủy `nguoiDaiDienId`, `cachNhanYeuCau`, `nguoiChungKien` | `prescriptionIds`, `medicationId`, `packagingRecognizable`, `basis`; `actualQuantity`; `quantity`, `recipient`, `returnedAt`; `representativeId`, `requestMethod`, `witness` |
| `LanGiaoMoi` (`chuyenId`, `nguoiNhan`, `dong` {`donThuocId`, `soLuong`, `loThuocGuiId`}); `LanGiao` (`trangThai`, `thoiDiem`, `lieu` {`lieuKey`, `thoiThuoc`}); thân nhận lại `dong`, `lyDo` | `NewMedicationHandover` (`tripId`, `recipient`, `items` {`prescriptionId`, `quantity`, `familyMedicationBatchId`}); `MedicationHandover` (`status`, `handoverTime`, `doses` {`doseKey`, `reverted`}); `items`, `reason` |
| thân không mang thuốc `chuyenId`, `lyDo` | `tripId`, `reason` |
| `Loi`; response `Loi`, `LoiChan`, `LoiVanDe`, `LoiLieuDaXacNhan` (`dongBoi`, `dongLuc`, `noiDungBiTuChoi`), `LoiPrn` (`somNhatDuocDung`, `viPham`); security scheme `phien` | `Problem`; `Problem`, `Blocked`, `IssuesProblem`, `DoseAlreadyClosed` (`closedBy`, `closedAt`, `rejectedContent`), `PrnProblem` (`earliestAllowedAt`, `violations`); `session` |

### 12.4. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| trạng thái đơn `CHO_HIEU_LUC`, `HIEU_LUC`, `TAM_DUNG`, `DA_NGUNG`, `HET_HAN`; lý do tạm dừng `LENH`, `CHO_DOI_CHIEU` | `PENDING_EFFECTIVE`, `ACTIVE`, `SUSPENDED`, `DISCONTINUED`, `EXPIRED`; `ORDER`, `RECONCILIATION_PENDING` |
| loại đơn `DINH_KY`, `PRN`; nguồn thuốc `VIEN`, `GIA_DINH_GUI`; cơ sở kê `NOI_BO`, `BEN_NGOAI` | `PERIODIC`, `PRN`; `FACILITY`, `FAMILY_SENT`; `INTERNAL`, `EXTERNAL` |
| tần suất `CAC_MOC`, `MOI_N_GIO`, `NGAY_TRONG_TUAN` | `TIMED_SLOTS`, `EVERY_N_HOURS`, `DAYS_OF_WEEK` |
| hình thức căn cứ `DON_MOI`, `GIAY_RA_VIEN`, `DIEN_THOAI` | `NEW_PRESCRIPTION`, `DISCHARGE_PAPER`, `PHONE_ORDER` |
| loại vấn đề `TRUNG_HOAT_CHAT`, `XUNG_DOT_DI_UNG`, `DI_UNG_KHAC_CAN_XEM` | `DUPLICATE_ACTIVE_INGREDIENT`, `ALLERGY_CONFLICT`, `OTHER_ALLERGY_REVIEW` |
| trạng thái liều `CHUA_DEN_GIO`, `DEN_GIO`, `TRE`, `BO_LO`, `DA_DUNG`, `TU_CHOI`, `KHONG_THUC_HIEN`, `TAM_DUNG`, `MANG_THEO`, `DA_HUY` | `UPCOMING`, `DUE`, `LATE`, `MISSED`, `ADMINISTERED`, `REFUSED`, `NOT_ADMINISTERED`, `SUSPENDED`, `LEAVE_MEDICATION`, `CANCELLED` |
| căn cứ ghi liều `THUONG`, `MANG_THEO`, `GHI_THEO_BAO_LAI` | `REGULAR`, `LEAVE_MEDICATION`, `REPORTED_BY_OTHERS` |
| xem lại liều `CHO_XEM_LAI`, `CHAP_NHAN`, `CAN_DINH_CHINH` | `PENDING_REVIEW`, `ACCEPTED`, `REQUIRES_CORRECTION` |
| vi phạm PRN `KHOANG_CACH_TOI_THIEU`, `SO_LAN_24H` | `MIN_INTERVAL`, `MAX_PER_24H` |
| người ngoài `NHAN_VIEN`, `NGUOI_THAN`, `TU_DO` | `STAFF`, `FAMILY_MEMBER`, `FREE` |
| lý do phiếu `TIEP_NHAN`, `TRO_VE_BENH_VIEN`, `THEO_YEU_CAU_CHUYEN_MON`; trạng thái phiếu `MO`, `CHO_XAC_NHAN`, `DA_XAC_NHAN`, `DA_HUY` | `ADMISSION`, `HOSPITAL_RETURN`, `PROFESSIONAL_REQUEST`; `OPEN`, `PENDING_CONFIRMATION`, `CONFIRMED`, `CANCELLED` |
| nguồn dòng `DON_HIEN_CO`, `DON_RA_VIEN`, `THUOC_DANG_DUNG_TIEP_NHAN`; quyết định `TIEP_TUC`, `NGUNG`, `THAY_DOI_LIEU`, `THEM_MOI` | `CURRENT_PRESCRIPTION`, `DISCHARGE_PRESCRIPTION`, `ADMISSION_CURRENT_MEDICATION`; `CONTINUE`, `DISCONTINUE`, `CHANGE_DOSE`, `ADD_NEW` |
| tiếp nhận thuốc cung ứng `DAT`, `KHONG_DAT` | `PASSED`, `NOT_PASSED` |
| thuốc gửi `CHO_DOI_CHIEU`, `DUOC_SU_DUNG`, `CHI_GIU_HO`, `DA_DUNG_HET`, `DA_HOAN_TRA`, `DA_HUY_THEO_YEU_CAU`; căn cứ sử dụng `TOA_GIA_DINH`, `DON_CUA_VIEN` | `PENDING_RECONCILIATION`, `IN_USE`, `HOLD_ONLY`, `DEPLETED`, `RETURNED`, `DISPOSED`; `FAMILY_PRESCRIPTION`, `FACILITY_PRESCRIPTION` |
| lần giao `DANG_MANG_THEO`, `CHO_NHAN_LAI`, `DA_NHAN_LAI` | `HANDED_OVER`, `WAITING_RETURN`, `RETURNED` |
| loại bản ghi đính chính, đồng bộ `M07.KET_QUA_LIEU`, `M07.LAN_DUNG_PRN`, `M07.LAN_GIAO_THUOC`, `M07.LAN_NHAN_LAI_THUOC` | `M07.DOSE_RESULT`, `M07.PRN_ADMINISTRATION`, `M07.MEDICATION_HANDOVER`, `M07.MEDICATION_RETURN` |

### 12.5. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `DANG_CO_PHIEU_DOI_CHIEU`, `DA_CO_PHIEU_MO`, `THIEU_DONG`, `QUY_TAC_HAI_NGUOI`, `CAN_BAC_SI_KE_DON` | `RECONCILIATION_IN_PROGRESS`, `OPEN_RECONCILIATION_EXISTS`, `MISSING_ITEMS`, `TWO_PERSON_RULE`, `PRESCRIBING_DOCTOR_REQUIRED` |
| `CAN_XAC_NHAN_VAN_DE`, `LIEU_DA_XAC_NHAN`, `VUOT_GIOI_HAN_PRN` | `ISSUES_REQUIRE_CONFIRMATION`, `DOSE_ALREADY_CLOSED`, `PRN_LIMIT_EXCEEDED` |
| `LO_KHONG_HOP_LE`, `CHUA_CO_THUOC`, `HET_HAN_DUNG`, `SO_LUONG_KHAC_SO_CON`, `BAT_BUOC_TRUC_TUYEN`, `CHUYEN_KHONG_HOP_LE` | `INVALID_BATCH`, `NO_MEDICATION_AVAILABLE`, `EXPIRED_MEDICATION`, `QUANTITY_DIFFERS_FROM_REMAINING`, `REQUIRES_ONLINE`, `INVALID_TRANSITION` |
| *(sửa 2026-10-29, Q-393)* bỏ `…/kiem-ke-dieu-chinh`, `soLuongCon`, `soNgayConLai`, `bienDong`, `loThuocGuiId` của `XacNhanLieu` và thân PRN, `SO_LUONG_KHAC_SO_CON`; thêm `…/ghi-dung-het`, `…/bao-sap-het` (thân `ghiChu`), `ThuocGui.soLuongKetThuc`, `ketThucLuc`; cột của `thuoc_gia_dinh_gui`: `so_luong_ket_thuc`, `ket_thuc_luc`, `ket_thuc_boi_id`, `nguoi_nhan_tra_ten`, `quan_he_nguoi_nhan_tra`, `nguoi_chung_kien` | bỏ `…/adjust-inventory`, `remainingQuantity`, `daysRemaining`, `movements`, `familyMedicationBatchId` của `DoseResult`, `NewPrnAdministration`, `PrnAdministration`, `QUANTITY_DIFFERS_FROM_REMAINING`; thêm `…/mark-depleted`, `…/notify-running-low` (`note`), `FamilyMedicationBatch.finalQuantity`, `closedAt`; cột của `family_medication_batch`: `final_quantity`, `closed_at`, `closed_by_id`, `return_recipient_name`, `return_relationship`, `witness` |
| cảnh báo `KHOANG_KHONG_CO_LIEU`; nhắc `GHI_NHAN_LAI_THUOC_MANG_THEO` | `NO_DOSE_GAP`; `RECORD_LEAVE_MEDICATION_RETURN` |
| mã điều kiện danh mục `TRUNG_THUOC`, `THIEU_TEN`, `THIEU_HAM_LUONG`, `THIEU_DANG_BAO_CHE`, `THIEU_DUONG_DUNG`, `THIEU_DON_VI`, `THIEU_HOAT_CHAT`, `CUA_SO_KHONG_HOP_LE`, `THUOC_DA_NGUNG` | `DUPLICATE_MEDICATION`, `NAME_REQUIRED`, `CONCENTRATION_REQUIRED`, `DOSAGE_FORM_REQUIRED`, `ROUTE_REQUIRED`, `UNIT_REQUIRED`, `ACTIVE_INGREDIENT_REQUIRED`, `INVALID_WINDOW`, `MEDICATION_INACTIVE` |
| mã điều kiện đơn `THIEU_TAN_SUAT`, `THIEU_GIOI_HAN_PRN`, `LOAI_DON_KHONG_HOP_LE`, `THOI_DIEM_KHONG_HOP_LE`, `NGAY_KET_THUC_KHONG_HOP_LE`, `CAN_CU_CO_SO_KE`, `VUOT_LIEU_DON` | `FREQUENCY_REQUIRED`, `PRN_LIMITS_REQUIRED`, `INVALID_PRESCRIPTION_TYPE`, `INVALID_TIME`, `INVALID_END_DATE`, `EXTERNAL_BASIS_REQUIRED`, `EXCEEDS_PRESCRIBED_DOSE` |
| mã điều kiện liều `MA_LIEU_KHONG_HOP_LE`, `THOI_DIEM_DUNG_TUONG_LAI`, `NGHI_DUNG_TRUNG_LIEU`, `TU_XEM_LAI_BI_CHAN` | `INVALID_DOSE_KEY`, `ADMINISTERED_AT_IN_FUTURE`, `POSSIBLE_DUPLICATE_DOSE`, `SELF_REVIEW_NOT_ALLOWED` |
| mã điều kiện thuốc gửi, mang theo `BAO_BI_KHONG_NHAN_DIEN_DUOC`, `THIEU_THUOC_ID`, `DON_THUOC_KHONG_HOP_LE`, `KHAC_NGUOI_CAO_TUOI`, `KHAC_THUOC`, `LO_CHUA_DOI_CHIEU`, `KHONG_DU_THUOC`, `SO_LUONG_KHONG_HOP_LE` | `PACKAGING_NOT_RECOGNIZABLE`, `MEDICATION_REQUIRED`, `INVALID_PRESCRIPTION`, `RESIDENT_MISMATCH`, `MEDICATION_MISMATCH`, `BATCH_NOT_RECONCILED`, `INSUFFICIENT_QUANTITY`, `INVALID_QUANTITY` |
| mã vấn đề `TRUNG_HC_{donId}_{diNguyenId}`, `DI_UNG_{mucId}`, `DI_UNG_KHAC_{mucId}` | `DUPLICATE_INGREDIENT_{prescriptionId}_{allergenId}`, `ALLERGY_{itemId}`, `OTHER_ALLERGY_{itemId}` |
| `TRANG_THAI_CUOI` | `FINAL_STATE` |


### 12.20. Bảng sau gộp R-15 *(bổ sung 2026-10-30; thay các dòng bảng đã bỏ ở 12.1; cột chi tiết ở data-model 006 mục 0.1)*

| Tên logic | Mã |
| --- | --- |
| `lan_giao_nhan_thuoc` (`loai` giao, nhận lại, không mang, tiếp nhận cung ứng; dòng thuốc) | `medication_handover` (`kind` `HANDOVER`, `RETURN`, `NO_MEDICATION`, `SUPPLIED_INTAKE`; `lines`; `no_medication_key`) |
| `ket_qua_lieu` gồm lần dùng PRN | `dose_result.kind` `SCHEDULED`, `PRN` |
| hoạt chất của `don_thuoc`; dòng của `phieu_doi_chieu`; đơn của `thuoc_gia_dinh_gui` | `prescription.active_ingredient_ids`; `medication_reconciliation.lines`; `family_medication_batch.prescription_bindings` |
| loại bảng chung của 006 | `M07.MEDICATION` (danh mục); `M07.PRESCRIPTION_ISSUE`, `M07.PRESCRIPTION_BASIS` (sự kiện); `M07.DOSE_REVIEW`, `M07.ABSENCE_INTERVAL`, `M07.RECONCILIATION_CORRECTION_REQUEST` (hồ sơ chung); `M07` (trạng thái theo người); `M07.JOB.<jobKey>` (mốc đã nhắc) |


### 12.21. Gói chức năng nội bộ và khóa dòng *(2026-10-31)*

Trong `medication.internal`, danh mục thuốc là `catalog`, đơn thuốc là `prescription`, liều và PRN là `dose`, lịch thuốc và job là `schedule`, đối chiếu là `reconciliation`, thuốc gia đình gửi là `family`, giao nhận và khoảng vắng là `handover`, tiếp nhận cung ứng là `procurement`, tích hợp quản trị là `governance`. Trong mỗi chức năng giữ các tầng `service`, `domain`, `persistence`, `config` theo backend/README.md. Đây là gói nội bộ, không đổi tên API công khai.

Thời điểm khóa dòng của phiếu đối chiếu và bàn giao: trường `lockedAt`, cột `locked_at`; hai entity dùng `@DataGroup(MULTIPLE)`. Dòng đối chiếu nhúng giữ mã phiếu cha `reconciliationId`; thời điểm trong JSON chép từ dữ liệu cũ dùng ISO-8601 UTC để đọc thành `Instant`.

## 13. Feature 007 (`007-health-alert-incident`, D)

*Chủ feature điền khi dịch `contracts/api-007.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

| Tên logic | Mã |
| --- | --- |
| sự kiện `SuCoDaGhiNhan(suCoId, nctId, loai, muc, canDanhGiaLai)` | `IncidentRecorded(incidentId, residentId, type, level, needsReassessment)` |
| `ChiSoApi.ganNhatMoiLoai` (`ganNhat` là tên logic 001 dùng), `ChiSoGanNhatView` | `VitalSignApi.latestPerType`, `LatestVitalSignView` (`recordId`, `indicatorId`, `indicatorCode`, `indicatorName`, `unit`, `values`, `measuredAt`) |
| Đọc lịch sử, nhóm sinh hiệu | `VitalSignApi.withinRange`, `vitalSignIndicatorIds`; `VitalSignReadingView`; `VitalSignLevel`, `VitalSignConfirmationStatus`, `VitalSignLabel` |
| Lịch đo, liên hệ viện, thống kê chỉ số | `MeasurementScheduleApi`, `MeasurementScheduleView`, `MeasurementFrequency`; `FacilityContactApi`, `FacilityContactView`; `VitalSignStatisticsApi`, `VitalSignReportView` |
| Chỉ số đã xác nhận, đã đính chính; lịch đo đã đổi; nhắc thiếu ngưỡng cá nhân | `VitalSignConfirmed`, `VitalSignCorrected`, `MeasurementScheduleChanged`, `MissingPersonalThresholdReminder` (gói `health.api.events`) |
| Bản ghi chỉ số, thành phần gộp JSON, danh mục chỉ số M06 | `vital_sign_record`, `VitalSignRecord`, `VitalSignComponent`; `HealthCatalogs.VITAL_SIGN` (`M06.VITAL_SIGN`), `VitalSignAttributes(unit, vitalSign)` |
| Danh mục quy tắc xu hướng, loại diễn biến trên bảng chung (007 T005) | `M05.TREND_RULE`, `M05.PROGRESSION_TYPE` trong `catalog_item`; `attributes.level`, `parameterCode`, `group`, `resuscitation`; trạng thái `ACTIVE`/`INACTIVE` của quy tắc là bật/tắt |
| Loại sự cố: Ngã; Đi lạc hoặc không trở về (T005, mục 9.3/BR-M04-17) | `M05.INCIDENT_TYPE`: `FALL`, `MISSING_OR_NOT_RETURNED`; `attributes.defaultLevel`, `nonHealth`, `allowsNoResident` là mức mặc định, dấu không thuộc sức khỏe, cho phép không gắn người cao tuổi |
| Sụt cân, bỏ lỡ lần đo, mất ngủ kéo dài, từ chối cùng thuốc | `WEIGHT_LOSS`, `MISSED_MEASUREMENT`, `PROLONGED_INSOMNIA`, `MEDICATION_REFUSAL`; mức `LOW`, `MEDIUM`, `CRITICAL` là Nhẹ, Trung bình, Khẩn cấp |
| Ép tim, hỗ trợ thông khí, sốc điện, đặt nội khí quản, dùng thuốc cấp cứu | `CHEST_COMPRESSION`, `VENTILATION_SUPPORT`, `DEFIBRILLATION`, `INTUBATION`, `EMERGENCY_MEDICATION` |
| Gọi hỗ trợ, gọi cấp cứu, báo cơ quan y tế; nguồn đã xử lý, chuyển viện bị từ chối, đã tìm thấy, đề nghị hủy | `CALL_SUPPORT`, `CALL_EMERGENCY`, `REPORT_HEALTH_AUTHORITY`; `SOURCE_RESOLVED`, `TRANSFER_REFUSED`, `FOUND`, `CANCELLATION_REQUESTED` |
| Nhóm diễn biến: đánh giá, hành động, thông báo, theo dõi, hệ thống | `EVALUATION`, `ACTION`, `NOTIFICATION`, `MONITORING`, `SYSTEM` |
| Giao diện, DTO người thân và sự kiện công khai M05 (T004) | `IncidentCommandApi`, `AlertCommandApi`, `IncidentApi`, `AlertApi`, `IsolationApi`, `FamilyPortalHealthApi`, `IncidentStatisticsApi`; `FamilyIncidentView`, `FamilyIncidentGeneralPart`, `FamilyIncidentHealthPart`; sự kiện ở `incident.api.events`, chi tiết module-interfaces 007 mục 2.7–2.8 |
| Handler chỉ số ngoại tuyến; cờ đồng bộ sau khi mất quyền; tác vụ xác nhận quá hạn đo lại (T014) | `VitalSignOfflineHandler`, `VitalSignOfflineContent`; `synced_after_permission_loss`; `OverdueRemeasurementTask` (nhịp `MINUTE`) |
| Nguồn đọc thật chỉ số M06 | `DefaultVitalSignApi`, `VitalSignRecordStore`; loại đính chính `M06.VITAL_SIGN_RECORD` |
| Nguồn thống kê lần đo cho 016, giữ mức lúc đo | `DefaultVitalSignStatistics`; `VitalSignStatisticsApi.readings` |
| Dừng nhắc gọi khi hạ sự cố khỏi Khẩn cấp; đã có kết quả xác nhận lại | `CallReminderStopRequested(sourceRecordType, sourceRecordId)`; `WishReconfirmationCompleted(alertId)` |
| Yêu cầu tạo, đóng nguồn, ghi diễn biến cảnh báo cho 011/014 | `AlertRequested`, `AlertSourceCloseRequested`, `AlertProgressionRequested` |
| Thêm người tiếp xúc, tạo/gỡ khoanh vùng, đổi dấu nghi nhiễm | `ContactAdded`, `IsolationCreated`, `IsolationLifted`, `InfectionSuspicionChanged` |
| Cảnh báo đã tạo, nâng mức, leo thang; dấu nguy kịch đã ghi; sự cố ngã | `AlertCreated`, `AlertLevelRaised`, `AlertEscalated`, `CriticalFlagRecorded`, `FallIncidentRecorded` |
| `SoNguong`, bốn cận ngưỡng; test xếp mức SC-001 | `ThresholdComparator`, `ThresholdBand`; `ThresholdComparatorTest`, `SC_001_VitalSignLevelTest` |
| Trạng thái chỉ số/theo dõi theo người sau gộp R-15 | `ResidentVitalSignState` (`M06.RESIDENT_VITAL_SIGNS`), `ResidentMonitoringState` (`M05.RESIDENT_MONITORING`) trong `resident_module_state` |
| Khóa tuần tự lệnh M06/M05 theo người | `VitalSignLock`, `MonitoringLock`; `VitalSignStateMutex`, `MonitoringStateMutex` chỉ bảo vệ lần tạo dòng đầu bằng MySQL advisory lock |
| Dịch vụ lệnh danh mục M05 (T006 một phần) | `IncidentCatalogService`, `IncidentCatalogs`; `TrendRuleAttributes`, `IncidentTypeAttributes`, `ProgressionTypeAttributes`; `IncidentLevel` (`LOW`, `MEDIUM`, `CRITICAL`), `ProgressionGroup` |
| Nội dung lệnh/kết quả danh mục nội bộ M05 | `TrendRuleUpdate` (bật/tắt, mức; `Reason` truyền riêng vào lệnh), `TrendRuleView`, `IncidentTypeInput`, `IncidentTypeView`, `ProgressionTypeInput`, `ProgressionTypeView` ở `incident.internal.alert.domain` |
| Nhật ký danh mục M05; nội dung danh mục không hợp lệ | `IncidentCatalogActions`: `TREND_RULE_UPDATED`, `INCIDENT_TYPE_CREATED`, `INCIDENT_TYPE_UPDATED`, `PROGRESSION_TYPE_CREATED`; `INVALID_CATALOG_INPUT` (422) |
| Dịch vụ/kiểu danh mục chỉ số và thành phần M06 | `VitalSignCatalogService`, `VitalSignCatalogInput`, `VitalSignCatalogView`, `VitalSignComponentDefinition`, `VitalSignComponentAttributes`; `HealthCatalogs.VITAL_SIGN_COMPONENT` (`M06.VITAL_SIGN_COMPONENT`), `parent_id`, `attributes.decimalPlaces` |
| Nhật ký tạo/sửa/ngừng danh mục chỉ số | `VitalSignCatalogActions`: `VITAL_SIGN_TYPE_CREATED`, `VITAL_SIGN_TYPE_UPDATED`, `VITAL_SIGN_TYPE_DEACTIVATED` |
| Loại sự cố nghi lây nhiễm, cơ sở vật chất, đồ gửi thất lạc/hư hỏng; bảo vệ mã đặc biệt | `SUSPECTED_INFECTION`, `FACILITY`, `BELONGINGS_LOST`, `BELONGINGS_DAMAGED`; `IncidentCatalogs.PROTECTED_TYPES`, `PROTECTED_CATALOG_CODE` (409) |
| `NguoiThanKhanCapPort` (`nguoiLienHeChinh`, `nguoiDaiDien`, `luotThamTrongKhoang`) | `EmergencyFamilyPort` (`primaryContact`, `representatives`, `visitsInRange`); `EmergencyFamilyContactView`, `VisitTraceView`, `VisitTracePersonView` |
| `DiemDanhTruyVetPort.cungThamGia`, `BuoiCungThamGiaView` | `AttendanceTracePort.coAttendance`, `ActivityAttendanceTraceView` |
| Dòng theo dõi người cao tuổi trong danh sách tiếp xúc | `ContactFollowUpStore`, `M05.CONTACT_PERSON` |
| Sự cố sau gộp R-15; nguồn đọc dấu nghi nhiễm | bảng `incident`, `IncidentStore.suspectedAt`; `NEW`, `IN_PROGRESS`, `MONITORING`, `CLOSED`, `CANCELLED` |
| Nguồn phân bổ, lệnh và dữ liệu khoanh vùng | `IncidentAllocationStatus`, `ContainmentService`, `ContainmentNotices`, `ContainmentContent`, `ContactListContent`, `ContactFollowUpContent` |
| Lệnh đặt/gỡ vùng, gắn sự cố và nhật ký | `NewIsolationRequest`, `IsolationReasonRequest`, `AttachIsolationIncidentRequest`, `IsolationResponse`; `CONTAINMENT_CREATED`, `CONTAINMENT_LIFTED`, `CONTAINMENT_INCIDENT_ATTACHED` |
| Lập danh sách tiếp xúc Nháp từ sự cố nghi nhiễm; nhật ký | `ContactListService`, `ContactListController`, `SuspectedIncident`; `ContactListActions.CONTACT_LIST_CREATED` |
| Tiếp nhận xử lý, ghi nhận sự cố, ghi diễn biến; nhật ký (T035) | `IncidentService.receive`, `IncidentRecordService`, `IncidentRecordController`, `IncidentProgressionService`, `IncidentProgressionController`, `DefaultIncidentCommandApi`; `RecordIncidentRequest`, `AddProgressionRequest`, `ActivationResultResponse`; `NewIncident`, `RecordedIncident`, `ProgressionContent`; `IncidentActions.INCIDENT_RECEIVED`, `INCIDENT_RECORDED`, `INCIDENT_PROGRESSION_ADDED` |
| Bản ghi chung `dien_bien_su_co`, `xac_nhan_doi_chieu` (R-15 → `su_kien_chung`) | loại `record_event`: `M05.INCIDENT_PROGRESSION`, `M05.WISH_CHECK` (cha `M05.INCIDENT`) |
| Nhóm người nhận khi ghi nhận sự cố; hạn tiếp nhận dùng chung | `IncidentNotices`; `AcknowledgementDeadlines` |
| Tiếp nhận cảnh báo; nhật ký (bảng FR-029, T028 một phần) | `AlertCommandService.receive`, `AlertController`, `DefaultAlertCommandApi`; `AlertReceiveState`; `AlertActions.ALERT_RECEIVED` |
| Lệnh vòng đời cảnh báo sau Tiếp nhận (bảng FR-029, T028): Bắt đầu xử lý, Nâng/Hạ mức, Đóng, Giao người phụ trách; nhật ký | `AlertHandlingService` (`startHandling`, `changeLevel`, `close`, `assign`); `AlertActions.ALERT_HANDLING_STARTED`, `ALERT_LEVEL_CHANGED`, `ALERT_CLOSED`, `ALERT_ASSIGNED`; request `AssignAlertRequest` (`staffId` là mã nhân viên, `note`), `ChangeAlertLevelRequest`, `CloseAlertRequest` |
| Tạo cảnh báo thủ công (FR-026 (c)), khóa `THU_CONG:<loại>` | `ManualAlertService.create` (`Created(alert, merged)`), `CreateAlertRequest`; `AlertActions.ALERT_CREATED_MANUALLY` |
| Đọc cảnh báo cho màn (danh sách theo phạm vi, chi tiết, `lenhDuocPhep`) | `AlertQueryService` (`list`, `detail`), `AlertDetails` (`Source`), `AlertResponse` (`AlertSource`); lệnh được phép `receive`, `assign`, `start-handling`, `change-level`, `close` |
| `lich_su_canh_bao` (R-15 → `su_kien_chung`): leo thang, tiếp nhận, xử lý, đổi mức, giao, đóng, chuyển người phụ trách | loại `record_event` `M05.ALERT_HISTORY` (cha `M05.ALERT`), lớp `AlertHistory` |
| `ChuoiLeoThang`, `DemLap24h` (research S-08) | `AlertEscalationChain`, `RepeatWithin24HoursCounter` |
| `ViecTheoPhutM05`: leo thang một cấp, nhắc leo thang tối đa; tạm nhận bàn giao (Q-94) | `AlertEscalationService`, `AlertEscalationTask` (`ALERT_ESCALATION`); `AlertProvisionalTakeoverService`, `ProvisionalHandoverTask` (`PROVISIONAL_HANDOVER`), dấu `reminder_marker` kind `M05.PROVISIONAL_HANDOVER` |
| Thông báo leo thang, nhắc tối đa, lặp lại, giao | `AlertNotices`; event type `ALERT_ESCALATED`, `ALERT_MAXIMUM_ESCALATION_REMINDER`, `ALERT_REPEATED`, `ALERT_ASSIGNED`, `HANDOVER_ASSIGNEE_CHANGED`, `HANDOVER_PROVISIONAL_ASSIGNEE` |
| Tầng, người phụ trách ban đầu, người tạm nhận (FR-031, FR-037, BR-M05-19) | `AlertResponsibility` (`floorOf`, `initialAssignee`, `provisionalAssignee`, `staffOfAccount`) |
| `CanhBaoBanGiaoApi` bản thật; mục bàn giao; dấu `tam_nhan_ban_giao` | `DefaultAlertHandoverApi`, `AlertAssigneeTransfer`, `HandoverItemStore`, `HandoverItem` (nhóm `ALERT`, `INCIDENT`); cột `handover_acknowledged` (bảng `alert`, `incident`) |
| `CanhBaoDocPort` (005) bản thật; trạng thái cuối dừng leo thang (FR-038) | `DefaultAlertReadPort` (nguồn công việc `M04.TASK`); `AlertFinalStatusListener` |
| Khoảng theo dõi sau ngã `khoang_theo_doi_sau_nga` (R-15 → hồ sơ chung), trạng thái `DANG_THEO_DOI`, `DUOC_THAY`; tham số CFG-M05-06 | loại `simple_record` `M05.POST_FALL_MONITORING`, trạng thái `ACTIVE`, `REPLACED`; `FallMonitoringStore`, `FallMonitoringContent`, `FallMonitoringSetting` (`interval`, `duration`, `anchor`) |
| Chuyển theo dõi, mở lại, đóng, tham gia xử lý sự cố; nhật ký; người tham gia (`nguoi_xu_ly_su_co` vai trò `THAM_GIA`, R-15 → `su_kien_chung`) | `IncidentHandlingService` (`moveToMonitoring`, `reopen`, `close`, `join`), `IncidentHandlingController`, `CloseIncidentRequest` (`resolution`, `infectionOutcome`), `IncidentReasonRequest`; `INCIDENT_MOVED_TO_MONITORING`, `INCIDENT_REOPENED`, `INCIDENT_CLOSED`, `INCIDENT_HANDLING_JOINED`; loại `record_event` `M05.INCIDENT_HANDLER` (`role = JOINED`); `ContainmentStore.activeOnlyFor` |
| Giao người xử lý sự cố; lịch sử phân công `phan_cong_su_co` (R-15 → `su_kien_chung`); nhật ký | `IncidentAssignmentService.assign`, `IncidentAssignmentController`, `AssignIncidentHandlerRequest` (`assigneeId`, `note`); loại `record_event` `M05.INCIDENT_ASSIGNMENT` (`assignedBy`, `assignee`, `note`, `statusAtAssignment`); `INCIDENT_HANDLER_ASSIGNED`; nhóm nhận `Staff(staffId, "INCIDENT_ASSIGNED")` |
| Đọc sự cố (danh sách theo phạm vi, hồ sơ, `allowedCommands`) (T037) | `IncidentQueryService` (`list`, `detail`), `IncidentQueryController`; `IncidentView`, `IncidentDetails`; `IncidentStore.view`, `search`; `IncidentProgressionStore.list`, `handlerAccountIds` |
| Chuyển sự cố từ cảnh báo (bảng FR-029; `canh_bao.su_co_id`, `su_co.canh_bao_nguon_id`) | `AlertConversionService.convert`, `AlertConversionController`, `ConvertToIncidentRequest` (schema `IncidentFromAlert`); `AlertStore.convert`; `NewIncident.sourceAlertId`; nhật ký `ALERT_CONVERTED_TO_INCIDENT` |
| `DinhChinhDuocProvider` `M05.SU_CO`, `M05.DIEN_BIEN_SU_CO` (T036); trigger mở Đã đóng → Đã hủy và đổi loại (V007_10) | `IncidentCorrectionProviders.IncidentProvider` (`M05.INCIDENT`, nội dung đúng `{incidentTypeId}`), `IncidentCorrectionProviders.ProgressionProvider` (`M05.INCIDENT_PROGRESSION`, nội dung đúng `{content}`); `IncidentStore.cancel`, `changeType`; `V007_10__incident_correction_trigger.sql` |
| Tác động còn hiệu lực khi Hủy ghi nhận, đổi loại sự cố (FR-046a; `TacDongConHieuLuc`) | `IncidentCorrectionEffects` (`voided`, `typeChanged`, `remainingImpacts`), `IncidentNotices.effectiveImpacts`, sự kiện `EffectiveImpactsRemain`; `ContainmentStore.activeFor`; `IncidentRecordService.startInfectionSuspicion` |
| `doi_muc_su_co` (R-15 → `su_kien_chung`): `muc_cu`, `muc_moi`, `ly_do` | `record_event` loại `M05.INCIDENT_LEVEL_CHANGE` (`from`, `to`, `reason`); `IncidentProgressionStore.recordLevelChange`, `levelChanges`; `Incident.levelChanges` |
| Đối tượng kiểm quyền của lệnh `@BusinessCommand` sự cố, cảnh báo | `IncidentHandlingService.target(id)`, `IncidentService.target(id)`, `AlertHandlingService.target(id)` |
| Phiên bản ngưỡng (`nguong_phien_ban` → `monitoring_setting` kind THRESHOLD): `CO_SO`/`CA_NHAN`, `CHO_HIEU_LUC`/`HIEU_LUC`/`HET_HIEU_LUC`/`DA_HUY`; `KiemTraNguong`; lệnh thiết lập, hủy, ngừng; nhắc thiếu ngưỡng cá nhân | `FACILITY`/`PERSONAL`, `PENDING`/`EFFECTIVE`/`EXPIRED`/`CANCELLED`; `ThresholdRules.violations`; `ThresholdService` (`set`, `cancel`, `stop`, `activateDue`), `ThresholdVersionStore`, `ThresholdIndicators`, `NewThreshold`, `ThresholdVersion`, `ThresholdIndicator`; `ThresholdActivationTask` (M06 phút), `MissingThresholdReminders` (`missing`, `remind`, `allowReminderAgain`), `MissingThresholdReminderTask` (M06 ngày), `ThresholdFinalStatusListener`; `THRESHOLD_SET`, `THRESHOLD_CANCELLED`, `THRESHOLD_STOPPED`; lỗi `INVALID_THRESHOLD` (kèm `violations`), `VALID_RANGE_MISSING` |
| Lịch đo (`lich_do` → `monitoring_setting` kind `MEASUREMENT_SCHEDULE`, V007_09): `MOC_GIO`/`MOI_N_GIO`/`THEO_THU`, `CHO_HIEU_LUC`/`HIEU_LUC`/`DA_NGUNG`/`HET_HAN`, `thay_the_cho`, `ngung_luc`; `ChuoiMocDo`; mức quan trọng `BINH_THUONG`/`QUAN_TRONG`/`BAT_BUOC` | `CLOCK_TIMES`/`EVERY_N_HOURS`/`WEEKDAYS`, `PENDING`/`EFFECTIVE`/`STOPPED`/`EXPIRED`, cột `replaces_id`, `stopped_at`; `MeasurementTimeline.occurrences`; `NORMAL`/`IMPORTANT`/`MANDATORY`; `MeasurementScheduleService` (`create`, `change`, `stop`, `transitionDue`, `stopForFinalStatus`), `MeasurementScheduleStore`, `ScheduleFrequency`, `NewMeasurementSchedule`, `MeasurementSchedule` (`basisAt`), `StoredScheduleContent`, `MeasurementScheduleTask` (M06 phút), `MeasurementScheduleFinalStatusListener`, `DefaultMeasurementScheduleApi`; `MEASUREMENT_SCHEDULE_CREATED`, `_CHANGED`, `_STOPPED` |
| Màn và route ngưỡng, lịch đo (T024) | `ThresholdController`, `MeasurementScheduleController`; `ThresholdRequest` (`ThresholdComponentRequest`), `MeasurementScheduleRequest` (`FrequencyRequest`), `ChangeMeasurementScheduleRequest`, `HealthReasonRequest`; `ThresholdVersionResponse` (`ThresholdComponentResponse`), `MeasurementScheduleResponse` (`ScheduleFrequencyResponse`); `ThresholdListing`; `ThresholdService.list`, `missingPersonal`; `MeasurementScheduleService.list`, `occurrences` |
| Phạm vi và khoảng hiệu lực khoanh vùng (Q-69, BR-M05-11/12) | `incident.internal.infection.domain.ContainmentScope`, `ContainmentPeriod` |
| `NhatKyThongBaoPort.theoNguon`, `DaThongBaoView` | `NotificationLogPort.bySource`, `IncidentNotificationView` (`recipientType`, `recipientId`, `channel`, `sentAt`, `viewedAt`, `callReminders`); lượt nhắc gọi `CallReminderSummary`, kết quả gọi `CallOutcome` (Q-388) |
| gói chức năng M06: danh mục, lịch đo, chỉ số, ngưỡng, giấy phép, khám, tham gia trạng thái | `health.internal.catalog`, `schedule`, `measurement`, `threshold`, `license`, `examination`, `governance` (cùng dưới `health.internal`) |
| Lệnh ghi chỉ số, đo lại, dịch vụ đo chỉ số, phân giải ngưỡng áp dụng | `RecordVitalSignCommand`, `RemeasureVitalSignCommand`, `VitalSignMeasurementService`, `ApplicableThresholdResolver`, `DefaultApplicableThresholdResolver` |
| gói chức năng M05: cảnh báo, xu hướng, sự cố, khẩn cấp, nguy kịch, ngã, lây nhiễm, tham gia trạng thái | `incident.internal.alert`, `trend`, `incident`, `emergency`, `critical`, `fall`, `infection`, `governance` (cùng dưới `incident.internal`) |
| Lịch sử và ghi chỉ số theo người cao tuổi (GET, POST); đo lại (POST) | `/residents/{residentId}/vital-signs`; `/vital-sign-records/{id}/remeasure`; client `getResidentsByResidentIdVitalSigns`, `postResidentsByResidentIdVitalSigns`, `postVitalSignRecordsByIdRemeasure` |
| Màn web danh mục sức khỏe (T009) | `HealthCatalogPage` (`frontend/src/features/health-catalog/HealthCatalogPage.jsx`) |


| Nguồn đọc giấy phép cơ sở, số liên hệ viện (S-16) | `health.internal.license.service.FacilitySources`; `M06.FACILITY_LICENSE`, `M06.FACILITY_CONTACT` qua `CatalogApi` |
| Bản dịch đủ api-007 (T062, duyệt 2026-11-03): đường dẫn, tag, tham số, schema, enum, mã lỗi | bảng ở `specs/007-health-alert-incident/contracts/api-translation.md` mục 1 → 6 (`donViDuaDi` → `transferUnit` như 001) |
| loại cảnh báo nút hỗ trợ `NV_YEU_CAU_HO_TRO` | `STAFF_ASSIST_REQUEST` (`V007_07__alert_staff_assist_type.sql`) |
| schema lỗi của 007 trong mã | `M06Problem` (`health.web.response.M06ProblemBody`), `M05Problem` (`incident.web.response.M05ProblemBody`) |
| nhóm dữ liệu `alert`, `monitoring_setting` | `BUSINESS` (`V007_06__alert_monitoring_data_group.sql`) |

## 14. Feature 008 (`008-staff-shift-handover`, E)

Bản dịch hợp đồng 008 cập nhật 2026-10-23, **đã được A duyệt theo xác nhận của E trong phiên làm việc, cho phép triển khai Phase 1**. Chỉ đổi tên; giữ nguyên kiểu, trường bắt buộc, ràng buộc, quyền và mã truy vết nghiệp vụ.

### 14.1. Tag và đường dẫn API

| Tên logic | Mã |
| --- | --- |
| `tag nhan-vien` | `staff` |
| `tag chung-chi` | `credentials` |
| `tag lich-ca` | `shift-schedules` |
| `tag phan-cong` | `assignments` |
| `tag ty-le` | `staffing-ratios` |
| `tag ban-giao` | `handovers` |
| `/nhan-vien, /nhan-vien/{id}` | `/staff, /staff/{id}` |
| `/nhan-vien/{id}/cho-nghi-viec` | `/staff/{id}/terminate-employment` |
| `/nhan-vien/{id}/nhan-lai-lam-viec` | `/staff/{id}/rehire` |
| `/nhan-vien/{id}/doi-bo-phan` | `/staff/{id}/change-department` |
| `/staff/import/mau` | `/staff/import/template` |
| `/staff/import/xem-truoc` | `/staff/import/preview` |
| `/staff/import/xac-nhan` | `/staff/import/confirm` |
| `/bo-phan` | `/departments` |
| `/bo-phan/{id}/chi-dinh-phu-trach` | `/departments/{id}/assign-lead` |
| `/loai-chung-chi` | `/credential-types` |
| `/pham-vi-hanh-nghe` | `/practice-scopes` |
| `/staff/{id}/chung-chi` | `/staff/{id}/credentials` |
| `/chung-chi/{id}/gia-han` | `/credentials/{id}/renew` |
| `/chung-chi/{id}/thu-hoi` | `/credentials/{id}/revoke` |
| `/chung-chi/{id}/sua-sai` | `/credentials/{id}/correct` |
| `/mau-ca` | `/shift-templates` |
| `/mau-ca/{id}` | `/shift-templates/{id}` |
| `/lich-ca` | `/shift-schedules` |
| `/lich-ca/{id}/ca` | `/shift-schedules/{id}/shifts` |
| `/ca/{id}` | `/shifts/{id}` |
| `/lich-ca/{id}/xem-truoc-cong-bo` | `/shift-schedules/{id}/publication-preview` |
| `/lich-ca/{id}/cong-bo` | `/shift-schedules/{id}/publish` |
| `/ca/{id}/bo-sung-nhan-vien` | `/shifts/{id}/add-staff` |
| `/ca/{id}/chuyen-nguoi-phu-trach` | `/shifts/{id}/change-lead` |
| `/ca/{id}/ghi-nhan-vang` | `/shifts/{id}/record-absence` |
| `/ca/{id}/huy` | `/shifts/{id}/cancel` |
| `/toi/ca` | `/me/shifts` |
| `/bac-si-truc` | `/on-duty-doctors` |
| `/giao-truong-tang` | `/floor-manager-assignments` |
| `/giao-truong-tang/{id}/ket-thuc` | `/floor-manager-assignments/{id}/end` |
| `/ca/{id}/phan-cong` | `/shifts/{id}/assignments` |
| `/phan-cong/{id}` | `/assignments/{id}` |
| `/phan-cong/{id}/huy` | `/assignments/{id}/cancel` |
| `/phan-cong/{id}/ket-thuc` | `/assignments/{id}/end` |
| `/ca/{id}/sao-chep-phan-cong` | `/shifts/{id}/copy-assignments` |
| `/ca/{id}/ty-le-phuc-vu` | `/shifts/{id}/staffing-ratios` |
| `/ca/{id}/ban-giao` | `/shifts/{id}/handovers` |
| `/ban-giao/{id}` | `/handovers/{id}` |
| `/ban-giao/{id}/ban-ghi-nguon` | `/handovers/{id}/source-records` |
| `/ban-giao/{id}/muc` | `/handovers/{id}/items` |
| `/ban-giao/{id}/muc/{mucId}` | `/handovers/{id}/items/{itemId}` |
| `/ban-giao/{id}/nhan-dinh` | `/handovers/{id}/assessments` |
| `/ban-giao/{id}/hoan-tat` | `/handovers/{id}/complete` |
| `/ban-giao/{id}/xac-nhan` | `/handovers/{id}/confirm` |
| `/ban-giao/{id}/y-kien` | `/handovers/{id}/comments` |
| `/ban-giao/{id}/bo-sung-gui-lai` | `/handovers/{id}/supplement-and-resubmit` |
| `/nhat-ky-can-cu-nhiem-vu` | `/duty-basis-audit-logs` |

### 14.2. Schema, response và phiên đăng nhập

| Tên logic | Mã |
| --- | --- |
| `NhanVien` | `StaffProfile` |
| `LyDo (schema)` | `Reason` |
| `LyDo (request body)` | `ReasonBody` |
| `Loi` | `Problem` |
| `CanhBao` | `Warning` |
| `KetQuaVoiCanhBao` | `ResultWithWarnings` |
| `DongImport` | `StaffImportRow` |
| `LoiDongImport` | `StaffImportRowError` |
| `KetQuaImport` | `StaffImportPreview` |
| `KetQuaXacNhanImport` | `StaffImportResult` |
| `BoPhan` | `Department` |
| `LoaiChungChi` | `CredentialType` |
| `ChungChi` | `Credential` |
| `MauCa` | `ShiftTemplate` |
| `LichCa` | `ShiftSchedule` |
| `CaMoi` | `NewShift` |
| `Ca` | `Shift` |
| `XemTruocCongBo` | `SchedulePublicationPreview` |
| `GiaoTruongTang` | `FloorManagerAssignment` |
| `PhanCongMoi` | `NewAssignment` |
| `PhanCong` | `Assignment` |
| `LanTinhTyLe` | `StaffingRatioCalculation` |
| `BanGhiNguon` | `HandoverSourceRecord` |
| `MucBanGiao` | `HandoverItem` |
| `BanGiao` | `Handover` |
| `Loi (response)` | `Problem` |
| `LoiChan (response)` | `BlockingProblem` |
| `phien (security scheme)` | `session` |

### 14.3. Trường JSON và tham số

Các trường dùng cùng tên logic giữ cùng bản dịch. `dong` trong kết quả xem trước là danh sách; trong từng dòng và lỗi dòng là số dòng.

| Tên logic | Mã |
| --- | --- |
| `hoTen` | `fullName` |
| `chucDanh` | `position` |
| `chuyenMon` | `specialty` |
| `ngayBatDau` | `startDate` |
| `dienThoai` | `phoneNumber` |
| `taiKhoanId` | `accountId` |
| `boPhanId` | `departmentId` |
| `lyDo` | `reason` |
| `hieuLuc của Cho nghỉ việc (date-time)` | `effectiveAt` |
| `nguoiThayId của Cho nghỉ việc, Đổi bộ phận` | `replacementStaffId` |
| `nhanVienId` | `staffId` |
| `hieuLucTu` | `effectiveFrom` |
| `thang` | `month` |
| `phamViId` | `scopeId` |
| `loaiPhamVi` | `scopeType` |
| `loaiBoPhanCong` | `assignmentIdsToRemove` |
| `xacNhanCanhBao` | `confirmWarnings` |
| `tu` | `from` |
| `den` | `to` |
| `thoiDiem` | `at` |
| `tangKhuVucId` | `floorAreaId` |
| `caNguonId` | `sourceShiftId` |
| `daSaoChep` | `copiedIds` |
| `khongSaoChep` | `notCopied` |
| `phanCongNguonId` | `sourceAssignmentId` |
| `sauKhiLap` | `afterPreparation` |
| `chung` | `general` |
| `theoNguoiCaoTuoi` | `byResident` |
| `nctId` | `residentId` |
| `noiDung` | `content` |
| `phanHoi` | `replies` |
| `yKienId` | `commentId` |
| `mucBoSung` | `additionalItems` |
| `mucId` | `itemId` |
| `maLoi` | `errorCode` |
| `dieuKienChuaThoa` | `unmetConditions` |
| `ma` | `code` |
| `moTa` | `description` |
| `canCu` | `basis` |
| `canhBao` | `warnings` |
| `chiTiet` | `details` |
| `nguon` | `source` |
| `lanImportId` | `importBatchId` |
| `cot` | `column` |
| `trungVoiNhanVienId` | `duplicateStaffId` |
| `trungVoiDong` | `duplicateRowNumber` |
| `tenFile` | `fileName` |
| `soDong` | `rowCount` |
| `loi` | `errors` |
| `xacNhanDuoc` | `canConfirm` |
| `soHoSoDaTao` | `createdProfileCount` |
| `nhanVienIds` | `staffIds` |
| `ten` | `name` |
| `phuTrachId` | `leadStaffId` |
| `hieuLuc` | `active` |
| `nhom` | `group` |
| `coThoiHan` | `hasExpiry` |
| `batBuocVoiVaiTro` | `requiredForRoles` |
| `coPhamVi` | `hasPracticeScopes` |
| `loaiId` | `credentialTypeId` |
| `so` | `credentialNumber` |
| `donViCap` | `issuingAuthority` |
| `phamVi` | `practiceScopes` |
| `ngayCap` | `issueDate` |
| `ngayHetHan` | `expiryDate` |
| `tepScanId` | `scanFileId` |
| `thayTheCho` | `replacesId` |
| `thuHoiTu` | `revokedFrom` |
| `tinhTrang` | `validityStatus` |
| `gioBatDau` | `startTime` |
| `gioKetThuc` | `endTime` |
| `yeuCauBanGiao` | `handoverRequired` |
| `trangThai` | `status` |
| `ca` | `shifts` |
| `mauCaId` | `shiftTemplateId` |
| `ngay` | `date` |
| `batDau` | `startsAt` |
| `ketThuc` | `endsAt` |
| `nhanVien` | `staff` |
| `bacSiTruc` | `onDutyDoctor` |
| `nguoiPhuTrachId` | `leadStaffId` |
| `nguoiPhuTrachMoiId` | `replacementLeadStaffId` |
| `thieuNguoiPhuTrach` | `missingLead` |
| `vang` | `absent` |
| `nghiCoDuyet` | `approvedLeave` |
| `tyLePhucVu` | `staffingRatio` |
| `lenhDuocPhep` | `allowedCommands` |
| `phanCongKhongDat` | `invalidAssignments` |
| `phanCongId` | `assignmentId` |
| `loaiDoiTuong` | `targetType` |
| `doiTuongId` | `targetId` |
| `loaiCongViecId` | `taskTypeId` |
| `vaiTro` | `role` |
| `chinhHoTro` | `assignmentKind` |
| `dieuDuongPhuTrach` | `assignedNurse` |
| `ketThucThucTe` | `actualEndAt` |
| `suKien` | `event` |
| `tongTrongSo` | `totalWeight` |
| `soNhanVien` | `staffCount` |
| `tyLe` | `ratio` |
| `nguong` | `threshold` |
| `dat` | `meetsThreshold` |
| `nguonLoai` | `sourceType` |
| `nguonId` | `sourceId` |
| `phongId` | `roomId` |
| `trangThaiHienTai` | `currentStatus` |
| `nghiemTrong` | `critical` |
| `daCoMuc` | `hasItem` |
| `chuaCoDuLieu` | `dataUnavailable` |
| `ghiChu` | `note` |
| `daXuLy` | `resolved` |
| `phan` | `section` |
| `trangThaiLucChot` | `statusAtFinalization` |
| `nguoiGhiChu` | `noteAuthor` |
| `caId` | `shiftId` |
| `caSauId` | `nextShiftId` |
| `nguoiBanGiaoId` | `handoverStaffId` |
| `chotLuc` | `finalizedAt` |
| `xacNhanLuc` | `confirmedAt` |
| `nhanDinhChung` | `generalAssessment` |
| `nhanDinhTheoNguoi` | `residentAssessments` |
| `muc` | `items` |
| `yKien` | `comments` |
| `nguoiGui` | `sender` |
| `truongBiAn` | `hiddenFields` |
| `dong trong DongImport, LoiDongImport` | `rowNumber` |
| `dong trong KetQuaImport` | `rows` |

### 14.4. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| `DANG_LAM_VIEC` | `WORKING` |
| `NGHI_VIEC` | `RESIGNED` |
| `GIAY_PHEP_HANH_NGHE` | `PROFESSIONAL_LICENSE` |
| `DAO_TAO` | `TRAINING` |
| `CON_HIEU_LUC` | `VALID` |
| `HET_HAN` | `EXPIRED` |
| `DA_THU_HOI` | `REVOKED` |
| `DA_THAY_THE` | `REPLACED` |
| `CHUA_TOI_NGAY_CAP` | `NOT_YET_ISSUED` |
| `TANG` | `FLOOR` |
| `KHU_VUC` | `AREA` |
| `TOAN_VIEN` | `FACILITY` |
| `NHAP` | `DRAFT` |
| `DA_CONG_BO` | `PUBLISHED` |
| `DANG_DIEN_RA` | `IN_PROGRESS` |
| `CHO_BAN_GIAO` | `AWAITING_HANDOVER` |
| `DA_DONG` | `CLOSED` |
| `DA_HUY` | `CANCELLED` |
| `TUONG_LAI` | `SCHEDULED` |
| `HIEU_LUC` | `ACTIVE` |
| `DA_KET_THUC` | `ENDED` |
| `LAP_TAY` | `MANUAL` |
| `TAO_TAY` | `MANUAL` |
| `SAO_CHEP` | `COPIED` |
| `DOI_CA_015` | `SHIFT_SWAP_015` |
| `CHINH` | `PRIMARY` |
| `HO_TRO` | `SUPPORT` |
| `PHONG` | `ROOM` |
| `NGUOI_CAO_TUOI` | `RESIDENT` |
| `CONG_VIEC` | `TASK` |
| `LIEU` | `DOSE` |
| `CHI_SO` | `VITAL_SIGN` |
| `CANH_BAO` | `ALERT` |
| `SU_CO` | `INCIDENT` |
| `BIEN_DONG_NCT` | `RESIDENT_CHANGE` |
| `GIUONG_CHO_SAN_SANG` | `BED_AWAITING_READINESS` |
| `DO_GUI` | `STORED_BELONGING` |
| `VAN_DE_THEO_DOI` | `FOLLOW_UP_ISSUE` |
| `DA_CHOT` | `FINALIZED` |
| `PHAT_SINH_SAU_KHI_LAP` | `AFTER_PREPARATION` |
| `DA_XU_LY_TRONG_CA` | `RESOLVED_DURING_SHIFT` |
| `BAN_NHAP` | `DRAFT` |
| `DA_LAP` | `PREPARED` |
| `CO_Y_KIEN` | `HAS_COMMENTS` |
| `DA_XAC_NHAN` | `CONFIRMED` |
| `GIO_LAM_LIEN_TUC` | `CONTINUOUS_WORK_HOURS` |
| `DAO_TAO_BAT_BUOC_HET_HIEU_LUC` | `REQUIRED_TRAINING_EXPIRED` |
| `DUOI_NGUONG_PHUC_VU` | `BELOW_STAFFING_THRESHOLD` |
| `LOAI_CONG_VIEC_KHONG_DU_DIEU_KIEN` | `TASK_TYPE_REQUIREMENTS_NOT_MET` |
| `NGAY_NGHI_CO_DUYET` | `APPROVED_LEAVE_DAY` |
| `THIEU_PHU_TOI_THIEU` | `INSUFFICIENT_MINIMUM_COVERAGE` |
| `THIEU_O_BAT_BUOC` | `REQUIRED_CELL_MISSING` |
| `NGAY_SAI_DINH_DANG` | `INVALID_DATE_FORMAT` |
| `DIEN_THOAI_SAI_DINH_DANG` | `INVALID_PHONE_NUMBER_FORMAT` |
| `EMAIL_SAI_DINH_DANG` | `INVALID_EMAIL_FORMAT` |
| `TRUNG_HO_SO` | `DUPLICATE_STAFF_PROFILE` |
| `TRUNG_DONG_TRONG_FILE` | `DUPLICATE_FILE_ROW` |

### 14.5. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `DIEU_KIEN_CHUA_THOA` | `CONDITION_NOT_MET` |
| `TRANG_THAI_DA_DOI` | `STATE_CHANGED` |
| `CAN_XAC_NHAN_CANH_BAO` | `WARNING_CONFIRMATION_REQUIRED` |
| `FILE_KHONG_DUNG_MAU` | `INVALID_IMPORT_TEMPLATE` |
| `QUA_SO_DONG` | `IMPORT_ROW_LIMIT_EXCEEDED` |
| `IMPORT_CO_DONG_LOI` | `IMPORT_HAS_ROW_ERRORS` |
| `CAN_NGUOI_THAY_PHU_TRACH` | `REPLACEMENT_LEAD_REQUIRED` |
| `NGOAI_KHOANG_GHI_VANG` | `OUTSIDE_ABSENCE_REPORTING_WINDOW` |
| `CHONG_CA` | `SHIFT_OVERLAP` |
| `CHONG_GIAO_TRUONG_TANG` | `FLOOR_MANAGER_ASSIGNMENT_OVERLAP` |
| `THIEU_NGUOI_PHU_TRACH` | `SHIFT_LEAD_REQUIRED` |
| `TANG_CHUA_CO_TRUONG_TANG` | `FLOOR_MANAGER_REQUIRED` |
| `PHAN_CONG_KHONG_DAT` | `INVALID_ASSIGNMENTS` |
| `TRUNG_NHAN_VIEN_CHINH` | `PRIMARY_STAFF_OVERLAP` |
| `THIEU_CHUNG_CHI` | `REQUIRED_CREDENTIAL_MISSING` |
| `CA_DA_BAT_DAU` | `SHIFT_ALREADY_STARTED` |
| `DA_CO_BAN_GIAO` | `HANDOVER_ALREADY_EXISTS` |
| `CHUA_TOI_MOC_LAP` | `BEFORE_HANDOVER_PREPARATION_WINDOW` |
| `MUC_KHONG_BO_DUOC` | `HANDOVER_ITEM_REMOVAL_NOT_ALLOWED` |
| `THIEU_GHI_CHU_MUC_NGHIEM_TRONG` | `CRITICAL_ITEM_NOTE_REQUIRED` |
| `NGOAI_PHAM_VI_CA` | `OUTSIDE_SHIFT_SCOPE` |

### 14.6. Gói, SPI và bản giả Phase 1 (2026-10-23)

Tên trong tasks/plan là tên logic; chỉ tạo gói khi có lớp theo `backend/README.md`. Các tên gói chức năng dưới đây dùng nhất quán khi triển khai các phase sau.

| Tên logic | Mã |
| --- | --- |
| `nhanvien`, `giayphep`, `lichca`, `phancong` | `staff`, `credential`, `shift`, `assignment` |
| `tyle`, `bangiao`, `nhiemvu`, `lich` | `staffing`, `handover`, `duty`, `scheduling` |
| `doica`, `xoayca` (015 cùng M09) | `swap`, `rotation` |
| Port lệnh ca, phân công, lịch nháp (N-07, 015) | `ShiftCommandPort`, `AssignmentCommandPort`, `DraftScheduleCommandPort`; ở `shift.internal.service.port` hoặc `assignment.internal.service.port` khi đến task tương ứng |
| `DoGuiBanGiaoPort` | `staffing.api.spi.HeldBelongingsHandoverPort` |
| `dangGiu(nhanVienIds)`, `conDangGiu(doGuiIds, nhanVienIds)` | `heldBy(staffIds)`, `stillHeld(belongingIds, staffIds)` |
| `DoGuiDangGiuView` | `HeldBelongingView` |
| Đồ gửi, người giữ, người cao tuổi, vị trí lưu giữ | `belongingId`, `holderStaffId`, `residentId`, `storageLocationId` |
| `ChuyenDiNhiemVuPort` | `staffing.api.spi.TripDutyPort` |
| `truongDoan(chuyenId)`, `nguoiDiCung(chuyenId)` | `groupLeader(tripId)`, `companions(tripId)` |
| `chuyenTrongCa(tangKhuVucId, tu, den)` | `inShift(floorAreaId, from, to)` |
| `ChuyenTrongCaView` | `TripInShiftView` |
| Chuyến, nhân viên đang đi, giờ về dự kiến | `tripId`, `staffIds`, `expectedReturnAt` |
| `KiemTraCongBoMoRong.kiemTra(lichCaId)` | `staffing.shift.internal.service.port.ExtendedPublicationCheck.check(scheduleId)` |
| `CanhBaoCongBo` | `PublicationWarning(code, description, details)` ở cùng gói port nội bộ |
| Bản giả đồ gửi, chuyến đi, kiểm tra công bố | `FakeHeldBelongings`, `FakeTripDuties`, `FakeExtendedPublicationChecks` |
| Cấu hình bản giả dùng chung cho test M09 | `support.fake.staffing.FakeM09Config` |
| Đặt đồ đang giữ / bỏ đồ trong bản giả | `hold` / `release` |
| Đặt chuyến, cảnh báo, công tắc nguồn dữ liệu trong bản giả | `trip`, `warnings`, `available` |

`M09ArchTest` kiểm lời gọi port lệnh nội bộ theo các tên trên và cấm job gọi `createSchedule`, `createShift`, `createAssignment`, `createHandover`, `addHandoverItem`. `ExtendedPublicationCheck` có chiều gọi 008 → 015 nên không thuộc nhóm port lệnh chỉ 015 được gọi. Công tắc bản giả chỉ dùng trong test, không thêm endpoint HTTP.

### 14.7. Backend Phase 2 (2026-10-23)

Tên và ánh xạ được bổ sung cùng mã T003–T009; chỉ mục 14 thay đổi. Các phần API 002, `Credential.hiddenFields` và đồng bộ `Problem` được người dùng cho phép trong phiên triển khai; ghi chú duyệt ở plan 008.

| Tên logic | Mã |
| --- | --- |
| `nhan_vien / bo_phan` | `staff_profile / department` |
| `nhan_vien_bo_phan / lich_su_phu_trach_bo_phan` | `staff_department / department_lead_history` |
| `loai_chung_chi / pham_vi_hanh_nghe` | `credential_type / practice_scope` |
| `chung_chi / chung_chi_pham_vi / lan_chay_m09` | `credential / credential_practice_scope / m09_job_run` |
| `ho_ten / chuc_danh / chuyen_mon / ngay_bat_dau` | `full_name / position / specialty / start_date` |
| `dien_thoai / dien_thoai_chuan / email / dia_chi` | `phone_number / normalized_phone / email / address` |
| `tai_khoan_id / nguon / lan_import_id / phien_ban` | `account_id / source / import_batch_id / version` |
| `nghi_viec_hieu_luc / ly_do_nghi_viec / da_ap_nghi_viec_luc` | `employment_ends_at / termination_reason / termination_applied_at` |
| `phu_trach_id / trang_thai_hieu_luc / khoa_hien_hanh / khoa_mo` | `lead_staff_id / status / open_staff_id / open_department_id` |
| `nhan_vien_id / bo_phan_id / tu / den / nguoi_thuc_hien_id / ly_do` | `staff_id / department_id / starts_at / ends_at / actor_id / reason` |
| `nhom / co_thoi_han / bat_buoc_voi_vai_tro / co_pham_vi` | `credential_group / has_expiry / required_for_roles / has_practice_scopes` |
| `loai_id / so / don_vi_cap / ngay_cap / ngay_het_han` | `credential_type_id / credential_number / issuing_authority / issue_date / expiry_date` |
| `thay_the_cho / thu_hoi_tu / ly_do_thu_hoi / tep_scan_id` | `replaces_id / revoked_from / revocation_reason / scan_file_id` |
| `da_canh_bao_het_han_luc / da_ap_het_hieu_luc_luc` | `expiry_warning_at / invalidity_applied_at` |
| `chung_chi_id / ma_pham_vi` | `credential_id / practice_scope_id (id kỹ thuật); code giữ mã danh mục` |
| `lần chạy: thời điểm dự kiến / bắt đầu / kết thúc / kết quả` | `scheduled_at / started_at / finished_at / result` |
| `CaTrucApi / NhanSuThongKeApi` | `ShiftApi / StaffStatisticsApi` |
| `caDangDienRa / caCuaNhanVien / dangTrongCa / nhanVien` | `currentShift / staffShifts / onShift / staff` |
| `tyLePhucVu / banGiao / chungChiSapHetHan / canCuKhacVaiTro / lichSuPhanCong` | `staffingRatios / handovers / expiringCredentials / nonRoleBases / assignmentHistory` |
| `CaView / NhanVienView / LanTinhTyLeView / BanGiaoBaoCaoView` | `ShiftView / StaffPublicView / StaffingRatioView / HandoverReportView` |
| `ChungChiSapHetHanView / CanCuNhiemVuView / LichSuPhanCongView` | `ExpiringCredentialView / DutyBasisView / AssignmentHistoryView` |
| `HieuLucChungChi / coHieuLucTrongKhoang` | `CredentialValidity / covers (credential.internal.domain)` |
| `Hồ sơ: dữ liệu / nội dung ghi / kho / dịch vụ / controller` | `StaffProfile / StaffInput / StaffStore / StaffService / StaffController` |
| `HTTP hồ sơ: yêu cầu / phản hồi` | `StaffProfileInput / StaffProfileBody` |
| `Chứng chỉ: bản ghi / nội dung ghi / loại / kho / dịch vụ` | `Credential / CredentialInput / CredentialType / CredentialStore / CredentialService` |
| `HTTP chứng chỉ: yêu cầu / loại / phản hồi / controller` | `CredentialInputBody / CredentialTypeInput / CredentialBody / CredentialTypeBody / CredentialController` |
| `Bản cài SPI hồ sơ / giấy phép / phạm vi hành nghề` | `StaffDirectory / StaffLicenseLookup / PracticeScopeLookup (duty.internal.service)` |
| `ViecTheoPhutM09 / ViecHangGioM09 / ViecHangNgayM09` | `M09MinuteJob / M09HourlyJob / M09DailyJob (scheduling)` |
| `Khung việc chạy định kỳ / điều phối / kho lần chạy` | `M09TimedTask / M09Jobs / M09JobRuns` |
| `Bộ provider: cấu hình / gắn phạm vi / tải scan / lưu giữ` | `M09GovernanceConfig / M09ScopeAssignment / M09ScanAccess / M09Retention` |
| `Lỗi dữ liệu đầu vào M09` | `M09ValidationAdvice; INVALID_VALUE` |
| `Mô tả lỗi HTTP của 008 / cảnh báo` | `M09ProblemBody / M09ProblemBody.M09Warning (web.response); trường lỗi chung 000 và warnings` |
| `Bản giả API đọc ca / thống kê` | `FakeShifts / FakeStaffStatistics trong FakeM09Config` |
| `HAN_CHE_NHAN_VIEN (liên hệ / phần riêng tư khác) / LY_DO_NHAN_SU` | `InfoGroup.STAFF_CONTACT / STAFF_RESTRICTED / STAFF_REASON` |
| `M09.NHAN_VIEN / M09.BO_PHAN / M09.CHUNG_CHI / M09.SCAN_CHUNG_CHI` | `M09.STAFF / M09.DEPARTMENT / M09.CREDENTIAL / M09.CREDENTIAL_SCAN` |
| `M09.BAN_GIAO / lý do vắng ca` | `M09.HANDOVER / M09.STAFF_ABSENCE` |
| `Hành động nhật ký tạo / sửa nhân viên, thêm chứng chỉ, tạo loại` | `STAFF_CREATED / STAFF_UPDATED / CREDENTIAL_ADDED / CREDENTIAL_TYPE_CREATED` |
| `Đổi bộ phận phải qua lệnh / bộ phận không hợp lệ` | `DEPARTMENT_CHANGE_REQUIRES_COMMAND / INVALID_DEPARTMENT` |
| `002: kiểm danh mục còn tham chiếu (đã duyệt)` | `ProfessionalActivityApi.usesCredentialType / usesPracticeScope` |

Sự kiện ở `staffing.api.events` (chỉ khai báo ở T003; dịch vụ nguồn dựng yêu cầu thông báo và phát ở story tương ứng):

| Tên logic | Mã |
| --- | --- |
| `LichCaDaCongBo` | `SchedulePublished` |
| `CaDaThayDoi` | `ShiftChanged` |
| `CaDaBatDau` | `ShiftStarted` |
| `CaDaKetThuc` | `ShiftEnded` |
| `NhanVienVangCa` | `StaffAbsent` |
| `NhanVienNghiViec` | `StaffEmploymentEnded` |
| `CaThieuNguoiPhuTrach` | `ShiftLeadMissing` |
| `TangChuaCoTruongTang` | `FloorManagerMissing` |
| `PhanCongDaDoi` | `AssignmentChanged` |
| `PhanCongKetThucDoChungChi` | `AssignmentsEndedByCredential` |
| `ChungChiSapHetHan` | `CredentialExpiring` |
| `CanhBaoChungChiDaDong` | `CredentialExpiryWarningClosed` |
| `CaDuoiNguongPhucVu` | `ShiftBelowStaffingThreshold` |
| `NctChuaCoNguoiPhuTrach` | `ResidentsWithoutAssignedStaff` |
| `BanGiaoChuaLap` | `HandoverNotPrepared` |
| `BanGiaoChuaHoanTatKhiHetCa` | `HandoverIncompleteAtShiftEnd` |
| `BanGiaoChuaXacNhan` | `HandoverNotConfirmed` |
| `BanGiaoCoYKien` | `HandoverFeedbackReceived` |
| `BanGiaoDaLap` | `HandoverPrepared` |
| `BanGiaoDangDoDaChuyen` | `HandoverReassigned` |
| `BanGiaoDaDinhChinh` | `HandoverCorrected` |
| `BanGiaoDaXacNhan` | `HandoverConfirmed` |
| `BoPhanChuaCoPhuTrach` | `DepartmentLeadMissing` |

Ba loại mặc định dùng code `DOCTOR_LICENSE`, `NURSING_LICENSE`, `FIRST_AID_TRAINING`; các cờ khởi tạo được người dùng duyệt trong phiên. `Credential.hiddenFields` là mảng tên trường chỉ đọc, không bắt buộc. Trường lỗi dùng nguyên tên chung ở mục 6; `warnings` giữ nguyên schema 008. Nhóm `STAFF_REASON` chưa mở quyền cho người không phải Quản lý viện trước khi có luồng vắng ca và dữ liệu người ghi/phạm vi ở T039–T040.

### 14.8. Lịch ca Phase 3 (2026-10-24)

Tên bổ sung cho T010–T016; chỉ mục 14 thay đổi. Người dùng duyệt khai báo tối thiểu SPI của C/D kèm bản giả dùng chung và `version` chỉ đọc trong `ShiftTemplate`, `ShiftSchedule`, `Shift`. C/D review các tên SPI dưới đây trước khi gộp; các hàm phân công/bàn giao nối ở T021/T026.

| Tên logic | Mã |
| --- | --- |
| `mau_ca / lich_ca / ca / ca_nhan_vien` | `shift_template / shift_schedule / work_shift / shift_staff` |
| `nguoi_phu_trach_ca / ban_cong_bo_lich` | `shift_lead_history / shift_publication` |
| `loai_pham_vi / pham_vi_id / khoa_pham_vi` | `scope_type / scope_id / scope_key` |
| `gio_bat_dau / gio_ket_thuc / yeu_cau_ban_giao` | `start_time / end_time / requires_handover` |
| `thang / nguoi_lap / nguoi_cong_bo / cong_bo_luc / tu_duyet` | `month / created_by / published_by / published_at / self_approved` |
| `lich_id / mau_ca_id / ca_id / thieu_nguoi_phu_trach` | `schedule_id / template_id / shift_id / missing_lead` |
| `bac_si_truc / vang_luc / rut_do_nghi_viec_luc / bo_sung_luc` | `on_duty_doctor / absent_at / withdrawn_on_resignation_at / added_at` |
| `nghi_co_duyet_yeu_cau_id / roi_ca_luc / can_cu_vao_ca / can_cu_roi_ca` | `approved_leave_request_id / left_at / entry_basis / departure_basis` |
| `da_nhac_lap_ban_giao_luc / da_nhac_ket_thuc_luc / da_nhac_chua_xac_nhan_luc / da_bao_chua_co_nguoi_phu_trach_luc` | `prepare_handover_reminded_at / shift_end_reminded_at / unconfirmed_handover_reminded_at / unassigned_residents_notified_at` |
| `dang_duoi_nguong / da_bao_quan_ly_luc / ly_do_huy / noi_dung` | `below_threshold / manager_notified_below_threshold_at / cancellation_reason / content` |
| `ChuoiCaLienTuc / kho / dịch vụ lịch / vòng đời ca` | `ContinuousShiftChain / ShiftStore / ShiftScheduleService / ShiftLifecycle` |
| `Mẫu / lịch / ca / thành viên / đầu vào` | `ShiftTemplate / ShiftSchedule / WorkShift / ShiftMember / TemplateInput / ShiftInput` |
| `Nguồn trạng thái bàn giao / phạm vi lịch-ca` | `ShiftHandoverState / ShiftScopeAssignment` |
| `TraCuuCaService / bản cài phần ca của SPI` | `ShiftQueries / CareShiftLookup / NursingShiftLookup / AlertStaffLookup / ShiftLocationReferences` |
| `ThamChieuViTri / ViTriRef / ThamChieuViTriView` (003) | `roombedallocation.api.spi.LocationReferences / roombedallocation.api.dto.LocationRef / LocationReferenceView` |
| `NhanSuCaPort / CaView` (005) | `care.api.spi.CareShiftPort / care.api.dto.CareShiftView` |
| `DieuDuongCaPort / CaView` (006) | `medication.api.spi.NursingShiftPort / medication.api.dto.NursingShiftView` |
| `NhanSuCanhBaoPort / CaView` (007) | `incident.api.spi.AlertStaffPort / incident.api.dto.AlertShiftView` |
| `conThamChieu / daTungThamChieu / caDaCongBo / caTai / caSau` | `activeReferences / everReferenced / publishedShifts / shiftAt / nextShift` |
| `coTenTrongCa / duDieuKien / caCoNguoi / dieuDuongCoCa` | `listedInShift / qualified / hasQualifiedStaff / nursesWithShift` |
| `coTenTrongCaDangDienRa / truongTangDuocGiao / tangTrongCa / cacTangCoCa` | `inProgressShiftMember / assignedFloorManager / floorDuringShift / floorsWithShift` |
| `Bản giả dùng chung 003 / 005 / 006 / 007` | `FakeLocationReferences / FakeCareShifts / FakeNursingShifts / FakeAlertStaff` và `FakeM03Config / FakeM04Config / FakeM07Config / FakeM05Config` |
| `HTTP lịch ca: controller / yêu cầu / phản hồi` | `ShiftController / ShiftRequests / ShiftResponses` |
| `Web lịch: trang / form ca / form lịch-mẫu-công bố / nhãn` | `features/shifts/ShiftsPage / ShiftForm / ScheduleForms / shiftLabels` |
| `M09.LICH_CA / M09.CA / M09.MAU_CA / M09.CA_NHAN_VIEN / M09.NGUOI_PHU_TRACH_CA / M09.BAN_CONG_BO_LICH` | `M09.SCHEDULE / M09.SHIFT / M09.SHIFT_TEMPLATE / M09.SHIFT_STAFF / M09.SHIFT_LEAD_HISTORY / M09.SHIFT_PUBLICATION` |
| `Tạo-sửa mẫu / công bố lịch / chuyển trạng thái` | `SHIFT_TEMPLATE_CREATED / SHIFT_TEMPLATE_UPDATED / SCHEDULE_PUBLISHED / SHIFT_STATUS_CHANGED` |
| `Trùng lịch / chồng ca / cần xác nhận cảnh báo / dữ liệu đã thay đổi` | `SCHEDULE_ALREADY_EXISTS / SHIFT_OVERLAP / WARNING_CONFIRMATION_REQUIRED / STATE_CHANGED` |
| `Vượt giờ liên tục / đào tạo hết hạn / cần phụ trách / cần Trưởng tầng / phân công không hợp lệ` | `CONTINUOUS_WORK_HOURS / REQUIRED_TRAINING_EXPIRED / SHIFT_LEAD_REQUIRED / FLOOR_MANAGER_REQUIRED / INVALID_ASSIGNMENTS` |
| `Nguồn ca tại tầng mâu thuẫn` | `InvalidPermissionDataException("M09.SHIFT", "MULTIPLE_CURRENT_SHIFTS", floorId)` |

`ShiftSchedule.version` lấy từ danh sách lịch, `ShiftTemplate.version` từ danh mục mẫu; `Shift.version` và ETag từ đọc chi tiết. If-Match là số phiên bản trong dấu ngoặc kép. Đây là bổ sung hợp đồng đã duyệt, không thêm đường dẫn. Khoảng ngày `/me/shifts` dùng `[from, to)` theo múi giờ Việt Nam.

### 14.9. Phân công Phase 4 (2026-10-24)

Người dùng đã duyệt API tối thiểu của B/C/D, `AssignmentView.scopeEndsAt` của A và endpoint xem trước. Chủ module review tên API của mình trước khi gộp; các tên mới được ghi trong mục 14 của 008.

| Tên logic | Mã |
| --- | --- |
| `tang_ca_khoa / giao_truong_tang / phan_cong` | `floor_scope_lock / floor_manager_assignment / staff_assignment` |
| `loai_doi_tuong / doi_tuong_id / loai_cong_viec_id` | `target_type / target_id / task_type_id` |
| `chinh_ho_tro / dieu_duong_phu_trach / ket_thuc_thuc_te / ly_do_ket_thuc / nguon` | `assignment_kind / assigned_nurse / actual_end_at / end_reason / source` |
| `nguoi_giao / đã nhắc phân công khi công bố` | `assigned_by / publication_assignments_notified_at` |
| `PhanCong / đầu vào / GiaoTruongTang / XacDinhNhanVienChinh` | `StaffAssignment / AssignmentInput / FloorManagerAssignment / PrimaryAssigneeResolver` (`TargetRef`) |
| Kho / lệnh / nguồn / truy vấn / vòng đời / nhắc phân công | `AssignmentStore / AssignmentService / AssignmentSources / AssignmentQueries / AssignmentLifecycle / AssignmentReminders` |
| Lệnh giao Trưởng tầng / provider phạm vi | `FloorManagerService / AssignmentScopeAssignment` |
| Phụ trách bộ phận / kho / lệnh / bản cài NhiemVuApi | `DepartmentDuty / DepartmentDutyStore / DepartmentDutyService / DutyDirectory` |
| `NguoiCaoTuoiApi.laTrangThaiCuoi / NguoiCaoTuoiDaChuyenTrangThai` (001) | `ResidentApi.isFinalStatus / ResidentStatusChanged`; dùng đúng API và enum `ResidentStatus`/`StatusCommand` của B trên develop |
| Tác động khi người cao tuổi sang trạng thái cuối | `ResidentFinalAssignmentEffects` |
| `ViTriApi.taiThoiDiem / CoCauViTriApi / LichSuViTriApi` (003) | `LocationApi.at / LocationStructureApi / LocationHistoryApi` (`atLocation`, `inDay`, `byResident`); bộ lọc vị trí null: toàn viện |
| Vị trí / phòng / tầng / khu vực / dòng lịch sử vị trí | `LocationView / RoomView / FloorView / AreaView / LocationStay` (`AbsenceInterval`) |
| `LoaiCongViecApi / YeuCauLoaiCongViec / KeHoachChamSocApi / CongViecCaApi` (005) | `TaskTypeApi / TaskRequirements / CarePlanApi / ShiftTaskApi` |
| `yeuCau / loaiCongViecHienHanh / khiDoiPhanCong / khiKetThucPhanCong` | `requirements / currentTaskTypes / onAssignmentChanged / onAssignmentEnded` |
| `PhanCongChamSocView / PhanCongTruyVetView` (005/007) | `CareAssignmentView / AssignmentTraceView` |
| Hàm SPI phân công của 005/006/007 | `assignments / assignedNurses` (005), `assignedTo` (006), `assignedNurse / assignmentsInRange` (007) |
| Bản giả nguồn dùng chung | `FakeResidents / FakeM01Config`, `FakeLocations` trong `FakeM03Config`, `FakeCareRequirements` trong `FakeM04Config`; mở rộng các fake SPI Phase 3 hiện có |
| Mốc ngừng cấp phạm vi khi kết thúc sớm | `AssignmentView.scopeEndsAt`; giữ constructor bảy tham số, không cộng CFG-M15-07 sau mốc này |
| Xem trước phân công | `POST /shifts/{id}/assignments/preview`: `NewAssignment` → `ResultWithWarnings` không có `id` |
| HTTP phân công / yêu cầu / phản hồi | `AssignmentController / AssignmentRequests / AssignmentResponses`; schema sao chép `CopyAssignmentsRequest / CopyAssignmentsResult` |
| Web phân công / giao Trưởng tầng | `features/shifts/AssignmentPanel / FloorManagerPanel` |
| `M09.PHAN_CONG / M09.GIAO_TRUONG_TANG` | `M09.ASSIGNMENT / M09.FLOOR_MANAGER_ASSIGNMENT` |
| Nhật ký lập / hủy / kết thúc / đổi trạng thái phân công | `ASSIGNMENT_CREATED / ASSIGNMENT_CANCELLED / ASSIGNMENT_ENDED / ASSIGNMENT_STATUS_CHANGED` |
| Nhật ký kết thúc theo trạng thái người cao tuổi | `ASSIGNMENT_ENDED_BY_RESIDENT_STATUS` |
| Nhật ký giao / kết thúc / đổi trạng thái giao Trưởng tầng | `FLOOR_MANAGER_ASSIGNED / FLOOR_MANAGER_ASSIGNMENT_ENDED / FLOOR_MANAGER_ASSIGNMENT_STATUS_CHANGED` |
| Nhật ký thay Phụ trách bộ phận | `DEPARTMENT_LEAD_REPLACED` |
| Thiếu nguồn / phân công không hợp lệ / đối tượng không hợp lệ | `ASSIGNMENT_SOURCE_UNAVAILABLE / INVALID_ASSIGNMENT / INVALID_ASSIGNMENT_TARGET` |
| Thiếu chứng chỉ / trùng nhân viên chính / công việc không đủ điều kiện | `ASSIGNMENT_CREDENTIAL_REQUIRED / PRIMARY_ASSIGNMENT_OVERLAP / UNQUALIFIED_TASK_TYPES` |
| Giao Trưởng tầng không hợp lệ / chồng thời gian / người thay bộ phận không hợp lệ | `INVALID_FLOOR_MANAGER_ASSIGNMENT / FLOOR_MANAGER_ASSIGNMENT_OVERLAP / INVALID_REPLACEMENT_DEPARTMENT_LEAD` |
| Dữ liệu nhiều người chính / nhiều Trưởng tầng / nhiều bộ phận | `MULTIPLE_PRIMARY_ASSIGNEES / MULTIPLE_FLOOR_MANAGERS / MULTIPLE_DEPARTMENTS` |

### 14.10. Nền bàn giao Phase 5 (2026-10-24)

| Tên logic | Mã triển khai |
| --- | --- |
| `XacDinhCaSau / MucNghiemTrong` | `NextHandoverShift / CriticalHandoverItem` |
| Bàn giao / mục / nhận định / ý kiến / phản hồi | `shift_handover / handover_item / handover_assessment / handover_comment / handover_reply` |
| Nhận định chung / theo người cao tuổi | `GENERAL / BY_RESIDENT` |
| Loại nhận định / khóa người cao tuổi / nhóm mục | `assessment_kind / resident_key / item_group` |
| Người bàn giao / tài khoản tạo / tài khoản xác nhận / căn cứ nhiệm vụ | `handover_staff_id / created_by / confirmed_by / duty_basis` |
| Lúc chốt / lúc xác nhận / trạng thái lúc chốt / người ghi chú | `finalized_at / confirmed_at / status_at_finalization / note_author` |
| Kho và truy vấn trạng thái bàn giao thật | `handover.internal.persistence.HandoverStore / handover.internal.service.HandoverQueries` |
| Đã lập xong / đã xác nhận theo ca | `prepared(shiftId) / confirmed(shiftId)`; `HAS_COMMENTS` không phải đã lập xong |
| Dịch vụ nguồn / lệnh / nội dung hiện hành / đính chính / lưu giữ / nhắc | `HandoverSources / HandoverService / HandoverContents / HandoverCorrection / HandoverRetention / HandoverReminders` |
| Tài khoản hoàn tất / các khóa nguồn lúc chốt | `finalized_by / source_keys_at_finalization` (metadata kỹ thuật, không phải mục sinh tự động) |
| Định danh nguồn bị thiếu lúc chốt | `UNAVAILABLE:<sourceType>` |
| Nguồn công việc / liều / cảnh báo / chỉ số / giường / phân bổ | `TaskHandoverApi / DoseHandoverApi / AlertHandoverApi / VitalSignApi / BedStatusApi / AllocationHandoverApi` |
| Bản nguồn công việc / liều / cảnh báo / chỉ số / giường / phân bổ | `TaskHandoverView / DoseHandoverView / AlertHandoverView / VitalSignHandoverView / AwaitingReadyBedView / AllocationHandoverView` |
| Trạng thái hiện tại nguồn / trạng thái thật giường / còn giữ đồ | `currentStatus / currentState / stillHeld` |
| Tham chiếu nguồn / kết quả chuyển công việc | `SourceRef / HandoverTransferResult` |
| Hồ sơ liên quan chuyến / thời điểm xuất phát | `residentIds / departedAt` trong `TripInShiftView` |
| Ca sau đã bắt đầu, bàn giao trước chưa xác nhận | `startedWithoutConfirmedHandover / UnconfirmedShiftHandoverView` |
| Chưa đọc được nguồn / thiếu ghi chú nghiêm trọng / trước mốc lập / đã có bàn giao | `HANDOVER_SOURCE_UNAVAILABLE / CRITICAL_ITEM_NOTE_REQUIRED / BEFORE_HANDOVER_PREPARATION_WINDOW / HANDOVER_ALREADY_EXISTS` |
| Mục không được bỏ / ngoài phạm vi ca | `HANDOVER_ITEM_REMOVAL_NOT_ALLOWED / OUTSIDE_SHIFT_SCOPE` |
| Khóa nội dung đính chính | `generalAssessment / items / residentAssessments / comments`; con giữ `id` hoặc `residentId`, chỉ sửa `description / note / content`, phản hồi giữ thứ tự |
| Nhãn loại JSON nội bộ nhóm nhận / cách thay thế (phần A được duyệt 2026-10-25) | `kind`; `RecipientGroup / Substitution`, `JsonTypeInfo.Id.NAME`, danh sách loại tường minh qua `JsonSubTypes` |
| Màn bàn giao / đính chính | `features/handover/HandoverPage.jsx / HandoverCorrections.jsx` |

### 14.11. Nhân sự và import Phase 6 (2026-10-25)

| Tên logic | Mã triển khai |
| --- | --- |
| Lưu yêu cầu nghỉ việc / tác động nghỉ việc | StaffEmploymentStore / StaffEmploymentEffects |
| Khi nhân viên nghỉ việc (005) | ShiftTaskApi.onResignation(staffId, effectiveAt); FakeCareRequirements.onResign |
| Lệnh cho nghỉ việc / nhận lại / đổi bộ phận | terminateEmployment / rehire / changeDepartment |
| Màn nhân sự / import | features/shifts/StaffPage.jsx / StaffImportPanel.jsx; route staff |
| Tác động hết hiệu lực / nhắc chứng chỉ | CredentialInvalidityEffects / CredentialReminders |
| Khuôn thông báo nhân sự | StaffNotifications; missingLeadInFloors |
| Danh mục bộ phận / lệnh chỉ định phụ trách | Department / DepartmentBody; assignLead |
| Lưu dấu lần import, tên file, số dòng | staff_import_batch; file_name / row_count / created_by / created_at |
| Đọc/chèn lô import / kiểm dòng / dòng lỗi | StaffImportStore / StaffImportService / StaffImportRowValidator; StaffImportData.Row / Error / Preview / Result |
| Xác nhận có dòng lỗi (409 đúng bảng xem trước) | StaffImportRejected; StaffImportResponses |
| Gia hạn / ghi thu hồi / sửa sai chứng chỉ | renew / revoke / correct; effectiveFrom |
| Lệnh/nhật ký nghỉ việc / nhận lại / chuyển bộ phận | STAFF_TERMINATION_REQUESTED / STAFF_EMPLOYMENT_ENDED / STAFF_REHIRED / STAFF_DEPARTMENT_CHANGED |
| Nhật ký loại chứng chỉ / bộ phận / giao phụ trách | CREDENTIAL_RENEWED / CREDENTIAL_REVOKED / CREDENTIAL_CORRECTED; DEPARTMENT_CREATED / DEPARTMENT_LEAD_ASSIGNED |
| Nhật ký tác động phân công / giao trưởng tầng | ASSIGNMENT_ENDED_BY_EMPLOYMENT / ASSIGNMENT_ENDED_BY_CREDENTIAL / FLOOR_MANAGER_ENDED_BY_EMPLOYMENT |
| Nhật ký import (NV_IMPORT) / thay đổi ca do nghỉ việc | STAFF_IMPORTED / EMPLOYMENT_ENDED |
| Form nhân sự / bộ phận / chứng chỉ / loại chứng chỉ | StaffForms / DepartmentPanel / CredentialPanel / CredentialTypePanel |
| Mã danh mục phạm vi trên HTTP (giữ SPI 002) | PracticeScope / PracticeScopeBody.code; schema StaffPracticeScope |


### 14.12. Tỷ lệ phục vụ và lịch sử thống kê Phase 7 (2026-10-25)

| Tên logic | Mã triển khai |
| --- | --- |
| Tính tỷ lệ phục vụ / đầu vào người cao tuổi, thành viên / kết quả | `ratio/internal/domain/StaffingRatioCalculator`; `Resident / Member / Result` |
| Lần tính tỷ lệ / bảng lưu bất biến | `StaffingRatioCalculation / staffing_ratio_calculation` |
| Ca / thời điểm / sự kiện / tổng trọng số / số nhân viên | `shift_id / calculated_at / event / total_weight / staff_count` |
| Tỷ lệ / ngưỡng tối đa / đạt ngưỡng | `ratio / threshold / meets_threshold` |
| Lưu nháp / công bố / vắng ca / bổ sung / nghỉ việc | `DRAFT_SAVED / PUBLISHED / STAFF_ABSENT / STAFF_ADDED / EMPLOYMENT_ENDED` |
| Đổi ca 015 / đổi mức / phân bổ / vắng NCT / trạng thái cuối | `SHIFT_SWAP_015 / CARE_LEVEL_CHANGED / ALLOCATION_CHANGED / RESIDENT_ABSENCE / FINAL_STATUS` |
| Lưu lịch sử / cờ cảnh báo / nhắc quản lý viện | `StaffingRatioStore / StaffingRatioService`; `below_threshold / manager_notified_below_threshold_at` (cột đã có từ Phase 3) |
| Provider lưu giữ lần tính | `M09.STAFFING_RATIO_CALCULATION` |
| Màn lịch sử tỷ lệ trong chi tiết ca | `features/shifts/StaffingRatioPanel.jsx` |
| Nguồn tỷ lệ / listener sau commit / truy vấn thống kê | `StaffingRatioSources / StaffingRatioEvents / StaffStatisticsService / StaffStatisticsStore / StaffStatisticsRows` |
| Lịch đến trong ngày (004, khai báo tối thiểu được duyệt) | `admission.api.ContractReadApi.attendanceOn`; `AttendanceScheduleView(date, arrivalTime, departureTime)`; `FakeContractSchedules / FakeM02Config` |
| Vị trí nghỉ bán trú (003, hàm đã có trong hợp đồng) | `LocationStructureApi.daycareRestLocation`; `FakeLocations.daycareRestLocation / closeStays` |
| Phân bổ bắt đầu / đóng / chuyển giường (003) | `roombedallocation.api.events.AllocationStarted / AllocationClosed / BedChanged` |
| Dữ liệu tỷ lệ / thống kê chưa đọc đủ | `STAFFING_RATIO_UNAVAILABLE / STAFF_STATISTICS_SOURCE_UNAVAILABLE`; không trả số không thay nguồn lỗi |
| Ngưỡng theo mẫu / trọng số theo mức (kiểu TABLE của CFG) | `CFG_M09_01`: object khóa là id mẫu ca dạng chuỗi; `CFG_M09_12`: object khóa `BASIC / REGULAR / SPECIAL`, giá trị số dương; không giá trị mặc định viết cứng |
| Đọc nhật ký theo khoảng và con trỏ (A duyệt E cài, A review) | `AuditLogApi.recordedBetween(entityType, from, to, afterId, limit)`; khoảng `[from,to)`, id tăng dần, trang tối đa `RECORDED_LIMIT` (200), giữ kiểu `AuditLogRecord` và hàm `recorded` |
| Lịch sử không gắn ca / nguồn Trưởng tầng, thành viên, Người phụ trách | `AssignmentHistoryView.shiftId: Long` (null khi không gắn ca); `role: FLOOR_MANAGER / SHIFT_MEMBER / SHIFT_LEAD`; khóa bản ghi theo nguồn và id, không tạo id ca giả |
| Căn cứ nhiệm vụ / thay Trưởng tầng / tạm nhận / vai trò với lời mời đã gửi (FR-056) | `SHIFT_LEAD / FLOOR_MANAGER_SUBSTITUTE / TEMPORARY_RECEIVER / ROLE_INVITATION_SENT`; đọc giá trị đã ghi, không suy từ vai trò hiện tại |

### 14.13. Điều chỉnh ca sau công bố Phase 8 (2026-10-25)

| Tên logic | Mã triển khai |
| --- | --- |
| Lệnh sau công bố / truy cập dữ liệu / thông báo | `PostPublicationService / PostPublicationStore / PostPublicationNotifications` trong chức năng `shift` |
| Quyền xem lý do vắng / dòng vắng | `StaffAbsenceApi.canViewReason / AbsenceReasonAccess / StaffAbsence`; id dòng `shift_staff`, nguồn `M09.STAFF_ABSENCE` |
| Bản giả SPI lý do vắng (A/002) | `FakeStaffAbsences` (`allow`, `clear`, `Config`); mặc định che |
| Khi vắng ca (C/005) | `ShiftTaskApi.onAbsence(staffId, shiftId)`; `FakeCareRequirements.onAbsent` |
| Port lệnh ca / phân công cho 015 | `ShiftCommandPort / ShiftCommandService`; `AssignmentCommandPort / AssignmentCommandService`; MANDATORY |
| Vào ca / rời ca / đổi phụ trách / gắn thiếu phụ trách | `addToShift / leaveShift / changeLead / markMissingLead` |
| Căn cứ vào: đổi ca, nhận thay, lời mời | `ShiftEntryBasis(Kind, sourceId, Reason)`; `SWAPPED / COVERED / INVITED` |
| Căn cứ rời: đổi ca, nghỉ có duyệt | `ShiftDepartureBasis(Kind, sourceId)`; `SWAPPED / APPROVED_LEAVE` |
| Kiểm tra / lập / hủy phân công từ yêu cầu | `validate / create / cancel`; `AssignmentInput`, source `SHIFT_SWAP_015` |
| Form lệnh ca / lịch sử căn cứ trên web | `features/shifts/PostPublicationPanel / DutyBasisPanel` |
| Lệnh backend cho phép | `addStaff / changeLead / recordAbsence / cancelShift / viewDutyBasisAudit` |
| Nhật ký thêm / rời / vắng / chuyển phụ trách / hủy | `STAFF_ADDED_TO_SHIFT / STAFF_LEFT_SHIFT / STAFF_ABSENCE_RECORDED / SHIFT_LEAD_CHANGED / SHIFT_CANCELLED` |
| Đã có tên trong ca / không còn trong ca / phụ trách không hợp lệ | `STAFF_ALREADY_IN_SHIFT / STAFF_NOT_IN_SHIFT / INVALID_SHIFT_LEAD` |
| Ngoài khoảng ghi vắng / cần người thay | `OUTSIDE_ABSENCE_REPORTING_WINDOW / REPLACEMENT_LEAD_REQUIRED` |
| DTO thân lệnh HTTP / phản hồi căn cứ | `ShiftRequests.AddStaff / ChangeLead / RecordAbsence / ReasonBody`; `ShiftResponses.DutyBasis` |

### 14.14. Kiểm tích hợp và lịch sử phụ trách Phase 9 (2026-10-26)

| Tiếng Việt | Tên trong mã / tài liệu |
| --- | --- |
| Khoảng phụ trách đã ghi theo thời gian ca/thành viên | `ShiftLeadPeriod(shiftId, floorAreaId, startsAt, endsAt, shiftEndsAt)` trong `shift.internal.domain` |
| Người phụ trách đúng thời điểm / khoảng lịch sử | `ShiftStore.leadStaffAt / leadPeriods`, `ShiftQueries.leadAtFloor`; đọc `shift_lead_history` |
| Lỗi dữ liệu nguồn nhiều người phụ trách tại một mốc | `MULTIPLE_SHIFT_LEADS` trong `InvalidPermissionDataException`, nguồn `M09.SHIFT` |
| Kiểm bảo vệ bản gốc JDBC / SPI ca cảnh báo / nối phạm vi thật | `M09RecordProtectionIT / AlertStaffPortIT / StaffModuleIntegrationIT` |
| Biên bản kiểm chứng, truy vết và số đo còn chờ | `specs/008-staff-shift-handover/validation.md` |
| Bảo vệ lịch sử chuyển bộ phận, đóng khoảng một lần | `V008_08__protect_staff_department_history.sql`; `trg_staff_department_close_once / trg_staff_department_no_delete`, chỉ `UPDATE(ends_at)` |

### 14.15. Đồng bộ API nguồn trên develop (2026-10-26)

Tên chính thức của C/D giữ theo feature sở hữu; các tên dưới đây ghi cách consumer 008 gọi sau khi ghép, không khai báo hợp đồng nguồn thứ hai.

| Tiếng Việt | Tên trong mã / cách dùng ở 008 |
| --- | --- |
| Vị trí người cao tuổi hiện tại / lịch sử theo vị trí, ngày | `ResidentLocationApi`; `LocationHistoryApi.byLocation / byLocationOn`; `ResidentLocationRow / BedLocationRow` |
| Mã tầng của vị trí / vị trí nghỉ bán trú | `LocationView.floorId`; `LocationStructureApi.dayCareRestAreaLocation` |
| Tham chiếu vị trí hiện tại từ lịch ca/phân công | `LocationReferences.currentReferences`; `roombedallocation.api.spi.LocationReferenceView(type, id, label)` |
| Giường chờ sẵn sàng quá hạn / khoảng chờ / trạng thái tại mốc | `BedStatusApi.overdueAwaitingReady`; `BedStatisticsApi.awaitingReady / stateAt`; nguồn C dùng `AWAITING_READY`, adapter bàn giao giữ `AWAITING_READINESS` |
| Liều thuốc đang chờ bàn giao / trạng thái hiện tại | `DosePendingHandoverView(doseKey, residentId, prescriptionId, scheduledTime, status, reason, controlledSubstance)`; `DoseHandoverApi.currentStatuses` |
| SPI ca/điều dưỡng chính thức của thuốc | `medication.api.spi.ShiftNursePort`, do `NursingShiftLookup` cài cùng nguồn với `NursingShiftPort` hiện có |
| Sự kiện phân bổ mở / đóng / chuyển giường thật | `BedAllocationStarted / BedAllocationClosed / BedTransferred`; `StaffingRatioEvents` tiếp nhận để tính lại theo logic hiện có |
| Cấu hình vị trí giả riêng cho test consumer 008 | `FakeLocations.Config` trong cùng bản giả dùng chung; không thay bean thật trong test chủ nguồn |
| Biên bản gỡ conflict và lỗi develop còn chờ | `specs/008-staff-shift-handover/merge-verification.md` |

### 14.16. Bảng sau gộp R-15 *(bổ sung 2026-10-30; cột chi tiết ở data-model 008 mục 0.1)*

| Tên logic | Mã |
| --- | --- |
| `phan_cong` gồm giao trưởng tầng | `staff_assignment.kind` `CARE`, `FLOOR_MANAGER` |
| mục, nhận định của `ban_giao`; phạm vi của `chung_chi` | `shift_handover.items`, `shift_handover.assessments`; `credential.practice_scope_ids` |
| loại bảng chung của 008 | `M09.DEPARTMENT`, `M09.CREDENTIAL_TYPE`, `M09.PRACTICE_SCOPE`, `M09.SHIFT_TEMPLATE` (danh mục); thuộc tính `NGUOI_PHU_TRACH` của `BO_PHAN`, `CA` (lịch sử chung); `M09.SHIFT_PUBLICATION`, `M09.HANDOVER_COMMENT`, `M09.HANDOVER_REPLY`, `M09.STAFFING_RATIO_CALCULATION` (sự kiện); `M09.STAFF_IMPORT_BATCH` (hồ sơ chung); `M09.TANG:<id>` (khóa); `M09.JOB.<job>` (mốc đã nhắc) |


### 14.17. Người nhận thông báo đính chính bàn giao (2026-10-27)

| Tiếng Việt | Tên trong mã / cách dùng ở 008 |
| --- | --- |
| Người xác nhận bàn giao đã ghi | `confirmed_by` là mã tài khoản; đọc hồ sơ nhân viên qua `AccountApi.byId(...).staffId`, không dùng liên kết tài khoản cục bộ của M09 |
| Người phụ trách ca hiện tại nhận đính chính | `RecipientGroup.ShiftLead(floorAreaId, Substitution.Default)` trong `HandoverCorrected`; M13 phân giải theo nhiệm vụ tại thời điểm gửi (FR-057) |

## 15. Feature 009 (`009-notification-service`, A) *(bổ sung 2026-10-21, khi dịch `contracts/api-009.yaml`)*

Bảng, cột, lớp của 009 bổ sung vào mục này cùng thay đổi với mã (R-03). Khuôn `common.notification` đã dịch ở mục 6.5 (`NotificationRequest`, `NotificationLevel`, `InformationType`, `RecipientGroup`, `Substitution`, `InformationSubject`, `Q354Case`) giữ nguyên.

### 15.1. Đường dẫn API (tiền tố `/api/v1`)

| Tên logic | Mã |
| --- | --- |
| `/toi/thong-bao`, `/toi/thong-bao/{id}`, `…/an` | `/me/notifications`, `/me/notifications/{id}`, `…/hide` |
| `/thong-bao/luong` (SSE), `/thiet-bi-day` | `/notifications/stream`, `/push-devices` |
| `/thong-bao/{id}/xac-nhan`, `…/xac-nhan-va-tiep-nhan` | `/notifications/{id}/acknowledge`, `…/acknowledge-and-take-charge` |
| `/toi/luot-nhac-goi`, `/luot-nhac-goi/{id}`, `…/ket-qua` | `/me/call-reminders`, `/call-reminders/{id}`, `…/results` |
| `/lien-he-truc-tiep`, `…/{id}/ket-qua` | `/direct-contacts`, `…/{id}/results` |
| `/lich-su-thong-bao`, `/loi-gui` | `/notification-history`, `/delivery-errors` |
| `/kenh-thong-bao`, `/kenh-thong-bao/{ma}` | `/notification-channels`, `/notification-channels/{code}` |
| tag `hop-thu`, `khan-cap`, `lien-he`, `tra-cuu`, `cau-hinh` | `inbox`, `urgent`, `direct-contact`, `lookup`, `configuration` |

### 15.2. Schema, tham số và trường JSON

| Tên logic | Mã |
| --- | --- |
| tham số hộp thông báo `muc`, `chuaXem`, `loaiSuKien`, `nctId`, `sau`; trang `soChuaXem`, `soKhanCapChuaXacNhan`, `muc`, `tiep` | `level`, `unread`, `eventType`, `residentId`, `cursor`; `unreadCount`, `unacknowledgedUrgentCount`, `items`, `nextCursor` |
| thiết bị đẩy `ungDung`, `fcmToken` | `app`, `fcmToken` |
| `Muc` | `NotificationLevel` (như khuôn `common.notification`) |
| `ThongBao` (`muc`, `loaiSuKien`, `tieuDe`, `noiDung` {`loaiThongTin`, `noiDung`}, `phanBiAn`, `chiNhanDang`, `trangThai`, `guiLuc`, `xemLuc`, `xacNhanLuc`, `cachXacNhan`, `banGhiNguon` {`loai`, `id`, `moDuoc`}, `coTheXacNhanVaTiepNhan`) | `Notification` (`level`, `eventType`, `title`, `content` {`informationType`, `content`}, `hiddenParts`, `identityOnly`, `status`, `sentAt`, `viewedAt`, `acknowledgedAt`, `acknowledgementMethod`, `sourceRecord` {`type`, `id`, `canOpen`}, `canAcknowledgeAndTakeCharge`) |
| kết quả xác nhận và tiếp nhận `xacNhan`, `tiepNhan` {`ketQua`, `maLoi`, `chiTiet`} | `acknowledgement`, `takeCharge` {`result`, `errorCode`, `details`} |
| `LuotNhacGoi` (`chuoiGoiId`, `nguoiCanGoi` {`loai`, `hoTen`, `quanHe`, `soDienThoai`}, `nctId`, `noiDungCanBao` {`thoiDiem`, `noiDung`}, `nguoiDuocNhacHienTai`, `hanLuc`, `trangThai`, `lyDoDung`, `chuyenNhac`, `ketQua`, `coQuyenGhiKetQua`) | `CallReminder` (`callChainId`, `personToCall` {`type`, `fullName`, `relationship`, `phoneNumber`}, `residentId`, `contentToReport` {`occurredAt`, `content`}, `currentlyReminded`, `dueAt`, `status`, `stopReason`, `handoffs`, `results`, `canRecordResult`) |
| `KetQuaGoi` (`ketQua`, `goiLuc`, `soDaGoi`, `lyDo`, `nguoiNgheMay`, `ghiChu`, `boSung`) | `CallResult` (`result`, `calledAt`, `calledPhoneNumber`, `reason`, `answeredBy`, `note`, `supplementary`) |
| `LienHeTrucTiep` (`hoTen`, `quanHe`, `soDienThoai`, `noiDungRutGon`, `trangThai`, `taoLuc`); thân ghi kết quả `ketQua`, `thoiDiem`, `lyDo`, `ghiChu`; tham số `trangThai` | `DirectContact` (`fullName`, `relationship`, `phoneNumber`, `shortContent`, `status`, `createdAt`); `result`, `occurredAt`, `reason`, `note`; `status` |
| `DongLichSu` (`yeuCauId`, `nguonKichHoat`, `banGhiNguon`, `muc`, `nguoiNhan`, `canCu`, `doiTuongThongTin`, `maTruongHop`, `tiepNhanLuc`, `guiLuc`, `kenh` {`kenh`, `ketQua`, `thoiDiem`}, `xemLuc`, `xacNhanLuc`, `luotNhacGoi`, `dau`) | `NotificationHistoryEntry` (`requestId`, `triggerSource`, `sourceRecord`, `level`, `recipient`, `basis`, `informationSubject`, `q354Case`, `receivedAt`, `sentAt`, `channels` {`channel`, `result`, `occurredAt`}, `viewedAt`, `acknowledgedAt`, `callReminders`, `flags`) |
| tham số lịch sử `loaiBanGhiNguon`, `idBanGhiNguon`, `nguoiNhanId`, `tangKhuVucId`, `tu`, `den`; tham số lỗi gửi `loai` | `sourceRecordType`, `sourceRecordId`, `recipientId`, `floorAreaId`, `from`, `to`; `type` |
| `Kenh` (`ma`, `ten`, `bat`, `mucGan`); thân sửa `lyDo` | `NotificationChannel` (`code`, `name`, `enabled`, `levels`); `reason` |
| `Loi`, response `Loi`, `LoiChan`; security scheme `phien` | `Problem`, response `Problem`, `Blocked`; `session` |

### 15.3. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| ứng dụng `APP_NHAN_VIEN`, `APP_NGUOI_THAN`, `APP_NGUOI_CAO_TUOI` | `STAFF_APP`, `FAMILY_APP`, `RESIDENT_APP` (như 7.3) |
| trạng thái thông báo `CHO_HET_GIO_YEN_TINH`, `KHONG_GUI`, `DA_GUI`, `GUI_THAT_BAI`, `DA_XEM`, `DA_XAC_NHAN`, `KHONG_LIEN_LAC_DUOC` | `HELD_FOR_QUIET_HOURS`, `NOT_SENT`, `SENT`, `DELIVERY_FAILED`, `VIEWED`, `ACKNOWLEDGED`, `UNREACHABLE` |
| cách xác nhận `TRONG_UNG_DUNG`, `QUA_CUOC_GOI`, `XAC_NHAN_MUON`, `XAC_NHAN_MUON_QUA_CUOC_GOI` | `IN_APP`, `BY_CALL`, `LATE`, `LATE_BY_CALL` |
| kết quả tiếp nhận `THANH_CONG`, `TU_CHOI`; xác nhận `DA_XAC_NHAN` | `SUCCEEDED`, `REJECTED`; `ACKNOWLEDGED` |
| người cần gọi `NHAN_VIEN`, `NGUOI_THAN` | `STAFF`, `FAMILY_MEMBER` (như loại tài khoản 7.3) |
| trạng thái lượt nhắc gọi `CHO_KET_QUA`, `DA_CO_KET_QUA`, `DA_DUNG` | `AWAITING_RESULT`, `RESULT_RECORDED`, `STOPPED` |
| kết quả gọi `DA_LIEN_LAC`, `KHONG_LIEN_LAC`; lý do `KHONG_NGHE_MAY`, `SAI_SO`, `KHONG_CO_SO` | `REACHED`, `NOT_REACHED`; `NO_ANSWER`, `WRONG_NUMBER`, `NO_NUMBER` |
| liên hệ trực tiếp `CHO_LIEN_HE`, `DA_LIEN_HE`, `KHONG_LIEN_HE_DUOC` | `PENDING`, `CONTACTED`, `UNREACHABLE` |
| loại lỗi gửi `BI_TU_CHOI`, `THIEU_MUC`, `THIEU_LOAI_THONG_TIN`, `KHONG_CO_NGUOI_NHAN`, `GUI_THAT_BAI`, `NOI_DUNG_KHONG_HOP_LE` | `REJECTED`, `MISSING_LEVEL`, `MISSING_INFORMATION_TYPE`, `NO_RECIPIENT`, `DELIVERY_FAILED`, `INVALID_CONTENT` |
| mã kênh `TRONG_UNG_DUNG`, `CONG_NGUOI_THAN`, `TIN_NHAN`, `KHAC_*` | `IN_APP`, `FAMILY_PORTAL`, `SMS`, `OTHER_*` |
| loại bản ghi đính chính `M13.KET_QUA_GOI`, `M13.KET_QUA_LIEN_HE` | `M13.CALL_RESULT`, `M13.DIRECT_CONTACT_RESULT` |

### 15.4. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `KHONG_PHAI_KHAN_CAP`, `DA_XAC_NHAN` | `NOT_URGENT`, `ALREADY_ACKNOWLEDGED` |
| `LUOT_DA_KET_THUC`, `KHONG_CO_QUYEN_GOI` | `CALL_REMINDER_ENDED`, `NOT_ALLOWED_TO_CALL` |
| `TRANG_THAI_CUOI`, `BAT_BUOC_TRUC_TUYEN` | `FINAL_STATE`, `REQUIRES_ONLINE` (như lý do đồng bộ 7.5) |
| *(bổ sung 2026-10-22)* ẩn thông báo chưa xem (FR-036) | `NOTIFICATION_NOT_VIEWED` (409) |

### 15.5. Gói và kiểu Java của `notification` (T002 → T011)

| Tên logic | Mã |
| --- | --- |
| `notification.tiepnhan`, `notification.gui`, `notification.lich`; phần dùng chung; provider | `notification.internal.intake`, `notification.internal.delivery`, `notification.scheduling` + `notification.internal.job`; `notification.internal.domain`, `notification.internal.persistence`; `notification.internal.governance` |
| `notification.nguoinhan`, `yentinh`, `khancap`, `hopthu`, `lienhe`, `tracuu`, `cauhinh` (tạo khi có lớp) | `notification.internal.recipient`, `quiethours`, `urgent`, `inbox`, `directcontact`, `lookup`, `channel` |
| `ThongBaoTraCuuApi` (`theoNguon`, `xemLuc`), `NguoiDaThongBaoView` | `NotificationLookupApi` (`bySource`, `viewedAt`), `NotifiedPersonView` (`ChannelResult`, `CallReminderSummary`, `CallOutcome`; `missingLevel`) *(2026-10-27, Q-388: `CallReminderSummary` thêm người cần gọi `personToCallType`, `personToCallId`)* |
| `loai_nguoi_nhan` (`NHAN_VIEN`, `NGUOI_THAN`, `NGUOI_CAO_TUOI`); trạng thái thông báo; cách xác nhận | `RecipientType` (`STAFF`, `FAMILY_MEMBER`, `RESIDENT`); `NotificationStatus`; `AcknowledgementMethod` (giá trị ở 15.3) |
| SPI `NguoiThanNhanThongBaoPort` (`quanHeHieuLuc`, `thuTuGoi`, `lienHe`), `QuanHeNhanThongBaoView`, `LienHeNguoiThanView` | `FamilyNotificationPort` (`activeRelationships`, `callOrder`, `contact`), `NotifiableRelationshipView` (`primaryContact`, `representative`, `rights`, `portalAccountId`, `portalAccountActive`, `phoneNumber`), `FamilyContactView` |
| SPI `NhaCungCapTinNhan` (`gui`), `KetQua`; bean `ChuaCauHinh` | `SmsProvider` (`send`), `SmsResult` (`SENT`, `TEMPORARY_FAILURE`, `PERMANENT_FAILURE`, `NOT_CONFIGURED`); `UnconfiguredSmsProvider` (T026) |
| sự kiện `ThongBaoDaXem`, `ThongBaoKhanCapDaXacNhan`, `ThongBaoKhongLienLacDuoc` | `NotificationViewed`, `UrgentNotificationAcknowledged`, `NotificationUnreachable` |
| `NhomNhan` loại (a): `DieuDuongPhuTrach`, `NhanVienPhanCong`, `NhanVienCoCa` (`chungChi`), `NguoiPhuTrachCa`, `BacSiTruc`, `TruongTang`, `TruongDoan`, `PhuTrachBoPhan` | `RecipientGroup.DutyGroup` (`substitution`): `AssignedNurse`, `AssignedStaff`, `OnShiftStaff` (`certificates`), `ShiftLead`, `OnDutyDoctor`, `FloorManager`, `TripLeader`, `DepartmentLead` |
| `NhomNhan` (d), (e): `NhanVien`, `NguoiThan` (`canCu`), `NguoiLienHeChinh`, `NguoiDaiDien`, `NguoiThanCoQuyen` | `Staff`, `FamilyMember` (`basis`), `PrimaryContact`, `Representative`, `FamilyWithRight` |
| `QuyenNguoiThan` (`XEM_SUC_KHOE`, `XEM_CHI_PHI`, `DANG_KY_THAM`, `NHAN_KHAN`); `TRUONG_SUC_KHOE` | `common.notification.FamilyRight` (`VIEW_HEALTH`, `VIEW_COSTS`, `REGISTER_VISITS`, `RECEIVE_URGENT`); `HealthFields.KEYS` |
| `TiepNhanListener`, kiểm tra yêu cầu, kết quả tiếp nhận, dấu | `NotificationIntakeListener`, `NotificationIntake`, `RequestValidation`, `ValidatedRequest`, `IntakeResult` (`VALID`, `REJECTED`), `RequestFlag`; `NotificationRequestStore` |
| lý do từ chối: thiếu bản ghi nguồn, khóa sự kiện, nhóm người nhận, nội dung; sự kiện nguồn dựng yêu cầu lỗi | `MISSING_SOURCE_RECORD`, `MISSING_EVENT_KEY`, `MISSING_RECIPIENT_GROUPS`, `MISSING_CONTENT`; `REQUEST_BUILD_FAILED` |
| dấu `THIEU_MUC`, `THIEU_LOAI_THONG_TIN`, `THIEU_RUT_GON`, `KHONG_CO_NGUOI_NHAN`, `VIEC_SAU_QUA_DOI`, `THIEU_DOI_TUONG_THONG_TIN`, `THIEU_MA_TRUONG_HOP` | `MISSING_LEVEL`, `MISSING_INFORMATION_TYPE`, `MISSING_SHORT_CONTENT`, `NO_RECIPIENT`, `AFTER_DEATH`, `MISSING_INFORMATION_SUBJECT`, `MISSING_Q354_CASE` |
| `BoPhatKenh`, bộ chuyển kênh, kênh trong ứng dụng, FCM | `DeliveryDispatcher`, `ChannelSender`, `InAppSender`, `PushGateway` (`FcmPushGateway`, `UnconfiguredPushGateway`), `PushGatewayConfig`; `DeliveryStore`, `AttemptContext`, `SendResult` |
| `TrangThaiThongBao`; loại sự kiện `GIU`, `TAI_DINH_THOI`, `GUI`, `KHONG_GUI`, `XEM`, `XAC_NHAN`, `KHONG_LIEN_LAC_DUOC`, `AN` | `NotificationStatusRules` (`EventFact`, `AttemptFact`, `State`); `NotificationEventType` (`HOLD`, `RESCHEDULE`, `SEND`, `NOT_SENT`, `VIEW`, `ACKNOWLEDGE`, `UNREACHABLE`, `HIDE`) |
| kết quả lần gửi `DA_GUI`, `THAT_BAI`, `KENH_CHUA_CAU_HINH`, `KHONG_CO_DIA_CHI`, `BO_QUA_NGOAI_CA`, `DA_GOP`, `NOI_DUNG_KHONG_HOP_LE` | `DeliveryOutcome` (`SENT`, `FAILED`, `CHANNEL_NOT_CONFIGURED`, `NO_ADDRESS`, `SKIPPED_OFF_SHIFT`, `BATCHED`, `INVALID_CONTENT`) |
| `ViecNhanhM13`, `ViecHangNgayM13` | `M13FastJob` (`M13_FAST`), `M13DailyJob` (`M13_DAILY`); `M13Jobs`, `M13FastTask`, `M13DailyTask`, `JobRunStore`; cấu hình `eldercare.jobs.m13-fast.cron`, `eldercare.jobs.m13-daily.cron`, `…on-startup` |
| `PhanLoaiTruongProvider`, `LuuTruProvider`, `GanPhamViProvider` của 009 | `NotificationFieldClassification` (`M13.NOTIFICATION`), `NotificationRequestRetention` (`M13.NOTIFICATION_REQUEST`), `NotificationScopeAssignment` (`M13.NOTIFICATION`) |
| *(bổ sung 2026-10-22, Phase 3)* `XacDinhNguoiNhan`, `ChuoiThay` (bậc thay), `LocPhanNoiDung`, `XetPhamViDongY`, `NguoiNhanDaXacDinh`; tạo thông báo lúc tiếp nhận; kết quả nhóm `CO_NGUOI_NHAN`, `DA_THAY`, `BO_QUA_KHONG_THAY`, `KHONG_CO` | gói `notification.internal.recipient`: `RecipientResolver` (`Resolution`), `SubstitutionChain` (`SubstitutionStep` `SHIFT_LEAD`, `FLOOR_MANAGER`, `FLOOR_MANAGER_SUBSTITUTE`, `MANAGERS`), `ContentFilter`, `ConsentScope` (`Decision` `SEND`, `SEND_WITH_CASE`, `SKIP`, `MISSING_CASE`), `ResolvedRecipient`; `NotificationCreation`; `HAS_RECIPIENTS`, `SUBSTITUTED`, `SKIPPED_NO_SUBSTITUTE`, `NONE` |
| *(bổ sung 2026-10-22, Phase 7)* `HopThuService`, `LuongThongBaoController`, đăng ký thiết bị đẩy; `TraCuuService`, lịch sử thông báo, lỗi gửi | gói `notification.internal.inbox`: `InboxService` (`Page`, `Opened`), `InboxStore`; bản ghi `InboxItem`, `InboxCursor`, `InboxFilter` (`notification.internal.inbox.domain`); `NotificationStreams` (SSE, `notification.internal.delivery`), `DeliveryStore.registerToken`; `InboxController`; gói `notification.internal.lookup`: `NotificationLookupService` (`HistoryEntry`), `LookupStore`; bản ghi `SentNotification`, `HistoryFilter`, `DeliveryError` (`notification.internal.lookup.domain`); `NotificationLookupController` (`ErrorType`); phản hồi `InboxBody`, `NotificationBody`, `NotificationHistoryBody`; thân `PushDeviceBody` |
| *(bổ sung 2026-10-23, Phase 6)* `GioYenTinh`, giữ và giải phóng, tính lại khi đổi CFG-M13-02; việc 20 giây giải phóng; `NhomNhan.NguoiCaoTuoi`; lý do từ chối nhóm (f) sai; lý do Không gửi khi giải phóng | gói `notification.internal.quiethours`: `QuietHours` (`Value`, `endIfWithin`), `QuietHoursPolicy`, `QuietHoursRelease` (`RELEASE_QUIET_HOURS`), `QuietHoursRescheduler`, `HeldNotificationStore`; `RecipientGroup.Resident`; `INVALID_RESIDENT_GROUP`; `RECIPIENT_NO_LONGER_ELIGIBLE` (`RESIDENT_DECEASED` khi có 001) |
| test `GioYenTinhTest`, `BR_M13_03_GioYenTinhIT`, `FR_024_DoiThamSoIT`, `FR_022a_NguoiCaoTuoiNhanIT` | `QuietHoursTest`, `BR_M13_03_QuietHoursIT`, `FR_024_ParameterChangeIT`, `FR_022a_ResidentRecipientIT` |
| *(bổ sung 2026-10-23, Phase 5)* `ChonKenh`, `KhungGop`, `KiemTraNoiDungKenhNgoai`, bean `ChuaCauHinh`, kênh tin nhắn; việc 20 giây đóng khung gộp, gửi lại kênh ngoài; số điện thoại người nhận; dấu nội dung kênh ngoài không hợp lệ | `ChannelSelection` (`Channel`, `Recipient`, `Planned`), `SmsBatching`, `OutOfAppContent` (`notification.internal.delivery.domain`); `UnconfiguredSmsProvider`, `SmsSender`, `OutOfAppPlanner`, `SmsWindowCloser` (`CLOSE_SMS_WINDOWS`), `OutOfAppResender` (`RESEND_OUT_OF_APP`), `SmsWindowStore`; `RecipientPhones`; `RequestFlag.INVALID_OUT_OF_APP_CONTENT` |
| *(bổ sung 2026-10-23, Phase 5)* `LienHeTrucTiepService`, phát hiện mục cần liên hệ, nhắc đầu ngày, đính chính `M13.KET_QUA_LIEN_HE`; `KenhService`; hành động nhật ký | gói `notification.internal.directcontact`: `DirectContactService` (`Result`), `DirectContactDetector` (`DIRECT_CONTACTS`), `DirectContactReminder` (`REMIND_DIRECT_CONTACTS`, sự kiện `DirectContactsPending`, khóa `DIRECT_CONTACT_PENDING:<ngày>`), `DirectContactStore`, `DirectContact`; `DirectContactResultCorrection` (`M13.DIRECT_CONTACT_RESULT`), `DirectContactFieldClassification` (`M13.DIRECT_CONTACT`); gói `notification.internal.channel`: `ChannelService`, `ChannelStore`, `ChannelConfig`; `DIRECT_CONTACT_RECORDED`, `NOTIFICATION_CHANNEL_CHANGED`; lỗi `FINAL_STATE` |
| SPI `HoSoNhanVienApi.soDienThoai` *(2026-10-23, luật "Đổi api")* | `StaffProfileApi.phoneNumber` |
| *(bổ sung 2026-10-23, US1 phần xác nhận)* `KhanCapService` phần xác nhận, `KhanCapController`; hành động nhật ký xác nhận; test `NhomGhiNhanCoTriggerIT` của 009 | `notification.internal.urgent.service.UrgentAcknowledgement`, `notification.web.UrgentController`; `NOTIFICATION_ACKNOWLEDGED`; `M13RecordGroupTriggerIT` |
| bản giả 012, nhà cung cấp tin nhắn giả, FCM giả | `support/fake/notification/FakeFamilyNotifications`, `FakeSmsProvider`, `FakeM13Config`; `notification/FakePushGateway` (test) |
| test `TrangThaiThongBaoTest`, `ChayBuM13IT`, `FR_003_KiemTraYeuCauIT`, `FR_005_ChongTrungIT`, `FR_006_LoiKhongHongNguonIT` | `NotificationStatusRulesTest`, `NFR_04_M13CatchUpIT`, `FR_003_RequestValidationIT`, `FR_005_DeduplicationIT`, `FR_006_SourceUnaffectedIT`; thêm `RequestValidationTest`, `NFR_03_DeliveryDispatcherIT`, `M13ProvidersIT`, `M13ArchTest` |
| biến môi trường khóa dịch vụ FCM | `FIREBASE_CREDENTIALS` (`eldercare.notification.fcm.credentials-file`) |
| *(bổ sung 2026-10-26, luật "Đổi api" trên M09, chủ dự án duyệt)* `CaTrucApi.nhanVienTrongCa(tang, t)`, `CaTrucApi.bacSiTruc(t)` | `ShiftApi.staffOnShift(floorAreaId, at)`, `ShiftApi.onDutyDoctors(at)`; bản giả `FakeShifts.onDutyDoctor(shiftId, staffId)`, `reset()` |
| *(bổ sung 2026-10-26, nối ca trực)* `NguoiNhacGoi` (bậc Người phụ trách ca, Trưởng tầng được giao, Quản lý viện; người trong thứ tự) | `notification.internal.urgent.domain.CallReminderTarget` (`Tier` `SHIFT_LEAD`, `FLOOR_MANAGER`, `MANAGERS`; `Candidate`, `Target`, `of`) |
| test `NguoiNhacGoiTest`, `SC_004_NguoiNhanTest`, `FR_016a_NgoaiCaIT`, `BR_M13_04_NguoiNhanTheoPhanCongIT` (phần nhóm có ca, Bác sĩ trực) | `CallReminderTargetTest`, `SC_004_RecipientTableIT`, `FR_016a_OffShiftIT`, `BR_M13_04_ShiftGroupsIT` |
| *(bổ sung 2026-10-26, US1 lượt nhắc gọi)* `KhanCapService` phần lượt nhắc gọi: tạo lượt, chuyển nhắc, kết quả gọi, xem lượt; bậc người được nhắc; người có quyền gọi; đính chính `M13.KET_QUA_GOI` | gói `notification.internal.urgent`: `CallReminderCreation` (việc `CREATE_CALL_REMINDERS`), `CallReminderHandoffs` (việc `HAND_OFF_CALL_REMINDERS`), `CallResultService` (`record`, `CallResultInput`), `CallReminderQueries` (`mine`, `detail`, `CallReminderView`), `CallReminderActions`, `CallReminderRouting` (`Assignment`, `canCall`), `CallReminderNotices` (nguồn `M13.CALL_REMINDER`), `CallResultCorrection` (`M13.CALL_RESULT`); `CallReminderTarget.after`; kho `CallReminderStore` |
| loại sự kiện thông báo của lượt nhắc: nhắc gọi, đã chuyển, đã dừng, cập nhật nội dung, người khác đã ghi kết quả, tới Quản lý viện, hết người trong chuỗi | `CALL_REMINDER`, `CALL_REMINDER_HANDED_OFF`, `CALL_REMINDER_STOPPED`, `CALL_REMINDER_UPDATED`, `CALL_RESULT_RECORDED`, `CALL_REMINDER_AT_MANAGER`, `CALL_CHAIN_NOT_REACHED`; hành động nhật ký `CALL_RESULT_RECORDED` (thực thể `M13.CALL_REMINDER`) |
| lý do dừng "người nhận đã tự xác nhận"; lý do Không liên lạc được của thông báo | `SELF_ACKNOWLEDGED`; `CALL_NOT_REACHED` |
| body `LuotNhacGoi`, `KetQuaGoi` | `CallReminderBody` (`PersonToCall`, `ContentToReport`), `CallResultBody` |
| test `BR_M13_02_LuotNhacGoiIT`, `FR_027a_GopLuotIT`, `FR_028_ThatBaiMoiKenhIT`, `FR_030a_ChuyenNhacIT`, `FR_032_MotLuotIT`; `FR_029a_ChuoiGoiIT`, kết quả gọi | `BR_M13_02_CallReminderIT`; `FR_029a_CallResultIT`; dựng chung `CallReminderTestBase` |

### 15.6. Bảng và cột (T006, `V009_01__notification.sql`)

| Tên logic | Mã |
| --- | --- |
| `yeu_cau_thong_bao` (`module_nguon`, `loai_su_kien`, `nhom_nguon`, `loai_ban_ghi_nguon`, `id_ban_ghi_nguon`, `khoa_su_kien`, `muc`, `nct_id`, `tang_khu_vuc_id`, `noi_dung_rut_gon`, `tieu_de`, `nguoi_gay_ra_id`, `bao_nguoi_gay_ra`, `phat_sinh_luc`, `tiep_nhan_luc`, `ket_qua_tiep_nhan`, lý do, `dau`, `doi_tuong_thong_tin`, `ma_truong_hop`) | `notification_request` (`source_module`, `event_type`, `source_group`, `source_record_type`, `source_record_id`, `event_key`, `level`, `resident_id`, `floor_area_id`, `short_content`, `title`, `caused_by`, `notify_causer`, `occurred_at`, `received_at`, `intake_result`, `rejection_reason`, `flags`, `information_subject`, `q354_case`) |
| `cach_thay` `MAC_DINH`, `KHONG_THAY`, `NGUOI_THAY_CHI_DINH`; kết quả nhóm `CO_NGUOI`, `THAY`, `BO_QUA_KHONG_THAY`, `KHONG_CO` | `DEFAULT`, `NO_SUBSTITUTE`, `DESIGNATED`; `HAS_RECIPIENTS`, `SUBSTITUTED`, `SKIPPED_NO_SUBSTITUTE`, `NONE` |
| `thong_bao` (`loai_nguoi_nhan`, `nguoi_nhan_id`, `tai_khoan_id`, `can_cu`, `phan_duoc_xem`, `chi_nhan_dang`, `ma_truong_hop`, `muc`, `tao_luc`) | `notification` (`recipient_type`, `recipient_id`, `account_id`, `basis`, `visible_parts`, `identity_only`, `q354_case`, `level`, `created_at`) |
| `thiet_bi_day` (`tai_khoan_id`, `ma_thiet_bi`, `ung_dung`, `fcm_token`, `cap_nhat_luc`, `vo_hieu_luc`), `lan_chay_m13` | `push_device` (`account_id`, `device_id`, `app`, `fcm_token`, `updated_at`, `invalidated_at`), `m13_job_run` (`job`, `business_date`, `scheduler_run_id`, `completed_at`) |
| *(bổ sung 2026-10-26, `V009_04__call_reminders.sql`)* `luot_nhac_goi` (`chuoi_goi_id`, `loai_nguoi_can_goi`, `nguoi_can_goi_id`, `nct_id`, `nguon_khi_khong_nct`, `tang_khu_vuc_id`, `tao_luc`, `ly_do_tao` `QUA_HAN_XAC_NHAN`, `GUI_THAT_BAI`, `KHONG_LIEN_LAC_NGUOI_TRUOC`) | `call_reminder` (`call_chain_id`, `callee_type`, `callee_id`, `resident_id`, `source_record_type`, `source_record_id`, `floor_area_id`, `created_at`, `creation_reason` `UNACKNOWLEDGED`, `DELIVERY_FAILED`, `PREVIOUS_NOT_REACHED`) |

### 15.7. Bảng sau gộp R-15 *(bổ sung 2026-10-30; thay các dòng bảng đã bỏ ở 15.6; cột chi tiết ở data-model 009 mục 0.1)*

| Tên logic | Mã |
| --- | --- |
| nội dung, nhóm nhận của `yeu_cau_thong_bao` | `notification_request.contents`, `notification_request.recipient_groups` |
| trạng thái của `thong_bao`, `luot_nhac_goi` (cột dẫn xuất) | `notification.status`, `scheduled_send_at`, `sent_at`, `viewed_at`, `acknowledged_at`, `acknowledgement_method`, `hidden`, `recipient_key`; `call_reminder.status`, `final_result`, `stop_reason`, `due_at`, `current_staff_id`, `callee_key`, `pending_key` |
| cột sinh cho truy vấn của 009 (T044) | `record_event.m13_temporary_failure` (kết quả lần gửi lỗi tạm); `notification.urgent_unacknowledged` (Khẩn cấp chưa xác nhận) |
| loại bảng chung của 009 | `M13.NOTIFICATION_CHANNEL` (danh mục); `M13.NOTIFICATION_EVENT`, `M13.DELIVERY_ATTEMPT`, `M13.DELIVERY_RESULT`, `M13.CALL_REMINDER_NOTIFICATION`, `M13.CALL_REMINDER_HANDOFF`, `M13.CALL_RESULT`, `M13.DIRECT_CONTACT_RESULT` (sự kiện); `M13.DIRECT_CONTACT`, `M13.SMS_BATCH_WINDOW` (hồ sơ chung); `M13.REQUEST_DUPLICATE`, `M13.JOB_DONE` (mốc đã nhắc) |


## 16. Feature 010 (`010-incurred-costs`, E) — bản dịch đã duyệt 2026-10-29

A/chủ dự án đã duyệt bản dịch hợp đồng 010 và bảng tên qua xác nhận của E trong phiên 2026-10-29. Đã bắt đầu phần hàm thuần M11; chưa cài HTTP. Mã FR/BR/DBR/CFG/Q giữ nguyên. Tên của API thuộc B/C/D do chủ nguồn chốt; không dùng bảng này để tự khai báo hoặc sửa API của họ. "Chốt" dịch là `finalize` theo yêu cầu tích hợp 016 ở team-plan mục 8.

### 16.1. Đường dẫn HTTP (giữ nguyên phương thức và phạm vi)

| Tên logic | Tên tiếng Anh đề xuất |
| --- | --- |
| `/chi-phi/ban-ghi-nguon` | `/costs/billing-sources` |
| `/chi-phi/khoan/theo-su-kien` | `/costs/items/from-events` |
| `/chi-phi/khoan/nhap-tay` | `/costs/items/manual` |
| `/chi-phi/khoan/{id}/dieu-chinh` | `/costs/items/{id}/adjust` |
| `/chi-phi/khoan/{id}` | `/costs/items/{id}` |
| `/chi-phi/khoan/kiem-tra` | `/costs/items/check` |
| `/chi-phi/khoan/{id}/bo-kiem-tra` | `/costs/items/{id}/uncheck` |
| `/chi-phi/khoan/duyet` | `/costs/items/approve` |
| `/chi-phi/khoan/{id}/tra-lai` | `/costs/items/{id}/return` |
| `/chi-phi/khoan/{id}/huy` | `/costs/items/{id}/cancel` |
| `/chi-phi/bien-dong-sau-chot` | `/costs/post-finalization-changes` |
| `/chi-phi/bang` | `/costs/statements` |
| `/chi-phi/bang/{id}` | `/costs/statements/{id}` |
| `/chi-phi/bang/{id}/gui-chot` | `/costs/statements/{id}/submit-for-finalization` |
| `/chi-phi/bang/{id}/tra-lai` | `/costs/statements/{id}/return` |
| `/chi-phi/bang/{id}/chot` | `/costs/statements/{id}/finalize` |
| `/chi-phi/ky` | `/costs/periods` |
| `/de-nghi-mua-ho` | `/purchase-assistance-requests` |
| `/de-nghi-mua-ho/{id}/gui` | `/purchase-assistance-requests/{id}/send` |
| `/de-nghi-mua-ho/{id}/dong-y` | `/purchase-assistance-requests/{id}/agree` |
| `/de-nghi-mua-ho/{id}/tu-choi` | `/purchase-assistance-requests/{id}/reject` |
| `/de-nghi-mua-ho/{id}/xin-bo-sung` | `/purchase-assistance-requests/{id}/request-additional-consent` |
| `/de-nghi-mua-ho/{id}/rut-bo-sung` | `/purchase-assistance-requests/{id}/withdraw-additional-consent` |
| `/de-nghi-mua-ho/{id}/huy` | `/purchase-assistance-requests/{id}/cancel` |
| `/de-nghi-mua-ho/{id}/ghi-da-mua` | `/purchase-assistance-requests/{id}/record-purchased` |
| `/nguoi-cao-tuoi/{nctId}/chi-phi/bang` | `/residents/{residentId}/costs/statements` |
| `/nguoi-cao-tuoi/{nctId}/chi-phi/tam-tinh` | `/residents/{residentId}/costs/provisional` |
| `/chi-phi/ky/{id}/xuat` | `/costs/periods/{id}/export` |

### 16.2. Schema, lỗi và thành phần hợp đồng

| Tên logic | Tên tiếng Anh đề xuất |
| --- | --- |
| `LyDo` | `Reason` |
| `LoaiNguon` | `BillingSourceType` |
| `LoaiChiPhi` | `CostType` |
| `Loi` | `Problem` |
| `LoiChan` | `BlockingProblem` |
| `BanGhiNguon` | `BillingSource` |
| `LapTheoSuKien` | `CreateCostsFromEvents` |
| `KetQuaHangLoat` | `CostBatchResult` |
| `NhapTay` | `ManualCostItem` |
| `DieuChinh` | `CostAdjustment` |
| `ChonKhoan` | `CostItemSelection` |
| `Khoan` | `CostItem` |
| `Bang` | `CostStatement` |
| `CanhBaoBienDong` | `CostChangeWarning` |
| `DeNghiMuaHoMoi` | `NewPurchaseAssistanceRequest` |
| `DeNghiMuaHo` | `PurchaseAssistanceRequest` |
| `GhiDaMua` | `RecordPurchased` |
| `TamTinh` | `ProvisionalCostSummary` |

`NctId → ResidentId`, `phien → session`. Tag: `nguon → source`, `khoan → items`, `bang → statements`, `mua-ho → purchase-assistance`, `cung-cap → cost-access`. Request body và response dùng chung giữ tên tương ứng `Reason`, `Problem`, `BlockingProblem`.

### 16.3. Trường JSON và tham số

| Tên logic | Tên tiếng Anh đề xuất |
| --- | --- |
| `nctId` | `residentId` |
| `loai` | `type` |
| `tu` | `from` |
| `den` | `to` |
| `kyId` | `periodId` |
| `trangThai` | `status` |
| `lyDoChapNhanVuot` | `excessApprovalReason` |
| `khoanIds` | `itemIds` |
| `soTienMoi` | `newAmount` |
| `lyDo` | `reason` |
| `phamVi` | `scope` |
| `dinhDang` | `format` |
| `maLoi` | `errorCode` |
| `dieuKienChuaThoa` | `unmetConditions` |
| `loaiNguon` | `sourceType` |
| `nguonId` | `sourceId` |
| `donVi` | `unit` |
| `ngayPhatSinh` | `incurredOn` |
| `thoiDiem` | `occurredAt` |
| `moTa` | `description` |
| `soLuong` | `quantity` |
| `heSo` | `factor` |
| `dau` | `flags` |
| `khoanHienCo` | `existingItemId` |
| `nguonIds` | `sourceIds` |
| `khoangNgay` | `dateRange` |
| `thanhCong` | `succeededIds` |
| `boQua` | `skipped` |
| `khoanId` | `itemId` |
| `donGia` | `unitPrice` |
| `tepIds` | `fileIds` |
| `soTien` | `amount` |
| `loaiDieuChinh` | `adjustmentType` |
| `bienDongId` | `sourceChangeId` |
| `boLoc` | `filter` |
| `bangId` | `statementId` |
| `loaiChiPhi` | `costType` |
| `nguonSinh` | `origin` |
| `banGhiNguon` | `billingSource` |
| `maVatPham` | `itemCode` |
| `tenThuoc` | `medicationName` |
| `canCuDonGia` | `unitPriceBasis` |
| `khoanGocId` | `originalItemId` |
| `kyGoc` | `originalPeriod` |
| `lichSuTinhLai` | `recalculationHistory` |
| `dieuChinh` | `adjustments` |
| `tongHienHanh` | `currentTotal` |
| `truongBiAn` | `hiddenFields` |
| `ky` | `period` |
| `loaiBang` | `statementType` |
| `kyCuoi` | `finalPeriod` |
| `denNgay` | `throughDate` |
| `tongTheoLoai` | `totalsByType` |
| `tong` | `total` |
| `canhBaoBienDong` | `changeWarning` |
| `tyLeTang` | `increaseRatio` |
| `baLoaiTangNhieuNhat` | `topThreeIncreases` |
| `vatPhamId` | `catalogItemId` |
| `soTienDuKien` | `estimatedAmount` |
| `nguoiYeuCau` | `requester` |
| `soTienDuocPhep` | `allowedAmount` |
| `dangChoBoSung` | `additionalConsentPending` |
| `soDuKhongDu` | `insufficientBalance` |
| `quyetDinh` | `decisions` |
| `soTienThucTe` | `actualAmount` |
| `ngayMua` | `purchasedOn` |
| `tepChungTuId` | `receiptFileId` |
| `lyDoMuaGap` | `urgentPurchase` |
| `nguoiYeuCauMuaGapId` | `urgentPurchaseRequesterId` |
| `nhan` | `label` |
| `conKhoanChuaCoDonGia` | `hasUnpricedItems` |

`nguon → source` trong dòng bị bỏ qua; `khoan → items` trong bảng chi phí. `urgentPurchase` giữ kiểu boolean của `lyDoMuaGap`; không đổi thành chuỗi lý do. Chuỗi nhãn `tạm tính, chưa chốt` vẫn bằng tiếng Việt. `flags` trong bộ lọc giữ kiểu string của bản gốc, còn danh sách dấu của bản ghi giữ kiểu array; chưa thêm enum dấu hoặc trường mới.

### 16.4. Giá trị enum

| Giá trị logic | Giá trị tiếng Anh đề xuất |
| --- | --- |
| `NGAY_LUU_TRU` | `RESIDENCY_DAY` |
| `BUOI_BAN_TRU` | `DAY_CARE_SESSION` |
| `KET_QUA_CONG_VIEC` | `CARE_TASK_RESULT` |
| `LIEU` | `DOSE` |
| `LAN_DUNG_PRN` | `PRN_ADMINISTRATION` |
| `LAN_GIAO` | `DISPENSE_RECORD` |
| `SUAT_AN_NGUOI_THAN` | `FAMILY_MEAL` |
| `DEM_O_LAI` | `OVERNIGHT_STAY` |
| `DIEM_DANH_HOAT_DONG` | `ACTIVITY_ATTENDANCE` |
| `PHI_LUU_TRU` | `RESIDENCY_FEE` |
| `PHI_BUOI_BAN_TRU` | `DAY_CARE_SESSION_FEE` |
| `DICH_VU` | `SERVICE` |
| `VAT_PHAM_TIEU_HAO` | `CONSUMABLE` |
| `HOAT_DONG` | `ACTIVITY` |
| `THUOC` | `MEDICATION` |
| `NGUOI_THAN_O_LAI` | `FAMILY_OVERNIGHT_STAY` |
| `MUA_HO` | `PURCHASE_ASSISTANCE` |
| `KHOAN_KHAC` | `OTHER` |
| `DANG_MO` | `OPEN` |
| `CHO_CHOT` | `AWAITING_FINALIZATION` |
| `DA_CHOT` | `FINALIZED` |
| `NHAP` | `DRAFT` |
| `DA_KIEM_TRA` | `CHECKED` |
| `DA_DUYET` | `APPROVED` |
| `DA_HUY` | `CANCELLED` |
| `THEO_SU_KIEN` | `EVENT_BASED` |
| `NHAP_TAY` | `MANUAL` |
| `DIEU_CHINH` | `ADJUSTMENT` |
| `THUONG` | `REGULAR` |
| `BO_SUNG` | `SUPPLEMENTARY` |
| `SUA_SAI_SAU_CHOT` | `POST_FINALIZATION_CORRECTION` |
| `THEO_NGUON_DOI` | `SOURCE_CHANGE` |
| `MIEN_GIAM` | `WAIVER` |
| `BO_SUNG_PHAT_SINH_MUON` | `LATE_ADDITION` |
| `NHAN_VIEN` | `STAFF` |
| `NGUOI_THAN` | `FAMILY_MEMBER` |
| `CHO_DONG_Y` | `AWAITING_CONSENT` |
| `DUOC_PHEP_MUA` | `PURCHASE_ALLOWED` |
| `TU_CHOI` | `REJECTED` |
| `HUY` | `CANCELLED` |
| `DA_MUA` | `PURCHASED` |
| `TOAN_BO` | `ALL` |
| `CHUA_TUNG_XUAT` | `NEVER_EXPORTED` |

`DOSE`, `PRN_ADMINISTRATION`, `DISPENSE_RECORD` khớp tên nguồn đã công bố của 006; không sửa `medication.api.events.BilledSource`. Hủy khoản là `DA_HUY`, hủy đề nghị là `HUY`; cả hai dịch thành `CANCELLED` trong hai vòng đời riêng.

### 16.5. Mã lỗi có trong hợp đồng gốc

| Mã logic | Mã tiếng Anh đề xuất |
| --- | --- |
| `DIEU_KIEN_CHUA_THOA` | `CONDITION_NOT_MET` |
| `DA_CO_KHOAN` | `COST_ITEM_ALREADY_EXISTS` |
| `NGOAI_THOI_GIAN_TINH_PHI` | `OUTSIDE_BILLING_PERIOD` |
| `NGAY_CHUA_KET_THUC` | `DAY_NOT_ENDED` |
| `KHONG_PHAI_SU_KIEN_TINH_PHI` | `NOT_A_BILLABLE_EVENT` |
| `CO_TRONG_DANH_MUC` | `ITEM_IN_CATALOG` |
| `TONG_AM` | `NEGATIVE_TOTAL` |
| `CON_KHOAN_NHAP` | `DRAFT_ITEMS_REMAIN` |
| `CON_KHOAN_CHUA_DUYET` | `UNAPPROVED_ITEMS_REMAIN` |
| `CHUA_QUA_NGAY_CUOI` | `PERIOD_NOT_ENDED` |
| `BANG_DA_CHOT` | `STATEMENT_FINALIZED` |
| `VUOT_SO_DUOC_PHEP` | `ALLOWED_AMOUNT_EXCEEDED` |
| `DA_DUOC_XU_LY` | `ALREADY_PROCESSED` |
| `CAN_LY_DO_CHAP_NHAN_VUOT` | `EXCESS_APPROVAL_REASON_REQUIRED` |
| `KHONG_HO_TRO_DINH_CHINH` | `CORRECTION_NOT_SUPPORTED` |

Các mã dùng chung `CONDITION_NOT_MET`, `ALREADY_PROCESSED`, `CORRECTION_NOT_SUPPORTED` theo mục 5 của glossary. Chưa bổ sung mã lỗi ngoài hợp đồng gốc.

### 16.6. Tên Java của phần E

| Tên logic | Tên tiếng Anh dự kiến |
| --- | --- |
| `ChiPhiCongNguoiThanApi` | `FamilyPortalCostApi` |
| `ChiPhiKyCuoiApi` | `FinalPeriodCostApi` |
| `ChiPhiThongKeApi` | `CostStatisticsApi` |
| `SuKienTinhPhiNguon` | `CostSourceBilled` |
| `SuKienTinhPhiNguonDaDoi` | `CostSourceChanged` |
| `BangChiPhiDaChot` | `CostStatementFinalized` |
| `DeNghiMuaHoDaGui` | `PurchaseAssistanceSent` |
| `DeNghiMuaHoDaQuyetDinh` | `PurchaseAssistanceDecided` |
| `DeNghiMuaHoDaHuy` | `PurchaseAssistanceCancelled` |
| `DeNghiMuaHoDaMua` | `PurchaseAssistancePurchased` |
| `KhoanChiPhiDaDoi` | `CostItemChanged` |
| `ChiPhiChoSoDuPort` | `CostForBalancePort` |
| `SoDuMuaHoPort` | `PurchaseBalancePort` |
| `LamTronDong` | `DongRounding` |
| `PhiNgayLuuTru` | `ResidencyDayFee` |
| `PhiBuoiBanTru` | `DayCareSessionFee` |
| `ThoiGianTinhPhi` | `BillingPeriod` |

Đã cài `DongRounding`, `ResidencyDayFee`, `DayCareSessionFee` tại `billing.internal.pricing.domain`. `round` trả BigDecimal số nguyên đồng; `base` tính giá cơ sở. Theo Q-395, `ResidencyDayFee.monthly(monthlyPrice, date)` và `direct(dailyPrice)` dùng đủ giá, không nhận hệ số. Chỉ `DayCareSessionFee.monthly`/`direct` áp `factorPercent` từ nguồn; tham số này là phần trăm (30 nghĩa là 30%, theo data-model 010); `scheduledSessionCount` là số buổi có lịch sau lọc ngày khu nghỉ do nguồn cung cấp; `lastScheduledSession` xác định buổi có lịch cuối tháng. Chưa có adapter đọc/lọc nguồn hoặc `BillingPeriod`. T003/T004 đã khai báo các API/port/sự kiện bên trên và fake chung; chưa có bản cài thật hoặc nơi phát sự kiện.

Ba API công khai và sự kiện đặt trong `billing.api`, `dto`, `events`; bản giả do E giữ tại `support/fake/billing/`. Port số dư nội bộ chỉ cho phần 017/019 của M11. Không tạo gói rỗng; lớp hàm thuần đặt trong `billing.internal.pricing.domain`, các tầng khác theo backend/README.md. Web `features/charges`, app `family/charges` theo README; 010 không có màn app nhân viên.

#### 16.6.1. Kiểu và hàm trao đổi đã khai báo T003/T004

| Tên logic / ý nghĩa | Tên tiếng Anh |
| --- | --- |
| Bảng chi phí, khoản chi phí, bản ghi nguồn | CostStatementView, CostItemView, BillingSourceView |
| Tổng theo loại, tạm tính | CostTypeTotal, ProvisionalCostSummaryView |
| Đề nghị mua hộ, người yêu cầu | PurchaseAssistanceView, PurchaseRequesterView |
| Điều kiện chi phí kỳ cuối | FinalPeriodConditionView (satisfied, reasons) |
| Dòng khoản/bảng/kỳ cho báo cáo | CostItemReportView, CostStatementReportView, CostPeriodView |
| Loại chi phí, loại bảng, trạng thái đề nghị | CostType, CostStatementType, PurchaseAssistanceStatus |
| Bảng đã chốt cho người thân | FamilyPortalCostApi.finalizedStatements |
| Tạm tính, đề nghị chờ đồng ý | FamilyPortalCostApi.provisional, awaitingConsent |
| Điều kiện kỳ cuối | FinalPeriodCostApi.finalPeriodCondition |
| Khoản, bảng, kỳ thống kê | CostStatisticsApi.items, statements, periods |
| Tổng chưa chốt, phí ngày cơ sở | CostForBalancePort.unfinalizedTotal, baseDailyFee |
| Số dư không đủ | PurchaseBalancePort.insufficientBalance |
| Khóa sự kiện, số lượng mới, đã hủy, căn cứ | eventKey, newQuantity, cancelled, basis |
| Mã bảng, thời điểm chốt, mã đề nghị | statementId, finalizedAt, requestId |

CostForBalancePort ở billing.internal.provision.domain; PurchaseBalancePort ở billing.internal.purchase.domain. Bản giả duy nhất: FakeFamilyPortalCosts, FakeFinalPeriodCosts, FakeCostStatistics, FakeCostForBalance, FakePurchaseBalance; cấu hình test FakeM11Config ở support.fake.billing. Tất cả chỉ là khai báo/bản giả ở lượt này. Chữ ký Java chi tiết ở contracts/module-interfaces 010 mục 4; chưa có bangDaChot của T037, chưa sửa trường/endpoint HTTP.

### 16.7. Vị trí lưu sau R-15 (2026-10-30)

Ba bảng riêng đã có trong V010_01__costs.sql; các loại ở bảng chung chỉ là ánh xạ của E, còn chờ A/000 T108 cung cấp API/bảng, không đọc/ghi trực tiếp.

| Tên logic | Vị trí thực tế sau gộp |
| --- | --- |
| ky_chi_phi | cost_statement, kind PERIOD |
| bang_chi_phi | cost_statement, kind STATEMENT |
| bang_chi_phi_tong_loai | cost_statement.totals_by_type (JSON) |
| khoan_chi_phi | cost_item |
| su_kien_tinh_phi | cost_source_index |
| chi_phi_nct | ResidentModuleStateApi, module M11, dữ liệu finalCostCompletedFrom |
| lich_su_tinh_lai | StatusHistoryApi, M11.COST_RECALCULATION |
| bien_dong_nguon_sau_chot | RecordEventApi, M11.POST_FINALIZATION_CHANGE |
| de_nghi_mua_ho | SimpleRecordApi, M11.PURCHASE_ASSISTANCE_REQUEST |
| de_nghi_gui_nguoi_dai_dien | RecordEventApi, M11.PURCHASE_RECIPIENT |
| quyet_dinh_mua_ho | RecordEventApi, M11.PURCHASE_DECISION |
| su_kien_nguon_da_xu_ly | ReminderMarkers, M11.SOURCE_PROCESSED |
| lan_xuat_ke_toan, lan_xuat_bang | Hành động nhật ký qua AuditLogApi |
| lan_chay_m11 | Structured log logs/jobs.log |

Không tạo các tên bảng legacy resident_cost_guard, cost_period, cost_statement_type_total, cost_recalculation_history, post_finalization_source_change, cost_source_event_processed, purchase_assistance_request/recipient/decision, cost_export/statement hoặc cost_scheduler_run.

### 16.8. Tên cột và dấu của T005a

| Tên logic | Tên mã |
| --- | --- |
| Loại dòng kỳ/bảng | kind: PERIOD/STATEMENT |
| Tháng, ngày đầu/cuối tháng | period_month, period_starts_on, period_ends_on |
| Kỳ, bảng, người cao tuổi | period_id, statement_id, resident_id |
| Loại bảng, kỳ cuối, tính tới ngày | statement_type, final_period, through_date |
| Hạn chốt, tổng theo loại, tổng | finalization_due_on, totals_by_type, total |
| Gửi chốt, cảnh báo biến động | submitted_by, submitted_at, change_warning |
| Chốt, tham chiếu xuất đầu, khóa dòng | finalized_by, finalized_at, first_export_id, locked_at |
| Khóa duy nhất đối chiếu bởi CHECK | unique_key |
| Loại khoản, nguồn sinh | cost_type, origin |
| Loại nguồn, mã nguồn, đơn vị tính phí | source_type, source_id, billing_unit |
| Đề nghị, dịch vụ, mã vật phẩm | purchase_assistance_id, service_id, item_code |
| Mô tả, ngày/thời điểm phát sinh | description, incurred_on, occurred_at |
| Số lượng, đơn vị, đơn giá, hệ số, tiền | quantity, unit, unit_price, factor_percent, amount |
| Căn cứ giá, giá trị nguồn đã dùng, dấu, lý do | unit_price_basis, source_values, flags, reason |
| Khoản/kỳ gốc, loại điều chỉnh, biến động nguồn | original_item_id, original_period_id, adjustment_type, source_change_id |
| Kiểm/duyệt, chấp nhận vượt, hủy | checked_by/at, approved_by/at, excess_approval_reason, cancelled_by, cancellation_reason |
| Phiên bản, lập bởi/lúc | version, created_by, created_at |
| Thuốc nguồn, số lượng hiện hành, đã hủy, cập nhật | medication_id, current_quantity, cancelled, updated_at |
| Cổng nội bộ khi tính lại | @m11_recalculating |
| Fixture nguồn chung, test schema | FakeCostSources, CostSchemaIT, CostSourceFixtureTest |

| Dấu logic | Giá trị mã (SET) |
| --- | --- |
| THUOC_GOI | INCLUDED_IN_PACKAGE |
| THIEU_DON_GIA | MISSING_UNIT_PRICE |
| CHUA_VAO_O | NOT_YET_MOVED_IN |
| LAP_TRUOC | CREATED_IN_ADVANCE |
| PHAT_SINH_THUOC_KY_DA_CHOT | AFTER_FINALIZED_PERIOD |
| VUOT_SO_DONG_Y | EXCEEDS_CONSENT |
| MUA_GAP_VUOT | URGENT_EXCESS |
| ANH_HUONG_KET_THUC | AFFECTS_TERMINATION |

Cột tiền/đơn giá là DECIMAL(15,0), số lượng DECIMAL(10,2), hệ số phần trăm DECIMAL(5,2), thời điểm DATETIME(6), id BIGINT. JSON tiền vẫn integer/int64. Kind của dòng SQL không mở rộng statementType trong REST. JSON ở bảng chung theo data-model 010 mục 0.1; chưa tự khai báo chữ ký Java của A.

## 17. Feature 011 (`011-nutrition-management`, D)

*Chủ feature điền khi dịch `contracts/api-011.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 18. Feature 012 (`012-family-portal`, E)

*Chủ feature điền khi dịch `contracts/api-012.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 19. Feature 013 (`013-personal-belongings`, B)

*Chủ feature điền khi dịch `contracts/api-013.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 20. Feature 014 (`014-activity-care-quality`, C)

*Chủ feature điền khi dịch `contracts/api-014.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 21. Feature 015 (`015-shift-rotation-swap`, E)

*Chủ feature điền khi dịch `contracts/api-015.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 22. Feature 016 (`016-reports-dashboard`, A) *(bổ sung 2026-10-24, khi dịch `contracts/api-016.yaml`)*

"Chỉ tiêu" dịch là `metric` như cột `metric_group` của 002 (mục 7.6). Lớp của 016 bổ sung vào mục này cùng thay đổi với mã (R-03). M14 không có bảng (data-model mục 1).

### 22.1. Đường dẫn API (tiền tố `/api/v1`)

| Tên logic | Mã |
| --- | --- |
| `/dashboard`, `/chi-tieu` | `/dashboard`, `/metrics` |
| `/bao-cao/{nhom}`, `…/xuat`, `/bao-cao/lan-xuat` | `/reports/{group}`, `…/export`, `/reports/exports` |
| `/chi-tiet/{maChiTieu}` | `/metric-details/{metricCode}` |
| tag `dashboard`, `bao-cao`, `xuat` | `dashboard`, `reports`, `export` |

### 22.2. Schema, tham số và trường JSON

| Tên logic | Mã |
| --- | --- |
| tham số báo cáo `nhom`, `tu`, `den`, `ngay`, `tangKhuVucId`, `loaiLuuTru`, `mucChamSocId`, `nctId`, `nhanVienId`, `caId`, `cheDoChiPhi` | `group`, `from`, `to`, `date`, `floorAreaId`, `residenceType`, `careLevelId`, `residentId`, `staffId`, `shiftId`, `costMode` |
| tham số chi tiết `nhomCon`, `tinhDen`, `trang` | `breakdownKey`, `asOf`, `page` |
| `ChiTieu` (`ma`, `nhom`, `ten`, `dinhNghia`, `nguon`) | `Metric` (`code`, `group`, `name`, `definition`, `source`) |
| `GiaTri` (`maChiTieu`, `giaTri`, `tuSo`, `mauSo`, `nhan`, `soChoXemLai`, `chiaNhom` {`khoa`, `giaTri`, `tuSo`, `mauSo`}) | `MetricValue` (`metricCode`, `value`, `numerator`, `denominator`, `labels`, `pendingReviewCount`, `breakdown` {`key`, `value`, `numerator`, `denominator`}) |
| `Dashboard` (`tinhDen`, `khoi` {`nhom`, `trangThai`, `giaTri`}, `thongDiep`) | `Dashboard` (`asOf`, `blocks` {`group`, `status`, `values`}, `message`) |
| `BaoCao` (`nhom`, `tu`, `den`, `tinhDen`, `boLoc`, `giaTri`, `ghiChu`) | `Report` (`group`, `from`, `to`, `asOf`, `filters`, `values`, `notes`) |
| `ChiTiet` (`maChiTieu`, `tongSo`, `dong` {`khoaNguon`, `feature`, `loai`, `trangThai`, `moc`, `tangKhuVuc`, `nguoiCaoTuoi`, `rutGon`, `nguoiThucHien`, `ketQua`, `choXemLai`, `moDuocBanGhi`}) | `MetricDetail` (`metricCode`, `total`, `rows` {`sourceKey`, `feature`, `type`, `status`, `referenceTime`, `floorArea`, `resident`, `reduced`, `performedBy`, `result`, `pendingReview`, `canOpenRecord`}) |
| `YeuCauXuat` (`tu`, `den`, `boLoc`, `cheDoChiPhi`, `coDinhDanh`, `mucDich`, `dinhDang`) | `ExportRequest` (`from`, `to`, `filters`, `costMode`, `includeIdentity`, `purpose`, `format`) |
| *(bổ sung 2026-10-24, chủ dự án duyệt)* kết quả xuất (`maTep`, `maNhatKy`); lần xuất (`maNhatKy`, `maTep`, `thoiDiem`, `nguoiXuat`, `vaiTro`, `nhom`, `tu`, `den`, `coDinhDanh`, `mucDich`, `soDong`) | `ExportResult` (`fileId`, `auditLogId`); `ExportRecord` (`auditLogId`, `fileId`, `exportedAt`, `exportedBy`, `role`, `group`, `from`, `to`, `includeIdentity`, `purpose`, `rowCount`) |
| `Loi`, response `Loi`; security scheme `phien` | `Problem` (như api-000), response `Problem`; `session` |

### 22.3. Giá trị enum

| Tên logic | Mã |
| --- | --- |
| nhóm báo cáo `NGUOI_CAO_TUOI`, `CHAM_SOC`, `CHAT_LUONG`, `SUC_KHOE_THUOC`, `CHI_PHI`, `NHAN_SU_CA` | `RESIDENTS`, `CARE`, `QUALITY`, `HEALTH_MEDICATION`, `INCURRED_COSTS`, `STAFFING_SHIFTS` |
| chế độ chi phí `DA_CHOT`, `TAM_TINH` | `FINALIZED`, `PROVISIONAL` |
| nhãn giá trị `KHONG_CO_DU_LIEU`, `CHUA_CAU_HINH`, `NGAY_CHUA_KET_THUC`, `CHO_XEM_LAI`, `LOAI_BO_LUU_TRU`, `TAM_TINH`, `TRONG_PHAM_VI_CUA_BAN` | `NO_DATA`, `NOT_CONFIGURED`, `DAY_NOT_ENDED`, `PENDING_REVIEW`, `PURGED_BY_RETENTION`, `PROVISIONAL`, `WITHIN_YOUR_SCOPE` |
| trạng thái khối dashboard `DU`, `DANG_CAP_NHAT` | `COMPLETE`, `UPDATING` |
| định dạng xuất `XLSX`, `CSV` | giữ nguyên |
| hành động nhật ký `XUAT_BAO_CAO`; loại tệp `M14.BAO_CAO_XUAT` | `REPORT_EXPORTED` (mục 6.5); `M14.REPORT_EXPORT` |

### 22.4. Mã chỉ tiêu (`contracts/chi-tieu.md`)

| Tên logic | Mã |
| --- | --- |
| tiền tố `NCT`, `GIUONG`, `CV`, `BUA`, `HD`, `KT`, `SK`, `THUOC`, `CA`, `GP`, `CP`, `SD`, `KB`, `TS`, `KHO` | `RES`, `BED`, `TASK`, `MEAL`, `ACT`, `QC`, `HEALTH`, `MED`, `SHIFT`, `LICENSE`, `COST`, `BALANCE`, `REGISTRATION`, `ASSET`, `INVENTORY` |
| `NCT.DANG_LUU_TRU`, `NCT.TAI_NGAY`, `NCT.BIEN_DONG`, `NCT.DANH_SACH_CHO` | `RES.IN_RESIDENCE`, `RES.AT_DATE`, `RES.MOVEMENTS`, `RES.WAITING_LIST` |
| `GIUONG.TRANG_THAI`, `GIUONG.CHO_SAN_SANG` | `BED.STATUS`, `BED.AWAITING_READY` |
| *(bổ sung 2026-10-27)* `GIUONG.CO_THE_DUNG`, `GIUONG.PHONG_CACH_LY`, `GIUONG.SO_NGUOI`, `GIUONG.TRONG_CHO_CHUYEN`, `GIUONG.CHO_SAN_SANG_DAI_NHAT`, `GIUONG.CHO_SAN_SANG_VUOT_HAN` | `BED.USABLE`, `BED.ISOLATED_ROOMS`, `BED.OCCUPANTS`, `BED.EMPTY_AWAITING_TRANSFER`, `BED.AWAITING_READY_MAX`, `BED.AWAITING_READY_OVERDUE` |
| `CV.HOM_NAY`, `CV.QUA_HAN`, `CV.THEO_TRANG_THAI`, `CV.TY_LE_DUNG_HAN_BAT_BUOC`, `CV.GHI_NHAN_MUON`, `CV.THEO_NHAN_VIEN` | `TASK.TODAY`, `TASK.OVERDUE`, `TASK.BY_STATUS`, `TASK.MANDATORY_ON_TIME_RATE`, `TASK.LATE_RECORDINGS`, `TASK.BY_STAFF` |
| `BUA.PHIEU_TRE`, `BUA.PHIEU_SAI_LECH` | `MEAL.LATE_TICKETS`, `MEAL.DISCREPANCY_TICKETS` |
| `HD.BUOI`, `HD.TY_LE_THAM_GIA`, `HD.CHUYEN_QUA_GIO`, `HD.THIEU_NGUOI`, `HD.CO_LAP` | `ACT.SESSIONS`, `ACT.PARTICIPATION_RATE`, `ACT.OVERDUE_TRIPS`, `ACT.MISSING_PARTICIPANTS`, `ACT.ISOLATION_RISK` |
| `KT.TY_LE_DAT`, `KT.THEO_VAI_TRO_KIEM_TRA` | `QC.PASS_RATE`, `QC.BY_CHECKER_ROLE` |
| `SK.CHI_SO`; `SK.CANH_BAO`, `SK.CANH_BAO_THOI_GIAN_TIEP_NHAN`, `SK.CANH_BAO_LEO_THANG`, `SK.CANH_BAO_SOS_HO_TRO` | `HEALTH.VITAL_SIGNS`; `HEALTH.ALERTS`, `HEALTH.ALERT_RESPONSE_TIME`, `HEALTH.ALERT_ESCALATIONS`, `HEALTH.SOS_ASSISTANCE_ALERTS` |
| `SK.SU_CO`, `SK.SU_CO_LEO_THANG`, `SK.SU_CO_THOI_GIAN_TIEP_NHAN`, `SK.THEO_DOI`, `SK.SU_CO_KHONG_THUOC_SK` | `HEALTH.INCIDENTS`, `HEALTH.INCIDENT_ESCALATIONS`, `HEALTH.INCIDENT_RESPONSE_TIME`, `HEALTH.FOLLOW_UPS`, `HEALTH.NON_HEALTH_INCIDENTS` |
| `SK.DANH_GIA_LAI_QUA_HAN`, `SK.KHOANH_VUNG`, `SK.NGUY_KICH` | `HEALTH.OVERDUE_REASSESSMENTS`, `HEALTH.CONTAINMENT_ZONES`, `HEALTH.CRITICAL_FLAGS` |
| `THUOC.TY_LE_BO_LO`, `THUOC.TY_LE_TU_CHOI`, `THUOC.DOI_CHIEU_QUA_HAN` | `MED.MISSED_RATE`, `MED.REFUSED_RATE`, `MED.OVERDUE_RECONCILIATIONS` |
| `CA.BAN_GIAO_CHUA_XAC_NHAN`, `CA.TY_LE_PHUC_VU`, `CA.THIEU_PHU`, `CA.THIEU_CA`, `CA.YEU_CAU`, `CA.LOI_MOI`, `CA.GIO_LAM` | `SHIFT.UNCONFIRMED_HANDOVERS`, `SHIFT.STAFFING_RATIO`, `SHIFT.UNDER_COVERAGE`, `SHIFT.MISSING_SHIFTS`, `SHIFT.REQUESTS`, `SHIFT.INVITATIONS`, `SHIFT.WORKING_HOURS` (`CA.THIEU_PHU`, `CA.THIEU_CA`, `CA.LOI_MOI` bỏ theo Q-397, giữ dòng để truy vết bản dịch) |
| `GP.CO_SO`, `GP.NHAN_VIEN` | `LICENSE.FACILITY`, `LICENSE.STAFF` |
| `CP.BANG`, `CP.TONG_HOP`, `CP.DIEU_CHINH_SAU_CHOT`, `CP.TY_LE_THEO_SU_KIEN`, `CP.MUA_SAM` | `COST.STATEMENTS`, `COST.SUMMARY`, `COST.POST_FINALIZATION_ADJUSTMENTS`, `COST.EVENT_GENERATED_RATE`, `COST.PURCHASING` |
| `SD.THAP_CON_NO`, `KB.CAN_LAM`, `TS.QUA_HAN`, `KHO.DUOI_TOI_THIEU`, `KHO.LO_SAP_HET_HAN` | `BALANCE.LOW_OR_OWING`, `REGISTRATION.TO_DO`, `ASSET.OVERDUE`, `INVENTORY.BELOW_MINIMUM`, `INVENTORY.EXPIRING_LOTS` (hai chỉ tiêu `KHO.*` bỏ theo Q-392, giữ dòng để truy vết bản dịch) |

### 22.5. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `VUOT_KHOANG_TOI_DA` (CFG-M14-01), `THIEU_MUC_DICH` | `RANGE_EXCEEDS_MAXIMUM` (422), `PURPOSE_REQUIRED` (422) |
| `KHONG_XAC_DINH_PHAM_VI` (FR-016) | không có mã riêng: `PERMISSION_DENIED` (403) kèm `failedLayer` `SOURCE_DATA` như 002 (chủ dự án chọn 2026-10-24) |

### 22.6. Gói và thư mục (dự kiến, chốt cùng mã)

| Tên logic | Mã |
| --- | --- |
| `reporting.chitieu`, `phamvi`, `dashboard`, `baocao`, `chitiet`, `xuat` | `reporting.internal.metric`, `scope`, `dashboard`, `report`, `detail`, `export` (mỗi gói `{service, domain}`); controller ở `reporting.web` |
| *(bổ sung 2026-10-24, Phase 1, 2)* `ChiTieu`, `GiaTriChiTieu`, `DongChiTiet`, bản ghi nguồn, danh mục chỉ tiêu; phép đếm, tỷ lệ, trung bình/lớn nhất/tổng; nhãn | `reporting.internal.metric.domain`: `Metric` (`Computation.Count`, `Ratio`, `Aggregate` `AVERAGE`/`MAXIMUM`/`SUM`), `MetricValue` (`Breakdown`), `DetailRow`, `MetricRecord`, `ValueLabel`; `reporting.internal.metric.service.MetricCatalog` |
| nhóm chỉ tiêu FR-010 (giá trị cột `metric_group` của 002); phạm vi ô `TOAN_VIEN`, `TANG_DUOC_GIAO`, `PHAN_CONG`, tầng/khu của phân công, `NHAN_VIEN_TANG`; `QuyenNhomChiTieu` | `MetricGroup` (`RESIDENTS`, `WAITING_LIST`, `BEDS`, `CARE_TASKS`, `PER_STAFF`, `BED_AWAITING_READY`, `MEAL_TICKETS`, `ACTIVITIES`, `QUALITY_CHECKS`, `HEALTH_EVENTS`, `NON_HEALTH_INCIDENTS`, `OVERDUE_REASSESSMENTS`, `MEDICATION_DOSES`, `MEDICATION_RECONCILIATIONS`, `CONTAINMENT_ZONES`, `HANDOVERS`, `STAFFING_RATIO`, `FACILITY_LICENSES`, `STAFF_LICENSES`, `INCURRED_COSTS`, `PURCHASING`, `BALANCES`, `RESIDENCE_REGISTRATION`, `ASSETS`, `INVENTORY`, `CRITICAL_FLAGS`, `STAFFING_SHIFTS`); `GroupScope` (`FACILITY`, `ASSIGNED_FLOORS`, `ASSIGNMENT`, `ASSIGNMENT_AREA`, `FLOOR_STAFF`); `MetricGroupAccess` |
| `PhamViBaoCao` (phần phạm vi, lọc thô, lọc chính xác); giới hạn trường dòng chi tiết; kiểm khoảng CFG-M14-01 | `reporting.internal.scope`: `ReportScope` (`Grant`, `rough`, `covers`), `ReportScopeService` (`dashboard`, `period`), `DetailRowPolicy`, `ReportPeriods` (`ReportPeriod`) |
| *(bổ sung 2026-10-24, "Đổi api" 002)* `QuyenApi.thuHepNhomChiTieu`, `ThuHepNhomChiTieu` (theo vai trò, theo tài khoản; dòng `nhomChiTieu`, `kenh`, loại `BO_THAO_TAC`, `HA_PHAM_VI`); `QuyenXemTruongApi.duocXemTheoVaiTro`; kênh `DASHBOARD`, `BAO_CAO` | `PermissionApi.metricGroupRestrictions`, `administration.account.api.dto.MetricGroupRestrictions` (`byRole`, `byAccount`; `Restriction` `metricGroup`, `channel`, `kind`, `appliesTo`; `Kind` `REMOVE_OPERATION`, `LOWER_SCOPE`); `FieldVisibilityApi.canViewByRole`; `administration.account.api.dto.ReportChannel` (`DASHBOARD`, `REPORT`); test `MetricGroupRestrictionsIT` |
| test `PhamViBaoCaoTest`; test khung chỉ tiêu, bảng FR-010, giới hạn trường, khoảng | `ReportScopeTest`; `MetricTest`, `MetricGroupAccessTest`, `DetailRowPolicyTest`, `FR_060_ReportPeriodTest`, `M14ArchTest` |
| *(bổ sung 2026-10-24, Phase 11)* nguồn bản ghi của chỉ tiêu; bộ lọc báo cáo; chế độ chi phí | `reporting.internal.metric.domain.MetricSource` (`metric`, `records`), `ReportFilters` (`floorAreaIds`, `residenceType`, `careLevelId`, `residentId`, `staffId`, `shiftId`, `costMode`; `NONE`, `matches`, `toMap`), `ReportFilters.CostMode`; `Metric.residents`; `MetricCatalog.sourcesIn`; `ReportScopeService.visible` |
| nhóm báo cáo và nhóm chỉ tiêu trong báo cáo; báo cáo đã tính, kết quả một chỉ tiêu | `reporting.internal.report`: `ReportGroup` (`label`, `metricGroups`), `Report`, `MetricResult` (`residents`, `residentsByKey`, `rows`), `ReportService.period` |
| `XuatBaoCaoService`, `YeuCauXuat`, `NoiDungDuocXuat`, file xuất, `QuyenTepProvider` `M14.BAO_CAO_XUAT`, định dạng | `reporting.internal.export`: `ReportExportService` (`export`, `list`, `canDownload`; `ExportRequest`, `ExportResult` {`fileId`, `auditLogId`}, `ExportRecord`, `ExportFormat`), `ExportContent` (`decide`, `SummaryRow`, `DetailLine`, `rowCount`), `ExportWorkbook` (`Header`), `ReportExportFilePermission`; `reporting.web.ReportExportController`, `web.request.ExportRequestBody` |
| chỉ tiêu `GP.CO_SO` | `reporting.internal.license.service.FacilityLicenseMetric` (`FacilityLicenseRecord`: `licenseId`, `name`, `expiryDate`, `daysRemaining`) |
| *(bổ sung 2026-10-27, US8 phần 008)* chỉ tiêu `GP.NHAN_VIEN`, `CA.TY_LE_PHUC_VU`, `CA.BAN_GIAO_CHUA_XAC_NHAN`; tình trạng ca đạt, không đạt, chưa cấu hình | `reporting.internal.license.service.StaffLicenseMetric` (`StaffLicenseRecord`: `staffName`, `daysRemaining`); `reporting.internal.staffing.service.StaffingRatioMetric` (`StaffingRatioRecord`, `Status`: `MET`, `NOT_MET`, `NOT_CONFIGURED`), `UnconfirmedHandoversMetric` (`HandoverRecord`: `waitingMinutes`) |
| *(bổ sung 2026-10-27)* bản ghi "lúc xem" của nguồn; tính, chi tiết "lúc xem" | `MetricSource.current`; `MetricEvaluator.current`, `currentDetails` |
| *(bổ sung 2026-10-27, luật "Đổi api" trên M09, chủ dự án duyệt)* `NhanSuThongKeApi.banGiaoDangMo`; trường số nhân viên được tính, tổng trọng số, mẫu ca, giờ bắt đầu/kết thúc ca, nhóm chứng chỉ | `StaffStatisticsApi.openHandovers(scope)`; `StaffingRatioView.staffCount`, `totalWeight`, `shiftTemplateId`, `shiftStartsAt`, `shiftEndsAt`; `HandoverReportView.shiftEndsAt`; `ExpiringCredentialView.credentialGroup` |
| test `ChiTieu_NS_*Test` (phần 008) | `MetricShiftStaffingRatioTest`, `MetricShiftHandoversTest`, `MetricLicenseStaffTest`, `FR_020_StaffingDashboardIT`; M09 `StaffStatisticsReportFieldsIT` |
| *(bổ sung 2026-10-27)* chỉ tiêu giường; khóa giữ chỗ hồ sơ chờ, người vắng | `reporting.internal.bed.service`: `BedStateRecords` (`BedStateRecord.key`), `BedStatusMetric`, `BedUsableMetric`, `IsolatedRoomsMetric`, `BedOccupantsMetric`, `AwaitingReadyRecords` (`AwaitingReadyRecord`: `minutes`, `overdue`), `AwaitingReadyMetric`, `AwaitingReadyMaximumMetric`, `AwaitingReadyOverdueMetric`; khóa `HELD_WAITING_RECORD`, `HELD_ABSENT_RESIDENT` |
| *(bổ sung 2026-10-27, luật "Đổi api" trên M03, chủ dự án duyệt)* phòng, lý do giữ chỗ của giường tại ngày; lúc bắt đầu thật của khoảng Chờ sẵn sàng | `BedStateOnDate.roomId`, `holdReason`; `AwaitingReadyInterval.startedAt` |
| test `ChiTieu_GIUONG_*Test` | `MetricBedStatusTest`, `MetricBedAwaitingReadyTest`; M03 `BedReportingFieldsIT` |
| test `FR_061_NoiDungXuatTest`, `FR_062_NhatKyXuatIT`, `ChiTieu_GP_CoSoTest`; chỉ tiêu giả của test | `FR_061_ExportContentTest`, `FR_062_ExportAuditIT`, `MetricLicenseFacilityTest`; `TestDoseMetric` (`TEST.MISSED_DOSES`) |
| *(bổ sung 2026-10-24, T009, khung T014, T022)* tính một chỉ tiêu; `DashboardService` (khối, trạng thái khối, bộ đệm); `ChiTietService`; controller; thân phản hồi | `reporting.internal.metric.service.MetricEvaluator` (`value`, `result`, `details`); `reporting.internal.dashboard.service.DashboardService` (`dashboard`, `metrics`, `GROUPS`, `Block`, `BlockStatus` `COMPLETE`/`UPDATING`, `Dashboard`); `reporting.internal.detail.service.MetricDetailService` (`MetricDetail`, `PAGE_SIZE`); `MetricCatalog.source`; `reporting.web.DashboardController`, `ReportController`; `web.response.DashboardBody`, `MetricValueBody`, `ReportBody`, `MetricBody`, `MetricDetailBody` |
| test dashboard, báo cáo, chi tiết | `FR_020_DashboardIT`, `ReportApiIT`; web `DashboardPage.test.jsx`, `ReportsPage.test.jsx` |
| *(bổ sung 2026-11-01, T011, T012)* test `FR_012_PhamViTheoThoiGianIT`, `SC_004_PhamViIT`, `FR_014_GioiHanTruongIT`, `SC_005_TenThuocIT`, `FR_042a_SuCoHanhChinhIT`; chỉ tiêu giả của test | `FR_012_TimeScopeIT`, `SC_004_ScopeIT`, `FR_014_FieldRestrictionIT` (gồm SC-005), `FR_042a_AdminIncidentIT`; `TestScopedMetrics`, `ReportScopeFixture` |
| *(bổ sung 2026-11-01, 009 T031, T042)* `mobile/lib/nguoi-than/thong-bao/`, `mobile/lib/nguoi-cao-tuoi/thong-bao/`; test `thong_bao_test.dart` | `mobile/lib/family/notifications/` (`FamilyInboxScreen`, `FamilyNotificationScreen`), `mobile/lib/resident/notifications/` (`ResidentNotificationsScreen`), `mobile/lib/common/notifications/` (`NotificationApi`); `family_inbox_test.dart`, `resident_notifications_test.dart` |
| *(bổ sung 2026-11-01, 000 T098)* `mobile/lib/nhan-vien/yeu-cau-phe-duyet/hop_duyet.dart`; test `hop_duyet_test.dart` | `mobile/lib/staff/approval_requests/` (`approval_inbox_screen.dart`, `approval_request_screen.dart`, `approval_labels.dart`); `test/staff/approval_requests/approval_inbox_test.dart` |
| *(bổ sung 2026-11-01, T021, T027)* test `FR_009_ChiTietKhopSoIT`, `SC_008_ChiDocIT` | `FR_009_DetailMatchIT`, `SC_008_ReadOnlyIT` |
| *(bổ sung 2026-11-01, "Đổi api" 002)* `PhamViApi.boLocPhanCong`; `QuyenApi.ghiLoiDuLieuQuyen`; giá trị lọc của báo cáo (`boLocCoThe` {`toanVien`, `tangKhuVuc`}); tầng của việc chung trong một phần phạm vi | `ScopeApi.assignedFilter`; `PermissionApi.recordDataError`; `Report.filterOptions` (`facilityWide`, `floorAreas` {`id`, `name`}), `ReportFilterOptions`; `ReportScope.Grant.commonFloorAreaIds` |
| màn web (chốt 2026-10-24): một màn Báo cáo có tab cho mọi nhóm thay các thư mục theo nhóm | `features/dashboard/DashboardPage.jsx`; `features/reports/` (`ReportsPage`, `ReportTable`, `MetricDetailSheet`, `ExportDialog`, `ExportList`, `reportLabels`) |
| web `tinh-nang/bao-cao/{dashboard, nguoi-cao-tuoi, cham-soc, chat-luong, suc-khoe-thuoc, chi-phi, nhan-su-ca, chi-tiet}` | `features/reports/{dashboard, residents, care, quality, health-medication, incurred-costs, staffing-shifts, metric-details}` |

## 23. Feature 017 (`017-resident-balance-ledger`, E)

*Chủ feature điền khi dịch `contracts/api-017.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 24. Feature 018 (`018-residence-registration`, B)

*Chủ feature điền khi dịch `contracts/api-018.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 25. Feature 019 (`019-inventory-assets`, C)

*Chủ feature điền khi dịch `contracts/api-019.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

#### Bổ sung 003 sau merge M01 (2026-10-24)

| Tên logic, mục đích | Tên trong mã |
| --- | --- |
| Dữ liệu người cao tuổi dùng kiểm phân bổ; mức đã chọn và nguồn | `ResidentAllocationFacts`, `CheckedCareLevel`; `CareLevel.REGULAR` (001) ánh xạ `CareLevelCode.FREQUENT` (003) |
| Ảnh chính sách giường, kiểm FR-018 dùng chung, SQL kiểm | `AllocationCandidate`, `AllocationChecker`, `AllocationCheckStore` |
| Ảnh lệnh và ảnh đọc phân bổ; lệnh, SQL ghi, SQL đọc, service đọc | `AllocationRecord`, `AllocationDetails`, `AllocationCommands`, `AllocationCommandStore`, `AllocationReadStore`, `AllocationReading` |
| HTTP phân bổ và vị trí | `BedAllocationController`, `ResidentLocationController`, `BedAllocationWrite`, `BedAllocationResponse`, `SuitableBedResponse`, `ResidentLocationResponse` |
| Mã vi phạm FR-018 (a) → (i) | `ALLOCATION_CARE_LEVEL`, `ALLOCATION_RESIDENCY_TYPE`, `ALLOCATION_GENDER`, `ALLOCATION_ISOLATION`, `ALLOCATION_BED_STATE`, `ALLOCATION_HOLD`, `ALLOCATION_TIME_OVERLAP`, `ALLOCATION_PROFILE_CLOSED`, `ALLOCATION_ASSET_FLAG` |
| Bắt đầu dự kiến trước hạn sẵn sàng; thiếu dấu chưa có giường; hủy chỉ tương lai | `ALLOCATION_NOT_READY_BEFORE_DEADLINE`, `WITHOUT_BED_REQUIRED`, `FUTURE_ALLOCATION_REQUIRED` |
| Nhật ký tạo/hủy phân bổ | `M03_ALLOCATION_CREATED`, `M03_ALLOCATION_CANCELLED` |
| Giữ lại giữ chỗ giường đích khi đặt trước | `BedHoldService.cancelHoldExcept` (helper nội bộ, không đổi API) |
| Đánh giá lại tính phù hợp khi nhận sự kiện M01 | `AllocationSuitabilityService`, `AllocationSuitabilityListener`, `AllocationChecker.policyConditions` |
| Báo quá bắt đầu dự kiến | `PastStartAllocation`, `PastStartAllocationStore`, `PastStartAllocationProcessor`, `FutureAllocationPastStartJob`; job `REPORT_FUTURE_ALLOCATION_PAST_START` |

**Bổ sung tên 003 sau khi nối M01 (2026-10-24)**

| Khái niệm | Tên mã đã dùng |
| --- | --- |
| Dữ kiện trạng thái/giới tính/mức chăm sóc, kiểm phân bổ | `ResidentAllocationFacts`, `CheckedCareLevel`, `AllocationChecker`, `AllocationCandidate`, `AllocationCheckStore` |
| Lệnh phân bổ, đọc chi tiết và vị trí | `AllocationCommands`, `AllocationCommandStore`, `AllocationReading`, `AllocationReadStore`, `AllocationRecord`, `AllocationDetails`, `ResidentLocationQueries`, `ResidentLocationDetails` |
| Tổng hợp giường | `BedCapacityQueries`, `BedCapacitySummary`, `BedSummaryController`, `BedSummaryResponse`, `BedResidencyType` |
| HTTP phân bổ và vị trí | `BedAllocationController`, `ResidentLocationController`, `BedAllocationWrite`, `BedAllocationResponse`, `SuitableBedResponse`, `ResidentLocationResponse` |
| Tham gia trạng thái M01 và đánh giá phù hợp | `AllocationStatusParticipant`, `AllocationSuitabilityListener`, `AllocationSuitabilityService` |
| Thông báo quá bắt đầu dự kiến | `PastStartAllocation`, `PastStartAllocationStore`, `PastStartAllocationProcessor`, `FutureAllocationPastStartJob`, `REPORT_FUTURE_ALLOCATION_PAST_START` |
| Chỉ mục kỹ thuật giữ chỗ, retry | `WaitingHoldIndexStore`, `resident_position.waiting_hold_bed_ids`, `AllocationRetryConfig`, `M03_BED_HOLD_UPDATED` |
| Màn phân bổ và bảng giường (T028) | `BedAllocationForm`, `BedAllocationsPanel` trong `frontend/src/features/beds/` |

Các tên mã trên đã có implementation; task chỉ đánh sau kiểm cuối. Enum đồng hồ thử 000 chưa thêm hai job theo lựa chọn chủ dự án; không đổi HTTP contract.

| Khái niệm kỹ thuật bổ sung | Tên mã |
| --- | --- |
| Pool đọc hiện hành sau khóa | `AllocationCurrentReadPool`, `CurrentAllocationOverlapStore` (chỉ đọc RC, không tham gia transaction nguồn) |
| Khóa kỹ thuật chống gap-lock cycle của transaction RR | `allocation_write_guard`, `AllocationStore.guardWrites` (RC S, RR/default X trước khóa vị trí) |

#### Bổ sung 003 Phase 8 — 2026-10-24

| Tên logic | Tên mã nguồn/đường dẫn |
| --- | --- |
| Khu nghỉ bán trú, buổi | `DayCareRestArea`, `DayCareSessionEntry` (entity kế thừa `CatalogBase`); `DayCareAreaState`, `DayCareSessionState` (ảnh đọc) |
| Cấu hình và kiểm sức chứa | `DayCareConfigurationService`, `DayCareCapacityService`, `DayCareStore`, `day_care_capacity_guard` |
| Đếm lại, cảnh báo đổi khung/CFG | `DayCareRecounting`, `DayCareParameterListener`, `SessionCapacityRow`, `DayCareSessionChange`, `M03_DAY_CARE_OVER_CAPACITY` |
| HTTP bán trú Phase 8 | `/day-care-rest-areas`, `/day-care-sessions`; `DayCareRestAreaWrite`, `DayCareSessionWrite`, `DayCareRestAreaResponse`, `DayCareSessionResponse`, `DayCareSessionChangeResponse`, `DayCareSessionCapacity` |
| Màn bán trú | `frontend/src/features/beds/DayCarePanel.jsx`, tab Bán trú trong `BedsPage` |
| Lỗi bán trú | `DAY_CARE_REST_AREA_EXISTS`, `DAY_CARE_SESSION_NAME_EXISTS`, `DAY_CARE_SESSION_OVERLAP`, `DAY_CARE_LOCATION_INACTIVE`, `DAY_CARE_FUTURE_ARRIVALS`, `DAY_CARE_NOT_CONFIGURED`, `DAY_CARE_CAPACITY_UNSET`, `DAY_CARE_OVER_CAPACITY`, `DAY_CARE_CORDONED`; thiếu nguồn dùng `SOURCE_UNAVAILABLE` đã có |

#### Bổ sung 003 Phase 9 — 2026-10-25

| Khái niệm | Tên mã |
| --- | --- |
| Lệnh và nhật sử cách ly phòng | `RoomIsolationService`, `RoomIsolationStore`, `RoomIsolationFact`, `room_isolation_history`, hành động `ISOLATE`/`LIFT` |
| Chuyển kiểm soát lây nhiễm | `ControlledTransferService`, `ControlledTransferStore`, `ControlledTransferResult`, lý do danh mục `INFECTION_CONTROL` |
| Phòng chịu chặn, Bác sĩ chỉ định | `IsolationRoom`, `PrescribingDoctor`, `PrescribingDoctorResponse` |
| HTTP cách ly | `RoomIsolationController`, `ControlledTransferController`, `IsolationOptionsController`, `RoomIsolationWrite`, `InfectionControlTransferWrite`, `RoomIsolationHistoryRow`, `BedTransferResult` |
| UI web cách ly | `RoomIsolationPanel`, `IsolationWorkspace` trong `frontend/src/features/beds/`, tab Cách ly |
| Lỗi lệnh cách ly/chuyển | `ROOM_ALREADY_ISOLATED`, `ROOM_NOT_ISOLATED`, `ALLOCATION_NOT_TRANSFERABLE`, `INVALID_PRESCRIBING_DOCTOR`, `DOCTOR_CONFIRMATION_NOT_PENDING`; tái dùng `SOURCE_UNAVAILABLE`, `UNMET_CONDITIONS`, `ALLOCATION_TIME_OVERLAP` |

#### Bổ sung 003 Phase 10 — 2026-10-25

| Khái niệm | Tên mã |
| --- | --- |
| Điều kiện/lệnh xác nhận sẵn sàng | `BedReadiness`, `BedReadinessStore`, `BedReadinessService`, `BedReadinessController`, `BedReadinessWrite`, `BedReadinessResponse`; schema HTTP `BedReadinessConditions` |
| Nhắc giường quá hạn | `BedReadinessReminderProcessor`, `BedReadinessReminderJob`, tên job `REMIND_BED_READINESS` |
| Nhắc dấu phân bổ còn mở | `AllocationReminderStore`, `AllocationReminderProcessor`, `AllocationReminderJob`, tên job `REMIND_ALLOCATIONS` |
| Nhật ký nhắc | `M03_REMINDER`, chi tiết `plannedAt`, `executedLate`, `flagType` (nhắc dấu) |
| Lỗi xác nhận | `BED_NOT_AWAITING_READY`, `SCHEDULED_TRANSFER_NOT_COMPLETED`; tái dùng `DISINFECTION_CONFIRMATION_REQUIRED`, `SOURCE_UNAVAILABLE` |

#### Bổ sung 003 Phase 11 — 2026-10-25

| Khái niệm | Tên mã |
| --- | --- |
| Adapter dữ liệu cho008/016 | `BedReportingService` cài `BedStatusApi`, `BedStatisticsApi`, `BedHandoverAlertApi` |
| Đọc snapshot/interval theo lô | `BedReportingStore`, `BedReportingSnapshot` và record nội bộ `State`, `Window`, `Policy`, `Flags` trong `internal/bed/domain` |
| Dấu chưa có giường | `BedFlagView.bedId` kiểu `Long`, null chỉ cho `WITHOUT_BED` theo duyệt chủ dự án2026-10-25 |
| Bằng chứng kiểm cuối | `specs/003-room-bed-allocation/verification.md`, `SC_008_BedReportingIT`, `DBR_23_RecordTablesIT`, `T064_BedListPerformanceIT` |
