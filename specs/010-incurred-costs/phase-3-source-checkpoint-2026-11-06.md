# Checkpoint Phase 3 — nguồn tính phí, 06/11/2026

Nhánh impl/010-foundation-next, nền 09d569c0. **T010 hoàn tất; Phase 3 chưa hoàn tất. Phase 2/T008 giữ nghiệm thu mở theo quyết định người dùng**, lỗi 004 bàn giao B. Không commit/push/merge/đổi nhánh hoặc tạo worktree mới.

## Phần đã cài

- T010: ba listener sự kiện generic/chăm sóc/thuốc ghi chỉ mục và dấu chống lặp trong cùng transaction. Giữ nguồn gốc, ngày thực hiện, lượng hiện hành; phân biệt hai thuốc/mỗi đêm cùng nguồn. Sự kiện ban đầu phát lại không khôi phục nguồn đã hủy/đính chính. Không tự tạo bảng, khoản hoặc điều chỉnh.
- V010_03 mở cột lượng nguồn/khoản lên DECIMAL(12,3), lịch sử dùng @Digits(9,3), giữ 0,125 của 006; lượng vượt precision bị từ chối trước ghi. Không sửa migration đã merge, tiền VND/hệ số không đổi.
- Phần truy vấn chỉ mục của T011: lọc đúng người/loại/khoảng ngày hai đầu, trả nguồn cả khi đã có khoản; nối đúng khoản hiệu lực theo bộ nguồn và đơn vị. Khoản hủy không che khoản mới hoặc nhân đôi dòng; trả lượng/cờ hủy/phiên bản hiện hành. Đây là kiểu nội bộ, chưa phải toàn bộ phản hồi HTTP.
- Đổi api đã được chủ dự án duyệt: BillingSource/BillingSourceView thêm billingUnit; CreateCostsFromEvents thêm sourceUnits, ba kiểu chọn loại trừ nhau. unit giữ nghĩa đơn vị sử dụng/hiển thị. Đã cài BillingSourceSelection xác định đúng đơn vị hoặc trả lý do bỏ qua id mơ hồ/không có nguồn; chưa có endpoint/lệnh lập khoản đầy đủ.

## Kiểm thử

- T010 có các lượt đỏ runtime trước cài; giữ bằng chứng source/care/medication, hai thuốc cùng nguồn và rollback. Precision đỏ thật do 0,125 thành 0,13; guard từ chối lượng không biểu diễn được có 2 assertion failure trước sửa.
- Truy vấn T011: lượt đầu có lỗi fixture khoản hủy thiếu locked_at; sửa fixture đúng CHECK, chạy lại 6/6 đỏ do UnsupportedOperationException trước cài query. Phần chọn nguồn bổ sung có 7 unit đỏ runtime và 2 IT đỏ trước cài, 6 IT query cũ là control xanh. Không đổi schema/test nền để né lỗi.
- Hợp đồng chọn đơn vị: 8 ca có 3 assertion failure trước sửa YAML/DTO (5 control xanh). Hai lượt kiểm còn dùng DTO bytecode cũ có 157 ca/2 failure; đã đối chiếu javap và chạy clean để biên dịch lại, không lấy các lượt đó làm kết quả cuối.
- Kết quả cuối trong một lượt Maven hoàn tất: **164 unit/arch (33 lớp) + 97 IT (12 lớp)**; 0 failure/error/skip, Maven exit 0. Gồm billing, toàn bộ *ArchTest, ModuleBoundaryTest, ApiContractIT, RecordGroupTriggerIT và ExpiredDataPurgeIT. Spotless apply/check các file Java thay đổi qua. Lượt cuối có clean trước biên dịch, không chạy lại full verify toàn backend ở checkpoint này.
- Bằng chứng log/XML mới giữ riêng ở đường dẫn do .worktrees/.snapshots/010-source-selection-green-latest-path.txt trỏ tới; không cộng test XML cũ vào lượt cuối.

- Lệnh thử có cờ maven.test.failure.ignore=true đã bị kiểm duyệt tự động từ chối trước khi thực thi. Đã bỏ cờ và chạy hai lượt đỏ riêng; Maven exit 1/đếm lỗi được giữ nguyên trong bằng chứng. Không dùng cờ bỏ lỗi, không tắt/bỏ qua test.

## Phần chưa hoàn tất và bàn giao

- T009 còn các test lệnh thật: lập trùng/đồng thời, hàng loạt bỏ qua, giới hạn thời gian. T011 còn adapter ngày lưu trú/buổi bán trú, metadata nguồn và lệnh tạo bảng/khoản/giá. T012 còn controller và màn web. Không đánh xong bằng test chỉ mục/schema.
- C/005: đơn vị/loại kết quả tính phí và producer thật. D/006: đơn vị/mã vật phẩm, nguồn lần giao nhiều thuốc và precision tài liệu. B/004: metadata dịch vụ và cách chọn dịch vụ người thân ở lại. A/000: đăng ký 010 trong bộ sinh client web. Chi tiết source-metadata-handoff-2026-11-06.md và mục 8 team-plan; không tự sửa phần của họ.
- T021 consumer đính chính/tính lại chưa cài; T033 còn nghiệm thu producer và retention toàn chuỗi. Quyết định quantity/newQuantity vẫn giữ nguyên.

## Môi trường

- Kiểm chỉ đọc vào schema dev mặc định elder_care trả MySQL 1049 (schema không tồn tại); chưa xác minh schema dev thực tế hoặc migration đã chạy. Không tự dựng, migrate hoặc sửa cấu hình dev.
- Nếu dev thực tế đã chạy migration lớn hơn V010_03, áp dụng dòng “Ghi nhớ migration số nhỏ” mục 8: chạy một lần với spring.flyway.out-of-order=true hoặc dựng lại schema dev. Đây là hướng dẫn, chưa thực hiện. Cấu hình application.yml đang bật out-of-order; vẫn cần đối chiếu profile thực chạy.
- Không thêm thư viện pom.xml/package.json/pubspec.yaml hoặc CFG. Không cần cài lại dependency do thay đổi này. Giữ nguyên file retention-checkpoint-2026-11-05.md người dùng đang sửa.
