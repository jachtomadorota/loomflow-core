package io.loomflow.http;

import java.util.Map;

public record Request(String method, String path, Map<String, String> pathParams, String body) {
    public long pathLong(String key) {
        return Long.parseLong(pathParams.get(key));
    }

    public <T> T body(Class<T> type) {
        throw new UnsupportedOperationException("Body mapping not implemented yet for " + type.getName());
    }
}
