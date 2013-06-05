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
   
    //init BarChart
    var barChart = new $jit.BarChart({
        width: 950,  
        height: 400,        
        //id of the visualization container  
        injectInto: 'infovis',
        //whether to add animations  
        animate: true,  
        //horizontal or vertical barcharts  
        orientation: 'vertical',  
        //bars separation  
        barsOffset: 80,  
        //visualization offset  
        Margin: {  
          top:5,  
          left: 50,  
          right: 50,  
          bottom:10  
        },  
        //labels offset position  
        labelOffset: 5,  
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
    barChart.loadJSON(json);
    //end
    var list = $jit.id('id-list');
    //dynamically add legend to list
    var legend = barChart.getLegend(),
        listItems = [];
    for(var name in legend) {
      listItems.push('<div class=\'query-color\' style=\'background-color:'
          + legend[name] +'\'>&nbsp;</div>' + name);
    }
    list.innerHTML = '<li>' + listItems.join('</li><li>') + '</li>';
    
    //
    // Consumption
    //
    
    var consumptionData = {  
        'label': ['Max VMs in the day', 'Cumulated CPUs hours (GHz)', 'Cumulated RAM hours (GB)', 'Cumulated Storage hours (GB)'],
        'values': [{
                'label': '01/04/2013',
                'values': [2, 5, 170, 200, ],
            }, {
                'label': '02/04/2013',
                'values': [20, 10, 260, 150, ],
            }, {
                'label': '03/04/2013',
                'values': [5, 60, 60, 100, ],
            }, {
                'label': '04/04/2013',
                'values': [2, 80, 80, 90, ],
            }, {
                'label': '05/04/2013',
                'values': [2, 120, 100, 80, ],
            }, {
                'label': '06/04/2013',
                'values': [29, 90, 120, 70, ],
            }, {
                'label': '07/04/2013',
                'values': [45, 80, 60, 100, ],
            }, {
                'label': '08/04/2013',
                'values': [30, 80, 60, 100, ],
            }, {
                'label': '09/04/2013',
                'values': [20, 80, 160, 50, ],
            }, {
                'label': '10/04/2013',
                'values': [12, 100, 190, 100, ],
            }, {
                'label': '11/04/2013',
                'values': [20, 90, 100, 100, ],
        }]  
    };  
    
    //init AreaChart
    var consumptionAreaChart = new $jit.AreaChart({
        width: 930,
        height: 400,
      //id of the visualization container
      injectInto: 'infovis-consumption',
      //add animations
      animate: true,
      //separation offsets
      Margin: {
        top: 5,
        left: 5,
        right: 5,
        bottom: 5
      },
      labelOffset: 10,
      //whether to display sums
      showAggregates: false,
      //whether to display labels at all
      showLabels: true,
      //could also be 'stacked'
      type: useGradients? 'stacked:gradient' : 'stacked',
      //label styling
      Label: {
          type: labelType, //Native or HTML  
          size: 13,  
          family: 'Arial',  
          color: 'black'  
      },
      //enable tips
      Tips: {
        enable: true,
        onShow: function(tip, elem) {
          tip.innerHTML = "<b>" + elem.name + "</b>: " + elem.value;
        }
      },
    });
    
    //load JSON data.
    consumptionAreaChart.loadJSON(consumptionData);
    var listConsumption = $jit.id('id-list-consumption');
    //dynamically add legend to list
    var legendConsumption = consumptionAreaChart.getLegend(),
        listItemsConsumption = [];
    for(var name in legendConsumption) {
      listItemsConsumption.push('<div class=\'query-color\' style=\'background-color:'
          + legendConsumption[name] +'\'>&nbsp;</div>' + name);
    }
    listConsumption.innerHTML = '<li>' + listItemsConsumption.join('</li><li>') + '</li>';
    
    
    //init AreaChart
    var consumptionAreaChart2 = new $jit.AreaChart({
        width: 930,
        height: 400,
      //id of the visualization container
      injectInto: 'infovis-consumption2',
      //add animations
      animate: true,
      //separation offsets
      Margin: {
        top: 5,
        left: 5,
        right: 5,
        bottom: 5
      },
      labelOffset: 10,
      //whether to display sums
      showAggregates: false,
      //whether to display labels at all
      showLabels: true,
      //could also be 'stacked'
      type: useGradients? 'stacked:gradient' : 'stacked',
      //label styling
      Label: {
          type: labelType, //Native or HTML  
          size: 13,  
          family: 'Arial',  
          color: 'black'  
      },
      //enable tips
      Tips: {
        enable: true,
        onShow: function(tip, elem) {
          tip.innerHTML = "<b>" + elem.name + "</b>: " + elem.value;
        }
      },
    });

    //load JSON data.
    consumptionAreaChart2.loadJSON(consumptionData);
    
});
