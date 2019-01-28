<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Spring Boot Hello World Example with FreeMarker</title>
    <link href="/css/main.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">

    <#--<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.3.1/core.js"></script>-->
    <script
            src="https://code.jquery.com/jquery-1.12.4.js"
            integrity="sha256-Qw82+bXyGq6MydymqBxNPYTaUXXq7c8v3CwiYwLLNXU="
            crossorigin="anonymous"></script>
    <script src="/js/main.js"></script>

    <script type="text/javascript" src="/ueditor/ueditor.config.js"></script>
    <script type="text/javascript" src="/ueditor/ueditor.all.js"></script>
    <script type="text/javascript" src="/ueditor/lang/zh-cn/zh-cn.js"></script>



<style>
    .menu{
        width:100%;
        color:white;
        text-align:center;
        background:black;
        margin-top:0px;
    }
    .codeDiv{
        height:500px;
    }
    .resultCodeDiv{
        height:500px;
        /*border: 1px solid #c3c0c0;*/
    }

    #editorCnt{
    }
    textarea{
        height:100%;
        width:100%;
    }
    #siteUrl{
        width:400px;
        margin-bottom: 20px;
    }

    .row{
        margin-bottom:5px;
    }
</style>
    <!-- 实例化编辑器 -->
    <script type="text/javascript">
        var editor = UE.getEditor('editorCnt');
    </script>
<script>

$(function(){
    $("#run").click(function () {
       var sendData = {}
       sendData.codeCont = $("#codeCont").val()

        $.post("/doParse", sendData, function (data) {

            // $(".resultCodeDiv").html(JSON.stringify(data.parseResult));
            // $("#docCont").val(JSON.stringify(data.parseResult));
            var newLine = "<br/>";
            var title = "<h2>"+ data.parseResult.title +"</h2>";
            var content = data.parseResult.content.replace(/\n/g,"<br>")
            editor.setContent(title + newLine + content);
        });
    });

    $("#loadScriptBtn").click(function () {
        var siteUrl = $("#siteUrl").val();
        var titleSelector = $("#titleSelector").val();
        var contentSelector = $("#contentSelector").val();
        var sendData = {
            "siteUrl": siteUrl,
            "titleSelector":titleSelector,
            "contentSelector":contentSelector
        };
        $.post("/getGroovyTemplate",sendData, function (data) {
            $("#codeCont").text(data.groovyTempalte);
        });
    });




})
</script>
</head>
<body>
    <h2 class="menu">Parser</h2>

<div class="container">
    <div class="row">
        <label>URL</label>:<input name="siteUrl" id="siteUrl" value="https://news.shoninsha.co.jp/world/116170">
        <button id="loadScriptBtn">Load Script</button>
        <button id="run">Run Script</button>
    </div>
    <div class="row">
        <label>タイトル css Selector</label>:<input id="titleSelector">
        <label>文章 css Selector</label>:<input id="contentSelector">
    </div>
    <div class="row">
        <div class="codeDiv col-xs-6">
            <textarea id="codeCont" name="codeCont"></textarea>
        </div>
        <div class="resultCodeDiv col-xs-6 clearpadding">
            <div id="editorCnt"></div>
        </div>
        <#--<textarea id="docCont" name="docCont"></textarea>-->
    </div>



</div>
</body>
</html>