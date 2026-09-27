package vn.rikkei.exam.trainingroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.exam.trainingroom.dto.*;
import vn.rikkei.exam.trainingroom.service.ReservationService;
import vn.rikkei.exam.trainingroom.service.chat.ChatOrchestrationService;
import vn.rikkei.exam.trainingroom.service.rag.RagIngestionService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReservationAssistantController {

    private final RagIngestionService ragIngestionService;
    private final ChatOrchestrationService chatOrchestrationService;
    private final ReservationService reservationService;

    @PostMapping("/api/admin/ingest")
    public ResponseEntity<ApiResponse<String>> ingest() throws IOException {
        String result = ragIngestionService.ingestDocument();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/api/assistant/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody @Valid ChatRequest request) {
        ChatResponse response = chatOrchestrationService.chat(request.getMessage(), request.getConversationId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/operations/approve-reservation")
    public ResponseEntity<ApiResponse<ApprovalResponse>> approveBooking(@RequestBody @Valid ApprovalRequest request) {
        ApprovalResponse response = reservationService.decide(request);
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý yêu cầu đặt phòng", response));
    }
}
