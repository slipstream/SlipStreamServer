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

$(document).ready(function() {

	alternateTableRows();

	$.ajaxSetup ({
	    // Disable caching of AJAX responses
	    cache: false
	});

	$(window.top.document).slipstreamns = {};

	$(window).unload(function() {
		$$.hideSubmitMessage();
	});

	var show = function(element, message) {
		element.find('span').html(message);
		element.show();
	}

	var showError = function(message) {
		hideSubmitMessage();
		show($('#error'), message);
	}

	var hideError = function() {
		$('#error').hide();
	}

	var extractError = function(data, settings) {
		if(settings.dataType === 'xml') {
			error = extractXmlError(xhr.responseText)
		} else {
			error = extractHtmlOrTextError(data);
		}
		return error;
	}
	
	var extractXmlError = function(xml) {
		return $(xml).html();
	}

	var extractHtmlOrTextError = function(htmlOrText) {
		var error = $(htmlOrText).find('#errorMessage');
		if(error.length > 0) {
			return error.html();
		} else {
			return htmlOrText;
		}
	}

	var extractUrlPath = function() {
		return window.location.pathname;
	}

	var extractUrlPathLast = function() {
		var path = extractUrlPath();
		return path.substring(path.lastIndexOf("/") + 1, path.length);
	}

	var extractUrlPathRoot = function() {
		var path = extractUrlPath();
		return path.substring(0, path.lastIndexOf("/"));
	}

	var extractNewName = function(){
		return $("#modulename").attr('value');
	}
	
	var isNameNew = function() {
		return (extractUrlPathLast() === "new");
	}

	var validate = function() {
		if(!extractNewName()) {
			var error = "Missing module name";
			showError(error);
			throw error;
		}
	}

	var send = function($this, event, action, callback){
		return $$.send($this, event, action, callback)
	};

	$('#formlogin').submit(function(event){
		var callback = function(data, status, xhr) {
			if(window.location.search) {
				var query = window.location.search;
				window.location = query.substring(query.lastIndexOf("=") + 1, query.length);
			}
			hideLogger();
		}
		return send($(this), event, $.post, callback);
	});

	$('#formsave').submit(function(event){
		event.preventDefault();
		validate();
		return send($(this), event, $.put);
	});

//	$('#formregister').css('background-color','blue');

	$('form[name="formexec"]').submit(function(event){
		event.preventDefault();
		return send($(this), event, $.post);
	});
	
	$('input[value="Cancel"]').click(function(event){
		if(isNameNew()) {
			window.location = extractUrlPathRoot();
		} else {
			window.location = extractUrlPath();
		}
	});
	
	$('input[value="Delete"]').click(function(event){
		event.preventDefault();
		return send($(this), event, $.delete_);
	});

	$('input[value="Refactor"]').click(function(event){
		showError("Refactoring is not supported yet");
	});

	$('input[value="New Project"]').click(function(event){
		newModuleRedirect($(this));
	});

	$('input[value="New Machine Image"]').click(function(event){
		newModuleRedirect($(this));
	});

	$('input[value="New Deployment"]').click(function(event){
		newModuleRedirect($(this));
	});

	$('input[value="New Disk Image"]').click(function(event){
		newModuleRedirect($(this));
	});

	var newModuleRedirect = function($this) {
		var category = $this.attr('name');
		window.location = 'module/' + $('#modulename').text() + "/new?category=" + category;
	};

	var decodeHtmlInInstructions = function() {
		$('.instructions > td').each(function(i, node) {
			var value = $(node).text();
	  		value = value.replace('&lt;', '<').replace('&gt;', '>');
			$(node).html(value);
		});
	} 

	decodeHtmlInInstructions();

	$('.instructions').click(function(event){
		event.preventDefault();
		var parentTr = $(this).closest('tr');
		var classTr = parentTr.attr('class');
		var instructiondTr = parentTr.next('.instructions');
		instructiondTr.addClass(classTr);
		instructiondTr.toggle();
	});

	$('.linkedinstructions').click(function(event){
		event.preventDefault();
		var parentTr = $(this).closest('tr');
		var classTr = parentTr.attr('class');
		var instructiondTr = parentTr.next('.linkedinstructions');
		instructiondTr.addClass(classTr);
		instructiondTr.toggle();
	});

	// Resize the td containing the question mark image
	$('a.instructions').parent().css('width', 24);
	
    $('input').watermark();
    
})
