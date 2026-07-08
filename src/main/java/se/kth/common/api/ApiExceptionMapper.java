package se.kth.common.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.security.UnauthorizedException;
import se.kth.common.exceptions.DatasetAlreadyExistsException;
import se.kth.common.exceptions.DatasetCreationException;
import se.kth.common.exceptions.DatasetDeletionException;
import se.kth.common.exceptions.DatasetInsertionException;

import java.util.NoSuchElementException;

@Slf4j
public class ApiExceptionMapper {

    @ServerExceptionMapper
    public Response datasetAlreadyExists(DatasetAlreadyExistsException e) {
        log.error("Dataset already exists:", e);
        return error(Response.Status.CONFLICT, "DATASET_ALREADY_EXISTS", e.getMessage());
    }

    @ServerExceptionMapper
    public Response notFound(NoSuchElementException e) {
        log.error("Not Found:", e);
        return error(Response.Status.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ServerExceptionMapper
    public Response badRequest(IllegalArgumentException e) {
        log.error("Bad Request:", e);
        return error(Response.Status.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    @ServerExceptionMapper
    public Response unauthorized(UnauthorizedException e) {
        log.error("Unauthorized:", e);
        return error(Response.Status.UNAUTHORIZED, "UNAUTHORIZED", e.getMessage());
    }

    @ServerExceptionMapper
    public Response datasetCreation(DatasetCreationException e) {
        log.error("Dataset Creation:", e);
        return error(
                Response.Status.BAD_REQUEST,
                "DATASET_CREATION_FAILED",
                readableMessage(e));
    }

    @ServerExceptionMapper
    public Response datasetInsertion(DatasetInsertionException e) {
        log.error("Dataset Insertion:", e);
        return error(
                Response.Status.BAD_REQUEST,
                "DATASET_INSERTION_FAILED",
                readableMessage(e));
    }

    @ServerExceptionMapper
    public Response datasetDeletion(DatasetDeletionException e) {
        log.error("Dataset Deletion:", e);
        return error(
                Response.Status.BAD_REQUEST,
                "DATASET_DELETION_FAILED",
                readableMessage(e));
    }

    @ServerExceptionMapper
    public Response upstreamHttp(WebApplicationException e) {
        log.error("Upstream HTTP:", e);
        int status = e.getResponse().getStatus();

        return Response.status(status)
                .entity(new ErrorResponse(
                        "UPSTREAM_HTTP_ERROR",
                        readableMessage(e)))
                .build();
    }

    @ServerExceptionMapper
    public Response unexpected(Throwable e) {
        log.error("Unexpected:", e);

        return error(
                Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Something went wrong");
    }

    private static Response error(Response.Status status, String code, String message) {

        return Response.status(status)
                .entity(new ErrorResponse(code, message))
                .build();
    }

    private static String readableMessage(Throwable e) {
        Throwable current = e;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage() != null
                ? current.getMessage()
                : e.getMessage();
    }
}