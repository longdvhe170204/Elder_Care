// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/007-health-alert-incident/contracts (Elder Care – Feature 007 (sức khỏe, cảnh báo, sự cố))
import { request } from './client.js'

/** Danh mục chỉ số, kèm khoảng hợp lệ hiện hành từ CFG-M06-03 và dấu chuaCoKhoangHopLe (FR-001) */
export const getVitalSignCatalog = (params) => request('GET', '/vital-sign-catalog', params)

/** Thêm chỉ số (Quản lý viện); khoảng hợp lệ thêm ở CFG-M06-03 (FR-001, FR-002) */
export const postVitalSignCatalog = (params) => request('POST', '/vital-sign-catalog', params)

/** Sửa tên, đơn vị, nhóm sinh hiệu (Quản lý viện) (FR-002) */
export const putVitalSignCatalogById = (params) => request('PUT', '/vital-sign-catalog/{id}', params)

/** Ngừng hiệu lực chỉ số (FR-002) */
export const postVitalSignCatalogByIdDeactivate = (params) => request('POST', '/vital-sign-catalog/{id}/deactivate', params)

/** Danh mục quy tắc xu hướng (FR-039) */
export const getTrendRules = (params) => request('GET', '/trend-rules', params)

/** Bật/tắt, đổi mức (Quản lý viện; lý do bắt buộc) (FR-039, BR-M05-04, BR-M15-04) */
export const putTrendRulesByCode = (params) => request('PUT', '/trend-rules/{code}', params)

/** Danh mục loại sự cố (mức mặc định, dấu không thuộc sức khỏe) (FR-042) */
export const getIncidentTypes = (params) => request('GET', '/incident-types', params)

/** Thêm loại sự cố (Quản lý viện) (FR-042) */
export const postIncidentTypes = (params) => request('POST', '/incident-types', params)

/** Sửa loại sự cố, gồm dấu khongThuocSucKhoe (nhật ký trước/sau) (FR-042) */
export const putIncidentTypesById = (params) => request('PUT', '/incident-types/{id}', params)

/** Danh mục loại diễn biến, có dấu bienPhapHoiSuc (FR-048a, BR-M05-20) */
export const getProgressionTypes = (params) => request('GET', '/progression-types', params)

/** Thêm loại diễn biến (Quản lý viện) (FR-048a) */
export const postProgressionTypes = (params) => request('POST', '/progression-types', params)

/** Lịch đo của người cao tuổi (Bác sĩ, Điều dưỡng, Quản lý viện; Trưởng tầng trong phạm vi) (FR-003, FR-080) */
export const getNguoiCaoTuoiByNctIdLichDo = (params) => request('GET', '/nguoi-cao-tuoi/{nctId}/lich-do', params)

/** Tạo lịch đo (Bác sĩ) (FR-003, FR-004, FR-006) */
export const postNguoiCaoTuoiByNctIdLichDo = (params) => request('POST', '/nguoi-cao-tuoi/{nctId}/lich-do', params)

/** Các mốc đo dự kiến trong khoảng (tính, không lưu) (FR-004) */
export const getLichDoByIdMocDo = (params) => request('GET', '/lich-do/{id}/moc-do', params)

/** Thay đổi lịch đo (Bác sĩ; lý do bắt buộc) (FR-005, FR-006) */
export const postLichDoByIdThayDoi = (params) => request('POST', '/lich-do/{id}/thay-doi', params)

/** Ngừng lịch đo (Bác sĩ; lý do bắt buộc) (FR-006) */
export const postLichDoByIdNgung = (params) => request('POST', '/lich-do/{id}/ngung', params)

/** Lịch sử chỉ số (giá trị hiện hành, giá trị gốc nếu đã đính chính, người đo, ngưỡng áp dụng, kết quả so) (FR-010, FR-080, BR-M06-06) */
export const getNguoiCaoTuoiByNctIdChiSo = (params) => request('GET', '/nguoi-cao-tuoi/{nctId}/chi-so', params)

