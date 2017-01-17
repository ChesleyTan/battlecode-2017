package finalVersion;

import battlecode.common.*;
import utils.*;

public class Tank extends Globals {
  public static void loop() {

    // The code you want your robot to perform every round should be in this loop
    while (true) {

      // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
      try {
        MapLocation myLocation = rc.getLocation();

        // See if there are any nearby enemy robots
        RobotInfo[] robots = rc.senseNearbyRobots(-1, them);

        // If there are some...
        if (robots.length > 0) {
          // And we have enough bullets, and haven't attacked yet this turn...
          if (rc.canFireSingleShot() && TargetingUtils.clearShot(here, robots[0])) {
            // ...Then fire a bullet in the direction of the enemy.
            rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
          }
        }

        // Move randomly
        RobotUtils.tryMove(RobotUtils.randomDirection(), 10, 3);

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        Clock.yield();

      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }
}