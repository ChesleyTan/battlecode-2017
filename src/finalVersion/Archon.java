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
        assert producedGardeners != -1;
        rc.broadcast(PRODUCED_GARDENERS_CHANNEL, producedGardeners + 1);
        assert rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL) == producedGardeners + 1;
        break;
      }
      attemptedDirection = attemptedDirection.rotateLeftDegrees(10);
      attempts++;
    } while (attempts < 36);
  }
  
  private static void replaceCount() throws GameActionException{
    if (currentRoundNum % 10 == 1){
      int roundLastUpdated = rc.readBroadcast(LAST_UPDATED_REPORT_CHANNEL);
      if (roundLastUpdated != currentRoundNum){
        int lumberjacks = rc.readBroadcast(LUMBERJACK_REPORT_CHANNEL);
        int soldiers = rc.readBroadcast(SOLDIER_REPORT_CHANNEL);
        int gardeners = rc.readBroadcast(GARDENER_REPORT_CHANNEL);
        int tanks = rc.readBroadcast(TANK_REPORT_CHANNEL);
        rc.broadcast(LUMBERJACK_PRODUCTION_CHANNEL, lumberjacks);
        rc.broadcast(LUMBERJACK_REPORT_CHANNEL, 0);
        rc.broadcast(SOLDIER_PRODUCTION_CHANNEL, soldiers);
        rc.broadcast(SOLDIER_REPORT_CHANNEL, 0);
        rc.broadcast(PRODUCED_GARDENERS_CHANNEL, gardeners);
        rc.broadcast(GARDENER_REPORT_CHANNEL, 0);
        rc.broadcast(TANK_PRODUCTION_CHANNEL, tanks);
        rc.broadcast(TANK_REPORT_CHANNEL, 0);
        rc.broadcast(LAST_UPDATED_REPORT_CHANNEL, currentRoundNum);
      }
    }
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
      System.out.println(producedGardeners);
      if (producedGardeners == 0) {
        trySpawnGardener(producedGardeners);
        determineMapSymmetry(myArchons, enemyArchons);
      }
      //calculateDistanceBetweenArchons();
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
          int soldiers = rc.readBroadcast(SOLDIER_PRODUCTION_CHANNEL);
          int treeCount = rc.getTreeCount();
          System.out.println(producedGardeners);
          if (((producedGardeners == 0 || (soldiers >= 2 && producedGardeners == 1 && treeCount >= 2)) || producedGardeners < treeCount * 3.5
              || productionGardeners < requiredProductionGardeners) && rc.senseNearbyRobots(3, us).length < 2) {
            if (soldiers >= (int) producedGardeners * 1.5){
              trySpawnGardener(producedGardeners);
            }
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
        if (currentRoundNum > 10){
          replaceCount();
        }
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }

}