/** Ghi chỉ số ngoài lịch (Nhân viên chăm sóc, Điều dưỡng trong phạm vi) (FR-010, FR-011, FR-012, FR-013, FR-015, BR-M06-03, BR-M06-04, BR-M04-12) */
export const postNguoiCaoTuoiByNctIdChiSo = (params) => request('POST', '/nguoi-cao-tuoi/{nctId}/chi-so', params)

/** Ghi lần đo lại cho bản ghi "chờ đo lại" (FR-016, FR-017, FR-018, BR-M06-04) */
export const postBanGhiChiSoByIdDoLai = (params) => request('POST', '/ban-ghi-chi-so/{id}/do-lai', params)

/** Danh sách người cao tuổi quá CFG-M06-01 chưa có ngưỡng cá nhân (Bác sĩ) (FR-025, BR-M06-02) */
export const getChiSoChuaCoNguongCaNhan = (params) => request('GET', '/chi-so/chua-co-nguong-ca-nhan', params)

/** Phiên bản ngưỡng (mặc định hoặc của một người), cả lịch sử (FR-020, FR-022, FR-080) */
export const getNguong = (params) => request('GET', '/nguong', params)

/** Thiết lập ngưỡng (Bác sĩ) (FR-020, FR-021, FR-022, FR-023, BR-M06-01) */
export const postNguong = (params) => request('POST', '/nguong', params)

/** Hủy phiên bản Chờ hiệu lực (Bác sĩ; lý do) (FR-022) */
export const postNguongByIdHuy = (params) => request('POST', '/nguong/{id}/huy', params)

/** Ngừng ngưỡng cá nhân Hiệu lực (Bác sĩ; lý do); từ đó so theo ngưỡng mặc định (FR-022) */
export const postNguongByIdNgung = (params) => request('POST', '/nguong/{id}/ngung', params)

/** Danh sách cảnh báo theo phạm vi (lọc trạng thái, mức, tầng, người phụ trách, leo thang tối đa) (FR-027, FR-034, FR-036, FR-080) */
export const getCanhBao = (params) => request('GET', '/canh-bao', params)

/** Tạo cảnh báo thủ công (Trưởng tầng, Bác sĩ, Điều dưỡng; mô tả bắt buộc) (FR-026, FR-028, FR-030, BR-M05-02) */
export const postCanhBao = (params) => request('POST', '/canh-bao', params)

/** Chi tiết cảnh báo, nguồn, lịch sử, lần nhắc, lenhDuocPhep[] (FR-027, FR-033) */
export const getCanhBaoById = (params) => request('GET', '/canh-bao/{id}', params)

/** Tiếp nhận (FR-029, FR-036, BR-M05-01) */
export const postCanhBaoByIdTiepNhan = (params) => request('POST', '/canh-bao/{id}/tiep-nhan', params)

/** Giao người phụ trách (Quản lý viện khi là người phụ trách; ghi chú bắt buộc) (FR-029, FR-031, FR-036) */
export const postCanhBaoByIdGiaoNguoiPhuTrach = (params) => request('POST', '/canh-bao/{id}/giao-nguoi-phu-trach', params)

/** Bắt đầu xử lý (FR-029, FR-036) */
export const postCanhBaoByIdBatDauXuLy = (params) => request('POST', '/canh-bao/{id}/bat-dau-xu-ly', params)

/** Nâng / hạ mức (lý do bắt buộc; hạ từ Khẩn cấp chỉ Bác sĩ) (FR-029, FR-038a, BR-M05-03) */
export const postCanhBaoByIdDoiMuc = (params) => request('POST', '/canh-bao/{id}/doi-muc', params)

/** Đóng (kết quả bắt buộc; lý do với cảnh báo công việc Bắt buộc quá hạn; nguy kịch cần kết quả xác nhận lại và lựa chọn) (FR-029, FR-050d, BR-M04-06) */
export const postCanhBaoByIdDong = (params) => request('POST', '/canh-bao/{id}/dong', params)

/** Chuyển sự cố (từ Đang xử lý); sự cố liên kết mức mặc định bằng mức cảnh báo (FR-029, FR-040) */
export const postCanhBaoByIdChuyenSuCo = (params) => request('POST', '/canh-bao/{id}/chuyen-su-co', params)

