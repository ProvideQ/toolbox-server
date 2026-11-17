package edu.kit.provideq.toolbox.cpuopt.solvers.nn;

import java.util.stream.IntStream;

public class Scaler {

    private double[] mean;
    private double[] scale;

    public double[] scale(double[] inputVec){
        return IntStream.range(0, inputVec.length)
                .mapToDouble(i -> (inputVec[i] - mean[i])/mean[i])
                .toArray();
    }

    public double[] inverseScale(double[] inputVec){
        return IntStream.range(0, inputVec.length)
                .mapToDouble(i -> inputVec[i]*scale[i]+mean[i])
                .toArray();
    }

    public void setMean(double[] mean) {
        this.mean = mean;
    }

    public void setScale(double[] scale) {
        this.scale = scale;
    }
}
