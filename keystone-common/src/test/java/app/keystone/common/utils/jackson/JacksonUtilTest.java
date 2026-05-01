package app.keystone.common.utils.jackson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author duanxinyuan 2019/1/21 18:17
 */
public class JacksonUtilTest {

    @Test
    public void testObjectToJson() {
        Person person = Person.newPerson();

        String jacksonStr = JacksonUtil.to(person);
        Assertions.assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(person.getDate()),
            JacksonUtil.getAsString(jacksonStr, "date"));
        Assertions.assertEquals(person.getLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            JacksonUtil.getAsString(jacksonStr, "localDateTime"));
        Assertions.assertEquals(person.getName(), JacksonUtil.getAsString(jacksonStr, "name"));
        Assertions.assertEquals(person.getAge(), JacksonUtil.getAsInt(jacksonStr, "age"));
        Assertions.assertEquals(person.isMan(), JacksonUtil.getAsBoolean(jacksonStr, "man"));
        Assertions.assertEquals(person.getMoney(), JacksonUtil.getAsBigDecimal(jacksonStr, "money"));
        Assertions.assertEquals(person.getTrait(), JacksonUtil.getAsList(jacksonStr, "trait", String.class));

        Assertions.assertNotNull(JacksonUtil.getAsString(jacksonStr, "name"));
    }

    /**
     * 测试兼容情况
     */
    @Test
    public void testAllPrimitiveTypeToJson() {
        String json = "{\n"
            + "\"code\": \"200\",\n"
            + "\"id\": \"2001215464647687987\",\n"
            + "\"message\": \"success\",\n"
            + "\"amount\": \"1.12345\",\n"
            + "\"amount1\": \"0.12345\",\n"
            + "\"isSuccess\": \"true\",\n"
            + "\"isSuccess1\": \"1\",\n"
            + "\"key\": \"8209167202090377654857374178856064487200234961995543450245362822537162918731039965956758726661669012305745755921310000297396309887550627402157318910581311\"\n"
            + "}";
        Assertions.assertEquals(200, JacksonUtil.getAsInt(json, "code"));
        Assertions.assertEquals(2001215464647687987L, JacksonUtil.getAsLong(json, "id"));
        Assertions.assertEquals("success", JacksonUtil.getAsString(json, "message"));
        Assertions.assertEquals(new BigDecimal("1.12345"), JacksonUtil.getAsBigDecimal(json, "amount"));
        Assertions.assertEquals(new BigDecimal("0.12345"), JacksonUtil.getAsBigDecimal(json, "amount1"));
        Assertions.assertEquals(1.12345d, JacksonUtil.getAsDouble(json, "amount"), 0.00001);
        Assertions.assertEquals(0.12345d, JacksonUtil.getAsDouble(json, "amount1"), 0.00001);
        Assertions.assertTrue(JacksonUtil.getAsBoolean(json, "isSuccess"));
        Assertions.assertTrue(JacksonUtil.getAsBoolean(json, "isSuccess1"));
        Assertions.assertEquals(new BigInteger(
            "8209167202090377654857374178856064487200234961995543450245362822537162918731039965956758726661669012305745755921310000297396309887550627402157318910581311"),
            JacksonUtil.getAsBigInteger(json, "key"));
        Assertions.assertEquals("1", JacksonUtil.getAsString(json, "isSuccess1"));
    }

}
