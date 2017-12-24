package com.hexaware.vedio.Rekognition;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;

public class DetectLabelsExample {

   public static void main(String[] args) throws Exception {

      String photo = "tiger.jpeg";
      String bucket = "cryptogen";

     // AWSCredentials credentials;
      
      BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAIICB24AANU2NT2JQ", "9dgH8r2jr2Q/YqjhVIhRoXIzih2A56xIaXY7nbKF");
    
      AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder
    	         .standard()
    	         .withRegion(Regions.US_WEST_2)
    	         .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
    	         .build();

      DetectLabelsRequest request = new DetectLabelsRequest()
    		  .withImage(new Image()
    		  .withS3Object(new S3Object()
    		  .withName(photo).withBucket(bucket)))
    		  .withMaxLabels(10)
    		  .withMinConfidence(75F);

      try {
         DetectLabelsResult result = rekognitionClient.detectLabels(request);
         List <Label> labels = result.getLabels();

         System.out.println("Detected labels for " + photo);
         for (Label label: labels) {
            System.out.println(label.getName() + ": " + label.getConfidence().toString());
         }
      } catch(AmazonRekognitionException e) {
         e.printStackTrace();
      }
   }
}