# Bảng thuật ngữ Việt – Anh cho mã nguồn

**Ngày**: 2026-10-16, bổ sung 2026-10-19 (mục 6, feature 000) | **Căn cứ**: constitution X (3.1.0), research R-03

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
| đơn vị `PHUT`, `GIO`, `NGAY`, `THANG`, `NAM`, `LAN`, `PHAN_TRAM`, đồng | `MINUTES`, `HOURS`, `DAYS`, `MONTHS`, `YEARS`, `TIMES`, `PERCENT`, `VND` |
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
| khóa quyền xem nhật ký (Phụ lục 27, dòng "Nhật ký thay đổi": X toàn viện, P trong phạm vi) | `AuditLogPermissions.VIEW_FACILITY`, `AuditLogPermissions.VIEW` (`PermissionKey("AUDIT_LOG", …)`, 002 đổi sang enum) |
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
| hành động nhật ký `THAM_SO_DOI`; khóa quyền tham số (xem, đổi) | `PARAMETER_CHANGED` (đối tượng `M15.PARAMETER`); `ParameterPermissions.VIEW`, `ParameterPermissions.CHANGE` (`PermissionKey("PARAMETER", …)`) |
| test `KiemTraThamSoTest`, `ThamSoIT`, `ThamSoQuyenIT`, `ThamSoBoDemSauCommitIT`, `ThamSoTaiThoiDiemIT`, `ThamSoBangIT` | `ParameterValidatorTest`, `ParameterChangeIT`, `ParameterPermissionIT`, `ParameterCacheAfterCommitIT`, `ParameterAsOfIT`, `ParameterTableIT` |
| bảng `yeu_cau_phe_duyet`, `ket_qua_ap_dung` | `approval_request`, `application_result` (`ApprovalRequestStore`); người duyệt, quyền xem: `ApproverDirectory`; `NhacQuaHanImpl` → `OverdueReminderImpl` |
| trạng thái yêu cầu `NHAP`, `CHO_DUYET`, `DA_DUYET`, `DA_AP_DUNG`, `AP_DUNG_KHONG_THANH`, `TU_CHOI`, `DA_HUY` | `ApprovalStatus.DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `APPLIED`, `APPLY_FAILED`, `REJECTED`, `CANCELLED` |
| hành động nhật ký `YC_LAP`, `YC_SUA_NHAP`, `YC_GUI_DUYET`, `YC_DUYET`, `YC_TU_CHOI`, `YC_HUY`, `YC_AP_DUNG`, `YC_AP_DUNG_KHONG_THANH`, `YC_AP_DUNG_LOI`, `YC_NHAC`, `YC_BAO_QLV`, `YC_CHUYEN_MO_RONG`, `YC_DONG_DO_TRANG_THAI_CUOI` | `REQUEST_CREATED`, `REQUEST_DRAFT_EDITED`, `REQUEST_SUBMITTED`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_CANCELLED`, `REQUEST_APPLIED`, `REQUEST_APPLY_FAILED`, `REQUEST_APPLY_ERROR`, `REQUEST_REMINDED`, `REQUEST_ESCALATED`, `REQUEST_EXTENDED_TRANSITION`, `REQUEST_CLOSED_FOR_FINAL_STATE` (đối tượng `M15.APPROVAL_REQUEST`) |
| sự kiện `YeuCauDaLap`, `YeuCauDaGuiDuyet`, `YeuCauCanNhacNguoiDuyet`, `YeuCauCanBaoQuanLyVien`, `YeuCauDaQuyetDinh`, `YeuCauDaApDung`, `YeuCauApDungKhongThanh`, `YeuCauDaHuy` | `RequestCreated`, `RequestSubmitted`, `ApproverReminderDue`, `ManagerEscalationDue`, `RequestDecided`, `RequestApplied`, `RequestApplyFailed`, `RequestCancelled` |
| hộp yêu cầu (`CUA_TOI`, `CHO_TOI_DUYET`, `TRONG_PHAM_VI`) | `ApprovalRequestService.Inbox` (`MINE`, `AWAITING_MY_APPROVAL`, `ALL_IN_SCOPE`); controller `ApprovalRequestController` |
| test `BangChuyenTrangThaiTest`, `YeuCauVongDoiIT`, `ApDungMotLanIT`, `ApDungBuIT`, `ApDungLoiIT`, `NhacYeuCauIT`, `QuyetDinhDongThoiIT`, `TuDuyetIT`, `GioiHanVaiTroDuyetIT`; handler mẫu `YeuCauMauHandler` | `RequestTransitionTableTest`, `ApprovalLifecycleIT`, `ApplyOnceIT`, `CatchUpApplyIT`, `ApplyFailureIT`, `PendingReminderIT`, `ConcurrentDecisionIT`, `SelfApprovalIT`, `ApproverRoleRestrictionIT`; `ApprovalFixtures.SampleRequestHandler` |
| `DinhChinhYeuCauHandler`, khóa quyền yêu cầu đính chính (lập, duyệt); test cách C | `CorrectionRequestHandler`, `CorrectionPermissions.REQUEST`, `CorrectionPermissions.APPROVE` (`PermissionKey("CORRECTION", …)`); `CorrectionApprovalIT` |
| loại dữ liệu lưu giữ yêu cầu phê duyệt | `M15.APPROVAL_REQUEST` (`ApprovalRequestRetention`) |
| `TepApi` (`taiLen`, `gan`), `QuyenTepProvider` | `FileApi` (`upload`, `attach`), `FilePermissionProvider` (`ownerType`, `canDownload`) |
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
