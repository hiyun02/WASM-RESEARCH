package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record DocumentsDTO(String id,
                           String question,
                           String content,
                           List<String> contents,
                           List<Double> embedding
) {

}

