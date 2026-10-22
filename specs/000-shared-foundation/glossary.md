# Bảng thuật ngữ Việt – Anh cho mã nguồn

**Ngày**: 2026-10-16, bổ sung 2026-10-19 (mục 6, feature 000; mục 7, feature 002), 2026-10-21 (mục 15, feature 009) | **Căn cứ**: constitution X (3.1.0), research R-03

Plan, data-model, contracts, tasks dùng tên logic tiếng Việt không dấu. Mã nguồn dùng tên tiếng Anh dịch theo bảng này. Một thuật ngữ chỉ có một bản dịch, dùng giống nhau ở bảng, lớp, endpoint, JSON và màn hình. Thuật ngữ chưa có thì thêm vào bảng trong cùng thay đổi với mã; đổi bản dịch đã dùng thì phải đổi cả mã đã viết.

Mã nghiệp vụ giữ nguyên, không dịch: BR-, FR-, CFG-, DBR-, Q-, UC-, AC-, BF-, NFR-, SC-.

## 1. Cách viết tên

| Loại | Quy ước | Ví dụ (tên logic → mã) |
| --- | --- | --- |
| Gói Java | chữ thường, một từ | `m15.pheduyet` → `m15.approval` |
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
| `m01` … `m15` | giữ nguyên |
| `m15.thamso` | `m15.parameter` |
| `m15.nhatky` | `m15.auditlog` |
| `m15.pheduyet` | `m15.approval` |
| `m15.dinhchinh` | `m15.correction` |
| `m15.luutru` | `m15.retention` |
| `m15.taikhoan` | `m15.account` |
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
| `Loi` (`maLoi`, `dieuKienChuaThoa[]` {`ma`, `moTa`}, `giaTriHienHanh`) | `Problem` (`errorCode`, `unmetConditions[]` {`code`, `description`}, `currentValue`) |
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
| `lan_chay_lich` | `scheduler_run` |
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
| SPI `DinhChinhDuocProvider`: `loaiBanGhi`, `daXacNhan`, `nguoiGhiGoc`, `tangKhuVuc`, `coQuyenGhiNhan`, `kiemTraNoiDung`, `noiDungGoc`, `choPhep`, `sauKhiDinhChinh` | `recordType`, `isConfirmed`, `originalRecorder`, `floorArea`, `canRecord`, `validateContent`, `originalContent`, `allows`, `afterCorrection` |
| `KetQuaDinhChinh` (`DaLuu`, `ChoDuyet`), `GiaTriHienHanh`, `ChuoiDinhChinhView`, `DinhChinhView`, sự kiện `DaDinhChinh` | `CorrectionResult` (`Saved`, `PendingApproval`), `CurrentValue`, `CorrectionChainView`, `CorrectionView`, `CorrectionMade` |
| bảng `dinh_chinh` (`loai_ban_ghi`, `id_ban_ghi_goc`, `kieu`, `noi_dung_dung`, `ly_do`, `nguoi_thuc_hien_id`, `cach_tao`, `yeu_cau_phe_duyet_id`, `thoi_diem`, `so_thu_tu`) | `correction` (`record_type`, `original_record_id`, `type`, `corrected_content`, `reason`, `actor_id`, `creation_method`, `approval_request_id`, `recorded_at`, `sequence_no`) |
| bảng `chuoi_dinh_chinh` (`so_ban`, `da_huy`) | `correction_chain` (`correction_count`, `voided`) |
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
| SPI `KiemTraGiaTriThamSo` (`ma`, `kiemTra`) (017 research E-04) | `ParameterValueCheck` (`code`, `check`) ở `m15.parameter.api.spi`; bản giả `FakeParameterValueCheck` |
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

## 7. Feature 002 *(bổ sung 2026-10-19, khi dịch `contracts/api-002.yaml`)*

Bảng, cột, lớp của 002 bổ sung vào mục này cùng thay đổi với mã (R-03). Tên đã có ở mục 6.5 (bản giả `m15.account.api` của 000) giữ nguyên khi 002 cài bản thật.

### 7.1. Đường dẫn API và header (tiền tố `/api/v1`)

