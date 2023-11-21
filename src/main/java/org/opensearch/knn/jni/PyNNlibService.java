/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.knn.jni;

import lombok.extern.log4j.Log4j2;
import org.opensearch.knn.index.query.KNNQueryResult;

import org.json.*;
import java.util.*;

/**
 * Service to interact with nmslib jni layer. Class dependencies should be minimal
 *
 * In order to compile C++ header file, run:
 * javac -h jni/include src/main/java/org/opensearch/knn/jni/NmslibService.java
 *      src/main/java/org/opensearch/knn/index/KNNQueryResult.java
 *      src/main/java/org/opensearch/knn/common/KNNConstants.java
 */
@Log4j2
class PyNNlibService {

    /**
     * Create an index for the native library
     *
     * @param ids array of ids mapping to the data passed in
     * @param data array of float arrays to be indexed
     * @param indexPath path to save index file to
     * @param parameters parameters to build index
     */
    public static void createIndex(int[] ids, float[][] data, String indexPath, Map<String, Object> parameters) {
        log.info("called create index with:" + indexPath);

        Map<String, Object> post = new LinkedHashMap<>();
        post.put("data", data);
        post.put("indexPath", indexPath);

        log.info(post);

        JSONObject code = TestClient.post("http://localhost:5000/create_index", post);

        log.info("done" + code);
    }

    /**
     * Load an index into memory
     *
     * @param indexPath path to index file
     * @param parameters parameters to be used when loading index
     * @return pointer to location in memory the index resides in
     */
    public static long loadIndex(String indexPath, Map<String, Object> parameters) {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("indexPath", indexPath);

        JSONObject resp = TestClient.post("http://localhost:5000/load_index", post);

        return resp.getInt("indexPointer");
    }

    /**
     * Query an index
     *
     * @param indexPointer pointer to index in memory
     * @param queryVector vector to be used for query
     * @param k neighbors to be returned
     * @return KNNQueryResult array of k neighbors
     */
    public static KNNQueryResult[] queryIndex(long indexPointer, float[] queryVector, int k) {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("indexPointer", indexPointer);
        post.put("queryVector", queryVector);
        post.put("k", k);

        JSONObject resp = TestClient.post("http://localhost:5000/load_index", post);

        JSONArray neighbors = resp.getJSONArray("neighbor_answers");
        JSONArray distances = resp.getJSONArray("distances");

        KNNQueryResult[] answer = new KNNQueryResult[neighbors.length()];
        for (int i = 0; i < neighbors.length(); i++) {
            answer[i] = new KNNQueryResult(neighbors.getInt(i), distances.getFloat(i));
        }

        return answer;
    }

    /**
     * Free native memory pointer
     */
    public static native void free(long indexPointer);

    /**
     * Initialize library
     */
    public static native void initLibrary();

}
