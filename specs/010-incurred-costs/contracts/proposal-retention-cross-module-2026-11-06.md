# Đề xuất nối chuỗi lưu giữ 001 → 010

**Trạng thái:** người dùng chốt 06/11 giữ phạm vi E, bàn giao để A review/cài. Chữ ký dưới đây vẫn là đề xuất, chưa thành API được công bố; chưa sửa API/job/test của A hoặc mã B. Phạm vi E: T008 là nền provider hiện có; tích hợp chuỗi liên module giữ mở ở 010 T033 (phân rã kỹ thuật 06/11, plan/tasks). Phạm vi nền cần mở rộng: 000 T070 (SPI), T071 (job), hồi quy T066; các task gốc của A đã được đánh xong, đây là nhu cầu nối liên module mới.

## Vấn đề có trong mã hiện tại

`RetentionProvider.referencesTo(long id)` chỉ nhận ID thuộc loại của chính provider. Khi dựng chuỗi, `ExpiredDataPurge.chain` chỉ gọi provider của loại bản ghi đang xét. `ResidentRetention.referencesTo` của B chỉ biết các loại con 001 và hồ sơ kế tiếp; không biết bảng chi phí 010.

Ví dụ: hồ sơ R đã đủ hạn theo CFG-M15-03; bảng S và khoản I của R vẫn phải giữ do nhật ký/đính chính chưa đủ hạn. Nếu xét R thành chuỗi riêng trước, các provider của E chưa có cơ hội báo S/I đang trỏ tới R. NFR-07, BR-M15-12/13 và 000 FR-051a yêu cầu giữ cả chuỗi cho tới khi mọi phần đủ hạn.

E không thể sửa việc này chỉ bằng đổi `CostRetention.referencesTo(long)`: hướng gọi hiện tại không hỏi E khi gốc là `M01.RESIDENT`. Đọc bảng riêng 001, thay bean B hoặc điều chỉnh thứ tự chạy provider không phải cách nối hợp đồng hợp lệ.

## Thay đổi đề xuất

### A/000: thêm overload mặc định, giữ chữ ký cũ

Trong `administration.retention.api.spi.RetentionProvider`:

```java
default Set<DataRef> referencesTo(DataRef target) {
    return dataType().equals(target.dataType()) ? referencesTo(target.id()) : Set.of();
}
```

Hàm cũ giữ nguyên. Provider hiện có không override hàm mới vẫn báo đúng tham chiếu của loại mình. Không đổi REST, DTO, CFG, schema SQL hoặc yêu cầu provider trả ứng viên của module khác.

Trong `ExpiredDataPurge.chain`, thay chỗ chỉ hỏi provider của target bằng hỏi mọi provider qua overload mới:

```java
for (RetentionProvider provider : byType.values()) {
    queue.addAll(provider.referencesTo(record));
}
```

Vẫn giữ tập `seen`, điều kiện mọi bản ghi phải là ứng viên hết hạn, kiểm phần đi kèm, xóa nguyên tử bằng RetentionDatabase và đếm mỗi bản ghi một lần. Không đổi thời hạn hoặc tự cho rằng tham chiếu cũng hết hạn.

### E/010: báo các bảng của một hồ sơ

Provider `M11.COST_STATEMENT` override overload mới:

- Target `M01.RESIDENT`: trả `M11.COST_STATEMENT` từ **bảng riêng E** `cost_statement`, lọc `kind='STATEMENT' AND resident_id=?`, giữ tất cả trạng thái và không lọc theo hạn.
- Target khác: gọi mặc định để giữ tham chiếu hiện có.

Chuỗi sau đó đi theo `M01.RESIDENT → M11.COST_STATEMENT → M11.COST_ITEM → khoản điều chỉnh`. Khoản luôn có statement_id; không thêm khoản thành nhánh ngang với bảng khiến thứ tự xóa vi phạm khóa ngoại. Bảng theo ID tăng ổn định; xóa theo thứ tự phụ thuộc do job cung cấp, không sắp lại ID khi delete.

Không cần sửa `ResidentRetention` hoặc API của B. B review ca hồ sơ thật; A review thay đổi SPI/job. E chỉ gọi API công khai, chỉ truy vấn/xóa dữ liệu của E.

## Kiểm trước khi đánh hoàn thành

1. Provider cũ không override overload mới giữ hành vi; target khác loại trả rỗng, loại mình gọi đúng hàm cũ.
2. Hồ sơ hết hạn nhưng S/I hoặc nhật ký/đính chính của chúng chưa hết hạn: giữ hồ sơ và toàn bộ dữ liệu chi phí.
3. Mọi phần hết hạn: xóa khoản điều chỉnh trước khoản gốc, khoản trước bảng, hồ sơ sau dữ liệu phụ thuộc; không trùng số đếm tóm tắt.
4. Lỗi giữa chuỗi: rollback cả chuỗi và giữ hành vi báo cáo gián đoạn hiện có.
5. Kiểm các provider hiện hữu và consumer 001/004/008/010, kiến trúc/Modulith/hợp đồng. Vì thay đổi nền 000 ảnh hưởng nhiều module, lệnh full `clean verify` thuộc gate theo AGENTS.md mục 3; không chỉ dùng bộ test riêng E để kết luận phần A đã xong.

## Phần đề xuất này chưa giải quyết

- Chỉ mục `cost_source_index` xóa theo bản ghi nguồn: khóa bộ ba không phải DataRef long. Cần A và chủ nguồn thống nhất cơ chế dọn liên kết kỹ thuật trong transaction nguồn bị loại bỏ và ánh xạ loại/ID nguồn; chưa thêm ID giả, băm khóa hoặc sửa event nguồn. Không tự coi nguồn đã hủy là nguồn đã bị loại bỏ.
- Metadata/phạm vi/retention/quyền tệp mua hộ làm cùng record/tệp thật ở T025–T027. Không tạo provider rỗng trước khi có dữ liệu.
- Do đó, duyệt và cài mở rộng trên chưa tự đóng T033 hoặc toàn feature 010: còn kiểm chuỗi thật/bảng chung, dọn chỉ mục theo nguồn và phần mua hộ. T008 chỉ nghiệm thu nền provider hiện có; T033 là gate trước hoàn tất feature/merge/vận hành.

## Quyền thực hiện

AGENTS.md mục 2.2 giới hạn phần E; mục 4 yêu cầu chủ dự án duyệt trước khi đổi API của module khác. Hiện chỉ viết đề xuất ở spec 010. Người dùng đã chọn giữ phạm vi E, bàn giao để A tự review/cài SPI/job/test và tài liệu nền; E chờ API được công bố để nối bản cài. Không tự gửi tin nhắn, mở PR, commit/push hay merge theo đề xuất này.
