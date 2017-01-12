package dummybot;

import battlecode.common.*;
import utils.MData;
import v1.Globals;

import javax.swing.*;

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
      int count = 0;
      /*System.out.println("North: " + Direction.getNorth().getAngleDegrees());
      System.out.println("South: " + Direction.getSouth().getAngleDegrees());
      System.out.println("East: " + Direction.getEast().getAngleDegrees());
      System.out.println("West: " + Direction.getWest().getAngleDegrees());*/

      while (true) {
        MData mdata = new MData(rc.getLocation(), Direction.getNorth(), rc.getRoundNum(), rc.getType().strideRadius);
        MapLocation predicted = mdata.predictPositionLinear(1.0);
        System.out.printf("Turn %d: Current: (%f,%f), Predicted: (%f,%f)",
                          rc.getRoundNum(), rc.getLocation().x, rc.getLocation().y, predicted.x, predicted.y);
        rc.move(Direction.getNorth());
        count++;
        Clock.yield();
      }
    } catch (Exception e) {
      e.printStackTrace(System.out);
      System.out.println();
      System.out.println();
      System.out.println();

    }

  }
}
