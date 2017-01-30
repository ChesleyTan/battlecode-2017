package fuzzywuzzy.rules;

/**
 * Represents the guard of the if statement in a FuzzyRule
 * @author Ryan Butler
 */
public abstract class Antecedent {

  /**
   * Links this antecedent into the supplied consequent, forming a FuzzyRule
   */
  public FuzzyRule then(Consequent c) {
    return new FuzzyRule(this,c);
  }

  /**
   * Evaluates the antecedent for the rule.
   * This is a fuzzy value that will be combined with others to eventually describe the weight of the rule.
   * It is unlikely that user-code will need to use this.
   */
  public abstract double evaluate();

  @Override
  public abstract String toString();

}
