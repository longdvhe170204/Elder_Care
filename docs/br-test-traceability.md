# Truy vết 78 quy tắc nghiệp vụ → test backend

Căn cứ: [nghiep-vu.md](business/nghiep-vu.md) mục 30 (78 quy tắc `BR-01` → `BR-78`). Một lớp test thuộc một quy tắc khi tên lớp bắt đầu bằng `BR_NN_`, khi lớp có phương thức test tên `BR_NN_…`, hoặc khi lớp gắn `@Tag("BR-NN")`. Chạy test của một quy tắc gắn thẻ: `./mvnw verify -Dgroups=BR-NN`; theo tên lớp: `-Dit.test="BR_NN_*"` hoặc `-Dtest="BR_NN_*"`.

Bảng sinh từ mã nguồn `backend/src/test/java` ngày 2026-11-13; đổi tên hoặc thêm test thì sinh lại. Test mang mã FR, DBR, SC, task của spec không nằm trong bảng trừ khi có phương thức hoặc thẻ mang mã quy tắc.

## FE-01 Resident Profile & Assessment

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-01 | 1 | `BR_01_TransitionTableTest` |
| BR-02 | 2 | `BR_02_ClosedProfileIT`, `ProfileOpenGuardArchTest` |
| BR-03 | 3 | `BR_03_AllergyIT`, `BR_03_PendingCheckIT`, `ConflictCheckTest` |
| BR-04 | 1 | `BR_04_AssessmentIT` |
| BR-05 | 2 | `BR_05_FallIT`, `BR_05_ReassessmentNoticeIT` |
| BR-06 | 2 | `EndOfLifeWishVersionIT`, `TreatmentWishVersionIT` |

## FE-02 Admission & Stay

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-07 | 1 | `BR_07_MatchingEntriesIT` |
| BR-08 | 2 | `BR_08_ContentAtDateIT`, `BR_08_ContractCareLevelIT` |
| BR-09 | 1 | `BR_09_ApplyAddendumIT` |
| BR-10 | 1 | `BR_10_ContractReminderIT` |
| BR-11 | 2 | `BR_11_ReleaseBedIT`, `BR_11_TemporaryAbsenceOverdueIT` |
| BR-12 | 3 | `BR_12_DeregistrationIT`, `BR_12_RenewalIT`, `BR_12_ResidenceRegistrationIT` |

## FE-03 Room & Bed

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-13 | 3 | `BR_13_AllocationConditionsIT`, `BR_13_AllocationSuitabilityTest`, `FR_034_ResidenceTypeReassessIT` |
| BR-14 | 1 | `BR_14_BedTransferIT` |
| BR-15 | 2 | `BR_15_BedReadinessIT`, `FR_011_BedTransitionsTest` |
| BR-16 | 1 | `BR_16_DayCareCapacityIT` |

## FE-04 Care Plan & Daily Care

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-17 | 1 | `BR_17_CarePlanReviewIT` |
| BR-18 | 5 | `BR_18_CarePlanItemSequenceTest`, `BR_18_DayCareAttendanceIT`, `BR_18_ResidentStatusTasksIT`, `BR_18_ShiftTaskPlanningIT`, `FR_026_CareTaskTransitionsTest` |
| BR-19 | 3 | `BR_19_OverdueReminderIT`, `BR_19_RemindOnceIT`, `FR_029_AlertLifecycleTest` |
| BR-20 | 2 | `BR_20_KiemTraChatLuongIT`, `BR_20_LateOfflineRecordingIT` |
| BR-21 | 3 | `BR_21_ReassignIT`, `BR_21_SharedTaskIT`, `FR_047_TaskHandoverIT` |
| BR-22 | 4 | `BR_22_NegativeMoodStreakTest`, `BR_22_PoorMealStreakTest`, `BR_22_ReviewIT`, `BR_22_WaterIntakeRuleTest` |
| BR-23 | 2 | `OfflinePendingReviewIT`, `SyncResultIT` |

