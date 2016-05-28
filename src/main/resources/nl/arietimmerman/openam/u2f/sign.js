/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 ******************************************************************************/

if (incomingSignData.length == 0) {
	//FIXME: implement some kind of error handling
	console.log('no keys have been registered');
} else {
	var signCallback = function (result) {
		if (result.errorCode) {
			//FIXME: implement some kind of error handling
			console.log(result);
			return;
		}

		var incomingRequest = incomingSignData[result.keyHandle];
		var result = {
			appId : incomingRequest.appId,
			clientData : result.clientData,
			signatureData : result.signatureData,
			sessionId : incomingRequest.sessionId,
		};

		console.log(JSON.stringify(result));

		document.getElementById('signResponse').value = JSON.stringify(result);
		$('input[type=submit]').trigger('click')
	};

	var regKeys = [];
	var challenge;
	var appId;
	for ( var k in incomingSignData) {
		challenge = incomingSignData[k].challenge;
		appId = incomingSignData[k].appId;
		// u2f.RegisteredKey:
		regKeys.push({
			keyHandle: incomingSignData[k].keyHandle,
			version: incomingSignData[k].version,
		});
	}
	u2f.sign(appId, challenge, regKeys, signCallback);
}
