# Đồng bộ develop vào 008 — 2026-11-05

## Trạng thái Git

Người dùng đã commit/push checkpoint ca179af1 lên origin/impl/008-integration. Đã fetch và chuẩn bị ghép origin/develop 2fd4cd8c (14 commit, PR108–110) vào đúng worktree008-integration bằng git merge --no-commit --no-ff. Không đổi nhánh, không tự commit/push. HEAD còn ca179af1; MERGE_HEAD là2fd4cd8c. Chỉ sau khi người dùng tự commit thì lịch sử nhánh mới ghi nhận đồng bộ hoàn tất.

Ảnh image.png và phần chèn ảnh trong integration-2026-11-04.md giữ ngoài index; SHA256 trước/sau khớp. Snapshot/diff ban đầu được giữ tại .worktrees/.snapshots/008-sync-20261105.89tNjZ. File API sinh có diff nội dung rỗng; đã đưa về bản index, chạy generator chuẩn sau ghép, không sửa tay client.

## Xung đột và quyết định đã chốt

Chỉ hai file conflict: docs/progress.md và docs/team-plan.md. Nhật ký giữ đủ mọi dòng của bản E và develop (kiểm lại219/204 dòng không rỗng, không mất dòng). Bảng chờ giữ nguồn CareStaffLookup đã cài/kiểm xanh, không khôi phục ghi chú E chưa cài; bỏ dòng bedRows cũ đã dùng startedAt trong mã hiện tại. Giữ chuyển chủ011 sangD và mọi việc mới của develop, gồm lỗi fixture StaffCommandsHttpIT còn cần rà ở chế độ song song.

Không có diff mã nguồn staffing/mobile008, nghiệp vụ13.5/24.2, spec008 hoặc HTTP008 do ghép develop. Các quyết định nhận đúng ca sau, nhận việc tồn theo giờ nhận thực tế, vòng đời hai cờ và quantity/newQuantity được giữ nguyên. Chưa thấy quyết định bị thay hoặc mất. Không tự đổi chữ ký API hay nghiệp vụ để hòa xung đột.

## Thay đổi từ develop

- A/002 thêm sessionIdleTimeoutSeconds trong UserInfo của GET/me và xử lý cảnh báo hết phiên/tạm dừng tự làm mới trên web. Giữ nguyên bản A; test phiên và hợp đồng chạy lại xanh.
- A/000 thêm it.forkCount, cách chia schema theo JVM; mặc định một JVM, hướng dẫn clean verify toàn kho dùng3JVM. Không có thư viện hoặc migration mới trong14commit này, không chạy cài lại hoặc đổi schema dev.
- CODEOWNERS/team-plan:011 nutrition trở vềD; E vẫn staffing/billing,008/010/015/017. Không thay phần việc E.

## Kiểm sau ghép

- Lượt đầu:53unit/arch xanh;126IT có1failure ở HandoverIT.FR_029_handoverChecklistFollowsPreparationFeedbackResubmissionAndConfirmation. Test đặt trước biên1giây, nhưng TestClockControl chạy tiếp theo đồng hồ thật và lượt kiểm mất6,154giây. Đọc schema test xác nhận CFG-M09-04 vẫn30phút, không sửa tham số.
- Sửa đúng fixture E: MockitoSpyBean BusinessTime, chỉ cố định now() tại các mốc trong test vòng đời; giữ assertion và biên1giây, không tăng ngưỡng hoặc đổi mã production. Mockito tự reset sau mỗi test. Spotless apply/check thực kiểm một file; chạy lại toàn HandoverIT37/37 xanh.
- Tổng bộ cuối:53unit/arch(23lớp) và126IT(12lớp),0failure/error/skip. XML cuối lấy11lớp đã xanh của lượt đầu và thay riêng XML HandoverIT bằng lượt kiểm lại37test; không cộng trùng hoặc xóa bằng chứng đỏ. AutoLogoutIT5/5,ApiContractIT1/1,StaffCommandsHttpIT5/5 thuộc bộ này. Chạy một JVM không chứng minh lỗi thứ tự của StaffCommandsHttpIT khi chạy song song đã được sửa.
- Web:generator chuẩn;485test/83file,lint/build xanh. App:227test,analyzer sạch. Không chạy verify toàn kho. Rawlog/XML và final-validation.json trong snapshot nêu trên.

