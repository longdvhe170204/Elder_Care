/// Nhãn loại bản ghi ghi khi mất mạng (`OfflineHandler.recordType` của từng module), cùng chữ với web
/// (`features/pending-review`). Loại chưa có nhãn thì hiện mã loại; module thêm nhãn khi cài bản ghi ngoại tuyến.
const _labels = {'M07.KET_QUA_LIEU': 'Kết quả liều thuốc'};

String recordTypeLabel(String code) => _labels[code] ?? code;
