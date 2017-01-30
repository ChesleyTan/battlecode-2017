package fuzzywuzzy.rules;

/**
 * Created by Ryan on 1/26/2017.
 */
public class FuzzyRule {

  private final Antecedent a;
  private final Consequent c;

  FuzzyRule(Antecedent antecedent, Consequent consequent) {
    this.a = antecedent;
    this.c = consequent;
  }

  public double getWeight() {
    return a.evaluate();
  }

  public double getTargetValue() {
    return c.getValue();
  }
}
