package com.vocaquiz.admin.api.dto;

import java.util.List;

public record AdminMeResponse(String name, List<String> authorities) {}
