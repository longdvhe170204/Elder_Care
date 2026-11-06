# Nền chọn đơn giá 010 — 2026-11-06

Nhánh `impl/010-foundation-next`, nền `70697650`, dùng worktree/spec hiện có. Theo chỉ đạo mới, tiếp tục code trên nhánh riêng; việc PR/merge chờ leader và người dùng. Không đồng bộ nhánh, commit, push, đổi trạng thái PR #112 hoặc tạo PR mới trong lượt này.

## T006a đã làm

Tách phần chọn đơn giá của T014 thành task nền độc lập. Giữ T008 và gate Phase 2 mở, không đưa luồng user story chưa đủ điều kiện ra HTTP.

`CostPricingService.event` dùng API công khai hiện có của B:

- Tra `ContractReadApi.contentAtDate` đúng ngày phát sinh. Dịch vụ đăng ký hoặc có giá đã ghi trong `recordedUnitPrices` dùng giá của nội dung hợp đồng đó; giữ contractId/contentVersion/addendumId. Giá danh mục mới không ghi đè giá đã ký; thiếu giá hợp đồng không tự chuyển sang giá ngoài hợp đồng.
- Dịch vụ trong gói có thành tiền 0. Giữ đơn giá đã ghi nếu có; nếu nguồn không có đơn giá riêng, giữ NULL và không đánh nhầm là thiếu giá cần bổ sung. Không tự gán đơn giá bằng 0 để che dữ liệu thiếu.
- Ngoài hợp đồng mới lấy `UnitPriceApi.unitPriceAt(serviceId, incurredOn, true)`, giữ priceVersionId. Mọi lần chọn giá phải ở transaction của caller để dấu đã tham chiếu cùng commit/rollback với lệnh lập khoản sau này.
- Ngoài gói mà không có giá: unitPrice/amount NULL; giá 0 được khai báo hợp lệ vẫn là giá đã xác định. Lỗi nguồn được trả lên caller, không giả thành giá rỗng hoặc miễn phí.
- Tiền sự kiện = lượng nguồn × đơn giá, làm tròn đồng sau nhân theo DongRounding. Không tự chuyển đơn vị hoặc đặt quantity=1; không tạo khoản/bảng/điều chỉnh, không thêm endpoint hoặc thay hợp đồng API.

## Kiểm thử

Test được viết trước logic. Khung service chỉ khai báo kiểu và ném UnsupportedOperationException:

1. `CostPricingServiceTest`: 12 ca đỏ runtime (2 failure/10 error), biên dịch thành công. Hai ca assertion lỗi nguồn thất bại vì khung chưa trả lỗi nguồn; không dùng lỗi testCompile làm bằng chứng đỏ.
2. `CostPricingTransactionIT`: 2 ca; control gọi ngoài transaction đã xanh, ca gọi trong transaction đỏ vì logic chưa cài.
3. Sau cài, bộ billing + mọi lớp `*ArchTest` + ModuleBoundaryTest: **148 test/30 lớp**, xanh.
4. IT billing + ApiContractIT + RecordGroupTriggerIT: **61 test/9 lớp**, xanh; gồm 2 IT mới với proxy Spring và nguồn đọc 004 thật, kiểm không tạo khoản/bảng khi thiếu giá.
5. IT nguồn B không sửa mã/test: `UnitPriceServiceIT`, `BR_M02_09_ContentAtDateIT`, `DBR_08_PriceVersionIT`: **24 test/3 lớp**, xanh.

Tổng phạm vi: **148 unit/arch + 85 IT**, 0 failure/error/skip. Hai lượt IT không trùng lớp. Kiểm XML mới sau mỗi lượt; Failsafe integration-test trả exit 0 khi đỏ vẫn được phát hiện. Spotless apply/check đúng ba file Java. Không chạy full verify; không sửa web/mobile nên không chạy kiểm hai phần đó.

Lệnh/log/XML nằm ở `.worktrees/.snapshots/010-pricing-tests.cjs` và các thư mục được ghi trong `010-pricing-{red,red-it,green,sources}-latest-path.txt`. Mỗi lần chạy dùng thư mục mới, giữ bằng chứng đỏ riêng với xanh. Ngày báo cáo theo Asia/Saigon; timestamp UTC trong JSON có thể còn là ngày 05/11.

## Còn lại

- T014 còn adapter nguồn lưu trú/bán trú, chia phí đúng nguồn/lịch, listener giá/phụ lục và tính lại/thông báo; chưa đóng US2.
- T008 còn chuỗi hồ sơ thật với A/B, chỉ mục theo nguồn và dữ liệu/quyền tệp mua hộ; chưa đóng Phase 2. Producer C/005 và consumer E/010 vẫn theo quyết định quantity đã chốt.
- 010 còn 17 phản hồi HTTP thành công thiếu schema; chưa tự bổ sung trường hoặc mở endpoint.
- 11/42 task sau tách T006a; toàn feature chưa hoàn tất. Không thêm migration, CFG hoặc thư viện; không có lệnh cài lại môi trường cho thay đổi này.
- Bản gỡ conflict 010 chuẩn bị ngày 05/11 chỉ khớp checkpoint cũ; khi leader cho đồng bộ, cần review lại thay đổi mới. Helper có kiểm HEAD/hash nên không được dùng để ghi đè dữ liệu đã đổi.

## Lệnh Bash để người dùng tự lưu checkpoint

```bash
cd '/c/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/010-foundation-next' &&
git add -- \
  backend/src/main/java/com/eldercare/backend/billing/internal/pricing/service/CostPricingService.java \
  backend/src/test/java/com/eldercare/backend/billing/CostPricingServiceTest.java \
  backend/src/test/java/com/eldercare/backend/billing/CostPricingTransactionIT.java \
  specs/010-incurred-costs/tasks.md \
  specs/010-incurred-costs/plan.md \
  specs/010-incurred-costs/contracts/module-interfaces.md \
  specs/010-incurred-costs/pricing-checkpoint-2026-11-06.md \
  specs/000-shared-foundation/glossary.md \
  docs/team-plan.md docs/progress.md &&
git diff --cached --check &&
git diff --cached --stat &&
git commit -m "feat(010): select effective cost prices and calculate event amounts" &&
git push origin HEAD:impl/010-foundation-next
```
