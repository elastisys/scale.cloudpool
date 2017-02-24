package com.elastisys.scale.cloudpool.kubernetes.mock;

import java.util.Map;
import java.util.Objects;

import com.elastisys.scale.commons.json.JsonUtils;

public class HttpRequest {

    /** Requested server path. For example, {@code /some/path} */
    private final String path;
    /** Request method. For example {@code POST}. */
    private final String method;
    /** Request body. */
    private final String body;
    /** Request headers. */
    private final Map<String, String> headers;

    public HttpRequest(String path, String method) {
        this(path, method, null, null);
    }

    public HttpRequest(String path, String method, String body, Map<String, String> headers) {
        this.path = path;
        this.method = method;
        this.body = body;
        this.headers = headers;
    }

    public String getPath() {
        return this.path;
    }

    public String getMethod() {
        return this.method;
    }

    public String getBody() {
        return this.body;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HttpRequest) {
            HttpRequest that = (HttpRequest) obj;
            return Objects.equals(this.path, that.path) //
                    && Objects.equals(this.method, that.method) //
                    && Objects.equals(this.body, that.body) //
                    && Objects.equals(this.headers, that.headers);
        }
        return false;
    }

    @Override
    public String toString() {
        return JsonUtils.toString(JsonUtils.toJson(this));
    }
}
