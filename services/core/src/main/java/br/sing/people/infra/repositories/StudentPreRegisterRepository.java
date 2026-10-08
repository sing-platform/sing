package br.sing.people.infra.repositories;

import br.sing.people.domain.entities.StudentPreRegister;
import br.sing.people.domain.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPreRegisterRepository extends JpaRepository<StudentPreRegister, Long> {

    long countByWorkshop_IdAndShift(Long workshopId, Shift shift);
}

