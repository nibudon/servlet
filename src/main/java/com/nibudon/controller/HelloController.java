package com.nibudon.controller;

import com.nibudon.annotation.MyAutoWired;
import com.nibudon.annotation.MyController;
import com.nibudon.annotation.MyRequestMapping;
import com.nibudon.service.HelloService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@MyController
@MyRequestMapping("/")
public class HelloController {

    @MyAutoWired
    private HelloService helloServiceImpl;

    @MyRequestMapping("/hello.html")
    public String hello(HttpServletRequest request, HttpServletResponse response){
        return "hello servlet !";
    }

}
