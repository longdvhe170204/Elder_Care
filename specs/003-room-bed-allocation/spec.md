# Feature Specification: Phòng, giường và phân bổ giường

**Feature Branch**: `003-room-bed-allocation`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Quản lý cơ cấu Khu vực → Tầng → Phòng → Giường và phân bổ giường theo docs/nghiep-vu.md Module 03 (mục 7): phòng có loại, mức chăm sóc cho phép, chính sách giới tính, trạng thái cách ly; giường có trạng thái; phân bổ giường là bản ghi có thời gian bắt đầu/kết thúc, chuyển giường là lệnh đóng bản ghi cũ và mở bản ghi mới; một giường không bao giờ có hai người cùng lúc; khu nghỉ bán trú có sức chứa theo buổi."

## Clarifications

### Session 2026-09-25

- Q: Khi một phòng đang cách ly (hoặc thuộc khu đang khoanh vùng), người đang nằm trong phòng đó có được chuyển sang giường khác không? → A: Chặn mọi chuyển ra hoặc chuyển trong vùng; ngoại lệ duy nhất là chuyển có bác sĩ chỉ định vì lý do kiểm soát lây nhiễm (ghi bác sĩ chỉ định), khi đó được chuyển kể cả sang phòng cách ly khác (đề xuất Q-17).
- Q: Khi người cao tuổi tạm vắng hoặc chuyển viện mà giường vẫn được giữ, bản ghi phân bổ giường của người đó có giữ nguyên trong thời gian vắng không? → A: Giữ nguyên bản ghi đang hiệu lực; giường chuyển Đang giữ chỗ (người vắng), khi trở về giường quay lại Đang sử dụng, không tạo bản ghi mới (đề xuất Q-18).
- Q: Nếu yêu cầu đổi mức chăm sóc được áp dụng mà phòng hiện tại không cho phép mức mới, yêu cầu đó có bắt buộc kèm giường mới hợp lệ không? → A: Không chặn; mức mới vẫn được áp dụng, hệ thống cảnh báo trên yêu cầu và nhắc trưởng tầng, hành chính chuyển giường cho tới khi xong (đề xuất Q-19; chu kỳ nhắc là tham số mới CFG-M03-02, mặc định 1 ngày).

### Cập nhật 2026-09-26 (bổ sung nghiệp vụ vệ sinh vào tài liệu nguồn)

Tài liệu nguồn thêm mục 7.5 (vệ sinh phòng và khu vực), trạng thái giường Chờ vệ sinh (7.2), BR-M03-08 → BR-M03-14, sửa BR-M03-06 và thêm Q-40. Spec được cập nhật theo: bảng trạng thái giường (mục C), FR-011, FR-014, FR-015, FR-024, FR-025, FR-032, User Story 3, 4 và User Story 8 mới (mục H).

- Q: Giường giữ tạm cho hồ sơ chờ (chưa từng có người nằm) khi hết hạn giữ chỗ hoặc bị hủy giữ thì chuyển sang trạng thái nào? → A: Về thẳng Trống; chỉ giường vừa kết thúc một phân bổ mới qua Chờ vệ sinh (BR-M03-06 đã sửa theo).

### Session 2026-09-26

- Q: Khi một giường vừa có người rời đi và đang chờ vệ sinh, có được đặt trước giường đó cho một người mới không? → A: Chỉ được tạo phân bổ tương lai có thời điểm bắt đầu không sớm hơn hạn vệ sinh trả giường (CFG-M03-04); không phân bổ bắt đầu ngay; BR-M02-01 chỉ kích hoạt khi giường về Trống; tới giờ bắt đầu mà giường chưa về Trống thì phân bổ chưa bắt đầu, trưởng tầng và hành chính được báo, và phân bổ bắt đầu ngay khi giường về Trống (chốt Q-40 theo mặc định mục 24).
- Q: Nếu công việc vệ sinh trả giường bị hủy hoặc đóng "Không thực hiện", làm thế nào để giường không bị kẹt mãi ở trạng thái Chờ vệ sinh? → A: Hệ thống tự sinh ngay một công việc thay thế cùng loại (khử khuẩn nếu công việc cũ là khử khuẩn), hạn tính lại theo CFG-M03-04, và báo trưởng tầng phụ trách (đề xuất Q-41).
- Q: Khi đến giờ bắt đầu của một phân bổ đặt trước cho người đang tiếp nhận mà người đó chưa thực sự vào viện, giường có tự chuyển sang "Đang sử dụng" không? → A: Không; với người Đang tiếp nhận, phân bổ chỉ bắt đầu khi lệnh "Hoàn tất tiếp nhận" được thực hiện (thời điểm bắt đầu thực tế là lúc đó); trước đó giường vẫn bị giữ bởi phân bổ tương lai; quá giờ bắt đầu dự kiến mà chưa Hoàn tất tiếp nhận thì báo hành chính (đề xuất Q-42).
- Q: Ai được nhập bù một phân bổ giường có thời điểm bắt đầu trong quá khứ, và được lùi xa tối đa bao lâu? → A: Hành chính và Trưởng tầng (trong phạm vi) nhập bù trực tiếp nếu lùi không quá CFG-M03-07 (đề xuất, mặc định 24 giờ); lùi xa hơn phải qua yêu cầu phê duyệt do Quản lý viện duyệt; nhập bù chồng lên khoảng Tạm vắng hoặc Điều trị tại bệnh viện thì bị chặn (đề xuất Q-43).
- Q: Khi trưởng tầng chuyển giường gấp vì lý do y tế hoặc an toàn sang phòng có giá khác mà không chờ duyệt, có cần ai xác nhận lại việc chuyển đó sau không? → A: Có; chuyển ngay, hệ thống tự tạo yêu cầu thay đổi lưu trú Chờ duyệt với đơn giá mới tính từ ngày chuyển nếu được duyệt, Quản lý viện được báo; nếu bị từ chối thì đơn giá cũ giữ nguyên và hệ thống nhắc chuyển người về phòng cùng giá (đề xuất Q-44).
- Q: Viện có một hay nhiều khu nghỉ bán trú, và người bán trú được tính vào sức chứa của khu nào? → A: Một khu nghỉ bán trú cho toàn viện, sức chứa mỗi buổi = CFG-M03-01, mọi người bán trú tính chung; bỏ khả năng khai báo nhiều khu (đề xuất Q-45).
- Q: Nếu người cao tuổi đang tạm vắng (giường vẫn được giữ) trở về đúng lúc phòng của họ đang bị cách ly, họ có được vào lại giường cũ không? → A: Vẫn cho Ghi nhận trở về, nhưng cảnh báo người thực hiện và báo ngay bác sĩ, trưởng tầng; người đó chỉ ở lại giường cũ khi bác sĩ xác nhận, nếu không thì chuyển ra giường ngoài vùng bằng lệnh chuyển có bác sĩ chỉ định (FR-010) (đề xuất Q-46).
- Q: Khi lịch đến của người bán trú không trùng khít với các buổi (ví dụ đến 10:00, về 15:00), người đó chiếm chỗ ở những buổi nào? → A: Mỗi buổi có khung giờ do cơ sở cấu hình; lịch đến chiếm chỗ ở mọi buổi có khung giờ giao với khoảng từ giờ đến tới giờ về (đề xuất Q-47).
- Q: Nếu hệ thống chạy việc tự động bị trễ (ví dụ chuyển giường theo lịch lúc 00:00 nhưng 02:30 mới chạy), thời điểm ghi trên bản ghi phân bổ và lịch sử giường là giờ dự kiến hay giờ thực tế chạy? → A: Giờ thực tế chạy, lưu kèm giờ dự kiến; trễ quá một ngưỡng (tham số mới CFG-M03-08, đề xuất mặc định 30 phút) thì báo Quản lý viện (đề xuất Q-48).
- Q: Công việc vệ sinh trả giường (phải xong thì giường mới dùng lại được) có mức quan trọng nào, để nó không bị hệ thống tự đóng rồi tự sinh lại mỗi ca mà không ai xử lý? → A: Vệ sinh trả giường mức Quan trọng; khử khuẩn (thay thế, theo khoanh vùng, kết thúc) mức Bắt buộc; vệ sinh định kỳ và đột xuất Thường mức Thường; đột xuất Gấp mức Quan trọng (đề xuất Q-49).
- Q: Nếu người cao tuổi được Hoàn tất tiếp nhận (đã vào viện) trong khi giường đặt trước vẫn đang chờ vệ sinh, thì trong lúc chờ, người đó được tính là đang ở đâu? → A: Không có khoảng chờ: chặn "Hoàn tất tiếp nhận" khi giường đặt trước chưa về Trống, trừ khi người thực hiện chuyển phân bổ sang một giường Trống khác ngay trong lệnh (đề xuất Q-50).
- Q: Khi vệ sinh phát hiện một giường bị hỏng nhưng giường đó đã được đặt trước cho người khác (phân bổ tương lai), hệ thống xử lý thế nào? → A: Cho đưa vào bảo trì; trong cùng lệnh, phân bổ tương lai chuyển Đã hủy (lý do "giường hỏng"), hành chính và trưởng tầng được báo để đặt giường khác; với chuyển giường theo lịch, yêu cầu thay đổi lưu trú chuyển "Áp dụng không thành" (đề xuất Q-51).
- Q: Khi trưởng tầng chuyển giường gấp sang phòng khác giá và hệ thống tự tạo yêu cầu thay đổi lưu trú, ai được ghi là người yêu cầu? → A: Hệ thống (như BR-M01-03), căn cứ là lệnh chuyển giường và người thực hiện lệnh; hành chính được giao theo dõi và bổ sung thông tin; trưởng tầng chỉ nhận thông báo kết quả (đề xuất Q-52).
- Q: Nếu người vừa rời giường chỉ bị đưa vào danh sách nghi nhiễm hoặc tiếp xúc sau khi giường đã chuyển sang chờ vệ sinh (nhưng trước khi vệ sinh xong), công việc có phải đổi từ vệ sinh thường sang khử khuẩn không? → A: Có; hệ thống đóng vệ sinh trả giường (lý do "nâng lên khử khuẩn"), sinh khử khuẩn thay thế (Bắt buộc, xác nhận đồ bảo hộ) và báo trưởng tầng; nếu giường đã về Trống thì feature 007 quyết định khử khuẩn đột xuất (đề xuất Q-53).

### Cập nhật 2026-09-27 (đồng bộ với spec 011)

Spec 011 cần biết ai nhận phiếu bữa ăn của khu bán trú (BR-M08-11, Q-143). Vì khu nghỉ bán trú đã gắn với một tầng hoặc khu vực (FR-038), người nhận là nhân viên có phân công tại tầng/khu vực đó trong ca; không cần thêm đối tượng phân công mới. FR-038 được bổ sung việc cung cấp tầng/khu vực gắn khu nghỉ cho feature 011. Giường và tầng của người nội trú, cùng việc chuyển giường, tiếp tục được cung cấp cho feature 011 để chia suất theo tầng/khu.

### Cập nhật 2026-09-27 (đồng bộ với spec 014)

Spec 014 và tài liệu nguồn (BR-M04-23, 3.4, Q-163, Q-174, Q-176) đã chốt:
- Kiểm tra chất lượng áp cho công việc vệ sinh của phòng và khu vực chung gắn tầng/khu vực; vệ sinh khu vực chung không gắn tầng không vào mẫu. Kết quả Không đạt sinh công việc vệ sinh làm lại trong cùng ca, không tự đổi trạng thái giường (FR-049a mới).
- Chỗ khu nghỉ bán trú (Q-47) tính theo giờ về theo ngày do feature 005 quản lý; giờ này có thể dời khi người bán trú có đồng ý về muộn vì hoạt động (3.4, Q-167).

### Cập nhật 2026-09-29 (đồng bộ với spec 019)

Spec 019 (tài sản của viện) và tài liệu nguồn (7.2, BR-M03-01, 06, 13, 15, Q-232, Q-234, Q-235) đã chốt:
- Mỗi giường có đúng một tài sản loại "giường" (feature 019 FR-011, DBR-32). Đang bảo trì và Không sử dụng chỉ phát sinh từ lệnh trên tài sản của giường ở feature 019 (Báo hỏng, Đưa vào bảo trì, Ghi kết quả bảo trì, Thanh lý). Spec này bỏ các lệnh "Đặt bảo trì", "Ngừng sử dụng", "Sẵn sàng" và "Đặt bảo trì do hư hỏng phát hiện khi vệ sinh"; bảng trạng thái giường nhận yêu cầu từ feature 019. Không sử dụng (sau Thanh lý) là trạng thái cuối của giường.
- Báo hỏng giường đang có người thì hư hỏng mang dấu "chờ chuyển người" (Q-232). Giường mang dấu không nhận phân bổ mới và không kích hoạt BR-M02-01 (Q-235) (FR-013b mới).
- (Bổ sung, checklist cross-feature CHK042, CHK043) Giường không còn lệnh Ngừng hiệu lực; ngừng dùng vĩnh viễn bằng Thanh lý tài sản, phòng chỉ Ngừng hiệu lực khi mọi giường đã Không sử dụng (Q-237; FR-002, FR-005, FR-007). Lệnh Tạo giường tự ghi tăng tài sản loại giường trong cùng một lần (Q-238).

## Phạm vi

**Trong phạm vi** (Module 03, mục 7; UC-19 → UC-21, UC-71 → UC-74):

1. Cơ cấu vị trí Khu vực → Tầng → Phòng → Giường: tạo, sửa, Ngừng hiệu lực (giường không Ngừng hiệu lực mà ngừng dùng bằng Thanh lý tài sản ở feature 019, Q-237); loại phòng, mức chăm sóc cho phép, chính sách giới tính (2.2, 7.1, UC-19).
2. Trạng thái cách ly của phòng: đặt và gỡ bằng lệnh; hiệu lực chặn phân bổ (7.1, BR-M03-01, BR-M05-11, BR-M05-12).
3. Vòng đời trạng thái giường: Trống, Đang sử dụng, Đang bảo trì, Không sử dụng, Đang giữ chỗ (hai lý do, mỗi lý do có hạn giữ), Chờ vệ sinh (7.2, BR-M03-05, BR-M03-06, BR-M03-09).
4. Phân bổ giường thành bản ghi có thời điểm bắt đầu/kết thúc; kiểm tra điều kiện; chống phân bổ trùng, kể cả khi hai người thao tác cùng lúc (7.3, UC-20, BR-M03-01, BR-M03-02, DBR-09, DBR-10).
5. Chuyển phòng/giường là một lệnh đóng bản ghi cũ và mở bản ghi mới trong cùng một lần thực hiện (7.4, UC-21, BR-M03-04, BR-M03-07).
6. Tra cứu lịch sử vị trí: "ngày X ai nằm giường nào", "người A ở đâu lúc T", "ai ở cùng phòng với A trong khoảng thời gian" (7.3, phục vụ BR-M05-10).
7. Khu nghỉ bán trú với sức chứa theo buổi; chặn đăng ký lịch đến vượt sức chứa (7.1, BR-M03-03, CFG-M03-01).
8. Vệ sinh phòng và khu vực: lịch vệ sinh theo phòng/khu vực; quy tắc sinh công việc vệ sinh định kỳ, trả giường, khử khuẩn, đột xuất; kết quả vệ sinh tác động lên trạng thái giường (7.5, BR-M03-08 → BR-M03-14, CFG-M03-03 → CFG-M03-06).

**Ngoài phạm vi** (spec này chỉ **cung cấp** kiểm tra/dữ liệu hoặc **được kích hoạt** bởi feature sở hữu):

- Quy tắc dùng chung (phân loại dữ liệu, lý do bắt buộc, đính chính, yêu cầu phê duyệt, nhật ký, tham số): feature 000 — spec này kế thừa, không lặp lại.
- Phạm vi dữ liệu của nhân viên suy ra từ phân bổ giường (BR-M15-02): feature 002 — spec này chỉ cung cấp vị trí hiện tại của người cao tuổi.
- Trạng thái người cao tuổi, giới tính, mức chăm sóc hiện hành: feature 001 — spec này chỉ đọc.
- Hồ sơ chờ, đề xuất người khi có giường trống (BR-M02-01), giữ chỗ tạm cho hồ sơ chờ (BR-M02-02), hợp đồng, yêu cầu thay đổi lưu trú, chính sách phí và giữ giường khi vắng (BR-M02-04, 06, 07), nội dung lệnh tạm vắng/kết thúc lưu trú: feature 004 — spec này quản lý trạng thái giường và bản ghi phân bổ khi các lệnh đó được thực hiện.
- Gán lại công việc chưa thực hiện theo khu mới sau khi chuyển (BR-M03-04): feature 005 thực hiện; spec này kích hoạt.
- Vòng đời trạng thái công việc, checklist, ghi nhận kết quả, quá hạn và kiểm tra chất lượng ngẫu nhiên áp cho công việc vệ sinh (8.3, BR-M04-05, BR-M04-23): feature 005 (và 014 cho kiểm tra ngẫu nhiên) — spec này quy định công việc vệ sinh được sinh khi nào, gắn với đâu, ghi nhận gì và tác động gì lên giường.
- Phân công nhân viên vệ sinh theo khu vực trong ca (13.4) và bản nháp bàn giao (BR-M09-06): feature 008.
- Danh sách nghi nhiễm, tiếp xúc và khoanh vùng/gỡ khoanh vùng (BR-M05-10 → 12): feature 007 — spec này nhận sự kiện để sinh công việc khử khuẩn.
- Khoanh vùng lây nhiễm, danh sách tiếp xúc (BR-M05-10 → 12, UC-37): feature 007 — spec này cung cấp dữ liệu lịch sử vị trí và áp chặn phân bổ.
- Lịch đến và điểm danh bán trú (3.4, UC-28): feature 004/005 — spec này chỉ kiểm tra sức chứa khi đăng ký lịch đến.
- Đơn giá theo loại phòng và chi phí: feature 004, 010.
- Tài sản của giường; các lệnh Báo hỏng, Đưa vào bảo trì, Ghi kết quả bảo trì, Thanh lý; dấu "chờ chuyển người" và việc nhắc chuyển người: feature 019. Spec này đổi trạng thái giường khi nhận yêu cầu từ feature 019 và áp dấu khi phân bổ (Q-234, Q-235).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quản lý viện dựng cơ cấu khu vực, tầng, phòng, giường (Priority: P1)

