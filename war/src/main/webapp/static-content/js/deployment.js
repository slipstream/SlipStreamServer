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

function toggleMultiplicity(index) {
    // Set the status of the multiplicity areas
    var multiplicityCheckbox = document.getElementById(index + "--multiplicity--checkbox");
    var multiplicityText = document.getElementById(index + "--multiplicity--text");
    if (multiplicityCheckbox.checked) {
        multiplicityText.removeAttribute("disabled");
    } else {
        multiplicityText.disabled = "true";
    }
}

var mapper = {
	
	createTdWithInput: function(inputName, inputValue, idValue) {
		var value = '';
		var id = '';
		if (idValue) {
			id = ' id="' + idValue + '"';
		}
		if (inputValue) {
			value = inputValue;
		}
		return $('<td><input type="text"' + id + ' name="' + inputName + '" value="' + value + '" /></td>');
	},

	addParameterMapping: function(mappingtable, iparam, oparam) {
		// This method can be called with either a mappingtable element, or the id of the mappingtable element
		// This depends if the method is called by the dynamic handler or by generated HTML.

	    // Check if mappingtable is the table or the id of the table
	    var mappingtableType = typeof mappingtable;
	    var _mappingtable;
	    var mappingtableid;
	    if (mappingtableType == 'string') {
	        _mappingtable = $('#' + mappingtable);
	        mappingtableid = mappingtable;
	    } else {
	        _mappingtable = mappingtable;
	        mappingtableid = mappingtable.attr('id');
	    }
	    var counter = elementCounter;
	    // Increment the counter since we're adding a new param
	    elementCounter += 1;
	    var counter = elementCounter;
	    var id = mappingtableid + '--' + counter;
	    var classattrib;
	    if (counter % 2 == 1) {
	        classattrib = 'odd';
	    } else {
	        classattrib = 'even';
	    }
	    var newtr = $('<tr id="' + id + '" class="' + classattrib + '">');

	    // Input
		var inputName = id + '--input';
	    var newtd = this.createTdWithInput(inputName, iparam);

	    newtd.appendTo(newtr);

	    //Output
	    newtd = this.createTdWithInput(id + '--output', oparam, 'nodeparametermappingoutput');

	    newtd.appendTo(newtr);

	    // Button
	    newtd = $('<td width="7%"><input class="button" type="button" value="Remove" onclick="removeElement(\'' + id + '\');" /></td>');
	    newtd.appendTo(newtr);

	    newtr.appendTo(_mappingtable);
	},

	addParameterMappings: function(inputParameters) {
		if (inputParameters) {
			var inputParameter = inputParameters[0];
			var parameters = $(inputParameter).find('#parameter');
			var iparamtd;
			var oparamtd;
			var defaulttd;
			var input;

			$(parameters).each(function(i) {
			    //retrieve the parameters from the collection
			    var item = parameters[i];
			    var value = item.getAttribute("name");
			    var _default = innerText(item);
			    if (_default != "") {
			        _default = "\"" + _default + "\"";
			    }
			alert($('#mappingtable').attr('id'));
			    this.addParameterMapping($('#mappingtable').attr('id'), value, _default);
			})
		}
	},
	
	add: function(inputParameters) {
		this.addParameterMappings(inputParameters);

        var input = $('<button value="Add Parameter Mapping" onclick="addParameterMapping(\'' + $('#mappingtable').attr('id') + '\'">');
        $('#mappingtable').appendTo($('nodes'));

        alternateTableRows($('nodes'));
	}
}

function addParameterMapping(mappingtable, iparam, oparam) {
	return mapper.addParameterMapping(mappingtable, iparam, oparam);
}

