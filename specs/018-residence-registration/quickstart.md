# Quickstart: Kiểm chứng feature 018

**Ngày**: 2026-10-10 | **Plan**: [plan.md](plan.md)

Hướng dẫn dùng ở giai đoạn triển khai; giai đoạn thiết kế chưa có mã nên các lệnh chưa chạy được. Hợp đồng xem [contracts/api-018.yaml](contracts/api-018.yaml) và [contracts/module-interfaces.md](contracts/module-interfaces.md); bảng xem [data-model.md](data-model.md).

## 1. Điều kiện cần

- Như quickstart của 000 → 017; profile `demo` có đồng hồ giả lập.
- Dữ liệu mẫu: Hành chính HC; Quản lý viện QL; Điều dưỡng DD; Kế toán KT. A nội trú dài hạn 01/10/2026 → 30/09/2027; B nội trú ngắn ngày 14 ngày; C bán trú; E nội trú 14 ngày; F nội trú dài hạn thường trú cùng phường với viện.
- CFG theo mặc định Phụ lục 25: CFG-M02-09 1 ngày, CFG-M02-11 30 ngày, CFG-M02-12 1 ngày, CFG-M02-13 30 ngày.

## 2. Chạy

```text
backend/  ./mvnw verify
backend/  ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
frontend/ npm ci && npm test && npm run dev
```

## 3. Kịch bản kiểm chứng (profile demo)

| # | Thao tác | Kết quả mong đợi | Spec |
| --- | --- | --- | --- |
| Q1 | Hoàn tất tiếp nhận A ngày 01/10 | HC nhận nhắc; danh sách "cần lập" có A, hạn 02/10; chưa có khai báo | US1 #1, SC-001 |
| Q2 | HC lập khai báo cho A ngày 03/10 | Đăng ký tạm trú, Cần khai báo, hạn vẫn 02/10 | US1 #1, FR-002 |
| Q3 | Hoàn tất tiếp nhận B, lập khai báo; C lập khai báo | B: Thông báo lưu trú; C: 409 `KHONG_PHAI_NOI_TRU`, không có nhắc | US1 #2, #3 |
| Q4 | Hết 02/10 chưa ghi nộp | HC nhắc mỗi ngày; QL báo một lần | US1 #4 |
| Q5 | Hoàn tất tiếp nhận khi chưa có khai báo; Kết thúc lưu trú khi khai báo chưa xong | Không bị chặn | US1 #5, SC-004 |
| Q6 | HC xác nhận F thường trú cùng phường | Loại → Thông báo lưu trú, hạn giữ; gia hạn sau vẫn Thông báo lưu trú | US1 #6, Q-215 |
| Q7 | Ghi đã nộp; ghi chấp nhận thiếu ngày hết hạn; đủ | Đã nộp, dừng nhắc; 409 `THIEU_NGAY_HET_HAN`; Đã xác nhận | US2 |
| Q8 | Ghi chấp nhận khi chưa ghi nộp | 409 `CHUA_GHI_NOP` | US2 #4 |
| Q9 | Ghi từ chối | Về Cần khai báo; hạn mới = lúc ghi + 1 ngày | US2 #3 |
| Q10 | DD, KT gọi lệnh | 403 | US2 #6 |
| Q11 | 31/08/2027 (CFG-M02-13) | HC nhắc lập khai báo gia hạn mỗi ngày | US3 #1 |
| Q12 | Gia hạn Đã xác nhận | Khai báo cũ Được thay thế; đúng một Đã xác nhận | US3 #2, SC-003 |
| Q13 | Qua hết hạn không có gia hạn Đã xác nhận | Hết hạn; HC nhắc lập; QL báo; không có khai báo tự tạo | US3 #3 |
| Q14 | Phụ lục gia hạn B lên 60 ngày | HC nhắc; lập → Đăng ký tạm trú | US3 #4, BR-M02-14 |
| Q15 | Lập khai báo thứ hai khi có khai báo Đã nộp | 409 `DA_CO_KHAI_BAO_DANG_XU_LY` | US3 #5, DBR-33 |
| Q16 | Phụ lục gia hạn E lên 21 ngày | HC nhắc; lập → Thông báo lưu trú; khi Đã xác nhận, cũ Được thay thế | US3 #6 |
| Q17 | Chuyển người bán trú sang nội trú ngắn ngày (phụ lục) | HC nhắc lập; tổng thời gian tính từ ngày hiệu lực phần nội trú | FR-001a, Q-289 |
| Q18 | A Kết thúc lưu trú khi có khai báo Đã xác nhận | A "cần khai báo xóa", HC nhắc mỗi ngày; ghi đã khai báo xóa (hồ sơ trạng thái cuối) → Đã xóa | US4 #1, #2, #4 |
| Q19 | B qua đời khi khai báo Đã nộp | Khai báo Đã hủy; không nhắc khai báo xóa | US4 #3 |
| Q20 | Hai hợp đồng có khoảng trống 1 ngày | Tổng thời gian tính lại từ hợp đồng sau | Q-241 |
| Q21 | Lập khai báo gia hạn cho A rồi không nộp; qua hết ngày hết hạn một ngày nữa | Khai báo gia hạn có trong bảng việc ("Sắp hết hạn", rồi "Quá hạn"); HC nhắc mỗi ngày; QL báo một lần; tính vào số liệu 016 | US3, FR-002, FR-011 |
| Q22 | Người chỉ có Thông báo lưu trú ký hợp đồng nội trú mới bắt đầu ngay ngày sau hợp đồng cũ | HC nhắc lập khai báo như khi gia hạn; có khoảng trống thì không nhắc | BR-M02-14, Q-241 |
| Q23 | Hợp đồng cũ Chấm dứt sớm, hợp đồng mới bắt đầu trong khoảng đã ghi của hợp đồng cũ; lập khai báo | Lập được; tổng thời gian tính liền chuỗi, không báo lỗi | Q-241 |
| Q24 | Đăng ký tạm trú của người đã Kết thúc lưu trú qua ngày hết hạn | Chuyển Hết hạn; Quản lý viện không nhận báo; vẫn cần khai báo xóa | BR-M02-12, BR-M02-13 |

