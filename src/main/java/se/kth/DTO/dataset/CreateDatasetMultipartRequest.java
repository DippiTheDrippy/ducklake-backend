package se.kth.DTO.dataset;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.ws.rs.core.MediaType;

public record CreateDatasetMultipartRequest(
        @RestForm("metadata") @PartType(MediaType.APPLICATION_JSON) CreateDatasetRequest metadata,

        @RestForm("file") FileUpload file) {

}