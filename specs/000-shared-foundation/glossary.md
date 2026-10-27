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
| bản giả `BT/support/gia002/` | `support/fake/m15/` (`FakeM15Config`, `FakeStaffProfiles`, `FakeLicenses`, `FakePracticeScopes`, `FakeAssignments`, `FakeDuties`, `FakeBedAllocations`, `FakeFamilyRelationships`, `FakeResidencyEnds`) |
| `xacthuc/internal/PhienService`, `xacthuc/web/XacThucTokenFilter` (T011); phiên đang mở; token phiên | `internal/auth/service/SessionService` (`resolve`), `web/security/SessionTokenFilter`, `web/security/SecurityConfig`; `ActiveSession` (`accountId`, `type`, `staffId`, `familyMemberId`, `roles`, `deviceId`, `channel`), `SessionAuthentication`; `SessionTokens` (`generate`, `hash`, `cookie`) |
| `xacthuc/internal/NguoiDungHienTaiImpl` (T012); bản Bộ lập lịch | `internal/auth/service/CurrentUserImpl`; `SchedulerUser`, `SchedulerUserAspect`; `ActiveSession` cài `CurrentUser` |
| `quyen/internal/TinhQuyenVaiTro` (T013); phạm vi `TOAN_VIEN`, `PHAN_CONG`, `QUAN_HE`; phương án quyền | `internal/permission/service/RolePermissionCalculator` (`current`, `at`); `PermissionScope` (`FACILITY`, `ASSIGNED`, `RELATIONSHIP`); `RoleGrant` (`scope`, `footnotes`, `source`), `RoleGrants` (`add`, `lower`), `GrantSource` (`ROLE`, `ADDITIONAL_GRANT`) |
| `xacthuc/internal/KhoiTaoQuanLyVien` (T016); lệnh dòng lệnh `khoi-tao-quan-ly-vien`; `HoSoNhanVienApi.taoHoSoToiThieu` | `internal/auth/service/FirstManagerBootstrap` (`run`, `Result`), `FirstManagerBootstrapRunner` (`eldercare.bootstrap-manager.run-once`, `.username`, `.full-name`), `PasswordConfig`; `StaffProfileApi.createMinimal(fullName, positionRole)` |
| hành động nhật ký `TK_TAO`, `VT_GAN`; đối tượng tài khoản; người thực hiện `KHOI_TAO` | `ACCOUNT_CREATED`, `ROLE_ASSIGNED`; `M15.ACCOUNT`; `actor_type = BOOTSTRAP` qua `AuditLogApi.writeAsBootstrap` |
| `quyen/internal/QuyenService` (T022); `phamvi/internal/PhamViService` (`trongPhamVi`, phần kéo lên US1); phân loại chú thích | `internal/permission/service/PermissionService`; `internal/scope/service/ScopeService` (`inScope`); `FootnoteRules` (`CONDITIONS`, `DUTIES`, `blockedAtRoleLayer`); nguồn quyền thêm `GrantSource.DUTY` |
| `quyen/internal/QuyenXemTruongService`, `GioiHanTruongSerializerModifier` (T023); `truongBiAn`; DTO có đối tượng | `internal/permission/service/FieldVisibilityService`, `FieldGroupSerializerModifier`, `FieldVisibilityJacksonConfig`; trường JSON `hiddenFields`; `api.VisibilityTarget` (`visibilityTarget()`) |
| *(bổ sung 2026-10-24, "Đổi api" cho 001 FR-023m)* DTO tự khai trường bị ẩn (`DtoTuAnTruong`, `truongTuAn`) | `m15.account.api.SelfHiddenFields` (`selfHiddenFields`); `FieldGroupSerializerModifier` bỏ các trường đó và ghi vào `hiddenFields` |
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
| thân lệnh trạng thái (`requestBodies/LenhTrangThai`: `lyDo`, `chiTiet`), thân chỉ có lý do (`LyDo`), thân đính chính (`DinhChinh`: `kieu`, `noiDungDung`, `lyDo`) | `StatusCommandBody` (`reason`, `details`), `ReasonBody` (`reason`), `CorrectionBody` (`type`, `correctedContent`, `reason`) |
| `chiTiet` của lệnh trạng thái: `doiPhanBoSangGiuongId`; chuyển viện `canCu` (`suCoId`, `chiDinh`); qua đời `bangChungTepId` | `details`: `switchAllocationToBedId`; `basis` (`incidentId`, `doctorOrder`); `evidenceFileId` |
| `KetQuaDinhChinh` (`cach`, `dinhChinhId`, `yeuCauPheDuyetId`) | `CorrectionResult` (`creationMethod`, `correctionId`, `approvalRequestId`) |
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

### 8.5. Gói, giao diện công khai `m01.api`, sự kiện (T001, T007 → T009)

| Tên logic | Mã |
| --- | --- |
| gói chức năng `hoso`, `trangthai`, `dongy`, `suckhoe`, `nguyenvong`, `danhgia`, `lich` | `internal.profile`, `internal.status`, `internal.consent`, `internal.health`, `internal.wish`, `internal.assessment`; `scheduling` |
| `m01.api` / `api/sukien`; `@NamedInterface` | `m01.api` (`dto`, `spi`, `events`); `resident`, `resident-spi`, `resident-events` |
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
| bản giả `support/gia001/` | `support/fake/m01/` (`FakeM01Config`) |

### 8.6. Bảng và migration (T003, T005, T006)

