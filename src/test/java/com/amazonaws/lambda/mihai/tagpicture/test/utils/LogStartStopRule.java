package com.amazonaws.lambda.mihai.tagpicture.test.utils;

import java.text.SimpleDateFormat;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LogStartStopRule implements TestRule {
    private Statement base;
    private Description description;

    @Override
    public Statement apply(Statement base, Description description) {
        this.base = base;
        this.description = description;
        return new MyStatement(base);
    }

    public class MyStatement extends Statement {
        private final Statement base;

        public MyStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
        	
        	SimpleDateFormat fmt = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss");
        	
            System.out.println(fmt.format(new java.util.Date()) + " " + description.getClassName() + "." + description.getMethodName() + " START" );
            try {
                base.evaluate();
            } finally {
            	System.out.println(fmt.format(new java.util.Date()) + " " + description.getClassName() + "." + description.getMethodName() + " STOP");
            }
        }
    }
}

