# Feature Specification: Chi phí phát sinh

**Feature Branch**: `010-incurred-costs`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Quản lý chi phí phát sinh của người cao tuổi theo docs/nghiep-vu.md Module 11 (mục 15): chi phí nháp được tự sinh từ sự kiện của các module khác (ngày lưu trú theo chính sách vắng, buổi bán trú, dịch vụ tính phí hoàn thành, vật phẩm tiêu hao, hoạt động có phí, liều thuốc nguồn viện, người thân ở lại) và luôn trỏ về bản ghi nguồn; đơn giá theo phiên bản tại ngày phát sinh; chi phí trong gói ghi số tiền 0; vòng đời Nháp → Đã kiểm tra → Đã duyệt → Đã chốt; sai sót sau chốt bằng khoản điều chỉnh; chia phí tháng theo ngày; xuất dữ liệu kỳ đã chốt cho kế toán. Không thu tiền, không hạch toán."

## Clarifications

### Session 2026-09-26

- Q: Chốt được thực hiện theo bảng chi phí của từng người cao tuổi hay một lần cho cả viện? → A: Theo bảng chi phí của từng người cao tuổi; kỳ chi phí của viện Đã chốt khi mọi bảng của kỳ đã chốt; kỳ cuối là một bảng bình thường có ngày kết thúc sớm (đề xuất Q-131).
- Q: Khoản chi phí loại Thuốc hiển thị tên thuốc cho ai? → A: Người xem chi phí chỉ thấy "Thuốc" + mã vật phẩm + số lượng + đơn giá + thành tiền; tên thuốc, hàm lượng chỉ hiện với người có quyền xem thuốc của người cao tuổi đó; file kế toán dùng mã vật phẩm (đề xuất Q-133).
- Q: CFG-M11-01 "Ngày chốt kỳ chi phí" là ranh giới kỳ hay hạn chốt? → A: Kỳ luôn là tháng dương lịch; CFG-M11-01 là hạn phải chốt xong các bảng của kỳ vừa kết thúc, dùng để nhắc (đề xuất Q-132).

### Session 2026-09-27

- Q: Nếu sau khi kỳ cuối đã chốt vẫn phát sinh chi phí trước lệnh Kết thúc lưu trú, điều kiện "chi phí đã chốt" xử lý thế nào? → A: Điều kiện vẫn Đạt; khoản phát sinh thêm thành khoản điều chỉnh bổ sung ở bảng sau kỳ cuối, hành chính được nhắc mỗi CFG-M02-09 tới khi bảng đó chốt (đề xuất Q-134).
- Q: Khoản "thiếu đơn giá" lấy đơn giá bằng cách nào? → A: Quản lý viện tạo phiên bản đơn giá có ngày hiệu lực lùi, chỉ cho khoảng chưa có phiên bản nào (không chồng khoảng, DBR-08); hệ thống tự tính lại các khoản thiếu đơn giá trong khoảng đó; không nhập đơn giá tay cho khoản (đề xuất Q-135).
- Q: "Chi phí tạm tính" hiển thị cho người thân được tính từ những khoản nào? → A: Mọi khoản chưa hủy, chưa chốt (Nháp, Đã kiểm tra, Đã duyệt), trừ khoản nhập tay và mua hộ chưa được Quản lý viện duyệt; chỉ hiện tổng theo loại, nhãn "tạm tính, chưa chốt" (đề xuất Q-136).
- Q: Khi số tiền mua hộ thực tế cao hơn số đã được đồng ý, khoản chi phí được xử lý thế nào? → A: Cho ghi đã mua; khoản mang dấu "vượt số tiền đã đồng ý"; Quản lý viện bắt buộc nhập lý do khi duyệt; người đại diện được báo phần vượt khi khoản được duyệt (đề xuất Q-137).
- Q: Với hợp đồng bán trú tính theo giá tháng, phí của từng buổi được tính thế nào? → A: Phí buổi = giá tháng / số buổi có lịch trong tháng × hệ số trạng thái có mặt; chênh lệch làm tròn dồn vào buổi có lịch cuối cùng của tháng (đề xuất Q-138).
- Q: Thuốc mua hộ sau đó được dùng theo liều có bị tính phí hai lần không? → A: Không; thuốc mua hộ được tiếp nhận ở feature 006 với nguồn "gia đình gửi", liều không sinh khoản Thuốc, chỉ tính một lần ở khoản Mua hộ (đề xuất Q-139).
- Q: Người bán trú đến vào ngày không có lịch, hoặc có lịch nhưng khu bán trú nghỉ, tính phí thế nào? → A: Ngày đến ngoài lịch tính một buổi phát sinh hệ số 100% theo đơn giá buổi, ngoài giá tháng; buổi trùng ngày nghỉ không tính và không đếm vào số buổi chia giá tháng (đề xuất Q-140).
- Q: Có tính phí lưu trú cho ngày người cao tuổi qua đời không? → A: Có, trọn ngày theo hệ số của ngày đó; "dừng từ thời điểm qua đời" áp cho sự kiện sau thời điểm đó (đề xuất Q-141).

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 đã chốt nguồn "suất ăn người thân" (FR-043 của spec 011): mỗi suất của người thân ở lại có đăng ký ăn, chỉ khi lượt Đang ở lại, là một bản ghi nguồn; suất bị hủy trước giờ bữa thì khoản tương ứng bị hủy; sau giờ bữa không hủy vì người thân không ăn. Bảng nguồn FR-005 và bảng giao tiếp được cập nhật; điểm báo lại 14 phần 011 đã hoàn tất.

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (8.8, 8.9, 3.4, BR-M04-18, Q-167, Q-170, Q-173, Q-176) đã chốt:
- Nguồn "điểm danh có mặt ở buổi hoạt động có thu phí": với chuyến đi là bản ghi điểm danh rời viện, ngày tính phí là ngày rời viện thực tế; lượt "bỏ giữa chừng" vẫn tính; lượt Vắng và "Không ghi nhận" không tạo khoản (bảng nguồn được sửa).
- Hủy lượt đến từ đính chính điểm danh và từ Hủy ghi nhận điểm danh rời viện (bảng giao tiếp được sửa).
- Phí buổi của bán trú dùng giờ về theo ngày do feature 005 quản lý, có thể dời vì đồng ý về muộn; spec 014 không tạo khoản riêng cho phần giờ thêm (Q-138, Q-140 áp như cũ).

### Cập nhật 2026-09-28 (đồng bộ với spec 016, rà chéo)

Spec 016 (báo cáo chi phí 18.4) tính tỷ lệ tự sinh / nhập tay theo trường **nguồn sinh** của FR-014: tự sinh = tự động, mua hộ, điều chỉnh do hệ thống tạo (FR-027); nhập tay = nhập tay, điều chỉnh do Hành chính lập (UC-63). Khoản điều chỉnh tính vào kỳ của bảng chứa nó (FR-002a, FR-003); "bảng chưa chốt" gồm cả bảng thường và bảng bổ sung. Dòng giao tiếp "Gửi Module 14" đổi thành "Gửi 016" với đủ dữ liệu tối thiểu. Không có thay đổi về hành vi của spec này.

### Cập nhật 2026-09-28 (đồng bộ với spec 017, góp ý nghiệp vụ Q-211, Q-212, Q-219)

Tài liệu nguồn (1.2, 15.1, 15.6, 15.9, BR-M11-11, BR-M11-13, BR-M11-15) và spec 017 đã chốt; spec này được sửa theo checklist consistency của spec 017 (CHK014 → CHK019):
- Người xuất file kế toán đổi từ Hành chính sang **Kế toán** (FR-039, FR-041, FR-043, User Story 7). Hành chính vẫn kiểm tra và gửi chốt.
- FR-042 giới hạn lại: feature này không thu tiền, nhưng số dư, công nợ và trừ bảng chi phí vào số dư thuộc feature 017.
- Thêm FR-038a (tổng chi phí chưa chốt cho feature 017) và hai dòng giao tiếp "Gửi 017", "Nhận 017" (sự kiện bảng Đã chốt, dấu "số dư không đủ" cho đề nghị mua hộ).
- Q-134 giữ nguyên; feature 017 FR-022 đã được sửa để bảng bổ sung chưa chốt không chặn quyết toán, nhất quán với FR-034a.

## Phạm vi

**Trong phạm vi** (Module 11, mục 15; UC-61 → UC-64, UC-79):

1. Kỳ chi phí của viện, bảng chi phí thường của từng người cao tuổi theo kỳ, bảng bổ sung cho khoản điều chỉnh khi bảng thường đã chốt (15.6, 15.7, KY_CHI_PHI, Q-131, Q-134).
2. Tự sinh khoản chi phí ở trạng thái Nháp từ sự kiện nguồn của các module khác. Mỗi khoản trỏ về đúng một bản ghi nguồn. Gồm cả ngày lưu trú do feature này lập, buổi bán trú phát sinh ngoài lịch, ngày khu bán trú nghỉ, và danh sách sự kiện không sinh chi phí (15.2, 15.4, BR-M11-01, DBR-15, Q-140).
3. Xác định đơn giá và số tiền: đơn giá theo hợp đồng hiệu lực hoặc theo phiên bản đơn giá tại ngày phát sinh; khoản trong gói ghi số tiền 0; hệ số chính sách vắng; chia phí tháng theo ngày và xử lý chênh lệch làm tròn (BR-M11-02, 03, 09, DBR-16, 15.7).
4. Vòng đời khoản chi phí Nháp → Đã kiểm tra → Đã duyệt → Đã chốt / Đã hủy (15.6, UC-61, UC-62).
5. Chi phí nhập tay cho khoản ngoài danh mục, và đề nghị mua hộ có đồng ý hoặc duyệt trước khi mua (BR-M11-04, 07).
6. Tính lại hoặc hủy khoản chưa chốt khi bản ghi nguồn thay đổi; khoản điều chỉnh cho sai sót sau chốt và cho miễn giảm có duyệt (BR-M11-05, DBR-17, UC-63).
7. Chốt kỳ, cảnh báo biến động chi phí, kỳ cuối khi kết thúc lưu trú hoặc qua đời (BR-M11-06, 07, 08, 15.7).
8. Cung cấp bảng chi phí và chi phí tạm tính cho người thân, có che tên thuốc. Xuất dữ liệu đã chốt cho kế toán (15.6, 23, UC-64, Q-133, Q-136).
9. Các thông báo của nghiệp vụ chi phí, gửi qua feature 009 (FR-044).

**Ngoài phạm vi** (spec này **nhận** sự kiện nguồn hoặc **cung cấp** dữ liệu cho feature sở hữu):

- Quy tắc dùng chung (nhóm dữ liệu, lý do bắt buộc, đính chính, vòng đời yêu cầu phê duyệt, nhật ký, tham số, "hoặc toàn bộ, hoặc không", Bộ lập lịch chạy lại không tạo trùng): feature 000. Spec này kế thừa và không lặp lại.
- Hợp đồng, phụ lục, nội dung hợp đồng hiệu lực tại một ngày, danh mục dịch vụ và phiên bản đơn giá, lượt vắng và hệ số phí vắng theo ngày, quyết định giữ giường, hồ sơ kết thúc lưu trú, ghi nhận qua đời, mốc tính phí "chưa vào ở": feature 004. Spec này **dùng** các kết quả đó, không tính lại hệ số vắng.
- Công việc có tính phí, số lượng vật phẩm đã dùng, trạng thái có mặt bán trú theo ngày: feature 005. Liều thuốc và lần dùng PRN nguồn viện, lần giao và nhận lại thuốc mang theo: feature 006. Lượt người thân ở lại: feature 012. Suất ăn của người ở lại: feature 011. Điểm danh hoạt động có thu phí: feature 014.
- Quyền xem chi phí của từng người thân, cổng người thân, bản tin định kỳ: feature 012. Gửi thông báo: feature 009.
- Báo cáo chi phí và dashboard (18.4, 18.5): feature báo cáo (Module 14). Spec này chỉ cung cấp dữ liệu.
- Thu tiền, hoàn cọc, số dư, trừ bảng chi phí vào số dư, đối soát chuyển khoản, quyết toán khi kết thúc lưu trú: feature 017 (15.9, Q-211). Hạch toán, hóa đơn: phần mềm kế toán bên ngoài (1.2, 15.1).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hệ thống tự sinh chi phí nháp từ sự kiện nguồn, mỗi khoản truy về đúng bản ghi nguồn (Priority: P1)

Khi một sự kiện nguồn trong bảng 15.2 xảy ra ở module khác, hệ thống tự tạo khoản chi phí ở trạng thái Nháp cho người cao tuổi. Các sự kiện gồm: một ngày lưu trú kết thúc, trạng thái có mặt bán trú được xác định, công việc có tính phí hoàn thành, vật phẩm được ghi dùng, người cao tuổi được điểm danh vào hoạt động có thu phí, liều thuốc nguồn viện được xác nhận Đã dùng, một đêm người thân ở lại. Mỗi khoản ghi rõ bản ghi nguồn nào tạo ra nó. Hành chính không nhập lại. Từ tổng chi phí của một người, hành chính mở được đến từng khoản và từ mỗi khoản mở được đến bản ghi nguồn.

**Why this priority**: Đây là nguyên tắc cốt lõi của Module 11 (1.3 "hệ thống chủ động sinh… chi phí", 15.4). Mọi bước kiểm tra, duyệt, chốt, xuất phía sau đều dựa trên các khoản này.

**Independent Test**: Dựng một người nội trú có hợp đồng tính theo tháng và phát sinh trong 3 ngày: 1 ngày vắng về nhà, 1 lần thay tã 2 gói, 1 lần đưa đi khám, 1 liều thuốc nguồn viện, 1 buổi hoạt động có phí, 1 đêm người thân ở lại. Chạy Bộ lập lịch bằng đồng hồ giả lập. Kiểm tra số khoản, nguồn của từng khoản, và việc chạy lại không tạo trùng.

**Acceptance Scenarios**:

1. **Given** A nội trú dài hạn có hợp đồng Hiệu lực và Đang lưu trú ngày 05/10, **When** Bộ lập lịch xử lý ngày 05/10 sau khi ngày đó kết thúc, **Then** có đúng một khoản "Phí lưu trú" Nháp ngày 05/10 cho A, hệ số 100%, nguồn là ngày lưu trú 05/10 của hợp đồng A (BR-M11-01, DBR-15).
2. **Given** A vắng về nhà ngày 06/10 và feature 004 đã xác định hệ số 100%, giữ giường cho lượt vắng này, **When** Bộ lập lịch xử lý ngày 06/10, **Then** khoản phí lưu trú ngày 06/10 có hệ số 100% và trỏ về lượt vắng. Không có khoản nào khác cho ngày 06/10 (BR-M02-06, 15.2).
3. **Given** nhân viên chăm sóc ghi hoàn thành công việc "thay tã" có tính phí của A với số lượng 2, **When** kết quả được lưu, **Then** có một khoản "Vật phẩm tiêu hao" Nháp: số lượng 2, đơn vị "gói", nguồn là kết quả ghi nhận đó (feature 005 FR-051).
4. **Given** điều dưỡng xác nhận Đã dùng một liều thuốc nguồn viện của A, **When** liều được lưu, **Then** có một khoản "Thuốc" Nháp có số lượng theo liều, nguồn là liều đó (BR-M07-14, feature 006 FR-033).
5. **Given** người thân M ở lại từ 18:00 ngày 01/11 tới 07:00 ngày 03/11, **When** feature 012 báo từng đêm, **Then** có 2 khoản "Người thân ở lại" (đêm bắt đầu 01/11 và đêm bắt đầu 02/11), nguồn là lượt ở lại (feature 012 FR-042, FR-043).
6. **Given** Bộ lập lịch đã sinh phí lưu trú ngày 05/10 cho A, **When** Bộ lập lịch chạy lại cho ngày 05/10 (chạy bù sau gián đoạn), **Then** không có khoản trùng (NFR-04).
7. **Given** khoản phí lưu trú ngày 06/10 của A (hệ số 100%, vắng về nhà) còn Nháp, **When** feature 004 đính chính lượt vắng thành "không vắng ngày 06/10", **Then** ngày lưu trú 06/10 được cập nhật thành có mặt, lịch sử cập nhật được lưu và khoản được tính lại, bỏ tham chiếu lượt vắng. Nếu khoản đã chốt thì tạo khoản điều chỉnh theo FR-027 (FR-005a).
8. **Given** tổng chi phí tháng 10 của A là 9.450.000 đồng, **When** hành chính mở bảng chi phí tháng 10 của A, **Then** hành chính xem được từng khoản cộng thành số đó, và từ mỗi khoản mở được bản ghi nguồn (công việc, liều, lượt vắng, lượt ở lại…) hoặc lý do nhập tay (15.4).

---

### User Story 2 - Đơn giá và số tiền được xác định đúng căn cứ (Priority: P1)

Với mỗi khoản, hệ thống tự xác định đơn giá và số tiền. Khoản thuộc hợp đồng (phí lưu trú, dịch vụ đăng ký) dùng đơn giá đã ghi trong nội dung hợp đồng hiệu lực tại ngày phát sinh. Khoản ngoài hợp đồng dùng phiên bản đơn giá hiệu lực tại ngày phát sinh, không dùng giá tại ngày chốt. Khoản đã nằm trong gói có số tiền 0 nhưng vẫn được giữ để truy xuất. Với hợp đồng tính theo tháng, phí mỗi ngày bằng giá tháng chia số ngày của tháng, nhân hệ số chính sách vắng. Một tháng không vắng ngày nào có tổng phí lưu trú đúng bằng giá tháng.

**Why this priority**: Sai đơn giá là nguồn tranh chấp trực tiếp với gia đình. Hai quyết định đã chốt (Q-25, Q-22) và quy tắc chia phí tháng (BR-M11-09) cần được kiểm thử độc lập.

