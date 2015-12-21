/*******************************************************************************
 * Copyright 2015 Arie Timmerman. All rights reserved.
 *******************************************************************************/

package nl.arietimmerman.openam.u2f;

import java.io.Serializable;
import java.security.Principal;

public class U2FPrincipal implements Principal, Serializable {
	
	private static final long serialVersionUID = 5347727723489433638L;
	private final String name;

	public U2FPrincipal(String name) {
		if (name == null) {
			throw new NullPointerException("illegal null input");
		}

		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return String.format("%s : %s", this.getClass().toString(), name);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}

		if (this == other) {
			return true;
		}

		if (!(other instanceof U2FPrincipal)) {
			return false;
		}
		U2FPrincipal that = (U2FPrincipal) other;
		
		if (this.getName().equals(that.getName())) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
