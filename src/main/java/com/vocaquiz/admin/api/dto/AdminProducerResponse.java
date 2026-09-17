package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.service.ProducerSummary;

import java.util.List;

public record AdminProducerResponse(
    Long id,
    List<AdminProducerNameResponse> names
) {

    public static AdminProducerResponse from(ProducerSummary summary) {
        return new AdminProducerResponse(
            summary.id(),
            summary.names().stream().map(AdminProducerNameResponse::from).toList());
    }

    /** `ProducerSummary.ProducerNameSummary`(record)와 필드가 같다. */
    public record AdminProducerNameResponse(Long languageId, String name, boolean primary) {

        static AdminProducerNameResponse from(ProducerSummary.ProducerNameSummary nameSummary) {
            return new AdminProducerNameResponse(
                nameSummary.languageId(), nameSummary.name(), nameSummary.primary());
        }
    }
}
