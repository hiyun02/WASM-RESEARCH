package kopo.poly.service;

import kopo.poly.dto.DocumentsDTO;

import java.util.List;

/**
 * 임베딩 생성 및 유사 문서 검색을 위한 서비스 인터페이스.
 * 임베딩을 생성하고, 문서를 저장하며, 유사한 문서를 검색하는 기능을 제공합니다.
 */
public interface IEmbeddingService {

    /**
     * 주어진 텍스트를 임베딩 벡터로 변환합니다.
     *
     * @param text 임베딩할 텍스트 데이터
     * @return 텍스트를 변환한 임베딩 벡터 (List<Double>)
     */
    List<Double> generateEmbedding(String text);

    /**
     * 콘텐츠를 임베딩 벡터로 변환하고 MongoDB에 저장합니다.
     *
     * @param content 저장할 콘텐츠 (문서의 내용)
     */
    void saveDocument(String content);

    /**
     * 질문 임베딩 벡터와 유사한 문서를 검색합니다.
     *
     * @param pDTO 질문을 포함한 DocumentsDTO 객체
     * @return 유사한 문서들의 리스트 (DocumentsDTO 형태)
     */
    List<DocumentsDTO> findSimilarDocuments(DocumentsDTO pDTO);
}
