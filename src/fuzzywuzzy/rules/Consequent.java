package fuzzywuzzy.rules;

import fuzzywuzzy.OutputTerm;

/**
 * Created by Ryan on 1/29/2017.
 */
public class Consequent {

  private OutputTerm term;

  public Consequent(OutputTerm term) {
    this.term = term;
  }

  public double getValue() {
    return term.getValue();
  }

  @Override
  public String toString() {
    return term.getName();
  }


}
