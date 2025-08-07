import in.dragonbra.javasteam.types.KeyValue;

public class Test {

  public static void main(String[] args) {
    KeyValue val = KeyValue.tryLoadAsBinary("C:/Program Files (x86)/Steam/appcache/stats/UserGameStats_1099110351_1245620.bin");
    printKeyValue(val, 1);
  }

  private static void printKeyValue(KeyValue keyvalue, int depth) {
    if (keyvalue.getChildren().isEmpty())
      System.out.println(" ".repeat(depth * 4) + " " + keyvalue.getName() + ": " + keyvalue.getValue());
    else {
      System.out.println(" ".repeat(depth * 4) + " " + keyvalue.getName() + ":");
      for (KeyValue child : keyvalue.getChildren())
        printKeyValue(child, depth + 1);
    }
  }
}
