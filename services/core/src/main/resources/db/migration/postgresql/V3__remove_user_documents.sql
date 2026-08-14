-- ============================================================================
-- E2: a tabela `user_documents` sai do core.
--
-- O servico cadi-documents passa a ser o unico dono desses dados, no proprio
-- banco. Database-per-service so vale se a tabela realmente deixar de existir
-- aqui — mantê-la "por seguranca" recriaria o shared database por outro nome.
--
-- `attendance.justified_by_document_id` PERMANECE: continua identificando qual
-- atestado justificou a falta. Deixa de ser FK e passa a ser referencia logica
-- para um agregado de outro servico — o mesmo padrao que a coluna ja usava no
-- lado Java (era `Long`, nunca `@ManyToOne`).
-- ============================================================================

ALTER TABLE attendance DROP CONSTRAINT IF EXISTS fk_attendance_justifying_doc;

DROP TABLE IF EXISTS user_documents;

COMMENT ON COLUMN attendance.justified_by_document_id IS
    'Id do documento no servico cadi-documents. Referencia logica: sem FK, '
    'porque o agregado vive em outro banco. Preenchido pelo consumidor do '
    'evento AtestadoApproved.';
