package br.sing.config;

import br.sing.iam.domain.RolesEnum;
import br.sing.people.domain.enums.FamilyTie;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

final class SeedPersonCatalog {

    static final String PASSWORD = "SING@123";

    static final String[] WORKSHOP_NAMES = {
            "Cidadania", "Futebol", "Ginástica Acrobática", "Jiu Jitsu", "Karatê",
            "Literatura", "Muay Thai", "Robótica", "Rugby"
    };

    private SeedPersonCatalog() {
    }

    static List<SeedPerson> allPeople() {
        List<SeedPerson> people = new ArrayList<>();
        people.addAll(adminAndStaff());
        people.addAll(teachers());
        people.addAll(secretaries());
        people.addAll(psychologists());
        people.addAll(financeStaff());
        people.addAll(parents());
        people.addAll(students());
        return people;
    }

    private static List<SeedPerson> adminAndStaff() {
        return List.of(new SeedPerson(
                "admin@sing.local", "admin", RolesEnum.ROLE_ADMIN.name(),
                "Admin", "SING", "111.444.777-35"));
    }

    private static List<SeedPerson> teachers() {
        return List.of(
                new SeedPerson("teacher@sing.local", "teacher", RolesEnum.ROLE_PROFESSOR.name(),
                        "Ricardo", "Mendes", "222.333.444-05"),
                person(RolesEnum.ROLE_PROFESSOR.name(), "Fernanda", "Oliveira", "222.333.444-16"),
                person(RolesEnum.ROLE_PROFESSOR.name(), "Paulo", "Almeida", "222.333.444-27"),
                person(RolesEnum.ROLE_PROFESSOR.name(), "Camila", "Rocha", "222.333.444-38")
        );
    }

    private static List<SeedPerson> secretaries() {
        return List.of(
                new SeedPerson("secretary@sing.local", "secretary", RolesEnum.ROLE_SECRETARY.name(),
                        "Amanda", "Costa", "555.666.777-42"),
                person(RolesEnum.ROLE_SECRETARY.name(), "Renata", "Souza", "555.666.777-51"),
                person(RolesEnum.ROLE_SECRETARY.name(), "Juliana", "Martins", "555.666.777-62")
        );
    }

    private static List<SeedPerson> psychologists() {
        return List.of(
                new SeedPerson("psychologist@sing.local", "psychologist", RolesEnum.ROLE_PSYCHOLOGIST.name(),
                        "Cláudia", "Ferreira", "666.777.888-10"),
                person(RolesEnum.ROLE_PSYCHOLOGIST.name(), "Bruno", "Nascimento", "666.777.888-21")
        );
    }

    private static List<SeedPerson> financeStaff() {
        return List.of(
                new SeedPerson("financeiro@sing.local", "financeiro", RolesEnum.ROLE_MANAGER.name(),
                        "Marcos", "Silva", "777.888.999-30"),
                person(RolesEnum.ROLE_MANAGER.name(), "Carla", "Mendes", "777.888.999-41")
        );
    }

    private static List<SeedPerson> parents() {
        return List.of(
                new SeedPerson("parent@sing.local", "parent", RolesEnum.ROLE_PARENT.name(),
                        "Roberto", "Souza", "444.555.666-20"),
                person(RolesEnum.ROLE_PARENT.name(), "Patrícia", "Lima", "444.555.666-31"),
                person(RolesEnum.ROLE_PARENT.name(), "Antônio", "Pires", "444.555.666-42"),
                person(RolesEnum.ROLE_PARENT.name(), "Márcia", "Nunes", "444.555.666-53"),
                person(RolesEnum.ROLE_PARENT.name(), "José", "Araújo", "444.555.666-64"),
                person(RolesEnum.ROLE_PARENT.name(), "Sandra", "Ferreira", "444.555.666-75"),
                person(RolesEnum.ROLE_PARENT.name(), "Carlos", "Cardoso", "444.555.666-86"),
                person(RolesEnum.ROLE_PARENT.name(), "Luciana", "Monteiro", "444.555.666-97"),
                person(RolesEnum.ROLE_PARENT.name(), "Paulo", "Santos", "444.555.666-08"),
                person(RolesEnum.ROLE_PARENT.name(), "Rita", "Ribeiro", "444.555.666-19"),
                person(RolesEnum.ROLE_PARENT.name(), "Marcos", "Costa", "444.555.666-22"),
                person(RolesEnum.ROLE_PARENT.name(), "Eliane", "Martins", "444.555.666-33")
        );
    }

