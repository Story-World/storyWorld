package com.storyworld.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.storyworld.conditions.CommonPredicate;
import com.storyworld.domain.elastic.CommentContent;
import com.storyworld.domain.json.Message;
import com.storyworld.domain.json.Request;
import com.storyworld.domain.json.Response;
import com.storyworld.domain.json.enums.StatusMessage;
import com.storyworld.domain.sql.Comment;
import com.storyworld.domain.sql.LikeTypeComment;
import com.storyworld.domain.sql.Story;
import com.storyworld.domain.sql.User;
import com.storyworld.domain.sql.enums.LikeType;
import com.storyworld.functionalInterface.JSONPrepare;
import com.storyworld.repository.elastic.CommentContentRepository;
import com.storyworld.repository.sql.CommentRepository;
import com.storyworld.repository.sql.LikeTypeCommentRepository;
import com.storyworld.repository.sql.StoryRepository;
import com.storyworld.repository.sql.UserRepository;
import com.storyworld.service.CommentService;
import com.storyworld.service.helper.CommentServiceHelper;

@Service
public class CommentServiceImpl implements CommentService {

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private StoryRepository storyRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CommentContentRepository commentContentRepository;

	@Autowired
	private LikeTypeCommentRepository likeTypeCommentRepository;

	@Autowired
	private CommonPredicate commonPredicate;

	@Autowired
	private CommentServiceHelper commentServiceHelper;

	private JSONPrepare<CommentContent> jsonPrepare = (statusMessage, message, commentContent, list,
			success) -> new Response<CommentContent>(new Message(statusMessage, message), commentContent, list,
					success);

	private static final Logger LOG = LoggerFactory.getLogger(CommentServiceImpl.class);

	@Override
	public Response<CommentContent> get(Long idStory, int page, int pageSize) {
		Story story = storyRepository.findOne(idStory);

		return Optional.ofNullable(story).isPresent() && commonPredicate.validatePageAndPageSize.test(page, pageSize)
				? commentServiceHelper.prepareCommentContent(story, page, pageSize)
				: jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false);
	}

	@Override
	public Response<CommentContent> save(Request request) {
		return userRepository.findByToken(request.getToken())
				.map(user -> commentServiceHelper.prepareToSaveComment(request, user))
				.orElse(jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false));
	}

	@Override
	public Response<CommentContent> update(Request request) {
		return userRepository.findByToken(request.getToken()).map(user -> {
			Optional<Comment> comment = commentRepository.findBy_id(request.getCommentContent().getId());
			return comment.isPresent() && request.getCommentContent() != null
					? commentServiceHelper.updateCommentContent(comment, user, request.getCommentContent())
					: jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false);
		}).orElse(jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false));
	}

	@Override
	public Response<CommentContent> delete(Request request) {
		Optional<User> user = userRepository.findByToken(request.getToken());
		Optional<Comment> comment = commentRepository.findBy_id(request.getComment().get_id());
		return user.isPresent() && comment.isPresent() ? commentServiceHelper.deleteComment(comment, user)
				: jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false);
	}

	@Override
	public synchronized Response<CommentContent> like(Request request) {
		Optional<User> userGet = userRepository.findByToken(request.getToken());
		return userGet.map(user -> {
			CommentContent commentContent = commentContentRepository.findOne(request.getCommentContent().getId());
			if (commentContent != null) {
				Optional<Comment> comment = commentRepository.findBy_id(commentContent.getId());
				Optional<LikeTypeComment> likeTypeComment = likeTypeCommentRepository.findByUserAndComment(user,
						comment.get());
				if ((likeTypeComment.isPresent() && likeTypeComment.get().getLikeType().equals(LikeType.DISLIKE))
						|| likeTypeComment == null) {
					if (likeTypeComment.isPresent() && likeTypeComment.get().getLikeType().equals(LikeType.DISLIKE)) {
						int dislike = commentContent.getDislikes() - 1;
						commentContent.setDislikes(dislike);
						likeTypeComment.get().setLikeType(LikeType.LIKE);
						likeTypeCommentRepository.save(likeTypeComment.get());
					} else {
						LikeTypeComment likeTypeCommentSave = new LikeTypeComment(user, comment.get(), LikeType.LIKE);
						likeTypeCommentRepository.save(likeTypeCommentSave);
					}
					int like = commentContent.getLikes() + 1;
					commentContent.setLikes(like);
					commentContent = commentContentRepository.save(commentContent);
					user.setLastActionTime(LocalDateTime.now());
					userRepository.save(user);
					return jsonPrepare.prepareResponse(StatusMessage.SUCCESS, "LIKED", commentContent, null, true);
				} else
					return jsonPrepare.prepareResponse(StatusMessage.WARNING, "UNIQUE_LIKE", null, null, true);
			} else
				return jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false);
		}).orElse(jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false));
	}

	@Override
	public synchronized Response<CommentContent> dislike(Request request) {
		Optional<User> userGet = userRepository.findByToken(request.getToken());
		return userGet.map(user -> {
			CommentContent commentContent = commentContentRepository.findOne(request.getCommentContent().getId());
			if (commentContent != null) {
				Optional<Comment> comment = commentRepository.findBy_id(commentContent.getId());
				Optional<LikeTypeComment> likeTypeComment = likeTypeCommentRepository.findByUserAndComment(user,
						comment.get());
				if ((likeTypeComment.isPresent() && likeTypeComment.get().getLikeType().equals(LikeType.LIKE))
						|| likeTypeComment == null) {
					if (likeTypeComment != null && likeTypeComment.get().getLikeType().equals(LikeType.LIKE)) {
						int like = commentContent.getLikes() - 1;
						commentContent.setLikes(like);
						likeTypeComment.get().setLikeType(LikeType.DISLIKE);
						likeTypeCommentRepository.save(likeTypeComment.get());
					} else {
						LikeTypeComment likeTypeCommentSave = new LikeTypeComment(user, comment.get(),
								LikeType.DISLIKE);
						likeTypeCommentRepository.save(likeTypeCommentSave);
					}
					int dislike = commentContent.getDislikes() + 1;
					commentContent.setDislikes(dislike);
					commentContent = commentContentRepository.save(commentContent);
					user.setLastActionTime(LocalDateTime.now());
					userRepository.save(user);
					return jsonPrepare.prepareResponse(StatusMessage.WARNING, "DISLIKED", commentContent, null, true);
				} else
					return jsonPrepare.prepareResponse(StatusMessage.WARNING, "UNIQUE_LIKE", null, null, true);
			} else
				return jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false);
		}).orElse(jsonPrepare.prepareResponse(StatusMessage.ERROR, "INCORRECT_DATA", null, null, false));
	}

}