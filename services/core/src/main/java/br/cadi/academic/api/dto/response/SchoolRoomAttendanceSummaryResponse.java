package br.cadi.academic.api.dto.response;

public record SchoolRoomAttendanceSummaryResponse(
        Long   schoolRoomId,
        String schoolRoomName,
        String classroom,
        String workshopName,

        // Grade
        int    totalExpectedLectures,   // dias letivos previstos pela grade no período
        int    registeredLectures,      // aulas em que a chamada foi registrada
        int    pendingLectures,         // aulas previstas sem chamada registrada
        double lectureRegistrationRate, // registeredLectures / totalExpectedLectures * 100

        // Frequência média dos alunos
        int    totalStudents,
        double avgAttendanceRate        // média de attendanceRate de todos os alunos
) {}
