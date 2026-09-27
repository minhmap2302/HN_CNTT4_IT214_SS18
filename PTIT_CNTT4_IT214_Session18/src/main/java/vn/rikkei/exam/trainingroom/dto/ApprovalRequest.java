package vn.rikkei.exam.trainingroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {
    @NotBlank(message = "Id không được để trống")
    private String requestId;
    private String decision;
    private String note;
}