## Còn lại

008 vẫn57/62task: T022b,T030,T043b,T049b,T044b. Chưa có ETag/hạn cache ở nền A; C còn callback005/claim; D cònUS13; chưa có số liệu nghiệm thu thật. Các giới hạn này không được đóng bằng việc đồng bộ hoặc test fake. Người dùng tự kiểm diff và tạo merge commit/push; chưa mở PR hay merge vào develop.

## Review PR #112 và chuẩn bị đồng bộ 010 — 2026-11-05

Người dùng đã commit/push merge 4ef94b87; origin/develop vẫn 2fd4cd8c. Trạng thái chuẩn bị merge ở các mục trên là lịch sử, không còn MERGE_HEAD. Đã tạo PR nháp https://github.com/longdvhe170204/Elder_Care/pull/112 (impl/008-integration → develop), chưa merge. Các quyền/nguồn/checklist đã chốt giữ nguyên; diff mã staffing, mobile008, nghiệp vụ và hợp đồng so checkpoint trước đồng bộ không thay bởi merge.

Nhánh 010 đã push 70697650, 3 commit riêng và thiếu 12 commit develop. Mô phỏng bằng git merge-tree, không đổi nhánh/index/worktree: ghép develop hiện tại chỉ conflict docs/progress.md; ghép sau checkpoint 008 còn conflict docs/team-plan.md. Chưa thấy conflict mã nguồn. Khi gỡ, giữ mọi dòng nhật ký, toàn bộ việc chờ 008 và 010; giữ quyết định quantity/newQuantity, không phục hồi ghi chú T050 chờ API hoặc E chưa gửi quyết định. Ghi chú T050 cũ trong bảng chờ đã sửa; việc review A/C vẫn giữ.

Không sửa production hoặc chạy lại test trong lượt review tài liệu này; bằng chứng ở mục Kiểm sau ghép vẫn là 53 unit/arch + 126 IT, 485 web, 227 Flutter của checkpoint đã kiểm. Chưa nghiệm thu 5 task mở của 008 hoặc chuỗi retention thật 001 → 010. Người dùng tự commit/push ba file review mới, tự quyết định Ready for review và merge; AI không commit, push hoặc merge.

### Bản gỡ xung đột 010 đã chuẩn bị

Snapshot cục bộ `.worktrees/.snapshots/pr-sync-review-20261105.tWHlf7` có hai file `resolved-progress.md`, `resolved-team-plan.md` và manifest SHA256. Đã kiểm mọi dòng không rỗng của hai nhật ký đều có trong kết quả. Team-plan giữ việc retention 010, mốc billingStopDate cần B/E rà, các việc 008 chờ A/C/D và quyết định quantity; không khôi phục hai dòng 008 đã được nhánh 008 xử lý/xóa. Cách này là bản chuẩn bị, chưa ghi vào nhánh 010 hoặc Git index.

Sau khi người dùng commit/push ba file review của 008 và PR #112 được merge, tự chạy trong Bash:

```bash
cd '/c/Users/PHAN THANH LONG/Capstone project/Elder_Care/.worktrees/010-foundation-next' &&
git fetch origin &&
git merge --no-commit --no-ff origin/develop
```

Nếu Git báo đúng hai file tài liệu conflict, chạy riêng khối sau. Helper chỉ áp dụng khi HEAD 010, hai phía của conflict và nội dung chưa sửa tay khớp bản đã review; nếu develop thay mã hoặc nghiệp vụ ngoài ba file tài liệu đã chuẩn bị, helper dừng để kiểm lại. Helper không commit, push hoặc tự chạy merge:

```bash
node ../.snapshots/apply-010-doc-resolution-20261105.cjs --apply &&
git add -- docs/progress.md docs/team-plan.md &&
git diff --cached --check &&
git diff --cached --stat
```

Chưa nên tạo commit đồng bộ 010 trước khi kiểm thử phần bị ảnh hưởng sau merge thật. Mô phỏng không thay kết quả compile/test. Lượt hiện tại chỉ kiểm helper ở chế độ `--check`, chưa chạy `--apply`.
