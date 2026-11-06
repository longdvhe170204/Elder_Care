# Phụ lục: file thay đổi của tám worktree E

Ngày 2026-10-31. Nguồn: git status --short --untracked-files=all của từng worktree. Các dòng này là thay đổi chưa commit so với HEAD riêng của từng nhánh; có file trùng tên giữa các bản. D biểu thị đường dẫn cũ bị xóa, thường do chuyển vị trí; M là sửa file đã theo dõi; ?? là file chưa được Git theo dõi. Danh sách gồm cả hai file báo cáo mới này.

[Đọc giải thích công việc](work-summary-for-e.md). File ở nhánh cũ được liên kết theo thư mục trên máy, cần giữ worktree đó để mở.

| Nhánh | Nền HEAD | Số đường dẫn thay đổi |
| --- | --- | --- |
| impl/008-fr057-handover-notifications | 9150bbfb | 12 |
| docs/008-api-review | 685dea63 | 5 |
| impl/008-governance-fixture | 685dea63 | 5 |
| impl/008-my-shifts-mobile | 685dea63 | 16 |
| impl/008-mobile-assignments | 685dea63 | 44 |
| impl/008-shift-query-fixture | 685dea63 | 6 |
| impl/010-incurred-costs | 73cc9d61 | 67 |
| impl/008-q262 | d2bd7de0 | 336 |

## impl/008-fr057-handover-notifications

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care

| Git | File |
| --- | --- |
|  M | [backend/src/main/java/com/cambae/backend/m09/handover/internal/service/HandoverCorrection.java](../../../../backend/src/main/java/com/cambae/backend/m09/handover/internal/service/HandoverCorrection.java) |
|  M | [backend/src/main/java/com/cambae/backend/m09/handover/internal/service/HandoverNotifications.java](../../../../backend/src/main/java/com/cambae/backend/m09/handover/internal/service/HandoverNotifications.java) |
|  M | [backend/src/test/java/com/cambae/backend/m09/HandoverIT.java](../../../../backend/src/test/java/com/cambae/backend/m09/HandoverIT.java) |
|  M | [docs/progress.md](../../../../docs/progress.md) |
|  M | [docs/team-plan.md](../../../../docs/team-plan.md) |
|  M | [specs/000-shared-foundation/glossary.md](../../../../specs/000-shared-foundation/glossary.md) |
|  M | [specs/008-staff-shift-handover/contracts/module-interfaces.md](../../../../specs/008-staff-shift-handover/contracts/module-interfaces.md) |
|  M | [specs/008-staff-shift-handover/plan.md](../../../../specs/008-staff-shift-handover/plan.md) |
|  M | [specs/008-staff-shift-handover/tasks.md](../../../../specs/008-staff-shift-handover/tasks.md) |
|  M | [specs/008-staff-shift-handover/validation.md](../../../../specs/008-staff-shift-handover/validation.md) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/HandoverCorrectionNotificationsTest.java](../../../../backend/src/test/java/com/cambae/backend/m09/HandoverCorrectionNotificationsTest.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/HandoverNotificationsTest.java](../../../../backend/src/test/java/com/cambae/backend/m09/HandoverNotificationsTest.java) |

## docs/008-api-review

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/008-api-review

| Git | File |
| --- | --- |
|  M | [docs/progress.md](../../../008-api-review/docs/progress.md) |
|  M | [docs/team-plan.md](../../../008-api-review/docs/team-plan.md) |
|  M | [specs/008-staff-shift-handover/plan.md](../../../008-api-review/specs/008-staff-shift-handover/plan.md) |
|  M | [specs/008-staff-shift-handover/tasks.md](../../../008-api-review/specs/008-staff-shift-handover/tasks.md) |
| ?? | [specs/008-staff-shift-handover/api-review-2026-10-28.md](../../../008-api-review/specs/008-staff-shift-handover/api-review-2026-10-28.md) |

## impl/008-governance-fixture

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/008-governance-fixture

| Git | File |
| --- | --- |
|  M | [backend/src/test/java/com/cambae/backend/m09/M09GovernanceIT.java](../../../008-governance-fixture/backend/src/test/java/com/cambae/backend/m09/M09GovernanceIT.java) |
|  M | [docs/progress.md](../../../008-governance-fixture/docs/progress.md) |
|  M | [specs/008-staff-shift-handover/plan.md](../../../008-governance-fixture/specs/008-staff-shift-handover/plan.md) |
|  M | [specs/008-staff-shift-handover/tasks.md](../../../008-governance-fixture/specs/008-staff-shift-handover/tasks.md) |
|  M | [specs/008-staff-shift-handover/validation.md](../../../008-governance-fixture/specs/008-staff-shift-handover/validation.md) |

## impl/008-my-shifts-mobile

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/008-my-shifts-mobile