## FE-05 Health Monitoring

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-24 | 7 | `BR_24_ApplicableThresholdIT`, `BR_24_CreateAlertIT`, `BR_24_MissingPersonalThresholdTest`, `BR_24_RemeasurementIT`, `BR_24_ThresholdRulesTest`, `BR_24_VitalSignPortIT`, `FR_034_CareResultRecordingIT` |
| BR-25 | 2 | `BR_25_ExaminationScopeTest`, `BR_25_PrescriberAuthorityIT` |

## FE-06 Medication

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-26 | 4 | `BR_26_CancelDosageChangeIT`, `BR_26_DosageChangeIT`, `BR_26_MedicationScheduleTest`, `BR_26_PrescriptionExpirationIT` |
| BR-27 | 5 | `BR_27_ConsecutiveRefusalTest`, `BR_27_FloorWithoutNurseTest`, `BR_27_LateMissedTest`, `BR_27_TwoOfflineCopiesTest`, `BR_M07_MedicationAlertRequestedTest` |
| BR-28 | 6 | `BR_28_AbsenceFlowIT`, `BR_28_AbsenceScheduleContextTest`, `BR_28_InitialHealthRecordChangedTest`, `BR_28_RoiVienIT`, `MedicationHandoverTest`, `MedicationStatusParticipantTest` |
| BR-29 | 1 | `BR_29_PrnLimitTest` |
| BR-30 | 2 | `BR_30_DuplicateIngredientAllergyTest`, `BR_30_ExternalPrescriberBasisIT` |
| BR-31 | 2 | `BR_31_ReconciliationDeadlineTest`, `BR_31_StatusParticipantDelegationTest` |
| BR-32 | 1 | `BR_32_SuppliedMedicationIntakeIT` |

## FE-07 Nutrition & Meals

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-33 | 2 | `BR_33_ConcurrentApplyIT`, `BR_33_DietAssignmentIT` |
| BR-34 | 3 | `AllergyWarningRuleTest`, `BR_34_CoverageIT`, `ConflictCheckTest` |
| BR-35 | 4 | `BR_35_AdjustmentIT`, `BR_35_DishSwapIT`, `BR_35_PortionClosingIT`, `M08RecordGroupTriggerIT` |
| BR-36 | 3 | `BR_36_RetainedSampleIT`, `BR_36_SlipDeliveryIT`, `BR_36_SlipReceiptIT` |
| BR-37 | 3 | `BR_37_FamilyFoodIT`, `BR_37_ServingConfirmationIT`, `FamilyFoodCheckTest` |

## FE-08 Activities & Engagement

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-38 | 2 | `BR_38_DangKyIT`, `BR_38_TaoBuoiTuMauIT` |
| BR-39 | 1 | `BR_39_ThieuNguoiIT` |

## FE-09 Incidents & Alerts

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-40 | 2 | `AlertEscalationChainTest`, `BR_40_EscalationIT` |
| BR-41 | 5 | `BR_41_AlertMergeIT`, `BR_41_CareAlertRequestedTest`, `BR_41_TrendRuleIT`, `BR_41_TrendRuleTest`, `RepeatWithin24HoursCounterTest` |
| BR-42 | 1 | `BR_42_EmergencyNotificationIT` |
| BR-43 | 6 | `BR_43_CancelEmergencyIncidentIT`, `BR_43_CloseIncidentIT`, `BR_43_IncidentLevelChangeTest`, `BR_43_IncidentLevelControllerTest`, `BR_43_RealSourceStopIT`, `IncidentTriggerIT` |
| BR-44 | 7 | `BR_44_AdministrativeContactIT`, `BR_44_AllocationCorrectionIT`, `BR_44_ControlledTransferIT`, `BR_44_IsolationApiTest`, `BR_44_NonBlockingIsolationTest`, `ContainmentScopeTest`, `FR_040_IncidentRecordTest` |
| BR-45 | 1 | `BR_45_TransferOutIT` |
| BR-46 | 2 | `BR_46_CriticalConditionIT`, `BR_46_WishReconfirmationIT` |
| BR-47 | 3 | `BR_47_EmergencyAssistButtonIT`, `BR_47_SosButtonIT`, `BR_47_SosLocationIT` |
| BR-48 | 2 | `BR_48_AbsentResidentFloorTest`, `BR_48_HandoverIT` |