## 4. Nhóm test tự động

| Nhóm | Test | Kịch bản |
| --- | --- | --- |
| Hàm thuần | `ExpectedStayDurationTest`, `RegistrationTypeRuleTest` | Q3, Q14, Q16, Q17, Q20 |
| Khai báo | `BR_M02_11_ResidenceRegistrationIT`, `RegistrationSubmissionResultIT`, `BR_M02_12_RenewalIT`, `BR_M02_13_DeregistrationIT`, `BR_M02_14_TypeChangeIT`, `DBR_33_OneRegistrationIT`, `RegistrationStoreIT`, `RegistrationProvidersIT`, `RegistrationStatisticsIT` | Q1 → Q19 |
| Việc hằng ngày | `RegistrationCatchUpIT` (bốn việc `REG_*` là `M02DailyTask` xét trạng thái tại ngày chạy; không nhắc trùng khi chạy lại); chạy bù từng ngày do `BR_M02_19_CatchUpRunIT` của 004 | Q4, Q11, Q13 |
| Kiến trúc | `ResidenceRegistrationArchTest`: `admission.scheduling` không lập khai báo (Q-306) | |

## 5. Tiêu chí hoàn tất

| SC | Cách đo |
| --- | --- |
| SC-001 | Q1 → Q3 |
| SC-002 | Q4, Q11, Q13 với đồng hồ giả lập |
| SC-003 | Q12, Q15 |
| SC-004 | Q5 |
| SC-005 | Đo thao tác ghi nộp, ghi kết quả |

## 6. Trạng thái kiểm chứng (cập nhật 2026-11-06, đủ 18/18 task)