Quản lý viện khai báo cơ cấu vật lý của viện: các khu vực, tầng trong khu vực, phòng trong tầng, giường trong phòng. Mỗi phòng có loại phòng, danh sách mức chăm sóc được phép, chính sách giới tính (nam / nữ / không giới hạn). Đây là dữ liệu danh mục: được tạo và sửa, nhưng khi đã có lịch sử tham chiếu thì chỉ Ngừng hiệu lực, không xóa. Trưởng tầng và hành chính xem được cơ cấu này, không sửa.

**Why this priority**: Không có phòng và giường thì không phân bổ được; không có phân bổ thì không hoàn tất tiếp nhận nội trú được (5.6) và không suy ra được phạm vi dữ liệu của nhân viên (feature 002).

**Independent Test**: Tạo 1 khu vực, 2 tầng, mỗi tầng 2 phòng với chính sách khác nhau, mỗi phòng 2–4 giường; thử tạo mã trùng, thử xóa một phòng đã từng có phân bổ, thử sửa bằng tài khoản trưởng tầng.

**Acceptance Scenarios**:

1. **Given** Quản lý viện, **When** tạo khu vực "Khu A", tầng "Tầng 2" thuộc Khu A, phòng "P201" thuộc Tầng 2 với loại phòng "Phòng 2 giường chăm sóc đặc biệt", mức chăm sóc cho phép {Chăm sóc thường xuyên, Chăm sóc đặc biệt}, chính sách giới tính "Nữ", rồi tạo giường "G1", "G2" trong P201, **Then** các đối tượng được lưu ở trạng thái Hiệu lực, hai giường ở trạng thái Trống, vị trí hiển thị đầy đủ "Khu A – Tầng 2 – P201 – G1".
2. **Given** Tầng 2 đã có phòng P201, **When** tạo thêm phòng mã "P201" ở bất kỳ tầng nào, **Then** hệ thống từ chối vì mã phòng phải duy nhất trong viện; **Given** P201 đã có giường G1, **When** tạo giường G1 thứ hai trong P201, **Then** hệ thống từ chối.
3. **Given** phòng P201 đã từng có phân bổ giường (kể cả đã đóng), **When** Quản lý viện cố xóa P201, **Then** hệ thống không có thao tác xóa; chỉ có "Ngừng hiệu lực" (mục 1.5 nhóm 1).
4. **Given** phòng P201 còn giường có phân bổ đang hiệu lực hoặc phân bổ tương lai, **When** Quản lý viện Ngừng hiệu lực P201, **Then** hệ thống từ chối và liệt kê các giường và người cao tuổi liên quan.
5. **Given** P201 đang có một người cao tuổi nam (phòng chính sách "Không giới hạn"), **When** Quản lý viện đổi chính sách giới tính P201 sang "Nữ", **Then** hệ thống từ chối và chỉ ra người đang vi phạm chính sách mới; tương tự khi bỏ một mức chăm sóc khỏi danh sách cho phép mà người đang ở phòng có mức đó.
6. **Given** trưởng tầng hoặc nhân viên hành chính, **When** xem cơ cấu, **Then** thấy toàn bộ khu vực, tầng, phòng, giường và trạng thái giường; **When** cố tạo, sửa hoặc Ngừng hiệu lực, **Then** hệ thống từ chối (Permission Matrix dòng "Cấu hình phòng, giường": QL C, TT X, HC X).
7. **Given** phòng P201 đã có lịch sử phân bổ, **When** Quản lý viện cố chuyển P201 sang thuộc Tầng 3, **Then** hệ thống không cho đổi tầng của phòng; muốn tổ chức lại thì tạo phòng mới ở Tầng 3 và Ngừng hiệu lực P201.

---

### User Story 2 - Hành chính phân bổ giường; hệ thống chặn mọi phân bổ không hợp lệ và không bao giờ để một giường có hai người (Priority: P1)

Khi tiếp nhận người cao tuổi nội trú, hành chính (hoặc trưởng tầng trong phạm vi) chọn giường. Hệ thống chỉ gợi ý giường phù hợp và kiểm tra đầy đủ: loại phòng cho phép mức chăm sóc và loại lưu trú; giới tính phù hợp chính sách phòng; phòng không cách ly và không nằm trong vùng khoanh vùng; giường không bảo trì, không ngừng sử dụng, không đang giữ chỗ cho người khác; thời gian không trùng với phân bổ khác, kể cả phân bổ tương lai. Vi phạm bất kỳ điều kiện nào thì chặn. Phân bổ được lưu thành bản ghi có người cao tuổi, giường, thời điểm bắt đầu, lý do, người thực hiện.

**Why this priority**: Đây là quy tắc an toàn cốt lõi của module (7.3, DBR-09, DBR-10) và là điều kiện của "Hoàn tất tiếp nhận" với nội trú (5.6).

**Independent Test**: Chuẩn bị một phòng nữ chỉ cho "Chăm sóc cơ bản", một phòng cách ly, một giường bảo trì; thử phân bổ lần lượt từng người vi phạm từng điều kiện; cho hai tài khoản cùng phân bổ một giường gần như đồng thời.

**Acceptance Scenarios**:

1. **Given** người cao tuổi A (nữ, Đang tiếp nhận, loại lưu trú nội trú dài hạn, mức "Chăm sóc đặc biệt") và giường P201-G1 Trống, phòng P201 cho phép "Chăm sóc đặc biệt", chính sách "Nữ", không cách ly, **When** hành chính phân bổ G1 cho A từ thời điểm T kèm lý do "tiếp nhận", **Then** hệ thống tạo bản ghi phân bổ ở trạng thái Tương lai (A, G1, bắt đầu dự kiến T, kết thúc trống, lý do, người thực hiện), G1 vẫn Trống nhưng không nhận phân bổ trùng thời gian; **When** T đến mà A chưa Hoàn tất tiếp nhận, **Then** G1 vẫn Trống, phân bổ vẫn Tương lai, hành chính được báo; **When** hành chính thực hiện "Hoàn tất tiếp nhận" cho A lúc T' (feature 001), **Then** phân bổ chuyển Đang hiệu lực với bắt đầu thực tế = T' và G1 chuyển Đang sử dụng trong cùng lần thực hiện (FR-023); **When** thay vào đó A Hủy tiếp nhận, **Then** phân bổ chuyển Đã hủy, G1 không qua Chờ vệ sinh.
2. **Given** người cao tuổi B là nam, **When** phân bổ giường trong phòng chính sách "Nữ", **Then** hệ thống chặn với lý do "giới tính không phù hợp chính sách phòng" (BR-M03-01).
3. **Given** B có mức "Chăm sóc đặc biệt", **When** phân bổ giường trong phòng chỉ cho phép "Chăm sóc cơ bản", **Then** hệ thống chặn với lý do "loại phòng không cho phép mức chăm sóc".
4. **Given** phòng P105 đang cách ly, hoặc P105 thuộc khu đang khoanh vùng (BR-M05-11), **When** phân bổ bất kỳ giường nào trong P105, **Then** hệ thống chặn.
5. **Given** giường ở trạng thái Đang bảo trì hoặc Không sử dụng, **When** phân bổ, **Then** hệ thống chặn.
6. **Given** giường G2 Trống nhưng đã có phân bổ tương lai cho người C từ ngày 10/10, **When** phân bổ G2 cho người D từ ngày 01/10 mà không có thời điểm kết thúc, **Then** hệ thống chặn vì trùng thời gian với phân bổ tương lai; **When** D là nội trú ngắn ngày có ngày kết thúc dự kiến theo hợp đồng là 08/10, **Then** hệ thống chấp nhận.
7. **Given** một phân bổ vi phạm đồng thời nhiều điều kiện, **When** hành chính thực hiện, **Then** hệ thống liệt kê tất cả điều kiện vi phạm trong một lần từ chối.
8. **Given** hai nhân viên cùng chọn giường G1 Trống cho hai người khác nhau, **When** cả hai gửi gần như đồng thời, **Then** chỉ phân bổ đến trước được lưu; người sau nhận thông báo "giường đã được phân bổ" và phải chọn giường khác (BR-M03-02); tại mọi thời điểm G1 có tối đa một phân bổ đang hiệu lực (DBR-09).
9. **Given** người cao tuổi A đã có một phân bổ đang hiệu lực, **When** hành chính tạo thêm một phân bổ mới cho A (không phải bằng lệnh chuyển giường), **Then** hệ thống chặn và hướng tới lệnh "Chuyển giường" (DBR-09).
10. **Given** người cao tuổi có loại lưu trú bán trú, **When** phân bổ giường nội trú, **Then** hệ thống chặn (3.3; bán trú dùng khu nghỉ ban ngày, User Story 6).
11. **Given** người cao tuổi ở trạng thái Kết thúc lưu trú, Qua đời hoặc Hủy tiếp nhận, **When** phân bổ giường, **Then** hệ thống chặn.
12. **Given** hành chính mở chức năng phân bổ cho người cao tuổi A, **When** chọn giường, **Then** hệ thống chỉ gợi ý các giường thỏa mọi điều kiện của BR-M03-01 cho A, sắp theo khu vực, tầng, phòng.
13. **Given** giường G3 Đang giữ chỗ cho hồ sơ chờ của người E (BR-M02-02), **When** phân bổ G3 cho E, **Then** chấp nhận; G3 giữ Đang giữ chỗ cho E cho tới khi E Hoàn tất tiếp nhận, lúc đó phân bổ bắt đầu và G3 chuyển Đang sử dụng (FR-023); **When** phân bổ G3 cho người F khác, **Then** hệ thống chặn (DBR-10).
14. **Given** trưởng tầng được phân công Tầng 1, **When** phân bổ giường ở Tầng 2, **Then** hệ thống từ chối vì ngoài phạm vi; **Given** nhân viên hành chính, **When** phân bổ giường ở bất kỳ tầng nào, **Then** được xét tiếp theo điều kiện nghiệp vụ (Permission Matrix dòng "Phân bổ, chuyển giường": TT T, HC T; phạm vi theo feature 002).
15. **Given** điều dưỡng, bác sĩ hoặc nhân viên chăm sóc, **When** cố phân bổ giường, **Then** hệ thống từ chối.

---

### User Story 3 - Trưởng tầng, hành chính chuyển giường bằng một lệnh đóng bản ghi cũ và mở bản ghi mới (Priority: P1)

Khi cần đổi chỗ ở (đổi mức chăm sóc, xung đột cùng phòng, yêu cầu gia đình, lý do y tế), người có quyền thực hiện lệnh "Chuyển giường". Hệ thống kiểm tra giường mới theo đúng các điều kiện của phân bổ, rồi trong cùng một lần thực hiện: đóng bản ghi phân bổ cũ tại thời điểm chuyển và mở bản ghi mới từ chính thời điểm đó. Không có thao tác sửa giường trên bản ghi phân bổ hay trên hồ sơ người cao tuổi. Bản ghi đã đóng không sửa được.

**Why this priority**: Không có lệnh này, người dùng sẽ sửa giường trực tiếp và làm mất lịch sử vị trí mà truy vết lây nhiễm (BR-M05-10) và phạm vi dữ liệu (feature 002) dựa vào.

**Independent Test**: Chuyển người A từ P201-G1 sang P305-G2 lúc 14:00; kiểm tra bản ghi cũ kết thúc đúng 14:00, bản ghi mới bắt đầu đúng 14:00, G1 Chờ vệ sinh, G2 Đang sử dụng; mô phỏng lỗi giữa chừng và kiểm tra không có thay đổi nửa vời.

**Acceptance Scenarios**:

1. **Given** A đang có phân bổ hiệu lực ở P201-G1 và P305-G2 thỏa mọi điều kiện BR-M03-01, **When** trưởng tầng thực hiện "Chuyển giường" sang P305-G2 lúc T kèm lý do, **Then** bản ghi cũ được đóng với kết thúc = T, lý do kết thúc "chuyển giường"; bản ghi mới mở với bắt đầu = T, tham chiếu bản ghi cũ; G1 chuyển Chờ vệ sinh và công việc vệ sinh trả giường cho G1 được sinh (BR-M03-09); G2 chuyển Đang sử dụng; nhật ký ghi vị trí cũ, vị trí mới, thời điểm, lý do, người thực hiện (7.4, BR-M03-07).
2. **Given** P305-G2 vi phạm một điều kiện của BR-M03-01, **When** thực hiện lệnh, **Then** hệ thống từ chối, bản ghi cũ vẫn mở, G1 vẫn Đang sử dụng — không có trạng thái trung gian nào được lưu.
3. **Given** lệnh chuyển giường đang thực hiện, **When** bất kỳ bước nào không thành (ví dụ G2 vừa bị người khác phân bổ), **Then** toàn bộ lệnh không có hiệu lực: không có bản ghi nào bị đóng hay mở.
4. **Given** bản ghi phân bổ đã đóng, **When** bất kỳ ai (kể cả Quản lý viện) cố sửa giường, thời điểm bắt đầu hoặc kết thúc, **Then** hệ thống không có thao tác đó; sai sót được xử lý bằng đính chính theo feature 000.
5. **Given** lệnh chuyển giường thành công từ Tầng 2 sang Tầng 3, **When** hoàn tất, **Then** hệ thống kích hoạt feature 005 gán lại các công việc chưa thực hiện theo phân công của khu mới, công việc đã thực hiện giữ nguyên người thực hiện (BR-M03-04); phạm vi dữ liệu của nhân viên thay đổi theo vị trí mới ngay từ thao tác kế tiếp (7.4, feature 002).
6. **Given** G1 Chờ vệ sinh sau lệnh chuyển giường, **When** hệ thống chưa ghi nhận công việc vệ sinh trả giường Hoàn thành, **Then** BR-M02-01 chưa được kích hoạt cho G1; **When** công việc đó Hoàn thành, **Then** G1 chuyển Trống và hệ thống kích hoạt đề xuất hồ sơ chờ BR-M02-01 cho G1 (BR-M03-06, BR-M03-09, feature 004).
7. **Given** yêu cầu thay đổi lưu trú có đổi giường đã được duyệt với ngày hiệu lực tương lai D (BR-M02-04), **When** yêu cầu được duyệt, **Then** hệ thống kiểm tra BR-M03-01 cho giường mới và tạo phân bổ tương lai từ D, khóa giường mới khỏi các phân bổ trùng thời gian; **When** đến D, **Then** Bộ lập lịch hệ thống thực hiện lệnh "Chuyển giường" với người thực hiện là hệ thống và căn cứ là yêu cầu.
8. **Given** A chuyển sang giường trong phòng có loại phòng mang đơn giá khác (feature 004), **When** trưởng tầng thực hiện chuyển giường trực tiếp không kèm yêu cầu thay đổi lưu trú đã duyệt và lý do không thuộc nhóm "y tế/an toàn", **Then** hệ thống chặn và hướng tới lập yêu cầu thay đổi lưu trú (6.6); **When** lý do thuộc nhóm "y tế/an toàn", **Then** lệnh được thực hiện ngay, một yêu cầu thay đổi lưu trú Chờ duyệt với ngày hiệu lực mong muốn là ngày chuyển được tạo tự động và Quản lý viện được báo; **When** Quản lý viện từ chối yêu cầu, **Then** đơn giá cũ giữ nguyên, phân bổ mang dấu "chờ chuyển về phòng cùng giá" và trưởng tầng, hành chính được nhắc mỗi CFG-M03-02 cho tới khi A được chuyển về phòng cùng giá (FR-030).
9. **Given** hai lệnh chuyển giường khác nhau cho cùng A được gửi gần như đồng thời, **When** cả hai tới, **Then** chỉ lệnh đầu tiên được áp dụng; lệnh sau được kiểm tra lại trên vị trí mới và bị từ chối nếu không còn hợp lệ.
10. **Given** A ở P110 chỉ cho phép "Chăm sóc cơ bản" và có yêu cầu đổi mức sang "Chăm sóc đặc biệt" không kèm giường mới, **When** Quản lý viện xem duyệt, **Then** yêu cầu hiển thị cảnh báo "phòng hiện tại không cho phép mức chăm sóc mới" nhưng vẫn duyệt được; **When** thay đổi được áp dụng, **Then** mức chăm sóc hiện hành của A là "Chăm sóc đặc biệt", A vẫn ở P110, phân bổ mang dấu "phòng không còn phù hợp", trưởng tầng và hành chính được nhắc mỗi CFG-M03-02 (mặc định \[1 ngày\]); **When** A được chuyển sang giường thỏa FR-018, **Then** việc nhắc dừng và dấu được gỡ (FR-034).

---

### User Story 4 - Trạng thái giường chỉ thay đổi qua lệnh và sự kiện nghiệp vụ; giữ chỗ có lý do và hạn (Priority: P1)

