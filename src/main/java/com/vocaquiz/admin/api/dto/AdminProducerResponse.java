package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.service.ProducerSummary;

public record AdminProducerResponse(
    Long id,
    String name
) {

    public static AdminProducerResponse from(ProducerSummary summary) {
        return new AdminProducerResponse(summary.id(), summary.name());
    }
}
