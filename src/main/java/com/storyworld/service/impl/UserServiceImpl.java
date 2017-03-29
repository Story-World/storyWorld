package com.storyworld.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
<<<<<<< HEAD
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
=======
import java.util.List;
>>>>>>> c9b81d0c914b4ff27fc13eade0eff256454fbfb6

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.storyworld.domain.json.Message;
import com.storyworld.domain.json.Request;
import com.storyworld.domain.json.Response;
import com.storyworld.domain.json.StatusMessage;
import com.storyworld.domain.sql.Mail;
import com.storyworld.domain.sql.MailToken;
import com.storyworld.domain.sql.Role;
import com.storyworld.domain.sql.User;
import com.storyworld.enums.Status;
import com.storyworld.enums.TokenStatus;
import com.storyworld.repository.sql.MailReposiotory;
import com.storyworld.repository.sql.MailTokenRepository;
import com.storyworld.repository.sql.RoleRepository;
import com.storyworld.repository.sql.UserRepository;
import com.storyworld.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private MailTokenRepository mailTokenRepository;

	@Autowired
	private MailReposiotory mailReposiotory;

	private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

	@Override
	public void removeToken(User user) {
		if (ChronoUnit.HOURS.between(user.getLastActionTime(), LocalDateTime.now()) >= 2) {
			user.setLastActionTime(null);
			user.setToken(null);
			userRepository.save(user);
		}
	}

	@Override
	public void login(Request request, Response response) {
		User userLogon = userRepository.findByName(request.getUser().getName());
		System.out.println(userLogon);
		Message message = new Message();

		if (userLogon != null && userLogon.getName().equals(request.getUser().getName())
				&& userLogon.getPassword().equals(request.getUser().getPassword())
				&& (!userLogon.isBlock() || (userLogon.getLastIncorrectLogin() != null
						&& ChronoUnit.MINUTES.between(userLogon.getLastIncorrectLogin(), LocalDateTime.now()) >= 10))) {
			userLogon.setToken(UUID.randomUUID().toString());
			userLogon.setLastActionTime(LocalDateTime.now());
			userLogon.setBlock(false);
			userLogon.setLastIncorrectLogin(null);
			userRepository.save(userLogon);
			response.setSuccess(true);
			message.setStatus(StatusMessage.SUCCESS);
			message.setMessage("LOGIN");
			response.setUser(userLogon);
			response.setMessage(message);
		} else {
			if (userLogon != null) {
				int incrementIncorrect = userLogon.getIncorrectLogin();
				incrementIncorrect++;
				userLogon.setIncorrectLogin(incrementIncorrect);
				if (userLogon.getIncorrectLogin() == 5) {
					userLogon.setBlock(true);
					userLogon.setLastIncorrectLogin(LocalDateTime.now());
				}
				userRepository.save(userLogon);
			}
			response.setSuccess(false);
			message.setStatus(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

	@Override
	public void register(Request request, Response response) {
		Message message = new Message();
		try {
			User user = request.getUser();
			user.setBlock(false);// change to true after ui already
			User userRegister = userRepository.save(user);
			Role role = roleRepository.findOne((long) 1);
			Set<Role> roles = new HashSet<>();
			roles.add(role);
			userRegister.setRoles(roles);
			MailToken mailToken = new MailToken();
			mailToken.setStatus(TokenStatus.REGISTER);
			mailToken.setToken(UUID.randomUUID().toString());
			Set<MailToken> tokens = new HashSet<>();
			tokens.add(mailToken);
			mailTokenRepository.save(tokens);
			response.setSuccess(true);
			message.setStatus(StatusMessage.INFO);
			message.setMessage("REGISTER");
			response.setMessage(message);
			Mail mail = new Mail();
			mail.setStatus(Status.READY);
			mail.setTemplate("TEST1");
			mail.setEmail(userRegister.getMail());
			mailReposiotory.save(mail);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			response.setSuccess(false);
			message.setStatus(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

	@Override
	public void restartPassword(String email, Response response) {
		User user = userRepository.findByMail(email);
		Message message = new Message();
		if (user != null) {
			MailToken mailToken = new MailToken();
			mailToken.setStatus(TokenStatus.RESTART_PASSWORD);
			mailToken.setToken(UUID.randomUUID().toString());
			mailToken.setValidationTime(LocalDateTime.now());
			Set<MailToken> tokens = mailTokenRepository.findByUser(user);
			tokens.add(mailToken);
			mailTokenRepository.save(tokens);
			Mail mail = new Mail();
			mail.setStatus(Status.READY);
			mail.setTemplate("TEST1");
			mail.setEmail(user.getMail());
			mailReposiotory.save(mail);
			response.setSuccess(true);
			message.setStatus(StatusMessage.INFO);
			message.setMessage("RESTARTED");
			response.setMessage(message);
		} else {
			response.setSuccess(false);
			message.setStatus(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

	@Override
	public void confirmPassword(Request request, Response response) {
		MailToken mailToken = mailTokenRepository.findByToken(request.getMailToken().getToken());
		Message message = new Message();
		if (mailToken != null && mailToken.getStatus().equals(TokenStatus.RESTART_PASSWORD)
				&& ChronoUnit.DAYS.between(mailToken.getValidationTime(), LocalDateTime.now()) >= 1
				&& mailToken.getToken().equals(request.getToken())) {
			User user = userRepository.findOne(mailToken.getUser().getId());
			try {
				user.setPassword(request.getUser().getPassword());
				userRepository.save(user);
				response.setSuccess(true);
				message.setStatus(StatusMessage.INFO);
				message.setMessage("RESTARTED");
				response.setMessage(message);
			} catch (Exception e) {
				LOG.error(e.getMessage());
				message.setStatus(StatusMessage.ERROR);
				message.setMessage("INCORRECT_DATA");
				response.setMessage(message);
			}
		} else {
			message.setStatus(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

	@Override
	public void confirmRegister(Request request, Response response) {
		MailToken mailToken = mailTokenRepository.findByToken(request.getMailToken().getToken());
		Message message = new Message();
		if (mailToken != null && mailToken.getStatus().equals(TokenStatus.REGISTER)
				&& ChronoUnit.DAYS.between(mailToken.getValidationTime(), LocalDateTime.now()) >= 1
				&& mailToken.getToken().equals(request.getToken())) {
			User user = userRepository.findOne(mailToken.getUser().getId());
			user.setLastActionTime(LocalDateTime.now());
			user.setBlock(false);
			userRepository.save(user);
			message.setStatus(StatusMessage.SUCCESS);
			message.setMessage("SUCCESS_REGISTERED");
			response.setMessage(message);
		} else {
			message.setStatus(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

	@Override
	public void changePassword(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		Message message = new Message();
		try {
			user.setPassword(request.getUser().getPassword());
			user.setLastActionTime(LocalDateTime.now());
			userRepository.save(user);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			message.setStatus(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

	@Override
	public void updateUser(Request request, Response response) {
		User user = userRepository.findByToken(request.getToken());
		Message message = new Message();
		try {
			user.setLastActionTime(LocalDateTime.now());
			user.setMail(request.getUser().getMail());
			userRepository.save(user);
			message.setStatus(StatusMessage.SUCCESS);
			message.setMessage("SUCCESS_UPDATED");
			response.setMessage(message);
			response.setUser(user);
		} catch (Exception e) {
			LOG.error(e.getMessage());
			message.setStatus(StatusMessage.ERROR);
			message.setMessage("INCORRECT_DATA");
			response.setMessage(message);
		}
	}

}