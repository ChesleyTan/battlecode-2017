package fuzzywuzzy;

import fuzzywuzzy.rules.Consequent;

/**
 * Created by Ryan on 1/26/2017.
 */
public class OutputVariable {

  private final String name;
  //private double value;

  public OutputVariable(String name) {
    this.name = name;
  }

  public Consequent set(OutputTerm term) {
    return new Consequent(term);
  }

  public String getName() {
    return name;
  }
}
