package com.wbscouting.api.service.content;

import com.wbscouting.api.dto.ApplyFaqCreateUpdateDto;
import com.wbscouting.api.dto.ApplyFaqDto;
import com.wbscouting.api.dto.ApplyFaqReorderDto;
import com.wbscouting.api.dto.ApplyHeaderDto;

import java.util.List;
import java.util.UUID;

public interface ApplyFaqService {

    // Public
    List<ApplyFaqDto> getPublicActiveFaqs();

    List<ApplyFaqDto> getPublicActiveFaqs(String lang);

    ApplyHeaderDto getPublicApplyHeader();

    // Admin
    List<ApplyFaqDto> getAllFaqs();

    List<ApplyFaqDto> updateAllFaqs(List<ApplyFaqDto> items);

    ApplyFaqDto createFaq(ApplyFaqCreateUpdateDto dto);

    ApplyFaqDto updateFaq(UUID id, ApplyFaqCreateUpdateDto dto);

    ApplyFaqDto toggleStatus(UUID id);

    void reorderFaqs(ApplyFaqReorderDto reorderDto);

    void deleteFaq(UUID id);

    ApplyHeaderDto getAdminApplyHeader();

    ApplyHeaderDto updateApplyHeader(ApplyHeaderDto dto);
}
