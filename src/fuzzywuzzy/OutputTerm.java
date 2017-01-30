package fuzzywuzzy;

/**
 * Created by Ryan on 1/29/2017.
 */
public class OutputTerm {

  private final String name;
  private final double value;

  public OutputTerm(String name, double value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public double getValue() {
    return value;
  }
}
