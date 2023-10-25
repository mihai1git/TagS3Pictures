package com.amazonaws.lambda.mihai.tagpicture.test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.amazonaws.lambda.mihai.tagpicture.test.utils.EventSourceTests;
import com.amazonaws.lambda.mihai.tagpicture.test.utils.TagSchemaTests;

@RunWith(Categories.class)
@IncludeCategory({
	TagSchemaTests.class,
	EventSourceTests.class
	}) //all: TagSchemaTests.class , EventSourceTests.class
//Note that Categories is a kind of Suite
@SuiteClasses({
		LambdaFunctionHandlerTest.class,
		LambdaFunctionHandlerTagsTest.class,
		LambdaFunctionHandlerParamTest.class
		})
public class AllTests {
	
}
