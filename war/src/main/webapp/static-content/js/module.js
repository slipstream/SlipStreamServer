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

	$('input[id="savemodulebutton"]').click(function(event){
		event.preventDefault();
		$$.hideError();
		background.fadeOutTopWindow();
		$('#savemoduledialog').dialog('open');
		return false;
	});

	$('#savemoduledialog').dialog({
		autoOpen: false,
		title: 'Save Module',
		buttons: {
			"Save": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
				$("#commentinput").val($("#savecomment").val());
				$("#formsave").submit();
			},
			"Cancel": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
			},
		}
	});

	var enableDisableImportButton = function() {
		var fileSet = false;
		var fileValue = $('#importinputfile').val();
		if (fileValue) {
			fileSet = true;
		}
		$(".ui-dialog-buttonpane button:contains('Import')").button(fileSet ? "enable" : "disable");		
	}

	$('input[id="importbutton"]').click(function(event){
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

	$('input[id="copybutton"]').click(function(event){
		event.preventDefault();
		$$.hideError();
		background.fadeOutTopWindow();
		$('#copydialog').dialog('open');
		return false;
	});

	$('#copydialog').dialog({
		autoOpen: false,
		title: 'Copy Module',
		width: 500,
		stack: false,
		buttons: {
			"Copy": function() {
				background.fadeInTopWindow();
				errors = 0;
				var showError = function(message) {
					$$.show($("#copydialogerror"), message);
				}
				var validate = function() {
					if($("#target_project_uri").val() === "") {
						showError("Missing project where to create the copy");
						errors += 1;
					}
					if($("#target_name").val() === "") {
						showError("Missing new name");
						errors += 1;
					}
				};
				validate();
				if(errors) {
					return;
				}
				$(this).dialog("close");
				var target = $("#target_project_uri").val();
				var action = $("#copyform").attr("action");
				$("#copyform").attr("action", "module/" + target);
				$("#copyform").submit();
			},
			"Cancel": function() {
				$(this).dialog("close");
				background.fadeInTopWindow();
			},
		}
	});

	$('#inheritedGroupMembers').click(function(event) { 
		if($('#inheritedGroupMembers').is(':checked')) {
			$('#groupmembers').hide();
		} else {
			$('#groupmembers').show();
		};
	});
	
	if($('#inheritedGroupMembers').is(':checked')) {
		$('#groupmembers').hide();
	}
	
	var initProjectPage = function() {
		$('#childrensection').each(function(i, node) {
			tabActivator.defaultSectionName = 'childrensection';
			tabActivator.defaultSectionTabName = 'childrensectionTab';
		});
	}

	initProjectPage();

	$('#isbase').click(function(event) { 
		if($('#isbase').is(':checked')) {
			$('#moduleReferenceChooser').attr('disabled', true);
			$('#cloudimages input').removeAttr('disabled');

			$('#platform').removeAttr('disabled');
			$('#loginUser').removeAttr('disabled');
		} else {
			$('#moduleReferenceChooser').removeAttr('disabled');
			$('#cloudimages input').attr('disabled', true);

			$('#platform').attr('disabled', true);
			$('#loginUser').attr('disabled', true);
		};
	});

	$('#runOptions').dialog({
		autoOpen: false,
		width: 600,
		buttons: {
			"Run": function() {				
				$(this).dialog("close");
				background.fadeInTopWindow();
				showSubmitMessage();
				$('form[name="runwithoptions"]').submit();
			},
			"Cancel": function() {
				$(this).dialog("close");
			}
		},
		close: function() {
			background.fadeInTopWindow();
		},
	});

	$('input[value="Run..."]').click(function(event){
		event.preventDefault();
		$$.hideError();
		background.fadeOutTopWindow();
		$('#runOptions').dialog('open');
		return false;
	});

	$('input[value="Run"]').click(function(event){
		$$.hideError();
		showSubmitMessage();
	});

	$('input[value="Build"]').click(function(event){
		$$.hideError();
		showSubmitMessage();
	});

//	$('form[name="runwithoptions"]').css('color','blue');


	$('form[name="runwithoptions"]').submit(function(event){
		event.preventDefault();
		background.fadeInTopWindow();
		showSubmitMessage();
		return $$.send($(this), event, $.post);
	});	

	$('button[name="cancelrunwithoptions"]').click(function(event){
		event.preventDefault();
		background.fadeInTopWindow();
		$('#runOptions').hide();
		hideSubmitMessage();
	});	
	
})