/** Kích hoạt khẩn cấp từ cảnh báo (Mới, Leo thang, Đã tiếp nhận); bắt buộc trực tuyến (FR-029, FR-047, FR-047a, BR-M05-06, BR-M05-13) */
export const postCanhBaoByIdKichHoatKhanCap = (params) => request('POST', '/canh-bao/{id}/kich-hoat-khan-cap', params)

/** Danh sách sự cố theo phạm vi (lọc trạng thái, mức, loại, tầng, người cao tuổi) (FR-045, FR-080) */
export const getSuCo = (params) => request('GET', '/su-co', params)

/** Ghi nhận sự cố (mọi nhân viên có quyền "T"; ngoại lệ ngoài phạm vi 002 FR-044a); bắt buộc trực tuyến (FR-040, FR-041, FR-042, FR-043, FR-047, FR-051, FR-060, BR-M05-06, BR-M05-07, BR-M05-10, BR-M05-17, BR-M05-18) */
export const postSuCo = (params) => request('POST', '/su-co', params)

/** Hồ sơ sự cố (nội dung, diễn biến, đổi mức, người xử lý, người được thông báo từ 009, lenhDuocPhep[]) (FR-040, FR-048b) */
export const getSuCoById = (params) => request('GET', '/su-co/{id}', params)

/** Tiếp nhận xử lý (Mới → Đang xử lý) (FR-044, FR-045) */
export const postSuCoByIdTiepNhan = (params) => request('POST', '/su-co/{id}/tiep-nhan', params)

/** Giao người xử lý (Mới → Đang xử lý; hoặc giao lại khi Đang xử lý, Đang theo dõi) (FR-045, FR-045a, FR-044) */
export const postSuCoByIdGiaoNguoiXuLy = (params) => request('POST', '/su-co/{id}/giao-nguoi-xu-ly', params)

/** Tham gia xử lý sự cố Khẩn cấp đang mở (không tạo sự cố mới) (FR-041, FR-048b) */
export const postSuCoByIdThamGiaXuLy = (params) => request('POST', '/su-co/{id}/tham-gia-xu-ly', params)

/** Ghi diễn biến (đánh giá, hành động, biện pháp hồi sức, thông báo, theo dõi, gọi hỗ trợ/cấp cứu, báo cơ quan y tế) (FR-045, FR-048a, FR-048b, FR-066, BR-M05-13, BR-M05-20) */
export const postSuCoByIdDienBien = (params) => request('POST', '/su-co/{id}/dien-bien', params)

/** Nâng / hạ mức (lý do; hạ từ Khẩn cấp chỉ Bác sĩ; nâng lên Khẩn cấp kích hoạt mục G; hạ khỏi Khẩn cấp dừng nhắc gọi điện) (FR-044, FR-045, BR-M05-22) */
export const postSuCoByIdDoiMuc = (params) => request('POST', '/su-co/{id}/doi-muc', params)

/** Chuyển theo dõi (cần ít nhất một diễn biến đánh giá và một diễn biến xử lý) (FR-045) */
export const postSuCoByIdChuyenTheoDoi = (params) => request('POST', '/su-co/{id}/chuyen-theo-doi', params)

/** Mở lại xử lý (lý do bắt buộc) (FR-045) */
export const postSuCoByIdMoLaiXuLy = (params) => request('POST', '/su-co/{id}/mo-lai-xu-ly', params)

/** Đóng sự cố (kết quả bắt buộc; Trung bình, Khẩn cấp chỉ Điều dưỡng, Bác sĩ; lây nhiễm chọn ketQuaLayNhiem và không còn vùng chỉ gắn sự cố này) (FR-045, FR-066, BR-M05-09, BR-M05-12) */
export const postSuCoByIdDong = (params) => request('POST', '/su-co/{id}/dong', params)

/** Kết thúc theo dõi sau ngã (Bác sĩ; lý do); công việc theo dõi Chưa đến hạn bị 005 hủy (FR-052, BR-M05-07) */
export const postSuCoByIdKetThucTheoDoiSauNga = (params) => request('POST', '/su-co/{id}/ket-thuc-theo-doi-sau-nga', params)

