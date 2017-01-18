package v2_soldier;

import battlecode.common.*;
import finalVersion.Gardener;
import finalVersion.Scout;
import finalVersion.Archon;
import finalVersion.Lumberjack;
import finalVersion.Tank;
import finalVersion.Soldier;
import utils.Globals;
import utils.RobotUtils;

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
        Soldier.loop();
        break;
      case TANK:
        Tank.loop();
        break;
    }
  }
}