package v1;

import battlecode.common.*;

public class Archon extends Globals {

  static int ArchonCount = rc.getInitialArchonLocations(us).length;

  public static void loop() throws GameActionException {
    EvasiveArchon.init();
    int producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
    if (producedGardeners == 0) {
      // TODO unrestrict gardener from north?
      if (rc.canHireGardener(NORTH)) {
        rc.hireGardener(NORTH);
        rc.broadcast(PRODUCED_GARDENERS_CHANNEL, 1);
      }
    }
    while (true) {
      int producedScouts = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
      System.out.println("S: " + producedScouts);
      if (producedScouts < 3) {
        EvasiveArchon.move();
        Clock.yield();
        continue;
      }
      producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
      System.out.println("G: " + producedGardeners);
      if (rc.canHireGardener(NORTH) && producedGardeners < 5 * ArchonCount) {
        rc.hireGardener(NORTH);
        rc.broadcast(PRODUCED_GARDENERS_CHANNEL, producedGardeners + 1);
      }
      EvasiveArchon.move();
      if (rc.getTeamBullets() > 200) {
        float donationAmount = ((int) (rc.getTeamBullets() / 10)) * 10 - 150;
        rc.donate(donationAmount);
      }
      //System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
      Clock.yield();
    }
  }

}