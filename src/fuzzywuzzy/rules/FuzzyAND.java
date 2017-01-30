package fuzzywuzzy.rules;

class FuzzyAND extends Antecedent {

  private final Antecedent left;
  private final Antecedent right; //should be null for NOT operations
  //private Antecedent[] operands;

  FuzzyAND(Antecedent left, Antecedent right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public double evaluate() {
    return Math.min(left.evaluate(),right.evaluate());
  }

  @Override
  public String toString() {
    return new StringBuilder("(").append(left).append(") && (").append(right).append(")").toString();
  }
}