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
				try{
					Gardener.loop();
					break;
				}
				catch(Exception e){
					e.printStackTrace();
				}
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
	
	public static float degreesBetween(Direction a, Direction b) {
    return (float) Math.toDegrees(radiansBetween(a, b));
  }
  
  public static float radiansBetween(Direction a, Direction b) {
    return reduce(b.radians - a.radians);
  }
  
  private static float reduce(float rads) {
    if (rads <= -Math.PI) {
        int circles = (int) Math.ceil(-(rads + Math.PI) / (2 * Math.PI));
        return rads + (float) (Math.PI * 2 * circles);
    }
    else if (rads > Math.PI) {
        int circles = (int) Math.ceil((rads - Math.PI) / (2 * Math.PI));
        return rads - (float) (Math.PI * 2 * circles);
    }
    return rads;
  }
  
}