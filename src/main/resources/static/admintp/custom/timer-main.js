var moduleObj = {};
$(function(){

    Object.keys(moduleObj).forEach(function(f){
        if(typeof(moduleObj[f]) == "function" && f.indexOf("ready_")==0){
            try{
                moduleObj[f]();
            }catch(e){
                console.log(e);
            }

        }
    });

});


moduleObj.ready_initTimerList = function(){
    $.get("/quartz/joblist", function (datas) {

        datas.forEach(function(item){
            item.triggerStatus  = (item.triggerStatus == "NORMAL" ? true: false);
            item.executeBtnDisabled = false;
        })

        window.tableApp = new Vue({
            el: '#vtable',
            data: {
                jobInfoList: datas
            },
            methods:{
                switchTimer:function(jobinfo,$event){
                    // window.tableApp.jobInfoList.forEach(function(elt){
                    //     elt.triggerStatus = true;
                    // });

                    // $event.preventDefault();


                    this.$http.get("/quartz/changeStatus/"+ jobinfo.triggerKey +"/" + jobinfo.triggerStatus,function(data, status, response){

                    });//.error(function(data, status, response){console.log(response)})




                    // console.log(JSON.stringify(jobinfo))

                    // jobinfo.triggerStatus = true;
                    // var domElt = $(e.target);
                    // var tid = $(domElt).attr("triggerId");
                    // var tval = $(domElt).prop("checked");
                    // $.get("/quartz/changeStatus/"+ tid +"/" + tval,function(rdatas){
                    // });

                     // $(e.target).prop("checked");
                },
                executeNow:function(jobinfo,e){
                    jobinfo.executeBtnDisabled = true;

                    setTimeout(function(){
                        jobinfo.executeBtnDisabled = false;
                    },3000);

                    // var domElt = $(e.target);
                    // $(domElt).addClass("disabled");
                    // var jobKey = $(domElt).attr("jobid");

                    $.get("/quartz/executeNow/"+jobinfo.jobKey,function(datas){

                    })
                }

            }
        })




    });
}
