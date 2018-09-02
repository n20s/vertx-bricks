package com.github.n20s.vertxbricks.response

/**
 * Exception class with status code property.
 * Based on vert.x 3.4
 *
 * https://github.com/n20s/vertx-bricks
 * Open Source MIT License (X11), see LICENSE.txt
 *
 * @author Nils Mitoussis
 * @version 1.0
 */
class RequestNotFulfillableException extends RuntimeException {

    private static boolean SIMPLIFIED_STACKTRACE_DEFAULT = true

    private int statusCode = 0
    private boolean simplifiedStackTrace = SIMPLIFIED_STACKTRACE_DEFAULT

    RequestNotFulfillableException(String parameter, int statusCode) {
        super(parameter)
        this.statusCode = statusCode
    }

    public int getStatusCode(){
        return this.statusCode
    }

    public boolean getSimplifiedStackTrace() {
        return this.simplifiedStackTrace
    }

    public RequestNotFulfillableException setSimplifiedStackTrace(boolean simplifiedStackTrace) {
        this.simplifiedStackTrace = simplifiedStackTrace
    }

    @Override
    String toString() {
        return super.toString()+" (http status ${statusCode})"
    }
}

