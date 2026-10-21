package com.stadtmeldung.config;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stadtmeldung.security.DatabaseUserDetailsService;

import database.connection.ConnectionProvider;
import database.connection.DatabaseConnectionProvider;
import repository.implementation.DatabaseAttachmentRepository;
import repository.implementation.DatabaseCommentRepository;
import repository.implementation.DatabaseReportRepository;
import repository.implementation.DatabaseUserRepository;
import repository.implementation.mapper.AttachmentResultSetMapper;
import repository.implementation.mapper.CommentResultSetMapper;
import repository.implementation.mapper.ReportResultSetMapper;
import repository.implementation.mapper.UserResultSetMapper;
import repository.interfaces.AttachmentRepository;
import repository.interfaces.CommentRepository;
import repository.interfaces.ReportRepository;
import repository.interfaces.UserRepository;
import service.implementation.AttachmentServiceImpl;
import service.implementation.CommentServiceImpl;
import service.implementation.ReportPhotoService;
import service.implementation.ReportServiceImpl;
import service.implementation.UserServiceImpl;
import service.implementation.security.BCryptPasswordHasher;
import service.implementation.storage.LocalFileStorageService;
import service.interfaces.AttachmentService;
import service.interfaces.CommentService;
import service.interfaces.FileStorageService;
import service.interfaces.PasswordHasher;
import service.interfaces.ReportService;
import service.interfaces.UserService;

@Configuration
public class BackendConfig {

    // Datenbankverbindung

    @Bean
    public ConnectionProvider connectionProvider() {
        return new DatabaseConnectionProvider();
    }

    // Mapper

    @Bean
    public UserResultSetMapper userResultSetMapper() {
        return new UserResultSetMapper();
    }

    @Bean
    public ReportResultSetMapper reportResultSetMapper() {
        return new ReportResultSetMapper();
    }

    @Bean
    public CommentResultSetMapper commentResultSetMapper() {
        return new CommentResultSetMapper();
    }

    @Bean
    public AttachmentResultSetMapper attachmentResultSetMapper() {
        return new AttachmentResultSetMapper();
    }

    // Repositories

    @Bean
    public UserRepository userRepository(
            ConnectionProvider connectionProvider,
            UserResultSetMapper mapper) {

        return new DatabaseUserRepository(
                connectionProvider, mapper
        );
    }

    @Bean
    public ReportRepository reportRepository(
            ConnectionProvider connectionProvider,
            ReportResultSetMapper mapper) {

        return new DatabaseReportRepository(
                connectionProvider, mapper
        );
    }

    @Bean
    public CommentRepository commentRepository(
            ConnectionProvider connectionProvider,
            CommentResultSetMapper mapper) {

        return new DatabaseCommentRepository(
                connectionProvider, mapper
        );
    }

    @Bean
    public AttachmentRepository attachmentRepository(
            ConnectionProvider connectionProvider,
            AttachmentResultSetMapper mapper) {

        return new DatabaseAttachmentRepository(
                connectionProvider, mapper
        );
    }

    // Passwort-Hashing

    @Bean
    public PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    // Services

    @Bean
    public UserService userService(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {

        return new UserServiceImpl(
                userRepository, passwordHasher
        );
    }

    @Bean
    public ReportService reportService(
            ReportRepository reportRepository,
            UserRepository userRepository) {

        return new ReportServiceImpl(
                reportRepository, userRepository
        );
    }

    @Bean
    public CommentService commentService(
            CommentRepository commentRepository,
            ReportRepository reportRepository,
            UserRepository userRepository) {

        return new CommentServiceImpl(
                commentRepository,
                reportRepository,
                userRepository
        );
    }

    @Bean
    public AttachmentService attachmentService(
            AttachmentRepository attachmentRepository,
            ReportRepository reportRepository) {

        return new AttachmentServiceImpl(
                attachmentRepository,
                reportRepository
        );
    }


    @Bean
    public DatabaseUserDetailsService databaseUserDetailsService(
            UserRepository userRepository) {

        return new DatabaseUserDetailsService(userRepository);
    }


    @Bean
    public FileStorageService fileStorageService() throws IOException {

        return new LocalFileStorageService(
                Path.of("uploads")
        );
    }


    @Bean
    public ReportPhotoService reportPhotoService(
            ReportService reportService,
            ReportRepository reportRepository,
            AttachmentRepository attachmentRepository,
            FileStorageService fileStorageService) {

        return new ReportPhotoService(
                reportService,
                reportRepository,
                attachmentRepository,
                fileStorageService
        );
    }



}
