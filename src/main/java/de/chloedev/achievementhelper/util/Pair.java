package de.chloedev.achievementhelper.util;

import org.json.JSONObject;

public class Pair<L, R> {

  private L left;
  private R right;

  Pair(L left, R right) {
    this.left = left;
    this.right = right;
  }

  public static <L, R> Pair<L, R> of(L left, R right) {
    return new Pair<>(left, right);
  }

  public static <L, R> Pair<L, R> ofNull() {
    return new Pair<L, R>(null, null);
  }

  public L getLeft() {
    return left;
  }

  public void setLeft(L left) {
    this.left = left;
  }

  public R getRight() {
    return right;
  }

  public void setRight(R right) {
    this.right = right;
  }

  @Override
  public String toString() {
    return this.toJson(0);
  }

  public String toJson(int indent) {
    JSONObject obj = new JSONObject();
    return obj.put("left", left).put("right", right).toString(indent);
  }
}