| Git | File |
| --- | --- |
|  M | [docs/progress.md](../../../008-my-shifts-mobile/docs/progress.md) |
|  M | [docs/team-plan.md](../../../008-my-shifts-mobile/docs/team-plan.md) |
|  M | [mobile/lib/common/api/api_client.dart](../../../008-my-shifts-mobile/mobile/lib/common/api/api_client.dart) |
|  M | [mobile/lib/staff/staff_root.dart](../../../008-my-shifts-mobile/mobile/lib/staff/staff_root.dart) |
|  M | [specs/000-shared-foundation/glossary.md](../../../008-my-shifts-mobile/specs/000-shared-foundation/glossary.md) |
|  M | [specs/008-staff-shift-handover/plan.md](../../../008-my-shifts-mobile/specs/008-staff-shift-handover/plan.md) |
|  M | [specs/008-staff-shift-handover/tasks.md](../../../008-my-shifts-mobile/specs/008-staff-shift-handover/tasks.md) |
|  M | [specs/008-staff-shift-handover/validation.md](../../../008-my-shifts-mobile/specs/008-staff-shift-handover/validation.md) |
| ?? | [mobile/lib/staff/shifts/my_shifts_controller.dart](../../../008-my-shifts-mobile/mobile/lib/staff/shifts/my_shifts_controller.dart) |
| ?? | [mobile/lib/staff/shifts/my_shifts_screen.dart](../../../008-my-shifts-mobile/mobile/lib/staff/shifts/my_shifts_screen.dart) |
| ?? | [mobile/lib/staff/shifts/shift_labels.dart](../../../008-my-shifts-mobile/mobile/lib/staff/shifts/shift_labels.dart) |
| ?? | [mobile/test/staff/shifts/api_list_test.dart](../../../008-my-shifts-mobile/mobile/test/staff/shifts/api_list_test.dart) |
| ?? | [mobile/test/staff/shifts/my_shifts_controller_test.dart](../../../008-my-shifts-mobile/mobile/test/staff/shifts/my_shifts_controller_test.dart) |
| ?? | [mobile/test/staff/shifts/my_shifts_navigation_test.dart](../../../008-my-shifts-mobile/mobile/test/staff/shifts/my_shifts_navigation_test.dart) |
| ?? | [mobile/test/staff/shifts/my_shifts_screen_test.dart](../../../008-my-shifts-mobile/mobile/test/staff/shifts/my_shifts_screen_test.dart) |
| ?? | [specs/008-staff-shift-handover/mobile-my-shifts-proposal.md](../../../008-my-shifts-mobile/specs/008-staff-shift-handover/mobile-my-shifts-proposal.md) |

## impl/008-mobile-assignments

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/008-mobile-assignments

