# Phase 5 — thay đổi hợp đồng và phần cần review, 07/11/2026

Người dùng giao E tự chọn cách triển khai hợp lý theo spec, không hỏi lại các lựa chọn thường lệ. Các thay đổi dưới đây đã cài và kiểm; đây là bàn giao review, không phải ghi nhận A đã duyệt. Không đổi nghiệp vụ hoặc spec.

| Phần | Cách triển khai | Căn cứ |
| --- | --- | --- |
| GET /costs/periods | Mảng CostPeriod, đúng các trường CostPeriodView đã công bố: id, month, fromDate, throughDate, status, finalizationDueDate, finalizedAt | FR-001/T019; schema phản hồi trước đây thiếu |
| CostItem.commandHistory | Mảng operation, actorId, at, reason tùy chọn. CHECK, UNCHECK, APPROVE, RETURN, CANCEL, FINALIZE. Ghi StatusHistoryApi cùng transaction; track M11.COST_ITEM/COMMAND, không tạo bảng lịch sử riêng | FR-018/R-15 |
| Tổng kỳ trước bằng 0 | Vẫn trả changeWarning và topThreeIncreases; bỏ increaseRatio tùy chọn. Màn giải thích mẫu số 0, không tạo phần trăm vô hạn, không thêm trường tiền ngoài hợp đồng | FR-032/BR-M11-07 |
| Client web | Đăng ký riêng 010 trong TRANSLATED, chạy npm run gen:api; không sửa tay file sinh | T019; frontend README |
| ApiContractIT | Đăng ký 010 theo T032, 12 operation đã cài, 17 pending còn ở story khác, checkOperations=true | Không bỏ assertion hoặc nới kiểm hợp đồng |

Public DTO CostItemView của E thêm commandHistory; đã sửa toàn bộ nơi tạo record hiện có. Mọi API nguồn C/D và common production giữ nguyên. A review phần đăng ký script/test chung; consumer review DTO và schema 010 trong PR. T032 toàn feature và gate nguồn thật T033 vẫn mở.

Bằng chứng: CostWorkflowIT 20, ApiContractIT 1; 9 test màn, toàn web 610 test, lint/build xanh. Xem ../phase5-checkpoint-2026-11-07.md. Không chạy verify; chưa commit/push/PR.