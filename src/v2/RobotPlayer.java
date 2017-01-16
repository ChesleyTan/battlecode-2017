package v2;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;
import finalVersion.Archon;
import finalVersion.Scout;
import finalVersion.Lumberjack;
import finalVersion.Gardener;
public class RobotPlayer extends Globals {

  public static void run(RobotController rc) throws GameActionException {
    Globals.init(rc);
    switch (rc.getType()) {
      case ARCHON:
        Archon.loop();
        break;
      case GARDENER:
        Gardener.loop();
        break;
      case SCOUT:
        Scout.loop();
        break;
      case LUMBERJACK:
        Lumberjack.loop();
        break;
      case SOLDIER:
        runSoldier();
        break;
      case TANK:
        //Run Tank
        break;
    }
  }

  // ------------ Example Func Player code ----------------
  
  static void runSoldier() throws GameActionException {
    System.out.println("I'm an soldier!");
    Team enemy = rc.getTeam().opponent();

    // The code you want your robot to perform every round should be in this loop
    while (true) {

        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
        try {
            MapLocation myLocation = rc.getLocation();

            // See if there are any nearby enemy robots
            RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

            // If there are some...
            if (robots.length > 0) {
                // And we have enough bullets, and haven't attacked yet this turn...
                if (rc.canFireSingleShot()) {
                    // ...Then fire a bullet in the direction of the enemy.
                    rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                }
            }

            // Move randomly
            RobotUtils.tryMove(RobotUtils.randomDirection(), 10, 3);

            // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
            Clock.yield();

        } catch (Exception e) {
            System.out.println("Soldier Exception");
            e.printStackTrace();
        }
    }
}

}