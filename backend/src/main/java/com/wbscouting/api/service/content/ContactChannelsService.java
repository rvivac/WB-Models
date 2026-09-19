package com.wbscouting.api.service.content;

import com.wbscouting.api.dto.admin.contact.ContactChannelsUpdateRequestDto;
import com.wbscouting.api.dto.publicapi.ContactChannelsPublicDto;

import java.util.UUID;

public interface ContactChannelsService {

    ContactChannelsPublicDto getContactChannels(String lang);

    ContactChannelsPublicDto updateContactChannels(ContactChannelsUpdateRequestDto dto, UUID adminId);
}
