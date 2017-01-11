package dummybot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import utils.LoggerUtils;
import v1.Archon;
import v1.Gardener;
import v1.Globals;
import v1.Scout;

/**
 * Dummy RobotPlayer that should be used for simple testing purposes only
 */
public strictfp class RobotPlayer {
  static RobotController rc;

  @SuppressWarnings("unused")
  public static void run(RobotController rc) throws GameActionException {
    try{
      Globals.init(rc);
      switch(rc.getType()){
        case ARCHON:
          //Run Archon
          break;
        case GARDENER:
          //Run Gardener
          break;
        case SCOUT:
          //Run Scout
          break;
        case SOLDIER:
          //Run Soldier
          break;
        case TANK:
          //Run Tank
          break;
      }
    } catch (Exception e) {
      LoggerUtils.printStackTrace(e);
    }

  }
}
