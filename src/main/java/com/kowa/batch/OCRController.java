package com.kowa.batch;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OCRController {

    @Resource
    FreeMarkerConfigurer configurer;

    @GetMapping("/ocrindex")
    public String index() {
        return "img-designer";
    }



    @PostMapping("/doOcrWidthRegion")
    @ResponseBody
    public Map doOcrWidthRegion(@RequestParam Map map){
        try{

            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("static\\ocr-toprocess.jpg");

//            String fileName = "D:\\shikaiwen\\projects\\springboot-freemarker\\src\\main\\resources\\static\\ocr-toprocess.jpg";

            BufferedImage image = ImageIO.read(inputStream);
            Double x = Double.parseDouble(map.get("x").toString());
            Double y = Double.parseDouble(map.get("y").toString());
            Double w = Double.parseDouble(map.get("w").toString());
            Double h = Double.parseDouble(map.get("h").toString());

            BufferedImage partImage = image.getSubimage(x.intValue(),
                    y.intValue(),
                    w.intValue(),
                    h.intValue()
            );

//            ImageIO.write(partImage,"jpg",new File("d://tmp/part.jpg"));


            String fullText = OCRHelper.doImageOCR(partImage);

            Map rootMap = new HashMap<>();
            rootMap.put("fullText", fullText);

            return rootMap;

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }



    @GetMapping("/coremytest")
    @ResponseBody
    public Map hello(Model model, @RequestParam(value="name", required=false, defaultValue="World") String name) {
        try{
            String fileName = "D:\\shikaiwen\\projects\\springboot-freemarker\\src\\main\\resources\\static\\ocr-toprocess.jpg";

            BufferedImage image = ImageIO.read(new File(fileName));

            Map rootMap = new HashMap<>();
            rootMap.put("width", image.getWidth());
            rootMap.put("height", image.getHeight());



            return rootMap;

        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

}
