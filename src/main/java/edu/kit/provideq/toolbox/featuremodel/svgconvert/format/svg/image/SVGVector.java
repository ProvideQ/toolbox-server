package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image;


import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class SVGVector extends Point2D {
    private final int dimension;
    private final double[] values;

    public SVGVector(int dimension) {
        this.dimension = dimension;
        this.values = new double[dimension];
    }

    public SVGVector(SVGVector vector) {
        this.dimension = vector.dimension;
        this.values = Arrays.copyOf(vector.values, vector.values.length);
    }

    public SVGVector(double ... args) {
        this.dimension = args.length;
        this.values = args;
    }

    /**
     * Calculates a vector that is scaled to the given length and
     * has the direction starting from this vector the given vector.
     *
     * @param vector
     * @return
     */
    public SVGVector calculateScaledDiffVector(SVGVector vector) {
        double scale = 10.0;
        SVGVector diff = this.subtract(vector);
        double diffLength = diff.getLength();
        if (diffLength > 0) {
            scale = scale / diffLength;
        }
        SVGVector scaledDiffVector = diff.multiply(scale);
        return vector.add(scaledDiffVector);
    }

    public SVGVector subtract(SVGVector vector) {
        if (this.dimension != vector.dimension) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        SVGVector result = new SVGVector(this.dimension);
        for (int i = 0; i < this.dimension; i++) {
            result.values[i] = this.values[i] - vector.values[i];
        }
        return result;
    }

    public double getLength() {
        double sum = 0;
        for (double value : values) {
            sum += Math.pow(value, 2);
        }
        return Math.sqrt(sum);

    }

    public SVGVector multiply(double scalar) {
        SVGVector result = new SVGVector(this.dimension);
        for (int i = 0; i < this.dimension; i++) {
            result.values[i] = this.values[i] * scalar;
        }
        return result;
    }

    public SVGVector add(SVGVector vector) {
        if (this.dimension != vector.dimension) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        SVGVector result = new SVGVector(this.dimension);
        for (int i = 0; i < this.dimension; i++) {
            result.values[i] = this.values[i] + vector.values[i];
        }
        return result;
    }

    public int getDimension() {
        return dimension;
    }

    public double[] getValues() {
        return values;
    }
    public double get(int index) {
        return values[index];
    }

    public void set(int index, double value) {
        values[index] = value;
    }

    @Override
    public double getX() {
        if (dimension == 2) {
            return values[0];
        }
        throw new UnsupportedOperationException("Vector has not the dimension 2");
    }

    @Override
    public double getY() {
        if (dimension == 2) {
            return values[1];
        }
        throw new UnsupportedOperationException("Vector has not the dimension 2");
    }

    public void setY(double y) {
        if (dimension == 2) {
            values[1] = y;
            return;
        }
        throw new UnsupportedOperationException("Vector has not the dimension 2");
    }

    @Override
    public void setLocation(double v, double v1) {
        if (dimension == 2) {
            values[0] = v;
            values[1] = v1;
        }
        throw new UnsupportedOperationException("Vector has not the dimension 2");

    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dimension);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SVGVector svgVector)) return false;
        return dimension == svgVector.dimension && Arrays.equals(values, svgVector.values);
    }

    public void setX(double x) {
        if (dimension == 2) {
            values[0] = x;
            return;
        }
        throw new UnsupportedOperationException("Vector has not the dimension 2");
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%.2f,%.2f", values[0], values[1]);
    }
}
