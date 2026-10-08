package br.sing.finance.infra.repositories;

import br.sing.finance.domain.FiscalDocument;
import br.sing.finance.domain.FiscalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, Long> {
    List<FiscalDocument> findByDocumentTypeOrderByIssueDateDesc(FiscalDocumentType documentType);
    List<FiscalDocument> findByDocumentNumberContainingIgnoreCaseOrderByIssueDateDesc(String documentNumber);
}