var nodeAdder = {
	
	nodeInfo: {

		nodePrefix: null,
		nodeName: '',
		imageShortName: null,
		imageUrl: null,
		index: -1,
		
		populate: function(image) {
			this.imageShortName = image.attr('shortName');
		},
		
		setNodePrefix: function(index) {
			this.nodePrefix = 'node--' + index;
			this.index = index;
		},
	},
	
	paramInfo: {

		name: null,
		value: null,
		description: "",
		
		populate: function(parameter) {
			this.name = parameter.attr('name');
			this.value = $(parameter).find('value').text().trim();
			if(this.value != "") {
				// Default values for linked properties must be double quoted
				this.value = "'" + this.value + "'";
			}
			this.description = parameter.attr('description');
		},
	},
	
	that: null,
	
	getSingleMappingPart: function(index, name, value) {
	    var nodePrefix = that.nodeInfo.nodePrefix;
		
		return '<tr id="' + nodePrefix + '--mappingtable--' + index + '"> \
			<td> \
				<input name="' + nodePrefix + '--mappingtable--' + index + '--input" value="' + name + '" \
					type="text" /> \
			</td> \
			<td> \
				<input id="nodeparametermappingoutput" name="' + nodePrefix + '--mappingtable--' + index + '--output" value="' + value + '" \
					type="text" /> \
			</td> \
			<td> \
				<input class=button" type="button" \
					onclick="removeElement(\'' + nodePrefix + '--mappingtable--' + index + '\');" \
					value="Remove" /> \
			</td> \
		</tr>'
		
	},
	
	getMappingsPart: function(parameters) {
		var mappings = '';
		var info;
		var i=0;
		parameters.each(function() {
			that.paramInfo.populate($(this));
			mappings += that.getSingleMappingPart(i, that.paramInfo.name, that.paramInfo.value) + '\n';
			i++;
		})

		return '<thead> \
			<tr> \
				<th>Input parameter</th> \
				<th>Linked to</th> \
			</tr> \
		</thead>' + mappings;
	},
	
	getNodePart: function(parameters) {
	    var nodePrefix = that.nodeInfo.nodePrefix;
		var nodeName = that.nodeInfo.nodeName;
		var imageShortName = that.nodeInfo.imageShortName;
		var imageUrl = that.nodeInfo.imageUrl;
		var index = that.nodeInfo.index;
		
		var mappingsPart = that.getMappingsPart(parameters);
		
		var cloudServiceSelect = $('#cloudServiceNamesList').clone(true, true)
		cloudServiceSelect.attr('name', nodePrefix + '--cloudservice--value')
		cloudServiceSelect.attr('id', '')
		

		return '<tr id="' + nodePrefix + '" class="even"> \
			<td> \
				<input id="nodename" name="' + nodePrefix + '--shortname" type="text" value="' + nodeName + '" /> \
			</td> \
			<td> \
				<table> \
					<tr> \
						<td> \
							Reference image: \
							<a href="' + imageUrl + '">' + imageUrl + '</a> \
							<input name="' + nodePrefix + '--imagelink" type="hidden" value="' + imageUrl + '" /> \
						</td> \
						<td style="align:right"> \
							<span> \
								Multiplicity: \
								<input type="text" name="' + nodePrefix + '--multiplicity--value" id="' + nodePrefix + '--multiplicity--value" \
									   size="3" value="1" /> \
							</span> \
						</td> \
						<td style="align:right"> \
							<span> \
								Cloud service: ' + cloudServiceSelect[0].outerHTML + ' \
							</span> \
						</td> \
					</tr> \
				</table> \
				<div> \
					<div class="section_head"> \
						<span>Parameter mappings </span> \
						<input type="hidden" value="' + index + '" id="' + nodePrefix + '--mappingtablecounter" /> \
						<input class="button" type="button" onclick="addParameterMapping(\'' + nodePrefix + '--mappingtable\');" \
							   value="Add Parameter Mapping" /> \
					</div> \
					<table id="' + nodePrefix + '--mappingtable" class="full">' + mappingsPart + '</table> \
				</div> \
			</td> \
			<td> \
				<input class="button" type="button" onclick="removeElement(\'' + nodePrefix + '\'); nodeParametersAutoCompleter.clear();" \
					value="Remove" /> \
			</td> \
		</tr>'
		
	},
	
	callback: function(data, status, xhr) {
        // Assign the XML file to a var
		var module = $(data.firstChild);
        var category = module.attr('category');
        if (category != 'Image') {
            $('#errorText').text("Wrong type!! Expecting 'Image', got: " + category);
            return;
        }

		that = nodeAdder;
		var image = module;
		that.nodeInfo.populate(image);

	    var index = $('#nodes > tr').size() + 1;
		that.nodeInfo.setNodePrefix(index);
		
		var parameters = image.find('parameters > entry > parameter[category="Input"][type!="Dummy"]');
		
		node = $(that.getNodePart(parameters));
        node.appendTo(nodes);

		addAutocompleteToNodeOutputFields();
        alternateTableRows(nodes.id);
    },
    
	add: function() {
		
		$.cookie("Cookie", document.cookie);
		
	    var qname = $('#' + slipstreamns.get('inputid')).attr('value');
		var uri = "module/" + urlEncode(qname);
		var url = "/" + uri;
		this.nodeInfo.imageUrl = uri;
	    $.get(url, nodeAdder.callback, "xml");
	},
}

