package br.com.uebiescola.core.application.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Upload de avatares (aluno, professor, responsável, usuário) para o bucket GCS.
 *
 * Fluxo:
 * 1. Valida tipo (jpg/png/webp) e tamanho (max 5MB).
 * 2. Redimensiona para 512x512 (crop centralizado, mantém aspect ratio quadrado).
 * 3. Comprime para JPEG quality 85.
 * 4. Faz upload pro caminho `{schoolId}/{kind}/{uuid}.jpg` no bucket público.
 * 5. Retorna URL `https://storage.googleapis.com/{bucket}/{path}`.
 *
 * Credenciais: usa Application Default Credentials (na VM do GCP, pega do
 * metadata server da Compute Engine SA; em dev local, usa o resultado de
 * `gcloud auth application-default login`).
 */
@Service
@Slf4j
public class MediaUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int TARGET_DIMENSION = 512;
    private static final float JPEG_QUALITY = 0.85f;

    @Value("${app.media.bucket:uebiescola-media}")
    private String bucketName;

    @Value("${app.media.public-base-url:https://storage.googleapis.com}")
    private String publicBaseUrl;

    private Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    public enum Kind {
        STUDENT, TEACHER, GUARDIAN, USER, SCHOOL;

        public String pathSegment() {
            return name().toLowerCase() + "s";
        }

        public static Kind fromString(String s) {
            if (s == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de mídia ausente");
            try {
                return Kind.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de mídia inválido: " + s);
            }
        }
    }

    /**
     * Faz upload de uma imagem e retorna a URL pública.
     *
     * @param file     arquivo enviado pelo cliente
     * @param schoolId tenant
     * @param kind     student / teacher / guardian / user / school
     * @param entityUuid uuid da entidade dona (vai virar parte do nome do arquivo)
     */
    public String upload(MultipartFile file, Long schoolId, Kind kind, String entityUuid) {
        validate(file);

        byte[] processed = processImage(file);

        String objectName = String.format("%d/%s/%s.jpg",
                schoolId,
                kind.pathSegment(),
                entityUuid != null && !entityUuid.isBlank() ? entityUuid : UUID.randomUUID());

        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/jpeg")
                    .setCacheControl("public, max-age=86400")
                    .build();
            storage().create(blobInfo, processed);
            String url = String.format("%s/%s/%s?v=%d",
                    publicBaseUrl, bucketName, objectName, System.currentTimeMillis());
            log.info("[MEDIA] Upload OK: school={} kind={} uuid={} bytes={} url={}",
                    schoolId, kind, entityUuid, processed.length, url);
            return url;
        } catch (Exception e) {
            log.error("[MEDIA] Falha no upload: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao enviar imagem: " + e.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo de imagem é obrigatório.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem excede 5 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato não suportado. Envie JPG, PNG ou WebP.");
        }
    }

    private byte[] processImage(MultipartFile file) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(file.getInputStream())
                    .size(TARGET_DIMENSION, TARGET_DIMENSION)
                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível processar a imagem: " + e.getMessage());
        }
    }

    /** Apaga um objeto do bucket (best effort — não falha se objeto não existe). */
    public void delete(String url) {
        if (url == null || url.isBlank()) return;
        try {
            String prefix = publicBaseUrl + "/" + bucketName + "/";
            if (!url.startsWith(prefix)) {
                log.debug("[MEDIA] URL fora do nosso bucket, ignorando delete: {}", url);
                return;
            }
            String objectName = url.substring(prefix.length());
            int q = objectName.indexOf('?');
            if (q > 0) objectName = objectName.substring(0, q);
            storage().delete(BlobId.of(bucketName, objectName));
            log.info("[MEDIA] Apagado: {}", objectName);
        } catch (Exception e) {
            log.warn("[MEDIA] Falha ao apagar (ignorando): {}", e.getMessage());
        }
    }
}
