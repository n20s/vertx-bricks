package com.github.n20s.vertxbricks.response

import io.vertx.core.Handler
import io.vertx.core.MultiMap
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Handles basic information of the request, provides information to the RoutingContext and performs request logging.
 *
 * <code>
 * router.route().handler(new RequestInformationHandler().setBuildVersion(buildVersion).setBasePath(basePath))
 * </code>
 *
 * Based on vert.x 3.4
 *
 * https://github.com/n20s/vertx-bricks
 * Open Source MIT License (X11), see LICENSE.txt
 *
 * @author Nils Mitoussis
 * @version 1.0
 */
class RequestInformationHandler implements Handler<RoutingContext> {

    static Logger log = LoggerFactory.getLogger(RequestInformationHandler.class)

    public static String CONTEXT_BASEPATH = "cBasePath"
    public static String CONTEXT_BUILDVERSION = "cBuildVersion"
    public static String CONTEXT_REQUESTID = "cRequestID"
    public static String CONTEXT_REQUESTTIME = "cRequestTime"

    private String ORIGIN_EVENT_HEADER = "X-Upstream-Origin-Event"
    private boolean ENABLE_ORIGIN_EVENTID_DEFAULT = false

    private String buildVersion
    private String basePath
    private boolean enableOriginEventId = ENABLE_ORIGIN_EVENTID_DEFAULT

    RequestInformationHandler setBuildVersion(String buildVersion) {
        this.buildVersion = buildVersion
        return this
    }

    RequestInformationHandler setBasePath(String basePath) {
        this.basePath = basePath
        return this
    }

    RequestInformationHandler setEnableOriginEventId(boolean enableOriginEventId) {
        this.enableOriginEventId = enableOriginEventId
        return this
    }

    @Override
    void handle(RoutingContext context) {

        context.put(CONTEXT_BASEPATH, basePath)
        if (buildVersion) {
            context.put(CONTEXT_BUILDVERSION, buildVersion)
        }

        // Determine request id/event id
        // (8 characters)
        String localRequestId = UUID.randomUUID().toString().substring(0,8)
        String requestId = "undefined"
        if (enableOriginEventId == false) {
            requestId = localRequestId
        } else {
            String originEventId = context.request().getHeader(ORIGIN_EVENT_HEADER)
            if (originEventId) {
                requestId = "origin-${originEventId}"
            } else {
                requestId = "local-${localRequestId}"
            }
        }
        context.put(CONTEXT_REQUESTID, requestId)

        Date date = new Date()
        context.put(CONTEXT_REQUESTTIME, date)

        // Logging
        def formatHeaders = { MultiMap headers ->
            // return "\n   "+headers.join("\n  ")
            return ""+headers.collect { [it.key, it.value] }
        }
        log.info ">>> Request ${requestId} ${context.request().method()} ${context.request().absoluteURI()}, headers: ${formatHeaders(context.request().headers())}"

        context.response().endHandler() {
            log.info "<<< Response ${requestId} end, status ${context.response().getStatusCode()}, ${new Date().time - date.time}ms, headers: ${formatHeaders(context.response().headers())}"
        }

        // Continue with next handler
        context.next()
    }
}
