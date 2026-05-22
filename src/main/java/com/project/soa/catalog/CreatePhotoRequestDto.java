package com.project.soa.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePhotoRequestDto {

    @NotBlank(message = "Photo URL is required")
    private String url;

    private String caption;

    @NotNull(message = "Photo type is required")
    private PhotoType type;

    private Integer displayOrder = 0;

    private Boolean isActive = true;

    public CreatePhotoRequestDto() {}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public PhotoType getType() { return type; }
    public void setType(PhotoType type) { this.type = type; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