| Git | File |
| --- | --- |
|  M | [backend/src/main/java/com/cambae/backend/m09/assignment/internal/persistence/AssignmentStore.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/internal/persistence/AssignmentStore.java) |
|  M | [backend/src/main/java/com/cambae/backend/m09/assignment/internal/service/AssignmentService.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/internal/service/AssignmentService.java) |
|  M | [backend/src/main/java/com/cambae/backend/m09/assignment/web/AssignmentController.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/web/AssignmentController.java) |
|  M | [backend/src/main/java/com/cambae/backend/m09/assignment/web/response/AssignmentResponses.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/web/response/AssignmentResponses.java) |
|  M | [docs/progress.md](../../../008-mobile-assignments/docs/progress.md) |
|  M | [docs/team-plan.md](../../../008-mobile-assignments/docs/team-plan.md) |
|  M | [frontend/src/api/api008.js](../../../008-mobile-assignments/frontend/src/api/api008.js) |
|  M | [mobile/lib/common/api/api_client.dart](../../../008-mobile-assignments/mobile/lib/common/api/api_client.dart) |
|  M | [mobile/lib/staff/staff_root.dart](../../../008-mobile-assignments/mobile/lib/staff/staff_root.dart) |
|  M | [specs/000-shared-foundation/glossary.md](../../../008-mobile-assignments/specs/000-shared-foundation/glossary.md) |
|  M | [specs/008-staff-shift-handover/contracts/api-008.yaml](../../../008-mobile-assignments/specs/008-staff-shift-handover/contracts/api-008.yaml) |
|  M | [specs/008-staff-shift-handover/data-model.md](../../../008-mobile-assignments/specs/008-staff-shift-handover/data-model.md) |
|  M | [specs/008-staff-shift-handover/plan.md](../../../008-mobile-assignments/specs/008-staff-shift-handover/plan.md) |
|  M | [specs/008-staff-shift-handover/tasks.md](../../../008-mobile-assignments/specs/008-staff-shift-handover/tasks.md) |
| ?? | [backend/src/main/java/com/cambae/backend/m09/assignment/internal/service/AssignmentReadService.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/internal/service/AssignmentReadService.java) |
| ?? | [backend/src/main/java/com/cambae/backend/m09/assignment/internal/service/AssignmentReadSources.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/internal/service/AssignmentReadSources.java) |
| ?? | [backend/src/main/java/com/cambae/backend/m09/assignment/web/AssignmentReadController.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/web/AssignmentReadController.java) |
| ?? | [backend/src/main/java/com/cambae/backend/m09/assignment/web/response/AssignmentReadResponses.java](../../../008-mobile-assignments/backend/src/main/java/com/cambae/backend/m09/assignment/web/response/AssignmentReadResponses.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentDetailSerializationTest.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentDetailSerializationTest.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentDraftAtomicityIT.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentDraftAtomicityIT.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentDraftPermissionIT.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentDraftPermissionIT.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentDraftUpdateIT.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentDraftUpdateIT.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentFixtureIsolationIT.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentFixtureIsolationIT.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentHttpTestBase.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentHttpTestBase.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentReadSourcesTest.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentReadSourcesTest.java) |
| ?? | [backend/src/test/java/com/cambae/backend/m09/AssignmentResolutionHttpIT.java](../../../008-mobile-assignments/backend/src/test/java/com/cambae/backend/m09/AssignmentResolutionHttpIT.java) |
| ?? | [mobile/lib/staff/shifts/assignment_form.dart](../../../008-mobile-assignments/mobile/lib/staff/shifts/assignment_form.dart) |
| ?? | [mobile/lib/staff/shifts/assignment_labels.dart](../../../008-mobile-assignments/mobile/lib/staff/shifts/assignment_labels.dart) |
| ?? | [mobile/lib/staff/shifts/assignment_moment.dart](../../../008-mobile-assignments/mobile/lib/staff/shifts/assignment_moment.dart) |
| ?? | [mobile/lib/staff/shifts/assignments_controller.dart](../../../008-mobile-assignments/mobile/lib/staff/shifts/assignments_controller.dart) |
| ?? | [mobile/lib/staff/shifts/assignments_screen.dart](../../../008-mobile-assignments/mobile/lib/staff/shifts/assignments_screen.dart) |
| ?? | [mobile/lib/staff/shifts/floor_manager_sheet.dart](../../../008-mobile-assignments/mobile/lib/staff/shifts/floor_manager_sheet.dart) |
| ?? | [mobile/test/staff/shifts/api_delete_test.dart](../../../008-mobile-assignments/mobile/test/staff/shifts/api_delete_test.dart) |
| ?? | [mobile/test/staff/shifts/api_list_test.dart](../../../008-mobile-assignments/mobile/test/staff/shifts/api_list_test.dart) |
| ?? | [mobile/test/staff/shifts/assignment_form_test.dart](../../../008-mobile-assignments/mobile/test/staff/shifts/assignment_form_test.dart) |
| ?? | [mobile/test/staff/shifts/assignment_moment_test.dart](../../../008-mobile-assignments/mobile/test/staff/shifts/assignment_moment_test.dart) |
| ?? | [mobile/test/staff/shifts/assignments_controller_test.dart](../../../008-mobile-assignments/mobile/test/staff/shifts/assignments_controller_test.dart) |
| ?? | [mobile/test/staff/shifts/assignments_navigation_test.dart](../../../008-mobile-assignments/mobile/test/staff/shifts/assignments_navigation_test.dart) |
| ?? | [mobile/test/staff/shifts/assignments_screen_test.dart](../../../008-mobile-assignments/mobile/test/staff/shifts/assignments_screen_test.dart) |
| ?? | [specs/008-staff-shift-handover/assignment-contract-review.md](../../../008-mobile-assignments/specs/008-staff-shift-handover/assignment-contract-review.md) |
| ?? | [specs/008-staff-shift-handover/assignment-verification-2026-10-29.md](../../../008-mobile-assignments/specs/008-staff-shift-handover/assignment-verification-2026-10-29.md) |
| ?? | [specs/008-staff-shift-handover/contracts/proposal-assignment-draft-update.yaml](../../../008-mobile-assignments/specs/008-staff-shift-handover/contracts/proposal-assignment-draft-update.yaml) |
| ?? | [specs/008-staff-shift-handover/contracts/proposal-assignment-resolution.yaml](../../../008-mobile-assignments/specs/008-staff-shift-handover/contracts/proposal-assignment-resolution.yaml) |
| ?? | [specs/008-staff-shift-handover/mobile-assignments-proposal.md](../../../008-mobile-assignments/specs/008-staff-shift-handover/mobile-assignments-proposal.md) |

## impl/008-shift-query-fixture

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/008-shift-query-fixture

| Git | File |
| --- | --- |
|  M | [backend/src/test/java/com/cambae/backend/m09/ShiftQueriesIT.java](../../../008-shift-query-fixture/backend/src/test/java/com/cambae/backend/m09/ShiftQueriesIT.java) |
|  M | [docs/progress.md](../../../008-shift-query-fixture/docs/progress.md) |
|  M | [docs/team-plan.md](../../../008-shift-query-fixture/docs/team-plan.md) |
|  M | [specs/008-staff-shift-handover/plan.md](../../../008-shift-query-fixture/specs/008-staff-shift-handover/plan.md) |
|  M | [specs/008-staff-shift-handover/tasks.md](../../../008-shift-query-fixture/specs/008-staff-shift-handover/tasks.md) |
|  M | [specs/008-staff-shift-handover/validation.md](../../../008-shift-query-fixture/specs/008-staff-shift-handover/validation.md) |

