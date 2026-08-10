package br.cadi.people.application;

import br.cadi.iam.domain.Role;
import br.cadi.iam.domain.User;
import br.cadi.iam.infra.RoleRepository;
import br.cadi.iam.infra.UserRepository;
import br.cadi.people.api.dto.request.GuardianRequest;
import br.cadi.people.api.dto.response.GuardianResponse;
import br.cadi.people.domain.entities.Address;
import br.cadi.people.domain.entities.Guardian;
import br.cadi.people.infra.repositories.AddressRepository;
import br.cadi.people.infra.repositories.GuardianRepository;
import br.cadi.people.infra.repositories.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public GuardianResponse create(GuardianRequest dto) {
        if (guardianRepository.findByCpf(dto.cpf()).isPresent()) {
            throw new RuntimeException("CPF já cadastrado.");
        }

        Address address = addressRepository.save(Address.builder()
                .zipCode(dto.address().zipCode())
                .street(dto.address().street())
                .number(dto.address().number())
                .neighborhood(dto.address().neighborhood())
                .city(dto.address().city())
                .state(dto.address().state())
                .complement(dto.address().complement())
                .build());

        Role guardianRole = roleRepository.findByName("ROLE_PARENT")
                .orElseThrow(() -> new RuntimeException("Role ROLE_PARENT não encontrada."));

        User user = userRepository.save(User.builder()
                .username(dto.email())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(guardianRole)
                .build());

        Guardian guardian = Guardian.builder()
                .name(dto.name())
                .email(dto.email())
                .cpf(dto.cpf())
                .phone(dto.phone())
                .birthDate(dto.birthDate())
                .address(address)
                .user(user)
                .isPrimary(dto.isPrimary())
                .build();

        return mapToResponse(guardianRepository.save(guardian));
    }

    @Transactional(readOnly = true)
    public List<GuardianResponse> findAll() {
        return guardianRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuardianResponse findById(Long id) {
        Guardian guardian = guardianRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsável não encontrado ID: " + id));
        return mapToResponse(guardian);
    }

    @Transactional
    public GuardianResponse update(Long id, GuardianRequest dto) {
        Guardian guardian = guardianRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsável não encontrado."));

        guardian.setName(dto.name());
        guardian.setEmail(dto.email());
        guardian.setPhone(dto.phone());
        guardian.setIsPrimary(dto.isPrimary());

        Address addr = guardian.getAddress();
        addr.setZipCode(dto.address().zipCode());
        addr.setStreet(dto.address().street());
        addr.setNumber(dto.address().number());
        addr.setNeighborhood(dto.address().neighborhood());
        addr.setCity(dto.address().city());
        addr.setState(dto.address().state());
        addr.setComplement(dto.address().complement());
        addressRepository.save(addr);

        if (guardian.getUser() != null) {
            User user = guardian.getUser();
            user.setEmail(dto.email());
            user.setUsername(dto.email());
            userRepository.save(user);
        }

        return mapToResponse(guardianRepository.save(guardian));
    }

    // --- APENAS UM MÉTODO DELETE AQUI ---
    @Transactional
    public void delete(Long id) {
        Guardian guardian = guardianRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Responsável não encontrado ID: " + id));

        // 1. Limpa vínculos na tabela de ligação
        studentGuardianRepository.deleteByGuardianId(id);

        // 2. Deleta o login (User)
        if (guardian.getUser() != null) {
            userRepository.deleteById(guardian.getUser().getId());
        }

        // 3. Deleta o perfil
        guardianRepository.delete(guardian);
    }

    private GuardianResponse mapToResponse(Guardian g) {
        return new GuardianResponse(
                g.getId(),
                g.getName(),
                g.getEmail(),
                g.getPhone(),
                "N/A",
                g.getIsPrimary()
        );
    }
}