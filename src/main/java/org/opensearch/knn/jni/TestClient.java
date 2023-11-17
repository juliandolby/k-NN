package org.opensearch.knn.jni;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class TestClient {

    public void post(String uri, Map<String, Object> map) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        UncheckedObjectMapper respObjectMapper = new UncheckedObjectMapper();

        String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();

        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.discarding());
        // .thenApply(HttpResponse::body)
        // .thenApply(respObjectMapper::readValue);

        System.out.println(response);
    }

    class UncheckedObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {
        /**
         * Parses the given JSON string into a Map.
         */
        Map<String, String> readValue(String content) {
            try {
                return this.readValue(content, new TypeReference<>() {
                });
            } catch (IOException ioe) {
                throw new CompletionException(ioe);
            }
        }
    }
}
