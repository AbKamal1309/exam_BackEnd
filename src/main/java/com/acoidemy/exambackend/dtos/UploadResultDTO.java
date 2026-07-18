package com.acoidemy.exambackend.dtos;

import com.acoidemy.exambackend.enums.AttachmentType;

public class UploadResultDTO {

    private String url;
    private AttachmentType type;
    private String name;

    public UploadResultDTO() {}

    public UploadResultDTO(String url, AttachmentType type, String name) {
        this.url = url;
        this.type = type;
        this.name = name;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public AttachmentType getType() { return type; }
    public void setType(AttachmentType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}