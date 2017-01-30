package fuzzywuzzy.rules;

class FuzzyNOT extends Antecedent {

  private final Antecedent operand;

  FuzzyNOT(Antecedent operand) {
    this.operand = operand;
  }

  @Override
  public double evaluate() {
    return 1-operand.evaluate();
  }

  @Override
  public String toString() {
    return "!" + operand.toString();
  }
}