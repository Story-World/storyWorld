package com.packt.storyworld.repository.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.packt.storyworld.repository.StoryRepository;

@Repository
public class StoryRepositoryImpl implements StoryRepository {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(StoryRepositoryImpl.class);

	@SuppressWarnings("unused")
	@Autowired
	private MongoTemplate mongoTemplate;

}
