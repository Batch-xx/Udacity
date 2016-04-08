/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.udacity.gradle.builditbiger.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.udacity.gradle.builditbigger.javalib.JavaJokes;

import javax.inject.Named;

@Api(
  name = "myApi",
  version = "v1",
  namespace = @ApiNamespace(
    ownerDomain = "backend.builditbiger.gradle.udacity.com",
    ownerName = "backend.builditbiger.gradle.udacity.com",
    packagePath=""
  )
)
public class MyEndpoint {
    @ApiMethod(name = "sayJoke")
    public MyBean sayJoke() {
        MyBean response = new MyBean();

        JavaJokes joke = new JavaJokes();
        response.setData(joke.getJoke());

        return response;
    }
}