**Independent Test**: Dựng hợp đồng giá tháng 9.300.000 đồng cho tháng 10 (31 ngày) có gói gồm "tã", một phiên bản đơn giá "đưa đi khám" đổi giữa tháng, và một phụ lục đổi giá tháng từ 20/11. Kiểm tra đơn giá và số tiền của từng khoản so với kết quả tính tay.

**Acceptance Scenarios**:

1. **Given** hợp đồng của A tính giá tháng 9.300.000 đồng và A Đang lưu trú cả tháng 10 (31 ngày) không vắng, **When** Bộ lập lịch sinh phí mọi ngày tháng 10, **Then** mỗi ngày có số tiền 300.000 đồng và tổng phí lưu trú tháng 10 đúng bằng 9.300.000 đồng (15.7, BR-M11-09).
2. **Given** hợp đồng giá tháng 10.000.000 đồng và tháng 11 có 30 ngày, **When** sinh phí mọi ngày tháng 11 không vắng, **Then** 29 ngày đầu mỗi ngày 333.333 đồng, ngày 30/11 là 333.343 đồng, và tổng đúng 10.000.000 đồng (chênh lệch làm tròn dồn vào ngày cuối, BR-M11-09).
3. **Given** C nằm viện và feature 004 cho các ngày vắng thứ 8 đến 30 hệ số 70%, **When** sinh phí các ngày đó, **Then** mỗi ngày có số tiền bằng phí ngày của tháng × 70%, làm tròn tới đồng, và khoản ghi hệ số 70% cùng lượt vắng nguồn.
4. **Given** "đưa đi khám" không thuộc hợp đồng của A, có phiên bản đơn giá 300.000 đồng hiệu lực tới 14/10 và 350.000 đồng từ 15/10, **When** A được đưa đi khám ngày 14/10 và hành chính chốt kỳ ngày 02/11, **Then** khoản có đơn giá 300.000 đồng, căn cứ là phiên bản hiệu lực ngày 14/10 (BR-M11-02, DBR-16).
5. **Given** hợp đồng của A có "tã" trong gói, **When** A dùng 2 gói tã, **Then** khoản được tạo với dấu "thuộc gói", số tiền 0, vẫn giữ số lượng và nguồn (BR-M11-03).
6. **Given** phụ lục đổi giá tháng của A từ 12.000.000 đồng lên 13.500.000 đồng được áp dụng, hiệu lực 20/11, **When** sinh phí tháng 11, **Then** các ngày 01/11 → 19/11 tính theo 12.000.000 / 30 và các ngày 20/11 → 30/11 tính theo 13.500.000 / 30 (BR-M02-04, BR-M02-09).
7. **Given** hợp đồng bán trú của E giá tháng 3.000.000 đồng, lịch đến thứ 2, 4, 6, tháng 11 có 13 buổi có lịch, **When** E Vắng có báo thứ 2 ngày 09/11, Vắng không báo thứ 4 ngày 11/11, và có mặt 11 buổi còn lại (kể cả buổi cuối tháng thứ 2 ngày 30/11), **Then** mỗi buổi có phí cơ sở 230.769 đồng, riêng buổi 30/11 là 230.772 đồng. Buổi 09/11 là 0 đồng; buổi 11/11 là 115.385 đồng (230.769 × 50% = 115.384,5, làm tròn nửa lên theo FR-013); nếu E có mặt đủ 13 buổi thì tổng đúng 3.000.000 đồng (BR-M11-09, Q-138). **When** khu bán trú nghỉ thứ 4 ngày 18/11, **Then** tháng 11 chỉ có 12 buổi có lịch, phí cơ sở là 250.000 đồng và buổi 18/11 không có khoản. **When** E đến thêm thứ 3 ngày 24/11 (ngoài lịch), **Then** có một khoản "buổi phát sinh" 100% theo phiên bản đơn giá buổi bán trú, không làm đổi phí cơ sở của 12 buổi (Q-140).
8. **Given** hợp đồng của A bắt đầu 10/10 nhưng A Hoàn tất tiếp nhận ngày 12/10, **When** sinh phí ngày 10/10 và 11/10, **Then** hai khoản được tạo với dấu "chưa vào ở" (feature 004 FR-074, Q-22).
9. **Given** thuốc X nguồn viện chưa có phiên bản đơn giá nào hiệu lực tại ngày phát sinh, **When** liều X được xác nhận Đã dùng, **Then** khoản Nháp vẫn được tạo, có dấu "thiếu đơn giá" và chưa có số tiền. Hành chính được thông báo. Khoản không kiểm tra được cho tới khi có đơn giá. **When** Quản lý viện tạo phiên bản đơn giá cho X hiệu lực lùi từ ngày 01/10 (trước đó X chưa có phiên bản nào), **Then** khoản được tự tính lại, dấu "thiếu đơn giá" được gỡ. Nếu phiên bản mới chồng lên một phiên bản đã có, hệ thống chặn (DBR-08, Q-135).
10. **Given** một khoản đã chốt có số tiền 115.385 đồng (hệ số 50%), **When** hệ số được sửa thành 70% sau chốt, trên phí cơ sở 230.769 đồng, **Then** số tiền mới là 161.538 đồng (161.538,3 làm tròn) và khoản điều chỉnh là +46.153 đồng. **When** hệ số được sửa ngược lại thành 30%, **Then** số tiền mới là 69.231 đồng (69.230,7 làm tròn) và khoản điều chỉnh tiếp theo là −92.307 đồng. Số âm được làm tròn theo giá trị tuyệt đối (FR-013, FR-027).

---

### User Story 3 - Hành chính kiểm tra, Quản lý viện duyệt và chốt bảng chi phí (Priority: P1)

Cuối kỳ, hành chính mở bảng chi phí của từng người cao tuổi, rà các khoản Nháp (lọc theo dấu như thiếu đơn giá, chờ quyết định, chưa vào ở; theo nguồn sinh như nhập tay, mua hộ, điều chỉnh; theo loại chi phí) và đánh dấu Đã kiểm tra, từng khoản hoặc hàng loạt. Khi mọi khoản đã được kiểm tra hoặc hủy, hành chính gửi chốt bảng. Quản lý viện duyệt các khoản, hoặc trả lại khoản sai về Nháp kèm lý do. Khi mọi khoản đã duyệt, Quản lý viện chốt bảng. Bảng đã chốt không sửa được và được gửi cho người thân có quyền xem chi phí.

**Why this priority**: Đây là quy trình 15.6 được nêu trực tiếp trong mô tả feature. Không có chốt thì không có dữ liệu cho kế toán và gia đình.

**Independent Test**: Với bảng tháng 10 của A gồm 40 khoản, kiểm tra hàng loạt 38 khoản, để 1 khoản Nháp và 1 khoản thiếu đơn giá. Thử gửi chốt, thử chốt, trả lại một khoản, hoàn tất rồi chốt. Thử sửa một khoản sau chốt.

**Acceptance Scenarios**:

1. **Given** bảng tháng 10 của A còn 1 khoản Nháp, **When** hành chính gửi chốt, **Then** hệ thống chặn và nêu khoản còn Nháp (BR-M11-06).
2. **Given** mọi khoản của bảng tháng 10 của A đã Đã kiểm tra hoặc Đã hủy và ngày 31/10 đã qua, **When** hành chính gửi chốt, **Then** bảng chuyển Chờ chốt và Quản lý viện được thông báo.
3. **Given** bảng Chờ chốt, **When** Quản lý viện trả lại một khoản "bỉm 5 gói" kèm lý do "số lượng sai", **Then** khoản về Nháp, bảng về Đang mở, và hành chính được thông báo kèm lý do.
4. **Given** bảng Chờ chốt còn 2 khoản Đã kiểm tra chưa duyệt, **When** Quản lý viện chốt, **Then** hệ thống chặn (BR-M11-06).
5. **Given** mọi khoản của bảng đã Đã duyệt hoặc Đã hủy, **When** Quản lý viện chốt, **Then** mọi khoản Đã duyệt chuyển Đã chốt, bảng chuyển Đã chốt. Người thân có quyền xem chi phí nhận thông báo "bảng chi phí tháng 10 đã chốt" (feature 009). Dữ liệu sẵn sàng để xuất cho kế toán.
6. **Given** một khoản Đã chốt, **When** bất kỳ ai, kể cả Quản lý viện, tìm cách sửa, xóa hoặc hủy khoản đó, **Then** không có thao tác nào như vậy. Chỉ có "Lập khoản điều chỉnh" (BR-M15-03, DBR-17).
7. **Given** tổng bảng tháng 10 của A là 13.200.000 đồng, tổng tháng 9 là 9.800.000 đồng và CFG-M11-03 mặc định \[30%\], **When** hành chính gửi chốt bảng tháng 10, **Then** hành chính nhận cảnh báo "tăng 34,7% so với kỳ trước" kèm các loại chi phí tăng nhiều nhất. Cảnh báo không chặn việc gửi chốt (BR-M11-07).
8. **Given** bảng tháng 10 của A đã đủ điều kiện, bảng tháng 10 của B còn 3 khoản Nháp, **When** Quản lý viện chốt bảng của A, **Then** bảng của A Đã chốt; kỳ tháng 10 của viện vẫn Chờ chốt. **When** bảng của B, là bảng cuối cùng của kỳ, Đã chốt, **Then** kỳ tháng 10 chuyển Đã chốt (Q-131, bảng trạng thái kỳ).
9. **Given** CFG-M11-01 được cấu hình là ngày 5 của tháng liền sau, **When** hết ngày 05/11 mà bảng tháng 10 của B vẫn Đang mở, **Then** từ ngày 06/11 hành chính được nhắc mỗi ngày; kỳ tháng 10 vẫn là 01/10 → 31/10, không đổi ranh giới (Q-132).
10. **Given** bảng tháng 10 của A có 60 khoản, trong đó 1 khoản "thiếu đơn giá" và 1 khoản nhập tay, **When** hành chính kiểm tra hàng loạt cả bảng, **Then** 59 khoản chuyển Đã kiểm tra, khoản thiếu đơn giá bị bỏ qua kèm lý do. **When** Quản lý viện duyệt hàng loạt, **Then** khoản nhập tay bị loại khỏi lần duyệt và phải được duyệt riêng (FR-015).
11. **Given** bảng tháng 10 của A đang Chờ chốt, **When** một khoản thay tã của ngày 31/10 được ghi muộn, **Then** khoản Nháp mới vào bảng tháng 10, bảng tự về Đang mở và hành chính được thông báo (FR-004).
12. **Given** hành chính đã kiểm tra nhầm một khoản và bảng còn Đang mở, **When** hành chính Bỏ kiểm tra kèm lý do, **Then** khoản về Nháp. **When** bảng đã Chờ chốt, **Then** lệnh Bỏ kiểm tra bị chặn; chỉ Quản lý viện Trả lại được.

---

### User Story 4 - Sai sót được xử lý bằng tính lại trước chốt và khoản điều chỉnh sau chốt (Priority: P2)

Khi bản ghi nguồn bị đính chính hoặc hủy ghi nhận, hệ thống tính lại hoặc hủy khoản tương ứng nếu khoản chưa chốt. Nếu khoản đã chốt, hệ thống tạo một khoản điều chỉnh cho phần chênh lệch, trỏ về khoản gốc. Khoản điều chỉnh này đi lại đủ vòng đời kiểm tra, duyệt, chốt trong kỳ đang mở. Hành chính cũng lập được khoản điều chỉnh khi phát hiện sai sót hoặc khi cần miễn giảm. Khoản gốc luôn giữ nguyên.

**Why this priority**: Bảo đảm tính toàn vẹn của dữ liệu đã chốt (1.5 nhóm 3, DBR-17). Cần có vòng đời và chốt (User Story 3) trước.

**Independent Test**: Tạo 4 tình huống: đính chính số lượng tã khi bảng còn mở; hủy ghi nhận một liều sau khi bảng đã chốt; quyết định giữ giường về muộn sau chốt; hành chính lập miễn giảm. Kiểm tra khoản gốc, khoản điều chỉnh, kỳ chứa khoản điều chỉnh và tổng.

**Acceptance Scenarios**:

1. **Given** khoản "tã 2 gói" của A còn Nháp, **When** trưởng tầng đính chính kết quả thành 1 gói, **Then** khoản được tính lại thành 1 gói, vẫn ở Nháp. Lịch sử tính lại lưu giá trị trước/sau và bản đính chính nguồn (BR-M11-05).
2. **Given** khoản "tã 2 gói" đã Đã kiểm tra (chưa chốt), **When** kết quả nguồn bị đính chính, **Then** hệ thống tính lại, đưa khoản về Nháp và báo hành chính (bảng Chờ chốt thì về Đang mở).
3. **Given** một liều nguồn viện có khoản Nháp, **When** liều bị đính chính "Hủy ghi nhận", **Then** khoản chuyển Đã hủy với lý do tham chiếu bản đính chính (BR-M11-05, feature 006).
4. **Given** bảng tháng 10 của A đã chốt, có khoản thuốc 2 viên × 15.000 đồng, **When** ngày 05/11 liều đó bị đính chính "Hủy ghi nhận", **Then** khoản gốc giữ nguyên. Hệ thống tạo khoản điều chỉnh −30.000 đồng ở Nháp, trỏ về khoản gốc và bản đính chính, thuộc bảng tháng 11 của A, và báo hành chính (BR-M11-05, DBR-17).
5. **Given** bảng tháng 10 của C đã chốt, các ngày 31/10 là ngày vắng thứ 31 mang dấu "chờ quyết định" với hệ số tạm 70%, **When** ngày 03/11 Quản lý viện quyết định hệ số 50% cho các ngày đó, **Then** hệ thống tạo khoản điều chỉnh cho phần chênh lệch của ngày 31/10 trong bảng tháng 11. Các ngày tháng 11 chưa chốt được tính lại trực tiếp (BR-M02-07, feature 004 FR-056).
6. **Given** A bị Hủy tiếp nhận ngày 12/10 và có 3 ngày phí "chưa vào ở", **When** hành chính lập khoản điều chỉnh miễn giảm −100% cho 3 khoản đó kèm lý do, **Then** các khoản điều chỉnh ở Nháp và chỉ có hiệu lực khi được kiểm tra, được Quản lý viện duyệt và được chốt (Q-27, feature 004 FR-075a).
7. **Given** một khoản gốc 300.000 đồng đã có điều chỉnh −300.000 đồng, **When** hành chính lập thêm điều chỉnh −50.000 đồng cho khoản đó, **Then** hệ thống chặn vì tổng của khoản gốc và các điều chỉnh không được âm.

---

### User Story 5 - Kỳ cuối khi kết thúc lưu trú hoặc qua đời (Priority: P2)

Khi hồ sơ kết thúc lưu trú được lập với ngày kết thúc dự kiến, hệ thống dừng sinh chi phí tự động sau ngày đó và tạo bảng kỳ cuối nháp tính đến ngày đó. Hành chính kiểm tra, Quản lý viện duyệt và chốt kỳ cuối trước lệnh Kết thúc lưu trú. Khi người cao tuổi qua đời, sinh chi phí dừng ngay từ thời điểm qua đời, và việc chốt kỳ cuối nằm trong danh sách việc sau qua đời.

**Why this priority**: Đây là điều kiện bắt buộc của lệnh Kết thúc lưu trú (5.6, 6.8, Q-21, Q-26). Cần có vòng đời và chốt trước.

**Independent Test**: Lập hồ sơ kết thúc với ngày dự kiến 20/11, chốt kỳ cuối. Đổi ngày thành 22/11 trước và sau khi chốt. Để quá ngày dự kiến. Ghi nhận qua đời cho một người khác lúc 10:00 ngày 15/11 có một liều được ghi muộn.

**Acceptance Scenarios**:

1. **Given** hành chính lập hồ sơ kết thúc lưu trú cho A ngày 10/11 với ngày kết thúc dự kiến 20/11, **When** hồ sơ được lưu, **Then** bảng tháng 11 của A trở thành bảng kỳ cuối nháp tính đến 20/11. Hệ thống sinh trước phí lưu trú các ngày 11/11 → 20/11 và không sinh phí tự động cho ngày sau 20/11 (BR-M11-08).
2. **Given** kỳ cuối của A đã được kiểm tra và duyệt đủ, **When** Quản lý viện chốt ngày 18/11, **Then** kỳ cuối Đã chốt và điều kiện "chi phí kỳ cuối đã chốt" của feature 004 chuyển Đạt.
3. **Given** kỳ cuối của A chưa chốt, **When** ngày kết thúc dự kiến đổi từ 20/11 sang 22/11, **Then** hệ thống sinh thêm phí các ngày 21/11, 22/11 ở Nháp. Nếu ngày đổi sớm hơn, các khoản ngày bị bỏ chuyển Đã hủy với lý do "đổi ngày kết thúc".
4. **Given** kỳ cuối của A đã chốt đến 20/11, **When** ngày kết thúc dự kiến đổi sang 22/11, **Then** hệ thống tạo khoản điều chỉnh bổ sung cho hai ngày 21/11, 22/11 mang dấu "ảnh hưởng kết thúc lưu trú". Điều kiện chi phí của feature 004 trở về Chưa đạt cho tới khi bảng chứa hai khoản đó được chốt (FR-034a, feature 004 FR-066).
5. **Given** hết ngày 20/11 mà lệnh Kết thúc lưu trú chưa thực hiện, **When** Bộ lập lịch chạy ngày 21/11, **Then** hệ thống sinh bù phí ngày 21/11 và tiếp tục sinh hằng ngày cho tới khi có ngày mới. Nếu kỳ cuối đã chốt, các ngày này là khoản điều chỉnh bổ sung (BR-M11-08, Q-26).
6. **Given** kỳ cuối của A đã chốt ngày 18/11, **When** ngày 20/11 A dùng một liều thuốc nguồn viện trước lệnh Kết thúc lưu trú, **Then** khoản đó thành khoản điều chỉnh bổ sung ở bảng bổ sung của A, không mang dấu "ảnh hưởng kết thúc lưu trú". Điều kiện chi phí của feature 004 vẫn Đạt. Hành chính được nhắc cho tới khi bảng đó được chốt (FR-029, FR-034a).
8. **Given** A đã Kết thúc lưu trú ngày 20/11, bảng tháng 11 và bảng bổ sung tháng 11 đều Đã chốt, **When** ngày 03/12 một liều thuốc ngày 20/11 bị đính chính "Hủy ghi nhận", **Then** hệ thống mở bảng bổ sung tháng 12 của A, ghi khoản điều chỉnh âm vào đó, và nhắc hành chính mỗi CFG-M02-09 cho tới khi bảng được chốt (FR-002a, FR-029).
9. **Given** hồ sơ kết thúc của A có ngày dự kiến 20/11, **When** một liều nguồn viện được ghi cho thời điểm 21/11 08:00, **Then** không có khoản nào được sinh. Sự kiện vào danh sách "sự kiện không sinh chi phí" kèm lý do "ngoài thời gian tính phí". **When** ngày kết thúc dự kiến đổi sang 22/11, **Then** khoản được sinh bù và mục trong danh sách được đánh dấu "đã sinh bù" (FR-011).
7. **Given** B qua đời lúc 10:00 ngày 15/11, **When** ghi nhận qua đời thành công, **Then** không có khoản tự sinh nào có thời điểm phát sinh sau 10:00 ngày 15/11. Ngày 15/11 vẫn có phí lưu trú. Một liều lúc 08:00 được ghi muộn lúc 11:00 vẫn sinh khoản. Mục "chốt chi phí kỳ cuối" trong danh sách việc sau qua đời chuyển Hoàn thành khi kỳ cuối của B Đã chốt (feature 004 FR-070, FR-071).

