package com.stadtmeldung.config;

import database.connection.ConnectionProvider;
import database.connection.DatabaseConnectionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import repository.implementation.DatabaseAttachmentRepository;
import repository.implementation.DatabaseCommentRepository;
import repository.implementation.DatabaseReportRepository;
import repository.implementation.DatabaseReportVoteRepository;
import repository.implementation.DatabaseUserRepository;
import repository.implementation.mapper.AttachmentResultSetMapper;
import repository.implementation.mapper.CommentResultSetMapper;
import repository.implementation.mapper.ReportResultSetMapper;
import repository.implementation.mapper.UserResultSetMapper;
import repository.interfaces.AttachmentRepository;
import repository.interfaces.CommentRepository;
import repository.interfaces.ReportRepository;
import repository.interfaces.ReportVoteRepository;
import repository.interfaces.UserRepository;
import service.implementation.AttachmentServiceImpl;
import service.implementation.CommentServiceImpl;
import service.implementation.ReportServiceImpl;
import service.implementation.ReportVoteServiceImpl;
import service.implementation.UserServiceImpl;
import service.implementation.security.BCryptPasswordHasher;
import service.interfaces.AttachmentService;
import service.interfaces.CommentService;
import service.interfaces.PasswordHasher;
import service.interfaces.ReportService;
import service.interfaces.ReportVoteService;
import service.interfaces.UserService;

@Configuration
public class BackendConfig {
    @Bean
    ConnectionProvider connectionProvider() {
        return new DatabaseConnectionProvider();
    }

    @Bean
    UserResultSetMapper userResultSetMapper() {
        return new UserResultSetMapper();
    }

    @Bean
    ReportResultSetMapper reportResultSetMapper() {
        return new ReportResultSetMapper();
    }

    @Bean
    CommentResultSetMapper commentResultSetMapper() {
        return new CommentResultSetMapper();
    }

    @Bean
    AttachmentResultSetMapper attachmentResultSetMapper() {
        return new AttachmentResultSetMapper();
    }

    @Bean
    UserRepository userRepository(ConnectionProvider connections,
                                  UserResultSetMapper mapper) {
        return new DatabaseUserRepository(connections, mapper);
    }

    @Bean
    ReportRepository reportRepository(ConnectionProvider connections,
                                      ReportResultSetMapper mapper) {
        return new DatabaseReportRepository(connections, mapper);
    }

    @Bean
    CommentRepository commentRepository(ConnectionProvider connections,
                                        CommentResultSetMapper mapper) {
        return new DatabaseCommentRepository(connections, mapper);
    }

    @Bean
    AttachmentRepository attachmentRepository(
            ConnectionProvider connections,
            AttachmentResultSetMapper mapper) {
        return new DatabaseAttachmentRepository(connections, mapper);
    }

    @Bean
    ReportVoteRepository reportVoteRepository(
            ConnectionProvider connections) {
        return new DatabaseReportVoteRepository(connections);
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    UserService userService(UserRepository users, PasswordHasher hasher) {
        return new UserServiceImpl(users, hasher);
    }

    @Bean
    ReportService reportService(ReportRepository reports,
                                UserRepository users) {
        return new ReportServiceImpl(reports, users);
    }

    @Bean
    CommentService commentService(CommentRepository comments,
                                  ReportRepository reports,
                                  UserRepository users) {
        return new CommentServiceImpl(comments, reports, users);
    }

    @Bean
    AttachmentService attachmentService(AttachmentRepository attachments,
                                        ReportRepository reports) {
        return new AttachmentServiceImpl(attachments, reports);
    }

    @Bean
    ReportVoteService reportVoteService(ReportVoteRepository votes,
                                        ReportRepository reports,
                                        UserRepository users) {
        return new ReportVoteServiceImpl(votes, reports, users);
    }
}
