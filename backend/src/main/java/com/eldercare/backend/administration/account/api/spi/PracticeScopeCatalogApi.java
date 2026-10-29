package com.eldercare.backend.administration.account.api.spi;

import java.util.Optional;

/**
 * SPI: danh mục phạm vi hành nghề của 008, để gắn vào nghiệp vụ chuyên môn (FR-037, BR-M15-19). Khai báo ở 002; M09
 * (008) cài (008 contracts mục 1.1).
 */
public interface PracticeScopeCatalogApi {

    Optional<PracticeScopeView> find(long practiceScopeId);
}
