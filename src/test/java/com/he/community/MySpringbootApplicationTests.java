package com.he.community;

import com.he.community.pojo.Student;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MySpringbootApplicationTests {

    @Qualifier(value = "student")
    @Autowired
    private Student student;

    @Test
    void contextLoads() {
        System.out.println(student.toString());
    }

}
