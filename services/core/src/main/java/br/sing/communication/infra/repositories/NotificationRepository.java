package br.sing.communication.infra.repositories;

import br.sing.communication.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndReferenceKeyAndReadAtIsNull(Long userId, String referenceKey);
}
