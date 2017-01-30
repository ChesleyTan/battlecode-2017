package fuzzywuzzy;

import fuzzywuzzy.rules.Antecedent;
import fuzzywuzzy.rules.FuzzyComparison;

/**
 * Created by Ryan on 1/26/2017.
 */
public class InputVariable {

  private final String name;
  private double value;

  public InputVariable(String name) {
    this.name = name;
  }

  public Antecedent is(InputTerm term) {
    String str = new StringBuilder(name).append(" == ").append(term.getName()).toString();
    return new FuzzyComparison(str, () -> {return term.getMembership(value);});
  }

  public Antecedent isNot(InputTerm term) {
    String str = new StringBuilder(name).append(" != ").append(term.getName()).toString();
    return new FuzzyComparison(str, () -> {return 1-term.getMembership(value);});
  }

  public String getName() {
    return name;
  }

  public void setValue(double value) {
    this.value = value;
  }
}


