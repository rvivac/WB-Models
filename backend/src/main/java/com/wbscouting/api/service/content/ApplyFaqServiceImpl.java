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
    private final com.wbscouting.api.repository.SiteContentRepository siteContentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApplyFaqDto> getPublicActiveFaqs() {
        return getPublicActiveFaqs("pt");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplyFaqDto> getPublicActiveFaqs(String lang) {
        List<ApplyFaqDto> all = getAllFaqs();
        boolean isEn = lang != null && lang.trim().toLowerCase().startsWith("en");

        return all.stream()
                .filter(f -> f.getIsActive() == null || f.getIsActive())
                .map(f -> {
                    String q = isEn && f.getQuestionEn() != null && !f.getQuestionEn().isBlank()
                            ? f.getQuestionEn()
                            : f.getQuestion();
                    String a = isEn && f.getAnswerEn() != null && !f.getAnswerEn().isBlank()
                            ? f.getAnswerEn()
                            : f.getAnswer();
                    return ApplyFaqDto.builder()
                            .id(f.getId())
                            .question(q)
                            .answer(a)
                            .questionEn(f.getQuestionEn())
                            .answerEn(f.getAnswerEn())
                            .order(f.getOrder())
                            .displayOrder(f.getDisplayOrder())
                            .isActive(f.getIsActive())
                            .createdAt(f.getCreatedAt())
                            .updatedAt(f.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
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
        try {
            List<ApplyFaq> faqs = applyFaqRepository.findAllByOrderByDisplayOrderAsc();
            if (!faqs.isEmpty()) {
                return faqs.stream().map(this::toDto).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar faqs de applyFaqRepository: {}", e.getMessage());
        }

        try {
            var scOpt = siteContentRepository.findBySectionKey("SCOUTING_FAQ");
            if (scOpt.isPresent() && scOpt.get().getPayloadPt() != null) {
                Object itemsObj = scOpt.get().getPayloadPt().get("items");
                if (itemsObj instanceof List<?> list && !list.isEmpty()) {
                    List<ApplyFaqDto> dtos = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        Object obj = list.get(i);
                        if (obj instanceof Map<?, ?> map) {
                            dtos.add(fromMap(map, i));
                        }
                    }
                    if (!dtos.isEmpty()) {
                        return dtos;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar FAQs de site_contents: {}", e.getMessage());
        }

        return getDefaultFaqs();
    }

    @Override
    @Transactional
    public List<ApplyFaqDto> saveBatch(Object payload) {
        List<?> rawList = null;
        if (payload instanceof List<?> list) {
            rawList = list;
        } else if (payload instanceof Map<?, ?> map) {
            Object itemsObj = map.containsKey("items") ? map.get("items") : map;
            if (itemsObj instanceof List<?> list) {
                rawList = list;
            }
        }

        if (rawList == null) {
            rawList = Collections.emptyList();
        }

        if (rawList.size() > 10) {
            throw new IllegalArgumentException("Máximo de 10 perguntas e respostas permitidas.");
        }

        List<ApplyFaq> existingFaqs = applyFaqRepository.findAll();
        Map<UUID, ApplyFaq> existingMap = existingFaqs.stream()
                .filter(f -> f.getId() != null)
                .collect(Collectors.toMap(ApplyFaq::getId, f -> f, (a, b) -> a));

        Set<UUID> keptIds = new HashSet<>();
        List<ApplyFaq> toSave = new ArrayList<>();

        for (int i = 0; i < rawList.size(); i++) {
            Object itemObj = rawList.get(i);
            String question = "";
            String answer = "";
            String questionEn = null;
            String answerEn = null;
            Boolean isActive = true;
            String idStr = null;

            if (itemObj instanceof Map<?, ?> map) {
                idStr = map.get("id") != null ? map.get("id").toString().trim() : null;
                question = map.get("question") != null ? map.get("question").toString().trim() : "";
                answer = map.get("answer") != null ? map.get("answer").toString().trim() : "";
                questionEn = map.get("questionEn") != null ? map.get("questionEn").toString().trim()
                        : (map.get("question_en") != null ? map.get("question_en").toString().trim() : null);
                answerEn = map.get("answerEn") != null ? map.get("answerEn").toString().trim()
                        : (map.get("answer_en") != null ? map.get("answer_en").toString().trim() : null);
                if (map.get("isActive") instanceof Boolean b) {
                    isActive = b;
                }
            } else if (itemObj instanceof ApplyFaqDto dto) {
                idStr = dto.getId() != null ? dto.getId().toString() : null;
                question = dto.getQuestion() != null ? dto.getQuestion().trim() : "";
                answer = dto.getAnswer() != null ? dto.getAnswer().trim() : "";
                questionEn = dto.getQuestionEn() != null ? dto.getQuestionEn().trim() : null;
                answerEn = dto.getAnswerEn() != null ? dto.getAnswerEn().trim() : null;
                if (dto.getIsActive() != null) {
                    isActive = dto.getIsActive();
                }
            }

            UUID uuid = null;
            if (idStr != null && !idStr.isBlank()) {
                try {
                    uuid = UUID.fromString(idStr);
                } catch (Exception ignored) {}
            }

            ApplyFaq entity;
            if (uuid != null && existingMap.containsKey(uuid)) {
                // Se tiver id válido existente no banco, atualiza
                entity = existingMap.get(uuid);
                entity.setQuestion(question);
                entity.setAnswer(answer);
                entity.setQuestionEn(questionEn);
                entity.setAnswerEn(answerEn);
                entity.setDisplayOrder(i);
                entity.setIsActive(isActive);
                keptIds.add(uuid);
            } else {
                // Se for item novo (sem id ou não encontrado), cria
                entity = ApplyFaq.builder()
                        .question(question)
                        .answer(answer)
                        .questionEn(questionEn)
                        .answerEn(answerEn)
                        .displayOrder(i)
                        .isActive(isActive)
                        .build();
            }
            toSave.add(entity);
        }

        // Se algum item foi removido da lista, exclui do banco
        List<ApplyFaq> toDelete = existingFaqs.stream()
                .filter(f -> !keptIds.contains(f.getId()))
                .toList();
        if (!toDelete.isEmpty()) {
            applyFaqRepository.deleteAll(toDelete);
            log.info("Removidos {} itens de FAQ excluídos pelo administrador", toDelete.size());
        }

        List<ApplyFaq> saved = applyFaqRepository.saveAll(toSave);
        log.info("Salvos/atualizados {} itens de FAQ com displayOrder sequencial", saved.size());

        // Redundância e sincronização com site_contents (SCOUTING_FAQ)
        try {
            List<Map<String, Object>> mapListPt = new ArrayList<>();
            List<Map<String, Object>> mapListEn = new ArrayList<>();
            for (int i = 0; i < saved.size(); i++) {
                ApplyFaq f = saved.get(i);
                Map<String, Object> mapPt = new LinkedHashMap<>();
                mapPt.put("id", f.getId() != null ? f.getId().toString() : null);
                mapPt.put("order", i + 1);
                mapPt.put("displayOrder", i);
                mapPt.put("question", f.getQuestion());
                mapPt.put("answer", f.getAnswer());
                mapPt.put("questionEn", f.getQuestionEn());
                mapPt.put("answerEn", f.getAnswerEn());
                mapPt.put("isActive", f.getIsActive());
                mapListPt.add(mapPt);

                Map<String, Object> mapEn = new LinkedHashMap<>();
                mapEn.put("id", f.getId() != null ? f.getId().toString() : null);
                mapEn.put("order", i + 1);
                mapEn.put("displayOrder", i);
                String qEn = (f.getQuestionEn() != null && !f.getQuestionEn().isBlank()) ? f.getQuestionEn() : f.getQuestion();
                String aEn = (f.getAnswerEn() != null && !f.getAnswerEn().isBlank()) ? f.getAnswerEn() : f.getAnswer();
                mapEn.put("question", qEn);
                mapEn.put("answer", aEn);
                mapEn.put("questionEn", f.getQuestionEn());
                mapEn.put("answerEn", f.getAnswerEn());
                mapEn.put("isActive", f.getIsActive());
                mapListEn.add(mapEn);
            }

            var sc = siteContentRepository.findBySectionKey("SCOUTING_FAQ")
                    .orElseGet(() -> com.wbscouting.api.entity.SiteContent.builder().sectionKey("SCOUTING_FAQ").build());

            Map<String, Object> syncPayloadPt = new LinkedHashMap<>();
            syncPayloadPt.put("items", mapListPt);

            Map<String, Object> syncPayloadEn = new LinkedHashMap<>();
            syncPayloadEn.put("items", mapListEn);

            sc.setPayloadPt(syncPayloadPt);
            sc.setPayloadEn(syncPayloadEn);
            sc.setUpdatedAt(OffsetDateTime.now());
            siteContentRepository.save(sc);
        } catch (Exception ex) {
            log.warn("Erro ao sincronizar SCOUTING_FAQ em siteContentRepository: {}", ex.getMessage());
        }

        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ApplyFaqDto> updateAllFaqs(List<ApplyFaqDto> items) {
        return saveBatch(items);
    }

    private ApplyFaqDto fromMap(Map<?, ?> map, int index) {
        String q = map.get("question") != null ? map.get("question").toString() : "";
        String a = map.get("answer") != null ? map.get("answer").toString() : "";
        String qEn = map.get("questionEn") != null ? map.get("questionEn").toString() : "";
        String aEn = map.get("answerEn") != null ? map.get("answerEn").toString() : "";
        int order = map.get("order") instanceof Number n ? n.intValue() : index + 1;
        int displayOrder = map.get("displayOrder") instanceof Number n ? n.intValue() : index;
        boolean isActive = map.get("isActive") instanceof Boolean b ? b : true;

        return ApplyFaqDto.builder()
                .id(UUID.nameUUIDFromBytes(("scouting_faq_" + index).getBytes()))
                .question(q)
                .answer(a)
                .questionEn(qEn)
                .answerEn(aEn)
                .order(order)
                .displayOrder(displayOrder)
                .isActive(isActive)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
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
                .questionEn(dto.getQuestionEn() != null ? dto.getQuestionEn().trim() : null)
                .answerEn(dto.getAnswerEn() != null ? dto.getAnswerEn().trim() : null)
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
        if (dto.getQuestionEn() != null) {
            faq.setQuestionEn(dto.getQuestionEn().trim());
        }
        if (dto.getAnswerEn() != null) {
            faq.setAnswerEn(dto.getAnswerEn().trim());
        }
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
                .order(1)
                .displayOrder(0)
                .question("Quais são as medidas ideais para o mercado da moda e publicidade?")
                .answer("Não existem medidas certas ou específicas. O importante é ter personalidade marcante, atitude e querer muito ser modelo.")
                .questionEn("What are the ideal measurements for fashion and advertising?")
                .answerEn("There are no strict specific measurements. What matters most is strong personality, attitude, and dedication.")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .order(2)
                .displayOrder(1)
                .question("Existe algum custo para inscrição ou avaliação?")
                .answer("Não. A WB Agency nunca cobra nenhuma taxa para inscrição, avaliação de perfil, teste de vídeo ou agenciamento inicial. O processo de scouting é 100% gratuito e desconfie de qualquer abordagem cobrando taxas em nosso nome.")
                .questionEn("Is there any cost for application or evaluation?")
                .answerEn("No. WB Agency never charges any fees for registration, profile evaluation, video tests, or initial signing. The scouting process is 100% free; beware of anyone charging fees in our name.")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .order(3)
                .displayOrder(2)
                .question("Como devem ser as polaroids e fotos enviadas?")
                .answer("As fotos devem ser o mais naturais possível: com boa luz natural (dia), fundo neutro (parede lisa), sem maquiagem pesada, sem filtros de redes sociais, sem óculos escuros e sem bonés ou acessórios cobrindo o rosto. Recomenda-se roupas básicas de tons neutros.")
                .questionEn("How should the polaroids and submitted photos look?")
                .answerEn("Photos must be as natural as possible: shot in daylight, neutral plain background, no heavy makeup, no social media filters, no sunglasses, and no hats or accessories covering your face. Basic neutral clothing is recommended.")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
                .order(4)
                .displayOrder(3)
                .question("Menores de 18 anos podem se cadastrar?")
                .answer("Sim, a WB Agency trabalha com formação e desenvolvimento de novos talentos a partir dos 13 anos. Para candidatos menores de 18 anos, é estritamente obrigatório o consentimento e preenchimento dos dados do responsável legal (nome completo, CPF, telefone e e-mail).")
                .questionEn("Can applicants under 18 apply?")
                .answerEn("Yes, WB Agency develops new talents starting from age 13. For applicants under 18, parental or legal guardian consent and contact information are strictly mandatory.")
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        list.add(ApplyFaqDto.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000005"))
                .order(5)
                .displayOrder(4)
                .question("Como e quando saberei o resultado da avaliação?")
                .answer("Nossa banca de diretores de casting analisa todos os dossiês enviados. Devido ao alto volume de inscrições nacionais, entramos em contato em até 5 dias úteis caso o seu perfil atenda às demandas atuais de campanhas e clientes da agência.")
                .questionEn("How and when will I know the evaluation results?")
                .answerEn("Our casting directors review all submitted portfolios. Due to the high volume of applications, we will contact you within 5 business days if your profile matches our current agency demands.")
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
                .questionEn(faq.getQuestionEn() != null && !faq.getQuestionEn().isBlank() ? faq.getQuestionEn() : faq.getQuestion())
                .answerEn(faq.getAnswerEn() != null && !faq.getAnswerEn().isBlank() ? faq.getAnswerEn() : faq.getAnswer())
                .order(faq.getDisplayOrder() != null ? faq.getDisplayOrder() + 1 : 1)
                .displayOrder(faq.getDisplayOrder())
                .isActive(faq.getIsActive())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
