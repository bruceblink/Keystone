package app.keystone.common.utils.poi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CustomExcelUtilTest {

    @Test
    void testImportAndExport() throws IOException {
        PostDTO post1 = new PostDTO(1L, "admin1", "管理员1", "1", "无备注", "1", "正常");
        PostDTO post2 = new PostDTO(2L, "admin2", "管理员2<script>alert(1)</script>", "2", "无备注", "1", "正常");

        List<PostDTO> postDTOList = new ArrayList<>();
        postDTOList.add(post1);
        postDTOList.add(post2);

        File file = File.createTempFile("custom-excel-util", ".xlsx");

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            CustomExcelUtil.writeToOutputStream(postDTOList, PostDTO.class, outputStream);
        }

        List<PostDTO> postListFromExcel;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            postListFromExcel = CustomExcelUtil.readFromInputStream(PostDTO.class, inputStream);
        }

        PostDTO post1fromExcel = postListFromExcel.get(0);
        PostDTO post2fromExcel = postListFromExcel.get(1);

        Assertions.assertEquals(post1.getPostId(), post1fromExcel.getPostId());
        Assertions.assertEquals(post1.getPostCode(), post1fromExcel.getPostCode());
        Assertions.assertEquals(post2.getPostId(), post2fromExcel.getPostId());
        Assertions.assertEquals(post2.getPostCode(), post2fromExcel.getPostCode());
        Assertions.assertNotEquals(post2.getPostName(), post2fromExcel.getPostName());
        Assertions.assertEquals(post2.getPostName().replaceAll("<[^>]*>", ""), post2fromExcel.getPostName());
    }

}
