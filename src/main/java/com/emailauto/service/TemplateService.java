package com.emailauto.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class TemplateService {
    private static final String EMAIL_WRAPPER_START =
            "<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:#202124;white-space:pre-wrap;word-break:break-word;\">";
    private static final String EMAIL_WRAPPER_END = "</div>";

    public String renderSubject(String template, Contact contact) {
        return replacePlaceholders(template, contact.placeholders());
    }

    public String renderBody(String template, Contact contact) {
        String rendered = replacePlaceholders(template, contact.placeholders());
        if (rendered == null || rendered.isBlank()) {
            return EMAIL_WRAPPER_START + "<p></p>" + EMAIL_WRAPPER_END;
        }
        return EMAIL_WRAPPER_START + rendered + EMAIL_WRAPPER_END;
    }

    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", HtmlUtils.htmlEscape(entry.getValue() == null ? "" : entry.getValue()));
        }
        return rendered;
    }
}