/** Thẻ thông tin khẩn cấp (bản chụp lúc kích hoạt và nội dung hiện hành); người không có quyền xem sức khỏe chỉ thấy nhận dạng (FR-047, FR-048, FR-050f, BR-M05-13) */
export const getSuCoByIdTheKhanCap = (params) => request('GET', '/su-co/{id}/the-khan-cap', params)

/** Xác nhận đã đối chiếu nguyện vọng (chỉ người được xem đủ thẻ); phản hồi hiển thị lại nguyện vọng (FR-048a, BR-M05-13) */
export const postSuCoByIdXacNhanDoiChieuNguyenVong = (params) => request('POST', '/su-co/{id}/xac-nhan-doi-chieu-nguyen-vong', params)

/** Chuyển viện từ sự cố (Điều dưỡng, Bác sĩ; sự cố Mới, Đang xử lý) (FR-049, FR-050, BR-M05-14) */
export const postSuCoByIdChuyenVien = (params) => request('POST', '/su-co/{id}/chuyen-vien', params)

/** Bản tóm tắt chuyển viện (in được; Bác sĩ, Điều dưỡng, Trưởng tầng trong phạm vi, Quản lý viện) (FR-050) */
export const getBanTomTatChuyenVienById = (params) => request('GET', '/ban-tom-tat-chuyen-vien/{id}', params)

/** Ghi dấu nguy kịch (Bác sĩ) hoặc dấu tạm (Điều dưỡng khi không có Bác sĩ trực) (FR-050a, FR-050b, BR-M05-15, BR-M05-16) */
export const postNguoiCaoTuoiByNctIdDauNguyKich = (params) => request('POST', '/nguoi-cao-tuoi/{nctId}/dau-nguy-kich', params)

/** Bác sĩ xác nhận dấu tạm (FR-050a) */
export const postDauNguyKichByIdXacNhan = (params) => request('POST', '/dau-nguy-kich/{id}/xac-nhan', params)

/** Gỡ dấu (Bác sĩ; lý do); cảnh báo nguy kịch vẫn phải đóng theo FR-050d (FR-050a) */
export const postDauNguyKichByIdGo = (params) => request('POST', '/dau-nguy-kich/{id}/go', params)

/** Ghi kết quả xác nhận lại nguyện vọng (Bác sĩ; Điều dưỡng trong phạm vi) (FR-050c, FR-050g, BR-M05-16) */
export const postCanhBaoByIdKetQuaXacNhanLai = (params) => request('POST', '/canh-bao/{id}/ket-qua-xac-nhan-lai', params)

/** Ghi lựa chọn thực hiện và thực hiện lệnh tương ứng (FR-050c, FR-050d, BR-M05-15) */
export const postCanhBaoByIdLuaChonThucHien = (params) => request('POST', '/canh-bao/{id}/lua-chon-thuc-hien', params)

/** Tra cứu dữ liệu tiếp xúc theo nhóm trong CFG-M05-07 trước thời điểm phát hiện (không lưu, không đề xuất) (FR-060, BR-M05-10) */
export const getSuCoByIdTraCuuTiepXuc = (params) => request('GET', '/su-co/{id}/tra-cuu-tiep-xuc', params)

/** Danh sách tiếp xúc (Bác sĩ, Điều dưỡng, Quản lý viện; Trưởng tầng trong phạm vi chỉ xem) (FR-061) */
export const getSuCoByIdDanhSachTiepXuc = (params) => request('GET', '/su-co/{id}/danh-sach-tiep-xuc', params)

/** Lập danh sách (Nháp) (FR-061) */
export const postSuCoByIdDanhSachTiepXuc = (params) => request('POST', '/su-co/{id}/danh-sach-tiep-xuc', params)

/** Thêm người (Nháp) hoặc bổ sung người (Đã xác nhận, lý do bắt buộc) (FR-061, FR-062, BR-M05-11) */
export const postDanhSachTiepXucByIdNguoi = (params) => request('POST', '/danh-sach-tiep-xuc/{id}/nguoi', params)

/** Bỏ người khỏi danh sách Nháp (FR-061) */
export const postDanhSachTiepXucByIdNguoiByNguoiIdBo = (params) => request('POST', '/danh-sach-tiep-xuc/{id}/nguoi/{nguoiId}/bo', params)

