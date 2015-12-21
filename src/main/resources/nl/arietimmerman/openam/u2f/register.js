/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 ******************************************************************************/

var registerRequest = {
	appId : registrationData.appId,
	challenge : registrationData.challenge,
	version : registrationData.version
};

window.u2f.register([ registerRequest ], [], function(result) {

	if (result.errorCode) {
		//FIXME: implement some kind of error handling
		console.log('error occurred!');
		return;
	}

	result.sessionId = registrationData.sessionId;

	document.getElementById('signResponse').value = JSON.stringify(result);
	$('input[type=submit]').trigger('click');

});
