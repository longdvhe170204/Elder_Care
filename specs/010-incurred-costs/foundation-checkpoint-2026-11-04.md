# Checkpoint nền 010 — 2026-11-04

Nhánh `impl/010-foundation-next`, worktree `.worktrees/010-foundation-next`, nền `origin/develop 62fdb4f2517aab104d073c309688c5dd2075a3da`. Chưa commit/push/PR. Đây là một phần nền, **chưa xong feature 010 hoặc Phase 2**.

## Phần đã kiểm xanh

- T002: dùng FakeRepresentatives do B sở hữu, không tạo fake thứ hai.
- T005: trạng thái người/M11, lịch sử tính lại giữ precision, chống trùng marker; kiểm trên API thật của A/MySQL.
- T006: hoàn tất BillingPeriod và 15 ca biên, giữ domain Java thuần. Adapter nguồn và ngày Giải phóng giường còn các story sau.
- T007: khóa người trước bảng, bảo vệ lệnh đầu đồng thời; khung hai job có ShedLock/công tắc chung, chạy bù và rollback marker cùng tác động. Chưa có tác vụ nghiệp vụ chuyển kỳ/nhắc thật.
- T008a: phân loại tài chính/thuốc và tra phạm vi bảng/khoản theo đúng người đã lưu. T008 vẫn mở cho retention/quyền tệp.

Không thêm migration, thư viện, endpoint hoặc API công khai. Chỉ phần đăng ký hai job M11 trong `SchedulerScopeArchTest` nằm ở test chung: AGENTS.md mục 4 yêu cầu đăng ký, A cần review; không đổi mã common production.

## Bằng chứng test

Log và bản sao báo cáo nằm ngoài Git tại `.worktrees/.snapshots/010-foundation-20261104/`.

| Nhóm | Đỏ trước cài | Xanh sau cài |
| --- | --- | --- |
| Fixture người đại diện | representative-red.log: runtime thiếu bean | CostSharedFakesTest |
| BillingPeriod, khóa, kiểu history | billing-period-red.log, cost-lock-red.log, shared-rows-red.log: thiếu lớp, lỗi testCompile | BillingPeriodTest 15; CostLockIT 6; CostSharedRowsIT |
| Precision history | shared-precision-red.log: 7 giá trị vượt precision vẫn được lưu | CostSharedRowsIT 10 ca tổng cộng, có 7 ca precision; không bỏ assertion |
| Khung job | jobs-red.log: testCompile thiếu khung | CostJobCatchUpIT 8; SchedulingSwitchIT 2 |
| Metadata/phạm vi | governance-red.log: runtime thiếu provider, 1 failure/2 error | CostGovernanceIT 4 |

Lượt chọn cuối `foundation-complete-checkpoint.log`: Maven exit 0; **104 unit/architecture + 45 integration**, không lỗi, không bỏ qua. IT gồm CostSchemaIT 15, CostLockIT 6, CostSharedRowsIT 10, CostJobCatchUpIT 8, CostGovernanceIT 4, SchedulingSwitchIT 2. Spotless đã áp dụng. Báo cáo giữ tại `reports-selected-checkpoint/`.

`clean verify` toàn backend kết thúc **Maven exit 0**: **1.087 unit/architecture (218 XML suites) + 2.629 integration (480 suites)**, 0 failure/error; 2 ca của SC_001_UrgentConcurrencyIT bỏ qua theo điều kiện sẵn có `eldercare.perf=true` (lượt này không bật). Không sửa điều kiện hoặc tắt test để xanh. MissingConsentReminderIT và các lớp M11 mới xanh trong lượt toàn kho này. Log `clean-verify-full.log`; bản sao XML/summary tại `reports-full/`. Tổng 3.716 ca báo cáo, gồm 2 ca bỏ qua; không cộng lại lượt chọn vào tổng.

Lượt này không chứa R-15 chưa commit của nhánh 008. Kết quả xanh của nhánh 010 không giải quyết gate `RecordEventApi.scanBefore`/T050 ở nhánh 008. A đã có các API bảng chung cần cho nền 010; thiếu riêng hàm quét mà 008 đang chờ.

Lưu ý kiểm: MySQL chuẩn hóa scale JSON (`100.00` thành `100.0`), nên test history so giá trị BigDecimal với comparator số học; không làm tròn/đổi payload production cho test. Các lỗi testCompile ở lượt đầu không phải bằng chứng assertion nghiệp vụ từng đỏ.

## Việc còn lại và người chặn

1. **Chặn C/005:** quantity/newQuantity theo từng loại kết quả, AMEND_CONTENT/VOID. Người dùng báo đã chốt và sẽ gửi; chưa có nội dung nên chưa tự cài quy đổi. C vẫn cần cài producer; E cài và nghiệm thu consumer sau khi hợp đồng chốt.
2. **E/010:** T008 retention toàn chuỗi, metadata/quyền tệp mua hộ khi record được cài. Chưa đủ điều kiện đóng Phase 2.
3. **B/E:** rà `ResidencyDayApi.billingStopDate` khi hồ sơ PREPARING quá ngày E: hiện trả ngày dự kiến; FR-035 cho lập ngày E+1 đã kết thúc khi chưa thực hiện Kết thúc lưu trú. Adapter T011 không được dùng mốc này một cách máy móc hoặc suy từ ngày hết hạn hợp đồng.
4. **Chủ dự án/E:** 17 phản hồi HTTP còn thiếu content/schema; danh sách thao tác/dòng ở [http-response-review-2026-11-04.md](http-response-review-2026-11-04.md), chốt trước endpoint liên quan theo plan.md. Không tự thêm DTO/field.
5. **E:** các story T009 trở đi chưa triển khai; CostStatisticsApi/FinalPeriodPort/PostDeathItemPort thật còn chờ E; 017 phụ thuộc 010, không tuyên bố đã có nguồn thật.

Hai quyết định 008 về thời điểm kiểm khi nhận việc tồn và cờ checklist bàn giao vẫn chờ chủ dự án theo phản hồi người dùng; không thuộc thay đổi 010 này.

Checklist hiện tại: **9/40 task**, gồm T008a mới tách từ T008; không dùng số này để kết luận toàn feature hoặc Phase 2 hoàn tất.

## Tự commit và push

Chỉ thực hiện sau khi đọc kết quả toàn bộ test cuối và diff. Các lệnh sau do người dùng tự chạy:

```powershell
Set-Location 'C:\Users\PHAN THANH LONG\Capstone project\Elder_Care\.worktrees\010-foundation-next'
git status --short
git diff --check
git add -- backend/src/main/java/com/eldercare/backend/billing backend/src/test/java/com/eldercare/backend/billing backend/src/test/java/com/eldercare/backend/support/fake/billing/FakeM11Config.java backend/src/test/java/com/eldercare/backend/common/scheduling/SchedulerScopeArchTest.java specs/010-incurred-costs specs/000-shared-foundation/glossary.md docs/team-plan.md docs/progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "feat(010): add billing foundation locks and scheduled job framework"
git push -u origin impl/010-foundation-next
```

Push lên nhánh feature, không push trực tiếp develop. Không cần merge/rebase để commit checkpoint này; nếu origin/develop đổi tiếp thì rà chênh lệch và xin đồng ý trước khi nhập mã mới. AI không chạy commit/push hoặc mở PR.
