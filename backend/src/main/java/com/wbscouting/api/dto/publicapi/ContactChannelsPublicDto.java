package com.wbscouting.api.dto.publicapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactChannelsPublicDto {

    private String email;
    private String whatsappNumber;
    private String whatsappUrl;
    private String instagramHandle;
    private String address;
    private String officeHours;
}
