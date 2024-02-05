package com.Maktab101.SpringProject.service.impl;

import com.Maktab101.SpringProject.model.Customer;
import com.Maktab101.SpringProject.repository.base.BaseUserRepository;
import com.Maktab101.SpringProject.service.CustomerService;
import com.Maktab101.SpringProject.service.base.BaseUserServiceImpl;
import com.Maktab101.SpringProject.dto.users.RegisterDto;
import com.Maktab101.SpringProject.utils.exceptions.CustomException;
import com.Maktab101.SpringProject.utils.exceptions.DuplicateValueException;
import com.Maktab101.SpringProject.utils.sortFilterable.CustomerSortFilterable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class CustomerServiceImpl extends BaseUserServiceImpl<Customer>
        implements CustomerService {

    @PersistenceContext
    private EntityManager entityManager;
    private final Validator validator;
    private final CustomerSortFilterable sortFilterable;


    @Autowired
    public CustomerServiceImpl(BaseUserRepository<Customer> baseRepository, Validator validator,
                               CustomerSortFilterable sortFilterable) {
        super(baseRepository);
        this.validator = validator;
        this.sortFilterable = sortFilterable;
    }


    @Override
    public Customer register(RegisterDto registerDto) {
        log.info("Registering with this data [{}]", registerDto);
        Set<ConstraintViolation<RegisterDto>> violations = validator.validate(registerDto);
        if (violations.isEmpty()) {
            log.info("Information is validated - commencing registration");
            checkCondition(registerDto);
            Customer customer = mapDtoValues(registerDto);
            try {
                log.info("Connecting to [{}]", baseRepository);
                return baseRepository.save(customer);
            } catch (PersistenceException e) {
                log.error("PersistenceException occurred throwing CustomException ... ");
                throw new CustomException(e.getMessage());
            }
        }
        String violationMessages = getViolationMessages(violations);
        throw new CustomException(violationMessages);
    }

    @Override
    public List<Customer> sort(List<String> sortingFields) {
        return sortFilterable.sort(entityManager, sortingFields);
    }

    protected String getViolationMessages(Set<ConstraintViolation<RegisterDto>> violations) {
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
            throw new DuplicateValueException("Email address already exists in the database");
        }
    }

    protected Customer mapDtoValues(RegisterDto registerDto) {
        log.info("Mapping [{}] values", registerDto);
        Customer customer = new Customer();
        customer.setFirstname(registerDto.getFirstname());
        customer.setLastname(registerDto.getLastname());
        customer.setEmail(registerDto.getEmailAddress());
        customer.setPassword(registerDto.getPassword());
        return customer;
    }
}