Mỗi giường luôn ở đúng một trạng thái: Trống, Đang sử dụng, Đang bảo trì, Không sử dụng, Đang giữ chỗ, Chờ vệ sinh. Trạng thái Đang sử dụng do phân bổ quyết định; giường vừa kết thúc một phân bổ chuyển Chờ vệ sinh và chỉ về Trống khi vệ sinh trả giường Hoàn thành; Đang giữ chỗ có một trong hai lý do (giữ cho người đang vắng; giữ tạm cho hồ sơ chờ), mỗi lý do có hạn giữ; Đang bảo trì và Không sử dụng chỉ phát sinh từ lệnh trên tài sản của giường ở feature 019 (Q-234). Giường có người không được đưa vào bảo trì hay ngừng sử dụng.

**Why this priority**: Trạng thái giường là căn cứ của dashboard giường trống (18.5), đề xuất hồ sơ chờ (BR-M02-01) và chính sách giữ giường khi vắng (BR-M02-06); sai trạng thái dẫn đến nhận người vào giường đang có chủ.

**Independent Test**: Đi hết mọi chuyển trong bảng trạng thái giường (mục C), thử mọi chuyển không có trong bảng; dùng đồng hồ giả lập (NFR-13) đưa thời gian qua hạn giữ chỗ.

**Acceptance Scenarios**:

1. **Given** giường G1 Đang sử dụng, **When** Quản lý viện Đưa vào bảo trì hoặc Thanh lý tài sản của G1 (feature 019), **Then** hệ thống chặn và yêu cầu chuyển người sang giường khác trước (BR-M03-05, BR-M03-15).
2. **Given** giường G4 Trống và không có phân bổ tương lai, **When** Quản lý viện Đưa vào bảo trì tài sản của G4 kèm lý do (feature 019), **Then** G4 chuyển Đang bảo trì trong cùng một lần, lịch sử ghi người, thời điểm, lý do, căn cứ là lệnh tài sản; **When** feature 019 ghi kết quả bảo trì Đạt, **Then** G4 chuyển Trống và BR-M02-01 được kích hoạt (Q-234).
3. **Given** G4 Trống nhưng có phân bổ tương lai, **When** Quản lý viện Đưa vào bảo trì tài sản của G4 mà không gắn với hư hỏng phát hiện khi vệ sinh, **Then** hệ thống chặn và chỉ ra phân bổ tương lai (FR-013).
4. **Given** hồ sơ chờ của E chuyển Đã liên hệ với giường G3 (feature 004), **When** sự kiện xảy ra, **Then** G3 chuyển Đang giữ chỗ lý do "giữ tạm cho hồ sơ chờ", hạn = thời điểm chuyển + CFG-M02-02 (mặc định \[48 giờ\]) (BR-M02-02).
5. **Given** G3 Đang giữ chỗ cho hồ sơ chờ, **When** quá hạn giữ mà chưa có phân bổ, **Then** Bộ lập lịch hệ thống chuyển G3 thẳng về Trống (không qua Chờ vệ sinh vì chưa có người sử dụng) và kích hoạt BR-M02-01 (BR-M03-06).
6. **Given** người cao tuổi A Đang lưu trú ở G1 và chính sách phí khi vắng cho loại vắng của A là "giữ giường" (BR-M02-06), **When** lệnh "Cho tạm vắng" hoặc "Chuyển viện" của A được thực hiện, **Then** G1 chuyển Đang giữ chỗ lý do "giữ cho người đang vắng", hạn giữ theo ngưỡng giữ giường của chính sách áp dụng; bản ghi phân bổ của A giữ nguyên đang hiệu lực, không bị đóng (FR-024).
7. **Given** G1 Đang giữ chỗ cho A đang vắng, **When** A được "Ghi nhận trở về", **Then** G1 chuyển lại Đang sử dụng, A vẫn chỉ có đúng một bản ghi phân bổ cho G1 từ trước khi vắng; **When** quản lý chọn giải phóng giường theo BR-M02-07, **Then** phân bổ của A được đóng tại thời điểm giải phóng với lý do "giải phóng khi vắng", G1 chuyển Chờ vệ sinh (BR-M03-09).
8. **Given** chính sách phí khi vắng của A là "không giữ giường", **When** lệnh "Cho tạm vắng" được thực hiện, **Then** phân bổ của A được đóng tại thời điểm rời viện và G1 chuyển Chờ vệ sinh; khi A trở về, cần phân bổ mới.
9. **Given** A có phân bổ đang hiệu lực, **When** lệnh "Kết thúc lưu trú" hoặc "Ghi nhận qua đời" (feature 001, 004) được thực hiện, **Then** phân bổ của A được đóng tại thời điểm hiệu lực của lệnh với lý do tương ứng, giường chuyển Chờ vệ sinh và công việc vệ sinh trả giường được sinh (BR-M03-09); BR-M02-01 chỉ được kích hoạt khi giường về Trống; **When** lệnh "Hủy tiếp nhận" được thực hiện với A có phân bổ tương lai hoặc giữ chỗ, **Then** phân bổ tương lai chuyển Đã hủy và giữ chỗ bị hủy.
10. **Given** bất kỳ giường nào, **When** người dùng cố đặt trực tiếp trạng thái Đang sử dụng, Trống, Đang bảo trì, Không sử dụng, Đang giữ chỗ hoặc Chờ vệ sinh, **Then** hệ thống không có thao tác đó; các trạng thái này chỉ phát sinh từ phân bổ, kết quả công việc vệ sinh, lệnh trên tài sản của giường (feature 019) và các sự kiện ở bảng trạng thái giường.
11. **Given** G1 Chờ vệ sinh, **When** Quản lý viện hoặc hành chính cố đưa G1 về Trống mà công việc vệ sinh trả giường chưa Hoàn thành, **Then** hệ thống không có thao tác đó (BR-M03-09).
12. **Given** G2 Đang sử dụng bởi A, Trưởng tầng Báo hỏng tài sản của G2 (feature 019) nên G2 mang dấu "chờ chuyển người" và vẫn Đang sử dụng, **When** A được chuyển sang giường khác và vệ sinh trả giường G2 Hoàn thành, **Then** G2 về Trống nhưng BR-M02-01 không được kích hoạt, và mọi lệnh phân bổ vào G2 (kể cả phân bổ tương lai) bị chặn với lý do "giường chờ xử lý hư hỏng"; **When** Trưởng tầng Báo hỏng lại, **Then** G2 chuyển Đang bảo trì và dấu được gỡ; **When** thay vào đó Trưởng tầng gỡ dấu kèm lý do "đã sửa tại chỗ", **Then** G2 vẫn Trống và BR-M02-01 được kích hoạt (FR-013b, Q-235).

---

### User Story 5 - Tra cứu lịch sử vị trí phục vụ truy vết lây nhiễm (Priority: P2)

Bác sĩ, điều dưỡng khi khoanh vùng lây nhiễm (feature 007), hoặc quản lý khi xem xét khiếu nại, cần biết chính xác ai nằm giường nào vào một ngày trong quá khứ, người A đã ở những đâu, và ai ở cùng phòng với A trong một khoảng thời gian. Hệ thống trả lời từ bản ghi phân bổ, không cần tra sổ giấy.

**Why this priority**: 7.3 nêu mục đích trực tiếp của bản ghi phân bổ là trả lời câu hỏi này cho BR-M05-10. Cần User Story 2, 3 có trước để có dữ liệu.

**Independent Test**: Tạo lịch sử 30 ngày gồm phân bổ, chuyển giường, tạm vắng, kết thúc lưu trú cho 10 người trong 3 phòng; hỏi vị trí tại 5 thời điểm ngẫu nhiên và danh sách cùng phòng của một người trong 5 ngày; so với dữ liệu đã tạo.

**Acceptance Scenarios**:

1. **Given** A ở P201-G1 từ 01/09 08:00 đến 15/09 14:00 rồi ở P305-G2 từ 15/09 14:00, **When** hỏi "ngày 15/09 lúc 10:00 A ở đâu", **Then** trả P201-G1; **When** hỏi lúc 14:00, **Then** trả P305-G2 (thời điểm bắt đầu tính vào bản ghi mới, thời điểm kết thúc không tính vào bản ghi cũ).
2. **Given** lịch sử phân bổ của phòng P201, **When** hỏi "ngày 10/09 ai nằm giường nào trong P201", **Then** trả danh sách giường của P201 kèm người có phân bổ hiệu lực trong ngày đó, gồm cả người chỉ ở một phần ngày.
3. **Given** A ở P201 trong khoảng 01/09 → 15/09, **When** feature 007 yêu cầu danh sách người cùng phòng với A trong khoảng thời gian do feature 007 xác định (BR-M05-10), **Then** hệ thống trả mọi người có phân bổ ở P201 chồng thời gian với phân bổ của A, kèm khoảng thời gian chồng.
4. **Given** A đang vắng có giữ giường trong khoảng 05/09 → 07/09, **When** hỏi người cùng phòng thực tế, **Then** kết quả đánh dấu khoảng A vắng mặt (theo dữ liệu tạm vắng của feature 004) để điều dưỡng quyết định khi xác nhận danh sách tiếp xúc.
5. **Given** nhân viên chăm sóc không có quyền X với phân bổ, **When** tra cứu lịch sử vị trí toàn viện, **Then** hệ thống từ chối; nhân viên vẫn thấy phòng/giường hiện tại của người cao tuổi trong phạm vi trên hồ sơ (feature 001).

---

### User Story 6 - Khu nghỉ bán trú có sức chứa theo buổi; đăng ký lịch đến vượt sức chứa bị chặn (Priority: P2)

Người bán trú không có giường cố định; họ nghỉ tại khu nghỉ ban ngày. Viện có một khu nghỉ bán trú duy nhất, sức chứa mỗi buổi theo CFG-M03-01, mọi người bán trú tính chung. Khi hành chính đăng ký hoặc thay đổi lịch đến của người bán trú, hệ thống đếm số người đã đăng ký cho từng buổi và chặn nếu vượt sức chứa.

**Why this priority**: Bán trú là một loại hình lưu trú chính (3.3) nhưng không phụ thuộc giường; có thể chạy sau khi nội trú ổn định.

**Independent Test**: Đặt CFG-M03-01 là 3 (dữ liệu kiểm thử); đăng ký lịch đến sáng thứ Hai cho 3 người, thử người thứ 4; báo vắng một người rồi đăng ký lại người thứ 4; đăng ký lịch lặp hằng tuần có một ngày vượt.

**Acceptance Scenarios**:

1. **Given** viện chưa có khu nghỉ bán trú, **When** Quản lý viện tạo "Khu nghỉ ban ngày A" gắn Tầng 1, **Then** khu nghỉ ở trạng thái Hiệu lực, sức chứa mỗi buổi là giá trị hiện hành của CFG-M03-01; **When** Quản lý viện tạo thêm "Khu nghỉ ban ngày B", **Then** hệ thống từ chối vì viện chỉ có một khu nghỉ bán trú (FR-038).
2. **Given** CFG-M03-01 là 3 và đã có 3 người đăng ký sáng 06/10, **When** hành chính đăng ký người thứ 4 cho sáng 06/10, **Then** hệ thống chặn với lý do "vượt sức chứa khu nghỉ", nêu buổi và số chỗ còn lại (BR-M03-03).
3. **Given** lịch đến lặp hằng tuần từ 01/10 đến 31/12 mà có 2 buổi đã đủ chỗ, **When** hành chính đăng ký, **Then** hệ thống chặn và liệt kê các buổi vượt sức chứa trong một lần.
4. **Given** một người đã đăng ký sáng 06/10 được ghi nhận Vắng có báo (3.4), **When** đếm chỗ cho sáng 06/10, **Then** chỗ của người đó được tính là trống.
5. **Given** người đăng ký cả ngày, **When** đếm chỗ, **Then** người đó chiếm một chỗ ở mỗi buổi trong ngày.
6. **Given** đã có 3 người đăng ký sáng 06/10, **When** Quản lý viện giảm CFG-M03-01 xuống 2 (áp cho mọi buổi), **Then** thay đổi được lưu (tham số, feature 000), lịch đã đăng ký không bị hủy; hệ thống cảnh báo Quản lý viện danh sách buổi đang vượt và chặn đăng ký mới cho các buổi đó.
7. **Given** buổi sáng 07:00–12:00, buổi chiều 12:00–17:00, **When** hành chính đăng ký lịch đến 10:00–15:00 ngày 06/10, **Then** người đó chiếm một chỗ ở cả buổi sáng và buổi chiều 06/10, và bị chặn nếu một trong hai buổi đã đủ chỗ; **When** đăng ký 07:00–12:00, **Then** chỉ chiếm buổi sáng (FR-039).

---

### User Story 7 - Bác sĩ, Quản lý viện đặt và gỡ cách ly phòng (Priority: P2)

Khi có người nghi nhiễm, bác sĩ hoặc Quản lý viện đặt phòng vào trạng thái cách ly; khi khoanh vùng cả một khu (feature 007), mọi phòng trong khu chịu cùng các chặn. Phòng đang cách ly không nhận phân bổ mới. Chỉ vai trò có thẩm quyền mới gỡ được, kèm lý do; khi gỡ, các chặn tự động bỏ.

**Why this priority**: Là điều kiện chặn của BR-M03-01 và cơ chế của BR-M05-11, 12; phần quy trình lây nhiễm đầy đủ thuộc feature 007.

**Independent Test**: Đặt cách ly P105, thử phân bổ và chuyển giường vào/ra P105; gỡ cách ly bằng tài khoản trưởng tầng (bị từ chối) rồi bằng bác sĩ.

**Acceptance Scenarios**:

1. **Given** bác sĩ, **When** thực hiện "Đặt cách ly phòng" cho P105 kèm lý do và tham chiếu sự cố (nếu có), **Then** P105 ở trạng thái cách ly, lịch sử ghi người, thời điểm, lý do.
2. **Given** P105 đang cách ly, **When** phân bổ hoặc chuyển giường vào P105, **Then** hệ thống chặn (BR-M03-01).
3. **Given** P105 đang cách ly, **When** trưởng tầng chuyển người đang ở P105 sang phòng khác, **Then** hệ thống chặn; **When** trưởng tầng thực hiện lại lệnh kèm bác sĩ chỉ định "kiểm soát lây nhiễm" và lý do, sang giường ở phòng cách ly P107, **Then** lệnh được thực hiện dù P107 đang cách ly, bản ghi phân bổ mới và nhật ký ghi bác sĩ chỉ định; **When** thiếu bác sĩ chỉ định, **Then** hệ thống chặn (FR-010).
4. **Given** trưởng tầng, điều dưỡng hoặc hành chính, **When** cố đặt hoặc gỡ cách ly phòng, **Then** hệ thống từ chối (Permission Matrix dòng "Khoanh vùng lây nhiễm": QL T, BS T).
5. **Given** P105 đang cách ly, **When** bác sĩ "Gỡ cách ly phòng" kèm lý do, **Then** P105 hết cách ly và phân bổ vào P105 được xét lại bình thường (BR-M05-12); **When** thiếu lý do, **Then** hệ thống từ chối.
6. **Given** feature 007 khoanh vùng Tầng 1, **When** phân bổ giường mới ở bất kỳ phòng nào thuộc Tầng 1, **Then** hệ thống chặn (BR-M05-11); **When** feature 007 gỡ khoanh vùng, **Then** chặn tự động bỏ, trừ phòng vẫn đang cách ly riêng.

---

### User Story 8 - Vệ sinh phòng, giường và khu vực chung; giường chỉ được dùng lại sau khi vệ sinh xong (Priority: P2)

Quản lý viện khai báo lịch vệ sinh cho từng phòng và khu vực chung. Hệ thống tự sinh công việc vệ sinh định kỳ theo lịch, vệ sinh trả giường khi một phân bổ kết thúc, khử khuẩn khi khu bị khoanh vùng; nhân viên chăm sóc, điều dưỡng, trưởng tầng tạo yêu cầu vệ sinh đột xuất. Nhân viên vệ sinh ghi nhận từng hạng mục Đạt / Không đạt và hư hỏng; hạng mục Không đạt hoặc hư hỏng được báo trưởng tầng. Giường vừa có người rời đi ở trạng thái Chờ vệ sinh và chỉ về Trống khi vệ sinh trả giường Hoàn thành.

**Why this priority**: Chặn việc nhận người mới vào giường chưa được vệ sinh (7.5, BR-M03-09) và hỗ trợ kiểm soát lây nhiễm (BR-M03-10, 11). Cần User Story 1, 3, 4 có trước; vòng đời công việc dùng chung với feature 005.

**Independent Test**: Khai báo lịch vệ sinh cho 2 phòng và 1 hành lang; chạy thời điểm sinh công việc với đồng hồ giả lập; kết thúc lưu trú một người, ghi nhận vệ sinh trả giường có một hạng mục Không đạt liên quan giường; đánh dấu người rời giường là nghi nhiễm; khoanh vùng rồi gỡ khoanh vùng một tầng.

**Acceptance Scenarios**:

