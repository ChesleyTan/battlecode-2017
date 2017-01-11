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
   * Checks if two numbers are near enough to each other to be considered equal.
   * Useful to avoid floating point errors.
   */
  public static boolean isNear(double a, double b) {
    return (Math.abs(a-b) < 0.00001d) ? true : false;
  }

  /**
   * Checks if two numbers are near enough to each other to be considered equal.
   * Useful to avoid floating point errors.
   */
  public static boolean isNear(float a, float b) {
    return (Math.abs(a-b) < 0.00001f) ? true : false;
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
