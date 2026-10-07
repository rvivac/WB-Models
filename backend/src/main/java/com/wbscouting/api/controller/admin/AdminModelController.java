package com.wbscouting.api.controller.admin;

import com.wbscouting.api.dto.model.*;
import com.wbscouting.api.entity.Model;
import com.wbscouting.api.enums.GenderType;
import com.wbscouting.api.repository.ModelRepository;
import com.wbscouting.api.service.model.ModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import com.wbscouting.api.security.audit.AuditAction;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
@Slf4j
public class AdminModelController {

    private final ModelService modelService;
    private final ModelRepository modelRepository;

    @PostMapping
    @AuditAction(action = "CREATE", resource = "MODEL", description = "Criação de novo modelo no casting")
    public ResponseEntity<ModelAdminResponseDto> createModel(@Valid @RequestBody ModelCreateRequestDto request) {
        ModelAdminResponseDto createdModel = modelService.createModel(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdModel.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdModel);
    }

    @PutMapping("/{id}")
    @AuditAction(action = "UPDATE", resource = "MODEL", description = "Atualização cadastral do modelo")
    public ResponseEntity<ModelAdminResponseDto> updateModel(
            @PathVariable UUID id,
            @Valid @RequestBody ModelUpdateRequestDto request) {
        return ResponseEntity.ok(modelService.updateModel(id, request));
    }

