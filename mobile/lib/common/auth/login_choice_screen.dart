import 'package:flutter/material.dart';

import '../widgets/sky_background.dart';

/// Màn đầu khi chưa đăng nhập (000 T006; 002 T046, T047; bố cục duyệt 2026-10-27): chọn "Tôi là" nhân viên, người thân
/// hoặc người cao tuổi rồi mở màn đăng nhập của phần đó. Máy đã gắn với tài khoản người cao tuổi không qua màn này
/// (FR-014a). Màn đăng nhập từng phần do `ElderCareApp` truyền vào để `common/` không phụ thuộc `staff/`, `family/`,
/// `resident/` (mobile/README.md luật 1).
class LoginChoiceScreen extends StatelessWidget {
  const LoginChoiceScreen({
    super.key,
    required this.staffLogin,
    required this.familyLogin,
    required this.residentLogin,
    this.notice,
  });

  final WidgetBuilder staffLogin;
  final WidgetBuilder familyLogin;
  final WidgetBuilder residentLogin;

  /// Câu báo, ví dụ sau khi phiên bị kết thúc.
  final String? notice;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    void open(WidgetBuilder builder) => Navigator.of(context).push(MaterialPageRoute<void>(builder: builder));
    return Scaffold(
      body: SkyBackground(
        child: Align(
          alignment: Alignment.topCenter,
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text('Elder Care', style: text.headlineMedium),
                  const SizedBox(height: 8),
                  Text('Tôi là', style: text.bodyMedium),
                  if (notice != null) ...[const SizedBox(height: 12), Text(notice!, style: text.bodyMedium)],
                  const SizedBox(height: 16),
                  FilledButton(onPressed: () => open(staffLogin), child: const Text('Nhân viên')),
                  const SizedBox(height: 12),
                  OutlinedButton(onPressed: () => open(familyLogin), child: const Text('Người thân')),
                  const SizedBox(height: 12),
                  OutlinedButton(onPressed: () => open(residentLogin), child: const Text('Người cao tuổi: quét mã QR')),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
