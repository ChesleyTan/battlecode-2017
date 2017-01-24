package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;

public class Archon extends Globals {

  static MapLocation[] myArchons = rc.getInitialArchonLocations(us);
  static MapLocation[] enemyArchons = rc.getInitialArchonLocations(them);
  static int ArchonCount = myArchons.length;

  private static void calculateDistanceBetweenArchons() throws GameActionException {
    int me = 0;
    for (int i = 0; i < myArchons.length; i++) {
      if (myArchons[i].equals(here)) {
        me = i;
        break;
      }
    }
  }

  private static void trySpawnGardener(int producedGardeners) throws GameActionException {
    Direction attemptedDirection = RobotUtils.randomDirection();
    int attempts = 0;
    do {
      if (rc.canHireGardener(attemptedDirection)) {
        rc.hireGardener(attemptedDirection);
        rc.broadcast(PRODUCED_GARDENERS_CHANNEL, producedGardeners + 1);
        break;
      }
      attemptedDirection = attemptedDirection.rotateLeftDegrees(10);
      attempts++;
    } while (attempts < 36);
  }

  public static void loop() {
    int producedGardeners;
    try {
      /*
      System.out.println(determineMapSymmetry(myArchons, enemyArchons));
      System.out.println(symmetryX);
      System.out.println(symmetryY);
      readMapSymmetry();
      updateMapBoundaries();
      System.out.println(symmetry);
      System.out.println(symmetryX);
      System.out.println(symmetryY);
      System.out.println(minX);
      System.out.println(maxX);
      System.out.println(minY);
      System.out.println(maxY);
      rc.disintegrate();
      */
      EvasiveArchon.init();
      
      producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
      if (producedGardeners == 0) {
        trySpawnGardener(producedGardeners);
      }
      calculateDistanceBetweenArchons();
    } catch (Exception e) {
      e.printStackTrace();
    }

    while (true) {
      try {
        Globals.update();
        int producedEarlyUnits = rc.readBroadcast(EARLY_UNITS_CHANNEL);
        int requiredProductionGardeners = (int) (rc.getTreeCount() / 15);
        rc.broadcast(PRODUCTION_GARDENERS_CHANNEL, requiredProductionGardeners);
        if (producedEarlyUnits < 3 && currentRoundNum < 55) {
          EvasiveArchon.move();
        }
        else {
          producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
          int productionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
          if ((producedGardeners < 3 || producedGardeners < currentRoundNum / 60
              || productionGardeners < requiredProductionGardeners) && rc.senseNearbyRobots(3, us).length < 2) {
            trySpawnGardener(producedGardeners);
          }
          EvasiveArchon.move();
        }
        /*
        if (currentRoundNum > 2500 && rc.getTeamBullets() >= 1000) {
          float donationAmount = ((int) (rc.getTeamBullets() / 10)) * 10 - 640;
          rc.donate(donationAmount);
        }
        */
        //System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        trackEnemyGardeners();
        RobotUtils.notifyBytecodeLimitBreach();
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }

}