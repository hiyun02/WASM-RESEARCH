package kopo.poly.controller.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

/**
 * 공통 응답 객체 클래스.
 * 모든 컨트롤러에서 일관된 형식으로 응답을 반환하기 위해 사용됩니다.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@Setter
public class CommonResponse<T> {

    /**
     * HTTP 상태 코드 (예: 200 OK, 400 BAD_REQUEST 등).
     */
    private HttpStatus httpStatus;

    /**
     * 응답 메시지 (예: 성공, 오류 메시지 등).
     */
    private String message;

    /**
     * 실제 응답 데이터.
     */
    private T data;

    /**
     * 생성자.
     *
     * @param httpStatus HTTP 상태 코드
     * @param message    응답 메시지
     * @param data       응답 데이터
     */
    @Builder
    public CommonResponse(HttpStatus httpStatus, String message, T data) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.data = data;
    }

    /**
     * 정적 팩토리 메서드로 CommonResponse 객체를 생성합니다.
     *
     * @param httpStatus HTTP 상태 코드
     * @param message    응답 메시지
     * @param data       응답 데이터
     * @param <T>        응답 데이터 타입
     * @return 생성된 CommonResponse 객체
     */
    public static <T> CommonResponse<T> of(HttpStatus httpStatus, String message, T data) {
        return new CommonResponse<>(httpStatus, message, data);
    }

    /**
     * 유효성 검사 오류를 처리하는 메서드.
     * BindingResult에서 발생한 모든 오류를 반환합니다.
     *
     * @param bindingResult 유효성 검사 결과 객체
     * @return 400 BAD_REQUEST 상태와 오류 메시지를 포함한 ResponseEntity 객체
     */
    public static ResponseEntity<CommonResponse> getErrors(BindingResult bindingResult) {
        return ResponseEntity.badRequest()
                .body(CommonResponse.of(
                        HttpStatus.BAD_REQUEST,
                        HttpStatus.BAD_REQUEST.series().name(),
                        bindingResult.getAllErrors()
                ));
    }
}
