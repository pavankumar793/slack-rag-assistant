package com.pbot.backend.rag;

import com.pbot.backend.api.IngestResponse;
import com.pbot.backend.config.RagProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common.Filter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class DocumentIngestionService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final RagProperties properties;
    private final VectorStore vectorStore;
    private final QdrantVectorStoreProperties qdrantProperties;
    private final TokenTextSplitter textSplitter = TokenTextSplitter.builder().build();

    public DocumentIngestionService(RagProperties properties, VectorStore vectorStore,
            QdrantVectorStoreProperties qdrantProperties) {
        this.properties = properties;
        this.vectorStore = vectorStore;
        this.qdrantProperties = qdrantProperties;
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
        resetVectorCollection();
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

    private void resetVectorCollection() {
        Optional<QdrantClient> client = vectorStore.getNativeClient();
        if (client.isEmpty()) {
            LOGGER.warn("Unable to reset Qdrant collection because native QdrantClient is unavailable.");
            return;
        }

        String collectionName = qdrantProperties.getCollectionName();
        try {
            client.get().deleteAsync(collectionName, Filter.newBuilder().build()).get();
            LOGGER.info("Deleted existing points from Qdrant collection {} before ingestion.", collectionName);
        }
        catch (Exception ex) {
            LOGGER.info("Qdrant collection {} was not cleared before ingestion: {}", collectionName, ex.getMessage());
        }
    }
}
