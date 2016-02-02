package com.siwimi.webparsers.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.siwimi.webparsers.domain.Activity;
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
			if (content.children().size()<2)
				continue;
			
			String title = content.select("div.views-field.views-field-title")
						   .select("span.field-content")
						   .text(); 
			
			String fromDate = content.select("div.views-field.views-field-field-date-full")
						 	  .select("strong.field-content")
						 	  .select("span.date-display-single")
						 	  .select("span.date-display-start")
						 	  .attr("content");
			
			String toDate = content.select("div.views-field.views-field-field-date-full")
				 	  		.select("strong.field-content")
				 	  		.select("span.date-display-single")
				 	  		.select("span.date-display-end")
				 	  		.attr("content");
			
			String ageRange = content.select("div.views-field.views-field-field-ages")
							  .select("div.field-content")
							  .text();
			
			Pattern AgePattern = Pattern.compile("\\d+");
			Matcher m = AgePattern.matcher(ageRange);
			while (m.find()) {
			  System.out.println(m.group());
			}

			
			System.out.println(content.toString());
			
			
			
			
		}
		
		
		return eventsOutput;
	}

		
}
