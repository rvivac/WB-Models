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
    private java.util.List<SocialMediaItemDto> socialMediaList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialMediaItemDto {
        private String id;
        private String name;
        private String url;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public String getWhatsappUrl() { return whatsappUrl; }
    public void setWhatsappUrl(String whatsappUrl) { this.whatsappUrl = whatsappUrl; }
    public String getInstagramHandle() { return instagramHandle; }
    public void setInstagramHandle(String instagramHandle) { this.instagramHandle = instagramHandle; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getOfficeHours() { return officeHours; }
    public void setOfficeHours(String officeHours) { this.officeHours = officeHours; }
    public java.util.List<SocialMediaItemDto> getSocialMediaList() { return socialMediaList; }
    public void setSocialMediaList(java.util.List<SocialMediaItemDto> socialMediaList) { this.socialMediaList = socialMediaList; }
}
