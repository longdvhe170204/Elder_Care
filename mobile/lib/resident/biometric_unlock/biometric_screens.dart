import 'package:flutter/material.dart';

import '../../common/auth/session_controller.dart';
import '../../common/widgets/sky_background.dart';

/// Hỏi một lần sau lần đăng nhập đầu (002 FR-074; DESIGN.md 15.6). Không có mã PIN.
class BiometricOfferScreen extends StatelessWidget {
  const BiometricOfferScreen({super.key, required this.session});

  final SessionController session;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SkyBackground(
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Mở bằng vân tay hoặc khuôn mặt?', style: text.headlineMedium),
                const SizedBox(height: 8),
                Text(
                  'Lần sau mở ứng dụng, chỉ cần chạm ngón tay hoặc nhìn vào máy.',
                  style: text.bodyLarge,
                ),
                const Spacer(),
                FilledButton(
                  onPressed: () => session.answerBiometricOffer(enable: true),
                  child: const Text('Có'),
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => session.answerBiometricOffer(enable: false),
                  child: const Text('Để sau'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Màn khóa khi đã bật mở bằng sinh trắc học (002 FR-074). Nút SOS của 007 sẽ hiện cả ở đây (DESIGN.md 15.6).
class LockedScreen extends StatelessWidget {
  const LockedScreen({super.key, required this.session});

  final SessionController session;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SkyBackground(
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Ứng dụng đang khóa', style: text.headlineMedium),
                const Spacer(),
                FilledButton(
                  onPressed: session.unlock,
                  child: const Text('Mở bằng vân tay hoặc khuôn mặt'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
