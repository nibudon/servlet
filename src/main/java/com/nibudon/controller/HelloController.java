package com.nibudon.controller;

import com.nibudon.annotation.MyAutoWired;
import com.nibudon.annotation.MyController;
import com.nibudon.annotation.MyRequestMapping;
import com.nibudon.service.HelloService;

@MyController
@MyRequestMapping("/hello")
public class HelloController {

    @MyAutoWired
    private HelloService helloServiceImpl;

    @MyRequestMapping("/hello.html")
    public String hello(){
        return "hello servlet !";
    }

}
