package v1;

import battlecode.common.*;

public class RobotPlayer{
	
	public static void run(RobotController rc) throws GameActionException{
		Globals.init(rc);
		switch(rc.getType()){
			case ARCHON:
				Archon.loop();
				break;
			case GARDENER:
				Gardener.loop();
				break;
			case SCOUT:
				Scout.loop();
				break;
			case SOLDIER:
				//Run Soldier
			case TANK:
				//Run Tank
			break;
		}
	}
}