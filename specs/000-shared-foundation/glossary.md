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
| SPI `QuanHeNguoiThanApi` (`hieuLuc`, `soDienThoaiDaDangKy`, `laNguoiDaiDienHieuLuc`, `thuocNguoiCaoTuoi`, `loaiQuanHe`), `QuanHeView` | `FamilyRelationshipApi` (`active`, `registeredPhoneNumber`, `isActiveRepresentative`, `belongsToResident`, `relationship`), `FamilyRelationshipView` (`relationshipId`, `residentId`) |
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
| các bảng còn lại của 001 (hồ sơ sức khỏe, mục sức khỏe, đánh giá…) | bổ sung vào mục này cùng migration (R-03) |

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
| Chỉ chuyển theo chỉ định / cần yêu cầu đổi lưu trú trong mô tả HTTP Phase 5 | `INFECTION_CONTROL_TRANSFER_REQUIRED`, `RESIDENCY_CHANGE_REQUEST_REQUIRED` (chưa có implementation) |
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

### 9.20. Bảng sau gộp R-15 *(bổ sung 2026-10-30; cột chi tiết ở data-model 003 mục 0.1)*

| Tên logic | Mã |
| --- | --- |
| mức chăm sóc cho phép của `phong` | `room.allowed_care_levels` |
| loại bảng chung của 003 | `M03.AREA`, `M03.FLOOR`, `M03.ROOM_TYPE`, `M03.BED_TRANSFER_REASON`, `M03.DAY_CARE_REST_AREA`, `M03.DAY_CARE_SESSION` (danh mục); thuộc tính `CACH_LY` của `PHONG` (lịch sử chung); `M03.BED_BACKFILL_REQUEST` (hồ sơ chung); `M03` (trạng thái theo người); `M03.ALLOCATION_REMINDER.<flagType>` (mốc đã nhắc); `M03.PHAN_BO`, `M03.SUC_CHUA_BAN_TRU`, `M03.KHU_NGHI` (khóa) |


## 10. Feature 004 (`004-admission-residency`, B)

*Chủ feature điền khi dịch `contracts/api-004.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

## 11. Feature 005 (`005-daily-care-tasks`, C)

*Chủ feature điền khi dịch `contracts/api-005.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*

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


## 13. Feature 007 (`007-health-alert-incident`, D)

*Chủ feature điền khi dịch `contracts/api-007.yaml` (đường dẫn, schema, enum, mã lỗi; bảng, lớp cùng thay đổi với mã). Chỉ sửa mục này.*


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
| loại bảng chung của 009 | `M13.NOTIFICATION_CHANNEL` (danh mục); `M13.NOTIFICATION_EVENT`, `M13.DELIVERY_ATTEMPT`, `M13.DELIVERY_RESULT`, `M13.CALL_REMINDER_NOTIFICATION`, `M13.CALL_REMINDER_HANDOFF`, `M13.CALL_RESULT`, `M13.DIRECT_CONTACT_RESULT` (sự kiện); `M13.DIRECT_CONTACT`, `M13.SMS_BATCH_WINDOW` (hồ sơ chung); `M13.REQUEST_DUPLICATE`, `M13.JOB_DONE` (mốc đã nhắc) |


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
| *(bổ sung 2026-10-27)* `GIUONG.CO_THE_DUNG`, `GIUONG.PHONG_CACH_LY`, `GIUONG.SO_NGUOI`, `GIUONG.TRONG_CHO_CHUYEN`, `GIUONG.CHO_SAN_SANG_DAI_NHAT`, `GIUONG.CHO_SAN_SANG_VUOT_HAN` | `BED.USABLE`, `BED.ISOLATED_ROOMS`, `BED.OCCUPANTS`, `BED.EMPTY_AWAITING_TRANSFER`, `BED.AWAITING_READY_MAX`, `BED.AWAITING_READY_OVERDUE` |
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
