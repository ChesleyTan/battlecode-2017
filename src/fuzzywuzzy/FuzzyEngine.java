package fuzzywuzzy;

import fuzzywuzzy.rules.FuzzyRule;

/**
 * Performs all the fuzzy logic necessary to get a crisp output from a crisp input.
 * @author Ryan Butler
 */
public class FuzzyEngine {

  private FuzzyRule[] rules;

  /**
   * Constructs the FuzzyEngine.
   * @param rules
   */
  public FuzzyEngine(FuzzyRule... rules) {
    this.rules = rules;
  }

  public double evaluate() {
    double result = 0;
    double weightSum = 0;
    for (FuzzyRule r : rules) {
      double weight = r.getWeight();
      double targetValue = r.getTargetValue();
      weightSum += weight;
      result += weight*targetValue;
    }
    return result/weightSum;
  }

  public String describeRules() {
    StringBuilder result = new StringBuilder();
    for (FuzzyRule r : rules) {
      result.append(r).append("\n");
    }
    return result.toString();
  }

}
