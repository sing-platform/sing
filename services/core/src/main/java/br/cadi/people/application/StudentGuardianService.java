package br.cadi.people.application;

import br.cadi.people.domain.entities.Student;
import br.cadi.people.domain.entities.StudentGuardian;
import br.cadi.people.infra.repositories.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentGuardianService {

    private final StudentGuardianRepository studentGuardianRepository;
    
    public List<StudentGuardian> getFamilyTree(Long guardianId) {
        return studentGuardianRepository.findFamilyTreeByGuardianId(guardianId);
    }

    public boolean isGuardianOf(Long guardianId, Long studentId) {
        return studentGuardianRepository.existsByStudentIdAndGuardianId(studentId, guardianId);
    }

    public List<String> getGuardianNamesByStudent(Student s) {
        return studentGuardianRepository.findAllByStudent(s).stream()
                .map(sg -> sg.getGuardian().getName())
                .toList();
    }
}