/** Loại người khỏi danh sách Đã xác nhận (lý do); gỡ dấu tiếp xúc, công việc đo nhiệt độ Chưa đến hạn bị 005 hủy (FR-061) */
export const postDanhSachTiepXucByIdNguoiByNguoiIdLoai = (params) => request('POST', '/danh-sach-tiep-xuc/{id}/nguoi/{nguoiId}/loai', params)

/** Xác nhận danh sách (≥ 1 người, hoặc khongCoNguoiTiepXuc kèm lý do) (FR-061, FR-062, BR-M05-11) */
export const postDanhSachTiepXucByIdXacNhan = (params) => request('POST', '/danh-sach-tiep-xuc/{id}/xac-nhan', params)

/** Người thân cần liên hệ trực tiếp (Hành chính; chỉ họ tên, quan hệ, số điện thoại, nội dung cần báo) (FR-062, BR-M05-24) */
export const getSuCoByIdNguoiThanCanLienHe = (params) => request('GET', '/su-co/{id}/nguoi-than-can-lien-he', params)

/** Vùng đang khoanh và lịch sử (Bác sĩ, Quản lý viện, Trưởng tầng, Điều dưỡng, Hành chính) (FR-064, FR-080) */
export const getKhoanhVung = (params) => request('GET', '/khoanh-vung', params)

/** Khoanh vùng (Bác sĩ, Quản lý viện; tầng hoặc một khu vực; lý do; gắn ≥ 1 sự cố lây nhiễm chưa đóng) (FR-063, FR-064, FR-065, BR-M05-11) */
export const postKhoanhVung = (params) => request('POST', '/khoanh-vung', params)

/** Gắn thêm sự cố lây nhiễm chưa đóng vào vùng (FR-066) */
export const postKhoanhVungByIdGanSuCo = (params) => request('POST', '/khoanh-vung/{id}/gan-su-co', params)

/** Gỡ khoanh vùng (Bác sĩ, Quản lý viện; lý do) (FR-064, BR-M05-12) */
export const postKhoanhVungByIdGo = (params) => request('POST', '/khoanh-vung/{id}/go', params)

/** Giấy phép cơ sở và lịch sử (Quản lý viện, Bác sĩ) (FR-070, FR-075) */
export const getGiayPhepCoSo = (params) => request('GET', '/giay-phep-co-so', params)

/** Lưu giấy phép mới (gia hạn, đổi phạm vi, sửa sai thay thế bản cũ; Quản lý viện) (FR-070, FR-071, FR-075, BR-M06-05) */
export const postGiayPhepCoSo = (params) => request('POST', '/giay-phep-co-so', params)

/** Số liên hệ của viện (FR-070) */
export const getSoLienHeVien = (params) => request('GET', '/so-lien-he-vien', params)

/** Đổi số liên hệ của viện (Quản lý viện; nhật ký) (FR-070, BR-M05-24) */
export const putSoLienHeVien = (params) => request('PUT', '/so-lien-he-vien', params)

/** Bản ghi khám và điều trị (FR-072, FR-080) */
export const getNguoiCaoTuoiByNctIdKhamDieuTri = (params) => request('GET', '/nguoi-cao-tuoi/{nctId}/kham-dieu-tri', params)

/** Ghi khám, đánh giá, chẩn đoán, chỉ định, điều trị, kết quả (FR-072, FR-073, BR-M06-05) */
export const postNguoiCaoTuoiByNctIdKhamDieuTri = (params) => request('POST', '/nguoi-cao-tuoi/{nctId}/kham-dieu-tri', params)

/** Người cao tuổi bấm nút SOS; tạo hoặc gộp cảnh báo Khẩn cấp (FR-026, FR-047d, FR-047e, BR-M05-27) */
export const postSos = (params) => request('POST', '/sos', params)

/** Trạng thái cảnh báo SOS đang mở gần nhất của người cao tuổi (FR-047f) */
export const getSosHienTai = (params) => request('GET', '/sos/hien-tai', params)

/** Người cao tuổi chọn nhanh nơi đang ở sau khi bấm (một lần) (FR-047f) */
export const postSosByIdNoiDangO = (params) => request('POST', '/sos/{id}/noi-dang-o', params)

