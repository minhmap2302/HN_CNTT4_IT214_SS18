package vn.rikkei.exam.trainingroom.dto;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalResponse {
    private String requestId;
    private String status;
    private String decisionNote;
}
