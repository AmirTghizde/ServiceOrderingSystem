package com.Maktab101.SpringProject.service.impl;


import com.Maktab101.SpringProject.model.Manager;
import com.Maktab101.SpringProject.repository.ManagerRepository;
import com.Maktab101.SpringProject.service.ManagerService;
import com.Maktab101.SpringProject.service.base.BaseUserServiceImpl;
import com.Maktab101.SpringProject.service.dto.RegisterDto;
import com.Maktab101.SpringProject.utils.CustomException;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.Set;

@Slf4j
@Service
public class ManagerServiceImpl extends BaseUserServiceImpl<Manager>
        implements ManagerService {

    private final Validator validator;

    public ManagerServiceImpl(ManagerRepository baseRepository, Validator validator) {
        super(baseRepository);
        this.validator = validator;
    }

    @Override
    public Manager register(RegisterDto registerDto) {
        log.info("Registering with this data [{}]", registerDto);
        Set<ConstraintViolation<RegisterDto>> violations = validator.validate(registerDto);
        if (violations.isEmpty()) {
            log.info("Information is validated - commencing registration");
            checkCondition(registerDto);
            Manager manager = mapDtoValues(registerDto);
            try {
                log.info("Connecting to [{}]",baseRepository);
                return baseRepository.save(manager);
            } catch (PersistenceException e) {
                System.out.println(e.getMessage());
            }
        }
        String violationMessages = getViolationMessages(violations);
        throw new CustomException("ValidationException", violationMessages);
    }

    private String getViolationMessages(Set<ConstraintViolation<RegisterDto>> violations) {
        log.error("RegisterDto violates some fields throwing exception");
        StringBuilder messageBuilder = new StringBuilder();
        for (ConstraintViolation<RegisterDto> violation : violations) {
            messageBuilder.append("\n").append(violation.getMessage());
        }
        return messageBuilder.toString().trim();
    }

    protected void checkCondition(RegisterDto registerDto) {
        log.info("Checking registration conditions");
        if (existsByEmailAddress(registerDto.getEmailAddress())) {
            log.error("[{}] already exists in the database throwing exception", registerDto.getEmailAddress());
            throw new CustomException("DuplicateEmailAddress", "Email address already exists in the database");
        }
    }

    protected Manager mapDtoValues(RegisterDto registerDto) {
        Random random = new Random();
        log.info("Mapping [{}] values",registerDto);
        Manager manager = new Manager();
        manager.setFirstname(registerDto.getFirstname());
        manager.setLastname(registerDto.getLastname());
        manager.setEmail(registerDto.getEmailAddress());
        manager.setPassword(registerDto.getPassword());

        int number = random.nextInt(90000) + 10000;
        manager.setManagerCode("M"+number);

        return manager;
    }
}
