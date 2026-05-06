package com.emailauto.service;

public record StoredAttachment(String fileName, String contentType, byte[] fileData) {
}
