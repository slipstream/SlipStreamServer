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

//
// Common section
//

// Extend jQuery with functions for PUT and DELETE requests.
function _ajax_request(url, data, callback, type, method) {
    if (jQuery.isFunction(data)) {
        callback = data;
        data = {};
    }
    return jQuery.ajax({
        type: method,
        url: url,
        data: data,
        success: callback,
        dataType: type
        });
}

jQuery.extend({
    put: function(url, data, callback, type) {
        return _ajax_request(url, data, callback, type, 'PUT');
    },
    delete_: function(url, data, callback, type) {
        return _ajax_request(url, data, callback, type, 'DELETE');
    }
});

// if($.browser.msie) {
//  alert('Sorry this version of SlipStream does not support Internet Explorer. Please use Firefox, Safari or Chrome.');
// }

// String object prototype additions
String.prototype.startsWith = function(str) {
    return (this.match("^" + str) == str);
}

String.prototype.endsWith = function(str) {
    return (this.match(str + "$") == str);
}

String.prototype.trim = function() {
    return (this.replace(/^[\s\xA0]+/, "").replace(/[\s\xA0]+$/, ""));
}

String.prototype.ellipse = function(max) {
    var maxStringSize = max || 18;
    var str = this
    if (str.length > maxStringSize) {
        var firstPart = str.substr(0, maxStringSize / 2 - 2);
        var lastPart = str.substr(str.length - maxStringSize / 2 + 2, str.length - 1);
        str = firstPart + '...' + lastPart;
    }
	return str;
}

// Cross browser function to retrieve element value
function getValue(element) {
    return element.firstChild.text;
}

// Cross browser function to retrieve text node
function getInnerText(element) {
    var text;
    if (document.all) {
        text = element.innerText;
    } else {
        text = element.textContent;
    }
    return text;
}

// Cross browser function to set text node
function setInnerText(element, text) {
    if (document.all) {
        element.innerText = text;
    } else {
        element.textContent = text;
    }
    return text;
}

function httpRequest(method, url, data, contentType, acceptType) {
    var xmlhttp;
    xmlhttp = new XMLHttpRequest();
    xmlhttp.open(method, url, false);
    xmlhttp.setRequestHeader("Content-type", contentType);
    xmlhttp.setRequestHeader("Content-length", data.length);
    xmlhttp.setRequestHeader("Cache-Control", "no-cache");
    if (acceptType) {
        xmlhttp.setRequestHeader("Accept", acceptType);
    }
    xmlhttp.setRequestHeader("Connection", "close");
    xmlhttp.send(data);
    return xmlhttp
}

function httpRequestWithForm(method, url, data) {
    return httpRequest(method, url, data, "application/x-www-form-urlencoded");
}

var hasher = {
    hash: 0,
    hashIt: function() {
        this.hash++;
        return String(this.hash);
    }
};

function setHash() {
    window.location.hash = hasher.hashIt();
}

//
// Navigator section
//
/*
** TODO: refactor this code to use a single counter and write a function to set the class for alternating the 
**       colors in the rows.
*/

var elementCounter = 1000;
// we start with a high enough number such that each element added dynamically won't clash
// with statically created ones
function _delete() {
    alert('Not implemented yet :-(');
}

function _refactor() {
    alert('Not implemented yet :-(');
}

function _save() {
    alert('Not implemented yet :-(');
}

function validate(url) {
    alert('Not implemented yet :-(');
    return;
    // TODO: extract the name/value pair from each input node from the form formid
    //       then build and string out of these pairs
    var form = document.getElementById('formid');
    var input = document.createElement('input');
    input.setAttribute('name', 'validate');
    input.setAttribute('type', 'hidden');
    input.setAttribute('id', 'validateinput');
    form.appendChild(input);
    httpRequestWithForm('post', url, formasstring);
    // Remove the input such that it won't vaidate next time the form is posted
    removeElement('validateinput');
}