---

### User Story 6 - Chi phí nhập tay và đề nghị mua hộ (Priority: P2)

Với khoản không có trong danh mục, hành chính lập khoản nhập tay, bắt buộc có lý do. Khoản nhập tay chỉ có hiệu lực khi được Quản lý viện duyệt. Với việc mua hộ (thuốc mua hộ, vật phẩm cá nhân), hành chính lập đề nghị mua hộ với số tiền dự kiến. Nếu số tiền vượt hạn mức, đề nghị phải được người đại diện đồng ý qua cổng hoặc được Quản lý viện duyệt trước khi mua. Sau khi mua, hành chính ghi số tiền thực tế và hệ thống tạo khoản chi phí trỏ về đề nghị.

**Why this priority**: Là lối ra cho khoản không tự sinh được. Tần suất thấp hơn khoản tự sinh nhưng rủi ro tranh chấp cao.

**Independent Test**: Lập một khoản nhập tay cho một mục đã có trong danh mục (phải bị chặn) và một mục ngoài danh mục. Lập hai đề nghị mua hộ 300.000 đồng và 800.000 đồng. Cho người đại diện đồng ý một đề nghị và từ chối đề nghị kia. Ghi đã mua.

**Acceptance Scenarios**:

1. **Given** "bỉm" có trong danh mục, **When** hành chính lập khoản nhập tay "bỉm 1 gói", **Then** hệ thống chặn và hướng về nguồn tự sinh (ghi nhận dùng vật phẩm ở feature 005) (BR-M11-04).
2. **Given** "phí làm lại thẻ BHYT" không có trong danh mục, **When** hành chính lập khoản nhập tay 50.000 đồng mà không nhập lý do, **Then** hệ thống chặn. **When** có lý do, **Then** khoản được tạo ở Nháp, nguồn "nhập tay", và không được Quản lý viện duyệt hàng loạt (FR-015).
3. **Given** CFG-M11-02 mặc định \[500.000 đồng\], **When** hành chính lập đề nghị mua hộ "máy đo đường huyết" dự kiến 300.000 đồng, **Then** đề nghị chuyển Được phép mua ngay, không cần đồng ý.
4. **Given** đề nghị mua hộ dự kiến 800.000 đồng, **When** hành chính gửi đề nghị, **Then** đề nghị chuyển Chờ đồng ý. Người đại diện nhận yêu cầu trên cổng. Quản lý viện thấy đề nghị trong danh sách chờ duyệt. "Ghi đã mua" bị chặn cho tới khi có đồng ý hoặc duyệt (BR-M11-07).
5. **Given** người đại diện R từ chối đề nghị 800.000 đồng kèm lý do, **When** hành chính xem đề nghị, **Then** đề nghị chuyển Từ chối và không tạo chi phí.
6. **Given** đề nghị đã được đồng ý với số tiền dự kiến 800.000 đồng, **When** hành chính ghi đã mua với số tiền thực tế 820.000 đồng và ảnh hóa đơn, **Then** có một khoản "Mua hộ" Nháp 820.000 đồng trỏ về đề nghị, mang dấu "vượt số tiền đã đồng ý" để Quản lý viện xem khi duyệt. **When** Quản lý viện duyệt khoản mà không nhập lý do chấp nhận phần vượt, **Then** hệ thống chặn; **When** có lý do, **Then** khoản Đã duyệt và người đại diện nhận thông báo "đã mua 820.000 đồng, vượt 20.000 đồng so với số đã đồng ý" kèm lý do (Q-137).
7. **Given** đề nghị 800.000 đồng đang Chờ đồng ý, A có hai người đại diện R và S, **When** R đồng ý và gần như cùng lúc S từ chối, **Then** chỉ quyết định tới trước được ghi nhận, quyết định sau bị từ chối kèm thông báo đề nghị đã có quyết định (feature 000 FR-040). **When** S thôi làm người đại diện trong lúc đề nghị khác đang Chờ đồng ý, **Then** S không còn quyết định được đề nghị đó (FR-023a).
8. **Given** đề nghị mua hộ thuốc X đã chuyển Đã mua, **When** điều dưỡng tiếp nhận X ở feature 006 với nguồn "gia đình gửi" rồi cho dùng 30 liều, **Then** không có khoản "Thuốc" nào được sinh cho 30 liều đó; X chỉ được tính một lần ở khoản Mua hộ (Q-139).

---

### User Story 7 - Người thân xem bảng chi phí, kế toán nhận dữ liệu kỳ đã chốt (Priority: P3)

Người thân có quyền xem chi phí xem được bảng chi phí đã chốt và chi phí tạm tính của kỳ đang mở trên cổng. Kế toán xuất dữ liệu các bảng đã chốt của một kỳ ra file cho phần mềm kế toán, theo đúng các cột ở mục 23 (Q-212). Mỗi lần xuất được ghi lại. Feature này không thu tiền và không hạch toán; thu tiền và số dư thuộc feature 017.

**Why this priority**: Đây là đầu ra của Module 11. Cần dữ liệu đã chốt có trước.

**Independent Test**: Chốt bảng tháng 10 của 3 người, để 1 người chưa chốt. Xuất kỳ tháng 10, xuất lại. Đối chiếu file với các bảng. Mở cổng bằng người thân có và không có quyền xem chi phí.

**Acceptance Scenarios**:

1. **Given** bảng tháng 10 của A, B, C đã chốt và của D chưa chốt, **When** Kế toán xuất kỳ tháng 10, **Then** file gồm mọi khoản đã chốt của A, B, C với đủ các cột ở FR-039, có dòng tổng kiểm soát. File ghi rõ D chưa chốt nên chưa có trong file (23, Q-05).
2. **Given** kỳ tháng 10 đã xuất một lần với phạm vi "toàn bộ bảng đã chốt", **When** Kế toán xuất lại cùng phạm vi mà không có bảng nào mới chốt, **Then** file có cùng nội dung, và mọi bảng được đánh dấu "đã xuất trước đó". Lần xuất được ghi nhật ký (người, thời điểm, kỳ, phạm vi, danh sách bảng, số khoản, tổng tiền) (FR-041).
3. **Given** sau lần xuất đầu, bảng tháng 10 của D được chốt, **When** Kế toán xuất phạm vi "chỉ bảng chưa từng xuất", **Then** file chỉ gồm bảng của D, đánh dấu "xuất lần đầu", và dòng tổng kiểm soát chỉ tính bảng này. **When** xuất phạm vi "toàn bộ", **Then** file gồm A, B, C (đã xuất trước đó) và D (xuất lần đầu) (FR-041).
4. **Given** một khoản điều chỉnh cho tháng 10 được chốt trong bảng tháng 11, **When** xuất kỳ tháng 11, **Then** khoản điều chỉnh nằm trong file tháng 11, trỏ về khoản gốc và kỳ gốc tháng 10. File tháng 10 không đổi.
5. **Given** R có quyền xem chi phí của A, **When** R mở cổng, **Then** R thấy bảng chi phí đã chốt từng kỳ và chi phí tạm tính của kỳ đang mở, có nhãn "tạm tính, chưa chốt". **When** N không có quyền xem chi phí mở cổng, **Then** N không thấy mục chi phí (feature 012 FR-046).
6. **Given** bảng tháng 10 của A chưa chốt, **When** Kế toán xuất kỳ tháng 10 chỉ để lấy riêng bảng của A, **Then** hệ thống không cho xuất bảng chưa chốt.
7. **Given** bảng tháng 10 của A có khoản thuốc "Amlodipin 5 mg, 30 viên", R có quyền xem chi phí và xem sức khỏe có tác dụng, N chỉ có quyền xem chi phí, **When** hành chính, Kế toán, R, N mở bảng và Kế toán xuất file, **Then** R thấy tên "Amlodipin 5 mg"; hành chính, Kế toán, N và file kế toán chỉ thấy "Thuốc", mã vật phẩm, 30 viên, đơn giá, thành tiền (Q-133).
8. **Given** kỳ tháng 11 đang mở, A có: phí lưu trú Nháp 3.000.000 đồng, thuốc Đã kiểm tra 150.000 đồng, khoản nhập tay Nháp 50.000 đồng, khoản mua hộ Đã duyệt 300.000 đồng, một khoản thuốc "thiếu đơn giá", **When** R mở chi phí tạm tính, **Then** R thấy Phí lưu trú 3.000.000, Thuốc 150.000, Mua hộ 300.000 và ghi chú "còn khoản chưa có đơn giá". Khoản nhập tay chưa duyệt không được cộng. Không có chi tiết từng khoản; có nhãn "tạm tính, chưa chốt" (FR-022, FR-038, Q-136).

---

### Edge Cases

- **Sự kiện nguồn có ngày phát sinh thuộc bảng đã chốt** (ghi nhận muộn, bản ghi ngoại tuyến đồng bộ muộn): khoản mới trở thành khoản điều chỉnh bổ sung ở bảng chưa chốt của kỳ hiện tại, hoặc bảng bổ sung (FR-003), mang dấu "phát sinh thuộc kỳ đã chốt" và kỳ gốc.
- **Một ngày vừa có mặt vừa vắng** (rời viện 15:00): là ngày vắng theo quy tắc tra bảng 6.7. Chỉ có một khoản phí lưu trú cho ngày đó, dùng hệ số do feature 004 xác định.
- **Hai hợp đồng nối tiếp, hoặc phụ lục đổi giá, trong cùng tháng**: mỗi ngày dùng nội dung hợp đồng hiệu lực ngày đó. Công thức ngày cuối tháng (FR-009) áp theo giá tháng hiệu lực ngày cuối tháng, và chỉ khi hợp đồng ngày đó tính theo tháng. Tháng có hai mức giá có tổng bằng tổng từng ngày; tổng này không bắt buộc bằng một giá tháng nào (User Story 2 kịch bản 6).
- **Chuyển từ nội trú sang bán trú giữa tháng** (chiều ngược với dòng dưới): các ngày trước ngày hiệu lực tính phí lưu trú ngày theo FR-009 với số ngày của tháng. Từ ngày hiệu lực, tính phí buổi. Nếu hợp đồng bán trú tính giá tháng, số buổi có lịch để chia chỉ đếm các buổi từ ngày hiệu lực tới cuối tháng, và buổi có lịch cuối cùng của tháng nhận chênh lệch làm tròn.
- **Chuyển giường sang phòng khác giá vì lý do y tế** (feature 003 FR-030): trong lúc yêu cầu thay đổi lưu trú chờ duyệt, và sau khi bị từ chối, phí ngày tính theo đơn giá của hợp đồng hiện hành. Khi yêu cầu được áp dụng, đơn giá mới áp từ ngày hiệu lực thực tế (feature 000 FR-037). Trong cùng lần áp dụng, hệ thống MUST tự tạo khoản điều chỉnh cho phần chênh lệch từ ngày chuyển giường tới trước ngày hiệu lực thực tế. Đây là việc bắt buộc, không tùy viện, theo feature 003 FR-030 ("đơn giá mới áp dụng từ ngày chuyển"). Khoản điều chỉnh đi đủ vòng đời mục D.
- **Phụ lục được duyệt muộn hơn ngày hiệu lực mong muốn**: tác động tính từ ngày hiệu lực thực tế (feature 000 FR-037). Phần chênh lệch trước đó, nếu được chấp nhận, do hành chính lập khoản điều chỉnh (FR-026).
- **Người đại diện thôi làm đại diện khi đề nghị mua hộ đang Chờ đồng ý**: người đó mất quyền đồng ý; đề nghị chờ những người đại diện còn lại hoặc Quản lý viện (FR-023a).
- **Sự kiện nguồn tới sau khi lưu trú đã kết thúc và mọi bảng đã chốt** (bản ghi ngoại tuyến đồng bộ muộn, đính chính một liều cũ): khoản điều chỉnh vào bảng bổ sung của kỳ hiện tại (FR-002a), và hành chính được nhắc theo FR-029.
- **Quyết định giữ giường chưa có khi tới lúc chốt**: các khoản mang dấu "chờ quyết định" vẫn được kiểm tra, duyệt và chốt với hệ số tạm. Khi có quyết định, phần chênh lệch là khoản điều chỉnh.
- **Bảng giá (CFG-M02-05) hoặc phiên bản đơn giá thay đổi sau khi khoản đã sinh**: khoản đã sinh không tự tính lại (feature 000 FR-017), trừ trường hợp khoản đang "thiếu đơn giá" và phiên bản mới bao trùm ngày phát sinh.
- **Bản ghi nguồn được đính chính nhiều lần**: khoản chưa chốt luôn theo giá trị hiện hành. Khoản đã chốt nhận một điều chỉnh cho mỗi lần giá trị hiện hành đổi, bằng chênh lệch so với tổng hiện tại (gốc + các điều chỉnh).
- **Khoản đã có điều chỉnh miễn giảm, sau đó bản ghi nguồn bị hủy ghi nhận**: điều chỉnh tự tạo đưa tổng về 0. Không tạo số âm.
- **Lần giao thuốc mang theo được nhận lại một phần**: khoản của lần giao được tính lại theo số viên thực dùng nếu chưa chốt, hoặc có điều chỉnh nếu đã chốt (feature 006 FR-048a).
- **Hoạt động có thu phí bị hủy điểm danh** hoặc buổi bị hủy sau khi điểm danh: xử lý như bản ghi nguồn bị hủy.
- **Người cao tuổi đổi từ bán trú sang nội trú giữa tháng**: ngày trước ngày hiệu lực tính phí buổi theo trạng thái có mặt, từ ngày hiệu lực tính phí lưu trú ngày.
- **Bán trú đến muộn sau khi đã Vắng không báo**: trạng thái có mặt đổi thành Có mặt. Khoản buổi chưa chốt được tính lại theo hệ số 100%.
- **Dịch vụ trong gói nhưng vượt số lần của gói**: tài liệu nguồn không có hạn mức gói. Mọi lần dùng dịch vụ thuộc gói đều có số tiền 0 (xem Assumptions).
- **Khoản thiếu đơn giá khi tới lúc chốt**: khoản không kiểm tra được, nên chặn gửi chốt bảng. Hành chính được nhắc để Quản lý viện bổ sung phiên bản đơn giá.
- **Hai người cùng thao tác trên một bảng** (hành chính gửi chốt trong lúc sự kiện mới tạo khoản Nháp): khoản Nháp mới đưa bảng về Đang mở. Lệnh chốt dựa trên trạng thái cũ bị từ chối.
- **Qua đời khi đã có bảng kỳ cuối do hồ sơ kết thúc**: hồ sơ kết thúc chuyển Đã hủy (feature 004 FR-070). Kỳ cuối tính lại đến thời điểm qua đời. Các khoản ngày sinh trước sau ngày qua đời chuyển Đã hủy, hoặc thành điều chỉnh nếu đã chốt.
- **Người cao tuổi không có người đại diện có tài khoản cổng**: đề nghị mua hộ vượt hạn mức chỉ đi được qua đường Quản lý viện duyệt.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, nhật ký cho mọi thay đổi, tham số theo mã CFG, vòng đời yêu cầu phê duyệt, "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động, Bộ lập lịch chạy lại không tạo trùng. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Kỳ chi phí | 2 | Chỉ qua sự kiện ở bảng mục A |
| Bảng chi phí (một người cao tuổi, một kỳ) | 2 → 3 khi Đã chốt | Chỉ qua lệnh ở bảng mục A; không sửa, không mở lại khi Đã chốt |
| Khoản chi phí (tự sinh, nhập tay, mua hộ, điều chỉnh) | 2 → 3 khi Đã chốt | Chỉ qua lệnh ở bảng mục D; khoản Đã chốt chỉ ghi thêm khoản điều chỉnh (DBR-17) |
| Ngày lưu trú | 2 – bản ghi dẫn xuất | Chỉ do Bộ lập lịch lập và cập nhật theo dữ liệu feature 004 (FR-005a); mỗi lần cập nhật lưu lịch sử trước/sau; không có lệnh cho người dùng. Khoản đã chốt dựa trên nó được bảo vệ bằng khoản điều chỉnh, không bằng việc khóa ngày lưu trú |
| Sự kiện không sinh chi phí | 3 | Chỉ ghi thêm; dấu "đã sinh bù" do hệ thống gắn (FR-011) |
| Lịch sử tính lại của khoản | 3 | Chỉ ghi thêm |
| Đề nghị mua hộ | 2 | Theo bảng mục E |
| Lần xuất dữ liệu kế toán | 3 | Chỉ ghi thêm |
| Tham số CFG-M11-01, 02, 03 | 1 – Tham số | Cấu hình (Quản lý viện, feature 000) |

