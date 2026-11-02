# Truy vết FR → task → test, feature 000 (T104, constitution XV)

Nguồn: specs/000-shared-foundation/spec.md (58 FR), tasks.md, backend/src/test/java/com/eldercare/backend/**, frontend/src/**/*.test.js(x). Kiểm ngày 2026-10-19, chỉ đọc.
Quy ước: "(phạm vi)" = task ghi dải, ví dụ "FR-023 → FR-030"; "(mục tiêu phase)" = chỉ có ở dòng Goal hoặc tiêu đề phase, không ở dòng task. Đường dẫn test bỏ tiền tố `com/eldercare/backend/`. "(gián tiếp)" = test không ghi mã FR nhưng kiểm hành vi đó.

| FR | Tiêu đề ngắn | Task | Test | Trạng thái |
|---|---|---|---|---|
| FR-001 | Ba nhóm dữ liệu | T022, T040; mục tiêu phase US1 | administration/DataGroupArchTest#FR_001_everyEntity_declaresDataGroup | COVERED |
| FR-002 | Danh mục: chỉ xóa khi chưa tham chiếu | T041, T046 | administration/CatalogSampleIT#US1_1_deleteUnusedItem_returns204_andIsLogged, #US1_2_deleteReferencedItem_returns409_evenIfReferencedByCancelledCase | COVERED |
| FR-003 | Ngừng hiệu lực, kích hoạt lại | T041, T046 | administration/CatalogSampleIT#US1_2_deactivatedItem_notSelectableForNewCase_butStillShownInOldCase, #US1_8_reactivate_whenNoActiveDuplicate_succeeds_otherwise409 | COVERED |
| FR-004 | Thuộc tính danh mục theo phiên bản | T047; mục tiêu phase US1 | administration/VersionedAttributeIT (3 FR_004_*: phiên bản mới đóng phiên bản đang mở, ngày hiệu lực không sau hôm nay bị chặn, tài khoản ứng dụng chỉ sửa được `effective_to`); thêm 2026-10-19 theo chủ dự án | COVERED |
| FR-005 | Nhóm 2 không sửa trực tiếp | T022, T040, T044 | administration/DataGroupArchTest; administration/BusinessCommandReasonIT#FR_005_businessAndRecordResources_haveNoPutPatchDelete | COVERED |
| FR-006 | Từ chối lệnh, nêu điều kiện | T015 | common/error/web/ProblemDetailsHandlerIT#FR_006_unmetConditions_listBrCodeAndDescription | COVERED |
| FR-007 | Lệnh nhóm 2 bắt buộc lý do | T022, T040, T044 | administration/DataGroupArchTest#FR_007_businessCommandRequiringReason_takesReason; administration/BusinessCommandReasonIT#SC_008_commandWithoutReason_isRejectedBeforeTouchingData | COVERED |
| FR-008 | Phiên bản không chồng khoảng | T011, T042, T047 | administration/EffectiveVersionNoOverlapIT#FR_008_overlappingVersion_isRejected_adjacentIsAccepted, #FR_008_concurrentOverlappingVersions_onlyOneSucceeds; common/time/EffectiveDateTest#BR_M15_07_isWithin_includesBothEnds (gián tiếp) | COVERED |
| FR-009 | Nhóm 3 không sửa, không xóa | T023, T043 | common/data/AppendOnlyRepositoryIT (3 phương thức FR_009_*); administration/RecordGroupTriggerIT | COVERED |
| FR-010 | Danh sách bản ghi bảo vệ | T043 | administration/RecordGroupTriggerIT#FR_010_everyRecordGroupTable_hasBothTriggers | COVERED |
| FR-010a | Hệ thống không tự sinh bản ghi | T104 (task rà truy vết, chỉ nêu tên) | common/scheduling/SchedulerScopeArchTest (2 FR_010a_*: việc theo lịch chỉ nằm trong `ScheduledJob`, mỗi job có trong danh sách loại việc được phép); thêm 2026-10-19 | COVERED |
| FR-011 | Tập tham số Phụ lục 25 | T080; T029–T031 (gián tiếp, ghi DBR-24) | administration/parameter/ParameterIT#FR_011_allCodesLoaded_eachWithInitialHistoryRow, #FR_011_appAccount_cannotAddOrRemoveCodes_norRewriteHistory; administration/parameter/api/dto/ParameterCodeTest#FR_011_enum_matchesAppendix25; administration/parameter/ParameterPermissionIT#FR_011_unknownCode_isNotFound | COVERED |
| FR-012 | Một giá trị hiện hành, có lịch sử | Không task nào ghi số; mục tiêu phase US4 (FR-011 → FR-022); T029, T075 ghi DBR-24 | administration/parameter/ParameterIT#FR_011_allCodesLoaded_eachWithInitialHistoryRow, administration/parameter/ParameterChangeIT#US4_2_change_updatesCurrentValue_addsHistory_andAuditBeforeAfter (gián tiếp) | COVERED |
| FR-013 | Chỉ Quản lý viện đổi tham số | Không task nào ghi số; T076 (BR-M15-04), T079 (khóa quyền THAM_SO_DOI) | administration/parameter/ParameterPermissionIT#US4_6_nonManager_cannotChange_norBrowseParameters (javadoc ghi FR-013) | COVERED |
| FR-014 | Đổi tham số: lý do, nhật ký | T079 (phạm vi) | administration/parameter/ParameterChangeIT#FR_014_missingReason_isRejectedBeforeAnyChange, #US4_2_… | COVERED |
| FR-015 | Kiểm kiểu, khoảng hợp lệ | T074, T079 | administration/parameter/ParameterValidatorTest (4 FR_015_*); ParameterChangeIT#US4_5_…, #FR_015_K3_…; ParameterIT#FR_015_crossConstraint_isLoaded; ParameterCacheAfterCommitIT#FR_015_rejectedChange_leavesCachedValue | COVERED |
| FR-016 | Hiệu lực ngay, không triển khai lại | T077, T079 | administration/parameter/ParameterCacheAfterCommitIT#SC_004_ruleReadsNewValueRightAfterCommit_withoutRestart | COVERED |
| FR-017 | Không tính lại kết quả cũ | T079 (phạm vi FR-014 → FR-018) | administration/approval/ParameterChangeNoRecalculationIT#FR_017_changingReminderMark_keepsExistingReminder_noRecalculation; thêm 2026-10-19 | COVERED |
| FR-018 | Mỗi lần kiểm dùng một giá trị | T032, T079 | administration/parameter/ParameterIT#FR_018_read_returnsCurrentValueByKind; administration/parameter/api/dto/ParameterValueTest | COVERED |
| FR-019 | Tra giá trị tại thời điểm quá khứ | T032, T078 | administration/parameter/ParameterAsOfIT#US4_8_valueAtPastInstant_isTheValueCurrentThen; ParameterIT#FR_019_readAt_usesHistory | COVERED |
| FR-020 | Khôi phục mặc định là thay đổi | T075, T079 | administration/parameter/ParameterChangeIT#US4_7_restoreDefault_isAnOrdinaryChange_markedAsRestore | COVERED |
| FR-021 | Chống ghi đè giá trị cũ (412) | T015, T018, T075, T079 | common/web/VersionEtagTest (4 FR_021_*); common/error/web/ProblemDetailsHandlerIT#FR_021_versionMismatch_returnsCurrentValue; ParameterChangeIT#US4_9_staleIfMatch_… | COVERED |
| FR-022 | Tham số dạng bảng đổi cả | T078 | administration/parameter/ParameterTableIT#US4_10_changingOneRow_logsWholeTableBeforeAndAfter | COVERED |
| FR-023 | Đính chính trỏ một bản gốc | T057 (phạm vi); mục tiêu phase US2 | administration/correction/CorrectionIT (javadoc FR-023 → FR-030) #US2_1_…, #US2_8_missingOriginal_or_unsupportedType_isRejected | COVERED |
| FR-024 | Đính chính: lý do, người, thời điểm | T050, T057 | administration/correction/CorrectionIT#US2_2_missingReason_isRejected, #FR_024_invalidContent_isRejected; frontend CorrectionSheet.test.jsx FR_024_lyDoTrongHoacChiKhoangTrang_… | COVERED |
| FR-025 | Không đổi bản gốc | T057 (phạm vi) | administration/correction/CorrectionIT#US2_1_recorderCorrects_originalUnchanged_andLogged | COVERED |
| FR-026 | Ba cách tạo đính chính A/B/C | T049, T056, T057, T058 | administration/correction/CorrectionMethodSelectorTest (5 FR_026_*); administration/correction/CorrectionApprovalIT (cách C); CorrectionIT#US2_3_…, #US2_4_… | COVERED |
| FR-027 | Nhiều đính chính, bản mới nhất | T050, T051 | administration/correction/CorrectionConcurrencyIT#FR_027_concurrentCorrections_getConsecutiveSequenceNumbers; CorrectionIT#US2_5_6_secondCorrection_pointsToOriginal_andBecomesCurrent | COVERED |
| FR-028 | Xem bản gốc thấy đính chính | T051, T106 | administration/correction/CorrectionConcurrencyIT (javadoc); CorrectionIT#T059_httpCreateThenReadChain; frontend CorrectionSheet.test.jsx FR_028_* (2 test) | COVERED |
| FR-029 | Hủy ghi nhận | T050, T052 | administration/correction/CorrectionViewIT#FR_029_voidedRecordView_hasVoidTime; CorrectionIT#US2_7_void_thenAmend_isRejected; CorrectionApprovalIT#FR_029_originalVoidedBeforeApproval_applyFails; frontend CorrectionSheet.test.jsx FR_029_* (3 test) | COVERED |
| FR-030 | Báo cáo dùng giá trị hiện hành | T052 | administration/correction/CorrectionViewIT#FR_030_latestCorrectionView_returnsHighestSequenceAmend | COVERED |
| FR-031 | Vòng đời yêu cầu phê duyệt chung | T083, T092 (phạm vi); T091 | administration/approval/ApprovalLifecycleIT (javadoc FR-031 → FR-036); administration/approval/RequestTransitionTableTest#FR_031_terminalStatuses_areFinal, #extendedTransition_onlyAsDeclaredByRequestType | COVERED |
| FR-031a | Nhắc, báo Quản lý viện khi quá hạn | T087, T094, T105 | administration/approval/PendingReminderIT (3 phương thức); administration/approval/ApproverRoleRestrictionIT. Gửi thông báo thật thuộc 009 (test kiểm sự kiện) | COVERED |
| FR-032 | Trường của yêu cầu | T083, T092 (phạm vi); T089 (cột bảng) | administration/approval/ApprovalLifecycleIT#US5_1_create_isDraft_withRequesterTypeContentDateReason | COVERED |
| FR-033 | Chỉ sửa nội dung ở Nháp | T083, T092 (phạm vi) | administration/approval/ApprovalLifecycleIT#FR_033_draftEditable_withReason_thenLockedAfterSubmit; RequestTransitionTableTest | COVERED |
| FR-034 | Quyền duyệt và phạm vi | T105; T083, T092 (phạm vi) | administration/approval/ApprovalLifecycleIT#US5_14_withoutPermissionOrOutOfScope_cannotCreateOrDecide; ApproverRoleRestrictionIT#Q_338_… (quyền lấy từ bản giả 002) | COVERED |
| FR-034a | Quyền xét lúc quyết định | T105, T109 | administration/approval/ApproverRoleRestrictionIT#FR_034a_restrictionIsEvaluatedAtEachDecision_andOffMeansAsBefore; AccountChangeAfterDecisionIT (quyết định giữ hiệu lực khi người duyệt mất quyền, bị khóa; người yêu cầu bị khóa, mất quyền tạo: Chờ duyệt vẫn duyệt, từ chối được, Đã duyệt vẫn áp dụng, không tự đổi trạng thái; khóa giả lập bằng bản giả quyền).; FR_034a_draftOfLockedRequester_noOneCanSubmit_staysDraft (Nháp của người bị khóa giữ nguyên, không ai gửi duyệt được) | COVERED |
| FR-035 | Tự duyệt có lý do, đánh dấu | T088, T105 | administration/approval/SelfApprovalIT (gồm FR_035_typeDisallowingSelfApproval_isBlocked); ApproverRoleRestrictionIT | COVERED |
| FR-036 | Từ chối có lý do, không tác động | T083 (phạm vi) | administration/approval/ApprovalLifecycleIT#US5_5_reject_requiresComment_andCreatesNoEffect; CorrectionApprovalIT#FR_036_rejected_noCorrection | COVERED |
| FR-037 | Áp dụng đúng một lần | T084, T093 | administration/approval/ApplyOnceIT (3 phương thức); common/web/IdempotencyFilterIT | COVERED |
| FR-037a | Áp dụng bù | T085, T093 | administration/approval/CatchUpApplyIT#US5_9_missedDays_applyInOrder_markedCatchUp | COVERED |
| FR-037b | Toàn bộ hoặc không | T028, T086, T093 | administration/approval/ApplyFailureIT#US5_10_…; administration/auditlog/AuditLogAtomicityIT#FR_037b_writeOutsideTransaction_survivesRollbackOfCaller | COVERED |
| FR-038 | Áp dụng không thành | T086 | administration/approval/ApplyFailureIT#US5_8_conditionNoLongerMet_endsApplyFailed_withoutEffect (thông báo thật thuộc 009; test kiểm sự kiện) | COVERED |
| FR-039 | Kết quả trỏ về yêu cầu | T092 (phạm vi); T089 (kết quả áp dụng: cột của `yeu_cau_phe_duyet` sau R-15, T108) | administration/approval/ApplyOnceIT#US5_7_11_jobRunThreeTimes_appliesExactlyOnce_withResultReference | COVERED |
| FR-040 | Quyết định đồng thời | T015, T018, T088 | administration/approval/ConcurrentDecisionIT (2 FR_040_*); common/web/VersionEtagTest; ProblemDetailsHandlerIT; RequestTransitionTableTest | COVERED |
| FR-041 | Chuyển trạng thái ghi nhật ký | T092 (phạm vi) | administration/approval/ApprovalLifecycleIT (gián tiếp: auditActions khớp REQUEST_CREATED, REQUEST_DRAFT_EDITED, REQUEST_SUBMITTED); ApplyOnceIT, ConcurrentDecisionIT kiểm REQUEST_APPLIED, REQUEST_APPROVED | COVERED |
| FR-042 | Mọi thay đổi nhóm 2, 3 có nhật ký | Tiêu đề "Nhật ký – phần ghi" (FR-042 → FR-047), mục tiêu phase US3; T061 (ghi SC-001, DBR-23) | administration/auditlog/AuditLogCompletenessIT (3 phương thức SC_001_*) | COVERED |
| FR-043 | Nhật ký danh mục, tham số, phân quyền | T041, T046 | administration/CatalogSampleIT; administration/auditlog/AuditLogCompletenessIT#SC_001_catalogChange_hasOneAuditInsert; ParameterChangeIT (tham số). Phần "đổi phân quyền" thuộc 002 | COVERED |
| FR-043a | Nhật ký hành động không đổi dữ liệu | T065 | administration/auditlog/AuditLogReportExportIT#US3_11_…; administration/auditlog/api/dto/AuditLogEntryTest#FR_043a_… (đăng nhập thuộc 002, xuất thật thuộc 016) | COVERED |
| FR-044 | Trường của bản ghi nhật ký | T017, T027 | administration/auditlog/api/dto/AuditLogEntryTest (FR_044_* 2 test); common/ReasonTest; AuditLogReportExportIT#FR_044_exportWithoutPurpose_isRejected_andNothingLogged | COVERED |
| FR-045 | Bộ lập lịch là người thực hiện, có căn cứ | T027, T035 | administration/auditlog/AuditLogSchedulerIT (2 FR_045_*); common/scheduling/SchedulingIT#FR_045_run_isRecordedWithId; AuditLogAtomicityIT#FR_045_basis_isStoredAsJson | COVERED |
| FR-046 | Múi giờ, giờ máy chủ, ngoại tuyến | T011, T021, T028 | administration/auditlog/AuditLogAtomicityIT#FR_046_* (2 test); administration/auditlog/AuditLogOfflineIT (2 test, T064); common/time/EffectiveDateTest | COVERED |
| FR-047 | Thay đổi và nhật ký cùng lưu | T028, T039 | administration/auditlog/AuditLogAtomicityIT#FR_047_* (3 test); AuditLogCompletenessIT | COVERED |
| FR-048 | Nhật ký chỉ ghi thêm | Không task nào ghi số; T026, T024 (bảng, quyền); mục tiêu phase US3 (FR-042 → FR-051a) | administration/auditlog/AuditLogTableIT#FR_048_appAccount_cannotUpdateOrDelete, #FR_048_triggers_blockEvenAdmin | COVERED |
| FR-049 | Tra cứu, phạm vi, che trường | T062, T063, T068, T073 | administration/auditlog/AuditLogFieldMaskingIT; administration/auditlog/AuditLogScopeIT; AuditLogReportExportIT#FR_049_viewingAuditLog_isNotLogged; frontend features/audit-log/AuditLogTable.test.jsx (2 FR_049_*). Quyền theo trường lấy từ 002 | COVERED |
| FR-050 | Thời hạn lưu giữ | T066, T072 | administration/retention/ExpiredDataPurgeIT#FR_050_logOfResidentRecord_isKept_untilResidentRetentionIsKnown | COVERED |
| FR-051 | Loại bỏ chỉ do Bộ lập lịch | T028, T066 | administration/retention/ExpiredDataPurgeIT#FR_051_appAccountCannotDelete_retentionCannotDeleteSummary; administration/auditlog/AuditLogTableIT#FR_051_retentionAccount_deletesOnlyNonExemptRows | COVERED |
| FR-051a | Giữ chuỗi còn tham chiếu | T066, T072 | administration/retention/ExpiredDataPurgeIT (3 FR_051a_*) | COVERED |

## Tổng

- COVERED: 58 (FR-034a đủ test từ 2026-11-01, T109 và lớp `AccountChangeAfterDecisionIT`; FR-004, FR-010a, FR-017 thêm test 2026-10-19 theo chủ dự án)
- TASK_ONLY: 0
- DELEGATED: 0 (không FR nào giao trọn cho feature khác; phần giao một phần ghi trong cột Test: FR-031a, FR-038 cho 009; FR-034, FR-034a, FR-043, FR-049 cho 002; FR-043a cho 002, 016)
- GAP: 0

Ghi chú: FR-012, FR-013, FR-048 có test nhưng không dòng task nào ghi số FR (chỉ ghi DBR-24, BR-M15-04 hoặc dải ở mục tiêu phase). Có thể bổ sung số vào task khi làm T104.