| Tên logic | Mã |
| --- | --- |
| `/xac-thuc/dang-nhap`, `/xac-thuc/dang-nhap-qr`, `/xac-thuc/dang-xuat`, `/xac-thuc/doi-mat-khau` | `/auth/login`, `/auth/qr-login`, `/auth/logout`, `/auth/change-password` |
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
| `ChucNang` (73 dòng Phụ lục 27, ví dụ `CHI_PHI_DIEU_CHINH`, `DASHBOARD_BAO_CAO`, `NHAT_KY_THAY_DOI`); `LoaiThaoTac`; `KhoaQuyen` | enum `m15.account.api.dto.Function` (`COST_ADJUSTMENT`, `DASHBOARD_REPORT`, `AUDIT_LOG`…; danh sách đủ, cùng thứ tự Phụ lục 27, `label()` là tên dòng); `Operation`; `PermissionKey(Function, Operation)` |
| loại yêu cầu `M15.KHOA_TAI_KHOAN_NCT`; loại bản ghi ví dụ `M04.KET_QUA_CONG_VIEC` | `M15.RESIDENT_ACCOUNT_LOCK`; `M04.CARE_TASK_RESULT` (004 chốt khi dịch hợp đồng của mình) |

### 7.4. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `DANG_NHAP_KHONG_THANH` | `LOGIN_FAILED` |
| `TAI_KHOAN_KHOA_TAM`, `TAI_KHOAN_CAN_LIEN_HE_VIEN`, `TAI_KHOAN_DA_KHOA` | `ACCOUNT_TEMPORARILY_LOCKED`, `ACCOUNT_CONTACT_FACILITY`, `ACCOUNT_LOCKED` |
| `MAT_KHAU_KHONG_DAT` | `PASSWORD_POLICY_NOT_MET` |
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

### 7.5. Gói và kiểu Java của `m15.account` (T002, T008 → T010)

*(Sửa 2026-10-21, tổ chức lại theo research R-02 mục "Cấu trúc trong module")* Đường dẫn tính từ `m15.account`: `api/` (interface), `api/dto/`, `api/spi/`, `api/events/`; `web/`, `web/request/`, `web/response/`, `web/security/`; `internal/<auth, permission, scope, legal, offline>/{service, domain, persistence, config}`, `internal/domain/` (`Account`, trước là `AccountStore.AccountRow`), `internal/persistence/` (`AccountStore`); `scheduling/`. Đường dẫn dạng cũ `auth/internal/…` trong plan, tasks là lịch sử.