| Tên logic | Mã |
| --- | --- |
| `V001_01__tep.sql`, `V001_02__nguoi_cao_tuoi.sql`, `V001_03__danh_muc_m01.sql` | `V001_01__file.sql`, `V001_02__resident.sql`, `V001_03__m01_catalogs.sql` |
| `tep` (`uuid`, `ten_goc`, `kieu_mime`, `kich_thuoc`, `tai_len_boi`, `tai_len_luc`, `chu_so_huu_loai`, `chu_so_huu_id`, `gan_luc`) | `file` (`uuid`, `original_name`, `mime_type`, `size_bytes`, `uploaded_by`, `uploaded_at`, `owner_type`, `owner_id`, `attached_at`) |
| `nguoi_cao_tuoi` (`ma_ho_so`, `ho_so_truoc_id`, `ho_ten`, `ho_ten_so_khop`, `ngay_sinh`, `gioi_tinh`, `loai_giay_to`, `so_giay_to`, `cccd_dang_mo`, `trang_thai`, `trang_thai_tu_luc`, `muc_cham_soc_hien_hanh`, `phien_ban`) | `resident` (`profile_code`, `previous_profile_id`, `full_name`, `full_name_match`, `date_of_birth`, `gender`, `id_document_type`, `id_document_number`, `open_citizen_id`, `status`, `status_since`, `current_care_level`, `version`) |
| `lich_su_trang_thai_nct`; `muc_cham_soc`; `lich_su_muc_cham_soc` (`can_cu_loai`) | `resident_status_history`; `care_level`; `care_level_history` (`basis_type`) |
| `di_nguyen`, `muc_do_di_ung`, `nguon_thong_tin`, `nhu_cau_cham_soc` | `allergen`, `allergy_severity`, `information_source`, `care_need` |
| `lich_su_trang_thai_nct` (`tu_trang_thai`, `den_trang_thai`, `lenh`, `thoi_diem`, `nguoi_thuc_hien_id`, `do_he_thong`, `ly_do`, `can_cu_loai`, `can_cu_id`, `mien_dieu_kien`) | `resident_status_history` (`from_status`, `to_status`, `command`, `occurred_at`, `performed_by`, `by_system`, `reason`, `basis_type`, `basis_id`, `condition_waiver`) |
| `muc_cham_soc` (`ma`, `ten`, `thu_tu`) | `care_level` (`code`, `name`, `display_order`) |
| `lich_su_muc_cham_soc` (`muc_cham_soc`, `hieu_luc_tu`, `can_cu_id`, `tao_luc`, `tao_boi`) | `care_level_history` (`care_level`, `effective_from`, `basis_id`, `recorded_at`, `recorded_by`) |
| `nguoi_cao_tuoi` còn lại: `anh_tep_id`, `dia_chi_thuong_tru`, `dia_chi_lien_he`, `lien_he`, `thong_tin_dac_biet`, `tao_luc`, `tao_boi` | `photo_file_id`, `permanent_address`, `contact_address`, `contact`, `special_notes`, `created_at`, `created_by` |
| `di_nguyen` (`nhom`), `muc_do_di_ung` (`thu_tu`), `nguon_thong_tin`, `nhu_cau_cham_soc` | `allergen` (`allergen_group`), `allergy_severity` (`display_order`), `information_source`, `care_need` |
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
| các bảng còn lại của 001 (hồ sơ sức khỏe, mục sức khỏe, đánh giá…) | bổ sung vào mục này cùng migration (R-03) |

## 9. Feature 003 (`003-room-bed-allocation`, C)

