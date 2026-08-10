package br.cadi.document.infra.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface DocumentStorageService {

    String upload(MultipartFile file, String storageKey);

    InputStream download(String storageKey);

    void delete(String storageKey);
}
