package com.vocaquiz.admin.api.dto;

import java.util.List;

public record AdminMeResponse(String email, List<String> authorities) {}
