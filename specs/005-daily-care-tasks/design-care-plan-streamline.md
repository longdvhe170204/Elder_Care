# Thiết kế: tinh gọn lập và duyệt kế hoạch chăm sóc (Q-447)

Ngày: 2026-11-25. Chủ dự án chốt hướng A và cách hiệu lực "kết hợp" trong buổi trao đổi cùng ngày.

## 1. Mục đích

Điều dưỡng lập kế hoạch xong thì gửi Bác sĩ duyệt luôn. Người dùng không thấy khái niệm "phiên bản" và không phải đi nhiều bước.

Luồng hiện tại phải qua 4 thao tác: Lập Nháp → Lưu mục → Gửi duyệt kèm ngày hiệu lực → Duyệt. Sau đó kế hoạch còn chờ tới ngày hiệu lực. Màn hình có thêm danh sách phiên bản, so sánh phiên bản và Thu hồi duyệt. Với người dùng, luồng này quá dài.

## 2. Mục tiêu, không làm, phản mục tiêu

**Mục tiêu**
- Mỗi người cao tuổi có **một kế hoạch** trên giao diện, gồm kế hoạch hiện hành và tối đa một bản sửa đang xử lý.
- Điều dưỡng chỉ cần 2 thao tác: "Sửa kế hoạch" (hoặc "Lập kế hoạch" ở lần đầu) và "Gửi Bác sĩ duyệt".
- Không nhập ngày hiệu lực. Hệ thống tự đặt ngày theo quy tắc ở mục 4.3.

**Không làm**
- Không đổi phần lập công việc ca, kiểm tra công việc, quy tắc kết quả (nước, ăn kém), checklist.
- Không đổi người lập và người duyệt. Điều dưỡng vẫn lập, người có quyền Duyệt kế hoạch chăm sóc (mặc định Bác sĩ) vẫn duyệt. Q-438 (Bác sĩ tự lập và Ban hành) vẫn gác lại.
- Không đổi form thỏa thuận lựa chọn có rủi ro.

**Phản mục tiêu.** Các trường hợp sau coi là hỏng, dù chức năng vẫn chạy:
- Ca đang chạy trong ngày bị báo "lệch kế hoạch" vì kế hoạch đổi giữa ngày.
- Mất dấu vết ai lập, ai duyệt, lúc nào, nội dung trước và sau.
- Có lúc ca không có căn cứ để lập công việc, vì bản sửa đang chờ duyệt mà bản cũ đã mất.

## 3. Các hướng đã cân nhắc

| Hướng | Mô tả | Đánh đổi | Chọn |
|---|---|---|---|
| A. Bỏ phiên bản trên giao diện | Dữ liệu vẫn lưu theo bản. Giao diện chỉ hiện "kế hoạch hiện hành" và "bản sửa" | Phần lập công việc ca, kiểm tra và nhật ký giữ nguyên. Backend vẫn còn khái niệm bản | **Chủ dự án chọn** |
| B. Bỏ hẳn trong dữ liệu | Một bản ghi, sửa đè | Lúc chờ duyệt ca không có căn cứ. Công việc mất liên kết tới mục. Phải sửa lại phần lập công việc ca | Bỏ |

Thời điểm có hiệu lực sau khi duyệt:

| Cách | Đánh đổi | Chọn |
|---|---|---|
| 1. Luôn hiệu lực ngay | Phải sửa phần kiểm tra công việc để việc đã lập theo bản cũ vẫn phân lại được, kể cả khi duyệt 2 lần trong ngày | Bỏ |
| 2. Luôn từ ngày mai | Không sửa phần lập công việc ca, nhưng người mới vào phải chờ hôm sau mới có kế hoạch | Bỏ |
| 3. Kết hợp | Người chưa có kế hoạch thì hiệu lực ngay. Người đã có thì bản sửa áp dụng từ ngày mai | **Chủ dự án chọn** |

## 4. Thiết kế đã chốt

### 4.1 Trạng thái nội bộ và tên hiển thị

Backend giữ bảng `care_plan` theo bản, giữ các trạng thái hiện có. Giao diện không hiện số phiên bản. Trạng thái hiển thị như sau:

| Nội bộ | Giao diện hiện |
|---|---|
| DRAFT | "Đang soạn" (lần đầu) hoặc "Bản sửa đang soạn" |
| PENDING_APPROVAL | "Chờ Bác sĩ duyệt" |
| PENDING_EFFECT | "Đã duyệt, áp dụng từ dd/mm" |
| EFFECTIVE | Kế hoạch hiện hành, không có nhãn riêng |
| EXPIRED, CANCELLED | Không hiện (chỉ còn trong nhật ký) |

### 4.2 Lệnh và chuyển trạng thái

| Lệnh | Từ → đến | Ai | Điều kiện, ghi chú |
|---|---|---|---|
| Lập kế hoạch / Sửa kế hoạch | (chưa có) → DRAFT | Điều dưỡng | Lần đầu thì bản rỗng. Đã có thì **tự sao chép** bản mới nhất còn dùng (PENDING_EFFECT nếu có, không thì EFFECTIVE), không còn ô tick "Sao chép" |
| Lưu nháp | DRAFT | Điều dưỡng | Như PUT items, PUT risk-agreements hiện tại |
| **Gửi Bác sĩ duyệt** | DRAFT → PENDING_APPROVAL | Điều dưỡng | Lưu mục rồi gửi trong một lần bấm. **Không có ngày hiệu lực.** Bắt buộc lý do sửa khi đã có kế hoạch. Cần ít nhất 1 mục, như hiện nay |
| Rút lại | PENDING_APPROVAL → DRAFT | Người lập | Lệnh mới, không bắt buộc lý do. Người lập sửa tiếp được |
| Duyệt | PENDING_APPROVAL → EFFECTIVE hoặc PENDING_EFFECT | Người có quyền duyệt | Theo 4.3. Tự duyệt vẫn bắt buộc lý do và ghi "tự duyệt" |
| Trả lại | PENDING_APPROVAL → DRAFT | Người có quyền duyệt | Bắt buộc lý do. Người lập được báo, như hiện nay |
| Bỏ bản sửa | DRAFT → CANCELLED | Người lập hoặc người có quyền duyệt | Bắt buộc lý do, như CANCEL hiện nay |
| ~~Thu hồi duyệt~~ | — | — | Bỏ trên giao diện và trong `allowedCommands`. Thay bằng việc sửa tiếp (4.4) |

