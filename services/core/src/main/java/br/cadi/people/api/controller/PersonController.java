package br.cadi.people.api.controller;


import br.cadi.people.application.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/person")
@RequiredArgsConstructor
@Tag(name = "Person", description = "EndPoint for Personal info of the user, by role")
public class PersonController {
    private final PersonService service;


    @Operation(
            summary = "Retornar o dado do usuario logado",
            description = """ 
                    Analiza a role do token e retorna uma serie de dados da pessoa (People)\n
                    \n
                        {\n
                          "name": "Prof 1 Silva",\n
                          "email": "teacher1@cadi.com",\n
                          "phone": "119000000",\n
                          "birthDate": "1990-01-01",\n
                          "address": {\n
                            "street": "Rua das Flores",\n
                            "number": "200",\n
                            "city": "São Paulo",\n
                            "state": "SP",\n
                            "zip": "01320-200",\n
                            "complement": "",\n
                            "neighborhood": "Tingui"\n
                          },\n
                      "role": "ROLE_PROFESSOR"\n
                    }\n
                    \n
                    --- Adicional por Role ---\n
                    \n
                    ROLE_PROFESSOR (Teacher):\n
                    {\n
                      "infos": [\n
                        {\n
                          "workshop": { "id": 4, "name": "Culinária", "active": true },\n
                          "students": [\n
                            { "name": "Aluno", "surname": "15 Silva", "email": "aluno15@cadi.com" },\n
                            { "name": "Aluno", "surname": "1 Silva", "email": "aluno1@cadi.com" }\n
                          ],\n
                          "schedules": [\n
                            { "id": 7, "dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "11:00" }\n
                          ]\n
                        }\n
                      ]\n
                    }\n
                    \n
                    ROLE_STUDENT (Student):\n
                    {\n
                      "lectures": [\n
                        {\n
                          "SchoolRoomName": "Futebol - Manhã",\n
                          "lectureCurrentProfessorName": "Professor: Prof 5 Silva"\n
                        }\n
                      ],\n
                      "guardiansNames": ["Pai 4 Silva"]\n
                    }\n
                    \n
                    ROLE_PARENT (Parent):\n
                    {\n
                      "students": [\n
                        {\n
                          "name": "Aluno 7 Silva",\n
                          "email": "aluno7@cadi.com",\n
                          "lectures": [\n
                            {\n
                              "schoolRoomName": "Pintura - Tarde",\n
                              "lectureCurrentProfessorName": "Professor ID: 18"\n
                            }\n
                          ]\n
                        }\n
                      ]\n
                    }
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Informações retornadas com sucesso"),

            @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content(examples = @ExampleObject(
                    "{\n" +
                            "  \"status\": 401,\n" +
                            "  \"error\": null,\n" +
                            "  \"message\": \"Unauthorized\",\n" +
                            "  \"timestamp\": 2026-03-27T14:44:25.3132291,\n" +
                            "  \"path\": \"/api/v1/person/me\",\n" +
                            "  \"details\": [\n" +
                            "  ]\n" +
                            "}"
            )))
    })
    @GetMapping("/me")
    public ResponseEntity<Object> me() {
        return ResponseEntity.ok(service.getMe());
    }

}
