package com.eldercare.backend.administration.parameter.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** FR-011 (000 T030): enum ParameterCode có đúng các mã của Phụ lục 25 trong tài liệu nghiệp vụ. */
class ParameterCodeTest {

    private static final Path SOURCE = Path.of("..", "docs", "business", "nghiep-vu.md");

    @Test
    void FR_011_enum_matchesAppendix25() throws IOException {
        Pattern row = Pattern.compile("^\\| (CFG-M\\d{2}-\\d{2}) ", Pattern.MULTILINE);
        Matcher m = row.matcher(Files.readString(SOURCE, StandardCharsets.UTF_8));
        Set<String> source = m.results().map(r -> r.group(1)).collect(Collectors.toSet());

        Set<String> codes =
                Arrays.stream(ParameterCode.values()).map(ParameterCode::code).collect(Collectors.toSet());

        assertThat(codes).hasSize(122).isEqualTo(source);
    }

    @Test
    void code_roundTrips() {
        assertThat(ParameterCode.CFG_M15_05.code()).isEqualTo("CFG-M15-05");
        assertThat(ParameterCode.of("CFG-M15-05")).isEqualTo(ParameterCode.CFG_M15_05);
    }
}
