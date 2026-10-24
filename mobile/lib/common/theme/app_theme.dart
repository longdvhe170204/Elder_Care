import 'package:flutter/material.dart';

/// Theme của app theo token trong frontmatter DESIGN.md (mục 15.2): màu sáng, tối; thang chữ mục 4 làm logical px;
/// bo góc. Widget chỉ dùng theme, không viết màu, cỡ chữ thô.
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

  static const _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: Color(0xFF24496F),
    onPrimary: Color(0xFFFFFFFF),
    secondary: Color(0xFF0B5F8A),
    onSecondary: Color(0xFFFFFFFF),
    secondaryContainer: Color(0xFFC0EAFF),
    onSecondaryContainer: Color(0xFF0B5F8A),
    error: Color(0xFFC9302C),
    onError: Color(0xFFFFFFFF),
    errorContainer: Color(0xFFFFECEC),
    onErrorContainer: Color(0xFFC9302C),
    surface: Color(0xFFFFFFFF),
    onSurface: Color(0xFF0F2235),
    onSurfaceVariant: Color(0xFF50627A),
    surfaceContainerHighest: Color(0xFFF5F7FA),
    outline: Color(0xFF868C98),
    outlineVariant: Color(0xFFEFEFF1),
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
    outline: Color(0xFF6E80A0),
    outlineVariant: Color(0xFF2A3A52),
  );

  static ThemeData _theme(ColorScheme scheme, Tones tones) => ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    fontFamily: fontFamily,
    textTheme: _textTheme(1, scheme),
    scaffoldBackgroundColor: tones.canvasGradient.first,
    extensions: [tones],
    cardTheme: CardThemeData(
      color: scheme.surface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: scheme.surface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: scheme.outline),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: scheme.outline),
      ),
    ),
  );

  /// Thang chữ mục 4: body 15/22, label 14/20, card-title 16/22, title 18/24, page 26/32; chữ số đều bề rộng.
  static TextTheme _textTheme(double factor, ColorScheme scheme) {
    TextStyle style(double size, double line, FontWeight weight) => TextStyle(
      fontFamily: fontFamily,
      fontSize: (size * factor).roundToDouble(),
      height: line / size,
      fontWeight: weight,
      color: scheme.onSurface,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
    return TextTheme(
      headlineMedium: style(26, 32, FontWeight.w600),
      titleLarge: style(18, 24, FontWeight.w600),
      titleMedium: style(16, 22, FontWeight.w600),
      bodyLarge: style(15, 22, FontWeight.w400),
      bodyMedium: style(15, 22, FontWeight.w400),
      bodySmall: style(14, 20, FontWeight.w400),
      labelLarge: style(14, 20, FontWeight.w500),
      labelSmall: style(12, 16, FontWeight.w500),
    );
  }
}

/// Màu không có chỗ trong `ColorScheme` (DESIGN.md 15.2): nền trời, tông trạng thái mục 3.5.
@immutable
class Tones extends ThemeExtension<Tones> {
  const Tones({
    required this.canvasGradient,
    required this.settledWash,
    required this.settledInk,
    required this.attentionWash,
    required this.attentionInk,
    required this.neutralWash,
  });

  static const light = Tones(
    canvasGradient: [Color(0xFFB4C8DD), Color(0xFFC7DCEB), Color(0xFFE2EDF5)],
    settledWash: Color(0xFFE2F4EA),
    settledInk: Color(0xFF1F6B45),
    attentionWash: Color(0xFFFFECC0),
    attentionInk: Color(0xFF8A5300),
    neutralWash: Color(0xFFF5F7FA),
  );

  static const dark = Tones(
    canvasGradient: [Color(0xFF0F1A2A), Color(0xFF15233A), Color(0xFF1B2B42)],
    settledWash: Color(0xFF173A2A),
    settledInk: Color(0xFF7FD3A3),
    attentionWash: Color(0xFF3A2C10),
    attentionInk: Color(0xFFFFCB6B),
    neutralWash: Color(0xFF1E2B44),
  );

  final List<Color> canvasGradient;
  final Color settledWash;
  final Color settledInk;
  final Color attentionWash;
  final Color attentionInk;
  final Color neutralWash;

  @override
  Tones copyWith() => this;

  @override
  Tones lerp(Tones? other, double t) => t < 0.5 || other == null ? this : other;
}