- Mã lỗi ở mục 3 là tên logic; mã trả về theo api-018 (glossary 24.4): `KHONG_PHAI_NOI_TRU` → `NOT_RESIDENTIAL_CARE`, `THIEU_NGAY_HET_HAN` → `EXPIRY_DATE_REQUIRED`, `CHUA_GHI_NOP` → `NOT_SUBMITTED`, `DA_CO_KHAI_BAO_DANG_XU_LY` → `REGISTRATION_IN_PROGRESS_EXISTS`.
- Chưa chạy kịch bản mục 3 trên profile `demo` bằng người thật: màn web đã có (BL-96) nhưng chưa có dữ liệu mẫu A, B, C, E, F; bốn việc hằng ngày là `M02DailyTask` của job `M02_DAILY` (không còn BL-90) nên chạy theo đồng hồ giả lập của `demo`. Kịch bản thủ công theo bước BF ở `test-018.md` (thư mục ElderCare_ThanhVienB). Bằng chứng hiện có là test tự động (MySQL test `localhost:3307`), gọi lệnh qua HTTP hoặc gọi thẳng hàm nhắc với ngày giả lập:

| # | Test | Ghi chú |
| --- | --- | --- |
| Q1, Q2 | `BR_M02_11_ResidenceRegistrationIT` `Q1_*` | |
| Q3 | `BR_M02_11_*` `Q2_*`, `Q3_*`, `BR_M02_11_thirtyDays*`; `RegistrationTypeRuleTest` | |
| Q4 | `BR_M02_11_*` `Q4_*`; `RegistrationSubmissionResultIT` `Q9_afterARejection_*`, `Q9_aManagerAlreadyTold*` | gọi `RegistrationOverdueReminders.remind(day)`; job `M02_DAILY` chạy bốn việc |
| Q5 | `BR_M02_11_*` `Q5_*`; `BR_M02_13_DeregistrationIT` `SC_004_*`, `US4_1_*` | |
| Q6 | `BR_M02_11_*` `Q6_*`; `RegistrationSubmissionResultIT` `FR_001_*Local*` | |
| Q7 → Q10 | `RegistrationSubmissionResultIT` `Q7_*` → `Q10_*` | |
| Q11 | `BR_M02_12_RenewalIT` `Q11_*` | gọi `RegistrationRenewalReminders.remind(day)` |
| Q12 | `BR_M02_12_*` `Q12_*`; `RegistrationStoreIT` `DBR_33_*` | |
| Q13 | `BR_M02_12_*` `Q13_*`, `Z_04_*` | gọi `RegistrationExpiry.expire(day)` |
| Q14, Q16, Q17 | `BR_M02_14_TypeChangeIT`; `BR_M02_11_*` `FR_001a_*` | |
| Q15 | `BR_M02_12_*` `Q15_*`; `DBR_33_OneRegistrationIT` | |
| Q18 | `BR_M02_13_*` `US4_1_*`, `US4_2_*`, `US4_4_*`, `FR_008_*` | nhắc mỗi ngày gọi `RegistrationDeregistration.remind(day)` |
| Q19 | `BR_M02_13_*` `US4_3_*` | |
| Q20 | `ExpectedStayDurationTest` `oneDayGap_*` | |
| Q21 | `RegistrationReviewIT` `FR_011_aRenewalInProgress_*`, `FR_002_aSubmittedRenewal_isNotOverdue` | gọi `RegistrationOverdueReminders.remind(day)` |
| Q22 | `RegistrationReviewIT` `BR_M02_14_aConsecutiveResidentialContract_*`, `BR_M02_14_aNonConsecutiveResidentialContract_*` | |
| Q23 | `RegistrationReviewIT` `Q_241_aTerminatedContractOverlappedByTheNextOne_*` | |
| Q24 | `RegistrationReviewIT` `BR_M02_12_expiryOfSomeoneWhoEndedResidency_*` | |

- SC-005 (thời gian thao tác) chưa đo: cần người thật trên màn web.
