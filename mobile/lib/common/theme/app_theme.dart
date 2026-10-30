import 'package:flutter/material.dart';

/// Theme của app theo token trong frontmatter DESIGN.md (mục 15.2): màu sáng, tối (3.1, 3.2); thang chữ mục 4 làm
/// logical px; bo góc mục 6. Widget chỉ dùng theme, không viết màu, cỡ chữ, bo góc thô.
abstract final class AppTheme {
  static const fontFamily = 'BeVietnamPro';

  static ThemeData light() => _theme(_lightScheme, Tones.light);

  static ThemeData dark() => _theme(_darkScheme, Tones.dark);

  /// Phần người cao tuổi (DESIGN.md 15.6): thang chữ nhân 1,25, vùng bấm tối thiểu 56, nút chính rộng hết màn cao 56.
  static ThemeData resident(Brightness brightness) {
    final base = brightness == Brightness.dark ? dark() : light();
    final text = _textTheme(1.25, base.colorScheme);
    return base.copyWith(
      textTheme: text,
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(56),
          textStyle: text.titleMedium,
          shape: const StadiumBorder(),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(56),
          textStyle: text.titleMedium,
          foregroundColor: base.colorScheme.onSurface,
          side: BorderSide(color: base.colorScheme.outline, width: 1.5),
          shape: const StadiumBorder(),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(minimumSize: const Size(56, 56), textStyle: text.titleMedium),
      ),
      inputDecorationTheme: base.inputDecorationTheme.copyWith(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
      ),
    );
  }

  /// Mục 3.1: `primary` xanh đêm, `secondary` xanh trời đậm (chữ nhấn, `tonal`), `error` đỏ chỉ việc gấp, `surface`
  /// bề mặt, `surfaceContainerHighest` nền phụ, `outline` viền điều khiển, `outlineVariant` đường kẻ.
  static const _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: Color(0xFF24496F),
    onPrimary: Color(0xFFFFFFFF),
    secondary: Color(0xFF0B5F8A),
    onSecondary: Color(0xFFFFFFFF),
    secondaryContainer: Color(0xFFC0EAFF),
    onSecondaryContainer: Color(0xFF0B5F8A),
    error: Color(0xFFB42318),
    onError: Color(0xFFFFFFFF),
    errorContainer: Color(0xFFFFECEC),
    onErrorContainer: Color(0xFFB42318),
    surface: Color(0xFFFFFFFF),
    onSurface: Color(0xFF0F2235),
    onSurfaceVariant: Color(0xFF50627A),
    surfaceContainerHighest: Color(0xFFF5F7FA),
    outline: Color(0xFF7E8591),
    outlineVariant: Color(0xFFDDE3EA),
  );

  static const _darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: Color(0xFFE6EDF6),
    onPrimary: Color(0xFF0F1A2A),
    secondary: Color(0xFF8DB4E2),
    onSecondary: Color(0xFF0F1A2A),
    secondaryContainer: Color(0xFF22344F),
    onSecondaryContainer: Color(0xFF8DB4E2),
    error: Color(0xFFFF7A7E),
    onError: Color(0xFF0F1A2A),
    errorContainer: Color(0xFF3A1A1C),
    onErrorContainer: Color(0xFFFF7A7E),
    surface: Color(0xFF18243A),
    onSurface: Color(0xFFE6EDF6),
    onSurfaceVariant: Color(0xFF9AABC2),
    surfaceContainerHighest: Color(0xFF1E2B44),
    outline: Color(0xFF7E8591),
    outlineVariant: Color(0xFF2A3A52),
  );

  static ThemeData _theme(ColorScheme scheme, Tones tones) => ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    fontFamily: fontFamily,
    textTheme: _textTheme(1, scheme),
    scaffoldBackgroundColor: tones.canvas,
    extensions: [tones],
    // Mục 6: bề mặt trắng viền 1px đường kẻ, không đổ bóng, bo 12
    cardTheme: CardThemeData(
      color: scheme.surface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.lg),
        side: BorderSide(color: scheme.outlineVariant),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: scheme.surface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppRadius.md),
        borderSide: BorderSide(color: scheme.outline),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppRadius.md),
        borderSide: BorderSide(color: scheme.outline),
      ),
    ),
  );

  /// Thang chữ mục 4: caption 12/16/500, body-sm 14/20, label 14/20/500, body 15/22, card-title 16/22/600,
  /// title 18/24/600, metric 24/28/600, page 26/32/600, clock 32/36/600; chữ số đều bề rộng.
  static TextTheme _textTheme(double factor, ColorScheme scheme) {
    TextStyle style(double size, double line, FontWeight weight, {double tracking = 0}) => TextStyle(
      fontFamily: fontFamily,
      fontSize: (size * factor).roundToDouble(),
      height: line / size,
      fontWeight: weight,
      letterSpacing: tracking * size * factor,
      color: scheme.onSurface,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
    return TextTheme(
      displaySmall: style(32, 36, FontWeight.w600),
      headlineMedium: style(26, 32, FontWeight.w600, tracking: -0.01),
      headlineSmall: style(24, 28, FontWeight.w600, tracking: -0.01),
      titleLarge: style(18, 24, FontWeight.w600),
      titleMedium: style(16, 22, FontWeight.w600),
      titleSmall: style(14, 20, FontWeight.w500),
      bodyLarge: style(15, 22, FontWeight.w400),
      bodyMedium: style(15, 22, FontWeight.w400),
      bodySmall: style(14, 20, FontWeight.w400),
      labelLarge: style(14, 20, FontWeight.w500),
      labelMedium: style(12, 16, FontWeight.w500),
      labelSmall: style(12, 16, FontWeight.w500),
    );
  }
}

