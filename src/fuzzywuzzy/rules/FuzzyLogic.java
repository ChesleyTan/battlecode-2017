package fuzzywuzzy.rules;

/**
 * Created by Ryan on 1/27/2017.
 */
public final class FuzzyLogic {

  public static Antecedent and(Antecedent... operands) {
    Antecedent result = operands[0];
    for (int i=1; i<operands.length; i++) {
      result = new FuzzyAND(result, operands[i]);
    }
    return result;
  }

  public static Antecedent or(Antecedent... operands) {
    Antecedent result = operands[0];
    for (int i=1; i<operands.length; i++) {
      result = new FuzzyOR(result, operands[i]);
    }
    return result;
  }

  public static Antecedent not(Antecedent operand) {
    return new FuzzyNOT(operand);
  }

  //Prevent instantiating this class
  private FuzzyLogic() {};

}
