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

String.prototype.trim = function() {  
   return this.replace(/^\s+|\s+$/g,"");  
}

$(document).ready(function() {

	setRefresh();

	$('input[value="Terminate"]').click(function(event){
		event.preventDefault();
		SS.hideError();
		background.fadeOutTopWindow();
		$('#terminaterundialog').dialog('open');
		return false;
	});

	$('#terminaterundialog').dialog({
		autoOpen: false,
		title: 'Terminate Virtual Machines',
		buttons: {
			"Terminate": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
				showSubmitMessage();
				$.delete_(window.location.pathname, success=function() {
					window.location.href=window.location.href;
				});
			},
			"Cancel": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
			},
		}
	});

})

var dashboardUpdater = {

	initialState: 'Inactive',
	nodesInfo: {},

	getRuntimeValue: function(nodeName, parameterName) {
		return $("#" + nodeName.replace(".", "\\.") + "\\:" + parameterName).text();
	},

	getGlobalRuntimeValue: function(parameterName) {
		return $("#ss\\:" + parameterName).text();
	},

	getRuntimeValueFullName: function(parameterName) {
		return $("#" + parameterName).text();
	},
	
	isAbort: function(nodeName) {
		return !(this.getRuntimeValue(nodeName, 'abort') === "");
	},

	nodeNodeCssClass: function(nodeName) {
		var globalAbort = !(this.getGlobalRuntimeValue('state') === "");
		var abort = false;
		if(globalAbort) {
			// find if vms under this node are the cause
			var multiplicity = this.getRuntimeValue(nodeName + ".1", "multiplicity");
			for(var i=1;i<=multiplicity;i++) {
				if(this.isAbort(nodeName + "." + i)) {
					abort = true;
					break;
				}
			}
		}
		return (abort) ? 'dashboard-vm-error' : 'dashboard-vm-ok';
	},

    updateProperty: function(propertyName, value) {
		var name = propertyName.replace(':', '\\:').replace('.', '\\.');
		var valueTd = $('#' + name);
        $(valueTd).text(value);
    },

	extractNodeName: function(vmname) {
		return vmname.split('\\.')[0];
	},
	
	updateCompletedNodesInfo: function(nodename, completed) {
		this.nodesInfo[nodename] = this.nodesInfo[nodename] || {};
		var noOfCompleted = this.nodesInfo[nodename].completed || 0;
		if(completed === 'true') {
			noOfCompleted++;
		}
		this.nodesInfo[nodename].completed = noOfCompleted;
	},

	setMultiplicityNodesInfo: function(nodename, multiplicity) {
		this.nodesInfo[nodename].multiplicity = multiplicity;
	},

	getIdPrefix: function(name) {
		return "dashboard-" + name;
	},
	
	getCssClass: function(abort) {
		return (abort) ? 'dashboard-vm-error' : 'dashboard-vm-ok';
	},

    updateVm: function(params) {
		// Update node info (to display the (x/y) in the node dashboard box)
		var vmname = params.name;
		this.updateCompletedNodesInfo(this.extractNodeName(vmname), params.completed);
		if(params.name.endsWith('.1')) {
			this.setMultiplicityNodesInfo(this.extractNodeName(vmname), params.multiplicity);
		}
		
		var idprefix = this.getIdPrefix(params.name);
		
        $('#' + idprefix + '-state').text("State: " + params.state);
        $('#' + idprefix + '-statecustom').text(params.statecustom);

        // Set the icon
        $('#' + idprefix + '-icon').attr('class', this.getCssClass(params.abort));
    },

	updateNode: function(nodename) {
		var idprefix = this.getIdPrefix(nodename);
		var nodeinfo = this.nodesInfo[nodename];
        $('#' + idprefix + '-ratio').text("State: " + this.getRuntimeValue(nodename + '.1', 'state') + " (" + nodeinfo.completed + "/" + nodeinfo.multiplicity + ")");
        // Set the icon
        $('#' + idprefix + '-icon').attr('class', this.nodeNodeCssClass(nodename));
	},

	updateOchestrator: function(nodename) {
		var idprefix = this.getIdPrefix(nodename);
        $('#' + idprefix + '-state').text("State: " + this.getRuntimeValue(nodename, 'state'));
        $('#' + idprefix + '-icon').attr('class', this.getCssClass(this.isAbort(nodename)));
	},

	truncate: function(message) {
	    var maxStringSize = 18;
	    if (message.length > maxStringSize) {
	        var firstPart = message.substr(0, maxStringSize / 2 - 2);
	        var lastPart = message.substr(message.length - maxStringSize / 2 + 2, message.length - 1);
	        message = firstPart + '...' + lastPart;
	    }
		return message;
	},

	buildParamsFromXmlRun: function(vmname, run) {
		var params = {};
		params.name = vmname;
		params.abort = $(run).find("runtimeParameter[key='" + vmname + ":abort']").text();
		params.state = $(run).find("runtimeParameter[key='" + vmname + ":state']").text();
		params.statemessage = $(run).find("runtimeParameter[key='" + vmname + ":statemessage']").text();
		params.statecustom = this.truncate($(run).find("runtimeParameter[key='" + vmname + ":statecustom']").text());
		params.vmstate = $(run).find("runtimeParameter[key='" + vmname + ":vmstate']").text();
		params.completed = $(run).find("runtimeParameter[key='" + vmname + ":complete']").text();
		params.multiplicity = $(run).find("runtimeParameter[key='" + vmname + ":multiplicity']").text();
		return params;
	},

	buildParamsFromLocalRun: function(vmname) {
		var params = {};
		params.name = vmname;
		vmname = vmname.replace(':', '\\:').replace('.', '\\.');
		params.abort = $('#' + vmname + "\\:abort").text();
		params.state = $('#' + vmname + "\\:state").text();
		params.statemessage = $('#' + vmname + "\\:statemessage").text();
		params.statecustom = $('#' + vmname + "\\:statecustom").text();
		params.vmstate = $('#' + vmname + "\\:vmstate").text();
		params.completed = $('#' + vmname + "\\:completed").text();
		params.multiplicity = $('#' + vmname + "\\:multiplicity").text();
		return params;
	},

    updateDashboard: function() {

        var that = this;

		var status = $('#status');

        var callback = function(data, textStatus, jqXHR) {

	        var run = $(data).find("run");
			that.nodesInfo = {};
	
	        // Update general status
	        var newStatus = $(run).attr('status');
	        $(status).text(newStatus);

	        var runtimeParameters = $(run).find('runtimeParameter');
			runtimeParameters.each(function (i, parameter) {
                var key = $(parameter).attr('key');
                var value = $(parameter).text();
                that.updateProperty(key, value);
	        });

			var nodeNames = $(run).attr('nodeNames');
			nodeNames = nodeNames.split(', ');

	        for (var i in nodeNames) {
				var vmname = nodeNames[i].trim().replace('.', '\\.');
				if(vmname === "") {
					continue;
				}
				var params = that.buildParamsFromXmlRun(vmname, run);
	            that.updateVm(params)
	        }

			for (var nodename in that.nodesInfo) {
				if(nodename.startsWith('orchestrator-')) {
					that.updateOchestrator(nodename);
				} else {
					that.updateNode(nodename);
				}
			}
			
			// In case we're dealing with a run or a build
			that.updateOchestrator('orchestrator');
			that.updateVm(that.buildParamsFromXmlRun('machine', run));
			
        };

		$.get(location.href, callback, 'xml');
    },

    stopRefreshing: function() {
        return;
    }
}

function updateDashboard() {
    dashboardUpdater.updateDashboard();
    alternateTableRows(['keyvaluepairstable']);
    setRefresh();
}

var refresher = {

    enableMessage: 'Enable auto refresh',
    disableMessage: 'Disable auto refresh',

    enable: function(refreshFunction) {
        // arg refreshFunction must be a string (e.g. 'afunc()')
        setTimeout(refreshFunction, 10000);
        $('#refresh').text(this.disableMessage);
        this.toggle = this.disable;
    },
    disable: function(refreshFunction) {
        clearTimeout(this.timoutObj);
        $('#refresh').text(this.enableMessage);
        this.toggle = this.enable;
    },
    toggle: function(refreshFunction) {
        return this.enable(refreshFunction);
    },
    setRefresh: function(refreshFunction) {
        var status = $('#status').text();
        if (status === "Finished" || status === "Aborted" || status === "Failed") {
            // Don't refresh
            } else {
            this.enable(refreshFunction);
        }
    }
};
