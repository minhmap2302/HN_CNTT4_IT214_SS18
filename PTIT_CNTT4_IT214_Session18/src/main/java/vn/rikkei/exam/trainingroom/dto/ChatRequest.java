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
public class ChatRequest {
    private String conversationId;
    @NotBlank(message = "Không được để trống tin nhắn")
    private String message;
}
