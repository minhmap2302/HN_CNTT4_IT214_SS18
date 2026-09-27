package vn.rikkei.exam.trainingroom.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import vn.rikkei.exam.trainingroom.dto.ChatResponse;
import vn.rikkei.exam.trainingroom.tool.ReservationTools;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatOrchestrationService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ReservationTools reservationTools;

    public static final String FALLBACK_MESSAGE =
            "Xin lỗi, tôi không có đủ căn cứ trong tài liệu nội bộ để trả lời câu hỏi của bạn.";

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý AI cho hệ thống đặt phòng training.
        Dùng CONTEXT bên dưới để trả lời câu hỏi về chính sách.
        Dùng các tool được cung cấp để tra phòng trống hoặc tạo yêu cầu đặt phòng -
        không tự suy đoán tình trạng phòng, không tự ghi dữ liệu.
        Nếu thiếu thông tin bắt buộc để gọi tool (ví dụ: chưa có ngày startDate/endDate,
        số khách, hoặc mã loại phòng), hãy hỏi người dùng để bổ sung trước khi gọi tool,
        không tự suy đoán hoặc bịa giá trị.
        Tuyệt đối không bịa thông tin ngoài CONTEXT và kết quả tool.
        
        Nếu câu hỏi về chính sách mà CONTEXT không đủ căn cứ, trả lời đúng nguyên văn:
        "%s"

        CONTEXT:
        %s
        """;

    public ChatResponse chat(String userMessage, String conversationIdInput) {
        String conversationId = (conversationIdInput == null || conversationIdInput.isBlank())
                ? UUID.randomUUID().toString()
                : conversationIdInput;

        SearchRequest searchRequest = SearchRequest.builder()
                .query(userMessage)
                .topK(4)
                .similarityThreshold(0.5)
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        boolean hasContext = !documents.isEmpty();

        String context = hasContext
                ? documents.stream().map(Document::getText).collect(Collectors.joining("\n\n---\n\n"))
                : "(không có tài liệu liên quan)";

        List<String> sources = hasContext
                ? documents.stream()
                .map(d -> d.getMetadata().get("section") + " (" + d.getMetadata().get("source") + ")")
                .distinct()
                .toList()
                : List.of();

        reservationTools.resetUsedTools();

        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT.formatted(FALLBACK_MESSAGE, context))
                .user(userMessage)
                .options(ChatOptions.builder().temperature(.3))
                .tools(reservationTools)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            answer = FALLBACK_MESSAGE;
        }

        return ChatResponse.builder()
                .answer(answer)
                .conversationId(conversationId)
                .sources(sources)
                .toolsUsed(reservationTools.getUsedTools())
                .build();
    }
}