package com.wbscouting.api.service.content;

import com.wbscouting.api.dto.ApplyFaqCreateUpdateDto;
import com.wbscouting.api.dto.ApplyFaqDto;
import com.wbscouting.api.dto.ApplyFaqReorderDto;
import com.wbscouting.api.dto.ApplyHeaderDto;
import com.wbscouting.api.entity.ApplyFaq;
import com.wbscouting.api.entity.InstitutionalSetting;
import com.wbscouting.api.exception.ResourceNotFoundException;
import com.wbscouting.api.repository.ApplyFaqRepository;
import com.wbscouting.api.repository.InstitutionalSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyFaqServiceImpl implements ApplyFaqService {

    public static final String APPLY_HEADER_KEY = "APPLY_HEADER";
    public static final String DEFAULT_TITLE = "QUERO SER MODELO";
    public static final String DEFAULT_SUBTITLE = "WB SCOUTING DESK";
    public static final String DEFAULT_DESCRIPTION = "Se você deseja fazer parte do casting da WB Agency, atenção para as informações abaixo: preencha o formulário e envie suas fotos para realizarmos a avaliação digital.";

    private final ApplyFaqRepository applyFaqRepository;
    private final InstitutionalSettingRepository institutionalSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApplyFaqDto> getPublicActiveFaqs() {
        try {
            List<ApplyFaq> faqs = applyFaqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            if (!faqs.isEmpty()) {
                return faqs.stream().map(this::toDto).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar FAQs do banco de dados, ativando fallback institucional: {}", e.getMessage());
        }
        return getDefaultFaqs();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyHeaderDto getPublicApplyHeader() {
        try {
            return institutionalSettingRepository.findBySettingKey(APPLY_HEADER_KEY)
                    .map(s -> ApplyHeaderDto.builder()
                            .title(s.getTitle() != null && !s.getTitle().isBlank() ? s.getTitle() : DEFAULT_TITLE)
                            .subtitle(s.getSubtitle() != null && !s.getSubtitle().isBlank() ? s.getSubtitle() : DEFAULT_SUBTITLE)
                            .description(s.getDescription() != null && !s.getDescription().isBlank() ? s.getDescription() : DEFAULT_DESCRIPTION)
                            .build())
                    .orElseGet(this::getDefaultHeader);
        } catch (Exception e) {
            log.warn("Erro ao buscar cabeçalho de candidatura no banco de dados, ativando fallback: {}", e.getMessage());
            return getDefaultHeader();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplyFaqDto> getAllFaqs() {
        List<ApplyFaq> faqs = applyFaqRepository.findAllByOrderByDisplayOrderAsc();
        if (faqs.isEmpty()) {
            return getDefaultFaqs();
        }
        return faqs.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApplyFaqDto createFaq(ApplyFaqCreateUpdateDto dto) {
        int nextOrder = 0;
        if (dto.getDisplayOrder() != null) {
            nextOrder = dto.getDisplayOrder();
        } else {
            List<ApplyFaq> all = applyFaqRepository.findAllByOrderByDisplayOrderAsc();
            nextOrder = all.stream().mapToInt(ApplyFaq::getDisplayOrder).max().orElse(-1) + 1;
        }

        ApplyFaq faq = ApplyFaq.builder()
                .question(dto.getQuestion().trim())
                .answer(dto.getAnswer().trim())
                .displayOrder(nextOrder)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        ApplyFaq saved = applyFaqRepository.save(faq);
        log.info("Nova pergunta de FAQ criada com ID {}", saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ApplyFaqDto updateFaq(UUID id, ApplyFaqCreateUpdateDto dto) {
        ApplyFaq faq = applyFaqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta de FAQ não encontrada com ID: " + id));

        faq.setQuestion(dto.getQuestion().trim());
        faq.setAnswer(dto.getAnswer().trim());
        if (dto.getDisplayOrder() != null) {
            faq.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getIsActive() != null) {
            faq.setIsActive(dto.getIsActive());
        }

        ApplyFaq updated = applyFaqRepository.save(faq);
        log.info("Pergunta de FAQ atualizada com ID {}", updated.getId());
        return toDto(updated);
    }

    @Override
    @Transactional
    public ApplyFaqDto toggleStatus(UUID id) {
        ApplyFaq faq = applyFaqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta de FAQ não encontrada com ID: " + id));

        faq.setIsActive(!Boolean.TRUE.equals(faq.getIsActive()));
        ApplyFaq updated = applyFaqRepository.save(faq);
        log.info("Status da pergunta de FAQ {} alternado para {}", id, updated.getIsActive());
        return toDto(updated);
    }

    @Override
    @Transactional
    public void reorderFaqs(ApplyFaqReorderDto reorderDto) {
        if (reorderDto == null || reorderDto.getItems() == null || reorderDto.getItems().isEmpty()) {
            return;
        }

        for (ApplyFaqReorderDto.ReorderItem item : reorderDto.getItems()) {
            if (item.getId() != null) {
                applyFaqRepository.findById(item.getId()).ifPresent(faq -> {
                    faq.setDisplayOrder(item.getDisplayOrder() != null ? item.getDisplayOrder() : 0);
                    applyFaqRepository.save(faq);
                });
            }
        }
        log.info("Reordenação de {} itens de FAQ concluída", reorderDto.getItems().size());
    }

    @Override
    @Transactional
    public void deleteFaq(UUID id) {
        if (!applyFaqRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pergunta de FAQ não encontrada com ID: " + id);
        }
        applyFaqRepository.deleteById(id);
        log.info("Pergunta de FAQ excluída com ID {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyHeaderDto getAdminApplyHeader() {
        return getPublicApplyHeader();
    }

    @Override
    @Transactional
    public ApplyHeaderDto updateApplyHeader(ApplyHeaderDto dto) {
        InstitutionalSetting setting = institutionalSettingRepository.findBySettingKey(APPLY_HEADER_KEY)
                .orElseGet(() -> InstitutionalSetting.builder()
                        .settingKey(APPLY_HEADER_KEY)
                        .build());

        setting.setTitle(dto.getTitle() != null && !dto.getTitle().isBlank() ? dto.getTitle().trim() : DEFAULT_TITLE);
        setting.setSubtitle(dto.getSubtitle() != null && !dto.getSubtitle().isBlank() ? dto.getSubtitle().trim() : DEFAULT_SUBTITLE);
        setting.setDescription(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription().trim() : DEFAULT_DESCRIPTION);

        InstitutionalSetting saved = institutionalSettingRepository.save(setting);
        log.info("Cabeçalho de candidatura institucional atualizado com sucesso");

        return ApplyHeaderDto.builder()
                .title(saved.getTitle())
                .subtitle(saved.getSubtitle())
                .description(saved.getDescription())
                .build();
    }

    private ApplyHeaderDto getDefaultHeader() {
        return ApplyHeaderDto.builder()
                .title(DEFAULT_TITLE)
                .subtitle(DEFAULT_SUBTITLE)
                .description(DEFAULT_DESCRIPTION)
                .build();
    }

    private List<ApplyFaqDto> getDefaultFaqs() {
        List<ApplyFaqDto> list = new ArrayList<>();
        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .question("Existe algum custo para inscrição ou avaliação?")
                .answer("Não. A WB Agency nunca cobra nenhuma taxa para inscrição, avaliação de perfil, teste de vídeo ou agenciamento inicial. O processo de scouting é 100% gratuito e desconfie de qualquer abordagem cobrando taxas em nosso nome.")
                .displayOrder(0)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .question("Como devem ser as polaroids e fotos enviadas?")
                .answer("As fotos devem ser o mais naturais possível: com boa luz natural (dia), fundo neutro (parede lisa), sem maquiagem pesada, sem filtros de redes sociais, sem óculos escuros e sem bonés ou acessórios cobrindo o rosto. Recomenda-se roupas básicas de tons neutros.")
                .displayOrder(1)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .question("Menores de 18 anos podem se cadastrar?")
                .answer("Sim, a WB Agency trabalha com formação e desenvolvimento de novos talentos a partir dos 13 anos. Para candidatos menores de 18 anos, é estritamente obrigatório o consentimento e preenchimento dos dados do responsável legal (nome completo, CPF, telefone e e-mail).")
                .displayOrder(2)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
                .question("Como e quando saberei o resultado da avaliação?")
                .answer("Nossa banca de diretores de casting analisa todos os dossiês enviados. Devido ao alto volume de inscrições nacionais, entramos em contato em até 5 dias úteis caso o seu perfil atenda às demandas atuais de campanhas e clientes da agência.")
                .displayOrder(3)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        return list;
    }

    private ApplyFaqDto toDto(ApplyFaq faq) {
        return ApplyFaqDto.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .displayOrder(faq.getDisplayOrder())
                .isActive(faq.getIsActive())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
