
# ForgeRock OpenAM FIDO U2F Authentication Module

This is a FIDO U2F authentication module for ForgeRock OpenAM.

This module allows you to use the FIDO U2F protocol on ForgeRock OpenAM.
It attempts to provide a full implementation of a FIDO U2F server for ForgeRock OpenAM.

The following features are supported

* Registration of devices, multiple devices per user
* Signing in using any registered FIDO U2F device
* Simple device management
* Whitelisting device types

For storing device registration data, any OpenAM identity store can be used (OpenDJ, SQL, LDAP, etc.) or the "Memory data store" can be used. The last one is enabled by default and allows testing this module without touching your identity store. Ideal for demo and POC scenarios.   

## What is FIDO U2F? 
> 
"The FIDO U2F protocol enables relying parties to offer a strong cryptographic 2nd factor option for end user security. The relying party's dependence on passwords is reduced. The password can even be simplified to a 4 digit PIN. End users carry a single U2F device which works with any relying party supporting the protocol. The user gets the convenience of a single 'keychain' device and convenient security."
[fidoalliance.org](https://fidoalliance.org "")

## What is ForgeRock OpenAM?
> 
"ForgeRock OpenAM provides core identity services to simplify the implementation of transparent single sign-on (SSO) as a security component in a network infrastructure. OpenAM provides the foundation for integrating diverse web applications that might typically operate against a disparate set of identity repositories and are hosted on a variety of platforms such as web and application servers."
[forgerock.org](https://forgerock.org "")

## Why this module?

Out of the box, ForgeRock OpenAM supports a lot of authentication standards. However, FIDO U2F was not yet supported.

## How to install it

1.  Ensure you can access OpenAM over HTTPS. Without HTTPS, FIDO U2F cannot work. Moreover, make sure the XUI interface is enabled.
2.  Download the source with git.
3.  Build the module with Maven.

    mvn clean package

4.  Copy the resulting jar file named "openam-u2f-0.5.jar" from the "target" directory, as well as all its dependencies found in the "target/dependencies" directory, to the "WEB-INF/lib" directory op OpenAM.
5.   Install the module using the [OpenAM tools](https://backstage.forgerock.com/#!/docs/openam/12.0.0/install-guide/chap-install-tools "OpenAM tools").
	
	./ssoadm create-svc -u amadmin -f /location/of/your/password --xmlfile 'src/main/resources/amAuthU2F.xml'

	./ssoadm register-auth-module --adminid amadmin --password-file /tmp/pwd.txt --authmodule nl.arietimmerman.openam.u2f.U2F
	
6. Restart OpenAM.
7. Configure the module. For testing, only an "App Id" is required. Make sure you use a HTTPS-url.	

## Known issues

Currently, there are some issues known.

*   ForgeRock OpenAM renders a ChoiceCallback element that allows multiple selections, as radio buttons. Hence, it is not possible to select multiple options. This functionality is used in the device listing.
*   The module requires that the XUI interface is enabled because of the usage of ScriptTextOutputCallback elements.
