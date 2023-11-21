package org.opensearch.knn.jni;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import org.json.*;

import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestClient {

    public static void main(String... args) {
        float[][] vec = new float[][] { { 0, 1, 2 }, { 1, 2, 3 }, { 2, 3, 4 } };
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("data", vec);
        m.put("indexPath", "/tmp/idx.dat");
        post("http://localhost:5000/create_index", m);
    }

    public static JSONObject post(String uri, Map<String, Object> map) {
        try {
            log.info("called post");

            HttpClient client = HttpClient.newBuilder().build();

            log.info("made httpclient");

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

            log.info("made mapper om");

            UncheckedObjectMapper respObjectMapper = new UncheckedObjectMapper();

            log.info("made mapper rom");

            String requestBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);

            log.info("made req body");

            HttpRequest request = HttpRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .uri(URI.create(uri))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            log.info("made req");

            AccessController.doPrivileged((PrivilegedExceptionAction<JSONObject>) () -> {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                log.info("sent");

                String body = (String) response.body();

                log.info(body);

                JSONTokener tok = new JSONTokener(body);
                JSONObject resp = new JSONObject(tok);

                return resp;
            });
            return null;

        } catch (Throwable e) {
            log.info("post failed", e);
            return null;
        }
    }

    static class UncheckedObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {
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
