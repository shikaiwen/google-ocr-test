package com.kowa.batch;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OCRHelper {

    public static void imageT1() throws Exception{

    }



    public static String doImageOCR(BufferedImage bufferedImage) throws Exception{

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            // The path to the image file to annotate
//            String fileName = "D:\\shikaiwen\\projects\\mysite\\books\\第三章-20181203T085112Z-001\\第三章\\IMG_20181202_145359.jpg";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write( bufferedImage, "jpg", baos );
            baos.flush();

            byte[] imageInByte = baos.toByteArray();

            // Reads the image file into memory
//            Path path = Paths.get(fileName);
//            byte[] data = Files.readAllBytes(path);

            ByteString imgBytes = ByteString.copyFrom(imageInByte);

            // Builds the image annotation request
            List<AnnotateImageRequest> requests = new ArrayList<>();
            Image img = Image.newBuilder().setContent(imgBytes).build();


            Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();

            requests.add(request);

            // Performs label detection on the image file
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            String fullText = responses.get(0).getFullTextAnnotation().getText();

            return fullText;
//            for (AnnotateImageResponse res : responses) {
//                if (res.hasError()) {
//                    System.out.printf("Error: %s\n", res.getError().getMessage());
//                    return "";
//                }
//
//                for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
//                    annotation.getAllFields().forEach((k, v) ->
//                            System.out.printf("%s : %s\n", k, v.toString()));
//                }
//            }

        }



    }

    public static void main(String... args) throws Exception {

        if(true){
//            for(String key : System.getenv().keySet()){
//                System.out.println(key +":"+ System.getenv().get(key));
//            }
//            String key = "GOOGLE_APPLICATION_CREDENTIALS";
            System.out.println(System.getProperty("os.arch"));
            String key = "SHIK";
            System.out.println(key +":"+ System.getenv().get(key));

//            System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        }

        // Instantiates a client
        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

            // The path to the image file to annotate
            String fileName = "D:\\shikaiwen\\projects\\mysite\\books\\第三章-20181203T085112Z-001\\第三章\\IMG_20181202_145359.jpg";

            // Reads the image file into memory
            Path path = Paths.get(fileName);
            byte[] data = Files.readAllBytes(path);
            ByteString imgBytes = ByteString.copyFrom(data);

            // Builds the image annotation request
            List<AnnotateImageRequest> requests = new ArrayList<>();
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();
            requests.add(request);

            // Performs label detection on the image file
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }

                for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
                    annotation.getAllFields().forEach((k, v) ->
                            System.out.printf("%s : %s\n", k, v.toString()));
                }
            }
        }
    }
}
