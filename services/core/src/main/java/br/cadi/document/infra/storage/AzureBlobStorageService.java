package br.cadi.document.infra.storage;

import br.cadi.document.domain.exception.DocumentStorageException;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class AzureBlobStorageService implements DocumentStorageService {

    @Value("${cadi.storage.connection-string}")
    private String connectionString;

    @Value("${cadi.storage.container-name}")
    private String containerName;

    private volatile BlobContainerClient containerClient;

    private BlobContainerClient client() {
        if (containerClient == null) {
            synchronized (this) {
                if (containerClient == null) {
                    BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                            .connectionString(connectionString)
                            .buildClient();
                    BlobContainerClient client = serviceClient.getBlobContainerClient(containerName);
                    client.createIfNotExists();
                    containerClient = client;
                }
            }
        }
        return containerClient;
    }

    @Override
    public String upload(MultipartFile file, String storageKey) {
        try {
            client().getBlobClient(storageKey)
                    .upload(file.getInputStream(), file.getSize(), true);
            return storageKey;
        } catch (IOException e) {
            throw new DocumentStorageException("upload", e);
        } catch (Exception e) {
            throw new DocumentStorageException("upload", e);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            return client().getBlobClient(storageKey).openInputStream();
        } catch (Exception e) {
            throw new DocumentStorageException("download", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            client().getBlobClient(storageKey).deleteIfExists();
        } catch (Exception e) {
            throw new DocumentStorageException("delete", e);
        }
    }
}