## impl/010-incurred-costs

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/010-incurred-costs

| Git | File |
| --- | --- |
|  M | [docs/progress.md](../../../010-incurred-costs/docs/progress.md) |
|  M | [docs/team-plan.md](../../../010-incurred-costs/docs/team-plan.md) |
|  M | [specs/000-shared-foundation/glossary.md](../../../010-incurred-costs/specs/000-shared-foundation/glossary.md) |
|  M | [specs/010-incurred-costs/contracts/api-010.yaml](../../../010-incurred-costs/specs/010-incurred-costs/contracts/api-010.yaml) |
|  M | [specs/010-incurred-costs/contracts/module-interfaces.md](../../../010-incurred-costs/specs/010-incurred-costs/contracts/module-interfaces.md) |
|  M | [specs/010-incurred-costs/data-model.md](../../../010-incurred-costs/specs/010-incurred-costs/data-model.md) |
|  M | [specs/010-incurred-costs/plan.md](../../../010-incurred-costs/specs/010-incurred-costs/plan.md) |
|  M | [specs/010-incurred-costs/research.md](../../../010-incurred-costs/specs/010-incurred-costs/research.md) |
|  M | [specs/010-incurred-costs/tasks.md](../../../010-incurred-costs/specs/010-incurred-costs/tasks.md) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/CostStatisticsApi.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/CostStatisticsApi.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/FamilyPortalCostApi.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/FamilyPortalCostApi.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/FinalPeriodCostApi.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/FinalPeriodCostApi.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/BillingSourceView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/BillingSourceView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostItemReportView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostItemReportView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostItemView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostItemView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostPeriodView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostPeriodView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostStatementReportView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostStatementReportView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostStatementType.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostStatementType.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostStatementView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostStatementView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostType.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostType.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/CostTypeTotal.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/CostTypeTotal.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/FinalPeriodConditionView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/FinalPeriodConditionView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/ProvisionalCostSummaryView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/ProvisionalCostSummaryView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/PurchaseAssistanceStatus.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/PurchaseAssistanceStatus.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/PurchaseAssistanceView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/PurchaseAssistanceView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/PurchaseRequesterView.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/PurchaseRequesterView.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/dto/package-info.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/dto/package-info.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/CostItemChanged.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/CostItemChanged.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/CostSourceBilled.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/CostSourceBilled.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/CostSourceChanged.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/CostSourceChanged.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/CostStatementFinalized.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/CostStatementFinalized.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistanceCancelled.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistanceCancelled.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistanceDecided.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistanceDecided.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistancePurchased.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistancePurchased.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistanceSent.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/PurchaseAssistanceSent.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/events/package-info.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/events/package-info.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/api/package-info.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/api/package-info.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/DayCareSessionFee.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/DayCareSessionFee.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/DongRounding.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/DongRounding.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/ResidencyDayFee.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/internal/pricing/domain/ResidencyDayFee.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/internal/provision/domain/CostForBalancePort.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/internal/provision/domain/CostForBalancePort.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/billing/internal/purchase/domain/PurchaseBalancePort.java](../../../010-incurred-costs/backend/src/main/java/com/eldercare/backend/billing/internal/purchase/domain/PurchaseBalancePort.java) |
| ?? | [backend/src/main/resources/db/migration/V010_01__costs.sql](../../../010-incurred-costs/backend/src/main/resources/db/migration/V010_01__costs.sql) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/CostApiContractsTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/CostApiContractsTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/CostSchemaIT.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/CostSchemaIT.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/CostSharedFakesTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/CostSharedFakesTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/CostSourceFixtureTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/CostSourceFixtureTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/CostViewSchemaTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/CostViewSchemaTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/DayCareSessionFeeTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/DayCareSessionFeeTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/DongRoundingTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/DongRoundingTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/M11ArchTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/M11ArchTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/billing/ResidencyDayFeeTest.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/billing/ResidencyDayFeeTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeCostForBalance.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeCostForBalance.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeCostSources.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeCostSources.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeCostStatistics.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeCostStatistics.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeFamilyPortalCosts.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeFamilyPortalCosts.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeFinalPeriodCosts.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeFinalPeriodCosts.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeM11Config.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeM11Config.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/support/fake/billing/FakePurchaseBalance.java](../../../010-incurred-costs/backend/src/test/java/com/eldercare/backend/support/fake/billing/FakePurchaseBalance.java) |
| ?? | [specs/010-incurred-costs/api-declaration-verification.md](../../../010-incurred-costs/specs/010-incurred-costs/api-declaration-verification.md) |
| ?? | [specs/010-incurred-costs/architecture-fix-2026-10-30.md](../../../010-incurred-costs/specs/010-incurred-costs/architecture-fix-2026-10-30.md) |
| ?? | [specs/010-incurred-costs/develop-sync-2026-10-29.md](../../../010-incurred-costs/specs/010-incurred-costs/develop-sync-2026-10-29.md) |
| ?? | [specs/010-incurred-costs/develop-sync-2026-10-30.md](../../../010-incurred-costs/specs/010-incurred-costs/develop-sync-2026-10-30.md) |
| ?? | [specs/010-incurred-costs/foundation-verification.md](../../../010-incurred-costs/specs/010-incurred-costs/foundation-verification.md) |
| ?? | [specs/010-incurred-costs/leader-changes-review-2026-10-30.md](../../../010-incurred-costs/specs/010-incurred-costs/leader-changes-review-2026-10-30.md) |
| ?? | [specs/010-incurred-costs/preparation-review.md](../../../010-incurred-costs/specs/010-incurred-costs/preparation-review.md) |
| ?? | [specs/010-incurred-costs/r15-storage-verification.md](../../../010-incurred-costs/specs/010-incurred-costs/r15-storage-verification.md) |

