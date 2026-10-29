import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Tông của nhãn trạng thái (DESIGN.md 3.5).
enum StatusTone { urgent, attention, settled, held, occupied, neutral, neutralOutline }

/// Nhãn trạng thái `status-pill` (DESIGN.md 7.2, 15.3; 6.0.0): chữ màu 12/500, không nền, không viền; riêng `held`,
/// `neutralOutline` có viền bo tròn để tách khỏi tông cùng màu chữ. Luôn có chữ (mục 1 điều 6).
class StatusPill extends StatelessWidget {
  const StatusPill({super.key, required this.label, required this.tone});

  final String label;
  final StatusTone tone;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final tones = Theme.of(context).extension<Tones>()!;
    final ink = switch (tone) {
      StatusTone.urgent => scheme.error,
      StatusTone.attention => tones.attentionInk,
      StatusTone.settled => tones.settledInk,
      StatusTone.held => scheme.secondary,
      StatusTone.occupied => tones.occupiedInk,
      StatusTone.neutral || StatusTone.neutralOutline => scheme.onSurfaceVariant,
    };
    final text = Text(label, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: ink));
    final border = switch (tone) {
      StatusTone.held => ink,
      StatusTone.neutralOutline => scheme.outline,
      _ => null,
    };
    if (border == null) return text;
    return DecoratedBox(
      decoration: ShapeDecoration(
        shape: StadiumBorder(side: BorderSide(color: border)),
      ),
      child: Padding(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2), child: text),
    );
  }
}