| Tên logic | Mã |
| --- | --- |
| gói `m15.taikhoan.{api, xacthuc, quyen, phamvi, phaply, ngoaituyen, lich}` | `m15.account.{api, auth, permission, scope, legal, offline, scheduling}`; sự kiện ở `m15.account.api.events` |
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
| SPI `QuanHeNguoiThanApi` (`hieuLuc`, `soDienThoaiDaDangKy`, `laNguoiDaiDienHieuLuc`, `thuocNguoiCaoTuoi`), `QuanHeView` | `FamilyRelationshipApi` (`active`, `registeredPhoneNumber`, `isActiveRepresentative`, `belongsToResident`), `FamilyRelationshipView` (`relationshipId`, `residentId`) |
| SPI `KetThucLuuTruApi.thoiDiemTrangThaiCuoi` | `ResidencyEndApi.finalStatusAt` |
| SPI `NgoaiTuyenHandler` (`loaiBanGhi`, `khoaQuyen`, `doiTuong`, `kiemTraNoiDung`, `apDung`, `batBuocTrucTuyen`) | `OfflineHandler` (`recordType`, `permissionKey`, `target`, `validate`, `apply`, `requiresOnline`) |
| SPI `DieuKienChuThich` (`maChuThich`, `thoa`), `GanPhamViProvider` (`loaiDoiTuong`, `ganPhamVi`) | `FootnoteCondition` (`footnoteCode`, `isSatisfied`), `ScopeAssignmentProvider` (`entityType`, `assignScope`) |
| ngoại lệ `DuLieuQuyenKhongHopLe` (`nguon`, `loaiLoi`, `banGhiId`) | `InvalidPermissionDataException` (`source`, `errorKind`, `recordId`) |
| SPI `HoSoNhanVienApi` (`trangThai`, `chucDanhCoVaiTro`); trạng thái `DANG_LAM_VIEC`, `NGHI_VIEC` | `StaffProfileApi` (`status`, `positionRole`); `StaffStatus` `WORKING`, `RESIGNED` |
| SPI `GiayPhepApi` (`hieuLuc`, `sapHetHan`) | `LicenseApi` (`valid`, `expiringWithin`); `LicenseValidity`, `ExpiringLicense` |
| SPI `DanhMucPhamViHanhNgheApi`; mục phạm vi hành nghề | `PracticeScopeCatalogApi` (`find`); `PracticeScopeView` |
| SPI `PhanCongApi` (`dangHieuLuc`, `lichSu`, `nhanVienPhanCongChoNguoiCaoTuoi`, `nhanVienPhanCongChoPhong`); loại phân công giao trưởng tầng, thành viên ca, người phụ trách ca | `AssignmentApi` (`activeAt`, `history`, `staffAssignedToResident`, `staffAssignedToRoom`), `AssignmentView`; `AssignmentKind` `FLOOR_MANAGER`, `SHIFT_MEMBER`, `SHIFT_LEAD` |
| SPI `NhiemVuApi` (`nguoiPhuTrachCa`, `truongTangDuocGiao`, `nguoiThayTruongTang`, `caKhongCoTruongTang`, `phuTrachBoPhan`, `laPhuTrachBoPhan`, `boPhanCua`, `truongDoan`, `nguoiDiCung`, `nguoiNhanTaiTang`) | `DutyApi` (`shiftLead`, `assignedFloorManager`, `floorManagerSubstitute`, `shiftHasNoFloorManager`, `departmentLead`, `isDepartmentLead`, `departmentOf`, `groupLeader`, `companions`, `floorReceivers`) |
| sự kiện `TaiKhoanDaTao`, `TaiKhoanDoiTrangThai`, `BanGhiDongBoSauMatQuyen`, `BanGhiChoXemLai`, `LoiDuLieuQuyen`, `GiayPhepSapHetHan` | `AccountCreated`, `AccountStatusChanged`, `RecordSyncedAfterPermissionLoss`, `RecordPendingReview`, `PermissionDataError`, `LicenseExpiring` |
| bản giả `BT/support/gia002/` | `support/fake/m15/` (`FakeM15Config`, `FakeStaffProfiles`, `FakeLicenses`, `FakePracticeScopes`, `FakeAssignments`, `FakeDuties`, `FakeBedAllocations`, `FakeFamilyRelationships`, `FakeResidencyEnds`) |
| `xacthuc/internal/PhienService`, `xacthuc/web/XacThucTokenFilter` (T011); phiên đang mở; token phiên | `internal/auth/service/SessionService` (`resolve`), `web/security/SessionTokenFilter`, `web/security/SecurityConfig`; `ActiveSession` (`accountId`, `type`, `staffId`, `familyMemberId`, `roles`, `deviceId`, `channel`), `SessionAuthentication`; `SessionTokens` (`generate`, `hash`, `cookie`) |
| `xacthuc/internal/NguoiDungHienTaiImpl` (T012); bản Bộ lập lịch | `internal/auth/service/CurrentUserImpl`; `SchedulerUser`, `SchedulerUserAspect`; `ActiveSession` cài `CurrentUser` |
| `quyen/internal/TinhQuyenVaiTro` (T013); phạm vi `TOAN_VIEN`, `PHAN_CONG`, `QUAN_HE`; phương án quyền | `internal/permission/service/RolePermissionCalculator` (`current`, `at`); `PermissionScope` (`FACILITY`, `ASSIGNED`, `RELATIONSHIP`); `RoleGrant` (`scope`, `footnotes`, `source`), `RoleGrants` (`add`, `lower`), `GrantSource` (`ROLE`, `ADDITIONAL_GRANT`) |
| `xacthuc/internal/KhoiTaoQuanLyVien` (T016); lệnh dòng lệnh `khoi-tao-quan-ly-vien`; `HoSoNhanVienApi.taoHoSoToiThieu` | `internal/auth/service/FirstManagerBootstrap` (`run`, `Result`), `FirstManagerBootstrapRunner` (`eldercare.bootstrap-manager.run-once`, `.username`, `.full-name`), `PasswordConfig`; `StaffProfileApi.createMinimal(fullName, positionRole)` |
| hành động nhật ký `TK_TAO`, `VT_GAN`; đối tượng tài khoản; người thực hiện `KHOI_TAO` | `ACCOUNT_CREATED`, `ROLE_ASSIGNED`; `M15.ACCOUNT`; `actor_type = BOOTSTRAP` qua `AuditLogApi.writeAsBootstrap` |
| `quyen/internal/QuyenService` (T022); `phamvi/internal/PhamViService` (`trongPhamVi`, phần kéo lên US1); phân loại chú thích | `internal/permission/service/PermissionService`; `internal/scope/service/ScopeService` (`inScope`); `FootnoteRules` (`CONDITIONS`, `DUTIES`, `blockedAtRoleLayer`); nguồn quyền thêm `GrantSource.DUTY` |
| `quyen/internal/QuyenXemTruongService`, `GioiHanTruongSerializerModifier` (T023); `truongBiAn`; DTO có đối tượng | `internal/permission/service/FieldVisibilityService`, `FieldGroupSerializerModifier`, `FieldVisibilityJacksonConfig`; trường JSON `hiddenFields`; `api.VisibilityTarget` (`visibilityTarget()`) |
| `phamvi/internal/LoiDuLieuQuyenService` (T024); bảng `loi_du_lieu_quyen` (`nguon`, `ban_ghi_id`, `loai_loi`, `lan_dau_luc`, `lan_cuoi_luc`, `so_lan`, `da_khac_phuc_luc`); hành động `QUYEN_TU_CHOI_DU_LIEU_NGUON` | `internal/scope/service/PermissionDataErrors` (`record`, `markFixed`); `permission_data_error` (`source`, `record_id`, `error_kind`, `first_at`, `last_at`, `occurrences`, `fixed_at`, `open_key`); `PERMISSION_DENIED_SOURCE_DATA` (đối tượng `M15.PERMISSION_DATA_ERROR`) |
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
| *(bổ sung 2026-10-21, Phase 9a)* `ThietBiGanApi.xacDinh`, `NguoiCaoTuoiCuaThietBi` (`nguoiCaoTuoiId`, `taiKhoanId`, `maThietBi`) | `m15.account.api.BoundDeviceApi.identify`, `api.dto.ResidentDevice` (`residentId`, `accountId`, `deviceId`) |
| `xacthuc/internal/MaQrService`, `ThietBiGanService`, khóa/mở khóa tài khoản người cao tuổi, loại yêu cầu `M15.KHOA_TAI_KHOAN_NCT` | `internal/auth/service/LoginService.qrLogin`, `ResidentDevices` (`bind`, `unbindActive`, `boundToOther`), `ResidentAccountService` (`createLoginCode`, `unbindDevice`, `lockResident`, `unlockResident`, `lockOnFamilyRequest`), `ResidentAccountLockRequestHandler`; `internal/auth/persistence/DeviceBindings`; `web/ResidentAccountController`; `QrLoginBody`, `QrLoginResponse`, `LockResidentBody`, `UnlockResidentBody` |
| hành động nhật ký `QR_TAO`, `QR_DUNG`, `TB_GAN`, `TB_GO` | `LOGIN_QR_CREATED`, `LOGIN_QR_USED`, `DEVICE_BOUND`, `DEVICE_UNBOUND` |
| phạm vi `BAN_THAN`; nguyên nhân khóa `NCT_KET_THUC_LUU_TRU`, `NCT_TU_YEU_CAU`, `CO_LY_DO`, `THEO_DE_NGHI_NGUOI_THAN`; lý do kết thúc phiên `GO_THIET_BI`, `DOI_MAT_KHAU`; lý do gỡ `GAN_MAY_MOI`, `GO_THU_CONG`, `TAI_KHOAN_DA_KHOA` | `SELF`; `RESIDENT_RESIDENCY_ENDED`, `RESIDENT_REQUESTED`, `WITH_REASON`, `FAMILY_REQUEST`; `DEVICE_UNBOUND`, `PASSWORD_CHANGED`; `NEW_DEVICE_BOUND`, `MANUAL`, `ACCOUNT_LOCKED` |
| bảng `ma_qr_dang_nhap` (`ma_bam`, `tao_boi`, `tao_luc`, `het_han_luc`, `dung_luc`, `ma_thiet_bi_dung`, `vo_hieu_luc_luc`), `thiet_bi_gan` (`ma_thiet_bi`, `ma_bam_token_thiet_bi`, `gan_luc`, `ma_qr_id`, `go_luc`, `go_boi`, `ly_do_go`); `tai_khoan.nguoi_cao_tuoi_id` | `login_qr_code` (`code_hash`, `created_by`, `created_at`, `expires_at`, `used_at`, `used_device_id`, `revoked_at`), `bound_device` (`device_id`, `device_token_hash`, `bound_at`, `login_qr_code_id`, `unbound_at`, `unbound_by`, `unbind_reason`, `unbind_note`; cột sinh `active_account_id`, `active_device_id`); `account.resident_id` |
| *(bổ sung 2026-10-21, Phase 10)* test T021 `PhamViBaoCaoLichSuIT`; bản giả `BT/support/QuyenGia` đổi thành tiện ích người dùng thật (T080); dữ liệu mẫu `V002_90__tai_khoan_mau` (T079a); test FR-023 (T082) | `HistoryReportScopeIT`; `support/TestUsers` (`staff`, `family`, `session`, `bearer`, `TestUser`), test `TestUsersIT`, `FakePermissions` giữ cho test của 000; `db/migration/demo/V002_90__sample_accounts.sql`, test `SampleAccountsIT`; `FloorManagerLimitsIT` |

