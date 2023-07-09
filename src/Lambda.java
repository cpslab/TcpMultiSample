import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Consumer は, 関数を変数に入れて後で呼び出せるようにしたもの
 * 
 */
public class Lambda {
    public static void main(String[] args) {
        /**
         * 1つの引数を持つ関数
         */
        final Consumer<String> stringConsumer = (stringValue) -> {
            System.out.println(stringValue + " を受け取った");
        };
        stringConsumer.accept("A"); // A を受け取った
        stringConsumer.accept("B"); // B を受け取った

        /**
         * BiConsumer は, 2つの引数をもつ関数
         */
        final BiConsumer<String, Integer> stringAndIntBiConsumer = (stringValue, integerValue) -> {
            System.out.println(stringValue + " と " + integerValue + " を受け取った");
        };

        stringAndIntBiConsumer.accept("あ", 5); // あ と 5 を受け取った
        stringAndIntBiConsumer.accept("は", 1); // は と 1 を受け取った

        // Function は 1つの引数と1つの戻り値を持つ関数
        final Function<Integer, String> repeatHello = (repeatCount) -> "hello".repeat(repeatCount);

        System.out.println(repeatHello.apply(1)); // hello
        System.out.println(repeatHello.apply(3)); // hellohellohello

        // Predicate は, 1つの引数を持ち, boolean を返す関数
        final Predicate<String> containsA = (stringValue) -> stringValue.contains("a");

        System.out.println(containsA.test("Java")); // true
        System.out.println(containsA.test("Kotlin")); // false

        final List<String> jvmLanguageList = Arrays.asList("Java", "Kotlin", "Scala", "Groovy", "Clojure");

        // Stream API でフィルターをかける
        final List<String> includedAList = jvmLanguageList.stream().filter(containsA).toList();
        System.out.println(includedAList); // [Java, Scala]
        System.out.println(jvmLanguageList.stream().filter(
                (languageName) -> languageName.contains("o")).toList()); // [Kotlin, Groovy, Clojure]

        // 変数ではなく引数としても Consumer を指定できる
        accept3((integerValue) -> {
            System.out.println("呼ばれた (" + integerValue + "回目)"); // 呼ばれた (0回目) 呼ばれた (1回目) 呼ばれた (2回目)
        });
    }

    /**
     * 引数に指定した関数を3回呼ぶ
     * 
     * @param consumer 関数. 引数 Integer は何回目かを渡す
     */
    public static void accept3(Consumer<Integer> consumer) {
        consumer.accept(0);
        consumer.accept(1);
        consumer.accept(2);
    }
}