Tại mỗi thời điểm vẫn chỉ có tối đa một bản DRAFT hoặc PENDING_APPROVAL cho mỗi người cao tuổi.

### 4.3 Ngày có hiệu lực khi Duyệt

- Người cao tuổi **chưa có bản EFFECTIVE**: bản vừa duyệt chuyển thẳng sang **EFFECTIVE**, `effective_from` = hôm nay. Phát `CarePlanChanged` ngay trong lệnh, gửi cùng nhóm người nhận như `CarePlanActivation`.
- **Đã có bản EFFECTIVE**: bản vừa duyệt chuyển sang **PENDING_EFFECT**, `effective_from` = ngày mai. Lịch chạy `CarePlanActivation` sẽ áp dụng như hiện nay.

### 4.4 Sửa tiếp khi đã có bản "áp dụng từ ngày mai"

Bản PENDING_EFFECT không còn chặn việc lập bản sửa mới. "Sửa kế hoạch" sẽ sao chép từ bản PENDING_EFFECT. Khi bản sửa mới được duyệt, bản PENDING_EFFECT cũ chuyển sang CANCELLED (`end_reason` = thay bằng bản sau), và bản mới nhận `effective_from` = ngày mai.

### 4.5 Giao diện web (màn Kế hoạch chăm sóc)

Bố cục sau khi chọn người cao tuổi:

```
Kế hoạch chăm sóc — Nguyễn Thị Lan
[!] Bản sửa chờ Bác sĩ duyệt · gửi 25/11 10:05      (hoặc "Đã duyệt, áp dụng từ 26/11")
Người lập · Người duyệt · Hiệu lực từ …

Mục công việc            (chỉ xem: kế hoạch hiện hành)
Thỏa thuận rủi ro        (chỉ xem)
                                   [Sửa kế hoạch]
```

Khi đang soạn:
- Form mục công việc và form thỏa thuận như hiện nay.
- Ô "Lý do sửa" (bắt buộc khi đã có kế hoạch).
- Các nút: **[Lưu nháp] [Gửi Bác sĩ duyệt] [Bỏ bản sửa]**.

Khi chờ duyệt:
- Người lập thấy nút **[Rút lại]**.
- Bác sĩ thấy nút **[Duyệt] [Trả lại]**, ô Lý do và nội dung bản sửa. Bên cạnh có khu "Thay đổi so với hiện hành", dùng lại API compare đã có, chỉ hiện khi đã có kế hoạch hiện hành.

Bỏ khỏi giao diện:
- danh sách "Lịch sử phiên bản",
- thẻ "So sánh phiên bản" (dạng chọn hai bản),
- ô tick "Sao chép phiên bản hiệu lực",
- ô "Ngày hiệu lực đề xuất",
- nút "Thu hồi duyệt",
- mọi chữ "Phiên bản N".

### 4.6 Mobile

Màn `care_plans_screen.dart` chỉ để xem. Đổi "Kế hoạch lần N, hiệu lực từ …" thành "Hiệu lực từ …". Không thêm lệnh nào.

### 4.7 Giữ nguyên

- Nhắc người duyệt (CFG-M15-05) và báo Quản lý viện (CFG-M15-06).
- Thông báo xem xét kế hoạch (BR-M04-20).
- Nhật ký từng lệnh.
- `effectiveOn` theo ngày và phần lập công việc ca.

## 5. Ràng buộc

- Dữ liệu cũ có thể đang có bản PENDING_EFFECT với `effective_from` xa hơn ngày mai. Bản đó vẫn áp dụng đúng ngày đã đặt, không cần migration.
- Quyền FR-049 không đổi. Lệnh Rút lại dùng quyền của người lập (UPDATE trên bản do chính mình lập).
- Sửa spec 005: FR-003 (bảng chuyển trạng thái), FR-004 (bản mở), User Story 1, BF-01 bước 16 và BF-08 bước 6. Ghi thành **Q-447** trong `docs/business/nghiep-vu.md`.

## 6. Phần để người làm tự quyết

- Gộp "lưu mục rồi gửi" bằng một endpoint mới hay bằng hai lời gọi liên tiếp. Nếu dùng hai lời gọi, PUT items cần trả ETag mới.
- Cách đặt tên và hiển thị các mã lỗi mới (vd. bỏ `INVALID_EFFECTIVE_DATE` khỏi SUBMIT).
- Sửa hay thay các test FR-003 và BR-17 cho khớp bảng chuyển mới.

## 7. Các điểm em tự quyết (chủ dự án kiểm tra)

1. Vẫn bắt buộc lý do sửa khi đã có kế hoạch, để Bác sĩ biết vì sao sửa.
2. Thêm lệnh **Rút lại** (chờ duyệt → soạn tiếp) thay cho việc phải Hủy rồi lập lại.
3. Bản PENDING_EFFECT không chặn việc sửa tiếp. Bản sau thay bản trước (4.4).
4. Bác sĩ được xem "Thay đổi so với hiện hành", dùng lại API compare có sẵn, thay cho màn so sánh phiên bản.
