$(document).ready(function() {

    var labelType, useGradients, nativeTextSupport, animate;

    (function() {
        var ua = navigator.userAgent,
            iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
            typeOfCanvas = typeof HTMLCanvasElement,
            nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
            textSupport = nativeCanvasSupport && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
        //I'm setting this based on the fact that ExCanvas provides text support for IE
        //and that as of today iPhone/iPad current text support is lame
        labelType = (!nativeCanvasSupport || (textSupport && !iStuff)) ? 'Native' : 'HTML';
        nativeTextSupport = labelType == 'Native';
        useGradients = nativeCanvasSupport;
        animate = !(iStuff || !nativeCanvasSupport);
    })();

    var json = {
        'label': ['VMs', 'CPUs - GHz', 'RAM - GB', 'Storage - GB'],
        'values': [{
            'label': 'CloudSigma',
            'values': [2,80,160,200,],
        }, {
            'label': 'Interoute VDC',
            'values': [100,80,40,10,],
        }, {
            'label': 'Atos Science Cloud',
            'values': [200,90,20,2],
        }]

    };

    //init PieChart
    var pieChart = new $jit.PieChart({
        //id of the visualization container  
        injectInto: 'infovis-user-a',  
        //whether to add animations  
        animate: true,  
        //horizontal or vertical barcharts  
        orientation: 'vertical',  
        //bars separation  
        barsOffset: 80,  
        //visualization offset  
        Margin: {  
          top:0,  
          left: 0,  
          right: 0,  
          bottom: 0  
        },  
        //labels offset position  
        labelOffset: 50,
        offset: 55,  
        //bars style  
        type: useGradients? 'stacked:gradient' : 'stacked',  
        //whether to show the aggregation of the values  
        showAggregates:false,  
        //whether to show the labels for the bars  
        showLabels:true,  
        //labels style  
        Label: {  
          type: labelType, //Native or HTML  
          size: 13,  
          family: 'Arial',  
          color: 'black'  
        },  
        //add tooltips  
        Tips: {  
          enable: true,  
          onShow: function(tip, elem) {  
            tip.innerHTML = "<b>" + elem.name + "</b>: " + elem.value;  
          }  
        }
    });
    //load JSON data.
    pieChart.loadJSON(json);
    //end
    var list = $jit.id('id-list');
    //dynamically add legend to list
    var legend = pieChart.getLegend(),
        listItems = [];
    for(var name in legend) {
      listItems.push('<div class=\'query-color\' style=\'background-color:'
          + legend[name] +'\'>&nbsp;</div>' + name);
    }
    list.innerHTML = '<li>' + listItems.join('</li><li>') + '</li>';


    var json2 = {
        'label': ['VMs', 'CPUs - GHz', 'RAM - GB', 'Storage - GB'],
        'values': [{
            'label': 'CloudSigma',
            'values': [6,80,100,150,],
        }, {
            'label': 'Interoute VDC',
            'values': [200,90,20,2],
        }, {
            'label': 'Atos Science Cloud',
            'values': [50,80,70,10,],
        }]

    };


    //init PieChart
    var pieChart2 = new $jit.PieChart({
        height: 400,
        //id of the visualization container  
        injectInto: 'infovis-user-b',  
        //whether to add animations  
        animate: true,  
        //horizontal or vertical barcharts  
        orientation: 'vertical',  
        //bars separation  
        barsOffset: 80,  
        //visualization offset  
        Margin: {  
          top:0,  
          left: 0,  
          right: 0,  
          bottom: 0  
        },  
        //labels offset position  
        labelOffset: 50,
        offset: 55,  
        //bars style  
        type: useGradients? 'stacked:gradient' : 'stacked',  
        //whether to show the aggregation of the values  
        showAggregates:false,  
        //whether to show the labels for the bars  
        showLabels:true,  
        //labels style  
        Label: {  
          type: labelType, //Native or HTML  
          size: 13,  
          family: 'Arial',  
          color: 'black'  
        },  
        //add tooltips  
        Tips: {  
          enable: true,  
          onShow: function(tip, elem) {  
            tip.innerHTML = "<b>" + elem.name + "</b>: " + elem.value;  
          }  
        }
    });
    //load JSON data.
    pieChart2.loadJSON(json2);
    //end
});
