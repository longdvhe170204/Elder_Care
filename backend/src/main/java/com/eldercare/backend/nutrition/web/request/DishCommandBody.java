package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh trên món: lý do (trả lại, ngừng hiệu lực), nội dung (gửi bản sửa). */
@Schema(name = "DishCommandBody")
public record DishCommandBody(
        @Schema(implementation = String.class, minLength = 1, maxLength = 2000)
        Reason reason,

        DishRequest content) {}
