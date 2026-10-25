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
| `BT/support/gia003/`, bản giả năm cổng T002 | `support/fake/m03/`: `FakeM03Config`, `FakeAllocationResidencySource`, `FakeDayCareArrivals`, `FakeAllocationInfectionStatus`, `FakeLocationReferences`, `FakeBedAssets` |
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
| `khu_vuc`, `tang`, `loai_phong`, `loai_phong_muc`, `phong`, `phong_muc_cho_phep`, `ly_do_chuyen_giuong` | `area`, `floor`, `room_type`, `room_type_care_level`, `room`, `room_allowed_care_level`, `bed_transfer_reason` |
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
| Bảng khóa người / mốc nhắc / view đã đính chính | `resident_position` (`resident_id`, `without_bed_since`, `version`), `allocation_reminder` (`target_type`, `target_id`, `flag_type`, `started_at`, `last_reminded_at`, `reminder_count`), `v_current_allocation` |
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
| Chỉ chuyển theo chỉ định / cần yêu cầu đổi lưu trú trong mô tả HTTP Phase 5 | `INFECTION_CONTROL_TRANSFER_REQUIRED`, `RESIDENCY_CHANGE_REQUEST_REQUIRED` (chưa có implementation) |
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

## 22. Feature 016 (`016-reports-dashboard`, A)

*Chủ feature điền khi dịch `contracts/api-016.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

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
