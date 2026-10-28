# Đề nghị duyệt hợp đồng HTTP Phase 3 — 2026-10-27

**Đã duyệt ngày 2026-10-27:** chủ dự án trả lời "1", duyệt bản dịch 11 operation và hai schema của 001. Đã áp dụng vào api-005.yaml trong lượt tiếp theo, có test hợp đồng riêng. Phạm vi T016/T016a và client web T017. Không coi YAML/test hợp đồng là endpoint đã được cài; chưa sinh client hoặc triển khai story khi nền còn thiếu.

## Operation

Giữ đúng 11 operation hiện có, mã HTTP, tính bắt buộc, kiểu dữ liệu và header. Prefix `/api/v1`. Không thêm operation hoặc trường version vào body; ETag giữ ở các phản hồi đã có trong hợp đồng.

| Hợp đồng hiện tại | Đề nghị tiếng Anh |
| --- | --- |
| GET, POST /nguoi-cao-tuoi/{nctId}/ke-hoach-cham-soc | GET, POST /residents/{residentId}/care-plans |
| GET /ke-hoach-cham-soc/{id} | GET /care-plans/{id} |
| PUT /ke-hoach-cham-soc/{id}/muc | PUT /care-plans/{id}/items |
| PUT /ke-hoach-cham-soc/{id}/thoa-thuan | PUT /care-plans/{id}/risk-agreements |
| POST /ke-hoach-cham-soc/{id}/gui-duyet | POST /care-plans/{id}/submit |
| POST /ke-hoach-cham-soc/{id}/duyet | POST /care-plans/{id}/approve |
| POST /ke-hoach-cham-soc/{id}/tra-lai | POST /care-plans/{id}/return |
| POST /ke-hoach-cham-soc/{id}/huy | POST /care-plans/{id}/cancel |
| POST /ke-hoach-cham-soc/{id}/thu-hoi | POST /care-plans/{id}/withdraw-approval |
| GET /ke-hoach-cham-soc/so-sanh?tu=&den= | GET /care-plans/compare?from=&to= |

OperationId đề nghị theo thứ tự trên: `listCarePlans`, `createCarePlan`, `getCarePlan`, `replaceCarePlanItems`, `replaceCarePlanRiskAgreements`, `submitCarePlan`, `approveCarePlan`, `returnCarePlan`, `cancelCarePlan`, `withdrawCarePlanApproval`, `compareCarePlans`.

## Schema và trường

Đây là tên HTTP; DTO nội bộ đã công bố không bị đổi chữ ký chỉ để khớp tên phản hồi HTTP.

| Tên hiện tại | Tên tiếng Anh đề nghị |
| --- | --- |
| PhienBanKeHoach, MucKeHoach, ThoaThuanRuiRo, SoSanhPhienBan | CarePlan, CarePlanItem, RiskAgreement, CarePlanComparison |
| saoChep | copyCurrent |
| nctId, soPhienBan, trangThai | residentId, revision, status |
| ngayHieuLuc, hieuLucDen | effectiveFrom, effectiveTo |
| nguoiLap, nguoiDuyet, duyetLuc, tuDuyet | createdBy, approvedBy, approvedAt, selfApproved |
| lyDoThayDoi, lyDo | changeReason, reason |
| muc, thoaThuan, chuaCoKeHoachHieuLuc, lenhDuocPhep | items, riskAgreements, noEffectivePlan, allowedCommands |
| mucGocId, loaiCongViecId, noiDung | sourceItemId, taskTypeId, content |
| kieuTanSuat, nGio, soLan, mocBatDau, cacMoc, ngayTrongTuan | frequencyType, intervalHours, timesPerDay, startsAt, times, weekdays |
| khungTruocPhut, khungSauPhut, vaiTroThucHien | windowBeforeMinutes, windowAfterMinutes, performerRole |
| loaiKetQua, mucQuanTrong, coTinhPhi, mucTieuMl | resultType, importance, billable, waterTargetMl |
| thoaThuanGocId, luaChon, ruiRoDaTraoDoi, nctThamGia | sourceAgreementId, choice, discussedRisks, residentParticipates |
| nguoiThamGia, loai, id | participants, type, id |
| bacSiNhanDinhId, nhanDinh, nguoiQuyetDinh | assessingDoctorId, selfDeterminationAssessment, decisionMaker |
| nguoiThanId, bacSiId | familyMemberId, doctorId |
| yKien, nguon, nguoiGhi, thoiDiem, ngayXemLai | opinions, source, recordedBy, at, reviewDate |
| them, bot, doi, truoc, sau, truongDoi | added, removed, changed, before, after, changedFields |