#### A. Kỳ chi phí và bảng chi phí

- **FR-001**: Kỳ chi phí MUST luôn là một tháng dương lịch (từ ngày 1 tới ngày cuối tháng); tham số MUST NOT làm thay đổi ranh giới kỳ. CFG-M11-01 là **hạn chốt**: ngày mà mọi bảng của kỳ vừa kết thúc phải Đã chốt. Giá trị mặc định \[Ngày cuối tháng\] được hiểu là ngày cuối của tháng liền sau kỳ; cơ sở có thể cấu hình ngắn hơn (ví dụ ngày 5 của tháng liền sau). Mỗi khoản chi phí MUST thuộc đúng một kỳ. *(Nguồn: 15.7, CFG-M11-01, DBR-17; Clarification 2026-09-26, đề xuất Q-132)*
- **FR-002**: Với mỗi kỳ và mỗi người cao tuổi có thời gian tính phí (FR-011) trong kỳ, hệ thống MUST có đúng một **bảng chi phí thường**. Bảng gồm mọi khoản **được ghi vào bảng** theo FR-003, kèm tổng theo loại chi phí và tổng cộng (bỏ qua khoản Đã hủy). Khoản được ghi vào bảng là khoản có ngày phát sinh trong kỳ, cộng với khoản điều chỉnh có kỳ gốc sớm hơn nhưng được ghi vào bảng này. Trong toàn spec, "khoản của bảng" và "khoản trong kỳ" hiểu theo định nghĩa này. Kỳ gốc của khoản điều chỉnh vẫn được lưu riêng (FR-025). Việc gửi chốt và chốt MUST được thực hiện trên từng bảng; điều kiện BR-M11-06 MUST xét trong phạm vi một bảng, nên khoản chưa duyệt của người này không chặn bảng của người khác. Kỳ chi phí của viện MUST chuyển Đã chốt khi mọi bảng của kỳ Đã chốt. Kỳ cuối (BR-M11-08) là bảng của kỳ chứa ngày kết thúc dự kiến, được chốt theo cùng quy tắc. *(Nguồn: 15.6, 15.7, BR-M11-06, BR-M11-08; Clarification 2026-09-26, đề xuất Q-131)*
- **FR-002a**: Ngoài bảng thường, mỗi người cao tuổi MAY có **bảng bổ sung** (trước đây gọi là "bảng sau kỳ cuối"), chỉ gồm khoản điều chỉnh. Hệ thống MUST mở bảng bổ sung của kỳ hiện tại khi cần ghi một khoản điều chỉnh cho người đó mà bảng thường của kỳ hiện tại đã chốt, hoặc người đó không có bảng thường trong kỳ hiện tại. Ví dụ: kỳ cuối đã chốt (FR-029), lưu trú đã kết thúc, hoặc người đó đã qua đời. Mỗi người cao tuổi MUST có tối đa một bảng bổ sung cho mỗi kỳ. Khoản điều chỉnh mới luôn vào bảng bổ sung của kỳ hiện tại. Bảng bổ sung của kỳ trước chưa chốt vẫn giữ các khoản đã có, không nhận thêm, và vẫn được nhắc theo FR-029. Bảng bổ sung có cùng vòng đời và điều kiện chốt như bảng thường. Nó MUST được tính khi xét kỳ của viện Đã chốt (bảng của kỳ nào thuộc kỳ đó), và MUST NOT được dùng làm kỳ so sánh ở FR-032. *(Nguồn: DBR-17, BR-M11-05, BR-M11-08; Clarification 2026-09-27, đề xuất Q-134)*
- **FR-003**: Khi một khoản có ngày phát sinh thuộc một bảng đã chốt, khoản đó MUST được ghi thành khoản điều chỉnh (mục F), không vào bảng đã chốt. Bảng nhận khoản là bảng chưa chốt của **kỳ hiện tại** (kỳ chứa ngày tạo khoản) của cùng người cao tuổi: bảng thường nếu chưa Đã chốt, ngược lại là bảng bổ sung (FR-002a). *(Nguồn: DBR-17, BR-M11-05)*
- **FR-004**: Khi một khoản Nháp mới được thêm vào bảng đang Chờ chốt, hoặc một khoản của bảng bị hệ thống đưa về Nháp, bảng MUST tự trở về Đang mở và hành chính MUST được thông báo. *(Suy ra từ BR-M11-06)*

**Bảng trạng thái bảng chi phí** *(15.6, BR-M11-06; quyền theo 4.4 dòng "Chốt kỳ, xuất kế toán")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Mở bảng thường | Đang mở | Bộ lập lịch | Đầu kỳ, hoặc ngày đầu tiên của thời gian tính phí trong kỳ (FR-011) | — |
| (chưa có) | Mở bảng bổ sung | Đang mở | Hệ thống | Theo FR-002a | — |
| Đang mở | Gửi chốt | Chờ chốt | Hành chính | Bảng thường: đã qua ngày cuối của bảng, hoặc bảng là kỳ cuối đã sinh đến ngày kết thúc dự kiến; bảng bổ sung: bất kỳ lúc nào; Bộ lập lịch đã xử lý mọi ngày của bảng; không còn khoản Nháp | Kiểm tra biến động (FR-032); báo Quản lý viện |
| Chờ chốt | Trả lại | Đang mở | Quản lý viện | Có lý do | Khoản được chọn về Nháp; báo hành chính |
| Chờ chốt | Có khoản Nháp mới hoặc khoản bị đưa về Nháp | Đang mở | Hệ thống | — | Báo hành chính (FR-004) |
| Chờ chốt | Chốt | Đã chốt | Quản lý viện | Mọi khoản Đã duyệt hoặc Đã hủy (BR-M11-06) | Mọi khoản Đã duyệt → Đã chốt; lưu tổng, người chốt, thời điểm; báo người thân có quyền xem chi phí (FR-040); sẵn sàng xuất (FR-039, FR-041); với kỳ cuối hoặc bảng bổ sung: cập nhật điều kiện của feature 004 (FR-034a) |

Đã chốt là trạng thái cuối. Quản lý viện được Trả lại từng khoản Đã kiểm tra hoặc Đã duyệt cả khi bảng còn Đang mở (bảng trạng thái khoản chi phí); khi đó bảng giữ Đang mở.

**Bảng trạng thái kỳ chi phí của viện** *(15.6, 15.7, Q-131, Q-132)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Mở kỳ | Đang mở | Bộ lập lịch | Ngày 1 của tháng | Mở bảng thường cho người có thời gian tính phí (FR-002) |
| Đang mở | Hết ngày cuối tháng | Chờ chốt | Bộ lập lịch | — | Bắt đầu tính hạn chốt CFG-M11-01 (FR-031) |
| Chờ chốt | Bảng cuối cùng của kỳ Đã chốt | Đã chốt | Hệ thống | Mọi bảng thường và bảng bổ sung thuộc kỳ Đã chốt | Kỳ sẵn sàng xuất trọn vẹn (FR-039) |

Đã chốt là trạng thái cuối. Một bảng bổ sung mở sau đó cho kỳ hiện tại không làm kỳ đã chốt mở lại, vì bảng bổ sung luôn thuộc kỳ hiện tại (FR-002a).

#### B. Sinh chi phí tự động

- **FR-005**: Khi một sự kiện nguồn ở bảng dưới xảy ra với người cao tuổi đang trong thời gian tính phí (FR-011), hệ thống MUST tự tạo khoản chi phí ở trạng thái Nháp. Khoản ghi người tạo là "Bộ lập lịch hệ thống" (AC-11) và có tham chiếu tới đúng một bản ghi nguồn. *(Nguồn: BR-M11-01, 15.2, DBR-15)*

| Sự kiện nguồn | Feature gửi | Loại chi phí | Bản ghi nguồn | Số lượng, đơn vị | Căn cứ đơn giá | Ngày phát sinh |
| --- | --- | --- | --- | --- | --- | --- |
| Một ngày lưu trú nội trú kết thúc (có mặt hoặc vắng) | 004 (dữ liệu), 010 (lập ngày lưu trú, FR-005a) | Phí lưu trú | Ngày lưu trú (FR-005a); ngày vắng kèm tham chiếu lượt vắng | 1 ngày, kèm hệ số (FR-009) | Hợp đồng hiệu lực ngày đó (FR-008) | Ngày đó |
| Trạng thái có mặt của một buổi bán trú có lịch được xác định (Có mặt / Vắng có báo / Vắng không báo) (FR-005b) | 005, 004 | Phí buổi bán trú | Trạng thái có mặt bán trú theo ngày | 1 buổi, kèm hệ số theo CFG-M02-05 | Hợp đồng (giá buổi, hoặc giá tháng chia theo FR-009) | Ngày của buổi |
| Điểm danh đến bán trú vào ngày không có lịch (buổi phát sinh, FR-005b (a)) | 005 | Phí buổi bán trú | Trạng thái có mặt bán trú theo ngày | 1 buổi, hệ số 100% | Giá buổi trong hợp đồng; nếu không có, phiên bản đơn giá "buổi bán trú" | Ngày đến |
| Công việc dịch vụ có tính phí Hoàn thành (đưa đi khám, tiêm, phục hồi ngoài gói…) | 005 | Dịch vụ | Kết quả ghi nhận công việc | Theo kết quả (mặc định 1 lần) | Hợp đồng nếu dịch vụ đăng ký; ngược lại phiên bản đơn giá | Thời điểm thực hiện (feature 005 FR-025), không phải thời điểm ghi |
| Ghi nhận dùng vật phẩm tiêu hao (tã, bỉm, sữa…) | 005 | Vật phẩm tiêu hao | Kết quả ghi nhận công việc | Số lượng đã ghi, đơn vị của vật phẩm | Như trên | Thời điểm thực hiện |
| Điểm danh có mặt ở buổi hoạt động có thu phí (chuyến đi: bản ghi điểm danh rời viện, đồng bộ spec 014) | 014 | Hoạt động | Lượt điểm danh | 1 lượt (kể cả mức "bỏ giữa chừng", Q-173; lượt Vắng, "Không ghi nhận" không tạo khoản) | Như trên | Ngày của buổi; chuyến đi: ngày rời viện thực tế |
| Liều nguồn viện Đã dùng; lần dùng PRN nguồn viện; trừ liều thuộc một lần giao mang theo đã tính phí (feature 006 FR-033) và liều nguồn gia đình gửi, kể cả thuốc mua hộ (Q-139) | 006 | Thuốc | Liều / lần dùng PRN | Số lượng theo liều | Như trên | Thời điểm dùng |
| Lần giao thuốc mang theo nguồn viện; nhận lại | 006 | Thuốc | Lần giao | Số lượng giao, trừ số nhận lại | Như trên | Thời điểm giao |
| Lượt ở lại đi qua một mốc 00:00 (mỗi mốc là một đêm, feature 012 FR-043); lượt kết thúc trước mốc 00:00 đầu tiên thì không sinh khoản | 012 | Người thân ở lại | Lượt ở lại | 1 đêm | Phiên bản đơn giá "Người thân ở lại" tại ngày bắt đầu của đêm | Ngày bắt đầu của đêm |
| Suất ăn của người thân ở lại có đăng ký ăn (chỉ khi lượt Đang ở lại; suất Đã hủy trước giờ bữa thì khoản bị hủy) | 011 | Suất ăn người thân | Suất ăn đã ghi nhận (feature 011 FR-043) | 1 suất | Phiên bản đơn giá suất ăn ngoài hợp đồng | Ngày của bữa |

- **FR-005a**: Với người cao tuổi nội trú, sau khi mỗi ngày trong thời gian tính phí kết thúc, Bộ lập lịch MUST lập đúng một **ngày lưu trú**. Ngày lưu trú gồm: người cao tuổi, hợp đồng hiệu lực ngày đó (feature 004 FR-024), ngày, có mặt hay vắng, lượt vắng và số thứ tự ngày vắng (nếu vắng), hệ số và các dấu do feature 004 xác định. Ngày lưu trú là bản ghi nguồn của khoản phí lưu trú. Nếu dữ liệu của feature 004 cho ngày đó đổi (đính chính lượt vắng, quyết định giữ giường), ngày lưu trú MUST được cập nhật và kéo theo tính lại hoặc điều chỉnh khoản (FR-020). *(Nguồn: 15.2, BR-M02-06, DBR-15)*
- **FR-005b**: Khoản phí buổi bán trú MUST được sinh khi trạng thái có mặt của buổi có lịch được xác định lần đầu (Có mặt khi điểm danh đến; Vắng có báo khi ghi báo vắng; Vắng không báo do Bộ lập lịch của feature 005 xác định sau CFG-M02-07, mặc định \[2 giờ\]; báo vắng trước CFG-M02-06, mặc định \[24 giờ\], là Vắng có báo). Khi trạng thái đổi sau đó, khoản MUST được tính lại (chưa chốt) hoặc điều chỉnh (đã chốt). Vì feature 005 luôn xác định trạng thái cho mọi buổi có lịch, không có buổi có lịch nào ở cuối ngày mà chưa có trạng thái. Hai trường hợp riêng:
  - (a) Người bán trú được điểm danh đến vào ngày không có lịch: hệ thống MUST sinh một khoản "buổi phát sinh", hệ số 100%. Đơn giá là giá buổi ghi trong hợp đồng; nếu hợp đồng tính giá tháng hoặc không ghi giá buổi, dùng phiên bản đơn giá "buổi bán trú" tại ngày đó. Khoản này nằm ngoài giá tháng và không được đếm vào số buổi có lịch ở FR-009.
  - (b) Buổi có lịch rơi vào ngày khu bán trú nghỉ (do feature 004 khai báo): buổi đó MUST NOT sinh khoản và MUST NOT được đếm vào số buổi có lịch để chia giá tháng.

  *(Nguồn: 3.4, BR-M11-01, feature 005 bảng trạng thái có mặt bán trú; Clarification 2026-09-27, đề xuất Q-140)*
- **FR-006**: Mỗi bản ghi nguồn MUST sinh tối đa một khoản đang hiệu lực cho mỗi đơn vị tính phí (một ngày, một buổi, một đêm, một liều, một kết quả, một lượt điểm danh, một suất), không kể các khoản điều chỉnh trỏ về khoản đó. Chạy lại hoặc chạy bù Bộ lập lịch MUST NOT tạo trùng. Ngày bị lỡ do gián đoạn MUST được sinh bù ở lần chạy kế tiếp. *(Nguồn: NFR-04, DBR-15, feature 000)*
- **FR-007**: Phí lưu trú của một ngày MUST được sinh sau khi ngày đó kết thúc, dùng hệ số và các dấu ("chờ quyết định", "không có dòng chính sách", "chưa vào ở") mà feature 004 đã xác định cho ngày đó. Ngoại lệ: bảng kỳ cuối được sinh trước tới ngày kết thúc dự kiến (FR-034). Khoản sinh trước mang dấu "sinh trước". Khi ngày đó thật sự kết thúc, Bộ lập lịch MUST đối chiếu với ngày lưu trú thật (FR-005a) và gỡ dấu. Nếu hệ số hoặc hợp đồng khác, khoản MUST được tính lại khi chưa chốt, hoặc tạo khoản điều chỉnh khi đã chốt (FR-027). *(Nguồn: BR-M02-06, BR-M11-08, feature 004 FR-052, FR-074)*

#### C. Đơn giá và số tiền

