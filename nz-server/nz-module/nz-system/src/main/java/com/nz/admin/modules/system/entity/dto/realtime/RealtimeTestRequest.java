package com.nz.admin.modules.system.entity.dto.realtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RealtimeTestRequest {

    @NotBlank
    @Size(max = 500)
    private String message;
}