*Chủ feature điền khi dịch `contracts/api-003.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

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

### 10.5. Gói, giao diện công khai `m02.api`, sự kiện (T001, T003, T004)

| Tên logic | Mã |
| --- | --- |
| gói chức năng `dangky`, `danhsachcho`, `hopdong`, `dichvu`, `thaydoi`, `vang`, `ketthuc`, `quadoi`, `tinhphi`, `trangthai`; `lich` | `internal.application`, `internal.waitinglist`, `internal.contract`, `internal.servicecatalog` (tránh trùng tầng `service`), `internal.change`, `internal.absence`, `internal.ending`, `internal.death`, `internal.residencyday`, `internal.transition`; `scheduling` |
| `m02.api` (`api/sukien`); `@NamedInterface` | `m02.api` (`dto`, `spi`, `events`); `admission`, `admission-dto`, `admission-spi`, `admission-events` |
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

*Tên kiểu trong chữ ký `m02.api` mà 10.5 chưa đặt, và chỗ cài khác 10.5, 10.6. Mọi chữ ký view là **provisional** tới khi module dùng xác nhận.*

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
| lớp trong `m02.internal.application` | `domain`: `AdmissionApplication`, `AdmissionOpinion`, `ApplicationStatus`, `OpinionChoice`, `OpinionStatus`; `persistence`: `AdmissionApplicationStore`; `service`: `AdmissionApplicationService`, `AdmissionOpinionService`, `AdmissionConditionService` (cài `AdmissionConditionApi`), `AdmissionContractConditions` ((b), (c), (e) dùng chung bean và danh sách), `AdmissionTransition` (tác động Hoàn tất, Hủy tiếp nhận), `AdmissionOpinionWarnings`, `NotYetAdmittedReminder`, `AdmissionApplicationScopeAssignment`, `AdmissionApplicationRetention`, `M02ActorNames` |
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
| lớp trong `m02.internal.waitinglist` | `domain`: `WaitingListEntry`, `WaitingListStatus`, `Circumstance`, `CircumstanceStatus`, `ScoreBreakdown`, `ScoreCause`, `ScoreHistoryRow`; `persistence`: `WaitingListStore`; `service`: `WaitingListService`, `PriorityScoreCalculator` (hàm thuần), `PriorityScoreTable` (và `Config` đọc CFG-M02-08), `ScoreRecalculation`, `CalculateScoresTask` (việc `CALCULATE_SCORES`, `latestDayOnly`), `WaitingListScoreListeners` (nghe `CareLevelChanged`, `ParameterChanged`), `ScoreAdjustmentRequestHandler`, `WaitingListTransition` (tác động lên hồ sơ chờ của lệnh Hủy tiếp nhận, do `M02StatusParticipant` gọi), `WaitingEntryUpdateReminder` (nhắc `WAITING_ENTRY_UPDATE`), `MatchingEntryService` (hồ sơ phù hợp, nhận `MatchCriteria` từ người gọi), `WaitingListEntryRetention`, `WaitingListScopeAssignment` |
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
| lớp trong `m02.internal.contract` | `domain`: `Contract`, `ContractContent` (`ServiceLine`, `AbsencePolicyLine`, `ArrivalLine`), `ContractVersion`, `NonStandardTerm`; `persistence`: `ContractStore`; `service`: `ContractService`, `ContractRules`, `ContractSnapshot` (JSON chuẩn hóa và băm SHA-256), `NonStandardTermDetector`, `ContractTermsRequestHandler`, `ContractTermsListeners`, `ContractReadService` (cài `ContractReadApi` và `ContractApi` của 001; thay `ContractLookup` khung), `ContractTransition` (Hủy tiếp nhận kéo theo hợp đồng), `ContractOverdueTask`, `ContractReminders`, `ContractScopeAssignment`, `ContractScanFilePermission` |
| phản hồi, thân yêu cầu | `ContractBody`, `ContractResultBody`, `ContractContentBody`, `NonStandardTermBody`, `AddendumBody`; `ContractInputBody` (kèm `FeeBillingPeriod`, `HoldBed`, `ServiceLineBody`, `AbsencePolicyLineBody`, `ArrivalLineBody`), `RecordSignedBody` |
| hợp đồng lệch glossary | `AddendumBody.Status` theo hợp đồng api-004 là `PENDING_EFFECTIVE`, `APPLIED`, `NOT_APPLICABLE`; 10.3 ghi `NOT_APPLIED` (lệch, chờ chủ dự án chọn khi làm Phase 7) |
| test | `ContractLifecycleIT`, `ContractContentLockDbIT`, `DBR_06_ContractNoOverlapIT`, `FR_021_RecordSignedIT`, `NonStandardTermDetectorTest`, `TermsApprovalIT`, `BR_M02_09_ContentAtDateIT`, `BR_M02_05_ContractReminderIT`, `FR_029_ContractOverdueIT`; nền `ContractTestBase`, `ContractFixtures` (thay `FakeContractReads` đã xóa) |

### 10.12. Bổ sung khi cài Phase 6 (US4, T039 → T041, 2026-10-26)

*Tên cài thực tế của phần đặt cọc. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `GET /hop-dong/{id}/dat-coc`; thân phản hồi | `GET /contracts/{id}/deposit`; `DepositBody` (`notRequired`, `requiredAmount`, `status`, `basis`), `DepositBody.Basis` (`at`, `transactionId`, `accountant` là họ tên Kế toán, `receiptNumber` luôn rỗng vì Q-385 bỏ số phiếu thu chi) |
| lớp trong `m02.internal.deposit.service` | `DepositViewService` (khoản cần đặt cọc từ hợp đồng, trạng thái từ `DepositStatusPort`; chưa có 017 thì Chưa đáp ứng), `DepositRecheckService.recheck(contractId)` (FR-032, listener của 017 sẽ gọi) |
| sự kiện khoản cọc | `DepositAmountChanged(contractId, newAmount)` phát khi hợp đồng vào Chờ ký (kể cả gửi ký lại sau Trả về nháp); `newAmount` bằng 0 khi không yêu cầu cọc; phụ lục đổi khoản cọc phát ở Phase 7 |
| test | `DepositConditionIT`, `FR_032_DepositAfterResidencyIT`, `Events017018IT` |

### 10.13. Bổ sung khi cài Phase 7 (US5, T042 → T047, T075, 2026-10-26)

*Tên cài thực tế của phần thay đổi lưu trú, phụ lục. Chờ chủ dự án duyệt cùng PR.*

| Tên logic | Mã |
| --- | --- |
| `V004_06__phu_luc.sql` (đổi tên theo 10.6: `V004_06__addendum.sql`); `phu_luc_hop_dong`, `yeu_cau_thay_doi_thuoc_tinh` | `V004_06__addendum.sql`; `contract_addendum` (`contract_id`, `approval_request_id`, `change_types`, `content_before`, `content_after`, `effective_date`, `status`, `content_version_id`, `created_at`), `change_request_attribute` (`approval_request_id`, `contract_id`, `resident_id`, `attribute`, `pending_effective`, `lock_key`) |
| trạng thái phụ lục (cột) | `PENDING_EFFECTIVE`, `APPLIED`, `NOT_APPLIED` (cột, theo 10.3); thân phản hồi `AddendumBody.Status` giữ `NOT_APPLICABLE` theo api-004 (chờ chủ dự án chọn, BL-66) |
| `THAY_DOI_LUU_TRU` (loại yêu cầu), `ThayDoiLuuTruHandler`, `LaThayDoiNho` | `M02.RESIDENCY_CHANGE`, `ResidencyChangeHandler`, `IsMinorChange` (`m02.internal.change.service`) |
| trạng thái mở rộng `DUOC_THAY_THE`, chuyển mở rộng | `SUPERSEDED`; mã chuyển `SUPERSEDE` (từ `DRAFT`, `PENDING_APPROVAL`, `APPROVED`) |
| loại thay đổi `LOAI_LUU_TRU`, `MUC_CHAM_SOC`, `DICH_VU`, `THOI_GIAN`, `GIUONG`, `LICH_DEN` | `RESIDENCE_TYPE`, `CARE_LEVEL`, `SERVICE`, `DURATION`, `BED`, `ARRIVAL_SCHEDULE` (`ChangeType`) |
| `newValue.services[]` | dòng thay đổi `{serviceId, action: ADD \| REMOVE, inPackage, quantity, customUnitPrice}` (không phải danh sách đích); khoản cọc đi kèm `newValue.depositAmount`, `newValue.noDepositRequired` |
| lớp trong `m02.internal.change` | `domain`: `ChangeType`, `Addendum`, `ResidencyChangeContent`; `persistence`: `AddendumStore`; `service`: `ResidencyChangeHandler`, `ResidencyChangeRules` (điều kiện FR-041, FR-045, FR-046; giá trị trước; ảnh hưởng chi phí), `AddendumContent` (áp thay đổi lên nội dung phiên bản, diff trước/sau), `IsMinorChange`, `ResidencyChangeService`, `ResidencyChangeListeners`, `CareLevelChangeRequestHandler` (cài `CareLevelChangeRequestApi`, thay khung ở `transition`) |
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
| `TraChinhSachVang` (hàm thuần) | `AbsencePolicyResolver` (`resolve`, `dayCare`, `noLine`; `Outcome`, `Hold` `YES`/`NO`/`MANAGER_DECIDES`, `Source` `CONTRACT`/`FACILITY`/`DEFAULT`) ở `m02.internal.absence.service` |
| lớp trong `m02.internal.absence` | `domain`: `AbsenceType`, `Absence`, `AbsenceDay`, `BedHoldChoice`, `BedHoldDecision`; `persistence`: `AbsenceStore`, `DayCareStore`; `service`: `AbsenceService` (đọc, gia hạn, quyết định giữ giường, tính lại sau đính chính), `AbsenceDayCalculator` (ghi `absence_day`), `AbsenceTransition` (tác động của `GRANT_TEMPORARY_ABSENCE`, `TRANSFER_OUT`, `RECORD_RETURN`), `AbsenceDayTask` (việc `ABSENCE_DAYS` của `M02_DAILY`), `AbsenceCorrectionProvider` (`M02.ABSENCE`), `BedHoldDecisionCorrectionProvider` (`M02.BED_HOLD_DECISION`), `DayCareService`, `AbsenceNames`; `AbsenceLookup` (`AbsenceApi.open`) ở `internal.transition.service` thay khung |
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
| lớp trong `m02.internal.ending` | `domain`: `ResidencyEnding`, `EndingStatus`, `Handover`, `RecipientType`, `EndingExceptionRecord`, `EndingConditionCode`; `persistence`: `ResidencyEndingStore`; `service`: `ResidencyEndingService` (lập, đổi ngày, hủy, bàn giao, lập yêu cầu ngoại lệ, xem), `EndingConditionService` (danh sách (a) → (h), cài `AdmissionConditionApi.endingConditions` qua `AdmissionConditionService`), `EndingTransition`, `EndingHandovers` (bàn giao hiện hành sau đính chính), `HandoverCorrectionProvider` (`M02.RESIDENT_HANDOVER`), `EndingExceptionHandler` (loại yêu cầu `M02.ENDING_EXCEPTION`), `EndingFootnoteCondition` (chú thích 39 của Phụ lục 27), `EndingOverdueTask` (việc `ENDINGS`), `EndingReminders` (`PAST_EXPECTED_DATE`), `ResidencyEndingReadApiImpl` |
| web | `ResidencyEndingController` (`POST /residency-endings`, `GET …/{id}`, `GET …/{id}/conditions`, `POST …/change-date`, `…/cancel`, `…/handover`, `…/request-exception`); thân `ResidencyEndingInputBody` (`EndCaseInput`), `ChangeExpectedEndDateBody`, `ResidentHandoverBody`, `RequestEndingExceptionBody`; phản hồi `ResidencyEndingBody` (`HandoverBody`), `ConditionListBody.ofEnding` (thêm `exception`: `ExceptionRefBody`) |
| `m02.api` mới (T108) | `ResidencyEndingReadApi` (`HoSoKetThucDocApi`: `preparing`, `settlementInfo`, `hasApprovedSettlementException`); `dto`: `EndCase` (thêm `DEATH` chỉ cho `SettlementInfoView`), `ResidencyEndingView`, `SettlementInfoView` |
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
| lớp trong `m02.internal.death` | `domain`: `DeathRecord`, `PostDeathTask`; `persistence`: `DeathStore`; `service`: `DeathTransition`, `PostDeathTaskService` (`PostDeathTaskApi`: `refresh`, `view`; thêm `list`, `recalculate`, `requestSettlementException`, `settlementExceptionAllowed`, `recordIdOf`), `DeathRecordService`, `DeathRecordCorrectionProvider`, `DeathEvidenceFilePermission`, `PostDeathReminders` |
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
| *(bổ sung 2026-10-23, Phase 6)* `GioYenTinh`, giữ và giải phóng, tính lại khi đổi CFG-M13-02; việc 20 giây giải phóng; `NhomNhan.NguoiCaoTuoi`; lý do từ chối nhóm (f) sai; lý do Không gửi khi giải phóng | gói `m13.internal.quiethours`: `QuietHours` (`Value`, `endIfWithin`), `QuietHoursPolicy`, `QuietHoursRelease` (`RELEASE_QUIET_HOURS`), `QuietHoursRescheduler`, `HeldNotificationStore`; `RecipientGroup.Resident`; `INVALID_RESIDENT_GROUP`; `RECIPIENT_NO_LONGER_ELIGIBLE` (`RESIDENT_DECEASED` khi có 001) |
| test `GioYenTinhTest`, `BR_M13_03_GioYenTinhIT`, `FR_024_DoiThamSoIT`, `FR_022a_NguoiCaoTuoiNhanIT` | `QuietHoursTest`, `BR_M13_03_QuietHoursIT`, `FR_024_ParameterChangeIT`, `FR_022a_ResidentRecipientIT` |
| *(bổ sung 2026-10-23, Phase 5)* `ChonKenh`, `KhungGop`, `KiemTraNoiDungKenhNgoai`, bean `ChuaCauHinh`, kênh tin nhắn; việc 20 giây đóng khung gộp, gửi lại kênh ngoài; số điện thoại người nhận; dấu nội dung kênh ngoài không hợp lệ | `ChannelSelection` (`Channel`, `Recipient`, `Planned`), `SmsBatching`, `OutOfAppContent` (`m13.internal.delivery.domain`); `UnconfiguredSmsProvider`, `SmsSender`, `OutOfAppPlanner`, `SmsWindowCloser` (`CLOSE_SMS_WINDOWS`), `OutOfAppResender` (`RESEND_OUT_OF_APP`), `SmsWindowStore`; `RecipientPhones`; `RequestFlag.INVALID_OUT_OF_APP_CONTENT` |
| *(bổ sung 2026-10-23, Phase 5)* `LienHeTrucTiepService`, phát hiện mục cần liên hệ, nhắc đầu ngày, đính chính `M13.KET_QUA_LIEN_HE`; `KenhService`; hành động nhật ký | gói `m13.internal.directcontact`: `DirectContactService` (`Result`), `DirectContactDetector` (`DIRECT_CONTACTS`), `DirectContactReminder` (`REMIND_DIRECT_CONTACTS`, sự kiện `DirectContactsPending`, khóa `DIRECT_CONTACT_PENDING:<ngày>`), `DirectContactStore`, `DirectContact`; `DirectContactResultCorrection` (`M13.DIRECT_CONTACT_RESULT`), `DirectContactFieldClassification` (`M13.DIRECT_CONTACT`); gói `m13.internal.channel`: `ChannelService`, `ChannelStore`, `ChannelConfig`; `DIRECT_CONTACT_RECORDED`, `NOTIFICATION_CHANNEL_CHANGED`; lỗi `FINAL_STATE` |
| SPI `HoSoNhanVienApi.soDienThoai` *(2026-10-23, luật "Đổi api")* | `StaffProfileApi.phoneNumber` |
| *(bổ sung 2026-10-23, US1 phần xác nhận)* `KhanCapService` phần xác nhận, `KhanCapController`; hành động nhật ký xác nhận; test `NhomGhiNhanCoTriggerIT` của 009 | `m13.internal.urgent.service.UrgentAcknowledgement`, `m13.web.UrgentController`; `NOTIFICATION_ACKNOWLEDGED`; `M13RecordGroupTriggerIT` |
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
| *(bổ sung 2026-10-23, `V009_03`)* `khung_gop_tin_nhan` (`nguoi_nhan_key`, `bat_dau`, `ket_thuc`, `da_dong_luc`, `tin_tong_hop_lan_gui_id`, `khoa_mo`); `lan_gui` thêm khung gộp, nội dung riêng; `lien_he_truc_tiep` (`thong_bao_id`, `nguoi_than_id`, `ho_ten`, `quan_he`, `so_dien_thoai`, `noi_dung_rut_gon`, `trang_thai` `CHO_LIEN_HE`, `DA_LIEN_HE`, `KHONG_LIEN_HE_DUOC`, `da_nhac_luc`, `phien_ban`); `ket_qua_lien_he` (`nguoi_lien_he_id`, `thoi_diem`, `ket_qua`, `ly_do`, `ghi_chu`) | `sms_batch_window` (`recipient_key`, `started_at`, `ends_at`, `closed_at`, `digest_attempt_id`, `open_key`; thêm `first_attempt_id`); `delivery_attempt` thêm `batch_window_id`, `content`; `direct_contact` (`notification_id`, `family_member_id`, `full_name`, `relationship`, `phone_number`, `short_content`, `status` `PENDING`, `CONTACTED`, `UNREACHABLE`, `reminded_at`, `version`); `direct_contact_result` (`contacted_by`, `occurred_at`, `result`, `reason`, `note`, `recorded_at`) |
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
| `CV.HOM_NAY`, `CV.QUA_HAN`, `CV.THEO_TRANG_THAI`, `CV.TY_LE_DUNG_HAN_BAT_BUOC`, `CV.GHI_NHAN_MUON`, `CV.THEO_NHAN_VIEN` | `TASK.TODAY`, `TASK.OVERDUE`, `TASK.BY_STATUS`, `TASK.MANDATORY_ON_TIME_RATE`, `TASK.LATE_RECORDINGS`, `TASK.BY_STAFF` |
| `BUA.PHIEU_TRE`, `BUA.PHIEU_SAI_LECH` | `MEAL.LATE_TICKETS`, `MEAL.DISCREPANCY_TICKETS` |
| `HD.BUOI`, `HD.TY_LE_THAM_GIA`, `HD.CHUYEN_QUA_GIO`, `HD.THIEU_NGUOI`, `HD.CO_LAP` | `ACT.SESSIONS`, `ACT.PARTICIPATION_RATE`, `ACT.OVERDUE_TRIPS`, `ACT.MISSING_PARTICIPANTS`, `ACT.ISOLATION_RISK` |
| `KT.TY_LE_DAT`, `KT.THEO_VAI_TRO_KIEM_TRA` | `QC.PASS_RATE`, `QC.BY_CHECKER_ROLE` |
| `SK.CHI_SO`; `SK.CANH_BAO`, `SK.CANH_BAO_THOI_GIAN_TIEP_NHAN`, `SK.CANH_BAO_LEO_THANG`, `SK.CANH_BAO_SOS_HO_TRO` | `HEALTH.VITAL_SIGNS`; `HEALTH.ALERTS`, `HEALTH.ALERT_RESPONSE_TIME`, `HEALTH.ALERT_ESCALATIONS`, `HEALTH.SOS_ASSISTANCE_ALERTS` |
| `SK.SU_CO`, `SK.SU_CO_LEO_THANG`, `SK.SU_CO_THOI_GIAN_TIEP_NHAN`, `SK.THEO_DOI`, `SK.SU_CO_KHONG_THUOC_SK` | `HEALTH.INCIDENTS`, `HEALTH.INCIDENT_ESCALATIONS`, `HEALTH.INCIDENT_RESPONSE_TIME`, `HEALTH.FOLLOW_UPS`, `HEALTH.NON_HEALTH_INCIDENTS` |
| `SK.DANH_GIA_LAI_QUA_HAN`, `SK.KHOANH_VUNG`, `SK.NGUY_KICH` | `HEALTH.OVERDUE_REASSESSMENTS`, `HEALTH.CONTAINMENT_ZONES`, `HEALTH.CRITICAL_FLAGS` |
| `THUOC.TY_LE_BO_LO`, `THUOC.TY_LE_TU_CHOI`, `THUOC.DOI_CHIEU_QUA_HAN` | `MED.MISSED_RATE`, `MED.REFUSED_RATE`, `MED.OVERDUE_RECONCILIATIONS` |
| `CA.BAN_GIAO_CHUA_XAC_NHAN`, `CA.TY_LE_PHUC_VU`, `CA.THIEU_PHU`, `CA.THIEU_CA`, `CA.YEU_CAU`, `CA.LOI_MOI`, `CA.GIO_LAM` | `SHIFT.UNCONFIRMED_HANDOVERS`, `SHIFT.STAFFING_RATIO`, `SHIFT.UNDER_COVERAGE`, `SHIFT.MISSING_SHIFTS`, `SHIFT.REQUESTS`, `SHIFT.INVITATIONS`, `SHIFT.WORKING_HOURS` |
| `GP.CO_SO`, `GP.NHAN_VIEN` | `LICENSE.FACILITY`, `LICENSE.STAFF` |
| `CP.BANG`, `CP.TONG_HOP`, `CP.DIEU_CHINH_SAU_CHOT`, `CP.TY_LE_THEO_SU_KIEN`, `CP.MUA_SAM` | `COST.STATEMENTS`, `COST.SUMMARY`, `COST.POST_FINALIZATION_ADJUSTMENTS`, `COST.EVENT_GENERATED_RATE`, `COST.PURCHASING` |
| `SD.THAP_CON_NO`, `KB.CAN_LAM`, `TS.QUA_HAN`, `KHO.DUOI_TOI_THIEU`, `KHO.LO_SAP_HET_HAN` | `BALANCE.LOW_OR_OWING`, `REGISTRATION.TO_DO`, `ASSET.OVERDUE`, `INVENTORY.BELOW_MINIMUM`, `INVENTORY.EXPIRING_LOTS` |

### 22.5. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `VUOT_KHOANG_TOI_DA` (CFG-M14-01), `THIEU_MUC_DICH` | `RANGE_EXCEEDS_MAXIMUM` (422), `PURPOSE_REQUIRED` (422) |
| `KHONG_XAC_DINH_PHAM_VI` (FR-016) | không có mã riêng: `PERMISSION_DENIED` (403) kèm `failedLayer` `SOURCE_DATA` như 002 (chủ dự án chọn 2026-10-24) |

### 22.6. Gói và thư mục (dự kiến, chốt cùng mã)

| Tên logic | Mã |
| --- | --- |
| `m14.chitieu`, `phamvi`, `dashboard`, `baocao`, `chitiet`, `xuat` | `m14.internal.metric`, `scope`, `dashboard`, `report`, `detail`, `export` (mỗi gói `{service, domain}`); controller ở `m14.web` |
| *(bổ sung 2026-10-24, Phase 1, 2)* `ChiTieu`, `GiaTriChiTieu`, `DongChiTiet`, bản ghi nguồn, danh mục chỉ tiêu; phép đếm, tỷ lệ, trung bình/lớn nhất/tổng; nhãn | `m14.internal.metric.domain`: `Metric` (`Computation.Count`, `Ratio`, `Aggregate` `AVERAGE`/`MAXIMUM`/`SUM`), `MetricValue` (`Breakdown`), `DetailRow`, `MetricRecord`, `ValueLabel`; `m14.internal.metric.service.MetricCatalog` |
| nhóm chỉ tiêu FR-010 (giá trị cột `metric_group` của 002); phạm vi ô `TOAN_VIEN`, `TANG_DUOC_GIAO`, `PHAN_CONG`, tầng/khu của phân công, `NHAN_VIEN_TANG`; `QuyenNhomChiTieu` | `MetricGroup` (`RESIDENTS`, `WAITING_LIST`, `BEDS`, `CARE_TASKS`, `PER_STAFF`, `BED_AWAITING_READY`, `MEAL_TICKETS`, `ACTIVITIES`, `QUALITY_CHECKS`, `HEALTH_EVENTS`, `NON_HEALTH_INCIDENTS`, `OVERDUE_REASSESSMENTS`, `MEDICATION_DOSES`, `MEDICATION_RECONCILIATIONS`, `CONTAINMENT_ZONES`, `HANDOVERS`, `STAFFING_RATIO`, `FACILITY_LICENSES`, `STAFF_LICENSES`, `INCURRED_COSTS`, `PURCHASING`, `BALANCES`, `RESIDENCE_REGISTRATION`, `ASSETS`, `INVENTORY`, `CRITICAL_FLAGS`, `STAFFING_SHIFTS`); `GroupScope` (`FACILITY`, `ASSIGNED_FLOORS`, `ASSIGNMENT`, `ASSIGNMENT_AREA`, `FLOOR_STAFF`); `MetricGroupAccess` |
| `PhamViBaoCao` (phần phạm vi, lọc thô, lọc chính xác); giới hạn trường dòng chi tiết; kiểm khoảng CFG-M14-01 | `m14.internal.scope`: `ReportScope` (`Grant`, `rough`, `covers`), `ReportScopeService` (`dashboard`, `period`), `DetailRowPolicy`, `ReportPeriods` (`ReportPeriod`) |
| *(bổ sung 2026-10-24, "Đổi api" 002)* `QuyenApi.thuHepNhomChiTieu`, `ThuHepNhomChiTieu` (theo vai trò, theo tài khoản; dòng `nhomChiTieu`, `kenh`, loại `BO_THAO_TAC`, `HA_PHAM_VI`); `QuyenXemTruongApi.duocXemTheoVaiTro`; kênh `DASHBOARD`, `BAO_CAO` | `PermissionApi.metricGroupRestrictions`, `m15.account.api.dto.MetricGroupRestrictions` (`byRole`, `byAccount`; `Restriction` `metricGroup`, `channel`, `kind`, `appliesTo`; `Kind` `REMOVE_OPERATION`, `LOWER_SCOPE`); `FieldVisibilityApi.canViewByRole`; `m15.account.api.dto.ReportChannel` (`DASHBOARD`, `REPORT`); test `MetricGroupRestrictionsIT` |
| test `PhamViBaoCaoTest`; test khung chỉ tiêu, bảng FR-010, giới hạn trường, khoảng | `ReportScopeTest`; `MetricTest`, `MetricGroupAccessTest`, `DetailRowPolicyTest`, `FR_060_ReportPeriodTest`, `M14ArchTest` |
| *(bổ sung 2026-10-24, Phase 11)* nguồn bản ghi của chỉ tiêu; bộ lọc báo cáo; chế độ chi phí | `m14.internal.metric.domain.MetricSource` (`metric`, `records`), `ReportFilters` (`floorAreaIds`, `residenceType`, `careLevelId`, `residentId`, `staffId`, `shiftId`, `costMode`; `NONE`, `matches`, `toMap`), `ReportFilters.CostMode`; `Metric.residents`; `MetricCatalog.sourcesIn`; `ReportScopeService.visible` |
| nhóm báo cáo và nhóm chỉ tiêu trong báo cáo; báo cáo đã tính, kết quả một chỉ tiêu | `m14.internal.report`: `ReportGroup` (`label`, `metricGroups`), `Report`, `MetricResult` (`residents`, `residentsByKey`, `rows`), `ReportService.period` |
| `XuatBaoCaoService`, `YeuCauXuat`, `NoiDungDuocXuat`, file xuất, `QuyenTepProvider` `M14.BAO_CAO_XUAT`, định dạng | `m14.internal.export`: `ReportExportService` (`export`, `list`, `canDownload`; `ExportRequest`, `ExportResult` {`fileId`, `auditLogId`}, `ExportRecord`, `ExportFormat`), `ExportContent` (`decide`, `SummaryRow`, `DetailLine`, `rowCount`), `ExportWorkbook` (`Header`), `ReportExportFilePermission`; `m14.web.ReportExportController`, `web.request.ExportRequestBody` |
| chỉ tiêu `GP.CO_SO` | `m14.internal.license.service.FacilityLicenseMetric` (`FacilityLicenseRecord`: `licenseId`, `name`, `expiryDate`, `daysRemaining`) |
| test `FR_061_NoiDungXuatTest`, `FR_062_NhatKyXuatIT`, `ChiTieu_GP_CoSoTest`; chỉ tiêu giả của test | `FR_061_ExportContentTest`, `FR_062_ExportAuditIT`, `MetricLicenseFacilityTest`; `TestDoseMetric` (`TEST.MISSED_DOSES`) |
| *(bổ sung 2026-10-24, T009, khung T014, T022)* tính một chỉ tiêu; `DashboardService` (khối, trạng thái khối, bộ đệm); `ChiTietService`; controller; thân phản hồi | `m14.internal.metric.service.MetricEvaluator` (`value`, `result`, `details`); `m14.internal.dashboard.service.DashboardService` (`dashboard`, `metrics`, `GROUPS`, `Block`, `BlockStatus` `COMPLETE`/`UPDATING`, `Dashboard`); `m14.internal.detail.service.MetricDetailService` (`MetricDetail`, `PAGE_SIZE`); `MetricCatalog.source`; `m14.web.DashboardController`, `ReportController`; `web.response.DashboardBody`, `MetricValueBody`, `ReportBody`, `MetricBody`, `MetricDetailBody` |
| test dashboard, báo cáo, chi tiết | `FR_020_DashboardIT`, `ReportApiIT`; web `DashboardPage.test.jsx`, `ReportsPage.test.jsx` |
| màn web (chốt 2026-10-24): một màn Báo cáo có tab cho mọi nhóm thay các thư mục theo nhóm | `features/dashboard/DashboardPage.jsx`; `features/reports/` (`ReportsPage`, `ReportTable`, `MetricDetailSheet`, `ExportDialog`, `ExportList`, `reportLabels`) |
| web `tinh-nang/bao-cao/{dashboard, nguoi-cao-tuoi, cham-soc, chat-luong, suc-khoe-thuoc, chi-phi, nhan-su-ca, chi-tiet}` | `features/reports/{dashboard, residents, care, quality, health-medication, incurred-costs, staffing-shifts, metric-details}` |

## 23. Feature 017 (`017-resident-balance-ledger`, E)

*Chủ feature điền khi dịch `contracts/api-017.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 24. Feature 018 (`018-residence-registration`, B) *(bổ sung 2026-10-27, khi dịch `contracts/api-018.yaml`)*

