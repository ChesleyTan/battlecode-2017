package utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

/**
 * Provides useful math functions to be used throughout the application.
 *
 * @author Ryan Butler
 */
public class MathUtils {

  /**
   * Clamps an input to lie in the range of two values
   * @param min The minimum value x can be (inclusive)
   * @param max The maximum value x can be (inclusive)
   * @param x The value to clamp
   * @return The clamped value
   */
  public static int clamp(int min, int max, int x) {
    return (x<min) ? min : (x>max) ?  max : x;
  }

  /**
   * Clamps an input to lie in the range of two values
   * @param min The minimum value x can be (inclusive)
   * @param max The maximum value x can be (inclusive)
   * @param x The value to clamp
   * @return The clamped value
   */
  public static double clamp(double min, double max, double x) {
    return (x<min) ? min : (x>max) ?  max : x;
  }

  /**
   * Clamps an input to lie in the range of two values
   * @param min The minimum value x can be (inclusive)
   * @param max The maximum value x can be (inclusive)
   * @param x The value to clamp
   * @return The clamped value
   */
  public static float clamp(float min, float max, float x) {
    return (x<min) ? min : (x>max) ?  max : x;
  }

  /**
   * Checks if a number is within a given range. The range is inclusive.
   */
  public static boolean isInRange(double min, double max, double x) {
    return (x>=min) && (x<=max);
  }

  /**
   * Checks if a number is within a given range. The range is inclusive.
   */
  public static boolean isInRange(float min, float max, float x) {
    return (x>=min) && (x<=max);
  }

  /**
   * Checks if a number is within a given range. The range is inclusive.
   */
  public static boolean isInRange(int min, int max, int x) {
    return (x>=min) && (x<=max);
  }

  /**
   * Checks if two numbers are near enough to each other to be considered equal.
   * Useful to avoid floating point errors.
   */
  public static boolean isNear(double a, double b) {
    return (Math.abs(a-b) < 0.000001d) ? true : false;
  }

  /**
   * Checks if two numbers are near enough to each other to be considered equal.
   * Useful to avoid floating point errors.
   */
  public static boolean isNear(float a, float b) {
    return (Math.abs(a-b) < 0.000001f) ? true : false;
  }

  /**
   * Checks if two numbers are near enough to each other to be considered equal.
   * Useful to avoid floating point errors.
   */
  public static boolean isNear(float a, float b, float epsilon) {
    return (Math.abs(a-b) < epsilon) ? true : false;
  }

  /**
   * Checks if two MapLocation objects are near enough to each other to be considered equal.
   * Useful to avoid floating point errors.
   */
  public static boolean isNear(MapLocation a, MapLocation b) {
    return isNear(a.x, b.x) && isNear(a.y, b.y);
  }

  /**
   * Checks if two Direction objects are similar enough to each other to be considered equal.
   * Useful to avoid floating point errors.
   */
  public static boolean isNear(Direction a, Direction b) {
    return isNear(a.getAngleDegrees(), b.getAngleDegrees());
  }
}
