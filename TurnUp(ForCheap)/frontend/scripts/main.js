function menuChange(ele){
    
    if(ele.id != "drinks"){
        document.getElementById("drinks").style.color="white";
    }
    if(ele.id != "bars"){
        document.getElementById("bars").style.color="white";
    }
    if(ele.id != "specials"){
        document.getElementById("specials").style.color="white";
    }
    if(ele.id != "cheap"){
        document.getElementById("cheap").style.color="white";
    }
    ele.style.color="#99BD74";
    document.getElementById("content").innerHTML=ele.id;

}