"Khai báo tạm trú, lưu trú" dịch là `residence registration` (mục 4); hai loại theo luật cư trú: Đăng ký tạm trú `TEMPORARY_RESIDENCE_REGISTRATION`, Thông báo lưu trú `STAY_NOTIFICATION`. "Thường trú cùng xã/phường với viện" (Q-215) dịch là `local permanent resident`. Bảng, lớp của 018 bổ sung vào mục này cùng thay đổi với mã (R-03).

### 24.1. Đường dẫn API (tiền tố `/api/v1`)

| Tên logic | Mã |
| --- | --- |
| `/khai-bao/can-lam` | `/residence-registrations/to-do` (cùng tên chỉ tiêu `REGISTRATION.TO_DO` của 016) |
| `/nguoi-cao-tuoi/{nctId}/khai-bao` | `/residents/{residentId}/residence-registrations` |
| `/khai-bao/{id}/{lenh}` | `/residence-registrations/{id}/{command}` |
| lệnh `xac-nhan-thuong-tru-cung-xa`, `ghi-da-nop`, `ghi-chap-nhan`, `ghi-tu-choi`, `ghi-da-khai-bao-xoa` | `confirm-local-permanent-resident`, `record-submitted`, `record-accepted`, `record-rejected`, `record-deregistered` |
| tag `khai-bao`; security `phien` | `residence-registrations`; `session` |