### 7.6. Bảng và cột (T004 → T006)

| Tên logic | Mã |
| --- | --- |
| `tai_khoan` (`ten_dang_nhap`, `loai`, `nhan_vien_id`, `nguoi_than_id`, `trang_thai`, `nguyen_nhan_khoa`, `khoa_tam_den`, `so_lan_sai_lien_tiep`, `mat_khau_bam`, `buoc_doi_mat_khau`, `lan_dang_nhap_cuoi`, `moc_tinh_khong_hoat_dong`, `phien_ban`, `phien_ban_quyen`, `tao_luc`, `tao_boi`) | `account` (`username`, `type`, `staff_id`, `family_member_id`, `status`, `lock_cause`, `locked_until`, `failed_login_count`, `password_hash`, `must_change_password`, `last_login_at`, `inactivity_reference_at`, `version`, `permission_version`, `created_at`, `created_by`) |
| nguyên nhân khóa `THU_CONG`, `NGHI_VIEC`, `HET_HAN_SAU_KET_THUC`, `HET_QUAN_HE` | `MANUAL`, `RESIGNATION`, `EXPIRED_AFTER_RESIDENCY_END`, `RELATIONSHIP_ENDED` |
| `lich_su_trang_thai_tai_khoan` (`hieu_luc_tu`, `nguoi_thuc_hien_id`, `nhat_ky_id`) | `account_status_history` (`effective_from`, `actor_id`, `audit_log_id`) |
| `phien_dang_nhap` (`ma_bam_token`, `ma_thiet_bi`, `kenh`, `bat_dau_luc`, `hoat_dong_cuoi_luc`, `ket_thuc_luc`, `ly_do_ket_thuc`, `thiet_bi_dang_mo`) | `login_session` (`token_hash`, `device_id`, `channel`, `started_at`, `last_active_at`, `ended_at`, `end_reason`, `open_device_id`) |
| lý do kết thúc phiên `DANG_XUAT`, `TAI_KHOAN_ROI_HOAT_DONG`, `THIET_BI_DANG_NHAP_KHAC`, `HET_THOI_GIAN` | `LOGOUT`, `ACCOUNT_LEFT_ACTIVE`, `DEVICE_SIGNED_IN_ELSEWHERE`, `TIMED_OUT` |
| `thiet_bi` (`ma_thiet_bi`, `lan_ket_noi_cuoi`, `tai_khoan_cuoi_id`) | `device` (`device_id`, `last_connected_at`, `last_account_id`) |
| *(bổ sung 2026-10-20, T055)* `ban_ghi_cho_xem_lai` (`loai_ban_ghi`, `noi_dung`, `idempotency_key`, `nguoi_ghi_id`, `ma_thiet_bi`, `thoi_diem_thiet_bi`, `thoi_diem_dong_bo`, `lan_ket_noi_cuoi`, `ly_do_cho`, `tang_khu_vuc_id`, `trang_thai`, `nguoi_quyet_dinh_id`, `thoi_diem_quyet_dinh`, `ly_do_tu_choi`, `ket_qua_loai` + `ket_qua_id`, `phien_ban`) | `pending_review_record` (`record_type`, `content`, `idempotency_key`, `recorder_id`, `device_id`, `device_time`, `synced_at`, `last_connected_at`, `pending_reason`, `floor_area_id`, `status`, `decided_by`, `decided_at`, `rejection_reason`, `result_reference`, `version`) |
| *(bổ sung 2026-10-21, T063)* `nghiep_vu_chuyen_mon` (`ma`, `ten`, `chuc_nang`, `loai_giay_phep_yeu_cau`, `can_pham_vi_co_so`, `mac_dinh`, `trang_thai`, `ten_hieu_luc`, `phien_ban`); `nghiep_vu_chuyen_mon_pham_vi` (`nghiep_vu_id`, `pham_vi_hanh_nghe_id`); `da_canh_bao_giay_phep` (`loai` `CO_SO`/`NGUOI_HANH_NGHE`, `giay_phep_id`, `ngay_het_han`, `canh_bao_luc`) | `professional_activity` (`code`, `name`, `function_code`, `required_license_type`, `requires_facility_scope`, `is_default`, `status`, `active_name`, `version`); `professional_activity_practice_scope` (`activity_id`, `practice_scope_id`); `license_expiry_warning` (`kind` `FACILITY`/`PRACTITIONER`, `license_id`, `expiry_date`, `warned_at`) |
| lý do chờ `MUON_HON_DONG_BO`, `SOM_HON_KET_NOI_CUOI`, `QUA_CFG_M15_08`; trạng thái `CHO_XEM_LAI`, `DA_CHAP_NHAN`, `DA_TU_CHOI` | `LATER_THAN_SYNC`, `EARLIER_THAN_LAST_CONNECTION`, `EXCEEDS_CFG_M15_08`; `PENDING_REVIEW`, `ACCEPTED`, `REJECTED` |
| `vai_tro` (`ma`, `ten`, `loai_tai_khoan`, `pham_vi_thuc_hien`, `phien_ban_quyen`) | `role` (`code`, `name`, `account_type`, `execution_scope`, `permission_version`) |
| `ma_tran_quyen` (`vai_tro`, `chuc_nang`, `loai_thao_tac`, `pham_vi`, `ma_chu_thich`, `nguon` `PL27`/`19.3`) | `permission_matrix` (`role`, `function_code`, `operation`, `scope`, `footnote_codes`, `source` `APPENDIX_27`/`SECTION_19_3`) |
| `gan_vai_tro` (`hieu_luc_tu`, `hieu_luc_den`, `nguoi_gan_id`, `ly_do`, `con_hieu_luc`) | `role_assignment` (`effective_from`, `effective_to`, `assigned_by`, `reason`, `active_role`) |
| `thu_hep_quyen` (`doi_tuong` `VAI_TRO`/`TAI_KHOAN`, `kieu`, `nhom_chi_tieu`, `kenh`, `nguoi_thuc_hien_id`) | `permission_restriction` (`applies_to` `ROLE`/`ACCOUNT`, `kind`, `metric_group`, `channel`, `actor_id`) |
| `quyen_gan_them` | `additional_grant` |
| `quy_tac_xem_truong` (`nhom_thong_tin`, `muc_xem`) | `field_visibility_rule` (`info_group`, `visibility`) |

