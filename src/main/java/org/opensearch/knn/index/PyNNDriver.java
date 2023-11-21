package org.opensearch.knn.index;

import org.opensearch.knn.index.util.KNNEngine;

public class PyNNDriver {
    private final float[][] data = new float[][] { { 0, 1, 2 }, { 1, 2, 3 }, { 3, 4, 5 } };

    public static void main(String[] args) {
        KNNEngine engine = KNNEngine.getEngine("pynn");
        assert engine != null;
        assert engine.isInitialized() : engine;
        System.err.println("name: " + engine.getName());

    }
}
