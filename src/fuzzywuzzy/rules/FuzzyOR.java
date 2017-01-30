package fuzzywuzzy.rules;

class FuzzyOR extends Antecedent {

  private final Antecedent left;
  private final Antecedent right; //should be null for NOT operations

  FuzzyOR(Antecedent left, Antecedent right) {
    this.left = left;
    this.right = right;
  }

  public double evaluate() {
    return Math.max(left.evaluate(),right.evaluate());
  }
}