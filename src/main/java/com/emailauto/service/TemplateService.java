package com.emailauto.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class TemplateService {
    public String renderSubject(String template, Contact contact) {
        return replacePlaceholders(template, contact.placeholders());
    }

    public String renderBody(String template, Contact contact) {
        String rendered = replacePlaceholders(template, contact.placeholders());
        return rendered == null || rendered.isBlank() ? "<p></p>" : rendered;
    }

    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", HtmlUtils.htmlEscape(entry.getValue() == null ? "" : entry.getValue()));
        }
        return rendered;
    }
}
