package com.hexaware.vedio.Rekognition;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.GetLabelDetectionRequest;
import com.amazonaws.services.rekognition.model.GetLabelDetectionResult;
import com.amazonaws.services.rekognition.model.LabelDetection;
import com.amazonaws.services.rekognition.model.LabelDetectionSortBy;
import com.amazonaws.services.rekognition.model.NotificationChannel;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.StartLabelDetectionRequest;
import com.amazonaws.services.rekognition.model.StartLabelDetectionResult;
import com.amazonaws.services.rekognition.model.Video;
import com.amazonaws.services.rekognition.model.VideoMetadata;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VideoDetect {
    private static AmazonSNS sns = null;
    private static AmazonSQS sqs = null;
    private static AmazonRekognition rek = null;
    private static NotificationChannel channel= new NotificationChannel()
            .withSNSTopicArn("TopicArn")
            .withRoleArn("RoleArn");
     
    private static String queueUrl =  "QueueURL";
    private static String startJobId = null;




   public static void main(String[] args)  throws Exception{


   AWSCredentials credentials;
   
  

   try {
	    credentials = new BasicAWSCredentials("AKIAIICB24AANU2NT2JQ", "9dgH8r2jr2Q/YqjhVIhRoXIzih2A56xIaXY7nbKF");
   } catch (Exception e) {
      throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
         + "Please make sure that your credentials file is at the correct "
         + "location (/Users/userid>.aws/credentials), and is in valid format.", e);
   }

    sns = AmazonSNSClientBuilder
      .standard()
      .withRegion(Regions.US_WEST_2)
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .build();

   sqs = AmazonSQSClientBuilder
                  .standard()
                  .withRegion(Regions.US_WEST_2)
                  .withCredentials(new AWSStaticCredentialsProvider(credentials))
                  .build();

       
   rek = AmazonRekognitionClientBuilder.standard().withCredentials( new ProfileCredentialsProvider("rathinamoorthy"))
    .withEndpointConfiguration(new EndpointConfiguration(AmazonRekognitionClient.ENDPOINT_PREFIX, "US_WEST_2")).build();  
   
   
   //=================================================
   StartLabels("cryptogen", "VID_20171116_201445827.mp4");
   //=================================================
   System.out.println("Waiting for job: " + startJobId);
   //Poll queue for messages
   List<Message> messages=null;
   int dotLine=0;
   boolean jobFound=false;

   //loop until the job status is published. Ignore other messages in queue.
   do{
        //Get messages.
       do{
          messages = sqs.receiveMessage(queueUrl).getMessages();
          if (dotLine++<20){
          System.out.print(".");
       }else{
          System.out.println();
          dotLine=0;
          }
       }while(messages.isEmpty());

       System.out.println();

       //Loop through messages received.
       for (Message message: messages) {
          String notification = message.getBody();

          // Get status and job id from notification.
          ObjectMapper mapper = new ObjectMapper();
          JsonNode jsonMessageTree = mapper.readTree(notification);
          JsonNode messageBodyText = jsonMessageTree.get("Message");
          ObjectMapper operationResultMapper = new ObjectMapper();
          JsonNode jsonResultTree = operationResultMapper.readTree(messageBodyText.textValue());
          JsonNode operationJobId = jsonResultTree.get("JobId");
          JsonNode operationStatus = jsonResultTree.get("Status");
          System.out.println("Job found was " + operationJobId);
          // Found job. Get the results and display.
          if(operationJobId.asText().equals(startJobId)){
              jobFound=true;
              System.out.println("Job id: " + operationJobId );
              System.out.println("Status : " + operationStatus.toString());
             if (operationStatus.asText().equals("SUCCEEDED")){
                //============================================
                GetResultsLabels();
                //============================================
             }
            else{
                   System.out.println("Video analysis failed");
             }

                sqs.deleteMessage(queueUrl,message.getReceiptHandle());
             }

             else{
                System.out.println("Job received was not job " +  startJobId);
              }
           }
    } while (!jobFound);


   System.out.println("Done!");
   }


   private static void StartLabels(String bucket, String video) throws Exception{

       StartLabelDetectionRequest req = new StartLabelDetectionRequest()
               .withVideo(new Video()
                       .withS3Object(new S3Object()
                           .withBucket(bucket)
                           .withName(video)))
               .withMinConfidence(50F)
               .withJobTag("DetectingLabels")
               .withNotificationChannel(channel);


       StartLabelDetectionResult startLabelDetectionResult = rek.startLabelDetection(req);
       startJobId=startLabelDetectionResult.getJobId();

   }

   private static void GetResultsLabels() throws Exception{

       int maxResults=10;
       String paginationToken=null;
       GetLabelDetectionResult labelDetectionResult=null;

       do {
               if (labelDetectionResult !=null){
                   paginationToken = labelDetectionResult.getNextToken();
               }

               GetLabelDetectionRequest labelDetectionRequest= new GetLabelDetectionRequest()
                   .withJobId(startJobId)
                   .withSortBy(LabelDetectionSortBy.TIMESTAMP)
                   .withMaxResults(maxResults)
                   .withNextToken(paginationToken);


               labelDetectionResult = rek.getLabelDetection(labelDetectionRequest);

           VideoMetadata videoMetaData=labelDetectionResult.getVideoMetadata();

           System.out.println("Format: " + videoMetaData.getFormat());
           System.out.println("Codec: " + videoMetaData.getCodec());
           System.out.println("Duration: " + videoMetaData.getDurationMillis());
           System.out.println("FrameRate: " + videoMetaData.getFrameRate());


           //Show labels, confidence and detection times
           List<LabelDetection> detectedLabels= labelDetectionResult.getLabels();

           for (LabelDetection detectedLabel: detectedLabels) {
               long seconds=detectedLabel.getTimestamp()/1000;
               System.out.print("Sec: " + Long.toString(seconds) + " ");
                System.out.println("\t" + detectedLabel.getLabel().getName() +
                   "     \t" +
                   detectedLabel.getLabel().getConfidence().toString());
                   System.out.println();
           }
       } while (labelDetectionResult !=null && labelDetectionResult.getNextToken() != null);

   }
}