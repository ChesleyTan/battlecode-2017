package v2;

import battlecode.common.*;

public class Archon extends Globals {

  static int ArchonCount = rc.getInitialArchonLocations(us).length;

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
      if (rc.canHireGardener(NORTH) && producedGardeners < 3 * ArchonCount) {
        rc.hireGardener(NORTH);
        rc.broadcast(PRODUCED_GARDENERS_CHANNEL, producedGardeners + 1);
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