    private static List<SeedPerson> students() {
        List<SeedPerson> list = new ArrayList<>(40);
        String[][] vespertino = {
                {"Lucas", "Souza"}, {"Mariana", "Lima"}, {"Gabriel", "Pires"}, {"Beatriz", "Nunes"},
                {"Thiago", "Araújo"}, {"Letícia", "Ferreira"}, {"André", "Cardoso"}, {"Juliana", "Monteiro"},
                {"Pedro", "Santos"}, {"Ana", "Ribeiro"}, {"Rafael", "Costa"}, {"Isabela", "Martins"},
                {"Felipe", "Barbosa"}, {"Camila", "Duarte"}, {"Gustavo", "Melo"}, {"Larissa", "Gomes"},
                {"Bruno", "Oliveira"}, {"Sofia", "Carvalho"}, {"Daniel", "Teixeira"}, {"Vitória", "Lopes"}
        };
        String[][] matutino = {
                {"João", "Marques"}, {"Carolina", "Dias"}, {"Mateus", "Freitas"}, {"Amanda", "Rocha"},
                {"Leonardo", "Vieira"}, {"Fernanda", "Castro"}, {"Igor", "Mendes"}, {"Helena", "Azevedo"},
                {"Vinícius", "Pinto"}, {"Bianca", "Moreira"}, {"Enzo", "Correia"}, {"Valentina", "Cunha"},
                {"Miguel", "Farias"}, {"Alice", "Moura"}, {"Arthur", "Nogueira"}, {"Laura", "Campos"},
                {"Henrique", "Lacerda"}, {"Manuela", "Peixoto"}, {"Caio", "Rangel"}, {"Giovanna", "Batista"}
        };
        int cpfSeq = 1;
        for (int i = 0; i < vespertino.length; i++) {
            String[] n = vespertino[i];
            String email = i == 0 ? "student@sing.local" : SeedEmailUtils.emailFromPersonName(n[0], n[1]);
            list.add(new SeedPerson(
                    email, email.split("@")[0], RolesEnum.ROLE_STUDENT.name(),
                    n[0], n[1], studentCpf(cpfSeq++), "VESPERTINO"));
        }
        for (String[] n : matutino) {
            list.add(new SeedPerson(
                    SeedEmailUtils.emailFromPersonName(n[0], n[1]),
                    SeedEmailUtils.emailFromPersonName(n[0], n[1]).split("@")[0],
                    RolesEnum.ROLE_STUDENT.name(),
                    n[0], n[1], studentCpf(cpfSeq++), "MATUTINO"));
        }
        return list;
    }

    record GuardianLink(String studentEmail, String guardianEmail, FamilyTie familyTie) {
    }

    static List<GuardianLink> guardianLinks() {
        List<SeedPerson> students = students();
        List<SeedPerson> guardians = parents();
        List<GuardianLink> links = new ArrayList<>();
        FamilyTie[] ties = {FamilyTie.FATHER, FamilyTie.MOTHER, FamilyTie.ANOTHER_FAMILY_TIE};
        for (int i = 0; i < students.size(); i++) {
            SeedPerson student = students.get(i);
            SeedPerson guardian = guardians.get(i % guardians.size());
            links.add(new GuardianLink(student.email(), guardian.email(), ties[i % ties.length]));
        }
        return links;
    }

    static List<SeedSlotSpec> morningScheduleSlots() {
        return List.of(
                slot(DayOfWeek.MONDAY, 8, 0, 10, 0),
                slot(DayOfWeek.MONDAY, 10, 15, 12, 15),
                slot(DayOfWeek.TUESDAY, 8, 0, 10, 0),
                slot(DayOfWeek.TUESDAY, 10, 15, 12, 15),
                slot(DayOfWeek.WEDNESDAY, 8, 0, 10, 0),
                slot(DayOfWeek.WEDNESDAY, 10, 15, 12, 15),
                slot(DayOfWeek.THURSDAY, 8, 0, 10, 0),
                slot(DayOfWeek.THURSDAY, 10, 15, 12, 15),
                slot(DayOfWeek.FRIDAY, 8, 0, 10, 0)
        );
    }

    static List<SeedSlotSpec> afternoonScheduleSlots() {
        return List.of(
                slot(DayOfWeek.MONDAY, 14, 0, 16, 0),
                slot(DayOfWeek.MONDAY, 16, 15, 18, 15),
                slot(DayOfWeek.TUESDAY, 14, 0, 16, 0),
                slot(DayOfWeek.TUESDAY, 16, 15, 18, 15),
                slot(DayOfWeek.WEDNESDAY, 14, 0, 16, 0),
                slot(DayOfWeek.WEDNESDAY, 16, 15, 18, 15),
                slot(DayOfWeek.THURSDAY, 14, 0, 16, 0),
                slot(DayOfWeek.THURSDAY, 16, 15, 18, 15),
                slot(DayOfWeek.FRIDAY, 14, 0, 16, 0)
        );
    }

    private static SeedPerson person(String role, String first, String last, String cpf) {
        String email = SeedEmailUtils.emailFromPersonName(first, last);
        return new SeedPerson(email, email.split("@")[0], role, first, last, cpf);
    }

    private static String studentCpf(int seq) {
        return String.format("333.444.555-%02d", seq);
    }

    private static SeedSlotSpec slot(DayOfWeek day, int sh, int sm, int eh, int em) {
        return new SeedSlotSpec(day, LocalTime.of(sh, sm), LocalTime.of(eh, em));
    }
}
