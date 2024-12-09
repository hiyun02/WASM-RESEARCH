package kopo.poly.service.impl;

import kopo.poly.domain.Documents;
import kopo.poly.dto.DocumentsDTO;
import kopo.poly.repository.DocumentRepository;
import kopo.poly.service.IEmbeddingClient;
import kopo.poly.service.IEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 임베딩 생성 및 유사도 기반 문서 검색을 처리하는 서비스 클래스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService implements IEmbeddingService {

    // 유사도 임계값 설정 (0.8 이상만 유사한 것으로 판단)
    // 저장된 데이터가 많이 없기에 0.5로 하자!
    private static final double SIMILARITY_THRESHOLD = 0.5;
    // 반환할 최대 문서 수 (상위 10개)
    private static final int TOP_K = 10;

    // 의존성 주입: 임베딩 API 호출을 위한 클라이언트
    private final IEmbeddingClient embeddingClient;
    // 문서 저장소 (Repository)
    private final DocumentRepository documentRepository;

    /**
     * 주어진 질문을 임베딩하고 유사한 문서를 검색한 후 DocumentsDTO 리스트로 반환합니다.
     *
     * @param pDTO 질문을 담은 DocumentsDTO 객체
     * @return 유사한 문서의 DTO 리스트
     */
    @Override
    public List<DocumentsDTO> findSimilarDocuments(DocumentsDTO pDTO) {
        log.info("findSimilarDocuments Start!");

        // Step 1: 질문을 임베딩 벡터로 변환
        double[] questionEmbedding = embedQuestion(pDTO.question());

        // Step 2: 임베딩된 질문과 유사한 문서를 검색
        List<Documents> relevantDocuments = getRelevantDocuments(questionEmbedding);

        // Step 3: 검색된 Documents를 DocumentsDTO로 변환
        List<DocumentsDTO> documentDTOList = relevantDocuments.stream()
                .map(doc -> DocumentsDTO.builder()
                        .id(doc.getId())
                        .content(doc.getContent())
                        .build())
                .collect(Collectors.toList());

        log.info("Document DTO list: {}", documentDTOList);
        log.info("findSimilarDocuments End!");

        return documentDTOList;
    }

    /**
     * 텍스트를 임베딩 벡터로 변환하는 메서드.
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터 (double 배열)
     */
    private double[] embedQuestion(String text) {
        return generateEmbedding(text).stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
    }

    /**
     * 모든 문서에서 주어진 임베딩과 유사도가 높은 문서를 검색합니다.
     *
     * @param questionEmbedding 질문의 임베딩 벡터
     * @return 유사한 문서 리스트
     */
    private List<Documents> getRelevantDocuments(double[] questionEmbedding) {
        return documentRepository.findAll().stream()
                .map(document -> {
                    // 문서의 임베딩과 질문 임베딩 간의 코사인 유사도 계산
                    double similarity = calculateCosineSimilarity(
                            questionEmbedding,
                            document.getEmbedding().stream().mapToDouble(Double::doubleValue).toArray()
                    );
                    // 유사도 점수를 문서 객체에 설정
                    document.setSimilarityScore(similarity);
                    return document;
                })
                // 유사도가 임계값 이상인 문서만 필터링
                .filter(doc -> doc.getSimilarityScore() >= SIMILARITY_THRESHOLD)
                // 유사도가 높은 순으로 정렬
                .sorted((d1, d2) -> Double.compare(d2.getSimilarityScore(), d1.getSimilarityScore()))
                // 상위 10개 문서만 선택
                .limit(TOP_K)
                .collect(Collectors.toList());
    }

    /**
     * 두 벡터 간의 코사인 유사도를 계산합니다.
     *
     * @param vectorA 첫 번째 벡터
     * @param vectorB 두 번째 벡터
     * @return 코사인 유사도 값
     */
    private double calculateCosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must be of the same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        // 각 요소별로 내적 및 벡터 크기 계산
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        // 코사인 유사도 계산
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 텍스트를 임베딩 벡터로 생성합니다.
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터 (List<Double>)
     */
    @Override
    public List<Double> generateEmbedding(String text) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        // 임베딩 API를 호출하여 임베딩 벡터 생성
        Map<String, Object> response = embeddingClient.generateEmbedding(requestBody);

        // 예외 처리: 임베딩이 반환되지 않은 경우
        if (!response.containsKey("embedding")) {
            throw new IllegalStateException("Embedding not found in the response");
        }

        return (List<Double>) response.get("embedding");
    }

    /**
     * 새 문서를 저장하고 임베딩 벡터를 생성하여 벡터 DB에 저장합니다.
     *
     * @param content 저장할 문서의 내용
     */
    @Override
    public void saveDocument(String content) {
        log.info("saveDocument Start!");

        // 문서 내용을 임베딩 벡터로 변환
        List<Double> embedding = generateEmbedding(content);

        // Documents 객체 생성 및 데이터 설정
        Documents documentEntry = Documents.builder()
                .content(content)
                .embedding(embedding)
                .build();

        // 문서 저장소에 저장
        documentRepository.save(documentEntry);

        log.info("Document saved successfully!");
        log.info("saveDocument End!");
    }
}
