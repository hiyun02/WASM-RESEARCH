package kopo.poly.controller;

import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.DocumentsDTO;
import kopo.poly.dto.MsgDTO;
import kopo.poly.service.IEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 벡터 임베딩 생성, 유사 문서 검색 및 저장 기능을 제공합니다.
 */
@Slf4j
@RequestMapping(value = "/vector/v1")
@RequiredArgsConstructor
@RestController
public class VectorController {

    // 의존성 주입: 임베딩 및 벡터 검색을 처리하는 서비스
    private final IEmbeddingService embeddingService;

    /**
     * 질문을 임베딩하고 유사한 문서를 벡터 DB에서 검색한 후 반환합니다.
     *
     * @param question 사용자로부터 입력받은 질문 (텍스트)
     * @return 유사한 문서의 내용 리스트를 담은 CommonResponse 객체
     */
    @PostMapping("/generate")
    public ResponseEntity<CommonResponse> generateAnswer(@RequestBody String question) {
        log.info("generate Start!");

        log.info("question : " + question);
        // DocumentsDTO 객체 생성 및 질문 설정
        DocumentsDTO pDTO = DocumentsDTO.builder().question(question).build();

        // 임베딩 서비스에서 유사한 문서 검색
        List<DocumentsDTO> rList = embeddingService.findSimilarDocuments(pDTO);

        log.info("generate End!");

        // 검색된 문서 리스트를 HTTP 200 OK 응답으로 반환
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList));
    }

    /**
     * 새로운 문서를 벡터 DB에 저장합니다.
     *
     * @param content 저장할 문서의 내용 (텍스트)
     * @return 저장 성공 메시지를 담은 CommonResponse 객체
     */
    @PostMapping("/document")
    public ResponseEntity<CommonResponse> saveDocument(@RequestBody String content) {
        log.info("saveDocument Start!");

        // 임베딩 서비스를 통해 문서 내용을 벡터 DB에 저장
        embeddingService.saveDocument(content);

        // 성공 메시지 DTO 생성
        MsgDTO msgDTO = MsgDTO.builder().result(1).msg("저장되었습니다.").build();

        log.info("saveDocument End!");

        // 저장 성공 메시지를 HTTP 200 OK 응답으로 반환
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), msgDTO));
    }

    /**
     * 여러 개의 문서를 한 번에 벡터 DB에 저장합니다.
     *
     * @param pDTO 저장할 문서들의 내용 리스트
     * @return 저장 성공 메시지를 담은 CommonResponse 객체
     */
    @PostMapping("/batch-documents")
    public ResponseEntity<CommonResponse> saveBatchDocuments(@RequestBody DocumentsDTO pDTO) {
        log.info("saveBatchDocuments Start!");

        log.info("pDTO : " + pDTO);
        // 각 문서 내용을 반복하며 벡터 DB에 저장
        pDTO.contents().forEach(embeddingService::saveDocument);

        // 성공 메시지 DTO 생성
        MsgDTO msgDTO = MsgDTO.builder().result(1).msg("여러 건이 저장되었습니다.").build();

        log.info("saveBatchDocuments End!");

        // 저장 성공 메시지를 HTTP 200 OK 응답으로 반환
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), msgDTO));
    }
}
