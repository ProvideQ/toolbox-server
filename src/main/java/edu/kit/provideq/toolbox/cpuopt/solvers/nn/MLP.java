package edu.kit.provideq.toolbox.cpuopt.solvers.nn;

import java.util.List;

public class MLP {

    private int[] architecture;
    private String activation;
    private List<Layer> layers;
    private Scaler input_scaler;
    private Scaler output_scaler;

    public void infer(double[] inputVec){
        for (Layer layer : layers) {
            inputVec = layer.forward(inputVec);
        }
    }

}
