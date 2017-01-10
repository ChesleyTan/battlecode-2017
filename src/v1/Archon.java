package v1;

import battlecode.common.*;

public class Archon extends Globals{
	
	public static void loop() throws GameActionException{
		
		while(true){
			System.out.println(here);
			if(rc.canHireGardener(Direction.getNorth())){
				rc.hireGardener(Direction.getNorth());
			}
			if (rc.getTeamBullets() > 200){
				int donationAmount = (int)((int)(rc.getTeamBullets() / 10) * 10 - 150);
				rc.donate((float)donationAmount);
			}
			Clock.yield();
		}
	}
}