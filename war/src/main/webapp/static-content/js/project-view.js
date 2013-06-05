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

    // Edit button
    $('#edit-button-top, #edit-button-bottom').click(function(event){
		window.location = '?edit=true';
    });	

    // New project button
    $('#new-project-button-top, #new-project-button-bottom').click(function(event){
		$$.newModuleRedirect($(this));
    });	

    // New image button
    $('#new-image-button-top, #new-image-button-bottom').click(function(event){
		$$.newModuleRedirect($(this));
    });	

    //
    // Import
    //
	var enableDisableImportButton = function() {
		var fileSet = false;
		var fileValue = $('#importinputfile').val();
		if (fileValue) {
			fileSet = true;
		}
		$(".ui-dialog-buttonpane button:contains('Import')").button(fileSet ? "enable" : "disable");		
	}

	$('#import-button-top, #import-button-bottom').click(function(event){
		$$.hideError();
		background.fadeOutTopWindow();
		enableDisableImportButton();
		$('#importdialog').dialog('open');
		return false;
	});

	$('#importinputfile').change(function() {
		var files = $('#importinputfile').prop("files");
        var fr = new FileReader();
	
		fr.onload = function(event) {
			var content = event.target.result;
			var parentUri = $(content).filter(":first").attr('parentUri');
			var shortName = $(content).filter(":first").attr('shortName');
			var moduleUri = parentUri + '/' + shortName;
			$("#importform").attr('action', moduleUri + '?method=put');
			enableDisableImportButton();
		};

        fr.readAsText(files[0]);
	});

	// clear import input value, since the file is only loaded on
	// change event
	if($('#importinputfile').val()) {
		$('#importinputfile').val("");
	};

	$('#importdialog').dialog({
		autoOpen: false,
		width: 500,
		stack: false,
		buttons: {
			"Import": function(event) {
				event.preventDefault();
				$(this).dialog("close");
				$("#importform").submit();
			},
			"Cancel": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
			},
		}
	});    

})
