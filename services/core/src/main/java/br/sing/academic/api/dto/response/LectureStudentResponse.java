package br.sing.academic.api.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class LectureStudentResponse {
    private Long schoolRoomId;
    public String SchoolRoomName;
    public String lectureCurrentProfessorName;

}
