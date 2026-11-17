package edu.kit.provideq.toolbox.cpuopt.solvers.nn;

import java.util.stream.IntStream;

public class Layer {
    private double[][] weights;
    private double[] bias;

    public double[] forward(double[]inputVec){

        return IntStream.range(0, bias.length)
                .mapToDouble(neuron -> {
                    double sum = IntStream.range(0, inputVec.length)
                            .mapToDouble(i -> inputVec[i] * weights[neuron][i])
                            .sum();

                    sum += bias[neuron];

                    // ReLU
                    return Math.max(0.0, sum);
                })
                .toArray();
    }

    public void setWeights(double[][] weights) {
        this.weights = weights;
    }

    public void setBias(double[] bias) {
        this.bias = bias;
    }
}
