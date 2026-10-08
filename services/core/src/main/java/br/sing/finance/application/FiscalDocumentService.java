package br.sing.finance.application;

import br.sing.exception.EntityNotFoundException;
import br.sing.finance.api.dto.request.FiscalDocumentCreateRequest;
import br.sing.finance.api.dto.request.FiscalDocumentUpdateRequest;
import br.sing.finance.api.dto.response.FiscalDocumentResponse;
import br.sing.finance.domain.FiscalDocument;
import br.sing.finance.domain.FiscalDocumentType;
import br.sing.finance.infra.repositories.FiscalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FiscalDocumentService {

    private final FiscalDocumentRepository fiscalDocumentRepository;

    @Transactional(readOnly = true)
    public List<FiscalDocumentResponse> listAll(FiscalDocumentType type, String documentNumber) {
        List<FiscalDocument> docs;
        if (documentNumber != null && !documentNumber.isBlank()) {
            docs = fiscalDocumentRepository.findByDocumentNumberContainingIgnoreCaseOrderByIssueDateDesc(documentNumber.trim());
        } else if (type != null) {
            docs = fiscalDocumentRepository.findByDocumentTypeOrderByIssueDateDesc(type);
        } else {
            docs = fiscalDocumentRepository.findAll();
        }
        return docs.stream().map(FiscalDocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FiscalDocumentResponse getById(Long id) {
        return FiscalDocumentResponse.from(findOrThrow(id));
    }

    @Transactional
    public FiscalDocumentResponse create(FiscalDocumentCreateRequest request) {
        FiscalDocument entity = FiscalDocument.builder()
                .documentType(request.documentType())
                .documentNumber(request.documentNumber().trim())
                .issueDate(request.issueDate())
                .totalAmount(request.totalAmount())
                .supplierName(request.supplierName())
                .supplierDoc(request.supplierDoc())
                .fileUrl(request.fileUrl())
                .build();
        return FiscalDocumentResponse.from(fiscalDocumentRepository.save(entity));
    }

    @Transactional
    public FiscalDocumentResponse update(Long id, FiscalDocumentUpdateRequest request) {
        FiscalDocument entity = findOrThrow(id);
        entity.setDocumentType(request.documentType());
        entity.setDocumentNumber(request.documentNumber().trim());
        entity.setIssueDate(request.issueDate());
        entity.setTotalAmount(request.totalAmount());
        entity.setSupplierName(request.supplierName());
        entity.setSupplierDoc(request.supplierDoc());
        entity.setFileUrl(request.fileUrl());
        return FiscalDocumentResponse.from(fiscalDocumentRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        FiscalDocument entity = findOrThrow(id);
        entity.setActive(false);
        entity.setDeactivatedAt(java.time.LocalDateTime.now());
        fiscalDocumentRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public FiscalDocument findEntityOrNull(Long id) {
        if (id == null) {
            return null;
        }
        return fiscalDocumentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento fiscal não encontrado"));
    }

    private FiscalDocument findOrThrow(Long id) {
        return fiscalDocumentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento fiscal não encontrado"));
    }
}