- **FR-008**: Đơn giá của khoản thuộc hợp đồng (phí lưu trú, phí buổi bán trú, dịch vụ đăng ký trong hợp đồng) MUST là đơn giá ghi trong nội dung hợp đồng hiệu lực tại ngày phát sinh, lấy từ feature 004 FR-024 (hợp đồng gốc + phụ lục Đã áp dụng). Đơn giá của khoản ngoài hợp đồng MUST là phiên bản đơn giá hiệu lực tại ngày phát sinh (6.4, DBR-08). Ngày chốt hay ngày kiểm tra MUST NOT ảnh hưởng tới đơn giá. Mỗi khoản MUST lưu căn cứ đơn giá (hợp đồng/phụ lục nào, hoặc phiên bản đơn giá nào). Ngày sau ngày kết thúc hợp đồng mà hợp đồng vẫn Hiệu lực với dấu "quá hạn hợp đồng" MUST dùng đơn giá của chính hợp đồng đó (Q-20, feature 004 FR-029). Khoản "Mua hộ" không theo quy tắc này: số tiền là số thực tế trên chứng từ mua (FR-024), kể cả khi mục có trong danh mục. *(Nguồn: BR-M11-02, DBR-16, Q-20, Q-25)*
- **FR-009**: Với hợp đồng tính theo tháng, phí lưu trú của ngày D MUST là: phí ngày cơ sở × hệ số chính sách vắng của ngày D, làm tròn tới đồng. Phí ngày cơ sở = giá tháng theo hợp đồng hiệu lực ngày D / số ngày của tháng chứa D, làm tròn tới đồng. Riêng ngày cuối tháng: phí ngày cơ sở = giá tháng − (số ngày của tháng − 1) × phí ngày cơ sở đã làm tròn, để một tháng đủ ngày, cùng giá, không vắng có tổng đúng bằng giá tháng. Với hợp đồng bán trú tính giá tháng: phí buổi của buổi có lịch B MUST là phí buổi cơ sở × hệ số trạng thái có mặt của B, làm tròn tới đồng. Phí buổi cơ sở = giá tháng / số buổi có lịch trong tháng chứa B (theo lịch đến của hợp đồng hiệu lực, trừ buổi trùng ngày khu bán trú nghỉ; không đếm buổi phát sinh, FR-005b), làm tròn tới đồng. Riêng buổi có lịch cuối cùng của tháng: phí buổi cơ sở = giá tháng − (số buổi có lịch − 1) × phí buổi cơ sở đã làm tròn. Với hợp đồng tính theo ngày hoặc theo buổi: số tiền = đơn giá trực tiếp × hệ số, làm tròn tới đồng. *(Nguồn: 15.7, BR-M11-09; Clarification 2026-09-27, đề xuất Q-138)*
- **FR-010**: Khi dịch vụ hoặc vật phẩm của khoản nằm trong gói của hợp đồng hiệu lực tại ngày phát sinh, khoản MUST được tạo với dấu "thuộc gói" và số tiền 0. Khoản vẫn giữ số lượng, đơn vị, nguồn, và được kiểm tra, duyệt, chốt như mọi khoản khác. *(Nguồn: BR-M11-03, DBR-16)*
- **FR-011**: Thời gian tính phí của một người cao tuổi MUST: bắt đầu từ ngày bắt đầu hợp đồng với nội trú, kể cả khi vào ở muộn (các ngày trước ngày Hoàn tất tiếp nhận mang dấu "chưa vào ở" và có hệ số 100%, vì Q-22 tính phí đầy đủ từ ngày bắt đầu hợp đồng); bắt đầu từ buổi có lịch đầu tiên từ ngày bắt đầu hợp đồng với bán trú; kết thúc sau ngày kết thúc dự kiến của hồ sơ kết thúc lưu trú (FR-034), sau ngày Hủy tiếp nhận (feature 004 FR-075a), hoặc tại thời điểm qua đời (FR-036). Sự kiện nguồn ngoài thời gian tính phí MUST NOT sinh khoản và MUST được ghi vào danh sách "sự kiện không sinh chi phí" (người cao tuổi, sự kiện, bản ghi nguồn, thời điểm, lý do không sinh). Danh sách này chỉ để xem, không có lệnh sửa. Khi thời gian tính phí thay đổi làm sự kiện rơi vào trong thời gian tính phí (ví dụ hồ sơ kết thúc bị hủy, ngày kết thúc dự kiến dời muộn hơn), hệ thống MUST sinh bù khoản và đánh dấu mục là "đã sinh bù". Hành chính không tự tính phí cho sự kiện trong danh sách. Nếu thời gian tính phí sai, phải sửa ở feature 004. *(Nguồn: Q-22, Q-27, BR-M11-08, feature 004 FR-074)*
- **FR-012**: Khi không xác định được đơn giá (không có phiên bản đơn giá hiệu lực tại ngày phát sinh, hoặc hợp đồng không ghi đơn giá cho khoản thuộc hợp đồng), khoản MUST vẫn được tạo ở Nháp với dấu "thiếu đơn giá" và chưa có số tiền. Hành chính MUST được thông báo (mức Nhẹ, feature 009). Quản lý viện MUST tạo được phiên bản đơn giá có ngày hiệu lực trong quá khứ, chỉ cho khoảng thời gian mà dịch vụ/vật phẩm đó chưa có phiên bản nào (không chồng khoảng, DBR-08); phiên bản đã có MUST NOT bị sửa hay rút ngắn. Khi có phiên bản bao trùm ngày phát sinh, hệ thống MUST tự tính lại khoản thiếu đơn giá chưa chốt. Khoản đã được sinh bằng đơn giá khác thì không tính lại (feature 000 FR-017). Đơn giá của khoản tự sinh MUST NOT do người dùng nhập. Quy tắc này không áp cho khoản nhập tay (FR-021) và khoản mua hộ (FR-024), vốn do Hành chính nhập. *(Nguồn: BR-M11-01, BR-M11-02, 6.4; Clarification 2026-09-27, đề xuất Q-135)*
- **FR-013**: Mọi số tiền MUST tính bằng đồng, là số nguyên (NFR-11). Mọi phép "làm tròn tới đồng" trong spec MUST làm tròn nửa lên: phần lẻ từ 0,5 đồng trở lên làm tròn lên, dưới 0,5 đồng bỏ; với số âm, làm tròn theo giá trị tuyệt đối. Số tiền của khoản tự sinh và số tiền điều chỉnh do hệ thống tạo MUST NOT được người dùng nhập hay sửa. *(Nguồn: 1.3 "nhân viên chủ yếu xác nhận")*

#### D. Vòng đời khoản chi phí

- **FR-014**: Mỗi khoản chi phí MUST gồm: người cao tuổi; kỳ và bảng; loại chi phí; dịch vụ/vật phẩm/hoạt động hoặc mô tả (với nhập tay); ngày phát sinh; số lượng; đơn vị; đơn giá; căn cứ đơn giá; hệ số (với phí lưu trú, phí buổi); dấu "thuộc gói"; số tiền; nguồn sinh (tự động / nhập tay / mua hộ / điều chỉnh); tham chiếu bản ghi nguồn hoặc lý do nhập tay; người tạo; ghi chú; các dấu (thiếu đơn giá, chờ quyết định, không có dòng chính sách, chưa vào ở, sinh trước, phát sinh thuộc kỳ đã chốt, vượt số tiền đã đồng ý, ảnh hưởng kết thúc lưu trú); trạng thái; khoản gốc và kỳ gốc (với khoản điều chỉnh). *(Nguồn: 15.3, DBR-15, DBR-17)*
- **FR-014a**: Loại chi phí MUST thuộc danh sách chuẩn sau, dùng chung cho tổng theo loại (FR-002), cảnh báo biến động (FR-032), chi phí tạm tính (FR-038), file kế toán (FR-039) và báo cáo 18.4: Phí lưu trú; Phí buổi bán trú; Dịch vụ; Vật phẩm tiêu hao; Hoạt động; Thuốc; Người thân ở lại; Suất ăn người thân; Mua hộ; Khoản khác (nhập tay). "Điều chỉnh" là nguồn sinh, không phải loại chi phí. Khoản điều chỉnh mang loại của khoản gốc, hoặc loại của bản ghi nguồn nếu là điều chỉnh bổ sung. *(Nguồn: 15.2, 18.4)*
- **FR-015**: Hành chính MUST kiểm tra được từng khoản hoặc hàng loạt theo bộ lọc (người cao tuổi, loại chi phí, dấu, nguồn sinh). Khi kiểm tra hàng loạt, khoản không đủ điều kiện MUST bị bỏ qua kèm lý do, các khoản còn lại vẫn được kiểm tra. Quản lý viện MUST duyệt được từng khoản hoặc hàng loạt theo bảng, cùng quy tắc bỏ qua. Riêng khoản nhập tay, khoản mua hộ và khoản điều chỉnh do Hành chính lập MUST bị loại khỏi duyệt hàng loạt. Quản lý viện MUST duyệt từng khoản loại này sau khi đã được hiển thị lý do và bằng chứng. *(Nguồn: 15.6, BR-M11-04 "phải được quản lý duyệt", 15.6 "khoản điều chỉnh phải có lý do, phải có người duyệt"; suy ra từ quy mô NFR-01 cho phần hàng loạt)*
- **FR-016**: Khoản tự sinh MUST NOT bị người dùng hủy hay sửa trực tiếp. Sai sót của khoản tự sinh MUST được xử lý bằng đính chính bản ghi nguồn ở feature sở hữu (FR-020) hoặc bằng khoản điều chỉnh (mục F). Hành chính MUST hủy được khoản nhập tay, khoản mua hộ và khoản điều chỉnh do mình lập khi khoản còn Nháp, bắt buộc có lý do. Khoản điều chỉnh do hệ thống tạo (FR-027) MUST NOT bị người dùng hủy. Nếu nó sai vì bản ghi nguồn bị đính chính nhầm, phải đính chính lại bản ghi nguồn; hệ thống khi đó tính lại khoản điều chỉnh (nếu còn Nháp) hoặc tạo khoản điều chỉnh bù (nếu đã chốt), theo FR-027. *(Nguồn: BR-M11-01, BR-M11-05, 1.5 nhóm 2)*

**Bảng trạng thái khoản chi phí** *(15.6, BR-M11-04, 05, 06; quyền theo 4.4 dòng "Chi phí, khoản điều chỉnh", "Chốt kỳ, xuất kế toán")*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Sinh tự động | Nháp | Bộ lập lịch / hệ thống | Sự kiện nguồn ở FR-005, trong thời gian tính phí (FR-011); chưa có khoản cho cùng đơn vị tính phí (FR-006) | Xác định đơn giá, số tiền (mục C); vào bảng theo FR-003 |
| (chưa có) | Lập khoản nhập tay | Nháp | Hành chính | Mục ngoài danh mục; có lý do (FR-021) | Không được duyệt hàng loạt (FR-015) |
| (chưa có) | Ghi đã mua (từ đề nghị mua hộ) | Nháp | Hành chính | Đề nghị ở trạng thái cho phép mua (mục E) | Nguồn là đề nghị mua hộ |
| (chưa có) | Lập khoản điều chỉnh | Nháp | Hành chính; hệ thống (FR-027) | Mục F | Trỏ về khoản gốc |
| Nháp | Tính lại | Nháp | Hệ thống | Một trong các nguyên nhân: bản ghi nguồn đổi giá trị hiện hành (FR-020); ngày lưu trú được cập nhật (FR-005a), gồm quyết định giữ giường; đối chiếu khoản sinh trước (FR-007); trạng thái có mặt bán trú đổi, hoặc khai báo ngày khu nghỉ (FR-005b); nhận lại thuốc mang theo; có đơn giá cho khoản "thiếu đơn giá" (FR-012); đổi ngày kết thúc dự kiến (FR-034) | Lưu lịch sử trước/sau và căn cứ |
| Nháp | Kiểm tra | Đã kiểm tra | Hành chính | Có số tiền (không "thiếu đơn giá") | Ghi người kiểm tra, thời điểm |
| Đã kiểm tra | Bỏ kiểm tra | Nháp | Hành chính | Bảng Đang mở (không Chờ chốt); có lý do | Ghi người thực hiện, lý do |
| Nháp | Hủy | Đã hủy | Hệ thống (bản ghi nguồn bị hủy ghi nhận, bị hủy, nằm ngoài thời gian tính phí sau khi đổi ngày kết thúc hoặc qua đời); Hành chính (chỉ khoản nhập tay, mua hộ, điều chỉnh do mình lập) | Có lý do hoặc tham chiếu sự kiện gây hủy | Loại khỏi tổng |
| Đã kiểm tra, Đã duyệt | Bản ghi nguồn thay đổi / bị hủy | Nháp (rồi Tính lại hoặc Hủy) | Hệ thống | Khoản chưa chốt | Báo hành chính; bảng về Đang mở (FR-004) |
| Đã kiểm tra | Duyệt | Đã duyệt | Quản lý viện | — | Ghi người duyệt, thời điểm; với nhập tay, mua hộ, điều chỉnh đây là lần duyệt bắt buộc (BR-M11-04, 15.6) |
| Đã kiểm tra | Trả lại | Nháp | Quản lý viện | Có lý do | Báo hành chính |
| Đã duyệt | Trả lại | Nháp | Quản lý viện | Bảng chưa chốt; có lý do | Báo hành chính |
| Đã duyệt | Chốt bảng | Đã chốt | Hệ thống, khi Quản lý viện chốt bảng | Mục A | Khoản thành nhóm 3 |

Đã chốt và Đã hủy là trạng thái cuối. Khoản Đã chốt MUST NOT có lệnh sửa, hủy, trả lại hay đính chính theo feature 000 FR-026 (c). Mọi thay đổi đi qua khoản điều chỉnh (BR-M15-03, DBR-17).

- **FR-017**: Quản lý viện MUST NOT thực hiện lệnh Kiểm tra, Bỏ kiểm tra, lập khoản nhập tay, lập khoản điều chỉnh, lập đề nghị mua hộ hay gửi chốt. Hành chính MUST NOT thực hiện lệnh Duyệt, Trả lại, Chốt. *(Nguồn: 4.4 dòng "Chi phí, khoản điều chỉnh" và "Chốt kỳ, xuất kế toán": HC T, QL D)*
- **FR-018**: Hành chính MUST xem được lịch sử của một khoản: các lần tính lại (giá trị trước/sau, căn cứ), người kiểm tra, người duyệt, người trả lại kèm lý do, các khoản điều chỉnh trỏ về khoản đó và tổng hiện hành (gốc + điều chỉnh). *(Nguồn: 15.4, 19.4)*
- **FR-019**: Với khoản chi phí loại Thuốc (kể cả khoản điều chỉnh và khoản mua hộ là thuốc), mọi người xem chi phí MUST chỉ thấy: loại "Thuốc", mã vật phẩm, ngày phát sinh, số lượng, đơn vị, đơn giá, thành tiền, mã tham chiếu nguồn. Tên thuốc và hàm lượng MUST chỉ hiện với người đồng thời có quyền xem thuốc của người cao tuổi đó: Quản lý viện; người thân có quyền xem sức khỏe có tác dụng (feature 012 FR-011). Hành chính MUST NOT thấy tên thuốc, kể cả qua lịch sử tính lại, nhật ký, thông báo hay bộ lọc (19.3, feature 002). File kế toán MUST dùng mã vật phẩm trong cột mô tả. Hành chính kiểm tra khoản thuốc bằng đối chiếu mã, số lượng, đơn giá với bản ghi nguồn đã được che tên. *(Nguồn: 19.3, BR-M10-01, 23; Clarification 2026-09-26, đề xuất Q-133)*
- **FR-020**: Khi bản ghi nguồn bị đính chính, bị hủy ghi nhận hoặc bị hủy ở feature sở hữu, hệ thống MUST xử lý khoản tương ứng: nếu khoản chưa chốt thì tính lại theo giá trị hiện hành hoặc chuyển Đã hủy (theo bảng mục D); nếu khoản đã chốt thì tạo khoản điều chỉnh (FR-027). *(Nguồn: BR-M11-05, feature 000 FR-030)*

#### E. Chi phí nhập tay và mua hộ

- **FR-021**: Hành chính MUST lập được khoản nhập tay chỉ khi mục không có trong danh mục dịch vụ, vật phẩm của viện (kể cả mục đã Ngừng hiệu lực). Khoản gồm: mô tả, ngày phát sinh (không muộn hơn hôm nay), số lượng, đơn vị, đơn giá, lý do bắt buộc, bằng chứng nếu có. *(Nguồn: BR-M11-04, 15.2 "Khoản phát sinh khác")*
- **FR-022**: Khoản nhập tay và khoản mua hộ MUST NOT được tính vào chi phí tạm tính hiển thị cho người thân khi chưa Đã duyệt. *(Nguồn: BR-M11-04 "phải được quản lý duyệt"; Clarification 2026-09-27, đề xuất Q-136)*
- **FR-023**: Hành chính MUST lập được **đề nghị mua hộ** cho người cao tuổi, gồm: mục cần mua (trong hoặc ngoài danh mục), số lượng, số tiền dự kiến, lý do, người yêu cầu (nhân viên hoặc người thân). Khi số tiền dự kiến vượt CFG-M11-02 (mặc định \[500.000 đồng\]), đề nghị MUST được người đại diện đồng ý qua cổng hoặc được Quản lý viện duyệt trước khi mua. Khi không vượt, đề nghị được phép mua ngay. Hạn mức xét theo giá trị CFG-M11-02 hiện hành lúc gửi đề nghị. Thay đổi tham số sau đó MUST NOT đổi trạng thái của đề nghị đã gửi (feature 000 FR-017, FR-018). Người thân không lập đề nghị qua cổng ở giai đoạn này: yêu cầu của người thân được gửi trực tiếp tới Hành chính, Hành chính lập đề nghị và ghi người thân là người yêu cầu. Thuốc mua hộ là thuốc của gia đình, được mua bằng khoản "Mua hộ". Khi đưa vào dùng, thuốc MUST được tiếp nhận ở feature 006 với nguồn "gia đình gửi", nên liều của nó MUST NOT sinh khoản "Thuốc". Thuốc chỉ được tính phí một lần, ở khoản Mua hộ. *(Nguồn: BR-M11-07, UC-79, 15.2 "thuốc mua hộ", BR-M07-14 "liều có nguồn viện cung cấp"; Clarification 2026-09-27, đề xuất Q-139)*
- **FR-023a**: Yêu cầu đồng ý MUST được gửi tới mọi người đại diện Hiệu lực tại thời điểm gửi. Khi một người đại diện thôi làm đại diện, hoặc quan hệ của họ hết hiệu lực trong lúc đề nghị Chờ đồng ý, người đó MUST mất quyền đồng ý đề nghị này. Đề nghị giữ trạng thái Chờ đồng ý cho những người đại diện còn lại và Quản lý viện. Khi không còn người đại diện nào có tài khoản cổng, chỉ Quản lý viện quyết định được, và Hành chính MUST được thông báo. *(Suy ra từ BR-M11-07; feature 012 FR-005)*

**Bảng trạng thái đề nghị mua hộ** *(BR-M11-07)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Lập đề nghị | Nháp | Hành chính | — | — |
| Nháp | Gửi | Được phép mua | Hành chính | Số tiền dự kiến ≤ CFG-M11-02 | — |
| Nháp | Gửi | Chờ đồng ý | Hành chính | Số tiền dự kiến > CFG-M11-02 | Yêu cầu đồng ý gửi người đại diện qua cổng (feature 012, 009); vào danh sách chờ duyệt của Quản lý viện |
| Chờ đồng ý | Đồng ý | Được phép mua | Một người đại diện (qua cổng) hoặc Quản lý viện | Quyết định đầu tiên được ghi nhận (feature 000 FR-040) | Lưu người quyết định, kênh, thời điểm |
| Chờ đồng ý | Từ chối | Từ chối | Người đại diện hoặc Quản lý viện | Có lý do | Báo hành chính |
| Nháp, Chờ đồng ý, Được phép mua | Hủy | Hủy | Hành chính | Chưa ghi đã mua; có lý do | Báo người đã đồng ý (nếu có) |
| Được phép mua | Ghi đã mua | Đã mua | Hành chính | Số tiền thực tế, ngày mua; bằng chứng mua | Tạo khoản "Mua hộ" Nháp trỏ về đề nghị; gắn dấu "vượt số tiền đã đồng ý" nếu số thực tế lớn hơn số dự kiến đã được đồng ý hoặc duyệt |

