package com.emailauto.service;

import java.util.Map;

public record Contact(String name, String email, String company, Map<String, String> placeholders) {
}