### 24.2. Schema, tham số và trường JSON

| Tên logic | Mã |
| --- | --- |
| tham số `NctId` (`nctId`) | `ResidentId` (`residentId`) |
| thân lệnh lập `giaHan` | `renewal` |
| `KhaiBao` (`nctId`, `loai`, `laGiaHan`, `hanKhaiBao`, `trangThai`, `ngayHetHan`, `thuongTruCungXa`) | `ResidenceRegistration` (`residentId`, `type`, `renewal`, `dueAt`, `status`, `expiryDate`, `localPermanentResident`) |
| `ViecKhaiBao` (`nctId`, `hoTen`, `loaiViec`, `han`, `khaiBaoId`) | `RegistrationTask` (`residentId`, `fullName`, `taskType`, `dueAt`, `registrationId`) |
| `LenhKhaiBao` (`ghiChuCanCu`, `ngayNop`, `kenh`, `coQuan`, `maHoSo`, `tepBangChungId`, `ngayHetHan`, `lyDo`) | `RegistrationCommand` (`basisNote`, `submittedDate`, `channel`, `receivingAuthority`, `dossierCode`, `evidenceFileId`, `expiryDate`, `reason`) |
| `Loi`, phản hồi `Loi`, `LoiChan` | `Problem` (bản chung của api-000, BL-23), phản hồi `Problem`, `Conflict` |

