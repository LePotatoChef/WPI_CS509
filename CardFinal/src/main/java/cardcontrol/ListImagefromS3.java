package cardcontrol;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.Gson;

import http.ListImagesResponse;
import http.ListImagesResponsefromS3;
import db.DatabaseUtil;
import db.ImageElementDAO;
import model.ImageElement;

public class ListImagefromS3 implements RequestStreamHandler {
	
	private AmazonS3 s3 = null;
	
	ObjectListing imageList;
	List<S3ObjectSummary> images;
	List<String> getImages() throws Exception{
		List<String> imageURL = new ArrayList<>();
		if (s3 == null) {
			s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
		}
		imageList = s3.listObjects("cs509finalinteration");

		images=imageList.getObjectSummaries();
		for(S3ObjectSummary s: images)
		{
			String key;
			key=s.getKey();
			System.out.println("https://cs509finalinteration.s3.us-east-2.amazonaws.com/"+key);
			imageURL.add("https://cs509finalinteration.s3.us-east-2.amazonaws.com/"+key);
			
		}
		return imageURL;
	}

/*====================Load all images from RDS========================*/	
	public LambdaLogger logger = null;

/*	List<ImageElement> getImages() throws Exception {
		if (logger != null) { logger.log("in getImages"); }
		ImageElementDAO dao = new ImageElementDAO();
		
		return dao.ListImages();
	}*/
	
	
/*======================Response a JSON file==================================*/		
	@Override
	public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
		logger = context.getLogger();
		logger.log("Loading Java Lambda handler to list all images");

		JSONObject headerJson = new JSONObject();
		headerJson.put("Content-Type", "application/json");  
		headerJson.put("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");
	    headerJson.put("Access-Control-Allow-Origin",  "*");
	        
		JSONObject responseJson = new JSONObject();
		responseJson.put("headers", headerJson);

		ListImagesResponsefromS3 response;
		try {
			List<String> list = getImages();
			response = new ListImagesResponsefromS3(list, 200);
		} catch (Exception e) {
			response = new ListImagesResponsefromS3(403, e.getMessage());
		}

		
        responseJson.put("body", new Gson().toJson(response));  
        responseJson.put("statusCode", response.statusCode);
        
        logger.log("end result:" + responseJson.toJSONString());
        logger.log(responseJson.toJSONString());
        OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
        writer.write(responseJson.toJSONString());  
        writer.close();
	}
}
