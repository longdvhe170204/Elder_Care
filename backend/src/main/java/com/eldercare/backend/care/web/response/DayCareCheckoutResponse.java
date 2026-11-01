package com.eldercare.backend.care.web.response;

import java.util.List;

public record DayCareCheckoutResponse(DayCareAttendanceResponse attendance, List<Long> cancelledTaskIds) {}