function removeElement(id) {
    var element = $('#' + id);
    var parent = element.parent();
    element.remove();

    try {
        alternateTableRows(parent);
    } catch(e) {
    }

    setHash();
}

function addTarget(targetid) {
    var targetElement = document.getElementById(targetid);
    elementCounter += 1;
    var id = targetid + '--' + elementCounter;
    var newtoptr = createElement('tr', 'id=' + id);
    var newtoptd = createElement('td');

    // name
    var newtable = createElement('table');
    var newtr = createElement('tr');
    var newth = createElement('th', 'class=row');
    newth.innerHTML = "Name: ";

    var select = createElement('select', 'name=' + id + '--name');
    var option = document.createElement('option');
    option.innerHTML = "execute";
    select.appendChild(option);
    option = document.createElement('option');
    option.innerHTML = "report";
    select.appendChild(option);
    newth.appendChild(select);
    newtr.appendChild(newth);

    // run in background checkbox
    var newth = createElement('th', 'class=row');
    newth.innerHTML = "Run in the background: ";

    var input = createElement('input', 'type=checkbox', 'name=' + id + '--runinbackground');
    newth.appendChild(input);
    newtr.appendChild(newth);

    // Remove button
    newth = createElement('th', 'align=right');
    var newinput = document.createElement('input');
    newinput.setAttribute('type', 'button');
    newinput.setAttribute('value', 'Remove');
    newinput.setAttribute('onClick', 'removeElement("' + id + '")');
    newth.appendChild(newinput);
    newtr.appendChild(newth);
    newtable.appendChild(newtr);

    // script
    newtr = document.createElement('tr');
    newth = createElement('th', 'class=row');
    newth.innerHTML = "Script:";
    newtr.appendChild(newth);
    newtable.appendChild(newtr);
    newtr = document.createElement('tr');
    newtd = createElement('td', 'class=literal', 'colspan=3');
    newtd.setAttribute('colspan', '2');
    var textarea = createElement('textarea', "type=text", "name=" + id + "--script", "rows=20", "cols=110");
    newtd.appendChild(textarea);
    newtr.appendChild(newtd);
    newtable.appendChild(newtr);
    newtoptd.appendChild(newtable);

    newtoptr.appendChild(newtoptd);
    targetElement.appendChild(newtoptr);

    alternateTableRows(targetid);

    $('targetsthead').show();

    setHash();
}

/**
	 * An alternative function to document.createElement() which accepts name=value pair arguments for specifying attributes
	 * 
	 * @param ele The tagname of the element you want to create
	 * @param [args] The attribute arguments in the format name=value
	 * @return returns a created element with the specified attributes
	 * @author Kyle D. Hayes (contact ~at~ kylehayes ~dot~ info)
	 * @version 0.1, July 4, 2006
	 * usage:
	 * txtbox = createElement('input','type=text','name=myTextBox','value=Enter Text Here','id=myTextBox');
	 * which yields the following element:
	 * <input type="text" name="myTextBox" value="Enter Text Here" id="myTextBox">
	 */
function createElement(ele) {
    var items = createElement.arguments.length;
    element = document.createElement(ele);
    if (items > 1) {
        for (i = 1; i < items; i++) {
            attribute = createElement.arguments[i].split("=");
            element.setAttribute(attribute[0], attribute[1]);
        }
    }
    return element;
}

function alternateTableRows(ids) {

    // Check if ids is an array of id or just one id
    if (ids && typeof ids === 'object' && ids.constructor === Array) {
        for (var i = 0; i < ids.length; i++) {
            alternateSingleTable(ids[i]);
        }
    } else {
        alternateSingleTable(ids);
    }
}

function alternateSingleTable(id) {
	id = null;
	var tag = (id == null) ? 'body' : '#'+id;
	$('tr',$(tag)).not('.instructions').filter(':odd').addClass('odd');
	$('tr',$(tag)).not('.instructions').filter(':odd').removeClass('even');
	$('tr',$(tag)).not('.instructions').filter(':even').addClass('even');
	$('tr',$(tag)).not('.instructions').filter(':even').removeClass('odd');
}

