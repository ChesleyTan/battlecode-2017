package v2_soldier;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;

public class Soldier extends Globals {

  private static final int ATTACK = 1;
  private static final int DEFEND = 2;
  private static final int ROAM = 3;
  private static int squad_channel;
  private static int mode;
  private static Direction mydir;
  private static RobotInfo target;

  private static void findSquad() throws GameActionException {
    int i = DEFENSE_START_CHANNEL;
    while (i < DEFENSE_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      // System.out.println(squad_count);
      if (squad_count == 0) {
        // Clear out target field
        rc.broadcast(i + 1, -1);
      }
      if (squad_count < 3) {
        squad_channel = i;
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i = i + 4;
    }
    squad_channel = ATTACK_START_CHANNEL;
  }

  public static RobotInfo priority(RobotInfo[] enemies) {
    RobotInfo result = null;
    int currvalue = 0;
    for (RobotInfo r : enemies) {
      int value = 0;
      switch (r.getType()) {
      case GARDENER:
        value = 4;
        break;
      case SCOUT:
        value = 3;
        break;
      case ARCHON:
        value = 2;
        break;
      case SOLDIER:
        value = 1;
        break;
      case LUMBERJACK:
        value = 1;
        break;
      case TANK:
        value = 0;
        break;
      }
      // System.out.println("Value: " + value);
      if (value > currvalue) {
        currvalue = value;
        result = r;
      }
    }
    return result;
  }

  public static void loop() {
    try{
      findSquad();
    }catch(Exception e){
      e.printStackTrace();
    }
    while(true){
      try{
        if (mode == ATTACK){
          if(target == null){
            //hasn't found him or he recently died
            float xCor = rc.readBroadcast(squad_channel + 2);
            float yCor = rc.readBroadcast(squad_channel + 3);
            if (xCor == 0 && yCor == 0){
              // Check for nearby enemies
              RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
              RobotInfo priority = priority(enemies);
              if (priority != null){
                MapLocation destination = priority.location;
                boolean successfulMove = RobotUtils.tryMove(here.directionTo(destination), 10, 6);
                if (successfulMove){
                  target = priority;
                  rc.broadcast(squad_channel + 1, target.ID);
                  rc.broadcast(squad_channel + 2, (int)target.location.x);
                  rc.broadcast(squad_channel + 3, (int)target.location.y);
                }
                else{
                  //roam();
                }
              }
              else{
                //roam();
              }
            }
            else{
              // if there is a target
              MapLocation destination = new MapLocation(xCor, yCor);
              Direction targetDirection = here.directionTo(destination);
              if (!RobotUtils.tryMove(targetDirection, 10, 9)){
                //roam()
                // try to find closer target
              }
            }
          }
        }
        else if (mode == DEFEND){
          // check defender's coords from defense channel, and stick near them
        }
        else if (mode == ROAM){
          // check defense every turn, if so then head to defend target
        }
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }
}