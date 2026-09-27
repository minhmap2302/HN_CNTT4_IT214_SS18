package vn.rikkei.exam.trainingroom.service.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagIngestionService {

    private final VectorStore vectorStore;

    private static final String CORPUS_PATH = "tai_lieu_noi_bo.md";
    private static final Pattern SECTION_SPLIT_PATTERN =
            Pattern.compile("(?=^##\\s)", Pattern.MULTILINE);

    public String ingestDocument() throws IOException {
        Resource resource = new ClassPathResource(CORPUS_PATH);
        String content = resource.getContentAsString(StandardCharsets.UTF_8);

        String[] rawSections = SECTION_SPLIT_PATTERN.split(content);
        List<Document> chunks = new ArrayList<>();
        int index = 0;

        for (String section : rawSections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            String firstLine = section.lines().findFirst().orElse("");
            String sectionTitle = firstLine.replaceFirst("^##\\s*", "").trim();
            if (sectionTitle.isBlank()) {
                sectionTitle = "section-" + index;
            }

            String docId = UUID.nameUUIDFromBytes(section.getBytes(StandardCharsets.UTF_8)).toString();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", CORPUS_PATH);
            metadata.put("section", sectionTitle);
            metadata.put("doc_id", docId);

            chunks.add(new Document(docId, section, metadata));
            index++;
        }

        if (chunks.isEmpty()) {
            log.warn("Nạp corpus: không tạo được đoạn nào từ {}", CORPUS_PATH);
            return "Không tìm thấy nội dung để nạp";
        }

        vectorStore.accept(chunks);
        log.info("Nạp corpus: đã nạp {} đoạn từ {}", chunks.size(), CORPUS_PATH);
        return "Đã nạp " + chunks.size() + " đoạn vào vector store";
    }
}
