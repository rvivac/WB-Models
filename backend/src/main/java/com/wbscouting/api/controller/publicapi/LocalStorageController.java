package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.config.SupabaseProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping({"/storage/local", "/api/v1/storage/local"})
@RequiredArgsConstructor
public class LocalStorageController {

    private final SupabaseProperties supabaseProperties;

    @GetMapping("/{bucket}/**")
    public ResponseEntity<Resource> getLocalFile(
            @PathVariable String bucket,
            HttpServletRequest request
    ) {
        String uri = request.getRequestURI();
        String prefix = "/storage/local/" + bucket + "/";
        int idx = uri.indexOf(prefix);
        if (idx < 0) {
            prefix = "/api/v1/storage/local/" + bucket + "/";
            idx = uri.indexOf(prefix);
        }

        if (idx < 0) {
            return ResponseEntity.notFound().build();
        }

        String relativePath = uri.substring(idx + prefix.length());
        if (!StringUtils.hasText(relativePath)) {
            return ResponseEntity.notFound().build();
        }

        String baseDir = (supabaseProperties.getStorage() != null && StringUtils.hasText(supabaseProperties.getStorage().getLocalDir()))
                ? supabaseProperties.getStorage().getLocalDir()
                : "uploads";

        Path baseFolder = Paths.get(baseDir, bucket).toAbsolutePath().normalize();
        Path targetFile = baseFolder.resolve(relativePath).normalize();

        // Proteção contra Directory Traversal (CWE-22)
        if (!targetFile.startsWith(baseFolder)) {
            log.warn("Tentativa de directory traversal detectada para path: {}", relativePath);
            return ResponseEntity.badRequest().build();
        }

        if (!java.nio.file.Files.exists(targetFile) || !java.nio.file.Files.isRegularFile(targetFile)) {
            log.debug("Arquivo local não encontrado: {}", targetFile);
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(targetFile.toUri());
            MediaType mediaType = MediaTypeFactory.getMediaType(targetFile.getFileName().toString())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(resource);
        } catch (IOException e) {
            log.error("Erro ao carregar recurso do arquivo local: {}", targetFile, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
