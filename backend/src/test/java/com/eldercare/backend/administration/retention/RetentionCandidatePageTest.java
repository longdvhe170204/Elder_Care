package com.eldercare.backend.administration.retention;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 000 T112: trang ứng viên xếp theo {@code (at, id)}, đứng sau mốc của trang trước, tối đa {@code batch}. */
class RetentionCandidatePageTest {

    private static final Instant T1 = Instant.parse("2015-01-01T00:00:00Z");
    private static final Instant T2 = Instant.parse("2015-02-01T00:00:00Z");

    private static Stream<Candidate> all() {
        return Stream.of(new Candidate(9, T2), new Candidate(5, T1), new Candidate(3, T2), new Candidate(7, T1));
    }

    @Test
    void firstPage_isOrderedByTimeThenId() {
        assertThat(RetentionProvider.page(all(), null, 3))
                .containsExactly(new Candidate(5, T1), new Candidate(7, T1), new Candidate(3, T2));
    }

    @Test
    void nextPage_startsAfterTheLastCandidate_evenWithTheSameTime() {
        assertThat(RetentionProvider.page(all(), new Candidate(7, T1), 3))
                .containsExactly(new Candidate(3, T2), new Candidate(9, T2));
        assertThat(RetentionProvider.page(all(), new Candidate(9, T2), 3)).isEmpty();
    }
}
