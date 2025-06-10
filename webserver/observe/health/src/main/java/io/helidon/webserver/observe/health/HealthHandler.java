/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.webserver.observe.health;

import java.util.List;

import io.helidon.health.HealthCheck;
import io.helidon.health.HealthCheckResponse;
import io.helidon.health.HealthCheckType;
import io.helidon.http.HeaderValues;
import io.helidon.http.HtmlEncoder;
import io.helidon.http.Status;
import io.helidon.http.media.EntityWriter;
import io.helidon.http.media.jsonp.JsonpSupport;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

class HealthHandler implements Handler {
    private static final System.Logger LOGGER = System.getLogger(HealthHandler.class.getName());

    private final EntityWriter<JsonObject> entityWriter;
    private final boolean details;
    private final HealthCheckType healthCheckType;
    private final io.helidon.health.HealthService healthService;

    HealthHandler(EntityWriter<JsonObject> entityWriter,
                  boolean details,
                  HealthCheckType healthCheckType,
                  io.helidon.health.HealthService healthService) {
        this.entityWriter = entityWriter;
        this.details = details;
        this.healthCheckType = healthCheckType;
        this.healthService = healthService;
    }

    @Override
    public void handle(ServerRequest req, ServerResponse res) {
        List<HealthCheckResponse> responses = (
                (healthCheckType == null)
                        ? healthService.checkHealth()
                        : healthService.checkHealth(healthCheckType))
                .stream()
                .map(HealthHandler::encodeAnyErrorMessageDetail)
                .toList();

        HealthCheckResponse.Status status = responses.stream()
                .map(HealthCheckResponse::status)
                .reduce(HealthCheckResponse.Status::poorer)
                .orElse(HealthCheckResponse.Status.UP);

        res.status(httpStatus(details, status));
        res.header(HeaderValues.CACHE_NO_CACHE)
                .header(HeaderValues.X_CONTENT_TYPE_OPTIONS_NOSNIFF);

        if (details) {
            entityWriter.write(JsonpSupport.JSON_OBJECT_TYPE,
                               toJson(status, responses),
                               res.outputStream(),
                               req.headers(),
                               res.headers());

        } else {
            res.send();
        }
    }

    /**
     * Returns a response with any error message in the details HTML-encoded.
     *
     * @param response original response
     * @return a response equivalent to the original with any error message HTML-encoded
     */
    static HealthCheckResponse encodeAnyErrorMessageDetail(HealthCheckResponse response) {

        if (response.status() != HealthCheckResponse.Status.ERROR) {
            return response;
        }
        return HealthCheckResponse.builder()
                .name(response.name())
                .status(response.status())
                .update(b -> response.details().forEach((k, v) ->
                                                                b.detail(k,
                                                                         k.equals("message") && v instanceof String vs
                                                                                 ? HtmlEncoder.encode(vs)
                                                                                 : v)))
                .build();
    }

    static Status httpStatus(boolean details, HealthCheckResponse.Status status) {
        return switch (status) {
            case UP -> details ? Status.OK_200 : Status.NO_CONTENT_204;
            case DOWN -> Status.SERVICE_UNAVAILABLE_503;
            case ERROR -> Status.INTERNAL_SERVER_ERROR_500;
        };
    }

    private static JsonObject toJson(HealthCheckResponse.Status status, List<HealthCheckResponse> responses) {
        JsonObjectBuilder response = HealthHelper.JSON.createObjectBuilder();
        response.add("status", status.toString());

        JsonArrayBuilder checks = HealthHelper.JSON.createArrayBuilder();
        responses.forEach(result -> checks.add(HealthHelper.toJson(result)));

        response.add("checks", checks);
        return response.build();
    }
}
