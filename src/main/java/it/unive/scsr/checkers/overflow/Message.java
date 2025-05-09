package it.unive.scsr.checkers.overflow;

import com.google.gson.Gson;

public record Message(Warning warning, Info info) {

	public record Warning(String description, String abstractRepresentation) {
	}

	public record Info(String expression, String size, String location) {
	}

	public String toJson() {
		return new Gson().toJson(this);
	}
}
