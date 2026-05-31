package com.emailauto.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class TemplateService {
    private static final String EMAIL_WRAPPER_START =
            "<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6;color:#202124;word-break:break-word;overflow-wrap:anywhere;\">";
    private static final String EMAIL_WRAPPER_END = "</div>";

    public String renderSubject(String template, Contact contact) {
        return replacePlaceholders(template, contact.placeholders());
    }

    public String renderBody(String template, Contact contact) {
        String rendered = normalizeEditorHtml(replacePlaceholders(template, contact.placeholders()));
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

    private String normalizeEditorHtml(String html) {
        if (html == null) {
            return "";
        }
        String normalized = html.trim()
                .replaceAll(">\\s+<", "><")
                .replace("<div><br></div>", "<p><br></p>")
                .replace("<div>", "<p style=\"margin:0 0 14px;\">")
                .replace("</div>", "</p>")
                .replace("<p>", "<p style=\"margin:0 0 14px;\">")
                .replace("<ul>", "<ul style=\"margin:0 0 14px 22px;padding:0;\">")
                .replace("<ol>", "<ol style=\"margin:0 0 14px 22px;padding:0;\">")
                .replace("<li>", "<li style=\"margin:0 0 6px;\">");
        if (!normalized.contains("<p") && !normalized.contains("<ul") && !normalized.contains("<ol")) {
            normalized = "<p style=\"margin:0;\">" + normalized + "</p>";
        }
        return normalized;
    }
}
