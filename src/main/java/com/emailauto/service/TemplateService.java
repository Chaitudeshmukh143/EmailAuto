package com.emailauto.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class TemplateService {
    public String render(String template, Contact contact) {
        return template
                .replace("{{name}}", HtmlUtils.htmlEscape(contact.name()))
                .replace("{{email}}", HtmlUtils.htmlEscape(contact.email()))
                .replace("{{company}}", HtmlUtils.htmlEscape(contact.company()));
    }
}