function toggleVisibility(id) {
    var element = document.getElementById(id);
    var img = document.getElementById('toggle-' + id);
    if (element.style.display === 'none') {
        element.style.display = 'block';
        img.setAttribute('src', 'images/common/reduce');
    } else {
        element.style.display = 'none';
        img.setAttribute('src', 'images/common/expand');
    }
    return;
}

var tabActivator = {

    activeTabElement: null,
    activeSectionElement: null,
    defaultSectionName: 'summarysection',
    defaultSectionTabName: 'summarysectionTab',

    initilize: function() {
        // Initialise the elements.  This is needed for a fresh page
        if (!this.activeTabElement) {
            this.activeTabElement = document.getElementById(this.defaultSectionTabName);
        }
        if (!this.activeSectionElement) {
            this.activeSectionElement = document.getElementById(this.defaultSectionName);
        }
    },

    activateTab: function(id) {
        var newSectionElement = document.getElementById(id);
        var newTabElement = document.getElementById(id + 'Tab');

        this.initilize();

        // Don't do anything if the new element is the old one
        if (newSectionElement === this.activeSectionElement) {
            return;
        }

        // Show the new element
        newSectionElement.setAttribute('class', 'visiblesection');
        newTabElement.setAttribute('class', 'tab_head');

        // Hide the old elements, if defined
        this.activeTabElement.setAttribute('class', 'tab_head_inactive');
        this.activeSectionElement.setAttribute('class', 'section');

        // reset the old element
        this.activeSectionElement = newSectionElement;
        this.activeTabElement = newTabElement;

        return;
    }
};

function activateTab(id) {
    tabActivator.activateTab(id);
    return;
}

function urlEncode(url) {
    return url.replace(/ /g, "+");
}

function stripModulePart(qname) {
    var module = qname;
    if (qname.indexOf('/versions/') != -1) {
        module = qname.split('/versions/')[0];
    }
    return module;
}


function login() {
    location.href = '/authn/login?redirectURL=' + location.pathname;
}

var slipstreamns = {
	slipstreamns: window.top.document.slipstreamns,
	
	get: function(key) {
		var value;
		if (window.top.document.slipstreamns) {
			value = window.top.document.slipstreamns[key];
		}
		return value;
	},
	
	set: function(key, value) {
		if (!window.top.document.slipstreamns) {
			window.top.document.slipstreamns = {};
		}
		window.top.document.slipstreamns[key] = value;
	}
}

// var background = {
// 
//  fadeOutTopWindow: function() {
//      $('#topdiv').fadeTo(0, 0.5);
//  },
// 
//  fadeInTopWindow: function() {
//      $('#topdiv', window.top.document).fadeTo(0, 1);
//  }
// }

var chooser = function() {
	
	function fadeOutTopWindow() {
		background.fadeOutTopWindow();
	};

	function fadeInTopWindow() {
		background.fadeInTopWindow();
	};

	function showChooserFrame() {
		$('#chooseriframe').show();
	};

	function hideChooserFrame() {
		$('#chooseriframe', window.top.document).hide();
	};

	function getGlobal(key) {
		return slipstreamns.get(key);
	};

	function setGlobal(key, value) {
		slipstreamns.set(key, value);
	};

	return {
		// the handler is a function, if defined, to be called when a selection is made by the chooser
		showChooser: function(inputid, targetType, handler) {
			$("#chooser").dialog("open");
			return;
			setGlobal('inputid', inputid);
			setGlobal('handler', handler);
			var inputtarget = $('#' + inputid);
			var src = '/' + inputtarget.attr('value');
			if (src === '/') {
			    src = '';
			}
			src = 'module' + urlEncode(src);
			var postfix = '?chooser=' + targetType;
//			$('#chooseriframe').attr('src', src + postfix);
			$('#chooseriframe').load(function() {
				$("#chooser").dialog("open");
//				fadeOutTopWindow();
				$(this).unbind('load');
			});
		},

		selectChooserWithVersion: function() {
		    var modulename = $('#modulename').text();
			var version = $('#moduleversion').text();
		    var targetInput = $('#' + slipstreamns.get('inputid'), window.top.document);
			targetInput.attr('value', modulename + "/" + version);
		    if (getGlobal('handler')) {
		        getGlobal('handler')();
		    }
		    cancelChooser();
			fadeInTopWindow();
		},
		
		selectChooser: function() {
		    var modulename = $('#modulename').text();
		    var targetInput = $('#' + slipstreamns.get('inputid'), window.top.document);
			targetInput.attr('value', modulename);
		    if (getGlobal('handler')) {
		        getGlobal('handler')();
		    }
		    cancelChooser();
			fadeInTopWindow();
		},
		
		cancelChooser: function() {
			hideChooserFrame();
		    fadeInTopWindow();
		}
	    
	};
}();

