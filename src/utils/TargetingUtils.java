package utils;

import battlecode.common.*;
import v1.RobotPlayer;

import static utils.Globals.rc;

/**
 * Targeting utilities to be used throughout the codebase.
 */
public class TargetingUtils {

  /**
   * Determines if the shooter can shoot the target location without hitting friendlies or trees.
   */
  public static boolean clearShot(MapLocation shooterLoc, MapLocation target) {
    Direction targetDir = shooterLoc.directionTo(target);
    float distanceTarget = shooterLoc.distanceTo(target);
    MapLocation outerEdge = shooterLoc.add(targetDir, RobotType.SCOUT.bodyRadius + 0.1f);
    RobotInfo[] friendlies = rc.senseNearbyRobots(distanceTarget, Globals.us);
    for (RobotInfo r : friendlies) {
      if (RobotPlayer.willCollideWithTargetLocation(outerEdge, targetDir, r.location,
        r.getRadius())) {
        return false;
      }
    }
    TreeInfo[] trees = rc.senseNearbyTrees(distanceTarget);
    for (TreeInfo t : trees) {
      if (RobotPlayer.willCollideWithTargetLocation(outerEdge, targetDir, t.location,
        t.getRadius())) {
        return false;
      }
    }
    return true;
  }

  public static MapLocation getLinearTargetPoint(MData target, MapLocation shooter, double bulletSpeed) {
    //LINEAR TARGETING ALGORITHM//////
    double targetBearing = target.getAngle();
    double targetSpeed = target.getSpeed();
    double targetX = target.getX();
    double targetY = target.getY();
    double shooterX = shooter.x;
    double shooterY = shooter.y;

    double A = (targetX - shooterX) / bulletSpeed;
    double B = targetSpeed/bulletSpeed*Math.cos(targetBearing);
    double C = (targetY - shooterY) / bulletSpeed;
    double D = targetSpeed/bulletSpeed*Math.sin(targetBearing);

    double a = A*A + C*C;
    double b = 2*(A*B + C*D);
    double c = B*B + D*D - 1;

    double discriminant = b*b - 4*a*c;

    if (discriminant >= 0) {
      //below we have gotten two possible time values
      double t1 = 2*a / (-b + Math.sqrt(discriminant));
      double t2 = 2*a / (-b - Math.sqrt(discriminant));

      //determine which one to use based on which is closer to zero but positive
      double time = (Math.min(t1, t2) >= 0) ? Math.min(t1, t2) : Math.max(t1, t2);
      return target.predictPositionLinear(time);
    } else {
      System.out.println("Discriminant < 0 !!!! Linear Prediction resorting to direct prediction...");
      return target.getLocation();
    }
  }

  /*public static MapLocation predictPositionCircular(double delta, MData mCurr, MData mPrev) {
    double turnRate = (mCurr.getAngle() - mPrev.getAngle())/(mCurr.getTime()-mPrev.getTime());
    //if not turning, resort to linear prediction
    if (MathUtils.isNear(turnRate, 0.0)) {
      System.out.println("resorting to linear");
      return mCurr.predictPositionLinear(delta);
    } else {
      System.out.println("usinc circular");
    }
    double radius = mCurr.getSpeed()/turnRate;
    double tothead = delta*turnRate;
    double xPredicted = mCurr.getX() - (Math.sin(mCurr.getAngle())
    double yPredicted = mCurr.getY() - mCurr.getSpeed()/turnRate*(Math.cos(mCurr.getAngle()-turnRate*delta) - Math.cos(mCurr.getAngle()));
    return new MapLocation((float)xPredicted, (float)yPredicted);
  }*/

}