Từ chối, Hủy, Đã mua là trạng thái cuối. Đề nghị mua hộ là đối tượng nhóm 2 riêng của feature này. Nó không phải một loại yêu cầu phê duyệt dùng chung của feature 000 FR-031, vì người quyết định có thể là người đại diện, và có trạng thái Được phép mua, Đã mua. Đề nghị mua hộ mượn hai quy tắc của feature 000: nhắc khi Chờ đồng ý quá CFG-M15-05 (mặc định \[48 giờ\]) và báo Quản lý viện khi quá CFG-M15-06 (mặc định \[96 giờ\]) theo FR-031a; chỉ ghi nhận quyết định đầu tiên theo FR-040. Tên "Chờ đồng ý" được giữ khác với "Chờ xác nhận" của feature 012, vì đây là đồng ý chi tiền, không phải xác nhận thay đổi quyền.

- **FR-024**: "Ghi đã mua" MUST NOT bị chặn khi số tiền thực tế vượt số tiền được phép. Số tiền được phép là số dự kiến đã được đồng ý hoặc duyệt, hoặc số dự kiến với đề nghị được phép mua ngay. Khi vượt, khoản "Mua hộ" MUST mang dấu "vượt số tiền đã đồng ý" và hiển thị số được phép bên cạnh số thực tế. Quản lý viện MUST nhập lý do chấp nhận phần vượt khi duyệt khoản đó; không có lý do thì lệnh Duyệt bị chặn. Khi khoản được duyệt, hệ thống MUST yêu cầu feature 009 báo người đại diện (mức Nhẹ, loại thông tin "chi phí") số được phép, số thực tế và lý do. Quy tắc áp dụng cả khi phần vượt làm số thực tế vượt CFG-M11-02 mà đề nghị trước đó không cần đồng ý. Hệ thống MUST NOT tự cắt số tiền về số được phép. *(Nguồn: BR-M11-07; Clarification 2026-09-27, đề xuất Q-137)*

#### F. Khoản điều chỉnh

- **FR-025**: Khoản điều chỉnh MUST là một khoản chi phí mới với số tiền âm hoặc dương, gồm: khoản gốc và kỳ gốc (bắt buộc, trừ khoản điều chỉnh bổ sung ở FR-003, FR-029, lúc đó là bản ghi nguồn và kỳ gốc), loại điều chỉnh (sửa sai sau chốt / theo bản ghi nguồn thay đổi / miễn giảm / bổ sung phát sinh muộn), lý do bắt buộc, số tiền chênh lệch. Khoản gốc MUST giữ nguyên. *(Nguồn: DBR-17, 15.6)*
- **FR-026**: Hành chính MUST lập được khoản điều chỉnh cho bất kỳ khoản chưa hủy nào, đã chốt hay chưa chốt (ví dụ miễn giảm theo Q-27). Tổng của khoản gốc và mọi khoản điều chỉnh chưa hủy trỏ về nó MUST NOT âm. Khoản điều chỉnh cho một khoản gốc chưa chốt MUST nằm trong cùng bảng với khoản gốc, để được chốt cùng nhau. Khoản điều chỉnh cho một khoản gốc đã chốt MUST theo FR-003. *(Nguồn: UC-63, Q-27, feature 004 FR-075a)*
- **FR-027**: Khi giá trị hiện hành của bản ghi nguồn đổi sau khi khoản gốc đã chốt (đính chính, hủy ghi nhận, quyết định giữ giường, nhận lại thuốc mang theo, đổi ngày kết thúc dự kiến, áp dụng yêu cầu đổi phòng/giường sau chuyển giường y tế theo feature 003 FR-030), hệ thống MUST tự tạo khoản điều chỉnh ở Nháp. Số tiền là chênh lệch giữa số tiền tính theo giá trị hiện hành và tổng hiện hành (gốc + các điều chỉnh đã có). Khoản trỏ về khoản gốc và sự kiện gây ra, và nằm ở bảng theo FR-003. Hành chính MUST được thông báo. BR-M11-05 viết "khoản Điều chỉnh chờ duyệt". Spec hiểu câu đó theo 15.6 ("khoản Điều chỉnh mới đi lại vòng đời này"): khoản bắt đầu ở Nháp, được Hành chính kiểm tra rồi Quản lý viện duyệt; không đi thẳng tới bước duyệt (xem điểm báo lại 13). *(Nguồn: BR-M11-05, DBR-17, 15.6)*
- **FR-028**: Khoản điều chỉnh MUST đi đủ vòng đời mục D (kiểm tra, duyệt, chốt trong bảng chứa nó). Khoản điều chỉnh chưa Đã chốt MUST NOT thay đổi số liệu của kỳ gốc trong bảng, trong file kế toán hay trên cổng người thân. *(Nguồn: 15.6 "khoản Điều chỉnh mới đi lại vòng đời này")*

#### G. Chốt kỳ và kỳ cuối

- **FR-029**: Sau khi kỳ cuối đã chốt, sự kiện nguồn trong thời gian tính phí còn lại (tới thời điểm Kết thúc lưu trú hoặc qua đời) MUST tạo khoản điều chỉnh bổ sung vào **bảng bổ sung** (FR-002a) của người cao tuổi. Hành chính MUST được nhắc mỗi CFG-M02-09 (mặc định \[1 ngày\]) cho tới khi bảng đó được chốt. Quy tắc nhắc này áp cho mọi bảng bổ sung, kể cả bảng bổ sung mở sau khi lưu trú đã kết thúc. Các khoản này MUST NOT làm điều kiện "chi phí kỳ cuối đã chốt" của feature 004 trở về Chưa đạt và MUST NOT chặn lệnh Kết thúc lưu trú. Ngoại lệ là đổi ngày kết thúc dự kiến (FR-034) và quá ngày kết thúc dự kiến (FR-035): hai trường hợp này vẫn làm điều kiện về Chưa đạt, theo FR-034a. *(Nguồn: BR-M11-08; Clarification 2026-09-27, đề xuất Q-134)*
- **FR-030**: Khi còn khoản Nháp, hoặc khoản Đã kiểm tra chưa duyệt, lệnh Chốt MUST bị chặn và hệ thống MUST liệt kê các khoản đó. *(Nguồn: BR-M11-06)*
- **FR-031**: Khi tới hạn chốt theo CFG-M11-01 (xem FR-001) mà bảng của một kỳ đã kết thúc chưa Đã chốt, Bộ lập lịch MUST nhắc hành chính (với bảng Đang mở) hoặc Quản lý viện (với bảng Chờ chốt) mỗi ngày cho tới khi chốt. Nếu bảng Đang mở còn khoản "thiếu đơn giá", Quản lý viện MUST cũng được nhắc mỗi ngày, vì chỉ Quản lý viện tạo được phiên bản đơn giá (FR-012). *(Nguồn: CFG-M11-01, Q-132, Q-135)*
- **FR-032**: Khi hành chính gửi chốt một bảng, hệ thống MUST so sánh tổng của bảng với tổng của bảng kỳ liền trước của cùng người cao tuổi. Nếu tổng tăng quá CFG-M11-03 (mặc định \[30%\]), hệ thống MUST cảnh báo hành chính (mức Nhẹ, feature 009) kèm tỷ lệ tăng và ba loại chi phí tăng nhiều nhất. Tổng để so sánh là tổng các khoản chưa hủy của **bảng thường**, gồm khoản điều chỉnh có kỳ gốc là chính kỳ đó. Không gồm khoản điều chỉnh có kỳ gốc khác, không gồm bảng bổ sung. Không so sánh khi kỳ liền trước không có bảng thường, hoặc một trong hai bảng không đủ tháng. "Đủ tháng" nghĩa là thời gian tính phí (FR-011) phủ từ ngày 1 tới ngày cuối tháng; ngày vắng vẫn nằm trong thời gian tính phí. Ngưỡng là giá trị CFG-M11-03 hiện hành lúc gửi chốt. Cảnh báo MUST NOT chặn việc gửi chốt. *(Nguồn: BR-M11-07, feature 000 FR-018)*
- **FR-033**: Chốt bảng MUST theo "hoặc toàn bộ, hoặc không": mọi khoản Đã duyệt chuyển Đã chốt, tổng được lưu và bảng Đã chốt trong cùng một lần. Lệnh chốt dựa trên trạng thái bảng đã cũ MUST bị từ chối. *(Nguồn: feature 000)*
- **FR-034**: Khi feature 004 báo hồ sơ kết thúc lưu trú được lập với ngày kết thúc dự kiến E, hệ thống MUST: dừng sinh chi phí tự động cho các ngày sau E; đánh dấu bảng của kỳ chứa E là **kỳ cuối**, tính đến E; sinh trước phí lưu trú các ngày tới E theo hợp đồng và hệ số hiện biết. Khi E đổi: nếu kỳ cuối chưa chốt thì sinh thêm hoặc hủy khoản ngày cho khớp E mới; nếu đã chốt thì tạo khoản điều chỉnh (FR-027) mang dấu "ảnh hưởng kết thúc lưu trú", và điều kiện chi phí về Chưa đạt theo FR-034a. Khi hồ sơ kết thúc bị hủy, sinh chi phí MUST tiếp tục và các ngày bị bỏ qua MUST được sinh bù. *(Nguồn: BR-M11-08, Q-21, feature 004 FR-066)*
- **FR-034a**: Feature này MUST cung cấp cho feature 004 trạng thái của điều kiện "chi phí kỳ cuối đã chốt". Điều kiện Đạt khi đồng thời thỏa: (1) bảng kỳ cuối Đã chốt; (2) không còn khoản điều chỉnh chưa chốt mang dấu "ảnh hưởng kết thúc lưu trú". Dấu này MUST được gắn cho khoản điều chỉnh sinh ra do đổi ngày kết thúc dự kiến (FR-034) hoặc do quá ngày kết thúc dự kiến (FR-035). Khoản điều chỉnh bổ sung theo FR-029 (sự kiện thường sau khi kỳ cuối đã chốt) MUST NOT mang dấu này. Điều kiện chuyển lại Đạt ngay khi bảng chứa khoản cuối cùng có dấu được chốt. *(Nguồn: BR-M11-08, Q-21, Q-26, Q-134, feature 004 FR-066, FR-066a)*
- **FR-035**: Khi hết ngày E mà lệnh Kết thúc lưu trú chưa thực hiện, hệ thống MUST sinh bù từ ngày E + 1 và tiếp tục sinh hằng ngày cho tới khi có ngày kết thúc dự kiến mới. Nếu kỳ cuối đã chốt, các khoản này là khoản điều chỉnh bổ sung mang dấu "ảnh hưởng kết thúc lưu trú" (FR-034a). *(Nguồn: BR-M11-08, Q-26, feature 004 FR-066a)*
- **FR-036**: Khi người cao tuổi qua đời, hệ thống MUST dừng sinh chi phí tự động từ thời điểm qua đời. Ngày qua đời vẫn được tính phí lưu trú trọn ngày, theo hệ số của ngày đó (Q-141). "Dừng từ thời điểm qua đời" áp cho mọi sự kiện nguồn có thời điểm (giờ) sau thời điểm qua đời, và cho mọi ngày lưu trú sau ngày qua đời. Phí lưu trú chỉ có ngày phát sinh, không có mốc giờ. Sự kiện nguồn có thời điểm phát sinh trước thời điểm qua đời nhưng được ghi sau vẫn sinh khoản. Khoản ngày đã sinh trước cho các ngày sau ngày qua đời MUST chuyển Đã hủy, hoặc được điều chỉnh nếu đã chốt. Khi kỳ cuối và mọi bảng bổ sung **đang tồn tại tại thời điểm đó** Đã chốt, hệ thống MUST báo feature 004 để mục "chốt chi phí kỳ cuối" của danh sách việc sau qua đời chuyển Hoàn thành. Bảng bổ sung mở sau thời điểm này (sự kiện tới muộn) MUST NOT làm mục đó quay lại Chưa hoàn thành, vì hồ sơ lưu trú có thể đã đóng. Bảng đó chỉ được nhắc theo FR-029. *(Nguồn: BR-M11-08, 6.7, feature 004 FR-070, FR-071; Clarification 2026-09-27, đề xuất Q-141)*

#### H. Cung cấp cho người thân và kế toán

- **FR-037**: Hệ thống MUST cung cấp cho feature 012 **bảng chi phí** của mỗi bảng Đã chốt, gồm từng khoản (theo FR-019 với khoản thuốc), khoản điều chỉnh kèm kỳ gốc, tổng theo loại và tổng cộng. Chỉ người thân có quyền xem chi phí được xem (feature 012 FR-046). *(Nguồn: 15.6 "Gửi người thân", 14.5)*
- **FR-038**: Hệ thống MUST cung cấp **chi phí tạm tính** của một người cao tuổi cho một khoảng ngày bất kỳ trong kỳ đang mở (dùng cho cổng và bản tin của feature 012 FR-052): tổng theo loại chi phí của các khoản chưa hủy, thuộc bảng chưa chốt, ở mọi trạng thái Nháp, Đã kiểm tra, Đã duyệt. Khoản được xếp vào khoảng ngày theo ngày phát sinh. Riêng khoản điều chỉnh có kỳ gốc đã chốt được xếp theo ngày tạo khoản, nên không làm đổi số của kỳ gốc (FR-028). Không cộng khoản nhập tay và mua hộ chưa Đã duyệt (FR-022). Tạm tính không kèm chi tiết từng khoản và có nhãn "tạm tính, chưa chốt". Khoản mang dấu "thiếu đơn giá" không có số tiền nên không được cộng; khi có, tổng MUST kèm ghi chú "còn khoản chưa có đơn giá". *(Nguồn: feature 012 FR-046, FR-052; Clarification 2026-09-27, đề xuất Q-136)*
- **FR-038a** *(mới 2026-09-28, đồng bộ spec 017)*: Hệ thống MUST cung cấp cho feature 017 **tổng chi phí chưa chốt** của một người cao tuổi: tổng các khoản thuộc **mọi** bảng chưa Đã chốt của người đó (bảng thường của kỳ hiện tại và kỳ trước còn Đang mở hoặc Chờ chốt, bảng bổ sung), theo cùng thành phần của FR-038 (Q-136): khoản chưa hủy, không cộng khoản nhập tay và mua hộ chưa Đã duyệt, không cộng khoản thiếu đơn giá. Giá trị MUST được tính lại mỗi khi một khoản được tạo, tính lại, hủy, hoặc một bảng Đã chốt. *(Nguồn: BR-M11-13, 15.9, Q-136; feature 017 FR-016)*
- **FR-039** *(đã chỉnh sửa 2026-09-28, Q-212)*: **Kế toán** MUST xuất được dữ liệu cho kế toán theo kỳ, gồm mọi khoản Đã chốt của các bảng Đã chốt trong kỳ, với các cột: mã người cao tuổi, họ tên, kỳ, loại chi phí, mô tả (với khoản thuốc là mã vật phẩm, FR-019), ngày phát sinh, số lượng, đơn vị, đơn giá, thành tiền, dấu "thuộc gói", mã tham chiếu nguồn, và với khoản điều chỉnh: mã khoản gốc, kỳ gốc, loại điều chỉnh. Định dạng là Excel/CSV theo Q-05. Bốn cột ngày phát sinh, đơn vị, dấu "thuộc gói", loại điều chỉnh được thêm so với mục 23 và cần kế toán của cơ sở (người quyết định Q-05) xác nhận. File MUST ghi danh sách người cao tuổi có bảng chưa chốt trong kỳ. MUST NOT xuất khoản hoặc bảng chưa chốt. *(Nguồn: 23, UC-64, Q-05)*
- **FR-040**: Khi một bảng Đã chốt, hệ thống MUST yêu cầu feature 009 thông báo mức Trung bình, loại thông tin "chi phí", tới người thân có quyền xem chi phí của người cao tuổi đó. *(Nguồn: 15.6, feature 009)*
- **FR-041**: Mỗi lần xuất MUST được ghi lại: người xuất, thời điểm, kỳ, phạm vi, danh sách bảng có trong file, số khoản, tổng số tiền. Mỗi bảng MUST lưu lần xuất đầu tiên có chứa nó. Kế toán MUST chọn được phạm vi xuất: (a) toàn bộ bảng đã chốt của kỳ; (b) chỉ các bảng đã chốt chưa từng được xuất. Mỗi file MUST có dòng tổng kiểm soát (số bảng, số khoản, tổng tiền) và đánh dấu từng bảng là "xuất lần đầu" hay "đã xuất trước đó", để kế toán không nhập trùng. Xuất lại cùng kỳ, cùng phạm vi (a), khi không có bảng mới chốt MUST cho cùng nội dung. *(Nguồn: 19.4, 23; suy ra từ 15.1 "cung cấp dữ liệu cho kế toán")*
- **FR-042** *(đã chỉnh sửa 2026-09-28, Q-211)*: **Feature này** MUST NOT ghi nhận thu tiền, công nợ, số đã thanh toán, hóa đơn hay bút toán, và đặt cọc không được trừ vào bảng chi phí. Việc thu tiền, số dư, công nợ ("còn nợ"), trừ bảng chi phí vào số dư và quyết toán thuộc **feature 017** (15.9): khi một bảng Đã chốt, feature này chỉ gửi sự kiện cho feature 017 (bảng giao tiếp "Gửi 017"). Hóa đơn và bút toán vẫn nằm ngoài hệ thống. *(Nguồn: 1.2, 6.5, 15.1, 15.9)*

