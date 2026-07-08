package se.kth.DTO.garage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateBucketResponse(
        String id,
        String created
) {
}