function showChooser(inputid, targetType, handler) {
	chooser.showChooser(inputid, targetType, handler);
}

function selectChooser() {
	chooser.selectChooser();
}

function selectChooserWithVersion() {
	chooser.selectChooserWithVersion();
}

var logger = function() {
	
	var loggerIframeId = '#loggeriframe';
	var topdiv = '#topdiv';
	
	function fadeOutTopWindow() {
		$(topdiv).fadeTo(0, 0.5);
	};

	function fadeInTopWindow() {
		$(topdiv, window.top.document).fadeTo(0, 1);
	};

	function showFrame() {
		$(loggerIframeId).show();
	};

	function hideFrame() {
		$(loggerIframeId, window.top.document).hide();
	};

	return {
		// the handler is a function, if defined, to be called when a selection is made by the logger
		show: function() {
			$(loggerIframeId).attr('src', 'login?embedded=true');
			showFrame();
			fadeOutTopWindow();
		},

		hide: function() {
			hideFrame();
		    fadeInTopWindow();
		}
	    
	};
}();

function showLogger() {
	logger.show();
};

function hideLogger() {
	logger.hide();
};


// Specific method for setting content in the image reference input field
function hideModuleReferenceChooser() {
    cancelChooser();
    var newselection = document.getElementById("moduleReferenceSelection").value;
    newselection = urlEncode(newselection);
    document.getElementById("moduleReferenceInput").value = newselection;
};

// Specific method for setting content in the block store reference input field
// FIXME: refactor to remove code duplication with hideModuleReferenceChooser
function hideBlockStoreReferenceChooser() {
    cancelChooser("blockStoreReferenceChooserTable");
    var newselection = document.getElementById("blockStoreReferenceSelection").value;
    newselection = urlEncode(newselection);
    document.getElementById("blockStoreReferenceInput").value = urlEncode(newselection);
};

function cancelChooser() {
	chooser.cancelChooser();
};


// Cross browser function to retrieve text node
function innerText(element) {
    var text;
    if (document.all) {
        text = element.innerText;
    } else {
        text = element.textContent;
    }
    return text;
};

//
// Execution section
//
function setRefresh() {
    refresher.setRefresh('updateDashboard()');
};

function toggleRefresh() {
    refresher.toggle();
};

function htmlEncode(value){
  //create a in-memory div, set it's inner text (which jQuery automatically encodes)
  //then grab the encoded contents back out.  The div never exists on the page.
  return $('<div/>').text(value).html();
}