### 24.3. Enum

| Tên logic | Mã |
| --- | --- |
| loại `DANG_KY_TAM_TRU`, `THONG_BAO_LUU_TRU` | `TEMPORARY_RESIDENCE_REGISTRATION`, `STAY_NOTIFICATION` |
| trạng thái `CAN_KHAI_BAO`, `DA_NOP`, `DA_XAC_NHAN`, `HET_HAN`, `DA_XOA`, `DUOC_THAY_THE`, `DA_HUY` | `TO_SUBMIT`, `SUBMITTED`, `CONFIRMED`, `EXPIRED`, `DEREGISTERED`, `SUPERSEDED`, `CANCELLED` |
| loại việc `CAN_LAP`, `QUA_HAN`, `SAP_HET_HAN`, `HET_HAN`, `CAN_KHAI_BAO_XOA` | `TO_CREATE`, `OVERDUE`, `EXPIRING_SOON`, `EXPIRED`, `TO_DEREGISTER` |
| kênh nộp `CONG_DICH_VU_CONG`, `TRUC_TIEP` | `PUBLIC_SERVICE_PORTAL`, `IN_PERSON` |
| loại đính chính `M02.KHAI_BAO`; loại tệp `M02.BANG_CHUNG_KHAI_BAO` | `M02.RESIDENCE_REGISTRATION`; `M02.REGISTRATION_EVIDENCE` |

