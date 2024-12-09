package kopo.poly.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * MongoDB에 저장되는 문서를 표현하는 엔티티 클래스.
 */
@Data
@NoArgsConstructor // 기본 생성자 추가
@Document(collection = "documents")
public class Documents {

    @Id
    private String id;

    private String content;

    private List<Double> embedding;

    @Transient
    @JsonIgnore // JSON 직렬화/역직렬화 시 무시
    private double similarityScore;

    @Builder
    public Documents(String id, String content, List<Double> embedding) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
    }
}