// SlipStream namespace (let's start putting things in here)
var $$ = {
	hideSubmitMessage: function() {
		$('#submit-message').hide();
		$('#overlay').hide();
	},
	showSubmitMessage: function(message) {
	    if (message) {
	        $("#submit-message h2").text(message);
	    }
	    $("#overlay").show();
		$('#submit-message').show();
	},
	extractUrlPath: function() {
		return window.location.pathname;
	},
	show: function(element, message) {
		element.find('span').html(message);
		element.show();
	},
	showError: function(message) {
		hideSubmitMessage();
		show($('#error'), message);
	},
	hideError: function() {
		$('#error').hide();
	},
	extractHtmlOrTextError: function(htmlOrText) {
	    try {
		    var error = $(htmlOrText).find('#errorMessage');
		} catch (Exception) {}
		if(error && error.length > 0) {
			return error.html();
		} else {
			return htmlOrText;
		}
	},
	extractXmlError: function(xml) {
		return $(xml).html();
	},
	extractError: function(data, settings) {
		if(settings.dataType === 'xml') {
			error = this.extractXmlError(data)
		} else {
			error = this.extractHtmlOrTextError(data);
		}
		return error;
	},
	showLogger: showLogger,
	hideLogger: hideLogger,
	send: function($this, event, action, callback){
        //      $(':button[type=submit]').attr('disabled', 'disabled');
        // event.preventDefault();

		var url = $this.attr('action');

		var dataToSend = $this.serialize();

		if(!callback) {
			callback = function(data, status, xhr) {
				var location = xhr.getResponseHeader('Location');
				window.location = location;
		    }
        }

		action(url, dataToSend, callback, 'text');
//	    $(':button[type=submit]').removeAttr('disabled');
	
		return false;
	},
	
	parameterDefaultUpdater: {

		buildNameDefaultValueMap: function(image) {
			var map = {};
			image.find('parameters > entry > parameter').each(function(i, p) {
				map[$(p).attr('name')] = $(p).find('defaultValue').text();
			});
			return map;
		},
		
		findInputByName: function(name) {
			return $('input[name="' + name + '"]');
		},

		findInputByValue: function(value) {
			return $('input[value="' + value + '"]');
		},

		setValueInputPlaceHolderAttribute: function(input, value) {
			$(input).attr('placeholder', value);
		},
			
		extractPartsFromInputName: function(name) {
			var x = $(name);
			var parts = $(name).attr('name').split('-');
			return {category: parts[1], index: parts[3]};
		},
				
		composeValueInputNameFromIndex: function(category, index) {
			return 'parameter-' + category + '--' + index + '--value';
		},
				
		updateDefault: function(inputName, value) {
			var nameInput = this.findInputByValue(inputName);
			var parts = this.extractPartsFromInputName(nameInput);
			var valueInputName = this.composeValueInputNameFromIndex(parts.category, parts.index);
			var valueInput = this.findInputByName(valueInputName);
			this.setValueInputPlaceHolderAttribute(valueInput, value);
		},
		
		callback: function(data, status, xhr) {
	        // Assign the XML file to a var
			var image = $(data.firstChild);

			var that = $$.parameterDefaultUpdater;
			var map = that.buildNameDefaultValueMap(image);
	        for (var key in map) {
				if(map[key]) {
	 				that.updateDefault(key, map[key]);
				}
			}
	    },

		update: function() {
		    var qname = $('#' + slipstreamns.get('inputid')).attr('value');
			var uri = "module/" + urlEncode(qname);
			var url = "/" + uri;
		    $.get(url, $$.parameterDefaultUpdater.callback, "xml");
		},
	},
	
	removeTrFromButton: function(element) {
        $(element).parent().parent().remove();
    },
    
    addParameter: function(element) {
        var table = $(element).prev();
    	var count = $(table).find('tr').size;
        var index = count + 1;

        var id = $(table).attr('id');
        var category = id.split('-')[1];
    	var entryPart = id + "--entry--" + index;

        var newParameter = $('<tr id="' + entryPart + '"> \
        <td> \
        	<input name="parameter-' + entryPart + '--name" value=""> \
        	<input type="hidden" name="parameter-' + entryPart + '--category" value="' + category + '"> \
        	<input type="hidden" name="parameter-' + entryPart + '--type" value="String"> \
        </td> \
        <td> \
        	<input name="parameter-' + entryPart + '--description" value=""> \
        </td> \
        <td> \
            <input name="parameter-' + entryPart + '--value" value="" placeholder=""> \
        </td> \
        <td> \
            <button class="close small_close ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" \
                    role="button" \
                    aria-disabled="false" \
                    onClick="$$.removeTrFromButton(this)"> \
        	    <span class="ui-button-text">Close</span> \
        	</button> \
        </td> \
    </tr>');

        $(table).find('thead').show();

        newParameter.appendTo(table);

        setHash();
    },

    addPackage: function(element) {
        var table = $(element).prev();
    	var count = $(table).find('tr').size;
        var index = count + 1;

        var id = $(table).attr('id');
    	var entryPart = id + "--entry--" + index;

        var newPackage = $('<tr id="' + entryPart + '"> \
		<td> \
			<input name="parameter-' + entryPart + '--name" value=""> \
		</td> \
		<td> \
			<input name="parameter-' + entryPart + '--repository" value=""> \
		</td> \
		<td> \
			<input name="parameter-' + entryPart + '--key" value=""> \
		</td> \
		<td> \
        <button class="close small_close" \
                onClick="$$.removeTrFromButton(this)"> \
    	    <span class="ui-button-text">Close</span> \
    	</button> \
        </td> \
        </tr>');

        $(table).find('thead').show();

        newPackage.appendTo(table);

        setHash();
    },
    
	newModuleRedirect: function($this) {
		var category = $this.attr('name');
		window.location = '/module/' + $('#module-name').text() + "/new?category=" + category;
	},

    decodeHtmlInInstructions: function() {
     $('.instructions > td').each(function(i, node) {
         var value = $(node).text();
             value = value.replace('&lt;', '<').replace('&gt;', '>');
         $(node).html(value);
     });
    },

	fadeOutTopWindow: function() {
		$('#wrapper').fadeTo(0, 0.5);
	},

	fadeInTopWindow: function() {
		$('#wrapper', window.top.document).fadeTo(0, 1);
	}

}

