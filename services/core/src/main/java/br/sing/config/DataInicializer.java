package br.sing.config;

import br.sing.academic.domain.entities.*;
import br.sing.academic.infra.repositories.*;
import br.sing.communication.domain.MuralPost;
import br.sing.communication.domain.MuralPostView;
import br.sing.communication.infra.repositories.MuralPostRepository;
import br.sing.communication.infra.repositories.MuralPostViewRepository;
import br.sing.iam.domain.RolesEnum;
import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.RoleRepository;
import br.sing.iam.infra.UserRepository;
import br.sing.people.domain.entities.Address;
import br.sing.people.domain.entities.*;
import br.sing.people.domain.enums.CollaboratorType;
import br.sing.people.infra.repositories.PersonRepository;
import br.sing.people.infra.repositories.*;
import br.sing.psychosocial.domain.PatientAssignment;
import br.sing.psychosocial.infra.PatientAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInicializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonRepository personRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final GuardianRepository guardianRepository;
    private final StudentRepository studentRepository;
    private final AddressRepository addressRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final WorkshopRepository workshopRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final MuralPostRepository muralPostRepository;
    private final MuralPostViewRepository muralPostViewRepository;
    private final PatientAssignmentRepository patientAssignmentRepository;

    private Address adminAddress;
    private Address profAddress;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        criarRoles();
        criarUsuariosSeed();
        criarEnderecos();
        popularPessoasSeed();
        popularAcademicoSeed();
        popularMuralSeed();
        popularVinculosPsicologoSeed();
    }

    private void criarUsuariosSeed() {
        for (SeedPerson person : SeedPersonCatalog.allPeople()) {
            criarUsuario(person.email(), SeedPersonCatalog.PASSWORD, person.username(), person.role());
        }
    }

    private void popularPessoasSeed() {
        for (SeedPerson person : SeedPersonCatalog.allPeople()) {
            Address addr = addressForRole(person.role());
            vincularPessoa(person, addr);
        }
        for (SeedPersonCatalog.GuardianLink link : SeedPersonCatalog.guardianLinks()) {
            vincularStudentGuardian(link.studentEmail(), link.guardianEmail(), link.familyTie());
        }
    }

    private Address addressForRole(String role) {
        if (RolesEnum.ROLE_ADMIN.name().equals(role)
                || RolesEnum.ROLE_SECRETARY.name().equals(role)
                || RolesEnum.ROLE_MANAGER.name().equals(role)
                || RolesEnum.ROLE_PARENT.name().equals(role)) {
            return adminAddress;
        }
        return profAddress;
    }

    private void popularAcademicoSeed() {
        Set<String> seedTeacherEmails = SeedPersonCatalog.allPeople().stream()
                .filter(p -> RolesEnum.ROLE_PROFESSOR.name().equals(p.role()))
                .map(SeedPerson::email)
                .collect(Collectors.toSet());
        List<Collaborator> teachers = collaboratorRepository.findAll().stream()
                .filter(c -> c.getType() == CollaboratorType.TEACHER)
                .filter(c -> seedTeacherEmails.contains(c.getEmail()))
                .sorted(java.util.Comparator.comparing(Collaborator::getEmail))
                .toList();
        if (teachers.isEmpty()) {
            throw new IllegalStateException("Nenhum professor seed encontrado.");
        }

        List<SchoolRoom> rooms = new ArrayList<>();
        List<SeedSlotSpec> morningSlots = SeedPersonCatalog.morningScheduleSlots();
        List<SeedSlotSpec> afternoonSlots = SeedPersonCatalog.afternoonScheduleSlots();
        int teacherIndex = 0;
        int lectureSeq = 0;
        int manhaSlotIdx = 0;
        int tardeSlotIdx = 0;

        for (String nomeW : SeedPersonCatalog.WORKSHOP_NAMES) {
            Workshop w = criarWorkshopSeNaoExistir(nomeW);
            for (int t = 1; t <= 2; t++) {
                Collaborator prof = teachers.get(teacherIndex % teachers.size());
                teacherIndex++;
                boolean isManha = t == 1;
                String period = isManha ? "Manhã" : "Tarde";
                SchoolRoom room = sincronizarTurmaSeed(
                        nomeW + " - " + period,
                        "Sala " + (100 + t),
                        w,
                        prof,
                        24);
                rooms.add(room);

                SeedSlotSpec primary = isManha
                        ? morningSlots.get(manhaSlotIdx++ % morningSlots.size())
                        : afternoonSlots.get(tardeSlotIdx++ % afternoonSlots.size());
                definirSlotsTurma(room, List.of(primary));

                lectureSeq++;
                criarLectureSeNaoExistir(
                        room,
                        LocalDateTime.now().plusDays(7L * lectureSeq),
                        "Aula inicial — " + nomeW);
            }
        }

        Set<String> seedStudentEmails = SeedPersonCatalog.allPeople().stream()
                .filter(p -> RolesEnum.ROLE_STUDENT.name().equals(p.role()))
                .map(SeedPerson::email)
                .collect(Collectors.toSet());
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> seedStudentEmails.contains(s.getEmail()))
                .toList();
        SeedEnrollmentPlanner.enrollBalanced(
                rooms,
                students,
                schoolRoomRepository,
                this::criarEnrollmentSeNaoExistir);
    }

    private void vincularPessoa(SeedPerson person, Address addr) {
        userRepository.findByEmail(person.email()).ifPresent(user ->
                criarPessoa(person, addr, user.getId()));
    }

    private void vincularStudentGuardian(String emailStudent, String emailParent,
                                         br.sing.people.domain.enums.FamilyTie tie) {
        guardianRepository.findByEmail(emailParent).ifPresent(g ->
                studentRepository.findByEmail(emailStudent).ifPresent(s -> {
                    if (!studentGuardianRepository.existsByStudentIdAndGuardianId(s.getId(), g.getId())) {
                        studentGuardianRepository.save(StudentGuardian.builder()
                                .familyTie(tie).guardian(g).student(s).build());
                    }
                }));
    }

    private void criarRoles() {
        List<String> roles = List.of(
                RolesEnum.ROLE_STUDENT.name(),
                RolesEnum.ROLE_ADMIN.name(),
                RolesEnum.ROLE_PARENT.name(),
                RolesEnum.ROLE_PROFESSOR.name(),
                RolesEnum.ROLE_SECRETARY.name(),
                RolesEnum.ROLE_PSYCHOLOGIST.name(),
                RolesEnum.ROLE_MANAGER.name());
        roles.forEach(nome -> {
            if (roleRepository.findByName(nome).isEmpty()) {
                roleRepository.save(Role.builder().name(nome).build());
            }
        });
    }

    @Transactional
    protected void criarUsuario(String email, String senha, String username, String roleName) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user = User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(senha))
                .role(role)
                .build();

        userRepository.save(user);
    }

    @Transactional
    protected void criarPessoa(SeedPerson person, Address address, Long userId) {
        if (personRepository.findByEmail(person.email()).isPresent()) {
            return;
        }
        if (personRepository.findByUserId(userId).isPresent()) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        var existingByCpf = personRepository.findByCpf(person.cpf());
        if (existingByCpf.isPresent()) {
            atualizarPessoaSeedExistente(person, address, user, existingByCpf.get());
            return;
        }

        String fullName = person.firstName() + " " + person.lastName();
        String phone = "11987654321";

        switch (user.getRole().getName()) {
            case "ROLE_ADMIN" -> collaboratorRepository.save(Collaborator.builder()
                    .cpf(person.cpf()).name(fullName).email(person.email()).phone(phone)
                    .birthDate(person.birthDate()).address(address).educationLevel("1")
                    .user(user).type(CollaboratorType.DIRECTOR).build());
            case "ROLE_PROFESSOR" -> collaboratorRepository.save(Collaborator.builder()
                    .cpf(person.cpf()).name(fullName).email(person.email()).phone(phone)
                    .birthDate(person.birthDate()).address(address).educationLevel("1")
                    .user(user).type(CollaboratorType.TEACHER).build());
            case "ROLE_SECRETARY" -> collaboratorRepository.save(Collaborator.builder()
                    .cpf(person.cpf()).name(fullName).email(person.email()).phone(phone)
                    .birthDate(person.birthDate()).address(address).educationLevel("1")
                    .user(user).type(CollaboratorType.SECRETARY).build());
            case "ROLE_PSYCHOLOGIST" -> collaboratorRepository.save(Collaborator.builder()
                    .cpf(person.cpf()).name(fullName).email(person.email()).phone(phone)
                    .birthDate(person.birthDate()).address(address).educationLevel("1")
                    .user(user).type(CollaboratorType.PSYCHOLOGIST).build());
            case "ROLE_MANAGER" -> collaboratorRepository.save(Collaborator.builder()
                    .cpf(person.cpf()).name(fullName).email(person.email()).phone(phone)
                    .birthDate(person.birthDate()).address(address).educationLevel("1")
                    .user(user).type(CollaboratorType.STAFF).build());
            case "ROLE_STUDENT" -> studentRepository.save(Student.builder()
                    .cpf(person.cpf()).name(fullName).email(person.email()).phone(phone)
                    .grade("1").currentSchool("PUCPR")
                    .birthDate(person.birthDate()).address(address)
                    .periodStudies(person.periodStudies() != null ? person.periodStudies() : "MATUTINO")
                    .user(user).build());
            case "ROLE_PARENT" -> guardianRepository.save(Guardian.builder()
                    .cpf(person.cpf()).name(fullName).email(person.email()).phone(phone)
                    .birthDate(person.birthDate()).address(address).user(user).isPrimary(true).build());
            default -> throw new RuntimeException("Unsupported role: " + user.getRole().getName());
        }
    }

    /**
     * Reaproveita colaborador/responsável criado em seed antigo (mesmo CPF, outro e-mail de login).
     */
    private void atualizarPessoaSeedExistente(SeedPerson person, Address address, User user, Person existing) {
        existing.setUser(user);
        existing.setEmail(person.email());
        existing.setName(person.firstName() + " " + person.lastName());
        existing.setPhone("11987654321");
        existing.setBirthDate(person.birthDate());
        existing.setAddress(address);

        if (existing instanceof Collaborator collaborator) {
            collaboratorRepository.save(collaborator);
        } else if (existing instanceof Guardian guardian) {
            guardianRepository.save(guardian);
        } else if (existing instanceof Student student) {
            if (person.periodStudies() != null) {
                student.setPeriodStudies(person.periodStudies());
            }
            studentRepository.save(student);
        }
    }

    private void criarEnderecos() {
        adminAddress = addressRepository.save(Address.builder().street("Av. Paulista").number(1000)
                .neighborhood("Bela Vista").city("São Paulo").state("SP").zipCode("01310-100").build());
        profAddress = addressRepository.save(Address.builder().street("Rua das Flores").number(200)
                .neighborhood("Vila Mariana").city("São Paulo").state("SP").zipCode("01320-200").build());
        addressRepository.save(Address.builder().street("Rua do Sol").number(150)
                .neighborhood("Pinheiros").city("São Paulo").state("SP").zipCode("01330-300").build());
        addressRepository.save(Address.builder().street("Av. Brasil").number(500)
                .neighborhood("Tatuapé").city("São Paulo").state("SP").zipCode("01340-400").build());
    }

    private Workshop criarWorkshopSeNaoExistir(String nome) {
        return workshopRepository.findByName(nome)
                .orElseGet(() -> workshopRepository.save(Workshop.builder().name(nome).isActive(true).build()));
    }

    private SchoolRoom sincronizarTurmaSeed(String nome, String sala, Workshop workshop,
                                            Collaborator teacher, Integer capacity) {
        return schoolRoomRepository.findByName(nome)
                .map(existing -> {
                    existing.setClassroom(sala);
                    existing.setWorkshop(workshop);
                    existing.setTeacher(teacher);
                    existing.setCapacity(capacity);
                    existing.setActive(true);
                    return schoolRoomRepository.save(existing);
                })
                .orElseGet(() -> schoolRoomRepository.save(SchoolRoom.builder()
                        .name(nome).classroom(sala).workshop(workshop).teacher(teacher)
                        .capacity(capacity).isActive(true).build()));
    }

    private void definirSlotsTurma(SchoolRoom schoolRoom, List<SeedSlotSpec> specs) {
        SchoolRoom managed = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(schoolRoom.getId())
                .orElse(schoolRoom);
        if (managed.getScheduleSlots() != null) {
            managed.getScheduleSlots().clear();
        }
        for (SeedSlotSpec spec : specs) {
            SchoolRoomScheduleSlot slot = SchoolRoomScheduleSlot.builder()
                    .schoolroom(managed)
                    .dayOfWeek(spec.dayOfWeek())
                    .startTime(spec.startTime())
                    .endTime(spec.endTime())
                    .build();
            if (managed.getScheduleSlots() == null) {
                managed.setScheduleSlots(new ArrayList<>());
            }
            managed.getScheduleSlots().add(slot);
        }
        schoolRoomRepository.save(managed);
    }

    private void criarEnrollmentSeNaoExistir(Student student, SchoolRoom schoolRoom) {
        if (enrollmentRepository.countByStudentIdAndStatusTrue(student.getId()) >= 2) {
            return;
        }
        if (!enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(
                student.getId(), schoolRoom.getId())) {
            enrollmentRepository.save(Enrollment.builder()
                    .student(student)
                    .schoolroom(schoolRoom)
                    .enrollmentDate(LocalDate.now())
                    .status(true)
                    .build());
        }
    }

    private void criarLectureSeNaoExistir(SchoolRoom schoolRoom, LocalDateTime dateTime, String topic) {
        if (!lectureRepository.existsBySchoolroomIdAndDateTime(schoolRoom.getId(), dateTime)) {
            lectureRepository.save(Lecture.builder()
                    .schoolroom(schoolRoom).dateTime(dateTime).topic(topic).build());
        }
    }

    private void popularMuralSeed() {
        if (muralPostRepository.count() > 0) {
            return;
        }

        User adminUser = userRepository.findByEmail("admin@sing.local").orElse(null);
        User teacherUser = userRepository.findByEmail("teacher@sing.local").orElse(null);
        User secretaryUser = userRepository.findByEmail("secretary@sing.local").orElse(null);

        if (adminUser != null) {
            Set<String> rolesPost1 = Set.of(
                    RolesEnum.ROLE_STUDENT.name(),
                    RolesEnum.ROLE_PARENT.name(),
                    RolesEnum.ROLE_PROFESSOR.name());
            MuralPost p1 = muralPostRepository.save(MuralPost.builder()
                    .title("Boas-vindas ao portal da Escola Modelo!")
                    .content("Seja bem-vindo ao novo portal da Escola Modelo. Aqui você encontrará todas as informações sobre suas atividades, horários e comunicados institucionais.")
                    .author(adminUser)
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .audienceRoles(rolesPost1)
                    .isActive(true)
                    .build());
            criarViewsSeed(p1, rolesPost1);
        }

        if (secretaryUser != null) {
            Set<String> rolesPost2 = Set.of(
                    RolesEnum.ROLE_STUDENT.name(),
                    RolesEnum.ROLE_PARENT.name());
            MuralPost p2 = muralPostRepository.save(MuralPost.builder()
                    .title("Calendário de oficinas — 2º semestre")
                    .content("O calendário completo das oficinas do 2º semestre já está disponível. Consulte os horários no painel de cada turma.")
                    .author(secretaryUser)
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .audienceRoles(rolesPost2)
                    .isActive(true)
                    .build());
            criarViewsSeed(p2, rolesPost2);
        }

        if (teacherUser != null) {
            Set<String> rolesPost3 = Set.of(
                    RolesEnum.ROLE_STUDENT.name(),
                    RolesEnum.ROLE_PSYCHOLOGIST.name());
            MuralPost p3 = muralPostRepository.save(MuralPost.builder()
                    .title("Reunião pedagógica — próxima segunda-feira")
                    .content("Lembramos a todos os alunos que a reunião pedagógica mensal acontecerá na próxima segunda-feira. Participação importante!")
                    .author(teacherUser)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .audienceRoles(rolesPost3)
                    .isActive(true)
                    .build());
            criarViewsSeed(p3, rolesPost3);
        }
    }

    private void criarViewsSeed(MuralPost post, Set<String> roles) {
        for (String role : roles) {
            List<User> recipients = userRepository.findByRole_Name(role);
            for (User recipient : recipients) {
                if (!muralPostViewRepository.existsByPostAndUser(post, recipient)) {
                    muralPostViewRepository.save(MuralPostView.builder()
                            .post(post)
                            .user(recipient)
                            .viewedAt(null)
                            .build());
                }
            }
        }
    }

    /**
     * Vincula cada psicólogo seed a um grupo de alunos (pacientes), sem
     * sobreposição, para que a visão de dados clínicos e anotações já tenha dados.
     */
    private void popularVinculosPsicologoSeed() {
        List<Collaborator> psychologists = collaboratorRepository
                .findAllByType(CollaboratorType.PSYCHOLOGIST).stream()
                .sorted(java.util.Comparator.comparing(Collaborator::getEmail))
                .toList();
        if (psychologists.isEmpty()) {
            return;
        }

        Set<String> seedStudentEmails = SeedPersonCatalog.allPeople().stream()
                .filter(p -> RolesEnum.ROLE_STUDENT.name().equals(p.role()))
                .map(SeedPerson::email)
                .collect(Collectors.toSet());
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> seedStudentEmails.contains(s.getEmail()))
                .sorted(java.util.Comparator.comparing(Student::getEmail))
                .toList();
        if (students.isEmpty()) {
            return;
        }

        int patientsPerPsychologist = 5;
        int studentIdx = 0;
        for (Collaborator psychologist : psychologists) {
            for (int i = 0; i < patientsPerPsychologist && studentIdx < students.size(); i++, studentIdx++) {
                vincularPacienteSeed(psychologist, students.get(studentIdx));
            }
        }
    }

    private void vincularPacienteSeed(Collaborator psychologist, Student student) {
        Long psychologistUserId = psychologist.getUser().getId();
        if (patientAssignmentRepository
                .existsByPsychologist_User_IdAndPatient_IdAndIsActiveTrue(psychologistUserId, student.getId())) {
            return;
        }
        patientAssignmentRepository.save(PatientAssignment.builder()
                .psychologist(psychologist)
                .patient(student)
                .createdBy(psychologist.getUser())
                .isActive(true)
                .build());
    }
}
