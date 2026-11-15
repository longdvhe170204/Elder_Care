package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.billing.internal.export.service.CostAccountingExport;
import com.eldercare.backend.billing.internal.provision.service.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;

/** T029/T036: dùng lại fixture 010, MySQL và lệnh thật. */
class CostProvisionIT extends CostWorkflowIT {
    @Autowired
    CostSummaries summaries;

    @Autowired
    CostForBalance balance;

    @Autowired
    CostAccountingExport exports;
}
