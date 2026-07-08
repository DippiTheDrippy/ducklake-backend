package se.kth.common.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ErrorResponse")
public record ErrorResponse(
                @Schema(examples = "DATASET_CREATION_FAILED") String code,

                @Schema(examples = "Could not create DuckLake table from uploaded file") String message) {
}