## 8. Feature 001 (`001-elder-profile-assessment`, B)

*Chủ feature điền khi dịch `contracts/api-001.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 9. Feature 003 (`003-room-bed-allocation`, C)

*Chủ feature điền khi dịch `contracts/api-003.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 10. Feature 004 (`004-admission-residency`, B)

*Chủ feature điền khi dịch `contracts/api-004.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 11. Feature 005 (`005-daily-care-tasks`, C)

*Chủ feature điền khi dịch `contracts/api-005.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 12. Feature 006 (`006-medication-management`, D)

*Chủ feature điền khi dịch `contracts/api-006.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 13. Feature 007 (`007-health-alert-incident`, D)

*Chủ feature điền khi dịch `contracts/api-007.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 14. Feature 008 (`008-staff-shift-handover`, E)

*Chủ feature điền khi dịch `contracts/api-008.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

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
| kết quả gọi `DA_LIEN_LAC`, `KHONG_LIEN_LAC`; lý do `KHONG_NGHE_MAY`, `SAI_SO`, `KHONG_CO_SO`, `KHAC` | `REACHED`, `NOT_REACHED`; `NO_ANSWER`, `WRONG_NUMBER`, `NO_NUMBER`, `OTHER` |
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

### 15.5. Gói và kiểu Java của `m13` (T002 → T011)

| Tên logic | Mã |
| --- | --- |
| `m13.tiepnhan`, `m13.gui`, `m13.lich`; phần dùng chung; provider | `m13.internal.intake`, `m13.internal.delivery`, `m13.scheduling` + `m13.internal.job`; `m13.internal.domain`, `m13.internal.persistence`; `m13.internal.governance` |
| `m13.nguoinhan`, `yentinh`, `khancap`, `hopthu`, `lienhe`, `tracuu`, `cauhinh` (tạo khi có lớp) | `m13.internal.recipient`, `quiethours`, `urgent`, `inbox`, `directcontact`, `lookup`, `channel` |
| `ThongBaoTraCuuApi` (`theoNguon`, `xemLuc`), `NguoiDaThongBaoView` | `NotificationLookupApi` (`bySource`, `viewedAt`), `NotifiedPersonView` (`ChannelResult`, `CallReminderSummary`, `CallOutcome`; `missingLevel`) |
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
| *(bổ sung 2026-10-22, Phase 3)* `XacDinhNguoiNhan`, `ChuoiThay` (bậc thay), `LocPhanNoiDung`, `XetPhamViDongY`, `NguoiNhanDaXacDinh`; tạo thông báo lúc tiếp nhận; kết quả nhóm `CO_NGUOI_NHAN`, `DA_THAY`, `BO_QUA_KHONG_THAY`, `KHONG_CO` | gói `m13.internal.recipient`: `RecipientResolver` (`Resolution`), `SubstitutionChain` (`SubstitutionStep` `SHIFT_LEAD`, `FLOOR_MANAGER`, `FLOOR_MANAGER_SUBSTITUTE`, `MANAGERS`), `ContentFilter`, `ConsentScope` (`Decision` `SEND`, `SEND_WITH_CASE`, `SKIP`, `MISSING_CASE`), `ResolvedRecipient`; `NotificationCreation`; `HAS_RECIPIENTS`, `SUBSTITUTED`, `SKIPPED_NO_SUBSTITUTE`, `NONE` |
| *(bổ sung 2026-10-22, Phase 7)* `HopThuService`, `LuongThongBaoController`, đăng ký thiết bị đẩy; `TraCuuService`, lịch sử thông báo, lỗi gửi | gói `m13.internal.inbox`: `InboxService` (`Page`, `Opened`), `InboxStore`; bản ghi `InboxItem`, `InboxCursor`, `InboxFilter` (`m13.internal.inbox.domain`); `NotificationStreams` (SSE, `m13.internal.delivery`), `DeliveryStore.registerToken`; `InboxController`; gói `m13.internal.lookup`: `NotificationLookupService` (`HistoryEntry`), `LookupStore`; bản ghi `SentNotification`, `HistoryFilter`, `DeliveryError` (`m13.internal.lookup.domain`); `NotificationLookupController` (`ErrorType`); phản hồi `InboxBody`, `NotificationBody`, `NotificationHistoryBody`; thân `PushDeviceBody` |
| bản giả 012, nhà cung cấp tin nhắn giả, FCM giả | `support/fake/m13/FakeFamilyNotifications`, `FakeSmsProvider`, `FakeM13Config`; `m13/FakePushGateway` (test) |
| test `TrangThaiThongBaoTest`, `ChayBuM13IT`, `FR_003_KiemTraYeuCauIT`, `FR_005_ChongTrungIT`, `FR_006_LoiKhongHongNguonIT` | `NotificationStatusRulesTest`, `NFR_04_M13CatchUpIT`, `FR_003_RequestValidationIT`, `FR_005_DeduplicationIT`, `FR_006_SourceUnaffectedIT`; thêm `RequestValidationTest`, `NFR_03_DeliveryDispatcherIT`, `M13ProvidersIT`, `M13ArchTest` |
| biến môi trường khóa dịch vụ FCM | `FIREBASE_CREDENTIALS` (`eldercare.notification.fcm.credentials-file`) |

### 15.6. Bảng và cột (T006, `V009_01__notification.sql`)

| Tên logic | Mã |
| --- | --- |
| `kenh_thong_bao` (`ma`, `ten`, `bat`, `muc_gan`, `cau_hinh`, `phien_ban`) | `notification_channel` (`code`, `name`, `enabled`, `levels`, `config`, `version`) |
| `yeu_cau_thong_bao` (`module_nguon`, `loai_su_kien`, `nhom_nguon`, `loai_ban_ghi_nguon`, `id_ban_ghi_nguon`, `khoa_su_kien`, `muc`, `nct_id`, `tang_khu_vuc_id`, `noi_dung_rut_gon`, `tieu_de`, `nguoi_gay_ra_id`, `bao_nguoi_gay_ra`, `phat_sinh_luc`, `tiep_nhan_luc`, `ket_qua_tiep_nhan`, lý do, `dau`, `doi_tuong_thong_tin`, `ma_truong_hop`) | `notification_request` (`source_module`, `event_type`, `source_group`, `source_record_type`, `source_record_id`, `event_key`, `level`, `resident_id`, `floor_area_id`, `short_content`, `title`, `caused_by`, `notify_causer`, `occurred_at`, `received_at`, `intake_result`, `rejection_reason`, `flags`, `information_subject`, `q354_case`) |
| `yeu_cau_nhan_trung`, `yeu_cau_phan_noi_dung` (`thu_tu`, `loai_thong_tin`, `noi_dung`), `yeu_cau_nhom_nhan` (`nhom`, `tham_so`, `cach_thay`, `nhom_thay`, `ket_qua`) | `notification_request_duplicate`, `notification_request_content` (`seq`, `information_type`, `content`), `notification_request_recipient_group` (`group_code`, `params`, `substitution`, `substitute_group`, `outcome`) |
| `cach_thay` `MAC_DINH`, `KHONG_THAY`, `NGUOI_THAY_CHI_DINH`; kết quả nhóm `CO_NGUOI`, `THAY`, `BO_QUA_KHONG_THAY`, `KHONG_CO` | `DEFAULT`, `NO_SUBSTITUTE`, `DESIGNATED`; `HAS_RECIPIENTS`, `SUBSTITUTED`, `SKIPPED_NO_SUBSTITUTE`, `NONE` |
| `thong_bao` (`loai_nguoi_nhan`, `nguoi_nhan_id`, `tai_khoan_id`, `can_cu`, `phan_duoc_xem`, `chi_nhan_dang`, `ma_truong_hop`, `muc`, `tao_luc`) | `notification` (`recipient_type`, `recipient_id`, `account_id`, `basis`, `visible_parts`, `identity_only`, `q354_case`, `level`, `created_at`) |
| `su_kien_thong_bao` (`loai`, `cach_xac_nhan`, `ly_do`, `gui_du_kien`, `nguoi_thuc_hien_id`, `thoi_diem`) | `notification_event` (`type`, `acknowledgement_method`, `reason`, `scheduled_send_at`, `actor_id`, `occurred_at`, cột sinh `once_key`) |
| `thong_bao_trang_thai` (`trang_thai`, `gui_du_kien`, `gui_luc`, `xem_luc`, `xac_nhan_luc`, `cach_xac_nhan`, `an`, `nguoi_nhan_key`, `muc`, `nct_id`) | `notification_state` (`status`, `scheduled_send_at`, `sent_at`, `viewed_at`, `acknowledged_at`, `acknowledgement_method`, `hidden`, `recipient_key`, `level`, `resident_id`) |
| `lan_gui` (`kenh`, `dia_chi`, `lan_thu`, `du_kien_luc`), `ket_qua_lan_gui` (`ket_qua`, `ly_do`, `tin_tong_hop_id`, `thoi_diem`) | `delivery_attempt` (`channel`, `address`, `attempt_no`, `scheduled_at`), `delivery_result` (`result`, `reason`, `digest_attempt_id`, `occurred_at`) |
| `thiet_bi_day` (`tai_khoan_id`, `ma_thiet_bi`, `ung_dung`, `fcm_token`, `cap_nhat_luc`, `vo_hieu_luc`), `lan_chay_m13` | `push_device` (`account_id`, `device_id`, `app`, `fcm_token`, `updated_at`, `invalidated_at`), `m13_job_run` (`job`, `business_date`, `scheduler_run_id`, `completed_at`) |

## 16. Feature 010 (`010-incurred-costs`, E)

*Chủ feature điền khi dịch `contracts/api-010.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

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

## 22. Feature 016 (`016-reports-dashboard`, A)

*Chủ feature điền khi dịch `contracts/api-016.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 23. Feature 017 (`017-resident-balance-ledger`, E)

*Chủ feature điền khi dịch `contracts/api-017.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 24. Feature 018 (`018-residence-registration`, B)

*Chủ feature điền khi dịch `contracts/api-018.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 25. Feature 019 (`019-inventory-assets`, C)

*Chủ feature điền khi dịch `contracts/api-019.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*
