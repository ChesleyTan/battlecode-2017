package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;

public class Archon extends Globals {

  static int ArchonCount = rc.getInitialArchonLocations(us).length;

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
      EvasiveArchon.init();
      producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
      if (producedGardeners == 0) {
        trySpawnGardener(producedGardeners);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    while (true) {
      try {
        int producedScouts = rc.readBroadcast(EARLY_UNITS_CHANNEL);
        int requiredProductionGardeners = (int) (rc.getTreeCount() / 15);
        rc.broadcast(PRODUCTION_GARDENERS_CHANNEL, requiredProductionGardeners);
        if (producedScouts < 4 && currentRoundNum < 55) {
          EvasiveArchon.move();
        }
        else {
          producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
          int productionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
          if (producedGardeners < 3 * ArchonCount
              || productionGardeners < requiredProductionGardeners) {
            trySpawnGardener(producedGardeners);
          }
          EvasiveArchon.move();
        }
        if (currentRoundNum > 2500 && rc.getTeamBullets() >= 1000) {
          float donationAmount = ((int) (rc.getTeamBullets() / 10)) * 10 - 640;
          rc.donate(donationAmount);
        }
        //System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        trackEnemyGardeners();
        
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }

}