function addAutocompleteToNodeOutputFields() {
	$('#nodes').children('tr').find('#nodename').change(cleanNodeAutocompleteOnNodeNameChange);
	$('#nodes').children('tr').find('#nodeparametermappingoutput').autocomplete({source: function(term, tags) {nodeParametersAutoComplete(term, tags);}});	
}

function addNode() {
	nodeAdder.add();
}

var nodeParametersAutoCompleter = {
	tags: null,
	tagsByNode: new Object(),
	imageNodeMapping: new Object(),

	update: function() {
		this.tagsByNode = new Object();
		this.imageNodeMapping = new Object();
	
		var images = this.findImageRefs();		
		var qualifiedTags = [];

		var that= this;
		$(images).each(function(index, image) {
			$.ajax({ url: image, success: that.extractOutputParameters, dataType: "xml", async: false });
		});
		$.each(
			that.tagsByNode,
			function(nodeName, tags) {
				$.each(
					tags,
					function(_, tag) {
						qualifiedTags.push(nodeName + ':' + tag)						
					}
				)
			}
		)
		this.tags = qualifiedTags;
	},

    extractImageRefNoVersion: function(imageRef) {
	    var versionPart = imageRef.match('/[0-9]*$');
		if(versionPart === null) {
			return imageRef;
		} else {
			return imageRef.substring(0, imageRef.lastIndexOf(versionPart));
		}
	},

	findImageRefs: function() {
		var that = this;
		var images = [];
		$('#nodes').children('tr').each(function(_, node) {
			var imageRef = that.extractImageRef(node);
			var imageRefNoVersion = that.extractImageRefNoVersion(imageRef);
			var nodeName = that.extractNodeName(node);
			if(!that.imageNodeMapping[imageRefNoVersion]) {
				that.imageNodeMapping[imageRefNoVersion] = [];
			}
			that.imageNodeMapping[imageRefNoVersion].push(nodeName);
			images.push(imageRefNoVersion);
		});
		return this.unique(images);
	},

	unique: function(list) {
        list.sort();
        for(var i = 1; i < list.length; ){
            if(list[i-1] == list[i]){
                list.splice(i, 1);
            } else {
                i++;
            }
        }
        return list;
    },

	extractImageRef: function(node) {
		return $('input[name=' + $(node).attr('id') + '--imagelink]').attr('value');
	},

	extractNodeName: function(node) {
		return $(node).find('input[type=text]').attr('value');
	},

	extractOutputParameters: function(data, status, xhr) {
	    var parameter = $(data.firstChild);
		var imageNameNoVersion = $(parameter).attr('parentUri') + '/' + $(parameter).attr('shortName');
		var inputs = [];
		$(data).find('parameter[category=Output][type!=Dummy]').each(function(index, input) {
			inputs.push($(input).attr('name'));
		});
		$.each(
			nodeParametersAutoCompleter.imageNodeMapping[imageNameNoVersion],
			function(_, node) {
				if(!nodeParametersAutoCompleter.tagsByNode[node]) {
					nodeParametersAutoCompleter.tagsByNode[node] = new Array();
				}
				$.each(
					inputs,
					function(_, tag) {
						nodeParametersAutoCompleter.tagsByNode[node].push(tag);						
					}
					)
			}
			);
	},

 	clear: function() {
		this.tags = null;
	},

	filteredTags: function(request, currentNode) {
		if(this.tags === null) {
			this.update();
		}
		var filtered = [];
		var that= this;
		$.each(
			that.tags,
			function(_, tag) {
				if(tag.startsWith(request.term)) {
					filtered.push(tag);
				}
			});
		return filtered;
	}	
}

function nodeParametersAutoComplete(request, response) {
	response(nodeParametersAutoCompleter.filteredTags(request));
}

function cleanNodeAutocompleteOnNodeNameChange() {
	nodeParametersAutoCompleter.clear();
}



$(document).ready(function() {

	addAutocompleteToNodeOutputFields();
	
	$('#nodes').children('tr').find('#nodename').change(cleanNodeAutocompleteOnNodeNameChange);

		$('#dialog').dialog({
			autoOpen: false,
			width: 600,
			buttons: {
				"Ok": function() {
					$(this).dialog("close");
				},
				"Cancel": function() {
					$(this).dialog("close");
				}
			}
		});
		$('#dialog_link').click(function(){
			$('#dialog').dialog('open');
			return false;
		});
});