/** App người cao tuổi gửi vị trí GPS cho lần bấm SOS (bổ sung 2026-10-22, Q-373) (FR-047h, BR-M05-28) */
export const postSosByIdViTri = (params) => request('POST', '/sos/{id}/vi-tri', params)

/** Nhân viên đang trong ca bấm nút hỗ trợ khẩn cấp; tạo hoặc gộp cảnh báo Khẩn cấp NV_YEU_CAU_HO_TRO (FR-026, FR-047i, BR-M05-29) */
export const postHoTroKhanCap = (params) => request('POST', '/ho-tro-khan-cap', params)

/** Trạng thái cảnh báo nút hỗ trợ đang mở gần nhất của người bấm (đang tới, đã tới) (FR-047j) */
export const getHoTroKhanCapHienTai = (params) => request('GET', '/ho-tro-khan-cap/hien-tai', params)

/** Người bấm chọn loại sự việc, tầng, nơi, người cao tuổi sau khi gửi (mỗi trường đặt một lần) (FR-047i) */
export const postHoTroKhanCapByIdChiTiet = (params) => request('POST', '/ho-tro-khan-cap/{id}/chi-tiet', params)

/** App nhân viên gửi vị trí GPS cho lần bấm nút hỗ trợ (như /sos/{id}/vi-tri) (FR-047h, BR-M05-28) */
export const postHoTroKhanCapByIdViTri = (params) => request('POST', '/ho-tro-khan-cap/{id}/vi-tri', params)

/** Người phụ trách ghi "Đã tới nơi" (cảnh báo từ nút SOS hoặc nút hỗ trợ; một lần; không đổi trạng thái) (FR-047j, FR-029, BR-M05-30) */
export const postCanhBaoByIdDaToiNoi = (params) => request('POST', '/canh-bao/{id}/da-toi-noi', params)

/** Thông tin đọc cho tổng đài 115 (cảnh báo Khẩn cấp) (FR-047k) */
export const getCanhBaoByIdThongTinGoi115 = (params) => request('GET', '/canh-bao/{id}/thong-tin-goi-115', params)

/** Ghi lượt bấm "Gọi 115" trước khi app mở trình quay số (cảnh báo Khẩn cấp) (FR-047k, BR-M05-31) */
export const postCanhBaoByIdGoi115 = (params) => request('POST', '/canh-bao/{id}/goi-115', params)

/** Thông tin đọc cho tổng đài 115 (sự cố Khẩn cấp) (FR-047k) */
export const getSuCoByIdThongTinGoi115 = (params) => request('GET', '/su-co/{id}/thong-tin-goi-115', params)

/** Ghi lượt bấm "Gọi 115" (sự cố Khẩn cấp) (FR-047k, FR-048b, BR-M05-31) */
export const postSuCoByIdGoi115 = (params) => request('POST', '/su-co/{id}/goi-115', params)

/** Người bấm chọn kết quả gọi (một lần); Đã gọi được thì thêm diễn biến GOI_CAP_CUU vào sự cố liên kết (FR-047k, FR-048b) */
export const postLuotGoi115ByIdKetQua = (params) => request('POST', '/luot-goi-115/{id}/ket-qua', params)

/** Thông tin hiện ở màn lệnh Chuyển viện (nguyện vọng khi điều trị, nguyện vọng cuối đời, người liên hệ chính); không gợi ý cơ sở (FR-049) */
export const getSuCoByIdChuyenVienThongTin = (params) => request('GET', '/su-co/{id}/chuyen-vien/thong-tin', params)

/** Tải phiếu tóm tắt chuyển viện PDF đã lưu (đúng file đã dựng trong lệnh) (FR-050h, BR-M05-32) */
export const getBanTomTatChuyenVienByIdPhieu = (params) => request('GET', '/ban-tom-tat-chuyen-vien/{id}/phieu', params)

/** In lại phiếu; trả file đã lưu có dấu "Bản in lại"; ghi lần in và nhật ký (FR-050h, BR-M05-32) */
export const postBanTomTatChuyenVienByIdInLai = (params) => request('POST', '/ban-tom-tat-chuyen-vien/{id}/in-lai', params)
