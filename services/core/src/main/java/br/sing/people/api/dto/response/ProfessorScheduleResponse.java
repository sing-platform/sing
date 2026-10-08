package br.sing.people.api.dto.response;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;

@SuperBuilder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class ProfessorScheduleResponse extends CollaboratorResponse {

    List<FullSchedulesProfessorResponse> infos;

}
