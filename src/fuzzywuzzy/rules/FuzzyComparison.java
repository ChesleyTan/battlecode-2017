package fuzzywuzzy.rules;


import java.util.function.DoubleSupplier;

public class FuzzyComparison extends Antecedent {
  private final DoubleSupplier func;

  public FuzzyComparison(DoubleSupplier func) {
    this.func = func;
  }

  public double evaluate() {
    return func.getAsDouble();
  }
}