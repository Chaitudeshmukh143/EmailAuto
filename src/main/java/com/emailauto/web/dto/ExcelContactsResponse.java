package com.emailauto.web.dto;

import com.emailauto.service.Contact;
import java.util.List;

public record ExcelContactsResponse(int count, List<Contact> contacts) {
}
