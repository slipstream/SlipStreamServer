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
    $('#slider1').bxSlider({
        auto: true,
        speed: 500,
        mode: 'fade',
        pause: 5000,
        prevImage: 'images/prev.png',
        nextImage: 'images/next.png',
    });
    
	$('#formlogin').submit(function(event){
		var callback = function(data, status, xhr) {
			if(window.location.search) {
				var query = window.location.search;
				window.location = query.substring(query.lastIndexOf("=") + 1, query.length);
			} else {
			    window.location = window.location;
			}
			hideLogger();
		}
		return $$.send($(this), event, $.post, callback);
	});

	$('#formregister').submit(function(event){
		event.preventDefault();
	
		var callback = function(data, status, xhr) {
			$('body').html(data);
		}

		return $$.send($(this), event, $.post, callback);
	});

	$('#button_register').click(function(event){
		$$.hideError();
		$$.showSubmitMessage();
    });

});