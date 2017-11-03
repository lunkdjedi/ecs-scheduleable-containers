package com.digitaljedi.lambda;

import com.digitaljedi.lambda.event.Hello;

public class HelloWorld {

	public static String handler(String name) {
		return "Hello " + name + "!";
	}
	
	public String pojoHandler(Hello hello) {
		return "Hello " + hello.getFirstName() + " " + hello.getLastName() + "!";
	}
}
