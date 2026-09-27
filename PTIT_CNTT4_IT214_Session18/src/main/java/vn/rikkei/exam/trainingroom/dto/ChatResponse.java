package vn.rikkei.exam.trainingroom.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String answer;
    private String conversationId;
    private List<String> sources;
    private List<String> toolsUsed;
}