function updateParameterDefaults() {
	$$.parameterDefaultUpdater.update();
}

var SS = $$;

// var hideSubmitMessage = $$.hideSubmitMessage;
// var showSubmitMessage = $$.showSubmitMessage;

$(document).ready(function() {

	$(document).ajaxError(function(e, xhr, settings, exception) {
		if(xhr.status === 401) {
			if($('#loggeriframe').length) {
				if($('#loggeriframe').is(':hidden')) {
					$$.showLogger();
				}
				return;
			}
		}
		var error = xhr.statusText;
		if(xhr.responseText) {
			error = $$.extractError(xhr.responseText, settings);
		}
		$$.hideSubmitMessage();
		$$.show($("#error"), error);
	});

	$(window).unload(function() {
		$$.hideSubmitMessage();
	});
	
	$('.accordion').accordion(
		{ heightStyle: "content",
		  collapsible: true });
	$('.tab_block').tabs();
	$('.add_item','.nodes')
		.button()
		.click(function( event ) {
//			event.preventDefault();
	});
	
	// Logout dialog
	$('#logoutdialog').dialog(
		{ autoOpen: false,
		  modal: true,
		  title: "Logout",
		  buttons: [ { text: "Logout", 
		               click: function() { $( this ).dialog( "close" ); 
		                                   $.delete_("/logout", $.delete_("/logout", success=function() {
                           					   window.location = "/login";
                           				       }));} },
		             { text: "Cancel", 
		               click: function() { $( this ).dialog( "close" ); } }
		           ]
	});
    $( "#logoutlink" ).click(function(event) {
	    event.preventDefault();
        $( "#logoutdialog" ).dialog( "open" );
    });
    
    // Convert instruction strings into HTML in the page 
	$$.decodeHtmlInInstructions();

	$('.instructions').click(function(event){
		event.preventDefault();
		var parentTr = $(this).closest('tr');
		var classTr = parentTr.attr('class');
		var instructiondTr = parentTr.next('.instructions');
		instructiondTr.addClass(classTr);
		instructiondTr.toggle();
	});
	
	$('.ui-widget-overlay, .please_wait').hide();
})