1. **Given** Quản lý viện, **When** khai báo lịch vệ sinh cho P201 gồm "lau phòng, hằng ngày, ca sáng, hạng mục {sàn, nhà vệ sinh, rác}" và "thay ga giường, hằng tuần thứ Hai, ca sáng, hạng mục {ga, gối}", **Then** lịch được lưu là dữ liệu danh mục (nhóm 1) (7.5, CFG-M03-03).
2. **Given** lịch của P201 và thời điểm sinh công việc CFG-M04-01, **When** Bộ lập lịch chạy, **Then** hệ thống sinh công việc vệ sinh định kỳ cho P201 cho ngày/ca tới, gắn với P201, không gắn người cao tuổi; chạy lại không tạo trùng (BR-M03-08, NFR-04).
3. **Given** mọi giường của P202 đang Không sử dụng, **When** Bộ lập lịch chạy, **Then** không sinh công việc vệ sinh định kỳ cho P202 (BR-M03-08); khu vực chung vẫn sinh theo lịch của nó.
4. **Given** A kết thúc lưu trú lúc T ở G1, **When** lệnh hoàn tất, **Then** G1 chuyển Chờ vệ sinh và hệ thống sinh công việc "vệ sinh trả giường" cho G1 với hạn T + CFG-M03-04 (mặc định \[4 giờ\]) (BR-M03-09).
5. **Given** công việc vệ sinh trả giường của G1 được ghi nhận Hoàn thành, **When** lưu kết quả, **Then** G1 chuyển Trống và BR-M02-01 được kích hoạt; **Given** công việc chưa Hoàn thành, **Then** G1 vẫn Chờ vệ sinh dù đã quá hạn, và quá hạn được xử lý theo feature 005 (BR-M04-05).
6. **Given** A rời G1 và A thuộc danh sách nghi nhiễm hoặc tiếp xúc (BR-M05-10, feature 007), **When** phân bổ đóng, **Then** hệ thống sinh công việc "khử khuẩn" thay cho vệ sinh trả giường; **When** nhân viên vệ sinh ghi nhận Hoàn thành mà chưa xác nhận đã dùng đồ bảo hộ, **Then** hệ thống chặn (BR-M03-10).
7. **Given** feature 007 khoanh vùng Tầng 1, **When** khoanh vùng có hiệu lực, **Then** hệ thống sinh công việc khử khuẩn cho mọi phòng và khu vực chung của Tầng 1 theo tần suất CFG-M03-05 (mặc định \[2 lần/ngày\]) cho tới khi gỡ khoanh vùng; **When** gỡ khoanh vùng, **Then** các công việc khử khuẩn định kỳ chưa đến hạn chuyển Hủy và hệ thống sinh một lần "khử khuẩn kết thúc" (BR-M03-11).
8. **Given** nhân viên chăm sóc trong phạm vi Tầng 2, **When** tạo yêu cầu vệ sinh đột xuất cho P205 với mô tả "nôn" và mức Gấp, **Then** hệ thống sinh công việc vệ sinh đột xuất gắn P205, hạn = thời điểm tạo + CFG-M03-06 (mặc định \[30 phút\]); **When** quá hạn chưa Hoàn thành, **Then** hệ thống cảnh báo trưởng tầng (BR-M03-12). **Given** nhân viên bếp hoặc hành chính, **When** tạo yêu cầu vệ sinh đột xuất, **Then** hệ thống từ chối.
9. **Given** kết quả vệ sinh của P201 có hạng mục "giường G1" Không đạt kèm hư hỏng "gãy thanh chắn", **When** lưu kết quả, **Then** hệ thống thông báo trưởng tầng phụ trách; **When** trưởng tầng gắn hư hỏng với tài sản của G1 và Báo hỏng (feature 019), **Then** G1 chuyển Đang bảo trì nếu không có phân bổ đang hiệu lực (BR-M03-13, BR-M03-05, Q-234); nếu G1 đang có người, trạng thái giữ nguyên và hư hỏng mang dấu "chờ chuyển người" (FR-013b, Q-232). **Given** G1 đang Chờ vệ sinh và có phân bổ tương lai cho người C bắt đầu ngày mai, **When** trưởng tầng Báo hỏng tài sản của G1 gắn với hư hỏng phát hiện khi vệ sinh, **Then** lệnh được chấp nhận, phân bổ tương lai của C chuyển Đã hủy với lý do "giường hỏng", hành chính và trưởng tầng được báo để đặt giường khác cho C (FR-013a).
10. **Given** công việc vệ sinh định kỳ của P201 chưa Hoàn thành khi hết ca, **When** ca kết thúc, **Then** công việc trở thành công việc chung của khu ở ca sau; **Given** yêu cầu vệ sinh Gấp chưa xong khi hết ca, **Then** yêu cầu được đưa vào bản nháp bàn giao (BR-M03-14, BR-M09-06, feature 008).
11. **Given** nhân viên vệ sinh được phân khu Tầng 2 trong ca, **When** mở checklist, **Then** thấy công việc vệ sinh của các phòng, khu vực thuộc Tầng 2 và công việc vệ sinh chung chưa có người nhận của khu; không thấy thông tin sức khỏe của người cao tuổi, kể cả lý do một công việc là khử khuẩn (19.3).

---

### Edge Cases

- **Phân bổ trước giường đang Chờ vệ sinh** (Q-40): giường G1 Chờ vệ sinh từ 10:00, hạn vệ sinh trả giường 14:00 (CFG-M03-04 \[4 giờ\]). Phân bổ cho người mới bắt đầu 12:00 hoặc bắt đầu ngay bị chặn; phân bổ tương lai bắt đầu 15:00 được chấp nhận và không kích hoạt BR-M02-01. Nếu đó là chuyển giường theo lịch và tới 15:00 vệ sinh vẫn chưa Hoàn thành, người cao tuổi ở lại giường cũ, phân bổ giữ Tương lai, trưởng tầng và hành chính được báo; vệ sinh Hoàn thành lúc 15:40 thì lệnh chuyển thực hiện lúc 15:40 và G1 chuyển Đang sử dụng (FR-011a). Nếu đó là phân bổ ban đầu của người Đang tiếp nhận, "Hoàn tất tiếp nhận" lúc 15:10 bị chặn vì giường chưa sẵn sàng, trừ khi hành chính chọn một giường Trống khác ngay trong lệnh (FR-023).
- **Người rời giường bị xác định nghi nhiễm sau khi giường đã Chờ vệ sinh**: A rời G1 lúc 09:00, vệ sinh trả giường hạn 13:00; lúc 10:30 feature 007 đưa A vào danh sách tiếp xúc — vệ sinh trả giường chuyển Hủy (lý do "nâng lên khử khuẩn"), khử khuẩn Bắt buộc được sinh với hạn 14:30 và yêu cầu xác nhận đồ bảo hộ, trưởng tầng được báo. Nếu G1 đã về Trống lúc 10:00, không tự sinh khử khuẩn; feature 007 quyết định (FR-045).
- **Vệ sinh trả giường bị Hủy hoặc Không thực hiện** (ví dụ trưởng tầng hủy nhầm, hoặc công việc Thường bị hệ thống đóng sau bàn giao theo feature 005): giường vẫn Chờ vệ sinh; hệ thống tự sinh ngay công việc thay thế cùng loại với hạn mới và báo trưởng tầng (FR-044a). Phân bổ tương lai đang chờ giường này (Q-40) vẫn chờ tới khi công việc thay thế Hoàn thành.
- **Chuyển giường khi giường đích vừa về Trống từ Chờ vệ sinh trong cùng lúc**: áp dụng FR-020, FR-033; lệnh sau được kiểm tra lại trên trạng thái mới.
- **Giường có phân bổ tương lai**: giường vẫn Trống cho tới khi phân bổ bắt đầu, hiển thị kèm "đã có phân bổ từ <thời điểm>"; chỉ nhận thêm phân bổ có thời điểm kết thúc dự kiến không muộn hơn thời điểm đó; không kích hoạt BR-M02-01.
- **Nội trú ngắn ngày gia hạn** (feature 004) mà giường đã có phân bổ tương lai của người khác chồng thời gian: hệ thống chặn việc kéo dài phân bổ trên giường đó; người thực hiện phải chuyển giường cho một trong hai người trước.
- **Mức chăm sóc thay đổi khi đang ở phòng không cho phép mức mới** (yêu cầu thay đổi lưu trú đổi mức, BR-M01-03): mức mới vẫn được áp dụng; hệ thống không tự chuyển giường, cảnh báo trên yêu cầu và nhắc trưởng tầng, hành chính theo CFG-M03-02 (mặc định \[1 ngày\]) cho tới khi chuyển xong (FR-034).
- **Chuyển giường khẩn cấp vì lý do y tế/an toàn** (ví dụ tách người có hành vi gây nguy hiểm, hoặc đưa vào phòng cách ly theo chỉ định bác sĩ ở FR-010) sang phòng có đơn giá khác: trưởng tầng thực hiện ngay với lý do thuộc nhóm "y tế/an toàn"; hệ thống tự tạo yêu cầu thay đổi lưu trú Chờ duyệt và báo Quản lý viện; duyệt thì đơn giá mới tính từ ngày chuyển (chênh lệch qua khoản điều chỉnh), từ chối thì giữ đơn giá cũ và nhắc chuyển người về phòng cùng giá (FR-030).
- **Chuyển giường theo lịch không thực hiện được vào ngày hiệu lực** (phòng mới vừa cách ly, người cao tuổi đang Điều trị tại bệnh viện): yêu cầu chuyển "Áp dụng không thành" theo feature 000, phân bổ tương lai chuyển Đã hủy, phân bổ cũ giữ nguyên, người liên quan được báo.
- **Bộ lập lịch chạy trễ**: chuyển giường theo lịch dự kiến 00:00 ngày 10/10, hệ thống gián đoạn và chạy lúc 02:30 — bản ghi cũ kết thúc 02:30, bản ghi mới bắt đầu 02:30, cả hai lưu giờ dự kiến 00:00 và mang dấu "thực hiện trễ" (vượt CFG-M03-08 \[30 phút\]), Quản lý viện được báo; đơn giá mới vẫn tính từ ngày 10/10 theo feature 000 (FR-016a).
- **Người vắng có giữ giường trở về khi phòng đang cách ly**: B vắng từ 01/10, giữ G2 ở P105; P105 bị đặt cách ly ngày 03/10; B trở về ngày 04/10. "Ghi nhận trở về" vẫn thực hiện được, người thực hiện thấy cảnh báo, bác sĩ trực và trưởng tầng được báo, phân bổ mang dấu "chờ bác sĩ xác nhận vùng cách ly" và dấu này có trong bàn giao ca cho tới khi bác sĩ xác nhận B ở lại, hoặc B được chuyển ra theo FR-010, hoặc P105 hết cách ly (FR-024a).
- **Người cao tuổi đang Tạm vắng hoặc Điều trị tại bệnh viện có giường giữ chỗ**: chuyển giường vẫn thực hiện được (ví dụ dọn phòng); giường mới nhận trạng thái Đang giữ chỗ cùng lý do và hạn còn lại, giường cũ chuyển Chờ vệ sinh (BR-M03-09).
- **Chuyển giường ban đêm, sáng hôm sau mới nhập** (nhập bù): chuyển thực tế lúc 02:00, nhập lúc 07:30 — trưởng tầng nhập bù trực tiếp (trong CFG-M03-07 \[24 giờ\]); nhập bù cho lần chuyển 3 ngày trước thì hệ thống lập yêu cầu phê duyệt cho Quản lý viện; nhập bù chồng lên khoảng người đó Tạm vắng thì bị chặn; mọi trường hợp chồng thời gian với phân bổ khác của giường hoặc của người cao tuổi đều bị chặn (FR-027).
- **Sai thời điểm trên bản ghi phân bổ đã đóng** (ví dụ chuyển lúc 14:00 nhưng ghi 16:00): đính chính theo feature 000; bản đính chính cũng phải thỏa ràng buộc không chồng thời gian (DBR-09), nếu không thì bị từ chối.
- **Người cao tuổi đổi giới tính trên hồ sơ** (sửa sai thông tin cá nhân, feature 001) trong khi đang ở phòng có chính sách giới tính khác: không tự chuyển; hệ thống cảnh báo trưởng tầng và hành chính để chuyển giường.
- **Ngừng hiệu lực tầng hoặc khu vực còn phân công nhân viên hiệu lực** (feature 008): hệ thống chặn và liệt kê phân công, để tránh phân công trỏ tới vị trí không còn hiệu lực (feature 002).
- **Hai lệnh làm thay đổi cùng một giường gần như đồng thời** (ví dụ phân bổ và đặt bảo trì): chỉ lệnh đến trước được áp dụng; lệnh sau được kiểm tra lại trên trạng thái mới.
- **Khu nghỉ bán trú Ngừng hiệu lực khi còn lịch đến tương lai**: hệ thống chặn và liệt kê người có lịch.

## Requirements *(mandatory)*

### Functional Requirements

Mọi yêu cầu dưới đây kế thừa feature 000: nhóm dữ liệu (mục 1.5), lý do bắt buộc cho lệnh nhóm 2, đính chính cho bản ghi đã xác nhận, nhật ký cho mọi thay đổi, tham số theo mã CFG, nguyên tắc "hoặc toàn bộ, hoặc không" khi một lệnh có nhiều tác động. Phân nhóm dữ liệu của feature này:

| Dữ liệu | Nhóm (1.5) | Thao tác được phép |
| --- | --- | --- |
| Khu vực, Tầng, Phòng, Giường (mã, tên, thuộc tính mô tả) | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện); giường không có Ngừng hiệu lực, ngừng dùng bằng Thanh lý tài sản (feature 019, Q-237); không xóa khi đã có lịch sử tham chiếu |
| Loại phòng | 1 – Danh mục | Tạo, sửa, Ngừng hiệu lực (Quản lý viện) |
| Khu nghỉ bán trú (duy nhất trong viện) | 1 – Danh mục; sức chứa là tham số CFG-M03-01 (feature 000) | Tạo (khi chưa có khu Hiệu lực), sửa, Ngừng hiệu lực (Quản lý viện) |
| Trạng thái giường | 2 | Chỉ qua lệnh và sự kiện ở bảng trạng thái giường (mục C) |
| Trạng thái cách ly phòng | 2 | Đặt cách ly, Gỡ cách ly (Bác sĩ, Quản lý viện); khoanh vùng khu (feature 007) |
| Phân bổ giường | 2 khi đang hiệu lực/tương lai; bất biến khi đã đóng hoặc đã hủy | Phân bổ, Chuyển giường, Đóng phân bổ (do lệnh của 001/004), Hủy phân bổ tương lai; không sửa; bản ghi đã đóng chỉ đính chính |
| Lịch sử trạng thái giường, lịch sử cách ly phòng | 3 | Chỉ ghi thêm |
| Lịch vệ sinh phòng/khu vực (mẫu lịch, hạng mục kiểm tra) | 1 – Danh mục (CFG-M03-03) | Tạo, sửa, Ngừng hiệu lực (Quản lý viện) |
| Công việc vệ sinh | 2 (vòng đời công việc 8.3, feature 005) | Sinh bởi hệ thống hoặc yêu cầu đột xuất; chuyển trạng thái theo feature 005 |
| Kết quả vệ sinh | 3 | Chỉ ghi thêm; sai sót xử lý bằng đính chính (feature 000) |

#### A. Cơ cấu khu vực, tầng, phòng, giường

- **FR-001**: Hệ thống MUST quản lý cơ cấu vị trí bốn cấp Khu vực → Tầng → Phòng → Giường; mỗi tầng thuộc đúng một khu vực, mỗi phòng thuộc đúng một tầng, mỗi giường thuộc đúng một phòng. Tầng MAY mang tên tòa nhà để thể hiện cấp "Tòa nhà/Tầng" của 2.2. *(Nguồn: 2.2, 7.1, UC-19)*
- **FR-002**: Chỉ Quản lý viện MUST được tạo, sửa khu vực, tầng, phòng, giường, loại phòng, khu nghỉ bán trú, và Ngừng hiệu lực các đối tượng đó trừ giường (FR-005, Q-237); Trưởng tầng và Hành chính MUST xem được toàn bộ cơ cấu và trạng thái giường; các vai trò khác chỉ thấy vị trí (phòng/giường) của người cao tuổi trong phạm vi qua hồ sơ (feature 001). *(Nguồn: 4.4 dòng "Cấu hình phòng, giường": QL C, TT X, HC X)*
- **FR-003**: Mã khu vực MUST duy nhất trong viện; mã tầng duy nhất trong khu vực; mã phòng duy nhất trong viện; mã giường duy nhất trong phòng. Vị trí đầy đủ MUST hiển thị dạng "Khu vực – Tầng – Phòng – Giường".
- **FR-004**: Mỗi phòng MUST có: loại phòng; danh sách mức chăm sóc được phép (khởi tạo từ loại phòng, Quản lý viện MAY thu hẹp hoặc mở rộng cho từng phòng); chính sách giới tính (Nam / Nữ / Không giới hạn); trạng thái cách ly. Loại phòng MUST có: tên, số giường tiêu chuẩn, danh sách mức chăm sóc được phép mặc định, loại hình lưu trú được phục vụ (nội trú dài hạn, nội trú ngắn ngày, hoặc cả hai). *(Nguồn: 7.1, 7.3, BR-M03-01)*
- **FR-005**: Đối tượng cơ cấu đã được lịch sử tham chiếu (phân bổ, phân công, công việc, sự cố) MUST NOT bị xóa, chỉ Ngừng hiệu lực. Giường MUST NOT có lệnh Ngừng hiệu lực; giường ngừng dùng vĩnh viễn bằng Thanh lý tài sản ở feature 019, khi đó giường chuyển Không sử dụng theo bảng trạng thái giường (Q-237). Ngừng hiệu lực MUST bị chặn khi: (a) với phòng — còn giường chưa ở Không sử dụng; (b) với tầng, khu vực — còn đối tượng con chưa Ngừng hiệu lực; (c) với phòng, tầng, khu vực — còn phân công nhân viên hiệu lực trỏ vào (feature 008). Khi chặn, hệ thống MUST liệt kê đối tượng gây chặn. Đối tượng Ngừng hiệu lực MUST NOT được chọn cho phân bổ mới và MUST vẫn hiển thị đúng trong lịch sử. *(Nguồn: 1.5 nhóm 1, 7.1; feature 000 FR-003; feature 002 FR-041b; Q-237)*
- **FR-006**: Thay đổi chính sách giới tính hoặc danh sách mức chăm sóc được phép của một phòng MUST bị chặn nếu làm người đang có phân bổ hiệu lực hoặc phân bổ tương lai trong phòng vi phạm điều kiện mới; hệ thống MUST chỉ ra người vi phạm. *(Suy ra từ BR-M03-01)*
- **FR-007**: Đối tượng cha của phòng hoặc giường đã có lịch sử phân bổ MUST NOT thay đổi; tổ chức lại cơ cấu được thực hiện bằng tạo đối tượng mới và Ngừng hiệu lực đối tượng cũ (với giường: Thanh lý tài sản của giường cũ, Q-237; giường chưa từng có phân bổ thì được đổi phòng, và vị trí tài sản giường ở feature 019 đổi theo, Q-243), để lịch sử vị trí và phạm vi dữ liệu tại các thời điểm cũ không bị thay đổi ngược. *(Nguồn: 7.3 "ngày X ai nằm giường nào")*

