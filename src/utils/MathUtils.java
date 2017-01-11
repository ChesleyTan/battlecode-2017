package utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

/**
 * Created by Ryan on 1/10/2017.
 */
public class MathUtils {

  public static boolean isNear(double a, double b) {
    return (Math.abs(a-b) < 0.00001d) ? true : false;
  }

  public static boolean isNear(float a, float b) {
    return (Math.abs(a-b) < 0.00001f) ? true : false;
  }

  public static boolean isNear(MapLocation a, MapLocation b) {
    return isNear(a.x, b.x) && isNear(a.y, b.y);
  }

  public static boolean isNear(Direction a, Direction b) {
    return isNear(a.getAngleDegrees(), b.getAngleDegrees());
  }
}
