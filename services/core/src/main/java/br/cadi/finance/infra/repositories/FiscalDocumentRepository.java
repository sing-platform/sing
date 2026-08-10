package br.cadi.finance.infra.repositories;

import br.cadi.finance.domain.FiscalDocument;
import br.cadi.finance.domain.FiscalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FiscalDocumentRepository extends JpaRepository<FiscalDocument, Long> {
    List<FiscalDocument> findByDocumentTypeOrderByIssueDateDesc(FiscalDocumentType documentType);
    List<FiscalDocument> findByDocumentNumberContainingIgnoreCaseOrderByIssueDateDesc(String documentNumber);
}