## impl/008-q262

Thư mục: C:/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/008-q262

| Git | File |
| --- | --- |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/domain/AssignmentInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/domain/FloorManagerAssignment.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/domain/PrimaryAssigneeResolver.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/domain/StaffAssignment.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/persistence/AssignmentStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/AssignmentCommandService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/AssignmentLifecycle.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/AssignmentQueries.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/AssignmentReminders.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/AssignmentService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/AssignmentSources.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/FloorManagerService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/ResidentFinalAssignmentEffects.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/internal/service/port/AssignmentCommandPort.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/web/AssignmentController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/web/request/AssignmentRequests.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/assignment/web/response/AssignmentResponses.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/domain/Credential.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/domain/CredentialInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/domain/CredentialType.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/domain/CredentialValidity.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/domain/PracticeScope.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/persistence/CredentialStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/service/CredentialInvalidityEffects.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/service/CredentialReminders.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/internal/service/CredentialService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/web/CredentialController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/web/request/CredentialCommands.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/web/request/CredentialInputBody.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/web/request/CredentialTypeInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/web/response/CredentialBody.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/web/response/CredentialTypeBody.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/credential/web/response/PracticeScopeBody.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/domain/Department.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/domain/DepartmentDuty.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/persistence/DepartmentDutyStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/AlertStaffLookup.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/CareShiftLookup.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/DepartmentDutyService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/DutyDirectory.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/NursingShiftLookup.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/PracticeScopeLookup.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/ShiftLocationReferences.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/ShiftQueries.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/StaffDirectory.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/internal/service/StaffLicenseLookup.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/web/DepartmentController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/duty/web/response/DepartmentBody.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/domain/CriticalHandoverItem.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/domain/HandoverItem.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/domain/HandoverItemInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/domain/HandoverRecord.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/domain/HandoverSource.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/domain/NextHandoverShift.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/persistence/HandoverStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverContents.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverCorrection.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverNotifications.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverQueries.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverReminders.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverRetention.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/internal/service/HandoverSources.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/web/HandoverController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/web/request/HandoverRequests.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/handover/web/response/HandoverBody.java (đường dẫn cũ không còn) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/config/M09GovernanceConfig.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/config/M09GovernanceConfig.java) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/M09JobRuns.java (đường dẫn cũ không còn) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffStatisticsStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffStatisticsStore.java) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/service/AssignmentScopeAssignment.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/AssignmentScopeAssignment.java) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09Jobs.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09Jobs.java) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09Retention.java (đường dẫn cũ không còn) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09ScanAccess.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09ScanAccess.java) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09ScopeAssignment.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09ScopeAssignment.java) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09TimedTask.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/M09TimedTask.java) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/service/ShiftScopeAssignment.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/ShiftScopeAssignment.java) |
|  M | [backend/src/main/java/com/eldercare/backend/staffing/internal/service/StaffStatisticsService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/service/StaffStatisticsService.java) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/ratio/internal/domain/StaffingRatioCalculation.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/ratio/internal/domain/StaffingRatioCalculator.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/ratio/internal/persistence/StaffingRatioStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/ratio/internal/service/StaffingRatioEvents.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/ratio/internal/service/StaffingRatioService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/ratio/internal/service/StaffingRatioSources.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/ContinuousShiftChain.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/ShiftInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/ShiftLeadPeriod.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/ShiftMember.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/ShiftSchedule.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/ShiftTemplate.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/StaffAbsence.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/TemplateInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/domain/WorkShift.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/persistence/PostPublicationStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/persistence/ShiftStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/AbsenceReasonAccess.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/PostPublicationNotifications.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/PostPublicationService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/ShiftCommandService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/ShiftLifecycle.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/ShiftScheduleService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/port/ExtendedPublicationCheck.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/port/PublicationWarning.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/port/ShiftCommandPort.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/port/ShiftDepartureBasis.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/port/ShiftEntryBasis.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/internal/service/port/ShiftHandoverState.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/web/PostPublicationController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/web/ShiftController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/web/request/ShiftRequests.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/shift/web/response/ShiftResponses.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/domain/StaffImportData.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/domain/StaffImportRejected.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/domain/StaffImportRowValidator.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/domain/StaffInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/domain/StaffProfile.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/persistence/StaffEmploymentStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/persistence/StaffImportStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/persistence/StaffStore.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/service/StaffEmploymentEffects.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/service/StaffImportService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/service/StaffNotifications.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/internal/service/StaffService.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/web/StaffController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/web/StaffImportController.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/web/request/StaffCommands.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/web/request/StaffProfileInput.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/web/response/StaffImportResponses.java (đường dẫn cũ không còn) |
|  D | backend/src/main/java/com/eldercare/backend/staffing/staff/web/response/StaffProfileBody.java (đường dẫn cũ không còn) |
|  M | [backend/src/test/java/com/eldercare/backend/LayerArchTest.java](../../backend/src/test/java/com/eldercare/backend/LayerArchTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/administration/RecordGroupTriggerIT.java](../../backend/src/test/java/com/eldercare/backend/administration/RecordGroupTriggerIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/reporting/FR_020_StaffingDashboardIT.java](../../backend/src/test/java/com/eldercare/backend/reporting/FR_020_StaffingDashboardIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/AlertStaffPortIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/AlertStaffPortIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/AssignmentGovernanceIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/AssignmentGovernanceIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/AssignmentHttpIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/AssignmentHttpIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/AssignmentIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/AssignmentIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/AssignmentSchemaIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/AssignmentSchemaIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/ContinuousShiftChainTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/ContinuousShiftChainTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/CredentialLifecycleIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/CredentialLifecycleIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/CredentialValidityTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/CredentialValidityTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/CriticalHandoverItemTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/CriticalHandoverItemTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/DepartmentCommandsIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/DepartmentCommandsIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/DepartmentDutyIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/DepartmentDutyIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/HandoverIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/HandoverIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/HandoverSchemaIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/HandoverSchemaIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/M09ArchRuleTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/M09ArchRuleTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/M09ArchTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/M09ArchTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/M09GovernanceIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/M09GovernanceIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/M09JobCatchUpIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/M09JobCatchUpIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/M09RecordProtectionIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/M09RecordProtectionIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/NextHandoverShiftTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/NextHandoverShiftTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/PostPublicationNotificationsTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/PostPublicationNotificationsTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/PostPublicationPortsIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/PostPublicationPortsIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/PostPublicationShiftIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/PostPublicationShiftIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/PrimaryAssigneeResolverTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/PrimaryAssigneeResolverTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/ShiftGovernanceIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/ShiftGovernanceIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/ShiftHttpIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/ShiftHttpIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/ShiftLifecycleIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/ShiftLifecycleIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/ShiftQueriesIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/ShiftQueriesIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/ShiftScheduleIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/ShiftScheduleIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/ShiftSchemaIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/ShiftSchemaIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffAccountLookupTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffAccountLookupTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffCommandsHttpIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffCommandsHttpIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffEmploymentIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffEmploymentIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffImportIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffImportIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffImportRowValidatorTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffImportRowValidatorTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffModuleIntegrationIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffModuleIntegrationIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffNotificationsTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffNotificationsTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffProfileHttpIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffProfileHttpIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffSchemaIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffSchemaIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffSpiIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffSpiIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffStatisticsIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffStatisticsIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffStatisticsReportFieldsIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffStatisticsReportFieldsIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioCalculatorTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioCalculatorTest.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioHttpIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioHttpIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioSchemaIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioSchemaIT.java) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioWarningIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingRatioWarningIT.java) |
|  D | backend/src/test/java/com/eldercare/backend/staffing/duty/internal/service/ForbiddenPortCallerFixture.java (đường dẫn cũ không còn) |
|  D | backend/src/test/java/com/eldercare/backend/staffing/ratio/internal/service/StaffingPresenceTest.java (đường dẫn cũ không còn) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/scheduling/M09SchedulingFixtures.java](../../backend/src/test/java/com/eldercare/backend/staffing/scheduling/M09SchedulingFixtures.java) |
|  D | backend/src/test/java/com/eldercare/backend/staffing/shift/internal/service/port/ShiftCommandPortFixture.java (đường dẫn cũ không còn) |
|  M | [backend/src/test/java/com/eldercare/backend/staffing/swap/internal/service/SwapPortCallerFixture.java](../../backend/src/test/java/com/eldercare/backend/staffing/swap/internal/service/SwapPortCallerFixture.java) |
|  M | [backend/src/test/java/com/eldercare/backend/support/fake/staffing/FakeExtendedPublicationChecks.java](../../backend/src/test/java/com/eldercare/backend/support/fake/staffing/FakeExtendedPublicationChecks.java) |
|  M | [backend/src/test/java/com/eldercare/backend/support/fake/staffing/FakeM09Test.java](../../backend/src/test/java/com/eldercare/backend/support/fake/staffing/FakeM09Test.java) |
|  M | [docs/progress.md](../../docs/progress.md) |
|  M | [docs/team-plan.md](../../docs/team-plan.md) |
|  M | [specs/000-shared-foundation/glossary.md](../000-shared-foundation/glossary.md) |
|  M | [specs/008-staff-shift-handover/data-model.md](data-model.md) |
|  M | [specs/008-staff-shift-handover/plan.md](plan.md) |
|  M | [specs/008-staff-shift-handover/tasks.md](tasks.md) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/AssignmentInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/AssignmentInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/FloorManagerAssignment.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/FloorManagerAssignment.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/PrimaryAssigneeResolver.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/PrimaryAssigneeResolver.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/StaffAssignment.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/domain/StaffAssignment.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/persistence/AssignmentStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/persistence/AssignmentStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentCommandPort.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentCommandPort.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentCommandService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentCommandService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentLifecycle.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentLifecycle.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentQueries.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentQueries.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentReminders.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentReminders.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentSources.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/AssignmentSources.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/FloorManagerService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/FloorManagerService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/ResidentFinalAssignmentEffects.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/assignment/service/ResidentFinalAssignmentEffects.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/Credential.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/Credential.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/CredentialInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/CredentialInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/CredentialType.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/CredentialType.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/CredentialValidity.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/CredentialValidity.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/PracticeScope.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/domain/PracticeScope.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/persistence/CredentialStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/persistence/CredentialStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/service/CredentialInvalidityEffects.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/service/CredentialInvalidityEffects.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/service/CredentialReminders.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/service/CredentialReminders.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/credential/service/CredentialService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/credential/service/CredentialService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingCatalogData.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingCatalogData.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingEventData.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingEventData.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingHistoryData.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingHistoryData.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingRecordData.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/domain/StaffingRecordData.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/domain/Department.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/domain/Department.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/domain/DepartmentDuty.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/domain/DepartmentDuty.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/persistence/DepartmentDutyStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/persistence/DepartmentDutyStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/AlertStaffLookup.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/AlertStaffLookup.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/CareShiftLookup.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/CareShiftLookup.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/DepartmentDutyService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/DepartmentDutyService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/DutyDirectory.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/DutyDirectory.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/NursingShiftLookup.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/NursingShiftLookup.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/PracticeScopeLookup.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/PracticeScopeLookup.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/ShiftLocationReferences.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/ShiftLocationReferences.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/ShiftQueries.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/ShiftQueries.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/StaffDirectory.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/StaffDirectory.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/StaffLicenseLookup.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/duty/service/StaffLicenseLookup.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/CriticalHandoverItem.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/CriticalHandoverItem.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverItem.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverItem.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverItemInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverItemInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverJsonData.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverJsonData.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverRecord.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverRecord.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverSource.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/HandoverSource.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/NextHandoverShift.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/domain/NextHandoverShift.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/persistence/HandoverStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/persistence/HandoverStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverContents.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverContents.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverCorrection.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverCorrection.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverNotifications.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverNotifications.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverQueries.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverQueries.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverReminders.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverReminders.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverRetention.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverRetention.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverSources.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/handover/service/HandoverSources.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/M09Retention.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/M09Retention.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/M09RetentionFactory.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/M09RetentionFactory.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingCatalogStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingCatalogStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingHistoryStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingHistoryStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingRecordStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/persistence/StaffingRecordStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/domain/StaffingRatioCalculation.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/domain/StaffingRatioCalculation.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/domain/StaffingRatioCalculator.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/domain/StaffingRatioCalculator.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/persistence/StaffingRatioStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/persistence/StaffingRatioStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingRatioEvents.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingRatioEvents.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingRatioService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingRatioService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingRatioSources.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingRatioSources.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ContinuousShiftChain.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ContinuousShiftChain.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftLeadPeriod.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftLeadPeriod.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftMember.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftMember.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftSchedule.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftSchedule.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftTemplate.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/ShiftTemplate.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/StaffAbsence.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/StaffAbsence.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/TemplateInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/TemplateInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/WorkShift.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/domain/WorkShift.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/persistence/PostPublicationStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/persistence/PostPublicationStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/persistence/ShiftStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/persistence/ShiftStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/AbsenceReasonAccess.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/AbsenceReasonAccess.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ExtendedPublicationCheck.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ExtendedPublicationCheck.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/PostPublicationNotifications.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/PostPublicationNotifications.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/PostPublicationService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/PostPublicationService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/PublicationWarning.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/PublicationWarning.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftCommandPort.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftCommandPort.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftCommandService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftCommandService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftDepartureBasis.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftDepartureBasis.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftEntryBasis.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftEntryBasis.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftHandoverState.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftHandoverState.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftLifecycle.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftLifecycle.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftScheduleService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/shift/service/ShiftScheduleService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffImportData.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffImportData.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffImportRejected.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffImportRejected.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffImportRowValidator.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffImportRowValidator.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffProfile.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/domain/StaffProfile.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/persistence/StaffEmploymentStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/persistence/StaffEmploymentStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/persistence/StaffImportStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/persistence/StaffImportStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/persistence/StaffStore.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/persistence/StaffStore.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffEmploymentEffects.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffEmploymentEffects.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffImportService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffImportService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffNotifications.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffNotifications.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffService.java](../../backend/src/main/java/com/eldercare/backend/staffing/internal/staff/service/StaffService.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/AssignmentController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/AssignmentController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/CredentialController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/CredentialController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/DepartmentController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/DepartmentController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/HandoverController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/HandoverController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/PostPublicationController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/PostPublicationController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/ShiftController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/ShiftController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/StaffController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/StaffController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/StaffImportController.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/StaffImportController.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/AssignmentRequests.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/AssignmentRequests.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/CredentialCommands.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/CredentialCommands.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/CredentialInputBody.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/CredentialInputBody.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/CredentialTypeInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/CredentialTypeInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/HandoverRequests.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/HandoverRequests.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/ShiftRequests.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/ShiftRequests.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/StaffCommands.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/StaffCommands.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/request/StaffProfileInput.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/request/StaffProfileInput.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/AssignmentResponses.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/AssignmentResponses.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/CredentialBody.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/CredentialBody.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/CredentialTypeBody.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/CredentialTypeBody.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/DepartmentBody.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/DepartmentBody.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/HandoverBody.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/HandoverBody.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/PracticeScopeBody.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/PracticeScopeBody.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/ShiftResponses.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/ShiftResponses.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/StaffImportResponses.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/StaffImportResponses.java) |
| ?? | [backend/src/main/java/com/eldercare/backend/staffing/web/response/StaffProfileBody.java](../../backend/src/main/java/com/eldercare/backend/staffing/web/response/StaffProfileBody.java) |
| ?? | [backend/src/main/resources/db/migration/V008_94__shared_staffing_legacy_ids.sql](../../backend/src/main/resources/db/migration/V008_94__shared_staffing_legacy_ids.sql) |
| ?? | [backend/src/main/resources/db/migration/V008_95__staffing_table_consolidation.sql](../../backend/src/main/resources/db/migration/V008_95__staffing_table_consolidation.sql) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/DutyDirectoryIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/DutyDirectoryIT.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/HandoverEmbeddedItemsIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/HandoverEmbeddedItemsIT.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/StaffingCatalogIdentityIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingCatalogIdentityIT.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/StaffingSharedConstraintsIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingSharedConstraintsIT.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/StaffingTableConsolidationMigrationIT.java](../../backend/src/test/java/com/eldercare/backend/staffing/StaffingTableConsolidationMigrationIT.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/internal/duty/service/ForbiddenPortCallerFixture.java](../../backend/src/test/java/com/eldercare/backend/staffing/internal/duty/service/ForbiddenPortCallerFixture.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingPresenceTest.java](../../backend/src/test/java/com/eldercare/backend/staffing/internal/ratio/service/StaffingPresenceTest.java) |
| ?? | [backend/src/test/java/com/eldercare/backend/staffing/internal/shift/service/ShiftCommandPortFixture.java](../../backend/src/test/java/com/eldercare/backend/staffing/internal/shift/service/ShiftCommandPortFixture.java) |
| ?? | [specs/008-staff-shift-handover/q262-review.md](q262-review.md) |
| ?? | [specs/008-staff-shift-handover/r15-architecture-files.md](r15-architecture-files.md) |
| ?? | [specs/008-staff-shift-handover/r15-architecture-review.md](r15-architecture-review.md) |
| ?? | [specs/008-staff-shift-handover/r15-implementation-files.md](r15-implementation-files.md) |
| ?? | [specs/008-staff-shift-handover/r15-implementation-review.md](r15-implementation-review.md) |
| ?? | [specs/008-staff-shift-handover/r15-migration-identity-review.md](r15-migration-identity-review.md) |
| ?? | [specs/008-staff-shift-handover/shared-event-scan-proposal.md](shared-event-scan-proposal.md) |
| ?? | [specs/008-staff-shift-handover/work-summary-for-e-files.md](work-summary-for-e-files.md) |
| ?? | [specs/008-staff-shift-handover/work-summary-for-e.md](work-summary-for-e.md) |