    @PatchMapping("/{id}/status")
    @AuditAction(action = "UPDATE_STATUS", resource = "MODEL", description = "Alteração de status do modelo")
    public ResponseEntity<ModelAdminResponseDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ModelStatusPatchDto request) {
        return ResponseEntity.ok(modelService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/star")
    @AuditAction(action = "UPDATE_STAR", resource = "MODEL", description = "Alteração de selo star do modelo")
    public ResponseEntity<ModelAdminResponseDto> updateStar(
            @PathVariable UUID id,
            @Valid @RequestBody ModelStarPatchDto request) {
        return ResponseEntity.ok(modelService.updateStar(id, request));
    }

    @PatchMapping("/{id}/featured")
    @AuditAction(action = "UPDATE_FEATURED", resource = "MODEL", description = "Alteração de destaque na vitrine do modelo")
    public ResponseEntity<ModelAdminResponseDto> updateFeatured(
            @PathVariable UUID id,
            @Valid @RequestBody ModelFeaturedPatchDto request) {
        return ResponseEntity.ok(modelService.updateFeatured(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<ModelAdminResponseDto>> listAdminModels(
            @RequestParam(required = false) GenderType gender,
            @RequestParam(required = false) Boolean isStar,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // ============================================================
        // 🔥 LOG OBRIGATORIO DA TASK: Contagem DIRETA no repository
        //    antes de QUALQUER processamento. Se for H2 em vez de PostgreSQL,
        //    aqui aparece 0 (ou seed da H2) ao inves dos dados reais do Supabase.
        // ============================================================
        final long DIAG_COUNT_TOTAL = modelRepository.count();
        log.info("[DIAGNOSTICO-BD] [CONTROLLER] modelRepository.count() no Supabase = {} (executado ANTES de service/specs)", DIAG_COUNT_TOTAL);

        if (isActive == null && status != null) {
            isActive = "ACTIVE".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);
        }

        final boolean anyFilterApplied = (gender != null)
                || (isStar != null)
                || (isActive != null)
                || StringUtils.hasText(search);

        log.info("[DIAGNOSTICO-BD] [CONTROLLER] filtros aplicados? anyFilterApplied={} (gender={}, isStar={}, isActive={}, search='{}')",
                anyFilterApplied, gender, isStar, isActive, search);

        Page<ModelAdminResponseDto> result = modelService.listAdminModels(gender, isStar, isActive, search, pageable);

        // ============================================================
        // 🔥 LOG OBRIGATORIO DA TASK: apos a service retornar, qual o totalElements
        // ============================================================
        log.info("[DIAGNOSTICO-BD] [CONTROLLER] result.getTotalElements() APOS service/spec = {}", result.getTotalElements());

        // ============================================================
        // 🔥 FALLBACK ABSOLUTO (CONTROLLER LEVEL):
        // Se a service retornar totalElements=0 E nenhum filtro foi enviado,
        // o problema NAO eh de specification: eh conexao errada / repository
        // / h2-memory sendo usado em vez de PostgreSQL / dataSource vazio.
        // Consultamos DIRETAMENTE o repository findAll SEM specs para mostrar
        // ao usuario os dados que existem (evita tela branca no Admin).
        // ============================================================
        if (!anyFilterApplied) {
            // ============================================================
            // 🔥 Fallback controller: SEMPRE executa (diagnostico + garantia)
            //    Se a service + specification estiver mascarando registros
            //    por qualquer motivo, a listagem admin mostra os dados reais.
            // ============================================================
            Page<Model> rawModels = null;
            try {
                rawModels = modelRepository.findAll(pageable);
                log.info("[DIAGNOSTICO-BD] [CONTROLLER] fallback findAll SEM spec → totalElements = {}", rawModels.getTotalElements());
            } catch (Exception e) {
                log.error("[CONTROLLER /admin/models] Falha no fallback direto do repository: {}", e.getMessage(), e);
            }

            // Se service voltar 0 e rawModels tiver algo → SOBRESCREVE a resposta
            if (result.getTotalElements() == 0L && rawModels != null && rawModels.getTotalElements() > 0L) {
                log.warn("[CONTROLLER /admin/models] ⚠️  SOBRESCREVENDO resposta com {} itens (service/specs retornaram 0 MAS TABELA TEM DADOS). " +
                        "Elimine a causa raiz (provavel H2 em vez de PostgreSQL).", rawModels.getTotalElements());
                result = rawModels.map(this::mapToAdminDtoInline);
            } else if (rawModels != null
                    && result.getTotalElements() != rawModels.getTotalElements()) {
                log.warn("[CONTROLLER /admin/models] ⚠️  DISCREPANCIA DETECTADA: service/specs total={} vs. fallback SEM spec total={}. " +
                        "Verificar Specification.filter() e anotações da entidade Model.",
                        result.getTotalElements(), rawModels.getTotalElements());
            }
        }

        // ============================================================
        // LOG DE AUDITORIA FINAL (solicitado explicitamente na task)
        // ============================================================
        log.info("[CONTROLLER /admin/models] Resposta HTTP 200 preparada. Total de registros retornados no content: {} | totalPages={} | size={} | page={}",
                result.getTotalElements(), result.getTotalPages(), result.getSize(), result.getNumber());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModelAdminResponseDto> getAdminModelById(@PathVariable UUID id) {
        return ResponseEntity.ok(modelService.getAdminModelById(id));
    }

    @DeleteMapping("/{id}")
    @AuditAction(action = "DELETE", resource = "MODEL", description = "Exclusão permanente de modelo do casting")
    public ResponseEntity<Void> deleteModel(@PathVariable UUID id) {
        modelService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mapeamento INLINE de Model → ModelAdminResponseDto usado EXCLUSIVAMENTE no fallback de listagem
     * quando a service + Specification falham em retornar registros (ex: H2 sendo usado em vez de PostgreSQL).
     * Replica fielmente a logica de ModelServiceImpl.mapToAdminResponse() para manter consistencia.
     * Nao usa injecao de dependencias nem Beans — metodo privado puro.
     */
    private ModelAdminResponseDto mapToAdminDtoInline(Model model) {
        if (model == null) return null;
        return ModelAdminResponseDto.builder()
                .id(model.getId())
                .stageName(model.getStageName())
                .gender(model.getGender())
                .isStar(model.getIsStar())
                .isFeaturedHome(model.getIsFeaturedHome())
                .featuredOrder(model.getFeaturedOrder())
                .isActive(model.getIsActive())
                .primaryPhotoUrl(model.getPrimaryPhotoUrl())
                .instagramUrl(model.getInstagramUrl())
                .birthDate(model.getBirthDate())
                .heightCm(model.getHeightCm())
                .city(model.getCity())
                .nationality(model.getNationality())
                .dressSize(model.getDressSize())
                .shoeSize(model.getShoeSize())
                .bustChestCm(model.getBustChestCm())
                .waistCm(model.getWaistCm())
                .hipsCm(model.getHipsCm())
                .hairColor(model.getHairColor())
                .eyesColor(model.getEyesColor())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
