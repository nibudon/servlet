package com.nibudon.listenner;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ApplicationContext implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("init:"+1111);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("init:"+2222);
    }
}
