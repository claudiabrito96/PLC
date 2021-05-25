package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import plc.homework.Regex;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                //Created by Claudia
                Arguments.of("Empty String", " ", false),
                Arguments.of("One char email", "a@gmail.com", true),
                Arguments.of("One char domain", "OneCharDomain@a.com", true),
                Arguments.of("One char after dot", " OneCharAfterDot@gmail.c", false),
                Arguments.of("Numeric email", "123@gmail.com", true),
                Arguments.of("Numeric domain", "NumericDomain@123.com", true),
                Arguments.of("Empty email body", "@gmail.com", false),
                //This should be false
                Arguments.of("Empty domain", "email@.com", true),
                Arguments.of("Empty after dot", "email@gmail.", false),
                Arguments.of("Not @", "email.com", false),
                Arguments.of("TechTogether Email", "claudia.brito@techtogether.io", true),
                Arguments.of("LowerDash body", "this_email@email.com", true),
                Arguments.of("UpperDash body","this-email@email.com", true),
                Arguments.of("Dash domain", "email@t-domain.com", true),
                Arguments.of("Two char after dot", "email@email.ab", true),
                Arguments.of("More than three after dot", "email@email.abcd", false)

        );
    }


    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                //even 10
                Arguments.of("10 Characters", "automobile", true),
                //even 10-20
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                //even less 10
                Arguments.of("6 Characters", "6chars", false),
                //odd 10-20
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                //Created by Claudia
                //even 20
                Arguments.of("20 characters", "abcdefghijabcdefghij", true),
                //odd more than 20
                Arguments.of("25 characters", "abcdefghijabcdefghij12345", false),
                //even more than 20
                Arguments.of("22 characters", "1111111111111111111111", false),
                Arguments.of("unrestricted char", "1qwndj!2#$%^7*()_-+=", true),
                //odd less than 10
                Arguments.of("5 characters", "hello", false),
                Arguments.of("zero characters", "", false),
                //odd more than 20
                Arguments.of("21 characters", "111111111111111111111", false),
                Arguments.of("Scape", "1111111111\\111111111", true)


        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                //Created by Claudia
                Arguments.of("Missing one brackets", "[1,2,3", false),
                Arguments.of("Space after number","[1, 2, 4]", true),
                Arguments.of("Double brackets", "[[1,2,3]]", false),
                Arguments.of("Comma after last number", "[1,]", false),
                Arguments.of("Two elements", "[1,2]", true),
                Arguments.of("Empty brackets", "[]", true),
                Arguments.of("Many numbers", "[1,2,3,4,5,6,7,8,9]", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                //created by Claudia
                Arguments.of("Single character","1",true),
                Arguments.of("Multiple characters", "12345", true),
                Arguments.of("Decimal", "123.456", true),
                Arguments.of("Leading zero", "01", true),
                Arguments.of("Trailing zero", "100", true),
                Arguments.of("Leading decimal",".1", false),
                Arguments.of("Trailing decimal", "1.", false),
                Arguments.of("More than one point", "1.2.3", false),
                Arguments.of("Non-digit", "abc", false),
                Arguments.of("Negative number", "-1", true),
                Arguments.of("Negative decimal", "-1.234", true),
                Arguments.of("Positive number", "+234", true),
                Arguments.of("Positive decimal", "+12.345", true),
                Arguments.of("Positive to the right", "123+", false),
                Arguments.of("Negative to the right", "2345-", false),
                Arguments.of("- symbol middle", "1-2", false),
                Arguments.of("+ symbol middle", "1+2", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input,Regex.STRING,success);
    }

    public static Stream<Arguments> testStringRegex() {
       return Stream.of(
               Arguments.of("Empty quotes","\"\"", true),
               Arguments.of("Quoted String","\"Hello,World!\"",true),
               Arguments.of("Proper spacing","\"1\\t2\"",true),
               Arguments.of("Symbols", "\"@#$%^&*()!{}\"", true),
               Arguments.of("Numbers", "\"1234567890\"", true),
               Arguments.of("Missing both quotes", "hola", false),
               Arguments.of("Missing left quote", "hello\"", false),
               Arguments.of("Missing right quote", "\"hello", false),
               Arguments.of("Invalid scape", "\"invalid\\scape\"",false),
               Arguments.of("Empty String", "", false),
               Arguments.of("Double double quoted", "\"\"hello\"\"", true)

       );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