#### B. Trạng thái cách ly phòng và khoanh vùng

- **FR-008**: Bác sĩ và Quản lý viện MUST thực hiện được lệnh "Đặt cách ly phòng" và "Gỡ cách ly phòng", mỗi lệnh bắt buộc lý do và MAY tham chiếu sự cố (feature 007); lịch sử cách ly ghi người, thời điểm, lý do. Các vai trò khác MUST NOT thực hiện. *(Nguồn: 7.1, BR-M05-12, UC-37, 4.4 dòng "Khoanh vùng lây nhiễm": QL T, BS T)*
- **FR-009**: Một phòng MUST được coi là "đang chịu chặn cách ly" khi phòng đang cách ly hoặc phòng thuộc một khu vực/tầng đang bị khoanh vùng theo feature 007. Gỡ khoanh vùng MUST tự động bỏ chặn cho các phòng không đang cách ly riêng. *(Nguồn: BR-M03-01, BR-M05-11, BR-M05-12)*
- **FR-010**: Phòng đang chịu chặn cách ly MUST NOT nhận phân bổ mới hay chuyển giường vào. Người đang có phân bổ (kể cả giữ chỗ khi vắng) ở phòng chịu chặn cách ly MUST NOT được chuyển giường ra phòng khác hay giữa các phòng trong cùng vùng. Ngoại lệ duy nhất cho cả hai chiều (đưa người từ phòng thường vào phòng cách ly, và chuyển người đang ở vùng cách ly) là lệnh "Chuyển giường theo chỉ định kiểm soát lây nhiễm": do Trưởng tầng hoặc Hành chính thực hiện theo FR-029, bắt buộc ghi bác sĩ chỉ định (một bác sĩ, tài khoản Hoạt động) và lý do; với lệnh này, điều kiện (d) của FR-018 được miễn cho giường đích, nên giường đích MAY thuộc phòng cách ly khác hoặc cùng vùng khoanh vùng; các điều kiện còn lại của FR-018 vẫn áp dụng. Nhật ký và bản ghi phân bổ mới MUST ghi bác sĩ chỉ định. *(Nguồn: BR-M03-01, BR-M05-11; Clarification 2026-09-25, đề xuất Q-17)*

#### C. Trạng thái giường

- **FR-011**: Mỗi giường MUST luôn ở đúng một trạng thái trong: Trống, Đang sử dụng, Đang bảo trì, Không sử dụng, Đang giữ chỗ, Chờ vệ sinh. Chỉ các chuyển trong bảng dưới đây MUST được chấp nhận; MUST NOT có thao tác "sửa trạng thái giường". *(Nguồn: 7.2, 1.5 nhóm 2)*
- **FR-011a**: Khi một phân bổ đang hiệu lực được đóng (chuyển giường, kết thúc lưu trú, qua đời, giải phóng giường giữ cho người vắng, tạm vắng không giữ giường), giường MUST chuyển Chờ vệ sinh và hệ thống MUST sinh công việc vệ sinh trả giường theo FR-044. Giường Chờ vệ sinh MUST chỉ chuyển Trống khi công việc vệ sinh trả giường (hoặc khử khuẩn thay thế) của lần đóng phân bổ đó được ghi nhận Hoàn thành. Giường Chờ vệ sinh MUST NOT nhận phân bổ bắt đầu ngay; giường Chờ vệ sinh MAY nhận phân bổ tương lai (kể cả phân bổ tương lai từ chuyển giường theo lịch) chỉ khi thời điểm bắt đầu không sớm hơn hạn của công việc vệ sinh trả giường hoặc khử khuẩn thay thế đang mở (thời điểm đóng phân bổ + CFG-M03-04, mặc định \[4 giờ\]); các điều kiện khác của FR-018 vẫn áp dụng. Việc tạo phân bổ tương lai MUST NOT kích hoạt BR-M02-01. Với phân bổ tương lai từ chuyển giường theo lịch (FR-029): khi tới thời điểm bắt đầu mà giường chưa về Trống, phân bổ MUST giữ trạng thái Tương lai (người cao tuổi ở lại giường cũ), hệ thống MUST báo trưởng tầng phụ trách và hành chính; ngay khi công việc vệ sinh Hoàn thành và giường về Trống, lệnh chuyển giường MUST được thực hiện với thời điểm thực tế là thời điểm giường về Trống. Với phân bổ ban đầu của người Đang tiếp nhận, việc bắt đầu theo FR-023 (Hoàn tất tiếp nhận bị chặn khi giường chưa Trống). *(Nguồn: 7.2, BR-M03-06, BR-M03-09; Clarification 2026-09-26, Q-40)*
- **FR-012**: Trạng thái Đang giữ chỗ MUST có lý do là một trong hai: "giữ cho người đang vắng" (BR-M02-06) hoặc "giữ tạm cho hồ sơ chờ" (BR-M02-02); MUST có người cao tuổi hoặc hồ sơ chờ được giữ cho và hạn giữ. Hạn giữ cho hồ sơ chờ là CFG-M02-02 (mặc định \[48 giờ\]); hạn giữ cho người vắng là ngưỡng giữ giường theo feature 004 FR-055 (ngày vắng đầu tiên mà dòng chính sách phí khi vắng CFG-M02-05 áp dụng có giữ giường "Không" hoặc "Quản lý quyết định"); nếu mọi dòng áp dụng đều giữ giường thì giữ chỗ không có hạn, và hết giữ khi người đó trở về hoặc có lệnh giải phóng. *(Nguồn: 7.2, BR-M02-02, BR-M02-06)*
- **FR-013**: Giường có phân bổ đang hiệu lực (Đang sử dụng hoặc Đang giữ chỗ cho người vắng) MUST NOT chuyển sang Đang bảo trì hoặc Không sử dụng; phải chuyển người sang giường khác trước. Giường có phân bổ tương lai hoặc đang giữ chỗ cho hồ sơ chờ MUST NOT chuyển sang Đang bảo trì hoặc Không sử dụng, trừ ngoại lệ ở FR-013a. Đang bảo trì và Không sử dụng MUST chỉ phát sinh từ yêu cầu của feature 019 khi có lệnh trên tài sản của giường; spec này MUST NOT có lệnh đổi trạng thái giường riêng. Các điều kiện chặn của FR-013 được kiểm tra khi feature 019 gửi yêu cầu, và lệnh tài sản bị chặn theo đó. *(Nguồn: BR-M03-05, BR-M03-15, 7.2; Q-234)*
- **FR-013a**: Khi feature 019 yêu cầu đưa giường Trống hoặc Chờ vệ sinh có phân bổ tương lai sang Đang bảo trì bằng lệnh Báo hỏng hoặc Đưa vào bảo trì **gắn với hư hỏng phát hiện khi vệ sinh** (FR-048, feature 019 FR-016) hoặc là Báo hỏng lại giường mang dấu "chờ chuyển người" (FR-013b, Q-235), hoặc sang Không sử dụng bằng lệnh Thanh lý (feature 019 FR-015), yêu cầu MUST được chấp nhận; trong cùng một lần thực hiện, mọi phân bổ tương lai của giường MUST chuyển Đã hủy với lý do "giường hỏng", và hệ thống MUST báo hành chính và trưởng tầng phụ trách kèm danh sách người cao tuổi bị ảnh hưởng để đặt giường khác. Nếu phân bổ tương lai phát sinh từ chuyển giường theo lịch, yêu cầu thay đổi lưu trú tương ứng MUST chuyển "Áp dụng không thành" (feature 000) và người cao tuổi ở lại giường hiện tại. Nếu là phân bổ ban đầu của người Đang tiếp nhận, người đó không còn giường đặt trước nên "Hoàn tất tiếp nhận" sẽ bị chặn tới khi có phân bổ mới (FR-023). Ngoại lệ này MUST NOT áp dụng cho Báo hỏng hoặc Đưa vào bảo trì không thuộc hai trường hợp trên. *(Nguồn: BR-M03-13, BR-M03-05; Clarification 2026-09-26, Q-51; Q-234)*
- **FR-013b**: Giường mà tài sản mang dấu "chờ chuyển người" (feature 019 FR-015, Q-232) MUST NOT nhận phân bổ mới, kể cả phân bổ tương lai và phân bổ mở bằng chuyển giường (FR-018 (i)). Khi giường đó về Trống, hệ thống MUST NOT kích hoạt BR-M02-01; feature 019 nhắc Trưởng tầng báo hỏng lại mỗi CFG-M03-02 (mặc định \[1 ngày\]). Khi dấu được gỡ, spec này MUST kích hoạt BR-M02-01 nếu giường đang Trống và không có phân bổ tương lai. Dấu do Trưởng tầng (tầng mình) hoặc Quản lý viện gỡ kèm lý do, hoặc tự gỡ khi Báo hỏng lại. Phân bổ tương lai đã có khi gắn dấu MUST được giữ và hệ thống MUST báo hành chính và trưởng tầng phụ trách kèm danh sách người cao tuổi bị ảnh hưởng; tới thời điểm bắt đầu mà dấu còn thì phân bổ MUST giữ trạng thái Tương lai và áp cách xử lý như giường chưa về Trống ở FR-011a (Q-40); khi Báo hỏng lại, phân bổ tương lai xử lý theo FR-013a; khi gỡ dấu, phân bổ tương lai chạy tiếp. *(Nguồn: BR-M03-01, BR-M03-06, BR-M03-15; Q-232, Q-235)*
- **FR-014**: Khi giường chuyển sang Trống vì bất kỳ lý do nào (vệ sinh trả giường Hoàn thành, hết hạn hoặc hủy giữ chỗ cho hồ sơ chờ, bảo trì xong theo feature 019), hệ thống MUST kích hoạt đề xuất hồ sơ chờ BR-M02-01 (feature 004), trừ khi giường có phân bổ tương lai hoặc mang dấu "chờ chuyển người" (FR-013b). Giường chuyển sang Chờ vệ sinh MUST NOT kích hoạt BR-M02-01. *(Nguồn: BR-M03-06, BR-M03-09)*
- **FR-015**: Hết hạn giữ chỗ MUST được Bộ lập lịch hệ thống xử lý: giữ tạm cho hồ sơ chờ hết hạn thì giường về thẳng Trống, không qua Chờ vệ sinh (và hồ sơ về Đang chờ, feature 004); giữ cho người vắng hết hạn thì hệ thống MUST NOT tự giải phóng mà chuyển sang yêu cầu Quản lý viện chọn giữ tiếp hoặc giải phóng (BR-M02-07, feature 004). *(Nguồn: BR-M02-02, BR-M02-07, BR-M03-06)*
- **FR-016**: Mỗi chuyển trạng thái giường MUST ghi lịch sử: trạng thái từ, đến, lệnh hoặc sự kiện, thời điểm, người thực hiện (hoặc Bộ lập lịch hệ thống), lý do, căn cứ (phân bổ, hồ sơ chờ, lượt vắng, yêu cầu).
- **FR-016a**: Với mọi việc do Bộ lập lịch hệ thống thực hiện theo mốc thời gian (hết hạn giữ chỗ FR-015, chuyển giường theo lịch FR-029), thời điểm ghi trên bản ghi phân bổ và lịch sử trạng thái giường MUST là thời điểm Bộ lập lịch thực sự thực hiện; bản ghi MUST lưu kèm thời điểm dự kiến. Khi thời điểm thực hiện trễ hơn thời điểm dự kiến quá CFG-M03-08 (đề xuất, mặc định \[30 phút\]), bản ghi MUST mang dấu "thực hiện trễ" và hệ thống MUST báo Quản lý viện. Quy tắc này chỉ áp cho thời điểm vị trí và trạng thái giường; ngày hiệu lực dùng để tính đơn giá và chi phí vẫn theo feature 000 FR-037, FR-037a. Điều kiện của việc (ví dụ FR-018 cho chuyển giường theo lịch) MUST được kiểm tra tại thời điểm thực hiện thực tế. *(Nguồn: DBR-25, NFR-09, NFR-13; feature 000 FR-037a; Clarification 2026-09-26, đề xuất Q-48)*

**Bảng trạng thái giường** *(7.2, BR-M02-02, 06, 07, BR-M03-05, 06, 09, 13, 15; Q-234, Q-235; quyền theo 4.4 và feature 019)*:

| Trạng thái hiện tại | Lệnh / sự kiện | Trạng thái kế tiếp | Ai thực hiện | Điều kiện (chặn nếu không đạt) | Tác động |
| --- | --- | --- | --- | --- | --- |
| (chưa có) | Tạo giường | Trống | Quản lý viện | Phòng Hiệu lực; mã duy nhất trong phòng (FR-003) | Trong cùng một lần, tự ghi tăng đúng một tài sản loại "giường" ở Sẵn sàng (feature 019 FR-011, DBR-32, Q-238) |
| Trống | Phân bổ bắt đầu hiệu lực | Đang sử dụng | Hành chính, Trưởng tầng (lệnh Chuyển giường); hệ thống khi lệnh "Hoàn tất tiếp nhận" được thực hiện (FR-023); Bộ lập lịch khi chuyển giường theo lịch đến giờ (FR-029) | BR-M03-01 (FR-018) | — |
| Trống | Hồ sơ chờ chuyển Đã liên hệ (004) | Đang giữ chỗ (hồ sơ chờ) | Hệ thống, khi hành chính cập nhật hồ sơ chờ | Giường không có phân bổ tương lai chồng khoảng giữ | Hạn giữ = CFG-M02-02 |
| Đang giữ chỗ (hồ sơ chờ) | Phân bổ cho chính người được giữ | Đang sử dụng | Hành chính, Trưởng tầng | BR-M03-01 | Kết thúc giữ chỗ |
| Đang giữ chỗ (hồ sơ chờ) | Hết hạn giữ / hồ sơ từ chối / hủy chờ / hủy tiếp nhận | Trống | Bộ lập lịch hoặc hệ thống theo sự kiện của 004, 001 | — | BR-M02-01 (FR-014) |
| Đang sử dụng | Cho tạm vắng / Chuyển viện với chính sách giữ giường | Đang giữ chỗ (người vắng) | Hệ thống, khi lệnh của 001/004 được thực hiện | Chính sách phí khi vắng áp dụng là "giữ giường" (BR-M02-06) | Hạn giữ theo chính sách; phân bổ: xem FR-024 |
| Đang giữ chỗ (người vắng) | Ghi nhận trở về | Đang sử dụng | Hệ thống, khi lệnh của 001 được thực hiện | — | — |
| Đang giữ chỗ (người vắng) | Giải phóng giường (quản lý chọn theo BR-M02-07) | Chờ vệ sinh | Quản lý viện (qua yêu cầu của 004) | — | Đóng phân bổ lý do "giải phóng khi vắng"; sinh vệ sinh trả giường (FR-044) |
| Đang giữ chỗ (người vắng) | Chuyển giường (FR-032) | Chờ vệ sinh | Hành chính, Trưởng tầng | FR-018 cho giường mới | Sinh vệ sinh trả giường (FR-044) |
| Đang sử dụng | Đóng phân bổ (chuyển giường, kết thúc lưu trú, qua đời, tạm vắng không giữ giường) | Chờ vệ sinh | Người thực hiện lệnh tương ứng; hệ thống | — | Sinh vệ sinh trả giường hoặc khử khuẩn (FR-044, FR-045) |
| Chờ vệ sinh | Công việc vệ sinh trả giường / khử khuẩn thay thế Hoàn thành | Trống | Hệ thống, khi nhân viên vệ sinh ghi nhận kết quả (feature 005) | Công việc thuộc đúng lần đóng phân bổ của giường | BR-M02-01 (FR-014), trừ khi giường có phân bổ tương lai; nếu có chuyển giường theo lịch đã quá thời điểm bắt đầu thì lệnh chuyển thực hiện ngay và giường chuyển Đang sử dụng (FR-011a, Q-40) |
| Trống, Chờ vệ sinh | Feature 019: Báo hỏng hoặc Đưa vào bảo trì tài sản của giường | Đang bảo trì | Trưởng tầng (tầng mình), Quản lý viện, qua lệnh của 019 | Không có phân bổ tương lai, không giữ chỗ (FR-013), trừ lệnh gắn với hư hỏng phát hiện khi vệ sinh hoặc Báo hỏng lại giường mang dấu (FR-013a); có lý do | Với Chờ vệ sinh: công việc vệ sinh trả giường chưa xong vẫn giữ nguyên; phân bổ tương lai (nếu có) chuyển Đã hủy, báo hành chính và trưởng tầng (FR-013a); dấu "chờ chuyển người" (nếu có) được gỡ (FR-013b) |
| Trống, Chờ vệ sinh, Đang bảo trì | Feature 019: Thanh lý tài sản của giường | Không sử dụng | Quản lý viện, qua lệnh của 019 | Có lý do; phân bổ tương lai xử lý theo FR-013a | Trạng thái cuối của giường |
| Đang bảo trì | Feature 019: Ghi kết quả bảo trì Đạt | Trống | Quản lý viện, qua lệnh của 019 | Phòng và giường Hiệu lực | BR-M02-01 (FR-014) |
| Đang sử dụng, Đang giữ chỗ | Feature 019: Báo hỏng, Đưa vào bảo trì, Thanh lý | — (chặn) | — | Bị chặn bởi BR-M03-05; riêng Báo hỏng thì feature 019 ghi hư hỏng với dấu "chờ chuyển người", giường giữ nguyên (FR-013b, Q-232) | — |
| Chờ vệ sinh | Sửa trực tiếp về Trống | — (chặn) | — | Chỉ về Trống qua công việc vệ sinh Hoàn thành (BR-M03-09) | — |

