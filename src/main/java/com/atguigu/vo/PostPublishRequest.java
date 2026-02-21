package com.atguigu.vo;

import org.springframework.web.multipart.MultipartFile;

public class PostPublishRequest {

    private String textContent;
    private String imageUrl;
    private MultipartFile imageFile;
    private String codeLanguage;
    private String codeContent;
    private MultipartFile codeFile;

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public String getCodeLanguage() {
        return codeLanguage;
    }

    public void setCodeLanguage(String codeLanguage) {
        this.codeLanguage = codeLanguage;
    }

    public String getCodeContent() {
        return codeContent;
    }

    public void setCodeContent(String codeContent) {
        this.codeContent = codeContent;
    }

    public MultipartFile getCodeFile() {
        return codeFile;
    }

    public void setCodeFile(MultipartFile codeFile) {
        this.codeFile = codeFile;
    }
}
