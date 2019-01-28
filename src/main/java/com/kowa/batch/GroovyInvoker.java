package com.kowa.batch;


import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class GroovyInvoker {

    public static void main(String[] args) {
        {

            try {

//            ScriptEngineManager factory= new ScriptEngineManager();
//            ScriptEngine engine =factory.getEngineByName("groovy");
//            String HelloLanguage ="def hello(language) {return \"Hello $language\"}";
//            engine.eval(HelloLanguage);
//            Invocable inv =(Invocable) engine;
//            Object[] params = { new String("Groovy") };
//            Object result =inv.invokeFunction("hello", params);



                GroovyClassLoader loader= new GroovyClassLoader();

                String groovyStr = FileUtils.readFileToString(new File("D:\\shikaiwen\\fenghe\\projects\\kowas\\trunk\\kowa-web\\src\\test\\com\\kw\\test\\aa.groovy"),"UTF-8");



                Class groovyClass = loader.parseClass(groovyStr);
                GroovyObject object =(GroovyObject) groovyClass.newInstance();

                Object resultObj = object.invokeMethod("doParse",null);

                System.out.println(resultObj);
            } catch (Exception e) {

                e.printStackTrace();

            }

        }
    }
}