/// Bo góc mục 6 (frontmatter `rounded`); nút, chip, nhãn nguồn, ảnh đại diện bo tròn hẳn (`StadiumBorder`).
abstract final class AppRadius {
  /// Điểm nhấn nhỏ.
  static const double sm = 6;

  /// Mục menu.
  static const double menu = 8;

  /// Ô giường, ô nhập, khối việc, khối dị ứng.
  static const double md = 10;

  /// Bề mặt: bảng, khối, dải cần làm ngay, hộp thoại.
  static const double lg = 12;
}

/// Màu không có chỗ trong `ColorScheme` (DESIGN.md 15.2): nền trang, đầu màn theo màu menu (3.2, 15.4), chữ tông trạng
/// thái và nền khối dị ứng (3.5), xanh thương hiệu chỉ cho đồ họa, ảnh đại diện (3.6), ô giường.
@immutable
class Tones extends ThemeExtension<Tones> {
  const Tones({
    required this.canvas,
    required this.header,
    required this.headerInk,
    required this.headerInkStrong,
    required this.headerInkMuted,
    required this.brandGraphic,
    required this.settledInk,
    required this.attentionWash,
    required this.attentionInk,
    required this.occupiedWash,
    required this.occupiedInk,
    required this.avatars,
    required this.bedOccupied,
    required this.bedHeld,
    required this.bedPending,
    required this.bedMaintenance,
  });

  static const light = Tones(
    canvas: Color(0xFFEEF2F6),
    header: Color(0xFF142B45),
    headerInk: Color(0xFFC9D6E5),
    headerInkStrong: Color(0xFFFFFFFF),
    headerInkMuted: Color(0xFF9FB3CA),
    brandGraphic: Color(0xFF2597D0),
    settledInk: Color(0xFF1F6B45),
    attentionWash: Color(0xFFFFECC0),
    attentionInk: Color(0xFF8A5300),
    occupiedWash: Color(0xFFC0D5FF),
    occupiedInk: Color(0xFF2A4A8F),
    avatars: [
      (Color(0xFFC0EAFF), Color(0xFF0B5F8A)),
      (Color(0xFFC0D5FF), Color(0xFF2A4A8F)),
      (Color(0xFFE2F4EA), Color(0xFF1F6B45)),
      (Color(0xFFEFEFF1), Color(0xFF4A4A55)),
    ],
    bedOccupied: Color(0xFF4A6FD6),
    bedHeld: Color(0xFF1A8CC4),
    bedPending: Color(0xFFC77A12),
    bedMaintenance: Color(0xFF7A8699),
  );

  static const dark = Tones(
    canvas: Color(0xFF0F1A2A),
    header: Color(0xFF0B1320),
    headerInk: Color(0xFFB8C6DA),
    headerInkStrong: Color(0xFFFFFFFF),
    headerInkMuted: Color(0xFFB8C6DA),
    brandGraphic: Color(0xFF6F9FD6),
    settledInk: Color(0xFF7FD3A3),
    attentionWash: Color(0xFF3A2C10),
    attentionInk: Color(0xFFFFCB6B),
    occupiedWash: Color(0xFF16273D),
    occupiedInk: Color(0xFF9CC3FF),
    avatars: [
      (Color(0xFF16273D), Color(0xFF9CC3FF)),
      (Color(0xFF2A2340), Color(0xFFC9B8FF)),
      (Color(0xFF173A2A), Color(0xFF7FD3A3)),
      (Color(0xFF2E2A20), Color(0xFFE2D3AE)),
    ],
    bedOccupied: Color(0xFF7B9BF0),
    bedHeld: Color(0xFF4FB6E6),
    bedPending: Color(0xFFF2A646),
    bedMaintenance: Color(0xFF8A97AD),
  );

  /// Nền trang, màu trơn.
  final Color canvas;

  /// Khối đầu màn xanh đêm đậm và chữ trên đó (15.4, theo menu 3.2).
  final Color header;
  final Color headerInk;
  final Color headerInkStrong;
  final Color headerInkMuted;

  /// Chỉ đồ họa (thanh tiến độ ca), không làm chữ hay nền nút chữ trắng (mục 1 điều 5).
  final Color brandGraphic;

  final Color settledInk;

  /// Nền khối dị ứng (`AllergyNote`), chỗ duy nhất tông trạng thái có nền.
  final Color attentionWash;
  final Color attentionInk;
  final Color occupiedWash;
  final Color occupiedInk;

  /// Bốn cặp (nền, chữ) của ảnh đại diện, chọn theo id người cao tuổi.
  final List<(Color, Color)> avatars;

  /// Chỉ ô giường trong sơ đồ (`bed-cell`).
  final Color bedOccupied;
  final Color bedHeld;
  final Color bedPending;
  final Color bedMaintenance;

  @override
  Tones copyWith() => this;

  @override
  Tones lerp(Tones? other, double t) => t < 0.5 || other == null ? this : other;
}
