package v1;

import battlecode.common.*;

public class Archon extends Globals{
	
	static int producedGardeners = 0;
	public static void loop() throws GameActionException{
		
		while(true){
			if(rc.canHireGardener(Direction.getNorth()) && producedGardeners < 5){
				rc.hireGardener(Direction.getNorth());
				producedGardeners ++;
			}
			else{
				float rotateAmt = (float)(Math.random() * 360);
				Direction dir = Direction.getNorth().rotateRightDegrees(rotateAmt);
				if(rc.canMove(dir)){
					rc.move(dir);
				}
				else if (rc.canMove(dir.opposite())){
					rc.move(dir.opposite());
				}
			}
			if (rc.getTeamBullets() > 200){
				int donationAmount = (int)((int)(rc.getTeamBullets() / 10) * 10 - 150);
				rc.donate((float)donationAmount);
			}
			Clock.yield();
		}
	}
}