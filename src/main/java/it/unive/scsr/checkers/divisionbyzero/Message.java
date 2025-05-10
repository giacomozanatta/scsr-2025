package it.unive.scsr.checkers.divisionbyzero;

import com.google.gson.Gson;

public record Message(Warning warning, Info info) {
  public record Warning(String description) {}

  public record Info(String expression, String location) {}

  public String toJson() {
    return new Gson().toJson(this);
  }
}
