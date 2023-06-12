package com.rcelik.springguru.reactivemongodb.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;

import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

@Configuration
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    // need to update the database name for the application
    @Override
    protected String getDatabaseName() {
        return "sfg";
    }

    // creates mongo client bean that runs reactive programming manner
    @Bean
    MongoClient mongoClient() {
        return MongoClients.create();
    }

    // to authorize the application with mongo db credential, need to override that
    // method
    @Override
    protected void configureClientSettings(Builder builder) {
        // root/example are user/password for used mongo db docker container
        // admin is general database name
        MongoCredential credential = MongoCredential.createCredential("root", "admin", "example".toCharArray());

        // need to initialize cluster settings for mongo db docker container
        // 127.0.0.1 is the mongo docker container access url and 27017 is the port 
        builder.credential(credential).applyToClusterSettings(settings -> {
            settings.hosts(Collections.singletonList(new ServerAddress("127.0.0.1", 27017)));
        });
    }

}
