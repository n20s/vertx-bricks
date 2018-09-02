package com.github.n20s.vertxbricks.response

import groovy.json.JsonOutput
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpStatusClass
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Vert.x Route failure handler that creates graceful error information, with configurable message detail level.
 * Note: Consider using this in combination with e.g. JSONResponse to archieve a response formatting.
 *
 * <code>
 * router.route().failureHandler(new ErrorResponseHandler({ RoutingContext context, Map content ->
 *
 *   // Note: Differentiate routes oder Accept headers if json responses should not always be delivered.
 *   new JSONResponse(context, [
 *     "result"        : "error",
 *     "displayMessage": "${content.get(ErrorResponseHandler.CONTENT_MESSAGE_KEY)}" as String]
 *
 *   ).sendJsonResponse(content.get(ErrorResponseHandler.CONTENT_STATUSCODE_KEY))
 * }).setMessageDetailLevel(ErrorResponseHandler.MessageDetailLevel.ClassNameSimple))
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
class ErrorResponseHandler implements Handler<RoutingContext> {

    public static enum MessageDetailLevel {
        NoDetail(1), LocalizedMessage(2), ClassNameSimple(3), ClassNameFull(4), Stacktrace(5)

        MessageDetailLevel(int value) {
            this.value = value
        }
        private final int value
        int getValue() {
            value
        }
    }

    static Logger log = LoggerFactory.getLogger(ErrorResponseHandler.class)

    public static String CONTENT_STATUSCODE_KEY = "statusCode"
    public static String CONTENT_MESSAGE_KEY = "message"

    private static MessageDetailLevel MESSAGE_DETAILLEVEL_DEFAULT = MessageDetailLevel.LocalizedMessage
    private static boolean MESSAGE_INCLUDE_REQUESTID_DEFAULT = true

    /**
     * Level of detail will be included in the display message.
     */
    private MessageDetailLevel messageDetailLevel = MESSAGE_DETAILLEVEL_DEFAULT

    /**
     * Enables including the request id as event in the display message.
     */
    private boolean enableMessageIncludeRequestId = MESSAGE_INCLUDE_REQUESTID_DEFAULT

    private Closure errorRenderingHandler

    ErrorResponseHandler(Closure errorRenderingHandler) {
        this.errorRenderingHandler = errorRenderingHandler
    }

    public ErrorResponseHandler setMessageDetailLevel(MessageDetailLevel messageDetailLevel) {
        this.messageDetailLevel = messageDetailLevel
        return this
    }

    public ErrorResponseHandler setEnableMessageIncludeRequestId(boolean enableMessageIncludeRequestId) {
        this.enableMessageIncludeRequestId = enableMessageIncludeRequestId
        return this
    }

