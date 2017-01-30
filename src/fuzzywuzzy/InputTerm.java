package fuzzywuzzy;

/**
 * Created by Ryan on 1/26/2017.
 */
public class InputTerm {

  private final String name;
  private final MembershipFunction func;

  public InputTerm(String name, double start, double upperLeft, double upperRight, double end, double height) {
    this.name = name;
    this.func = new MembershipFunction(start, upperLeft, upperRight, end, height);
  }

  public InputTerm(String name, MembershipFunction func) {
    this.name = name;
    this.func = func;
  }

  double getMembership(double input) {
    return func.evaluate(input);
  }

  public String getName() {
    return name;
  }
}
