<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Image Designer</title>
    <link href="/css/main.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">
    <link href="/jcrop/css/jquery.Jcrop.css" rel="stylesheet">
    <#--<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.3.1/core.js"></script>-->
    <script src="/jcrop/js/jquery.min.js"></script>
    <script src="/jcrop/js/jquery.Jcrop.js"></script>

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
        word-break: break-all;
    }
    .resultCodeDiv{
        /*height:500px;*/
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
<script>

$(function(){


/*    $("#sampleImg").one("load", function() {
        var toSize = 900;
        var originalWidth = $(this).width();
        var originalHeight = $(this).height();

        var ratio = toSize / originalWidth;
        var transferWidth = originalWidth * ratio;
        var transferHeight = originalHeight * ratio;

        $(this).width(transferWidth);
        $(this).height(transferHeight);

    }).each(function() {
        if(this.complete) {
            $(this).load();
        }
    });*/

    function showCoords(c){
        $(".codeDiv").html(JSON.stringify(c))
        window.rect = c;
    }

    $('#sampleImg').Jcrop({
        boxWidth: 900,
        boxHeight: 900,
        onSelect: showCoords,
        onChange: showCoords
    });

    $("#run").click(function () {

        $.post("/doOcrWidthRegion", window.rect ,function (data) {
            $("#titleSelector").val(data.fullText);
            // console.log()
        });
    });


});

</script>
</head>
<body>
    <h2 class="menu">OCR Image Designer</h2>

<div class="container">
    <div class="row">
        <label>URL</label>:<input name="siteUrl" id="siteUrl" value="https://news.shoninsha.co.jp/world/116170">
        <#--<button id="loadScriptBtn">Load Script</button>-->
        <button id="run">doOCR</button>
    </div>
    <div class="row">
        <label>ORC Result</label>:<input id="titleSelector">
    </div>
    <div class="row">
        <div class="codeDiv col-xs-2">
            <#--<textarea id="codeCont" name="codeCont"></textarea>-->

        </div>
        <div class="resultCodeDiv col-xs-8 clearpadding">
            <img id="sampleImg" src="ocr-toprocess.jpg">
        </div>
    </div>


</div>
</body>
</html>