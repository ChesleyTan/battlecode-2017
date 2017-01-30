package fuzzywuzzy;

import java.util.function.DoubleSupplier;

/**
 * Created by Ryan on 1/29/2017.
 */
public class MembershipFunction {

  private double start, upperLeft, upperRight, end, height;

  private double risingSlope, fallingSlope;

  public MembershipFunction(double start, double upperLeft, double upperRight, double end, double height) {
    this.start = start;
    this.upperLeft = upperLeft;
    this.upperRight = upperRight;
    this.end = end;
    this.height = height;

    this.risingSlope = height/(upperLeft-start);
    this.fallingSlope = height/(end-upperRight);

  }

  public double evaluate(double input) {
    if (input<= start) {
      return 0; //out of bounds
    } else if (input<upperLeft) {
      return (input - start) * risingSlope; //rising slope
    } else if (input <= upperRight) {
      return height; //plateau
    } else if (input < end) {
      return (end - input) * fallingSlope; //falling slope
    } else {
      return 0; //out of bounds
    }
  }
}
