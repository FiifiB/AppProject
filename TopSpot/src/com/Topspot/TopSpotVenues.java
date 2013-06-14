package com.Topspot;

public class TopSpotVenues {
	
	public enum venue{
		HOME(53.952919,-1.057290, 0);
		
		
		
		private final double latitude;
		private final double longitude;
		private int numberOfPpl;
		
		venue(double latitude,double longitude,int numberOfPpl){
			this.latitude = latitude;
			this.longitude = longitude;
			this.numberOfPpl = numberOfPpl;
		}
		double latitude(){
			return latitude;
		}
		double longitude(){
			return longitude;
		}
		String location(){
			return latitude+","+longitude; 
		}
		int BuzzingNo(){
			return numberOfPpl;
		}
		void inVenue(){
			numberOfPpl++;
		}
		void outVenue(){
			numberOfPpl--;
		} 
		
	}

	

}
