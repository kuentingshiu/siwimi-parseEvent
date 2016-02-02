package com.siwimi.webparsers.parser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Activity.Category;
import com.siwimi.webparsers.domain.Activity.LifeStage;
import com.siwimi.webparsers.domain.Location;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

public class AADLParser implements Parser{

	@Override
	public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
		List<Activity> activities = new ArrayList<Activity>();
		String defaultEventHostUrl = "http://www.aadl.org";

		// Retrieve three pages from AADL
		for (int i=0; i<3; i++) {
			String activitiesUrl = i==0 ? eventsSourceUrl: "http://www.aadl.org/events?page="+i;
			Document doc = null; 
			try {
				// Build up connection, and parse activities
				doc = Jsoup.connect(activitiesUrl).get();
				String parseActivity = "node node-type-pub-event node-teaser build-mode-teaser with-picture";
				Elements divs = doc.getElementsByAttributeValue("class", parseActivity);
				
				// Retrieve data from a single activity
				for (Element e : divs) {
					String eventUrl = e.select("h2.title a").attr("href");
					String title = e.select("h2.title a").text();
					String content1 = null;
					String content2 = null;
					for (Element content : e.getElementsByClass("content")) {
						content1 = content.select("h3").text();
						content2 = content.select("p").text();
					}
				
					String[] parts = eventUrl.split("/");
					String nodeId = parts[2];
					
					// Populate data into activity object	
					if ((title != null) && !title.isEmpty() && (nodeId != null)) {
						if (activityRep.isExisted(nodeId, parser)) {
							continue;
						}
						
						// Populate activity : url
						if (eventUrl != null)
							eventUrl = defaultEventHostUrl + eventUrl;
						
						// Populate activity : title

						// Populate activity : type
						int fromAge = 0, toAge = 0;
						Category type = defaultCategory;
						if (title != null) {
							if (title.toLowerCase().contains("story")) {
								type = Category.storytelling;
								fromAge = 2;
								toAge = 5;
							} else if (title.toLowerCase().contains("playgroup")) {
								type =  Category.playdate;
								fromAge = 0;
								toAge = 2;
							} else if (title.toLowerCase().contains("dancing babies")) {
								type = Category.playdate;
								fromAge = 0;
								toAge = 5;
							} else if (title.toLowerCase().contains("concert")) {
								type = Category.concert;
								fromAge = 2;
								toAge = 5;
							} else if (title.toLowerCase().contains("film")) {
								type = Category.movie;								
							}
						}
						
						String imageUrl = e.select("img").attr("src");
						String image64Data = "";
						if (imageUrl != null) {
							image64Data = getImageBase64(imageUrl);
						}
						
						Activity activity = new Activity();

						if (content1 != null) {
							String [] part1;
							String [] part2;
							String [] part3;
							if (content1.contains("through")) {
								part1 = content1.split("through");
								SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy hh:mm aaa");
								//populate fromDate
								if (part1[0].toLowerCase().contains("now")) {
									activity.setFromDate(new Date());
								} else {
									try {
										Date fromDate = formatter.parse(part1[0].trim()+" 8:00 am");
										activity.setFromDate(fromDate);
									} catch (ParseException e1) {
										e1.printStackTrace();
									}
								}
								//populate toDate
								part2 = part1[1].split("--");
								try {
									Date toDate = formatter.parse(part2[0].trim()+" 8:00 pm");
									activity.setToDate(toDate);
								} catch (ParseException e1) {
									e1.printStackTrace();
								}
								part3 = part2;		
							} else {
								part1 = content1.split(":");
								//populate fromTime
								content1 = content1.replaceAll(part1[0]+":", "");
								part2 = content1.trim().split("to");
								activity.setFromTime(part2[0].trim());
								//populate toTime
								content1 = content1.replaceAll(part2[0]+"to", "");
								part3 = content1.trim().split("--");		
								activity.setToTime(part3[0].trim());
								// Populate activity : fromDate and toDate
								SimpleDateFormat formatter = new SimpleDateFormat("EEEE MMMM dd, yyyy hh:mm aaa");
								try {
									Date fromDate = formatter.parse(part1[0].trim()+" "+part2[0].trim());
									activity.setFromDate(fromDate);
									Date toDate = formatter.parse(part1[0].trim()+" "+part3[0].trim());
									activity.setToDate(toDate);
								} catch (ParseException e1) {
									e1.printStackTrace();
								}
							}
								
							// Populate activity : address and zipCode
							if (part3[1].contains("Malletts Creek")) {
								activity.setAddress("3090 E. Eisenhower Parkway (The Ann Arbor District Library, "
							                        + part3[1].trim()
							                        + ")");
								activity.setZipCode("48108");
								
							} else if (part3[1].contains("Traverwood")) {
								activity.setAddress("3333 Traverwood Drive (The Ann Arbor District Library, "
				                        + part3[1].trim()
				                        + ")");
								activity.setZipCode("48105");
								
							}  else if (part3[1].contains("West Branch")) {
								activity.setAddress("2503 Jackson Ave (The Ann Arbor District Library, "
				                        + part3[1].trim()
				                        + ")");
								activity.setZipCode("48103");
								
							} else if (part3[1].contains("Pittsfield")) {
								activity.setAddress("2359 Oak Valley Dr. (The Ann Arbor District Library, "
				                        + part3[1].trim()
				                        + ")");
								activity.setZipCode("48103");
								
							} else if (part3[1].contains("Downtown")) {
								activity.setAddress("343 South Fifth Avenue (The Ann Arbor District Library, "
				                        + part3[1].trim()
				                        + ")");
								activity.setZipCode("48104");
								
							} else {
								activity.setZipCode("48105");
							}
						}
						
						activity.setCustomData(nodeId);
						activity.setParser(parser);
						
						activity.setTitle(title);
						activity.setUrl(eventUrl);
						activity.setType(type);
						activity.setCreatedDate(new Date());
						activity.setFromAge(fromAge);
						activity.setToAge(toAge);
						activity.setImageUrl(imageUrl);		
						activity.setImageData(image64Data);
						activity.setDescription(content2);
						activity.setStage(LifeStage.Approved);
						activity.setIsDeletedRecord(false);
						activity.setViewCount(0);
						
						PostProcessing(activity, locationRep);
						
						activities.add(activity);
					}										
				}				
			} catch (IOException e) {				
			}						
		}
		return activities;
	}
	
	public void saveActivity(List<Activity> activities, ActivityRepository activityRep, LocationRepository locationRep) {
		if (activities != null) {
			for (Activity activity : activities) {
				activity = updateLocationAndTime(activity,locationRep);
				if (activityRep.queryExistedActivity(activity.getCreator(),activity.getTitle(),activity.getDescription()) == null)
					activityRep.saveActivity(activity);
			}
		}
	}
	
	public Activity updateLocationAndTime(Activity activity, LocationRepository locationRep) {
		// lookup location from the collection Location;
		Location thisLocation = locationRep.queryLocation(activity.getZipCode(), activity.getCity(), activity.getState());
		// set longitude and latitude 
		if (thisLocation!=null) {
			double[] location = {thisLocation.getLongitude(), thisLocation.getLatitude()};
			activity.setZipCode(thisLocation.getZipCode());
			activity.setLocation(location);
			activity.setCity(thisLocation.getTownship());
			activity.setState(thisLocation.getStateCode());
		}

		SimpleDateFormat formatter = new SimpleDateFormat("EEEE MMMM dd, yyyy hh:mm aaa");
	    String fromDateString = formatter.format(activity.getFromDate());	 
	    String toDateString = formatter.format(activity.getToDate());
	    if (thisLocation.getTimezone() != null) {
	    	if (thisLocation.getTimezone().contains("-5"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    	else if (thisLocation.getTimezone().contains("-6"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/Winnipeg"));
	    	else if (thisLocation.getTimezone().contains("-7"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/Phoenix"));
	    	else if (thisLocation.getTimezone().contains("-8"))
	    		formatter.setTimeZone(TimeZone.getTimeZone("America/Vancouver"));		    	
	    } else {
	    	formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    }	    
	   try {
		   activity.setFromDate(formatter.parse(fromDateString));
		   activity.setToDate(formatter.parse(toDateString));
	   } catch (ParseException e) {
		   e.printStackTrace();
	   }								
		return activity;
	}	
}
