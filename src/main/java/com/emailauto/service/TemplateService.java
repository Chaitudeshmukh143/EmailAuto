package com.emailauto.service;

import java.util.Arrays;
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
        String[] paragraphs = rendered.strip().split("(\\r?\\n){2,}");
        StringBuilder html = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }
            if (!html.isEmpty()) {
                html.append('\n');
            }
            html.append("<p>")
                    .append(HtmlUtils.htmlEscape(paragraph).replace("\r\n", "\n").replace("\n", "<br>"))
                    .append("</p>");
        }
        return html.isEmpty() ? "<p></p>" : html.toString();
    }

    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }
}
