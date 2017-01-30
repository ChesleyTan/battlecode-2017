package fuzzywuzzy.rules;


import java.util.function.DoubleSupplier;

public class FuzzyComparison extends Antecedent {
  private final DoubleSupplier func;
  private final String str;

  public FuzzyComparison(String str, DoubleSupplier func) {
    this.str = str;
    this.func = func;
  }

  @Override
  public double evaluate() {
    return func.getAsDouble();
  }

  @Override
  public String toString() {
    return str;
  }
}