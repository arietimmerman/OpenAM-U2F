package nl.arietimmerman.u2f.server.message;

public class ClientData {
	
	protected byte[] challenge;
	protected String origin;
	protected String typ;
	protected String cidPubkey;
		
	public static final String TYPE_PARAM = "typ";
	public static final String CHALLENGE_PARAM = "challenge";
	public static final String ORIGIN_PARAM = "origin";
	public static final String CID_PUBKEY_PARAM = "cid_pubkey";

	public static final String MESSAGETYPE_FINISH_ENROLLMENT = "navigator.id.finishEnrollment";
	public static final String MESSAGETYPE_GET_ASSERTION = "navigator.id.getAssertion";
	
	public byte[] getChallenge() {
		return challenge;
	}

	public String getOrigin() {
		return origin;
	}

	public String getTyp() {
		return typ;
	}
	
	public String getCidPubkey() {
		return cidPubkey;
	}
	
	public void setTyp(String typ) {
		this.typ = typ;
	}
	
	public void setChallenge(byte[] challenge) {
		this.challenge = challenge;
	}
	
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	
	public void setCidPubkey(String cidPubkey) {
		this.cidPubkey = cidPubkey;
	}
	
	public ClientData(){
		
	}
	
	public ClientData(byte[] challenge, String origin, String typ, String cidPubkey) {
		this.challenge = challenge;
		this.origin = origin;
		this.typ = typ;
		this.cidPubkey = cidPubkey;
	}
	
	public ClientData(byte[] challenge, String appId) {
		new ClientData(challenge, appId, MESSAGETYPE_GET_ASSERTION,null);
	}
	
}
