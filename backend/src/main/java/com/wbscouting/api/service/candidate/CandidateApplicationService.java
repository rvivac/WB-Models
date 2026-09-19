package com.wbscouting.api.service.candidate;

import com.wbscouting.api.dto.candidate.CandidateApplyRequestDto;
import com.wbscouting.api.dto.candidate.CandidateApplyResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CandidateApplicationService {

    CandidateApplyResponseDto apply(CandidateApplyRequestDto requestDto, List<MultipartFile> photos);
}
