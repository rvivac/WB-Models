package com.wbscouting.api.controller.publicapi;

import com.wbscouting.api.dto.content.ContactChannelDto;
import com.wbscouting.api.entity.ContactChannel;
import com.wbscouting.api.repository.ContactChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/contact-channels", "/contact-channels"})
@RequiredArgsConstructor
public class PublicContactChannelController {

    private final ContactChannelRepository contactChannelRepository;

    @GetMapping
    @Transactional
    public ResponseEntity<List<ContactChannelDto>> getActiveContactChannels() {
        List<ContactChannel> channels = contactChannelRepository.findByActiveTrueOrderByDisplayOrderAsc();

        // Se a tabela estiver vazia, inicializa os canais padrão reais no PostgreSQL/Supabase
        if (channels.isEmpty() && contactChannelRepository.count() == 0) {
            log.info("Inicializando canais de contato padrão no banco de dados...");
            List<ContactChannel> seed = List.of(
                    ContactChannel.builder()
                            .type("WHATSAPP")
                            .value("+55 11 97065-6003")
                            .label("WhatsApp Oficial")
                            .active(true)
                            .displayOrder(1)
                            .build(),
                    ContactChannel.builder()
                            .type("EMAIL")
                            .value("info@wbagency.com.br")
                            .label("E-mail Geral & Scouting")
                            .active(true)
                            .displayOrder(2)
                            .build(),
                    ContactChannel.builder()
                            .type("PHONE")
                            .value("+55 11 97065-6003")
                            .label("Telefone Comercial")
                            .active(true)
                            .displayOrder(3)
                            .build(),
                    ContactChannel.builder()
                            .type("INSTAGRAM")
                            .value("https://instagram.com/wbagency")
                            .label("Instagram Oficial")
                            .active(true)
                            .displayOrder(4)
                            .build(),
                    ContactChannel.builder()
                            .type("ADDRESS")
                            .value("Avenida Paulista, 1000, Cj 1402 - Bela Vista, São Paulo - SP")
                            .label("Endereço Matriz")
                            .active(true)
                            .displayOrder(5)
                            .build(),
                    ContactChannel.builder()
                            .type("OFFICE_HOURS")
                            .value("Segunda a Sexta: 09h às 18h (GMT-3)")
                            .label("Horário de Atendimento")
                            .active(true)
                            .displayOrder(6)
                            .build()
            );
            channels = contactChannelRepository.saveAllAndFlush(seed);
        }

        List<ContactChannelDto> dtos = channels.stream()
                .map(ContactChannelDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
