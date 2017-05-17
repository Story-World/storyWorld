package com.storyworld.repository.sql;

import org.springframework.data.jpa.repository.JpaRepository;

import com.storyworld.domain.sql.Comment;
import com.storyworld.domain.sql.LikeTypeComment;
import com.storyworld.domain.sql.User;

public interface LikeTypeCommentRepository extends JpaRepository<LikeTypeComment, Long> {

	public LikeTypeComment findByUserAndComment(User user, Comment comment);

}
