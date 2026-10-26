package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.m15.account.api.spi.PracticeScopeCatalogApi;
import com.cambae.backend.m15.account.api.spi.PracticeScopeView;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T007: tra danh mục M09 qua SPI của 002. */
@Service
@Transactional(readOnly = true)
class PracticeScopeLookup implements PracticeScopeCatalogApi {
    private final JdbcTemplate jdbc;

    PracticeScopeLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PracticeScopeView> find(long id) {
        return jdbc
                .query(
                        "SELECT id,name,status FROM practice_scope WHERE id=?",
                        (r, i) -> new PracticeScopeView(r.getLong(1), r.getString(2), "ACTIVE".equals(r.getString(3))),
                        id)
                .stream()
                .findFirst();
    }
}
