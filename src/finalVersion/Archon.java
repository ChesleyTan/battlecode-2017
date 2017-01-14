package finalVersion;

import battlecode.common.*;
import utils.Globals;

public class Archon extends Globals {

  static int ArchonCount = rc.getInitialArchonLocations(us).length;

  private static void trySpawnGardener(int producedGardeners) throws GameActionException{
    Direction attempt = new Direction(rand.nextFloat() * 2 * (float)Math.PI);
    while (!rc.canHireGardener(attempt)){
      attempt.rotateLeftDegrees(10);
    }
    if (rc.canHireGardener(attempt)){
      rc.hireGardener(attempt);
      rc.broadcast(PRODUCED_GARDENERS_CHANNEL, producedGardeners + 1);
    }
  }
  public static void loop() throws GameActionException {
    EvasiveArchon.init();
    int producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
    if (producedGardeners == 0) {
      Direction randomDir = new Direction((float)(rand.nextFloat() * 2 * Math.PI));
      while(!rc.canHireGardener(randomDir)) {
        randomDir = randomDir.rotateLeftDegrees(10);
      }
      rc.hireGardener(randomDir);
      rc.broadcast(PRODUCED_GARDENERS_CHANNEL, 1);
    }
    
    while (true) {
      int producedScouts = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
      if (producedScouts < 3 && rc.getRoundNum() < 55) {
        EvasiveArchon.move();
        Clock.yield();
        continue;
      }
      producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
      if (producedGardeners < 3 * ArchonCount) {
        trySpawnGardener(producedGardeners);
      }
      EvasiveArchon.move();
      /*if (rc.getTeamBullets() >= 200) {
        float donationAmount = ((int) (rc.getTeamBullets() / 10)) * 10 - 150;
        rc.donate(donationAmount);
      }*/
      //System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
      Clock.yield();
    }
  }

}