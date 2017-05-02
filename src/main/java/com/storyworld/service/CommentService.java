package com.storyworld.service;

import com.storyworld.domain.json.Request;
import com.storyworld.domain.json.Response;

public interface CommentService {

	public void get(Long idStory, int page, int pageSize, Response response);

	public void save(Request request, Response response);

	public void update(Request request, Response response);

	public void delete(Long id, Response response);

}
