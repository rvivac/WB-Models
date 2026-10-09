package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.ApplyFaqCreateUpdateDto;
import com.wbscouting.api.dto.ApplyFaqDto;
import com.wbscouting.api.dto.ApplyFaqReorderDto;
import com.wbscouting.api.dto.ApplyHeaderDto;
import com.wbscouting.api.service.content.ApplyFaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@PreAuthorize("hasAnyRole('WEBMASTER', 'SUPER_ADMIN', 'CONTENT_ADMIN', 'ADMIN')")
@RequiredArgsConstructor
public class AdminApplyFaqController {

    private final ApplyFaqService applyFaqService;

    // FAQ CRUD
    @GetMapping({"/api/v1/admin/apply-faq", "/admin/apply-faq", "/api/v1/admin/apply-faq/", "/admin/apply-faq/"})
    public ResponseEntity<List<ApplyFaqDto>> getAllFaqs() {
        log.info("Admin listando todas as perguntas de FAQ de candidatura");
        return ResponseEntity.ok(applyFaqService.getAllFaqs());
    }

    @PostMapping({"/api/v1/admin/apply-faq", "/admin/apply-faq", "/api/v1/admin/apply-faq/", "/admin/apply-faq/"})
    @com.wbscouting.api.security.audit.AuditAction(action = "CREATE", resource = "APPLY_FAQ", description = "Criação de pergunta de FAQ")
    public ResponseEntity<ApplyFaqDto> createFaq(@Valid @RequestBody ApplyFaqCreateUpdateDto dto) {
        log.info("Admin cadastrando nova pergunta de FAQ");
        ApplyFaqDto created = applyFaqService.createFaq(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 3. Endpoint para salvar/sincronizar a lista completa em lote via PUT na raiz (Resolve o 405)
    @PutMapping({"/api/v1/admin/apply-faq", "/admin/apply-faq", "/api/v1/admin/apply-faq/", "/admin/apply-faq/"})
    @com.wbscouting.api.security.audit.AuditAction(action = "UPDATE_ALL", resource = "APPLY_FAQ", description = "Atualização de lista completa de FAQs")
    public ResponseEntity<List<ApplyFaqDto>> saveAllBatch(@RequestBody Object payload) {
        log.info("Admin salvando/sincronizando lista completa de FAQ em lote via PUT");
        List<ApplyFaqDto> updatedList = applyFaqService.saveBatch(payload);
        return ResponseEntity.ok(updatedList);
    }

    @PutMapping({"/api/v1/admin/apply-faq/{id}", "/admin/apply-faq/{id}"})
    @com.wbscouting.api.security.audit.AuditAction(action = "UPDATE", resource = "APPLY_FAQ", description = "Atualização de pergunta de FAQ")
    public ResponseEntity<ApplyFaqDto> updateFaq(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ApplyFaqCreateUpdateDto dto) {
        log.info("Admin atualizando pergunta de FAQ com ID {}", id);
        return ResponseEntity.ok(applyFaqService.updateFaq(id, dto));
    }

    @PatchMapping({"/api/v1/admin/apply-faq/{id}/status", "/admin/apply-faq/{id}/status"})
    @com.wbscouting.api.security.audit.AuditAction(action = "TOGGLE_STATUS", resource = "APPLY_FAQ", description = "Alternância de status de pergunta de FAQ")
    public ResponseEntity<ApplyFaqDto> toggleStatus(@PathVariable("id") UUID id) {
        log.info("Admin alternando status da pergunta de FAQ com ID {}", id);
        return ResponseEntity.ok(applyFaqService.toggleStatus(id));
    }

    @PatchMapping({"/api/v1/admin/apply-faq/reorder", "/admin/apply-faq/reorder"})
    @com.wbscouting.api.security.audit.AuditAction(action = "REORDER", resource = "APPLY_FAQ", description = "Reordenação de itens de FAQ")
    public ResponseEntity<Void> reorderFaqs(@RequestBody ApplyFaqReorderDto dto) {
        log.info("Admin reordenando itens de FAQ");
        applyFaqService.reorderFaqs(dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping({"/api/v1/admin/apply-faq/{id}", "/admin/apply-faq/{id}"})
    @com.wbscouting.api.security.audit.AuditAction(action = "DELETE", resource = "APPLY_FAQ", description = "Exclusão permanente de pergunta de FAQ")
    public ResponseEntity<Void> deleteFaq(@PathVariable("id") UUID id) {
        log.info("Admin excluindo pergunta de FAQ com ID {}", id);
        applyFaqService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }

    // Institutional Header
    @GetMapping({"/api/v1/admin/institutional/apply-header", "/admin/institutional/apply-header"})
    public ResponseEntity<ApplyHeaderDto> getApplyHeader() {
        log.info("Admin consultando cabeçalho institucional de candidatura");
        return ResponseEntity.ok(applyFaqService.getAdminApplyHeader());
    }

    @PutMapping({"/api/v1/admin/institutional/apply-header", "/admin/institutional/apply-header"})
    @com.wbscouting.api.security.audit.AuditAction(action = "UPDATE", resource = "APPLY_HEADER", description = "Atualização de cabeçalho da página Quero Ser Modelo")
    public ResponseEntity<ApplyHeaderDto> updateApplyHeader(@RequestBody ApplyHeaderDto dto) {
        log.info("Admin atualizando cabeçalho institucional de candidatura");
        return ResponseEntity.ok(applyFaqService.updateApplyHeader(dto));
    }
}
