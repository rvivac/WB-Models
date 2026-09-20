package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.config.SupabaseProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LocalStorageControllerTest {

    private MockMvc mockMvc;
    private SupabaseProperties supabaseProperties;
    private final Path testUploadDir = Paths.get("target/test-storage-controller");

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(testUploadDir.resolve("candidates-uploads/submissions/test-id"));
        Files.write(testUploadDir.resolve("candidates-uploads/submissions/test-id/photo.jpg"), "fake-image-data".getBytes());

        supabaseProperties = new SupabaseProperties();
        supabaseProperties.getStorage().setLocalDir(testUploadDir.toString());

        LocalStorageController controller = new LocalStorageController(supabaseProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (Files.exists(testUploadDir)) {
            Files.walk(testUploadDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("Deve servir arquivo local com 200 OK e MediaType correto")
    void shouldServeLocalFileSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/storage/local/candidates-uploads/submissions/test-id/photo.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes("fake-image-data".getBytes()));
    }

    @Test
    @DisplayName("Deve retornar 404 quando arquivo local não existir")
    void shouldReturn404WhenFileNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/storage/local/candidates-uploads/submissions/test-id/not-found.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve bloquear tentativa de directory traversal")
    void shouldBlockDirectoryTraversal() throws Exception {
        mockMvc.perform(get("/api/v1/storage/local/candidates-uploads/../../etc/passwd"))
                .andExpect(status().is4xxClientError());
    }
}
