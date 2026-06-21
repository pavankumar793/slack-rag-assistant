package com.pbot.backend.rag;

import com.pbot.backend.api.IngestResponse;
import com.pbot.backend.config.RagProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class DocumentIngestionService implements ApplicationRunner {

    private final RagProperties properties;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter = TokenTextSplitter.builder().build();

    public DocumentIngestionService(RagProperties properties, VectorStore vectorStore) {
        this.properties = properties;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.ingestOnStartup()) {
            ingest();
        }
    }

    public IngestResponse ingest() {
        Path docsPath = Path.of(properties.documentsPath()).toAbsolutePath().normalize();
        if (!Files.exists(docsPath)) {
            return new IngestResponse(0, 0);
        }

        List<Document> documents = readDocuments(docsPath);
        List<Document> chunks = textSplitter.apply(documents);
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
        }
        return new IngestResponse(documents.size(), chunks.size());
    }

    private List<Document> readDocuments(Path docsPath) {
        try (Stream<Path> files = Files.walk(docsPath)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedTextFile)
                    .map(this::readDocument)
                    .toList();
        }
        catch (IOException ex) {
            throw new IllegalStateException("Unable to scan documents from " + docsPath, ex);
        }
    }

    private boolean isSupportedTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".txt");
    }

    private Document readDocument(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return new Document(content, Map.of(
                    "source", path.getFileName().toString(),
                    "path", path.toAbsolutePath().toString(),
                    "documentHash", Integer.toHexString(content.hashCode())));
        }
        catch (IOException ex) {
            throw new IllegalStateException("Unable to read document " + path, ex);
        }
    }
}
