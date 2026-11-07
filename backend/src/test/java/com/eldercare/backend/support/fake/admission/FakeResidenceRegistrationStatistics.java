package com.eldercare.backend.support.fake.admission;

import com.eldercare.backend.admission.api.ResidenceRegistrationStatisticsApi;
import com.eldercare.backend.admission.api.dto.RegistrationSummaryView;

/** Bản giả {@link ResidenceRegistrationStatisticsApi} cho test của 016: trả số đã đặt, mặc định bằng không. */
public class FakeResidenceRegistrationStatistics implements ResidenceRegistrationStatisticsApi {

    private RegistrationSummaryView summary = new RegistrationSummaryView(0, 0, 0, 0);

    public FakeResidenceRegistrationStatistics with(RegistrationSummaryView summary) {
        this.summary = summary;
        return this;
    }

    public FakeResidenceRegistrationStatistics reset() {
        summary = new RegistrationSummaryView(0, 0, 0, 0);
        return this;
    }

    @Override
    public RegistrationSummaryView summary() {
        return summary;
    }
}
