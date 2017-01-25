package v3_gardeners;

import battlecode.common.*;
import finalVersion.*;
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