#### D. Phân bổ giường

- **FR-017**: Phân bổ giường MUST được lưu thành bản ghi phân bổ gồm: người cao tuổi; giường; thời điểm bắt đầu; thời điểm kết thúc (trống khi chưa kết thúc); thời điểm kết thúc dự kiến (với nội trú ngắn ngày, theo hợp đồng); lý do bắt đầu; lý do kết thúc; người thực hiện mở và đóng; căn cứ (lệnh, yêu cầu thay đổi lưu trú); bản ghi trước (với phân bổ mở bằng chuyển giường); trạng thái bản ghi (Tương lai / Đang hiệu lực / Đã đóng / Đã hủy). Vị trí hiện tại của người cao tuổi MUST suy ra từ bản ghi phân bổ đang hiệu lực; MUST NOT có thao tác sửa giường trực tiếp trên hồ sơ người cao tuổi. *(Nguồn: 7.3, 3.2 PHAN_BO_GIUONG)*
- **FR-018**: Khi phân bổ (kể cả phân bổ tương lai và phân bổ mở bằng chuyển giường), hệ thống MUST kiểm tra toàn bộ các điều kiện sau và chặn nếu vi phạm bất kỳ điều kiện nào, liệt kê mọi điều kiện vi phạm trong một lần:
  (a) mức chăm sóc hiện hành của người cao tuổi (feature 001) thuộc danh sách mức được phép của phòng;
  (b) loại hình lưu trú của người cao tuổi là nội trú và thuộc loại hình được phục vụ của loại phòng;
  (c) giới tính người cao tuổi phù hợp chính sách giới tính của phòng;
  (d) phòng không đang chịu chặn cách ly (FR-009);
  (e) giường và phòng Hiệu lực; giường không Đang bảo trì, không Không sử dụng; nếu giường Chờ vệ sinh thì chỉ nhận phân bổ tương lai theo FR-011a;
  (f) giường không Đang giữ chỗ cho người khác hoặc hồ sơ chờ khác;
  (g) khoảng thời gian của phân bổ không chồng với bất kỳ phân bổ nào khác của cùng giường, kể cả phân bổ tương lai;
  (h) người cao tuổi không ở trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận);
  (i) tài sản của giường không mang dấu "chờ chuyển người" (FR-013b, BR-M03-01, Q-235).
  *(Nguồn: 7.3, BR-M03-01, DBR-10)*
- **FR-019**: Tại mọi thời điểm, mỗi giường MUST có tối đa một phân bổ đang hiệu lực, và mỗi người cao tuổi MUST có tối đa một phân bổ đang hiệu lực. Mỗi người cao tuổi MUST có tối đa một phân bổ tương lai; nếu người đó đang có phân bổ hiệu lực thì phân bổ tương lai MUST chỉ phát sinh từ lệnh chuyển giường theo lịch (FR-029). Hai khoảng thời gian được coi là chồng nhau khi có một thời điểm chung; thời điểm bắt đầu thuộc bản ghi, thời điểm kết thúc không thuộc bản ghi. *(Nguồn: 7.3, DBR-09)*
- **FR-020**: Khi hai người dùng phân bổ (hoặc chuyển tới) cùng một giường cho khoảng thời gian chồng nhau gần như đồng thời, chỉ thao tác đến trước MUST được lưu; người thao tác sau MUST nhận thông báo "giường đã được phân bổ" và chọn giường khác. Ràng buộc FR-019 MUST đúng trong mọi trường hợp đồng thời. *(Nguồn: BR-M03-02)*
- **FR-021**: Hành chính và Trưởng tầng MUST thực hiện được lệnh "Phân bổ giường"; Hành chính trong toàn viện, Trưởng tầng chỉ với giường và người cao tuổi thuộc phạm vi được phân công (feature 002). Mọi vai trò khác MUST NOT thực hiện. Lý do MUST bắt buộc. *(Nguồn: UC-20, 4.4 dòng "Phân bổ, chuyển giường": TT T, HC T, QL X; feature 002)*
- **FR-022**: Khi người dùng chọn giường cho một người cao tuổi, hệ thống MUST gợi ý chỉ các giường thỏa FR-018 cho người đó và khoảng thời gian dự kiến, kèm vị trí đầy đủ; giường có phân bổ tương lai MUST hiển thị thời điểm bắt đầu của phân bổ đó.
- **FR-023**: Phân bổ ban đầu cho người cao tuổi Đang tiếp nhận MUST được tạo ở trạng thái Tương lai với thời điểm bắt đầu dự kiến (đặt trước giường cho ngày vào) và MUST NOT tự bắt đầu theo đồng hồ. Phân bổ này MUST bắt đầu khi và chỉ khi lệnh "Hoàn tất tiếp nhận" (feature 001) được thực hiện, trong cùng một lần thực hiện với lệnh, với thời điểm bắt đầu thực tế là thời điểm thực hiện lệnh (sớm hay muộn hơn dự kiến đều được, miễn khoảng thời gian mới vẫn thỏa FR-018 (g)). Nếu lúc thực hiện lệnh giường đặt trước chưa ở trạng thái Trống (ví dụ còn Chờ vệ sinh, Đang bảo trì), lệnh "Hoàn tất tiếp nhận" MUST bị chặn với điều kiện chưa thỏa "giường đặt trước chưa sẵn sàng", trừ khi người thực hiện chọn một giường Trống khác thỏa FR-018 ngay trong lệnh; khi đó, trong cùng một lần thực hiện, phân bổ tương lai cũ chuyển Đã hủy (lý do "đổi giường khi tiếp nhận") và phân bổ mới trên giường được chọn bắt đầu tại thời điểm thực hiện lệnh. Người cao tuổi MUST NOT ở trạng thái Đang lưu trú (nội trú) mà không có phân bổ Đang hiệu lực. *(Clarification 2026-09-26, đề xuất Q-50)* Phân bổ Tương lai này đáp ứng điều kiện "nội trú đã có giường" của Hoàn tất tiếp nhận. Khi quá thời điểm bắt đầu dự kiến mà người cao tuổi chưa Hoàn tất tiếp nhận, hệ thống MUST báo hành chính (một lần cho mỗi thời điểm bắt đầu dự kiến); phân bổ vẫn giữ giường cho tới khi Hoàn tất tiếp nhận, bị Hủy phân bổ tương lai hoặc người cao tuổi Hủy tiếp nhận. Vì vậy người Đang tiếp nhận không bao giờ có phân bổ Đang hiệu lực. Phân bổ tương lai MUST được lệnh "Hủy phân bổ tương lai" (có lý do) chuyển Đã hủy trước khi bắt đầu; MUST tự động chuyển Đã hủy khi người cao tuổi Hủy tiếp nhận (feature 001). Bản ghi Đã hủy MUST được giữ lại, không xóa. *(Nguồn: BR-M03-01 "kể cả phân bổ tương lai", 5.6; Clarification 2026-09-26, đề xuất Q-42)*
- **FR-024**: Khi lệnh "Cho tạm vắng" hoặc "Chuyển viện" được thực hiện và chính sách phí khi vắng áp dụng là "giữ giường", giường MUST chuyển Đang giữ chỗ (người vắng) và bản ghi phân bổ của người đó MUST giữ nguyên đang hiệu lực suốt thời gian vắng; khi "Ghi nhận trở về", giường MUST quay lại Đang sử dụng mà không tạo bản ghi phân bổ mới. Khoảng vắng mặt được xác định từ dữ liệu tạm vắng/chuyển viện (feature 001, 004), không từ bản ghi phân bổ (FR-036). Nếu giữ giường kết thúc bằng giải phóng (BR-M02-07), phân bổ được đóng theo FR-025. Khi chính sách là "không giữ giường", phân bổ MUST được đóng tại thời điểm rời viện và giường chuyển Chờ vệ sinh (FR-011a). *(Nguồn: 6.7, BR-M02-06, 5.6, DBR-10, BR-M03-09)*
- **FR-024a**: Khi "Ghi nhận trở về" được thực hiện mà giường đang giữ chỗ của người đó thuộc phòng đang chịu chặn cách ly (FR-009), hệ thống MUST NOT chặn lệnh; giường chuyển Đang sử dụng như FR-024, nhưng hệ thống MUST: (1) cảnh báo người thực hiện trước khi xác nhận lệnh; (2) báo ngay bác sĩ trực và trưởng tầng phụ trách; (3) đánh dấu phân bổ "chờ bác sĩ xác nhận vùng cách ly". Dấu này MUST chỉ được gỡ khi (a) một bác sĩ xác nhận người đó ở lại giường cũ, kèm lý do, hoặc (b) người đó được chuyển ra giường ngoài vùng bằng lệnh "Chuyển giường theo chỉ định kiểm soát lây nhiễm" (FR-010), hoặc (c) phòng hết chịu chặn cách ly. Trong khi dấu còn, cảnh báo MUST được đưa vào bàn giao ca (feature 008) và hiển thị trên danh sách giường. *(Nguồn: BR-M03-01, BR-M05-11, 1.3 "cảnh báo chưa xử lý đưa vào bàn giao"; Clarification 2026-09-26, đề xuất Q-46)*
- **FR-025**: Khi lệnh "Kết thúc lưu trú" hoặc "Ghi nhận qua đời" được thực hiện, phân bổ đang hiệu lực của người đó MUST được đóng tại thời điểm hiệu lực của lệnh và phân bổ tương lai (nếu có) MUST chuyển Đã hủy, trong cùng một lần thực hiện với lệnh (feature 001 FR-047). Khi Quản lý viện chọn giải phóng giường theo BR-M02-07, phân bổ MUST được đóng tại thời điểm giải phóng. Trong cả hai trường hợp, giường chuyển Chờ vệ sinh (FR-011a). *(Nguồn: 5.6, 6.8, BR-M02-07, BR-M03-09)*
- **FR-026**: Bản ghi phân bổ Đã đóng hoặc Đã hủy MUST NOT bị sửa; sai sót về thời điểm hoặc giường được xử lý bằng đính chính theo feature 000, và bản đính chính MUST thỏa FR-019. Bản ghi đang hiệu lực MUST chỉ thay đổi qua các lệnh ở FR-023 → FR-029. *(Nguồn: BR-M03-07, 1.5)*
- **FR-027**: "Nhập bù" là lệnh Chuyển giường trực tiếp có thời điểm chuyển trong quá khứ (phân bổ ban đầu luôn bắt đầu theo FR-023 nên không nhập bù). Nhập bù MUST: (a) do Hành chính hoặc Trưởng tầng (trong phạm vi, FR-029) thực hiện trực tiếp khi thời điểm chuyển lùi không quá CFG-M03-07 (đề xuất, mặc định \[24 giờ\]) so với thời điểm thực hiện; (b) khi lùi xa hơn, được lập thành yêu cầu phê duyệt (vòng đời feature 000) và chỉ có hiệu lực khi Quản lý viện duyệt, với điều kiện được kiểm tra lại lúc duyệt; (c) bị chặn nếu khoảng thời gian lùi chồng lên khoảng người cao tuổi Tạm vắng, Điều trị tại bệnh viện hoặc Hoạt động bên ngoài (dữ liệu feature 001, 004), hoặc lùi trước thời điểm bắt đầu của phân bổ hiện tại; (d) thỏa FR-018 và FR-019 trên toàn bộ khoảng thời gian lùi; (e) được đánh dấu "nhập bù" trên bản ghi phân bổ và trong nhật ký, kèm thời điểm thực hiện thật. Các vai trò khác MUST NOT nhập bù. *(Nguồn: 7.3, 1.3; Clarification 2026-09-26, đề xuất Q-43)*

#### E. Chuyển phòng/giường

- **FR-028**: Lệnh "Chuyển giường" MUST, trong cùng một lần thực hiện: kiểm tra FR-018 cho giường mới; đóng bản ghi phân bổ hiện tại với thời điểm kết thúc = thời điểm chuyển, lý do kết thúc "chuyển giường"; mở bản ghi mới với thời điểm bắt đầu = thời điểm chuyển, tham chiếu bản ghi cũ; cập nhật trạng thái hai giường theo bảng mục C. Nếu bất kỳ bước nào không thành, MUST không có thay đổi nào được lưu. Lệnh MUST ghi vị trí cũ, vị trí mới, thời điểm, lý do, người thực hiện. *(Nguồn: 7.4, BR-M03-07, UC-21)*
- **FR-029**: Hành chính và Trưởng tầng MUST thực hiện được lệnh "Chuyển giường" trực tiếp (hiệu lực ngay); Trưởng tầng chỉ khi cả người cao tuổi và giường mới thuộc phạm vi được phân công. Chuyển giường theo lịch (ngày hiệu lực tương lai) MUST chỉ phát sinh từ yêu cầu thay đổi lưu trú đã duyệt (feature 004): khi yêu cầu được duyệt, hệ thống MUST kiểm tra FR-018 và tạo phân bổ tương lai cho giường mới; vào ngày hiệu lực, Bộ lập lịch hệ thống MUST thực hiện lệnh Chuyển giường với căn cứ là yêu cầu. Nếu khi đó FR-018 không còn thỏa, yêu cầu MUST chuyển "Áp dụng không thành" (feature 000), phân bổ tương lai chuyển Đã hủy, phân bổ cũ giữ nguyên. *(Nguồn: UC-21, 6.6, BR-M02-04; feature 000 FR-037, FR-038)*
- **FR-030**: Chuyển giường trực tiếp sang phòng có loại phòng mang đơn giá khác (feature 004) MUST bị chặn và hướng tới yêu cầu thay đổi lưu trú, trừ khi lý do thuộc nhóm "y tế/an toàn". Khi đó lệnh được thực hiện ngay và, trong cùng lần thực hiện, hệ thống MUST tự tạo một yêu cầu thay đổi lưu trú loại "đổi phòng/giường" (feature 004) ở trạng thái Chờ duyệt, người yêu cầu là "Hệ thống" (như yêu cầu tự tạo theo BR-M01-03), căn cứ là lệnh chuyển giường kèm người đã thực hiện lệnh, ngày hiệu lực mong muốn là ngày chuyển; yêu cầu MUST được giao cho hành chính theo dõi (hành chính được bổ sung thông tin và hủy theo quyền T của dòng UC-13), MUST báo Quản lý viện, và kết quả duyệt/từ chối MUST được báo cho người thực hiện lệnh chuyển giường và hành chính. Trưởng tầng MUST NOT được ghi là người yêu cầu (Permission Matrix dòng UC-13: TT "—"). Trong lúc chờ, đơn giá giữ theo hợp đồng hiện hành. Nếu yêu cầu được duyệt, đơn giá mới áp dụng từ ngày chuyển; phần chênh lệch trước ngày hiệu lực thực tế (feature 000 FR-037) được xử lý bằng khoản điều chỉnh (feature 010), không sửa khoản đã ghi. Nếu yêu cầu bị từ chối, đơn giá cũ giữ nguyên, phân bổ được đánh dấu "chờ chuyển về phòng cùng giá" và hệ thống MUST nhắc trưởng tầng phụ trách và hành chính theo chu kỳ CFG-M03-02 (mặc định \[1 ngày\]) cho tới khi người cao tuổi được chuyển sang giường có cùng đơn giá hợp đồng, hoặc một yêu cầu thay đổi lưu trú khác được áp dụng. Hệ thống MUST NOT tự chuyển giường. *(Nguồn: 6.6 "thay đổi ảnh hưởng đến chi phí cần phê duyệt", 4.4 dòng UC-13; Clarification 2026-09-26, đề xuất Q-44, Q-52)*
- **FR-031**: Sau khi chuyển giường thành công, hệ thống MUST kích hoạt: feature 005 gán lại các công việc chưa thực hiện theo phân công của khu mới, công việc đã thực hiện giữ nguyên người thực hiện (BR-M03-04); phạm vi dữ liệu của nhân viên theo vị trí mới có hiệu lực từ thao tác kế tiếp (7.4, feature 002); giường cũ chuyển Chờ vệ sinh và sinh vệ sinh trả giường (FR-011a); BR-M02-01 cho giường cũ chỉ khi giường về Trống (FR-014). *(Nguồn: 7.4, BR-M03-04, BR-M03-06, BR-M03-09)*
- **FR-032**: Khi người cao tuổi đang có giường Đang giữ chỗ (người vắng) được chuyển giường, giường mới MUST nhận trạng thái Đang giữ chỗ với cùng lý do và hạn giữ còn lại; giường cũ chuyển Chờ vệ sinh (FR-011a).
- **FR-033**: Khi hai lệnh chuyển giường (hoặc một lệnh chuyển giường và một lệnh khác làm thay đổi phân bổ) cho cùng một người cao tuổi được gửi gần như đồng thời, chỉ lệnh đầu tiên MUST được áp dụng; lệnh sau MUST được kiểm tra lại trên trạng thái mới.
- **FR-034**: Khi mức chăm sóc hiện hành, giới tính hoặc loại hình lưu trú của người cao tuổi thay đổi làm phân bổ đang hiệu lực không còn thỏa FR-018 (a), (b) hoặc (c), hệ thống MUST NOT tự chuyển giường và MUST NOT chặn việc duyệt hay áp dụng thay đổi đó. Cụ thể: (1) khi một yêu cầu đổi mức chăm sóc được lập hoặc xem duyệt (feature 004) mà phòng hiện tại không cho phép mức mới, hệ thống MUST hiển thị cảnh báo "phòng hiện tại không cho phép mức chăm sóc mới" trên yêu cầu; (2) từ khi thay đổi được áp dụng, phân bổ được đánh dấu "phòng không còn phù hợp" và hệ thống MUST nhắc trưởng tầng phụ trách và hành chính theo chu kỳ CFG-M03-02 (đề xuất, mặc định \[1 ngày\]) cho tới khi người cao tuổi được chuyển sang giường thỏa FR-018 hoặc điều kiện của phòng thay đổi để thỏa lại; (3) dấu "phòng không còn phù hợp" MUST hiển thị trên danh sách giường và dashboard tình trạng giường (feature 016). *(Nguồn: BR-M01-03, BR-M03-01; Clarification 2026-09-25, đề xuất Q-19)*

