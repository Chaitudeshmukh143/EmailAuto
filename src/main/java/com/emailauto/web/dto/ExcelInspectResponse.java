package com.emailauto.web.dto;

import java.util.List;

public record ExcelInspectResponse(List<String> headers, List<String> placeholders, int rowCount) {
}
