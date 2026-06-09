package br.com.uebiescola.core.application.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Upload de avatares (aluno, professor, responsavel, usuario) pro bucket S3.
 *
 * Fluxo:
 * 1. Valida tipo (jpg/png/webp) e tamanho (max 5MB).
 * 2. Redimensiona pra 512x512 (crop centralizado).
 * 3. Comprime pra JPEG quality 85.
 * 4. Upload pro caminho `{schoolId}/{kind}/{uuid}.jpg` no bucket publico.
 * 5. Retorna URL `https://{bucket}.s3.{region}.amazonaws.com/{path}`.
 *
 * Credenciais: na EC2 da producao, usa a IAM role atrelada (instance profile);
 * em dev local, usa AWS_PROFILE/AWS credentials default chain.
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

    @Value("${app.media.region:sa-east-1}")
    private String region;

    @Value("${app.media.public-base-url:}")
    private String publicBaseUrlOverride;

    private S3Client s3() {
        return S3Client.builder().region(Region.of(region)).build();
    }

    private String publicBaseUrl() {
        if (publicBaseUrlOverride != null && !publicBaseUrlOverride.isBlank()) {
            return publicBaseUrlOverride.replaceAll("/$", "");
        }
        return String.format("https://%s.s3.%s.amazonaws.com", bucketName, region);
    }

    public enum Kind {
        STUDENT, TEACHER, GUARDIAN, USER, SCHOOL;

        public String pathSegment() {
            return name().toLowerCase() + "s";
        }

        public static Kind fromString(String s) {
            if (s == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de midia ausente");
            try {
                return Kind.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de midia invalido: " + s);
            }
        }
    }

    /**
     * Faz upload de uma imagem e retorna a URL publica.
     */
    public String upload(MultipartFile file, Long schoolId, Kind kind, String entityUuid) {
        validate(file);

        byte[] processed = processImage(file);

        String objectKey = String.format("%d/%s/%s.jpg",
                schoolId,
                kind.pathSegment(),
                entityUuid != null && !entityUuid.isBlank() ? entityUuid : UUID.randomUUID());

        try (S3Client client = s3()) {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType("image/jpeg")
                    .cacheControl("public, max-age=86400")
                    .build();
            client.putObject(put, RequestBody.fromBytes(processed));
            String url = String.format("%s/%s?v=%d",
                    publicBaseUrl(), objectKey, System.currentTimeMillis());
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo de imagem e obrigatorio.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem excede 5 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato nao suportado. Envie JPG, PNG ou WebP.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel processar a imagem: " + e.getMessage());
        }
    }

    /** Apaga um objeto do bucket (best effort — nao falha se objeto nao existe). */
    public void delete(String url) {
        if (url == null || url.isBlank()) return;
        try {
            String prefix = publicBaseUrl() + "/";
            if (!url.startsWith(prefix)) {
                log.debug("[MEDIA] URL fora do nosso bucket, ignorando delete: {}", url);
                return;
            }
            String objectKey = url.substring(prefix.length());
            int q = objectKey.indexOf('?');
            if (q > 0) objectKey = objectKey.substring(0, q);
            try (S3Client client = s3()) {
                client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build());
            }
            log.info("[MEDIA] Apagado: {}", objectKey);
        } catch (Exception e) {
            log.warn("[MEDIA] Falha ao apagar (ignorando): {}", e.getMessage());
        }
    }
}
