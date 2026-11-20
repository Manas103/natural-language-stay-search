package com.manas.staysearch.search;

import java.util.List;

/**
 * The learned reranker: logistic regression over {@code FeatureExtractor}'s 7 features, trained
 * by plain batch gradient descent on the binarized (grade &gt;= 2) synthetic labels. A linear
 * model, not a tree ensemble, is a deliberate choice: its score is a dot product a reader can
 * check by hand, and {@code BookingStepExecutor}-style exactness (the same real number every run,
 * given the same seed and data) matters more here than squeezing out a few more points of NDCG
 * with a black-box model.
 */
public final class LogisticReranker {

    private double[] weights;
    private double bias;

    public LogisticReranker(int featureCount) {
        this.weights = new double[featureCount];
        this.bias = 0.0;
    }

    public double score(double[] features) {
        return sigmoid(dot(features));
    }

    private double dot(double[] features) {
        double sum = bias;
        for (int i = 0; i < features.length; i++) {
            sum += weights[i] * features[i];
        }
        return sum;
    }

    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /** Trains in place by batch gradient descent on binary cross-entropy loss. */
    public void train(List<double[]> featureRows, List<Integer> labels, int epochs, double learningRate) {
        int n = featureRows.size();
        int featureCount = weights.length;
        for (int epoch = 0; epoch < epochs; epoch++) {
            double[] gradW = new double[featureCount];
            double gradB = 0.0;
            for (int i = 0; i < n; i++) {
                double[] features = featureRows.get(i);
                double prediction = sigmoid(dot(features));
                double error = prediction - labels.get(i);
                for (int j = 0; j < featureCount; j++) {
                    gradW[j] += error * features[j];
                }
                gradB += error;
            }
            for (int j = 0; j < featureCount; j++) {
                weights[j] -= learningRate * gradW[j] / n;
            }
            bias -= learningRate * gradB / n;
        }
    }

    public double[] weights() {
        return weights;
    }

    public double bias() {
        return bias;
    }
}
