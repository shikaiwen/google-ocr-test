package com.kowa.batch;

import freemarker.template.Configuration;
import freemarker.template.Template;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HelloController {

    @Resource
    FreeMarkerConfigurer configurer;

    @GetMapping("/")
    public String index() {
        return "front-parser";
    }

    @GetMapping("/hello")
    public String hello(Model model, @RequestParam(value="name", required=false, defaultValue="World") String name) {
        model.addAttribute("name", name);
        return "hello";
    }

    @PostMapping("/getGroovyTemplate")
    @ResponseBody
    public Map getGroovyTemplate(String siteUrl,String titleSelector,String contentSelector) throws Exception{
        Configuration config = configurer.getConfiguration();
        Template pattern1 = config.getTemplate("groovy/pattern1.ftl");
        StringWriter strWriter = new StringWriter();

        Map frootMap = new HashMap();
        frootMap.put("siteUrl", siteUrl);
        frootMap.put("titleSelector", titleSelector);
        frootMap.put("contentSelector", contentSelector);
        pattern1.process(frootMap, strWriter);


        Map<String, Object> httpRootMap = new HashMap<>();
        httpRootMap.put("groovyTempalte", strWriter.toString());
        return httpRootMap;
    }

    @PostMapping("/doParse")
    @ResponseBody
    public Map doParse(String codeCont) throws Exception{

//        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader().getParent();
//        GroovyClassLoader loader = new GroovyClassLoader(parentClassLoader, CompilerConfiguration.DEFAULT, true);
        GroovyClassLoader loader = new GroovyClassLoader();

//        String groovyStr = FileUtils.readFileToString(new File("D:\\shikaiwen\\fenghe\\projects\\kowas\\trunk\\kowa-web\\src\\test\\com\\kw\\test\\aa.groovy"),"UTF-8");
        String groovyStr = codeCont;

//        URLClassLoader urlClassLoader = (URLClassLoader)loader;

//        for (URL url : URLClassLoader.class.cast(parentClassLoader).getURLs()) {
//            System.out.println(url.getPath());
//        }

        Class groovyClass = loader.parseClass(groovyStr);
        GroovyObject object =(GroovyObject) groovyClass.newInstance();

        Object resultObj = object.invokeMethod("doParse",null);


        Map<String, Object> httpRootMap = new HashMap<>();
        httpRootMap.put("parseResult", resultObj);
        return httpRootMap;
    }


    public String getGroovyScript(HttpServletResponse response) throws Exception{

        Configuration config = configurer.getConfiguration();
        Template pattern1 = config.getTemplate("groovy/pattern1.ftl");

        StringWriter strWriter = new StringWriter();
        pattern1.process(new HashMap(), strWriter);

        return strWriter.toString();

    }
}
