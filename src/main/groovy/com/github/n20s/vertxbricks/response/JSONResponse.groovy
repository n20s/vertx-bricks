package com.github.n20s.vertxbricks.response

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

/**
 * Simple JSON response creator with header content.
 * Based on vert.x 3.4
 *
 * https://github.com/n20s/vertx-bricks
 * Open Source MIT License (X11), see LICENSE.txt
 *
 * @author Nils Mitoussis
 * @version 1.0
 */
class JSONResponse {

    private RoutingContext context
    private Map content

    String jsonResponseBody

    JSONResponse(RoutingContext context, Map content) {
        this.context = context
        this.content = content
    }

    public void sendJsonResponse(int statusCode) {

        context.response().putHeader('Content-Type', 'application/json')
        context.response().setStatusCode(statusCode).end(getJsonResponseBody())
    }

    public String getJsonResponseBody() {

        if (jsonResponseBody == null) {
            jsonResponseBody = createJsonResponseBody()
        }
        return jsonResponseBody
    }

    private String createJsonResponseBody() {

        String buildVersion = context.get(RequestInformationHandler.CONTEXT_BUILDVERSION)
        String requestId = context.get(RequestInformationHandler.CONTEXT_REQUESTID)
        Date requestTime = context.get(RequestInformationHandler.CONTEXT_REQUESTTIME)

        Integer durationMs
        if (requestTime) {
            durationMs = new Date().time - requestTime.time
        }

        // Generate output

        Map header = [:]
        if (buildVersion) {
            context.response().headers().add('X-Verticle-Version', buildVersion as String)
            header.put("version", buildVersion as String)
        }
        if (requestId) {
            context.response().headers().add('X-Verticle-Event', requestId as String)
            header.put("event", requestId as String)
        }
        if (durationMs != null) {
            context.response().headers().add('X-Verticle-ProcessingTimeMs', "${durationMs}" as String)
            header.put("processingTimeMs", "${durationMs}" as String)
        }

        Map response = [:]
        response.put("header", header)
        response.putAll(content)

        return new JsonObject(response).encodePrettily()
    }

}
