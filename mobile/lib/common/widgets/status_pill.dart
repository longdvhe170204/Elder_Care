import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Tông của nhãn trạng thái (DESIGN.md 3.5).
enum StatusTone { settled, attention, neutral, urgent }

/// Nhãn trạng thái `status-pill` (DESIGN.md 7.2, 15.3): bo tròn, chữ 12/500, luôn có chữ.
class StatusPill extends StatelessWidget {
  const StatusPill({super.key, required this.label, required this.tone});

  final String label;
  final StatusTone tone;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final tones = Theme.of(context).extension<Tones>()!;
    final (background, ink) = switch (tone) {
      StatusTone.settled => (tones.settledWash, tones.settledInk),
      StatusTone.attention => (tones.attentionWash, tones.attentionInk),
      StatusTone.neutral => (tones.neutralWash, scheme.onSurfaceVariant),
      StatusTone.urgent => (scheme.errorContainer, scheme.onErrorContainer),
    };
    return DecoratedBox(
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(999)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
        child: Text(label, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: ink)),
      ),
    );
  }
}
