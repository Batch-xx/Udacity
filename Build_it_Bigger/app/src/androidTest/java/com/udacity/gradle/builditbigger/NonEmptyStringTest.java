package com.udacity.gradle.builditbigger;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class NonEmptyStringTest extends ApplicationTestCase<Application> {
    String mJoke = null;
    CountDownLatch signal = null;

    public NonEmptyStringTest() {
        super(Application.class);
    }


    @Override
    protected void setUp() throws Exception {
        signal =  new CountDownLatch(1);
    }

    @Override
    protected void tearDown() throws Exception {
        signal.countDown();
    }

    public void testAsynTask() throws InterruptedException{
        EndpointsAsyncTask task = new EndpointsAsyncTask();
        task.setListener(new EndpointsAsyncTask.JokeTaskListener() {
            @Override
            public void onComplete(String joke) {
                mJoke = joke;
                signal.countDown();
            }
        }).execute();
        signal.await();

        assertEquals(false,mJoke == null);
        assertEquals(false,mJoke.isEmpty());
    }
}