### 24.4. Mã lỗi

| Tên logic | Mã |
| --- | --- |
| `TRANG_THAI_DA_DOI` | `STATE_CHANGED` (mục 5) |
| `KHONG_PHAI_NOI_TRU`, `CHUA_PHAT_SINH_NGHIA_VU`, `DA_CO_KHAI_BAO_DANG_XU_LY` | `NOT_RESIDENTIAL_CARE`, `NO_REGISTRATION_OBLIGATION`, `REGISTRATION_IN_PROGRESS_EXISTS` |
| `CHUA_CO_DANG_KY_TAM_TRU_DA_XAC_NHAN`, `CHUA_GHI_NOP` | `NO_CONFIRMED_TEMPORARY_RESIDENCE_REGISTRATION`, `NOT_SUBMITTED` |
| `THIEU_BANG_CHUNG`, `THIEU_NGAY_HET_HAN`, `CHUA_KET_THUC_LUU_TRU` | `EVIDENCE_REQUIRED`, `EXPIRY_DATE_REQUIRED`, `RESIDENCY_NOT_ENDED` |

### 24.5. Gói và kiểu Java (T001)

| Tên logic | Mã |
| --- | --- |
| gói `m02.khaibao` (`internal`, `web`, `lich`) | `m02.internal.registration`; `m02.web`, `m02.scheduling` (dùng chung với 004) |
| test `KhaiBaoArchTest` | `ResidenceRegistrationArchTest` (chỉ cấm Bộ lập lịch lập khai báo) |
| lệnh lập khai báo, lập khai báo gia hạn; lớp ghi bảng khai báo | tên bắt đầu `create` (chỉ dành cho lập khai báo trong `m02.internal.registration`); `ResidenceRegistrationStore` (`insert…`) |
| bảng `khai_bao_nct` (`nct_id`, `thuong_tru_cung_xa`, `ghi_chu_can_cu`, `xac_nhan_boi_id`, `xac_nhan_luc`, `phien_ban`) *(T002)* | `registration_resident` (`resident_id`, `local_permanent_resident`, `basis_note`, `confirmed_by_id`, `confirmed_at`, `version`) |
| bảng `khai_bao_tam_tru` (`loai`, `la_gia_han`, `nghia_vu_id`, `han_khai_bao`, `trang_thai`, `lan_nop_hien_hanh_id`, `ngay_het_han`, `tep_bang_chung_id`, `xoa_luc`, `xoa_boi_id`, `tep_bang_chung_xoa_id`, `thay_the_khai_bao_id`, `khoa_dang_xu_ly`, `khoa_xac_nhan`) | `residence_registration` (`type`, `renewal`, `obligation_id`, `due_at`, `status`, `current_submission_id`, `expiry_date`, `evidence_file_id`, `deregistered_at`, `deregistered_by_id`, `deregistration_evidence_file_id`, `replaced_registration_id`, `in_progress_key`, `confirmed_key`) |
| bảng `khai_bao_lan_nop` (`khai_bao_id`, `ngay_nop`, `kenh`, `co_quan`, `ma_ho_so`, `ket_qua` `CHO`/`CHAP_NHAN`/`TU_CHOI`, `ly_do_tu_choi`, `boi_id`, `luc`) | `registration_submission` (`registration_id`, `submitted_date`, `channel`, `receiving_authority`, `dossier_code`, `result` `PENDING`/`ACCEPTED`/`REJECTED`, `rejection_reason`, `recorded_by`, `recorded_at`) |
| bảng `nghia_vu_khai_bao` (`moc_loai` `HOAN_TAT_TIEP_NHAN`/`NOI_TRU_HIEU_LUC`/`PHU_LUC_GIA_HAN`/`HET_HAN`, `moc_luc`, `han`, `nguon_id`, `khai_bao_id`, `da_nop`, `da_bao_ql_luc`, `huy_luc`) | `registration_obligation` (`trigger_type` `ADMISSION_COMPLETED`/`RESIDENTIAL_EFFECTIVE`/`RENEWAL_ADDENDUM`/`EXPIRED`, `trigger_at`, `due_at`, `source_id`, `registration_id`, `submitted`, `manager_notified_at`, `cancelled_at`) |
| bảng `nhac_khai_bao` (`loai` `LAP`/`QUA_HAN`/`GIA_HAN`/`XOA`, `nct_id`, `ngay`) | `registration_reminder` (`type` `CREATE`/`OVERDUE`/`RENEWAL`/`DEREGISTER`, `resident_id`, `date`); enum `ReminderType`, lớp `RegistrationReminderStore` |
| hàm `TongThoiGianLuuTru`, `LoaiKhaiBao` *(T003)* | `ExpectedStayDuration` (`StayPeriod`), `RegistrationTypeRule` (trả `RegistrationType`) |
| job `ViecHangNgayM02KhaiBao` (`ViecKhaiBao`), test `ChayBuM02KhaiBaoIT` *(T004)* | `ResidenceRegistrationJob` (tên `M02_REGISTRATION`), `RegistrationDailyRunner`, `RegistrationDailyTask`; `RegistrationCatchUpIT` |
| `PhanLoaiTruongProvider`, `LuuTruProvider`, `QuyenTepProvider`, `GanPhamViProvider` của khai báo *(T005)* | `RegistrationFieldClassificationConfig`, `RegistrationRetention`, `RegistrationEvidenceFilePermission`, `RegistrationScopeAssignment` |
| `NghiaVuService` (listener `TiepNhanHoanTat`, `HopDongDaDoiTrangThai`, ghi mốc) *(T007)* | `RegistrationObligationListeners`, `RegistrationObligations`; lớp ghi bảng `RegistrationObligationStore` (`record`, không phải `insert…`); enum mốc `ObligationTrigger`; kiểu `RegistrationObligation` |
| nhắc quá hạn (Hành chính mỗi ngày, Quản lý viện một lần) *(T007)* | `RegistrationOverdueReminders.remind(day)` |
| sự kiện nhắc khai báo (FR-010) | `m02.api.events.ResidenceRegistrationReminder` (`Kind`: `TO_CREATE`, `OVERDUE`, `OVERDUE_MANAGER`); loại sự kiện thông báo `M02_REGISTRATION_<Kind>`; nguồn `M02.REGISTRATION_REMINDER` |
| `KhaiBaoService`, `KhaiBaoController` *(T008)* | `ResidenceRegistrationService` (lệnh `createRegistration`, `confirmLocalPermanentResident`; đọc `ofResident`, `toDo`), `ResidenceRegistrationController`; khóa `RegistrationResidentStore.lock`; kiểu `ResidenceRegistration`, `RegistrationStatus`, `SubmissionChannel`; thân `CreateRegistrationBody`, `RegistrationCommandBody`, `ResidenceRegistrationBody`, `RegistrationTaskBody` |
| hành động nhật ký lập khai báo, xác nhận thường trú cùng xã/phường | `M02_REGISTRATION_CREATED`, `M02_REGISTRATION_LOCAL_CONFIRMED` |
| test `BR_M02_11_KhaiBaoIT`, `DBR_33_MotKhaiBaoIT` *(T006)* | `BR_M02_11_ResidenceRegistrationIT`, `DBR_33_OneRegistrationIT` (nền `RegistrationTestBase`) |
| lệnh ghi đã nộp, ghi được chấp nhận, ghi bị từ chối *(T010)* | `ResidenceRegistrationService.recordSubmitted` (`Submission`), `recordAccepted` (`Acceptance`), `recordRejected`; lần nộp `RegistrationSubmission`, `SubmissionResult` (`PENDING`, `ACCEPTED`, `REJECTED`), `RegistrationSubmissionStore` (`add`, `setResult`) |
| hạn đang có hiệu lực của nghĩa vụ (khai báo bị từ chối: hạn mới của khai báo; còn lại: hạn nghĩa vụ) *(T010)* | `RegistrationObligation.effectiveDueAt` (cột tính `effective_due_at`) |
| `DinhChinhDuocProvider` `M02.KHAI_BAO` *(T010)* | `RegistrationCorrectionProvider` (loại `M02.RESIDENCE_REGISTRATION`) |
| hành động nhật ký ghi nộp, chấp nhận, từ chối | `M02_REGISTRATION_SUBMITTED`, `M02_REGISTRATION_ACCEPTED`, `M02_REGISTRATION_REJECTED` |
| test `KhaiBaoNopKetQuaIT` *(T009)* | `RegistrationSubmissionResultIT` |
| lệnh lập khai báo gia hạn *(T012)* | `ResidenceRegistrationService.createRenewalRegistration` |
| nhắc gia hạn, chuyển Hết hạn *(T012)* | `RegistrationRenewalReminders.remind(day)` (`due`), `RegistrationExpiry.expire(day)`; ngày hết hạn hiện hành theo đính chính `RegistrationExpiryDates`; tên việc dự kiến `REG_RENEWAL`, `REG_EXPIRY` |
| loại nhắc gia hạn, báo Hết hạn | `ResidenceRegistrationReminder.Kind.RENEWAL`, `EXPIRED_MANAGER`; hành động nhật ký `M02_REGISTRATION_EXPIRED` |
| test `BR_M02_12_GiaHanIT`, `BR_M02_14_ChuyenLoaiIT` *(T011)* | `BR_M02_12_RenewalIT`, `BR_M02_14_TypeChangeIT` |
| bean `ThamGiaKhaiBao` (`ThamGiaChuyenTrangThai`) *(T014)* | `RegistrationStatusParticipant` |
| hủy khai báo đang xử lý, nghĩa vụ đang mở khi sang trạng thái cuối; "cần khai báo xóa"; nhắc khai báo xóa *(T014)* | `RegistrationDeregistration` (`finalStatusReached`, `due`, `remind(day)`); tên việc dự kiến `REG_DEREGISTER` |
| lệnh ghi đã khai báo xóa *(T014)* | `ResidenceRegistrationService.recordDeregistered` |
| loại nhắc khai báo xóa; hành động nhật ký hủy, khai báo xóa | `ResidenceRegistrationReminder.Kind.DEREGISTER`; `M02_REGISTRATION_CANCELLED`, `M02_REGISTRATION_DEREGISTERED` |
| test `BR_M02_13_KhaiBaoXoaIT` *(T013)* | `BR_M02_13_DeregistrationIT` |

## 25. Feature 019 (`019-inventory-assets`, C)

*Chủ feature điền khi dịch `contracts/api-019.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*