    @Override
    void handle(RoutingContext context) {

        Throwable failure = context.failure()

        int statusCode = context.statusCode()
        if (failure && failure instanceof RequestNotFulfillableException) {
            failure = (RequestNotFulfillableException)failure
            statusCode = failure.getStatusCode()
        }

        String responseMessage = ''
        String logMessage = "${getRequestId(context)?:''} failure handler "
        String stackTrace = ''
        try {
            if (failure == null) {
                logMessage += "triggered (status code ${statusCode}, no failure/throwable specified)"
            } else {
                if (this.messageDetailLevel.value >= MessageDetailLevel.LocalizedMessage.value) {
                    responseMessage += failure.localizedMessage

                    if (this.messageDetailLevel.value >= MessageDetailLevel.ClassNameSimple.value) {
                        responseMessage += outputSignificantThrowableClassName(failure)
                    }
                }

                logMessage += "caught exception "
                Throwable throwable = failure
                Throwable rootCause
                while (throwable) {
                    boolean simplifiedStackTrace = false
                    if (throwable instanceof RequestNotFulfillableException) {
                        simplifiedStackTrace = throwable.simplifiedStackTrace
                    }
                    stackTrace += "${throwable.toString()}"

                    String strackTracePart = createStackTrace(throwable, simplifiedStackTrace)
                    stackTrace += strackTracePart

                    Throwable cause = throwable.getCause()
                    throwable = cause
                    if (cause) {
                        stackTrace += "\ncaused by\n"
                        rootCause = cause
                        // check next loop, rootCause may be updated if a subsequent cause is found in exception chain.
                    }
                }
                logMessage += stackTrace

                if (rootCause) {
                    if (this.messageDetailLevel.value >= MessageDetailLevel.LocalizedMessage.value) {
                        responseMessage += ", root cause '${rootCause.localizedMessage}"

                        if (this.messageDetailLevel.value >= MessageDetailLevel.ClassNameSimple.value) {
                            responseMessage += ${outputSignificantThrowableClassName(rootCause)}
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log.error "Exception in failure handler: ${t} ${createStackTrace(t, false)}"
            responseMessage = "Exception in failure handler: ${t}\n${responseMessage}"
            statusCode = 500
        } finally {
            log.error "${logMessage}"
        }

        // Fallback if no response message is available or details are disabled
        if (responseMessage?.length() == 0) {
            if (statusCode > 0) {
                // This most likely is a prepared status code with no failure/throwable, e.g. 401 when not authenticated.
                // Handle this gracefully.
                String statusDescription = HttpResponseStatus.valueOf(statusCode).reasonPhrase()+", "+HttpStatusClass.valueOf(statusCode).defaultReasonPhrase()
                responseMessage = "HTTP ${statusCode} ${statusDescription}"
            } else {
                responseMessage = "Request could not be processed"
            }
        }

        if (this.messageDetailLevel.value >= MessageDetailLevel.Stacktrace.value) {
            responseMessage += " \nStacktrace: ${stackTrace}"
        }

        if (statusCode > 0) {
            this.replyError(context, statusCode, responseMessage)
        } else {
            def msg = "Internal Server Error"
            if (responseMessage) msg += ": ${responseMessage}"
            this.replyError(context, 500, msg)
        }
    }

    public void replyError(RoutingContext context, int statusCode, String message) {

        log.error "${getRequestId(context)?:''} failed with ${statusCode}: \"${message}\""

        String requestId = getRequestId(context)
        if (this.enableMessageIncludeRequestId && requestId.isEmpty() == false) {
            message = message + " (event ${requestId})"
        }

        if (this.errorRenderingHandler) {
            this.errorRenderingHandler(context,
                    [(CONTENT_STATUSCODE_KEY): new Integer(statusCode),
                     (CONTENT_MESSAGE_KEY)   : message])
        } else {
            context.response().putHeader('Content-Type', 'text/plain')
            def content = JsonOutput.prettyPrint(JsonOutput.toJson([message: "${statusCode} ${message}"]))
            context.response().setStatusCode(statusCode).end(content)
        }
    }

    private String getRequestId(RoutingContext context) {
        return context.get('cRequestID')
    }

    private String outputSignificantThrowableClassName(Throwable throwable) {
        if (this.messageDetailLevel.value < MessageDetailLevel.ClassNameSimple.value) {
            return ''
        }
        if (throwable == null) {
            return ''
        }

        String name
        if (this.messageDetailLevel.value >= MessageDetailLevel.ClassNameFull.value) {
            name = throwable.getClass().getName()
            // if (name == "java.lang.RuntimeException") return "" // not significant
            if (name.startsWith('java.lang.') && name.length() >= 10) { name = name.substring(10)}
        } else if (this.messageDetailLevel.value >= MessageDetailLevel.ClassNameSimple.value) {
            name = throwable.class.getSimpleName()
        }

        return " (${name})"
    }

    private String createStackTrace(Throwable throwable, boolean filtered) {

        final String seperator = "\n    "

        List<String> filterList = []
        if (filtered) {
            filterList = ['java.', 'com.sun.', 'sun.', 'org.codehaus.groovy', 'groovy.lang', 'io.vertx.', 'io.netty.']
        }
        String msg = seperator + throwable.stackTrace.findAll() { StackTraceElement e ->
            boolean accept = true
            if (filtered) {
                filterList.each { String filterEntry ->
                    if (e.declaringClass.startsWith(filterEntry)) {
                        accept = false
                    }
                }
            }
            return accept
        }.join(seperator)

        return msg
    }
}
