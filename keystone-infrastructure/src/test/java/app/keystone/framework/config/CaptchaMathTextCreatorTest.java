package app.keystone.framework.config;

import app.keystone.infrastructure.config.captcha.CaptchaMathTextCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CaptchaMathTextCreatorTest {

    @Test
    public void test() {
        CaptchaMathTextCreator captchaMathTextCreator = new CaptchaMathTextCreator();
        for (int i = 0; i < 50; i++) {
            validateExpressionAndResult(captchaMathTextCreator.getText());
        }
    }

    private void validateExpressionAndResult(String expression) {
        String[] expressionAndResult = expression.split("@");
        Assertions.assertEquals(expressionAndResult.length, 2);
        System.out.println(expressionAndResult[0] + "  answer is " + expressionAndResult[1]);
        String safeExpression = expressionAndResult[0].endsWith("=?")
            ? expressionAndResult[0].substring(0, expressionAndResult[0].length() - 2)
            : expressionAndResult[0];
        Assertions.assertEquals(String.valueOf((int) evaluateExpression(safeExpression)), expressionAndResult[1]);
    }

    private static long evaluateExpression(String expression) {
        String operator = null;
        for (String candidate : new String[]{"+", "-", "*", "/"}) {
            if (expression.contains(candidate)) {
                operator = candidate;
                break;
            }
        }
        Assertions.assertNotNull(operator);
        String[] parts = expression.split("\\" + operator);
        long left = Long.parseLong(parts[0].trim());
        long right = Long.parseLong(parts[1].trim());
        if ("+".equals(operator)) {
            return left + right;
        }
        if ("-".equals(operator)) {
            return left - right;
        }
        if ("*".equals(operator)) {
            return left * right;
        }
        return left / right;
    }
}