`id` giữ nguyên. Chuỗi người lập/duyệt/ghi vẫn là chuỗi theo YAML hiện tại, không đổi sang ID. `weekdays` HTTP vẫn là mảng số 1–7; bitmask của persistence/API nội bộ không rò ra HTTP.

## Enum và lỗi

| Giá trị hiện tại | Giá trị tiếng Anh đề nghị |
| --- | --- |
| NHAP, CHO_DUYET, CHO_HIEU_LUC, HIEU_LUC, HET_HIEU_LUC, DA_HUY | DRAFT, PENDING_APPROVAL, PENDING_EFFECT, EFFECTIVE, EXPIRED, CANCELLED |
| SO_LAN_NGAY, MOI_N_GIO, NGAY_TRONG_TUAN, MOT_LAN_MOI_CA | TIMES_PER_DAY, EVERY_N_HOURS, DAYS_OF_WEEK, ONCE_PER_SHIFT |
| THUONG, QUAN_TRONG, BAT_BUOC | NORMAL, IMPORTANT, MANDATORY |
| NGUOI_THAN, NHAN_VIEN (participants) | FAMILY_MEMBER, STAFF |
| NGUOI_CAO_TUOI, DAI_DIEN_NGUOI_BENH, BAC_SI (decisionMaker.type) | RESIDENT, PATIENT_REPRESENTATIVE, DOCTOR |
| DA_CO_PHIEN_BAN_MO, PHIEN_BAN_DA_KHOA, NGAY_HIEU_LUC_KHONG_HOP_LE | OPEN_REVISION_EXISTS, REVISION_LOCKED, INVALID_EFFECTIVE_DATE |
| maLoi, dieuKienChuaThoa, ma, moTa, canCu (lỗi) | errorCode, unmetConditions, code, description, basis |

Vai trò và loại kết quả dùng tên tiếng Anh đã công bố trong glossary/common.Role và m04.api.dto.ResultType. Mã BR/FR/Q/CFG không dịch. Lệnh được phép dùng tên tương ứng các hành động tiếng Anh nêu trên.

## Hai schema đang thiếu đích tham chiếu

YAML 005 tham chiếu `NhanDinhTuQuyet` và `NguonYKien` nhưng chưa khai báo chúng. Đề nghị đưa đúng schema công khai của 001 vào 005, như mô tả tại api-001.yaml yêu cầu bên dùng chép đúng schema; không tự thêm trường:

- `SelfDeterminationAssessment`: bắt buộc `canSelfDetermine`, `supportMethod`, `reviewDate`; `at` date-time readOnly; `supportMethod` minLength 1; `reviewDate` date.
- `OpinionSource`: bắt buộc `type` = `RESIDENT | FAMILY_MEMBER | OBSERVING_STAFF`; `familyMemberId` integer int64 hoặc null, bắt buộc theo điều kiện FAMILY_MEMBER đã có trong mô tả 001.

Không sửa schema/API của 001. Giới hạn 2000 ký tự của các nội dung thỏa thuận và 1000 ký tự của lý do bác sĩ quyết định giữ nguyên như 005.

## Điều kiện triển khai

Chủ dự án đã chọn phương án 2 cho quyền danh mục Phase 2: chờ A công bố khóa chính thức. T007/T007a còn mở; tasks.md yêu cầu hoàn tất nền trước user story. Duyệt bản dịch này chỉ giải quyết hợp đồng Phase 3, không đồng nghĩa duyệt AC-01 trực tiếp, bỏ gate Phase 2 hoặc đánh xong task. Chưa thêm thư viện, thay đổi nghiệp vụ hoặc cài endpoint.