#### I. Quyền

- **FR-043**: Quyền theo Permission Matrix 4.4. Đây là tập lệnh đầy đủ và MUST khớp cột "Ai thực hiện" của ba bảng trạng thái:
  - **Hành chính** (T, toàn viện): Kiểm tra, Bỏ kiểm tra, lập khoản nhập tay, lập khoản điều chỉnh, Hủy khoản do mình lập, lập / gửi / hủy đề nghị mua hộ, Ghi đã mua, Gửi chốt.
  - **Kế toán** *(mới 2026-09-28, Q-212; Phụ lục 27 cột KT, chú thích ²⁸)*: xuất kế toán (FR-039, FR-041); xem bảng chi phí và khoản (X¹⁶, khoản thuốc chỉ thấy mã vật phẩm, FR-019).
  - **Quản lý viện** (D, toàn viện): Duyệt, Trả lại, Chốt bảng, đồng ý / từ chối đề nghị mua hộ, tạo phiên bản đơn giá (FR-012), xem toàn bộ. Quản lý viện MUST NOT xuất kế toán, vì ma trận chỉ cho D ở dòng "Chốt kỳ, xuất kế toán"; quyền xuất là T của Kế toán.
  - **Người thân** (X): xem theo FR-037, FR-038; người đại diện đồng ý hoặc từ chối đề nghị mua hộ (FR-023a).
  - **Hệ thống / Bộ lập lịch**: sinh, tính lại, hủy khoản tự sinh, tạo khoản điều chỉnh tự tạo, mở bảng và kỳ.

  Các vai trò khác MUST NOT xem chi phí. *(Nguồn: 4.4 dòng "Chi phí, khoản điều chỉnh", "Chốt kỳ, xuất kế toán"; UC-61 → UC-64; BR-M11-07)*

#### J. Thông báo

- **FR-044**: Feature này MUST yêu cầu feature 009 gửi đúng các thông báo ở bảng dưới, theo khuôn chung của feature 009 FR-001. Mọi thông báo tới người thân mang loại thông tin "chi phí", nên chỉ tới người có quyền xem chi phí (feature 009 FR-013). Nội dung thông báo MUST NOT chứa tên thuốc (FR-019). Thông báo không khẩn tới người thân chịu giờ yên tĩnh do feature 009 áp dụng (BR-M13-03); spec này không đặt ngoại lệ. Mọi "báo", "thông báo", "nhắc" trong FR và bảng trạng thái của spec này MUST có dòng tương ứng ở bảng dưới. *(Nguồn: 17, BR-M13-01, BR-M13-03, BR-M13-04, feature 009)*

| Sự kiện | Người nhận | Mức | Căn cứ |
| --- | --- | --- | --- |
| Khoản mang dấu "thiếu đơn giá" | Hành chính | Nhẹ | FR-012 |
| Khoản bị Quản lý viện trả lại; bảng về Đang mở do khoản Nháp mới hoặc do khoản bị hệ thống đưa về Nháp | Hành chính | Nhẹ | FR-004, bảng khoản |
| Khoản điều chỉnh do hệ thống tạo; khoản điều chỉnh bổ sung phát sinh muộn; khoản có dấu "phát sinh thuộc kỳ đã chốt" | Hành chính | Nhẹ | FR-003, FR-027 |
| Điều kiện "chi phí kỳ cuối đã chốt" về Chưa đạt | Hành chính | Nhẹ | FR-034a |
| Bảng được gửi chốt | Quản lý viện | Nhẹ | Bảng trạng thái bảng chi phí |
| Cảnh báo biến động chi phí | Hành chính | Nhẹ | FR-032 |
| Quá hạn chốt; còn khoản thiếu đơn giá sau hạn | Hành chính / Quản lý viện | Nhẹ | FR-031 |
| Nhắc bảng bổ sung chưa chốt | Hành chính | Nhẹ | FR-029 |
| Bảng Đã chốt | Người thân có quyền xem chi phí | Trung bình | FR-040 |
| Đề nghị mua hộ cần đồng ý | Người đại diện Hiệu lực; Quản lý viện | Trung bình (người đại diện); Nhẹ (Quản lý viện) | FR-023, FR-023a |
| Đề nghị mua hộ được đồng ý hoặc bị từ chối; không còn người đại diện có tài khoản | Hành chính | Nhẹ | Bảng đề nghị mua hộ, FR-023a |
| Đề nghị mua hộ bị hủy sau khi đã được đồng ý | Người đã đồng ý | Nhẹ | Bảng đề nghị mua hộ |
| Khoản mua hộ vượt số đã đồng ý được duyệt | Người đại diện | Nhẹ | FR-024 |
| Đề nghị Chờ đồng ý quá CFG-M15-05 (mặc định \[48 giờ\]) / CFG-M15-06 (mặc định \[96 giờ\]) | Người có quyền quyết định / Quản lý viện | Theo feature 000 FR-031a | Bảng đề nghị mua hộ |

### Giao tiếp với feature khác

| Hướng | Feature | Nội dung | Dữ liệu tối thiểu |
| --- | --- | --- | --- |
| Nhận | 004 | Nội dung hợp đồng hiệu lực tại ngày D (đơn giá, cách tính giá, gói, loại lưu trú) | Hợp đồng, phụ lục, đơn giá từng khoản, dịch vụ thuộc gói |
| Nhận | 004 | Dữ liệu để feature này lập ngày lưu trú (FR-005a): trạng thái lưu trú theo ngày, ngày vắng với hệ số, dấu "chờ quyết định", "không có dòng chính sách", "chưa vào ở", quyết định giữ giường | Người cao tuổi, ngày, lượt vắng, số thứ tự ngày vắng, hệ số, dấu |
| Nhận | 004 | Ngày khu bán trú nghỉ (Q-140) | Ngày, người khai báo |
| Nhận | 004 | Hồ sơ kết thúc lưu trú (lập, đổi ngày, hủy, quá ngày dự kiến); Hủy tiếp nhận; qua đời | Người cao tuổi, ngày kết thúc dự kiến, thời điểm |
| Nhận | 004 | Danh mục dịch vụ, vật phẩm và phiên bản đơn giá | Dịch vụ/vật phẩm, đơn vị, đơn giá, hiệu lực từ/đến |
| Gửi | 004 | Điều kiện "chi phí kỳ cuối đã chốt" (FR-034a); mục "chốt chi phí kỳ cuối" sau qua đời (FR-036) | Người cao tuổi, trạng thái, thời điểm chốt |
| Nhận | 005 | Kết quả có tính phí; đính chính hoặc hủy ghi nhận; trạng thái có mặt bán trú theo ngày, kể cả điểm danh đến ngoài lịch (Q-140) | Như bảng giao tiếp của feature 005 |
| Nhận | 006 | Liều và lần dùng PRN nguồn viện Đã dùng (không gồm liều thuộc lần giao đã tính phí, liều nguồn gia đình gửi); lần giao và nhận lại thuốc mang theo; đính chính | Liều/lần giao, thuốc, nguồn thuốc, số lượng, thời điểm |
| Gửi | 006 | Đề nghị mua hộ thuốc đã Đã mua, để tiếp nhận với nguồn "gia đình gửi" (Q-139) | Đề nghị, người cao tuổi, mục, số lượng, ngày mua |
| Nhận | 011 | Suất ăn của người thân ở lại: ghi và hủy (hủy trước giờ bữa; sau giờ bữa không hủy vì người thân không ăn) | Suất, người thân, lượt ở lại, người cao tuổi được gắn, bữa, ngày bữa, trạng thái (Đã ghi / Đã hủy) |
| Nhận | 012 | Đêm người thân ở lại; đồng ý hoặc từ chối đề nghị mua hộ của người đại diện | Lượt ở lại, đêm; đề nghị, người đại diện, quyết định |
| Gửi | 012 | Bảng chi phí đã chốt; chi phí tạm tính; đề nghị mua hộ cần đồng ý (kèm dấu "số dư không đủ" nhận từ 017) | Theo FR-037, FR-038, FR-023 |
| Gửi | 017 | *(mới 2026-09-28)* Bảng Đã chốt (thường hoặc bổ sung), gửi đúng một lần cho mỗi bảng, gửi lặp được nhận diện theo mã bảng; tổng chi phí chưa chốt (FR-038a); phí ngày cơ sở của hợp đồng tính theo tháng (FR-009); đề nghị mua hộ được gửi, được quyết định, bị hủy | Mã bảng (duy nhất), loại bảng, kỳ, người cao tuổi, tổng có dấu, thời điểm chốt; tổng chưa chốt; đề nghị, số tiền dự kiến, trạng thái |
| Nhận | 017 | *(mới 2026-09-28)* Dấu "số dư không đủ" của đề nghị mua hộ, tính lại tới khi đề nghị được quyết định (feature 017 FR-021, BR-M11-15). Dấu MUST hiển thị cho người đại diện và Quản lý viện khi đồng ý hoặc từ chối; dấu không chặn | Đề nghị, dấu, thời điểm tính |
| Nhận | 014 | Điểm danh hoạt động có thu phí và việc hủy lượt (đính chính điểm danh, Hủy ghi nhận điểm danh rời viện, feature 014 FR-031, FR-031a); giờ về theo ngày của bán trú lấy từ feature 005 (Q-176) | Buổi, người cao tuổi, hoạt động/dịch vụ, bản ghi điểm danh |
| Gửi | 009 | Các thông báo ở bảng FR-044 | Nguồn, mức, nhóm người nhận, loại thông tin "chi phí" |
| Gửi | 016 | Dữ liệu cho báo cáo chi phí 18.4 và dashboard 18.5 | Khoản (loại, nguồn sinh, người lập khoản điều chỉnh: hệ thống hay Hành chính, dịch vụ/vật phẩm/thuốc/hoạt động, ngày phát sinh, số lượng, đơn giá, thành tiền, dấu "thiếu đơn giá", "thuộc gói", trạng thái); bảng (thường/bổ sung, kỳ, trạng thái, hạn chốt); khoản điều chỉnh (khoản gốc, kỳ gốc); kỳ của viện và trạng thái |

### Key Entities *(include if feature involves data)*

- **Kỳ chi phí (KY_CHI_PHI)** – nhóm 2: từ ngày, đến ngày, hạn chốt, trạng thái, thời điểm Đã chốt.
- **Bảng chi phí** – nhóm 2 → 3 khi chốt: kỳ, người cao tuổi, loại bảng (thường / bổ sung), là kỳ cuối, đến ngày (với kỳ cuối), tổng theo loại, tổng cộng, người gửi chốt, người chốt, thời điểm chốt, lần xuất đầu tiên, trạng thái. Mỗi người, mỗi kỳ có đúng một bảng thường; tối đa một bảng bổ sung mỗi kỳ (FR-002, FR-002a).
- **Ngày lưu trú** – nhóm 2, bản ghi dẫn xuất: người cao tuổi, hợp đồng, ngày, có mặt / vắng, lượt vắng và số thứ tự ngày vắng, hệ số, các dấu, lịch sử cập nhật (FR-005a).
- **Sự kiện không sinh chi phí** – nhóm 3: người cao tuổi, sự kiện, bản ghi nguồn, thời điểm, lý do không sinh, dấu "đã sinh bù" (FR-011).
- **Khoản chi phí (CHI_PHI)** – nhóm 2 → 3 khi chốt: các trường ở FR-014, cộng với lý do chấp nhận phần vượt (khoản mua hộ, FR-024), người kiểm tra, người duyệt; khoản điều chỉnh là một khoản chi phí có khoản gốc (quan hệ "điều chỉnh cho" trong ERD).
- **Lịch sử tính lại** – nhóm 3: khoản, giá trị trước/sau (số lượng, đơn giá, hệ số, số tiền), căn cứ, sự kiện gây ra, thời điểm.
- **Đề nghị mua hộ** – nhóm 2: người cao tuổi, mục, số lượng, số tiền dự kiến, số tiền được phép, giá trị CFG-M11-02 lúc gửi, lý do, người yêu cầu (nhân viên hoặc người thân), những người đại diện được gửi yêu cầu đồng ý, người quyết định và kênh (cổng / Quản lý viện), số tiền thực tế, ngày mua, bằng chứng, tham chiếu lần tiếp nhận thuốc ở feature 006 (với thuốc, Q-139), trạng thái.
- **Lần xuất dữ liệu kế toán** – nhóm 3: kỳ, phạm vi, người xuất, thời điểm, danh sách bảng, số khoản, tổng số tiền.
- Dùng từ feature khác: **Hợp đồng**, **Phụ lục**, **Dịch vụ/vật phẩm (DICH_VU)**, **Phiên bản đơn giá (PHIEN_BAN_DON_GIA)**, **Lượt vắng (LUOT_VANG)** (feature 004); **Công việc/kết quả (CONG_VIEC)**, **Có mặt bán trú (CO_MAT_BAN_TRU)** (feature 005); **Liều thuốc (LIEU_THUOC)** (feature 006); **Lượt ở lại** (feature 012); **Điểm danh (DIEM_DANH)** (feature 014).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% khoản tự sinh trong đợt kiểm thử trỏ về đúng một bản ghi nguồn tồn tại. 100% sự kiện nguồn trong thời gian tính phí sinh đúng một khoản. 0 khoản trùng sau 3 lần chạy lại Bộ lập lịch cho cùng ngày.
- **SC-002**: Với bộ kiểm thử 12 tháng (tháng 28, 29, 30, 31 ngày; có và không có vắng; có phụ lục đổi giá), 100% tháng đủ ngày, cùng giá, không vắng có tổng phí lưu trú đúng bằng giá tháng. 100% ngày có số tiền khớp tính tay theo FR-009.
- **SC-003**: 100% khoản trong bộ kiểm thử có đơn giá khớp căn cứ đúng (hợp đồng hiệu lực hoặc phiên bản đơn giá tại ngày phát sinh). 100% khoản thuộc gói có số tiền 0 và vẫn có nguồn.
- **SC-004**: 0 lệnh chốt thành công khi bảng còn khoản Nháp hoặc Đã kiểm tra chưa duyệt. 0 thao tác sửa, xóa, hủy thành công trên khoản hoặc bảng Đã chốt.
- **SC-005**: 100% lần bản ghi nguồn thay đổi sau chốt tạo đúng một khoản điều chỉnh với số tiền bằng chênh lệch tính tay. 100% lần bản ghi nguồn thay đổi trước chốt tính lại hoặc hủy đúng khoản. Cả hai việc xong trong vòng 5 phút kể từ khi bản đính chính nguồn được lưu. Tổng kỳ gốc trong bảng đã chốt và file đã xuất không đổi.
- **SC-006**: Bộ lập lịch sinh phí lưu trú và phí buổi của một ngày cho 300 người cao tuổi xong trong không quá 5 phút (NFR-04).
- **SC-007**: Trong đợt thử với dữ liệu một tháng của 300 người cao tuổi, hành chính chỉ phải xem xét riêng từng khoản có dấu, khoản nhập tay, mua hộ, điều chỉnh (các khoản còn lại kiểm tra hàng loạt được). Toàn bộ bảng thường của kỳ được gửi chốt trong không quá 2 ngày làm việc sau ngày cuối kỳ. Đây là mục tiêu đo trong đợt thử để đánh giá lợi ích của kiểm tra hàng loạt, không phải tham số và không thay hạn chốt CFG-M11-01 (FR-001).
- **SC-008**: 100% khoản trong bảng đã chốt (trừ khoản nhập tay) truy được về một bản ghi nguồn tồn tại mà không cần tra cứu ngoài hệ thống. 100% khoản nhập tay có lý do và người duyệt.
- **SC-009**: File xuất kỳ khớp 100% với các bảng đã chốt (số khoản, tổng theo người, tổng kỳ, dòng tổng kiểm soát). Xuất lại cùng phạm vi "toàn bộ" khi không có bảng mới chốt cho cùng nội dung. Tổng các file phạm vi "chỉ bảng chưa từng xuất" của một kỳ bằng file phạm vi "toàn bộ" của kỳ đó, không trùng bảng nào.
- **SC-010**: 0 khoản tự sinh cho sự kiện có thời điểm sau thời điểm qua đời, hoặc cho ngày sau ngày qua đời. Khoản phí lưu trú của chính ngày qua đời là đúng quy tắc (Q-141). 0 khoản tự sinh cho ngày sau ngày kết thúc dự kiến (khi chưa quá hạn). 100% kỳ cuối được chốt trước khi lệnh Kết thúc lưu trú thành công.
- **SC-011**: 0 lần mua hộ vượt CFG-M11-02 được ghi đã mua khi chưa có đồng ý của người đại diện hoặc duyệt của Quản lý viện.
- **SC-012**: 0 lần tên thuốc hoặc hàm lượng xuất hiện với Hành chính, với người thân không có quyền xem sức khỏe có tác dụng, hoặc trong file kế toán, kể cả qua lịch sử tính lại, nhật ký, thông báo và chi phí tạm tính (FR-019, Q-133).

## Assumptions