#### F. Tra cứu lịch sử vị trí

- **FR-035**: Hệ thống MUST trả lời được, từ bản ghi phân bổ: (a) tại thời điểm T, người cao tuổi A ở giường nào; (b) tại thời điểm T hoặc trong ngày D, mỗi giường của một phòng/tầng/khu vực có ai; (c) trong khoảng thời gian cho trước, những ai có phân bổ trong cùng phòng với A, kèm khoảng thời gian chồng. Kết quả MUST tính cả bản ghi đã đóng và bản ghi đã được đính chính (theo nội dung đính chính). *(Nguồn: 7.3, BR-M05-10)*
- **FR-036**: Kết quả tra cứu (c) MUST đánh dấu các khoảng người cao tuổi vắng mặt (Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài — dữ liệu từ feature 001, 004) để điều dưỡng xem xét khi xác nhận danh sách tiếp xúc (feature 007).
- **FR-037**: Tra cứu lịch sử vị trí toàn viện MUST dành cho các vai trò có quyền X hoặc T ở dòng "Phân bổ, chuyển giường" và "Khoanh vùng lây nhiễm" (Quản lý viện, Trưởng tầng trong phạm vi, Hành chính, Bác sĩ); điều dưỡng MUST tra cứu được qua chức năng khoanh vùng của feature 007. Các vai trò khác chỉ thấy vị trí hiện tại của người cao tuổi trong phạm vi. *(Nguồn: 4.4)*

#### G. Khu nghỉ bán trú

- **FR-038**: Viện MUST có đúng một khu nghỉ bán trú, do Quản lý viện khai báo và gắn với một tầng hoặc khu vực; sức chứa của khu nghỉ ở mỗi buổi MUST bằng giá trị hiện hành của tham số CFG-M03-01 (mặc định: theo cơ sở), như nhau cho mọi buổi. Danh mục buổi là cấu hình của cơ sở; mỗi buổi MUST có tên và khung giờ bắt đầu–kết thúc trong ngày (ví dụ sáng 07:00–12:00, chiều 12:00–17:00), các khung giờ MUST NOT chồng nhau. Mọi người bán trú MUST được tính chung vào sức chứa này; hợp đồng và lịch đến không chọn khu nghỉ. Hệ thống MUST NOT cho khai báo khu nghỉ bán trú thứ hai khi đã có một khu Hiệu lực. Dời khu nghỉ sang nơi khác được thực hiện bằng cách sửa tầng/khu vực gắn với khu nghỉ hiện có (dữ liệu nhóm 1), không cần tạo khu mới; lịch đến đã đăng ký giữ nguyên. Tầng hoặc khu vực mà khu nghỉ bán trú gắn vào MUST được cung cấp cho feature 011: phiếu bữa ăn của khu bán trú do nhân viên có phân công tại tầng/khu vực đó nhận (feature 011 FR-048, BR-M08-11). *(Nguồn: 7.1, BR-M03-03, CFG-M03-01; Clarification 2026-09-26, đề xuất Q-45; đồng bộ spec 011)*
- **FR-039**: Khi đăng ký hoặc thay đổi lịch đến của người bán trú (feature 004/005), hệ thống MUST đếm số người đã đăng ký cho từng buổi của khu nghỉ bán trú, không tính người đã được ghi nhận Vắng có báo (3.4) cho buổi đó; nếu số đăng ký sau thay đổi vượt sức chứa ở bất kỳ buổi nào, hệ thống MUST chặn và liệt kê mọi buổi vượt cùng số chỗ còn lại. Một lịch đến chiếm một chỗ ở mọi buổi có khung giờ giao với khoảng [giờ đến, giờ về) của lịch đó (giờ bắt đầu thuộc khoảng, giờ kết thúc không thuộc, cùng quy ước FR-019); vì vậy đăng ký cả ngày chiếm một chỗ ở mỗi buổi trong ngày, và lịch 07:00–12:00 không chiếm buổi 12:00–17:00. *(Nguồn: BR-M03-03; Clarification 2026-09-26, đề xuất Q-47)*
- **FR-040**: Giảm sức chứa xuống dưới số đã đăng ký MUST NOT hủy lịch đã đăng ký; hệ thống MUST cảnh báo Quản lý viện danh sách buổi đang vượt và MUST chặn đăng ký mới cho các buổi đó cho tới khi số đăng ký thấp hơn sức chứa.
- **FR-041**: Người bán trú MUST NOT được phân bổ giường nội trú (FR-018 b); khu nghỉ bán trú MUST NOT dùng cho phân bổ giường.
- **FR-042**: Khu nghỉ bán trú đang chịu khoanh vùng (feature 007) MUST NOT nhận đăng ký lịch đến mới cho các buổi trong thời gian khoanh vùng. *(Suy ra từ BR-M05-11)*

#### H. Vệ sinh phòng và khu vực

Công việc vệ sinh dùng chung vòng đời trạng thái, checklist, quá hạn và ghi nhận của feature 005 (8.3); khác biệt là gắn với phòng, giường hoặc khu vực chung thay vì người cao tuổi. Đối tượng vệ sinh: phòng; giường; khu vực chung (hành lang, nhà vệ sinh chung, phòng ăn, phòng sinh hoạt, khu nghỉ bán trú).

- **FR-043**: Quản lý viện MUST khai báo được lịch vệ sinh cho từng phòng và khu vực chung, mỗi mục gồm: loại vệ sinh (định kỳ); tần suất; ca thực hiện; danh sách hạng mục kiểm tra (ví dụ sàn, nhà vệ sinh, giường, rác). Lịch là dữ liệu danh mục; thay đổi lịch chỉ áp cho công việc sinh sau thời điểm sửa. Quản lý viện MUST khai báo được khu vực chung như một đối tượng vệ sinh gắn với tầng hoặc khu vực. *(Nguồn: 7.5, CFG-M03-03, 1.5 nhóm 1, UC-71, 4.4 dòng "Lịch vệ sinh": QL C, TT X)*
- **FR-044**: Vào thời điểm CFG-M04-01, Bộ lập lịch MUST sinh công việc vệ sinh định kỳ cho ngày/ca tới từ lịch vệ sinh (FR-043); MUST NOT sinh cho phòng có toàn bộ giường Không sử dụng; chạy lại MUST NOT tạo trùng. Khi một phân bổ đóng (FR-011a), hệ thống MUST sinh ngay một công việc "vệ sinh trả giường" gắn với giường đó, hạn = thời điểm đóng + CFG-M03-04 (mặc định \[4 giờ\]), hạng mục mặc định gồm giường, tủ, nệm. *(Nguồn: BR-M03-08, BR-M03-09, 7.5, UC-72)*
- **FR-044a**: Khi công việc vệ sinh trả giường hoặc khử khuẩn thay thế của một giường đang Chờ vệ sinh chuyển Hủy hoặc Không thực hiện (bởi người dùng hoặc hệ thống, feature 005; trừ trường hợp nâng lên khử khuẩn ở FR-045), hệ thống MUST, trong cùng một lần thực hiện, sinh ngay một công việc thay thế cùng loại cho cùng giường và cùng lần đóng phân bổ, hạn = thời điểm sinh + CFG-M03-04 (mặc định \[4 giờ\]), giữ nguyên yêu cầu xác nhận đồ bảo hộ nếu là khử khuẩn, tham chiếu công việc bị đóng; và MUST báo trưởng tầng phụ trách kèm lý do đóng. Giường MUST giữ trạng thái Chờ vệ sinh. Tại mọi thời điểm, mỗi giường Chờ vệ sinh MUST có đúng một công việc vệ sinh trả giường (hoặc khử khuẩn thay thế) đang mở. *(Nguồn: BR-M03-09; Clarification 2026-09-26, đề xuất Q-41)*
- **FR-044b**: Mức quan trọng (feature 005) của công việc vệ sinh MUST cố định theo loại, không do cơ sở cấu hình: vệ sinh trả giường — Quan trọng; khử khuẩn thay thế vệ sinh trả giường, khử khuẩn theo khoanh vùng và khử khuẩn kết thúc — Bắt buộc; vệ sinh định kỳ theo lịch — Thường; vệ sinh đột xuất mức ưu tiên Thường — Thường; vệ sinh đột xuất mức ưu tiên Gấp — Quan trọng. Công việc thay thế sinh theo FR-044a MUST giữ mức quan trọng của loại. Vì vệ sinh trả giường và khử khuẩn không ở mức Thường, chúng MUST NOT bị tự đóng sau bàn giao theo feature 005; quá hạn được nhắc, leo thang và đưa vào bàn giao theo mức tương ứng của feature 005. *(Nguồn: BR-M03-09, BR-M03-10, BR-M03-11, BR-M03-12, 8.3; Clarification 2026-09-26, đề xuất Q-49)*
- **FR-045**: Nếu người vừa rời giường thuộc danh sách nghi nhiễm hoặc tiếp xúc tại thời điểm đóng phân bổ (BR-M05-10, dữ liệu từ feature 007), hệ thống MUST sinh công việc "khử khuẩn" thay cho vệ sinh trả giường, cùng hạn CFG-M03-04; ghi nhận Hoàn thành MUST bắt buộc xác nhận đã dùng đồ bảo hộ. Công việc hiển thị cho nhân viên vệ sinh MUST NOT nêu tên người rời giường hay lý do sức khỏe. Nếu người rời giường được thêm vào danh sách nghi nhiễm hoặc tiếp xúc (feature 007) sau thời điểm đóng phân bổ, trong khi giường vẫn Chờ vệ sinh và công việc đang mở là vệ sinh trả giường, hệ thống MUST, trong cùng một lần thực hiện: đóng công việc vệ sinh trả giường với trạng thái Hủy, lý do "nâng lên khử khuẩn"; sinh công việc khử khuẩn thay thế (mức Bắt buộc theo FR-044b, bắt buộc xác nhận đồ bảo hộ, hạn = thời điểm sinh + CFG-M03-04, tham chiếu công việc bị đóng); báo trưởng tầng phụ trách. Trường hợp này MUST NOT kích hoạt FR-044a (không sinh lại vệ sinh trả giường). Nếu giường đã về Trống trước khi người đó được thêm vào danh sách, spec này không tự sinh khử khuẩn; feature 007 quyết định yêu cầu khử khuẩn đột xuất. *(Nguồn: BR-M03-10, 19.3; Clarification 2026-09-26, đề xuất Q-53)*
- **FR-046**: Khi feature 007 khoanh vùng một khu, hệ thống MUST sinh công việc khử khuẩn cho mọi phòng và khu vực chung trong vùng theo tần suất CFG-M03-05 (mặc định \[2 lần/ngày\]) cho tới khi gỡ khoanh vùng. Khi gỡ khoanh vùng, công việc khử khuẩn định kỳ của vùng ở trạng thái Chưa đến hạn MUST chuyển Hủy (lý do: gỡ khoanh vùng) và hệ thống MUST sinh một lần "khử khuẩn kết thúc" cho mỗi phòng và khu vực chung trong vùng. *(Nguồn: BR-M03-11, BR-M05-11, BR-M05-12)*
- **FR-047**: Nhân viên chăm sóc, Điều dưỡng và Trưởng tầng MUST tạo được yêu cầu vệ sinh đột xuất cho phòng hoặc khu vực chung trong phạm vi được phân công, gồm: phòng/khu vực, mô tả, mức ưu tiên Thường / Gấp. Yêu cầu tạo ngay một công việc vệ sinh đột xuất; mức Gấp có hạn = thời điểm tạo + CFG-M03-06 (mặc định \[30 phút\]), quá hạn thì hệ thống MUST cảnh báo trưởng tầng phụ trách. Hạn của mức Thường theo khung mặc định của loại công việc (CFG-M04-02, feature 005). Các vai trò khác MUST NOT tạo. *(Nguồn: BR-M03-12, UC-73, 4.4 dòng "Yêu cầu vệ sinh đột xuất": TT, ĐD, CS T; VS P chỉ xem trong phạm vi; QL X)*
- **FR-048**: Kết quả vệ sinh MUST gồm: kết quả từng hạng mục (Đạt / Không đạt); hư hỏng phát hiện (nếu có, kèm hạng mục và mô tả); ghi chú; người thực hiện; thời điểm. Khi có hạng mục Không đạt hoặc hư hỏng, hệ thống MUST thông báo trưởng tầng phụ trách. Khi hư hỏng liên quan đến giường, trưởng tầng (trong phạm vi) hoặc Quản lý viện MAY gắn hư hỏng với tài sản của giường và Báo hỏng hoặc Đưa vào bảo trì ở feature 019 (FR-016 của 019); giường đổi trạng thái theo bảng trạng thái giường, chịu điều kiện BR-M03-05 (FR-013, FR-013a, FR-013b; Q-234). Hạng mục Không đạt MUST NOT tự đổi trạng thái công việc; công việc vẫn đóng theo feature 005. *(Nguồn: 7.5, BR-M03-13, UC-74)*
- **FR-049**: Công việc vệ sinh chưa Hoàn thành khi hết ca MUST trở thành công việc chung của khu ở ca sau. Yêu cầu vệ sinh Gấp chưa Hoàn thành khi hết ca MUST được đưa vào bản nháp bàn giao (feature 008). *(Nguồn: BR-M03-14, BR-M09-06)*
- **FR-049a**: **(Đồng bộ spec 014, BR-M04-23, Q-174)** Khi feature 014 ghi kết quả kiểm tra chất lượng Không đạt cho một công việc vệ sinh, hệ thống MUST sinh một công việc vệ sinh làm lại cùng loại, cùng phòng/khu vực, mức quan trọng bằng công việc gốc (Q-49), thời điểm dự kiến trong ca của danh sách kiểm tra, giao người thực hiện gốc nếu còn trong ca, nếu không thì thành việc chung của tầng; công việc làm lại trỏ về công việc gốc và kết quả kiểm tra. Kết quả Không đạt MUST NOT tự đổi trạng thái giường; công việc vệ sinh của khu vực chung không gắn tầng/khu vực không thuộc mẫu kiểm tra. *(Nguồn: BR-M04-23; feature 014 FR-060, FR-064)*
- **FR-050**: Người thực hiện công việc vệ sinh MUST lấy theo phân công nhân viên vệ sinh theo khu vực trong ca (feature 008); công việc chưa có người nhận là công việc chung của khu. Nhân viên vệ sinh MUST chỉ xem công việc vệ sinh của phòng/khu vực được phân công và công việc chung của khu đó; MUST NOT xem thông tin sức khỏe của người cao tuổi. *(Nguồn: 7.5, 13.4, 19.3)*

### Key Entities *(include if feature involves data)*

