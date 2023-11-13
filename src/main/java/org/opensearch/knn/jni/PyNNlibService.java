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

import java.util.Map;
import java.util.Arrays;
import java.util.List;

import jep.JepConfig;
import jep.MainInterpreter;
import jep.NDArray;
import jep.SharedInterpreter;
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

    private static SharedInterpreter subInterp;
    static {

        // define the JEP library path
        String jepPath = "/data/anaconda3/envs/jep/lib/python3.11/site-packages/jep/libjep.so";

        // initialize the MainInterpreter
        MainInterpreter.setJepLibraryPath(jepPath);

        jep.JepConfig jepConf = new JepConfig();

        SharedInterpreter.setConfig(jepConf);

        subInterp = new SharedInterpreter();

        subInterp.eval("import pynndescent");

        subInterp.eval("import pickle");

        subInterp.eval("import numpy as np");

        subInterp.eval("indexes = {}");

        subInterp.eval("indexCounter = 0");


    }

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
        subInterp.set("data", data);
        subInterp.eval("data = np.array(data)");
        subInterp.eval("index = pynndescent.NNDescent(data,metric='hamming')");
        subInterp.set("indexPath", indexPath);
        subInterp.eval("with open(indexPath, 'wb') as f:\n   pickle.dump(index, f)");
        log.info("finished create index with:");
    }

    /**
     * Load an index into memory
     *
     * @param indexPath path to index file
     * @param parameters parameters to be used when loading index
     * @return pointer to location in memory the index resides in
     */
    public static long loadIndex(String indexPath, Map<String, Object> parameters) {
        subInterp.set("indexPath", indexPath);
        subInterp.eval("indexCounter += 1");
        subInterp.eval("with open(indexPath, 'rb') as f:\n   indexes[indexCounter] = pickle.load(f)");
        subInterp.eval("index.prepare()");
        return ((Number)subInterp.getValue("indexCounter")).longValue();
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
        subInterp.set("qv", queryVector);
        subInterp.eval("index = indexes[" + indexPointer + "]");
        subInterp.eval("queryVector = np.array([qv])");
        subInterp.eval("ans = index.query(queryVector)");
        List<NDArray<?>> result = (List<NDArray<?>>)subInterp.getValue("ans");
        assert result.size() == 2;
        int nodes = result.get(0).getDimensions()[0];
        int neighbors = result.get(0).getDimensions()[1];
        KNNQueryResult[][] answers = new KNNQueryResult[nodes][];
        for (int i = 0; i < nodes; i++) {
            KNNQueryResult[] elts = answers[i] = new KNNQueryResult[neighbors];
            for (int j = 0; j < neighbors; j++) {
                elts[j] = new KNNQueryResult(
                    ((int[]) result.get(0).getData())[i * neighbors + j],
                    ((float[]) result.get(1).getData())[i * neighbors + j]
                );
            }
        }
        return answers[0];
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