## FE-10 Staff & Shifts

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-49 | 1 | `CredentialValidityTest` |
| BR-50 | 3 | `ContinuousShiftChainTest`, `StaffingRatioCalculatorTest`, `SwapRulesTest` |
| BR-51 | 1 | `BR_51_ShiftSwapIT` |
| BR-52 | 1 | `HandoverIT` |
| BR-53 | 3 | `EarlyAssignmentEndTest`, `PrimaryAssigneeResolverTest`, `StaffEmploymentIT` |

## FE-11 Assets & Personal Belongings

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-54 | 3 | `BR_54_BedAssetIT`, `BR_54_VehicleScheduleIT`, `VehicleBookingCheckTest` |
| BR-55 | 1 | `BR_55_HandoverIT` |
| BR-56 | 2 | `BR_56_ReturnIT`, `BR_56_SelfCustodyConsentIT` |
| BR-57 | 3 | `BR_57_DisposalIT`, `BR_57_InventoryCheckIT`, `BR_57_LostDamageIncidentIT` |

## FE-12 Billing & Payment

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-58 | 4 | `BR_58_FeeEventTest`, `BR_58_OvernightStayIT`, `BR_58_SuKienTinhPhiIT`, `CostPricingServiceTest` |
| BR-59 | 1 | `CostRecalculationServiceTest` |
| BR-60 | 2 | `DayCareSessionFeeTest`, `ResidencyDayFeeTest` |
| BR-61 | 2 | `BalanceStatusRuleTest`, `NoticeDecisionTest` |
| BR-62 | 3 | `PeriodCostChangeTest`, `PurchaseConsentTest`, `PurchaseExcessCheckTest` |
| BR-63 | 3 | `PaymentNoticeDocumentTest`, `SettlementConditionsTest`, `SettlementStatementDocumentTest` |
| BR-64 | 6 | `ApprovedAmountCheckTest`, `BR_64_OverrunIT`, `BR_64_ProcurementRequestIT`, `BR_64_ReconciliationIT`, `BR_64_ReminderIT`, `ReceiptMatchTest` |

## FE-13 Family

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-65 | 2 | `BR_65_PortalByRightsIT`, `BR_65_RightChangeRequestIT` |
| BR-66 | 2 | `BR_66_PickupProcessIT`, `BR_66_VisitRegistrationIT` |
| BR-67 | 1 | `BR_67_FeedbackIT` |

## FE-14 Discharge & Closure

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-68 | 1 | `BR_68_EndingConditionIT` |
| BR-69 | 1 | `M02FieldClassificationConfigTest` |

## FE-15 Admin, Notification & Reporting

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-70 | 2 | `RequiresPermissionArchTest`, `RestrictionOrderTest` |
| BR-71 | 4 | `BR_71_CareResultCorrectionIT`, `BR_71_CareResultCorrectionProviderTest`, `BR_71_SourceCorrectedRecipientTest`, `VitalSignHistoryCorrectionTest` |
| BR-72 | 1 | `EffectiveDateTest` |
| BR-73 | 2 | `FR_052a_PasswordResetRulesTest`, `SessionTokenFilterIT` |
| BR-74 | 1 | `RequestTransitionTableTest` |
| BR-75 | 4 | `BR_75_CallReminderIT`, `BR_75_ChannelSelectionIT`, `BR_75_QuietHoursIT`, `ChannelSelectionTest` |
| BR-76 | 1 | `RecipientResolutionIT` |
| BR-77 | 3 | `FR_012_TimeScopeIT`, `SC_004_ScopeIT`, `SC_008_ReadOnlyIT` |

## FE-16 Resident Life

| Quy tắc | Số lớp | Lớp test |
| --- | --- | --- |
| BR-78 | 3 | `CreateResidentAccountIT`, `FR_068a_KenhNguoiCaoTuoiIT`, `LoginQrCodeIT` |
