# Lưu giữ chi phí hiện có — checkpoint 2026-11-05

Nhánh `impl/010-foundation-next`, nền commit `8842c8b2`. Dùng worktree đã có, không tạo thư mục spec hoặc worktree mới. Không commit, push hoặc đồng bộ nhánh 010 trong lượt này. Việc ghép develop đang chờ commit nằm riêng ở nhánh 008.

## Đã cài và kiểm

Tách **T008b** từ T008 để nghiệm thu ba provider có dữ liệu thật:

| Loại | Mốc tính hạn | Bản ghi trỏ tới được trả về |
| --- | --- | --- |
| M11.COST_PERIOD | Ngày tạo kỳ; CFG-M15-04 | Bảng của kỳ, khoản thuộc kỳ hoặc trỏ kỳ gốc |
| M11.COST_STATEMENT | `ResidentApi.finalStatusAt`; CFG-M15-03 | Khoản thuộc bảng |
| M11.COST_ITEM | `ResidentApi.finalStatusAt`; CFG-M15-03 | Khoản điều chỉnh trỏ khoản gốc |

`CostRetention` nằm trong persistence, đăng ký qua `M11GovernanceConfig`. Hồ sơ chưa kết thúc, thiếu mốc cuối hoặc mốc đúng cutoff không thành ứng viên hết hạn. Không lấy ngày tạo khoản thay mốc hồ sơ. Tham chiếu gồm cả bản ghi chưa hết hạn. Truy vấn phân biệt PERIOD/STATEMENT dù cùng bảng SQL. Xóa dùng kết nối retention, tham gia transaction của job; không tự xóa thêm dữ liệu để né kiểm chuỗi. Lịch sử/nhật ký đi kèm thuộc cơ chế bảng chung của A, không gọi trực tiếp API dành riêng cho job.

Không thêm migration, thư viện, CFG, API công khai hoặc provider rỗng. Không sửa mã của A/B/C/D.

## Bằng chứng kiểm thử

- Đỏ trước khi cài: `CostRetentionIT` 9 ca, 8 lỗi thiếu provider và 1 assertion rollback thất bại do thiếu provider; biên dịch thành công. Không dùng lỗi biên dịch làm bằng chứng test nghiệp vụ đỏ.
- Lượt sau sửa phát hiện và sửa hai lỗi mã mới: JDBC xuất hiện trong config, và repository `final` khiến Spring không tạo proxy. Giữ nguyên test kiến trúc, assertion và kiểm transaction.
- Lượt cuối: **106 unit/arch, 62 IT, 0 failure/error/skip**, Maven exit 0; XML Surefire/Failsafe được kiểm lại, không chỉ dựa vào exit của `failsafe:integration-test`. `CostRetentionIT` **9/9**.
- IT liên quan: `CostGovernanceIT`, `CostJobCatchUpIT`, `CostLockIT`, `CostSchemaIT`, `CostSharedRowsIT`, `ExpiredDataPurgeIT`. Có Spotless apply/check đúng ba file Java thay đổi. Không chạy full `verify`.
- Lệnh và XML/log cục bộ: `.worktrees/.snapshots/010-retention-tests.cjs`, con trỏ `.worktrees/.snapshots/010-retention-latest-path.txt`. Giữ riêng lượt đỏ ban đầu và lượt xanh cuối; file green được ghi đè khi chạy lại, không có bản log đầy đủ của hai lượt sửa lỗi trung gian.

## T008 còn mở; giới hạn cần phối hợp

1. **A/000 + B/001 + E/010:** `resident/internal/retention/service/ResidentRetention.referencesTo` hiện chỉ trả loại con của 001 và hồ sơ kế tiếp; chưa kéo bảng/khoản 010 vào chuỗi hồ sơ. Khi nhật ký/khoản bị giữ lại, phải bảo đảm hồ sơ và mốc cuối không bị xóa trước. Kiểm thử đang dùng fake ResidentApi và kiểm job chung với dữ liệu riêng của test A, chưa nghiệm thu chuỗi hồ sơ thật 001 → 010. Cần thống nhất cách khai báo tham chiếu liên module qua API công khai trước khi mở luồng chi phí thật; E không đọc bảng riêng 001 hoặc tự sửa provider của B.
2. **A/chủ nguồn + E/010:** chỉ mục `cost_source_index` phải xóa theo nguồn. Khóa hiện là bộ `(source_type, source_id, billing_unit)`, trong khi Candidate/DataRef dùng `long id`; chưa có ánh xạ định danh/hợp đồng nối việc nguồn bị loại bỏ. Không băm khóa thành id hoặc tự đổi SPI/schema để đánh xong. Nối và kiểm cùng T010/T021 sau khi hợp đồng được review.
3. **E/010:** metadata/phạm vi/retention đề nghị mua hộ, quyết định và quyền tệp làm cùng record/tệp ở T025–T027. Không tạo provider tương lai trả rỗng. Liên kết tới giao dịch 017 phải kiểm khi 017 có dữ liệu thật.

Vì vậy chỉ T008b được đánh xong. Parent T008, Phase 2 và toàn feature 010 chưa hoàn tất; 10/41 dòng task được đánh `[x]` sau tách task.

## Quyết định lượng tính phí

Đã chuyển nội dung người dùng chốt sang [care-quantity-decision-2026-11-05.md](care-quantity-decision-2026-11-05.md), cập nhật plan, hợp đồng và hai dòng chờ E/C. Không còn yêu cầu người dùng gửi lại quyết định; producer C/005 và consumer E/010 vẫn cần cài và kiểm toàn chuỗi.

## Lệnh Bash để người dùng tự lưu checkpoint

Chỉ chạy sau khi xem diff, ở đúng nhánh 010; lệnh dưới không hoàn tất merge đang chờ của 008:

```bash
cd '/c/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/010-foundation-next' &&
git status --short &&
git diff --check &&
git add -- \
  backend/src/main/java/com/eldercare/backend/billing/internal/governance/config/M11GovernanceConfig.java \
  backend/src/main/java/com/eldercare/backend/billing/internal/governance/persistence/CostRetention.java \
  backend/src/test/java/com/eldercare/backend/billing/CostRetentionIT.java \
  specs/010-incurred-costs/tasks.md \
  specs/010-incurred-costs/plan.md \
  specs/010-incurred-costs/data-model.md \
  specs/010-incurred-costs/contracts/module-interfaces.md \
  specs/010-incurred-costs/care-quantity-decision-2026-11-05.md \
  specs/010-incurred-costs/retention-checkpoint-2026-11-05.md \
  specs/000-shared-foundation/glossary.md \
  docs/team-plan.md docs/progress.md &&
git diff --cached --stat &&
git commit -m "feat(010): add retention providers for existing cost records" &&
git push -u origin HEAD:impl/010-foundation-next
```