- Số feature `010` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-61 → UC-64) và theo cách các spec 001, 004, 005, 006, 012 đã gọi Module 11.
- "Chốt kỳ" thuộc hai bước theo dòng "Chốt kỳ, xuất kế toán" của Permission Matrix (HC T, QL D): hành chính gửi chốt, Quản lý viện chốt. Cách hiểu này khớp UC-62 ("Duyệt và chốt kỳ chi phí", Quản lý viện) và BR-M11-08 ("quản lý duyệt và chốt").
- Kiểm tra và duyệt thực hiện được hàng loạt (trừ khoản do Hành chính lập, FR-015). Với quy mô 300 người, mỗi kỳ có hàng nghìn khoản, nên thao tác từng khoản là không khả thi.
- Phí lưu trú của một ngày sinh sau khi ngày kết thúc, vì hệ số phụ thuộc trạng thái vắng của cả ngày (6.7 "ngày vắng là ngày dương lịch có một phần thời gian vắng"). Riêng bảng kỳ cuối được sinh trước theo yêu cầu của Q-21.
- Buổi bán trú "Vắng có báo" vẫn tạo khoản với hệ số 0% (số tiền 0) để truy xuất. Nguồn: bảng 6.7 cho dòng này hệ số 0%, không nói "không tính"; cùng cách với BR-M11-03 "vẫn giữ để truy xuất".
- Gói trong hợp đồng không có hạn mức số lần. Mọi lần dùng dịch vụ hoặc vật phẩm thuộc gói có số tiền 0. Nguồn: BR-M11-03 và DBR-16 không nêu hạn mức; hợp đồng (feature 004 FR-019) chỉ đánh dấu "dịch vụ thuộc gói". Nếu cơ sở cần hạn mức gói, phải bổ sung vào 6.3 trước.
- Khoản điều chỉnh tự tạo và khoản điều chỉnh bổ sung nằm ở bảng chưa chốt của kỳ hiện tại, hoặc bảng bổ sung (FR-002a, FR-003); không mở lại bảng đã chốt.
- Khoản mang dấu "chờ quyết định" không chặn việc chốt. Chênh lệch khi có quyết định được xử lý bằng khoản điều chỉnh.
- Cảnh báo biến động (BR-M11-07) tính theo từng người cao tuổi, chỉ khi tổng tăng, và bỏ qua khi một trong hai bảng không đủ tháng (FR-032).
- Đơn giá thuốc nguồn viện nằm trong danh mục vật phẩm, phiên bản đơn giá do Quản lý viện quản lý (feature 004 FR-034, feature 006 FR-003).
- Đặt cọc không được khấu trừ hay hiển thị trong bảng chi phí; tiền cọc và cấn trừ cọc thuộc feature 017 (6.5, 15.9, đã chỉnh sửa 2026-09-28).
- Người thân chưa gửi yêu cầu mua hộ qua cổng ở giai đoạn này (FR-023); cổng chỉ nhận yêu cầu đồng ý. Có thể mở rộng khi làm lại feature 012.
- Làm tròn nửa lên (FR-013) là cách làm tròn thông dụng cho tiền đồng; tài liệu nguồn không quy định.
- Không đề xuất tham số mới. Chu kỳ nhắc bảng bổ sung dùng lại CFG-M02-09.

## Điểm cần báo lại về tài liệu nguồn

**Trạng thái (2026-09-27):** theo yêu cầu của người dùng, các điểm dưới đây đã được đưa vào tài liệu nguồn và spec liên quan, trừ phần "Còn mở".

**Đã phản ánh vào tài liệu nguồn và spec liên quan**
- Điểm 1: 15.2 (bảng nguồn, danh sách không sinh chi phí), ERD miền E (NGAY_LUU_TRU, BANG_CHI_PHI, DE_NGHI_MUA_HO), mô tả thực thể 3.2, DBR-15.
- Điểm 2: sơ đồ và mục "Bảng chi phí và người chốt" ở 15.6.
- Điểm 3: 15.6, dòng UC-62, chú thích ¹⁷ của 4.4.
- Điểm 4: BR-M10-10, UC-79, dòng "Đề nghị mua hộ" của 4.4; feature 012 FR-046a, FR-067.
- Điểm 5: BR-M11-06, BANG_CHI_PHI.
- Điểm 6: BR-M11-09, DBR-17.
- Điểm 7: BR-M11-08; feature 004 FR-063, FR-066, FR-066a.
- Điểm 8: 6.4; feature 004 FR-035, User Story 9 kịch bản 6.
- Điểm 10: Q-131 → Q-141 ở mục 24.2.
- Điểm 11: CFG-M11-01 ở Phụ lục 25, 15.7.
- Điểm 12: 15.3, 19.3, 23, chú thích ¹⁶ của 4.4; feature 002 FR-022.
- Điểm 13: BR-M11-05.
- Điểm 15: feature 000 FR-026 (c), FR-031; 002 FR-022; 004 FR-034, FR-035, FR-052, FR-060a, FR-063, FR-066, FR-066a, FR-070, SC-008; 005 FR-013, FR-016; 006 FR-003, FR-033, FR-040; 012 FR-046, FR-046a, FR-052, FR-067. Mục 1.5 và 3.4, 11.4 của tài liệu nguồn cũng được sửa.
- Điểm 16: mục 23.

**Còn mở**
- Điểm 9: Q-05 và Q-08 vẫn chờ người quyết định. Các cột xuất và thời điểm xuất ở mục 23 cần kế toán của cơ sở xác nhận.
- Điểm 14: phần yêu cầu với feature 011 đã hoàn tất (2026-09-27, spec 011 FR-043); phần feature 014 chờ khi viết spec đó.

Nội dung gốc của từng điểm được giữ dưới đây để truy vết.

1. **DBR-15 và quan hệ nguồn của CHI_PHI (3.1)** chỉ liệt kê nguồn: công việc, liều thuốc, điểm danh, lượt vắng, có mặt bán trú, nhập tay. Còn thiếu:
   - các nguồn mà bảng 15.2 và các spec 006, 011, 012 đã dùng: lượt người thân ở lại, suất ăn của người ở lại, lần giao thuốc mang theo, đề nghị mua hộ, khoản điều chỉnh (trỏ khoản gốc);
   - **ngày lưu trú**: thực thể mới do feature này lập mỗi ngày (FR-005a), là nguồn của phí lưu trú cả ngày có mặt lẫn ngày vắng (lượt vắng chỉ là tham chiếu phụ);
   - buổi bán trú phát sinh ngoài lịch (FR-005b, Q-140).
   ERD miền E cần thêm NGAY_LUU_TRU, BANG_CHI_PHI, DE_NGHI_MUA_HO.
2. **Sơ đồ vòng đời 15.6** chỉ có Nháp → Hủy và Đã kiểm tra → Nháp (trả lại). Spec cần thêm:
   - Đã kiểm tra, Đã duyệt → Nháp khi bản ghi nguồn đổi trước chốt;
   - Đã duyệt → Nháp khi Quản lý viện trả lại, cả khi bảng còn Đang mở;
   - lệnh Bỏ kiểm tra của Hành chính (Đã kiểm tra → Nháp);
   - vòng đời **bảng chi phí** (Đang mở → Chờ chốt → Đã chốt), với hai loại bảng thường và bảng bổ sung (FR-002a);
   - vòng đời **kỳ chi phí của viện** (Đang mở → Chờ chốt → Đã chốt).
   Các vòng đời này chưa có trong tài liệu.
3. **UC-62** ghi actor là Quản lý viện cho "Duyệt và chốt kỳ", còn dòng "Chốt kỳ, xuất kế toán" của Permission Matrix ghi HC T, QL D. Spec đọc là: hành chính gửi chốt, Quản lý viện chốt (Assumptions). Nên ghi rõ ở 15.6 hoặc 4.4.
4. **BR-M11-07 (mua hộ)**: người đại diện "đồng ý qua cổng" vượt quyền X của người thân ở dòng "Chi phí, khoản điều chỉnh" (4.4). Chưa có use case cho mua hộ. Feature 012 chưa có luồng đồng ý đề nghị mua hộ. Đề xuất thêm UC mới (ví dụ UC-79 "Đề nghị và đồng ý mua hộ"), chú thích quyền cho người đại diện, và bổ sung vào feature 012.
5. **KY_CHI_PHI (3.2)** không gắn người cao tuổi. Theo Q-131, việc chốt diễn ra trên từng bảng chi phí, nên cần thêm thực thể "Bảng chi phí" (KY_CHI_PHI 1–n BANG_CHI_PHI 1–n CHI_PHI) vào ERD miền E. BR-M11-06 cần đổi "không thể chốt kỳ" thành "không thể chốt bảng chi phí".
6. **BR-M11-09** chỉ quy định chia giá tháng cho phí lưu trú hằng ngày. Đã chốt (Q-138) cách chia cho hợp đồng bán trú tính giá tháng theo số buổi có lịch (FR-009); cần bổ sung vào BR-M11-09. **DBR-17** yêu cầu khoản điều chỉnh "trỏ về khoản gốc", nhưng khoản bổ sung phát sinh muộn không có khoản gốc.
7. **Khoản phát sinh sau khi kỳ cuối đã chốt** (ví dụ liều thuốc sáng ngày kết thúc, trước lệnh Kết thúc lưu trú): feature 004 FR-066 chưa nêu các khoản này có làm điều kiện "chi phí đã chốt" về Chưa đạt không. Đã chốt (Q-134): không làm về Chưa đạt; các khoản vào bảng bổ sung có nhắc (FR-029). Cần đồng bộ feature 004 FR-066 và bổ sung BR-M11-08.
8. **Phiên bản đơn giá có hiệu lực lùi** (đã chốt, Q-135): được phép, chỉ cho khoảng chưa có phiên bản nào (FR-012). Cần bổ sung vào 6.4 và feature 004 FR-034 (mục E, User Story 9 về phiên bản đơn giá).
9. **Q-05** (định dạng xuất cho kế toán) và **Q-08** (bảng chính sách phí khi vắng) vẫn mở ở mục 24.1. Spec dùng mặc định đề xuất. Các cột xuất ở FR-039 thêm "ngày phát sinh", "đơn vị", "thuộc gói", "loại điều chỉnh" so với mục 23.
10. Đề xuất ghi vào mục 24.2 ba quyết định đã chốt ngày 2026-09-26: **Q-131** (chốt theo bảng chi phí từng người; kỳ của viện chốt khi mọi bảng chốt), **Q-132** (kỳ luôn là tháng dương lịch; CFG-M11-01 là hạn chốt), **Q-133** (tên thuốc trên khoản chi phí chỉ hiện với người có quyền xem thuốc; hành chính, người thân không có quyền sức khỏe và file kế toán thấy mã vật phẩm). Ngày 2026-09-27 chốt thêm: **Q-134** (khoản sau kỳ cuối đã chốt không chặn Kết thúc lưu trú), **Q-135** (phiên bản đơn giá hiệu lực lùi cho khoảng trống), **Q-136** (thành phần chi phí tạm tính), **Q-137** (mua hộ vượt số đã đồng ý), **Q-138** (chia giá tháng bán trú theo buổi có lịch), **Q-139** (thuốc mua hộ là nguồn gia đình gửi, không tính theo liều), **Q-140** (bán trú: buổi phát sinh ngoài lịch, ngày khu nghỉ), **Q-141** (tính phí ngày qua đời).
11. **CFG-M11-01 (Phụ lục 25)**: cần đổi mô tả thành "Hạn chốt bảng chi phí của kỳ vừa kết thúc" và làm rõ giá trị mặc định \[Ngày cuối tháng\] là ngày cuối của tháng liền sau kỳ (FR-001). Câu "ngày chốt kỳ là tham số cấu hình" ở 15.7 cần sửa theo.
12. **Mục 23 và 19.3**: cột "mô tả" trong file kế toán là mã vật phẩm với khoản thuốc (Q-133). Nên thêm vào 19.3 rằng giới hạn "hành chính không xem thuốc" áp cả cho khoản chi phí thuốc, và bổ sung chú thích cho dòng "Chi phí, khoản điều chỉnh" của 4.4 với cột NT.
13. **BR-M11-05** viết "hệ thống tạo khoản Điều chỉnh chờ duyệt", còn spec cho khoản bắt đầu ở Nháp theo câu "đi lại vòng đời này" ở 15.6 (FR-027). Nên sửa BR-M11-05 thành "tạo khoản Điều chỉnh ở trạng thái Nháp" cho khớp.
14. **Yêu cầu đối với feature chưa viết**, cần đưa vào khi làm các spec đó:
    - **011**: ghi suất ăn của người thân ở lại có đăng ký ăn làm bản ghi nguồn chi phí (người cao tuổi được gắn, ngày bữa, hủy suất). Cung cấp đơn giá suất ăn ngoài hợp đồng qua danh mục của feature 004. — *đã đưa vào spec 011 ngày 2026-09-27 (feature 011 FR-043, bảng giao tiếp).*
    - **014**: cung cấp lượt điểm danh có mặt ở buổi hoạt động có thu phí, và sự kiện hủy điểm danh hoặc hủy buổi. Đánh dấu hoạt động "có thu phí", kèm dịch vụ tương ứng trong danh mục.
15. **Cần đồng bộ ở feature đã viết**:
    - **004**:
      - FR-066, FR-066a: điều kiện (a) lấy theo FR-034a của spec này (kỳ cuối Đã chốt và không còn khoản mang dấu "ảnh hưởng kết thúc lưu trú" chưa chốt); khoản thường sau kỳ cuối không làm điều kiện về Chưa đạt (Q-134).
      - FR-034 và User Story 9: cho phép phiên bản đơn giá hiệu lực lùi (Q-135).
      - FR-052: cung cấp đủ dữ liệu cho mọi ngày lưu trú, không chỉ ngày vắng (FR-005a).
      - FR-070: diễn đạt lại "dừng sinh chi phí" cho khớp cách tính ngày qua đời (Q-141).
      - Cho khai báo ngày khu bán trú nghỉ (Q-140); mục E: có phiên bản đơn giá "buổi bán trú" dùng cho buổi phát sinh ngoài lịch.
    - **012**:
      - Luồng đồng ý đề nghị mua hộ trên cổng (FR-023, FR-023a).
      - Che tên thuốc trên bảng chi phí khi người thân không có quyền xem sức khỏe (Q-133).
      - Thành phần chi phí tạm tính trong bản tin (Q-136).
      - Thông báo phần mua hộ vượt số đã đồng ý (Q-137).
    - **000**: FR-026 (c) đang lấy "chi phí đã chốt" làm ví dụ cho đính chính qua yêu cầu phê duyệt "Đính chính". Theo 15.6 ("sai sót tạo khoản điều chỉnh") và DBR-17, khoản chi phí đã chốt chỉ sửa bằng khoản điều chỉnh (UC-63: Hành chính lập, Quản lý viện duyệt), không có bản đính chính (bảng trạng thái khoản, mục D). Cần bỏ "chi phí đã chốt" khỏi ví dụ của FR-026 (c) và ghi chi phí là ngoại lệ theo FR-030. Có thể thêm đề nghị mua hộ vào danh sách đối tượng tự có vòng đời riêng, không dùng FR-031.
    - **003**: FR-030 không cần sửa. Spec này đã nêu hệ thống tự tạo khoản điều chỉnh chênh lệch từ ngày chuyển giường (Edge Cases, FR-027).
    - **002**: giới hạn trường của Hành chính (FR-022 của feature 002) áp cả cho tên thuốc trên khoản chi phí, lịch sử tính lại và thông báo chi phí (Q-133).
    - **006**:
      - FR-003 và FR-033: đơn giá thuốc nguồn viện nằm trong danh mục vật phẩm có phiên bản đơn giá.
      - Mục 11.4 / tiếp nhận thuốc gia đình gửi: thuốc mua hộ được tiếp nhận với nguồn "gia đình gửi", có tham chiếu đề nghị mua hộ (Q-139).
    - **005**: điểm danh đến bán trú vào ngày không có lịch phải được phép và gửi sự kiện cho feature này; buổi trùng ngày khu nghỉ không chuyển Vắng không báo (Q-140).
16. **Mục 23 (tích hợp kế toán)** ghi "file tải về sau khi chốt kỳ". Spec cho xuất từng bảng đã chốt trước khi cả kỳ của viện Đã chốt, và có phạm vi "chỉ bảng chưa từng xuất" (FR-039, FR-041, Q-131). Cần sửa mục 23 thành "sau khi bảng chi phí được chốt", kèm yêu cầu dòng tổng kiểm soát. Việc này cần kế toán của cơ sở xác nhận cùng Q-05.

**(2026-09-28, góp ý nghiệp vụ Q-207 → Q-222) Tài liệu nguồn đã thay đổi, spec cần rà lại.** *(2026-09-29: mọi điểm dưới đây đã xử lý, xem nhãn "Đã xử lý" từng điểm.)* Các điểm dưới đây đã có trong `docs/nghiep-vu.md` và `docs/luong-nghiep-vu.md`; spec **chưa** được sửa theo, và cần chạy `/speckit-clarify` hoặc cập nhật FR tương ứng.

1. **[Đã xử lý 2026-09-28: FR-039, FR-041, FR-043, User Story 7]** **Xuất file kế toán chuyển sang Kế toán (Q-212; UC-64, 15.6, Phụ lục 27 ²⁸).** Mọi chỗ "hành chính xuất kỳ" (User Story xuất dữ liệu, FR-039 → FR-041, bảng quyền "Chốt kỳ, xuất kế toán") đổi người thực hiện thành Kế toán. Hành chính vẫn kiểm tra và gửi chốt.
2. **[Đã xử lý 2026-09-28: bảng giao tiếp "Gửi 017", FR-042, FR-038a]** **Chốt bảng tự trừ số dư (Q-211; BR-M11-11, DBR-30).** Khi bảng (thường hoặc bổ sung) Đã chốt, hệ thống tạo đúng một giao dịch "thanh toán bảng chi phí". Đây là sự kiện feature này phải phát ra cho chức năng số dư (spec 017).
3. **[Đã xử lý 2026-09-28: bảng giao tiếp "Nhận 017"]** **Mua hộ khi số dư không đủ (Q-219; BR-M11-15).** Đề nghị mua hộ mang dấu "số dư không đủ"; không chặn.
4. **[Đã xử lý 2026-09-28: Phạm vi, FR-042; spec riêng là 017]** **Phạm vi 1.2, 15.1 đã đổi.** Hệ thống nay quản lý số dư, thu chi, đối soát chuyển khoản của từng người cao tuổi (15.9). Đề xuất viết spec riêng cho 15.9 (BF-16), không gộp vào spec này.
