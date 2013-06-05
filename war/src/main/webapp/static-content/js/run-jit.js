/*
 * +=================================================================+
 * SlipStream Server (WAR)
 * =====
 * Copyright (C) 2013 SixSq Sarl (sixsq.com)
 * =====
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -=================================================================-
 */
var labelType, useGradients, nativeTextSupport, animate;

(function() {
  var ua = navigator.userAgent,
      iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
      typeOfCanvas = typeof HTMLCanvasElement,
      nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
      textSupport = nativeCanvasSupport 
        && (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
  //I'm setting this based on the fact that ExCanvas provides text support for IE
  //and that as of today iPhone/iPad current text support is lame
  labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
  nativeTextSupport = labelType == 'Native';
  useGradients = nativeCanvasSupport;
  animate = !(iStuff || !nativeCanvasSupport);
})();

function init(){
    //init data
	var getRuntimeValue = function(nodeName, parameterName) {
		return dashboardUpdater.getRuntimeValue(nodeName, parameterName);
	};

	var getGlobalRuntimeValue = function(parameterName) {
		return dashboardUpdater.getGlobalRuntimeValue(parameterName);
	};

	var getRuntimeValueFullName = function(parameterName) {
		return dashboardUpdater.getRuntimeValueFullName(parameterName);
	};

	var cloudServiceNodesMap = function() {
		var map = {};
		var nodeGroups = $("#" + "ss\\:groups").text().split(", ");
		for(var index in nodeGroups) {
			var nodeGroup = nodeGroups[index].trim();
			if(!nodeGroup) {
				continue;
			}
			var parts = nodeGroup.split(":");
			var service = parts[0];
			var node = parts[1];
			var values = map[service] || [];
			values.push(node);
			map[service] = values;
		}
		return map;
	}();

	var createMachine = function() {
		return {name: "machine", id: "id_machine", data: {type: "vm"}};
	};

	var addDeploymentOrchestrator = function() {
		for(var cloudService in cloudServiceNodesMap) {
			var orchestrator = {name: "orchestrator-" + cloudService, id: "id_" + cloudService, data: {type: "orchestrator"}, children: []};
			var nodes = cloudServiceNodesMap[cloudService];
			for(var index in nodes) {
				addNode(nodes[index], $("#" + nodes[index] + "\\.1\\:multiplicity").text(), orchestrator);
			}
			root.children.push(orchestrator);
		}
		return root;
	};

	var addBuildOrchestrator = function() {
		for(var cloudService in cloudServiceNodesMap) {
			var orchestrator = {name: "orchestrator", id: "id_orchestrator", data: {type: "orchestrator"}, children: []};
			orchestrator.children.push(createMachine());
			root.children.push(orchestrator);
		}
		return root;
	};

	var size = function(obj) {
	    var size = 0, key;
	    for (key in obj) {
	        if (obj.hasOwnProperty(key)) size++;
	    }
	    return size;
	}
	// As in running a single VM, as opposed to a deployment or a build
	var isBuild = function() {
		// check for existence of this parameter
		return $("#orchestrator\\:state").length > 0;
	}

	var isDeployment = function() {
		return getGlobalRuntimeValue("category") === 'Deployment';
	}

	var addOrchestrators = function() {
		if(isDeployment()) {
			addDeploymentOrchestrator();
		} else {
			if(isBuild()) {
				addBuildOrchestrator();
			} else {
				root.children.push(createMachine());
			}
		}
		if(root.children.length === 1) {
			// collapse the orchestrator to the root node
			root = root.children[0];
		}
	};

	var addNode = function(nodeName, multiplicity, orchestrator) {
		nodeName = nodeName.trim();
		var node = {name: nodeName, id: "id_" + nodeName, data: {type: "node"}, children: []};
		addVm($("#" + nodeName + "\\.1\\:multiplicity").text(), node);
		orchestrator.children.push(node);
	};

	var addVm = function(multiplicity, node) {
		for(var i=1;i<=multiplicity;i++) {
			node.children.push({name: node.name + "." + i, id: "id_" + node.name + "." + i, data: {type: "vm"}});
		}
	};

	var root = function() {
		return {id: "id_slipstream",
    			name: "slipstream",
				data: {type: "slipstream"},
    			children: []}}();

	addOrchestrators();

	var canvasHeight = 200;
	
	//Increase height for deployment
	if(isDeployment()) {
		canvasHeight = 600;
		var height = canvasHeight + 'px';
		$("#center-container").css('height', height)
		$("#infovis").css('height', height)
	}

    //Create a new ST instance
    var st = new $jit.ST({
        //id of viz container element
        injectInto: 'infovis',
        //set duration for the animation
        duration: 600,
        //set animation transition type
        transition: $jit.Trans.Quart.easeInOut,
        //set distance between node and its children
        levelDistance: 50,
        //enable panning
        Navigation: {
          enable:true,
          panning:true
        },
		offsetX: 300,
		offsetY: 200,
        //set node and edge styles
        //set overridable=true for styling individual
        //nodes or edges
        Node: {
            height: 60,
            width: 200,
            type: 'rectangle',
            color: 'white',
            overridable: true,
        },
        
        Edge: {
            type: 'bezier',
            overridable: true
        },
	    //Add Tips
	    Tips: {
	      enable: true,
	      onShow: function(tip, node) {
			tip.innerHTML = "";
	        //display node info in tooltip
	        if(node.data.type === "slipstream") {
				tip.innerHTML += "<div class=\"tip-text\"><b>global state: " + getGlobalRuntimeValue("state") + "</b></div>";
			}
	        if(node.data.type === "orchestrator") {
				tip.innerHTML += "<div class=\"tip-text\"><b>ip: " + getRuntimeValue(node.name, "hostname") + "</b></div>"
					+ "<div class=\"tip-text\">vm (cloud) state: " + getRuntimeValue(node.name, "vmstate") + "</div>"
					+ "<div class=\"tip-text\">instance id: " + getRuntimeValue(node.name, "instanceid") + "</div>";
			}
	        if(node.data.type === "node") {
				tip.innerHTML += "<div class=\"tip-text\"><b>multiplicity: " + $("#" + node.name + "\\.1\\:multiplicity").text() + "</b></div>";
			}
	        if(node.data.type === "vm") {
				tip.innerHTML += "<div class=\"tip-text\"><b>ip: " + getRuntimeValue(node.name, "hostname") + "</b></div>"
					+ "<div class=\"tip-text\">vm (cloud) state: " + getRuntimeValue(node.name, "vmstate") + "</div>"
					+ "<div class=\"tip-text\">instance id: " + getRuntimeValue(node.name, "instanceid") + "</div>"
					+ "<div class=\"tip-text\">msg: " + getRuntimeValue(node.name, "statecustom") + "</div>";
			}
	      }
        },
        
		isAbort: function(nodeName) {
			return dashboardUpdater.isAbort(nodeName);
		},
		
		nodeCssClass: function(nodeName) {
			var abort = this.isAbort(nodeName);
			return (abort) ? 'dashboard-vm-error' : 'dashboard-vm-ok';
		},

		// Node as in grouping of vms
		nodeNodeCssClass: function(nodeName) {
			return dashboardUpdater.nodeNodeCssClass(nodeName);
		},

		getTruncatedState: function(nodeName) {
			var state = getRuntimeValue(nodeName, 'state');
			return dashboardUpdater.truncate(state);
		},

        //This method is called on DOM label creation.
        //Use this method to add event handlers and styles to
        //your node.
        onCreateLabel: function(label, node){
            label.id = node.id;
            label.innerHTML = "<div><b>" + node.name + "</b></div>";
			var idprefix = "dashboard-" + node.name;

			if(node.data.type === "slipstream") {
			}

			if(node.data.type === "orchestrator") {
				label.innerHTML = "<div id='" + idprefix + "'><div class='" + this.nodeCssClass(node.name) + "' id='" + idprefix + "-icon'/> \
					<ul style='list-style-type:none'> \
						<li id='" + idprefix + "'><b>" + node.name + "</b></li> \
					 	<li id='" + idprefix + "-state'>State: " + this.getTruncatedState(node.name) + "</li> \
					</ul></div>";
			}

			if(node.data.type === "node") {
				label.innerHTML = "<div id='" + idprefix + "'><div class='" + this.nodeNodeCssClass(node.name) + "' id='" + idprefix + "-icon'/> \
					<ul style='list-style-type:none'> \
						<li id='" + idprefix + "'><b>" + node.name + "</b></li> \
			            <li id='" + idprefix + "-ratio'>State: " + dashboardUpdater.truncate(getRuntimeValueFullName(node.name + "\\.1\\:state")) + " (?/" + getRuntimeValueFullName(node.name + "\\.1\\:multiplicity") + ")</div> \
					</ul></div>";
			}

			if(node.data.type === "vm") {
				label.innerHTML = "<div id='" + idprefix + "'><div class='" + this.nodeCssClass(node.name) + "' id='" + idprefix + "-icon'/> \
					<ul style='list-style-type:none'> \
				 		<li id='" + idprefix + "-name'><b>" + node.name + "</b></li> \
				 		<li id='" + idprefix + "-state'>State: " + this.getTruncatedState(node.name) + "</li> \
					 	<li id='" + idprefix + "-statecustom'>" + dashboardUpdater.truncate(getRuntimeValue(node.name, 'statecustom')) + "</li> \
					</ul></div>";
			}

            label.onclick = function(){
            	st.onClick(node.id);
            };

            //set label styles
            var style = label.style;
            style.width = 180 + 'px';
            style.height = 22 + 'px';            
            style.cursor = 'pointer';
            style.color = '#333';
            style.fontSize = '1em';
            style.textAlign= 'left';
            style.paddingLeft = '10px';
            style.paddingTop = '3px';
        },
        
        //This method is called right before plotting
        //an edge. It's useful for changing an individual edge
        //style properties before plotting it.
        //Edge data proprties prefixed with a dollar sign will
        //override the Edge global style properties.
        onBeforePlotLine: function(adj){
            if (adj.nodeFrom.selected && adj.nodeTo.selected) {
                adj.data.$color = "#eed";
                adj.data.$lineWidth = 3;
            }
            else {
                delete adj.data.$color;
                delete adj.data.$lineWidth;
            }
        }
    });
    //load json data
    st.loadJSON(root);
    //compute node positions and layout
    st.compute();
    //optional: make a translation of the tree
    st.geom.translate(new $jit.Complex(-200, 0), "current");
    //emulate a click on the root node.
    st.onClick(st.root);
    //end
}