- **Khu vực (KHU_VUC)** – nhóm 1: mã, tên, trạng thái Hiệu lực / Ngừng hiệu lực.
- **Tầng (TANG)** – nhóm 1: mã, tên, tên tòa nhà (tùy chọn), khu vực, trạng thái.
- **Loại phòng** – nhóm 1: tên, số giường tiêu chuẩn, mức chăm sóc được phép mặc định, loại hình lưu trú được phục vụ, trạng thái.
- **Phòng (PHONG)** – nhóm 1 cho thuộc tính mô tả; trạng thái cách ly thay đổi qua lệnh: mã, tên, tầng, loại phòng, mức chăm sóc được phép, chính sách giới tính, trạng thái cách ly, trạng thái Hiệu lực.
- **Lịch sử cách ly phòng** – nhóm 3: phòng, đặt/gỡ, thời điểm, người thực hiện, lý do, sự cố tham chiếu.
- **Giường (GIUONG)** – nhóm 1 cho mã; trạng thái nhóm 2: mã, phòng, trạng thái, lý do giữ chỗ, đối tượng được giữ cho (người cao tuổi hoặc hồ sơ chờ), hạn giữ chỗ, trạng thái Hiệu lực.
- **Lịch sử trạng thái giường** – nhóm 3: giường, trạng thái từ, đến, lệnh/sự kiện, thời điểm, người thực hiện, lý do, căn cứ.
- **Phân bổ giường (PHAN_BO_GIUONG)** – nhóm 2, bất biến khi đã đóng/hủy: người cao tuổi, giường, bắt đầu, kết thúc, kết thúc dự kiến, lý do bắt đầu, lý do kết thúc, người mở, người đóng, căn cứ, bản ghi trước, trạng thái bản ghi (Tương lai / Đang hiệu lực / Đã đóng / Đã hủy), bác sĩ chỉ định (FR-010), dấu "nhập bù" kèm thời điểm thực hiện thật (FR-027), dấu "phòng không còn phù hợp" (FR-034), dấu "chờ chuyển về phòng cùng giá" (FR-030), dấu "chờ bác sĩ xác nhận vùng cách ly" kèm bác sĩ xác nhận và lý do (FR-024a), thời điểm dự kiến và dấu "thực hiện trễ" với việc do Bộ lập lịch thực hiện (FR-016a).
- **Khu nghỉ bán trú** – nhóm 1, duy nhất trong viện: mã, tên, tầng/khu vực, trạng thái; sức chứa mỗi buổi lấy từ CFG-M03-01, không lưu riêng.
- **Khu vực chung** – nhóm 1: mã, tên, loại (hành lang, nhà vệ sinh chung, phòng ăn, phòng sinh hoạt, khu nghỉ bán trú), tầng/khu vực, trạng thái.
- **Lịch vệ sinh** – nhóm 1: đối tượng (phòng hoặc khu vực chung), loại vệ sinh, tần suất, ca thực hiện, danh sách hạng mục kiểm tra, trạng thái.
- **Công việc vệ sinh** – dùng thực thể Công việc của feature 005 với đối tượng là phòng, giường hoặc khu vực chung: loại (định kỳ / trả giường / khử khuẩn / khử khuẩn kết thúc / đột xuất), nguồn sinh (lịch, phân bổ đã đóng, khoanh vùng, yêu cầu đột xuất), mức ưu tiên (với đột xuất), mức quan trọng cố định theo loại (FR-044b), hạn, yêu cầu xác nhận đồ bảo hộ, công việc bị thay thế (FR-044a).
- **Kết quả vệ sinh** – nhóm 3: công việc, kết quả từng hạng mục, hư hỏng, ghi chú, xác nhận đồ bảo hộ, người thực hiện, thời điểm.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 trường hợp một giường có hai phân bổ chồng thời gian, hoặc một người cao tuổi có hai phân bổ đang hiệu lực, trong mọi kịch bản kiểm thử, kể cả kịch bản 10 người dùng cùng phân bổ một giường trong cùng một giây.
- **SC-002**: 100% phân bổ vi phạm ít nhất một điều kiện của BR-M03-01 bị chặn, và người dùng thấy đầy đủ danh sách điều kiện vi phạm ngay trong lần từ chối đầu tiên.
- **SC-003**: Với bộ dữ liệu kiểm thử 30 ngày lịch sử, 100% câu hỏi "tại thời điểm T ai nằm giường nào" và "ai ở cùng phòng với A trong khoảng thời gian" trả kết quả khớp dữ liệu gốc; điều dưỡng nhận được danh sách người cùng phòng trong không quá 1 phút, không tra sổ giấy.
- **SC-004**: 100% lệnh chuyển giường thành công có bản ghi cũ kết thúc đúng bằng thời điểm bản ghi mới bắt đầu; 100% lệnh bị lỗi giữa chừng không để lại thay đổi nửa vời.
- **SC-005**: Hành chính hoàn tất phân bổ giường cho một người cao tuổi mới (từ lúc mở chức năng tới khi lưu) trong không quá 2 phút với cơ sở 300 giường, nhờ danh sách chỉ gồm giường phù hợp.
- **SC-006**: 0 buổi có số đăng ký bán trú vượt sức chứa khu nghỉ do đăng ký mới (chỉ có thể vượt khi Quản lý viện chủ động giảm sức chứa, và khi đó 100% buổi vượt được cảnh báo).
- **SC-007**: 100% chuyển trạng thái giường trong đợt kiểm thử thuộc bảng trạng thái giường và có bản ghi lịch sử đầy đủ; 100% giường giữ chỗ cho hồ sơ chờ quá hạn CFG-M02-02 được trả về Trống và kích hoạt BR-M02-01 trong đợt kiểm thử với đồng hồ giả lập.
- **SC-008**: Dashboard tình trạng giường (feature 016) và số giường trống theo điều kiện khớp 100% với trạng thái giường tại cùng thời điểm.
- **SC-009**: 0 phân bổ bắt đầu trên giường chưa có vệ sinh trả giường (hoặc khử khuẩn thay thế) Hoàn thành sau lần đóng phân bổ gần nhất, trong mọi kịch bản kiểm thử.
- **SC-010**: 100% lần đóng phân bổ trong đợt kiểm thử sinh đúng một công việc vệ sinh trả giường hoặc khử khuẩn, đúng loại theo danh sách nghi nhiễm/tiếp xúc tại thời điểm đóng.
- **SC-011**: Trong đợt kiểm thử với đồng hồ giả lập, 100% phòng/khu vực trong vùng khoanh vùng có công việc khử khuẩn đúng tần suất CFG-M03-05 và đúng một lần khử khuẩn kết thúc khi gỡ; 100% yêu cầu vệ sinh Gấp quá hạn sinh cảnh báo trưởng tầng.

## Assumptions

- Số feature `003` theo bản đồ feature ở mục 4.2 `docs/phan-tich-yeu-cau.md` (UC-19 → UC-21) và trùng số tuần tự kế tiếp.
- Quy tắc dùng chung (nhóm dữ liệu, đính chính, yêu cầu phê duyệt, nhật ký, tham số) theo feature 000 và không lặp lại ở đây.
- Cấp "Tòa nhà/Tầng" ở 2.2 được thể hiện bằng Tầng có thuộc tính tên tòa nhà; không tách thêm một cấp Tòa nhà.
- Kiểm tra mức chăm sóc của BR-M03-01 dùng danh sách mức chăm sóc được phép của phòng (7.1); trọng số chăm sóc (mục 4) không tham gia kiểm tra phân bổ ở spec này (xem điểm báo lại 1).
- "Phù hợp loại lưu trú" (7.3) được thể hiện bằng thuộc tính loại hình lưu trú được phục vụ của loại phòng; mặc định phục vụ cả nội trú dài hạn và ngắn ngày.
- Thời điểm phân bổ, chuyển giường tính đến phút theo múi giờ Asia/Ho_Chi_Minh, lấy theo đồng hồ hệ thống (DBR-25, NFR-09).
- **(Sửa 2026-09-29, Q-234)** Trạng thái Đang bảo trì / Không sử dụng chỉ phát sinh từ lệnh trên tài sản của giường ở feature 019. Quyền theo Phụ lục 27 dòng "Tài sản, lịch xe" (QL mọi lệnh; Trưởng tầng Báo hỏng, Đưa vào bảo trì tài sản trong tầng mình, chú thích ³³). Hư hỏng phát hiện ngoài công việc vệ sinh được báo qua feature 019 (BR-M03-18).
- Công việc vệ sinh là một nhóm của thực thể Công việc ở feature 005, gắn với phòng/giường/khu vực chung; spec này quy định nguồn sinh và tác động lên giường, feature 005 quy định vòng đời và ghi nhận.
- Hạng mục mặc định của vệ sinh trả giường (giường, tủ, nệm) lấy theo ví dụ ở 7.5; cơ sở MAY cấu hình thêm trong danh mục loại công việc (feature 005 FR-038).
- Trạng thái cách ly phòng do Bác sĩ và Quản lý viện đặt/gỡ, theo quyền của dòng "Khoanh vùng lây nhiễm" (UC-37); "khu" trong BR-M05-11 được hiểu là khu vực hoặc tầng do feature 007 chọn khi khoanh vùng.
- Chuyển giường theo lịch chỉ phát sinh từ yêu cầu thay đổi lưu trú (6.6, BR-M02-04); chuyển trực tiếp luôn có hiệu lực ngay.
- Nhóm lý do "y tế/an toàn" cho phép chuyển giường sang phòng khác đơn giá mà không chờ duyệt là danh mục lý do do cơ sở cấu hình.
- Sức chứa khu nghỉ bán trú được đếm theo lịch đăng ký (BR-M03-03 "đăng ký lịch đến"), không theo số người có mặt thực tế; người bán trú không được gán chỗ nghỉ cụ thể. Viện có một khu nghỉ bán trú duy nhất (Q-45); nếu sau này cần nhiều khu, phải sửa FR-038 và CFG-M03-01.
- Trưởng tầng thực hiện phân bổ/chuyển giường trong phạm vi được phân công, Hành chính trong toàn viện, theo Clarification của feature 002.

## Điểm cần báo lại về tài liệu nguồn

Ngày 2026-09-25, các điểm đã chốt đã được đưa vào `docs/nghiep-vu.md` và `docs/phan-tich-yeu-cau.md` theo yêu cầu. Danh sách dưới đây ghi nơi đã phản ánh và các điểm còn mở.

**Đã phản ánh vào tài liệu nguồn**

1. Trọng số chăm sóc không tham gia kiểm tra phòng; BR-M03-01 dùng danh sách mức chăm sóc được phép: mục 4.
2. Loại hình lưu trú được phục vụ của loại phòng: 7.1 và thực thể KHU_VUC, TANG, PHONG ở 3.2.
3. DBR-09 nói theo "đang hiệu lực", tối đa một phân bổ tương lai mỗi người: DBR-09.
4. DBR-10 "giữ tạm cho hồ sơ chờ của chính người đó", thêm loại hình lưu trú và ngoại lệ Q-17: DBR-10.
5. Quyền đặt/gỡ cách ly phòng (Quản lý viện, Bác sĩ): 7.1 và dòng "Đặt, gỡ cách ly phòng" của Permission Matrix 4.4.
6. Tòa nhà là thuộc tính tùy chọn của tầng: thực thể KHU_VUC, TANG, PHONG ở 3.2.
7. Chuyển giường trực tiếp khác với chuyển qua yêu cầu thay đổi lưu trú: 7.4 và 6.6.
8. Q-17 (ngoại lệ chuyển vì kiểm soát lây nhiễm), Q-18 (phân bổ giữ nguyên khi giữ giường cho người vắng), Q-19 (đổi mức chăm sóc không bị chặn): BR-M03-01, 7.2, BR-M01-03, mục 24.2; bác sĩ chỉ định lưu ở thực thể PHAN_BO_GIUONG.
9. Tham số CFG-M03-02: Phụ lục 25.

**Còn mở**

1. **"Khu" trong BR-M05-11** là khu vực, tầng hay tập phòng do feature 007 chọn: cần chốt khi làm spec feature 007.
2. **BR-M05-10 không nêu khoảng thời gian xét "người cùng phòng"** (CFG-M05-07 \[5 ngày\] chỉ gắn với hoạt động chung): spec này nhận khoảng thời gian làm tham số đầu vào từ feature 007; cần chốt khi làm spec feature 007.
3. **Đặt giường Đang bảo trì / Không sử dụng chỉ do Quản lý viện**: giả định theo dòng "Cấu hình phòng, giường" (QL C). Từ 2026-09-26, BR-M03-13 cho trưởng tầng đưa giường sang Đang bảo trì khi vệ sinh phát hiện hư hỏng; Permission Matrix 4.4 trong `docs/phan-tich-yeu-cau.md` cần bổ sung quyền này. *(2026-09-29: không còn áp dụng; Đang bảo trì, Không sử dụng chỉ qua lệnh tài sản ở feature 019, quyền theo Phụ lục 27 dòng "Tài sản, lịch xe", Q-234.)*
4. **Vệ sinh trả giường bị Hủy hoặc Không thực hiện**: đã chốt khi clarify 2026-09-26 — hệ thống tự sinh công việc thay thế cùng loại và báo trưởng tầng (FR-044a). Cần bổ sung vào BR-M03-09 và thêm Q-41 vào mục 24.
5. **Q-40** (phân bổ trước giường Chờ vệ sinh): đã chốt khi clarify 2026-09-26 theo mặc định mục 24.1, bổ sung cách xử lý khi tới giờ bắt đầu mà vệ sinh chưa xong (FR-011a); cần chuyển Q-40 từ 24.1 sang các quyết định đã chốt và ghi phần bổ sung vào 7.2.
6. **Quyền vệ sinh trong Permission Matrix 4.4**: đã có ở tài liệu nguồn (dòng "Lịch vệ sinh" UC-71, "Yêu cầu vệ sinh đột xuất" UC-73; ghi nhận kết quả vệ sinh theo dòng "Checklist, ghi nhận công việc", VS T); spec đã dẫn UC-71 → UC-74 ở FR-043, FR-044, FR-047, FR-048. Còn thiếu: quyền trưởng tầng đưa giường hỏng sang Đang bảo trì (UC-74) chưa có dòng trong 4.4 (xem điểm 3). *(2026-09-29: xử lý theo điểm 3.)*
7. **Phân bổ ban đầu bắt đầu theo lệnh Hoàn tất tiếp nhận** (đề xuất Q-42, FR-023): cần ghi vào 7.3 và dòng "Đang tiếp nhận → Đang lưu trú" của 5.6 (tác động: bắt đầu phân bổ giường), thêm Q-42 vào mục 24.
8. **Giới hạn nhập bù** (đề xuất Q-43, FR-027): tham số mới CFG-M03-07 (hạn nhập bù trực tiếp, mặc định 24 giờ) cần bổ sung vào Phụ lục 25; loại yêu cầu phê duyệt "Nhập bù phân bổ" cần bổ sung vào danh sách loại yêu cầu của feature 000 và Permission Matrix (QL D).
9. **Chuyển giường gấp sang phòng khác giá** (đề xuất Q-44, FR-030): cần ghi vào 6.6 và 7.4 rằng hệ thống tự tạo yêu cầu thay đổi lưu trú Chờ duyệt, và cột "Dùng tại" của CFG-M03-02 thêm FR-030; thêm Q-44 vào mục 24.
10. **Một khu nghỉ bán trú duy nhất** (đề xuất Q-45, FR-038): cần ghi rõ ở 7.1 ("một khu nghỉ ban ngày cho toàn viện") và thêm Q-45 vào mục 24; CFG-M03-01 giữ nguyên là một giá trị.
11. **Trở về phòng đang cách ly** (đề xuất Q-46, FR-024a): cần ghi ngoại lệ này vào 7.2 hoặc BR-M03-01 (Ghi nhận trở về không phải phân bổ mới nên không bị chặn, nhưng cần bác sĩ xác nhận), thêm Q-46 vào mục 24.
12. **Buổi có khung giờ, lịch đến chiếm mọi buổi giao nhau** (đề xuất Q-47, FR-038, FR-039): cần ghi vào BR-M03-03 và 3.3, thêm Q-47 vào mục 24.
13. **Thời điểm ghi khi Bộ lập lịch chạy trễ** (đề xuất Q-48, FR-016a): tham số mới CFG-M03-08 (ngưỡng báo thực hiện trễ, mặc định 30 phút) cần bổ sung vào Phụ lục 25; thêm Q-48 vào mục 24.
14. **Mức quan trọng cố định của công việc vệ sinh** (đề xuất Q-49, FR-044b): cần ghi vào 7.5 và bảng mức quan trọng ở 8.3; thêm Q-49 vào mục 24. Feature 005 cần ghi rằng loại công việc thuộc nhóm "vệ sinh phòng" có mức quan trọng do feature 003 quy định, không cấu hình ở FR-038 của 005.
15. **Hoàn tất tiếp nhận bị chặn khi giường đặt trước chưa Trống** (đề xuất Q-50, FR-023): cần thêm điều kiện "giường đặt trước sẵn sàng (Trống)" vào dòng "Đang tiếp nhận → Đang lưu trú" của 5.6 và bảng trạng thái của feature 001, thêm Q-50 vào mục 24.
16. **Giường hỏng có phân bổ tương lai** (đề xuất Q-51, FR-013a): cần ghi ngoại lệ vào BR-M03-05 hoặc BR-M03-13, thêm Q-51 vào mục 24.
17. **Người yêu cầu của yêu cầu thay đổi lưu trú tự tạo khi chuyển gấp** (đề xuất Q-52, FR-030): là "Hệ thống", giao hành chính theo dõi; cần ghi vào 6.6 và thêm Q-52 vào mục 24. Không cần sửa Permission Matrix.
18. **Nâng vệ sinh trả giường lên khử khuẩn khi danh sách nghi nhiễm cập nhật muộn** (đề xuất Q-53, FR-045): cần ghi vào BR-M03-10, thêm Q-53 vào mục 24.

**(2026-09-27)** Các quyết định Q-40 → Q-53 của spec này đã được phản ánh vào `docs/nghiep-vu.md` (3.3, 7.2 → 7.5; CFG-M03-07, CFG-M03-08 ở Phụ lục 25) và nằm ở mục 24.2. Q-40 được chuyển khỏi 24.1 vì đã chốt. Các điểm "Còn mở" 4, 5, 7 của mục này được giải quyết.

**(2026-09-29)** 19. **Rà chéo với spec 019 (tài sản giường)** – người dùng chốt: Đang bảo trì, Không sử dụng chỉ qua lệnh tài sản của feature 019; spec này bỏ lệnh Đặt bảo trì, Ngừng sử dụng, Sẵn sàng; Không sử dụng là trạng thái cuối (Q-234). Giường mang dấu "chờ chuyển người" không nhận phân bổ mới, không kích hoạt BR-M02-01; phân bổ tương lai đã có được giữ và chưa bắt đầu khi dấu còn (Q-235). *(Đã xử lý 2026-09-29: 7.2, BR-M03-01, 06, 13, 15, BF-05; FR-013, FR-013a, FR-013b, FR-014, FR-018 (i), FR-048, bảng trạng thái giường, User Story 4 #1 → #3, #10, #12, User Story 8 #9.)*
