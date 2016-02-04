package com.siwimi.webparsers.parser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.siwimi.webparsers.domain.Activity;
import com.siwimi.webparsers.domain.Activity.Category;
import com.siwimi.webparsers.domain.Activity.LifeStage;
import com.siwimi.webparsers.repository.ActivityRepository;
import com.siwimi.webparsers.repository.LocationRepository;

public class LeslieSciCenterParser implements Parser {
	
	@Override
	public List<Activity> getEvents(String eventsSourceUrl, String parser, LocationRepository locationRep, ActivityRepository activityRep) {
		
		/*
		 *	All of the event data from this website is contained in its Javascript variable. 
		 */
		
		// Step 1: Initialize States
		List<Activity> eventsOutput = new ArrayList<Activity>();
		String defaultZipCode = "48105";
		String defaultAddress = "1831 Traver Road"; 		
		String defaultEventHostUrl = "http://www.lesliesnc.org/";
		String defaultEventImgUrl = "http://www.lesliesnc.org/sites/default/files/lsnc_logo.jpg";
			
		// Step 2: Retrieve HTML Page
		Document doc = null;
		try {
			doc = Jsoup.connect(eventsSourceUrl).get();
		} catch (IOException e) {
			doc = null;
			e.printStackTrace();
		}				
		
		if (doc == null)
			return eventsOutput;
		
		// Step 3: Retrieve contents Elements
		Elements contents = doc.select("div#page-wrapper")
				 		   .select("div#page")
				 		   .select("div#main-wrapper")
				 		   .select("div#main")
				 		   .select("div#content")
				 		   .select("div.section")
				 		   .select("div.region.region-content")
				 		   .select("div#block-system-main")
				 		   .select("div.content")
				 		   .first()
				 		   .child(0)
				 		   .select("div.view-content")
				 		   .first()
				 		   .children();
		if (contents.size()<1)
			return eventsOutput;
		
		// Step 4: Loop through the elements list - each look will add an event			
		for (Element content : contents) {
			
			int errorCode = 0;
			
			if (content.children().size()<1)
				continue;
			
			// Induce title
			String title = content.select("div.views-field.views-field-title")
						   .select("span.field-content")
						   .text().trim(); 
			if (title == null)
				errorCode += ErrorCode.NoTitle.getValue();
			else if (title.isEmpty())
				errorCode += ErrorCode.NoTitle.getValue();
			
			// Induce fromDate
			Date fromDate = null;
			String fromDateString = content.select("div.views-field.views-field-field-date-full")
						 	        .select("strong.field-content")
	//					 	        .select("span.date-display-single")
						 	        .select("span.date-display-start")
						 	        .attr("content");
			if (fromDateString == null) {
				errorCode += ErrorCode.NoFromDate.getValue();
				errorCode += ErrorCode.NoFromTime.getValue();
			} else if (fromDateString.isEmpty()) {
				errorCode += ErrorCode.NoFromDate.getValue();
				errorCode += ErrorCode.NoFromTime.getValue();
			} else {
				SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
				isoFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
				try {
					fromDate = isoFormat.parse(fromDateString);
				} catch (ParseException e) {
					errorCode += ErrorCode.NoFromDate.getValue();
					errorCode += ErrorCode.NoFromTime.getValue();
					e.printStackTrace();
				}		
			}
							
			// Induce fromTime
			String fromTime = null;
			if (fromDate == null) {
				errorCode += ErrorCode.NoFromTime.getValue();
			} else {
				try {
					fromTime = getHM(fromDate);
				} catch (Exception e2) {
					errorCode += ErrorCode.NoFromTime.getValue();
				}					
			}
			
			// Induce toDate
			Date toDate = null;
			String toDateString = content.select("div.views-field.views-field-field-date-full")
				 	  		      .select("strong.field-content")
	//			 	  		      .select("span.date-display-single")
				 	  		      .select("span.date-display-end")
				 	  		      .attr("content");
			try {
				SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
				isoFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
				toDate = isoFormat.parse(toDateString);
			} catch (ParseException e) {
				errorCode += ErrorCode.NoFromDate.getValue();
				errorCode += ErrorCode.NoFromTime.getValue();
				e.printStackTrace();
			}				
			// Induce toTime
			String toTime = null;
			if (toDate != null) {
				toTime = getHM(toDate);
			}
						
			// Induce fromAge and toAge
			String ageRange = content.select("div.views-field.views-field-field-ages")
					  	      .select("div.field-content")
					          .text();
			Pattern AgePattern = Pattern.compile("\\d+");
			Matcher m = AgePattern.matcher(ageRange);
			List<Integer> ages = new ArrayList<Integer>();
			while (m.find())
				ages.add(new Integer(m.group()));
			int fromAge = 0;
			int toAge = 99;
			if (ages.size() == 2) {
				fromAge = ages.get(0).intValue();
				toAge = ages.get(1).intValue();
			} else if (ages.size() == 1) {
				fromAge = ages.get(0).intValue();
			}
			
			// Induce description
			Elements fieldBodies = content.select("div.views-field.views-field-body")
					   		   	  .select("div.field-content")
					   		      .first()
					   		      .children();
			String description = "";
			for (Element e : fieldBodies)
				description += Jsoup.parse(e.text()).text();
			if (description.isEmpty())
				errorCode += ErrorCode.NoDescription.getValue();
			
			// Induce price
			String cost = content.select("span.views-field.views-field-field-cost")
					   	  .select("span.field-content")
					      .text().trim();
			List<Integer> prices = new ArrayList<Integer>();
			int price = 0;
			if (!cost.equals("Free")) {
				Pattern PricePattern = Pattern.compile("\\d+");
				Matcher m2 = PricePattern.matcher(cost);
				while (m2.find())
					prices.add(new Integer(m2.group()));
				if (!prices.isEmpty())
					price = prices.get(0);
			}
			
			// Induce custom data
			String customData = "";
			if (fromDate != null)
				customData += fromDate;
			if (title != null)
				customData += title;
			
			// customData is guaranteed unique, so let's check it with the database.
			if (activityRep.isExisted(customData, parser)) {
				continue;
			}	
			
			// Induce category
			Category category = Category.animal;
			if (title != null)
				if (title.toLowerCase().contains("science"))
					category = Category.science;

			// It's a good idea to create the event object and set the fields at the same place
			// So in future if we want to add new field or delete a field, we always know to locate
			// at the bottom of this getEvents function.
			Activity newEvent = new Activity();
			newEvent.setIsDeletedRecord(false);		
			newEvent.setCreatedDate(new Date());
			newEvent.setCustomData(customData);
			newEvent.setParser(parser);
			newEvent.setUrl(defaultEventHostUrl);
			newEvent.setTitle(title);
			newEvent.setType(category);
			newEvent.setFromDate(fromDate);
			newEvent.setFromTime(fromTime);
			newEvent.setFromAge(fromAge);
			newEvent.setToDate(toDate);
			newEvent.setToTime(toTime);
			newEvent.setToAge(toAge);
			newEvent.setDescription(description);
			newEvent.setAddress(defaultAddress);
			newEvent.setZipCode(defaultZipCode);
			newEvent.setPrice(price);
            newEvent.setStage(LifeStage.Approved);
            errorCode += this.setImage(newEvent, defaultEventImgUrl);
			newEvent.setErrorCode(errorCode);
				
			// Every event must run this function after event object is created.
			// Basically it'll populate some necessary data such as coordinates and time zone.
			PostProcessing(newEvent, locationRep);
			
			eventsOutput.add(newEvent);
		}
		
		
		return eventsOutput;
	}
	
	private String getHM(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		int hour = c.get(Calendar.HOUR);
		int minute = c.get(Calendar.MINUTE);
		int am_pm = c.get(Calendar.AM_PM);
		
		//The format string %02d formats an integer with leading zeros 
		//(that's the 0 in %02d), always taking up 2 digits of width
		String time = String.format("%02d:%02d", hour, minute);;
		switch (am_pm) {
	    	case Calendar.AM:
	    		time += " AM";
	        	break;
	    	default:
	    		time += " PM";
	    		break;
		}
		
		return time;
	}
		
}
